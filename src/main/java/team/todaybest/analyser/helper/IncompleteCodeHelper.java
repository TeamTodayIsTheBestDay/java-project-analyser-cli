package team.todaybest.analyser.helper;

import java.util.Stack;

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
        Stack<Character> stack = new Stack<>();
        boolean inSingleQuotes = false;//判断是否在单引号内部
        boolean inDoubleQuotes = false;//判断是否为双引号内部的字符串
        boolean inComment = false;
        boolean inLineComment = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            if ((c == '\'' || c == '\"') && !inComment && !inLineComment) {//对引号要判断转义的可能性
                int continousBackslashNum = 0;
                while (i - continousBackslashNum - 1 >= 0 && code.charAt(i - continousBackslashNum - 1) == '\\') {
                    continousBackslashNum++;
                }
                if (continousBackslashNum % 2 == 0) {//前面有连续奇数个反斜杠是转义，不用管
                    if (c == '\'' && !inDoubleQuotes) {
                        inSingleQuotes = !inSingleQuotes;
                    } else if (c == '\"' && !inSingleQuotes) {
                        inDoubleQuotes = !inDoubleQuotes;
                    }
                }
            } else if (i < code.length() - 1 && c == '/' && code.charAt(i + 1) == '*' && !inSingleQuotes && !inDoubleQuotes&&!inComment&&!inLineComment) {
                inComment = true;
                i++;
            } else if (i < code.length() - 1 && c == '*' && code.charAt(i + 1) == '/' && !inSingleQuotes && !inDoubleQuotes&&inComment) {
                inComment = false;
                i++;
            }else if (i < code.length() - 1 && c == '/' && code.charAt(i + 1) == '/' && !inSingleQuotes && !inDoubleQuotes && !inComment&&!inLineComment) {
                inLineComment = true;
                i++;
            } else if (c == '\n'&&inLineComment) {
                inLineComment = false;
            }


            if (!inSingleQuotes && !inDoubleQuotes && !inComment&&!inLineComment) {
                if (c == '(' || c == '{' || c == '[') {
                    stack.push(c);
                } else if (c == ')' || c == '}' || c == ']') {
                    if (stack.isEmpty()){
                        printReviewError("括号"+c+"找不到匹配项 ",path);
                    }
                    char lastBracket = stack.pop();
                    if ((c == ')' && lastBracket != '(') || (c == '}' && lastBracket != '{') || (c == ']' && lastBracket != '[')) {
                        printReviewError(lastBracket+"与"+c+"不匹配",path);

                    }
                }
            }

        }
        if(!stack.isEmpty()){
            String str="";
            while(!stack.isEmpty()){
                str+=stack.peek();
                str+="  ";
            }
            printReviewError(str+"找不到匹配项",path);
        }

        return true;
    }

}
