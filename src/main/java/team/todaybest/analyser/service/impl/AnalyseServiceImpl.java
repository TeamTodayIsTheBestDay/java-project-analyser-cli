package team.todaybest.analyser.service.impl;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.model.JavaProject;
import team.todaybest.analyser.service.AnalyseService;

import java.io.File;
import java.nio.file.Path;

/**
 * @author cineazhan
 */
@Service
public class AnalyseServiceImpl implements AnalyseService {

    JavaProject javaProject = null;

    @Override
    public void openProject(String path) {
        // 检查path是否合法
        var dir = new File(path);
        if(!dir.exists()) {
            System.err.println("Wrong path given: Not exists.");
            return;
        }




    }
}
