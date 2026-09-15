package com.my.movierecord.common.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.FlashMap;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class CookieFlashMapManagerTest {

    private static final String COOKIE_PREFIX = CookieFlashMapManager.COOKIE_NAME + "=";

    private final CookieFlashMapManager manager = new CookieFlashMapManager(JsonMapper.builder().build(), false);

    @Test
    void 리다이렉트_시_flash를_쿠키에_저장하고_대상_페이지가_소비하면_쿠키를_만료시킨다() {
        MockHttpServletResponse redirect = new MockHttpServletResponse();
        manager.saveOutputFlashMap(flashTo("/records", "success", "감상평이 등록되었습니다."),
                new MockHttpServletRequest("POST", "/records"), redirect);

        String setCookie = redirect.getHeader("Set-Cookie");
        assertThat(setCookie).startsWith(COOKIE_PREFIX).contains("HttpOnly").contains("SameSite=Lax");
        assertThat(redirect.getHeaders("Set-Cookie")).noneMatch(h -> h.startsWith("JSESSIONID="));

        MockHttpServletRequest target = new MockHttpServletRequest("GET", "/records");
        target.setCookies(new Cookie(CookieFlashMapManager.COOKIE_NAME, cookieValue(setCookie)));
        MockHttpServletResponse page = new MockHttpServletResponse();

        FlashMap input = manager.retrieveAndUpdate(target, page);

        assertThat((Object) input).isNotNull();
        assertThat(input.get("success")).isEqualTo("감상평이 등록되었습니다.");
        assertThat(page.getHeader("Set-Cookie")).startsWith(COOKIE_PREFIX + ";").contains("Max-Age=0");
    }

    @Test
    void 대상_경로가_다른_요청은_flash를_소비하지_않고_쿠키도_유지한다() {
        MockHttpServletResponse redirect = new MockHttpServletResponse();
        manager.saveOutputFlashMap(flashTo("/records", "success", "완료"),
                new MockHttpServletRequest("POST", "/records"), redirect);

        MockHttpServletRequest other = new MockHttpServletRequest("GET", "/my-page");
        other.setCookies(new Cookie(CookieFlashMapManager.COOKIE_NAME, cookieValue(redirect.getHeader("Set-Cookie"))));
        MockHttpServletResponse page = new MockHttpServletResponse();

        assertThat((Object) manager.retrieveAndUpdate(other, page)).isNull();
        assertThat(page.getHeader("Set-Cookie")).isNull();
    }

    @Test
    void 손상된_쿠키는_flash가_없는_것으로_취급한다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/records");
        request.setCookies(new Cookie(CookieFlashMapManager.COOKIE_NAME, "not-base64-json!!"));

        assertThat((Object) manager.retrieveAndUpdate(request, new MockHttpServletResponse())).isNull();
    }

    private static FlashMap flashTo(String path, String key, String value) {
        FlashMap flashMap = new FlashMap();
        flashMap.setTargetRequestPath(path);
        flashMap.put(key, value);
        return flashMap;
    }

    private static String cookieValue(String setCookieHeader) {
        return setCookieHeader.substring(COOKIE_PREFIX.length(), setCookieHeader.indexOf(';'));
    }
}
