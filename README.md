* 需要加入依赖
```
<plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.7</version>
        <executions>
          <execution>
            <goals>
              <goal>prepare-agent</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
```
* mvn clean install安装插件至本地仓库，指定maven本地仓库路径
* 配置
```
<plugin>
    <groupId>com.hhh.maven_plugin</groupId>
    <artifactId>chatunitest-maven-plugin</artifactId>
    <version>1.2.3</version>
    <configuration>
        <target>D:\\coverage</target>
            <mavenHome>
            C:\\software\\apache-maven-3.9.2
            </mavenHome>
    </configuration>
</plugin>
            ```