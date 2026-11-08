"""The stylesheet and scripts of guhs.nl/vadsrijk (on top of the wiki's own site.css: the same tokens, fonts and frame)."""

CSS = r"""
/* ---- Het Vadsrijk (S-SITE): on top of the wiki stylesheet ---- */
:root{--vr:#c2577f;--vr-goud:#c8955a;--vr-soft:#f8e4ea;--zone1:#e0559a;--zone2:#5aa9e6;--zone3:#e8a33a;--zone4:#2bb3a3;--zone5:#9b7be0;--zone6:#e07a5f;--ok:#2f9e5b;--uit:#c0392b}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--vr:#f08cb0;--vr-goud:#e6b67a;--vr-soft:#3a2230}}
:root[data-theme="dark"]{--vr:#f08cb0;--vr-goud:#e6b67a;--vr-soft:#3a2230}
.brand .word small{font:600 13px var(--body);color:var(--vr-goud);margin-left:6px;letter-spacing:.04em}
.vr-nav{display:flex;flex-wrap:wrap;gap:4px;max-width:1320px;margin:0 auto;padding:6px 16px 0}
.vr-nav a{padding:6px 12px;border-radius:999px;text-decoration:none;color:var(--ink);font:600 14.5px var(--body)}
.vr-nav a:hover{background:var(--rasp-soft)}
.vr-nav a[aria-current="page"]{background:var(--vr);color:#fff}
.vr-shell{max-width:1320px;margin:0 auto;padding:14px 16px 48px}
.vr-hero{position:relative;border-radius:22px;overflow:hidden;background:linear-gradient(135deg,#2a1830,#4a2440 55%,#7a3a52);color:#fff;
  padding:28px 26px;margin:6px 0 22px;box-shadow:var(--shadow)}
.vr-hero h1{color:#fff;margin:0 0 8px}
.vr-hero p{max-width:62ch;margin:0 0 12px;font-size:17.5px;color:#f6e6ee}
.vr-hero img.kop{display:block;width:100%;height:auto;border-radius:12px;margin:0 0 14px;image-rendering:auto}
.vr-hero .knoppen{display:flex;flex-wrap:wrap;gap:8px}
.knop{display:inline-flex;align-items:center;gap:6px;padding:9px 16px;border-radius:999px;background:var(--vr);color:#fff;text-decoration:none;font:700 15px var(--body);border:0;cursor:pointer}
.knop.licht{background:rgba(255,255,255,.14);color:#fff;border:1px solid rgba(255,255,255,.35)}
.knop:hover{filter:brightness(1.08);color:#fff}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:14px}
.kaartje{background:var(--card);border:1px solid var(--line);border-radius:16px;padding:14px 16px;box-shadow:var(--shadow);min-width:0}
.kaartje h3{margin:0 0 6px}
.kaartje p{margin:.3em 0}
a.kaartje{display:block;text-decoration:none;color:var(--ink)}
a.kaartje:hover{border-color:var(--vr)}
.zonekaart{border-top:6px solid var(--zk,var(--vr))}
.badge{display:inline-block;font:700 12px/1.6 var(--body);padding:0 9px;border-radius:999px;background:var(--rasp-soft);color:var(--rasp-ink);white-space:nowrap;vertical-align:1px}
.badge.lv{background:var(--cheese-soft);color:#7a5200}
:root[data-theme="dark"] .badge.lv{color:var(--cheese)}
.badge.z1{background:color-mix(in srgb,var(--zone1) 20%,transparent);color:var(--zone1)}
.badge.z2{background:color-mix(in srgb,var(--zone2) 22%,transparent);color:var(--zone2)}
.badge.z3{background:color-mix(in srgb,var(--zone3) 22%,transparent);color:#b0700c}
.badge.z4{background:color-mix(in srgb,var(--zone4) 22%,transparent);color:#1b7f74}
.badge.z5{background:color-mix(in srgb,var(--zone5) 22%,transparent);color:#6a4fc0}
.badge.z6{background:color-mix(in srgb,var(--zone6) 22%,transparent);color:#b0472c}
.voorbeeldbalk{background:#fff3c4;color:#5a4300;border-bottom:2px dashed #e0b400;font:600 14px/1.4 var(--body);padding:8px 16px;text-align:center}
:root[data-theme="dark"] .voorbeeldbalk{background:#3a3214;color:#ffe9a6}
.el{display:inline-block;font:700 12px/1.6 var(--body);padding:0 8px;border-radius:999px;color:#fff}
.el.roze{background:#e05aa8}.el.vuur{background:#e2572a}.el.natuur{background:#3fa83a}.el.water{background:#2f8fdf}.el.geen{background:#8a8a8a}
.zz{font-weight:700}
.zz.gewoon{color:#8f8f8f}.zz.ongewoon{color:#2fae2f}.zz.zeldzaam{color:#2f7fe0}.zz.episch{color:#9a3fe6}.zz.legendarisch{color:#e07d00}.zz.guhddelijk{color:#e0409f}
.icoon{width:32px;height:32px;image-rendering:pixelated;vertical-align:middle}
.icoon.groot{width:96px;height:96px}
.stappen{counter-reset:stap;list-style:none;padding:0;margin:8px 0}
.stappen li{counter-increment:stap;position:relative;padding:8px 10px 8px 44px;border-left:3px solid var(--line2);margin:0 0 6px}
.stappen li::before{content:counter(stap);position:absolute;left:8px;top:7px;width:26px;height:26px;border-radius:50%;background:var(--vr);color:#fff;
  font:700 13px/26px var(--body);text-align:center}
.stappen .waar{font-size:13.5px}
.dialoog{margin:6px 0;padding:0;list-style:none}
.dialoog li{margin:4px 0;padding:6px 10px;border-radius:12px;background:var(--stage);max-width:72ch}
.dialoog li.jij{background:var(--cheese-soft);margin-left:28px}
.dialoog li.verteller{font-style:italic;background:transparent;border:1px dashed var(--line2)}
.dialoog b{display:block;font-size:12.5px;color:var(--muted)}
details.spoiler{border:1.5px dashed var(--vr);border-radius:12px;padding:6px 12px;margin:10px 0;background:color-mix(in srgb,var(--vr-soft) 50%,transparent)}
details.spoiler>summary{cursor:pointer;font-weight:700;color:var(--vr)}
.zoneslot{display:inline-flex;align-items:center;gap:6px;font-size:14px}
.zoneslot select{font:600 14px var(--body);border-radius:999px;border:1.5px solid var(--line2);background:var(--card);color:var(--ink);padding:5px 10px}
.komma{list-style:none;padding:0;margin:0 0 1em}.komma li{display:inline}.komma li+li::before{content:", "}
.spoileruitleg{position:fixed;left:50%;bottom:18px;transform:translateX(-50%);z-index:2000;width:min(560px,calc(100vw - 32px));background:var(--card);color:var(--ink);border:2px solid var(--vr);border-radius:16px;box-shadow:0 10px 30px rgba(0,0,0,.25);padding:14px 16px;font:15px/1.45 var(--body)}
.spoileruitleg p{margin:0 0 10px}
.spoileruitleg .su-knoppen{display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap}
.spoileruitleg label{font-size:14px;color:var(--muted);cursor:pointer}
.spoileruitleg .knop{min-width:80px;cursor:pointer}
.verstopt-zone{border:1.5px dashed var(--line2);border-radius:14px;padding:10px 14px;color:var(--muted);margin:10px 0}
html[data-zonetot="1"] [data-zone="2"]:not(.verstopt-zone),html[data-zonetot="1"] [data-zone="3"]:not(.verstopt-zone),html[data-zonetot="1"] [data-zone="4"]:not(.verstopt-zone),html[data-zonetot="1"] [data-zone="5"]:not(.verstopt-zone),html[data-zonetot="1"] [data-zone="6"]:not(.verstopt-zone),html[data-zonetot="2"] [data-zone="3"]:not(.verstopt-zone),html[data-zonetot="2"] [data-zone="4"]:not(.verstopt-zone),html[data-zonetot="2"] [data-zone="5"]:not(.verstopt-zone),html[data-zonetot="2"] [data-zone="6"]:not(.verstopt-zone),html[data-zonetot="3"] [data-zone="4"]:not(.verstopt-zone),html[data-zonetot="3"] [data-zone="5"]:not(.verstopt-zone),html[data-zonetot="3"] [data-zone="6"]:not(.verstopt-zone),html[data-zonetot="4"] [data-zone="5"]:not(.verstopt-zone),html[data-zonetot="4"] [data-zone="6"]:not(.verstopt-zone),html[data-zonetot="5"] [data-zone="6"]:not(.verstopt-zone){display:none!important}
.verstopt-zone{display:none}
html[data-zonetot="1"] .verstopt-zone[data-zone="2"],html[data-zonetot="1"] .verstopt-zone[data-zone="3"],html[data-zonetot="1"] .verstopt-zone[data-zone="4"],html[data-zonetot="1"] .verstopt-zone[data-zone="5"],html[data-zonetot="1"] .verstopt-zone[data-zone="6"],html[data-zonetot="2"] .verstopt-zone[data-zone="3"],html[data-zonetot="2"] .verstopt-zone[data-zone="4"],html[data-zonetot="2"] .verstopt-zone[data-zone="5"],html[data-zonetot="2"] .verstopt-zone[data-zone="6"],html[data-zonetot="3"] .verstopt-zone[data-zone="4"],html[data-zonetot="3"] .verstopt-zone[data-zone="5"],html[data-zonetot="3"] .verstopt-zone[data-zone="6"],html[data-zonetot="4"] .verstopt-zone[data-zone="5"],html[data-zonetot="4"] .verstopt-zone[data-zone="6"],html[data-zonetot="5"] .verstopt-zone[data-zone="6"]{display:block}
.zonevak{border:1px solid var(--line);border-left:6px solid var(--zk,var(--vr));border-radius:14px;background:var(--card);margin:10px 0;box-shadow:var(--shadow)}
.zonevak>summary{cursor:pointer;padding:12px 16px;font:700 19px/1.3 var(--display,inherit);list-style:none}
.zonevak>summary::-webkit-details-marker{display:none}
.zonevak>summary::before{content:"\25B8";display:inline-block;width:1.1em;transition:transform .15s;color:var(--zk,var(--vr))}
.zonevak[open]>summary::before{transform:rotate(90deg)}
.zonevak .zv-in{padding:0 16px 14px}
img.render{display:block;image-rendering:auto}
img.render.groot{max-width:min(320px,100%);max-height:320px;margin:4px 0 14px;filter:drop-shadow(0 8px 10px rgba(60,40,80,.25));
  background:radial-gradient(ellipse at 50% 85%,color-mix(in srgb,var(--vr-soft) 80%,transparent),transparent 70%);border-radius:18px;padding:10px}
img.render.klein{max-width:100%;height:120px;object-fit:contain;margin:0 auto 6px;filter:drop-shadow(0 5px 6px rgba(60,40,80,.22))}
img.render.mini{width:34px;height:34px;object-fit:contain;vertical-align:middle;margin-right:6px;display:inline-block}
a.baasfoto{display:block;text-align:center}
td.tipcel{min-width:220px;font-size:14px}
.pager{display:flex;flex-wrap:wrap;gap:6px;align-items:center;margin:8px 0 14px}
.pager button{font:600 14px var(--body);border:1.5px solid var(--line2);border-radius:999px;background:var(--card);color:var(--ink);padding:4px 11px;cursor:pointer}
.pager button[aria-current="page"]{background:var(--vr);border-color:var(--vr);color:#fff}
.pager button:disabled{opacity:.4;cursor:default}
.pager .muted{margin-left:6px}
.filters{display:flex;flex-wrap:wrap;gap:8px;margin:8px 0 12px}
.filters input,.filters select{font:15px var(--body);border:1.5px solid var(--line2);border-radius:999px;background:var(--card);color:var(--ink);padding:7px 12px}
.filters input{flex:1;min-width:180px}
.leeg{color:var(--muted);font-style:italic}
.tw{display:grid;grid-template-columns:minmax(0,1fr) 320px;gap:24px;align-items:start}
.zij{position:sticky;top:72px;display:flex;flex-direction:column;gap:12px}
.feiten{margin:0;padding:0}
.feiten>div{display:grid;grid-template-columns:120px minmax(0,1fr);gap:8px;padding:6px 0;border-top:1px dashed var(--line);font-size:14.5px}
.feiten>div:first-child{border-top:0}
.feiten dt{color:var(--muted);font-weight:700;font-size:12.5px;text-transform:uppercase;letter-spacing:.03em}
.feiten dd{margin:0;overflow-wrap:anywhere}
.minikaart{display:block;width:100%;aspect-ratio:1/1;border-radius:12px;background:#0f1a24;border:1px solid var(--line);overflow:hidden;position:relative}
.minikaart img{width:100%;height:100%;object-fit:cover;image-rendering:pixelated}
.minikaart .stip{position:absolute;width:14px;height:14px;margin:-7px 0 0 -7px;border-radius:50%;background:var(--vr);border:2px solid #fff;box-shadow:0 0 0 3px rgba(0,0,0,.35)}
/* live */
.live-rij{display:flex;flex-wrap:wrap;gap:12px;align-items:stretch}
.live-tegel{flex:1 1 160px;background:var(--card);border:1px solid var(--line);border-radius:16px;padding:12px 14px;box-shadow:var(--shadow)}
.live-tegel .k{font-size:12.5px;text-transform:uppercase;letter-spacing:.05em;color:var(--muted);font-weight:700}
.live-tegel .v{font:600 28px/1.15 var(--display);font-variant-numeric:tabular-nums}
.live-tegel .s{font-size:13.5px;color:var(--muted)}
.stip-status{display:inline-block;width:11px;height:11px;border-radius:50%;background:var(--muted);margin-right:6px;vertical-align:1px}
.stip-status.aan{background:var(--ok);box-shadow:0 0 0 4px color-mix(in srgb,var(--ok) 25%,transparent)}
.stip-status.uit{background:var(--uit)}
.voorbeeld{font-size:13px;background:var(--cheese-soft);border-radius:999px;padding:2px 10px;display:inline-block;margin:4px 0}
.timers td.num{font-family:var(--mono)}
.hs-tabs{display:flex;flex-wrap:wrap;gap:6px;margin:6px 0 12px}
.hs-tabs button{border:1.5px solid var(--line2);background:var(--card);color:var(--ink);border-radius:999px;padding:6px 12px;font:600 14px var(--body);cursor:pointer}
.hs-tabs button[aria-pressed="true"]{background:var(--vr);border-color:var(--vr);color:#fff}
.hoofd{width:24px;height:24px;border-radius:4px;vertical-align:middle;margin-right:6px;image-rendering:pixelated;background:var(--stage)}
.hoofd.groot{width:96px;height:96px;border-radius:10px;margin:0}
.gear{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:8px}
.gear div{background:var(--card);border:1px solid var(--line);border-left:5px solid var(--rk,#999);border-radius:10px;padding:8px 10px;font-size:14.5px}
.balk{height:8px;background:var(--line);border-radius:99px;overflow:hidden}
.balk i{display:block;height:100%;background:var(--vr)}
/* map */
.kaart-pagina .vr-shell{max-width:none;padding:0}
.sr{position:absolute!important;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0);white-space:nowrap}
#kaart{height:calc(100vh - 112px);min-height:420px;background:#0d1620}
.leaflet-container{font:14px var(--body)}
.leaflet-tile{image-rendering:pixelated}
.kaart-paneel{position:absolute;z-index:500;top:10px;left:56px;right:10px;display:flex;flex-wrap:wrap;gap:8px;pointer-events:none}
.kaart-paneel>*{pointer-events:auto}
.kaart-zoek{position:relative;flex:0 1 320px}
.kaart-zoek input{width:100%;font:15px var(--body);padding:8px 12px;border-radius:999px;border:1.5px solid #3b4b5c;background:rgba(13,22,32,.88);color:#eaf2f8}
.kaart-zoek .res{position:absolute;top:calc(100% + 4px);left:0;right:0;background:#132131;border:1px solid #3b4b5c;border-radius:12px;max-height:50vh;overflow:auto;display:none}
.kaart-zoek .res.open{display:block}
.kaart-zoek .res a{display:block;padding:6px 10px;color:#eaf2f8;text-decoration:none}
.kaart-zoek .res a:hover,.kaart-zoek .res a.sel{background:#22384f}
.kaart-zoek .res small{color:#8fa6ba}
.kaart-info{background:rgba(13,22,32,.88);color:#cfe0ee;border:1px solid #3b4b5c;border-radius:999px;padding:6px 12px;font:600 13px var(--mono)}
.kaart-info select{background:transparent;color:#eaf2f8;border:0;font:600 13px var(--body)}
.leaflet-control-layers{font:14px var(--body)}
.vr-label{background:transparent;border:0;box-shadow:none;color:#fff;font:700 13px var(--body);text-shadow:0 0 3px #000,0 0 6px #000;white-space:nowrap}
.vr-label.zone{font:700 20px var(--display);letter-spacing:.05em;text-transform:uppercase;opacity:.92}
.vr-label.gebied{font:600 14px var(--body);opacity:.95}
.vr-label .lvr{display:block;font:700 11px var(--mono);color:#ffd98a;text-align:center}
.vr-pin{display:flex;align-items:center;justify-content:center;width:22px;height:22px;border-radius:50%;border:2px solid #fff;color:#fff;
  font:800 12px/1 var(--body);box-shadow:0 1px 4px rgba(0,0,0,.6)}
.vr-speler{display:inline-flex;align-items:center;gap:4px;background:rgba(255,255,255,.92);border:2px solid #c2577f;border-radius:999px;padding:1px 7px 1px 1px;
  font:700 12px/1.2 var(--body);color:#3a1d33;white-space:nowrap;box-shadow:0 2px 6px rgba(0,0,0,.3)}
.vr-speler img{border-radius:50%;image-rendering:pixelated}.vr-speler i{font-style:normal;font-weight:600;color:#7a4a6a}
.vr-pin.vliegt{animation:vrzweef 2.4s ease-in-out infinite}@keyframes vrzweef{50%{transform:translateY(-3px)}}
.kaart-info.spelers{margin-left:6px}
table.data td.num{white-space:nowrap}
.vr-pin.kampvuur{background:#e2572a}.vr-pin.uitkijk{background:#2b8f8f}.vr-pin.luchtschip{background:#6b5bd6}.vr-pin.portaal{background:#c2577f}
.vr-pin.kerker{background:#3d3d3d}.raidfotos{display:flex;flex-wrap:wrap;gap:8px}.raidfotos img{flex:1 1 160px;max-width:100%}
.vr-pin.geheim{background:#e0559a;border:2px dashed #fff}.vr-pin.raid{background:#6b2fa8;width:28px;height:28px}.vr-pin.baas{background:#b0182c;width:26px;height:26px}.vr-pin.quest{background:#e0a800;color:#2a1a00}
.vr-pin.questzij{background:#f2d36b;color:#2a1a00}.vr-pin.stad{background:#fff;color:#2a1830;border-color:#2a1830}.vr-pin.gondel,.vr-pin.lift{background:#55708a}
.vr-pin.karavaan{background:#8a5a2b;width:28px;height:28px}
.leaflet-popup-content{font:14px/1.45 var(--body)}
.leaflet-popup-content h4{margin:0 0 4px;font:600 16px var(--display)}
@media (max-width:900px){.tw{grid-template-columns:minmax(0,1fr)}.zij{position:static}.kaart-paneel{left:10px;top:56px}}
@media (max-width:760px){.top .in{flex-wrap:wrap;gap:8px}.top .ver,.zoneslot>span{display:none}.vr-nav{flex-wrap:nowrap;overflow-x:auto;-webkit-overflow-scrolling:touch;padding-bottom:6px}
  .vr-nav a{flex:none}.tools{flex-wrap:wrap}.brand .word small{display:none}}
@media (max-width:640px){.vr-hero{padding:20px 16px}.feiten>div{grid-template-columns:100px minmax(0,1fr)}#kaart{height:calc(100vh - 150px)}}
"""

