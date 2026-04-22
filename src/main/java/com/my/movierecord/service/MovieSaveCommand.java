package com.my.movierecord.service;

import com.my.movierecord.domain.Emotion;
import com.my.movierecord.domain.Immersion;
import com.my.movierecord.domain.Story;
import com.my.movierecord.domain.Taste;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovieSaveCommand(
        String title,
        LocalDate watchedDate,
        String thumbnailPath,
        String oneLiner,
        Immersion immersion,
        Story story,
        Emotion emotion,
        String goodPoints,
        String badPoints,
        Taste taste,
        BigDecimal rating
) {}
