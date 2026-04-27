package com.my.movierecord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 영화 기록 애플리케이션의 메인 진입점.
 * Spring Boot 애플리케이션을 초기화하고 실행한다.
 */
@SpringBootApplication
public class MovierecordApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovierecordApplication.class, args);
    }

}
