package team.todaybest.analyser.model;

import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.Data;

/**
 * 方法（和定义）
 * @author cinea
 */
@Data
public class JavaMethod {
    private MethodDeclaration declaration;

    /**
     * 所在的类
     */
    private String classReference;
    private JavaClass javaClass;

    /**
     * 返回的类
     */
    private String returnClassReference;
    private String name;

    @Override
    public String toString() {
        return "JavaMethod{" +
                "classReference='" + classReference + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
