package zju.cst.aces.prompt;

import freemarker.template.TemplateException;
import zju.cst.aces.dto.PromptInfo;

import java.io.IOException;

public interface Prompt {
    // TODO: 完成AbstractRunner.generateUserPrompt的功能
    String getUserPrompt(PromptInfo promptInfo) throws IOException;
    // TODO: 完成AbstractRunner.generateSystemPrompt的功能
    String getSystemPrompt(PromptInfo promptInfo) throws IOException, TemplateException;
}
