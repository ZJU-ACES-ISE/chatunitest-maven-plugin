package zju.cst.aces.parser;

import zju.cst.aces.utils.TestMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author volunze
 * @date 2023/6/26 12:37
 * @className: ErrorParser
 * @description: read error message from file
 * @version 1.0
 */
public class ErrorParser {
    public static TestMessage loadMessage(List<String> msg) throws IOException {
        List<String>  lines = msg;
        List<String> errorlines = new ArrayList<>();
        List<String> errorMessage = new ArrayList<>();
        TestMessage testMessage=new TestMessage();
        StringBuffer errorMessageBuffer = new StringBuffer();
        boolean isErrorSection = false;
        for(String line:lines){
            if(line.contains("[ERROR]")){
                isErrorSection=true;
                errorMessageBuffer.append(line).append("\n");
                //TODO: Unused ErrorType
                if(hasErrorFlag(line,"COMPILATION ERROR")){
                    testMessage.setErrorType(TestMessage.ErrorType.COMPILE_ERROR);
                }else{
                    testMessage.setErrorType(TestMessage.ErrorType.RUNTIME_ERROR);
                }
            }else if(isErrorSection && !line.startsWith("[INFO]") && !line.startsWith("[WARNING]")){
                errorMessageBuffer.append(line).append("\n");
            }else if(isErrorSection && (line.startsWith("[INFO]") || line.startsWith("[WARNING]"))){
                isErrorSection=false;
                errorlines.add(errorMessageBuffer.toString());
                errorMessageBuffer.setLength(0);
            }
        }
        // 处理可能在文件末尾的错误信息
        if (isErrorSection) {
            errorlines.add(errorMessageBuffer.toString());
        }

        for (int i = 0; i < errorlines.size(); i++) {
            errorlines.set(i, getErrorMessage(errorlines.get(i)));
        }
        testMessage.setErrorMessage(errorlines);

        return testMessage;
    }
    static boolean hasErrorFlag(String message,String type){
        String patternString = "(?i)" + Pattern.quote(type);
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(message);

        return matcher.find();
    }
    static String getErrorMessage(String message){
        String errorMessage="";
        if(message.contains("[ERROR]")){
            errorMessage=message.replace("[ERROR]","");
        }
        return errorMessage.trim();
    }

}
