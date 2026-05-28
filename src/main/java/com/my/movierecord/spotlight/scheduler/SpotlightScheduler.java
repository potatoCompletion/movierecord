package com.my.movierecord.spotlight.scheduler;

import com.my.movierecord.spotlight.service.SpotlightService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 스포트라이트 캐시 사전 워밍 스케줄러.
 *
 * <p>매일 23:00에 실행되어 익일 스포트라이트를 미리 fetch·캐싱한다.
 * 자정 전 워밍으로 00:00 직후 접속 유저에게 cache miss 없이 즉시 응답한다.
 * 스케줄러가 SpotlightService 를 외부에서 호출하므로 @Cacheable 프록시가 정상 동작한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpotlightScheduler {

    private final SpotlightService spotlightService;

    /**
     * 매일 23:00:00 에 익일 스포트라이트를 사전 워밍한다.
     * 자정 전 워밍으로 00:00 직후 접속 유저에게 cache miss 없이 즉시 응답한다.
     */
    @Scheduled(cron = "0 0 23 * * *")
    public void warmDailySpotlight() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        log.info("[SpotlightScheduler] 익일({}) 스포트라이트 캐시 사전 워밍 시작", tomorrow);
        try {
            spotlightService.getSpotlights(tomorrow);
            log.info("[SpotlightScheduler] 사전 워밍 완료");
        } catch (Exception e) {
            log.error("[SpotlightScheduler] 사전 워밍 실패: {}", e.getMessage(), e);
        }
    }
}
