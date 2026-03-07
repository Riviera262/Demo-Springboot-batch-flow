package org.example.transactionsmanagement.repository;

import org.example.transactionsmanagement.entity.MbTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MbTransactionRepository extends JpaRepository<MbTransaction, String> {

    //Insert clean transaction data of the selected batches from MBTRANSACTIONUPL, duplicate one already got update status to 'DEL' so the remaining only have transaction with status 'INIT'
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO MB_TRANSACTION (" +
                "TRACE, BATCH_ID, INSERTED_AT, UPLOADED_BY, " +
                "FROM_ACC, TRANX_TIME, AMOUNT, " +
                "TO_ACC, REMARK, TRANX_TYPE, " +
                "STATUS, CREATE_BY, CREATE_TM) " +
            "SELECT " +
                "TRACE, BATCH_ID, INSERTED_AT, UPLOADED_BY, " +
                "FROM_ACC, TRANX_TIME, AMOUNT, " +
                "TO_ACC, REMARK, TRANX_TYPE, " +
                "'ACTIVE', :approverUsername, CREATE_TM " +
            "FROM MB_TRANSACTION_UPL " +
            "WHERE BATCH_ID = :batchId "+
            "AND STATUS = 'INIT'"
            , nativeQuery = true)
    int insertFromUpl(@Param("batchId") String Id, @Param("approverUsername") String approverUsername);

    //Count how much transactions in a batch in mbtransaction table
    long countByBatchId(String batchId);

}
