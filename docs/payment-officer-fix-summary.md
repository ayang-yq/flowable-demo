# Payment Officer Variable Fix Summary

## Issue Description

When a claim case was approved, the CMMN case workflow triggered the BPMN `ClaimPaymentProcess` to handle payment processing. However, the BPMN process used `${paymentOfficer}` and `${paymentManager}` expressions for task assignment, but these variables were not available in the execution context, causing the following error:

```
ERROR c.flowable.demo.service.CaseService - Failed to complete CMMN task taskFinalApproval: 
Unknown property used in expression: ${paymentOfficer} with Execution[ id '...' ] 
- definition 'ClaimPaymentProcess:4:...' - activity 'userTask_validatePayment'
```

## Root Cause

The CMMN case defines a `processTask` with `taskProcessPayment` that invokes the BPMN `ClaimPaymentProcess`. The CMMN processTask passes variables to BPMN via `<flowable:in>` mappings:

```xml
<cmmn:processTask id="taskProcessPayment" name="Process Claim Payment">
    <cmmn:extensionElements>
        <flowable:in source="amount" target="amount" />
        <flowable:in source="reference" target="reference" />
        <flowable:in source="payeeName" target="payeeName" />
        <flowable:in source="claimId" target="claimId" />
        <flowable:in source="paymentOfficer" target="paymentOfficer" />
        <flowable:in source="paymentManager" target="paymentManager" />
        <flowable:in source="caseInstanceId" target="caseInstanceId" />
        <flowable:out source="paymentStatus" target="paymentStatus" />
    </cmmn:extensionElements>
    <cmmn:processRefExpression><![CDATA[ClaimPaymentProcess]]></cmmn:processRefExpression>
</cmmn:processTask>
```

The BPMN process `ClaimPaymentProcess.bpmn` uses expression language for task assignment:
- `userTask_validatePayment` → `flowable:assignee="${paymentOfficer}"`
- `userTask_confirmPayment` → `flowable:assignee="${paymentOfficer}"`
- `userTask_handleDispute` → `flowable:assignee="${paymentManager}"`

When the CMMN case process completes the `taskFinalApproval` task, it triggers the `taskProcessPayment` stage, which in turn starts the BPMN `ClaimPaymentProcess`. The variables available to the BPMN process are inherited from the CMMN case instance. However, several required variables were not set:
- `paymentOfficer` and `paymentManager` were not set → caused task assignment errors
- `amount`, `reference`, `payeeName` were not set → required by BPMN start event
- `claimId` and `caseInstanceId` were not set → needed for process tracking and updates

## Solution

Modified `backend/src/main/java/com/flowable/demo/service/CaseService.java` in two places:

### 1. Initialize payment variables when starting the case process

In the `startCaseProcess()` method, added initialization of payment-related variables:

```java
// Set other required role variables with default values
variables.put("damageAssessor", "admin");
variables.put("approverGroup", "managers");
variables.put("paymentOfficer", "admin");
variables.put("paymentManager", "admin");
```

### 2. Set all required BPMN parameters when approving a claim

In the `approveClaimCase()` method, added all required parameters for the ClaimPaymentProcess BPMN:

```java
// [FIX] Set all required parameters for the ClaimPaymentProcess BPMN
// These are required by the CMMN processTask which passes them to BPMN
variables.put("paymentOfficer", approvedBy.getUsername());
variables.put("paymentManager", "admin");

// Payment information required by BPMN process
variables.put("amount", approveRequestDTO.getApprovedAmount());
variables.put("reference", "PAY-" + claimCase.getClaimNumber() + "-" + System.currentTimeMillis());
variables.put("payeeName", claimCase.getClaimantName());

// Case information for process tracking
variables.put("claimId", claimCase.getId().toString());
variables.put("caseInstanceId", claimCase.getCaseInstanceId());
```

## Files Modified

- `backend/src/main/java/com/flowable/demo/service/CaseService.java`

## Testing

After applying the fix:
1. Restarted the backend service
2. The claim approval process now successfully triggers the BPMN payment process
3. The `paymentOfficer` variable is properly set to the approving user's username
4. The `paymentManager` variable defaults to "admin"

## Parameter Mapping Summary

| CMMN Variable | BPMN Usage | Source in approveClaimCase() |
|--------------|-----------|----------------------------|
| `amount` | Start event form property, payment execution | `approveRequestDTO.getApprovedAmount()` |
| `reference` | Start event form property, payment execution | Generated: `"PAY-" + claimNumber + "-" + timestamp` |
| `payeeName` | Start event form property, payment execution | `claimCase.getClaimantName()` |
| `claimId` | Notification service | `claimCase.getId().toString()` |
| `caseInstanceId` | Case update service | `claimCase.getCaseInstanceId()` |
| `paymentOfficer` | Task assignment (validate, confirm) | `approvedBy.getUsername()` |
| `paymentManager` | Task assignment (dispute) | `"admin"` (default) |

## Impact

This fix ensures that:
- The CMMN to BPMN integration works correctly
- All required parameters are passed from CMMN to BPMN
- Payment tasks are properly assigned to users
- The BPMN start event receives all required form properties
- The claim payment workflow completes successfully
- No more "Unknown property used in expression" errors
