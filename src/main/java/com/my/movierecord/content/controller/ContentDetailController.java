package com.my.movierecord.content.controller;

import com.my.movierecord.omdb.client.OmdbClient;
import com.my.movierecord.omdb.dto.OmdbRating;
import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.repository.WatchRecordRepository;
import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.TmdbMovieDetail;
import com.my.movierecord.tmdb.dto.TmdbTvDetail;
import java.util.ArrayList;
import java.util.List;
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
    private final OmdbClient omdbClient;

    @GetMapping("/movie/{id}")
    public String movieDetail(@PathVariable Long id, Model model) {
        TmdbMovieDetail detail = tmdbClient.getMovieDetail(id);
        if (detail == null) {
            return "redirect:/search";
        }
        List<WatchRecord> records = watchRecordRepository.findByContent(id, "movie");
        double murabelAvg = records.stream().mapToDouble(r -> r.getRating().doubleValue()).average().orElse(0.0);
        List<OmdbRating> ratings = new ArrayList<>();
        if (detail.voteAverage() != null) {
            String count = detail.voteCount() != null
                    ? String.format("(%,d)", detail.voteCount())
                    : null;
            ratings.add(new OmdbRating("TMDB", String.format("%.1f", detail.voteAverage()), "rating-tmdb", "/10", count));
        }
        ratings.addAll(omdbClient.getRatings(detail.imdbId()));
        model.addAttribute("detail", detail);
        model.addAttribute("records", records);
        model.addAttribute("ratings", ratings);
        model.addAttribute("murabelAvg", murabelAvg);
        return "content/movie-detail";
    }

    @GetMapping("/tv/{id}")
    public String tvDetail(@PathVariable Long id, Model model) {
        TmdbTvDetail detail = tmdbClient.getTvDetail(id);
        if (detail == null) {
            return "redirect:/search";
        }
        String imdbId = tmdbClient.getTvExternalIds(id);
        List<WatchRecord> records = watchRecordRepository.findByContent(id, "tv");
        double murabelAvg = records.stream().mapToDouble(r -> r.getRating().doubleValue()).average().orElse(0.0);
        List<OmdbRating> ratings = new ArrayList<>();
        if (detail.voteAverage() != null) {
            String count = detail.voteCount() != null
                    ? String.format("(%,d)", detail.voteCount())
                    : null;
            ratings.add(new OmdbRating("TMDB", String.format("%.1f", detail.voteAverage()), "rating-tmdb", "/10", count));
        }
        ratings.addAll(omdbClient.getRatings(imdbId));
        model.addAttribute("detail", detail);
        model.addAttribute("records", records);
        model.addAttribute("ratings", ratings);
        model.addAttribute("murabelAvg", murabelAvg);
        return "content/tv-detail";
    }
}
