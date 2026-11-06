"""Palet voor zone 6 "Het Zijdeland": roze zandsteen, pastel pleister, gouden en turquoise koepels, mozaiek.

Alleen bestaande blokken (Guhs k-int + vanilla 26.1) plus een paar zone-5-conceptblokken (hv_*: marmer, goudlijst,
gouden dakpan). In de code heet alles met een korte sleutel; Z["rots"] enz. Trappen/platen via trap()/plaat().
"""
G = "guhs:"
M = "minecraft:"

Z = {
    # zand en rots
    "zand": G + "roze_zand",
    "zand2": M + "pink_concrete_powder",
    "zand_licht": M + "white_concrete_powder",
    "rots": M + "white_terracotta",          # roze-beige zandsteen (hoofdkleur)
    "rots_roze": G + "pleister_roze",
    "rots_perzik": G + "pleister_perzik",
    "rots_marmer": G + "roze_marmer",
    "rots_rood": M + "pink_terracotta",      # de diepe Petra-band (spaarzaam)
    "rots_diep": M + "magenta_terracotta",
    "zout": M + "calcite",
    "zout2": M + "white_concrete_powder",
    "zoutkristal": G + "fossielmijn_zoutkristalblok",
    "zoutkristalletjes": G + "fossielmijn_zoutkristalletjes",
    "gras": M + "grass_block[snowy=false]",
    "modder": M + "packed_mud",
    # gevels
    "muur_vanille": G + "pleister_vanille",
    "muur_roze": G + "pleister_roze",
    "muur_perzik": G + "pleister_perzik",
    "muur_boter": G + "pleister_boter",
    "muur_framboos": G + "pleister_framboos",
    "muur_lavendel": G + "pleister_lavendel",
    "steen_roze": G + "pastelbaksteen_roze",
    "steen_vanille": G + "pastelbaksteen_vanille",
    "steen_perzik": G + "pastelbaksteen_perzik",
    "steen_boter": G + "pastelbaksteen_boter",
    "marmer": G + "roze_marmer",
    "marmer_glad": G + "roze_marmer_gepolijst",
    "wit": G + "wolkmarmer",
    "wit_glad": G + "wolkmarmer_gepolijst",
    "zuil": G + "roze_marmer_zuil[axis=y]",
    "zuil_wit": G + "wolkmarmer_zuil[axis=y]",
    "zuilkop": G + "gouden_zuilkop[axis=y]",
    "goud": G + "gouden_kroonlijst",
    "filigraan": G + "rozenzandsteen_gebeiteld",
    "goudblok": M + "gold_block",
    # tegels / mozaiek
    "tegel_roze": M + "pink_glazed_terracotta",
    "tegel_blauw": M + "light_blue_glazed_terracotta",
    "tegel_turkoois": M + "cyan_glazed_terracotta",
    "tegel_wit": M + "white_glazed_terracotta",
    "tegel_hart": G + "tegel_hartjes",
    "tegel_parel": G + "parelmoer_tegels",
    "vloer_dambord": G + "tegel_dambord",
    "vloer_zand": M + "smooth_sandstone",
    # daken / koepels
    "koepel_roze": G + "guh_dakpan_roze",
    "koepel_goud": G + "gouden_dakpan",
    "koepel_lavendel": G + "dakpan_lavendel",
    "koepel_turkoois": G + "dakpan_hemelsblauw",
    "koepel_mint": G + "dakpan_mint",
    # hout, stof, glas
    "hout": G + "guhwaii_palm_planken",
    "hout_kers": M + "cherry_planks",
    "luik_kers": M + "cherry_trapdoor",
    "glas": G + "glasinlood_bloem",
    "glas_guh": G + "glasinlood_guh",
    "glas_regenboog": G + "glasinlood_regenboog",
    "glas_hart": G + "glasinlood_hartje",
    # grauw (de Keizer: alleen een vleugje)
    "grauw": M + "light_gray_concrete_powder",
    "grauw2": M + "light_blue_terracotta",
    "grauw3": M + "tuff",
    "grauw_steen": M + "tuff_bricks",
    # [zone 6 kaart] the new Zijdeland blocks (tools/features/vadsrijk_u_z6blokken.py)
    "zs": G + "rozenzandsteen",
    "zs_beitel": G + "rozenzandsteen_gebeiteld",      # carved rose sandstone with the guh motif
    "mozaiek_ster": G + "mozaiek_ster",
    "mozaiek_guh": G + "mozaiek_guh",
    "mozaiek_bloem": G + "mozaiek_bloem",
    "mashrabiya": G + "mashrabiya_tralies",          # lattice pane (connects like iron bars: verbind.py)
    "koepel_tegel": G + "koepeltegel_turkoois",
    "zijde": G + "zijden_doek",
    "luifel_zijde": G + "zijden_luifel",
    "messing_lantaarn": G + "messing_lantaarn",
    "palm_stam": G + "dadelpalm_stam[axis=y]",
    "palm_planken": G + "dadelpalm_planken",
    "palm_hek": G + "dadelpalm_hek",
}

