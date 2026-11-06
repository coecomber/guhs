"""
1.10.0 "Het Vadsrijk", round U, slice U-GILDEN: the Knuffelbonden (guilds), the Bondenvallei and the castles
(Java: feature/vadsrijk/gilden).

Only texts here (Dutch + real English). The castle templates are made by tools/vadsrijk/gilden/kasteel.py.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_gilden.py
"""
import json
import os

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")

K = "vadsrijk.gilden."
N = "vadsrijk.npc."
TEKSTEN = {
    # ranks
    K + "rang.leider": ("Leider", "Leader"),
    K + "rang.officier": ("Officier", "Officer"),
    K + "rang.lid": ("Lid", "Member"),
    K + "rang.rekruut": ("Rekruut", "Recruit"),
    K + "rang.regel": ("lv %s - %s bondsxp - %s leden - %s", "lv %s - %s bond xp - %s members - %s"),
    # castle levels
    K + "kasteel.1": ("Wachttoren", "Watchtower"),
    K + "kasteel.2": ("Torenhof", "Tower Court"),
    K + "kasteel.3": ("Burcht", "Stronghold"),
    K + "kasteel.4": ("Vesting", "Fortress"),
    K + "kasteel.5": ("Knuffelslot", "Cuddle Castle"),
    K + "kasteel.uitleg.1": ("Een roze wachttoren met de teleportsteen en de bondskluis.",
                             "A pink watchtower with the teleport stone and the bond vault."),
    K + "kasteel.uitleg.2": ("Een ommuurd hof met poortgebouw en de Trofeeënzaal: elke baas die jullie verslaan krijgt daar een plekje.",
                             "A walled court with a gatehouse and the Trophy Hall: every boss you beat gets a spot there."),
    K + "kasteel.uitleg.3": ("Een burcht met twee hoektorens, een smidse en een keuken (werkplaatsen van de Ambachten).",
                             "A stronghold with two corner towers, a smithy and a kitchen (workshops of the Crafts)."),
    K + "kasteel.uitleg.4": ("Een vesting met vier hoektorens, een naaiatelier, een brouwerij en een tuin.",
                             "A fortress with four corner towers, a sewing studio, a brewery and a garden."),
    K + "kasteel.uitleg.5": ("Het Knuffelslot: een grote donjon met een feestzaal vol kroonluchters, torenspitsen en alles erop en eraan.",
                             "The Cuddle Castle: a great keep with a hall full of chandeliers, spires and everything."),
    K + "kasteel.max": ("Jullie kasteel is helemaal af!", "Your castle is completely finished!"),
    K + "kasteel.niets": ("Je hebt niets bij je wat het kasteel nog nodig heeft.", "You carry nothing the castle still needs."),
    K + "kasteel.gedoneerd": ("%s bracht %s bouwmaterialen naar het kasteel.", "%s brought %s building materials to the castle."),
    K + "kasteel.gedoneerd_ok": ("Je gaf %s bouwmaterialen aan het kasteel. Dank je!", "You gave %s building materials to the castle. Thank you!"),
    K + "kasteel.nog_niet": ("Het kasteel kan nog niet verder: kijk bij /bond kasteel wat er nog mist.",
                             "The castle can't grow yet: see /bond kasteel for what's missing."),
    K + "kasteel.gebouwd": ("Het kasteel is uitgebouwd tot %s (niveau %s)!", "The castle has grown into a %s (level %s)!"),
    K + "kasteel.gebouwd_ok": ("Gebouwd: %s.", "Built: %s."),
    K + "kasteel.mist.level": ("Bondslevel %s", "Bond level %s"),
    K + "kasteel.mist.kas": ("Bondskas %s van %s", "Bond treasury %s of %s"),
    K + "kasteel.mist.mat": ("%s %s/%s", "%s %s/%s"),
    K + "kasteel.eis.level": ("Bondslevel %s", "Bond level %s"),
    K + "kasteel.eis.kas": ("Bondskas: %s van %s", "Bond treasury: %s of %s"),
    K + "kasteel.leeg": ("Dit kavel is nog vrij.", "This plot is still free."),
    K + "kasteel.van": ("Dit is het kasteel van %s.", "This is the castle of %s."),
    # perks
    K + "perk.basis": ("Bondschat, een naamplaatje, een banier en de Wachttoren met 1 kluisvak", "Bond chat, a name badge, a banner and the Watchtower with 1 vault tab"),
    K + "perk.rust": ("Bondsrust: offline tijd vult een rustpot; zolang die vol is krijg je %s%% meer gevechts-xp",
                      "Bond rest: time offline fills a rest pool; while it lasts you get %s%% more fighting xp"),
    K + "perk.rust2": ("Bondsrust II: de rustpot vult twee keer zo snel en wordt twee keer zo groot", "Bond rest II: the rest pool fills twice as fast and holds twice as much"),
    K + "perk.bank": ("Bondskluis: %s vakken", "Bond vault: %s tabs"),
    K + "perk.reis": ("Reiskorting %s%% op de luxe vlucht met de reiskaart", "%s%% discount on the luxury travel-map flight"),
    K + "perk.tp": ("Naar het kasteel teleporteren: elke %s minuten", "Teleport to the castle: every %s minutes"),
    K + "perk.goud": ("Een gouden rand om je bondsnaam op de ranglijst", "A golden frame round your bond name on the ranking"),
    # week tasks
    K + "taak.opgevrolijkt": ("Wezens opvrolijken", "Cheer up creatures"),
    K + "taak.bazen": ("Bazen verslaan", "Beat bosses"),
    K + "taak.quests": ("Quests afronden", "Finish quests"),
    K + "taak.ambacht": ("Ambachts-xp verdienen", "Earn craft xp"),
    K + "taak.levels": ("Levels omhoog", "Level ups"),
    K + "taak.opgevrolijkt.lang": ("Vrolijk samen %s wezens op", "Together, cheer up %s creatures"),
    K + "taak.bazen.lang": ("Versla samen %s keer een baas", "Together, beat a boss %s times"),
    K + "taak.quests.lang": ("Rond samen %s quests af", "Together, finish %s quests"),
    K + "taak.ambacht.lang": ("Verdien samen %s ambachts-xp", "Together, earn %s craft xp"),
    K + "taak.levels.lang": ("Ga samen %s keer een level omhoog", "Together, gain %s levels"),
    K + "taak.klaar": ("Weektaak klaar: %s! +%s bondsxp en %s in de kas.", "Week task done: %s! +%s bond xp and %s in the treasury."),
    K + "taak.alle": ("Alle drie de weektaken klaar! Nog eens +%s bondsxp.", "All three week tasks done! Another +%s bond xp."),
    # messages
    K + "level_op": ("%s is nu bondslevel %s!", "%s is now bond level %s!"),
    K + "auto_lid": ("%s heeft genoeg bijgedragen en is nu lid.", "%s has contributed enough and is now a member."),
    K + "opgericht": ("Je hebt de Knuffelbond %s opgericht! Jullie kavel met een wachttoren staat klaar in de Bondenvallei.",
                      "You founded the Cuddle Bond %s! Your plot with a watchtower is ready in the Bond Valley."),
    K + "opgericht.iedereen": ("%s richtte de Knuffelbond %s [%s] op!", "%s founded the Cuddle Bond %s [%s]!"),
    K + "uitgenodigd": ("%s is uitgenodigd.", "%s has been invited."),
    K + "uitnodiging": ("%s nodigt je uit voor", "%s invites you to"),
    K + "geweigerd": ("Uitnodiging geweigerd.", "Invitation declined."),
    K + "toegetreden": ("Welkom bij %s! Typ /bond voor je bondspaneel.", "Welcome to %s! Type /bond for your bond panel."),
    K + "nieuw_lid": ("%s is erbij gekomen. Welkom!", "%s joined. Welcome!"),
    K + "aangemeld": ("Je aanmelding bij %s is verstuurd. Een officier kijkt ernaar.", "Your application to %s has been sent. An officer will look at it."),
    K + "afgewezen": ("Aanvraag van %s afgewezen.", "Application of %s declined."),
    K + "goedgekeurd": ("%s is aangenomen.", "%s has been accepted."),
    K + "vertrokken": ("%s heeft de bond verlaten.", "%s left the bond."),
    K + "verlaten": ("Je hebt %s verlaten.", "You left %s."),
    K + "verwijderd": ("%s is uit de bond gezet door %s.", "%s was removed from the bond by %s."),
    K + "verwijderd_ok": ("%s is uit de bond gezet.", "%s was removed from the bond."),
    K + "jij_verwijderd": ("Je bent uit %s gezet.", "You were removed from %s."),
    K + "bevorderd": ("%s is bevorderd tot %s.", "%s was promoted to %s."),
    K + "gedegradeerd": ("%s is nu %s.", "%s is now %s."),
    K + "rang_ok": ("%s is nu %s.", "%s is now %s."),
    K + "nieuwe_leider": ("%s is de nieuwe leider van de bond!", "%s is the new leader of the bond!"),
    K + "overgedragen": ("Je hebt het leiderschap aan %s gegeven.", "You handed the leadership to %s."),
    K + "opgeheven": ("De Knuffelbond %s is opgeheven.", "The Cuddle Bond %s has been disbanded."),
    K + "opgeheven_ok": ("%s is opgeheven. De kas en het bouwdepot zijn naar jou gegaan.", "%s has been disbanded. The treasury and building depot went to you."),
    K + "werving_ok": ("Werving aangepast.", "Recruiting updated."),
    K + "banier_ok": ("Banier aangepast (ook in jullie kasteel).", "Banner updated (in your castle too)."),
    K + "kas_gift": ("%s stopte %s in de bondskas.", "%s put %s in the bond treasury."),
    K + "kas_ok": ("Je stopte %s in de bondskas.", "You put %s in the bond treasury."),
    K + "trofee": ("Nieuwe trofee in de Trofeeënzaal: %s!", "New trophy in the Trophy Hall: %s!"),
    K + "rust.vol": ("Bondsrust: je rustpot heeft nu %s xp (+50%% gevechts-xp tot hij leeg is).", "Bond rest: your rest pool now holds %s xp (+50%% fighting xp until it's empty)."),
    K + "chat.aan": ("Bondschat AAN: wat je typt gaat naar je bond. Begin met ! om één keer naar iedereen te praten.",
                     "Bond chat ON: what you type goes to your bond. Start with ! to talk to everyone once."),
    K + "chat.uit": ("Bondschat UIT: je praat weer met iedereen.", "Bond chat OFF: you talk to everyone again."),
    K + "verlaat.zeker": ("Weet je zeker dat je de bond wilt verlaten?", "Are you sure you want to leave the bond?"),
    K + "ophef.zeker": ("Weet je zeker dat je de bond wilt OPHEFFEN? Het kasteel verdwijnt.", "Are you sure you want to DISBAND the bond? The castle disappears."),
    # errors
    K + "fout.geen_bond": ("Je zit (met dit karakter) niet in een Knuffelbond.", "You are not in a Cuddle Bond (with this character)."),
    K + "fout.geen_bond_id": ("Die bond bestaat niet meer.", "That bond no longer exists."),
    K + "fout.klerk": ("Een bond oprichten kan alleen bij de Bondsklerk in de kanselarij van Hoogvads.",
                       "You can only found a bond at the Bond Clerk in the chancellery of Hoogvads."),
    K + "fout.al_in_bond": ("Je zit al in een bond.", "You are already in a bond."),
    K + "fout.te_vroeg": ("Een bond oprichten kan pas als zone 4 klaar is, of vanaf level %s.", "You can found a bond once zone 4 is done, or from level %s."),
    K + "fout.naam": ("Een bondsnaam is 3 tot 20 tekens: letters, cijfers, spaties en streepjes.", "A bond name is 3 to 20 characters: letters, digits, spaces and dashes."),
    K + "fout.tag": ("Een tag is 2 tot 4 letters of cijfers, zoals ROZE.", "A tag is 2 to 4 letters or digits, like PINK."),
    K + "fout.bezet": ("Die naam of tag is al van een andere bond.", "That name or tag already belongs to another bond."),
    K + "fout.munten": ("Je hebt %s nodig.", "You need %s."),
    K + "fout.recht": ("Dat mag jouw rang niet.", "Your rank can't do that."),
    K + "fout.zelf": ("Jezelf uitnodigen? Je bent er al!", "Invite yourself? You're already here!"),
    K + "fout.ander_al": ("%s zit al in een bond.", "%s is already in a bond."),
    K + "fout.vol": ("De bond is vol (max %s).", "The bond is full (max %s)."),
    K + "fout.geen_uitnodiging": ("Je hebt geen (geldige) uitnodiging.", "You have no (valid) invitation."),
    K + "fout.werft_niet": ("Die bond zoekt nu geen leden.", "That bond isn't recruiting right now."),
    K + "fout.min_level": ("Die bond neemt leden vanaf level %s.", "That bond takes members from level %s."),
    K + "fout.al_aangemeld": ("Je hebt je daar al aangemeld.", "You already applied there."),
    K + "fout.geen_aanvraag": ("Die aanvraag bestaat niet (meer).", "That application doesn't exist (any more)."),
    K + "fout.leider_eerst": ("Geef eerst het leiderschap aan iemand anders (/bond leden, ♛).", "First hand the leadership to someone else (/bond leden, ♛)."),
    K + "fout.geen_lid": ("%s zit niet in je bond.", "%s is not in your bond."),
    K + "fout.rang_hoog": ("Dat kan alleen bij iemand met een lagere rang.", "You can only do that to someone of a lower rank."),
    K + "fout.rang_grens": ("Hoger of lager kan niet.", "Can't go higher or lower."),
    K + "fout.bank_vol": ("Haal eerst de bondskluis leeg: er mag nooit iets verloren gaan.", "Empty the bond vault first: nothing may ever get lost."),
    K + "fout.patroon": ("Dat patroon ken ik niet.", "I don't know that pattern."),
    K + "fout.geen_kasteel": ("Jullie hebben nog geen kasteel.", "You don't have a castle yet."),
    K + "fout.rijk": ("Dat kan alleen in Het Vadsrijk.", "That only works in the Vadsrijk."),
    K + "fout.stad": ("Naar je kasteel teleporteren kan vanuit elke stad (Knuffelkamp, Zwamzoet, Kandijkom, Honingtak, Lampionhaven, Hoogvads).",
                      "You can teleport to your castle from any town (Knuffelkamp, Zwamzoet, Kandijkom, Honingtak, Lampionhaven, Hoogvads)."),
    K + "fout.steen": ("Raak eerst de teleportsteen in je kasteel aan.", "Touch the teleport stone in your castle first."),
    K + "fout.geen_terug": ("Er is geen plek om naar terug te gaan.", "There's no place to go back to."),
    K + "fout.hub_dicht": ("Daar ben je nog niet geweest (volg het verhaal).", "You haven't been there yet (follow the story)."),
    # teleports
    K + "tp.wacht": ("De teleportsteen rust nog %s.", "The teleport stone is resting for %s."),
    K + "tp.kasteel": ("Welkom thuis bij %s!", "Welcome home at %s!"),
    K + "tp.plein": ("Naar het plein van de Bondenvallei.", "To the square of the Bond Valley."),
    K + "tp.terug": ("Terug naar waar je vandaan kwam.", "Back to where you came from."),
    K + "tp.hub": ("Op naar %s!", "Off to %s!"),
    K + "hub.knuffelkamp": ("Knuffelkamp", "Knuffelkamp"),
    K + "hub.zwamzoet": ("Zwamzoet", "Zwamzoet"),
    K + "hub.kandijkom": ("Kandijkom", "Kandijkom"),
    K + "hub.honingtak": ("Honingtak", "Honingtak"),
    K + "hub.lampionhaven": ("Lampionhaven", "Lampionhaven"),
    K + "hub.hoogvads": ("Hoogvads", "Hoogvads"),
    K + "vallei.welkom": ("Welkom in de Bondenvallei! Elke Knuffelbond heeft hier een kavel met een eigen kasteel.",
                          "Welcome to the Bond Valley! Every Cuddle Bond has a plot here with its own castle."),
    K + "vallei.gevangen": ("Een wolkje ving je op en bracht je terug naar het plein.", "A little cloud caught you and brought you back to the square."),
    # the vault
    K + "bank.titel": ("Bondskluis - %s", "Bond vault - %s"),
    K + "bank.tab": ("Vak %s", "Tab %s"),
    K + "bank.tab_dicht": ("Vak %s (bondslevel %s)", "Tab %s (bond level %s)"),
    K + "bank.sorteer": ("Sorteren", "Sort"),
    K + "bank.info_ja": ("%s: je mag hier dingen in leggen en uit halen", "%s: you may put things in and take them out here"),
    K + "bank.info_nee": ("%s: je mag hier alleen dingen in leggen", "%s: you may only put things in here"),
    # the panel
    K + "paneel.titel": ("Je Knuffelbond", "Your Cuddle Bond"),
    K + "paneel.titel_geen": ("Knuffelbonden", "Cuddle Bonds"),
    K + "paneel.geen": ("Je zit (met dit karakter) nog niet in een Knuffelbond. Meld je aan bij een Bondenbord in elke stad, of word uitgenodigd.",
                        "You're not in a Cuddle Bond yet (with this character). Apply at a Bond Board in any town, or get invited."),
    K + "paneel.uitnodiging": ("Uitnodiging van %s!", "Invitation from %s!"),
    K + "paneel.level": ("lv %s", "lv %s"),
    K + "paneel.info": ("Rang: %s - %s leden - Kasteel: %s (%s) - Kas: %s", "Rank: %s - %s members - Castle: %s (%s) - Treasury: %s"),
    K + "paneel.rust": ("Bondsrust: nog %s xp met +50%%", "Bond rest: %s xp left at +50%%"),
    K + "paneel.aanvragen": ("%s aanmelding(en) wachten op je.", "%s application(s) are waiting for you."),
    K + "paneel.leden": ("Leden (%s/%s)", "Members (%s/%s)"),
    K + "paneel.aanvraag": ("Aanmelding: %s", "Application: %s"),
    K + "paneel.taken": ("Weektaken (nieuw elke maandag 06:00)", "Week tasks (new every Monday 06:00)"),
    K + "paneel.taken_uitleg": ("Elke klare taak: +%s bondsxp en %s in de kas; alle drie: nog +%s bondsxp.",
                                "Each finished task: +%s bond xp and %s in the treasury; all three: another +%s bond xp."),
    K + "paneel.perks": ("Bondsperks", "Bond perks"),
    K + "paneel.perks_uitleg": ("Bondsxp komt vanzelf: van wezens, bazen, quests, levels en ambachten van alle leden (max 600 per lid per dag).",
                                "Bond xp comes by itself: from creatures, bosses, quests, levels and crafts of every member (max 600 per member per day)."),
    K + "paneel.kasteel": ("Kasteel: %s (niveau %s)", "Castle: %s (level %s)"),
    K + "paneel.volgende": ("Volgende: %s (niveau %s). Nodig:", "Next: %s (level %s). Needed:"),
    K + "paneel.werving": ("Werving", "Recruiting"),
    K + "paneel.bord": ("Bondenbord: bonden die leden zoeken", "Bond Board: bonds looking for members"),
    K + "paneel.ranglijst": ("Ranglijst van de Knuffelbonden", "Ranking of the Cuddle Bonds"),
    K + "paneel.banier": ("Banier", "Banner"),
    K + "paneel.steen": ("Teleportsteen", "Teleport stone"),
    K + "paneel.uitleg": ("Wat is een Knuffelbond?", "What is a Cuddle Bond?"),
    K + "uitleg.1": ("Een Knuffelbond is een club van ridders die samen het Vadsrijk vrolijker maken. Lid worden kan overal: aan een Bondenbord in elke stad, of met een uitnodiging.",
                     "A Cuddle Bond is a club of knights who make the Vadsrijk happier together. You can join anywhere: at a Bond Board in any town, or with an invitation."),
    K + "uitleg.2": ("Een bond oprichten kan alleen hier in Hoogvads, bij de Bondsklerk. Het kost %s en kan als zone 4 klaar is, of vanaf level %s.",
                     "Founding a bond is only possible here in Hoogvads, at the Bond Clerk. It costs %s and you need zone 4 done, or level %s."),
    K + "uitleg.3": ("Alles wat leden doen (wezens, bazen, quests, ambachten) geeft bondsxp. Bondslevels geven handige perks, geen grote stats.",
                     "Everything members do (creatures, bosses, quests, crafts) gives bond xp. Bond levels give handy perks, no big stats."),
    K + "uitleg.4": ("Elke bond krijgt een kavel in de Bondenvallei met een kasteel dat jullie samen uitbouwen: van wachttoren tot Knuffelslot.",
                     "Every bond gets a plot in the Bond Valley with a castle you build up together: from watchtower to Cuddle Castle."),
    K + "werving.werft": ("Zoeken we leden?", "Are we recruiting?"),
    K + "werving.vrij": ("Hoe komen ze erbij?", "How do they join?"),
    K + "werving.min": ("Vanaf level %s", "From level %s"),
    K + "werving.tekst": ("Bordtekst: %s", "Board text: %s"),
    K + "bord.regel": ("lv %s - %s leden - vanaf lv %s", "lv %s - %s members - from lv %s"),
    K + "bord.leeg": ("Er zoekt nu nog geen bond leden.", "No bond is recruiting right now."),
    K + "bord.zelf": ("Zelf een bond oprichten? Dat kan bij de Bondsklerk in Hoogvads.", "Found a bond yourself? That's possible at the Bond Clerk in Hoogvads."),
    K + "banier.hover": ("(beweeg over de naam om de banier te zien)", "(hover over the name to see the banner)"),
    K + "banier.basis": ("Grondkleur:", "Base colour:"),
    K + "banier.laag": ("Laag %s:", "Layer %s:"),
    # buttons
    K + "knop.typ": ("Klik: het commando staat klaar in je chat, vul aan en druk Enter", "Click: the command is ready in your chat, fill in and press Enter"),
    K + "knop.ja": ("Ja", "Yes"),
    K + "knop.nee": ("Nee", "No"),
    K + "knop.bord": ("Bonden die leden zoeken", "Bonds looking for members"),
    K + "knop.ranglijst": ("Ranglijst", "Ranking"),
    K + "knop.uitleg": ("Uitleg", "Explain"),
    K + "knop.leden": ("Leden", "Members"),
    K + "knop.taken": ("Weektaken", "Week tasks"),
    K + "knop.perks": ("Perks", "Perks"),
    K + "knop.kasteel": ("Kasteel", "Castle"),
    K + "knop.chat_aan": ("Bondschat aan", "Bond chat on"),
    K + "knop.chat_uit": ("Bondschat uit", "Bond chat off"),
    K + "knop.thuis": ("Naar ons kasteel", "To our castle"),
    K + "knop.werving": ("Werving", "Recruiting"),
    K + "knop.banier": ("Banier", "Banner"),
    K + "knop.verlaat": ("Verlaten", "Leave"),
    K + "knop.bekijk": ("Bekijken", "View"),
    K + "knop.bevorder": ("Bevorderen", "Promote"),
    K + "knop.degradeer": ("Degraderen", "Demote"),
    K + "knop.verwijder": ("Uit de bond zetten", "Remove from the bond"),
    K + "knop.leider": ("Leiderschap overdragen", "Hand over leadership"),
    K + "knop.uitnodigen": ("Iemand uitnodigen", "Invite someone"),
    K + "knop.terug": ("Terug", "Back"),
    K + "knop.doneer": ("Bouwmaterialen geven", "Give building materials"),
    K + "knop.kas": ("Stop %s in de bondskas", "Put %s in the bond treasury"),
    K + "knop.bouw": ("Bouwen!", "Build!"),
    K + "knop.aan": ("Ja", "Yes"),
    K + "knop.uit": ("Nee", "No"),
    K + "knop.vrij": ("Meteen lid", "Join at once"),
    K + "knop.aanvraag": ("Na goedkeuring", "After approval"),
    K + "knop.wijzig": ("Wijzigen", "Change"),
    K + "knop.word_lid": ("Word lid", "Join"),
    K + "knop.aanmelden": ("Aanmelden", "Apply"),
    K + "knop.weg": ("Weghalen", "Remove"),
    K + "knop.laag_erbij": ("+ laag", "+ layer"),
    K + "knop.terug_waar": ("Terug naar waar ik was", "Back to where I was"),
    K + "knop.plein": ("Valleiplein", "Valley square"),
    K + "knop.hub_hover": ("Teleporteren (gebruikt de teleport-wachttijd)", "Teleport (uses the teleport wait time)"),
    K + "knop.oprichten": ("Bond oprichten", "Found a bond"),
    K + "knop.inventaris": ("Je Knuffelbond (/bond)", "Your Cuddle Bond (/bond)"),
    # banner patterns
    K + "patroon.rand": ("rand", "border"),
    K + "patroon.kruis": ("kruis", "cross"),
    K + "patroon.schuinkruis": ("schuin kruis", "saltire"),
    K + "patroon.bloem": ("bloem", "flower"),
    K + "patroon.cirkel": ("cirkel", "roundel"),
    K + "patroon.ruit": ("ruit", "lozenge"),
    K + "patroon.paal": ("paal", "pale"),
    K + "patroon.balk": ("balk", "fess"),
    K + "patroon.bovenhelft": ("bovenhelft", "top half"),
    K + "patroon.verloop": ("verloop", "gradient"),
    K + "patroon.schuin": ("schuine helft", "per bend"),
    K + "patroon.punt": ("punt", "chevron"),
    K + "patroon.tanden": ("tanden", "indented base"),
    K + "patroon.wereldbol": ("wereldbol", "globe"),
    # NPC talk
    K + "npc.klerk.hallo": ("Goedendag! Ik ben Zegeltje, de Bondsklerk. Bij mij schrijf je een nieuwe Knuffelbond in. Stempel erop, klaar!",
                            "Good day! I'm Zegeltje, the Bond Clerk. I register new Cuddle Bonds. Stamp on it, done!"),
    K + "npc.klerk.al": ("Jij zit al in een bond! Eén bond per ridder, dat is de regel.", "You're already in a bond! One bond per knight, that's the rule."),
    K + "npc.klerk.te_vroeg": ("Een eigen bond? Kom terug als je zone 4 hebt afgerond, of als je level %s bent. Lid worden van een bond kan wel al!",
                               "Your own bond? Come back once you've finished zone 4, or at level %s. Joining a bond is possible already!"),
    K + "npc.klerk.oprichten": ("Prachtig! Een bond kost %s inschrijfgeld. Kies een naam en een korte tag van 2 tot 4 tekens, dan stempel ik hem in.",
                                "Wonderful! A bond costs %s to register. Pick a name and a short tag of 2 to 4 characters, then I'll stamp it in."),
    K + "npc.klerk.typ": ("Bond oprichten: klik hier en vul je tag en naam in ->", "Found a bond: click here and fill in your tag and name ->"),
    K + "npc.poortwachter.hallo": ("Achter deze poort ligt de Bondenvallei: elke Knuffelbond heeft daar een eigen kasteel. Wil je erheen?",
                                   "Beyond this gate lies the Bond Valley: every Cuddle Bond has its own castle there. Want to go?"),
    K + "npc.pleinwachter.hallo": ("Welkom in de Bondenvallei! Ik breng je terug naar Hoogvads, of naar het kasteel van je bond.",
                                   "Welcome to the Bond Valley! I'll take you back to Hoogvads, or to your bond's castle."),
    K + "npc.bord.hallo": ("Kijk eens op het Bondenbord! Hier hangen de Knuffelbonden die nieuwe leden zoeken.",
                           "Have a look at the Bond Board! Here hang the Cuddle Bonds looking for new members."),
    K + "optie.oprichten": ("Ik wil een bond oprichten", "I want to found a bond"),
    K + "optie.uitleg": ("Wat is een Knuffelbond?", "What is a Cuddle Bond?"),
    K + "optie.bord": ("Welke bonden zoeken leden?", "Which bonds are recruiting?"),
    K + "optie.mijn": ("Mijn bond", "My bond"),
    K + "optie.vallei": ("Naar de Bondenvallei!", "To the Bond Valley!"),
    K + "optie.hoogvads": ("Terug naar Hoogvads", "Back to Hoogvads"),
    K + "optie.kasteel": ("Naar ons kasteel", "To our castle"),
    K + "optie.dag": ("Dag!", "Bye!"),
    # tips
    K + "tip.vallei": ("De Bondenvallei: op het plein staat de poort terug naar Hoogvads. Je kasteel vind je via /bond, [Naar ons kasteel].",
                       "The Bond Valley: the gate back to Hoogvads is on the square. Find your castle via /bond, [To our castle]."),
    K + "tip.vallei.titel": ("Bondenvallei", "Bond Valley"),
    K + "tip.bank": ("De bondskluis is van de hele bond. Rekruten mogen er alleen iets in leggen; leden ook uit vak 1 pakken; officieren overal.",
                     "The bond vault belongs to the whole bond. Recruits may only put things in; members may also take from tab 1; officers from every tab."),
    K + "tip.bank.titel": ("Bondskluis", "Bond vault"),
    # the area
    "vadsrijk.kern.gebied.bondenvallei": ("Bondenvallei", "Bond Valley"),
}

