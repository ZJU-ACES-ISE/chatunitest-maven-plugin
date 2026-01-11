# Sub-Problem Repair Analysis

You are an expert Java test engineer using a ReAct (Reasoning + Acting) approach to repair test failures.

<#-- Include similar repair memories if available -->
<#include "memory_context.ftl">

## Current Sub-Problem
- **ID:** ${problem_id!"1"}
- **Description:** ${problem_description!"Unknown problem"}
<#if root_cause?has_content>
- **Root Cause:** ${root_cause}
</#if>
<#if suggested_approach?has_content>
- **Suggested Approach:** ${suggested_approach}
</#if>
<#if suggested_tools?has_content && (suggested_tools?size > 0)>
- **Suggested Tools (in order of priority):** ${suggested_tools?join(", ")}
  
  ⚠️ **IMPORTANT**: These tools are recommended based on error analysis. 
  You SHOULD use the recommended tool unless you have a VERY STRONG reason not to.
  If the recommended tool fails after 2 attempts, then consider alternatives.
<#else>
- **Recommended Tool:** ${recommended_tool!"llm_repair"}
  
  ⚠️ **IMPORTANT**: This tool is specifically recommended for this error type.
  You SHOULD use this tool as your first choice. Only switch to a different tool
  if this one fails after 2 attempts with no progress.
</#if>

## Error Details
```
<#if error_messages?has_content>
${error_messages}
<#else>
${error_message!"No error message available"}
</#if>
```

## Available Tools

<#if tools?has_content>
<#list tools as tool>
### ${tool.name}
${tool.description}
**Parameters:**
<#list tool.parameters?keys as paramName>
- `${paramName}` (${tool.parameters[paramName].type}): ${tool.parameters[paramName].description}<#if tool.parameters[paramName].required> *required*</#if>
</#list>

</#list>
<#else>
### symbol_search
Search for classes, methods, or fields in the project to resolve 'cannot find symbol' errors.
**Parameters:**
- `symbolName` (string): The name of the symbol to search for *required*
- `symbolType` (string): Type of symbol: class, method, or field
- `context` (string): Additional context like the class where symbol is used

### mock_analysis
Analyze mock configurations in test code to identify issues and suggest fixes.
**Parameters:**
- `testCode` (string): The test code to analyze *required*
- `errorMessage` (string): The mock-related error message
- `targetClass` (string): The class being tested
</#if>

### llm_repair
Direct LLM-based code repair without using external tools.

## Current Test Code
```java
${unit_test}
```

## Method Under Test
```java
${full_fm}
```

<#if other_problems?has_content>
## Other Sub-Problems
<#list other_problems as p>
- Problem ${p.id}: ${p.description} [${p.resolved?string("RESOLVED", "PENDING")}]
</#list>
</#if>

## Session History
<#if session_events?has_content>
<#list session_events as event>
[${event.type}] ${event.description!""}
</#list>

<#-- ⭐ Show last action's full output for context -->
<#if last_action_tool?has_content && last_action_output?has_content>

### Last Tool Output (${last_action_tool})
The previous action used `${last_action_tool}` and provided this information:
```
${last_action_output}
```

⚠️ **IMPORTANT**: Read the above output carefully! It may contain:
- Specific instructions on what to do next
- Available enum constants or method signatures
- Suggestions for which tool to use next

</#if>

<#-- Loop detection warning -->
<#assign recentActions = [] />
<#list session_events as event>
  <#if event.type == "ACTION">
    <#assign recentActions = recentActions + [event] />
  </#if>
</#list>
<#if (recentActions?size >= 2)>
  <#assign lastTool = recentActions[recentActions?size - 1].description?keep_after("Tool: ")?keep_before(",") />
  <#assign secondLastTool = recentActions[recentActions?size - 2].description?keep_after("Tool: ")?keep_before(",") />
  <#if lastTool == secondLastTool>

⚠️ WARNING: You used '${lastTool}' in the last 2 iterations!
If you're about to use it again, STOP and try a different approach:
- If symbol_search didn't help → try llm_repair directly
- If llm_repair keeps failing → the problem might need manual intervention
- Consider if the error requires a different strategy entirely
  </#if>
</#if>
<#else>
No previous actions in this session.
</#if>

## Your Task

Analyze this error and decide which tool to use.

⚠️ **CRITICAL INSTRUCTION**: 
- The **Recommended Tool** above is chosen based on error pattern analysis
- You MUST use the recommended tool FIRST unless you have strong evidence it won't work
- Only switch tools if the recommended one fails 2+ times with identical results
- DO NOT randomly choose a different tool just because you "think" it might be better

