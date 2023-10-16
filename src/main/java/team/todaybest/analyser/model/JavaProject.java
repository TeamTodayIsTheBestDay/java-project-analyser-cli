package team.todaybest.analyser.model;

import com.github.javaparser.ast.PackageDeclaration;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Java项目的抽象描述
 *
 * @author cineazhan
 */
@Data
public class JavaProject {
    private List<JavaPackage> packages;

    private Map<String,JavaMethod> methodMap;

    public JavaProject() {
        this.packages = new ArrayList<>();
    }
}
