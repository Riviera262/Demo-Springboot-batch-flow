package org.example.transactionsmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionsmanagement.dto.transactionUpl.ProcessingResult;
import org.example.transactionsmanagement.entity.UploadBatch;
import org.example.transactionsmanagement.repository.UploadBatchRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionUplAsyncService {
    private final UploadBatchRepository uploadBatchRepository;
    private final ChunkProcessService chunkProcessService;

    @Qualifier("fileProcessorExecutor")
    private final Executor fileProcessorExecutor;

    @Async("excelExecutor")
    public void processExcelBatch(String batchId){
        log.info("Starting parallel processing for batch: {}", batchId);
        LocalDateTime startTime = LocalDateTime.now();

        try {
            //Mark batch status as 'PROCESSING'
            updateStatus(batchId, "PROCESSING" ,LocalDateTime.now(), null);
            log.info("Batch {} status had marked as 'PROCESSING'", batchId);

            //Load batch info
            UploadBatch batch = uploadBatchRepository.findById(batchId)
                    .orElseThrow(() -> new RuntimeException("Batch not fount: " + batchId));

            List<Path> excelFiles = getExcelFiles(batch.getFileStoragePath());

            if (excelFiles.isEmpty()){
                throw new RuntimeException("No excel file found in the batch folder");
            }

            //Batch Results counter
            AtomicLong totalRows = new AtomicLong();
            AtomicLong successRows = new AtomicLong();
            AtomicLong failedRows = new AtomicLong();
            Set<String> globalSeenTraces = ConcurrentHashMap.newKeySet();

            //Process all files parallel
            List<CompletableFuture<Void>> futures = excelFiles.stream()
                    .map(file -> processSingleFile(
                            batchId,
                            batch.getUploadedBy(),
                            file,
                            totalRows,
                            successRows,
                            failedRows,
                            globalSeenTraces
                    ))
                    .toList();

            CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            ).join();

            completeBatch(batchId, startTime, excelFiles.size(),totalRows.get() ,successRows.get(), failedRows.get());

        } catch (Exception e) {
            log.error("Fatal error processing batch: {}", batchId, e);
            updateStatus(batchId, "FAILED", LocalDateTime.now(), e.getMessage());
        }
    }

    //Getting every single file in the folder and give the file path location
    private List<Path> getExcelFiles(String folderPath) throws IOException {
        try (Stream<Path> paths = Files.list(Paths.get(folderPath))){
            return paths
                    .filter(p -> p.toString().toLowerCase().endsWith("xlsx"))
                    .sorted()
                    .toList();
        }
    }

    //Process every single file in a batch (1 thread of fileProcessor = 1 file)
    private CompletableFuture<Void> processSingleFile(
            String batchId,
            String uploadedBy,
            Path file,
            AtomicLong totalRows,
            AtomicLong successRows,
            AtomicLong failedRows,
            Set<String> globalSeenTraces
    ){
        return CompletableFuture.runAsync(() -> {
            String filename = file.getFileName().toString();
            String threadName = Thread.currentThread().getName();

            try {
                log.info("[{}] Processing: {}", threadName, filename);
                ProcessingResult result = chunkProcessService.processExcelFile(batchId, file, uploadedBy, globalSeenTraces);

                //Result total, success, failed of that file
                totalRows.addAndGet(result.getTotalRows());
                successRows.addAndGet(result.getSuccessRows());
                failedRows.addAndGet(result.getFailedRows());
            } catch (Exception e){
                log.info("[{}] error while processing file: {}", threadName, filename, e);
            }
        }, fileProcessorExecutor);
    }

    //Conclude the result of the batch
    @Transactional
    private void completeBatch(
            String batchId,
            LocalDateTime startTime,
            int fileCount,
            long totalRows,
            long successRows,
            long failedRows
    ){
        LocalDateTime endTime = LocalDateTime.now();
        long duration = Duration.between(startTime, endTime).getSeconds();

        String resultMessage = String.format(
                "Processed %d files in %d seconds. Total %d rows(%d success, %d failed)",
                fileCount,
                duration,
                totalRows,
                successRows,
                failedRows
        );

        uploadBatchRepository.markAsCompleted(
            batchId,
            endTime,
            totalRows,
            successRows,
            failedRows,
            resultMessage
        );

        log.info("batch {} completed in: {} second", batchId, duration);
    }

    //Update upload batch status(because modify requires @transactional)
    @Transactional
    public void updateStatus(String batchId, String status, LocalDateTime time, String message) {
        if ("PROCESSING".equals(status)) {
            uploadBatchRepository.markedAsProcessing(batchId, time);
        } else if ("FAILED".equals(status)) {
            uploadBatchRepository.markedAsFailed(batchId, time, message);
        }
    }
}
