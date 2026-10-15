"""
bbq2 (ring-h1) - every Dutch text of chapter 1 of the Knabbelring, "Een langverwacht knabbelfeest" (the Knabbelgouw).
English: phase 3 (tools/lang/en); proposals for the proper names are in guhs_work130/reports/slice_ring-h1.md.
"""

# --- the signs in the templates (sign_text keys sign.guhs.ring_h1.*) -----------------------------------------------------------
BORDEN = {
    "welkom": ["~ De Knabbelgouw ~", "Hier gebeurt nooit", "iets. En dat is", "prima zo, njeg."],
    "eind": ["Knabbel-eind", "Geen toegang,", "behalve voor", "feestgangers!"],
    "eind_binnen": ["Tweede ontbijt:", "10 uur. Elfuurtje:", "11 uur. Daarna", "gewoon doorgaan."],
    "sam": ["Sam-guh", "Hovenier", "Aardappels te", "koop (bijna op)"],
    "sam_binnen": ["Kook ze, stamp ze,", "stop ze in een", "stoofpotje.", "- Sam-guh"],
    "moestuin": ["Sam-guhs", "moestuin", "Afblijven,", "meneer Guhdalf!"],
}

STRUCTUUR = ("Knabbelgouw", "De grote barbecueput van de Grillguh met de heuvelholletjes van de Knabbelgouw eromheen. Hier begint "
             "het verhaal van de Knabbelring (Guhmensie)")

# --- the questline (Guhdex tab Verhalen, the objective line, what Sam-guh and Guhdalf say) -----------------------------------
LIJN_NAAM = "Een langverwacht knabbelfeest"
LIJN_UITLEG = ("Hoofdstuk 1 van de Knabbelring. Guhdalf heeft het grillportaal uitgezet tot jij hem helpt. Maar in de Knabbelgouw gaat "
               "niemand op reis zonder afscheidsfeest, njeg.")
WAAR = "Bij Guhdalfs kar, naast de grote barbecueput in de Guhmensie"
STAPPEN = [
    ("Zoek Guhdalf", "Zoek Guhdalf. Hij staat met zijn kar vol vuurwerk bij de grote barbecueput in de Guhmensie. Praat met hem.",
     "Bij de grote barbecueput in de Guhmensie (het Superkompas wijst naar 'Mijn verhaal')"),
    ("Help met het feest", "Help Guhdalf met het afscheidsfeest: steek een vuurpijl af uit de kist op zijn kar, dek de feesttafel "
     "en nodig Sam-guh uit.", WAAR),
    ("Het afscheidsfeest", "Alles staat klaar. Praat met Guhdalf: het feest kan beginnen, vahoeg!", WAAR),
    ("Vraag Sam-guh mee", "Je hebt de Knabbelring. Niet opeten! Praat met Sam-guh: zonder hem (en zijn pannen) kom je nergens.",
     "Sam-guh staat in zijn moestuin, of bij Guhdalfs tent"),
    ("Pak proviand in", "Pak samen met Sam-guh de proviand in: de worst, de kaas en de knabbels uit de kratten bij Guhdalfs kar.", WAAR),
    ("Samen naar het portaal", "Loop met Sam-guh naar het grillportaal van de barbecueput. Daar begint de reis echt.",
     "Het grillkoolframe van de grote barbecueput (of een ander grillportaal)"),
]
KLAAR = ("Het grillportaal doet het weer en Sam-guh loopt met je mee. Op naar de Guhbarbecuether: Guhvendel wacht, njeg!", WAAR)
KORT = {
    "0": "Praat met Guhdalf bij de grote barbecueput",
    "1": "Help met het feest: vuurpijl, feesttafel, Sam-guh",
    "2": "Praat met Guhdalf: het feest begint",
    "3": "Vraag Sam-guh mee op reis",
    "4": "Pak de proviand in: worst, kaas, knabbels",
    "5": "Loop met Sam-guh naar het grillportaal",
}

# --- the narrator card and the two scenes ------------------------------------------------------------------------------------
KAART_TITEL = "Een langverwacht knabbelfeest"
KAART_REGELS = [
    "In een hol in de grond woonde een guh. Geen vies, nat hol: een heuvelholletje, en dat betekent knabbels.",
    "In de Knabbelgouw gebeurt nooit iets. Ze eten zes keer per dag en vinden avonturen maar vervelend: daar kom je te laat van voor het eten.",
    "Maar vandaag ratelt er een kar vol vuurwerk over het pad. Guhdalf de Grijze is er. En hij heeft iets in zijn zak dat héél lekker ruikt.",
    "Zo begint het verhaal van de Knabbelring. Njeg.",
]

