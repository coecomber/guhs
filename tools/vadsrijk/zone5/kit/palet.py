"""Paletten voor Hoogvads: dezelfde bouwscripts, drie blokkeuzes.

  "nieuw"   : bestaande Guhs-blokken + de concept-blokken (hv_*) uit concept_blokken.py   (standaard)
  "guhs"    : alleen wat er nu al in de mod (k-int) + vanilla zit
  "vanilla" : alleen vanilla 26.1

Gebruik: import palet; P = palet.P (dict) en palet.kies("vanilla") om te wisselen (vóór het bouwen).
Trappen/platen: P.trap("marmer", "north"), P.plaat("marmer").
"""
G = "guhs:"
M = "minecraft:"

# [zone 5 kaart] "nieuw" = the game palette: the Hoogvads blocks of the mod (vadsrijk_t_z5blokken.py, the minimal epic set,
# without the hv_ prefix); the concept blocks that were NOT added map to existing Guhs blocks (like the "guhs" palette)
_NIEUW = {
    # gevels / muren
    "marmer": G + "roze_marmer",
    "marmer_glad": G + "roze_marmer_gepolijst",
    "marmer_trap": G + "roze_marmer_gepolijst_trap",
    "marmer_plaat": G + "roze_marmer_gepolijst_plaat",
    "marmer_muur": G + "roze_marmer_gepolijst_muur",
    "marmer_gezicht": G + "roze_marmer_guhgezicht",
    "marmer_tegels": G + "viltsteen_tegels",
    "goudrand": G + "gouden_kroonlijst",
    "wit": G + "wolkmarmer",
    "wit_glad": G + "wolkmarmer_gepolijst",
    "wit_trap": G + "wolkmarmer_gepolijst_trap",
    "wit_plaat": G + "wolkmarmer_gepolijst_plaat",
    "wit_muur": G + "wolkmarmer_gepolijst_muur",
    "zuil": G + "roze_marmer_zuil[axis=y]",
    "zuil_wit": G + "wolkmarmer_zuil[axis=y]",
    "zuilkop": G + "gouden_zuilkop[axis=y]",
    "vloer": G + "tegel_dambord",
    "vloer2": G + "viltsteen_tegels",
    # goud
    "goud": G + "gouden_kroonlijst",
    "goud_trap": G + "gouden_kroonlijst_trap",
    "goud_plaat": G + "gouden_kroonlijst_plaat",
    "filigraan": G + "tegel_hartjes",
    "goudblok": M + "gold_block",
    # glas
    "glas": G + "kathedraalglas_ruitjes",
    "glas_guh": G + "kathedraalglas_guh",
    "glas_roos": G + "kathedraalglas_roos",
    "glas_hart": G + "kathedraalglas_hart",
    "raam": G + "kathedraalglas_ruitjes",
    # daken
    "dak_goud": G + "gouden_dakpan",
    "dak_goud_trap": G + "gouden_dakpan_trap",
    "dak_goud_plaat": G + "gouden_dakpan_plaat",
    "dak_roze": G + "guh_dakpan_roze",
    "dak_roze_trap": G + "guh_dakpan_roze_trap",
    "dak_roze_plaat": G + "guh_dakpan_roze_plaat",
    "dak_wit": G + "guh_dakpan_wit",
    "dak_wit_trap": G + "guh_dakpan_wit_trap",
    "dak_wit_plaat": G + "guh_dakpan_wit_plaat",
    # beelden
    "beeld": G + "pleister_vanille",
    "beeld_gezicht": G + "knuffelsteen_gezicht",
    "beeld_goud": M + "gold_block",
    # fundering / terrassen
    "steen": G + "wolkenweide_steen",
    "steen_bak": G + "pastelbaksteen_wolkje",
    "steen_trap": G + "pastelbaksteen_wolkje_trap",
    "steen_plaat": G + "pastelbaksteen_wolkje_plaat",
    "steen_muur": G + "pastelbaksteen_wolkje_muur",
    # walvis
    "huid": G + "walvishuid",
    "huid_licht": G + "walvishuid_licht",
    "buik": G + "walvisbuik",
    "huid_pok": G + "walvishuid_wolkpok",
    # vaandel
    "vaandel_boven": G + "hoogvads_vaandel_boven",
    "vaandel_onder": G + "hoogvads_vaandel_onder",
}

