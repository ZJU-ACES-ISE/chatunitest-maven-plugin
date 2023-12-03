package zju.cst.aces.prompt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import zju.cst.aces.ProjectTestMojo;
import zju.cst.aces.config.Config;
import zju.cst.aces.dto.*;
import zju.cst.aces.parser.ProjectParser;
import zju.cst.aces.runner.AbstractRunner;
import zju.cst.aces.util.TokenCounter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

//该类方法于AbstratRunner中调用
public class PromptGenerator implements Prompt {
    public Config config;
    PromptTemplate promptTemplate;
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public void setConfig(Config config) {
        this.config = config;
        this.promptTemplate = new PromptTemplate(config);
    }

    @Override
    public String getUserPrompt(PromptInfo promptInfo) throws IOException {
        try {
            promptTemplate.readProperties();
            ExampleUsage exampleUsage = new ExampleUsage(config, promptInfo.className);
            String userPrompt = null;
            Map<String, String> cdep_temp = new HashMap<>();
            Map<String, String> mdep_temp = new HashMap<>();
            promptTemplate.dataModel.put("c_deps", cdep_temp);
            promptTemplate.dataModel.put("m_deps", mdep_temp);


            // String
            promptTemplate.dataModel.put("project_full_code", getFullProjectCode(promptInfo.getClassName(), config));
            promptTemplate.dataModel.put("method_name", promptInfo.getMethodName());
            promptTemplate.dataModel.put("method_sig", promptInfo.getMethodSignature());
            promptTemplate.dataModel.put("method_body", promptInfo.getMethodInfo().sourceCode);
            promptTemplate.dataModel.put("class_name", promptInfo.getClassName());
            promptTemplate.dataModel.put("class_sig", promptInfo.getClassInfo().classSignature);
            promptTemplate.dataModel.put("package", promptInfo.getClassInfo().packageDeclaration);
            promptTemplate.dataModel.put("class_body", promptInfo.getClassInfo().classDeclarationCode);
            promptTemplate.dataModel.put("file_content", promptInfo.getClassInfo().compilationUnitCode);
            promptTemplate.dataModel.put("full_fm", promptInfo.getInfo());
            promptTemplate.dataModel.put("imports", AbstractRunner.joinLines(promptInfo.getClassInfo().imports));
            promptTemplate.dataModel.put("fields", AbstractRunner.joinLines(promptInfo.getClassInfo().fields));
            promptTemplate.dataModel.put("example_usage", exampleUsage.getShortestUsage(promptInfo.getMethodInfo().methodSignature));
            if (!promptInfo.getClassInfo().constructorSigs.isEmpty()) {
                promptTemplate.dataModel.put("constructor_sigs", AbstractRunner.joinLines(promptInfo.getClassInfo().constructorBrief));
                promptTemplate.dataModel.put("constructor_bodies", AbstractRunner.getBodies(config, promptInfo.getClassInfo(), promptInfo.getClassInfo().constructorSigs));
            } else {
                promptTemplate.dataModel.put("constructor_sigs", null);
                promptTemplate.dataModel.put("constructor_bodies", null);
            }
            if (!promptInfo.getClassInfo().getterSetterSigs.isEmpty()) {
                promptTemplate.dataModel.put("getter_setter_sigs", AbstractRunner.joinLines(promptInfo.getClassInfo().getterSetterBrief));
                promptTemplate.dataModel.put("getter_setter_bodies", AbstractRunner.getBodies(config, promptInfo.getClassInfo(), promptInfo.getClassInfo().getterSetterSigs));
            } else {
                promptTemplate.dataModel.put("getter_setter_sigs", null);
                promptTemplate.dataModel.put("getter_setter_bodies", null);
            }
            if (!promptInfo.getOtherMethodBrief().trim().isEmpty()) {
                promptTemplate.dataModel.put("other_method_sigs", promptInfo.getOtherMethodBrief());
                promptTemplate.dataModel.put("other_method_bodies", promptInfo.getOtherMethodBodies());
            } else {
                promptTemplate.dataModel.put("other_method_sigs", null);
                promptTemplate.dataModel.put("other_method_bodies", null);
            }

            // Map<String, String>, key: dependent class names
            promptTemplate.dataModel.put("dep_packages", getDepPackages(promptInfo.getClassInfo(), promptInfo.getMethodInfo()));
            promptTemplate.dataModel.put("dep_imports", getDepImports(promptInfo.getClassInfo(), promptInfo.getMethodInfo()));
            promptTemplate.dataModel.put("dep_class_sigs", getDepClassSigs(promptInfo.getClassInfo(), promptInfo.getMethodInfo()));
            promptTemplate.dataModel.put("dep_class_bodies", getDepClassBodies(promptInfo.getClassInfo(), promptInfo.getMethodInfo()));
            promptTemplate.dataModel.put("dep_m_sigs", getDepBrief(promptInfo.getMethodInfo()));
            promptTemplate.dataModel.put("dep_m_bodies", getDepBodies(promptInfo.getMethodInfo()));
            promptTemplate.dataModel.put("dep_c_sigs", getDepConstructorSigs(promptInfo.getClassInfo(), promptInfo.getMethodInfo()));
            promptTemplate.dataModel.put("dep_c_bodies", getDepConstructorBodies(promptInfo.getClassInfo(), promptInfo.getMethodInfo()));
            promptTemplate.dataModel.put("dep_fields", getDepFields(promptInfo.getClassInfo(), promptInfo.getMethodInfo()));
            promptTemplate.dataModel.put("dep_gs_sigs", getDepGSSigs(promptInfo.getClassInfo(), promptInfo.getMethodInfo()));
            promptTemplate.dataModel.put("dep_gs_bodies", getDepGSBodies(promptInfo.getClassInfo(), promptInfo.getMethodInfo()));


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
            throw new IOException("An error occurred while generating the user prompt: " + e);
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
            if (e instanceof IOException) {
                return "";
            }
            throw new RuntimeException("An error occurred while generating the system prompt: " + e);
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

    public Map<String, String> getDepBrief(MethodInfo methodInfo) throws IOException {
        Map<String, String> depBrief = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depBrief;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            String info = "";
            for (String depMethodSig : entry.getValue()) {
                MethodInfo depMethodInfo = AbstractRunner.getMethodInfo(config, depClassInfo, depMethodSig);
                if (depMethodInfo == null) {
                    continue;
                }
                info += depMethodInfo.brief + "\n";
            }
            depBrief.put(depClassName, info.trim());
        }
        return depBrief;
    }

    public Map<String, String> getDepBodies(MethodInfo methodInfo) throws IOException {
        Map<String, String> depBodies = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depBodies;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            String info = "";
            for (String depMethodSig : entry.getValue()) {
                MethodInfo depMethodInfo = AbstractRunner.getMethodInfo(config, depClassInfo, depMethodSig);
                if (depMethodInfo == null) {
                    continue;
                }
                info += depMethodInfo.sourceCode + "\n";
            }
            depBodies.put(depClassName, info.trim());
        }
        return depBodies;
    }

