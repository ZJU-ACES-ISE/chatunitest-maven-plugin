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
