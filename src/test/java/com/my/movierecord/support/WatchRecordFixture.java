package com.my.movierecord.support;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.content.domain.Content;
import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.dto.RecordForm;
import com.my.movierecord.record.enums.Emotion;
import com.my.movierecord.record.enums.Immersion;
import com.my.movierecord.record.enums.Story;
import com.my.movierecord.record.enums.Taste;
import com.my.movierecord.record.service.WatchRecordSaveCommand;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public class WatchRecordFixture {

    public static User createUser() {
        User user = User.builder()
                .username("user")
                .password("password")
                .name("테스트유저")
                .build();
        setField(user, "id", 1L);
        return user;
    }

    public static WatchRecord createWatchRecord() {
        return WatchRecord.builder()
                .title("테스트 영화")
                .watchedDate(LocalDate.of(2024, 6, 1))
                .oneLiner("재미있었다")
                .immersion(Immersion.GOOD)
                .story(Story.CONVINCING)
                .emotions(Set.of(Emotion.FUNNY))
                .goodPoints("연출이 좋았다")
                .badPoints("결말이 아쉬웠다")
                .taste(Taste.MATCH)
                .rating(new BigDecimal("4.5"))
                .user(createUser())
                .build();
    }

    public static WatchRecord createWatchRecordWithId(Long id) {
        WatchRecord record = createWatchRecord();
        setField(record, "id", id);
        return record;
    }

    public static WatchRecord createWatchRecordWithContent(Long id, String thumbnailPath) {
        Content content = Content.of(12345L, "movie");
        content.updateThumbnailPath(thumbnailPath);
        WatchRecord record = WatchRecord.builder()
                .title("테스트 영화")
                .watchedDate(LocalDate.of(2024, 6, 1))
                .oneLiner("재미있었다")
                .immersion(Immersion.GOOD)
                .story(Story.CONVINCING)
                .emotions(Set.of(Emotion.FUNNY))
                .goodPoints("연출이 좋았다")
                .badPoints("결말이 아쉬웠다")
                .taste(Taste.MATCH)
                .rating(new BigDecimal("4.5"))
                .content(content)
                .user(createUser())
                .build();
        setField(record, "id", id);
        return record;
    }

    public static WatchRecordSaveCommand createCommand() {
        return new WatchRecordSaveCommand(
                "테스트 영화",
                LocalDate.of(2024, 6, 1),
                null, null, null,
                "재미있었다",
                Immersion.GOOD,
                Story.CONVINCING,
                Set.of(Emotion.FUNNY),
                "연출이 좋았다",
                "결말이 아쉬웠다",
                Taste.MATCH,
                new BigDecimal("4.5"),
                1L
        );
    }

    public static RecordForm validForm() {
        RecordForm form = new RecordForm();
        form.setTitle("테스트 영화");
        form.setWatchedDate(LocalDate.of(2024, 6, 1));
        form.setOneLiner("재미있었다");
        form.setImmersion(Immersion.GOOD);
        form.setStory(Story.CONVINCING);
        form.setEmotions(new java.util.HashSet<>(Set.of(Emotion.FUNNY)));
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
