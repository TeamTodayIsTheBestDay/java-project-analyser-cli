package team.todaybest.analyser.model;

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

    private Map<String, JavaMethod> methodMap;

    private Map<String, JavaClass> classMap;

    public JavaProject() {
        this.packages = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "JavaProject{" +
                "classes(" + classMap.size() + ")" +
                '}';
    }
}
