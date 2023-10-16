package team.todaybest.analyser.service.impl;

import org.springframework.stereotype.Service;
import team.todaybest.analyser.model.JavaMethod;
import team.todaybest.analyser.model.JavaPackage;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.service.MethodService;

import java.util.HashMap;

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
}
