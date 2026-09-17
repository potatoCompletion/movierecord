package com.my.movierecord.tmdb.dto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TMDB {@code /movie/{id}/release_dates} 응답에서 한국 극장 개봉일을 고르는 순수 함수.
 *
 * <p>discover의 {@code release_date}는 최초(전세계) 개봉일이라 한국 재개봉작이면 과거 날짜가 온다.
 * KR 목록에는 원 개봉(과거)과 재개봉(미래) type 3 항목이 함께 오므로, "가장 이른 날짜"가 아니라
 * "오늘 이후 가장 이른 날짜"를 골라야 한다.
 */
public final class TmdbReleaseDates {

    private static final String KOREA = "KR";
    private static final int TYPE_THEATRICAL = 3;
    private static final int TYPE_THEATRICAL_LIMITED = 2;
    private static final int ISO_DATE_LENGTH = 10;

    private TmdbReleaseDates() {
    }

    /**
     * KR 항목 중 type 3(Theatrical)에서 {@code today} 이후(당일 포함) 가장 이른 날짜를 고르고,
     * 없으면 type 2(Theatrical limited)에 같은 규칙을 적용한다. 둘 다 없으면 empty.
     */
    @SuppressWarnings("unchecked")
    public static Optional<LocalDate> koreanTheatricalDate(Map<String, Object> body, LocalDate today) {
        if (body == null || !(body.get("results") instanceof List<?> results)) {
            return Optional.empty();
        }
        List<Map<String, Object>> koreanDates = results.stream()
                .filter(Map.class::isInstance)
                .map(r -> (Map<String, Object>) r)
                .filter(r -> KOREA.equals(r.get("iso_3166_1")))
                .findFirst()
                .map(r -> r.get("release_dates") instanceof List<?> l ? (List<Map<String, Object>>) l : List.<Map<String, Object>>of())
                .orElse(List.of());

        return earliestOnOrAfter(koreanDates, TYPE_THEATRICAL, today)
                .or(() -> earliestOnOrAfter(koreanDates, TYPE_THEATRICAL_LIMITED, today));
    }

    private static Optional<LocalDate> earliestOnOrAfter(List<Map<String, Object>> dates, int type, LocalDate today) {
        return dates.stream()
                .filter(d -> d.get("type") instanceof Number n && n.intValue() == type)
                .map(d -> parseDate(d.get("release_date")))
                .flatMap(Optional::stream)
                .filter(date -> !date.isBefore(today))
                .min(LocalDate::compareTo);
    }

    /** {@code "2026-10-01T00:00:00.000Z"} 형식에서 앞 10자만 날짜로 읽는다. 형식이 다르면 empty. */
    private static Optional<LocalDate> parseDate(Object raw) {
        if (!(raw instanceof String s) || s.length() < ISO_DATE_LENGTH) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(s.substring(0, ISO_DATE_LENGTH)));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
