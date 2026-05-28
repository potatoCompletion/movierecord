package com.my.movierecord.common.controller;

import com.my.movierecord.kobis.service.KobisService;
import com.my.movierecord.record.repository.WatchRecordRepository;
import com.my.movierecord.spotlight.service.SpotlightService;
import com.my.movierecord.tmdb.service.TmdbHomeService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("M월 d일");

    private final KobisService kobisService;
    private final TmdbHomeService tmdbHomeService;
    private final SpotlightService spotlightService;
    private final WatchRecordRepository watchRecordRepository;

    @GetMapping("/")
    public String index(Model model) {
        // ── 오늘의 스포트라이트 ───────────────────────────────────────
        model.addAttribute("spotlights", spotlightService.getSpotlights(LocalDate.now()));

        // ── 박스오피스 ────────────────────────────────────────────────
        LocalDate boxOfficeBaseDay = LocalDate.now().minusDays(1);
        model.addAttribute("boxOfficeBaseDayText", boxOfficeBaseDay.format(FORMATTER));
        model.addAttribute("boxOffice", kobisService.getDailyBoxOffice());

        // ── TMDB: 현재 상영작 / 곧 개봉해요 ───────────────────────────
        model.addAttribute("nowPlaying", tmdbHomeService.getNowPlaying());
        model.addAttribute("upcoming",   tmdbHomeService.getUpcoming());

        // ── DB: 이번 주 인기 평점 TOP 5 / 최근 감상평 4건 ─────────────────────
        LocalDateTime startDateTime = LocalDateTime.now().minusDays(7);
        model.addAttribute("topRatings",    watchRecordRepository.findTopRated(startDateTime, PageRequest.of(0, 3)));
        model.addAttribute("recentReviews", watchRecordRepository.findTop4ByOrderByCreatedAtDesc());

        return "home";
    }
}
