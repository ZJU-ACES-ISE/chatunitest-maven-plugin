The focal method is `${method_sig}` in the focal class `${class_name}`, and their information is
```${full_fm}```.
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
The paths you need to cover are:
<#if minPaths?has_content>
    ```
    //import sentence...
    public class MethodTest {
    <#list minPaths as path>
        @Test
        public void methodTest${path_index+1}(){//cover the path when:${path}

        }
    </#list>

    }
    ```
</#if>

