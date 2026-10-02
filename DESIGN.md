# DESIGN.md — Murabel UI 디자인 현황과 시스템 제안

`src/main/resources/templates` 와 `src/main/resources/static` 을 2026-10-02 기준으로 정적 분석한 문서다.
수치(빈도)는 `static/css/*.css` 의 선언을 집계한 값이며, 토큰 정의 파일 `colors_and_type.css` 자체는 빈도에서 제외했다.

---

## 0. 기술 제약 (반드시 지킬 것)

| 항목 | 제약 |
|---|---|
| 렌더링 | **Spring Boot SSR + Thymeleaf.** 모든 화면은 서버에서 렌더링한다. SPA 프레임워크(React/Vue 등)와 빌드 파이프라인(npm, bundler)을 도입하지 않는다. |
| 스크립트 | **vanilla JS** 만 사용한다. `static/js/*.js` 를 `<script th:src>` 로 페이지별로 로드한다. 외부 라이브러리는 WebJars(현재 Swiper) 로만 들여온다. |
| 컴포넌트 | **컴포넌트는 `th:fragment` 로 구현한다.** 재사용 UI는 `templates/fragments/` 에 fragment 로 만들고 `th:replace="~{fragments/파일 :: 이름(파라미터)}"` 로 삽입한다. |
| 스타일 | 순수 CSS. 디자인 토큰은 `static/css/colors_and_type.css` 의 CSS custom property 가 단일 출처이며, `style.css` 가 이를 `@import` 한다. 페이지 전용 CSS 는 `head(title, extraHead)` fragment 의 `extraHead` 로 주입한다. |
| 테마 | 다크 단일 테마 (`html { color-scheme: dark; }`). 라이트 모드 없음. |
| 폰트 | Pretendard Variable (jsDelivr CDN `@import`). |
| 이미지 | 포스터/배경은 TMDB CDN URL 을 서버가 조립해 내려준다. 로컬 저장 없음. 실패 시 `/images/poster-fallback.svg`. |

### CSS 로딩 구조

```
fragments/layout.html :: head
  └─ style.css ──@import── colors_and_type.css ──@import── Pretendard(CDN)
  └─ swiper-bundle.min.css (webjars)
  └─ extraHead (페이지별)
       home.html ................ home.css + home.js
       records/list, detail ..... records.css
       records/form ............. records.css + form.js + tmdb-search.js
       my-page/my-page .......... my-page.css + records.css
       content/*-detail ......... content.css
       search/results ........... search.css
       auth/login, signup ....... auth.css
       admin/members ............ admin.css
       error/404,500,503 ........ (style.css 만)
```

---

## 1. 색상

### 1-1. 토큰 정의 (`colors_and_type.css`) 와 사용 빈도

`var(--토큰)` 참조 횟수 기준. 테마는 **딥 네이비-퍼플 배경 + 크림 텍스트 + 골드(마키 조명) 포인트**.

#### 배경 / 표면

| 토큰 | 값 | 사용 | 주요 위치 |
|---|---|---|---|
| `--bg` | `#0c0b18` | 6 | `body`, `.home-hero`, `.tab-bar` |
| `--bg-alt` | `#100f1c` | 5 | `.site-header`(덮어써짐), 검색 드롭다운, admin |
| `--surface` | `#141320` | 17 | 카드·패널 공통 표면 (records, home, my-page) |
| `--surface-2` | `#181626` | 1 | home.css 1곳 |
| `--elevated` | `#1e1c2e` | 7 | records.css 전용 (칩, 드롭다운, 모달 내부) |
| `--surface-alpha` | `rgba(20,19,32,.85)` | **0** | 미사용 |
| `--overlay` | `rgba(6,5,20,.82)` | **0** | 미사용 — 실제 오버레이는 리터럴 사용(아래) |

그라디언트 패널(`#1c1a2e→#131220`, `#1a1828→#131220`)은 `--grad-panel`, `--grad-panel-soft` 로 정의돼 있지만 **사용 0회** — `.modal`, `.form`, `.auth-container` 가 같은 값을 리터럴로 다시 쓰고 있다.

#### 테두리

| 토큰 | 값 | 사용 |
|---|---|---|
| `--border` | `rgba(240,235,224,.09)` | 32 |
| `--border-strong` | `rgba(240,235,224,.18)` | 5 |
| `--border-gold` | `rgba(232,184,75,.20)` | 19 |
| `--border-gold-mid` | `rgba(232,184,75,.55)` | 11 |

#### 텍스트

| 토큰 | 값 | 사용 | 용도 |
|---|---|---|---|
| `--fg` | `#f0ebe0` | 42 | 본문·제목 |
| `--fg-muted` | `rgba(240,235,224,.62)` | 43 | 보조 텍스트, 라벨, secondary 버튼 |
| `--fg-dim` | `rgba(240,235,224,.40)` | 12 | 메타, 힌트 |
| `--fg-faint` | `rgba(240,235,224,.20)` | 12 | placeholder, 카운터, 빈 별 |

#### 포인트 (골드)

