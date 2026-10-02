# Murabel design system — conventions

Murabel is a server-rendered app (Spring Boot + Thymeleaf). **There are no React/JS components** — `window.Murabel` is empty. Build every screen from **plain HTML elements carrying Murabel's CSS classes**, styled by `styles.css` (tokens + the app's real global CSS). Read each `components/<group>/<Name>/<Name>.prompt.md` for the exact markup; copy structure and class names verbatim.

## Setup

- Load `styles.css`. It imports `fonts/pretendard.css` (Pretendard Variable, shipped woff2), `tokens/colors_and_type.css` (custom properties) and `_ds_bundle.css` (the app's `style.css`, `records.css`, `my-page.css`).
- Dark theme only: `body` already gets `background: var(--bg)` and `color: var(--fg)`. Never put content on white.
- Page frame: `<main class="container">…</main>` (max 1200px, 1.5rem side padding).

## Styling idiom — CSS classes + `var(--*)` tokens

No utility classes exist. For layout glue you write yourself, use tokens:

| Family | Tokens |
|---|---|
| Surfaces | `--bg`, `--bg-alt`, `--surface`, `--surface-2`, `--elevated` |
| Borders | `--border`, `--border-strong`, `--border-gold`, `--border-gold-mid` |
| Text | `--fg`, `--fg-muted`, `--fg-dim`, `--fg-faint` |
| Accent (gold) | `--accent`, `--accent-hover`, `--accent-dark`, `--accent-bg`, `--accent-bg-strong` |
| State | `--success`, `--danger`, `--error` |
| Emotions | `--emo-funny`, `--emo-tense`, `--emo-sad`, `--emo-linger`, `--emo-scary`, `--emo-none` |
| Type size | `--fs-hero`, `--fs-h2`, `--fs-h3`, `--fs-sub`, `--fs-lg`, `--fs-md`, `--fs-sm`, `--fs-caption`, `--fs-meta`, `--fs-label` |
| Family | `--font-display`, `--font-body` (both Pretendard) |
| Radius | `--r-sm` 8px, `--r-md` 12px, `--r-lg` 18px, `--r-pill` |
| Spacing | `--space-1`…`--space-16` (0.25rem steps: 1,2,3,4,5,6,8,10,12,16) |

Gold (`--accent`) is the one accent: primary CTA, active state, rating stars, focus ring. Text on gold is `#0e0b1a`. Weights: 500 meta, 600 controls/labels, 700 headings.

## Component vocabulary

- Buttons: `.btn` + `btn-primary | btn-secondary | btn-ghost | btn-danger`, modifiers `btn-sm`, `btn-block`.
- Forms: `.form-group` > `label` + `.form-control` + `.field-error` / `.field-hint` / `.char-counter`; `.input-row` for input + button.
- Choice chips: `.chip-group` > `label.chip-label` > `input` + `span.chip[data-emo]`.
- Rating input: `.star-rating`. Read-only rating: text `★ 4.5` in gold.
- Record cards: `.movie-grid` > `article.movie-card.style-side`. Posters are TMDB CDN URLs: `https://image.tmdb.org/t/p/w342/<path>`.
- Empty lists: `.empty-state`.
- Flash: `<div class="flash-message">` (success) / `<div class="flash-message error">`.

UI copy is Korean.

## Example

```html
<main class="container">
  <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:var(--space-6)">
    <h1 style="font-size:var(--fs-hero);font-weight:700;letter-spacing:-0.02em;margin:0">감상평</h1>
    <a href="/records/new" class="btn btn-primary">＋ 감상평 등록</a>
  </div>
  <div class="movie-grid">
    <article class="movie-card style-side" role="button" tabindex="0">
      <div class="movie-thumb" style="background-image:url(https://image.tmdb.org/t/p/w342/jjHccoFjbqlfr4VGLVLT7yek0Xn.jpg)">
        <span class="movie-thumb-rating">★ <span>4.5</span></span>
      </div>
      <div class="movie-info">
        <div>
          <h3 class="movie-title">기생충</h3>
          <div class="movie-meta"><span>2026-09-27</span></div>
        </div>
        <div class="card-nickname">by <span>무비러버</span></div>
      </div>
    </article>
  </div>
</main>
```
