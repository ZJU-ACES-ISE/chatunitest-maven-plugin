<#-- Template for including similar repair memories in the prompt -->
<#if similarMemories?? && (similarMemories?size > 0)>

========================================
RELEVANT REPAIR HISTORY
========================================

I have found ${similarMemories?size} similar repair case(s) from previous successful repairs in this project.
These may provide useful patterns and approaches for the current error:

<#list similarMemories as memory>

--- Repair Case ${memory_index + 1} (Similarity: ${memory.score?string("0.00")}) ---

Class: ${memory.entry.fullClassName}
Method: ${memory.entry.methodName}
Error Type: ${memory.entry.errorType}
Original Error: ${memory.entry.originalErrorMessage?truncate(200, "...")}

Sub-problems Identified:
<#if memory.entry.subProblems?? && (memory.entry.subProblems?size > 0)>
<#list memory.entry.subProblems as problem>
  ${problem_index + 1}. ${problem}
</#list>
<#else>
  (Direct repair without decomposition)
</#if>

Repair Steps Taken:
<#if memory.entry.repairSteps?? && (memory.entry.repairSteps?size > 0)>
<#list memory.entry.repairSteps as step>
  Step ${step.stepNumber}: ${step.subProblemDescription}
    - Tool: ${step.toolUsed}
    - Result: ${step.wasSuccessful?then("✓ Success", "✗ Failed")}
    - Change: ${step.codeChangeSummary}
</#list>
<#else>
  (No detailed steps recorded)
</#if>

Success Reason: ${memory.entry.successReason}

Original Test Code (excerpt):
```java
${memory.entry.originalTestCode?truncate(300, "\n... (truncated) ...")}
```

Repaired Test Code (excerpt):
```java
${memory.entry.repairedTestCode?truncate(300, "\n... (truncated) ...")}
```

</#list>

IMPORTANT: These are reference cases to learn from. Your current situation may differ,
so adapt the approaches as needed rather than copying blindly.

========================================

</#if>

