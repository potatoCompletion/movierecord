package com.my.movierecord.admin.controller;

import com.my.movierecord.admin.dto.PosterPathBackfillResult;
import com.my.movierecord.admin.service.PosterPathBackfillService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기존 콘텐츠의 {@code poster_path} 백필용 관리자 엔드포인트.
 *
 * <p><b>일회성 임시 엔드포인트다.</b> TMDB 포스터 로컬 캐싱(thumbnailPath) 제거 전환 과정에서
 * poster_path 가 null 인 기존 콘텐츠를 TMDB 상세 API 로 채우기 위한 것으로, 백필 완료 후 이 컨트롤러와
 * {@link PosterPathBackfillService}, {@link PosterPathBackfillResult} 를 함께 제거한다.
 *
 * <p>인가: {@code /admin/**} 는 {@code SecurityConfig} 에서 {@code ROLE_ADMIN} 으로 보호된다.
 * 여러 번 호출해도 안전하며(이미 채워진 행은 대상에서 빠진다), 건별 집계를 JSON 으로 반환한다.
 */
@RestController
@RequestMapping("/admin/backfill")
public class PosterPathBackfillController {

    private final PosterPathBackfillService posterPathBackfillService;

    public PosterPathBackfillController(PosterPathBackfillService posterPathBackfillService) {
        this.posterPathBackfillService = posterPathBackfillService;
    }

    @PostMapping("/poster-path")
    public PosterPathBackfillResult backfillPosterPath() {
        return posterPathBackfillService.backfill();
    }
}
