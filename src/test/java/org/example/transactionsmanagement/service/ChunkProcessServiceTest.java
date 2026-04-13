package org.example.transactionsmanagement.service;

import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.transactionsmanagement.dto.transactionUpl.ProcessingResult;
import org.example.transactionsmanagement.entity.MbTransactionUpl;
import org.example.transactionsmanagement.repository.MbTransactionUplRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ChunkProcessServiceTest {

    @Mock
    private MbTransactionUplRepository mbTransactionUplRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ChunkProcessService chunkProcessService;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("test-excel-chunks");

        //Inject @lazy self
        ReflectionTestUtils.setField(chunkProcessService, "self", chunkProcessService);

        //Self invoke entity manager
        ReflectionTestUtils.setField(chunkProcessService, "entityManager", entityManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    // Helper method to dynamically generate a real Excel file
    private Path createMiniExcelFile(String fileName, Object[][] data) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");
            int rowNum = 0;
            for (Object[] rowData : data) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                for (Object field : rowData) {
                    Cell cell = row.createCell(colNum++);
                    if (field == null) continue;
                    if (field instanceof String) cell.setCellValue((String) field);
                    else if (field instanceof Double) cell.setCellValue((Double) field);
                }
            }
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                workbook.write(fos);
            }
        }
        return filePath;
    }

    //Happy Path: Process excel file, everything match the requirement
    @Test
    void processExcelFile_HappyPath_ShouldProcessAllRowsSuccessfully() throws IOException{
        //ARRANGE
        String username = "TestUser";
        String batchId = "BATCH_20260201152030_A1B2C3D4";

        Object[][] data = {
                {"TRACE", "FROM_ACC", "TRANX_TIME", "AMOUNT", "TO_ACC", "REMARK", "TRANX_TYPE"}, // Header
                {"TRX-01", "ACC-A", "2023-10-24 15:30:00", 50000.0, "ACC-B", "Salary", "TRANSFER"},
                {"TRX-02", "ACC-C", "24/10/2023 16:00:00", 20000.0, "ACC-D", "Bonus", "TRANSFER"}
        };

        Path excelPath = createMiniExcelFile("happy_path.xlsx",data);
        Set<String> globalSeenTrace = new HashSet<>();

        //ACT
        ProcessingResult result = chunkProcessService.processExcelFile(batchId, excelPath, username, globalSeenTrace);

        //ASSERT
        assertNotNull(result);
        assertEquals(2, result.getTotalRows());
        assertEquals(2, result.getSuccessRows());
        assertEquals(0, result.getFailedRows());
        assertEquals(0, result.getErrors().size());

        //Verify repository saveAll is called
        verify(mbTransactionUplRepository, times(1)).saveAll(any());
    }

    //Case: Header is not what the trasactional required


    //Case: Mixed rows in an excel file
    @Test
    void processExcelFile_MixedRows_ShouldCatchAllTheErrorsAndSaveValidRows() throws IOException{
        String username = "testUser";
        String batchId = "BATCH_20260201152030_A1B2C3D4";

        Object[][] data = {
                {"TRACE", "FROM_ACC", "TRANX_TIME", "AMOUNT", "TO_ACC", "REMARK", "TRANX_TYPE"},
                {"TRX-VALID", "ACC-A", "2023-10-24 15:30:00", 50000.0, "ACC-B", "Good", "TRANSFER"}, // 1. Valid
                {"", "ACC-A", "2023-10-24 15:30:00", 50000.0, "ACC-B", "No Trace", "TRANSFER"},      // 2. Missing Trace
                {"TRX-ERR2", "ACC-A", "Invalid-Date-Format", 50000.0, "ACC-B", "Bad Date", "TRANSFER"},// 3. Invalid Time
                {"TRX-ERR3", "ACC-A", "2023-10-24 15:30:00", -100.0, "ACC-B", "Negative Amt", "TRANSFER"},// 4. Invalid Amount
                {"TRX-VALID", "ACC-X", "2023-10-24 15:30:00", 10000.0, "ACC-Y", "Dup Trace", "TRANSFER"} // 5. Duplicate Trace
        };

        Path excelPath = createMiniExcelFile("MixedRows.xlsx", data);
        Set<String> globalSeenTrace= new HashSet<>();

        //ACT
        ProcessingResult result = chunkProcessService.processExcelFile(batchId, excelPath, username,globalSeenTrace);

        //ASSERT
        assertNotNull(result);
        assertEquals(5, result.getTotalRows());
        assertEquals(1, result.getSuccessRows());
        assertEquals(4, result.getFailedRows());
        assertEquals(4, result.getErrors().size());

        assertTrue(globalSeenTrace.contains("TRX-VALID"));
        assertEquals(1, globalSeenTrace.size());
    }

    //Case: Invalid header template required
    @Test
    void processExcelFile_InvalidHeaderTemplate_ShouldThrowRuntimeException() throws IOException {
        //ARRANGE
        String username = "testUser";
        String batchId = "BATCH_20260201152030_A1B2C3D4";

        Object[][] data = {
                {"TRACE", "TESTING", "TIME", "AMOUNT", "TO_ACC", "REMARK", "TYPE"}, //Wrong header template
                {"TRX-VALID", "ACC-A", "2023-10-24 15:30:00", 50000.0, "ACC-B", "Good", "TRANSFER"}, // 1. Valid data
        };

        Path excelPath = createMiniExcelFile("WrongHeaderTemplate.xlsx", data);
        Set<String> globalSeenTrace= new HashSet<>();

        //ACT
        ProcessingResult result = chunkProcessService.processExcelFile(batchId, excelPath, username, globalSeenTrace);

        //ASSERT
        assertEquals(0, result.getTotalRows());
        assertEquals(0, result.getSuccessRows());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("Template"));

        verify(mbTransactionUplRepository, never()).saveAll(any());
    }

    //Case: Only valid header but no data
    @Test
    void ProcessExcelFile_OnlyHeaderNoData_ShouldReturnZeroRow() throws IOException {
        //ARRANGE
        String username = "testUser";
        String batchId = "BATCH_20260201152030_A1B2C3D4";

        Object[][] emptyData = {
                {"TRACE", "FROM_ACC", "TRANX_TIME", "AMOUNT", "TO_ACC", "REMARK", "TRANX_TYPE"}
        };

        Path emptyExcel = createMiniExcelFile("EmptyData.xlsx", emptyData);
        Set<String> globalSeenTrace= new HashSet<>();

        //ACT
        ProcessingResult result = chunkProcessService.processExcelFile(batchId, emptyExcel, username, globalSeenTrace);

        //ASSERT
        assertNotNull(result);
        assertEquals(0, result.getTotalRows());
        assertEquals(0, result.getSuccessRows());
        assertEquals(0, result.getFailedRows());

        verify(mbTransactionUplRepository, never()).saveAll(any());
    }

    //Case: Duplicate trace in DB
    @Test
    void processExcelFile_DuplicateTraceInDB_ShouldFallBack() throws IOException{
        //ARRANGE
        String username = "testUser";
        String batchId = "BATCH_20260201152030_A1B2C3D4";

        Object[][] data = {
                {"TRACE", "FROM_ACC", "TRANX_TIME", "AMOUNT", "TO_ACC", "REMARK", "TRANX_TYPE"},
                {"TRX-DB-1", "ACC-A", "2023-10-24 15:30:00", 50000.0, "ACC-B", "OK", "TRANSFER"},
                {"TRX-DB-2", "ACC-C", "2023-10-24 15:30:00", 20000.0, "ACC-D", "Dup in DB", "TRANSFER"}
        };

        Path excelPath = createMiniExcelFile("DuplicateTraceInDB.xlsx", data);
        Set<String> globalSeenTrace = new HashSet<>();

        //Simulate saveAll encounters error and system must switch to save 1 by 1
        doThrow(new DataIntegrityViolationException("Batch insert failed"))
                .when(mbTransactionUplRepository).saveAll(any());

        //simulate duplicate trace case when save new data to db, row 1 valid but row 2 duplicate
        when(mbTransactionUplRepository.save(any(MbTransactionUpl.class)))
                .thenReturn(new MbTransactionUpl())
                .thenThrow(new DataIntegrityViolationException("Duplicate"));

        //ACT
        ProcessingResult result = chunkProcessService.processExcelFile(batchId, excelPath, username, globalSeenTrace);

        //ASSERT
        assertEquals(2, result.getTotalRows());
        assertEquals(1, result.getSuccessRows());
        assertEquals(1, result.getFailedRows());

        verify(mbTransactionUplRepository, times(1)).saveAll(any());
        verify(mbTransactionUplRepository, times(2)).save(any(MbTransactionUpl.class));
    }

    //Case: Exceed chunk size
    @Test
    void processExcelFile_ExceedChunkSize_ShouldSplitUp() throws IOException {
        //ARRANGE
        String username = "testUser";
        String batchId = "BATCH_20260201152030_A1B2C3D4";

        Object[][] massiveData = new Object[5002][7];
        massiveData[0] = new Object[]{"TRACE", "FROM_ACC", "TRANX_TIME", "AMOUNT", "TO_ACC", "REMARK", "TRANX_TYPE"};

        for (int i = 1; i <= 5001; i++) {
            massiveData[i] = new Object[]{"TRX-MASS-" + i, "ACC-A", "2023-10-24 15:30:00", 50000.0, "ACC-B", "Mass", "TRANSFER"};
        }

        Path massiveExcelPath = createMiniExcelFile("massive_data.xlsx", massiveData);
        Set<String> globalSeenTrace = new HashSet<>();

        //ACT
        ProcessingResult result = chunkProcessService.processExcelFile("BATCH-MASSIVE", massiveExcelPath, "TestUser", globalSeenTrace);

        //ASSERT
        assertEquals(5001, result.getTotalRows());
        assertEquals(5001, result.getSuccessRows());

        //Verify if the system save all 2 times(each time 5000 objects for each chunk)
        verify(mbTransactionUplRepository, times(2)).saveAll(any());

        //Make sure RAM must be free after save chunk
        verify(entityManager, atLeast(1)).clear();
    }

    //Case: IOException errors(Batch or file is not existed)
    @Test
    void processExcelFile_IOException_ShouldCatchAndThrow(){
        //ARRANGE
        String username = "testUser";
        String batchId = "BATCH_20260201152030_A1B2C3D4";

        Path notExistPath = tempDir.resolve("ghost_file.xlsx");
        Set<String> globalSeenTrace = new HashSet<>();

        //ACT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chunkProcessService.processExcelFile(batchId, notExistPath, username, globalSeenTrace);
        });

        //ASSERT
        assertTrue(exception.getMessage().contains("Failed to process Excel file"));
        verify(mbTransactionUplRepository, never()).saveAll(any());
    }
}
