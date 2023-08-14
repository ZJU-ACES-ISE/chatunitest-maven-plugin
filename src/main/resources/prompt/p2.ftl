The focal method is `${focal_method}` in the focal class `${class_name}`, and their information is
```${information}```.

<#if other_methods?has_content>
    Signatures of Other methods in the focal class are `${other_methods}`.
</#if>
<#list c_deps?keys as key>
    The brief information of dependent class `${key}` is
```${c_deps[key]}```.
</#list>
<#list m_deps?keys as key>
    The brief information of dependent class `${key}` is
    ```${m_deps[key]}```.
</#list>
