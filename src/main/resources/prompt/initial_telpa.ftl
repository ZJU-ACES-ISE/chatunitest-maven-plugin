## Summarize the target method‘s intention
There is a Java function ```${full_fm}```.
The context of the function is
    <#list forward_analysis?keys as key>
        ```${forward_analysis[key]}```
    </#list>
    <#list backward_analysis?keys as key>
        ```${backward_analysis[key]}```
    </#list>
What is the functionality of the function?

## Generating test case
The test programs below are designed to test the function `${method_sig}`.
They can cover different part of the function.
    <#if counter_examples?has_content>
    The contents of the test programs are ```${counter_examples}```
    </#if>
Please help me generate new test programs that cover different scenarios or edge cases.