# :mega: ChatUnitest Maven Plugin

[English](./README.md) | [中文](./Readme_zh.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.ZJU-ACES-ISE/chatunitest-maven-plugin?color=hex&style=plastic)](https://maven-badges.herokuapp.com/maven-central/io.github.ZJU-ACES-ISE/chatunitest-maven-plugin)

## 更新
💥 添加docker映像以在隔离的沙箱环境中生成测试。

💥 新增多线程功能，实现更快的测试生成。

💥 插件现在可以导出运行时和错误日志。

💥 新增自定义提示支持。 

💥 优化算法以减少令牌使用。

💥 扩展配置选项。请参考**运行步骤**了解详情。

💥 集成多项相关工作。

## 动机
相信很多人试过用ChatGPT帮助自己完成各种各样的编程任务，并且已经取得了不错的效果。但是，直接使用ChatGPT存在一些问题： 一是生成的代码很多时候不能正常执行，**“编程五分钟，调试两小时”**； 二是不方便跟现有工程进行集成，需要手动与ChatGPT进行交互，并且在不同页面间切换。为了解决这些问题，我们提出了 **“生成-验证-修复”** 框架，并实现了原型系统，同时为了方便大家使用，我们开发了一些插件，能够方便的集成到已有开发流程中。已完成Maven插件开发，并且已发布到Maven中心仓库，欢迎试用和反馈。我们已在 IntelliJ IDEA 插件市场中上架了 Chatunitest 插件，您可以在市场中搜索并安装 ChatUniTest，或者访问插件页面[Chatunitest:IntelliJ IDEA Plugin](https://plugins.jetbrains.com/plugin/22522-chatunitest) 了解有关我们插件的更多信息。在这个最新分支，整合了多项我们复现的相关工作，大家可以自行选择，按需使用。

## 运行步骤（Docker）
见[chenyi26/chatunitest](https://hub.docker.com/repository/docker/chenyi26/chatunitest/general)

## 运行步骤

**总的来说，你需要在maven-plugin的pom中引入一个dependency（core），在待测项目的pom中引入一个dependency（starter）和一个plugin（maven-plugin）**

**并且注意引入的version是否正确**

### 1. `pom.xml`文件配置

在待生成单测的项目中的`pom.xml`文件内加入 chatunitest-maven-plugin 的插件配置，并按照您的需求添加参数：
```xml
<plugin>
    <groupId>io.github.ZJU-ACES-ISE</groupId>
    <artifactId>chatunitest-maven-plugin</artifactId>
    <version>1.5.1</version>
    <configuration>
        <!-- Required: You must specify your OpenAI API keys. -->
        <apiKeys></apiKeys>g
        <model>gpt-4o-mini</model>
        <proxy>${proxy}</proxy>
    </configuration>
</plugin>
```
一般情况下，您只需要提供API密钥。**如果出现APIConnectionError，您可以在proxy参数中添加您的代理ip和端口号**。Windows系统里下的代理ip和端口可以在设置->网络和Internet->代理中查看：

**下面是每个配置选项的详细说明:**

- `apiKeys`: (**必需**) 您的OpenAI API keys，示例：`Key1, Key2, ...`
- `model`: (**可选**) OpenAI模型，默认值：`gpt-3.5-turbo`
- `url`: (**可选**) 调用模型的API，默认值：`https://api.openai.com/v1/chat/completions`
- `testNumber`: (**可选**) 每个方法的生成的测试数量，默认值：`5`
- `maxRounds`: (**可选**) 修复过程的最大轮次，默认值：`5`
- `minErrorTokens`: (**可选**) 修复过程中错误信息的最小token数，默认值：`500`
- `temperature`: (**可选**) OpenAI API参数，默认值：`0.5`
- `topP`: (**可选**) OpenAI API参数，默认值： `1`
- `frequencyPenalty`: (**可选**) OpenAI API参数，默认值： `0`
- `presencePenalty`: (**可选**) OpenAI API参数，默认值： `0`
- `proxy`: (**可选**)如果需要，填写您的主机名和端口号，示例：`127.0.0.1:7078`
- `selectClass`: (**可选**) 被测试的类，如果项目中有同名类，需要指定完整的类名。
- `selectMethod`: (**可选**) 被测试的方法
- `tmpOutput`: (**可选**) 解析项目信息的输出路径，默认值： `/tmp/chatunitest-info`
- `testOutput`: (**可选**) 由 `chatunitest`生成的测试的输出路径，默认值：`{basedir}/chatunitest`
- `project`: (**可选**) 目标项目路径，默认值：`{basedir}`
- `thread`: (**可选**) 开启或关闭多线程，默认值：`true`
- `maxThread`: (**可选**) 最大线程数，默认值：`CPU核心数 * 5`
- `stopWhenSuccess`: (**可选**) 是否在生成一个成功的测试后停止，默认值：`true`
- `noExecution`: (**可选**) 是否跳过执行测试验证的步骤，默认值：`false`
所有这些参数也可以在命令行中使用-D选项指定。
- `merge` : (**可选**) 将每个类对应的所有测试合并为测试套件，默认值: `true`.
- `promptPath` : (**可选**) 自定义prompt的路径. 参考默认promp目录: `src/main/resources/prompt`.
- `obfuscate` : (**可选**) 开启混淆功能以保护隐私代码. 默认值: false. 
- `obfuscateGroupIds` : (**可选**) 需要进行混淆的group ID. 默认值仅包含当前项目的group ID. 所有这些参数也可以在命令行中使用-D选项指定。
- `phaseType` : (**可选**) 选择复现方案，如果未选择，则会执行默认的chatunitest进程. 所有这些参数也可以在命令行中使用-D选项指定。
    - COVERUP
    - HITS
    - TELPA
    - SYMPROMPT
    - CHATTESTER
    - TESTSPARK
    - TESTPILOT
    - MUTAP

如果使用本地大模型（例如code-llama），只需修改模型名和请求url即可，例如：
```xml
<plugin>
    <groupId>io.github.ZJU-ACES-ISE</groupId>
    <artifactId>chatunitest-maven-plugin</artifactId>
    <version>1.5.1</version>
    <configuration>
        <!-- Required: Use any string to replace your API keys -->
        <apiKeys>xxx</apiKeys>
        <model>code-llama</model>
        <url>http://0.0.0.0:8000/v1/chat/completions</url>
    </configuration>
</plugin>
```

![img.png](src/main/resources/img/win_proxy.png)

### 1. 将以下依赖项添加到`pom.xml`文件中
同样的，在待生成单测的项目的pom中添加依赖
```xml
<dependency>
    <groupId>io.github.ZJU-ACES-ISE</groupId>
    <artifactId>chatunitest-starter</artifactId>
    <version>1.4.0</version>
    <type>pom</type>
</dependency>
```

### 2. 运行

**首先你需要安装项目并下载所需的依赖，这可以通过运行`mvn install`命令来完成。**

**您可以用下面的命令运行插件:**

**为目标方法生成单元测试：**

```shell
mvn chatunitest:method -DselectMethod=className#methodName
```

**为目标类生成单元测试：**

```shell
mvn chatunitest:class -DselectClass=className
```

当执行 `mvn chatunitest:method` 或 `mvn chatunitest:class` 命令时，您必须指定 `selectMethod` 和 `selectClass`，可以使用 -D 选项来实现这一点。

示例：

```
public class Example {
    public void method1(Type1 p1, ...) {...}
    public void method2() {...}
    ...
}
```

对Example类及其所有方法进行测试：

```shell
mvn chatunitest:class -DselectClass=Example
```

对Example类中的方法method1进行测试：（目前ChatUnitest将为类中所有名为method1的方法生成测试）

```shell
mvn chatunitest:method -DselectMethod=Example#method1
```

**为整个项目生成单元测试：**

:warning: :warning: :warning: 对于大型项目来说，可能会消耗大量的token，导致相当大的费用。

```shell
mvn chatunitest:project
```

**使用目标方案生成单元测试：**

```shell
mvn chatunitest:method -DselectMethod=className#methodName -DselectMethod=className#methodName -DphaseType=COVERUP
```

**清理生成的测试代码：**

```shell
mvn chatunitest:clean
```

运行该命令将删除所有生成的测试代码并恢复您的测试目录。

**手动运行生成的测试：**

```shell
mvn chatunitest:copy
```

运行该命令将复制所有生成的测试代码到您的测试文件夹，同时备份您的测试目录。

如果启用了`merge`配置，则可以运行每个类的测试套件。

```shell
mvn chatunitest:restore
```

运行该命令将恢复您的测试目录

## 自定义内容
### 使用 FTL 模板

#### 1. 配置映射关系
在 `config.properties` 中定义映射关系。

#### 2. 定义 PromptFile 枚举类
在 `PromptFile` 枚举类中定义枚举常量及其对应的模板文件名。

#### 3. 引用模板
在 `PromptGenerator` 类中的 `getInitPromptFile` 和 `getRepairPromptFile` 方法中引用 `PromptFile` 的模板。

#### 4. 生成 Prompt
后续调用 `PromptGenerator` 的 `generateMessages` 方法即可获取 prompt。具体实现方式可参见 HITS 的实现。

### 扩展 FTL 模板
`PromptInfo` 是一个数据实体类，这部分可以按需扩展。`PromptTemplate` 中的 `dataModel` 存放着供 FTL 模板使用的变量数据。如果有自定义新的 FTL 模板，请检查是否有新的变量引入，并及时更新 `dataModel`。

### 修改生成单测的粒度
可以构造一个 `MethodRunner` 的继承类，参见 `HITSRunner`。并在 `selectRunner` 方法中添加新的实现。

### 自定义单元测试生成方案
如果你想要自行定义单元测试生成方案，下面给出一个示例：

- 首先，你需要定义一个 `PhaseImpl` 的继承类，用于实现核心的生成方案。我们一般将其放置在 `phase` 的 `solution` 文件夹中。
  
- 接着，你需要在 `PhaseImpl` 类中的 `createPhase` 方法中添加新的实现。如果有新增模板，请参考上述使用 FTL 模板的部分；如果有新的数据变量引入，请参见修改 FTL 模板的部分。

- 如需修改生成单测的粒度，例如 HITS 是针对方法切片生成单元测试，请参考修改生成单测的粒度部分。


## :email: 联系我们

如果您有任何问题或想了解我们的实验结果，请随时通过电子邮件与我们联系，联系方式如下：

1. Corresponding author: `zjuzhichen AT zju.edu.cn`
2. Author: `yh_ch AT zju.edu.cn`









