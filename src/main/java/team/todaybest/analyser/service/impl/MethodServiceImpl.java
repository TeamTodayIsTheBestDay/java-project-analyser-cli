package team.todaybest.analyser.service.impl;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import team.todaybest.analyser.model.JavaInvokeChain;
import team.todaybest.analyser.model.JavaMethod;
import team.todaybest.analyser.model.JavaPackage;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.service.MethodService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author cinea
 */
@Slf4j
@Service
public class MethodServiceImpl implements MethodService {
    @Override
    public void makeMethodsTrie(JavaProject project) {
        var map = new PatriciaTrie<JavaMethod>();
        project.getPackages().forEach(javaPackage -> {
            traverseMethodsRecursive(javaPackage, javaMethod -> map.put(javaMethod.getQualifiedSignature(), javaMethod));
        });
        project.setMethodTrie(map);
    }

    @Override
    public List<String> getAllOverloads(JavaProject project, String classReference, String methodName) {
        return getAllOverloads(project, classReference + '.' + methodName);
    }

    @Override
    public List<String> getAllOverloads(JavaProject project, String methodReference) {
        var searchString = methodReference + '(';
        var searchResult = project.getMethodTrie().prefixMap(searchString);

        return new ArrayList<>(searchResult.keySet());
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

                super.visit(expr, arg);
            }
        }, null);

        return result.stream().toList();
    }

    @Override
    public List<JavaInvokeChain> getInvokedBy(JavaProject project, JavaMethod method, int depth) {
        if (depth == 0) {
            return null;
        }

        var result = new ArrayList<JavaInvokeChain>();
        var tasks = new ArrayList<Future<?>>();

        // 准备一个线程安全的缓存
        Map<String, JavaInvokeChain> chainMap = new ConcurrentHashMap<>();
        Set<String> visited = Sets.newConcurrentHashSet();

        project.getInvokedRelation().get(method.getQualifiedSignature()).forEach(expr -> {
            // 找到了一个调用方

            // 寻找其所位于的方法
            var containingMethodOpt = expr.findAncestor(MethodDeclaration.class);

            if (containingMethodOpt.isEmpty()) {
                return;
            }

            var hostMethodResolved = containingMethodOpt.get().resolve();
            var hostMethod = hostMethodResolved.declaringType().getId() + '.' + containingMethodOpt.get().getSignature().toString();

            // 去重
            if (visited.contains(hostMethod)) {
                return;
            } else {
                visited.add(hostMethod);
            }

            if (chainMap.containsKey(hostMethod)) {
                resultArr.add(chainMap.get(hostMethod));
                return;
            }

            var javaMethod = project.getMethodTrie().get(hostMethod);
            if (javaMethod == null) {
                // 出现严重异常
                log.warn("严重异常：{}方法被遍历到，但不在预编译的索引中", hostMethod);
                return;
            }

            var chain = new JavaInvokeChain();
            chain.setMethod(javaMethod);

            if (depth > 0) {
                chain.setInvokedBy(getInvokedBy(project, javaMethod, depth - 1));
                chainMap.put(hostMethod, chain);
            }

            resultArr.add(chain);
        });

        return result;
    }

    @Override
    public void getInvokedBy(JavaProject project, JavaMethod method, MethodCallExprOperation operation) {
        var tasks = new ArrayList<Future<?>>();

        project.getClassMap().values().forEach(javaClass -> {
            javaClass.getDeclaration().accept(new VoidVisitorAdapter<Object>() {
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

                    if (Objects.equals(className, method.getClassReference()) && Objects.equals(methodName, method.getName())) {
                        // 找到了一个调用方
                        var task = executorService.submit(() -> operation.operate(expr));
                        tasks.add(task);
                    }
                }
            }, null);
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
    }
}
