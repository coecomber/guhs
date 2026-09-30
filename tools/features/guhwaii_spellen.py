"""
Surfen & hula op Guhwai'i (3.0, slice guhwaii-spellen; DESIGN_30 §5): het surfstrand van Guhwai'i met Lilo-guh (surfen en hula),
de schelpjesmunt en Tikiguh's Tiki-kraampje.

  build(h)    the things (guhwaii_spellen_blokken.py: the Tiki decorations, the schelpjesmunt, the loaned surfplankje, the surf
              board entity, the wave texture, Tikiguh's model), the sounds (the three hula songs and the beach sounds, made by
              tools/remix/make_hula.py), all texts (Dutch in both languages), the advancements (tab Guhverhalen), the surf beach
              structure (guhwaii_spellen_bouw.py: a regio structure on the island's beach, with a geometry self-check) and the
              GameTest templates
  ftb(fq)     the quests of the section "Surfen & hula" (chapter Guhverhalen)
  no clothes (CONTRACT_30 §5.4: Tikiguh sells decorations).

Java side: nl.juiced.guhs.feature.guhwaiispellen (SurfSim/SurfGolven/SurfSpel, HulaLiedje/HulaKaart/HulaSpel, TikiWinkel, ...).
"""
import os

from features import guhwaii_spellen_blokken as blokken
from features import guhwaii_spellen_bouw as bouw
from features import sterrenwacht_hulp as hulp
from features import verhaal, verhaal_wereld

BONES = {}
CLOTHES = []
NAME = bouw.NAME
LIEDJES = {"aloha_njeg": "Aloha, Njeg", "guhla_hula_rock": "Guhla-Hula Rock", "vahoeg_hula_hop": "Vahoeg Hula Hop"}
GELUIDEN = {"ukelele_tokkel": "Ukelele: tokkel!", "golf_breekt": "Een golf breekt", "schelpje": "Schelpjesmunt: tinkel",
            "aloha": "Guh: Aloha!", "plons": "PLONS!"}


