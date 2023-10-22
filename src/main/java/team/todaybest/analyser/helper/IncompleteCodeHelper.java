package team.todaybest.analyser.helper;

/**
 * 不完全代码的相关工具
 *
 * @author cinea
 */
public class IncompleteCodeHelper {

    private static void printReviewError(String message, String path) {
        System.err.printf("In file %s: %s%n", path, message);
    }

    public static boolean reviewCode(String code, String path) {
        return true;
    }

}
