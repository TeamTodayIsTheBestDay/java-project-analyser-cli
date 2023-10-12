package team.todaybest.analyser.component;

import com.github.javaparser.StaticJavaParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import team.todaybest.analyser.service.AnalyseService;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author cineazhan
 */
@ShellComponent("Analyse Application")
public class AnalyseShellComponent {

    AnalyseService analyseService;

    @Autowired
    public void setAnalyseService(AnalyseService analyseService) {
        this.analyseService = analyseService;
    }

    @ShellMethod("Open a project")
    public void open(String path){
        analyseService.openProject(path);
    }

    @ShellMethod("Test")
    void test() throws IOException {
        var unit = StaticJavaParser.parse(Path.of("/Users/cineazhan/codelab/53JavaEEProjectA/java-project-analyser-cli/src/main/java/team/todaybest/analyser/component/AnalyseShellComponent.java"));
        var classShellC = unit.getClassByName("AnalyseShellComponent");
    }

}
