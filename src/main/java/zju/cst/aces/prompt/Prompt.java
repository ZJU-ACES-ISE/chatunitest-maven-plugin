package zju.cst.aces.prompt;

import freemarker.template.TemplateException;
import zju.cst.aces.dto.PromptInfo;

import java.io.IOException;

public interface Prompt {
    String getUserPrompt(PromptInfo promptInfo) throws IOException;
    String getSystemPrompt(PromptInfo promptInfo) throws IOException, TemplateException;
}
