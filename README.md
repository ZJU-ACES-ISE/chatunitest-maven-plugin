# ChatUnitest Maven Plugin

Add ChatUnitest to your pom.xml:

```
  <plugin>
    <groupId>edu.zju.cst.aces</groupId>
    <artifactId>chatunitest-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
<!--          You must specify you OpenAI API keys.-->
      <apiKeys>Key1, Key2, ...</apiKeys>
      <selectClass>${selectClass}</selectClass>
      <selectMethod>${selectMethod}</selectMethod>
    </configuration>
  </plugin>
```

## Run
You can run the plugin with the following command:

Generate unit tests for the whole project:
```
mvn chatunitest:project
```

Generate unit tests for the target class:
```
mvn chatunitest:class -DselectClass=className
```

Generate unit tests for the target method:
```
mvn chatunitest:method -DselectMethod=className#methodName
```

## Configuration
You can configure the plugin with the following parameters:
```
  <plugin>
    <groupId>edu.zju.cst.aces</groupId>
    <artifactId>chatunitest-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
<!--          You must specify you OpenAI API keys.-->
      <apiKeys>Key1, Key2, ...</apiKeys>
      <maxRounds>6</maxRounds>
      <minErrorTokens>500</minErrorTokens>
      <model>gpt-3.5-turbo</model>
      <temperature>0.5</temperature>
      <topP>1</topP>
      <frequencyPenalty>0</frequencyPenalty>
      <presencePenalty>0</presencePenalty>
      <project>${project}</project>
      <tmpOutput>${tmpOutput}</tmpOutput>
      <testOutput>${testOutput}</testOutput>
      <selectClass>${selectClass}</selectClass>
      <selectMethod>${selectMethod}</selectMethod>
    </configuration>
  </plugin>
```

## Dependencies
```
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
```

```
<plugin>
<groupId>org.apache.maven.plugins</groupId>
<artifactId>maven-surefire-plugin</artifactId>
<version>3.1.0</version>
</plugin>
```
