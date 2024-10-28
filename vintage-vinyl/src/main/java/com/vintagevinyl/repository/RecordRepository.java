package com.vintagevinyl.repository;

import com.vintagevinyl.model.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
    Page<Record> findByTitleContainingOrArtistContaining(String title, String artist, Pageable pageable);


    /**
     * Find all records with stock below their low stock threshold
     */
    @Query("SELECT r FROM Record r WHERE r.stock <= r.lowStockThreshold")
    List<Record> findLowStockRecords();

    /**
     * Find all records with stock below a specific threshold
     */
    List<Record> findByStockLessThanEqual(int threshold);

    /**
     * Find all records that are out of stock
     */
    List<Record> findByStockEquals(int stock);

    /**
     * Find records with stock between min and max values
     */
    List<Record> findByStockBetween(int minStock, int maxStock);

    /**
     * Get count of records with low stock
     */
    @Query("SELECT COUNT(r) FROM Record r WHERE r.stock <= r.lowStockThreshold")
    long countLowStockRecords();

    /**
     * Find records that need restocking (stock below threshold) ordered by remaining stock
     */
    @Query("SELECT r FROM Record r WHERE r.stock <= r.lowStockThreshold ORDER BY r.stock ASC")
    List<Record> findRecordsNeedingRestock();

    /**
     * Update stock quantity for a specific record
     */
    @Query("UPDATE Record r SET r.stock = :stock WHERE r.id = :id")
    void updateStock(@Param("id") Long id, @Param("stock") int stock);
}