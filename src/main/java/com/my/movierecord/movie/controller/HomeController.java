package com.my.movierecord.movie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 홈페이지 요청을 처리하는 컨트롤러.
 * 루트 경로("/")의 모든 요청을 /movies로 리다이렉트한다.
 */
@Controller
public class HomeController {

    /**
     * 루트 경로 접근 시 영화 목록 페이지로 리다이렉트한다.
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/movies";
    }
}
