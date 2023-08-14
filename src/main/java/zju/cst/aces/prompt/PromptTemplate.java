package zju.cst.aces.prompt;


import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class PromptTemplate {

    public static final String CONFIG_FILE = "config.properties";
    public static final String TEMPLATE_FILE_PATH = "src/main/resources/prompt";
    public static String TEMPLATE_NO_DEPS = "";
    public static String TEMPLATE_DEPS = "";
    public static String TEMPLATE_ERROR = "";
    public static Map<String, Object> dataModel = new HashMap<>();

    public void readProperties() throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = PromptTemplate.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        properties.load(inputStream);
        TEMPLATE_NO_DEPS = properties.getProperty("PROMPT_TEMPLATE_NO_DEPS");//d1.ftl
        TEMPLATE_DEPS = properties.getProperty("PROMPT_TEMPLATE_DEPS");//d3.ftl
        TEMPLATE_ERROR = properties.getProperty("PROMPT_TEMPLATE_ERROR");//error.ftl
    }



    //渲染
    public static String renderTemplate(String templateFileName) throws IOException, TemplateException{
        Configuration configuration = new Configuration(Configuration.getVersion());
        configuration.setDirectoryForTemplateLoading(new File(TEMPLATE_FILE_PATH));
        configuration.setDefaultEncoding("utf-8");
        Template template = configuration.getTemplate(templateFileName);

        StringWriter writer = new StringWriter();
        template.process(dataModel, writer);
        String generatedText = writer.toString();

        return generatedText;
    }

}
