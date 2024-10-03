package com.vintagevinyl.service;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;

    @Autowired
    public RecordServiceImpl(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @Override
    public List<Record> getAllRecords() {
        return recordRepository.findAll();
    }

    @Override
    public Optional<Record> getRecordById(Long id) {
        return recordRepository.findById(id);
    }

    @Override
    public Record saveRecord(Record record) {
        if (record.getArtist() == null || record.getArtist().trim().isEmpty()) {
            throw new IllegalArgumentException("Artist cannot be null or empty");
        }
        if (record.getTitle() == null || record.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        // Add more validations as needed
        return recordRepository.save(record);
    }

    @Override
    public void deleteRecord(Long id) {
        recordRepository.deleteById(id);
    }

    @Override
    public List<Record> findByTitleContaining(String title) {
        return recordRepository.findByTitleContainingIgnoreCase(title);
    }

    @Override
    public List<Record> findByArtistContaining(String artist) {
        return recordRepository.findByArtistContainingIgnoreCase(artist);
    }

    @Override
    public List<Record> findByGenre(String genre) {
        return recordRepository.findByGenreIgnoreCase(genre);
    }
}