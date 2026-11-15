"""
1.10.4 "T6 in het Zijdeland" + the islet honey: tier T6 of the Ambachten is on (zone 6, Het Zijdeland), and the Ambachtseiland's
Bijenhofje gives its own T1 honey. Java: feature/vadsrijk/ambacht/Zone6Haken (the zone-6 gathering groups), Materialen (the T6
materials, klaverhoning, the Sterrenkern), Recepten/Eten/Brouwsels (the T6 jewellery and Koken row), EilandRegistratie (klaverhoning).

One source for:
  - the icons of the T6 materials, the islet honey, the Sterrenkern and the T6 dishes/potions (the t-ambacht painters);
  - every new text (Dutch) + the English overlay tools/lang/en/c482_vr_ambacht_t6.json: the zone-6 group names, the T6 recipe names,
    the made pieces' names (vadsrijk.loot.mw_t6_*), the dishes/potions, the klaverhoning, the "only in Saffraanpoort" line.
    (The T6 material names themselves come from vadsrijk_u_ambacht2.py's ZONE6 table, written when T6 was still "zone 6 volgt".)

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_ambacht_t6.py
then: python tools/lang/apply_en.py --no-ftb   (en_us from nl_nl + the overlays)
"""
import json
import os

from vadsrijk_economie import drankje
import vadsrijk_t_ambacht as T
import vadsrijk_u_ambacht2 as U

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
S = "vadsrijk.ambacht."

TEKSTEN = {}   # key -> Dutch
EN = {}        # key -> English


def t(k, nl, en):
    assert k not in TEKSTEN, k
    TEKSTEN[k] = nl
    EN[k] = en


# ---- icons (same ids as Materialen.java; painter kinds of vadsrijk_t_ambacht / vadsrijk_u_ambacht2) ------------------------------
ICONEN = {
    "sterrengoud": ("erts", 0xFFD870), "dadelpalmhout": ("hout", 0xC89A6A), "wierookhars": ("hars", 0xE8C8A0),
    "saffraan": ("bloem", 0xFF8A2A), "zijdedraad": ("vezel", 0xFFE8F4), "oasekarper": ("vis", 0x4AC8B8),
    "sterrenschubvis": ("vis", 0x3A3A8A), "zandaal": ("vis", 0xD8B888), "saffraanhoning": ("honing", 0xFF9A2A),
    "klaverhoning": ("honing", 0xFFE89A),
}
KERN = ("sterrenkern", 0xFFD870)
ETEN = {   # id: (shape, colour, Dutch, English)
    "saffraanrijst": ("kom", 0xFF8A2A, "Saffraanrijst", "Saffron Rice"),
    "dadelstoofpot": ("bord", 0xC89A6A, "Dadelstoofpot", "Date Stew"),
    "sterrenfeestmaal": ("feest", 0xFFD870, "Sterrenfeestmaal", "Star Feast"),
}
DRANKEN = {   # id: (colour, Dutch item name, English item name, Dutch recipe name, English recipe name)
    "saffraan_vadsdrank": (0xFF7A3A, "Gebrouwen Saffraan-Vadsdrank", "Brewed Saffron Chonk Potion", "Saffraan-Vadsdrank", "Saffron Chonk Potion"),
    "saffraan_energiedrank": (0xFFC83A, "Gebrouwen Saffraan-Energiedrank", "Brewed Saffron Energy Potion", "Saffraan-Energiedrank",
                              "Saffron Energy Potion"),
    "sterrenelixer": (0x8A7AE0, "Sterrenelixer", "Star Elixir", "Sterrenelixer", "Star Elixir"),
}

# ---- the islet honey ------------------------------------------------------------------------------------------------------------
t("item.guhs.vr_amb_klaverhoning", "Klaverhoning", "Clover Honey")
t("item.guhs.vr_amb_klaverhoning.uitleg", "Lichte honing van het Bijenhofje op het Ambachtseiland. Lekker in een pudding of een "
  "energiedrankje (T1).", "Light honey from the Bee Garden on the Craft Island. Nice in a pudding or an energy potion (T1).")

# ---- dishes, potions and their recipes --------------------------------------------------------------------------------------------
for _id, (_s, _k, _nl, _en) in ETEN.items():
    t(f"item.guhs.vr_amb_eten_{_id}", _nl, _en)
    t(S + "recept." + _id, _nl, _en)
for _id, (_k, _nl, _en, _rnl, _ren) in DRANKEN.items():
    t(f"item.guhs.vr_amb_drank_{_id}", _nl, _en)
    t(S + "recept." + _id, _rnl, _ren)

# ---- the T6 jewellery (Maatwerk + Meesterwerk), the made pieces' names, the core from splinters --------------------------------------
TIER6 = ("Sterrengouden", "Zijden", "Sterrenkern-")
TIER6_EN = {"metaal": "Star Gold", "vezel": "Silk", "kern": "Star Core"}
MW_EN = {"zwaard": "{metaal} Sword", "boog": "{metaal} Bow", "staf": "{metaal} Staff", "helm": "{metaal} Helmet",
         "borstplaat": "{metaal} Breastplate", "broek": "{vezel} Pants", "schoentjes": "{vezel} Shoes", "ring": "{metaal} Ring",
         "ketting": "{metaal} Necklace", "oorbel": "{metaal} Earring", "armband": "{metaal} Bracelet"}
