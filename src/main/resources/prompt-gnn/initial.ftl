The focal method is `${method_sig}` in the focal class `${class_name}`, and their information is
```${full_fm}```.
<#if other_method_sigs?has_content>
    Signatures of Other methods in the focal class are `${other_method_sigs}`.
</#if>

<#list predict_deps?keys as key>
    The brief code of dependent class `${key}` is
    ```${predict_deps[key]}```.
</#list>

Follow these steps to generate the test case, starting each step with ${'<STEP>'}:
1. **Initialize dependent classes:** Write simple initialization statements for the objects of all dependent classes.
2. **Initialize the focal class:** Use the class constructor to create the focal class object. Use mock objects for any dependencies not initialized in the first step.
3. **Review and revise:** Carefully review your initialization code for completeness and correctness, especially the private access. If issues are found during the review, revise the code and provide the updated version.
4. **Write the test class:** Create a complete test class that ensures full branch and line coverage of the focal method.

**Example**:
${'<STEP>'} Initialize dependent classes:
```java
StringBuilder sb = new StringBuilder();
```
${'<STEP>'} Initialize the focal class:
```java
UserServer server = new UserServer(sb);
```
${'<STEP>'} Review and revise:
If no revision is needed, write "No revision is needed.", else provide revised code:
```java
YOUR REVISED CODE
```
${'<STEP>'} Write the test class:
```java
YOUR TEST
```
