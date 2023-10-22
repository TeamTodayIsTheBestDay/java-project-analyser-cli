package team.todaybest.analyser.service;

import com.google.common.collect.ImmutableList;
import team.todaybest.analyser.model.JavaInvokeChain;
import team.todaybest.analyser.model.JavaMethod;
import team.todaybest.analyser.model.JavaProject;

import java.util.List;

/**
 * 和方法相关的服务
 *
 * @author cinea
 */
public interface MethodService {
    /**
     * 将读取出的项目的Methods缓存到一个Map中，提高后续速度。
     */
    void makeMethodsMap(JavaProject project);

    /**
     * 获取基函数所调用的函数们
     */
    List<ImmutableList<String>> getInvokes(JavaProject project, JavaMethod method);

    /**
     * 获取调用基函数的函数，需要递归寻找整个调用链。
     */
    List<JavaInvokeChain> getInvokedBy(JavaProject project, JavaMethod method, int depth);
}
