# MovieCard — record card in `.movie-grid`: `article.movie-card.style-side` > `.movie-thumb` (poster + rating) + `.movie-info`.

```html
<div class="movie-grid">
  <article class="movie-card style-side" role="button" tabindex="0" data-id="101">
    <div class="movie-thumb" style="background-image:url(https://image.tmdb.org/t/p/w342/jjHccoFjbqlfr4VGLVLT7yek0Xn.jpg)">
      <span class="movie-thumb-rating">★ <span>4.5</span></span>
    </div>
    <div class="movie-info">
      <div>
        <h3 class="movie-title" title="기생충">기생충</h3>
        <div class="movie-meta"><span>2026-09-27</span></div>
        <div class="card-emotions">
          <span class="card-emotion-badge" style="background:var(--emo-tense)">긴장</span>
        </div>
      </div>
      <div class="card-nickname">by <span>무비러버</span></div>
    </div>
  </article>
</div>
```

- Always place cards inside `.movie-grid` (responsive: 4 → 3 → 1 columns at 1100/820/560px).
- `style-side` is the only card variant in use.
- The poster is a CSS background on `.movie-thumb`, from the TMDB CDN: `https://image.tmdb.org/t/p/w342/<posterPath>`. Omit the style when there is no poster; the thumb shows a gold-tinted placeholder.
- `.movie-thumb-rating` is the overlay pill `★ 4.5` (one decimal).
- Emotion badges: `.card-emotion-badge` with `background:var(--emo-<code>)`, codes `funny|tense|sad|linger|scary|none`. Omit `.card-emotions` when there are none.
- In the app, `data-*` attributes on the article (title, one-liner, ratings…) feed the detail modal (`fragments/record-modal.html :: detailModal`).
