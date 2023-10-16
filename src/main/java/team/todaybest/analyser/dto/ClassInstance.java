package team.todaybest.analyser.dto;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import lombok.Builder;
import lombok.Data;

/**
 * 特定类的实例
 *
 * @author cinea
 */
@Data
@Builder
public class ClassInstance {
    /**
     * 所属包
     */
    String packageName;

    /**
     * 所属文件
     */
    String fileName;

    /**
     * <p>所位于的类</p>
     */
    ClassOrInterfaceDeclaration classDeclaration;

    /**
     * <p>所位于的函数</p>
     */
    MethodDeclaration methodDeclaration;

    /**
     * <p>所位于的代码块</p>
     * <p>若为null，则表示对整个类生效</p>
     */
    BlockStmt blockStatement;

    /**
     * 名字
     */
    String name;
}
