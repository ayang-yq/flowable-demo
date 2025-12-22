package com.flowable.demo.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 理赔文档实体
 */
@Entity
@Table(name = "claim_document")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "claim" })
public class ClaimDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private ClaimCase claim;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "document_name", nullable = false, length = 200)
    private String documentName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    // 文档类型常量
    public static final String TYPE_PHOTO = "PHOTO";
    public static final String TYPE_REPORT = "REPORT";
    public static final String TYPE_RECEIPT = "RECEIPT";
    public static final String TYPE_ID_CARD = "ID_CARD";
    public static final String TYPE_POLICY = "POLICY";
    public static final String TYPE_MEDICAL_RECORD = "MEDICAL_RECORD";
    public static final String TYPE_POLICE_REPORT = "POLICE_REPORT";
    public static final String TYPE_OTHER = "OTHER";

    // Custom equals and hashCode to prevent infinite recursion
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ClaimDocument))
            return false;
        ClaimDocument that = (ClaimDocument) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}
