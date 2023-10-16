package team.todaybest.analyser.service;

import team.todaybest.analyser.model.JavaProject;

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
}
