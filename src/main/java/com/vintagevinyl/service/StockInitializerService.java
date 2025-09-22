package com.vintagevinyl.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.vintagevinyl.model.Record;

/**
 * Service class to initialize stock levels for records.
 *
 * This service ensures that stock and low stock threshold values are properly set
 * for records when the application starts.
 * It uses raw SQL queries to update
 * the database directly.
 */
@Service
public class StockInitializerService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    /**
     * Initializes stock and low stock threshold values for records after the application
     * is fully ready.
     *
     * This method retrieves the actual table name for the Record entity and executes
     * SQL queries to update stock levels and low stock thresholds where necessary.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeStock() {
        // Retrieve the actual table name for the Record entity
        String tableName = entityManager.getMetamodel()
                .entity(Record.class)
                .getJavaType()
                .getAnnotation(Table.class)
                .name();

        // Update stock for records where stock is 0
        String updateStockSql = String.format("UPDATE %s SET stock = 10 WHERE stock = 0", tableName);
        jdbcTemplate.update(updateStockSql);

        // Update a low stock threshold for records where it is not set (NULL)
        String updateThresholdSql = String.format(
                "UPDATE %s SET low_stock_threshold = 5 WHERE low_stock_threshold IS NULL",
                tableName);
        jdbcTemplate.update(updateThresholdSql);
    }
}
