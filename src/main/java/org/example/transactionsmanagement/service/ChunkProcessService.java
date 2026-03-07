package org.example.transactionsmanagement.service;

import com.github.pjfanning.xlsx.StreamingReader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.example.transactionsmanagement.dto.error.ErrorDetail;
import org.example.transactionsmanagement.dto.transactionUpl.*;
import org.example.transactionsmanagement.entity.MbTransactionId;
import org.example.transactionsmanagement.entity.MbTransactionUpl;
import org.example.transactionsmanagement.repository.MbTransactionUplRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChunkProcessService {

    private final MbTransactionUplRepository mbTransactionUplRepository;

    @Lazy
    @Autowired
    private ChunkProcessService self;

    private static final int CHUNK_SIZE = 1000;
    private static final int STREAMING_ROW_CACHE = 100;

    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),

            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),

            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),

            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a", new Locale("vi", "VN")), //process SA/CH
            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a", Locale.ENGLISH),        //process AM/PM
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    @PersistenceContext
    private EntityManager entityManager;

    //Processing excel file
    public ProcessingResult processExcelFile(String batchId, Path excelFilePath, String uploadedBy, Set<String> globalSeenTrace) {
        String fileName = excelFilePath.getFileName().toString();
        DataFormatter dataFormatter = new DataFormatter();

        long totalRows = 0;
        long successRows = 0;
        long failedRows = 0;

        List<ErrorDetail> errors = new ArrayList<>();

        try (InputStream is = new FileInputStream(excelFilePath.toFile());
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(STREAMING_ROW_CACHE)
                     .bufferSize(8192)
                     .open(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<MbTransactionUpl> chunk = new ArrayList<>();

            boolean firstRow = true;
            int rowIndex = 0;

            for (Row row : sheet) {
                rowIndex++;

                //Skip header
                if (firstRow) {
                    firstRow = false;
                    if (isHeaderRow(row, dataFormatter)) {
                        continue;
                    }
                }

                //Plus 1 for every 1 row we start to process
                totalRows++;

                try {
                    ParsedRowData parsed = ParsedRowData.builder()
                            .trace(getString(row.getCell(0), dataFormatter))
                            .fromAcc(getString(row.getCell(1), dataFormatter))
                            .tranxTime(getLocalDateTime(row.getCell(2), dataFormatter))
                            .amount(getBigDecimal(row.getCell(3), dataFormatter))
                            .toAcc(getString(row.getCell(4), dataFormatter))
                            .remark(getString(row.getCell(5), dataFormatter))
                            .tranxType(getString(row.getCell(6), dataFormatter))
                            .build();

                    ValidateResult validate = validate(parsed);
                    if (!validate.isValid()) {
                        failedRows++;
                        errors.add(buildError(fileName, parsed.getTrace(), validate.getErrorMessage() + " at row" + rowIndex + " in the excel file" ));
                        continue;
                    }

                    //Glocal hashmap check duplicate traces
                    if (!globalSeenTrace.add(parsed.getTrace())){
                        failedRows++;
                        errors.add(buildError(fileName, parsed.getTrace(), validate.getErrorMessage() + " duplicate trace at row" + rowIndex + " in the excel file"));
                        continue;
                    }

                    MbTransactionId id = MbTransactionId.builder()
                            .batchId(batchId)
                            .trace(parsed.getTrace())
                            .build();
                    MbTransactionUpl mbTransactionUpl = MbTransactionUpl.builder()
                            .id(id)
                            .insertedAt(LocalDateTime.now())
                            .uploadedBy(uploadedBy)
                            .fromAcc(parsed.getFromAcc())
                            .tranxTime(parsed.getTranxTime())
                            .amount(parsed.getAmount())
                            .toAcc(parsed.getToAcc())
                            .remark(parsed.getRemark())
                            .tranxType(parsed.getTranxType())
                            .status("INIT")
                            .createBy(uploadedBy)
                            .createTm(LocalDateTime.now())
                            .fileName(fileName)
                            .build();

                    //Add to chunk
                    chunk.add(mbTransactionUpl);

                    //If chunk reach 1000 then save
                    if (chunk.size() >= CHUNK_SIZE) {
                        try {
                            self.saveChunk(chunk, fileName);
                            successRows += chunk.size();
                        } catch (DataIntegrityViolationException ex) {
                            for (MbTransactionUpl entity : chunk) {
                                ErrorDetail err = self.saveSingleRow(entity, fileName);
                                if (err == null) {
                                    successRows++;
                                } else {
                                    failedRows++;
                                    errors.add(err);
                                }
                            }
                        }
                        chunk.clear();
                    }
                } catch (Exception e) {
                    failedRows++;
                    errors.add(buildError(
                            fileName,
                            null,
                            "Unexpected error: " + e.getMessage()
                    ));
                }
            }

            if (!chunk.isEmpty()) {
                try {
                    self.saveChunk(chunk, fileName);
                    successRows += chunk.size();
                } catch (DataIntegrityViolationException ex){
                    for (MbTransactionUpl entity : chunk) {
                        ErrorDetail err = self.saveSingleRow(entity, fileName);
                        if (err == null){
                            successRows++;
                        } else {
                            errors.add(err);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process Excel file");
        }

        ProcessingResult result = ProcessingResult.builder()
                .totalRows(totalRows)
                .successRows(successRows)
                .failedRows(failedRows)
                .errors(errors)
                .build();

        log.info("Processing file {} done", fileName);
        log.info("File: {}, Total rows: {}, success rows: {}, failed rows: {}",
                fileName, totalRows, successRows, failedRows);

        if (!errors.isEmpty()) {
            log.error("List of errors(maximum first 10 rows): ");
            errors.stream().limit(10).forEach(err ->
                    log.error("Trace: {} | Message: {}", err.getTrace(), err.getMessage())
            );
        }

        log.info("------------------------------------------------");
        log.info("");

        log.info("Number of traces in global hashSet: " + globalSeenTrace.size());
        return result;
    }

    //Save 1000 rows a chunk
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveChunk(List<MbTransactionUpl> chunk, String filename) {
        mbTransactionUplRepository.saveAll(chunk);
        mbTransactionUplRepository.flush();
        entityManager.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ErrorDetail saveSingleRow(MbTransactionUpl entity, String filename){
        try {
            mbTransactionUplRepository.save(entity);
            mbTransactionUplRepository.flush();
            return null;
        } catch (DataIntegrityViolationException duplicate){
            return buildError(filename, entity.getId().getTrace(), "Duplicate trace in database (Batch/conflict)");
        } catch (Exception e){
            return buildError(filename, entity.getId().getTrace(), "Database error: " + e.getMessage());
        }
    }

    //Validate trace, tranxTime and amount values
    private ValidateResult validate(ParsedRowData data){
        if (data.getTrace() == null || data.getTrace().isBlank()) {
            return ValidateResult.invalid("TRACE is invalid");
        }
        if (data.getTranxTime() == null) {
            return ValidateResult.invalid("TRANX_TIME is invalid");
        }
        if (data.getAmount() == null || data.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidateResult.invalid("AMOUNT is invalid");
        }

        return ValidateResult.valid();
    }

    private String getString(Cell cell, DataFormatter dataFormatter){
        if (cell == null){
            return null;
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    private LocalDateTime getLocalDateTime(Cell cell, DataFormatter dataFormatter){
        if (cell == null){
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)){
                return cell.getDateCellValue()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            }

            String value = getString(cell, dataFormatter);
            if (value == null || value.isBlank()){
                return null;
            }

            for (DateTimeFormatter dateTimeFormatter : DATETIME_FORMATTERS){
                try {
                    return LocalDateTime.parse(value, dateTimeFormatter);
                } catch (Exception ignored){
                }
            }
        } catch (Exception ignored){
        }

        return null;
    }

    //Get BigDecimal cell value
    private BigDecimal getBigDecimal(Cell cell, DataFormatter dataFormatter){
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC){
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }

            String value = getString(cell, dataFormatter);
            if (value != null && !value.isBlank()){
                return new BigDecimal(value.replace(",", ""));
            }
        } catch (Exception ignored){
        }

        return null;
    }

    //Check the first row if it true or false
    private boolean isHeaderRow(Row row, DataFormatter dataFormatter){
        String first = getString(row.getCell(0), dataFormatter);
        return first != null && first.toUpperCase().contains("TRACE");
    }

    //Catch errors
    private ErrorDetail buildError(String file, String trace, String message){
        return ErrorDetail.builder()
                .filename(file)
                .trace(trace != null ? trace : "UNKNOWN")
                .message(message)
                .build();
    }
}
