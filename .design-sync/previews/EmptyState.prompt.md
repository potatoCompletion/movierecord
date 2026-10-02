# EmptyState — `.empty-state` block: optional `.empty-illustration` SVG ("EMPTY REEL"), `h2`, `p`, primary CTA.

```html
<div class="empty-state">
  <div class="empty-illustration"><!-- film-reel SVG, see EmptyState.html --></div>
  <h2>아직 기록된 감상평이 없어요</h2>
  <p>방금 본 영화의 첫 한 줄을 남겨보세요.</p>
  <a href="/records/new" class="btn btn-primary">＋ 첫 감상평 등록하기</a>
</div>
```

- Use for empty lists (records, my page, search). The minimal form is just `<div class="empty-state"><p>…</p></div>` (search: `"<query>" 에 대한 검색 결과가 없습니다.`).
- The illustration is an inline 100×100 viewBox SVG reel drawn with `currentColor`; copy it from EmptyState.html rather than drawing a new one. The `.empty-reel-spin` group rotates slowly (16s loop).
- `.empty-state .btn-primary` gets an extra shine effect. Use at most one CTA.
- Plain links inside `.empty-state` are gold (`--accent`).
- Copy is friendly Korean (-요 style), short: one heading line plus one sentence.
