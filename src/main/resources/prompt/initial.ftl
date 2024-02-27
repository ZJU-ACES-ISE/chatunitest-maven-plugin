The focal method is `${method_sig}` in focal class `${class_name}`, the information is ```${full_fm}```.
<#if other_method_sigs?has_content>
    Signatures of Other methods in the focal class are `${other_method_sigs}`.
</#if>
You should follow these steps to generate the test, starting each step with the word ${'<INFO>'}:
1. List all necessary dependencies.
2. Think step by step, describe how to prepare all necessary classes and methods for invoking the focal method.
3. Write a complete test class with imports, ensuring to cover all branches and lines in the test.
4. Review your code to ensure all statements are implemented and free from bugs. Provide a revised version if modifications are made during the review.
e.g., "${'<INFO>'} First, import the following dependencies...".
