package com.my.movierecord.admin.controller;

import com.my.movierecord.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/members")
    public String memberList(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin/members";
    }

    @GetMapping("/users")
    public String pendingUsers(Model model) {
        model.addAttribute("pendingUsers", userService.findPendingUsers());
        return "admin/users";
    }

    @PostMapping("/users/{id}/approve")
    public String approveUser(@PathVariable Long id) {
        userService.approveUser(id);
        return "redirect:/admin/users";
    }
}
