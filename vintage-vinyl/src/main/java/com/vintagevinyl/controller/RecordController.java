package com.vintagevinyl.controller;

import com.vintagevinyl.model.Record;
import com.vintagevinyl.service.RecordService;
import com.vintagevinyl.service.UserService;
import com.vintagevinyl.service.WishlistService;
import com.vintagevinyl.service.CSVImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import com.vintagevinyl.model.User;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/records")
public class RecordController {

    private static final Logger logger = LoggerFactory.getLogger(RecordController.class);

    @Autowired
    private RecordService recordService;

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private UserService userService;

    @Autowired
    private CSVImportService csvImportService;

    @GetMapping
    public String listRecords(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "") String search,
                              Model model) {
        Page<Record> recordPage = recordService.getAllRecords(PageRequest.of(page - 1, 10), search);
        model.addAttribute("records", recordPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", recordPage.getTotalPages());
        model.addAttribute("search", search);
        return "records";
    }

    @GetMapping("/{id}")
    public String viewRecord(@PathVariable Long id, Model model) {
        Record record = recordService.getRecordById(id);
        if (record == null) {
            return "redirect:/records?error=Record+not+found";
        }
        model.addAttribute("record", record);
        model.addAttribute("inStock", record.getStock() > 0);
        model.addAttribute("stockStatus", record.getStock() == 0 ? "Out of Stock" :
                (record.isLowStock() ? "Low Stock" : "In Stock"));
        return "record-detail";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/new")
    public String newRecordForm(Model model) {
        Record record = new Record();
        record.setLowStockThreshold(5); // Set default low stock threshold
        model.addAttribute("record", record);
        return "record-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public String saveRecord(@Valid @ModelAttribute Record record,
                             @RequestParam("releaseYear") Integer releaseYear,
                             @RequestParam(value = "initialStock", defaultValue = "0") Integer initialStock,
                             BindingResult result) {
        if (result.hasErrors()) {
            return "record-form";
        }
        record.setReleaseDate(LocalDate.of(releaseYear, 1, 1));
        record.setStock(initialStock); // Set initial stock
        recordService.saveRecord(record);
        return "redirect:/records";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String editRecordForm(@PathVariable Long id, Model model) {
        Record record = recordService.getRecordById(id);
        if (record == null) {
            throw new RecordNotFoundException("Record not found with id: " + id);
        }
        model.addAttribute("record", record);
        model.addAttribute("currentStock", record.getStock());
        model.addAttribute("lowStockThreshold", record.getLowStockThreshold());
        return "record-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}")
    public String updateRecord(@PathVariable Long id,
                               @Valid @ModelAttribute Record record,
                               @RequestParam("releaseYear") Integer releaseYear,
                               BindingResult result) {
        if (result.hasErrors()) {
            return "record-form";
        }

        // Retrieve existing record's stock information
        Record existingRecord = recordService.getRecordById(id);
        record.setId(id);
        record.setStock(existingRecord.getStock()); // Keep original stock
        record.setLowStockThreshold(existingRecord.getLowStockThreshold()); // Keep original threshold
        record.setReleaseDate(LocalDate.of(releaseYear, 1, 1));

        recordService.updateRecord(record);
        return "redirect:/records";
    }

    @GetMapping("/{id}/stock-status")
    @ResponseBody
    public String checkStockStatus(@PathVariable Long id) {
        Record record = recordService.getRecordById(id);
        if (record.getStock() == 0) {
            return "Out of Stock";
        } else if (record.isLowStock()) {
            return "Low Stock";
        } else {
            return "In Stock";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteRecord(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return "redirect:/records";
    }

    @PostMapping("/{id}/addToWishlist")
    public String addToWishlist(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            Record record = recordService.getRecordById(id);
            wishlistService.addToWishlist(user, record);
            redirectAttributes.addFlashAttribute("message", "Record added to wishlist successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add record to wishlist: " + e.getMessage());
        }
        return "redirect:/records/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/import")
    public String showImportForm() {
        return "record-import";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import")
    public String handleImport(@RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/records/import";
        }

        try {
            int importedCount = csvImportService.importCSVData(file.getInputStream());
            redirectAttributes.addFlashAttribute("message",
                    "Successfully imported " + importedCount + " records. Please check and update stock levels.");
        } catch (IOException e) {
            logger.error("Failed to import CSV file", e);
            redirectAttributes.addFlashAttribute("error", "Failed to import file: " + e.getMessage());
        }

        return "redirect:/records/import";
    }

    @ExceptionHandler(RecordNotFoundException.class)
    public String handleRecordNotFound(RecordNotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
}

class RecordNotFoundException extends RuntimeException {
    public RecordNotFoundException(String message) {
        super(message);
    }
}
