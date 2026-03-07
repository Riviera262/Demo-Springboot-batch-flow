package org.example.transactionsmanagement.dto.transactionUpl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedRowData {
    private String trace;
    private String fromAcc;
    private LocalDateTime tranxTime;
    private BigDecimal amount;
    private String toAcc;
    private String remark;
    private String tranxType;
}
