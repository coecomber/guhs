"""The stylesheet and the script of the site (written to docs/site/assets/)."""

CSS = r"""
:root{
  --paper:#fff7fa; --card:#ffffff; --ink:#3a1c30; --muted:#8a6479; --line:#f0d9e3; --line2:#e8c7d6;
  --rasp:#d9467a; --rasp-ink:#b8325f; --rasp-soft:#fde3ec; --cheese:#e9a21c; --cheese-soft:#fff1d2; --lilac:#b8a1d4;
  --slot:#8b8b8b; --slot-in:#c6c6c6; --stage:#fbeef3; --shadow:0 1px 2px rgba(80,20,50,.06),0 6px 18px rgba(80,20,50,.07);
  --display:"Fredoka","Baloo 2","Trebuchet MS",system-ui,sans-serif;
  --body:"Nunito Sans","Segoe UI",system-ui,-apple-system,sans-serif;
  --mono:"JetBrains Mono",ui-monospace,Consolas,monospace;
  color-scheme:light;
}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){
  color-scheme:dark;--paper:#1d1219;--card:#291a23;--ink:#f7e7ef;--muted:#bb97ab;--line:#3f2836;--line2:#553546;
  --rasp:#f06a9a;--rasp-ink:#ff8db5;--rasp-soft:#3a1d2a;--cheese:#f2b544;--cheese-soft:#35291a;--lilac:#c7b3e0;--stage:#24161f;
  --slot:#555;--slot-in:#777;--shadow:0 1px 2px rgba(0,0,0,.3),0 6px 18px rgba(0,0,0,.25);}}
:root[data-theme="dark"]{color-scheme:dark;--paper:#1d1219;--card:#291a23;--ink:#f7e7ef;--muted:#bb97ab;--line:#3f2836;--line2:#553546;
  --rasp:#f06a9a;--rasp-ink:#ff8db5;--rasp-soft:#3a1d2a;--cheese:#f2b544;--cheese-soft:#35291a;--lilac:#c7b3e0;--stage:#24161f;
  --slot:#555;--slot-in:#777;--shadow:0 1px 2px rgba(0,0,0,.3),0 6px 18px rgba(0,0,0,.25);}
*{box-sizing:border-box}
html{-webkit-text-size-adjust:100%}
body{margin:0;background:var(--paper);color:var(--ink);font:16px/1.6 var(--body)}
html[data-lang="en"] [lang="nl"]:not(html),html[data-lang="nl"] [lang="en"]:not(html){display:none!important}
a{color:var(--rasp-ink);text-decoration-thickness:1px;text-underline-offset:2px}
a:hover{color:var(--rasp)}
a.auto{color:inherit;text-decoration:underline dotted var(--rasp);text-decoration-thickness:2px}
a.auto:hover{color:var(--rasp-ink)}
img{max-width:100%;height:auto}
:focus-visible{outline:3px solid var(--cheese);outline-offset:2px;border-radius:4px}
.skip{position:absolute;left:-9999px}.skip:focus{left:12px;top:8px;z-index:50;background:var(--card);padding:6px 10px}
/* --- header --- */
.top{position:sticky;top:0;z-index:20;background:color-mix(in srgb,var(--paper) 86%,transparent);backdrop-filter:blur(10px);
  border-bottom:1px solid var(--line)}
.top .in{max-width:1320px;margin:0 auto;display:flex;align-items:center;gap:12px;padding:8px 16px}
.brand{display:flex;align-items:center;gap:8px;text-decoration:none;color:var(--rasp);font:700 22px/1 var(--display);letter-spacing:.3px;white-space:nowrap}
.brand img{width:32px;height:32px;image-rendering:pixelated}
.ver{font:600 12px/1 var(--mono);color:var(--muted);border:1px solid var(--line2);border-radius:999px;padding:3px 7px}
.icon-btn.menu-btn{display:none}
.search{position:relative;flex:1;min-width:0;max-width:520px;margin-left:auto}
.search input{width:100%;min-width:0;font:15px var(--body);color:var(--ink);background:var(--card);border:1.5px solid var(--line2);border-radius:999px;
  padding:8px 14px 8px 36px}
.search input:focus{border-color:var(--rasp);outline:none;box-shadow:0 0 0 3px var(--rasp-soft)}
.search .mag{position:absolute;left:12px;top:50%;width:14px;height:14px;transform:translateY(-50%);border:2px solid var(--muted);border-radius:50%}
.search .mag::after{content:"";position:absolute;width:6px;height:2px;background:var(--muted);right:-6px;bottom:-3px;transform:rotate(45deg)}
.results{position:absolute;left:0;right:0;top:calc(100% + 6px);background:var(--card);border:1px solid var(--line2);border-radius:14px;
  box-shadow:var(--shadow);max-height:70vh;overflow:auto;padding:6px;display:none}
.results.open{display:block}
.results a{display:flex;align-items:center;gap:10px;padding:6px 8px;border-radius:10px;text-decoration:none;color:var(--ink)}
.results a[aria-selected="true"],.results a:hover{background:var(--rasp-soft)}
.results img{width:36px;height:36px;object-fit:contain;flex:none}
.results .rt{font-weight:700}.results .rc{font-size:12px;color:var(--muted)}
.results .none{padding:10px;color:var(--muted)}
.tools{display:flex;gap:8px;align-items:center}
.seg{display:inline-flex;border:1.5px solid var(--rasp);border-radius:999px;overflow:hidden}
.seg button{border:0;background:transparent;color:var(--rasp-ink);font:700 13px/1 var(--body);padding:7px 11px;cursor:pointer}
.seg button[aria-pressed="true"]{background:var(--rasp);color:#fff}
.icon-btn{border:1.5px solid var(--line2);background:var(--card);color:var(--ink);border-radius:999px;width:36px;height:34px;cursor:pointer;
  font:16px/1 var(--body);display:inline-flex;align-items:center;justify-content:center}
/* --- layout --- */
.shell{max-width:1320px;margin:0 auto;display:grid;grid-template-columns:230px minmax(0,1fr);gap:28px;padding:18px 16px 40px}
.side{position:sticky;top:64px;align-self:start;max-height:calc(100vh - 80px);overflow:auto;font-size:15px;padding-right:4px}
.side h2{font:600 13px var(--body);text-transform:uppercase;letter-spacing:.08em;color:var(--muted);margin:14px 8px 6px}
.side ul{list-style:none;margin:0;padding:0}
.side a{display:flex;align-items:center;gap:9px;padding:5px 8px;border-radius:10px;text-decoration:none;color:var(--ink)}
.side a:hover{background:var(--rasp-soft)}
.side a[aria-current="page"],.side a.here{background:var(--rasp-soft);color:var(--rasp-ink);font-weight:700}
.side img{width:24px;height:24px;object-fit:contain}
.side .n{margin-left:auto;font:600 11px var(--mono);color:var(--muted)}
main{min-width:0}
.crumbs{font-size:14px;color:var(--muted);margin:0 0 8px}
.crumbs a{color:var(--muted)}
.crumbs span[aria-hidden]{margin:0 6px}
h1{font:600 clamp(30px,4.4vw,44px)/1.1 var(--display);margin:0 0 6px;text-wrap:balance}
h1 .en-sub{display:block;font:500 17px/1.3 var(--body);color:var(--muted);margin-top:4px}
h2{font:600 24px/1.25 var(--display);margin:28px 0 10px;scroll-margin-top:76px}
h3{font:600 19px/1.3 var(--display);margin:20px 0 6px}
.kind{display:inline-block;font:700 12px var(--body);letter-spacing:.04em;text-transform:uppercase;color:var(--rasp-ink);background:var(--rasp-soft);
  border-radius:999px;padding:3px 10px;margin-bottom:10px}
.lead{font-size:18px;max-width:72ch}
.game-text{font-style:italic}
.muted{color:var(--muted)}
.note{border-left:4px solid var(--cheese);background:var(--cheese-soft);padding:8px 14px;border-radius:0 12px 12px 0;margin:10px 0;font-size:15px}
.article{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:28px;align-items:start}
.article>.content{min-width:0}
.content p,.content li{max-width:78ch}
/* --- infobox --- */
.infobox{position:sticky;top:72px;background:var(--card);border:1px solid var(--line);border-radius:18px;box-shadow:var(--shadow);overflow:hidden;font-size:14.5px}
.infobox .stage{min-height:170px;padding:14px}
.infobox .stage img{max-height:220px;width:auto}
.infobox .ititle{font:600 18px var(--display);text-align:center;padding:8px 12px 0}
.infobox dl{margin:0;padding:8px 14px 14px}
.infobox dl div{display:grid;grid-template-columns:minmax(0,110px) minmax(0,1fr);gap:8px;padding:6px 0;border-top:1px dashed var(--line)}
.infobox dl div:first-child{border-top:0}
.infobox dt{color:var(--muted);font-weight:700;font-size:12.5px;text-transform:uppercase;letter-spacing:.03em;padding-top:1px}
.infobox dd{margin:0;overflow-wrap:anywhere}
.stage{background:radial-gradient(circle at 50% 60%,var(--stage),transparent 70%);border-radius:14px;display:flex;align-items:center;justify-content:center}
img[data-pix]{image-rendering:pixelated;width:128px;height:auto}
.px{image-rendering:pixelated;width:32px;height:32px;vertical-align:middle}
.swatch{display:inline-block;width:14px;height:14px;border-radius:4px;vertical-align:-2px;margin:0 4px 0 6px;border:1px solid rgba(0,0,0,.2)}
.swatch:first-child{margin-left:0}
/* --- knowledge-base blocks --- */
.kb-entry{display:grid;grid-template-columns:220px minmax(0,1fr);gap:18px;padding:14px 0;border-top:1px dashed var(--line)}
.kb-entry.wide{grid-template-columns:minmax(0,1fr)}
.kb-entry.wide .stage img{max-height:420px}
.kb-entry .stage{min-height:150px;align-self:start}
.kb-entry .stage img{max-height:240px;width:auto}
.kb-entry h2{margin-top:0}
.kb-text>p:first-child{margin-top:0}
.stats{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:6px 14px;margin:10px 0 0;padding:10px 12px;background:var(--rasp-soft);border-radius:12px;font-size:14px}
.stats div{display:flex;flex-direction:column}.stats dt{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.06em}.stats dd{margin:0;font-weight:700}
.gallery{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:14px;margin:10px 0}
.gallery figure{margin:0;border:1px solid var(--line);border-radius:14px;padding:10px;background:var(--card);display:flex;flex-direction:column;gap:8px}
.gallery .stage{min-height:150px}
.gallery .stage img{max-height:220px;width:auto}
.gallery figcaption h3{font-size:17px;margin:0}
.gallery figcaption p{margin:.2em 0 0;font-size:15px}
.gallery img.shot,.kb-entry img.shot,.pics img.shot{border-radius:10px;box-shadow:0 2px 10px rgba(58,28,48,.18)}
.rarity{display:inline-block;font:700 11.5px var(--body);padding:1px 8px;border-radius:999px;background:var(--cheese-soft);margin-left:6px;vertical-align:2px}
.pics{display:flex;flex-wrap:wrap;gap:10px;margin:8px 0}
.pics figure{margin:0;background:var(--stage);border-radius:12px;padding:8px;display:flex;align-items:center}
.pics img{max-height:180px;width:auto}
dl.rowbox{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:6px 14px;background:var(--card);border:1px solid var(--line);border-radius:12px;padding:10px 12px}
dl.rowbox dt{font-size:12px;text-transform:uppercase;color:var(--muted);letter-spacing:.05em}dl.rowbox dd{margin:0}
/* --- tables --- */
.tscroll{overflow-x:auto;-webkit-overflow-scrolling:touch}
table{border-collapse:collapse;width:100%;font-size:15px}
th,td{text-align:left;padding:7px 10px;border-bottom:1px solid var(--line);vertical-align:top}
th{font:600 13.5px var(--display);color:var(--muted);white-space:nowrap}
td.num,th.num{text-align:right;font-variant-numeric:tabular-nums}
table.data tbody tr:nth-child(even){background:color-mix(in srgb,var(--rasp-soft) 35%,transparent)}
.chips-list{list-style:none;padding:0;margin:8px 0;display:flex;flex-wrap:wrap;gap:6px}
.chips-list li{background:var(--card);border:1px solid var(--line);border-radius:999px;padding:3px 12px 3px 6px;font-size:14.5px;display:flex;align-items:center;gap:4px}
.chips-list li.sub{background:transparent;border:0;font-weight:700;color:var(--muted);padding-left:0;width:100%}
.chips-list .px{width:24px;height:24px}
.tagref{font:13px var(--mono);color:var(--muted)}
/* --- recipes --- */
.recipes{display:flex;flex-wrap:wrap;gap:14px}
.recipe{display:flex;align-items:center;gap:10px;flex-wrap:wrap;background:var(--card);border:1px solid var(--line);border-radius:14px;padding:10px 12px}
.craft{display:grid;grid-template-columns:repeat(3,40px);gap:2px;background:var(--slot);padding:3px;border-radius:4px}
.inputs{display:flex;gap:4px}
.slot{width:40px;height:40px;background:var(--slot-in);display:flex;align-items:center;justify-content:center;position:relative;
  box-shadow:inset 2px 2px 0 rgba(0,0,0,.25),inset -2px -2px 0 rgba(255,255,255,.4)}
.slot a{display:flex}
.slot.big{width:52px;height:52px}
.slot .count{position:absolute;right:3px;bottom:0;color:#fff;text-shadow:2px 2px 0 #3f3f3f;font:700 15px var(--mono)}
.slot-txt{font:700 10px var(--mono);color:#333}
.arrow{width:28px;height:16px;background:var(--muted);clip-path:polygon(0 35%,60% 35%,60% 0,100% 50%,60% 100%,60% 65%,0 65%)}
.tag{font-size:12px;color:var(--muted);border:1px solid var(--line2);border-radius:999px;padding:1px 8px}
ol.steps{padding-left:1.4em}ol.steps li{margin:0 0 10px}
.task{margin-top:4px;font-size:14px}
details.book{border:1px solid var(--line);border-radius:12px;padding:8px 12px;margin:8px 0;background:var(--card)}
details.book summary{cursor:pointer;font-weight:700}
code,kbd{font-family:var(--mono);font-size:.88em;background:var(--rasp-soft);padding:1px 6px;border-radius:6px}
.cmd{display:flex;gap:8px;align-items:center;margin:6px 0;flex-wrap:wrap}
.cmd code{flex:1;min-width:0;overflow-x:auto;white-space:nowrap;padding:6px 10px}
.ghost{border:1.5px solid var(--line2);background:var(--card);color:var(--ink);border-radius:999px;font:600 13px/1 var(--body);padding:7px 12px;cursor:pointer}
/* --- related cards --- */
.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:10px;margin:8px 0}
.cards a{display:flex;align-items:center;gap:10px;padding:8px;border:1px solid var(--line);border-radius:12px;background:var(--card);text-decoration:none;color:var(--ink);font-weight:700;font-size:14.5px}
.cards a:hover{border-color:var(--rasp)}
.cards img{width:40px;height:40px;object-fit:contain;flex:none}
.cards small{display:block;font-weight:400;color:var(--muted);font-size:12px}
.backlinks{font-size:14.5px}
.backlinks h3{font-size:15px;margin:10px 0 4px;color:var(--muted)}
/* --- category lists --- */
.filters{display:flex;gap:8px;flex-wrap:wrap;align-items:center;margin:14px 0}
.filters input{font:15px var(--body);color:var(--ink);background:var(--card);border:1.5px solid var(--line2);border-radius:999px;padding:7px 14px;min-width:220px;flex:1;max-width:360px}
.chip{border:1.5px solid var(--line2);background:var(--card);color:var(--ink);border-radius:999px;font:600 13px var(--body);padding:5px 11px;cursor:pointer}
.chip[aria-pressed="true"]{background:var(--rasp);border-color:var(--rasp);color:#fff}
table.list td{vertical-align:middle}
table.list th[data-sort]{cursor:pointer;user-select:none}
table.list th[data-sort]::after{content:" \2195";opacity:.4}
table.list th[aria-sort="ascending"]::after{content:" \2191";opacity:1}
table.list th[aria-sort="descending"]::after{content:" \2193";opacity:1}
table.list td.th{width:56px}
table.list td.th img{width:48px;height:48px;object-fit:contain;display:block}
table.list td.name a{font-weight:700}
table.list td.sum{color:var(--muted);font-size:14px;min-width:220px}
.count-line{color:var(--muted);font-size:14px}
/* --- home --- */
.hero{display:grid;grid-template-columns:minmax(0,1.1fr) minmax(0,1fr);gap:24px;align-items:center;padding:10px 0 6px}
.hero h1{font-size:clamp(44px,7vw,78px)}
.hero h1 em{font-style:normal;color:var(--rasp)}
.tagline{font:500 20px/1.4 var(--display);color:var(--muted);margin:0 0 14px}
.hero-art{display:flex;justify-content:center;position:relative}
.hero-art img{width:min(100%,420px);filter:drop-shadow(0 18px 18px rgba(80,20,50,.18))}
.pills{display:flex;flex-wrap:wrap;gap:8px;margin:0;padding:0;list-style:none}
.pills li{background:var(--card);border:1px solid var(--line);border-radius:999px;padding:4px 12px;font-size:14px}
.pills b{font-family:var(--mono)}
.dl{display:flex;flex-wrap:wrap;gap:10px;margin:12px 0}
.dl .btn{display:inline-flex;align-items:center;gap:8px;border-radius:14px;padding:10px 16px;font:700 15px var(--body);text-decoration:none;
  background:var(--rasp);color:#fff;border:0}
.dl .btn.soon{background:var(--card);color:var(--muted);border:1.5px dashed var(--line2);cursor:default}
.tiles{display:grid;grid-template-columns:repeat(auto-fill,minmax(190px,1fr));gap:12px;margin:12px 0}
.tiles a{display:flex;flex-direction:column;gap:6px;padding:12px;border:1px solid var(--line);border-radius:16px;background:var(--card);
  text-decoration:none;color:var(--ink);box-shadow:var(--shadow);transition:transform .15s}
.tiles a:hover{transform:translateY(-2px);border-color:var(--rasp)}
.tiles .stage{height:110px}
.tiles img{max-height:96px;width:auto}
.tiles b{font:600 18px var(--display)}
.tiles small{color:var(--muted)}
.box{background:var(--card);border:1px solid var(--line);border-radius:18px;padding:6px 20px 14px;box-shadow:var(--shadow);margin:18px 0}
.fact{font-style:italic}
footer.site{border-top:1px solid var(--line);color:var(--muted);font-size:14px}
footer.site .in{max-width:1320px;margin:0 auto;padding:18px 16px 40px}
/* --- the "Aan de slag" guide --- */
.article.guide{grid-template-columns:minmax(0,1fr)}
.ghero{display:grid;grid-template-columns:minmax(0,1fr) 220px;gap:20px;align-items:center;background:linear-gradient(135deg,var(--rasp-soft),var(--cheese-soft));
  border:1px solid var(--line);border-radius:22px;padding:14px 24px 18px;margin:8px 0 6px}
.ghero p{font-size:18px;max-width:70ch}
.ghero .muted{font-size:15px;margin-bottom:6px}
.ghero-art{display:flex;justify-content:center}
.ghero-art img{max-height:190px;width:auto;filter:drop-shadow(0 12px 12px rgba(80,20,50,.16))}
.gtoc{display:flex;flex-wrap:wrap;gap:8px;list-style:none;padding:0;margin:0}
.gtoc a{display:inline-flex;align-items:center;gap:7px;padding:5px 12px 5px 5px;border-radius:999px;background:var(--card);border:1px solid var(--line2);
  text-decoration:none;color:var(--ink);font-weight:700;font-size:14px}
.gtoc a:hover{border-color:var(--rasp);color:var(--rasp-ink)}
.gtoc b{display:inline-grid;place-items:center;width:24px;height:24px;border-radius:50%;background:var(--rasp);color:#fff;font:700 13px/1 var(--display)}
.gstep{display:grid;grid-template-columns:46px minmax(0,1fr);gap:16px;align-items:start;background:var(--card);border:1px solid var(--line);
  border-radius:20px;padding:16px 22px 10px 16px;box-shadow:var(--shadow);margin:18px 0;scroll-margin-top:76px}
.gstep.has-pic{grid-template-columns:46px minmax(0,1fr) 200px}
.gstep h2{margin:6px 0 8px}
.gstep h3{font:600 18px var(--display);margin:16px 0 4px}
.gnum{width:44px;height:44px;border-radius:50%;background:var(--rasp);color:#fff;font:700 22px/44px var(--display);text-align:center;box-shadow:0 3px 0 var(--rasp-ink)}
.gpic{min-height:170px;padding:10px;position:sticky;top:84px}
.gpic img{max-height:170px;width:auto}
.gtip{border-left:4px solid var(--rasp);background:var(--rasp-soft);padding:8px 14px;border-radius:0 12px 12px 0;margin:12px 0;font-size:15.5px;max-width:78ch}
.gtip>b{color:var(--rasp-ink);margin-right:4px}
.goals{display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:12px;margin:10px 0 8px}
.goal{border:1px solid var(--line);border-radius:14px;padding:10px 14px;background:var(--paper)}
.goal h3{margin:2px 0 4px;display:flex;align-items:center;gap:8px;font:600 17px/1.25 var(--display)}
.goal p{font-size:15px;margin:0 0 6px}
.goal .px,.gicon{width:32px;height:32px;object-fit:contain;flex:none}
.gend{text-align:center;background:var(--card);border:1px dashed var(--line2);border-radius:20px;padding:10px 20px 16px;margin:22px 0}
.gend p{margin-left:auto;margin-right:auto}
.gbtn{display:inline-flex;align-items:center;border-radius:14px;padding:9px 16px;font:700 15px var(--body);text-decoration:none;background:var(--rasp);color:#fff;margin:4px}
.gbtn:hover{color:#fff;filter:brightness(1.05)}
.gbtn.alt{background:var(--card);color:var(--rasp-ink);border:1.5px solid var(--line2)}
.start-cta{display:grid;grid-template-columns:130px minmax(0,1fr) auto;gap:20px;align-items:center;text-decoration:none;color:#fff;
  background:linear-gradient(120deg,var(--rasp) 0%,#e8739f 60%,var(--cheese) 140%);border-radius:24px;padding:16px 26px;margin:16px 0 8px;
  box-shadow:var(--shadow);transition:transform .15s}
.start-cta:hover{color:#fff;transform:translateY(-2px)}
.cta-art{display:flex;justify-content:center;background:rgba(255,255,255,.22);border-radius:18px;padding:6px}
.cta-art img{max-height:110px;width:auto}
.cta-txt{display:flex;flex-direction:column;gap:2px}
.cta-txt small{font:700 13px var(--body);text-transform:uppercase;letter-spacing:.08em;opacity:.9}
.cta-txt b{font:700 clamp(28px,4vw,40px)/1.05 var(--display)}
.cta-txt>span{font-size:16px;opacity:.95;max-width:62ch}
.start-cta .go{background:#fff;color:var(--rasp-ink);border-radius:999px;padding:10px 18px;font-weight:700;white-space:nowrap}
.side a[href$="aan-de-slag.html"]{font-weight:700;color:var(--rasp-ink)}
.side a[href$="server.html"]{font-weight:700;color:var(--rasp-ink)}
.start-cta.srv-cta{background:linear-gradient(120deg,#7a5bd6 0%,#b06fd0 55%,var(--rasp) 130%);margin-top:10px}
.gbody code{font-size:.92em;background:var(--paper);border:1px solid var(--line);border-radius:6px;padding:1px 5px;overflow-wrap:anywhere}
@media (max-width:860px){.gstep,.gstep.has-pic{grid-template-columns:36px minmax(0,1fr);padding:14px 14px 8px 12px;gap:10px}
  .gnum{width:34px;height:34px;font-size:18px;line-height:34px}.gpic{grid-column:2;grid-row:1;position:static;min-height:0}.gpic img{max-height:120px}
  .gstep.has-pic .gbody{grid-row:2;grid-column:1/-1}
  .ghero{grid-template-columns:minmax(0,1fr);padding:12px 16px}.ghero-art{display:none}
  .start-cta{grid-template-columns:72px minmax(0,1fr);padding:14px 16px;gap:14px}.cta-art img{max-height:64px}.start-cta .go{grid-column:1/-1;justify-self:start}}
/* --- small screens --- */
@media (max-width:1060px){.article{grid-template-columns:minmax(0,1fr)}.infobox{position:static;order:-1;max-width:520px}}
@media (max-width:860px){
  .shell{grid-template-columns:minmax(0,1fr)}
  .icon-btn.menu-btn{display:inline-flex}
  .side{display:none;position:fixed;inset:57px 0 0 0;max-height:none;background:var(--paper);z-index:15;padding:10px 16px 40px}
  body.nav-open .side{display:block}
  .hero{grid-template-columns:minmax(0,1fr)}
  .kb-entry{grid-template-columns:minmax(0,1fr)}
  .brand .word{display:none}
  .ver{display:none}
  table.list td.sum{display:none}
}
@media (max-width:520px){.tools .seg button{padding:7px 8px}.search input{padding-left:30px}.top .in{gap:8px;padding:8px 10px}}
@media (prefers-reduced-motion:reduce){.tiles a{transition:none}}
@media print{.top,.side,.results,.filters{display:none}.shell{display:block}.article{display:block}.infobox{position:static}}
"""

