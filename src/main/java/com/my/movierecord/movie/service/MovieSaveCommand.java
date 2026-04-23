package com.my.movierecord.movie.service;

import com.my.movierecord.movie.enums.Emotion;
import com.my.movierecord.movie.enums.Immersion;
import com.my.movierecord.movie.enums.Story;
import com.my.movierecord.movie.enums.Taste;
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