| 토큰 | 값 | 사용 |
|---|---|---|
| `--accent` | `#e8b84b` | **58** (가장 많이 쓰이는 토큰) |
| `--accent-hover` | `#f2c85c` | 2 |
| `--accent-dark` | `#c4901f` | 9 (그라디언트 끝점) |
| `--accent-bg` | `rgba(232,184,75,.06)` | 12 (hover wash) |
| `--accent-bg-strong` | `rgba(232,184,75,.15)` | 4 |
| `--accent-glow` | `rgba(232,184,75,.25)` | 1 |

골드 위 텍스트는 토큰 없이 `#0e0b1a` 리터럴 6회 (`.btn-primary`, `::selection`, `.pagination .active`, 선택된 `.chip`, hero 버튼).

#### 상태색

| 토큰 | 값 | 사용 | 비고 |
|---|---|---|---|
| `--success` | `#6ee7a0` | 3 | flash(기본), `.field-success` |
| `--success-bg` / `--success-border` | `.08` / `.25` 알파 | 1 / 1 | `.flash-message` |
| `--danger` / `--danger-hover` | `#e05252` / `#c94040` | 1 / 1 | `.btn-danger` |
| `--error` | `#ff7575` | 2 | `.field-error` |
| `--error-soft` | `#ff8585` | **0** | 대신 `#ff8585` 리터럴 2회 (`.flash-message.error`, `.field-hint.field-error`) |

> 에러 계열이 `#e05252` / `#ff7575` / `#ff8585` / `rgba(255,100,100,…)` 4가지로 갈라져 있다.

#### 도메인 색 (감정 / 취향)

| 토큰 | 값 | 사용 위치 |
|---|---|---|
| `--emo-funny` | `#f0c674` | `.chip[data-emo]`, `.card-emotion-badge`, 마이페이지 도넛 차트 |
| `--emo-tense` | `#d97757` | 〃 |
| `--emo-sad` | `#7aa9d4` | 〃 |
| `--emo-linger` | `#b08fc7` | 〃 |
| `--emo-scary` | `#e05252` | 〃 (`--danger` 와 같은 값) |
| `--emo-none` | `#8a8578` | 〃 |
| `--taste-yes` / `--taste-no` | `#6ee7a0` / `#ff8585` | **0** — 미사용 |

감정 색은 CSS 3회씩 + 템플릿 인라인 `background:var(--emo-${code})` 로 동적 참조된다(§6 참고). records.css 의 선택 칩은 감정별 어두운 글자색(`#1a1003`, `#1a0805`, `#06121e`, `#150a1c`, `#1a0404`, `#0c0c0a`) 과 glow 를 리터럴로 갖는다.

### 1-2. 토큰 밖 하드코딩 색상 (분류)

리터럴 색상은 CSS 에 약 **200회** 등장한다. 파일별로는 home.css > records.css > content.css > style.css 순.

| 분류 | 값 | 빈도 | 위치 | 비고 |
|---|---|---|---|---|
| 배경 (토큰 중복) | `#0c0b18` | 4 | home.css `.poster-art`, `.home-hero`, `.hero-fade` | = `--bg` |
| 텍스트 (토큰 중복) | `#f0ebe0` | 5 | home.css `.poster-art`, `.hero-title`, `.up-date` | = `--fg` |
| 포인트 (토큰 중복) | `#e8b84b` | 10 | home.css hero 전반, `.poster-art` | = `--accent`; home.css 는 토큰 대신 리터럴을 주로 씀 |
| 포인트 (토큰 중복) | `#c4901f` | 2 | home.css hero 버튼, 1위 메달 | = `--accent-dark` |
| 텍스트 알파 변종 | `rgba(240,235,224, .07/.14/.2/.35/.55/.6/.7/.78/.82)` | 14 | home.css, records.css `.stars-wrap` | 토큰(.62/.40/.20)과 어긋난 중간값 |
| 골드 알파 변종 | `rgba(232,184,75, .04~.85)` 15종 | 약 40 | 전 파일 | 토큰은 .06/.15/.20/.25/.55 뿐 |
| 표면 (토큰 밖) | `#0e0b1a`, `#1c1a2e`, `#1a1828`, `#131220`, `#15131f`, `#221f33`, `#1f1a2a`, `#2a2540` | 각 1~7 | 패널 그라디언트, `.poster-art` | |
| 반투명 표면 | `rgba(255,255,255, .04/.05/.06/.07/.08/.1/.11/.12/.18)` | 17 | 입력창, 검색 드롭다운, 플레이스홀더, `.record-card` | 크림(`240,235,224`)이 아닌 순백 알파 — 두 체계 혼재 |
| 오버레이 | `rgba(6,5,20,.78)`, `rgba(0,0,0,.45)`, `rgba(10,9,20,.6~.92)`, `rgba(12,11,24,.78/.85)`, `rgba(16,15,28,.92)`, `rgba(13,12,26,.96)` | 10 | 모달, 드로어, 상세 hero, 포스터 위 배지, 헤더/탭바 | `--overlay` 미사용 |
| 그림자 | `rgba(0,0,0, .22~.75)` | 18 | box-shadow 전반 | |
| 외부 브랜드 | `#f5c518`/`#F5C518`/`#e6b800`(IMDb), `rgba(1,180,228,…)`(TMDB), RT fresh/rotten, Metacritic green/yellow/red | 15 | content.css `.rating-badge-external.*`, `.rating-star`, `.record-card-rating` | 별점 노랑 `#f5c518` 이 골드 `--accent` 와 별개로 쓰임 |
| 소셜 로그인 | `#fff`/`#3c4043`(Google), `#03C75A`(Naver), Google 로고 4색 | 6 | auth.css, login.html 인라인 SVG | 브랜드 가이드 준수 대상, 토큰화 제외 |
| 메달 | gold `#e8c84b`, silver `#dcdcdc`/`#9e9e9e`, bronze `#d4854a`/`#9c5520` + 알파 | 15 | home.css `.ratings-panel .rating-row:nth-child(1~3)` | |
| 기타 포인트 | `#7fd3c8` (재개봉 D-day 민트) | 3 | home.css `.up-dday--rerelease` | 유일한 청록 계열 |
| 리뷰 카드 순환색 | `#d8c577`, `#a8c3e8`, `#c8a8e8` + `.19/.33` 알파 | 9 | home.html `th:style` (인라인) | §6 |
| hero 톤 기본값 | `#1f2a1a`, `#3a3520`, `#e8b84b` | 각 3 | home.css, home.html 인라인, home.js 폴백 | 세 군데 중복 |