JS = r"""
(function(){
  var html=document.documentElement, root=document.body.getAttribute('data-root')||'';
  function store(k,v){try{localStorage.setItem(k,v);}catch(e){}}
  // language
  function setLang(l){html.setAttribute('data-lang',l);html.lang=l;store('guhs-wiki-lang',l);
    document.querySelectorAll('[data-setlang]').forEach(function(b){b.setAttribute('aria-pressed',String(b.getAttribute('data-setlang')===l));});
    var s=document.getElementById('q');if(s){s.placeholder=s.getAttribute('data-ph-'+l)||s.placeholder;}}
  document.querySelectorAll('[data-setlang]').forEach(function(b){b.addEventListener('click',function(){setLang(b.getAttribute('data-setlang'));});});
  setLang(html.getAttribute('data-lang')||'nl');
  // theme
  var tb=document.getElementById('theme');
  function isDark(){var t=html.getAttribute('data-theme');return t?t==='dark':matchMedia('(prefers-color-scheme: dark)').matches;}
  function showTheme(){if(tb){tb.textContent=isDark()?'☀':'☾';tb.setAttribute('aria-label',isDark()?'Licht thema':'Donker thema');}}
  if(tb){tb.addEventListener('click',function(){var t=isDark()?'light':'dark';html.setAttribute('data-theme',t);store('guhs-wiki-theme',t);showTheme();});}
  showTheme();
  // mobile menu
  var mb=document.getElementById('menu');
  if(mb){mb.addEventListener('click',function(){var o=document.body.classList.toggle('nav-open');mb.setAttribute('aria-expanded',String(o));});}
  // copy buttons
  document.querySelectorAll('.copy').forEach(function(b){b.addEventListener('click',function(){
    var code=b.previousElementSibling;var txt=code.textContent;
    try{navigator.clipboard.writeText(txt).then(function(){var o=b.textContent;b.textContent='✓';setTimeout(function(){b.textContent=o;},1200);});}catch(e){}});});
  // search
  var q=document.getElementById('q'), box=document.getElementById('results'), idx=null, loading=false, sel=-1;
  function fold(s){return (s||'').normalize('NFKD').replace(/[̀-ͯ]/g,'').toLowerCase();}
  function load(cb){if(idx){cb();return;}if(loading)return;loading=true;var s=document.createElement('script');s.src=root+'assets/search-index.js';
    s.onload=function(){idx=(window.GUHS_INDEX||[]).map(function(e){e.f=fold(e.t+' '+(e.e||''));e.k2=fold(e.k||'');e.s2=fold(e.s||'');return e;});cb();};
    document.head.appendChild(s);}
  function esc(s){return String(s).replace(/[&<>"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
  function run(){var v=fold(q.value.trim());sel=-1;if(!v){box.classList.remove('open');box.innerHTML='';return;}
    var words=v.split(/\s+/),out=[];
    idx.forEach(function(e){var sc=0,ok=true;words.forEach(function(w){var p=e.f.indexOf(w);
      if(p===0)sc+=100;else if(p>0)sc+=(e.f.charAt(p-1)===' '?60:40);else if(e.k2.indexOf(w)>=0)sc+=25;else if(e.s2.indexOf(w)>=0)sc+=8;else ok=false;});
      if(ok){sc-=e.t.length/10;if(e.p)sc+=e.p;out.push([sc,e]);}});
    out.sort(function(a,b){return b[0]-a[0];});out=out.slice(0,30);
    var lang=html.getAttribute('data-lang');
    if(!out.length){box.innerHTML='<div class="none">'+(lang==='en'?'Nothing found. Njeg!':'Niks gevonden. Njeg!')+'</div>';}
    else box.innerHTML=out.map(function(x,i){var e=x[1];var title=(lang==='en'&&e.e)?e.e:e.t;var cat=(lang==='en'&&e.ce)?e.ce:e.c;
      return '<a role="option" href="'+root+e.u+'" id="r'+i+'">'+(e.i?'<img src="'+root+e.i+'" alt="" loading="lazy">':'<span style="width:36px"></span>')+
        '<span><span class="rt">'+esc(title)+'</span><br><span class="rc">'+esc(cat)+'</span></span></a>';}).join('');
    box.classList.add('open');}
  if(q){
    q.addEventListener('focus',function(){load(function(){});});
    q.addEventListener('input',function(){load(run);});
    q.addEventListener('keydown',function(ev){var items=box.querySelectorAll('a');
      if(ev.key==='ArrowDown'||ev.key==='ArrowUp'){ev.preventDefault();if(!items.length)return;sel=(sel+(ev.key==='ArrowDown'?1:-1)+items.length)%items.length;
        items.forEach(function(a,i){a.setAttribute('aria-selected',String(i===sel));});items[sel].scrollIntoView({block:'nearest'});}
      else if(ev.key==='Enter'){var a=items[sel>=0?sel:0];if(a){ev.preventDefault();location.href=a.getAttribute('href');}}
      else if(ev.key==='Escape'){box.classList.remove('open');q.blur();}});
    document.addEventListener('click',function(ev){if(!ev.target.closest('.search'))box.classList.remove('open');});
    document.addEventListener('keydown',function(ev){if(ev.key==='/'&&document.activeElement.tagName!=='INPUT'){ev.preventDefault();q.focus();}});
    var pq=/[?&]q=([^&]*)/.exec(location.search);if(pq){q.value=decodeURIComponent(pq[1].replace(/\+/g,' '));load(run);}
  }
  // category tables: sort + filter
  document.querySelectorAll('table.list').forEach(function(tbl){
    var tbody=tbl.tBodies[0], rows=[].slice.call(tbody.rows), f=document.getElementById('filter'), chips=document.querySelectorAll('.chip[data-kind]'), kind='';
    var cnt=document.getElementById('shown');
    function apply(){var v=fold(f?f.value:'');var n=0;rows.forEach(function(r){var ok=(!v||fold(r.getAttribute('data-text')).indexOf(v)>=0)&&(!kind||r.getAttribute('data-kind')===kind);
      r.hidden=!ok;if(ok)n++;});if(cnt)cnt.textContent=n;}
    if(f)f.addEventListener('input',apply);
    chips.forEach(function(c){c.addEventListener('click',function(){var on=c.getAttribute('aria-pressed')!=='true';chips.forEach(function(x){x.setAttribute('aria-pressed','false');});
      c.setAttribute('aria-pressed',String(on));kind=on?c.getAttribute('data-kind'):'';apply();});});
    tbl.querySelectorAll('th[data-sort]').forEach(function(th,ci){th.addEventListener('click',function(){
      var col=[].indexOf.call(th.parentNode.children,th), asc=th.getAttribute('aria-sort')!=='ascending', num=th.getAttribute('data-sort')==='num';
      tbl.querySelectorAll('th').forEach(function(x){x.removeAttribute('aria-sort');});th.setAttribute('aria-sort',asc?'ascending':'descending');
      rows.sort(function(a,b){var x=a.cells[col].getAttribute('data-v')||a.cells[col].textContent,y=b.cells[col].getAttribute('data-v')||b.cells[col].textContent;
        var r=num?(parseFloat(x)||0)-(parseFloat(y)||0):fold(x).localeCompare(fold(y),'nl');return asc?r:-r;});
      rows.forEach(function(r){tbody.appendChild(r);});});});
  });
})();
"""

EARLY_JS = ("(function(){var d=document.documentElement,l=null,t=null;try{l=localStorage.getItem('guhs-wiki-lang');t=localStorage.getItem('guhs-wiki-theme');}catch(e){}"
            "var m=/[?&]lang=(en|nl)/.exec(location.search);if(m)l=m[1];m=/[?&]theme=(dark|light)/.exec(location.search);if(m)t=m[1];"
            "if(l==='en'||l==='nl'){d.setAttribute('data-lang',l);d.lang=l;}if(t==='dark'||t==='light')d.setAttribute('data-theme',t);})();")
