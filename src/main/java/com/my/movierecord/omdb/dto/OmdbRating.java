package com.my.movierecord.omdb.dto;

public record OmdbRating(String source, String value, String cssClass) {

    public String emoji() {
        if (!"IMDb".equals(source)) return null;
        try {
            double score = Double.parseDouble(value.split("/")[0].trim());
            if (score >= 7.0) return "😊";
            if (score >= 5.0) return "🤔";
            return "😢";
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
