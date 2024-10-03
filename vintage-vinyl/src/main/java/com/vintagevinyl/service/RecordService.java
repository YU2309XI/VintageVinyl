package com.vintagevinyl.service;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    }}