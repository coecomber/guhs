"""
The "Aan de slag" guide: a hand-written, step-by-step page for new players (NL first, EN behind the language toggle).

Every fact here was checked against the code and data (recipes, loot tables, worldgen, villager trades, Java); when the
game changes, check this page too. It lives at the site root (aan-de-slag.html) and is linked from the home page and the
sidebar. Links use the page tokens of pages.py (L / item_ref), so the link checker catches a page that goes away.
"""
from .pages import L
from .site import Page, t

PID = "systemen/aan-de-slag"


def _p(nl, en):
    return f'<p lang="nl">{nl}</p><p lang="en">{en}</p>'


def _ul(items):
    return "<ul>" + "".join(f"<li>{t(en, nl)}</li>" for nl, en in items) + "</ul>"


def _tip(nl, en, label_nl="Tip!", label_en="Tip!"):
    return f'<div class="gtip"><b>{t(label_en, label_nl)}</b> {t(en, nl)}</div>'


class Guide:
    def __init__(self, builder):
        self.b = builder
        self.g = builder.g
        self.recipes = {r["id"]: r for r in self.g.recipes}

    def recipe(self, *ids):
        cards = "".join(self.b.recipe_html(self.recipes[i]) for i in ids if i in self.recipes)
        return f'<div class="recipes">{cards}</div>' if cards else ""

    def item(self, iid):
        return self.b.item_ref("guhs:" + iid)

    def pic(self, name, alt):
        return self.b.img(name, alt) if self.b.im.has(name) else ""

    def step(self, key, num, h_nl, h_en, body, pic=None):
        pic_html = f'<div class="gpic stage">{pic}</div>' if pic else ""
        return (f'<section class="gstep{" has-pic" if pic else ""}" id="{key}"><div class="gnum" aria-hidden="true">{num}</div>'
                f'<div class="gbody"><h2>{t(h_en, h_nl)}</h2>{body}</div>{pic_html}</section>')

    # --- the page ------------------------------------------------------------------------------------------------------------
    def build(self):
        pg = Page("systemen", PID, "Aan de slag", "Getting started", "Gids", "Guide", "guh")
        pg.path_override = "aan-de-slag.html"
        pg.no_autolink = True
        pg.lead_nl = ("Nieuw in de wereld van de guhs? Deze gids neemt je stap voor stap mee: van je eerste kaasknabbel tot je eerste "
                      "minigame in de Guhmensie. Vahoeg!")
        pg.lead_en = ("New to the world of the guhs? This guide takes you step by step from your first kaasknabbel to your first "
                      "minigame in the Guhmension. Vahoeg!")
        pg.data["sort"] = -1
        pg.data["guide"] = True
        pg.related = ["systemen/temmen", "dimensies/guhmension", "npcs/reisguh", "systemen/superkompas", "systemen/guhdex",
                      "bouwwerken/knuffeldal_stadje", "minigames/index", "dimensies/guhmaag", "systemen/ftb-quests"]
        pg.aliases = {"Aan de slag", "Getting started", "Beginnersgids", "Beginners guide"}
        pg.data["guide_html"] = self.html()
        return pg

    def html(self):
        steps = [
            ("voorbereiding", "Voorbereiding", "Get ready"),
            ("kaasknabbels", "Kaasknabbels", "Kaasknabbels"),
            ("temmen", "Je eerste guh", "Your first guh"),
            ("portaal", "Het guhportaal", "The guh portal"),
            ("reisguh", "De Reisguh", "The Reisguh"),
            ("superkompas", "Het superkompas", "The super compass"),
            ("doelen", "Eerste doelen", "First goals"),
            ("tips", "Handige tips", "Handy tips"),
        ]
        toc = '<ol class="gtoc">' + "".join(
            f'<li><a href="#{k}"><b>{i}</b>{t(en, nl)}</a></li>' for i, (k, nl, en) in enumerate(steps, 1)) + "</ol>"
        hero = (f'<div class="ghero"><div>{_p(self.lead_nl(), self.lead_en())}'
                f'<p class="muted">{t("In a hurry? Jump straight to a step:", "Haast? Spring meteen naar een stap:")}</p>{toc}</div>'
                f'<div class="ghero-art">{self.pic("guh_sitting", "Een guh")}</div></div>')
        return hero + "".join([
            self.s_voorbereiding(), self.s_kaasknabbels(), self.s_temmen(), self.s_portaal(), self.s_reisguh(),
            self.s_superkompas(), self.s_doelen(), self.s_tips()]) + self.outro()

    @staticmethod
    def lead_nl():
        return ("Nieuw in de wereld van de <b>lieve vadsige guhs</b>? Deze gids neemt je stap voor stap mee: van je eerste kaasknabbel "
                "in de gewone wereld tot je eerste minigame in de <b>Guhmensie</b>. Pak een snackje, njeg, en daar gaan we!")

    @staticmethod
    def lead_en():
        return ("New to the world of the <b>lieve vadsige guhs</b>? This guide takes you step by step from your first kaasknabbel in "
                "the normal world to your first minigame in the <b>Guhmension</b>. Grab a snack, njeg, and off we go!")

    # 1 ------------------------------------------------------------------------------------------------------------------------
    def s_voorbereiding(self):
        body = _p("Je hoeft geen pro te zijn om de Guhmensie in te gaan. Met dit in je rugzak zit je goed:",
                  "You don't need to be a pro to enter the Guhmension. With this in your bag you're fine:") + _ul([
            ("<b>Stenen of ijzeren gereedschap</b> is genoeg. <b>Diamanten heb je niet nodig</b>, nergens voor.",
             "<b>Stone or iron tools</b> are enough. <b>You don't need diamonds</b>, not for anything."),
            (f"Neem een <b>ijzeren houweel</b> mee: het glanzend roze erts {L('blokken/compressed_super_vahoege_vads', 'samengeperste supervahoege vads')} "
             "in de Guhmensie geeft alleen iets met ijzer (of beter). Al het andere Guhs-erts, ook kaasknabbelerts, hak je met elk houweel.",
             f"Bring an <b>iron pickaxe</b>: the shiny pink {L('blokken/compressed_super_vahoege_vads', 'compressed super vahoege vads')} ore in the "
             "Guhmension only drops something with iron (or better). Every other Guhs ore, kaasknabbel ore too, breaks with any pickaxe."),
            (f"Veel later wil je {L('blokken/grillkool', 'grillkool')} voor het portaal naar de {L('dimensies/barbecuether')}: dat vraagt een houweel van "
             f"diamantniveau, maar een {L('items/vahoege_vads_pickaxe', 'vahoege-vadshouweel')} telt ook. Dus nog steeds geen diamanten nodig. Vahoeg!",
             f"Much later you'll want {L('blokken/grillkool', 'grillkool')} for the portal to the {L('dimensies/barbecuether')}: that takes a diamond-level "
             f"pickaxe, but a {L('items/vahoege_vads_pickaxe', 'vahoege vads pickaxe')} counts too. So still no diamonds needed. Vahoeg!"),
            ("<b>Wat eten</b> voor onderweg (brood, gebakken vlees, of straks guhsnacks).",
             "<b>Some food</b> for the trip (bread, cooked meat, or guh snacks later on)."),
            ("<b>Een bed</b>. In de Guhmensie werken bedden gewoon: slaap er een keer in, vlak bij je portaal, dan word je daar wakker als het misgaat "
             "en hoef je niet terug te lopen vanuit de gewone wereld.",
             "<b>A bed</b>. Beds work in the Guhmension: sleep in one once, right next to your portal, and that's where you wake up if something goes "
             "wrong, instead of walking back from the normal world."),
            ("<b>Een beetje pantser</b> (ijzer is prima). De Guhmensie is lief: er zijn geen zombies of creepers. Gewone Mika's doen geen pijn, ze "
             f"<b>duwen</b> je alleen weg (pas op bij randjes en ravijnen). Alleen {L('wezens/big_mika', 'Grote Mika')}, de baas van de "
             f"{L('bouwwerken/challenging_guh_caves', 'uitdagende guhgrotten')}, doet echt pijn: 200 levens en klappen van 4,5 hartje. Die bewaar je voor later.",
             "<b>A bit of armour</b> (iron is fine). The Guhmension is sweet: there are no zombies or creepers. Normal Mikas don't hurt, they only "
             f"<b>shove</b> you away (mind the edges and ravines). Only {L('wezens/big_mika', 'Big Mika')}, the boss of the "
             f"{L('bouwwerken/challenging_guh_caves', 'challenging guh caves')}, really hurts: 200 health and hits of 4.5 hearts. Save him for later."),
            ("<b>Kaasknabbels, kaasknabbels, kaasknabbels.</b> Daar draait alles om (zie de volgende stap).",
             "<b>Kaasknabbels, kaasknabbels, kaasknabbels.</b> Everything runs on them (see the next step)."),
        ])
        return self.step("voorbereiding", 1, "Voorbereiding", "Get ready", body, self.pic("guh_saddle", "Een guh met zadel"))

    # 2 ------------------------------------------------------------------------------------------------------------------------
    def s_kaasknabbels(self):
        body = _p(f"{self.item('kaas_knabbels')} zijn het lievelingseten van elke guh: je temt ze ermee, je geneest ze ermee en je bouwt er je "
                  "portaal van. Gelukkig zit de gewone wereld er vol mee:",
                  f"{self.item('kaas_knabbels')} are every guh's favourite food: you tame them with it, heal them with it and build your portal "
                  "out of it. Luckily the normal world is full of them:") + _ul([
            (f"{L('blokken/kaasknabbel_stone')} en {L('blokken/kaasknabbel_deepslate')}: stenen met gele stipjes, op elke hoogte onder de grond. Heel vaak!",
             f"{L('blokken/kaasknabbel_stone')} and {L('blokken/kaasknabbel_deepslate')}: stone with yellow dots, at every height underground. Very common!"),
            (f"{L('blokken/kaasknabbel_dirt')} in aarde en {L('blokken/kaasknabbel_cobblestone')} tussen de steen.",
             f"{L('blokken/kaasknabbel_dirt')} in dirt and {L('blokken/kaasknabbel_cobblestone')} in between the stone."),
            ("Elk blokje geeft <b>1 tot 3 kaasknabbels</b>. <b>Geluk</b> (Fortune) geeft er meer; met <b>Zijden aanraking</b> krijg je het blok zelf.",
             "Every block drops <b>1 to 3 kaasknabbels</b>. <b>Fortune</b> gives more; with <b>Silk Touch</b> you get the block itself."),
            ("Negen kaasknabbels maken een <b>blok kaasknabbels</b>, en die heb je zo nodig voor het portaal.",
             "Nine kaasknabbels make a <b>block of kaasknabbels</b>, and you'll need those for the portal in a moment."),
        ]) + self.recipe("block_of_kaasknabbels")
        return self.step("kaasknabbels", 2, "Kaasknabbels zoeken", "Find kaasknabbels", body, self.pic("kaasknabbel_stone", "Kaasknabbelsteen"))

    # 3 ------------------------------------------------------------------------------------------------------------------------
    def s_temmen(self):
        body = _p("In de gewone wereld wonen <b>wilde guhs</b>, in elk bioom. Ze zijn wel een beetje zeldzaam: kijk goed rond, ze lopen in groepjes "
                  "van 1 tot 3. (In de Guhmensie wemelt het ervan.) Guhs zijn altijd lief, ze vallen je nooit aan.",
                  "<b>Wild guhs</b> live in the normal world, in every biome. They are a bit rare though: look around well, they walk in groups of "
                  "1 to 3. (The Guhmension is full of them.) Guhs are always sweet, they never attack you.") + _ul([
            ("<b>Temmen</b>: rechtsklik met kaasknabbels. Elke knabbel is een kansje, meestal 1 op 3. Een <i>vadsige</i> guh is makkelijker (1 op 2), een "
             "<i>verlegen</i> guh lastiger (1 op 5): die houdt afstand, tenzij je kaasknabbels vasthoudt.",
             "<b>Taming</b>: right-click with kaasknabbels. Every knabbel is a chance, usually 1 in 3. A <i>vadsig</i> guh is easier (1 in 2), a "
             "<i>shy</i> guh harder (1 in 5): it keeps its distance unless you hold kaasknabbels."),
            ("Hartjes? Dan is hij van jou! Een tamme guh krijgt <b>1000 levens</b> en loopt achter je aan.",
             "Hearts? Then it's yours! A tamed guh gets <b>1000 health</b> and follows you around."),
            ("Zijn levens niet vol? Elke kaasknabbel geeft er 100 terug.", "Not at full health? Every kaasknabbel heals 100."),
            (f"Elke guh heeft een eigen {L('systemen/karakters', 'karakter')}: speels, lui, vadsig, verlegen, dapper, kletskous, knuffelig of nieuwsgierig.",
             f"Every guh has its own {L('systemen/karakters', 'personality')}: playful, lazy, vadsig, shy, brave, chatty, cuddly or curious."),
        ]) + _p(f"Alles over temmen, rijden en oppakken: {L('systemen/temmen')}.", f"All about taming, riding and picking up: {L('systemen/temmen')}.")
        return self.step("temmen", 3, "Je eerste guh", "Your first guh", body, self.pic("guh", "Een guh"))

    # 4 ------------------------------------------------------------------------------------------------------------------------
    def s_portaal(self):
        body = _p("Tijd voor het echte werk: de Guhmensie! Je bouwt het portaal net als een netherportaal, maar dan van "
                  f"{L('blokken/block_of_kaasknabbels', 'blokken kaasknabbels')}.",
                  "Time for the real thing: the Guhmension! You build the portal just like a Nether portal, but out of "
                  f"{L('blokken/block_of_kaasknabbels', 'blocks of kaasknabbels')}.") + _ul([
            ("Een <b>rechtopstaand, rechthoekig frame</b>. De opening is minstens <b>2 breed en 3 hoog</b> en hooguit 21 bij 21.",
             "An <b>upright, rectangular frame</b>. The opening is at least <b>2 wide and 3 high</b> and at most 21 by 21."),
            ("De <b>hoeken hoeven niet</b>. Het kleinste portaal kost dus <b>10 blokken</b> (90 kaasknabbels); met hoeken 14.",
             "The <b>corners are optional</b>. So the smallest portal costs <b>10 blocks</b> (90 kaasknabbels); with corners 14."),
            ("<b>Aansteken hoeft niet</b>: zet je het laatste blok neer, dan gaat het portaal meteen open. Roze wiebelwerk!",
             "<b>No lighting needed</b>: place the last block and the portal opens straight away. Pink wobbles!"),
            ("Stap erin. Aan de andere kant verschijnt vanzelf een portaal (op een roze wollen vloertje), en daarmee kun je altijd terug.",
             "Step in. On the other side a portal appears by itself (on a little pink wool platform), and you can always go back through it."),
            (f"Kom je binnen zonder {L('systemen/guhdex', 'Guhdex')}, dan krijg je er <b>gratis</b> een: <i>\"Welkom in de Guhmensie! Hier, een Guhdex: vul hem "
             "met alle guhs die je tegenkomt. Njeg!\"</i> (Zakken vol? Dan valt hij voor je voeten.)",
             f"Come in without a {L('systemen/guhdex', 'Guhdex')} and you get one <b>for free</b> (full pockets? It drops at your feet). "
             "It keeps every guh, character and critter you meet."),
        ]) + _p(f"Meer over de wereld aan de andere kant: {L('dimensies/guhmension')}.", f"More about the world on the other side: {L('dimensies/guhmension')}.")
        return self.step("portaal", 4, "Het guhportaal bouwen", "Build the guh portal", body, self.pic("guh_portal", "Het guhportaal"))

    # 5 ------------------------------------------------------------------------------------------------------------------------
    def s_reisguh(self):
        body = _p(f"Het eerste wat je ziet: een guh met een donkerblauw <b>conducteurspetje</b> en een gouden fluitje. Dat is de {L('npcs/reisguh')}, en "
                  "bij elk guhportaal in de Guhmensie staat er een. <b>Praat meteen met de Reisguh!</b> Echt, doe het nu, je toekomstige zelf zegt dankjewel.",
                  f"The first thing you see: a guh with a dark blue <b>conductor's cap</b> and a golden whistle. That is the {L('npcs/reisguh')}, and one stands "
                  "by every guh portal in the Guhmension. <b>Talk to the Reisguh right away!</b> Really, do it now, your future self says thank you.") + _ul([
            ("<b>Rechtsklik</b> = ontdekt. <i>Tuut tuut!</i> Vanaf nu staat deze Reisguh in jouw reislijstje.",
             "<b>Right-click</b> = discovered. <i>Toot toot!</i> From now on this Reisguh is in your travel list."),
            ("Rechtsklik nog eens en je krijgt het menu: <b>reis in een tel</b> naar elke andere Reisguh die je al kent, of geef deze een eigen naam.",
             "Right-click again for the menu: <b>travel in a blink</b> to any other Reisguh you already know, or give this one its own name."),
            ("Reisguhs staan bij elk portaal, bij het guhkasteel en in de grote plekken: het Knuffeldal-stadje, de Guhkermis, Guhland, het Ballonfestival, "
             "het Guhcircuit, Guhwarden en de verhaalplekken. Soms zit er zomaar een wilde in het landschap. Praat met ze allemaal!",
             "Reisguhs stand by every portal, at the guh castle and in the big places: the Knuffeldal town, the Guhkermis, Guhland, the Ballonfestival, "
             "the Guhcircuit, Guhwarden and the story places. Now and then a wild one just sits somewhere in the landscape. Talk to all of them!"),
            (f"Een eigen reispunt? Maak een {self.item('reisguh_fluitje')} (enderparel + kaasknabbels + lichtblauwe kleurstof = 2 fluitjes) en fluit "
             "waar je wilt: daar komt een Reisguh zitten. Sluip + rechtsklik pakt een Reisguh op, om hem ergens anders neer te zetten.",
             f"Your own travel point? Craft a {self.item('reisguh_fluitje')} (ender pearl + kaasknabbels + light blue dye = 2 whistles) and blow it "
             "wherever you like: a Reisguh comes to sit there. Sneak + right-click picks a Reisguh up, to put it somewhere else."),
            ("Reisguhs werken alleen <b>binnen de Guhmensie</b>.", "Reisguhs only work <b>inside the Guhmension</b>."),
        ]) + _tip("De Reisguh bij je portaal is je weg naar huis. Ben je ver weg en verdwaald? Zoek een Reisguh en reis terug naar <i>Guhportaal</i>.",
                  "The Reisguh at your portal is your way home. Far away and lost? Find any Reisguh and travel back to <i>Guhportaal</i>.") + \
            self.recipe("reisguh_fluitje")
        return self.step("reisguh", 5, "Praat meteen met de Reisguh", "Talk to the Reisguh right away", body, self.pic("npc_reisguh", "De Reisguh"))

    # 6 ------------------------------------------------------------------------------------------------------------------------
    def s_superkompas(self):
        body = _p(f"De Guhmensie is groot, en de leukste plekken zie je niet vanaf de weg. Haal daarom <b>zo snel mogelijk</b> een {self.item('guhmensie_superkompas')}. "
                  "Rechtsklik erop, kies een tabblad en een plek, en hij wijst naar de dichtstbijzijnde (in de dimensie waar je bent).",
                  f"The Guhmension is big, and the best places are hidden. So get a {self.item('guhmensie_superkompas')} <b>as soon as you can</b>. "
                  "Right-click it, pick a tab and a place, and it points to the nearest one (in the dimension you are in).") + \
            f'<h3>{t("How to get one", "Zo kom je eraan")}</h3>' + _ul([
                (f"<b>Kopen</b> bij een guhdorpeling in een {L('bouwwerken/guh_village', 'guhdorp')}: de {L('npcs/vads_temmer')} verkoopt hem voor "
                 f"<b>10 emeralds</b> zodra hij niveau 2 is, de {L('npcs/mika_jager')} op niveau 3 voor 12.",
                 f"<b>Buy</b> one from a guh villager in a {L('bouwwerken/guh_village', 'guh village')}: the {L('npcs/vads_temmer')} sells it for "
                 f"<b>10 emeralds</b> once he reaches level 2, the {L('npcs/mika_jager')} at level 3 for 12."),
                ("Emeralds? De Vadstemmer koopt <b>16 kaasknabbels voor 1 emerald</b>: handel een paar keer en hij stijgt vanzelf een niveau. Twee vliegen in één klap!",
                 "Emeralds? The Vads temmer buys <b>16 kaasknabbels for 1 emerald</b>: trade a few times and he levels up by himself. Two birds with one stone!"),
                (f"<b>Maken</b>: een kompas, 3 {L('items/guh_kristal', 'guhkristallen')} en een {L('items/vahoege_vads_ingot', 'vahoege-vadsstaaf')}. "
                 f"Guhkristallen groeien in de {L('biomen/guh_kristalmijn')} (onder de grond, y 30 tot 60); vads hak je met je ijzeren houweel en smelt je tot een staaf "
                 f"(of koop de staaf bij de {L('npcs/vadssmid')}).",
                 f"<b>Craft</b> it: a compass, 3 {L('items/guh_kristal', 'guh crystals')} and a {L('items/vahoege_vads_ingot', 'vahoege vads ingot')}. "
                 f"Guh crystals grow in the {L('biomen/guh_kristalmijn')} (underground, y 30 to 60); mine vads with your iron pickaxe and smelt it into an ingot "
                 f"(or buy the ingot from the {L('npcs/vadssmid')})."),
                ("<b>Vinden</b>: Grote Mika laat er altijd een vallen, en ze liggen soms in de kisten van de uitdagende guhgrotten.",
                 "<b>Find</b> one: Big Mika always drops one, and they sometimes lie in the chests of the challenging guh caves."),
            ]) + self.recipe("guhmensie_superkompas") + \
            f'<h3>{t("What it shows", "Wat hij laat zien")}</h3>' + _p(
                "Tien tabbladen: <b>Avontuur</b>, <b>Quests</b>, <b>Minigames</b> (Klassiekers, Knuffeldal en De Grote Guhspelen), <b>Wonderen</b>, <b>Wonen</b>, "
                "<b>Einde</b>, <b>Ondergrond</b>, <b>Barbecue</b>, <b>Knus</b> en <b>Verhalen</b> (de plekken van de grote guhverhalen). Houd hem vast om te zien hoe ver het nog is.",
                "Ten tabs: <b>Adventure</b>, <b>Quests</b>, <b>Minigames</b> (Classics, Knuffeldal and the Grote Guhspelen), <b>Wonders</b>, <b>Homes</b>, "
                "<b>End</b>, <b>Underground</b>, <b>Barbecue</b>, <b>Cosy</b> and <b>Stories</b> (the places of the big guh stories). Hold it to see how far it still is.") + \
            _tip(f"Er zijn ook losse kompasjes voor één plek, zoals het {L('items/heiligdom_kompas', 'heiligdomkompas')} (bij de Vadstemmer, naar Moeder Vadsig). "
                 "Handig, maar het superkompas doet ze allemaal tegelijk. Oude kompasjes blijven gewoon werken.",
                 f"There are single-place compasses too, like the {L('items/heiligdom_kompas', 'shrine compass')} (from the Vads temmer, to Mother Vadsig). "
                 "Handy, but the super compass does them all at once. Old compasses keep working.") + \
            _p(f"Alle tabbladen en plekken: {L('systemen/superkompas')}.", f"Every tab and place: {L('systemen/superkompas')}.")
        return self.step("superkompas", 6, "Haal zo snel mogelijk een superkompas", "Get a super compass as soon as you can", body,
                         self.pic("icon_guhmensie_superkompas_00", "Het superkompas"))

    # 7 ------------------------------------------------------------------------------------------------------------------------
    def goal(self, icon, h_nl, h_en, nl, en):
        if icon and self.b.im.has(icon):
            ic = self.b.icon(icon) if icon.startswith("icon_") else f'<img class="gicon" src="@thumb:{icon}@" alt="" loading="lazy">'
        else:
            ic = ""
        return f'<div class="goal"><h3>{ic}{t(h_en, h_nl)}</h3>{_p(nl, en)}</div>'

    def s_doelen(self):
        goals = [
            self.goal("icon_guhdex", "Vul je Guhdex", "Fill your Guhdex",
                      f"Elke guh, elk personage en elk diertje dat je ziet komt in je {L('systemen/guhdex', 'Guhdex')}. Tabjes: Guhs, Knus, Minigames, Kleding, "
                      "Mijn guhs en Verhalen.",
                      f"Every guh, character and critter you see goes into your {L('systemen/guhdex', 'Guhdex')}. Tabs: Guhs, Knus, Minigames, Clothes, "
                      "My guhs and Stories."),
            self.goal("villager_vads_temmer", "Guhdorpen en guhdorpelingen", "Guh villages and guh villagers",
                      f"{L('bouwwerken/guh_village', 'Guhdorpen')} liggen in open biomen zoals de {L('biomen/guh_fields')} en de {L('biomen/guh_meadows')} (superkompas-tab Wonen). De guhdorpelingen hebben guhberoepen: "
                      f"{L('npcs/vads_temmer')}, {L('npcs/vadssmid')}, {L('npcs/knabbelboer')}, {L('npcs/mika_jager')}, {L('npcs/guh_kleermaker')} en "
                      f"{L('npcs/hamsterbouwer')}.",
                      f"{L('bouwwerken/guh_village', 'Guh villages')} lie in open biomes like the {L('biomen/guh_fields')} and the {L('biomen/guh_meadows')} (super compass tab Homes). The guh villagers have guh jobs: "
                      f"{L('npcs/vads_temmer')}, {L('npcs/vadssmid')}, {L('npcs/knabbelboer')}, {L('npcs/mika_jager')}, {L('npcs/guh_kleermaker')} and "
                      f"{L('npcs/hamsterbouwer')}."),
            self.goal("npc_timmerguh", "Het Knuffeldal en de Timmerguh", "The Knuffeldal and the Timmerguh",
                      f"In het bioom {L('biomen/knuffeldal')} ligt het {L('bouwwerken/knuffeldal_stadje', 'Knuffeldal-stadje')}: spelletjes, winkeltjes en de bouwplaats van de "
                      f"{L('npcs/timmerguh')}. Doe zijn questline {L('verhalen/timmerguh', 'Samen een huisje bouwen')} en je krijgt zijn "
                      f"{L('items/timmerguh_bouwboekje', 'bouwboekje')}: daarmee kun je {L('systemen/guhhuisje', 'guhhuisjes')} maken (het boekje blijft in het rooster liggen).",
                      f"In the {L('biomen/knuffeldal')} biome lies the {L('bouwwerken/knuffeldal_stadje', 'Knuffeldal town')}: games, shops and the building site of the "
                      f"{L('npcs/timmerguh')}. Do his questline {L('verhalen/timmerguh', 'Samen een huisje bouwen')} and you get his "
                      f"{L('items/timmerguh_bouwboekje', 'building book')}: with it you can craft {L('systemen/guhhuisje', 'guh houses')} (the book stays in the grid)."),
            self.goal("icon_discomunt", "Je eerste minigames", "Your first minigames",
                      f"Er zijn {L('minigames/index', 'meer dan twintig minigames')}. Bij bijna elke kies je {L('systemen/moeilijkheid', 'makkelijk, medium of lastig')}, "
                      "en je wint de eigen muntjes of bonnen van dat spel (mepmunten, discomunten, kermisbonnen...). Daarmee koop je kleding en prijzen.",
                      f"There are {L('minigames/index', 'more than twenty minigames')}. In almost every one you choose {L('systemen/moeilijkheid', 'easy, medium or hard')}, "
                      "and you win that game's own coins or tickets (mepmunten, discomunten, kermisbonnen...). Spend them on clothes and prizes."),
            self.goal("icon_guh_buikfluitje", "De Guhmaag", "The guh stomach",
                      f"Volg het heiligdomkompas naar {L('npcs/moeder_vadsig')} en help haar in {L('verhalen/moeder-vadsig', 'haar verhaal')}. Aan het eind krijg je het "
                      f"{L('items/guh_buikfluitje', 'buikfluitje')}: blaas erop (of druk op <b>G</b>) en je bent in je eigen {L('dimensies/guhmaag', 'Guhmaag')}. HAP!",
                      f"Follow the shrine compass to {L('npcs/moeder_vadsig')} and help her in {L('verhalen/moeder-vadsig', 'her story')}. At the end you get the "
                      f"{L('items/guh_buikfluitje', 'belly whistle')}: blow it (or press <b>G</b>) and you're in your own {L('dimensies/guhmaag', 'guh stomach')}. NOM!"),
            self.goal("icon_knuffel_normal", "Hartjes en een huisje", "Hearts and a house",
                      f"Aaien, knuffelen, voeren, spelen: alles geeft {L('systemen/hartjes', 'hartjes')}, en die gaan nooit omlaag. Geef je guhs een "
                      f"{L('systemen/guhhuisje', 'guhhuisje')}, dan wonen ze daar en doen ze overdag {L('systemen/klusjes', 'klusjes')}.",
                      f"Petting, cuddling, feeding, playing: everything gives {L('systemen/hartjes', 'hearts')}, and they never go down. Give your guhs a "
                      f"{L('systemen/guhhuisje', 'guh house')} and they live there and do {L('systemen/klusjes', 'chores')} during the day."),
            self.goal("icon_baltoguh_beeldje", "De guhverhalen", "The guh stories",
                      f"De {L('verhalen/index', 'guhverhalen')} zijn grote avonturen met een nieuwe vriend aan het eind. Het tabblad <b>Verhalen</b> in je Guhdex laat "
                      "per verhaal zien waar je bent en wat de volgende stap is; de superkompas-tab Verhalen wijst de weg.",
                      f"The {L('verhalen/index', 'guh stories')} are big adventures with a new friend at the end. The <b>Stories</b> tab in your Guhdex shows "
                      "for every story where you are and what's next; the super compass tab Stories points the way."),
            self.goal("icon_timmerguh_bouwboekje", "FTB-quests", "FTB quests",
                      f"Zit {L('systemen/ftb-quests', 'FTB Quests')} in je pack? Dan staat er vanzelf een groep <b>Guhs</b> in je questboek, met dertien hoofdstukken. "
                      "Niks zit op slot, en elk hoofdstuk begint met <i>Hoe kom je hier?</i>.",
                      f"Got {L('systemen/ftb-quests', 'FTB Quests')} in your pack? Then a <b>Guhs</b> group with thirteen chapters is in your quest book automatically. "
                      "Nothing is locked, and every chapter starts with <i>Hoe kom je hier?</i>."),
            self.goal("icon_guh_ballon", "Samen spelen op een server?", "Playing together on a server?",
                      "Installeer Guhs op de server en bij iedereen. Tip: zet er <b>Lootr</b> bij, dan krijgt elke speler zijn eigen buit uit de kisten "
                      "van de bouwwerken. Geen ruzie om de schatkist!",
                      "Install Guhs on the server and for every player. Tip: add <b>Lootr</b>, so every player gets their own loot from the structure "
                      "chests. No fighting over the treasure chest!"),
        ]
        body = _p("Je bent binnen, je hebt een Reisguh en een kompas. En nu? Kies maar wat je leuk lijkt, er is geen verkeerde volgorde:",
                  "You're in, you have a Reisguh and a compass. Now what? Pick whatever sounds fun, there is no wrong order:") + \
            '<div class="goals">' + "".join(goals) + "</div>"
        return self.step("doelen", 7, "Eerste doelen", "First goals", body)

    # 8 ------------------------------------------------------------------------------------------------------------------------
    def s_tips(self):
        body = _ul([
            (f"<b>Het guhmenu</b>: houd rechtsklik ingedrukt op je tamme guh. Aaien, knuffelen, emotes, kleding, hernoemen en het dagboekje. {L('systemen/guhmenu', 'Meer')}",
             f"<b>The guh menu</b>: hold right-click on your tamed guh. Petting, hugging, emotes, clothes, renaming and the diary. {L('systemen/guhmenu', 'More')}"),
            ("<b>Zitten</b>: kies <i>Zitten</i> in het guhmenu. Een zittende guh beweegt helemaal niet, zelfs niet voor kaasknabbels. <i>Rondvadsen</i> uit = blijven staan zonder te zitten.",
             "<b>Sit</b>: choose <i>Sit</i> in the guh menu. A sitting guh doesn't move at all, not even for kaasknabbels. <i>Wander</i> off = stay put without sitting."),
            ("<b>Oppakken</b>: sluip + rechtsklik op je guh. Hij wordt een voorwerp dat alles onthoudt (naam, levens, zadel, pantser). Rechtsklik op een blok om hem neer te zetten.",
             "<b>Pick up</b>: sneak + right-click your guh. It becomes an item that remembers everything (name, health, saddle, armour). Right-click a block to put it down."),
            ("<b>Rijden</b>: grote guhs (vanaf ongeveer 1,7 blok lang) kunnen een <b>zadel</b> dragen. Geen zadel? De Vadstemmer verkoopt er een op niveau 2.",
             "<b>Riding</b>: big guhs (about 1.7 blocks long or more) can wear a <b>saddle</b>. No saddle? The Vads temmer sells one at level 2."),
            (f"<b>De koekenpan</b>: maak een {L('blokken/frying_pan', 'koekenpan')} (4 ijzerstaven en een stok), doe er wat {self.item('mika_vet')} in (dat laten Mika's vallen) "
             f"en rechtsklik met kaasknabbels: {self.item('gefrituurde_kaasknabbels')}. Die maken je guh <b>in één hap helemaal beter</b>.",
             f"<b>The frying pan</b>: craft a {L('blokken/frying_pan', 'frying pan')} (4 iron ingots and a stick), put some {self.item('mika_vet')} in it (Mikas drop it) "
             f"and right-click with kaasknabbels: {self.item('gefrituurde_kaasknabbels')}. They heal your guh <b>to full in one bite</b>."),
            ("<b>Teleporteren</b>: in het guhmenu stel je in of je guh naar je toe teleporteert als hij achterblijft.",
             "<b>Teleport</b>: in the guh menu you choose whether your guh teleports to you when it falls behind."),
            (f"<b>Kleding</b>: een kledingstuk eet je één keer op, en dan is het voor altijd van jou in de {L('systemen/kleding', 'kledingkast')}.",
             f"<b>Clothes</b>: eat a piece of clothing once and it's yours forever in the {L('systemen/kleding', 'wardrobe')}."),
            ("<b>De G-toets</b> (maag heen en terug) kun je aanpassen bij Besturing &rarr; Guhs.",
             "<b>The G key</b> (stomach there and back) can be changed in Controls &rarr; Guhs."),
            (f"<b>Handige commando's</b> voor creative en operators staan bij {L('systemen/commandos')}.",
             f"<b>Handy commands</b> for creative mode and operators are on {L('systemen/commandos')}."),
        ]) + self.recipe("frying_pan")
        return self.step("tips", 8, "Handige tips", "Handy tips", body, self.pic("frying_pan", "De koekenpan"))

    def outro(self):
        text = _p("Dat was het! Nu ben jij aan de beurt. Veel plezier in de Guhmensie, en vergeet niet: een guh is nooit te vadsig. <b>Njeg!</b>",
                  "That&#39;s it! Now it&#39;s your turn. Have fun in the Guhmension, and remember: a guh is never too vadsig. <b>Njeg!</b>")
        return (f'<div class="gend">{text}'
                f'<p><a class="gbtn" href="@@index@@">{t("Back to the home page", "Terug naar de startpagina")}</a> '
                f'<a class="gbtn alt" href="@@dimensies/guhmension@@">{t("Explore the Guhmension", "Ontdek de Guhmensie")}</a></p></div>')
