package com.vintagevinyl.controller;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    @Autowired
    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public ResponseEntity<List<Record>> getAllRecords() {
        List<Record> records = recordService.getAllRecords();
        return new ResponseEntity<>(records, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Record> getRecordById(@PathVariable Long id) {
        return recordService.getRecordById(id)
                .map(record -> new ResponseEntity<>(record, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Record> createRecord(@RequestBody Record record) {
        Record savedRecord = recordService.saveRecord(record);
        return new ResponseEntity<>(savedRecord, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecord(@PathVariable Long id, @RequestBody Record updatedRecord) {
        return recordService.getRecordById(id)
                .map(existingRecord -> {
                    if (updatedRecord.getTitle() != null) {
                        existingRecord.setTitle(updatedRecord.getTitle());
                    }
                    if (updatedRecord.getArtist() != null) {
                        existingRecord.setArtist(updatedRecord.getArtist());
                    }
                    if (updatedRecord.getReleaseYear() != null) {
                        existingRecord.setReleaseYear(updatedRecord.getReleaseYear());
                    }
                    if (updatedRecord.getGenre() != null) {
                        existingRecord.setGenre(updatedRecord.getGenre());
                    }
                    if (updatedRecord.getRecordCondition() != null) {
                        existingRecord.setRecordCondition(updatedRecord.getRecordCondition());
                    }
                    if (updatedRecord.getPrice() != null) {
                        existingRecord.setPrice(updatedRecord.getPrice());
                    }
                    if (updatedRecord.getImageUrl() != null) {
                        existingRecord.setImageUrl(updatedRecord.getImageUrl());
                    }
                    Record savedRecord = recordService.saveRecord(existingRecord);
                    return ResponseEntity.ok(savedRecord);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Record>> searchRecords(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String genre) {
        List<Record> records;
        if (title != null && !title.isEmpty()) {
            records = recordService.findByTitleContaining(title);
        } else if (artist != null && !artist.isEmpty()) {
            records = recordService.findByArtistContaining(artist);
        } else if (genre != null && !genre.isEmpty()) {
            records = recordService.findByGenre(genre);
        } else {
            records = recordService.getAllRecords();
        }
        return new ResponseEntity<>(records, HttpStatus.OK);
    }
}