    public Map<String, String> getDepFields(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        Map<String, String> depFields = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classInfo.constructorDeps.entrySet()) {
            String depClassName = entry.getKey();
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depFields;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depFields.put(depClassName, AbstractRunner.joinLines(depClassInfo.fields));
        }

        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            if (depFields.containsKey(depClassName)) {
                continue;
            }
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depFields;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depFields.put(depClassName, AbstractRunner.joinLines(depClassInfo.fields));
        }
        return depFields;
    }

    public Map<String, String> getDepConstructorSigs(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        Map<String, String> depConstructorSigs = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classInfo.constructorDeps.entrySet()) {
            String depClassName = entry.getKey();
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depConstructorSigs;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depConstructorSigs.put(depClassName, AbstractRunner.joinLines(depClassInfo.constructorBrief));
        }

        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            if (depConstructorSigs.containsKey(depClassName)) {
                continue;
            }
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depConstructorSigs;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depConstructorSigs.put(depClassName, AbstractRunner.joinLines(depClassInfo.constructorBrief));
        }
        return depConstructorSigs;
    }

    public Map<String, String> getDepConstructorBodies(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        Map<String, String> depConstructorBodies = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classInfo.constructorDeps.entrySet()) {
            String depClassName = entry.getKey();
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depConstructorBodies;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }

            String info = "";
            for (String sig : depClassInfo.constructorSigs) {
                MethodInfo depConstructorInfo = AbstractRunner.getMethodInfo(config, depClassInfo, sig);
                if (depConstructorInfo == null) {
                    continue;
                }
                info += depConstructorInfo.sourceCode + "\n";
            }
            depConstructorBodies.put(depClassName, info.trim());
        }

        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            if (depConstructorBodies.containsKey(depClassName)) {
                continue;
            }
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depConstructorBodies;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }

            String info = "";
            for (String sig : depClassInfo.constructorSigs) {
                MethodInfo depConstructorInfo = AbstractRunner.getMethodInfo(config, depClassInfo, sig);
                if (depConstructorInfo == null) {
                    continue;
                }
                info += depConstructorInfo.sourceCode + "\n";
            }
            depConstructorBodies.put(depClassName, info.trim());
        }
        return depConstructorBodies;
    }

    public Map<String, String> getDepClassSigs(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        Map<String, String> depClassSigs = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classInfo.constructorDeps.entrySet()) {
            String depClassName = entry.getKey();
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depClassSigs;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depClassSigs.put(depClassName, depClassInfo.classSignature);
        }

        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            if (depClassSigs.containsKey(depClassName)) {
                continue;
            }
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depClassSigs;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depClassSigs.put(depClassName, depClassInfo.classSignature);
        }
        return depClassSigs;
    }

    public Map<String, String> getDepClassBodies(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        Map<String, String> depClassBodies = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classInfo.constructorDeps.entrySet()) {
            String depClassName = entry.getKey();
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depClassBodies;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depClassBodies.put(depClassName, depClassInfo.classDeclarationCode);
        }

        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            if (depClassBodies.containsKey(depClassName)) {
                continue;
            }
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depClassBodies;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depClassBodies.put(depClassName, depClassInfo.classDeclarationCode);
        }
        return depClassBodies;
    }

    public Map<String, String> getDepPackages(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        Map<String, String> depPackages = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classInfo.constructorDeps.entrySet()) {
            String depClassName = entry.getKey();
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depPackages;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depPackages.put(depClassName, depClassInfo.packageDeclaration);
        }

        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            if (depPackages.containsKey(depClassName)) {
                continue;
            }
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depPackages;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depPackages.put(depClassName, depClassInfo.packageDeclaration);
        }
        return depPackages;
    }

    public Map<String, String> getDepImports(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        Map<String, String> depImports = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classInfo.constructorDeps.entrySet()) {
            String depClassName = entry.getKey();
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depImports;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depImports.put(depClassName, AbstractRunner.joinLines(depClassInfo.imports));
        }

        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            if (depImports.containsKey(depClassName)) {
                continue;
            }
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depImports;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depImports.put(depClassName, AbstractRunner.joinLines(depClassInfo.imports));
        }
        return depImports;
    }

    public Map<String, String> getDepGSSigs(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        Map<String, String> depGSSigs = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classInfo.constructorDeps.entrySet()) {
            String depClassName = entry.getKey();
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depGSSigs;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depGSSigs.put(depClassName, AbstractRunner.joinLines(depClassInfo.getterSetterSigs));
        }

        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            if (depGSSigs.containsKey(depClassName)) {
                continue;
            }
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depGSSigs;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }
            depGSSigs.put(depClassName, AbstractRunner.joinLines(depClassInfo.getterSetterSigs));
        }
        return depGSSigs;
    }

    public Map<String, String> getDepGSBodies(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        Map<String, String> depGSBodies = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classInfo.constructorDeps.entrySet()) {
            String depClassName = entry.getKey();
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depGSBodies;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }

            String info = "";
            for (String sig : depClassInfo.getterSetterSigs) {
                MethodInfo depConstructorInfo = AbstractRunner.getMethodInfo(config, depClassInfo, sig);
                if (depConstructorInfo == null) {
                    continue;
                }
                info += depConstructorInfo.sourceCode + "\n";
            }
            depGSBodies.put(depClassName, info.trim());
        }

        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            if (depGSBodies.containsKey(depClassName)) {
                continue;
            }
            String fullDepClassName = ProjectTestMojo.getFullClassName(config, depClassName);
            Path depClassInfoPath = config.getParseOutput().resolve(fullDepClassName.replace(".", File.separator)).resolve("class.json");
            if (!depClassInfoPath.toFile().exists()) {
                return depGSBodies;
            }
            ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);
            if (depClassInfo == null) {
                continue;
            }

            String info = "";
            for (String sig : depClassInfo.getterSetterSigs) {
                MethodInfo depConstructorInfo = AbstractRunner.getMethodInfo(config, depClassInfo, sig);
                if (depConstructorInfo == null) {
                    continue;
                }
                info += depConstructorInfo.sourceCode + "\n";
            }
            depGSBodies.put(depClassName, info.trim());
        }
        return depGSBodies;
    }

    public String getFullProjectCode(String className, Config config) {
        String fullProjectCode = "";
        List<String> classPaths = new ArrayList<>();
        ProjectParser.scanSourceDirectory(config.project, classPaths);
        // read the file content of each path and append to fullProjectCode
        for (String path : classPaths) {
            String cn = path.substring(path.lastIndexOf(File.separator) + 1, path.lastIndexOf("."));
            if (cn.equals(className)) {
                continue;
            }
            try {
                fullProjectCode += Files.readString(Paths.get(path), StandardCharsets.UTF_8) + "\n";
            } catch (IOException e) {
                config.getLog().warn("Failed to append class code for " + className);
            }
        }
        return fullProjectCode;
    }
}
