"""
1.10.9 "Guhwinter", slice verhaal: the three-chapter winter story. Java: feature/guhwinter/verhaal.

  build(h)   the texts of the story (the three Verhaallijnen "guhwinter_sint/kerst/oudjaar" with their steps and objective
             lines, the camera scenes scene.guhs.guhwinter_*, the talks guhwinter.verhaal.praat.*, the kerstwandeling's lines,
             the chat lines, the prikbord screens), the hidden step advancements quest/guhwinter_*_stap_<i>, and the three deco
             blocks you keep (Pakjesstapel, Kerststukje, Oliebollenkraam): textures, models, blockstates, item definitions, loot,
             and their wiki "Zo werkt het" (tools/wiki_site/blokuitleg.py, between markers).
             Dutch in nl_nl, English in tools/lang/en/c510_guhwinter_verhaal.json (written here too).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/guhwinter_verhaal.py
"""
import json
import os
import random
import re

from PIL import Image

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
D = os.path.join(R, "data", "guhs")
EN_FILE = os.path.join("tools", "lang", "en", "c510_guhwinter_verhaal.json")
V = "guhwinter.verhaal."
P = V + "praat."
W = V + "wandel."
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}

# ---------------------------------------------------------------------------------------------------------------------
# texts: key -> (Dutch, English)
# ---------------------------------------------------------------------------------------------------------------------
TEKSTEN = {
    # --- the blocks you keep ---
    "block.guhs.guhwinter_pakjesstapel": ("Pakjesstapel", "Pile of Presents"),
    "block.guhs.guhwinter_kerststukje": ("Kerststukje", "Christmas Centrepiece"),
    "block.guhs.guhwinter_oliebollenkraam": ("Oliebollenkraampje", "Little Oliebollen Stall"),

    # --- general ---
    V + "verteller": ("Verteller", "Narrator"),
    V + "bezig": ("Je bent nog met iets anders bezig, vadsje.", "You're still busy with something else, vadsje."),
    V + "open.sint": ("Er is een stoomboot aangekomen op het Winterplein... Het winterverhaal begint! (Hoofdstuk 1: Verdwaalde Sint)",
                      "A steamboat has arrived at the Winter Square... The winter story begins! (Chapter 1: The Lost Sint)"),
    V + "open.kerst": ("Hoofdstuk 2 van het winterverhaal is open: Kerst bij Papa & Mama! Joost en Niels zoeken nog een kerstboom.",
                       "Chapter 2 of the winter story is open: Christmas at Mum & Dad's! Joost and Niels still need a Christmas tree."),
    V + "open.oudjaar": ("Hoofdstuk 3 van het winterverhaal is open: Oud & Nieuw! Lin heeft iets te zeggen over oliebollen.",
                         "Chapter 3 of the winter story is open: New Year's Eve! Lin has something to say about oliebollen."),
    V + "hint.spoor": ("Volg het wortelspoor bij het portaal: Ozovads kan niet ver zijn!", "Follow the carrot trail by the portal: Ozovads can't be far!"),
    V + "hint.pakjes": ("Ozovads is terug! Zoek nu %s glinsterende pakjes die overboord zijn geslagen. Ze glinsteren alleen voor jou.",
                        "Ozovads is back! Now find %s glittering presents that went overboard. They only glitter for you."),
    V + "pakje": ("Pakje gevonden! %s/%s", "Present found! %s/%s"),
    V + "hint.terug": ("Alle pakjes gevonden! Breng ze terug naar de Pakjesboot aan de kade.", "All presents found! Bring them back to the Pakjesboot at the quay."),
    V + "hint.schoen": ("Je hebt een wortel voor Ozovads. Zet je schoentje bij de schoorsteen in het huis van Krijn en Corine!",
                        "You've got a carrot for Ozovads. Put your shoe by the chimney in Krijn and Corine's house!"),
    V + "hint.boek": ("Het Grote Boek ligt naast de stoel van de Sint. Schrijf je in voor de lootjes (als je wilt)!",
                      "Het Grote Boek lies next to the Sint's chair. Sign up for the Secret Sint draw (if you like)!"),
    V + "hint.pakjesavond": ("Het is pakjesavond! Iedereen is bij Krijn en Corine. Kom snel naar het Winterplein!",
                             "It's pakjesavond! Everyone is at Krijn and Corine's. Hurry to the Winter Square!"),
    V + "hint.sparren": ("Op naar de sparretjes buiten het plein, bij de rotsjes in het dal!", "Off to the little spruces outside the square, by the rocks in the valley!"),
    V + "bal": ("Je hangt een glimmende roze bal in de boom. Hij past er precies naast het ringdoosje van Suus.",
                "You hang a shiny pink bauble in the tree. It fits right next to Suus' ring box."),
    V + "schoen.gezet": ("Je zet je schoentje bij de schoorsteen, met een wortel erin voor Ozovads. Nu een nachtje slapen...",
                         "You put your shoe by the chimney, with a carrot in it for Ozovads. Now sleep on it..."),
    V + "schoen.suiker": ("Je zet je schoentje bij de schoorsteen. Geen wortel? Dan maar een suikerklontje voor Ozovads. Nu een nachtje slapen...",
                          "You put your shoe by the chimney. No carrot? A sugar cube for Ozovads, then. Now sleep on it..."),
    V + "schoen.nacht": ("(Kom morgen terug: na een nachtje heeft de Sint er misschien iets in gestopt.)",
                         "(Come back tomorrow: after a night the Sint may have put something in it.)"),
    V + "schoen.wacht": ("Je schoentje staat er nog. De wortel ook. Nog even geduld, vadsje!", "Your shoe is still there. So is the carrot. A little more patience, vadsje!"),
    V + "schoen.vol": ("Je schoentje is gevuld! De wortel is op, en er ligt een %s en %s pepernoten in. Dankjewel Sint!",
                       "Your shoe has been filled! The carrot is gone, and there's a %s and %s pepernoten in it. Thank you, Sint!"),
    V + "schoen.vol_terug": ("De Sint is al terug naar Spanje, maar in de schoorsteen lag nog iets voor jou: een %s en %s pepernoten!",
                             "The Sint has already gone back to Spain, but there was still something in the chimney for you: a %s and %s pepernoten!"),
    V + "klaar.sint": ("Hoofdstuk 1 klaar! Voor de Sint-helper: een %s en %s pepernoten. Vahoeg!",
                       "Chapter 1 done! For the Sint's helper: a %s and %s pepernoten. Vahoeg!"),
    V + "klaar.kerst": ("Hoofdstuk 2 klaar! Van Papa Krijn en Mama Corine: een %s en %s pepernoten. Fijne kerst!",
                        "Chapter 2 done! From Papa Krijn and Mama Corine: a %s and %s pepernoten. Merry Christmas!"),
    V + "klaar.oudjaar": ("Hoofdstuk 3 klaar! Je eigen %s en %s pepernoten. Gelukkig nieuwjaar, vahoeg!",
                          "Chapter 3 done! Your very own %s and %s pepernoten. Happy New Year, vahoeg!"),
    # the Superkompas ("Mijn verhaal")
    V + "doel.plein": ("het Winterplein", "the Winter Square"),
    V + "doel.spoor": ("het wortelspoor", "the carrot trail"),
    V + "doel.pakjes": ("de verloren pakjes", "the lost presents"),
    V + "doel.boot": ("de Pakjesboot", "the Pakjesboot"),
    V + "doel.schoen": ("de schoorsteen", "the chimney"),
    V + "doel.joost": ("Joostguh", "Joostguh"),
    V + "doel.huis": ("het huis van Krijn en Corine", "Krijn and Corine's house"),
    V + "doel.sparren": ("de sparretjes in het dal", "the little spruces in the valley"),
    V + "doel.boom": ("de kerstboom", "the Christmas tree"),
    V + "doel.vijver": ("de vijver", "the pond"),
    V + "doel.tafel": ("de lange tafel", "the long table"),
    V + "doel.lin": ("Linguh", "Linguh"),
    V + "doel.prikbord": ("het prikbord", "the notice board"),

    # --- the prikbord ---
    V + "prikbord.titel": ("Prikbord · Goede voornemens", "Notice Board · New Year's Resolutions"),
    V + "prikbord.leeg": ("Nog geen kaartjes! Het hele bord is nog leeg. Net als het agenda van Joost.",
                          "No cards yet! The whole board is still empty. Just like Joost's diary."),
    V + "prikbord.schrijf": ("Schrijf jouw voornemen", "Write your resolution"),
    V + "prikbord.aanpassen": ("Mijn kaartje aanpassen", "Change my card"),
    V + "prikbord.sluit": ("Sluiten", "Close"),
    V + "kaartje.titel": ("Mijn goede voornemen", "My New Year's resolution"),
    V + "kaartje.uitleg": ("Wat ga jij dit jaar doen? Het komt op een kaartje op het prikbord, voor iedereen om te lezen.",
                           "What are you going to do this year? It goes on a card on the notice board, for everyone to read."),
    V + "kaartje.iedereen": ("Iedereen kan het lezen!", "Everyone can read it!"),
    V + "kaartje.ophangen": ("Ophangen", "Pin it up"),
    V + "kaartje.terug": ("Terug", "Back"),
    V + "kaartje.hangt": ("Je kaartje hangt op het prikbord! Iedereen kan het lezen. Vahoeg!", "Your card is on the notice board! Everyone can read it. Vahoeg!"),
    V + "kaartje.nieuw": ("Je kaartje is aangepast.", "Your card has been changed."),
    V + "kaartje.leeg": ("Een leeg kaartje? Dat is wel héél makkelijk vol te houden. Schrijf er iets op!",
                         "An empty card? That's a very easy one to keep. Write something on it!"),
    V + "kaartje.niet": ("Je kunt nu geen kaartje ophangen. Eerst horen wat de zes van plan zijn (Oud & Nieuw)!",
                         "You can't pin up a card right now. First hear what the six are planning (New Year's Eve)!"),
}

