package com.vintagevinyl.repository;

import com.vintagevinyl.model.Record;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class RecordRepositoryTest {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private EntityManager entityManager;

    private Record testRecord;

    @BeforeEach
    void setUp() {
        recordRepository.deleteAll();
        testRecord = createTestRecord(
                "Test Album",
                "Test Artist",
                BigDecimal.valueOf(29.99),
                10,
                5
        );
    }

    private Record createTestRecord(String title, String artist, BigDecimal price, int stock, int lowStockThreshold) {
        Record record = new Record();
        record.setTitle(title);
        record.setArtist(artist);
        record.setAlbum(title);
        record.setReleaseDate(LocalDate.now());
        record.setGenre("Rock");
        record.setPrice(price);
        record.setStock(stock);
        record.setLowStockThreshold(lowStockThreshold);
        return record;
    }

    @Test
    @DisplayName("Should save and retrieve a record")
    void shouldSaveAndRetrieveRecord() {
        // Arrange & Act
        Record savedRecord = recordRepository.save(testRecord);
        Optional<Record> foundRecord = recordRepository.findById(savedRecord.getId());

        // Assert
        assertThat(foundRecord)
                .isPresent()
                .hasValueSatisfying(record -> {
                    assertThat(record.getTitle()).isEqualTo(testRecord.getTitle());
                    assertThat(record.getArtist()).isEqualTo(testRecord.getArtist());
                    assertThat(record.getPrice()).isEqualTo(testRecord.getPrice());
                });
    }

    @Test
    @DisplayName("Should find records by title or artist containing search term")
    void shouldFindByTitleOrArtistContaining() {
        // Arrange
        recordRepository.saveAll(List.of(
                createTestRecord("Abbey Road", "The Beatles", BigDecimal.valueOf(29.99), 10, 5),
                createTestRecord("Let It Be", "The Beatles", BigDecimal.valueOf(27.99), 15, 5),
                createTestRecord("Thriller", "Michael Jackson", BigDecimal.valueOf(34.99), 20, 5)
        ));

        // Act
        Page<Record> foundRecords = recordRepository.findByTitleContainingOrArtistContaining(
                "Beatles",
                "Beatles",
                PageRequest.of(0, 10)
        );

        // Assert
        assertThat(foundRecords.getContent())
                .hasSize(2)
                .extracting(Record::getArtist)
                .containsOnly("The Beatles");
    }

    @Test
    @DisplayName("Should find low stock records")
    void shouldFindLowStockRecords() {
        // Arrange
        recordRepository.saveAll(List.of(
                createTestRecord("Record 1", "Artist 1", BigDecimal.valueOf(29.99), 3, 5),
                createTestRecord("Record 2", "Artist 2", BigDecimal.valueOf(29.99), 0, 5),
                createTestRecord("Record 3", "Artist 3", BigDecimal.valueOf(29.99), 10, 5)
        ));

        // Act
        List<Record> lowStockRecords = recordRepository.findLowStockRecords();

        // Assert
        assertThat(lowStockRecords)
                .hasSize(2)
                .extracting(Record::getStock)
                .allMatch(stock -> stock <= 5);
    }

    @Test
    @DisplayName("Should find records by stock threshold")
    void shouldFindByStockThreshold() {
        // Arrange
        recordRepository.saveAll(List.of(
                createTestRecord("Record 1", "Artist 1", BigDecimal.valueOf(29.99), 2, 5),
                createTestRecord("Record 2", "Artist 2", BigDecimal.valueOf(29.99), 4, 5),
                createTestRecord("Record 3", "Artist 3", BigDecimal.valueOf(29.99), 6, 5)
        ));

        // Act
        List<Record> records = recordRepository.findByStockLessThanEqual(4);

        // Assert
        assertThat(records)
                .hasSize(2)
                .extracting(Record::getStock)
                .allMatch(stock -> stock <= 4);
    }

    @Test
    @DisplayName("Should find out of stock records")
    void shouldFindOutOfStockRecords() {
        // Arrange
        recordRepository.saveAll(List.of(
                createTestRecord("Record 1", "Artist 1", BigDecimal.valueOf(29.99), 0, 5),
                createTestRecord("Record 2", "Artist 2", BigDecimal.valueOf(29.99), 1, 5)
        ));

        // Act
        List<Record> outOfStockRecords = recordRepository.findByStockEquals(0);

        // Assert
        assertThat(outOfStockRecords)
                .hasSize(1)
                .extracting(Record::getStock)
                .containsOnly(0);
    }

    @Test
    @DisplayName("Should update stock quantity")
    @Transactional
    void shouldUpdateStock() {
        // Arrange
        Record savedRecord = recordRepository.save(testRecord);

        // Act
        recordRepository.updateStock(savedRecord.getId(), 100);
        entityManager.flush();
        entityManager.clear();

        Record updatedRecord = recordRepository.findById(savedRecord.getId()).orElseThrow();

        // Assert
        assertThat(updatedRecord.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should count low stock records")
    void shouldCountLowStockRecords() {
        // Arrange
        recordRepository.saveAll(List.of(
                createTestRecord("Record 1", "Artist 1", BigDecimal.valueOf(29.99), 3, 5),
                createTestRecord("Record 2", "Artist 2", BigDecimal.valueOf(29.99), 4, 5),
                createTestRecord("Record 3", "Artist 3", BigDecimal.valueOf(29.99), 6, 5)
        ));

        // Act
        long count = recordRepository.countLowStockRecords();

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find records needing restock ordered by stock")
    void shouldFindRecordsNeedingRestock() {
        // Arrange
        recordRepository.saveAll(List.of(
                createTestRecord("Record 1", "Artist 1", BigDecimal.valueOf(29.99), 3, 5),
                createTestRecord("Record 2", "Artist 2", BigDecimal.valueOf(29.99), 1, 5),
                createTestRecord("Record 3", "Artist 3", BigDecimal.valueOf(29.99), 6, 5)
        ));

        // Act
        List<Record> records = recordRepository.findRecordsNeedingRestock();

        // Assert
        assertThat(records)
                .hasSize(2)
                .extracting(Record::getStock)
                .isSortedAccordingTo(Integer::compareTo);
    }

    @Test
    @DisplayName("Should find records with stock between range")
    void shouldFindRecordsByStockRange() {
        // Arrange
        recordRepository.saveAll(List.of(
                createTestRecord("Record 1", "Artist 1", BigDecimal.valueOf(29.99), 3, 5),
                createTestRecord("Record 2", "Artist 2", BigDecimal.valueOf(29.99), 5, 5),
                createTestRecord("Record 3", "Artist 3", BigDecimal.valueOf(29.99), 7, 5),
                createTestRecord("Record 4", "Artist 4", BigDecimal.valueOf(29.99), 10, 5)
        ));

        // Act
        List<Record> records = recordRepository.findByStockBetween(3, 7);

        // Assert
        assertThat(records)
                .hasSize(3)
                .extracting(Record::getStock)
                .allMatch(stock -> stock >= 3 && stock <= 7);
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void shouldHandlePaginationCorrectly() {
        // Arrange
        List<Record> records = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            records.add(createTestRecord(
                    "Record " + i,
                    "Artist " + i,
                    BigDecimal.valueOf(29.99),
                    10,
                    5
            ));
        }
        recordRepository.saveAll(records);

        // Act
        Page<Record> firstPage = recordRepository.findByTitleContainingOrArtistContaining(
                "Record",
                "Artist",
                PageRequest.of(0, 10)
        );
        Page<Record> secondPage = recordRepository.findByTitleContainingOrArtistContaining(
                "Record",
                "Artist",
                PageRequest.of(1, 10)
        );

        // Assert
        assertThat(firstPage.getContent()).hasSize(10);
        assertThat(secondPage.getContent()).hasSize(10);
        assertThat(firstPage.getTotalElements()).isEqualTo(25);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle edge cases")
    void shouldHandleEdgeCases() {
        // Arrange
        Record maxStockRecord = createTestRecord(
                "Max Stock",
                "Artist",
                BigDecimal.valueOf(29.99),
                Integer.MAX_VALUE,
                5
        );
        Record zeroStockRecord = createTestRecord(
                "Zero Stock",
                "Artist",
                BigDecimal.valueOf(29.99),
                0,
                5
        );
        recordRepository.saveAll(List.of(maxStockRecord, zeroStockRecord));

        // Act & Assert
        assertThat(recordRepository.findByStockEquals(0))
                .hasSize(1)
                .extracting(Record::getTitle)
                .containsExactly("Zero Stock");

        assertThat(recordRepository.findByStockEquals(Integer.MAX_VALUE))
                .hasSize(1)
                .extracting(Record::getTitle)
                .containsExactly("Max Stock");
    }

    @Test
    @DisplayName("Should handle invalid search terms")
    void shouldHandleInvalidSearchTerms() {
        // Act
        Page<Record> emptyResult = recordRepository.findByTitleContainingOrArtistContaining(
                "",
                "",
                PageRequest.of(0, 10)
        );

        Page<Record> nullResult = recordRepository.findByTitleContainingOrArtistContaining(
                null,
                null,
                PageRequest.of(0, 10)
        );

        // Assert
        assertThat(emptyResult).isEmpty();
        assertThat(nullResult).isEmpty();
    }

}