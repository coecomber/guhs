"""
1.10.0 "Het Vadsrijk", the npc slice (CONTRACT_VADSRIJK.md). Java: feature/vadsrijk/npc.

One source for the whole zone-1 story:
  - the NPC guhs' names and idle lines (the NPCs themselves, their spots and clothes: Npcs.java / NpcPlekken.java);
  - every quest (QUESTS) -> data/guhs/vadsrijk/npc/quests.json (read by Quests.java) + all their texts;
  - quest items (vanilla textures), hidden FTB advancements guhs:quest/vr_<quest>, the FTB chapter (ftb());
  - tips (first-time hints), the Avonturiersgids pages and the bestiary pages of the questlog (J), scene/title texts.
All texts Dutch; en_us gets the same Dutch texts (contract).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_npc.py
"""
import json
import os
import re

P = "vadsrijk.npc."
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "npc")

# ---------------------------------------------------------------------------------------------------------------------------
# NPCs: name + 3 idle lines (vadsrijk.npc.naam.<id>, vadsrijk.npc.praat.<id>.1..3)
# ---------------------------------------------------------------------------------------------------------------------------
NPCS = {
    # Knuffelkamp
    "rekruteerder": ("Sergeant Pluizebol", [
        "Rug recht, oortjes omhoog, rekruut! Zo. Vahoeg.",
        "Een goede ridder van De Knuffelorde slaat nooit hard. Alleen heel lief en heel vaak.",
        "Ik heb ooit honderd klodders opgevrolijkt op één dag. Daarna heb ik een week geslapen. Njeg."]),
    "kapitein": ("Kapitein Wolkewiet", [
        "Mijn ballonschip vliegt op warme lucht en goede zin. Vooral goede zin.",
        "Niet over de reling hangen, rekruut. Vorige week verloor iemand zijn knuffel boven zee.",
        "Windkracht vads, zicht vahoeg. Mooi vliegweer!"]),
    "kok": ("Kok Knapperkorst", [
        "Pluiskoeken! Vers van de plaat! Ze zijn een beetje zwart aan de onderkant, maar dat hoort zo.",
        "Wie vads wil geven, moet eerst zelf goed eten. Dat is regel één van de Orde. Of regel twee. Na het eten.",
        "Ruik je dat? Dat is het kampvuur. En een klein beetje mijn schort."]),
    "priesteres": ("Zuster Zachtjes", [
        "Elk pad van de Orde begint met één klein, zacht stapje.",
        "Het tempeltje is oud, maar het knuffelt nog steeds terug als je de zuilen aait.",
        "Ridder, jager of magiër: je hart is wat telt. En een beetje je vaardigheden."]),
    "trainer_ridder": ("Meester Kussensloop", [
        "Linkerklik is je gewone mep. R, V en G zijn je vaardigheden. Z is de grote klapper!",
        "Een Knuffelridder staat vooraan. Met een kussen. Altijd met een kussen.",
        "Die trainingspoppen kunnen wel tegen een stootje. Ze zijn van hooi en goede moed."]),
    "trainer_jager": ("Juf Pijltje", [
        "Een Pluisjager houdt afstand en mikt met haar hart. En met haar ogen, dat helpt ook.",
        "Rolsalto! Weg uit de klodderprut, en je volgende schot is extra raak.",
        "Ik raak een appel op honderd stappen. Daarna eet ik hem op."]),
    "trainer_magier": ("Opa Sterrenstof", [
        "Elementen, rekruut! Vuur wint van zwam, zwam van slijm, slijm van vuur. Roze is overal goed voor.",
        "Mijn eerste vuurbal was een roze bubbeltje. Hij is nooit geland. Hij zweeft nog steeds ergens.",
        "Een Vadsmagiër heelt ook. Een cirkel op de grond, en iedereen voelt zich weer vads."]),
    "visser_dobber": ("Visser Dobber", [
        "Sssst. De slijmpjes in de vijver bijten niet, maar ze kriebelen wel.",
        "Ik vis al twintig jaar in deze vijver. Ik heb nog nooit een vis gezien. Wel heel veel rust.",
        "Mijn hoedje is weggewaaid, maar ik zit net zo lekker."]),
    # Zwamzoet: order, trade, services
    "grootmeester": ("Grootmeester Vadsvoet", [
        "Welkom in de tempel van De Knuffelorde. Hier wissel je van pad, kies je je vaardigheden opnieuw en word je groter.",
        "Ik ben oud, maar mijn knuffels zijn nog steeds legendarisch.",
        "Vahoegnir... ik heb hem gekend toen hij nog roze was. Hij at suikerspin uit mijn poot."]),
    "smid": ("Smid Hamerbol", [
        "Sterren erop? Kan! Maar soms springt er eentje af. Dat is het spannende eraan, njeg.",
        "Verpluisd? Breng maar hier, dan klop ik het weer glad. Niks is ooit echt kapot.",
        "Ik smeed met liefde en een héél zachte hamer."]),
    "winkelier": ("Winkelguh Snoepje", [
        "Drankjes, snoepjes, verband met hartjes erop! Kom maar kijken.",
        "Mijn pluisdrankje helpt tegen alles. Behalve tegen heimwee. Daar helpen knuffels tegen.",
        "Alles voor een eerlijke Vadsmunt. Brons, zilver of goud!"]),
    "wapenhandelaar": ("Wapenhandelaar Klingel", [
        "Zwaarden, bogen, staven! Allemaal met een zachte rand. Veiligheid voorop.",
        "Dat ooglapje? Gewoon stijl. Ik zie alles prima. Bijna alles.",
        "Een goed wapen past bij je klasse. Een slecht wapen past bij niemand."]),
    "marktmeester": ("Marktmeester Hamertje-Tik", [
        "Eenmaal, andermaal... VADS! Het veilinghuis is open!",
        "Verkoop je buit aan andere avonturiers. Gebonden spullen niet, die blijven bij jou.",
        "Mijn hamertje-tik is het belangrijkste hamertje van Zwamzoet. Vraag maar aan de smid."]),
    "bankier": ("Bankier Spaarvarken", [
        "Uw Vadsmunten zijn hier veiliger dan in een kluis. Dit ís een kluis. Nou ja, straks. De kluisdeur is nog in de maak.",
        "Honderd brons is één zilver. Honderd zilver is één goud. Duizend goud is één vakantie.",
        "Rente? Natuurlijk. Wij betalen in knuffels."]),
    "kleermaker": ("Kleermaker Knoopje", [
        "Kleding uit het Vadsrijk mag mee naar huis! Voor je guh, of voor jezelf.",
        "Een strikje hier, een sjaaltje daar. Mode is ook een soort knuffel.",
        "Ik naai alles met roze draad. Ook de zwarte sokken."]),
    "klerk": ("Klerk Stempeltje", [
        "Het questbord! Hier hangt wie er in Zwamzoet hulp nodig heeft. Stempeltje erop, klaar.",
        "Ik stempel alles. Brieven, premies, mijn lunch. Dat laatste was een vergissing.",
        "Premies voor dappere avonturiers! Even tekenen hier... en hier... en hier."]),
    "stalmeester": ("Stalmeester Hoefje", [
        "Rijdieren! Ze springen, ze rennen en ze worden sterker met jou mee.",
        "Een Hoedhupper is een paddenstoel met pootjes. Hij is dol op wortelzwammen.",
        "Wil je een rijdier? Kom terug als je een beetje sterker bent, rekruut. Level 8, zeg maar."]),
    "havenmeester": ("Havenmeester Ankertje", [
        "Meeuwen, wind en de geur van zout. Heerlijk. Behalve de meeuwen.",
        "Het luchtschip? Dat ligt bij De Haven, aan de overkant van de baai. Stuurguh Zeppie vliegt je waarheen je wilt.",
        "Ik tel elke dag de bootjes. Het zijn er drie. Al jaren. Maar je weet maar nooit."]),
    "luchtschipper": ("Stuurguh Zeppie", [
        "Het luchtschip vertrekt wanneer jij wilt. Wij zijn heel flexibel.",
        "Terug naar Knuffelkamp? Groet de Kampklodder van me!",
        "Ik vlieg al twintig jaar. Ik ben nog nooit geland zonder te stuiteren."]),
    "akim_kaasguh": ("Akim de Kaasguh", [
        "Kaas! Echte guh-kaas! Zelf gemaakt! Niet aankomen. Wel kopen. Eh... later.",
        "Ik ben een gewone guh. Heel gewoon. Ik hou van kaasknabbels en... eh... muizen niet. Nee. Bah, muizen.",
        "Waarom kijk je zo? Ik heb gewoon een zonnebril op. Het is zonnig. Binnen ook."]),
    # Zwamzoet: citizens
    "oma_truus": ("Oma Truus", [
        "Breien is net als knuffelen, vadsje. Steekje voor steekje.",
        "Toen ik jong was, waren de klodders nog roze en de manen nog maar met z'n eentjes.",
        "Kom eens hier, je hebt een pluisje op je oortje. Zo."]),
    "bakker_bram": ("Bakker Bram", [
        "Mijn zwamtaart is beroemd tot in de Druipgrotten. Daar wonen er alleen niet zoveel.",
        "Deeg kneden is goed voor je pootjes. En voor je humeur.",
        "Ruik eens! Dat is versgebakken vads."]),
    "kleine_kiki": ("Kleine Kiki", [
        "Heb jij mijn knuffels gezien? Ze zijn op avontuur gegaan zonder mij!",
        "Later word ik ook een ridder. Of een prinses. Of een ridder-prinses!",
        "Ik kan al tot tien tellen. Een, twee, drie, vier, vads, zes..."]),
    "dichter_daan": ("Dichter Daan", [
        "O Zwamzoet, gij zoete zwam, waar elke guh... eh... iets met jam.",
        "Een dichter heeft inspiratie nodig. En koffie. Vooral inspiratie.",
        "Rijmt 'vahoeg' op iets? Ik zoek al drie jaar."]),
    "postguh_pim": ("Postguh Pim", [
        "Post! Post! Wie wil er post?",
        "Ik ken elk huisje op elke hoed. Behalve dat ene. Dat zoek ik nog.",
        "Regen, wind of grauwe pluis: de post komt altijd. Soms een beetje plakkerig."]),
    "tuinier_tilly": ("Tuinier Tilly", [
        "Mijn tulpen zijn grijs geworden! Grijs! Ze waren altijd roze!",
        "Planten moet je toezingen. Mijn rozen houden van smartlappen.",
        "Een tuin is als een guh: hij groeit van aandacht."]),
    "muzikant_mo": ("Muzikant Mo", [
        "Boem-pa-pa, boem-pa-pa... zonder tuba klinkt het een beetje zielig.",
        "Ik speel op elk feest in Zwamzoet. Nou ja, ik zou spelen. Als ik mijn tuba had.",
        "Muziek maakt vads. Dat is wetenschap. Vraag maar aan de professor."]),
    "professor_pluis": ("Professor Pluis", [
        "Fascinerend! Grauwe pluis is precies zo zacht als gewone pluis, maar dan... chagrijnig.",
        "Ik heb drie diploma's in Pluiskunde. En één in Breien, van Oma Truus.",
        "Hypothese: de Vadsloosheid is besmettelijk. Conclusie: handjes wassen!"]),
    "ridder_rammel": ("Ridder Rammel", [
        "In mijn tijd versloegen we klodders met alleen een theelepeltje! ... Nou ja, opvrolijken.",
        "Mijn harnas rammelt een beetje. Daarom heet ik zo. Of andersom.",
        "Ik ben met pensioen. Maar mijn avontuurlust niet!"]),
    "kapster_fohnie": ("Kapster Föhnie", [
        "Je oortjes kunnen wel een kleine coupe gebruiken, schat.",
        "Het geheim van mooie krullen? Slijmgel. Echte, verse klodderslijm.",
        "Föhnen, kammen, pluizen. In die volgorde. Altijd."]),
    "schilder_sien": ("Schilder Sien", [
        "Kijk naar de weide! Al die kleuren! Nou ja... vroeger.",
        "Ik schilder alleen wat vrolijk is. Daarom heb ik zo weinig grijze verf.",
        "Mijn meesterwerk heet 'Klodder in de ochtendzon'. Hij werd opgegeten."]),
    "slaapkop_sjors": ("Slaapkop Sjors", [
        "Zzz... eh, wat? Ik sliep niet. Ik rustte mijn ogen.",
        "Ik heb al drie nachten niet geslapen. Daar buiten snurkt iets heel hard.",
        "Een dutje is een knuffel voor je hoofd."]),
    "koopman_kees": ("Koopman Kees", [
        "Alles te koop! Behalve mijn karretje. Dat is weg.",
        "Een goede koopman verkoopt alles. Zelfs zand in de Druipgrotten.",
        "Handjeklap, oortjeklap, deal!"]),
    "weerguh_wolkje": ("Weerguh Wolkje", [
        "Het weer voor vandaag: zonnig, met kans op knuffels.",
        "Sinds de draak overkwam, is de lucht een beetje grijzig. Dat zit me niet lekker.",
        "Lichtjes in de nacht, dat zijn de kampvuren. Hoe meer, hoe vadser."]),
    "tweeling_tip": ("Tip", [
        "Ik ben Tip! Mijn zus heet Tap. Zij is zoek. Expres.",
        "Verstoppertje is het allerleukste spel. Behalve als jij moet zoeken.",
        "Tap verstopt zich altijd op plekken met paddenstoelen. Dat zegt niet zoveel hier."]),
    "wachter_dikkie": ("Wachter Dikkie", [
        "Ik bewaak Zwamzoet. Vooral de taartjes van Bakker Bram.",
        "Koning Klodder is zo groot als een huis. Een groot huis. Met een zolder.",
        "Halt! Wie gaat daar? O, jij. Ga maar door."]),
    # out in the zone
    "boswachter_mos": ("Boswachter Mos", [
        "Het Sporenwoud is mooi, maar pas op voor de sporenpuffers. Ze niezen.",
        "De zwammen hier zijn al eeuwen oud. Sommige zijn ouder dan de Grootmeester.",
        "Volg de lichtstralen, dan verdwaal je nooit. Bijna nooit."]),
    "tweeling_tap": ("Tap", [
        "Hihi! Niet verklappen dat ik hier zit!",
        "Ik zit hier al heel lang. Ik heb een paddenstoel als vriendje.",
        "Tip zoekt nooit goed. Hij kijkt altijd eerst onder zijn eigen bed."]),
    "mijnwerker_mopje": ("Mijnwerker Mopje", [
        "Druppel, druppel, druppel. De hele dag. Ik word er een beetje nat van.",
        "Kristallen! Overal kristallen! En slijm. Heel veel slijm.",
        "Mijn lampje is bijna op. Gelukkig gloeien de zwammen hier."]),
    "tempelwachter_steeltje": ("Tempelwachter Steeltje", [
        "De Zwamtempel is heilig. En heel erg trappig.",
        "De grote deur gaat alleen open voor wie de zwammen van de tempel helpt.",
        "Ik sta hier al honderd jaar. Mijn poten slapen."]),
}
KAMPWACHT_NAMEN = ["Warmpootje", "Gloeiwang", "Vonkje", "Haardje", "Smeulie", "Knetter", "Pookje", "Sintel", "Vlammetje", "Roosterbol", "Asje",
                   "Gensje"]
