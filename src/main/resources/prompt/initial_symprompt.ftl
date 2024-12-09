The focal method is `${method_sig}` in the focal class `${class_name}`, and their information is
```${full_fm}```.
<#if other_method_sigs?has_content>
    Signatures of Other methods in the focal class are `${other_method_sigs}`.
</#if>
<#list c_deps?keys as key>
    The brief information of dependent class `${key}` is
    ```${c_deps[key]}```.
</#list>
<#list m_deps?keys as key>
    The brief information of dependent class `${key}` is
    ```${m_deps[key]}```.
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

