package org.example.transactionsmanagement.repository;

import org.example.transactionsmanagement.entity.MbTransactionId;
import org.example.transactionsmanagement.entity.MbTransactionUpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MbTransactionUplRepository extends JpaRepository<MbTransactionUpl, MbTransactionId> {

    //Update all transactions of the selected batches with status 'INIT' that trace already existed in MBTRANSACTION to 'DEL', use 'exists' to check if that trace already existed in MBTRANSACTION, if yes then change status to 'DEL'
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE MB_TRANSACTION_UPL u SET STATUS = 'DEL'
        WHERE BATCH_ID = :batchId AND STATUS = 'INIT'
          AND EXISTS (SELECT 1 FROM MB_TRANSACTION t WHERE t.TRACE = u.TRACE)
        """, nativeQuery = true)
    int markAsDel(@Param("batchId") String batchId);

    //Update all the remaining transactions of the selected batches with status 'INIT', this is 100% clean data no duplicate trace due to we update all the duplicate one to 'DEL' already
    @Modifying
    @Transactional
    @Query(value = "UPDATE MB_TRANSACTION_UPL SET STATUS = 'ACTIVE' WHERE BATCH_ID = :batchId AND STATUS = 'INIT'", nativeQuery = true)
    int markAsActive(@Param("batchId") String batchId);

    //Get all result of a batch with all status in it (INIT, DEL, ACTIVE)
    @Query("SELECT u.status, COUNT(u) FROM MbTransactionUpl u WHERE u.id.batchId = :batchId GROUP BY u.status")
    List<Object[]> countByBatchIdAndStatus(@Param("batchId") String batchId);

    @Query("SELECT COUNT(u) FROM MbTransactionUpl u WHERE u.id.batchId = :batchId AND u.status = :status")
    long countRowsByBatchIdAndStatus(
            @Param("batchId") String batchId,
            @Param("status") String status
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM MbTransactionUpl u WHERE u.id.batchId = :batchId")
    void deleteByBatchId(@Param("BatchId") String batchId);
}
