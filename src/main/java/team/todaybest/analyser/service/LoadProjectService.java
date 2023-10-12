package team.todaybest.analyser.service;

import team.todaybest.analyser.model.JavaProject;

import java.io.File;

/**
 * @author cineazhan
 */
public interface LoadProjectService {
    /**
     * 从指定目录读取项目
     */
    JavaProject loadProject(File startDir);
}
