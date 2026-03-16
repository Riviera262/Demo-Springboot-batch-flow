package org.example.transactionsmanagement.controller;

import lombok.RequiredArgsConstructor;
import org.example.transactionsmanagement.dto.transaction.ApprovalRequest;
import org.example.transactionsmanagement.dto.transaction.ApprovalResult;
import org.example.transactionsmanagement.dto.uploadBatch.BatchStatusResponse;
import org.example.transactionsmanagement.dto.transactionUpl.ExcelUploadResponse;
import org.example.transactionsmanagement.service.UploadBatchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/upload-batches")
@RequiredArgsConstructor
public class UploadBatchController {

    private final UploadBatchService uploadBatchService;

    // Upload excel files
    @PostMapping
    @PreAuthorize("hasAnyRole('UPLOADER','ADMIN')")
    public ResponseEntity<ExcelUploadResponse> upload(
            @RequestParam("files") List<MultipartFile> files,
            Principal principal
    ) {
        String username = principal.getName();
        return ResponseEntity.ok(
                uploadBatchService.excelUpload(files, username)
        );
    }

    // Get batch stat by batchId
    @GetMapping("/{batchId}")
    @PreAuthorize("hasAnyRole('UPLOADER','APPROVER','ADMIN')")
    public ResponseEntity<BatchStatusResponse> getBatchByBatchId(
            @PathVariable String batchId
    ) {
        return ResponseEntity.ok(
                uploadBatchService.getBatchByBatchId(batchId)
        );
    }

    //Search batches by date range and status
    //Example: http://localhost:8081/api/upload-batches?start=2026-03-01T00:00:00&end=2026-03-07T23:59:59
    @GetMapping
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public ResponseEntity<List<BatchStatusResponse>> searchBatch(
            // Thêm @DateTimeFormat để tránh lỗi 400 khi parse ngày tháng từ URL
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(
                uploadBatchService.getBatchesByDateRangeAndStatus(start, end, status)
        );
    }

    //Get your own batches
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('UPLOADER','ADMIN')")
    public ResponseEntity<List<BatchStatusResponse>> getMyBatches(
            Principal principal
    ) {
        return ResponseEntity.ok(
                uploadBatchService.getBatchesByUploader(principal.getName())
        );
    }

    //Approve selected batch
    @PostMapping("/{batchId}/approve")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public ResponseEntity<ApprovalResult> approveSingleBatch(
            @PathVariable String batchId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                uploadBatchService.approveSingleBatch(batchId, principal.getName()));
    }

    //Approve batches with range date(startTime and endTime)
    @PostMapping("/batches/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<List<ApprovalResult>> approveBatches(
            @RequestBody ApprovalRequest request,
            Principal principal
    ){
        return ResponseEntity.ok(
                uploadBatchService.processBatchApprovals(request, principal.getName())
        );
    }

    //Recover batches with status 'APPROVING'
    @PostMapping("/recover-approving-batches")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> recoverApprovingBatches(Principal principal) {
        return ResponseEntity.ok(
                uploadBatchService.recoverApprovingBatches(principal.getName())
        );
    }

    @PostMapping("/recover-processing-batches")
    @PreAuthorize("hasRole ('ADMIN')")
    public ResponseEntity<String> recoverProcessingBatches(Principal principal){
        return ResponseEntity.ok(
                uploadBatchService.recoverProcessingBatches(principal.getName())
        );
    }
}