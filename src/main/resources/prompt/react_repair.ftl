# Fix Sub-Problem Using Tool Results

## Sub-Problem
**ID:** ${problem_id!"1"}
**Description:** ${problem_description!"Fix the test error"}

## Tool Findings
**Tool Used:** ${tool_name!"llm_repair"}

<#if tool_output?has_content>
**Tool Output:**
```
${tool_output}
```
</#if>

## Error Messages
```
<#if error_messages?has_content>
${error_messages}
<#else>
${error_message!"No error message available"}
</#if>
```

<#-- ⭐ 修复提示：基于错误特征 -->
<#if error_features??>
**Repair Hints:**
<#if error_features.has_type_conversion>
- Type conversion error detected: Check if the target type is an enum (use enum constants instead of strings)
</#if>
<#if error_features.has_inner_class>
- Inner class: Use format `OuterClass.InnerClass` for imports
</#if>
<#if error_features.has_enum>
- Enum usage: Use enum constants directly (e.g., `EnumName.VALUE`), not string literals
</#if>
<#if error_features.mentioned_types?? && (error_features.mentioned_types?size > 0)>
- Types mentioned: ${error_features.mentioned_types?join(", ")} - Check Tool Output for their definitions
</#if>

</#if>

## Current Test Code
```java
${unit_test}
```

## Method Under Test
**Class:** `${class_name}`
**Method:** `${method_sig}`

**Source Code:**
```java
${full_fm}
```

<#if other_method_sigs?has_content>
## Other Methods in Class
```
${other_method_sigs}
```
</#if>

## Instructions

Based on the tool findings and error analysis, fix the test code to resolve: **${problem_description!"the error"}**

<#if error_type?has_content>
<#if error_type == "CANNOT_FIND_SYMBOL">
**For "cannot find symbol" errors:**
- **CRITICAL:** Read the Tool Output above carefully - it contains the EXACT information you need
- If a class was found, the Tool Output shows:
  * The correct import statement to use
  * **⭐ Generic Parameters** (e.g., `<M, B>`) - MUST be provided when extending/implementing!
  * All available fields in that class
  * All available public methods in that class (with signatures)
  * Inner types (inner classes/enums) if any
- **For Generic Classes:** If Tool Output shows "Generic Parameters: <M, B>":
  * You MUST provide ALL type parameters when extending/using the class
  * Example: `class MyClass extends BaseClass<Type1, Type2>` NOT `BaseClass<Type1>`
  * Read the bounds carefully (e.g., `M extends ObjectMapper` means M must be ObjectMapper or its subclass)
- **If a method/field is not found:** Check the Tool Output for the actual available methods/fields and use the correct name
- **For Inner Classes/Enums:** The Tool Output provides specific instructions - follow them exactly:
  * Use fully qualified name: `OuterClass.InnerClass` (e.g., `RepairMemory.SessionEvent`)
  * Import statement: `import package.OuterClass.InnerClass;` (e.g., `import zju.cst.aces.agent.memory.RepairMemory.SessionEvent;`)
  * NEVER use: `import package.InnerClass;` (this is incorrect)
- **For Lombok @Data classes:** If Tool Output mentions Lombok, use the auto-generated getters shown (e.g., `getFieldName()`)
- **For Batch Search Results:** All symbols are listed together - add ALL required imports at once
- Check for typos by comparing with the exact names shown in Tool Output
<#elseif error_type == "NO_SUITABLE_METHOD">
**For "no suitable method found" errors:**
- Check method parameter types match exactly
- **IMPORTANT - Mock Return Types:** When using `when().thenReturn()`, the return type MUST match the method's return type:
  * If method returns `ErrorCategory` enum, use: `thenReturn(ErrorCategory.COMPILATION_ERROR)` NOT `thenReturn("COMPILATION_ERROR")`
  * If method returns `String`, use: `thenReturn("value")` NOT `thenReturn(SomeEnum.VALUE)`
  * Use the correct enum constant, not a String representation
- Verify argument matchers (any(), eq()) are used correctly
- Check if method is overloaded and you're calling the right version
<#elseif error_type?starts_with("MOCKITO")>
**For Mockito-related errors:**
- Review mock configurations and ensure @Mock annotations are correct
- Ensure mocks are properly initialized with MockitoAnnotations.openMocks()
- Check when/thenReturn statements match actual method signatures
- **Return Type Matching:** Ensure thenReturn() value type matches the mocked method's return type exactly
- Verify you're not mixing argument matchers with raw values
- Consider if mocking is necessary or if real objects can be used
<#elseif error_type == "NULL_POINTER">
**For NullPointerException:**
- Initialize objects properly before use
- Add null checks where needed
- Ensure dependencies are injected correctly
- Check mock return values are properly stubbed
<#elseif error_type == "ASSERTION_ERROR" || error_type == "EXPECTED_BUT_WAS">
**For Assertion errors:**
- Verify expected values match actual behavior
- Consider using actual runtime values from test execution
- Check if the assertion logic is correct
</#if>
</#if>

**General Guidelines:**
- Use JUnit 5 (@Test, @BeforeEach, Assertions.*)
- Use Mockito 3 (@Mock, @InjectMocks, when/thenReturn)
- Use reflection for accessing private members if necessary
- Make minimal changes to fix the specific issue
- Ensure the code compiles

Return ONLY the complete fixed test code:

```java
// Your fixed test code here
```
