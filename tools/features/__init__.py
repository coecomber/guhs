"""
The 2.4 features (minigames and rare structures), one module each. Every module may define:
  build(h)            make its resources: h is make_v2 (w, lang, save, structure, Structure, mc, ... all of it)
  ftb(fq)             add FTB quests: fq is make_ftbquests (q, item, adv, structure, ...)
  BONES               extra guh model bones for clothes (make_guh_variants format)
  clothes(rng, v)     textures of its guh clothes: {clothes id: {swatch: painter}} (v is make_guh_variants)
  variants(rng, v)    extra guh variant textures: {id: (fur colour or None, {swatch: painter})} (like make_guh_variants.variants)
  CLOTHES             ids of its guh clothes (item models; same ids as the GuhClothes enum, lower case)
  icons(ic)           item icons of its guh clothes: {clothes id: image} (ic is make_clothes_icons)
"""
import importlib

FEATURES = ['beauty', 'race', 'meppen', 'disco', 'golf', 'smul', 'vissen', 'eilanden', 'kaasmijn', 'bibliotheek', 'evenementen', 'emotes', 'onderwater', 'guheinde', 'gatenkaas', 'kaasmoeras', 'vadswoud', 'barbecuether', 'spiesburcht', 'diepzee',
            # 2.8 (Knuffeldal): phase 1, then the phase-2 features (append only: the 2000+i variant seeds of the older modules must not change)
            'knuffeldal', 'bakkerij', 'creche', 'theehuis', 'kapper', 'boerderij', 'tuintjes', 'sterrenwacht', 'ballon', 'kamperen', 'knuffelbad',
            'wereldleven',
            # 2.8.1 (Piep)
            'piep',
            # 2.9 (De Grote Guhspelen): phase 1 (spelen), then the phase-2 slices (append only, see above)
            'spelen', 'kleding', 'gids', 'klassiekers', 'sjoelen', 'doolhof', 'katapult', 'knabbelspelen', 'guhpolder', 'elftocht',
            'circuit', 'beroepen',
            # 2.10 (Lieve vadsjes van elkaar)
            'band', 'huisje', 'klusjes', 'speelgoed', 'guhkamer', 'samen', 'favorietjes',
            # 3.0 (Guhverhalen): the fundament, then the ten slices (append only, see above)
            'verhaal', 'timmerguh', 'balto', 'balto_slee', 'mewtwo', 'hemel', 'guhwaii', 'guhwaii_spellen', 'vogels',
            'waterdiertjes', 'landdiertjes',
            # 3.0.x: the Guhdex tab Verhalen
            'gids_verhalen',
            # 1.1.2: the Guhmension placement rebalance (guaranteed minigames/landmarks, story tag): after everything else
            'plaatsing',
            # 1.2.0: the texts of the NL/EN switch and of what the server used to resolve (lang only)
            'taal',
            # 1.2.0: the Wilde-guhweerder
            'weerder',
            # 1.2.5: the Guhoven (bakes on guh power)
            'guhoven',
            # 1.2.8: het Bleekwoud
            'bleekwoud',
            # bbq2: the foundations and the slices (F0 "skelet" owns this list: CONTRACT_130 5.1; here only what F1 needs)
            'vadskracht']


def modules():
    return [importlib.import_module(f"features.{name}") for name in FEATURES]
