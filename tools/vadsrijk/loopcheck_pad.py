# Fix round 1 (quests): walks between quest spots on the real tiles. usage (project root): python tools/vadsrijk/loopcheck_pad.py .
import sys
src=open(sys.argv[0].replace("loopcheck_pad.py","loopcheck_doelen.py")).read()
exec(src.split("DOELEN = {")[0])
def pad(a, b, marge=40, max_n=600000):
    ax,ay,az=a; bx,by,bz=b
    ay=vloer(ax,ay,az); by=vloer(bx,by,bz)
    if ay is None or by is None: return "no floor"
    x0,x1=min(ax,bx)-marge,max(ax,bx)+marge; z0,z1=min(az,bz)-marge,max(az,bz)+marge
    seen={(ax,ay,az)}; q=deque([(ax,ay,az)])
    while q and len(seen)<max_n:
        cx,cy,cz=q.popleft()
        if abs(cx-bx)<=2 and abs(cz-bz)<=2 and abs(cy-by)<=2: return f"OK ({len(seen)} cells)"
        for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
            nx,nz=cx+dx,cz+dz
            if not(x0<=nx<=x1 and z0<=nz<=z1): continue
            for dy in (0,1,-1,-2,-3):
                ny=cy+dy
                if (nx,ny,nz) in seen: break
                if staan(nx,ny,nz) and (dy<=0 or lucht(blok(cx,cy+2,cz))):
                    seen.add((nx,ny,nz)); q.append((nx,ny,nz)); break
            # lifts (wolkenlift/wolkenstroom) and ladders/vines: climb straight up
        n=blok(cx,cy,cz)
        if "wolken" in n or "ladder" in n or "vine" in n or "scaffold" in n:
            for up in range(1,140):
                if staan(cx,cy+up,cz) and "wolken" not in blok(cx,cy+up,cz):
                    if (cx,cy+up,cz) not in seen: seen.add((cx,cy+up,cz)); q.append((cx,cy+up,cz))
                    break
    return f"NOT FOUND ({len(seen)} cells searched)"
P=POI
for naam,a,b in [
  ("jetty -> kampplein", (1,65,1134), P["kampvuur_kamp"]),
  ("haven -> plein", P["luchtschip_haven_kapitein"], P["plein"]),
  ("lift top -> grootmeester", P["lift_tempel_top"], P["ordetempel_binnen"]),
  ("boswachter -> grotkamp", (-214,65,6), P["kampvuur_druipgrotten_1"]),
  ("grotkamp -> grot ingang 1", P["kampvuur_druipgrotten_1"], P["grot_ingangen.1"]),
  ("grot ingang 1 -> mopje", P["grot_ingangen.1"], P["npc_mopje"]),
  ("plein -> fossielbrug van", P["plein"], P["fossielbrug.van"]),
  ("fossielbrug van -> naar", P["fossielbrug.van"], P["fossielbrug.naar"]),
  ("fossielbrug naar -> tempelpoort", P["fossielbrug.naar"], P["deur_zwamtempel.voor"]),
]:
    print(f"{naam:32s}", pad(a,b,marge=90,max_n=900000), flush=True)
