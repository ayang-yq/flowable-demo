package com.flowable.demo.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "保单DTO")
public class InsurancePolicyDTO {
    
    @Schema(description = "保单ID")
    private String id;
    
    @Schema(description = "保单号")
    private String policyNumber;
    
    @Schema(description = "投保人")
    private String policyholderName;
    
    @Schema(description = "保单类型")
    private String policyType;
    
    @Schema(description = "保额")
    private Double coverageAmount;
    
    @Schema(description = "保费")
    private Double premium;
    
    @Schema(description = "生效日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;
    
    @Schema(description = "到期日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;
    
    @Schema(description = "保单状态")
    private String status;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getPolicyNumber() {
        return policyNumber;
    }
    
    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }
    
    public String getPolicyholderName() {
        return policyholderName;
    }
    
    public void setPolicyholderName(String policyholderName) {
        this.policyholderName = policyholderName;
    }
    
    public String getPolicyType() {
        return policyType;
    }
    
    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }
    
    public Double getCoverageAmount() {
        return coverageAmount;
    }
    
    public void setCoverageAmount(Double coverageAmount) {
        this.coverageAmount = coverageAmount;
    }
    
    public Double getPremium() {
        return premium;
    }
    
    public void setPremium(Double premium) {
        this.premium = premium;
    }
    
    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }
    
    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
    
    public LocalDate getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
