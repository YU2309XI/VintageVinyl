package com.vintagevinyl.service;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CSVImportServiceTest {

    @Mock
    private RecordRepository recordRepository;

    @InjectMocks
    private CSVImportService csvImportService;

    @Captor
    private ArgumentCaptor<List<Record>> recordsCaptor;

    private String validCsvHeader;
    private String validCsvRecord;

    @BeforeEach
    void setUp() {
        validCsvHeader = "Track Name,Artist,Album,Release Date,Cover Image URL,Genre,Price\n";
        validCsvRecord = "Test Track,Test Artist,Test Album,2024-01-01,https://example.com/cover.jpg,Rock,29.99\n";
    }

    /**
     * Test case for importing valid CSV data successfully.
     */
    @Test
    @DisplayName("Should import valid CSV data successfully")
    void importCSVData_ValidData_Success() throws IOException {
        // Given
        String csvData = validCsvHeader + validCsvRecord;
        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        when(recordRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // When
        int importedCount = csvImportService.importCSVData(inputStream);

        // Then
        assertEquals(1, importedCount);
        verify(recordRepository).saveAll(recordsCaptor.capture());

        List<Record> importedRecords = recordsCaptor.getValue();
        Record importedRecord = importedRecords.getFirst();
        assertEquals("Test Track", importedRecord.getTitle());
        assertEquals("Test Artist", importedRecord.getArtist());
        assertEquals("Test Album", importedRecord.getAlbum());
        assertEquals(LocalDate.parse("2024-01-01"), importedRecord.getReleaseDate());
        assertEquals("https://example.com/cover.jpg", importedRecord.getCoverImageUrl());
        assertEquals("Rock", importedRecord.getGenre());
        assertEquals(new BigDecimal("29.99"), importedRecord.getPrice());
    }

    /**
     * Test case for importing multiple records successfully.
     */
    @Test
    @DisplayName("Should import multiple records successfully")
    void importCSVData_MultipleRecords_Success() throws IOException {
        // Given
        String csvData = validCsvHeader +
                validCsvRecord +
                "Another Track,Another Artist,Another Album,2024-02-01,https://example.com/cover2.jpg,Jazz,19.99\n";
        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        when(recordRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // When
        int importedCount = csvImportService.importCSVData(inputStream);

        // Then
        assertEquals(2, importedCount);
        verify(recordRepository).saveAll(recordsCaptor.capture());
        assertEquals(2, recordsCaptor.getValue().size());
    }

    /**
     * Test case for handling invalid date format.
     */
    @Test
    @DisplayName("Should handle invalid date format")
    void importCSVData_InvalidDate_UsesCurrentDate() throws IOException {
        // Given
        String csvData = validCsvHeader +
                "Test Track,Test Artist,Test Album,invalid-date,https://example.com/cover.jpg,Rock,29.99\n";
        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        when(recordRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // When
        int importedCount = csvImportService.importCSVData(inputStream);

        // Then
        assertEquals(1, importedCount);
        verify(recordRepository).saveAll(recordsCaptor.capture());
        Record importedRecord = recordsCaptor.getValue().getFirst();
        assertEquals(LocalDate.now(), importedRecord.getReleaseDate());
    }

    /**
     * Test case for handling an empty CSV file.
     */
    @Test
    @DisplayName("Should handle empty CSV file")
    void importCSVData_EmptyFile_ImportZeroRecords() throws IOException {
        // Given
        String csvData = validCsvHeader;
        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

        // When
        int importedCount = csvImportService.importCSVData(inputStream);

        // Then
        assertEquals(0, importedCount);
        verify(recordRepository).saveAll(recordsCaptor.capture());
        assertTrue(recordsCaptor.getValue().isEmpty());
    }

    /**
     * Test case for continuing import when one record fails.
     */
    @Test
    @DisplayName("Should continue import when one record fails")
    void importCSVData_OneRecordFails_ContinuesWithOthers() throws IOException {
        // Given
        String csvData = validCsvHeader +
                validCsvRecord +
                "Test Track 2,Test Artist 2,Test Album 2,2024-01-01,https://example.com/cover2.jpg,Rock,invalid-price\n" +
                "Test Track 3,Test Artist 3,Test Album 3,2024-01-01,https://example.com/cover3.jpg,Rock,39.99\n";
        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        when(recordRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // When
        int importedCount = csvImportService.importCSVData(inputStream);

        // Then
        assertEquals(2, importedCount); // Only valid records should be counted
        verify(recordRepository).saveAll(recordsCaptor.capture());
        assertEquals(2, recordsCaptor.getValue().size());
    }

    /**
     * Test case for handling missing optional fields.
     */
    @Test
    @DisplayName("Should handle missing optional fields")
    void importCSVData_MissingOptionalFields_Success() throws IOException {
        // Given
        String csvData = validCsvHeader +
                "Test Track,Test Artist,,2024-01-01,,Rock,29.99\n"; // Missing Album and Cover Image URL
        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        when(recordRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // When
        int importedCount = csvImportService.importCSVData(inputStream);

        // Then
        assertEquals(1, importedCount);
        verify(recordRepository).saveAll(recordsCaptor.capture());
        Record importedRecord = recordsCaptor.getValue().getFirst();
        assertEquals("", importedRecord.getAlbum());
        assertEquals("", importedRecord.getCoverImageUrl());
    }

    /**
     * Test case for handling a corrupted CSV file.
     */
    @Test
    @DisplayName("Should throw UncheckedIOException when CSV file is corrupted")
    void importCSVData_CorruptedCSV_ThrowsException() {
        // Given
        String invalidCsvData = "Track Name,Artist\n\"Unclosed quote,Test Artist\nNext Line";
        InputStream inputStream = new ByteArrayInputStream(invalidCsvData.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThrows(UncheckedIOException.class, () -> csvImportService.importCSVData(inputStream));
        verify(recordRepository, never()).saveAll(anyList());
    }

    /**
     * Test case for handling UTF-8 characters in CSV.
     */
    @Test
    @DisplayName("Should handle UTF-8 characters")
    void importCSVData_UTF8Characters_Success() throws IOException {
        // Given
        String csvData = validCsvHeader +
                "Test Track é,Test Artist ñ,Test Album ü,2024-01-01,https://example.com/cover.jpg,Rock,29.99\n";
        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        when(recordRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // When
        int importedCount = csvImportService.importCSVData(inputStream);

        // Then
        assertEquals(1, importedCount);
        verify(recordRepository).saveAll(recordsCaptor.capture());
        Record importedRecord = recordsCaptor.getValue().getFirst();
        assertEquals("Test Track é", importedRecord.getTitle());
        assertEquals("Test Artist ñ", importedRecord.getArtist());
        assertEquals("Test Album ü", importedRecord.getAlbum());
    }
}
