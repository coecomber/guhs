"""
bbq2 (ring-kern) - every Dutch text of the core of "In de ban van de Knabbelring" (the source language; English is phase 3).
TEKSTEN: lang key -> text. ADVANCEMENTS: the visible ones of the tab knabbelring. VERBORGEN: the hidden quest/ring_* ones.
"""

KINDS = {
    "guhdalf": "Guhdalf", "smikagol": "Smikagol", "araguh": "Araguh", "leguhlas": "Leguhlas", "gimguh": "Gimguh", "boromika": "Boromika",
    "merrie": "Merrie", "pippguh": "Pippguh", "guhrond": "Guhrond", "guhladriel": "Guhladriel",
}

# what a cast character says when you just walk up to it: (before the story, on the trip, afterwards), two lines each
CAST = {
    "guhdalf": (["Een tovenaar komt nooit te laat, njeg. En ook nooit te vroeg. Hij komt precies wanneer er knabbels zijn.",
                 "Ik zoek iemand met stevige pootjes en een lege maag. Nee, wacht. Juist géén lege maag. Njeg."],
                ["Vlieg, dwaze guhs! Nou ja. Loop maar rustig. Maar wel de goede kant op.",
                 "Niet opeten, die ring. Ook niet een klein hoekje. Ik zie alles, njeg."],
                ["Gefrituurd en gedeeld. Zo hoort het met een knabbel, njeg. Ik ben trots op je.",
                 "Mijn vuurwerk is op, maar mijn trek niet. Zullen we nog een feestje bouwen?"]),
    "smikagol": (["Wij willen het... het vadsje... Niet tegen de dikke guhs zeggen, sssst!",
                  "Lekkere vissss. Rauw en glibberig. Wil je er één? Nee? Gelukkig, vahoeg."],
                 ["Deze kant op, vadsje! Smikagol weet een geheim paadje. Heel geheim. Heel glibberig.",
                  "Het is van ons... van ons! Nou ja. Van jou. Voor even. Sssst."],
                 ["Smikagol is nu goudbruin. En knapperig van buiten. Het staat ons goed, vadsje!",
                  "Wij hebben een stukje gekregen. Een héél stukje! Niemand heeft het afgepakt, njeg."]),
    "araguh": (["Ze noemen mij Guhstapper. Ik sluip. Jij hoort mij niet. Ik hoor jou wel, je smakt.",
                "Een koning? Ik? Nee hoor. Ik ben gewoon een guh met een kapje op. Kijk niet zo naar mijn kroonkruimels."],
               ["Sluipen is simpel: buik in, oren plat, en niet 'njeg' zeggen. Dat laatste is het moeilijkst.",
                "Als je door mijn leven of dood de ring kunt beschermen, dan doe ik dat. Liever door mijn leven, njeg."],
               ["De knabbelkroon zit lekker. Hij kruimelt alleen een beetje in mijn oren.",
                "Een koning deelt zijn knabbels. Dat staat in de wet. Die heb ik zelf geschreven, vahoeg."]),
    "leguhlas": (["Mijn elfenogen zien heel ver. Daar, achter die heuvel: een knabbel. Hij is van mij.",
                  "Ik heb vandaag al zeventien knabbels gevonden. Zeg dat maar niet tegen Gimguh. Of juist wel."],
                 ["Ze nemen de knabbels mee naar de Frituurberg! Wat zeggen mijn elfenogen? Dat het ver lopen is, njeg.",
                  "Eenenveertig knabbels! Gimguh zegt tweeënveertig, maar die laatste lag al op de grond."],
                 ["Uiteindelijk stond het gelijk. Dat zeg ik niet graag. Dus ik zeg het zacht.",
                  "Ik woon nu in de boomstad. Het uitzicht is goed. Ik zie vanaf hier wat jij in je rugzak hebt."]),
    "gimguh": (["Niemand gooit zomaar een dwergguh! Hooguit over een heel klein greppeltje. Als niemand kijkt.",
                "En mijn bijl? Die is voor de kaas, njeg. Dikke plakken."],
               ["Tweeënveertig knabbels! Die elf telt de kruimels als hele. Zo ken ik er nog wel een paar.",
                "Onder deze berg is een geheim gangetje. Ik zeg niet waar. Het is achter die steen daar."],
               ["Gelijkspel met een elf. Ik heb er drie nachten niet van geslapen. Wel van gegeten.",
                "Mijn baard ruikt nog naar frituur. Ik was hem nooit meer, vahoeg."]),
    "boromika": (["Ik ben een Mika, ja. Maar ik steel niet. Bijna nooit. Ik oefen er heel hard op.",
                  "Eén knabbel kan je niet zomaar naar de Frituurberg brengen. Je hebt er minstens broodjes bij nodig."],
                 ["Laat mij de ring even vasthouden. Alleen vasthouden! Nee? Nee. Je hebt gelijk. Sorry. Ik schaam me, njeg.",
                  "Ik blaas op mijn hoorn als er gevaar is. Of als het etenstijd is. Meestal dat tweede."],
                 ["Ik heb een stukje gekregen. Gewoon gekregen! Zonder te stelen. Dat was nieuw voor mij.",
                  "Ik bewaak nu de koekjestrommel van de koning. Er is nog niets uit. Bijna niets."]),
    "merrie": (["Wij zoeken het tweede ontbijt. Het eerste was op voordat we wakker waren.",
                "Pippguh heeft niets gedaan. Wat het ook was. En ik stond erbij en zag het ook niet."],
               ["Zit er in jouw rugzak nog iets lekkers? We kijken alleen even. Met onze pootjes.",
                "Dat was Pippguh, met die emmer. Ik heb alleen gezegd dat hij het moest doen, njeg."],
               ["We zijn gegroeid, zie je dat? Van de knabbels van de boomguhs. Minstens een kruimel hoger.",
                "Derde ontbijt is nu ook een ding. Dat hebben wij uitgevonden, vahoeg."]),
    "pippguh": (["En het tweede ontbijt dan? En het elfuurtje? En de tussendoorknabbel?",
                 "Ik raak nooit ergens aan. Behalve aan alles. Maar altijd per ongeluk."],
                ["Een emmer? Welke emmer? O, díé emmer. Die is gevallen. Helemaal vanzelf. In die put.",
                 "Dwaze Pippguh, zegt Guhdalf dan. Maar hij zegt het lief, njeg."],
                ["Ik heb op het feest gezongen. Iedereen moest huilen. Van het lachen, zeiden ze.",
                 "Die put heb ik nooit meer gezien. Hij mij wel, denk ik."]),
    "guhrond": (["Welkom, reiziger. Dit huis is drieduizend jaar oud. De kaassaus ook. Proef maar niet.",
                 "Ik was erbij, toen. Drieduizend jaar geleden. Toen at iemand de laatste knabbel op en zei niets."],
                ["De raad heeft gesproken. Nou ja, geschreeuwd. Jij draagt de ring. Njeg.",
                 "Negen metgezellen. Laat het zo zijn. Zijn er genoeg borden?"],
                ["De ring is gedeeld. Na drieduizend jaar. Ik had hem zelf ook wel willen proeven, eerlijk gezegd.",
                 "Kom gerust langs. De watervallen zijn nu van verse kaassaus. Die mag je wél proeven."]),
    "guhladriel": (["Ik weet wat je zoekt. Het ligt in de koelkast. Bij mij weten ze altijd alles.",
                    "Zelfs de kleinste guh kan de loop van het avondeten veranderen."],
                   ["Kijk in de spiegel, als je durft. Je ziet wat was, wat is, en wat je vanavond eet.",
                    "Drie gaven geef ik je. Raak ze niet kwijt. En was het manteltje op dertig graden, njeg."],
                   ["De schaduw is weg en de frituur staat aan. Dit is een goede tijd.",
                    "Het flesje geeft nog licht, zie ik. Handig in de voorraadkast, vahoeg."]),
}

