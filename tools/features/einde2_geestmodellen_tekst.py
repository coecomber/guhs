"""
einde2, slice geestmodellen - every Dutch text of the slice (the source language; English: tools/lang/en/c73_e2_geestmodellen.json).

  WEZENS    entity id -> (Dutch name, spawn egg base colour, spawn egg spot colour); the koi's two own names: KOI
  KINDS     GuhNpcEntity.Kind id -> its Dutch name (entity.guhs.guh_npc.<kind>)
  PRAATJES  entity or kind id -> its two lines of small talk (quest.guhs.einde2_geestmodellen.praatje.<id>.0 / .1)

The spirits are mysterious and never mean (a schaduwgeest grumbles and shoves, nothing more); a line is a wink at who someone
is, never what happens in the story. Nothing calls a story, a tier or a place the last.
"""
P = "quest.guhs.einde2_geestmodellen.praatje."

WEZENS = {
    "koh": ("Koh de Gezichtenrover", 0x2C2634, 0xEEE6DA),
    "hei_bai_guh": ("Hei Bai-guh", 0xF6F4F0, 0x222026),
    "fang": ("Fang", 0xCE2E2C, 0xFCECCE),
    "geest_koi": ("Geest-koi", 0xF6F4F0, 0x1E1C22),
    "libelkonijnguh": ("Libelkonijnguh", 0xE8E2F8, 0x6ED6FF),
    "lantaarngeest": ("Lantaarngeest", 0xD63A32, 0xF0C446),
    "schaduwgeest": ("Schaduwgeest", 0x181420, 0xF6F8FF),
    # phase B (CONTRACT_E2_B 7.7)
    "kennisvosje": ("Kennisvosje", 0xD6E4FA, 0xC83C3C),
    "mistreiger": ("Mistreiger", 0xF6F8FC, 0xF6C446),
    "oude_reigerguh": ("Oude Reigerguh", 0xECF4FF, 0xEC4650),
    "wan_guh": ("Wan-guh", 0x2C2E4A, 0x78D6EC),
    "geestreus": ("Geestreus", 0x283A78, 0xC8F0FF),
    "kijkmasker": ("Kijkmasker", 0x46342C, 0xF0ECE2),
}
KOI = {0: "Tui, de maangeest", 1: "La, de oceaangeest"}

KINDS = {
    "theegeest_guh": "De Theegeest",
    "bestelguh": "De Bestelguh",
    # phase B (CONTRACT_E2_B 7.7): the echoes of the White Lotus, Guru Pathik and earlier Guhvatars
    "jeong_jeong_guh_echo": "Echo van Jeong Jeong-guh", "oom_miroh_echo": "Droom van Oom Miroh", "bumi_guh_echo": "Echo van Bumi-guh",
    "pakguh_echo": "Echo van Pakguh", "piandao_guh_echo": "Echo van Piandao-guh", "lotus_stichter_echo": "Stichter van de Witte Lotus",
    "lotus_lid_echo": "Lid van de Witte Lotus", "guru_pathik_guh": "Guru Pathik-guh", "kuruk_guh": "Kuruk-guh",
    "yangchen_guh": "Yangchen-guh", "szeto_guh": "Szeto-guh",
}

