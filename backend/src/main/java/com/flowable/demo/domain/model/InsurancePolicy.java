package com.flowable.demo.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 保险保单实体
 */
@Entity
@Table(name = "insurance_policy")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false, of = {"id"})
public class InsurancePolicy {
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InsurancePolicy)) return false;
        InsurancePolicy that = (InsurancePolicy) o;
        return getId() != null && getId().equals(that.getId());
    }
    
    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "policy_number", unique = true, nullable = false, length = 50)
    private String policyNumber;
    
    @Column(name = "policy_holder_name", nullable = false, length = 100)
    private String policyHolderName;
    
    @Column(name = "policy_holder_phone", length = 20)
    private String policyHolderPhone;
    
    @Column(name = "policy_holder_email", length = 100)
    private String policyHolderEmail;
    
    @Column(name = "policy_type", nullable = false, length = 50)
    private String policyType;
    
    @Column(name = "coverage_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal coverageAmount;
    
    @Column(name = "premium_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal premiumAmount;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 检查保单是否有效
     */
    public boolean isActive() {
        LocalDate now = LocalDate.now();
        return "ACTIVE".equals(status) && 
               !now.isBefore(startDate) && 
               !now.isAfter(endDate);
    }
    
    /**
     * 检查保单是否过期
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate);
    }
    
    /**
     * 检查理赔金额是否在保额范围内
     */
    public boolean isWithinCoverage(BigDecimal claimAmount) {
        return claimAmount != null && 
               claimAmount.compareTo(BigDecimal.ZERO) > 0 && 
               claimAmount.compareTo(coverageAmount) <= 0;
    }
    
    /**
     * 获取保单剩余天数
     */
    public long getRemainingDays() {
        if (isExpired()) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate);
    }
}
