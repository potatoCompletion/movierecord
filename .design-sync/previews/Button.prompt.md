# Button — `.btn` + one variant class; markup only, no JS component.

```html
<a href="/records/new" class="btn btn-primary">＋ 감상평 등록</a>
<button type="button" class="btn btn-secondary">닫기</button>
<button type="submit" class="btn btn-danger">삭제</button>
<a href="/my-page" class="btn btn-ghost btn-sm">마이페이지</a>
<button type="submit" class="btn btn-primary btn-block">회원가입</button>
```

| Class | Use |
|---|---|
| `btn-primary` | Gold gradient CTA (one per view). Text color is dark `#0e0b1a`. |
| `btn-secondary` | Outlined, muted text; gold border on hover. Cancel/close/logout. |
| `btn-ghost` | No border; header/nav links. |
| `btn-danger` | Destructive (`--danger`). |
| `btn-sm` | 36px min-height, `--fs-fine`. Header and inline actions. |
| `btn-block` | Full width (auth forms). |

- Always combine `.btn` with exactly one color variant; size modifiers are additive.
- `.btn` is `inline-flex`, min-height 44px, `gap: 0.4rem` — an inline SVG icon can sit before the label.
- A form wrapping a single button uses `style="display:inline"` in the app.
- Do not invent `btn-outline`, `btn-link`, `btn-lg` — they don't exist.
