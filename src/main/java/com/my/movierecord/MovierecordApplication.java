package com.my.movierecord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MovierecordApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovierecordApplication.class, args);
    }

}
