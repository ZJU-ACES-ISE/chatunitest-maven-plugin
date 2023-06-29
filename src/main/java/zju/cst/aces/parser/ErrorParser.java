package zju.cst.aces.parser;

import zju.cst.aces.utils.TestMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author volunze
 * @Date 2023/6/26 12:37
 * @ClassName: ErrorParser
 * @Description: read error message from file
 * @Version 1.0
 */
public class ErrorParser {
    public static TestMessage loadMessage(List<String> msg) throws IOException {
        List<String> errorlines = new ArrayList<>();
        List<String> errorMessage = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        TestMessage testMessage=new TestMessage();
        for(String line:msg){
            if(line.contains("[ERROR]")){
                errorlines.add(line);
            }
        }
        for(String eline :errorlines){
            if(hasErrorFlag(eline,"COMPILATION ERROR")){
                testMessage.setErrorType(TestMessage.ErrorType.COMPILE_ERROR);
            }
            if(hasErrorFlag(eline,"RUNTIME ERROR")){
                testMessage.setErrorType(TestMessage.ErrorType.RUNTIME_ERROR);
            }
            if(eline.contains(".java")){
                testMessage.setUnitTest(getUnitTest(eline));
            }
            if(!eline.equals("[ERROR] ")){
                errorMessage.add(getErrorMessage(eline));
            }

            //sb.append(getErrorMessage(eline));
        }
        //testMessage.setErrorMessage(sb.toString());
        testMessage.setErrorMessage(errorMessage);
        return testMessage;
    }
    static boolean hasErrorFlag(String message,String type){
        String patternString = "(?i)" + Pattern.quote(type);
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(message);

        return matcher.find();

    }
    static String getUnitTest(String message){
        String testClass="";
        if(message.contains(".java")){
            testClass=message.substring(message.lastIndexOf("/") + 1,message.indexOf(":"));
        }
        return testClass;
    }

    static String getErrorMessage(String message){
        String errorMessage="";
        if(message.contains("[ERROR]")){
            errorMessage=message.replace("[ERROR]","");
            if(message.contains(".java")){
                errorMessage=message.substring(message.indexOf(".java")+1);
                //TODO: Add the information of error line and column
//                errorMessage=errorMessage.substring(errorMessage.indexOf("]")+2);
            }
        }
        return errorMessage.trim();
    }

}
