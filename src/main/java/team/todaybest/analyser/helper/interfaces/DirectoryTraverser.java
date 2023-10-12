package team.todaybest.analyser.helper.interfaces;

import java.io.File;

/**
 * 使用遍历工具时，需要实现的接口。
 * @author cineazhan
 */
public interface DirectoryTraverser {
    void process(File file);
}
