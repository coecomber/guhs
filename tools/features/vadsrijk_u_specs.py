"""
1.10.0 "Het Vadsrijk", round U, slice U-SPECS: specialisations + the new job-advancement model (Java: feature/vadsrijk/klassen/spec).

Only texts (Dutch + real English): the six specs, their step titles and explanations, the re-laid tree (branch names, new nodes),
the spec screen, the switch rules, the migration message, the gear compare arrows, and a few overrides of vadsrijk_klassen.py
texts that still said "1x per uur" or "lv 10/20". Must run AFTER vadsrijk_klassen (it overrides some of its keys).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_specs.py
"""
import json
import os

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")

K = "vadsrijk.klassen."
TEKSTEN = {}


def t(key, nl, en):
    TEKSTEN[K + key] = (nl, en)


# ---- the six specs: name, role, style -----------------------------------------------------------------------------------------
SPECS = {
    "schildridder": ("Schildridder", "Shield Knight", "Zwaard en schild, tegenaanvallen", "Sword and shield, counters",
                     "Vaste combo's met je kussenzwaard. Vang klappen op met je Knuffelschild en sla dan keihard terug. Wil je tanken? "
                     "De Tank-tak staat ernaast in je skillboom.",
                     "Steady combos with your pillow sword. Catch hits on your Cuddle Shield, then strike back hard. Want to tank? The "
                     "Tank branch sits right next to it in your skill tree."),
    "reuzenkussen": ("Reuzenkussen", "Giant Pillow", "Trage, enorme zwaaien en sprongen", "Slow, huge sweeps and leaps",
                     "Trage, enorme zwaaien die ook alles rond je doel raken, en sprongen die de grond laten schudden. Een knots of "
                     "pluishamer voelt hier extra lekker.",
                     "Slow, huge sweeps that also hit everything around your target, and leaps that shake the ground. A club or fluff "
                     "hammer feels extra good here."),
    "scherpschutter": ("Scherpschutter", "Sharpshooter", "Van ver, kritieke treffers", "Long range, critical hits",
                       "Lange afstand en kritieke klappen. Je pijlen buigen naar je doel, schoten van ver doen meer, en je Knuffelpijl "
                       "wordt een opgeladen pijl.",
                       "Long range and critical hits. Your arrows curve to the target, far shots hit harder, and your Cuddle Arrow "
                       "becomes a charged shot."),
    "strikkenzetter": ("Strikkenzetter", "Trapper", "Vallen en een snelle boog", "Traps and a quick bow",
                       "Snelle schoten, strikken en pluisvallen. Je houdt vijanden vast, laat ze plakken en laat ze langzaam "
                       "leeglopen. Sterk tegen groepen. Wil je je groep helpen? De Steun-tak staat ernaast.",
                       "Quick shots, snares and fluff traps. You hold foes in place, make them sticky and wear them down over time. "
                       "Strong against groups. Want to help your group? The Support branch sits next to it."),
    "sterrenwever": ("Sterrenwever", "Starweaver", "Elementen, grote klappen", "Elements, big bursts",
                     "Element-explosies: je Vuurbal laat een plas achter, elke derde spreuk is gratis, en later spat het ijs uit "
                     "elkaar en brandt de grond.",
                     "Elemental bursts: your Fireball leaves a puddle, every third spell is free, and later the ice shatters and the "
                     "ground burns."),
    "zegenzanger": ("Zegenzanger", "Blessing Singer", "Liedjes die schade doen en helpen", "Songs that hurt foes and help friends",
                    "Je zingt terwijl je vecht: je lied maakt jou en je groep sterker, en elke 2 s raakt een liedpuls alle vijanden om "
                    "je heen. Wil je helen? De Heel-tak staat ernaast.",
                    "You sing while you fight: your song makes you and your group stronger, and every 2 s a song pulse hits every "
                    "foe around you. Want to heal? The Heal branch sits next to it."),
}
for _id, (nl, en, rol_nl, rol_en, kort_nl, kort_en) in SPECS.items():
    t(f"spec.{_id}", nl, en)
    t(f"spec.{_id}.rol", rol_nl, rol_en)
    t(f"spec.{_id}.kort", kort_nl, kort_en)
    _pre = {"schildridder": "r", "reuzenkussen": "r", "scherpschutter": "j", "strikkenzetter": "j"}.get(_id, "m")
    t(f"knoop.{_pre}_spec_{_id}", f"Specialisatie: {nl}", f"Specialisation: {en}")
    t(f"knoop.{_pre}_spec_{_id}.uitleg", f"Je koos {nl} bij je eerste job-advancement.", f"You chose {en} at your first job advancement.")

