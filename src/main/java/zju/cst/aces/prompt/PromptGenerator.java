package zju.cst.aces.prompt;

import zju.cst.aces.config.Config;
import zju.cst.aces.dto.PromptInfo;
import zju.cst.aces.dto.TestMessage;
import zju.cst.aces.runner.AbstractRunner;
import zju.cst.aces.util.TokenCounter;

import java.util.HashMap;
import java.util.Map;

//该类方法于AbstratRunner中调用
public class PromptGenerator implements Prompt {
    public Config config;
    PromptTemplate promptTemplate;

    public void setConfig(Config config) {
        this.config = config;
        this.promptTemplate = new PromptTemplate(config);
    }

    @Override
    public String getUserPrompt(PromptInfo promptInfo) {
        try {
            promptTemplate.readProperties();
            String userPrompt = null;
            Map<String, String> cdep_temp = new HashMap<>();
            Map<String, String> mdep_temp = new HashMap<>();
            promptTemplate.dataModel.put("method_sig", promptInfo.getMethodSignature());
            promptTemplate.dataModel.put("method_body", promptInfo.getMethodInfo().sourceCode);
            promptTemplate.dataModel.put("class_name", promptInfo.getClassName());
            promptTemplate.dataModel.put("class_sig", promptInfo.getClassInfo().classSignature);
            promptTemplate.dataModel.put("package", promptInfo.getClassInfo().packageDeclaration);
            promptTemplate.dataModel.put("full_fm", promptInfo.getInfo());
            promptTemplate.dataModel.put("imports", AbstractRunner.joinLines(promptInfo.getClassInfo().imports));
            promptTemplate.dataModel.put("fields", AbstractRunner.joinLines(promptInfo.getClassInfo().fields));

            if (!promptInfo.getClassInfo().constructorSigs.isEmpty()) {
                promptTemplate.dataModel.put("constructor_sigs", promptInfo.getClassInfo().constructorBrief);
                promptTemplate.dataModel.put("constructor_bodies", AbstractRunner.getBodies(config, promptInfo.getClassInfo(), promptInfo.getClassInfo().constructorSigs));
            } else {
                promptTemplate.dataModel.put("constructor_sigs", null);
                promptTemplate.dataModel.put("constructor_bodies", null);
            }
            if (!promptInfo.getClassInfo().getterSetterSigs.isEmpty()) {
                promptTemplate.dataModel.put("getter_setter_sigs", promptInfo.getClassInfo().getterSetterBrief);
                promptTemplate.dataModel.put("getter_setter_bodies", AbstractRunner.getBodies(config, promptInfo.getClassInfo(), promptInfo.getClassInfo().getterSetterSigs));
            } else {
                promptTemplate.dataModel.put("getter_setter_sigs", null);
                promptTemplate.dataModel.put("getter_setter_bodies", null);
            }
            if (!promptInfo.getOtherMethodBrief().trim().isEmpty()) {
                promptTemplate.dataModel.put("other_method_sigs", promptInfo.getOtherMethodBrief());
                promptTemplate.dataModel.put("other_method_bodies", promptInfo.getOtherMethodBodies());
            }else {
                promptTemplate.dataModel.put("other_method_sigs", null);
                promptTemplate.dataModel.put("other_method_bodies", null);
            }

            // round 0
            if (promptInfo.errorMsg == null) {
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
                    promptTemplate.dataModel.put("c_deps", cdep_temp);
                    promptTemplate.dataModel.put("m_deps", mdep_temp);
                    userPrompt = promptTemplate.renderTemplate(promptTemplate.TEMPLATE_DEPS);
                }
            } else { // round > 0 -- repair prompt

                int promptTokens = TokenCounter.countToken(promptInfo.getUnitTest())
                        + TokenCounter.countToken(promptInfo.getMethodSignature())
                        + TokenCounter.countToken(promptInfo.getClassName())
                        + TokenCounter.countToken(promptInfo.getInfo())
                        + TokenCounter.countToken(promptInfo.getOtherMethodBrief());
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
