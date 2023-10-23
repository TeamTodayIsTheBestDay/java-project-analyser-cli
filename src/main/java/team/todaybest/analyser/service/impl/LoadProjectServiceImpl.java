package team.todaybest.analyser.service.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.helper.FileSystemHelper;
import team.todaybest.analyser.helper.IncompleteCodeHelper;
import team.todaybest.analyser.model.*;
import team.todaybest.analyser.service.ImportService;
import team.todaybest.analyser.service.LoadProjectService;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    ExecutorService executor = Executors.newFixedThreadPool(4);

    // 线程安全的几个成员变量
    Map<String, JavaPackage> packageMap = new ConcurrentHashMap<>(); // 线程安全的Map

    // 锁
    final Object lock = new Object();

    JavaParser javaParser;

    ImportService importService;

    @Autowired
    public void setImportService(ImportService importService) {
        this.importService = importService;
    }

    @Override
    public JavaProject loadProject(File startDir) {
        // 清理局部变量
        packageMap.clear();

        // 初始化分析器
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setSymbolResolver(new JavaSymbolSolver(new CombinedTypeSolver(
                new ReflectionTypeSolver(),
                new JavaParserTypeSolver(startDir)
        )));
        javaParser = new JavaParser(parserConfiguration);

        // 准备并发锁
        AtomicInteger fileNum = new AtomicInteger();
        FileSystemHelper.traverseDirectories(startDir, file -> {
            if (file.getName().matches(".*\\.java")) {
                fileNum.getAndIncrement();
            }
        });

        try (var pb = new ProgressBarBuilder()
                .setTaskName("Resolving")
                .setInitialMax(fileNum.get())
                .setStyle(ProgressBarStyle.ASCII)
                .setMaxRenderedLength(80)
                .setUpdateIntervalMillis(60)
                .build()) {
            var latch = new CountDownLatch(fileNum.get());

            // 遍历目录、读取所有文件
            FileSystemHelper.traverseDirectories(startDir, file -> {
                if (file.getName().matches(".*\\.java")) {
                    executor.submit(() -> {
                        try {
                            var javaFile = loadFile(file);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            pb.step();
                            latch.countDown();
                        }
                    });
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // 将缓存的Packages制作成Project
        return resolvePackages();
    }

    @Override
    public void makeInvokeIndex(JavaProject project) {
        // 初始化
        var map = new ConcurrentHashMap<String, List<MethodCallExpr>>();

        var pbb = new ProgressBarBuilder()
                .setTaskName("Indexing")
                .setStyle(ProgressBarStyle.ASCII)
                .setUpdateIntervalMillis(60)
                .setMaxRenderedLength(80);

        for (var javaMethod : ProgressBar.wrap(project.getMethodTrie().values(), pbb)) {
            try {
                var imports = javaMethod.getJavaClass().getJavaFile().getCompilationUnit().getImports();
                javaMethod.getDeclaration().accept(new VoidVisitorAdapter<Object>() {
                    @Override
                    public void visit(MethodCallExpr expr, Object arg) {
                        // do nothing, just see how performance is
                        try {
                            resolveMethodCallExpr(expr, map, project);
                        } catch (Exception e) {
                            return;
                        }
                    }
                }, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        project.setInvokedRelation(map);
    }

    private void resolveMethodCallExpr(MethodCallExpr expr, Map<String, List<MethodCallExpr>> map, JavaProject javaProject) {
        var resolved = expr.resolve();

        if (!javaProject.getClassMap().containsKey(resolved.declaringType().getId())) {
            // 非本项目的方法
            return;
        }

        var signature = resolved.getQualifiedSignature();
        map.getOrDefault(signature, new ArrayList<>()).add(expr);

    }

    private JavaFile loadFile(File fileObj) throws IOException {
        var file = new JavaFile();
        file.setPath(fileObj.getAbsolutePath());
        file.setFileName(fileObj.getName());

        // 发挥并发优势，在锁外读取文件
        var reader = new BufferedInputStream(new FileInputStream(fileObj));
        var fileContent = IOUtils.toString(reader, StandardCharsets.UTF_8);

        // 检查简单的语法错误
        var syntaxOk = IncompleteCodeHelper.reviewCode(fileContent, fileObj.getAbsolutePath());
        if (!syntaxOk) {
            throw new RuntimeException("Syntax Error Found. Please review your code and retry.");
        }

        // 读入JavaParser
        CompilationUnit cu;
        synchronized (lock) {
            try {
                cu = javaParser.parse(fileContent).getResult().orElseThrow();
            } catch (Exception e) {
                log.error("解析文件{}时出现异常：{}", fileObj.getName(), e.getMessage());
                throw new RuntimeException();
            }
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
                .peek(javaClass -> javaClass.setJavaFile(file))
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
        var resolved = declaration.resolve();

        var javaClass = new JavaClass();

        javaClass.setDeclaration(declaration);
        javaClass.setClassReference(resolved.getQualifiedName());

        // 扫描其中的方法
        javaClass.setMethods(declaration.getMethods().stream().map(methodDeclaration -> {
            var resolvedMethod = methodDeclaration.resolve();
            var method = new JavaMethod();

            method.setDeclaration(methodDeclaration);
            method.setName(methodDeclaration.getNameAsString());
            method.setClassReference(javaClass.getClassReference());
            method.setJavaClass(javaClass);

            method.setQualifiedSignature(resolvedMethod.getQualifiedSignature());

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