# ---------------------------------------------------------------------------------------------------------------------
# the talks (Praat): guhwinter.verhaal.praat.<key>
# ---------------------------------------------------------------------------------------------------------------------
PRAAT = {
    # pakjesavond missed (after 6 Dec)
    "kort.0": ("Hé, jij hebt pakjesavond gemist! Het was zó gezellig. Er werd hard op de deur gebonkt en er stond een hele zak vol pakjes.",
               "Hey, you missed pakjesavond! It was so cosy. Someone banged on the door and there was a whole sack full of presents."),
    "kort.1": ("En ik had een gedicht voor Joost. Van de Sint. Zeker niet van mij. Wil je het horen?",
               "And I had a poem for Joost. From the Sint. Definitely not from me. Want to hear it?"),
    "kort.2": ("'Geen boksring, geen sleutelring, geen uienring, maar eentje met een steentje. Je weet wel: dat ding.'",
               "'Not a boxing ring, not a key ring, not an onion ring, but one with a little stone. You know: that thing.'"),
    "kort.3": ("Mooi hè? Het rijmde allemaal op 'ring'. Ik snap nog steeds niet waarom. Misschien is dat een Sint-ding?",
               "Lovely, right? It all rhymed on 'ring'. I still don't get why. Maybe that's a Sint thing?"),
    "kort.4": ("...Ja Joost. Een Sint-ding.", "...Yes Joost. A Sint thing."),
    # chapter 2: picking the tree
    "boom.0": ("Buiten het plein, tussen de rotsjes, staan een paar sparretjes in de sneeuw.", "Outside the square, between the rocks, a few little spruces stand in the snow."),
    "boom.1": ("Daar zijn ze! Welke vind jij het mooist, vadsje?", "There they are! Which one do you like best, vadsje?"),
    "boom.2": ("Ik heb ze al opgemeten. Geen van allen past in de trein. Ik zeg het maar even.",
               "I've already measured them. None of them fits in the train. Just saying."),
    "boom.klein": ("Die kleine schattige!", "That cute little one!"),
    "boom.groot": ("Die grote dikke!", "That big fat one!"),
    "boom.klein.0": ("Goede keus. Hij past precies niet in de trein, maar wel bijna.", "Good choice. It just doesn't fit in the train, but almost."),
    "boom.groot.0": ("Ambitieus. Daar heb je minstens een goederentrein voor nodig. Respect.", "Ambitious. You'd need a freight train at least. Respect."),
    "boom.3": ("Tillen maar! Eén, twee, drie... VAHOEG!", "Lift! One, two, three... VAHOEG!"),
    "boom.4": ("Met z'n drieën sjouwen jullie de boom naar het huis van Krijn en Corine. Hij staat nu in de kamer. Tijd om te versieren!",
               "The three of you haul the tree to Krijn and Corine's house. It's standing in the living room now. Time to decorate!"),
    # chapter 2: decorating
    "versieren.0": ("Wat staat hij mooi, lieverds! Nu nog versieren.", "Doesn't it look lovely, darlings! Now to decorate it."),
    "versieren.1": ("Kijk Joost, ik hang er een bal in. Hij is een beetje... vierkant. Met fluweel. En hij kan open. Er past precies iets kleins in.",
                    "Look Joost, I'm hanging up a bauble. It's a bit... square. With velvet. And it opens. Something small would fit in it perfectly."),
    "versieren.2": ("Haha, een vierkante bal! Daar kun je mooi een pepernoot in bewaren.", "Haha, a square bauble! You could keep a pepernoot in there."),
    "versieren.3": ("...Een pepernoot. Ja. Precies wat ik dacht.", "...A pepernoot. Yes. Exactly what I was thinking."),
    "versieren.4": ("Hang jij jouw tekening er ook in? Na kerst krijgt iedereen alle tekeningen in het Kerstalbum!",
                    "Will you hang your drawing in it too? After Christmas everyone gets all the drawings in the Christmas Album!"),
    "versieren.tekening": ("Ik maak een tekening voor in de boom", "I'll make a drawing for the tree"),
    "versieren.bal": ("Ik hang gewoon een bal op", "I'll just hang up a bauble"),
    # chapter 2: opa
    "opa.0": ("Lieve vadsjes, opa is er! Dit is opa Jochum!", "Dear vadsjes, grandpa's here! This is grandpa Jochum!"),
    "opa.1": ("...Wijnand.", "...Wijnand."),
    "opa.2": ("Ja ja. Wil je koffie, opa Jochum?", "Yes, yes. Would you like some coffee, grandpa Jochum?"),
    "opa.3": ("Corine, schat. Hij heet Wijnand. Het is mijn vader.", "Corine, dear. His name is Wijnand. He's my father."),
    "opa.4": ("Ik noem hem al veertig jaar Jochum. Dat gaan we nu niet meer veranderen, hoor.",
              "I've called him Jochum for forty years. We're not changing that now, dear."),
    "opa.5": ("Ik heet al tachtig jaar Wijnand. Dat gaan we óók niet meer veranderen.", "I've been called Wijnand for eighty years. We're not changing that either."),
    "opa.wijnand": ("Hoi opa Wijnand!", "Hi grandpa Wijnand!"),
    "opa.jochum": ("Hoi opa Jochum!", "Hi grandpa Jochum!"),
    "opa.wijnand.0": ("Hoi opa Wijnand! Fijne kerst!", "Hi grandpa Wijnand! Merry Christmas!"),
    "opa.wijnand.1": ("Eindelijk. Iemand met verstand. Jij krijgt een extra kerstkransje. Twee zelfs.",
                      "Finally. Someone with some sense. You get an extra Christmas cookie. Two, even."),
    "opa.jochum.0": ("Hoi opa Jochum! Fijne kerst!", "Hi grandpa Jochum! Merry Christmas!"),
    "opa.jochum.1": ("...Wijnand. Maar goed. Je bent lief, dus je krijgt tóch twee kerstkransjes.", "...Wijnand. But fine. You're sweet, so you get two Christmas cookies anyway."),
    "opa.6": ("Kom, opa Jochum, ga lekker bij de haard zitten.", "Come on, grandpa Jochum, have a nice seat by the fire."),
    "opa.7": ("...Wijnand. (Hij gaat toch lekker zitten. Het is een hele fijne stoel.)", "...Wijnand. (He sits down anyway. It's a very nice chair.)"),
    # chapter 3: Lin and the airfryer
    "lin.0": ("Oliebollen bakken! Het hele huis gaat naar frituurvet ruiken. Heerlijk.", "Oliebollen baking! The whole house will smell of frying fat. Lovely."),
    "lin.1": ("Ik bak niks in de frituur. Nooit meer. Niet na... het churro-incident.", "I'm not deep-frying anything. Never again. Not after... the churro incident."),
    "lin.2": ("Ze praat er nog steeds niet over. Er was een kermis. Er waren churros. Meer mag ik niet zeggen.",
              "She still won't talk about it. There was a funfair. There were churros. That's all I'm allowed to say."),
    "lin.3": ("Er was vet. Overal vet. Ik zie nog steeds churros als ik mijn ogen dichtdoe.", "There was fat. Fat everywhere. I still see churros when I close my eyes."),
    "lin.4": ("Dus ik doe de mijne in de airfryer. Gezond, veilig, geen spetters.", "So I'm doing mine in the airfryer. Healthy, safe, no splatters."),
    "lin.5": ("Oliebollen uit een airfryer? Dat zijn toch gewoon... warme broodjes?", "Oliebollen from an airfryer? Aren't those just... warm bread rolls?"),
    "lin.6": ("Een airfryer is geen echte frituur. Net zoals een luchtballon geen echt OV is.", "An airfryer isn't a real fryer. Just like a hot-air balloon isn't real public transport."),
    "lin.pan": ("Ik bak ze in de pan, zoals oma Marrie", "I'll bake them in the pan, like grandma Marrie"),
    "lin.airfryer": ("Ik bak met Lin in de airfryer", "I'll bake with Lin in the airfryer"),
    "lin.nog": ("Toch even geen zin? Geeft niks. Het beslag wacht wel op je.", "Not in the mood after all? No problem. The batter will wait for you."),
    "proef.pan": ("Jouw oliebollen uit de pan zijn klaar. Score: %s!", "Your oliebollen from the pan are done. Score: %s!"),
    "proef.airfryer": ("Jouw oliebollen uit de airfryer zijn klaar. Score: %s!", "Your oliebollen from the airfryer are done. Score: %s!"),
    "proef.top": ("Dit zijn de mooiste oliebollen die ik ooit heb gezien. Ik ga een beetje huilen. Van geluk. En van de poedersuiker in mijn oog.",
                  "These are the most beautiful oliebollen I have ever seen. I'm going to cry a little. From happiness. And from the powdered sugar in my eye."),
    "proef.goed": ("Rond, bruin, lekker. Precies goed! Oma Marrie zou trots zijn.", "Round, brown, tasty. Just right! Grandma Marrie would be proud."),
    "proef.raar": ("Ze zijn... uniek. Er is er niet één hetzelfde. Eentje lijkt op een tram.", "They're... unique. Not a single one is the same. One of them looks like a tram."),
    "proef.vulkaan": ("Is dit een oliebol of een vulkaan? ...Ik eet hem toch op. Njeg.", "Is this an oliebol or a volcano? ...I'm eating it anyway. Njeg."),
    "proef.0": ("Oké, nu die van Lin. Uit de airfryer. Ik verwacht er niks van. Wacht... WACHT.", "Okay, now Lin's. From the airfryer. I'm not expecting much. Wait... WAIT."),
    "proef.1": ("Hoe kan dat nou?! Die van Lin zijn het lekkerst! Luchtig, knapperig, en geen druppel vet!",
                "How is that possible?! Lin's are the tastiest! Fluffy, crispy, and not a drop of fat!"),
    "proef.2": ("Geen vet. Geen spetters. Geen churros. Alleen liefde.", "No fat. No splatters. No churros. Only love."),
    "proef.3": ("Mijn lieve Lin is een genie. Dat zeg ik al jaren.", "My sweet Lin is a genius. I've been saying it for years."),
    "proef.4": ("Ik moet toegeven: de airfryer is wél een echte frituur. De luchtballon blijft geen echt OV.",
                "I have to admit: the airfryer IS a real fryer. The hot-air balloon is still not real public transport."),
    # chapter 3: goede voornemens
    "voornemen.0": ("Bijna nieuwjaar! Tijd voor goede voornemens. Wie begint?", "Almost New Year! Time for resolutions. Who's first?"),
    "voornemen.suus": ("Ik. Dit jaar krijg ik... duidelijkheid. (Ze kijkt heel lang naar Joost.)", "Me. This year I'm getting... clarity. (She looks at Joost for a very long time.)"),
    "voornemen.joost": ("Ik ga dit jaar eindelijk leren jongleren! Met oliebollen!", "This year I'm finally going to learn to juggle! With oliebollen!"),
    "voornemen.niels": ("Meer OV. Elke week een nieuwe treinlijn. En een abonnement op de nachtbus. Gewoon omdat het kan.",
                        "More public transport. A new train line every week. And a night-bus pass. Just because I can."),
    "voornemen.evi": ("En wij gaan weer naar Japan!", "And we're going to Japan again!"),
    "voornemen.niels2": ("Met de trein.", "By train."),
    "voornemen.evi2": ("Met het vliegtuig, Niels. ...Oké, met de trein naar het vliegveld.", "By plane, Niels. ...Okay, by train to the airport."),
    "voornemen.lin": ("Niks gefrituurds meer. Ook geen frietjes. ...Behalve misschien op zaterdag. Uit de airfryer.",
                      "No more fried things. No chips either. ...Except maybe on Saturdays. From the airfryer."),
    "voornemen.liek": ("Ik ga elke dag iemand een compliment geven. Lin, je haar zit leuk vandaag.", "I'm going to give someone a compliment every day. Lin, your hair looks nice today."),
    "voornemen.lin2": ("Dank je, lief. Dat zei je gisteren ook.", "Thanks, love. You said that yesterday too."),
    "voornemen.suus2": ("Joost... jouw voornemen. Gaat dat toevallig ook ergens over een knie? En een vraag?",
                        "Joost... your resolution. Does it happen to involve a knee? And a question?"),
    "voornemen.joost2": ("Een knie? Nee joh, jongleren doe je met je handen, Suus. Met oliebollen.", "A knee? No, you juggle with your hands, Suus. With oliebollen."),
    "voornemen.suus3": ("...Natuurlijk. Met oliebollen.", "...Of course. With oliebollen."),
    "voornemen.kaartje": ("En jij? Schrijf jouw voornemen op een kaartje en prik het op het prikbord! Dan kan iedereen het lezen.",
                          "And you? Write your resolution on a card and pin it on the notice board! Then everyone can read it."),
}
for _k, _v in PRAAT.items():
    TEKSTEN[P + _k] = _v