NPC_NAMEN = {
    "bond_klerk": ("Bondsklerk Zegeltje", "Bond Clerk Zegeltje"),
    "bond_poortwachter": ("Poortwachter Wolkenpoort", "Gatekeeper Cloudgate"),
    "bond_pleinwachter": ("Pleinwachter Pluim", "Square Warden Pluim"),
    "bond_bord_vallei": ("Bondenbord", "Bond Board"),
}
HUBS = ["knuffelkamp", "zwamzoet", "kandijkom", "honingtak", "lampionhaven", "hoogvads"]
for _h in HUBS:
    NPC_NAMEN["bond_bord_" + _h] = ("Bondenbord", "Bond Board")

PRAAT = {
    "klerk": [("Stempeltje hier, stempeltje daar!", "A little stamp here, a little stamp there!"),
              ("Een bond zonder tag is als een guh zonder oortjes.", "A bond without a tag is like a guh without ears."),
              ("Ik heb al meer bonden ingeschreven dan er wolkjes zijn. Bijna.", "I've registered more bonds than there are clouds. Almost.")],
    "poortwachter": [("De poort is altijd open voor vrienden.", "The gate is always open to friends."),
                     ("Daarachter staan de mooiste kastelen van het hele Vadsrijk.", "Behind it stand the finest castles of the whole Vadsrijk."),
                     ("Wolkje erin, wolkje eruit!", "Cloud in, cloud out!")],
    "pleinwachter": [("Elke kavel een kasteel, elk kasteel een bond.", "Every plot a castle, every castle a bond."),
                     ("Niet over de rand leunen, hè!", "Don't lean over the edge, now!"),
                     ("Het mooiste kasteel? Dat bouwen jullie samen.", "The finest castle? You build it together.")],
    "bord": [("Nieuwe briefjes! Wie zoekt er leden?", "New notes! Who's looking for members?"),
             ("Samen ben je sterker, en vrolijker.", "Together you're stronger, and happier."),
             ("Een bond vind je hier, vriendjes erbij.", "Find a bond here, and friends too.")],
}


def _praat_rol(npc):
    if npc.startswith("bond_bord_"):
        return "bord"
    return npc[len("bond_"):]


def alles():
    t = dict(TEKSTEN)
    for npc, (nl, en) in NPC_NAMEN.items():
        t[N + "naam." + npc] = (nl, en)
        for i, (pn, pe) in enumerate(PRAAT[_praat_rol(npc)], 1):
            t[N + "praat." + npc + "." + str(i)] = (pn, pe)
    return t


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


EN_OVERLAY = os.path.join("tools", "lang", "en", "c300_vr_u_gilden.json")


def _engels():
    """The real English goes into the overlay (make_v2.write_lang builds en_us from nl_nl + tools/lang/en)."""
    _schrijf(EN_OVERLAY, {k: v[1] for k, v in alles().items()})


def build(h):
    _engels()
    for k, (nl, en) in alles().items():
        h.lang(k, en, nl)   # (make_v2.lang(key, en, nl))


def _direct():
    _engels()
    t = alles()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in t.items()})
        _schrijf(p, data)
    print(f"vadsrijk_u_gilden: {len(t)} texts")


if __name__ == "__main__":
    _direct()
