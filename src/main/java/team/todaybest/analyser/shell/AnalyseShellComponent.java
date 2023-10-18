package team.todaybest.analyser.shell;

import com.github.javaparser.StaticJavaParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
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
    public void open(String path){
        analysisService.openProject(path);
    }

    @ShellMethod("List the packages, files, classes and interfaces of the project")
    public void list(){
        analysisService.listProject();
    }

    @ShellMethod("Get all instances of a specified class.")
    public void inst(String classReference){
        analysisService.getInstances(classReference);
    }

    @ShellMethod("Show invoke relationships of a specified function.")
    public void func(String classReference, String funcName){
    }

    @ShellMethod("Test")
    void testJp() throws IOException {
        var unit = StaticJavaParser.parse(Path.of("D:\\IdeaProjects\\java-project-analyser-cli\\src\\main\\java\\team\\todaybest\\analyser\\service\\impl\\LoadProjectServiceImpl.java"));
        var classShellC = unit.getClassByName("AnalyseShellComponent");
    }

}
