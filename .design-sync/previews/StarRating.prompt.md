# StarRating — 0.5-step 5-star input: `.star-rating` > `.stars-wrap` (bg + fg + 10 `.star-slot`s) + `.rating-value`.

```html
<div class="star-rating">
  <div class="stars-wrap" id="starRating">
    <span class="stars-bg"></span>
    <span class="stars-fg" id="starsFg" style="width:90%"></span>
    <span class="star-slot" data-value="0.5" style="left:0%;  width:10%"></span>
    <!-- … one slot per 0.5, left: 10% steps … -->
    <span class="star-slot" data-value="5.0" style="left:90%; width:10%"></span>
  </div>
  <span class="rating-value">4.5</span>
  <input type="hidden" name="rating" value="4.5">
</div>
```

- Stars are drawn by CSS (`.stars-bg` dim, `.stars-fg` gold) — no icon font or images.
- Filled amount = `.stars-fg` width = `rating / 5 * 100%`. In the app `static/js/form.js` sets it on hover/click; for a static mock set it inline.
- Exactly 10 `.star-slot`s, `left` 0%–90% in 10% steps, `data-value` 0.5–5.0. They are invisible hit areas.
- `.rating-value` shows one decimal place (`4.5`, `0.0`).
- This is the input. Read-only ratings in the app are text: `★ 4.5` (see MovieCard's `.movie-thumb-rating`).