# The page script: language, theme, zone switch, tables, live data, highscores, profile. (No framework.)
JS = r"""
(function(){
  var html=document.documentElement, body=document.body;
  function store(k,v){try{localStorage.setItem(k,v);}catch(e){}}
  function read(k){try{return localStorage.getItem(k);}catch(e){return null;}}
  // language
  function setLang(l){html.setAttribute('data-lang',l);html.lang=l;store('guhs-wiki-lang',l);
    document.querySelectorAll('[data-setlang]').forEach(function(b){b.setAttribute('aria-pressed',String(b.getAttribute('data-setlang')===l));});
    document.dispatchEvent(new Event('vr-lang'));}
  document.querySelectorAll('[data-setlang]').forEach(function(b){b.addEventListener('click',function(){setLang(b.getAttribute('data-setlang'));});});
  setLang(html.getAttribute('data-lang')||'nl');
  // theme
  var tb=document.getElementById('theme');
  function isDark(){var t=html.getAttribute('data-theme');return t?t==='dark':matchMedia('(prefers-color-scheme: dark)').matches;}
  function showTheme(){if(tb)tb.innerHTML=isDark()?'&#9788;':'&#9790;';}
  if(tb){tb.addEventListener('click',function(){var t=isDark()?'light':'dark';html.setAttribute('data-theme',t);store('guhs-wiki-theme',t);showTheme();});}
  showTheme();
  // spoiler switch: show everything up to zone X
  var zs=document.getElementById('zonetot');
  // W-SITE (user request): EVERY page load starts at zone 1 (tutorial + zone 1); a higher choice is never remembered
  function setZone(z){html.setAttribute('data-zonetot',z);if(zs)zs.value=z;document.dispatchEvent(new Event('vr-zone'));}
  try{localStorage.removeItem('guhs-vr-zonetot');}catch(e){}
  setZone('1');
  // W-SITE: a friendly note on load; "niet meer tonen" only hides the NOTE next time (the switch still starts at zone 1)
  var uitleg=null;
  function sluitUitleg(){if(uitleg){uitleg.remove();uitleg=null;}}
  if(zs){zs.addEventListener('change',function(){setZone(zs.value);sluitUitleg();});}
  if(read('guhs-vr-spoileruitleg-weg')!=='1'){
    uitleg=document.createElement('div');uitleg.className='spoileruitleg';uitleg.setAttribute('role','dialog');uitleg.setAttribute('aria-live','polite');
    uitleg.setAttribute('aria-label','Spoilers');
    uitleg.innerHTML='<p><span lang="nl">Alle informatie toont alleen de tutorial en de eerste zone. Spoilers of meer informatie? Kies rechtsboven bij <b>Toon tot</b> tot welke zone je alles wilt zien.</span>'+
      '<span lang="en">Everything shows only the tutorial and the first zone. Want spoilers or more information? Pick up to which zone you want to see everything at <b>Show up to</b> (top right).</span></p>'+
      '<div class="su-knoppen"><label><input type="checkbox" id="su-weg"> <span lang="nl">Niet meer tonen</span><span lang="en">Don&#39;t show again</span></label>'+
      '<button type="button" class="knop" id="su-ok">OK</button></div>';
    body.appendChild(uitleg);
    uitleg.querySelector('#su-ok').addEventListener('click',function(){if(uitleg.querySelector('#su-weg').checked)store('guhs-vr-spoileruitleg-weg','1');sluitUitleg();});}
  window.VR={lang:function(){return html.getAttribute('data-lang')||'nl';},zone:function(){return +(html.getAttribute('data-zonetot')||1);},
    t:function(en,nl){return (html.getAttribute('data-lang')==='en')?en:nl;},root:body.getAttribute('data-root')||'',
    live:body.getAttribute('data-live')||''};
  // filter tables: <input data-filter="#id">, <select data-filter-col="n" data-filter="#id">
  document.querySelectorAll('[data-filter]').forEach(function(el){
    var tab=document.querySelector(el.getAttribute('data-filter'));if(!tab)return;
    var controls=document.querySelectorAll('[data-filter="'+el.getAttribute('data-filter')+'"]');
    function run(){var rows=tab.tBodies[0].rows,n=0;
      for(var i=0;i<rows.length;i++){var r=rows[i],ok=true;
        controls.forEach(function(c){var v=(c.value||'').toLowerCase().trim();if(!v)return;
          var col=c.getAttribute('data-filter-col');var txt=(col?(r.cells[+col].getAttribute('data-v')||r.cells[+col].textContent):r.textContent).toLowerCase();
          if(col?txt.split('|').indexOf(v)<0&&txt!==v:txt.indexOf(v)<0)ok=false;});
        r._uit=!ok;r.hidden=!ok;if(ok)n++;}
      tab._p=0;var m=pageer(tab);
      var cnt=document.querySelector(el.getAttribute('data-filter')+'-n');if(cnt)cnt.textContent=(m==null?n:m);}
    el.addEventListener('input',run);el.addEventListener('change',run);});
  // sortable tables
  document.querySelectorAll('table.sort th').forEach(function(th,ci){th.style.cursor='pointer';th.addEventListener('click',function(){
    var tab=th.closest('table'),idx=Array.prototype.indexOf.call(th.parentNode.children,th),rows=Array.prototype.slice.call(tab.tBodies[0].rows);
    var asc=th.getAttribute('aria-sort')!=='ascending';th.parentNode.querySelectorAll('th').forEach(function(x){x.removeAttribute('aria-sort');});
    th.setAttribute('aria-sort',asc?'ascending':'descending');
    rows.sort(function(a,b){var x=a.cells[idx].getAttribute('data-s')||a.cells[idx].textContent,y=b.cells[idx].getAttribute('data-s')||b.cells[idx].textContent;
      var nx=parseFloat(x),ny=parseFloat(y);var c=(!isNaN(nx)&&!isNaN(ny))?nx-ny:x.localeCompare(y,'nl');return asc?c:-c;});
    rows.forEach(function(r){tab.tBodies[0].appendChild(r);});pageer(tab);});});
  // U-SITE: pagination for <table data-pagina="N"> (respects the filters and the spoiler switch)
  function pageer(tab){var per=+(tab.getAttribute('data-pagina')||0);if(!per)return null;var rows=tab.tBodies[0].rows,z=VR.zone(),zicht=[];
    for(var i=0;i<rows.length;i++){var r=rows[i];if(!r._uit&&+(r.getAttribute('data-zone')||1)<=z)zicht.push(r);r.hidden=true;}
    var pag=Math.max(1,Math.ceil(zicht.length/per));tab._p=Math.min(Math.max(0,tab._p||0),pag-1);
    zicht.forEach(function(r,i){r.hidden=Math.floor(i/per)!==tab._p;});
    var pg=tab._pager;if(!pg){pg=document.createElement('div');pg.className='pager';var w=tab.closest('.tscroll')||tab;w.parentNode.insertBefore(pg,w.nextSibling);tab._pager=pg;
      pg.addEventListener('click',function(e){var b=e.target.closest('button[data-p]');if(b){tab._p=+b.getAttribute('data-p');pageer(tab);tab.scrollIntoView({block:'nearest'});}});}
    var h='<button type="button" data-p="'+(tab._p-1)+'"'+(tab._p<=0?' disabled':'')+' aria-label="'+VR.t('Previous','Vorige')+'">&lsaquo;</button>';
    for(var k=0;k<pag;k++){if(pag>9&&k>1&&k<pag-2&&Math.abs(k-tab._p)>2){if(k===2||k===pag-3)h+='<span class="muted">&hellip;</span>';continue;}
      h+='<button type="button" data-p="'+k+'"'+(k===tab._p?' aria-current="page"':'')+'>'+(k+1)+'</button>';}
    h+='<button type="button" data-p="'+(tab._p+1)+'"'+(tab._p>=pag-1?' disabled':'')+' aria-label="'+VR.t('Next','Volgende')+'">&rsaquo;</button>';
    var a=zicht.length?tab._p*per+1:0,b2=Math.min(zicht.length,(tab._p+1)*per);
    h+='<span class="muted">'+a+'-'+b2+' '+VR.t('of','van')+' '+zicht.length+'</span>';pg.innerHTML=h;return zicht.length;}
  document.querySelectorAll('table[data-pagina]').forEach(function(tab){pageer(tab);});
  document.addEventListener('vr-zone',function(){document.querySelectorAll('table[data-pagina]').forEach(function(tab){pageer(tab);});});

  // ---- live data ----
  // W-SITE: always the real endpoint (live.guhs.nl); 8 s timeout; when it is unreachable or not live yet every part shows a friendly empty state
  function haal(naam){var url=VR.live+naam,ac=window.AbortController?new AbortController():null,tm=ac?setTimeout(function(){ac.abort();},8000):0;
    return fetch(url,{cache:'no-store',signal:ac?ac.signal:undefined}).then(function(r){clearTimeout(tm);if(!r.ok)throw new Error(r.status);return r.json();})
      .then(function(d){if(d&&d.nog_niet_live)throw new Error('nog niet live');return d;},function(e){clearTimeout(tm);throw e;});}
  VR.haal=haal;
  VR.leeg=function(){return VR.t('No players in the Vadsrijk yet','Nog geen spelers in het Vadsrijk');};
  VR.nietLive=function(){return VR.t('Live as soon as the server runs the update','Live zodra de server de update draait');};
  function duur(s){s=Math.max(0,Math.round(s));var h=Math.floor(s/3600),m=Math.floor(s%3600/60),x=s%60;
    return (h?h+':'+(m<10?'0':''):'')+m+':'+(x<10?'0':'')+x;}
  VR.duur=duur;
  var V=window.VR_VERTAAL||{};
  function vt(k){return V[k]||k;}
  VR.vt=vt;
  function esc(s){return String(s==null?'':s).replace(/[&<>"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
  VR.esc=esc;
  // status tiles: [data-live-status]
  (function(el){if(!el)return;
    function toon(s){var oud=(Date.now()-Date.parse(s.tijd))/1000;var aan=s.online&&oud<180;
      el.querySelector('[data-k=status]').innerHTML='<span class="stip-status '+(aan?'aan':'uit')+'"></span>'+(aan?VR.t('Online','Online'):VR.t('Offline','Offline'));
      el.querySelector('[data-k=spelers]').textContent=aan?s.aantal+' / '+s.max:'-';
      el.querySelector('[data-k=rijk]').textContent=aan?s.in_vadsrijk:'-';
      el.querySelector('[data-k=versie]').textContent=s.versie||'?';
      var namen=el.querySelector('[data-k=namen]');if(namen)namen.innerHTML=aan&&s.spelers.length?s.spelers.map(function(n){return '<a href="'+VR.root+'speler.html#'+encodeURIComponent(n)+'">'+esc(n)+'</a>';}).join(', '):'<span class="leeg">'+VR.t('nobody right now','nu niemand')+'</span>';}
    function fout(){el.querySelector('[data-k=status]').innerHTML='<span class="stip-status uit"></span>'+VR.t('Not live yet','Nog niet live');
      el.querySelector('[data-k=spelers]').textContent='-';el.querySelector('[data-k=rijk]').textContent='-';el.querySelector('[data-k=versie]').textContent='-';
      var namen=el.querySelector('[data-k=namen]');if(namen)namen.innerHTML='<span class="leeg">'+esc(VR.nietLive())+'</span>';}
    function laad(){haal('status.json').then(toon,fout);}laad();setInterval(laad,60000);})(document.querySelector('[data-live-status]'));
  // world clock: [data-live-wereld]
  (function(el){if(!el)return;
    var wd=null,geladen=0;
    function teken(){if(!wd)return;var weg=(Date.now()-geladen)/1000;
      var tb=el.querySelector('[data-k=bazen]');if(tb){tb.innerHTML=wd.bazen.map(function(b){var over=Math.max(0,b.terug_over_s-weg);
        return '<tr data-zone="'+(b.zone||1)+'"><td>'+esc(vt('arena:'+b.arena))+'</td><td>'+(over>0?'<span class="badge">'+VR.t('beaten','verslagen')+'</span>':'<span class="badge lv">'+VR.t('waiting for you','wacht op je')+'</span>')+'</td><td class="num">'+(over>0?duur(over):'-')+'</td><td class="num">'+duur(b.respawn_s)+'</td></tr>';}).join('');}
      var r=el.querySelector('[data-k=reset]');if(r)r.textContent=duur((Date.parse(wd.dagreset)-Date.now())/1000);
      var z=el.querySelector('[data-k=zegel]');if(z)z.textContent=vt(wd.weekzegel);
      var c=el.querySelector('[data-k=contracten]');if(c)c.textContent=wd.contracten.map(vt).join(', ')||'-';
      var k=el.querySelector('[data-k=karavaan]');if(k&&wd.karavaan){var kv=wd.karavaan;
        k.innerHTML=(kv.fase==='stop'?VR.t('Standing at ','Staat bij ')+'<b>'+esc(vt('stop:'+kv.stop))+'</b>'+(kv.vertrekt_over_s!=null?VR.t(', leaves in ',', vertrekt over ')+duur(kv.vertrekt_over_s-weg):''):
          kv.fase==='aanval'?VR.t('Under attack by klodders! Help!','Wordt aangevallen door klodders! Help!'):VR.t('On the road','Onderweg'))+
          ' &middot; <a href="'+VR.root+'kaart.html?plek=karavaan">'+VR.t('on the map','op de kaart')+'</a>';}
    }
    function nietLive(){if(wd)return;var tb=el.querySelector('[data-k=bazen]');if(tb)tb.innerHTML='<tr><td colspan=4 class="leeg">'+esc(VR.nietLive())+'</td></tr>';
      var k=el.querySelector('[data-k=karavaan]');if(k)k.innerHTML='<span class="leeg">'+esc(VR.nietLive())+'</span>';}
    function laad2(){haal('wereld.json').then(function(d){wd=d;geladen=Date.now()-Math.max(0,(Date.now()-Date.parse(d.tijd)));teken();},nietLive);}
    laad2();setInterval(laad2,30000);setInterval(teken,1000);document.addEventListener('vr-lang',teken);})(document.querySelector('[data-live-wereld]'));
  // highscores: [data-hiscores]
  (function(el){if(!el)return;
    var lijsten=JSON.parse(el.getAttribute('data-hiscores'));var tabs=el.querySelector('.hs-tabs'),tb2=el.querySelector('tbody'),kop=el.querySelector('[data-k=kop]'),zoek=el.querySelector('input');
    var huidig=(location.hash||'#generiek').slice(1),rijen=[];
    function kies(id){huidig=id;tabs.querySelectorAll('button').forEach(function(b){b.setAttribute('aria-pressed',String(b.getAttribute('data-l')===id));});
      var L=lijsten.filter(function(l){return l.id===id;})[0]||lijsten[0];kop.innerHTML=VR.t(L.k_en,L.k_nl);
      haal('hiscores/'+L.id+'.json').then(function(d){rijen=d.rijen;vul();el.querySelector('[data-k=tijd]').textContent=new Date(d.tijd).toLocaleString();},function(){rijen=[];tb2.innerHTML='<tr><td colspan=4 class="leeg">'+esc(VR.leeg())+'. '+esc(VR.nietLive())+'.</td></tr>';});
      history.replaceState(null,'','#'+id);}
    function vul(){var q=(zoek.value||'').toLowerCase();tb2.innerHTML=rijen.filter(function(r){return !q||r.naam.toLowerCase().indexOf(q)>=0;}).slice(0,100).map(function(r){
      return '<tr><td class="num">'+r.plek+'</td><td><img class="hoofd" alt="" loading="lazy" src="https://mc-heads.net/avatar/'+encodeURIComponent(r.naam)+'/24"><a href="speler.html#'+encodeURIComponent(r.naam)+'">'+esc(r.naam)+'</a></td><td class="num">'+r.waarde+'</td><td class="num">'+(r.xp!=null?r.xp.toLocaleString():'')+'</td></tr>';}).join('')||'<tr><td colspan=4 class="leeg">'+esc(VR.leeg())+'</td></tr>';}
    lijsten.forEach(function(l){var b=document.createElement('button');b.type='button';b.setAttribute('data-l',l.id);b.innerHTML='<span lang="en">'+l.en+'</span><span lang="nl">'+l.nl+'</span>';b.addEventListener('click',function(){kies(l.id);});tabs.appendChild(b);});
    zoek.addEventListener('input',vul);kies(huidig);})(document.querySelector('[data-hiscores]'));
  // profile: [data-profiel]
  (function(el){if(!el)return;
    function laadP(){var naam=decodeURIComponent((location.hash||'').slice(1));var uit=el.querySelector('[data-k=uit]');
      if(!naam){haal('spelers/index.json').then(function(d){if(!d.spelers||!d.spelers.length){uit.innerHTML='<p class="leeg">'+esc(VR.leeg())+'.</p>';return;}uit.innerHTML='<h2>'+VR.t('All players','Alle spelers')+'</h2><p>'+d.spelers.map(function(s){return '<a href="#'+encodeURIComponent(s.naam)+'">'+esc(s.naam)+'</a> <span class="badge lv">lv '+s.lv+'</span>';}).join(' &middot; ')+'</p>';},function(){uit.innerHTML='<p class="leeg">'+esc(VR.leeg())+'. '+esc(VR.nietLive())+'.</p>';});return;}
      haal('spelers/'+naam.toLowerCase()+'.json').then(function(p){
        var kl=Object.keys(p.klassen).map(function(k){return '<div><dt>'+esc(vt('klasse:'+k))+'</dt><dd>lv '+p.klassen[k]+'</dd></div>';}).join('');
        var boek=Object.keys(p.boek).map(function(z){return '<div><dt>'+esc(vt('zone:'+z))+'</dt><dd><div class="balk"><i style="width:'+p.boek[z]+'%"></i></div>'+p.boek[z]+'%</dd></div>';}).join('');
        var kleuren={gewoon:'#9a9a9a',ongewoon:'#2fae2f',zeldzaam:'#2f7fe0',episch:'#9a3fe6',legendarisch:'#e07d00',guhddelijk:'#e0409f'};
        var gear=Object.keys(p.gear).map(function(s){var g=p.gear[s];return '<div style="--rk:'+(kleuren[g.zeldzaamheid]||'#999')+'"><b class="zz '+g.zeldzaamheid+'">'+esc(vt('loot:'+g.type))+'</b><br><small>'+esc(vt('slot:'+s))+' &middot; lv '+g.lv+(g.sterren?' &middot; '+'&#9733;'.repeat(g.sterren):'')+(g.sokjes?' &middot; '+g.sokjes+' '+VR.t('sockets','sokjes'):'')+'</small></div>';}).join('')||'<p class="leeg">-</p>';
        uit.innerHTML='<div class="tw"><div><h1><img class="hoofd groot" alt="" src="https://mc-heads.net/head/'+encodeURIComponent(p.naam)+'/96"> '+esc(p.naam)+'</h1>'+
          '<p><span class="badge lv">lv '+p.lv+'</span> '+esc(vt(p.titel))+' &middot; '+esc(vt('klasse:'+p.klasse))+'</p>'+
          '<h2>'+VR.t('Gear','Uitrusting')+'</h2><div class="gear">'+gear+'</div>'+
          '<h2>'+VR.t('Exploration book','Verkenningsboek')+'</h2><dl class="feiten">'+boek+'</dl></div>'+
          '<aside class="zij"><div class="kaartje"><dl class="feiten">'+kl+'<div><dt>'+VR.t('Cheered up','Opgevrolijkt')+'</dt><dd>'+p.opgevrolijkt+'</dd></div><div><dt>'+VR.t('Bosses','Bazen')+'</dt><dd>'+p.bazen_totaal+'</dd></div>'+
          '<div><dt>'+VR.t('Pets','Huisdieren')+'</dt><dd>'+p.huisdieren+' / '+p.huisdieren_max+'</dd></div><div><dt>'+VR.t('Mounts','Rijdieren')+'</dt><dd>'+p.rijdieren+' / '+p.rijdieren_max+'</dd></div>'+
          (p.waar?'<div><dt>'+VR.t('Now','Nu')+'</dt><dd>'+esc(p.waar)+'</dd></div>':'')+'<div><dt>'+VR.t('Last seen','Laatst gezien')+'</dt><dd>'+esc(p.gezien)+'</dd></div>'+(p.eerste_50?'<div><dt>Lv 50</dt><dd>'+esc(p.eerste_50)+'</dd></div>':'')+'</dl></div></aside></div>';
        document.title=p.naam+' - Het Vadsrijk';},function(){uit.innerHTML='<p class="leeg">'+VR.t('No player with this name (yet).','(Nog) geen speler met deze naam.')+'</p>';});}
    window.addEventListener('hashchange',laadP);laadP();document.addEventListener('vr-lang',laadP);})(document.querySelector('[data-profiel]'));
})();
"""

