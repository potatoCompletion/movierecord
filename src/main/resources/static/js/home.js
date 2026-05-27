/* ============================================================
   MURABEL — HOME (Variant A)
   파일 위치: src/main/resources/static/js/home.js
   ------------------------------------------------------------
   - Hero 캐러셀 자동 회전 (7s)
   - 도트 클릭으로 직접 이동
   ============================================================ */

(function () {
    'use strict';

    const hero = document.querySelector('.home-hero');
    if (!hero) return;

    const slides = Array.from(hero.querySelectorAll('.hero-slide'));
    const dots   = Array.from(hero.querySelectorAll('.hero-dots button'));
    if (slides.length <= 1) return;

    const bg = hero.querySelector('.hero-bg');
    let idx = 0;
    let timer = null;

    function applyTone(slide) {
        // 각 slide 는 data-c1, data-c2, data-accent 를 가진다.
        const c1     = slide.dataset.c1     || '#1f2a1a';
        const c2     = slide.dataset.c2     || '#3a3520';
        const accent = slide.dataset.accent || '#e8b84b';

        hero.style.setProperty('--hero-c1', c1);
        hero.style.setProperty('--hero-c2', c2);
        hero.style.setProperty('--hero-accent', accent);
        // hex + 알파 합성 (대략 30%, 19% 정도의 투명도)
        hero.style.setProperty('--hero-accent-30', accent + '30');
    }

    function show(i) {
        idx = ((i % slides.length) + slides.length) % slides.length;
        slides.forEach((el, k) => el.classList.toggle('active', k === idx));
        dots.forEach((el, k)   => el.classList.toggle('active', k === idx));
        applyTone(slides[idx]);
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
