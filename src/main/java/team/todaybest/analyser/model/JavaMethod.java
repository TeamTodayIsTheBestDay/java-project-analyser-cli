package team.todaybest.analyser.model;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.base.Objects;
import lombok.Data;

import java.util.List;

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
     * 完整签名
     */
    private String qualifiedSignature;


    private String name;

    @Override
    public String toString() {
        return "JavaMethod{" +
                "classReference='" + classReference + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaMethod that = (JavaMethod) o;
        return Objects.equal(qualifiedSignature, that.qualifiedSignature);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(qualifiedSignature);
    }
}
