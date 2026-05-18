package com.my.movierecord.content.controller;

import com.my.movierecord.record.repository.WatchRecordRepository;
import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.TmdbMovieDetail;
import com.my.movierecord.tmdb.dto.TmdbTvDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RequiredArgsConstructor
@Controller
public class ContentDetailController {

    private final TmdbClient tmdbClient;
    private final WatchRecordRepository watchRecordRepository;

    @GetMapping("/movie/{id}")
    public String movieDetail(@PathVariable Long id, Model model) {
        TmdbMovieDetail detail = tmdbClient.getMovieDetail(id);
        if (detail == null) {
            return "redirect:/search";
        }
        model.addAttribute("detail", detail);
        model.addAttribute("records", watchRecordRepository.findByContent(id, "movie"));
        return "content/movie-detail";
    }

    @GetMapping("/tv/{id}")
    public String tvDetail(@PathVariable Long id, Model model) {
        TmdbTvDetail detail = tmdbClient.getTvDetail(id);
        if (detail == null) {
            return "redirect:/search";
        }
        model.addAttribute("detail", detail);
        model.addAttribute("records", watchRecordRepository.findByContent(id, "tv"));
        return "content/tv-detail";
    }
}