# =====================================================================================================================
# texts
# =====================================================================================================================
TEXTS = {
    # things
    "item.guhs.schelpjesmunt": "Schelpjesmunt",
    "item.guhs.schelpjesmunt.lore": "Verdiend met surfen en hula op Guhwai'i. Tikiguh ruilt ze voor mooie Tiki-spulletjes.",
    "item.guhs.surfplankje_leen": "Lilo's surfplankje",
    "item.guhs.surfplankje_leen.lore": "Met dit plankje surf je met Lilo-guh mee. Pas op voor het schuim, njeg!",
    "item.guhs.surfplankje_leen.loan": "Geleend van Lilo-guh: na het surfen gaat het terug naar haar hutje.",
    "entity.guhs.guhwaiispellen_surfplank": "Surfplankje",
    "block.guhs.tiki_fakkel": "Tiki-fakkel",
    "block.guhs.tiki_fakkel.lore": "Een bamboestok met een kokosnootschaaltje en een vrolijk vlammetje erin.",
    "block.guhs.tiki_masker": "Tiki-masker",
    "block.guhs.tiki_masker.lore": "Een gesneden guh-tiki met grote ogen en een brede grijns. Hang hem aan de muur!",
    "block.guhs.tiki_masker_roze": "Roze tiki-masker",
    "block.guhs.tiki_masker_roze.lore": "Roze geschilderd, met turquoise oortjes. Het vrolijkste masker van het strand.",
    "block.guhs.tiki_beeld": "Tiki-beeld",
    "block.guhs.tiki_beeld.lore": "Een guh-tiki op een gesneden lijfje met een bloem op zijn buik. Hij let op je huisje.",
    "block.guhs.tiki_rietdak": "Rieten dak",
    "block.guhs.tiki_rietdak.lore": "Dik strodak, zoals op Lilo's surfhut. Knisper!",
    "block.guhs.tiki_rietdak_trap": "Rieten daktrap",
    "block.guhs.tiki_rietdak_trap.lore": "Voor een schuin tiki-dakje.",
    "block.guhs.tiki_rietdak_plaat": "Rieten dakplaat",
    "block.guhs.tiki_rietdak_plaat.lore": "Een half laagje riet.",
    "block.guhs.tiki_bloemenslinger": "Bloemenslinger",
    "block.guhs.tiki_bloemenslinger.lore": "Een slinger van hibiscus en blaadjes, voor aan de muur. Meteen feest!",
    "block.guhs.tiki_schelpjeslampion": "Schelpjeslampion",
    "block.guhs.tiki_schelpjeslampion.lore": "Een lampje van roze schelpjes dat zachtjes gloeit.",
    "block.guhs.tiki_surfplankrek": "Surfplankenrek",
    "block.guhs.tiki_surfplankrek.lore": "Drie surfplankjes in een bamboerekje: roze, turquoise en geel.",
    "block.guhs.tiki_bloemenmat": "Hula-bloemenmat",
    "block.guhs.tiki_bloemenmat.lore": "Een gevlochten matje vol bloemen. Op het podium dans je hierop de hula!",
    "block.guhs.tiki_kruk": "Tiki-krukje",
    "block.guhs.tiki_kruk.lore": "Bamboe pootjes, een gevlochten zitje. Voor aan je eigen tiki-bar.",
    "block.guhs.tiki_radiootje": "Tiki-radiootje",
    "block.guhs.tiki_radiootje.lore": "Rechtsklik: de hula-liedjes van Guhwai'i, het ene na het andere (en dan weer uit).",
    "block.guhs.tiki_radiootje.speelt": "♪ Het radiootje speelt: %s",
    "block.guhs.tiki_radiootje.uit": "Het radiootje is stil. Sssst, njeg.",
    # Tikiguh
    "entity.guhs.guh_npc.tikiguh": "Tikiguh",
    "gui.guhs.guhdex.rarity.tikiguh": "Zeldzaamheid: uniek (één op elk surfstrand van Guhwai'i)",
    "gui.guhs.guhdex.info.tikiguh": "Niemand weet hoe Tikiguh er zonder zijn masker uitziet. Hij houdt het altijd voor zijn snoetje, aan een "
                                    "bamboestokje, met palmbladeren erbovenop. \"Een tiki moet je voelen, niet zien\", zegt hij dan. Hij "
                                    "verkoopt de mooiste tiki-spulletjes voor schelpjesmunten: fakkels, maskers, lampionnetjes en zelfs een "
                                    "radiootje met hula-muziek. Onder zijn grasrokje zit gewoon een vadsig guhbuikje. Njeg!",
    "structure.guhs.guhwaii_surfstrand": "Het surfstrand van Guhwai'i",
    "structure.guhs.guhwaii_surfstrand.tooltip": "Minigames: surfen en hula met Lilo-guh, schelpjesmunten en Tikiguh's Tiki-kraampje (Guhwai'i)",
    "gui.guhs.spelgroep.guhwaii_spellen.waar": "Op het strand van een eiland van Guhwai'i: het surfstrand, bij Lilo-guh. Aloha, njeg!",
    "gui.guhs.guhwaiispellen.niet_bouwen": "Njeg! Het surfstrand is van Lilo-guh en Tikiguh: hier mag je niks slopen of bouwen.",
    # --- surfing: Lilo-guh talks ---
    "quest.guhs.guhwaiispellen.surf.welkom": "Aloha! Ik ben Lilo-guh. Heb jij ooit gesurft? Nee? Dan leer ik het je! Ik leen je mijn plankje, "
                                            "en ik surf gewoon naast je. Ohana surft samen!",
    "quest.guhs.guhwaiispellen.surf.hallo1": "Aloha, njeg! De golven zijn vandaag VAHOEG. Zin in een ritje?",
    "quest.guhs.guhwaiispellen.surf.hallo2": "Kijk, daar komt een setje golven aan! Pak je plankje, snel!",
    "quest.guhs.guhwaiispellen.surf.hallo3": "Weet je wat het geheim is? Blijf voor het schuim. Het schuim is lief, maar het gooit je van je plankje.",
    "quest.guhs.guhwaiispellen.surf.hallo4": "Ik heb vanochtend drie knabbeldraaien gedaan! Nou ja, twee. Eentje was meer een plons.",
    "quest.guhs.guhwaiispellen.surf.bezig": "Je bent aan het surfen! Houd sluipen even ingedrukt als je terug naar het strand wilt peddelen.",
    "quest.guhs.guhwaiispellen.surf.geen_golven": "Njeg... ik zie hier nergens open water. Waar zijn de golven gebleven?",
    "quest.guhs.guhwaiispellen.surf.uitleg": "Surfen! Kijk naar het strand. A/D: langs het strand. Komt er een golf? Peddel met W! Op de golf: "
                                            "W de golf af (vaart!), S omhoog naar de kam, spatie: springen bij de kam. In de lucht: A/D draaien, "
                                            "S je plankje grijpen. Blijf voor het schuim! Stoppen: houd sluipen in.",
    "quest.guhs.guhwaiispellen.surf.plons1": "Oei, PLONS! Geeft niks, klim er maar weer op. Ohana laat niemand achter!",
    "quest.guhs.guhwaiispellen.surf.plons2": "Njeg, nat! Dat hoort erbij. Volgende golf!",
    "quest.guhs.guhwaiispellen.surf.plons3": "Hihi, je zag eruit als een kaasknabbel in de soep. Nog een keer!",
    "quest.guhs.guhwaiispellen.surf.plons4": "Plonsje! Zelfs de beste surfguhs gaan weleens kopje onder.",
    "quest.guhs.guhwaiispellen.surf.eerste": "Je allereerste keer surfen! Hier: schelpjesmunten en een surfplankenrek voor thuis. Aloha, vahoeg!",
    "quest.guhs.guhwaiispellen.surf.uitslag": "≈ Surfen klaar: %s punten (%s), +%s schelpjesmunten. Golven uitgesurft: %s, trucs: %s ≈",
    "quest.guhs.guhwaiispellen.record": "Nieuw record: %s punten! (was %s)",
    "quest.guhs.guhwaiispellen.record_eerste": "Je eerste score: %s punten. Meteen je record, njeg!",
    # --- surfing: the panel ---
    "gui.guhs.guhwaiispellen.surf.titel": "Surfen!",
    "gui.guhs.guhwaiispellen.surf.titel.sub": "%s - kijk naar het strand, de golven komen eraan",
    "gui.guhs.guhwaiispellen.surf.sneak": "Blijf sluipen om terug naar het strand te peddelen...",
    "gui.guhs.guhwaiispellen.surf.paneel": "≈ Surfen (%s) - golf %s van %s ≈",
    "gui.guhs.guhwaiispellen.surf.record": "Record: %s",
    "gui.guhs.guhwaiispellen.surf.vang": "Golf gevangen! Surfen maar!",
    "gui.guhs.guhwaiispellen.surf.mis": "Mis! Wacht op de volgende golf...",
    "gui.guhs.guhwaiispellen.surf.schuim": "Oei, het schuim rolt over je heen!",
    "gui.guhs.guhwaiispellen.surf.tube_in": "IN DE TUBE! Blijf erin!",
    "gui.guhs.guhwaiispellen.surf.plons.schuim": "PLONS! Het schuim had je te pakken...",
    "gui.guhs.guhwaiispellen.surf.plons.scheef": "PLONS! Scheef geland, njeg...",
    "gui.guhs.guhwaiispellen.surf.golf_strand": "Tot op het strand, VAHOEG!",
    "gui.guhs.guhwaiispellen.surf.golf_uit": "Golf uitgesurft!",
    "gui.guhs.guhwaiispellen.surf.klaar": "De golven zijn op. Aloha!",
    "gui.guhs.guhwaiispellen.surf.hint.wacht": "Wacht op een golf... (A/D langs het strand)",
    "gui.guhs.guhwaiispellen.surf.hint.nu": "NU! Peddel met W!",
    "gui.guhs.guhwaiispellen.surf.hint.komt": "Hij komt! Houd je vast...",
    "gui.guhs.guhwaiispellen.surf.hint.rijden": "W: de golf af (vaart)  S: omhoog naar de kam  spatie: springen",
    "gui.guhs.guhwaiispellen.surf.hint.tube": "In de tube! Blijf net voor het schuim, hoog op de golf!",
    "gui.guhs.guhwaiispellen.surf.hint.lucht": "Draai met A/D, grijp met S! (%s°)",
    "gui.guhs.guhwaiispellen.surf.hint.plons": "Pfff... proest... even boven komen...",
    "gui.guhs.guhwaiispellen.surf.hint.terug": "Terug naar de golven peddelen...",
    "gui.guhs.guhwaiispellen.surf.hint.stoppen": "(sluipen ingedrukt houden: terug naar het strand)",
    "gui.guhs.guhwaiispellen.surf.einde": "Surfen klaar: %s punten (%s)",
    "gui.guhs.guhwaiispellen.nieuw_record": "Nieuw record! VAHOEG!",
    "gui.guhs.guhwaiispellen.munten": "+%s schelpjesmunten",
    "gui.guhs.guhwaiispellen.truc.cutback": "Knabbel-cutback!",
    "gui.guhs.guhwaiispellen.truc.hop": "Guh-hop!",
    "gui.guhs.guhwaiispellen.truc.draai": "Knabbeldraai!",
    "gui.guhs.guhwaiispellen.truc.dubbel": "Dubbele knabbeldraai!!",
    "gui.guhs.guhwaiispellen.truc.drie": "DRIEDUBBELE KNABBELDRAAI!!!",
    "gui.guhs.guhwaiispellen.truc.hop_grab": "Guh-hop met njeg-grab!",
    "gui.guhs.guhwaiispellen.truc.draai_grab": "Knabbeldraai met njeg-grab!",
    "gui.guhs.guhwaiispellen.truc.dubbel_grab": "Dubbele knabbeldraai met njeg-grab!!",
    "gui.guhs.guhwaiispellen.truc.drie_grab": "DRIEDUBBEL MET NJEG-GRAB!!!",
    "gui.guhs.guhwaiispellen.truc.tube": "Door de tube, VAHOEG!",
    # --- the hula ---
    "quest.guhs.guhwaiispellen.hula.welkom": "Aloha! Kom je dansen? Bij de hula praat je met je heupjes. Ik doe het voor, jij doet het na, "
                                            "precies op de maat van de muziek. Njeg-njeg!",
    "quest.guhs.guhwaiispellen.hula.hallo1": "Aloha, njeg! De ukelele is gestemd, de bloemen liggen klaar. Dansen?",
    "quest.guhs.guhwaiispellen.hula.hallo2": "Heupjes links, heupjes rechts, en dan... VAHOEG! Zo simpel is het!",
    "quest.guhs.guhwaiispellen.hula.hallo3": "Mijn zus Nani-guh zegt dat ik te veel dans. Maar een guh kan nooit te veel dansen!",
    "quest.guhs.guhwaiispellen.hula.hallo4": "Dit liedje zong een guh met een grote kuif ooit op het strand. Iedereen viel flauw. Van blijdschap!",
    "quest.guhs.guhwaiispellen.hula.bezig": "Je danst! Kijk naar de stapjes die aan komen schuiven, en druk precies op de maat.",
    "quest.guhs.guhwaiispellen.hula.druk": "%s danst nu met mij. Kijk maar mee, en dans straks zelf!",
    "quest.guhs.guhwaiispellen.hula.geen_mat": "Njeg... waar is mijn bloemenmat? Zonder mat kan ik niet dansen.",
    "quest.guhs.guhwaiispellen.hula.uitleg.makkelijk": "Hula! De stapjes schuiven van rechts naar de bloem. Druk als ze erop zijn: A heupjes links, "
                                                       "D heupjes rechts. Langzaam en lief, precies op de maat!",
    "quest.guhs.guhwaiispellen.hula.uitleg.medium": "Hula! Druk als een stapje op de bloem is: A heupjes links, D heupjes rechts, W armen omhoog, "
                                                    "S door de knietjes. Op de maat van de rock'n'roll!",
    "quest.guhs.guhwaiispellen.hula.uitleg.lastig": "Hula, snel! A links, D rechts, W armen omhoog, S door de knietjes, en bij een gouden ster: "
                                                    "spatie, VAHOEG! Er komen ook halve tellen aan.",
    "quest.guhs.guhwaiispellen.hula.weg": "Je stapte van het podium. De muziek stopt... njeg.",
    "quest.guhs.guhwaiispellen.hula.uitslag": "✿ Hula klaar: %s punten (%s), +%s schelpjesmunten. VAHOEG! %s, Njeg! %s, Guh. %s, mis %s, "
                                              "langste reeks %s ✿",
    "quest.guhs.guhwaiispellen.hula.foutloos": "GEEN ENKEL STAPJE MIS! Jij bent een echte hula-guh! Ik krijg er tranen van. Blije tranen!",
    "quest.guhs.guhwaiispellen.hula.klaar1": "Mooi gedanst! Je heupjes waren helemaal vahoeg.",
    "quest.guhs.guhwaiispellen.hula.klaar2": "Aloha! Nog een liedje? Oefening maakt de heupjes los.",
    "quest.guhs.guhwaiispellen.hula.klaar3": "Hihi, jij danst net als mijn zus: met je tong uit je mond. Heel knap!",
    "quest.guhs.guhwaiispellen.hula.eerste": "Je eerste hula! Hier: schelpjesmunten en twee bloemenmatjes, om thuis verder te oefenen. Aloha!",
    "gui.guhs.guhwaiispellen.hula.titel.sub": "%s - %s BPM, op de maat!",
    "gui.guhs.guhwaiispellen.hula.oordeel.vahoeg": "VAHOEG!",
    "gui.guhs.guhwaiispellen.hula.oordeel.njeg": "Njeg!",
    "gui.guhs.guhwaiispellen.hula.oordeel.guh": "Guh.",
    "gui.guhs.guhwaiispellen.hula.oordeel.mis": "Mis",
    "gui.guhs.guhwaiispellen.hula.einde": "Hula klaar: %s punten, langste reeks %s",
    "gui.guhs.guhwaiispellen.hula.telling": "VAHOEG! %s  Njeg! %s  Guh. %s  Mis %s",
    "gui.guhs.guhwaiispellen.hula.score": "Punten: %s",
    "gui.guhs.guhwaiispellen.hula.combo": "Reeks: %s",
    "gui.guhs.guhwaiispellen.hula.record": "Record: %s",
    "gui.guhs.guhwaiispellen.hula.klaar_makkelijk": "Klaar? A en D, op de maat... heupjes los!",
    "gui.guhs.guhwaiispellen.hula.klaar_medium": "Klaar? A, D, W en S, op de maat van de rock'n'roll!",
    "gui.guhs.guhwaiispellen.hula.klaar_lastig": "Klaar? Snel nu, en spatie bij de gouden sterren: VAHOEG!",
    # --- Lilo-guh's screen ---
    "gui.guhs.guhwaiispellen.surf.scherm": "Lilo-guh's surfschool",
    "gui.guhs.guhwaiispellen.hula.scherm": "Hula met Lilo-guh",
    "gui.guhs.guhwaiispellen.surf.vraag": "Surfen? Ik leen je mijn plankje en we peddelen samen naar de golven. Vang ze, surf ze uit, doe trucs "
                                         "van de kam en blijf voor het schuim. Hoe meer punten, hoe meer schelpjesmunten!",
    "gui.guhs.guhwaiispellen.hula.vraag": "Hula dansen? Kies een liedje. Ik doe de stapjes voor, jij drukt ze precies op de maat na. Hoe mooier je "
                                         "danst, hoe meer schelpjesmunten!",
    "gui.guhs.guhwaiispellen.surf.mine": "Je bent aan het surfen! Wil je terug naar het strand?",
    "gui.guhs.guhwaiispellen.hula.mine": "Je bent aan het dansen! Wil je stoppen?",
    "gui.guhs.guhwaiispellen.hula.druk": "%s danst nu de hula. Kijk mee, straks is het jouw beurt!",
    "gui.guhs.guhwaiispellen.surf.stop": "Terug naar het strand",
    "gui.guhs.guhwaiispellen.surf.stop.tooltip": "Je surfpartijtje stopt; je punten tellen wel",
    "gui.guhs.guhwaiispellen.hula.stop": "Stoppen met dansen",
    "gui.guhs.guhwaiispellen.hula.stop.tooltip": "Het liedje stopt; wat je al danste telt",
    "gui.guhs.guhwaiispellen.surf.kies": "Welke golven?",
    "gui.guhs.guhwaiispellen.hula.kies": "Welk liedje?",
    "gui.guhs.guhwaiispellen.surf.niveau.makkelijk": "Kleine, trage golfjes. Je vangt ze vanzelf. Geen tube.",
    "gui.guhs.guhwaiispellen.surf.niveau.medium": "Mooie golven. Peddel ze in met W. Naast het schuim krult een tube!",
    "gui.guhs.guhwaiispellen.surf.niveau.lastig": "Grote, snelle golven die snel breken. Precies landen!",
    "gui.guhs.guhwaiispellen.hula.niveau.makkelijk": "Een langzaam maanlicht-liedje (%s BPM). Alleen je heupjes: A en D.",
    "gui.guhs.guhwaiispellen.hula.niveau.medium": "Rock'n'roll op het strand (%s BPM). A, D, W en S.",
    "gui.guhs.guhwaiispellen.hula.niveau.lastig": "Surfrock! (%s BPM) Halve tellen, en spatie bij de VAHOEG!-sterren.",
    "gui.guhs.guhwaiispellen.jouw_best": "Jouw record: %s punten",
    "gui.guhs.guhwaiispellen.nog_geen_best": "Nog geen record",
    "gui.guhs.guhwaiispellen.strand_best": "Strandrecord: %s",
    "gui.guhs.guhwaiispellen.strand_best_geen": "Strandrecord: nog niemand!",
    "gui.guhs.guhwaiispellen.best_kort": "record %s",
    "gui.guhs.guhwaiispellen.surf.toetsen": "Kijk naar het strand. A/D: langs het strand. W: peddelen / de golf af. S: naar de kam. Spatie: springen. "
                                           "In de lucht: A/D draaien, S grijpen.",
    "gui.guhs.guhwaiispellen.hula.toetsen": "A heupjes links, D heupjes rechts, W armen omhoog, S door de knietjes, spatie VAHOEG!",
    "gui.guhs.guhwaiispellen.munten_nu": "Je hebt %s schelpjesmunten. Tikiguh ruilt ze voor tiki-spulletjes!",
    "gui.guhs.guhwaiispellen.eerste": "Je eerste keer? Dan krijg je er een cadeautje bij!",
    # --- scoreboards ---
    "gui.guhs.scorebord.guhwaiispellen.surfen": "Surfen op Guhwai'i: top 3",
    "gui.guhs.scorebord.guhwaiispellen.surf": "Golven: %s",
    "gui.guhs.scorebord.guhwaiispellen.hula": "Hula met Lilo-guh: top 3",
    "gui.guhs.scorebord.guhwaiispellen.lied": "%s (%s)",
    # --- Tikiguh ---
    "quest.guhs.guhwaiispellen.tiki.blut": "Aloha! Geen schelpjesmunten? Ga surfen of dansen bij Lilo-guh, dan kom je rijk terug. Kijken mag altijd!",
    "quest.guhs.guhwaiispellen.tiki.hallo1": "Aloha, njeg! %s schelpjesmunten? Daar kun je mooie dingen voor krijgen!",
    "quest.guhs.guhwaiispellen.tiki.hallo2": "Welkom bij mijn kraampje! Alles met de hand gesneden. Nou ja, met de poot. (%s schelpjes op zak)",
    "quest.guhs.guhwaiispellen.tiki.hallo3": "Nee, ik doe mijn masker niet af. Een tiki moet je voelen, niet zien. (%s schelpjes)",
    "quest.guhs.guhwaiispellen.tiki.hallo4": "Het radiootje is mijn lievelingetje. Het speelt de hula-liedjes, ook thuis! (%s schelpjes)",
}
for _id, _naam in LIEDJES.items():
    TEXTS[f"gui.guhs.guhwaiispellen.lied.{_id}"] = _naam
