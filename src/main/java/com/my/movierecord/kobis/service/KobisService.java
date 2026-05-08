package com.my.movierecord.kobis.service;

import tools.jackson.databind.ObjectMapper;
import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.service.ContentService;
import com.my.movierecord.kobis.domain.DailyBoxOffice;
import com.my.movierecord.kobis.dto.BoxOfficeItemDto;
import com.my.movierecord.kobis.dto.KobisBoxOfficeResponse;
import com.my.movierecord.kobis.dto.KobisMovieItem;
import com.my.movierecord.kobis.repository.DailyBoxOfficeRepository;
import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.TmdbSearchItem;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import kr.or.kobis.kobisopenapi.consumer.rest.KobisOpenAPIRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KobisService {

    private static final DateTimeFormatter KOBIS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KobisOpenAPIRestService kobisOpenAPIRestService;
    private final DailyBoxOfficeRepository dailyBoxOfficeRepository;
    private final TmdbClient tmdbClient;
    private final ContentService contentService;
    private final ObjectMapper objectMapper;

    public List<BoxOfficeItemDto> getDailyBoxOffice() {
        LocalDate targetDt = LocalDate.now().minusDays(1);
        List<DailyBoxOffice> cached = dailyBoxOfficeRepository.findByTargetDtOrderByRankAsc(targetDt);
        if (!cached.isEmpty()) {
            return cached.stream().map(BoxOfficeItemDto::fromEntity).toList();
        }
        return fetchAndSave(targetDt);
    }

    private List<BoxOfficeItemDto> fetchAndSave(LocalDate targetDt) {
        try {
            String dateStr = targetDt.format(KOBIS_DATE);
            String json = kobisOpenAPIRestService
                    .getDailyBoxOffice(true, dateStr, "10", "", "", "");

            KobisBoxOfficeResponse response = objectMapper.readValue(json, KobisBoxOfficeResponse.class);
            if (response.boxOfficeResult() == null
                    || response.boxOfficeResult().dailyBoxOfficeList() == null) {
                return List.of();
            }

            List<DailyBoxOffice> entities = new ArrayList<>();
            for (KobisMovieItem item : response.boxOfficeResult().dailyBoxOfficeList()) {
                if (item.openDt() == null || item.openDt().isBlank()) {
                    continue;
                }
                int rank = Integer.parseInt(item.rank());
                Long audiAcc = parseAudiAcc(item.audiAcc());
                String primaryReleaseYear = item.openDt().split("-")[0];
                Content content = findContent(item.movieNm(), primaryReleaseYear);
                LocalDate releaseDate = LocalDate.parse(item.openDt());
                entities.add(DailyBoxOffice.of(targetDt, rank, item.movieNm(), audiAcc, content, releaseDate));
            }

            return dailyBoxOfficeRepository.saveAll(entities).stream()
                    .map(BoxOfficeItemDto::fromEntity)
                    .toList();
        } catch (Exception e) {
            log.warn("KOBIS box office fetch failed for {}: {}", targetDt, e.getMessage());
            return List.of();
        }
    }

    private Content findContent(String movieNm, String primaryReleaseYear) {
        try {
            List<TmdbSearchItem> results = tmdbClient.searchMovie(movieNm, primaryReleaseYear);
            if (results.isEmpty() || results.getFirst().id() == null) {
                return null;
            }

            List<TmdbSearchItem> validResults = results.stream()
                    .filter(r -> r.title() != null)
                    .toList();
            if (validResults.isEmpty()) {
                return null;
            }

            String replacedMovieNm = movieNm.replace(" ", "");
            int bestScoreIndex = -1;
            int bestScore = Integer.MIN_VALUE;

            for (int i = 0; i < validResults.size(); i++) {
                TmdbSearchItem item = validResults.get(i);

                String replacedTitle = item.title().replace(" ", "");
                int score = getTitleEqualScore(replacedTitle, replacedMovieNm);

                if (score > bestScore) {
                    bestScore = score;
                    bestScoreIndex = i;
                }
            }

            TmdbSearchItem foundItem = validResults.get(bestScoreIndex);
            return contentService.findOrCreate(foundItem.id(), "movie", foundItem.posterPath());
        } catch (Exception e) {
            log.warn("TMDB search failed for '{}': {}", movieNm, e.getMessage());
            return null;
        }
    }

    private int getTitleEqualScore(String title, String movieNm) {
        int minLength = Math.min(title.length(), movieNm.length());

        int matched = 0;
        int mismatched = 0;

        for (int i = 0; i < minLength; i++) {
            if (title.charAt(i) == movieNm.charAt(i)) {
                matched++;
            } else {
                mismatched++;
            }
        }

        mismatched += Math.abs(title.length() - movieNm.length());

        return matched -  mismatched;
    }

    private Long parseAudiAcc(String audiAcc) {
        if (audiAcc == null || audiAcc.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(audiAcc.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
