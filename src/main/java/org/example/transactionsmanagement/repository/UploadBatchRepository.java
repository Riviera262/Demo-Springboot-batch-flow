package org.example.transactionsmanagement.repository;

import org.example.transactionsmanagement.entity.UploadBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UploadBatchRepository extends JpaRepository<UploadBatch, String> {

    List<UploadBatch> findByUploadedByOrderByUploadTimeDesc(String uploadedBy);
    //Find batches by status
    List<UploadBatch> findByStatus(String status);

    //Find batches by date range
    //Output all batches by date range with uploadTime
    List<UploadBatch> findByUploadTimeBetweenOrderByUploadTimeDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    //Find batches by date range and status
    //Output all batches by date range with the status request
    List<UploadBatch> findByUploadTimeBetweenAndStatusOrderByUploadTimeDesc(
            LocalDateTime start,
            LocalDateTime end,
            String status
    );

    //Update batch status
    @Modifying
    @Transactional
    @Query("UPDATE UploadBatch ub SET ub.status = :status, ub.updatedAt = CURRENT_TIMESTAMP WHERE ub.batchId = :batchId")
    int updateStatusByBatchId(
        @Param("batchId") String batchId,
        @Param("status") String status
    );

    //Mark batch status as 'PROCESSING' and set startTime
    @Modifying
    @Transactional
    @Query("Update UploadBatch ub SET ub.status = 'PROCESSING', ub.startProcessTime = :startTime, ub.updatedAt = CURRENT_TIMESTAMP WHERE ub.batchId = :batchId")
    int markedAsProcessing (
        @Param("batchId") String batchId,
        @Param("startTime") LocalDateTime startTime
    );

    @Modifying
    @Transactional
    @Query("Update UploadBatch ub SET ub.status = 'FAILED', ub.endProcessTime = :endProcessTime, ub.resultMessage = :errorMessage, ub.updatedAt = CURRENT_TIMESTAMP WHERE ub.batchId = :batchId")
    int markedAsFailed (
            @Param("batchId") String batchId,
            @Param("endProcessTime") LocalDateTime endProcessTime,
            @Param("errorMessage") String errorMessage
    );

    //Target multi batch and mark their status as 'APPROVING' to prevent any other party touch them while processing
    @Modifying
    @Transactional
    @Query("UPDATE UploadBatch ub SET ub.status = 'APPROVING', ub.updatedAt = CURRENT_TIMESTAMP WHERE ub.batchId IN :batchIds AND ub.status = 'COMPLETED'")
    int markAsApproving(@Param("batchIds") List<String> batchIds);

    //Mark as 'APPROVED' and conclude all the result approval of a batch
    @Modifying
    @Transactional
    @Query("UPDATE UploadBatch ub SET " +
            "ub.status = 'APPROVED', " +
            "ub.approvedAt = CURRENT_TIMESTAMP, " +
            "ub.approvedBy = :approvedBy, " +
            "ub.approvedRows = :approvedRows, " +
            "ub.rejectedRows = :rejectedRows, " +
            "ub.updatedAt = CURRENT_TIMESTAMP, " +
            "ub.approvalMessage = :approvalMessage " +
            "WHERE ub.batchId = :batchId AND ub.status = 'APPROVING'")
    int markAsApproved(@Param("batchId") String batchId,
                       @Param("approvedBy") String approveUsername,
                       @Param("approvedRows") Long approvedRows,
                       @Param("rejectedRows") Long rejectedRows,
                       @Param("approvalMessage") String approvalMessage);

    //Mark batch status as 'COMPLETED' and conclude all the results of a batch
    @Modifying
    @Transactional
    @Query("UPDATE UploadBatch ub SET " +
            "ub.status = 'COMPLETED', " +
            "ub.endProcessTime = :endTime, " +
            "ub.totalRows = :totalRows, " +
            "ub.successRows = :successRows, " +
            "ub.failedRows = :failedRows, "+
            "ub.resultMessage = :resultMessage, "+
            "ub.updatedAt = CURRENT_TIMESTAMP "+
            "WHERE ub.batchId = :batchId")
    int markAsCompleted(
        @Param("batchId") String batchId,
        @Param("endTime") LocalDateTime endTime,
        @Param("totalRows") Long totalRows,
        @Param("successRows") Long successRows,
        @Param("failedRows") Long failedRows,
        @Param("resultMessage") String resultMessage
    );

    //Find stuck batch(status 'PROCESSING') if server crashed
    @Query("SELECT ub FROM UploadBatch ub WHERE ub.status = :status AND ub.startProcessTime < :thresholdTime")
    List<UploadBatch> findStuckBatches(
        @Param ("status") String status,
        @Param("thresholdTime") LocalDateTime thresholdTime
    );

}
