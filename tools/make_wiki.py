"""
Builds the Guhs wiki: one HTML page (English + Dutch, collapsible sections) + the renders from wiki_renders.py.

Usage (from the project root):
    python tools/wiki_renders.py <out>/img
    python tools/make_wiki.py <out>          -> <out>/index.html
Open <out>/index.html#print-en or #print-nl in a browser to get a print-ready version (all sections open).
"""
import html
import os
import sys

VERSION = "1.1.0"


def quest_count():
    """How many quests the FTB chapter has (tools/make_ftbquests.py, without writing anything)."""
    import runpy
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    return len(runpy.run_path(os.path.join(os.path.dirname(os.path.abspath(__file__)), "make_ftbquests.py"), run_name="count")["QUESTS"])


def t(en, nl):
    """Inline text in both languages (only the chosen one is shown)."""
    return f'<span lang="en">{en}</span><span lang="nl">{nl}</span>'


def p(en, nl):
    return f'<p lang="en">{en}</p><p lang="nl">{nl}</p>'


def ul(items):
    return "<ul>" + "".join(f"<li>{t(en, nl)}</li>" for en, nl in items) + "</ul>"


def img(name, alt, cls=""):
    return f'<img src="img/{name}.png" alt="{html.escape(alt)}" loading="lazy" class="{cls}">'


def icon(name, label):
    return f'<img class="px" src="img/icon_{name}.png" alt="{html.escape(label)}" title="{html.escape(label)}">'


def section(sid, en, nl, body, open_=True):
    return f'''<details class="sec" id="{sid}"{" open" if open_ else ""}>
<summary><h2>{t(en, nl)}</h2><span class="chev" aria-hidden="true"></span></summary>
<div class="sec-body">{body}</div></details>'''


def entry(image, title_en, title_nl, body, stats=None, wide=False):
    stat_html = ""
    if stats:
        stat_html = '<dl class="stats">' + "".join(
            f"<div><dt>{t(k_en, k_nl)}</dt><dd>{v}</dd></div>" for (k_en, k_nl), v in stats) + "</dl>"
    return f'''<article class="entry{" wide" if wide else ""}">
<figure class="stage">{image}</figure>
<div class="entry-text"><h3>{t(title_en, title_nl)}</h3>{body}{stat_html}</div></article>'''


def grid(cells, result, count=1, shapeless=False):
    """A crafting-table grid (3x3 list of icon names or None) -> result."""
    slots = "".join(f'<span class="slot">{icon(c, c.replace("_", " ")) if c else ""}</span>' for c in cells)
    tag = f'<span class="tag">{t("shapeless", "vormloos")}</span>' if shapeless else ""
    n = f'<b class="count">{count}</b>' if count > 1 else ""
    return f'''<div class="recipe"><div class="craft">{slots}</div><span class="arrow" aria-hidden="true"></span>
<span class="slot big">{icon(result, result.replace("_", " "))}{n}</span>{tag}</div>'''


def rcard(en, nl, g):
    return f'<div class="recipe-card"><h3>{t(en, nl)}</h3>{g}</div>'


def row(*names):
    return list(names)


# ---------------------------------------------------------------------------------------------------------------------
CSS = r"""
:root{
  --paper:#fff7fa; --card:#ffffff; --ink:#3a1c30; --muted:#8a6479; --line:#f0d9e3;
  --rasp:#d9467a; --rasp-soft:#fde3ec; --cheese:#e9a21c; --cheese-soft:#fff1d2; --lilac:#b8a1d4;
  --slot:#8b8b8b; --slot-in:#c6c6c6; --stage:#fbeef3;
  --display:"Fredoka","Baloo 2","Trebuchet MS",system-ui,sans-serif;
  --body:"Nunito Sans","Segoe UI",system-ui,sans-serif;
  --mono:"JetBrains Mono",ui-monospace,Consolas,monospace;
}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){
  color-scheme:dark;--paper:#1d1219;--card:#291a23;--ink:#f7e7ef;--muted:#bb97ab;--line:#3f2836;
  --rasp:#f06a9a;--rasp-soft:#3a1d2a;--cheese:#f2b544;--cheese-soft:#35291a;--lilac:#c7b3e0;--stage:#24161f;
  --slot:#555;--slot-in:#777;}}
:root[data-theme="dark"]{color-scheme:dark;--paper:#1d1219;--card:#291a23;--ink:#f7e7ef;--muted:#bb97ab;--line:#3f2836;
  --rasp:#f06a9a;--rasp-soft:#3a1d2a;--cheese:#f2b544;--cheese-soft:#35291a;--lilac:#c7b3e0;--stage:#24161f;--slot:#555;--slot-in:#777;}
body{background:var(--paper);color:var(--ink);font:16px/1.6 var(--body);}
#app[data-lang="en"] [lang="nl"],#app[data-lang="nl"] [lang="en"]{display:none!important}
.wrap{max-width:1180px;margin:0 auto;padding-inline:20px}
.bar{position:sticky;top:env(safe-area-inset-top,0px);z-index:5;background:color-mix(in srgb,var(--paper) 88%,transparent);
  backdrop-filter:blur(8px);border-bottom:1px solid var(--line)}
.bar .wrap{display:flex;align-items:center;gap:12px;padding-block:10px;flex-wrap:wrap}
.brand{font:700 22px/1 var(--display);letter-spacing:.5px;color:var(--rasp);margin-right:auto;display:flex;align-items:center;gap:8px}
.brand img{width:30px;height:30px;image-rendering:pixelated}
.seg{display:inline-flex;border:1.5px solid var(--rasp);border-radius:999px;overflow:hidden}
.seg button{border:0;background:transparent;color:var(--rasp);font:600 13px/1 var(--body);padding:7px 12px;cursor:pointer}
.seg button[aria-pressed="true"]{background:var(--rasp);color:#fff}
.ghost{border:1.5px solid var(--line);background:var(--card);color:var(--ink);border-radius:999px;font:600 13px/1 var(--body);padding:7px 12px;cursor:pointer}
button:focus-visible,summary:focus-visible,a:focus-visible{outline:3px solid var(--cheese);outline-offset:2px}
.hero{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1.1fr);gap:24px;align-items:center;padding-block:28px 8px}
.hero h1{font:600 clamp(42px,7vw,76px)/1 var(--display);margin:0 0 8px;color:var(--ink);text-wrap:balance}
.hero h1 em{font-style:normal;color:var(--rasp)}
.tagline{font:500 20px/1.4 var(--display);color:var(--muted);margin:0 0 16px}
.chips{display:flex;flex-wrap:wrap;gap:8px;margin:0;padding:0;list-style:none}
.chips li{background:var(--card);border:1px solid var(--line);border-radius:999px;padding:4px 12px;font-size:14px}
.chips b{font-family:var(--mono);font-weight:600}
.hero-art{position:relative;display:flex;justify-content:center}
.hero-art img{width:min(100%,460px);filter:drop-shadow(0 18px 18px rgba(80,20,50,.18))}
.hero-art .puff{position:absolute;width:26%;right:6%;bottom:4%;transform:rotate(-8deg)}
.layout{display:grid;grid-template-columns:200px minmax(0,1fr);gap:28px;padding-block:18px 60px}
.toc{position:sticky;top:70px;align-self:start;font-size:14px}
.toc a{display:block;color:var(--muted);text-decoration:none;padding:5px 10px;border-left:2px solid var(--line)}
.toc a:hover{color:var(--rasp);border-left-color:var(--rasp)}
.sec{background:var(--card);border:1px solid var(--line);border-radius:18px;margin-bottom:18px}
.sec>summary{list-style:none;display:flex;align-items:center;gap:12px;padding:16px 22px;cursor:pointer}
.sec>summary::-webkit-details-marker{display:none}
.sec h2{font:600 26px/1.2 var(--display);margin:0;flex:1}
.chev{width:12px;height:12px;border-right:3px solid var(--rasp);border-bottom:3px solid var(--rasp);transform:rotate(45deg);transition:transform .2s}
.sec[open] .chev{transform:rotate(-135deg)}
.sec-body{padding:0 22px 22px}
.sec-body>p{max-width:68ch}
h3{font:600 20px/1.25 var(--display);margin:0 0 6px}
.entry{display:grid;grid-template-columns:200px minmax(0,1fr);gap:18px;padding-block:16px;border-top:1px dashed var(--line)}
.entry:first-of-type{border-top:0}
.entry.wide{grid-template-columns:minmax(0,1.15fr) minmax(0,1fr)}
.stage{margin:0;background:radial-gradient(circle at 50% 60%,var(--stage),transparent 70%);border-radius:14px;
  display:flex;align-items:center;justify-content:center;min-height:150px}
.stage img{max-width:100%;max-height:260px}
.entry.wide .stage img{max-height:380px}
.stage img.shot{border-radius:10px;box-shadow:0 2px 10px rgba(58,28,48,.18)}
.entry-text p{margin:.2em 0 .6em;max-width:66ch}
.stats{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:6px 14px;margin:10px 0 0;padding:10px 12px;
  background:var(--rasp-soft);border-radius:12px;font-size:14px}
.stats div{display:flex;flex-direction:column}
.stats dt{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.06em}
.stats dd{margin:0;font-weight:700;font-variant-numeric:tabular-nums}
.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:14px}
.card{border:1px solid var(--line);border-radius:14px;padding:12px;display:flex;flex-direction:column;gap:6px;background:var(--paper)}
.card .stage{min-height:120px}
.card .stage img{max-height:130px}
.card h3{font-size:18px}
.card p{margin:0;font-size:15px}
.px{image-rendering:pixelated;width:32px;height:32px}
table{border-collapse:collapse;width:100%;font-size:15px}
.tscroll{overflow-x:auto}
th,td{text-align:left;padding:8px 10px;border-bottom:1px solid var(--line);vertical-align:top}
th{font:600 14px var(--display);color:var(--muted)}
.swatch{display:inline-block;width:14px;height:14px;border-radius:4px;vertical-align:-2px;margin-right:6px;border:1px solid rgba(0,0,0,.15)}
.recipes{display:grid;grid-template-columns:repeat(auto-fill,minmax(270px,1fr));gap:16px}
.recipe-card{border:1px solid var(--line);border-radius:14px;padding:12px;background:var(--paper)}
.recipe-card h3{font-size:17px}
.recipe{display:flex;align-items:center;gap:10px;flex-wrap:wrap}
.craft{display:grid;grid-template-columns:repeat(3,40px);gap:2px;background:var(--slot);padding:3px;border-radius:4px}
.slot{width:40px;height:40px;background:var(--slot-in);display:flex;align-items:center;justify-content:center;position:relative;
  box-shadow:inset 2px 2px 0 rgba(0,0,0,.25),inset -2px -2px 0 rgba(255,255,255,.4)}
.slot.big{width:52px;height:52px}
.slot .count{position:absolute;right:3px;bottom:0;color:#fff;text-shadow:2px 2px 0 #3f3f3f;font:700 15px var(--mono)}
.arrow{width:28px;height:16px;background:var(--muted);clip-path:polygon(0 35%,60% 35%,60% 0,100% 50%,60% 100%,60% 65%,0 65%)}
.tag{font-size:12px;color:var(--muted);border:1px solid var(--line);border-radius:999px;padding:1px 8px}
.gallery{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:16px}
.gallery figure{margin:0;border:1px solid var(--line);border-radius:14px;padding:12px;background:var(--paper);display:flex;flex-direction:column;gap:8px}
.gallery figure .stage{min-height:200px}
.gallery figure img{max-height:260px}
.gallery figcaption h3{font-size:18px}
.gallery figcaption p{margin:0;font-size:15px}
.rarity{display:inline-block;font:600 12px var(--body);padding:1px 9px;border-radius:999px;background:var(--cheese-soft);color:var(--ink);margin-left:6px;vertical-align:2px}
code,kbd{font-family:var(--mono);font-size:.88em;background:var(--rasp-soft);padding:1px 6px;border-radius:6px}
.cmd{display:flex;gap:8px;align-items:center;margin:6px 0;flex-wrap:wrap}
.cmd code{flex:1;min-width:0;overflow-x:auto;white-space:nowrap;padding:6px 10px}
.note{border-left:4px solid var(--cheese);background:var(--cheese-soft);padding:10px 14px;border-radius:0 12px 12px 0;margin:10px 0}
footer{color:var(--muted);font-size:14px;padding-bottom:40px}
@media (max-width:860px){
  .layout{grid-template-columns:minmax(0,1fr)}
  .toc{position:static;display:flex;flex-wrap:wrap;gap:4px}
  .toc a{border:1px solid var(--line);border-radius:999px;padding:3px 10px}
  .hero{grid-template-columns:minmax(0,1fr)}
  .entry,.entry.wide{grid-template-columns:minmax(0,1fr)}
}
@media (prefers-reduced-motion:reduce){.chev{transition:none}}
#app.print .bar,#app.print .toc,#app.print .copy{display:none}
#app.print .layout{grid-template-columns:minmax(0,1fr)}
#app.print .sec{break-inside:auto;border-radius:0;border:0;border-top:2px solid var(--rasp)}
#app.print .entry,#app.print .card,#app.print .recipe-card,#app.print .gallery figure{break-inside:avoid}
#app.print .sec>summary{break-after:avoid;padding-inline:0}#app.print .chev{display:none}#app.print .sec-body{padding-inline:0}
#app.print h3{break-after:avoid}
@media print{.bar,.toc,.copy{display:none}.layout{grid-template-columns:1fr}body{background:#fff}}
"""

JS = r"""
(function(){
  var app=document.getElementById('app');
  function setLang(l){app.dataset.lang=l;document.documentElement.lang=l;
    document.querySelectorAll('[data-setlang]').forEach(function(b){b.setAttribute('aria-pressed',String(b.dataset.setlang===l));});
    try{localStorage.setItem('guhs-wiki-lang',l);}catch(e){}}
  var saved=null;try{saved=localStorage.getItem('guhs-wiki-lang');}catch(e){}
  var hash=(location.hash||'').replace('#','');
  if(hash.indexOf('print-')===0){app.classList.add('print');setLang(hash.slice(6));
    document.querySelectorAll('details').forEach(function(d){d.open=true;});}
  else if(hash==='nl'||hash==='en'){setLang(hash);} else {setLang(saved||'en');}
  document.querySelectorAll('[data-setlang]').forEach(function(b){b.addEventListener('click',function(){setLang(b.dataset.setlang);});});
  var all=document.getElementById('toggle-all');
  all.addEventListener('click',function(){var ds=document.querySelectorAll('details.sec');
    var anyClosed=[].some.call(ds,function(d){return !d.open;});ds.forEach(function(d){d.open=anyClosed;});});
  document.querySelectorAll('.toc a').forEach(function(a){a.addEventListener('click',function(){
    var d=document.getElementById(a.getAttribute('href').slice(1));if(d)d.open=true;});});
  document.querySelectorAll('.copy').forEach(function(b){b.addEventListener('click',function(){
    var code=b.previousElementSibling;var txt=code.textContent;
    var done=function(){var o=b.textContent;b.textContent='✓';setTimeout(function(){b.textContent=o;},1200);};
    try{navigator.clipboard.writeText(txt).then(done,function(){select(code);});}catch(e){select(code);}});});
  function select(el){var r=document.createRange();r.selectNodeContents(el);var s=getSelection();s.removeAllRanges();s.addRange(r);}
})();
"""


def cmd(text):
    return f'<div class="cmd"><code>{html.escape(text)}</code><button class="ghost copy" type="button">{t("Copy", "Kopieer")}</button></div>'


def guheinde_section():
    """2.6.0: Het Guheinde, the endgame (a parody of the End and the Ender Dragon)."""
    intro = p("<b>Het Guheinde</b> is the endgame of Guhs: a guh parody of the End and the Ender Dragon. <b>Opper-Mika</b>, the boss of all "
              "Mikas, has stolen every kaasknabbel of the guh kingdom, and without knabbels no guh is ever <i>vahoeg</i>. The <b>Koningguh</b> "
              "on his throne in the guh castle tells you the story (right-click him while he is wild) and gives you the book about it. "
              "Nothing is locked: you can do every step whenever you like.",
              "<b>Het Guheinde</b> is het eindspel van Guhs: een guh-parodie op de End en de Enderdraak. <b>Opper-Mika</b>, de baas van alle "
              "Mika's, heeft alle kaasknabbels van het guhrijk gestolen, en zonder knabbels is geen enkele guh <i>vahoeg</i>. De <b>Koningguh</b> "
              "op zijn troon in het guhkasteel vertelt je het verhaal (rechtsklik hem zolang hij wild is) en geeft je het boek erover. "
              "Niets zit op slot: je doet elke stap wanneer je wilt.")
    steps = ul([
        ("<b>Mika-tranen</b>: Mikas sometimes cry one when beaten (looting helps); Big Mika always cries two or three, and beating the Mika-baas three times in a row with VADS gives two.",
         "<b>Mika-tranen</b>: Mika's huilen er soms een als je ze verslaat (plundering helpt); Grote Mika huilt er altijd twee of drie, en drie keer op rij winnen van de Mika-baas met VADS levert er twee op."),
        ("<b>Oog van Vadsig</b> = guh crystal + kaasknabbel + Mika-traan. Throw it in the Guhmension: it flies towards the nearest <b>Knabbelkelder</b> and drops back (1 in 5 breaks with a 'njeg'). Only works in the Guhmension.",
         "<b>Oog van Vadsig</b> = guhkristal + kaasknabbel + Mika-traan. Gooi het in de Guhmensie: het vliegt naar de dichtstbijzijnde <b>Knabbelkelder</b> en valt terug (1 op 5 gaat 'njeg' kapot). Werkt alleen in de Guhmensie."),
        ("There are four Knabbelkelders, in a ring about 500 to 900 blocks from the middle of the Guhmension, deep underground. The super compass finds them too (category <i>Einde</i>).",
         "Er zijn vier Knabbelkelders, in een ring op zo'n 500 tot 900 blokken van het midden van de Guhmensie, diep onder de grond. Het superkompas vindt ze ook (categorie <i>Einde</i>)."),
        ("Put an Oog van Vadsig in each of the <b>12 knabbelportaalframes</b> in the portal room and the portal to the Guheinde opens.",
         "Stop een Oog van Vadsig in elk van de <b>12 knabbelportaalframes</b> in de portaalkamer en het portaal naar het Guheinde gaat open."),
    ])
    body = intro + steps + \
        '<div class="recipes">' + rcard("Oog van Vadsig", "Oog van Vadsig", grid(["guh_kristal", "kaas_knabbels", "mika_traan", None, None, None, None, None, None], "oog_van_vadsig", shapeless=True)) + \
        rcard("Knabbelkristal", "Knabbelkristal", grid(["glass", "glass", "glass", "glass", "oog_van_vadsig", "glass", "glass", "mika_traan", "glass"], "knabbelkristal")) + '</div>' + \
        entry(img("structure_knabbelkelder", "The Knabbelkelder"), "The Knabbelkelder", "De Knabbelkelder",
              p("A cellar of cheese-crust bricks under the Guhmension, partly gnawed away by the Mikas. <b>Mika-larfjes</b> (tiny Mikas, the silverfish of the guhs) "
                "live in the bitten bricks and steal your knabbels. In the <b>cells</b> sit grey, <b>starving guhs</b>: give one a kaasknabbel and VAHOEG, it gets its colour back and runs home "
                "(save six for an advancement). There is a library with the new lore book, a Mika feast hall, a looted pantry and the open <b>portal room</b> with the twelve frames above a pool of kaassaus.",
                "Een kelder van kaaskorststenen onder de Guhmensie, deels weggeknaagd door de Mika's. In de aangevreten stenen wonen <b>Mika-larfjes</b> (piepkleine Mika's, de zilvervisjes van de guhs) "
                "die je knabbels inpikken. In de <b>cellen</b> zitten grauwe, <b>magere guhs</b>: geef er een een kaasknabbel en VAHOEG, hij krijgt zijn kleur terug en rent naar huis "
                "(red er zes voor een vooruitgang). Er is een bibliotheek met het nieuwe lore-boek, een Mika-feestzaal, een leeggeroofde voorraadkamer en de open <b>portaalkamer</b> met de twaalf frames boven een poel kaassaus."),
              wide=True) + \
        entry(img("structure_guheinde_knabbelberg", "The Knabbelberg"), "The Guheinde and the Knabbelberg", "Het Guheinde en de Knabbelberg",
              p("An island of <b>kaaskorst</b> floating in nothing, with a purple sky full of knabbel crumbs. In the middle stands the <b>Knabbelberg</b>, a giant half-eaten kaasknabbel with a spiral path up. "
                "Around it stand eight <b>kaaspilaren</b> with a <b>knabbelkristal</b> on top: crystals full of stolen knabbels that heal Opper-Mika with a beam (two are in a cage). "
                "Die in the Guheinde and you keep your things: the Mikas only take your kaasknabbels.",
                "Een eiland van <b>kaaskorst</b> dat in het niets zweeft, onder een paarse lucht vol knabbelkruimels. In het midden staat de <b>Knabbelberg</b>, een reusachtige half opgegeten kaasknabbel met een spiraalpad naar boven. "
                "Eromheen staan acht <b>kaaspilaren</b> met een <b>knabbelkristal</b> erop: kristallen vol gestolen knabbels die Opper-Mika met een straal genezen (twee zitten in een kooi). "
                "Ga je dood in het Guheinde, dan houd je je spullen: de Mika's pikken alleen je kaasknabbels in."), wide=True) + \
        entry(img("opper_mika", "Opper-Mika"), "Opper-Mika and the starved Enderguh", "Opper-Mika en de hongerige Enderguh",
              p("<b>Opper-Mika</b> (150 HP, a boss bar) rides the last big <b>Enderguh</b>, grey and thin because the Mikas never give it a knabbel. You can't hurt the Enderguh (you don't hit a guh!). "
                "<b>Phase 1</b>: it circles, swoops and lands on the Knabbelberg now and then, and Opper-Mika throws <b>Mika-vetballen</b> that leave slippery puddles. As long as he rides, he never goes below half his health. "
                "Every crystal you smash rains knabbels and makes the Enderguh a bit more vahoeg (its colours come back). <b>Phase 2</b>: with all crystals gone the worn-out Enderguh lands; "
                "feed it a kaasknabbel and VAHOEG, it throws Opper-Mika off. On foot he steals your knabbels (you get them back when he loses), makes vetplassen, does a belly flop and calls Mika helpers. "
                "'NJEG... IK BEN... OPPER-GEVADST!'",
                "<b>Opper-Mika</b> (150 levens, een baasbalk) rijdt op de laatste grote <b>Enderguh</b>, grauw en mager omdat de Mika's hem nooit een knabbel geven. De Enderguh kun je geen pijn doen (een guh mep je niet!). "
                "<b>Fase 1</b>: hij cirkelt, duikt en landt af en toe op de Knabbelberg, en Opper-Mika gooit <b>Mika-vetballen</b> die gladde plassen maken. Zolang hij rijdt, gaat hij nooit onder de helft van zijn levens. "
                "Elk kristal dat je stukslaat laat knabbels regenen en maakt de Enderguh een beetje vahoeger (zijn kleuren komen terug). <b>Fase 2</b>: zijn alle kristallen kapot, dan landt de uitgeputte Enderguh; "
                "geef hem een kaasknabbel en VAHOEG, hij gooit Opper-Mika eraf. Te voet pikt hij je knabbels in (die krijg je terug als hij verliest), maakt hij vetplassen, doet hij een buikplof en roept hij Mika-hulpjes. "
                "'NJEG... IK BEN... OPPER-GEVADST!'"), wide=True) + \
        entry(img("guh_variant_vahoege_ender", "Vahoege Enderguh"), "The rewards", "De beloningen",
              ul([("A rain of knabbels, experience, and a floating <b>top 3</b> of the fastest wins at the arrival platform.",
                   "Een regen van knabbels, ervaring, en een zwevende <b>top 3</b> van de snelste overwinningen bij het aankomstplatform."),
                  ("First win (per player): the <b>Knabbelkroon</b> (a helmet as strong as vahoege vads: never really hungry, happy guhs around you, Mikas come at you and take 1.5x damage), "
                   "the Opper-Mikatrofee and a tamed, saddled <b>Vahoege Enderguh</b> to fly on.",
                   "Eerste overwinning (per speler): de <b>Knabbelkroon</b> (een helm zo sterk als vahoege vads: nooit echt honger, blije guhs om je heen, Mika's komen op je af en krijgen 1,5x zoveel schade), "
                   "de Opper-Mikatrofee en een getemde <b>Vahoege Enderguh</b> met zadel om op te vliegen."),
                  ("Later wins: an <b>Enderguh-ei</b>. Put it down: it cracks and a baby Vahoege Enderguh hops out.",
                   "Latere overwinningen: een <b>Enderguh-ei</b>. Zet het neer: het barst open en er hupt een baby-Vahoege Enderguh uit."),
                  ("The first win in a world opens Opper-Mika's <b>knabbelschat</b> under the Knabbelberg and lights the terugportaal on top.",
                   "De eerste overwinning in een wereld opent Opper-Mika's <b>knabbelschat</b> onder de Knabbelberg en zet het terugportaal bovenop aan."),
                  ("Every win makes a <b>Knabbelpoort</b> around the island (up to 20): it throws you ~1200 blocks out to the outer islands.",
                   "Elke overwinning maakt een <b>Knabbelpoort</b> rond het eiland (tot 20): die gooit je zo'n 1200 blokken ver naar de buiteneilanden."),
                  ("Go back to the Koningguh to be knighted <b>Ridder van het Guheinde</b>.", "Ga terug naar de Koningguh en laat je tot <b>Ridder van het Guheinde</b> slaan."),
                  ("Put four <b>knabbelkristallen</b> on the four knabbelsokkels around the terugportaal to call Opper-Mika back for another fight.",
                   "Zet vier <b>knabbelkristallen</b> op de vier knabbelsokkels rond het terugportaal om Opper-Mika terug te roepen voor nog een gevecht.")]) +
              '<p>' + ' '.join([icon('knabbelkroon', 'Knabbelkroon'), icon('enderguh_ei_0', 'Enderguh-ei'), icon('opper_mikatrofee_front', 'Opper-Mikatrofee'),
                                icon('knabbelkristal', 'Knabbelkristal'), icon('guhvleugels', 'Guhvleugels')]) + '</p>', wide=True) + \
        entry(img("structure_mika_vesting_schip", "Mika-vesting with vetschip"), "Mika-vestingen and the Guhvleugels", "Mika-vestingen en de Guhvleugels",
              p("On the outer islands stand <b>Mika-vestingen</b>: towers of purple Mika stone with Mikas, loot and a little Mika throne. Half of them have a greasy <b>vetschip</b> floating next to them. "
                "In the cabin at its bow hang the <b>Guhvleugels</b>: little Enderguh wings to glide with, like an elytra (mend them with Mika's vet). "
                "All over the outer islands (every few hundred blocks) stands a <b>Terugpoort</b>: a little lit Knabbelpoort gate with a guh head on top. Step in and you "
                "float back to the arrival platform on the main island. Tuut tuut!",
                "Op de buiteneilanden staan <b>Mika-vestingen</b>: torens van paarse Mika-steen met Mika's, buit en een Mika-troontje. Bij de helft zweeft er een vettig <b>vetschip</b> naast. "
                "In het kamertje op de boeg hangen de <b>Guhvleugels</b>: kleine Enderguhvleugeltjes om mee te zweven, zoals een elytra (repareren met Mika's vet). "
                "Overal op de buiteneilanden (om de paar honderd blokken) staat een <b>Terugpoort</b>: een verlicht Knabbelpoortje met een guhkop erop. Stap erin en je "
                "zweeft terug naar het aankomstplatform op het grote eiland. Tuut tuut!"), wide=True) + \
        entry(img("guh_variant_mager", "Magere guh"), "Two new Guhdex pages", "Twee nieuwe Guhdex-pagina's",
              p("The <b>magere guh</b> (in the cells of the Knabbelkelder; you get its star for saving one) and the <b>Vahoege Enderguh</b> (gold and pink, with dragon wings; it flies like the Enderguh).",
                "De <b>magere guh</b> (in de cellen van de Knabbelkelder; zijn ster krijg je voor het redden) en de <b>Vahoege Enderguh</b> (goud en roze, met drakenvleugels; hij vliegt net als de Enderguh)."))
    return section("new26", "New in 2.6: Het Guheinde", "Nieuw in 2.6: Het Guheinde", body)


def table(head, rows):
    """A table: head = [(en, nl)...], rows = [[cell...]...]; a cell is (en, nl) or plain html."""
    cell = lambda c: t(*c) if isinstance(c, tuple) else c
    return ('<div class="tscroll"><table><tr>' + "".join(f"<th>{cell(h)}</th>" for h in head) + "</tr>"
            + "".join("<tr>" + "".join(f"<td>{cell(c)}</td>" for c in r) + "</tr>" for r in rows) + "</table></div>")


def h3(en, nl):
    return f'<h3 style="margin-top:22px">{t(en, nl)}</h3>'


def smelt(src, dst, en, nl, how_en="furnace", how_nl="oven"):
    return rcard(en, nl, f'<div class="recipe"><span class="slot">{icon(src, src.replace("_", " "))}</span><span class="arrow" aria-hidden="true"></span>'
                         f'<span class="slot big">{icon(dst, dst.replace("_", " "))}</span><span class="tag">{t(how_en, how_nl)}</span></div>')


def diepzee_part():
    """2.7.0: the Diepe Guhzee (tools/features/diepzee.py) with one Guhbubbel in the middle of every sea (onderwater.py)."""
    return h3("De Diepe Guhzee", "De Diepe Guhzee") + \
        entry(img("diepe_guhzee", "De Diepe Guhzee"), "A big, deep guh sea", "Een grote, diepe guhzee",
              p("Until now the Guhmension only had its shallow pink pools. Now it has real seas: the <b>Diepe Guhzee</b> (<code>guhs:diepe_guhzee</code>), big round "
                "seas of a few hundred blocks, <b>28 blocks deep</b> (the surface at y 62, a flat bottom at y 34 with low dunes), in deep pink water under the pink guh sky. "
                "The bottom is <b>sand</b> with patches of pink terracotta and clay; on it grow <b>kaaskoraal</b> (it blows air bubbles: swim to it when you run out of air), "
                "corals, seagrass, kelp and sea pickles. Schools of <b>guhvisjes</b> swim around, and now and then a wild <b>Zeemeerguh</b>. Every sea has a <b>dam</b> "
                "around it, just one block higher than the water, with a sandy <b>beach</b> where guhs walk: valleys that run into the sea are closed off, so the water "
                "never leaks into the land. Under the sea there are no cheese holes, so no gatenkaas caves leak either. About 8 of every 100 blocks of the Guhmension are "
                "deep sea.",
                "Tot nu toe had de Guhmensie alleen haar ondiepe roze poeltjes. Nu heeft hij echte zeeën: de <b>Diepe Guhzee</b> (<code>guhs:diepe_guhzee</code>), grote "
                "ronde zeeën van een paar honderd blokken, <b>28 blokken diep</b> (het wateroppervlak op y 62, een vlakke bodem op y 34 met lage duintjes), in dieproze water "
                "onder de roze guhlucht. De bodem is <b>zand</b> met plekken roze terracotta en klei; daarop groeien <b>kaaskoraal</b> (dat blaast luchtbelletjes: zwem ernaartoe "
                "als je lucht op is), koraal, zeegras, kelp en zeeaugurken. Er zwemmen scholen <b>guhvisjes</b> en af en toe een wilde <b>Zeemeerguh</b>. Om elke zee ligt "
                "een <b>dam</b>, net één blok hoger dan het water, met een zandig <b>strandje</b> waar guhs wandelen: dalen die naar de zee lopen worden afgesloten, dus "
                "het water lekt nooit het land in. Onder de zee zitten geen kaasgaten, dus ook de gatenkaasgrotten lekken niet leeg. Ongeveer 8 van elke 100 blokken van "
                "de Guhmensie zijn diepe zee.") +
              '<p>' + ' '.join([icon("kaaskoraal", "Kaaskoraal"), icon("guh_vis", "Guhvis"), icon("guh_vis_bucket", "Emmer met guhvis"), icon("parel", "Parel")]) + '</p>',
              stats=[(("Depth", "Diepte"), t("28 blocks (y 34&ndash;62)", "28 blokken (y 34&ndash;62)")),
                     (("Advancement", "Vooruitgang"), "<i>Blub, wat diep!</i>"), (("FTB quest", "FTB-quest"), "De Diepe Guhzee")], wide=True) + \
        entry(img("structure_onderwater", "De Guhbubbel"), "One Guhbubbel in every sea", "Eén Guhbubbel in elke zee",
              p("The <b>Guhbubbel</b> of 2.5 moved house: it no longer sits in a lagoon somewhere in the shallow Guhzee, but in the <b>deep middle of every Diepe "
                "Guhzee, exactly one per sea</b>. Look for the little <b>island with the Duikpost</b> in the middle of the sea (a guh head wearing diving goggles and a "
                "snorkel, with a jetty to its mouth): walk in through the mouth and take the spiral staircase in its glass tube down to the sea floor, then the glass "
                "tunnel into the dome. The <b>bubble lift</b> next to the stairs brings you back up. Under the dome (76 blocks wide, shaped like a guh head with two ear "
                "bubbles) there's air, a kaaskoraal garden and the Zeemeerguh's diving shop; outside lie a coral reef with giant guh shells and the sunken guh ship. "
                "The <b>super compass</b> (Wonders) or <code>/locate structure guhs:onderwater</code> shows the way. (In the picture the water is left out, so you "
                "can see the dome and the island.)",
                "De <b>Guhbubbel</b> uit 2.5 is verhuisd: hij ligt niet meer in een lagune ergens in de ondiepe Guhzee, maar in het <b>diepe midden van elke Diepe "
                "Guhzee, precies één per zee</b>. Zoek het <b>eilandje met de Duikpost</b> midden in de zee (een guhhoofd met een duikbril en een snorkel, met een "
                "steiger naar zijn mond): loop door de mond naar binnen en neem de wenteltrap in de glazen buis naar de zeebodem, en dan de glazen tunnel de koepel in. "
                "De <b>bubbellift</b> naast de trap brengt je weer omhoog. Onder de koepel (76 blokken breed, in de vorm van een guhhoofd met twee oorbubbels) is lucht, "
                "een kaaskoraaltuin en het duikwinkeltje van de Zeemeerguh; buiten liggen een koraalrif met reuzenschelpen en het gezonken guhschip. Het "
                "<b>superkompas</b> (Wonderen) of <code>/locate structure guhs:onderwater</code> wijst de weg. (Op het plaatje is het water weggelaten, zodat je de "
                "koepel en het eilandje ziet.)"), wide=True) + \
        entry(img("npc_zeemeerguh", "De Zeemeerguh"), "The Zeemeerguh has a tail now", "De Zeemeerguh heeft nu een staart",
              p("The Zeemeerguh in the dome finally looks the part: no more paws, but a scaly <b>mermaid tail</b> curled in front of her on the floor, with a pink "
                "tail fin and a little fin on her back. She sits in front of her giant shell and sells the duikhelm, the diving outfit and more for pearls. Blub!",
                "De Zeemeerguh in de koepel ziet er eindelijk naar uit: geen pootjes meer, maar een geschubde <b>zeemeerminstaart</b> die voor haar op de grond krult, "
                "met een roze staartvin en een vinnetje op haar rug. Ze zit voor haar reuzenschelp en verkoopt de duikhelm, het duikpakje en meer voor parels. "
                "Blub!"))


def minigames27_part():
    """2.7.0: the Highscores tab of the Guhdex, the bigger minigame rewards (Minigames.give), the Guhdex on arrival."""
    rewards = table([("Minigame", "Minigame"), ("Pays in", "Betaalt in"), ("Per game (2.7)", "Per potje (2.7)"), ("First time ever", "De allereerste keer")], [
        [t("Vads Contest", "Vads-wedstrijd"), icon("showrozet", "Showrozet") + " showrozetten",
         t("Per round 2 / 3 / 4 / 5 (below 16, from 16, 22 and 27 points; a naked walk: 0). End of the show +2, or +5 from 65 points.",
           "Per ronde 2 / 3 / 4 / 5 (onder 16, vanaf 16, 22 en 27 punten; bloot over de catwalk: 0). Einde van de show +2, of +5 vanaf 65 punten."),
         t("+4, 3 guh balloons, 4 fried kaasknabbels", "+4, 3 guhballonnen, 4 gefrituurde kaasknabbels")],
        [t("Guhrace", "Guhrace"), icon("raceprijsje", "Raceprijsje") + " raceprijsjes",
         t("Gold 6 (under 1:12), silver 4 (under 1:25), bronze 3 (under 1:45), finished 2; beat your own record: +2.",
           "Goud 6 (onder 1:12), zilver 4 (onder 1:25), brons 3 (onder 1:45), gefinisht 2; je eigen record verbeterd: +2."),
         t("+4, 16 kaasknabbels", "+4, 16 kaasknabbels")],
        [t("Mika Whacking", "Mika meppen"), icon("mepmunt", "Mepmunt") + " mepmunten",
         t("2 + 1 per 300 points, at most 11 (not a single Mika whacked: 0).", "2 + 1 per 300 punten, hoogstens 11 (geen enkele Mika gemept: 0)."),
         t("+4, 8 kaasknabbels", "+4, 8 kaasknabbels")],
        ["Guhdisco", icon("discomunt", "Discomunt") + " discomunten",
         t("From 3 colours: 1 per 2 colours + 1 (3 colours = 2), +2 from 10 colours and another +3 from 15 (10 colours = 7, 15 = 13).",
           "Vanaf 3 kleuren: 1 per 2 kleuren + 1 (3 kleuren = 2), +2 vanaf 10 kleuren en nog +3 vanaf 15 (10 kleuren = 7, 15 = 13)."),
         t("+4, a kaasknabbel milkshake", "+4, een kaasknabbelmilkshake")],
        ["Guhgolf", icon("golfballetje", "Golfballetje") + " golfballetjes",
         t("Per hole: hole in one 6, eagle 5, birdie 4, par 3, bogey 2, worse 1 (8 strokes and not in: 0). After the round +3, +4 more at par or better, +3 more "
           "for a new record.",
           "Per hole: hole-in-one 6, eagle 5, birdie 4, par 3, bogey 2, slechter 1 (8 slagen en nog niet in de hole: 0). Na het rondje +3, nog +4 bij par of beter, "
           "nog +3 voor een nieuw record."),
         t("+6, 16 kaasknabbels, 4 fried kaasknabbels", "+6, 16 kaasknabbels, 4 gefrituurde kaasknabbels")],
        [t("Vadsig eetfestijn", "Vadsig eetfestijn"), icon("smulmunt", "Smulmunt") + " smulmunten",
         t("2 as soon as you scored, +1 at 25, 50, 80, 120, 170 and 230 points (at most 8).", "2 zodra je iets scoorde, +1 bij 25, 50, 80, 120, 170 en 230 punten (hoogstens 8)."),
         t("+4, a guh cake", "+4, een guhtaart")],
        [t("Guhfish Contest", "Guhvis-wedstrijd"), icon("visbon", "Visbon") + " visbonnen",
         t("2 + 1 per 60 points, at most 13 (stopped early: 1 less); winner +3, new record +3.", "2 + 1 per 60 punten, hoogstens 13 (eerder gestopt: 1 minder); winnaar +3, nieuw record +3."),
         t("+4, 4 baked guh fish, 16 kaasknabbels", "+4, 4 gebakken guhvissen, 16 kaasknabbels")],
        ["Verstopguh", icon("verstopguhticket", "Verstopguhticket") + t(" tickets", " tickets"),
         t("Makkelijk 2, medium 3, moeilijk 5; +2 if you find them all within 2 / 4 / 6 minutes.", "Makkelijk 2, medium 3, moeilijk 5; +2 als je ze allemaal vindt binnen 2 / 4 / 6 minuten."),
         "&ndash;"]])
    return h3("Minigames and the Guhdex", "Minigames en de Guhdex") + \
        entry(icon("guhdex", "Guhdex").replace('class="px"', 'class="px" style="width:96px;height:96px"'), "Highscores in the Guhdex", "Highscores in de Guhdex",
              p("Open the <b>Guhdex</b> and click the <b>Highscores</b> button at the top: a page with all your minigame records, one line per game with its icon. "
                "Big: <b>your own best</b> (kept for every player, also if you never made the world's top 3; records from before 2.7 count too). Smaller underneath: "
                "the <b>server record</b> and who holds it (<i>\"dat ben JIJ! VAHOEG!\"</i> if it's you). Never played? <i>\"Nog nooit gespeeld, njeg!\"</i>. "
                "The page updates as soon as a score changes. 12 lines: Vads-wedstrijd, Guhrace (total time), Guhrace: fastest lap, Mika meppen, Guhdisco, "
                "Guhgolf (9 holes), Vadsig eetfestijn, Guhvis-wedstrijd (points), heaviest fish, and Verstopguh makkelijk, medium and moeilijk. For times and golf "
                "strokes less is better, for the rest more.",
                "Open de <b>Guhdex</b> en klik bovenaan op <b>Highscores</b>: een pagina met al je minigame-records, één regel per spel met zijn icoontje. Groot: "
                "<b>je eigen beste</b> (voor elke speler bewaard, ook als je nooit in de top 3 van de wereld kwam; records van vóór 2.7 tellen ook). Kleiner eronder: "
                "het <b>serverrecord</b> en wie het heeft (<i>\"dat ben JIJ! VAHOEG!\"</i> als jij het bent). Nog nooit gespeeld? <i>\"Nog nooit gespeeld, njeg!\"</i>. "
                "De pagina werkt zich bij zodra er een score verandert. 12 regels: Vads-wedstrijd, Guhrace (totale tijd), Guhrace: snelste ronde, Mika meppen, Guhdisco, "
                "Guhgolf (9 holes), Vadsig eetfestijn, Guhvis-wedstrijd (punten), zwaarste vis, en Verstopguh makkelijk, medium en moeilijk. Bij tijden en golfslagen "
                "is minder beter, bij de rest meer."), wide=True) + \
        p("<b>More tickets and coins</b>: every reward of every minigame now gives <b>at least 1 more</b> than before (the zeros for doing nothing stay zero: no AFK "
          "farming, njeg). And if your inventory is full, the reward doesn't vanish any more: <b>what doesn't fit drops on the ground in front of you</b>.",
          "<b>Meer tickets en munten</b>: elke beloning van elke minigame geeft nu <b>minstens 1 meer</b> dan eerst (de nullen voor niksdoen blijven nul: geen "
          "AFK-boeren, njeg). En zitten je zakken vol, dan verdwijnt de beloning niet meer: <b>wat niet past valt voor je op de grond</b>.") + rewards + \
        p("<b>A Guhdex when you arrive</b>: whoever steps into the Guhmension through a portal (from the Overworld, the Barbecuether or the Guheinde) and has no "
          "Guhdex yet gets one for free: <i>\"Welkom in de Guhmensie! Hier, een Guhdex: vul hem met alle guhs die je tegenkomt. Njeg!\"</i> Full pockets? Then it "
          "lands on the ground in front of you.",
          "<b>Een Guhdex bij aankomst</b>: wie via een portaal de Guhmensie binnenstapt (uit de Overworld, de Barbecuether of het Guheinde) en nog geen Guhdex heeft, "
          "krijgt er gratis een: <i>\"Welkom in de Guhmensie! Hier, een Guhdex: vul hem met alle guhs die je tegenkomt. Njeg!\"</i> Zakken vol? Dan valt hij voor "
          "je op de grond.") + \
        p("<b>Buildings no longer grow into each other</b>: every building now claims its own space when the world is made, so a minigame hall, a village or a "
          "Guhbubbel is always generated whole (operators can check it with <code>/guhs bouwcheck</code>, see the commands below).",
          "<b>Gebouwen groeien niet meer in elkaar</b>: elk gebouw claimt nu zijn eigen ruimte als de wereld wordt gemaakt, dus een minigamehal, een dorp of een "
          "Guhbubbel wordt altijd heel gegenereerd (operators kunnen het nakijken met <code>/guhs bouwcheck</code>, zie de commando's hieronder).")


def barbecuether_section():
    """2.7.0: De Guhbarbecuether (a Nether parody) and a livelier Guhmension (Gatenkaasgrotten, Kaasmoeras, Vadswoud)."""
    K, G = "kaas_knabbels", "grillkool"
    intro = p("Guhs 2.7 opens a whole new, very hot dimension: <b>De Guhbarbecuether</b>, a guh parody of the Nether full of kaasfrituursaus, giant saté skewers and "
              "sausages. And the Guhmension itself got livelier: deep underground the <b>Gatenkaasgrotten</b> with the Mikas' secret pantry, on the surface the misty "
              "<b>Kaasmoeras</b>, the <b>Vadswoud</b> with its giant guh trees, where wild guhs now live in <b>families</b>, and the big, deep <b>Diepe Guhzee</b> with the "
              "Guhbubbel in its middle. The Guhdex got a <b>Highscores</b> tab, and every minigame now pays out more. The guhs stay what they always were: sweet and "
              "passive. Only the Mikas are, well... Mikas. Nothing is locked, everything is in the creative tab and there are 72 new FTB quests.",
              "Guhs 2.7 opent een hele nieuwe, heel hete dimensie: <b>De Guhbarbecuether</b>, een guh-parodie op de Nether vol kaasfrituursaus, reuzensatéspiezen en "
              "braadworsten. En de Guhmensie zelf is levendiger geworden: diep onder de grond de <b>Gatenkaasgrotten</b> met de geheime voorraadkelder van de Mika's, aan "
              "het oppervlak het mistige <b>Kaasmoeras</b>, het <b>Vadswoud</b> met zijn reuzenguhbomen, waar wilde guhs nu in <b>gezinnetjes</b> wonen, en de grote, diepe "
              "<b>Diepe Guhzee</b> met de Guhbubbel in het midden. De Guhdex kreeg een tabblad <b>Highscores</b>, en elke minigame betaalt nu meer uit. De guhs blijven wat ze "
              "altijd waren: lief en vredig. Alleen de Mika's zijn, tja... Mika's. Niets zit op slot, alles staat in het creatieve tabblad en er zijn 72 nieuwe FTB-quests bij.")
    parts = ul([("<b>De Guhbarbecuether</b>: the dimension, the grillkool portal, the barbecueputten and the <b>Grillguh</b>.",
                 "<b>De Guhbarbecuether</b>: de dimensie, het grillkoolportaal, de barbecueputten en de <b>Grillguh</b>."),
                ("<b>De Spiesburcht</b>: Rookguhs, Vonk- and Knekel-Mikas, trading with Nether-Mikas, the Spiesburcht and the Mika-grillpaleis, the <b>Guhbrouwketel</b>, the "
                 "<b>Knabbelbaken</b>, the Asguh and the boss: the <b>Aangebrande Mika</b>.",
                 "<b>De Spiesburcht</b>: Rookguhs, Vonk- en Knekel-Mika's, ruilen met Nether-Mika's, de Spiesburcht en het Mika-grillpaleis, de <b>Guhbrouwketel</b>, het "
                 "<b>Knabbelbaken</b>, de Asguh en de baas: de <b>Aangebrande Mika</b>."),
                ("<b>De Gatenkaasgrotten</b>: a cheese-hole cave biome, the Stille Voorraadkelder and the blind <b>Vadswaker</b>.",
                 "<b>De Gatenkaasgrotten</b>: een gatenkaas-grottenbioom, de Stille Voorraadkelder en de blinde <b>Vadswaker</b>."),
                ("<b>Het Kaasmoeras</b>: a bouncy swamp, kikkerguhs, kaasmotten, the Kaasmoerasguh and the <b>Moerasheks-Mika</b> in her hut on stilts.",
                 "<b>Het Kaasmoeras</b>: een stuiterend moeras, kikkerguhs, kaasmotten, de Kaasmoerasguh en de <b>Moerasheks-Mika</b> in haar paalhut."),
                ("<b>Het Vadswoud</b>: giant guh trees, a treehouse village, the <b>Boswachterguh</b> and the <b>Knabbelplukker</b>, guh families and guh nests.",
                 "<b>Het Vadswoud</b>: reuzenguhbomen, een boomhutdorp, de <b>Boswachterguh</b> en de <b>Knabbelplukker</b>, guhfamilies en guhnestjes."),
                ("<b>De Diepe Guhzee</b>: big deep seas with beaches, kaaskoraal and Zeemeerguhs, and in the middle of every sea the <b>Guhbubbel</b>.",
                 "<b>De Diepe Guhzee</b>: grote diepe zeeën met strandjes, kaaskoraal en Zeemeerguhs, en in het midden van elke zee de <b>Guhbubbel</b>."),
                ("<b>Minigames and Guhdex</b>: the <b>Highscores</b> tab, bigger rewards for every minigame, and a free Guhdex when you arrive.",
                 "<b>Minigames en Guhdex</b>: het tabblad <b>Highscores</b>, grotere beloningen bij elke minigame, en een gratis Guhdex bij aankomst.")])

    # ---------------------------------------------------------------- De Guhbarbecuether
    bbq = h3("De Guhbarbecuether: getting there", "De Guhbarbecuether: hoe kom je er?") + \
        entry(img("barbecuether_portaal", "Grillkool portal"), "The grillkool portal", "Het grillkoolportaal",
              ul([("<b>Grillkool</b> (the obsidian of the guhs): let <b>kaassaus</b> flow over or against a <b>block of coal</b>. Sssss... grillkool! In the Barbecuether itself a "
                   "kaasfrituursaus source next to water or kaassaus becomes grillkool too. Mine it with a diamond pickaxe.",
                   "<b>Grillkool</b> (de obsidiaan van de guhs): laat <b>kaassaus</b> over of tegen een <b>blok steenkool</b> stromen. Sssss... grillkool! In de Barbecuether zelf "
                   "wordt een bron kaasfrituursaus naast water of kaassaus ook grillkool. Hakken met een diamanten houweel."),
                  ("Build a rectangular frame of grillkool, from 4&times;5 up to 23&times;23 (corners optional), in the <b>Guhmension</b>.",
                   "Bouw een rechthoekig frame van grillkool, van 4&times;5 tot 23&times;23 (hoeken hoeven niet), in de <b>Guhmensie</b>."),
                  ("Light it with an <b>Aanmaakblokje</b> (a guh lighter, 64 uses). Anywhere else the blokje only says <i>\"Njeg...\"</i> and works as a normal lighter. "
                   "You get your first Aanmaakblokjes from the Grillguh (see below) or from the chests in the barbecueputten.",
                   "Steek het aan met een <b>Aanmaakblokje</b> (een guh-aansteker, 64 keer). Ergens anders zegt het blokje alleen <i>\"Njeg...\"</i> en werkt het als een gewone "
                   "aansteker. Je eerste Aanmaakblokjes krijg je van de Grillguh (zie hieronder) of uit de kisten in de barbecueputten."),
                  ("Just like the Nether: stand in it for 4 seconds, and <b>1 block in the Barbecuether is 8 in the Guhmension</b>. Break the frame and the portal goes out.",
                   "Net als de Nether: sta er 4 seconden in, en <b>1 blok in de Barbecuether is 8 in de Guhmensie</b>. Breek het frame en het portaal gaat uit."),
                  ("Beds don't explode there, but you won't sleep either: <i>\"Te warm om te slapen, njeg!\"</i>. Respawn anchors do work.",
                   "Bedden ontploffen er niet, maar slapen lukt ook niet: <i>\"Te warm om te slapen, njeg!\"</i>. Respawn anchors werken wel.")]), wide=True) + \
        p("A hot, smoky dimension with a ceiling, caves and pillars. Below y 32 lies a sea of <b>kaasfrituursaus</b>, the lava of the guhs: it burns like lava, gives light, "
          "flows faster in the heat and water fizzles away. Scoop it up with a bucket (carefully).",
          "Een hete, rokerige dimensie met een plafond, grotten en pilaren. Onder y 32 ligt een zee van <b>kaasfrituursaus</b>, de lava van de guhs: hij brandt als lava, "
          "geeft licht, stroomt sneller in de hitte en water verdampt er. Schep hem op met een emmer (voorzichtig).") + \
        table([("Biome", "Bioom"), ("Parody of", "Parodie op"), ("What it looks like", "Hoe het eruitziet")], [
            ["<b>Houtskoolvlakte</b>", "Nether Wastes", ("Dark orange smoke, floating sparks, eternal little fires, smeulkooltjes and patches of ash.", "Donkeroranje rookmist, zwevende vonkjes, eeuwige vuurtjes, smeulkooltjes en asplekken.")],
            ["<b>Satébos</b>", "Crimson Forest", ("A red glow, pindasaus nylium, giant saté skewers, pindascheutjes, sticky pindasaus puddles and uienlicht.", "Een rode gloed, pindasaus-nylium, reuzensatéspiezen, pindascheutjes, plakkerige pindasausplasjes en uienlicht.")],
            ["<b>Worstenwoud</b>", "Warped Forest", ("Mustard-yellow mist, mosterd nylium and giant sausages (standing, bent or lying down) with a mustard zigzag.", "Mosterdgele mist, mosterd-nylium en reuzenbraadworsten (staand, gebogen of liggend) met een mosterdzigzag.")],
            ["<b>Asdal</b>", "Soul Sand Valley", ("Grey ash mist, ash blocks that slow you down, blue ash fires and charred guh skeletons. Home of the Asguh.", "Grijze asmist, asblokken die je vertragen, blauwe asvuurtjes en verkoolde guh-skeletten. Hier woont de Asguh.")],
            ["<b>Rookdelta</b>", "Basalt Deltas", ("Thick smoke with white ash, roosterijzer, grill pillars with grates in between, sauce deltas and smoking rookgaten.", "Dikke rookmist met witte as, roosterijzer, roosterpilaren met grillroosters ertussen, saus-delta's en rokende rookgaten.")],
        ]) + \
        entry(img("structure_barbecueput_groot", "Barbecueput"), "The barbecueputten and the Grillguh", "De barbecueputten en de Grillguh",
              p("A ruined-portal parody: broken barbecues with half a grillkool frame (holes and cracked stones), charred guh statues with glowing eyes and a chest. "
                "About as rare as the guh kermis, on flat land in the Guhmension <b>and</b> on cave floors in the Barbecuether (so you can get home when your portal is gone). "
                "The <b>big put</b> (37&times;37) has a long brick barbecue with sausages and saté, an oven wall with a chimney and the corner of the <b>Grillguh</b>; the two "
                "small ones (19&times;19) have no guh. The super compass finds them (<i>Barbecue &gt; Barbecueput</i>).",
                "Een ruined-portal-parodie: kapotte barbecues met een half grillkoolframe (gaten en gebarsten stenen), verkoolde guhbeelden met gloeiende ogen en een kist. "
                "Ongeveer zo zeldzaam als de guhkermis, op vlakke plekken in de Guhmensie <b>én</b> op grotbodems in de Barbecuether (zo kom je thuis als je portaal weg is). "
                "De <b>grote put</b> (37&times;37) heeft een lange bakstenen barbecue met worsten en saté, een ovenmuur met schoorsteen en het hoekje van de <b>Grillguh</b>; de "
                "twee kleine (19&times;19) hebben geen guh. Het superkompas vindt ze (<i>Barbecue &gt; Barbecueput</i>)."), wide=True) + \
        entry(img("npc_grillguh", "Grillguh"), "The Grillguh's questline", "De questline van de Grillguh",
              "<ol>" + "".join(f"<li>{t(en, nl)}</li>" for en, nl in [
                  ("Talk to him: <i>\"NJEG, mijn barbecue! De Mika's hebben mijn Aanmaakblokjes gejat!\"</i>", "Praat met hem: <i>\"NJEG, mijn barbecue! De Mika's hebben mijn Aanmaakblokjes gejat!\"</i>"),
                  ("Repair the grillkool frame of his put (grillkool in the holes, cracked stones out) and tell him.", "Maak het grillkoolframe van zijn put weer heel (grillkool in de gaten, gebarsten stenen eruit) en zeg het hem."),
                  ("Go to a <b>Mika camp</b>: while the quest runs, every Mika you beat there drops an Aanmaakblokje (at most 3 in your pockets). Bring one back.",
                   "Ga naar een <b>Mika-kamp</b>: zolang de quest loopt, laat elke Mika die je daar verslaat een Aanmaakblokje vallen (hooguit 3 op zak). Breng er een terug."),
                  ("Light his put with it: <i>\"VAHOEG, mijn barbecue brandt weer!\"</i> You get <b>Grillguhs geheime recept</b> (to craft Aanmaakblokjes yourself; the recipe stays in the grid) and a Grillguh-koksmuts. Lost the recipe? He gives you a new one.",
                   "Steek zijn put ermee aan: <i>\"VAHOEG, mijn barbecue brandt weer!\"</i> Je krijgt <b>Grillguhs geheime recept</b> (om zelf Aanmaakblokjes te maken; het recept blijft liggen) en een Grillguh-koksmuts. Recept kwijt? Hij geeft je een nieuw.")]) + "</ol>" +
              p("After that his shop is open (for kaasknabbels):", "Daarna is zijn winkeltje open (voor kaasknabbels):") +
              table([("You get", "Je krijgt"), ("Price", "Prijs")], [
                  [icon("aanmaakblokje", "Aanmaakblokje") + " Aanmaakblokje", "12"], [icon("grillkool", "Grillkool") + " 2 grillkool", "16"],
                  [icon("gegrilde_kaasknabbelsate", "Saté") + " 2 gegrilde kaasknabbelsaté", "6"], [icon("guhbraadworst", "Guhbraadworst") + " 2 guhbraadworsten", "8"],
                  [icon("gloeikoolgruis", "Gloeikoolgruis") + " 6 gloeikoolgruis", "10"], [icon("grill_halsdoek", "Koksdoekje") + " Koksdoekje", "16"],
                  [icon("grill_koksmuts", "Grillguh-koksmuts") + " Grillguh-koksmuts", "24"], [icon("grill_schort", "Barbecueschort") + " Barbecueschort", "32"]]) +
              p("<b>Guhdex</b>: a new character page, <i>\"Een guh-kok met een torenhoge koksmuts en een schort.\"</i> (rare, big barbecueput).",
                "<b>Guhdex</b>: een nieuwe personagepagina, <i>\"Een guh-kok met een torenhoge koksmuts en een schort.\"</i> (zeldzaam, grote barbecueput)."), wide=True) + \
        entry(img("guh_outfit_grill", "Chef outfit"), "Chef outfit", "Kokspakje",
              p("Real, removable guh clothes from the Grillguh's shop: Grillguh-koksmuts (head), Barbecueschort (body) and Koksdoekje (neck). Buy all three for <b>Chef Guh</b>.",
                "Echte, uittrekbare guhkleding uit het winkeltje van de Grillguh: Grillguh-koksmuts (hoofd), Barbecueschort (lijf) en Koksdoekje (nek). Koop ze alle drie voor <b>Chef Guh</b>.")) + \
        '<div class="cards">' + "".join(
            f'<div class="card"><figure class="stage">{img(n, n)}</figure><h3>{nm}</h3><p>{t(den, dnl)}</p></div>' for n, nm, den, dnl in [
                ("houtskoolsteen", "Houtskoolsteen", "The netherrack of the guhs. Fire on top burns forever, in every dimension. Makes stones, stairs, slabs, walls and a stone fence.", "De netherrack van de guhs. Vuur erop brandt eeuwig, in elke dimensie. Wordt stenen, trappen, platen, muren en een stenen hek."),
                ("gebeitelde_houtskoolsteen_stenen", "Gebeitelde houtskoolsteen stenen", "With a guh face chiselled in. Two slabs on top of each other.", "Met een guh-gezicht erin gebeiteld. Twee platen op elkaar."),
                ("roosterijzer", "Roosterijzer", "The basalt of the Rookdelta: polished, as a pillar and as roosterijzer tralies (grates).", "De basalt van de Rookdelta: gepolijst, als pilaar en als roosterijzer tralies."),
                ("gloeikool", "Gloeikool", "Glows on the ceiling (light 15). Drops 2&ndash;4 gloeikoolgruis (Fortune helps), with Silk Touch the block.", "Gloeit aan het plafond (licht 15). Geeft 2&ndash;4 gloeikoolgruis (geluk helpt), met zijdezacht het blok."),
                ("as_blok", "Asblok", "Slows you down like soul sand; blue fire burns on it. Also as aarde.", "Vertraagt je als zielenzand; blauw vuur brandt erop. Ook asaarde."),
                ("pindasaus_nylium", "Pindasaus- and mosterd-nylium", "Bone meal grows scheutjes and zwammetjes; bone meal on a zwammetje on its own nylium grows a giant saté skewer or sausage.", "Beenmeel laat scheutjes en zwammetjes groeien; beenmeel op een zwammetje op zijn eigen nylium laat een reuzensatéspies of reuzenbraadworst groeien."),
                ("sate_stam", "Satéstam and worststam", "The trunks of the giant skewers and sausages, with sate vlees, mosterdblok and uienlicht (light) as their 'leaves'.", "De stammen van de reuzenspiezen en -worsten, met satévlees, mosterdblok en uienlicht (licht) als 'bladeren'."),
                ("rookgat", "Rookgat", "Smokes like a signal fire. Roosterijzer tralies on top of gloeikool.", "Rookt als een signaalvuur. Roosterijzer tralies boven op gloeikool."),
                ("houtskoolsteen_kaasknabbelerts", "Houtskoolsteen-kaasknabbelerts", "Drops 2&ndash;6 kaasknabbels and a little experience.", "Geeft 2&ndash;6 kaasknabbels en wat ervaring."),
                ("verkoold_guhbot", "Verkoold guhbot", "Charred guh bones in the Asdal. Poor guh.", "Verkoolde guhbotjes in het Asdal. Arme guh."),
            ]) + "</div>" + \
        p("<b>Food</b>: a stick and 3 kaasknabbels make a <b>kaasknabbelsaté</b>; grill it (furnace, smoker or campfire) into <b>gegrilde kaasknabbelsaté</b>. The <b>guhbraadworst</b> "
          "sometimes makes you fire-proof for a moment. <b>Drops</b>: gloeikool 2&ndash;4 gloeikoolgruis, kaasknabbelerts 2&ndash;6 knabbels, nylium gives houtskoolsteen "
          "(Silk Touch: itself), everything else drops itself.",
          "<b>Eten</b>: een stokje en 3 kaasknabbels worden een <b>kaasknabbelsaté</b>; gril hem (oven, rookoven of kampvuur) tot <b>gegrilde kaasknabbelsaté</b>. De "
          "<b>guhbraadworst</b> maakt je soms even vuurbestendig. <b>Drops</b>: gloeikool 2&ndash;4 gloeikoolgruis, kaasknabbelerts 2&ndash;6 knabbels, nylium geeft "
          "houtskoolsteen (zijdezacht: zichzelf), de rest geeft zichzelf.") + \
        '<p>' + " ".join(icon(n, n) for n in ("aanmaakblokje", "grillguh_recept", "kaasfrituursaus_bucket", "gloeikoolgruis", "kaasknabbelsate",
                                               "gegrilde_kaasknabbelsate", "guhbraadworst")) + '</p>'

    # ---------------------------------------------------------------- De Spiesburcht
    sb = h3("De Spiesburcht: creatures of the Barbecuether", "De Spiesburcht: wezens van de Barbecuether") + \
        entry(img("rookguh", "Rookguh"), "Rookguh", "Rookguh",
              p("The ghast of the guhs: a real little ghast (3/4 of the size, about 3 blocks) with a flat <b>guh face</b> (big glossy eyes, a snoet, a blush) that floats "
                "sadly around the Rookdelta and the Houtskoolvlakte, its nine tentacles swaying. Always peaceful, and you <b>can't hurt it</b>. "
                "Feed it <b>6 kaasknabbels</b> (right-click, or throw them at it: it comes to get them). With every knabbel it gets rounder and pinker, and then it shouts "
                "<i>\"VAHOEG!\"</i> and floats home through the ceiling. Its own Guhdex page (since 2.10.1) counts the Rookguhs you saved.",
                "De ghast van de guhs: een echte kleine ghast (3/4 zo groot, zo'n 3 blokken) met een plat <b>guhgezicht</b> (grote glanzende ogen, een snoetje, blosjes) die "
                "zielig door de Rookdelta en de Houtskoolvlakte zweeft, met zijn negen tentakels wiebelend. Altijd vredig, en je <b>kunt hem geen pijn doen</b>. "
                "Voer hem <b>6 kaasknabbels</b> (rechtsklik, of gooi ze naar hem: hij komt ze zelf halen). Bij elke knabbel wordt hij ronder en roziger, en dan roept hij "
                "<i>\"VAHOEG!\"</i> en zweeft hij door het plafond naar huis. Zijn eigen Guhdex-pagina (sinds 2.10.1) telt hoeveel Rookguhs je hebt gered."),
              stats=[(("Health", "Levens"), t("20 (can't be hurt)", "20 (onkwetsbaar)")), (("Where", "Waar"), "Rookdelta, Houtskoolvlakte, Asdal")]) + \
        entry(img("vonk_mika", "Vonk-Mika"), "Vonk-Mika", "Vonk-Mika",
              p("The blaze of the Mikas: floats and throws three glowing coals in a row. They singe you, but never set blocks on fire. Lives at its spawner in the Spiesburcht, "
                "sometimes in the Rookdelta and the Houtskoolvlakte.",
                "De blaze van de Mika's: zweeft en gooit drie gloeiende kooltjes achter elkaar. Die schroeien je, maar steken nooit blokken aan. Woont bij zijn spawner in de "
                "Spiesburcht, soms in de Rookdelta en de Houtskoolvlakte."),
              stats=[(("Health", "Levens"), "20"), (("Drops", "Laat vallen"), icon("grillspies", "Grillspies") + icon("gloeikoolgruis", "Gloeikoolgruis") + " " + t("0&ndash;1 grillspies, gloeikoolgruis", "0&ndash;1 grillspies, gloeikoolgruis"))]) + \
        entry(img("knekel_mika", "Knekel-Mika"), "Knekel-Mika", "Knekel-Mika",
              p("A tall, charred Mika skeleton with a hot grill fork: its stab sets you on fire for 2 seconds. In the Spiesburcht and the Asdal.",
                "Een lang, verkoold Mika-skelet met een hete grillvork: zijn steek zet je 2 seconden in brand. In de Spiesburcht en het Asdal."),
              stats=[(("Health", "Levens"), "24"), (("Damage", "Schade"), "6"),
                     (("Drops", "Laat vallen"), icon("verkoolde_mikakop", "Verkoolde mikakop") + " " + t("coal, charred guh bones, a <b>verkoolde mikakop</b> (2.5%, looting helps)", "steenkool, verkoolde guhbotten, een <b>verkoolde mikakop</b> (2,5%, plundering helpt)"))]) + \
        entry(img("nether_mika", "Nether-Mika"), "Trading with Nether-Mikas", "Ruilen met Nether-Mika's",
              ul([("Wear a piece of <b>vahoege vads</b> gear and Nether-Mikas leave you alone.", "Draag een stuk <b>vahoege vads</b>-uitrusting en Nether-Mika's laten je met rust."),
                  ("Hit one and every Nether-Mika within 16 blocks is angry at you for 30 seconds, vads or not.", "Sla je er een, dan zijn alle Nether-Mika's binnen 16 blokken 30 seconden boos op je, vads of niet."),
                  ("Give one a <b>vahoege vads ingot</b> (or throw it at its feet): it holds it up, sniffs it for 5 seconds and throws you loot: potions, grillkool, an Aanmaakblokje, knabbels, gloeikoolgruis, sausages, ender pearls...",
                   "Geef er een een <b>vahoege vads-staaf</b> (of gooi hem voor zijn voeten): hij houdt hem omhoog, snuffelt 5 seconden en gooit je buit toe: drankjes, grillkool, een Aanmaakblokje, knabbels, gloeikoolgruis, braadworsten, enderparels...")])) + \
        entry(img("guh_variant_asguh", "Asguh"), "Asguh", "Asguh",
              p("A new guh variant, only in the Asdal: grey and sooty, with <b>glowing cheeks</b>. Fire and kaasfrituursaus don't bother it. Sweet and tameable with kaasknabbels; "
                "give it a knabbel and it <i>glows with joy</i> (hearts and little flames). Guhdex page with a star.",
                "Een nieuwe guhvariant, alleen in het Asdal: grijs en roetig, met <b>gloeiende wangetjes</b>. Vuur en kaasfrituursaus doen hem niets. Lief en tembaar met "
                "kaasknabbels; geef hem een knabbel en hij <i>gloeit van blijdschap</i> (hartjes en vlammetjes). Guhdex-pagina met ster.")) + \
        table([("Biome", "Bioom"), ("Monsters", "Monsters"), ("Creatures", "Wezens")], [
            ["Houtskoolvlakte", "Nether-Mika, Vonk-Mika", "Rookguh"], ["Satébos", "Nether-Mika", "&ndash;"], ["Worstenwoud", "Nether-Mika", "&ndash;"],
            ["Asdal", "Nether-Mika, Knekel-Mika", t("Asguh, Rookguh (rare)", "Asguh, Rookguh (zelden)")], ["Rookdelta", "Nether-Mika, Vonk-Mika", "Rookguh"]]) + \
        h3("The Spiesburcht and the Mika-grillpaleis", "De Spiesburcht en het Mika-grillpaleis") + \
        entry(img("structure_spiesburcht", "Spiesburcht"), "Spiesburcht", "Spiesburcht",
              p("The fortress of the Barbecuether (121&times;66&times;121): a castle of houtskoolsteen stones on the height where the cave is most open. Giant guh faces with "
                "gloeikool eyes on all four sides, corner towers with Mika heads, a hall with saté pillars and fire bowls, a <b>pindasaus garden</b> in the open courtyard (with a "
                "guh scarecrow) and four long bridges on arches down into the sauce, with brug towers and a <b>Vonk-Mika spawner</b>. Vonk-, Knekel- and Nether-Mikas spawn inside. "
                "Super compass: <i>Barbecue &gt; Spiesburcht</i>.",
                "Het fort van de Barbecuether (121&times;66&times;121): een burcht van houtskoolsteen stenen op de hoogte waar de grot het meest open is. Reusachtige "
                "guhgezichten met gloeikool-ogen aan alle vier de kanten, hoektorentjes met Mika-koppen, een hal met satépilaren en vuurschalen, een <b>pindasaus-tuintje</b> "
                "op de open binnenplaats (met een guh-vogelverschrikker) en vier lange bruggen op bogen tot in de saus, met brugtorens en een <b>Vonk-Mika-spawner</b>. Binnen "
                "spawnen Vonk-, Knekel- en Nether-Mika's. Superkompas: <i>Barbecue &gt; Spiesburcht</i>."), wide=True) + \
        entry(img("structure_mika_grillpaleis", "Mika-grillpaleis"), "Mika-grillpaleis", "Mika-grillpaleis",
              p("The bastion of the Mikas (73&times;52&times;73): roosterijzer on a foundation rising out of the sauce sea, with a moat of kaasfrituursaus, a gatehouse with a "
                "giant Mika face, four horned towers, a treasure hall with a <b>mountain of stolen kaasknabbels</b> and vads treasure chests, an empty Mika throne, cages with "
                "stolen cuddly guhs, a grill kitchen and 14 Nether-Mikas. Wear vads and they let you look around...",
                "Het bastion van de Mika's (73&times;52&times;73): roosterijzer op een fundament dat uit de sauszee oprijst, met een slotgracht van kaasfrituursaus, een "
                "poortgebouw met een reusachtig Mika-gezicht, vier gehoornde torens, een schatzaal met een <b>berg gestolen kaasknabbels</b> en vads-schatkisten, een lege "
                "Mika-troon, kooien met gejatte knuffelguhs, een grillkeuken en 14 Nether-Mika's. Draag vads, dan mag je rondkijken..."), wide=True) + \
        h3("Boss: the Aangebrande Mika", "Baas: de Aangebrande Mika") + \
        entry(img("aangebrande_mika", "Aangebrande Mika"), "The Aangebrande Mika", "De Aangebrande Mika",
              ul([("<b>Call him, in any dimension</b>: a T of 4 <b>ash blocks</b> (or as aarde) with 3 <b>verkoolde mikakoppen</b> on top, standing or on the wall. Just like the wither.",
                   "<b>Oproepen, in elke dimensie</b>: een T van 4 <b>asblokken</b> (of asaarde) met 3 <b>verkoolde mikakoppen</b> erop, staand of aan de muur. Net als de wither."),
                  ("First he bakes up for 7 seconds and can't be hurt. Then: <i>\"WIE HEEFT MIJ... AANGEBRAND?!\"</i> and he pushes everyone away (no damage).",
                   "Eerst bakt hij 7 seconden op en kan hij geen schade krijgen. Dan: <i>\"WIE HEEFT MIJ... AANGEBRAND?!\"</i> en hij duwt iedereen weg (zonder schade)."),
                  ("He floats above you and fires volleys of 3 slow burning coals (5 damage + 3 s of fire), announced with hissing and glowing heads.",
                   "Hij zweeft boven je en schiet salvo's van 3 trage brandende kooltjes (5 schade + 3 s brand), aangekondigd met sissen en gloeiende koppen."),
                  ("Below half: <b>doorgebakken</b>. He calls 2 Vonk-Mikas once and shoots faster, but after every third volley he has to catch his breath low above the ground: then he takes 1.5&times; damage.",
                   "Onder de helft: <b>doorgebakken</b>. Hij roept één keer 2 Vonk-Mika's en schiet sneller, maar na elk derde salvo moet hij laag bij de grond uithijgen: dan krijgt hij 1,5&times; schade."),
                  ("He never breaks blocks, never sets anything on fire and only attacks players. <i>\"NJEG... IK BEN... DOORGEBAKKEN!\"</i>",
                   "Hij breekt nooit blokken, steekt niets aan en valt alleen spelers aan. <i>\"NJEG... IK BEN... DOORGEBAKKEN!\"</i>")]),
              stats=[(("Health", "Levens"), t("300, boss bar", "300, baasbalk")),
                     (("Drops", "Laat vallen"), icon("gloeister", "Gloeister") + icon("gloeikoolgruis", "Gloeikoolgruis") + icon("grillkool", "Grillkool") + " " + t("the <b>gloeister</b> (always), 4&ndash;8 gloeikoolgruis, 1&ndash;3 grillkool", "de <b>gloeister</b> (altijd), 4&ndash;8 gloeikoolgruis, 1&ndash;3 grillkool"))], wide=True) + \
        entry(img("aangebrande_mika_t", "The T with three heads"), "Calling him", "Oproepen",
              p("Four ash blocks in a T, three charred Mika heads on top. You only need the heads from Knekel-Mikas: collecting three is a quest of its own (<b>Hoofdzaak</b>).",
                "Vier asblokken in een T, drie verkoolde mikakoppen erop. De koppen krijg je alleen van Knekel-Mika's: er drie verzamelen is een quest op zich (<b>Hoofdzaak</b>).")) + \
        h3("The Guhbrouwketel (brewing)", "De Guhbrouwketel (brouwen)") + \
        entry(img("guhbrouwketel_vahoegheid_3", "Guhbrouwketel"), "The Guhbrouwketel", "De Guhbrouwketel",
              "<ol>" + "".join(f"<li>{t(en, nl)}</li>" for en, nl in [
                  ("<b>Stoke</b>: put in <b>grillspiespoeder</b>. One portion of powder is good for 4 brews (up to 20).", "<b>Stoken</b>: doe er <b>grillspiespoeder</b> in. Eén keer poeder is goed voor 4 brouwsels (tot 20)."),
                  ("<b>Sauce</b>: a bucket of kaassaus gives 3 portions of kaasbouillon.", "<b>Saus</b>: een emmer kaassaus geeft 3 porties kaasbouillon."),
                  ("<b>Ingredient</b>: stir one in. After 10 seconds of bubbling the brew is ready.", "<b>Ingrediënt</b>: roer er een door. Na 10 seconden pruttelen is het brouwsel klaar."),
                  ("<b>Fill</b>: glass bottles take out 3 potions. An empty hand shows the status.", "<b>Vullen</b>: met glazen flesjes haal je er 3 drankjes uit. Met een lege hand zie je de status.")]) + "</ol>" +
              table([("Potion", "Drankje"), ("Ingredient", "Ingrediënt"), ("Effect", "Effect")], [
                  [icon("drankje_van_vahoegheid", "Vahoegheid") + " Drankje van Vahoegheid", icon(K, "Kaasknabbels") + icon("gefrituurde_kaasknabbels", "Gefrituurd") + t(" kaasknabbels or fried ones", " kaasknabbels of gefrituurde"), t("Speed II 3:00 + saturation", "Snelheid II 3:00 + verzadiging")],
                  [icon("rookloopdrankje", "Rookloop") + " Rookloopdrankje", icon("gloeikoolgruis", "Gloeikoolgruis") + icon("guhbraadworst", "Braadworst") + t(" gloeikoolgruis or a guhbraadworst", " gloeikoolgruis of een guhbraadworst"), t("Fire resistance 3:00", "Vuurbestendigheid 3:00")],
                  [icon("sluipknabbeldrankje", "Sluipknabbel") + " Sluipknabbeldrankje", icon("moeraskaas", "Moeraskaas") + icon("mika_vet", "Mika's vet") + t(" moeraskaas or Mika's vet", " moeraskaas of Mika's vet"), t("<b>Stil</b> 2:00 (nobody hears you, see the Gatenkaasgrotten)", "<b>Stil</b> 2:00 (niemand hoort je, zie de Gatenkaasgrotten)")],
                  [icon("guhsprongdrankje", "Guhsprong") + " Guhsprongdrankje", icon("guh_slimeball", "Guhslijmbal") + t(" guh slimeball", " guhslijmbal"), t("Jump boost II + slow falling 1:30", "Sprongkracht II + traag vallen 1:30")]]),
              wide=True) + \
        h3("The Knabbelbaken", "Het Knabbelbaken") + \
        entry(img("knabbelbaken", "Knabbelbaken"), "The Knabbelbaken", "Het Knabbelbaken",
              p("The beacon of the guhs, made with the <b>gloeister</b>. Put it on a pyramid of 1&ndash;4 layers of <b>blocks of kaasknabbels</b>, compressed super vahoege vads "
                "or gatenkaas. Every 4 seconds <b>players and tamed guhs</b> within 10 + 10&times;layers blocks get the chosen effect (wild guhs don't). Right-click to pick the "
                "effect; the beam takes its colour and shoots all the way up to the sky (you see it from far away). With 4 layers VAHOEG and Guhsprong get stronger.",
                "Het baken van de guhs, gemaakt met de <b>gloeister</b>. Zet het op een piramide van 1&ndash;4 lagen <b>blokken kaasknabbels</b>, compressed super vahoege vads "
                "of gatenkaas. Elke 4 seconden krijgen <b>spelers en getemde guhs</b> binnen 10 + 10&times;lagen blokken het gekozen effect (wilde guhs niet). Rechtsklik om "
                "het effect te kiezen; de straal krijgt zijn kleur en schiet helemaal tot in de hemel (die zie je van ver). Met 4 lagen worden VAHOEG en Guhsprong sterker.") +
              table([("Effect", "Effect"), ("What", "Wat"), ("From", "Vanaf")], [
                  ["<b>VAHOEG</b>", t("Speed + saturation", "Snelheid + verzadiging"), t("1 layer", "1 laag")],
                  ["<b>Guhsprong</b>", t("Jump boost", "Sprongkracht"), t("2 layers", "2 lagen")],
                  ["<b>Vadsschild</b>", t("Resistance", "Weerstand"), t("3 layers", "3 lagen")],
                  ["<b>Knabbelherstel</b>", t("Regeneration", "Regeneratie"), t("4 layers", "4 lagen")]]), wide=True)

    # ---------------------------------------------------------------- De Gatenkaasgrotten
    gk = h3("De Gatenkaasgrotten", "De Gatenkaasgrotten") + \
        entry(img("gatenkaas", "Gatenkaas"), "Deep under the Guhmension", "Diep onder de Guhmensie",
              p("Below about y 36 (and at least 15 blocks under the surface) the ground of the Guhmension is sometimes one big <b>gatenkaas</b>: round cheese holes that run "
                "into each other, cheese stalactites and stalagmites that drip kaassaus, glowing <b>kaasmos</b>, kaassaus puddles and cheese-coloured dust in the air. It never "
                "reaches the surface. Mine <b>kaaskorrelerts</b> for crunchy <b>kaaskorrels</b>. Look out: a stalactite falls when you mine the ceiling (au!). "
                "<code>/execute in guhs:guhmension run locate biome guhs:gatenkaasgrotten</code>",
                "Onder ongeveer y 36 (en minstens 15 blokken onder het oppervlak) is de grond van de Guhmensie soms één grote <b>gatenkaas</b>: ronde kaasgaten die in elkaar "
                "overlopen, kaasstalactieten en -stalagmieten die kaassaus druppelen, gloeiend <b>kaasmos</b>, kaassausplasjes en kaaskleurige stofjes in de lucht. Het komt "
                "nooit aan het oppervlak. Hak <b>kaaskorrelerts</b> voor knapperige <b>kaaskorrels</b>. Pas op: een stalactiet valt als je het plafond weghakt (au!). "
                "<code>/execute in guhs:guhmension run locate biome guhs:gatenkaasgrotten</code>")) + \
        '<div class="cards">' + "".join(
            f'<div class="card"><figure class="stage">{img(n, n)}</figure><h3>{nm}</h3><p>{t(den, dnl)}</p></div>' for n, nm, den, dnl in [
                ("gatenkaas_stenen", "Gatenkaasstenen", "Building stone (with stairs, slab and wall). Smelt into belegen kaasstenen.", "Bouwsteen (met trap, plaat en muur). Smelt tot belegen kaasstenen."),
                ("belegen_kaas_tegels", "Belegen kaasstenen and -tegels", "Dark, old cheese: the stone of the Voorraadkelder.", "Donkere, oude kaas: de steen van de Voorraadkelder."),
                ("kaasmos", "Gloeiend kaasmos", "Light 9 (the carpet: 6). Moss block + kaasknabbels.", "Licht 9 (het tapijt: 6). Mosblok + kaasknabbels."),
                ("kaaskorrelerts", "Kaaskorrelerts", "Rare, only in gatenkaas. 1&ndash;2 kaaskorrels (Fortune works).", "Zeldzaam, alleen in gatenkaas. 1&ndash;2 kaaskorrels (geluk werkt)."),
                ("knabbelsensor", "Knabbelsensor", "A lump of old cheese with two listening guh ears. Only with Silk Touch.", "Een klompje oude kaas met twee luisterende guhoren. Alleen met zijdezacht."),
                ("knabbelschreeuwer", "Knabbelschreeuwer", "A Mika alarm with a wide-open mouth full of teeth. Only with Silk Touch.", "Een Mika-alarm met een wijd open tandenmond. Alleen met zijdezacht."),
            ]) + "</div>" + \
        entry(icon("stille_knabbel", "Stille knabbel").replace('class="px"', 'class="px" style="width:96px;height:96px"'), "The stille knabbel and the effect Stil", "De stille knabbel en het effect Stil",
              p("Kaaskorrel + kaasknabbels + kaasmos = 2 <b>stille knabbels</b>. Eat one (always possible) for 90 seconds of <b>Stil</b>: knabbelsensors don't hear you walking, "
                "eating, mining or building, the Vadswaker can't hear or smell you, and his anger fades six times as fast. The Sluipknabbeldrankje gives Stil too.",
                "Kaaskorrel + kaasknabbels + kaasmos = 2 <b>stille knabbels</b>. Eet er een (kan altijd) voor 90 seconden <b>Stil</b>: knabbelsensoren horen je niet lopen, "
                "eten, hakken of bouwen, de Vadswaker hoort en ruikt je niet, en zijn boosheid zakt zes keer zo snel. Het Sluipknabbeldrankje geeft ook Stil.")) + \
        entry(img("structure_gatenkaas_mijnschacht", "Mijnschacht"), "The abandoned cheese mine shaft", "De verlaten kaasmijnschacht",
              p("Small and fairly common in the Gatenkaasgrotten: two crossing corridors with old planks, props (some caved in), a rail with a <b>chest minecart</b>, a room with "
                "a chest and barrels, a caved-in shaft with a ladder and a sign: <i>\"SCHACHT 7 - DICHT! Te veel gaten, njeg!\"</i> Super compass: <i>Ondergrond</i>.",
                "Klein en vaak te vinden in de Gatenkaasgrotten: twee kruisende gangen met oude planken, stutten (sommige ingezakt), een spoor met een <b>kistkarretje</b>, een "
                "kamer met een kist en tonnen, een ingestorte schacht met een ladder en een bord: <i>\"SCHACHT 7 - DICHT! Te veel gaten, njeg!\"</i> Superkompas: <i>Ondergrond</i>.")) + \
        entry(img("structure_stille_voorraadkelder", "Stille Voorraadkelder"), "The Stille Voorraadkelder", "De Stille Voorraadkelder",
              p("The Mikas' secret pantry (an Ancient City parody, 96&times;30&times;96, rare, only in the Gatenkaasgrotten). A huge round cheese cave with, in the middle, "
                "the <b>Mika-voorraadschuur</b>: a giant Mika head (the open mouth is the door; <i>a Mika has two faces</i>, so the back has one too) full of stolen kaasknabbel "
                "blocks, with the lore book <b>Het dagboek van de Voorraadmika</b> on a golden lectern. Around it a moat of kaassaus, a ring road with 8 pantry huts, 4 stolen "
                "guh statues (<i>\"GEJAT! uit Guhdorp\"</i>) and the sleeping pit of the Vadswaker (<i>\"SSSST!!!\"</i>). Guarded by 44 knabbelsensors and 9 knabbelschreeuwers.",
                "De geheime voorraadkelder van de Mika's (een Ancient City-parodie, 96&times;30&times;96, zeldzaam, alleen in de Gatenkaasgrotten). Een enorme ronde kaasgrot "
                "met in het midden de <b>Mika-voorraadschuur</b>: een reuzen-Mikahoofd (de open mond is de deur; <i>een Mika heeft twee gezichten</i>, dus de achterkant ook) vol "
                "gestolen kaasknabbelblokken, met het lore-boek <b>Het dagboek van de Voorraadmika</b> op een gouden lessenaar. Eromheen een gracht van kaassaus, een ringstraat "
                "met 8 voorraadhutjes, 4 gejatte guhbeelden (<i>\"GEJAT! uit Guhdorp\"</i>) en de slaapkuil van de Vadswaker (<i>\"SSSST!!!\"</i>). Bewaakt door 44 "
                "knabbelsensoren en 9 knabbelschreeuwers."), wide=True) + \
        ul([("You make noise when you <b>walk</b> (not sneaking), <b>chew</b> or <b>mine/place</b> a block. Every knabbelsensor within 8 blocks lights up its ears; a knabbelschreeuwer near it screams <i>\"NJEEEG!\"</i> and gives you a <b>warning</b> (at most 1 per 10 seconds, forgotten after 10 minutes of silence).",
             "Je maakt geluid als je <b>loopt</b> (niet sluipend), <b>kauwt</b> of een blok <b>hakt of plaatst</b>. Elke knabbelsensor binnen 8 blokken laat zijn oren gloeien; een knabbelschreeuwer ernaast schreeuwt <i>\"NJEEEG!\"</i> en geeft je een <b>waarschuwing</b> (hooguit 1 per 10 seconden, vergeten na 10 minuten stilte)."),
            ("1/3: <i>\"NJEEEG! Diep onder de kaas spitst iemand zijn oren...\"</i> 2/3: <i>\"...Sluip, guh!\"</i> 3/3: <i>\"De Vadswaker komt eraan!\"</i>",
             "1/3: <i>\"NJEEEG! Diep onder de kaas spitst iemand zijn oren...\"</i> 2/3: <i>\"...Sluip, guh!\"</i> 3/3: <i>\"De Vadswaker komt eraan!\"</i>")]) + \
        entry(img("vadswaker", "Vadswaker"), "The Vadswaker", "De Vadswaker",
              p("A giant Mika the colour of old cheese, with glowing cheese holes in his fur, huge dish ears and a <b>sleeping mask</b> with a pink heart. He is blind, but he "
                "<b>hears</b> you: steps, mining, building and above all <b>chewing</b>. Now and then he sniffs (sneaking helps a bit). Close by he does a <b>NJEG roar</b> "
                "(a shock wave straight through armour). He never breaks blocks, and after 60 seconds of silence he digs himself back in. Honestly: a real guh just sneaks away.",
                "Een reuzenmika in de kleur van oude kaas, met gloeiende kaasgaatjes in zijn vacht, enorme schotelooren en een <b>slaapmasker</b> met een roze hartje. Hij is "
                "blind, maar hij <b>hoort</b> je: stappen, hakken, bouwen en vooral <b>kauwen</b>. Af en toe snuffelt hij (sluipen helpt een beetje). Van dichtbij doet hij een "
                "<b>NJEG-brul</b> (een schokgolf dwars door pantser). Hij breekt nooit blokken, en na 60 seconden stilte graaft hij zich weer in. Eerlijk: een echte guh sluipt gewoon weg."),
              stats=[(("Health", "Levens"), "300"), (("Damage", "Schade"), t("16 per hit, roar 10", "16 per klap, brul 10")),
                     (("Drops", "Laat vallen"), icon("kaaskorrel", "Kaaskorrel") + icon("vahoege_vads_ingot", "Vads") + icon("stille_knabbel", "Stille knabbel") + " " + t("6&ndash;10 kaaskorrels, 2&ndash;3 vads ingots, 16&ndash;32 knabbels, 2&ndash;4 stille knabbels", "6&ndash;10 kaaskorrels, 2&ndash;3 vadsstaven, 16&ndash;32 knabbels, 2&ndash;4 stille knabbels")),
                     (("Guhdex", "Guhdex"), t("found within 12 blocks", "gevonden binnen 12 blokken"))]) + \
        p("<b>Het dagboek van de Voorraadmika</b> (5 pages, 2 quiz questions) is always in the big chest in the schuur and counts for the Bibliothecaris' collection: "
          "now 13 books and 26 quiz questions. The chests also hold stille knabbels, kaaskorrels, goudkaas, diamonds, the music disc and (rarely) an enchanted golden apple.",
          "<b>Het dagboek van de Voorraadmika</b> (5 bladzijden, 2 kwisvragen) zit altijd in de grote kist in de schuur en telt mee voor de verzameling van de Bibliothecaris: "
          "nu 13 boeken en 26 kwisvragen. In de kisten zitten ook stille knabbels, kaaskorrels, goudkaas, diamanten, de muziekplaat en (zelden) een betoverde gouden appel.")

    # ---------------------------------------------------------------- Het Kaasmoeras
    km = h3("Het Kaasmoeras", "Het Kaasmoeras") + \
        entry(img("borrelende_kaassaus", "Borrelende kaassaus"), "A bouncy cheese swamp", "Een stuiterend kaasmoeras",
              p("A misty, bubbling swamp on flat, wet land in the Guhmension (about 4% of the surface): olive-yellow grass, murky cheese-green water, a hazy yellow-green "
                "sky and a light mist (you still see some 60&ndash;90 blocks far). <b>Modderig kaasgras</b> on <b>kaasmodder</b> (you sink in a little), swamp oaks with vines, tall <b>kaasriet</b>, pink "
                "guh lily pads and pools of <b>borrelende kaassaus</b>: step on it and you <b>bounce ~4 blocks up</b> (falling in never hurts; sneak to wade through). On big "
                "pools a <b>knabbelvlotje</b> sometimes floats by: a little raft with a pink guh flag and a barrel of loot.",
                "Een mistig, borrelend moeras op vlak, nat land in de Guhmensie (ongeveer 4% van het oppervlak): olijfgeel gras, troebel kaasgroen water, een wazige "
                "geelgroene lucht en een lichte mist (je kijkt nog zo'n 60&ndash;90 blokken ver). <b>Modderig kaasgras</b> op <b>kaasmodder</b> (je zakt er een beetje in), moeraseiken met lianen, hoog "
                "<b>kaasriet</b>, roze guhwaterlelies en plassen <b>borrelende kaassaus</b>: stap erop en je <b>stuitert ~4 blokken omhoog</b> (vallen doet nooit pijn; sluip om "
                "erdoorheen te waden). Op grote plassen dobbert soms een <b>knabbelvlotje</b>: een vlotje met een roze guhvlag en een ton met buit.")) + \
        entry(img("kikkerguhs", "Kikkerguhs"), "Kikkerguh", "Kikkerguh",
              p("A guh that is a frog: the real guh head with its two big round ears, glossy guh eyes, a tiny snoet and a blush, on a spotted frog body with frog legs, "
                "in <b>pink, mint or yellow</b>. Always sweet. Hops, swims, croaks (its throat puffs up) and snaps up "
                "<b>kaasmotten</b> with its long tongue: then it spits out a glowing <b>motknabbel</b> in its own colour (the froglight of the guhs). Lure and breed with "
                "kaasknabbels; a baby gets the colour of one of its parents. Guhdex: <i>\"Kwaak-guh!\"</i>",
                "Een guh die een kikker is: de echte guhkop met twee grote ronde oren, glanzende guhogen, een klein snoetje en blosjes, op een gespikkeld kikkerlijfje met kikkerpootjes, "
                "in <b>roze, mint of geel</b>. Altijd lief. Hupt, zwemt, kwaakt (zijn keeltje blaast op) en hapt met zijn "
                "lange tong <b>kaasmotten</b> weg: dan spuugt hij een gloeiende <b>motknabbel</b> uit in zijn eigen kleur (de froglight van de guhs). Lokken en fokken met "
                "kaasknabbels; een kikkerguhtje krijgt de kleur van een van zijn ouders. Guhdex: <i>\"Kwaak-guh!\"</i>"),
              stats=[(("Health", "Levens"), "10"), (("Temper", "Humeur"), t("always friendly", "altijd lief"))], wide=True) + \
        entry(img("motknabbels", "Motknabbels"), "Motknabbel", "Motknabbel",
              p("A light block (15) in three colours. You only get them from kikkerguhs and from the loot of rafts and the witch's hut. Pink, mint and yellow at once: <b>Alle kikkerkleurtjes</b>.",
                "Een lichtblok (15) in drie kleuren. Je krijgt ze alleen van kikkerguhs en uit de buit van vlotjes en de heksenhut. Roze, mint en geel tegelijk: <b>Alle kikkerkleurtjes</b>.")) + \
        entry(img("kaasmot", "Kaasmot"), "Kaasmot", "Kaasmot",
              p("A tiny Mika moth (purple fluff, red eyes, devil tail, gatenkaas wings). Flutters around cheese and lamps and pecks up kaasknabbels lying on the ground "
                "(a Mika, eh). Harmless to you, and sometimes it drops a knabbel.",
                "Een piepklein Mika-motje (paars donsje, rode oogjes, duivelsstaartje, gatenkaas-vleugels). Fladdert rond kaas en lampjes en pikt kaasknabbels die op de grond "
                "liggen (een Mika, hè). Ongevaarlijk voor jou, en soms laat hij een knabbel vallen."), stats=[(("Health", "Levens"), "3")]) + \
        entry(img("guh_variant_kaasmoerasguh", "Kaasmoerasguh"), "Kaasmoerasguh", "Kaasmoerasguh",
              p("A new guh variant: green-yellow spotted, as if it rolled through the mud (it did). Only in the Kaasmoeras: 40% of the wild grown-up guhs born there. Tame it "
                "with kaasknabbels; its babies inherit the spots. Guhdex page with a star.",
                "Een nieuwe guhvariant: groen-geel gevlekt, alsof hij door de modder heeft gerold (heeft hij ook). Alleen in het Kaasmoeras: 40% van de wilde volwassen guhs die "
                "daar geboren worden. Tem hem met kaasknabbels; zijn baby's erven de vlekken. Guhdex-pagina met ster.")) + \
        entry(img("moerasheks_mika", "Moerasheks-Mika"), "Moerasheks-Mika", "Moerasheks-Mika",
              p("The witch of the Mikas: a green Mika with a big purple pointed hat (with a cheese-yellow band and a glowing star), a hooked nose with a wart and a scarf. "
                "Keeps her distance and throws <b>vadsverdrijvende drankjes</b> (8 seconds of <b>Onvahoeg</b>: 12% slower and a rumbling tummy, never damage). Below half "
                "health she nibbles her own <b>moeraskaas</b> to heal. <i>\"Njeg! Blijf van mijn moeraskaas af, vadsig ding!\"</i> Lives in her hut; rarely at night in the swamp.",
                "De heks van de Mika's: een groene Mika met een grote paarse punthoed (met een kaasgele band en een lichtgevend sterretje), een haakneus met wrat en een sjaal. "
                "Houdt afstand en gooit <b>vadsverdrijvende drankjes</b> (8 seconden <b>Onvahoeg</b>: 12% trager en een rommelend buikje, nooit schade). Onder de helft "
                "knabbelt ze haar eigen <b>moeraskaas</b> om te genezen. <i>\"Njeg! Blijf van mijn moeraskaas af, vadsig ding!\"</i> Woont in haar hut; zelden 's nachts in het moeras."),
              stats=[(("Health", "Levens"), "26"), (("Drops", "Laat vallen"), icon("moeraskaas", "Moeraskaas") + icon("vadsverdrijvend_drankje", "Drankje") + " " + t("1&ndash;2 moeraskaas, 2&ndash;5 knabbels, 35% a potion", "1&ndash;2 moeraskaas, 2&ndash;5 knabbels, 35% een drankje"))]) + \
        entry(img("structure_moerasheks_hut", "Paalhut van de Moerasheks"), "The Moerasheks' hut on stilts", "De paalhut van de Moerasheks",
              p("Only in the Kaasmoeras (super compass: <i>Avontuur</i>). A big platform on mangrove stilts above a pond, and the hut itself is the <b>head of the "
                "Moerasheks-Mika</b>: green skin with moss, angry red windows as eyes, a grin with fangs, the door in her mouth and a giant witch's hat on top with a yellow "
                "motknabbel as its star. Kikkerguh faces hang under the platform. Inside: the witch's kitchen (a big cauldron, brewing stands, mushroom jars), a pantry with "
                "<b>stolen kaasknabbels</b> and her chest (always moeraskaas and drankjes), and her bed in the attic. Outside: a jetty with a knabbelvlotje, a borrelplas "
                "(<i>\"Pas op: borrelende kaassaus! BOING!\"</i>), three kikkerguhs and a Kaasmoerasguh.",
                "Alleen in het Kaasmoeras (superkompas: <i>Avontuur</i>). Een groot platform op mangrovepalen boven een vijver, en de hut zelf is het <b>hoofd van de "
                "Moerasheks-Mika</b>: groene huid met mos, boze rode ramen als ogen, een grijns met hoektandjes, de deur in haar mond en bovenop een reuzenheksenhoed met een "
                "gele motknabbel als sterretje. Onder het platform hangen kikkerguh-gezichten. Binnen: de heksenkeuken (een grote ketel, brouwstandaards, paddenstoelpotjes), "
                "een voorraadkamer met <b>gestolen kaasknabbels</b> en haar kist (altijd moeraskaas en drankjes), en haar bed op de vliering. Buiten: een steiger met een "
                "knabbelvlotje, een borrelplas (<i>\"Pas op: borrelende kaassaus! BOING!\"</i>), drie kikkerguhs en een Kaasmoerasguh."), wide=True) + \
        p("<b>Moeraskaas</b>: stinky swamp cheese (5 hunger, 30% chance to feel a bit sick), crumbles into 3 kaasknabbels and is a brewing ingredient for the Sluipknabbeldrankje. "
          "<b>Moerasgras</b> sometimes drops kaasknabbel seeds; bone meal turns it into kaasriet.",
          "<b>Moeraskaas</b>: stinkende moeraskaas (5 honger, 30% kans op even misselijk), valt uiteen in 3 kaasknabbels en is een brouwingrediënt voor het "
          "Sluipknabbeldrankje. <b>Moerasgras</b> geeft soms kaasknabbelzaadjes; beenmeel maakt er kaasriet van.")

    # ---------------------------------------------------------------- Het Vadswoud
    vw = h3("Het Vadswoud", "Het Vadswoud") + \
        entry(img("vadshout_gezichtjes", "Guhgezichtjes in de schors"), "A forest of giant guh trees", "Een woud van reuzenguhbomen",
              p("A misty, mint-green forest in the Guhmension (about 10% of the surface; the mist is light, you see some 60&ndash;90 blocks far) under a lavender sky, with glowing <b>vadspluisjes</b> in the air and falling mint "
                "leaves. On soft <b>vadsmos</b> grow <b>reuzenguhbomen</b> (25&ndash;40 blocks tall, trunks 5&ndash;7 blocks thick with roots, branches and a huge crown) with "
                "<b>guh faces in the bark</b> looking every way, smaller vadshout trees, <b>knabbelbessenstruiken</b> (they don't prick, guhs walk right through), pink grass, "
                "flowers and <b>guhnestjes</b>. No monsters. Right-click a guh face with an empty hand for another mood: happy, sleepy, surprised or vads.",
                "Een mistig, mintgroen woud in de Guhmensie (ongeveer 10% van het oppervlak; de mist is licht, je kijkt zo'n 60&ndash;90 blokken ver) onder een lavendelkleurige lucht, met gloeiende <b>vadspluisjes</b> in de lucht "
                "en vallende mintblaadjes. Op zacht <b>vadsmos</b> groeien <b>reuzenguhbomen</b> (25&ndash;40 blokken hoog, stammen van 5&ndash;7 blokken dik met wortels, "
                "takken en een enorme kroon) met <b>guhgezichtjes in de schors</b> die alle kanten op kijken, kleinere vadshoutbomen, <b>knabbelbessenstruiken</b> (die prikken "
                "niet, guhs lopen er gewoon doorheen), roze gras, bloemetjes en <b>guhnestjes</b>. Geen monsters. Rechtsklik een guhgezichtje voor een ander humeur: blij, "
                "slaperig, verbaasd of vads."), wide=True) + \
        '<div class="cards">' + "".join(
            f'<div class="card"><figure class="stage">{img(n, n)}</figure><h3>{nm}</h3><p>{t(den, dnl)}</p></div>' for n, nm, den, dnl in [
                ("vadshout_stam", "Vadshout", "A whole wood set: stam, gestripte stam, planken, trap, plaat, hek, poort, and a deur and luik with a guh face.", "Een hele houtset: stam, gestripte stam, planken, trap, plaat, hek, poort, en een deur en luik met een guhgezichtje."),
                ("vadshout_bladeren", "Vadshoutbladeren", "Mint green with pink blossom. Drop saplings, sticks and sometimes knabbelbessen.", "Mintgroen met roze bloesem. Laten zaailingen, stokjes en soms knabbelbessen vallen."),
                ("vadshout_zaailing", "Vadshoutzaailing", "One sapling: a vadshout tree. <b>Four in a square: a giant guh tree!</b>", "Eén zaailing: een vadshoutboom. <b>Vier in een vierkant: een reuzenguhboom!</b>"),
                ("knabbelbessenstruik", "Knabbelbessenstruik", "Pick the yellow knabbelbessen (they grow back). Eat them, plant them, bake a taartje (8 hunger).", "Pluk de gele knabbelbessen (ze groeien terug). Eten, planten, of bak er een taartje van (8 honger)."),
                ("guhnestje", "Guhnestje", "Guhs sleep in it at night, with their family (up to 6 per nest).", "Guhs slapen er 's nachts in, met hun gezin (tot 6 per nestje)."),
                ("vadsmos", "Vadsmos", "Mint-green moss with pink speckles; counts as dirt.", "Mintgroen mos met roze spikkels; telt als aarde."),
            ]) + "</div>" + \
        entry(img("guh", "Guh"), "Guh families", "Guhfamilies",
              ul([("Wild guhs that spawn by themselves in the Guhmension now form a <b>family</b>: 1&ndash;2 parents and 1&ndash;3 babies, all the same variant. Bred babies join their parents' family.",
                   "Wilde guhs die vanzelf in de Guhmensie spawnen vormen nu een <b>gezin</b>: 1&ndash;2 ouders en 1&ndash;3 baby's, allemaal dezelfde variant. Gefokte baby's horen bij het gezin van hun ouders."),
                  ("Babies <b>walk in a line</b> behind their parent: baby 1 behind the parent, baby 2 behind baby 1, and so on. Adorable. Vads, even.",
                   "Babyguhs <b>lopen in een rijtje</b> achter hun ouder: baby 1 achter de ouder, baby 2 achter baby 1, enzovoort. Schattig. Vads, zelfs."),
                  ("At night every guh looks for a guhnestje within 20 blocks and sleeps in it (Zzz) with its family: it heals, and babies grow a bit. In the morning, or when hurt, they wake up.",
                   "'s Nachts zoekt elke guh een guhnestje binnen 20 blokken en slaapt erin (Zzz) met zijn gezin: dat geneest, en baby's groeien een beetje. 's Ochtends, of als ze pijn krijgen, worden ze wakker."),
                  ("Your own tamed guhs sleep in a nest too while you're within 24 blocks (not when sitting or told not to wander).",
                   "Je eigen tamme guhs gaan ook in een nestje slapen zolang jij binnen 24 blokken bent (niet als ze zitten of niet mogen rondlopen)."),
                  ("<b>Babies are easier to tame</b>: a knabbelbes tames a wild baby 1 in 2 (big guhs don't want berries), and kaasknabbels get an extra 1 in 3 chance on a baby.",
                   "<b>Babyguhs zijn makkelijker te temmen</b>: een knabbelbes temt een wilde babyguh 1 op 2 (grote guhs willen geen bessen), en kaasknabbels krijgen bij een baby een extra kans van 1 op 3.")]),
              wide=True) + \
        entry(img("structure_boomhutdorp", "Boomhutdorp"), "The treehouse village", "Het boomhutdorp",
              p("Only in the Vadswoud, about 1 per km (super compass: <i>Wonen &gt; Boomhutdorp</i>). Five giant guh trees: the <b>Moederboom</b> in the middle (a big guh face "
                "in the trunk; its mouth is the door to a hollow with a nest and a chest) and four around it. A spiral staircase leads to the <b>village square</b> around the "
                "trunk, ladders and six <b>rope bridges</b> of vadstouw lead to the decks in the other trees. Six <b>treehouses shaped like guh heads</b> (the mouth is the door), "
                "the <b>boswachterspost</b>, the <b>plukkershut</b> by the berry garden, a lookout at the very top with the best chest, and six guh families with 12 babies.",
                "Alleen in het Vadswoud, ongeveer 1 per km (superkompas: <i>Wonen &gt; Boomhutdorp</i>). Vijf reuzenguhbomen: de <b>Moederboom</b> in het midden (een groot "
                "guhgezicht in de stam; de mond is de deur naar een holte met een nestje en een kist) en vier eromheen. Een wenteltrap loopt naar het <b>dorpsplein</b> rond de "
                "stam, ladders en zes <b>touwbruggen</b> van vadstouw naar de dekken in de andere bomen. Zes <b>boomhutten in de vorm van guhhoofden</b> (de mond is de deur), de "
                "<b>boswachterspost</b>, de <b>plukkershut</b> bij de bessentuin, een uitkijkhut in de top met de beste kist, en zes guhfamilies met 12 baby's."), wide=True) + \
        entry(img("npc_boswachterguh", "Boswachterguh"), "The Boswachterguh", "De Boswachterguh",
              p("High in the Moederboom. Tells you about guh families, then a tip about the forest. Guhdex: <i>\"Zorgt voor het Vadswoud en alle guhfamilies.\"</i>",
                "Hoog in de Moederboom. Vertelt je over guhfamilies, daarna een tip over het woud. Guhdex: <i>\"Zorgt voor het Vadswoud en alle guhfamilies.\"</i>") +
              table([("You get", "Je krijgt"), ("Kaasknabbels", "Kaasknabbels")], [
                  ["8 vadshoutstammen", "3"], ["16 vadshoutplanken", "2"], ["2 vadshoutzaailingen", "5"], ["4 vadshoutzaailingen", "9"], ["Guhnestje", "4"],
                  ["8 vadstouw", "3"], ["2 guhgezichtjes", "4"], [icon("boswachtershoed", "Boswachtershoed") + " Boswachtershoed", "10"], [icon("boswachtersjas", "Boswachtersjas") + " Boswachtersjas", "14"]])) + \
        entry(img("npc_knabbelplukker", "Knabbelplukker"), "The Knabbelplukker", "De Knabbelplukker",
              p("Picks knabbelbessen all day (and secretly eats a few). Guhdex: <i>\"Pas op: soms eet ze de oogst zelf op.\"</i> She trades in berries:",
                "Plukt de hele dag knabbelbessen (en eet er stiekem een paar op). Guhdex: <i>\"Pas op: soms eet ze de oogst zelf op.\"</i> Ze ruilt in bessen:") +
              table([("You give", "Je geeft"), ("You get", "Je krijgt")], [
                  ["4 kaasknabbels", "12 knabbelbessen"], ["12 knabbelbessen", "6 kaasknabbels"], ["9 knabbelbessen", "2 knabbelbessentaartjes"],
                  ["10 knabbelbessen", "2 gefrituurde kaasknabbels"], ["16 knabbelbessen", "Guhnestje"], ["20 knabbelbessen", icon("plukmuts", "Plukmuts") + " Plukmuts"],
                  ["28 knabbelbessen", icon("plukmandje", "Plukmandje") + " Plukmandje"]])) + \
        entry(img("guh_outfit_boswachter", "Boswachter outfit"), "Forester and picker outfits", "Boswachters- en plukpakje",
              p("Removable guh clothes: the <b>Boswachtershoed</b> (wide brim, green band with pink blossoms) and <b>Boswachtersjas</b> (moss green with pockets), and the "
                "<b>Plukmuts</b> (striped, with a knabbelbes on a stalk) and <b>Plukmandje</b> (on the back, full of berries).",
                "Uittrekbare guhkleding: de <b>Boswachtershoed</b> (brede rand, groene band met roze bloesempjes) en <b>Boswachtersjas</b> (mosgroen met zakken), en de "
                "<b>Plukmuts</b> (gestreept, met een knabbelbes op een steeltje) en het <b>Plukmandje</b> (op de rug, vol bessen).") +
              f'<p>{img("guh_outfit_pluk", "Pluk outfit", "px")}</p>'.replace('class="px"', 'style="max-width:220px"'))

    # ---------------------------------------------------------------- recipes
    HS, HSS, PL = "houtskoolsteen", "houtskoolsteen_stenen", "houtskoolsteen_stenen_plaat"
    GR, GT = "gepolijst_roosterijzer", "roosterijzer_tralies"
    rec = h3("All new recipes", "Alle nieuwe recepten") + '<div class="recipes">' + "".join([
        rcard("Aanmaakblokje (the recipe stays)", "Aanmaakblokje (het recept blijft liggen)", grid(["grillguh_recept", "charcoal", "flint", K] + [None] * 5, "aanmaakblokje", shapeless=True)),
        rcard("Houtskoolsteen stenen", "Houtskoolsteen stenen", grid([HS, HS, None, HS, HS, None, None, None, None], HSS, 4)),
        rcard("Stairs", "Trap", grid([HSS, None, None, HSS, HSS, None, HSS, HSS, HSS], "houtskoolsteen_stenen_trap", 4)),
        rcard("Slab", "Plaat", grid([HSS, HSS, HSS] + [None] * 6, PL, 6)),
        rcard("Wall", "Muur", grid([HSS] * 6 + [None] * 3, "houtskoolsteen_stenen_muur", 6)),
        rcard("Stone fence", "Stenen hek", grid([HSS, HS, HSS, HSS, HS, HSS] + [None] * 3, "houtskoolsteen_stenen_hek", 6)),
        rcard("Gebeitelde houtskoolsteen stenen", "Gebeitelde houtskoolsteen stenen", grid([PL, None, None, PL, None, None, None, None, None], "gebeitelde_houtskoolsteen_stenen")),
        rcard("Gepolijst roosterijzer", "Gepolijst roosterijzer", grid(["roosterijzer", "roosterijzer", None, "roosterijzer", "roosterijzer", None, None, None, None], GR, 4)),
        rcard("Roosterijzer pilaar", "Roosterijzer pilaar", grid([GR, None, None, GR, None, None, None, None, None], "roosterijzer_pilaar", 2)),
        rcard("Roosterijzer tralies", "Roosterijzer tralies", grid([GR] * 6 + [None] * 3, GT, 16)),
        rcard("Gloeikool", "Gloeikool", grid(["gloeikoolgruis", "gloeikoolgruis", None, "gloeikoolgruis", "gloeikoolgruis", None, None, None, None], "gloeikool")),
        rcard("Rookgat", "Rookgat", grid([GT, None, None, "gloeikool", None, None, None, None, None], "rookgat")),
        rcard("Kaasknabbelsaté", "Kaasknabbelsaté", grid(["stick", K, K, K] + [None] * 5, "kaasknabbelsate", shapeless=True)),
        smelt("kaasknabbelsate", "gegrilde_kaasknabbelsate", "Grilled saté", "Gegrilde saté", "furnace, smoker, campfire", "oven, rookoven, kampvuur"),
        smelt(HSS, "gebarsten_houtskoolsteen_stenen", "Cracked stones", "Gebarsten stenen"),
        smelt(HS, "charcoal", "Charcoal", "Houtskool"),
        rcard("Grillspiespoeder", "Grillspiespoeder", grid(["grillspies"] + [None] * 8, "grillspiespoeder", 2, shapeless=True)),
        rcard("Guhbrouwketel", "Guhbrouwketel", grid(["iron_ingot", None, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", GT, "grillspies", GT], "guhbrouwketel")),
        rcard("Knabbelbaken", "Knabbelbaken", grid(["glass", "glass", "glass", "glass", "gloeister", "glass", G, G, G], "knabbelbaken")),
        rcard("Gatenkaasstenen", "Gatenkaasstenen", grid(["gatenkaas", "gatenkaas", None, "gatenkaas", "gatenkaas", None, None, None, None], "gatenkaas_stenen", 4)),
        smelt("gatenkaas_stenen", "belegen_kaas_stenen", "Belegen kaasstenen", "Belegen kaasstenen"),
        rcard("Belegen kaastegels", "Belegen kaastegels", grid(["belegen_kaas_stenen", "belegen_kaas_stenen", None, "belegen_kaas_stenen", "belegen_kaas_stenen", None, None, None, None], "belegen_kaas_tegels", 4)),
        rcard("Kaasmos", "Kaasmos", grid(["moss_block", K] + [None] * 7, "kaasmos", shapeless=True)),
        rcard("Kaasmostapijt", "Kaasmostapijt", grid(["kaasmos", "kaasmos"] + [None] * 7, "kaasmos_tapijt", 3)),
        rcard("Stille knabbel", "Stille knabbel", grid(["kaaskorrel", K, "kaasmos"] + [None] * 6, "stille_knabbel", 2, shapeless=True)),
        smelt("kaaskorrelerts", "kaaskorrel", "Kaaskorrel", "Kaaskorrel", "furnace or blast furnace", "oven of hoogoven"),
        rcard("Borrelende kaassaus", "Borrelende kaassaus", grid(["moeraskaas", "guh_slimeball", K, "mud"] + [None] * 5, "borrelende_kaassaus", 2, shapeless=True)),
        rcard("Vadsverdrijvend drankje", "Vadsverdrijvend drankje", grid(["glass_bottle", "moeraskaas", "fermented_spider_eye"] + [None] * 6, "vadsverdrijvend_drankje", 2, shapeless=True)),
        rcard("Kaasmodder", "Kaasmodder", grid(["mud", K] + [None] * 7, "kaasmodder", shapeless=True)),
        rcard("Kaasknabbels from moeraskaas", "Kaasknabbels uit moeraskaas", grid(["moeraskaas"] + [None] * 8, K, 3, shapeless=True)),
        rcard("Vadshoutplanken", "Vadshoutplanken", grid(["vadshout_stam"] + [None] * 8, "vadshout_planken", 4, shapeless=True)),
        rcard("Guhgezichtje", "Guhgezichtje", grid(["vadshout_stam", K] + [None] * 7, "vadshout_gezicht", shapeless=True)),
        rcard("Guhnestje (any wool)", "Guhnestje (elke wol)", grid(["stick", None, "stick", "pink_wool", K, "pink_wool", "stick", "stick", "stick"], "guhnestje")),
        rcard("Vadstouw", "Vadstouw", grid(["string", None, None, "string", None, None, "string", None, None], "vadstouw", 3)),
        rcard("Knabbelbessentaartje", "Knabbelbessentaartje", grid(["knabbelbessen", "knabbelbessen", "knabbelbessen", "sugar", K] + [None] * 4, "knabbelbessentaartje", shapeless=True)),
        rcard("Yellow dye", "Gele kleurstof", grid(["knabbelbessen"] + [None] * 8, "yellow_dye", shapeless=True)),
    ]) + "</div>" + p("The vadshout stairs, slab, fence, gate, door and trapdoor use the oak recipes; the stone cutter makes all houtskoolsteen, roosterijzer, gatenkaas and "
                      "belegen kaas shapes too.",
                      "Vadshout trap, plaat, hek, poort, deur en luik gaan net als eikenhout; de steenzaag maakt ook alle vormen van houtskoolsteen, roosterijzer, gatenkaas en "
                      "belegen kaas.")

    # ---------------------------------------------------------------- advancements and quests
    STAR = " &#9733;"
    adv_rows = lambda rows: table([("Advancement", "Vooruitgang"), ("How", "Hoe")], [[f"<b>{a}</b>{STAR if star else ''}", (en, nl)] for a, en, nl, star in rows])
    adv = h3("Advancements: tab De Guhbarbecuether", "Vooruitgangen: tabblad De Guhbarbecuether") + adv_rows([
        ("De Guhbarbecuether", "Grillkool, an Aanmaakblokje or being inside.", "Grillkool, een Aanmaakblokje of binnen zijn.", False),
        ("Wie heeft hier gebarbecued?", "Find a broken barbecueput.", "Vind een kapotte barbecueput.", False),
        ("Zwart als houtskool", "Make grillkool: kaassaus over a block of coal.", "Maak grillkool: kaassaus over een blok steenkool.", False),
        ("De Grillguh helpt", "Light the Grillguh's barbecue again and get his secret recipe.", "Steek de barbecue van de Grillguh weer aan en krijg zijn geheime recept.", False),
        ("Chef Guh", "Buy the whole chef outfit from the Grillguh.", "Koop het hele kokspakje bij de Grillguh.", True),
        ("Het is hier heet, njeg!", "Step into the Guhbarbecuether.", "Stap de Guhbarbecuether in.", False),
        ("Rondje barbecue", "Visit all five biomes.", "Bezoek alle vijf de biomen.", True),
        ("Frituurvads in een emmer / Lekker warm licht / Even afkoelen", "A bucket of kaasfrituursaus, a block of gloeikool, back to the Guhmension.", "Een emmer kaasfrituursaus, een blok gloeikool, terug naar de Guhmensie.", False),
        ("Te warm om te slapen", "(hidden) Try to sleep in the Barbecuether.", "(verborgen) Probeer in de Barbecuether te slapen.", False),
        ("Een burcht vol spiesen / Van de grill gegrist / Guh-alchemist", "Find a Spiesburcht, get a grillspies, brew a Guhdrankje.", "Vind een Spiesburcht, pak een grillspies, brouw een Guhdrankje.", False),
        ("Hoofdzaak / Wie heeft dit aangebrand?", "Get a verkoolde mikakop, call the Aangebrande Mika.", "Pak een verkoolde mikakop, roep de Aangebrande Mika op.", False),
        ("Doorgebakken!", "Beat the Aangebrande Mika and take the gloeister.", "Versla de Aangebrande Mika en pak de gloeister.", True),
        ("Een baken van knabbels", "Light a Knabbelbaken on a pyramid.", "Laat een Knabbelbaken branden op een piramide.", False),
        ("Wie heeft onze knabbels? / Vads tegen buit", "Find the Mika-grillpaleis, trade with a Nether-Mika.", "Vind het Mika-grillpaleis, ruil met een Nether-Mika.", False),
        ("Vahoeg naar huis / Rookguh-redder", "Save a Rookguh / save 10.", "Red een Rookguh / red er 10.", True),
        ("Gloeiende wangetjes", "Tame an Asguh.", "Tem een Asguh.", False)]) + \
        h3("Advancements: tab Guhmensie", "Vooruitgangen: tabblad Guhmensie") + adv_rows([
        ("Vol gaten! / Knapperig! / Knabbelen zonder kraken", "Visit the Gatenkaasgrotten, mine a kaaskorrel, eat a stille knabbel.", "Bezoek de Gatenkaasgrotten, hak een kaaskorrel, eet een stille knabbel.", False),
        ("Sssst... niet knabbelen! / Het geheim van de kelder", "Find the Stille Voorraadkelder and the Voorraadmika's diary.", "Vind de Stille Voorraadkelder en het dagboek van de Voorraadmika.", False),
        ("Wie knabbelt daar?", "Wake the Vadswaker (oops).", "Maak de Vadswaker wakker (oeps).", False),
        ("Vadswaker? Vadsslaper!", "Beat the Vadswaker (not required, really not!).", "Versla de Vadswaker (niet verplicht, echt niet!).", True),
        ("Tot je knieën in de kaas / Boing, njeg! / Ahoy, knabbels!", "Step into the Kaasmoeras, bounce on borrelende kaassaus, open a raft's barrel.", "Stap in het Kaasmoeras, stuiter op borrelende kaassaus, open de ton op een vlotje.", False),
        ("Hap, slik, licht! / Kwaak-vahoeg! / Stinkend lekker", "Get a motknabbel, breed kikkerguhs, get moeraskaas.", "Krijg een motknabbel, fok kikkerguhs, krijg moeraskaas.", False),
        ("Alle kikkerkleurtjes", "Pink, mint and yellow motknabbels at once.", "Roze, mint en gele motknabbel tegelijk.", True),
        ("Modderguhtje / Een hut op pootjes / Weg met die heks!", "Tame a Kaasmoerasguh, find the hut, beat the Moerasheks-Mika.", "Tem een Kaasmoerasguh, vind de paalhut, versla de Moerasheks-Mika.", False),
        ("Tussen de reuzen / Hoog in de bomen", "Walk through the Vadswoud, find the treehouse village.", "Loop door het Vadswoud, vind het boomhutdorp.", False),
        ("Klein maar vads / Welterusten, guh / Boswachter in spe", "Tame a baby guh, your guh sleeps in a nest, wear the forester outfit.", "Tem een babyguh, je guh slaapt in een nestje, boswachtershoed + -jas.", False),
        ("Plukker van het jaar / Een boom van een guh", "64 knabbelbessen at once; four saplings become a giant guh tree.", "64 knabbelbessen tegelijk; vier zaailingen worden een reuzenguhboom.", True),
        ("Blub, wat diep!", "Find a Diepe Guhzee.", "Vind een Diepe Guhzee.", False)]) + \
        p("&#9733; = challenge (purple frame).", "&#9733; = uitdaging (paarse rand).")
    quests = h3("FTB quests (72 new, nothing locked)", "FTB-quests (72 nieuwe, niets op slot)") + table([("Part", "Onderdeel"), ("Quests", "Quests")], [
        ["De Guhbarbecuether", "Wie heeft hier gebarbecued?, NJEG mijn barbecue!, Zwart als houtskool, Het frame weer heel, Gejatte Aanmaakblokjes, VAHOEG hij brandt weer!, "
                               "Chef Guh, Grillguh in de Guhdex, Het is hier heet njeg!, " + t("the five biomes", "de vijf biomen") + ", Frituurvads in een emmer, Lekker warm licht, "
                               "Te warm om te slapen, Barbecue-bouwer"],
        ["De Spiesburcht", "Een burcht vol spiesen, Van de grill gegrist, Knekel-Mika's, De Guhbrouwketel, Guh-alchemist, Alle Guhdrankjes, Hoofdzaak, Doorgebakken!, "
                           "Een baken van knabbels, Vahoeg naar huis, Rookguh-redder, Wie heeft onze knabbels?, Vads tegen buit, Gloeiende wangetjes, "
                           "De Barbecuether in de Guhdex, Asguh-knuffel, Zo stil als een knabbel"],
        ["De Gatenkaasgrotten", "Vol gaten!, Gloeiend kaasmos, Knapperig!, Knabbelen zonder kraken, Schacht 7: dicht!, Sssst... niet knabbelen!, Het geheim van de kelder, "
                                "Wie knabbelt daar?, Vadswaker? Vadsslaper!"],
        ["Het Kaasmoeras", "Het Kaasmoeras, Boing njeg!, Ahoy knabbels!, Kwaak-guh!, Mika-motjes, Hap slik licht!, Alle kikkerkleurtjes, Modderguhtje, Een hut op pootjes, "
                           "Bleh onvahoeg, Weg met die heks!, Stinkend lekker, Heksenpagina"],
        ["Het Vadswoud", "Het Vadswoud, Hoog in de bomen, De Boswachterguh, De Knabbelplukker, Knabbelbessen plukken, Vadsig taartje, Vadshout, Een boom van een guh, "
                         "Guhfamilie, Klein maar vads, Een eigen guhnestje, Welterusten guh, Boswachter in spe, Plukker van het jaar"],
        ["De Diepe Guhzee", "De Diepe Guhzee"]]) + \
        h3("Handy commands", "Handige commando's") + \
        cmd("/execute in guhs:barbecuether run tp @s 0 70 0") + cmd("/execute in guhs:guhmension run locate structure guhs:barbecueput") + \
        cmd("/execute in guhs:barbecuether run locate structure guhs:spiesburcht") + cmd("/execute in guhs:guhmension run locate structure guhs:stille_voorraadkelder") + \
        cmd("/execute in guhs:guhmension run locate structure guhs:moerasheks_hut") + cmd("/execute in guhs:guhmension run locate structure guhs:boomhutdorp") + \
        cmd("/execute in guhs:guhmension run locate biome guhs:vadswoud") + cmd("/execute in guhs:guhmension run locate biome guhs:diepe_guhzee") + \
        cmd("/execute in guhs:guhmension run locate structure guhs:onderwater") + cmd("/summon guhs:rookguh ~ ~2 ~") + \
        p("Operators can check that the buildings generate whole: <code>/guhs bouwcheck &lt;dimension&gt; &lt;radius&gt; [overlap|compleet]</code> "
          "(<i>overlap</i> looks for buildings that would grow into each other, <i>compleet</i> generates them and compares every block with its design; the "
          "report goes to the chat and to <code>&lt;world&gt;/bouwcheck/</code>).",
          "Operators kunnen controleren of de gebouwen heel worden gegenereerd: <code>/guhs bouwcheck &lt;dimensie&gt; &lt;straal&gt; [overlap|compleet]</code> "
          "(<i>overlap</i> zoekt gebouwen die in elkaar zouden groeien, <i>compleet</i> genereert ze en vergelijkt elk blok met het ontwerp; het verslag komt in "
          "de chat en in <code>&lt;wereld&gt;/bouwcheck/</code>).")

    body = intro + parts + bbq + sb + gk + km + vw + diepzee_part() + minigames27_part() + rec + adv + quests
    return section("new27", "New in 2.7: De Guhbarbecuether en een levendiger Guhmensie", "Nieuw in 2.7: De Guhbarbecuether en een levendiger Guhmensie", body)


def ftb_chapters():
    """The FTB chapters (tools/make_ftbquests.py, without writing anything): [(file, title, subtitle, number of quests)]."""
    import collections
    import re
    import runpy
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    g = runpy.run_path(os.path.join(os.path.dirname(os.path.abspath(__file__)), "make_ftbquests.py"), run_name="count")
    if "assign" in g:           # (2.9: the real placement, with sections that move quests between chapters; +1: the chapter's intro)
        count = {c: sum(len(sec["quests"]) for sec in secs) + 1 for c, secs in g["assign"]().items()}
    else:
        keys = [q if isinstance(q, str) else (q[0] if isinstance(q, tuple) else q["key"]) for q in g["QUESTS"]]
        count = collections.Counter(g["chapter_of"](k) for k in keys)
    plain = lambda s: re.sub(r"&[0-9a-fk-or]", "", s)
    return [(f, plain(c["title"]), plain(c["sub"]), count.get(f, 0)) for f, c in g["CHAPTERS"].items()]


def recipe_card(rid, en=None, nl=None):
    """A recipe card straight from the mod's recipe json (data/guhs/recipe/<rid>.json): shaped, shapeless."""
    import json
    path = os.path.join("src", "main", "resources", "data", "guhs", "recipe", rid + ".json")
    r = json.load(open(path, encoding="utf-8"))
    TAGS = {"minecraft:wool": "pink_wool", "minecraft:planks": "oak_planks", "guhs:knus/pluiswol": "pluiswol", "guhs:knus/kaasmelk": "kaasmelk",
            "minecraft:wooden_slabs": "oak_slab", "minecraft:wooden_fences": "oak_fence", "minecraft:wooden_doors": "oak_door"}

    def name(ing):
        ing = ing[0] if isinstance(ing, list) else ing
        if isinstance(ing, str):          # 1.1.0 (26.1 recipes): "ns:item" or "#ns:tag"
            ing = {"tag": ing[1:]} if ing.startswith("#") else {"item": ing}
        if "tag" in ing:
            return TAGS.get(ing["tag"], ing["tag"].split("/")[-1].split(":")[-1])
        return ing["item"].split(":")[1]
    cells = [None] * 9
    if "pattern" in r:
        for y, line in enumerate(r["pattern"]):
            for x, ch in enumerate(line):
                if ch != " ":
                    cells[y * 3 + x] = name(r["key"][ch])
    else:
        for i, ing in enumerate(r["ingredients"]):
            cells[i] = name(ing)
    result = r["result"]["id"].split(":")[1]
    title = result.replace("_", " ").capitalize()
    return rcard(en or title, nl or title, grid(cells, result, r["result"].get("count", 1), shapeless="pattern" not in r))


def knuffel_section():
    """2.8.0 "Knuffeldal": the cosy update (a town with four buildings on its plein, the Knusfeest, seasons, a farm, gardens,
    a sterrenwacht, a balloon festival, camping, the Knuffelbad, a livelier guh day, ice cream, a choir and a claw machine)."""
    chapters = ftb_chapters()
    knus_quests = next((n for f, _, _, n in chapters if f == "guhs_knuffeldal"), 0)
    intro = p("Guhs 2.8 is the <b>cosy update</b>: no new dimension, no boss, no tension. Somewhere in the Guhmension lies the <b>Knuffeldal</b>, a small, "
              "pink, fluffy valley with exactly one little <b>town</b> in its middle. Its mayor is planning the <b>Grote Knusfeest</b>, and every building "
              "in town (and a few outside it) has its own activity, minigame or collection: a bakery, a tea house, a hairdresser, a creche, a farm with "
              "gardens, a star observatory, a balloon festival, camp sites with Opa Guh's stories and a swimming pool with three water slides. The whole "
              "Guhmension got <b>seasons</b>, and the guhs got a <b>day rhythm</b>. The Guhdex has a new tab <b>Knus</b> for all of it. Remember: a guh is "
              "never too vads. The only problem is that it's never VAHOEG enough, because the Mikas keep stealing the kaasknabbels. And even the Mikas "
              "here (the <b>Kruimel-Mikas</b>) never fight: they only nick things and run off giggling.",
              "Guhs 2.8 is de <b>knusse update</b>: geen nieuwe dimensie, geen baas, geen spanning. Ergens in de Guhmensie ligt het <b>Knuffeldal</b>, een klein, "
              "roze, pluizig dal met precies één <b>stadje</b> in het midden. De burgemeester is het <b>Grote Knusfeest</b> aan het voorbereiden, en elk gebouw "
              "in het stadje (en een paar erbuiten) heeft zijn eigen activiteit, minigame of verzameling: een bakkerij, een theehuisje, een kapper, een creche, "
              "een boerderij met tuintjes, een sterrenwacht, een ballonfestival, kampeerplekjes met de verhalen van Opa Guh en een zwembad met drie glijbanen. "
              "De hele Guhmensie kreeg <b>seizoenen</b>, en de guhs een <b>dagritme</b>. De Guhdex heeft een nieuw tabblad <b>Knus</b> voor alles. Denk erom: een "
              "guh is nooit te vads. Het enige probleem is dat hij nooit VAHOEG genoeg is, omdat de Mika's steeds de kaasknabbels pikken. En zelfs de Mika's hier "
              "(de <b>Kruimel-Mika's</b>) vechten nooit: ze pikken alleen iets en rennen giechelend weg.")
    parts = ul([("<b>Het Knuffeldal</b>: the biome, the town with its plein, Burgemeester Vadsema, <b>Cocotje</b>, Opa Guh on his bench, the <b>Pluisguh</b>.",
                 "<b>Het Knuffeldal</b>: het bioom, het stadje met zijn plein, Burgemeester Vadsema, <b>Cocotje</b>, Opa Guh op zijn bankje, de <b>Pluisguh</b>."),
                ("<b>Het Grote Knusfeest</b>: six feesttaakjes, Kruimel-Mikas, the feestbuffet, the title <i>Knuffelburgemeester</i>, a seasonal feast every season.",
                 "<b>Het Grote Knusfeest</b>: zes feesttaakjes, Kruimel-Mika's, het feestbuffet, de titel <i>Knuffelburgemeester</i>, elk seizoen een seizoensfeest."),
                ("<b>Seasons</b> (a week each) and the <b>day rhythm</b> of the guhs.", "<b>Seizoenen</b> (een week elk) en het <b>dagritme</b> van de guhs."),
                ("<b>On the plein</b>: the Knabbelbakkerij, the Knabbelthee-huisje, the kapper <i>Knip &amp; Vads</i> (hairstyles!) and the Knuffelcreche.",
                 "<b>Aan het plein</b>: de Knabbelbakkerij, het Knabbelthee-huisje, kapper <i>Knip &amp; Vads</i> (kapsels!) en de Knuffelcreche."),
                ("<b>Outside the town</b>: the Guhboerderij with farm animals and gardens, the Guh-Sterrenwacht, the Ballonfestival, camp sites and the Knuffelbad.",
                 "<b>Buiten het stadje</b>: de Guhboerderij met boerderijdieren en tuintjes, de Guh-Sterrenwacht, het Ballonfestival, kampeerplekjes en het Knuffelbad."),
                ("<b>Everywhere</b>: IJscoguh Tingeling, the koortje (xylophone and whistle) and the grijpmachine with 21 plushies.",
                 "<b>Overal</b>: IJscoguh Tingeling, het koortje (xylofoon en fluitje) en de grijpmachine met 21 knuffels."),
                ("<b>Also new</b>: the Guhdex tab <i>Knus</i>, a restyled Rookguh, Kikkerguh and Reisguh, Terugpoorten and more vetschepen in the Guheinde, "
                 "and the FTB quests sorted into nine themed chapters.",
                 "<b>Ook nieuw</b>: het Guhdex-tabblad <i>Knus</i>, een nieuwe Rookguh, Kikkerguh en Reisguh, Terugpoorten en meer vetschepen in het Guheinde, "
                 "en de FTB-quests netjes in negen hoofdstukken.")])

    # ---------------------------------------------------------------- the Knuffeldal and its town
    dal = h3("Het Knuffeldal", "Het Knuffeldal") + \
        entry(img("structure_knuffeldal_stadje", "The Knuffeldal town"), "The biome and the town", "Het bioom en het stadje",
              p("The <b>Knuffeldal</b> (<code>guhs:knuffeldal</code>) is a small but findable biome in the Guhmension (about 3% of it, never next to the Diepe Guhzee): "
                "soft pink <b>knuffelgras</b>, fluffy <b>pluizenbomen</b>, <b>guhpaddenstoelen</b> and floating pluisjes. No Mikas spawn here. In the middle of every "
                "dal stands exactly <b>one town</b>: a plein with a fountain, the town hall steps of the Burgemeester, Opa Guh's bench by the campfire, the "
                "feestbuffet, six guh houses with residents (make friends with all of them) and a grijpmachine under the arcade. On the four sides of the plein stand "
                "the <b>Knabbelbakkerij</b>, the <b>Knabbelthee-huisje</b>, the kapper <b>Knip &amp; Vads</b> and the <b>Knuffelcreche</b>. The town is protected: "
                "you can't break or build in it (in survival). Find it with the superkompas, category <b>Knus</b>.",
                "Het <b>Knuffeldal</b> (<code>guhs:knuffeldal</code>) is een klein maar vindbaar bioom in de Guhmensie (zo'n 3%, nooit naast de Diepe Guhzee): "
                "zacht roze <b>knuffelgras</b>, pluizige <b>pluizenbomen</b>, <b>guhpaddenstoelen</b> en zwevende pluisjes. Hier spawnen geen Mika's. In het midden "
                "van elk dal staat precies <b>één stadje</b>: een plein met een fontein, het bordes van de Burgemeester, het bankje van Opa Guh bij het kampvuur, "
                "het feestbuffet, zes guhhuisjes met bewoners (word vriendjes met ze allemaal) en een grijpmachine onder de arcade. Aan de vier kanten van het plein "
                "staan de <b>Knabbelbakkerij</b>, het <b>Knabbelthee-huisje</b>, kapper <b>Knip &amp; Vads</b> en de <b>Knuffelcreche</b>. Het stadje is beschermd: "
                "je kunt er (in survival) niet breken of bouwen. Vind het met het superkompas, categorie <b>Knus</b>."), wide=True) + \
        entry(img("knuffeldal_npcs", "The new guh characters"), "Eleven new guh characters", "Elf nieuwe guhpersonages",
              p("From left to right: <b>Burgemeester Vadsema</b>, <b>Cocotje</b>, <b>Bakker Korstje</b>, <b>Juf Knuffel</b>, <b>Mevrouw Theelepel</b>, "
                "<b>Kapper Krulletje</b>, <b>Boerin Hooibaal</b>, <b>Professor Sterretje</b>, <b>Kapitein Wolkje</b>, <b>Opa Guh</b> and <b>Badmeester Bubbel</b>. "
                "Each has his own model (a top hat, an apron, a telescope, goggles, a whistle...), a Guhdex page and a lot to say, full of njeg and VAHOEG.",
                "Van links naar rechts: <b>Burgemeester Vadsema</b>, <b>Cocotje</b>, <b>Bakker Korstje</b>, <b>Juf Knuffel</b>, <b>Mevrouw Theelepel</b>, "
                "<b>Kapper Krulletje</b>, <b>Boerin Hooibaal</b>, <b>Professor Sterretje</b>, <b>Kapitein Wolkje</b>, <b>Opa Guh</b> en <b>Badmeester Bubbel</b>. "
                "Elk heeft een eigen model (een hoge hoed, een schortje, een telescoop, een vliegbril, een fluitje...), een Guhdex-pagina en heel veel te vertellen, "
                "vol njeg en VAHOEG."), wide=True) + \
        entry(img("structure_knuffeldal_plein", "The plein"), "The plein", "Het plein",
              p("The heart of the town: the fountain, the Burgemeester on his steps (top left), Opa Guh on his bench by the campfire circle (bottom right), the "
                "<b>feestbuffettafels</b>, the seasonal <b>seizoensbloembakken</b> and <b>seizoensslingers</b> (they change with the season) and the arcade with the grijpmachine.",
                "Het hart van het stadje: de fontein, de Burgemeester op zijn bordes (linksboven), Opa Guh op zijn bankje bij de kampvuurkring (rechtsonder), de "
                "<b>feestbuffettafels</b>, de <b>seizoensbloembakken</b> en <b>seizoensslingers</b> (die veranderen mee met het seizoen) en de arcade met de grijpmachine.")) + \
        entry(img("guh_variant_pluisguh", "Pluisguh"), "The Pluisguh", "De Pluisguh",
              p("A new guh variant, only in the Knuffeldal (about a third of the wild guhs born there): extra fluffy and pink, with a fluffy tuft and fluffy cheeks. "
                "Sweet and passive like every guh; tame it with kaasknabbels for its Guhdex star (advancement <i>Superpluizig</i>). There is a Pluisguh plushie too.",
                "Een nieuwe guhvariant, alleen in het Knuffeldal (zo'n derde van de wilde guhs die daar geboren worden): extra pluizig en roze, met een pluizig kuifje en "
                "pluizige wangetjes. Lief en vredig zoals elke guh; tem hem met kaasknabbels voor zijn Guhdex-ster (vooruitgang <i>Superpluizig</i>). Er is ook een Pluisguh-knuffel.")) + \
        entry(img("structure_cocotje_straat", "Cocotje's street"), "Cocotje", "Cocotje",
              p("In a little house with a guh face in one of the town's streets lives <b>Cocotje</b>, with a bow on her head. She has lost something and asks you "
                "<i>\"WEET JIJ WAAR ZE ZIJN??????\"</i>. You get three answers to click in the chat. Only one is right... look closely at Cocotje. The right answer "
                "gives you a kaasknabbel, a Knus entry and the advancement <i>Ze hangen aan haar veh</i>. Njeg.",
                "In een huisje met een guhgezicht in een van de straten van het stadje woont <b>Cocotje</b>, met een strikje op haar hoofd. Ze is iets kwijt en vraagt je "
                "<i>\"WEET JIJ WAAR ZE ZIJN??????\"</i>. Je krijgt drie antwoorden om in de chat aan te klikken. Maar één is goed... kijk eens goed naar Cocotje. Het goede "
                "antwoord geeft je een kaasknabbel, een Knus-vermelding en de vooruitgang <i>Ze hangen aan haar veh</i>. Njeg.")) + \
        entry(img("knuffelsteen_gezichtjes", "Knuffelsteen faces"), "Building blocks", "Bouwblokken",
              p("Build your own knuffel town: <b>knuffelsteen</b> (smooth sandstone + pink dye) with stairs, slabs and walls, <b>knuffelsteen met gezicht</b> "
                "(knuffelsteen + a kaasknabbel; four moods), <b>pluisdak</b> (pink wool roof) with stairs and slabs, and the <b>knuffelklinkers</b> paving.",
                "Bouw je eigen knuffelstadje: <b>knuffelsteen</b> (gladde zandsteen + roze kleurstof) met trappen, platen en muren, <b>knuffelsteen met gezicht</b> "
                "(knuffelsteen + een kaasknabbel; vier stemmingen), <b>pluisdak</b> (roze wollen dak) met trappen en platen, en de <b>knuffelklinkers</b> als bestrating.") +
              '<p>' + ' '.join(icon(n, n) for n in ("knuffelgras", "pluizenboom_stam", "pluizenboom_bladeren", "guhpaddenstoel", "knuffelsteen", "pluisdak", "knuffelklinkers")) + '</p>')

    feest = h3("Het Grote Knusfeest", "Het Grote Knusfeest") + \
        entry(img("npc_burgemeesterguh", "Burgemeester Vadsema"), "Burgemeester Vadsema and the six feesttaakjes", "Burgemeester Vadsema en de zes feesttaakjes",
              p("Talk to <b>Burgemeester Vadsema</b> on his steps: <i>\"Ahum!\"</i> He is planning the <b>Grote Knusfeest</b> and gives you a <b>Knusfeestlijstje</b> "
                "with six feesttaakjes. Each building makes one thing for the feast; bring them all to the Burgemeester:",
                "Praat met <b>Burgemeester Vadsema</b> op zijn bordes: <i>\"Ahum!\"</i> Hij bereidt het <b>Grote Knusfeest</b> voor en geeft je een <b>Knusfeestlijstje</b> "
                "met zes feesttaakjes. Elk gebouw maakt één ding voor het feest; breng ze allemaal naar de Burgemeester:") +
              table([("Feesttaakje", "Feesttaakje"), ("Where", "Waar"), ("How", "Hoe")], [
                  ["Feesttaart " + icon("feesttaart", "Feesttaart"), "Knabbelbakkerij", ("Bake Bakker Korstje's special order", "Bak de speciale bestelling van Bakker Korstje")],
                  ["Theeservies " + icon("feest_theeservies", "Theeservies"), "Knabbelthee-huisje", ("Hold a gezellig theekransje: Mevrouw Theelepel lends you her set", "Houd een gezellig theekransje: Mevrouw Theelepel leent je haar servies")],
                  ["Feestkapsels " + icon("feestkapselset", "Feestkapselset"), "Knip &amp; Vads", ("A kappersshow round in the feest theme", "Een kappersshow-ronde in feeststijl")],
                  ["Feestslingers " + icon("feestslingers", "Feestslingers"), "Knuffelcreche", ("A calm care round: the babies knutsel slingers", "Een rustige verzorgronde: de babyguhtjes knutselen slingers")],
                  ["Feestbloemen " + icon("feestboeket", "Feestboeket"), ("Your garden", "Je tuintje"), ("A feestboeket from five guhbloemetjes", "Een feestboeket van vijf guhbloemetjes")],
                  ["Sterrenlantaarns " + icon("sterrenlantaarn", "Sterrenlantaarn"), "Guh-Sterrenwacht", ("Connect a constellation: Professor Sterretje gives you three", "Verbind een sterrenbeeld: Professor Sterretje geeft je er drie")]]), wide=True) + \
        entry(img("kruimel_mika", "Kruimel-Mika"), "Kruimel-Mikas", "Kruimel-Mika's",
              p("On your way to the Burgemeester with the taart, the theeservies or the slingers, a <b>Kruimel-Mika</b> may snatch it: <i>*giechel*</i>. A little Mika "
                "full of cookie crumbs that never fights (don't hit it: njeg!). Follow the <b>crumb trail</b>, hold a treat (kaasknabbels or anything from the "
                "bakery) and give it to him: he forgets his loot and runs off giggling. Nothing is ever lost: a stolen thing always comes back by itself after a few minutes.",
                "Onderweg naar de Burgemeester met de taart, het theeservies of de slingers kan een <b>Kruimel-Mika</b> het weggraaien: <i>*giechel*</i>. Een kleine Mika "
                "vol koekkruimels die nooit vecht (niet meppen: njeg!). Volg het <b>kruimelspoor</b>, houd een lekkernij vast (kaasknabbels of iets uit de bakkerij) "
                "en geef hem die: hij vergeet zijn buit en rent giechelend weg. Er raakt nooit iets kwijt: wat gepikt is komt na een paar minuten vanzelf terug.")) + \
        entry(img("feestbuffettafel", "Feestbuffettafel"), "The feestbuffet and the rewards", "Het feestbuffet en de beloningen",
              p("With all six brought, the feast starts at the <b>feestbuffet</b>: the tables fill with what you baked, grew and poured, and all your tamed guhs come to eat. "
                "Your reward: the <b>Knus-oorkonde</b> (a trophy block), the title <b>Knuffelburgemeester</b> (the whole server hears it) and the "
                "<b>burgemeesterssjerp</b> for your guh. After that the Burgemeester asks for a few feesttaakjes again every season: the <b>seasonal Knusfeest</b>, "
                "a guh event for the lente-, zomer-, herfst- and winterfeest.",
                "Zijn alle zes gebracht, dan begint het feest aan het <b>feestbuffet</b>: de tafels lopen vol met wat jij hebt gebakken, gekweekt en ingeschonken, en al je "
                "tamme guhs komen smullen. Je beloning: de <b>Knus-oorkonde</b> (een trofeeblok), de titel <b>Knuffelburgemeester</b> (de hele server hoort het) en de "
                "<b>burgemeesterssjerp</b> voor je guh. Daarna vraagt de Burgemeester elk seizoen weer een paar feesttaakjes: het <b>seizoensfeest</b>, een guh-evenement "
                "voor het lente-, zomer-, herfst- en winterfeest.") +
              '<p>' + ' '.join(icon(n, n) for n in ("knusfeestlijstje", "knus_oorkonde", "burgemeesterssjerp")) + '</p>')

    tijd = h3("Seasons and the day rhythm", "Seizoenen en het dagritme") + \
        entry(img("seizoensbloembakken", "Seizoensbloembakken"), "Seasons", "Seizoenen",
              p("The Guhmension now has four <b>seasons</b> of 7 Minecraft days each: lente, zomer, herfst and winter (the chat tells you when one starts; "
                "<code>/guhs seizoen</code> shows which one it is). The seizoensbloembakken and slingers on the plein change along, and every season has its own thing to do "
                "(right-click a seizoensbloembak to see what): <b>lente</b>: three flowers on a bloembak make a <b>bloesemkransje</b>; <b>zomer</b>: three wheat make a straw "
                "<b>zonnehoedje</b>; <b>herfst</b>: jump into the big soft <b>bladerhoopjes</b> (four leaves on a bloembak: bladerhoopjes for at home); <b>winter</b>: three "
                "wool make a knus <b>sjaaltje</b>, and a <b>sneeuwguhkopje</b> on two snow blocks makes a <b>sneeuwpopguh</b>. Put them on your guh for the "
                "<b>seizoensplakboek</b> (two entries per season). Operators: <code>/guhs seizoen &lt;lente|zomer|herfst|winter&gt;</code>.",
                "De Guhmensie heeft nu vier <b>seizoenen</b> van elk 7 Minecraft-dagen: lente, zomer, herfst en winter (de chat zegt het als er een begint; "
                "<code>/guhs seizoen</code> laat zien welk het is). De seizoensbloembakken en -slingers op het plein veranderen mee, en elk seizoen heeft iets eigens om te doen "
                "(rechtsklik een seizoensbloembak om te zien wat): <b>lente</b>: drie bloemetjes op een bloembak worden een <b>bloesemkransje</b>; <b>zomer</b>: drie tarwe "
                "worden een strooien <b>zonnehoedje</b>; <b>herfst</b>: spring in de grote zachte <b>bladerhoopjes</b> (vier bladeren op een bloembak: bladerhoopjes voor thuis); "
                "<b>winter</b>: drie wol worden een knus <b>sjaaltje</b>, en een <b>sneeuwguhkopje</b> op twee sneeuwblokken wordt een <b>sneeuwpopguh</b>. Zet ze op je guh "
                "voor het <b>seizoensplakboek</b> (twee dingen per seizoen). Operators: <code>/guhs seizoen &lt;lente|zomer|herfst|winter&gt;</code>.") +
              '<p>' + ' '.join(icon(n, n) for n in ("bloesemkransje", "zonnehoedje", "bladerhoopje", "knus_sjaaltje", "sneeuwguhkopje", "sneeuwpopguh")) + '</p>', wide=True) + \
        entry(img("guh_outfit_knuffeldal", "A knus guh"), "The day rhythm", "Het dagritme",
              p("The guhs of the Knuffeldal, of the guh villages and your own free-roaming tamed guhs now live by the clock: in the <b>morning</b> they yawn and stretch "
                "(the new emote <i>Gapen</i>), during the <b>day</b> they wave at you, in the <b>afternoon</b> they take a nap in a little nest, in the <b>evening</b> "
                "they sit by a campfire with marshmallow knabbels, and at <b>night</b>: zzz. Two more new emotes: <i>Zingen</i> and <i>Knuffelen</i>.",
                "De guhs van het Knuffeldal, van de guhdorpen en je eigen vrij rondlopende tamme guhs leven nu op de klok: 's <b>ochtends</b> gapen en rekken ze zich uit "
                "(de nieuwe emote <i>Gapen</i>), <b>overdag</b> zwaaien ze naar je, 's <b>middags</b> doen ze een dutje in een nestje, 's <b>avonds</b> zitten ze bij een "
                "kampvuur met marshmallowknabbels, en 's <b>nachts</b>: zzz. Nog twee nieuwe emotes: <i>Zingen</i> en <i>Knuffelen</i>."))

    # ---------------------------------------------------------------- the four buildings on the plein
    plein = h3("On the plein", "Aan het plein") + \
        entry(img("structure_bakkerij", "De Knabbelbakkerij"), "De Knabbelbakkerij", "De Knabbelbakkerij",
              p("A giant kaasknabbel bread with a guh face in its crust, and a chimney that puffs knabbel clouds. <b>Bakker Korstje</b> (he started to look like his bread "
                "himself) runs a minigame: customer guhs come in with an <b>order bubble</b>; pick the dough, the shape and the topping, time the <b>oven</b> "
                "right (not too pale, not burnt!) and serve before they get impatient. Happy customers in a row make a <b>combo</b>. You earn <b>bakmunten</b> "
                "(+1 extra per reward) for his shop, and your score goes into the highscores. With a <b>knabbeloven</b> of your own you bake the <b>12 recipes</b> of the "
                "<b>receptenboek</b> (knabbelbroodje, kaaskrakeling, vadsvlaai, guhcroissant, knabbelkoekje, kaasbolletje, pluismuffin, theetaartje, knabbeltompouce, "
                "vadsdonut, guhwafel, sterrenkoekje) from knabbelgraan, kaasmelk and knabbeleitjes (wheat, milk and eggs work too, but fresh farm things give one extra).",
                "Een reuzenkaasknabbelbrood met een guhgezicht in de korst, en een schoorsteen die knabbelwolkjes puft. <b>Bakker Korstje</b> (hij is zelf op zijn brood "
                "gaan lijken) heeft een minigame: klantguhs komen binnen met een <b>bestelwolkje</b>; kies het deeg, de vorm en het toppinkje, haal het precies op tijd uit de "
                "<b>oven</b> (niet te bleek, niet aangebrand!) en serveer voor ze ongeduldig worden. Blije klanten op rij geven een <b>combo</b>. Je verdient <b>bakmunten</b> "
                "(+1 extra per beloning) voor zijn winkeltje, en je score komt in de highscores. Met een eigen <b>knabbeloven</b> bak je de <b>12 recepten</b> van het "
                "<b>receptenboek</b> (knabbelbroodje, kaaskrakeling, vadsvlaai, guhcroissant, knabbelkoekje, kaasbolletje, pluismuffin, theetaartje, knabbeltompouce, "
                "vadsdonut, guhwafel, sterrenkoekje) van knabbelgraan, kaasmelk en knabbeleitjes (tarwe, melk en eieren gaan ook, maar verse boerderijspullen geven er een extra).") +
              '<p>' + ' '.join(icon(n, n) for n in ("bakmunt", "knabbeloven", "knabbelbroodje", "guhcroissant", "vadsdonut", "guhwafel", "sterrenkoekje", "pluismuffin", "feesttaart")) + '</p>', wide=True) + \
        entry(img("structure_theehuis", "Het Knabbelthee-huisje"), "Het Knabbelthee-huisje", "Het Knabbelthee-huisje",
              p("A teapot with a guh-face lid. <b>Mevrouw Theelepel</b> stirs with her giant teaspoon and knows every tea. Bring your tamed guhs for a <b>theekransje</b> "
                "at the <b>theetafel</b>: they chat with emotes, you pour tea and serve cakes (home-baked from the bakery counts extra). A happy table gives everyone "
                "the effect <b>Gezellig</b>. Four teas for the <b>theesoorten</b> page: <b>knabbelthee</b>, <b>kaasmelkthee</b>, <b>theekruidthee</b> and "
                "<b>guhbloementhee</b>. Not a minigame, so no coins: just gezellig.",
                "Een theepot met een guhgezicht op het deksel. <b>Mevrouw Theelepel</b> roert met haar reuzentheelepel en kent elke theesoort. Neem je tamme guhs mee voor een "
                "<b>theekransje</b> aan de <b>theetafel</b>: ze kletsen met emotes, jij schenkt thee in en serveert gebak (zelfgebakken uit de bakkerij telt extra). Een blije "
                "tafel geeft iedereen het effect <b>Gezellig</b>. Vier theesoorten voor de pagina <b>theesoorten</b>: <b>knabbelthee</b>, <b>kaasmelkthee</b>, "
                "<b>theekruidthee</b> en <b>guhbloementhee</b>. Geen minigame, dus geen munten: gewoon gezellig.") +
              '<p>' + ' '.join(icon(n, n) for n in ("theetafel", "theepotje", "knabbelthee", "kaasmelkthee", "theekruidthee", "guhbloementhee", "theemutsje")) + '</p>', wide=True) + \
        entry(img("structure_kapper", "Knip &amp; Vads"), "Knip &amp; Vads, the hairdresser", "Knip &amp; Vads, de kapper",
              p("<b>Kapper Krulletje</b> knips, curls and blow-dries every guh VAHOEG pretty: <i>\"A guh is never too vads for a new hairstyle, only not vahoeg enough.\"</i> "
                "His minigame is the <b>kappersshow</b>: give the customer the hairstyle on his picture, in time (with the loaned <b>kappersschaar</b>), for "
                "<b>krulmunten</b> and a highscore. Guhs now have a sixth clothes slot: <b>hair</b>. There are <b>8 hairstyles</b> (krullen, kuifje, knotjes, strikjes, "
                "pluisbol, vlechtjes, hanenkam, matje) and <b>8 hair dyes</b> (roze, mint, citroen, lavendel, hemelsblauw, perzik, zilver and regenboog). "
                "Use them on your own tamed guh: hair is permanent (only the kapper changes it) and fits under every hat. Collect them all for the <b>kapselcollectie</b>.",
                "<b>Kapper Krulletje</b> knipt, krult en föhnt elke guh VAHOEG mooi: <i>\"Een guh is nooit te vads voor een nieuw kapsel, alleen niet vahoeg genoeg.\"</i> "
                "Zijn minigame is de <b>kappersshow</b>: geef de klant op tijd het kapsel van zijn plaatje (met de geleende <b>kappersschaar</b>), voor <b>krulmunten</b> en "
                "een highscore. Guhs hebben nu een zesde kledingvakje: <b>haar</b>. Er zijn <b>8 kapsels</b> (krullen, kuifje, knotjes, strikjes, pluisbol, vlechtjes, "
                "hanenkam, matje) en <b>8 haarverfjes</b> (roze, mint, citroen, lavendel, hemelsblauw, perzik, zilver en regenboog). Gebruik ze op je eigen tamme guh: haar "
                "is blijvend (alleen de kapper verandert het) en past onder elk hoedje. Verzamel ze allemaal voor de <b>kapselcollectie</b>."), wide=True) + \
        entry(img("kapsels", "The eight hairstyles"), "The eight hairstyles", "De acht kapsels",
              p("Krullen, kuifje, knotjes, strikjes, pluisbol, vlechtjes, hanenkam and matje, in the natural colour.",
                "Krullen, kuifje, knotjes, strikjes, pluisbol, vlechtjes, hanenkam en matje, in de natuurlijke kleur."), wide=True) + \
        entry(img("haarverf", "Hair dyes"), "Hair dyes", "Haarverf",
              p("The krullen in seven of the dyes (regenboog changes colour). A dye: a glass bottle, a dye and a kaasknabbel.",
                "De krullen in zeven van de verfjes (regenboog verandert van kleur). Een verfje: een glazen flesje, een kleurstof en een kaasknabbel.") +
              '<p>' + ' '.join(icon(n, n) for n in ("krulmunt", "kappersschaar", "haarverf_roze", "haarverf_mint", "haarverf_regenboog", "kappersstoel", "haarwasbak")) + '</p>', wide=True) + \
        entry(img("structure_creche", "De Knuffelcreche"), "De Knuffelcreche", "De Knuffelcreche",
              p("A giant baby guh with a pacifier. <b>Juf Knuffel</b> looks after all the babyguhtjes of the dal. The calm <b>care round</b>: give each baby a "
                "<b>babyflesje</b>, a <b>schone luier</b>, tuck it into its <b>wiegje</b> and sing a <b>slaapliedje</b> (a little rhythm game; four songs for the "
                "slaapliedjes page). The minigame: babies crawl off everywhere, put them back in their cribs in time for <b>speenmunten</b> and a highscore.",
                "Een reuzenbabyguh met een speen. <b>Juf Knuffel</b> past op alle babyguhtjes van het dal. De rustige <b>verzorgronde</b>: geef elk babytje een "
                "<b>babyflesje</b>, een <b>schone luier</b>, stop het in zijn <b>wiegje</b> en zing een <b>slaapliedje</b> (een klein ritmespelletje; vier liedjes voor de "
                "pagina slaapliedjes). De minigame: babyguhtjes kruipen overal heen, breng ze op tijd terug naar hun wiegje voor <b>speenmunten</b> en een highscore.") +
              '<p>' + ' '.join(icon(n, n) for n in ("speenmunt", "babyflesje", "schone_luier", "knuffeldekentje", "guh_wiegje", "speelkleed", "feestslingers")) + '</p>', wide=True) + \
        entry(img("creche_babyguh", "A babyguhtje"), "The babyguhtjes", "De babyguhtjes",
              p("Tiny, purple and never still. Njeg!", "Piepklein, paars en nooit stil. Njeg!"))

    # ---------------------------------------------------------------- outside the town
    buiten = h3("Outside the town", "Buiten het stadje") + \
        entry(img("structure_guhboerderij", "De Guhboerderij"), "De Guhboerderij", "De Guhboerderij",
              p("A farm in the guhweides and on the kaasvlakte (superkompas: Knus). <b>Boerin Hooibaal</b> (with a straw in her mouth) has a <b>klusje</b> for you every day "
                "and sells brushes, feed, seeds and watering cans. Her animals are new: the <b>guhschaapje</b>, the <b>knabbelkippetje</b> and the <b>guhkoe</b> "
                "(they also live in the wild now and then). Care for an animal in two of three ways on a day (<b>pet</b> it with an empty hand, <b>brush</b> it with the "
                "<b>guhborstel</b>, <b>feed</b> it <b>knabbelvoer</b> or from a filled <b>guh_voerbak</b>) and it is happy: the schaapje drops <b>pluiswol</b> (and "
                "looks shorn until the next day), the kippetje lays a <b>knabbelei</b> in a <b>kippennestje</b>, and the guhkoe gives <b>kaasmelk</b> in an empty bottle. "
                "Those go to the bakery, the tea house, the creche and the camp sites.",
                "Een boerderij in de guhweides en op de kaasvlakte (superkompas: Knus). <b>Boerin Hooibaal</b> (met een strootje in haar mond) heeft elke dag een "
                "<b>klusje</b> voor je en verkoopt borstels, voer, zaadjes en gieters. Haar dieren zijn nieuw: het <b>guhschaapje</b>, het <b>knabbelkippetje</b> en de "
                "<b>guhkoe</b> (ze lopen soms ook in het wild rond). Verzorg een dier op een dag op twee van de drie manieren (<b>aaien</b> met een lege hand, <b>borstelen</b> "
                "met de <b>guhborstel</b>, <b>voeren</b> met <b>knabbelvoer</b> of uit een gevulde <b>guhvoerbak</b>) en het is blij: het schaapje laat <b>pluiswol</b> "
                "los (en is geschoren tot de volgende dag), het kippetje legt een <b>knabbelei</b> in een <b>kippennestje</b>, en de guhkoe geeft <b>kaasmelk</b> in een leeg "
                "flesje. Die gaan naar de bakkerij, het theehuisje, de creche en de kampeerplekjes."), wide=True) + \
        entry(img("boerderij_dieren", "Farm animals"), "Guhkoe, guhschaapje, knabbelkippetje", "Guhkoe, guhschaapje, knabbelkippetje",
              p("Always sweet and passive, with a Guhdex page each (stand close to them). <i>Moeh! Bleh! Tok tok, vahoeg!</i>",
                "Altijd lief en vredig, met elk een Guhdex-pagina (ga er dichtbij staan). <i>Moeh! Bleh! Tok tok, vahoeg!</i>") +
              '<p>' + ' '.join(icon(n, n) for n in ("pluiswol", "knabbelei", "kaasmelk", "guhborstel", "knabbelvoer", "guh_voerbak", "kippennestje", "pluiswolblok")) + '</p>') + \
        entry(img("guh_moestuinbak", "Guhmoestuinbak"), "Guhtuintjes", "Guhtuintjes",
              p("Plant <b>knabbelzaadjes</b>, <b>theekruidzaadjes</b> or <b>guhbloemzaadjes</b> in a <b>guh_bloempot</b> or a <b>guh_moestuinbak</b> and water them with the "
                "<b>guhgieter</b>. They grow much faster when a <b>tamed guh</b> waters them (it does that by itself, with little drops) or when guhs <b>sing</b> nearby "
                "(see the koortje). Harvest <b>knabbelgraan</b>, <b>theekruid</b> and <b>guhbloemetjes</b> for the bakery, the tea house and the feestbuffet, and fill "
                "the <b>tuinboek</b>. Five guhbloemetjes make a <b>feestboeket</b>.",
                "Plant <b>knabbelzaadjes</b>, <b>theekruidzaadjes</b> of <b>guhbloemzaadjes</b> in een <b>guhbloempot</b> of een <b>guhmoestuinbak</b> en geef ze water met de "
                "<b>guhgieter</b>. Ze groeien veel sneller als een <b>tamme guh</b> ze water geeft (dat doet hij vanzelf, met druppeltjes) of als er guhs in de buurt "
                "<b>zingen</b> (zie het koortje). Oogst <b>knabbelgraan</b>, <b>theekruid</b> en <b>guhbloemetjes</b> voor de bakkerij, het theehuisje en het feestbuffet, en "
                "vul het <b>tuinboek</b>. Vijf guhbloemetjes worden een <b>feestboeket</b>.") +
              '<p>' + ' '.join(icon(n, n) for n in ("guh_bloempot", "guh_gieter", "knabbelzaadjes", "knabbelgraan", "theekruid", "guhbloemetje", "feestboeket")) + '</p>') + \
        entry(img("structure_guh_sterrenwacht", "De Guh-Sterrenwacht"), "De Guh-Sterrenwacht", "De Guh-Sterrenwacht",
              p("High on the Guhpieken and the Vadskliffen stands the observatory of <b>Professor Sterretje</b>. At night, right-click the <b>guh-telescoop</b> and "
                "connect the stars into guh constellations: <b>12</b> of them (De Grote Knabbel, De Kleine Vads, Het Guhoor, De Vluchtende Mika, De Kaasschaaf, "
                "De Slapende Guh, Het Frituurpannetje, De Vahoege Buik, De Guhstaart, Het Knuffelhart, De Luchtballon, Het Kampvuur), plus <b>3 rare ones</b> that only "
                "shine during a sterrenregen (De Gouden Guh, De Vallende Knabbel, De Guhkroon). Up to three per night. Each gives <b>wenssterren</b> to make a wish "
                "or to spend in the professor's shop (sterrenlantaarns, his muts and cape). Fill the <b>sterrenatlas</b>.",
                "Hoog op de Guhpieken en de Vadskliffen staat de sterrenwacht van <b>Professor Sterretje</b>. Rechtsklik 's nachts de <b>guh-telescoop</b> en verbind de "
                "sterren tot guh-sterrenbeelden: <b>12</b> stuks (De Grote Knabbel, De Kleine Vads, Het Guhoor, De Vluchtende Mika, De Kaasschaaf, De Slapende Guh, "
                "Het Frituurpannetje, De Vahoege Buik, De Guhstaart, Het Knuffelhart, De Luchtballon, Het Kampvuur), plus <b>3 zeldzame</b> die alleen tijdens een "
                "sterrenregen schijnen (De Gouden Guh, De Vallende Knabbel, De Guhkroon). Hooguit drie per nacht. Elk geeft <b>wenssterren</b> om een wens te doen of uit "
                "te geven in de winkel van de professor (sterrenlantaarns, zijn muts en cape). Vul de <b>sterrenatlas</b>.") +
              '<p>' + ' '.join(icon(n, n) for n in ("guh_telescoop", "wensster", "sterrenlantaarn", "sterrenkijkersmuts", "sterrencape")) + '</p>', wide=True) + \
        entry(img("structure_ballonfestival", "Het Ballonfestival"), "Het Ballonfestival", "Het Ballonfestival",
              p("On the Guhvelden and the Roze pluisjes: balloons in four colours, a grandstand, a tent with a guh-faced portal and a lookout tower with a giant guh head. "
                "<b>Kapitein Wolkje</b> takes you on a <b>guided round flight</b> in a <b>guh-luchtballon</b> (no steering needed: just enjoy the view while he tells "
                "you what you see). Every viewpoint gives a <b>stamp</b> on your <b>ballonstempelkaart</b> (8 stamps, four routes), and every flight pays "
                "<b>ballonmunten</b> for his shop (the ballonpet, the ballonbril, a mini-luchtballon to put down and the ballonsteiger).",
                "Op de Guhvelden en de Roze pluisjes: ballonnen in vier kleuren, een tribune, een tent met een guhgezicht-portaal en een uitkijktoren met een reuzenguhkop. "
                "<b>Kapitein Wolkje</b> neemt je mee op een <b>rondvlucht met gids</b> in een <b>guh-luchtballon</b> (sturen hoeft niet: geniet van het uitzicht terwijl hij "
                "vertelt wat je ziet). Elk uitzichtpunt geeft een <b>stempel</b> op je <b>ballonstempelkaart</b> (8 stempels, vier routes), en elke vlucht betaalt "
                "<b>ballonmunten</b> voor zijn winkeltje (de ballonpet, de ballonbril, een mini-luchtballonnetje om neer te zetten en de ballonsteiger).") +
              '<p>' + ' '.join(icon(n, n) for n in ("ballonmunt", "ballonpet", "ballonbril", "mini_luchtballon", "ballonsteiger")) + '</p>', wide=True) + \
        entry(img("luchtballonnen", "Guh-luchtballonnen"), "Guh-luchtballonnen", "Guh-luchtballonnen",
              p("Roze, mint, lavendel and citroen, each with guh ears on top.", "Roze, mint, lavendel en citroen, elk met guhoortjes erop."), wide=True) + \
        entry(img("structure_kampeerplekje", "A kampeerplekje"), "Kampeerplekjes and Opa Guh", "Kampeerplekjes en Opa Guh",
              p("Little camp sites with tents and a campfire, in the Knuffeldal and the guhweides. At night <b>Opa Guh</b> tells a story by the fire (he is also on his "
                "bench in the town): <b>12 stories</b>, one per night, about Mikas, kaasknabbels and why guhs have such big ears. Listen to most of it for the "
                "<b>verhalenbundel</b> and a few kaasknabbels. Guhs near a burning campfire at night wear <b>pyjamas</b>, and they come and listen too. Roast a "
                "<b>marshmallowknabbel</b> at a campfire in the evening. Sleep a whole night in a <b>guh-slaapzak</b> (no bed needed, your spawn point stays where it was) "
                "and you wake up <b>Uitgerust</b> (a bit faster, a bit of healing).",
                "Kleine kampeerplekjes met tentjes en een kampvuur, in het Knuffeldal en de guhweides. 's Nachts vertelt <b>Opa Guh</b> een verhaal bij het vuur (hij zit ook op "
                "zijn bankje in het stadje): <b>12 verhalen</b>, één per nacht, over Mika's, kaasknabbels en waarom guhs zulke grote oren hebben. Luister het meeste ervan voor "
                "de <b>verhalenbundel</b> en een paar kaasknabbels. Guhs bij een brandend kampvuur dragen 's nachts een <b>pyjama</b>, en ze komen ook luisteren. Rooster "
                "'s avonds een <b>marshmallowknabbel</b> bij een kampvuur. Slaap een hele nacht in een <b>guh-slaapzak</b> (geen bed nodig, je spawnpunt blijft waar het was) "
                "en je wordt <b>Uitgerust</b> wakker (iets sneller, een beetje genezing).") +
              '<p>' + ' '.join(icon(n, n) for n in ("guh_slaapzak", "marshmallow_knabbel", "pyjama_pakje", "slaapmutsje")) + '</p>') + \
        entry(img("guh_outfit_kamperen", "Pyjama"), "Pyjamas", "Pyjama's",
              p("The pyjama pakje and the slaapmutsje. Welterusten, guh!", "Het pyjamapakje en het slaapmutsje. Welterusten, guh!")) + \
        entry(img("structure_knuffelbad", "Het Knuffelbad"), "Het Knuffelbad", "Het Knuffelbad",
              p("A big swimming pool on the guhzee coast, with <b>Badmeester Bubbel</b> (<i>Tuuut!</i> no running!). <b>Wash your guh</b> in a <b>guh-wastobbe</b>: "
                "soap it with <b>guhshampoo</b>, scrub it five times, rinse it in the tub or in water and blow-dry it with the <b>guh-föhn</b>: it is fluffy and "
                "<b>shiny</b> for a whole day. The showpiece: <b>three water slides</b>, ridden in first person in a <b>zwembandje</b>: the <b>Roze Trechter</b> (spinning "
                "funnels), the <b>Glimtunnel</b> (a glow-in-the-dark star tunnel) and the <b>Grote Plons</b> (a jump and a big splash). Steer left and right to grab "
                "<b>rubber ducks</b>; every slide has its own highscore, and there are <b>12 special ducks</b> to find (some only on one slide, the golden one is very "
                "rare). You earn <b>eendjesmunten</b> for the badmeester's shop (badmutsje, badjasje, shampoo, föhn, tubs, tiles and foam).",
                "Een groot zwembad aan de guhzee, met <b>Badmeester Bubbel</b> (<i>Tuuut!</i> niet rennen!). <b>Was je guh</b> in een <b>guh-wastobbe</b>: zeep hem in met "
                "<b>guhshampoo</b>, schrob hem vijf keer, spoel hem in de tobbe of in het water en föhn hem met de <b>guh-föhn</b>: hij is een hele dag pluizig en "
                "<b>glanzend</b>. Het pronkstuk: <b>drie glijbanen</b>, die je in de ik-persoon afgaat in een <b>zwembandje</b>: de <b>Roze Trechter</b> (draaiende "
                "trechters), de <b>Glimtunnel</b> (een sterrentunnel die in het donker glimt) en de <b>Grote Plons</b> (een sprong en een grote plons). Stuur naar links en "
                "rechts om <b>badeendjes</b> te pakken; elke glijbaan heeft een eigen highscore, en er zijn <b>12 bijzondere eendjes</b> te vinden (sommige alleen op één "
                "glijbaan, het gouden is heel zeldzaam). Je verdient <b>eendjesmunten</b> voor het winkeltje van de badmeester (badmutsje, badjasje, shampoo, föhn, tobbes, "
                "tegels en schuim).") +
              '<p>' + ' '.join(icon(n, n) for n in ("eendjesmunt", "guhshampoo", "guh_fohn", "guh_wastobbe", "glimtegel", "trechtertegel", "badmutsje", "badjasje", "knuffelbad_badeendje")) + '</p>', wide=True) + \
        entry(img("badeendjes", "Badeendjes"), "Badeendjes", "Badeendjes",
              p("A normal duck, the guheendje, the badmeester-eendje, the duikeendje, the maaneendje and the rare golden one.",
                "Een gewoon eendje, het guheendje, het badmeester-eendje, het duikeendje, het maaneendje en het zeldzame gouden eendje.")) + \
        entry(img("zwembandje", "Zwembandje"), "The zwembandje", "Het zwembandje",
              p("Your ride down the slides, with a guh head at the front.", "Je ritje van de glijbaan, met een guhkopje voorop."))

    # ---------------------------------------------------------------- everywhere
    overal = h3("Everywhere in the Guhmension", "Overal in de Guhmensie") + \
        entry(img("ijscoguh", "IJscoguh Tingeling"), "IJscoguh Tingeling", "IJscoguh Tingeling",
              p("<i>Tingeling! Tingeling!</i> Now and then <b>IJscoguh Tingeling</b> cycles through the Guhmension with his ice cream cart (and visits the plein); the guhs "
                "run after him. He sells <b>kaasijsjes</b> for kaasknabbels: roze (blosjes), mint (a bit faster) and choco (light as a pluisje), plus one per "
                "season: bloesem (lente), zonnetje (zomer), appeltaart (herfst) and sneeuw (winter). Give one to your guh and it gets an <b>ice cream hat</b>. "
                "After a day he cycles on.",
                "<i>Tingeling! Tingeling!</i> Af en toe fietst <b>IJscoguh Tingeling</b> met zijn ijscokar door de Guhmensie (en langs het plein); de guhs rennen hem "
                "achterna. Hij verkoopt <b>kaasijsjes</b> voor kaasknabbels: roze (blosjes), mint (even harder rennen) en choco (zo licht als een pluisje), plus één per "
                "seizoen: bloesem (lente), zonnetje (zomer), appeltaart (herfst) en sneeuw (winter). Geef er een aan je guh en hij krijgt een <b>ijshoedje</b>. Na een dag "
                "fietst hij weer verder.") +
              '<p>' + ' '.join(icon(n, n) for n in ("kaasijsje_roze", "kaasijsje_mint", "kaasijsje_choco", "kaasijsje_bloesem", "kaasijsje_zonnetje", "kaasijsje_appeltaart", "kaasijsje_sneeuw", "ijscopetje")) + '</p>') + \
        entry(img("guh_xylofoon", "Guh-xylofoon"), "The koortje", "Het koortje",
              p("Play a song on the <b>guh-xylofoon</b>: all guhs nearby sing along, wobbling, with little notes (and gardens grow from it). The <b>guh-fluitje</b> plays your "
                "last song wherever you are. Six songs for the <b>liedjesboek</b>: <i>De VAHOEGE Toonladder</i>, <i>Vader Guh</i>, <i>Altijd is Guhtje vads</i>, "
                "<i>Alle guhtjes zwemmen in de kaassaus</i>, <i>In de knabbelmaneschijn</i> and <i>Ode aan de Knabbel</i>.",
                "Speel een liedje op de <b>guh-xylofoon</b>: alle guhs in de buurt zingen wiebelend mee, met muzieknootjes (en tuintjes groeien ervan). Het <b>guh-fluitje</b> "
                "speelt je laatste liedje waar je ook bent. Zes liedjes voor het <b>liedjesboek</b>: <i>De VAHOEGE Toonladder</i>, <i>Vader Guh</i>, <i>Altijd is Guhtje "
                "vads</i>, <i>Alle guhtjes zwemmen in de kaassaus</i>, <i>In de knabbelmaneschijn</i> en <i>Ode aan de Knabbel</i>.") +
              '<p>' + ' '.join(icon(n, n) for n in ("guh_xylofoon", "guh_fluitje", "koorstrikje", "wereldleven_liedjesboekje")) + '</p>') + \
        entry(img("grijpmachine", "Grijpmachine"), "The grijpmachine", "De grijpmachine",
              p("At the guh kermis and on the plein of the Knuffeldal. One go costs a ticket: a <b>kermisbon</b> or a coin of a minigame (bakmunt, speenmunt, krulmunt or "
                "eendjesmunt). Steer the claw yourself (WASD or the arrows, space to grab) and grab a <b>guh plushie</b>: one for each of the 20 real guh kinds, and a rare "
                "golden <b>glitterknuffel</b>. Plus a kermisbon, VAHOEG! Put the plushies down as deco: tamed guhs come to cuddle them. Fill the <b>knuffelkast</b> (21).",
                "Op de guhkermis en op het plein van het Knuffeldal. Eén keer kost een kaartje: een <b>kermisbon</b> of een munt van een minigame (bakmunt, speenmunt, "
                "krulmunt of eendjesmunt). Stuur de klauw zelf (WASD of de pijltjes, spatie om te grijpen) en grijp een <b>guhknuffel</b>: een voor elk van de 20 echte "
                "guhsoorten, en een zeldzame gouden <b>glitterknuffel</b>. Plus een kermisbon, VAHOEG! Zet de knuffels neer als deco: tamme guhs komen ze knuffelen. Vul de "
                "<b>knuffelkast</b> (21).")) + \
        entry(img("knuffels", "Guh plushies"), "The 21 plushies", "De 21 knuffels",
              p("Every real guh kind as a plushie, the Pluisguh included, and the glitterknuffel.", "Elke echte guhsoort als knuffel, de Pluisguh ook, en de glitterknuffel."), wide=True)

    # ---------------------------------------------------------------- the Guhdex, restyles, the Guheinde, FTB
    dex = h3("The Guhdex: tab Knus", "De Guhdex: tabblad Knus") + \
        entry(icon("guhdex", "Guhdex").replace('class="px"', 'class="px" style="width:96px;height:96px"'), "Guhs &middot; Knus &middot; Highscores", "Guhs &middot; Knus &middot; Highscores",
              p("The Guhdex now has three tabs. <b>Knus</b> has a part for every 2.8 feature (Het Knuffeldal, Seizoenen, Knabbelbakkerij, Knuffelcreche, Knabbelthee-huisje, "
                "Knip &amp; Vads, Guhboerderij, Guhtuintjes, Guh-Sterrenwacht, Ballonfestival, Kampeerplekjes, Knuffelbad and Guhleven) with <b>milestones</b> and progress "
                "bars (claim the reward when one is full) and <b>collection pages</b>: knuffelvriendjes, seizoensplakboek, receptenboek, slaapliedjes, theesoorten, "
                "kapselcollectie, tuinboek, sterrenatlas, ballonstempelkaart, verhalenbundel (read Opa's stories again), badeendjes, knuffelkast, liedjesboek and kaasijsjes. "
                "<b>Highscores</b> got six new games: Knabbelbakkerij, Babyguhtjes terugbrengen, Kappersshow and the three slides. New Guhdex pages: the <b>Pluisguh</b>, "
                "the eleven characters, the <b>Kruimel-Mika</b>, the three farm animals and <b>IJscoguh Tingeling</b>.",
                "De Guhdex heeft nu drie tabbladen. <b>Knus</b> heeft een onderdeel voor elk 2.8-ding (Het Knuffeldal, Seizoenen, Knabbelbakkerij, Knuffelcreche, "
                "Knabbelthee-huisje, Knip &amp; Vads, Guhboerderij, Guhtuintjes, Guh-Sterrenwacht, Ballonfestival, Kampeerplekjes, Knuffelbad en Guhleven) met "
                "<b>mijlpalen</b> en voortgangsbalkjes (haal de beloning op als er een vol is) en <b>verzamelpagina's</b>: knuffelvriendjes, seizoensplakboek, receptenboek, "
                "slaapliedjes, theesoorten, kapselcollectie, tuinboek, sterrenatlas, ballonstempelkaart, verhalenbundel (lees de verhalen van Opa nog eens), badeendjes, "
                "knuffelkast, liedjesboek en kaasijsjes. <b>Highscores</b> kreeg zes nieuwe spellen: Knabbelbakkerij, Babyguhtjes terugbrengen, Kappersshow en de drie "
                "glijbanen. Nieuwe Guhdex-pagina's: de <b>Pluisguh</b>, de elf personages, de <b>Kruimel-Mika</b>, de drie boerderijdieren en <b>IJscoguh Tingeling</b>."), wide=True)
    restyle = h3("A new look for three old friends", "Een nieuw jasje voor drie oude vriendjes") + \
        entry(img("rookguh", "Rookguh"), "The Rookguh", "De Rookguh",
              p("The Barbecuether's Rookguh now looks like a real little ghast (about three blocks) with a flat guh face: big glossy guh eyes, a tiny snoet and blush. "
                "Its nine tentacles sway as it floats. Still always peaceful: feed it until it is vahoeg and it floats home.",
                "De Rookguh uit de Barbecuether lijkt nu op een echte kleine ghast (zo'n drie blokken) met een plat guhgezichtje: grote glanzende guhogen, een klein snoetje "
                "en blosjes. Zijn negen tentakels wiegen als hij zweeft. Nog altijd vredig: voer hem tot hij vahoeg is en hij zweeft naar huis.")) + \
        entry(img("kikkerguhs", "Kikkerguhs"), "The Kikkerguh", "De Kikkerguh",
              p("\"A guh that is a frog\": the real guh head with two big round ears, glossy guh eyes, a small snoet and blush, on a spotted frog body with frog legs. "
                "Still pink, mint or yellow, and still hopping, croaking and catching kaasmotten.",
                "\"Een guh die een kikker is\": de echte guhkop met twee grote ronde oren, glanzende guhogen, een klein snoetje en blosjes, op een gespikkeld kikkerlijfje met "
                "kikkerpootjes. Nog steeds roze, mint of geel, en nog steeds huppend, kwakend en kaasmotten happend.")) + \
        entry(img("npc_reisguh", "Reisguh"), "The Reisguh", "De Reisguh",
              p("A real guh conductor: a dark blue conductor's cap with a shiny peak and a golden guh badge, and a golden whistle on a cord. Now and then, and every time "
                "you travel, he blows it: <i>tuut!</i>",
                "Een echte guh-conducteur: een donkerblauw conducteurspetje met een glimmende klep en een gouden guh-embleem, en een gouden fluitje aan een koord. Af en toe, "
                "en elke keer dat je reist, blaast hij erop: <i>tuut!</i>"))
    einde = h3("The Guheinde: Terugpoorten and more vetschepen", "Het Guheinde: Terugpoorten en meer vetschepen") + \
        entry(img("structure_guheinde_terugpoort", "Terugpoort"), "Terugpoorten", "Terugpoorten",
              p("All over the outer islands of the Guheinde (every few hundred blocks) stands a <b>Terugpoort</b>: a little lit Knabbelpoort shrine with a guh head. Step in "
                "and you float back to the main island, safely on the platform. The superkompas (category Einde) points to the nearest one. And <b>half</b> of the "
                "Mika-vestingen now have a <b>vetschip</b> (was a third), so the Guhvleugels are easier to find.",
                "Overal op de buiteneilanden van het Guheinde (om de paar honderd blokken) staat een <b>Terugpoort</b>: een verlicht Knabbelpoortje met een guhkop. Stap erin en "
                "je zweeft terug naar het grote eiland, veilig op het platform. Het superkompas (categorie Einde) wijst de dichtstbijzijnde aan. En de <b>helft</b> van de "
                "Mika-vestingen heeft nu een <b>vetschip</b> (was een derde), dus de Guhvleugels zijn makkelijker te vinden."))
    ftb = h3("FTB quests: a chapter group", "FTB-quests: een groep hoofdstukken") + \
        p("The FTB quests are no longer one big chapter: they are a chapter group <b>Guhs</b> with themed chapters (nine in 2.8; 2.8.1 added the tenth, <i>Piep!</i>), each with its own pictures (guh renders, "
          "buildings, guh faces) and icon, sections inside, and a <i>Hoe kom je hier?</i> quest at the top that links to the quest in another chapter where it starts. "
          "Your progress is kept (the quest ids stayed the same). Nothing is locked, except the stomach sizes in De Guhmaag. Read them from top to bottom:",
          "De FTB-quests zijn niet meer één groot hoofdstuk: het is een groep <b>Guhs</b> met hoofdstukken per thema (negen in 2.8; 2.8.1 voegde het tiende toe, <i>Piep!</i>), elk met eigen plaatjes (guhrenders, "
          "gebouwen, guhgezichtjes) en een icoontje, onderdelen erin, en bovenaan een <i>Hoe kom je hier?</i>-quest met een linkje naar de quest in een ander hoofdstuk "
          "waar het begint. Je voortgang blijft bewaard (de quest-id's zijn hetzelfde gebleven). Niets zit op slot, behalve de maaggroottes in De Guhmaag. Lees ze van "
          "boven naar beneden:") + \
        table([("#", "#"), ("Chapter", "Hoofdstuk"), ("Subtitle", "Ondertitel"), ("Quests", "Quests")],
              [[str(i + 1), f"<b>{title}</b>", f"<i>{sub}</i>", str(n)] for i, (_, title, sub, n) in enumerate(chapters)]) + \
        p(f"The Knuffeldal chapter alone has {knus_quests} quests: every building has its own row.",
          f"Het hoofdstuk Knuffeldal heeft alleen al {knus_quests} quests: elk gebouw heeft zijn eigen rijtje.")

    rec = h3("Some recipes", "Een paar recepten") + '<div class="recipes">' + "".join(recipe_card(r) for r in (
        "knuffelsteen", "knuffelsteen_gezicht", "pluisdak", "knuffelklinkers", "knabbeloven", "guh_bloempot", "guh_moestuinbak", "guh_gieter", "feestboeket",
        "guhborstel", "knabbelvoer", "guh_voerbak", "kippennestje", "theetafel", "theepotje", "guh_wiegje", "babyflesje", "schone_luier", "knuffeldekentje",
        "kappersstoel", "haarverf_roze", "sterrenlantaarn", "mini_luchtballon", "guh_slaapzak", "guh_slaapzak_wol", "marshmallow_knabbel", "guh_wastobbe",
        "guhshampoo", "guh_fohn", "glimtegel", "guh_xylofoon", "guh_fluitje", "sneeuwguhkopje", "feestbuffettafel")) + '</div>'
    STAR = ' <span title="challenge">&#9733;</span>'
    adv_rows = lambda rows: table([("Advancement", "Vooruitgang"), ("How", "Hoe")], [[f"<b>{a}</b>{STAR if star else ''}", (en, nl)] for a, en, nl, star in rows])
    adv = h3("Advancements: tab Het Knuffeldal", "Vooruitgangen: tabblad Het Knuffeldal") + adv_rows([
        ("Het Knuffeldal / Zo zacht! / Welkom in het stadje", "Step into the Guhmension, walk on knuffelgras, find the town.", "Stap de Guhmensie in, loop over knuffelgras, vind het stadje.", False),
        ("Ahum! / Kruimeldief! / Het Grote Knusfeest", "Talk to the Burgemeester, lure away a Kruimel-Mika, celebrate the Knusfeest.", "Praat met de Burgemeester, lok een Kruimel-Mika weg, vier het Knusfeest.", False),
        ("Knuffelburgemeester", "Become Knuffelburgemeester.", "Word Knuffelburgemeester.", True),
        ("Superpluizig / Het hele jaar knus / Ze hangen aan haar veh", "Tame a Pluisguh, fill the seizoensplakboek, help Cocotje.", "Tem een Pluisguh, vul het seizoensplakboek, help Cocotje.", False),
        ("Warme broodjes! / Bakken aan de lopende band", "A first bakery order, six happy customers in a row.", "Een eerste bestelling, zes blije klantjes op rij.", False),
        ("Meesterbakker", "Bake all 12 recipes.", "Bak alle 12 recepten.", True),
        ("Stil maar, babyguhtje / Niemand ontsnapt! / Het hele liedjesboek", "A care round, 8 babies back in one game, all four lullabies.", "Een verzorgronde, 8 babyguhtjes terug in één spelletje, alle vier de slaapliedjes.", False),
        ("Gezellig! / Zelfgebakken! / Theeproever", "A gezellig theekransje, serve something from the bakery, all four teas.", "Een gezellig theekransje, serveer iets uit de bakkerij, alle vier de theesoorten.", False),
        ("Knip knip! / Vahoege kappersshow", "A first hairstyle, 120 points in a kappersshow.", "Een eerste kapsel, 120 punten in een kappersshow.", False),
        ("Kapselcollectie compleet", "All 8 hairstyles and all 8 dyes.", "Alle 8 kapsels en alle 8 haarverfjes.", True),
        ("Boer zoekt Guh / Blij beestje / Wol, ei en melk", "Find the farm, make an animal happy, get all three products.", "Vind de boerderij, maak een dier blij, krijg alle drie de producten.", False),
        ("Eigen kweek / Groene guhvingers", "A first harvest, the whole tuinboek.", "Een eerste oogst, het hele tuinboek.", False),
        ("Hoog in de sterren / Verbind de sterretjes / Een vallende knabbel!", "Find the sterrenwacht, a first constellation, a rare one.", "Vind de sterrenwacht, een eerste sterrenbeeld, een zeldzaam sterrenbeeld.", False),
        ("De hele sterrenatlas", "All 15 constellations.", "Alle 15 sterrenbeelden.", True),
        ("Ballonnen in de lucht! / Hoog, hoger, VAHOEG! / Volle stempelkaart", "Find the festival, a first flight, all 8 stamps.", "Vind het festival, een eerste vlucht, alle 8 stempels.", False),
        ("Kampvuurtje / Er was eens... / De hele verhalenbundel / Lekker uitgeslapen", "Find a camp site, a first story, all 12 stories, a night in a slaapzak.", "Vind een kampeerplekje, een eerste verhaal, alle 12 verhalen, een nacht in een slaapzak.", False),
        ("Plons! / Schoon en vahoeg / Wieeeee!", "Find the Knuffelbad, wash your guh, a first slide.", "Vind het Knuffelbad, was je guh, een eerste glijbaan.", False),
        ("Glijbaankampioen / Eendjesverzamelaar", "All three slides; all 12 special ducks.", "Alle drie de glijbanen; alle 12 bijzondere eendjes.", True),
        ("Tingeling! / Het koortje / Het hele liedjesboekje / Grijpen maar!", "An ice cream, a song with singing guhs, all six songs, a plushie from the grijpmachine.", "Een ijsje, een liedje met zingende guhs, alle zes de liedjes, een knuffel uit de grijpmachine.", False),
        ("De volle knuffelkast", "All 21 plushies.", "Alle 21 knuffels.", True)]) + \
        p("&#9733; = challenge (purple frame).", "&#9733; = uitdaging (paarse rand).")
    cmds = h3("Handy commands", "Handige commando's") + \
        cmd("/execute in guhs:guhmension run locate biome guhs:knuffeldal") + cmd("/execute in guhs:guhmension run locate structure guhs:knuffeldal_stadje") + \
        cmd("/execute in guhs:guhmension run locate structure guhs:guhboerderij") + cmd("/execute in guhs:guhmension run locate structure guhs:guh_sterrenwacht") + \
        cmd("/execute in guhs:guhmension run locate structure guhs:ballonfestival") + cmd("/execute in guhs:guhmension run locate structure guhs:kampeerplekje") + \
        cmd("/execute in guhs:guhmension run locate structure guhs:knuffelbad") + cmd("/guhs seizoen") + cmd("/guhs seizoen winter") + \
        cmd("/guhs evenement knusfeest") + cmd("/guhs ijscoguh")

    body = intro + parts + dal + feest + tijd + plein + buiten + overal + dex + restyle + einde + ftb + rec + adv + cmds
    return section("new28", "New in 2.8: Het Knuffeldal", "Nieuw in 2.8: Het Knuffeldal", body)


def piep_section():
    """2.8.1 "Piep": the pieppiepmuisje, Poepschilly and Schilly, the roze guh koek, and the kaasknabbel-nest with the boze
    kaasknabbels and the Boze Oppernabbel. Texts after tools/features/piep.py (lang, piepboek, FTB) and PIEP_NOTES."""
    piep_quests = next((n for f, _, _, n in ftb_chapters() if f == "guhs_piep"), 0)
    shot = lambda name, alt: img("shot_" + name, alt, "shot")
    wentry = lambda *a, **k: entry(*a, wide=True, **k)     # (in-game screenshots: wide, they are landscape)
    big = lambda name, label: icon(name, label).replace('class="px"', 'class="px" style="width:96px;height:96px"')
    intro = p("Guhs 2.8.1 <b>Piep</b> is a small, sweet update on top of the Knuffeldal. Piep piep! Tiny <b>pieppiepmuisjes</b> moved into the lieve guh houses, "
              "two plush sea turtles live on the guhzee coast (<b>Poepschilly</b> and <b>Schilly</b>, and no, they are not the same turtle), there is a new "
              "treat, the <b>Roze Guh Koek</b>, and somewhere in the Guhmension stands a cheese-crust hill full of <b>boze kaasknabbels</b>. Knabbels are not "
              "guhs (they are a snack!), so they may be angry. But when you beat them, it turns out they were just... zieli.",
              "Guhs 2.8.1 <b>Piep</b> is een kleine, lieve update bovenop het Knuffeldal. Piep piep! Piepkleine <b>pieppiepmuisjes</b> zijn in de lieve guhhuizen "
              "komen wonen, aan de guhzee-kust wonen twee pluchen zeeschildpadden (<b>Poepschilly</b> en <b>Schilly</b>, en nee, dat is niet dezelfde schildpad), "
              "er is een nieuw lekkers, de <b>Roze Guh Koek</b>, en ergens in de Guhmensie staat een kaaskorstheuvel vol <b>boze kaasknabbels</b>. Knabbels zijn "
              "geen guhs (het is een snack!), dus die mogen best boos zijn. Maar als je ze verslaat, blijken ze gewoon... zieli.")
    parts = ul([("<b>Pieppiepmuisje</b>: peeps, runs to kaasknabbels, tame it, carry it on your shoulder, pick it up, play verstoppertje.",
                 "<b>Pieppiepmuisje</b>: piept, rent naar kaasknabbels, tem het, draag het op je schouder, pak het op, speel verstoppertje."),
                ("<b>Poepschilly</b>: the kontpoetser. A poetsbeurt makes your guh <i>Fris van binnen</i>.",
                 "<b>Poepschilly</b>: de kontpoetser. Na een poetsbeurt is je guh <i>Fris van binnen</i>."),
                ("<b>Schilly</b>: a different turtle, a minihoofdje and the guhs' bestie (with the occasional beef).",
                 "<b>Schilly</b>: een andere schildpad, een minihoofdje en de bestie van de guhs (met af en toe beef)."),
                ("<b>Roze Guh Koek</b>: a tray with 1-6 koeken; eat one and you are <i>Lief kijken</i>.",
                 "<b>Roze Guh Koek</b>: een bakje met 1-6 koeken; eet er een en je gaat <i>Lief kijken</i>."),
                ("<b>Kaasknabbel-nest</b>: three waves of boze kaasknabbels, then the <b>Boze Oppernabbel</b>, and the secret koek recipe.",
                 "<b>Kaasknabbel-nest</b>: drie golven boze kaasknabbels, daarna de <b>Boze Oppernabbel</b>, en het geheime koekrecept."),
                ("<b>Also new</b>: the Guhdex section <i>Piep!</i> in the Knus tab, and the FTB chapter <i>Piep!</i>.",
                 "<b>Ook nieuw</b>: het Guhdex-onderdeel <i>Piep!</i> in het tabblad Knus, en het FTB-hoofdstuk <i>Piep!</i>.")])

    # ---------------------------------------------------------------- the muisje
    muis = h3("The pieppiepmuisje", "Het pieppiepmuisje") + \
        entry(img("pieppiepmuisje", "Pieppiepmuisje"), "Piep piep!", "Piep piep!",
              p("A tiny plush mouse (about a third of a block): dark purple, with a fluffy cream band round its tummy, a cream chin, a shiny pink nose and felt ears. "
                "It lives in the <b>lieve guh buildings of the Guhmension</b>: roughly <b>one muisje for every ten guhs</b> in a guh house, a village, the Knuffeldal "
                "town and so on. Never in Mika places, never in the kaasknabbel-nest, and never in the Barbecuether (not lief enough there). It scurries about, "
                "<b>peeps</b> (five real piep recordings) and <b>runs to kaasknabbels</b> lying on the ground to nibble them.",
                "Een piepklein pluchen muisje (zo'n derde blok): donkerpaars, met een pluizig crème bandje om zijn buik, een crème kinnetje, een glimmend roze "
                "neusje en vilten oortjes. Het woont in de <b>lieve guhgebouwen van de Guhmensie</b>: ongeveer <b>één muisje per tien guhs</b> in een guhhuis, een "
                "dorp, het Knuffeldal-stadje enzovoort. Nooit bij de Mika's, nooit in het kaasknabbel-nest, en nooit in de Barbecuether (daar is het niet lief "
                "genoeg). Het scharrelt rond, <b>piept</b> (vijf echte piepopnames) en <b>rent naar kaasknabbels</b> die op de grond liggen om eraan te knabbelen."),
              stats=[(("Size", "Grootte"), "~1/3 " + t("block", "blok")), (("Spawns", "Spawnt"), t("1 in 10 guhs, lieve buildings", "1 op 10 guhs, lieve gebouwen")),
                     (("Tame", "Temmen"), t("kaasknabbel (1 in 3)", "kaasknabbel (1 op 3)"))]) + \
        wentry(shot("piep_schouder_voor", "A muisje on your shoulder"), "Taming, shoulder, pocket", "Temmen, schouder, broekzak",
              ul([("<b>Aaien</b>: right-click with an empty hand. The muisje peeps happily (the Guhdex counts every aai).",
                   "<b>Aaien</b>: rechtsklik met een lege hand. Het muisje piept blij (de Guhdex telt elke aai)."),
                  ("<b>Tame</b> it with a <b>kaasknabbel</b>: 1 in 3 per knabbel, or always when you are <i>Lief kijken</i> (see the koek). Then it follows you.",
                   "<b>Tem</b> het met een <b>kaasknabbel</b>: 1 op 3 per knabbel, of altijd als je <i>Lief kijkt</i> (zie de koek). Daarna loopt het achter je aan."),
                  ("<b>Pick it up</b>: sneak + right-click your own muisje. It becomes an item that keeps its name and everything.",
                   "<b>Oppakken</b>: sluip + rechtsklik je eigen muisje. Het wordt een voorwerp dat zijn naam en alles onthoudt."),
                  ("With that item: right-click a block and it hops off there; right-click in the air and it climbs <b>onto your shoulder</b>, looking where you look. "
                   "Sneak + right-click a block with an empty hand and it hops down again. If you die, it jumps off in time.",
                   "Met dat voorwerp: rechtsklik op een blok en het hopt daar neer; rechtsklik in de lucht en het klimt <b>op je schouder</b> en kijkt mee waar jij kijkt. "
                   "Sluip + rechtsklik met een lege hand op een blok en het hopt er weer af. Ga je dood, dan springt het er op tijd af.")])) + \
        entry(big("pieppiepmuisje_item", "Pieppiepmuisje"), "Verstoppertje", "Verstoppertje",
              p("Now and then your tamed muisje wants to play: <i>\"... wil verstoppertje spelen... piep! Zoek het muisje!\"</i> It hides in a flower pot, a "
                "decorated pot, a barrel, a composter, a cauldron, a hay bale, a guh bloempot, a wastobbe, a nest, a bladerhoopje, a voerbak or a seizoensbloembak "
                "nearby (or in the grass), and keeps peeping until you find it. <b>Right-click the spot where it peeps</b>: a happy muisjesdansje! After three "
                "minutes it comes out by itself (a little proud). The Guhdex has milestones for 3 and 15 finds.",
                "Af en toe wil je tamme muisje spelen: <i>\"... wil verstoppertje spelen... piep! Zoek het muisje!\"</i> Het verstopt zich in een bloempot, een "
                "versierde pot, een ton, een compostbak, een ketel, een hooibaal, een guh-bloempot, een wastobbe, een nestje, een bladerhoopje, een voerbak of een "
                "seizoensbloembak in de buurt (of in het gras), en blijft piepen tot je het vindt. <b>Rechtsklik op de plek waar het piept</b>: een blij "
                "muisjesdansje! Na drie minuten komt het zelf tevoorschijn (een tikje trots). De Guhdex heeft mijlpalen voor 3 en 15 keer gevonden."))

    # ---------------------------------------------------------------- the two turtles
    schild = h3("Two turtles: Poepschilly and Schilly", "Twee schildpadden: Poepschilly en Schilly") + \
        p("<b>Careful: these are two different turtles.</b> They look a lot alike (same shape, same size, both from the guhzee coast), but they are slightly "
          "different: <b>Poepschilly</b> is the dark brown one, and he is the kontpoetser. <b>Schilly</b> is the green one with the spotted head and flippers: "
          "a minihoofdje who never cleans anything, but who is the guhs' bestie. The Guhdex has a page for each of them.",
          "<b>Let op: dit zijn twee verschillende schildpadden.</b> Ze lijken veel op elkaar (dezelfde vorm, even groot, allebei van de guhzee-kust), maar ze "
          "verschillen net een beetje: <b>Poepschilly</b> is de donkerbruine, en hij is de kontpoetser. "
          "<b>Schilly</b> is de groene met het gespikkelde koppie en de gespikkelde flippers: een minihoofdje dat nooit iets poetst, maar wel de bestie van de guhs is. De Guhdex heeft voor elk een eigen pagina.") + \
        entry(img("schildpadden", "Schilly and Poepschilly"), "Spot the difference", "Zoek de verschillen",
              p("Dark brown = <b>Poepschilly</b>. Green and spotted = <b>Schilly</b>. Both live along the coasts of the guhzeeen (in the water and on the beach next "
                "to land; you meet Poepschilly about twice as often), swim well and walk on land. Tame either one with <b>kelp</b>, dried kelp, seagrass or a "
                "kaasknabbel; a tamed turtle follows you, on land and under water.",
                "Donkerbruin = <b>Poepschilly</b>. Groen en gespikkeld = <b>Schilly</b>. Allebei wonen ze langs de kust van de guhzeeën (in het water en op het "
                "strandje naast land; Poepschilly kom je ongeveer twee keer zo vaak tegen), ze zwemmen goed en lopen op het land. Tem ze met <b>kelp</b>, gedroogde "
                "kelp, zeegras of een kaasknabbel; een tamme schildpad volgt je, op het land en onder water."), wide=True) + \
        entry(img("poepschilly", "Poepschilly"), "Poepschilly, the kontpoetser", "Poepschilly, de kontpoetser",
              p("Poepschilly has a very special job: he crawls into a guh and <b>cleans its bum from the inside</b>. Right-click your tamed Poepschilly (<i>\"Poepschilly "
                "is er klaar voor!\"</i>), then one of <b>your own tamed guhs</b>. Poepschilly crawls in, the guh <b>wiggles</b> and blows bubbles (it tickles terribly: "
                "guh guh guh!), and after five seconds Poepschilly pops out again: <i>plop!</i> The guh is now <b>Fris van binnen</b>: 25% faster and sparkling, for "
                "three minutes. After that Poepschilly rests for five minutes. Keurig, hoor!",
                "Poepschilly heeft een heel bijzonder beroep: hij kruipt in een guh en <b>poetst zijn billen van binnenuit schoon</b>. Rechtsklik je tamme Poepschilly "
                "(<i>\"Poepschilly is er klaar voor!\"</i>) en dan een van <b>je eigen tamme guhs</b>. Poepschilly kruipt erin, de guh <b>wiebelt</b> en blaast belletjes "
                "(het kriebelt verschrikkelijk: guh guh guh!), en na vijf seconden floept Poepschilly er weer uit: <i>plop!</i> De guh is nu <b>Fris van binnen</b>: 25% "
                "sneller en sprankelend, drie minuten lang. Daarna rust Poepschilly vijf minuten uit. Keurig, hoor!"),
              stats=[(("Effect", "Effect"), "Fris van binnen"), (("Duration", "Duur"), "3 min, +25% " + t("speed", "snelheid")), (("Rest", "Rust"), "5 min")]) + \
        wentry(shot("piep_guh_wiebel", "A guh wiggling during a poetsbeurt"), "The wiggle", "Het gewiebel",
              p("During a poetsbeurt the guh wiggles and bubbles. Nothing to worry about. It is <i>very</i> clean afterwards.",
                "Tijdens een poetsbeurt wiebelt en bubbelt de guh. Niks aan de hand. Daarna is hij <i>heel</i> schoon.")) + \
        entry(img("schilly", "Schilly"), "Schilly, the minihoofdje-bestie", "Schilly, het minihoofdje-bestie",
              p("Schilly never cleans a guh. Schilly is a <b>minihoofdje</b>, and the guhs' <b>bestie</b>. Now and then Schilly waddles over to a guh nearby: usually "
                "for a <b>bestie moment</b> (hearts, a happy wiggle). But about one time in four they have <b>beef</b>: they turn their backs on each other with "
                "angry puffs (<i>\"Schilly en ... hebben beef... (minihoofdje!)\"</i>). Wait a moment: <i>\"...toch besties &lt;3\"</i>. They always make up. "
                "Right-click your tamed Schilly and then one of your guhs for a bestie moment on purpose: both get <b>Besties</b> (regeneration, 30 seconds), and "
                "Schilly rests for three minutes.",
                "Schilly poetst nooit een guh. Schilly is een <b>minihoofdje</b>, en de <b>bestie</b> van de guhs. Af en toe waggelt Schilly naar een guh in de buurt: "
                "meestal voor een <b>bestie-moment</b> (hartjes, een blij gewiebel). Maar ongeveer één op de vier keer hebben ze <b>beef</b>: ze draaien elkaar met boze "
                "wolkjes de rug toe (<i>\"Schilly en ... hebben beef... (minihoofdje!)\"</i>). Wacht even: <i>\"...toch besties &lt;3\"</i>. Ze maken het altijd weer "
                "goed. Rechtsklik je tamme Schilly en dan een van je guhs voor een bestie-moment met opzet: allebei krijgen ze <b>Besties</b> (regeneratie, 30 "
                "seconden), en Schilly rust drie minuten uit."),
              stats=[(("Effect", "Effect"), "Besties"), (("Duration", "Duur"), "30 s, " + t("regeneration", "regeneratie")), (("Rest", "Rust"), "3 min")]) + \
        wentry(shot("piep_twee_schildpadden", "Poepschilly and Schilly in the game"), "In the game", "In het spel",
              p("Schilly (left, green) and Poepschilly (right, brown). Almost the same... but not quite.",
                "Schilly (links, groen) en Poepschilly (rechts, bruin). Bijna hetzelfde... maar net niet."))

    # ---------------------------------------------------------------- the koek
    koek = h3("The Roze Guh Koek", "De Roze Guh Koek") + \
        entry(img("roze_guh_koek_bakje", "A tray of roze guh koeken"), "Six in a tray", "Zes in een bakje",
              p("A pink-glazed koek with big shiny blue eyes and a tiny snoet. Place it and you get a clear plastic <b>tray</b> with room for <b>six</b>: right-click "
                "the tray with a koek to add one. With an empty hand you eat one straight from the tray; sneak to take one out instead. <b>Eat one</b> (from your "
                "hand or the tray) and you get <b>Lief kijken</b> for three minutes: you look so sweet that guhs around you make <b>hearts</b>, and taming guhs, "
                "muisjes and Poepschilly becomes a lot easier.",
                "Een roze geglazuurde koek met grote glimmende blauwe ogen en een klein snoetje. Zet hem neer en je krijgt een doorzichtig plastic <b>bakje</b> met "
                "plek voor <b>zes</b>: rechtsklik het bakje met een koek om er een bij te leggen. Met een lege hand eet je er meteen een uit het bakje; sluip om er "
                "een uit te pakken. <b>Eet er een</b> (uit je hand of uit het bakje) en je krijgt drie minuten <b>Lief kijken</b>: je kijkt zo lief dat de guhs om je "
                "heen <b>hartjes</b> maken, en guhs, muisjes en Poepschilly temmen gaat veel makkelijker."),
              stats=[(("Effect", "Effect"), "Lief kijken"), (("Duration", "Duur"), "3 min"), (("Tray", "Bakje"), "1-6 koeken")]) + \
        entry(img("roze_guh_koeken", "Trays with 1 to 6 koeken"), "One to six", "Een tot zes",
              p("The tray from one koek to six.", "Het bakje van één koek tot zes."), wide=True) + \
        entry(big("roze_guh_koek_recept", "Recept: Roze Guh Koek"), "The secret recipe", "Het geheime recept",
              p("There is no crafting recipe. The recipe <b>Recept: Roze Guh Koek</b> lies in the treasure chest of the <b>kaasknabbel-nest</b> (the first time you "
                "win). Right-click it to learn it; from then on you (only you) can bake the koek in the <b>Knabbeloven</b> of the Knabbelbakkerij: <b>zoetdeeg + "
                "plaatje + glazuur</b>. Before that the oven says: <i>\"Dat recept ken je nog niet...\"</i> You can already find a few koeken: 30% of the chests "
                "in lieve buildings (guh villages, hamster houses, picnics, the guh castle, the boomhutdorp, camp sites, the ballonfestival, the Guhboerderij, the "
                "sterrenwacht, the floating islands) hold 1-3, and some trays stand ready on the tables of the ballonfestival, the boomhutdorp and the Knuffeldal town.",
                "Er is geen werkbankrecept. Het recept <b>Recept: Roze Guh Koek</b> ligt in de schatkist van het <b>kaasknabbel-nest</b> (de eerste keer dat je wint). "
                "Rechtsklik het om het te leren; vanaf dan kun jij (alleen jij) de koek bakken in de <b>Knabbeloven</b> van de Knabbelbakkerij: <b>zoetdeeg + plaatje + "
                "glazuur</b>. Daarvoor zegt de oven: <i>\"Dat recept ken je nog niet...\"</i> Een paar koeken kun je al vinden: 30% van de kisten in lieve gebouwen "
                "(guhdorpen, hamsterhuizen, picknicks, het guhkasteel, het boomhutdorp, kampeerplekjes, het ballonfestival, de Guhboerderij, de sterrenwacht, de "
                "zwevende eilanden) heeft er 1-3, en op de tafels van het ballonfestival, het boomhutdorp en het Knuffeldal-stadje staan al bakjes klaar.")) + \
        wentry(shot("piep_koek_bakjes", "Koek trays in the game"), "In the game", "In het spel",
              p("Trays with one, three and six koeken.", "Bakjes met één, drie en zes koeken."))

    # ---------------------------------------------------------------- the nest
    nest = h3("The kaasknabbel-nest", "Het kaasknabbel-nest") + \
        entry(img("structure_kaasknabbel_nest", "The kaasknabbel-nest"), "A hill full of holes", "Een heuvel vol gaten",
              p("A rare <b>kaaskorst hill</b> (37 &times; 37 blocks) full of swiss-cheese holes, with guh faces peeking out on every side, pink flowers round its foot "
                "and <b>four tunnels</b> into a round arena. It stands on the land of the Guhmension (not in the Knuffeldal). The <b>superkompas</b> points the way "
                "(category <b>Avontuur</b>). The nest belongs to the knabbels: you can't break it (<i>\"niet slopen, njeg!\"</i>).",
                "Een zeldzame <b>kaaskorstheuvel</b> (37 &times; 37 blokken) vol gatenkaasgaten, met aan elke kant guhgezichtjes die naar buiten kijken, roze bloemen "
                "rond de voet en <b>vier tunnels</b> naar een ronde arena. Hij staat op het land van de Guhmensie (niet in het Knuffeldal). Het <b>superkompas</b> "
                "wijst de weg (categorie <b>Avontuur</b>). Het nest is van de knabbels: je kunt het niet slopen (<i>\"niet slopen, njeg!\"</i>)."),
              stats=[(("Size", "Grootte"), "37 &times; 22 &times; 37"), (("Where", "Waar"), t("Guhmension land", "Guhmensie-land")),
                     (("Superkompas", "Superkompas"), "Avontuur")], wide=True) + \
        wentry(shot("piep_nest_zuid", "The nest in the game"), "In the game", "In het spel",
              p("The nest from the south. Look closely: guh faces!", "Het nest vanaf het zuiden. Kijk goed: guhgezichtjes!")) + \
        entry(img("kaasknabbel_nest_arena", "The arena, cut open"), "The arena and the golden kern", "De arena en de gouden kern",
              p("Inside is a round arena with <b>six little holes</b> in its wall and a <b>golden kern</b> in the middle. Step on the kern if you dare: <i>\"De boze "
                "kaasknabbels worden wakker! Njeg njeg njeg!\"</i> Three <b>waves</b> of boze kaasknabbels come out of the holes: 4, 6 and 8 of them (one more per "
                "extra player, at most two more). A knabbel hops at you and pushes a bit. Beaten knabbels show a little floating <i>\"zieli...\"</i> and drop "
                "kaasknabbels.",
                "Binnen is een ronde arena met <b>zes gaatjes</b> in de muur en een <b>gouden kern</b> in het midden. Stap op de kern als je durft: <i>\"De boze "
                "kaasknabbels worden wakker! Njeg njeg njeg!\"</i> Uit de gaten komen drie <b>golven</b> boze kaasknabbels: 4, 6 en 8 (één meer per extra speler, "
                "hooguit twee meer). Een knabbel hupt op je af en duwt een beetje. Verslagen knabbels laten een zwevend <i>\"zieli...\"</i> zien en laten "
                "kaasknabbels vallen."), wide=True) + \
        entry(img("boze_kaasknabbels", "The Boze Oppernabbel and two boze kaasknabbels"), "The Boze Oppernabbel", "De Boze Oppernabbel",
              p("After the third wave: <i>\"De Boze Oppernabbel komt eraan! Hij is heel boos. Of... gewoon zieli?\"</i> The biggest, angriest knabbel of the nest, three "
                "times as big as the others, with a little crown and a boss bar. His <b>stamp</b> sends you flying: <b>jump</b> just in time and it misses. At half "
                "health he calls <b>three helpers</b>. He never breaks a block. Beat him and... <i>\"Hij was gewoon zieli...\"</i>",
                "Na de derde golf: <i>\"De Boze Oppernabbel komt eraan! Hij is heel boos. Of... gewoon zieli?\"</i> De grootste, boosste knabbel van het nest, drie keer "
                "zo groot als de rest, met een kroontje en een baasbalk. Van zijn <b>stamp</b> vlieg je weg: <b>spring</b> op tijd en hij mist. Op halve kracht roept "
                "hij <b>drie helpers</b>. Hij breekt nooit een blok. Versla hem en... <i>\"Hij was gewoon zieli...\"</i>"),
              stats=[(("Health", "Levens"), "110"), (("Waves", "Golven"), "4 / 6 / 8"), (("Helpers", "Helpers"), "3")]) + \
        wentry(shot("piep_oppernabbel_bossbar", "The Boze Oppernabbel with its boss bar"), "Face to face", "Oog in oog",
              p("The Boze Oppernabbel and one of his knabbels.", "De Boze Oppernabbel en een van zijn knabbels.")) + \
        wentry(shot("piep_nest_binnen", "Inside the arena"), "Inside, and the reward", "Binnen, en de beloning",
              p("When he is beaten, a <b>treasure chest</b> appears on the kern. The first time it holds the <b>Recept: Roze Guh Koek</b> (and knabbels), after that "
                "knabbels only. Three days later the knabbels are awake again and you can do the nest once more.",
                "Als hij verslagen is, verschijnt er een <b>schatkist</b> op de kern. De eerste keer zit het <b>Recept: Roze Guh Koek</b> erin (en knabbels), daarna "
                "alleen knabbels. Drie dagen later zijn de knabbels weer wakker en kun je het nest nog een keer doen."))

    # ---------------------------------------------------------------- Guhdex, FTB, commands
    dex = h3("Guhdex: Piep!", "Guhdex: Piep!") + \
        wentry(shot("piep_knus_overzicht", "The Knus tab with Piep!"), "The section Piep!", "Het onderdeel Piep!",
              p("The Knus tab of the Guhdex has a new section at the bottom: <b>Piep!</b>, with twelve milestones (muisjes petted and tamed, a whole muizenfamilie, "
                "verstoppertje, poetsbeurten, bestie moments with Schilly, beef made up, zielige knabbels beaten, the nest won, koeken eaten) and the collection "
                "<b>Piepboek</b>: six pages with a little story each, for the pieppiepmuisje, Poepschilly, Schilly, the Roze Guh Koek, the boze kaasknabbel and the "
                "Boze Oppernabbel.",
                "Het tabblad Knus van de Guhdex heeft onderaan een nieuw onderdeel: <b>Piep!</b>, met twaalf mijlpalen (muisjes geaaid en getemd, een hele "
                "muizenfamilie, verstoppertje, poetsbeurten, bestie-momenten met Schilly, beef bijgelegd, zielige knabbels verslagen, het nest gewonnen, koeken "
                "gesmikkeld) en de verzameling <b>Piepboek</b>: zes pagina's met elk een verhaaltje, voor het pieppiepmuisje, Poepschilly, Schilly, de Roze Guh Koek, "
                "de boze kaasknabbel en de Boze Oppernabbel."))
    ftb = h3("FTB quests: the chapter Piep!", "FTB-quests: het hoofdstuk Piep!") + \
        p(f"The Guhs chapter group got a tenth chapter, <b>Piep!</b> (<i>Muisjes, Schilly, Poepschilly en zielige knabbels</i>), with {piep_quests} quests: the muisje, and the nest with the koek. Nothing is locked. (Since 2.9 the five turtle quests are in the chapter Onderwater, section Schildpadjes.)",
          f"De Guhs-groep in het questboek kreeg een tiende hoofdstuk, <b>Piep!</b> (<i>Muisjes, Schilly, Poepschilly en zielige knabbels</i>), met {piep_quests} "
          "quests in drie rijtjes: het muisje, de twee schildpadden, en het nest met de koek. Niets zit op slot.")
    cmds = h3("Handy commands", "Handige commando's") + \
        cmd("/execute in guhs:guhmension run locate structure guhs:kaasknabbel_nest") + cmd("/summon guhs:pieppiepmuisje ~ ~ ~") + \
        cmd("/summon guhs:poepschilly ~ ~ ~") + cmd("/summon guhs:schilly ~ ~ ~") + cmd("/give @s guhs:roze_guh_koek 6") + \
        cmd("/give @s guhs:roze_guh_koek_recept")

    body = intro + parts + muis + schild + koek + nest + dex + ftb + cmds
    return section("new281", "New in 2.8.1: Piep!", "Nieuw in 2.8.1: Piep!", body)


def grote_guhspelen_section():
    """2.9.0 "De Grote Guhspelen": six new minigames (sjoelen, doolhof, katapult, knabbelspelen, the Elf-Guhjestocht in the new
    Guhpolder, the Guh-Circuit), levels for the classics, new disco songs, clothing as unlocks (new wardrobe, OREN slot, one
    source per piece), the Guhdex/Superkompas rework, the beroepen, the piep menus, sleeping eyes and more. Texts after
    guhs_work29/DESIGN_29.md, CONTRACT_29.md, the slice reports and the lang files; pictures from wiki_renders.main_v29."""
    minigames_quests = next((n for f, _, _, n in ftb_chapters() if f == "guhs_minigames"), 0)
    shot = lambda name, alt: img("shot29_" + name, alt, "shot")
    wentry = lambda *a, **k: entry(*a, wide=True, **k)
    big = lambda name, label: icon(name, label).replace('class="px"', 'class="px" style="width:80px;height:80px"')
    icons = lambda *names: '<p>' + ' '.join(icon(n, n.replace("_", " ")) for n in names) + '</p>'

    def gfig(name, en, nl, sub_en, sub_nl, den, dnl):
        return f'''<figure><div class="stage">{img(name, en, "shot" if name.startswith("shot") else "")}</div><figcaption><h3>{t(en, nl)}<span class="rarity">{t(sub_en, sub_nl)}</span></h3>
<p>{t(den, dnl)}</p></figcaption></figure>'''

    def outfit(name, en, nl, pieces, coin_en, coin_nl):
        """A card with the outfit render, the pieces with their price and where to spend the coins."""
        rows = "".join(f"<li>{icon(pid, pn)} {pn}: <b>{price}</b></li>" for pid, pn, price in pieces)
        return entry(img(f"guh_outfit_{name}", en), en, nl,
                     p(f"Only here, from the shop, paid in {coin_en}. Unlock a piece once and every tamed guh of yours can wear it:",
                       f"Alleen hier, in het winkeltje, te betalen met {coin_nl}. Eén keer ontgrendelen en al je tamme guhs kunnen het aan:") +
                     f"<ul>{rows}</ul>")

    intro = p("Guhs 2.9 <b>De Grote Guhspelen</b> is the games update. Six new minigames, each in its own big, lief guh building with its own "
              "guh, its own coin, its own outfit and its own floating top-3 boards: <b>sjoelen</b> with Opoe Njegschuif, a <b>hedge maze</b> that is "
              "different every time, a <b>catapult</b> against twelve crooked Mika forts, <b>De Knabbelspelen</b> (a six-part circus contest), the "
              "<b>Elf-Guhjestocht</b>, a skating tour past eleven villages on a frozen canal in the brand-new, ice-cold <b>Guhpolder</b>, and the "
              "<b>Guh-Circuit</b> with three wild race tracks. The seven classic games of 2.4 got <b>makkelijk, medium and lastig</b>, the Guhdisco got "
              "real songs, clothes became <b>unlocks</b> with a brand-new wardrobe (and a slot for the <b>ears</b>), the Guhdex and the superkompas got "
              "icon tabs, and in the Knuffeldal town you can now help the <b>brandweer</b>, the <b>politie</b> and the <b>apotheek</b>. As always: a guh is "
              "never too vads. The Mikas may play along as opponents, but they never do any harm: they only pinch a knabbel or a boost and run off giggling.",
              "Guhs 2.9 <b>De Grote Guhspelen</b> is de spelletjesupdate. Zes nieuwe minigames, elk in een eigen groot, lief guhgebouw met een eigen "
              "guh, een eigen munt, een eigen pakje en eigen zwevende top 3-borden: <b>sjoelen</b> bij Opoe Njegschuif, een <b>heggendoolhof</b> dat "
              "elke keer anders is, een <b>katapult</b> tegen twaalf scheve Mika-forten, <b>De Knabbelspelen</b> (een zeskamp in een circustent), de "
              "<b>Elf-Guhjestocht</b>, een schaatstocht langs elf dorpjes over een bevroren kanaal in de gloednieuwe, ijskoude <b>Guhpolder</b>, en het "
              "<b>Guh-Circuit</b> met drie wilde racebanen. De zeven klassiekers van 2.4 kregen <b>makkelijk, medium en lastig</b>, de Guhdisco kreeg "
              "echte liedjes, kleertjes werden <b>ontgrendelingen</b> met een gloednieuwe kledingkast (en een vakje voor de <b>oren</b>), de Guhdex en "
              "het superkompas kregen icoontjestabbladen, en in het Knuffeldal-stadje help je nu de <b>brandweer</b>, de <b>politie</b> en de "
              "<b>apotheek</b>. Zoals altijd: een guh is nooit te vads. De Mika's mogen meedoen als tegenstanders, maar ze doen nooit kwaad: ze pikken "
              "alleen een knabbel of een boost en rennen giechelend weg.")
    parts = ul([("<b>Six new games</b>: Sjoelhuisje, Guhdoolhof, Knabbelkatapult, Knabbelspelen, Elf-Guhjestocht (in the Guhpolder, with the <b>Pinguh</b>) and Guh-Circuit.",
                 "<b>Zes nieuwe spellen</b>: Sjoelhuisje, Guhdoolhof, Knabbelkatapult, Knabbelspelen, Elf-Guhjestocht (in de Guhpolder, met de <b>Pinguh</b>) en Guh-Circuit."),
                ("<b>The classics</b>: makkelijk, medium and lastig for beauty, race, meppen, golf, smul and vissen; four songs in the disco.",
                 "<b>De klassiekers</b>: makkelijk, medium en lastig bij beauty, race, meppen, golf, smul en vissen; vier liedjes in de disco."),
                ("<b>Clothes</b>: eat a piece once to unlock it for all your guhs, a new wardrobe with a 3D preview, the ear slot <b>Oren</b>, and one clear source per piece.",
                 "<b>Kleding</b>: eet een stuk één keer op en al je guhs kunnen het aan, een nieuwe kledingkast met 3D-voorbeeld, het oorvakje <b>Oren</b>, en één duidelijke plek per stuk."),
                ("<b>Guhdex and superkompas</b> with icon tabs: Guhs, Knus, Minigames and Kleding.",
                 "<b>Guhdex en superkompas</b> met icoontjestabbladen: Guhs, Knus, Minigames en Kleding."),
                ("<b>Beroepen</b>: four one-time jobs (brandweer, politie, apotheek, bouw) with their work clothes as a reward.",
                 "<b>Beroepen</b>: vier klusjes van één keer (brandweer, politie, apotheek, bouw) met hun werkkleding als beloning."),
                ("<b>Also new</b>: menus for the pieppiepmuisje and the turtles, sleeping guhs close their eyes, more Zeemeerguhs, and a new advancement tab.",
                 "<b>Ook nieuw</b>: menuutjes voor het pieppiepmuisje en de schildpadjes, slapende guhs doen hun oogjes dicht, meer Zeemeerguhs, en een nieuw vooruitgangentabblad.")])
    npcs = wentry(img("grote_guhspelen_npcs", "The new game guhs"), "Seven new game guhs", "Zeven nieuwe spelletjesguhs",
                  p("From left to right: <b>Opoe Njegschuif</b> (sjoelen, golden glasses and a knitted shawl), <b>Meneer Vadskronkel</b> (the maze, a gardener's "
                    "hat and hedge clippers), <b>Kapitein Floepguh</b> (the catapult, a bicorne with a wiggling feather and a heart eye patch), <b>Juf Vahoegsakee</b> "
                    "(the Knabbelspelen, a sweatband, a whistle and a clipboard), <b>Schaatsmeester Guhglij</b> (the Elf-Guhjestocht), a <b>Stempelguh</b> (one in "
                    "every village of the tour) and <b>Coach Vahoegvroem</b> (the circuit, a chequered cap, a headset and a golden stopwatch). Each has a "
                    "Guhdex page and a lot to say, full of njeg and VAHOEG. Every game building is protected, and nobody gets hurt or hungry while playing.",
                    "Van links naar rechts: <b>Opoe Njegschuif</b> (sjoelen, gouden brilletje en een gebreide omslagdoek), <b>Meneer Vadskronkel</b> (het doolhof, "
                    "een tuinmanshoed en een heggenschaar), <b>Kapitein Floepguh</b> (de katapult, een steek met een wiebelende veer en een hartjesooglapje), "
                    "<b>Juf Vahoegsakee</b> (de Knabbelspelen, een zweetbandje, een fluitje en een klembord), <b>Schaatsmeester Guhglij</b> (de Elf-Guhjestocht), "
                    "een <b>Stempelguh</b> (in elk dorp van de tocht één) en <b>Coach Vahoegvroem</b> (het circuit, een geblokt petje, een headset en een gouden "
                    "stopwatch). Elk heeft een Guhdex-pagina en heel veel te vertellen, vol njeg en VAHOEG. Elk spelgebouw is beschermd, en tijdens het spelen "
                    "krijgt niemand pijn of honger."))

    # ---------------------------------------------------------------- A: sjoelen
    sjoel = h3("A &middot; Het Sjoelhuisje", "A &middot; Het Sjoelhuisje") + \
        wentry(img("structure_sjoelhuisje", "Het Sjoelhuisje"), "The Sjoelhuisje", "Het Sjoelhuisje",
               p("In the <b>Guhweides</b> stands a long brown house with Dutch stepped gables, a big guh face on both ends, round puck windows, and a "
                 "<b>giant sjoelbak as its roof</b> (with the numbers 2 3 4 1 on a tall gate bar and giant pucks lying on it). In the garden: a klinker "
                 "path, flower beds and a giant stack of sjoelschijven with a guh face. Inside is the real sjoelbak (5 wide, 24 long), Opoe's knitting "
                 "corner, and benches full of guh plushies that watch you play.",
                 "In de <b>Guhweides</b> staat een lang bruin huis met trapgevels, een grote guhkop aan beide kanten, ronde schijfjesramen en een "
                 "<b>reuzensjoelbak als dak</b> (met de cijfers 2 3 4 1 op een hoge poortjesbalk en reuzenschijven erop). In de tuin: een klinkerpad, "
                 "bloemperken en een reuzenstapel sjoelschijven met een guhgezicht. Binnen staat de echte sjoelbak (5 breed, 24 lang), het breihoekje "
                 "van Opoe, en bankjes vol guhknuffels die toekijken hoe je speelt."),
               stats=[(("Where", "Waar"), "Guhweides"), (("Guh", "Guh"), "Opoe Njegschuif"), (("Coin", "Munt"), icon("sjoelschijfje", "Sjoelschijfje") + " sjoelschijfje")]) + \
        wentry(shot("npc_sjoelguh_in_gebouw", "Opoe Njegschuif in her Sjoelhuisje"), "Sjoelen with Opoe", "Sjoelen bij Opoe",
               ul([("One turn is <b>20 pucks</b>. Opoe lends them to you (the stack counts down).", "Eén beurt is <b>20 schijfjes</b>. Opoe leent ze je uit (het stapeltje telt af)."),
                   ("<b>Hold right-click</b>: a power bar goes up and down, with Opoe's two marks. Let go to shoot. Look to aim, and walk sideways to shoot from the left or right.",
                    "<b>Houd rechtsklik ingedrukt</b>: een krachtbalk gaat op en neer, met de twee streepjes van Opoe. Laat los om te schuiven. Kijk om te mikken, en loop opzij om van links of rechts te schuiven."),
                   ("The pucks really slide: they bounce off the rims, push each other, stop short or slip through the gates <b>2 - 3 - 4 - 1</b>.",
                    "De schijfjes glijden echt: ze stuiten tegen de randen, duwen elkaar weg, blijven liggen of glippen door de poortjes <b>2 - 3 - 4 - 1</b>."),
                   ("<b>Counting</b>: every complete set (one puck in each gate) is <b>20 points</b>; what's left counts per gate (2, 3, 4 or 1). Five in every gate: <b>100</b>, the maximum.",
                    "<b>Tellen</b>: elk heel setje (één schijfje in elk poortje) is <b>20 punten</b>; wat overblijft telt per poortje (2, 3, 4 of 1). Vijf in elk poortje: <b>100</b>, het maximum."),
                   ("Sjoelschijfjes: 1 per turn, +1 per set, +1 for a new record. One player at a time; the others wait and watch.",
                    "Sjoelschijfjes: 1 per beurt, +1 per setje, +1 voor een nieuw record. Eén speler tegelijk; de anderen wachten en kijken."),
                   ("No levels: sjoelen is sjoelen. The board <i>sjoelen</i> floats above Opoe and above the gates.",
                    "Geen niveaus: sjoelen is sjoelen. Het bord <i>sjoelen</i> zweeft boven Opoe en boven de poortjes.")])) + \
        entry(shot("scherm_sjoelen", "Opoe's screen"), "Opoe's screen", "Het scherm van Opoe",
              p("Play, the rules, your records and the house record, and the shop.", "Spelen, de regels, je records en het huisrecord, en het winkeltje.")) + \
        outfit("sjoelen", "Sjoel outfit", "Sjoelpakje", [("sjoelen_petje", "Sjoelpetje", "4"), ("sjoelen_broche", "Sjoelbroche", "6"),
                                                        ("sjoelen_vestje", "Sjoelvestje", "10")], "sjoelschijfjes", "sjoelschijfjes")

    # ---------------------------------------------------------------- B: doolhof
    doolhof = h3("B &middot; Het Guhdoolhof", "B &middot; Het Guhdoolhof") + \
        wentry(img("structure_guhdoolhof", "Het Guhdoolhof"), "The Guhdoolhof", "Het Guhdoolhof",
               p("In the <b>Guhvelden</b>: a big square hedge field with guh-ear topiary and trimmed guh faces all around, an exit gate with a hedge arch "
                 "in the north, and in the middle a <b>lookout tower shaped like a sitting guh</b> (you stand on its shoulders). A bridge on pillars runs to it. "
                 "At the plaza Meneer Vadskronkel has his kiosk with a pink pluisdak roof. About eighty guh faces in all, and <b>guh-ear lanterns</b> that switch "
                 "on by themselves at night.",
                 "In de <b>Guhvelden</b>: een groot vierkant heggenveld met guhoor-vormsnoei en geknipte guhgezichten rondom, een uitgang met een heggenboog in "
                 "het noorden, en in het midden een <b>uitkijktoren in de vorm van een zittende guh</b> (je staat op zijn schouders). Een brug op pilaren loopt "
                 "ernaartoe. Op het pleintje heeft Meneer Vadskronkel zijn kiosk met een roze pluisdak. Zo'n tachtig guhgezichten, en <b>guhoor-lantaarntjes</b> "
                 "die 's nachts vanzelf aangaan."),
               stats=[(("Where", "Waar"), "Guhvelden"), (("Guh", "Guh"), "Meneer Vadskronkel"), (("Coin", "Munt"), icon("doolhofknabbel", "Doolhofknabbel") + " doolhofknabbel")]) + \
        wentry(shot("gebouw_guhdoolhof_3_close", "The maze from above"), "A new maze every time", "Elke keer een nieuw doolhof",
               p("The Mikas have hidden stolen kaasknabbels in the maze! Pick a level and Meneer Vadskronkel <b>grows a brand-new random maze</b>, row by row. "
                 "Find all the knabbels (the real ones glint) and run out of the north gate: your <b>time</b> is your score. Leave without all of them and you are "
                 "sent back in. <b>Heg-Mikas</b> chase you when they see you; one that touches you giggles, pinches one knabbel back, hides it far away and runs "
                 "off. They never do damage and you can't hurt them. One player at a time; friends watch from the tower.",
                 "De Mika's hebben gestolen kaasknabbels in het doolhof verstopt! Kies een niveau en Meneer Vadskronkel <b>laat een gloednieuw doolhof groeien</b>, "
                 "rij voor rij. Vind alle knabbels (de echte glinsteren) en ren door de noordpoort naar buiten: je <b>tijd</b> is je score. Ga je zonder alles naar "
                 "buiten, dan moet je terug. <b>Heg-Mika's</b> zitten achter je aan als ze je zien; eentje die je aantikt giechelt, pikt één knabbel terug, verstopt "
                 "hem ver weg en rent weg. Ze doen nooit kwaad en jij kunt ze ook geen pijn doen. Eén speler tegelijk; vriendjes kijken mee vanaf de toren.") +
               table([("Level", "Niveau"), ("Maze", "Doolhof"), ("Knabbels", "Knabbels"), ("Heg-Mikas", "Heg-Mika's")], [
                   [("Makkelijk", "Makkelijk"), ("small (11 cells)", "klein (11 vakjes)"), "8", ("1, slow", "1, langzaam")],
                   [("Medium", "Medium"), ("middle (15 cells)", "middel (15 vakjes)"), "12", "2"],
                   [("Lastig", "Lastig"), ("the whole field (19 cells)", "het hele veld (19 vakjes)"), "16", ("3, fast, + 5 fake knabbels in dead ends (+5 s each)", "3, snel, + 5 nepknabbels in doodlopende gangetjes (+5 s per stuk)")]])) + \
        entry(img("doolhof_mika", "Heg-Mika"), "The Heg-Mika", "De Heg-Mika",
              p("A Mika in a hedge suit, with a leaf on its head. It only pinches knabbels: never damage.",
                "Een Mika in een heggenpakje, met een blaadje op zijn kop. Hij pikt alleen knabbels: nooit schade.")) + \
        entry(shot("scherm_doolhofguh_in_gebouw", "Meneer Vadskronkel's screen"), "Meneer Vadskronkel", "Meneer Vadskronkel",
              p("Three level buttons with your best time under each, and the shop (the outfit, hedges and lanterns).",
                "Drie niveauknoppen met je beste tijd eronder, en het winkeltje (het pakje, heggen en lantaarntjes).")) + \
        outfit("doolhof", "Explorer outfit", "Ontdekkingspakje", [("doolhof_hoedje", "Ontdekkingshoedje", "4"), ("doolhof_kompas", "Doolhofkompasje", "6"),
                                                                 ("doolhof_rugzakje", "Ontdekkingsrugzakje", "8")], "doolhofknabbels", "doolhofknabbels")

    # ---------------------------------------------------------------- C: katapult
    katapult = h3("C &middot; De Knabbelkatapult", "C &middot; De Knabbelkatapult") + \
        wentry(img("structure_knabbelkatapult", "De Knabbelkatapult"), "The Knabbelkatapult", "De Knabbelkatapult",
               p("On the <b>Vadskliffen</b>: a cheerful pink guh castle with battlements, four towers with guh ears and a gatehouse with a big guh face. On "
                 "its wide north wall stands the <b>catapult</b> (a kaasknabbel counterweight, a bucket to shoot from). Across a rocky gorge, on a cliff ledge, "
                 "stands a crooked, leaning <b>Mika fort</b> with a big red-eyed Mika face, purple roofs and stolen kaasknabbels.",
                 "Op de <b>Vadskliffen</b>: een vrolijk roze guhkasteel met kantelen, vier torens met guhoren en een poortgebouw met een grote guhkop. Op de "
                 "brede noordmuur staat de <b>katapult</b> (een kaasknabbel als contragewicht, een bakje om uit te schieten). Aan de overkant van een rotskloof, op "
                 "een richel, staat een scheef, hellend <b>Mika-fort</b> met een grote Mika-kop met rode ogen, paarse daken en gestolen kaasknabbels."),
               stats=[(("Where", "Waar"), "Vadskliffen"), (("Guh", "Guh"), "Kapitein Floepguh"), (("Coin", "Munt"), icon("katapultster", "Katapultster") + " katapultster")]) + \
        wentry(img("katapult_forten", "The twelve Mika forts"), "Twelve Mika forts", "Twaalf Mika-forten",
               p("A round is <b>twelve hand-made forts</b>, one after the other, each built up layer by layer on the ledge: the Mikahutje, the Wiebeltoren, "
                 "Twee Torentjes, the IJspaleisje, the Stenen Muur, the Mikapiramide, the Wolkenkrabber, the Mika-kasteeltje, the Hangbrug, the Knabbelpakhuis, "
                 "the Mikamolen and the Grote Mikaburcht. Wood, stone, glass, ice and wool. Stand in the bucket, aim and shoot <b>pluisballen</b>: the ball "
                 "flies in a real arc and knocks blocks out, and whatever loses its support tumbles down (a heavy piece knocks more over). The Mikas never get "
                 "hurt: they run off giggling. Crates with stolen knabbels burst open, and you get those knabbels for real.",
                 "Een ronde is <b>twaalf handgemaakte forten</b> achter elkaar, elk laag voor laag opgebouwd op de richel: het Mikahutje, de Wiebeltoren, Twee "
                 "Torentjes, het IJspaleisje, de Stenen Muur, de Mikapiramide, de Wolkenkrabber, het Mika-kasteeltje, de Hangbrug, het Knabbelpakhuis, de "
                 "Mikamolen en de Grote Mikaburcht. Hout, steen, glas, ijs en wol. Ga in het bakje staan, mik en schiet <b>pluisballen</b>: de bal vliegt in een "
                 "echte boog en slaat blokjes eruit, en wat geen steun meer heeft tuimelt naar beneden (een zwaar stuk gooit nog meer om). De Mika's krijgen "
                 "nooit pijn: ze rennen giechelend weg. Kisten met gestolen knabbels springen open, en die knabbels krijg je echt.") +
               table([("Level", "Niveau"), ("Pluisballen per fort", "Pluisballen per fort"), ("Extra", "Extra")], [
                   [("Makkelijk", "Makkelijk"), "5", ("an aiming line", "een mikstreep")], [("Medium", "Medium"), "4", ""],
                   [("Lastig", "Lastig"), "3", ("wind (arrows in the action bar)", "wind (pijltjes in de actiebalk)")]]) +
               p("<b>Stars</b> per fort: all Mikas gone, all crates open, and a pluisbal left over (36 in a round). <b>Points</b>: a block 10, a Mika 500, a crate 300, a "
                 "pluisbal left over 1000.",
                 "<b>Sterren</b> per fort: alle Mika's weg, alle kisten open, en een pluisbal over (36 in een ronde). <b>Punten</b>: een blokje 10, een Mika 500, een "
                 "kist 300, een pluisbal over 1000.")) + \
        entry(shot("scherm_katapult", "Kapitein Floepguh's screen"), "Kapitein Floepguh", "Kapitein Floepguh",
              p("Pick a level (the chosen one is framed: <b>&raquo; Medium &laquo;</b>) and press <b>Floepen maar!</b>. Katapultsterren: 1 per round, +1 per 9 stars, +1 for a record; lastig +50%.",
                "Kies een niveau (het gekozen niveau staat tussen haakjes: <b>&raquo; Medium &laquo;</b>) en druk op <b>Floepen maar!</b>. Katapultsterren: 1 per ronde, +1 per 9 sterren, +1 voor een record; lastig +50%.")) + \
        outfit("katapult", "Catapult outfit", "Katapultpakje", [("katapult_helmpje", "Katapulthelmpje", "4"), ("katapult_oorbelletjes", "Pluisbal-oorbelletjes", "6"),
                                                               ("katapult_riem", "Katapultriem", "10")], "katapultsterren", "katapultsterren")

    # ---------------------------------------------------------------- D: knabbelspelen
    EVENTS = [("Knabbelhappen", ("Kaasknabbels swing on strings from a striped beam: bite as many as you can (a golden one is 3). Nice and vahoeg!",
                                 "Kaasknabbels zwaaien aan touwtjes aan een gestreepte balk: hap er zoveel je kunt (een gouden telt 3). Lekker vahoeg!"), ("points", "punten")),
              ("Zaklopen", ("In a guh sack: every jump is a hop forward, over fluffy bumps to the finish.",
                            "In een guhzak: elke sprong is een hupje vooruit, over pluizige bulten naar de finish."), ("time", "tijd")),
              ("Mika-blikgooien", ("A pyramid of tins with Mika faces and 8 pluisballen. A tin you hit falls, and so does every tin on top of it. All down: +20 and a new pyramid.",
                                   "Een piramide van blikken met Mika-koppen en 8 pluisballen. Een blik dat je raakt valt, en alles wat erop staat ook. Alles om: +20 en een nieuwe piramide."), ("points", "punten")),
              ("Eierlopen", ("A knabbelei on a spoon through a slalom of 4 flags. Too wild (running, sharp turns, jumping) and the egg drops: back to the last flag.",
                             "Een knabbelei op een lepel door een slalom van 4 vlaggetjes. Te wild (rennen, scherpe bochten, springen) en het ei valt: terug naar het laatste vlaggetje."), ("time", "tijd")),
              ("Spijkerpoepen", ("A big knabbelspijker dangles on a string behind your guh belt and swings with every step. Crouch still above a kaasmelk bottle: plonk! Three bottles.",
                                 "Een grote knabbelspijker bungelt aan een touwtje achter je guhriem en zwaait mee met elke stap. Sta stil gebukt boven een kaasmelkfles: plonk! Drie flessen."), ("time", "tijd")),
              ("Guhguhtje prik", ("Blindfolded (a pink cloth over your eyes) and spun round three times: pin the tail on the big guh board. The closer, the more points (0 to 1000).",
                                  "Geblinddoekt (een roze doek voor je ogen) en drie keer rondgedraaid: prik het staartje op het grote guhbord. Hoe dichterbij, hoe meer punten (0 tot 1000)."), ("closeness", "hoe dichtbij"))]
    spelen = h3("D &middot; De Knabbelspelen", "D &middot; De Knabbelspelen") + \
        wentry(img("structure_knabbelspelen", "De Knabbelspelen"), "The circus tent", "De circustent",
               p("In the <b>Guhweides</b> and the <b>Roze pluisjes</b>: a big pink-and-white striped circus tent whose two tent tops are <b>guh ears</b> (with "
                 "flags), with a guh face on its roof, four entrances, a circus ring with Juf Vahoegsakee in the middle and tiers of seats. Around it lie six "
                 "fenced play fields with four lanes each, an entrance arch and a floating board per event.",
                 "In de <b>Guhweides</b> en de <b>Roze pluisjes</b>: een grote roze-wit gestreepte circustent waarvan de twee tentpunten <b>guhoren</b> zijn (met "
                 "vlaggetjes), met een guhgezicht op het dak, vier ingangen, een piste met Juf Vahoegsakee in het midden en tribunes. Eromheen liggen zes "
                 "omheinde speelvelden met elk vier baantjes, een ingangsboog en een zwevend bord per onderdeel."),
               stats=[(("Where", "Waar"), "Guhweides, Roze pluisjes"), (("Guh", "Guh"), "Juf Vahoegsakee"), (("Coin", "Munt"), icon("spelenlintje", "Spelenlintje") + " spelenlintje")]) + \
        wentry(shot("gebouw_knabbelspelen_3_close", "De Knabbelspelen"), "Six events", "Zes onderdelen",
               table([("Event", "Onderdeel"), ("What you do", "Wat je doet"), ("Score", "Score")], [[f"<b>{n}</b>", d, s] for n, d, s in EVENTS]) +
               p("Play one event on its own, or all six in a row as the <b>Grote Zeskamp</b>: every event gives 0 to 1000 zeskamp points (for the timed ones: the "
                 "faster, the more), so 6000 is the maximum; the score table is in Juf Vahoegsakee's screen. <b>Friends</b> may join in (up to four, each in their own "
                 "lane): whoever opens the round waits 15 seconds for them. Spelenlintjes: 1 to 3 for an event, 2 plus one per 1000 points for the Zeskamp, always +1, "
                 "and +1 more for winning against your friends.",
                 "Speel één onderdeel los, of alle zes achter elkaar als de <b>Grote Zeskamp</b>: elk onderdeel geeft 0 tot 1000 zeskamppunten (bij de tijden: hoe "
                 "sneller, hoe meer), dus 6000 is het maximum; de scoretabel staat in het scherm van Juf Vahoegsakee. <b>Vriendjes</b> mogen meedoen (tot vier, "
                 "ieder in een eigen baantje): wie de ronde opent wacht 15 seconden op ze. Spelenlintjes: 1 tot 3 voor een onderdeel, 2 plus één per 1000 punten "
                 "voor de Zeskamp, altijd +1, en nog +1 als je van je vriendjes wint.")) + \
        entry(shot("scherm_spelleiderguh_in_gebouw", "Juf Vahoegsakee's screen"), "Juf Vahoegsakee", "Juf Vahoegsakee",
              p("<i>\"Hooggeëerd publiek!\"</i> Six event buttons, De Grote Zeskamp, the score table and the shop (the outfit, tins and bottles).",
                "<i>\"Hooggeëerd publiek!\"</i> Zes knoppen voor de onderdelen, De Grote Zeskamp, de scoretabel en het winkeltje (het pakje, blikken en flessen).")) + \
        outfit("knabbelspelen", "Sports outfit", "Sportpakje", [("spelen_zweetbandje", "Sportzweetbandje", "4"), ("spelen_fluitje", "Scheidsrechtersfluitje", "6"),
                                                               ("spelen_sportshirtje", "Knabbelspelen-sportshirtje", "10")], "spelenlintjes", "spelenlintjes")

    # ---------------------------------------------------------------- the Guhpolder and the Pinguh
    polder = h3("De Guhpolder and the Pinguh", "De Guhpolder en de Pinguh") + \
        wentry(shot("biome_guhpolder", "De Guhpolder"), "De Guhpolder", "De Guhpolder",
               p("A new, rare, ice-cold and <b>very flat</b> biome in the Guhmension (<code>guhs:guhpolder</code>, about 2.5% of the land): frosty <b>rijpgras</b> "
                 "with glitter, <b>knotwilgen</b> with snow caps along frozen ditches, little frozen ponds, <b>snowman-guh hills</b> with a guh face, glowing "
                 "<b>ijspegelguh-kristallen</b> shaped like a guh ear, blue <b>guh-ijsbloempjes</b> with a tiny face (pick one for light blue dye), and here and there "
                 "a little <b>guh-molentje</b>. It snows a lot. Only guhs live here: no Mikas. In the middle of most polders lies the Elf-Guhjestocht.",
                 "Een nieuw, zeldzaam, ijskoud en <b>heel plat</b> bioom in de Guhmensie (<code>guhs:guhpolder</code>, zo'n 2,5% van het land): berijpt <b>rijpgras</b> "
                 "met glitters, <b>knotwilgen</b> met sneeuwmutsjes langs bevroren slootjes, kleine bevroren vennetjes, <b>sneeuwguh-heuveltjes</b> met een guhgezicht, "
                 "gloeiende <b>ijspegelguh-kristallen</b> in de vorm van een guhoor, blauwe <b>guh-ijsbloempjes</b> met een piepklein gezichtje (pluk er een voor "
                 "lichtblauwe kleurstof), en hier en daar een <b>guh-molentje</b>. Het sneeuwt er veel. Hier wonen alleen guhs: geen Mika's. Midden in de meeste "
                 "polders ligt de Elf-Guhjestocht.")) + \
        entry(img("guhpolder_blokken", "Guhpolder blocks"), "Polder blocks", "Polderblokken",
              p("Rijpgras, <b>polderijs</b> (never melts, no snow on it; 2 packed ice + 2 snowballs make 2), knotwilgstam (4 spruce planks) and knotwilgtakjes, "
                "the ijspegelguh-kristal (gives light), the guh-ijsbloempje, rijpsprietjes and the guh-molentje.",
                "Rijpgras, <b>polderijs</b> (smelt nooit, er blijft geen sneeuw op liggen; 2 pakijs + 2 sneeuwballen worden er 2), knotwilgstam (4 sparrenplanken) "
                "en knotwilgtakjes, het ijspegelguh-kristal (geeft licht), het guh-ijsbloempje, rijpsprietjes en het guh-molentje.")) + \
        entry(img("guh_molentje", "Guh-molentje"), "The guh-molentje and knabbelmeel", "Het guh-molentje en knabbelmeel",
              p("A little windmill with a guh face and turning sails. Put <b>knabbelgraan</b> in it (the grain of the Knuffeldal gardens) and it grinds "
                "<b>knabbelmeel</b>; in snow and storms it turns faster. Hoppers work too. Bake with knabbelmeel instead of knabbelgraan in the Knabbeloven of "
                "the bakery and you get <b>twice as much</b>. Craft one from 2 white wool, 2 sticks, a kaasknabbel and 3 planks.",
                "Een molentje met een guhgezicht en draaiende wieken. Stop er <b>knabbelgraan</b> in (het graan uit de tuintjes van het Knuffeldal) en het maalt "
                "<b>knabbelmeel</b>; bij sneeuw en storm draait het sneller. Trechters werken ook. Bak je met knabbelmeel in plaats van knabbelgraan in de "
                "Knabbeloven van de bakkerij, dan krijg je <b>twee keer zoveel</b>. Maak er een van 2 witte wol, 2 stokjes, een kaasknabbel en 3 planken.") +
              icons("knabbelmeel", "guh_molentje", "guh_ijsbloempje", "polderijs")) + \
        '<div class="recipes">' + recipe_card("guh_molentje", "Guh-molentje", "Guh-molentje") + recipe_card("polderijs", "Polderijs (&times;2)", "Polderijs (&times;2)") + \
        recipe_card("guh_ijsbloempje_kleurstof", "Light blue dye", "Lichtblauwe kleurstof") + recipe_card("knotwilg_planken", "Spruce planks (&times;4)", "Sparrenplanken (&times;4)") + '</div>' + \
        wentry(img("pinguhs", "Pinguhs: klassiek, keizer and a chick"), "The Pinguh", "De Pinguh",
               p("A guh in a penguin suit! About <b>half of the wild guhs</b> in the Guhpolder are a Pinguh (a new variant after the Pluisguh). There are "
                 "<b>klassieke</b> Pinguhs (a black back and head, a white tummy and face around the big guh eyes, an orange beak with pink blush, flippers and "
                 "orange feet), stately <b>Keizerpinguhs</b> (a bit bigger once grown, with golden cheek patches beside the eyes, a thin golden brow and golden patches by the ears, so you know them from the front too) and now and then a grey, fluffy "
                 "<b>Pinguh chick</b>: every baby looks like that. It waddles vadsig about and <b>slides on its tummy</b> over the ice. Tame one with "
                 "kaasknabbels for its Guhdex star; it wears clothes like any guh, and during the Elf-Guhjestocht it slides along behind you. There is a "
                 "<b>Pinguhknuffel</b> in the grijpmachine too (now 22 plushies).",
                 "Een guh in een pinguinpakje! Ongeveer <b>de helft van de wilde guhs</b> in de Guhpolder is een Pinguh (een nieuwe variant na de Pluisguh). Er zijn "
                 "<b>klassieke</b> Pinguhs (zwarte rug en kop, een wit buikje en een wit gezicht rond de grote guhogen, een oranje snaveltje met roze blosjes, "
                 "flippers en oranje pootjes), deftige <b>Keizerpinguhs</b> (als ze groot zijn een beetje groter, met gouden wangvlekjes naast de ogen, een dun gouden wenkbrauwtje en gouden vlekjes bij de oren, dus je herkent ze ook van voren) en heel soms een "
                 "grijs pluizig <b>Pinguh-kuikentje</b>: alle baby's zien er zo uit. Hij waggelt vadsig rond en <b>glijdt op zijn buik</b> over het ijs. Tem er een "
                 "met kaasknabbels voor zijn Guhdex-ster; hij draagt kleertjes zoals elke guh, en tijdens de Elf-Guhjestocht glijdt hij gezellig achter je aan. "
                 "In de grijpmachine zit ook een <b>Pinguhknuffel</b> (nu 22 knuffels)."),
               stats=[(("Where", "Waar"), "Guhpolder"), (("Chance", "Kans"), t("1 in 2 wild guhs there", "1 op 2 wilde guhs daar")),
                      (("Tame", "Temmen"), icon("kaas_knabbels", "Kaas Knabbels") + " kaasknabbels")]) + \
        entry(shot("pinguh_klassiek_keizer", "Klassiek and keizer"), "Klassiek, keizer and pluis", "Klassiek, keizer en pluis",
              p("In the game: a klassieke Pinguh, a Keizerpinguh and a Pinguh chick lying on the snow.",
                "In het spel: een klassieke Pinguh, een Keizerpinguh en een Pinguh-kuikentje op de sneeuw.")) + \
        entry(shot("pinguh_kleding", "A Pinguh with a hat"), "Dressed up", "Aangekleed",
              p("A Pinguh with a sjoelpetje. Every piece of clothing fits a Pinguh too.", "Een Pinguh met een sjoelpetje. Alle kleertjes passen ook een Pinguh."))

    # ---------------------------------------------------------------- E: the Elf-Guhjestocht
    DORPEN = [("guhwarden", "Guhwarden", ("start and finish", "start en finish"),
               ("De Bonkevads with koek-en-zopie and erwtensoep, a grandstand, the start arch with a guh face, and Schaatsmeester Guhglij.",
                "De Bonkevads met koek-en-zopie en erwtensoep, een tribune, de startboog met een guhkop, en Schaatsmeester Guhglij.")),
              ("snuh", "Snuh", ("village 2", "dorp 2"), ("Grachtenpandjes with guh gables and a drawbridge you skate under.", "Grachtenpandjes met guhgevels en een ophaalbrug waar je onderdoor schaatst.")),
              ("ijlguh", "IJlguh", ("village 3", "dorp 3"), ("A giant mug as a kiosk: warme chocovet, a vuurkorf and marshmallow knabbels.", "Een reuzenmok als kiosk: warme chocovet, een vuurkorf en marshmallowknabbels.")),
              ("knabbelsloten", "Knabbelsloten", ("village 4", "dorp 4"), ("A little lock and the guh band De Vadse Toeters.", "Een sluisje en het guhbandje De Vadse Toeters.")),
              ("vadsvoren", "Vadsvoren", ("village 5", "dorp 5"), ("A harbour with a frozen-in botter, an ijszeiler and a lighthouse.", "Een haventje met een ingevroren botter, een ijszeiler en een vuurtoren.")),
              ("guhdeloopen", "Guhdeloopen", ("village 6", "dorp 6"), ("A raadhuis, a poffertjes stall and flag-waving guhs.", "Een raadhuis, een poffertjeskraam en vlaggenzwaaiende guhs.")),
              ("vadskum", "Vadskum", ("village 7", "dorp 7"), ("The klunplek: a stretch over land across a klunbrugje, and ice-hockey guhtjes.", "De klunplek: een stukje over land via een klunbrugje, en ijshockeyende guhtjes.")),
              ("knabbelsward", "Knabbelsward", ("village 8", "dorp 8"), ("A church on a terp with a guh-faced tower, and a snert tent.", "Een kerkje op een terp met een guhkop-toren, en een snerttent.")),
              ("guhlingen", "Guhlingen", ("village 9", "dorp 9"), ("The big guh windmill De Vahoege Wiek and an oliebollen stall.", "De grote guhmolen De Vahoege Wiek en een oliebollenkraam.")),
              ("franeguh", "Franeguh", ("village 10", "dorp 10"), ("A little star dome, Het Guhsterrenhuis, with lampions.", "Een sterrenkoepeltje, Het Guhsterrenhuis, met lampionnen.")),
              ("dokguh", "Dokguh", ("village 11", "dorp 11"), ("Sneeuwbert the snowman-guh, sleds, a lampion arch and a Pinguh colony.", "Sneeuwbert de sneeuwpopguh, sleetjes, een lampionnenboog en een Pinguhkolonie."))]
    dorpen = '<div class="gallery">' + "".join(
        gfig(f"shot29_elftocht_dorp{i:02d}_{sid}", name, name, sub[0], sub[1], d[0], d[1]) for i, (sid, name, sub, d) in enumerate(DORPEN, 1)) + '</div>'
    elftocht = h3("E &middot; De Elf-Guhjestocht", "E &middot; De Elf-Guhjestocht") + \
        wentry(img("structure_elfguhjestocht", "De Elf-Guhjestocht from above"), "A skating tour past eleven villages", "Een schaatstocht langs elf dorpjes",
               p("<i>Rebuilt in 2.10 with a winding canal, evenly spread villages, ditches and snowy hills: see <a href='#fixes210'>Fixes in 2.10</a>.</i> "
                 "The showpiece of 2.9, and the biggest guh building of all (256 by 256 blocks): a <b>frozen canal</b> of almost 1300 blocks that winds "
                 "through the Guhpolder past <b>eleven little villages</b>, each different, full of food and drink, Dutch buildings, winter fun and cheering "
                 "audience guhs (164 of them). Start and finish are in <b>Guhwarden</b>, with a chequered start line, a start arch and blue arrows on the ice. "
                 "Along the way: bridges, place-name boards before every village, koek-en-zopie stalls, hooibergen with a guh face, skating ponds, snowman-guhs, "
                 "flags and straw bales. Find it with the superkompas (Minigames); there is one in about two of every three polders.",
                 "<i>In 2.10 opnieuw gebouwd, met een kronkelend kanaal, mooi verspreide dorpjes, slootjes en sneeuwheuveltjes: zie <a href='#fixes210'>Fixes in 2.10</a>.</i> "
                 "Het pronkstuk van 2.9, en het grootste guhbouwwerk van allemaal (256 bij 256 blokken): een <b>bevroren kanaal</b> van bijna 1300 blokken dat door "
                 "de Guhpolder slingert langs <b>elf dorpjes</b>, allemaal anders, vol eten en drinken, Hollandse gebouwtjes, winterpret en juichend guhpubliek "
                 "(164 guhs). Start en finish zijn in <b>Guhwarden</b>, met een geblokte startstreep, een startboog en blauwe pijlen op het ijs. Onderweg: bruggetjes, "
                 "plaatsnaamborden voor elk dorp, koek-en-zopiekraampjes, hooibergen met een guhgezicht, ijsbaantjes, sneeuwpopguhs, vlaggetjes en strobalen. "
                 "Vind hem met het superkompas (Minigames); ongeveer twee op de drie polders hebben er een."),
               stats=[(("Where", "Waar"), "Guhpolder"), (("Guh", "Guh"), "Schaatsmeester Guhglij"), (("Coin", "Munt"), icon("elfstempel", "Elfstempel") + " elfstempel")]) + \
        wentry(shot("npc_schaatsmeester_template", "Schaatsmeester Guhglij at the start"), "It giet oan!", "It giet oan!",
               ul([("Talk to <b>Schaatsmeester Guhglij</b> in Guhwarden: he lends you <b>guh-schaatsen</b> (held in your hand, like the golf club: +50% speed on ice, "
                    "you lean and sway, the blades hiss) and a <b>stempelkaart</b>. 3 - 2 - 1, the whistle, and off you go.",
                    "Praat met <b>Schaatsmeester Guhglij</b> in Guhwarden: hij leent je <b>guh-schaatsen</b> (in je hand, zoals de golfclub: +50% snelheid op ijs, "
                    "je leunt en zwiert, de ijzers sissen) en een <b>stempelkaart</b>. 3 - 2 - 1, het fluitje, en gaan."),
                   ("In every village a <b>Stempelguh</b> (with his own hat or scarf) holds a stamp: right-click him, <i>plof</i>, a stamp! The order is fixed: from "
                    "Snuh to Dokguh, and Guhwarden last (that's the finish). A wrong order doesn't count. The whole village cheers at every stamp.",
                    "In elk dorp heeft een <b>Stempelguh</b> (met een eigen hoedje of sjaal) een stempel in zijn hand: rechtsklik hem, <i>plof</i>, een stempel! De volgorde "
                    "ligt vast: van Snuh tot Dokguh, en Guhwarden als laatste (dat is de finish). Een verkeerde volgorde telt niet. Bij elke stempel juicht het hele dorp."),
                   ("Your split time per village shows green or red against your best. At the finish: your time on the board <i>elfguhjestocht</i>, fireworks "
                    "(only sparkles, never real rockets) and everyone cheers <i>VAHOEG!</i>",
                    "Je tussentijd per dorp is groen of rood tegen je beste. Bij de finish: je tijd op het bord <i>elfguhjestocht</i>, vuurwerk (alleen fonkels, nooit echte "
                    "vuurpijlen) en iedereen roept <i>VAHOEG!</i>"),
                   ("At the stalls you can grab a cup of <b>warme chocovet</b> or <b>snert</b> for a short speed boost. At night lampions and vuurkorven light up along the canal.",
                    "Bij de kraampjes pak je een kopje <b>warme chocovet</b> of <b>snert</b> voor een korte zet. 's Nachts gaan lampionnen en vuurkorven aan langs het kanaal."),
                   ("Friends can skate at the same time, each with their own time. A tamed <b>Pinguh</b> slides along behind you. Just want to skate? <b>Vrij schaatsen</b>: no clock, no coins.",
                    "Vriendjes kunnen tegelijk schaatsen, ieder met een eigen tijd. Een tamme <b>Pinguh</b> glijdt achter je aan. Gewoon lekker schaatsen? <b>Vrij schaatsen</b>: geen klok, geen munten.")])) + \
        dorpen + \
        entry(shot("npc_stempelguh_dorp", "A Stempelguh"), "The Stempelguhs", "De Stempelguhs",
              p("Every village has its own Stempelguh, <i>\"Stempelguh van ...\"</i>. Plof!", "Elk dorp heeft een eigen Stempelguh, <i>\"Stempelguh van ...\"</i>. Plof!")) + \
        entry(shot("scherm_schaatsmeester", "Schaatsmeester Guhglij's screen"), "Rewards", "Beloningen",
              table([("Time", "Tijd"), ("Elfstempels", "Elfstempels")], [[("any finish", "elke finish"), "12"], [("under 6:00", "onder 6:00"), "+2"], [("under 5:00", "onder 5:00"), "+2"],
                                                                          [("under 4:15", "onder 4:15"), "+2"], [("under 3:40", "onder 3:40"), "+2 &#9733;"],
                                                                          [("the first finish", "de eerste finish"), "+5"]]) +
              p("<i>Since 2.10.1 (it used to be 5, +1 per mark): a tour is a long ride, so it pays more. See <a href='#fixes2101'>2.10.1</a>.</i>",
                "<i>Sinds 2.10.1 (vroeger 5, +1 per grens): een tocht is een lange rit, dus hij betaalt meer. Zie <a href='#fixes2101'>2.10.1</a>.</i>") +
              p("The first time you finish you also get the <b>Elf-Guhjeskruisje</b>, a little medal block for at home, and 5 extra elfstempels. A good tour takes about 3&frac12; to 5 minutes. "
                "No levels here.",
                "De eerste keer dat je hem uitrijdt krijg je ook het <b>Elf-Guhjeskruisje</b>, een medailleblokje voor thuis, en 5 elfstempels extra. Een goede tocht duurt zo'n 3&frac12; tot 5 "
                "minuten. Hier zijn geen niveaus.") + icons("guh_schaatsen", "stempelkaart", "warme_chocovet", "snert_kommetje", "elf_guhjeskruisje")) + \
        entry(img("elftocht_blokken", "Elftocht blocks"), "Lights, stalls and the kruisje", "Lichtjes, kraampjes en het kruisje",
              p("The night lampion and the vuurkorf (both light up at night, and are harmless), trays of warm cups (chocovet and snert) and the Elf-Guhjeskruisje.",
                "De nachtlampion en de vuurkorf (allebei aan in het donker, en ongevaarlijk), dienbladen met warme kopjes (chocovet en snert) en het Elf-Guhjeskruisje.")) + \
        outfit("elftocht", "Skating outfit", "Schaatspakje", [("elftocht_schaatsmuts", "Schaatsmuts met pompom", "4"), ("elftocht_sjaal", "Oranje schaatssjaal", "6"),
                                                            ("elftocht_oorwarmers", "Pluizige oorwarmers", "6"), ("elftocht_truitje", "Wollen schaatstruitje", "10")],
               "elfstempels", "elfstempels")

    # ---------------------------------------------------------------- F: the circuit
    circuit = h3("F &middot; Het Guh-Circuit", "F &middot; Het Guh-Circuit") + \
        wentry(img("structure_guh_circuit", "Het Guh-Circuit"), "Het Guh-Circuit", "Het Guh-Circuit",
               p("A rare, huge race park (192 by 192 blocks) in the <b>Guhvelden</b> and on the <b>Kaasvlakte</b>, next to the old guhracebaan (which stays). In the "
                 "middle: the <b>Pitpaleis</b> with guh faces on both gables (their mouths are the doors) and a giant guh head on the roof, two grandstands full of "
                 "zitzakken, a boulevard with flags and snack stands, and <b>Coach Vahoegvroem</b>. Around it lie three new tracks, all three laps, with lamp posts "
                 "along every road.",
                 "Een zeldzaam, reusachtig racepark (192 bij 192 blokken) in de <b>Guhvelden</b> en op de <b>Kaasvlakte</b>, naast de oude guhracebaan (die blijft). In het "
                 "midden: het <b>Pitpaleis</b> met guhkoppen op beide gevels (hun monden zijn de deuren) en een reuzenguhkop op het dak, twee tribunes vol zitzakken, "
                 "een boulevard met vlaggetjes en snackkraampjes, en <b>Coach Vahoegvroem</b>. Eromheen liggen drie nieuwe banen, allemaal drie rondes, met "
                 "lantaarnpalen langs elke weg."),
               stats=[(("Where", "Waar"), "Guhvelden, Kaasvlakte"), (("Guh", "Guh"), "Coach Vahoegvroem"), (("Coin", "Munt"), icon("circuitbeker", "Circuitbeker") + " circuitbeker")]) + \
        wentry(shot("gebouw_guh_circuit_3_close", "The circuit"), "Three tracks", "Drie banen",
               table([("Track", "Baan"), ("What's special", "Wat er bijzonder is")], [
                   ["<b>Regenboogbaan</b>", ("A floating road in 7 glowing colours on cloud pillars, climbing to 14 blocks high: a chicane, two jumps over gaps and 4 rainbow "
                                             "<b>VAHOEG rings</b> for a boost. A rainbow trail follows your guh. At a gap your guh makes a <b>regenboogsprong</b> "
                                             "all by itself and <b>glides</b> across in a cloud of rainbow sparkles (no jump key needed, at any speed and level).",
                                             "Een zwevende weg in 7 gloeiende kleuren op wolkenpilaren, tot 14 blokken hoog: een chicane, twee sprongen over gaten en 4 "
                                             "regenboog-<b>VAHOEG-ringen</b> voor een zet. Achter je guh komt een regenboogspoor. Bij een gat maakt je guh vanzelf een "
                                             "<b>regenboogsprong</b> en <b>zweeft</b> hij eroverheen in een wolk regenboogsterretjes (geen springknop nodig, op elke snelheid en elk niveau).")],
                   ["<b>Vadsbaan</b>", ("A cheese road with slippery <b>kaassaus</b> hairpins, bouncy <b>stuiterpaddenstoelen</b> and the <b>Vadslooping</b>: a corkscrew ring with "
                                        "guh ears. Run into it and your guh loops the loop, ending with a VAHOEG boost. Wieeee!",
                                        "Een kaasweg met gladde <b>kaassaus</b>-haarspeldbochten, <b>stuiterpaddenstoelen</b> en de <b>Vadslooping</b>: een kurkentrekker-ring "
                                        "met guhoren. Ren erin en je guh gaat over de kop, met aan het eind een VAHOEG-zet. Wieeee!")],
                   ["<b>Kaasbergbaan</b>", ("Up the Kaasberg, a cheese mountain with a snowy top and a big guh face: under the <b>Mikapoort</b> Mikas roll kaasknabbels down "
                                            "the slope (they bump your guh), then over the icy summit and down the other side.",
                                            "De Kaasberg op, een kaasberg met een besneeuwde top en een grote guhkop: onder de <b>Mikapoort</b> rollen Mika's kaasknabbels "
                                            "naar beneden (die botsen tegen je guh), dan over de ijzige top en aan de andere kant weer naar beneden.")]]) +
               p("On every track stand <b>Mika-pikkers</b>: Mikas in a chequered bandana that, if you pass too close, pinch your VAHOEG boost, giggle and pop back. "
                 "Never damage. Your best race drives along as a <b>ghost</b>, and if you like, the server record holder's race as a <b>golden ghost</b>.",
                 "Op elke baan staan <b>Mika-pikkers</b>: Mika's met een geblokte bandana die, als je te dichtbij komt, je VAHOEG-zet inpikken, giechelen en weer "
                 "terugspringen. Nooit schade. Je beste race rijdt mee als <b>geest</b>, en als je wilt ook de race van de recordhouder van de server als <b>gouden geest</b>.")) + \
        entry(img("circuit_mikapikker", "Mika-pikker"), "Levels", "Niveaus",
              table([("", ""), ("Makkelijk", "Makkelijk"), ("Medium", "Medium"), ("Lastig", "Lastig")], [
                  [("Race guh", "Renguh"), ("calm", "rustig"), ("as in 2.4", "zoals in 2.4"), ("faster, but turns slowly", "sneller, maar draait langzaam")],
                  [("Mika-pikkers", "Mika-pikkers"), "2", "4", "6"],
                  [("Rolling knabbels", "Rollende knabbels"), ("few", "weinig"), ("more", "meer"), ("lots", "veel")],
                  [("Off the road", "Van de weg"), ("wide rails: back to where you were", "brede vangrails: terug naar waar je was"), ("back to the last ring", "terug naar de laatste ring"), ("back to the last ring", "terug naar de laatste ring")],
                  [("VAHOEG pads", "VAHOEG-pads"), ("gold and silver", "goud en zilver"), ("gold and silver", "goud en zilver"), ("only gold", "alleen goud")]]) +
              p("The old guhracebaan got the same three levels. Lastig pays 50% more.", "De oude guhracebaan kreeg dezelfde drie niveaus. Lastig betaalt 50% meer.")) + \
        entry(shot("scherm_circuitguh_in_gebouw", "Coach Vahoegvroem's screen"), "Coach Vahoegvroem", "Coach Vahoegvroem",
              p("Pick a track and a level, see your records, the track record, the medal times and the coins, switch the ghosts on or off, and open the shop. "
                "Every track has boards for the whole race and the fastest lap, per level (18 in all), and six of them float at the grandstands.",
                "Kies een baan en een niveau, zie je records, het baanrecord, de medailletijden en de munten, zet de geesten aan of uit, en open het winkeltje. "
                "Elke baan heeft borden voor de hele race en de snelste ronde, per niveau (18 in totaal), en zes daarvan zweven bij de tribunes.")) + \
        entry(img("circuit_blokken", "Circuit blocks"), "Track blocks", "Baanblokken",
              p("Regenboogweg (7 colours), kaasweg, glibberige kaassaus, kaasbergijs, the stuiterpaddenstoel and the regenboog-boostring.",
                "Regenboogweg (7 kleuren), kaasweg, glibberige kaassaus, kaasbergijs, de stuiterpaddenstoel en de regenboog-boostring.")) + \
        outfit("circuit", "Racing outfit", "Racepakje", [("circuit_helmpje", "Circuithelmpje", "5"), ("circuit_vlagcape", "Geblokte vlagcape", "7"),
                                                        ("circuit_racepak", "Vahoeg-racepak", "10")], "circuitbekers", "circuitbekers")

    # ---------------------------------------------------------------- G: the classics and the disco
    KLAS = [("Guh Beauty", ("60 s to dress, a mild jury (+1 from every judge)", "60 s aankleden, een milde jury (+1 van elk jurylid)"), ("45 s, as before", "45 s, zoals altijd"),
             ("30 s and a strict jury: off-theme pieces cost double", "30 s en een strenge jury: stukken die niet bij het thema passen kosten dubbel")),
            ("Mika meppen", ("slower, fewer guh decoys", "langzamer, minder guh-lokkertjes"), ("as before", "zoals altijd"), ("faster, more decoys, gold Mikas rarer", "sneller, meer lokkertjes, gouden Mika's zeldzamer")),
            ("Guhgolf", ("10 strokes a hole, no penalty stroke", "10 slagen per hole, geen strafslag"), ("8 strokes, penalty strokes", "8 slagen, strafslagen"),
             ("6 strokes; since 2.10 its own tees further back, par 36, a windmill twice as fast and extra slime bumpers (the wind is gone)",
              "6 slagen; sinds 2.10 eigen afslagen verder weg, par 36, een molen die twee keer zo snel draait en extra slijmbumpers (de wind is weg)")),
            ("Vadsig eetfestijn", ("less Mika-vet, falls slower", "minder Mika-vet, valt langzamer"), ("as before", "zoals altijd"), ("more Mika-vet, falls 1.3&times; faster", "meer Mika-vet, valt 1,3&times; sneller")),
            ("Guhvissen", ("a long bite window", "lang de tijd om te slaan"), ("as before", "zoals altijd"), ("a short bite window, and fish can wriggle off the hook", "kort de tijd, en vissen kunnen van je haakje spartelen")),
            ("Guhrace", ("a calm race guh, wide rails", "een rustige renguh, brede vangrails"), ("as before", "zoals altijd"), ("a fast, stubborn race guh, Mika-pikkers", "een snelle, eigenwijze renguh, Mika-pikkers"))]
    klassiek = h3("G &middot; Makkelijk, medium or lastig", "G &middot; Makkelijk, medium of lastig") + \
        p("Every classic minigame of 2.4 now has three levels, with three buttons on its guh's screen. <b>Medium is the game as you knew it</b> (your old "
          "records count as medium). Every level has its own records and boards, the floating boards show all three, and <b>lastig pays 50% more</b>. "
          "(The Verstopguhhuis already had levels of its own.)",
          "Elke klassieke minigame van 2.4 heeft nu drie niveaus, met drie knoppen in het scherm van zijn guh. <b>Medium is het spel zoals je het kende</b> "
          "(je oude records tellen als medium). Elk niveau heeft eigen records en borden, de zwevende borden laten ze alle drie zien, en <b>lastig betaalt 50% "
          "meer</b>. (Het Verstopguhhuis had al eigen niveaus.)") + \
        table([("Game", "Spel"), ("Makkelijk", "Makkelijk"), ("Medium", "Medium"), ("Lastig", "Lastig")], [[f"<b>{n}</b>", a, b, c] for n, a, b, c in KLAS]) + \
        '<div class="gallery">' + "".join(gfig(f"shot29_scherm_{n}", en, nl, "", "", den, dnl) for n, en, nl, den, dnl in [
            ("beauty", "The Showguh", "De Showguh", "Three level buttons above <i>Start de show!</i>", "Drie niveauknoppen boven <i>Start de show!</i>"),
            ("meppen", "The Mepguh", "De Mepguh", "Your record per level and the world record.", "Je record per niveau en het wereldrecord."),
            ("golf", "The Golfguh", "De Golfguh", "Par 27 on 9 holes; lastig blows.", "Par 27 op 9 holes; op lastig waait het."),
            ("smul", "The Smulguh", "De Smulguh", "One minute of food falling from the sky.", "Een minuut lang valt er eten uit de lucht."),
            ("vissen", "The Visguh", "De Visguh", "Everyone in a contest plays the starter's level.", "Iedereen in een wedstrijd speelt het niveau van wie hem start."),
            ("race", "The Raceguh", "De Raceguh", "Three race buttons, one per level.", "Drie raceknoppen, één per niveau.")]) + '</div>'
    SONGS = [("Vadsige Tango", ("makkelijk", "makkelijk"), "100", ("Slow, dramatic and very vads: bandoneon, pizzicato bass and the guhs singing the melody. Starts with one colour, never double counts.",
                                                                  "Langzaam, dramatisch en heel erg vads: bandoneon, pizzicato-bas en de guhs die de melodie zingen. Begint met één kleur, nooit dubbel tellen.")),
             ("Ze hangen aan me vet (70's)", ("medium", "medium"), "110", ("A 70's disco version of the guh record <i>Ze hangen aan me veh</i>, with the whole text sung: <i>\"Ze hangen aan me vet, omdat ik vadsig ben...\"</i> Also the music in the club when nobody dances.",
                                                                          "Een 70's discoversie van de guhplaat <i>Ze hangen aan me veh</i>, met de hele tekst gezongen: <i>\"Ze hangen aan me vet, omdat ik vadsig ben...\"</i> Ook de muziek in de club als er niemand danst.")),
             ("Mika-Mambo", ("lastig", "lastig"), "140", ("Clave, cowbell, congas and brass, with the Mikas giggling as the choir. Starts with three colours, double counts from row 6.",
                                                        "Clave, koebel, conga's en blazers, met de giechelende Mika's als koortje. Begint met drie kleuren, dubbel tellen vanaf rij 6.")),
             ("Njeg-Njeg Boogie", ("bonus, own board", "bonus, eigen bord"), "128", ("Boogie-woogie piano on a disco beat, and guhs singing <i>njeg-njeg!</i>", "Boogiewoogie-piano op een discobeat, en guhs die <i>njeg-njeg!</i> zingen."))]
    disco = h3("The Guhdisco: four real songs", "De Guhdisco: vier echte liedjes") + \
        wentry(shot("scherm_disco", "The DJ-guh's screen"), "The song is the level", "Het liedje is het niveau",
               p("The DJ-guh now plays <b>real songs</b>, and the dance floor <b>follows the beat</b>: the colours flash exactly on the beat, and the tempo no longer "
                 "speeds up. It gets harder with longer rows and double counts. Pick your song, and with it your level:",
                 "De DJ-guh draait nu <b>echte liedjes</b>, en de dansvloer <b>volgt de beat</b>: de kleuren flitsen precies op de tel, en het tempo gaat niet meer "
                 "omhoog. Het wordt moeilijker door langere rijtjes en dubbele tellen. Kies je liedje, en daarmee je niveau:") +
               table([("Song", "Liedje"), ("Level", "Niveau"), ("BPM", "BPM"), ("", "")], [[f"<b>{n}</b>", l, b, d] for n, l, b, d in SONGS]) +
               p("Every song has its own palette and records, the floating board shows all four, and the song loops as long as you dance. The DJ shop now also "
                 "sells the <b>discokoptelefoontje</b> (6 discomunten) for your guh's ears.",
                 "Elk liedje heeft eigen kleuren en records, het zwevende bord laat ze alle vier zien, en het liedje gaat door zolang je danst. Het DJ-winkeltje "
                 "verkoopt nu ook het <b>discokoptelefoontje</b> (6 discomunten) voor de oortjes van je guh.") + icons("disco_koptelefoontje"))

    # ---------------------------------------------------------------- I: clothes as unlocks
    MOVES = [("Striped sweater, raincoat, rain hat, bow ties, sunglasses + the three <b>oorstrikjes</b>", "Gestreepte trui, regenjas, zuidwester, strikjes, zonnebril + de drie <b>oorstrikjes</b>",
              ("the kleermaker (always his whole offer now)", "de kleermaker (nu altijd zijn hele aanbod)")),
             ("Chef jacket and chef hat", "Koksbuis en koksmuts", ("Bakker Korstje in the Knuffeldal (bakmunten)", "Bakker Korstje in het Knuffeldal (bakmunten)")),
             ("Straw hat and overalls", "Strohoed en tuinbroek", ("Boerin Hooibaal: after her 1st and 3rd daily chore", "Boerin Hooibaal: na haar 1e en 3e klusje")),
             ("Firefighter, police, doctor and builder outfits", "Brandweer-, politie-, dokters- en bouwpakje", ("the four <b>beroepen</b> (see below)", "de vier <b>beroepen</b> (zie hieronder)")),
             ("Party hat", "Feesthoedje", ("guh picnic chests only", "alleen de kisten van de guhpicknick")),
             ("Heart glasses, monocle, royal crown", "Hartjesbril, monocle, koninklijke kroon", ("Guhdex milestones only", "alleen Guhdex-mijlpalen")),
             ("Pink onesie", "Roze onesie", ("taming a Brococolief guh (once)", "een Brococolief-guh temmen (één keer)")),
             ("Guh backpack", "Guhrugzak", ("crafting only", "alleen zelf maken")),
             ("Koning set", "Koningspakje", ("the treasure chest of the guh castle", "de schatkist van het guhkasteel")),
             ("Wolkenmuts and wolkenkraag", "Wolkenmuts en wolkenkraag", ("a new chest on the Wolkje of the floating islands", "een nieuwe kist op het Wolkje van de zwevende eilandjes")),
             ("Knight helmet and armour", "Ridderhelm en -harnas", ("cave and castle chests", "kisten in grotten en het kasteel")),
             ("Grill koksmuts, boswachtershoed, plukmuts", "Grill-koksmuts, boswachtershoed, plukmuts", ("their shops only", "alleen hun winkeltjes"))]
    kleding = h3("I &middot; Clothes are now unlocks", "I &middot; Kleertjes zijn nu ontgrendelingen") + \
        wentry(shot("kleding_eten_klaar", "Unlocking a piece"), "Eat it once, wear it forever", "Eén keer opeten, voor altijd aan",
               p("Clothes are still items (from shops, chests and trades), but now you <b>hold right-click for 1.5 seconds</b> to use one up, like eating: sparkles, "
                 "a <i>plop</i>, confetti, and <i>\"Ontgrendeld: ... ! VAHOEG!\"</i>. From then on the piece is yours for <b>all your tamed guhs</b>, even the same "
                 "piece on many guhs at once. Unlocks are per player and stay when you die. Already have it? <i>\"Deze heb je al, njeg! Geef hem aan een vriend.\"</i> "
                 "and the item stays whole.",
                 "Kleertjes zijn nog steeds voorwerpen (uit winkeltjes, kisten en ruilen), maar nu <b>houd je rechtsklik 1,5 seconde ingedrukt</b> om er een op te "
                 "maken, net als eten: fonkels, een <i>plop</i>, confetti, en <i>\"Ontgrendeld: ... ! VAHOEG!\"</i>. Vanaf dan is het stuk van jou voor <b>al je tamme "
                 "guhs</b>, zelfs hetzelfde stuk op heel veel guhs tegelijk. Ontgrendelingen zijn per speler en blijven als je doodgaat. Heb je hem al? <i>\"Deze heb je "
                 "al, njeg! Geef hem aan een vriend.\"</i> en het voorwerp blijft heel.") +
               ul([("Only the <b>owner</b> dresses a guh, with his own unlocks. A guh that gets a new owner keeps what it wears.",
                    "Alleen de <b>baas</b> kleedt een guh aan, met zijn eigen ontgrendelingen. Een guh die een nieuwe baas krijgt houdt aan wat hij draagt."),
                   ("Guhs never drop clothes: wild guhs and the guhs in buildings still wear them, just for looks.",
                    "Guhs laten nooit kleertjes vallen: wilde guhs en de guhs in gebouwen dragen ze nog wel, gewoon omdat het leuk staat."),
                   ("Hairstyles stay with Kapper Krulletje: they are not unlocks.", "Kapsels blijven bij Kapper Krulletje: dat zijn geen ontgrendelingen.")])) + \
        entry(shot("kleding_al_heb", "Deze heb je al"), "Deze heb je al, njeg!", "Deze heb je al, njeg!",
              p("The second time: the piece stays whole, so you can give it to a friend.", "De tweede keer: het stuk blijft heel, dus je kunt het aan een vriend geven.")) + \
        wentry(shot("kast_hoofd", "The new wardrobe"), "The new wardrobe", "De nieuwe kledingkast",
               ul([("A big <b>3D preview</b> of your guh: drag or scroll to turn it. Everything you pick is tried on first; <b>Aantrekken!</b> makes it real, <b>Terug</b> undoes it.",
                    "Een groot <b>3D-voorbeeld</b> van je guh: sleep of scroll om hem te draaien. Alles wat je kiest past hij eerst; <b>Aantrekken!</b> maakt het echt, <b>Terug</b> zet het terug."),
                   ("Per slot a tab (with the piece that's tried on as its icon) and a scrolling list of your unlocks + <i>Niets</i>; the worn piece says <i>draagt hij</i>.",
                    "Per vakje een tabblad (met het gepaste stuk als icoontje) en een scrollende lijst van je ontgrendelingen + <i>Niets</i>; wat hij draagt staat erbij als <i>draagt hij</i>."),
                   ("A <b>search box</b>, a <b>filter per source</b>, the <b>Dobbel!</b> button (a random outfit) and <b>5 favourite outfits</b> (click = try on, shift-click = save).",
                    "Een <b>zoekvakje</b>, een <b>filter per bron</b>, de knop <b>Dobbel!</b> (een willekeurige outfit) en <b>5 lievelingsoutfits</b> (klik = passen, shift-klik = bewaren)."),
                   ("The tab <b>Rugzak &amp; harnas</b>: the armour slot and the 18 backpack slots (they belong to that guh). A backpack only comes off when it's empty.",
                    "Het tabblad <b>Rugzak &amp; harnas</b>: het pantservakje en de 18 rugzakvakjes (die horen bij die guh). Een rugzak gaat alleen af als hij leeg is.")])) + \
        '<div class="gallery">' + "".join(gfig(f"shot29_kast_{n}", en, nl, "", "", den, dnl) for n, en, nl, den, dnl in [
            ("oren", "The ear slot", "Het oorvakje", "Oorstrikjes, oorbelletjes, oorwarmers and the koptelefoontje.", "Oorstrikjes, oorbelletjes, oorwarmers en het koptelefoontje."),
            ("zoek", "Search", "Zoeken", "Type <i>muts</i> and see every hat you have.", "Typ <i>muts</i> en zie al je mutsen."),
            ("dobbel", "Dobbel!", "Dobbel!", "<i>Gedobbeld! Bevalt hij? Klik Aantrekken!</i>", "<i>Gedobbeld! Bevalt hij? Klik Aantrekken!</i>"),
            ("rugzak", "Rugzak &amp; harnas", "Rugzak &amp; harnas", "Armour and the backpack, with your own things below.", "Pantser en de rugzak, met je eigen spullen eronder.")]) + '</div>' + \
        wentry(shot("oren_rij2_oorwarmers", "Ear pieces with hats"), "The new slot: Oren", "Het nieuwe vakje: Oren",
               p("Guhs now have a seventh clothing slot, for the <b>ears</b> (after the hair of 2.8). Everything sits on the ears themselves, so it wiggles along and "
                 "fits with every hat and hairstyle. The pieces:",
                 "Guhs hebben nu een zevende kledingvakje, voor de <b>oren</b> (na het haar van 2.8). Alles zit op de oortjes zelf, dus het wiebelt mee en past bij elke "
                 "hoed en elk kapsel. De stukken:") +
               '<div class="cards">' + "".join(f'<div class="card"><figure class="stage">{img("oren_" + pid, name)}</figure><h3>{icon(pid, name)} {name}</h3><p>{t(wen, wnl)}</p></div>'
                                              for pid, name, wen, wnl in [
                   ("oorstrikje_roze", "Roze oorstrikjes", "the kleermaker", "de kleermaker"), ("oorstrikje_mint", "Mintgroene oorstrikjes", "the kleermaker", "de kleermaker"),
                   ("oorstrikje_geel", "Gele oorstrikjes", "the kleermaker", "de kleermaker"), ("katapult_oorbelletjes", "Pluisbal-oorbelletjes", "the Knabbelkatapult", "de Knabbelkatapult"),
                   ("elftocht_oorwarmers", "Pluizige oorwarmers", "the Elf-Guhjestocht", "de Elf-Guhjestocht"), ("disco_koptelefoontje", "Discokoptelefoontje", "the Guhdisco", "de Guhdisco")]) + '</div>') + \
        wentry(img("oren_met_muts", "Oorwarmers with a schaatsmuts"), "Every piece has exactly one source", "Elk stuk komt van precies één plek",
               p("A minigame piece only comes from its own minigame, and every other piece has one logical home. The Guhdex tab <i>Kleding</i> tells you where each "
                 "one comes from. What moved:",
                 "Een minigamestuk komt alleen van zijn eigen minigame, en elk ander stuk heeft één logische plek. Het Guhdex-tabblad <i>Kleding</i> vertelt je waar elk "
                 "stuk vandaan komt. Wat er verhuisde:") +
               table([("Piece", "Stuk"), ("Now from", "Nu van")], [[t(a, b), c] for a, b, c in MOVES]))

    # ---------------------------------------------------------------- H + K: Guhdex and superkompas
    gids = h3("H &middot; The Guhdex and the superkompas", "H &middot; De Guhdex en het superkompas") + \
        wentry(shot("gids_minigames_01", "The Minigames tab"), "The Guhdex: four icon tabs", "De Guhdex: vier icoontjestabbladen",
               p("The Guhdex now has <b>icon tabs</b> on top (the name shows when you hover): <b>Guhs</b>, <b>Knus</b>, <b>Minigames</b> and <b>Kleding</b>. The old "
                 "Highscores tab is inside <i>Minigames</i> now. Everything scrolls with the mouse wheel, and groups fold open and shut.",
                 "De Guhdex heeft nu <b>icoontjestabbladen</b> bovenaan (de naam zie je als je erop wijst): <b>Guhs</b>, <b>Knus</b>, <b>Minigames</b> en <b>Kleding</b>. "
                 "Het oude tabblad Highscores zit nu in <i>Minigames</i>. Alles scrollt met het muiswiel, en groepjes klappen open en dicht.") +
               ul([("<b>Minigames</b>: how many game buildings you found, then the groups <i>Klassiekers</i>, <i>Knuffeldal</i> and <i>De Grote Guhspelen</i>. Per "
                    "building: where to find it, whether you've been there, its clothes as icons, and a score table per level, track, event or song with your best, "
                    "the server record and who holds it (a gold star when it's you).",
                    "<b>Minigames</b>: hoeveel spelgebouwen je vond, en dan de groepen <i>Klassiekers</i>, <i>Knuffeldal</i> en <i>De Grote Guhspelen</i>. Per gebouw: "
                    "waar je het vindt, of je er al was, zijn kleertjes als icoontjes, en een scoretabel per niveau, baan, onderdeel of liedje met je beste, het "
                    "serverrecord en wie het heeft (een gouden ster als jij het bent)."),
                   ("<b>Kleding</b>: <i>Ontgrendeld x / 141</i>, grouped per source. Grey icons are still locked (hover to see the name, the source and the price), "
                    "colourful ones with a green border are yours. Hairstyles are listed last, <i>bij de kapper</i>.",
                    "<b>Kleding</b>: <i>Ontgrendeld x / 141</i>, per bron gegroepeerd. Grijze icoontjes zitten nog op slot (wijs erop voor de naam, de bron en de prijs), "
                    "gekleurde met een groen randje zijn van jou. Kapsels staan onderaan, <i>bij de kapper</i>."),
                   ("<b>Knus</b> and <b>Guhs</b> work as before (with the Piep section), and there are twelve new pages: the Pinguh and the eleven new guh characters.",
                    "<b>Knus</b> en <b>Guhs</b> werken zoals altijd (met het onderdeel Piep), en er zijn twaalf nieuwe pagina's: de Pinguh en de elf nieuwe guhpersonages.")])) + \
        '<div class="gallery">' + "".join(gfig(f"shot29_{n}", en, nl, "", "", den, dnl) for n, en, nl, den, dnl in [
            ("gids_kleding_slot_01", "Kleding: 5 of 141", "Kleding: 5 van 141", "Just started: almost everything is still grey.", "Net begonnen: bijna alles is nog grijs."),
            ("gids_kleding_01", "Kleding: all 141", "Kleding: alle 141", "Everything unlocked: green borders everywhere. VAHOEG!", "Alles ontgrendeld: overal groene randjes. VAHOEG!"),
            ("gids_minigames_16", "Minigames: the Elf-Guhjestocht", "Minigames: de Elf-Guhjestocht", "Scores per event and the tour, with the outfit.", "Scores per onderdeel en de tocht, met het pakje."),
            ("guhdex_38_pinguh", "The Pinguh's page", "De pagina van de Pinguh", "Page 38 of 65.", "Pagina 38 van 65."),
            ("guhdex_55_sjoelguh", "Opoe Njegschuif's page", "De pagina van Opoe Njegschuif", "Unique: in the Sjoelhuisje.", "Uniek: in het Sjoelhuisje."),
            ("gids_knus_overzicht", "The Knus tab", "Het tabblad Knus", "Still all your Knuffeldal milestones.", "Nog steeds al je Knuffeldal-mijlpalen.")]) + '</div>' + \
        wentry(shot("gids_superkompas_03_minigames", "The superkompas"), "The superkompas", "Het superkompas",
               p("The superkompas has a row of <b>icon tabs</b> too (Avontuur, Quests, Minigames, Wonderen, Wonen, Einde, Ondergrond, Barbecue, Knus) with a "
                 "two-column list of places. <b>Minigames</b> now holds every game, under the subheadings <i>Klassiekers</i>, <i>Knuffeldal</i> and <i>De Grote "
                 "Guhspelen</i>, with the game's icon and a green tick once you've been there. The place you're heading for is gold.",
                 "Het superkompas heeft ook een rij <b>icoontjestabbladen</b> (Avontuur, Quests, Minigames, Wonderen, Wonen, Einde, Ondergrond, Barbecue, Knus) met "
                 "een lijst van plekken in twee kolommen. <b>Minigames</b> heeft nu alle spellen, onder de kopjes <i>Klassiekers</i>, <i>Knuffeldal</i> en <i>De Grote "
                 "Guhspelen</i>, met het icoontje van het spel en een groen vinkje als je er al was. De plek waar je heen gaat is goud.") +
               p("Visit all six new buildings for <i>Guhspelen-ontdekker</i>, and every minigame building for <i>Vahoege wereldreiziger</i>.",
                 "Bezoek alle zes nieuwe gebouwen voor <i>Guhspelen-ontdekker</i>, en elk minigamegebouw voor <i>Vahoege wereldreiziger</i>."))

    # ---------------------------------------------------------------- J: beroepen
    BEROEPEN = [("brandweerguh", "Brandweercommandant Blusguh", ("Brandweerkazerne", "Brandweerkazerne"),
                 ("Tatuu-tatuu! The marshmallow campfires on the oefenterrein flared up. Take the <b>guh-brandslang</b>, hold right-click to spray them out, then "
                  "climb the ladder and help the guhtje out of the tree.",
                  "Tatuu-tatuu! De marshmallowkampvuurtjes op het oefenterrein laaien op. Pak de <b>guh-brandslang</b>, houd rechtsklik ingedrukt om ze uit te spuiten, "
                  "klim dan de ladder op en help het guhtje uit de boom."), ("Firefighter helmet + jacket", "Brandweerhelm + -jas")),
                ("politieguh", "Inspecteur Vahoegsma", ("Politiebureautje", "Politiebureautje"),
                 ("<i>De Knabbeldief-zaak</i>: a Mika pinched the town's knabbel stock from the knabbelkluis! Follow the pink paw prints to the stolen sack (a "
                  "different hiding place each time). The Knabbeldief giggles and runs off.",
                  "<i>De Knabbeldief-zaak</i>: een Mika heeft de knabbelvoorraad van het stadje uit de knabbelkluis gepikt! Volg de roze pootafdrukken naar de gestolen zak "
                  "(elke keer een andere verstopplek). De Knabbeldief giechelt en rent weg."), ("Police cap + uniform", "Politiepet + -uniform")),
                ("apothekerguh", "Dokter Snotneus-guh", ("Apotheekje", "Apotheekje"),
                 ("Snotje is snotterig (hatsjoe!). Get a kaasmelk from the doctor, pick 3 <b>snotkruidjes</b> in the herb garden, mix them in the mengketel and give "
                  "Snotje the <b>kaasmelkdrankje</b>. Hatsjoe... VAHOEG!",
                  "Snotje is snotterig (hatsjoe!). Haal een kaasmelk bij de dokter, pluk 3 <b>snotkruidjes</b> in de kruidentuin, meng ze in de mengketel en geef Snotje "
                  "het <b>kaasmelkdrankje</b>. Hatsjoe... VAHOEG!"), ("Doctor coat + stethoscope", "Doktersjas + stethoscoop")),
                ("bouwvakkerguh", "Bob de Guhbouwer", ("a guh village (1 in 3)", "een guhdorp (1 op 3)"),
                 ("Bob's house has no roof yet. Bring him 16 planks and 8 kaasknabbels (his lunch), then lay a roof tile on every ghost tile. The last one raises the "
                  "flag. Kunnen wij het maken?",
                  "Het huisje van Bob heeft nog geen dak. Breng hem 16 planken en 8 kaasknabbels (zijn lunch), en leg dan op elk spookpannetje een dakpan. De laatste "
                  "hijst de vlag. Kunnen wij het maken?"), ("Builder helmet + safety vest", "Bouwhelm + veiligheidshesje"))]
    beroepen = h3("J &middot; Beroepen", "J &middot; Beroepen") + \
        wentry(img("structure_beroepenstraat", "The Beroepenstraat"), "The Beroepenstraat", "De Beroepenstraat",
               p("The Knuffeldal town got a new street along its east side: the <b>Beroepenstraat</b>, with the <b>Brandweerkazerne</b> (red brick, two guh fire trucks, "
                 "a hose tower and a roof like a giant fire helmet), a little square with a guh statue wearing three hats, the <b>Politiebureautje</b> (blue and orange "
                 "stripes, the knabbelkluis, a knuffelcel and a roof like a giant police cap) and the <b>Apotheekje</b> (a glowing green cross and a round guh-head roof "
                 "with a big mortar), with a herb garden and a little park behind it.",
                 "Het Knuffeldal-stadje kreeg een nieuwe straat langs de oostkant: de <b>Beroepenstraat</b>, met de <b>Brandweerkazerne</b> (rode baksteen, twee "
                 "guh-brandweerautootjes, een slangentoren en een dak als een reuzenbrandweerhelm), een pleintje met een guhstandbeeld met drie hoeden op, het "
                 "<b>Politiebureautje</b> (blauw-oranje strepen, de knabbelkluis, een knuffelcel en een dak als een reuzenpolitiepet) en het <b>Apotheekje</b> (een "
                 "gloeiend groen kruis en een rond guhkopdak met een grote vijzel), met een kruidentuin en een parkje erachter.")) + \
        wentry(shot("beroepenstraat_west", "The Beroepenstraat"), "Four jobs, one time each", "Vier klusjes, elk één keer",
               table([("Guh", "Guh"), ("Where", "Waar"), ("The job", "Het klusje"), ("Reward", "Beloning")],
                     [[f'<img src="img/npc_{k}.png" alt="{n}" loading="lazy" style="width:56px;height:56px;vertical-align:middle"> <b>{n}</b>', w, d, r] for k, n, w, d, r in BEROEPEN]) +
               p("Every job is <b>one time only</b>; afterwards the guh just thanks you. The reward is the work outfit (as items: eat them to unlock). All four: "
                 "<i>Guh van alle markten</i>.",
                 "Elk klusje doe je <b>maar één keer</b>; daarna bedankt de guh je alleen nog. De beloning is het werkpakje (als voorwerpen: eet ze op om ze te "
                 "ontgrendelen). Alle vier: <i>Guh van alle markten</i>.")) + \
        entry(img("beroepen_npcs", "The four beroepen guhs"), "The beroepen guhs", "De beroepenguhs",
              p("Blusguh, Inspecteur Vahoegsma, Dokter Snotneus-guh and Bob de Guhbouwer.", "Blusguh, Inspecteur Vahoegsma, Dokter Snotneus-guh en Bob de Guhbouwer.")) + \
        entry(shot("beroepenstraat_oost", "The Beroepenstraat from the back"), "From the back", "Van achteren",
              p("The backs on the little achterpad are nice too: a balcony with flower boxes and a big guh face at the kazerne, barred cell windows and a "
                "balcony with blue lamps at the politie, a glowing green cross and flower boxes at the apotheek, and rain chains into water butts.",
                "Ook de achterkanten aan het achterpaadje zijn mooi: een balkon met bloembakken en een groot guhgezicht bij de kazerne, tralieraampjes en een "
                "balkon met blauwe lampjes bij de politie, een gloeiend groen kruis en bloembakken bij de apotheek, en regenkettingen in regentonnen.")) +         entry(shot("npc_politieguh_in_gebouw", "Inspecteur Vahoegsma"), "Inside the Politiebureautje", "In het Politiebureautje",
              p("Inspecteur Vahoegsma behind his counter.", "Inspecteur Vahoegsma achter zijn balie.")) + \
        entry(img("structure_guhdorp_bouwplaats", "Bob's bouwplaats"), "Bob's bouwplaats", "De bouwplaats van Bob",
              p("One in three new guh villages has a half-built house with scaffolding, a little yellow crane with a pallet of roof tiles, a cement mixer and a "
                "building sign. Its roof is still see-through ghost tiles.",
                "Eén op de drie nieuwe guhdorpen heeft een half gebouwd huisje met steigers, een geel kraantje met een pallet dakpannen, een cementmolen en een "
                "bouwbord. Het dak is nog van doorzichtige spookpannetjes.")) + \
        entry(shot("npc_bouwvakkerguh_template", "Bob de Guhbouwer"), "Bob de Guhbouwer", "Bob de Guhbouwer",
              p("<i>\"Kunnen wij het maken? JA, WIJ KUNNEN HET!\"</i> (When the next player comes, the Mikas have \"borrowed\" the tiles again.)",
                "<i>\"Kunnen wij het maken? JA, WIJ KUNNEN HET!\"</i> (Als de volgende speler komt, hebben de Mika's de dakpannen weer \"geleend\".)")) + \
        entry(img("beroepen_blokken", "Beroepen blocks"), "Blocks of the jobs", "Blokken van de klusjes",
              p("The marshmallow campfire pit, the mengketel, snotkruid, a sack of stolen knabbels and a roof tile. The campfire pit and the mengketel can be crafted.",
                "De marshmallowkampvuurkuil, de mengketel, snotkruid, een zak gepikte knabbels en een dakpan. De kampvuurkuil en de mengketel kun je zelf maken.")) + \
        '<div class="recipes">' + recipe_card("beroepen_marshmallowvuur", "Marshmallow campfire pit", "Marshmallowkampvuurkuil") + \
        recipe_card("beroepen_mengketel", "Mengketel", "Mengketel") + '</div>'

    # ---------------------------------------------------------------- piep menus and the rest
    piep = h3("Piep: little menus, picking up, and kontpoetsen", "Piep: menuutjes, oppakken en kontpoetsen") + \
        '<div class="gallery">' + "".join(gfig(f"shot29_piepmenu_{n}", en, en, "", "", den, dnl) for n, en, den, dnl in [
            ("muisje", "Pieppiepmuisje", "Rondvadsen, Volg mij, Piepjes, Verstoppertje and <i>Op mijn schouder!</i>", "Rondvadsen, Volg mij, Piepjes, Verstoppertje en <i>Op mijn schouder!</i>"),
            ("poepschilly", "Poepschilly", "Rondvadsen, Volg mij, Zwemmen and <i>Kontje poetsen!</i>", "Rondvadsen, Volg mij, Zwemmen en <i>Kontje poetsen!</i>"),
            ("schilly", "Schilly", "Rondvadsen, Volg mij, Zwemmen, Besties zoeken and <i>Bestie-moment!</i>", "Rondvadsen, Volg mij, Zwemmen, Besties zoeken en <i>Bestie-moment!</i>")]) + '</div>' + \
        ul([("Click your own pieppiepmuisje, Poepschilly or Schilly with an empty hand: a heart, and its <b>own little menu</b>: rename it, switch rondvadsen, "
             "following, peeping, verstoppertje, swimming or bestie-seeking on and off, and the big button.",
             "Klik je eigen pieppiepmuisje, Poepschilly of Schilly aan met een lege hand: een hartje, en zijn <b>eigen menuutje</b>: geef het een naam, zet rondvadsen, "
             "volgen, piepen, verstoppertje, zwemmen of besties zoeken aan en uit, en de grote knop."),
            ("<b>Pick up</b> the turtles too now (sneak + right-click, or <i>Oppakken</i>): they become an item that keeps their name and everything. Right-click a block to put them back.",
             "Nu kun je ook de schildpadjes <b>oppakken</b> (sluip + rechtsklik, of <i>Oppakken</i>): ze worden een voorwerp dat hun naam en alles onthoudt. Rechtsklik op een blok om ze terug te zetten."),
            ("<b>Kontpoetsen</b> works on your own tamed guhs now: press <i>Kontje poetsen!</i> and click a guh. Poepschilly waddles to its kontje, crawls in "
             "(it tickles! guh guh guhhh), scrubs for five seconds with soap bubbles and sparkles, and pops out again: the guh is <i>Fris van binnen</i> and blushes. "
             "<i>Bestie-moment!</i> works the same way for Schilly.",
             "<b>Kontpoetsen</b> werkt nu op je eigen tamme guhs: druk op <i>Kontje poetsen!</i> en klik een guh aan. Poepschilly waggelt naar zijn kontje, kruipt erin "
             "(het kriebelt! guh guh guhhh), poetst vijf seconden met zeepbelletjes en fonkels, en ploept er weer uit: de guh is <i>Fris van binnen</i> en bloost. "
             "<i>Bestie-moment!</i> werkt zo ook voor Schilly."),
            ("The five turtle quests moved to the FTB chapter <b>Onderwater</b> (section <i>Schildpadjes</i>).",
             "De vijf schildpadquests zijn verhuisd naar het FTB-hoofdstuk <b>Onderwater</b> (onderdeel <i>Schildpadjes</i>).")])
    rest = h3("And also", "En verder") + \
        entry(img("guh_slapen", "A sleeping guh"), "Sleepy eyes", "Slaapoogjes",
              p("A guh that sleeps (the emote <i>Slapen</i>, or napping in a guh nest) now <b>closes its eyes</b>: two soft curves with little lashes. Every guh "
                "variant, the Pinguhs too. Zzz.",
                "Een guh die slaapt (de emote <i>Slapen</i>, of een dutje in een guhnestje) doet nu <b>zijn oogjes dicht</b>: twee zachte boogjes met wimpertjes. Elke "
                "guhvariant, de Pinguhs ook. Zzz.")) + \
        entry(img("pinguhs_slapen", "Sleeping Pinguhs"), "Sleeping Pinguhs", "Slapende Pinguhs",
              p("Even a Pinguh keeps its white face when it dozes off.", "Zelfs een Pinguh houdt zijn witte gezichtje als hij indut.")) + \
        entry(img("guh_variant_zeemeerguh", "Zeemeerguh"), "More Zeemeerguhs", "Meer Zeemeerguhs",
              p("In the Guhzee and the Diepe Guhzee you now meet a wild <b>Zeemeerguh</b> much more often (about three times as often), and up to two at a time.",
                "In de Guhzee en de Diepe Guhzee kom je nu veel vaker een wilde <b>Zeemeerguh</b> tegen (zo'n drie keer zo vaak), en soms twee tegelijk.")) + \
        entry(img("knabbeldief_mika", "Knabbeldief-Mika"), "Three new Mikas, all harmless", "Drie nieuwe Mika's, allemaal onschuldig",
              p("The <b>Heg-Mika</b> (doolhof), the <b>Mika-pikker</b> (circuit) and the <b>Knabbeldief</b> (politie). They pinch, giggle and run. That's all.",
                "De <b>Heg-Mika</b> (doolhof), de <b>Mika-pikker</b> (circuit) en de <b>Knabbeldief</b> (politie). Ze pikken, giechelen en rennen weg. Meer niet."))

    ADV = [("De Grote Guhspelen", ["De Grote Guhspelen", "Guhspelen-ontdekker", "Vahoege wereldreiziger &#9733;"]),
           ("Sjoelen", ["Komt dat zien!", "Twintig schijfjes", "Opoe is trots", "Honderd! VAHOEG! &#9733;", "Gebreid door Opoe"]),
           ("Doolhof", ["Verdwaald in de heg", "Alle knabbels terug!", "Doolhofmeester", "Niet te pakken! &#9733;", "Op ontdekkingsreis"]),
           ("Katapult", ["Floep!", "Twaalf forten", "Drie sterren!", "Tegen de wind in &#9733;", "Zesendertig sterren &#9733;", "Klaar om te floepen"]),
           ("Knabbelspelen", ["Hooggeëerd publiek!", "Op je plaatsen...", "Samen VAHOEG", "Precies op z'n kontje!", "De Grote Zeskamp", "Kampioen van de tent",
                              "Vahoegste van de Zeskamp &#9733;", "Sportief gekleed"]),
           ("Guhpolder", ["Brrr, de Guhpolder!", "Een bloemetje van ijs", "Vers gemalen", "Een vadsige Pinguh"]),
           ("Elf-Guhjestocht", ["Een bevroren kanaal!", "It giet oan!", "Elf-Guhjeskruisje &#9733;", "Supervahoege schaatsguh &#9733;", "Warm ingepakt"]),
           ("Circuit", ["Welkom op het circuit!", "Brrrm, VAHOEG!", "Boven de wolken", "Wieeee, de looping!", "Bovenop de Kaasberg", "Circuitkampioen",
                        "Eigenwijze renguh", "Goud op lastig! VAHOEG! &#9733;", "Sneller dan de legende &#9733;", "Echte coureur", "Racen op lastig"]),
           ("Klassiekers", ["Rustig aan, guhtje", "Zoals vanouds", "Streng maar vadsig", "Bliksemmepper", "Tegen de wind in", "Vet vadsig", "Snelle hengel",
                            "Klassiekers-kampioen &#9733;"]),
           ("Disco", ["Ze hangen aan me vet!", "Vadsige Tango", "Mika-Mambo", "Njeg-Njeg Boogie", "Mika-Mambo-meester &#9733;", "De hele plaat rond", "Beats op je oren"]),
           ("Kleding", ["Ontgrendeld!", "Oren op steeltjes", "Mijn lievelingsoutfit", "Van top tot teen", "Een vadsige kledingkast",
                        "De chicste guh van de Guhmensie &#9733;"]),
           ("Beroepen", ["Tatuu-tatuu!", "De Knabbeldief-zaak", "Hatsjoe... VAHOEG!", "Kunnen wij het maken?", "Guh van alle markten &#9733;"])]
    adv = h3("Advancements and quests", "Vooruitgangen en quests") + \
        p("A new advancement tab, <b>De Grote Guhspelen</b> (&#9733; = challenge):", "Een nieuw vooruitgangentabblad, <b>De Grote Guhspelen</b> (&#9733; = uitdaging):") + \
        table([("Part", "Onderdeel"), ("Advancements", "Vooruitgangen")], [[f"<b>{g}</b>", " &middot; ".join(a)] for g, a in ADV]) + \
        p(f"In <b>FTB Quests</b> the chapter <i>Minigames &amp; bijzondere plekken</i> now has {minigames_quests} quests, with sections for the classics' levels, "
          "the six new games, the Guhpolder and <i>Op ontdekkingstocht</i>. The beroepen are in the Knuffeldal chapter, the clothing quests in the first chapter. "
          "Nothing is locked.",
          f"In <b>FTB Quests</b> heeft het hoofdstuk <i>Minigames &amp; bijzondere plekken</i> nu {minigames_quests} quests, met onderdelen voor de niveaus van de "
          "klassiekers, de zes nieuwe spellen, de Guhpolder en <i>Op ontdekkingstocht</i>. De beroepen staan in het Knuffeldal-hoofdstuk, de kledingquests in het "
          "eerste hoofdstuk. Niets zit op slot.")
    cmds = h3("Handy commands", "Handige commando's") + \
        cmd("/execute in guhs:guhmension run locate biome guhs:guhpolder") + \
        "".join(cmd(f"/execute in guhs:guhmension run locate structure guhs:{s}") for s in
                ("sjoelhuisje", "guhdoolhof", "knabbelkatapult", "knabbelspelen", "elfguhjestocht", "guh_circuit", "knuffeldal_stadje")) + \
        cmd('/summon guhs:guh ~ ~ ~ {Variant:"pinguh"}') + cmd("/give @s guhs:guh_molentje") + cmd("/give @s guhs:elftocht_oorwarmers")

    body = intro + parts + npcs + sjoel + doolhof + katapult + spelen + polder + elftocht + circuit + klassiek + disco + kleding + gids + beroepen + piep + \
        rest + adv + cmds
    return section("new29", "New in 2.9: De Grote Guhspelen", "Nieuw in 2.9: De Grote Guhspelen", body)


def lieve_vadsjes_section():
    """2.10.0 "Lieve vadsjes van elkaar": the hartjesmeter (3 levels), favorietjes, the Guhhuisje (3 sizes, its screen and the
    klus-area dome), the ten klusjes, four toys, samen (minigames, reactions, friendships), the zielsguh extras, the Guhdex tab
    Mijn guhs (dagboekje) and the Guhkamer + Guhbel. Texts after guhs_work210/DESIGN_210.md, CONTRACT_210.md, the slice
    reports and the lang files; pictures from wiki_renders.main_v210 and the 2.10 visual QA screenshots."""
    import json
    band_quests = next((n for f, _, _, n in ftb_chapters() if f == "guhs_band"), 0)
    shot = lambda name, alt: img("shot210_" + name, alt, "shot")
    wentry = lambda *a, **k: entry(*a, wide=True, **k)

    def fig(name, en, nl, den, dnl, sub_en="", sub_nl=""):
        cls = "shot" if name.startswith("shot") else ""
        sub = f'<span class="rarity">{t(sub_en, sub_nl)}</span>' if sub_en else ""
        return f'''<figure><div class="stage">{img(name, en, cls)}</div><figcaption><h3>{t(en, nl)}{sub}</h3>
<p>{t(den, dnl)}</p></figcaption></figure>'''

    gallery = lambda *figs: '<div class="gallery">' + "".join(figs) + '</div>'

    intro = p("Guhs 2.10 <b>Lieve vadsjes van elkaar</b> is the cuddle update: it's all about <b>you and your own guhs</b>. Every tamed guh now has a "
              "<b>hartjesmeter</b> with you that only ever goes up, secret <b>favorietjes</b> to discover, a <b>Guhhuisje</b> shaped like a guh head to live in, "
              "<b>ten little chores</b> it does around its home, <b>four toys</b>, guh <b>friends</b>, a <b>dagboekje</b> in the Guhdex and a <b>Guhkamer</b> in "
              "your Guhmaag where it can stay when it doesn't come along. Your guh cheers for you in the minigames, jumps on the back of your kart, skates along "
              "on the Elf-Guhjestocht, comforts you, waves you goodnight and cuddles you when it thunders. As always: guhs are only ever lief, hearts never "
              "go down, and a guh is never too vads (it's always just not VAHOEG enough yet). Njeg!",
              "Guhs 2.10 <b>Lieve vadsjes van elkaar</b> is de knuffelupdate: alles draait om <b>jou en je eigen guhs</b>. Elke tamme guh heeft nu een "
              "<b>hartjesmeter</b> met jou die alleen maar omhoog gaat, geheime <b>favorietjes</b> om te ontdekken, een <b>Guhhuisje</b> in de vorm van een "
              "guhhoofd om in te wonen, <b>tien klusjes</b> die hij rond zijn huisje doet, <b>vier soorten speelgoed</b>, guh-<b>vriendjes</b>, een "
              "<b>dagboekje</b> in de Guhdex en een <b>Guhkamer</b> in je Guhmaag waar hij kan logeren als hij niet meegaat. Je guh juicht voor je bij de "
              "minigames, springt achterop je kart, schaatst mee op de Elf-Guhjestocht, troost je, zwaait je welterusten en kruipt tegen je aan als het "
              "onweert. Zoals altijd: guhs zijn alleen maar lief, hartjes gaan nooit omlaag, en een guh is nooit te vads (hij is hooguit nog niet VAHOEG "
              "genoeg). Njeg!")
    parts = ul([("<b>Hartjes</b>: a hartjesmeter per guh with three levels, each with a piece of clothing and an emote; extra sparkle for a zielsguh.",
                 "<b>Hartjes</b>: een hartjesmeter per guh met drie niveaus, elk met een kleertje en een emote; extra glitter voor een zielsguh."),
                ("<b>Favorietjes</b>: eight secret favourites per guh, found by trying things (warm and cold hints).",
                 "<b>Favorietjes</b>: acht geheime lievelingsdingen per guh, te vinden door dingen te proberen (warm- en koud-hints)."),
                ("<b>Het Guhhuisje</b> in three sizes, with its own screen, a name and a blue <i>klus-area</i> dome.",
                 "<b>Het Guhhuisje</b> in drie maten, met een eigen scherm, een naam en een blauwe <i>klus-area</i>-koepel."),
                ("<b>Tien klusjes</b>: digging, farming, tidying, animals, baking, fishing, keeping watch, picking, lamps and babysitting.",
                 "<b>Tien klusjes</b>: graven, farmen, opruimen, dieren, bakken, vissen, waken, plukken, lampjes en oppassen."),
                ("<b>Speelgoed</b>: the knabbelbal, the guh-glijbaantje, the pluizige tunnel, the wip and the schommel.",
                 "<b>Speelgoed</b>: de knabbelbal, het guh-glijbaantje, de pluizige tunnel, de wip en de schommel."),
                ("<b>Samen</b>: cheering in the minigames, riding and skating along, sweet reactions, and guh friendships.",
                 "<b>Samen</b>: juichen bij de minigames, meerijden en meeschaatsen, lieve reacties, en guh-vriendschappen."),
                ("<b>Guhdex tab Mijn guhs</b>: a dagboekje for every tamed guh, including <i>where is it now?</i>",
                 "<b>Guhdex-tabblad Mijn guhs</b>: een dagboekje voor elke tamme guh, met <i>waar is hij nu?</i>"),
                ("<b>De Guhkamer</b>: a guest room in your Guhmaag, and the <b>Guhbel</b> to send guhs there and call them back.",
                 "<b>De Guhkamer</b>: een logeerkamer in je Guhmaag, en de <b>Guhbel</b> om guhs erheen te sturen en terug te roepen."),
                ("<b>Fixes</b>: land around sunk buildings, the circuit, golf tees, the doolhof, names, the Guhdex and a rebuilt Elf-Guhjestocht (see <a href='#fixes210'>Fixes in 2.10</a>).",
                 "<b>Fixes</b>: land rond verzonken gebouwen, het circuit, golf-afslagen, het doolhof, naampjes, de Guhdex en een nieuwe Elf-Guhjestocht (zie <a href='#fixes210'>Fixes in 2.10</a>).")])

    # ---------------------------------------------------------------- A: the hartjesmeter
    HEARTS = [("Feeding a snack", "Een snackje geven", "2", "40"), ("Petting (a short tap)", "Aaien (kort tikken)", "1", "20"),
              ("<i>Knuffelen!</i> in the guh menu", "<i>Knuffelen!</i> in het guhmenu", "5", "50"),
              ("Time together (per minute within 16 blocks)", "Samen zijn (per minuut binnen 16 blokken)", "1", "60"),
              ("Travelling together", "Samen op reis", "1", "50"), ("A chore from its huisje", "Een klusje vanuit zijn huisje", "2", "40"),
              ("A minigame together", "Samen een minigame", "15", "100"), ("A record while it watches", "Een record terwijl hij kijkt", "10", "50"),
              ("Discovering a favourite", "Een favorietje ontdekken", "50", "400"), ("A favourite again", "Weer een favorietje", "5", "30"),
              ("Playing with a toy", "Spelen met speelgoed", "3", "30"), ("With its guh friends", "Met zijn guh-vriendjes", "2", "20")]
    hartjes = h3("A &middot; De hartjesmeter", "A &middot; De hartjesmeter") + \
        wentry(img("band_niveaus", "The three levels: hartjesspeldje, knuffeltruitje, zielskroontje"), "Hearts that never go down", "Hartjes die nooit omlaag gaan",
               p("Every guh you tame starts a <b>hartjesmeter</b> with you (guhs you already had count too). Everything you do together fills it: feed it a snack, "
                 "tap it for an <i>aaitje</i>, give it a big <b>Knuffelen!</b> from its menu, play, travel, let it do chores. Pink hearts float up and the bar "
                 "above your hotbar says <i>+2 &hearts;</i>. <b>Hearts never go down</b>, not ever: no punishment, no hunger, no sulking. There is a little limit "
                 "per day for every source, so a guh doesn't get spoilt (njeg), and a <b>blij</b> guh (see favorietjes) gets 1.5&times; as many.",
                 "Elke guh die je temt begint een <b>hartjesmeter</b> met jou (guhs die je al had tellen ook mee). Alles wat je samen doet vult hem: geef hem een "
                 "snackje, tik hem aan voor een <i>aaitje</i>, geef hem een dikke <b>Knuffelen!</b> uit zijn menu, speel, reis, laat hem klusjes doen. Er zweven "
                 "roze hartjes omhoog en boven je hotbar staat <i>+2 &hearts;</i>. <b>Hartjes gaan nooit omlaag</b>, nooit: geen straf, geen honger, geen gemok. "
                 "Per bron is er een klein daglimiet, zodat een guh niet verwend raakt (njeg), en een <b>blije</b> guh (zie favorietjes) krijgt er 1,5&times; zoveel.") +
               table([("Level", "Niveau"), ("Hearts", "Hartjes"), ("You unlock", "Je krijgt")], [
                   ["<b>lieve vadsjes van elkaar</b>", "100", ("the <b>hartjesspeldje</b> (a heart pin for the ear) and the emote <b>Hartjes</b>",
                                                            "het <b>hartjesspeldje</b> (een hartjesspeld voor aan het oor) en de emote <b>Hartjes</b>")],
                   ["<b>mega lieve vadsjes van elkaar</b>", "600", ("the <b>knuffeltruitje</b> and the emote <b>Knuffeldansje</b>",
                                                                  "het <b>knuffeltruitje</b> en de emote <b>Knuffeldansje</b>")],
                   ["<b>zielsguh bff 5evr &lt;3</b>", "2000", ("the <b>zielskroontje</b>, the exclusive <b>gouden hartjes-halsbandje</b> and the <b>Bff-knuffel</b>",
                                                             "het <b>zielskroontje</b>, het exclusieve <b>gouden hartjes-halsbandje</b> en de <b>Bff-knuffel</b>")]]) +
               p("From left to right: hartjesspeldje, knuffeltruitje, zielskroontje with the gouden hartjes-halsbandje. The clothes are unlocked for all your guhs "
                 "(Kleding source <i>Hartjes met je guh</i>); the emotes are unlocked in the Emotes menu of every guh.",
                 "Van links naar rechts: hartjesspeldje, knuffeltruitje, zielskroontje met het gouden hartjes-halsbandje. De kleertjes zijn ontgrendeld voor al je "
                 "guhs (Kleding-bron <i>Hartjes met je guh</i>); de emotes gaan open in het Emotes-menu van elke guh.")) + \
        entry(img("band_hartjes", "Band hearts"), "Where hearts come from", "Waar hartjes vandaan komen",
              table([("What", "Wat"), ("Hearts", "Hartjes"), ("Max per day", "Max per dag")], [[(en, nl), n, m] for en, nl, n, m in HEARTS]) +
              p("Snacks are 42 treats: kaas knabbels, cupcakes, macarons, kaasijsjes, koekjes, tompoucen, vadsdonuts, sweet berries, an apple... (only you, the "
                "owner, can feed your guh). A level-up gives a big heart, a jingle and a line in the chat; when you were offline, you hear it at your next login.",
                "Snackjes zijn 42 lekkernijen: kaasknabbels, cupcakes, macarons, kaasijsjes, koekjes, tompoucen, vadsdonuts, zoete bessen, een appel... (alleen "
                "jij, de baas, kunt je guh voeren). Een nieuw niveau geeft een groot hart, een deuntje en een regel in de chat; was je offline, dan hoor je het bij "
                "je volgende login.")) + \
        gallery(fig("kleding_samen_hartjesspeldje", "Hartjesspeldje", "Hartjesspeldje", "Lieve vadsjes van elkaar (100).", "Lieve vadsjes van elkaar (100)."),
                fig("kleding_samen_knuffeltruitje", "Knuffeltruitje", "Knuffeltruitje", "Mega lieve vadsjes van elkaar (600).", "Mega lieve vadsjes van elkaar (600)."),
                fig("kleding_samen_zielskroontje", "Zielskroontje", "Zielskroontje", "Zielsguh bff 5evr &lt;3 (2000).", "Zielsguh bff 5evr &lt;3 (2000)."),
                fig("kleding_gouden_hartjeshalsbandje", "Gouden hartjes-halsbandje", "Gouden hartjes-halsbandje",
                    "The prettiest collar there is, only for a zielsguh.", "Het allermooiste halsbandje dat er is, alleen voor een zielsguh."))
    ziel = h3("The zielsguh", "De zielsguh") + \
        gallery(fig("shot210_samen_zielsguh_naam", "A heart after its name", "Een hartje achter zijn naam",
                    "A zielsguh has a twinkling pink heart after its name and now and then sparkling hearts float off it.",
                    "Een zielsguh heeft een fonkelend roze hartje achter zijn naam, en af en toe zweven er glinsterhartjes van hem af."),
                fig("shot210_samen_bff_knuffel", "The bff-knuffel", "De bff-knuffel",
                    "Pick <i>Bff-knuffel</i> in the Emotes menu while you stand close: your guh stands on its hind legs, hops in front of you and you hug, with a big beating heart above you both.",
                    "Kies <i>Bff-knuffel</i> in het Emotes-menu terwijl je vlakbij staat: je guh gaat op zijn achterpootjes staan, huppelt voor je en jullie knuffelen, met een groot kloppend hart boven jullie."),
                fig("shot210_samen_kleding_dichtbij", "Crown and golden collar", "Kroontje en gouden halsbandje",
                    "The zielskroontje and the gouden hartjes-halsbandje together. VAHOEG!", "Het zielskroontje en het gouden hartjes-halsbandje samen. VAHOEG!"))
    emotes = h3("Four new emotes", "Vier nieuwe emotes") + \
        entry(shot("gui_samen_emotes_opslot", "The emote picker with locked emotes"), "Hartjes, Knuffeldansje, Bff-knuffel and Verdrietje",
              "Hartjes, Knuffeldansje, Bff-knuffel en Verdrietje",
              ul([("<b>Hartjes</b>: blows little hearts at you, paw at its snoet. <i>Lieve vadsjes van elkaar!</i>",
                   "<b>Hartjes</b>: blaast kleine hartjes naar je toe, pootje bij de snoet. <i>Lieve vadsjes van elkaar!</i>"),
                  ("<b>Knuffeldansje</b>: a happy twirl with little hops. Mega lief and mega vadsig!", "<b>Knuffeldansje</b>: een blij rondjesdansje met huppeltjes. Mega lief en mega vadsig!"),
                  ("<b>Bff-knuffel</b>: stands on its hind legs and hugs you as hard as it can.", "<b>Bff-knuffel</b>: gaat op zijn achterpootjes staan en knuffelt je zo hard als hij kan."),
                  ("<b>Verdrietje</b>: ears down, a tiny sigh... ooh njeg. Luckily a cuddle always helps. (Never cross, only a little sad.)",
                   "<b>Verdrietje</b>: oortjes omlaag, een klein zuchtje... ooh njeg. Gelukkig helpt een knuffel altijd. (Nooit boos, alleen een beetje verdrietig.)")]) +
              p("The Emotes menu now has two columns. The first three are locked until one of your guhs reaches their level: they are grey with a little heart and say which level opens them.",
                "Het Emotes-menu heeft nu twee kolommen. De eerste drie zitten op slot tot een van je guhs hun niveau haalt: ze zijn grijs met een hartje en zeggen welk niveau ze opent."))

    # ---------------------------------------------------------------- B: favorietjes
    FAVS = [("Lievelingshapje", "a snack (feed it all kinds)", "een snackje (voer hem van alles)"),
            ("Lievelingsplek", "a biome (take it travelling)", "een bioom (neem hem mee op reis)"),
            ("Lievelingsknuffel", "a plushie from the grijpmachine (put plushies near it)", "een knuffel uit de grijpmachine (zet knuffels bij hem neer)"),
            ("Lievelingsliedje", "a koortje song (xylofoon) or a disco song", "een koortjesliedje (xylofoon) of een discoliedje"),
            ("Lievelingsspeeltje", "one of the four toys", "een van de vier soorten speelgoed"),
            ("Lievelingsemote", "one of its emotes", "een van zijn emotes"),
            ("Lievelingskleur", "a clothing colour (12 colours: roze, rood, oranje, geel, groen, mint, blauw, paars, wit, zwart, bruin, goud)",
             "een kleur kleertje (12 kleuren: roze, rood, oranje, geel, groen, mint, blauw, paars, wit, zwart, bruin, goud)"),
            ("Beste guh-vriendje", "one of your other guhs", "een van je andere guhs")]
    fav = h3("B &middot; Favorietjes", "B &middot; Favorietjes") + \
        wentry(shot("favorietjes_explosie", "A heart explosion"), "Eight secrets per guh", "Acht geheimpjes per guh",
               p("Every guh has <b>eight secret favourites</b>, the same for as long as it lives. You find them by <b>trying things</b> while you're close by. "
                 "Is it the right one? A big <b>heart explosion</b> with golden sparkles, a happy dance, <b>50 hearts</b>, a line in its dagboekje and in the chat "
                 "(<i>Njeg!! Guh smult en smakt en straalt: Cupcake is zijn LIEVELINGSHAPJE! VAHOEG!</i>), and it is <b>blij</b> for five minutes. Every time after "
                 "that it gets a few hearts and a little heart burst again.",
                 "Elke guh heeft <b>acht geheime favorietjes</b>, die hij zijn hele leven houdt. Je vindt ze door <b>dingen te proberen</b> terwijl je in de buurt "
                 "bent. Is het de goeie? Een grote <b>hartjesexplosie</b> met gouden glinsters, een blij dansje, <b>50 hartjes</b>, een regel in zijn dagboekje en "
                 "in de chat (<i>Njeg!! Guh smult en smakt en straalt: Cupcake is zijn LIEVELINGSHAPJE! VAHOEG!</i>), en hij is vijf minuten <b>blij</b>. Elke "
                 "keer daarna krijgt hij weer wat hartjes en een klein hartjeswolkje.") +
               table([("Favourite", "Favorietje"), ("What it can be", "Wat het kan zijn")], [[f"<b>{n}</b>", (en, nl)] for n, en, nl in FAVS])) + \
        entry(shot("favorietjes_hints", "Warm and cold hints"), "Warm or cold?", "Warm of koud?",
              p("Not the favourite yet? Then you get a <b>hint</b> above your hotbar. <b>Warm</b> (close: the same kind of snack, a biome of the same family, a "
                "neighbouring colour, a plush of the same group...): <i>Guh kijkt nieuwsgierig...</i> with a pink bobbing <b>?</b>. <b>Cold</b>: <i>Guh snuffelt... "
                "njeg?</i> with little sniff puffs at its snoet. A hint is never a discovery, and when nobody is watching the guh is just happy.",
                "Nog niet het favorietje? Dan krijg je een <b>hint</b> boven je hotbar. <b>Warm</b> (dichtbij: hetzelfde soort snackje, een bioom uit dezelfde "
                "familie, een kleur ernaast, een knuffel uit hetzelfde groepje...): <i>Guh kijkt nieuwsgierig...</i> met een roze dobberend <b>?</b>. <b>Koud</b>: "
                "<i>Guh snuffelt... njeg?</i> met snuffelwolkjes bij zijn snoet. Een hint is nooit een ontdekking, en als niemand kijkt is de guh gewoon blij.")) + \
        h3("Blij!", "Blij!") +         ul([("A <b>blij</b> guh gets 1.5&times; hearts and does its chores 1.5&times; as fast.", "Een <b>blije</b> guh krijgt 1,5&times; hartjes en doet zijn klusjes 1,5&times; zo snel."),
            ("Standing in its favourite biome, wearing its favourite colour or next to its best friend keeps it blij.",
             "In zijn lievelingsbioom staan, zijn lievelingskleur dragen of naast zijn beste vriendje zijn houdt hem blij."),
            ("In a minigame a blij guh <b>cheers louder</b> and hums its <b>favourite song</b>; at a record it sings eight notes of it out loud.",
             "Bij een minigame <b>juicht</b> een blije guh <b>harder</b> en neuriet hij zijn <b>lievelingsliedje</b>; bij een record zingt hij er acht noten van hardop.")])

    # ---------------------------------------------------------------- C: the Guhhuisje
    huisje = h3("C &middot; Het Guhhuisje", "C &middot; Het Guhhuisje") + \
        wentry(img("guhhuisjes", "The three Guhhuisjes"), "A little house shaped like a guh head", "Een huisje in de vorm van een guhhoofd",
               p("A <b>Guhhuisje</b> is a guh head you can live in: the round <b>ears are the roof</b> (with pink insides and a pluis tuft), the two big shiny "
                 "<b>eyes are the windows</b> and the <b>snoet is the door</b> (an arched door with a heart knob, a little nose and blush). It is guh-style through "
                 "and through, not a hamster house! There are three sizes: <b>klein</b> (2&times;2&times;2, 3 residents), <b>medium</b> (3&times;3&times;3, 5 "
                 "residents, flower boxes under the eyes) and <b>groot</b> (4&times;4&times;4, 8 residents, a pink chimney and a bow on its ear: Villa Vahoeg!). "
                 "You craft the next size from the one before.",
                 "Een <b>Guhhuisje</b> is een guhhoofd waar je in kunt wonen: de ronde <b>oortjes zijn het dak</b> (roze vanbinnen, met een plukje pluis), de twee "
                 "grote glimmende <b>ogen zijn de raampjes</b> en de <b>snoet is de deur</b> (een boogdeurtje met een hartjesknop, een neusje en blosjes). Helemaal "
                 "guh-stijl, geen hamsterhuisje! Er zijn drie maten: <b>klein</b> (2&times;2&times;2, 3 bewoners), <b>medium</b> (3&times;3&times;3, 5 bewoners, "
                 "bloembakjes onder de oogjes) en <b>groot</b> (4&times;4&times;4, 8 bewoners, een roze schoorsteentje en een strik op het oor: Villa Vahoeg!). "
                 "De volgende maat maak je van de vorige."),
               stats=[(("Residents", "Bewoners"), "3 / 5 / 8"), (("Home base", "Thuisgebied"), t("16 blocks", "16 blokken")),
                      (("Who", "Wie"), t("tamed guhs, pieppiepmuisjes, Schilly, Poepschilly", "tamme guhs, pieppiepmuisjes, Schilly, Poepschilly"))]) + \
        gallery(fig("guhhuisje_klein", "Klein", "Klein", "Room for 3. Knus!", "Plek voor 3. Knus!"),
                fig("guhhuisje_medium", "Medium", "Medium", "Room for 5, with flower boxes.", "Plek voor 5, met bloembakjes."),
                fig("guhhuisje_groot", "Groot", "Groot", "Room for 8, with a chimney and a bow.", "Plek voor 8, met schoorsteentje en strik.")) + \
        ul([("<b>Moving in</b>: right-click the huisje with a picked-up guh or maatje, or press <i>Nieuwe bewoner</i> in its screen (your own tamed guhs and "
             "maatjes within 16 blocks). Every huisje gets its own <b>unique cute name</b> (<i>Het Warme Snoetje</i>, <i>Het Pluisnestje</i>...), and you can rename it.",
             "<b>Erin wonen</b>: rechtsklik het huisje met een opgepakte guh of een maatje, of druk op <i>Nieuwe bewoner</i> in zijn scherm (je eigen tamme guhs en "
             "maatjes binnen 16 blokken). Elk huisje krijgt een eigen <b>unieke lieve naam</b> (<i>Het Warme Snoetje</i>, <i>Het Pluisnestje</i>...), en je kunt het hernoemen."),
            ("<b>Home base</b>: residents stay within about 16 blocks of their huisje: they wander, do their chores and play there, and don't teleport after you.",
             "<b>Thuisgebied</b>: bewoners blijven zo'n 16 blokken rond hun huisje: daar lopen ze rond, doen ze hun klusjes en spelen ze, en ze teleporteren niet achter je aan."),
            ("<b>Night</b>: they go inside and sleep (zzz, a little snoring, and friends show hearts at the windows). In the <b>morning</b> they come out of the door with a big yawn.",
             "<b>'s Nachts</b> gaan ze naar binnen en slapen ze (zzz, een beetje gesnurk, en vriendjes laten hartjes zien bij de raampjes). 's <b>Ochtends</b> komen ze met een grote gaap de deur uit."),
            ("<b>Where the spoils go</b>: a <b>Bank Guh</b> in the area sorts everything; otherwise a chest next to the huisje; otherwise they go in front of the door.",
             "<b>Waar de buit heen gaat</b>: een <b>Bank Guh</b> in de buurt sorteert alles; anders een kist naast het huisje; anders komt het voor de deur te liggen."),
            ("Babies and sitting guhs don't do chores; they just live there and play. Break the huisje and the residents are free again (nothing is lost).",
             "Baby's en zittende guhs doen geen klusjes; ze wonen er gewoon gezellig en spelen. Breek je het huisje af, dan zijn de bewoners weer vrij (er gaat niks verloren).")]) + \
        gallery(fig("shot210_huisje_groot", "Groot, with residents", "Groot, met bewoners", "A resident walks home.", "Een bewoner loopt naar huis."),
                fig("shot210_huisje_groot_nacht", "At night", "'s Nachts", "Everyone inside, lights in the eyes. Zzz...", "Iedereen binnen, licht in de oogjes. Zzz..."),
                fig("shot210_huisjes_voor", "Three sizes", "Drie maten", "Klein, medium and groot in a row.", "Klein, medium en groot op een rij."))
    scherm = h3("The huisje screen and the klus-area", "Het huisjesscherm en de klus-area") + \
        wentry(shot("gui_huisje_klusjes", "The huisje screen"), "Right-click your huisje", "Rechtsklik je huisje",
               ul([("On top: the size, the number of residents, the <b>name</b> with <i>Hernoem</i>, and where the chore spoils go.",
                    "Bovenaan: de maat, het aantal bewoners, de <b>naam</b> met <i>Hernoem</i>, en waar de spulletjes van klusjes heen gaan."),
                   ("Left: the <b>residents</b>, each with a mini render and its hearts level (<i>slaapt</i> when it's asleep inside).",
                    "Links: de <b>bewoners</b>, elk met een mini-plaatje en zijn hartjesniveau (<i>slaapt</i> als hij binnen ligt te slapen)."),
                   ("Right: the <b>chores of the chosen resident</b>, each with an <b>aan / uit</b> toggle and a tip about what it needs nearby. Chores it can't do say <i>kan niet</i>.",
                    "Rechts: de <b>klusjes van de gekozen bewoner</b>, elk met een <b>aan / uit</b>-knopje en een tip over wat er in de buurt nodig is. Klusjes die hij niet kan zeggen <i>kan niet</i>."),
                   ("Buttons: <i>Nieuwe bewoner</i>, <i>Uit huis</i> (moves the chosen resident out) and <b>Klus-area: aan/uit</b>.",
                    "Knoppen: <i>Nieuwe bewoner</i>, <i>Uit huis</i> (de gekozen bewoner verhuist eruit) en <b>Klus-area: aan/uit</b>.")])) + \
        gallery(fig("shot210_huisjes_koepel", "Laat klus-area zien", "Laat klus-area zien",
                    "The toggle shows a see-through <b>blue dome</b> over the area where the residents wander, do chores and play.",
                    "Het knopje laat een doorzichtige <b>blauwe koepel</b> zien over het gebied waar de bewoners rondlopen, klusjes doen en spelen."),
                fig("shot210_gui_huisje_scherm", "Another huisje", "Een ander huisje", "Het Pluisnestje, with a pieppiepmuisje living in it too.",
                    "Het Pluisnestje, waar ook een pieppiepmuisje woont.")) + \
        p("<i>Since 3.0 the three recipes need the <b>Bouwboekje van de Timmerguh</b> (it stays in the grid): do his questline <i>Samen een huisje bouwen</i> "
          "in a Knuffeldal town first. Huisjes you already placed keep working. See <a href='#new30'>New in 3.0</a>.</i>",
          "<i>Sinds 3.0 hebben de drie recepten het <b>Bouwboekje van de Timmerguh</b> nodig (het blijft in het rooster liggen): doe eerst zijn questline "
          "<i>Samen een huisje bouwen</i> in een Knuffeldal-stadje. Huisjes die al staan blijven gewoon werken. Zie <a href='#new30'>Nieuw in 3.0</a>.</i>") + \
        '<div class="recipes">' + recipe_card("guhhuisje_klein", "Guhhuisje (klein)", "Guhhuisje (klein)") + \
        recipe_card("guhhuisje_medium", "Guhhuisje (medium)", "Guhhuisje (medium)") + recipe_card("guhhuisje_groot", "Guhhuisje (groot)", "Guhhuisje (groot)") + '</div>' + \
        entry(icon("wilde_guhweerder", "Wilde-guhweerder").replace('class="px"', 'class="px" style="width:80px;height:80px"'),
              "Wilde-guhweerder", "Wilde-guhweerder",
              p("<i>New in 1.2.0.</i> A little pink sign with guh ears and a sleeping guh face: <i>a chonky lives here already</i>. Put it in your "
                "house or base and no <b>wild</b> guhs pop up in the area around it (8, 16, 24, 32 or 48 blocks, also a bit above and below). Still "
                "lief: the wild guhs see the sign and go cuddle somewhere else. Your own guhs, babies and guhs from a spawn egg are welcome, and guhs "
                "that are already there stay. Right-click: choose the size and <b>Show the Area</b> (the same blue dome as the huisje's "
                "chore area). Only the one who placed it can change or break it.",
                "<i>Nieuw in 1.2.0.</i> Een klein roze bordje met guhoortjes en een slapend guhgezichtje: <i>hier woont al een vadsje</i>. Zet hem in "
                "je huis of basis en er komen geen <b>wilde</b> guhs meer tevoorschijn in de area eromheen (8, 16, 24, 32 of 48 blokken, ook een "
                "stukje erboven en eronder). Nog steeds lief: de wilde guhs zien het bordje en gaan ergens anders knuffelen. Je eigen guhs, "
                "baby'tjes en guhs uit een spawn-ei zijn welkom, en guhs die er al zijn blijven. Rechtsklik: kies de grootte en <b>Laat de area "
                "zien</b> (dezelfde blauwe koepel als de klus-area van het huisje). Alleen wie hem neerzette kan hem veranderen of weghalen.")) + \
        '<div class="recipes">' + recipe_card("wilde_guhweerder", "Wilde-guhweerder", "Wilde-guhweerder") + '</div>'

    # ---------------------------------------------------------------- D: the ten klusjes
    KLUS = [("Kaasknabbels opgraven", "guh, muisje",
             "Digs in grass, dirt or sand in the area (the ground stays whole): mostly kaasknabbels, seeds or a schelpje, and sometimes something <b>rare</b> (gefrituurde knabbels, a marshmallow knabbel, a guh crystal, vahoege vads, the music disc).",
             "Graaft in gras, aarde of zand in de klus-area (de grond blijft heel): meestal kaasknabbels, zaadjes of een schelpje, en soms iets <b>zeldzaams</b> (gefrituurde knabbels, een marshmallowknabbel, een guhkristal, vahoege vads, de muziekplaat)."),
            ("Farmen", "guh",
             "Harvests ripe crops and replants them at once (wheat, carrots, potatoes, beetroot, kaasknabbelplantjes) and ripe guhtuintjes, up to 5 a trip. Nothing ripe? It waters thirsty tuintjes.",
             "Oogst rijpe gewassen en plant ze meteen opnieuw (tarwe, wortels, aardappels, bieten, kaasknabbelplantjes) en rijpe guhtuintjes, tot 5 per rondje. Niks rijp? Dan geeft hij dorstige tuintjes water."),
            ("Opruimen &amp; sorteren", "guh, muisje",
             "Picks up things lying around and brings them to the chest; with a <b>Bank Guh</b> in the area everything (also what's in the chest) gets sorted. Needs a chest or a Bank Guh.",
             "Raapt spulletjes op die rondslingeren en brengt ze naar de kist; staat er een <b>Bank Guh</b> in de klus-area, dan wordt alles gesorteerd (ook wat in de kist ligt). Nodig: een kist of een Bank Guh."),
            ("Dieren &amp; bijen verzorgen", "guh",
             "Pets and feeds guhschaapjes, knabbelkippetjes and guhkoeien (knabbelvoer from the chest), picks up wool and eggs, empties the kippennestjes, milks a happy guhkoe (kaasmelk, with an empty bottle) and harvests full knabbelkorven.",
             "Aait en voert guhschaapjes, knabbelkippetjes en guhkoeien (knabbelvoer uit de kist), raapt wol en eitjes op, haalt de kippennestjes leeg, melkt een blije guhkoe (kaasmelk, met een leeg flesje) en oogst volle knabbelkorven."),
            ("Bakken &amp; molen", "guh",
             "Carries knabbelgraan from the chest to a guh-molentje and the knabbelmeel back, and bakes in a knabbeloven when the chest has everything for a recipe (with knabbelmeel: twice as much).",
             "Brengt knabbelgraan uit de kist naar een guh-molentje en het knabbelmeel terug, en bakt in een knabbeloven als de kist alles voor een recept heeft (met knabbelmeel: dubbel zoveel)."),
            ("Vissen", "guh, Schilly, Poepschilly",
             "With a little pond (3+ water blocks) in the area it sits at the bank and catches guhvissen and <b>schelpjes</b>, and very rarely a <b>Gouden Guhvis</b>.",
             "Met een vijvertje (3+ waterblokjes) in de klus-area zit hij aan de kant en vangt guhvissen en <b>schelpjes</b>, en heel soms een <b>Gouden Guhvis</b>."),
            ("Wachten &amp; waarschuwen", "guh (muisjes only peep)",
             "A Mika or monster near the huisje? It peeps (a pink !), runs to you and warns you, and <b>gently pushes Mikas away</b>. It never fights and never hurts anyone. Always lief!",
             "Een Mika of monster bij het huisje? Hij piept (een roze !), rent naar je toe en waarschuwt je, en <b>duwt Mika's heel zachtjes weg</b>. Nooit vechten, nooit pijn doen. Altijd lief!"),
            ("Bloemetjes &amp; bessen plukken", "guh, muisje",
             "Picks knabbelbessen and sweet berries (the bush stays) and a bloom from kaasbloemen and roze guhbloemen (the flower stays), and sometimes plants a new little flower.",
             "Plukt knabbelbessen en zoete bessen (de struik blijft) en een bloemetje van kaasbloemen en roze guhbloemen (de bloem blijft staan), en plant soms een nieuw bloemetje."),
            ("Lampjes aan &amp; uit", "guh",
             "In the evening it switches the guhlampjes and candles in the area on, in a little round; in the morning off again, with a big yawn.",
             "'s Avonds doet hij in een rondje de guhlampjes en kaarsjes in de klus-area aan; 's ochtends weer uit, met een grote gaap."),
            ("Muisje-oppas &amp; verzorgen", "guh",
             "Cuddles and feeds pieppiepmuisjes, Schilly and Poepschilly nearby, and a hurt tamed guh gets a snack from the chest (cupcake, koekje, kaasknabbels...) from a friend.",
             "Knuffelt en voert pieppiepmuisjes, Schilly en Poepschilly in de buurt, en een gewonde tamme guh krijgt van een vriendje een snackje uit de kist (cupcake, koekje, kaasknabbels...).")]
    klusjes = h3("D &middot; Tien klusjes", "D &middot; Tien klusjes") + \
        p("Residents do little chores around their huisje all by themselves, one after the other (with a tiny tool or what they carry floating above their "
          "head). Every chore gives hearts, a line in the dagboekje and a small sparkle. You choose per resident which chores it does in the huisje screen.",
          "Bewoners doen helemaal zelf kleine klusjes rond hun huisje, de een na de ander (met een klein gereedschapje of wat ze dragen zwevend boven hun "
          "kopje). Elk klusje geeft hartjes, een regel in het dagboekje en een klein glinstertje. In het huisjesscherm kies je per bewoner welke klusjes hij doet.") + \
        table([("Chore", "Klusje"), ("Who", "Wie"), ("What it does", "Wat het doet")],
              [[f"<b>{i}. {n}</b>", who.replace("(muisjes only peep)", t("(muisjes only peep)", "(muisjes piepen alleen)")), (en, nl)]
               for i, (n, who, en, nl) in enumerate(KLUS, 1)]) + \
        gallery(fig("shot210_klusjes_erf_bezig", "A busy yard", "Een druk erfje", "Wheat, a pond, tuintjes, animals, a chest and a Bank Guh: everyone at work.",
                    "Tarwe, een vijver, tuintjes, dieren, een kist en een Bank Guh: iedereen aan het werk."),
                fig("shot210_klusjes_lampjes_avond", "Lampjes in the evening", "Lampjes 's avonds", "The guhlampjes go on one by one.", "De guhlampjes gaan een voor een aan."),
                fig("klusjes_guhlampje", "Guhlampje", "Guhlampje",
                    "A standing lamp with guh ears, a pluis tuft and a glowing snoet face (asleep when off). Right-click: on or off. 2 per craft.",
                    "Een staande lamp met guhoortjes, een plukje pluis en een gloeiend snoetgezichtje (slapend als hij uit is). Rechtsklik: aan of uit. 2 per keer.")) + \
        entry(icon("klusjes_schelpje", "Schelpje").replace('class="px"', 'class="px" style="width:80px;height:80px"'), "Schelpje", "Schelpje",
              p("Fished up by a huisje guh. Hold it to your ear (right-click) and listen to the Guhzee: <i>Ruisss... je hoort een guh snurken. O nee, dat is je eigen guh.</i> "
                "Crafts into 2 bone meal, and two make pink dye.",
                "Opgevist door een huisjesguh. Houd hem aan je oor (rechtsklik) en luister naar de Guhzee: <i>Ruisss... je hoort een guh snurken. O nee, dat is je eigen guh.</i> "
                "Wordt 2 beendermeel, en van twee maak je roze kleurstof.")) + \
        '<div class="recipes">' + recipe_card("klusjes_guhlampje", "Guhlampje", "Guhlampje") + '</div>'

    # ---------------------------------------------------------------- E: toys
    speel = h3("E &middot; Speelgoed", "E &middot; Speelgoed") + \
        wentry(shot("speelgoed_spelen2", "Guhs playing"), "A guh playground", "Een guhspeeltuin",
               p("Put toys down and your tamed guhs play with them now and then when you are near (and huisje residents also on their own, when a toy is in "
                 "their home base, and so do guests in the Guhkamer). Playing gives hearts, and afterwards a guh often does a happy emote. All toys are "
                 "guh-style: pink fur, round ears, guh eyes and a snoet.",
                 "Zet speelgoed neer en je tamme guhs spelen er af en toe mee als jij in de buurt bent (en huisjesbewoners ook vanzelf, als het speelgoed in hun "
                 "thuisgebied staat, en logés in de Guhkamer ook). Spelen geeft hartjes, en daarna doet een guh vaak een blije emote. Al het speelgoed is guh-stijl: "
                 "roze vacht, ronde oortjes, guhogen en een snoet.")) + \
        gallery(fig("icon_knabbelbal", "Knabbelbal", "Knabbelbal",
                    "A fluffy ball with guh ears and a kaasknabbel in its tummy window. Guhs nudge it with their snoet until the knabbel pops out, then eat it. <b>Left-click</b> = kick (your guhs chase it), right-click = nudge, with kaas knabbels = refill, sneak + right-click = pick up.",
                    "Een pluizige bal met guhoortjes en een kaasknabbel achter zijn buikraampje. Guhs duwen hem met hun snoet tot de knabbel eruit ploept, en eten hem op. <b>Linksklik</b> = schoppen (je guhs rennen erachteraan), rechtsklik = duwtje, met kaasknabbels = bijvullen, sluip + rechtsklik = oppakken."),
                fig("guh_glijbaantje", "Guh-glijbaantje &amp; klimrek", "Guh-glijbaantje &amp; klimrek",
                    "Climb the mint ladder, over the platform on two guh faces, and <i>wieee!</i> down the shiny slide. Guhs do a few laps; right-click to slide yourself.",
                    "Klim de mintgroene ladder op, over het platform op twee guhsnoetjes, en <i>wieee!</i> van de glimmende glijbaan. Guhs doen een paar rondjes; rechtsklik om zelf te glijden."),
                fig("pluizige_tunnel", "Pluizige tunnel", "Pluizige tunnel",
                    "Soft pink fur tunnel pieces join up by themselves (straight, bends, T, crossings), every entrance a guh face. Guhs scurry through and <b>hide</b>; knock (right-click) to find them, or a tame muisje comes looking. 4 per craft.",
                    "Zachte roze vachttunnelstukjes plakken vanzelf aan elkaar (recht, bochten, T, kruising), elke ingang een guhsnoetje. Guhs rennen erdoor en <b>verstoppen</b> zich; klop (rechtsklik) om ze te vinden, of een tam muisje komt zoeken. 4 per keer."),
                fig("guh_wip", "Guh-wip", "Guh-wip",
                    "A plank on a little guh head with cushions and ear handles: two guhs seesaw together (friends love it). Right-click = a push, sneak + right-click = ride yourself.",
                    "Een plank op een guhhoofdje met kussentjes en oorhandvatjes: twee guhs wippen samen (vriendjes vinden dat het leukst). Rechtsklik = duwtje, sluip + rechtsklik = zelf wippen."),
                fig("guh_schommel", "Guh-schommel", "Guh-schommel",
                    "A pink swing with a guh head on the beam (the ropes come out of its chin). Push for higher, higher! (your push gives a heart), or sneak + right-click to swing yourself.",
                    "Een roze schommel met een guhhoofd op de balk (de touwtjes komen uit zijn kin). Duw hem hoger, hoger! (jouw duwtje geeft een hartje), of sluip + rechtsklik om zelf te schommelen."),
                fig("shot210_speelgoed_tunnel", "In the tunnel", "In de tunnel", "Verstoppertje! Where did it go?", "Verstoppertje! Waar is hij gebleven?")) + \
        '<div class="recipes">' + "".join(recipe_card(r, en, nl) for r, en, nl in (
            ("knabbelbal", "Knabbelbal", "Knabbelbal"), ("guh_glijbaantje", "Guh-glijbaantje", "Guh-glijbaantje"), ("pluizige_tunnel", "Pluizige tunnel", "Pluizige tunnel"),
            ("guh_wip", "Guh-wip", "Guh-wip"), ("guh_schommel", "Guh-schommel", "Guh-schommel"))) + '</div>'

    # ---------------------------------------------------------------- F: samen
    samen = h3("F &middot; Samen", "F &middot; Samen") + \
        wentry(shot("samen_juichen", "A guh cheering"), "Cheering in the minigames", "Juichen bij de minigames",
               p("Play a minigame with your own guh nearby (within 24 blocks) and it plays along: a <b>good</b> throw, catch or time gives a VAHOEG jump with little "
                 "hearts and a cheer; a <b>miss</b> gives a lovingly sad <i>ooh njeg...</i> (never cross!); a <b>record</b> gets the <b>knuffeldansje</b>. At the "
                 "start it waves <i>succes!</i>, and every game together gives hearts and a dagboekje line (all 23 minigames).",
                 "Speel een minigame met je eigen guh in de buurt (binnen 24 blokken) en hij speelt mee: een <b>goede</b> worp, vangst of tijd geeft een VAHOEG-sprongetje "
                 "met hartjes en gejuich; <b>mis</b> geeft een lief verdrietig <i>ooh njeg...</i> (nooit boos!); een <b>record</b> krijgt het <b>knuffeldansje</b>. Bij "
                 "de start zwaait hij <i>succes!</i>, en elk spelletje samen geeft hartjes en een regel in het dagboekje (alle 23 minigames).")) + \
        gallery(fig("shot210_samen_verdrietje", "Ooh njeg...", "Ooh njeg...", "A miss: ears down, a tiny tear. A cuddle helps.", "Mis: oortjes omlaag, een klein traantje. Een knuffel helpt."),
                fig("shot210_samen_record_dans", "A record!", "Een record!", "The knuffeldansje, with hearts and notes.", "Het knuffeldansje, met hartjes en noten."),
                fig("shot210_samen_kart_achter", "Achterop!", "Achterop!",
                    "In the <b>race</b> and on the <b>Guh-Circuit</b> your guh (the one with the most hearts) hops on the back of the kart, cheers every lap and goes back where it was afterwards.",
                    "Bij de <b>race</b> en op het <b>Guh-Circuit</b> springt je guh (die met de meeste hartjes) achterop de kart, juicht elke ronde en gaat daarna terug naar waar hij was.")) + \
        ul([("<b>Really joining in</b>: it skates next to you on the <b>Elf-Guhjestocht</b> (snow sprays!), and jumps into the <b>Knuffelbad</b> to swim beside you "
             "(and cheers from the side while you go down a slide). At sjoelen, the katapult, golf and the rest it cheers.",
             "<b>Echt meedoen</b>: hij schaatst naast je op de <b>Elf-Guhjestocht</b> (sneeuwspetters!), en springt in het <b>Knuffelbad</b> om naast je te zwemmen "
             "(en juicht vanaf de kant als jij van een glijbaan gaat). Bij sjoelen, de katapult, golf en de rest juicht hij."),
            ("<b>Welcome home</b>: back after a long time? A welcome-back dance. <b>After dying</b>: the guh with the most hearts waits at your respawn with a "
             "comfort hug. <b>Thunder</b>: it walks to you and cuddles up close. <b>Going to bed</b>: your guhs wave you goodnight.",
             "<b>Welkom thuis</b>: na lange tijd terug? Een welkom-terug-dansje. <b>Na doodgaan</b>: de guh met de meeste hartjes wacht bij je respawn met een "
             "troostknuffel. <b>Onweer</b>: hij loopt naar je toe en kruipt dicht tegen je aan. <b>Naar bed</b>: je guhs zwaaien je welterusten.")]) + \
        wentry(shot("speelgoed_spelen3", "Guhs playing together"), "Guh friendships", "Guh-vriendschappen",
               p("Tamed guhs of yours that spend a lot of time together become <b>vriendjes</b>, and later <b>beste vriendjes</b> (a real bff duo). Friends walk "
                 "together, cuddle each other (hearts between them), seesaw and swing together on the wip and schommel, and sleep side by side in the same "
                 "huisje. Being close, cuddling, playing and a night together all count; it only ever grows. The Mijn guhs page shows each guh's friends, "
                 "with a &#9733; for the bestie.",
                 "Tamme guhs van jou die veel samen zijn worden <b>vriendjes</b>, en later <b>beste vriendjes</b> (een echt bff-duo). Vriendjes lopen samen, "
                 "knuffelen elkaar (hartjes ertussen), wippen en schommelen samen op de wip en de schommel, en slapen naast elkaar in hetzelfde huisje. Dichtbij "
                 "zijn, knuffelen, spelen en een nachtje samen tellen allemaal mee; het groeit alleen maar. De Mijn guhs-pagina laat de vriendjes van elke guh "
                 "zien, met een &#9733; voor de bestie."))

    # ---------------------------------------------------------------- G: Guhdex Mijn guhs
    PLEK = [("walking around / sitting and waiting", "loopt rond / zit braaf te wachten"), ("in (or asleep in) a named Guhhuisje", "in (of slapend in) een Guhhuisje met naam"),
            ("in your pockets, a chest, a backpack, a Bank Guh or as a parcel on the ground", "in je zakken, een kist, een rugzak, een Bank Guh of als pakketje op de grond"),
            ("running in a Guh Wheel, riding or being ridden, on a shoulder", "rondjes in een Guh Wheel, rijden of bereden worden, op een schouder"),
            ("in the Guhkamer, or inside another guh for a poetsbeurt", "in de Guhkamer, of in een andere guh voor een poetsbeurt")]
    dex = h3("G &middot; De Guhdex: Mijn guhs", "G &middot; De Guhdex: Mijn guhs") + \
        wentry(shot("gui_mijnguhs_lijst", "The tab Mijn guhs"), "A dagboekje for every guh", "Een dagboekje voor elke guh",
               p("The Guhdex has a fifth tab, <b>Mijn guhs</b>: all your tamed guhs with a mini render, their level and a hearts bar. Click one (or press "
                 "<b>Dagboekje</b> in its guh menu) for its own page:",
                 "De Guhdex heeft een vijfde tabblad, <b>Mijn guhs</b>: al je tamme guhs met een mini-plaatje, hun niveau en een hartjesbalk. Klik er een aan (of "
                 "druk op <b>Dagboekje</b> in zijn guhmenu) voor zijn eigen pagina:") +
               ul([("a <b>3D preview</b> you can drag to rotate, its name, variant and personality, the level and hearts progress;",
                    "een <b>3D-voorbeeld</b> dat je kunt ronddraaien, zijn naam, variant en karakter, het niveau en de hartjesvoortgang;"),
                   ("its <b>favorietjes</b> (<i>???</i> until you find them), its <b>vriendjes</b>, its <b>huisje &amp; klusjes</b>;",
                    "zijn <b>favorietjes</b> (<i>???</i> tot je ze vindt), zijn <b>vriendjes</b>, zijn <b>huisje &amp; klusjes</b>;"),
                   ("<b>Waar is hij?</b>: the dimension and coordinates and what it's doing: " + "; ".join(en for en, _ in PLEK) + ";",
                    "<b>Waar is hij?</b>: de dimensie en coördinaten en wat hij doet: " + "; ".join(nl for _, nl in PLEK) + ";"),
                   ("<b>statistieken</b>: blocks travelled together, knuffels, snacks eaten, minigames together, knabbels dug up, days together, chores done, times played;",
                    "<b>statistieken</b>: blokken samen gereisd, knuffels, hapjes gegeten, minigames samen, knabbels opgegraven, dagen samen, klusjes gedaan, keer gespeeld;"),
                   ("<b>eerste keren</b> with funny njeg/vads texts (first time tamed, first ride, first minigame, first Guheinde, first ijsje...), and",
                    "<b>eerste keren</b> met grappige njeg/vads-tekstjes (eerste keer getemd, eerste ritje, eerste minigame, eerste Guheinde, eerste ijsje...), en"),
                   ("<b>wist-je-datjes</b> the guh writes itself after big moments: <i>Vandaag ben ik zo VAHOEG geworden, njeg.</i>",
                    "<b>wist-je-datjes</b> die de guh zelf schrijft na belangrijke momenten: <i>Vandaag ben ik zo VAHOEG geworden, njeg.</i>")])) + \
        gallery(fig("shot210_gui_mijnguhs_pagina", "A page", "Een pagina", "Favorietjes (found ones in pink), vriendjes and the huisje.", "Favorietjes (gevonden in roze), vriendjes en het huisje."),
                fig("shot210_gui_mijnguhs_pagina1_scroll", "Waar is hij? and statistieken", "Waar is hij? en statistieken", "Scroll down for where it is and the numbers.",
                    "Scroll omlaag voor waar hij is en de getallen."),
                fig("shot210_gui_mijnguhs_pagina1_eind", "Eerste keren and wist-je-datjes", "Eerste keren en wist-je-datjes", "The dagboekje bits, newest first.",
                    "De dagboekjesstukjes, nieuwste eerst."))

    # ---------------------------------------------------------------- H: Guhkamer
    kamer = h3("H &middot; De Guhkamer in je Guhmaag", "H &middot; De Guhkamer in je Guhmaag") + \
        wentry(shot("guhkamer_binnen", "Inside the Guhkamer"), "A guest room in your stomach", "Een logeerkamer in je maag",
               p("In every Guhmaag stands a curtain door with guh ears and a little heart: the <b>Guhkamer door</b>. Step through and you are in your own "
                 "<b>Guhkamer</b>, a cosy room (cherry floor, pink walls, guh crystal lamps, a rug) where your tamed guhs can stay when they don't come along. "
                 "Put Guhhuisjes and toys in it: they work just like outside. Guests pad around and play while you're there; while nobody is in, they are kept "
                 "safe. The room <b>grows with every zielsguh</b>: 16&times;16 blocks and 6 guests, +4 blocks and +3 guests per zielsguh bff 5evr &lt;3 "
                 "(up to 48&times;48 and 30 guests). Others can visit when your maag is public (the same rules as the maag): looking is fine, petting is not.",
                 "In elke Guhmaag staat een gordijndeur met guhoortjes en een hartje: de <b>deur van de Guhkamer</b>. Stap erdoor en je bent in je eigen "
                 "<b>Guhkamer</b>, een knusse kamer (kersenhouten vloer, roze muren, guhkristallampjes, een kleedje) waar je tamme guhs kunnen logeren als ze niet "
                 "meegaan. Zet er Guhhuisjes en speelgoed in: die werken net als buiten. Logés drentelen rond en spelen als jij er bent; als er niemand is, worden ze "
                 "veilig bewaard. De kamer <b>groeit met elke zielsguh</b>: 16&times;16 blokken en 6 logés, +4 blokken en +3 logés per zielsguh bff 5evr &lt;3 "
                 "(tot 48&times;48 en 30 logés). Anderen mogen op visite als je maag openbaar is (dezelfde regels als de maag): kijken mag, aaien niet.")) + \
        gallery(fig("guhkamer_deur", "The door", "De deur", "A curtain arch with ears, a name board and a heart.", "Een gordijnboog met oortjes, een naambordje en een hartje."),
                fig("shot210_guhkamer_deur", "In the maag", "In de maag", "The door stands near the middle of every maag, with a sign.",
                    "De deur staat bij het midden van elke maag, met een bordje."),
                fig("shot210_gui_guhbel_kamer", "De Guhbel", "De Guhbel",
                    "Ring the Guhbel: <i>Bij jou</i> lists your guhs within 24 blocks (click = go and stay in the Guhkamer), <i>In de Guhkamer</i> the guests (click = tingeling, it comes to you, wherever you are).",
                    "Rinkel de Guhbel: <i>Bij jou</i> toont je guhs binnen 24 blokken (klik = logeren in de Guhkamer), <i>In de Guhkamer</i> de logés (klik = tingeling, hij komt naar je toe, waar je ook bent).")) + \
        '<div class="recipes">' + recipe_card("guhbel", "Guhbel", "Guhbel") + '</div>'

    # ---------------------------------------------------------------- menu, advancements, quests, commands
    menu = h3("The guh menu", "Het guhmenu") + \
        p("The guh menu (hold right-click on your guh) has two new buttons: <b>Knuffelen!</b> (a big vadsige cuddle, lots of hearts; afterwards it needs a "
          "moment to cool down) and <b>Dagboekje</b> (opens its page in the Guhdex). Feeding: just right-click your own guh with a snack.",
          "Het guhmenu (houd rechtsklik ingedrukt op je guh) heeft twee nieuwe knoppen: <b>Knuffelen!</b> (een dikke vadsige knuffel, veel hartjes; daarna moet "
          "hij even bijkomen) en <b>Dagboekje</b> (opent zijn pagina in de Guhdex). Voeren: rechtsklik je eigen guh gewoon met een snackje.")

    lang = json.load(open(os.path.join("src", "main", "resources", "assets", "guhs", "lang", "nl_nl.json"), encoding="utf-8"))
    adv_dir = os.path.join("src", "main", "resources", "data", "guhs", "advancement", "lieve_vadsjes")
    groups = [("band", "Hartjes", "Hartjes"), ("huisje", "Guhhuisje", "Guhhuisje"), ("klusjes", "Klusjes", "Klusjes"), ("speelgoed", "Speelgoed", "Speelgoed"),
              ("guhkamer", "Guhkamer", "Guhkamer"), ("samen", "Samen", "Samen"), ("favorietjes", "Favorietjes", "Favorietjes")]
    rows, total = [], 1
    for pre, en, nl in groups:
        names = []
        for key in lang:
            m = key.startswith(f"advancements.guhs.lieve_vadsjes.{pre}_") and key.endswith(".title")
            if not m:
                continue
            aid = key[len("advancements.guhs.lieve_vadsjes."):-len(".title")]
            frame = ""
            path = os.path.join(adv_dir, aid + ".json")
            if os.path.exists(path) and json.load(open(path, encoding="utf-8")).get("display", {}).get("frame") == "challenge":
                frame = " &#9733;"
            names.append(html.escape(lang[key]) + frame)
        total += len(names)
        rows.append([f"<b>{t(en, nl)}</b>", " &middot; ".join(names)])
    adv = h3("Advancements and quests", "Vooruitgangen en quests") + \
        p(f"A new advancement tab, <b>Lieve vadsjes van elkaar</b>, with {total} advancements (it opens when you tame your first guh; &#9733; = challenge):",
          f"Een nieuw vooruitgangentabblad, <b>Lieve vadsjes van elkaar</b>, met {total} vooruitgangen (het gaat open als je je eerste guh temt; &#9733; = uitdaging):") + \
        table([("Part", "Onderdeel"), ("Advancements", "Vooruitgangen")], rows) + \
        p(f"In <b>FTB Quests</b> there is an eleventh chapter, <b>Lieve vadsjes van elkaar</b> (<i>Hartjes, huisjes, klusjes en favorietjes. Njeg!</i>), with {band_quests} "
          "quests in seven sections: Hartjes voor je guh, Het Guhhuisje, Klusjes rond het huisje, Speelgoed, De Guhkamer in je Guhmaag, Samen spelen, samen "
          "knuffelen, and Favorietjes. Nothing is locked.",
          f"In <b>FTB Quests</b> is er een elfde hoofdstuk, <b>Lieve vadsjes van elkaar</b> (<i>Hartjes, huisjes, klusjes en favorietjes. Njeg!</i>), met "
          f"{band_quests} quests in zeven onderdelen: Hartjes voor je guh, Het Guhhuisje, Klusjes rond het huisje, Speelgoed, De Guhkamer in je Guhmaag, Samen "
          "spelen, samen knuffelen, en Favorietjes. Niets zit op slot.")
    cmds = h3("Handy commands (operators)", "Handige commando's (operators)") + \
        cmd("/guhs samen hartjes lief") + cmd("/guhs samen hartjes mega") + cmd("/guhs samen hartjes zielsguh") + \
        p("Fills the hearts of your nearest own guh up to that level (as if you spent days together). <code>/guhs samen welkom|troost|welterusten|bff</code> "
          "plays a reaction.",
          "Vult de hartjes van je dichtstbijzijnde eigen guh tot dat niveau (alsof jullie dagen samen waren). <code>/guhs samen welkom|troost|welterusten|bff</code> "
          "speelt een reactie af.") + \
        cmd("/give @s guhs:guhhuisje_groot") + cmd("/give @s guhs:guhbel") + cmd("/give @s guhs:knabbelbal")

    body = intro + parts + hartjes + ziel + emotes + fav + huisje + scherm + klusjes + speel + samen + dex + kamer + menu + adv + cmds
    return section("new210", "New in 2.10: Lieve vadsjes van elkaar", "Nieuw in 2.10: Lieve vadsjes van elkaar", body)


def fixes210_section():
    """2.10.0: the fixes and the 2.9 feedback (DESIGN_210 par. 9): land around sunk buildings, the Guh-Circuit reset loop, three golf
    tees per hole, the doolhof knabbels, names above the piep animals, the Guhdex range and the rebuilt Elf-Guhjestocht."""
    shot = lambda name, alt: img("shot210_" + name, alt, "shot")
    wentry = lambda *a, **k: entry(*a, wide=True, **k)

    def fig(name, en, nl, den, dnl):
        cls = "shot" if name.startswith("shot") else ""
        return f'''<figure><div class="stage">{img(name, en, cls)}</div><figcaption><h3>{t(en, nl)}</h3>
<p>{t(den, dnl)}</p></figcaption></figure>'''
    gallery = lambda *figs: '<div class="gallery">' + "".join(figs) + '</div>'

    intro = p("Besides all the cuddling, 2.10 fixes a few things from 2.9 and earlier. Thanks for the feedback, njeg! (The things you liked, and how "
              "rare every building is, stay exactly as they were.)",
              "Naast al het geknuffel repareert 2.10 een paar dingen uit 2.9 en eerder. Dank je wel voor de feedback, njeg! (Wat jullie leuk vonden, en hoe "
              "zeldzaam elk gebouw is, blijft precies zoals het was.)")
    grond = h3("The land meets every building", "Het land sluit netjes aan") + \
        gallery(fig("shot210_structure_sjoelhuisje_2_side", "Sjoelhuisje", "Sjoelhuisje", "The grass runs right up to the floor.", "Het gras loopt tot aan de vloer."),
                fig("shot210_structure_guh_sterrenwacht_2_side", "Guh-Sterrenwacht", "Guh-Sterrenwacht", "No 20-block pit around it any more.", "Geen kuil van 20 blokken meer eromheen."),
                fig("shot210_structure_knabbelkatapult_3_close", "Knabbelkatapult", "Knabbelkatapult", "The ledge sits in the land.", "De richel zit gewoon in het land."),
                fig("shot210_structure_kaasknabbel_nest_2_side", "Kaasknabbel-nest", "Kaasknabbel-nest", "Snug in the ground.", "Knus in de grond.")) + \
        p("Around buildings that sit a little sunk in the ground there used to be an <i>upside-down staircase</i>: a moat that stepped down to the walls "
          "(up to 20 blocks deep at the Sterrenwacht). Now the land meets every building <b>at its real floor</b>, with a smooth slope: the Sjoelhuisje, "
          "Guh-Sterrenwacht, Knabbelkatapult, Guh-Circuit, racebaan, Knabbelspelen, Guhdoolhof, Knuffeldal town, Elf-Guhjestocht, Guhboerderij, Knuffelbad, "
          "Ballonfestival, kampeerplekjes, the boomhutdorp, the moerasheks-hut and the small ones (24 buildings). This works in <b>newly generated chunks</b>.",
          "Rond gebouwen die een beetje in de grond gezakt zijn zat vroeger een <i>omgekeerd trapje</i>: een slootje dat naar de muren toe omlaag trapte (tot "
          "20 blokken diep bij de Sterrenwacht). Nu sluit het land <b>op de echte vloer</b> aan bij elk gebouw, met een zachte helling: het Sjoelhuisje, de "
          "Guh-Sterrenwacht, de Knabbelkatapult, het Guh-Circuit, de racebaan, de Knabbelspelen, het Guhdoolhof, het Knuffeldal-stadje, de Elf-Guhjestocht, de "
          "Guhboerderij, het Knuffelbad, het Ballonfestival, de kampeerplekjes, het boomhutdorp, de moerasheks-hut en de kleintjes (24 gebouwen). Dit werkt in "
          "<b>nieuw gemaakte chunks</b>.")
    circuit = h3("Guh-Circuit: no more <i>Oepsie!</i> loop", "Guh-Circuit: geen <i>Oepsie!</i>-lus meer") + \
        p("On the Regenboogbaan you could get stuck in a loop of <i>Oepsie! Een klein stukje terug...</i>. Now a fall is measured from the road you last drove "
          "on (the big downhill after ring 4 is no fall), you only land on safe road spots, you get a short breather after a reset, and the message comes at "
          "most once every few seconds in the chat.",
          "Op de Regenboogbaan kon je vast komen te zitten in een lus van <i>Oepsie! Een klein stukje terug...</i>. Nu wordt een val gemeten vanaf de weg waar je "
          "het laatst op reed (de grote afdaling na ring 4 is geen val), je landt alleen op veilige plekjes op de weg, je krijgt even rust na een reset, en het "
          "berichtje komt hooguit eens per paar seconden in de chat.")
    golf = h3("Guhgolf: three tees per hole", "Guhgolf: drie afslagen per hole") + \
        wentry(shot("golf_hole1_tees", "Three tees on hole 1"), "Makkelijk, medium and lastig tees", "Afslagen voor makkelijk, medium en lastig",
               table([("Level", "Niveau"), ("Tee", "Afslag"), ("Par", "Par"), ("Extra", "Extra")], [
                   [("Makkelijk", "Makkelijk"), ("green mat with an arrow and a heart, close to the cup", "groene mat met een pijl en een hartje, dicht bij de hole"),
                    "18 (2 &times; 9)", ("no penalty strokes", "geen strafslagen")],
                   [("Medium", "Medium"), ("pink mat, the old tee", "roze mat, de oude afslag"), "27", ("as before", "zoals altijd")],
                   [("Lastig", "Lastig"), ("red mat with Mika horns, further back and behind obstacles", "rode mat met Mika-hoorntjes, verder weg en achter obstakels"),
                    "36", ("the windmill turns twice as fast and 28 extra pink slime bumpers appear for the round", "de molen draait twee keer zo snel en er komen 28 extra roze slijmbumpers voor die ronde")]]) +
               p("The <b>wind is gone</b>. Every level has its own par on the card and the boards. Golf courses that were already generated keep only the medium "
                 "tees (every level then uses those and medium par); new courses get all three.",
                 "De <b>wind is weg</b>. Elk niveau heeft een eigen par op de kaart en de borden. Golfbanen die al gemaakt waren houden alleen de medium-afslagen "
                 "(elk niveau gebruikt die dan, met medium-par); nieuwe banen krijgen ze alle drie.")) + \
        gallery(fig("structure_guh_golfbaan", "The course", "De baan", "The golf course from above, with all 27 tees.", "De golfbaan van bovenaf, met alle 27 afslagen."),
                fig("shot210_golf_hole1_mat_dichtbij", "The makkelijk mat", "De makkelijk-mat", "Green, with an arrow and a heart.", "Groen, met een pijl en een hartje."),
                fig("shot210_golf_hole8_9_tees", "Holes 8 and 9", "Holes 8 en 9", "Red lastig tees behind the obstacles.", "Rode lastig-afslagen achter de obstakels."))
    doolhof = h3("Guhdoolhof: floating, sparkling knabbels", "Guhdoolhof: zwevende, glinsterende knabbels") + \
        gallery(fig("shot210_doolhof_knabbel_zweeft", "Above the hedge", "Boven de heg",
                    "The stolen knabbels float and sparkle just above the hedges, so you see them from afar. Walk under one to grab it.",
                    "De gepikte knabbels zweven en glinsteren net boven de heggen, zodat je ze van ver ziet. Loop eronder om hem te pakken."),
                fig("shot210_doolhof_start_hud", "Nog 8 verstopt", "Nog 8 verstopt", "On makkelijk the bar counts how many are still hidden.",
                    "Op makkelijk telt de balk hoeveel er nog verstopt zijn.")) + \
        p("And none get lost any more: every second the doolhof checks that exactly the missing knabbels are hidden in the maze, and hides a new one when one "
          "went missing (for example when its chunk unloaded).",
          "En er raakt er geen meer kwijt: elke seconde kijkt het doolhof of precies de ontbrekende knabbels in het doolhof verstopt zijn, en verstopt er een "
          "nieuwe als er een zoekgeraakt is (bijvoorbeeld als zijn chunk ontladen werd).")
    namen = h3("Names and the Guhdex", "Naampjes en de Guhdex") + \
        entry(shot("namen_piep", "Schilly, Poepschilly and a pieppiepmuisje with their names"), "Names above their heads", "Naampjes boven hun kopje",
              ul([("<b>Schilly</b>, <b>Poepschilly</b> and the <b>pieppiepmuisje</b> show their name above their head, like guhs (not while they hide or crawl inside a guh).",
                   "<b>Schilly</b>, <b>Poepschilly</b> en het <b>pieppiepmuisje</b> laten hun naam boven hun kopje zien, net als guhs (niet als ze zich verstoppen of in een guh kruipen)."),
                  ("The <b>Guhdex</b> fills a page as soon as you are within <b>3 blocks</b> of a guh or a guh character (it used to be 1).",
                   "De <b>Guhdex</b> vult een pagina zodra je binnen <b>3 blokken</b> van een guh of een guhpersonage bent (vroeger 1).")]))
    elftocht = h3("The Elf-Guhjestocht, rebuilt", "De Elf-Guhjestocht, opnieuw gebouwd") + \
        wentry(img("structure_elfguhjestocht", "The new Elf-Guhjestocht from above"), "A winding canal through a snowy polder", "Een kronkelend kanaal door een besneeuwde polder",
               ul([("The canal now winds like a real river (a smooth loop with meanders, 1278 blocks) and is <b>2 to 5 blocks wide</b> per bank, with natural, slightly ragged banks (always wide at villages, bridges and the start).",
                    "Het kanaal kronkelt nu als een echte rivier (een zachte lus met bochtjes, 1278 blokken) en is per oever <b>2 tot 5 blokken breed</b>, met natuurlijke, een beetje rafelige oevers (altijd breed bij de dorpjes, de bruggetjes en de start)."),
                   ("The <b>eleven villages are spread evenly</b> along the whole route (every 100 to 131 blocks), turned to face the ice.",
                    "De <b>elf dorpjes liggen mooi verspreid</b> langs de hele route (elke 100 tot 131 blokken), met hun voorkant naar het ijs."),
                   ("The ice grid is gone: <b>seven wobbly ditches</b> with rows of knotwilgen run through the fields (some into the canal), with groups of knotwilgen, reed clumps and a guh-molentje here and there.",
                    "Het ijsrooster is weg: <b>zeven kronkelige slootjes</b> met rijen knotwilgen lopen door de weilanden (een paar tot in het kanaal), met groepjes knotwilgen, rietpollen en hier en daar een guh-molentje."),
                   ("<b>Gentle snowy hills</b> (at most 2 blocks, snow blocks and snow layers), flat under every building and at the ice.",
                    "<b>Zachte sneeuwheuveltjes</b> (hooguit 2 blokken, sneeuwblokken en sneeuwlaagjes), vlak onder elk gebouwtje en bij het ijs."),
                   ("Lamps and flags in a natural rhythm, straw bales and vuurkorven in the sharp bends, two audience groups per stretch (177 guhs), koek-en-zopie stalls, 6 bridges, 23 boosts and 345 night lights.",
                    "Lampjes en vlaggetjes in een natuurlijk ritme, strobalen en vuurkorven in de scherpe bochten, twee groepjes publiek per stuk (177 guhs), koek-en-zopiekraampjes, 6 bruggetjes, 23 boosts en 345 nachtlampjes."),
                   ("Your <b>skates work on the whole tour</b> now: all of the structure plus 16 blocks around it. Skate off it and you first get a friendly warning and a few seconds to come back.",
                    "Je <b>schaatsen werken nu op de hele tocht</b>: het hele bouwwerk plus 16 blokken eromheen. Schaats je eraf, dan krijg je eerst een lieve waarschuwing en een paar seconden om terug te komen.")])) + \
        gallery(fig("shot210_elftocht_bovenaf", "From above", "Van bovenaf", "The loop through the polder.", "De lus door de polder."),
                fig("shot210_elftocht_relief_sloten1", "Snowy hills and ditches", "Sneeuwheuveltjes en slootjes", "A skating pond between the fields.", "Een ijsbaantje tussen de weilanden."),
                fig("shot210_elftocht_kanaal_bocht", "A bend", "Een bocht", "A village on the outside of a bend.", "Een dorpje aan de buitenkant van een bocht."),
                fig("shot210_elftocht_dorp08_knabbelsward", "Knabbelsward", "Knabbelsward", "Village 8, facing the ice.", "Dorp 8, met zijn voorkant naar het ijs."),
                fig("shot210_elftocht_startboog", "The start", "De start", "Guhwarden with the start arch.", "Guhwarden met de startboog."),
                fig("shot210_elftocht_overzicht_zuid", "Overview", "Overzicht", "Villages spread along the whole route.", "Dorpjes verspreid langs de hele route.")) + \
        p("The new layout comes in newly generated Elf-Guhjestochten; an old one keeps its 2.9 layout, but the skates fix works there too.",
          "De nieuwe indeling komt in nieuw gemaakte Elf-Guhjestochten; een oude houdt zijn 2.9-indeling, maar de schaatsfix werkt daar ook.")
    body = intro + grond + circuit + golf + doolhof + namen + elftocht
    return section("fixes210", "Fixes in 2.10", "Fixes in 2.10", body)


def verhalen30_section():
    """3.0.0 "Guhverhalen": the Timmerguh and the huisjes, the Baltoguh in Nomguh with the sled ride and the sledesprint, the
    Guhtwo on the kloon-eiland with Mieuwguh and Sjokkel, the Hemelkapelletje with the Knuffelhart, Lilo & 626-guh on Guhwai'i
    with the scanner, surfing, hula and the Tiki shop, all the critters, the new clothes and biomes, and the story-guh rules.
    Texts after guhs_work30/DESIGN_30.md, CONTRACT_30.md, the slice reports and the lang files; pictures from
    wiki_renders.main_v30 and the 3.0 visual QA screenshots (shot30_*)."""
    import json
    chapters = {f: n for f, _, _, n in ftb_chapters()}
    verhalen_quests, diertjes_quests = chapters.get("guhs_verhalen", 0), chapters.get("guhs_diertjes", 0)
    shot = lambda name, alt: img("shot30_" + name, alt, "shot")
    wentry = lambda *a, **k: entry(*a, wide=True, **k)

    def fig(name, en, nl, den, dnl, sub_en="", sub_nl=""):
        cls = "shot" if name.startswith("shot") else ""
        sub = f'<span class="rarity">{t(sub_en, sub_nl)}</span>' if sub_en else ""
        return f'''<figure><div class="stage">{img(name, en, cls)}</div><figcaption><h3>{t(en, nl)}{sub}</h3>
<p>{t(den, dnl)}</p></figcaption></figure>'''

    gallery = lambda *figs: '<div class="gallery">' + "".join(figs) + '</div>'
    icons = lambda *names: "<p>" + " ".join(icon(n, n.replace("_", " ")) for n in names) + "</p>"
    quote = lambda en, nl: f'<blockquote class="note">{t(en, nl)}</blockquote>'

    intro = p("Guhs 3.0 <b>Guhverhalen</b> is the story update: four big guh stories to play through, each in its own new place in the Guhmension, "
              "and each ending with a very special guh that comes home with <b>you</b>. Help the <b>Baltoguh</b> race the medicine through a snowstorm to the "
              "sick babies of <b>Nomguh</b>. Repair the cloning tank of a scatterbrained professor and share a double meal with the <b>Guhtwo</b>. Make the "
              "<b>Knuffelhart</b> beat again in a chapel on the clouds, so guhs that went to the wolkjes can come back. And on the tropical island "
              "<b>Guhwai'i</b>, teach the wild blue <b>626-guh</b> that <i>ohana</i> means family. On top of that: the <b>Timmerguh</b> who teaches you to build "
              "guhhuisjes, surfing and hula with Lilo-guh, a sled race against the show-off <b>Steele-Mika</b>, two new biomes, <b>thirteen little critters</b> "
              "(birds, guhxolotls, ducklings, bunnies, hedgehogs, squirrels, fireflies... and a Sjokkel!) and sixteen new pieces of clothing. As always: guhs are "
              "only ever lief, Mika's are naughty but never really hurt anyone, and a guh is never too vads. Njeg!",
              "Guhs 3.0 <b>Guhverhalen</b> is de verhalenupdate: vier grote guhverhalen om te spelen, elk op een eigen nieuwe plek in de Guhmensie, en elk "
              "eindigt met een heel bijzondere guh die met <b>jou</b> mee naar huis gaat. Help de <b>Baltoguh</b> het medicijn door een sneeuwstorm naar de zieke "
              "guhbaby's van <b>Nomguh</b> te brengen. Repareer de kloontank van een verstrooide professor en deel een dubbele maaltijd met de "
              "<b>Guhtwo</b>. Laat het <b>Knuffelhart</b> in een kapelletje op de wolken weer kloppen, zodat guhs die naar de wolkjes zijn gegaan terug "
              "kunnen komen. En leer op het tropische eiland <b>Guhwai'i</b> de wilde blauwe <b>626-guh</b> dat <i>ohana</i> familie betekent. En verder: de "
              "<b>Timmerguh</b> die je leert guhhuisjes te bouwen, surfen en hula met Lilo-guh, een sledesprint tegen die opschepper <b>Steele-Mika</b>, twee "
              "nieuwe biomen, <b>dertien kleine diertjes</b> (vogeltjes, guhxolotls, eendjes, konijntjes, egeltjes, eekhoorntjes, glimguhtjes... en een Sjokkel!) "
              "en zestien nieuwe kleertjes. Zoals altijd: guhs zijn alleen maar lief, Mika's zijn ondeugend maar doen nooit echt pijn, en een guh is nooit te "
              "vads. Njeg!")
    parts = ul([("<b>De Timmerguh</b>: a building site in every Knuffeldal town and the questline <i>Samen een huisje bouwen</i>. Guhhuisjes now need his "
                 "<b>bouwboekje</b>, and only the owner may change a huisje.",
                 "<b>De Timmerguh</b>: een bouwplaats in elk Knuffeldal-stadje en de questline <i>Samen een huisje bouwen</i>. Guhhuisjes hebben nu zijn "
                 "<b>bouwboekje</b> nodig, en alleen de eigenaar mag een huisje veranderen."),
                ("<b>Baltoguh en Nomguh</b>: the snowy town in the new <b>Sneeuwguhtoendra</b>, Boris the goose, Steele-Mika, Muk &amp; Luk, little Rosy and the "
                 "medicine ride through the storm on a real <b>sled</b>.",
                 "<b>Baltoguh en Nomguh</b>: het besneeuwde stadje in de nieuwe <b>Sneeuwguhtoendra</b>, Boris de gans, Steele-Mika, Muk &amp; Luk, kleine Rosy en de "
                 "medicijntocht door de storm op een echte <b>slee</b>."),
                ("<b>De sledesprint</b> against Steele-Mika (3 levels, sledebelletjes, a winter shop) and your own <b>sneeuwslee</b>.",
                 "<b>De sledesprint</b> tegen Steele-Mika (3 niveaus, sledebelletjes, een winterwinkeltje) en je eigen <b>sneeuwslee</b>."),
                ("<b>Het kloon-eiland</b>: Professor Knabbelkloon's lab in the Guhzee, the <b>Guhtwo</b>, the giggly <b>Mieuwguh</b> and <b>Sjokkel</b>.",
                 "<b>Het kloon-eiland</b>: het lab van Professor Knabbelkloon in de Guhzee, de <b>Guhtwo</b>, de giechelende <b>Mieuwguh</b> en <b>Sjokkel</b>."),
                ("<b>Het Hemelkapelletje</b>: the <b>Knuffelhart</b> brings your guhs back from the wolkjes, free and as often as you like.",
                 "<b>Het Hemelkapelletje</b>: het <b>Knuffelhart</b> haalt je guhs terug uit de wolkjes, gratis en zo vaak als je wilt."),
                ("<b>Guhwai'i</b>: a new tropical island biome, Lilo-guh and Nani-guh, the crashed capsule with the <b>vadsigheid-scanner</b>, and the "
                 "<b>626-guh</b>.",
                 "<b>Guhwai'i</b>: een nieuw tropisch eilandbioom, Lilo-guh en Nani-guh, de neergestorte capsule met de <b>vadsigheid-scanner</b>, en de "
                 "<b>626-guh</b>."),
                ("<b>Surfen &amp; hula</b> with Lilo-guh on the surf beach, <b>schelpjesmunten</b> and Tikiguh's <b>Tiki shop</b>.",
                 "<b>Surfen &amp; hula</b> met Lilo-guh op het surfstrand, <b>schelpjesmunten</b> en het <b>Tiki-winkeltje</b> van Tikiguh."),
                ("<b>Diertjes van de Guhmensie</b>: 4 birds, guhxolotls, guh-eendjes, 3 insects, 3 little mammals and Sjokkel; most of them tameable and "
                 "pick-uppable.",
                 "<b>Diertjes van de Guhmensie</b>: 4 vogeltjes, guhxolotls, guh-eendjes, 3 insectjes, 3 kleine zoogdiertjes en Sjokkel; de meeste tembaar en "
                 "op te pakken."),
                ("<b>Also</b>: the superkompas tab <b>Verhalen</b>, 29 new Guhdex pages, two new advancement tabs and two new FTB chapters.",
                 "<b>Verder</b>: het superkompas-tabblad <b>Verhalen</b>, 29 nieuwe Guhdex-pagina's, twee nieuwe vooruitgangentabbladen en twee nieuwe "
                 "FTB-hoofdstukken.")])
    hero = gallery(fig("verhaalguhs_30", "Baltoguh, Guhtwo and 626-guh", "Baltoguh, Guhtwo en 626-guh",
                       "The three story guhs. Each of them is yours once, at the end of its story.",
                       "De drie verhaalguhs. Elk ervan wordt een keer van jou, aan het eind van zijn verhaal."),
                   fig("shot30_verhaal_verhaalguhs", "Home with you", "Mee naar huis",
                       "Just tamed: a shower of hearts. They are a bit bigger than a normal guh.",
                       "Net getemd: een regen van hartjes. Ze zijn een beetje groter dan een gewone guh."))

    # ---------------------------------------------------------------- where: the superkompas tab Verhalen
    waar = h3("Where are the stories?", "Waar zijn de verhalen?") + \
        wentry(shot("gui_verhaal_superkompas_10_verhalen", "The superkompas tab Verhalen"), "The superkompas tab Verhalen", "Het superkompas-tabblad Verhalen",
               p("Everything happens in the <b>Guhmension</b>. The <b>superkompas</b> has a new tab <b>Verhalen</b> (the icon is the Baltoguh-beeldje) that points "
                 "you to all of them:",
                 "Alles gebeurt in de <b>Guhmensie</b>. Het <b>superkompas</b> heeft een nieuw tabblad <b>Verhalen</b> (het icoontje is het Baltoguh-beeldje) dat "
                 "je naar allemaal de weg wijst:") +
               table([("Place", "Plek"), ("Where", "Waar"), ("Story", "Verhaal")], [
                   ["<b>Nomguh</b>", ("in the middle of every Sneeuwguhtoendra (one per tundra)", "midden in elke Sneeuwguhtoendra (een per toendra)"), "Baltoguh"],
                   [("<b>Het kloon-eiland</b>", "<b>Het kloon-eiland</b>"), ("a rock island out in the Guhzee", "een rotseiland ver in de Guhzee"), "Guhtwo"],
                   [("<b>Het Hemelkapelletje</b>", "<b>Het Hemelkapelletje</b>"), ("on a floating islet above guh land; a bit rarer than the other buildings",
                                                                                    "op een zwevend eilandje boven het guhland; iets zeldzamer dan de andere gebouwen"),
                    ("Knuffelhart", "Knuffelhart")],
                   [("<b>Lilo and Nani's stilt house</b>", "<b>Het paalhuisje van Lilo en Nani</b>"), ("on the beach of a Guhwai'i island", "aan het strand van een Guhwai'i-eiland"), "626-guh"],
                   [("<b>The crashed capsule</b>", "<b>De neergestorte capsule</b>"), ("on the top of a Guhwai'i island", "boven op een Guhwai'i-eiland"), "626-guh"],
                   [("<b>The surf beach of Guhwai'i</b>", "<b>Het surfstrand van Guhwai'i</b>"), ("on the other beach of the same island", "aan het andere strand van hetzelfde eiland"),
                    ("surfing &amp; hula", "surfen &amp; hula")],
                   ["<b>Knuffeldal</b>", ("every Knuffeldal town now has the Timmerguh's building site", "elk Knuffeldal-stadje heeft nu de bouwplaats van de Timmerguh"), "Timmerguh"]]) +
               p("The Minigames tab of the superkompas has a fourth heading, <b>Verhalen</b>, with Nomguh (the sledesprint) and the surf beach. Picking a story place "
                 "for the first time gives the advancement <i>Er was eens...</i>. Every story place has its own <b>Reisguh</b>, so once you've been there you can "
                 "travel back in one click.",
                 "Het tabblad Minigames van het superkompas heeft een vierde kopje, <b>Verhalen</b>, met Nomguh (de sledesprint) en het surfstrand. De eerste keer "
                 "dat je een verhaalplek kiest krijg je de vooruitgang <i>Er was eens...</i>. Elke verhaalplek heeft een eigen <b>Reisguh</b>, dus als je er een keer "
                 "bent geweest reis je met een klik terug."))

    # ---------------------------------------------------------------- story-guh rules
    regels = h3("The story guhs: once per player", "De verhaalguhs: een keer per speler") + \
        p("The <b>Baltoguh</b>, the <b>Guhtwo</b> and the <b>626-guh</b> are new guh variants, but you can't find them in the wild. How it works:",
          "De <b>Baltoguh</b>, de <b>Guhtwo</b> en de <b>626-guh</b> zijn nieuwe guhvarianten, maar je vindt ze niet in het wild. Zo werkt het:") + \
        ul([("In its building lives a <b>story copy</b>: it's part of the story, stays at its spot (it walks back if it wanders off), can't be hurt, and "
             "kaas knabbels don't tame it.",
             "In zijn gebouw woont een <b>verhaalkopie</b>: die hoort bij het verhaal, blijft op zijn plekje (hij loopt terug als hij wegdwaalt), kan niet gewond "
             "raken, en kaasknabbels temmen hem niet."),
            ("When you finish its story, the copy asks if it may come along. Then <b>your own</b> story guh appears next to you, already tamed, with a shower "
             "of hearts. It is a bit bigger than a normal guh (1.25&times;).",
             "Als je zijn verhaal af hebt, vraagt de kopie of hij mee mag. Dan verschijnt <b>jouw eigen</b> verhaalguh naast je, al getemd, met een regen van "
             "hartjes. Hij is een beetje groter dan een gewone guh (1,25&times;)."),
            ("<b>Once per player</b>: you get one of each. Every other player on the server can play the story too and gets their own. The copy always stays "
             "in the building for the next player.",
             "<b>Een keer per speler</b>: je krijgt er van elk een. Elke andere speler op de server kan het verhaal ook spelen en krijgt zijn eigen. De kopie "
             "blijft altijd in het gebouw voor de volgende speler."),
            ("After that it's a normal guh of yours: hearts, favorietjes, a dagboekje, a huisje, klusjes, clothes, riding, picking up. Each one has its own "
             "<b>special button</b> in the guh menu (next to the Guhkamer button): <i>Snuffel!</i>, <i>Telekinese</i> or <i>Ukelele</i>.",
             "Daarna is het een gewone guh van jou: hartjes, favorietjes, een dagboekje, een huisje, klusjes, kleertjes, rijden, oppakken. Elk heeft een eigen "
             "<b>speciale knop</b> in het guhmenu (naast de Guhkamer-knop): <i>Snuffel!</i>, <i>Telekinese</i> of <i>Ukelele</i>."),
            ("Babies of story guhs are ordinary guhs, and there are no plushies of them in the grijpmachine.",
             "Baby's van verhaalguhs zijn gewone guhs, en er zijn geen knuffels van ze in de grijpmachine.")])

    # ---------------------------------------------------------------- A: the Timmerguh
    timmer = h3("A &middot; De Timmerguh: Samen een huisje bouwen", "A &middot; De Timmerguh: Samen een huisje bouwen") + \
        wentry(img("structure_timmerguh_bouwplaats", "The building site"), "The building site in every Knuffeldal town", "De bouwplaats in elk Knuffeldal-stadje",
               p("At the end of the street past Cocotje's house (follow the signpost <i>Naar de bouwplaats!</i>) every Knuffeldal town now has a "
                 "<b>bouwplaats</b>: a half-finished guhhuisje (a big guh head with eye windows and a mouth door), a scaffold with a ladder, a pink crane with guh "
                 "ears, a site hut with the building plan, a sawhorse, stacks of planks and wool, a wheelbarrow and a lunch table. The roof and the two round "
                 "ears are still see-through <b>ghost tiles</b>. There you meet the <b>Timmerguh</b>, in his crooked helmpje and tool belt, humming along with "
                 "every tick of his hammer.",
                 "Aan het eind van de straat langs het huisje van Cocotje (volg het bordje <i>Naar de bouwplaats!</i>) heeft elk Knuffeldal-stadje nu een "
                 "<b>bouwplaats</b>: een half afgebouwd guhhuisje (een groot guhhoofd met oogramen en een mondjesdeur), een steigertje met een ladder, een roze "
                 "bouwkraan met guhoortjes, een bouwkeet met de bouwtekening, een zaagbok, stapels planken en wol, een kruiwagen en een lunchtafel. Het dak en "
                 "de twee ronde oortjes zijn nog doorzichtige <b>spookplekjes</b>. Daar woont de <b>Timmerguh</b>, met zijn scheve helmpje en gereedschapsriem, "
                 "die bij elke tik van zijn hamertje neuriet.")) + \
        gallery(fig("shot30_timmerguh_bouwplaats", "The building site", "De bouwplaats", "The half-built huisje with the crane and the scaffold.",
                    "Het half gebouwde huisje met de kraan en de steiger."),
                fig("npc_timmerguh", "The Timmerguh", "De Timmerguh", "Helmpje with a pink ridge, a pencil behind his ear, a tuinbroek and a tool belt.",
                    "Helmpje met een roze kammetje, een potlood achter zijn oor, een tuinbroek en een gereedschapsriem."),
                fig("shot30_gui_timmerguh_praat", "Talking to him", "Met hem praten", "He talks with answer buttons, like all 3.0 characters.",
                    "Hij praat met antwoordknoppen, net als alle 3.0-personages.")) + \
        entry(img("block_timmerguh_dakplek", "A ghost tile"), "The questline", "De questline",
              ol_steps([("Say hello and pick <i>Ik help je mee!</i>", "Zeg hallo en kies <i>Ik help je mee!</i>"),
                        ("Bring him <b>16 planks</b> (any wood) and <b>8 pink wool</b>.", "Breng hem <b>16 planken</b> (elk soort hout) en <b>8 roze wol</b>."),
                        ("He lends you <b>dakpluisjes</b>. Right-click every ghost tile of the roof and the ears (46 of them): tik, tik, pluf! The last one puts the "
                         "pink flag on top (<i>De vlag in top!</i>) with fireworks, and you get a small huisje: <i>ons eerste huisje</i>.",
                         "Hij leent je <b>dakpluisjes</b>. Rechtsklik elk spookplekje van het dak en de oortjes (46 stuks): tik, tik, pluf! De laatste zet de roze "
                         "vlag in top (<i>De vlag in top!</i>) met vuurwerk, en je krijgt een klein huisje: <i>ons eerste huisje</i>."),
                        ("Let one of your guhs <b>move into</b> a huisje of yours.", "Laat een van je guhs <b>intrekken</b> in een huisje van jou."),
                        ("Tell the Timmerguh: you get his <b>bouwboekje</b>, another small huisje and the <b>timmermanshelmpje</b>.",
                         "Vertel het de Timmerguh: je krijgt zijn <b>bouwboekje</b>, nog een klein huisje en het <b>timmermanshelmpje</b>."),
                        ("Optional: put a <b>toy</b> (glijbaantje, wip, schommel, tunnel or a knabbelbal) and a <b>guhlampje</b> in the home area of a huisje: the "
                         "<b>gereedschapsriem</b>.",
                         "Optioneel: zet een <b>speeltje</b> (glijbaantje, wip, schommel, tunnel of een knabbelbal) en een <b>guhlampje</b> in het thuisgebied van een "
                         "huisje: de <b>gereedschapsriem</b>.")]) +
              p("With friends: everybody who is at the roof step helps on the same roof and finishes together. When the next player brings materials to a "
                "finished roof, the Timmerguh simply starts a new one (<i>er is meteen een guhgezinnetje ingetrokken!</i>). Lost your bouwboekje or your first "
                "huisje? He gives you another.",
                "Met vriendjes: iedereen die bij de dak-stap is helpt aan hetzelfde dak en is samen klaar. Brengt de volgende speler materiaal bij een af dak, dan "
                "begint de Timmerguh gewoon aan een nieuw (<i>er is meteen een guhgezinnetje ingetrokken!</i>). Je bouwboekje of je eerste huisje kwijt? Hij geeft "
                "je een nieuw.")) + \
        wentry(icon("timmerguh_bouwboekje", "Bouwboekje").replace('class="px"', 'class="px" style="width:96px;height:96px"'),
               "Huisjes need the bouwboekje now", "Huisjes hebben nu het bouwboekje nodig",
               p("Since 3.0 the three <b>guhhuisje recipes</b> need the <b>Bouwboekje van de Timmerguh</b> in the crafting grid. It stays in the grid (like a "
                 "bucket of milk gives its bucket back), so one bouwboekje builds as many huisjes as you like. <b>Huisjes that are already placed keep working</b>; "
                 "even if you already had huisjes, you do the quest once to get the book. (The small huisje moved the book into its empty top slot; the big one "
                 "has one pink wool less.)",
                 "Sinds 3.0 hebben de drie <b>guhhuisje-recepten</b> het <b>Bouwboekje van de Timmerguh</b> in het werkbankrooster nodig. Het blijft in het "
                 "rooster liggen (zoals een emmer melk zijn emmer teruggeeft), dus met een bouwboekje bouw je zoveel huisjes als je wilt. <b>Huisjes die al "
                 "staan blijven gewoon werken</b>; had je al huisjes, dan doe je de quest een keer om het boekje te krijgen. (Het kleine huisje heeft het boekje "
                 "in zijn lege bovenste vakje; het grote heeft een roze wol minder.)") +
               '<div class="recipes">' + recipe_card("guhhuisje_klein", "Guhhuisje (klein)", "Guhhuisje (klein)") +
               recipe_card("guhhuisje_medium", "Guhhuisje (medium)", "Guhhuisje (medium)") + recipe_card("guhhuisje_groot", "Guhhuisje (groot)", "Guhhuisje (groot)") +
               '</div>') + \
        wentry(shot("gui_timmerguh_huisje_alleen_kijken", "Someone else's huisje: only looking"), "Only the owner changes a huisje", "Alleen de eigenaar verandert een huisje",
               p("A huisje belongs to whoever placed it. On a server, <b>everyone may open its screen and look</b>, but for others it says <i>Dit is het huisje "
                 "van X (alleen kijken)</i>: the name can't be edited, <i>Hernoem</i>, <i>Nieuwe bewoner</i> and <i>Uit huis</i> are grey (with a tooltip), and "
                 "the chore switches can't be clicked. Others can't break it (it doesn't even crack), and can't put guhs or maatjes in or take them out. The owner, "
                 "and an operator, can do everything.",
                 "Een huisje is van wie het neerzette. Op een server <b>mag iedereen zijn scherm openen en kijken</b>, maar voor anderen staat er <i>Dit is het "
                 "huisje van X (alleen kijken)</i>: de naam is niet te veranderen, <i>Hernoem</i>, <i>Nieuwe bewoner</i> en <i>Uit huis</i> zijn grijs (met een "
                 "tooltip), en de klusjes-schakelaars zijn niet aan te klikken. Anderen kunnen het niet afbreken (er komt niet eens een barstje in), en er geen guhs "
                 "of maatjes in zetten of uit halen. De eigenaar, en een operator, mag alles.")) + \
        gallery(fig("kleding_timmer_helmpje", "Timmermanshelmpje", "Timmermanshelmpje", "Cream with a pink ridge and a heart sticker.",
                    "Crème met een roze kammetje en een hartjessticker."),
                fig("kleding_timmer_gereedschapsriem", "Gereedschapsriem", "Gereedschapsriem", "With a little hammer, a folding rule and a nail pouch.",
                    "Met een hamertje, een duimstok en een spijkerzakje."))

    # ---------------------------------------------------------------- B: Baltoguh and Nomguh
    balto = h3("B &middot; Baltoguh en Nomguh", "B &middot; Baltoguh en Nomguh") + \
        wentry(shot("biome_guhmension_sneeuwguhtoendra", "The Sneeuwguhtoendra"), "New biome: the Sneeuwguhtoendra", "Nieuw bioom: de Sneeuwguhtoendra",
               p("White snowy hills next to the Guhpieken, with groves of <b>sneeuwguhsparren</b> (spruce-like guh trees with a sleepy guh face in the trunk and "
                 "snow caps on their needles), snow drifts, snowy boulders, frosty sprigs and guh ice flowers. There's always a little snow in the air; when it "
                 "rains it becomes a storm, and in a thunderstorm a real <b>blizzard</b> with gusts and white fog. White guh-konijntjes hop around.",
                 "Witte besneeuwde heuvels naast de Guhpieken, met bosjes <b>sneeuwguhsparren</b> (sparachtige guhbomen met een slaperig guhgezichtje in de stam "
                 "en sneeuwmutsjes op hun naalden), sneeuwduinen, besneeuwde keien, rijpsprietjes en guh-ijsbloempjes. Er hangt altijd een beetje sneeuw in de "
                 "lucht; als het regent wordt het een storm, en bij onweer een echte <b>sneeuwstorm</b> met windvlagen en witte mist. Er huppelen witte "
                 "guh-konijntjes rond.") + icons("sneeuwguhspar_gezicht", "sneeuwguhspar_naalden", "sneeuwguhspar_zaailing")) + \
        wentry(img("structure_nomguh", "Nomguh"), "Nomguh", "Nomguh",
               p("In the middle of every tundra lies <b>Nomguh</b>, a snowy guh town (a wink at Nome): a round square with a big decorated sneeuwguhspar, a hot "
                 "chocolate kiosk and an <b>empty statue pedestal</b> (<i>Hier komt een held te staan...</i>), eight pastel log cottages with snowy roofs and pink "
                 "guh ears, the <b>ziekenhuisje</b> with tucked-in babies (three of them sneeze: <i>hatsjoe-njeg!</i>), the red <b>sled-dog stable</b> with the start "
                 "line, Muk &amp; Luk's igloo by the ice pond, and <b>the old boat</b> frozen in the bay where Baltoguh and Boris live. Around it the town has its own "
                 "landscape: the Nomguh peaks with the <b>berghut</b> on top, the Stormdal, the Kloof, the <b>Lawineberg</b> and the frozen bay. The whole town is "
                 "protected.",
                 "Midden in elke toendra ligt <b>Nomguh</b>, een besneeuwd guhstadje (een knipoog naar Nome): een rond plein met een grote versierde "
                 "sneeuwguhspar, een warme-chocoladekiosk en een <b>lege sokkel</b> (<i>Hier komt een held te staan...</i>), acht pastel blokhutten met "
                 "sneeuwdaken en roze guhoortjes, het <b>ziekenhuisje</b> met ingestopte baby's (drie ervan niezen: <i>hatsjoe-njeg!</i>), de rode "
                 "<b>sledehondenstal</b> met de startlijn, de iglo van Muk &amp; Luk bij het ijsvijvertje, en <b>de oude boot</b> vastgevroren in de baai, waar "
                 "Baltoguh en Boris wonen. Om het stadje ligt een eigen landschap: de Nomguh-pieken met de <b>berghut</b> bovenop, het Stormdal, de Kloof, de "
                 "<b>Lawineberg</b> en de bevroren baai. Het hele stadje is beschermd.")) + \
        gallery(fig("shot30_balto_nomguh_overzicht", "Nomguh from above", "Nomguh van bovenaf", "The square, the cottages and the frozen bay with the boat.",
                    "Het plein, de huisjes en de bevroren baai met de boot."),
                fig("shot30_balto_nomguh_plein", "The square", "Het plein", "Snowy roofs with pink guh ears.", "Sneeuwdaken met roze guhoortjes."),
                fig("shot30_balto_nomguh_stal_start", "The sled-dog stable", "De sledehondenstal", "The start line of every ride.", "De startlijn van elke tocht."),
                fig("shot30_nomguh_ziekenhuisje_binnen", "The ziekenhuisje", "Het ziekenhuisje", "Rosy and the sick babies.", "Rosy en de zieke baby's."),
                fig("shot30_nomguh_stal_binnen", "In the stable", "In de stal", "Stalls, harnesses and a hay loft.", "Boxen, tuigjes en een hooizolder."),
                fig("shot30_nomguh_berghut_binnen", "The berghut", "De berghut", "The medicijnkist on the table, far up on the Hutkop.",
                    "De medicijnkist op tafel, hoog op de Hutkop.")) + \
        h3("The characters of Nomguh", "De personages van Nomguh") + \
        gallery(fig("guh_variant_baltoguh", "Baltoguh", "Baltoguh", "Half guh, half wolf: grey fur, pointy wolf ears, a bushy tail and a real guh snoet. A bit of an outsider.",
                    "Half guh, half wolf: grijze vacht, spitse wolfsoortjes, een pluimstaart en een echte guhsnoet. Een beetje een buitenbeentje."),
                fig("npc_boris", "Boris", "Boris", "A real goose (not a guh!) with a fur hat and a Russian accent. <i>Da, kleine guh... gak!</i>",
                    "Een echte gans (geen guh!) met een bontmutsje en een Russisch accent. <i>Da, kleine guh... gak!</i>"),
                fig("npc_steele_mika", "Steele-Mika", "Steele-Mika", "The show-off champion sled leader: red racing cap, goggles, a gold medal. <i>IK ben de snelste, njeh-heh!</i>",
                    "De opschepperige kampioen-sledeleider: rood racepetje, stofbril, gouden medaille. <i>IK ben de snelste, njeh-heh!</i>"),
                fig("npc_muk", "Muk", "Muk", "A big sweet polar-bear guh with a blue scarf. Muk talks for two.",
                    "Een grote lieve ijsbeer-guh met een blauwe sjaal. Muk praat voor twee."),
                fig("npc_luk", "Luk", "Luk", "Green scarf and a fishing rod. Luk never says a word, but nods and hugs all the more.",
                    "Groene sjaal en een hengeltje. Luk zegt nooit iets, maar knikt en knuffelt des te meer."),
                fig("npc_rosy", "Rosy", "Rosy", "A little pink baby guh in her bed, with a yellow scarf and a red sniffly nose. She needs the medicine.",
                    "Een klein roze babyguhtje in haar bedje, met een geel sjaaltje en een rood snotneusje. Zij heeft het medicijn nodig."),
                fig("npc_witte_wolfguh", "The white wolf-guh", "De witte wolf-guh", "Glows softly like a star in the snow. She only appears at the darkest moment.",
                    "Gloeit zachtjes als een sterretje in de sneeuw. Ze verschijnt alleen op het donkerste moment.")) + \
        wentry(shot("gui_balto_wolfmoment", "The wolf moment"), "The story", "Het verhaal",
               ol_steps([("Meet <b>Baltoguh</b> at the old boat. Steele-Mika laughs at him: a half-wolf can't pull a sled!",
                          "Ontmoet <b>Baltoguh</b> bij de oude boot. Steele-Mika lacht hem uit: een halve wolf kan toch geen slee trekken!"),
                         ("Visit <b>Rosy</b> in the ziekenhuisje: the babies have the knabbelkuch and the medicine cupboard is empty.",
                          "Bezoek <b>Rosy</b> in het ziekenhuisje: de baby's hebben de knabbelkuch en het medicijnkastje is leeg."),
                         ("<b>Boris</b> gives you advice: the medicine is in the berghut, far away on the Nomguh peaks.",
                          "<b>Boris</b> geeft je raad: het medicijn ligt in de berghut, ver weg op de Nomguh-pieken."),
                         ("<i>Op naar de berghut!</i> You steer the sled with Baltoguh in front (see <i>The medicine ride</i> below) and load the <b>medicijnkist</b>.",
                          "<i>Op naar de berghut!</i> Je stuurt de slee met Baltoguh voorop (zie <i>De medicijntocht</i> hieronder) en laadt de <b>medicijnkist</b> in."),
                         ("On the way back, at the Wolvenrots, the storm is too strong and Baltoguh is lost. Then the <b>white wolf-guh</b> appears...",
                          "Op de terugweg, bij de Wolvenrots, is de storm te sterk en is Baltoguh de weg kwijt. Dan verschijnt de <b>witte wolf-guh</b>..."),
                         ("Back in town on time: Steele-Mika tries to take the credit, and all the guhs giggle at him lovingly. Bring the kist to Rosy.",
                          "Op tijd terug in het stadje: Steele-Mika probeert de eer op te strijken, en alle guhs giechelen hem lief uit. Breng de kist naar Rosy.")]) +
               quote("<i>\"A normal guh can't make this trip alone... But just maybe... a Baltoguh can.\"</i> Baltoguh howls, and the storm clears.",
                     "<i>\"Een gewone guh kan deze tocht niet alleen volbrengen... Maar heel misschien... een Baltoguh wel.\"</i> Baltoguh huilt, en de storm klaart op.") +
               p("Too late? <i>Njeg, nog een keer!</i> Rosy is fine, you simply try again. The last quest is called <b>Maar heel misschien... een Baltoguh wel</b>. "
                 "The rewards: your own <b>Baltoguh</b> (he asks <i>Mag ik mee?</i>), your own <b>sneeuwslee</b>, the <b>Baltoguh-beeldje</b>, the title <b>Held van "
                 "Nomguh</b> (behind your name in the tab list) and four pieces of clothing.",
                 "Te laat? <i>Njeg, nog een keer!</i> Met Rosy gaat het prima, je probeert het gewoon opnieuw. De laatste quest heet <b>Maar heel misschien... een "
                 "Baltoguh wel</b>. De beloningen: je eigen <b>Baltoguh</b> (hij vraagt <i>Mag ik mee?</i>), je eigen <b>sneeuwslee</b>, het "
                 "<b>Baltoguh-beeldje</b>, de titel <b>Held van Nomguh</b> (achter je naam in de spelerslijst) en vier kleertjes.")) + \
        gallery(fig("shot30_balto_boris", "Boris on the boat", "Boris op de boot", "", ""),
                fig("shot30_balto_steele_mika", "Steele-Mika", "Steele-Mika", "", ""),
                fig("shot30_balto_muk_luk", "Muk &amp; Luk", "Muk &amp; Luk", "", ""),
                fig("shot30_balto_rosy", "Rosy", "Rosy", "", "")) + \
        wentry(img("guh_variant_baltoguh_back", "The Baltoguh from behind"), "Your Baltoguh", "Jouw Baltoguh",
               ul([("<b>Fast in the snow</b>: on snow, snowy grass, ice and sled tracks he runs 45% faster (and faster still when you ride him), with little snow puffs.",
                    "<b>Snel in de sneeuw</b>: op sneeuw, besneeuwd gras, ijs en sledesporen rent hij 45% sneller (en nog sneller als je op hem rijdt), met "
                    "sneeuwwolkjes."),
                   ("<b>Snuffel!</b> (guh menu): nose to the ground, he sniffs the way to his own huisje, or else your last huisje, or else your spawn point. "
                    "A trail of glowing paw prints shows the way, and he tells you: <i>nog 212 blokjes naar het noordoosten</i>.",
                    "<b>Snuffel!</b> (guhmenu): neus naar de grond, hij ruikt de weg naar zijn eigen huisje, anders je laatste huisje, anders je spawnpunt. "
                    "Een spoor van gloeiende pootafdrukjes wijst de weg, en hij zegt: <i>nog 212 blokjes naar het noordoosten</i>.")]) +
               p("The <b>Baltoguh-beeldje</b> is a bronze Baltoguh on a snowy pedestal for at home: click it for the quote and a tiny howl. <i>Auuuhoe-njeg!</i>",
                 "Het <b>Baltoguh-beeldje</b> is een bronzen Baltoguh op een besneeuwde sokkel voor thuis: klik erop voor de quote en een klein gehuil. "
                 "<i>Auuuhoe-njeg!</i>") + icons("baltoguh_beeldje", "nomguh_routepaal", "nomguh_medicijnkist", "nomguh_sneeuwdak")) + \
        gallery(fig("kleding_balto_sjaaltje", "Rood sjaaltje", "Rood sjaaltje", "Baltoguh's red scarf.", "Het rode sjaaltje van Baltoguh."),
                fig("kleding_balto_wolfsoortjes", "Wolfsoortjes", "Wolfsoortjes", "Ears slot: pointy wolf ears.", "Oren-vakje: spitse wolfsoortjes."),
                fig("kleding_balto_sneeuwmuts", "Sneeuwmuts met pompon", "Sneeuwmuts met pompon", "Ice blue, with a snowflake band and two braids.",
                    "IJsblauw, met een sneeuwvlokjesband en twee vlechtjes."),
                fig("kleding_balto_wantjes", "Wantjes aan een touwtje", "Wantjes aan een touwtje", "Red mittens on the front paws.", "Rode wantjes aan de voorpootjes."))

    # ---------------------------------------------------------------- C: the sled
    SHOP = [("baltoslee_sledebellen", "Sledebellenboog", "tap it: tingeling", "tik erop: tingeling", "4"),
            ("baltoslee_lantaarnpaal", "Routelantaarn", "gives light", "geeft licht", "5"),
            ("baltoslee_sneeuwguh", "Sneeuwguh", "a snowman, but a guh", "een sneeuwpop, maar dan een guh", "6"),
            ("baltoslee_hondenmand", "Sledehondenmandje", "a warm basket for tired paws", "een warm mandje voor moeie pootjes", "8"),
            ("baltoslee_minislee", "Minisleetje", "you can sit in it", "je kunt erin zitten", "10"),
            ("baltoslee_beker", "Sledesprintbeker", "the golden cup, with guh ears", "de gouden beker, met guhoortjes", "16")]
    slee = h3("C &middot; De slee: the medicine ride, the sledesprint and your own sneeuwslee",
              "C &middot; De slee: de medicijntocht, de sledesprint en je eigen sneeuwslee") + \
        wentry(shot("gui_baltoslee_tocht_storm", "The medicine ride in the storm"), "The medicine ride", "De medicijntocht",
               p("You stand on the runners of a real sled, pulled by <b>four guh-sledehondjes</b> with Baltoguh in front. Keys: <b>W</b> <i>hup hup</i>, "
                 "<b>S</b> brake, <b>A/D</b> steer, hold <b>Shift</b> to stop. The route is <b>304 blocks each way</b>, marked with 82 red-and-white "
                 "<b>routepaaltjes</b> with glowing guh ears. The panel at the top shows the route (stable, rest points, ice bridge, avalanche, berghut), the "
                 "clock and how warm the dogs are; under the crosshair it tells you what's coming.",
                 "Je staat op de glijders van een echte slee, getrokken door <b>vier guh-sledehondjes</b> met Baltoguh voorop. Toetsen: <b>W</b> <i>hup hup</i>, "
                 "<b>S</b> remmen, <b>A/D</b> sturen, <b>Shift</b> vasthouden om te stoppen. De route is <b>304 blokken heen en 304 terug</b>, gemarkeerd met 82 "
                 "rood-witte <b>routepaaltjes</b> met gloeiende guhoortjes. Het paneel bovenin laat de route zien (stal, rustpunten, ijsbrug, lawine, berghut), de "
                 "klok en hoe warm de hondjes zijn; onder je vizier staat wat er aankomt.") +
               table([("On the way", "Onderweg"), ("What to do", "Wat moet je doen")], [
                   [("<b>Windvlagen</b>", "<b>Windvlagen</b>"), ("push you sideways; a whoosh warns you just before", "duwen je opzij; een woesj waarschuwt je vlak ervoor")],
                   [("<b>Diepe sneeuw</b>", "<b>Diepe sneeuw</b>"), ("next to the track: slow", "naast het spoor: langzaam")],
                   [("<b>De ijsbrug</b>", "<b>De ijsbrug</b>"), ("narrow and slippery: steer carefully, or plof into the soft snow and try the bridge again",
                                                              "smal en glad: stuur voorzichtig, anders plof je in de zachte sneeuw en probeer je de brug opnieuw")],
                   [("<b>De lawine</b>", "<b>De lawine</b>"), ("rolls down the Lawineberg: be on the far side of the track (<i>LAWINE van links! Stuur naar rechts</i>) or "
                                                            "BOEF, buried and dug out a little way back",
                                                            "rolt van de Lawineberg: zorg dat je aan de andere kant van het spoor bent (<i>LAWINE van links! Stuur naar "
                                                            "rechts</i>), anders BOEF, bedolven en een stukje terug weer uitgegraven")],
                   [("<b>Rustpuntjes</b>", "<b>Rustpuntjes</b>"), ("4 shelters with a vuurkorf: stop there and the dogs warm up (cold paws run slower)",
                                                                  "4 schuilhutjes met een vuurkorf: stop daar en de hondjes warmen op (koude pootjes lopen langzamer)")],
                   [("<b>Baltoguh's nose</b>", "<b>De neus van Baltoguh</b>"), ("in a white-out, glowing sniff sparkles show the middle of the track",
                                                                              "in een witte storm laten gloeiende snuffelsterretjes het midden van het spoor zien")]]) +
               p("At the berghut the ride pauses while the kist is loaded. The way back is timed (4 minutes). At the dieptepunt the clock stops for the wolf "
                 "moment. You can't get hurt while riding.",
                 "Bij de berghut pauzeert de tocht terwijl de kist wordt ingeladen. De terugweg gaat op tijd (4 minuten). Bij het dieptepunt staat de klok stil "
                 "voor het wolvenmoment. Je kunt niet gewond raken tijdens het rijden.")) + \
        wentry(shot("baltoslee_sprint_steele_naast_je", "Steele-Mika's sled next to you"), "The sledesprint against Steele-Mika", "De sledesprint tegen Steele-Mika",
               p("After the story, Steele-Mika at the start line wants a race (before that he only boasts and opens his shop). Pick <b>makkelijk</b>, "
                 "<b>medium</b> or <b>lastig</b>: there to the berghut, round it, and back to the start line, in a storm that gets stronger per level. His "
                 "dark sled with the purple blanket rides next to you and he boasts when he passes you (and when you pass him). Your time goes on the board "
                 "<i>sledesprint</i> of that level, and the world top 3 floats next to him.",
                 "Na het verhaal wil Steele-Mika bij de startlijn een wedstrijdje (daarvoor schept hij alleen op en opent hij zijn winkeltje). Kies "
                 "<b>makkelijk</b>, <b>medium</b> of <b>lastig</b>: heen naar de berghut, eromheen, en terug naar de startlijn, in een storm die per niveau sterker "
                 "wordt. Zijn donkere slee met het paarse dekentje rijdt naast je en hij schept op als hij je inhaalt (en als jij hem inhaalt). Je tijd komt op het "
                 "bord <i>sledesprint</i> van dat niveau, en de top 3 van de wereld zweeft naast hem.") +
               p("<b>Sledebelletjes</b>: 3 for a finish, +3 when you beat Steele-Mika, +2 the first time on a level, +2 for a personal record; lastig gives 50% more. "
                 "When you win, all the guhs giggle at him lovingly. He's never really mean.",
                 "<b>Sledebelletjes</b>: 3 voor een finish, +3 als je Steele-Mika verslaat, +2 de eerste keer op een niveau, +2 voor een persoonlijk record; lastig "
                 "geeft 50% meer. Win je, dan giechelen alle guhs hem lief uit. Hij is nooit echt gemeen.") +
               table([("Winter shop", "Winterwinkeltje"), ("", ""), ("Sledebelletjes", "Sledebelletjes")],
                     [[icon(i, n), f"<b>{n}</b>: {t(en, nl)}", c] for i, n, en, nl, c in SHOP])) + \
        gallery(fig("shot30_gui_baltoslee_steele", "Steele-Mika before the story", "Steele-Mika voor het verhaal",
                    "<i>Racen tegen MIJ? Eerst maar eens door de storm, net als die Baltoguh.</i>", "<i>Racen tegen MIJ? Eerst maar eens door de storm, net als die Baltoguh.</i>"),
                fig("shot30_gui_baltoslee_sprint_onderweg", "The sprint panel", "Het sprintpaneel", "Your time, the dogs' warmth and Steele-Mika on the route line.",
                    "Je tijd, de warmte van de hondjes en Steele-Mika op de routelijn."),
                fig("shot30_baltoslee_deco", "The winter decorations", "De winterdecoratie", "Sneeuwguh, minisleetje, bellenboog, beker, mandje and lantaarn.",
                    "Sneeuwguh, minisleetje, bellenboog, beker, mandje en lantaarn.")) + \
        wentry(img("sneeuwslee_span", "Your sneeuwslee with four guh-sledehondjes"), "Your own sneeuwslee", "Je eigen sneeuwslee",
               p("The <b>sneeuwslee</b> is a reward of the Nomguh story: your own sled with four guh-sledehondjes (chibi guh huskies with a blush, pointy ears and a "
                 "curly tail). Put it down on snow or ice and ride it like a boat: W/S/A/D. It only runs on <b>snow and ice</b> (elsewhere the dogs sit down: "
                 "<i>De sledehondjes willen sneeuw onder hun pootjes, njeg!</i>). Sneak + right-click puts it back in your pocket; a kaas knabbel makes the dogs happy.",
                 "De <b>sneeuwslee</b> is een beloning van het Nomguh-verhaal: je eigen slee met vier guh-sledehondjes (chibi guh-husky's met blosjes, spitse oortjes "
                 "en een krulstaartje). Zet hem neer op sneeuw of ijs en rijd ermee als met een boot: W/S/A/D. Hij rijdt alleen op <b>sneeuw en ijs</b> (ergens "
                 "anders gaan de hondjes zitten: <i>De sledehondjes willen sneeuw onder hun pootjes, njeg!</i>). Sluipen + rechtsklik stopt hem terug in je zak; een "
                 "kaasknabbel maakt de hondjes blij.") + icons("sneeuwslee", "sledebelletje"))

    # ---------------------------------------------------------------- D: the kloon-eiland
    mewtwo = h3("D &middot; Het kloon-eiland: Guhtwo, Mieuwguh and Sjokkel", "D &middot; Het kloon-eiland: Guhtwo, Mieuwguh en Sjokkel") + \
        wentry(img("structure_kloon_eiland", "The kloon-eiland"), "The island lab", "Het eilandlab",
               p("Far out in the Guhzee a rock island rises from the sea floor, with pink coral, two sandy coves and a grassy top with blossom bushes. On it: the "
                 "<b>koepelhal</b>, a glass dome with the cracked <b>kloontank</b> in the middle (pink knabbelsap leaks out) and five lab stations with blinking "
                 "guh computers; the messy <b>kantoortje</b> of the professor; a tower with a spiral staircase up to the <b>arena</b> on top, with our own "
                 "knabbelbal logo on the floor, where the Guhtwo sits sulking; and a <b>steiger</b> with a boathouse and a Reisguh. The island is protected.",
                 "Ver in de Guhzee rijst een rotseiland op uit de zeebodem, met roze koraal, twee zandbaaitjes en een grasveld bovenop met bloesemstruikjes. "
                 "Daarop: de <b>koepelhal</b>, een glazen koepel met de gebarsten <b>kloontank</b> in het midden (er lekt roze knabbelsap uit) en vijf labplekken met "
                 "knipperende guh-computers; het rommelige <b>kantoortje</b> van de professor; een toren met een wenteltrap naar de <b>arena</b> bovenop, met ons "
                 "eigen knabbelbal-logo op de vloer, waar de Guhtwo zit te mokken; en een <b>steiger</b> met een boothuisje en een Reisguh. Het eiland is "
                 "beschermd.")) + \
        gallery(fig("shot30_mewtwo_eiland_overzicht", "The island", "Het eiland", "", ""),
                fig("shot30_mewtwo_koepelhal_kapot", "The cracked tank", "De gebarsten tank", "", ""),
                fig("shot30_mewtwo_koepelhal_heel", "Repaired!", "Gerepareerd!", "The tank bubbles again. Blub, blub!", "De tank borrelt weer. Blub, blub!"),
                fig("shot30_mewtwo_kantoortje", "The kantoortje", "Het kantoortje", "Papers everywhere. Very important. Probably.",
                    "Overal papieren. Heel belangrijk. Waarschijnlijk."),
                fig("shot30_mewtwo_arena", "The arena", "De arena", "With the grote knabbelschaal.", "Met de grote knabbelschaal."),
                fig("shot30_mewtwo_steiger", "The steiger", "De steiger", "Where your boat lands.", "Waar je boot aanlegt.")) + \
        wentry(img("npc_knabbelkloon", "Professor Knabbelkloon"), "Professor Knabbelkloon's story", "Het verhaal van Professor Knabbelkloon",
               p("The professor (crooked round glasses he keeps pushing straight, a messy white tuft, a lab coat with a pink stain) wanted to clone a guh that could "
                 "eat <b>twice as many kaas knabbels</b>. It didn't become a clone, but something new: the <b>Guhtwo</b>. And he really can eat twice as "
                 "much, so the wish came true after all.",
                 "De professor (een scheef rond brilletje dat hij steeds rechtduwt, een warrig wit plukje, een labjas met een roze vlek) wilde een guh klonen die "
                 "<b>dubbel zoveel kaasknabbels</b> kon eten. Het werd geen kloon, maar iets nieuws: de <b>Guhtwo</b>. En die kan echt dubbel zoveel eten, "
                 "dus de wens kwam toch uit.") +
               ol_steps([("He tells the story in bits and has lost his notes. Find the <b>6 labnotities</b> around the lab (the spots sparkle for you; right-click "
                          "a note to read it). With all six he remembers, and you get the <b>trainerpetje</b>.",
                          "Hij vertelt het verhaal in stukjes en is zijn notities kwijt. Zoek de <b>6 labnotities</b> in het lab (de plekjes glinsteren voor jou; "
                          "rechtsklik een notitie om hem te lezen). Met alle zes weet hij het weer, en krijg je het <b>trainerpetje</b>."),
                         ("Get the <b>4 tank parts</b> from the crates (a glass panel, a copper knabbel tube, a bubble pump and a bottle of pink knabbelsap) and "
                          "click the tank to build them in: its four lamps go from red to green. The tank bubbles, and <b>Mieuwguh</b> appears, giggling. You get the "
                          "<b>trainerpakje</b>.",
                          "Haal de <b>4 tankonderdelen</b> uit de kisten (een glazen paneel, een koperen knabbelbuisje, een borrelpompje en een fles roze "
                          "knabbelsap) en klik op de tank om ze in te bouwen: zijn vier lampjes gaan van rood naar groen. De tank borrelt, en <b>Mieuwguh</b> "
                          "verschijnt giechelend. Je krijgt het <b>trainerpakje</b>."),
                         ("The big meal: put a <b>double portion</b> (32 kaas knabbels and 2 snacks) in the grote knabbelschaal in the arena. The Guhtwo and "
                          "Mieuwguh eat together, x2!, and become best friends. You get the <b>Guhtwo-staartje</b> and the <b>Mieuwguh-ballonnetje</b>.",
                          "De grote maaltijd: doe een <b>dubbele portie</b> (32 kaasknabbels en 2 snackjes) in de grote knabbelschaal in de arena. De Guhtwo en "
                          "Mieuwguh eten samen, x2!, en worden beste vriendjes. Je krijgt het <b>Guhtwo-staartje</b> en het <b>Mieuwguh-ballonnetje</b>."),
                         ("Click the Guhtwo: <i>Mag ik met jou mee?</i> Your own Guhtwo!",
                          "Klik op de Guhtwo: <i>Mag ik met jou mee?</i> Je eigen Guhtwo!")]) +
               icons("mewtwo_labnotitie", "mewtwo_tankonderdeel_1", "mewtwo_tankonderdeel_2", "mewtwo_tankonderdeel_3", "mewtwo_tankonderdeel_4",
                     "mewtwo_computer", "mewtwo_reageerbuisjes", "mewtwo_knabbelschaal")) + \
        gallery(fig("guh_variant_mewtwo", "The Guhtwo", "De Guhtwo", "Lilac grey with a purple tail with a bulb and a tube from his head to his neck, "
                    "but with fat guh cheeks and guh ears. Cute, never scary.",
                    "Lichtgrijs-paars met een paarse staart met een bolletje en een buisje van zijn achterhoofd naar zijn nek, maar met dikke guhwangen en "
                    "guhoortjes. Schattig, nooit eng."),
                fig("critter_mew", "Mieuwguh", "Mieuwguh", "A tiny pink floating guh with a long thin tail. She giggles at everything and makes somersaults in the air. "
                    "Only around the island after the tank, not tameable, but she has a Guhdex page.",
                    "Een piepklein roze zwevend guhtje met een lange dunne staart. Ze giechelt om alles en maakt koprolletjes in de lucht. Alleen rond het eiland "
                    "na de tank, niet tembaar, maar ze heeft wel een Guhdex-pagina."),
                fig("shot30_mewtwo_mew_rond_het_eiland", "Mieuwguh round the island", "Mieuwguh rond het eiland", "", "")) + \
        wentry(img("guh_variant_mewtwo_back", "The Guhtwo from behind"), "Your Guhtwo", "Jouw Guhtwo",
               ul([("<b>He floats</b> a little above the ground, with a soft purple glow under him. Riding him, he floats over short gaps (about 4 blocks) and "
                    "then sinks down gently.",
                    "<b>Hij zweeft</b> een stukje boven de grond, met een zachte paarse gloed onder zich. Als je op hem rijdt zweeft hij over korte gaten (zo'n 4 "
                    "blokken) en zakt dan zachtjes omlaag."),
                   ("<b>Knabbel-telekinese</b>: kaas knabbels and items within 8 blocks float to him, and on to you when you're within 12 blocks. Switch it "
                    "on and off with the button <i>Telekinese</i> in the guh menu.",
                    "<b>Knabbel-telekinese</b>: kaasknabbels en spulletjes binnen 8 blokken zweven naar hem toe, en door naar jou als je binnen 12 blokken bent. "
                    "Aan en uit met de knop <i>Telekinese</i> in het guhmenu."),
                   ("<b>x2</b>: he eats every snack twice, with a double chomp, an x2 sparkle and <b>double hearts</b>.",
                    "<b>x2</b>: hij eet elk snackje dubbel, met een dubbele hap, een x2-glinster en <b>dubbele hartjes</b>.")])) + \
        wentry(img("critter_shuckle", "Sjokkel"), "Sjokkel (not a guh!)", "Sjokkel (geen guh!)",
               p("A red shell full of cheese holes, a little yellow head with bead eyes and yellow feet peeking out of the holes. Sjokkel crawls <b>very</b> slowly "
                 "and is shy: come too close without sneaking and he hides in his shell (<i>tink</i>). He lives on the rocky coast of the kloon-eiland (his "
                 "<b>Sjokkel-plekjes</b> keep one or two around) and, very rarely, deep in the Gatenkaasgrotten.",
                 "Een rode schelp vol kaasgaatjes, een geel kopje met kraaloogjes en gele pootjes die uit de gaatjes piepen. Sjokkel kruipt <b>heel</b> traag en is "
                 "verlegen: kom je te dichtbij zonder te sluipen, dan kruipt hij in zijn schelp (<i>tink</i>). Hij woont op de rotskust van het kloon-eiland (zijn "
                 "<b>Sjokkel-plekjes</b> houden er een of twee in de buurt) en, heel zelden, diep in de Gatenkaasgrotten.") +
               ul([("Sneak up and feed him <b>sweet berries</b>: 1 in 4 tames him. He follows you slowly, you can pick him up, and he can live in a guhhuisje.",
                    "Sluip dichterbij en voer hem <b>zoete bessen</b>: 1 op 4 temt hem. Hij volgt je traag, je kunt hem oppakken, en hij kan in een guhhuisje wonen."),
                   ("He keeps berries in his shell and turns every 3 into a <b>Sjokkel-bessensapje</b> (food + Regeneration), by himself or with the button "
                    "<i>Bessensapje!</i>.",
                    "Hij bewaart bessen in zijn schelp en maakt van elke 3 een <b>Sjokkel-bessensapje</b> (eten + Regeneratie), vanzelf of met de knop "
                    "<i>Bessensapje!</i>."),
                   ("His huisje chore is <b>polijsten</b>: he takes cobblestone from the chest and polishes it slowly into smooth stone, or (1 in 4) into two "
                    "<b>gepolijste guhsteentjes</b>. Four of those make three pieces of <b>guhsteentjespad</b>.",
                    "Zijn huisjesklusje is <b>polijsten</b>: hij pakt keien uit de kist en poetst ze langzaam tot gladde steen, of (1 op 4) tot twee <b>gepolijste "
                    "guhsteentjes</b>. Vier daarvan maken drie stukjes <b>guhsteentjespad</b>.")]) +
               icons("shuckle_item", "landdiertjes_bessensapje", "landdiertjes_guhsteentje", "landdiertjes_steentjespad")) + \
        gallery(fig("kleding_mewtwo_trainerpetje", "Trainerpetje", "Trainerpetje", "With our own logo: the knabbelbal with guh ears. <i>Ik kies jou, njeg!</i>",
                    "Met ons eigen logo: de knabbelbal met guhoortjes. <i>Ik kies jou, njeg!</i>"),
                fig("kleding_mewtwo_trainerpakje", "Trainerpakje", "Trainerpakje", "A lilac vest and a backpack with a knabbelbal.", "Een lila vestje en een rugzakje met een knabbelbal."),
                fig("kleding_mewtwo_staartje", "Guhtwo-staartje + nekbuisje", "Guhtwo-staartje + nekbuisje", "Now you're a bit of a Guhtwo too!",
                    "Nu ben jij ook een beetje Guhtwo!"),
                fig("kleding_mew_ballonnetje", "Mieuwguh-ballonnetje", "Mieuwguh-ballonnetje", "A pink balloon that looks like Mieuwguh. It giggles in the wind.",
                    "Een roze ballonnetje dat op Mieuwguh lijkt. Het giechelt als het waait."))

    # ---------------------------------------------------------------- E: the Hemelkapelletje
    hemel = h3("E &middot; Het Hemelkapelletje en het Knuffelhart", "E &middot; Het Hemelkapelletje en het Knuffelhart") + \
        wentry(img("structure_hemelkapelletje", "The Hemelkapelletje"), "A chapel on the clouds", "Een kapelletje op de wolken",
               p("Somewhere above the guh land (a bit rarer than the other buildings) you find a round <b>cloud plaza</b> with a big pink pixel heart. Take one of "
                 "its two <b>wolkenliften</b> 40 blocks up to a <b>floating islet</b>: a lawn with guh flowers and blossom trees, hanging guh crystals underneath, "
                 "and a little white chapel with gold trims, pink windows, a heart window and a pink-and-white cloud dome. Soft music plays inside. On the altar, "
                 "under a glass dome, glows <b>het Knuffelhart</b>, a pink heart you can't craft, mine or move: it only exists here. Next to it stands "
                 "<b>de wolkenhoeder</b>, a cloud guh with a golden crook and a tiny halo. There's also a <b>hemelkist</b> in the garden.",
                 "Ergens boven het guhland (iets zeldzamer dan de andere gebouwen) vind je een rond <b>wolkenplein</b> met een groot roze pixelhart. Neem een van "
                 "zijn twee <b>wolkenliften</b> 40 blokken omhoog naar een <b>zwevend eilandje</b>: een grasveldje met guhbloemen en bloesembomen, hangende "
                 "guhkristallen eronder, en een wit kapelletje met gouden randjes, roze ramen, een hartjesraam en een roze-witte wolkenkoepel. Binnen speelt zachte "
                 "muziek. Op het altaar, onder een glazen koepel, gloeit <b>het Knuffelhart</b>, een roze hart dat je niet kunt craften, minen of verplaatsen: het "
                 "bestaat alleen hier. Ernaast staat <b>de wolkenhoeder</b>, een wolkenguh met een gouden herdersstaf en een piepklein aureooltje. In de tuin staat "
                 "ook een <b>hemelkist</b>.")) + \
        gallery(fig("shot30_hemel_kapelletje", "The floating islet", "Het zwevende eilandje", "", ""),
                fig("shot30_hemel_kapel_binnen", "Inside", "Binnen", "A pink aisle, benches and hanging lampions.", "Een roze loper, bankjes en hangende lampionnen."),
                fig("shot30_hemel_kapel_altaar", "The Knuffelhart", "Het Knuffelhart", "On the altar under its glass dome.", "Op het altaar onder zijn glazen koepel."),
                fig("npc_wolkenhoeder", "De wolkenhoeder", "De wolkenhoeder", "He knows exactly what the heart needs.", "Hij weet precies wat het hart nodig heeft.")) + \
        wentry(shot("gui_hemel_scherm", "The Knuffelhart screen"), "Make the heart beat again", "Laat het hart weer kloppen",
               p("The heart is asleep. The wolkenhoeder asks for three things, in any order and over as many visits as you like:",
                 "Het hart slaapt. De wolkenhoeder vraagt drie dingen, in elke volgorde en in zoveel bezoekjes als je wilt:") +
               icons("guh_kristal", "gouden_kaasknabbel", "pluisveertje") +
               ul([("a <b>guhkristal</b> (Guh-Kristalmijn),", "een <b>guhkristal</b> (Guh-Kristalmijn),"),
                   ("a <b>gouden kaasknabbel</b>,", "een <b>gouden kaasknabbel</b>,"),
                   ("a <b>pluisveertje</b> (from the Pluisvinkjes, see <i>Diertjes</i>).", "een <b>pluisveertje</b> (van de Pluisvinkjes, zie <i>Diertjes</i>).")]) +
               p("With all three the Knuffelhart wakes up: <i>ba-dum, ba-dum</i>, for you. From then on, talk to the wolkenhoeder and pick <i>Laat me mijn guhs in "
                 "de wolkjes zien</i>. The screen shows <b>every tamed guh of yours that died</b>: its name and variant, a little turning figure with its clothes, "
                 "its hearts and level, and since which day it's in the wolkjes. Pick one and press <b>Haal ... terug &hearts;</b>: it comes back between you and "
                 "the heart with <b>all its hearts, favorietjes, clothes, saddle and dagboekje</b>, and sparkles for a minute. <b>Free, and as often as you like.</b>",
                 "Met alle drie wordt het Knuffelhart wakker: <i>ba-dum, ba-dum</i>, voor jou. Vanaf dan praat je met de wolkenhoeder en kies je <i>Laat me mijn "
                 "guhs in de wolkjes zien</i>. Het scherm laat <b>elke tamme guh van jou zien die is doodgegaan</b>: zijn naam en variant, een draaiend poppetje met "
                 "zijn kleertjes, zijn hartjes en niveau, en sinds welke dag hij in de wolkjes is. Kies er een en druk op <b>Haal ... terug &hearts;</b>: hij komt "
                 "terug tussen jou en het hart met <b>al zijn hartjes, favorietjes, kleertjes, zadel en dagboekje</b>, en glinstert een minuutje. <b>Gratis, en zo "
                 "vaak als je wilt.</b>")) + \
        wentry(shot("gui_verhaal_wolkjes", "A dagboekje in the wolkjes"), "When a tamed guh dies", "Als een tamme guh doodgaat",
               ul([("It leaves a glowing star behind: <b>Herinnering aan &lt;naam&gt;</b>. Right-click it to hug the memory; use it on the Knuffelhart to bring "
                    "exactly that guh back. (The heart can also do it without the star.)",
                    "Er blijft een gloeiend sterretje achter: <b>Herinnering aan &lt;naam&gt;</b>. Rechtsklik om de herinnering te knuffelen; gebruik hem op het "
                    "Knuffelhart om precies die guh terug te halen. (Het hart kan het ook zonder sterretje.)"),
                   ("It drops nothing else: its saddle, armour and backpack stay with it and come back with it.",
                    "Hij laat verder niets vallen: zijn zadel, harnas en rugzak blijven bij hem en komen met hem terug."),
                   ("Its dagboekje stays in <i>Mijn guhs</i> as <i>&#9729; naam</i>: <i>In de wolkjes... njeg</i>, with a hint that the Knuffelhart in the "
                    "Hemelkapelletje can bring it back. Its hearts never go down, not even there.",
                    "Zijn dagboekje blijft in <i>Mijn guhs</i> staan als <i>&#9729; naam</i>: <i>In de wolkjes... njeg</i>, met een tip dat het Knuffelhart in het "
                    "Hemelkapelletje hem kan terughalen. Zijn hartjes gaan nooit omlaag, ook daar niet.")]) +
               icons("herinnering")) + \
        gallery(fig("kleding_hemel_aureooltje", "Gouden aureooltje", "Gouden aureooltje", "A golden halo floating above the head.", "Een gouden aureooltje dat boven het hoofd zweeft."),
                fig("kleding_hemel_wolkenvleugeltjes", "Wolkenvleugeltjes", "Wolkenvleugeltjes", "Two puffy cloud wings with golden edges.", "Twee pluizige wolkenvleugeltjes met gouden randjes."))

    # ---------------------------------------------------------------- F: Guhwai'i
    guhwaii = h3("F &middot; Guhwai'i: Lilo-guh, 626-guh en ohana", "F &middot; Guhwai'i: Lilo-guh, 626-guh en ohana") + \
        wentry(shot("biome_guhmension_guhwaii", "Guhwai'i"), "New biome: Guhwai'i", "Nieuw bioom: Guhwai'i",
               p("A <b>tropical island</b> (about 250 blocks across) in a warm turquoise lagoon, towards the Diepe Guhzee. It has <b>guh palms</b> with a face in the "
                 "trunk (click it: it winks, <i>Njeg!</i>) and coconuts under the fronds, roze hibiscus, plumeria, paradijsvogelbloemen and orchids, a shallow "
                 "<b>kaaskoraal reef</b> with guhvisjes to snorkel over (you can breathe near the coral trees), and <b>Schilly eggs</b> in the sand that hatch "
                 "into baby Poepschilly's and Schilly's. Zeemeeuwtjes circle above the beach.",
                 "Een <b>tropisch eiland</b> (zo'n 250 blokken breed) in een warme turquoise lagune, richting de Diepe Guhzee. Er groeien <b>guh-palmen</b> met een "
                 "gezichtje in de stam (klik erop: hij knipoogt, <i>Njeg!</i>) en kokosnoten onder de bladeren, roze hibiscus, plumeria, paradijsvogelbloemen en "
                 "orchideeën, er is een ondiep <b>kaaskoraalrif</b> met guhvisjes om boven te snorkelen (bij de koraalboompjes kun je ademen), en er liggen "
                 "<b>Schilly-eitjes</b> in het zand die uitkomen in baby-Poepschilly's en -Schilly's. Boven het strand cirkelen zeemeeuwtjes.") +
               ul([("<b>Kokosnoot</b>: pick a ripe one (or wait until it falls). Eat it (krak, slurp!), give it to your guh (guhs love coconut), plant it on sand "
                    "or grass (it grows into a guh palm) or hang it under a frond.",
                    "<b>Kokosnoot</b>: pluk een rijpe (of wacht tot hij valt). Eet hem op (krak, slurp!), geef hem aan je guh (guhs zijn dol op kokos), plant hem "
                    "in zand of gras (er groeit een guh-palm uit) of hang hem onder een blad."),
                   ("<b>Kokosmelk</b>: a coconut and a glass bottle. Cool and sweet, gives Regeneration, and you get the bottle back.",
                    "<b>Kokosmelk</b>: een kokosnoot en een glazen flesje. Koel en zoet, geeft Regeneratie, en je krijgt het flesje terug."),
                   ("The four tropical flowers make dye and fit in a flower pot.", "De vier tropische bloemen geven kleurstof en passen in een bloempot.")]) +
               icons("guhwaii_palm_gezicht", "guhwaii_palm_kiemplant", "kokosnoot", "kokosmelk", "roze_hibiscus", "guhwaii_plumeria", "guhwaii_paradijsbloem",
                     "guhwaii_orchidee", "schilly_eitjes")) + \
        gallery(fig("shot30_structure_guhwaii_ohana_1_overview", "Lilo and Nani's stilt house", "Het paalhuisje van Lilo en Nani",
                    "On palm-log stilts over a lagoon pond, with a thatched roof with two pink guh ears, and the guh-asiel <i>Pootje Thuis</i>.",
                    "Op palmstammen boven een lagunevijver, met een rieten dak met twee roze guhoortjes, en het guh-asiel <i>Pootje Thuis</i>."),
                fig("shot30_guhwaii_ohana_lilo_kamer", "Lilo's room", "De kamer van Lilo", "Her bed, 626's basket, the jukebox and the poster.",
                    "Haar bed, het mandje van 626, de jukebox en de poster."),
                fig("shot30_guhwaii_ohana_keuken", "Nani's kitchen", "De keuken van Nani", "", ""),
                fig("shot30_structure_guhwaii_capsule_1_overview", "The crashed capsule", "De neergestorte capsule",
                    "A skid trail, a crater and the round pod with a cracked glass dome and guh-ear fins.",
                    "Een sleepspoor, een krater en de ronde capsule met een gebarsten glazen koepel en guhoorvinnen."),
                fig("shot30_guhwaii_capsule_binnen", "Inside the capsule", "In de capsule", "The scanner, the pilot seat, the panel and <i>Logboek 626</i>.",
                    "De scanner, de pilotenstoel, het paneel en <i>Logboek 626</i>."),
                fig("npc_lilo_guh", "Lilo-guh", "Lilo-guh", "Black hair, a red dress with white leaves and a hibiscus behind her ear. Loves Elvis-guh music.",
                    "Zwart haar, een rode jurk met witte blaadjes en een hibiscus achter haar oor. Houdt van Elvis-guhmuziek."),
                fig("npc_nani_guh", "Nani-guh", "Nani-guh", "Lilo-guh's big sister: a bit strict, but very lief.", "De grote zus van Lilo-guh: een beetje streng, maar heel lief.")) + \
        wentry(img("block_vadsigheid_scanner", "The vadsigheid-scanner"), "The vadsigheid-scanner", "De vadsigheid-scanner",
               p("In the capsule stands the <b>vadsigheid-scanner</b>, and it works on <b>every guh</b>: put a guh on the plate (or hold a picked-up guh, or your "
                 "nearest guh hops on) and click. A scan beam goes over the guh while the big screen fills up: <i>een beetje vads, vads, heel vads, VAHOEG</i>... "
                 "and then the needle breaks right through the frame, with cracks and sparks: <b>ONBEREKENBAAR VAHOEG!!!</b> Confetti, and the guh does a VAHOEG "
                 "jump. (A guh can never be too vads, of course.) The famous picture is also a <b>poster</b> and a painting for your wall.",
                 "In de capsule staat de <b>vadsigheid-scanner</b>, en die werkt op <b>elke guh</b>: zet een guh op de plaat (of houd een opgepakte guh vast, of je "
                 "dichtstbijzijnde guh springt erop) en klik. Een scanstraal gaat over de guh terwijl het grote scherm volloopt: <i>een beetje vads, vads, heel "
                 "vads, VAHOEG</i>... en dan slaat de naald dwars door het kader, met barstjes en vonkjes: <b>ONBEREKENBAAR VAHOEG!!!</b> Confetti, en de guh maakt "
                 "een VAHOEG-sprongetje. (Een guh kan natuurlijk nooit te vads zijn.) Het beroemde plaatje is ook een <b>poster</b> en een schilderij voor aan de "
                 "muur.") + icons("vadsigheid_scanner", "vadsigheid_poster")) + \
        wentry(shot("gui_guhwaii_626_menu", "The scanner with 626-guh"), "Ohana: the story of 626-guh", "Ohana: het verhaal van 626-guh",
               p("Experiment 626 is a blue alien guh with big ears, four paws and an extra pair of little arms, made to be <i>too vads</i> (but a guh can never "
                 "be too vads!). His capsule crashed on Guhwai'i and Lilo-guh <i>adopted</i> him from the guh-asiel. He wrecks things, but it's only mess and he "
                 "never hurts anyone. Help him be <i>good</i>:",
                 "Experiment 626 is een blauwe alien-guh met grote oren, vier pootjes en een extra paar armpjes, gemaakt om <i>te vads</i> te zijn (maar een guh kan "
                 "nooit te vads zijn!). Zijn capsule stortte neer op Guhwai'i en Lilo-guh heeft hem uit het guh-asiel <i>geadopteerd</i>. Hij sloopt van alles, maar "
                 "het is alleen rommel en hij doet nooit iemand pijn. Help hem om <i>goed</i> te zijn:") +
               ol_steps([("Say aloha to <b>Lilo-guh</b> at the stilt house.", "Zeg aloha tegen <b>Lilo-guh</b> bij het paalhuisje."),
                         ("Find the <b>crashed capsule</b> on top of the island.", "Vind de <b>neergestorte capsule</b> boven op het eiland."),
                         ("Back at the house: the adoption scene at the asiel.", "Terug bij het huisje: de adoptie bij het asiel."),
                         ("Clean up <b>5 rommeltjes</b> 626 left around Nani-guh (click them: only mess, nothing broken!).",
                          "Ruim <b>5 rommeltjes</b> op die 626 rond Nani-guh heeft achtergelaten (klik erop: alleen rommel, niks kapot!)."),
                         ("Teach him to be lief: give 626-guh a <b>kokosnoot</b>, a <b>roze hibiscus</b> and <b>8 kaas knabbels</b>.",
                          "Leer hem lief te zijn: geef 626-guh een <b>kokosnoot</b>, een <b>roze hibiscus</b> en <b>8 kaasknabbels</b>.")]) +
               quote("<i>\"Ohana means family. Family means nobody gets left behind... or forgotten. Njeg.\"</i>",
                     "<i>\"Ohana betekent familie. Familie betekent dat niemand wordt achtergelaten... of vergeten. Njeg.\"</i>") +
               p("Rewards: the <b>poster</b>, a spare <b>ukelele</b> (strum it and all guhs around you dance), four pieces of clothing and 16 kaas knabbels. Then give "
                 "the 626-guh a kaas knabbel and your own 626-guh comes home with you.",
                 "Beloningen: de <b>poster</b>, een reserve-<b>ukelele</b> (tokkel erop en alle guhs om je heen gaan dansen), vier kleertjes en 16 kaasknabbels. Geef "
                 "de 626-guh daarna een kaasknabbel en je eigen 626-guh gaat met je mee naar huis.")) + \
        gallery(fig("guh_variant_stitch626", "Your 626-guh", "Jouw 626-guh", "Blue fur, a dark back patch, a big dark nose, big notched ears and two extra little arms.",
                    "Blauwe vacht, een donker vlekje op zijn rug, een grote donkere neus, grote gekartelde oren en twee extra armpjes."),
                fig("guh_variant_stitch626_back", "Climbs walls and ceilings", "Klimt tegen muren en plafonds",
                    "He climbs up walls (also when you ride him) and hangs upside down from ceilings.",
                    "Hij klimt tegen muren op (ook als je op hem rijdt) en hangt ondersteboven aan plafonds."),
                fig("shot30_guhwaii_scanner_palmen_626", "Carries two", "Draagt er twee",
                    "With his extra arms he carries two things at once: double loads in the huisje chores.",
                    "Met zijn extra armpjes draagt hij twee dingen tegelijk: dubbel sjouwen bij de huisjesklusjes."),
                fig("icon_guhwaii_ukelele", "Ukelele", "Ukelele", "The button <i>Ukelele</i> (and the emote, only for him): he strums with all four arms. <i>Aloha, njeg!</i>",
                    "De knop <i>Ukelele</i> (en de emote, alleen voor hem): hij tokkelt met alle vier zijn armpjes. <i>Aloha, njeg!</i>")) + \
        gallery(fig("kleding_guhwaii_hularokje", "Hula-rokje", "Hula-rokje", "", ""),
                fig("kleding_guhwaii_bloemenkrans", "Bloemenkrans", "Bloemenkrans", "", ""),
                fig("kleding_guhwaii_stitchoren", "626-oren met antennes", "626-oren met antennes", "Ears slot.", "Oren-vakje."),
                fig("kleding_guhwaii_surfplankje", "Surfplankje", "Surfplankje", "On the back.", "Op de rug."))

    # ---------------------------------------------------------------- G: surfing, hula and the Tiki shop
    SONGS = [("<i>Aloha, Njeg</i>", "makkelijk", "88", ("only hips: A and D", "alleen heupjes: A en D")),
             ("<i>Guhla-Hula Rock</i>", "medium", "132", ("A, D, W and S", "A, D, W en S")),
             ("<i>Vahoeg Hula Hop</i>", "lastig", "160", ("eighth notes, and space on every VAHOEG! shout", "achtste noten, en spatie bij elke VAHOEG!-kreet"))]
    TIKI = [("tiki_fakkel", "Tiki-fakkel", 2, 2), ("tiki_masker", "Tiki-masker", 1, 3), ("tiki_masker_roze", "Roze tiki-masker", 1, 3),
            ("tiki_beeld", "Tiki-beeld", 1, 6), ("tiki_rietdak", "Rieten dak (+ trap, plaat)", 8, "2 / 2 / 1"), ("tiki_bloemenslinger", "Bloemenslinger", 4, 2),
            ("tiki_schelpjeslampion", "Schelpjeslampion", 2, 3), ("tiki_surfplankrek", "Surfplankenrek", 1, 5), ("tiki_bloemenmat", "Hula-bloemenmat", 4, 2),
            ("tiki_kruk", "Tiki-krukje", 2, 2), ("tiki_radiootje", "Tiki-radiootje (plays the hula songs / speelt de hula-liedjes)", 1, 10)]
    spellen = h3("G &middot; Surfen, hula en het Tiki-winkeltje", "G &middot; Surfen, hula en het Tiki-winkeltje") + \
        wentry(img("structure_guhwaii_surfstrand", "The surf beach"), "The surf beach of Guhwai'i", "Het surfstrand van Guhwai'i",
               p("On the other beach of the island: a round plaza with a hula podium under a big thatched <b>hale</b>, a flower arch, tiki torches, Lilo's "
                 "<b>surf shack</b> with board racks, Tikiguh's round <b>Tiki stall</b>, a lifeguard chair, parasols, a sand castle and an outrigger canoe. Lilo-guh "
                 "is here twice: one on the podium (hula) and one at the shack (surfing). Both games have the levels makkelijk, medium and lastig, their own "
                 "boards (the world top 3 floats over the shack and the podium), and pay in <b>schelpjesmunten</b>. You can't get hurt or hungry while playing.",
                 "Aan het andere strand van het eiland: een rond plein met een hulapodium onder een grote rieten <b>hale</b>, een bloemenboog, tiki-fakkels, het "
                 "<b>surfhutje</b> van Lilo met plankenrekken, het ronde <b>Tiki-kraampje</b> van Tikiguh, een strandwachtstoel, parasols, een zandkasteel en een "
                 "vlerkkano. Lilo-guh is hier twee keer: een op het podium (hula) en een bij het hutje (surfen). Beide spelletjes hebben de niveaus makkelijk, medium "
                 "en lastig, eigen borden (de top 3 van de wereld zweeft boven het hutje en het podium), en betalen in <b>schelpjesmunten</b>. Tijdens het spelen "
                 "raak je niet gewond en krijg je geen honger.") + icons("schelpjesmunt", "surfplankje_leen")) + \
        wentry(shot("guhwaiispellen_surfen", "Surfing"), "Surfing with Lilo-guh", "Surfen met Lilo-guh",
               p("Lilo-guh lends you a board and surfs next to you on her own. Sets of waves roll in, grow, break at a peak and peel to one side.",
                 "Lilo-guh leent je een plankje en surft zelf naast je. Er rollen sets golven binnen die groeien, breken bij een piek en naar een kant afpellen.") +
               table([("Key", "Toets"), ("What", "Wat")], [
                   ["A / D", ("paddle, and steer along the face of the wave; in the air: spin", "peddelen, en langs de golf sturen; in de lucht: draaien")],
                   ["W", ("catch the wave (on makkelijk it goes by itself); on the wave: pump down for speed", "de golf pakken (op makkelijk gaat dat vanzelf); op de golf: omlaag pompen voor snelheid")],
                   ["S", ("pump up; in the air: grab the board (the <i>njeg-grab</i>)", "omhoog pompen; in de lucht: je plank pakken (de <i>njeg-grab</i>)")],
                   [("space", "spatie"), ("jump off the lip", "van de lip springen")],
                   [("hold sneak", "sluipen vasthouden"), ("paddle back to the beach and stop", "terugpeddelen naar het strand en stoppen")]]) +
               p("Tricks: the <b>Guh-hop</b>, the <b>Knabbeldraai</b>, the <b>Dubbele</b> and <b>Driedubbele knabbeldraai</b>, the njeg-grab, cutbacks and (on medium "
                 "and lastig) <b>tube riding</b> right next to the break. They raise a multiplier up to x5. Whitewater or a crooked landing is a lovingly sad "
                 "<b>PLONS</b>: the multiplier goes back to 1, but you never lose points. You get a schelpjesmunt per 700 points (lastig: 50% more).",
                 "Trucjes: de <b>Guh-hop</b>, de <b>Knabbeldraai</b>, de <b>Dubbele</b> en <b>Driedubbele knabbeldraai</b>, de njeg-grab, cutbacks en (op medium en "
                 "lastig) <b>door de tube</b> vlak naast de breking. Ze verhogen een vermenigvuldiger tot x5. Schuim of een scheve landing is een lief-zielige "
                 "<b>PLONS</b>: de vermenigvuldiger gaat terug naar 1, maar je verliest nooit punten. Je krijgt een schelpjesmunt per 700 punten (lastig: 50% meer).")) + \
        wentry(shot("guhwaiispellen_hula", "The hula dance"), "The hula dance", "De hula-dans",
               p("Step onto the flower mat on the podium and pick a song. The steps slide in along a lane towards a pulsing hibiscus: press the right key right on "
                 "the beat. <b>VAHOEG!</b> (perfect), <b>Njeg!</b>, <b>Guh.</b> or <b>Mis</b>. A combo multiplies your points up to x3, your own guhs dance along, "
                 "and Lilo-guh sways on the beat. Three brand-new, Elvis-like guh songs (ukulele, lap steel, a slapback guitar and a guh choir):",
                 "Stap op de bloemenmat op het podium en kies een liedje. De stapjes schuiven over een baan naar een kloppende hibiscus: druk de goede toets precies "
                 "op de maat. <b>VAHOEG!</b> (perfect), <b>Njeg!</b>, <b>Guh.</b> of <b>Mis</b>. Een combo vermenigvuldigt je punten tot x3, je eigen guhs dansen mee, "
                 "en Lilo-guh wiegt op de maat. Drie gloednieuwe, Elvis-achtige guhliedjes (ukelele, lap steel, een slapback-gitaar en een guhkoortje):") +
               table([("Song", "Liedje"), ("Level", "Niveau"), ("BPM", "BPM"), ("Keys", "Toetsen")], [[s, lv, b, k] for s, lv, b, k in SONGS]) +
               p("A flawless dance is a challenge advancement. At home, the <b>Tiki-radiootje</b> plays the three songs for everyone nearby.",
                 "Een foutloze dans is een uitdaging. Thuis speelt het <b>Tiki-radiootje</b> de drie liedjes voor iedereen in de buurt.")) + \
        gallery(fig("shot30_gui_guhwaiispellen_lilo", "Pick a song", "Kies een liedje", "", ""),
                fig("npc_tikiguh", "Tikiguh", "Tikiguh", "Nobody knows what he looks like without his mask. <i>Een tiki moet je voelen, niet zien.</i>",
                    "Niemand weet hoe hij er zonder masker uitziet. <i>Een tiki moet je voelen, niet zien.</i>"),
                fig("shot30_gui_guhwaiispellen_tikiwinkel", "The Tiki shop", "Het Tiki-winkeltje", "", "")) + \
        entry(img("block_tiki_beeld", "Tiki-beeld"), "Tikiguh's Tiki shop", "Het Tiki-winkeltje van Tikiguh",
              table([("", ""), ("For sale", "Te koop"), ("Pieces", "Stuks"), ("Schelpjesmunten", "Schelpjesmunten")],
                    [[icon(i, n), n, str(a), str(c)] for i, n, a, c in TIKI]) +
              p("Never sold out. No clothes here: the Guhwai'i clothes come from the 626 story.",
                "Nooit uitverkocht. Geen kleertjes hier: de Guhwai'i-kleertjes komen uit het 626-verhaal.")) + \
        gallery(*[fig("block_" + b, n, n, "", "") for b, n in (("tiki_fakkel", "Tiki-fakkel"), ("tiki_masker", "Tiki-masker"), ("tiki_masker_roze", "Roze tiki-masker"),
                                                                 ("tiki_schelpjeslampion", "Schelpjeslampion"), ("tiki_surfplankrek", "Surfplankenrek"),
                                                                 ("tiki_radiootje", "Tiki-radiootje"))])

    # ---------------------------------------------------------------- H: the critters
    CRIT = [("pluisvinkje", "Pluisvinkje", ("Guhvelden, Roze pluisjes, Guhweides", "Guhvelden, Roze pluisjes, Guhweides"),
             ("round pink-white finches with guh ears; fly in little flocks after their leader; drop <b>pluisveertjes</b> (more when you feed them seeds)",
              "ronde roze-witte vinkjes met guhoortjes; vliegen in groepjes achter hun leidertje aan; laten <b>pluisveertjes</b> vallen (vaker als je ze zaadjes "
              "geeft)"), "&ndash;"),
            ("kaasmeesje", "Kaasmeesje", ("Vadswoud, Kaasvlakte", "Vadswoud, Kaasvlakte"),
             ("hang upside down under the leaves and peck at knabbelbessen (they leave them for you); give berries and they sing <i>tsjie-tsjie-bee!</i>",
              "hangen ondersteboven onder de blaadjes en pikken aan knabbelbessen (die laten ze voor jou hangen); geef bessen en ze zingen <i>tsjie-tsjie-bee!</i>"), "&ndash;"),
            ("guh_uiltje", "Guh-uiltje", ("Guhpieken, Vadswoud (night)", "Guhpieken, Vadswoud ('s nachts)"),
             ("sleeps fluffed up by day; at night its eyes glow and its head turns almost all the way round. <i>Oehoe... njeg!</i>",
              "slaapt overdag dik opgepluisd; 's nachts gloeien zijn oogjes en draait zijn kopje bijna helemaal rond. <i>Oehoe... njeg!</i>"), "&ndash;"),
            ("zeemeeuwtje", "Zeemeeuwtje", ("Guhzee, Diepe Guhzee, Guhwai'i", "Guhzee, Diepe Guhzee, Guhwai'i"),
             ("hold a fish or bread and they circle over you shouting <i>Mijn! Mijn!</i>; they snatch fish or bread from the ground, never anything else",
              "houd een visje of brood vast en ze cirkelen boven je en roepen <i>Mijn! Mijn!</i>; ze pikken visjes of brood van de grond, nooit iets anders"), "&ndash;"),
            ("guhxolotl_roze", "Guhxolotl", ("pools of the Guhzee, Kaasmoeras", "vijvers van de Guhzee, Kaasmoeras"),
             ("like an axolotl, with a round guh head and pink fluffy gills; roze, mint, choco, wit and very rare <b>goud</b>; plays dead when hurt; a water "
              "bucket scoops it into a <b>guhxolotl-emmertje</b>; button <i>Blubbeltjes!</i> (2 minutes of water breathing for you)",
              "zoals een axolotl, met een ronde guhkop en roze pluizige kieuwtjes; roze, mint, choco, wit en heel zeldzaam <b>goud</b>; speelt dood als hij "
              "gewond raakt; een emmer water schept hem in een <b>guhxolotl-emmertje</b>; knop <i>Blubbeltjes!</i> (2 minuten onder water ademen voor jou)"),
             ("guhvisje (1 in 3)", "guhvisje (1 op 3)")),
            ("guh_eendje", "Guh-eendje", ("ponds, Guhzee, Kaasmoeras", "vijvers, Guhzee, Kaasmoeras"),
             ("a mama duck with guh ears and 2 to 4 fluffy yellow ducklings that follow her in a <b>rijtje</b>; breed them with seeds, bread or kaas knabbels",
              "een mama-eendje met guhoortjes en 2 tot 4 pluizige gele kuikentjes die haar in een <b>rijtje</b> volgen; kweek ze met zaadjes, brood of kaasknabbels"), "&ndash;"),
            ("knabbelvlindertje_roze", "Knabbelvlindertje", ("flowery land, by day", "bloemrijk land, overdag"),
             ("four colours; flutter from flower to flower and sleep on one at night", "vier kleurtjes; fladderen van bloem naar bloem en slapen 's nachts op een bloemetje"), "&ndash;"),
            ("glimguhtje", "Glimguhtje", ("Kaasmoeras, Guhweides (night)", "Kaasmoeras, Guhweides ('s nachts)"),
             ("tiny round guhs with a lantern in their belly; they float in dreamy circles and fade away at sunrise",
              "piepkleine ronde guhtjes met een lampje in hun buik; ze zweven in dromerige rondjes en glimmen weg bij zonsopgang"), "&ndash;"),
            ("lieveheersbeestje", "Lieveheersbeestje", ("near flowers and guhtuintjes", "bij bloemen en guhtuintjes"),
             ("spots shaped like guh heads; sits on your growing guhtuintjes and makes them grow a tick faster",
              "stipjes in de vorm van guhkopjes; gaat op je groeiende guhtuintjes zitten en laat ze een tikje sneller groeien"), "&ndash;"),
            ("pluisegeltje", "Pluisegeltje", ("Vadswoud", "Vadswoud"),
             ("fluffy spikes that never prick; rolls up into a ball when startled (sneak!) or when a Mika comes near; walks through berry bushes; button "
              "<i>Rol eens!</i>",
              "pluizige stekeltjes die nooit prikken; rolt zich op tot een bolletje als hij schrikt (sluip!) of als er een Mika in de buurt komt; loopt dwars "
              "door bessenstruiken; knop <i>Rol eens!</i>"), ("sweet or glow berries", "zoete bessen of gloeibessen")),
            ("guh_konijntje_roze", "Guh-konijntje", ("Guhweides (white ones in the Sneeuwguhtoendra)", "Guhweides (witte in de Sneeuwguhtoendra)"),
             ("lop ears and a pompon tail; roze, wit, choco and grijs; hops away if you stomp; button <i>Hophop!</i>: a happy binky and Jump Boost for you",
              "hangoortjes en een pomponstaartje; roze, wit, choco en grijs; huppelt weg als je stampt; knop <i>Hophop!</i>: een blije binky en Sprongkracht voor jou"),
             ("carrot, golden carrot or kaas knabbel", "wortel, gouden wortel of kaasknabbel")),
            ("pluiseekhoorntje", "Pluiseekhoorntje", ("Vadswoud, Roze pluisjes, Guhvelden", "Vadswoud, Roze pluisjes, Guhvelden"),
             ("a huge curly tail and ear tufts; buries <b>knabbelvoorraadjes</b> (right-click a little heap of dirt to dig one up); tamed it sits on your "
              "<b>shoulder</b> (<i>Op mijn schouder!</i>) and now and then digs up a stash for you",
              "een enorme krulstaart en pluimpjes op zijn oortjes; begraaft <b>knabbelvoorraadjes</b> (rechtsklik een hoopje aarde om er een op te graven); getemd "
              "zit hij op je <b>schouder</b> (<i>Op mijn schouder!</i>) en graaft hij af en toe een voorraadje voor je op"), ("kaas knabbels", "kaasknabbels")),
            ("shuckle", "Sjokkel", ("rocky coast of the kloon-eiland; very rare deep in the Gatenkaasgrotten", "rotskust van het kloon-eiland; heel zelden diep in de Gatenkaasgrotten"),
             ("see <i>D &middot; Het kloon-eiland</i>: shy, slow, makes bessensapje and polishes stones", "zie <i>D &middot; Het kloon-eiland</i>: verlegen, traag, maakt "
              "bessensapje en poetst steentjes"), ("sweet berries (1 in 4)", "zoete bessen (1 op 4)"))]
    diertjes = h3("H &middot; Diertjes van de Guhmensie", "H &middot; Diertjes van de Guhmensie") + \
        p("The Guhmension got a lot livelier: thirteen kinds of little critters, all lief and guh-inspired, but not guhs. Each one has a <b>Guhdex page</b> (they "
          "count for <i>alles verzameld</i>, and so does Sjokkel). The ones you can tame can also be <b>picked up</b> (sneak + right-click with an empty hand, like "
          "the pieppiepmuisje), they follow you or sit, and they can <b>live in a guhhuisje</b>. Most of them get startled if you run up to them: <b>sneak</b> to "
          "come close.",
          "De Guhmensie is een stuk levendiger geworden: dertien soorten kleine diertjes, allemaal lief en guh-geïnspireerd, maar geen guhs. Elk heeft een "
          "<b>Guhdex-pagina</b> (die tellen mee voor <i>alles verzameld</i>, Sjokkel ook). De tembare kun je ook <b>oppakken</b> (sluipen + rechtsklik met een lege "
          "hand, net als het pieppiepmuisje), ze volgen je of blijven zitten, en ze kunnen <b>in een guhhuisje wonen</b>. De meeste schrikken als je op ze af "
          "rent: <b>sluip</b> dichterbij.") + \
        table([("", ""), ("Critter", "Diertje"), ("Where", "Waar"), ("What it does", "Wat doet het"), ("Tame with", "Temmen met")],
              [[img("critter_" + pic, name, "px").replace('class="px"', 'class="px" style="width:56px;height:56px"'), f"<b>{name}</b>", waar_, wat, tam]
               for pic, name, waar_, wat, tam in CRIT]) + \
        gallery(fig("critters_vogeltjes", "The four birds", "De vier vogeltjes", "Pluisvinkje, kaasmeesje, guh-uiltje and zeemeeuwtje.",
                    "Pluisvinkje, kaasmeesje, guh-uiltje en zeemeeuwtje."),
                fig("critters_guhxolotls", "Five guhxolotls", "Vijf guhxolotls", "Roze, mint, choco, wit and the rare goud.", "Roze, mint, choco, wit en de zeldzame goud."),
                fig("critters_konijntjes", "Guh-konijntjes", "Guh-konijntjes", "In four furs.", "In vier vachtjes."),
                fig("critters_vlindertjes", "Knabbelvlindertjes", "Knabbelvlindertjes", "Kaasgeel, roze, mint and lila.", "Kaasgeel, roze, mint en lila.")) + \
        gallery(fig("shot30_vogels_overdag", "Birds by day", "Vogeltjes overdag", "A flock of pluisvinkjes and a kaasmeesje hanging under a leaf.",
                    "Een groepje pluisvinkjes en een kaasmeesje dat onder een blaadje hangt."),
                fig("shot30_waterdiertjes_eendjes_en_insectjes", "A rijtje of ducklings", "Een rijtje kuikentjes", "", ""),
                fig("shot30_waterdiertjes_glimguhtjes_nacht", "Glimguhtjes at night", "Glimguhtjes 's nachts", "", ""),
                fig("shot30_landdiertjes_rij", "The little mammals", "De kleine zoogdiertjes", "Egeltje, konijntjes, eekhoorntje and Sjokkel.",
                    "Egeltje, konijntjes, eekhoorntje en Sjokkel."),
                fig("shot30_landdiertjes_schouder", "On your shoulder", "Op je schouder", "A tamed pluiseekhoorntje.", "Een tam pluiseekhoorntje."),
                fig("shot30_landdiertjes_voorraadje", "Knabbelvoorraadjes", "Knabbelvoorraadjes", "Dig them up!", "Graaf ze op!")) + \
        entry(img("block_vogels_voerhuisje", "Vogelvoerhuisje"), "The vogelvoerhuisje", "Het vogelvoerhuisje",
              p("A pink bird table (pink dye, planks, wheat seeds and a stick). Put seeds on it and the birds within 16 blocks come to eat; pluisvinkjes sometimes "
                "leave a pluisveertje behind. The pluisveertje is also what the wolkenhoeder needs for the Knuffelhart.",
                "Een roze vogeltafeltje (roze kleurstof, planken, tarwezaadjes en een stok). Leg er zaadjes op en de vogeltjes binnen 16 blokken komen smullen; "
                "pluisvinkjes laten er soms een pluisveertje achter. Het pluisveertje is ook wat de wolkenhoeder nodig heeft voor het Knuffelhart.") +
              icons("vogels_voerhuisje", "pluisveertje", "guhxolotl_emmertje", "landdiertjes_knabbelvoorraadje", "pluisegeltje_item", "guh_konijntje_item",
                    "pluiseekhoorntje_item"))

    # ---------------------------------------------------------------- I: all new clothes
    KLEREN = [("timmer_helmpje", "Timmermanshelmpje", ("head", "hoofd"), "Timmerguh"), ("timmer_gereedschapsriem", "Gereedschapsriem", ("body", "lijf"), ("Timmerguh (optional step)", "Timmerguh (optionele stap)")),
              ("balto_sjaaltje", "Rood sjaaltje van Baltoguh", ("neck", "nek"), "Nomguh"), ("balto_wolfsoortjes", "Wolfsoortjes", ("ears", "oren"), "Nomguh"),
              ("balto_sneeuwmuts", "Sneeuwmuts met pompon", ("head", "hoofd"), "Nomguh"), ("balto_wantjes", "Wantjes aan een touwtje", ("body", "lijf"), "Nomguh"),
              ("mewtwo_trainerpetje", "Trainerpetje", ("head", "hoofd"), ("kloon-eiland (notes)", "kloon-eiland (notities)")),
              ("mewtwo_trainerpakje", "Trainerpakje", ("body", "lijf"), ("kloon-eiland (tank)", "kloon-eiland (tank)")),
              ("mewtwo_staartje", "Guhtwo-staartje + nekbuisje", ("neck", "nek"), ("kloon-eiland (meal)", "kloon-eiland (maaltijd)")),
              ("mew_ballonnetje", "Mieuwguh-ballonnetje", ("back", "rug"), ("kloon-eiland (meal)", "kloon-eiland (maaltijd)")),
              ("hemel_aureooltje", "Gouden aureooltje", ("head", "hoofd"), "Hemelkapelletje"), ("hemel_wolkenvleugeltjes", "Wolkenvleugeltjes", ("back", "rug"), "Hemelkapelletje"),
              ("guhwaii_hularokje", "Hula-rokje", ("body", "lijf"), "Guhwai'i"), ("guhwaii_bloemenkrans", "Bloemenkrans", ("neck", "nek"), "Guhwai'i"),
              ("guhwaii_stitchoren", "626-oren met antennes", ("ears", "oren"), "Guhwai'i"), ("guhwaii_surfplankje", "Surfplankje", ("back", "rug"), "Guhwai'i")]
    kleding = h3("I &middot; Sixteen new clothes", "I &middot; Zestien nieuwe kleertjes") + \
        p("Every story gives its clothes as items (right-click to unlock the piece for all your guhs, like in 2.9). In the Guhdex tab <i>Kleding</i> they are "
          "grouped under <b>Guhverhalen</b>. Each piece has exactly one source.",
          "Elk verhaal geeft zijn kleertjes als voorwerp (rechtsklik om het stuk voor al je guhs te ontgrendelen, zoals in 2.9). In het Guhdex-tabblad "
          "<i>Kleding</i> staan ze bij elkaar onder <b>Guhverhalen</b>. Elk stuk heeft precies een bron.") + \
        table([("", ""), ("Piece", "Kledingstuk"), ("Slot", "Plek"), ("From", "Van")], [[icon(i, n), f"<b>{n}</b>", s, f] for i, n, s, f in KLEREN]) + \
        gallery(fig("guh_outfit_timmerguh", "Timmerguh", "Timmerguh", "", ""), fig("guh_outfit_balto", "Nomguh", "Nomguh", "", ""),
                fig("guh_outfit_mewtwo", "Kloon-eiland", "Kloon-eiland", "", ""), fig("guh_outfit_hemel_back", "Hemelkapelletje", "Hemelkapelletje", "", ""),
                fig("guh_outfit_guhwaii", "Guhwai'i", "Guhwai'i", "", ""), fig("guh_outfit_guhwaii_back", "Guhwai'i (back)", "Guhwai'i (achterkant)", "", ""))

    # ---------------------------------------------------------------- J: Guhdex, advancements, quests, commands
    dex = h3("The Guhdex", "De Guhdex") + \
        gallery(fig("shot30_dex_67_baltoguh", "Baltoguh", "Baltoguh", "", ""), fig("shot30_dex_69_stitch626", "626-guh", "626-guh", "", ""),
                fig("shot30_dex_87_guhxolotl", "Guhxolotl", "Guhxolotl", "", ""), fig("shot30_dex_95_shuckle", "Sjokkel", "Sjokkel", "Page 95.", "Pagina 95.")) + \
        p("29 new pages: the three story guhs (tame yours for a gold star), Mieuwguh, the twelve new characters (Timmerguh, Boris, Steele-Mika, Muk, Luk, Rosy, the "
          "white wolf-guh, Professor Knabbelkloon, the wolkenhoeder, Lilo-guh, Nani-guh, Tikiguh) and the thirteen critters. The Guhdex now has <b>95 pages</b>; "
          "94 of them count for the rewards (the Rookguh page is a bonus page). The critter pages fill in from a bit further away (4 to 8 "
          "blocks), because they're shy.",
          "29 nieuwe pagina's: de drie verhaalguhs (tem de jouwe voor een gouden ster), Mieuwguh, de twaalf nieuwe personages (Timmerguh, Boris, Steele-Mika, Muk, "
          "Luk, Rosy, de witte wolf-guh, Professor Knabbelkloon, de wolkenhoeder, Lilo-guh, Nani-guh, Tikiguh) en de dertien diertjes. De Guhdex heeft nu "
          "<b>95 pagina's</b>; 94 daarvan tellen mee voor de beloningen (de Rookguh-pagina is een bonuspagina). De diertjespagina's vullen zich "
          "al van iets verder weg (4 tot 8 blokken), want ze zijn verlegen.")

    lang = json.load(open(os.path.join("src", "main", "resources", "assets", "guhs", "lang", "nl_nl.json"), encoding="utf-8"))

    def adv_rows(tab, groups):
        rows, total = [], 1
        adv_dir = os.path.join("src", "main", "resources", "data", "guhs", "advancement", tab)
        for pre, en, nl in groups:
            names = []
            for key in lang:
                if not (key.startswith(f"advancements.guhs.{tab}.{pre}_") and key.endswith(".title")):
                    continue
                aid = key[len(f"advancements.guhs.{tab}."):-len(".title")]
                if pre == "balto" and aid.startswith("balto_slee_") or pre == "guhwaii" and aid.startswith("guhwaii_spellen_"):
                    continue
                path = os.path.join(adv_dir, aid + ".json")
                if not os.path.exists(path):
                    continue
                frame = " &#9733;" if json.load(open(path, encoding="utf-8")).get("display", {}).get("frame") == "challenge" else ""
                names.append(html.escape(lang[key]) + frame)
            total += len(names)
            rows.append([f"<b>{t(en, nl)}</b>", " &middot; ".join(names)])
        return rows, total
    vrows, vtotal = adv_rows("verhalen", [("verhaal", "The places", "De plekken"), ("timmerguh", "Timmerguh", "Timmerguh"), ("balto", "Nomguh", "Nomguh"),
                                          ("balto_slee", "The sled", "De slee"), ("mewtwo", "Kloon-eiland", "Kloon-eiland"), ("hemel", "Hemelkapelletje", "Hemelkapelletje"),
                                          ("guhwaii", "Guhwai'i", "Guhwai'i"), ("guhwaii_spellen", "Surfing &amp; hula", "Surfen &amp; hula")])
    drows, dtotal = adv_rows("diertjes", [("vogels", "Birds", "Vogeltjes"), ("waterdiertjes", "Water &amp; insects", "Water &amp; insectjes"),
                                          ("landdiertjes", "Hedgehogs, bunnies, squirrels &amp; Sjokkel", "Egeltjes, konijntjes, eekhoorntjes &amp; Sjokkel")])
    adv = h3("Advancements and quests", "Vooruitgangen en quests") + \
        p(f"Two new advancement tabs (&#9733; = challenge). <b>Guhverhalen</b> has {vtotal} advancements:",
          f"Twee nieuwe vooruitgangentabbladen (&#9733; = uitdaging). <b>Guhverhalen</b> heeft {vtotal} vooruitgangen:") + \
        table([("Part", "Onderdeel"), ("Advancements", "Vooruitgangen")], vrows) + \
        p(f"<b>Diertjes van de Guhmensie</b> has {dtotal} (it opens when you see your first critter):",
          f"<b>Diertjes van de Guhmensie</b> heeft er {dtotal} (het gaat open als je je eerste diertje ziet):") + \
        table([("Part", "Onderdeel"), ("Advancements", "Vooruitgangen")], drows) + \
        p(f"In <b>FTB Quests</b> there are two new chapters. <b>Guhverhalen</b> (<i>Baltoguh, Guhtwo, 626-guh, het Knuffelhart en de Timmerguh. Njeg!</i>) "
          f"has {verhalen_quests} quests in eight sections: Nieuwe plekken &amp; herinneringen, Samen een huisje bouwen, Baltoguh en Nomguh, Door de sneeuwstorm, "
          f"Het kloon-eiland, Het Hemelkapelletje, Ohana op Guhwai'i and Surfen &amp; hula. <b>Diertjes van de Guhmensie</b> (<i>Vinkjes, eendjes, egeltjes "
          f"en... Sjokkel?</i>) has {diertjes_quests} quests in three sections. Nothing is locked.",
          f"In <b>FTB Quests</b> zijn er twee nieuwe hoofdstukken. <b>Guhverhalen</b> (<i>Baltoguh, Guhtwo, 626-guh, het Knuffelhart en de Timmerguh. "
          f"Njeg!</i>) heeft {verhalen_quests} quests in acht onderdelen: Nieuwe plekken &amp; herinneringen, Samen een huisje bouwen, Baltoguh en Nomguh, Door de "
          f"sneeuwstorm, Het kloon-eiland, Het Hemelkapelletje, Ohana op Guhwai'i en Surfen &amp; hula. <b>Diertjes van de Guhmensie</b> (<i>Vinkjes, eendjes, "
          f"egeltjes en... Sjokkel?</i>) heeft {diertjes_quests} quests in drie onderdelen. Niets zit op slot.")
    cmds = h3("Handy commands (creative / cheats on)", "Handige commando's (creatief / cheats aan)") + \
        cmd("/execute in guhs:guhmension run locate structure guhs:nomguh") + cmd("/execute in guhs:guhmension run locate structure guhs:kloon_eiland") + \
        cmd("/execute in guhs:guhmension run locate structure guhs:hemelkapelletje") + cmd("/execute in guhs:guhmension run locate structure guhs:guhwaii_capsule") + \
        cmd("/execute in guhs:guhmension run locate structure guhs:guhwaii_surfstrand") + cmd("/execute in guhs:guhmension run locate biome guhs:sneeuwguhtoendra") + \
        cmd("/execute in guhs:guhmension run locate biome guhs:guhwaii") + cmd('/summon guhs:guh ~ ~ ~ {Variant:"baltoguh"}') + \
        cmd("/summon guhs:shuckle") + cmd("/give @s guhs:timmerguh_bouwboekje") + cmd("/give @s guhs:sneeuwslee") + cmd("/give @s guhs:vadsigheid_scanner")

    body = intro + parts + hero + waar + regels + timmer + balto + slee + mewtwo + hemel + guhwaii + spellen + diertjes + kleding + dex + adv + cmds
    return section("new30", "New in 3.0: Guhverhalen", "Nieuw in 3.0: Guhverhalen", body)


def ol_steps(items):
    """A numbered list of steps (both languages)."""
    return "<ol>" + "".join(f"<li>{t(en, nl)}</li>" for en, nl in items) + "</ol>"


def fixes2101_section():
    """2.10.1 (made before 3.0, part of this release): the Dagboekje button, the Rookguh's own Guhdex page, Moeder Vadsig's text, the Guhkamer
    buttons in the guh menu, more elfstempels, guh-sneeuw in the Guhpolder and Reisguhs in the big places (guhs_30_base/MERGE_2101.md)."""
    shot = lambda name, alt: img("shot30_" + name, alt, "shot")

    def fig(name, en, nl, den, dnl):
        cls = "shot" if name.startswith("shot") else ""
        return f'''<figure><div class="stage">{img(name, en, cls)}</div><figcaption><h3>{t(en, nl)}</h3>
<p>{t(den, dnl)}</p></figcaption></figure>'''
    gallery = lambda *figs: '<div class="gallery">' + "".join(figs) + '</div>'

    intro = p("A few small things from your feedback on 2.10, all part of this release. Thanks, njeg!",
              "Een paar kleine dingen uit jullie feedback op 2.10, allemaal onderdeel van deze versie. Dank je wel, njeg!")
    menu = h3("The guh menu: Dagboekje and the Guhkamer", "Het guhmenu: Dagboekje en de Guhkamer") + \
        gallery(fig("shot30_gui_guhmenu_guhkamer", "The Guhkamer button", "De Guhkamer-knop", "Next to <i>Klaar</i> at the bottom of the guh menu.",
                    "Naast <i>Klaar</i> onderaan het guhmenu."),
                fig("shot30_gui_guhmenu_dagboekje", "Dagboekje", "Dagboekje", "Opens straight on that guh's own page.", "Opent meteen op de eigen pagina van die guh.")) + \
        ul([("<b>Dagboekje</b> now always opens <b>that guh's own page</b> in <i>Mijn guhs</i>, scrolled to the top, whatever tab the Guhdex had open.",
             "<b>Dagboekje</b> opent nu altijd <b>de eigen pagina van die guh</b> in <i>Mijn guhs</i>, bovenaan, welk tabblad de Guhdex ook open had."),
            ("A new button next to <i>Klaar</i>: <b>Logeren in de Guhkamer</b> sends your guh to the Guhkamer in your Guhmaag, just like the Guhbel (is the room "
             "full? then it stays with you). For a guest it says <b>Uit de logeerkamer</b>: it comes to you and follows you again.",
             "Een nieuwe knop naast <i>Klaar</i>: <b>Logeren in de Guhkamer</b> stuurt je guh naar de Guhkamer in je Guhmaag, net als met de Guhbel (is de kamer "
             "vol? dan blijft hij gezellig bij jou). Bij een logeetje staat er <b>Uit de logeerkamer</b>: hij komt naar je toe en gaat weer met je mee.")])
    rook = h3("The Rookguh has its own Guhdex page", "De Rookguh heeft een eigen Guhdex-pagina") + \
        entry(img("rookguh", "Rookguh"), "Rookguhs gered", "Rookguhs gered",
              p("The count of Rookguhs you saved used to sit above the whole Guhdex. Now the <b>Rookguh</b> has its own page (in the Barbecuether part, next to the "
                "Asguh) with <i>&hearts; Rookguhs gered: 7</i> on it. It fills in when you save your first one (saved some before? it's filled in at once). It's a "
                "<b>bonus page</b>: it doesn't count for the Guhdex rewards, so <i>alles verzameld</i> is exactly what it was.",
                "Het aantal Rookguhs dat je hebt gered stond vroeger boven de hele Guhdex. Nu heeft de <b>Rookguh</b> een eigen pagina (bij de Barbecuether, naast de "
                "Asguh) met <i>&hearts; Rookguhs gered: 7</i> erop. Hij vult zich als je je eerste redt (al eerder een paar gered? dan staat hij meteen ingevuld). Het "
                "is een <b>bonuspagina</b>: hij telt niet mee voor de Guhdex-beloningen, dus <i>alles verzameld</i> is precies wat het was."))
    vadsig = h3("Moeder Vadsig", "Moeder Vadsig") + \
        p("Moeder Vadsig no longer tells you to hurry up. When she still needs the guh-taart and the balloons she now says: <i>Njeg, wat wordt dat een vadsig "
          "feestje!</i>",
          "Moeder Vadsig zegt niet meer dat je moet opschieten. Als ze de guh-taart en de ballonnen nog nodig heeft zegt ze nu: <i>Njeg, wat wordt dat een "
          "vadsig feestje!</i>")
    elf = h3("More elfstempels on the Elf-Guhjestocht", "Meer elfstempels op de Elf-Guhjestocht") + \
        p("A tour is a long ride, so it pays more now (the whole shop costs 26):", "Een tocht is een lange rit, dus hij betaalt nu meer (de hele winkel kost 26):") + \
        table([("Time", "Tijd"), ("Elfstempels", "Elfstempels"), ("Was", "Was")],
              [[("any finish", "elke finish"), "<b>12</b>", "5"], [("under 6:00", "onder 6:00"), "+2", "+1"], [("under 5:00", "onder 5:00"), "+2", "+1"],
               [("under 4:15", "onder 4:15"), "+2", "+1"], [("under 3:40", "onder 3:40"), "+2", "+1"],
               [("the very first finish", "de allereerste finish"), "+5 " + t("and the Elf-Guhjeskruisje", "en het Elf-Guhjeskruisje"), "&ndash;"]]) + \
        p("So a fast tour gives up to 20, and your first one up to 25.", "Een snelle tocht geeft dus tot 20, en je eerste tot 25.")
    sneeuw = h3("Guh-sneeuw in the Guhpolder", "Guh-sneeuw in de Guhpolder") + \
        gallery(fig("shot2101_guhsneeuw_ooghoogte", "Guh-sneeuw", "Guh-sneeuw", "Soft, sparse flakes, and now and then a tiny pink guh head.",
                    "Zachte, losse vlokjes, en af en toe een piepklein roze guhkopje."),
                fig("shot2101_guhsneeuw_oost", "No snow piling up", "Geen sneeuw die blijft liggen", "The polder keeps exactly its own snow and ice.",
                    "De polder houdt precies zijn eigen sneeuw en ijs."),
                fig("shot30_guhsneeuw_landschap", "Above the land", "Boven het land", "", "")) + \
        p("When it snows in the Guhpolder you no longer get the busy vanilla snow curtain: a few soft flakes drift down around you, swaying in a light wind and "
          "melting when they land, and one in ten is a <b>tiny pink guh head</b>. The weather also doesn't pile up snow or freeze water there any more, so the "
          "Elf-Guhjestocht and the polder stay exactly as they were built. Everywhere else the weather is normal (the new Sneeuwguhtoendra has its own storms).",
          "Als het sneeuwt in de Guhpolder krijg je niet meer het drukke gewone sneeuwgordijn: er dwarrelen een paar zachte vlokjes om je heen, die wiegen in een "
          "zacht windje en smelten als ze landen, en een op de tien is een <b>piepklein roze guhkopje</b>. Het weer legt daar ook geen sneeuw meer neer en laat "
          "geen water meer bevriezen, dus de Elf-Guhjestocht en de polder blijven precies zoals ze gebouwd zijn. Overal anders is het weer gewoon (de nieuwe "
          "Sneeuwguhtoendra heeft zijn eigen stormen).")
    reis = h3("Reisguhs in the big places", "Reisguhs in de grote plekken") + \
        entry(img("npc_reisguh", "Reisguh"), "More Reisguhs", "Meer Reisguhs",
              p("Besides the one at every guh portal and at the guh castle, a Reisguh now lives in the big places: <b>Guhwarden</b> (on the terrace of the "
                "Elf-Guhjestocht), the <b>Knuffeldal</b> town, the <b>Guhkermis</b> (on the station platform), <b>Guhland</b>, the <b>Ballonfestival</b> and the "
                "<b>Guhcircuit</b> (just inside the Pitpaleis). The 3.0 story places have one too. Wild Reisguhs also turn up a bit more often (and a bit closer "
                "together). Right-click one to discover it, then travel there in one click. (Only in newly generated places.)",
                "Naast die bij elk guhportaal en bij het guhkasteel woont er nu een Reisguh in de grote plekken: <b>Guhwarden</b> (op het terras van de "
                "Elf-Guhjestocht), het <b>Knuffeldal</b>-stadje, de <b>Guhkermis</b> (op het perron), <b>Guhland</b>, het <b>Ballonfestival</b> en het "
                "<b>Guhcircuit</b> (net binnen het Pitpaleis). De 3.0-verhaalplekken hebben er ook een. Wilde Reisguhs duiken ook wat vaker op (en iets dichter bij "
                "elkaar). Rechtsklik er een om hem te ontdekken, en reis er daarna met een klik naartoe. (Alleen in nieuw gemaakte plekken.)"))
    body = intro + menu + rook + vadsig + elf + sneeuw + reis
    return section("fixes2101", "2.10.1: small fixes", "2.10.1: kleine verbeteringen", body)


def build():
    S = []

    # --- getting started ------------------------------------------------------------------------------------------
    S.append(section("start", "Getting started", "Aan de slag", f"""
{p("Guhs is a NeoForge mod for <b>Minecraft 26.1.2</b> (Guhs 1.1.x; Guhs 1.0.x is for Minecraft 1.21.1) that fills the world with chubby pink plush mice: the guhs. "
   "It needs <b>GeckoLib</b> (5.5.2 or newer; 4.8+ on 1.21.1) for the animated models.",
   "Guhs is een NeoForge-mod voor <b>Minecraft 26.1.2</b> (Guhs 1.1.x; Guhs 1.0.x is voor Minecraft 1.21.1) die de wereld vult met mollige roze knuffelmuisjes: de guhs. "
   "Hij heeft <b>GeckoLib</b> (5.5.2 of nieuwer; 4.8+ op 1.21.1) nodig voor de geanimeerde modellen.")}
{ul([("Put <code>guhs-" + VERSION + ".jar</code> in the <code>mods</code> folder of a NeoForge 26.1.2 instance (in Prism: right-click the instance, <i>Folder</i>).",
      "Zet <code>guhs-" + VERSION + ".jar</code> in de map <code>mods</code> van een NeoForge 26.1.2-instantie (in Prism: rechtsklik op de instantie, <i>Map</i>)."),
     ("Also add GeckoLib for NeoForge 26.1.2 (Prism: <i>Edit</i> &rarr; <i>Mods</i> &rarr; <i>Download mods</i>).",
      "Voeg ook GeckoLib voor NeoForge 26.1.2 toe (Prism: <i>Bewerken</i> &rarr; <i>Mods</i> &rarr; <i>Mods downloaden</i>)."),
     ("Multiplayer: the server and every player need the mod.",
      "Multiplayer: de server en alle spelers hebben de mod nodig."),
     ("Everything is in the creative tab <b>Guhs</b>.", "Alles staat in het creatieve tabblad <b>Guhs</b>."),
     ("Every name and text in the game is in <b>English and Dutch</b>. Guhs follows your Minecraft language (Dutch when Minecraft is in Dutch, "
      "English for every other language). To choose yourself, use the <b>Lang</b> button in the guh menu, or <i>Guhs language</i> in the mod's "
      "settings (<i>Mods</i> &rarr; <i>Guhs</i> &rarr; <i>Config</i>; in the file: <code>language</code> in <code>config/guhs-client.toml</code>): "
      "Auto, NL or EN. Every player chooses for themselves, also on a server.",
      "Alle namen en teksten in het spel zijn er in het <b>Nederlands en het Engels</b>. Guhs volgt de taal van Minecraft (Nederlands als Minecraft "
      "Nederlands is, anders Engels). Zelf kiezen kan met de knop <b>Taal</b> in het guhmenu, of met <i>Taal van Guhs</i> in de instellingen van de mod "
      "(<i>Mods</i> &rarr; <i>Guhs</i> &rarr; <i>Config</i>; in het bestand: <code>language</code> in <code>config/guhs-client.toml</code>): "
      "Auto, NL of EN. Elke speler kiest voor zichzelf, ook op een server."),
     ("<b>FTB Quests</b> in your pack? Then a <b>Guhs</b> chapter group is added to the quest book automatically: thirteen chapters with "
      + str(quest_count()) + " quests (every guh kind, every structure and biome, the stomach, the minigames, the Guheinde, the Barbecuether, the Knuffeldal, the pieppiepmuisjes, De Grote Guhspelen, your guh's hearts and huisje, the Guhverhalen and the critters...). "
      "Each chapter starts with a <i>Hoe kom je hier?</i> quest that links to where it begins.",
      "<b>FTB Quests</b> in je pack? Dan komt er vanzelf een groep <b>Guhs</b> in het questboek: dertien hoofdstukken met " + str(quest_count()) + " quests "
      "(elke guhsoort, elk bouwwerk en bioom, de maag, de minigames, het Guheinde, de Barbecuether, het Knuffeldal, de pieppiepmuisjes, De Grote Guhspelen, de hartjes en het huisje van je guh, de Guhverhalen en de diertjes...). Elk hoofdstuk begint met een "
      "<i>Hoe kom je hier?</i>-quest met een linkje naar waar het begint.")])}
"""))

    # --- new in 3.0: Guhverhalen, and 2.10.1 ------------------------------------------------------------------------------
    S.append(verhalen30_section())
    S.append(fixes2101_section())

    # --- new in 2.10: Lieve vadsjes van elkaar, and the fixes ------------------------------------------------------------
    S.append(lieve_vadsjes_section())
    S.append(fixes210_section())

    # --- new in 2.9: De Grote Guhspelen -------------------------------------------------------------------------------------
    S.append(grote_guhspelen_section())

    # --- new in 2.8.1: Piep! ---------------------------------------------------------------------------------------------
    S.append(piep_section())

    # --- new in 2.8: het Knuffeldal ---------------------------------------------------------------------------------------
    S.append(knuffel_section())

    # --- new in 2.7: de Guhbarbecuether, the Gatenkaasgrotten, the Kaasmoeras and the Vadswoud -------------------------
    S.append(barbecuether_section())

    # --- new in 2.6: the Guheinde ---------------------------------------------------------------------------------------
    S.append(guheinde_section())

    # --- new in 2.5: events, emotes, the Guhbubbel, music ------------------------------------------------------------------------
    new25 = entry(img("guh_outfit_evenementen", "Parade outfit"), "Guh events", "Guh-evenementen",
                  p('<b>Guh events</b>: every 2 to 3 Minecraft days you spend in the Guhmension, something happens around you outdoors. The chat announces it and a bar at the top shows the time left. In a <b>kaasregen</b> kaasknabbels rain from the sky for a minute: walk into them to catch them, and look out for the rare <b>golden kaasknabbel</b>, which tames any wild guh at once. Wild guhs come running for the knabbels too, and a guh that ate one is happy and much easier to tame. The <b>Vadsparade</b> marches by with the drumming <b>Tamboerguh</b> and ten dressed-up guhs. Walk along until the very end for confetti and a piece of the <b>parade outfit</b> for your guh (sjako, jacket, little drum), which you can only get from the parade. At night a <b>sterrenregen</b> can fall: pick up the <b>stardust</b> where the stars land, and where a <b>starry guh</b> appears, feed it quickly before it twinkles back up to the stars! (Operators can start one with <code>/guhs evenement kaasregen|parade|sterrenregen</code>.)',
                    "<b>Guh-evenementen</b>: ongeveer eens per 2 à 3 Minecraft-dagen in de Guhmensie gebeurt er buiten iets bij jou in de buurt. Je ziet het in de chat, en een balk bovenin laat zien hoe lang het nog duurt. Bij een <b>kaasregen</b> vallen er een minuut lang kaasknabbels uit de lucht: loop ertegenaan om ze te vangen, en let op de zeldzame <b>gouden kaasknabbel</b>, die elke wilde guh meteen tam maakt. De wilde guhs rennen ook op de knabbels af, en een guh die er een heeft opgesmuld is blij en veel makkelijker te temmen. De <b>Vadsparade</b> marcheert voorbij met de trommelende <b>Tamboerguh</b> en tien verklede guhs. Loop mee tot het allerlaatste eind voor confetti en een stuk van het <b>paradepakje</b> voor je guh (sjako, jasje, trommeltje), dat je alleen bij de parade krijgt. 's Nachts kan er een <b>sterrenregen</b> vallen: raap het <b>sterrenstof</b> op waar de sterren landen, en verschijnt er een <b>sterrenguh</b>, voer hem dan snel, voordat hij terug de sterren in twinkelt! (Operators starten er een met <code>/guhs evenement kaasregen|parade|sterrenregen</code>.)") +
                  '<p>' + ' '.join([icon("gouden_kaasknabbel", "Gouden kaasknabbel"), icon("sterrenstof", "Sterrenstof"),
                                    icon("vadsparade_sjako", "Sjako"), icon("vadsparade_jasje", "Paradejasje"),
                                    icon("vadsparade_trommeltje", "Trommeltje")]) + '</p>', wide=True) + \
        entry(img("guh", "A guh"), "Guh emotes", "Guh-emotes", p("<b>Guh emotes</b>: hold right-click on your guh and press <b>Emotes</b>. Your guh can wave, dance, sleep (softly snoring, with little zzz's), do a <b>VAHOEG jump</b>, roll onto its back, munch an imaginary kaasknabbel (crumbs everywhere!) or be shy, paws in front of its eyes. Choose <b>Now</b> to do it once or <b>Keep going</b> to repeat it until you press Stop. Tap the star to make it the guh's <b>favourite emote</b>: it will do that one by itself now and then. Wild guhs have emotes too: now and then one that fits their personality, a wave when you walk up to them (shy guhs hide their eyes if you bring kaasknabbels), and every guh nearby starts dancing when a jukebox plays. Guhs never emote while ridden or swimming, and getting hurt stops them straight away.", "<b>Guh-emotes</b>: houd rechtsklik op je guh en druk op <b>Emotes</b>. Je guh kan zwaaien, dansen, slapen (zachtjes snurkend, met kleine zzz'tjes), een <b>VAHOEG-sprong</b> maken, op zijn rug rollen, smakken op een denkbeeldige kaasknabbel (overal kruimels!) of verlegen doen met zijn pootjes voor zijn ogen. Kies <b>Nu</b> om het één keer te doen of <b>Blijven doen</b> om het te herhalen tot je op Stop drukt. Klik op het sterretje om er zijn <b>lievelingsemote</b> van te maken: die doet hij af en toe uit zichzelf. Wilde guhs doen ook emotes: af en toe eentje die bij hun karakter past, ze zwaaien als je naar ze toe loopt (verlegen guhs verstoppen hun ogen als je kaasknabbels bij je hebt) en alle guhs in de buurt gaan dansen als er een jukebox speelt. Guhs doen geen emotes terwijl je erop rijdt of als ze zwemmen, en pijn stopt ze meteen.")) + \
        entry(img("structure_onderwater", "The Guhbubbel"), "The Guh Bubble", "De Guhbubbel",
              p("<b>The Guh Bubble</b>: in the deep middle of every <b>Diepe Guhzee</b> (since 2.7, see there; exactly one per sea) lies a giant glass bubble shaped like a guh head on the sea floor. Walk into the <b>Duikpost</b> on its little island through its mouth and take the spiral staircase down to the sea floor (the bubble lift brings you back up). Inside there's air, a garden of glowing <b>kaaskoraal</b>, and the <b>Zeemeerguh</b> with her diving shop. Pay with <b>pearls</b> from the <b>giant guh shells</b>: an open shell holds a pearl, a closed one grows a new pearl after a while. She sells the <b>duikhelm</b> (breathe under water), the <b>diving outfit</b> for your guh (goggles, snorkel, swim ring; only sold here), a saddle, and kaaskoraal and mother-of-pearl blocks to build with. Swim out through a dive door: kaaskoraal blows air bubbles that let you breathe again, and just north of the bubble lies a <b>sunken guh ship</b> with a treasure chest. The <b>super compass</b> (Wonders) shows the way to the nearest one.", '<b>De Guhbubbel</b>: in het diepe midden van elke <b>Diepe Guhzee</b> (sinds 2.7, zie daar; precies één per zee) ligt op de zeebodem een reusachtige glazen bubbel in de vorm van een guhhoofd. Loop door de mond de <b>Duikpost</b> op zijn eilandje binnen en neem de wenteltrap naar de zeebodem (de bubbellift brengt je weer omhoog). Binnen is er lucht, een tuin vol gloeiend <b>kaaskoraal</b> en de <b>Zeemeerguh</b> met haar duikwinkeltje. Je betaalt met <b>parels</b> uit de <b>reuzenschelpen</b>: in een open schelp zit een parel, een dichte schelp krijgt na een tijdje een nieuwe. Ze verkoopt de <b>duikhelm</b> (ademen onder water), het <b>duikpakje</b> voor je guh (duikbril, snorkel en zwemband, alleen hier te koop), een zadel, en kaaskoraal en parelmoer om mee te bouwen. Zwem door een duikdeur naar buiten: kaaskoraal blaast luchtbelletjes waarmee je weer kunt ademen, en vlak ten noorden van de bubbel ligt een <b>gezonken guhschip</b> met een schatkist. Het <b>superkompas</b> (Wonderen) wijst de weg naar de dichtstbijzijnde.') +
              '<p>' + ' '.join([icon("parel", "Parel"), icon("duikhelm", "Duikhelm"), icon("duikbril", "Duikbril"),
                                icon("snorkel", "Snorkel"), icon("zwemband", "Zwemband")]) + '</p>', wide=True) + \
        entry(img("guh_variant_zeemeerguh", "Zeemeerguh"), "The Zeemeerguh", "De Zeemeerguh", p('In the Guh Sea and the Diepe Guhzee you may meet a wild <b>Zeemeerguh</b>, a guh with a fish tail that never drowns and swims fast. Tame it with kaasknabbels, saddle it and ride it under water: it goes where you look (jump = up, sprint = faster) and keeps you breathing.', 'In de Guhzee en de Diepe Guhzee zwemt soms een wilde <b>Zeemeerguh</b>, een guh met een vissenstaart die nooit verdrinkt en snel zwemt. Tem hem met kaasknabbels, zadel hem en rijd onder water: hij gaat waar je kijkt (springen = omhoog, sprinten = sneller) en jij blijft ademen.')) + \
        entry(img("guh_outfit_onderwater", "Diving outfit"), "Diving outfit", "Duikpakje",
              p("Only from the Zeemeerguh: goggles, snorkel and swim ring.", "Alleen bij de Zeemeerguh: duikbril, snorkel en zwemband.")) + \
        entry(img("icon_music_disc_ze_hangen", "Guh music disc"), "Guh music", "Guhmuziek", p('<b>Guh music</b>: at the guh picnic a jukebox plays the guh record <i>Ze hangen aan me veh</i> as soon as you come near. Take the record out to play it at home (and every guh nearby starts dancing). Guhs also have two new noises: <i>nyehnyeh</i> and <i>nnnnyeeeehhh</i>.', '<b>Guhmuziek</b>: bij de guh-picknick speelt een jukebox de guhplaat <i>Ze hangen aan me veh</i> zodra je in de buurt komt. Haal de plaat eruit om hem thuis te draaien (en alle guhs in de buurt gaan dansen). Guhs hebben ook twee nieuwe geluidjes: <i>nyehnyeh</i> en <i>nnnnyeeeehhh</i>.'))
    S.append(section("new25", "New in 2.5: events, emotes and the Guh Bubble", "Nieuw in 2.5: evenementen, emotes en de Guhbubbel", new25))

    # --- new in 2.4: seven minigames and three rare places --------------------------------------------------------------------
    new24 = p("Seven new <b>minigames</b> in the Guhmension, each in its own big guh building with its own guh, its own tickets and "
              "an outfit you can only get there. You never need to bring anything: the guh lends you what you need. Every game has a "
              "floating <b>top 3 of the world</b> and keeps your personal record (all of them together in the Guhdex, since 2.9 in the tab <b>Minigames</b>, and since 2.9 with makkelijk, medium and lastig; the rewards "
              "went up too, see <i>New in 2.7</i>). The <b>super compass</b> (Minigames) finds them all. "
              "Plus three very rare places to discover (Wonders / Adventure).",
              "Zeven nieuwe <b>minigames</b> in de Guhmensie, elk in een eigen groot guhgebouw met een eigen guh, eigen tickets en een "
              "pakje dat je alleen daar krijgt. Je hoeft nooit iets mee te nemen: de guh leent je wat je nodig hebt. Elk spel heeft een "
              "zwevende <b>top 3 van de wereld</b> en onthoudt je eigen record (allemaal bij elkaar in de Guhdex, sinds 2.9 in het tabblad <b>Minigames</b>, en sinds 2.9 met makkelijk, medium en lastig; de beloningen "
              "gingen ook omhoog, zie <i>Nieuw in 2.7</i>). Het <b>superkompas</b> (Minigames) vindt ze allemaal. "
              "En drie heel zeldzame plekken om te ontdekken (Wonderen / Avontuur).") + \
        entry(img("structure_guh_beauty_theater", 'Guh Beauty: the Vads Contest'), 'Guh Beauty: the Vads Contest', 'Guh Beauty: de Vads-wedstrijd',
              p("The <b>Guh Beauty Theater</b> is a large, rare catwalk theatre in the Guhmension; its front is a giant guh face and the door is its mouth. Talk to the <b>Showguh</b> on the stage to join the <b>Vads Contest</b>. You don't need to bring anything: the Showguh gives you a model guh, or you can let <b>your own tamed guh</b> walk (it gets its own clothes back afterwards). The show has three rounds, each with a theme such as Winter Fun, Gala or Dutch Weather. Dress the model from the <b>loaner wardrobe</b> (head, eyes, neck and body), and it walks the catwalk to the jury. Each of the three jury guhs holds up a <b>score sign</b> (1-10) and says why in chat: <b>Miss Vadsma</b> judges whether every piece fits the theme, <b>Mister Glitterguh</b> wants a complete look with pieces that belong together, and <b>Granny Knabbel</b> scores with her heart. Points earn <b>show rosettes</b>, which buy the Showster tiara, the Miss Vadsig sash and the glitter bow in the Showguh's shop. After every show your personal record appears in chat, and the world's <b>top 3</b> floats above the Showguh.",
                'Het <b>Guh Beauty Theater</b> is een groot, zeldzaam catwalktheater in de Guhmension. De gevel is een reuzenguh, en de deur is zijn mond. Praat met de <b>Showguh</b> op het podium om mee te doen aan de <b>Vads-wedstrijd</b>. Je hoeft zelf niks mee te nemen: de Showguh geeft je een model, of je laat <b>je eigen tamme guh</b> lopen (die krijgt na afloop zijn eigen kleertjes terug). De show heeft drie rondes, elk met een thema zoals Winterpret, Gala of Hollands Weer. Kleed het model aan uit de <b>leenkledingkast</b> (hoofd, ogen, nek en lijf), en het loopt over de catwalk naar de jury. Elk van de drie juryleden houdt een <b>puntenbordje</b> (1-10) omhoog en zegt in de chat waarom: <b>Juf Vadsma</b> kijkt of elk kledingstuk bij het thema past, <b>Meneer Glitterguh</b> wil een complete look met kleertjes die bij elkaar horen, en <b>Oma Knabbel</b> geeft punten met haar hart. Punten leveren <b>showrozetten</b> op, waarmee je in de winkel van de Showguh de Showster-tiara, de Miss Vadsig-sjerp en de glitterstrik koopt. Na elke show zie je je persoonlijke record in de chat, en boven de Showguh zweeft de <b>top 3</b> van de wereld.') +
              '<p>' + ' '.join([icon('showrozet', 'Showrozet'), icon('showster_tiara', 'Showster-tiara'), icon('showster_sjerp', 'Miss Vadsig-sjerp'), icon('showster_strik', 'Glitterstrik')]) + '</p>', wide=True) + \
        entry(img("guh_outfit_beauty", 'Showster outfit'), 'Showster outfit', 'Showster-pakje',
              p("Only here: " + ", ".join(['Showster-tiara', 'Miss Vadsig-sjerp', 'Glitterstrik']) + ".",
                "Alleen hier: " + ", ".join(['Showster-tiara', 'Miss Vadsig-sjerp', 'Glitterstrik']) + ".")) + \
        entry(img("structure_guh_racebaan", 'Guhrace'), 'Guhrace', 'Guhrace',
              p("At the <b>Guh Racebaan</b> (a rare 100-block race circuit in the Guhmension) the <b>Raceguh</b> lends you a rental race guh. You ride it yourself: <b>W</b> to run, mouse to steer, <b>S</b> to brake. Race 3 laps through all 6 glowing rings in order, and hit the golden <b>VAHOEG pads</b> for a boost. Cut across the grass, fall off or land in the kaas saus and you're sent back to the last ring. Faster times earn gold, silver or bronze and more <b>raceprijsjes</b>, which you spend on the jockey outfit (sold only here). After every race you see your personal best in chat, and your best race drives along as a see-through <b>ghost</b>. A floating <b>top-3 board</b> above the Raceguh shows the fastest races and fastest laps of the whole world. Place 1 holds the track record!",
                'Op de <b>Guhracebaan</b> (een zeldzaam racecircuit van 100 blokken in de Guhmensie) leent de <b>Raceguh</b> je een huur-renguh. Die stuur je helemaal zelf: <b>W</b> om te rennen, muis om te sturen, <b>S</b> om te remmen. Race 3 rondes door alle 6 lichtgevende ringen, op volgorde, en pak de gouden <b>VAHOEG-pads</b> voor een zet. Afsnijden over het gras, eraf vallen of in de kaas saus belanden? Dan ga je terug naar de laatste ring. Hoe sneller je bent, hoe meer <b>raceprijsjes</b> (goud, zilver, brons), en die geef je uit aan het jockeypakje (alleen hier te koop). Na elke race zie je je persoonlijk record in de chat, en je beste race rijdt doorzichtig met je mee als <b>geest</b>. Boven de Raceguh zweeft een <b>top-3 scorebord</b> met de snelste races en snelste rondes van de hele wereld. Wie op plek 1 staat, heeft het baanrecord. VAHOEG!') +
              '<p>' + ' '.join([icon('raceprijsje', 'Raceprijsje'), icon('jockey_pet', 'Jockeypetje'), icon('jockey_jasje', 'Gestreept zijden jockeyjasje'), icon('racebril', 'Vahoege racebril')]) + '</p>', wide=True) + \
        entry(img("guh_outfit_race", 'Jockey outfit'), 'Jockey outfit', 'Jockeypakje',
              p("Only here: " + ", ".join(['Jockeypetje', 'Gestreept zijden jockeyjasje', 'Vahoege racebril']) + ".",
                "Alleen hier: " + ", ".join(['Jockeypetje', 'Gestreept zijden jockeyjasje', 'Vahoege racebril']) + ".")) + \
        entry(img("structure_mika_mep_hal", 'Mika Whacking'), 'Mika Whacking', 'Mika meppen',
              p("In the rare <b>Mika Whack Hall</b> (a fairground hall behind a giant guh-face facade, about as rare as the kermis) lives the <b>Whack Guh</b>. Talk to him and you get a loaned Mika whacker. You stand in the middle of the <b>4x4 board</b>, and for 60 seconds Mikas pop out of the holes, faster and faster. Left-click them: a Mika is 10 points and a <b>golden Mika</b> 50, both times your combo (every 5 in a row, up to x5). Sometimes a <b>guh</b> pops up. <b>Don't whack it</b>: that's -25 and your combo is gone. Let it go and you get +5. After the game you see your personal best in the chat and get <b>whack coins</b>, which buy the Mika hunter outfit for your guh (only sold here). The <b>top 3 whackers of the world</b> float on the scoreboard wall of the hall. VAHOEG!",
                "In de zeldzame <b>Mika-mephal</b> (een kermishal achter een gevel met een reuzenguhgezicht, ongeveer zo zeldzaam als de kermis) woont de <b>Mepguh</b>. Praat met hem en je krijgt een geleende Mika-mephamer. Je staat midden op het <b>4x4 mepbord</b>, en 60 seconden lang ploppen de Mika's steeds sneller uit de gaten. Linksklik ze: een Mika is 10 punten en een <b>gouden Mika</b> 50, allebei keer je combo (elke 5 op rij, tot x5). Soms plopt er een <b>guh</b> op. <b>Niet meppen</b>: dat is -25 en je combo is weg. Laat je hem met rust, dan krijg je +5. Na het potje zie je je persoonlijke record in de chat en krijg je <b>mepmunten</b>, waarmee je het Mika-jagerpakje voor je guh koopt (alleen hier te koop). De <b>top 3 meppers van de wereld</b> zweven bij de scorebordmuur van de hal. VAHOEG!") +
              '<p>' + ' '.join([icon('mepmunt', 'Mepmunt'), icon('mikajager_hoed', 'Mika-jagershoed'), icon('mikajager_vest', 'Mepvest met zakjes'), icon('mikamepper_medaille', 'Mika-mepper-medaille')]) + '</p>', wide=True) + \
        entry(img("guh_outfit_meppen", 'Mika hunter outfit'), 'Mika hunter outfit', 'Mika-jagerpakje',
              p("Only here: " + ", ".join(['Mika-jagershoed', 'Mepvest met zakjes', 'Mika-mepper-medaille']) + ".",
                "Alleen hier: " + ", ".join(['Mika-jagershoed', 'Mepvest met zakjes', 'Mika-mepper-medaille']) + ".")) + \
        entry(img("structure_guh_disco", 'Guhdisco'), 'Guhdisco', 'Guhdisco',
              p("The <b>Guhdisco</b> is a rare pink club in the Guhmension, with a giant guh face as its front, set in big grounds (96x84) with a neon <b>DISCO</b> arch, a shake terrace and a stable to park your guh. Talk to the <b>DJ Guh</b> in the mouth of the big guh head on the stage and dance <b>Simon says</b>: the tiles flash in a row of colours, each with its own note, and you step on the same colours in the same order. Every round adds one colour and gets faster. <b>One wrong step (or waiting too long) and the music stops: no second chances!</b> You need no items, and you can't get hurt or hungry while dancing. You earn <b>disco coins</b> (from 3 colours: 1 per 2 colours plus 1, and extra from 10 and 15 colours). After every game the chat shows your <b>personal record</b>, and the best dancers of the world stand on the floating <b>top 3</b> above the stage. Spend your coins on the disco outfit (glitter vads suit, groovy afro, star glasses), which you can only get here. VAHOEG!",
                "De <b>Guhdisco</b> is een zeldzame roze club in de Guhmensie met een reuzeguhgezicht als voorgevel. Hij staat op een groot terrein (96x84) met een neon <b>DISCO</b>-boog, een shaketerras en een stal om je guh te parkeren. Praat met de <b>DJ-guh</b> in de mond van de grote guhkop op het podium en dans <b>'Simon zegt'</b>: de tegels flitsen een rij kleuren, elk met een eigen toon, en jij stapt op dezelfde kleuren in dezelfde volgorde. Elke ronde komt er een kleur bij en gaat het sneller. <b>Een verkeerde stap (of te lang wachten) en de muziek stopt: geen tweede kans!</b> Je hebt niks nodig, en tijdens het dansen krijg je geen honger of schade. Je verdient <b>discomunten</b> (vanaf 3 kleuren: 1 per 2 kleuren plus 1, en extra vanaf 10 en 15 kleuren). Na elk potje zie je in de chat je <b>persoonlijk record</b>, en de beste dansers van de wereld staan in de zwevende <b>top 3</b> boven het podium. Besteed je munten aan het discopakje (glittervadspak, vahoege afropruik, sterren-discobril), dat je alleen hier krijgt. VAHOEG!") +
              '<p>' + ' '.join([icon('discomunt', 'Discomunt'), icon('disco_glitterpak', 'Glittervadspak'), icon('disco_afro', 'Vahoege afropruik'), icon('disco_bril', 'Sterren-discobril')]) + '</p>', wide=True) + \
        entry(img("guh_outfit_disco", 'Disco outfit'), 'Disco outfit', 'Discopakje',
              p("Only here: " + ", ".join(['Glittervadspak', 'Vahoege afropruik', 'Sterren-discobril']) + ".",
                "Alleen hier: " + ", ".join(['Glittervadspak', 'Vahoege afropruik', 'Sterren-discobril']) + ".")) + \
        entry(img("structure_guh_golfbaan", 'Guhgolf'), 'Guhgolf', 'Guhgolf',
              p('The <b>Guh Golf Course</b> is a rare minigame in the Guhmension (about 96 blocks across). The <b>Golfguh</b> lives in the clubhouse with the guh face. She lends you a club for a round of 9 holes, with at most 8 strokes per hole. <b>Hold right-click</b> near your ball to charge the power bar: the longer you hold, the harder you hit, up to 100%. The ball goes the way you look, and you <b>let go to hit</b>. Kaassaus or going off the course costs a penalty stroke. Watch out for the windmill, the Vahoegschans and the Kaasknabbelheuvel! Since 2.10 every hole has three tees: green for makkelijk, pink for medium and red for lastig (see <i>Fixes in 2.10</i>). The fewer strokes you take, the more <b>golfballetjes</b> you get. You spend them in her shop on the golf outfit (flat cap, sun visor and argyle sweater). After every round your personal best shows up in chat. The <b>top 3 of the whole world</b> floats above the Golfguh.',
                "De <b>Guhgolfbaan</b> is een zeldzame minigame in de Guhmension (zo'n 96 blokken breed). In het clubhuis met het guhgezicht woont de <b>Golfguh</b>. Zij leent je een club voor een rondje van 9 holes, met maximaal 8 slagen per hole. <b>Houd rechtsklik ingedrukt</b> bij je bal om de krachtbalk op te laden: hoe langer je vasthoudt, hoe harder je slaat, tot 100%. De bal gaat de kant op waar je kijkt, en je <b>laat los om te slaan</b>. Kaassaus of buiten de baan kost een strafslag. Pas op voor de guhmolen, de Vahoegschans en de Kaasknabbelheuvel! Sinds 2.10 heeft elke hole drie afslagen: groen voor makkelijk, roze voor medium en rood voor lastig (zie <i>Fixes in 2.10</i>). Hoe minder slagen, hoe meer <b>golfballetjes</b>. Die geef je in haar winkeltje uit aan het golfpakje (vadsige golfpet, guhzonneklep en ruitjesvadstrui). Na elk rondje zie je je persoonlijke record in de chat. De <b>top 3 van de hele wereld</b> zweeft boven de Golfguh. VAHOEG!") +
              '<p>' + ' '.join([icon('golfballetje', 'Golfballetje'), icon('golf_pet', 'Vadsige golfpet'), icon('golf_zonneklep', 'Guhzonneklep'), icon('golf_trui', 'Ruitjesvadstrui')]) + '</p>', wide=True) + \
        entry(img("guh_outfit_golf", 'Golf outfit'), 'Golf outfit', 'Golfpakje',
              p("Only here: " + ", ".join(['Vadsige golfpet', 'Guhzonneklep', 'Ruitjesvadstrui']) + ".",
                "Alleen hier: " + ", ".join(['Vadsige golfpet', 'Guhzonneklep', 'Ruitjesvadstrui']) + ".")) + \
        entry(img("structure_vadsig_eetfestijn", 'The Vadsig Food Festival'), 'The Vadsig Food Festival', 'Het Vadsig eetfestijn',
              p("The <b>Vadsig eetfestijn</b> is a rare, huge guh food festival (96 blocks wide) in the Guhmension. In its striped tent the <b>Smulguh</b> lends you her big <b>smulschaal</b>: for one minute food rains down from the sky and out of the chutes, and you walk around the arena freely to catch it. Kaasknabbel 1, cupcake 2, macaron and milkshake 3, guh cake 5, and a <b>golden smulknabbel</b> 10 plus double points for a while. Five in a row is combo x2, ten is x3. Watch out for the dark <b>Mika-vet</b>: it costs 5 points and your combo, and makes you slow! Afterwards you get <b>smulmunten</b>, hear your personal record in the chat, and the bowl goes back. The <b>top 3 of the whole world</b> floats above the Smulguh's head. Spend smulmunten on the smul outfit (bib, baker's hat, pink apron), which is only sold here.",
                'Het <b>Vadsig eetfestijn</b> is een zeldzaam, reusachtig guh-smulfeest (96 blokken breed) in de Guhmensie. In de gestreepte tent leent de <b>Smulguh</b> je haar grote <b>smulschaal</b>: één minuut lang regent het eten uit de lucht en uit de trechters, en jij loopt vrij door de arena om het te vangen. Kaasknabbel 1, cupcake 2, macaron en milkshake 3, guhtaart 5, en een <b>gouden smulknabbel</b> 10 plus een tijdje dubbele punten. Vijf op rij is combo x2, tien op rij x3. Pas op voor het zwarte <b>Mika-vet</b>: dat kost 5 punten en je combo, en je wordt er traag van! Na afloop krijg je <b>smulmunten</b>, hoor je in de chat je eigen record, en gaat de schaal terug. De <b>top 3 van de hele wereld</b> zweeft boven het hoofd van de Smulguh. Met smulmunten koop je het smulpakje (slabbetje, bakkersmuts, roze schort), dat je alleen hier kunt kopen. VAHOEG!') +
              '<p>' + ' '.join([icon('smulmunt', 'Smulmunt'), icon('smul_slabbetje', 'Smulslabbetje'), icon('smul_bakkersmuts', 'Vadsige bakkersmuts'), icon('smul_schort', 'Roze smulschort')]) + '</p>', wide=True) + \
        entry(img("guh_outfit_smul", 'Smul outfit'), 'Smul outfit', 'Smulpakje',
              p("Only here: " + ", ".join(['Smulslabbetje', 'Vadsige bakkersmuts', 'Roze smulschort']) + ".",
                "Alleen hier: " + ", ".join(['Smulslabbetje', 'Vadsige bakkersmuts', 'Roze smulschort']) + ".")) + \
        entry(img("structure_guhvis_vijver", 'The Guhfish Contest'), 'The Guhfish Contest', 'De Guhvis-wedstrijd',
              p("At the <b>Guhfish Pond</b> (guhvis_vijver, a large pond about 96 blocks across in the Guhmension that is rare, like the fair), the <b>Visguh</b> runs the <b>Guhfish contest</b>. She lends you a rod, and you fish her pond for 3 minutes. Every bite is a special fish: Cheese Fish, Chubby Perch, Guh Puffer, Pink Njeg Trout, the <b>Mika Catfish</b> (costs points!) and the legendary <b>Golden Guhfish</b>. The heavier the fish, the more points. <b>You keep every fish</b>: they're edible trophies, and each Golden Guhfish is a real collector's piece that shines and carries your name, its weight and its number. Friends can join, and the winner gets bonus vouchers. Your points turn into <b>visbonnen</b>, which buy the angler outfit for your guh (only sold here). After every contest the chat shows your personal best. The <b>world top 3</b> (most points, heaviest fish ever) floats above the record board at the Visguh's desk. VAHOEG!",
                "Bij de <b>Guhvisvijver</b> (guhvis_vijver, een grote vijver van zo'n 96 blokken breed in de Guhmensie, net zo zeldzaam als de kermis) houdt de <b>Visguh</b> de <b>Guhvis-wedstrijd</b>. Je leent haar hengel en vist 3 minuten in haar vijver. Elke beet is een speciale vis: kaasvis, vadsbaars, guhpuffer, roze njegforel, de <b>Mika-meerval</b> (kost punten!) en de legendarische <b>Gouden Guhvis</b>. Hoe zwaarder de vis, hoe meer punten. <b>Alle vissen mag je houden</b>: je kunt ze opeten of bewaren als trofee, en elke Gouden Guhvis is een echt verzamelstuk dat glimt en jouw naam, zijn gewicht en een nummer draagt. Vrienden kunnen meedoen en de winnaar krijgt extra visbonnen. Punten worden <b>visbonnen</b>, en daarmee koop je het vadsige vispakje voor je guh (alleen hier te koop). Na elke wedstrijd zie je je persoonlijke record in de chat. De <b>top 3 van de wereld</b> (meeste punten en vadsigste vis ooit) zweeft boven het recordbord bij de Visguh. VAHOEG!") +
              '<p>' + ' '.join([icon('visbon', 'Visbon'), icon('vissershoedje', 'Vadsig vissershoedje'), icon('visvest', 'Guhvisvest'), icon('vis_aan_de_haak', 'Vis-aan-de-haak'), icon('kaasvis', 'Kaasvis'), icon('vadsbaars', 'Vadsbaars'), icon('guhpuffer', 'Guhpuffer'), icon('njegforel', 'Roze njegforel'), icon('mika_meerval', 'Mika-meerval'), icon('gouden_guhvis', 'Gouden Guhvis')]) + '</p>', wide=True) + \
        entry(img("guh_outfit_vissen", 'Angler outfit'), 'Angler outfit', 'Visserspakje',
              p("Only here: " + ", ".join(['Vadsig vissershoedje', 'Guhvisvest', 'Vis-aan-de-haak']) + ".",
                "Alleen hier: " + ", ".join(['Vadsig vissershoedje', 'Guhvisvest', 'Vis-aan-de-haak']) + "."))
    S.append(section("new24", "New in 2.4: seven minigames", "Nieuw in 2.4: zeven minigames", new24))
    rare24 = entry(img("guh_variant_wolk", "Wolkguh"), "The Wolkguh", "De Wolkguh",
                   p("On the floating islands sits the only <b>Wolkguh</b> in the world: white and fluffy with a little cloud on its "
                     "head. It floats gently when it falls. Taming it takes patience!",
                     "Op de zwevende eilandjes zit de enige <b>Wolkguh</b> van de wereld: wit en pluizig met een wolkje op zijn kop. "
                     "Hij zweeft zachtjes als hij valt. Temmen vraagt geduld!")) + \
        entry(img("structure_zwevende_eilanden", 'The floating guh islands'), 'The floating guh islands', 'De zwevende guh-eilandjes',
              p("High in the Guhmension sky float the <b>floating guh islands</b> (very rare). Step onto the <b>wolkenlift</b> on the guh-face square below and VAHOEG, up into the clouds! On the main island, in a nest of wool, lives the one and only <b>Wolkguh</b>: white and fluffy with a sky-blue shine and a little cloud on its head, and so light that it floats down gently when it falls. It only trusts you after <b>8 kaas knabbels</b>, and after that you still need some luck to tame it. Once tamed it catches you like a pillow of cloud, so no fall damage while it's near. Undress it for the <b>wolkenmuts</b> and <b>wolkenkraag</b>. To get down, take the second wolkenlift or just jump: the clouds catch you with slow falling.",
                'Hoog in de lucht van de Guhmensie zweven de <b>zwevende guh-eilandjes</b> (heel zeldzaam). Stap op de <b>wolkenlift</b> op het guh-gezichtplein eronder en VAHOEG, de wolken in! Op het grote eiland woont in een nest van wol de enige echte <b>Wolkguh</b>: wit en pluizig met een hemelsblauwe glans en een wolkje op zijn hoofd, en zo licht dat hij zachtjes zweeft als hij valt. Pas na <b>8 kaasknabbels</b> is hij vads genoeg om je te vertrouwen, en daarna heb je nog wat geluk nodig om hem te temmen. Getemd vangt hij je op als een kussen van wolk: zolang hij dichtbij is, heb je geen valschade. Kleed hem uit voor de <b>wolkenmuts</b> en de <b>wolkenkraag</b>. Naar beneden ga je met de tweede wolkenlift, of je springt gewoon: de wolkjes vangen je op met zweefval.') +
              '<p>' + ' '.join([icon('wolkensuikerspin', 'Wolkensuikerspin'), icon('wolkenmuts', 'Wolkenmuts'), icon('wolkenkraag', 'Wolkenkraag')]) + '</p>', wide=True) + \
        entry(img("guh_outfit_eilanden", 'Cloud outfit'), 'Cloud outfit', 'Wolkenpakje',
              p("Only here: " + ", ".join(['Wolkenmuts', 'Wolkenkraag']) + ".",
                "Alleen hier: " + ", ".join(['Wolkenmuts', 'Wolkenkraag']) + ".")) + \
        entry(img("structure_kaasmijn", 'The cheese mine'), 'The cheese mine', 'De kaasmijn',
              p("Deep in the Guhmension, very rarely, a giant guh head in a yellow hard hat sticks out of the ground: the <b>kaasmijn</b>. Walk in through its mouth and the <b>Mijnguh</b> lends you a pickaxe, so you don't need to bring your own (he takes it back when you leave). Whack the yellow <b>cheese veins</b> for kaasbrokken. They exist nowhere else in the world, and mined-out veins slowly grow back. Deep down in the deepslate there are <b>gold cheese veins</b>. Take the VAHOEG-jump down, ride the <b>Kaasexpress</b> carts around the tunnels, and open the <b>kaaskluis</b> in the treasure room with 2 goudkaas. The Mijnguh also sells the <b>miner outfit</b> for your guh (a hard hat with a lamp, overalls and a dusty neckerchief), and you can only get it from him.",
                'Heel zeldzaam steekt er diep in de Guhmensie een reuzenguhhoofd met een gele bouwhelm uit de grond: de <b>kaasmijn</b>. Loop door zijn mond naar binnen, dan leent de <b>Mijnguh</b> je een houweel. Je hoeft dus niks mee te nemen (bij het weggaan wil hij hem wel terug). Hak op de gele <b>kaasaders</b> voor kaasbrokken. Die vind je nergens anders in de wereld, en uitgemijnde aders groeien vanzelf weer aan. Diep in het diepsteen glinsteren <b>gouden kaasaders</b>. Neem de VAHOEG-sprong naar beneden, rij met de <b>Kaasexpress</b> rondjes door de gangen en open de <b>kaaskluis</b> in de schatkamer met 2 goudkaas. De Mijnguh verkoopt ook het <b>mijnwerkerspakje</b> voor je guh (een helm met lampje, een overall en een stoffige zakdoek). Dat koop je alleen bij hem.') +
              '<p>' + ' '.join([icon('kaasbrok', 'Kaasbrok'), icon('kaasmijn_helm', 'Mijnguhhelm'), icon('kaasmijn_overall', 'Kaasmijnoverall'), icon('kaasmijn_zakdoek', 'Stoffige zakdoek'), icon('goudkaas', 'Goudkaas'), icon('kaashouweel', 'Kaashouweel')]) + '</p>', wide=True) + \
        entry(img("guh_outfit_kaasmijn", 'Miner outfit'), 'Miner outfit', 'Mijnwerkerspakje',
              p("Only here: " + ", ".join(['Mijnguhhelm', 'Kaasmijnoverall', 'Stoffige zakdoek']) + ".",
                "Alleen hier: " + ", ".join(['Mijnguhhelm', 'Kaasmijnoverall', 'Stoffige zakdoek']) + ".")) + \
        entry(img("structure_guhbibliotheek", 'The guh library'), 'The guh library', 'De guhbibliotheek',
              p("The very rare <b>guh library</b> (Guhbibliotheek) in the Guhmension has three floors under a glass dome with guh ears. In the <b>reading room</b> every guh book lies open on a <b>lectern</b>: right-click to read it straight away, no borrowing and no waiting. Of every book you may take <b>one copy</b> home, just once (the <b>Take a copy</b> button). If you lose it, the <b>Bibliothecaris</b> behind the desk sells spare copies for <b>book vouchers</b>. He also runs the <b>Guhkwis</b>: questions about the books you've read. Your first right answer to each question earns a book voucher. His shop is the only place that sells the <b>scholar's outfit</b> for your guh: little scholar's glasses, the bookworm cardigan and the scholar's beret. There's also a guh-shaped reading armchair. The twelfth book, the <b>Secret Guh Book</b>, isn't on any lectern. Look for a bookcase with a pink book sticking out... VAHOEG!",
                'De zeer zeldzame <b>guhbibliotheek</b> in de Guhmensie heeft drie verdiepingen onder een glazen koepel met guhoren. In de <b>leeszaal</b> ligt elk guhboek open op een <b>lessenaar</b>: rechtsklik en je leest het meteen, zonder lenen en zonder wachten. Van elk boek mag je <b>één exemplaar</b> meenemen, maar één keer (knop <b>Exemplaar meenemen</b>). Ben je het kwijt? Dan verkoopt de <b>Bibliothecaris</b> achter de balie reserve-exemplaren voor <b>boekenbonnen</b>. Hij doet ook de <b>Guhkwis</b>: vragen over de boeken die je gelezen hebt. Je eerste goede antwoord op elke vraag levert een boekenbon op. Alleen in zijn winkeltje koop je het <b>geleerdenpakje</b> voor je guh: het geleerdenbrilletje, het boekenwurmvest en de geleerdenbaret. Er is ook een guhvormige leesfauteuil. Het twaalfde boek, het <b>Geheime Guhboek</b>, ligt op geen enkele lessenaar. Zoek een boekenkast waar een roze boek uitsteekt... VAHOEG!') +
              '<p>' + ' '.join([icon('boekenbon', 'Boekenbon'), icon('bieb_leesbril', 'Geleerdenbrilletje'), icon('bieb_vest', 'Boekenwurmvest'), icon('bieb_hoed', 'Geleerdenbaret')]) + '</p>', wide=True) + \
        entry(img("guh_outfit_bibliotheek", 'Scholar outfit'), 'Scholar outfit', 'Geleerdenpakje',
              p("Only here: " + ", ".join(['Geleerdenbrilletje', 'Boekenwurmvest', 'Geleerdenbaret']) + ".",
                "Alleen hier: " + ", ".join(['Geleerdenbrilletje', 'Boekenwurmvest', 'Geleerdenbaret']) + "."))
    S.append(section("rare24", "New in 2.4: rare places", "Nieuw in 2.4: zeldzame plekken", rare24))

    # --- new in 2.3: the guh castle ------------------------------------------------------------------------------------------
    new23 = entry(img("structure_guh_kasteel", "The guh castle"), "The guh castle", "Het guhkasteel",
                  p("The biggest thing in the mod: a legendary rare <b>guh castle</b> of 256&times;256 in the Guhmension, gothic with a "
                    "guh twist. A moat of kaas saus with a drawbridge and a twin-tower gatehouse, buttressed walls with round towers and "
                    "spires, and in the middle the <b>guhdraal</b>: a cathedral-castle with a tall nave (the <b>throne room</b>, with "
                    "pointed arches and aisles), transepts with the feast hall and the library, flying buttresses with pinnacles, a "
                    "<b>rose window</b> between two spired towers, and a crossing tower with a <b>giant guh head</b> on top (up to 130 "
                    "blocks high). South of it the royal garden in pink and green: a pergola avenue of guh blossom with two big "
                    "<b>Koningguh statues</b>, a fountain plaza with a golden guh, parterres of flowers and hedges, a hedge maze, a pond "
                    "with guh fish and more fountains. A <b>dungeon</b> with Mikas: take the stairs down in the chapel of the east aisle of the guhdraal (or in the kitchen). The <b>Guhmension super compass</b> "
                    "(Wonders &gt; Guh castle) shows the way; it can be thousands of blocks away.",
                    "Het grootste van de hele mod: een legendarisch zeldzaam <b>guhkasteel</b> van 256&times;256 in de Guhmensie, gotisch "
                    "met een guhtwist. Een slotgracht van kaassaus met een ophaalbrug en een poortgebouw met twee torens, muren met "
                    "steunberen, ronde torens en spitsen, en in het midden de <b>guhdraal</b>: een kathedraal-kasteel met een hoog schip "
                    "(de <b>troonzaal</b>, met spitsbogen en zijbeuken), dwarsschepen met de feestzaal en de bibliotheek, luchtbogen met "
                    "pinakels, een <b>roosvenster</b> tussen twee torens met spitsen, en een kruisingstoren met een <b>reuzen-guhhoofd</b> "
                    "erop (tot 130 blokken hoog). Ten zuiden de koninklijke tuin in roze en groen: een pergolalaan van guhbloesem met twee "
                    "grote <b>Koningguh-standbeelden</b>, een fonteinplein met een gouden guh, perken met bloemen en hagen, een "
                    "heggendoolhof, een vijver met guhvissen en meer fonteinen. Een <b>kerker</b> met Mika's: neem de trap naar beneden in de kapel in de oostbeuk van de guhdraal (of in de keuken). Het "
                    "<b>Guhmensie-superkompas</b> (Wonderen &gt; Guhkasteel) wijst de weg; het kan duizenden blokken ver zijn."), wide=True) +         entry(img("npc_poortwachter", "Gate guard"), "The gate guards: friends only", "De poortwachters: alleen voor vrienden",
              p("Two gate guards stand in front of the drawbridge. They only let <b>friends of the guhs</b> in: walk into the gate "
                "before that and they gently put you back. Talk to them for a hint. Prove you're a friend by being very good at "
                "<b>vadsen</b> (sit lazily on the guh bench by the gate for 30 seconds), or by saying the <b>secret guh word</b> in the "
                "chat (it starts with an N...). They remember you forever, at every castle.",
                "Voor de ophaalbrug staan twee poortwachters. Ze laten alleen <b>guhvrienden</b> binnen: loop je eerder de poort in, "
                "dan zetten ze je vriendelijk terug. Praat met ze voor een hint. Bewijs dat je een guhvriend bent door heel goed te zijn "
                "in <b>vadsen</b> (ga 30 seconden lekker lui op het guhbankje bij de poort zitten), of door het <b>geheime guhwoord</b> "
                "in de chat te zeggen (het begint met een N...). Ze onthouden je voor altijd, bij elk kasteel.")) +         f'''<div class="cards"><div class="card"><figure class="stage">{img("guh_kasteel_poort", "The gate")}</figure>
<h3>{t("The gate", "De poort")}</h3><p>{t("The gatehouse, the drawbridge over the moat and the two Koningguh statues.", "Het poortgebouw, de ophaalbrug over de gracht en de twee Koningguh-standbeelden.")}</p></div>
<div class="card"><figure class="stage">{img("guh_kasteel_hoofd", "The guh head on the tower")}</figure>
<h3>{t("The guh head", "Het guhhoofd")}</h3><p>{t("On top of the crossing tower, facing the garden.", "Boven op de kruisingstoren, met zijn gezicht naar de tuin.")}</p></div>
<div class="card"><figure class="stage">{img("guh_kasteel_troonzaal", "The throne room")}</figure>
<h3>{t("Inside", "Binnen")}</h3><p>{t("The nave with the throne in front of the apse, the feast hall and the library in the transepts.",
   "Het schip met de troon voor de apsis, de feestzaal en de bibliotheek in de dwarsschepen.")}</p></div></div>''' +         entry(img("guh_koning_pakje", "The Koningguh"), "The Koningguh", "De Koningguh",
              p("On the throne sits the <b>Koningguh</b>: always a giant (about 2.5 blocks), royal purple with golden socks, a fluffy white "
                "mane and a moustache. He wears the <b>guh king's outfit</b>: the <b>vahoege guh king's crown</b>, the <b>ermine king's "
                "cape</b> and the <b>golden guh medallion</b>. You can only get it from him: tame him with kaas knabbels, like any guh, "
                "and take the outfit off in his wardrobe (or leave it on: he's your king now). A few days after the throne is empty, a "
                "new Koningguh takes his place. He has his own page in the Guhdex.",
                "Op de troon zit de <b>Koningguh</b>: altijd een reus (zo'n 2,5 blok), koningspaars met gouden sokjes, witte pluizige "
                "manen en een snorretje. Hij draagt het <b>guhkoningpakje</b>: de <b>vahoege guhkoningskroon</b>, de <b>hermelijnen "
                "koningsmantel</b> en het <b>gouden guhmedaillon</b>. Dat krijg je alleen van hem: tem hem met kaasknabbels, zoals elke "
                "guh, en doe het pakje uit in zijn kledingkast (of laat het aan: hij is nu jouw koning). Een paar dagen nadat de troon "
                "leeg is, komt er een nieuwe Koningguh. Hij heeft een eigen pagina in de Guhdex.") +
              f'<p>{icon("koning_kroon", "Kroon")} {icon("koning_mantel", "Mantel")} {icon("koning_ketting", "Medaillon")} '
              f'</p>') + \
        entry(img("schilderij_guh_stapel", "The guh pile", "px"), "Guh paintings: the Guh Pile", "Guhschilderijen: De Guhstapel",
              p("Guh paintings hang just like normal paintings. The first one is <b>De Guhstapel</b>: a pile of guh plushies in the garden, "
                "9&times;9 blocks, pixelated like a real Minecraft painting. The big one hangs behind the Koningguh's throne; you can take "
                "one from the creative tab <b>Guhs</b>, and a normal painting can come out as the guh pile too if there's room for 9&times;9.",
                "Guhschilderijen hang je op zoals gewone schilderijen. De eerste is <b>De Guhstapel</b>: een stapel guhknuffels in de tuin, "
                "9&times;9 blokken, pixelig zoals een echt Minecraft-schilderij. De grote hangt achter de troon van de Koningguh; je pakt er "
                "een uit het creatieve tabblad <b>Guhs</b>, en een gewoon schilderij kan ook de guhstapel worden als er plek is voor 9&times;9.")) +         entry(img("koningstroon", "The royal guh throne"), "The royal guh throne", "De koninklijke guhtroon",
              p("In the treasure room (behind the throne, through the door in the apse) and in the dungeon: royal loot, and in the treasure room a "
                "<b>royal guh throne</b> to take home. You can sit on it; at home it's just a (very nice) chair.",
                "In de schatkamer (achter de troon, door de deur in de apsis) en in de kerker: koninklijke buit, en in de schatkamer een "
                "<b>koninklijke guhtroon</b> om mee naar huis te nemen. Je kunt erop zitten; thuis is het gewoon een (heel mooie) stoel."))
    S.append(section("new23", "New in 2.3: the guh castle", "Nieuw in 2.3: het guhkasteel", new23))

    # --- travelling: Reisguhs and the super compass --------------------------------------------------------------------------
    reis = entry(img("npc_reisguh", "Reisguh"), "Reisguhs: travel through the Guhmension", "Reisguhs: reizen door de Guhmensie",
                 p("A <b>Reisguh</b> is a sky blue guh conductor with a real <b>conductor's cap</b> (golden band, a guh badge) and a golden <b>whistle</b> on a cord: "
                   "now and then, and whenever someone travels with him, he blows it (<i>tuut tuut!</i>). He is a waypoint. Right-click one to <b>discover</b> it. After that, right-click "
                   "opens its menu: give it a name (for everyone), and travel with one click to any other Reisguh <b>you</b> have discovered "
                   "(nearest first). Reisguhs only work in the Guhmension.",
                   "Een <b>Reisguh</b> is een hemelsblauwe guh-conducteur met een echt <b>conducteurspetje</b> (gouden band, een guh-embleem) en een gouden <b>fluitje</b> aan een koord: "
                   "af en toe, en telkens als iemand met hem reist, blaast hij erop (<i>tuut tuut!</i>). Hij is een reispunt. Rechtsklik er een om hem te <b>ontdekken</b>. "
                   "Daarna opent een rechtsklik zijn menu: geef hem een naam (voor iedereen), en reis met een klik naar elke andere Reisguh "
                   "die <b>jij</b> ontdekt hebt (dichtstbijzijnde eerst). Reisguhs werken alleen in de Guhmensie.") +
                 ul([("One sits within 5 blocks of every new guh portal in the Guhmension (so also where you first came in), and one by the "
                      "garden gate of the guh castle.",
                      "Er zit er een binnen 5 blokken van elk nieuw guhportaal in de Guhmensie (dus ook waar je als eerste binnenkwam), en "
                      "een bij de tuinpoort van het guhkasteel."),
                     ("Since 2.10.1 one also lives in the big places: Guhwarden (Elf-Guhjestocht), the Knuffeldal town, the Guhkermis, Guhland, the "
                      "Ballonfestival and the Guhcircuit; since 3.0 also at every story place (Nomguh, the kloon-eiland, the stilt house and the capsule on Guhwai'i).",
                      "Sinds 2.10.1 woont er ook een in de grote plekken: Guhwarden (Elf-Guhjestocht), het Knuffeldal-stadje, de Guhkermis, Guhland, het "
                      "Ballonfestival en het Guhcircuit; sinds 3.0 ook bij elke verhaalplek (Nomguh, het kloon-eiland, het paalhuisje en de capsule op Guhwai'i)."),
                     ("Now and then a Reisguh just turns up somewhere in the Guhmension (a bit more often since 2.10.1).",
                      "Af en toe duikt er ergens in de Guhmensie vanzelf een Reisguh op (sinds 2.10.1 wat vaker)."),
                     ("<b>Move</b> one like a tamed guh: sneak + right-click picks it up, right-click a block puts it down. Its name and "
                      "who discovered it stay; its waypoint moves along.",
                      "<b>Verplaats</b> er een zoals een getemde guh: sluip + rechtsklik pakt hem op, rechtsklik op een blok zet hem neer. "
                      "Zijn naam en wie hem ontdekt heeft blijven; zijn reispunt verhuist mee."),
                     ("A <b>Reisguh whistle</b> (ender pearl, kaas knabbels and light blue dye: 2 whistles) puts down a new one.",
                      "Een <b>Reisguh-fluitje</b> (enderparel, kaasknabbels en lichtblauwe kleurstof: 2 fluitjes) zet een nieuwe neer.")]) +
                 f'<p>{icon("reisguh_fluitje", "Reisguh-fluitje")} {icon("guhmensie_superkompas_00", "Superkompas")}</p>', wide=True) + \
        p("The <b>Guhmension super compass</b> replaces the single-purpose guh compasses: right-click it and pick what to look for, "
          "by category (Adventure, Quests, Minigames, Wonders, Homes). The quest compasses (shrine compass, Mika trail compass, cake "
          "crumbs) stay as they are.",
          "Het <b>Guhmensie-superkompas</b> vervangt de losse guhkompassen: rechtsklik en kies wat je zoekt, per categorie (Avontuur, "
          "Quests, Minigames, Wonderen, Wonen). De questkompassen (heiligdomkompas, Mika-spoorkompas, taartkruimels) blijven gewoon.") + \
        p("Since 2.9 the categories are <b>icon tabs</b> on top (Avontuur, Quests, Minigames, Wonderen, Wonen, Einde, Ondergrond, Barbecue, Knus), and "
          "<b>Minigames</b> holds every game building under the subheadings Klassiekers, Knuffeldal and De Grote Guhspelen, with a green tick where you've been. "
          "Since 3.0 there is a tab <b>Verhalen</b> (the story places) and Minigames has a fourth heading, Verhalen.",
          "Sinds 2.9 zijn de categorieën <b>icoontjestabbladen</b> bovenaan (Avontuur, Quests, Minigames, Wonderen, Wonen, Einde, Ondergrond, Barbecue, Knus), en "
          "staat bij <b>Minigames</b> elk spelgebouw onder de kopjes Klassiekers, Knuffeldal en De Grote Guhspelen, met een groen vinkje waar je al was. "
          "Sinds 3.0 is er een tabblad <b>Verhalen</b> (de verhaalplekken) en heeft Minigames een vierde kopje, Verhalen.")
    S.append(section("reizen", "Travelling: Reisguhs and the super compass", "Reizen: Reisguhs en het superkompas", reis))

    # --- new in 2.2: verstopguh --------------------------------------------------------------------------------------------
    new22 = entry(img("structure_verstopguh_huis", "The verstopguh house"), "The verstopguh house", "Het verstopguhhuis",
                  p("Somewhere in the Guhmension stands the rare <b>verstopguh house</b>: a giant dollhouse of 80&times;80 with two floors "
                    "full of rooms (kitchen, library, playroom, greenhouse, bedrooms, the cuddle room, the attic...). Climb the stairs on "
                    "the side to the roof: it's made of <b>one-way glass</b>. From above you look straight into the house; from inside it's "
                    "just a pink ceiling. On the roof sits <b>Verstopguhtje</b>. The <b>super compass</b> (Minigames) shows the way.",
                    "Ergens in de Guhmensie staat het zeldzame <b>verstopguhhuis</b>: een reuzenpoppenhuis van 80&times;80 met twee "
                    "verdiepingen vol kamers (keuken, bibliotheek, speelkamer, tuinkas, slaapkamers, de knuffelkamer, de zolder...). Klim "
                    "via de trap aan de zijkant naar het dak: dat is van <b>eenrichtingsglas</b>. Van boven kijk je zo het huis in; van "
                    "binnen is het gewoon een roze plafond. Op het dak zit <b>Verstopguhtje</b>. Het <b>superkompas</b> (Minigames) "
                    "wijst de weg."), wide=True) + \
        entry(img("npc_verstopguhtje", "Verstopguhtje"), "Playing verstopguh", "Verstopguh spelen",
              p("Right-click Verstopguhtje and pick <b>makkelijk</b> (5 guhs of 1.5 blocks), <b>medium</b> (8 of 1 block) or "
                "<b>moeilijk</b> (12 of just half a block). They hide on random spots all over the house (there are over 700) and you're "
                "taken inside. Right-click a guh when you find one. Listen well: hidden guhs make soft guh noises now and then. The game "
                "ends when all are found, or when you step on the pink <b>way out</b> in the hall. While you search you can't get hurt or "
                "hungry, so you can sprint all you like, and only the seekers inside hear the hidden guhs. Every 30-40 seconds you get a hint "
                "that stays: where a guh still is (chat), the lights going out and the windows turning dark in a room without guhs, or a guh that keeps sniffing. Really stuck? "
                "Ask the yellow <b>Tipguh</b> in the hall: once a minute it makes a guh light up through the walls for 10 seconds. You still get "
                "your tickets, but your time no longer counts for your record.",
                "Rechtsklik op Verstopguhtje en kies <b>makkelijk</b> (5 guhs van 1,5 blok), <b>medium</b> (8 van 1 blok) of "
                "<b>moeilijk</b> (12 van maar een half blokje). Ze verstoppen zich op willekeurige plekjes in het hele huis (er zijn er "
                "meer dan 700) en jij gaat naar binnen. Rechtsklik op een guh als je hem vindt. Luister goed: verstopte guhs maken af en "
                "toe zachte guhgeluidjes. Het spel is afgelopen als ze allemaal gevonden zijn, of als je op de roze <b>uitgang</b> in de "
                "hal stapt. Tijdens het zoeken raak je niet gewond en krijg je geen honger, dus je kunt eindeloos rennen, en alleen de "
                "zoekers binnen horen de verstopte guhs. Elke 30-40 seconden krijg je een hint die blijft: waar nog een guh zit (chat), de "
                "lampjes die uitgaan en ramen die donker worden in een kamer zonder guhs, of een guh die steeds blijft snuffelen. Echt vast? Vraag de gele <b>Tipguh</b> "
                "in de hal: elke minuut laat hij een guh 10 seconden door de muren heen oplichten. Je tickets krijg je nog, maar je tijd telt "
                "dan niet meer voor je record.") +
              p("Found them all: 2 / 3 / 5 <b>verstopguh tickets</b>, and 2 more if you're quick (within 2 / 4 / 6 minutes). Your time is on screen and your best "
                "time per level is kept. Others can join a game that's running (everyone gets the tickets), but searching together doesn't "
                "count for records.",
                "Alles gevonden: 2 / 3 / 5 <b>verstopguhtickets</b>, en 2 extra als je snel bent (binnen 2 / 4 / 6 minuten). Je tijd staat in beeld en je beste tijd "
                "per moeilijkheid wordt bewaard. Anderen kunnen meedoen met een spel dat al loopt (iedereen krijgt de tickets), maar samen "
                "zoeken telt niet voor records."),
              stats=[(("Guhs", "Guhs"), "5 / 8 / 12"), (("Tickets", "Tickets"), "2 / 3 / 5 (+2)")]) + \
        entry(img("guh_outfit_detective", "A guh in the detective outfit"), "Guhlock Holmes", "Guhlock Holmes",
              p("For tickets Verstopguhtje sells the <b>detective outfit</b>: the <b>Vergroot-vadsglas</b> (2), the <b>Guhlock cap</b> "
                "(3) and the <b>detective coat</b> (5). The whole outfit is the advancement <i>Guhlock Holmes</i>.",
                "Voor tickets verkoopt Verstopguhtje het <b>detectivepakje</b>: het <b>Vergroot-vadsglas</b> (2), de <b>Guhlock-pet</b> "
                "(3) en de <b>detectivejas</b> (5). Het hele pakje is de vooruitgang <i>Guhlock Holmes</i>.") +
              f'<p>{icon("verstopguhticket", "Verstopguhticket")} {icon("detective_pet", "Guhlock-pet")} '
              f'{icon("detective_vergrootglas", "Vergroot-vadsglas")} {icon("detective_jas", "Detectivejas")}</p>') + \
        entry(img("eenrichtingsglas", "One-way glass"), "One-way glass", "Eenrichtingsvadsglas",
              p("You can make it yourself too: 8 pink stained glass around pink wool gives 8. Seen from above it's glass, from below a "
                "pink ceiling: build your own secret lookout.",
                "Je kunt het ook zelf maken: 8 roze glas om roze wol geeft er 8. Van boven glas, van onder een roze plafond: bouw je "
                "eigen geheime uitkijkpost.")) + \
        f'''<figure class="stage" style="margin-top:10px">{img("verstopguh_huis_beneden", "Inside, downstairs")}</figure>'''
    S.append(section("new22", "New in 2.2: verstopguh", "Nieuw in 2.2: verstopguh", new22))

    # --- new in 2.1: the guh kermis, the coaster pieces, the ender guh --------------------------------------------------
    BK, IR, PW, ST, KR = "sleebouwersboek", "iron_ingot", "pink_wool", "stick", "guh_kristal"
    COASTER_PIECES = [
        ("rail_drop", "Vadsdrop", "Vadsdrop", "A steep coaster hill: 8 up over 4 blocks, flat at both ends. <b>Sneak</b> (on the end of a track) to go down.",
         "Een steile achtbaanhelling: 8 omhoog over 4 blokken, vlak aan beide kanten. <b>Sluip</b> (op het eind van een baan) om te dalen."),
        ("rail_spiral_right", "Guhkurkentrekker", "Guhkurkentrekker", "Half a turn going 4 up: you come back alongside, 3 blocks over. "
         "<b>Sneak</b> for a left one; <b>look down</b> while laying it to go down. Two of them make a corkscrew tower.",
         "Een halve draai, 4 omhoog: je komt 3 blokken verderop terug. <b>Sluip</b> voor links; <b>kijk omlaag</b> tijdens het leggen om te "
         "dalen. Twee achter elkaar maken een kurkentrekkertoren."),
        ("rail_jump", "Vahoegschans", "Vahoegschans", "A little ramp: the sled flies 10 blocks through the air and lands on the next rail. "
         "Click the end of the ramp and the next piece goes right onto the landing spot.",
         "Een schansje: de slee vliegt 10 blokken door de lucht en landt op de volgende rail. Klik op het eind van de schans en het "
         "volgende stuk komt precies op de landingsplek."),
    ]
    new21 = entry(img("structure_guh_kermis", "The guh kermis"), "The guh kermis", "De Guhkermis",
                  p("Somewhere in the Guhmension stands the rare <b>guh kermis</b>: a fair with a real <b>sled coaster</b> around it. A "
                    "corkscrew tower, a steep drop, a jump over a kaas saus pool, a camel hump of 8 high and a little hill. In the middle: "
                    "a carousel, a tent, lampgions and the stall of the <b>Kermis-guh</b>. The <b>super compass</b> (Minigames) shows "
                    "the way.",
                    "Ergens in de Guhmensie staat de zeldzame <b>Guhkermis</b>: een kermis met een echte <b>slee-achtbaan</b> eromheen. "
                    "Een kurkentrekkertoren, een steile drop, een schans over een kaassausbad, een kamelenbult van 8 hoog en een klein "
                    "heuveltje. In het midden: een draaimolen, een tent, lampgions en het kraampje van de <b>Kermis-guh</b>. Het "
                    "<b>superkompas</b> (Minigames) wijst de weg.") +
                  p("Get into one of the two sleds at the <b>station</b>, click the blinking <b>buttons</b> (right-click) and press Start. "
                    "Every lap over the finish line gives everyone in the sled a <b>kermisbon</b>. Your very first lap also gives a prize "
                    "bag: 3 balloons, 4 kaashoning, 8 guh crystals and 2 extra kermisbonnen. On a coaster the sled goes faster downhill "
                    "and slower uphill, and it leans into the bends. Get out up high? You float down.",
                    "Stap in een van de twee sleeën op het <b>station</b>, klik op de knipperende <b>knopjes</b> (rechtermuisklik) en druk "
                    "op Start. Elk rondje over de finish geeft iedereen in de slee een <b>kermisbon</b>. Je allereerste rondje geeft ook "
                    "een prijzenzakje: 3 ballonnen, 4 kaashoning, 8 guhkristallen en 2 extra kermisbonnen. Op een achtbaan gaat de slee "
                    "bergaf harder en bergop langzamer, en hij helt mee in de bochten. Hoog uitstappen? Dan zweef je naar beneden."),
                  wide=True) + \
        f'''<figure class="stage" style="margin-top:10px">{img("kermis_coaster", "The kermis coaster")}</figure>''' + \
        entry(img("npc_kermis_guh", "Kermis Guh"), "The Kermis-guh and the kermis outfit", "De Kermis-guh en het kermispakje",
              p("Right-click the <b>Kermis-guh</b> in her stall to trade. For kermisbonnen she sells the <b>kermis outfit</b> for your guh: "
                "the <b>vahoege kermis hat</b> (4), the <b>kermis jacket</b> (6) and the <b>guh kermis bow</b> (2), and 2 guh balloons "
                "for 1 bon. The whole outfit is the advancement <i>Vahoeg op de kermis</i>.",
                "Rechtsklik op de <b>Kermis-guh</b> in haar kraampje om te ruilen. Voor kermisbonnen verkoopt ze het <b>kermispakje</b> "
                "voor je guh: de <b>vahoege kermishoed</b> (4), het <b>kermisjasje</b> (6) en de <b>guhkermisstrik</b> (2), en 2 "
                "guhballonnen voor 1 bon. Het hele pakje is de vooruitgang <i>Vahoeg op de kermis</i>.") +
              f'<p>{icon("kermisbon", "Kermisbon")} {icon("kermis_hoed", "Kermishoed")} {icon("kermis_jasje", "Kermisjasje")} '
              f'{icon("kermis_strik", "Kermisstrik")}</p>') + \
        entry(img("guh_outfit_kermis", "A guh in the kermis outfit"), "Dressed for the fair", "Klaar voor de kermis",
              p("Red and white striped hat, a red jacket with gold buttons and a yellow bow with red dots.",
                "Rood-wit gestreepte hoed, een rood jasje met gouden knopen en een gele strik met rode stippen.")) + \
        f'''<h3 style="margin-top:14px">{t("New rail pieces: build your own coaster", "Nieuwe railstukken: bouw je eigen achtbaan")}</h3>
<div class="cards">{"".join(f'<div class="card"><figure class="stage">{img(r, en)}</figure><h3>{t(en, nl)}</h3><p>{t(den, dnl)}</p></div>' for r, en, nl, den, dnl in COASTER_PIECES)}</div>
<div class="recipes">{rcard("Vadsdrop", "Vadsdrop", grid([None, PW, IR, PW, BK, IR, IR, ST, ST], "sleerail_drop"))}
{rcard("Guhkurkentrekker", "Guhkurkentrekker", grid([IR, PW, IR, PW, BK, PW, IR, ST, IR], "sleerail_kurkentrekker"))}
{rcard("Vahoegschans", "Vahoegschans", grid([None, None, KR, None, BK, PW, IR, ST, ST], "sleerail_schans"))}</div>''' + \
        entry(img("guh_slee_knopjes", "The sled's buttons"), "The sled's buttons", "De knopjes van de slee",
              p("The sled has a little dashboard with three buttons at your feet. While the sled stands still they blink: right-click "
                "while riding to open the panel. Every button in the panel (and in the guh menu) tells you what it does when you hover over it.",
                "De slee heeft een dashboardje met drie knopjes bij je voeten. Zolang de slee stilstaat knipperen ze: rechtsklik tijdens "
                "het rijden om het paneel te openen. Elke knop in het paneel (en in het guhmenu) vertelt wat hij doet als je er met je muis "
                "op staat.")) + \
        entry(img("guh_variant_ender", "Ender guh"), "The ender guh", "De Enderguh",
              p("A black and purple guh with <b>dragon wings</b>, little horns and glowing purple eyes. It lives only on the "
                "<b>Guh Peaks</b> (about 1 in 30 guhs there) and flies around; hold kaas knabbels and it comes to you. Taming is harder: "
                "1 in 8 knabbels, or at once with a <b>fried kaas knabbel</b>. A tamed ender guh is always big enough to ride: saddle it "
                "and <b>fly</b>! Look where you want to go and hold W, jump = up, let go = hover. Getting off in the air? You float down.",
                "Een zwart-paarse guh met <b>drakenvleugels</b>, hoorntjes en gloeiende paarse ogen. Hij woont alleen op de "
                "<b>Guhpieken</b> (ongeveer 1 op de 30 guhs daar) en vliegt rond; houd kaasknabbels vast en hij komt naar je toe. Temmen "
                "is pittiger: 1 op 8 knabbels, of in een keer met een <b>gefrituurde kaasknabbel</b>. Een tamme Enderguh is altijd groot "
                "genoeg om te berijden: zadel hem en <b>vlieg</b>! Kijk waar je heen wilt en houd W vast, spatie = omhoog, loslaten = "
                "zweven. In de lucht afstappen? Dan zweef je naar beneden."),
              stats=[(("Where", "Waar"), t("Guh Peaks", "Guhpieken")), (("Taming", "Temmen"), t("1 in 8, or fried knabbels", "1 op 8, of gefrituurde knabbels"))]) + \
        p("Also new: every guh compass now really points to the <b>nearest</b> structure of its kind (it used to search around 0,0), and "
          "the guh blossom tree has a guh-fur bark, sometimes with a little face.",
          "Ook nieuw: elk guhkompas wijst nu echt naar het <b>dichtstbijzijnde</b> bouwwerk van zijn soort (hij zocht per ongeluk rond "
          "0,0), en de guhbloesemboom heeft een schors van guhvacht, soms met een gezichtje.")
    S.append(section("new21", "New in 2.1: the guh kermis and the ender guh", "Nieuw in 2.1: de Guhkermis en de Enderguh", new21))

    # --- mobs -----------------------------------------------------------------------------------------------------
    mobs = entry(img("guh", "Guh"), "Guh", "Guh",
                 p("A chubby plush mouse with big glossy eyes. Guhs live in every overworld land biome (a little rarer than sheep and cows), "
                   "but the Guhmension is full of them. Every guh has its own size, from half a block to about three blocks long; bigger guhs have deeper voices. "
                   "Wild guhs panic and run when hurt. Every guh shows its name above its head: <b>Guh</b>, its variant, or the name you gave it.",
                   "Een mollig knuffelmuisje met grote glanzende ogen. Guhs wonen in elk landbioom van de overworld (iets zeldzamer dan schapen en "
                   "koeien), maar de Guhmensie zit er vol mee. Elke guh heeft een eigen formaat, van een half blok tot zo'n drie blokken lang; grotere guhs hebben een "
                   "lagere stem. Wilde guhs raken in paniek en rennen weg als ze pijn krijgen. Elke guh heeft zijn naam boven zijn hoofd: "
                   "<b>Guh</b>, zijn variant, of de naam die jij hem gaf."),
                 stats=[(("Health", "Levens"), t("25 wild &middot; 1000 tamed", "25 wild &middot; 1000 tam")),
                        (("Size", "Formaat"), t("0.5 &ndash; 3 blocks", "0,5 &ndash; 3 blokken")),
                        (("Drops", "Laat vallen"), t("0&ndash;2 pink wool", "0&ndash;2 roze wol")),
                        (("Tame with", "Temmen met"), icon("kaas_knabbels", "Kaas Knabbels") + " " + t("Kaas Knabbels", "Kaas Knabbels"))])
    giant = entry(img("guh_front", "Giant guh"), "Giant and mega guhs", "Reuzen- en megaguhs",
                  p("Hamster houses are home to a <b>giant guh</b> (about 10 blocks long); the large hamster house even has a "
                    "<b>mega guh</b> of about 14 blocks. They are peaceful, never despawn and can be tamed like any guh "
                    "&mdash; imagine riding one.",
                    "In hamsterhuizen woont een <b>reuzenguh</b> (zo'n 10 blokken lang); het grote hamsterhuis heeft zelfs een "
                    "<b>megaguh</b> van ongeveer 14 blokken. Ze zijn vredig, verdwijnen nooit en zijn te temmen zoals elke guh "
                    "&mdash; stel je voor dat je erop rijdt."))
    mika = entry(img("mika", "Mika"), "Mika", "Mika",
                 p("The evil guh: red slit eyes, angry brows, a fanged grin and a devil tail, with <b>Mika</b> above its head. "
                   "Mika chases you and shoves you away hard (with a little hop) and a distorted growl &mdash; but it never does any damage. "
                   "Lives in Evil Mika homes and challenging guh caves, and only very rarely wanders Mika's biome.",
                   "De kwaadaardige guh: rode spleetogen, boze wenkbrauwen, een grijns met hoektand en een duivelsstaartje, met "
                   "<b>Mika</b> boven zijn hoofd. Mika zit je achterna en duwt je weg met een vervormd gegrom &mdash; maar doet nooit "
                   "echt pijn, al duwt hij je wel flink weg (met een sprongetje). Woont in Evil Mika-huizen en uitdagende guhgrotten, en "
                   "zwerft maar heel af en toe door Mika's bioom."),
                 stats=[(("Health", "Levens"), "50"), (("Damage", "Schade"), t("none (knockback only)", "geen (alleen wegduwen)")),
                        (("Drops", "Laat vallen"), icon("mika_vet", "Mika's vet") + " " + t("1&ndash;2 Mika's vet", "1&ndash;2 Mika's vet"))])
    VARIANTS = [  # render, EN, NL, EN text, NL text, rarity EN, rarity NL
        ("mint", "Mint Guh", "Muntguh", "Fresh mint-green fur.", "Frisse mintgroene vacht.", "rare", "zeldzaam"),
        ("choco", "Choco Guh", "Chocoguh", "Chocolate-brown fur.", "Chocoladebruine vacht.", "rare", "zeldzaam"),
        ("snow", "Snow Guh", "Sneeuwguh", "Snow-white fur.", "Sneeuwwitte vacht.", "rare", "zeldzaam"),
        ("brontosaurus", "Brontosaurus Guh", "Brontosaurusguh", "Its head sits way up on a long, wrinkly neck with rubber bands around it.",
         "Zijn hoofd zit hoog op een lange, gerimpelde nek met elastiekjes eromheen.", "super rare", "super zeldzaam"),
        ("golden", "Golden Guh", "Gouden Guh", "Glittering gold. The rarest of them all.", "Glinsterend goud. De zeldzaamste van allemaal.", "legendary", "legendarisch"),
        ("teckel", "Teckel Guh", "Teckelguh", "A sausage guh: an extra long body and six little legs.", "Een worstguh: een extra lang lijf en zes pootjes.", "super rare", "super zeldzaam"),
        ("ghost", "Ghost Guh", "Spookguh", "See-through and pale. Only turns up at night &mdash; wild ones fade away at sunrise.",
         "Doorzichtig en bleek. Komt alleen 's nachts &mdash; wilde spookguhs vervagen als de zon opkomt.", "super rare, night only", "super zeldzaam, alleen 's nachts"),
        ("starry", "Starry Guh", "Sterrenguh", "Fur like a night sky, with stars that glow in the dark.", "Een vacht als een nachthemel, met sterren die in het donker gloeien.", "ultra rare", "ultra zeldzaam"),
        ("rainbow", "Rainbow Guh", "Regenboogguh", "Slowly changes through all the colours of the rainbow.", "Kleurt langzaam door alle kleuren van de regenboog.", "ultra rare", "ultra zeldzaam"),
    ]
    variant_cards = '<div class="cards">' + "".join(
        f'<div class="card"><figure class="stage">{img("guh_variant_" + key, en)}</figure><h3>{t(en, nl)}<span class="rarity">{t(ren, rnl)}</span></h3>'
        f'<p>{t(den, dnl)}</p></div>' for key, en, nl, den, dnl, ren, rnl in VARIANTS) + "</div>"
    variants = f'''<h3 style="margin-top:18px">{t("Guh variants", "Guhvarianten")}</h3>
{p("In the Guhmension, about 1 in 11 wild guhs is a variant: another fur colour, or another shape. They only turn up there, "
   "keep their look when tamed, and their babies usually look like one of their parents. About 1 in 25 wild Guhmension guhs "
   "also already wears clothes (see <i>Guh clothes</i>). (Some say there's one more, very secret variant...)",
   "In de Guhmensie is ongeveer 1 op de 11 wilde guhs een variant: een andere vachtkleur of een andere vorm. Ze komen alleen "
   "daar voor, houden hun uiterlijk als je ze temt, en hun baby's lijken meestal op een van hun ouders. Ongeveer 1 op de 25 "
   "wilde guhs in de Guhmensie draagt ook al kleertjes (zie <i>Guhkleertjes</i>). (Er schijnt nog een heel geheime variant te zijn...)")}
{variant_cards}'''
    big_mika = entry(img("mika", "Big Mika"), "Big Mika (boss)", "Grote Mika (baas)",
                     p("The guardian of every challenging guh cave: a Mika two and a half times as big, waiting next to the "
                       "treasure. Unlike other Mikas, Big Mika <b>really hurts</b> and knocks you far back, hardly budges when you "
                       "hit it and shows a pink boss bar. Bring good armour and food.",
                       "De bewaker van elke uitdagende guhgrot: een Mika van tweeënhalf keer zo groot, naast de schat. Anders dan "
                       "andere Mika's doet Grote Mika <b>echt pijn</b> en slaat je ver weg, geeft zelf nauwelijks mee als je hem "
                       "raakt en heeft een roze baasbalk. Neem goed pantser en eten mee."),
                     stats=[(("Health", "Levens"), "200"), (("Damage", "Schade"), t("9 (4.5 hearts)", "9 (4,5 hartjes)")),
                            (("Drops", "Laat vallen"), icon("mika_vet", "Mika's vet") + icon("vahoege_vads_ingot", "Vads ingot")
                             + icon("guhmensie_superkompas_00", "Super compass") + " " + t("6 vet, 3 vads ingots, a Guhmension super compass",
                                                                                            "6 vet, 3 vadsstaven, een Guhmensie-superkompas"))])
    quest = entry(img("guh_sitting", "Hungry Guh"), "Hungry Guh", "Hongerige Guh",
                  p("Sits up on its hind legs at guh picnics. Right-click it and it tells you in chat that it wants "
                    "<b>10 gefrituurde kaasknabbels</b>. Right-click it while holding at least 10 and it happily disappears, "
                    "leaving you a <b>Bank Guh</b>. It can't be hurt or pushed.",
                    "Zit rechtop op zijn achterpootjes bij guhpicknicks. Rechtsklik erop en hij vertelt in de chat dat hij "
                    "<b>10 gefrituurde kaasknabbels</b> wil. Rechtsklik terwijl je er minstens 10 vasthoudt en hij verdwijnt blij, "
                    "met een <b>Bankguh</b> voor jou als beloning. Hij kan geen pijn krijgen en niet geduwd worden."))
    new_mobs = f'''<h3 style="margin-top:18px">{t("New in 2.0", "Nieuw in 2.0")}</h3>''' + \
        entry(img("guh_bee", "Guh bee"), "Guh bee", "Guhbij",
              p("A fluffy pink bee with guh eyes and little guh ears. Flies from flower to flower like a normal bee and fills a "
                "<b>knabbelkorf</b> (or a beehive) with nectar &mdash; but a guh bee <b>never gets angry and never stings</b>, not "
                "even when you hit it or take its honey. Lives in the Guhmension and in flowery overworld biomes (plains, meadows, "
                "flower forests, cherry groves).",
                "Een pluizige roze bij met guhogen en guhoortjes. Vliegt van bloem naar bloem zoals een gewone bij en vult een "
                "<b>knabbelkorf</b> (of een bijenkorf) met nectar &mdash; maar een guhbij <b>wordt nooit boos en steekt nooit</b>, ook "
                "niet als je hem slaat of zijn honing pakt. Woont in de Guhmensie en in bloemrijke overworldbiomen (vlaktes, weides, "
                "bloemenbossen, kersenbossen)."),
              stats=[(("Health", "Levens"), "10"), (("Temper", "Humeur"), t("always friendly", "altijd vriendelijk"))]) + \
        entry(img("guh_slime", "Guh slime"), "Guh slime", "Guhslijm",
              p("A pink slime with big guh eyes. Bounces around happily and splits like a normal slime, but it's a <b>peaceful</b> "
                "creature: it never hurts you. The little ones drop <b>guh slimeballs</b> &mdash; nine make a pink slime block.",
                "Een roze slijm met grote guhogen. Stuitert vrolijk rond en splitst zoals een gewone slijm, maar het is een "
                "<b>vredig</b> wezen: hij doet je nooit pijn. De kleintjes laten <b>guhslijmballen</b> vallen &mdash; negen worden een "
                "roze slijmblok."),
              stats=[(("Drops", "Laat vallen"), icon("guh_slimeball", "Guh slimeball") + " " + t("guh slimeballs (small ones)", "guhslijmballen (kleintjes)"))]) + \
        entry(img("guh_vis", "Guh fish"), "Guh fish", "Guhvis",
              p("Chubby pink fish with guh eyes, swimming in schools in the pink pools of the <b>guh sea</b>. Catch one in a bucket, "
                "or fish in the Guhmension. Eat it raw, or <b>fry</b> it: in a furnace, smoker, on a campfire &mdash; or in the guh "
                "frying pan, just like kaas knabbels.",
                "Mollige roze visjes met guhogen, die in scholen zwemmen in de roze poelen van de <b>guhzee</b>. Vang er een in een "
                "emmer, of vis in de Guhmensie. Eet hem rauw of <b>bak</b> hem: in een oven, roker, op een kampvuur &mdash; of in de "
                "guh-koekenpan, net als kaasknabbels."),
              stats=[(("Food", "Voedsel"), icon("guh_vis", "Guh fish") + icon("gebakken_guh_vis", "Fried guh fish") + " " + t("2 raw, 6 fried", "2 rauw, 6 gebakken"))]) + \
        entry(img("nether_mika", "Nether Mika"), "Nether Mika", "Nether-Mika",
              p("A Mika that wandered into the Nether: scorched dark, with glowing embers in its fur, and completely <b>fire-proof</b>. "
                "Fairly rare in every Nether biome. Just like a normal Mika it only shoves you away, and it sometimes drops magma cream.",
                "Een Mika die de Nether in is gedwaald: donker geschroeid, met gloeiende sintels in zijn vacht, en helemaal "
                "<b>vuurbestendig</b>. Vrij zeldzaam in elk Netherbioom. Net als een gewone Mika duwt hij je alleen weg, en soms laat hij "
                "magmacrème vallen."))
    MOBS27 = [("rookguh", "Rookguh", "Barbecuether: a sad smoky guh. Feed it 6 knabbels and it floats home. Can't be hurt.", "Barbecuether: een zielige rokerige guh. Voer hem 6 knabbels en hij zweeft naar huis. Onkwetsbaar."),
              ("vonk_mika", "Vonk-Mika", "Barbecuether: throws glowing coals. Drops the grillspies.", "Barbecuether: gooit gloeiende kooltjes. Laat de grillspies vallen."),
              ("knekel_mika", "Knekel-Mika", "Barbecuether: a charred Mika skeleton with a hot grill fork. Sometimes drops a verkoolde mikakop.", "Barbecuether: een verkoold Mika-skelet met een hete grillvork. Laat soms een verkoolde mikakop vallen."),
              ("aangebrande_mika", "Aangebrande Mika", "Boss (300 HP): four ash blocks in a T with three verkoolde mikakoppen. Drops the gloeister.", "Baas (300 levens): vier asblokken in een T met drie verkoolde mikakoppen. Laat de gloeister vallen."),
              ("vadswaker", "Vadswaker", "Gatenkaasgrotten: a blind giant Mika who hears you chew. 300 HP. Better sneak away.", "Gatenkaasgrotten: een blinde reuzenmika die je hoort kauwen. 300 levens. Sluip maar weg."),
              ("kikkerguhs", "Kikkerguh", "Kaasmoeras: a frog guh in pink, mint or yellow. Eats kaasmotten and spits out motknabbels.", "Kaasmoeras: een kikker-guhtje in roze, mint of geel. Eet kaasmotten en spuugt motknabbels uit."),
              ("kaasmot", "Kaasmot", "Kaasmoeras: a tiny Mika moth that pecks up your knabbels.", "Kaasmoeras: een piepklein Mika-motje dat je knabbels inpikt."),
              ("moerasheks_mika", "Moerasheks-Mika", "Kaasmoeras: throws vadsverdrijvende drankjes and nibbles moeraskaas.", "Kaasmoeras: gooit vadsverdrijvende drankjes en knabbelt moeraskaas."),
              ("guh_variant_asguh", "Asguh", "New variant, only in the Asdal: glowing cheeks, fire-proof, tameable.", "Nieuwe variant, alleen in het Asdal: gloeiende wangetjes, vuurbestendig, tembaar."),
              ("guh_variant_kaasmoerasguh", "Kaasmoerasguh", "New variant, only in the Kaasmoeras: green-yellow spots, tameable.", "Nieuwe variant, alleen in het Kaasmoeras: groen-geel gevlekt, tembaar.")]
    new_mobs27 = f'''<h3 style="margin-top:18px">{t("New in 2.7 (see <a href='#new27'>New in 2.7</a>)", "Nieuw in 2.7 (zie <a href='#new27'>Nieuw in 2.7</a>)")}</h3>''' +         '<div class="cards">' + "".join(f'<div class="card"><figure class="stage">{img(n, nm)}</figure><h3>{nm}</h3><p>{t(den, dnl)}</p></div>'
                                        for n, nm, den, dnl in MOBS27) + "</div>"
    MOBS28 = [("guh_variant_pluisguh", "Pluisguh", "New variant, only in the Knuffeldal: extra fluffy and pink, tameable.", "Nieuwe variant, alleen in het Knuffeldal: extra pluizig en roze, tembaar."),
              ("kruimel_mika", "Kruimel-Mika", "Knusfeest: steals feest things and runs off giggling. Never fights; lure it away with a treat.", "Knusfeest: pikt feestspullen en rent giechelend weg. Vecht nooit; lok hem weg met een lekkernij."),
              ("guhschaapje", "Guhschaapje", "Guhboerderij: a happy one drops pluiswol.", "Guhboerderij: een blij schaapje laat pluiswol los."),
              ("knabbelkippetje", "Knabbelkippetje", "Guhboerderij: a happy one lays a knabbelei.", "Guhboerderij: een blij kippetje legt een knabbelei."),
              ("guhkoe", "Guhkoe", "Guhboerderij: a happy one gives kaasmelk.", "Guhboerderij: een blije koe geeft kaasmelk."),
              ("ijscoguh", "IJscoguh Tingeling", "Cycles through the Guhmension with kaasijsjes. Tingeling!", "Fietst met kaasijsjes door de Guhmensie. Tingeling!"),
              ("rookguh", "Rookguh", "Restyled: a little ghast with a guh face. Always peaceful.", "Nieuw jasje: een kleine ghast met een guhgezicht. Altijd vredig."),
              ("kikkerguhs", "Kikkerguh", "Restyled: a guh that is a frog.", "Nieuw jasje: een guh die een kikker is.")]
    new_mobs28 = f'''<h3 style="margin-top:18px">{t("New in 2.8 (see <a href='#new28'>New in 2.8</a>)", "Nieuw in 2.8 (zie <a href='#new28'>Nieuw in 2.8</a>)")}</h3>''' +         '<div class="cards">' + "".join(f'<div class="card"><figure class="stage">{img(n, nm)}</figure><h3>{nm}</h3><p>{t(den, dnl)}</p></div>'
                                        for n, nm, den, dnl in MOBS28) + "</div>"
    MOBS281 = [("pieppiepmuisje", "Pieppiepmuisje", "A tiny peeping mouse in lieve guh buildings. Tame it with a kaasknabbel, carry it on your shoulder.",
                "Een piepklein muisje in lieve guhgebouwen. Tem het met een kaasknabbel, draag het op je schouder."),
               ("poepschilly", "Poepschilly", "The brown kontpoetser turtle of the guhzee coast: a poetsbeurt makes a guh Fris van binnen.",
                "De bruine kontpoetser-schildpad van de guhzee-kust: na een poetsbeurt is een guh Fris van binnen."),
               ("schilly", "Schilly", "A different (green) turtle: a minihoofdje and the guhs' bestie. Sometimes beef... toch besties &lt;3.",
                "Een andere (groene) schildpad: een minihoofdje en de bestie van de guhs. Soms beef... toch besties &lt;3."),
               ("boze_kaasknabbel", "Boze Kaasknabbel", "An angry knabbel from the kaasknabbel-nest. Beaten, it is just zieli.",
                "Een boze knabbel uit het kaasknabbel-nest. Verslagen is hij gewoon zieli."),
               ("boze_oppernabbel", "Boze Oppernabbel", "The crowned boss of the nest (110 health). Jump over his stamp!",
                "De gekroonde baas van het nest (110 levens). Spring over zijn stamp!")]
    new_mobs281 = f'''<h3 style="margin-top:18px">{t("New in 2.8.1 (see <a href='#new281'>New in 2.8.1</a>)", "Nieuw in 2.8.1 (zie <a href='#new281'>Nieuw in 2.8.1</a>)")}</h3>''' + \
        '<div class="cards">' + "".join(f'<div class="card"><figure class="stage">{img(n, nm)}</figure><h3>{nm}</h3><p>{t(den, dnl)}</p></div>'
                                        for n, nm, den, dnl in MOBS281) + "</div>"
    MOBS29 = [("guh_variant_pinguh_klassiek", "Pinguh", "A guh in a penguin suit, in the Guhpolder (half of the wild guhs there). Klassiek, keizer or a grey fluffy chick. Slides on its tummy.",
                "Een guh in een pinguinpakje, in de Guhpolder (de helft van de wilde guhs daar). Klassiek, keizer of een grijs pluizig kuikentje. Glijdt op zijn buik."),
               ("doolhof_mika", "Heg-Mika", "Chases you in the Guhdoolhof and pinches one knabbel back. Never damage.", "Zit je achterna in het Guhdoolhof en pikt één knabbel terug. Nooit schade."),
               ("circuit_mikapikker", "Mika-pikker", "Pinches your VAHOEG boost on the Guh-Circuit, giggles and pops back.", "Pikt je VAHOEG-zet in op het Guh-Circuit, giechelt en springt terug."),
               ("knabbeldief_mika", "Knabbeldief", "The Mika of the Knabbeldief-zaak at the Politiebureautje. Runs off giggling.", "De Mika van de Knabbeldief-zaak bij het Politiebureautje. Rent giechelend weg.")]
    new_mobs29 = f'''<h3 style="margin-top:18px">{t("New in 2.9 (see <a href='#new29'>New in 2.9</a>)", "Nieuw in 2.9 (zie <a href='#new29'>Nieuw in 2.9</a>)")}</h3>''' + \
        '<div class="cards">' + "".join(f'<div class="card"><figure class="stage">{img(n, nm)}</figure><h3>{nm}</h3><p>{t(den, dnl)}</p></div>'
                                        for n, nm, den, dnl in MOBS29) + "</div>"
    S.append(section("mobs", "Creatures", "Wezens", mobs + variants + giant + mika + big_mika + quest + new_mobs + new_mobs27 + new_mobs28 + new_mobs281 + new_mobs29))

    # --- your guh ------------------------------------------------------------------------------------------------
    care = f"""
{entry(img("guh_saddle", "Guh with a saddle"), "Taming, riding and picking up", "Temmen, rijden en oppakken",
       ul([("<b>Tame</b>: feed a wild guh Kaas Knabbels (1 in 3 chance each). A tamed guh gets 1000 HP.",
            "<b>Temmen</b>: voer een wilde guh Kaas Knabbels (1 op 3 kans per stuk). Een tamme guh krijgt 1000 levens."),
           ("<b>Heal</b>: Kaas Knabbels heal 100 HP; Gefrituurde Kaasknabbels heal to full instantly.",
            "<b>Genezen</b>: Kaas Knabbels geven 100 levens; Gefrituurde Kaasknabbels maken hem meteen helemaal beter."),
           ("<b>Breed</b>: two tamed guhs at full health + Kaas Knabbels. Babies get a size in between their parents.",
            "<b>Fokken</b>: twee tamme guhs met volle levens + Kaas Knabbels. Baby's krijgen een formaat tussen dat van hun ouders in."),
           ("<b>Pet</b>: tap right-click (short) on your own guh. It squishes flat with happiness, eyes shut, pushes its head and a paw "
            "against your hand and wiggles, with pink hearts: <i>You pet ...!</i> Every pet is a moment, also when today's petting hearts are "
            "used up. Sitting and standing up is in the Guh menu (hold right-click).",
            "<b>Aaien</b>: tik kort rechtsklik op je eigen guh. Hij wordt even helemaal plat van geluk, ogen dicht, duwt zijn kopje en een "
            "pootje tegen je hand en wiebelt, met roze hartjes: <i>Je aait ...!</i> Elke aai is een momentje, ook als de aai-hartjes van "
            "vandaag op zijn. Zitten en opstaan doe je in het guhmenu (houd rechtsklik ingedrukt)."),
           ("<b>Ride</b>: big guhs (at least ~1.7 blocks long) take a saddle; then tap right-click to hop on.",
            "<b>Rijden</b>: grote guhs (minstens ~1,7 blok lang) kunnen een zadel dragen; tik dan rechtsklik om op te stappen."),
           ("<b>Pick up</b>: sneak + right-click your guh. It becomes an item that keeps its name, size, HP, saddle and armour. "
            "Right-click a block to put it down again.",
            "<b>Oppakken</b>: sluip + rechtsklik op je guh. Hij wordt een voorwerp dat zijn naam, formaat, levens, zadel en pantser "
            "houdt. Rechtsklik op een blok om hem weer neer te zetten."),
           ("<b>Name</b>: use a name tag or rename it in the Guh menu. Named guhs always show their name.",
            "<b>Naam</b>: gebruik een naamkaartje of hernoem hem in het guhmenu. Guhs met een naam laten die altijd zien.")]))}
<h3 style="margin-top:14px">{t("The Guh menu (hold right-click on your guh)", "Het guhmenu (houd rechtsklik ingedrukt op je guh)")}</h3>
<div class="tscroll"><table>
<tr><th>{t("Option", "Optie")}</th><th>{t("What it does", "Wat het doet")}</th></tr>
<tr><td>{t("HP, size, rideable", "Levens, formaat, berijdbaar")}</td><td>{t("Health bar, length in blocks and whether you can ride it (and why not).", "Levensbalk, lengte in blokken en of je erop kunt rijden (en waarom niet).")}</td></tr>
<tr><td>{t("Clothes, armour &amp; backpack", "Kleding, pantser &amp; rugzak")}</td><td>{t("Opens the wardrobe (new in 2.9): a big 3D preview, a tab per clothing slot with your unlocked pieces, search, filter, Dobbel! and favourite outfits, and a tab <i>Rugzak &amp; harnas</i> with the armour slot and the backpack.", "Opent de kledingkast (nieuw in 2.9): een groot 3D-voorbeeld, een tabblad per kledingvakje met je ontgrendelde stukken, zoeken, filteren, Dobbel! en lievelingsoutfits, en een tabblad <i>Rugzak &amp; harnas</i> met het pantservakje en de rugzak.")}</td></tr>
<tr><td>{t("Knuffelen!", "Knuffelen!")}</td><td>{t("A big vadsige cuddle: lots of hearts (new in 2.10). Afterwards your guh needs a moment to cool down.", "Een dikke vadsige knuffel: veel hartjes (nieuw in 2.10). Daarna moet je guh even bijkomen.")}</td></tr>
<tr><td>{t("Dagboekje", "Dagboekje")}</td><td>{t("Opens its page in the Guhdex tab <i>Mijn guhs</i> (new in 2.10): hearts, favorietjes, vriendjes, where it is and everything it did.", "Opent zijn pagina in het Guhdex-tabblad <i>Mijn guhs</i> (nieuw in 2.10): hartjes, favorietjes, vriendjes, waar hij is en alles wat hij heeft meegemaakt.")}</td></tr>
<tr><td>{t("Personality", "Karakter")}</td><td>{t("Its personality (see below). Hover over it to see what every personality does.", "Zijn karakter (zie hieronder). Houd je muis erop om te zien wat elk karakter doet.")}</td></tr>
<tr><td>{t("Sit / Stand", "Zitten / Opstaan")}</td><td>{t("Stay put or follow you. A sitting guh doesn't move at all, not even for kaas knabbels.", "Blijven zitten of je volgen. Een zittende guh beweegt helemaal niet, zelfs niet voor kaasknabbels.")}</td></tr>
<tr><td>{t("Teleport to me", "Naar mij teleporteren")}</td><td>{t("Whether it teleports to you when it falls behind.", "Of hij naar je toe teleporteert als hij achterblijft.")}</td></tr>
<tr><td>{t("Gravity", "Zwaartekracht")}</td><td>{t("On: heavy and squished flat, can't jump, falls faster.", "Aan: zwaar en platgedrukt, kan niet springen, valt sneller.")}</td></tr>
<tr><td>{t("Behaviour", "Gedrag")}</td><td>{t("<b>Passive (flee)</b>: runs from mobs that hurt it. <b>Passive</b>: does nothing. <b>Neutral</b>: fights back. <b>Aggressive</b>: attacks mobs within the attack radius (1&ndash;20 blocks) &mdash; never players, guhs or your pets. Bigger guhs hit harder.",
                                                                    "<b>Passief (vluchten)</b>: rent weg van mobs die hem pijn doen. <b>Passief</b>: doet niks. <b>Neutraal</b>: vecht terug. <b>Agressief</b>: valt mobs binnen de aanvalsstraal aan (1&ndash;20 blokken) &mdash; nooit spelers, guhs of je huisdieren. Grotere guhs slaan harder.")}</td></tr>
<tr><td>{t("Sounds...", "Geluiden...")}</td><td>{t("Guh noises on/off and how often (very rarely &hellip; very often).", "Guhgeluidjes aan/uit en hoe vaak (heel zelden &hellip; heel vaak).")}</td></tr>
<tr><td>{t("Rename", "Hernoem")}</td><td>{t("Type a name, no name tag needed.", "Typ een naam, geen naamkaartje nodig.")}</td></tr>
<tr><td>{t("Wander", "Rondvadsen")}</td><td>{t("Off: the guh stays put where it stands (without sitting down): no strolling around, no following you.", "Uit: de guh blijft staan waar hij staat (zonder te gaan zitten): niet rondlopen, niet achter je aan.")}</td></tr>
</table></div>
<h3 style="margin-top:18px">{t("Guh armour", "Guhpantser")}</h3>
{p("Right-click your tamed guh with guh armour: it gets a little helmet with a brim and plates on its sides, in the colour of the tier.",
   "Rechtsklik op je tamme guh met guhpantser: hij krijgt een helmpje met rand en plaatjes op zijn zij, in de kleur van het soort pantser.")}
<div class="cards">
<div class="card"><figure class="stage">{img("guh_armor_iron", "Iron guh armour")}</figure><h3>{icon("iron_guh_armor", "Iron guh armour")} {t("Iron", "IJzer")}</h3><p>{t("Armour 5", "Pantser 5")}</p></div>
<div class="card"><figure class="stage">{img("guh_armor_diamond", "Diamond guh armour")}</figure><h3>{icon("diamond_guh_armor", "Diamond guh armour")} {t("Diamond", "Diamant")}</h3><p>{t("Armour 11, toughness 2", "Pantser 11, taaiheid 2")}</p></div>
<div class="card"><figure class="stage">{img("guh_armor_netherite", "Netherite guh armour")}</figure><h3>{icon("netherite_guh_armor", "Netherite guh armour")} Netherite</h3><p>{t("Armour 11, toughness 3, knockback resistance", "Pantser 11, taaiheid 3, terugslagbestendig")}</p></div>
</div>"""
    OUTFITS = [("guh_outfit_onesie", "Pink onesie", "Roze onesie", "pink_onesie"),
               ("guh_outfit_sweater", "Striped sweater", "Gestreepte trui", "striped_sweater"),
               ("guh_outfit_rain", "Raincoat + sou'wester", "Regenjas + zuidwester", "raincoat"),
               ("guh_outfit_party", "Party hat + red bow tie", "Feesthoedje + rode strik", "party_hat"),
               ("guh_outfit_chef", "Chef jacket, hat + black bow tie", "Koksbuis, -muts + zwarte strik", "chef_hat")]
    THEMES = [("Work", "Werk", [("guh_outfit_work_fire", "Firefighter", "Brandweer"), ("guh_outfit_work_police", "Police", "Politie"),
                                 ("guh_outfit_work_doctor", "Doctor", "Dokter"), ("guh_outfit_work_builder", "Builder", "Bouwvakker"),
                                 ("guh_outfit_work_farmer", "Farmer", "Boer")]),
              ("Holidays", "Feestdagen", [("guh_outfit_holiday_christmas", "Christmas", "Kerst"), ("guh_outfit_holiday_sint", "Sinterklaas", "Sinterklaas"),
                                          ("guh_outfit_holiday_piet", "Piet", "Piet"), ("guh_outfit_holiday_halloween", "Halloween", "Halloween"),
                                          ("guh_outfit_holiday_witch", "Witch", "Heks"), ("guh_outfit_holiday_kingsday", "King's Day", "Koningsdag")]),
              ("Fantasy", "Fantasy", [("guh_outfit_fantasy_wizard", "Wizard", "Tovenaar"), ("guh_outfit_fantasy_knight", "Knight", "Ridder"),
                                      ("guh_outfit_fantasy_royal", "Royal", "Koninklijk"), ("guh_outfit_fantasy_pirate", "Pirate", "Piraat")])]
    theme_html = "".join(f'<h3 style="margin-top:12px">{t(en, nl)}</h3><div class="cards">' + "".join(
        f'<div class="card"><figure class="stage">{img(r, ren)}</figure><h3>{t(ren, rnl)}</h3></div>' for r, ren, rnl in looks) + "</div>"
        for en, nl, looks in THEMES)
    CLOTHES_ICONS = ["pink_onesie", "striped_sweater", "raincoat", "chef_jacket", "rain_hat", "party_hat", "chef_hat", "red_bowtie",
                     "black_bowtie", "sunglasses", "heart_glasses", "monocle", "eyepatch", "firefighter_helmet", "firefighter_jacket",
                     "police_cap", "police_uniform", "doctor_coat", "stethoscope", "builder_helmet", "safety_vest", "straw_hat",
                     "overalls", "santa_hat", "christmas_sweater", "winter_scarf", "sint_mitre", "piet_beret", "witch_hat",
                     "ghost_sheet", "pumpkin_head", "orange_crown", "orange_shirt", "wizard_hat", "wizard_robe", "knight_helmet",
                     "knight_armour", "royal_crown", "royal_cape", "pirate_hat", "guh_backpack"]
    all_icons = "".join(icon(c, c.replace("_", " ")) for c in CLOTHES_ICONS)
    care += f'''<h3 style="margin-top:18px">{t("Guh clothes and the wardrobe", "Guhkleertjes en de kledingkast")}</h3>
{p("Dress up your tamed guh! A guh has seven clothing slots: <b>head</b>, <b>eyes</b>, <b>body</b>, <b>neck</b>, <b>back</b>, <b>hair</b> (2.8, at the kapper) "
   "and <b>ears</b> (2.9). Since 2.9 clothes are <b>unlocks</b>: hold right-click on a piece for 1.5 seconds to use it up, and from then on all your tamed guhs "
   "can wear it (see <i>New in 2.9</i>). Dress a guh in the wardrobe (<b>Clothes, armour &amp; backpack</b> in the Guh menu), or right-click your guh with a "
   "piece you already unlocked. Suits cover the body but leave the paws free. These are the first 41 pieces: everyday clothes, <b>work</b> outfits, "
   "<b>holiday</b> outfits and <b>fantasy</b> outfits; there are 141 unlockable pieces now.",
   "Kleed je tamme guh aan! Een guh heeft zeven kledingvakjes: <b>hoofd</b>, <b>ogen</b>, <b>lijf</b>, <b>nek</b>, <b>rug</b>, <b>haar</b> (2.8, bij de kapper) "
   "en <b>oren</b> (2.9). Sinds 2.9 zijn kleertjes <b>ontgrendelingen</b>: houd rechtsklik 1,5 seconde ingedrukt op een stuk om het op te maken, en vanaf dan "
   "kunnen al je tamme guhs het aan (zie <i>Nieuw in 2.9</i>). Kleed een guh aan in de kledingkast (<b>Kleding &amp; rugzak</b> in het guhmenu), of rechtsklik "
   "je guh met een stuk dat je al ontgrendeld hebt. Pakjes bedekken het lijf maar laten de pootjes vrij. Dit zijn de eerste 41 stuks: gewone kleertjes, "
   "<b>werk</b>kleding, <b>feestdag</b>kleding en <b>fantasy</b>kleding; inmiddels zijn er 141 ontgrendelbare stukken.")}
{ul([("<b>Where</b> (since 2.9 every piece has exactly one source; the Guhdex tab <i>Kleding</i> shows it): the <b>Guh kleermaker</b> in guh villages always "
      "sells his whole everyday set (and the oorstrikjes); the chef set is at Bakker Korstje, the straw hat and overalls at Boerin Hooibaal, the work outfits come "
      "from the four <b>beroepen</b>, the party hat from picnics, the heart glasses, monocle and royal crown from the Guhdex, the onesie from taming a Brococolief "
      "guh, and the backpack you craft. Holiday and fantasy pieces are found in chests: hamster houses (Sinterklaas, winter), picnics (Christmas, King's Day), "
      "Evil Mika homes (Halloween), caves (pirate, knight) and the guhramid (royal cape, wizard).",
      "<b>Waar</b> (sinds 2.9 heeft elk stuk precies één bron; het Guhdex-tabblad <i>Kleding</i> laat hem zien): de <b>guhkleermaker</b> in guhdorpen verkoopt "
      "altijd zijn hele gewone setje (en de oorstrikjes); het koksetje is bij Bakker Korstje, de strohoed en tuinbroek bij Boerin Hooibaal, de werkkleding komt van "
      "de vier <b>beroepen</b>, het feesthoedje uit picknicks, de hartjesbril, het monocle en de koninklijke kroon van de Guhdex, de onesie van het temmen van een "
      "Brococolief-guh, en de rugzak maak je zelf. Feestdag- en fantasykleding vind je in kisten: hamsterhuizen (Sinterklaas, winter), picknicks (kerst, "
      "Koningsdag), Evil Mika-huizen (Halloween), grotten (piraat, ridder) en de guhramide (koningsmantel, tovenaar)."),
     ("<b>Wild guhs</b> wear clothes just for looks: since 2.9 guhs never drop clothes.",
      "<b>Wilde guhs</b> dragen kleertjes gewoon omdat het leuk staat: sinds 2.9 laten guhs nooit kleertjes vallen.")])}
<div class="iconrow" style="display:flex;flex-wrap:wrap;gap:6px;margin:10px 0">{all_icons}</div>
<div class="cards">{"".join(f'<div class="card"><figure class="stage">{img(r, en)}</figure><h3>{icon(ic, en)} {t(en, nl)}</h3></div>' for r, en, nl, ic in OUTFITS)}</div>
{theme_html}
{entry(img("guh_outfit_backpack", "Guh backpack"), "Guh backpack", "Guhrugzak",
       p("A little pink backpack for the <b>back</b> slot. While your guh wears it, the wardrobe tab <i>Rugzak &amp; harnas</i> shows <b>18 extra slots</b>: "
         "your guh carries your stuff. The 18 slots belong to that guh, and the backpack only comes off when it's empty. Craft it from pink wool, string and a "
         "chest (since 2.9 that's the only way to get one).",
         "Een roze rugzakje voor het <b>rug</b>vakje. Zolang je guh hem draagt, laat het kledingkast-tabblad <i>Rugzak &amp; harnas</i> <b>18 extra vakjes</b> "
         "zien: je guh draagt je spullen. De 18 vakjes horen bij die guh, en de rugzak gaat alleen af als hij leeg is. Maak hem van roze wol, touw en een kist "
         "(sinds 2.9 is dat de enige manier)."))}
{entry(img("guh_saddle", "Guh launch"), "Launch! (riding)", "Lanceren! (rijden)",
       p("Riding your own guh with an <b>empty hand</b>? Right-click: your guh sucks in air for 4 seconds, puffing up... and then "
         "<b>shoots off</b> straight ahead, fast and slowly sinking, until it hits something. The landing is a harmless <b>plof</b> "
         "that pushes mobs away (no damage to anyone). Right-click again in flight to lock the distance: it stops right there and drops straight down. Then it needs 10 seconds "
         "to catch its breath.",
         "Rijd je op je eigen guh met een <b>lege hand</b>? Rechtsklik: je guh zuigt 4 seconden lucht naar binnen en wordt dikker... "
         "en <b>schiet</b> dan recht vooruit weg, snel en langzaam zakkend, tot hij ergens tegenaan vliegt. De landing is een "
         "onschuldige <b>plof</b> die mobs wegduwt (niemand krijgt schade). Rechtsklik nog eens in de lucht om de afstand vast te zetten: hij stopt meteen en valt "
         "recht naar beneden. Daarna moet hij 10 seconden op adem komen."))}'''
    S.append(section("care", "Your guh", "Jouw guh", care))

    # --- personalities ---------------------------------------------------------------------------------------------------
    PERSONALITIES = [
        ("Playful", "Speels", "A bit faster than other guhs, and now and then it does happy zoomies: a few quick runs and jumps.",
         "Een beetje sneller dan andere guhs, en af en toe doet hij blije rondjes: een paar snelle sprintjes en sprongetjes."),
        ("Lazy", "Lui", "Slower, and wanders around much less. Would rather just lie there.", "Langzamer, en loopt veel minder rond. Ligt liever gewoon."),
        ("Vadsig", "Vadsig", "Sniffs out kaas knabbels lying on the ground (up to 10 blocks away) and eats them, healing itself. Tames more easily (1 in 2).",
         "Snuffelt kaasknabbels op de grond op (tot 10 blokken ver) en eet ze op, wat hem geneest. Makkelijker te temmen (1 op 2)."),
        ("Shy", "Verlegen", "Wild: keeps away from players, unless they hold kaas knabbels. Harder to tame (1 in 5).",
         "Wild: blijft uit de buurt van spelers, tenzij ze kaasknabbels vasthouden. Moeilijker te temmen (1 op 5)."),
        ("Brave", "Dapper", "Hits 50% harder and never runs away, not even when it's hurt.", "Slaat 50% harder en rent nooit weg, zelfs niet als hij pijn heeft."),
        ("Chatty", "Kletskous", "Makes guh noises twice as often.", "Maakt twee keer zo vaak guhgeluidjes."),
        ("Cuddly", "Knuffelig", "When tamed and close to you, it heals you half a heart about every 30 seconds (with little hearts).",
         "Als hij tam is en dicht bij je, geneest hij je ongeveer elke 30 seconden een half hartje (met hartjes)."),
        ("Curious", "Nieuwsgierig", "Now and then walks up to a nearby player and has a good look at them.",
         "Loopt af en toe naar een speler in de buurt en bekijkt die eens goed."),
    ]
    prows = "".join(f'<tr><td><b>{t(en, nl)}</b></td><td>{t(den, dnl)}</td></tr>' for en, nl, den, dnl in PERSONALITIES)
    pers = p("Every guh is born with one of eight personalities. You can see it in the Guh menu (hover over it for a list of "
             "all of them). Babies usually take after one of their parents, but sometimes have a personality of their own.",
             "Elke guh wordt geboren met een van acht karakters. Je ziet het in het guhmenu (houd je muis erop voor een lijstje van "
             "allemaal). Baby's lijken meestal op een van hun ouders, maar hebben soms een eigen karakter.") + \
        f'<div class="tscroll"><table><tr><th>{t("Personality", "Karakter")}</th><th>{t("What it does", "Wat het doet")}</th></tr>{prows}</table></div>'
    S.append(section("personalities", "Personalities", "Karakters", pers))

    # --- the guh stomach -------------------------------------------------------------------------------------------------
    maag = entry(img("npc_moeder_vadsig", "Mother Vadsig"), "Your own guh stomach", "Je eigen guhmaag",
                 p("Every player can get their own <b>guh stomach</b>: a private pocket dimension inside a very, very big guh. "
                   "It starts as a 48&times;48 room with soft pink stomach walls, half-digested kaasknabbel arches and bubbling "
                   "stomach acid (only for the look &mdash; it doesn't hurt). Build whatever you like in it. To get one, you first "
                   "have to help <b>Mother Vadsig</b>.",
                   "Elke speler kan een eigen <b>guhmaag</b> krijgen: een eigen zakdimensie in een heel, heel grote guh. Hij begint "
                   "als een kamer van 48&times;48 met zachte roze maagwanden, half verteerde kaasknabbelbogen en borrelend maagzuur "
                   "(alleen voor het zicht &mdash; het doet geen pijn). Bouw erin wat je maar wilt. Om er een te krijgen moet je eerst "
                   "<b>Moeder Vadsig</b> helpen."), wide=True)
    QUEST = [
        ("npc_moeder_vadsig", "1. Mother Vadsig", "1. Moeder Vadsig",
         "Very rarely you find the <b>Vadsig shrine</b> in the Guhmension: a round pink temple with a huge sitting guh on a cushion "
         "(the <b>shrine compass</b> from the vads temmer points the way). "
         "Talk to her (right-click): her little <b>Guhbert</b> has been kidnapped by the Mikas! She gives you the <b>Mika trail compass</b>.",
         "Heel zeldzaam vind je in de Guhmensie het <b>Vadsig-heiligdom</b>: een rond roze tempeltje met een enorme zittende guh op "
         "een kussen (het <b>heiligdomkompas</b> van de vadstemmer wijst de weg). Praat met haar (rechtsklik): haar kleine <b>Guhbert</b> is ontvoerd door de Mika's! Je krijgt het "
         "<b>Mika-spoorkompas</b>."),
        ("mika_baas", "2. The Mika camp", "2. Het Mika-kamp",
         "Follow the compass to a <b>Mika camp</b>: black tents, a soul campfire and Guhbert in a cage. The <b>Mika boss</b> won't fight "
         "&mdash; he wants to play <b>rock-paper-scissors-VADS</b>. Win three times in a row &mdash; but Mika seems to win every time... until he lets slip that there's a secret fourth move. Win and he "
         "squeaks <i>\"NJEG... IK BEN GEVADST!\"</i>. Guhbert is free and follows you as your own tame baby guh.",
         "Volg het kompas naar een <b>Mika-kamp</b>: zwarte tentjes, een zielenkampvuur en Guhbert in een kooi. De <b>Mika-baas</b> "
         "wil niet vechten &mdash; hij wil <b>steen-papier-schaar-VADS</b> spelen. Win drie keer op rij &mdash; maar Mika lijkt elke keer te winnen... tot hij verklapt dat er een geheime vierde zet is. Win je, dan "
         "piept hij <i>\"NJEG... IK BEN GEVADST!\"</i>. Guhbert is vrij en volgt jou als je eigen tamme babyguh."),
        ("guh_sitting", "3. The lost cake", "3. De verloren taart",
         "Back home, Mother Vadsig's party cake is gone! Follow the <b>cake crumbs</b> (another compass) to a <b>guh picnic</b>: "
         "walk onto it and you find the lost cake. Also collect <b>3 guh balloons</b> (picnic chests, or craft them).",
         "Terug thuis blijkt de feesttaart van Moeder Vadsig weg! Volg de <b>taartkruimels</b> (nog een kompas) naar een "
         "<b>guhpicknick</b>: loop erop en je vindt de verloren taart. Verzamel ook <b>3 guh-ballonnen</b> (picknickkisten, of maak ze)."),
        ("npc_moeder_vadsig", "4. The party", "4. Het feest",
         "Bring the cake and the balloons to Mother Vadsig: fireworks, a party hat for Guhbert &mdash; and you get the "
         "<b>Guh belly whistle</b>... and then she <b>swallows you whole</b>. NJEG... HAP! Your stomach is a spot inside her belly.",
         "Breng de taart en de ballonnen naar Moeder Vadsig: vuurwerk, een feesthoedje voor Guhbert &mdash; en jij krijgt het "
         "<b>Guh-buikfluitje</b>... en dan <b>slikt ze je in</b>. NJEG... HAP! Je maag is een plekje in haar buik."),
    ]
    maag += '<div class="cards">' + "".join(f'<div class="card"><figure class="stage">{img(r, en)}</figure><h3>{t(en, nl)}</h3><p>{t(den, dnl)}</p></div>'
                                            for r, en, nl, den, dnl in QUEST) + "</div>"
    maag += f'''<h3 style="margin-top:14px">{t("Getting around", "Heen en terug")}</h3>
{ul([("<b>Blow the belly whistle</b> or press <b>G</b> (change it in Controls &rarr; Guhs): you're in your stomach. Blow again (or press G) "
      "to go back to exactly where you were.",
      "<b>Blaas op het buikfluitje</b> of druk op <b>G</b> (aan te passen bij Besturing &rarr; Guhs): je bent in je maag. Blaas nog eens "
      "(of druk op G) om terug te gaan naar precies waar je was."),
     ("In your stomach are two portals with signs: <b>&quot;naar de mond&quot;</b> (to the mouth) and <b>&quot;naar de darmen&quot;</b> "
      "(through the intestines: back to the world).",
      "In je maag staan twee portalen met bordjes: <b>&quot;naar de mond&quot;</b> en <b>&quot;naar de darmen&quot;</b> (terug naar de wereld)."),
     ("<b>The mouth</b> is the lobby everyone shares: a tongue floor between rows of teeth, with a portal to every player's stomach "
      "(with their name above it). Visit your friends!",
      "<b>De mond</b> is de lobby van iedereen: een tongvloer tussen rijen tanden, met een portaal naar de maag van elke speler (met "
      "zijn naam erboven). Ga bij je vrienden op bezoek!")])}
{entry(img("npc_maagenzym", "Stomach Enzyme Guh"), "The Stomach Enzyme Guh: who may come in", "De Maagenzym-guh: wie mag erin",
       p("Next to your portals sits your <b>Stomach Enzyme Guh</b>. Right-click it for your stomach's settings: <b>public</b> (the "
         "default: anyone may visit), <b>private</b> (only you) or <b>whitelist</b> (only the players you add &mdash; and per player "
         "you choose whether they may build). It also shows how big your stomach is and how to make it bigger. Only you (and "
         "whitelisted builders) can build, break or open things in your stomach.",
         "Naast je portalen zit je <b>Maagenzym-guh</b>. Rechtsklik erop voor de instellingen van je maag: <b>openbaar</b> (standaard: "
         "iedereen mag komen), <b>privé</b> (alleen jij) of <b>whitelist</b> (alleen de spelers die je toevoegt &mdash; en per speler "
         "kies je of ze mogen bouwen). Hij laat ook zien hoe groot je maag is en hoe je hem groter maakt. Alleen jij (en bouwers op de "
         "whitelist) kunnen in je maag bouwen, breken of dingen openen."))}
'''
    maag += entry(img("npc_tandarts", "Dentist Guh"), "The Dentist Guh: a bigger stomach", "De Tandarts-guh: een grotere maag",
                  p("In the mouth sits the <b>Dentist Guh</b>. Do its jobs and your stomach stretches:",
                    "In de mond zit de <b>Tandarts-guh</b>. Doe zijn klusjes en je maag rekt uit:") +
                  '<div class="tscroll"><table><tr><th>' + t("Size", "Grootte") + '</th><th>' + t("What the dentist wants", "Wat de tandarts wil") + '</th></tr>'
                  + '<tr><td>48 &rarr; 64</td><td>' + t("64 kaas knabbels, 16 vahoege vads ingots, 8 guh crystals", "64 kaasknabbels, 16 vadsstaven, 8 guhkristallen") + '</td></tr>'
                  + '<tr><td>64 &rarr; 80</td><td>' + t("16 guh crystals, and you must have defeated Big Mika", "16 guhkristallen, en je moet Grote Mika verslagen hebben") + '</td></tr>'
                  + '<tr><td>80 &rarr; 96</td><td>' + t("32 guh crystals, and bring a Golden Guh along", "32 guhkristallen, en neem een Gouden Guh mee") + '</td></tr>'
                  + '<tr><td>96 &rarr; 112</td><td>' + t("32 guh slimeballs and 8 kaashoning (a softer stomach wall)", "32 guhslijmballen en 8 kaashoning (een zachtere maagwand)") + '</td></tr>'
                  + '<tr><td>112 &rarr; 128</td><td>' + t("16 guh blossom saplings, 48 guh crystals and a full Guhdex", "16 guhbloesemboompjes, 48 guhkristallen en een volle Guhdex") + '</td></tr></table></div>'
                  + p("Every size is a little story inside Moeder Vadsig's belly: stomach cramps, a Mika smell, a golden visitor, a softer "
                      "wall and finally a blossom garden. While you're in your stomach, she now and then talks to you from above. After every "
                      "step of the quest a golden <b>Volgende stap</b> message tells you exactly what to do next.",
                      "Elke grootte is een klein verhaal in de buik van Moeder Vadsig: maagkrampjes, een Mika-geur, gouden visite, een zachtere "
                      "wand en tot slot een bloesemtuin. Terwijl je in je maag bent, praat ze af en toe tegen je van boven. Na elke stap van de "
                      "opdracht vertelt een gouden <b>Volgende stap</b>-bericht precies wat je nu moet doen."))
    S.append(section("maag", "The guh stomach", "De guhmaag", maag))

    # --- the guh sled ---------------------------------------------------------------------------------------------------
    sled = entry(img("guh_slee", "Guh sled"), "The guh sled", "De guh-slee",
                 p("A pink sled with guh ears on the back and a bell on the front, <b>pulled by four little guhs</b> that walk along the rails ahead of it. It turns with every bend and rides on its own <b>sled rails</b>, forwards "
                   "or backwards, up and down hills. Get on with right-click; <b>right-click while riding</b> opens the control panel: "
                   "<b>start / stop</b>, <b>turn around</b> and <b>three speeds</b> (slow, normal, WHEEE). At the end of the line the "
                   "sled stops and turns around, ready to go back. There's room for two (a guh fits behind you). Sneak + right-click "
                   "or hit an empty sled to pick it up.",
                   "Een roze slee met guhoortjes op de rugleuning en een belletje voorop, <b>voortgetrokken door vier kleine guhs</b> die voor hem uit over de rails lopen. Hij draait mee in elke bocht en rijdt op zijn eigen <b>sleerails</b>, "
                   "vooruit of achteruit, heuvels op en af. Stap in met rechtsklik; <b>rechtsklik tijdens het rijden</b> opent het "
                   "bedieningspaneel: <b>start / stop</b>, <b>omkeren</b> en <b>drie snelheden</b> (langzaam, normaal, WIEEE). Aan het "
                   "eind van de baan stopt de slee en keert hij om, klaar om terug te gaan. Er is plek voor twee (een guh past achter "
                   "je). Sluip + rechtsklik of sla een lege slee om hem op te pakken."), wide=True) + \
        entry(img("npc_slee_guh", "Sled Guh"), "Quest: the broken sled", "Opdracht: de kapotte slee",
              p("On the <b>Guh Peaks</b> stands the <b>sled hut</b>, a snowy log cabin. The <b>Sled Guh</b> outside is sad: her sled is "
                "broken. She needs three parts: a <b>sled runner</b> (in guh cave chests), a <b>guh bell</b> (in guh village chests) and "
                "a <b>pink ribbon</b> (sold by the kleermaker). Bring all three and you get a <b>guh sled</b> and the <b>sled builder's "
                "book</b>: with it you can craft sled rails (the book stays in the crafting grid).",
                "Op de <b>Guhpieken</b> staat de <b>sleehut</b>, een besneeuwd blokhutje. De <b>Slee-guh</b> ervoor is verdrietig: haar "
                "slee is kapot. Ze heeft drie onderdelen nodig: een <b>sleeglijder</b> (in kisten in guhgrotten), een <b>guh-belletje</b> "
                "(in kisten in guhdorpen) en een <b>roze lint</b> (bij de kleermaker). Breng ze alle drie en je krijgt een <b>guh-slee</b> "
                "en het <b>sleebouwersboek</b>: daarmee maak je sleerails (het boek blijft in het rooster liggen)."))
    RAILS = [("rail_straight", "Straight", "Recht", "4 blocks straight on.", "4 blokken rechtdoor."),
             ("rail_curve_right", "Curve", "Bocht", "A quarter turn to the right; <b>sneak</b> for a left turn.", "Een kwartslag naar rechts; <b>sluip</b> voor links."),
             ("rail_slope", "Slope", "Helling", "4 forward and 4 up. <b>Sneak</b> when adding it to a track to go <b>down</b> instead.",
              "4 vooruit en 4 omhoog. <b>Sluip</b> als je hem aan een baan vastmaakt om juist te <b>dalen</b>.")]
    sled += f'''<h3 style="margin-top:14px">{t("Building a track", "Een baan bouwen")}</h3>
{p("Every rail piece is 4&times;4 blocks. Click on the ground to start a track in the direction you're looking; click on the <b>end "
   "of an existing rail</b> to add the next piece exactly there. Breaking any block of a piece breaks the whole piece (you get "
   "it back). Put a sled on the rails with right-click.",
   "Elk stuk rail is 4&times;4 blokken. Klik op de grond om een baan te beginnen in de richting waarin je kijkt; klik op het "
   "<b>eind van een bestaande rail</b> om het volgende stuk precies daar vast te maken. Breek je een blok van een stuk, dan gaat "
   "het hele stuk (je krijgt het terug). Zet een slee met rechtsklik op de rails.")}
<div class="cards">{"".join(f'<div class="card"><figure class="stage">{img(r, en)}</figure><h3>{t(en, nl)}</h3><p>{t(den, dnl)}</p></div>' for r, en, nl, den, dnl in RAILS)}</div>
<figure class="stage" style="margin-top:10px">{img("sled_track", "A sled track")}</figure>
{p("<b>Guhland</b> has a sled coaster now: a big loop with a hill, a station with a ladder, and two Guhland sleds that can't "
   "be taken home (one with a guh already riding it).",
   "<b>Guhland</b> heeft nu een sleebaan: een grote lus met een heuvel, een station met een ladder en twee Guhland-sleeën die je "
   "niet mee naar huis kunt nemen (in een ervan rijdt al een guh).")}'''
    S.append(section("sled", "The guh sled", "De guh-slee", sled))

    # --- the guhdex -----------------------------------------------------------------------------------------------------
    dex = entry(icon("guhdex", "Guhdex").replace('class="px"', 'class="px" style="width:96px;height:96px"'), "The Guhdex", "De Guhdex",
                p("Craft a <b>Guhdex</b> (a book, kaas knabbels and pink dye) and right-click it. Every kind of guh you've been close to "
                  "(within 3 blocks since 2.10; it used to be 1) fills in its page, with a picture, how rare it is and something about it; guhs you haven't seen are a "
                  "dark \"???\". Tamed a guh of that kind? It gets a gold star. Collect them all for rewards:",
                  "Maak een <b>Guhdex</b> (een boek, kaasknabbels en roze kleurstof) en rechtsklik erop. Elke soort guh waar je dichtbij "
                  "hebt gestaan (binnen 3 blokken sinds 2.10; vroeger 1) vult zijn pagina in, met een plaatje, hoe zeldzaam hij is en iets over hem; guhs die je nog "
                  "niet zag zijn een donker \"???\". Een guh van die soort getemd? Dan krijgt hij een gouden ster. Verzamel ze allemaal "
                  "voor beloningen:") +
                ul([("5 seen: heart glasses", "5 gezien: hartjesbril"), ("8 seen + 3 tamed: a monocle", "8 gezien + 3 getemd: een monocle"),
                    ("All seen: the guh crystal spyglass", "Alles gezien: de guh-kristalverrekijker"),
                    ("All seen and all tamed: the royal crown", "Alles gezien en alles getemd: de koninklijke kroon")]) +
                p("New pages in 2.7: the variants <b>Asguh</b> and <b>Kaasmoerasguh</b> (tame one for a star), the characters <b>Grillguh</b>, <b>Boswachterguh</b> and "
                  "<b>Knabbelplukker</b>, and the creatures <b>Kikkerguh</b>, <b>Kaasmot</b> and <b>Moerasheks-Mika</b> (stand close to them). How many Rookguhs you saved "
                  "is on the Rookguh's own page (since 2.10.1; it used to be at the top of the Guhdex).",
                  "Nieuwe pagina's in 2.7: de varianten <b>Asguh</b> en <b>Kaasmoerasguh</b> (tem er een voor een ster), de personages <b>Grillguh</b>, <b>Boswachterguh</b> en "
                  "<b>Knabbelplukker</b>, en de wezens <b>Kikkerguh</b>, <b>Kaasmot</b> en <b>Moerasheks-Mika</b> (ga er dichtbij staan). Hoeveel Rookguhs je hebt "
                  "gered staat op de eigen pagina van de Rookguh (sinds 2.10.1; vroeger bovenaan de Guhdex).") +
                p("Also new in 2.7: the <b>Highscores</b> tab with your best score and the server record of every minigame (see <i>New in 2.7</i>), and a free Guhdex "
                  "the first time you step into the Guhmension through a portal without one.",
                  "Ook nieuw in 2.7: het tabblad <b>Highscores</b> met je beste score en het serverrecord van elke minigame (zie <i>Nieuw in 2.7</i>), en een gratis "
                  "Guhdex als je zonder er een via een portaal de Guhmensie binnenstapt.") +
                p("New in 2.8: the Guhdex has three tabs, <b>Guhs</b>, <b>Knus</b> and <b>Highscores</b>. <i>Knus</i> holds the milestones and collections of the Knuffeldal "
                  "and everything around it (see <i>New in 2.8</i>). New pages: the <b>Pluisguh</b> (a variant: tame one for a star), the characters of the Knuffeldal, the "
                  "<b>Kruimel-Mika</b>, the <b>guhschaapje</b>, <b>knabbelkippetje</b> and <b>guhkoe</b>, and <b>IJscoguh Tingeling</b>.",
                  "Nieuw in 2.8: de Guhdex heeft drie tabbladen, <b>Guhs</b>, <b>Knus</b> en <b>Highscores</b>. <i>Knus</i> bevat de mijlpalen en verzamelingen van het Knuffeldal "
                  "en alles eromheen (zie <i>Nieuw in 2.8</i>). Nieuwe pagina's: de <b>Pluisguh</b> (een variant: tem er een voor een ster), de personages van het Knuffeldal, de "
                  "<b>Kruimel-Mika</b>, het <b>guhschaapje</b>, <b>knabbelkippetje</b> en de <b>guhkoe</b>, en <b>IJscoguh Tingeling</b>.") +
                p("New in 2.8.1: the Knus tab has the section <b>Piep!</b> with twelve milestones and the <b>Piepboek</b> (six pages: the pieppiepmuisje, Poepschilly, "
                  "Schilly, the Roze Guh Koek, the boze kaasknabbel and the Boze Oppernabbel; see <i>New in 2.8.1</i>).",
                  "Nieuw in 2.8.1: het tabblad Knus heeft het onderdeel <b>Piep!</b> met twaalf mijlpalen en het <b>Piepboek</b> (zes pagina's: het pieppiepmuisje, "
                  "Poepschilly, Schilly, de Roze Guh Koek, de boze kaasknabbel en de Boze Oppernabbel; zie <i>Nieuw in 2.8.1</i>).") +
                p("New in 2.9: <b>icon tabs</b> on top: <b>Guhs</b>, <b>Knus</b>, <b>Minigames</b> (every game building, where it is, your records and the server "
                  "records per level, track, event or song; the old Highscores tab is in here) and <b>Kleding</b> (all 141 unlockable pieces, grouped per source: grey = "
                  "locked, green border = yours). New pages: the <b>Pinguh</b> and the eleven new guh characters (see <i>New in 2.9</i>).",
                  "Nieuw in 2.9: <b>icoontjestabbladen</b> bovenaan: <b>Guhs</b>, <b>Knus</b>, <b>Minigames</b> (elk spelgebouw, waar het is, je records en de "
                  "serverrecords per niveau, baan, onderdeel of liedje; het oude tabblad Highscores zit hierin) en <b>Kleding</b> (alle 141 ontgrendelbare stukken, "
                  "per bron: grijs = op slot, groen randje = van jou). Nieuwe pagina's: de <b>Pinguh</b> en de elf nieuwe guhpersonages (zie <i>Nieuw in 2.9</i>).") +
                p("New in 2.10: a fifth tab, <b>Mijn guhs</b>: a dagboekje for every tamed guh with its hearts, favorietjes, vriendjes, huisje and klusjes, where it is "
                  "right now, statistics, first times and wist-je-datjes (see <i>New in 2.10</i>). A page now fills in within <b>3 blocks</b>, for guh characters too.",
                  "Nieuw in 2.10: een vijfde tabblad, <b>Mijn guhs</b>: een dagboekje voor elke tamme guh met zijn hartjes, favorietjes, vriendjes, huisje en klusjes, "
                  "waar hij nu is, statistieken, eerste keren en wist-je-datjes (zie <i>Nieuw in 2.10</i>). Een pagina vult zich nu al binnen <b>3 blokken</b>, ook bij "
                  "guhpersonages.") +
                p("New in 2.10.1: the <b>Rookguh</b> has its own page with your saved-Rookguh count; it is a bonus page that doesn't count for the rewards. "
                  "The guh menu's <b>Dagboekje</b> opens straight on that guh's page in <i>Mijn guhs</i>.",
                  "Nieuw in 2.10.1: de <b>Rookguh</b> heeft een eigen pagina met hoeveel Rookguhs je hebt gered; het is een bonuspagina die niet meetelt voor de "
                  "beloningen. <b>Dagboekje</b> in het guhmenu opent meteen de pagina van die guh in <i>Mijn guhs</i>.") +
                p("New in 3.0: 29 pages (the Baltoguh, Guhtwo and 626-guh, Mieuwguh, the twelve new characters and the thirteen critters, Sjokkel included), "
                  "so the Guhdex has <b>95 pages</b>, 94 of which count. In <i>Mijn guhs</i> a guh that died stays with <i>&#9729; In de wolkjes... njeg</i> until the "
                  "Knuffelhart brings it back, and the Kleding tab groups the 3.0 clothes under <b>Guhverhalen</b> (see <i>New in 3.0</i>).",
                  "Nieuw in 3.0: 29 pagina's (de Baltoguh, Guhtwo en 626-guh, Mieuwguh, de twaalf nieuwe personages en de dertien diertjes, Sjokkel ook), dus de "
                  "Guhdex heeft nu <b>95 pagina's</b>, waarvan er 94 meetellen. In <i>Mijn guhs</i> blijft een guh die is doodgegaan staan met <i>&#9729; In de "
                  "wolkjes... njeg</i> tot het Knuffelhart hem terughaalt, en het tabblad Kleding zet de 3.0-kleertjes bij elkaar onder <b>Guhverhalen</b> (zie "
                  "<i>Nieuw in 3.0</i>).") +
                p("The sixth tab, <b>Verhalen</b> (the book icon), shows every story and questline with <b>your own progress</b>: Timmerguh, Baltoguh and "
                  "Nomguh, the kloon-eiland and the Guhtwo, the Hemelkapelletje and the Knuffelhart, Guhwai'i and the 626-guh, and the older adventures (De ontvoerde guh, "
                  "De kapotte slee, Het Grote Knusfeest, Het Guheinde, De Grillguh helpt and the four jobs). Each row says <i>Nog niet begonnen</i>, <i>Stap x van y</i> "
                  "or <i>Klaar! Vahoeg!</i>. Click a row for its page: <b>Wat nu?</b> (what to do now), <b>waar</b> (which character, where), the items you still need "
                  "(what you have versus what you need), every step ticked off and the rewards. Progress is per player and shared by every structure of that kind, "
                  "so a second kloon-eiland or Nomguh continues where you were.",
                  "Het zesde tabblad, <b>Verhalen</b> (het boekje), laat elk verhaal en elke questlijn zien met <b>je eigen voortgang</b>: Timmerguh, Baltoguh en "
                  "Nomguh, het kloon-eiland en de Guhtwo, het Hemelkapelletje en het Knuffelhart, Guhwai'i en de 626-guh, en de eerdere avonturen (De ontvoerde guh, "
                  "De kapotte slee, Het Grote Knusfeest, Het Guheinde, De Grillguh helpt en de vier beroepen). Elke rij zegt <i>Nog niet begonnen</i>, <i>Stap x van y</i> "
                  "of <i>Klaar! Vahoeg!</i>. Klik op een rij voor de pagina: <b>Wat nu?</b>, <b>waar</b> (welk personage, op welke plek), de spullen die je nog nodig "
                  "hebt (wat je hebt tegenover wat je nodig hebt), alle stappen met vinkjes en de beloningen. Voortgang is per speler en geldt voor elk bouwwerk van "
                  "die soort, dus op een tweede kloon-eiland of Nomguh ga je verder waar je was."), wide=True)
    S.append(section("guhdex", "Guhdex", "Guhdex", dex))

    # --- food, furniture and deco ---------------------------------------------------------------------------------------
    def deco_card(image, en, nl, den, dnl):
        return f'<div class="card"><figure class="stage">{image}</figure><h3>{t(en, nl)}</h3><p>{t(den, dnl)}</p></div>'
    big = lambda n, a: icon(n, a).replace('class="px"', 'class="px" style="width:96px;height:96px"')
    food = '<div class="cards">' + "".join([
        deco_card(img("guh_taart", "Guh cake"), "Guh cake", "Guhtaart", "A pink cake with guh eyes. Place it and eat it in 7 bites, like a normal cake.",
                  "Een roze taart met guhogen. Zet hem neer en eet hem op in 7 happen, net als een gewone taart."),
        deco_card(big("guh_cupcake", "Cupcake"), "Guh cupcake", "Guh-cupcake", "A little cake with pink frosting.", "Een taartje met roze glazuur."),
        deco_card(f'<div>{big("macaron_roze", "m")}{big("macaron_mint", "m")}<br>{big("macaron_citroen", "m")}{big("macaron_choco", "m")}</div>',
                  "Macarons", "Guhcarons", "In four colours: pink, mint, lemon and chocolate. Quick to eat.",
                  "In vier kleuren: roze, mint, citroen en chocolade. Snel op te eten."),
        deco_card(big("kaasfondue", "Kaasfondue"), "Cheese fondue", "Kaasfondguh", "A big warm bowl of melted kaas knabbels. Very filling.",
                  "Een grote warme kom gesmolten kaasknabbels. Heel vullend."),
        deco_card(big("kaasknabbel_milkshake", "Milkshake"), "Kaasknabbel milkshake", "Vahoege kaasknabbelshake",
                  "Clears all your effects, just like milk. You get the bottle back.", "Haalt al je effecten weg, net als melk. Je krijgt het flesje terug."),
        deco_card(big("kaashoning", "Kaashoning"), "Cheese honey", "Kaashoning", "From a full knabbelkorf. Like honey: cures poison.",
                  "Uit een volle knabbelkorf. Net als honing: geneest vergiftiging."),
    ]) + "</div>"
    deco = '<div class="cards">' + "".join([
        deco_card(img("guh_stoel", "Guh chair"), "Guh chair", "Guhstoel", "The backrest is a guh head, ears and all. <b>Sit</b> on it (right-click).",
                  "De rugleuning is een guhhoofd, met oortjes en al. <b>Ga erop zitten</b> (rechtsklik)."),
        deco_card(img("guh_tafel", "Guh table"), "Guh table", "Guhtafel", "A guh face looks up at you from the table top.", "Een guhgezichtje kijkt je aan vanaf het tafelblad."),
        deco_card(img("guh_bank", "Guh sofa"), "Guh sofa", "Vadszetel", "A soft sofa with a big guh head as its back.", "Een zachte bank met een groot guhhoofd als rugleuning."),
        deco_card(img("guh_kast", "Guh cupboard"), "Guh cupboard", "Vadskast", "A guh head with ears that stores 27 stacks, like a barrel.",
                  "Een guhhoofd met oortjes waar 27 stapels in passen, net als een ton."),
        deco_card(img("zitzakken", "Bean bags"), "Bean bags", "Vadszakken", "In all 16 wool colours, with little guh ears. Sit on them!", "In alle 16 wolkleuren, met guhoortjes. Ga erop zitten!"),
        deco_card(img("kussens", "Cushions"), "Cushions", "Guhkussens", "In all 16 wool colours, with little tassels. Also to sit on.",
                  "In alle 16 wolkleuren, met kwastjes. Ook om op te zitten."),
        deco_card(img("lampion_roze", "Lampgion"), "Lampgions", "Lampgions", "Pink, yellow and mint. Give full light; hang them under a block or put them down.",
                  "Roze, geel en mint. Geven vol licht; hang ze onder een blok of zet ze neer."),
        deco_card(img("vlaggetjes", "Bunting"), "Bunting", "Guhvlaggetjes", "A string of little flags. Put several in a row for a garland; they hang in the air.",
                  "Een touwtje met vlaggetjes. Zet er een paar op een rij voor een slinger; ze blijven in de lucht hangen."),
        deco_card(img("kaasbloem", "Guh flowers"), "Guh flowers", "Guhbloemen", "Four new flowers grow all over the Guhmension: cheese flower, guh ears, pink guh flower "
                  "and knabbel rose. Pot them, make dye of them &mdash; bees love them.",
                  "Vier nieuwe bloemen groeien overal in de Guhmensie: kaasbloem, guhoortjes, roze guhbloem en knabbelroos. Zet ze in een "
                  "pot, maak er kleurstof van &mdash; bijen zijn er dol op."),
        deco_card(img("knabbelkorf_honey", "Knabbelkorf"), "Knabbelkorf", "Knabbelkorf", "A beehive for guh bees. When it's full: <b>shears</b> give 3&ndash;5 kaas knabbels, "
                  "a <b>glass bottle</b> gives kaashoning.", "Een bijenkorf voor guhbijen. Als hij vol is: een <b>schaar</b> geeft 3&ndash;5 kaasknabbels, "
                  "een <b>glazen fles</b> geeft kaashoning."),
        deco_card(img("roze_slijmblok", "Pink slime block"), "Pink slime block", "Roze guhslijmblok", "From 9 guh slimeballs. Bouncy and sticky, exactly like a slime block "
                  "(also for sticky pistons: guh slimeball + piston).", "Van 9 guhslijmballen. Verend en plakkerig, precies als een slijmblok "
                  "(ook voor kleverige zuigers: guhslijmbal + zuiger)."),
        deco_card(img("guh_kristal_lamp", "Guh crystal lamp"), "Guh crystals", "Guhkristallen", "From the crystal mines: crystal blocks and crystal lamps "
                  "(full light), the guh crystal spyglass &mdash; and the dentist wants them too.",
                  "Uit de kristalmijnen: kristalblokken en kristallampen (vol licht), de guh-kristalverrekijker &mdash; en de tandarts wil ze ook."),
    ]) + "</div>"
    S.append(section("food", "Food, furniture and deco", "Eten, meubels en deco",
                     f'<h3>{t("Food", "Eten")}</h3>' + food + f'<h3 style="margin-top:14px">{t("Furniture and deco", "Meubels en deco")}</h3>' + deco))

    # --- items & blocks -------------------------------------------------------------------------------------------
    def card(image, en, nl, den, dnl):
        return f'<div class="card"><figure class="stage">{image}</figure><h3>{t(en, nl)}</h3><p>{t(den, dnl)}</p></div>'
    items = '<div class="cards">' + "".join([
        card(icon("kaas_knabbels", "Kaas Knabbels").replace('class="px"', 'class="px" style="width:96px;height:96px"'),
             "Kaas Knabbels", "Kaas Knabbels", "Tames, heals and breeds guhs. Also a quick snack. Found in kaasknabbel ores.",
             "Temt, geneest en fokt guhs. Ook een snel hapje. Te vinden in kaasknabbel-ertsen."),
        card(icon("gefrituurde_kaasknabbels", "Gefrituurde Kaasknabbels").replace('class="px"', 'class="px" style="width:96px;height:96px"'),
             "Gefrituurde Kaasknabbels", "Gefrituurde kaasknabbels", "Heals a tamed guh to full health instantly. Made in the frying pan.",
             "Maakt een tamme guh meteen helemaal beter. Gemaakt in de koekenpan."),
        card(icon("mika_vet", "Mika's vet").replace('class="px"', 'class="px" style="width:96px;height:96px"'),
             "Mika's Vet", "Mika's vet", "Dropped by Mika. One jar gives the frying pan fat for 64 kaas knabbels.",
             "Valt van Mika. Een pot geeft de koekenpan vet voor 64 kaasknabbels."),
        card(img("kaasknabbel_stone", "Kaasknabbel ores"), "Kaasknabbel ores", "Kaasknabbel-ertsen",
             "Stone, deepslate, dirt and cobblestone with cheese puffs, on every height. Drop 1&ndash;3 knabbels (Fortune works, Silk Touch gives the block).",
             "Steen, diepsteen, aarde en keien met kaaspuffen, op elke hoogte. Laten 1&ndash;3 knabbels vallen (Geluk werkt, Zijden aanraking geeft het blok)."),
        card(img("block_of_kaasknabbels", "Block of Kaasknabbels"), "Block of Kaasknabbels", "Blok kaasknabbels",
             "Nine knabbels in one block. Build a portal frame from it to reach the Guhmension.",
             "Negen knabbels in een blok. Bouw er een portaalframe van om naar de Guhmensie te gaan."),
        card(img("frying_pan", "Guh frying pan"), "Guh Frying Pan", "Guh-koekenpan",
             "Right-click with Mika's vet to add fat (+64, up to 256), then with kaas knabbels to fry them. Empty hand shows the fat left.",
             "Rechtsklik met Mika's vet voor vet (+64, tot 256), dan met kaasknabbels om ze te frituren. Lege hand laat het vet zien."),
        card(img("guh_spawner", "Guh spawner"), "Guh Spawner", "Guhspawner",
             "Found in hamster houses. Unlike normal spawners you can mine it with a pickaxe and take it home.",
             "Te vinden in hamsterhuizen. Anders dan gewone spawners kun je hem met een houweel meenemen."),
        card(img("guh_sitting", "Bank Guh"), "Bank Guh", "Bankguh",
             "So <i>vadsig</i> it stores infinite items in its stomach. Search (<code>@mod</code>), sort, filter, a crafting grid, deposit all. Keeps everything when broken.",
             "Zo <i>vadsig</i> dat er oneindig veel spullen in zijn buikje passen. Zoeken (<code>@mod</code>), sorteren, filteren, een werkbank, alles erin. Houdt alles als je hem breekt."),
        card(icon("picked_up_guh", "Picked-up guh").replace('class="px"', 'class="px" style="width:96px;height:96px"'),
             "Picked-up Guh", "Opgepakte guh", "Your guh, in your pocket. Put it down on a block, or into a Guh Wheel.",
             "Je guh, in je zak. Zet hem op een blok, of in een guhrad."),
        card(icon("guhmensie_superkompas_00", "Guhmension super compass").replace('class="px"', 'class="px" style="width:96px;height:96px"'),
             "Guhmension Super Compass", "Guhmensie-superkompas",
             "Right-click it and choose what to look for: guh caves, quest places, minigames, wonders, villages... Then it points to the nearest one (in the dimension you're in); hold it to see how far. Crafted from a compass, guh crystals and a vads ingot, sold by the vads temmer and the Mika hunter, dropped by Big Mika and found in guh chests. It replaces the old guh cave, challenge, kermis, verstop and king's compasses (old ones still work).",
             "Rechtsklik en kies wat je zoekt: guhgrotten, questplekken, minigames, wonderen, dorpen... Dan wijst hij naar de dichtstbijzijnde (in de dimensie waar je bent); houd hem vast om te zien hoe ver. Te maken van een kompas, guhkristallen en een vadsstaaf, te koop bij de vadstemmer en de Mika-jager, van Grote Mika en in guhkisten. Hij vervangt de oude guhgrot-, uitdagings-, kermis-, verstop- en koningskompassen (oude werken nog)."),
        card(icon("kaas_saus_bucket", "Bucket of kaas saus").replace('class="px"', 'class="px" style="width:96px;height:96px"'),
             "Bucket of Kaas Saus", "Emmer kaassaus", "Scoop kaas saus up and pour it wherever you like.",
             "Schep kaassaus op en giet het waar je maar wilt."),
    ]) + "</div>"
    items += '<div class="cards">' + "".join([
        card(icon("guh_kristal_verrekijker", "Spyglass").replace('class="px"', 'class="px" style="width:96px;height:96px"'),
             "Guh crystal spyglass", "Guh-kristalverrekijker",
             "A spyglass with a guh crystal lens. Use it and every <b>rare guh</b> within 64 blocks glows for 30 seconds &mdash; through walls.",
             "Een verrekijker met een lens van guhkristal. Gebruik hem en elke <b>zeldzame guh</b> binnen 64 blokken gloeit 30 seconden op &mdash; door muren heen."),
        card(icon("guh_buikfluitje", "Belly whistle").replace('class="px"', 'class="px" style="width:96px;height:96px"'),
             "Guh belly whistle", "Guh-buikfluitje", "Takes you to your guh stomach and back (so does the G key). A reward from Mother Vadsig.",
             "Brengt je naar je guhmaag en terug (net als de G-toets). Een beloning van Moeder Vadsig."),
        card(icon("mika_spoorkompas_00", "Mika trail compass").replace('class="px"', 'class="px" style="width:96px;height:96px"'),
             "Mika trail compass &amp; cake crumbs", "Mika-spoorkompas &amp; taartkruimels",
             "The two quest compasses: to the nearest Mika camp (Guhmension), and to the nearest guh picnic (Guhmension or overworld). Hold one to see the distance.",
             "De twee opdrachtkompassen: naar het dichtstbijzijnde Mika-kamp (Guhmensie), en naar de dichtstbijzijnde guhpicknick (Guhmensie of overworld). Houd er een vast om de afstand te zien."),
    ]) + "</div>"
    S.append(section("items", "Items and blocks", "Voorwerpen en blokken", items))

    # --- vahoege vads ---------------------------------------------------------------------------------------------------
    gear_icons = "".join(icon(f"vahoege_vads_{g}", g) for g in ("sword", "pickaxe", "axe", "shovel", "hoe", "paxel",
                                                                "helmet", "chestplate", "leggings", "boots"))
    vads = entry(img("compressed_super_vahoege_vads", "Compressed super vahoege vads"),
                 "Compressed Super Vahoege Vads", "Samengeperste supervahoege vads",
                 p("A glossy guh-pink ore hidden in the Guhmension's ground (never on the surface), from the bottom up to y90 "
                   "&mdash; and extra, sometimes even on the cliff faces, in the Vads Cliffs. Mine it with an <b>iron pickaxe</b> or "
                   "better: it drops <b>Vahoege Vads</b> (Fortune works). Smelt that into a <b>Vahoege Vads Ingot</b>.",
                   "Een glanzend guhroze erts diep in de grond van de Guhmensie (nooit aan het oppervlak), van de bodem tot y90 "
                   "&mdash; en extra veel, soms zelfs op de rotswanden, in de Vadskliffen. Hak het met een <b>ijzeren houweel</b> of "
                   "beter: je krijgt <b>Vahoege vads</b> (Geluk werkt). Smelt dat tot een <b>vahoege-vadsstaaf</b>."),
                 stats=[(("Tool", "Gereedschap"), t("iron pickaxe+", "ijzeren houweel+")),
                        (("Drops", "Laat vallen"), icon("vahoege_vads", "Vahoege vads") + " " + t("Vahoege Vads", "Vahoege vads")),
                        (("Smelts into", "Smelt tot"), icon("vahoege_vads_ingot", "Vads ingot") + " " + t("Vads Ingot", "Vadsstaaf"))]) + \
        entry(f'<div style="display:grid;grid-template-columns:repeat(5,32px);gap:6px">{gear_icons.replace(chr(34) + "px" + chr(34), chr(34) + "px" + chr(34) + " style=" + chr(34) + "width:32px;height:32px" + chr(34))}</div>',
              "Vads tools and armour", "Vadsgereedschap en -pantser",
              ul([("<b>As good as diamond</b>: the same mining speed, damage and armour (plus armour toughness 2).",
                   "<b>Zo goed als diamant</b>: net zo snel hakken, net zoveel schade en pantser (plus pantsertaaiheid 2)."),
                  ("<b>Never breaks</b>: infinite durability.", "<b>Gaat nooit kapot</b>: oneindig lang houdbaar."),
                  ("<b>Kept on death</b>: when you die, vads gear stays in your inventory (same slot) instead of dropping.",
                   "<b>Blijft bij je als je doodgaat</b>: vadsspullen blijven in je inventaris (zelfde vakje) in plaats van te vallen."),
                  ("<b>Paxel</b>: pickaxe, axe and shovel in one. Mines stone, wood and dirt at full speed, strips logs and makes paths. "
                   "Craft it from a vads pickaxe, axe and shovel.",
                   "<b>Paxel</b>: houweel, bijl en schep in een. Hakt steen, hout en aarde op volle snelheid, schilt stammen en maakt "
                   "paadjes. Maak hem van een vadshouweel, -bijl en -schep."),
                  ("Enchant them like any tool or armour.", "Betover ze zoals elk gereedschap of pantser.")]))
    S.append(section("vads", "Vahoege Vads", "Vahoege vads", vads))

    # --- redstone -------------------------------------------------------------------------------------------------
    red = entry(img("guh_wheel", "Guh wheel"), "Guh Wheel", "Guhrad",
                p("A big pink flower-shaped running wheel (3 blocks wide and tall). Pick up your tamed guh and right-click the wheel "
                  "with it: the guh runs and the wheel spins. While it runs, the wheel gives <b>full redstone power (15)</b> in every "
                  "direction. Right-click the running wheel to get your guh back.",
                  "Een groot roze bloemvormig loopwiel (3 blokken breed en hoog). Pak je tamme guh op en rechtsklik er het rad mee: "
                  "de guh rent en het rad draait. Zolang hij rent geeft het rad <b>volle redstonestroom (15)</b> in alle richtingen. "
                  "Rechtsklik op het draaiende rad om je guh terug te krijgen."), wide=True) + \
          entry(img("guh_wire", "Guh wire"), "Guh Wire", "Guhdraad",
                p("Pink redstone dust that <b>never fades</b>. It connects like redstone dust (dots, lines, corners, up steps). "
                  "When any part of a connected wire is powered, the whole wire gives full power &mdash; however long it is. "
                  "Dark rose when off, bright pink with sparkles when on. It's powered by anything that gives redstone power except "
                  "redstone dust (put a repeater in between).",
                  "Roze redstonestof die <b>nooit zwakker wordt</b>. Hij verbindt zoals redstonestof (puntjes, lijnen, hoeken, trapjes op). "
                  "Als er ergens stroom op een verbonden draad staat, geeft de hele draad volle stroom &mdash; hoe lang hij ook is. "
                  "Donker oudroze als hij uit is, felroze met glinsteringen als hij aan is. Werkt met alles wat redstonestroom geeft "
                  "behalve redstonestof (zet er een versterker tussen)."), wide=True)
    S.append(section("redstone", "Redstone", "Redstone", red))

    # --- guhmension -------------------------------------------------------------------------------------------------
    FLAT, ROLL, STEEP = ("almost flat", "bijna vlak"), ("rolling hills", "glooiende heuvels"), ("very steep", "heel steil")
    biomes = [("#ffb3d1", "Guh Fields", "Guhvelden", ROLL, "Pink wool hills with big kaasknabbel patches.", "Roze wolheuvels met grote kaasknabbelvlakken."),
              ("#ffc29e", "Knabbel Crumbs", "Knabbelkruimels", ROLL, "Mostly kaasknabbels on top, extra veins below.", "Vooral kaasknabbels bovenop, extra aders eronder."),
              ("#f7c6e6", "Pink Puffs", "Roze pluisjes", ROLL, "Soft plain pink wool, lots of guhs.", "Zachte roze wol, veel guhs."),
              ("#8c2a5a", "Mika's Biome", "Mika's bioom", ROLL, "Rare. Darker magenta wool, a little crying obsidian at every height, a dark sky, the odd Mika.",
               "Zeldzaam. Donkerder magenta wol, wat huilende obsidiaan op elke hoogte, een donkere lucht, af en toe een Mika."),
              ("#ffd08a", "Kaas Flats", "Kaasvlakte", FLAT, "A flat plain of kaasknabbels criss-crossed with pink wool. Great for building.",
               "Een vlakte van kaasknabbels met roze wolstrepen. Heerlijk om op te bouwen."),
              ("#ffa6cc", "Guh Meadows", "Guhweides", FLAT, "Flat pink wool with pink powder flowers and the biggest guh herds.",
               "Vlakke roze wol met roze poederbloemetjes en de grootste guhkuddes."),
              ("#e8b4f0", "Guh Peaks", "Guhpieken", STEEP, "Towering pink wool mountains and spikes (up to ~y215) with white wool tops.",
               "Torenhoge roze wolbergen en pieken (tot ~y215) met witte wollen toppen."),
              ("#d08abf", "Vads Cliffs", "Vadskliffen", STEEP, "Sheer pink and magenta terracotta cliffs, rich in Vahoege Vads.",
               "Steile roze en magenta terracotta kliffen, rijk aan vahoege vads."),
              ("#ff8fc8", "Guh Sea", "Guhzee", FLAT, "Flat land full of pink water pools (1&ndash;3 deep) with guh-head lily pads and schools of guh fish.",
               "Vlak land vol roze waterpoelen (1&ndash;3 diep) met guhhoofd-waterlelies en scholen guhvissen."),
              ("#e86ab8", "Diepe Guhzee (2.7)", "Diepe Guhzee (2.7)", ("sea, 28 deep (y 34&ndash;62)", "zee, 28 diep (y 34&ndash;62)"),
               "Big deep pink seas with a sand bottom, kaaskoraal, seagrass, kelp, guh fish and Zeemeerguhs, a dam with beaches around them, and one Guhbubbel in the middle.",
               "Grote diepe roze zeeën met een zandbodem, kaaskoraal, zeegras, kelp, guhvissen en Zeemeerguhs, een dam met strandjes eromheen, en één Guhbubbel in het midden."),
              ("#e890c8", "Guh Crystal Mine", "Guhkristalmijn", ("underground, y30&ndash;60", "ondergronds, y30&ndash;60"),
               "Rare. Caves lined with crystal stone and glowing guh crystals that <b>sing</b> (a high guh) when you come close.",
               "Zeldzaam. Grotten vol kristalsteen en gloeiende guhkristallen die <b>zingen</b> (een hoge guh) als je dichtbij komt."),
              ("#c9d94a", "Kaasmoeras (2.7)", "Kaasmoeras (2.7)", FLAT, "A misty swamp with bouncy borrelende kaassaus, kikkerguhs, kaasmotten, Kaasmoerasguhs and the Moerasheks-Mika's hut.",
               "Een mistig moeras met stuiterende borrelende kaassaus, kikkerguhs, kaasmotten, Kaasmoerasguhs en de paalhut van de Moerasheks-Mika."),
              ("#9fe0c4", "Vadswoud (2.7)", "Vadswoud (2.7)", ROLL, "A mint-green forest of giant guh trees with faces in the bark, knabbelbessen, guh nests and the treehouse village.",
               "Een mintgroen woud van reuzenguhbomen met gezichtjes in de schors, knabbelbessen, guhnestjes en het boomhutdorp."),
              ("#f2c14e", "Gatenkaasgrotten (2.7)", "Gatenkaasgrotten (2.7)", ("underground, below y36", "ondergronds, onder y36"),
               "Round cheese holes, cheese stalactites, glowing kaasmos, kaaskorrelerts and the Stille Voorraadkelder.",
               "Ronde kaasgaten, kaasstalactieten, gloeiend kaasmos, kaaskorrelerts en de Stille Voorraadkelder.")]
    brows = "".join(f'<tr><td><span class="swatch" style="background:{c}"></span>{t(en, nl)}</td><td>{t(*terrain)}</td><td>{t(den, dnl)}</td></tr>'
                    for c, en, nl, terrain, den, dnl in biomes)
    guhm = entry(img("guh_portal", "Guh portal"), "The portal", "Het portaal",
                 p("Build a nether-portal-shaped frame of <b>Blocks of Kaasknabbels</b> (inside 2&times;3 up to 21&times;21, corners "
                   "optional). It lights up <b>by itself</b> as soon as the frame is complete. Stand in it for 1.5 seconds. "
                   "If there's no portal on the other side yet, one is built on the surface on a small pink wool platform.",
                   "Bouw een frame in de vorm van een netherportaal van <b>Blokken kaasknabbels</b> (binnenkant 2&times;3 tot 21&times;21, "
                   "hoeken hoeven niet). Het gaat <b>vanzelf</b> aan zodra het frame dicht is. Sta er 1,5 seconde in. "
                   "Is er aan de andere kant nog geen portaal, dan wordt er een gebouwd op het oppervlak, op een roze wolplatformpje."), wide=True) + \
        f'<h3 style="margin-top:12px">{t("Biomes", "Biomen")}</h3><div class="tscroll"><table><tr><th>{t("Biome", "Bioom")}</th><th>{t("Terrain", "Terrein")}</th><th>{t("What it looks like", "Hoe het eruitziet")}</th></tr>{brows}</table></div>' + \
        p("The ground: <b>bedrock</b> at y0, then always solid (pink wool, ores, caves) up to y30. The surface starts from y30 "
          "and its shape depends on the biome: almost flat, rolling hills, or very steep mountains and cliffs.",
          "De grond: <b>bedrock</b> op y0, daarna altijd vol (roze wol, ertsen, grotten) tot y30. Vanaf y30 begint het oppervlak "
          "en de vorm hangt af van het bioom: bijna vlak, glooiende heuvels of heel steile bergen en kliffen.") + \
        entry(img("kaas_saus", "Kaas saus"), "Kaas saus", "Kaassaus",
              p("Thick, bubbly cheese sauce (like lava, but it doesn't burn) that flows from the spouts of cheese fountains and through cave pantries. It spreads as far as water but three times "
                "slower (lava is six times slower) and is thicker to swim in. <b>Right-click it with an empty hand</b> to fill your "
                "hunger bar &mdash; the sauce isn't used up.",
                "Dikke, borrelende kaassaus (net lava, maar hij brandt niet) die uit de spuiten van kaasfonteinen en door voorraadkamers in de grotten stroomt. Hij stroomt net zo ver als water, maar drie "
                "keer zo langzaam (lava zes keer) en is dikker om in te zwemmen. <b>Rechtsklik erop met een lege hand</b> om je "
                "hongerbalk te vullen &mdash; de saus raakt niet op."))
    guhm += entry(img("pink_sky", "The Guhmension night sky"), "A pink night sky", "Een roze nachthemel",
                  p("At night the Guhmension has its own sky: a big <b>pink moon</b> and thousands of softly twinkling <b>pink and lilac "
                    "stars</b>.", "'s Nachts heeft de Guhmensie een eigen hemel: een grote <b>roze maan</b> en duizenden zacht fonkelende "
                    "<b>roze en lila sterren</b>."), wide=True) + \
        entry(img("guh_kristal_cluster", "Guh crystal"), "Guh crystal mines", "Guhkristalmijnen",
              p("Dig down in the Guhmension and now and then you break into a crystal mine: caves (between y30 and y60) lined with pink "
                "crystal stone, crystal blocks, and <b>guh crystals</b>: little see-through crystal guh heads growing from floor, walls and ceiling. "
                "They glow, sparkle, and now and then one gives a soft happy \"guh\" when you're near. Mine them for <b>guh crystals</b> (Fortune works; Silk Touch gives the cluster).",
                "Graaf in de Guhmensie naar beneden en af en toe breek je een kristalmijn binnen: grotten (tussen y30 en y60) vol roze "
                "kristalsteen, kristalblokken en <b>guhkristallen</b>: kleine doorzichtige kristallen guhhoofdjes die uit vloer, muren en plafond groeien. "
                "Ze gloeien, glinsteren en af en toe geeft er een een zacht blij \"guh\" als je in de buurt bent. Hak ze voor <b>guhkristallen</b> (Geluk werkt; Zijden aanraking geeft de cluster)."), wide=True) + \
        entry(img("guhbloesem_tree", "Guh blossom tree"), "Guh blossom trees", "Guhbloesembomen",
              p("Here and there in the Guhmension grows a <b>guh blossom</b> tree (Guhbloesem): lilac bark and soft pink blossom from "
                "which <b>tiny guhs</b> drift down, like cherry petals. Its blossom now and then drops a sapling (Guhbloesemboompje) that "
                "grows on grass, dirt and pink wool; the logs make <b>Vadsplanken</b>. They also grow around the sled hut.",
                "Hier en daar in de Guhmensie groeit een <b>guhbloesemboom</b>: lila schors en zachtroze bloesem waar <b>piepkleine "
                "guhtjes</b> uit naar beneden dwarrelen, net als kersenbloesem. De bloesem laat soms een Guhbloesemboompje vallen dat groeit "
                "op gras, aarde en roze wol; de stammen worden <b>Vadsplanken</b>. Ze staan ook rond de sleehut."), wide=True) +         entry(img("roze_gras", "Pink grass"), "Pink grass and flowers", "Roze gras en bloemen",
              p("The Guhmension is greener now &mdash; well, pinker: <b>pink grass</b> (sometimes drops kaasknabbel seeds) and the four guh "
                "flowers grow everywhere.", "De Guhmensie is nu groener &mdash; nou ja, rozer: <b>roze gras</b> (geeft soms kaasknabbelzaadjes) "
                "en de vier guhbloemen groeien overal."))
    S.append(section("guhmension", "The Guhmension", "De Guhmensie", guhm))

    # --- guh villages ---------------------------------------------------------------------------------------------------
    JOBS = [
        ("vads_temmer", "knabbelbak", "Vads temmer", "Vadstemmer", "Knabbelbak", "Knabbelbak",
         "Guh spawn eggs, saddles, iron / diamond / netherite guh armour, fried knabbels. Buys kaas knabbels.",
         "Guh-spawneieren, zadels, ijzeren / diamanten / netherieten guhpantser, gefrituurde knabbels. Koopt kaasknabbels."),
        ("guh_kleermaker", "naaitafel", "Guh kleermaker", "Guhkleermaker", "Guh sewing table", "Guhnaaitafel",
         "Everyday and work clothes (glasses, hats, uniforms...), the pink ribbon, and for masters the onesie, monocle and guh backpack. Buys wool and string.",
         "Gewone en werkkleding (brillen, hoedjes, uniformen...), het roze lint, en bij meesters de onesie, het monocle en de guhrugzak. Koopt wol en touw."),
        ("vadssmid", "vadsaambeeld", "Vadssmid", "Vadssmid", "Vads anvil", "Vadsaambeeld",
         "Vahoege vads ingots, vads tools, armour and even the paxel (pricey!). Buys vahoege vads.",
         "Vadsstaven, vadsgereedschap, -pantser en zelfs de paxel (duur!). Koopt vahoege vads."),
        ("hamsterbouwer", "buizenbank", "Hamsterbouwer", "Hamsterbouwer", "Tube workbench", "Buizenwerkbank",
         "Tube glass, guh wire, guh wheels, frying pans and (for masters) a guh spawner. Buys yellow dye and redstone.",
         "Buisglas, guhdraad, guhraderen, koekenpannen en (bij meesters) een guhspawner. Koopt gele kleurstof en redstone."),
        ("knabbelboer", "zaadbak", "Knabbelboer", "Knabbelboer", "Seed tray", "Zaadbak",
         "Kaasknabbel seeds, cupcakes, macarons, milkshakes, fondue, a knabbelkorf, kaashoning and (for masters) a guh cake. Buys kaas knabbels and sugar.",
         "Kaasknabbelzaadjes, cupcakes, macarons, milkshakes, fondue, een knabbelkorf, kaashoning en (bij meesters) een guh-taart. Koopt kaasknabbels en suiker."),
        ("mika_jager", "mikatrofee", "Mika-jager", "Mika-jager", "Mika trophy", "Mikatrofee",
         "The Mika-mepper, shields, arrows, the Guhmension super compass, golden apples, a totem. Buys Mika's vet.",
         "De Mika-mepper, schilden, pijlen, uitdagings- en guhgrotkompassen, gouden appels, een totem. Koopt Mika's vet."),
    ]
    jobs = "".join(f'''<div class="card"><figure class="stage">{img("villager_" + prof, en)}</figure>
<h3>{t(en, nl)}</h3><p>{icon(block, ben)} <b>{t(ben, bnl)}</b></p><p>{t(den, dnl)}</p></div>''' for prof, block, en, nl, ben, bnl, den, dnl in JOBS)
    villages = entry(img("structure_layout_a", "Guh village"), "Guh villages", "Guhdorpen",
                     p("In the flat and rolling Guhmension biomes you'll find guh villages: hamster houses with cheese roofs around a "
                       "plaza with a kaas saus fountain and a bell. They work just like normal villages (trading, beds, babies, "
                       "iron golems), but the villagers are <b>guh villagers</b>: pink fur, round ears, big glossy guh eyes and a "
                       "little tail. Villagers born in the Guhmension are always guh villagers.",
                       "In de vlakke en glooiende biomen van de Guhmensie vind je guhdorpen: hamsterhuisjes met kaasdaken rond een "
                       "pleintje met een kaassausfontein en een bel. Ze werken net als gewone dorpen (handelen, bedden, baby's, "
                       "ijzergolems), maar de dorpelingen zijn <b>guhdorpelingen</b>: roze vacht, ronde oortjes, grote glanzende "
                       "guhogen en een staartje. Dorpelingen die in de Guhmensie geboren worden, zijn altijd guhdorpelingen."), wide=True) + \
        p("Guh villagers only do guh jobs: they take their profession from these six job site blocks (they ignore lecterns, "
          "composters and the like). The <b>knabbelboer</b> (the guh farmer) has a field of <b>kaasknabbel plants</b> next to his house: "
          "plant kaasknabbel seeds on farmland and harvest 2&ndash;4 kaas knabbels when they're ripe.",
          "Guhdorpelingen doen alleen guhbanen: ze krijgen hun beroep van deze zes werkplekken (lessenaars, composters en "
          "dergelijke negeren ze). De <b>knabbelboer</b> (de guhboer) heeft een veldje <b>kaasknabbelplanten</b> naast zijn huis: "
          "plant kaasknabbelzaadjes op akkergrond en oogst 2&ndash;4 kaasknabbels als ze rijp zijn.") + \
        f'<div class="cards">{jobs}</div>' + \
        entry(icon("mika_mepper", "Mika-mepper").replace('class="px"', 'class="px" style="width:96px;height:96px"'), "Mika-mepper", "Mika-mepper",
              p("The Mika-jager's swatter. An iron-level sword that <b>never breaks</b> and hits Mikas (Nether Mikas and Big Mika too) "
                "<b>five times as hard</b>.",
                "De vliegenmepper van de Mika-jager. Een zwaard van ijzerniveau dat <b>nooit kapotgaat</b> en Mika's (ook Nether-Mika's en "
                "Grote Mika) <b>vijf keer zo hard</b> raakt."))
    S.append(section("villages", "Guh villages", "Guhdorpen", villages))

    # --- structures ---------------------------------------------------------------------------------------------------
    def fig(name, en, nl, rar_en, rar_nl, den, dnl):
        return f'''<figure><div class="stage">{img(name, en)}</div><figcaption><h3>{t(en, nl)}<span class="rarity">{t(rar_en, rar_nl)}</span></h3>
<p>{t(den, dnl)}</p></figcaption></figure>'''
    gal = '<div class="gallery">' + "".join([
        fig("structure_hamster_house", "Hamster house (small)", "Hamsterhuis (klein)", "rare", "zeldzaam",
            "A Littlest Pet Shop style playset: flower wheel, cheese wedge house with a guh spawner and a chest, twisty tube, pink ball, apple house &mdash; and a giant guh.",
            "Een speelset in Littlest Pet Shop-stijl: bloemenwiel, kaaspunthuisje met guhspawner en kist, kronkelbuis, roze bal, appelhuisje &mdash; en een reuzenguh."),
        fig("structure_hamster_house_medium", "Hamster house (medium)", "Hamsterhuis (middel)", "rare", "zeldzaam",
            "Two wheels, two glass balls, a bigger cheese house and a 7.5x guh.",
            "Twee wielen, twee glazen ballen, een groter kaashuisje en een 7,5x guh."),
        fig("structure_hamster_house_large", "Hamster house (large)", "Hamsterhuis (groot)", "very rare", "heel zeldzaam",
            "A whole hamster playground city: a tower with a ball on top, three wheels, tube network, two spawners and a mega guh of ~14 blocks.",
            "Een hele hamsterspeeltuinstad: een toren met een bal erop, drie wielen, buizennetwerk, twee spawners en een megaguh van ~14 blokken."),
        fig("structure_evil_mika_home", "Evil Mika home", "Evil Mika-huis", "rare, Mika's biome", "zeldzaam, Mika's bioom",
            "The dark version: black and crying obsidian, a broken wheel, Mikas inside and a chest with Mika's vet.",
            "De donkere versie: zwart en huilende obsidiaan, een kapot wiel, Mika's binnen en een kist met Mika's vet."),
        fig("structure_guh_picnic", "Hungry Guh picnic", "Picknick van de hongerige guh", "semi-rare, also overworld", "vrij zeldzaam, ook overworld",
            "A checkered blanket with a parasol, cake, an empty frying pan (hint!) and the Hungry Guh with its quest.",
            "Een geblokt kleed met parasol, taart, een lege koekenpan (hint!) en de Hongerige Guh met zijn opdracht."),
        fig("structure_hamster_house_extra_extra_large", "Guhland (extra extra large hamster house)", "Guhland (extra extra groot hamsterhuis)",
            "very very rare", "heel heel zeldzaam",
            "A whole guh theme park of 128&times;128 blocks: a giant ferris wheel with guhs in the gondolas, a carousel, a guh sled "
            "coaster, snack stands (frituur!), a flowing fountain plaza, guh portrait billboards, a block guh mascot, a cheese castle, "
            "running wheels and a mega guh.",
            "Een heel guhpretpark van 128&times;128 blokken: een reuzenrad met guhs in de gondels, een draaimolen, een "
            "guh-sleebaan, snackkraampjes (frituur!), een plein met stromende fontein, reclameborden met guhportretten, "
            "een blokkenguh als mascotte, een kaaskasteel, loopwielen en een megaguh."),
        fig("structure_vadsig_heiligdom", "Vadsig shrine", "Vadsig-heiligdom", "very very rare", "heel heel zeldzaam",
            "A round pink temple on quartz pillars under a glass dome. Mother Vadsig sits on her huge cushion in the middle. The start of the guh stomach quest.",
            "Een rond roze tempeltje op kwartszuilen onder een glazen koepel. Moeder Vadsig zit in het midden op haar enorme kussen. Het begin van de guhmaag-opdracht."),
        fig("structure_mika_kamp", "Mika camp", "Mika-kamp", "uncommon", "niet vaak",
            "Black tents, soul lanterns, a soul campfire, Mikas &mdash; and a caged baby guh. The Mika boss plays rock-paper-scissors-VADS.",
            "Zwarte tentjes, zielenlantaarns, een zielenkampvuur, Mika's &mdash; en een babyguh in een kooi. De Mika-baas speelt steen-papier-schaar-VADS."),
        fig("structure_sleehut", "Sled hut", "Sleehut", "Guh Peaks", "Guhpieken",
            "A snowy log cabin with a campfire and a chest. The Sled Guh sits outside next to her broken sled.",
            "Een besneeuwd blokhutje met een kampvuur en een kist. De Slee-guh zit buiten naast haar kapotte slee."),
        fig("structure_guh_statue", "Guh statue", "Guhstandbeeld", "quite rare", "vrij zeldzaam",
            "The guh model, built out of blocks (about 35 long and 24 high). That's it. Just admire it.",
            "Het guhmodel, gebouwd van blokken (zo'n 35 lang en 24 hoog). Meer niet. Gewoon bewonderen."),
        fig("structure_guhramid", "Guhramid", "Guhramide", "very rare", "heel zeldzaam",
            "A big stepped pink pyramid. Inside: a treasure hall with four chests and a Mummy Guh on a golden dais (mind the "
            "pressure plate!), kaas saus pools along the entrance, and, if you look carefully behind a portrait, a secret room "
            "with a Golden Guh and the best loot.",
            "Een grote getrapte roze piramide. Binnen: een schatkamer met vier kisten en een Mummieguh op een gouden verhoging (pas "
            "op voor de drukplaat!), kaassausbadjes langs de ingang en, als je goed kijkt achter een portret, een geheime kamer met "
            "een Gouden Guh en de beste buit."),
        fig("structure_giant_cake", "Giant cake", "Reuzentaart", "rare", "zeldzaam",
            "A cake as big as a house, with frosting and strawberries.", "Een taart zo groot als een huis, met glazuur en aardbeien."),
        fig("structure_giant_kaasknabbel", "Giant kaasknabbel", "Reuzenkaasknabbel", "rare", "zeldzaam",
            "A giant kaasknabbel with crumbs around it, and a sign: <i>\"Ik had zn honger - Guh\"</i>.",
            "Een reuzenkaasknabbel met kruimels eromheen, en een bordje: <i>\"Ik had zn honger - Guh\"</i>."),
        fig("structure_kaasknabbel_arch", "Kaasknabbel arch", "Kaasknabbelboog", "rare", "zeldzaam",
            "A curled kaasknabbel so big you can walk under it.", "Een gekrulde kaasknabbel zo groot dat je eronder door kunt lopen."),
        fig("structure_block_guh", "Block guh", "Blokguh", "common", "vaak",
            "A big pink wool guh cube.", "Een grote roze wollen guhkubus."),
        fig("structure_guh_fossil", "Guh fossil", "Guhfossiel", "common", "vaak",
            "The bones of a long-gone guh. Also in Mika's biome and the steep biomes.",
            "De botjes van een guh van lang geleden. Ook in Mika's bioom en de steile biomen."),
        fig("structure_mini_picnic", "Mini picnic", "Minipicknick", "common", "vaak",
            "A little checkered blanket with cake, a lantern and flowers.", "Een klein geblokt kleedje met taart, een lantaarn en bloemen."),
        fig("structure_quartz_statue", "Quartz statue", "Kwartsbeeldje", "common", "vaak",
            "A small white quartz statue.", "Een klein wit kwartsbeeldje."),
        fig("structure_cheese_fountain", "Cheese fountain", "Kaasfontein", "semi-rare, also overworld", "vrij zeldzaam, ook overworld",
            "A spout on top of a Block of Kaasknabbels column: the kaas saus really flows, spilling over two bowls into the basin. Have a sip!",
            "Een spuit boven op een zuil van Blokken kaasknabbels: de kaassaus stroomt echt, over twee schalen het bassin in. Neem een slokje!"),
        fig("structure_grand_cheese_fountain", "Grand cheese fountain", "Grote kaasfontein", "rare, also overworld", "zeldzaam, ook overworld",
            "The big one: a vads orb on top, three cascading bowls, four spouting pillars in a deep kaas saus pool and a picnic chest.",
            "De grote: een vadsbol bovenop, drie schalen waar de saus overheen klatert, vier spuitende zuilen in een diep kaassausbad en een picknickkist."),
        fig("structure_central_room", "Guh caves: central room", "Guhgrotten: middenkamer", "common, deep underground (y14)", "vaak, diep onder de grond (y14)",
            "Underground networks of yellow hamster tubes around a big hamster room: bedding, hay, a guh wheel, wool guh portraits, a chest and a frying pan.",
            "Ondergrondse netwerken van gele hamsterbuizen rond een grote hamsterkamer: bodembedekking, hooi, een guhrad, wollen guhportretten, een kist en een koekenpan."),
        fig("structure_nest_room", "Guh caves: nest", "Guhgrotten: nestje", "underground", "ondergronds",
            "Side rooms along the tubes: nests with sleepy guhs, pantries with a kaas saus pool, and portrait rooms.",
            "Zijkamers langs de buizen: nestjes met slaperige guhs, voorraadkamers met een kaassausbadje en portretkamers."),
        fig("structure_dungeon_hall", "Challenging guh caves: dungeon hall", "Uitdagende guhgrotten: kerkerhal",
            "uncommon, very deep (y5)", "niet vaak, heel diep (y5)",
            "The hostile version of the guh caves: black and red glass tubes leading to a dark hall with Mika spawners, a ring of magma "
            "&mdash; and Big Mika guarding two treasure chests (vads, diamonds, compasses, enchanted books).",
            "De vijandige versie van de guhgrotten: zwarte en rode glazen buizen naar een donkere hal met Mika-spawners, een ring van "
            "magma &mdash; en Grote Mika die twee schatkisten bewaakt (vads, diamanten, kompassen, betoverde boeken)."),
        fig("structure_parkour_room", "Challenging guh caves: parkour room", "Uitdagende guhgrotten: parkourkamer",
            "in challenging caves", "in uitdagende grotten",
            "Jump across kaasknabbel pillars over a magma floor to reach the chest. Fall in and climb out by the ladders.",
            "Spring over kaasknabbelzuilen boven een magmavloer naar de kist. Val je erin, klim dan via de ladders eruit."),
        fig("structure_spawner_room", "Challenging guh caves: spawner room", "Uitdagende guhgrotten: spawnerkamer",
            "in challenging caves", "in uitdagende grotten",
            "A zombie and a skeleton spawner, cobwebs and a loot chest. There's also a Mika den with its own Mika spawner.",
            "Een zombie- en een skeletspawner, spinnenwebben en een buitkist. Er is ook een Mika-hol met een eigen Mika-spawner."),
        fig("structure_barbecueput_groot", "Barbecueput (2.7)", "Barbecueput (2.7)", "rare, also in the Barbecuether", "zeldzaam, ook in de Barbecuether",
            "A broken barbecue with half a grillkool frame. The big one is home to the Grillguh.", "Een kapotte barbecue met een half grillkoolframe. In de grote woont de Grillguh."),
        fig("structure_spiesburcht", "Spiesburcht (2.7)", "Spiesburcht (2.7)", "Barbecuether", "Barbecuether",
            "The fortress of the Barbecuether, with long bridges, a pindasaus garden and a Vonk-Mika spawner.", "Het fort van de Barbecuether, met lange bruggen, een pindasaus-tuintje en een Vonk-Mika-spawner."),
        fig("structure_mika_grillpaleis", "Mika-grillpaleis (2.7)", "Mika-grillpaleis (2.7)", "Barbecuether", "Barbecuether",
            "A roosterijzer bastion in the sauce sea with a mountain of stolen kaasknabbels.", "Een bastion van roosterijzer in de sauszee met een berg gestolen kaasknabbels."),
        fig("structure_stille_voorraadkelder", "Stille Voorraadkelder (2.7)", "Stille Voorraadkelder (2.7)", "rare, Gatenkaasgrotten", "zeldzaam, Gatenkaasgrotten",
            "The Mikas' secret pantry, guarded by knabbelsensors and the Vadswaker. Sssst!", "De geheime voorraadkelder van de Mika's, bewaakt door knabbelsensoren en de Vadswaker. Sssst!"),
        fig("structure_gatenkaas_mijnschacht", "Kaasmijnschacht (2.7)", "Kaasmijnschacht (2.7)", "common, Gatenkaasgrotten", "vaak, Gatenkaasgrotten",
            "An abandoned mine shaft with a chest minecart. Schacht 7: dicht!", "Een verlaten mijnschacht met een kistkarretje. Schacht 7: dicht!"),
        fig("structure_moerasheks_hut", "Paalhut van de Moerasheks (2.7)", "Paalhut van de Moerasheks (2.7)", "Kaasmoeras", "Kaasmoeras",
            "A hut on stilts shaped like the witch's own head, with a giant hat.", "Een paalhut in de vorm van het hoofd van de heks zelf, met een reuzenhoed."),
        fig("structure_boomhutdorp", "Boomhutdorp (2.7)", "Boomhutdorp (2.7)", "Vadswoud", "Vadswoud",
            "Treehouses shaped like guh heads, rope bridges, the Boswachterguh and the Knabbelplukker.", "Boomhutten in de vorm van guhhoofden, touwbruggen, de Boswachterguh en de Knabbelplukker."),
        fig("structure_sjoelhuisje", "Sjoelhuisje (2.9)", "Sjoelhuisje (2.9)", "Guhweides", "Guhweides",
            "A long house with a giant sjoelbak as its roof. Sjoelen with Opoe Njegschuif.", "Een lang huis met een reuzensjoelbak als dak. Sjoelen bij Opoe Njegschuif."),
        fig("structure_guhdoolhof", "Guhdoolhof (2.9)", "Guhdoolhof (2.9)", "Guhvelden", "Guhvelden",
            "A hedge maze with a guh-shaped lookout tower: a new maze every time.", "Een heggendoolhof met een uitkijktoren in guhvorm: elke keer een nieuw doolhof."),
        fig("structure_knabbelkatapult", "Knabbelkatapult (2.9)", "Knabbelkatapult (2.9)", "Vadskliffen", "Vadskliffen",
            "A pink guh castle with a catapult, opposite a crooked Mika fort.", "Een roze guhkasteel met een katapult, tegenover een scheef Mika-fort."),
        fig("structure_knabbelspelen", "Knabbelspelen (2.9)", "Knabbelspelen (2.9)", "Guhweides, Roze pluisjes", "Guhweides, Roze pluisjes",
            "A circus tent with guh-ear tops and six play fields for the zeskamp.", "Een circustent met guhoren als tentpunten en zes speelvelden voor de zeskamp."),
        fig("structure_elfguhjestocht", "Elf-Guhjestocht (2.9)", "Elf-Guhjestocht (2.9)", "Guhpolder", "Guhpolder",
            "A frozen canal past eleven villages: the biggest guh building of all.", "Een bevroren kanaal langs elf dorpjes: het grootste guhbouwwerk van allemaal."),
        fig("structure_guh_circuit", "Guh-Circuit (2.9)", "Guh-Circuit (2.9)", "rare, Guhvelden, Kaasvlakte", "zeldzaam, Guhvelden, Kaasvlakte",
            "The Pitpaleis and three tracks: Regenboogbaan, Vadsbaan and Kaasbergbaan.", "Het Pitpaleis en drie banen: Regenboogbaan, Vadsbaan en Kaasbergbaan."),
        fig("structure_beroepenstraat", "Beroepenstraat (2.9)", "Beroepenstraat (2.9)", "Knuffeldal town", "Knuffeldal-stadje",
            "Brandweerkazerne, Politiebureautje and Apotheekje.", "Brandweerkazerne, Politiebureautje en Apotheekje."),
        fig("structure_guhdorp_bouwplaats", "Bob's bouwplaats (2.9)", "Bouwplaats van Bob (2.9)", "1 in 3 guh villages", "1 op 3 guhdorpen",
            "A guh village with a half-built house and Bob de Guhbouwer.", "Een guhdorp met een half gebouwd huisje en Bob de Guhbouwer."),
    ]) + "</div>"
    S.append(section("structures", "Structures", "Bouwwerken", gal))

    # --- recipes ------------------------------------------------------------------------------------------------------
    K, I, S_, R, D, B = "kaas_knabbels", "iron_ingot", "stick", "redstone", "diamond", "block_of_kaasknabbels"
    recipes = '<div class="recipes">' + "".join([
        rcard("Block of Kaasknabbels", "Blok kaasknabbels", grid([K] * 9, B)),
        rcard("Kaas Knabbels from a block", "Kaasknabbels uit een blok", grid([B] + [None] * 8, K, 9, shapeless=True)),
        rcard("Guh Frying Pan", "Guh-koekenpan", grid([I, I, None, I, I, S_, None, None, None], "frying_pan")),
        rcard("Guh Wheel", "Guhrad", grid(["pink_concrete"] * 3 + ["pink_concrete", K, "pink_concrete"] + [I, R, I], "guh_wheel")),
        rcard("Guh Wire", "Guhdraad", grid([R, "pink_dye", K] + [None] * 6, "guh_wire", 3, shapeless=True)),
        rcard("Iron Guh Armour", "IJzeren guhpantser", grid([I, None, I, I, K, I, I, I, I], "iron_guh_armor")),
        rcard("Diamond Guh Armour", "Diamanten guhpantser", grid([D, None, D, D, K, D, D, D, D], "diamond_guh_armor")),
        rcard("Netherite Guh Armour (smithing table)", "Netherieten guhpantser (smeedtafel)",
              f'<div class="recipe"><span class="slot">{icon("netherite_upgrade_smithing_template", "template")}</span>'
              f'<span class="slot">{icon("diamond_guh_armor", "diamond guh armour")}</span><span class="slot">{icon("netherite_ingot", "netherite ingot")}</span>'
              f'<span class="arrow" aria-hidden="true"></span><span class="slot big">{icon("netherite_guh_armor", "netherite guh armour")}</span></div>'),
    ]) + "</div>"
    V = "vahoege_vads_ingot"
    vads_recipes = '<div class="recipes">' + "".join([
        rcard("Vads Ingot (furnace or blast furnace)", "Vadsstaaf (oven of hoogoven)",
              f'<div class="recipe"><span class="slot">{icon("vahoege_vads", "vahoege vads")}</span><span class="arrow" aria-hidden="true"></span>'
              f'<span class="slot big">{icon(V, "vads ingot")}</span></div>'),
        rcard("Vads Sword", "Vadszwaard", grid([None, V, None, None, V, None, None, S_, None], "vahoege_vads_sword")),
        rcard("Vads Pickaxe", "Vadshouweel", grid([V, V, V, None, S_, None, None, S_, None], "vahoege_vads_pickaxe")),
        rcard("Vads Axe", "Vadsbijl", grid([V, V, None, V, S_, None, None, S_, None], "vahoege_vads_axe")),
        rcard("Vads Shovel", "Vadsschep", grid([None, V, None, None, S_, None, None, S_, None], "vahoege_vads_shovel")),
        rcard("Vads Hoe", "Vadsschoffel", grid([V, V, None, None, S_, None, None, S_, None], "vahoege_vads_hoe")),
        rcard("Vads Paxel", "Vadspaxel", grid(["vahoege_vads_pickaxe", "vahoege_vads_axe", "vahoege_vads_shovel"] + [None] * 6,
                                              "vahoege_vads_paxel", shapeless=True)),
        rcard("Vads Helmet", "Vadshelm", grid([V, V, V, V, None, V, None, None, None], "vahoege_vads_helmet")),
        rcard("Vads Chestplate", "Vadsborstplaat", grid([V, None, V, V, V, V, V, V, V], "vahoege_vads_chestplate")),
        rcard("Vads Leggings", "Vadsbeenbescherming", grid([V, V, V, V, None, V, V, None, V], "vahoege_vads_leggings")),
        rcard("Vads Boots", "Vadslaarzen", grid([V, None, V, V, None, V, None, None, None], "vahoege_vads_boots")),
    ]) + "</div>"
    recipes += f'<h3 style="margin-top:18px">{t("Vahoege Vads", "Vahoege vads")}</h3>' + vads_recipes
    W_, S2, G_, P_, T_, B_ = "pink_wool", "string", "yellow_stained_glass", "oak_planks", "pink_terracotta", "polished_blackstone"
    job_recipes = '<div class="recipes">' + "".join([
        rcard("Knabbelbak (vads temmer)", "Knabbelbak (vadstemmer)", grid([K, K, K, W_, "composter_side", W_, W_, W_, W_], "knabbelbak")),
        rcard("Guh sewing table (kleermaker)", "Guhnaaitafel (kleermaker)", grid([S2, S2, None, W_, W_, None, None, None, None], "naaitafel")),
        rcard("Vads anvil (vadssmid)", "Vadsaambeeld (vadssmid)", grid(["vahoege_vads", "vahoege_vads", None, T_, T_, None, None, None, None], "vadsaambeeld")),
        rcard("Tube workbench (hamsterbouwer)", "Buizenwerkbank (hamsterbouwer)", grid([G_, G_, None, P_, P_, None, None, None, None], "buizenbank")),
        rcard("Mika trophy (Mika-jager)", "Mikatrofee (Mika-jager)", grid(["mika_vet", None, None, B_, None, None, None, None, None], "mikatrofee")),
    ]) + "</div>"
    recipes += f'<h3 style="margin-top:18px">{t("Guh village job sites", "Werkplekken voor guhdorpen")}</h3>' + job_recipes
    BK, IR, PW, ST = "sleebouwersboek", "iron_ingot", "pink_wool", "stick"
    KR, SU, EG, MB = "guh_kristal", "sugar", "egg", "milk_bucket"
    new_recipes = '<div class="recipes">' + "".join([
        rcard("Sled rail, straight (&times;4)", "Sleerail, recht (&times;4)", grid([IR, None, IR, PW, BK, PW, ST, None, ST], "sleerail_recht", 4)),
        rcard("Sled rail, curve (&times;2)", "Sleerail, bocht (&times;2)", grid([IR, IR, None, PW, BK, PW, None, ST, ST], "sleerail_bocht", 2)),
        rcard("Sled rail, slope (&times;2)", "Sleerail, helling (&times;2)", grid([None, None, IR, None, BK, PW, IR, ST, ST], "sleerail_helling", 2)),
        rcard("Guh sled", "Guhslee", grid([None, None, IR, PW, PW, PW, IR, BK, IR], "guh_slee")),
        rcard("Guhdex", "Guhdex", grid(["book", K, "pink_dye"] + [None] * 6, "guhdex", shapeless=True)),
        rcard("Guh balloon (&times;2)", "Guh-ballon (&times;2)", grid([None, PW, None, None, PW, None, None, "string", None], "guh_ballon", 2)),
        rcard("Guh crystal spyglass", "Guh-kristalverrekijker", grid([KR, None, None, "spyglass", None, None, None, None, None], "guh_kristal_verrekijker")),
        rcard("Block of guh crystal", "Guhkristalblok", grid([KR, KR, None, KR, KR, None, None, None, None], "guh_kristal_blok")),
        rcard("Guh crystal lamp (&times;2)", "Guhkristallamp (&times;2)", grid(["iron_nugget", "glass", "iron_nugget", "glass", KR, "glass", "iron_nugget", "glass", "iron_nugget"], "guh_kristal_lamp", 2)),
        rcard("Knabbelkorf", "Knabbelkorf", grid(["cherry_planks"] * 3 + [K] * 3 + ["cherry_planks"] * 3, "knabbelkorf_honey")),
        rcard("Pink slime block", "Roze guhslijmblok", grid(["guh_slimeball"] * 9, "roze_slijmblok")),
        rcard("Guh cake", "Guhtaart", grid([MB, MB, MB, SU, K, SU, "wheat", "pink_dye", "wheat"], "guh_taart_item")),
        rcard("Guh cupcake (&times;3)", "Guh-cupcake (&times;3)", grid(["wheat", SU, EG, "pink_dye"] + [None] * 5, "guh_cupcake", 3, shapeless=True)),
        rcard("Macaron (&times;4)", "Macaron (&times;4)", grid([SU, EG, "pink_dye"] + [None] * 6, "macaron_roze", 4, shapeless=True)),
        rcard("Cheese fondue", "Kaasfondguh", grid(["bowl", K, K, K, MB] + [None] * 4, "kaasfondue", shapeless=True)),
        rcard("Kaasknabbel milkshake (&times;3)", "Kaasknabbel-milkshake (&times;3)", grid([MB, K, SU, "glass_bottle", "glass_bottle", "glass_bottle"] + [None] * 3, "kaasknabbel_milkshake", 3, shapeless=True)),
        rcard("Guh chair (&times;2)", "Guh-stoel (&times;2)", grid(["cherry_planks", None, None, "cherry_planks", PW, "cherry_planks", ST, None, ST], "guh_stoel", 2)),
        rcard("Guh table", "Guhtafel", grid(["cherry_planks"] * 3 + [ST, None, ST, ST, None, ST], "guh_tafel")),
        rcard("Guh sofa", "Vadszetel", grid([None, None, PW, PW, PW, PW, "cherry_planks", "cherry_planks", "cherry_planks"], "guh_bank")),
        rcard("Guh cupboard", "Vadskast", grid(["cherry_planks"] * 4 + ["chest"] + ["cherry_planks"] * 4, "guh_kast")),
        rcard("Bean bag (any wool colour)", "Zitzak (elke wolkleur)", grid([PW, None, PW, PW, PW, PW, None, None, None], "pink_zitzak")),
        rcard("Cushion (&times;2)", "Kussen (&times;2)", grid([PW, "feather", PW] + [None] * 6, "pink_kussen", 2)),
        rcard("Lampgion (&times;2)", "Lampgion (&times;2)", grid(["paper", "paper", "torch", "string", "pink_dye"] + [None] * 4, "lampion_roze", 2, shapeless=True)),
        rcard("Bunting (&times;6)", "Vlaggetjes (&times;6)", grid(["string", "string", "string", PW, "white_wool", "yellow_wool", None, None, None], "vlaggetjes", 6)),
        rcard("Seed tray (knabbelboer)", "Zaadbak (knabbelboer)", grid(["kaasknabbelzaadjes"] * 3 + [PW, "composter_side", PW, None, None, None], "zaadbak")),
    ]) + "</div>"
    recipes += f'<h3 style="margin-top:18px">{t("New in 2.0", "Nieuw in 2.0")}</h3>' + new_recipes
    S.append(section("recipes", "Recipes", "Recepten", recipes))

    # --- advancements & commands ---------------------------------------------------------------------------------------
    adv = f"""<div class="tscroll"><table><tr><th>{t("Advancement", "Vooruitgang")}</th><th>{t("How", "Hoe")}</th></tr>
<tr><td><b>Guhmension</b> ({t("tab", "tabblad")})</td><td>{t("Appears when you get a Block of Kaasknabbels, enter the Guhmension or ride a guh.", "Verschijnt als je een Blok kaasknabbels krijgt, de Guhmensie in gaat of op een guh rijdt.")}</td></tr>
<tr><td><b>Welcome to vads dimension!</b></td><td>{t("Enter the Guhmension for the first time.", "Ga voor het eerst de Guhmensie in.")}</td></tr>
<tr><td><b>{t("Giddy-up, Guh!", "Hop hop, Guh!")}</b></td><td>{t("Ride a saddled guh.", "Rijd op een guh met zadel.")}</td></tr>
{"".join(f'<tr><td><b>{t(en_t, nl_t)}</b>{rare}</td><td>{t(en_d, nl_d)}</td></tr>' for en_t, nl_t, en_d, nl_d, rare in [
    ("Home Sweet Hamster Home", "Huisje, boompje, hamster", "Find a hamster house.", "Vind een hamsterhuis.", ""),
    ("Welcome to Guhland!", "Welkom in Guhland!", "Find the very rare guh theme park.", "Vind het zeer zeldzame guhpretpark.", " &#9733;"),
    ("Won't You Be My Neighbour?", "Buurguhs!", "Find a guh village.", "Vind een guhdorp.", ""),
    ("Walk Like a Guhgyptian", "Loop als een Guhgypter", "Find the very rare guhramid.", "Vind de zeer zeldzame guhramide.", " &#9733;"),
    ("Set in Stone", "In steen gebeiteld", "Find the giant guh statue.", "Vind het reuzenguhstandbeeld.", ""),
    ("Say Cheese!", "Zeg eens kaas!", "Find a cheese fountain.", "Vind een kaasfontein.", ""),
    ("The Grandest Saus", "De allergrootste saus", "Find the rare grand cheese fountain.", "Vind de zeldzame grote kaasfontein.", " &#9733;"),
    ("Mika's Lair", "Het hol van Mika", "Find an Evil Mika home.", "Vind een huis van Boze Mika.", ""),
    ("Down the Hamster Tube", "Door de hamsterbuis", "Find the guh caves.", "Vind de guhgrotten.", ""),
    ("Something Smells Evil", "Hier ruikt iets kwaads", "Find a challenging guh cave.", "Vind een uitdagende guhgrot.", ""),
    ("Big Mika, Bigger Fall", "Hoe groter de Mika...", "Defeat Big Mika.", "Versla Grote Mika.", " &#9733;"),
    ("Vadsig Fed", "Vadsig gevoerd", "Give a Hungry Guh 10 gefrituurde kaasknabbels.", "Geef een Hongerige Guh 10 gefrituurde kaasknabbels.", ""),
    ("Super Vahoege!", "Super vahoege!", "Smelt a Vahoege Vads ingot.", "Smelt een vahoege-vadsstaaf.", "")])}</table></div>
<p class="note">{t("&#9733; = challenge advancement (purple frame).", "&#9733; = uitdaging (paarse rand).")}</p>
<h3 style="margin-top:16px">{t("Handy commands (creative / cheats on)", "Handige commando's (creatief / cheats aan)")}</h3>
{cmd("/execute in guhs:guhmension run tp @s 0 150 0")}
{cmd("/locate structure guhs:hamster_house_large")}
{cmd("/execute in guhs:guhmension run locate structure guhs:challenging_guh_caves")}
{cmd("/execute in guhs:guhmension run locate structure guhs:hamster_house_extra_extra_large")}
{cmd("/give @s guhs:vahoege_vads_paxel")}
{cmd('/summon guhs:guh ~ ~ ~ {Variant:"brontosaurus",Personality:"playful",ClothesHead:"party_hat"}')}
{cmd("/execute in guhs:guhmension run locate structure guhs:guhramid")}
{cmd("/execute in guhs:guhmension run locate structure guhs:guh_village")}
{cmd("/locate biome guhs:mikas_biome")}
{cmd("/summon guhs:mika ~ ~ ~")}
{cmd("/give @s guhs:block_of_kaasknabbels 14")}
{cmd("/execute in guhs:guhmension run locate structure guhs:vadsig_heiligdom")}
{cmd("/execute in guhs:guhmension run locate structure guhs:sleehut")}
{cmd("/execute in guhs:guhmension run locate biome guhs:guh_kristalmijn")}
{cmd("/give @s guhs:guh_slee")}"""
    S.append(section("more", "Advancements and commands", "Vooruitgangen en commando's", adv))

    toc = "".join(f'<a href="#{sid}">{t(en, nl)}</a>' for sid, en, nl in [
        ("start", "Getting started", "Aan de slag"), ("new30", "New in 3.0", "Nieuw in 3.0"), ("fixes2101", "2.10.1", "2.10.1"), ("new210", "New in 2.10", "Nieuw in 2.10"), ("fixes210", "Fixes in 2.10", "Fixes in 2.10"), ("new29", "New in 2.9", "Nieuw in 2.9"), ("new281", "New in 2.8.1", "Nieuw in 2.8.1"), ("new28", "New in 2.8", "Nieuw in 2.8"), ("new27", "New in 2.7", "Nieuw in 2.7"), ("new26", "New in 2.6", "Nieuw in 2.6"), ("new25", "New in 2.5", "Nieuw in 2.5"), ("new24", "New in 2.4", "Nieuw in 2.4"), ("rare24", "Rare places", "Zeldzame plekken"), ("mobs", "Creatures", "Wezens"), ("care", "Your guh", "Jouw guh"), ("personalities", "Personalities", "Karakters"),
        ("maag", "The guh stomach", "De guhmaag"), ("sled", "The guh sled", "De guh-slee"), ("guhdex", "Guhdex", "Guhdex"),
        ("food", "Food &amp; deco", "Eten &amp; deco"),
        ("items", "Items &amp; blocks", "Voorwerpen &amp; blokken"), ("vads", "Vahoege Vads", "Vahoege vads"),
        ("redstone", "Redstone", "Redstone"),
        ("guhmension", "The Guhmension", "De Guhmensie"), ("villages", "Guh villages", "Guhdorpen"), ("structures", "Structures", "Bouwwerken"),
        ("recipes", "Recipes", "Recepten"), ("more", "Advancements", "Vooruitgangen")])

    return f"""<title>Guhs Wiki</title>
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Fredoka:wght@500;600;700&family=Nunito+Sans:ital,wght@0,400;0,600;0,700;1,400&family=JetBrains+Mono:wght@500;600&display=swap">
<style>{CSS}</style>
<div id="app" data-lang="en">
<header class="bar"><div class="wrap">
  <span class="brand"><img src="img/icon_picked_up_guh.png" alt="">Guhs</span>
  <button id="toggle-all" class="ghost" type="button">{t("Open / close all", "Alles open / dicht")}</button>
  <span class="seg" role="group" aria-label="Language / Taal"><button type="button" data-setlang="en" aria-pressed="true">EN</button><button type="button" data-setlang="nl" aria-pressed="false">NL</button></span>
</div></header>
<main class="wrap">
<section class="hero">
  <div>
    <h1>{t("Meet the <em>guhs</em>", "Maak kennis met de <em>guhs</em>")}</h1>
    <p class="tagline">{t("Add lieve vadsige guhs to Minecraft! Chubby plush mice, a pink wool dimension, cheese sauce, hamster playsets, a whole guh theme park, a very hot barbecue dimension &mdash; and now the Knuffeldal: a cosy pink valley with a little guh town, a bakery, a tea house, a hairdresser, a farm and water slides. Piep piep: pieppiepmuisjes, two turtles and a nest of boze kaasknabbels! And now De Grote Guhspelen: sjoelen, a hedge maze, a catapult, a circus zeskamp, a skating tour through the ice-cold Guhpolder and a race circuit. And now Lieve vadsjes van elkaar: hearts with your own guh, a Guhhuisje shaped like a guh head, little chores, toys, guh friends and a dagboekje. And now Guhverhalen: a Baltoguh racing through a snowstorm, a Guhtwo who eats everything twice, a Knuffelhart on the clouds, the 626-guh on the tropical island Guhwai'i, surfing, hula, a Timmerguh and thirteen little critters. VAHOEG!",
                          "Voeg lieve vadsige guhs toe aan Minecraft! Mollige knuffelmuisjes, een dimensie van roze wol, kaassaus, hamsterspeelsets, een heel guhpretpark, een heel hete barbecuedimensie &mdash; en nu het Knuffeldal: een knus roze dal met een guhstadje, een bakkerij, een theehuisje, een kapper, een boerderij en glijbanen. Piep piep: pieppiepmuisjes, twee schildpadden en een nest boze kaasknabbels! En nu De Grote Guhspelen: sjoelen, een heggendoolhof, een katapult, een zeskamp in een circustent, een schaatstocht door de ijskoude Guhpolder en een racecircuit. En nu Lieve vadsjes van elkaar: hartjes met je eigen guh, een Guhhuisje in de vorm van een guhhoofd, klusjes, speelgoed, guh-vriendjes en een dagboekje. En nu Guhverhalen: een Baltoguh die door een sneeuwstorm racet, een Guhtwo die alles dubbel eet, een Knuffelhart op de wolken, de 626-guh op het tropische eiland Guhwai'i, surfen, hula, een Timmerguh en dertien kleine diertjes. VAHOEG!")}</p>
    <ul class="chips"><li>{t("Version", "Versie")} <b>{VERSION}</b></li><li>Minecraft <b>26.1.2</b></li><li>NeoForge</li><li>GeckoLib <b>5.5.2+</b></li></ul>
  </div>
  <div class="hero-art">{img("guh", "A guh")}{img("block_of_kaasknabbels", "", "puff")}</div>
</section>
<div class="layout">
<nav class="toc" aria-label="Contents">{toc}</nav>
<div>{"".join(S)}
<footer>{t("Made for the Guhs mod &middot; model by Lieke &middot; all pictures are rendered from the mod's own models and textures.",
              "Gemaakt voor de Guhs-mod &middot; model door Lieke &middot; alle plaatjes zijn gerenderd uit de modellen en textures van de mod zelf.")}</footer>
</div></div></main></div>
<script>{JS}</script>
"""


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join("docs", "wiki")
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, "index.html"), "w", encoding="utf-8") as f:
        f.write(build())
    print("wrote", os.path.join(out, "index.html"))
