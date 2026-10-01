"""
The landing page of https://guhs.nl/ (the site root; the wiki lives in wiki/ below it).

Writes into the landing folder: index.html (the page), 404.html (sends old wiki links like /items/x.html on to
/wiki/items/x.html, and shows the wiki's own 404 page for a real miss), CNAME, robots.txt, sitemap.xml and .nojekyll.
Pictures come from the wiki (wiki/img/...), so they are written by the wiki's Images step. NL first, EN behind the
toggle (same language/theme memory as the wiki). No frameworks: one inline style and one small inline script.
"""
import json
import os
from xml.sax.saxutils import escape as xml_escape

from .assets import EARLY_JS
from .site import MOD_VERSION, t

DOMAIN = "guhs.nl"
ADDRESS = "guhs.nl"
MAP_URL = "https://map.guhs.nl/"
STATUS_API = "https://api.mcsrvstat.us/3/" + ADDRESS
# Is the server open? False = "binnenkort" state (address with a "coming soon" pill, no live status call, a badge on the
# map tile, and a banner on the wiki's server page). Keep this name: the 1.0.0 site on main has False, the 1.1.0 build True.
SERVER_OPEN = True
GUHS_VERSION = MOD_VERSION
MC_VERSION = "26.1.2"
LINKS = [
    ("CurseForge", "https://www.curseforge.com/minecraft/mc-mods/guhs"),
    ("Modrinth", "https://modrinth.com/mod/guhs"),
    ("GitHub", "https://github.com/coecomber/guhs"),
    ("Guhs-pack", "https://github.com/coecomber/guhs-pack"),
]

