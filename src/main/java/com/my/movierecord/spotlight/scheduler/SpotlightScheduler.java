package com.my.movierecord.spotlight.scheduler;

import com.my.movierecord.spotlight.service.SpotlightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 스포트라이트 캐시 사전 워밍 스케줄러.
 *
 * <p>매일 자정(00:00)에 실행되어 당일 스포트라이트를 미리 fetch·캐싱한다.
 * 스케줄러가 SpotlightService 를 외부에서 호출하므로 @Cacheable 프록시가 정상 동작한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpotlightScheduler {

    private final SpotlightService spotlightService;

    /**
     * 매일 00:00:00 에 스포트라이트를 사전 워밍한다.
     * key 가 LocalDate 기반이므로 자정 이후 첫 호출에서 cache miss → 신규 픽.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void warmDailySpotlight() {
        log.info("[SpotlightScheduler] 자정 스포트라이트 캐시 사전 워밍 시작");
        try {
            spotlightService.getTodaySpotlights();
            log.info("[SpotlightScheduler] 사전 워밍 완료");
        } catch (Exception e) {
            log.error("[SpotlightScheduler] 사전 워밍 실패: {}", e.getMessage(), e);
        }
    }
}
