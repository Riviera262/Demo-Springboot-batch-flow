package org.example.transactionsmanagement.dto.transactionUpl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.transactionsmanagement.dto.error.ErrorDetail;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResult {
    private int successRows;
    private int failedRows;
    @Builder.Default
    private List<ErrorDetail> errors = new ArrayList<>();
}