---

## 2. 타이포그래피

### 2-1. 패밀리

| 토큰 | 값 | 사용 |
|---|---|---|
| `--font-display` | `'Pretendard Variable', Pretendard, 'Noto Sans KR', sans-serif` | 35 (제목·숫자·라벨) |
| `--font-body` | 동일 스택 | 7 (`body` 등) |
| `--font-mono` | `ui-monospace, …` | **0** |
| `inherit` | — | 7 (버튼·입력) |

display 와 body 가 **같은 폰트**다. 위계는 크기·굵기·자간으로만 만든다. 숫자 정렬은 `font-variant-numeric: tabular-nums` 를 국소적으로 쓴다.

### 2-2. 크기

`font-size` 선언 173개 중 **113개(65%) 가 토큰**, 60개가 리터럴.

| 토큰 | rem | px | 사용 | 용도 (토큰 주석 기준) |
|---|---|---|---|---|
| `--fs-hero` | 1.85 | 29.6 | 2 | 페이지 H1 |
| `--fs-h2` | 1.55 | 24.8 | 2 | auth 제목 |
| `--fs-h3` | 1.20 | 19.2 | 4 | 모달/섹션 제목 |
| `--fs-sub` | 1.10 | 17.6 | 6 | 하위 섹션 제목 |
| `--fs-lg` | 0.95 | 15.2 | 11 | 강조 본문, 입력값 |
| `--fs-body` | 0.93 | 14.9 | **0** | 본문 (미사용) |
| `--fs-md` | 0.90 | 14.4 | 10 | 컴팩트 본문 |
| `--fs-sm` | 0.88 | 14.1 | 15 | 버튼, 정렬 select |
| `--fs-caption` | 0.85 | 13.6 | 15 | 보조 캡션 |
| `--fs-xs` | 0.83 | 13.3 | 3 | 부제, 알림 |
| `--fs-fine` | 0.80 | 12.8 | 9 | 작은 글씨, `.btn-sm` |
| `--fs-meta` | 0.78 | 12.5 | 9 | 메타, 글자 수 |
| `--fs-2xs` | 0.75 | 12.0 | 10 | 마이크로 라벨 |
| `--fs-label` | 0.72 | 11.5 | 10 | 대문자 폼 라벨 |
| `--fs-3xs` | 0.70 | 11.2 | 7 | 최소 가독 크기 |

리터럴 크기(60개): `1rem`(8), `0.74rem`(5), `0.65rem`(4), `0.92rem`(4), `0.68rem`(4), `1.05rem`(3), `1.5rem`(2), `0.76/0.82/0.84rem`(각 2), 그 외 1회씩 `0.55, 0.62, 0.86, 1.15, 1.25, 1.3, 1.35, 1.4, 1.6, 1.75, 2, 2.1, 2.2, 2.4, 3.4, 5.5rem`, `16px`, `1.0em`, `clamp()` 5종(records 제목, 에러 코드 `clamp(5rem,…,8rem)`).

> 0.70~0.95rem 사이에 토큰이 **10단계**(0.02~0.05rem 간격) 있어 시각적으로 구분되지 않는 단계가 많고, 1.2rem 이상의 대형 단계는 3개뿐이라 home/records 의 큰 제목은 전부 리터럴로 처리됐다.

### 2-3. 굵기

| 값 | 사용 | 주 용도 |
|---|---|---|
| 600 | 41 | 버튼, 라벨, 카드 제목 |
| 700 | 32 | 페이지/섹션 제목, 숫자(별점, 랭크) |
| 500 | 13 | 메타, 링크 |
| 400 | 3 | home/content 본문 리셋 |
| 800 | 2 | auth 로고, home hero |

### 2-4. 행간 / 자간

