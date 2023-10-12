package team.todaybest.analyser.helper;

import java.io.File;
import java.util.Objects;

/**
 * 文件系统工具类🔧
 *
 * @author cineazhan
 */
public class FileSystemHelper {
    /**
     * 遍历目录下的文件
     *
     * @param startDir 起点
     */
    public static void traverseDirectories(File startDir, DirectoryTraverser traverser) {
        for (var file : Objects.requireNonNull(startDir.listFiles())) {
            if (file.isFile()) {
                traverser.process(file);
            } else if (file.isDirectory()) {
                traverseDirectories(file, traverser);
            }
        }
    }
}
