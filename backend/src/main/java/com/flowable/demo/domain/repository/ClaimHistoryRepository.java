package com.flowable.demo.domain.repository;

import com.flowable.demo.domain.model.ClaimHistory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 理赔历史记录仓储接口
 */
@Repository
public interface ClaimHistoryRepository extends BaseRepository<ClaimHistory, UUID> {
    
    /**
     * 根据案件 ID 查找历史记录
     */
    List<ClaimHistory> findByClaimId(UUID claimId);
    
    /**
     * 根据案件 ID 查找历史记录（按时间降序）
     */
    List<ClaimHistory> findByClaimIdOrderByPerformedAtDesc(UUID claimId);
    
    /**
     * 根据案件 ID 和操作类型查找历史记录
     */
    List<ClaimHistory> findByClaimIdAndAction(UUID claimId, String action);
    
    /**
     * 根据操作人查找历史记录
     */
    List<ClaimHistory> findByPerformedById(UUID performedById);
    
    /**
     * 根据操作类型查找历史记录
     */
    List<ClaimHistory> findByAction(String action);
    
    /**
     * 查找指定时间段内的历史记录
     */
    @Query("SELECT h FROM ClaimHistory h WHERE h.performedAt BETWEEN :startDate AND :endDate")
    List<ClaimHistory> findByPerformedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * 根据案件 ID 和时间范围查找历史记录
     */
    @Query("SELECT h FROM ClaimHistory h WHERE h.claim.id = :claimId AND h.performedAt BETWEEN :startDate AND :endDate ORDER BY h.performedAt DESC")
    List<ClaimHistory> findByClaimIdAndPerformedAtBetween(@Param("claimId") UUID claimId,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);
    
    /**
     * 统计各操作类型的数量
     */
    @Query("SELECT h.action, COUNT(h) FROM ClaimHistory h GROUP BY h.action")
    List<Object[]> countByAction();
    
    /**
     * 查找案件的最新状态变更记录
     */
    @Query("SELECT h FROM ClaimHistory h WHERE h.claim.id = :claimId AND h.action = 'STATUS_CHANGED' ORDER BY h.performedAt DESC")
    List<ClaimHistory> findLatestStatusChanges(@Param("claimId") UUID claimId);
}
