(function () {
    const input = document.getElementById('header-search-input');
    const dropdown = document.getElementById('header-search-dropdown');
    if (!input || !dropdown) return;

    const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p/w92';
    const MEDIA_LABEL = { movie: '영화', tv: 'TV', person: '인물' };

    let timer = null;

    input.addEventListener('input', function () {
        clearTimeout(timer);
        const q = this.value.trim();
        if (!q) {
            hideDropdown();
            return;
        }
        timer = setTimeout(() => fetchResults(q), 300);
    });

    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            const q = this.value.trim();
            if (q) {
                window.location.href = '/search?q=' + encodeURIComponent(q);
            }
        }
    });

    document.addEventListener('click', function (e) {
        if (!document.getElementById('header-search').contains(e.target)) {
            hideDropdown();
        }
    });

    function fetchResults(q) {
        fetch('/api/tmdb/search/unified?query=' + encodeURIComponent(q))
            .then(function (res) { return res.json(); })
            .then(function (items) { renderDropdown(items.slice(0, 5), q); })
            .catch(function () { hideDropdown(); });
    }

    function renderDropdown(items, q) {
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
    }
}());
