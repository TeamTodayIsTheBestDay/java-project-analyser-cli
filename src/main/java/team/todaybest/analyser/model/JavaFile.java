package team.todaybest.analyser.model;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件的抽象
 *
 * @author cineazhan
 */
@Data
public class JavaFile {
    String fileName;
    String packageName;
    CompilationUnit compilationUnit;
    List<ClassOrInterfaceDeclaration> declarations;

    public JavaFile(){
        this.declarations = new ArrayList<>();
    }
}
