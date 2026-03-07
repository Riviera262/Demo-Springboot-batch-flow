package org.example.transactionsmanagement.dto.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetail implements Serializable {
    private String filename;   //Identify which file got error
    private String trace;      //Identify which trace error (duplicate)
    private String message;    //Output reason why error
}
