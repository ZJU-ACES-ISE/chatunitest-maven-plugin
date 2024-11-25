The code below, extracted from method `${method_sig}` in the class `${class_name}`, does not achieve full coverage: when tested, lines ${uncovered_lines} do not execute.

The unit test is
```
${unit_test}
```

The method that not fully covered is
```
${coverage_message}
```


The unit test is testing the method `${method_sig}` in the class `${class_name}`,
the source code of the method under test and its class is:
```
${full_fm}
```
<#if other_method_sigs?has_content>
    ```
    The signatures of other methods in its class are `${other_method_sigs}`
    ```
</#if>

Create new JUnit test functions to execute these missing lines while preserving the previous tests, always making sure that the new test is correct and indeed improves coverage. You can use Junit 5, Mockito 3 and reflection. No explanation is needed.

