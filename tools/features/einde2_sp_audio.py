"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.9), slice sp_audio: the music of the two places, the three songs (own melodies and lyrics), tunes and stingers, licences.
Java: nl.juiced.guhs.feature.einde2.spaudio (E2SpAudioFeature). Chunk c118_e2_sp_audio.

STUB written by the skelet (einde2-sponsguh-skelet). This module is yours: replace what you need, keep build(h) and ftb(fq).
"""

# the stand-in lyrics (own words, drafts): scene id -> lines; SpAudio.lied puts them into a scene as run-on captions
# (keys scene.guhs.<scene id>.lied.<n>, routed to this slice's chunk). Rewrite them with the real songs.
LIEDEREN = {
    "einde2_sp_d1_recept_zoete_zege": [
        "Een knabbel alleen is maar een kruimel in de zee,",
        "maar knabbel met je vrienden en de hele stad doet mee!",
        "Laat de bellen maar knallen, zet de lampen maar aan,",
        "vanavond, Bikinibodem, zijn we samen niet te verslaan!",
    ],
    "einde2_sp_d2_finale_guhfy_guhber": [
        "Ze zeggen: je bent nog te klein, je bent maar een kind,",
        "maar ik draag een ijshoedje en ik dans tegen de wind!",
        "Ik ben een Guhfy Guhber, ik ben een Guhfy Guhber,",
        "en wie een emmer op heeft: zet hem af en zing mee!",
    ],
    "einde2_sp_d2_weg_monsters": [
        "Diep in de loopgraaf, waar het licht nooit komt,",
        "zingen wij monsters, en niemand die bromt.",
        "Wij lijken eng, met tanden zo lang,",
        "maar wie met ons meezingt, is nergens meer bang!",
    ],
}


def build(h):
    for sid, regels in LIEDEREN.items():
        for i, regel in enumerate(regels, 1):
            h.lang(f"scene.guhs.{sid}.lied.{i}", regel, regel)


def ftb(fq):
    pass