- `line-height`: `1`(12, 숫자·아이콘), `1.5`(4, body 기본), `1.6`(3, textarea/본문), `1.3`(3), 그 외 `1.02~1.7` 9종. 토큰 `--lh-tight`(1.2)/`--lh-base`(1.5) 는 **미사용**.
- `letter-spacing`: 대문자 라벨 `0.08em`(7)·`0.09em`(2)·`0.18em`(3), 제목 `-0.02em`(5)·`-0.015em`(2), 그 외 13종. 토큰 3개 중 `--tracking-loose` 만 1회 사용.

### 2-5. 제안 타이포 스케일

기존 사용 빈도 상위값에 맞춰 단계를 줄인다(토큰 이름은 유지 가능한 것은 유지).

| 역할 | 제안 토큰 | 값 | 굵기 / 행간 / 자간 | 흡수 대상 |
|---|---|---|---|---|
| Display | `--fs-display` | `clamp(2rem, 1.4rem + 2vw, 2.8rem)` | 700 / 1.1 / -0.02em | hero·records 제목 clamp, 2~2.4rem |
| H1 | `--fs-hero` | 1.85rem | 700 / 1.2 / -0.02em | 1.75, 2.1rem |
| H2 | `--fs-h2` | 1.5rem | 700 / 1.2 / -0.015em | 1.4, 1.55, 1.6rem |
| H3 | `--fs-h3` | 1.2rem | 600 / 1.3 | 1.15, 1.25, 1.3rem |
| Sub | `--fs-sub` | 1.05rem | 600 / 1.3 | 1, 1.05, 1.1rem |
| Body | `--fs-body` | 0.93rem | 400–500 / 1.6 | `--fs-lg`, `--fs-md`, 0.92rem |
| Control | `--fs-sm` | 0.88rem | 600 / 1 | 버튼, select, 0.84/0.86rem |
| Caption | `--fs-caption` | 0.82rem | 500 / 1.5 | `--fs-xs`, `--fs-fine`, 0.82rem |
| Meta | `--fs-meta` | 0.76rem | 500 / 1.4 | `--fs-2xs`, 0.74/0.76rem |
| Label | `--fs-label` | 0.70rem | 600 / 1 / 0.09em, uppercase | `--fs-3xs`, 0.68rem |
| Micro | `--fs-micro` | 0.62rem | 700 / 1 / 0.12em | 0.55~0.65rem (배지) |

굵기는 **500 / 600 / 700** 세 단계로 고정하고 800 은 로고 전용으로 둔다.

---

## 3. 간격

### 3-1. 현재 분포

`margin`/`padding` 선언 235개, `gap` 선언 108개. 0 과 auto 를 제외한 값을 px 로 환산한 분포:

| px | margin/padding 값 (빈도) | gap 값 (빈도) | 합 |
|---|---|---|---|
| 2 | `2px`(4) `0.15rem`(2) `0.1rem`(1) | `2px`(2) `0.15rem`(2) `0.1rem`(1) | 12 |
| 3–5 | `4px`(11) `0.25rem`(4) `0.2rem`(4) `3px`(2) `0.3rem`(3) `5px`(1) | `4px`(6) `0.25rem`(3) `0.3rem`(3) `5px`(1) | 38 |
| 6–7 | `0.4rem`(10) `6px`(5) `0.35rem`(3) `0.45rem`(1) `7px`(1) | `0.4rem`(8) `6px`(5) `0.35rem`(2) `0.45rem`(1) | 36 |
| 8–10 | `0.5rem`(14) `0.6rem`(9) `8px`(5) `0.55rem`(4) `10px`(6) `9px`(1) | `0.5rem`(12) `0.6rem`(11) `8px`(2) `10px`(2) | 66 |
| 11–14 | `0.75rem`(10) `0.85rem`(12) `0.7rem`(3) `0.8rem`(3) `0.65rem`(3) `12px`(2) `14px`(5) `0.9rem`(4) | `0.75rem`(9) `0.85rem`(2) `0.65rem`(1) `12px`(1) `14px`(2) | 58 |
| 16–18 | `1rem`(24) `16px`(2) `1.1rem`(4) `1.15rem`(1) | `1rem`(6) `16px`(1) `18px`(2) | 40 |
| 19–22 | `1.25rem`(14) `1.2rem`(3) `1.3rem`(1) `20px`(1) `22px`(1) `1.4rem`(6) | `1.25rem`(5) `1.4rem`(1) | 32 |
| 24–28 | `1.5rem`(19) `1.75rem`(8) `26px`(2) `28px`(1) | `1.5rem`(4) `28px`(1) | 35 |
| 32–40 | `2rem`(18) `2.2rem`(1) `2.25rem`(1) `2.4rem`(1) `2.5rem`(7) | `2rem`(4) `2.5rem`(2) | 34 |
| 48+ | `3rem`(4) `3.5rem`(1) `4rem`(5) `5rem`(4) | `3rem`(2) | 16 |

- 서로 다른 값 **약 60종**, rem 과 px 혼용.
- `--space-1` ~ `--space-16` 토큰 10개가 정의돼 있지만 **사용 0회**.
- 실제 리듬은 `0.5 / 0.75 / 1 / 1.25 / 1.5 / 2rem` 근처에 몰려 있고, 그 사이를 `0.4, 0.6, 0.85rem` 같은 중간값이 채운다.

### 3-2. 제안 간격 스케일 (4px 기반, 기존 `--space-*` 재사용)

