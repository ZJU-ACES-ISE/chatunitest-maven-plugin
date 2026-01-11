The focal method is `${method_sig}` in the focal class `${class_name}`,
<#if method_invocation_codes_outerclass?has_content>
    Below are examples of existing code in the project where the target method is invoked.
    These snippets can serve as a reference for understanding how the method is used in contexts.
    <#list method_invocation_codes_outerclass as invoke_code>
        ```
        ${invoke_code}
        ```
    </#list>
<#elseif method_invocation_codes_innerclass?has_content>
    Below are examples of existing code in the focal class where the target method is invoked.
    These snippets can serve as a reference for understanding how the method is used in contexts,
    note that you should initialize the focal class object before invoking the method.
    <#list method_invocation_codes_innerclass as invoke_code>
        ```
        ${invoke_code}
        ```
    </#list>
</#if>
Information of the focal method is
```${full_fm}```.
<#if java_doc?has_content>
    The JavaDoc of the focal method is
    ```${java_doc}```
</#if>
<#if other_method_sigs?has_content>
    Signatures of Other methods in the focal class are `${other_method_sigs}`.
</#if>

<#-- ⭐ Enhanced: Provide complete information about dependent classes -->
<#list c_deps?keys as key>
    **Dependent Class: `${key}`**
    
    Class Signature:
    ```${c_deps[key]}```
    
    <#if dep_c_sigs?? && dep_c_sigs[key]??>
    Available Constructors:
    ```
${dep_c_sigs[key]}
    ```
    </#if>
    
    <#if dep_fields?? && dep_fields[key]??>
    Fields:
    ```
${dep_fields[key]}
    ```
    </#if>
    
    <#if dep_gs_sigs?? && dep_gs_sigs[key]??>
    Getter/Setter Methods:
    ```
${dep_gs_sigs[key]}
    ```
    </#if>
    
</#list>

<#-- ⭐ Enhanced: Provide method information for dependent classes -->
<#list m_deps?keys as key>
    **Methods in dependent class `${key}`:**
    ```
${m_deps[key]}
    ```
</#list>
