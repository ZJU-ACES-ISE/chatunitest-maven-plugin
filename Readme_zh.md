# :mega: ChatUnitest Maven Plugin

[English](./README.md) | [中文](./Readme_zh.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.zju-aces-ise/chatunitest-maven-plugin?color=hex&style=plastic)](https://maven-badges.herokuapp.com/maven-central/io.github.zju-aces-ise/chatunitest-maven-plugin)

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


## 运行步骤

### 1. `pom.xml`文件配置

在待生成单测的项目中的`pom.xml`文件内加入 chatunitest-maven-plugin 的插件配置，并按照您的需求添加参数：
```xml
<plugin>
    <groupId>io.github.zju-aces-ise</groupId>
    <artifactId>chatunitest-maven-plugin</artifactId>
    <!-- Required: Use  the lastest version -->
    <version>2.0.0</version>
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
    <groupId>io.github.zju-aces-ise</groupId>
    <artifactId>chatunitest-maven-plugin</artifactId>
    <version>2.0.0</version>
    <configuration>
        <!-- Required: Use any string to replace your API keys -->
        <apiKeys>xxx</apiKeys>
        <model>code-llama</model>
        <url>http://0.0.0.0:8000/v1/chat/completions</url>
    </configuration>
</plugin>
```

### 1. 将以下依赖项添加到`pom.xml`文件中
同样的，在待生成单测的项目的pom中添加依赖
```xml
<dependency>
    <groupId>io.github.zju-aces-ise</groupId>
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
mvn chatunitest:method -DselectMethod=className#methodName -DselectMethod=className#methodName -DphaseType=CHATTESTER
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
如果您想要自定义内容，例如扩展ftl，或者使用自定义单测生成方案，可以[参考此处](https://github.com/ZJU-ACES-ISE/chatunitest-core/blob/corporation/Readme_zh.md#%E4%BD%BF%E7%94%A8-ftl-%E6%A8%A1%E6%9D%BF)

## 注意事项
### 1. COVERUP
初次使用可能报错，需要将resources目录下面的[jacoco-integration.zip](https://github.com/ZJU-ACES-ISE/chatunitest-maven-plugin/blob/main/src/main/resources/jacoco-integration.zip)解压至指定目录（io\github\ZJU-ACES-ISE）中

### 2. HITS
①切片存放于tmp\chatunitest-info\`项目名称`\methodSlice中

②模型能力过弱可能会导致生成不出slices

## :email: 联系我们

如果您有任何问题或想了解我们的实验结果，请随时通过电子邮件与我们联系，联系方式如下：

1. Corresponding author: `zjuzhichen AT zju.edu.cn`
2. Author: `yh_ch AT zju.edu.cn`









