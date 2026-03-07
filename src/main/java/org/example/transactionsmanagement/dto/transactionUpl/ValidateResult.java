package org.example.transactionsmanagement.dto.transactionUpl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateResult {
    private boolean valid;
    private String errorMessage;

    public static ValidateResult valid(){
        return new ValidateResult(true, null);
    }

    public static ValidateResult invalid(String message){
        return new ValidateResult(false, message);
    }
}
