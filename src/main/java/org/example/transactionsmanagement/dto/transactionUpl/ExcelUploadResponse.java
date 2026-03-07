package org.example.transactionsmanagement.dto.transactionUpl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelUploadResponse {
    private String batchId;             //Give user batchId to search history of the upload
    private Integer fileCount;          //Tell user how many files that system had received
    private String status;              //Current status of the upload
    private String message;             //Output message to complete controller duty and for user know
    private LocalDateTime uploadTime;   //Time got the upload
}
