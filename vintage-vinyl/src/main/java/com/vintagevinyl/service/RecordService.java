package com.vintagevinyl.service;

import com.vintagevinyl.model.Record;
import java.util.List;
import java.util.Optional;

public interface RecordService {
    List<Record> getAllRecords();
    Optional<Record> getRecordById(Long id);
    Record saveRecord(Record record);
    void deleteRecord(Long id);
    List<Record> findByTitleContaining(String title);
    List<Record> findByArtistContaining(String artist);
    List<Record> findByGenre(String genre);
}