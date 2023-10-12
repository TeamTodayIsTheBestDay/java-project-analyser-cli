package team.todaybest.analyser.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.helper.ProjectListHelper;
import team.todaybest.analyser.helper.ProjectStatisticsHelper;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.service.AnalyseService;
import team.todaybest.analyser.service.LoadProjectService;

import java.io.File;

/**
 * @author cineazhan
 */
@Service
public class AnalyseServiceImpl implements AnalyseService {

    LoadProjectService loadProjectService;

    @Autowired
    public void setLoadProjectService(LoadProjectService loadProjectService) {
        this.loadProjectService = loadProjectService;
    }

    JavaProject javaProject = null;

    @Override
    public void openProject(String path) {
        // 检查path是否合法
        var dir = new File(path);
        if (!dir.exists()) {
            System.err.println("Wrong path given: Not exists.");
            return;
        } else if (!dir.isDirectory()) {
            System.err.println("Wrong path given: Not a directory.");
            return;
        }

        // 打开项目，顺便记个时
        var startTime = System.currentTimeMillis();
        javaProject = loadProjectService.loadProject(dir);
        var endTime = System.currentTimeMillis();

        var statics = ProjectStatisticsHelper.doStatistics(javaProject);
        System.out.println("Open project success: ");
        System.out.printf("                       %d packages found.%n", statics.getPackageCount());
        System.out.printf("                       %d files found.%n", statics.getFileCount());
        System.out.printf("                       %d classes and interfaces found.%n", statics.getClassAndInterfaceCount());
        System.out.printf("                       %dms used.%n", endTime - startTime);
    }

    @Override
    public void listProject() {
        if(javaProject==null){
            System.err.println("You haven't opened a project yet. Use \"open <path>\" to open one.");
            return;
        }

        ProjectListHelper.listProject(javaProject);
    }

}
