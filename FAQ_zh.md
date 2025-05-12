# ChatUniTest 常见问题 (FAQ)

以下是一些关于 ChatUniTest 的常见问题。

1.  **问：什么是 ChatUniTest？**
    **答：** ChatUniTest 是一个工具/框架，旨在通过利用大型语言模型 (LLM) 帮助开发人员为其 Java 代码生成单元测试。它旨在简化测试创建过程并提高代码覆盖率。`chatunitest-core` 库提供核心功能，而 `chatunitest-maven-plugin` 允许轻松集成到 Maven 构建过程中。

2.  **问：如何开始使用 ChatUniTest？**

    **答：** 开始使用的主要方法是在项目的 `pom.xml` 文件中配置 `chatunitest-maven-plugin`（https://github.com/ZJU-ACES-ISE/chatunitest-maven-plugin）。该插件将使用 `chatunitest-core`。

   * **配置 Maven 插件：** 在您的 `pom.xml` 中添加并配置 `chatunitest-maven-plugin`。这包括设置 API 密钥、选择模型以及其他必要的参数。有关详细的配置选项和目标，请参阅我们的 Maven 插件使用指南。
   * 您需要配置对 LLM 的访问权限（请参阅下一个 FAQ）。

3.  **问：如何为大型语言模型 (LLM) 配置我的 API 密钥？**
    **答：** ChatUniTest 需要 API 密钥才能访问 LLM 以生成测试。这些密钥应在 `pom.xml` 文件中 `chatunitest-maven-plugin` 的 `<configuration>` 部分内进行配置，特别是在 `<apiKeys>` 标记内。

    **示例：**
    ```xml
    <configuration>
        <apiKeys>
            <openaiKey>您的OPENAI_API密钥</openaiKey>
        </apiKeys>
    </configuration>
    ```
    将 `您的OPENAI_API密钥` 替换为您的实际 API 密钥。如果使用不同的 LLM 提供程序，请参阅插件文档以了解特定的密钥名称。

4.  **问：目前支持哪些 LLM 模型，我可以添加其他模型吗？**
    **答：** ChatUniTest 内置支持多种模型。截至最新更新，这些模型包括：
   * `gpt-3.5-turbo`
   * `gpt-3.5-turbo-1106`
   * `gpt-4o`
   * `gpt-4o-mini`
   * `gpt-4o-mini-2024-07-18`
   * `code-llama`
   * `codeqwen:v1.5-chat`

    如果您希望使用上面未列出的模型，可以通过修改 `chatunitest-core` 项目中 `src/main/java/zju/cst/aces/api/config/Model.java` 文件来手动添加其配置（例如 API 端点 URL）。然后，您需要重新构建核心库。

5.  **问：我收到关于 `chatunitest-core` 或 `chatunitest-maven-plugin` 的“未找到版本”错误。可能是什么问题？**

    **答：** 如果您使用了不正确的 `groupId`，通常会发生这种情况。请确保您使用的是正确且当前的 `groupId`：

   * **正确的 `groupId`：** `io.github.zju-aces-ise`
   * **不正确/过时的 `groupId`：** `io.github.ZJU-ACES-ISE` (注意大小写)

    使用过时的 `groupId` 将导致“未找到版本”或“未找到构件”错误。请务必仔细检查您的 `pom.xml` 或依赖项声明。

6.  **问：`tmpOutput` 目录位于何处？我在我的项目目录中找不到它。**
    **答：** `tmpOutput` 目录包含测试构建过程中生成的信息，默认情况下创建在项目所在磁盘分区的根目录中，而不是直接在项目根文件夹内。例如，如果您的项目位于 `/Users/mike/IdeaProjects/MyProject` (macOS 或 Linux) 或 `D:\Projects\MyProject` (Windows)，则 `tmpOutput` 目录可能分别创建在 `/tmpOutput` 或 `D:\tmpOutput`。

7.  **问：我遇到了与[常见问题，例如 API 身份验证、测试生成失败]相关的错误。常见的故障排除步骤有哪些？**
    **答：** 如果遇到错误，请尝试以下操作：

   * **检查 API 密钥和 LLM 配置：** 确保您的 LLM API 密钥在 `<apiKeys>` 标记中正确设置、有效并且具有足够的配额。验证您的 LLM 端点和模型名称配置。
   * **检查输入代码：** 确保您要为其生成测试的 Java 代码在语法上是正确的并且可以访问。
   * **检查日志：** 在控制台输出中查找详细的错误消息。使用 `-X` (调试) 或 `-e` (错误) 运行 Maven 可以提供更多详细信息。
   * **验证 `tmpOutput` 位置：** 如果错误与文件操作有关，请检查 `tmpOutput` 目录（请参阅 FAQ #6）。
   * **查阅文档：** 查看我们文档的相关部分，以获取针对特定功能或错误的故障排除提示。
   * **报告问题：** 如果问题仍然存在，请在我们的 GitHub Issues 页面上提出问题，并提供尽可能详细的信息，包括错误消息、配置的相关部分以及重现问题的步骤。