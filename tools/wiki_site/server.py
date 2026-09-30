"""
"Speel op de officiële server": a hand-written page about the official 24/7 Guhs server (NL first, EN behind the toggle).

Lives at the site root (server.html), next to the "Aan de slag" guide, and uses the same step layout (guide.py). The
server facts (address, versions, rules, restart times, the pack) are hand-kept: when the server or the pack changes,
change them here. The pack itself lives in https://github.com/coecomber/guhs-pack (packwiz).
"""
from .guide import Guide, _p, _tip, _ul
from .landing import SERVER_OPEN
from .pages import L
from .site import Page, t

PID = "systemen/officiele-server"

ADDRESS = "guhs.nl"
HOST = "play.guhs.nl"
IP = "2.28.142.15"
MC = "26.1.2"
NEOFORGE = "26.1.2.112"
JAVA = "25"
GUHS = "1.1.0"
MAP_URL = "https://map.guhs.nl/"
PACK_REPO = "https://github.com/coecomber/guhs-pack"
PACK_TOML = "https://coecomber.github.io/guhs-pack/pack.toml"
PRISM_ZIP = "https://coecomber.github.io/guhs-pack/prism/guhs-server-instance.zip"
PRISM_SITE = "https://prismlauncher.org/"
NEOFORGE_SITE = "https://neoforged.net/"

# (name, version) of the pack; "" = whatever version the pack has (see the pack repo)
MODS = [("Guhs", GUHS), ("GeckoLib", "5.5.2"), ("JEI", "29.34.0.90"), ("Jade", "26.1.10"), ("JourneyMap", "26.1.2-6.0.9"),
        ("AppleSkin", "3.0.9+mc26.1"), ("Lootr", "1.23.38.123"), ("Architectury", "20.1.16"), ("FTB Library", "26.1.2.9"),
        ("FTB Teams", "26.1.2.4"), ("FTB Filter System", "26.1.2.2"), ("FTB Quests", "26.1.2.8"), ("FTB Essentials", "26.1.2.4"),
        ("ModernFix", ""), ("FerriteCore", ""), ("spark", "")]
CLIENT_MODS = [("Sodium", "0.9.2"), ("Mouse Tweaks", "26.1-2.31")]
SERVER_MODS = ["Chunky", "BlueMap"]


def _modlist(mods):
    return '<ul class="modlist">' + "".join(f"<li>{n}{f' <code>{v}</code>' if v else ''}</li>" for n, v in mods) + "</ul>"


def _code(s):
    return f"<code>{s}</code>"


def _ext(url, label=None):
    return f'<a href="{url}" rel="noopener">{label or url}</a>'


