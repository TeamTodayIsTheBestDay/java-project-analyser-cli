package team.todaybest.analyser.service.impl;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.model.JavaInvokeChain;
import team.todaybest.analyser.model.JavaMethod;
import team.todaybest.analyser.model.JavaPackage;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.service.MethodService;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author cinea
 */
@Slf4j
@Service
public class MethodServiceImpl implements MethodService {
    @Override
    public void makeMethodsMap(JavaProject project) {
        var map = new HashMap<String, JavaMethod>();
        project.getPackages().forEach(javaPackage -> {
            traverseMethodsRecursive(javaPackage, javaMethod -> map.put(
                    javaMethod.getClassReference() + "." + javaMethod.getName(),
                    javaMethod
            ));
        });
        project.setMethodMap(map);
    }

    private void traverseMethodsRecursive(JavaPackage javaPackage, TraverseMethodsAction traverseMethodsAction) {
        javaPackage.getFiles().forEach(javaFile -> {
            javaFile.getClasses().forEach(javaClass -> {
                javaClass.getMethods().forEach(traverseMethodsAction::action);
            });
        });
        javaPackage.getChildrenPackages().forEach(childPackage -> {
            traverseMethodsRecursive(childPackage, traverseMethodsAction);
        });
    }

    interface TraverseMethodsAction {
        void action(JavaMethod javaMethod);
    }

    @Override
    public List<ImmutableList<String>> getInvokes(JavaProject project, JavaMethod method) {
        var result = new HashSet<ImmutableList<String>>();

        // 遍历，寻找函数调用
        method.getDeclaration().accept(new VoidVisitorAdapter<Object>() {
            @Override
            public void visit(MethodCallExpr expr, Object arg) {
                ResolvedMethodDeclaration methodDeclaration;
                try {
                    methodDeclaration = expr.resolve();
                } catch (Exception e) {
                    return;
                }
                var className = methodDeclaration.declaringType().getQualifiedName();
                var methodName = methodDeclaration.getName();

                result.add(ImmutableList.of(className, methodName));
                System.out.println(result);

                super.visit(expr, arg);
            }
        }, null);

        return result.stream().toList();
    }

    // 来点并发
    ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public List<JavaInvokeChain> getInvokedBy(JavaProject project, JavaMethod method, int depth) {
        if (depth == 0) {
            return null;
        }

        // 准备一个线程安全的缓存
        Map<ImmutableList<String>, JavaInvokeChain> chainMap = new ConcurrentHashMap<>();

        var result = new ArrayList<JavaInvokeChain>();
        var tasks = new ArrayList<Future<?>>();

        project.getClassMap().values().forEach(javaClass -> {
            javaClass.getDeclaration().accept(new VoidVisitorAdapter<List<JavaInvokeChain>>() {
                @Override
                public void visit(MethodCallExpr expr, List<JavaInvokeChain> resultArr) {
                    ResolvedMethodDeclaration methodDeclaration;
                    try {
                        methodDeclaration = expr.resolve();
                    } catch (Exception e) {
                        return;
                    }
                    var className = methodDeclaration.declaringType().getQualifiedName();
                    var methodName = methodDeclaration.getName();

                    if (Objects.equals(className, method.getClassReference()) && Objects.equals(methodName, method.getName())) {
                        // 找到了一个调用方

                        // 寻找其所位于的方法
                        var containingMethodOpt = expr.findAncestor(MethodDeclaration.class);
                        var containingClassOpt = expr.findAncestor(ClassOrInterfaceDeclaration.class);

                        if (containingMethodOpt.isEmpty() || containingClassOpt.isEmpty() || containingClassOpt.get().getFullyQualifiedName().isEmpty()) {
                            return;
                        }

                        var hostClassReference = containingClassOpt.get().getFullyQualifiedName().get();
                        var hostMethodName = containingMethodOpt.get().getNameAsString();

                        if (chainMap.containsKey(ImmutableList.of(hostClassReference, hostMethodName))) {
                            resultArr.add(chainMap.get(ImmutableList.of(hostClassReference, hostMethodName)));
                            return;
                        }

                        var javaMethod = project.getMethodMap().get(hostClassReference + "." + hostMethodName);
                        if (javaMethod == null) {
                            // 出现严重异常
                            log.warn("严重异常：{}类{}方法被遍历到，但不在预编译的索引中", hostClassReference, hostMethodName);
                            return;
                        }

                        var chain = new JavaInvokeChain();
                        chain.setMethod(javaMethod);

                        if (depth > 0) {
                            var task = executorService.submit(() -> chain.setInvokedBy(getInvokedBy(project, javaMethod, depth - 1)));
                            tasks.add(task);
                        }

                        resultArr.add(chain);
                    }
                }
            }, result);
        });

        // 等待遍历结束。
        // 注意，不可换为增强for，因为增强for的遍历内容是固定的。
        for (int i = 0; i < tasks.size(); i++) {
            try {
                tasks.get(i).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }
}
