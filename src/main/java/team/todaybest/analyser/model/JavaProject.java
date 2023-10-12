package team.todaybest.analyser.model;

import com.github.javaparser.ast.PackageDeclaration;
import lombok.Data;

/**
 * Java项目的抽象描述
 *
 * @author cineazhan
 */
@Data
public class JavaProject {
    JavaPackage packages;
}
