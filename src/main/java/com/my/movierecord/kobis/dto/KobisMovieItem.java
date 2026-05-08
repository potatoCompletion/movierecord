package com.my.movierecord.kobis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KobisMovieItem(
    @JsonProperty("rank") String rank,
    @JsonProperty("movieNm") String movieNm,
    @JsonProperty("audiAcc") String audiAcc,
    @JsonProperty("openDt") String openDt
) {}
