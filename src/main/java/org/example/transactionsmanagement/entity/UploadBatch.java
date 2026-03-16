package org.example.transactionsmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "UPLOAD_BATCH")
public class UploadBatch {
    @Id
    @Column(name = "batch_id", length = 50, nullable = false)
    private String batchId;

    @Column(name = "uploaded_by", length = 50, nullable = false)
    private String uploadedBy;

    @Column(name = "upload_time", nullable = false, updatable = false)
    private LocalDateTime uploadTime;

    @Column(name = "file_count", nullable = false)
    private Integer fileCount;

    @Column(name = "file_storage_path", length = 500)
    private String fileStoragePath;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, APPROVING, APPROVED, FAILED_APPROVED, REJECTED, DELETED

    @Column(name = "total_rows", length = 10)
    @Builder.Default
    private Long totalRows = 0L;

    @Column(name = "success_rows", length = 10)
    @Builder.Default
    private Long successRows = 0L;

    @Column(name = "failed_rows", length = 10)
    @Builder.Default
    private Long failedRows = 0L;

    @Column(name = "start_process_time")
    private LocalDateTime startProcessTime;

    @Column(name = "end_process_time")
    private LocalDateTime endProcessTime;

    @Lob
    @Column(name = "result_message", columnDefinition = "CLOB")
    private String resultMessage;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_rows", length = 10)
    @Builder.Default
    private Long approvedRows = 0L;

    @Column(name = "rejected_rows", length = 10)
    @Builder.Default
    private Long rejectedRows = 0L;

    @Lob
    @Column(name = "approval_message", columnDefinition = "CLOB")
    private String approvalMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }
}