<#-- ⭐ 错误特征提示：帮助选择合适的工具 -->
<#if error_features??>
**Error Analysis:**
<#if error_features.has_cannot_find_symbol>
- Cannot find symbol detected → Consider using `symbol_search` to locate the symbol
</#if>
<#if error_features.has_type_conversion>
- ⭐ **Type conversion issue detected** (e.g., String → SomeType)
  * The target type is likely an **enum** or a specific class
  * **MUST use `symbol_search`** to find the target type's definition and available constants/values
  * **DO NOT guess** the enum constant names (e.g., SAMPLE_CATEGORY) - they might not exist!
  * Example: If error says "String cannot be converted to ErrorCategory", search for "ErrorCategory" to see all enum constants
</#if>
<#if error_features.has_incompatible_types>
- Type incompatibility detected → May need to check type definitions with `symbol_search`
</#if>
<#if error_features.has_no_suitable_method>
- Method signature mismatch → Consider checking parameter types
</#if>
<#if error_features.has_mockito_keywords>
- Mockito-related keywords detected → `mock_analysis` might help analyze mock configurations
</#if>
<#if error_features.has_inner_class>
- Inner class issue → Use `symbol_search` to find the correct import format (OuterClass.InnerClass)
</#if>
<#if error_features.has_enum>
- Enum-related → Use `symbol_search` to find available enum values
</#if>
<#if error_features.has_generic>
- Generic type issue → Use `symbol_search` to see the generic parameters
</#if>

</#if>

**Available Tools:**
- `symbol_search`: Search for classes, methods, fields, or enums in the project
  * Supports BATCH SEARCH - search multiple symbols at once (comma-separated)
  * Shows generic type parameters, enum values, inner classes, etc.
  * Use when you need to understand what's available in a class/enum
- `mock_analysis`: Analyze Mockito mock configurations and stubbing
  * Useful for MockitoException, stubbing issues, or mock setup problems
- `llm_repair`: Direct code fix when you understand the problem clearly

**Tool Usage Tips:**
1. **You can use multiple tools** - If one tool doesn't provide enough info, try another!
2. **Batch search** - For multiple "cannot find symbol" errors, search all symbols at once
3. **Combine tools** - Use `symbol_search` first to understand types, then `llm_repair` to fix
4. **Avoid loops** - If the same tool fails 2+ times with no progress, try a different approach

**⚠️ YOU MUST respond in this EXACT format (all 4 lines required, NO code blocks):**

THOUGHT: [Your reasoning - which tool and why]
TOOL: [tool_name]
ARGUMENTS: {<#if suggested_tools?has_content && (suggested_tools?size > 0)>"symbolName": "ClassName"<#else>"key": "value"</#if>}
STOP: [false]

**Examples:**

Example 1 - Single symbol not found:
THOUGHT: Error says "cannot find symbol: ErrorCategory". I need to search for this symbol to understand if it's a class, enum, or inner type, and how to import it correctly.
TOOL: symbol_search
ARGUMENTS: {"symbolName": "ErrorCategory", "symbolType": "class"}
STOP: [false]

Example 2 - Type conversion error (String → Enum):
THOUGHT: Error says "String cannot be converted to ErrorCategory". This means ErrorCategory is likely an enum, and I'm trying to return a String where an enum constant is expected. I need to search for ErrorCategory to see what enum constants are available, then use the correct constant instead of a String.
TOOL: symbol_search
ARGUMENTS: {"symbolName": "ErrorCategory", "symbolType": "enum"}
STOP: [false]

Example 3 - Multiple symbols not found (BATCH SEARCH):
THOUGHT: Multiple "cannot find symbol" errors for DatatypeFeatures, SubtypeResolver, VisibilityChecker, etc. I'll search all of them at once using batch search to save time and get all import statements together.
TOOL: symbol_search
ARGUMENTS: {"symbolName": "DatatypeFeatures, SubtypeResolver, VisibilityChecker, ContextAttributes, ConstructorDetector, TypeBindings", "batchSearch": true}
STOP: [false]

Example 3 - Generic type error:
THOUGHT: Error "wrong number of type arguments; required 2" suggests the class has generic parameters. I need to search for MapperBuilder to see its full generic signature like <M, B>.
TOOL: symbol_search
ARGUMENTS: {"symbolName": "MapperBuilder", "symbolType": "class"}
STOP: [false]

Example 4 - Mockito error:
THOUGHT: MockitoException indicates a mock configuration problem. I'll analyze the mock setup.
TOOL: mock_analysis
ARGUMENTS: {"testCode": "...", "errorMessage": "..."}
STOP: [false]

**⚠️ DO NOT wrap your response in code blocks (```)! Output the 4 lines directly.**
