package com.my.movierecord.tmdb.controller;

import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.TmdbSearchItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tmdb")
public class TmdbController {

    private final TmdbClient tmdbClient;

    @GetMapping("/search")
    public List<TmdbSearchItem> search(@RequestParam String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return tmdbClient.search(query);
    }
}
