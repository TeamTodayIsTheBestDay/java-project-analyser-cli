package team.todaybest.analyser.service;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;

import java.util.Optional;

/**
 * 用于处理Import的服务还是很有必要的，因为经常需要根据一个名字获取它的完整包路径，以防重名。
 *
 * @author cinea
 */
public interface ImportService {
    /**
     * 获取某个标识符的完整包路径
     *
     * @param importDeclarations 编译单元的imports，除此以外请不要从其他地方拿
     * @param identifier         标识符
     */
    Optional<String> getReferenceFromName(NodeList<ImportDeclaration> importDeclarations, String identifier);

    /**
     * 某个Reference是否存在
     */
    boolean referenceExists(NodeList<ImportDeclaration> importDeclarations, String classReference);
}