| 토큰 | 값 | 흡수 대상 |
|---|---|---|
| `--space-0-5` (신규) | 2px | 1–3px, 0.1–0.2rem |
| `--space-1` | 4px (0.25rem) | 4–5px, 0.3rem |
| `--space-1-5` (신규) | 6px (0.375rem) | 0.35–0.45rem, 6–7px — 칩·배지 내부 gap 에 빈도 높음 |
| `--space-2` | 8px (0.5rem) | 0.5–0.55rem, 8–9px |
| `--space-2-5` (신규) | 10px (0.625rem) | 0.6–0.65rem, 10px — 버튼 세로 패딩, 폼 gap |
| `--space-3` | 12px (0.75rem) | 0.7–0.8rem, 12px |
| `--space-3-5` (신규) | 14px (0.875rem) | 0.85–0.9rem, 14px — 카드 내부 패딩 |
| `--space-4` | 16px (1rem) | 1–1.15rem, 16–18px |
| `--space-5` | 20px (1.25rem) | 1.2–1.3rem, 20–22px |
| `--space-6` | 24px (1.5rem) | 1.4–1.5rem — 컨테이너 좌우 패딩 |
| `--space-7` (신규) | 28px (1.75rem) | 1.75rem, 26–28px |
| `--space-8` | 32px (2rem) | 2–2.25rem — 섹션 간격, main 상단 |
| `--space-10` | 40px (2.5rem) | 2.4–2.5rem |
| `--space-12` | 48px (3rem) | 3rem |
| `--space-16` | 64px (4rem) | 3.5–4rem |
| `--space-20` (신규) | 80px (5rem) | 5rem — `main.container` 하단 |

적용 원칙: 컴포넌트 **내부**는 2–14px, 컴포넌트 **사이**는 16–32px, **섹션 사이**는 40px 이상.

---

## 4. 모서리 · 그림자

### 4-1. border-radius (선언 78개, 토큰 사용 28개)

| 값 | 사용 | 위치 |
|---|---|---|
| `var(--r-sm)` = 8px | 11 | 버튼, 입력, flash, 모달 내부 |
| `8px` 리터럴 | 11 | home.css 포스터/카드(8), search.css(2) — **토큰과 같은 값을 리터럴로** |
| `var(--r-md)` = 12px | 8 (+`--radius-md` 1) | 카드, 통계 카드 |
| `999px` / `9999px` / `var(--r-pill)` | 7 / 2 / 3 | 칩, 배지, 검색창 |
| `10px` | 5 | content.css 전용 (상세 포스터, 외부 평점 배지, 레코드 카드) |
| `var(--r-lg)` = 18px | 4 (+ 상단만 1) | auth 카드, 모달, 폼 패널 |
| `6px` | 4 | 작은 썸네일, 배지 |
| `4px` / `3px` / `2px` | 4 / 3 / 3 | 포스터 아트, 별 슬롯, 막대 그래프 |
| `50%` | 4 | 아바타, 점 |
| `12px` / `14px` / `16px` | 3 / 1 / 1 | home.css, auth.css |
| 비대칭 | 3 | `6px 6px 2px 2px`(차트 막대), `0 0 3px 3px`, `r-lg r-lg 0 0`(모바일 바텀시트 모달) |

제안: **`4px`(xs) / `8px`(sm) / `12px`(md) / `18px`(lg) / `999px`(pill) / `50%`(circle)** 6단계. `6px`·`10px`·`14px`·`16px` 은 인접 단계로 흡수.

### 4-2. box-shadow (선언 27개, `--shadow-*` 토큰 사용 **0개**)

| 의도 | 실제 값 | 위치 | 대응 토큰 (미사용) |
|---|---|---|---|
| 골드 CTA | `0 2px 12px rgba(232,184,75,.25)` → hover `0 4px 22px …,.42` | `.btn-primary`, hero 버튼 | `--shadow-cta`, `--shadow-cta-hover` (hover 값 20px vs 22px 로 미세하게 다름) |
| 골드 CTA (소) | `0 2px 10px rgba(232,184,75,.3)` | `.pagination .active`, 선택 `.chip` | — |
| 카드 hover | `0 8px 28px rgba(0,0,0,.55)`, `0 12px 32px …,.5`, `0 14px 38px …,.55` | `.movie-card`, `.tmdb-dropdown` | `--shadow-card` 의 일부 |
| 드롭다운/패널 | `0 8px 32px rgba(0,0,0,.5/.6)` | 헤더 검색 드롭다운, 상세 포스터 | — |
| 모달 | `0 0 0 1px rgba(0,0,0,.5), 0 30px 80px rgba(0,0,0,.7)` | `.modal` | `--shadow-modal` 에서 골드 glow 만 빠진 값 |
| auth 카드 | `0 0 0 1px …,.45, 0 28px 72px …,.65, 0 0 90px rgba(232,184,75,.05)` | `.auth-container` | `--shadow-auth` 와 **완전히 동일한 값을 리터럴로** |
| 포커스 링 | `0 0 0 3px rgba(232,184,75,.12)` | `.form-control:focus` | — (토큰 없음) |
| 감정 칩 glow | `0 2px 12px rgba(<감정색>,.32)` ×5 | records.css 선택 칩 | — |
| 메달 glow | `0 3px 12–14px …` ×3 | home.css 랭크 1~3위 | — |
| 기타 | `0 0 0 18px var(--up-accent-22)` 등 | home.css | — |

