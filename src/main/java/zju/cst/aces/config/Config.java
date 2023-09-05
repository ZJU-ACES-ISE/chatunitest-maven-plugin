package zju.cst.aces.config;

import com.github.javaparser.JavaParser;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import zju.cst.aces.util.TestCompiler;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class Config {
    public String date;
    public MavenSession session;
    public MavenProject project;
    public DependencyGraphBuilder dependencyGraphBuilder;
    public JavaParser parser;
    public JavaParserFacade parserFacade;
    public List<String> classPaths;
    public Path promptPath;
    public String url;
    public String[] apiKeys;
    public Log log;
    public String OS;
    public boolean stopWhenSuccess;
    public boolean noExecution;
    public boolean enableMultithreading;
    public boolean enableRuleRepair;
    public int maxThreads;
    public int classThreads;
    public int methodThreads;
    public int testNumber;
    public int maxRounds;
    public int maxPromptTokens;
    public int minErrorTokens;
    public int sleepTime;
    public String model;
    public Double temperature;
    public int topP;
    public int frequencyPenalty;
    public int presencePenalty;
    public Path testOutput;
    public Path tmpOutput;
    public Path parseOutput;
    public Path errorOutput;
    public Path classNameMapPath;
    public Path historyPath;
    public Path examplePath;

    public String proxy;
    public String hostname;
    public String port;
    public OkHttpClient client;
    public static AtomicInteger sharedInteger = new AtomicInteger(0);
    public static Map<String, Map<String, String>> classMapping;

    public static class ConfigBuilder {
        public String date;
        public MavenSession session;
        public MavenProject project;
        public DependencyGraphBuilder dependencyGraphBuilder;
        public JavaParser parser;
        public JavaParserFacade parserFacade;
        public List<String> classPaths;
        public Path promptPath;
        public String url;
        public String[] apiKeys;
        public Log log;
        public String OS = System.getProperty("os.name").toLowerCase();
        public boolean stopWhenSuccess = true;
        public boolean noExecution = false;
        public boolean enableMultithreading = true;
        public boolean enableRuleRepair = true;
        public int maxThreads = Runtime.getRuntime().availableProcessors() * 5;
        public int classThreads = (int) Math.ceil((double)  this.maxThreads / 10);
        public int methodThreads = (int) Math.ceil((double) this.maxThreads / this.classThreads);
        public int testNumber = 5;
        public int maxRounds = 5;
        public int maxPromptTokens = 2600;
        public int minErrorTokens = 500;
        public int sleepTime = 0;
        public String model = "gpt-3.5-turbo";
        public Double temperature = 0.5;
        public int topP = 1;
        public int frequencyPenalty = 0;
        public int presencePenalty = 0;
        public Path testOutput;
        public Path tmpOutput = Paths.get(System.getProperty("java.io.tmpdir"), "chatunitest-info");
        public Path parseOutput;
        public Path errorOutput;
        public Path classNameMapPath;
        public Path historyPath;
        public Path examplePath;
        public String proxy = "null:-1";
        public String hostname = "null";
        public String port = "-1";
        public OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();


        public ConfigBuilder(MavenSession session, MavenProject project, DependencyGraphBuilder dependencyGraphBuilder, Log log) {
            this.date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")).toString();
            this.session = session;
            this.project = project;
            this.dependencyGraphBuilder = dependencyGraphBuilder;
            this.classPaths = TestCompiler.listClassPaths(session, project, dependencyGraphBuilder);
            this.log = log;

            MavenProject parent = project.getParent();
            while(parent != null && parent.getBasedir() != null) {
                this.tmpOutput = this.tmpOutput.resolve(parent.getArtifactId());
                parent = parent.getParent();
            }
            this.tmpOutput = this.tmpOutput.resolve(project.getArtifactId());
            this.parseOutput = this.tmpOutput.resolve("class-info");
            this.errorOutput = this.tmpOutput.resolve("error-message");
            this.classNameMapPath = this.tmpOutput.resolve("classNameMapping.json");
            this.historyPath = this.tmpOutput.resolve("history" + this.date);
        }

        public ConfigBuilder maxThreads(int maxThreads) {
            if (maxThreads <= 0) {
                this.maxThreads = Runtime.getRuntime().availableProcessors() * 5;
            } else {
                this.maxThreads = maxThreads;
            }
            this.classThreads = (int) Math.ceil((double)  this.maxThreads / 10);
            this.methodThreads = (int) Math.ceil((double) this.maxThreads / this.classThreads);
            if (this.stopWhenSuccess == false) {
                this.methodThreads = (int) Math.ceil((double)  this.methodThreads / this.testNumber);
            }
            return this;
        }

        public ConfigBuilder proxy(String proxy) {
            setProxy(proxy);
            return this;
        }

        public ConfigBuilder tmpOutput(Path tmpOutput) {
            this.tmpOutput = tmpOutput;
            MavenProject parent = project.getParent();
            while(parent != null && parent.getBasedir() != null) {
                this.tmpOutput = this.tmpOutput.resolve(parent.getArtifactId());
                parent = parent.getParent();
            }
            this.tmpOutput = this.tmpOutput.resolve(project.getArtifactId());
            this.parseOutput = this.tmpOutput.resolve("class-info");
            this.errorOutput = this.tmpOutput.resolve("error-message");
            this.classNameMapPath = this.tmpOutput.resolve("classNameMapping.json");
            this.historyPath = this.tmpOutput.resolve("history" + this.date);
            return this;
        }

        public ConfigBuilder session(MavenSession session) {
            this.session = session;
            return this;
        }

        public ConfigBuilder project(MavenProject project) {
            this.project = project;
            return this;
        }

        public ConfigBuilder dependencyGraphBuilder(DependencyGraphBuilder dependencyGraphBuilder) {
            this.dependencyGraphBuilder = dependencyGraphBuilder;
            return this;
        }

        public ConfigBuilder promptPath(File promptPath) {
            if (promptPath != null) {
                this.promptPath = promptPath.toPath();
            }
            return this;
        }

        public ConfigBuilder parser(JavaParser parser) {
            this.parser = parser;
            return this;
        }

        public ConfigBuilder parserFacade(JavaParserFacade parserFacade) {
            this.parserFacade = parserFacade;
            return this;
        }

        public ConfigBuilder classPaths(List<String> classPaths) {
            this.classPaths = classPaths;
            return this;
        }

        public ConfigBuilder log(Log log) {
            this.log = log;
            return this;
        }

        public ConfigBuilder OS(String OS) {
            this.OS = OS;
            return this;
        }

        public ConfigBuilder stopWhenSuccess(boolean stopWhenSuccess) {
            this.stopWhenSuccess = stopWhenSuccess;
            return this;
        }

        public ConfigBuilder noExecution(boolean noExecution) {
            this.noExecution = noExecution;
            return this;
        }

        public ConfigBuilder enableMultithreading(boolean enableMultithreading) {
            this.enableMultithreading = enableMultithreading;
            return this;
        }

        public ConfigBuilder enableRuleRepair(boolean enableRuleRepair) {
            this.enableRuleRepair = enableRuleRepair;
            return this;
        }

        public ConfigBuilder classThreads(int classThreads) {
            this.classThreads = classThreads;
            return this;
        }

        public ConfigBuilder methodThreads(int methodThreads) {
            this.methodThreads = methodThreads;
            return this;
        }

        public ConfigBuilder url(String url) {
            if (!this.model.contains("gpt-4") && !this.model.contains("gpt-3.5") && !url.equals("https://api.openai.com/v1/chat/completions")) {
                throw new RuntimeException("Invalid url for model: " + this.model + ". Please configure the url in plugin configuration.");
            }
            this.url = url;
            return this;
        }

        public ConfigBuilder apiKeys(String[] apiKeys) {
            this.apiKeys = apiKeys;
            return this;
        }

        public ConfigBuilder testNumber(int testNumber) {
            this.testNumber = testNumber;
            return this;
        }

        public ConfigBuilder maxRounds(int maxRounds) {
            this.maxRounds = maxRounds;
            return this;
        }

        public ConfigBuilder maxPromptTokens(int maxPromptTokens) {
            this.maxPromptTokens = maxPromptTokens;
            return this;
        }

        public ConfigBuilder minErrorTokens(int minErrorTokens) {
            this.minErrorTokens = minErrorTokens;
            return this;
        }

        public ConfigBuilder sleepTime(int sleepTime) {
            this.sleepTime = sleepTime;
            return this;
        }

        public ConfigBuilder model(String model) {
            this.model = model;
            return this;
        }

        public ConfigBuilder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public ConfigBuilder topP(int topP) {
            this.topP = topP;
            return this;
        }

        public ConfigBuilder frequencyPenalty(int frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public ConfigBuilder presencePenalty(int presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public ConfigBuilder testOutput(Path testOutput) {
            this.testOutput = testOutput;
            return this;
        }

        public ConfigBuilder parseOutput(Path parseOutput) {
            this.parseOutput = parseOutput;
            return this;
        }

        public ConfigBuilder errorOutput(Path errorOutput) {
            this.errorOutput = errorOutput;
            return this;
        }

        public ConfigBuilder classNameMapPath(Path classNameMapPath) {
            this.classNameMapPath = classNameMapPath;
            return this;
        }

        public ConfigBuilder examplePath(Path examplePath) {
            this.examplePath = examplePath;
            return this;
        }

        public ConfigBuilder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public ConfigBuilder port(String port) {
            this.port = port;
            return this;
        }

        public ConfigBuilder client(OkHttpClient client) {
            this.client = client;
            return this;
        }

        public void setProxy(String proxy) {
            this.proxy = proxy;
            setProxyStr();
            if (!hostname.equals("null") && !port.equals("-1")) {
                setClinetwithProxy();
            } else {
                setClinet();
            }
        }

        public void setProxyStr() {
            this.hostname = this.proxy.split(":")[0];
            this.port = this.proxy.split(":")[1];
        }

        public void setClinet() {
            this.client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.MINUTES)
                    .writeTimeout(5, TimeUnit.MINUTES)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .build();
        }

        public void setClinetwithProxy() {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.hostname, Integer.parseInt(this.port)));
            this.client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.MINUTES)
                    .writeTimeout(5, TimeUnit.MINUTES)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .proxy(proxy)
                    .build();
        }

        public Config build() {
            Config config = new Config();
            config.setDate(this.date);
            config.setSession(this.session);
            config.setProject(this.project);
            config.setDependencyGraphBuilder(this.dependencyGraphBuilder);
            config.setParser(this.parser);
            config.setParserFacade(this.parserFacade);
            config.setClassPaths(this.classPaths);
            config.setPromptPath(this.promptPath);
            config.setUrl(this.url);
            config.setApiKeys(this.apiKeys);
            config.setOS(this.OS);
            config.setStopWhenSuccess(this.stopWhenSuccess);
            config.setNoExecution(this.noExecution);
            config.setEnableMultithreading(this.enableMultithreading);
            config.setEnableRuleRepair(this.enableRuleRepair);
            config.setMaxThreads(this.maxThreads);
            config.setClassThreads(this.classThreads);
            config.setMethodThreads(this.methodThreads);
            config.setTestNumber(this.testNumber);
            config.setMaxRounds(this.maxRounds);
            config.setMaxPromptTokens(this.maxPromptTokens);
            config.setMinErrorTokens(this.minErrorTokens);
            config.setSleepTime(this.sleepTime);
            config.setModel(this.model);
            config.setTemperature(this.temperature);
            config.setTopP(this.topP);
            config.setFrequencyPenalty(this.frequencyPenalty);
            config.setPresencePenalty(this.presencePenalty);
            config.setTestOutput(this.testOutput);
            config.setTmpOutput(this.tmpOutput);
            config.setParseOutput(this.parseOutput);
            config.setErrorOutput(this.errorOutput);
            config.setClassNameMapPath(this.classNameMapPath);
            config.setHistoryPath(this.historyPath);
            config.setExamplePath(this.examplePath);
            config.setProxy(this.proxy);
            config.setHostname(this.hostname);
            config.setPort(this.port);
            config.setClient(this.client);
            config.setLog(this.log);
            return config;
        }
    }

    public String getRandomKey() {
        Random rand = new Random();
        if (apiKeys.length == 0) {
            throw new RuntimeException("apiKeys is null!");
        }
        String apiKey = apiKeys[rand.nextInt(apiKeys.length)];
        return apiKey;
    }
}
