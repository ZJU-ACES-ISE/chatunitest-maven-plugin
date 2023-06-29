package zju.cst.aces.utils;

import zju.cst.aces.parser.ErrorParser;

import java.io.IOException;
import java.util.List;

/**
 * @Author volunze
 * @Date 2023/6/26 13:15
 * @ClassName: ProcessError
 * @Description: Delete unnecessary error messages
 * @Version 1.0
 */
public class ErrorProcesser {
    public static String processErrorMessage(List<String> msg, int allowedTokens) throws IOException {
        if(allowedTokens<=0)
            return "";
        ErrorParser errorParser = new ErrorParser();
        TestMessage testMessage = errorParser.loadMessage(msg);
        TokenCounter tokenCounter = new TokenCounter();
        int count=0;
        List<String> errors = testMessage.getErrorMessage();
        String errorMessage = String.join(",",errors);
        while(tokenCounter.countToken(errorMessage) > allowedTokens){
            if(errorMessage.length()>50){
                errorMessage=errorMessage.substring(0,errorMessage.length()-50);
            }else{
                break;
            }
        }
        return errorMessage;
    }

}
