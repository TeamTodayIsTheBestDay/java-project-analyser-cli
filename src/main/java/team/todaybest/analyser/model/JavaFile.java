package team.todaybest.analyser.model;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.Data;

import java.util.List;

/**
 * 文件的抽象
 *
 * @author cineazhan
 */
@Data
public class JavaFile {
    String fileName;
    CompilationUnit compilationUnit;
    List<ClassOrInterfaceDeclaration> declarations;
}
