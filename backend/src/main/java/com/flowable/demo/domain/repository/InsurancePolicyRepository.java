package com.flowable.demo.domain.repository;

import com.flowable.demo.domain.model.InsurancePolicy;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 保险保单仓储接口
 */
@Repository
public interface InsurancePolicyRepository extends BaseRepository<InsurancePolicy, UUID> {
    
    /**
     * 根据保单号查找保单
     */
    Optional<InsurancePolicy> findByPolicyNumber(String policyNumber);
    
    /**
     * 根据状态查找保单
     */
    List<InsurancePolicy> findByStatus(String status);
    
    /**
     * 根据保单类型查找保单
     */
    List<InsurancePolicy> findByPolicyType(String policyType);
    
    /**
     * 根据投保人姓名查找保单
     */
    List<InsurancePolicy> findByPolicyHolderNameContaining(String policyHolderName);
    
    /**
     * 查找指定日期范围内的有效保单
     */
    @Query("SELECT p FROM InsurancePolicy p WHERE p.status = 'ACTIVE' AND p.startDate <= :date AND p.endDate >= :date")
    List<InsurancePolicy> findActivePoliciesAtDate(@Param("date") LocalDate date);
    
    /**
     * 查找即将过期的保单（30天内）
     */
    @Query("SELECT p FROM InsurancePolicy p WHERE p.status = 'ACTIVE' AND p.endDate BETWEEN :today AND :expiryDate")
    List<InsurancePolicy> findPoliciesExpiringSoon(@Param("today") LocalDate today, 
                                                   @Param("expiryDate") LocalDate expiryDate);
    
    /**
     * 查找已过期的保单
     */
    @Query("SELECT p FROM InsurancePolicy p WHERE p.status = 'ACTIVE' AND p.endDate < :today")
    List<InsurancePolicy> findExpiredPolicies(@Param("today") LocalDate today);
    
    /**
     * 根据投保人身份证号或姓名模糊查找
     */
    @Query("SELECT p FROM InsurancePolicy p WHERE LOWER(p.policyHolderName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.policyNumber LIKE CONCAT('%', :keyword, '%')")
    List<InsurancePolicy> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 统计各类型保单数量
     */
    @Query("SELECT p.policyType, COUNT(p) FROM InsurancePolicy p GROUP BY p.policyType")
    List<Object[]> countByPolicyType();
    
    /**
     * 统计各状态保单数量
     */
    @Query("SELECT p.status, COUNT(p) FROM InsurancePolicy p GROUP BY p.status")
    List<Object[]> countByStatus();
}
