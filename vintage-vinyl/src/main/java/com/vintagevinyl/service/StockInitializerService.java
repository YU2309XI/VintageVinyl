package com.vintagevinyl.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.vintagevinyl.model.Record;

@Service
public class StockInitializerService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeStock() {
        // Retrieve the actual table name
        String tableName = entityManager.getMetamodel()
                .entity(Record.class)
                .getJavaType()
                .getAnnotation(Table.class)
                .name();

        // Update records with no stock
        String updateStockSql = String.format("UPDATE %s SET stock = 10 WHERE stock = 0", tableName);
        jdbcTemplate.update(updateStockSql);

        // Update records with no threshold set
        String updateThresholdSql = String.format(
                "UPDATE %s SET low_stock_threshold = 5 WHERE low_stock_threshold IS NULL",
                tableName);
        jdbcTemplate.update(updateThresholdSql);
    }
}
