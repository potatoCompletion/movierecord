package com.my.movierecord.common.client;

import com.my.movierecord.common.exception.ExternalApiClientException;
import com.my.movierecord.common.exception.ExternalApiException;
import com.my.movierecord.common.exception.ExternalApiTransientException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ExternalApiErrorHandlerTest {

    private final RestClient.ResponseSpec.ErrorHandler handler = ExternalApiErrorHandler.forApi("tmdb");

    @Test
    void 서버_5xx_오류는_transient로_변환된다() throws Exception {
        HttpRequest request = request();
        ClientHttpResponse response = response(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThatThrownBy(() -> handler.handle(request, response))
                .isInstanceOf(ExternalApiTransientException.class)
                .satisfies(t -> {
                    assertThat(((ExternalApiException) t).getStatus()).isEqualTo(500);
                    assertThat(((ExternalApiException) t).getApiName()).isEqualTo("tmdb");
                });
    }

    @Test
    void 요청량_초과_429는_transient로_변환된다() throws Exception {
        HttpRequest request = request();
        ClientHttpResponse response = response(HttpStatus.TOO_MANY_REQUESTS);

        assertThatThrownBy(() -> handler.handle(request, response))
                .isInstanceOf(ExternalApiTransientException.class)
                .satisfies(t -> assertThat(((ExternalApiException) t).getStatus()).isEqualTo(429));
    }

    @Test
    void 클라이언트_4xx_오류는_client로_변환된다() throws Exception {
        HttpRequest request = request();
        ClientHttpResponse response = response(HttpStatus.NOT_FOUND);

        assertThatThrownBy(() -> handler.handle(request, response))
                .isInstanceOf(ExternalApiClientException.class)
                .satisfies(t -> assertThat(((ExternalApiException) t).getStatus()).isEqualTo(404));
    }

    private HttpRequest request() {
        HttpRequest request = mock(HttpRequest.class);
        given(request.getURI()).willReturn(URI.create("https://api.themoviedb.org/3/movie/1"));
        return request;
    }

    private ClientHttpResponse response(HttpStatus status) throws Exception {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        given(response.getStatusCode()).willReturn(status);
        return response;
    }
}
