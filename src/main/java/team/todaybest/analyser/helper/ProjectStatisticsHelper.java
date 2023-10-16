package team.todaybest.analyser.helper;

import team.todaybest.analyser.dto.ProjectStatistics;
import team.todaybest.analyser.model.JavaPackage;
import team.todaybest.analyser.model.JavaProject;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 小工具，用于在导入之后统计项目的规模
 *
 * @author cinea
 */
public class ProjectStatisticsHelper {
    public static ProjectStatistics doStatistics(JavaProject project) {
        int packageCount = 0;
        int fileCount = 0;
        int classAndInterfaceCount = 0;
        int methodsCount = 0;

        // 统计包
        packageCount = countPackagesRecursive(project.getPackages());

        // 统计文件
        fileCount = countFilesRecursive(project.getPackages());

        // 统计声明数
        classAndInterfaceCount = countClassAndInterfaceRecursive(project.getPackages());

        // 统计方法数
        methodsCount = countMethodsRecursive(project.getPackages());

        return ProjectStatistics.builder()
                .packageCount(packageCount)
                .fileCount(fileCount)
                .classAndInterfaceCount(classAndInterfaceCount)
                .methodsCount(methodsCount)
                .build();
    }

    private static int countPackagesRecursive(List<JavaPackage> packages) {
        AtomicInteger result = new AtomicInteger(packages.size());

        packages.forEach(javaPackage -> {
            result.addAndGet(countPackagesRecursive(javaPackage.getChildrenPackages()));
        });

        return result.get();
    }

    private static int countFilesRecursive(List<JavaPackage> packages) {
        AtomicInteger result = new AtomicInteger(0);

        packages.forEach(javaPackage -> {
            result.addAndGet(javaPackage.getFiles().size());
            result.addAndGet(countFilesRecursive(javaPackage.getChildrenPackages()));
        });

        return result.get();
    }

    private static int countClassAndInterfaceRecursive(List<JavaPackage> packages) {
        AtomicInteger result = new AtomicInteger(0);

        packages.forEach(javaPackage -> {
            javaPackage.getFiles().forEach(javaFile -> {
                result.addAndGet(javaFile.getClasses().size());
            });
            result.addAndGet(countClassAndInterfaceRecursive(javaPackage.getChildrenPackages()));
        });

        return result.get();
    }

    private static int countMethodsRecursive(List<JavaPackage> packages) {
        AtomicInteger result = new AtomicInteger(0);

        packages.forEach(javaPackage -> {
            javaPackage.getFiles().forEach(javaFile -> {
                javaFile.getClasses().forEach(javaClass -> {
                    result.addAndGet(javaClass.getMethods().size());
                });
            });
            result.addAndGet(countMethodsRecursive(javaPackage.getChildrenPackages()));
        });

        return result.get();
    }
}
