package team.todaybest.analyser.model;

import com.github.javaparser.ast.expr.MethodCallExpr;
import lombok.Data;
import org.apache.commons.collections4.trie.PatriciaTrie;

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

    private Map<String, JavaClass> classMap;

    /**
     * 使用压缩前缀树来存储方法
     */
    private PatriciaTrie<JavaMethod> methodTrie;

    /**
     * （被）调用关系，String是被调用函数的签名
     */
    private Map<String,List<MethodCallExpr>> invokedRelation;

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
