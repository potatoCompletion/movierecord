package com.my.movierecord.common.controller;

import com.my.movierecord.kobis.service.KobisService;
import com.my.movierecord.record.dto.RecentRecordItem;
import com.my.movierecord.record.dto.TopRatingItem;
import com.my.movierecord.record.repository.WatchRecordRepository;
import com.my.movierecord.spotlight.service.SpotlightService;
import com.my.movierecord.tmdb.dto.UpcomingCard;
import com.my.movierecord.tmdb.image.TmdbImageUrlProvider;
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
    private final TmdbImageUrlProvider images;

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
        // D-Day 는 캐시 저장 시점이 아니라 요청 시점 기준으로 계산하고, 개봉일이 지난 항목은 제외한다.
        model.addAttribute("upcoming",   UpcomingCard.fromAll(tmdbHomeService.getUpcoming(), LocalDate.now()));

        // ── DB: 이번 주 인기 평점 TOP 5 / 최근 감상평 4건 ─────────────────────
        LocalDateTime startDateTime = LocalDateTime.now().minusDays(7);
        model.addAttribute("topRatings", watchRecordRepository.findTopRated(startDateTime, PageRequest.of(0, 3)).stream()
                .map(p -> TopRatingItem.from(p, images))
                .toList());
        model.addAttribute("recentReviews", watchRecordRepository.findTop4ByOrderByCreatedAtDesc().stream()
                .map(wr -> RecentRecordItem.from(wr, images))
                .toList());

        return "home";
    }
}
