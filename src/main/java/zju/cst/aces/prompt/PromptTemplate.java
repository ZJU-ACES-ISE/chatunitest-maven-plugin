package zju.cst.aces.prompt;


import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import zju.cst.aces.config.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class PromptTemplate {

    public static final String CONFIG_FILE = "config.properties";
    public String TEMPLATE_NO_DEPS = "";
    public String TEMPLATE_DEPS = "";
    public String TEMPLATE_ERROR = "";
    public Map<String, Object> dataModel = new HashMap<>();
    public Config config;

    public PromptTemplate(Config config) {
        this.config = config;
    }

    public void readProperties() throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = PromptTemplate.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        properties.load(inputStream);
        TEMPLATE_NO_DEPS = properties.getProperty("PROMPT_TEMPLATE_NO_DEPS");//p1.ftl
        TEMPLATE_DEPS = properties.getProperty("PROMPT_TEMPLATE_DEPS");//p2.ftl
        TEMPLATE_ERROR = properties.getProperty("PROMPT_TEMPLATE_ERROR");//error.ftl
    }

    //渲染
    public String renderTemplate(String templateFileName) throws IOException, TemplateException{
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_30);

        if (config.getPromptPath() == null) {
            // 使用类加载器获取插件自身的resources目录下的文件
            configuration.setClassForTemplateLoading(PromptTemplate.class, "/prompt");
        } else {
            configuration.setDirectoryForTemplateLoading(config.getPromptPath().toFile());
        }

        if(config.getPromptPath() != null){
            // 外面处理好的文件，直接读取 txt
            String processedFileName = templateFileName.replace(".ftl",".txt");
            File promptPath = config.getPromptPath().toFile();
            File processedFile = new File(promptPath, processedFileName);
            if(processedFile.exists()){
                return Files.readString(processedFile.toPath());
            }
        }

        configuration.setDefaultEncoding("utf-8");
        Template template = configuration.getTemplate(templateFileName);

        StringWriter writer = new StringWriter();
        template.process(dataModel, writer);
        String generatedText = writer.toString();

        return generatedText;
    }

}
