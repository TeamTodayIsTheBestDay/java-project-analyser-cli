package team.todaybest.analyser.dto;

import com.google.common.collect.ImmutableList;
import lombok.Data;
import team.todaybest.analyser.model.JavaMethod;

import java.util.List;

/**
 * 函数之间的关系
 *
 * @author cinea
 */
@Data
public class MethodRelation {
    private JavaMethod baseFunc;

    /**
     * 格式：ImmutableList，其中第一项是类，第二项是方法名。
     */
    private List<ImmutableList<String>> invokedBy;

    /**
     * 格式：ImmutableList，其中第一项是类，第二项是方法名。
     */
    private List<ImmutableList<String>> invoked;
}
