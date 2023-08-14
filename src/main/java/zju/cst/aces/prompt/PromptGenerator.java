package zju.cst.aces.prompt;

import zju.cst.aces.config.Config;
import zju.cst.aces.dto.PromptInfo;
import zju.cst.aces.dto.TestMessage;
import zju.cst.aces.util.TokenCounter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//该类方法于AbstratRunner中调用
public class PromptGenerator implements Prompt {
    PromptTemplate promptTemplate = new PromptTemplate();
    public Config config;

    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public String getUserPrompt(PromptInfo promptInfo) {
        try {
            promptTemplate.readProperties();
            String userPrompt = null;
            Map<String, String> cdep_temp = new HashMap<>();
            Map<String, String> mdep_temp = new HashMap<>();
            // round 1
            if (promptInfo.errorMsg == null) {
                promptTemplate.dataModel.put("focal_method", promptInfo.getMethodSignature());
                promptTemplate.dataModel.put("class_name", promptInfo.getClassName());
                promptTemplate.dataModel.put("information", promptInfo.getInfo());
                if (!promptInfo.getOtherMethods().trim().isEmpty()) {
                    //≈ d1
                    promptTemplate.dataModel.put("other_methods", promptInfo.getOtherMethods());

                }else {
                    promptTemplate.dataModel.put("other_methods", null);
                }
                userPrompt = promptTemplate.renderTemplate(promptTemplate.TEMPLATE_NO_DEPS);
                if (promptInfo.hasDep) {
                    for (Map<String, String> cDeps : promptInfo.getConstructorDeps()) {
                        for (Map.Entry<String, String> entry : cDeps.entrySet()) {
                            cdep_temp.put(entry.getKey(), entry.getValue());
                        }
                    }
                    for (Map<String, String> mDeps : promptInfo.getMethodDeps()) {
                        for (Map.Entry<String, String> entry : mDeps.entrySet()) {
                            mdep_temp.put(entry.getKey(), entry.getValue());
                        }
                    }
                    promptTemplate.dataModel.put("c_dep", cdep_temp);
                    promptTemplate.dataModel.put("m_dep", mdep_temp);
                    userPrompt = promptTemplate.renderTemplate(promptTemplate.TEMPLATE_DEPS);
                }
            } else { // round > 1 -- repair prompt

                int promptTokens = TokenCounter.countToken(promptInfo.getUnitTest())
                        + TokenCounter.countToken(promptInfo.getMethodSignature())
                        + TokenCounter.countToken(promptInfo.getClassName())
                        + TokenCounter.countToken(promptInfo.getInfo())
                        + TokenCounter.countToken(promptInfo.getOtherMethods());
                int allowedTokens = Math.max(config.getMaxPromptTokens() - promptTokens, config.getMinErrorTokens());
                TestMessage errorMsg = promptInfo.getErrorMsg();
                String processedErrorMsg = "";
                for (String error : errorMsg.getErrorMessage()) {
                    if (TokenCounter.countToken(processedErrorMsg + error + "\n") <= allowedTokens) {
                        processedErrorMsg += error + "\n";
                    }
                }
                config.getLog().debug("Allowed tokens: " + allowedTokens);
                config.getLog().debug("Processed error message: \n" + processedErrorMsg);

                promptTemplate.dataModel.put("unit_test", promptInfo.getUnitTest());
                promptTemplate.dataModel.put("error_message", processedErrorMsg);
                promptTemplate.dataModel.put("method_name", promptInfo.getMethodSignature());
                promptTemplate.dataModel.put("class_name", promptInfo.getClassName());
                promptTemplate.dataModel.put("method_code", promptInfo.getInfo());

                if (!promptInfo.getOtherMethods().trim().isEmpty()) {
                    promptTemplate.dataModel.put("other_methods_code", promptInfo.getOtherMethods());
                }else {
                    promptTemplate.dataModel.put("other_methods_code",null);
                }

                userPrompt = promptTemplate.renderTemplate(promptTemplate.TEMPLATE_ERROR);
            }
            return userPrompt;

        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred while generating the user prompt.";
        }

    }


    @Override
    public String getSystemPrompt(PromptInfo promptInfo) {
        try {
            promptTemplate.readProperties();
            String filename;
            if (promptInfo.isHasDep()) {
                //d3,渲染d3_system.ftl
                filename = addSystemFileName(promptTemplate.TEMPLATE_DEPS);
                return promptTemplate.renderTemplate(filename);
            }
            //d1,渲染d1_system.ftl
            filename = addSystemFileName(promptTemplate.TEMPLATE_NO_DEPS);
            return promptTemplate.renderTemplate(filename);

        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred while generating the system prompt.";
        }
    }

    //system角色 修改文件名
    public String addSystemFileName(String filename) {
        String[] parts = filename.split("\\.");
        if (parts.length > 1) {
            return parts[0] + "_system." + parts[1];
        }
        return filename;
    }
}
