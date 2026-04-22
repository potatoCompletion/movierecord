(function () {
    const modal = document.getElementById('movieModal');
    if (!modal) return;

    const els = {
        title: document.getElementById('modalTitle'),
        thumb: document.getElementById('modalThumb'),
        watchedDate: document.getElementById('modalWatchedDate'),
        rating: document.getElementById('modalRating'),
        oneLiner: document.getElementById('modalOneLiner'),
        immersion: document.getElementById('modalImmersion'),
        story: document.getElementById('modalStory'),
        emotion: document.getElementById('modalEmotion'),
        good: document.getElementById('modalGood'),
        bad: document.getElementById('modalBad'),
        taste: document.getElementById('modalTaste'),
        editLink: document.getElementById('modalEditLink'),
        deleteForm: document.getElementById('modalDeleteForm'),
    };

    function renderStars(rating) {
        const r = parseFloat(rating || '0');
        const full = Math.floor(r);
        const half = r - full >= 0.5 ? 1 : 0;
        const empty = 5 - full - half;
        return '★'.repeat(full) + (half ? '½' : '') + '☆'.repeat(empty) + '  ' + r.toFixed(1);
    }

    function textOrDash(value) {
        return value && value.trim().length > 0 ? value : '-';
    }

    function openModal(card) {
        const d = card.dataset;
        els.title.textContent = d.title || '';
        if (d.thumbnailUrl) {
            els.thumb.style.backgroundImage = `url(${d.thumbnailUrl})`;
            els.thumb.textContent = '';
            els.thumb.classList.remove('no-thumb');
        } else {
            els.thumb.style.backgroundImage = '';
            els.thumb.textContent = '이미지 없음';
            els.thumb.classList.add('no-thumb');
        }
        els.watchedDate.textContent = d.watchedDate || '-';
        els.rating.textContent = renderStars(d.rating);
        els.oneLiner.textContent = textOrDash(d.oneLiner);
        els.immersion.textContent = d.immersion || '-';
        els.story.textContent = d.story || '-';
        els.emotion.textContent = d.emotion || '-';
        els.good.textContent = textOrDash(d.goodPoints);
        els.bad.textContent = textOrDash(d.badPoints);
        els.taste.textContent = d.taste || '-';
        els.editLink.href = `/movies/${d.id}/edit`;
        els.deleteForm.action = `/movies/${d.id}/delete`;

        modal.classList.add('open');
        modal.setAttribute('aria-hidden', 'false');
    }

    function closeModal() {
        modal.classList.remove('open');
        modal.setAttribute('aria-hidden', 'true');
    }

    document.querySelectorAll('.movie-card').forEach(card => {
        card.addEventListener('click', () => openModal(card));
    });

    modal.querySelectorAll('[data-modal-close]').forEach(btn => {
        btn.addEventListener('click', closeModal);
    });

    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal.classList.contains('open')) {
            closeModal();
        }
    });
})();