# ---- per spec and step: title, card line, explanation, short line --------------------------------------------------------------
STAPPEN = {
    "schildridder": [
        ("Schildridder", "Shield Knight", "Je schild is je beste vriend.", "Your shield is your best friend.",
         "Dubbelklap: Kussenklap slaat meteen nog een keer (60 %). Tegenstoot: vang je met je Knuffelschild een klap op, dan doet je "
         "volgende Kussenklap binnen 4 s 50 % meer.",
         "Double Smack: Pillow Smack swings again (60 %). Counter: catch a hit with your Cuddle Shield and your next Pillow Smack within "
         "4 s does 50 % more.",
         "Dubbelklap + Tegenstoot", "Double Smack + Counter"),
        ("Kristalkampioen", "Crystal Champion", "Wie je schild raakt, bevriest.", "Whoever hits your shield freezes.",
         "Kristalwal: zolang je Knuffelschild op staat, bevriest elke vijand die jou raakt 3 s. Kussenklap doet 40 % meer op bevroren "
         "vijanden.",
         "Crystal Wall: while your Cuddle Shield is up, every foe that hits you freezes for 3 s. Pillow Smack does 40 % more to frozen foes.",
         "Kristalwal: schild bevriest, +40 % op bevroren", "Crystal Wall: shield freezes, +40 % vs frozen"),
        ("Vahoege Paladijn", "Vahoege Paladin", "Een muur van pluis.", "A wall of fluff.",
         "Bolwerkplof: Vahoege Plof doet 30 % meer en verdooft alles wat hij raakt 2 s. Kussenklap doet 15 % meer.",
         "Bulwark Flop: Vahoege Flop does 30 % more and stuns everything it hits for 2 s. Pillow Smack does 15 % more.",
         "Ult: Bolwerkplof (+30 %, verdooft)", "Ult: Bulwark Flop (+30 %, stuns)"),
    ],
    "reuzenkussen": [
        ("Reuzenkussen", "Giant Pillow", "Groot, zacht en onhoudbaar.", "Big, soft and unstoppable.",
         "Reuzenzwaai: je gewone klappen doen 30 % meer en raken ook vijanden binnen 3 blokken van je doel (50 %). Kussenklap doet "
         "60 % meer, maar laadt 1 s langer op.",
         "Giant Sweep: your basic hits do 30 % more and also hit foes within 3 blocks of your target (50 %). Pillow Smack does 60 % "
         "more but recharges 1 s slower.",
         "Reuzenzwaai: zwaaien die alles raken", "Giant Sweep: swings that hit everything"),
        ("Stormkussen", "Storm Pillow", "De grond schudt als jij landt.", "The ground shakes when you land.",
         "Kussenkoprol: Stormsprong doet 60 % meer, is 2 s sneller weer klaar en verdooft wat hij raakt 1 s.",
         "Pillow Roll: Storm Leap does 60 % more, is ready 2 s sooner and stuns what it hits for 1 s.",
         "Kussenkoprol: sterkere sprong", "Pillow Roll: a stronger leap"),
        ("Nestwachter", "Nest Warden", "Vleugels van kussens.", "Wings of pillows.",
         "Nestvorm: Vahoege Plof doet 30 % meer en zijn wind trekt alle vijanden binnen 8 blokken naar het midden. Gewone klappen nog "
         "10 % harder.",
         "Nest Form: Vahoege Flop does 30 % more and its wind pulls every foe within 8 blocks to the middle. Basic hits 10 % harder still.",
         "Ult: Nestvorm (+30 %, trekt vijanden)", "Ult: Nest Form (+30 %, pulls foes)"),
    ],
    "scherpschutter": [
        ("Scherpschutter", "Sharpshooter", "Eén oog dicht, één schot raak.", "One eye closed, one shot home.",
         "Valkenblik: je pijlen buigen een beetje naar je doel, +8 % kans op kritiek en +15 % kritieke schade. Verre schoten: gewone "
         "schoten op vijanden verder dan 10 blokken doen 30 % meer.",
         "Falcon Gaze: your arrows curve a little to the target, +8 % crit chance and +15 % crit damage. Far shots: basic shots at foes "
         "further than 10 blocks do 30 % more.",
         "Valkenblik + verre schoten", "Falcon Gaze + far shots"),
        ("Sterrenschutter", "Star Shooter", "Span, wacht, los.", "Draw, wait, loose.",
         "Opgeladen pijl: je Knuffelpijl doet 80 % meer schade op vijanden.",
         "Charged Arrow: your Cuddle Arrow does 80 % more damage to foes.",
         "Opgeladen Knuffelpijl (+80 %)", "Charged Cuddle Arrow (+80 %)"),
        ("Kruinvalk", "Crown Falcon", "Niets ontsnapt aan jouw oog.", "Nothing escapes your eye.",
         "Kruinvalk: Sterrenschot doet 40 % meer en nog een kwart extra als ware schade, dwars door schilden. Gewone schoten 8 % harder.",
         "Crown Falcon: Star Shot does 40 % more plus a quarter again as true damage, right through shields. Basic shots 8 % harder.",
         "Ult: Sterrenschot +40 %, door schilden", "Ult: Star Shot +40 %, through shields"),
    ],
    "strikkenzetter": [
        ("Strikkenzetter", "Trapper", "Kijk uit waar je loopt.", "Watch your step.",
         "Saltosalvo: midden in je Rolsalto schiet je 3 pijlen op de dichtstbijzijnde vijand. Strikje: waar je wegrolt, blijft 6 s een "
         "val liggen: wie erin stapt, plakt 3 s en wordt Gespoord (4 s schade). Snelle boog: 8 % aanvalssnelheid.",
         "Somersault Volley: mid-roll you fire 3 arrows at the nearest foe. Snare: where you roll away, a trap stays for 6 s: whoever "
         "steps in sticks for 3 s and is Tracked (4 s of damage). Quick bow: 8 % attack speed.",
         "Saltosalvo + Strikje + snelle boog", "Volley + Snare + quick bow"),
        ("Poolstervalk", "Pole Star Falcon", "Ijzige vallen.", "Icy traps.",
         "IJsster: elk 5e gewone schot dat raakt, schiet een ijsster mee (1x wapenschade, bevriest 2 s). Sterrenschot bevriest alles "
         "wat het doorboort 4 s.",
         "Ice Star: every 5th basic shot that hits fires an ice star along (1x weapon damage, freezes 2 s). Star Shot freezes everything "
         "it pierces for 4 s.",
         "IJsster: bevriezen", "Ice Star: freezing"),
        ("Vahoege Strikmeester", "Vahoege Trapmaster", "Het hele veld is jouw val.", "The whole field is your trap.",
         "Strikkenregen: waar Sterrenschot inslaat, ligt 5 s een strikkenveld (straal 4): elke seconde 40 % schade en Plakkerig. Nog 8 % "
         "aanvalssnelheid.",
         "Snare Rain: where Star Shot lands, a snare field lies for 5 s (radius 4): 40 % damage and Sticky every second. 8 % more "
         "attack speed.",
         "Ult: Strikkenregen (veld)", "Ult: Snare Rain (field)"),
    ],
    "sterrenwever": [
        ("Sterrenwever", "Starweaver", "De sterren dansen op jouw maat.", "The stars dance to your beat.",
         "Je Roze Vuurbal laat 3 s een elementplas achter. Vadsoverloop: elke 3e vaardigheid geeft zijn energie terug; +10 % "
         "cooldownreductie en +8 % elementschade.",
         "Your Pink Fireball leaves an element puddle for 3 s. Vads Overflow: every 3rd skill gives its energy back; +10 % cooldown "
         "reduction and +8 % element damage.",
         "Elementplas + Vadsoverloop", "Element puddle + Vads Overflow"),
        ("Kristalorakel", "Crystal Oracle", "IJs dat zingt als het breekt.", "Ice that sings as it breaks.",
         "Spiegelecho: je Vuurbal bevriest 1,5 s; raakt hij een vijand die al bevroren was, dan spat het ijs uit elkaar (1,2x schade, "
         "straal 3).",
         "Mirror Echo: your Fireball freezes for 1.5 s; when it hits a foe that was already frozen, the ice shatters (1.2x damage, radius 3).",
         "Spiegelecho: ijs spat uit elkaar", "Mirror Echo: shattering ice"),
        ("Schemerwijze", "Twilight Sage", "Waar jij toverde, gloeit de grond.", "Where you cast, the ground glows.",
         "Schemerwijze: Meteoorguh laat 5 s een roze vuurveld achter (elke seconde 30 % schade + Brandend). Nog 8 % elementschade.",
         "Twilight Sage: Meteor Guh leaves a pink fire field for 5 s (30 % damage + Burning every second). 8 % more element damage.",
         "Ult: vuurveld na Meteoorguh", "Ult: fire field after Meteor Guh"),
    ],
    "zegenzanger": [
        ("Zegenzanger", "Blessing Singer", "La la la, en pats.", "La la la, and bonk.",
         "Strijdlied: jij doet 15 % meer schade, je groep binnen 10 blokken 8 % meer. In een gevecht raakt elke 2 s een liedpuls alle "
         "vijanden binnen 6 blokken (50 % wapenschade).",
         "Battle Song: you do 15 % more damage, your group within 10 blocks 8 % more. In a fight a song pulse hits every foe within "
         "6 blocks every 2 s (50 % weapon damage).",
         "Strijdlied: aura + liedpuls", "Battle Song: aura + song pulse"),
        ("Koorvads", "Choir of Vads", "Iedereen zingt mee.", "Everybody sing along.",
         "Zangkring: je Vadsbron zingt mee: 6 s lang krijgt elke vijand erin elke seconde 40 % wapenschade.",
         "Song Circle: your Vads Spring sings along: for 6 s every foe inside takes 40 % weapon damage each second.",
         "Zangkring: Vadsbron doet schade", "Song Circle: Vads Spring hurts foes"),
        ("Vahoege Aartszanger", "Vahoege Archsinger", "Het slotakkoord van het rijk.", "The final chord of the realm.",
         "Slotakkoord: Meteoorguh doet 30 % meer en geeft je groep 8 s Vahoeg (+20 % schade).",
         "Final Chord: Meteor Guh does 30 % more and gives your group 8 s of Vahoeg (+20 % damage).",
         "Ult: Slotakkoord (groep +20 %)", "Ult: Final Chord (group +20 %)"),
    ],
}
for _id, stappen in STAPPEN.items():
    for _i, (titel_nl, titel_en, onder_nl, onder_en, uit_nl, uit_en, kort_nl, kort_en) in enumerate(stappen, 1):
        t(f"titel.{_id}.{_i}", titel_nl, titel_en)
        t(f"adv.onder.{_id}.{_i}", onder_nl, onder_en)
        t(f"adv.uitleg.{_id}.{_i}", uit_nl, uit_en)
        t(f"adv.kort.{_id}.{_i}", kort_nl, kort_en)
