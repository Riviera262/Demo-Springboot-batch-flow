package org.example.transactionsmanagement.service;

import org.example.transactionsmanagement.dto.transaction.ApprovalRequest;
import org.example.transactionsmanagement.dto.transaction.ApprovalResult;
import org.example.transactionsmanagement.dto.transactionUpl.ExcelUploadResponse;
import org.example.transactionsmanagement.entity.UploadBatch;
import org.example.transactionsmanagement.repository.MbTransactionUplRepository;
import org.example.transactionsmanagement.repository.UploadBatchRepository;
import org.example.transactionsmanagement.utils.BatchIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UploadBatchServiceTest {

    @Mock
    private BatchIdGenerator batchIdGenerator;

    @Mock
    private UploadBatchRepository uploadBatchRepository;

    @Mock
    private ApprovalBatchService approvalBatchService;

    @Mock
    private TransactionUplAsyncService transactionUplAsyncService;

    @Mock
    private MbTransactionUplRepository mbTransactionUplRepository;

    @InjectMocks
    private UploadBatchService uploadBatchService;

    private Path tempUploadDir;

    @BeforeEach
    void setUp() throws IOException {
        tempUploadDir = Files.createTempDirectory("test-uploads");

        ReflectionTestUtils.setField(uploadBatchService, "uploadDir", tempUploadDir.toString());
    }

    @AfterEach
    void cleanUp() throws IOException{
        //Delete all folder after every test
        Files.walk(tempUploadDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    //Happy path: Upload 1 excel file
    @Test
    void excelUpload_Success_ShouldReturnResponseAndTriggerAsync(){
        //ARRANGE
        String username = "testUsser";
        String mockBatchId = "BATCH_20260201152030_A1B2C3D4";

        //Create a mock excel file
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy content".getBytes()
        );

        List<MultipartFile> files = List.of(mockFile);

        //Define behavior for mocks
        when(batchIdGenerator.generateBatchId()).thenReturn(mockBatchId);
        when(uploadBatchRepository.saveAndFlush(any(UploadBatch.class))).thenReturn(new UploadBatch());

        //ACT
        ExcelUploadResponse response = uploadBatchService.excelUpload(files, username);

        //ASSERT
        assertNotNull(response);
        assertEquals(mockBatchId, response.getBatchId());
        assertEquals("PENDING", response.getStatus());
        assertEquals(1, response.getFileCount());

        //Verify if any files got copied to a folder
        Path expectedFolder = tempUploadDir.resolve(mockBatchId);
        assertTrue(Files.exists(expectedFolder), "Batch folder should be created");
        assertTrue(Files.exists(expectedFolder.resolve("test-data.xlsx")),
                "Excel file should be saved");

        //Verify internal method got called
        verify(batchIdGenerator, times(1)).generateBatchId();
        verify(uploadBatchRepository, times(1)).saveAndFlush(any(UploadBatch.class));
        verify(transactionUplAsyncService, times(1)).processExcelBatch(mockBatchId);
    }

    //Case: invalid file 0 byte
    @Test
    void excelUpload_EmptyFiles_ShouldThrowIllegalArgumentException(){
        //ARRANGE
        List<MultipartFile> emptyFiles = Collections.emptyList();
        String username = "TestUser";

        //ACT
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            uploadBatchService.excelUpload(emptyFiles, username);
        });

        //ASSERT
        assertEquals("No files provided", exception.getMessage());

        verify(uploadBatchRepository, never()).saveAndFlush(any());
        verify(transactionUplAsyncService, never()).processExcelBatch(any());
    }

    //Exceed max excel limited(10 file per upload)
    @Test
    void excelUpload_ExceedMaxExcelFileLimit_ShouldThrowIllegalArgumentException(){
        //ARRANGE
        List<MultipartFile> tooManyFiles = new ArrayList<>();

        for (int i = 0; i < 12; i++){
            tooManyFiles.add(new MockMultipartFile("file", "test.xlsx", "test/plain", new byte[0]));
        }

        String username = "TestUser";

        //ACT
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            uploadBatchService.excelUpload(tooManyFiles, username);
        });

        //ASSERT
        assertEquals("Maximun 10 files per batch upload", exception.getMessage());

        //No BatchId should got generated
        verify(batchIdGenerator, never()).generateBatchId();
    }

    //Case: Invalid file type
    @Test
    void excelUpload_WrongFileType_ShouldThrowIllegalArgumentException(){
        //ARRANGE
        String username = "TestUser";
        String mockBatchId = "BATCH_20260201152030_A1B2C3D4";

        MockMultipartFile txtFile = new MockMultipartFile("file", "doc.txt", "text/plain", "text".getBytes());
        List<MultipartFile> wrongFiles = List.of(txtFile);

        when(batchIdGenerator.generateBatchId()).thenReturn(mockBatchId);

        //ACT
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            uploadBatchService.excelUpload(wrongFiles, username);
        });

        //ASSERT
        assertEquals("No valid Excel file found", exception.getMessage());
        verify(uploadBatchRepository, never()).saveAndFlush(any());
    }

    //Case: Mixed file in a batch
    @Test
    void excelUpload_MixedFiles_ShouldProcessOnlyValidExcelFiles(){
        //ARRANGE
        String username = "TestUser";
        String mockBatchId = "BATCH_20260201152030_A1B2C3D4";

        MockMultipartFile validExcel = new MockMultipartFile("file", "good.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data".getBytes());
        MockMultipartFile emptyExcel = new MockMultipartFile("file", "empty.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        MockMultipartFile txtFile = new MockMultipartFile("file", "bad.txt", "text/plain", "data".getBytes());

        List<MultipartFile> mixedFiles = List.of(validExcel, emptyExcel, txtFile);

        when(batchIdGenerator.generateBatchId()).thenReturn(mockBatchId);
        when(uploadBatchRepository.saveAndFlush(any(UploadBatch.class))).thenReturn(new UploadBatch());

        //ACT
        ExcelUploadResponse response = uploadBatchService.excelUpload(mixedFiles, username);

        //ASSERT
        assertNotNull(response);
        assertEquals(mockBatchId, response.getBatchId());
        assertEquals(1, response.getFileCount());

        verify(uploadBatchRepository, times(1)).saveAndFlush(any(UploadBatch.class));
        verify(transactionUplAsyncService, times(1)).processExcelBatch(mockBatchId);
    }

    //Happy path: approve batches in a range date without any problems
    @Test
    void approveMultipleBatchesWithDateRange_HappyPath_ShouldProcessAllBatches(){
        //ARRANGE
        String approver = "testApprover";
        String batchId_1 = "BATCH_1";
        String batchId_2 = "BATCH_2";

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        ApprovalRequest request = new ApprovalRequest();
        request.setStartTime(start);
        request.setEndTime(end);

        UploadBatch batch1 = UploadBatch.builder().batchId(batchId_1).build();
        UploadBatch batch2 = UploadBatch.builder().batchId(batchId_2).build();

        when(uploadBatchRepository.findByUploadTimeBetweenAndStatusOrderByUploadTimeDesc(start, end, "COMPLETED"))
                .thenReturn(List.of(batch1, batch2));

        when(uploadBatchRepository.markAsApproving(anyList())).thenReturn(2);

        ApprovalResult result1 = ApprovalResult.builder().batchId(batchId_1).status("APPROVED").activeCount(10L).delCount(0L).build();
        ApprovalResult result2 = ApprovalResult.builder().batchId(batchId_2).status("APPROVED").activeCount(20L).delCount(5L).build();

        when(approvalBatchService.approveSingleBatch(batchId_1, approver)).thenReturn(result1);
        when(approvalBatchService.approveSingleBatch(batchId_2, approver)).thenReturn(result2);

        //ACT
        List<ApprovalResult> results = uploadBatchService.processBatchApprovals(request, approver);

        //ASSERT
        assertEquals(2, results.size());

        verify(uploadBatchRepository).markAsApproved(eq(batchId_1), eq(approver), eq(10L), eq(0L), any());
        verify(uploadBatchRepository).markAsApproved(eq(batchId_2), eq(approver), eq(20L), eq(5L), any());
    }


    //Case: Test auto recovery stuck batch with status 'PROCESSING' or 'APPROVING'
    @Test
    void recoverStuckBatchesProcessing_WhenStuck_ShouldDeleteDataAndMarkFailed(){
        //ARRANGE
        String batchId = "StuckBatch_Processing";

        //Simulate case system found a stuck batch with status 'PROCESSING' duo to server crashed
        UploadBatch stuckBatch = UploadBatch.builder().batchId(batchId).build();

        //Mock found a batch got stuck for over 30 minutes
        when(uploadBatchRepository.findStuckBatches(eq("PROCESSING"), any(LocalDateTime.class)))
                .thenReturn(List.of(stuckBatch));

        //ACT
        String resultMsg = uploadBatchService.recoverProcessingBatches("SYSTEM_AUTO_RECOVERY");

        //ASSERT
        assertTrue(resultMsg.contains("done"));

        //Make sure to delete all data of the stuck batch and mark it status as 'FAILED'
        verify(mbTransactionUplRepository).deleteByBatchId(batchId);
        verify(uploadBatchRepository).updateStatusByBatchId(batchId, "FAILED");
    }

    @Test
    void recoverStuckBatchApproving_WhenNoInitRowLeft_ShouldMarkAsApproved(){
        //ARRANGE
        String batchId = "StuckBatchApproving";

        UploadBatch stuckBatch = UploadBatch.builder().batchId(batchId).build();

        when(uploadBatchRepository.findStuckBatches(eq("APPROVING"), any(LocalDateTime.class)))
                .thenReturn(List.of(stuckBatch));
        when(mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "INIT")).thenReturn(0L); //No 'INIT' row left to approve
        when(mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "ACTIVE")).thenReturn(80L);
        when(mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "DEL")).thenReturn(20L);

        //ACT
        String resultMsg = uploadBatchService.recoverApprovingBatches("SYSTEM_AUTO_RECOVERY");

        //ASSERT
        assertTrue(resultMsg.contains("1 got 'APPROVED'"));

        //System must mark the batch as status 'APPROVED' due to no 'INIT' row left to approve
        verify(uploadBatchRepository).markAsApproved(eq(batchId), eq("SYSTEM_AUTO_RECOVERY"), eq(80L), eq(20L), anyString());
    }

    @Test
    void recoverStuckBatchesApproving_WhenInitRowsStillExist_ShouldResetToCompleted(){
        //ARRANGE
        String batchId = "StuckBatchApproving";

        UploadBatch stuckBatch = UploadBatch.builder().batchId(batchId).build();

        when(uploadBatchRepository.findStuckBatches(eq("APPROVING"), any(LocalDateTime.class)))
                .thenReturn(List.of(stuckBatch));

        when(mbTransactionUplRepository.countRowsByBatchIdAndStatus(batchId, "INIT")).thenReturn(50L); //'INIT' rows still exist in the batch

        //ACT
        String resultMsg = uploadBatchService.recoverApprovingBatches("SYSTEM_AUTO_RECOVERY");

        //ASSERT
        assertTrue(resultMsg.contains("1 got reset to status 'COMPLETED'"));

        //System must reset the batch back to status 'COMPLETED' in order to approve it again
        verify(uploadBatchRepository).updateStatusByBatchId(batchId, "COMPLETED");

    }
}
