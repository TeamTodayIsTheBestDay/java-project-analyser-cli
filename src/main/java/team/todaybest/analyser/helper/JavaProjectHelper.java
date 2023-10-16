package team.todaybest.analyser.helper;

import team.todaybest.analyser.model.JavaFile;
import team.todaybest.analyser.model.JavaPackage;

/**
 * JavaProject的相关工具
 *
 * @author cinea
 */
public class JavaProjectHelper {
    public static void traverseJavaFiles(JavaPackage javaPackage, TraverseJavaFilesAction traverseAction) {
        javaPackage.getFiles().forEach(traverseAction::action);
        javaPackage.getChildrenPackages().forEach(childPackage -> traverseJavaFiles(childPackage, traverseAction));
    }

    public interface TraverseJavaFilesAction {
        void action(JavaFile javaFile);
    }
}
