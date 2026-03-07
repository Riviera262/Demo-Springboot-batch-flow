package org.example.transactionsmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class MbTransactionId implements Serializable {
    @Column(name="batch_id", length = 40, nullable = false)
    private String batchId;

    @Column(name="trace", length = 20, nullable = false)
    private String trace;
}
