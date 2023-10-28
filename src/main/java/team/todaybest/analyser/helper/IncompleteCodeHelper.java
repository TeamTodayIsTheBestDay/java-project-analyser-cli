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
    // 判断操作符
    private static boolean isBinaryOperator(char c) {
        if(c=='+'||c=='-'||c=='*'||c=='/')
            return true;
        return false;
    }


    private static boolean isValidStartCharacter(char c) {
        return Character.isAlphabetic(c) || Character.isDigit(c) || c == '(' || c == '[' || c == '{' || c == '\''||c=='"';
    }

    public static boolean reviewCode(String code, String path) {
        Stack<Character> stack = new Stack<>();
        boolean inSingleQuotes = false;//判断是否在单引号内部
        boolean inDoubleQuotes = false;//判断是否为双引号内部的字符串
        boolean inComment = false;//判断是否在块注释内
        boolean inLineComment = false;//判断是否在行注释内
        int flag = 0;//

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            if ((c == '\'' || c == '\"') && !inComment && !inLineComment) {  // 对引号要判断转义的可能性
                int continousBackslashNum = 0;
                while (i - continousBackslashNum - 1 >= 0 && code.charAt(i - continousBackslashNum - 1) == '\\') {
                    continousBackslashNum++;
                }
                if (continousBackslashNum % 2 == 0) {  // 前面有连续奇数个反斜杠是转义，不用管
                    if (c == '\'' && !inDoubleQuotes) {
                        inSingleQuotes = !inSingleQuotes;
                    } else if (c == '\"' && !inSingleQuotes) {
                        inDoubleQuotes = !inDoubleQuotes;
                    }
                } else if (!inSingleQuotes && !inDoubleQuotes) {  // 即出现了奇数个反斜杠且不在字符串内，在字符串内的无视
                    printReviewError(" impermissible\\", path);
                }
            } else if (i < code.length() - 1 && c == '/' && code.charAt(i + 1) == '*' && !inSingleQuotes && !inDoubleQuotes && !inComment && !inLineComment) {
                inComment = true;
                i++;
            } else if (i < code.length() - 1 && c == '*' && code.charAt(i + 1) == '/' && !inSingleQuotes && !inDoubleQuotes && inComment) {
                inComment = false;
                i++;
                flag=1;
            } else if (i < code.length() - 1 && c == '/' && code.charAt(i + 1) == '/' && !inSingleQuotes && !inDoubleQuotes && !inComment && !inLineComment) {
                inLineComment = true;
                i++;
            } else if (c == '\n' && inLineComment) {
                inLineComment = false;
            }

            // 不在注释、字符串内的括号判断
            if (!inSingleQuotes && !inDoubleQuotes && !inComment && !inLineComment) {
                if (c == '(' || c == '{' || c == '[') {
                    stack.push(c);
                } else if (c == ')' || c == '}' || c == ']') {
                    if (stack.isEmpty()) {
                        printReviewError("Bracket " + c + " does not find a match ", path);
                    }
                    char lastBracket = stack.pop();
                    if ((c == ')' && lastBracket != '(') || (c == '}' && lastBracket != '{') || (c == ']' && lastBracket != '[')) {
                        printReviewError(lastBracket + " does not match with" + c, path);
                    }
                }
                // 检查操作符后面的字符
                if (isBinaryOperator(c)) {
                    if(flag==1){
                        flag=0;
                    }
                    else {
                        if (code.charAt(i + 1) == '+'||code.charAt(i + 1) == '-'||code.charAt(i + 1) == '>')
                        {
                            i++;
                        }
                        else
                        {
                            if (code.charAt(i + 1) == '=') {
                                i++;
                            }
                            while (i < code.length() - 1 && Character.isWhitespace(code.charAt(i + 1))) {
                                i++;
                            }
                            if (i == code.length() - 1 || !isValidStartCharacter(code.charAt(i + 1))) {
                                printReviewError("The operand is incomplete", path);
                            }
                        }

                    }
                }
            }
        }




        if (!stack.isEmpty()) {
            StringBuilder str = new StringBuilder();
            while (!stack.isEmpty()) {
                str.append(stack.peek());
                str.append("  ");
                stack.pop();
            }
            printReviewError(str + "does not find a match", path);
        }

        return true;
    }

}
