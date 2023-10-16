package team.todaybest.analyser.service.impl;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.dto.ClassInstance;
import team.todaybest.analyser.helper.JavaProjectHelper;
import team.todaybest.analyser.model.JavaFile;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.service.ClassesService;
import team.todaybest.analyser.service.ImportService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cinea
 */
@Slf4j
@Service
public class ClassesServiceImpl implements ClassesService {

    ImportService importService;

    @Autowired
    public void setImportService(ImportService importService) {
        this.importService = importService;
    }

    /**
     * 在整个项目中搜索某个类的实例。
     *
     * @param classReference 形如<code>team.todaybest.analyser.service.ClassesService</code>的类路径。注意，如有泛型参数，也需要一并给出。
     */
    @Override
    public List<ClassInstance> searchAllInstance(String classReference, JavaProject project) {
        var result = new ArrayList<ClassInstance>();

        project.getPackages().forEach(javaPackage -> JavaProjectHelper.traverseJavaFiles(javaPackage, javaFile -> {
            if (importService.referenceExists(javaFile.getCompilationUnit().getImports(), classReference)) {
                var packageNameList = classReference.split("\\.");
                var className = packageNameList[packageNameList.length - 1];
                javaFile.getCompilationUnit().getTypes().forEach(typeDeclaration -> {
                    if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
                        var subResult = searchInstancesInClasses(className, (ClassOrInterfaceDeclaration) typeDeclaration);
                        subResult.forEach(classInstance -> classInstance.setPackageName(javaFile.getPackageName()));
                        subResult.forEach(classInstance -> classInstance.setFileName(javaFile.getFileName()));
                        result.addAll(subResult);
                    }
                });
            }
        }));

        return result;
    }

    /**
     * 备注：只有import了目标的类，才进入这个函数进行扫描。
     */
    private List<ClassInstance> searchInstancesInClasses(String className, ClassOrInterfaceDeclaration declaration) {
        if (declaration.isInterface()) {
            // 不扫描接口
            return new ArrayList<>();
        }

        var result = new ArrayList<ClassInstance>();

        // 首先扫描类成员
        declaration.getFields().forEach(field -> {
            if (field.isFieldDeclaration()) {
                field.getVariables().forEach(variableDeclarator -> {
                    if (className.equals(variableDeclarator.getType().asString())) {
                        log.debug("searchInstancesInClasses：找到类{}的成员{}！", declaration.getName(), variableDeclarator.getName());
                        result.add(ClassInstance.builder()
                                .classDeclaration(declaration)
                                .name(variableDeclarator.getNameAsString())
                                .blockStatement(null) // null表示对类内所有部分都有效。
                                .build());
                    }
                });
            }
        });

        // 接下来该扫描类内所有函数了
        declaration.getMethods().forEach(methodDeclaration -> result.addAll(searchInstancesInMethods(className, methodDeclaration)));

        result.forEach(classInstance -> classInstance.setClassDeclaration(declaration));

        return result;
    }

    /**
     * 备注：只有import了目标的类，才进入这个函数进行扫描。
     */
    private List<ClassInstance> searchInstancesInMethods(String className, MethodDeclaration methodDeclaration) {
        var result = new ArrayList<ClassInstance>();

        if (methodDeclaration.getBody().isEmpty()) {
            return result;
        }

        // 扫描函数参数
        methodDeclaration.getParameters().forEach(parameter -> {
            if (className.equals(parameter.getTypeAsString())) {
                log.debug("searchInstancesInMethods: 在函数{}内找到通过形式参数获得的类{}的实例{}", methodDeclaration.getNameAsString(), className, parameter.getNameAsString());
                result.add(ClassInstance.builder()
                        .name(parameter.getNameAsString())
                        .classDeclaration(null)
                        .blockStatement(methodDeclaration.getBody().get())
                        .build());
            }
        });

        // 扫描函数体
        methodDeclaration.getBody().get().getStatements().forEach(statement -> {
            searchInstancesInNodes(className, statement, methodDeclaration, result);
        });

        result.forEach(classInstance -> classInstance.setMethodDeclaration(methodDeclaration));

        return result;
    }

    /**
     * <p>递归调用</p>
     * <p>边界情况目前靠手工定义</p>
     */
    private void searchInstancesInNodes(String className, Node node, MethodDeclaration methodDeclaration, List<ClassInstance> result) {
        if (node instanceof NameExpr || node instanceof SimpleName || node instanceof MethodCallExpr || node instanceof LiteralExpr) {
            return;
        }
        if (node instanceof VariableDeclarationExpr) {
            assert methodDeclaration.getBody().isPresent(); // 消除警告
            var expr = (VariableDeclarationExpr) node; // 不能用模式变量，因为JavaParser识别不了。
            // 我们只关心变量定义的表达式
            expr.getVariables().forEach(variableDeclarator -> {
                // 处理三种可能的情况：Cast、new和普通赋值。尽量回避var。
                if (variableDeclarator.getInitializer().isPresent()) {
                    var initializer = variableDeclarator.getInitializer().get();
                    if (initializer.isCastExpr()) {
                        var iniExpr = (CastExpr) initializer;
                        var type = iniExpr.getTypeAsString();
                        if (className.equals(type)) {
                            log.debug("searchInstancesInMethods: 在函数{}内找到通过强制转换获得的类{}的实例{}", methodDeclaration.getNameAsString(), className, variableDeclarator.getNameAsString());
                            result.add(ClassInstance.builder()
                                    .name(variableDeclarator.getNameAsString())
                                    .classDeclaration(null)
                                    .blockStatement(methodDeclaration.getBody().get())
                                    .build());
                        }
                    } else if (initializer.isObjectCreationExpr()) {
                        var objExpr = (ObjectCreationExpr) initializer;
                        var type = objExpr.getTypeAsString();
                        if (className.equals(type)) {
                            log.debug("searchInstancesInMethods: 在函数{}内找到通过创建对象获得的类{}的实例{}", methodDeclaration.getNameAsString(), className, variableDeclarator.getNameAsString());
                            result.add(ClassInstance.builder()
                                    .name(variableDeclarator.getNameAsString())
                                    .classDeclaration(null)
                                    .blockStatement(methodDeclaration.getBody().get())
                                    .build());
                        }
                    } else if (className.equals(variableDeclarator.getTypeAsString())) {
                        log.debug("searchInstancesInMethods: 在函数{}内找到通过直接声明/赋值获得的类{}的实例{}", methodDeclaration.getNameAsString(), className, variableDeclarator.getNameAsString());
                        result.add(ClassInstance.builder()
                                .name(variableDeclarator.getNameAsString())
                                .classDeclaration(null)
                                .blockStatement(methodDeclaration.getBody().get())
                                .build());
                    }
                }
            });
        }
        node.getChildNodes().forEach(childNode -> {
            searchInstancesInNodes(className, childNode, methodDeclaration, result);
        });
    }

    @ShellComponent("Test Classes Service")
    static class ClassServiceTestProvider {
        @ShellMethod("Spring Shell命令必须具有非空描述")
        void test() throws IOException {
            var cu = StaticJavaParser.parse(Path.of("D:\\IdeaProjects\\java-project-analyser-cli\\src\\main\\java\\team\\todaybest\\analyser\\service\\impl\\ClassesServiceImpl.java"));
            var clz = cu.getClassByName("ClassesServiceImpl");
            var res = new ClassesServiceImpl().searchInstancesInClasses("ArrayList<ClassInstance>", clz.get());
            System.out.println(res.size());
        }
    }
}
