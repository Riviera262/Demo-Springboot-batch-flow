package org.example.transactionsmanagement.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResult {

    private String batchId;
    private long totalInitRows;
    private long activeCount;
    private long delCount;
    private String status;
    private String message;
}