`text-shadow` 5개: 골드 glow(`0 0 14~60px rgba(232,184,75,…)`) 4개 + 포스터 아트 그림자 1개.

제안: 그림자를 **elevation 3단계 + 기능 2종**으로 정리하고 기존 토큰을 실제로 참조한다.
`--shadow-1`(드롭다운, `0 8px 28px rgba(0,0,0,.5)`) / `--shadow-2`(카드 hover, `--shadow-card`) / `--shadow-3`(모달·auth, `--shadow-modal`) / `--shadow-cta(-hover)` / `--focus-ring`(`0 0 0 3px rgba(232,184,75,.12)`, 신규).

### 4-3. z-index (참고)

`1, 1, 2, 2, 30(헤더), 40, 45, 50, 60, 100, 200` — 토큰 없음. 헤더(30) · 탭바 · 드로어 · 모달 순서를 `--z-*` 로 고정할 것을 권장.

---

## 5. 반복 UI 요소와 위치

현재 `th:fragment` 는 **2개 파일, 5개**뿐이다: `layout :: head(title, extraHead)`, `layout :: header`, `layout :: footer`, `layout :: flash`, `record-modal :: detailModal`. 나머지 반복 요소는 템플릿마다 마크업이 복사돼 있다.

| UI 요소 | 클래스 | 마크업 위치 (템플릿) | 스타일 위치 | 현재 fragment 여부 |
|---|---|---|---|---|
| 헤더 (로고·검색·사용자) | `.site-header`, `.header-search*`, `.header-avatar` | `fragments/layout.html :: header` | style.css:47, 487~ | ✅ `header` |
| 사이드 드로어 | `.side-drawer`, `.drawer-link` | `fragments/layout.html :: header` | style.css:650~ | ✅ (`header` 에 포함) |
| 모바일 탭바 | `.tab-bar`, `.tab-link` | `fragments/layout.html :: header` | style.css:120~ | ✅ (`header` 에 포함) |
| 푸터 (TMDB 고지) | `.site-footer`, `.tmdb-attribution` | `fragments/layout.html :: footer` | style.css:210~ | ✅ `footer` |
| Flash 메시지 | `.flash-message(.error)` | `layout :: flash`, 그리고 **login.html·my-page.html 에 직접 작성** | style.css:382 | 부분 |
| 기록 상세 모달 | `.modal-overlay`, `.modal*` | `fragments/record-modal.html :: detailModal` (list, my-page) | records.css:528~ | ✅ `detailModal` |
| 버튼 | `.btn` + `-primary/-secondary/-ghost/-danger/-sm/-block` | 15개 템플릿 | style.css:237~ | ❌ (클래스 기반) |
| 소셜 로그인 버튼 | `.btn-social.btn-google/.btn-naver` | auth/login.html | auth.css:116~ | ❌ |
| hero 버튼 | `.btn-hero-primary`, `.btn-hero-learn` | home.html | home.css:403~ | ❌ — `.btn-primary` 와 별도 구현 |
| 폼 필드 | `.form-group` + `label` + `.form-control` + `.field-error`/`.field-hint` | auth/login, auth/signup, my-page, records/form | style.css:300~, records.css:848~ | ❌ |
| 폼 필드 (변형) | `.form-field` | records/form.html | style.css:300~ | ❌ — `.form-group` 과 두 체계 공존 |
| 감정 칩 (선택형) | `.chip-group` > `input` + `.chip[data-emo]` | records/form.html | records.css:418~ | ❌ |
| 감정 배지 (표시용) | `.card-emotion-badge` | records/list, my-page, `app.js` 템플릿 문자열 | records.css:290 | ❌ (JS 와 HTML 이중 구현) |
| 기록 카드 (그리드) | `.movie-card.style-side` > `.movie-thumb` + `.movie-info` | records/list.html, my-page.html | records.css:118~ | ❌ — 두 파일에 거의 동일 마크업 |
| 레코드 카드 (상세 페이지) | `.record-card*` | content/movie-detail, tv-detail | content.css:350~ | ❌ — 두 파일 중복 |
| 리뷰 카드 (홈) | `.review-card`, `.rc-*` | home.html | home.css:880~ | ❌ |
| 포스터 카드 (홈 레일) | `.boxoffice-card`, `.np-item`, `.up-item`, `.rating-row` | home.html (4종) | home.css:520~840 | ❌ — 레일마다 별도 클래스 |
| 포스터 플레이스홀더 | `.poster-art(.poster-art--no-meta)` > `.pa-rule` + `.pa-eng` | home.html 5곳 | home.css:124~ | ❌ — 같은 블록 5회 복사 |
| 포스터 (상세/검색) | `.detail-poster(-placeholder)`, `.search-card-poster`, `.filmography-card-poster` | content/*, search/results | content.css, search.css | ❌ |
| 별점 입력 | `.star-rating` > `.stars-wrap` > `.stars-bg`/`.stars-fg` + `.star-slot`×10 | records/form.html | records.css:471~, form.js | ❌ |
| 별점 표시 | ① `.movie-thumb-rating` "★ 4.5" ② `.murabel-star`+`.murabel-score`+`.rating-max` ③ `.record-card-rating` ④ `.rr-rating` ⑤ `.rc-stars` ⑥ `.rating-display-compact`(인라인 스타일) ⑦ `.rating-display`(모달, JS) | list/my-page, content/*, home, records/detail, record-modal | records.css, content.css, home.css | ❌ — **7가지 표현**, 별 색도 `--accent` 와 `#f5c518` 혼재 |
| 외부 평점 배지 | `.rating-badge-external.rating-{tmdb,imdb,rt-*,mc-*}` | content/movie-detail, tv-detail | content.css:122~ | ❌ |
| 미디어 타입 배지 | `.media-type-badge` | content/*, search/results | style.css:615, content.css:55 | ❌ |
| 섹션 헤딩 | `.section-heading` > `.eyebrow` + `h2` + `.subtitle` | home.html 5회 | home.css:57~ | ❌ |
| 툴바 (제목·정렬) | `.toolbar` > `.toolbar-left/right`, `.sort-form` > `.sort-select` | records/list, my-page | records.css:7~ | ❌ |
| 페이지네이션 | `.pagination` (`.active`, `.disabled`) | records/list, my-page | records.css:308~ | ❌ |
| 빈 상태 | `.empty-state` + `.empty-illustration`(SVG "EMPTY REEL") | records/list, my-page(2회), search/results | records.css:346~, content.css:396 | ❌ — SVG 3회 복사 |
| 빈 상태 (소) | `.empty-state-small` | content/movie-detail, tv-detail | content.css | ❌ |
| 에러 카드 | `.error-scene` > `.error-card` > `.error-code/-title/-desc/-actions` | error/404, 500, 503 | style.css:400~ | ❌ — 세 파일 구조 동일 |
| auth 카드 | `.auth-scene` > `.auth-container` > `.auth-brand` + `.film-strip` | auth/login, signup | auth.css | ❌ |
| 상세 hero | `.detail-hero` > `-overlay` + `-inner` + `.detail-poster` + `.detail-info` | content/movie-detail, tv-detail | content.css:11~ | ❌ — 두 파일 중복 |
| 홈 hero 슬라이더 | `.home-hero`, `.hero-*` | home.html | home.css:220~, home.js | ❌ (단일 사용) |
| 가로 레일 | `.h-rail-track`, `.swiper.boxoffice-swiper` | home.html | home.css:30~, 1024~ | ❌ |
| 관리자 탭/테이블 | `.admin-*` | admin/members.html | admin.css | ❌ (단일 사용) |

**fragment 추출 우선순위 (중복도 기준)**: 기록 카드 → 별점 표시 → 빈 상태(+SVG) → 폼 필드 → 에러 카드 → 상세 hero/레코드 카드 → 포스터 플레이스홀더 → 섹션 헤딩 → 페이지네이션·툴바.

### 참고: 정리 시 함께 볼 것

- **죽은 CSS**: `.variant-bold *`(style.css:448~, 템플릿 사용 없음), `.movie-card.style-poster`(records.css, 사용 없음), `.tab-add*`(layout.html 에서 주석 처리됨).
- **미사용 토큰**: `--space-*` 전부, `--shadow-*` 전부, `--grad-*` 전부, `--dur-*` 전부, `--lh-*`, `--tracking-tight(-2)`, `--font-mono`, `--fs-body`, `--surface-alpha`, `--overlay`, `--error-soft`, `--taste-*`, `--container-max/-pad`, `--ease-settle`, `--radius-*`(alias `--r-*` 만 사용).
- **브레이크포인트**: `480 / 560 / 720 / 820 / 960 / 1100px` (720px 가 16회로 사실상 모바일 기준). 토큰 없음.
- **탭바 active 조건**: `layout.html` 의 탭바 "홈" 링크 `th:classappend` 가 `/records` 경로 조건으로 되어 있어 "감상평" 탭과 동시에 active 가 된다(문서 범위 밖이지만 발견 사항으로 기록).

---

## 6. 인라인 style 목록

### 6-1. 정적 `style="…"` (토큰/클래스로 옮길 대상)

| 위치 | 내용 | 비고 |
|---|---|---|
| `templates/fragments/layout.html:50` | `<form … style="display:inline">` (로그아웃) | 유틸 클래스로 대체 가능 |
| `templates/fragments/layout.html:157` | `<img … tmdb-logo.svg style="height: 20px;">` | |
| `templates/fragments/record-modal.html:31` | `<form id="modalDeleteForm" style="display:inline">` | `app.js:99` 가 다시 `display` 를 덮어씀 |
| `templates/records/detail.html:31` | `<span style="color:var(--accent);letter-spacing:1px">★</span>` | 별점 표시 ⑥ |
| `templates/records/detail.html:32` | `<span style="color:var(--fg-muted);font-size:0.85rem;margin-left:4px">` | |
| `templates/records/detail.html:48` | `<form … style="display:inline">` (삭제) | |
| `templates/records/form.html:128–137` | `.star-slot` 10개 `style="left:N0%; width:10%"` | 0.5점 단위 클릭 영역. `nth-child` 또는 생성 루프로 대체 가능 |
| `templates/home.html:38` | `style="--c1:#1f2a1a; --c2:#3a3520; --c3:#0c0b18; --pa-accent:#e8b84b;"` | hero 포스터 아트 색. home.css·home.js 와 3중 중복 |
| `templates/admin/members.html:71` | `style="display:flex; justify-content:center; gap:0.4rem;"` | |
| `templates/admin/members.html:93` | `<div id="tab-withdrawn" … style="display:none">` | 탭 초기 상태 — `hidden` 속성 권장 |
| `templates/error/404.html:11` | `style="font-family:var(--font-display);color:var(--fg-dim);letter-spacing:0.2em;font-size:0.78rem;margin-bottom:0.5rem"` | "SCENE NOT FOUND" eyebrow |
| `templates/error/500.html:11` | 〃 | "TECHNICAL DIFFICULTIES" |
| `templates/error/503.html:11` | 〃 | "SERVICE UNAVAILABLE" — 세 파일 동일 스타일 → `.error-eyebrow` 클래스 |

SVG 프레젠테이션 속성 `font-style="italic"` (my-page.html:65, 182 / records/list.html:78) 은 SVG 속성이므로 인라인 CSS 가 아니다.

### 6-2. 동적 `th:style` (서버 데이터 바인딩 — 유지가 타당한 것 위주)

| 위치 | 내용 | 판단 |
|---|---|---|
| `templates/home.html:24` | hero 배경 `background-image: url(spotlights[0].backdropUrl)` | 유지 (데이터) |
| `templates/home.html:113` | 박스오피스 포스터 `background-image` | 유지 |
| `templates/home.html:273` | 리뷰 카드 `--rc-accent / --rc-avatar-bg / --rc-avatar-bd` 를 `index % 3` 으로 3색 순환 | **CSS 로 이동 권장** — `.review-card:nth-child(3n+1)` 로 표현 가능, 색 리터럴 9개 제거 |
| `templates/content/movie-detail.html:15`, `tv-detail.html:15` | 상세 hero 배경 이미지 | 유지 |
| `templates/my-page/my-page.html:99` | 월별 막대 `height: count*100/max %` | 유지 (데이터) |
| `templates/my-page/my-page.html:116` | 도넛 세그먼트 `stroke: var(--emo-${code})` | 클래스(`.emo-${code}`)로 대체 가능 |
| `templates/my-page/my-page.html:122` | 범례 스와치 `background: var(--emo-${code})` | 〃 |
| `templates/my-page/my-page.html:211` | 카드 썸네일 `background-image` | 유지 |
| `templates/my-page/my-page.html:223` | 감정 배지 `background: var(--emo-…)` | `[data-emo]` 속성 선택자로 대체 가능 (칩과 통일) |
| `templates/records/form.html:38` | 기존 썸네일 미리보기 `background-image` | 유지 |
| `templates/records/detail.html:20` | 썸네일 `background-image` | 유지 |
| `templates/records/list.html:109` | 카드 썸네일 `background-image` | 유지 |
| `templates/records/list.html:122` | 감정 배지 `background: var(--emo-…)` | `[data-emo]` 로 대체 가능 |

### 6-3. JS 가 직접 쓰는 스타일

| 위치 | 내용 |
|---|---|
| `static/js/app.js:64` | 감정 배지 HTML 문자열에 `style="background:var(--emo-${code})"` |
| `static/js/app.js:73, 77` | 모달 썸네일 `style.backgroundImage` |
| `static/js/app.js:98, 99` | 수정/삭제 버튼 `style.display` 토글 |
| `static/js/form.js:20` | 별점 전경 `style.width = (v/5*100)%` |
| `static/js/home.js:31–34` | hero `--hero-c1/-c2/-accent/-accent-30` setProperty (폴백 `#1f2a1a`, `#3a3520`, `#e8b84b`) |
| `static/js/home.js:46` | hero 배경 `style.backgroundImage` |
| `static/js/tmdb-search.js:72, 75` | 포스터 미리보기 `style.backgroundImage` |

---

## 요약: 우선 정리할 것

1. **이미 정의된 토큰을 실제로 쓰게 하기** — `--space-*`, `--shadow-*`, `--grad-*`, `--overlay` 는 정의만 있고 참조 0회. 같은 값의 리터럴(예: `.auth-container` 그림자 = `--shadow-auth`)부터 치환.
2. **home.css 의 리터럴 골드/크림/배경**(`#e8b84b`×10, `#f0ebe0`×5, `#0c0b18`×4) → `var(--accent|--fg|--bg)`.
3. **알파 변종 정리** — 골드 알파 15종, 크림 알파 9종, 순백 알파 9종을 각 4~5단계 토큰으로.
4. **별점 표현 7종 → fragment 1개**(`fragments/rating :: stars(value, size)`), 별 색은 `--accent` 로 통일하고 외부 평점(IMDb 노랑)만 예외.
5. **기록 카드·빈 상태·폼 필드·에러 카드**를 `th:fragment` 로 추출해 복사 마크업 제거.
