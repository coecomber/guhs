"""
bbq2 (tech-klusjes): the new chores of the Guhhuisje around the guh machines. Java: feature/techklus (the chores
"machines" and "plantage") plus the klusjes package it extends (the farmen chore harvests more, chore output goes into a
Hapluikje when no Bank Guh stands in the klus-area).

No blocks, items, models or textures of its own: this module writes the texts (the chores' names and tips, what the
overview "Wat kan hier?" says about them, the dagboek lines), the two block tags, the advancements and the game test
garden. A few texts of the huisje and of the old chores are written again here, because what they say has changed
(more crops, the Hapluikje): this module runs after `klusjes` and `huisje`, so its line wins.

No FTB quests (the chapter Guh-technologie is tech-quests'): the hidden advancements `quest/tech_klusjes_*` are its tasks.
"""
import os

from features import bbq2

# the machines the chore "machines" serves (tag guhs:techklus/machines): kern blocks with an item capability that takes
# things in through the top and gives them out through the bottom
MACHINES = ["guhs:guh_oven", "guhs:knutselmachine", "guhs:brouwautomaat", "guhs:frituurautomaat", "guhs:vadsmolen", "guhs:grillkoolpers",
            "guhs:oogster", "guhs:knabbelaar", "guhs:neerzetter", "guhs:opzuiger"]
# the little plants the farmen chore picks a scheutje from (tag guhs:klusjes/scheutjes)
SCHEUTJES = ["guhs:pindascheutjes", "guhs:mosterdscheutjes"]

KLUSSEN = {  # id: (name, tip of the huisje screen)
    "machines": ("Machines bijvullen & leeghalen",
                 "Haalt op wat er klaarligt in je guh-machines (Guh Oven, Knutselmachine, Brouwautomaat, Frituurautomaat, Vadsmolen, "
                 "Oogster...) en vult ze bij uit de kist. Bijvullen doet hij alleen met wat jij hebt voorgedaan: leg zelf één keer een "
                 "stapeltje in de machine, dan onthoudt hij wat erin hoort. Een Knutselmachine krijgt precies wat er op zijn "
                 "Bouwtekening staat. Dit klusje staat uit tot jij het aanzet. Njeg, daarna werkt het vanzelf! (Guhs)"),
    "plantage": ("Plantage: planten & hakken",
                 "Hakt met zijn bijltje de boom op een Plantagebak om en brengt het hout naar de kist. Is een bak leeg en zijn de "
                 "zaailingen op? Dan haalt hij nieuwe uit de kist. Alleen in Plantagebakken: andere bomen laat hij lekker staan. (Guhs)"),
}

