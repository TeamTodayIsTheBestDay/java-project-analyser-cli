package team.todaybest.analyser.service.impl;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserParameterDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserVariableDeclaration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.model.JavaMethod;
import team.todaybest.analyser.model.JavaParameterOrigin;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.model.JavaValue;
import team.todaybest.analyser.service.MethodService;
import team.todaybest.analyser.service.ParameterService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * @author cinea
 */
@Slf4j
@Service
public class ParameterServiceImpl implements ParameterService {

    MethodService methodService;

    @Autowired
    public void setMethodService(MethodService methodService) {
        this.methodService = methodService;
    }

    @Override
    public List<List<JavaParameterOrigin>> getParameterOrigin(JavaProject project, JavaMethod method, int depth) {
        List<List<JavaParameterOrigin>> result = new ArrayList<>();

        // 找寻调用的函数的地方
        methodService.getInvokedBy(project, method, expr -> {
            List<JavaParameterOrigin> parameterOrigins = new ArrayList<>();

            expr.getArguments().forEach(expression -> {
                if (expression instanceof NameExpr nameExpr) {
                    // 变量，复杂情况
                    parameterOrigins.add(traceParameterOrigin(nameExpr, project, depth));
                } else {
                    // 其他情况，不继续追踪，直接给出代码中的值
                    parameterOrigins.add(traceParameterOrigin(expression, method));
                }
            });

            result.add(parameterOrigins);
        });

        return result;
    }

    private JavaParameterOrigin traceParameterOrigin(Expression expr, JavaMethod javaMethod) {
        var origin = new JavaParameterOrigin();
        var value = new JavaValue();

        value.setInstant(true);
        value.setName(expr.toString());
        value.setMethod(javaMethod);
        origin.getOriginChain().add(value);

        return origin;
    }

    private JavaParameterOrigin traceParameterOrigin(NameExpr expr, JavaProject javaProject, int depth) {
        var origin = new JavaParameterOrigin();

        traceParameterOriginRecursive(expr, javaProject, depth, origin.getOriginChain());

        return origin;
    }

    private void traceParameterOriginRecursive(MethodCallExpr expr, int argumentIndex, JavaProject project, int depth, List<JavaValue> chain) {
        if (argumentIndex >= expr.getArguments().size()) {
            return; // 遇到重载了，不考虑了
        }
        var argument = expr.getArguments().get(argumentIndex);
        if (argument instanceof NameExpr nameExpr) {
            // 变量，复杂情况
            traceParameterOriginRecursive(nameExpr, project, depth - 1, chain);
        } else {
            // 其他情况，不继续追踪，直接给出代码中的值
            var value = new JavaValue();
            var javaMethod = getMethodFromAstNode(expr, project);
            if (javaMethod.isEmpty()) {
                return; // 没必要继续了
            }

            value.setInstant(true);
            value.setName(argument.toString());
            value.setMethod(javaMethod.get());
            chain.add(value);
        }
    }

    private void traceParameterOriginRecursive(NameExpr expr, JavaProject javaProject, int depth, List<JavaValue> chain) {
        var value = new JavaValue();

        var javaMethod = getMethodFromAstNode(expr, javaProject);
        if (javaMethod.isEmpty()) {
            return; // 没必要继续了
        }

        // 解析！
        ResolvedValueDeclaration resolvedValue = expr.resolve();

        if (resolvedValue instanceof JavaParserVariableDeclaration variableDeclaration) {
            // 如果它是一个局部变量或参数
            String variableName = variableDeclaration.getName();

            // 获取NameExpr的父BlockStmt或MethodDeclaration
            Optional<BlockStmt> scopeNodeOpt = expr.findAncestor(BlockStmt.class);

            scopeNodeOpt.ifPresent(scopeNode -> {
                JavaValue candidate = null;

                // 先找初始化
                var variableDeclarator = scopeNode.findAll(VariableDeclarator.class).stream().filter(vd -> vd.getNameAsString().equals(variableName)).findFirst();
                if (variableDeclarator.isPresent()) {
                    candidate = new JavaValue();
                    candidate.setMethod(javaMethod.get());
                    var val = "null";
                    if (variableDeclarator.get().getInitializer().isPresent()) {
                        val = variableDeclarator.get().getInitializer().get().toString();
                    }
                    candidate.setName(val);
                    candidate.setInstant(true);
                }

                // 再找赋值表达式
                var assignments = scopeNode.findAll(AssignExpr.class);
                for (int i = assignments.size() - 1; i >= 0; i--) {
                    var assignment = assignments.get(i);
                    if (assignment.getTarget().asNameExpr().getNameAsString().equals(variableName)) {
                        candidate = new JavaValue();
                        candidate.setMethod(javaMethod.get());
                        candidate.setInstant(true);
                        candidate.setName(assignment.getValue().toString());
                        break;
                    }
                }

                if (candidate != null) {
                    chain.add(candidate);
                }
            });
        } else if (resolvedValue instanceof JavaParserParameterDeclaration parameterDeclaration) {
            // 如果它是一个函数参数
            var parameter = parameterDeclaration.asParameter();
            if (parameter.toAst().isEmpty() || parameter.toAst().get().getParentNode().isEmpty()) {
                return; // 保护
            }
            var method = (MethodDeclaration) parameter.toAst().get().getParentNode().get();

            int paramIndex;
            List<Parameter> params = method.getParameters();
            paramIndex = IntStream.range(0, params.size()).filter(i -> Objects.equals(params.get(i).getNameAsString(), parameter.getName())).findFirst().orElse(-1);
            if (paramIndex == -1) {
                return; // 没必要继续了
            }

            value.setMethod(javaMethod.get());
            value.setInstant(false);
            value.setName(expr.getNameAsString());
            chain.add(value);

            if (depth > 1) {
                // 继续遍历
                methodService.getInvokedBy(javaProject, javaMethod.get(), subExpr -> {
                    traceParameterOriginRecursive(subExpr, paramIndex, javaProject, depth, chain);
                });
            }
        }

    }

    private Optional<JavaMethod> getMethodFromAstNode(Node node, JavaProject javaProject) {
        var optMethod = node.findAncestor(MethodDeclaration.class);
        var optClass = node.findAncestor(ClassOrInterfaceDeclaration.class);
        if (optMethod.isEmpty() || optClass.isEmpty()) {
            return Optional.empty();
        }

        if (optClass.get().getFullyQualifiedName().isEmpty()) {
            return Optional.empty();
        }

        var javaMethod = javaProject.getMethodMap().get(optClass.get().getFullyQualifiedName().get() + "." + optMethod.get().getName());
        return Optional.ofNullable(javaMethod);
    }
}

