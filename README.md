# :mega: ChatUnitest Maven Plugin

## Run

**You can run the plugin with the following command:**

**Generate unit tests for the target method:**

```shell
mvn chatunitest:method -DselectMethod=className#methodName
```

**Generate unit tests for the target class:**

```shell
mvn chatunitest:class -DselectClass=className
```

Example:

```
public class Example {
    public void method1(Type1 p1, ...) {...}
    public void method2() {...}
    ...
}
```

To test the class `Example` and all methods in it:

```shell
mvn chatunitest:class -DselectClass=Example
```

To test the method `method1` in the class `Example` (Now ChatUnitest will generate tests for all methods named method1 in the class)

```shell
mvn chatunitest:method -DselectMethod=Example#method1
```

**Generate unit tests for the whole project:**

:warning: :warning: :warning: For a large project, it may consume a significant number of tokens, resulting in a
substantial bill.

```shell
mvn chatunitest:project
```

**Clean the generated tests:**

```shell
mvn chatunitest:clean
```
Running this command will delete all generated tests and restore your test folder.

**Note:** When running generated tests, ChatUnitest will backup your test folder and restore it when finished.
You can use the following command to restore the test folder manually: `mvn chatunitest:restore`

## Configuration

You can configure the plugin with the following parameters:

```xml

<plugin>
    <groupId>edu.zju.cst.aces</groupId>
    <artifactId>chatunitest-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- Required: You must specify your OpenAI API keys. -->
        <apiKeys></apiKeys>
        <maxRounds>6</maxRounds>
        <minErrorTokens>500</minErrorTokens>
        <model>gpt-3.5-turbo</model>
        <temperature>0.5</temperature>
        <topP>1</topP>
        <frequencyPenalty>0</frequencyPenalty>
        <presencePenalty>0</presencePenalty>
        <selectClass>${selectClass}</selectClass>
        <selectMethod>${selectMethod}</selectMethod>
        <tmpOutput>${tmpOutput}</tmpOutput>
        <testOutput>${testOutput}</testOutput>
        <project>${project}</project>
    </configuration>
</plugin>
```

**Here's a detailed explanation of each configuration option:**

- `apiKeys`: (**Required**) Your OpenAI API keys. Example: `Key1, Key2, ...`.
- `selectClass`: (**Required**) The name of the class to be tested. Example: `Class1`.
- `selectMethod`: (**Required**) The class and method name of the method to be tested. Example: `Class1#method1`.
- `maxRounds`: (**Optional**) The maximum rounds of the repair process. Default: `6`.
- `minErrorTokens`: (**Optional**) The minimum tokens of error message in the repair process. Default: `500`.
- `model`: (**Optional**) The OpenAI model. Default: `gpt-3.5-turbo`.
- `temperature`: (**Optional**) The OpenAI API parameters. Default: `0.5`.
- `topP`: (**Optional**) The OpenAI API parameters. Default: `1`.
- `frequencyPenalty`: (**Optional**) The OpenAI API parameters. Default: `0`.
- `presencePenalty`: (**Optional**) The OpenAI API parameters. Default: `0`.
- `tmpOutput`: (**Optional**) The output path for parsed information. Default: `/tmp/chatunitest-info`.
- `testOutput`: (**Optional**) The output path for tests generated by `chatunitest`. Default: `{basedir}/chatunitest`.
- `project`: (**Optional**) The target project path. Default: `{basedir}`.

## Dependencies

```xml

<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-api</artifactId>
        <version>5.8.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>3.12.4</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>3.12.4</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-inline</artifactId>
        <version>3.12.4</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

```xml

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.1.0</version>
</plugin>
```

## :construction: TODO

- Add code obfuscation to avoid sending the original code to ChatGPT.
- Add expense estimation and quota.
- Optimize the structure of generated test cases.

## MISC

Our work has been submitted to arXiv. Check it out here: [ChatUniTest](https://arxiv.org/abs/2305.04764).

```
@misc{xie2023chatunitest,
      title={ChatUniTest: a ChatGPT-based automated unit test generation tool}, 
      author={Zhuokui Xie and Yinghao Chen and Chen Zhi and Shuiguang Deng and Jianwei Yin},
      year={2023},
      eprint={2305.04764},
      archivePrefix={arXiv},
      primaryClass={cs.SE}
}
```

## :email: Contact us

If you have any questions or would like to inquire about our experimental results, please feel free to contact us via
email. The email addresses of the authors are as follows:

1. Corresponding author: `zjuzhichen AT zju.edu.cn`
2. Author: `yh_ch AT zju.edu.cn`, `xiezhuokui AT zju.edu.cn`