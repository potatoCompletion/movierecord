(function () {
    const modal = document.getElementById('movieModal');
    if (!modal) return;

    const currentUserEl = document.getElementById('currentUser');
    const currentUserId = currentUserEl ? currentUserEl.dataset.userId : null;
    const isAdmin = currentUserEl ? currentUserEl.dataset.isAdmin === 'true' : false;

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
        nickname: document.getElementById('modalNickname'),
        editLink: document.getElementById('modalEditLink'),
        deleteForm: document.getElementById('modalDeleteForm'),
    };

    function renderStars(rating) {
        const r = parseFloat(rating || '0');
        const full = Math.floor(r);
        const half = r - full >= 0.5 ? 1 : 0;
        const empty = 5 - full - half;
        let html = '<span class="modal-stars">';
        for (let i = 0; i < full; i++) html += '<span class="modal-star full">★</span>';
        if (half) html += '<span class="modal-star half"><span class="half-inner">★</span>★</span>';
        for (let i = 0; i < empty; i++) html += '<span class="modal-star empty">★</span>';
        html += '<span class="modal-star-value">' + r.toFixed(1) + '</span>';
        html += '</span>';
        return html;
    }

    function textOrDash(value) {
        return value && value.trim().length > 0 ? value : '-';
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function renderEmotionBadges(emotionsText) {
        if (!emotionsText) {
            return '-';
        }

        return emotionsText
            .split('|')
            .filter(Boolean)
            .map(item => {
                const [code, displayName] = item.split(':');

                return `<span class="card-emotion-badge" style="background:var(--emo-${escapeHtml(code)});">${escapeHtml(displayName)}</span>`;
            })
            .join('');
    }

    function openModal(card) {
        const d = card.dataset;
        els.title.textContent = d.title || '';
        if (d.thumbnailUrl) {
            els.thumb.style.backgroundImage = `url(${d.thumbnailUrl})`;
            els.thumb.innerHTML = '';
            els.thumb.classList.remove('no-thumb');
        } else {
            els.thumb.style.backgroundImage = '';
            els.thumb.textContent = '이미지 없음';
            els.thumb.classList.add('no-thumb');
        }
        els.watchedDate.textContent = d.watchedDate || '-';
        els.rating.innerHTML = renderStars(d.rating);
        els.oneLiner.textContent = textOrDash(d.oneLiner);
        els.immersion.textContent = d.immersion || '-';
        els.story.textContent = d.story || '-';

        els.emotion.innerHTML = renderEmotionBadges(d.emotions);
        // els.emotion.textContent = d.emotion || '-';

        els.good.textContent = textOrDash(d.goodPoints);
        els.bad.textContent = textOrDash(d.badPoints);
        els.taste.textContent = d.taste || '-';
        if (els.nickname) els.nickname.textContent = d.nickname || '-';
        els.editLink.href = `/contents/${d.id}/edit`;
        els.deleteForm.action = `/contents/${d.id}/delete`;

        const canEdit = isAdmin || (currentUserId && String(d.ownerId) === currentUserId);
        els.editLink.style.display = canEdit ? '' : 'none';
        els.deleteForm.style.display = canEdit ? 'inline' : 'none';

        modal.classList.add('open');
        modal.setAttribute('aria-hidden', 'false');
    }

    function closeModal() {
        modal.classList.remove('open');
        modal.setAttribute('aria-hidden', 'true');
    }

    function shouldIgnoreCardEvent(event) {
        return event.target.closest('a, button, input, select, textarea, form');
    }

    document.querySelectorAll('.movie-card').forEach(card => {
        card.addEventListener('click', (event) => {
            if (shouldIgnoreCardEvent(event)) return;
            openModal(card);
        });
        card.addEventListener('keydown', (event) => {
            if (event.key !== 'Enter' && event.key !== ' ') return;
            if (shouldIgnoreCardEvent(event)) return;
            event.preventDefault();
            openModal(card);
        });
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
