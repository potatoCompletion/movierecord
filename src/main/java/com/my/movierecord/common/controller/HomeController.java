package com.my.movierecord.common.controller;

import com.my.movierecord.kobis.service.KobisService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final KobisService kobisService;

    public HomeController(KobisService kobisService) {
        this.kobisService = kobisService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("boxOffice", kobisService.getDailyBoxOffice());
        return "home";
    }
}
