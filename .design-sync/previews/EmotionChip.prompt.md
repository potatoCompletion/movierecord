# EmotionChip — radio/checkbox chips: `.chip-group` > `label.chip-label` > `input` + `span.chip`.

```html
<div class="chip-group">
  <label class="chip-label">
    <input type="checkbox" name="emotions" value="TENSE" checked>
    <span class="chip" data-emo="tense">긴장</span>
  </label>
</div>
```

- The native input is visually hidden; the selected state is styled with `input:checked ~ .chip`. Keep the input immediately before the `.chip`.
- Use `type="radio"` for single choice (몰입감 좋음/보통/별로, 스토리, 내 취향 맞음/안맞음) and `type="checkbox"` for emotions (multi-select).
- Without `data-emo` a checked chip is gold. With `data-emo` it takes the emotion color.
- Emotion codes → labels → token: `funny` 웃김 `--emo-funny`, `tense` 긴장 `--emo-tense`, `sad` 슬픔 `--emo-sad`, `linger` 여운 `--emo-linger`, `scary` 공포 `--emo-scary`, `none` 없음 `--emo-none`. Only these six codes exist.
- Read-only emotion display on cards is a different element: `.card-emotion-badge` (see MovieCard).
