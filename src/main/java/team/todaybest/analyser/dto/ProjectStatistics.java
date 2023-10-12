package team.todaybest.analyser.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 项目统计的结果
 * @author cinea
 */
@Data
@Builder
public class ProjectStatistics {
    private int packageCount;
    private int fileCount;
    private int classAndInterfaceCount;
}