for _i, (lv, waar_nl, waar_en) in enumerate(((17, "bij de Grootmeester in Zwamzoet", "with the Grootmeester in Zwamzoet"),
                                               (35, "in de Nestkapel van Honingtak (zone 3)", "in the Nest Chapel of Honingtak (zone 3)"),
                                               (58, "bij Kapitein Veerkracht in Hoogvads (zone 5)", "with Captain Veerkracht in Hoogvads (zone 5)")), 1):
    t(f"adv.uitleg.geen.{_i}", f"Job-advancement {_i}, op klasse-level {lv}, {waar_nl}. Wat je krijgt hangt af van je specialisatie.",
      f"Job advancement {_i}, at class level {lv}, {waar_en}. What you get depends on your specialisation.")
    t(f"knoop.r_adv{_i}", f"Job-advancement {_i}", f"Job advancement {_i}")
    t(f"knoop.j_adv{_i}", f"Job-advancement {_i}", f"Job advancement {_i}")
    t(f"knoop.m_adv{_i}", f"Job-advancement {_i}", f"Job advancement {_i}")
    for _p in "rjm":
        t(f"knoop.{_p}_adv{_i}.uitleg", f"Behaald in de ceremonie op klasse-level {lv}.", f"Earned in the ceremony at class level {lv}.")