MEESTER_EN = {"ring": "{kern} Master Ring", "ketting": "{kern} Master Necklace", "oorbel": "{kern} Master Earring",
              "armband": "{kern} Master Bracelet"}
for _s in T.SIERADEN:
    t(S + f"recept.{_s}_t6", T.MW_TIER[_s].format(*TIER6), MW_EN[_s].format(**TIER6_EN))
    t(S + f"recept.meester_{_s}_t6", T.MEESTER_NAAM[_s].format(*TIER6).replace("- ", "-"), MEESTER_EN[_s].format(**TIER6_EN))
for _id in T.MW:
    t(f"vadsrijk.loot.mw_t6_{_id}", T.MW_TIER[_id].format(*TIER6).replace("- ", "-"), MW_EN[_id].format(**TIER6_EN))
t(S + "recept.kern_sterren", "Sterrenkern (uit splinters)", "Star Core (from splinters)")

# ---- where things are made, the book ---------------------------------------------------------------------------------------------
t(S + "werk.zijdeland", "Alleen in de werkplaatsen van Saffraanpoort (Het Zijdeland).", "Only in the workshops of Saffron Gate (the Silklands).")
t(S + "boek.cat.plekken.6", "Plekken zone 6", "Spots zone 6")

# ---- the zone-6 gathering groups (Zone6Haken.PLEKKEN + the bee meadow) -------------------------------------------------------------
GROEPEN = {
    "z6_pluk_rozenkloof": ("de terrastuinen van Rozenkloof", "the terrace gardens of Rose Gorge"),
    "z6_pluk_kanaalwijk": ("de kruidentuintjes in de Kanaalwijk van Saffraanpoort", "the herb gardens in the Canal Quarter of Saffron Gate"),
    "z6_pluk_moerbei": ("de moerbeibomen in De Palmenkloven (zijde!)", "the mulberry trees in the Palm Gorges (silk!)"),
    "z6_hout_palmenbos": ("het palmenbos in De Palmenkloven", "the palm grove in the Palm Gorges"),
    "z6_hout_duinput": ("de palmen bij de Duinput in De Roze Duinen", "the palms by the Dune Well in the Pink Dunes"),
    "z6_hout_oase": ("de dadelpalmen van De Moederoase", "the date palms of the Mother Oasis"),
    "z6_mijn_zoutgrot": ("De Zingende Zoutgrot, onder De Zoutvlakte", "the Singing Salt Cave, under the Salt Flats"),
    "z6_mijn_zoutrots": ("de zoutrotsen bij De Moederoase", "the salt rocks by the Mother Oasis"),
    "z6_mijn_mesa": ("de tafelbergen van Het Ruïnedal", "the mesas of Ruin Valley"),
    "z6_vis_kanaal": ("de kanalen van de Kanaalwijk in Saffraanpoort", "the canals of the Canal Quarter in Saffron Gate"),
    "z6_vis_saffraanbrug": ("de Saffraanrivier onder de Saffraanbrug", "the Saffron River under the Saffron Bridge"),
    "z6_vis_oase": ("het meer van De Moederoase", "the lake of the Mother Oasis"),
    "z6_vis_zeemond": ("de Zeemond van Rozenkloof", "the Sea Mouth of Rose Gorge"),
    "z6_korf_paradijstuin": ("de Paradijstuin van Saffraanpoort", "the Paradise Garden of Saffron Gate"),
}
for _id, (_nl, _en) in GROEPEN.items():
    t(S + "groep." + _id, _nl, _en)


# ======================================================================================================================
# output
# ======================================================================================================================
def _assets():
    for _id, (s, k) in ICONEN.items():
        T._item(f"vr_amb_{_id}", U.SCHILDER[s](T._hex(k)))
    T._item(f"vr_amb_kern_{KERN[0]}", T.kern(T._hex(KERN[1])))
    for _id, (s, k, _n, _e) in ETEN.items():
        T._item(f"vr_amb_eten_{_id}", T.eten(s, T._hex(k)))
    for _id, (k, *_r) in DRANKEN.items():
        T._item(f"vr_amb_drank_{_id}", drankje(T._hex(k)))


def build(h):
    _assets()
    for k, nl in TEKSTEN.items():
        h.lang(k, EN[k], nl)   # (make_v2.lang(key, en, nl))


OVERLAY = os.path.join("tools", "lang", "en", "c482_vr_ambacht_t6.json")


def _direct():
    _assets()
    p = os.path.join(A, "lang", "nl_nl.json")
    with open(p, encoding="utf-8") as f:
        data = json.load(f)
    data.update(TEKSTEN)
    T._schrijf(p, data)
    T._schrijf(OVERLAY, dict(sorted(EN.items())))
    print(f"vadsrijk_ambacht_t6: {len(TEKSTEN)} texts; now run python tools/lang/apply_en.py --no-ftb")


if __name__ == "__main__":
    _direct()