KAMPWACHT_REGELS = [
    "Steek het kampvuur aan: dan word je hier wakker als je flauwvalt. Geen zorgen, je verliest niks.",
    "Een kampvuur is een knuffel voor je hele lijf. Even zitten?",
    "Ik bewaak dit vuurtje met mijn leven. Nou ja, met mijn kussen."]

# ---------------------------------------------------------------------------------------------------------------------------
# Quest items (registered in NpcRegistry.QUEST_ITEMS): name, vanilla texture
# ---------------------------------------------------------------------------------------------------------------------------
QUEST_ITEMS = {
    "grauwe_pluis": ("Grauwe pluis", "minecraft:item/gray_dye"),
    "wolletje": ("Bolletje wol van Oma Truus", "minecraft:item/string"),
    "zwamhoedje": ("Zwamhoedje", "minecraft:block/red_mushroom"),
    "slijmdruppel": ("Verse slijmdruppel", "minecraft:item/slime_ball"),
    "druppelkristal": ("Druppelkristal", "minecraft:item/prismarine_crystals"),
    "kistje": ("Kistje van Koopman Kees", "minecraft:item/bundle"),
    "wortelzwam": ("Wortelzwam", "minecraft:item/carrot"),
    "brief": ("Brief met een hartje", "minecraft:item/paper"),
    "mika_speldje": ("Speldje met een Mika-kop", "minecraft:item/gold_nugget"),
}

# ---------------------------------------------------------------------------------------------------------------------------
# Quests. D(type, doel, n, text, bron, kans) = an objective (see Quest.Doel.Type in Quest.java).
# aanbod/klaar: [(speaker, text)], speaker "npc" (the quest NPC), "jij" (the player) or an NPC id.
# ---------------------------------------------------------------------------------------------------------------------------
QUESTS = []


def D(soort, doel, n, tekst, bron="", kans=0.0):
    return dict(soort=soort, doel=doel, n=n, tekst=tekst, bron=bron, kans=kans)


def Q(id, soort, gever, titel, tekst, doelen, aanbod, bezig, klaar, xp=0, munten=0, items=(), wapen=False, loot="", lv=1, na=(),
      inleveren=None, klasse=None, auto=False, praat=None, actie_aanname="", actie_klaar="", start_items=(), ftb=None):
    QUESTS.append(dict(id=id, soort=soort, gever=gever, titel=titel, tekst=tekst, doelen=doelen, aanbod=aanbod, bezig=bezig, klaar=klaar,
                       beloning=dict(xp=xp, munten=munten, items=[list(i) for i in items], wapen=wapen, loot=loot), lv=lv, na=list(na),
                       inleveren=inleveren or gever, klasse=klasse, auto=auto, praat=praat or {}, actie_aanname=actie_aanname,
                       actie_klaar=actie_klaar, start_items=[list(i) for i in start_items], ftb=ftb))


# ===== the main story (~12) =================================================================================================
Q("hq01_welkom", "hoofd", "rekruteerder", "Welkom, rekruut!",
  "Je bent per luchtschip aangekomen op Knuffelkamp, het oefeneiland van De Knuffelorde. Sergeant Pluizebol stuurt je naar het "
  "kampvuur op het plein, waar Kok Knapperkorst op je wacht.",
  [D("gebied", "poi:kampplein", 1, "Loop naar het kampplein"), D("praat", "kok", 1, "Praat met Kok Knapperkorst")],
  [("npc", "Njeg! Daar ben je eindelijk! Een echte, verse rekruut! Welkom op Knuffelkamp, het oefeneiland van De Knuffelorde. "
           "Ik ben Sergeant Pluizebol."),
   ("jij", "Hoi! Ik kom me aanmelden. Wat doet De Knuffelorde eigenlijk?"),
   ("npc", "Wij vrolijken op wat chagrijnig is geworden! In Het Vadsrijk worden wezens grauw en knorrig. Dat noemen we de "
           "Vadsloosheid. Wij geven ze hun vads terug. Met een kussen. Of een pijl. Of een vuurbal. Heel liefdevol."),
   ("npc", "...Njeg. Waarom kijk je zo? Heb jij ook het gevoel dat je dit eerder hebt gezien? Iets met 'Wynn'-nogwat?"),
   ("npc", "Nee hoor. Dit is héél anders. Hier zijn knuffels! En de monsters worden blij in plaats van dood. Heel anders. Echt."),
   ("npc", "Loop maar naar het kampvuur op het plein. Daar zit Kok Knapperkorst. Zeg maar dat ik je stuur, dan krijg je iets lekkers.")],
  "Het kampvuur, rekruut! Rechtdoor, langs de tenten. Je ruikt het vanzelf.",
  [("npc", "Aha! Een rekruut! Pluizebol stuurt je, hè? Ga zitten. Hier, een warme pluiskoek."),
   ("jij", "Hij is een beetje zwart aan de onderkant."),
   ("npc", "Dat hoort zo! Regel één van de Orde: wie vads wil geven, moet eerst zelf goed eten. Loop daarna naar Zuster Zachtjes bij "
           "het tempeltje in het zuidwesten. Zij helpt je je pad te kiezen.")],
  xp=60, munten=50, items=[("minecraft:cookie", 3)], inleveren="kok")

Q("hq02_pad", "hoofd", "priesteres", "Kies je pad",
  "Bij het klasse-tempeltje kies je je eerste pad: Knuffelridder (dichtbij, stevig), Pluisjager (van ver, snel) of Vadsmagiër "
  "(elementen en heling). Later kun je in de ordetempel van Zwamzoet altijd wisselen.",
  [D("klasse", "*", 1, "Kies een klasse bij het tempeltje")],
  [("npc", "Welkom bij het tempeltje, lieve rekruut. Hier begint elk pad van De Knuffelorde."),
   ("npc", "De Knuffelridder staat vooraan, met zwaard, knots of hamer, en beschermt de anderen. De Pluisjager blijft op afstand en "
           "schiet met boog of katapult. De Vadsmagiër speelt met de elementen en heelt zijn vrienden."),
   ("npc", "Elke klasse heeft een eigen level en eigen vaardigheden. Later kun je in de grote ordetempel van Zwamzoet wisselen. Dus "
           "kies gewoon met je hart. Ben je er klaar voor?")],
  "Kies maar rustig, lieverd. Klik nog eens op mij als je de keuze wilt zien.",
  [("npc", "Wat een mooie keuze. Ik zie het al aan je oortjes: dat past bij jou."),
   ("npc", "Ga nu naar het trainingsveld in het noordoosten. Meester Kussensloop laat je zien hoe je mept. Lief, maar raak.")],
  xp=80, munten=50, na=["hq01_welkom"], actie_aanname="klasse")

Q("hq03_poppen", "hoofd", "trainer_ridder", "Poppen meppen",
  "Op het trainingsveld staan trainingspoppen van hooi. Mep ze met je linkermuisknop (je gewone aanval). Probeer ook je "
  "vaardigheden op R, V en G, en spaar je ultieme vaardigheid op Z.",
  [D("pop", "*", 6, "Mep 6 keer op een trainingspop")],
  [("npc", "Aha, een nieuwe! Ik ben Meester Kussensloop. Ik leer rekruten meppen. Lief meppen."),
   ("npc", "Linkerklik is je gewone aanval. Met R, V en G doe je je vaardigheden, die kosten wat energie. En Z is je ultieme "
           "vaardigheid: die laadt op terwijl je vecht."),
   ("npc", "Mep maar eens zes keer op die trainingspoppen. Ze kunnen ertegen. Ze zijn van hooi en goede moed.")],
  "Zes keer meppen op de poppen! Toe maar, ze voelen er niks van.",
  [("npc", "Kijk eens aan! Netjes. Die pop is helemaal blij van je."),
   ("npc", "Hier, je eerste echte wapen. Zie je die lichtstraal? De kleur vertelt hoe zeldzaam iets is: grijs, groen, blauw, paars, "
           "oranje... en heel soms roze. Roze is guhddelijk!"),
   ("npc", "Nog één belangrijk ding, rekruut. Zit je ooit vast? In een kuil, tussen rotsen, of ergens waar je niet meer uit kunt "
           "klimmen? Druk dan op U en blijf drie tellen stilstaan. De knuffelwind zet je op het dichtstbijzijnde veilige plekje."),
   ("npc", "Loop nu eerst even naar Opa Sterrenstof, hier op het trainingsveld. Hij legt je de elementen uit. Dat moet je echt "
           "weten voordat je op avontuur gaat!")],
  xp=120, munten=80, wapen=True, na=["hq02_pad"])

Q("hq03b_elementen", "hoofd", "trainer_magier", "De elementendriehoek",
  "Opa Sterrenstof leert je de elementendriehoek: Vuur wint van Zwam, Zwam wint van Slijm, en Slijm wint van Vuur. Een sterke "
  "aanval doet x1,5 en je ziet 'Zwak!'; de andere kant op doet maar x0,7. Roze is sterk tegen alles wat ontvadst is. Het lettertje "
  "of icoontje naast de levensbalk van een wezen vertelt zijn element. Beantwoord zijn vraag!",
  [D("meld", "quiz:elementen", 1, "Beantwoord de elementenvraag van Opa Sterrenstof")],
  [("npc", "Aha, de nieuwe! Ik ben Opa Sterrenstof. Weet je wat het belangrijkste is van meppen? Niet hoe hard. Maar WAARMEE."),
   ("npc", "Elk wezen in het Vadsrijk heeft een element. Kijk naar het icoontje naast zijn levensbalk: een vlammetje is Vuur, een "
           "paddenstoeltje is Zwam, een druppeltje is Slijm, en een hartje is Roze."),
   ("npc", "En dan de driehoek, onthoud hem goed:  VUUR > ZWAM > SLIJM > VUUR.  Vuur verbrandt de zwam, de zwam slurpt het slijm "
           "op, en het slijm blust het vuur. Pfssst!"),
   ("npc", "Mep je een wezen met wat er van wint, dan zie je 'Zwak!' en doe je anderhalf keer zoveel. Andersom doe je maar een "
           "beetje. En Roze? Roze is extra sterk tegen alles wat ontvadst is. Klik straks op mij, dan overhoor ik je!")],
  "Vuur > Zwam > Slijm > Vuur. Klik op mij, dan overhoor ik je!",
  [("npc", "Goed zo! Jij bent een slimme. Kijk in je questlog (J) bij Gids, daar staat de driehoek nog een keer getekend."),
   ("npc", "Ga nu terug naar Kok Knapperkorst, bij het kampvuur op het kampplein. Hij wil dat het vuur brandt voor het donker "
           "wordt.")],
  xp=80, munten=40, na=["hq03_poppen"])

Q("hq04_kampvuur", "hoofd", "kok", "Een warm plekje",
  "Kampvuren zijn je checkpoints: steek er een aan met rechtsklik en daar word je wakker als je flauwvalt. Steek het kampvuur op "
  "het plein aan en ga even zitten bij de kok.",
  [D("kampvuur", "knuffelkamp", 1, "Steek een kampvuur aan in Knuffelkamp"), D("praat", "kok", 1, "Ga zitten bij Kok Knapperkorst")],
  [("npc", "Daar is mijn favoriete rekruut! Wil je iets voor me doen? Het kampvuur is uitgegaan, en zonder vuur geen koekjes."),
   ("npc", "Rechtsklik op een kampvuur om het aan te steken. Dan is het jouw checkpoint: als je ooit flauwvalt, word je daar weer "
           "wakker. Je verliest niks, je bent alleen even duizelig.")],
  "Rechtsklik op het kampvuur, rekruut! Dan zitten we zo lekker warm.",
  [("npc", "Heerlijk. Ga even zitten. Kijk, de zon komt op boven het water. Zie je in de verte de lichtjes van Zwamzoet?"),
   ("jij", "Het is hier echt mooi."),
   ("npc", "Ja hè? Zo vreedzaam. Zo stil. Zo..."),
   ("npc", "...hé. Wat is dat voor schaduw boven het water?")],
  xp=100, munten=50, items=[("minecraft:cookie", 2)], na=["hq03b_elementen"], actie_klaar="bam")

Q("hq05_klodderreus", "hoofd", "rekruteerder", "De Grauwe Klodderreus",
  "Een grote draak vloog over het kamp en liet een klont zwarte pluis in de vijver vallen. Daar kwam een reusachtige grauwe klodder "
  "uit! Vrolijk hem op. Let op zijn schaduw: daar landt hij.",
  [D("baas", "guhs:vr_klodderreus", 1, "Vrolijk de Grauwe Klodderreus op")],
  [("npc", "Rekruut! Naar de vijver! Dat ding is gigantisch!")],
  "Bij de vijver, rekruut! Kijk naar zijn schaduw en spring weg! Gebruik R op de kleintjes!",
  [("npc", "Je hebt hem opgevrolijkt! Kijk hem nou, helemaal roze. Hij kwispelt. Kunnen klodders kwispelen?"),
   ("npc", "Hij mag blijven. Vanaf nu is hij onze Kampklodder. Hij past precies in de vijver."),
   ("jij", "Maar die draak... wat wás dat?"),
   ("npc", "Dat... was Vahoegnir. Ooit was hij de grote roze beschermdraak van het Vadsrijk. Waar hij vloog, bloeiden de bloemen. "
           "Nu is hij grauw, en waar hij vliegt verwelken ze. Hij is de bron van de Vadsloosheid."),
   ("npc", "Dit moet de Grootmeester weten. Kapitein Wolkewiet vliegt je naar Zwamzoet: je vindt hem bij zijn luchtschip op de "
           "oostelijke steiger van het kamp. Je bent geen rekruut meer: je bent een echte avonturier van De Knuffelorde!")],
  xp=300, munten=300, loot="zeldzaam", na=["hq04_kampvuur"], auto=True, actie_klaar="kampklodder")

Q("hq06_zwamzoet", "hoofd", "kapitein", "Naar Zwamzoet!",
  "Kapitein Wolkewiet vliegt je met het luchtschip naar Zwamzoet, de paddenstoelenstad. De Grootmeester wacht in de tempel van De "
  "Knuffelorde, helemaal bovenop de hoogste zwam.",
  [D("gebied", "zwamzoet|haven", 1, "Vlieg naar Zwamzoet"), D("praat", "grootmeester", 1, "Meld je bij Grootmeester Vadsvoet in de ordetempel")],
  [("npc", "Daar is de held van het kamp! Instappen! Mijn ballonschip vliegt op warme lucht en goede zin, en we hebben allebei genoeg."),
   ("npc", "Zwamzoet ligt op een eiland ten noorden van hier. Paddenstoelen zo hoog als torens, met huisjes op de hoeden. De tempel "
           "van de Orde staat helemaal bovenop. Klaar?")],
  "Zeg het maar als je wilt vertrekken! Kies hieronder 'Naar Zwamzoet', of luid de scheepsbel op de steiger (rechtsklik) en kies "
  "Zwamzoet op de reiskaart.",
  [("npc", "Dus jij bent de rekruut die de Klodderreus heeft opgevrolijkt. Welkom in de tempel van De Knuffelorde. Ik ben "
           "Grootmeester Vadsvoet."),
   ("npc", "Vahoegnir... Ik heb hem gekend toen hij nog roze was. Hij at suikerspin uit mijn poot. Iets of iemand heeft hem grauw "
           "gemaakt, en nu verspreidt hij de Vadsloosheid over het hele rijk."),
   ("npc", "Overal waar hij vloog, liggen nu grauwe littekens in het land. Daar moeten we beginnen. Praat nog eens met mij, dan "
           "vertel ik je waar het eerste litteken ligt.")],
  xp=150, munten=100, na=["hq05_klodderreus"], inleveren="grootmeester", actie_aanname="luchtschip")