t("adv.klaar1", "✦ Je %s is level 17! Ga naar Grootmeester Vadsvoet in Zwamzoet: daar kies je je specialisatie.",
  "✦ Your %s is level 17! Go to Grootmeester Vadsvoet in Zwamzoet to choose your specialisation.")
t("adv.klaar2", "✦ Je %s is level 35! In zone 3 wacht Ordemeester Wasja in de Nestkapel op je (na de Kaaszeppelinwerf).",
  "✦ Your %s is level 35! In zone 3 Ordemeester Wasja waits for you in the Nest Chapel (after the Cheese Zeppelin Yard).")
t("adv.klaar3", "✦ Je %s is level 58! Na het slaaplied van Moeder Wolkwal wacht Kapitein Veerkracht in Hoogvads op je.",
  "✦ Your %s is level 58! After Mother Wolkwal's lullaby, Captain Veerkracht waits for you in Hoogvads.")

# ---- the re-laid tree: branches and new nodes --------------------------------------------------------------------------------
t("tak.stam", "Stam", "Trunk")
t("tak.stam.uitleg", "Voor iedereen van deze klasse, welke specialisatie je ook kiest.", "For everyone of this class, whatever specialisation you choose.")
t("tak.zij.knuffelridder", "Tank", "Tank")
t("tak.zij.knuffelridder.uitleg", "Optioneel: taaier worden en je groep beschermen.", "Optional: get tougher and protect your group.")
t("tak.zij.pluisjager", "Steun", "Support")
t("tak.zij.pluisjager.uitleg", "Optioneel: je groep helpen met heelpijlen, schildjes en markeringen.",
  "Optional: help your group with healing arrows, small shields and marks.")
