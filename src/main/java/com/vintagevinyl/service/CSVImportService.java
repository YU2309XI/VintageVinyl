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

/**
 * Service class for importing data from a CSV file into the database.
 *
 * This service reads a CSV file, parses its content, maps each record to a
 * Record entity, and saves the entities in the database using the RecordRepository.
 */
@Service
public class CSVImportService {

    private static final Logger logger = LoggerFactory.getLogger(CSVImportService.class);

    @Autowired
    private RecordRepository recordRepository;

    /**
     * Imports data from a CSV file and saves it to the database.
     *
     * @param inputStream the InputStream of the CSV file
     * @return the number of records successfully imported
     * @throws IOException if an I/O error occurs while reading the file
     */
    @Transactional
    public int importCSVData(InputStream inputStream) throws IOException {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            List<Record> records = new ArrayList<>();

            // Loop through each record in the CSV file
            for (CSVRecord csvRecord : csvParser) {
                try {
                    // Create a Record entity from the CSV data
                    Record record = createRecordFromCSV(csvRecord);
                    records.add(record);
                } catch (Exception e) {
                    // Log and skip invalid records
                    logger.error("Error parsing record: " + csvRecord.toString(), e);
                }
            }

            // Save all valid records to the database
            recordRepository.saveAll(records);
            logger.info("Imported " + records.size() + " records successfully.");
            return records.size();
        }
    }

    /**
     * Creates a Record entity from a CSV record.
     *
     * @param csvRecord the CSV record to map
     * @return the Record entity created
     */
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

    /**
     * Parses a date string into a LocalDate object.
     *
     * If the date string cannot be parsed, the current date is used as a fallback.
     *
     * @param dateString the date string to parse
     * @return the parsed LocalDate object, or the current date if parsing fails
     */
    private LocalDate parseDate(String dateString) {
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            logger.warn("Unable to parse date: " + dateString + ". Using current date instead.");
            return LocalDate.now();
        }
    }
}
