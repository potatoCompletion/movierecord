package com.my.movierecord.kobis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KobisBoxOfficeResult(
    @JsonProperty("dailyBoxOfficeList") List<KobisMovieItem> dailyBoxOfficeList
) {}