# ---------------------------------------------------------------------------------------------------------------------
# the kerstwandeling (chat lines of the real guhs): guhwinter.verhaal.wandel.<key>
# ---------------------------------------------------------------------------------------------------------------------
WANDEL = {
    "begin": ("De kerstwandeling begint! Loop mee met de zes rond de vijver.", "The Christmas walk begins! Walk along with the six around the pond."),
    "mee": ("Je loopt mee met de kerstwandeling!", "You join the Christmas walk!"),
    "niemand": ("De guhs zijn er nu niet. Probeer het zo nog eens.", "The guhs aren't here right now. Try again in a bit."),
    "gestopt": ("De kerstwandeling is gestopt: iedereen is weggelopen. Klik op een van de zes om opnieuw te beginnen.",
                "The Christmas walk stopped: everyone wandered off. Click one of the six to start again."),
    "klaar": ("Wat een wandeling! Opa komt zo, ga even langs bij Mama Corine in huis.", "What a walk! Grandpa is coming soon, drop by Mama Corine in the house."),
    "start.0": ("Kerstwandeling! Iedereen jas aan, sjaal om, vahoeg!", "Christmas walk! Coats on, scarves on, vahoeg!"),
    "start.1": ("Gezellig, samen wandelen. Weet je wat ook gezellig is, Joost? Samen... dingen beloven. Voor altijd.",
                "Cosy, walking together. You know what else is cosy, Joost? Promising... things. Together. Forever."),
    "start.2": ("Ja! Ik beloof dat ik deze keer bijhou!", "Yes! I promise I'll keep up this time!"),
    "vijver": ("Wist je dat hier geen trein rijdt? Zonde. Hier zou prima een sprinter kunnen.", "Did you know no train runs here? Shame. A sprinter would fit here just fine."),
    "koud": ("Mijn neus is een ijsje.", "My nose is an ice lolly."),
    "noway_water": ("NOWAY EEN WATERHOEN!????", "NOWAY A MOORHEN!????"),
    "foto.0": ("WAIT. Is that a CHICKEN? In the WATER?? *klik klik klik klik*", "WAIT. Is that a CHICKEN? In the WATER?? *click click click click*"),
    "water.0": ("Kijk die lange teentjes! Net een balletdanseres!", "Look at those long toes! Just like a ballerina!"),
    "water.1": ("Mijn hart. Mijn hart kan dit niet aan.", "My heart. My heart can't handle this."),
    "water.2": ("Ik wilde dit al zó lang zien. Langer dan ik op de Noord/Zuidlijn heb gewacht.", "I've wanted to see this for so long. Longer than I waited for the metro line."),
    "hint.0": ("Joost, als je ooit iets belangrijks wilt vragen... een wandeling is een héél goed moment.",
               "Joost, if you ever want to ask something important... a walk is a really good moment."),
    "hint.1": ("Oh ja! Suus... hoe laat is het eigenlijk?", "Oh yes! Suus... what time is it, actually?"),
    "hint.2": ("...Kwart over teleurgesteld.", "...Quarter past disappointed."),
    "noway_alpine": ("NOWAYYY EEN ALPINE HOENTJE?!?!??!?!", "NOWAYYY AN ALPINE CHICK?!?!??!?!"),
    "foto.1": ("I have four hundred photos now. They are ALL blurry. Best day of my life.", "I have four hundred photos now. They are ALL blurry. Best day of my life."),
    "alpine.0": ("Een ALPINE hoentje? Hier is niet eens een ber... oh. Daar is een berg.", "An ALPINE chick? There isn't even a mount... oh. There's a mountain."),
    "alpine.1": ("Ik ga nooit meer slapen. Dit was alles.", "I'm never sleeping again. This was everything."),
    "alpine.2": ("Zelfs in Japan hadden ze dit niet!", "Even in Japan they didn't have this!"),
    "alpine.3": ("Anthony, mag ik die foto's? Voor op de koelkast. Alle vierhonderd.", "Anthony, can I have those photos? For the fridge. All four hundred."),
    "einde.0": ("Wat een wandeling! Kom, we gaan opwarmen met warme chocomel.", "What a walk! Come on, let's warm up with hot chocomel."),
    "einde.1": ("Twee bijzondere vogels gezien vandaag. En nog steeds geen ring. Maar wel twee vogels.",
                "Saw two special birds today. Still no ring. But two birds."),
}
for _k, _v in WANDEL.items():
    TEKSTEN[W + _k] = _v

# ---------------------------------------------------------------------------------------------------------------------
# the camera scenes: id -> (title, {key: (nl, en)}, {actor: (nl, en)})
# ---------------------------------------------------------------------------------------------------------------------
N = {  # the speakers' names (as the plein's NPCs)
    "sint": ("Sinterklaas", "Sinterklaas"), "ozovads": ("Ozovads", "Ozovads"),
    "pakjesguh_0": ("Pakjesguh Pepernootje", "Parcel Guh Pepernootje"), "pakjesguh_1": ("Pakjesguh Kruimel", "Parcel Guh Kruimel"),
    "joost": ("Joostguh", "Joostguh"), "suus": ("Suusguh", "Suusguh"), "liek": ("Liekguh", "Liekguh"), "lin": ("Linguh", "Linguh"),
    "evi": ("Eviguh", "Eviguh"), "niels": ("Nielsguh", "Nielsguh"), "krijn": ("Papa Krijnguh", "Papa Krijnguh"),
    "corine": ("Mama Corineguh", "Mama Corineguh"), "marrie": ("Oma Marrieguh", "Oma Marrieguh"), "wijnand": ("Opa Wijnand", "Opa Wijnand"),
    "rins": ("Rinsguh", "Rinsguh"), "anthony": ("Anthonyguh", "Anthonyguh"), "daan": ("Daanguh", "Daanguh"), "kim": ("Kimguh", "Kimguh"),
    "finn": ("Finnguh", "Finnguh"), "liv": ("Livguh", "Livguh"), "allen": ("Iedereen", "Everyone"),
}

