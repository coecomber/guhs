"""
biomes3 slice "kompas" (Java: feature/bio/kompas; English: tools/lang/en/c85_bio_kompas.json; contract: CONTRACT_BIO.md).

The Superkompas tab Biomes: every biome of the Guhmensie, the Barbecuether and the Guheinde, per dimension; a dimension's
section opens once the player has been there, and the compass points at the nearest spot of the biome you click. No
registry content: this module only writes the texts.

  gui.guhs.superkompas.biomes (+ .tooltip)        the tab
  gui.guhs.superkompas.sectie.<id> (+ .slot)      a section's name and what its padlock says on hover (how to get there,
                                                  friendly, no spoilers); <id> = a section of feature/bio/BiomeLijst.java
  gui.guhs.biokompas.*                            the rest of the tab and what the compass says while it looks for a biome

Another update that adds a SECTION (BiomeLijst.sectie) writes those two section texts in its own module; a biome only
needs its name biome.guhs.<id>. The self-check reads the sections of BiomeLijst.java, so no section shows a raw key
(the names and icons of the biomes are checked by BioKompasGameTests, on the finished lang file).
"""
import os
import re

from features import bio_lib as lib

JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "bio")

TEKSTEN = {
    "gui.guhs.superkompas.biomes": "Biomes",
    "gui.guhs.superkompas.biomes.tooltip": "Alle biomes, per dimensie. Kies er een en je superkompas wijst naar het dichtstbijzijnde!",
    # the sections, in menu order
    "gui.guhs.superkompas.sectie.guhmension": "Guhmensie",
    "gui.guhs.superkompas.sectie.guhmension.slot": "Hier ben je nog nooit geweest. Stap eens door een guhportaal de Guhmensie in, dan gaat dit "
                                                   "slotje vanzelf open. Vahoeg!",
    "gui.guhs.superkompas.sectie.barbecuether": "Barbecuether",
    "gui.guhs.superkompas.sectie.barbecuether.slot": "Hier ben je nog nooit geweest. Zoek in de Guhmensie een barbecueput en stap door het "
                                                     "barbecueportaal. Het is er wel heet, njeg!",
    "gui.guhs.superkompas.sectie.guheinde": "Guheinde",
    "gui.guhs.superkompas.sectie.guheinde.slot": "Hier ben je nog nooit geweest. Diep onder de Guhmensie ligt een Knabbelkelder, en daar staat "
                                                 "een portaal. Durf jij? Njeg!",
    "gui.guhs.superkompas.sectie.echte_guheinde": "Het echte Guheinde",
    "gui.guhs.superkompas.sectie.echte_guheinde.slot": "Hier is nog niemand geweest... njeg?",
    # the tab
    "gui.guhs.biokompas.op_slot": "Op slot",
    "gui.guhs.biokompas.telling": "%s van de %s biomes bezocht",
    "gui.guhs.biokompas.open": "Klik om open te klappen",
    "gui.guhs.biokompas.dicht": "Klik om dicht te klappen",
    "gui.guhs.biokompas.geweest": "Hier ben je geweest, vahoeg!",
    "gui.guhs.biokompas.niet_geweest": "Hier ben je nog niet geweest, njeg...",
    "gui.guhs.biokompas.tip_elders": "Dit biome ligt in: %s. Daar wijst je superkompas de weg!",
    # what the compass says while you hold it
    "gui.guhs.biokompas.zoeken": "%s: zoeken... njeg, even geduld!",
    "gui.guhs.biokompas.afstand": "%s: ongeveer %s blokken ver",
    "gui.guhs.biokompas.hier": "Je bent er: %s. Vahoeg!",
    "gui.guhs.biokompas.niets": "%s: niet gevonden in de buurt, njeg. Loop een eind verder, dan zoek ik opnieuw!",
    "gui.guhs.biokompas.elders": "%s ligt ergens anders: %s. Daar wijst je superkompas de weg!",
}


def build(h):
    lib.teksten(h, TEKSTEN)
    selfcheck(h)


def _java(naam):
    pad = os.path.join(JAVA, naam)
    return open(pad, encoding="utf-8").read() if os.path.exists(pad) else None


def selfcheck(h):
    problems = []
    lijst = _java("BiomeLijst.java")
    if lijst is not None:
        secties = re.findall(r'new Sectie\("([a-z_]+)"', lijst)
        if secties[:4] != ["guhmension", "barbecuether", "guheinde", "echte_guheinde"]:
            problems.append(f"the sections of BiomeLijst.java are not in the design's order: {secties}")
        for s in secties:
            for key in (f"gui.guhs.superkompas.sectie.{s}", f"gui.guhs.superkompas.sectie.{s}.slot"):
                if key not in h.NL:
                    problems.append(f"no text {key}")
    # the teaser tells nothing: its hover is the one mysterious line
    if h.NL.get("gui.guhs.superkompas.sectie.echte_guheinde.slot") != "Hier is nog niemand geweest... njeg?":
        problems.append("the teaser's hover text changed")
    if problems:
        raise SystemExit("bio_kompas self-check failed:\n  " + "\n  ".join(problems))
