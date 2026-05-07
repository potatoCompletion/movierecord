package com.my.movierecord.kobis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KobisBoxOfficeResponse(
    @JsonProperty("boxOfficeResult") KobisBoxOfficeResult boxOfficeResult
) {}
