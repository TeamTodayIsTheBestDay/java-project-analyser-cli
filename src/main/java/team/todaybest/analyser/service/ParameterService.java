package team.todaybest.analyser.service;

import team.todaybest.analyser.model.JavaMethod;
import team.todaybest.analyser.model.JavaParameterOrigin;
import team.todaybest.analyser.model.JavaProject;

import java.util.List;

/**
 * 函数参数的相关服务类
 * @author cinea
 */
public interface ParameterService {
    /**
     * 为指定函数溯源，需要回溯整个调用链。
     * <br/>
     * 第一层List：不同的调用者
     * <br/>
     * 第二层List：不同的函数参数
     */
    List<List<JavaParameterOrigin>> getParameterOrigin(JavaProject project, JavaMethod method, int depth);
}
