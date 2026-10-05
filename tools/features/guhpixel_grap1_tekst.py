"""
Every Dutch text of the guhpixel slice "grap1" (Skyblok, Bedwars, Vadsnite). English: tools/lang/en/c32_px_grap1.json.
Keys follow CONTRACT_PX 1.1: gui/quest/sign/subtitles/entity/block/item .guhs.<skyblok|bedwars|vadsnite>...
"""

TEAMS = {"rood": "Rood", "blauw": "Blauw", "groen": "Groen", "geel": "Geel"}

TEXTS = {
    # --- names ---------------------------------------------------------------------------------------------------------
    "entity.guhs.guh_npc.skyblok_guh": "Skyblok-guh",
    "entity.guhs.guh_npc.bedwars_guh": "Bedwars-guh",
    "entity.guhs.guh_npc.vadsnite_guh": "Vadsnite-guh",
    "entity.guhs.bedwars_teamguh": "Teamguh",
    "block.guhs.skyblok_fles": "Eilandje-in-een-fles",
    "block.guhs.skyblok_fles.lore": "Hét eiland, maar dan klein. Met boom, bed en al. De cobblestone generator past er niet meer bij, njeg.",
    "item.guhs.vadsnite_parachuterugzakje": "Parachuterugzakje",
    "gui.guhs.kledingbron.bedwars_aandenken": "Aandenken van Bedwars (Guhpixel)",
    "gui.guhs.kledingbron.vadsnite_aandenken": "Aandenken van Vadsnite (Guhpixel)",
    "subtitles.guhs.skyblok.uitgespeeld": "Skyblok uitgespeeld",
    "subtitles.guhs.bedwars.verdedigd": "Bed verdedigd",
    "subtitles.guhs.vadsnite.sprong": "Guhs springen uit de Vadsbus",
    "subtitles.guhs.vadsnite.overwinning": "Vadsoverwinning",

    # --- the Guhdex section -----------------------------------------------------------------------------------------------
    "gui.guhs.skyblok.gids.kop": "Skyblok, Bedwars & Vadsnite",
    "gui.guhs.skyblok.gids.uitleg": "Drie beroemde minigames, op z'n guhs. Praat in de lobby met de spelguhs. De eerste keer krijg je 100 "
                                    "muntjes en een aandenken; daarna mag je zo vaak spelen als je wilt.",
    "gui.guhs.skyblok.gids.voortgang": "Uitgespeeld",

    # =====================================================================================================================
    # Skyblok
    # =====================================================================================================================
    "gui.guhs.skyblok.grap.naam": "Skyblok",
    "gui.guhs.skyblok.grap.uitleg": "Eén eiland, één boom en een handleiding van 4.812 stappen. Alleen voor echte pro's, zegt de Skyblok-guh.",
    "gui.guhs.skyblok.grap.stap.1": "Reis naar hét eiland",
    "gui.guhs.skyblok.grap.stap.2": "Open je startkist",
    "gui.guhs.skyblok.grap.stap.3": "Maak een cobblestone generator",
    "gui.guhs.skyblok.grap.stap.4": "Geef het op en ga in bed liggen",
    "gui.guhs.skyblok.grap.clou": "SKYBLOK UITGESPEELD!",
    "gui.guhs.skyblok.clou.onder": "Je ging liggen. Meer was het niet, njeg.",
    "quest.guhs.skyblok.hallo": "Stil. Ik zit in een call. ...Oké. %s, dit is Skyblok. Eén eiland. Eén boom. Vierduizend achthonderdtwaalf "
                                "stappen. Alleen voor echte pro's, njeg. Durf je?",
    "quest.guhs.skyblok.hallo.opnieuw": "Jij weer, %s. Jij hebt Skyblok uitgespeeld. Niemand speelt Skyblok uit! Ik train al drie jaar op "
                                        "stap 1. ...Wil je nog eens?",
    "quest.guhs.skyblok.uitleg": "Je staat op hét eiland, hoog in de lucht. Heel hoog. Echte lucht, hoor, niks aan gedaan. Volg de handleiding "
                                 "bovenin je scherm. En wat je ook doet: ga NIET in dat bed liggen. Dat is voor beginners, njeg.",
    "quest.guhs.skyblok.optie.spelen": "Ik ben er klaar voor!",
    "quest.guhs.skyblok.optie.opnieuw": "Nog een keer!",
    "quest.guhs.skyblok.optie.uitleg": "Wat is Skyblok?",
    "quest.guhs.skyblok.optie.kwijt": "Ik ben mijn flesje kwijt, njeg",
    "quest.guhs.skyblok.optie.doei": "Later misschien",
    "quest.guhs.skyblok.kwijt.hier": "Tss. Een pro raakt zijn spullen niet kwijt. Hier, een nieuw Eilandje-in-een-fles. Zuinig op zijn, njeg.",
    "quest.guhs.skyblok.kwijt.nee": "Je hebt hem gewoon nog. Kijk eens goed in je zakken, njeg.",
    "gui.guhs.skyblok.begin": "Welkom op hét eiland! Volg de handleiding bovenin je scherm. Succes. Je zult het nodig hebben, njeg.",
    "gui.guhs.skyblok.handleiding.balk": "Stap 1 van %s: maak een cobblestone generator",
    "gui.guhs.skyblok.handleiding.klaar": "Stap %1$s van %1$s: vadsen. Gelukt!",
    "gui.guhs.skyblok.hint.bed": "Psst... dat bed ziet er wel erg zacht uit, njeg.",
    "gui.guhs.skyblok.leegte": "Je viel in de eindeloze leegte! Of nou ja... het is een vloer. Lichtblauw geverfd. Kijk maar rustig rond, njeg.",
    "gui.guhs.skyblok.leegte.terug": "Hup, terug naar je eiland. De leegte gaat zo dicht, njeg.",
    "gui.guhs.skyblok.nee.1": "Dat lukt niet. Probeer stap 1 nog eens, njeg.",
    "gui.guhs.skyblok.nee.2": "Bijna! (Niet echt.)",
    "gui.guhs.skyblok.nee.3": "De handleiding zegt: eerst een cobblestone generator.",
    "gui.guhs.skyblok.nee.4": "Hmm. Skyblok is moeilijk, hè?",
    "gui.guhs.skyblok.nee.5": "Nog 4.812 stappen te gaan. Of... dat bed?",
    "gui.guhs.skyblok.nee.6": "Njeg. Dat werkt hier niet.",
    "gui.guhs.skyblok.nee.deur": "Alleen voor personeel, njeg! Terug naar je eiland, jij.",
    "gui.guhs.skyblok.nee.werkbank": "Knutselen? Zonder cobblestone generator? Njeg.",
    "gui.guhs.skyblok.nee.lava": "De lava blijft in de emmer zitten. Hij is een beetje verlegen, njeg.",
    "gui.guhs.skyblok.nee.ijs": "Het ijs wil niet uit je pootjes. Lekker koel is het wel.",
    "gui.guhs.skyblok.nee.lucht": "Dat is de lucht! Niet aankomen, de verf is nog nat.",
    "gui.guhs.skyblok.nee.boom": "De boom is van de decorguhs. Afblijven, njeg.",
    "gui.guhs.skyblok.nee.breken": "Er is maar één eiland. Heel laten, njeg.",
    "gui.guhs.skyblok.nee.plaatsen": "Bouwen mag pas bij stap 2.406, njeg.",
    "gui.guhs.skyblok.einde": "Je ging liggen. Dat was het hele spel! Het doel in het leven is vadsen, niet een cobblestone generator bouwen. "
                              "Vahoeg!",
    "gui.guhs.skyblok.aftiteling.1.rol": "SKYBLOK",
    "gui.guhs.skyblok.aftiteling.1.naam": "een Guhpixel-productie",
    "gui.guhs.skyblok.aftiteling.2.rol": "Regie",
    "gui.guhs.skyblok.aftiteling.2.naam": "een guh (sliep)",
    "gui.guhs.skyblok.aftiteling.3.rol": "Cobblestone generator",
    "gui.guhs.skyblok.aftiteling.3.naam": "niet gebouwd",
    "gui.guhs.skyblok.aftiteling.4.rol": "De lucht",
    "gui.guhs.skyblok.aftiteling.4.naam": "lichtblauwe verf, twee lagen",
    "gui.guhs.skyblok.aftiteling.5.rol": "Wolkjes",
    "gui.guhs.skyblok.aftiteling.5.naam": "met de kwast, door de decorguhs",
    "gui.guhs.skyblok.aftiteling.6.rol": "Stunts",
    "gui.guhs.skyblok.aftiteling.6.naam": "jij (ging liggen)",
    "gui.guhs.skyblok.aftiteling.7.rol": "Met dank aan",
    "gui.guhs.skyblok.aftiteling.7.naam": "het bed",
    "sign.guhs.skyblok.personeel": "ALLEEN PERSONEEL",
    "sign.guhs.skyblok.zon": "ZON (40 watt)",

    # =====================================================================================================================
    # Bedwars
    # =====================================================================================================================
    "gui.guhs.bedwars.grap.naam": "Bedwars",
    "gui.guhs.bedwars.grap.uitleg": "Vier teams willen bij je bed. Jij verdedigt het, met alles wat je hebt. Zegt de Bedwars-guh, in zijn "
                                    "harnas van kussens.",
    "gui.guhs.bedwars.grap.stap.1": "Ga naar je teameiland",
    "gui.guhs.bedwars.grap.stap.2": "Verdedig je bed: ga erin liggen",
    "gui.guhs.bedwars.grap.stap.3": "Blijf liggen, de teams komen eraan",
    "gui.guhs.bedwars.grap.stap.4": "Maak plaats: ze willen er alleen maar bij",
    "gui.guhs.bedwars.grap.clou": "BED VERDEDIGD!",
    "gui.guhs.bedwars.clou.onder": "Bedden vernield: %1$s · dutjes: %2$s",
    "quest.guhs.bedwars.hallo": "HALT! Wie daar? O, %s. Ik ben de Bedwars-guh. Zie je dit harnas? Echte kussens. Dons! In Bedwars is er maar "
                                "één regel: verdedig je bed. Met alles wat je hebt, njeg!",
    "quest.guhs.bedwars.hallo.opnieuw": "Daar is %s, de beste bedverdediger van heel Guhpixel! Nul bedden vernield. NUL. Wil je je bed nog "
                                        "eens verdedigen?",
    "quest.guhs.bedwars.uitleg": "Jij krijgt een eilandje met een bed. Om je heen wonen vier teams: Rood, Blauw, Groen en Geel. Die komen "
                                 "eraan, over bruggetjes van wol. Jij verdedigt je bed. Hoe? Dat merk je vanzelf. Tip: het is een heel "
                                 "lekker bed, njeg.",
    "quest.guhs.bedwars.optie.spelen": "Ik verdedig mijn bed!",
    "quest.guhs.bedwars.optie.opnieuw": "Nog een keer verdedigen!",
    "quest.guhs.bedwars.optie.uitleg": "Wat is Bedwars?",
    "quest.guhs.bedwars.optie.doei": "Ik ga eerst oefenen",
    "gui.guhs.bedwars.begin": "Dit is jouw eiland, met jouw bed. De andere teams hebben het al gezien, njeg...",
    "gui.guhs.bedwars.titel.verdedig": "VERDEDIG JE BED!",
    "gui.guhs.bedwars.titel.verdedig.onder": "Ze komen eraan over de wol, njeg!",
    "gui.guhs.bedwars.ligt": "Je ligt in je bed. Nu kan niemand er meer bij. Perfecte verdediging, njeg!",
    "gui.guhs.bedwars.aangekomen": "%s staat op je eiland! ...en wacht netjes op het randje.",
    "gui.guhs.bedwars.dutje": "%1$s komt naast je bed liggen. Zzz... (dutjes: %2$s)",
    "gui.guhs.bedwars.hint.terug": "Terug je bed in, njeg! Zo verdedig je niks.",
    "gui.guhs.bedwars.hint.wachten": "Ze wachten netjes tot jij in je bed ligt, njeg.",
    "gui.guhs.bedwars.hint.bed": "Je verdedigt je bed het best door erin te gaan liggen, njeg.",
    "gui.guhs.bedwars.eind.streep": "-----------------------------",
    "gui.guhs.bedwars.eind.kop": "BED VERDEDIGD",
    "gui.guhs.bedwars.eind.vernield": "Bedden vernield: %s",
    "gui.guhs.bedwars.eind.dutjes": "Dutjes: %s",
    "gui.guhs.bedwars.eind.wol": "Blokken wol verbruikt: %s",
    "gui.guhs.bedwars.eind.beste": "Beste verdediger: jij (lag erbij, njeg)",
    "gui.guhs.bedwars.guh.slaapt": "%s slaapt. Sssst.",
    "gui.guhs.bedwars.guh.njeg": "%s: \"Njeg. Is er nog plek bij dat bed?\"",
    "gui.guhs.bedwars.nee.bed": "Je eigen bed slopen? Dat mag niet eens van de regels, njeg.",
    "gui.guhs.bedwars.nee.breken": "Niks slopen. Dit is Bedwars, geen Sloopwars, njeg.",
    "sign.guhs.bedwars.generator": "KUSSEN-GENERATOR (op, njeg)",

    # =====================================================================================================================
    # Vadsnite
    # =====================================================================================================================
    "gui.guhs.vadsnite.grap.naam": "Vadsnite",
    "gui.guhs.vadsnite.grap.uitleg": "Honderd springen uit de vliegende Vadsbus. Eentje blijft over. De Vadsnite-guh heeft zijn "
                                     "parachuterugzakje al om.",
    "gui.guhs.vadsnite.grap.stap.1": "Stap in de Vadsbus",
    "gui.guhs.vadsnite.grap.stap.2": "Spring door het luik",
    "gui.guhs.vadsnite.grap.stap.3": "Land op het eiland",
    "gui.guhs.vadsnite.grap.stap.4": "Blijf als laatste wakker",
    "gui.guhs.vadsnite.grap.clou": "#1 VADSOVERWINNING",
    "gui.guhs.vadsnite.clou.onder": "Jij bent de enige die nog wakker is, njeg!",
    "quest.guhs.vadsnite.hallo": "Yo %s! Zin in een potje Vadsnite? Honderd springen eruit, eentje blijft over. Meestal degene die het langst "
                                 "wakker blijft, njeg. Rugzakje om en gaan!",
    "quest.guhs.vadsnite.hallo.opnieuw": "%s! De kampioen! Iedereen heeft het nog over jouw Vadsoverwinning. Of nou ja... iedereen sliep. "
                                         "Nog een rondje?",
    "quest.guhs.vadsnite.uitleg": "Je vliegt met 99 guhs in de Vadsbus boven een eilandje. Spring door het luik, zweef naar beneden en blijf "
                                  "als laatste over. Pas op voor de slaapwolk: die is roze, zacht en komt steeds dichterbij. Hij doet niks, "
                                  "hoor. Hij is alleen heel knus, njeg.",
    "quest.guhs.vadsnite.optie.spelen": "In de Vadsbus!",
    "quest.guhs.vadsnite.optie.opnieuw": "Nog een rondje!",
    "quest.guhs.vadsnite.optie.uitleg": "Wat is Vadsnite?",
    "quest.guhs.vadsnite.optie.doei": "Ik blijf op de grond",
    "gui.guhs.vadsnite.teller": "Nog wakker: %s",
    "gui.guhs.vadsnite.begin": "Welkom in de Vadsbus! We vliegen boven het eiland. Het luik zit achterin, njeg.",
    "gui.guhs.vadsnite.aftellen": "Maak je klaar...",
    "gui.guhs.vadsnite.springen": "SPRINGEN!",
    "gui.guhs.vadsnite.springen.onder": "Door het luik, njeg!",
    "gui.guhs.vadsnite.duwtje": "De chauffeur helpt een pootje: hup, door het luik!",
    "gui.guhs.vadsnite.geland": "Geland! Nu alleen nog als laatste wakker blijven. Dat kan nooit lang duren, njeg...",
    "gui.guhs.vadsnite.wolk": "Gaaap... de slaapwolk. Roze en zacht. Er gebeurt niks, maar knus is het wel, njeg.",
    "gui.guhs.vadsnite.gelijkspel": "GELIJKSPEL",
    "gui.guhs.vadsnite.gelijkspel.onder": "Iedereen slaapt. Gelijkspel, njeg.",
    "gui.guhs.vadsnite.einde.gelijkspel": "Jij ging ook liggen. Nu slaapt echt iedereen. Dat telt gewoon, njeg!",
    "gui.guhs.vadsnite.einde.overwinning": "Alle %s guhs slapen. Jij bent als enige nog wakker. Je hoefde er niks voor te doen. Vahoeg!",
    "gui.guhs.vadsnite.nee.breken": "Hout hakken? In Vadsnite wordt alleen gedut, njeg.",
    "sign.guhs.vadsnite.luik": "LUIK: HIER SPRINGEN",
}
for _id, _naam in TEAMS.items():
    TEXTS[f"entity.guhs.bedwars_teamguh.{_id}"] = f"Team {_naam}"
    TEXTS[f"item.guhs.bedwars_slaapmuts_{_id}"] = f"Teamslaapmuts {_naam}"

# hidden advancements (FTB tasks and the kern's Grappen grant them): <id>_stap_1..4 and <id>_klaar
QUEST_ADVS = [f"{g}_stap_{i}" for g in ("skyblok", "bedwars", "vadsnite") for i in range(1, 5)] + \
             [f"{g}_klaar" for g in ("skyblok", "bedwars", "vadsnite")]

SOUNDS = {
    "skyblok.uitgespeeld": [{"name": "minecraft:ui.toast.challenge_complete", "type": "event", "pitch": 1.1, "volume": 0.8}],
    "bedwars.verdedigd": [{"name": "minecraft:entity.player.levelup", "type": "event", "pitch": 0.8, "volume": 0.9}],
    "vadsnite.sprong": [{"name": "minecraft:entity.ender_dragon.flap", "type": "event", "pitch": 1.3, "volume": 0.8}],
    "vadsnite.overwinning": [{"name": "minecraft:ui.toast.challenge_complete", "type": "event", "pitch": 0.9, "volume": 0.9}],
}
