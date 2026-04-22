package com.my.movierecord.web;

import org.springframework.data.domain.Sort;

public enum SortOption {
    LATEST("latest", "최신순", Sort.by(Sort.Direction.DESC, "watchedDate").and(Sort.by(Sort.Direction.DESC, "id"))),
    RATING("rating", "별점순", Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "id"))),
    TITLE("title", "제목순", Sort.by(Sort.Direction.ASC, "title").and(Sort.by(Sort.Direction.ASC, "id")));

    private final String code;
    private final String label;
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

    public static SortOption from(String code) {
        if (code == null) {
            return LATEST;
        }
        for (SortOption option : values()) {
            if (option.code.equalsIgnoreCase(code)) {
                return option;
            }
        }
        return LATEST;
    }
}
