package com.my.movierecord.record.service;

import com.my.movierecord.record.enums.Emotion;
import com.my.movierecord.record.enums.Immersion;
import com.my.movierecord.record.enums.Story;
import com.my.movierecord.record.enums.Taste;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record WatchRecordSaveCommand(
        String title,
        LocalDate watchedDate,
        Long tmdbId,
        String mediaType,
        String posterPath,
        String oneLiner,
        Immersion immersion,
        Story story,
        Set<Emotion> emotions,
        String goodPoints,
        String badPoints,
        Taste taste,
        BigDecimal rating,
        Long userId
) {}
