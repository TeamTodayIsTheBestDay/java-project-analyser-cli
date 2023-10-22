package team.todaybest.analyser.shell;

import com.github.javaparser.StaticJavaParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import team.todaybest.analyser.service.AnalysisService;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author cineazhan
 */
@ShellComponent("Analyse Application")
public class AnalyseShellComponent {

    AnalysisService analysisService;

    @Autowired
    public void setAnalyseService(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @ShellMethod("Open a project")
    public void open(
            @ShellOption(help = "The path to the root package of the source code.") String path
    ) {
        analysisService.openProject(path);
    }

    @ShellMethod("List the packages, files, classes and interfaces of the project")
    public void list() {
        analysisService.listProject();
    }

    @ShellMethod("Get all instances of a specified class.")
    public void inst(
            @ShellOption(help = "The reference to the class.") String classReference
    ) {
        analysisService.getInstances(classReference);
    }

    @ShellMethod("Show invoke relationships of a specified function.")
    public void func(
            @ShellOption(help = "The reference to the class.") String classReference,
            @ShellOption(help = "The name of the function.") String funcName,
            @ShellOption(help = "The depth when searching for invoked relation.", defaultValue = "2") int depth
    ) {
        analysisService.methodRelationship(classReference, funcName, depth);
    }

    @ShellMethod("Show the parameter source chain of a specified function.")
    public void param(
            @ShellOption(help = "The reference to the class.") String classReference,
            @ShellOption(help = "The name of the function.") String funcName,
            @ShellOption(help = "The depth when searching for invoked relation.", defaultValue = "20") int depth
    ) {
        analysisService.methodParameterOrigin(classReference, funcName, depth);
    }

}
