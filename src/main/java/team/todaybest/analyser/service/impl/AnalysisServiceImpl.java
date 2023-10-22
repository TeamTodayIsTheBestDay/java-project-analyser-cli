package team.todaybest.analyser.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.helper.ProjectListHelper;
import team.todaybest.analyser.helper.ProjectStatisticsHelper;
import team.todaybest.analyser.helper.ShellHelper;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.service.AnalysisService;
import team.todaybest.analyser.service.ClassesService;
import team.todaybest.analyser.service.LoadProjectService;
import team.todaybest.analyser.service.MethodService;

import java.io.File;
import java.util.Objects;

/**
 * @author cineazhan
 */
@Service
public class AnalysisServiceImpl implements AnalysisService {

    LoadProjectService loadProjectService;

    @Autowired
    public void setLoadProjectService(LoadProjectService loadProjectService) {
        this.loadProjectService = loadProjectService;
    }

    MethodService methodService;

    @Autowired
    public void setMethodService(MethodService methodService) {
        this.methodService = methodService;
    }

    ClassesService classesService;

    @Autowired
    public void setClassesService(ClassesService classesService) {
        this.classesService = classesService;
    }

    JavaProject javaProject = null;

    @Override
    public void openProject(String path) {
        // 检查path是否合法
        var dir = new File(path);
        if (!dir.exists()) {
            System.err.println("Wrong path given: Not exists.");
            return;
        } else if (!dir.isDirectory()) {
            System.err.println("Wrong path given: Not a directory.");
            return;
        }

        var waitThread = new ShellHelper.PleaseWaitThread();
        waitThread.start();

        // 打开项目，顺便记个时
        var startTime = System.currentTimeMillis();
        javaProject = loadProjectService.loadProject(dir);
        var endTime = System.currentTimeMillis();

        var statics = ProjectStatisticsHelper.doStatistics(javaProject);

        waitThread.missionFinish = true;
        try {
            waitThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("\nOpen project success: ");
        System.out.printf("                       %d packages found.%n", statics.getPackageCount());
        System.out.printf("                       %d files found.%n", statics.getFileCount());
        System.out.printf("                       %d classes and interfaces found.%n", statics.getClassAndInterfaceCount());
        System.out.printf("                       %d methods found.%n", statics.getMethodsCount());
        System.out.printf("                       %dms used.%n", endTime - startTime);

        // 缓存项目的类，顺便记个时
        startTime = System.currentTimeMillis();
        methodService.makeMethodsMap(javaProject);
        classesService.makeClassesMap(javaProject);
        endTime = System.currentTimeMillis();
        System.out.printf("Successfully indexed methods and classes in %dms.%n", endTime - startTime);
    }

    @Override
    public void listProject() {
        if (javaProject == null) {
            System.err.println("You haven't opened a project yet. Use \"open <path>\" to open one.");
            return;
        }

        ProjectListHelper.listProject(javaProject);
    }

    @Override
    public void getInstances(String classReference) {
        if (javaProject == null) {
            System.err.println("You haven't opened a project yet. Use \"open <path>\" to open one.");
            return;
        }

        var startTime = System.currentTimeMillis();
        var result = classesService.searchAllInstance(classReference, javaProject);
        var endTime = System.currentTimeMillis();

        System.out.printf("%n%d instances found in %dms:%n", result.size(), endTime - startTime);
        result.forEach(classInstance -> {
            var classFlag = "Class";
            if (classInstance.getClassDeclaration().isInterface()) {
                classFlag = "Interface";
            }
            var methodFlag = "Field";
            if (classInstance.getMethodDeclaration() != null) {
                methodFlag = String.format("Method %s", classInstance.getMethodDeclaration().getNameAsString());
            }
            System.out.printf("* %s\t%s:\t%s %s:\t%s%n", classInstance.getPackageName(), classInstance.getFileName(), classFlag, methodFlag, classInstance.getName());
        });

    }

    @Override
    public void methodRelationship(String classReference, String methodName, int depth) {
        if (javaProject == null) {
            System.err.println("You haven't opened a project yet. Use \"open <path>\" to open one.");
            return;
        }

        var javaClass = javaProject.getClassMap().get(classReference);
        if (javaClass == null) {
            System.err.printf("Class '%s' is not found.%n", classReference);
            return;
        }

        var optMethod = javaClass.getMethods().stream().filter(javaMethod -> Objects.equals(javaMethod.getName(), methodName)).findFirst();
        if (optMethod.isEmpty()) {
            System.err.printf("Method '%s' is not found.%n", methodName);
            return;
        }

        methodService.getInvokes(javaProject, optMethod.get());
    }



}
