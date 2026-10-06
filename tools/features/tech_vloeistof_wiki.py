"""
Wiki texts of the sauce machines (bbq2, tech-vloeistof). Not a feature module (not in FEATURES): the docs step of the merge
reads WIKI and turns it into pages (CONTRACT_130 2.4). The numbers come from SausGetallen.java (tech_vloeistof.getal).
The six blocks get their own block pages from the game data by themselves; this is the page that explains how they work together.
"""
from features import tech_vloeistof as t

_per_emmer = 1000 // (t.getal("POMP_PER_TIK") * 20)

WIKI = {
    "systemen": {
        "saus": ("Saus door slangen", "sauspomp",
                 "Met een Sauspomp, Sausslangen en een Sausvat laat je kaassaus, kaasfrituursaus, water en melk vanzelf naar je "
                 "machines lopen. De Brouwautomaat, de Frituurautomaat en de Grillkoolpers doen er de rest mee, op vadskracht.",
                 ["systemen/vadskracht"]),
    },
    "tekst": [
        ("systemen/saus", "Pompen",
         "Zet een Sauspomp bovenop een bron: een blok kaassaus, kaasfrituursaus of water dat niet stroomt. Geef hem "
         f"{t.getal('POMP')} vadskracht en hij slurpt een emmer per {_per_emmer} tellen omhoog. De bron raakt nooit op: een guhpomp "
         "is beleefd en drinkt geen meer leeg. Is zijn tankje vol en neemt niemand het af, dan kijkt hij verbaasd."),
        ("systemen/saus", "Slangen leggen",
         "Een Sausslang sluit vanzelf aan op de slangen ernaast en op alles waar saus in kan: een pomp, een vat of een machine. "
         f"Hij mag alle kanten op, ook omhoog en omlaag, en hooguit {t.getal('SLANG_MAX')} slangen lang. In de slang "
         "zelf blijft niks staan. Een pomp duwt zijn saus naar alles wat aan de slang hangt en verdeelt eerlijk. Een machine slurpt "
         "zelf uit de vaten en pompen waar hij aan vast zit, maar alleen de saus die hij nodig heeft: over één slang mogen dus "
         "gerust twee sauzen lopen. Een Sausslang heeft geen vadskracht nodig."),
        ("systemen/saus", "Het Sausvat",
         f"In een Sausvat past {t.emmers(t.getal('VAT'))} emmers van één saus tegelijk. Door het glaasje in de zijkant zie je welke "
         "saus het is en hoeveel. Tap er met een lege emmer uit, of giet er een volle emmer in. Melk komt niet uit een bron: die "
         "giet je er met een emmer in. Slaapt het vat, dan is het leeg; kijkt het verbaasd, dan zit het tot de rand vol. Pak je het "
         "vat op, dan gaat de saus gewoon mee."),
        ("systemen/saus", "De Brouwautomaat",
         "De Guhbrouwketel die zelf roert. Hij wil een emmer kaassaus (door een slang of uit een emmer), één ingrediënt en "
         f"{t.getal('BROUW_FLESJES')} glazen flesjes, en maakt daar {t.getal('BROUW_FLESJES')} Guhdrankjes van. Grillspiespoeder is "
         f"niet nodig: het vuur is vadskracht ({t.getal('BROUWAUTOMAAT')}). Hij begint pas als alles er is én de drankjes erbij "
         "passen. De gewone Guhbrouwketel blijft werken zoals altijd."),
        ("systemen/saus", "De Frituurautomaat",
         f"De frituurpan die zelf frituurt, in kaasfrituursaus en op {t.getal('FRITUURAUTOMAAT')} vadskracht. Kaasknabbels worden "
         f"gefrituurde kaasknabbels, een guhvis wordt een gebakken guhvis. Een emmer saus is goed voor {1000 // t.getal('FRITUUR_SAUS')} "
         "stuks. De saus blijft netjes binnen: niemand brandt zich. De gewone frituurpan met Mika's vet blijft werken."),
        ("systemen/saus", "De Grillkoolpers",
         f"Twee blokken hoog. Hij perst op {t.getal('GRILLKOOLPERS')} vadskracht een emmer kaasfrituursaus en een emmer water samen "
         "tot één blok grillkool: hetzelfde wat er in de Guhbarbecuether gebeurt als water de saus raakt, maar dan zonder verbrande "
         "pootjes. Terwijl hij perst zie je de stempel omlaag komen."),
        ("systemen/saus", "Met de hand",
         "Geen van de machines heeft een scherm. Klik met een ingrediënt, flesjes of knabbels om ze erin te doen, met een emmer om "
         "saus bij te gieten of eruit te scheppen, en met een lege hand om te pakken wat klaar is. Is er niks klaar, dan lees je "
         "boven je balk hoe het ervoor staat; sluipend pak je de ingrediënten weer terug. Kijk je naar een machine, dan lees je "
         "onder je vizier wat er in de tanks zit en waar hij op wacht. Buizen, trechters en klusguhs kunnen er ook bij."),
    ],
}
