package com.my.movierecord.kobis.service;

import tools.jackson.databind.ObjectMapper;
import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.service.ContentService;
import com.my.movierecord.kobis.config.KobisProperties;
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

    private final KobisProperties kobisProperties;
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
            String json = new KobisOpenAPIRestService(kobisProperties.key())
                    .getDailyBoxOffice(true, dateStr, "10", "", "", "");

            KobisBoxOfficeResponse response = objectMapper.readValue(json, KobisBoxOfficeResponse.class);
            if (response.boxOfficeResult() == null
                    || response.boxOfficeResult().dailyBoxOfficeList() == null) {
                return List.of();
            }

            List<DailyBoxOffice> entities = new ArrayList<>();
            for (KobisMovieItem item : response.boxOfficeResult().dailyBoxOfficeList()) {
                int rank = Integer.parseInt(item.rank());
                Long audiAcc = parseAudiAcc(item.audiAcc());
                Content content = findContent(item.movieNm());
                entities.add(DailyBoxOffice.of(targetDt, rank, item.movieNm(), audiAcc, content));
            }

            return dailyBoxOfficeRepository.saveAll(entities).stream()
                    .map(BoxOfficeItemDto::fromEntity)
                    .toList();
        } catch (Exception e) {
            log.warn("KOBIS box office fetch failed for {}: {}", targetDt, e.getMessage());
            return List.of();
        }
    }

    private Content findContent(String movieNm) {
        try {
            List<TmdbSearchItem> results = tmdbClient.search(movieNm);
            if (results.isEmpty() || results.getFirst().id() == null) {
                return null;
            }
            TmdbSearchItem first = results.getFirst();
            return contentService.findOrCreate(first.id(), first.mediaType(), first.posterPath());
        } catch (Exception e) {
            log.warn("TMDB search failed for '{}': {}", movieNm, e.getMessage());
            return null;
        }
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
