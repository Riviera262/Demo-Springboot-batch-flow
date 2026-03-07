package org.example.transactionsmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionsmanagement.dto.transaction.ApprovalRequest;
import org.example.transactionsmanagement.dto.transaction.ApprovalResult;
import org.example.transactionsmanagement.dto.uploadBatch.BatchStatusResponse;
import org.example.transactionsmanagement.dto.transactionUpl.ExcelUploadResponse;
import org.example.transactionsmanagement.entity.UploadBatch;
import org.example.transactionsmanagement.repository.MbTransactionUplRepository;
import org.example.transactionsmanagement.repository.UploadBatchRepository;
import org.example.transactionsmanagement.utils.BatchIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadBatchService {

    //For Upload excel files
    private final BatchIdGenerator batchIdGenerator;
    private final UploadBatchRepository uploadBatchRepository;
    private final TransactionUplAsyncService transactionUplAsyncService;

    //For approval batches
    private final ApprovalBatchService approvalBatchService;
    private final MbTransactionUplRepository mbTransactionUplRepository;

    @Value("${app.upload.dir:./uploads}")
    public String uploadDir;

    @Transactional
    public ExcelUploadResponse excelUpload(List<MultipartFile> files, String username) {
        //Validate files input
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }
        if (files.size() > 11) {
            throw new IllegalArgumentException("Maximun 10 files per batch upload");
        }

        String batchId = batchIdGenerator.generateBatchId();
        log.info("Starting upload for batch: {} by user:{} ", batchId, username);

        Path batchFolder = null;
        try {
            //Create batch folder
            batchFolder = createBatchFolder(batchId);
            log.info("Created batch folder: {}", batchFolder);

            //Save files to folder and count how many success upload file
            int saveCount = saveFilesToTheFolder(files, batchFolder);
            log.info("Saved {} files to folder {} ", files, batchFolder);

            //If no file saved
            if (saveCount == 0) {
                throw new IllegalArgumentException("No valid Excel file found");
            }

            //Create a batch in UPLOAD_BATCH db
            UploadBatch uploadBatch = UploadBatch.builder()
                    .batchId(batchId)
                    .uploadedBy(username)
                    .uploadTime(LocalDateTime.now())
                    .status("PENDING")
                    .fileCount(saveCount)
                    .fileStoragePath(batchFolder.toString())
                    .totalRows(0L)
                    .successRows(0L)
                    .failedRows(0L)
                    .build();

            //Created UPLOAD_BATCH record
            uploadBatchRepository.saveAndFlush(uploadBatch);
            log.info("Created UploadBatch record for batch: {}", batchId);

            //Trigger async service to process those files(run in background)
            transactionUplAsyncService.processExcelBatch(batchId);
            log.info("Triggered async processing for batch: {}", batchId);

            //Return response for user, system got the files user uploaded, and system will process them
            return ExcelUploadResponse.builder()
                    .batchId(batchId)
                    .fileCount(saveCount)
                    .status("PENDING")
                    .message("Files uploaded successfully. System will process them, user can go do anything else while waiting")
                    .uploadTime(LocalDateTime.now())
                    .build();

        } catch (IOException e) {
            log.error("Failed to save files to batch: {}", batchId, e);

            //Clean up folder if error
            if (batchFolder != null) {
                cleanUpFolderQuietLy(batchFolder);
            }

            throw new RuntimeException("Failed to save uploaded files: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during upload for batch: {} ", batchId, e);
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    //Create folder for the batch upload
    private Path createBatchFolder(String batchId) throws IOException {
        Path batchPath = Paths.get(uploadDir, batchId);
        Files.createDirectories(batchPath);
        return batchPath;
    }

    //Save all the uploaded files to the batch folder
    private int saveFilesToTheFolder(List<MultipartFile> files, Path batchFolder) throws IOException {
        int count = 0;

        for (MultipartFile file : files){
            String originalFilename = file.getOriginalFilename();
            if (file.isEmpty()){
                log.info("Skipping empty excel file: {}", originalFilename);
                continue;
            }

            if (originalFilename == null || !originalFilename.toLowerCase().endsWith("xlsx")){
                log.info("Skipping non excel file: {}", originalFilename);
                continue;
            }

            //Prevent path traversal attack and copy file to batch folder
            String cleanFilename = Paths.get(originalFilename).getFileName().toString();
            Path targetBath = batchFolder.resolve(cleanFilename);
            Files.copy(file.getInputStream(), targetBath, StandardCopyOption.REPLACE_EXISTING);

            count++;
        }

        return count;
    }


    //Clean folder
    private void cleanUpFolderQuietLy(Path folder){
        try {
            if (Files.exists(folder)) {
                Files.walk(folder)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path ->{
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException ignored) {
        }
    }

    //Get batch by batchId
    @Transactional(readOnly = true)
    public BatchStatusResponse getBatchByBatchId(String batchId) {
        UploadBatch batch = uploadBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        return mapToResponse(batch);
    }

    //Get batch by date range and batch status
    @Transactional(readOnly = true)
    public List<BatchStatusResponse> getBatchesByDateRangeAndStatus(LocalDateTime startTime, LocalDateTime endTime, String status){
        List<UploadBatch> batches;

        //If these is a status
        if (status != null && !status.isBlank()) {
            batches = uploadBatchRepository.findByUploadTimeBetweenAndStatusOrderByUploadTimeDesc(startTime, endTime, status);
        } else {
            //If status is not selected
            batches = uploadBatchRepository.findByUploadTimeBetweenOrderByUploadTimeDesc(startTime, endTime);
        }

        return batches.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    //Get batch by uploadedBy (username)
    @Transactional(readOnly = true)
    public List<BatchStatusResponse> getBatchesByUploader(String username){

        return uploadBatchRepository.findByUploadedByOrderByUploadTimeDesc(username)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    //Batch status response
    private BatchStatusResponse mapToResponse(UploadBatch batch){

        return BatchStatusResponse.builder()
                .batchId(batch.getBatchId())
                .uploadedBy(batch.getUploadedBy())
                .uploadTime(batch.getUploadTime())
                .status(batch.getStatus())
                .fileCount(batch.getFileCount())
                .totalRows(batch.getTotalRows())
                .successRows(batch.getSuccessRows())
                .failedRows(batch.getFailedRows())
                .startProcessTime(batch.getStartProcessTime())
                .endProcessTime(batch.getEndProcessTime())
                .resultMessage(batch.getResultMessage())
                .approvedBy(batch.getApprovedBy())
                .approvedAt(batch.getApprovedAt())
                .approvedRows(batch.getApprovedRows())
                .rejectedRows(batch.getRejectedRows())
                .approvalMessage(batch.getApprovalMessage())
                .build();
    }


    //Approval batches in a range date
    public List<ApprovalResult> processBatchApprovals(ApprovalRequest request, String approvedBy){
        List<String> targetBatchIds = request.getBatchId();

        if (targetBatchIds == null || targetBatchIds.isEmpty()){
            if (request.getStartTime() == null || request.getEndTime() == null){
                throw new IllegalArgumentException("Must provide either batchID or a Date range(startTime and endTime");
            }

            log.info("Fetching batches to approve between {} and {}", request.getStartTime(), request.getEndTime());
        }

        //Find all the batch in range date with status 'COMPLETED'
        List<UploadBatch> batchesInRande = uploadBatchRepository.findByUploadTimeBetweenAndStatusOrderByUploadTimeDesc(
                request.getStartTime(),
                request.getEndTime(),
                "COMPLETED"
        );

        targetBatchIds = batchesInRande.stream()
                .map(UploadBatch::getBatchId)
                .toList();

        //If no batch found
        if (targetBatchIds.isEmpty()){
            log.info("No batches found for approval in the date range");
            return List.of();
        }

        //Locked selected target batches to avoid another approver try to approve them at the same time
        int lockedCount = uploadBatchRepository.markAsApproving(targetBatchIds);
        log.info("Successfully locked {}/{} batches for approval: ", lockedCount, targetBatchIds.size());

        if (lockedCount == 0){
            log.warn("Could not lock any batches. They may processed by someone else");
            return List.of();
        }

        List<ApprovalResult> results = new ArrayList<>();

        //Check each batch and process them separately
        for (String batchId : targetBatchIds){
            try {
                ApprovalResult result = approvalBatchService.approveSingleBatch(batchId, approvedBy);
                results.add(result);

                if ("APPROVED".equals(result.getStatus())) {
                    uploadBatchRepository.markAsApproved(
                            batchId,
                            approvedBy,
                            result.getActiveCount(),
                            result.getDelCount(),
                            result.getMessage()
                    );
                } else {
                    //If unexpected error happens, mark it as 'APPROVED_FAILED'
                    uploadBatchRepository.updateStatusByBatchId(batchId, "APPROVED_FAILED");
                    log.error("Unexpected error happend when system trying to approved the batch");
                }
            } catch (Exception e){
                //If unexpected error happens when processing the batch, mark it as 'COMPLETED' so we can try it again later
                log.error("Failed to process approval for the batch {}", batchId, e);
                uploadBatchRepository.updateStatusByBatchId(batchId, "COMPLETED");

                results.add(ApprovalResult.builder()
                        .batchId(batchId)
                        .status("failed")
                        .message("Unexpected error: " + e.getMessage())
                        .build());
            }
        }

        return results;
    }

    public ApprovalResult approveSingleBatch(String batchId, String approvedBy){
        log.info("Requesting approval for batch {}", batchId);

        int lockedRows = uploadBatchRepository.markAsApproving(List.of(batchId));

        if (lockedRows == 0){
            log.warn("Could not lock batch. It may processed by someone else");
            return ApprovalResult.builder()
                    .batchId(batchId)
                    .status("FAILED_APPROVED")
                    .message("Batch may already got processed by someone else")
                    .build();
        }
        try {
            ApprovalResult result = approvalBatchService.approveSingleBatch(batchId, approvedBy);

            if ("APPROVED".equals(result.getStatus())){
                uploadBatchRepository.markAsApproved(
                        batchId,
                        approvedBy,
                        result.getActiveCount(),
                        result.getDelCount(),
                        result.getMessage()
                );
            } else {
                uploadBatchRepository.updateStatusByBatchId(batchId, "APPROVED_FAILED");
                log.error("Unexpected error happened when system trying to approve batch {}", batchId);
            }
            return result;

        } catch (Exception e){
            log.error("Failed to approve batch {}", batchId);

            //Restore batch back to origin status "COMPLETED" to retry later
            uploadBatchRepository.updateStatusByBatchId(batchId, "COMPLETED");
            return ApprovalResult.builder()
                    .batchId(batchId)
                    .status("FAILED")
                    .message("Unexpected error: " + e.getMessage())
                    .build();
        }
    }

    //Recover stuck batches with status 'APPROVING'
    public String recoverStuckBatches(String approvedBy) {
        log.info("Finding stuck batches with status 'APPROVING'...");

        //Find all the stuck batches with status 'APPROVING'
        List<UploadBatch> stuckBatches = uploadBatchRepository.findByStatus("APPROVING");

        if (stuckBatches.isEmpty()) {
            return "System have 0 stuck batches. No stuck batches found";
        }

        //Number of batch got recover as status 'APPROVED'
        int recoveredAsApproved = 0;

        //Number of batch got recover as status 'COMPLETED'
        int resetToCompleted = 0;

        for (UploadBatch batch : stuckBatches) {
            String batchId = batch.getBatchId();

            //Check is there any 'INIT' rows in batch
            long initRowsCount = mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "INIT");

            if (initRowsCount == 0) {
                // Case 1: If there are no 'INIT' row left in batch need to process, approve. Then mark that batch status as 'APPROVED' and update approved, rejected rows number
                long activeCount = mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "ACTIVE");
                long delCount = mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "DEL");

                uploadBatchRepository.markAsApproved(
                        batchId,
                        approvedBy,
                        activeCount,
                        delCount,
                        "Recovered and synced after system crash"
                );
                recoveredAsApproved++;
                log.info("Restore {} done(status: APPROVED)", batchId);

            } else {
                //Case 2: if there still have 'INIT' rows in batch. Update batch status to 'COMPLETED' to retry later
                uploadBatchRepository.updateStatusByBatchId(batchId, "COMPLETED");
                resetToCompleted++;
                log.info("Recovered {} back to status 'COMPLETED', wait for approve again later", batchId);
            }
        }

        String resultMsg = String.format("recoverd done: " + recoveredAsApproved + " got 'APPROVED'. " + resetToCompleted + " got reset to status 'COMPLETED'");
        log.info(resultMsg);
        return resultMsg;
    }
}