P = "gui.guhs.huisje.overzicht.klus."
OVERZICHT = {  # what the overview "Wat kan hier?" says (KlusStand reden -> text; %s = the count)
    P + "machines.halen": "Machines waar iets klaarligt om op te halen: %s",
    P + "machines.vullen": "Machines om bij te vullen uit de kist: %s",
    P + "machines.geen_voorraad": "Machines die bijna leeg zijn: %s. Maar in de kist ligt niets meer van wat erin hoort",
    P + "machines.rustig": "Machines: %s. Nu valt er niets op te halen of bij te vullen",
    P + "machines.geen": "Geen guh-machines van jou",
    P + "machines.nodig": "een guh-machine die jij zelf hebt neergezet in de klus-area: Guh Oven, Knutselmachine, Brouwautomaat, "
                          "Frituurautomaat, Vadsmolen, Grillkoolpers, Oogster, Knabbelaar, Neerzetter of Opzuiger. Wat eruit komt halen "
                          "ze op. Bijvullen doen ze uit de kist naast het huisje of de Bank Guh, en alleen met wat jij hebt voorgedaan: "
                          "leg zelf één keer een stapeltje in de machine, dan onthouden ze per vakje wat erin hoort. Iets anders erin "
                          "leggen = iets anders onthouden. De Knutselmachine krijgt precies wat er op zijn Bouwtekening staat. Het "
                          "klusje staat bij elke bewoner uit tot jij het aanzet.",
    P + "plantage.bomen": "Bomen om te hakken op een Plantagebak: %s",
    P + "plantage.planten": "Lege Plantagebakken die een zaailing uit de kist krijgen: %s",
    P + "plantage.geen_zaailing": "Lege Plantagebakken: %s. Maar er liggen geen zaailingen in de kist",
    P + "plantage.geen_kracht": "Plantagebakken zonder vadskracht: %s. Zo groeit er niets, njeg",
    P + "plantage.groeit": "Nu nog niets te hakken. Plantagebakken waar een boom groeit: %s",
    P + "plantage.geen": "Geen Plantagebak van jou",
    P + "plantage.nodig": "een Plantagebak die jij zelf hebt neergezet in de klus-area, met vadskracht. Staat er een boom op, dan hakken "
                          "ze hem in één keer om; de bak houdt zelf een paar zaailingen en plant de volgende. Is een bak helemaal leeg, "
                          "dan halen ze zaailingen uit de kist naast het huisje of de Bank Guh (ook sate- en worstzwammetjes, "
                          "paddenstoelen en azalea).",
    # the farmen chore harvests more now: its texts say so (they were written by huisje.py / klusjes.py)
    P + "farmen.geen": "Geen gewassen, guhtuintjes, pompoenen, suikerriet of scheutjes",
    P + "farmen.nodig": "een akkertje met gewassen (tarwe, wortels, aardappels, bieten, kaasknabbelplantjes, pepers...) of een beplant "
                        "guhtuintje in de klus-area. Rijp wordt geoogst en opnieuw geplant, een dorstig tuintje krijgt water. Ook: "
                        "pompoenen en meloenen aan hun stengel (de stengel blijft), suikerriet (het onderste stuk blijft), rijpe cacao "
                        "waar ze bij kunnen, netherwrat, en van elk pinda- of mosterdscheutjesplantje één scheutje per 5 minuten (het "
                        "plantje blijft staan).",
    # the Hapluikje is storage too
    P + "opruimen.geen_opslag": "Geen kist, Bank Guh of Hapluikje om de spullen in te doen",
    P + "opruimen.nodig": "een kist naast het huisje, een Bank Guh in de klus-area of een werkend Hapluikje in de klus-area. Dan rapen ze "
                          "op wat er in de klus-area op de grond ligt.",
}

TEXTS = {
    "gui.guhs.klus.farmen.tip": "Oogst rijpe gewassen en plant ze meteen opnieuw: tarwe, wortels, aardappels, bieten, kaasknabbelplantjes, "
                                "pepers en guhtuintjes. Ook pompoenen, meloenen, suikerriet, cacao, netherwrat en pinda- en "
                                "mosterdscheutjes. Geeft dorstige tuintjes water. (Guhs)",
    "gui.guhs.huisje.opslag.luikje": "Spulletjes van klusjes gaan door het Hapluikje naar je Bank Guh. Hap!",
    "gui.guhs.huisje.overzicht.ding.hapluikje": "Staat er geen Bank Guh in de klus-area? Dan brengen je bewoners de spulletjes van hun "
                                                "klusjes naar een Hapluikje: dat hapt ze door naar zijn Bank Guh, hoe ver weg die ook "
                                                "staat. Hier telt alleen een luikje dat werkt: met vadskracht, en met de Banksleutel "
                                                "gekoppeld aan een Bank Guh die ergens staat. Is die bank vol, dan gaat de rest in de kist.",
    "gui.guhs.dagboek.eerste.klusjes_machines": "Machinist guh",
    "gui.guhs.dagboek.eerste.klusjes_machines.tekst": "Ik heb een machine eten gegeven. Hij keek blij. Daarna gaf hij mij iets terug. "
                                                     "Wij zijn nu collega's, njeg.",
    "gui.guhs.wistjedat.klusjes.machines": "Wist je dat de machines bij %s ook een snoet hebben? Ik geef ze te eten en zij maken er iets "
                                           "van. Zo doe ik het zelf ook, maar dan met knabbels.",
    "gui.guhs.dagboek.eerste.klusjes_plantage": "Houthakkertje guh",
    "gui.guhs.dagboek.eerste.klusjes_plantage.tekst": "Ik heb een hele boom omgehakt met mijn bijltje. In één keer! De boom zei krak. "
                                                     "Ik zei njeg. Daarna stond er alweer een nieuw boompje.",
    "gui.guhs.wistjedat.klusjes.plantage": "Wist je dat er bij %s een bos in een bak groeit? Ik hak de boom om en de bak maakt gewoon een "
                                           "nieuwe. Het is het kleinste bos van de wereld en het is van mij.",
}