for _id, _sub in GELUIDEN.items():
    TEXTS[f"subtitles.guhs.guhwaiispellen.{_id}"] = _sub


def texts(h):
    for key, text in TEXTS.items():
        h.lang(key, text, text)


# =====================================================================================================================
# sounds: the three hula songs (streamed) and the beach sounds (tools/remix/make_hula.py writes the .ogg files)
# =====================================================================================================================
def sounds(h):
    for name in list(LIEDJES) + list(GELUIDEN):
        if not os.path.exists(f"{h.A}/sounds/guhwaiispellen/{name}.ogg"):
            raise SystemExit(f"guhwaii_spellen: sounds/guhwaiispellen/{name}.ogg is missing - run python tools/remix/make_hula.py")

    def patch(d):
        for lied in LIEDJES:
            d[f"guhwaiispellen.hula_{lied}"] = {"sounds": [{"name": f"guhs:guhwaiispellen/{lied}", "stream": True, "attenuation_distance": 48}]}
        for g in GELUIDEN:
            d[f"guhwaiispellen.{g}"] = {"sounds": [{"name": f"guhs:guhwaiispellen/{g}", "attenuation_distance": 32 if g == "golf_breekt" else 16}],
                                        "subtitle": f"subtitles.guhs.guhwaiispellen.{g}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# advancements (tab Guhverhalen) and their hidden twins for the FTB quests
# =====================================================================================================================
TIKI_ITEMS = [f"guhs:{n}" for n in blokken.TIKI]
ADVANCEMENTS = [  # name, parent, icon, frame, criteria, title, description
    ("guhwaii_spellen_strand", "root", "guhs:tiki_masker", "task", hulp.in_structure(NAME),
     "Aloha, surfstrand!", "Vind het surfstrand van Guhwai'i: surfen en hula met Lilo-guh, en Tikiguh's Tiki-kraampje"),
    ("guhwaii_spellen_surf", "guhwaii_spellen_strand", "guhs:surfplankje_leen", "task", None,
     "Golfje gevangen, njeg!", "Surf met Lilo-guh een golf helemaal uit"),
    ("guhwaii_spellen_knabbeldraai", "guhwaii_spellen_surf", "guhs:schelpjesmunt", "task", None,
     "Knabbeldraai!", "Spring van de kam van een golf en draai een heel rondje in de lucht voor je landt"),
    ("guhwaii_spellen_tube", "guhwaii_spellen_knabbeldraai", "minecraft:heart_of_the_sea", "goal", None,
     "Door de tube, VAHOEG!", "Surf een tijdje in de krul van een brekende golf, net voor het schuim (medium of lastig)"),
    ("guhwaii_spellen_surf_lastig", "guhwaii_spellen_tube", "minecraft:trident", "challenge", None,
     "Koning van de golven", "Surf 6000 punten of meer op lastig"),
    ("guhwaii_spellen_hula", "guhwaii_spellen_strand", "guhs:tiki_bloemenmat", "task", None,
     "Heupjes los!", "Dans een hula-liedje met Lilo-guh op het podium"),
    ("guhwaii_spellen_hula_foutloos", "guhwaii_spellen_hula", "guhs:tiki_bloemenslinger", "challenge", None,
     "Geen stapje mis", "Dans een heel liedje op medium of lastig zonder één stapje te missen"),
    ("guhwaii_spellen_alle_niveaus", "guhwaii_spellen_hula", "guhs:tiki_beeld", "goal", None,
     "Makkelijk, medium, lastig!", "Surf en dans op alle drie de niveaus (zes rijen in je Guhdex)"),
    ("guhwaii_spellen_tiki", "guhwaii_spellen_strand", "guhs:tiki_fakkel", "task",
     {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": TIKI_ITEMS}]}}},
     "Tiki-tiki-njeg", "Koop iets moois bij Tikiguh's Tiki-kraampje"),
    ("guhwaii_spellen_radiootje", "guhwaii_spellen_tiki", "guhs:tiki_radiootje", "goal",
     {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:tiki_radiootje"}]}}},
     "Hula aan huis", "Koop het Tiki-radiootje: de hula-liedjes van Guhwai'i, ook thuis"),
]
HIDDEN = ["seen_tikiguh", "guhwaii_spellen_surf", "guhwaii_spellen_knabbeldraai", "guhwaii_spellen_tube", "guhwaii_spellen_surf_lastig", "guhwaii_spellen_hula",
          "guhwaii_spellen_hula_foutloos", "guhwaii_spellen_alle_niveaus"] + \
         [f"guhwaii_spellen_{spel}_{n}" for spel in ("surf", "hula") for n in ("makkelijk", "medium", "lastig")]


def advancements(h):
    for name, parent, icon, frame, crit, title, desc in ADVANCEMENTS:
        verhaal.zichtbaar(h, "verhalen", name, parent, icon, frame, title, desc, criteria=crit)
    hulp.quest_advancements(h, HIDDEN)


# =====================================================================================================================
# the surf beach (a regio structure on the island's beach) and the GameTest templates
# =====================================================================================================================
def structuur(h):
    s = bouw.build(h)
    n = bouw.check(s)
    s.save(NAME)
    h.w(f"{h.D}/worldgen/template_pool/{NAME}/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{NAME}",
                                  "projection": "rigid", "processors": "guhs:guhwaiispellen_fundering"}}]})
    # (the Java FunderingProcessor fills sandstone under the bottom layer down to the ground: the shore drops to the lagoon)
    h.w(f"{h.D}/worldgen/processor_list/guhwaiispellen_fundering.json", {"processors": [{"processor_type": "guhs:guhwaiispellen_fundering"}]})
    verhaal_wereld.regio_structuur(h, NAME, f"guhs:{NAME}/start", "guhwaii", "kust", 20300203, hoek=180, afstand=0, reach=40,
                                   voorrang=800, grond_y=bouw.G)
    return n


