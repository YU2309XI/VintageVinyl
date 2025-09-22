package com.vintagevinyl.integration;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.model.User;
import com.vintagevinyl.service.RecordService;
import com.vintagevinyl.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for admin operations
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminOperationsIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RecordService recordService;

    private User testUser;
    private User adminUser;
    private Record testRecord;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    void setUp() {
        // Create admin user
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("admin123");
        adminUser.setEnabled(true);
        adminUser.setAccountNonLocked(true);
        adminUser = userService.save(adminUser);
        userService.setAdminRole(adminUser.getUsername());

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser = userService.save(testUser);

        // Create test record
        Record record = new Record();
        record.setTitle("Test Album");
        record.setArtist("Test Artist");
        record.setPrice(new BigDecimal("29.99"));
        record.setStock(10);
        record.setLowStockThreshold(5);
        record.setReleaseDate(LocalDate.now());
        recordService.saveRecord(record);
        testRecord = record;
    }

    /**
     * Test user management operations
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void userManagement() {
        // Test user activation/deactivation
        userService.deactivateUser(testUser.getId(), "admin");
        User updatedUser = userService.getById(testUser.getId());
        assertFalse(updatedUser.isEnabled(), "User should be deactivated");

        userService.activateUser(testUser.getId(), "admin");
        updatedUser = userService.getById(testUser.getId());
        assertTrue(updatedUser.isEnabled(), "User should be activated");

        // Test user lock/unlock
        userService.lockUser(testUser.getId(), "admin");
        updatedUser = userService.getById(testUser.getId());
        assertFalse(updatedUser.isAccountNonLocked(), "User should be locked");

        userService.unlockUser(testUser.getId(), "admin");
        updatedUser = userService.getById(testUser.getId());
        assertTrue(updatedUser.isAccountNonLocked(), "User should be unlocked");

        // Test admin role assignment
        userService.setAdminRole(testUser.getUsername());
        updatedUser = userService.findByUsername(testUser.getUsername());
        assertTrue(updatedUser.getRoles().contains("ROLE_ADMIN"),
                "User should have admin role");
    }

    /**
     * Test record management operations
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void recordManagement() {
        // Test record creation
        Record newRecord = new Record();
        newRecord.setTitle("New Album");
        newRecord.setArtist("New Artist");
        newRecord.setPrice(new BigDecimal("19.99"));
        newRecord.setStock(5);
        recordService.saveRecord(newRecord);
        assertNotNull(newRecord.getId(), "Record should be saved");

        // Test record update
        newRecord.setTitle("Updated Album");
        recordService.updateRecord(newRecord);
        Record updatedRecord = recordService.getRecordById(newRecord.getId());
        assertEquals("Updated Album", updatedRecord.getTitle(),
                "Record title should be updated");

        // Test record listing
        Page<Record> records = recordService.getAllRecords(PageRequest.of(0, 10), "");
        assertTrue(records.getTotalElements() > 0,
                "Should return existing records");

        // Test record deletion
        recordService.deleteRecord(newRecord.getId());
        assertThrows(RuntimeException.class, () ->
                        recordService.getRecordById(newRecord.getId()),
                "Record should be deleted");
    }

    /**
     * Test inventory management operations
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void inventoryManagement() {
        // Test stock update
        recordService.updateStock(testRecord.getId(), 20);
        Record updatedRecord = recordService.getRecordById(testRecord.getId());
        assertEquals(20, updatedRecord.getStock(),
                "Stock should be updated");

        // Test low stock threshold update
        recordService.updateLowStockThreshold(testRecord.getId(), 10);
        updatedRecord = recordService.getRecordById(testRecord.getId());
        assertEquals(10, updatedRecord.getLowStockThreshold(),
                "Threshold should be updated");

        // Test low stock detection
        recordService.updateStock(testRecord.getId(), 5);
        List<Record> lowStockRecords = recordService.getLowStockRecords();
        assertTrue(lowStockRecords.contains(testRecord),
                "Record should be in low stock list");

        long lowStockCount = recordService.getLowStockCount();
        assertTrue(lowStockCount > 0,
                "Should have low stock records");
    }

    /**
     * Test admin privilege restrictions
     */
    @Test
    void adminPrivilegeRestrictions() {
        // Attempt to deactivate last admin
        Exception exception = assertThrows(IllegalStateException.class, () -> userService.deactivateUser(adminUser.getId(), "admin"), "Should not allow deactivating last admin");

        assertTrue(exception.getMessage().contains("Cannot deactivate the last admin user"),
                "Should show appropriate error message");

        // Attempt to lock last admin
        exception = assertThrows(IllegalStateException.class, () -> userService.lockUser(adminUser.getId(), "admin"), "Should not allow locking last admin");

        assertTrue(exception.getMessage().contains("Cannot lock the last admin user"),
                "Should show appropriate error message");
    }

    /**
     * Test bulk operations
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void bulkOperations() {
        // Create multiple test records
        Record record1 = new Record();
        record1.setTitle("Album 1");
        record1.setArtist("Artist 1");
        record1.setStock(0);
        recordService.saveRecord(record1);

        Record record2 = new Record();
        record2.setTitle("Album 2");
        record2.setArtist("Artist 2");
        record2.setStock(0);
        recordService.saveRecord(record2);

        // Get out of stock records
        List<Record> outOfStockRecords = recordService.getOutOfStockRecords();
        assertTrue(outOfStockRecords.size() >= 2,
                "Should have out of stock records");

        // Get records needing restocking
        List<Record> needingRestock = recordService.getRecordsNeedingRestock();
        assertFalse(needingRestock.isEmpty(),
                "Should have records needing restock");
    }
}