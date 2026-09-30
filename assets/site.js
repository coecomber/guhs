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
