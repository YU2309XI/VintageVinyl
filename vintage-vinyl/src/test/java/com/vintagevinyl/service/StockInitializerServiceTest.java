package com.vintagevinyl.service;

import com.vintagevinyl.model.Record;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockInitializerServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Metamodel metamodel;

    @Mock
    private EntityType<Record> entityType;

    @InjectMocks
    private StockInitializerService stockInitializerService;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    @BeforeEach
    void setUp() {
        // Mock EntityManager and Metamodel to simulate entity setup
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        when(metamodel.entity(Record.class)).thenReturn(entityType);
        when(entityType.getJavaType()).thenReturn(Record.class);
    }

    /**
     * Test initializing stock and low stock threshold values when updates are required.
     */
    @Test
    @DisplayName("Should initialize stock values correctly")
    void initializeStock_ShouldUpdateStockAndThreshold() {
        when(jdbcTemplate.update(anyString())).thenReturn(1);

        stockInitializerService.initializeStock();

        verify(jdbcTemplate, times(2)).update(sqlCaptor.capture());
        List<String> capturedSqls = sqlCaptor.getAllValues();

        // Verify stock update SQL
        assertTrue(capturedSqls.get(0).contains("SET stock = 10"));
        assertTrue(capturedSqls.get(0).contains("WHERE stock = 0"));

        // Verify low stock threshold update SQL
        assertTrue(capturedSqls.get(1).contains("SET low_stock_threshold = 5"));
        assertTrue(capturedSqls.get(1).contains("WHERE low_stock_threshold IS NULL"));
    }

    /**
     * Test handling the case where no records need updating.
     */
    @Test
    @DisplayName("Should handle when no records need updating")
    void initializeStock_NoRecordsNeedUpdate() {
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        stockInitializerService.initializeStock();

        verify(jdbcTemplate, times(2)).update(anyString());
    }

    /**
     * Test the execution of correct SQL statements for stock initialization.
     */
    @Test
    @DisplayName("Should execute correct SQL updates")
    void initializeStock_ExecutesCorrectUpdates() {
        when(jdbcTemplate.update(anyString())).thenReturn(1);

        stockInitializerService.initializeStock();

        verify(jdbcTemplate, times(2)).update(sqlCaptor.capture());
        List<String> sqls = sqlCaptor.getAllValues();

        // Verify stock update SQL
        String stockUpdateSql = sqls.get(0);
        assertTrue(stockUpdateSql.contains("UPDATE"));
        assertTrue(stockUpdateSql.contains("SET stock = 10"));
        assertTrue(stockUpdateSql.contains("WHERE stock = 0"));

        // Verify low stock threshold update SQL
        String thresholdUpdateSql = sqls.get(1);
        assertTrue(thresholdUpdateSql.contains("UPDATE"));
        assertTrue(thresholdUpdateSql.contains("SET low_stock_threshold = 5"));
        assertTrue(thresholdUpdateSql.contains("WHERE low_stock_threshold IS NULL"));
    }

    /**
     * Test handling database errors during stock initialization.
     */
    @Test
    @DisplayName("Should handle database errors gracefully")
    void initializeStock_HandlesDatabaseErrors() {
        RuntimeException expectedException = new RuntimeException("Database error");
        when(jdbcTemplate.update(anyString())).thenThrow(expectedException);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> stockInitializerService.initializeStock());
        assertEquals("Database error", thrown.getMessage());
        verify(jdbcTemplate).update(anyString());
    }

    /**
     * Test the exact SQL format used for updates in stock initialization.
     */
    @Test
    @DisplayName("Should use correct SQL statements in exact format")
    void initializeStock_CorrectSqlFormat() {
        when(jdbcTemplate.update(anyString())).thenReturn(1);

        stockInitializerService.initializeStock();

        verify(jdbcTemplate, times(2)).update(sqlCaptor.capture());
        List<String> sqls = sqlCaptor.getAllValues();

        String expectedStockSql = "UPDATE records SET stock = 10 WHERE stock = 0";
        String expectedThresholdSql =
                "UPDATE records SET low_stock_threshold = 5 WHERE low_stock_threshold IS NULL";

        assertTrue(sqls.stream().anyMatch(sql -> sql.trim().equalsIgnoreCase(expectedStockSql)));
        assertTrue(sqls.stream().anyMatch(sql -> sql.trim().equalsIgnoreCase(expectedThresholdSql)));
    }
}
