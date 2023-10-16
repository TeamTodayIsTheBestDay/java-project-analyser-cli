package team.todaybest.analyser.service.impl;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.helper.interfaces.DirectoryTraverser;
import team.todaybest.analyser.helper.FileSystemHelper;
import team.todaybest.analyser.model.*;
import team.todaybest.analyser.service.LoadProjectService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 读取一个项目。
 * 为了优化性能，项目采用并发方式读取文件并解析AST语法树，减少I/O过程造成的忙等待。
 *
 * @author cineazhan
 */
@Slf4j
@Service
public class LoadProjectServiceImpl implements LoadProjectService {
    ExecutorService executor = Executors.newCachedThreadPool();
    AtomicInteger loadProjectThreads = new AtomicInteger(0);

    // 线程安全的几个成员变量
    Map<String, JavaPackage> packageMap = new ConcurrentHashMap<>(); // 线程安全的Map

    @Override
    public JavaProject loadProject(File startDir) {
        // 清理局部变量
        packageMap.clear();

        // 遍历目录、读取所有文件
        FileSystemHelper.traverseDirectories(startDir, file -> {
            if (file.getName().matches(".*\\.java")) {
                executor.submit(() -> {
                    loadProjectThreads.addAndGet(1);
                    var javaFile = loadFile(file);
                    loadProjectThreads.addAndGet(-1);
                });
            }
        });

        // 等待遍历结束
        do {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } while (loadProjectThreads.get() > 0);

        // 将缓存的Packages制作成Project
        return resolvePackages();
    }

    private JavaFile loadFile(File fileObj) {
        var file = new JavaFile();
        file.setFileName(fileObj.getName());

        // 读入JavaParser
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(fileObj);
        } catch (Exception e) {
            log.error("解析文件{}时出现异常：{}", fileObj.getName(), e.getMessage());
            throw new RuntimeException();
        }

        // 获取包信息
        var pdOption = cu.getPackageDeclaration();
        if (pdOption.isEmpty()) {
            log.error("解析文件{}时读取包信息失败", fileObj.getName());
            throw new RuntimeException();
        }
        var pd = pdOption.get();
        var pdName = pd.getName().asString();
        file.setPackageName(pdName);

        // 将包信息写入暂存区域
        if (packageMap.containsKey(pdName)) {
            packageMap.get(pdName).getFiles().add(file);
        } else {
            var pkg = new JavaPackage();
            pkg.setPackageDeclaration(pd);
            pkg.setFiles(new ArrayList<>());
            pkg.getFiles().add(file);
            packageMap.put(pdName, pkg);
        }

        file.setCompilationUnit(cu);

        // 预处理导入项目s
        var importDeclarationMap = cu.getImports().stream().collect(Collectors.toMap(importDeclaration -> importDeclaration.getName().getIdentifier(), NodeWithName::getNameAsString));

        // 将接口和类的定义保存
        file.setClasses(cu.getChildNodes().stream()
                .filter(node -> node instanceof ClassOrInterfaceDeclaration)
                .map(node -> resolveJavaClasses(node, importDeclarationMap))
                .toList());

        return file;
    }

    /**
     * 将Java类解析并导入。
     * <br/>
     * 备注：为了调用的时候方便，传入类型是AST树的节点。
     *
     * @return 完成制作的Class
     */
    private JavaClass resolveJavaClasses(Node node, Map<String, String> importDeclarationMap) {
        assert node instanceof ClassOrInterfaceDeclaration;
        var declaration = (ClassOrInterfaceDeclaration) node;

        var javaClass = new JavaClass();

        assert declaration.getFullyQualifiedName().isPresent(); // 暴躁老哥，在线assert（懒鬼是这样的）
        var classRef = declaration.getFullyQualifiedName().get();
        javaClass.setDeclaration(declaration);
        javaClass.setClassReference(classRef);

        // 扫描其中的方法
        javaClass.setMethods(declaration.getMethods().stream().map(methodDeclaration -> {
            var method = new JavaMethod();
            method.setDeclaration(methodDeclaration);
            method.setName(methodDeclaration.getNameAsString());
            method.setClassReference(classRef);

            var returnReference = methodDeclaration.getTypeAsString();
            if (importDeclarationMap.containsKey(returnReference)) {
                returnReference = importDeclarationMap.get(returnReference);
            }
            method.setReturnClassReference(returnReference);

            return method;
        }).toList());

        return javaClass;
    }

    /**
     * 将上一步中收集的Packages制作成树形结构
     *
     * @return 完成制作的Project
     */
    private JavaProject resolvePackages() {
        // 制作树形结构
        packageMap.forEach((key, value) -> {
            var packageNameList = splitPackageName(key);
            for (int i = packageNameList.size() - 1; i > 0; i--) {
                // 层层遍历寻找父包
                var fatherName = joinPackageName(packageNameList.subList(0, i));
                if (packageMap.containsKey(fatherName)) {
                    var fatherPackage = packageMap.get(fatherName);
                    fatherPackage.getChildrenPackages().add(value);
                    value.setParentPackage(fatherPackage);
                    break;
                }
            }
        });

        // 找出根节点（们）
        var project = new JavaProject();
        packageMap.values().forEach(javaPackage -> {
            if (javaPackage.getParentPackage() == null) {
                project.getPackages().add(javaPackage);
            }
        });
        return project;
    }

    /**
     * 帮手：拆解包名
     */
    private List<String> splitPackageName(String packageName) {
        return List.of(packageName.split("\\."));
    }

    /**
     * 帮手：合并包名
     */
    private String joinPackageName(List<String> packageNameList) {
        return String.join(".", packageNameList);
    }
}
