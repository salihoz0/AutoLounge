// Usage: node website/scripts/build-pages.mjs
// index.html holds the `translations` dictionary and the body. This script
//  1. bakes the EN strings into index.html (so crawlers see them without JS),
//  2. generates tr/index.html with the TR strings baked in,
//  3. regenerates <head> (meta, hreflang, JSON-LD) for both from the same dictionary.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const file = path.join(root, 'index.html');
const src = fs.readFileSync(file, 'utf8');
const T = new Function('return ' + src.match(/const translations = (\{[\s\S]*?\n    \});/)[1])();

const SITE = 'https://autolounge.app';
const REL = 'https://github.com/salihoz0/AutoLounge';
const META = {
  en: {
    url: `${SITE}/`, locale: 'en_US', other: 'tr_TR',
    title: 'AutoLounge: Free & Open Source YouTube & Live TV on Android Auto (No Root)',
    desc: 'AutoLounge brings YouTube, 100+ live TV channels and streaming sites to your Android Auto car screen while parked. Free and open source, no root needed.',
    ogTitle: 'AutoLounge: Free & Open Source Video on Android Auto',
    ogDesc: 'Watch YouTube, live TV and streaming sites on your car screen while parked. 100% free, open source, no root.',
    keywords: 'Android Auto YouTube, watch video on Android Auto without root, Android Auto live TV, AutoLounge, Android Auto apps, open source car infotainment',
    site: 'AutoLounge website'
  },
  tr: {
    url: `${SITE}/tr/`, locale: 'tr_TR', other: 'en_US',
    title: "AutoLounge: Android Auto'da YouTube ve Canlı TV (Açık Kaynak & Ücretsiz)",
    desc: "AutoLounge, YouTube'u, 100+ canlı TV kanalını ve yayın sitelerini park halindeyken Android Auto araç ekranına getirir. Tamamen ücretsiz ve açık kaynak, root gerekmez.",
    ogTitle: "AutoLounge: Android Auto Ekranınızda YouTube ve Canlı TV (Ücretsiz)",
    ogDesc: 'Park halindeyken araç ekranınızda YouTube, canlı TV ve yayın sitelerini izleyin. %100 ücretsiz, açık kaynak, root gerekmez.',
    keywords: "Android Auto YouTube, araçta youtube izleme, Android Auto canlı TV, root olmadan Android Auto video, AutoLounge, açık kaynak Android Auto",
    site: 'AutoLounge web sitesi'
  }
};
const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const attr = (s) => esc(s).replace(/"/g, '&quot;');

function jsonLd(lang) {
  const d = T[lang], m = META[lang], page = m.url;
  const org = { '@type': 'Organization', '@id': `${SITE}/#org`, name: 'AutoLounge', url: `${SITE}/`,
    logo: { '@type': 'ImageObject', url: `${SITE}/assets/logo.png`, width: 512, height: 512 },
    sameAs: [REL, 'https://github.com/salihoz0'] };
  const site = { '@type': 'WebSite', '@id': `${SITE}/#website`, url: `${SITE}/`, name: 'AutoLounge',
    inLanguage: ['en', 'tr'], publisher: { '@id': `${SITE}/#org` } };
  const offer = (price, cur, url, region) => ({ '@type': 'Offer', name: 'Free Open Source', price, priceCurrency: cur,
    availability: 'https://schema.org/InStock', url, eligibleRegion: region });
  const software = { '@type': 'SoftwareApplication', '@id': `${SITE}/#software`, name: 'AutoLounge',
    operatingSystem: 'Android', applicationCategory: 'MultimediaApplication', applicationSubCategory: 'Android Auto video and live TV',
    description: d.about_def, url: `${SITE}/`, downloadUrl: `${REL}/releases/latest`, softwareVersion: '1.0',
    inLanguage: ['tr', 'en', 'es', 'pt', 'fr', 'de', 'it', 'ru', 'ar', 'hi', 'zh'],
    screenshot: `${SITE}/assets/car_cockpit_display.png`, publisher: { '@id': `${SITE}/#org` },
    offers: [
      offer('0', 'USD', `${REL}/releases/latest`, 'Worldwide')
    ],
    featureList: [d.f1_title, d.f2_title, d.f3_title, d.f4_title, d.f5_title, d.f6_title] };
  const webpage = { '@type': 'WebPage', '@id': `${page}#webpage`, url: page, name: m.title, description: m.desc,
    inLanguage: lang, isPartOf: { '@id': `${SITE}/#website` }, about: { '@id': `${SITE}/#software` },
    primaryImageOfPage: { '@type': 'ImageObject', url: `${SITE}/assets/car_cockpit_display.png`, width: 1280, height: 720 } };
  const step = (name, text, url) => ({ '@type': 'HowToStep', name, text, ...(url ? { url: `${page}${url}` } : {}) });
  const howto = { '@type': 'HowTo', '@id': `${page}#howto`, name: d.ig_title, inLanguage: lang, step: [
    step(d.s1_title, d.s1_desc, '#setup'),
    step(d.s2_title, [d.ig_tab_a15 + ': ' + d.ig_a15_1, d.ig_tab_a14 + ': ' + d.ig_new_1 + ' ' + d.ig_new_2 + ' ' + d.ig_new_3,
      d.ig_tab_old + ': ' + d.ig_old_1 + ' ' + d.ig_old_2 + ' ' + d.ig_old_3].join(' '), '#install-guide'),
    step(d.ig_aa_title, d.ig_aa_2 + ' ' + d.ig_aa_3, '#install-guide'),
    step(d.ig_step3, d.ig_aa_4 + ' ' + d.ig_s3_2, '#install-guide')] };
  const faq = { '@type': 'FAQPage', '@id': `${page}#faq`, inLanguage: lang, mainEntity: [...src.matchAll(/data-i18n="q(\d+)"/g)].map((x) => x[1]).map((i) => ({
    '@type': 'Question', name: d['q' + i], acceptedAnswer: { '@type': 'Answer', text: d['a' + i] } })) };
  return JSON.stringify({ '@context': 'https://schema.org', '@graph': [org, site, software, webpage, howto, faq] }, null, 2)
    .replace(/<\//g, '<\\/').replace(/^/gm, '  ');
}

function head(lang, p) {
  const m = META[lang], img = `${SITE}/assets/car_cockpit_display.png`;
  return `<!-- HEAD:START (generated by scripts/build-pages.mjs; edit the template there) -->
  <title>${esc(m.title)}</title>
  <meta name="description" content="${attr(m.desc)}" />
  <meta name="keywords" content="${attr(m.keywords)}" />
  <meta name="author" content="AutoLounge" />
  <meta name="robots" content="index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1" />
  <meta name="theme-color" content="#06080D" />
  <link rel="canonical" href="${m.url}" />
  <link rel="alternate" hreflang="en" href="${SITE}/" />
  <link rel="alternate" hreflang="tr" href="${SITE}/tr/" />
  <link rel="alternate" hreflang="x-default" href="${SITE}/" />

  <meta property="og:type" content="website" />
  <meta property="og:site_name" content="AutoLounge" />
  <meta property="og:url" content="${m.url}" />
  <meta property="og:title" content="${attr(m.ogTitle)}" />
  <meta property="og:description" content="${attr(m.ogDesc)}" />
  <meta property="og:image" content="${img}" />
  <meta property="og:image:width" content="1280" />
  <meta property="og:image:height" content="720" />
  <meta property="og:image:alt" content="${attr(T[lang].alt_cockpit)}" />
  <meta property="og:locale" content="${m.locale}" />
  <meta property="og:locale:alternate" content="${m.other}" />

  <meta name="twitter:card" content="summary_large_image" />
  <meta name="twitter:title" content="${attr(m.ogTitle)}" />
  <meta name="twitter:description" content="${attr(m.ogDesc)}" />
  <meta name="twitter:image" content="${img}" />
  <meta name="twitter:image:alt" content="${attr(T[lang].alt_cockpit)}" />

  <link rel="icon" type="image/svg+xml" href="${p}assets/logo.svg" />
  <link rel="apple-touch-icon" href="${p}assets/logo.png" />
  <link rel="preload" as="image" href="${p}assets/car_cockpit_display.png" fetchpriority="high" />

  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Space+Grotesk:wght@600;700&display=swap" rel="stylesheet" />

  <script type="application/ld+json">
${jsonLd(lang)}
  </script>
  <!-- HEAD:END -->`;
}

function build(lang) {
  const p = lang === 'en' ? '' : '../';
  let html = src.replace(/<!-- HEAD:START[\s\S]*?<!-- HEAD:END -->/, () => head(lang, p));
  const cut = html.indexOf('<!-- Dynamic Localization');
  let [top, js] = [html.slice(0, cut), html.slice(cut)];
  const d = T[lang];
  const need = (k) => { if (d[k] === undefined) throw new Error(`missing ${lang} key ${k}`); return d[k]; };
  top = top.replace(/(<(\w+)\b[^>]*?\bdata-i18n="(\w+)"[^>]*>)([\s\S]*?)(<\/\2>)/g, (_, open, _t, k, _old, close) => open + esc(need(k)) + close);
  top = top.replace(/<img\b[^>]*\bdata-i18n-alt="(\w+)"[^>]*>/g, (tag, k) => tag.replace(/ alt="[^"]*"/, ` alt="${attr(need(k))}"`));
  if (lang === 'tr') {
    top = top.replace(/<html lang="en">/, '<html lang="tr">')
      .replace(/(src|href)="(assets\/|llms\.txt|sitemap\.xml)/g, '$1="../$2')
      .replace('href="tr/" hreflang="tr" lang="tr" aria-label="Türkçe"', 'href="../" hreflang="en" lang="en" aria-label="English"')
      .replace('<span id="currentLang">TR</span>', '<span id="currentLang">EN</span>')
      .replace('id="priceDisplay">$12.99<', 'id="priceDisplay">199 ₺<');
    js = js.replace("const PAGE_LANG = 'en';", "const PAGE_LANG = 'tr';");
  }
  return top + js;
}

const en = build('en');
fs.writeFileSync(file, en);
fs.mkdirSync(path.join(root, 'tr'), { recursive: true });
fs.writeFileSync(path.join(root, 'tr', 'index.html'), build('tr'));
console.log('built index.html and tr/index.html');
