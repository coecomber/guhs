"""
1.1.2: the placement rebalance of the Guhmension (Java: world/GegarandeerdPlacement, world/BouwRuimte).

  - Every minigame gets ONE guaranteed copy 700-1500 blocks from 0,0 (structure set <name>_gegarandeerd, placement
    guhs:gegarandeerd), every landmark one 1500-2500 blocks away; their normal random_spread sets stay (the minigames'
    44/16 sets became 32/12 in their own modules). The ring is cut into slices (one per structure of the ring), so the
    guaranteed buildings lie spread around spawn. The search in GegarandeerdPlacement tries spots until the building really
    starts (biome, flat ground, room: a guaranteed copy goes before every normal building).
  - The story structures (the places of the Verhalen questlines, Opa Guh's kampeerplekjes and the Guhbubbel) get the
    structure tag guhs:verhaal: BouwRuimte never starts them within 600 blocks of 0,0.

Runs after every other feature module (last in features.FEATURES), so all structures exist already.
"""
import json
import os

# (the Minigames registry: the classics, the Knuffeldal games with a building of their own, De Grote Guhspelen; plus the
# kermis. The Knuffeldal town's games and the Elf-Guhjestocht are story places, see VERHAAL)
MINIGAMES = ["guh_circuit", "guhdoolhof", "guh_golfbaan", "guh_racebaan", "knabbelspelen", "mika_mep_hal", "vadsig_eetfestijn",
             "guh_beauty_theater", "guh_disco", "guh_kermis", "verstopguh_huis", "guhvis_vijver", "sjoelhuisje", "knabbelkatapult",
             "guh_sterrenwacht", "ballonfestival", "knuffelbad"]
MINIGAME_RING = (700, 1500)

LANDMARKS = ["guh_kasteel", "guh_village", "guhbibliotheek", "kaasmijn", "hemelkapelletje", "zwevende_eilanden"]
LANDMARK_RING = (1500, 2500)

# the places of the Verhalen questlines (timmerguh/knusfeest/beroepen: the Knuffeldal town; balto: Nomguh; slee: the sleehut;
# mewtwo: the kloon-eiland; hemel: the Hemelkapelletje; guhwaii: its three places; vadsig: the heiligdom and the Mika-kamp;
# guheinde: the Guhkasteel; grillguh: the barbecueput), the Elf-Guhjestocht, the Guhbubbel, Piep's nest, the Evil Mika home
# and Opa Guh's kampeerplekjes. (Not the Knabbelkelders: the four portals keep their fixed rings around spawn.)
VERHAAL = ["knuffeldal_stadje", "nomguh", "sleehut", "kloon_eiland", "hemelkapelletje", "guhwaii_capsule", "guhwaii_ohana",
           "guhwaii_surfstrand", "vadsig_heiligdom", "mika_kamp", "guh_kasteel", "barbecueput", "elfguhjestocht", "onderwater",
           "kaasknabbel_nest", "evil_mika_home", "kampeerplekje"]
VERHAAL_AFSTAND = 600   # (BouwRuimte.VERHAAL_AFSTAND)


def build(h):
    D, w = h.D, h.w
    for names, (lo, hi) in ((MINIGAMES, MINIGAME_RING), (LANDMARKS, LANDMARK_RING)):
        for i, name in enumerate(names):
            assert os.path.exists(f"{D}/worldgen/structure/{name}.json"), name
            normal = json.load(open(f"{D}/worldgen/structure_set/{name}.json", encoding="utf-8"))
            assert normal["placement"]["type"] == "minecraft:random_spread", name
            w(f"{D}/worldgen/structure_set/{name}_gegarandeerd.json", {
                "structures": [{"structure": f"guhs:{name}", "weight": 1}],
                "placement": {"type": "guhs:gegarandeerd", "salt": normal["placement"]["salt"] + 7,
                              "min_afstand": lo, "max_afstand": hi, "sector": i, "sectoren": len(names)}})
    for name in VERHAAL:
        assert os.path.exists(f"{D}/worldgen/structure/{name}.json"), name
    w(f"{D}/tags/worldgen/structure/verhaal.json", {"values": [f"guhs:{n}" for n in VERHAAL]})
