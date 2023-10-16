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
    private String fileName;
    private String packageName;
    private CompilationUnit compilationUnit;
    private List<JavaClass> classes;

    public JavaFile(){
        this.classes = new ArrayList<>();
    }
}
