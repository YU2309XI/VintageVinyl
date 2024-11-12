package com.vintagevinyl.controller;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.model.User;
import com.vintagevinyl.service.RecordService;
import com.vintagevinyl.service.UserService;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private RecordService recordService;

    @MockBean
    private CartItemCountControllerAdvice cartItemCountControllerAdvice;

    private User testUser;
    private Record testRecord;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");

        testRecord = new Record();
        testRecord.setId(1L);
        testRecord.setTitle("Test Record");
        testRecord.setPrice(new BigDecimal("29.99"));
        testRecord.setStock(10);
        testRecord.setLowStockThreshold(5);
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {
        // Existing tests remain the same...

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should edit user successfully")
        void editUser_Success() throws Exception {
            when(userService.updateUser(eq(1L), any(User.class), anyString())).thenReturn(testUser);

            mockMvc.perform(post("/admin/users/1/edit")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User updated successfully"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete user successfully")
        void deleteUser_Success() throws Exception {
            doNothing().when(userService).delete(anyLong());

            mockMvc.perform(post("/admin/users/1/delete")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User deleted successfully"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should deactivate user successfully")
        void deactivateUser_Success() throws Exception {
            when(userService.deactivateUser(eq(1L), anyString())).thenReturn(testUser);

            mockMvc.perform(post("/admin/users/1/deactivate")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User deactivated successfully"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should lock user successfully")
        void lockUser_Success() throws Exception {
            when(userService.lockUser(eq(1L), anyString())).thenReturn(testUser);

            mockMvc.perform(post("/admin/users/1/lock")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User locked successfully"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should unlock user successfully")
        void unlockUser_Success() throws Exception {
            when(userService.unlockUser(eq(1L), anyString())).thenReturn(testUser);

            mockMvc.perform(post("/admin/users/1/unlock")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User unlocked successfully"));
        }
    }

    @Nested
    @DisplayName("Inventory Management Tests")
    class InventoryManagementTests {
        // Existing tests remain the same...

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should add stock successfully")
        void addStock_Success() throws Exception {
            doNothing().when(recordService).addStock(anyLong(), anyInt());

            mockMvc.perform(post("/admin/api/records/1/stock/add")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("quantity", 5))))
                    .andExpect(status().isOk());

            verify(recordService).addStock(1L, 5);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get low stock records")
        void getLowStockRecords_Success() throws Exception {
            List<Record> lowStockRecords = Collections.singletonList(testRecord);
            when(recordService.getLowStockRecords()).thenReturn(lowStockRecords);

            mockMvc.perform(get("/admin/api/records/low-stock"))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(lowStockRecords)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get inventory status")
        void getInventoryStatus_Success() throws Exception {
            when(recordService.getLowStockCount()).thenReturn(1L);
            when(recordService.getOutOfStockRecords()).thenReturn(Collections.emptyList());
            when(recordService.getRecordsNeedingRestock()).thenReturn(Collections.singletonList(testRecord));

            mockMvc.perform(get("/admin/api/inventory/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lowStockCount").value(1))
                    .andExpect(jsonPath("$.outOfStockCount").value(0));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update stock threshold")
        void updateThreshold_Success() throws Exception {
            doNothing().when(recordService).updateLowStockThreshold(anyLong(), anyInt());

            mockMvc.perform(put("/admin/api/records/1/threshold")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("threshold", 10))))
                    .andExpect(status().isOk());

            verify(recordService).updateLowStockThreshold(1L, 10);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should perform batch stock update")
        void batchUpdateStock_Success() throws Exception {
            List<Map<String, Object>> updates = Arrays.asList(
                    Map.of("id", 1, "quantity", 20),
                    Map.of("id", 2, "quantity", 30)
            );

            mockMvc.perform(put("/admin/api/records/stock/batch")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updates)))
                    .andExpect(status().isOk());

            verify(recordService).updateStock(1L, 20);
            verify(recordService).updateStock(2L, 30);
        }
    }
}