SCENE_AANKOMST = ("Guhdalf komt aan", {
    "verteller": "De Knabbelgouw. Hier gebeurt nooit iets. En dat vinden ze prima.",
    "sam_laat": "Meneer Guhdalf! U bent te laat. Het tweede ontbijt is al op, njeg.",
    "nooit_te_laat": "Een tovenaar komt nooit te laat, Sam-guh. Hij komt precies wanneer de knabbels klaar zijn.",
    "portaal": "En jij wou door het grillportaal, hè? Dat heb ik even uitgezet.",
    "ring": "Ik heb een ring gevonden. Een knabbel in de vorm van een ring. Iedereen die hem ruikt, wil hem opeten.",
    "ook_ik": "Zelfs ik. Vooral ik.",
    "berg": "Hij moet naar de Frituurberg, in de Guhbarbecuether. En jij ging toch al die kant op.",
    "feest": "Maar eerst een afscheidsfeest. In de Knabbelgouw gaat niemand op reis met een lege maag. Help je even mee?",
}, {"guhdalf": "Guhdalf", "sam": "Sam-guh"})

SCENE_FEEST = ("Het afscheidsfeest", {
    "verteller": "Die avond is het feest. De hele Knabbelgouw is er, want er is taart.",
    "sam_taart": "Dit is de derde taart, meneer Guhdalf. De eerste twee waren om te proeven, njeg.",
    "speech1": "Lieve guhs! Ik ken de helft van jullie maar half zo goed als ik zou willen.",
    "speech2": "En de andere helft eet twee keer zoveel als ik kan bakken.",
    "vahoeg": "VAHOEG!",
    "apart": "Kom eens hier. Dit is hem: de Knabbelring. Eén knabbel om ze allemaal te delen.",
    "geheim": "Bewaar hem geheim. Bewaar hem heel. En wat je ook doet: NIET opeten.",
    "omdoen": "En niet omdoen. Dan ziet het Oog van Sausron je. Die heeft al heel lang trek.",
    "sam_mee": "Op reis? Zonder kok? Dat dacht ik niet, njeg. Vraag het me straks maar, ik zeg ja.",
    "vuurwerk": "En nu: vuurwerk! Iedereen bukken, deze is nieuw.",
}, {"guhdalf": "Guhdalf", "sam": "Sam-guh", "gast": "Gouwguh"})

