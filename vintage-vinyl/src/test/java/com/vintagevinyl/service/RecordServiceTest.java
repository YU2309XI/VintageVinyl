package com.vintagevinyl.service;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private ShoppingCartService shoppingCartService;

    @InjectMocks
    private RecordService recordService;

    private Record testRecord;
    private List<Record> recordList;

    @BeforeEach
    void setUp() {
        // Initialize a test record
        testRecord = new Record();
        testRecord.setId(1L);
        testRecord.setTitle("Test Album");
        testRecord.setArtist("Test Artist");
        testRecord.setPrice(new BigDecimal("29.99"));
        testRecord.setStock(10);
        testRecord.setLowStockThreshold(5);
        testRecord.setReleaseDate(LocalDate.now());
        testRecord.setGenre("Rock");

        // Initialize a list of records
        recordList = List.of(testRecord);
    }

    /**
     * Test retrieving all records with pagination
     */
    @Test
    @DisplayName("Should get all records with pagination")
    void getAllRecords_WithPagination_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Record> recordPage = new PageImpl<>(recordList);
        when(recordRepository.findAll(pageable)).thenReturn(recordPage);

        Page<Record> result = recordService.getAllRecords(pageable, null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recordRepository).findAll(pageable);
    }

    /**
     * Test retrieving all records with search functionality
     */
    @Test
    @DisplayName("Should get all records with search")
    void getAllRecords_WithSearch_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        String search = "Test";
        Page<Record> recordPage = new PageImpl<>(recordList);
        when(recordRepository.findByTitleContainingOrArtistContaining(search, search, pageable))
                .thenReturn(recordPage);

        Page<Record> result = recordService.getAllRecords(pageable, search);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recordRepository).findByTitleContainingOrArtistContaining(search, search, pageable);
    }

    /**
     * Test retrieving a record by ID
     */
    @Test
    @DisplayName("Should get record by ID")
    void getRecordById_Success() {
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        Record result = recordService.getRecordById(1L);

        assertNotNull(result);
        assertEquals(testRecord.getId(), result.getId());
        verify(recordRepository).findById(1L);
    }

    /**
     * Test exception when record is not found
     */
    @Test
    @DisplayName("Should throw exception when record not found")
    void getRecordById_NotFound_ThrowsException() {
        when(recordRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> recordService.getRecordById(999L));
        verify(recordRepository).findById(999L);
    }

    /**
     * Test saving a record
     */
    @Test
    @DisplayName("Should save record successfully")
    void saveRecord_Success() {
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        recordService.saveRecord(testRecord);

        verify(recordRepository).save(testRecord);
    }

    /**
     * Test updating a record
     */
    @Test
    @DisplayName("Should update record successfully")
    void updateRecord_Success() {
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        recordService.updateRecord(testRecord);

        verify(recordRepository).save(testRecord);
    }

    /**
     * Test deleting a record
     */
    @Test
    @DisplayName("Should delete record successfully")
    void deleteRecord_Success() {
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        recordService.deleteRecord(1L);

        verify(shoppingCartService).removeAllCartItemsForRecord(1L);
        verify(recordRepository).delete(testRecord);
    }

    /**
     * Test updating stock quantity of a record
     */
    @Test
    @DisplayName("Should update stock quantity successfully")
    void updateStock_Success() {
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        recordService.updateStock(1L, 20);

        assertEquals(20, testRecord.getStock());
        verify(recordRepository).save(testRecord);
    }

    /**
     * Test adding stock to a record
     */
    @Test
    @DisplayName("Should add stock successfully")
    void addStock_Success() {
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        recordService.addStock(1L, 5);

        assertEquals(15, testRecord.getStock());
        verify(recordRepository).save(testRecord);
    }

    /**
     * Test reducing stock of a record
     */
    @Test
    @DisplayName("Should reduce stock successfully")
    void reduceStock_Success() {
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        recordService.reduceStock(1L, 5);

        assertEquals(5, testRecord.getStock());
        verify(recordRepository).save(testRecord);
    }

    /**
     * Test exception when reducing stock below available quantity
     */
    @Test
    @DisplayName("Should throw exception when reducing more than available stock")
    void reduceStock_InsufficientStock_ThrowsException() {
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        assertThrows(IllegalStateException.class, () -> recordService.reduceStock(1L, 15));
    }

    /**
     * Test retrieving low stock records
     */
    @Test
    @DisplayName("Should get low stock records")
    void getLowStockRecords_Success() {
        when(recordRepository.findLowStockRecords()).thenReturn(recordList);

        List<Record> result = recordService.getLowStockRecords();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(recordRepository).findLowStockRecords();
    }

    /**
     * Test retrieving records needing restocking
     */
    @Test
    @DisplayName("Should get records needing restock")
    void getRecordsNeedingRestock_Success() {
        when(recordRepository.findRecordsNeedingRestock()).thenReturn(recordList);

        List<Record> result = recordService.getRecordsNeedingRestock();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(recordRepository).findRecordsNeedingRestock();
    }

    /**
     * Test checking if record has enough stocks
     */
    @Test
    @DisplayName("Should check if record has enough stock")
    void hasEnoughStock_Success() {
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        boolean result = recordService.hasEnoughStock(1L, 5);

        assertTrue(result);
        verify(recordRepository).findById(1L);
    }

    /**
     * Test retrieving current stock level
     */
    @Test
    @DisplayName("Should get current stock level")
    void getCurrentStock_Success() {
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        int result = recordService.getCurrentStock(1L);

        assertEquals(10, result);
        verify(recordRepository).findById(1L);
    }

    /**
     * Test retrieving low stock count
     */
    @Test
    @DisplayName("Should get low stock count")
    void getLowStockCount_Success() {
        when(recordRepository.countLowStockRecords()).thenReturn(1L);

        long result = recordService.getLowStockCount();

        assertEquals(1L, result);
        verify(recordRepository).countLowStockRecords();
    }

    /**
     * Test retrieving out-of-stock records
     */
    @Test
    @DisplayName("Should get out of stock records")
    void getOutOfStockRecords_Success() {
        when(recordRepository.findByStockEquals(0)).thenReturn(recordList);

        List<Record> result = recordService.getOutOfStockRecords();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(recordRepository).findByStockEquals(0);
    }

    /**
     * Test updating a low stock threshold
     */
    @Test
    @DisplayName("Should update low stock threshold successfully")
    void updateLowStockThreshold_Success() {
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        recordService.updateLowStockThreshold(1L, 10);

        assertEquals(10, testRecord.getLowStockThreshold());
        verify(recordRepository).save(testRecord);
    }

    /**
     * Test exception when updating a low stock threshold with a negative value
     */
    @Test
    @DisplayName("Should throw exception when updating threshold with negative value")
    void updateLowStockThreshold_NegativeValue_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> recordService.updateLowStockThreshold(1L, -1));
    }
}