CSS = r"""
:root{
  --paper:#fff7fa;--card:#fff;--ink:#3a1c30;--muted:#8a6479;--line:#f0d9e3;--line2:#e8c7d6;
  --rasp:#d9467a;--rasp-ink:#b8325f;--rasp-soft:#fde3ec;--cheese:#e9a21c;--cheese-soft:#fff1d2;--lilac:#8f6fc4;--lilac-soft:#efe7fb;
  --mint:#2f9d7e;--mint-soft:#dff5ec;--shadow:0 1px 2px rgba(80,20,50,.06),0 10px 30px rgba(80,20,50,.10);
  --display:"Fredoka","Baloo 2","Trebuchet MS",system-ui,sans-serif;--body:"Nunito Sans","Segoe UI",system-ui,-apple-system,sans-serif;
  --mono:"JetBrains Mono",ui-monospace,Consolas,monospace;color-scheme:light}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){color-scheme:dark;--paper:#1d1219;--card:#291a23;--ink:#f7e7ef;--muted:#bb97ab;
  --line:#3f2836;--line2:#553546;--rasp:#f06a9a;--rasp-ink:#ff8db5;--rasp-soft:#3a1d2a;--cheese:#f2b544;--cheese-soft:#35291a;--lilac:#b99be6;
  --lilac-soft:#2c2138;--mint:#5fcfab;--mint-soft:#18302a;--shadow:0 1px 2px rgba(0,0,0,.3),0 10px 30px rgba(0,0,0,.3)}}
:root[data-theme="dark"]{color-scheme:dark;--paper:#1d1219;--card:#291a23;--ink:#f7e7ef;--muted:#bb97ab;--line:#3f2836;--line2:#553546;
  --rasp:#f06a9a;--rasp-ink:#ff8db5;--rasp-soft:#3a1d2a;--cheese:#f2b544;--cheese-soft:#35291a;--lilac:#b99be6;--lilac-soft:#2c2138;
  --mint:#5fcfab;--mint-soft:#18302a;--shadow:0 1px 2px rgba(0,0,0,.3),0 10px 30px rgba(0,0,0,.3)}
*{box-sizing:border-box}
html{-webkit-text-size-adjust:100%}
body{margin:0;background:var(--paper);color:var(--ink);font:17px/1.6 var(--body);overflow-x:hidden;
  background-image:radial-gradient(var(--cheese-soft) 18%,transparent 19%),radial-gradient(var(--rasp-soft) 14%,transparent 15%);
  background-size:46px 46px,46px 46px;background-position:0 0,23px 23px}
html[data-lang="en"] [lang="nl"]:not(html),html[data-lang="nl"] [lang="en"]:not(html){display:none!important}
a{color:var(--rasp-ink)}
img{max-width:100%;height:auto}
:focus-visible{outline:3px solid var(--cheese);outline-offset:3px;border-radius:6px}
.wrap{max-width:1120px;margin:0 auto;padding:0 16px}
/* top bar */
.bar{display:flex;align-items:center;gap:12px;padding:14px 0}
.brand{display:flex;align-items:center;gap:8px;font:700 26px/1 var(--display);color:var(--rasp);text-decoration:none}
.brand img{width:36px;height:36px;image-rendering:pixelated}
.bar .sp{flex:1}
.seg{display:inline-flex;border:1.5px solid var(--rasp);border-radius:999px;overflow:hidden;background:var(--card)}
.seg button{border:0;background:transparent;color:var(--rasp-ink);font:700 13px/1 var(--body);padding:8px 12px;cursor:pointer}
.seg button[aria-pressed="true"]{background:var(--rasp);color:#fff}
.icon-btn{border:1.5px solid var(--line2);background:var(--card);color:var(--ink);border-radius:999px;width:38px;height:34px;cursor:pointer;font:16px/1 var(--body)}
/* hero */
.hero{position:relative;isolation:isolate;display:grid;grid-template-columns:minmax(0,1.25fr) minmax(0,.9fr);gap:24px;align-items:center;
  background:linear-gradient(140deg,var(--card) 0%,var(--rasp-soft) 55%,var(--cheese-soft) 100%);border:2px solid var(--line2);
  border-radius:36px 36px 36px 8px;padding:34px 38px;box-shadow:var(--shadow);margin:8px 0 26px}
.kicker{display:inline-flex;align-items:center;gap:8px;font:700 13px/1 var(--body);letter-spacing:.09em;text-transform:uppercase;color:var(--rasp-ink);
  background:var(--card);border:1.5px solid var(--line2);border-radius:999px;padding:7px 12px}
.hero h1{font:700 clamp(36px,6.2vw,64px)/1.02 var(--display);margin:16px 0 12px;letter-spacing:-.5px}
.hero h1 em{font-style:normal;color:var(--rasp);position:relative;white-space:nowrap}
.hero h1 em::after{content:"";position:absolute;left:0;right:0;bottom:.04em;height:.22em;background:var(--cheese);opacity:.45;border-radius:99px;z-index:-1}
.pitch{font-size:19px;max-width:40ch;margin:0 0 20px}
.join{display:flex;flex-wrap:wrap;align-items:center;gap:10px}
.addr{display:inline-flex;align-items:center;gap:0;border-radius:18px;background:var(--ink);color:var(--paper);box-shadow:0 5px 0 var(--rasp-ink);overflow:hidden}
.addr code{font:600 clamp(20px,3.4vw,28px)/1 var(--mono);padding:14px 16px 14px 20px;letter-spacing:.5px}
.addr button{align-self:stretch;border:0;background:var(--rasp);color:#fff;font:700 15px/1 var(--body);padding:0 18px;cursor:pointer;min-width:112px}
.addr button:hover{filter:brightness(1.08)}
.status{display:inline-flex;align-items:center;gap:8px;font-weight:700;font-size:15px;background:var(--card);border:1.5px solid var(--line2);
  border-radius:999px;padding:8px 14px;min-height:40px}
.dot{width:11px;height:11px;border-radius:50%;background:var(--muted);flex:none}
.status.on .dot{background:#3cc47c;box-shadow:0 0 0 0 rgba(60,196,124,.6);animation:ping 1.8s infinite}
.status.off .dot{background:#e0625b}
.status.soon{background:var(--cheese-soft);border-color:var(--cheese);color:var(--ink)}.status.soon .dot{background:var(--cheese)}
.badge{position:absolute;top:12px;right:12px;z-index:2;background:var(--cheese);color:#3a1c30;font:700 12px/1 var(--body);letter-spacing:.06em;
  text-transform:uppercase;border-radius:999px;padding:6px 10px;transform:rotate(4deg);box-shadow:0 2px 0 rgba(0,0,0,.15)}
@keyframes ping{70%{box-shadow:0 0 0 9px rgba(60,196,124,0)}100%{box-shadow:0 0 0 0 rgba(60,196,124,0)}}
.small{font-size:14px;color:var(--muted);margin:14px 0 0}
.art{position:relative;display:flex;justify-content:center;align-items:flex-end;min-height:280px}
.art::before{content:"";position:absolute;width:82%;aspect-ratio:1;border-radius:50%;background:var(--card);border:2px dashed var(--line2);top:4%}
.art img{position:relative;max-height:320px;width:auto;filter:drop-shadow(0 16px 14px rgba(80,20,50,.2));animation:bob 4.5s ease-in-out infinite}
.bubble{position:absolute;top:2%;right:2%;background:var(--card);border:2px solid var(--ink);border-radius:18px 18px 18px 4px;padding:6px 12px;
  font:700 18px/1.1 var(--display);transform:rotate(6deg);box-shadow:3px 3px 0 var(--cheese)}
@keyframes bob{50%{transform:translateY(-8px) rotate(-1.5deg)}}
@media (prefers-reduced-motion:reduce){.art img,.status.on .dot{animation:none}}
/* tiles */
.tiles{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px;margin:0 0 26px}
.tile{position:relative;display:flex;flex-direction:column;text-decoration:none;color:var(--ink);background:var(--card);border:2px solid var(--line2);
  border-radius:28px;overflow:hidden;box-shadow:var(--shadow);transition:transform .15s,border-color .15s}
.tile:hover{transform:translateY(-4px) rotate(-.4deg);border-color:var(--c)}
.tile:nth-child(2):hover{transform:translateY(-4px) rotate(.4deg)}
.tile .pic{height:180px;display:flex;align-items:center;justify-content:center;background:var(--cs);overflow:hidden}
.tile .pic img{max-height:150px;width:auto}
.tile .pic.photo{position:relative}.tile .pic.photo img{position:absolute;inset:0;max-height:none;width:100%;height:100%;object-fit:cover}
.tile .txt{padding:16px 20px 20px;display:flex;flex-direction:column;gap:4px;flex:1}
.tile b{font:700 26px/1.1 var(--display);color:var(--c)}
.tile span.d{font-size:15.5px}
.tile .go{margin-top:auto;padding-top:10px;font-weight:700;color:var(--c)}
.tile .num{position:absolute;top:12px;left:12px;width:38px;height:38px;border-radius:50%;background:var(--c);color:#fff;
  font:700 20px/38px var(--display);text-align:center;box-shadow:0 3px 0 rgba(0,0,0,.18);z-index:2}
.t-map{--c:var(--mint);--cs:var(--mint-soft)}.t-join{--c:var(--rasp);--cs:var(--rasp-soft)}.t-wiki{--c:var(--lilac);--cs:var(--lilac-soft)}
.t-dl{--c:var(--mint);--cs:var(--mint-soft)}.t-guide{--c:var(--rasp);--cs:var(--rasp-soft)}
/* facts + links */
.facts{display:flex;flex-wrap:wrap;gap:8px;list-style:none;padding:0;margin:0 0 22px;justify-content:center}
.facts li{background:var(--card);border:1.5px solid var(--line2);border-radius:999px;padding:6px 14px;font-size:14.5px}
.links{display:flex;flex-wrap:wrap;gap:10px;justify-content:center;margin:0 0 18px}
.links a{display:inline-flex;align-items:center;gap:6px;text-decoration:none;font-weight:700;color:var(--ink);background:var(--card);
  border:1.5px solid var(--line2);border-radius:14px;padding:9px 16px}
.links a:hover{border-color:var(--rasp);color:var(--rasp-ink)}
footer{text-align:center;color:var(--muted);font-size:13.5px;padding:8px 0 36px}
@media (max-width:860px){
  .hero{grid-template-columns:minmax(0,1fr);padding:22px 18px;border-radius:26px 26px 26px 8px}
  .art{min-height:0;order:-1;margin-bottom:6px}.art img{max-height:150px}.art::before{width:170px;top:0}
  .bubble{font-size:15px;right:6%}
  .tiles{grid-template-columns:minmax(0,1fr);gap:14px}
  .tile{flex-direction:row}.tile .pic{height:auto;width:118px;flex:none}.tile .pic img{max-height:86px}
  .tile .txt{padding:12px 14px}.tile b{font-size:21px}.tile .num{width:30px;height:30px;font-size:16px;line-height:30px;top:8px;left:8px}
  .pitch{font-size:17px}
}
@media (max-width:420px){.addr{width:100%}.addr code{flex:1}.addr button{min-width:96px}.tile .pic{width:92px}}
"""