Q("hq07_littekens", "hoofd", "grootmeester", "Grauwe littekens",
  "Onderzoek het grauwe litteken in de Klodderweide, ten oosten van Zwamzoet. Vrolijk de klodders op die er knorrig van geworden "
  "zijn en verzamel wat grauwe pluis voor onderzoek.",
  [D("geheim", "poi:litteken_weide", 1, "Onderzoek het litteken in de Klodderweide"),
   D("opvrolijk", "gebied:klodderweide", 8, "Vrolijk klodders op in de Klodderweide"),
   D("verzamel", "guhs:vr_npc_grauwe_pluis", 5, "Verzamel grauwe pluis", "gebied:klodderweide", 0.55)],
  [("npc", "Ten oosten van de stad ligt de Klodderweide. Daar wonen de vrolijkste klodders van het rijk. Wóónden. Er loopt een "
           "grauw litteken dwars door de wei."),
   ("npc", "Ga kijken. Vrolijk de klodders op die knorrig zijn geworden, en neem wat grauwe pluis mee. Professor Pluis wil het vast "
           "ook zien.")],
  "Het litteken ligt in het zuidoosten van de Klodderweide. Volg het grijze spoor.",
  [("npc", "Grauwe pluis... Zacht als altijd, maar het voelt koud. En kijk: dit stukje heeft een stempel. Een klein rond pootje."),
   ("jij", "Een pootje? Van een klodder?"),
   ("npc", "Klodders hebben geen pootjes. Hm. Hm hm. Ik moet hier over nadenken. Ga intussen sterker worden, avonturier. Kom "
           "terug bij mij als je level 6 bent: dan stuur ik je naar het Sporenwoud.")],
  xp=400, munten=300, na=["hq06_zwamzoet"])

Q("hq08_sporenwoud", "hoofd", "grootmeester", "Sporen in het woud",
  "Het tweede litteken loopt door het Sporenwoud, ten westen van de stad. Boswachter Mos houdt daar de wacht bij het eerste "
  "kampvuur. Vrolijk onderweg de zwammen op.",
  [D("geheim", "poi:litteken_woud", 1, "Onderzoek het litteken in het Sporenwoud"),
   D("opvrolijk", "gebied:sporenwoud", 12, "Vrolijk wezens op in het Sporenwoud"),
   D("praat", "boswachter_mos", 1, "Praat met Boswachter Mos")],
  [("npc", "Het tweede litteken loopt door het Sporenwoud. Reuzenpaddenstoelen, lichtstralen, en zwammen die nu heel slecht "
           "gehumeurd zijn."),
   ("npc", "Boswachter Mos kent het woud als geen ander. Zoek hem bij het kampvuur aan de rand van het bos.")],
  "Het Sporenwoud ligt ten westen van de stad. Boswachter Mos zit bij het eerste kampvuur.",
  [("npc", "Ah, een avonturier van de Orde. Dat werd tijd. De zwammen zijn chagrijnig sinds die grauwe wolk overkwam."),
   ("npc", "Maar ik heb iets gezien. 's Nachts liepen er figuurtjes door het woud. Klein. Met zakken. Ze gingen naar het noorden, naar "
           "de Druipgrotten."),
   ("jij", "Figuurtjes met zakken?"),
   ("npc", "Ik kon ze niet goed zien. Ze miauwden een beetje. Vreemd, hè?"),
   ("npc", "Word eerst wat sterker, level 12, en kom dan terug bij mij, hier bij het kampvuur aan de rand van het woud. Dan vertel "
           "ik je wie die figuurtjes misschien van dichtbij heeft gezien.")],
  xp=700, munten=500, loot="ongewoon", lv=6, na=["hq07_littekens"], inleveren="boswachter_mos")

Q("hq09_druipgrotten", "hoofd", "boswachter_mos", "Diep in de Druipgrotten",
  "In de Druipgrotten in het noordwesten werkt Mijnwerker Mopje. Hij heeft de figuurtjes misschien gezien. Zoek hem diep in de "
  "grotten en breng hem veilig naar het grotkampvuur.",
  [D("gebied", "druipgrotten", 1, "Ga naar de Druipgrotten"),
   D("opvrolijk", "gebied:druipgrotten", 12, "Vrolijk wezens op in de Druipgrotten"),
   D("escorte", "mijnwerker_mopje", 1, "Breng Mijnwerker Mopje naar het grotkampvuur", "poi:grotkamp")],
  [("npc", "Mijnwerker Mopje werkt diep in de Druipgrotten. Als iemand die figuurtjes heeft gezien, is hij het."),
   ("npc", "Maar ik heb al dagen niks van hem gehoord. Zoek hem, en breng hem naar het kampvuur bij de grotingang. Hij verdwaalt "
           "altijd, de lieverd.")],
  "Mopje zit ergens diep in de Druipgrotten. Breng hem naar het grotkampvuur!",
  [("npc", "Pfoe! Licht! Lucht! Een kampvuur! Dank je wel, avonturier."),
   ("npc", "Die figuurtjes? Ja! Ik heb ze gezien. Ze sjouwden zakken vol grauwe pluis door de grotten, naar het oosten. Naar de berg "
           "van de Zwamtempel."),
   ("jij", "Wat waren het voor wezens?"),
   ("npc", "Kleine. Met puntoortjes. En een staart. Ze praatten over 'het plan' en 'de baas'. Meer heb ik niet verstaan, mijn "
           "helmpje zat over mijn oren."),
   ("npc", "Ga naar de Zwamtempel, hoog op de berg in het noordoosten, en zoek Tempelwachter Steeltje bij de grote deur. Steek de "
           "Kloof over via de drakenfossiel-brug. Maar pas op: daar zijn de wezens sterk. Word eerst level 16, dat is veiliger.")],
  xp=1100, munten=800, loot="zeldzaam", lv=12, na=["hq08_sporenwoud"], inleveren="mijnwerker_mopje", actie_klaar="start:hq10_zwamtempel",
  praat={"mijnwerker_mopje": [("npc", "Hallo? Een avonturier! Eindelijk! Ik ben de weg kwijt. Weer. Breng je me naar het kampvuur? "
                                       "Ik loop wel achter je aan.")]})

Q("hq10_zwamtempel", "hoofd", "mijnwerker_mopje", "De Zwamtempel",
  "De figuurtjes gingen naar de Zwamtempel, hoog op de berg in het noordoosten. Steek de Kloof over via de drakenfossiel-brug en "
  "meld je bij Tempelwachter Steeltje bij de grote deur.",
  [D("gebied", "zwamtempel", 1, "Ga naar de berg van de Zwamtempel"),
   D("geheim", "poi:tempelpoort", 1, "Vind de grote tempeldeur"),
   D("praat", "tempelwachter_steeltje", 1, "Praat met Tempelwachter Steeltje")],
  [("npc", "De Zwamtempel staat op de berg in het noordoosten. Je moet over de Kloof, via de oude drakenbotten. Brrr."),
   ("npc", "Als die figuurtjes daar iets uitspoken... dan moet de Orde dat weten. Ga jij maar. Ik blijf lekker bij dit vuurtje.")],
  "De Zwamtempel ligt in het noordoosten, op de berg. Over de drakenfossiel-brug!",
  [("npc", "Halt! Ah, een avonturier van de Orde. Ik ben Tempelwachter Steeltje."),
   ("npc", "Er is iets mis in de tempel. De Opperzwam, de oude hoeder van de tempel, is grauw geworden. Al zijn tempelzwammen zijn "
           "knorrig. En ik hoorde gemiauw achter de muren."),
   ("npc", "Ben je level 18? Praat dan nog eens met mij, hier bij de grote deur. En wil je naar binnen: klik met rechts op de grote "
           "deur, dan gaat hij open. Vanaf level 18 lukt dat altijd.")],
  xp=1400, munten=1000, lv=16, na=["hq09_druipgrotten"], inleveren="tempelwachter_steeltje", auto=True)

Q("hq11_opperzwam", "hoofd", "tempelwachter_steeltje", "De Opperzwam",
  "Klik met rechts op de grote deur van de Zwamtempel: dan schuift hij open. Ga naar binnen, vrolijk de tempelzwammen op en help "
  "de Opperzwam. Pas op voor zijn sporenwolken, en als de tempel donker wordt: steek de lichtzwammen aan!",
  [D("meld", "zwamtempel:deur", 1, "Klik op de grote deur van de Zwamtempel"),
   D("opvrolijk", "gebied:zwamtempel", 15, "Vrolijk wezens op in de Zwamtempel"),
   D("baas", "gebied:zwamtempel", 1, "Vrolijk de Opperzwam op")],
  [("npc", "De Opperzwam was ooit de liefste zwam van het rijk. Nu laat hij wortels uit de grond schieten en blaast hij grauwe "
           "sporen rond."),
   ("npc", "Als de tempel donker wordt, zie je alleen nog zijn gloeiende ogen. Steek dan de lichtzwammen aan! Neem vrienden mee "
           "als het moet. Samen is altijd vadser.")],
  "Klik met rechts op de grote deur om hem te openen. De Opperzwam zit diep in de tempel. Vrolijk eerst zijn tempelzwammen op.",
  [("npc", "Je hebt het gedaan! De Opperzwam is weer zichzelf. Hij huilt een beetje. Van blijdschap, denk ik. Of van de sporen."),
   ("npc", "En kijk wat er uit zijn hoed viel. Een speldje. Met een kattenkopje erop. Met een gemeen lachje."),
   ("jij", "Een Mika!"),
   ("npc", "Breng het naar Grootmeester Vadsvoet, in de ordetempel bovenop de hoogste zwam van Zwamzoet. Neem de lift aan de voet "
           "van die zwam. Snel!")],
  xp=3000, munten=2500, loot="episch", lv=18, na=["hq10_zwamtempel"], actie_klaar="start:hq12_schaduw")

Q("hq12_schaduw", "hoofd", "tempelwachter_steeltje", "Een grauwe schaduw",
  "Breng het Mika-speldje naar Grootmeester Vadsvoet in de ordetempel van Zwamzoet.",
  [D("praat", "grootmeester", 1, "Breng het Mika-speldje naar Grootmeester Vadsvoet")],
  [("npc", "Ga maar, avonturier. De Grootmeester moet dit zien.")],
  "Naar de Grootmeester, snel! Hij staat in de ordetempel op de hoogste zwam.",
  [("npc", "Een Mika-speldje... Dus het pootje op de pluis, de figuurtjes met zakken, het gemiauw in de tempel. Het waren Mika's."),
   ("npc", "De Mika's hebben Vahoegnir grauw gemaakt. Ze vangen zijn vads op in zakken, en waar hij vliegt, laten ze grauwe pluis "
           "achter. Zo wordt het hele rijk vadsloos, en zij worden steeds sterker."),
   ("jij", "Waar is de draak nu?"),
   ("npc", "Kijk maar naar de horizon. Daar, achter de bergen. Hij cirkelt boven de landen die nog niemand van ons heeft gezien."),
   ("npc", "Zone één is weer vads, dankzij jou. De littekens kleuren al terug. Maar dit was pas het begin. Rust wat uit, "
           "avonturier. Het Vadsrijk is groot.")],
  xp=2000, munten=5000, na=["hq11_opperzwam"], inleveren="grootmeester", actie_klaar="cliffhanger", auto=True,
  start_items=[("guhs:vr_npc_mika_speldje", 1)])

# ===== side quests in Knuffelkamp ===========================================================================================
Q("k01_dobber", "zij", "visser_dobber", "Het hoedje van Dobber",
  "Visser Dobber is zijn hoedje kwijt. Het waaide richting de vuurtoren.",
  [D("geheim", "poi:dobberhoed", 1, "Zoek het hoedje bij de vuurtoren")],
  [("npc", "Psst. Niet te hard praten, de slijmpjes slapen. Mijn hoedje is weggewaaid. Richting de vuurtoren, denk ik."),
   ("npc", "Wil jij even kijken? Ik kan hier niet weg. Ik zit net zo lekker.")],
  "Mijn hoedje ligt vast ergens bij de vuurtoren in het noordwesten.",
  [("npc", "Je hebt het gevonden! Er zat een slakje in. Die mag blijven wonen. Dank je wel!")],
  xp=60, munten=40, items=[("minecraft:cookie", 2)], na=["hq01_welkom"])

Q("k02_uitzicht", "zij", "trainer_jager", "Het mooiste uitzicht",
  "Juf Pijltje vindt dat elke jager eerst het uitzicht vanaf de vuurtoren moet zien. Daar zie je Zwamzoet liggen.",
  [D("gebied", "poi:vuurtoren", 1, "Klim naar de vuurtoren")],
  [("npc", "Een goede jager kijkt eerst ver weg, en dan pas dichtbij. Loop eens naar de vuurtoren met de guh-oortjes."),
   ("npc", "Daarvandaan zie je in de verte Zwamzoet. Paddenstoelen zo hoog als bergen!")],
  "De vuurtoren staat in het noordwesten, op het klif.",
  [("npc", "Mooi hè? Daar ga je straks naartoe. Onthoud dit gevoel, avonturier.")],
  xp=60, munten=40, na=["hq01_welkom"])

# ===== side quests in Zwamzoet (~20) ========================================================================================
Q("z01_wolletjes", "zij", "oma_truus", "Wolletjes kwijt",
  "De knorrige klodders in de Klodderweide hebben de bolletjes wol van Oma Truus opgegeten. Vrolijk ze op, dan spugen ze de wol "
  "vanzelf weer uit.",
  [D("verzamel", "guhs:vr_npc_wolletje", 6, "Bolletjes wol", "gebied:klodderweide", 0.45)],
  [("npc", "Vadsje, kom eens hier. Ik zat te breien op een bankje bij de wei, en toen kwam er een klodder. Hap! Mijn bolletje wol. "
           "En nog een. En nog een."),
   ("npc", "Als je ze opvrolijkt, spugen ze het vast weer uit. Zes bolletjes, dan kan ik je sjaal afmaken.")],
  "Zes bolletjes, vadsje. De klodders in de Klodderweide hebben ze.",
  [("npc", "Daar zijn ze! Een beetje plakkerig, maar dat breit juist lekker. Hier, een koekje. En nog een. Je bent veel te mager.")],
  xp=250, munten=200, items=[("minecraft:cookie", 4)], na=["hq06_zwamzoet"])

Q("z02_zwamtaart", "zij", "bakker_bram", "Zwamtaart",
  "Bakker Bram heeft zwamhoedjes nodig voor zijn beroemde zwamtaart. De zwammen in het Sporenwoud laten ze vallen als ze weer "
  "vrolijk zijn.",
  [D("verzamel", "guhs:vr_npc_zwamhoedje", 5, "Zwamhoedjes", "gebied:sporenwoud", 0.45)],
  [("npc", "Help! Morgen is het Zwamtaartfeest en ik heb geen zwamhoedjes meer!"),
   ("npc", "In het Sporenwoud lopen zwammen die hun hoedje verliezen als je ze opvrolijkt. Vijf stuks, dan bak ik de grootste "
           "taart ooit. Jij krijgt het eerste stuk!")],
  "Vijf zwamhoedjes uit het Sporenwoud! De oven staat al aan.",
  [("npc", "Perfect! Ruik je dat? Dat is de geur van succes. En van paddenstoel. Hier, een stuk taart voor onderweg.")],
  xp=550, munten=400, items=[("minecraft:pumpkin_pie", 3)], lv=6, na=["hq06_zwamzoet"])