LEAFLET_CSS = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
LEAFLET_JS = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"

# The map: Leaflet, CRS.Simple, lat = -z, lng = x (see kaart.py).
KAART_JS = r"""
(function(){
  var D=window.VR_KAART, K=D.kaart, el=document.getElementById('kaart');
  var map=L.map(el,{crs:L.CRS.Simple,minZoom:-4,maxZoom:3,zoomSnap:0.5,attributionControl:false,preferCanvas:false});
  function ll(x,z){return L.latLng(-z,x);}
  var b=L.latLngBounds(ll(K.x0,K.z1),ll(K.x1,K.z0));
  map.fitBounds(b);map.setMaxBounds(b.pad(0.35));
  L.tileLayer(VR.root+'kaart/tegels/{z}/{x}/{y}.webp',{minZoom:-4,maxZoom:3,minNativeZoom:-4,maxNativeZoom:0,tileSize:256,noWrap:true,bounds:b,
    errorTileUrl:'data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw=='}).addTo(map);
  function T(en,nl){return VR.t(en,nl);}
  function zoneNr(z){return +String(z||'zone1').replace('zone','');}
  var lagen={},alle=[];
  function laag(id,en,nl,aan){var g=L.layerGroup();lagen[id]={g:g,en:en,nl:nl};if(aan)g.addTo(map);return g;}
  var Lzones=laag('zones','Zones and areas','Zones en gebieden',true),Lst=laag('steden','Towns','Steden',true),Lvuur=laag('kampvuur','Campfires','Kampvuren',true),
    Luit=laag('uitkijk','Viewpoints','Uitkijkpunten',true),Lreis=laag('reizen','Airships, portals, lifts','Luchtschepen, portalen, liften',true),
    Lbaas=laag('bazen','Bosses','Bazen',true),Lquest=laag('quests','Quest givers','Questgevers',false),Lkar=laag('karavaan','Caravan (live)','Karavaan (live)',true),
    Lspel=laag('spelers','Show players (live)','Spelers tonen (live)',true),
    Lgeheim=laag('geheimen','Show secrets (spoilers!)','Geheimen tonen (spoilers!)',false);
  // U-SITE round 3: every secret with its spot, OFF by default and never in the search (pin() would add it to the search list)
  (D.geheimpunten||[]).forEach(function(g){var m=L.marker(ll(g.pos[0],g.pos[2]),{icon:L.divIcon({className:'',html:'<span class="vr-pin geheim">?</span>',iconSize:[20,20],iconAnchor:[10,10]})});
    m.bindPopup('<h4>'+VR.esc(g.naam)+'</h4><div>'+VR.esc(g.beloning||'')+'</div><div class="muted" style="font:12px var(--mono)">x '+g.pos[0]+' &middot; y '+g.pos[1]+' &middot; z '+g.pos[2]+'</div><a href="'+VR.root+'geheimen/index.html#'+g.id+'">'+VR.t('On the secrets page','Op de geheimenpagina')+'</a>');
    m._vrZone=zoneNr(g.zone);m.addTo(Lgeheim);});
  if(new URLSearchParams(location.search).get('geheim')==='1')Lgeheim.addTo(map);
  function pin(soort,teken,x,z,titel,html,zone,id){var m=L.marker(ll(x,z),{icon:L.divIcon({className:'',html:'<span class="vr-pin '+soort+'">'+teken+'</span>',iconSize:[22,22],iconAnchor:[11,11]}),title:titel,keyboard:true});
    m.bindPopup('<h4>'+VR.esc(titel)+'</h4>'+(html||'')+'<div class="muted" style="font:12px var(--mono)">x '+x+' &middot; z '+z+'</div>');m._vrZone=zoneNr(zone);m._vrId=id;alle.push({id:id,naam:titel,m:m,zone:m._vrZone,soort:soort});return m;}
  // areas: outlines + level labels
  var rond={};
  D.gebieden.forEach(function(g){if(g.id==='ring'||/_ring$/.test(g.id)||g.ring)return;var zn=zoneNr(g.zone),kleur=D.zonekleur[g.zone]||'#fff',vorm;
    if(g.laag&&g.laag.r_max){if(g.laag.r_max>500)return;vorm=L.circle(ll(g.laag.cx,g.laag.cz),{radius:g.laag.r_max,color:kleur,weight:1,opacity:.7,fill:false,dashArray:'4 8'});}
    else{vorm=L.rectangle(L.latLngBounds(ll(g.doos[0],g.doos[3]),ll(g.doos[2],g.doos[1])),{color:kleur,weight:1.5,fillOpacity:0.04,dashArray:g.stad?null:'6 6'});}
    vorm._vrZone=zn;vorm.addTo(Lzones);
    var c;if(g.laag&&g.laag.r_max){var key=g.laag.cx+','+g.laag.cz;rond[key]=(rond[key]||0)+1;var a=(-120+rond[key]*47)*Math.PI/180,rr=g.laag.r_min?(g.laag.r_min+g.laag.r_max)/2:g.laag.r_max*(rond[key]>1?0.75:0.35);
      c=[g.laag.cx+rr*Math.cos(a),g.laag.cz+rr*Math.sin(a)];}else{c=[(g.doos[0]+g.doos[2])/2,(g.doos[1]+g.doos[3])/2];}
    var lab=L.marker(ll(c[0],c[1]),{icon:L.divIcon({className:'vr-label gebied',html:VR.esc(g.naam)+'<span class="lvr">lv '+g.lv[0]+'-'+g.lv[1]+'</span>',iconSize:null}),interactive:false});lab._vrZone=zn;lab._vrMin=-1.5;lab.addTo(Lzones);
    var n=D.geheimen[g.id];if(n){vorm.bindTooltip(VR.esc(g.naam)+': '+T('there are '+n+' secrets here','er liggen hier '+n+' geheimen'),{sticky:true});}
    alle.push({id:g.id,naam:g.naam,m:lab,zone:zn,soort:'gebied',ll:ll(c[0],c[1])});});
  D.zones.forEach(function(z){if(!z.mid)return;var lab=L.marker(ll(z.mid[0],z.mid[1]),{icon:L.divIcon({className:'vr-label zone',html:VR.esc(z.naam)+'<span class="lvr">lv '+z.lv[0]+'-'+z.lv[1]+'</span>',iconSize:null}),interactive:false});lab._vrZone=z.nr;lab._vrMax=-2;lab.addTo(Lzones);});
  D.steden.forEach(function(s){pin('stad','&#9873;',s.pos[0],s.pos[2],s.naam,'<a href="'+VR.root+s.url+'">'+T('Zone page','Zonepagina')+'</a>',s.zone,s.id).addTo(Lst);});
  var tekens={kampvuur:'&#128293;',uitkijk:'&#128065;',luchtschip:'&#9992;',portaal:'&#10022;',kerker:'&#9876;',raid:'&#9733;',gondel:'&#8597;',lift:'&#8597;'};
  D.punten.forEach(function(p){var L2=p.soort==='kampvuur'?Lvuur:p.soort==='uitkijk'?Luit:(p.soort==='kerker'||p.soort==='raid')?Lbaas:Lreis;
    var m=pin(p.soort,tekens[p.soort]||'&#8226;',p.pos[0],p.pos[2],p.naam,'',p.zone,p.id);m._vrMin=(p.soort==='kerker'||p.soort==='raid')?-2:-0.5;if(p.soort==='raid')m.bindPopup('<h4>'+VR.esc(p.naam)+'</h4><a href="'+VR.root+'raid/index.html">'+VR.t('Raid page','Raidpagina')+'</a>');m.addTo(L2);});
  D.bazen.forEach(function(b){if(!b.pos)return;var m=pin('baas','&#9760;',b.pos[0],b.pos[2],b.naam,'<span class="badge lv">'+VR.esc(b.soort)+'</span> '+T('Respawn ','Komt terug na ')+VR.duur(b.respawn_s)+
    '<div data-baas="'+b.arena+'"></div><a href="'+VR.root+b.url+'">'+T('Boss page','Bazenpagina')+'</a>',b.zone,'baas-'+b.id);m._vrMin=-2;m.addTo(Lbaas);});
  D.questgevers.forEach(function(q){var m=pin(q.hoofd?'quest':'questzij',q.hoofd?'!':'?',q.pos[0],q.pos[2],q.naam,q.quests.map(function(x){return '<a href="'+VR.root+x.url+'">'+VR.esc(x.titel)+'</a>';}).join('<br>'),q.zone,'npc-'+q.id);m._vrMin=-0.5;m.addTo(Lquest);});
  D.routes.forEach(function(r){var l=L.polyline([ll(r.a[0],r.a[2]),ll(r.b[0],r.b[2])],{color:'#b9a7ff',weight:2.5,dashArray:'2 8',opacity:.9});l._vrZone=Math.max(zoneNr(r.za),zoneNr(r.zb));l.addTo(Lreis);});
  var lc=L.control.layers(null,{},{collapsed:window.innerWidth<900}).addTo(map);
  function lcNamen(){lc.remove();lc=L.control.layers(null,{},{collapsed:window.innerWidth<900});Object.keys(lagen).forEach(function(k){lc.addOverlay(lagen[k].g,T(lagen[k].en,lagen[k].nl));});lc.addTo(map);}
  lcNamen();document.addEventListener('vr-lang',lcNamen);
  // spoiler switch: hide everything of zones > X
  function zoneFilter(){var z=VR.zone(),zm=map.getZoom();Object.keys(lagen).forEach(function(k){lagen[k].g.eachLayer(function(l){var e=l.getElement&&l.getElement();
    var weg=(l._vrZone||1)>z||(l._vrMin!=null&&zm<l._vrMin)||(l._vrMax!=null&&zm>l._vrMax);
    if(l.setStyle){l.setStyle({opacity:weg?0:1,fillOpacity:weg?0:(l.options.fillOpacity||0)});} if(e)e.style.display=weg?'none':'';});});}
  map.on('zoomend overlayadd',zoneFilter);document.addEventListener('vr-zone',zoneFilter);setTimeout(zoneFilter,50);
  // coordinates
  var co=document.getElementById('coords');map.on('mousemove',function(e){co.textContent='x '+Math.round(e.latlng.lng)+'  z '+Math.round(-e.latlng.lat);});
  // search + deep link
  var inp=document.getElementById('kzoek'),res=document.getElementById('kres'),sel=-1,hits=[];
  function ga(h){var p=h.m.getLatLng?h.m.getLatLng():h.ll;map.setView(p,Math.max(map.getZoom(),0));if(h.m.openPopup&&h.soort!=='gebied')h.m.openPopup();history.replaceState(null,'','#'+h.id);res.classList.remove('open');}
  function zoek(){var q=inp.value.toLowerCase().trim();sel=-1;if(!q){res.classList.remove('open');return;}
    hits=alle.filter(function(h){return h.zone<=VR.zone()&&h.naam.toLowerCase().indexOf(q)>=0;}).slice(0,20);
    res.innerHTML=hits.map(function(h,i){return '<a href="#'+h.id+'" data-i="'+i+'">'+VR.esc(h.naam)+' <small>'+h.soort+'</small></a>';}).join('')||'<a>'+T('Nothing found','Niks gevonden')+'</a>';res.classList.add('open');}
  inp.addEventListener('input',zoek);res.addEventListener('click',function(e){var a=e.target.closest('a[data-i]');if(a){e.preventDefault();ga(hits[+a.getAttribute('data-i')]);}});
  inp.addEventListener('keydown',function(e){if(e.key==='Enter'&&hits.length){e.preventDefault();ga(hits[Math.max(0,sel)]);}});
  var stip=null;
  function hash(){var q=new URLSearchParams(location.search);
    if(q.get('x')&&q.get('z')){var x=+q.get('x'),z=+q.get('z');if(stip)stip.remove();stip=L.circleMarker(ll(x,z),{radius:9,color:'#fff',weight:3,fillColor:'#c2577f',fillOpacity:1}).addTo(map);map.setView(ll(x,z),1);return;}
    var id=decodeURIComponent(location.hash.slice(1))||q.get('plek')||'';if(!id)return;var h=alle.filter(function(x){return x.id===id||x.id.split(':').pop()===id;})[0];if(h)ga(h);}
  window.addEventListener('hashchange',hash);setTimeout(hash,100);
  // live: caravan + boss timers
  var kar=null;
  function live(){VR.haal('wereld.json').then(function(w){if(w.karavaan){var kv=w.karavaan;if(!kar){kar=pin('karavaan','&#128012;',kv.x,kv.z,T('The caravan','De karavaan'),'',1,'karavaan');kar.addTo(Lkar);}
      kar.setLatLng(ll(kv.x,kv.z));kar.setPopupContent('<h4>'+T('The caravan','De karavaan')+'</h4>'+(kv.fase==='stop'?T('At a stop','Bij een halte'):kv.fase==='aanval'?T('Under attack!','Wordt aangevallen!'):T('On the road','Onderweg')));}
    w.bazen.forEach(function(b){document.querySelectorAll('[data-baas="'+b.arena+'"]').forEach(function(d){d.innerHTML=b.terug_over_s>0?T('Back in ','Terug over ')+VR.duur(b.terug_over_s):T('Waiting for you now','Wacht nu op je');});});},function(){});}
  live();setInterval(live,30000);
  // U-SITE: live player positions + airships in flight (posities.json, ~10 s); markers glide to their new spot
  var spelersM={},schipM={},pnote=document.getElementById('spelersnoot');
  var KI={knuffelridder:'&#9876;',pluisjager:'&#127993;',vadsmagier:'&#10024;'};
  function glij(m,naar){var van=m.getLatLng(),t0=performance.now(),d=1800;cancelAnimationFrame(m._raf);
    (function stap(t){var f=Math.min(1,(t-t0)/d);f=f<.5?2*f*f:1-Math.pow(-2*f+2,2)/2;m.setLatLng(L.latLng(van.lat+(naar.lat-van.lat)*f,van.lng+(naar.lng-van.lng)*f));if(f<1)m._raf=requestAnimationFrame(stap);})(t0);}
  function posities(){VR.haal('posities.json').then(function(P){var zien={},sz={};if(P.tijd&&Date.now()-P.tijd>120000)P={spelers:[],status:[],schepen:[]};   // W-SITE: an old file (server stopped) = nobody
      (P.spelers||[]).forEach(function(s){var id=s.naam;zien[id]=1;var to=ll(s.x,s.z),m=spelersM[id];
        var html='<span class="vr-speler"><img src="https://mc-heads.net/avatar/'+encodeURIComponent(s.naam)+'/20" alt="" width="20" height="20"><b>'+VR.esc(s.naam)+'</b><i>'+(KI[s.klasse]||'')+' '+(s.level||'')+'</i></span>';
        if(!m){m=L.marker(to,{icon:L.divIcon({className:'',html:html,iconSize:null,iconAnchor:[10,10]}),title:s.naam,zIndexOffset:1000});
          m.bindPopup('<h4>'+VR.esc(s.naam)+'</h4>'+VR.t('Level ','Level ')+(s.level||'?')+(s.klasse?' &middot; '+VR.esc(VR.vt?VR.vt('klasse:'+s.klasse):s.klasse):'')+'<br><a href="'+VR.root+'speler.html#'+encodeURIComponent(s.naam)+'">'+VR.t('Profile','Profiel')+'</a>');
          m._vrZone=+String(s.zone||'zone1').replace('zone','')||1;m.addTo(Lspel);spelersM[id]=m;}else{glij(m,to);}});
      Object.keys(spelersM).forEach(function(id){if(!zien[id]){Lspel.removeLayer(spelersM[id]);delete spelersM[id];}});
      (P.schepen||[]).forEach(function(s){sz[s.id]=1;var to=ll(s.x,s.z),m=schipM[s.id];
        if(!m){m=L.marker(to,{icon:L.divIcon({className:'',html:'<span class="vr-pin luchtschip vliegt">&#9992;</span>',iconSize:[24,24],iconAnchor:[12,12]}),title:VR.t('Airship','Luchtschip')});
          m.bindPopup('<h4>'+VR.t('Airship in flight','Luchtschip onderweg')+'</h4>'+(s.naar?VR.t('to ','naar ')+VR.esc(s.naar):''));m.addTo(Lspel);schipM[s.id]=m;}else{glij(m,to);}});
      Object.keys(schipM).forEach(function(id){if(!sz[id]){Lspel.removeLayer(schipM[id]);delete schipM[id];}});
      if(pnote){var n=(P.spelers||[]).length,st=(P.status||[]).length;
        pnote.textContent=n?VR.t(n+' player'+(n>1?'s':'')+' on the map','Spelers op de kaart: '+n)+(st?VR.t(' ('+st+' hidden in a dungeon, raid or house)',' (+'+st+' in een kerker, raid of huisje)'):''):(st?VR.t(st+' in a dungeon, raid or house',st+' in een kerker, raid of huisje'):VR.leeg());}
      pfout=0;setTimeout(zoneFilter,30);},function(){pfout++;if(pnote)pnote.textContent=VR.nietLive();});}
  // W-SITE: unreachable -> back off (10 s, then every 60 s) instead of hammering a missing endpoint
  var pfout=0,ptik=0;posities();setInterval(function(){ptik++;if(pfout<2||ptik%6===0)posities();},10000);
})();
"""
