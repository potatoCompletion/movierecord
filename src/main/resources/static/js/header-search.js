(function () {
    const input = document.getElementById('header-search-input');
    const dropdown = document.getElementById('header-search-dropdown');
    const btn = document.getElementById('header-search-btn');
    if (!input || !dropdown) return;

    const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p/w92';
    const MEDIA_LABEL = { movie: '영화', tv: 'TV', person: '인물' };

    let timer = null;
    let activeIndex = -1;

    if (btn) {
        btn.addEventListener('click', function () {
            const q = input.value.trim();
            if (q) window.location.href = '/search?q=' + encodeURIComponent(q);
        });
    }

    input.addEventListener('input', function () {
        clearTimeout(timer);
        activeIndex = -1;
        const q = this.value.trim();
        if (!q) {
            hideDropdown();
            return;
        }
        timer = setTimeout(() => fetchResults(q), 300);
    });

    input.addEventListener('keydown', function (e) {
        const items = dropdown.querySelectorAll('.header-search-item, .header-search-more');
        const total = items.length;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            if (total === 0) return;
            activeIndex = Math.min(activeIndex + 1, total - 1);
            updateActive(items);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            if (total === 0) return;
            activeIndex = Math.max(activeIndex - 1, -1);
            updateActive(items);
        } else if (e.key === 'Enter') {
            if (activeIndex >= 0 && items[activeIndex]) {
                e.preventDefault();
                window.location.href = items[activeIndex].href;
            } else {
                const q = this.value.trim();
                if (q) window.location.href = '/search?q=' + encodeURIComponent(q);
            }
        } else if (e.key === 'Escape') {
            hideDropdown();
            input.blur();
        }
    });

    document.addEventListener('click', function (e) {
        if (!document.getElementById('header-search').contains(e.target)) {
            hideDropdown();
        }
    });

    function updateActive(items) {
        items.forEach(function (el, i) {
            el.classList.toggle('header-search-item--active', i === activeIndex);
        });
    }

    function fetchResults(q) {
        fetch('/api/tmdb/search/unified?query=' + encodeURIComponent(q))
            .then(function (res) { return res.json(); })
            .then(function (items) { renderDropdown(items.slice(0, 5), q); })
            .catch(function () { hideDropdown(); });
    }

    function renderDropdown(items, q) {
        activeIndex = -1;
        if (!items.length) {
            hideDropdown();
            return;
        }
        dropdown.innerHTML = '';
        items.forEach(function (item) {
            const el = document.createElement('a');
            el.className = 'header-search-item';
            el.href = buildHref(item);

            const img = document.createElement('img');
            img.className = 'header-search-thumb';
            img.alt = item.title || '';
            img.src = item.posterPath ? (TMDB_IMAGE_BASE + item.posterPath) : '/images/no-poster.png';

            const info = document.createElement('div');
            info.className = 'header-search-info';

            const title = document.createElement('span');
            title.className = 'header-search-title';
            title.textContent = item.title || '';

            const meta = document.createElement('span');
            meta.className = 'header-search-meta';
            const label = MEDIA_LABEL[item.mediaType] || item.mediaType;
            const year = item.releaseDate ? item.releaseDate.substring(0, 4) : '';
            meta.textContent = label + (year ? ' · ' + year : '');

            info.appendChild(title);
            info.appendChild(meta);
            el.appendChild(img);
            el.appendChild(info);
            dropdown.appendChild(el);
        });

        const more = document.createElement('a');
        more.className = 'header-search-more';
        more.href = '/search?q=' + encodeURIComponent(q);
        more.textContent = '전체 결과 보기 →';
        dropdown.appendChild(more);

        dropdown.removeAttribute('hidden');
    }

    function buildHref(item) {
        if (item.mediaType === 'movie') return '/movie/' + item.id;
        if (item.mediaType === 'tv') return '/tv/' + item.id;
        if (item.mediaType === 'person') return '/person/' + item.id;
        return '/search?q=' + encodeURIComponent(item.title || '');
    }

    function hideDropdown() {
        dropdown.setAttribute('hidden', '');
        dropdown.innerHTML = '';
        activeIndex = -1;
    }
}());