Q("z03_knuffels", "zij", "kleine_kiki", "Tien knuffels kwijt",
  "Kleine Kiki's tien knuffels zijn 'op avontuur' gegaan en liggen verspreid over het hele Vadsrijk. Zoek ze! Ze glinsteren een "
  "beetje als je dichtbij bent.",
  [D("geheim", "knuffel", 10, "Vind de verloren knuffels")],
  [("npc", "Mijn knuffels zijn weg! Alle tien! Ze zijn op avontuur gegaan zonder mij!"),
   ("npc", "Ze liggen overal. In het kamp, in de stad, in de wei, in het bos, in de grotten... Als je dichtbij bent, glinsteren ze. "
           "Wil jij ze zoeken? Pleeeease?")],
  "Heb je ze al? Ze glinsteren met hartjes als je dichtbij bent!",
  [("npc", "ALLEMAAL! Je hebt ze allemaal gevonden! Meneer Pluis, Juffrouw Oortje, Kapitein Knuf... iedereen is er!"),
   ("npc", "Hier, dit vond ik onder mijn bed. Het glimt. Jij mag het hebben.")],
  xp=900, munten=600, loot="zeldzaam", na=["hq06_zwamzoet"])

Q("z04_inspiratie", "zij", "dichter_daan", "Op zoek naar inspiratie",
  "Dichter Daan heeft inspiratie nodig. Bezoek drie uitkijkpunten (de rotsnaalden) en vertel hem wat je zag.",
  [D("geheim", "poi:uitkijk_klodderkijk", 1, "Bezoek de Klodderkijk"),
   D("geheim", "poi:uitkijk_zwamnaald", 1, "Bezoek de Zwamnaald"),
   D("geheim", "poi:uitkijk_haventand", 1, "Bezoek de Haventand")],
  [("npc", "O avonturier, o dappere vads! Ik heb een probleem. Een dichtersprobleem. Mijn inspiratie is op."),
   ("npc", "Er zijn rotsnaalden in het rijk met een prachtig uitzicht: de Klodderkijk, de Zwamnaald en de Haventand. Klim erop en "
           "vertel me wat je ziet!")],
  "De Klodderkijk in de wei, de Zwamnaald in het woud, de Haventand bij de haven. Kijk goed!",
  [("jij", "Ik zag de hele weide vol klodders, een woud vol lichtstralen en de zee met een schildpad-eiland in de verte."),
   ("npc", "Een schildpad-eiland... in de verte... O! O! 'Daar, waar de schildpad slaapt in zee, droomt Zwamzoet zachtjes met mij "
           "mee.' VAHOEG! Dank je wel!")],
  xp=700, munten=400, na=["hq07_littekens"])

Q("z05_post", "zij", "postguh_pim", "Post voor Zwamzoet",
  "Postguh Pim heeft te veel brieven. Breng er drie rond: naar Oma Truus, Smid Hamerbol en Havenmeester Ankertje.",
  [D("praat", "oma_truus", 1, "Brief voor Oma Truus"), D("praat", "smid", 1, "Brief voor Smid Hamerbol"),
   D("praat", "havenmeester", 1, "Brief voor Havenmeester Ankertje")],
  [("npc", "Post! Post! Te veel post! Mijn tas is zo zwaar dat ik scheef loop."),
   ("npc", "Wil jij er drie voor me rondbrengen? Oma Truus bij het plein, Smid Hamerbol bij zijn smidse en Havenmeester Ankertje "
           "bij de haven.")],
  "Oma Truus, de smid en de havenmeester. Hup hup!",
  [("npc", "Alles bezorgd? Super! Nu loop ik weer recht. Hier, een fooitje.")],
  xp=220, munten=250, na=["hq06_zwamzoet"], start_items=[("guhs:vr_npc_brief", 3)],
  praat={"oma_truus": [("npc", "Een brief van mijn kleinkind! Kijk, met een tekening van een klodder. Of een wolk. Allebei mooi.")],
         "smid": [("npc", "Een brief voor mij? 'Beste smid, mijn zwaard is roze geworden.' Ja, dat doen zwaarden hier. Dank je!")],
         "havenmeester": [("npc", "Post! Een kaartje van mijn zus uit het verre zuiden. 'Groetjes uit de zon.' Wat lief.")]})

Q("z06_tulpen", "zij", "tuinier_tilly", "Grijze tulpen",
  "Sinds de draak overvloog, zijn de tulpen van Tuinier Tilly grijs. Bekijk het litteken in de Klodderweide en vrolijk tien "
  "klodders op: misschien helpt het.",
  [D("geheim", "poi:litteken_weide", 1, "Bekijk het litteken"), D("opvrolijk", "gebied:klodderweide", 10, "Vrolijk klodders op")],
  [("npc", "Mijn tulpen! Kijk dan! Grijs! Ze waren altijd zo roze als een guhneus!"),
   ("npc", "Het komt door dat litteken in de wei, dat weet ik zeker. Als de klodders daar weer vrolijk zijn, krijgen mijn bloemen "
           "misschien hun kleur terug.")],
  "Tien klodders opvrolijken in de Klodderweide. Ik zing ondertussen voor mijn tulpen.",
  [("npc", "Kijk! Het eerste blaadje wordt al roze! Dank je, dank je, dank je. Hier, een bosje voor jou.")],
  xp=300, munten=200, items=[("minecraft:pink_tulip", 3)], na=["hq06_zwamzoet"])

Q("z07_tuba", "zij", "muzikant_mo", "Waar is mijn tuba?",
  "Muzikant Mo heeft zijn tuba laten liggen bij een picknick diep in het Sporenwoud, bij de waterval in het zuidwesten.",
  [D("geheim", "poi:tuba", 1, "Vind de tuba in het Sporenwoud")],
  [("npc", "Boem-pa... boem... nee. Zonder tuba gaat het niet."),
   ("npc", "Ik had een picknick in het Sporenwoud, helemaal in het zuidwesten. En toen kwam er een sporenpuffer. Ik rende weg. "
           "Mijn tuba niet.")],
  "Mijn tuba ligt diep in het Sporenwoud, in het zuidwesten.",
  [("npc", "MIJN TUBA! Er zit een paddenstoel in. Boem-pa-pa-POEF! Haha, nu klinkt hij zelfs beter!")],
  xp=600, munten=450, lv=7, na=["hq06_zwamzoet"])

Q("z08_pluis", "zij", "professor_pluis", "Grauwe pluis onder de loep",
  "Professor Pluis wil grauwe pluis onderzoeken. Ontvadste wezens laten het soms vallen als je ze opvrolijkt.",
  [D("verzamel", "guhs:vr_npc_grauwe_pluis", 8, "Grauwe pluis", "*", 0.3)],
  [("npc", "Ah! Een avonturier! Precies wat ik nodig heb. Ik onderzoek de Vadsloosheid, wetenschappelijk!"),
   ("npc", "Breng me acht plukjes grauwe pluis. Van elk ontvadst wezen, maakt niet uit welk. Voorzichtig: het niest.")],
  "Acht plukjes grauwe pluis! Elk ontvadst wezen kan het laten vallen.",
  [("npc", "Fascinerend! Kijk door mijn vergrootglas. Zie je die kleine stempeltjes? Pootjes. Overal pootjes."),
   ("npc", "Wie stempelt er nou pootjes op pluis? Wetenschappelijk gezien: iemand met pootjes.")],
  xp=400, munten=300, lv=4, na=["hq06_zwamzoet"])

Q("z09_kristal", "zij", "professor_pluis", "Kristalhelder",
  "Professor Pluis denkt dat druppelkristallen de grauwe pluis kunnen zuiveren. In de Druipgrotten ligt een verborgen kristalkamer.",
  [D("geheim", "poi:kristalkamer", 1, "Vind de verborgen kristalkamer"),
   D("verzamel", "guhs:vr_npc_druppelkristal", 3, "Druppelkristallen", "gebied:druipgrotten", 0.35)],
  [("npc", "Nieuwe hypothese! Druppelkristallen uit de Druipgrotten zijn heel zuiver. Misschien maken ze grauwe pluis weer roze!"),
   ("npc", "Ergens diep in de grotten ligt een verborgen kristalkamer. Vind hem, en breng me drie druppelkristallen.")],
  "De kristalkamer ligt in het verste hoekje van de Druipgrotten. En drie druppelkristallen!",
  [("npc", "Kijk! Pluis plus kristal is... ROZE PLUIS! Het werkt! Dit wordt mijn vierde diploma!")],
  xp=1300, munten=900, loot="zeldzaam", lv=12, na=["z08_pluis"])

Q("z10_rammel", "zij", "ridder_rammel", "Nog één keer op avontuur",
  "Ridder Rammel is met pensioen, maar wil nog één keer naar de Klodderweide. Breng hem veilig naar het eerste kampvuur in de wei.",
  [D("escorte", "ridder_rammel", 1, "Breng Ridder Rammel naar het kampvuur in de wei", "poi:kampvuur_weide_1")],
  [("npc", "Jij daar! Jonge avonturier! In mijn tijd liep ik elke dag naar de Klodderweide. Nu zeggen ze dat ik 'te oud' ben."),
   ("npc", "Neem me mee! Nog één keer naar het kampvuur in de wei. Ik loop wel achter je aan. Rammel rammel.")],
  "Rammel rammel! Ik loop achter je aan, naar het kampvuur in de wei!",
  [("npc", "Daar is het! Het oude kampvuur. Hier zat ik, honderd jaar geleden. Het ruikt nog precies hetzelfde."),
   ("npc", "Dank je, jonge vads. Hier, mijn oude medaille. Ik loop zelf wel terug. Langzaam. Heel langzaam.")],
  xp=400, munten=300, loot="ongewoon", na=["hq06_zwamzoet"])

Q("z11_slijmgel", "zij", "kapster_fohnie", "Slijmgel",
  "Kapster Föhnie maakt haargel van verse klodderslijm. Vrolijke klodders laten soms een druppel vallen.",
  [D("verzamel", "guhs:vr_npc_slijmdruppel", 6, "Verse slijmdruppels", "gebied:klodderweide", 0.5)],
  [("npc", "Schat, mijn gel is op. En zonder gel geen krullen, en zonder krullen geen klanten."),
   ("npc", "Het beste spul komt van klodders uit de Klodderweide. Zes druppels, vers graag.")],
  "Zes slijmdruppels, schat. Vers uit de Klodderweide.",
  [("npc", "Mmm, die ruiken naar aardbei. Kom even zitten. Klaar! Je oortjes zitten fantastisch.")],
  xp=300, munten=250, na=["hq06_zwamzoet"])

Q("z12_kleuren", "zij", "schilder_sien", "Alle kleuren van de weide",
  "Schilder Sien wil de Klodderweide weer schilderen, maar alle klodders zijn grijs. Vrolijk er vijftien op, dan krijgen ze hun "
  "kleur terug.",
  [D("opvrolijk", "gebied:klodderweide", 15, "Vrolijk klodders op")],
  [("npc", "Ik wilde de Klodderweide schilderen, maar alles is grijs! Grijs, grijs, grijs! Ik heb maar één tube grijs."),
   ("npc", "Vrolijk er vijftien op, dan krijgen ze hun kleur terug. Roze, blauw, geel... dan kan ik weer schilderen!")],
  "Vijftien klodders opvrolijken. Ik zit hier klaar met mijn kwast.",
  [("npc", "KLEUREN! Overal kleuren! Dit wordt mijn meesterwerk. Ik noem het 'Avonturier met klodders'. Jij staat erop!")],
  xp=400, munten=300, na=["hq06_zwamzoet"])

Q("z13_snurk", "zij", "slaapkop_sjors", "Wie snurkt daar?",
  "Slaapkop Sjors kan niet slapen: in de Klodderweide snurkt elke nacht iets heel hard. Het is vast een elite-klodder. Vrolijk "
  "een elite op.",
  [D("elite", "gebied:klodderweide", 1, "Vrolijk een elite-wezen op in de Klodderweide")],
  [("npc", "Zzz... huh? O. Hoi. Sorry. Ik slaap al drie nachten niet."),
   ("npc", "Elke nacht: SNOOOORK. Uit de Klodderweide. Het is vast zo'n grote elite-klodder met een gloed eromheen. Kun jij hem "
           "opvrolijken? Blije klodders snurken zachter.")],
  "Een elite-klodder in de wei. Je herkent ze aan hun gloed en hun grote naam. Zzz...",
  [("npc", "Het is stil! Heerlijk stil! Ik ga meteen slapen. Hier, neem dit maar, zzz...")],
  xp=500, munten=400, loot="ongewoon", lv=4, na=["hq06_zwamzoet"])

Q("z14_kistjes", "zij", "koopman_kees", "Kistjes in de grot",
  "Het karretje van Koopman Kees is in de Druipgrotten omgevallen. Vier kistjes zijn zoek, en de grotwezens hebben ze gevonden.",
  [D("verzamel", "guhs:vr_npc_kistje", 4, "Kistjes van Kees", "gebied:druipgrotten", 0.3)],
  [("npc", "Ramp! Drama! Mijn karretje is in de Druipgrotten de helling af gerold. Vier kistjes kwijt!"),
   ("npc", "De grotwezens hebben ze vast meegenomen. Vrolijk ze op en kijk of ze iets laten vallen. Ik betaal goed! Nou ja, redelijk.")],
  "Vier kistjes uit de Druipgrotten! Er zitten mijn beste spullen in.",
  [("npc", "Mijn kistjes! Alles heel? Ja! Hier, je beloning. En korting, voor altijd. Tien procent. Op niks. Grapje!")],
  xp=1300, munten=1500, lv=13, na=["hq06_zwamzoet"])

Q("z15_lichtjes", "zij", "weerguh_wolkje", "Lichtjes in het rijk",
  "Weerguh Wolkje wil dat het rijk weer gloeit. Steek een kampvuur aan in de Klodderweide, het Sporenwoud en de Druipgrotten.",
  [D("kampvuur", "klodderweide", 1, "Een kampvuur in de Klodderweide"), D("kampvuur", "sporenwoud", 1, "Een kampvuur in het Sporenwoud"),
   D("kampvuur", "druipgrotten", 1, "Een kampvuur in de Druipgrotten")],
  [("npc", "Sinds de draak overkwam, is de lucht grijzig. Ik meet het elke dag. Grijzigheid: zeven. Te hoog!"),
   ("npc", "Kampvuren helpen. Als er in elk gebied weer eentje brandt, wordt de lucht vanzelf vadser. Weerkundig bewezen!")],
  "Een kampvuur in de wei, het woud en de grotten. Bij elk vuur staat een ridder-guh.",
  [("npc", "Grijzigheid... twee! Kijk, de wolken worden al roze. Je bent een held, en een weerkundige!")],
  xp=900, munten=700, lv=12, na=["hq06_zwamzoet"])

