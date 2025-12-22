package com.flowable.demo.domain.repository;

import com.flowable.demo.domain.model.ClaimDocument;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 理赔文档仓储接口
 */
@Repository
public interface ClaimDocumentRepository extends BaseRepository<ClaimDocument, UUID> {
    
    /**
     * 根据案件 ID 查找文档
     */
    List<ClaimDocument> findByClaimId(UUID claimId);
    
    /**
     * 根据案件 ID 和文档类型查找文档
     */
    List<ClaimDocument> findByClaimIdAndDocumentType(UUID claimId, String documentType);
    
    /**
     * 根据上传人查找文档
     */
    List<ClaimDocument> findByUploadedById(UUID uploadedById);
    
    /**
     * 根据文档类型查找文档
     */
    List<ClaimDocument> findByDocumentType(String documentType);
    
    /**
     * 统计案件文档数量
     */
    @Query("SELECT COUNT(d) FROM ClaimDocument d WHERE d.claim.id = :claimId")
    long countByClaimId(@Param("claimId") UUID claimId);
    
    /**
     * 统计各类型文档数量
     */
    @Query("SELECT d.documentType, COUNT(d) FROM ClaimDocument d GROUP BY d.documentType")
    List<Object[]> countByDocumentType();
}
