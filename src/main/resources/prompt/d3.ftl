The focal method is `${focal_method}` in the focal class `${class_name}`, and their information is
```${full_fm }```.

<#list c_deps?keys as key>
    The brief information of dependent class `${key}` is
```${c_deps[key]}```.
</#list>
<#list m_deps?keys as key>
    The brief information of dependent class `${key}` is
    ```${m_deps[key]}```.
</#list>