t("tak.zij.vadsmagier", "Heel", "Heal")
t("tak.zij.vadsmagier.uitleg", "Optioneel: helen en beschermen.", "Optional: healing and protecting.")
t("tak.dicht_nog", "Kies eerst je specialisatie (klasse-level 17, bij de Grootmeester).",
  "First choose your specialisation (class level 17, with the Grootmeester).")
t("tak.dicht_ander", "Dit is de andere specialisatie. Wisselen kan in elke stad bij de klassemeester.",
  "This is the other specialisation. You can switch in any town at the class master.")
NIEUW = {
    "r_schildarm": ("Schildarm", "Shield Arm", "Meer Verdediging en wat Kracht. Je schild zit vastgegroeid.", "More Defence and some Strength."),
    "r_vaste_hand": ("Vaste Hand", "Steady Hand", "Meer kans op kritiek en sneller slaan.", "More crit chance and faster swings."),
    "r_schildmeester": ("Schildmeester", "Shield Master", "Verdediging, Kracht en kritiek in één.", "Defence, Strength and crit in one."),
    "r_schildmuur": ("Schildmuur", "Shield Wall", "Veel Verdediging en hardere kritieke klappen.", "Lots of Defence and harder crits."),
    "r_onbreekbaar": ("Onbreekbaar", "Unbreakable", "Elke klap die je met je Knuffelschild opvangt, maakt Kussenklap 1 s sneller weer klaar.",
                      "Every hit you catch on your Cuddle Shield makes Pillow Smack ready 1 s sooner."),
    "r_zware_zwaai": ("Zware Zwaai", "Heavy Swing", "Meer Kracht en hardere kritieke klappen.", "More Strength and harder crits."),
    "r_reuzenkracht": ("Reuzenkracht", "Giant Strength", "Veel Kracht en extra Vads.", "Lots of Strength and extra Vads."),
    "r_reuzenplof": ("Reuzenplof", "Giant Flop", "Na Stormsprong doet je volgende Kussenklap (binnen 4 s) 60 % meer schade.",
                     "After Storm Leap your next Pillow Smack (within 4 s) does 60 % more damage."),
    "r_paladijnsaura": ("Paladijnsaura", "Paladin Aura", "Elke 2 s helen jij en je groep binnen 10 blokken 1,5 % van hun Vads.",
                        "Every 2 s you and your group within 10 blocks heal 1.5 % of their Vads."),
    "j_spanpees": ("Strakke Pees", "Taut String", "Hardere kritieke klappen en wat Behendigheid.", "Harder crits and some Agility."),
    "j_sluipschutter": ("Sluipschutter", "Sniper", "Meer kans op kritiek en veel hardere kritieke klappen.", "More crit chance and much harder crits."),
    "j_strikdraad": ("Strikdraad", "Snare Wire", "Wat Behendigheid en sneller schieten.", "Some Agility and faster shooting."),
    "j_valstrikken": ("Valstrikken", "Pitfalls", "Behendigheid, kritiek en aanvalssnelheid.", "Agility, crit and attack speed."),
    "j_strikkenweb": ("Strikkenweb", "Snare Web", "Je gewone schoten doen 25 % meer schade op vijanden die Plakkerig zijn.",
                      "Your basic shots do 25 % more damage to Sticky foes."),
    "j_pijlenkoker": ("Volle Koker", "Full Quiver", "Behendigheid en kritiek.", "Agility and crit."),
    "j_zorgzaam": ("Zorgzaam", "Caring", "Meer Heal-kracht en extra Vads.", "More Healing Power and extra Vads."),
    "m_sterrenvonk": ("Sterrenvonk", "Star Spark", "Meer Elementschade en wat Wijsheid.", "More Element Damage and some Wisdom."),
    "m_zangstem": ("Zangstem", "Singing Voice", "Wijsheid en cooldownreductie.", "Wisdom and cooldown reduction."),
    "m_koorkracht": ("Koorkracht", "Choir Power", "Veel Wijsheid en extra Vads.", "Lots of Wisdom and extra Vads."),
    "m_slotlied": ("Slotlied", "Closing Song", "Je Strijdlied geeft jou 5 % meer schade en de liedpuls reikt 2 blokken verder.",
                   "Your Battle Song gives you 5 % more damage and the song pulse reaches 2 blocks further."),
    "m_warme_handen": ("Warme Handen", "Warm Hands", "Meer Heal-kracht en wat Vads.", "More Healing Power and some Vads."),
    "m_hartenbron": ("Hartenbron", "Heart Spring", "Heal-kracht en Wijsheid.", "Healing Power and Wisdom."),
}
for _id, (nl, en, uit_nl, uit_en) in NIEUW.items():
    t(f"knoop.{_id}", nl, en)
    t(f"knoop.{_id}.uitleg", uit_nl, uit_en)

