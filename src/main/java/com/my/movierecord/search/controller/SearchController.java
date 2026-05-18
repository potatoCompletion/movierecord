package com.my.movierecord.search.controller;

import com.my.movierecord.tmdb.client.TmdbClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller
public class SearchController {

    private final TmdbClient tmdbClient;

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("query", q != null ? q : "");
        if (q != null && !q.isBlank()) {
            model.addAttribute("results", tmdbClient.searchMultiUnified(q));
        } else {
            model.addAttribute("results", List.of());
        }
        return "search/results";
    }
}
