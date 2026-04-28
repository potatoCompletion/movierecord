(function () {
    const searchInput = document.getElementById('title');
    const dropdown = document.getElementById('tmdbDropdown');
    const thumbPreview = document.getElementById('thumbPreview');
    const tmdbIdInput = document.getElementById('tmdbId');
    const mediaTypeInput = document.getElementById('mediaType');
    const posterPathInput = document.getElementById('posterPath');

    if (!searchInput || !dropdown) return;

    const IMAGE_BASE = 'https://image.tmdb.org/t/p/w300';
    let debounceTimer;
    let activeIndex = -1;
    let currentItems = [];

    function search(query) {
        if (!query.trim()) {
            hideDropdown();
            return;
        }
        fetch('/api/tmdb/search?query=' + encodeURIComponent(query))
            .then(function (r) { return r.json(); })
            .then(function (items) { renderDropdown(items); })
            .catch(function () { hideDropdown(); });
    }

    function renderDropdown(items) {
        currentItems = items;
        activeIndex = -1;
        dropdown.innerHTML = '';
        if (!items.length) {
            dropdown.hidden = true;
            return;
        }
        items.forEach(function (item, i) {
            const el = document.createElement('div');
            el.className = 'tmdb-item';
            el.dataset.index = i;
            const year = item.releaseDate ? item.releaseDate.slice(0, 4) : '—';
            const badge = item.mediaType === 'tv' ? 'TV' : '영화';
            const posterHtml = item.posterPath
                ? '<img src="' + IMAGE_BASE + item.posterPath + '" alt="">'
                : '<span class="no-poster">?</span>';
            el.innerHTML =
                '<div class="tmdb-item-poster">' + posterHtml + '</div>' +
                '<div class="tmdb-item-info">' +
                '<span class="tmdb-item-title">' + escapeHtml(item.title) + '</span>' +
                '<span class="tmdb-item-meta">' + year + ' · <span class="tmdb-badge">' + badge + '</span></span>' +
                '</div>';
            el.addEventListener('click', function () { selectItem(item); });
            dropdown.appendChild(el);
        });
        dropdown.hidden = false;
    }

    function setActive(index) {
        const els = dropdown.querySelectorAll('.tmdb-item');
        els.forEach(function (el) { el.classList.remove('tmdb-item--active'); });
        if (index >= 0 && index < els.length) {
            els[index].classList.add('tmdb-item--active');
            els[index].scrollIntoView({ block: 'nearest' });
        }
        activeIndex = index;
    }

    function selectItem(item) {
        if (tmdbIdInput) tmdbIdInput.value = item.id != null ? item.id : '';
        if (mediaTypeInput) mediaTypeInput.value = item.mediaType || '';
        if (posterPathInput) posterPathInput.value = item.posterPath || '';
        if (thumbPreview) {
            if (item.posterPath) {
                thumbPreview.style.backgroundImage = 'url(' + IMAGE_BASE + item.posterPath + ')';
                thumbPreview.innerHTML = '';
            } else {
                thumbPreview.style.backgroundImage = '';
                thumbPreview.textContent = '포스터 없음';
            }
        }
        searchInput.value = item.title || '';
        hideDropdown();
    }

    function hideDropdown() {
        dropdown.hidden = true;
        dropdown.innerHTML = '';
        currentItems = [];
        activeIndex = -1;
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    searchInput.addEventListener('input', function () {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(function () { search(searchInput.value); }, 300);
    });

    searchInput.addEventListener('keydown', function (e) {
        if (dropdown.hidden) return;
        const count = currentItems.length;
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            setActive((activeIndex + 1) % count);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setActive((activeIndex - 1 + count) % count);
        } else if (e.key === 'Enter' && activeIndex >= 0) {
            e.preventDefault();
            selectItem(currentItems[activeIndex]);
        } else if (e.key === 'Escape') {
            hideDropdown();
        }
    });

    document.addEventListener('click', function (e) {
        if (!dropdown.contains(e.target) && e.target !== searchInput) {
            hideDropdown();
        }
    });
})();
