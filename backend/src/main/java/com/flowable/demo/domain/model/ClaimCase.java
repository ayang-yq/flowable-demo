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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 理赔案件实体
 */
@Entity
@Table(name = "claim_case")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "policy", "assignedTo", "createdBy", "history", "documents" })
public class ClaimCase {

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ClaimCase))
            return false;
        ClaimCase that = (ClaimCase) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "claim_number", nullable = false, unique = true, length = 50)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @Column(name = "claim_type", nullable = false, length = 50)
    private String claimType;

    @Column(name = "claimed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal claimedAmount;

    @Column(name = "approved_amount", precision = 19, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "incident_description", columnDefinition = "TEXT")
    private String incidentDescription;

    @Column(name = "incident_location", length = 200)
    private String incidentLocation;

    @Column(name = "claimant_name", nullable = false, length = 100)
    private String claimantName;

    @Column(name = "claimant_email", length = 100)
    private String claimantEmail;

    @Column(name = "claimant_phone", length = 20)
    private String claimantPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClaimStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "case_instance_id", length = 64)
    private String caseInstanceId;

    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Builder.Default
    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ClaimHistory> history = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ClaimDocument> documents = new HashSet<>();

    // 理赔类型枚举
    public enum ClaimType {
        VEHICLE("车辆损失"),
        PROPERTY("财产损失"),
        PERSONAL_INJURY("人身伤害"),
        MEDICAL("医疗费用"),
        TRAVEL("旅行中断"),
        OTHER("其他");

        private final String description;

        ClaimType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 严重程度枚举
    public enum Severity {
        LOW("低"),
        MEDIUM("中"),
        HIGH("高"),
        CRITICAL("紧急");

        private final String description;

        Severity(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 理赔状态枚举
    public enum ClaimStatus {
        DRAFT("草稿"),
        SUBMITTED("已提交"),
        UNDER_REVIEW("审核中"),
        INVESTIGATING("调查中"),
        APPROVED("已批准"),
        REJECTED("已拒绝"),
        PAYMENT_PROCESSING("支付处理中"),
        PAID("已支付"),
        CLOSED("已关闭"),
        CANCELLED("已取消");

        private final String description;

        ClaimStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public boolean canTransitionTo(ClaimStatus newStatus) {
            switch (this) {
                case DRAFT:
                    return newStatus == SUBMITTED || newStatus == CANCELLED;
                case SUBMITTED:
                    return newStatus == UNDER_REVIEW || newStatus == CANCELLED;
                case UNDER_REVIEW:
                    return newStatus == INVESTIGATING || newStatus == APPROVED || newStatus == REJECTED
                            || newStatus == CANCELLED;
                case INVESTIGATING:
                    return newStatus == APPROVED || newStatus == REJECTED || newStatus == CANCELLED;
                case APPROVED:
                    return newStatus == PAYMENT_PROCESSING || newStatus == CANCELLED;
                case PAYMENT_PROCESSING:
                    return newStatus == PAID || newStatus == CANCELLED;
                case PAID:
                    return newStatus == CLOSED;
                case REJECTED:
                case CLOSED:
                case CANCELLED:
                    return false;
                default:
                    return false;
            }
        }
    }

    // 业务方法
    public void assignTo(User user) {
        if (!canAssign()) {
            throw new IllegalStateException("Case cannot be assigned in current status: " + status);
        }
        this.assignedTo = user;
        addHistory("ASSIGNED", "Case assigned to " + user.getFullName(), user);
    }

    public void updateStatus(String newStatus, String description, User performedBy) {
        ClaimStatus statusEnum = ClaimStatus.valueOf(newStatus);
        if (!this.status.canTransitionTo(statusEnum)) {
            throw new IllegalStateException("Cannot transition from " + this.status + " to " + statusEnum);
        }
        ClaimStatus oldStatus = this.status;
        this.status = statusEnum;
        addHistory("STATUS_CHANGED",
                String.format("Status changed from %s to %s: %s", oldStatus, statusEnum, description),
                performedBy);
    }

    public boolean canAssign() {
        return status == ClaimStatus.SUBMITTED || status == ClaimStatus.UNDER_REVIEW
                || status == ClaimStatus.INVESTIGATING;
    }

    public boolean canUpdate() {
        return status == ClaimStatus.DRAFT;
    }

    public boolean canSubmit() {
        return status == ClaimStatus.DRAFT;
    }

    public boolean canApprove() {
        return status == ClaimStatus.UNDER_REVIEW || status == ClaimStatus.INVESTIGATING;
    }

    public boolean canReject() {
        return status == ClaimStatus.UNDER_REVIEW || status == ClaimStatus.INVESTIGATING;
    }

    public boolean canClose() {
        return status == ClaimStatus.PAID || status == ClaimStatus.REJECTED;
    }

    public boolean canCancel() {
        return status != ClaimStatus.CLOSED && status != ClaimStatus.CANCELLED;
    }

    // 添加历史记录
    public void addHistory(String action, String description, User performedBy) {
        ClaimHistory historyItem = ClaimHistory.builder()
                .claim(this)
                .action(action)
                .description(description)
                .performedBy(performedBy)
                .build();
        history.add(historyItem);
    }

    // 添加文档
    public void addDocument(ClaimDocument document) {
        document.setClaim(this);
        documents.add(document);
    }

    // 移除文档
    public void removeDocument(ClaimDocument document) {
        documents.remove(document);
        document.setClaim(null);
    }
}