TERUG = ("Een paar weken geleden, op dit plein... (een herinnering)", "A few weeks ago, on this very square... (a memory)")

INTOCHT = {
    "intro": ("Het Winterplein, diep in de Guhmension. Uit de mist tuft een roze stoomboot de haven in. Een beetje te hard.",
              "The Winter Square, deep in the Guhmension. A pink steamboat chugs out of the mist into the harbour. A little too fast."),
    "plons": ("BONK. Plons... plons... plons.", "BONK. Splash... splash... splash."),
    "welkom": ("Welkom, lieve kinderen van GORINGUHM!", "Welcome, dear children of GORINGUHM!"),
    "guhmension": ("...dit is de Guhmension, Sint.", "...this is the Guhmension, Sint."),
    "kaart": ("De Guhmension? Maar ik had de kaart toch goed? Bij Slot Loevestijn links... of was het nou rechts?",
              "The Guhmension? But I had the map right, didn't I? Left at Slot Loevestijn... or was it right?"),
    "goringuhm": ("Twee jaar geleden kwam ik nog keurig aan in Goringuhm. Met dezelfde boot en dezelfde kaart! Nou ja. Bijna dezelfde kaart.",
                  "Two years ago I arrived in Goringuhm right on time. Same boat, same map! Well. Almost the same map."),
    "bishop": ("Why is the bishop on a boat?? Is this normal?", "Why is the bishop on a boat?? Is this normal?"),
    "spanje": ("Schat, dat is Sinterklaas. Hij komt elk jaar met de stoomboot uit Spanje.", "Honey, that's Sinterklaas. He comes from Spain by steamboat every year."),
    "boot": ("From SPAIN? By BOAT? With a HORSE?", "From SPAIN? By BOAT? With a HORSE?"),
    "trein": ("Had gewoon de trein genomen. Een stoomboot is geen echt OV.", "Should've just taken the train. A steamboat isn't real public transport."),
    "geentrein": ("Niels, er gaat geen trein naar Spanje.", "Niels, there's no train to Spain."),
    "nog": ("...Nóg niet.", "...Not YET."),
    "ozovads": ("Ozovads! Nee, niet doen! OZOVADS! ...Hij is weg. Richting het portaal.", "Ozovads! No, don't! OZOVADS! ...He's gone. Towards the portal."),
    "pakjes": ("Mijn Ozovads! En... waar zijn de pakjes? Oh nee, die zijn overboord geslagen bij die bonk!",
               "My Ozovads! And... where are the presents? Oh no, they went overboard with that bump!"),
    "helpen": ("Jij helpt de Sint toch wel even, vadsje? Volg het wortelspoor bij het portaal. Ozovads kan nooit ver zijn met al die wortels.",
               "You'll help the Sint, won't you, vadsje? Follow the carrot trail by the portal. Ozovads can't have gone far with all those carrots."),
}
OZOVADS = {
    "spoor": ("Het wortelspoor houdt op bij een paar rotsjes en twee sparretjes. Daar steekt een roze dekentje uit!",
              "The carrot trail ends at a few rocks and two little spruces. A pink blanket is sticking out!"),
    "psst": ("Psst... Ozovads? Ik heb een wortel voor je.", "Psst... Ozovads? I've got a carrot for you."),
    "hinnik": ("Hinnik! (Dat betekent: sorry, vadsje. Die boot schommelde zo. En ik zag een wortel liggen.)",
               "Neigh! (That means: sorry, vadsje. That boat was rocking so much. And I saw a carrot.)"),
    "samen": ("Ozovads knabbelt de wortel op en loopt braaf met je mee terug naar het plein.", "Ozovads munches the carrot and trots back to the square with you, good as gold."),
}
SINTBLIJFT = {
    "held": ("Ozovads! En de pakjes! Lieve help, wat ben jij een held, vadsje.", "Ozovads! And the presents! Goodness me, what a hero you are, vadsje."),
    "tel": ("Ik tel ze even... één, twee, vijf, vahoeg... ze zijn er allemaal!", "Let me count them... one, two, five, vahoeg... they're all here!"),
    "kaart": ("Maar mijn kaart is nat, de boot is moe en ik weet echt niet meer waar Spanje ligt.",
              "But my map is soaked, the boat is tired and I really don't know where Spain is any more."),
    "blijf": ("Blijf dan gewoon hier, Sint! Wij hebben sneeuw, chocomel en een heel plein!", "Then just stay here, Sint! We've got snow, chocomel and a whole square!"),
    "hier": ("Dan vieren we het dit jaar hier! VAHOEG!", "Then this year we'll celebrate it here! VAHOEG!"),
    "boek": ("Schrijf je naam in Het Grote Boek, naast mijn stoel. Op 2 december trek ik de lootjes!",
             "Write your name in Het Grote Boek, next to my chair. On 2 December I'll draw the names!"),
    "schoen": ("En wie zijn schoentje zet bij de schoorsteen van Krijn en Corine... wie weet wat erin ligt? Hier, een wortel voor Ozovads.",
               "And whoever puts their shoe by Krijn and Corine's chimney... who knows what they'll find? Here, a carrot for Ozovads."),
    "lijstje": ("Sint, mag ik ook iets op mijn verlanglijstje zetten? Iets kleins. Rond. Glimmend. Voor aan mijn vinger.",
                "Sint, can I put something on my wish list too? Something small. Round. Shiny. For my finger."),
    "suus": ("Ho ho... daar ga ik niet over, lieve Suus. (De Sint kijkt héél nadrukkelijk naar Joost.)",
             "Ho ho... that's not up to me, dear Suus. (The Sint looks very pointedly at Joost.)"),
    "donut": ("Een donut! Suus wil een donut, Sint! Met glazuur!", "A doughnut! Suus wants a doughnut, Sint! With icing!"),
    "zucht": ("...Ja Joost. Een donut.", "...Yes Joost. A doughnut."),
}
PAKJESAVOND = {
    "avond": ("Pakjesavond. Iedereen zit gezellig bij Krijn en Corine. Dan...", "Pakjesavond. Everyone is sitting cosily at Krijn and Corine's. Then..."),
    "bonk": ("BONK! BONK! BONK!", "BONK! BONK! BONK!"),
    "deur": ("Daar is hij! Krijn, kijk eens gauw bij de deur!", "There he is! Krijn, quick, go and look at the door!"),
    "zak": ("Een hele zak vol pakjes! En een briefje: 'Groetjes van de Sint. PS: Ozovads zegt hinnik.'",
            "A whole sack of presents! And a note: 'Love from the Sint. PS: Ozovads says neigh.'"),
    "uitpakken": ("Er wordt uitgepakt, gelachen en gesmuld van pepernoten. Dan staat Suus op, met een velletje papier.",
                  "There's unwrapping, laughing and munching on pepernoten. Then Suus stands up, holding a sheet of paper."),
    "gedicht": ("Joost, ik heb een gedicht voor je. Van de Sint. Zeker niet van mij.", "Joost, I have a poem for you. From the Sint. Definitely not from me."),
    "rijm1": ("'De Sint zag Joost op het plein, die lieveling, en dacht: wat mist die jongen nog? Een soort van... ring.'",
              "'The Sint saw Joost on the square, that darling, and thought: what is that boy still missing? Some kind of... ring.'"),
    "rijm2": ("'Geen boksring, geen sleutelring, geen uienring, maar eentje met een steentje. Je weet wel: dat ding.'",
              "'Not a boxing ring, not a key ring, not an onion ring, but one with a little stone. You know: that thing.'"),
    "rijm3": ("'Ga op één knie, dat is heus een simpel ding, en vraag het dan maar gauw, met een glimmende... ring.'",
              "'Go down on one knee, it's really a simple thing, and then ask it quickly, with a shiny... ring.'"),
    "rijm4": ("'Groetjes van de Sint. En zeker niet van Suus. (Ring.)'", "'Love from the Sint. And definitely not from Suus. (Ring.)'"),
    "snap": ("Wauw, wat rijmt dat lekker! Maar waarom rijmt alles op 'ring'? Is dat een Sint-ding?",
             "Wow, that rhymes so nicely! But why does everything rhyme on 'ring'? Is that a Sint thing?"),
    "toeval": ("...Puur toeval, Joost.", "...Pure coincidence, Joost."),
    "uienring": ("Ik heb trouwens wel zin in uienringen. Wie nog meer?", "I really fancy some onion rings now, by the way. Anyone else?"),
    "santa": ("So Sinterklaas is like Santa, but with a horse, a boat, and poems about rings?", "So Sinterklaas is like Santa, but with a horse, a boat, and poems about rings?"),
    "suus": ("Alleen bij Suus, Anthony. Alleen bij Suus.", "Only with Suus, Anthony. Only with Suus."),
}
BOOMHALEN = {
    "boom": ("Njeg! We hebben nog geen kerstboom! Mama Corine rekent op ons drieën.", "Njeg! We still don't have a Christmas tree! Mama Corine is counting on the three of us."),
    "plan": ("Ik heb al een route. De 14:07 naar Guhmension-Dal, overstappen op de sprinter, laatste stukje met de buurtbus.",
             "I've already got a route. The 14:07 to Guhmension Valley, change to the sprinter, last bit by local bus."),
    "meter": ("Niels. Het is een boom. Van twee meter.", "Niels. It's a tree. Two metres tall."),
    "fiets": ("Dan kopen we voor de boom een dagkaart fiets.", "Then we buy the tree a bicycle day ticket."),
    "past": ("Past niet.", "Won't fit."),
    "bakfiets": ("Bakfiets?", "Cargo bike?"),
    "ballon": ("Luchtballon? ...Nee, laat maar. Een luchtballon is geen echt OV.", "Hot-air balloon? ...No, never mind. A hot-air balloon isn't real public transport."),
    "dragen": ("Zullen we hem gewoon... dragen?", "Shall we just... carry it?"),
    "stil": ("...", "..."),
    "briljant": ("...Dat is zo simpel dat het briljant is. Vahoeg!", "...That's so simple it's brilliant. Vahoeg!"),
    "plek": ("Er staan mooie sparretjes buiten het plein, bij de rotsjes in het dal. Kom!", "There are lovely little spruces outside the square, by the rocks in the valley. Come on!"),
}
KERSTDINER = {
    "gourmet": ("Aan tafel, lieve vadsjes! Het gourmetstel staat aan!", "To the table, dear vadsjes! The gourmet set is on!"),
    "zelf": ("So... you cook your own dinner. At the table. With tiny pans. On Christmas.", "So... you cook your own dinner. At the table. With tiny pans. On Christmas."),
    "gezellig": ("Ja. Dat is nou gezellig.", "Yes. That's what makes it cosy."),
    "pannetje": ("Schat, pak gewoon een pannetje. Je mag ook een spiesje.", "Honey, just grab a little pan. You can have a skewer too."),
    "chef": ("Wait. I made a tiny burger. A TINY BURGER. I am a chef now.", "Wait. I made a tiny burger. A TINY BURGER. I am a chef now."),
    "gefrituurd": ("Er wordt toch niks gefrituurd, hè? ...Oké. Dan doe ik mee.", "Nothing's being deep-fried, right? ...Okay. Then I'm in."),
    "optijd": ("Mijn pannetje is altijd precies op tijd klaar. Net als de trein. Meestal.", "My little pan is always done right on time. Just like the train. Usually."),
    "toetje": ("Joost... weet je wat ik nog nooit bij het toetje heb gehad? Iets kleins. Glimmends. In een champagneglas.",
               "Joost... you know what I've never had with dessert? Something small. Shiny. In a champagne glass."),
    "nogeen": ("Wil je nog een gourmetpannetje?", "Would you like another gourmet pan?"),
    "ja": ("...Ja. Graag, Joost.", "...Yes. Please, Joost."),
    "opa": ("Nog wat stokbrood, opa Jochum?", "Some more baguette, grandpa Jochum?"),
    "naam": ("...Wijnand.", "...Wijnand."),
    "deur": ("Er wordt aangebeld. Daan, Kim, Finn en Liv wippen even binnen!", "The doorbell rings. Daan, Kim, Finn and Liv pop in for a moment!"),
    "jochum": ("OPA JOCHUM!!", "GRANDPA JOCHUM!!"),
    "finn": ("Ach... voor jou mag het, Finn.", "Oh... for you that's fine, Finn."),
    "kaas": ("Ik heb een pannetje gepakt! Met kaas! En nog meer kaas!", "I took a little pan! With cheese! And more cheese!"),
    "even": ("We blijven maar even hoor, de kleintjes moeten zo naar bed.", "We're only staying a moment, the little ones have to go to bed soon."),
    "kerst": ("Fijne kerst allemaal!", "Merry Christmas, everyone!"),
    "vahoeg": ("Fijne kerst! VAHOEG!", "Merry Christmas! VAHOEG!"),
}

