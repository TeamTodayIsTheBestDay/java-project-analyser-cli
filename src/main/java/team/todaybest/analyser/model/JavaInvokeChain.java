package team.todaybest.analyser.model;

import lombok.Data;

import java.util.List;

/**
 * 调用链
 *
 * @author cinea
 */
@Data
public class JavaInvokeChain {
    private JavaMethod method;
    private List<JavaInvokeChain> invokes;
}