WOL = {"roze": M + "pink_wool", "wit": M + "white_wool", "geel": M + "yellow_wool", "turkoois": M + "cyan_wool",
       "lila": M + "purple_wool", "oranje": M + "orange_wool", "blauw": M + "light_blue_wool",
       "magenta": M + "magenta_wool", "lime": M + "lime_wool"}
TAPIJT = {k: v.replace("_wool", "_carpet") for k, v in WOL.items()}


def b(k):
    return Z.get(k, k) if isinstance(k, str) else k


def trap(k, facing, half="bottom", shape="straight"):
    s = b(k)
    if not s.endswith("_stairs") and not s.endswith("_trap"):
        s = TRAP.get(k, s)
    return f"{s}[facing={facing},half={half},shape={shape},waterlogged=false]"


def plaat(k, typ="bottom"):
    s = PLAAT.get(k, b(k))
    return f"{s}[type={typ},waterlogged=false]"


TRAP = {"zs": G + "rozenzandsteen_trap", "koepel_tegel": G + "koepeltegel_turkoois_trap", "palm_planken": G + "dadelpalm_trap",
        "muur_vanille": G + "pleister_vanille_trap", "muur_roze": G + "pleister_roze_trap",
        "muur_perzik": G + "pleister_perzik_trap", "muur_boter": G + "pleister_boter_trap",
        "steen_roze": G + "pastelbaksteen_roze_trap", "steen_vanille": G + "pastelbaksteen_vanille_trap",
        "steen_perzik": G + "pastelbaksteen_perzik_trap", "marmer": G + "roze_marmer_gepolijst_trap",
        "marmer_glad": G + "roze_marmer_gepolijst_trap", "wit": G + "wolkmarmer_gepolijst_trap",
        "wit_glad": G + "wolkmarmer_gepolijst_trap", "goud": G + "gouden_kroonlijst_trap",
        "koepel_roze": G + "guh_dakpan_roze_trap", "koepel_goud": G + "gouden_dakpan_trap",
        "hout": G + "guhwaii_palm_trap", "rots": M + "smooth_sandstone_stairs",
        "grauw_steen": M + "tuff_brick_stairs", "vloer_zand": M + "smooth_sandstone_stairs"}
PLAAT = {"zs": G + "rozenzandsteen_plaat", "palm_planken": G + "dadelpalm_plaat",
         "muur_vanille": G + "pleister_vanille_plaat", "muur_roze": G + "pleister_roze_plaat",
         "muur_perzik": G + "pleister_perzik_plaat", "muur_boter": G + "pleister_boter_plaat",
         "steen_roze": G + "pastelbaksteen_roze_plaat", "steen_vanille": G + "pastelbaksteen_vanille_plaat",
         "steen_perzik": G + "pastelbaksteen_perzik_plaat", "marmer": G + "roze_marmer_gepolijst_plaat",
         "marmer_glad": G + "roze_marmer_gepolijst_plaat", "wit": G + "wolkmarmer_gepolijst_plaat",
         "wit_glad": G + "wolkmarmer_gepolijst_plaat", "goud": G + "gouden_kroonlijst_plaat",
         "koepel_roze": G + "guh_dakpan_roze_plaat", "koepel_goud": G + "gouden_dakpan_plaat",
         "hout": G + "guhwaii_palm_plaat", "grauw_steen": M + "tuff_brick_slab", "vloer_zand": M + "smooth_sandstone_slab"}
MUURTJE = {"zs": G + "rozenzandsteen_muur",
           "muur_vanille": G + "pleister_vanille_muur", "muur_roze": G + "pleister_roze_muur",
           "muur_perzik": G + "pleister_perzik_muur", "muur_boter": G + "pleister_boter_muur",
           "steen_roze": G + "pastelbaksteen_roze_muur", "steen_vanille": G + "pastelbaksteen_vanille_muur",
           "marmer": G + "roze_marmer_gepolijst_muur", "wit": G + "wolkmarmer_gepolijst_muur",
           "grauw_steen": M + "tuff_brick_wall"}
