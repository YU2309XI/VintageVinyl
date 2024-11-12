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
        // Initialize test record
        testRecord = new Record();
        testRecord.setId(1L);
        testRecord.setTitle("Test Album");
        testRecord.setArtist("Test Artist");
        testRecord.setPrice(new BigDecimal("29.99"));
        testRecord.setStock(10);
        testRecord.setLowStockThreshold(5);
        testRecord.setReleaseDate(LocalDate.now());
        testRecord.setGenre("Rock");

        // Initialize a record list
        recordList = List.of(testRecord);
    }

    @Test
    @DisplayName("Should get all records with pagination")
    void getAllRecords_WithPagination_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Record> recordPage = new PageImpl<>(recordList);
        when(recordRepository.findAll(pageable)).thenReturn(recordPage);

        // When
        Page<Record> result = recordService.getAllRecords(pageable, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recordRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should get all records with search")
    void getAllRecords_WithSearch_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        String search = "Test";
        Page<Record> recordPage = new PageImpl<>(recordList);
        when(recordRepository.findByTitleContainingOrArtistContaining(search, search, pageable))
                .thenReturn(recordPage);

        // When
        Page<Record> result = recordService.getAllRecords(pageable, search);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recordRepository).findByTitleContainingOrArtistContaining(search, search, pageable);
    }

    @Test
    @DisplayName("Should get record by ID")
    void getRecordById_Success() {
        // Given
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        // When
        Record result = recordService.getRecordById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testRecord.getId(), result.getId());
        verify(recordRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when record not found")
    void getRecordById_NotFound_ThrowsException() {
        // Given
        when(recordRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> recordService.getRecordById(999L));
        verify(recordRepository).findById(999L);
    }

    @Test
    @DisplayName("Should save record successfully")
    void saveRecord_Success() {
        // Given
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        // When
        recordService.saveRecord(testRecord);

        // Then
        verify(recordRepository).save(testRecord);
    }

    @Test
    @DisplayName("Should update record successfully")
    void updateRecord_Success() {
        // Given
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        // When
        recordService.updateRecord(testRecord);

        // Then
        verify(recordRepository).save(testRecord);
    }

    @Test
    @DisplayName("Should delete record successfully")
    void deleteRecord_Success() {
        // Given
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        // When
        recordService.deleteRecord(1L);

        // Then
        verify(shoppingCartService).removeAllCartItemsForRecord(1L);
        verify(recordRepository).delete(testRecord);
    }

    @Test
    @DisplayName("Should update stock quantity successfully")
    void updateStock_Success() {
        // Given
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        // When
        recordService.updateStock(1L, 20);

        // Then
        assertEquals(20, testRecord.getStock());
        verify(recordRepository).save(testRecord);
    }

    @Test
    @DisplayName("Should add stock successfully")
    void addStock_Success() {
        // Given
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        // When
        recordService.addStock(1L, 5);

        // Then
        assertEquals(15, testRecord.getStock());
        verify(recordRepository).save(testRecord);
    }

    @Test
    @DisplayName("Should reduce stock successfully")
    void reduceStock_Success() {
        // Given
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        // When
        recordService.reduceStock(1L, 5);

        // Then
        assertEquals(5, testRecord.getStock());
        verify(recordRepository).save(testRecord);
    }

    @Test
    @DisplayName("Should throw exception when reducing more than available stock")
    void reduceStock_InsufficientStock_ThrowsException() {
        // Given
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        // When & Then
        assertThrows(IllegalStateException.class, () -> recordService.reduceStock(1L, 15));
    }

    @Test
    @DisplayName("Should get low stock records")
    void getLowStockRecords_Success() {
        // Given
        when(recordRepository.findLowStockRecords()).thenReturn(recordList);

        // When
        List<Record> result = recordService.getLowStockRecords();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(recordRepository).findLowStockRecords();
    }

    @Test
    @DisplayName("Should get records needing restock")
    void getRecordsNeedingRestock_Success() {
        // Given
        when(recordRepository.findRecordsNeedingRestock()).thenReturn(recordList);

        // When
        List<Record> result = recordService.getRecordsNeedingRestock();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(recordRepository).findRecordsNeedingRestock();
    }

    @Test
    @DisplayName("Should check if record has enough stock")
    void hasEnoughStock_Success() {
        // Given
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        // When
        boolean result = recordService.hasEnoughStock(1L, 5);

        // Then
        assertTrue(result);
        verify(recordRepository).findById(1L);
    }

    @Test
    @DisplayName("Should get current stock level")
    void getCurrentStock_Success() {
        // Given
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        // When
        int result = recordService.getCurrentStock(1L);

        // Then
        assertEquals(10, result);
        verify(recordRepository).findById(1L);
    }

    @Test
    @DisplayName("Should get low stock count")
    void getLowStockCount_Success() {
        // Given
        when(recordRepository.countLowStockRecords()).thenReturn(1L);

        // When
        long result = recordService.getLowStockCount();

        // Then
        assertEquals(1L, result);
        verify(recordRepository).countLowStockRecords();
    }

    @Test
    @DisplayName("Should get out of stock records")
    void getOutOfStockRecords_Success() {
        // Given
        when(recordRepository.findByStockEquals(0)).thenReturn(recordList);

        // When
        List<Record> result = recordService.getOutOfStockRecords();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(recordRepository).findByStockEquals(0);
    }

    @Test
    @DisplayName("Should update low stock threshold successfully")
    void updateLowStockThreshold_Success() {
        // Given
        when(recordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(recordRepository.save(any(Record.class))).thenReturn(testRecord);

        // When
        recordService.updateLowStockThreshold(1L, 10);

        // Then
        assertEquals(10, testRecord.getLowStockThreshold());
        verify(recordRepository).save(testRecord);
    }

    @Test
    @DisplayName("Should throw exception when updating threshold with negative value")
    void updateLowStockThreshold_NegativeValue_ThrowsException() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class,
                () -> recordService.updateLowStockThreshold(1L, -1));
    }
}