JS = r"""
(function(){
  var html=document.documentElement;
  function store(k,v){try{localStorage.setItem(k,v);}catch(e){}}
  function setLang(l){html.setAttribute('data-lang',l);html.lang=l;store('guhs-wiki-lang',l);
    document.querySelectorAll('[data-setlang]').forEach(function(b){b.setAttribute('aria-pressed',String(b.getAttribute('data-setlang')===l));});
    var te=html.getAttribute('data-title-en');if(te){if(!html.getAttribute('data-title-nl'))html.setAttribute('data-title-nl',document.title);
      document.title=l==='en'?te:html.getAttribute('data-title-nl');}}
  document.querySelectorAll('[data-setlang]').forEach(function(b){b.addEventListener('click',function(){setLang(b.getAttribute('data-setlang'));});});
  setLang(html.getAttribute('data-lang')||'nl');
  var tb=document.getElementById('theme');
  function isDark(){var t=html.getAttribute('data-theme');return t?t==='dark':matchMedia('(prefers-color-scheme: dark)').matches;}
  function showTheme(){if(tb)tb.textContent=isDark()?'☀':'☾';}
  if(tb){tb.addEventListener('click',function(){var t=isDark()?'light':'dark';html.setAttribute('data-theme',t);store('guhs-wiki-theme',t);showTheme();});}
  showTheme();
  // copy the address
  var cb=document.getElementById('copy');
  if(cb){cb.addEventListener('click',function(){
    var a=cb.getAttribute('data-addr'),en=html.getAttribute('data-lang')==='en';
    function done(ok){cb.textContent=ok?(en?'Copied! Njeg!':'Gekopieerd! Njeg!'):(en?'Select & copy':'Selecteer & kopieer');
      setTimeout(function(){cb.textContent=en?'Copy':'Kopieer';},2200);}
    if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(a).then(function(){done(true);},function(){done(false);});}
    else{try{var r=document.createRange();r.selectNodeContents(document.getElementById('addr'));var s=getSelection();s.removeAllRanges();s.addRange(r);done(document.execCommand('copy'));}catch(e){done(false);}}
  });}
  // live status (mcsrvstat.us); stays "unknown" when it can't be reached
  var st=document.getElementById('status');
  if(st&&st.getAttribute('data-api')&&window.fetch){
    var ctl=window.AbortController?new AbortController():null,timer=setTimeout(function(){if(ctl)ctl.abort();},7000);
    fetch(st.getAttribute('data-api'),ctl?{signal:ctl.signal}:{}).then(function(r){return r.json();}).then(function(d){
      clearTimeout(timer);
      var nl=st.querySelector('[lang=nl]'),en=st.querySelector('[lang=en]');
      if(d&&d.online){
        var p=d.players||{},n=p.online||0,m=p.max;
        st.className='status on';
        nl.textContent='Online · '+n+(m?' / '+m:'')+(n===1?' speler':' spelers');
        en.textContent='Online · '+n+(m?' / '+m:'')+(n===1?' player':' players');
      }else if(d){
        st.className='status off';nl.textContent='Even offline (herstart?)';en.textContent='Offline for a bit (restart?)';
      }
    }).catch(function(){clearTimeout(timer);});
  }
})();
"""


