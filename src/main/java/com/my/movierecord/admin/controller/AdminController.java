package com.my.movierecord.admin.controller;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final SessionRegistry sessionRegistry;

    @GetMapping("/members")
    public String memberList(Model model, Principal principal) {
        model.addAttribute("users", userService.findAllUsers());
        model.addAttribute("withdrawnUsers", userService.findWithdrawnUsers());
        model.addAttribute("currentUsername", principal.getName());
        return "admin/members";
    }

    @PostMapping("/members/{id}/withdraw")
    public String withdrawUser(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User target = userService.findById(id);
        if (target.getUsername().equals(principal.getName())) {
            redirectAttributes.addFlashAttribute("errorMessage", "본인 계정은 탈퇴 처리할 수 없습니다.");
            return "redirect:/admin/members";
        }
        userService.withdrawUser(id);

        String targetUsername = target.getUsername();
        sessionRegistry.getAllPrincipals().stream()
                .filter(p -> p instanceof UserDetails ud && ud.getUsername().equals(targetUsername))
                .flatMap(p -> sessionRegistry.getAllSessions(p, false).stream())
                .forEach(SessionInformation::expireNow);

        return "redirect:/admin/members";
    }

    @PostMapping("/members/{id}/approve")
    public String approveUser(@PathVariable Long id) {
        userService.approveUser(id);
        return "redirect:/admin/members";
    }

    @PostMapping("/members/{id}/restore")
    public String restoreUser(@PathVariable Long id) {
        userService.restoreUser(id);
        return "redirect:/admin/members";
    }

    @GetMapping("/users")
    public String redirectToMembers() {
        return "redirect:/admin/members";
    }
}