# --- chat lines -------------------------------------------------------------------------------------------------------------
TEKSTEN = {
    # Guhdalf
    "quest.guhs.ringh1.guhdalf.grillguh": "Hm. De barbecue van de Grillguh is nog koud. Help hem eerst, en kom dan terug. Een reis begint met een "
                                           "brandend portaal, njeg.",
    "quest.guhs.ringh1.guhdalf.geen_kamp": "Hier is geen plekje voor mijn kar, njeg. Zoek me bij een andere grote barbecueput: daar staat alles klaar.",
    "quest.guhs.ringh1.guhdalf.hallo": "Aha, daar ben je. Precies op tijd. Ga zitten, dit moet je even zien...",
    "quest.guhs.ringh1.guhdalf.klusjes": "Het feest dekt zichzelf niet! Nog te doen: %s.",
    "quest.guhs.ringh1.guhdalf.klus.vuurwerk": "een vuurpijl afsteken (de kist op mijn kar)",
    "quest.guhs.ringh1.guhdalf.klus.tafel": "de feesttafel dekken",
    "quest.guhs.ringh1.guhdalf.klus.sam": "Sam-guh uitnodigen",
    "quest.guhs.ringh1.guhdalf.klaar_voor_feest": "Alles staat klaar? Dan gaan we feesten. Let op mijn hoed.",
    "quest.guhs.ringh1.guhdalf.na_feest": "Je hebt de ring. Niet opeten, niet omdoen. Praat met Sam-guh: hij wil mee, of je wilt of niet.",
    "quest.guhs.ringh1.guhdalf.proviand": "Sam-guh loopt mee, mooi. Pak eerst de proviand in: de kratten staan bij mijn kar.",
    "quest.guhs.ringh1.guhdalf.portaal": "Alles ingepakt? Loop dan met Sam-guh naar het grillportaal. Ik zet het aan zodra je er staat. Vlieg... "
                                         "nee wacht, dat is voor later.",
    "quest.guhs.ringh1.guhdalf.ring_kwijt": "Kwijt? De ENE knabbel? Hier, ik had hem al teruggevonden. In je andere zak. Njeg.",
    # the chores
    "quest.guhs.ringh1.vuurwerk.klus": "FWOESH! De eerste vuurpijl is de lucht in. Guhdalf knikt tevreden.",
    "quest.guhs.ringh1.vuurwerk.los": "Fwoesh! Nog eentje, omdat het kan.",
    "quest.guhs.ringh1.vuurwerk.wacht": "De volgende pijl moet nog even afkoelen, njeg.",
    "quest.guhs.ringh1.vuurwerk.dak": "Hier kan geen vuurpijl omhoog: er zit iets boven de kist.",
    "quest.guhs.ringh1.tafel.klus": "Je zet de taart en de knabbels op tafel. Het ziet er feestelijk uit. Er ontbreekt al een hapje...",
    "quest.guhs.ringh1.tafel.kijk": "De feesttafel van de Knabbelgouw. Er staat altijd taart. Er ontbreekt altijd een hapje.",
    "quest.guhs.ringh1.tafel.straks": "Nog niet snoepen! Eerst moet het feest beginnen, njeg.",
    "quest.guhs.ringh1.klusjes_klaar": "Alles staat klaar voor het feest. Praat met Guhdalf!",
    # Sam-guh at home
    "quest.guhs.ringh1.sam.onbekend": "Goeiedag! Ik ben Sam-guh, de hovenier. Als u Guhdalf zoekt: die staat bij zijn kar te mopperen, njeg.",
    "quest.guhs.ringh1.sam.uitnodiging": "Een feest? Met taart? Ik kom, njeg! Ik neem mijn eigen lepel mee.",
    "quest.guhs.ringh1.sam.komt_al": "Ik kom eraan! Even de aardappels water geven.",
    "quest.guhs.ringh1.sam.eerst_feest": "Eerst het feest, dan de rest. Zo doen we dat hier.",
    "quest.guhs.ringh1.sam.mee1": "Op reis met een ring die naar knabbels ruikt? Zonder kok? Dat gaat niet door, njeg.",
    "quest.guhs.ringh1.sam.mee2": "Ik ga mee! Ik heb mijn pannen al om. Maar eerst proviand: zonder worst loop ik niet verder dan het tuinhek.",
    "quest.guhs.ringh1.sam.proviand.0": "Worst! Daar kom je de eerste heuvel mee over, njeg.",
    "quest.guhs.ringh1.sam.proviand.1": "Kaas. Een hele bol. Voor als de worst op is. Dus voor straks.",
    "quest.guhs.ringh1.sam.proviand.2": "Knabbels! Kook ze, stamp ze, stop ze in een stoofpotje.",
    "quest.guhs.ringh1.proviand.al": "Dat zit al in Sam-guhs rugzak.",
    "quest.guhs.ringh1.proviand.kijk": "Een krat met proviand voor onderweg. Het ruikt heerlijk.",
    "quest.guhs.ringh1.proviand.zonder_sam": "Zonder Sam-guh krijg je dit nooit gedragen. Vraag hem eerst mee, njeg.",
    "quest.guhs.ringh1.proviand.sam_ver": "Sam-guh is te ver weg om het aan te pakken. Wacht even tot hij bij je is.",
    "quest.guhs.ringh1.proviand.klaar": "De rugzak zit vol. Tijd om te gaan: op naar het grillportaal!",
    # the portal
    "quest.guhs.ringh1.portaal.sam1": "Hier is het. Als ik nog één pootje verzet, ben ik verder van huis dan ik ooit geweest ben, njeg.",
    "quest.guhs.ringh1.portaal.sam2": "...Vooruit dan. Maar als er daar geen tweede ontbijt is, ga ik klagen bij Guhdalf.",
    "quest.guhs.ringh1.portaal.sam_ver": "Wacht op Sam-guh! Zonder hem ga je niet door dat portaal.",
    # the residents of the Gouw
    "quest.guhs.ringh1.bewoner.0": "Goeiemorgen! Of bedoel je dat het een goeie morgen is, of ik het nou wil of niet? Njeg.",
    "quest.guhs.ringh1.bewoner.1": "Avonturen? Nare, vervelende dingen. Daar kom je te laat van voor het eten.",
    "quest.guhs.ringh1.bewoner.2": "Heb je het tweede ontbijt al gehad? En het elfuurtje? De lunch? De middagthee? Njeg, wat een leven.",
    "quest.guhs.ringh1.bewoner.3": "Die Guhdalf. Altijd vuurwerk, altijd gedoe. Maar zijn taart is goed.",
    "quest.guhs.ringh1.bewoner.4": "Sam-guhs aardappels zijn de beste van de hele Gouw. Zeg maar niet dat ik het zei.",
    "quest.guhs.ringh1.bewoner.5": "Ik heb gehoord dat er aan de andere kant van het portaal saus uit de grond komt. Zou het waar zijn?",
    "quest.guhs.ringh1.bewoner.6": "Mijn deur is rond. Mijn raam is rond. Ik ben rond. Alles klopt hier, njeg.",
    "quest.guhs.ringh1.bewoner.7": "Als je een ring vindt die lekker ruikt: niet opeten. Dat zei mijn oma altijd. Geen idee waarom.",
    "entity.guhs.ringh1.bewoner.0": "Bakkerguh Knabbelings",
    "entity.guhs.ringh1.bewoner.1": "Tante Lobelia Guhzak",
    "entity.guhs.ringh1.bewoner.2": "Visser Guhpkuil",
    "entity.guhs.ringh1.bewoner.3": "Rozie Katoenguh",
    # things in the Guhdex
    "gui.guhs.ringh1.nodig.vuurwerk": "Vuurpijl afgestoken",
    "gui.guhs.ringh1.nodig.tafel": "Feesttafel gedekt",
    "gui.guhs.ringh1.nodig.sam": "Sam-guh uitgenodigd",
    "gui.guhs.ringh1.nodig.worst": "Worst ingepakt",
    "gui.guhs.ringh1.nodig.kaas": "Kaas ingepakt",
    "gui.guhs.ringh1.nodig.knabbels": "Knabbels ingepakt",
    "gui.guhs.ringh1.beloning.portaal": "Het grillportaal doet het weer",
    "gui.guhs.ringh1.beloning.sam": "Sam-guh loopt met je mee",
    "gui.guhs.ringh1.doel.guhdalf": "Guhdalf bij de grote barbecueput",
    "gui.guhs.ringh1.doel.portaal": "Het grillportaal",
    # the blocks
    "block.guhs.ringh1_vuurwerkkist": "Guhdalfs vuurwerkkist",
    "block.guhs.ringh1_feesttafel": "Feesttafel van de Knabbelgouw",
    "block.guhs.ringh1_proviand": "Proviandkrat",
    "block.guhs.ringh1_schoorsteentje": "Heuvelschoorsteentje",
}

