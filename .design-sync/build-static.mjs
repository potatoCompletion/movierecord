// Off-script design-sync generator for Murabel (Thymeleaf SSR, no React package).
// The standard converter needs a React dist/ entry; this repo has none, so this
// script assembles the upload layout directly from the real CSS and the static
// HTML previews authored in .design-sync/previews/.
//
//   node .design-sync/build-static.mjs            -> ./ds-bundle
//
// Output contract (see design-sync skill): styles.css @import closure,
// _ds_bundle.js with a first-line @ds-bundle header (empty body - there are no
// JS components), components/<group>/<Name>/<Name>.{html,prompt.md} with the
// @dsCard first line, README.md with the conventions header prepended.
import { readFileSync, writeFileSync, mkdirSync, rmSync, existsSync, readdirSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const CSS = join(ROOT, 'src/main/resources/static/css');
const PREVIEWS = join(ROOT, '.design-sync/previews');
const OUT = join(ROOT, 'ds-bundle');
const cfg = JSON.parse(readFileSync(join(ROOT, '.design-sync/config.json'), 'utf8'));

// Component -> display group. Order here is the README order.
const COMPONENTS = cfg.components;

if (existsSync(OUT)) {
  if (!existsSync(join(OUT, '.ds-build-meta.json'))) throw new Error(`refusing to rm ${OUT}: not a prior bundle`);
  rmSync(OUT, { recursive: true, force: true });
}
const write = (rel, data) => {
  const p = join(OUT, rel);
  mkdirSync(dirname(p), { recursive: true });
  writeFileSync(p, data);
};

// ── Fonts: Pretendard Variable (OFL-1.1), shipped locally ──
// The app loads it from jsDelivr via @import in colors_and_type.css. Shipping
// the woff2 keeps designs on-brand without a runtime CDN dependency. The file
// is fetched once into the gitignored cache (pinned version).
const FONT_CACHE = join(ROOT, '.design-sync/.cache/fonts');
const PRETENDARD = 'https://cdn.jsdelivr.net/npm/pretendard@1.3.9/dist';
for (const [file, url] of [
  ['PretendardVariable.woff2', `${PRETENDARD}/web/variable/woff2/PretendardVariable.woff2`],
  ['LICENSE.txt', `${PRETENDARD}/LICENSE.txt`],
]) {
  const p = join(FONT_CACHE, file);
  if (!existsSync(p)) {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`font fetch failed ${res.status}: ${url}`);
    mkdirSync(FONT_CACHE, { recursive: true });
    writeFileSync(p, Buffer.from(await res.arrayBuffer()));
  }
  write(`fonts/${file === 'LICENSE.txt' ? 'Pretendard-LICENSE.txt' : file}`, readFileSync(p));
}
write('fonts/pretendard.css', ['Pretendard Variable', 'Pretendard'].map((family) =>
  `@font-face{font-family:'${family}';font-weight:45 920;font-style:normal;font-display:swap;src:url('./PretendardVariable.woff2') format('woff2-variations')}`,
).join('\n') + '\n');

// ── Styles: tokens + global component CSS ──
// style.css starts with @import url('colors_and_type.css'); that import is
// re-expressed in styles.css so the closure stays flat and resolvable. The
// tokens file's remote Pretendard @import is dropped in favor of fonts/.
write('tokens/colors_and_type.css', readFileSync(join(CSS, 'colors_and_type.css'), 'utf8')
  .replace(/^\s*@import url\('https:\/\/cdn\.jsdelivr\.net\/npm\/pretendard[^;]+;\s*/m, ''));
const styleCss = readFileSync(join(CSS, 'style.css'), 'utf8').replace(/^\s*@import[^;]+;\s*/m, '');
const bundleCss = cfg.componentCss
  .map((f) => `/* ── ${f} ── */\n` + (f === 'style.css' ? styleCss : readFileSync(join(CSS, f), 'utf8')))
  .join('\n\n');
write('_ds_bundle.css', bundleCss);
write('styles.css', '@import "./fonts/pretendard.css";\n@import "./tokens/colors_and_type.css";\n@import "./_ds_bundle.css";\n');

// ── JS bundle: header only. Murabel components are markup + CSS classes. ──
const header = {
  namespace: cfg.globalName,
  components: [],
  sourceHashes: {},
  inlinedExternals: [],
  builtBy: 'murabel-build-static',
};
write('_ds_bundle.js', `/* @ds-bundle: ${JSON.stringify(header)} */\nwindow.${cfg.globalName} = window.${cfg.globalName} || {};\n`);

// ── Component cards ──
const CARD_STYLE = `
    body{margin:0;padding:24px;background:var(--bg);color:var(--fg);font-family:var(--font-body)}
    .ds-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:20px;align-items:start}
    .ds-grid.ds-col{grid-template-columns:1fr}
    .ds-cell{border:1px solid var(--border);border-radius:8px;padding:16px;min-width:0;overflow:hidden;transform:translateZ(0)}
    .ds-cell>h4{margin:0 0 12px;font:600 11px var(--font-body);color:var(--fg-dim);text-transform:uppercase;letter-spacing:.06em}`;

// Every card is one cell per row: the DS pane renders cards narrow, and a
// multi-column grid crops the right-hand cells.
for (const { name, group } of COMPONENTS) {
  const src = readFileSync(join(PREVIEWS, `${name}.html`), 'utf8');
  const prompt = readFileSync(join(PREVIEWS, `${name}.prompt.md`), 'utf8');
  const base = `components/${group}/${name}/${name}`;
  write(`${base}.html`, `<!-- @dsCard group="${group}" -->
<!doctype html>
<html lang="ko"><head><meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="../../../styles.css">
  <style>${CARD_STYLE}
  </style>
</head><body>
<div class="ds-grid ds-col">
${src.trim()}
</div>
</body></html>
`);
  write(`${base}.prompt.md`, prompt);
}

// ── README: conventions header + generated index ──
const headerMd = cfg.readmeHeader ? readFileSync(join(ROOT, cfg.readmeHeader), 'utf8').trim() + '\n\n' : '';
const index = COMPONENTS.map(({ name, group }) => {
  const first = readFileSync(join(PREVIEWS, `${name}.prompt.md`), 'utf8').split('\n', 1)[0].replace(/^#\s*/, '').replace(new RegExp(`^${name}\\s+—\\s+`), '');
  return `- **${name}** (${group}) — ${first} — \`components/${group}/${name}/${name}.prompt.md\``;
}).join('\n');
write('README.md', `${headerMd}## Components

${index}

## Files

- \`styles.css\` — entry point. Imports \`tokens/colors_and_type.css\` (design tokens), \`fonts/pretendard.css\` (Pretendard Variable woff2, OFL-1.1) and \`_ds_bundle.css\` (the app's real global CSS: ${cfg.componentCss.join(', ')}).
- \`_ds_bundle.js\` — empty namespace \`window.${cfg.globalName}\`. There are **no JS/React components**: every component is HTML markup + CSS classes.
- \`components/<group>/<Name>/<Name>.html\` — static preview rendered from the original Thymeleaf markup with th:* attributes removed.
- \`components/<group>/<Name>/<Name>.prompt.md\` — markup recipe and class vocabulary.
`);

write('_ds_needs_recompile', JSON.stringify({ by: 'design-sync-cli' }) + '\n');
write('.ds-build-meta.json', JSON.stringify({ componentCount: COMPONENTS.length, shape: 'static' }, null, 2) + '\n');

console.log(`ds-bundle: ${COMPONENTS.length} components, ${readdirSync(OUT).length} root entries`);
