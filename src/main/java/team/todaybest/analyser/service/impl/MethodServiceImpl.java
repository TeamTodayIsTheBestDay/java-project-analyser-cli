package team.todaybest.analyser.service.impl;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.model.JavaInvokeChain;
import team.todaybest.analyser.model.JavaMethod;
import team.todaybest.analyser.model.JavaPackage;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.service.MethodService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    public List<JavaInvokeChain> getInvokes(JavaProject project, JavaMethod method, int depth) {
        var result = new ArrayList<JavaInvokeChain>();

        Set<String> visited = Sets.newConcurrentHashSet();

        // 遍历，寻找函数调用
        method.getDeclaration().accept(new VoidVisitorAdapter<>() {
            @Override
            public void visit(MethodCallExpr expr, Object arg) {
                var resolved = expr.resolve();
                var javaMethod = project.getMethodTrie().get(resolved.getQualifiedSignature());

                var chain = new JavaInvokeChain();

                if (javaMethod == null) {
                    var newMethod = new JavaMethod();
                    newMethod.setQualifiedSignature(resolved.getQualifiedSignature());

                    chain.setMethod(newMethod);
                    result.add(chain);
                } else {
                    chain.setMethod(javaMethod);

                    if (depth > 0) {
                        chain.setInvokes(getInvokes(project, javaMethod, depth - 1));
                    } else {
                        chain.setInvokes(null);
                    }

                    result.add(chain);
                }
            }
        }, null);

        return result;
    }

    @Override
    public List<JavaInvokeChain> getInvokedBy(JavaProject project, JavaMethod method, int depth) {
        if (depth == 0) {
            return null;
        }

        var result = new ArrayList<JavaInvokeChain>();

        // 准备一个线程安全的缓存
        Map<String, JavaInvokeChain> chainMap = new ConcurrentHashMap<>();
        Set<String> visited = Sets.newConcurrentHashSet();

        project.getInvokedRelation().getOrDefault(method.getQualifiedSignature(), new ArrayList<>()).forEach(expr -> {
            // 找到了一个调用方

            // 寻找其所位于的方法
            var containingMethodOpt = expr.findAncestor(MethodDeclaration.class);

            if (containingMethodOpt.isEmpty()) {
                return;
            }

            var hostMethodResolved = containingMethodOpt.get().resolve();
//            var hostMethod = hostMethodResolved.declaringType().getId() + '.' + containingMethodOpt.get().getSignature().toString();
            var hostMethod = hostMethodResolved.getQualifiedSignature();

            // 去重
            if (visited.contains(hostMethod)) {
                return;
            } else {
                visited.add(hostMethod);
            }

            if (chainMap.containsKey(hostMethod)) {
                result.add(chainMap.get(hostMethod));
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
                chain.setInvokes(getInvokedBy(project, javaMethod, depth - 1));
                chainMap.put(hostMethod, chain);
            }

            result.add(chain);
        });

        return result;
    }

    @Override
    public void getInvokedBy(JavaProject project, JavaMethod method, MethodCallExprOperation operation) {
        project.getInvokedRelation().getOrDefault(method.getQualifiedSignature(), new ArrayList<>()).forEach(operation::operate);
    }
}
