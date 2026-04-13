package org.example.transactionsmanagement.service;


import org.example.transactionsmanagement.dto.transaction.ApprovalRequest;
import org.example.transactionsmanagement.dto.transaction.ApprovalResult;
import org.example.transactionsmanagement.entity.UploadBatch;
import org.example.transactionsmanagement.repository.MbTransactionRepository;
import org.example.transactionsmanagement.repository.MbTransactionUplRepository;
import org.example.transactionsmanagement.repository.UploadBatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ApprovalBatchServiceTest {

    @Mock
    private MbTransactionUplRepository mbTransactionUplRepository;

    @Mock
    private MbTransactionRepository mbTransactionRepository;

    @Mock
    private UploadBatchRepository uploadBatchRepository;

    @Mock
    private UploadBatchService uploadBatchService;

    @InjectMocks
    private ApprovalBatchService approvalBatchService;

    //Happy path: Approve a batch with all 100% data passed(No duplicate trace found)
    @Test
    void approveSingleBatch_HappyPath_ShouldApproveAll(){
        //ARRANGE
        String approver = "testApprover";
        String batchId = "BATCH_20260201152030_A1B2C3D4";

        when(mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "INIT")).thenReturn(100L);
        when(mbTransactionUplRepository.markAsDel(batchId)).thenReturn(0L); //No invalid data in the batch
        when(mbTransactionRepository.insertFromUpl(batchId, approver)).thenReturn(100L); //Insert 100% data all are valid
        when(mbTransactionUplRepository.markAsActive(batchId)).thenReturn(100L); //Update all the valid data to 'ACTIVE' and done

        //ACT
        ApprovalResult result = approvalBatchService.approveSingleBatch(batchId, approver);

        //ASSERT
        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        assertEquals(100L, result.getTotalInitRows());
        assertEquals(100L, result.getActiveCount());
        assertEquals(0L, result.getDelCount());

        verify(mbTransactionUplRepository).markAsDel(batchId);
        verify(mbTransactionRepository).insertFromUpl(batchId, approver);
        verify(mbTransactionUplRepository).markAsActive(batchId);
    }

    //Case: Duplicate multiple traces within a batch
    @Test
    void approveSingleBatch_DuplicateTracesMixedIn_ShouldApproveAndReject(){
        //ARRANGE
        String approver = "testApprover";
        String batchId = "BATCH_20260201152030_A1B2C3D4";

        when(mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "INIT")).thenReturn(100L);
        when(mbTransactionUplRepository.markAsDel(batchId)).thenReturn(20L); //Found 20 traces that are duplicate
        when(mbTransactionRepository.insertFromUpl(batchId, approver)).thenReturn(80L); //Insert 80 valid data
        when(mbTransactionUplRepository.markAsActive(batchId)).thenReturn(80L); //Update all the valid data to 'ACTIVE' and done

        //ACT
        ApprovalResult result = approvalBatchService.approveSingleBatch(batchId, approver);

        //ASSERT
        assertEquals("APPROVED", result.getStatus());
        assertEquals(100L, result.getTotalInitRows());
        assertEquals(80L, result.getActiveCount());
        assertEquals(20L, result.getDelCount());
    }

    //Case: batch with 0 data, not having a single row with status 'INIT'
    @Test
    void approveSingleBatch_ZeroInitRow_ShouldReturnFailedStatus(){
        //ARRANGE
        String approver = "testApprover";
        String batchId = "BATCH_20260201152030_A1B2C3D4";

        when(mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "INIT")).thenReturn(0L);

        // ACT
        ApprovalResult result = approvalBatchService.approveSingleBatch(batchId, approver);

        //ASSERT
        assertEquals("FAILED_APPROVED", result.getStatus());
        assertTrue(result.getMessage().contains("no valid row"));

        verify(mbTransactionUplRepository, never()).markAsDel(anyString());
        verify(mbTransactionRepository, never()).insertFromUpl(anyString(), anyString());
    }

    //Case: DataBase error when inserting data
    @Test
    void approveSingleBatch_DatabaseError_ShouldCatchAndReturnFailedStatus(){
        String approver = "testApprover";
        String batchId = "BATCH_20260201152030_A1B2C3D4";

        when(mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "INIT")).thenReturn(50L);
        when(mbTransactionUplRepository.markAsDel(batchId)).thenReturn(0L);

        //Simulate encounter case database error occurred when system trying to insert data
        when(mbTransactionRepository.insertFromUpl(batchId, approver))
                .thenThrow(new RuntimeException("Connection timeout"));

        // ACT
        ApprovalResult result = approvalBatchService.approveSingleBatch(batchId, approver);

        // ASSERT
        assertEquals("FAILED_APPROVED", result.getStatus());
        assertTrue(result.getMessage().contains("Connection timeout"));
    }

}
