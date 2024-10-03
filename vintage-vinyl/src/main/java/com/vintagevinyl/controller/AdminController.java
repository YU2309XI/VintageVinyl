package com.vintagevinyl.controller;

import com.vintagevinyl.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

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
}