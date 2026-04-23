package com.my.movierecord;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.upload.dir=${java.io.tmpdir}/movierecord-test")
class MovierecordApplicationTests {

    @Test
    void contextLoads() {
    }

}
