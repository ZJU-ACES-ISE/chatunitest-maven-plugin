Generate unit tests in Java for `${full_class_name}.${method_name}` to achieve 100% line coverage for this method.
Dont use @Before and @After test methods.
Make tests as atomic as possible.
All tests should be for JUnit 5.
In case of mocking, use Mockito 5. But, do not use mocking for all tests.
Name all methods according to the template - [MethodUnderTest][Scenario]Test, and use only English letters.
The source code of method under test is as follows:
```
${full_method_info}
```

Here are the method signatures of classes used by the method under test. Only use these signatures for creating objects, not your own ideas.
<#list dep_class_sigs?keys as key>
    === methods in ${key}:
    ${dep_m_sigs_ano_com[key]}
</#list>

Polymorphism relations:
<#if subClasses??>
    <#list subClasses as subClass>
        ${subClass} is a sub-class of ${full_class_name}.
    </#list>
</#if>
