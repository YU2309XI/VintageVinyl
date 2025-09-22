package com.vintagevinyl.controller;

import com.vintagevinyl.exception.RecordNotFoundException;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.model.User;
import com.vintagevinyl.service.CSVImportService;
import com.vintagevinyl.service.RecordService;
import com.vintagevinyl.service.UserService;
import com.vintagevinyl.service.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecordService recordService;

    @MockBean
    private WishlistService wishlistService;

    @MockBean
    private UserService userService;

    @MockBean
    private CSVImportService csvImportService;

    private Record testRecord;
    private User testUser;

    @BeforeEach
    void setUp() {
        testRecord = new Record();
        testRecord.setId(1L);
        testRecord.setTitle("Test Album");
        testRecord.setArtist("Test Artist");
        testRecord.setPrice(new BigDecimal("29.99"));
        testRecord.setStock(10);
        testRecord.setLowStockThreshold(5);
        testRecord.setReleaseDate(LocalDate.of(2024, 1, 1));

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
    }

    @Nested
    @DisplayName("Record Listing Tests")
    class RecordListingTests {

        /**
         * Test case for listing records with pagination.
         */
        @Test
        @WithMockUser
        @DisplayName("Should list records with pagination")
        void listRecords_Success() throws Exception {
            List<Record> records = Collections.singletonList(testRecord);
            Page<Record> recordPage = new PageImpl<>(records);
            when(recordService.getAllRecords(any(PageRequest.class), anyString())).thenReturn(recordPage);

            mockMvc.perform(get("/records")
                            .param("page", "1")
                            .param("search", ""))
                    .andExpect(status().isOk())
                    .andExpect(view().name("records"))
                    .andExpect(model().attributeExists("records", "currentPage", "totalPages", "search"));
        }

        /**
         * Test case for viewing details of a specific record.
         */
        @Test
        @WithMockUser
        @DisplayName("Should show record details")
        void viewRecord_Success() throws Exception {
            when(recordService.getRecordById(1L)).thenReturn(testRecord);

            mockMvc.perform(get("/records/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("record-detail"))
                    .andExpect(model().attributeExists("record", "inStock", "stockStatus"));
        }

        /**
         * Test case for handling non-existent record access.
         */
        @Test
        @WithMockUser
        @DisplayName("Should handle record not found")
        void viewRecord_NotFound() throws Exception {
            when(recordService.getRecordById(1L)).thenReturn(null);

            mockMvc.perform(get("/records/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/records?error=Record+not+found"));
        }
    }

    @Nested
    @DisplayName("Admin Record Management Tests")
    class AdminRecordTests {

        /**
         * Test case for displaying the form to add a new record.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should show new record form")
        void newRecordForm_Success() throws Exception {
            mockMvc.perform(get("/records/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("record-form"))
                    .andExpect(model().attributeExists("record"));
        }

        /**
         * Test case for saving a new record.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should save new record")
        void saveRecord_Success() throws Exception {
            mockMvc.perform(post("/records")
                            .with(csrf())
                            .param("title", "Test Album")
                            .param("artist", "Test Artist")
                            .param("price", "29.99")
                            .param("releaseYear", "2024")
                            .param("initialStock", "10"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/records"));

            verify(recordService).saveRecord(any(Record.class));
        }

        /**
         * Test case for displaying the edit form for a specific record.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should show edit record form")
        void editRecordForm_Success() throws Exception {
            when(recordService.getRecordById(1L)).thenReturn(testRecord);

            mockMvc.perform(get("/records/1/edit"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("record-form"))
                    .andExpect(model().attributeExists("record", "currentStock", "lowStockThreshold"));
        }

        /**
         * Test case for handling non-existent record access in edit form.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle edit form for non-existent record")
        void editRecordForm_NotFound() throws Exception {
            when(recordService.getRecordById(1L)).thenReturn(null);

            mockMvc.perform(get("/records/1/edit"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        /**
         * Test case for updating an existing record.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update record")
        void updateRecord_Success() throws Exception {
            when(recordService.getRecordById(1L)).thenReturn(testRecord);

            mockMvc.perform(post("/records/1")
                            .with(csrf())
                            .param("title", "Updated Album")
                            .param("artist", "Updated Artist")
                            .param("price", "39.99")
                            .param("releaseYear", "2024"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/records"));

            verify(recordService).updateRecord(any(Record.class));
        }

        /**
         * Test case for deleting a record.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete record")
        void deleteRecord_Success() throws Exception {
            mockMvc.perform(post("/records/1/delete")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/records"));

            verify(recordService).deleteRecord(1L);
        }

        /**
         * Test case for displaying the CSV import form.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should show import form")
        void showImportForm_Success() throws Exception {
            mockMvc.perform(get("/records/import"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("record-import"));
        }

        /**
         * Test case for handling successful CSV import.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle CSV import")
        void handleImport_Success() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.csv",
                    "text/csv",
                    "test data".getBytes()
            );

            when(csvImportService.importCSVData(any())).thenReturn(5);

            mockMvc.perform(multipart("/records/import")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/records/import"))
                    .andExpect(flash().attributeExists("message"));
        }

        /**
         * Test case for handling empty file upload during CSV import.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle empty file upload")
        void handleImport_EmptyFile() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.csv",
                    "text/csv",
                    new byte[0]
            );

            mockMvc.perform(multipart("/records/import")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/records/import"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("Stock Management Tests")
    class StockManagementTests {

        /**
         * Test case for checking stock status as "In Stock."
         */
        @Test
        @WithMockUser
        @DisplayName("Should check stock status")
        void checkStockStatus_Success() throws Exception {
            when(recordService.getRecordById(1L)).thenReturn(testRecord);

            mockMvc.perform(get("/records/1/stock-status"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("In Stock"));
        }

        /**
         * Test case for checking stock status as "Out of Stock."
         */
        @Test
        @WithMockUser
        @DisplayName("Should check out of stock status")
        void checkStockStatus_OutOfStock() throws Exception {
            testRecord.setStock(0);
            when(recordService.getRecordById(1L)).thenReturn(testRecord);

            mockMvc.perform(get("/records/1/stock-status"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Out of Stock"));
        }

        /**
         * Test case for checking stock status as "Low Stock."
         */
        @Test
        @WithMockUser
        @DisplayName("Should check low stock status")
        void checkStockStatus_LowStock() throws Exception {
            testRecord.setStock(3);
            when(recordService.getRecordById(1L)).thenReturn(testRecord);

            mockMvc.perform(get("/records/1/stock-status"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Low Stock"));
        }
    }

    @Nested
    @DisplayName("Wishlist Integration Tests")
    class WishlistTests {

        /**
         * Test case for adding a record to the wishlist.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should add record to wishlist")
        void addToWishlist_Success() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(recordService.getRecordById(1L)).thenReturn(testRecord);
            doNothing().when(wishlistService).addToWishlist(any(User.class), any(Record.class));

            mockMvc.perform(post("/records/1/addToWishlist")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/records/1"))
                    .andExpect(flash().attributeExists("message"));
        }

        /**
         * Test case for handling errors while adding a record to the wishlist.
         */
        @Test
        @WithMockUser(username = "testUser")
        @DisplayName("Should handle wishlist error")
        void addToWishlist_Error() throws Exception {
            when(userService.findByUsername("testUser")).thenReturn(testUser);
            when(recordService.getRecordById(1L)).thenThrow(new RuntimeException("Failed to add to wishlist"));

            mockMvc.perform(post("/records/1/addToWishlist")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/records/1"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    /**
     * Test case for handling RecordNotFoundException.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle record not found exception")
    void handleRecordNotFoundException() throws Exception {
        when(recordService.getRecordById(1L))
                .thenThrow(new RecordNotFoundException("Record not found"));

        mockMvc.perform(get("/records/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"));
    }
}
