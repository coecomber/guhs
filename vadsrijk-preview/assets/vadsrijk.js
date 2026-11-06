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
  function setZone(z){html.setAttribute('data-zonetot',z);store('guhs-vr-zonetot',z);if(zs)zs.value=z;document.dispatchEvent(new Event('vr-zone'));}
  if(zs){zs.addEventListener('change',function(){setZone(zs.value);});}
  setZone(read('guhs-vr-zonetot')||html.getAttribute('data-zonetot')||'3');
  window.VR={lang:function(){return html.getAttribute('data-lang')||'nl';},zone:function(){return +(html.getAttribute('data-zonetot')||3);},
    t:function(en,nl){return (html.getAttribute('data-lang')==='en')?en:nl;},root:body.getAttribute('data-root')||'',
    live:body.getAttribute('data-live')||'',voorbeeld:body.getAttribute('data-voorbeeld')==='1'};
  // filter tables: <input data-filter="#id">, <select data-filter-col="n" data-filter="#id">
  document.querySelectorAll('[data-filter]').forEach(function(el){
    var tab=document.querySelector(el.getAttribute('data-filter'));if(!tab)return;
    var controls=document.querySelectorAll('[data-filter="'+el.getAttribute('data-filter')+'"]');
    function run(){var rows=tab.tBodies[0].rows,n=0;
      for(var i=0;i<rows.length;i++){var r=rows[i],ok=true;
        controls.forEach(function(c){var v=(c.value||'').toLowerCase().trim();if(!v)return;
          var col=c.getAttribute('data-filter-col');var txt=(col?(r.cells[+col].getAttribute('data-v')||r.cells[+col].textContent):r.textContent).toLowerCase();
          if(col?txt.split('|').indexOf(v)<0&&txt!==v:txt.indexOf(v)<0)ok=false;});
        r.hidden=!ok;if(ok)n++;}
      var cnt=document.querySelector(el.getAttribute('data-filter')+'-n');if(cnt)cnt.textContent=n;}
    el.addEventListener('input',run);el.addEventListener('change',run);});
  // sortable tables
  document.querySelectorAll('table.sort th').forEach(function(th,ci){th.style.cursor='pointer';th.addEventListener('click',function(){
    var tab=th.closest('table'),idx=Array.prototype.indexOf.call(th.parentNode.children,th),rows=Array.prototype.slice.call(tab.tBodies[0].rows);
    var asc=th.getAttribute('aria-sort')!=='ascending';th.parentNode.querySelectorAll('th').forEach(function(x){x.removeAttribute('aria-sort');});
    th.setAttribute('aria-sort',asc?'ascending':'descending');
    rows.sort(function(a,b){var x=a.cells[idx].getAttribute('data-s')||a.cells[idx].textContent,y=b.cells[idx].getAttribute('data-s')||b.cells[idx].textContent;
      var nx=parseFloat(x),ny=parseFloat(y);var c=(!isNaN(nx)&&!isNaN(ny))?nx-ny:x.localeCompare(y,'nl');return asc?c:-c;});
    rows.forEach(function(r){tab.tBodies[0].appendChild(r);});});});

  // ---- live data ----
  function haal(naam){var url=VR.live+naam;return fetch(url,{cache:'no-store'}).then(function(r){if(!r.ok)throw new Error(r.status);return r.json();});}
  VR.haal=haal;
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
    function toon(s){var oud=(Date.now()-Date.parse(s.tijd))/1000;var aan=s.online&&(oud<180||VR.voorbeeld);
      el.querySelector('[data-k=status]').innerHTML='<span class="stip-status '+(aan?'aan':'uit')+'"></span>'+(aan?VR.t('Online','Online'):VR.t('Offline','Offline'));
      el.querySelector('[data-k=spelers]').textContent=aan?s.aantal+' / '+s.max:'-';
      el.querySelector('[data-k=rijk]').textContent=aan?s.in_vadsrijk:'-';
      el.querySelector('[data-k=versie]').textContent=s.versie||'?';
      var namen=el.querySelector('[data-k=namen]');if(namen)namen.innerHTML=aan&&s.spelers.length?s.spelers.map(function(n){return '<a href="'+VR.root+'speler.html#'+encodeURIComponent(n)+'">'+esc(n)+'</a>';}).join(', '):'<span class="leeg">'+VR.t('nobody right now','nu niemand')+'</span>';}
    function fout(){el.querySelector('[data-k=status]').innerHTML='<span class="stip-status uit"></span>'+VR.t('No data','Geen gegevens');}
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
      var e=el.querySelector('[data-k=event]');if(e){var ev=window.VR_EVENTS||[];var d=new Date().getDay();var x=ev[d%ev.length];if(x)e.innerHTML='<b>'+esc(VR.t(x.en,x.nl))+'</b> '+esc(VR.t(x.uit_en,x.uit_nl));}}
    function laad2(){haal('wereld.json').then(function(d){wd=d;geladen=Date.now()-Math.max(0,(Date.now()-Date.parse(d.tijd)))*(VR.voorbeeld?0:1);teken();},function(){});}
    laad2();setInterval(laad2,30000);setInterval(teken,1000);document.addEventListener('vr-lang',teken);})(document.querySelector('[data-live-wereld]'));
  // highscores: [data-hiscores]
  (function(el){if(!el)return;
    var lijsten=JSON.parse(el.getAttribute('data-hiscores'));var tabs=el.querySelector('.hs-tabs'),tb2=el.querySelector('tbody'),kop=el.querySelector('[data-k=kop]'),zoek=el.querySelector('input');
    var huidig=(location.hash||'#generiek').slice(1),rijen=[];
    function kies(id){huidig=id;tabs.querySelectorAll('button').forEach(function(b){b.setAttribute('aria-pressed',String(b.getAttribute('data-l')===id));});
      var L=lijsten.filter(function(l){return l.id===id;})[0]||lijsten[0];kop.innerHTML=VR.t(L.k_en,L.k_nl);
      haal('hiscores/'+L.id+'.json').then(function(d){rijen=d.rijen;vul();el.querySelector('[data-k=tijd]').textContent=new Date(d.tijd).toLocaleString();},function(){tb2.innerHTML='<tr><td colspan=4 class="leeg">'+VR.t('No data (is the server online?)','Geen gegevens (staat de server aan?)')+'</td></tr>';});
      history.replaceState(null,'','#'+id);}
    function vul(){var q=(zoek.value||'').toLowerCase();tb2.innerHTML=rijen.filter(function(r){return !q||r.naam.toLowerCase().indexOf(q)>=0;}).slice(0,100).map(function(r){
      return '<tr><td class="num">'+r.plek+'</td><td><img class="hoofd" alt="" loading="lazy" src="https://mc-heads.net/avatar/'+encodeURIComponent(r.naam)+'/24"><a href="speler.html#'+encodeURIComponent(r.naam)+'">'+esc(r.naam)+'</a></td><td class="num">'+r.waarde+'</td><td class="num">'+(r.xp!=null?r.xp.toLocaleString():'')+'</td></tr>';}).join('')||'<tr><td colspan=4 class="leeg">'+VR.t('Nobody yet','Nog niemand')+'</td></tr>';}
    lijsten.forEach(function(l){var b=document.createElement('button');b.type='button';b.setAttribute('data-l',l.id);b.innerHTML='<span lang="en">'+l.en+'</span><span lang="nl">'+l.nl+'</span>';b.addEventListener('click',function(){kies(l.id);});tabs.appendChild(b);});
    zoek.addEventListener('input',vul);kies(huidig);})(document.querySelector('[data-hiscores]'));
  // profile: [data-profiel]
  (function(el){if(!el)return;
    function laadP(){var naam=decodeURIComponent((location.hash||'').slice(1));var uit=el.querySelector('[data-k=uit]');
      if(!naam){haal('spelers/index.json').then(function(d){uit.innerHTML='<h2>'+VR.t('All players','Alle spelers')+'</h2><p>'+d.spelers.map(function(s){return '<a href="#'+encodeURIComponent(s.naam)+'">'+esc(s.naam)+'</a> <span class="badge lv">lv '+s.lv+'</span>';}).join(' &middot; ')+'</p>';},function(){uit.innerHTML='<p class="leeg">'+VR.t('No data.','Geen gegevens.')+'</p>';});return;}
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
          '<div><dt>'+VR.t('Last seen','Laatst gezien')+'</dt><dd>'+esc(p.gezien)+'</dd></div>'+(p.eerste_50?'<div><dt>Lv 50</dt><dd>'+esc(p.eerste_50)+'</dd></div>':'')+'</dl></div></aside></div>';
        document.title=p.naam+' - Het Vadsrijk';},function(){uit.innerHTML='<p class="leeg">'+VR.t('No player with this name (yet).','(Nog) geen speler met deze naam.')+'</p>';});}
    window.addEventListener('hashchange',laadP);laadP();document.addEventListener('vr-lang',laadP);})(document.querySelector('[data-profiel]'));
})();
