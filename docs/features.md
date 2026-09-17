# 주요 기능

> [← README](../README.md)

## 스포트라이트

매일 TMDB discover 결과에서 무작위로 영화를 고른 뒤 OMDb API로 IMDb·Rotten Tomatoes·Metacritic 점수를 검증해 품질 기준을 통과한 영화 3편을 홈 히어로 섹션에 표시합니다. 기준에 못 미치면 다시 픽하며 최대 10회 재시도하고, 모두 실패하면 전날 기록을 폴백으로 사용합니다. 매일 23:00에 스케줄러가 익일 캐시를 사전 워밍하고, 첫 번째 픽은 DB(`spotlight_history`)에 이력을 기록합니다.

> 품질 기준: IMDb > 7.5 **또는** Rotten Tomatoes > 60% **또는** Metacritic > 75

![홈 화면](images/home.png)

## 박스오피스 TOP 10

KOBIS 오픈API로 전날 일별 TOP 10을 조회하고, TMDB 영화 검색 API로 포스터 이미지를 매칭해 Swiper 캐러셀 카드로 표시합니다. 섹션 제목은 데이터 성격에 맞춰 "전일 기준"으로 표기합니다.

![박스오피스](images/boxoffice.png)

## 곧 개봉해요

TMDB discover로 3주 안에 한국 극장 개봉하는 영화를 골라 한국 개봉일 기준 D-Day를 표시합니다. 재개봉작은 "재개봉" 뱃지로 구분합니다. 개봉일 확정 방식은 [external-api.md](external-api.md#3-tmdb-곧-개봉해요--한국-개봉일-기준-d-day)에 있습니다.

## 통합 검색

TMDB 멀티 검색 엔드포인트로 영화·TV·인물을 한 번에 검색합니다. 결과 카드에 미디어 타입 배지를 표시하고, 클릭하면 해당 상세 페이지로 이동합니다.

## 작품·인물 상세 페이지

영화(`/movie/{id}`), TV(`/tv/{id}`), 인물(`/person/{id}`) 각각의 상세 페이지를 제공합니다. 작품 페이지에서는 TMDB 기본 정보 위에 OMDb에서 가져온 IMDb·RT·Metacritic 배지를 함께 표시하고, 사용자들이 남긴 murabel 평균 별점도 확인할 수 있습니다.

## 감상 기록

TMDB 멀티 검색(영화·TV)으로 작품을 선택한 뒤 별점, 한줄평, 몰입감, 스토리, 감정, 취향 일치도를 기록합니다. 기록 목록과 상세 화면에서 작성한 내용을 확인할 수 있습니다.

![감상 기록 작성](images/record-form.png)
![감상 기록 목록](images/record-list.png)

## 마이페이지 통계

총 기록 수, 연간·월간 기록 수, 평균 별점, 취향 일치율을 집계합니다. 최근 12개월 월별 기록 수 그래프와 감정 분포 차트를 함께 제공합니다.

![마이페이지](images/mypage.png)

## 인증

폼 로그인과 OAuth2 소셜 로그인(Google, Naver)을 지원합니다. 어느 경로로 로그인해도 JWT 액세스 토큰과 리프레시 토큰을 httpOnly 쿠키로 발급하며, 서버는 세션을 만들지 않습니다. 신규 가입 계정은 관리자 승인(PENDING → ACTIVE) 후 서비스를 이용할 수 있습니다. 설계는 [auth.md](auth.md)에 있습니다.

![로그인](images/login.png)

## 관리자 회원 관리

가입 대기(PENDING) 회원 승인, 활성 회원 강제 탈퇴, 탈퇴 회원 복구 기능을 제공합니다. 강제 탈퇴 시 해당 사용자의 리프레시 토큰을 모두 폐기해 토큰 재발급을 차단합니다.

![관리자](images/admin.png)