# Java scene ids -> (title, lines, speakers); line keys must match feature/guhwinter/verhaal/WinterScenes.java
SCENES = {}


def _scene(id, titel, regels, sprekers, terug=False):
    r = dict(regels)
    if terug:
        r["terug"] = TERUG
    SCENES[id] = (titel, r, {s: N[s] for s in sprekers})


_INTOCHT_SPREKERS = ["sint", "allen", "anthony", "rins", "niels", "evi", "pakjesguh_0", "suus", "ozovads", "joost", "liek", "lin", "pakjesguh_1"]
_INTOCHT_REGELS = INTOCHT


def _alle_scenes():
    intocht = dict(_INTOCHT_REGELS)
    _scene("guhwinter_intocht", ("De intocht", "The arrival"), intocht, _INTOCHT_SPREKERS)
    _scene("guhwinter_intocht_terug", ("De intocht (herinnering)", "The arrival (a memory)"), intocht, _INTOCHT_SPREKERS, terug=True)
    _scene("guhwinter_ozovads", ("Ozovads gevonden", "Ozovads found"), OZOVADS, ["ozovads"])
    sb = ["sint", "pakjesguh_0", "pakjesguh_1", "joost", "suus", "evi", "niels", "liek", "lin", "ozovads"]
    _scene("guhwinter_sintblijft", ("Dan vieren we het hier!", "Then we'll celebrate here!"), SINTBLIJFT, sb)
    _scene("guhwinter_sintblijft_terug", ("Dan vieren we het hier! (herinnering)", "Then we'll celebrate here! (a memory)"), SINTBLIJFT, sb, terug=True)
    _scene("guhwinter_pakjesavond", ("Pakjesavond", "Pakjesavond"), PAKJESAVOND,
           ["marrie", "niels", "evi", "liek", "lin", "rins", "anthony", "suus", "joost", "krijn", "corine"])
    _scene("guhwinter_boomhalen", ("Een boom halen", "Fetching a tree"), BOOMHALEN, ["joost", "niels"])
    _scene("guhwinter_kerstdiner", ("Het kerstdiner", "Christmas dinner"), KERSTDINER,
           ["wijnand", "marrie", "niels", "evi", "lin", "liek", "corine", "krijn", "suus", "joost", "rins", "anthony", "daan", "kim", "finn", "liv", "allen"])


_alle_scenes()

# ---------------------------------------------------------------------------------------------------------------------
# the three Verhaallijnen (gui.guhs.verhalen.<id>.*): (name, explanation, [(step name, now, where)], extra sleutels, short lines)
# ---------------------------------------------------------------------------------------------------------------------
WACHT_K = ("Dit hoofdstuk begint op 6 december", "Kom vanaf 6 december terug naar het Winterplein")
WACHT_K_EN = ("This chapter starts on 6 December", "Come back to the Winter Square from 6 December")
WACHT_O = ("Dit hoofdstuk begint op 26 december", "Kom vanaf 26 december terug naar het Winterplein")
WACHT_O_EN = ("This chapter starts on 26 December", "Come back to the Winter Square from 26 December")
VOORBIJ = ("Het Winterplein is weer leeg. Tot volgend jaar!", "Het Winterplein (voorbij)")
VOORBIJ_EN = ("The Winter Square is empty again. See you next year!", "The Winter Square (over)")

