package org.example.transactionsmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionsmanagement.dto.transaction.ApprovalResult;
import org.example.transactionsmanagement.repository.MbTransactionRepository;
import org.example.transactionsmanagement.repository.MbTransactionUplRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalBatchService {

    private final MbTransactionUplRepository mbTransactionUplRepository;
    private final MbTransactionRepository mbTransactionRepository;

    //Approve a single batch
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApprovalResult approveSingleBatch(String batchId, String approvedBy) {
        log.info("Approving batch: {}", batchId);

        try {
            //Count the number of transactions with status 'INIT' in a batch we need to process
            long totalInitRows = mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "INIT");

            if (totalInitRows == 0){
                throw new RuntimeException("There are no valid row with status 'INIT' in this batch");
            }
            log.info("Batch {} has total {} rows with status 'INIT' to approve", batchId, totalInitRows);
            log.info("");

            //Check and mark any transaction with trace that already existed in MB_TRANSACTION to status: 'DEL'
            log.info("Checking for duplicate TRACE");
            long delCount = mbTransactionUplRepository.markAsDel(batchId);
            log.info("Marked {} rows duplicate TRACE as 'DEL'", delCount);
            log.info("");

            //Insert any remaining transaction with status 'INIT' to MB_TRANSACTION (Insert Select)
            log.info("Inserted new trace to MB_Transaction");
            long activeCount = mbTransactionRepository.insertFromUpl(batchId, approvedBy);
            log.info("Inserted {} new rows to MB_TRANSACTION", activeCount);
            log.info("");

            //Mark inserted transactions status as 'ACTIVE' in MB_TRANSACTION_UPL
            log.info("Marking inserted rows as 'ACTIVE' in MB_TRANSACTION_UPL");
            int updatedActiveCount = mbTransactionUplRepository.markAsActive(batchId);
            log.info("Marked {} rows as 'ACTIVE' in MB_TRANSACTION_UPL", updatedActiveCount);
            log.info("");

            //Make sure the total of inserted rows with status 'ACTIVE' in mbTransaction and mbTransactionUpl are equals
            if (activeCount != updatedActiveCount){
                log.warn("WARNING DATA MISMATCH: total {} rows were inserted to MB_TRANSACTION, but only {} in MB_TRANSACTION_UPL got updated", activeCount, updatedActiveCount);
            }

            if (totalInitRows != (activeCount + delCount)){
                log.warn("WARNING DATA MISMATCH: there are total {} rows with status 'INIT' at the beginning, total of {} active and {} del rows not equal", totalInitRows, activeCount, delCount);
            }

            String message = String.format(
                    "Approve batch done. There are total " + activeCount + " success approved and "+ delCount +" got rejected"
            );

            return ApprovalResult.builder()
                    .batchId(batchId)
                    .status("APPROVED")
                    .totalInitRows(totalInitRows)
                    .activeCount(activeCount)
                    .delCount(delCount)
                    .message(message)
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error happens when system trying to approve batch {}", batchId);

            return ApprovalResult.builder()
                    .batchId(batchId)
                    .status("FAILED_APPROVED")
                    .message("Approved batch failed due to error in system: "+ e.getMessage())
                    .build();
        }
    }

}