PRAATJES = {
    "koh": ("Welkom, kleine guh. Wat een... interessant gezichtje heb je. Blijf vooral heel, heel kalm.",
            "Ik heb al duizend gezichten, njeg. Eentje erbij zou mij zo... vadsig staan."),
    "hei_bai_guh": ("*snuffelt aan je hand* ...Ruik jij een beetje naar knabbelkoor? Mijn bos ruikt al zo lang naar as.",
                    "Ssst. Hoor je het woud fluisteren? Het fluistert jouw naam. ...Nee, grapje. Het fluistert 'knabbel'."),
    "fang": ("Grrrom. Roku-guh en ik vlogen elke ochtend boven de wolken. Ik mis vooral het ontbijt erna.",
             "Een geest-draak vliegt waar zijn hart heen wil. Mijn hart wil meestal naar het theepaviljoen, njeg."),
    "geest_koi": ("Wij zwemmen al eeuwen rondjes. Duw en trek, licht en donker, Tui en La. Duizelig? Nooit.",
                  "Zonder de maan geen getij, zonder de oceaan geen maan. ...En zonder knabbels geen guhs."),
    "libelkonijnguh": ("Bzzzz-hop! *kijkt je héél nieuwsgierig aan* ...Bzzzz-HOP!",
                       "*snuffelt aan je neus en zoeft weg* ...*komt terug* ...*zoeft weer weg*"),
    "lantaarngeest": ("Ik ben een beetje verdwaald. Maar zolang ik brand, ben ik nooit helemaal in het donker, njeg!",
                      "*knipoogt* Een lantaarn die knipoogt? In de Geestenwereld is dat heel gewoon, hoor."),
    "schaduwgeest": ("Ssst... ga weg. Dit is mijn donkere randje. Ik duw maar een beetje. *duw*",
                     "Ik was ooit een gedachte die niemand wilde denken. Nu duw ik guhs opzij. Mooie carrière, njeg."),
    "theegeest_guh": ("Ik kwam hier ooit voor één kopje thee. Dat was driehonderd jaar geleden. De thee is nog steeds héérlijk.",
                      "Ik ken nog iemand die hier ooit wil wonen... rond, baard, zegt wijze dingen over thee. Ik hou een kussentje voor hem vrij."),
    "bestelguh": ("Eén bestelling? Komt eraan! *rent naar de bank* ...Bonnetje erbij, njeg?",
                  "Zeg maar wat je nodig hebt: ik haal alles uit je bank en laat de machines draaien. Snel, vrolijk en vads!"),
    # --- phase B: the new creatures ---
    "kennisvosje": ("*legt een boekrol voor je neer en tikt erop met zijn pootje* ...Niet lezen? Dan volg je mij maar, njeg!",
                    "Kennis raakt nooit kwijt, zegt de uil. Alleen soms een beetje verdwaald. Daarvoor zijn wij er."),
    "mistreiger": ("*kijkt je aan met één oog* ...Kraa?", "*schudt de mist van zijn veren en gaat op één poot staan*"),
    "oude_reigerguh": ("Ik heb eeuwen om de stenen lotus heen geslapen. Wie alle tegels legt, maakt mij wakker. Jij dus, njeg.",
                       "De orde begon met één gesprek boven een speelbord. Alles wat groot wordt, begint heel klein."),
    "wan_guh": ("...", "Jij lijkt op iemand die ik heel lang geleden was."),
    "geestreus": ("*een zucht zo diep als de mist zelf*", "*de verte rommelt zachtjes, alsof iets heel groots glimlacht*"),
    "kijkmasker": ("*het masker draait langzaam naar je toe*", "*het masker kijkt. Het blijft kijken. Jij kijkt terug. Het wint.*"),
    # --- phase B: the echoes, Guru Pathik, earlier Guhvatars (every wisdom in own words) ---
    "jeong_jeong_guh_echo": ("Vuur is geen speelgoed, jonge guh. Het eet alles wat je het geeft... ook je knabbels.",
                             "Ik ben ooit weggelopen van mijn eigen vlam. Hier in de mist leer ik hem eindelijk kennen, njeg."),
    "oom_miroh_echo": ("Ben jij het echt, of droom ik weer over thee? ...Het maakt niet uit. Ga zitten, njeg.",
                       "Ergens ver weg zit ik gewoon thee te drinken. Dit hier is maar een droom van mij. Een gezellige droom."),
    "bumi_guh_echo": ("Hahaha! Jij denkt nog in rechte lijnen! Draai jezelf eens om. Of draai de kamer om!",
                      "Een gekke oude koning weet: het antwoord dat voor het grijpen ligt, is meestal het verkeerde, njeg."),
    "pakguh_echo": ("Nee. ...Grapje. Ik heb geleerd dat je geen snor nodig hebt om water te sturen.",
                    "Water vergeet niets. Ook de ketting niet die ik ooit sneed, voor iemand die ik nooit vergat."),
    "piandao_guh_echo": ("Een zwaard is geen wapen maar een penseel. Je schildert er je eigen verhaal mee.",
                         "Op de maat ademen, op de maat pareren. Ritme is de stilste soort moed, njeg."),
    "lotus_stichter_echo": ("In de tijd van Roku-guh legden wij de eerste tegel. Eén witte lotus, tussen alle vlaggen in.",
                            "De orde hoort bij geen enkel volk. Wij horen bij iedereen die thee, wijsheid en knabbels deelt."),
    "lotus_lid_echo": ("Het wachtwoord? ...Een lotus bloeit ook in de modder. Welkom, njeg.",
                       "Ik speelde vroeger elke avond Pai Sho. Nu nog steeds, alleen een beetje doorzichtiger."),
    "guru_pathik_guh": ("Ah, daar ben je! Ik wachtte al op je. Ik wacht graag. Een slokje ui-banaansap? Heel goed voor je chakra's.",
                        "Laat los wat je zwaar maakt, jonge guh. Behalve je knabbels. Die mag je houden."),
    "kuruk_guh": ("Ik heb ooit gevochten met een geest die gezichten stal. Hou het jouwe goed vast, njeg.",
                  "Ik dacht vroeger dat ik alle tijd van de wereld had. Gebruik de jouwe goed, Guhvatar."),
    "yangchen_guh": ("Ook een luchtnomade moet soms stevig staan, voor wie van haar houdt.",
                     "De wind waait nooit voor zichzelf. Hij draagt altijd iets mee: een blaadje, een liedje, een knabbelgeur."),
    "szeto_guh": ("Een volk leid je niet met vuur, maar met goede papieren en een warm hart. Vooral de papieren, njeg.",
                  "Ik heb mijn hele leven formulieren ingevuld. Zelfs als geest heb ik er nog een paar bij me."),
}