_GUHS = dict(_NIEUW)
_GUHS.update({
    "marmer": G + "pastelbaksteen_roze",
    "marmer_glad": G + "pleister_roze",
    "marmer_trap": G + "pleister_roze_trap",
    "marmer_plaat": G + "pleister_roze_plaat",
    "marmer_muur": G + "pleister_roze_muur",
    "marmer_gezicht": G + "gebeitelde_viltsteen",
    "marmer_tegels": G + "viltsteen_tegels",
    "goudrand": G + "pastelbaksteen_boter",
    "wit": G + "pastelbaksteen_vanille",
    "wit_glad": G + "pleister_vanille",
    "wit_trap": G + "pleister_vanille_trap",
    "wit_plaat": G + "pleister_vanille_plaat",
    "wit_muur": G + "pleister_vanille_muur",
    "zuil": M + "quartz_pillar[axis=y]",
    "zuil_wit": M + "quartz_pillar[axis=y]",
    "zuilkop": M + "chiseled_quartz_block",
    "vloer": G + "tegel_dambord",
    "vloer2": G + "viltsteen_tegels",
    "goud": M + "gold_block",
    "goud_trap": G + "pastelbaksteen_boter_trap",
    "goud_plaat": G + "pastelbaksteen_boter_plaat",
    "filigraan": G + "tegel_hartjes",
    "glas": G + "glasinlood_bloem",
    "glas_guh": G + "glasinlood_guh",
    "glas_roos": G + "glasinlood_regenboog",
    "glas_hart": G + "glasinlood_hartje",
    "raam": G + "glasinlood_bloem",
    "dak_goud": G + "pastelbaksteen_boter",
    "dak_goud_trap": G + "pastelbaksteen_boter_trap",
    "dak_goud_plaat": G + "pastelbaksteen_boter_plaat",
    "beeld": G + "pleister_vanille",
    "beeld_gezicht": G + "knuffelsteen_gezicht",
    "beeld_goud": M + "gold_block",
    "steen": G + "wolkenweide_steen",
    "steen_bak": G + "pastelbaksteen_wolkje",
    "steen_trap": G + "pastelbaksteen_wolkje_trap",
    "steen_plaat": G + "pastelbaksteen_wolkje_plaat",
    "steen_muur": G + "pastelbaksteen_wolkje_muur",
    "huid": G + "pastelbaksteen_korenbloem",
    "huid_licht": G + "pleister_orchidee",
    "buik": G + "pleister_vanille",
    "huid_pok": G + "wolkenblok_wit",
    "vaandel_boven": None,
    "vaandel_onder": G + "vr_eco_vadsrijk_vaandel",
})

_VANILLA = dict(_NIEUW)
_VANILLA.update({
    "marmer": M + "pink_terracotta",
    "marmer_glad": M + "pink_concrete",
    "marmer_trap": M + "cherry_stairs",
    "marmer_plaat": M + "cherry_slab",
    "marmer_muur": M + "cherry_fence",
    "marmer_gezicht": M + "pink_glazed_terracotta",
    "marmer_tegels": M + "pink_terracotta",
    "goudrand": M + "yellow_terracotta",
    "wit": M + "quartz_bricks",
    "wit_glad": M + "smooth_quartz",
    "wit_trap": M + "smooth_quartz_stairs",
    "wit_plaat": M + "smooth_quartz_slab",
    "wit_muur": M + "diorite_wall",
    "zuil": M + "quartz_pillar[axis=y]",
    "zuil_wit": M + "quartz_pillar[axis=y]",
    "zuilkop": M + "chiseled_quartz_block",
    "vloer": M + "pink_glazed_terracotta",
    "vloer2": M + "pink_terracotta",
    "goud": M + "gold_block",
    "goud_trap": M + "smooth_sandstone_stairs",
    "goud_plaat": M + "smooth_sandstone_slab",
    "filigraan": M + "chiseled_quartz_block",
    "glas": M + "pink_stained_glass",
    "glas_guh": M + "magenta_stained_glass",
    "glas_roos": M + "pink_stained_glass",
    "glas_hart": M + "magenta_stained_glass",
    "raam": M + "pink_stained_glass",
    "dak_goud": M + "gold_block",
    "dak_goud_trap": M + "smooth_sandstone_stairs",
    "dak_goud_plaat": M + "smooth_sandstone_slab",
    "dak_roze": M + "cherry_planks",
    "dak_roze_trap": M + "cherry_stairs",
    "dak_roze_plaat": M + "cherry_slab",
    "dak_wit": M + "pale_oak_planks",
    "dak_wit_trap": M + "pale_oak_stairs",
    "dak_wit_plaat": M + "pale_oak_slab",
    "beeld": M + "white_concrete",
    "beeld_gezicht": M + "white_glazed_terracotta",
    "beeld_goud": M + "gold_block",
    "steen": M + "calcite",
    "steen_bak": M + "quartz_bricks",
    "steen_trap": M + "quartz_stairs",
    "steen_plaat": M + "quartz_slab",
    "steen_muur": M + "diorite_wall",
    "huid": M + "light_blue_terracotta",
    "huid_licht": M + "purple_terracotta",
    "buik": M + "white_terracotta",
    "huid_pok": M + "white_wool",
    "vaandel_boven": None,
    "vaandel_onder": None,
})

PALETTEN = {"nieuw": _NIEUW, "guhs": _GUHS, "vanilla": _VANILLA}
MODUS = "nieuw"
P = dict(_NIEUW)


def kies(modus):
    global MODUS
    MODUS = modus
    P.clear()
    P.update(PALETTEN[modus])


def _props(s, extra):
    if "[" in s:
        naam, rest = s[:-1].split("[")
        return f"{naam}[{rest},{extra}]"
    return f"{s}[{extra}]"


def trap(sleutel, facing, half="bottom", shape="straight"):
    s = P[sleutel]
    return f"{s}[facing={facing},half={half},shape={shape},waterlogged=false]"


def plaat(sleutel, typ="bottom"):
    return f"{P[sleutel]}[type={typ},waterlogged=false]"


def zuil(sleutel="zuil", as_="y"):
    s = P[sleutel]
    return s.replace("axis=y", f"axis={as_}")
