(function () {
    // Char counters
    document.querySelectorAll('textarea[data-counter]').forEach(textarea => {
        const counter = document.getElementById(textarea.dataset.counter);
        if (!counter) return;
        const update = () => { counter.textContent = textarea.value.length; };
        textarea.addEventListener('input', update);
        update();
    });

    // Thumbnail preview
    const thumbInput = document.getElementById('thumbnail');
    const thumbPreview = document.getElementById('thumbPreview');
    if (thumbInput && thumbPreview) {
        thumbInput.addEventListener('change', () => {
            const file = thumbInput.files && thumbInput.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = (e) => {
                thumbPreview.style.backgroundImage = `url(${e.target.result})`;
                thumbPreview.textContent = '';
            };
            reader.readAsDataURL(file);
        });
    }

    // Star rating
    const stars = document.getElementById('starRating');
    const starsFg = document.getElementById('starsFg');
    const ratingInput = document.getElementById('ratingInput');
    const ratingValue = document.getElementById('ratingValue');

    if (stars && starsFg && ratingInput && ratingValue) {
        function render(value) {
            const v = parseFloat(value) || 0;
            starsFg.style.width = `${(v / 5) * 100}%`;
            ratingValue.textContent = v.toFixed(1);
        }

        const initial = parseFloat(ratingInput.value) || 0;
        render(initial);

        const slots = stars.querySelectorAll('.star-slot');
        slots.forEach(slot => {
            slot.addEventListener('mouseenter', () => render(slot.dataset.value));
            slot.addEventListener('click', () => {
                ratingInput.value = slot.dataset.value;
                render(slot.dataset.value);
            });
        });
        stars.addEventListener('mouseleave', () => render(ratingInput.value));
    }
})();