# --- advancements of the visible tab knabbelring (name, parent, icon, frame, title, text) ------------------------------------
ADVANCEMENTS = [
    ("ring_h1_vuurwerk", "ring_guhdalf", "minecraft:firework_rocket", "task", "Fwoesh!", "Steek een vuurpijl af uit Guhdalfs kist"),
    ("ring_h1_feest", "ring_guhdalf", "guhs:ring_feestknabbel", "task", "Een langverwacht knabbelfeest", "Vier het afscheidsfeest in de Knabbelgouw"),
    ("ring_h1_proviand", "ring_gekregen", "guhs:guhbraadworst", "task", "Kook ze, stamp ze", "Pak met Sam-guh de proviand in"),
    ("ring_h1_klaar", "ring_gekregen", "guhs:grillkool", "goal", "Verder van huis dan ooit", "Loop met Sam-guh naar het grillportaal: hoofdstuk 1 is klaar"),
    ("ring_h1_bewoners", "ring_guhdalf", "minecraft:warped_door", "task", "Goeiemorgen!", "Praat met vier bewoners van de Knabbelgouw"),
]
VERBORGEN = ["ring_h1_gouw"]

# --- FTB (section ring_h1 of chapter guhs_knabbelring; one chain) -------------------------------------------------------------
FTB = [
    ("ring_h1_guhdalf", "Precies op tijd", "Praat met &dGuhdalf&r bij de &6grote barbecueput&r in de Guhmensie (eerst moet de barbecue van de "
     "Grillguh branden). Hij vertelt over een knabbel in de vorm van een ring. En over een feest.", "npc:guhdalf", 1),
    ("ring_h1_klusjes", "Het feest dekt zichzelf niet", "Drie klusjes bij Guhdalfs kar: steek een &6vuurpijl&r af uit de kist, dek de "
     "&6feesttafel&r en nodig &dSam-guh&r uit. Iedereen kan dit doen: er raakt niets op.", "minecraft:firework_rocket", 2),
    ("ring_h1_feest", "Een langverwacht knabbelfeest", "Praat weer met Guhdalf. Het afscheidsfeest begint, met een toespraak, veel taart en "
     "een cadeau: de &6Knabbelring&r. Niet opeten. Niet omdoen. Njeg.", "guhs:knabbelring", 3),
    ("ring_h1_sam", "Ik ga mee!", "Praat met &dSam-guh&r. Vanaf nu loopt hij de hele reis met je mee, met al zijn pannen op zijn rug.",
     "guh:sam_guh", 4),
    ("ring_h1_proviand", "Niet zonder worst", "Pak de proviand in: klik op de drie &6proviandkratten&r bij de kar (worst, kaas en knabbels). "
     "Sam-guh moet in de buurt zijn om het aan te pakken.", "guhs:guhbraadworst", 5),
    ("ring_h1_portaal", "Verder van huis dan ooit", "Loop met Sam-guh naar het &6grillportaal&r van de barbecueput. Daarmee is hoofdstuk 1 klaar "
     "en doet het portaal het weer: de Guhbarbecuether wacht!", "guhs:grillkool", 6),
]
