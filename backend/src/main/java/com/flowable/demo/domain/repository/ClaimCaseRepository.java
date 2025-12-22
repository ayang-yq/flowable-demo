package com.flowable.demo.domain.repository;

import com.flowable.demo.domain.model.ClaimCase;
import com.flowable.demo.domain.model.InsurancePolicy;
import com.flowable.demo.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 理赔案件仓储接口
 */
@Repository
public interface ClaimCaseRepository extends JpaRepository<ClaimCase, UUID>, BaseRepository<ClaimCase, UUID> {

        /**
         * 根据理赔编号查找案件
         */
        Optional<ClaimCase> findByClaimNumber(String claimNumber);

        /**
         * 根据 Case 实例 ID 查找案件
         */
        Optional<ClaimCase> findByCaseInstanceId(String caseInstanceId);

        /**
         * 根据状态查找案件
         */
        List<ClaimCase> findByStatus(ClaimCase.ClaimStatus status);

        /**
         * 根据状态查找案件（分页）
         */
        Page<ClaimCase> findByStatus(ClaimCase.ClaimStatus status, Pageable pageable);

        /**
         * 根据状态查找案件（按创建时间降序）
         */
        List<ClaimCase> findByStatusOrderByCreatedAtDesc(ClaimCase.ClaimStatus status);

        /**
         * 根据状态统计数量
         */
        long countByStatus(ClaimCase.ClaimStatus status);

        /**
         * 根据分配人查找案件
         */
        List<ClaimCase> findByAssignedTo(User assignedTo);

        /**
         * 根据分配人查找案件（分页）
         */
        Page<ClaimCase> findByAssignedTo(User assignedTo, Pageable pageable);

        /**
         * 根据分配人查找案件（按创建时间降序）
         */
        List<ClaimCase> findByAssignedToOrderByCreatedAtDesc(User assignedTo);

        /**
         * 根据分配人ID查找案件
         */
        List<ClaimCase> findByAssignedToId(UUID assignedToId);

        /**
         * 根据创建人查找案件
         */
        List<ClaimCase> findByCreatedBy(User createdBy);

        /**
         * 根据保单查找案件
         */
        List<ClaimCase> findByPolicy(InsurancePolicy policy);

        /**
         * 根据保单查找案件（分页）
         */
        Page<ClaimCase> findByPolicy(InsurancePolicy policy, Pageable pageable);

        /**
         * 根据保单ID查找案件
         */
        List<ClaimCase> findByPolicyId(UUID policyId);

        /**
         * 根据严重性查找案件
         */
        List<ClaimCase> findBySeverity(ClaimCase.Severity severity);

        /**
         * 根据严重性统计数量
         */
        long countBySeverity(ClaimCase.Severity severity);

        /**
         * 根据理赔类型查找案件
         */
        List<ClaimCase> findByClaimType(String claimType);

        /**
         * 查找指定用户的待处理案件
         */
        @Query("SELECT c FROM ClaimCase c WHERE c.assignedTo.id = :userId AND (c.status = 'SUBMITTED' OR c.status = 'UNDER_REVIEW' OR c.status = 'INVESTIGATING')")
        List<ClaimCase> findPendingClaimsForUser(@Param("userId") UUID userId);

        /**
         * 查找指定时间段内创建的案件
         */
        @Query("SELECT c FROM ClaimCase c WHERE c.createdAt BETWEEN :startDate AND :endDate")
        List<ClaimCase> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * 统计指定时间后创建的案件数量
         */
        long countByCreatedAtAfter(LocalDateTime dateTime);

        /**
         * 查找理赔金额大于指定金额的案件
         */
        List<ClaimCase> findByClaimedAmountGreaterThan(BigDecimal amount);

        /**
         * 查找理赔金额在指定范围内的案件
         */
        @Query("SELECT c FROM ClaimCase c WHERE c.claimedAmount BETWEEN :minAmount AND :maxAmount")
        List<ClaimCase> findByClaimedAmountBetween(@Param("minAmount") BigDecimal minAmount,
                        @Param("maxAmount") BigDecimal maxAmount);

        /**
         * 根据理赔人姓名模糊查找
         */
        @Query("SELECT c FROM ClaimCase c WHERE LOWER(c.claimantName) LIKE LOWER(CONCAT('%', :name, '%'))")
        List<ClaimCase> findByClaimantNameContaining(@Param("name") String name);

        /**
         * 统计各状态案件数量
         */
        @Query("SELECT c.status, COUNT(c) FROM ClaimCase c GROUP BY c.status")
        List<Object[]> countByStatusGroup();

        /**
         * 统计指定用户的案件数量
         */
        @Query("SELECT COUNT(c) FROM ClaimCase c WHERE c.assignedTo.id = :userId")
        long countByAssignedToId(@Param("userId") UUID userId);

        /**
         * 查找需要审核的案件
         */
        @Query("SELECT c FROM ClaimCase c WHERE c.status = 'UNDER_REVIEW' ORDER BY c.severity DESC, c.createdAt ASC")
        List<ClaimCase> findClaimsNeedingReview();

        /**
         * 查找需要支付的案件
         */
        List<ClaimCase> findByStatusAndClaimedAmountGreaterThanOrderByCreatedAtAsc(ClaimCase.ClaimStatus status,
                        BigDecimal amount);

        /**
         * 根据关键词搜索案件（分页）
         */
        @Query("SELECT c FROM ClaimCase c WHERE " +
                        "LOWER(c.claimNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.claimantName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.claimantPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.incidentLocation) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<ClaimCase> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        /**
         * 查找我的案件（我创建的或分配给我的）
         */
        @Query("SELECT c FROM ClaimCase c WHERE c.createdBy.id = :userId OR c.assignedTo.id = :userId")
        Page<ClaimCase> findMyClaimCases(@Param("userId") UUID userId, Pageable pageable);

        /**
         * 计算平均处理时间（天）
         */
        @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 86400) FROM claim_case WHERE status IN ('PAID', 'REJECTED', 'CLOSED')", nativeQuery = true)
        Double getAverageProcessingDays();

        /**
         * 计算总理赔申请金额
         */
        @Query("SELECT SUM(c.claimedAmount) FROM ClaimCase c")
        Double getTotalClaimedAmount();
}
