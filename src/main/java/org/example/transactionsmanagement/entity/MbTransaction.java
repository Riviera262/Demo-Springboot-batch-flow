package org.example.transactionsmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "MB_TRANSACTION")
public class MbTransaction {
    @Id
    @Column(name="trace", length = 20, nullable = false)
    private String trace;

    @Column(name="batch_id", length = 40, nullable = false)
    private String batchId;

    @Column(name="inserted_at")
    private LocalDateTime insertedAt;

    @Column(name="uploaded_by", length = 50)
    private String uploadedBy;

    @Column(name="from_acc", length = 20)
    private String fromAcc;

    @Column(name="tranx_time")
    private LocalDateTime tranxTime;

    @Column(name="amount")
    private BigDecimal amount;

    @Column(name="to_acc", length = 20)
    private String toAcc;

    @Column(name="remark", length = 200)
    private String remark;

    @Column(name="tranx_type", length = 30)
    private String tranxType;

    @Column(name="status", length = 10) //ACTIVE
    private String status;

    @Column(name = "createBy", length = 50)
    private String createBy;

    @Column(name = "createTm")
    private LocalDateTime createTm;

    @PrePersist
    protected void onCreate(){
        if (createTm == null){
            createTm = LocalDateTime.now();
        }
        if (status == null){
            status = "ACTIVE";
        }
    }
}