def test_templates(h):
    # a sandy floor with the hula flower mat (the dance test) and room for a surf board
    s = h.Structure((13, 5, 13))
    for x in range(13):
        for z in range(13):
            s.set(x, 0, z, "minecraft:sand")
    s.set(6, 1, 6, "guhs:tiki_bloemenmat")
    s.save("guhwaiispellen_test_strand")
    # a beach with a channel of water going east (the surf spot search)
    s = h.Structure((32, 4, 11))
    for x in range(32):
        for z in range(11):
            s.set(x, 0, z, "minecraft:sandstone")
            if x >= 6 and 1 <= z <= 9:
                s.set(x, 1, z, "minecraft:water", {"level": "0"})
            else:
                s.set(x, 1, z, "minecraft:sand")
    s.save("guhwaiispellen_test_water")


# =====================================================================================================================
def build(h):
    blokken.build(h)
    sounds(h)
    texts(h)
    advancements(h)
    n = structuur(h)
    test_templates(h)
    selfcheck(h)
    print(f"guhwaii_spellen: surfstrand geometry check ok ({n} walkable spots), {len(blokken.TIKI)} tiki things, {len(LIEDJES)} hula songs")


def selfcheck(h):
    missing = [f for f in ("geo/entity/guh_npc_tikiguh.geo.json", "geo/entity/guhwaiispellen_surfplank.geo.json", "textures/entity/npc_tikiguh.png",
                           "textures/entity/guhwaiispellen_surfplank.png", "textures/misc/guhwaiispellen_golf.png", "textures/item/schelpjesmunt.png")
               if not os.path.exists(f"{h.A}/{f}")]
    for n in blokken.TIKI:
        for f in (f"blockstates/{n}.json", f"models/item/{n}.json"):
            if not os.path.exists(f"{h.A}/{f}"):
                missing.append(f)
        for key in (f"block.guhs.{n}", f"block.guhs.{n}.lore"):
            if key not in h.NL:
                missing.append(key)
    for key in ("quest.guhs.guhwaiispellen.hula.uitslag", "gui.guhs.guhwaiispellen.truc.drie_grab", "entity.guhs.guh_npc.tikiguh"):
        if key not in h.NL:
            missing.append(key)
    if missing:
        raise SystemExit(f"guhwaii_spellen assets missing: {missing}")


