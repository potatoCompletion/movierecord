package com.my.movierecord.kobis.client;

import io.github.resilience4j.retry.RetryRegistry;
import kr.or.kobis.kobisopenapi.consumer.rest.KobisOpenAPIRestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * resilience4j 애노테이션이 실제로 동작하는지 검증한다. (재시도 횟수 + 최종 fallback)
 * KOBIS 벤더 호출은 mock으로 대체해 transient 실패/성공 시나리오를 재현한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class KobisClientResilienceTest {

    @MockitoBean
    KobisOpenAPIRestService kobisOpenAPIRestService;

    @Autowired
    KobisClient kobisClient;

    @Autowired
    RetryRegistry retryRegistry;

    @Test
    void transient_실패는_max_attempts만큼_재시도한_뒤_null로_degrade() throws Exception {
        given(kobisOpenAPIRestService.getDailyBoxOffice(anyBoolean(), any(), any(), any(), any(), any()))
                .willThrow(new RuntimeException("KOBIS down"));

        String result = kobisClient.fetchDailyBoxOffice("20260715");

        int maxAttempts = retryRegistry.retry("kobisApi").getRetryConfig().getMaxAttempts();
        assertThat(maxAttempts).isEqualTo(3);
        assertThat(result).isNull();
        verify(kobisOpenAPIRestService, times(maxAttempts))
                .getDailyBoxOffice(anyBoolean(), any(), any(), any(), any(), any());
    }

    @Test
    void 성공하면_재시도_없이_본문을_반환() throws Exception {
        String json = "{\"boxOfficeResult\":null}";
        given(kobisOpenAPIRestService.getDailyBoxOffice(anyBoolean(), any(), any(), any(), any(), any()))
                .willReturn(json);

        String result = kobisClient.fetchDailyBoxOffice("20260715");

        assertThat(result).isEqualTo(json);
        verify(kobisOpenAPIRestService, times(1))
                .getDailyBoxOffice(anyBoolean(), any(), any(), any(), any(), any());
    }
}
