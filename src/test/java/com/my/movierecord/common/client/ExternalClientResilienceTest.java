package com.my.movierecord.common.client;

import com.my.movierecord.omdb.client.OmdbClient;
import com.my.movierecord.tmdb.client.TmdbClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TMDB/OMDB 클라이언트의 하이브리드 degradation 계약을 실제 컨텍스트에서 검증한다.
 * RestClient 빈을 항상 전송 오류(IOException → ResourceAccessException)를 내는 스텁으로
 * 오버라이드해, resilience4j 재시도 소진 후의 동작을 확인한다.
 *
 * <ul>
 *   <li>부가 호출(홈 캐러셀, OMDB 평점)은 fallback 으로 빈 결과를 반환한다.</li>
 *   <li>핵심 호출(영화 상세)은 fallback 없이 예외를 전파한다.</li>
 * </ul>
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class ExternalClientResilienceTest {

    @TestConfiguration
    static class StubClientConfig {

        @Bean
        RestClient tmdbRestClient() {
            return alwaysFailingClient("tmdb");
        }

        @Bean
        RestClient omdbRestClient() {
            return alwaysFailingClient("omdb");
        }

        private RestClient alwaysFailingClient(String api) {
            ClientHttpRequestFactory failingFactory = (uri, httpMethod) -> {
                throw new IOException("simulated " + api + " transport failure");
            };
            return RestClient.builder()
                    .baseUrl("http://localhost")
                    .requestFactory(failingFactory)
                    .defaultStatusHandler(HttpStatusCode::isError, ExternalApiErrorHandler.forApi(api))
                    .build();
        }
    }

    @Autowired
    TmdbClient tmdbClient;

    @Autowired
    OmdbClient omdbClient;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreakers() {
        circuitBreakerRegistry.circuitBreaker("tmdbApi").reset();
        circuitBreakerRegistry.circuitBreaker("omdbApi").reset();
    }

    @Test
    void 부가_TMDB_호출은_실패시_빈결과로_degrade() {
        assertThat(tmdbClient.getNowPlaying()).isEmpty();
        assertThat(tmdbClient.getUpcoming()).isEmpty();
    }

    @Test
    void 핵심_TMDB_상세호출은_실패시_예외를_전파() {
        assertThatThrownBy(() -> tmdbClient.getMovieDetail(1L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void OMDB_평점_호출은_실패시_빈결과로_degrade() {
        assertThat(omdbClient.getRatings("tt1375666")).isEmpty();
    }
}