TEKSTEN = {
    # --- the ring ---------------------------------------------------------------------------------------------------------
    "item.guhs.knabbelring": "De Knabbelring",
    "item.guhs.knabbelring.lore": "Eén knabbel om ze allemaal te delen. Hij ruikt heerlijk. Te heerlijk, njeg.",
    "item.guhs.knabbelring.lore2": "Rechtsklik: omdoen of afdoen. Om: Mika's zien je niet.",
    "item.guhs.knabbelring.lore3": "Maar het Oog ziet je wél, en in de Guhbarbecuether komen de Negen.",
    "quest.guhs.ring.om": "Je doet de Knabbelring om. Niemand ziet je... behalve het Oog.",
    "quest.guhs.ring.af": "Je doet de Knabbelring af. Poeh.",
    "quest.guhs.ring.portaal_dicht": "Guhdalf heeft deze teleportatiemagie uitgezet. Misschien kan jij hem helpen een ring mee te nemen als je toch op reis gaat?",
    "quest.guhs.ring.portaal_open": "Guhdalf heeft de teleportatiemagie van het grillportaal weer aangezet. Voor jou, njeg. De reis kan beginnen!",
    "quest.guhs.ring.wegwijs": "Je verhaal is begonnen! Linksboven zie je steeds wat je moet doen. In de Guhdex (tab Verhalen) staat je reiskaart, "
                               "het Superkompas wijst naar 'Mijn verhaal' en Sam-guh weet altijd de weg. Vahoeg!",
    "quest.guhs.ring.beloning": "De Knabbelring is gefrituurd en gedeeld. Je bent nu Ringdrager! Sam-guh wil met je mee naar huis (klik op hem) "
                                "en Smikagol gaat voor je vissen. Vahoeg!",
    # the ring whispers
    "quest.guhs.ring.trek.0": "De ring fluistert: eet me op... één hapje maar... niemand ziet het...",
    "quest.guhs.ring.trek.1": "De ring ruikt opeens naar gesmolten kaas. Dat doet hij expres.",
    "quest.guhs.ring.trek.2": "Je maag knort. De ring knort terug.",
    "quest.guhs.ring.trek.3": "De ring fluistert: ik ben knapperig van buiten en zacht van binnen, njeg...",
    "quest.guhs.ring.trek.4": "Was dat een kruimel? Nee. De ring kruimelt niet. Hij plaagt alleen.",
    "quest.guhs.ring.trek.5": "De ring fluistert: Guhdalf hoeft het niet te weten...",
    "quest.guhs.ring.trek.6": "Je likt per ongeluk aan je pootje. Het smaakt naar ring. Vahoeg, wat lekker.",
    "quest.guhs.ring.trek.7": "De ring wordt een beetje warm in je zak. Hij weet dat je aan hem denkt.",
    # back to the rest point
    "quest.guhs.ring.terug.negen": "De Negen hebben je te pakken! Ze duwen je terug naar je laatste rustpunt. Niet omdoen, die ring, njeg.",
    "quest.guhs.ring.terug.oog": "Het Oog heeft je gezien! Met één blik sta je weer bij je laatste rustpunt.",
    "quest.guhs.ring.terug.gevallen": "Oeps. Sam-guh raapt je op en zet je terug bij je laatste rustpunt.",
    # the Nine
    "quest.guhs.ring.negen.waarschuwing": "Het wordt koud... Het Oog heeft je gezien. Doe de ring af, snel!",
    "quest.guhs.ring.negen.komen": "De Negen komen eraan! Ren, doe de ring af of verblind ze met het Lichtflesje!",
    "quest.guhs.ring.negen.kwijt": "De Negen zijn je kwijt. Ze snuffelen nog wat en druipen af. Poeh, njeg!",
    "quest.guhs.ring.negen.betrapt": "Een Knekel-Mika-ruiter heeft je gezien!",
    # where to go
    "quest.guhs.ring.doel.klaar": "De reis is gemaakt en de ring is gedeeld. Nu is het tijd voor feest, njeg!",
    "quest.guhs.ring.doel.niet_begonnen": "Er hangt avontuur in de lucht. Zoek Guhdalf bij de grote barbecueput in de Guhmensie.",
    "quest.guhs.ring.doel.portaal": "Daarvoor moeten we eerst door het grillportaal. Je Superkompas wijst het aan.",
    "quest.guhs.ring.doel.hier": "We zijn er: %s. Kijk maar eens goed rond.",
    "quest.guhs.ring.doel.richting": "Naar %1$s: die kant op, naar het %2$s. Nog ongeveer %3$s blokken.",
    "quest.guhs.ring.windstreek.n": "noorden", "quest.guhs.ring.windstreek.no": "noordoosten", "quest.guhs.ring.windstreek.o": "oosten",
    "quest.guhs.ring.windstreek.zo": "zuidoosten", "quest.guhs.ring.windstreek.z": "zuiden", "quest.guhs.ring.windstreek.zw": "zuidwesten",
    "quest.guhs.ring.windstreek.w": "westen", "quest.guhs.ring.windstreek.nw": "noordwesten",

    # --- the gifts --------------------------------------------------------------------------------------------------------
    "item.guhs.lichtflesje": "Lichtflesje",
    "item.guhs.lichtflesje.lore": "Het licht van Guhladriels lievelingsster, in een flesje. Rechtsklik: een flits die rook wegblaast en de Negen verblindt.",
    "item.guhs.lichtflesje.lore2": "In je hand is het een lampje dat met je meeloopt.",
    "quest.guhs.ring.lichtflesje.verblind": "Flits! De Knekel-Mika's zien even niets meer.",
    "item.guhs.elfenmanteltje": "Elfenmanteltje",
    "item.guhs.elfenmanteltje.lore": "Een manteltje van de boomguhs. Wie het bij zich heeft, bukt en stilstaat, lijkt op een rots.",
    "item.guhs.elfenmanteltje.lore2": "Het Oog en de ruiters kijken langs een rots heen. Na het verhaal krijg je het ook als kleding voor je guh.",
    "quest.guhs.ring.manteltje.hoe": "Buk (sneak) en blijf heel stil staan: dan lijk je op een rots.",
    "quest.guhs.ring.manteltje.rots": "Je lijkt op een rots. Niet bewegen, njeg!",
    "item.guhs.elfentouw": "Elfentouw",
    "item.guhs.elfentouw.lore": "Licht als een veertje en sterk als een guhknuffel. Kijk naar een Elfentouwhaak en rechtsklik: het touw trekt je erheen.",
    "item.guhs.elfentouw.lore2": "Reikt 24 blokken ver. Buk om los te laten.",
    "quest.guhs.ring.touw.geen_haak": "Het touw vindt geen haak. Kijk naar een Elfentouwhaak (hooguit 24 blokken ver).",
    "quest.guhs.ring.touw.te_ver": "Die haak is te ver weg voor het touw.",
    "block.guhs.elfentouw_haak": "Elfentouwhaak",
    "block.guhs.elfentouw_haak.lore": "Een haakje voor het Elfentouw. Hang het aan een muur, een plafond of zet het op de grond.",

    # --- rest points, Sam-guh ---------------------------------------------------------------------------------------------
    "block.guhs.ring_rustvuur": "Rustvuurtje",
    "block.guhs.ring_rustvuur.lore": "Een kookvuurtje waar reizigers uitrusten. Het brandt nooit aan je pootjes.",
    "quest.guhs.ring.rustpunt.nieuw": "Rustpunt bereikt! Word je gezien of gepakt, dan kom je hier weer terug. Sam-guh zet de pan op het vuur.",
    "quest.guhs.ring.rustpunt.gezellig": "Een gezellig kookvuurtje. Hier rusten reizigers uit, njeg.",
    "quest.guhs.ring.rustpunt.herinnering": "Hier smaakte de stoofpot van Sam-guh het allerbest.",
    "quest.guhs.ring.rustpunt.al": "De stoofpot moet nog pruttelen. Morgen is er weer.",
    "item.guhs.ring_stoofpotje": "Stoofpotje van Sam-guh",
    "item.guhs.ring_feestknabbel": "Feestknabbel",
    "quest.guhs.ring.sam.praat.0": "Ik ga niet zonder jou, baas. En ook niet zonder mijn pannen, njeg.",
    "quest.guhs.ring.sam.praat.1": "Als we thuiskomen plant ik overal knabbelstruiken. Tot aan het hek.",
    "quest.guhs.ring.sam.praat.2": "Hebben we de zoutjes wel bij ons? Zonder zout loop ik geen berg op.",
    "quest.guhs.ring.sam.praat.3": "Eén poot voor de andere, baas. En tussendoor een hapje.",
    "quest.guhs.ring.sam.praat.4": "Ik vertrouw die ring niet. Hij kijkt naar mijn worstjes.",
    "quest.guhs.ring.sam.eten.0": "Stoofpotje van de tuin, baas! Met extra knabbel. Eet maar lekker op.",
    "quest.guhs.ring.sam.eten.1": "Het is niet veel, maar het is warm. En er zit kaas in, vahoeg.",
    "quest.guhs.ring.sam.eten.2": "Aardappelen! Koken, prakken, in een stoofpot doen. Hier, voor jou.",
    "quest.guhs.ring.sam.pruttelt": "De pot moet nog pruttelen, baas. Morgen is er weer een stoofpotje.",
    "quest.guhs.ring.sam.zwaar": "Baas, je loopt zo krom. Is de ring zo zwaar? Klik maar op mij: ik kan de ring niet dragen, maar wel jou, njeg!",
    "quest.guhs.ring.sam.draag": "Ik kan de ring niet dragen, maar wel jou, njeg! Hou je vast aan mijn oren.",
    "quest.guhs.ring.sam.aangekomen": "Zo. Verder moet je zelf, baas. Mijn pootjes trillen ervan.",
    "quest.guhs.ring.sam.ring_terug": "Baas! Je liet de ring vallen. Hier. Niet opeten, njeg.",
    "quest.guhs.ring.sam.niet_omdoen": "Niet omdoen, baas! Straks ziet het Oog je. Hier in de Guhmensie kan het nog net.",
    "quest.guhs.ring.sam.andere_baas": "Ik hoor bij een andere baas, njeg. Die mag ik niet alleen laten.",
    "quest.guhs.ring.sam.getemd": "Ik ga met je mee naar huis, baas. Voor altijd. Heb je daar een tuin? Vahoeg!",

    # --- Smikagol ---------------------------------------------------------------------------------------------------------
    "entity.guhs.smikagol": "Smikagol",
    "entity.guhs.knekel_ruiter": "Knekel-Mika-ruiter",
    "item.guhs.smikagol_spawn_egg": "Smikagol-spawnei",
    "item.guhs.knekel_ruiter_spawn_egg": "Knekel-Mika-ruiter-spawnei",
    "item.guhs.ring_vissenbotje": "Smikagols vissenbotje",
    "item.guhs.ring_vissenbotje.lore": "Afgekloven tot op de graat. Rechtsklik: je eigen Smikagol komt aangescharreld.",
    "quest.guhs.ring.smikagol.niet_van_jou": "Wij kennen jou niet! Wij horen bij een ander vadsje. Sssst!",
    "quest.guhs.ring.smikagol.gids.0": "Deze kant op, vadsje! Smikagol weet de weg. Bijna altijd.",
    "quest.guhs.ring.smikagol.gids.1": "Zachtjes lopen, vadsje. Het Oog heeft hele grote oren. Nee, ogen. Eén oog.",
    "quest.guhs.ring.smikagol.gids.2": "Wij passen op het vadsje. Heel goed. Mag Smikagol hem even zien? Nee? Jammer.",
    "quest.guhs.ring.smikagol.gids.3": "Niet door het moeras met je mond open, vadsje. Dat smaakt naar oude saus.",
    "quest.guhs.ring.smikagol.kom.0": "Kom, vadsje, kom! Niet treuzelen!",
    "quest.guhs.ring.smikagol.kom.1": "Deze kant, vadsje! Smikagol wacht. Smikagol wacht niet graag.",
    "quest.guhs.ring.smikagol.kom.2": "Sssst! Hierheen! Voordat de ruiters komen!",
    "quest.guhs.ring.smikagol.blijft": "Smikagol blijft hier. Bij het water, als het mag. Lekkere vissss.",
    "quest.guhs.ring.smikagol.volgt": "Smikagol loopt mee, vadsje. Achter je. Heel dicht achter je.",
    "quest.guhs.ring.smikagol.vis": "Lekkere vissss! %s gevangen. Voor jou, vadsje. Smikagol heeft er stiekem al één op.",
    "quest.guhs.ring.smikagol.bijt_nog_niet": "Ze bijten nog niet, vadsje. Visjes hebben geen haast. Njeg.",
    "quest.guhs.ring.smikagol.maatje.0": "Breng Smikagol naar water, vadsje. Dan vangen wij vissss voor je.",
    "quest.guhs.ring.smikagol.maatje.1": "Wij zijn nu goudbruin en lief. Meestal lief.",
    "quest.guhs.ring.smikagol.maatje.2": "Buk en klik: dan blijft Smikagol zitten waar hij zit. Of loopt hij weer mee.",
    "quest.guhs.ring.smikagol.maatje.3": "Mijn vadsje... nee, óns vadsje. Delen is ook lekker, hebben wij geleerd.",
    "quest.guhs.ring.smikagol.geroepen": "Smikagol is er al, vadsje! Riep je? Is er vissss?",
    "quest.guhs.ring.smikagol.geen_maatje": "Er komt niemand. Smikagol wordt pas je maatje na het verhaal van de Knabbelring.",

    # --- the party --------------------------------------------------------------------------------------------------------
    "quest.guhs.ring.feest.nog_niet": "Het feest wacht op je, als de ring gedeeld is. Tot die tijd bewaren we een stukje taart. Een klein stukje.",
    "quest.guhs.ring.feest.morgen": "Je hebt je traktatie van vandaag al op, njeg. Morgen is er weer feest!",
    "quest.guhs.ring.feest.traktatie.0": "Feest in de Gouw! Hier, een Feestknabbel. Eén per dag, anders rol je de heuvel af.",
    "quest.guhs.ring.feest.traktatie.1": "Hiep hiep, njeg! Een Feestknabbel voor de Ringdrager.",
    "quest.guhs.ring.feest.traktatie.2": "Er is altijd wel iets te vieren. Vandaag: dat het vandaag is. Vahoeg!",

    # --- titles, Guhdex, clothes --------------------------------------------------------------------------------------------
    "gui.guhs.titels.naam.ringdrager": "Ringdrager",
    "gui.guhs.titels.hint.ringdrager": "Breng de Knabbelring naar de Frituurberg en deel hem met iedereen",
    "item.guhs.ring_guhdalfhoed": "Guhdalfs punthoed met baard",
    "item.guhs.ring_elfenmantel": "Elfenmantel met blaadjesspeld",
    "item.guhs.ring_hobbitvoeten": "Harige hobbitvoetjes",
    "item.guhs.ring_ringketting": "De ring aan een kettinkje",
    "gui.guhs.verhaal.kopie.sam_guh": "Njeg, baas? Ik pas op de pannen. Kom straks maar terug.",
}

