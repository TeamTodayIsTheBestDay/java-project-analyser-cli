package team.todaybest.analyser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数来源
 *
 * @author cinea
 */
@Data
public class JavaParameterOrigin {
    private List<JavaValue> originChain;

    public JavaParameterOrigin(){
        originChain = new ArrayList<>();
    }
}