Q("z16_verstoppertje", "zij", "tweeling_tip", "Verstoppertje",
  "Tip speelt verstoppertje met zijn tweelingzus Tap, maar hij kan haar al twee dagen niet vinden. Ze zit ergens in het "
  "Sporenwoud.",
  [D("praat", "tweeling_tap", 1, "Vind Tap in het Sporenwoud")],
  [("npc", "Ik ben Tip! We spelen verstoppertje! Al twee dagen! Ik kan Tap niet vinden. Ze is heel goed."),
   ("npc", "Ze verstopt zich altijd bij paddenstoelen. Dus... in het Sporenwoud? Kun jij zoeken? Ik tel wel. Een, twee, drie...")],
  "...honderdvierenveertig, honderdvijfenveertig... Heb je haar al?",
  [("npc", "Je hebt haar gevonden! Ze zegt dat ze gewonnen heeft. Ik zeg van niet. Hier, voor de moeite!")],
  xp=550, munten=350, lv=6, na=["hq06_zwamzoet"],
  praat={"tweeling_tap": [("npc", "Hihi! Gevonden! Zeg maar tegen Tip dat ik gewonnen heb. Ik blijf nog even, mijn paddenstoel is "
                                    "zo gezellig.")]})

Q("z17_koning", "zij", "wachter_dikkie", "Koning Klodder",
  "In de Klodderweide woont Koning Klodder, de grootste klodder van het rijk. Hij is grauw en knorrig. Vrolijk hem op, liefst met "
  "vrienden.",
  [D("baas", "gebied:klodderweide", 1, "Vrolijk Koning Klodder op")],
  [("npc", "Hoor je dat gerommel? Dat is Koning Klodder, in zijn arena midden in de wei. Zo groot als een huis. Met een zolder."),
   ("npc", "Hij splitst zich in drie kleurtjes, en die moet je tegelijk opvrolijken. En als hij springt, wordt de hele wei een "
           "schaduw. Zoek dan een veilig plekje! Neem vrienden mee.")],
  "Koning Klodder zit in zijn arena in de Klodderweide. Op het bord bij de deur zie je wanneer hij wakker is.",
  [("npc", "Je hebt de koning opgevrolijkt! Ik hoorde hem tot hier giechelen. Hier, je verdient een beloning van de Wacht.")],
  xp=1500, munten=1200, loot="episch", lv=6, na=["hq06_zwamzoet"])

Q("z18_premie_woud", "zij", "klerk", "Premie: het Sporenwoud",
  "Het questbord: premie voor wie vijfentwintig wezens opvrolijkt in het Sporenwoud.",
  [D("opvrolijk", "gebied:sporenwoud", 25, "Vrolijk wezens op in het Sporenwoud")],
  [("npc", "Premie! Gestempeld en goedgekeurd. Vijfentwintig wezens opvrolijken in het Sporenwoud. Even tekenen hier."),
   ("jij", "Waar moet ik tekenen?"),
   ("npc", "Hier. En hier. En hier. Klaar! Stempeltje erop.")],
  "Vijfentwintig in het Sporenwoud. Ik heb je stempelkaart klaarliggen.",
  [("npc", "Vijfentwintig! Stempel, stempel, stempel... Alsjeblieft, je premie. Gestempeld.")],
  xp=900, munten=800, lv=7, na=["hq07_littekens"])

Q("z19_premie_grotten", "zij", "klerk", "Premie: de Druipgrotten",
  "Het questbord: premie voor wie vijfentwintig wezens opvrolijkt in de Druipgrotten.",
  [D("opvrolijk", "gebied:druipgrotten", 25, "Vrolijk wezens op in de Druipgrotten")],
  [("npc", "Nieuwe premie! De Druipgrotten. Vijfentwintig wezens. Natte voeten gegarandeerd. Tekenen hier.")],
  "Vijfentwintig in de Druipgrotten. Neem een handdoekje mee.",
  [("npc", "Druipnat, maar klaar! Stempel. Premie. Alsjeblieft.")],
  xp=1600, munten=1300, lv=13, na=["z18_premie_woud"])

Q("z20_premie_tempel", "zij", "klerk", "Premie: de Zwamtempel",
  "Het questbord: premie voor wie vijfentwintig wezens opvrolijkt in de Zwamtempel.",
  [D("opvrolijk", "gebied:zwamtempel", 25, "Vrolijk wezens op in de Zwamtempel")],
  [("npc", "De zwaarste premie van het bord: de Zwamtempel. Vijfentwintig tempelwezens. Ik stempel met trillende poot.")],
  "Vijfentwintig in de Zwamtempel. Pas op voor de sporenpriesters!",
  [("npc", "Jij bent een legende. Ik stempel je kaart met mijn GOUDEN stempel. Die gebruik ik bijna nooit.")],
  xp=2500, munten=2500, loot="zeldzaam", lv=18, na=["z19_premie_grotten"])

# ===== the mount quest (lv 8) ===============================================================================================
Q("rijdier_hoedhupper", "rijdier", "stalmeester", "Een eigen Hoedhupper",
  "Stalmeester Hoefje helpt je aan je eerste rijdier: een Hoedhupper. Vind de wilde Hoedhupperwei in de Klodderweide en verzamel "
  "wortelzwammen om er eentje te lokken.",
  [D("geheim", "poi:hoedhupperwei", 1, "Vind de Hoedhupperwei"),
   D("verzamel", "guhs:vr_npc_wortelzwam", 5, "Wortelzwammen", "gebied:sporenwoud", 0.4)],
  [("npc", "Zo, jij bent sterk genoeg geworden voor een eigen rijdier. Tijd voor een Hoedhupper!"),
   ("npc", "Een Hoedhupper is een paddenstoel met pootjes. Hij springt hoog en rent hard, en hij wordt sterker als jij met hem "
           "rijdt. In de Klodderweide is een wei waar ze wild rondhuppen."),
   ("npc", "Neem vijf wortelzwammen mee uit het Sporenwoud. Daar is geen Hoedhupper tegen bestand.")],
  "Vind de Hoedhupperwei in het zuidoosten van de Klodderweide, en vijf wortelzwammen uit het Sporenwoud.",
  [("npc", "Kijk eens wie er achter je aan gehupt is! Hij heeft je gekozen. Dat zie ik aan zijn hoedje."),
   ("npc", "Verzorg hem goed. Hoe meer je met hem rijdt, hoe sterker hij wordt. Fluit maar als je hem nodig hebt.")],
  xp=800, munten=500, lv=8, na=["hq06_zwamzoet"], actie_klaar="rijdier:hoedhupper")

# ===== job advancements at lv 10 and 20 per class (klassen slice does the ceremony) ========================================
KLASSEN = {
    "knuffelridder": ("Knuffelridder", "Kussenkampioen", "Vahoege Paladijn", "Meester Kussensloop"),
    "pluisjager": ("Pluisjager", "Pluizenschutter", "Sterrenjager", "Juf Pijltje"),
    "vadsmagier": ("Vadsmagiër", "Roze Tovenaar", "Vahoege Aartsmagiër", "Opa Sterrenstof"),
}
for k, (naam, t1, t2, leraar) in KLASSEN.items():
    Q(f"adv10_{k}", "klasse", "grootmeester", f"De proef van de {t1}",
      f"Je bent sterk geworden als {naam}. Bewijs het in het Sporenwoud, dan word je {t1}.",
      [D("level", "*", 10, f"Bereik level 10 als {naam}"), D("opvrolijk", "gebied:sporenwoud", 15, "Vrolijk wezens op in het Sporenwoud")],
      [("npc", f"Ik zie het aan je houding: je bent een echte {naam} geworden. Maar een titel moet je verdienen."),
       ("npc", f"Bereik level 10 en vrolijk vijftien wezens op in het Sporenwoud. Dan word je {t1}. {leraar} zou trots zijn.")],
      f"Level 10 als {naam}, en vijftien wezens in het Sporenwoud.",
      [("npc", f"Kniel, avonturier. Nou ja, ga een beetje zitten. Vanaf nu ben je {t1}!")],
      xp=600, munten=500, lv=8, klasse=k, na=["hq07_littekens"], actie_klaar=f"advancement:{k}:1")
    Q(f"adv20_{k}", "klasse", "grootmeester", f"De proef van de {t2}",
      f"De hoogste titel van zone 1: {t2}. Bereik level 20 en vrolijk de Opperzwam op.",
      [D("level", "*", 20, f"Bereik level 20 als {naam}"), D("baas", "gebied:zwamtempel", 1, "Vrolijk de Opperzwam op")],
      [("npc", f"Er is één titel die maar weinigen dragen: {t2}. Daarvoor moet je de sterkste zijn van het hele rijk."),
       ("npc", "Bereik level 20 en vrolijk de Opperzwam op in de Zwamtempel. Dan houd ik de grote ceremonie.")],
      f"Level 20 als {naam}, en de Opperzwam in de Zwamtempel.",
      [("npc", f"De klokken luiden, de oortjes wapperen. Ik benoem je tot {t2}! Het hele Vadsrijk is trots op je.")],
      xp=2000, munten=2000, lv=18, klasse=k, na=[f"adv10_{k}"], actie_klaar=f"advancement:{k}:2")

# ===== the secret: who is Akim de Kaasguh? (bazen owns the spy and his rooftop fight; we run the clues) =====================
Q("geheim_spion", "geheim", "grootmeester", "Wie is Akim de Kaasguh?",
  "Er klopt iets niet in Zwamzoet. Overal in het rijk liggen vreemde aanwijzingen: zwarte pluis, pootafdrukken, een gescheurde "
  "vermomming... Vind ze alle vijf. Dan weet je wie de verrader is, en kun je hem ontmaskeren.",
  [D("geheim", "aanwijzing", 5, "Vind de vijf aanwijzingen"),
   D("meld", "spion:ontmaskerd", 1, "Ontmasker Akim de Kaasguh op de markt (klik op hem)"),
   D("meld", "spion:gevangen", 1, "Vang de Mika-spion op de daken van Zwamzoet")],
  [("npc", "...")],
  "Een Mika-spion in onze eigen stad... Vind hem, avonturier. De Orde rekent op je.",
  [("npc", "Een Mika-spion! In onze eigen stad, op onze eigen markt. Met kaas! En niemand had het door."),
   ("jij", "Hij had een zonnebril op. Binnen."),
   ("npc", "Dat had ons op moeten vallen. Hij bespioneerde de tempel en de Knuffeldeur. De Mika's willen iets in de ordetempel. "
           "Maar wat?"),
   ("npc", "Dankzij jou weten we nu dat ze overal zijn. De Orde zal waakzamer zijn. Hier, een beloning voor de scherpste speurneus "
           "van het Vadsrijk.")],
  xp=2500, munten=2000, loot="episch", auto=True)

# ---------------------------------------------------------------------------------------------------------------------------
# Secrets: names + found texts
# ---------------------------------------------------------------------------------------------------------------------------
KNUFFELS = {
    "vuurtoren": "Meneer Pluis", "tentje": "Juffrouw Oortje", "plein": "Kapitein Knuf", "steeltje": "Bolletje", "haven": "Zeebeer Sokje",
    "slaapheuvel": "Slaapmuts", "weidevijver": "Klodderkusje", "holle_boom": "Mosje", "druipnis": "Druppie", "tempeltrap": "Prinses Zwam",
}
AANWIJZINGEN = {
    "pluisplukje": ("Zwarte pluis onder de kraam", "Onder de kaaskraam van Akim de Kaasguh ligt een plukje pluis. Zwart. En er zitten "
                                                   "kattenhaartjes in. Kaas maakt toch geen haartjes?"),
    "pootjes": ("Pootafdrukken in het pakhuis", "In het pakhuis van De Haven staan pootafdrukken. Vier kleine teentjes en een rond "
                                                "kussentje. Geen guh. Geen klodder. Iets met... kattenpootjes?"),
    "vermomming": ("Een gescheurde vermomming", "In een holle paddenstoel ligt een gescheurde cape met nep-guhoortjes eraan genaaid. "
                                                "Op het labeltje staat: 'Eigendom van A. Niet wassen.'"),
    "krassen": ("Krassen in de kristalkamer", "In de kristallen staan krassen. Krabsporen. Iemand heeft hier kristallen losgekrabd en "
                                              "in een zak gestopt. Er ligt een strohoedje-strootje."),
    "uitkijkpost": ("Een uitkijkpost op de oostzwam", "Op de hoed van de oostzwam staat een verrekijker, gericht op de Knuffeldeur "
                                                       "van de ordetempel. Ernaast: een kaasbroodje en een briefje 'Hoofdkwartier: "
                                                       "deur zit op slot. Miauw. A.'"),
}
SCHATTEN = {
    "kristalkamer": ("De kristalkamer", "Tussen de gloeiende kristallen ligt iets heel bijzonders."),
    "waterval": ("Achter de waterval", "Achter het watergordijn ligt een vergeten schatkist van de Orde!"),
    "slaapheuvel": ("In de slapende slijmheuvel", "Diep in de slapende reuzenslijm-heuvel, waar het zachtjes snurkt. Plakkerig, maar "
                                                  "de moeite waard!"),
    "schildpad": ("Op het schildpad-eiland", "Helemaal op het verre schildpad-eiland. Hoe ben je hier gekomen? Zwemmend? Vahoeg!"),
    "vuurtorenkelder": ("De kelder van de vuurtoren", "Onder de vuurtoren van Knuffelkamp. Iemand heeft hier ooit een kistje verstopt."),
    "hoedenschat": ("De verstopte hoedenhut", "Hoog op een eenzame zwamhoed in het Sporenwoud, aan het eind van een smal plankenpaadje. "
                                              "Wie heeft dit hutje hier gebouwd?"),
}

# ---------------------------------------------------------------------------------------------------------------------------
# Tips (kern Tips.toon, keys vadsrijk.npc.tip.<id> + .titel). Triggered by the npc slice: quests, loot, flauw, elementen, skill.
# The rest are written here for the slices that own the system (NOTITIES_NPC.md: who calls which).
# ---------------------------------------------------------------------------------------------------------------------------
TIPS = {
    "skill": ("Je eerste vaardigheid", "Druk op R, V of G voor je vaardigheden en op Z voor je ultieme vaardigheid. Ze kosten energie "
                                      "en hebben een korte afkoeltijd. Je ziet ze onderaan in je balk."),
    "loot": ("Buit!", "De kleur van de lichtstraal vertelt hoe zeldzaam iets is: grijs, groen, blauw, paars, oranje en heel soms roze "
                      "(Guhddelijk!). Buit is van jou alleen: niemand kan hem pakken."),
    "levelop": ("Level omhoog!", "Je bent sterker geworden en hebt een skillpunt gekregen. Zet het in je skillboom bij de ordetempel."),
    "kampvuur": ("Kampvuur", "Rechtsklik op een kampvuur: daar word je wakker als je flauwvalt. Het vult ook je Vads en energie."),
    "skillboom": ("Je skillboom", "Met skillpunten kies je varianten van je vaardigheden en handige passieven. Opnieuw kiezen is gratis, "
                                  "één keer per uur."),
    "sterren": ("Sterren", "Bij de smid zet je sterren op je spullen. Spannend: soms valt er een ster af, en heel soms raakt iets "
                           "verpluisd. Verpluisd is nooit voor altijd: de smid klopt het weer glad."),
    "markt": ("De markt", "In het veilinghuis koop en verkoop je buit met Vadsmunten. Gebonden buit blijft bij jou."),
    "party": ("Samen op pad", "In een groep tot vijf deel je xp in de buurt, tel je samen voor opvrolijk-quests en doen jullie elkaar "
                              "nooit pijn. /guhs vadsrijk party uitnodigen <naam>."),
    "flauw": ("Flauwgevallen", "Geen zorgen: je verliest niks. Je wordt wakker bij je laatste kampvuur en bent 30 tellen een beetje "
                               "duizelig."),
    "elementen": ("Elementen", "Vuur wint van zwam, zwam van slijm, slijm van vuur (x1,5). Roze is sterk tegen alles wat ontvadst is. "
                               "Het gekleurde icoontje voor de naam van een wezen vertelt zijn element."),
    "quests": ("Je questlog", "Rechtsboven staat altijd je volgende stap: wat, bij wie en waar, met een pijltje. Een roze lichtstraal "
                              "wijst de plek aan. Druk op J voor je questlog. Een ! boven een guh is een nieuwe quest, een ? betekent: "
                              "hier inleveren of even praten."),
}

