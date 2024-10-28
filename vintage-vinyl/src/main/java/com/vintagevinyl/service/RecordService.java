package com.vintagevinyl.service;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RecordService {
    @Autowired
    private RecordRepository recordRepository;
    @Autowired
    private ShoppingCartService shoppingCartService;

    public Page<Record> getAllRecords(Pageable pageable, String search) {
        if (search != null && !search.isEmpty()) {
            return recordRepository.findByTitleContainingOrArtistContaining(search, search, pageable);
        } else {
            return recordRepository.findAll(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Record getRecordById(Long id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
    }

    public void saveRecord(Record record) {
        recordRepository.save(record);
    }

    public void updateRecord(Record record) {
        recordRepository.save(record);
    }

    @Transactional
    public void deleteRecord(Long id) {
        Record record = recordRepository.findById(id).orElse(null);
        if (record != null) {
            shoppingCartService.removeAllCartItemsForRecord(id);
            recordRepository.delete(record);
        }
    }

    // New inventory management methods

    /**
     * Update stock quantity for a record
     * @param recordId Record identifier
     * @param quantity New stock quantity
     * @throws RuntimeException if record not found
     */
    @Transactional
    public void updateStock(Long recordId, int quantity) {
        Record record = getRecordById(recordId);
        record.setStock(quantity);
        recordRepository.save(record);
    }

    /**
     * Add stock to a record
     * @param recordId Record identifier
     * @param quantity Quantity to add
     * @throws RuntimeException if record not found
     */
    @Transactional
    public void addStock(Long recordId, int quantity) {
        Record record = getRecordById(recordId);
        record.addStock(quantity);
        recordRepository.save(record);
    }

    /**
     * Reduce stock from a record
     * @param recordId Record identifier
     * @param quantity Quantity to reduce
     * @throws RuntimeException if record not found or insufficient stock
     */
    @Transactional
    public void reduceStock(Long recordId, int quantity) {
        Record record = getRecordById(recordId);
        record.reduceStock(quantity);
        recordRepository.save(record);
    }

    /**
     * Get all records with stock below their threshold
     * @return List of records with low stock
     */
    @Transactional(readOnly = true)
    public List<Record> getLowStockRecords() {
        return recordRepository.findLowStockRecords();
    }

    /**
     * Get records that need restocking, ordered by remaining stock
     * @return List of records needing restock
     */
    @Transactional(readOnly = true)
    public List<Record> getRecordsNeedingRestock() {
        return recordRepository.findRecordsNeedingRestock();
    }

    /**
     * Check if record has sufficient stock
     * @param recordId Record identifier
     * @param quantity Quantity to check
     * @return true if record has enough stock
     * @throws RuntimeException if record not found
     */
    @Transactional(readOnly = true)
    public boolean hasEnoughStock(Long recordId, int quantity) {
        Record record = getRecordById(recordId);
        return record.hasEnoughStock(quantity);
    }

    /**
     * Get current stock level for a record
     * @param recordId Record identifier
     * @return Current stock quantity
     * @throws RuntimeException if record not found
     */
    @Transactional(readOnly = true)
    public int getCurrentStock(Long recordId) {
        Record record = getRecordById(recordId);
        return record.getStock();
    }

    /**
     * Get count of records with low stock
     * @return Number of records with low stock
     */
    @Transactional(readOnly = true)
    public long getLowStockCount() {
        return recordRepository.countLowStockRecords();
    }

    /**
     * Get out of stock records
     * @return List of records with zero stock
     */
    @Transactional(readOnly = true)
    public List<Record> getOutOfStockRecords() {
        return recordRepository.findByStockEquals(0);
    }

    /**
     * Update low stock threshold for a record
     * @param recordId Record identifier
     * @param threshold New threshold value
     * @throws RuntimeException if record not found
     */
    @Transactional
    public void updateLowStockThreshold(Long recordId, int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold cannot be negative");
        }
        Record record = getRecordById(recordId);
        record.setLowStockThreshold(threshold);
        recordRepository.save(record);
    }
}