# the pet libelkonijnguh (Java Libelkonijnguh): a click of its owner, and the sneak-click "stay here" / "come along"
HUISDIER = {
    "0": "*maakt een vrolijk rondje om je hoofd* Bzzz-hop! Ik blijf lekker bij jou, njeg.",
    "1": "*landt even op je schouder, snuffelt aan je oor en zoeft weer op*",
    "blijf": "*gaat op een blaadje zitten* Ik wacht hier wel. Bzzz... (sluipklik om me weer mee te nemen)",
    "mee": "*schiet op je af* Bzzz-HOP! Mee, mee, mee!",
}

# what the slice says on screen (Java: Fang)
GUI = {
    "gui.guhs.einde2_geestmodellen.fang.vast": "Fang houdt je stevig vast. Nog even volhouden, njeg!",
}

# the creature pages of the Guhdex (all bonus pages): id -> (rarity, info)
GUHDEX = {
    "koh": ("Eén, diep in zijn grot (en hij heeft liever niet dat je komt)",
            "Een reusachtige duizendpoot-geest die gezichten verzamelt: honderden, ook van guhs en Mika's. Wie bij hem op bezoek "
            "gaat, laat niets op zijn gezicht zien: geen lach, geen schrik, geen njeg. Echt stelen doet hij nooit, maar hij kijkt "
            "wel heel hongerig."),
    "hei_bai_guh": ("Eén, in het Gefluisterwoud",
                    "Een schattige bosgeest-panda met een guh-snoet. Heeft zijn bos verdriet, dan wordt hij een zwarte reus met zes "
                    "armen en een wit masker. Een knabbelkoor-eikel uit het Guheinde stelt hem gerust: dan is hij weer klein en "
                    "knuffelbaar."),
    "fang": ("Eén, op het Hemelpad",
             "De geest-draak van Guhvatar Roku-guh: rood, doorschijnend, met guh-oortjes en lange snorharen. Hij draagt een Guhvatar "
             "op zijn nek waar dat nodig is. Het ontbijt na een vlucht vindt hij het allerbelangrijkste."),
    "geest_koi": ("Twee, boven de Hemelvijver",
                  "Tui en La: de maangeest en de oceaangeest, twee reusachtige gloeiende koi die elkaar eeuwig achternazwemmen. Duw "
                  "en trek, licht en donker. Zonder hen geen getij, en zonder getij geen strand om op te vadsen."),
    "libelkonijnguh": ("Overal waar het stil is",
                       "Een piepklein konijntje met een guh-gezicht en vier libellenvleugels. Ontzettend nieuwsgierig: het volgt je, "
                       "snuffelt aan je neus en schiet weg zodra je te dichtbij komt. Wie de verdwaalde lantaarns thuisbrengt, krijgt er "
                       "een die blijft."),
    "lantaarngeest": ("Vaak, maar soms verdwaald",
                      "Zwevende papieren lantaarns met een geschilderd gezichtje, in rood, amber, roze en blauw. Ze knipogen. Ze "
                      "verdwalen graag, en vinden het heerlijk als je ze naar de lantaarnboom terugbrengt."),
    "schaduwgeest": ("In de donkere randen van het woud",
                     "Lange rokerige schaduwen met twee witte ogen. Ze duwen je zachtjes weg van hun donkere plekje, meer niet. "
                     "Mopperen kunnen ze als de beste."),
    "kennisvosje": ("Een paar, bij de Witte Lotus (en heel soms een gouden)",
                    "Geestvosjes met ronde guh-oortjes, familie van de vossen van Wan Shi Guh. Ze sjouwen boekrollen rond. Volg er een, "
                    "dan wijst hij je een geheimpje aan. Een gouden kennisvosje zien is een verhaal voor je kleinguhs."),
    "mistreiger": ("Op de daken van de Witte Lotus",
                   "Kleine witte reigers met piepkleine oortjes. Ze staan op één poot in de mist en vliegen op zodra je te dichtbij "
                   "komt. Stiekem dichterbij sluipen helpt een beetje."),
    "oude_reigerguh": ("Eén, opgerold om de stenen lotus",
                       "Een reusachtige doorschijnende kraanvogelgeest met guh-oortjes en een rode kroon. Hij sliep eeuwen om de stenen "
                       "lotus heen, tot iemand alle Pai Sho-tegels legde. Hij kent de hele geschiedenis van de Witte Lotus, vanaf de "
                       "allereerste tegel."),
}


def texts():
    """{lang key: Dutch} of the slice (the spawn egg names are written by kit.spawn_ei)."""
    out = {}
    for id, (naam, _, _) in WEZENS.items():
        out[f"entity.guhs.{id}"] = naam
    for soort, naam in KOI.items():
        out[f"entity.guhs.geest_koi.{soort}"] = naam
    for kind, naam in KINDS.items():
        out[f"entity.guhs.guh_npc.{kind}"] = naam
    for id, (een, twee) in PRAATJES.items():
        out[f"{P}{id}.0"] = een
        out[f"{P}{id}.1"] = twee
    for k, tekst in HUISDIER.items():
        out[f"{P}huisdier.{k}"] = tekst
    out.update(GUI)
    return out
