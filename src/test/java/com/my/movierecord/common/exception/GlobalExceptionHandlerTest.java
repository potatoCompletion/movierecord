package com.my.movierecord.common.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * GlobalExceptionHandler 의 도메인/외부 API 예외 → HTTP 상태·뷰 매핑을 검증한다.
 * standalone MockMvc + setControllerAdvice 로 보안 필터의 개입 없이 어드바이스만 격리 검증한다.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(objectMapper))
                .build();
    }

    @Test
    void 엔티티_미조회는_404_뷰() throws Exception {
        mockMvc.perform(get("/boom/entity-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void 외부API_클라이언트오류는_404_뷰() throws Exception {
        mockMvc.perform(get("/boom/api-client"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void 잘못된_인자는_400() throws Exception {
        mockMvc.perform(get("/boom/illegal-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/500"));
    }

    @Test
    void 접근거부는_403() throws Exception {
        mockMvc.perform(get("/boom/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/500"));
    }

    @Test
    void 외부API_일시장애는_503_뷰() throws Exception {
        mockMvc.perform(get("/boom/api-transient"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error/503"));
    }

    @Test
    void 전송오류_타임아웃은_503_뷰() throws Exception {
        mockMvc.perform(get("/boom/resource-access"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error/503"));
    }

    @Test
    void REST_요청은_JSON_에러_바디로_응답() throws Exception {
        mockMvc.perform(get("/boom/api-transient").accept(APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/boom/api-transient"));
    }

    @Controller
    static class ThrowingController {

        @GetMapping("/boom/entity-not-found")
        String entityNotFound() {
            throw new EntityNotFoundException("not found");
        }

        @GetMapping("/boom/api-client")
        String apiClient() {
            throw new ExternalApiClientException("tmdb", 404, "bad request");
        }

        @GetMapping("/boom/illegal-arg")
        String illegalArg() {
            throw new IllegalArgumentException("bad arg");
        }

        @GetMapping("/boom/access-denied")
        String accessDenied() {
            throw new AccessDeniedException("denied");
        }

        @GetMapping("/boom/api-transient")
        String apiTransient() {
            throw new ExternalApiTransientException("tmdb", 503, "service down");
        }

        @GetMapping("/boom/resource-access")
        String resourceAccess() {
            throw new ResourceAccessException("read timeout");
        }
    }
}
