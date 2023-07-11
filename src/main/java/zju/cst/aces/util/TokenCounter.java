package zju.cst.aces.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;

/**
 * @Author volunze
 * @Date 2023/6/26 1:20
 * @ClassName: CountToken
 * @Description: count the number of tokens for openai models
 * @Version 1.0
 */
public class TokenCounter {

    public TokenCounter() {
    }

    public static int countToken(String error_message){
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        // Get encoding for a specific model via string name
        Encoding encoding = registry.getEncodingForModel(ModelType.GPT_3_5_TURBO);
        int tokenCount = encoding.countTokens(error_message);
        return tokenCount;
    }
}