LIJNEN = {
    "guhwinter_sint": dict(
        naam=("Verdwaalde Sint", "The Lost Sint"),
        uitleg=("Sinterklaas nam bij Slot Loevestijn de verkeerde afslag en meerde aan in de Guhmension. Zijn paard is weg, zijn pakjes "
                "zijn overboord geslagen... Help de Sint, en vier Sinterklaas op het Winterplein!",
                "Sinterklaas took the wrong turn at Slot Loevestijn and moored in the Guhmension. His horse is gone, his presents went "
                "overboard... Help the Sint, and celebrate Sinterklaas on the Winter Square!"),
        stappen=[
            (("De intocht", "Ga naar het Winterplein", "Het Winterplein in de Guhmension (Reisguh: Winterplein)"),
             ("The arrival", "Go to the Winter Square", "The Winter Square in the Guhmension (Reisguh: Winterplein)")),
            (("Ozovads is weg!", "Volg het wortelspoor naar Ozovads", "Bij het portaal, het dal in"),
             ("Ozovads is gone!", "Follow the carrot trail to Ozovads", "By the portal, down into the valley")),
            (("Pakjes overboord", "Zoek %s glinsterende pakjes" % 5, "Overal op het plein en bij de aankomst"),
             ("Presents overboard", "Find %s glittering presents" % 5, "All over the square and by the arrival")),
            (("Terug naar de boot", "Breng de pakjes naar de Pakjesboot", "De kade bij de Pakjesboot"),
             ("Back to the boat", "Bring the presents to the Pakjesboot", "The quay by the Pakjesboot")),
            (("Het schoentje", "Zet je schoentje bij de schoorsteen", "De haard in het huis van Krijn en Corine"),
             ("The shoe", "Put your shoe by the chimney", "The fireplace in Krijn and Corine's house")),
            (("Pakjesavond", "Vier pakjesavond op het plein", "Het huis van Krijn en Corine, 5 december vanaf 19:00"),
             ("Pakjesavond", "Celebrate pakjesavond on the square", "Krijn and Corine's house, 5 December from 19:00")),
        ],
        extra={
            "5_wacht": (("Pakjesavond is op 5 december om 19:00", "Kom dan naar het plein (en zet vast je schoentje)"),
                        ("Pakjesavond is on 5 December at 19:00", "Come to the square then (and put out your shoe)")),
            "5_kort": (("Vraag Joost en Suus hoe pakjesavond was", "Joostguh en Suusguh op het Winterplein"),
                       ("Ask Joost and Suus how pakjesavond went", "Joostguh and Suusguh on the Winter Square")),
            "wacht": (("Het winterfeest begint binnenkort", "Het Winterplein"), ("The winter party starts soon", "The Winter Square")),
            "voorbij": (VOORBIJ, VOORBIJ_EN),
        },
        klaar=(("Klaar! De Sint viert het hier. Vahoeg!", "Het Winterplein"), ("Done! The Sint is celebrating here. Vahoeg!", "The Winter Square")),
        kort={"0": ("Ga naar het Winterplein", "Go to the Winter Square"), "1": ("Volg het wortelspoor", "Follow the carrot trail"),
              "2": ("Zoek de glinsterende pakjes", "Find the glittering presents"), "3": ("Breng de pakjes naar de boot", "Bring the presents to the boat"),
              "4": ("Zet je schoentje bij de schoorsteen", "Put your shoe by the chimney"), "5": ("Pakjesavond op het plein!", "Pakjesavond on the square!"),
              "5_wacht": ("Pakjesavond: 5 december, 19:00", "Pakjesavond: 5 December, 19:00"), "5_kort": ("Vraag Joost of Suus naar pakjesavond", "Ask Joost or Suus about pakjesavond")},
        nodig={"pakjes": ("Verloren pakjes", "Lost presents")},
    ),
    "guhwinter_kerst": dict(
        naam=("Kerst bij Papa & Mama", "Christmas at Mum & Dad's"),
        uitleg=("Kerst bij Papa Krijn en Mama Corine: een boom halen (niet met de trein), versieren, een kerstwandeling met verrassend "
                "veel hoentjes, opa 'Jochum', en gourmetten aan de lange tafel.",
                "Christmas at Papa Krijn and Mama Corine's: fetching a tree (not by train), decorating, a Christmas walk with surprisingly "
                "many birds, grandpa 'Jochum', and gourmet dinner at the long table."),
        stappen=[
            (("Boom halen", "Praat met Joost of Niels over de kerstboom", "Op het Winterplein"),
             ("Fetching a tree", "Talk to Joost or Niels about the Christmas tree", "On the Winter Square")),
            (("De sparretjes", "Kies een boom bij de sparretjes", "Buiten het plein, bij de rotsjes in het dal"),
             ("The little spruces", "Pick a tree at the little spruces", "Outside the square, by the rocks in the valley")),
            (("Versieren", "Versier de kerstboom", "De kerstboom in het huis van Krijn en Corine"),
             ("Decorating", "Decorate the Christmas tree", "The Christmas tree in Krijn and Corine's house")),
            (("Kerstwandeling", "Maak een kerstwandeling met de zes", "Rond de vijver (klik op een van de zes)"),
             ("Christmas walk", "Go for a Christmas walk with the six", "Around the pond (click one of the six)")),
            (("Opa komt", "Zeg hoi tegen opa", "Bij de haard in het huis van Krijn en Corine"),
             ("Grandpa's coming", "Say hi to grandpa", "By the fireplace in Krijn and Corine's house")),
            (("Kerstdiner", "Schuif aan voor het kerstdiner", "De lange tafel in het huis van Krijn en Corine"),
             ("Christmas dinner", "Join the Christmas dinner", "The long table in Krijn and Corine's house")),
        ],
        extra={"wacht": (WACHT_K, WACHT_K_EN), "voorbij": (VOORBIJ, VOORBIJ_EN)},
        klaar=(("Klaar! Fijne kerst, vahoeg!", "Het huis van Krijn en Corine"), ("Done! Merry Christmas, vahoeg!", "Krijn and Corine's house")),
        kort={"0": ("Praat met Joost of Niels", "Talk to Joost or Niels"), "1": ("Kies een boom bij de sparretjes", "Pick a tree at the spruces"),
              "2": ("Versier de kerstboom", "Decorate the Christmas tree"), "3": ("Kerstwandeling: klik op een van de zes", "Christmas walk: click one of the six"),
              "4": ("Zeg hoi tegen opa", "Say hi to grandpa"), "5": ("Schuif aan voor het kerstdiner", "Join the Christmas dinner"),
              "wacht": ("Hoofdstuk 2: vanaf 6 december", "Chapter 2: from 6 December")},
    ),
    "guhwinter_oudjaar": dict(
        naam=("Oud & Nieuw", "New Year's Eve"),
        uitleg=("Oliebollen bakken (Lin doet het in de airfryer, na het churro-incident) en goede voornemens: die van de zes, en die van jou "
                "op het prikbord.",
                "Baking oliebollen (Lin uses the airfryer, after the churro incident) and New Year's resolutions: the six's, and yours on "
                "the notice board."),
        stappen=[
            (("Oliebollen", "Bak oliebollen met Lin en Liek", "Linguh en Liekguh op het Winterplein"),
             ("Oliebollen", "Bake oliebollen with Lin and Liek", "Linguh and Liekguh on the Winter Square")),
            (("Goede voornemens", "Hoor de voornemens en hang jouw kaartje op", "Een van de zes, en het prikbord bij het huis"),
             ("Resolutions", "Hear the resolutions and pin up your card", "One of the six, and the notice board by the house")),
        ],
        extra={"wacht": (WACHT_O, WACHT_O_EN), "voorbij": (VOORBIJ, VOORBIJ_EN)},
        klaar=(("Klaar! Gelukkig nieuwjaar!", "Het prikbord bij het huis"), ("Done! Happy New Year!", "The notice board by the house")),
        kort={"0": ("Bak oliebollen met Lin", "Bake oliebollen with Lin"), "1": ("Hang je voornemen op het prikbord", "Pin your resolution on the board"),
              "wacht": ("Hoofdstuk 3: vanaf 26 december", "Chapter 3: from 26 December")},
    ),
}

GROEP_KOP = ("Guhwinter", "Guhwinter")


def _lijn_teksten():
    out = {"gui.guhs.verhalen.kop.guhwinter": GROEP_KOP}
    for id, l in LIJNEN.items():
        b = f"gui.guhs.verhalen.{id}"
        out[b + ".naam"] = l["naam"]
        out[b + ".uitleg"] = l["uitleg"]
        sleutels = {}
        for i, (nl, en) in enumerate(l["stappen"]):
            out[f"{b}.stap.{i}"] = (nl[0], en[0])
            sleutels[str(i)] = ((nl[1], en[1]), (nl[2], en[2]))
        (knl, ken) = l["klaar"]
        sleutels["klaar"] = ((knl[0], ken[0]), (knl[1], ken[1]))
        for s, (nl, en) in l["extra"].items():
            sleutels[s] = ((nl[0], en[0]), (nl[1], en[1]))
        for s, (nu, waar) in sleutels.items():
            out[f"{b}.nu.{s}"] = nu
            out[f"{b}.waar.{s}"] = waar
        for s, v in l.get("kort", {}).items():
            if s not in sleutels:
                raise SystemExit(f"guhwinter_verhaal: kort for an unknown sleutel {id} {s}")
            out[f"{b}.kort.{s}"] = v
        for k, v in l.get("nodig", {}).items():
            out[f"{b}.nodig.{k}"] = v
    return out


def _scene_teksten():
    out = {}
    for id, (titel, regels, sprekers) in SCENES.items():
        out[f"scene.guhs.{id}.titel"] = titel
        for k, v in regels.items():
            out[f"scene.guhs.{id}.{k}"] = v
        for a, v in sprekers.items():
            out[f"scene.guhs.{id}.naam.{a}"] = v
    return out


TEKSTEN.update(_lijn_teksten())
TEKSTEN.update(_scene_teksten())


def engels(t):
    """The English guh words of tools/lang/GLOSSARY.md (nyeg, wahoog, chonky) and the event's English names."""
    for nl, en in (("Het Grote Boek lies", "The Big Book lies"), ("Het Grote Boek", "the Big Book"), ("Winter Square", "Winterplein"),
                   ("vadsjes", "chonkies"), ("vadsje", "chonky"), ("VAHOEG", "WAHOOG"), ("Vahoeg", "Wahoog"), ("vahoeg", "wahoog"),
                   ("Njeg", "Nyeg"), ("njeg", "nyeg")):
        t = t.replace(nl, en)
    return t


TEKSTEN = {k: (nl, engels(en)) for k, (nl, en) in TEKSTEN.items()}
# the block items' names (mc26.lang_block_items copies them in a full run; here for the standalone run)
for _k in [k for k in TEKSTEN if k.startswith("block.guhs.") and k.count(".") == 2]:
    TEKSTEN["item.guhs." + _k[len("block.guhs."):]] = TEKSTEN[_k]

# ---------------------------------------------------------------------------------------------------------------------
# wiki "Zo werkt het" (blokuitleg.py, between markers)
# ---------------------------------------------------------------------------------------------------------------------
UITLEG = {
    "guhwinter_pakjesstapel": (
        "Een stapeltje ingepakte cadeautjes met gouden strikjes. Je krijgt het als je hoofdstuk 1 van het winterverhaal "
        "(\"Verdwaalde Sint\") uitspeelt op het Winterplein. Zet het neer waar je wilt: onder de boom, in de hal, op de tafel. Het "
        "smelt niet: je houdt het voor altijd. (Niet openmaken, ze zijn van karton.)",
        "A little pile of wrapped presents with golden bows. You get it when you finish chapter 1 of the winter story (\"The Lost "
        "Sint\") on the Winter Square. Put it wherever you like: under the tree, in the hall, on the table. It never melts: you keep it "
        "forever. (Don't open them, they're cardboard.)"),
    "guhwinter_kerststukje": (
        "Een echt Hollands kerststukje: een rode kaars tussen dennengroen, met een rood lint en dennenappels. Je krijgt het als je "
        "hoofdstuk 2 van het winterverhaal (\"Kerst bij Papa & Mama\") uitspeelt.\n\nRechtsklik erop om de kaars aan te steken of "
        "uit te blazen. Brandt hij, dan geeft hij een beetje licht. Het stukje smelt niet: je houdt het voor altijd.",
        "A proper Dutch Christmas centrepiece: a red candle among pine greens, with a red ribbon and pinecones. You get it when you "
        "finish chapter 2 of the winter story (\"Christmas at Mum & Dad's\").\n\nRight-click it to light the candle or blow it out. "
        "When it burns it gives a little light. The centrepiece never melts: you keep it forever."),
    "guhwinter_oliebollenkraam": (
        "Een klein oliebollenkraampje met een gestreept dakje en een schaal vol oliebollen met poedersuiker. Je krijgt het als je "
        "hoofdstuk 3 van het winterverhaal (\"Oud & Nieuw\") uitspeelt. Het is versiering: de oliebollen zijn om naar te kijken (Lin "
        "vraagt zich nog steeds af of ze gefrituurd zijn). Het smelt niet: je houdt het voor altijd.",
        "A little oliebollen stall with a striped roof and a bowl full of oliebollen with powdered sugar. You get it when you finish "
        "chapter 3 of the winter story (\"New Year's Eve\"). It's decoration: the oliebollen are for looking at (Lin still wonders "
        "whether they're deep-fried). It never melts: you keep it forever."),
}
BLOKUITLEG = os.path.join("tools", "wiki_site", "blokuitleg.py")
_MARK = "# <guhwinter_verhaal> (1.10.9, tools/features/guhwinter_verhaal.py: rewritten on every run)"


