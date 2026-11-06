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
    Lbaas=laag('bazen','Bosses','Bazen',true),Lquest=laag('quests','Quest givers','Questgevers',false),Lkar=laag('karavaan','Caravan (live)','Karavaan (live)',true);
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
  var tekens={kampvuur:'&#128293;',uitkijk:'&#128065;',luchtschip:'&#9992;',portaal:'&#10022;',kerker:'&#9876;',gondel:'&#8597;',lift:'&#8597;'};
  D.punten.forEach(function(p){var L2=p.soort==='kampvuur'?Lvuur:p.soort==='uitkijk'?Luit:p.soort==='kerker'?Lbaas:Lreis;
    var m=pin(p.soort,tekens[p.soort]||'&#8226;',p.pos[0],p.pos[2],p.naam,'',p.zone,p.id);m._vrMin=p.soort==='kerker'?-2:-0.5;m.addTo(L2);});
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
})();
