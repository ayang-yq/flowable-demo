package com.flowable.demo.admin.service;

import com.flowable.demo.domain.repository.ClaimCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Case Migration Service - for fixing existing cases without businessKey
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseMigrationService {

    private final CmmnRuntimeService cmmnRuntimeService;
    private final ClaimCaseRepository claimCaseRepository;

    /**
     * Fix existing cases by adding businessKey information to variables
     * 
     * Note: Flowable CMMN doesn't support updating businessKey after creation.
     * This method stores the claim number as a variable for existing cases.
     */
    @Transactional
    public Map<String, Object> fixExistingCasesBusinessKey() {
        log.info("Starting migration to fix businessKey for existing cases...");
        
        Map<String, Object> result = new HashMap<>();
        int fixedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        
        // Get all active case instances for insuranceClaimCase
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("insuranceClaimCase")
                .list();
        
        log.info("Found {} active case instances to process", caseInstances.size());
        
        for (CaseInstance caseInstance : caseInstances) {
            try {
                // Check if businessKey is already set
                if (caseInstance.getBusinessKey() != null && !caseInstance.getBusinessKey().isEmpty()) {
                    log.debug("Case {} already has businessKey: {}, skipping", 
                            caseInstance.getId(), caseInstance.getBusinessKey());
                    skippedCount++;
                    continue;
                }
                
                // Get claimCaseId from variables
                String claimCaseId = (String) cmmnRuntimeService.getVariable(caseInstance.getId(), "claimCaseId");
                if (claimCaseId == null) {
                    log.warn("Case {} has no claimCaseId variable, skipping", caseInstance.getId());
                    skippedCount++;
                    continue;
                }
                
                // Find the claim case to get the claim number
                java.util.UUID claimId = java.util.UUID.fromString(claimCaseId);
                var claimCase = claimCaseRepository.findById(claimId).orElse(null);
                if (claimCase == null) {
                    log.warn("ClaimCase not found for ID: {}, skipping case {}", claimCaseId, caseInstance.getId());
                    skippedCount++;
                    continue;
                }
                
                // Store claimNumber as a variable (since businessKey cannot be updated)
                Map<String, Object> variables = cmmnRuntimeService.getVariables(caseInstance.getId());
                if (!variables.containsKey("claimNumber") || variables.get("claimNumber") == null) {
                    cmmnRuntimeService.setVariable(caseInstance.getId(), "claimNumber", claimCase.getClaimNumber());
                    fixedCount++;
                    log.info("Added claimNumber variable '{}' to case {}", claimCase.getClaimNumber(), caseInstance.getId());
                } else {
                    skippedCount++;
                    log.debug("Case {} already has claimNumber variable: {}, skipping", 
                            caseInstance.getId(), variables.get("claimNumber"));
                }
                
            } catch (Exception e) {
                errorCount++;
                log.error("Error processing case {}: {}", caseInstance.getId(), e.getMessage(), e);
            }
        }
        
        result.put("totalCases", caseInstances.size());
        result.put("fixedCount", fixedCount);
        result.put("skippedCount", skippedCount);
        result.put("errorCount", errorCount);
        result.put("message", String.format("Migration completed. Fixed: %d, Skipped: %d, Errors: %d", 
                fixedCount, skippedCount, errorCount));
        
        log.info("Migration completed: {}", result.get("message"));
        
        return result;
    }
}
