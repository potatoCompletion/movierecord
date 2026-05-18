package com.my.movierecord.person.controller;

import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.TmdbPersonDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RequiredArgsConstructor
@Controller
public class PersonDetailController {

    private final TmdbClient tmdbClient;

    @GetMapping("/person/{id}")
    public String personDetail(@PathVariable Long id, Model model) {
        TmdbPersonDetail detail = tmdbClient.getPersonDetail(id);
        if (detail == null) {
            return "redirect:/search";
        }
        model.addAttribute("detail", detail);
        return "content/person-detail";
    }
}