# ---------------------------------------------------------------------------------------------------------------------------
# The Avonturiersgids (questlog tab "Gids"; QuestlogScherm.GIDS has the same ids)
# ---------------------------------------------------------------------------------------------------------------------------
GIDS = {
    "begin": ("Welkom in het Vadsrijk", "Het Vadsrijk is een eigen wereld vol avontuur. Je normale spullen blijven veilig thuis; hier "
                                         "heb je je avonturiersinventaris. Volg de gouden ! en de verhaalquests: die leiden je door "
                                         "Knuffelkamp naar Zwamzoet en verder. Druk op J voor je questlog. Zit je vast? Druk op U."),
    "vastzitten": ("Vastzitten? Druk op U", "Zit je vast in een kuil, tussen rotsen, in het water of ergens waar je niet meer uit kunt "
                                             "klimmen? Druk op U (Losknuffelen; ook /guhs vadsrijk vast) en blijf 3 tellen stilstaan. "
                                             "De knuffelwind zet je dan op het dichtstbijzijnde veilige plekje, of bij je kampvuur als er "
                                             "geen plekje dichtbij is. Het kan elke minuut, maar niet tijdens een bazengevecht. De toets "
                                             "kun je veranderen in Opties > Besturing > Vadsrijk."),
    "klassen": ("Klassen", "Knuffelridder (dichtbij, stevig), Pluisjager (van ver, beweeglijk) en Vadsmagiër (elementen en heling). "
                           "Elke klasse heeft een eigen level. Wisselen doe je in de ordetempel van Zwamzoet. Op level 10 en 20 krijg "
                           "je een nieuwe titel bij de Grootmeester."),
    "vaardigheden": ("Vaardigheden", "Linkerklik is je gewone aanval; elk wapen heeft zijn eigen aanvalssnelheid (zie de tooltip, het kruisje "
                                     "laat zien wanneer je weer kunt). R, V en G zijn je vaardigheden (ze kosten energie), Z is je "
                                     "ultieme vaardigheid: die laadt op door te vechten. C is de Knuffelsprong: een grote sprong vooruit, elke klasse, "
                                     "zonder valschade. De toetsen kun je zelf instellen."),
    "loot": ("Buit en zeldzaamheid", "Grijs (gewoon), groen (ongewoon), blauw (zeldzaam), paars (episch), oranje (legendarisch) en roze "
                                     "(guhddelijk). Buit is persoonlijk. Bij een baas krijgt iedereen die genoeg hielp een eigen kist."),
    "sterren": ("Sterren en de smid", "De smid zet sterren op je spullen, met een beetje risico. Verpluisde spullen repareert hij. Je kunt "
                                      "ook de bonussen opnieuw laten rollen."),
    "kampvuren": ("Kampvuren", "In elk gebied staan 2-3 kampvuren met een ridder-guh. Rechtsklik: dan is het je checkpoint. Er is geen "
                               "snelreizen; het luchtschip brengt je tussen Knuffelkamp en Zwamzoet."),
    "flauw": ("Flauwvallen", "Valt je Vads op nul, dan word je wakker bij je kampvuur (bij een baas: bij de arenadeur). Je verliest niks, "
                             "je bent alleen 30 tellen duizelig."),
    "elementen": ("Elementen", "Vuur > Zwam > Slijm > Vuur: x1,5 schade op een zwakte en x0,7 de andere kant op. Roze is extra sterk "
                               "tegen ontvadste wezens. Het icoontje voor de naam van een wezen (en op je wapen) is zijn element: "
                               " vlammetje = Vuur,  blaadje = Zwam (natuur),  druppel = Slijm (water),  hartje = Roze. "
                               "Geen icoontje = geen element."),
    "statussen": ("Statussen", "Brandend en Gesporen doen schade over tijd, Plakkerig maakt traag, Verdoofd houdt je even stil. "
                               "Teambuffs: Vahoeg (meer schade), Knuffelschild (vangt klappen op) en Snelpootjes (sneller)."),
    "party": ("Samen spelen", "Een groep is maximaal vijf avonturiers. Je deelt xp in de buurt en opvrolijk-voortgang, en je kunt elkaar "
                              "nooit pijn doen. Bazen worden sterker met meer spelers in de arena."),
    "quests": ("Quests", "Gouden ! = verhaal, gele ! = zijquest, ? = inleveren of even praten. Questdialogen klik je door; "
                         "tussenfilmpjes lopen vanzelf. Je questlog (J) laat zien wat je nog moet doen."),
    "munten": ("Vadsmunten", "Brons, zilver en goud met een guhgezichtje. Honderd brons is een zilver, honderd zilver is een goud. Ze "
                             "gaan niet mee naar de gewone wereld."),
    "markt": ("Markt en handel", "Ruil vrij met andere spelers, of zet je buit in het veilinghuis van Marktmeester Hamertje-Tik. "
                                 "Gebonden buit kan niet verhandeld worden."),
    "rijdieren": ("Rijdieren", "Rond level 8 helpt Stalmeester Hoefje je aan een Hoedhupper. Rijdieren hebben eigen snelheid en sprong "
                               "en worden sterker met jou mee."),
    "geheimen": ("Geheimen", "Overal in het rijk liggen geheimen: tien verloren knuffels, vijf vreemde aanwijzingen en verborgen "
                             "schatten. Ze glinsteren een beetje als je dichtbij bent. Wie weet waar de aanwijzingen naartoe leiden..."),
}

# ---------------------------------------------------------------------------------------------------------------------------
# The bestiary pages (questlog tab "Wezens": vadsrijk.npc.wezen.<entity path>). The mob ids are the wezens/bazen slices'
# (guessed from the contract prefixes; integration aligns them: NOTITIES_NPC.md).
# ---------------------------------------------------------------------------------------------------------------------------
WEZENS = {
    "vr_mob_klodder": "Een gewone klodder uit de Klodderweide. Ontvadst is hij grijs en knorrig; opgevrolijkt stuitert hij weer in "
                      "zijn eigen kleurtje. Eet graag bolletjes wol.",
    "vr_mob_kloddertje": "Een heel klein klodderje. Komt vaak met z'n allen, of uit een Moederklodder.",
    "vr_mob_bubbelklodder": "Blaast bubbels als hij boos is. De bubbels plakken. Opgevrolijkt blaast hij hartjesbubbels.",
    "vr_mob_stuiterklodder": "Stuitert. Altijd. Ook als hij slaapt. Kijk naar waar hij gaat landen.",
    "vr_mob_moederklodder": "Een elite: een grote klodder met kleintjes om zich heen. Snurkt 's nachts verschrikkelijk hard.",
    "vr_mob_hoedjesbol": "Een zwammetje met een te grote hoed. Hij verliest hem als hij weer vrolijk wordt.",
    "vr_mob_sporenpuffer": "Niest sporenwolkjes. Gesporen! Ga uit de wolk en vecht verder.",
    "vr_mob_zwamwachter": "De bewaker van het Sporenwoud. Stevig, langzaam, en eigenlijk heel verlegen.",
    "vr_mob_mosgeest": "Een zwevend plukje mos met oogjes. Ontvadst fluistert hij grauwe dingen; opgevrolijkt neuriet hij.",
    "vr_mob_druipslijm": "Druipt van het plafond van de Druipgrotten. Kijk omhoog!",
    "vr_mob_kristalklodder": "Een klodder vol kristallen. Glinstert prachtig, en is daar heel trots op.",
    "vr_mob_knaag": "Een knaagdiertje dat aan grauwe pluis heeft geknabbeld. Dat had hij beter niet kunnen doen.",
    "vr_mob_glimzwam": "Een gloeiende zwam diep in de grotten. Zijn licht wordt grijs als hij boos is.",
    "vr_mob_tempelridder": "Een ridder van de Zwamtempel. Draagt een hoed als helm en een steel als lans.",
    "vr_mob_sporenpriester": "De priester van de Opperzwam. Zegent zijn vrienden en vervloekt jou een beetje.",
    "vr_klodderreus": "De Grauwe Klodderreus: een klont zwarte pluis van de draak, in de kampvijver gevallen. Opgevrolijkt werd hij de "
                      "Kampklodder van Knuffelkamp.",
    "vr_baas_koning_klodder": "Koning Klodder, de wereldbaas van de Klodderweide. Splitst zich in drie kleuren die je samen moet "
                              "opvrolijken.",
    "vr_baas_opperzwam": "De Opperzwam, de oude hoeder van de Zwamtempel. Ontvadst door een Mika-speldje.",
    "vr_baas_mika_spion": "Een Mika vermomd als marktguh. Zonnebril, strohoed, nepsnor. Miauwt als hij zenuwachtig is.",
    "vr_proefklodder": "Een oefenklodder van de Orde. Hij vindt het niet erg.",
    "vr_proefkloddertje": "Een heel klein oefenklodderje.",
}

