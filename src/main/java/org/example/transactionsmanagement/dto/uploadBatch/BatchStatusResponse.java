package org.example.transactionsmanagement.dto.uploadBatch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatusResponse {
    private String batchId;
    private String uploadedBy;
    private LocalDateTime uploadTime;

    private Integer fileCount;
    private String status;

    private Long totalRows;
    private Long successRows;
    private Long failedRows;

    private LocalDateTime startProcessTime;
    private LocalDateTime endProcessTime;
    private String resultMessage;

    private String approvedBy;
    private LocalDateTime approvedAt;

    private Long approvedRows;
    private Long rejectedRows;

    private String approvalMessage;
}
