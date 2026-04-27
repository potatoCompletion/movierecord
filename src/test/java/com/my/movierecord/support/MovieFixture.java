package com.my.movierecord.support;

import com.my.movierecord.movie.domain.Movie;
import com.my.movierecord.movie.dto.MovieForm;
import com.my.movierecord.movie.enums.Emotion;
import com.my.movierecord.movie.enums.Immersion;
import com.my.movierecord.movie.enums.Story;
import com.my.movierecord.movie.enums.Taste;
import com.my.movierecord.movie.service.MovieSaveCommand;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

public class MovieFixture {

    public static Movie createMovie() {
        return Movie.builder()
                .title("테스트 영화")
                .watchedDate(LocalDate.of(2024, 6, 1))
                .thumbnailPath(null)
                .oneLiner("재미있었다")
                .immersion(Immersion.GOOD)
                .story(Story.CONVINCING)
                .emotion(Emotion.FUNNY)
                .goodPoints("연출이 좋았다")
                .badPoints("결말이 아쉬웠다")
                .taste(Taste.MATCH)
                .rating(new BigDecimal("4.5"))
                .build();
    }

    public static Movie createMovieWithId(Long id) {
        Movie movie = createMovie();
        setField(movie, "id", id);
        return movie;
    }

    public static Movie createMovieWithThumbnail(Long id, String thumbnailPath) {
        Movie movie = Movie.builder()
                .title("테스트 영화")
                .watchedDate(LocalDate.of(2024, 6, 1))
                .thumbnailPath(thumbnailPath)
                .oneLiner("재미있었다")
                .immersion(Immersion.GOOD)
                .story(Story.CONVINCING)
                .emotion(Emotion.FUNNY)
                .goodPoints("연출이 좋았다")
                .badPoints("결말이 아쉬웠다")
                .taste(Taste.MATCH)
                .rating(new BigDecimal("4.5"))
                .build();
        setField(movie, "id", id);
        return movie;
    }

    public static MovieSaveCommand createCommand() {
        return new MovieSaveCommand(
                "테스트 영화",
                LocalDate.of(2024, 6, 1),
                null,
                "재미있었다",
                Immersion.GOOD,
                Story.CONVINCING,
                Emotion.FUNNY,
                "연출이 좋았다",
                "결말이 아쉬웠다",
                Taste.MATCH,
                new BigDecimal("4.5"),
                1L
        );
    }

    public static MovieSaveCommand createCommandWithThumbnail(String thumbnailPath) {
        return new MovieSaveCommand(
                "테스트 영화",
                LocalDate.of(2024, 6, 1),
                thumbnailPath,
                "재미있었다",
                Immersion.GOOD,
                Story.CONVINCING,
                Emotion.FUNNY,
                "연출이 좋았다",
                "결말이 아쉬웠다",
                Taste.MATCH,
                new BigDecimal("4.5"),
                1L
        );
    }

    public static MovieForm validForm() {
        MovieForm form = new MovieForm();
        form.setTitle("테스트 영화");
        form.setWatchedDate(LocalDate.of(2024, 6, 1));
        form.setOneLiner("재미있었다");
        form.setImmersion(Immersion.GOOD);
        form.setStory(Story.CONVINCING);
        form.setEmotion(Emotion.FUNNY);
        form.setGoodPoints("연출이 좋았다");
        form.setBadPoints("결말이 아쉬웠다");
        form.setTaste(Taste.MATCH);
        form.setRating(new BigDecimal("4.5"));
        return form;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