# ---------------------------------------------------------------------------------------------------------------------------
# All other texts
# ---------------------------------------------------------------------------------------------------------------------------
OVERIG = {
    "entity.guhs.vr_npc_guh": "Guh",
    "key.guhs.vr_questlog": "Questlog (Vadsrijk)",
    P + "jij": "Jij",
    P + "verteller": "Verteller",
    P + "voortgang": "%s: %s/%s",
    P + "quest_nieuw": "Nieuwe quest: %s",
    P + "quest_klaar": "Quest klaar: %s! Ga terug naar %s.",
    P + "quest_af": "Quest voltooid: %s. Vahoeg!",
    P + "escorte.volgt": "%s loopt met je mee!",
    P + "optie.hoofd": "Vahoeg, ik doe het!",
    P + "optie.ja": "Ik help je!",
    P + "optie.later": "Nu even niet",
    P + "optie.naar_stad": "Naar Zwamzoet!",
    P + "optie.naar_kamp": "Naar Knuffelkamp",
    # (fix round 2) the puzzle rooms: this line stays at the bottom of the screen while you are inside (npc/KamerTips)
    P + "kamer.lichtzwamkamer": "Lichtzwamkamer: kijk welke lichtzwammen na elkaar oplichten en steek ze in dezelfde volgorde aan "
                                "(rechtsklik). Fout? Dan laten ze het opnieuw zien.",
    P + "kamer.hendelkamer": "Hendelkamer: de zwammen op de verre muur verklappen welke hendels AAN moeten, maar gespiegeld! Zet de "
                             "hendels goed en stap dan op de drukplaat.",
    P + "kamer.ridderzaal": "Ridderzaal: vrolijk alle drie de golven tempelridders op. Dan gaat de poort naar de Opperzwam open.",
    # (fix round 2) the airship NPCs: one real choice per destination; a locked line says why when you pick it
    P + "optie.vlieg.zwamzoet": "Naar Zwamzoet (De Haven)",
    P + "optie.vlieg.zwamzoet.slot": "Naar Zwamzoet (nog op slot)",
    P + "optie.vlieg.knuffelkamp": "Naar Knuffelkamp",
    P + "optie.vlieg.knuffelkamp.slot": "Naar Knuffelkamp (nog op slot)",
    P + "optie.vlieg.kandijkom": "Naar Kandijkom (De Splinterpieken)",
    P + "optie.vlieg.kandijkom.slot": "Naar Kandijkom (nog op slot)",
    P + "reis.kies.kapitein": "Waar wil je heen? Kies hieronder een bestemming of bekijk de reiskaart. (Luid je de scheepsbel op "
                              "de steiger, dan krijg je de reiskaart ook.)",
    P + "reis.kies.luchtschipper": "Waar wil je heen? Kies hieronder een bestemming of bekijk de reiskaart. (Luid je de scheepsbel op "
                                   "de steiger, dan krijg je de reiskaart ook.)",
    P + "reis.slot.zwamzoet.kort": "Nog niet ontdekt. Het verhaal van Knuffelkamp brengt je hier.",
    P + "reis.slot.kandijkom_verhaal.kort": "Nog niet ontdekt. Maak eerst het verhaal van Zwamzoet af.",
    P + "reis.slot.kandijkom_level.kort": "Nog niet ontdekt. Vanaf level 20 (jij: level %s).",
    # (fix round 2) the travel map (npc/Reiskaart + client/ReiskaartScherm; art tools/features/vadsrijk_reiskaart_art.py)
    P + "optie.reiskaart": "Laat de reiskaart zien",
    P + "reiskaart.titel": "Reiskaart van het Vadsrijk",
    P + "reiskaart.uitleg": "Houd de muis ingedrukt en sleep om de kaart te verschuiven. Klik op een bestemming om te vliegen.",
    P + "reiskaart.jij": "Jij bent hier",
    P + "reiskaart.hier": "Hier ben je nu.",
    P + "reiskaart.vlieg": "Klik om hierheen te vliegen!",
    P + "reiskaart.binnenkort": "Nog niet ontdekt. Binnenkort...",
    P + "reiskaart.plek.knuffelkamp": "Knuffelkamp",
    P + "reiskaart.plek.zwamzoet": "Zwamzoet (De Haven)",
    P + "reiskaart.plek.kandijkom": "Kandijkom",
    P + "reiskaart.plek.schemerwortel": "De Schemerwortel",
    P + "reiskaart.plek.zone4": "???",
    P + "reis.slot.zwamzoet": "Nog niet, rekruut! Eerst moet Knuffelkamp veilig zijn. Volg Sergeant Pluizebol en het verhaal van het "
                              "kamp (je tracker rechtsboven), daarna vlieg ik je naar Zwamzoet.",
    P + "reis.slot.kandijkom_verhaal": "Kandijkom? Daar vlieg ik pas heen als het verhaal van dit land af is. Help eerst Grootmeester "
                                       "Vadsvoet in Zwamzoet.",
    P + "reis.slot.kandijkom_level": "Brrr, in de Splinterpieken is het ijskoud en gevaarlijk. Kom terug als je level 20 bent (nu level %s).",
    P + "optie.rol.smid": "Naar de smidse",
    P + "optie.rol.winkel": "Laat je winkel zien",
    P + "optie.rol.wapenwinkel": "Laat je wapens zien",
    P + "optie.rol.markt": "Naar het veilinghuis",
    P + "optie.rol.kleermaker": "Laat je kleding zien",
    P + "optie.rol.bank": "Naar mijn kluis",
    P + "optie.rol.stal": "Naar de stal",
    P + "optie.startwapen.knuffelridder": "Mag ik een Kussenzwaard? (de eerste is gratis, daarna 30 brons)",
    P + "optie.startwapen.pluisjager": "Mag ik een Pluisboog? (de eerste is gratis, daarna 30 brons)",
    P + "optie.startwapen.vadsmagier": "Mag ik een Rozenstaf? (de eerste is gratis, daarna 30 brons)",
    P + "optie.rol.ordetempel": "Klasse wisselen en skillboom",
    P + "rol.nog_niet": "%s is er nog niet (dat komt met een ander deel van het Vadsrijk).",
    P + "rol.luchtschip.nog_niet": "Eerst je opleiding afmaken, rekruut! Daarna vlieg ik je overal heen. Nou ja, naar Zwamzoet.",
    P + "rol.luchtschip.vraag": "Instappen? Het luchtschip staat klaar. Windkracht vads, zicht vahoeg!",
    P + "rol.luchtschip_kamp.vraag": "Terug naar Knuffelkamp? Groet de Kampklodder van me!",
    P + "rol.klasse.kies": "Welk pad kies je? Je kunt later altijd nog wisselen in de grote ordetempel van Zwamzoet.",
    P + "rol.questbord.lijst": "Op het questbord hangt nu:%s",
    P + "rol.questbord.leeg": "Het questbord is leeg! Iedereen in Zwamzoet is geholpen. Of je moet eerst nog wat sterker worden.",
    P + "klasse.gekozen": "Je nieuwe pad begint hier!",
    P + "rijdier.later": "Je Hoedhupper komt met de stal van het Vadsrijk (economie). Hoefje heeft hem voor je gereserveerd!",
    P + "bam.aanval": "Er valt iets uit de lucht in de kampvijver! De Grauwe Klodderreus!",
    P + "opening.welkom": "Welkom in Het Vadsrijk! Sergeant Pluizebol wacht op je hier op de steiger. Rechtsboven zie je altijd je "
                        "volgende stap; J = questlog. Zit je ergens vast? Druk op U en blijf 3 tellen stilstaan.",
    P + "scene.opening.titel": "Het Vadsrijk",
    P + "scene.opening.onder": "Knuffelkamp, bij zonsopgang",
    P + "scene.draak.titel": "Vahoegnir",
    P + "scene.draak.onder": "De Vadsloze Draak",
    P + "scene.stad.titel": "Zwamzoet",
    P + "scene.stad.onder": "De stad van De Knuffelorde",
    P + "scene.kamp.titel": "Knuffelkamp",
    P + "scene.kamp.onder": "Het oefeneiland van de Orde",
    P + "scene.einde.titel": "Wordt vervolgd...",
    P + "scene.einde.onder": "Vahoegnir cirkelt verder. En ergens lachen de Mika's in hun pootjes.",
    P + "spion.titel": "De Mika-spion!",
    P + "spion.onder": "Akim de Kaasguh was een Mika!",
    P + "spion.later": "(Het gevecht op de daken van Zwamzoet komt met de bazen van het Vadsrijk.)",
    P + "spion.weet_het": "Alle vijf de aanwijzingen wijzen dezelfde kant op: naar Akim de Kaasguh op de markt. Ga hem ontmaskeren!",
    P + "spion.dialoog.0": "Akim. Ik heb zwarte pluis onder je kraam gevonden. Pootafdrukken. Een vermomming. Krassen. En je uitkijkpost.",
    P + "spion.dialoog.1": "Njeg?? Dat... dat is allemaal toeval! Ik ben een gewone guh! Ik hou van kaas! En van...",
    P + "spion.dialoog.2": "...MIAUW. Eh. Ik bedoel: VADS. Vads vads.",
    P + "spion.dialoog.3": "Die zonnebril. Doe eens af.",
    P + "spion.dialoog.4": "NOOIT! Je krijgt me nooit! De baas zal horen wat je gedaan hebt! Naar de daken!",
    P + "adv.onder": "Een nieuwe titel van De Knuffelorde!",
    # the element quiz (hq03b, QuestMotor.quiz)
    P + "quiz.vraag.zwam": "Overhoring! Een boze Hoedjesbol is van het element Zwam. Waarmee mep je hem het hardst?",
    P + "quiz.vraag.slijm": "Overhoring! Een knorrige Klodder is van het element Slijm. Waarmee mep je hem het hardst?",
    P + "quiz.vraag.vuur": "Overhoring! Een wezen met een vlammetje naast zijn levensbalk is van het element Vuur. Waarmee mep je hem het hardst?",
    P + "quiz.vuur": "Met Vuur!",
    P + "quiz.zwam": "Met Zwam!",
    P + "quiz.slijm": "Met Slijm!",
    P + "quiz.fout": "Bijna! Denk aan de driehoek: VUUR > ZWAM > SLIJM > VUUR. Probeer het nog eens!",
    P + "gids.driehoek": "wint van",
    P + "gids.driehoek.uitleg": "Sterk = x1,5 en 'Zwak!'. Andersom = x0,7. Roze is sterk tegen alles wat ontvadst is.",
    P + "knuffel.gevonden": "Verloren knuffel gevonden: %s! (%s/%s)",
    P + "aanwijzing.naam": "Aanwijzing",
    P + "aanwijzing.teller": "Aanwijzingen gevonden: %s van de %s. Er klopt iets niet in Zwamzoet...",
    P + "schat.titel": "Schat gevonden!",
    # the questlog
    P + "log.titel": "Questlog",
    P + "log.tab.quests": "Quests",
    P + "log.tab.geheimen": "Geheimen",
    P + "log.tab.wezens": "Wezens",
    P + "log.tab.gids": "Gids",
    P + "log.af": "Voltooid: %s",
    P + "log.leeg": "Geen quests op dit moment. Kijk uit naar een ! boven een guh.",
    P + "log.inleveren": "Klaar! Inleveren bij %s.",
    P + "log.soort.hoofd": "Verhaal",
    P + "log.soort.zij": "Zijquest",
    P + "log.soort.klasse": "Klassenproef",
    P + "log.soort.rijdier": "Rijdier",
    P + "log.soort.geheim": "Geheim",
    P + "log.knuffels": "Verloren knuffels: %s/%s",
    P + "log.knuffels.uitleg": "Ze glinsteren met hartjes als je dichtbij bent. Kleine Kiki mist ze vreselijk.",
    P + "log.aanwijzingen": "Vreemde aanwijzingen: %s/%s",
    P + "log.aanwijzingen.uitleg": "Nog niks gevonden. Paarse glinsters betekenen dat er iets ligt...",
    P + "log.schatten": "Verborgen schatten: %s/%s",
    P + "log.zone1": "Zone 1 is weer vads! De grauwe littekens kleuren terug.",
    P + "log.wezens.leeg": "Je hebt nog geen wezens opgevrolijkt. Elk wezen dat je opvrolijkt krijgt hier een pagina.",
    P + "log.wezens.teller": "Opgevrolijkt: %s keer",
    P + "wezen.onbekend": "Over dit wezen weet de Orde nog weinig. Vrolijk er nog een paar op!",
    P + "adv.knuffelridder.1": "Kussenkampioen", P + "adv.knuffelridder.2": "Vahoege Paladijn",
    P + "adv.pluisjager.1": "Pluizenschutter", P + "adv.pluisjager.2": "Sterrenjager",
    P + "adv.vadsmagier.1": "Roze Tovenaar", P + "adv.vadsmagier.2": "Vahoege Aartsmagiër",
}


# ---------------------------------------------------------------------------------------------------------------------------
# Where things are (fix round 1, the quest tracker + questlog + hints): full phrases "bij ... in <gebied>", from the real map
# (POI_VADSRIJK.md). vadsrijk.npc.plek.npc.<id> / .poi.<id> / .gebied.<id> / .kampvuur.<id> / .baas.<x> (QuestStap.java).
# ---------------------------------------------------------------------------------------------------------------------------
_ZZ = "in Zwamzoet"
PLEK_NPC = {
    "rekruteerder": "op de lage zuidsteiger van Knuffelkamp, waar je aankwam (onderaan de trap van het klif)",
    "kapitein": "bij zijn luchtschip op de oostelijke steiger van Knuffelkamp",
    "kok": "bij het kampvuur op het kampplein, midden in Knuffelkamp",
    "priesteres": "bij het klasse-tempeltje op de heuvel in het zuidwesten van Knuffelkamp",
    "trainer_ridder": "op het trainingsveld in het noordoosten van Knuffelkamp",
    "trainer_jager": "op het trainingsveld in het noordoosten van Knuffelkamp",
    "trainer_magier": "op het trainingsveld in het noordoosten van Knuffelkamp",
    "visser_dobber": "aan de kampvijver in het oosten van Knuffelkamp",
    "grootmeester": "in de ordetempel bovenop de hoogste zwam van Zwamzoet (neem de lift aan de voet van die zwam)",
    "smid": "in de smidse " + _ZZ,
    "winkelier": "in de kruidenierswinkel " + _ZZ,
    "wapenhandelaar": "in de uitrustingswinkel " + _ZZ,
    "marktmeester": "in het veilinghuis " + _ZZ,
    "bankier": "in de bank " + _ZZ,
    "kleermaker": "in de kleermakerij " + _ZZ,
    "klerk": "bij het questbord aan de noordkant van het plein " + _ZZ,
    "stalmeester": "in de stal " + _ZZ,
    "havenmeester": "in het havenmeesterhuisje aan de westkant van Zwamzoet",
    "luchtschipper": "bij het luchtschip in De Haven, aan de overkant van de baai ten zuiden van Zwamzoet",
    "akim_kaasguh": "bij zijn kaaskraam op de markt, aan de westkant van het plein " + _ZZ,
    "oma_truus": "op een bankje bij het plein " + _ZZ,
    "bakker_bram": "aan de zuidoostkant van het plein " + _ZZ,
    "kleine_kiki": "midden op het plein " + _ZZ,
    "dichter_daan": "aan de zuidwestkant van het plein " + _ZZ,
    "postguh_pim": "aan de noordkant van het plein " + _ZZ,
    "tuinier_tilly": "aan de oostkant van het plein " + _ZZ,
    "muzikant_mo": "op het plein " + _ZZ,
    "professor_pluis": "bij het questbord aan de noordwestkant van het plein " + _ZZ,
    "ridder_rammel": "bij de zuidpoort (de Guhpoort) van Zwamzoet",
    "kapster_fohnie": "aan de zuidwestkant van het plein " + _ZZ,
    "schilder_sien": "aan de westkant van het plein " + _ZZ,
    "slaapkop_sjors": "op een bankje aan de oostkant van het plein " + _ZZ,
    "koopman_kees": "bij de zuidpoort (de Guhpoort) van Zwamzoet",
    "weerguh_wolkje": "aan de noordkant van het plein " + _ZZ,
    "tweeling_tip": "op het plein " + _ZZ,
    "wachter_dikkie": "bij de zuidpoort (de Guhpoort) van Zwamzoet",
    "boswachter_mos": "bij het eerste kampvuur aan de oostrand van het Sporenwoud, ten westen van Zwamzoet",
    "tweeling_tap": "ergens tussen de reuzenpaddenstoelen midden in het Sporenwoud",
    "mijnwerker_mopje": "diep in de Druipgrotten (ten noordwesten van Zwamzoet): ga een grotingang in en volg de tunnels omlaag",
    "tempelwachter_steeltje": "bij de grote deur van de Zwamtempel, bovenop de berg in het noordoosten (over de drakenfossiel-brug)",
}
for _k, _w in [("weide", "de Klodderweide"), ("woud", "het Sporenwoud"), ("grotten", "de Druipgrotten"), ("tempel", "de berg van de Zwamtempel")]:
    for _i in (1, 2, 3):
        PLEK_NPC[f"kampwacht_{_k}_{_i}"] = f"bij een kampvuur in {_w}"
PLEK_POI = {
    "kampplein": "midden in Knuffelkamp, bij het kampvuur",
    "vuurtoren": "op het klif in het noordwesten van Knuffelkamp",
    "dobberhoed": "bij de vuurtoren in het noordwesten van Knuffelkamp",
    "kampvuur_weide_1": "bij het eerste kampvuur van de Klodderweide, ten oosten van Zwamzoet",
    "grotkamp": "bij het kampvuur bij de grotingang van de Druipgrotten",
    "tempelpoort": "bovenop de berg van de Zwamtempel in het noordoosten (over de drakenfossiel-brug, dan het pad omhoog)",
    "hoedhupperwei": "in het zuidoosten van de Klodderweide",
    "tuba": "bij de picknickplek in het zuidwesten van het Sporenwoud",
    "kristalkamer": "in het verste zuidwestelijke hoekje van de Druipgrotten, diep onder de grond",
    "litteken_weide": "in het zuiden van de Klodderweide (volg het grijze spoor)",
    "litteken_woud": "in het zuiden van het Sporenwoud (volg het grijze spoor)",
    "litteken_tempel": "tussen Zwamzoet en de Zwamtempel (volg het grijze spoor)",
    "uitkijk_klodderkijk": "bovenop de rotsnaald in het noordoosten van de Klodderweide",
    "uitkijk_zwamnaald": "bovenop de rotsnaald in het westen van het Sporenwoud",
    "uitkijk_druipspits": "bovenop de rotsnaald boven de Druipgrotten",
    "uitkijk_haventand": "bovenop de rotsnaald ten oosten van De Haven",
    "uitkijk_slijmnaald": "bovenop de rotsnaald in het zuidoosten van de Klodderweide",
}
PLEK_GEBIED = {
    "knuffelkamp": "in Knuffelkamp, het oefeneiland",
    "zwamzoet": "in Zwamzoet, de paddenstoelenstad",
    "haven": "in De Haven, ten zuiden van Zwamzoet",
    "klodderweide": "in de Klodderweide, ten oosten van Zwamzoet",
    "sporenwoud": "in het Sporenwoud, ten westen van Zwamzoet",
    "druipgrotten": "in de Druipgrotten, ten noordwesten van Zwamzoet",
    "zwamtempel": "bij de Zwamtempel, op de berg in het noordoosten (over de drakenfossiel-brug)",
    "proeftuin": "in de proeftuin",
    "ring": "in het Vadsrijk",
}
PLEK_KAMPVUUR = {
    "knuffelkamp": "op het kampplein, midden in Knuffelkamp",
    "zwamzoet": "op het plein van Zwamzoet",
    "haven": "in De Haven",
    "klodderweide": "in de Klodderweide, ten oosten van Zwamzoet (bij elk vuur staat een ridder-guh)",
    "sporenwoud": "aan de oostrand van het Sporenwoud, ten westen van Zwamzoet",
    "druipgrotten": "bij de grotingang van de Druipgrotten",
    "zwamtempel": "op de berg van de Zwamtempel",
}
PLEK_BAAS = {
    "vr_klodderreus": "bij de kampvijver in het oosten van Knuffelkamp",
    "knuffelkamp": "bij de kampvijver in het oosten van Knuffelkamp",
    "klodderweide": "in zijn arena midden in de Klodderweide (op het bord bij de deur zie je of hij wakker is)",
    "zwamtempel": "diep in de Zwamtempel: door de voorhal, de Lichtzwamkamer, de Hendelkamer en de Ridderzaal",
}
PLEK_OVERIG = {
    P + "plek.overal": "overal in het Vadsrijk",
    P + "plek.overal.knuffel": "overal in het Vadsrijk (ze glinsteren met hartjes als je dichtbij bent)",
    P + "plek.overal.aanwijzing": "overal in het Vadsrijk (paarse glinsters)",
    P + "plek.overal.schat": "op geheime plekjes in het Vadsrijk",
    P + "plek.training": "op het trainingsveld in het noordoosten van Knuffelkamp",
    P + "plek.daken": "op de daken van Zwamzoet",
    # the step texts (QuestStap)
    P + "stap.praat": "Praat met %s",
    P + "stap.inleveren": "Lever in bij %s",
    P + "stap.af": "Voltooid",
    P + "stap.level": "Word eerst level %s (nu %s)",
    P + "stap.level.tip": "Vrolijk wezens op %s, of help de guhs in Zwamzoet met hun zijquests.",
    P + "stap.daarna": "Praat daarna met %s, %s.",
    P + "stap.aanrader": "Aanrader: level %s (jij bent level %s)",
    P + "stap.escorte.ophalen": "Haal %s op: %s",
    P + "stap.luchtschip": "%s. Eerst met het luchtschip: praat met %s, %s.",
    P + "stap.regel": "Volgende stap: %s, %s",
    P + "stap.hint": "Weet je nog? %s, %s.",
    # the tracker + questlog
    "key.guhs.vr_volger": "Questvolger aan/uit (Vadsrijk)",
    "key.guhs.vr_straal": "Lichtstraal aan/uit (Vadsrijk)",
    P + "volg.afstand": "%s blokken",
    P + "volg.hoger": "%s blokken, %s hoger",
    P + "volg.lager": "%s blokken, %s lager",
    P + "volg.straal_aan": "Lichtstraal boven je doel: aan",
    P + "volg.straal_uit": "Lichtstraal boven je doel: uit",
    P + "log.nu": "Nu: ",
    P + "log.eerder": "Eerder in het verhaal:",
    P + "log.nog_niet": "Deze quest heb je nog niet: praat eerst met de guh hierboven.",
    P + "log.leeg.zone1": "Het verhaal van zone 1 is af! Kijk uit naar een ! boven een guh voor nog meer quests.",
    P + "log.volger.aan": "Volger: aan",
    P + "log.volger.uit": "Volger: uit",
    P + "log.straal.aan": "Straal: aan",
    P + "log.straal.uit": "Straal: uit",
    P + "log.pin.vast": "Volgen",
    P + "log.pin.los": "Niet volgen",
}


