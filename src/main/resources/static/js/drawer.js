(function () {
    const toggles = document.querySelectorAll('.menu-toggle');
    const overlay = document.querySelector('.drawer-overlay');
    const closeBtn = document.querySelector('.drawer-close');

    function open() {
        document.body.classList.add('drawer-open');
        toggles.forEach(t => t.setAttribute('aria-expanded', 'true'));
    }

    function close() {
        document.body.classList.remove('drawer-open');
        toggles.forEach(t => t.setAttribute('aria-expanded', 'false'));
    }

    toggles.forEach(t => t.addEventListener('click', () => {
        document.body.classList.contains('drawer-open') ? close() : open();
    }));

    overlay?.addEventListener('click', close);
    closeBtn?.addEventListener('click', close);

    document.addEventListener('keydown', e => {
        if (e.key === 'Escape') close();
    });

    const tabBar = document.querySelector('.tab-bar');
    let lastScrollY = window.scrollY;

    window.addEventListener('scroll', () => {
        const currentScrollY = window.scrollY;
        if (currentScrollY > lastScrollY && currentScrollY > 50) {
            tabBar?.classList.add('tab-bar--hidden');
        } else {
            tabBar?.classList.remove('tab-bar--hidden');
        }
        lastScrollY = currentScrollY;
    }, { passive: true });
})();