for _kind, _naam in KINDS.items():
    TEKSTEN[f"entity.guhs.guh_npc.{_kind}"] = _naam
for _kind, (_voor, _reis, _na) in CAST.items():
    for _fase, _regels in (("voor", _voor), ("reis", _reis), ("na", _na)):
        for _i, _regel in enumerate(_regels):
            TEKSTEN[f"quest.guhs.ring.cast.{_kind}.{_fase}.{_i}"] = _regel

# the Guhdex page of Sam-guh (variant sam_guh): (name, rarity, info)
SAM_PAGINA = ("Sam-guh", "Eén per speler, na het verhaal van de Knabbelring",
              "Sam-guh, de trouwe tuinguh van de Knabbelgouw. Hij loopt de hele reis met je mee met al zijn pannen op zijn rug, kookt bij "
              "elk rustpunt een stoofpotje en weet altijd wat je nu moet doen. Als de ring te zwaar wordt, draagt hij jou. Njeg!")

# the visible advancements of the tab knabbelring: (name, parent, icon, frame, title, text). Java grants knabbelring/<name>
# together with the hidden quest/<name> (Ring.behaald).
ADVANCEMENTS = [
    ("ring_guhdalf", "root", "minecraft:firework_rocket", "task", "Een tovenaar komt nooit te laat", "Praat met Guhdalf bij de grote barbecueput"),
    ("ring_gekregen", "ring_guhdalf", "guhs:knabbelring", "task", "Eén knabbel om ze allemaal te delen", "Krijg de Knabbelring. Niet opeten, njeg"),
    ("ring_sam", "ring_gekregen", "guhs:ring_stoofpotje", "task", "Ik ga niet zonder jou", "Sam-guh loopt met je mee, met al zijn pannen"),
    ("ring_rustpunt", "ring_sam", "guhs:ring_rustvuur", "task", "Even uitpuffen", "Bereik een rustpunt: hier kom je terug als je gepakt wordt"),
    ("ring_stoofpotje", "ring_rustpunt", "guhs:ring_stoofpotje", "task", "Aardappelen!", "Laat Sam-guh bij een rustvuurtje een stoofpotje voor je koken"),
    ("ring_kwijlen", "ring_gekregen", "guhs:kaas_knabbels", "task", "Iedereen wil een hapje", "Loop met de ring langs wilde guhs: ze lopen kwijlend achter je aan"),
    ("ring_omgedaan", "ring_gekregen", "minecraft:ender_eye", "task", "Niet omdoen!", "Doe de Knabbelring toch om. Mika's zien je niet meer. Het Oog wel"),
    ("ring_negen_ontsnapt", "ring_omgedaan", "minecraft:wither_skeleton_skull", "goal", "De Negen te snel af", "Ontsnap aan de negen Knekel-Mika-ruiters"),
    ("ring_gaven", "ring_rustpunt", "guhs:lichtflesje", "task", "De gaven van Guhladriel", "Krijg het Lichtflesje, het Elfenmanteltje en het Elfentouw"),
    ("ring_verblind", "ring_gaven", "guhs:lichtflesje", "task", "Even niet kijken", "Verblind een Knekel-Mika-ruiter met het Lichtflesje"),
    ("ring_rots", "ring_gaven", "guhs:elfenmanteltje", "task", "Ik ben een rots, njeg", "Buk en sta stil met het Elfenmanteltje: je lijkt op een rots"),
    ("ring_elfentouw", "ring_gaven", "guhs:elfentouw", "task", "Hup, omhoog", "Laat je door het Elfentouw naar een haak trekken"),
    ("ring_gedragen", "ring_gaven", "minecraft:saddle", "task", "Maar wel jou", "Laat Sam-guh je dragen als de ring te zwaar wordt"),
    ("ring_klaar", "ring_gedragen", "guhs:knabbelring", "challenge", "Ringdrager", "Breng de Knabbelring naar de Frituurberg, laat hem frituren en deel hem met iedereen"),
    ("ring_smikagol_maatje", "ring_klaar", "guhs:ring_vissenbotje", "goal", "Ons vadsje", "Smikagol is je maatje: zet hem bij water en hij vangt vis voor je"),
    ("ring_feest", "ring_klaar", "guhs:ring_feestknabbel", "task", "Er is altijd wel iets te vieren", "Haal je dagelijkse traktatie op het feest in de Knabbelgouw"),
]
# hidden ones without a visible twin (FTB tasks, counters)
VERBORGEN = ["ring_wegwijs", "ring_rustpunt_5", "ring_lichtflesje", "ring_negen_gezien", "ring_smikagol", "ring_smikagol_vis"]

# the travel map and the story as a whole
REISKAART_NAAM = "De reis van de Knabbelring"
STRUCTUUR = ("Rustpunt", "Een kampje met een rustvuurtje: hier kookt Sam-guh en hier kom je terug als de Negen je pakken (Guhbarbecuether)")
BORD = ["Rustpunt van de", "Reisgenoten", "Sam-guh kookt.", "Njeg!"]
BORDEN = [["Rust hier uit,", "reiziger.", "Niet de ring", "opeten, njeg!"],
          ["De Frituurberg:", "die kant op.", "Nog een eind", "lopen. Vahoeg."],
          ["Laatste zoutjes", "voor de berg.", "Sam-guh was", "hier."]]
