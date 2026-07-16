package com.my.movierecord.kobis.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import tools.jackson.databind.ObjectMapper;
import com.my.movierecord.kobis.client.KobisClient;
import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.service.ContentService;
import com.my.movierecord.kobis.dto.BoxOfficeItemDto;
import com.my.movierecord.kobis.dto.KobisBoxOfficeResponse;
import com.my.movierecord.tmdb.client.TmdbClient;
import com.my.movierecord.tmdb.dto.TmdbSearchItem;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KobisService {

    private static final DateTimeFormatter KOBIS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KobisClient kobisClient;
    private final TmdbClient tmdbClient;
    private final ContentService contentService;
    private final ObjectMapper objectMapper;

    @Cacheable(value = "dailyBoxOffice", key = "T(java.time.LocalDate).now().minusDays(1).toString()")
    public List<BoxOfficeItemDto> getDailyBoxOffice() {
        LocalDate targetDt = LocalDate.now().minusDays(1);

        return fetchDailyBoxOffice(targetDt);
    }

    private List<BoxOfficeItemDto> fetchDailyBoxOffice(LocalDate targetDt) {
        try {
            String dateStr = targetDt.format(KOBIS_DATE);
            String json = kobisClient.fetchDailyBoxOffice(dateStr);
            if (json == null) {
                return List.of();
            }

            KobisBoxOfficeResponse response = objectMapper.readValue(json, KobisBoxOfficeResponse.class);
            if (response.boxOfficeResult() == null
                    || response.boxOfficeResult().dailyBoxOfficeList() == null) {
                return List.of();
            }

            return response.boxOfficeResult()
                    .dailyBoxOfficeList()
                    .stream()
                    .filter(item -> StringUtils.isNotBlank(item.openDt()))
                    .map(item -> {
                        String year = item.openDt().split("-")[0];
                        Content content = findContent(item.movieNm(), year);
                        return BoxOfficeItemDto.of(item, content);
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("KOBIS box office fetch failed for {}: {}", targetDt, e.getMessage());
            return List.of();
        }
    }

    private Content findContent(String movieNm, String year) {
        try {
            List<TmdbSearchItem> results = tmdbClient.searchMovie(movieNm, year);
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

}
