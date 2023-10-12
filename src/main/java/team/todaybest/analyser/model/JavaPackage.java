package team.todaybest.analyser.model;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import lombok.Data;

import java.util.List;

/**
 * Java Package的抽象描述
 *
 * @author cineazhan
 */
@Data
public class JavaPackage {
    PackageDeclaration packageDeclaration;
    List<JavaFile> files;
}
