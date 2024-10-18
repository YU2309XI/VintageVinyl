package com.vintagevinyl.service;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.RecordRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CSVImportService {

    private static final Logger logger = LoggerFactory.getLogger(CSVImportService.class);

    @Autowired
    private RecordRepository recordRepository;

    @Transactional
    public int importCSVData(InputStream inputStream) throws IOException {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            List<Record> records = new ArrayList<>();
            for (CSVRecord csvRecord : csvParser) {
                try {
                    Record record = createRecordFromCSV(csvRecord);
                    records.add(record);
                } catch (Exception e) {
                    logger.error("Error parsing record: " + csvRecord.toString(), e);
                }
            }

            recordRepository.saveAll(records);
            logger.info("Imported " + records.size() + " records successfully.");
            return records.size();
        }
    }

    private Record createRecordFromCSV(CSVRecord csvRecord) {
        Record record = new Record();
        record.setTitle(csvRecord.get("Track Name"));
        record.setArtist(csvRecord.get("Artist"));
        record.setAlbum(csvRecord.get("Album"));
        record.setReleaseDate(parseDate(csvRecord.get("Release Date")));
        record.setCoverImageUrl(csvRecord.get("Cover Image URL"));
        record.setGenre(csvRecord.get("Genre"));
        record.setPrice(new BigDecimal(csvRecord.get("Price")));
        return record;
    }

    private LocalDate parseDate(String dateString) {
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            logger.warn("Unable to parse date: " + dateString + ". Using current date instead.");
            return LocalDate.now();
        }
    }
}