package com.my.movierecord.movie.dto;

import org.springframework.data.domain.Sort;

/**
 * 영화 목록 정렬 옵션 enum.
 * 각 정렬 옵션은 URL 쿼리 파라미터(code)와 UI 표시 라벨, Spring Data의 Sort 객체를 가진다.
 * MovieController의 list() 메서드에서 ?sort=latest|rating|title 파라미터를 처리할 때 사용된다.
 */
public enum SortOption {
    // 최신순: watchedDate 내림차순, id 내림차순 (tiebreaker)
    LATEST("latest", "최신순", Sort.by(Sort.Direction.DESC, "watchedDate").and(Sort.by(Sort.Direction.DESC, "id"))),

    // 별점순: rating 내림차순, id 내림차순 (tiebreaker)
    RATING("rating", "별점순", Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "id"))),

    // 제목순: title 오름차순, id 오름차순 (tiebreaker)
    TITLE("title", "제목순", Sort.by(Sort.Direction.ASC, "title").and(Sort.by(Sort.Direction.ASC, "id")));

    // URL 쿼리 파라미터로 사용되는 코드 값
    private final String code;

    // UI에 표시될 한글 라벨
    private final String label;

    // Spring Data JPA에 전달될 Sort 객체
    private final Sort sort;

    SortOption(String code, String label, Sort sort) {
        this.code = code;
        this.label = label;
        this.sort = sort;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public Sort getSort() {
        return sort;
    }

    /**
     * 문자열 코드로부터 SortOption enum을 조회한다.
     * - null이거나 일치하는 옵션이 없으면 기본값(LATEST)을 반환
     * - 대소문자 구분 없음
     */
    public static SortOption from(String code) {
        if (code == null) {
            return LATEST;
        }
        for (SortOption option : values()) {
            if (option.code.equalsIgnoreCase(code)) {
                return option;
            }
        }
        return LATEST;  // 잘못된 코드일 때도 기본값 반환
    }
}
