package zju.cst.aces.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeExtractor {
    private static JavaParser java_parser = new JavaParser();
    private boolean hasCode;
    private boolean hasSyntacticError;
    private String extractedCode;

    public CodeExtractor(String code) {
        extractedCode = extract(code);
    }

    public String extract(String code) {
        String extractedCode = "";

        // If the string is valid code, return true
        if (isSyntacticCorrect(code)) {
            hasCode = true;
            extractedCode = code;
            hasSyntacticError = false;
        } else {
            hasCode = false;
            hasSyntacticError = false;

            // Define regex pattern to match the code blocks
            Pattern pattern = Pattern.compile("```[java]*([\\s\\S]*?)```");
            Matcher matcher = pattern.matcher(code);

            // Find all matches in the text
            while (matcher.find()) {
                String match = matcher.group(1).trim();
                if (match.contains("@Test") && match.contains("class") && match.contains("import")) {
                    extractedCode= syntacticCheck(match);
                    hasSyntacticError = !match.equals(extractedCode);
                    if (!extractedCode.equals("")) {
                        hasCode = true;
                        break;
                    }
                }
            }

            if (!hasCode) {
                if (code.contains("```java")) {
                    String separateString = code.split("```java")[1];
                    if (separateString.contains("@Test")) {
                        extractedCode = syntacticCheck(separateString);
                        hasSyntacticError = !separateString.equals(extractedCode);
                        if (!extractedCode.equals("")) {
                            hasCode = true;
                        }
                    }
                } else if (code.contains("```")) {
                    String[] separateStrings = code.split("```");
                    for (String separateString : separateStrings) {
                        if (separateString.contains("@Test")) {
                            extractedCode = syntacticCheck(separateString);
                            hasSyntacticError = !separateString.equals(extractedCode);
                            if (!extractedCode.equals("")) {
                                hasCode = true;
                                break;
                            }
                        }
                    }
                } else {
                    // Define boundary
                    String[] allowed = {"import", "packages", "", "@"};
                    String[] codeLines = code.split("\\n");
                    int start = -1, anchor = -1, end = -1;
                    boolean[] allowedLines = new boolean[codeLines.length];
                    Map<Integer, Integer> leftBrace = new HashMap<>();
                    Map<Integer, Integer> rightBrace = new HashMap<>();

                    for (int i = 0; i < codeLines.length; i++) {
                        leftBrace.put(i, codeLines[i].length() - codeLines[i].replace("{", "").length());
                        rightBrace.put(i, codeLines[i].length() - codeLines[i].replace("}", "").length());

                        String stripedLine = codeLines[i].trim();
                        for (String allowStart : allowed) {
                            if (stripedLine.startsWith(allowStart)) {
                                allowedLines[i] = true;
                                break;
                            }
                        }

                        if (codeLines[i].matches(".*public class .*Test.*") && anchor == -1) {
                            anchor = i;
                        }
                    }

                    if (anchor != -1) {
                        start = anchor;
                        while (start > 0) {
                            if (allowedLines[start]) {
                                start -= 1;
                            } else {
                                break;
                            }
                        }

                        end = anchor;
                        int leftSum = 0, rightSum = 0;
                        while (end < codeLines.length) {
                            leftSum += leftBrace.get(end);
                            rightSum += rightBrace.get(end);
                            if (leftSum == rightSum && leftSum >= 1 && rightSum >= 1) {
                                break;
                            }
                            end += 1;
                        }

                        String tempCode = String.join("\n", Arrays.copyOfRange(codeLines, start, end + 1));
                        extractedCode = syntacticCheck(tempCode);
                        hasSyntacticError = !tempCode.equals(extractedCode);
                        if (!extractedCode.equals("")) {
                            hasCode = true;
                        }
                    }
                }
            }
        }

        extractedCode = extractedCode.trim();
//        System.out.println("Has Code: " + hasCode);
//        System.out.println("Extracted Code:\n" + extractedCode);
//        System.out.println("Has Syntactic Error: " + hasSyntacticError);
        return extractedCode;
    }

    private static boolean isSyntacticCorrect(String code) {
        ParseResult<CompilationUnit> parseResult = java_parser.parse(code);
        if (parseResult.isSuccessful()) {
            return true;
        }
        return false;
    }

    /**
     * Check and fix the syntax.
     */
    public static String syntacticCheck(String code) {
        if (isSyntacticCorrect(code)) {
            return code;
        } else {
            String[] stopPoints = {";", "}", "{", " "}; // Stop point
            for (int idx = code.length() - 1; idx >= 0; idx--) {
                if (contains(stopPoints, code.charAt(idx))) {
                    code = code.substring(0, idx + 1);
                    break;
                }
            }
            int leftBracket = countOccurrences(code, "{");
            int rightBracket = countOccurrences(code, "}");
            for (int idx = leftBracket - rightBracket; idx > 0; idx--) {
                code += "}\n";
            }

            if (isSyntacticCorrect(code)) {
                return code;
            }

            Pattern pattern = Pattern.compile("(?<=\\})[^\\}]+(?=@)");
            Matcher matcher = pattern.matcher(code);
            List<String> matches = new ArrayList<>();
            while (matcher.find()) {
                matches.add(matcher.group());
            }
            if (!matches.isEmpty()) {
                String lastMatch = matches.get(matches.size() - 1);
                int endIdx = code.lastIndexOf(lastMatch) + lastMatch.length();
                code = code.substring(0, endIdx).trim();
                int leftCount = countOccurrences(code, "{");
                int rightCount = countOccurrences(code, "}");
                for (int i = leftCount - rightCount; i > 0; i--) {
                    code += "\n}";
                }
            }
            if (isSyntacticCorrect(code)) {
                return code;
            } else {
                return "";
            }
        }
    }

    private static boolean contains(String[] arr, char target) {
        for (String c : arr) {
            if (c.charAt(0) == target) {
                return true;
            }
        }
        return false;
    }

    private static int countOccurrences(String str, String target) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(target, idx)) != -1) {
            count++;
            idx += target.length();
        }
        return count;
    }

    public boolean getHasCode() {
        return hasCode;
    }

    public boolean getHasSyntacticError() {
        return hasSyntacticError;
    }

    public String getExtractedCode() {
        if (extractedCode == null) {
            return "";
        }
        return extractedCode;
    }
}
