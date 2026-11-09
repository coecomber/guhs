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
  if(read('guhs-vr-spoileruitleg-weg')!=='1'&&zs&&zs.options.length>1){   // X-SITE: only one open zone = nothing to explain
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
