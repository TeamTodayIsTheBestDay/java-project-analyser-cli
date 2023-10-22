package team.todaybest.analyser.model;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Java类（和接口）
 * @author cinea
 */
@Data
public class JavaClass {
    private ClassOrInterfaceDeclaration declaration;
    private String classReference;
    private List<JavaMethod> methods;

    // 所在的文件
    private JavaFile javaFile;

    public JavaClass(){
        this.methods = new ArrayList<>();
    }
}