def blokuitleg():
    with open(BLOKUITLEG, encoding="utf-8") as f:
        src = f.read()
    src = re.sub(r"\n" + re.escape(_MARK) + r".*?# </guhwinter_verhaal>\n", "\n", src, flags=re.S)
    regels = [_MARK] + [f"UITLEG[{k!r}] = dict(nl={nl!r}, en={engels(en)!r})" for k, (nl, en) in UITLEG.items()] + ["# </guhwinter_verhaal>"]
    if not src.endswith("\n"):
        src += "\n"
    with open(BLOKUITLEG, "w", encoding="utf-8", newline="\n") as f:
        f.write(src + "\n".join(regels) + "\n")


# ---------------------------------------------------------------------------------------------------------------------
# textures (16x16)
# ---------------------------------------------------------------------------------------------------------------------

def leeg():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def vol(c):
    return Image.new("RGBA", (16, 16), c + (255,) if len(c) == 3 else c)


def tint(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def ruis(c, var, seed):
    rng = random.Random(seed)
    img = leeg()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), tint(c, 1 + rng.uniform(-var, var)))
    return img


def pakje(kleur, lint, seed):
    """Wrapping paper with a little dot print and a golden ribbon cross."""
    img = ruis(kleur, 0.05, seed)
    licht = tint(kleur, 1.25)
    for y in range(1, 16, 4):
        for x in range((y // 4) % 2 * 2 + 1, 16, 4):
            img.putpixel((x, y), licht)
    for i in range(16):
        for d in (7, 8):
            img.putpixel((d, i), tint(lint, 1.0 if d == 7 else 0.82))
            img.putpixel((i, d), tint(lint, 1.0 if d == 7 else 0.82))
    for i in range(16):        # a darker edge
        img.putpixel((i, 0), tint(img.getpixel((i, 0)), 0.85))
        img.putpixel((i, 15), tint(img.getpixel((i, 15)), 0.8))
    return img


def strik(c):
    img = ruis(c, 0.08, 7)
    for x in range(16):
        img.putpixel((x, 0), tint(c, 1.3))
    return img


def dennengroen():
    rng = random.Random(21)
    img = vol((46, 104, 58))
    for _ in range(60):
        x, y = rng.randrange(16), rng.randrange(16)
        img.putpixel((x, y), rng.choice([(30, 78, 42, 255), (70, 138, 74, 255), (58, 122, 66, 255), (98, 160, 92, 255)]))
    for _ in range(6):         # a few frosty tips
        img.putpixel((rng.randrange(16), rng.randrange(16)), (226, 240, 236, 255))
    return img


def kaars():
    img = vol((196, 32, 46))
    for y in range(16):
        img.putpixel((3, y), (232, 84, 90, 255))
        img.putpixel((4, y), (220, 60, 70, 255))
        img.putpixel((12, y), (150, 20, 34, 255))
    for x in range(16):
        img.putpixel((x, 0), (240, 120, 120, 255))
    img.putpixel((6, 1), (240, 140, 140, 255))   # a wax drip
    img.putpixel((6, 2), (232, 100, 104, 255))
    return img


def vlam():
    img = vol((255, 210, 80))
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) / 8 + (15 - y) / 30
            img.putpixel((x, y), (255, int(240 - 90 * d), int(150 - 140 * d), 255))
    return img


def lint_rood():
    img = ruis((206, 30, 44), 0.06, 3)
    for x in range(16):
        img.putpixel((x, 1), (240, 90, 96, 255))
        img.putpixel((x, 14), (150, 16, 30, 255))
    return img


def dennenappel():
    img = vol((112, 70, 38))
    for y in range(0, 16, 3):
        for x in range((y // 3) % 2 * 2, 16, 4):
            img.putpixel((x, y), (150, 100, 56, 255))
            img.putpixel((min(15, x + 1), y), (136, 88, 50, 255))
            if y + 1 < 16:
                img.putpixel((x, y + 1), (80, 48, 26, 255))
    return img


def schaal():
    img = ruis((222, 214, 200), 0.04, 11)
    for x in range(16):
        img.putpixel((x, 0), (196, 170, 90, 255))     # a golden rim
        img.putpixel((x, 15), (160, 150, 136, 255))
    return img


def kraamstreep():
    img = leeg()
    for y in range(16):
        for x in range(16):
            rood = (x // 4) % 2 == 0
            img.putpixel((x, y), (214, 46, 58, 255) if rood else (250, 244, 236, 255))
    for x in range(16):
        img.putpixel((x, 15), (150, 30, 40, 255) if (x // 4) % 2 == 0 else (210, 200, 190, 255))
    return img


def kraamhout():
    rng = random.Random(5)
    img = vol((150, 100, 60))
    for y in range(16):
        f = 0.9 if y % 4 == 0 else 1.0
        for x in range(16):
            img.putpixel((x, y), tint((150, 100, 60), f * (1 + rng.uniform(-0.05, 0.05))))
    return img


def oliebol():
    rng = random.Random(13)
    img = leeg()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 / 8
            img.putpixel((x, y), tint((176, 104, 44), 1.15 - 0.35 * d + rng.uniform(-0.05, 0.05)))
    for _ in range(14):         # raisins and powdered sugar
        img.putpixel((rng.randrange(16), rng.randrange(16)), (70, 36, 30, 255))
    for _ in range(40):
        x, y = rng.randrange(16), rng.randrange(8)
        img.putpixel((x, y), (250, 248, 244, 255))
    return img


def kraambord():
    img = vol((250, 244, 230))
    for x in range(16):
        img.putpixel((x, 0), (150, 100, 60, 255))
        img.putpixel((x, 15), (150, 100, 60, 255))
    for y in range(16):
        img.putpixel((0, y), (150, 100, 60, 255))
        img.putpixel((15, y), (150, 100, 60, 255))
    for y in range(4, 12):       # an oliebol drawn on the sign
        for x in range(4, 12):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 13:
                img.putpixel((x, y), (180, 106, 46, 255))
    for x, y in ((6, 5), (8, 5), (9, 6), (5, 6)):
        img.putpixel((x, y), (255, 255, 255, 255))
    return img


# ---------------------------------------------------------------------------------------------------------------------
# models
# ---------------------------------------------------------------------------------------------------------------------

def el(frm, to, faces, rot=None):
    if isinstance(faces, str):
        faces = {f: faces for f in ("north", "south", "east", "west", "up", "down")}
    e = {"from": frm, "to": to, "faces": {f: {"texture": "#" + t} for f, t in faces.items()}}
    if rot:
        e["rotation"] = rot
    return e


def y_rot(angle, origin):
    return {"angle": angle, "axis": "y", "origin": origin}


def pakjesstapel_model():
    bt = lambda n: f"guhs:block/{n}"   # noqa: E731
    els = [
        el([1, 0, 1], [9, 7, 9], "r"),                                     # the big red one
        el([8.5, 0, 7.5], [15, 5, 14], "g"),                               # the green one
        el([10, 5, 9], [14, 8, 13], "b", y_rot(22.5, [12, 6, 11])),        # a little blue one on top of it
        el([2.5, 7, 2.5], [7.5, 11, 7.5], "p", y_rot(-22.5, [5, 9, 5])),   # the pink one on the red one
        el([2, 0, 10], [6, 3, 14], "b"),                                   # a small one in front
        # golden bows
        el([3.4, 11, 4.6], [5.0, 12.4, 5.4], "s", y_rot(-22.5, [5, 11, 5])),
        el([5.0, 11, 4.6], [6.6, 12.4, 5.4], "s", y_rot(-22.5, [5, 11, 5])),
        el([10.8, 5, 10.6], [12.0, 6.2, 11.4], "s"),
        el([12.0, 5, 10.6], [13.2, 6.2, 11.4], "s"),
    ]
    return {"parent": "minecraft:block/block", "textures": {"particle": bt("guhwinter_pakje_rood"), "r": bt("guhwinter_pakje_rood"),
                                                             "g": bt("guhwinter_pakje_groen"), "b": bt("guhwinter_pakje_blauw"),
                                                             "p": bt("guhwinter_pakje_roze"), "s": bt("guhwinter_strik_goud")},
            "elements": els}


def kerststukje_model(aan):
    bt = lambda n: f"guhs:block/{n}"   # noqa: E731
    els = [
        el([2, 0, 3.5], [14, 1.5, 12.5], "s"),                             # the dish
        el([2.5, 1.5, 4], [13.5, 3.5, 12], "d"),                           # the pine bed
        el([1, 1.5, 6], [3, 3, 10], "d", y_rot(22.5, [2, 2, 8])),          # sprigs sticking out
        el([13, 1.5, 6], [15, 3, 10], "d", y_rot(-22.5, [14, 2, 8])),
        el([5, 1.5, 2], [11, 3, 4], "d", y_rot(22.5, [8, 2, 3])),
        el([5, 1.5, 12], [11, 3, 14], "d", y_rot(-22.5, [8, 2, 13])),
        el([4, 3.5, 5], [12, 4.5, 11], "d"),
        el([6.75, 3.5, 6.75], [9.25, 10, 9.25], "k"),                      # the candle
        el([7.85, 10, 7.85], [8.15, 10.6, 8.15], "a"),                     # the wick
        el([3, 3.5, 5], [4.6, 5.4, 6.6], "a", y_rot(22.5, [3.8, 4, 5.8])),  # pinecones
        el([11.4, 3.5, 9.4], [13, 5.4, 11], "a", y_rot(-22.5, [12.2, 4, 10.2])),
        el([10.6, 3.5, 4.6], [12, 5, 6], "a"),
        el([4.4, 3.5, 10.2], [6.6, 5.2, 11.2], "l"),                       # the red ribbon bow and its tails
        el([6.6, 3.5, 10.4], [7.6, 4.8, 11.0], "l"),
        el([3.6, 3.0, 11.2], [4.6, 3.6, 13.4], "l", y_rot(-22.5, [4, 3, 12])),
        el([6.6, 3.0, 11.2], [7.6, 3.6, 13.4], "l", y_rot(22.5, [7, 3, 12])),
    ]
    tex = {"particle": bt("guhwinter_dennengroen"), "s": bt("guhwinter_stukje_schaal"), "d": bt("guhwinter_dennengroen"),
           "k": bt("guhwinter_kaars_rood"), "a": bt("guhwinter_dennenappel"), "l": bt("guhwinter_lint_rood")}
    if aan:
        els.append(el([7.4, 10.4, 7.4], [8.6, 12.2, 8.6], "v"))          # the flame
        tex["v"] = bt("guhwinter_vlam")
    m = {"parent": "minecraft:block/block", "textures": tex, "elements": els}
    if aan:
        m["ambientocclusion"] = False
    return m


def oliebollenkraam_model():
    bt = lambda n: f"guhs:block/{n}"   # noqa: E731
    st, ho, bo, bd = "t", "h", "o", "b"
    els = [
        el([1, 0, 4], [15, 8, 13], {"north": st, "south": ho, "east": st, "west": st, "up": ho, "down": ho}),   # the counter
        el([0.5, 8, 3.5], [15.5, 9, 13.5], ho),                            # its top
        el([1, 9, 11.5], [2, 14.5, 12.5], ho), el([14, 9, 11.5], [15, 14.5, 12.5], ho),   # posts
        el([1, 9, 4], [2, 14, 5], ho), el([14, 9, 4], [15, 14, 5], ho),
        el([0, 14.5, 2], [16, 15.6, 14], st),                              # the striped roof
        el([0, 13, 2], [16, 14.5, 2.6], st),                               # its front valance
        el([3, 9, 5], [9.5, 9.6, 10.5], ho),                               # the tray
        el([3.4, 9.6, 5.4], [5.4, 11.6, 7.4], bo),                         # oliebollen
        el([5.8, 9.6, 5.4], [7.8, 11.6, 7.4], bo),
        el([4.6, 9.6, 7.8], [6.6, 11.6, 9.8], bo),
        el([7.0, 9.6, 7.8], [9.0, 11.6, 9.8], bo),
        el([5.2, 11.4, 6.6], [7.2, 13.2, 8.6], bo),
        el([10.5, 9, 7.5], [14, 12.5, 8.1], {"north": bd, "south": ho, "east": ho, "west": ho, "up": ho, "down": ho}),   # a little sign
        el([12, 9, 7.6], [12.6, 9.6, 8.0], ho),
    ]
    return {"parent": "minecraft:block/block", "textures": {"particle": bt("guhwinter_kraam_streep"), "t": bt("guhwinter_kraam_streep"),
                                                             "h": bt("guhwinter_kraamhout"), "o": bt("guhwinter_oliebol"),
                                                             "b": bt("guhwinter_kraambord")},
            "elements": els}


def facing_states(model, extra=""):
    return {f"facing={f}{extra}": ({"model": model, **({"y": y} if y else {})})
            for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}


def build(h):
    w = h.w
    # --- textures ---
    for naam, img in (("guhwinter_pakje_rood", pakje((200, 36, 48), (246, 200, 70), 1)), ("guhwinter_pakje_groen", pakje((52, 140, 72), (246, 200, 70), 2)),
                      ("guhwinter_pakje_blauw", pakje((70, 130, 210), (250, 250, 250), 3)), ("guhwinter_pakje_roze", pakje((240, 140, 186), (246, 200, 70), 4)),
                      ("guhwinter_strik_goud", strik((236, 186, 60))), ("guhwinter_dennengroen", dennengroen()), ("guhwinter_kaars_rood", kaars()),
                      ("guhwinter_vlam", vlam()), ("guhwinter_lint_rood", lint_rood()), ("guhwinter_dennenappel", dennenappel()),
                      ("guhwinter_stukje_schaal", schaal()), ("guhwinter_kraam_streep", kraamstreep()), ("guhwinter_kraamhout", kraamhout()),
                      ("guhwinter_oliebol", oliebol()), ("guhwinter_kraambord", kraambord())):
        h.save(img, "block", naam + ".png")
    # --- models, blockstates, items, loot ---
    w(os.path.join(A, "models", "block", "guhwinter_pakjesstapel.json"), pakjesstapel_model())
    w(os.path.join(A, "blockstates", "guhwinter_pakjesstapel.json"), {"variants": facing_states("guhs:block/guhwinter_pakjesstapel")})
    w(os.path.join(A, "models", "block", "guhwinter_kerststukje.json"), kerststukje_model(True))
    w(os.path.join(A, "models", "block", "guhwinter_kerststukje_uit.json"), kerststukje_model(False))
    states = {}
    states.update(facing_states("guhs:block/guhwinter_kerststukje", ",lit=true"))
    states.update(facing_states("guhs:block/guhwinter_kerststukje_uit", ",lit=false"))
    w(os.path.join(A, "blockstates", "guhwinter_kerststukje.json"), {"variants": states})
    w(os.path.join(A, "models", "block", "guhwinter_oliebollenkraam.json"), oliebollenkraam_model())
    w(os.path.join(A, "blockstates", "guhwinter_oliebollenkraam.json"), {"variants": facing_states("guhs:block/guhwinter_oliebollenkraam")})
    for naam in ("guhwinter_pakjesstapel", "guhwinter_kerststukje", "guhwinter_oliebollenkraam"):
        w(os.path.join(A, "models", "item", naam + ".json"), {"parent": f"guhs:block/{naam}"})
        w(os.path.join(A, "items", naam + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})
        h.self_drop(naam)
    # --- the hidden step advancements of the three lines (the engine grants quest/<id>_stap_<i>) ---
    for id, l in LIJNEN.items():
        for i in range(1, len(l["stappen"]) + 1):
            w(os.path.join(D, "advancement", "quest", f"{id}_stap_{i}.json"), {"criteria": IMPOSSIBLE})
    # --- texts ---
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    schrijf_en()
    blokuitleg()


def schrijf_en():
    os.makedirs(os.path.dirname(EN_FILE), exist_ok=True)
    with open(EN_FILE, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in sorted(TEKSTEN.items())}, f, indent=2, ensure_ascii=False)
        f.write("\n")


def selfcheck():
    """Every text the Java uses is here: the scene lines and speakers, the talks, the walk, the chat lines."""
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhwinter", "verhaal")
    if not os.path.isdir(java):
        return
    bron = ""
    for n in sorted(os.listdir(java)):
        if n.endswith(".java"):
            with open(os.path.join(java, n), encoding="utf-8") as f:
                bron += f.read()
    mis = []
    for key in re.findall(r'"(guhwinter\.verhaal\.[a-z0-9_.]+[a-z0-9_])"', bron):
        if key not in TEKSTEN and not any(k.startswith(key + ".") for k in TEKSTEN):
            mis.append(key)
    for key in re.findall(r'\b(?:r\(p, "[a-z_0-9]*", |optie\(\d, )"([a-z0-9_.]+)"', bron):
        if P + key not in TEKSTEN:
            mis.append(P + key)
    for key in re.findall(r'w\.(?:zeg\("[a-z]+", |foto\(|noway\()"([a-z0-9_.]+)"', bron):
        if W + key not in TEKSTEN:
            mis.append(W + key)
    for key in re.findall(r'W \+ "([a-z0-9_.]+)"', bron):
        if W + key not in TEKSTEN:
            mis.append(W + key)
    # scene lines: .zeg(t, "who", "key", n) inside each scene method: check against every scene (a key may be in any one of them)
    alle_scene_keys = {k.split(".", 3)[3] for k in TEKSTEN if k.startswith("scene.guhs.guhwinter_")}
    for wie, key in re.findall(r'\.zeg\(\d+, (?:"([a-z_0-9]*)"|Cutscene\.SPELER), "([a-z0-9_.]+)"', bron):
        if key not in alle_scene_keys:
            mis.append("scene line " + key)
    if mis:
        raise SystemExit("guhwinter_verhaal: texts missing for " + ", ".join(sorted(set(mis))))


def _direct():
    import sys
    sys.path.insert(0, "tools")
    import make_v2 as h
    selfcheck()
    build(h)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"guhwinter_verhaal: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
