There is a Java function ```${full_fm}```.
The context of the function is
<#list forward_analysis?keys as key>
    ```${forward_analysis[key]}```
</#list>
<#list backward_analysis?keys as key>
    ```${backward_analysis[key]}```
</#list>
What is the functionality of the function?
The test programs below are designed to test the function `${method_sig}`.
They can cover different part of the function.
The contents of the test programs are ```${counter-examples}```
Please help me generate new test programs that cover different scenarios or edge cases.