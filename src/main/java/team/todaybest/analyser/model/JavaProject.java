package team.todaybest.analyser.model;

import com.github.javaparser.ast.PackageDeclaration;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Java项目的抽象描述
 *
 * @author cineazhan
 */
@Data
public class JavaProject {
    List<JavaPackage> packages;

    public JavaProject() {
        this.packages = new ArrayList<>();
    }
}
