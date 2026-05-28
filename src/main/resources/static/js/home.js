/* ============================================================
   MURABEL — HOME (Variant A)
   파일 위치: src/main/resources/static/js/home.js
   ------------------------------------------------------------
   - Hero 캐러셀 자동 회전 (7s)
   - 도트 클릭으로 직접 이동
   - 슬라이드 전환 시 포스터 이미지 / 백드롭 동기화
   ============================================================ */

(function () {
    'use strict';

    const hero = document.querySelector('.home-hero');
    if (!hero) return;

    const slides  = Array.from(hero.querySelectorAll('.hero-slide'));
    const dots    = Array.from(hero.querySelectorAll('.hero-dots button'));
    const posterImg = hero.querySelector('.hero-poster-img');
    const backdrop  = hero.querySelector('.hero-backdrop');

    if (slides.length <= 1) return;

    let idx   = 0;
    let timer = null;

    function applyTone(slide) {
        // 각 slide 는 data-c1, data-c2, data-accent 를 가질 수 있다.
        const c1     = slide.dataset.c1     || '#1f2a1a';
        const c2     = slide.dataset.c2     || '#3a3520';
        const accent = slide.dataset.accent || '#e8b84b';
        hero.style.setProperty('--hero-c1', c1);
        hero.style.setProperty('--hero-c2', c2);
        hero.style.setProperty('--hero-accent', accent);
        hero.style.setProperty('--hero-accent-30', accent + '30');
    }

    function applyMedia(slide) {
        // 포스터 이미지 교체
        if (posterImg) {
            const src = slide.dataset.poster || '';
            if (src) posterImg.src = src;
        }
        // 백드롭 교체
        if (backdrop) {
            const bg = slide.dataset.backdrop || '';
            backdrop.style.backgroundImage = bg ? `url('${bg}')` : '';
        }
    }

    function show(i) {
        idx = ((i % slides.length) + slides.length) % slides.length;
        slides.forEach((el, k) => el.classList.toggle('active', k === idx));
        dots.forEach((el, k)   => el.classList.toggle('active', k === idx));
        applyTone(slides[idx]);
        applyMedia(slides[idx]);
    }

    function start() {
        stop();
        const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (prefersReduced) return;
        timer = setInterval(() => show(idx + 1), 7000);
    }

    function stop() {
        if (timer) { clearInterval(timer); timer = null; }
    }

    dots.forEach((d, i) => {
        d.addEventListener('click', () => { show(i); start(); });
    });

    hero.addEventListener('mouseenter', stop);
    hero.addEventListener('mouseleave', start);

    show(0);
    start();
})();
