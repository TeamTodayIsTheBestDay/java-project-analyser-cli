package team.todaybest.analyser.service.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.model.JavaMethod;
import team.todaybest.analyser.model.JavaPackage;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.service.MethodService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * @author cinea
 */
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
                var methodDeclaration = expr.resolve();
                var className = methodDeclaration.declaringType().getQualifiedName();
                var methodName = methodDeclaration.getName();

                result.add(ImmutableList.of(className,methodName));

                super.visit(expr, arg);
            }
        }, null);

        return result.stream().toList();
    }
}