class ServerPage(Guide):
    """Re-uses the step/tip helpers of the guide."""

    def build(self):
        pg = Page("systemen", PID, "Speel op de officiële server", "Play on the official server", "Gids", "Guide", "guh")
        pg.path_override = "server.html"
        pg.no_autolink = True
        pg.lead_nl = ("De officiële Guhs-server: 24/7 online, gratis en open voor iedereen. Met Prism Launcher sta je er in een paar minuten, "
                      "en je mods blijven vanzelf bij. Adres: guhs.nl.")
        pg.lead_en = ("The official Guhs server: online 24/7, free and open to everyone. With Prism Launcher you're in within minutes, "
                      "and your mods stay up to date by themselves. Address: guhs.nl.")
        pg.data["sort"] = -1
        pg.data["guide"] = True
        pg.related = ["systemen/aan-de-slag", "dimensies/guhmension", "systemen/ftb-quests", "systemen/commandos", "npcs/reisguh"]
        pg.aliases = {"Officiële server", "Official server", "Guhs Server", "Server", "Multiplayer", "guhs.nl", "Prism Launcher", "Serverregels", "Server rules"}
        pg.data["guide_html"] = self.html()
        return pg

    def html(self):
        steps = [
            ("wat", "De server", "The server"),
            ("prism", "Prism installeren", "Install Prism"),
            ("account", "Je account", "Your account"),
            ("importeren", "Instance importeren", "Import the instance"),
            ("joinen", "Starten en joinen", "Start and join"),
            ("handmatig", "Zonder Prism", "Without Prism"),
            ("regels", "Serverregels", "Server rules"),
            ("commandos", "Handige commando's", "Handy commands"),
            ("problemen", "Problemen?", "Problems?"),
            ("links", "Links", "Links"),
        ]
        toc = '<ol class="gtoc">' + "".join(
            f'<li><a href="#{k}"><b>{i}</b>{t(en, nl)}</a></li>' for i, (k, nl, en) in enumerate(steps, 1)) + "</ol>"
        lead = _p("Speel samen met andere guhvrienden op de <b>officiële Guhs-server</b>! Hij staat <b>dag en nacht</b> aan, is gratis en "
                  f"iedereen mag erop. Het adres is makkelijk: {_code(ADDRESS)}. Pak een kaasknabbel, njeg, en volg de stappen.",
                  "Play together with other guh friends on the <b>official Guhs server</b>! It runs <b>day and night</b>, it's free and "
                  f"everyone is welcome. The address is easy: {_code(ADDRESS)}. Grab a kaasknabbel, njeg, and follow the steps.")
        hero = (f'<div class="ghero"><div>{lead}'
                f'<p class="muted">{t("Already have Prism? Jump to step 4. Otherwise:", "Heb je Prism al? Ga naar stap 4. Anders:")}</p>{toc}</div>'
                f'<div class="ghero-art">{self.pic("guh_outfit_evenementen", "Een feestguh")}</div></div>')
        soon = "" if SERVER_OPEN else _tip(
            f"De officiële server opent binnenkort met Guhs {GUHS} (Minecraft {MC}). Deze instructies gelden vanaf dan.",
            f"The official server opens soon with Guhs {GUHS} (Minecraft {MC}). These instructions apply from then on.",
            "Binnenkort!", "Coming soon!")
        return soon + hero + "".join([
            self.s_wat(), self.s_prism(), self.s_account(), self.s_import(), self.s_join(), self.s_handmatig(),
            self.s_regels(), self.s_commandos(), self.s_problemen(), self.s_links()]) + self.outro()

    # 1 ------------------------------------------------------------------------------------------------------------------------
    def s_wat(self):
        body = _p("Een gewone Minecraft-wereld met Guhs erin, plus een paar handige mods. Je bouwt, temt, speelt minigames en gaat samen de "
                  f"{L('dimensies/guhmension', 'Guhmensie')} in. Zo staat hij ingesteld:",
                  "A normal Minecraft world with Guhs in it, plus a few handy mods. You build, tame, play minigames and go into the "
                  f"{L('dimensies/guhmension', 'Guhmension')} together. This is how it is set up:") + _ul([
            (f"<b>Adres:</b> {_code(ADDRESS)} (werkt dat nog niet? Gebruik dan {_code(IP)}).",
             f"<b>Address:</b> {_code(ADDRESS)} (doesn't work yet? Use {_code(IP)} instead)."),
            (f"<b>Versie:</b> Minecraft {MC} met NeoForge {NEOFORGE}.", f"<b>Version:</b> Minecraft {MC} with NeoForge {NEOFORGE}."),
            ("<b>Moeilijkheid normaal</b>, <b>geen PvP</b> (spelers kunnen elkaar geen pijn doen) en <b>open</b>: geen whitelist, iedereen kan joinen.",
             "<b>Normal difficulty</b>, <b>no PvP</b> (players can't hurt each other) and <b>open</b>: no whitelist, anyone can join."),
            ("<b>Geen claims</b>: je kunt je land niet op slot zetten. We vertrouwen op elkaar (zie de regels).",
             "<b>No claims</b>: you can't lock your land. We trust each other (see the rules)."),
            ("Elke nacht om <b>04:30</b> een <b>back-up</b> en om <b>05:00</b> een korte <b>herstart</b> (Nederlandse tijd). Ben je dan online? Even wachten en opnieuw verbinden.",
             "Every night at <b>04:30</b> a <b>backup</b> and at <b>05:00</b> a short <b>restart</b> (Dutch time, CET/CEST). Online at that moment? Wait a bit and reconnect."),
            (f"<b>Live kaart:</b> op {_ext(MAP_URL, 'map.guhs.nl')} zie je de hele wereld van de server, de Overworld én de Guhmensie. Kijk alvast rond!",
             f"<b>Live map:</b> on {_ext(MAP_URL, 'map.guhs.nl')} you can see the whole server world, the Overworld and the Guhmension. Have a look around!"),
            (f"Dankzij <b>Lootr</b> krijgt iedereen zijn eigen buit uit de kisten van de bouwwerken, en met {L('systemen/ftb-quests', 'FTB Quests')} "
             "heb je het hele Guhs-questboek. Vahoeg!",
             f"Thanks to <b>Lootr</b> everyone gets their own loot from the structure chests, and with {L('systemen/ftb-quests', 'FTB Quests')} "
             "you have the whole Guhs quest book. Vahoeg!"),
        ]) + f'<h3>{t("The mods", "De mods")}</h3>' + _p(
            f"Deze mods zitten in de instance (versies van Guhs {GUHS}); de server heeft precies dezelfde:",
            f"These mods are in the instance (versions of Guhs {GUHS}); the server has exactly the same:") + _modlist(MODS) + _p(
            "Alleen bij jou, niet op de server (niet verplicht):", "Only on your side, not on the server (optional):") + _modlist(CLIENT_MODS) + _p(
            f"Alleen op de server (daar merk je niks van, behalve de live kaart): {', '.join(SERVER_MODS)}.",
            f"Only on the server (you won't notice them, except for the live map): {', '.join(SERVER_MODS)}.") + _tip("Nieuw in Guhs? Lees ook even de gids " + L("systemen/aan-de-slag", "Aan de slag") + ": die werkt op de server precies hetzelfde.",
                  "New to Guhs? Also have a look at the " + L("systemen/aan-de-slag", "Getting started") + " guide: it works exactly the same on the server.")
        return self.step("wat", 1, "Wat is de officiële server?", "What is the official server?", body)

    # 2 ------------------------------------------------------------------------------------------------------------------------
    def s_prism(self):
        body = _p("Om te spelen heb je precies dezelfde mods nodig als de server. Dat is een hoop gedoe met de hand, dus doen we het met "
                  f"<b>Prism Launcher</b>: een gratis launcher die de mods voor je binnenhaalt én ze bijwerkt als de server verandert.",
                  "To play you need exactly the same mods as the server. Doing that by hand is a lot of fuss, so we use "
                  "<b>Prism Launcher</b>: a free launcher that fetches the mods for you and updates them when the server changes.") + _ul([
            (f"Download Prism Launcher van {_ext(PRISM_SITE, 'prismlauncher.org')} (Windows, macOS en Linux) en installeer hem.",
             f"Download Prism Launcher from {_ext(PRISM_SITE, 'prismlauncher.org')} (Windows, macOS and Linux) and install it."),
            ("Start hem. Vraagt hij bij de eerste start naar <b>Java</b>? Kies dan de aanbevolen instelling: Prism haalt zelf de juiste Java "
             f"(Java {JAVA} voor Minecraft {MC}). Zelf Java installeren hoeft dus niet.",
             "Start it. Does it ask about <b>Java</b> on first start? Pick the recommended option: Prism fetches the right Java itself "
             f"(Java {JAVA} for Minecraft {MC}). So no need to install Java yourself."),
            ("Zet in de instellingen (Settings &rarr; Java) het <b>geheugen</b> op 4 tot 6 GB (4096 tot 6144 MB). Minder is te krap, meer helpt niet.",
             "In the settings (Settings &rarr; Java) set the <b>memory</b> to 4 to 6 GB (4096 to 6144 MB). Less is too tight, more doesn't help."),
        ])
        return self.step("prism", 2, "Installeer Prism Launcher", "Install Prism Launcher", body)

    # 3 ------------------------------------------------------------------------------------------------------------------------
    def s_account(self):
        body = _ul([
            ("Klik rechtsboven op <b>Accounts</b> &rarr; <b>Manage Accounts</b> &rarr; <b>Add Microsoft</b>.",
             "Click <b>Accounts</b> at the top right &rarr; <b>Manage Accounts</b> &rarr; <b>Add Microsoft</b>."),
            ("Log in met het Microsoft-account waarmee je <b>Minecraft: Java Edition</b> hebt gekocht. Een gekochte Java Edition is nodig: "
             "de server controleert je account.",
             "Log in with the Microsoft account you bought <b>Minecraft: Java Edition</b> with. You need a bought Java Edition: "
             "the server checks your account."),
            ("Zie je je Minecraft-naam in de lijst staan? Dan zit je goed. Njeg!",
             "See your Minecraft name in the list? Then you're good. Njeg!"),
        ])
        return self.step("account", 3, "Voeg je Minecraft-account toe", "Add your Minecraft account", body)

    # 4 ------------------------------------------------------------------------------------------------------------------------
    def s_import(self):
        body = _p("Nu de Guhs-instance. Dat is een kant-en-klaar Minecraft met alle mods van de server, die zichzelf bijwerkt.",
                  "Now the Guhs instance. That's a ready-made Minecraft with all the server's mods, which keeps itself up to date.") + _ul([
            ("Klik linksboven op <b>Add Instance</b> en kies links <b>Import</b>.",
             "Click <b>Add Instance</b> at the top left and pick <b>Import</b> on the left."),
            (f"Plak deze link in het vak: {_code(PRISM_ZIP)}",
             f"Paste this link into the box: {_code(PRISM_ZIP)}"),
            ("Klik <b>OK</b>. Prism maakt een nieuwe instance aan (de naam mag je houden of veranderen).",
             "Click <b>OK</b>. Prism creates a new instance (keep the name or change it)."),
        ]) + _tip("Je hoeft zelf geen mods te downloaden. De instance haalt ze bij elke start op van het pack, en kijkt dan meteen of er iets nieuws is.",
                  "You don't have to download any mods yourself. The instance fetches them from the pack on every start, and checks for updates straight away.")
        return self.step("importeren", 4, "Importeer de Guhs-instance", "Import the Guhs instance", body)

    # 5 ------------------------------------------------------------------------------------------------------------------------
    def s_join(self):
        body = _ul([
            ("Dubbelklik op de instance (of kies <b>Launch</b>). De <b>eerste keer</b> duurt het even: er komt een venstertje dat de mods downloadt. "
             "Even geduld, een guh is ook niet in één hap vadsig.",
             "Double-click the instance (or pick <b>Launch</b>). The <b>first time</b> takes a while: a little window downloads the mods. "
             "Be patient, a guh doesn't get vadsig in one bite either."),
            ("Minecraft start. Kies <b>Multiplayer</b>.", "Minecraft starts. Pick <b>Multiplayer</b>."),
            (f"<b>Guhs Server</b> staat al bovenaan je lijst: de instance brengt hem mee, en Guhs {GUHS} zet hem er zelf ook in. Klik erop en dan "
             "<b>Join Server</b>. Vahoeg, je bent binnen!",
             f"<b>Guhs Server</b> is already at the top of your list: the instance brings it along, and Guhs {GUHS} adds it too. Click it and then "
             "<b>Join Server</b>. Vahoeg, you're in!"),
            (f"Zelf weggehaald? Klik <b>Add Server</b>, vul als adres {_code(ADDRESS)} in en klik <b>Done</b>.",
             f"Removed it yourself? Click <b>Add Server</b>, type {_code(ADDRESS)} as the address and click <b>Done</b>."),
        ]) + _tip(f"Bij je eerste bezoek sta je bij de spawn. Zet meteen een thuis neer met {_code('/sethome')}, dan kom je altijd terug met {_code('/home')}.",
                  f"On your first visit you stand at spawn. Set a home right away with {_code('/sethome')}, then {_code('/home')} always brings you back.")
        return self.step("joinen", 5, "Starten en joinen", "Start and join", body)

    # 6 ------------------------------------------------------------------------------------------------------------------------
    def s_handmatig(self):
        body = _p("Liever geen Prism? Het kan ook met de hand, maar dan moet je zelf alles bijhouden:",
                  "Rather not use Prism? You can do it by hand, but then you have to keep everything up to date yourself:") + _ul([
            (f"Installeer <b>NeoForge {NEOFORGE}</b> voor Minecraft {MC} (installer van {_ext(NEOFORGE_SITE, 'neoforged.net')}), of maak in je eigen launcher een "
             "NeoForge-profiel met precies die versie.",
             f"Install <b>NeoForge {NEOFORGE}</b> for Minecraft {MC} (installer from {_ext(NEOFORGE_SITE, 'neoforged.net')}), or make a NeoForge profile "
             "with exactly that version in your own launcher."),
            (f"Zet alle mods in de map <b>mods</b>, met <b>precies dezelfde versies</b> als de server. De lijst (met downloadlinks) staat in de "
             f"README van {_ext(PACK_REPO, 'de pack-repo')} en in de bestanden {_code('mods/*.pw.toml')}.",
             f"Put every mod in the <b>mods</b> folder, with <b>exactly the same versions</b> as the server. The list (with download links) is in the "
             f"README of {_ext(PACK_REPO, 'the pack repo')} and in the {_code('mods/*.pw.toml')} files."),
            ("Sodium en Mouse Tweaks zijn niet verplicht; al het andere wel.", "Sodium and Mouse Tweaks are optional; everything else is required."),
            ("Verandert het pack? Dan moet je zelf de nieuwe versies ophalen, anders kom je er niet meer in ('mod mismatch'). Daarom raden we Prism aan.",
             "Does the pack change? Then you have to fetch the new versions yourself, or you can't get in any more ('mod mismatch'). That's why we recommend Prism."),
        ]) + _tip(f"Kun je met packwiz overweg? Dan kun je ook zelf de pack-URL gebruiken: {_code(PACK_TOML)}",
                  f"Know your way around packwiz? You can use the pack URL directly: {_code(PACK_TOML)}")
        return self.step("handmatig", 6, "Handmatig (zonder Prism)", "By hand (without Prism)", body)

    # 7 ------------------------------------------------------------------------------------------------------------------------
    def s_regels(self):
        body = _p("Een guh is altijd lief, en dat verwachten we van jou ook. De regels zijn simpel:",
                  "A guh is always sweet, and we expect the same from you. The rules are simple:") + _ul([
            ("<b>Wees vriendelijk.</b> Geen schelden, pesten of spammen in de chat. Iedereen is welkom, ook beginners.",
             "<b>Be kind.</b> No swearing, bullying or spamming in chat. Everyone is welcome, beginners too."),
            ("<b>Sloop niets van een ander.</b> Er zijn geen claims, dus alles kan kapot. Doe het gewoon niet: niet slopen, niet stelen uit "
             "andermans kisten, geen guhs van een ander meenemen.",
             "<b>Don't wreck other people's stuff.</b> There are no claims, so anything can break. Just don't: no griefing, no stealing from "
             "other people's chests, no taking someone else's guhs."),
            ("<b>Bouw niet tegen een ander aan</b> zonder te vragen. Er is ruimte genoeg, de wereld is groot en de Guhmensie nog groter.",
             "<b>Don't build right up against someone else</b> without asking. There's plenty of room, the world is big and the Guhmension even bigger."),
            ("<b>Geen cheats of hacks</b> (x-ray, fly, dupes). Mods uit het pack en Sodium/Mouse Tweaks zijn prima.",
             "<b>No cheats or hacks</b> (x-ray, fly, dupes). Mods from the pack and Sodium/Mouse Tweaks are fine."),
            ("<b>Maak geen lag-machines</b> (enorme farms, duizenden guhs op een kluitje). De server is voor iedereen.",
             "<b>No lag machines</b> (huge farms, thousands of guhs in one spot). The server is for everyone."),
        ]) + _tip("Is er toch iets gesloopt? Niet zelf gaan terugpesten: meld het bij de beheerder. Er is elke nacht een back-up, dus er valt vaak nog wat te redden.",
                  "Did something get wrecked anyway? Don't take revenge: tell the admin. There's a backup every night, so things can often still be saved.",
                  "Oeps?", "Oops?")
        return self.step("regels", 7, "Serverregels", "Server rules", body)

    # 8 ------------------------------------------------------------------------------------------------------------------------
    def s_commandos(self):
        rows = [
            ("/sethome [naam]", "/sethome [name]", "Zet een thuis neer waar je staat.", "Set a home where you stand."),
            ("/home [naam]", "/home [name]", "Ga naar (een van) je thuis(sen).", "Go to (one of) your home(s)."),
            ("/delhome naam", "/delhome name", "Gooi een thuis weg.", "Delete a home."),
            ("/spawn", "/spawn", "Terug naar de spawn.", "Back to spawn."),
            ("/tpa speler", "/tpa player", "Vraag of je naar een andere speler mag teleporteren.", "Ask to teleport to another player."),
            ("/tpaccept &middot; /tpdeny", "/tpaccept &middot; /tpdeny", "Een teleportverzoek aannemen of weigeren.", "Accept or refuse a teleport request."),
        ]
        body = _p("Via <b>FTB Essentials</b> heb je een paar handige commando's (alleen op de server, niet in je eigen wereld):",
                  "Thanks to <b>FTB Essentials</b> you have a few handy commands (on the server only, not in your own world):") + \
            "<ul>" + "".join(f"<li>{_code(t(c_en, c_nl))}: {t(en, nl)}</li>" for c_nl, c_en, nl, en in rows) + "</ul>" + _ul([
                ("<b>J</b> opent de kaart van JourneyMap; <b>U</b> en <b>R</b> bij een voorwerp laten zien waar het voor is en hoe je het maakt (JEI).",
                 "<b>J</b> opens the JourneyMap map; <b>U</b> and <b>R</b> on an item show what it's used for and how to craft it (JEI)."),
                (f"Het questboek van {L('systemen/ftb-quests', 'FTB Quests')} zit in je inventaris (het boekje-knopje linksboven). Samen met vrienden quests doen? "
                 "Maak een team met de team-knop van FTB Teams.",
                 f"The {L('systemen/ftb-quests', 'FTB Quests')} quest book is in your inventory (the little book button at the top left). Want to do quests with friends? "
                 "Make a team with the FTB Teams team button."),
                (f"De {L('systemen/commandos', 'Guhs-commando')}'s zijn voor beheerders; als gewone speler heb je ze niet nodig.",
                 f"The {L('systemen/commandos', 'Guhs commands')} are for admins; as a normal player you don't need them."),
            ])
        return self.step("commandos", 8, "Handige commando's", "Handy commands", body)

    # 9 ------------------------------------------------------------------------------------------------------------------------
    def s_problemen(self):
        faq = [
            ("'Mod mismatch' of 'incompatible mods' bij het joinen", "'Mod mismatch' or 'incompatible mods' when joining",
             "Het pack is bijgewerkt en jouw instance nog niet. Sluit Minecraft helemaal af en start de instance <b>opnieuw</b> vanuit Prism: bij het starten "
             "haalt hij vanzelf de nieuwe mods op. Speel je zonder Prism? Werk dan de mods bij volgens de lijst in de pack-repo.",
             "The pack was updated and your instance wasn't yet. Close Minecraft completely and start the instance <b>again</b> from Prism: on start it "
             "fetches the new mods by itself. Playing without Prism? Then update the mods from the list in the pack repo."),
            ("Het spel crasht, loopt vast of is heel traag", "The game crashes, freezes or is very slow",
             "Meestal is het geheugen. Rechtsklik de instance &rarr; <b>Edit</b> &rarr; <b>Settings</b> &rarr; <b>Java</b>, vink geheugen aan en zet "
             "het maximum op <b>4096 tot 6144 MB</b> (4 tot 6 GB). Heeft je computer maar 8 GB? Neem dan 4 GB. Sluit ook zware programma's.",
             "Usually it's memory. Right-click the instance &rarr; <b>Edit</b> &rarr; <b>Settings</b> &rarr; <b>Java</b>, tick memory and set the "
             "maximum to <b>4096 to 6144 MB</b> (4 to 6 GB). Only 8 GB in your computer? Take 4 GB. Also close heavy programs."),
            ("Java-fout bij het starten ('wrong Java version')", "Java error on start ('wrong Java version')",
             f"Minecraft {MC} wil <b>Java {JAVA}</b>. Normaal haalt Prism die vanzelf. Toch een fout? In Prism: Settings &rarr; Java &rarr; <b>Download Java</b> en kies Java {JAVA} "
             f"(of <b>Auto-detect</b> als je hem al hebt). Handmatig spelen? Installeer een Java {JAVA} (bijvoorbeeld van Adoptium).",
             f"Minecraft {MC} wants <b>Java {JAVA}</b>. Normally Prism fetches it by itself. Still an error? In Prism: Settings &rarr; Java &rarr; <b>Download Java</b> and pick Java {JAVA} "
             f"(or <b>Auto-detect</b> if you already have it). Playing by hand? Install a Java {JAVA} (from Adoptium, for example)."),
            ("'Unknown host' of 'Can't connect to server'", "'Unknown host' or 'Can't connect to server'",
             f"Probeer het adres {_code(IP)} of {_code(HOST)}. Werkt niets? Misschien is het net 05:00 (herstart) of is de server even in onderhoud: "
             "wacht een paar minuten.",
             f"Try the address {_code(IP)} or {_code(HOST)}. Nothing works? It might be just 05:00 (restart) or the server is briefly under maintenance: "
             "wait a few minutes."),
            ("'Outdated client' of 'Outdated server'", "'Outdated client' or 'Outdated server'",
             f"Je speelt een andere Minecraft-versie. De server draait {MC}: start de Guhs-instance, niet je gewone Minecraft.",
             f"You're on a different Minecraft version. The server runs {MC}: start the Guhs instance, not your normal Minecraft."),
            ("'Failed to verify username' of 'Invalid session'", "'Failed to verify username' or 'Invalid session'",
             "Je login is verlopen. Herstart Prism, of haal je account weg en voeg hem opnieuw toe (Accounts &rarr; Manage Accounts).",
             "Your login expired. Restart Prism, or remove your account and add it again (Accounts &rarr; Manage Accounts)."),
            ("De Guhs Server staat niet in mijn lijst", "The Guhs Server isn't in my list",
             f"Klik bij Multiplayer op <b>Add Server</b> en vul {_code(ADDRESS)} in. Weggehaald? Dan komt hij niet vanzelf terug, maar zo zet je hem er weer in.",
             f"In Multiplayer click <b>Add Server</b> and enter {_code(ADDRESS)}. Removed it? It won't come back by itself, but this is how you add it again."),
        ]
        body = "".join(f'<div class="goal"><h3>{t(q_en, q_nl)}</h3>{_p(nl, en)}</div>' for q_nl, q_en, nl, en in faq)
        body = (_p("Vads, het wil niet? Kijk hier eerst:", "Vads, it won't work? Look here first:") +
                f'<div class="goals">{body}</div>')
        return self.step("problemen", 9, "Veelgestelde problemen", "Common problems", body)

    # 10 -----------------------------------------------------------------------------------------------------------------------
    def s_links(self):
        body = _ul([
            (f"Live kaart (Overworld en Guhmensie): {_ext(MAP_URL)}", f"Live map (Overworld and Guhmension): {_ext(MAP_URL)}"),
            (f"De startpagina van de server: {_ext('https://guhs.nl/', 'guhs.nl')}", f"The server's home page: {_ext('https://guhs.nl/', 'guhs.nl')}"),
            (f"Prism-import-URL: {_code(PRISM_ZIP)}", f"Prism import URL: {_code(PRISM_ZIP)}"),
            (f"Het pack (packwiz): {_ext(PACK_TOML)}", f"The pack (packwiz): {_ext(PACK_TOML)}"),
            (f"De pack-repo, met de modlijst: {_ext(PACK_REPO)}", f"The pack repo, with the mod list: {_ext(PACK_REPO)}"),
            (f"Prism Launcher: {_ext(PRISM_SITE)}", f"Prism Launcher: {_ext(PRISM_SITE)}"),
            (f"NeoForge: {_ext(NEOFORGE_SITE)}", f"NeoForge: {_ext(NEOFORGE_SITE)}"),
            (f"Guhs zelf: {_ext('https://github.com/coecomber/guhs', 'GitHub')} &middot; {_ext('https://modrinth.com/mod/guhs', 'Modrinth')} &middot; "
             f"{_ext('https://www.curseforge.com/minecraft/mc-mods/guhs', 'CurseForge')}",
             f"Guhs itself: {_ext('https://github.com/coecomber/guhs', 'GitHub')} &middot; {_ext('https://modrinth.com/mod/guhs', 'Modrinth')} &middot; "
             f"{_ext('https://www.curseforge.com/minecraft/mc-mods/guhs', 'CurseForge')}"),
        ])
        return self.step("links", 10, "Links", "Links", body)

    def outro(self):
        text = _p(f"Tot op {_code(ADDRESS)}! Neem kaasknabbels mee, zeg hoi in de chat en wees lief voor elkaars guhs. <b>Njeg!</b>",
                  f"See you on {_code(ADDRESS)}! Bring kaasknabbels, say hi in chat and be sweet to each other's guhs. <b>Njeg!</b>")
        return (f'<div class="gend">{text}'
                f'<p><a class="gbtn" href="@@systemen/aan-de-slag@@">{t("Getting started", "Aan de slag")}</a> '
                f'<a class="gbtn alt" href="@@index@@">{t("Back to the home page", "Terug naar de startpagina")}</a></p></div>')