def _t(nl, en):
    return t(en, nl)


class Landing:
    def __init__(self, builder, images, site_url, wiki_rel):
        self.b, self.im = builder, images
        self.site_url = site_url.rstrip("/") + "/"
        self.wiki = wiki_rel.strip("/") + "/"

    def img(self, name, alt=""):
        if not self.im.has(name):
            return ""
        path = self.im.use(name)
        w, h = self.im.size(name)
        return f'<img src="{self.wiki}{path}" alt="{alt}" width="{w}" height="{h}" loading="lazy">'

    def first(self, *names):
        for n in names:
            if self.im.has(n):
                return n
        return None

    def html(self):
        w = self.wiki
        hero_img = self.first("guh", "guh_front")
        map_img = self.first("shot210_elftocht_bovenaf", "shot210_doolhof_bovenaf")
        join_img = self.first("npc_reisguh", "guh_outfit_evenementen")
        wiki_img = self.first("icon_guhdex", "guh_sitting", "guh")
        dl_img = self.first("guh_outfit_party", "icon_kaas_knabbels", "guh")
        icon = f'{w}favicon-64.png'
        # The official server is only on the Dutch side: the map and "how do I join" tiles are Dutch-only (lang="nl", hidden by
        # the language switch); the English side gets "Download" and "Getting started" in their place. None = not in that language.
        tiles = [
            ("t-map", MAP_URL, 1, map_img, True, "Live kaart" if SERVER_OPEN else "Live kaart|soon", None,
             "Vlieg over de wereld van de server, in de Overworld én de Guhmensie. Kijk wat iedereen heeft gebouwd!", None,
             "Open de kaart", None),
            ("t-join", f"{w}server.html", 2, join_img, False, "Hoe speel ik mee?", None,
             "Prism Launcher, één link plakken, klaar. Stap voor stap uitgelegd, met de regels en hulp bij problemen.", None,
             "Naar de uitleg", None),
            ("t-dl", f"{w}index.html#download", 1, dl_img, False, None, "Download",
             None, "Guhs is free on CurseForge, Modrinth and GitHub. Put it in your mods folder with GeckoLib and play!",
             None, "Get the mod"),
            ("t-guide", f"{w}aan-de-slag.html", 2, join_img, False, None, "Getting started",
             None, "New here? The step-by-step guide: your first guh, the portal, the Guhmension and your first goals.",
             None, "Read the guide"),
            ("t-wiki", f"{w}index.html", 3, wiki_img, False, "Wiki", "Wiki",
             "Alles over de guhs: temmen, de Guhmensie, minigames, verhalen, kleding en meer dan duizend pagina's.",
             "Everything about the guhs: taming, the Guhmension, minigames, stories, clothes and over a thousand pages.",
             "Open de wiki", "Open the wiki"),
        ]

        def tt(nl, en):        # a text in one or both languages
            return nl if en is None else en if nl is None else _t(nl, en)
        tile_html = ""
        for cls, href, num, pic, photo, h_nl, h_en, d_nl, d_en, go_nl, go_en in tiles:
            only = ' lang="nl"' if h_en is None else ' lang="en"' if h_nl is None else ""
            soon = bool(h_nl) and h_nl.endswith("|soon")
            h_nl = h_nl.split("|")[0] if h_nl else None
            badge = f'<span class="badge">{_t("binnenkort", "soon")}</span>' if soon else ""
            ext = ' rel="noopener"' if href.startswith("http") else ""
            pic_html = self.img(pic, "") if pic else ""
            tile_html += (f'<a class="tile {cls}"{only} href="{href}"{ext}><span class="num" aria-hidden="true">{num}</span>{badge}'
                          f'<span class="pic{" photo" if photo else ""}">{pic_html}</span>'
                          f'<span class="txt"><b>{tt(h_nl, h_en)}</b><span class="d">{tt(d_nl, d_en)}</span>'
                          f'<span class="go">{tt(go_nl, go_en)} &rarr;</span></span></a>')
        # (Dutch, English): None = only in the other language (the server facts are Dutch-only)
        facts = [("Dag en nacht online", None), ("Gratis &amp; open voor iedereen", None), (None, "Free"),
                 ("Geen PvP", None), (f"Minecraft {MC_VERSION}", f"Minecraft {MC_VERSION}"), (f"Guhs {GUHS_VERSION}", f"Guhs {GUHS_VERSION}"),
                 (None, "NeoForge"), (None, "Singleplayer &amp; multiplayer"), ("Mods werken zichzelf bij", None)]
        facts_html = "".join(f"<li>{_t(nl, en)}</li>" if nl and en else f'<li lang="{"nl" if nl else "en"}">{nl or en}</li>'
                             for nl, en in facts)
        links = "".join(f'<a href="{u}" rel="noopener">{n}</a>' for n, u in LINKS)
        if SERVER_OPEN:
            status = (f'<span class="status" id="status" data-api="{STATUS_API}" role="status"><span class="dot" aria-hidden="true"></span>'
                      f'{_t("Status ophalen...", "Checking status...")}</span>')
            small = f"Minecraft {MC_VERSION} met Guhs {GUHS_VERSION}. Nieuw? Begin bij"
        else:
            status = f'<span class="status soon"><span class="dot" aria-hidden="true"></span>{_t("Binnenkort open!", "Opening soon!")}</span>'
            small = f"De server opent binnenkort met Guhs {GUHS_VERSION} (Minecraft {MC_VERSION}). Alvast lezen hoe het werkt:"
        head_desc = "De officiële Guhs-server: lieve vadsige guhs, dag en nacht online op guhs.nl. Live kaart, uitleg om mee te spelen en de Guhs-wiki."
        return f"""<!doctype html>
<html lang="nl" data-lang="nl" data-title-en="Guhs - the Minecraft mod full of lieve vadsige guhs">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Guhs - de officiële Guhs-server</title>
<meta name="description" content="{head_desc}">
<link rel="canonical" href="{self.site_url}">
<meta property="og:title" content="Guhs - de officiële Guhs-server">
<meta property="og:description" content="{head_desc}">
<meta property="og:url" content="{self.site_url}">
{f'<meta property="og:image" content="{self.site_url}{w}{self.im.use(hero_img)}">' if hero_img else ''}
<script>{EARLY_JS}</script>
<link rel="icon" type="image/png" sizes="32x32" href="{w}favicon-32.png">
<link rel="icon" href="{w}favicon.ico" sizes="any">
<link rel="apple-touch-icon" href="{w}apple-touch-icon.png">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Fredoka:wght@500;600;700&amp;family=Nunito+Sans:ital,wght@0,400;0,700;1,400&amp;family=JetBrains+Mono:wght@500;600&amp;display=swap">
<style>{CSS.strip()}</style>
</head>
<body>
<div class="wrap">
<header class="bar"><a class="brand" href="./"><img src="{icon}" alt="" width="36" height="36">Guhs</a><span class="sp"></span>
<span class="seg" role="group" aria-label="Taal / Language"><button type="button" data-setlang="nl" aria-pressed="true">NL</button><button type="button" data-setlang="en" aria-pressed="false">EN</button></span>
<button class="icon-btn" id="theme" type="button" aria-label="Thema / Theme">&#9790;</button></header>
<main>
<section class="hero">
<div>
<span class="kicker">&#9733; {_t("De officiële Guhs-server", "A Minecraft mod")}</span>
<h1>{_t("Lieve vadsige guhs, <em>dag en nacht</em> online.", "Lieve vadsige guhs, in <em>your own world</em>.")}</h1>
<p class="pitch">{_t("Tem je eigen guh, stap samen door het kaasknabbelportaal de Guhmensie in en speel minigames met andere guhvrienden. Njeg!",
                     "Tame your own guh, step through the cheese nibble portal into the Guhmension and play minigames with your guh friends. Nyeg!")}</p>
<div class="join" lang="nl">
<span class="addr"><code id="addr">{ADDRESS}</code><button id="copy" type="button" data-addr="{ADDRESS}">{_t("Kopieer", "Copy")}</button></span>
{status}
</div>
<p class="small" lang="nl">{small} <a href="{w}server.html">Hoe speel ik mee?</a></p>
<p class="small" lang="en">Minecraft {MC_VERSION} with Guhs {GUHS_VERSION}. New? Start with the <a href="{w}aan-de-slag.html">getting-started guide</a>.</p>
</div>
<div class="art">{self.img(hero_img, "Een guh") if hero_img else ""}<span class="bubble" aria-hidden="true">njeg!</span></div>
</section>
<nav class="tiles" aria-label="{'Guhs'}">{tile_html}</nav>
<ul class="facts">{facts_html}</ul>
<div class="links">{links}</div>
</main>
<footer>{_t("Guhs is een fanmod en hoort niet bij Mojang of Microsoft. Guh-model door Lieke, de rest door Juiced.",
            "Guhs is a fan-made mod and is not affiliated with Mojang or Microsoft. Guh model by Lieke, everything else by Juiced.")}</footer>
</div>
<script>{JS.strip()}</script>
</body>
</html>
"""

    def not_found(self):
        """GitHub Pages serves the root 404.html for every miss. Old wiki links (the wiki used to live at the root) are sent on to
        wiki/; a miss inside wiki/ shows the wiki's own 404 page (which finds its own base)."""
        w = self.wiki
        js = ("(function(){var p=location.pathname,w='/" + w + "';"
              "if(p.indexOf(w)!==0){location.replace(w+p.replace(/^\\/+/,'')+location.search+location.hash);return;}"
              "if(window.fetch){fetch(w+'404.html').then(function(r){if(!r.ok)throw 0;return r.text();})"
              ".then(function(h){document.open();document.write(h);document.close();}).catch(function(){});}})();")
        return f"""<!doctype html>
<html lang="nl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Guhs - even zoeken...</title>
<script>{js}</script>
<style>body{{margin:0;font:17px/1.6 system-ui,sans-serif;background:#fff7fa;color:#3a1c30;display:grid;place-items:center;min-height:100vh;text-align:center;padding:16px}}
a{{color:#b8325f;font-weight:700}}</style>
</head>
<body>
<div><p>Njeg! Deze pagina is verhuisd of bestaat niet. / This page moved or doesn't exist.</p>
<p><a href="/">guhs.nl</a> &middot; <a href="/{w}">Wiki</a></p></div>
</body>
</html>
"""

    def write(self, root):
        os.makedirs(root, exist_ok=True)

        def put(name, text):
            with open(os.path.join(root, name), "w", encoding="utf-8", newline="\n") as f:
                f.write(text)

        put("index.html", self.html())
        put("404.html", self.not_found())
        put("CNAME", DOMAIN + "\n")
        put(".nojekyll", "")
        urls = [self.site_url, self.site_url + self.wiki]
        put("sitemap.xml", '<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n'
            + "".join(f"<url><loc>{xml_escape(u)}</loc></url>\n" for u in urls) + "</urlset>\n")
        put("robots.txt", f"User-agent: *\nAllow: /\nSitemap: {self.site_url}sitemap.xml\nSitemap: {self.site_url}{self.wiki}sitemap.xml\n")
        return json.dumps(urls)
