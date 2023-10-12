package team.todaybest.analyser.service.impl;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.helper.interfaces.DirectoryTraverser;
import team.todaybest.analyser.helper.FileSystemHelper;
import team.todaybest.analyser.model.JavaFile;
import team.todaybest.analyser.model.JavaPackage;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.service.LoadProjectService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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

    // 线程安全的几个成员变量
    Map<String, JavaPackage> packageMap = new ConcurrentHashMap<>(); // 线程安全的Map

    @Override
    public JavaProject loadProject(File startDir) {
        List<Future<Void>> futures = new ArrayList<>(); // 用于跟踪已提交任务的Future

        // 清理局部变量
        packageMap.clear();

        // 遍历目录、读取所有文件
        FileSystemHelper.traverseDirectories(startDir, new DirectoryTraverser() {
            @Override
            public void process(File file) {
                if (file.getName().matches(".*\\.java")) {
                    Future<Void> future = executor.submit(() -> {
                        var javaFile = loadFile(file);
                        return null;
                    });
                    futures.add(future);
                }
            }
        });

        // 等待遍历结束
        for (var future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                log.error("Error processing file", e);
                throw new RuntimeException();
            }
        }

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

        // 将接口和类的定义保存
        file.setDeclarations(cu.getChildNodes().stream()
                .filter(node -> node instanceof ClassOrInterfaceDeclaration)
                .map(node -> (ClassOrInterfaceDeclaration) node)
                .toList())
        ;

        return file;
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
        packageMap.values().forEach(javaPackage->{
            if (javaPackage.getParentPackage()==null){
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