# ---- the spec screen + rules --------------------------------------------------------------------------------------------------
t("spec.kop", "Specialisatie", "Specialisation")
t("spec.onderkop", "%s: twee wegen. Wisselen kan in elke stad bij de klassemeester (1x per 10 minuten).",
  "%s: two paths. Switch in any town at the class master (once per 10 minutes).")
t("spec.onderkop.kies", "%s: kies je weg! Allebei doen ze goed schade, ook alleen.", "%s: choose your path! Both deal good damage, also solo.")
t("spec.word", "Word %s!", "Become %s!")
t("spec.word.tip", "Je eerste job-advancement: een nieuwe titel, een nieuwe kracht en je eigen tak in de skillboom.",
  "Your first job advancement: a new title, a new power and your own branch in the skill tree.")
t("spec.jouw", "Jouw specialisatie", "Your specialisation")
t("spec.wissel", "Wissel naar %s", "Switch to %s")
t("spec.wissel.tip", "Gratis. Je punten in de oude spec-tak komen terug. Daarna 10 minuten wachten.",
  "Free. Your points in the old spec branch come back. Then wait 10 minutes.")
t("spec.later", "Vanaf klasse-level 17", "From class level 17")
t("spec.later.tip", "Je kiest je specialisatie bij Grootmeester Vadsvoet in Zwamzoet, als je klasse level 17 is.",
  "You choose your specialisation with Grootmeester Vadsvoet in Zwamzoet when your class is level 17.")
t("spec.stap_lv", "nv %s", "lv %s")
t("spec.knop", "✦ Specialisatie", "✦ Specialisation")
t("spec.knop.kies", "✦ Kies je specialisatie!", "✦ Choose your specialisation!")
t("spec.knop.tip", "Bekijk allebei de specialisaties van deze klasse, of wissel (1x per 10 minuten, niet in een gevecht).",
  "See both specialisations of this class, or switch (once per 10 minutes, not in a fight).")
t("spec.gewisseld", "✦ Je bent nu %s! Je punten uit de oude tak zijn terug.", "✦ You are now %s! Your points from the old branch are back.")
t("spec.migratie", "✦ De klassen zijn vernieuwd! Je skillboom is opnieuw ingedeeld: al je punten zijn terug (gratis). Je job-advancements "
  "zijn omgezet naar het nieuwe model. Kijk met K.",
  "✦ The classes were renewed! Your skill tree was re-laid: all your points are back (free). Your job advancements were moved to "
  "the new model. Have a look with K.")
t("spec.kies_nog", "✦ Je %s mag nog een specialisatie kiezen! Druk op K en klik op 'Kies je specialisatie'.",
  "✦ Your %s may still choose a specialisation! Press K and click 'Choose your specialisation'.")
