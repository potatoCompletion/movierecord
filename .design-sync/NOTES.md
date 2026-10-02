# design-sync notes — movierecord (Murabel)

- **Off-script shape (`"shape": "static"`).** This repo is Spring Boot SSR + Thymeleaf with no `package.json`, no React, and no Storybook, so the standard converter (`package-build.mjs`) cannot run. `.design-sync/build-static.mjs` builds `ds-bundle/` directly. Re-sync = `node .design-sync/build-static.mjs`, then `package-validate.mjs ./ds-bundle`.
- **User decision (2026-10-02): no React reimplementation.** Upload the real CSS/tokens and static HTML previews only. Previews are the original Thymeleaf markup with `th:*`/`sec:*` removed and sample data filled in. DOM structure and class names stay identical to the source, and every preview file starts with a comment naming its source template lines.
- Scope: Button, FormField, EmotionChip, StarRating, MovieCard, EmptyState. **None of these is a `th:fragment`** in the app; they're repeated inline markup. The comments point at template line ranges, which drift when templates change.
- `_ds_bundle.js` is a header plus an empty `window.Murabel`. Components ship no `.jsx`/`.d.ts` because there's no JS API, so `[BUNDLE_EXPORT]` has nothing to check.
- CSS closure: `styles.css` imports `tokens/colors_and_type.css` and `_ds_bundle.css` (= `style.css` minus its `@import`, + `records.css` + `my-page.css`). `my-page.css` is needed for `.input-row`. Page CSS such as home/content/auth/search/admin is NOT included.
- Pretendard Variable (OFL-1.1) is shipped as `fonts/PretendardVariable.woff2` (~2MB). The build fetches it once from jsDelivr `pretendard@1.3.9` into the gitignored `.design-sync/.cache/fonts/` and drops the remote `@import` from the tokens copy. A fresh clone needs network for the first build.
- Sample posters are real TMDB `poster_path`s taken from themoviedb.org og:image on 2026-10-02 (기생충, 인터스텔라, 센과 치히로의 행방불명, 아가씨), served at `image.tmdb.org/t/p/w342`.
- Render check treats `#root` and any `[id^="r"]` as mount roots. Preview cells use `r0..rN`; avoid other ids that start with `r` in previews (StarRating's `ratingValue`/`ratingInput` ids were dropped for that reason and because ids repeat across cells).
- No `_ds_sync.json` anchor is produced (off-script), so every re-sync re-verifies everything. That is fine at 6 components.

## Re-sync risks

- Previews are hand-copied snapshots of template markup. If `records/list.html`, `records/form.html`, `auth/signup.html`, `my-page.html`, `layout.html`, or `record-modal.html` change, the previews do NOT update automatically. Diff the cited line ranges before re-uploading.
- `.prompt.md` and `conventions.md` name classes and tokens. Re-grep them against `ds-bundle/_ds_bundle.css` and `tokens/` after any CSS change.
- TMDB poster URLs are external; if a path is retired the card shows the placeholder thumb.
