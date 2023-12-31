package team.todaybest.analyser.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.helper.ProjectListHelper;
import team.todaybest.analyser.helper.ProjectStatisticsHelper;
import team.todaybest.analyser.helper.ShellHelper;
import team.todaybest.analyser.model.JavaInvokeChain;
import team.todaybest.analyser.model.JavaMethod;
import team.todaybest.analyser.model.JavaParameterOrigin;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.service.*;

import java.io.File;
import java.util.List;

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

        // 打开项目，顺便记个时
        var startTime = System.currentTimeMillis();
        javaProject = loadProjectService.loadProject(dir);

        methodService.makeMethodsTrie(javaProject);
        classesService.makeClassesMap(javaProject);

        loadProjectService.makeInvokeIndex(javaProject);
        var endTime = System.currentTimeMillis();

        var statics = ProjectStatisticsHelper.doStatistics(javaProject);

        System.out.println("\nOpen project success: ");
        System.out.printf("                       %d packages found.%n", statics.getPackageCount());
        System.out.printf("                       %d files found.%n", statics.getFileCount());
        System.out.printf("                       %d classes and interfaces found.%n", statics.getClassAndInterfaceCount());
        System.out.printf("                       %d methods found.%n", statics.getMethodsCount());
        System.out.printf("                       %dms used.%n", endTime - startTime);
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
            System.out.printf("* %s\t%s:\t%s %s:\t%s%n",
                    classInstance.getPackageName(), classInstance.getFileName(), classFlag, methodFlag, classInstance.getName());
        });

    }

    @Override
    public void methodRelationship(String classReference, String methodName, int depth) {
        JavaMethod javaMethod = methodPreprocess(classReference, methodName, depth);
        if (javaMethod == null)
            return;

        // 打开友好提示
        var waitThread = new ShellHelper.PleaseWaitThread();
        waitThread.start();

        long startTime, endTime;

        startTime = System.currentTimeMillis();
        var invokes = methodService.getInvokes(javaProject, javaMethod, depth);
        endTime = System.currentTimeMillis();
        var getInvokesTime = endTime - startTime;

        startTime = System.currentTimeMillis();
        var invoked = methodService.getInvokedBy(javaProject, javaMethod, depth);
        endTime = System.currentTimeMillis();
        var getInvokedByTime = endTime - startTime;

        // 关闭友好提示
        waitThread.missionFinish = true;
        try {
            waitThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        printInvokes(invokes, true);
        printInvokes(invoked, false);

        System.out.printf("%nTime cost:%n\t\tGet Invokes: %d ms%n\t\tGet Invoked: %d ms%n", getInvokesTime, getInvokedByTime);
    }

    private void printInvokes(List<JavaInvokeChain> invokes, boolean direction) {
        if (direction) {
            System.out.println("******************** Invokes *******************");
        } else {
            System.out.println("****************** Invoked by ******************");
        }
        if (invokes != null) {
            invokes.forEach(invokedChain -> {
                printInvokesRecursive(invokedChain, 0, direction);
            });
        }
        System.out.println("************************************************");
        System.out.println();
    }

    private void printInvokesRecursive(JavaInvokeChain invokes, int depth, boolean direction) {
        if (invokes == null) {
            return;
        }

        String directionStr = direction ? "->" : "<-";

        System.out.printf("%s%s %s%n", "\t".repeat(depth * 2 + 1), directionStr,
                invokes.getMethod().getQualifiedSignature()
        );

        var subInvoked = invokes.getInvokes();
        if (subInvoked != null) {
            subInvoked.forEach(invokedChain -> {
                printInvokesRecursive(invokedChain, depth + 1, direction);
            });
        }
    }

    ParameterService parameterService;

    @Autowired
    public void setParameterService(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    @Override
    public void methodParameterOrigin(String classReference, String methodName, int depth) {
        JavaMethod javaMethod = methodPreprocess(classReference, methodName, depth);
        if (javaMethod == null)
            return;

        // 打开友好提示
        var waitThread = new ShellHelper.PleaseWaitThread();
        waitThread.start();

        long startTime, endTime;

        startTime = System.currentTimeMillis();
        var result = parameterService.getParameterOrigin(javaProject, javaMethod, depth);
        endTime = System.currentTimeMillis();

        // 关闭友好提示
        waitThread.missionFinish = true;
        try {
            waitThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        printParameterOrigin(result, javaMethod);
        System.out.printf("%nTime cost: %d ms%n", endTime - startTime);
    }

    private JavaMethod methodPreprocess(String classReference, String methodName, int depth) {
        if (javaProject == null) {
            System.err.println("You haven't opened a project yet. Use \"open <path>\" to open one.");
            return null;
        }

        JavaMethod javaMethod;
        var candidates = methodService.getAllOverloads(javaProject, classReference, methodName);
        if (candidates.isEmpty()) {
            System.err.println("No method found with specific class and name.");
            return null;
        } else if (candidates.size() == 1) {
            javaMethod = javaProject.getMethodTrie().get(candidates.get(0));
        } else {
            var k = ShellHelper.consoleChooseOne(candidates);
            javaMethod = javaProject.getMethodTrie().get(candidates.get(k));
        }

        if (depth > 4) {
            System.err.println("Warning: you give a depth of more than 4. This may cost very very lots of time!");
        }

        return javaMethod;
    }

    private void printParameterOrigin(List<List<JavaParameterOrigin>> origin, JavaMethod javaMethod) {
        var parameters = javaMethod.getDeclaration().getParameters();

        System.out.println("********************Parameter Origin********************");

        for (int j = 0; j < parameters.size(); j++) {
            System.out.printf("\t%s %s:%n", parameters.get(j).getType().asString(), parameters.get(j).getNameAsString());
            for (List<JavaParameterOrigin> javaParameterOrigins : origin) {

                var parameterOrigin = javaParameterOrigins.get(j);
                var valueChain = parameterOrigin.getOriginChain();

//                System.out.println("\t\t\t[");

                System.out.print("\t\t\t  ");

                for (int k = 0; k < valueChain.size(); k++) {
                    if (k > 0) {
                        System.out.print("\t\t\t\t <- ");
                    }

                    var value = valueChain.get(k);
                    System.out.printf("%s: (%s) %s%n", value.getName(), value.getMethod().getClassReference(), value.getMethod().getName());
                }

//                System.out.println("\t\t\t]");

            }
        }

        System.out.println("********************************************************");
    }
}
