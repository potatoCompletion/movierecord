package com.my.movierecord.admin.dto;

import java.util.List;

/**
 * poster_path 백필 집계 결과. 호출 직후 결과를 검증할 수 있도록 건수와 실패 대상을 함께 반환한다.
 *
 * @param processed 조회된 대상 중 실제로 순회한 건수 ({@code succeeded + skipped + failed}).
 *                  서킷브레이커 open 이나 인터럽트로 중단되면 조회 건수보다 작을 수 있다
 * @param succeeded TMDB 에서 poster_path 를 받아 저장한 건수
 * @param skipped   루프 안 방어적 재확인에서 이미 채워져 있던 건수 (조회 쿼리가 걸러내므로 통상 0)
 * @param failed    TMDB 404·응답에 poster_path 없음·기타 오류로 채우지 못한 건수
 * @param failedIds 실패한 콘텐츠 식별자 ({@code "mediaType:tmdbId"} 형식. 복합 키라 tmdbId 만으로는 구분되지 않는다)
 */
public record PosterPathBackfillResult(
        int processed,
        int succeeded,
        int skipped,
        int failed,
        List<String> failedIds
) {
}
