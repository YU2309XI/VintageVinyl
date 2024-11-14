package com.vintagevinyl.integration;

import com.vintagevinyl.model.*;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.service.RecordService;
import com.vintagevinyl.service.StockInitializerService;
import com.vintagevinyl.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for inventory management functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InventoryManagementIntegrationTest {

    @Autowired
    private RecordService recordService;

    @Autowired
    private RecordRepository recordRepository;

    private Record testRecord;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    void setUp() {
        // Create test record
        Record record = new Record();
        record.setTitle("Test Album");
        record.setArtist("Test Artist");
        record.setPrice(new BigDecimal("29.99"));
        record.setStock(10);
        record.setLowStockThreshold(5);
        record.setReleaseDate(LocalDate.now());
        testRecord = recordRepository.save(record);
    }

    /**
     * Test stock addition
     */
    @Test
    void addStock() {
        recordService.addStock(testRecord.getId(), 5);
        Record updatedRecord = recordService.getRecordById(testRecord.getId());
        assertEquals(15, updatedRecord.getStock(), "Stock should be increased by 5");
        assertFalse(updatedRecord.isLowStock(), "Stock should not be low after addition");
    }

    /**
     * Test stock reduction
     */
    @Test
    void reduceStock() {
        recordService.reduceStock(testRecord.getId(), 3);
        Record updatedRecord = recordService.getRecordById(testRecord.getId());
        assertEquals(7, updatedRecord.getStock(), "Stock should be reduced by 3");
        assertFalse(updatedRecord.isLowStock(), "Stock should not be low yet");

        recordService.reduceStock(testRecord.getId(), 3);
        updatedRecord = recordService.getRecordById(testRecord.getId());
        assertTrue(updatedRecord.isLowStock(), "Stock should be low after reduction");
    }

    /**
     * Test insufficient stock handling
     */
    @Test
    void insufficientStock() {
        Exception exception = assertThrows(IllegalStateException.class, () -> recordService.reduceStock(testRecord.getId(), 15), "Should throw exception when reducing more than available stock");

        assertTrue(exception.getMessage().contains("Insufficient stock"),
                "Exception message should indicate insufficient stock");

        Record updatedRecord = recordService.getRecordById(testRecord.getId());
        assertEquals(10, updatedRecord.getStock(), "Stock should remain unchanged");
    }

    /**
     * Test low stock detection
     */
    @Test
    void lowStockDetection() {
        recordService.reduceStock(testRecord.getId(), 6);
        List<Record> lowStockRecords = recordService.getLowStockRecords();
        assertTrue(lowStockRecords.contains(testRecord),
                "Record should be in low stock list");

        long lowStockCount = recordService.getLowStockCount();
        assertTrue(lowStockCount > 0, "Should have at least one low stock record");
    }

    /**
     * Test zero stock handling
     */
    @Test
    void zeroStock() {
        recordService.updateStock(testRecord.getId(), 0);
        List<Record> outOfStockRecords = recordService.getOutOfStockRecords();
        assertTrue(outOfStockRecords.contains(testRecord),
                "Record should be in out of stock list");
    }

    /**
     * Test stock management
     */
    @Test
    void stockManagement() {
        // Create a record with no stock
        Record record = new Record();
        record.setTitle("Test Album");
        record.setArtist("Test Artist");
        record.setStock(0);
        record = recordRepository.save(record);

        // Test setting stock
        recordService.updateStock(record.getId(), 10);

        // Verify update
        Record updatedRecord = recordService.getRecordById(record.getId());
        assertEquals(10, updatedRecord.getStock(), "Stock should be set to 10");
    }

    /**
     * Test threshold update
     */
    @Test
    void updateThreshold() {
        // First set a lower stock
        recordService.updateStock(testRecord.getId(), 7);

        // Then update threshold to trigger low stock state
        recordService.updateLowStockThreshold(testRecord.getId(), 8);

        Record updatedRecord = recordService.getRecordById(testRecord.getId());
        assertEquals(8, updatedRecord.getLowStockThreshold(), "Threshold should be updated");
        assertTrue(updatedRecord.isLowStock(), "Record should be considered low stock with new threshold");
    }

    /**
     * Test negative values handling
     */
    @Test
    void negativeValues() {
        assertThrows(IllegalArgumentException.class, () -> recordService.updateStock(testRecord.getId(), -1), "Should not allow negative stock");

        assertThrows(IllegalArgumentException.class, () -> recordService.addStock(testRecord.getId(), -5), "Should not allow negative stock addition");

        assertThrows(IllegalArgumentException.class, () -> recordService.reduceStock(testRecord.getId(), -3), "Should not allow negative stock reduction");

        assertThrows(IllegalArgumentException.class, () -> recordService.updateLowStockThreshold(testRecord.getId(), -1), "Should not allow negative threshold");
    }
}