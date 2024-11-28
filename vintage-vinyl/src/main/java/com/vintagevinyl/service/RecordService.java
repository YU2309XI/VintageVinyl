package com.vintagevinyl.service;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class for managing records (vinyl records).
 *
 * This class provides methods for managing record entities, including CRUD operations,
 * inventory management, and querying records based on stock levels and search criteria.
 */
@Service
public class RecordService {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * Retrieves all records, optionally filtered by a search term, with pagination support.
     *
     * @param pageable the pagination and sorting information
     * @param search the search term to filter records by title or artist
     * @return a page of records
     */
    public Page<Record> getAllRecords(Pageable pageable, String search) {
        if (search != null && !search.isEmpty()) {
            return recordRepository.findByTitleContainingOrArtistContaining(search, search, pageable);
        } else {
            return recordRepository.findAll(pageable);
        }
    }

    /**
     * Retrieves a record by its ID.
     *
     * @param id the ID of the record
     * @return the record entity
     * @throws RuntimeException if the record is not found
     */
    @Transactional(readOnly = true)
    public Record getRecordById(Long id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
    }

    /**
     * Saves a new record or updates an existing one.
     *
     * @param record the record entity to save
     */
    public void saveRecord(Record record) {
        recordRepository.save(record);
    }

    /**
     * Updates an existing record.
     *
     * @param record the record entity with updated information
     */
    public void updateRecord(Record record) {
        recordRepository.save(record);
    }

    /**
     * Deletes a record by its ID and removes associated items from all shopping carts.
     *
     * @param id the ID of the record to delete
     */
    @Transactional
    public void deleteRecord(Long id) {
        Record record = recordRepository.findById(id).orElse(null);
        if (record != null) {
            shoppingCartService.removeAllCartItemsForRecord(id);
            recordRepository.delete(record);
        }
    }

    // Inventory management methods

    /**
     * Updates the stock quantity of a record.
     *
     * @param recordId the ID of the record
     * @param quantity the new stock quantity
     * @throws RuntimeException if the record is not found
     */
    @Transactional
    public void updateStock(Long recordId, int quantity) {
        Record record = getRecordById(recordId);
        record.setStock(quantity);
        recordRepository.save(record);
    }

    /**
     * Adds stock to a record.
     *
     * @param recordId the ID of the record
     * @param quantity the quantity to add
     * @throws RuntimeException if the record is not found
     */
    @Transactional
    public void addStock(Long recordId, int quantity) {
        Record record = getRecordById(recordId);
        record.addStock(quantity);
        recordRepository.save(record);
    }

    /**
     * Reduces stock from a record.
     *
     * @param recordId the ID of the record
     * @param quantity the quantity to reduce
     * @throws RuntimeException if the record is not found or stock is insufficient
     */
    @Transactional
    public void reduceStock(Long recordId, int quantity) {
        Record record = getRecordById(recordId);
        record.reduceStock(quantity);
        recordRepository.save(record);
    }

    /**
     * Retrieves records with stock below their low stock threshold.
     *
     * @return a list of records with low stock
     */
    @Transactional(readOnly = true)
    public List<Record> getLowStockRecords() {
        return recordRepository.findLowStockRecords();
    }

    /**
     * Retrieves records that need restocking, ordered by their remaining stock.
     *
     * @return a list of records needing restocking
     */
    @Transactional(readOnly = true)
    public List<Record> getRecordsNeedingRestock() {
        return recordRepository.findRecordsNeedingRestock();
    }

    /**
     * Checks if a record has sufficient stock for a given quantity.
     *
     * @param recordId the ID of the record
     * @param quantity the quantity to check
     * @return true if the record has enough stocks
     * @throws RuntimeException if the record is not found
     */
    @Transactional(readOnly = true)
    public boolean hasEnoughStock(Long recordId, int quantity) {
        Record record = getRecordById(recordId);
        return record.hasEnoughStock(quantity);
    }

    /**
     * Retrieves the current stock level of a record.
     *
     * @param recordId the ID of the record
     * @return the current stock quantity
     * @throws RuntimeException if the record is not found
     */
    @Transactional(readOnly = true)
    public int getCurrentStock(Long recordId) {
        Record record = getRecordById(recordId);
        return record.getStock();
    }

    /**
     * Counts the number of records with stock below their low-stock threshold.
     *
     * @return the count of low-stock records
     */
    @Transactional(readOnly = true)
    public long getLowStockCount() {
        return recordRepository.countLowStockRecords();
    }

    /**
     * Retrieves records that are out of stock.
     *
     * @return a list of records with zero stocks
     */
    @Transactional(readOnly = true)
    public List<Record> getOutOfStockRecords() {
        return recordRepository.findByStockEquals(0);
    }

    /**
     * Updates the low stock threshold for a record.
     *
     * @param recordId the ID of the record
     * @param threshold the new low stock threshold
     * @throws RuntimeException if the record is not found
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