# =====================================================================================================================
def ftb(fq):
    q = fq.q
    q("guhwaii_spellen_strand", "Het surfstrand", "Op het strand van een eiland van &dGuhwai'i&r ligt soms een zonnig surfstrand: een rond "
      "hula-podium met fakkels, &6Lilo-guh&r bij haar surfhut en &6Tikiguh&r achter zijn Tiki-kraampje (superkompas: Verhalen of Minigames).",
      "guhs:tiki_masker", [fq.structure(NAME)], rewards=(("guhs:kaas_knabbels", 8),), shape="circle", xp=100)
    q("guhwaii_spellen_tikiguh", "Achter het masker", "Zet &6Tikiguh&r in je Guhdex (kom dichtbij genoeg). Hoe zou hij er zonder masker "
      "uitzien?", "guhs:tiki_masker_roze", [fq.adv("seen_tikiguh")], rewards=(("guhs:schelpjesmunt", 1),), xp=50)
    for n, titel, uitleg, beloning in (("makkelijk", "Eerste golfjes", "Surf met Lilo-guh op &amakkelijk&r: kleine golfjes die je vanzelf "
                                                       "vangt. Blijf voor het schuim!", 2),
                                       ("medium", "Peddelen maar!", "Surf op &emedium&r: peddel de golf in met &6W&r als hij bij je is. "
                                                   "Pump met W en S voor vaart!", 3),
                                       ("lastig", "Grote golven", "Surf op &clastig&r: grote, snelle golven die snel breken. Land je trucs recht!",
                                        4)):
        q(f"guhwaii_spellen_surf_{n}", titel, uitleg, "guhs:surfplankje_leen", [fq.adv(f"guhwaii_spellen_surf_{n}")],
          rewards=(("guhs:schelpjesmunt", beloning),), xp=100 + 50 * beloning)
    q("guhwaii_spellen_knabbeldraai", "Knabbeldraai!", "Spring met &6spatie&r van de kam van een golf (met genoeg vaart) en draai met &6A/D&r "
      "een heel rondje voor je landt. Grijp je plankje met &6S&r voor extra punten!", "guhs:schelpjesmunt",
      [fq.adv("guhwaii_spellen_knabbeldraai")], rewards=(("guhs:schelpjesmunt", 3),), xp=200)
    q("guhwaii_spellen_tube", "Door de tube", "Op medium en lastig krult de golf naast het schuim om: blijf hoog op de golf, net voor het "
      "schuim, en surf door de &dtube&r!", "minecraft:heart_of_the_sea", [fq.adv("guhwaii_spellen_tube")],
      rewards=(("guhs:schelpjesmunt", 4),), shape="hexagon", xp=300)
    for n, titel, uitleg, beloning in (("makkelijk", "Aloha, Njeg", "Dans de hula op &aAloha, Njeg&r: een langzaam maanlicht-liedje, alleen "
                                                    "je heupjes (&6A&r en &6D&r).", 2),
                                       ("medium", "Guhla-Hula Rock", "Dans op &eGuhla-Hula Rock&r: heupjes, armen omhoog (&6W&r) en door de "
                                                  "knietjes (&6S&r), op de maat van de rock'n'roll!", 3),
                                       ("lastig", "Vahoeg Hula Hop", "Dans op &cVahoeg Hula Hop&r: snel, met halve tellen, en &6spatie&r bij "
                                                  "de gouden VAHOEG!-sterren.", 4)):
        q(f"guhwaii_spellen_hula_{n}", titel, uitleg, "guhs:tiki_bloemenmat", [fq.adv(f"guhwaii_spellen_hula_{n}")],
          rewards=(("guhs:schelpjesmunt", beloning),), xp=100 + 50 * beloning)
    q("guhwaii_spellen_foutloos", "Geen stapje mis", "Dans een heel liedje op medium of lastig zonder één stapje te missen. Lilo-guh "
      "krijgt er blije tranen van!", "guhs:tiki_bloemenslinger", [fq.adv("guhwaii_spellen_hula_foutloos")],
      rewards=(("guhs:schelpjesmunt", 6),), shape="hexagon", xp=400)
    q("guhwaii_spellen_alle", "Makkelijk, medium, lastig!", "Surf én dans op alle drie de niveaus: zes rijen in de Minigames-tab van je "
      "Guhdex.", "guhs:tiki_beeld", [fq.adv("guhwaii_spellen_alle_niveaus")], rewards=(("guhs:schelpjesmunt", 5),), shape="gear", xp=300)
    q("guhwaii_spellen_schelpjes", "Een zakje vol schelpjes", "Spaar 20 &dschelpjesmunten&r bij elkaar. Ze rinkelen zo gezellig!",
      "guhs:schelpjesmunt", [fq.item("guhs:schelpjesmunt", 20)], rewards=(("guhs:kaas_knabbels", 16),), xp=150)
    q("guhwaii_spellen_tiki", "Tiki-tiki-njeg", "Koop iets moois bij &6Tikiguh&r: fakkels, maskers, een tiki-beeld, rieten daken, "
      "bloemenslingers, schelpjeslampionnen...", "guhs:tiki_fakkel", [fq.adv("guhs:verhalen/guhwaii_spellen_tiki")],
      rewards=(("guhs:kaas_knabbels", 8),), xp=100)
    q("guhwaii_spellen_radiootje", "Hula aan huis", "Koop het &6Tiki-radiootje&r (10 schelpjesmunten). Rechtsklik: de hula-liedjes van "
      "Guhwai'i spelen, ook thuis!", "guhs:tiki_radiootje", [fq.item("guhs:tiki_radiootje")], rewards=(("guhs:schelpjesmunt", 2),),
      shape="rsquare", xp=200)
