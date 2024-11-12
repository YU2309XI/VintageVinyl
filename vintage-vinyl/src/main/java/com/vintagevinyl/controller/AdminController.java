package com.vintagevinyl.controller;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.model.User;
import com.vintagevinyl.service.RecordService;
import com.vintagevinyl.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RecordService recordService;

    @GetMapping
    public String adminConsole(Model model) {
        model.addAttribute("users", userService.getAll());
        return "admin-console";
    }

    @PostMapping("/set-admin")
    @ResponseBody
    public ResponseEntity<String> setAdminRole(@RequestParam String username) {
        try {
            userService.setAdminRole(username);
            return ResponseEntity.ok("User role updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating user role: " + e.getMessage());
        }
    }

    @PostMapping("/users/{id}/edit")
    @ResponseBody
    public ResponseEntity<String> editUser(@PathVariable Long id,
                                           @RequestBody User updatedUser,
                                           @AuthenticationPrincipal UserDetails currentUser) {
        try {
            userService.updateUser(id, updatedUser, currentUser.getUsername());
            return ResponseEntity.ok("User updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating user: " + e.getMessage());
        }
    }

    @PostMapping("/users/{id}/delete")
    @ResponseBody
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Cannot delete user: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting user: " + e.getMessage());
        }
    }

    @PostMapping("/users/{id}/activate")
    @ResponseBody
    public ResponseEntity<String> activateUser(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails currentUser) {
        try {
            userService.activateUser(id, currentUser.getUsername());
            return ResponseEntity.ok("User activated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error activating user: " + e.getMessage());
        }
    }

    @PostMapping("/users/{id}/deactivate")
    @ResponseBody
    public ResponseEntity<String> deactivateUser(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails currentUser) {
        try {
            userService.deactivateUser(id, currentUser.getUsername());
            return ResponseEntity.ok("User deactivated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deactivating user: " + e.getMessage());
        }
    }

    @PostMapping("/users/{id}/lock")
    @ResponseBody
    public ResponseEntity<String> lockUser(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails currentUser) {
        try {
            userService.lockUser(id, currentUser.getUsername());
            return ResponseEntity.ok("User locked successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error locking user: " + e.getMessage());
        }
    }

    @PostMapping("/users/{id}/unlock")
    @ResponseBody
    public ResponseEntity<String> unlockUser(@PathVariable Long id,
                                             @AuthenticationPrincipal UserDetails currentUser) {
        try {
            userService.unlockUser(id, currentUser.getUsername());
            return ResponseEntity.ok("User unlocked successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error unlocking user: " + e.getMessage());
        }
    }

    /**
     * Display inventory management page
     */
    @GetMapping("/inventory")
    public String inventoryManagement(Model model) {
        // Use PageRequest.of(0, Integer.MAX_VALUE) to retrieve all records
        Page<Record> recordPage = recordService.getAllRecords(
                PageRequest.of(0, Integer.MAX_VALUE),
                ""
        );

        // Retrieve the list of all records
        List<Record> records = recordPage.getContent();
        model.addAttribute("records", records);

        // Calculate total stock
        int totalStock = records.stream()
                .mapToInt(Record::getStock)
                .sum();
        model.addAttribute("totalStock", totalStock);

        // Low stock and out of stock records
        model.addAttribute("lowStockRecords", recordService.getLowStockRecords());
        model.addAttribute("outOfStockRecords", recordService.getOutOfStockRecords());
        model.addAttribute("lowStockCount", recordService.getLowStockCount());

        return "inventory-management";
    }

    /**
     * Update stock quantity for a record
     */
    @PutMapping("/api/records/{id}/stock")
    @ResponseBody
    public ResponseEntity<?> updateStock(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            logger.info("Stock update requested by {} for record {}",
                    currentUser.getUsername(), id);

            Integer quantity = request.get("quantity");
            if (quantity == null) {
                return ResponseEntity.badRequest().body("Quantity is required");
            }

            recordService.updateStock(id, quantity);
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            logger.error("Error updating stock", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Add stock to a record
     */
    @PostMapping("/api/records/{id}/stock/add")
    @ResponseBody
    public ResponseEntity<?> addStock(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {

        try {
            Integer quantity = request.get("quantity");
            if (quantity == null) {
                return ResponseEntity.badRequest().body("Quantity is required");
            }

            recordService.addStock(id, quantity);
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            logger.error("Error adding stock", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get low-stock records
     */
    @GetMapping("/api/records/low-stock")
    @ResponseBody
    public ResponseEntity<List<Record>> getLowStockRecords() {
        return ResponseEntity.ok(recordService.getLowStockRecords());
    }

    /**
     * Get inventory status dashboard data
     */
    @GetMapping("/api/inventory/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getInventoryStatus() {
        Map<String, Object> status = Map.of(
                "lowStockCount", recordService.getLowStockCount(),
                "outOfStockCount", recordService.getOutOfStockRecords().size(),
                "needingRestock", recordService.getRecordsNeedingRestock()
        );
        return ResponseEntity.ok(status);
    }

    /**
     * Batch update stock quantities
     */
    @PutMapping("/api/records/stock/batch")
    @ResponseBody
    public ResponseEntity<?> batchUpdateStock(
            @RequestBody List<Map<String, Object>> updates) {

        try {
            for (Map<String, Object> update : updates) {
                Long recordId = ((Number) update.get("id")).longValue();
                Integer quantity = ((Number) update.get("quantity")).intValue();
                recordService.updateStock(recordId, quantity);
            }
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error in batch stock update", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Update a low stock threshold for a record
     */
    @PutMapping("/api/records/{id}/threshold")
    @ResponseBody
    public ResponseEntity<?> updateThreshold(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            logger.info("Threshold update requested by {} for record {}",
                    currentUser.getUsername(), id);

            Integer threshold = request.get("threshold");
            if (threshold == null) {
                return ResponseEntity.badRequest().body("Threshold value is required");
            }

            recordService.updateLowStockThreshold(id, threshold);
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            logger.error("Error updating threshold", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<String> handleException(Exception e) {
        logger.error("Error in admin controller", e);
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