# ---------------------------------------------------------------------------------------------------------------------------
# building
# ---------------------------------------------------------------------------------------------------------------------------
def _java_ids():
    """The NPC and POI ids of NpcPlekken.java (so a quest never points at an NPC or place that is not there)."""
    with open(os.path.join(JAVA, "NpcPlekken.java"), encoding="utf-8") as f:
        src = f.read()
    npc = set(re.findall(r'NPC\.put\("([a-z_0-9]+)"', src)) | set(re.findall(r'KAMPWACHT\.put\("([a-z_0-9]+)"', src))
    poi = set(re.findall(r'POI\.put\("([a-z_0-9]+)"', src))
    return npc, poi


def teksten():
    t = dict(OVERIG)
    npc_ids, poi_ids = _java_ids()
    for id, (naam, regels) in NPCS.items():
        assert len(regels) == 3, id
        t[P + "naam." + id] = naam
        for i, r in enumerate(regels, 1):
            t[P + f"praat.{id}.{i}"] = r
    for n, id in enumerate(kampwacht_orde()):
        t[P + "naam." + id] = "Ridder " + KAMPWACHT_NAMEN[n % len(KAMPWACHT_NAMEN)]
        for i, r in enumerate(KAMPWACHT_REGELS, 1):
            t[P + f"praat.{id}.{i}"] = r
    missend = npc_ids - {k[len(P) + 5:] for k in t if k.startswith(P + "naam.")}
    assert not missend, f"NPCs without a name: {missend}"
    for id, (naam, _tex) in QUEST_ITEMS.items():
        t[f"item.guhs.vr_npc_{id}"] = naam
    for q in QUESTS:
        s = P + "q." + q["id"]
        t[s + ".titel"] = q["titel"]
        t[s + ".tekst"] = q["tekst"]
        t[s + ".bezig"] = q["bezig"]
        for i, d in enumerate(q["doelen"]):
            t[s + f".doel.{i}"] = d["tekst"]
        for i, (_w, x) in enumerate(q["aanbod"]):
            t[s + f".aanbod.{i}"] = x
        for i, (_w, x) in enumerate(q["klaar"]):
            t[s + f".klaar.{i}"] = x
        for npc, regels in q["praat"].items():
            for i, (_w, x) in enumerate(regels):
                t[s + f".praat.{npc}.{i}"] = x
    for id, naam in KNUFFELS.items():
        t[P + "knuffel." + id] = naam
    for id, (titel, tekst) in AANWIJZINGEN.items():
        t[P + "aanwijzing." + id] = tekst
        t[P + "aanwijzing.titel." + id] = titel
    for id, (naam, tekst) in SCHATTEN.items():
        t[P + "schat." + id] = tekst
        t[P + "schat.naam." + id] = naam
    for id, (titel, tekst) in TIPS.items():
        t[P + "tip." + id] = tekst
        t[P + "tip." + id + ".titel"] = titel
    for id, (titel, tekst) in GIDS.items():
        t[P + "gids." + id + ".titel"] = titel
        t[P + "gids." + id + ".tekst"] = tekst
    for id, tekst in WEZENS.items():
        t[P + "wezen." + id] = tekst
    t.update(PLEK_OVERIG)
    for groep, tabel in (("npc", PLEK_NPC), ("poi", PLEK_POI), ("gebied", PLEK_GEBIED), ("kampvuur", PLEK_KAMPVUUR), ("baas", PLEK_BAAS)):
        for id, x in tabel.items():
            t[P + f"plek.{groep}.{id}"] = x
    assert not (npc_ids - set(PLEK_NPC)), f"NPCs without a place text: {npc_ids - set(PLEK_NPC)}"
    assert not (poi_ids - set(PLEK_POI)), f"POIs without a place text: {poi_ids - set(PLEK_POI)}"
    for k, v in t.items():
        assert "%" not in v.replace("%s", ""), f"stray % in {k}"
    return t


def kampwacht_orde():
    """The campfire ridder-guhs in NpcPlekken order (each gets the next name of KAMPWACHT_NAMEN)."""
    with open(os.path.join(JAVA, "NpcPlekken.java"), encoding="utf-8") as f:
        return re.findall(r'KAMPWACHT\.put\("([a-z_0-9]+)"', f.read())


def quests_json():
    npc_ids, poi_ids = _java_ids()
    ids = [q["id"] for q in QUESTS]
    assert len(ids) == len(set(ids)), "duplicate quest id"
    uit = []
    for q in QUESTS:
        for wie in (q["gever"], q["inleveren"]):
            assert wie in npc_ids, f"{q['id']}: unknown NPC {wie}"
        for na in q["na"]:
            assert na in ids, f"{q['id']}: unknown previous quest {na}"
        for d in q["doelen"]:
            for x in (d["doel"], d["bron"]):
                if x.startswith("poi:"):
                    assert x[4:] in poi_ids, f"{q['id']}: unknown poi {x}"
            if d["soort"] in ("praat", "escorte"):
                assert d["doel"] in npc_ids, f"{q['id']}: unknown NPC {d['doel']}"
            if d["soort"] == "verzamel" and d["doel"].startswith("guhs:vr_npc_"):
                assert d["doel"][len("guhs:vr_npc_"):] in QUEST_ITEMS, f"{q['id']}: unknown quest item {d['doel']}"
        for groep in (q["aanbod"], q["klaar"], *q["praat"].values()):
            for wie, _x in groep:
                assert wie in ("npc", "jij") or wie in npc_ids, f"{q['id']}: unknown speaker {wie}"
        o = dict(id=q["id"], soort=q["soort"], gever=q["gever"], inleveren=q["inleveren"], lv=q["lv"], na=q["na"],
                 doelen=[dict(soort=d["soort"], doel=d["doel"], n=d["n"], bron=d["bron"], kans=d["kans"]) for d in q["doelen"]],
                 beloning=q["beloning"], aanbod=[w for w, _x in q["aanbod"]], klaar=[w for w, _x in q["klaar"]],
                 praat={npc: [w for w, _x in r] for npc, r in q["praat"].items()}, actie_aanname=q["actie_aanname"],
                 actie_klaar=q["actie_klaar"], start_items=q["start_items"], auto=q["auto"])
        if q["klasse"]:
            o["klasse"] = q["klasse"]
        uit.append(o)
    return {"_": "generated by tools/features/vadsrijk_npc.py; do not edit", "quests": uit}


# the hidden FTB advancements: one per quest + the secret counters
def advancements():
    namen = [f"vr_{q['id']}" for q in QUESTS] + ["vr_knuffels_alle", "vr_aanwijzingen_alle"] + [f"vr_schat_{s}" for s in SCHATTEN]
    return {f"advancement/quest/{n}.json": {"criteria": {"done": {"trigger": "minecraft:impossible"}}} for n in namen}


def _assets(w, a):
    for id, (_naam, tex) in QUEST_ITEMS.items():
        w(os.path.join(a, "items", f"vr_npc_{id}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/vr_npc_{id}"}})
        w(os.path.join(a, "models", "item", f"vr_npc_{id}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})


def build(h):
    h.w(f"{h.D}/vadsrijk/npc/quests.json", quests_json())
    for pad, obj in advancements().items():
        h.w(f"{h.D}/{pad}", obj)
    _assets(h.w, h.A)
    for k, nl in teksten().items():
        h.lang(k, nl, nl)


# ---------------------------------------------------------------------------------------------------------------------------
# FTB Quests: the chapter "Het Vadsrijk" (FTB_CHAPTER; the CHAPTERS entry is added at integration, see NOTITIES_NPC.md).
# Nothing is locked; every task is the hidden advancement the Java grants on hand-in.
# ---------------------------------------------------------------------------------------------------------------------------
FTB_CHAPTER = "guhs_vadsrijk"
FTB_PORTRAIT = "guh:golden"
FTB_SECTIES = [
    ("vr_kamp", "Knuffelkamp", "guh:normal", ["vr_binnen", "vr_hq01_welkom", "vr_hq02_pad", "vr_hq03_poppen", "vr_hq03b_elementen", "vr_hq04_kampvuur",
                                               "vr_hq05_klodderreus", "vr_k01_dobber", "vr_k02_uitzicht"]),
    ("vr_verhaal", "Het verhaal van zone 1", "guh:golden", ["vr_hq06_zwamzoet", "vr_hq07_littekens", "vr_hq08_sporenwoud",
                                                             "vr_hq09_druipgrotten", "vr_hq10_zwamtempel", "vr_hq11_opperzwam",
                                                             "vr_hq12_schaduw"]),
    ("vr_zwamzoet", "Zwamzoet helpen", "guh:mint", None),
    ("vr_klassen", "Klassen en rijdieren", "guh:snow", [f"vr_adv10_{k}" for k in KLASSEN] + [f"vr_adv20_{k}" for k in KLASSEN]
     + ["vr_rijdier_hoedhupper"]),
    ("vr_geheimen", "Geheimen", "guh:choco", ["vr_geheim_spion", "vr_knuffels_alle", "vr_aanwijzingen_alle"]
     + [f"vr_schat_{s}" for s in SCHATTEN]),
]


def ftb(fq):
    q, adv = fq.q, fq.adv
    K = "guhs:kaas_knabbels"
    q("vr_binnen", "Het Vadsrijk", "Vind de ingestorte ordetempel in de Guhmensie en stap door het slapende portaal. Je komt aan op "
      "&dKnuffelkamp&r, het oefeneiland van &dDe Knuffelorde&r. Je eigen spullen blijven veilig thuis.", "minecraft:pink_wool",
      [fq.dim("guhs:vadsrijk")], rewards=((K, 8),), shape="gear", xp=50)
    for d in QUESTS:
        tekst = d["tekst"].replace("Vadsrijk", "&dVadsrijk&r")
        icon = {"hoofd": "minecraft:golden_sword", "klasse": "minecraft:enchanted_book", "rijdier": "minecraft:saddle",
                "geheim": "minecraft:spyglass"}.get(d["soort"], "minecraft:writable_book")
        vorm = "gear" if d["soort"] == "hoofd" else ("hexagon" if d["soort"] in ("klasse", "geheim") else None)
        q(f"vr_{d['id']}", d["titel"], tekst + " &7(Vinkt zichzelf af als je hem in het Vadsrijk inlevert.)", icon,
          [fq.met(adv(f"vr_{d['id']}"), d["titel"])], rewards=((K, 8 if d["soort"] == "zij" else 16),), shape=vorm,
          xp=100 if d["soort"] == "hoofd" else 50)
    q("vr_knuffels_alle", "Alle knuffels terug", "Vind alle &e10 verloren knuffels&r van Kleine Kiki. Ze glinsteren met hartjes.",
      "guhs:knuffel_normal", [fq.met(adv("vr_knuffels_alle"), "10 knuffels")], rewards=((K, 16),), shape="hexagon")
    q("vr_aanwijzingen_alle", "Er klopt iets niet", "Vind alle &e5 vreemde aanwijzingen&r in het rijk. Waar leiden ze naartoe?",
      "minecraft:spyglass", [fq.met(adv("vr_aanwijzingen_alle"), "5 aanwijzingen")], rewards=((K, 16),), shape="hexagon")
    for s, (naam, _t) in SCHATTEN.items():
        q(f"vr_schat_{s}", naam, "Een verborgen schat van het Vadsrijk. Hij glinstert goudgeel als je dichtbij bent.", "minecraft:chest",
          [fq.met(adv(f"vr_schat_{s}"), naam)], rewards=((K, 8),))


# ---------------------------------------------------------------------------------------------------------------------------
# standalone
# ---------------------------------------------------------------------------------------------------------------------------
def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources")
    d = os.path.join(a, "data", "guhs")
    _schrijf(os.path.join(d, "vadsrijk", "npc", "quests.json"), quests_json())
    for pad, obj in advancements().items():
        _schrijf(os.path.join(d, *pad.split("/")), obj)
    _assets(_schrijf, os.path.join(a, "assets", "guhs"))
    t = teksten()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        for k in [k for k in data if k.startswith((P, "item.guhs.vr_npc_")) and k not in t and ".z2_" not in k and ":z2_" not in k]:
            del data[k]   # (texts this generator no longer writes; zone 2's keys belong to vadsrijk_z2_npc.py)
        data.update(t)
        _schrijf(p, data)
    oud = os.path.join(d, "advancement", "quest")
    for f in os.listdir(oud):
        if f.startswith("vr_") and not f.startswith("vr_z2_") and f"advancement/quest/{f}" not in advancements():
            os.remove(os.path.join(oud, f))   # (a quest or secret that is gone)
    print(f"vadsrijk_npc: {len(QUESTS)} quests, {len(NPCS)} + {len(KAMPWACHT_NAMEN)} NPC names, {len(t)} texts")


if __name__ == "__main__":
    _direct()
