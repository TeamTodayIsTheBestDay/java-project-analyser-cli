package team.todaybest.analyser.model;

import lombok.Data;

/**
 * 值，可以是变量也可以是立即数，在溯源中用的
 *
 * @author cinea
 */
@Data
public class JavaValue {
    private JavaMethod method;

    private boolean isInstant;

    /**
     * 如果是立即数，那这里就是toString的产物；如果这里是变量，那就是变量名。
     */
    private String name;
}
