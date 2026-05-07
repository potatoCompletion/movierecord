package com.my.movierecord.kobis.controller;

import com.my.movierecord.kobis.config.KobisProperties;
import kr.or.kobis.kobisopenapi.consumer.rest.KobisOpenAPIRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
@RestController
public class KobisController {

    private final KobisProperties kobisProperties;

    @GetMapping("/test/kobis")
    public String test() throws Exception {
        KobisOpenAPIRestService service = new KobisOpenAPIRestService(kobisProperties.key());
        String result = service.getDailyBoxOffice(true, "20260506", "10", "", "", "");

        return result;
    }

}
