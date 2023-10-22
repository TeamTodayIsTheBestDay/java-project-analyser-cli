package team.todaybest.analyser.service;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import team.todaybest.analyser.dto.ClassInstance;
import team.todaybest.analyser.model.JavaProject;

import java.util.List;

/**
 * 解析类的各种关系
 *
 * @author cinea
 */
public interface ClassesService {
    /**
     * 搜索某个类的所有实例
     * @param classPath 形如<code>team.todaybest.analyser.service.ClassesService</code>的类路径
     * @return 所有存在的类实例列表
     */
    List<ClassInstance> searchAllInstance(String classPath, JavaProject project);

    /**
     * 预缓存project的类
     */
    void makeClassesMap(JavaProject project);

}
