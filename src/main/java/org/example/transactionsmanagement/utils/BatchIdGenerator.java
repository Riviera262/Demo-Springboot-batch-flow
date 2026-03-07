package org.example.transactionsmanagement.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class BatchIdGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    //Generate unique batch Id
    //Example: BATCH_20260201152030_A1B2C3D4
    public String generateBatchId(){
        String timeStamp = LocalDateTime.now().format(FORMATTER);
        String uuid = UUID.randomUUID()
                .toString()
                .replace("-","")
                .substring(0,8)
                .toUpperCase();

        return String.format("BATCH_%s_%s", timeStamp, uuid);
    }
}