t("spec.optie_balie", "Skillboom, specialisatie en reset", "Skill tree, specialisation and reset")
t("spec.geen", "(nog geen)", "(none yet)")
t("spec.cmd.info", "%s nv %s: specialisatie %s, job-advancement %s (%s)", "%s lv %s: specialisation %s, job advancement %s (%s)")
t("spec.cmd.wissel", "Volgende spec-wissel mogelijk over: %s", "Next spec switch possible in: %s")
t("spec.nee.al", "Je hebt al een specialisatie. Wisselen doe je bij de klassemeester.", "You already have a specialisation. Switch at the class master.")
t("spec.nee.nog_niet", "Je specialisatie kies je bij Grootmeester Vadsvoet, op klasse-level 17.",
  "You choose your specialisation with Grootmeester Vadsvoet, at class level 17.")
t("spec.nee.geen", "Je hebt nog geen specialisatie.", "You have no specialisation yet.")
t("spec.nee.balie", "Wisselen kan alleen in een stad, bij de klassemeester.", "You can only switch in a town, at the class master.")
t("spec.nee.gevecht", "Niet midden in een gevecht! Wacht even tot het rustig is.", "Not in the middle of a fight! Wait until it is calm.")
t("spec.nee.arena", "Niet in een bazenarena of kerker.", "Not in a boss arena or dungeon.")
t("spec.nee.wacht", "Je kunt weer wisselen over %s.", "You can switch again in %s.")

# ---- gear compare --------------------------------------------------------------------------------------------------------------
t("vergelijk.kop", "Vergeleken met je %s:", "Compared with your %s:")
t("vergelijk.leeg", "Je draagt nog niets als %s:", "You wear nothing as %s yet:")
t("vergelijk.gedragen", "(dit draag je nu)", "(you are wearing this)")
t("vergelijk.schade", "Schade per klap", "Damage per hit")
t("vergelijk.beter", "▲ Beter dan wat je draagt", "▲ Better than what you have")
t("vergelijk.slechter", "▼ Slechter dan wat je draagt", "▼ Worse than what you have")
t("vergelijk.gelijk", "= Ongeveer even goed", "= About as good")
for _slot, nl, en in (("wapen", "wapen", "weapon"), ("hoofd", "helm", "helmet"), ("borst", "borststuk", "chest piece"),
                      ("benen", "broek", "leggings"), ("voeten", "schoenen", "boots"), ("ring", "ring", "ring"), ("ring2", "ring", "ring"),
                      ("ketting", "ketting", "necklace"), ("oorbel", "oorbel", "earring"), ("armband", "armband", "bracelet"),
                      ("handschoenen", "handschoenen", "gloves"), ("cape", "cape", "cape")):
    t(f"vergelijk.slot.{_slot}", nl, en)

# ---- overrides of older klassen texts (respec always free, new levels) ---------------------------------------------------------
t("respec.klaar", "Je skillboom is leeg en je punten zijn terug. Kies opnieuw! (Resetten is altijd gratis.)",
  "Your skill tree is empty and your points are back. Choose again! (Resetting is always free.)")
t("scherm.ordetempel", "Je bent altijd alle drie de klassen: val aan met een ander wapen om te wisselen. Resetten (altijd gratis) en je "
  "specialisatie wisselen doe je bij de klassemeester in elke stad.",
  "You are always all three classes: attack with another weapon to switch. Reset (always free) and switch your specialisation at the "
  "class master in any town.")
t("scherm.respec.tip", "Al je punten in deze klasse terug. Altijd gratis. Je specialisatie en advancements blijven.",
  "All your points in this class back. Always free. Your specialisation and advancements stay.")
t("scherm.respec.uitleg", "Resetten is altijd gratis:", "Resetting is always free:")
t("scherm.adv.dicht", "Vanaf niveau %s, via de klasse-quest.", "From level %s, through the class quest.")
t("tip.adv", "Je klasse is klaar voor een job-advancement! Kijk in je questlog welke klassemeester op je wacht.",
  "Your class is ready for a job advancement! Check your quest log for the class master who is waiting for you.")


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


EN_OVERLAY = os.path.join("tools", "lang", "en", "c300_vr_u_specs.json")


def _engels():
    """The real English goes into the overlay (make_v2.write_lang builds en_us from nl_nl + tools/lang/en)."""
    _schrijf(EN_OVERLAY, {k: v[1] for k, v in TEKSTEN.items()})


def build(h):
    _engels()
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)   # (make_v2.lang(key, en, nl))


def _direct():
    _engels()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, data)
    print(f"vadsrijk_u_specs: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
