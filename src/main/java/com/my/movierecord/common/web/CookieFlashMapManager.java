package com.my.movierecord.common.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.AbstractFlashMapManager;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 리다이렉트 flash 속성을 HTTP 세션 대신 쿠키에 보관하는 {@link org.springframework.web.servlet.FlashMapManager}.
 *
 * <p>기본 {@code SessionFlashMapManager}는 {@code addFlashAttribute} 호출 시 세션을 생성해 stateless 설정에서도
 * {@code JSESSIONID}가 발급됐다. 이 구현은 flash 목록을 JSON → Base64URL 로 직렬화해 짧은 수명의 쿠키에 담고,
 * 대상 페이지가 소비하면 쿠키를 즉시 만료시킨다. 속성 값은 문자열만 지원한다(이 프로젝트의 flash 는 모두 메시지 문자열).
 */
public class CookieFlashMapManager extends AbstractFlashMapManager {

    public static final String COOKIE_NAME = "FLASH";

    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE = "Lax";
    private static final TypeReference<List<Entry>> ENTRY_LIST = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final boolean secure;

    public CookieFlashMapManager(ObjectMapper objectMapper, boolean secure) {
        this.objectMapper = objectMapper;
        this.secure = secure;
    }

    @Override
    protected List<FlashMap> retrieveFlashMaps(HttpServletRequest request) {
        String raw = readCookie(request);
        if (raw == null) {
            return null;
        }
        try {
            List<Entry> entries = objectMapper.readValue(Base64.getUrlDecoder().decode(raw), ENTRY_LIST);
            List<FlashMap> flashMaps = new ArrayList<>(entries.size());
            for (Entry entry : entries) {
                flashMaps.add(entry.toFlashMap());
            }
            return flashMaps;
        } catch (RuntimeException ex) {
            // 손상되거나 형식이 바뀐 쿠키는 flash 가 없는 것으로 취급한다. 다음 저장 시 덮어써진다.
            logger.debug("Ignoring unreadable flash cookie", ex);
            return null;
        }
    }

    @Override
    protected void updateFlashMaps(List<FlashMap> flashMaps, HttpServletRequest request, HttpServletResponse response) {
        if (flashMaps.isEmpty()) {
            writeCookie(response, "", Duration.ZERO);
            return;
        }
        List<Entry> entries = flashMaps.stream().map(Entry::from).toList();
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(entries));
        writeCookie(response, value, Duration.ofSeconds(getFlashMapTimeout()));
    }

    private static String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .sameSite(SAME_SITE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** 쿠키에 직렬화되는 flash 한 건. {@link FlashMap}의 대상 경로/파라미터/만료 시각/속성을 담는다. */
    public record Entry(String path,
                        Map<String, List<String>> params,
                        long expiresAt,
                        Map<String, String> attributes) {

        static Entry from(FlashMap flashMap) {
            Map<String, String> attributes = new LinkedHashMap<>();
            flashMap.forEach((key, value) -> attributes.put(key, String.valueOf(value)));
            return new Entry(flashMap.getTargetRequestPath(),
                    new LinkedHashMap<>(flashMap.getTargetRequestParams()),
                    flashMap.getExpirationTime(),
                    attributes);
        }

        FlashMap toFlashMap() {
            FlashMap flashMap = new FlashMap();
            flashMap.setTargetRequestPath(path);
            if (params != null) {
                flashMap.addTargetRequestParams(new LinkedMultiValueMap<>(params));
            }
            flashMap.setExpirationTime(expiresAt);
            if (attributes != null) {
                flashMap.putAll(attributes);
            }
            return flashMap;
        }
    }
}
