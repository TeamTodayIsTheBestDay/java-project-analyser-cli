package team.todaybest.analyser.helper;

import team.todaybest.analyser.model.JavaPackage;
import team.todaybest.analyser.model.JavaProject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cinea
 */
public class ProjectListHelper {
    /**
     * 打印项目的包、文件和类
     */
    public static void listProject(JavaProject project) {
        listPackages(project.getPackages());
        listFiles(project.getPackages());
        listClassesAndInterfaces(project.getPackages());
    }

    private static void listPackages(List<JavaPackage> javaPackages) {
        var packages = traversePackageRecursive(javaPackages);

        System.out.printf("**** Packages (%d)%n", packages.size());
        packages.forEach(s -> System.out.printf("*  %s%n", s));
        System.out.println("********\n");
    }

    private static List<String> traversePackageRecursive(List<JavaPackage> javaPackages) {
        List<String> packages = new ArrayList<>();

        javaPackages.forEach(javaPackage -> {
            packages.add(String.valueOf(javaPackage.getPackageDeclaration().getName()));
            packages.addAll(traversePackageRecursive(javaPackage.getChildrenPackages()));
        });

        return packages;
    }

    private static void listFiles(List<JavaPackage> javaPackages) {
        var files = traverseFilesRecursive(javaPackages);

        System.out.printf("**** Files (%d)%n", files.size());
        files.forEach(s -> System.out.printf("*  %s%n", s));
        System.out.println("********\n");
    }

    private static List<String> traverseFilesRecursive(List<JavaPackage> javaPackages) {
        List<String> files = new ArrayList<>();

        javaPackages.forEach(javaPackage -> {
            javaPackage.getFiles().forEach(javaFile -> {
                files.add(
                        String.format("(%s) %s", javaPackage.getPackageDeclaration().getName(), javaFile.getFileName())
                );
            });
            files.addAll(traverseFilesRecursive(javaPackage.getChildrenPackages()));
        });

        return files;
    }

    private static void listClassesAndInterfaces(List<JavaPackage> javaPackages) {
        var classes = traverseClassAndInterfaceRecursive(javaPackages);

        System.out.printf("**** Classes and Interfaces (%d)%n", classes.size());
        classes.forEach(s -> System.out.printf("*  %s%n", s));
        System.out.println("********\n");
    }

    private static List<String> traverseClassAndInterfaceRecursive(List<JavaPackage> javaPackages) {
        List<String> classes = new ArrayList<>();

        javaPackages.forEach(javaPackage -> {
            javaPackage.getFiles().forEach(javaFile -> {
                javaFile.getClasses().forEach(declaration -> {
                    String type = "C";
                    if (declaration.getDeclaration().isInterface()) {
                        type = "I";
                    }

                    classes.add(
                            String.format("%s (%s) %s", type, javaPackage.getPackageDeclaration().getName(), declaration.getDeclaration().getName())
                    );
                });
            });
            classes.addAll(traverseClassAndInterfaceRecursive(javaPackage.getChildrenPackages()));
        });

        return classes;
    }
}
