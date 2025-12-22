# 理赔决策表规则

本文件记录了理赔决策 (ClaimDecisionTable.dmn) 中定义的规则。

## 输入变量

| ID | 名称 | 类型 |
|---|---|---|
| policyType | 保单类型 | string |
| claimedAmount | 理赔金额 | double |
| coverageAmount | 保额 | double |
| claimType | 报案类别 | string |
| severity | 严重性 | string |

## 输出变量

| ID | 名称 | 类型 |
|---|---|---|
| paymentMethod | 赔付方式 | string |
| needInvestigation | 是否升级调查 | boolean |
| needManualReview | 是否触发人工审核 | boolean |
| approvalLevel | 审批级别 | string |
| priority | 优先级 | string |
| claimComplexity | 案件复杂度 | string |

## 规则详情

| 规则 | 描述 | 保单类型 | 理赔金额 | 报案类别 | 严重性 | 赔付方式 | 是否升级调查 | 是否触发人工审核 | 审批级别 | 优先级 | 案件复杂度 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 车险小额理赔，直接处理 | "车险" | <= 10000 | | "LOW" | "快速赔付" | false | false | "自动" | "普通" | "simple" |
| 2 | 车险大额理赔，需要审核 | "车险" | > 10000 and <= 50000 | | "MEDIUM" | "标准赔付" | false | true | "主管" | "重要" | "simple" |
| 3 | 车险超大额理赔，需要调查和高级审核 | "车险" | > 50000 | | | "分级赔付" | true | true | "经理" | "紧急" | "complex" |
| 4 | 财产险小额理赔 | "财产险" | <= 20000 | | "LOW" | "标准赔付" | false | true | "主管" | "普通" | "simple" |
| 5 | 财产险中额理赔 | "财产险" | > 20000 and <= 100000 | | "MEDIUM" | "分级赔付" | true | true | "经理" | "重要" | "complex" |
| 6 | 财产险大额理赔 | "财产险" | > 100000 | | | "分级赔付" | true | true | "总监" | "紧急" | "complex" |
| 7 | 人身险小额理赔 | "人身险" | <= 30000 | | "LOW" | "快速赔付" | false | true | "主管" | "普通" | "simple" |
| 8 | 人身险中额理赔 | "人身险" | > 30000 and <= 200000 | | "MEDIUM" | "分级赔付" | true | true | "经理" | "重要" | "complex" |
| 9 | 人身险大额理赔 | "人身险" | > 200000 | | | "分级赔付" | true | true | "总监" | "紧急" | "complex" |
| 10 | 盗窃案件，必须调查 | | | "盗窃" | | "标准赔付" | true | true | "经理" | "重要" | "complex" |
| 11 | 自然灾害案件 | | | "自然灾害" | | "分级赔付" | true | true | "经理" | "紧急" | "complex" |
| 12 | 高严重性案件 | | | | "HIGH", "CRITICAL" | "分级赔付" | true | true | "总监" | "紧急" | "complex" |
| 13 | 超出保额的案件 | | > coverageAmount | | | "按保额赔付" | true | true | "经理" | "重要" | "complex" |
| 14 | 默认规则 | | | | | "标准赔付" | false | true | "主管" | "普通" | "simple" |
