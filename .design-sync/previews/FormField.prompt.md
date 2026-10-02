# FormField — `.form-group` wrapping a label, `.form-control`, and optional error/hint/counter.

```html
<div class="form-group">
  <label for="username">아이디</label>
  <input type="text" id="username" name="username" class="form-control" placeholder="사용할 아이디를 입력하세요">
  <span class="field-error">아이디는 4자 이상 20자 이하로 입력해주세요.</span>
</div>

<div class="form-group">
  <label for="oneLiner">한줄평</label>
  <textarea id="oneLiner" name="oneLiner" class="form-control" maxlength="1000"></textarea>
  <div class="char-counter"><span>0</span>/1000</div>
</div>
```

- `label` inside `.form-group` renders as an uppercase micro label (`--fs-label`, 0.09em tracking, `--fg-muted`). Required mark: `<span class="required">*</span>`.
- `.form-control` works on `input` (text/password/date) and `textarea`: 48px min-height, translucent fill, gold focus ring.
- Feedback: `.field-error` (red), `.field-hint` (dim), `.field-hint.field-success` (green), `.field-hint.field-error`.
- `.char-counter` right-aligns a tabular-number count under textareas.
- Date input: add `form-group-date` to the group. Input + button on one row: wrap them in `.input-row`.
- Forms in the app stack `.form-group`s with `margin-bottom: 1.15rem`; finish with a `.form-actions` row of buttons.