ADV = [  # name, parent (in the tab techniek), icon, frame, title, description
    ("tech_klusjes_machines", "root", "guhs:guh_oven", "task", "Collega's met een snoet",
     "Laat een bewoner van je Guhhuisje een guh-machine bijvullen of leeghalen"),
    ("tech_klusjes_plantage", "tech_machines_boom", "minecraft:iron_axe", "task", "Houthakkertje guh",
     "Laat een bewoner de boom op een Plantagebak omhakken of er een zaailing in stoppen"),
    ("tech_klusjes_luikje", "bank_gehapt", "guhs:hapluikje", "task", "Hap, opgeruimd!",
     "Laat een bewoner de spulletjes van zijn klusje in een Hapluikje stoppen"),
    ("tech_klusjes_oogst", "root", "minecraft:pumpkin", "task", "Pompoenenplukker",
     "Laat een bewoner een pompoen, meloen, suikerriet, cacao, netherwrat of een scheutje oogsten"),
]


def tags(h):
    h.add_tag("guhs/tags/block/techklus/machines", MACHINES)
    h.add_tag("guhs/tags/block/klusjes/scheutjes", SCHEUTJES)


def texts(h):
    for k, (naam, tip) in KLUSSEN.items():
        h.lang(f"gui.guhs.klus.{k}", naam, naam)
        h.lang(f"gui.guhs.klus.{k}.tip", tip, tip)
    for table in (OVERZICHT, TEXTS):
        for key, nl in table.items():
            h.lang(key, nl, nl)


def advancements(h):
    for name, parent, icon, frame, titel, tekst in ADV:
        bbq2.verborgen(h, name)
        bbq2.zichtbaar(h, "techniek", name, None if parent == "root" else parent, icon, frame, titel, tekst)


def test_templates(h):
    # a garden like klusjes_test_tuin, but higher: the Hapluikje test puts its Bank Guh ABOVE the klus-area (9 blocks up),
    # and the plantage test grows a tree
    t = h.Structure((24, 20, 24))
    for x in range(24):
        for z in range(24):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("techklus_test_tuin")


def selfcheck(h):
    missing = []
    for k in KLUSSEN:
        for key in (f"gui.guhs.klus.{k}", f"gui.guhs.klus.{k}.tip", f"{P}{k}.nodig", f"{P}{k}.geen", f"gui.guhs.dagboek.eerste.klusjes_{k}",
                    f"gui.guhs.dagboek.eerste.klusjes_{k}.tekst", f"gui.guhs.wistjedat.klusjes.{k}"):
            if key not in h.NL:
                missing.append(key)
    # every reason the two chores can answer (MachineKlus.stand, PlantageKlus.stand)
    for key in ("machines.halen", "machines.vullen", "machines.geen_voorraad", "machines.rustig", "plantage.bomen", "plantage.planten",
                "plantage.geen_zaailing", "plantage.geen_kracht", "plantage.groeit"):
        if P + key not in h.NL:
            missing.append(P + key)
    for name, parent, *_ in ADV:
        for p in (f"{h.D}/advancement/quest/{name}.json", f"{h.D}/advancement/techniek/{name}.json"):
            if not os.path.exists(p):
                missing.append(p)
        if parent != "root" and not os.path.exists(f"{h.D}/advancement/techniek/{parent}.json"):
            missing.append(f"parent {parent}")
    for block in MACHINES + SCHEUTJES:
        if f"block.guhs.{block.split(':')[1]}" not in h.NL:
            missing.append(f"no such block: {block}")
    if not os.path.exists(f"{h.D}/structure/techklus_test_tuin.nbt"):
        missing.append("techklus_test_tuin.nbt")
    if missing:
        raise SystemExit(f"tech_klusjes: missing {missing}")


def build(h):
    tags(h)
    texts(h)
    advancements(h)
    test_templates(h)
    selfcheck(h)
