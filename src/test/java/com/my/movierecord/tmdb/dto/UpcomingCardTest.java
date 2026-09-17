package com.my.movierecord.tmdb.dto;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpcomingCardTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    private static UpcomingItem item(long id, String releaseDate, boolean reRelease) {
        return new UpcomingItem(id, "제목" + id, "Title" + id, "/p.jpg", "https://img/p.jpg", releaseDate, reRelease);
    }

    @Test
    void 개봉_당일은_ddays_0() {
        List<UpcomingCard> cards = UpcomingCard.fromAll(List.of(item(1L, "2026-09-17", false)), TODAY);

        assertThat(cards).singleElement().satisfies(card -> {
            assertThat(card.ddays()).isZero();
            assertThat(card.releaseDateText()).isEqualTo("09.17");
        });
    }

    @Test
    void 미래_개봉일은_양수_ddays() {
        List<UpcomingCard> cards = UpcomingCard.fromAll(List.of(item(1L, "2026-10-01", true)), TODAY);

        assertThat(cards).singleElement().satisfies(card -> {
            assertThat(card.ddays()).isEqualTo(14);
            assertThat(card.releaseDateText()).isEqualTo("10.01");
            assertThat(card.reRelease()).isTrue();
        });
    }

    @Test
    void 개봉일이_지난_항목은_제외하고_순서를_유지한다() {
        List<UpcomingCard> cards = UpcomingCard.fromAll(List.of(
                item(1L, "2026-09-16", false),
                item(2L, "2026-09-20", false),
                item(3L, "2026-09-18", false)), TODAY);

        assertThat(cards).extracting(UpcomingCard::id).containsExactly(2L, 3L);
        assertThat(cards).allSatisfy(card -> assertThat(card.ddays()).isNotNegative());
    }
}
