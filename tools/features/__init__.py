"""
The 2.4 features (minigames and rare structures), one module each. Every module may define:
  build(h)            make its resources: h is make_v2 (w, lang, save, structure, Structure, mc, ... all of it)
  ftb(fq)             add FTB quests: fq is make_ftbquests (q, item, adv, structure, ...)
  BONES               extra guh model bones for clothes (make_guh_variants format)
  clothes(rng, v)     textures of its guh clothes: {clothes id: {swatch: painter}} (v is make_guh_variants)
  variants(rng, v)    extra guh variant textures: {id: (fur colour or None, {swatch: painter})} (like make_guh_variants.variants)
  CLOTHES             ids of its guh clothes (item models; same ids as the GuhClothes enum, lower case)
  icons(ic)           item icons of its guh clothes: {clothes id: image} (ic is make_clothes_icons)
  FTB_CHAPTER, FTB_SECTION, FTB_PORTRAIT, and (bbq2) FTB_SECTIES, FTB_LINEAIR, FTB_SLOT: where its FTB quests land (make_ftbquests.py)
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
            # guhpixel: the kern, then its nine slices (each a stub until its slice fills it in)
            'guhpixel', 'guhpixel_lobby', 'guhpixel_grap1', 'guhpixel_grap2', 'guhpixel_among', 'guhpixel_guhkade', 'guhpixel_kantoor',
            'guhpixel_bioscoop', 'guhpixel_reisbureau', 'guhpixel_parkour',
            # bbq2 (CONTRACT_130 5.1): the skelet and its helper library, the three foundations, then the 27 slices (append only, see above).
            # They stand AFTER guhpixel (released in 1.3.0, before bbq2): the index seeds a module's variant and clothes textures
            # (make_guh_variants.py: 2000 + i, 1000 + i), and textures that are out in the world must not change
            'bbq2', 'vadskracht', 'verhaal_motor', 'wereld', 'bank', 'tech_bronnen', 'tech_buizen', 'tech_machines', 'tech_vloeistof', 'tech_bezorg',
            'tech_klusjes', 'tech_quests', 'paleizen', 'bestaand', 'fossiel_mijn', 'sausdieren', 'camping_markt', 'toren_peper', 'ring', 'ring_h1',
            'ring_h2', 'ring_h3', 'ring_h4', 'ring_h5', 'ring_h6', 'ring_sausuman', 'guhrio', 'guhrio_w1', 'guhrio_w2', 'guhrio_w3',
            'guhrio_beloning',
            # bbq2 phase 3 (append only): the winks at the older stories
            'ring_knipogen',
            # verhalenpad: het Guhpad (the stories open the worlds; lang, hidden advancements, two pictures, FTB lock quests)
            'guhpad',
            'oude_scenes',    # bbq2 verhalenpad: oude-scenes (append only, see above)
            'snuffel',   # verhalenpad: snuffel-kern (Het Snuffeleiland; append only, see above)
            'snuffel_steiger',   # verhalenpad: snuffel-steiger (the dock and the opening; after snuffel)
            'snuffel_dorp',   # verhalenpad: snuffel-dorp (the island and the first series; after 'snuffel': it writes eiland.json again)
            # biomes3 (merged into 1.4.0 after bbq2; append only; bio_wereld wraps the Guhmensie's final_density, so no module that
            # writes the Guhmensie's terrain may come after it): the kern, then its slices, the blocks first (nothing here may
            # depend on its index: variant and clothes painters take their rng from bio_lib.rng)
            'bio', 'bio_blokken_dal', 'bio_blokken_wolk', 'bio_wereld', 'bio_dieren', 'bio_kompas', 'bio_bouw_dal', 'bio_bouw_meer',
            'bio_bouw_wolk1', 'bio_bouw_wolk2', 'bio_systemen',
            # 1.4.2 (append only, see above): the Rookguh-vlucht of the Knabbelring
            'ring_vlucht',
            # 1.4.2: Het Knabbelpark (park-kern, then park-plekjes in the same module; append only)
            'park',
            'park_plek_a',   # <park-a> 1.4.2: five random plekjes of the park (append only)
            'park_plek_b',   # 1.4.2 park-plekjes b (after 'park': it writes the texts and models of its plekjes)
            # einde2 (the Guheinde expansion): what the update shares (the post-pass hook), then its seventeen slices (append only)
            'einde2', 'einde2_biomes', 'einde2_biomeguhs', 'einde2_mikas', 'einde2_zwevers', 'einde2_appa', 'einde2_vetbootje', 'einde2_vesting', 'einde2_deco',
            'einde2_bouw_lucht', 'einde2_bouw_water', 'einde2_bouw_aarde', 'einde2_bouw_vuur', 'einde2_bouw_zij1', 'einde2_bouw_zij2',
            'einde2_sponsguh_wezens', 'einde2_bouw_bikinibodem', 'einde2_bouw_schelpenstad',
            'einde2_cast_team', 'einde2_cast_volk', 'einde2_cast_schurk',
            'einde2_geestmodellen', 'einde2_geestenwereld',
            # einde2 phase B + C (CONTRACT_E2_B 1.2): the story state, then the ten slices (this order = the order of the FTB sections)
            'einde2_verhaal', 'einde2_h1_lucht', 'einde2_h2_water', 'einde2_h3_aarde', 'einde2_h4_vuur', 'einde2_geest_spel', 'einde2_zij',
            'einde2_reizen', 'einde2_audio', 'einde2_trap5_machines', 'einde2_trap5_ballon',
            # einde2 Sponsguh (CONTRACT_E2_C 1.2): the skelet, then the nine slices (this order = the story order of the FTB chain)
            'einde2_sp_verhaal', 'einde2_sp_cast', 'einde2_sp_d1_start', 'einde2_sp_d1_stad', 'einde2_sp_d1_recept', 'einde2_sp_d2_heen',
            'einde2_sp_d2_weg', 'einde2_sp_d2_finale', 'einde2_sp_audio', 'einde2_sp_zij',
            # 1.6.0 Op Safari! (append only, see above): the kern of the safari park
            'safari', 'safari_dieren',   # (safari_park is NOT a feature: the package safari_park/ shadows safari_park.py; the park is built on its own)
            # 1.6.0 step 2 (append only)
            'safari_blokken', 'safari_camera', 'safari_lobby', 'safari_verblijf', 'safari_rit', 'safari_quests',
            # 1.6.0: De Eeuwige Knabbelvelden (append only)
            'knabbelvelden',
            # 1.6.0 deel 2: de Bouwhulp (F8 kern; append only)
            'bouwhulp',
            # 1.6.0 deel 2 F9: vormstaf, kwasten, blauwdrukken (append only)
            'bouwhulp_gereedschap',
            # 1.6.0 deel 2 F10: de Beverbeek (F10a: worldgen; it only adds its own region term and biome entry, idempotently, so
            # its place after bio_wereld is the state F10a's standalone run left), then the helper texts (F10b)
            'safari_beverbeek', 'bouwhulp_helpers',
            # 1.6.0 G1: the gaps of the design audit (append only)
            'safari_g1',
            # 1.6.0 F1b: het safari-busstation in de Guhmension (append only)
            'safari_busstation',
            # 1.6.0 test round 5, fix slice FX-B: its texts (append only)
            'safari_fxb',
            # 1.6.0 test round 5 FX-C: the verblijfsbord and the Bouwkeet explained (texts; after the slices whose keys it rewrites)
            'bouwhulp_fxc',
            # 1.6.2: Roep naar mij always (append only)
            'roep162',
            # 1.6.2: the Beverguh + the knaagdier outfit (append only)
            'beverbeek_guhs',
            # 1.6.4: the verblijfsbord's family dialog (append only)
            'familie164',
            # 1.7.0 Onder de Wol: the kern, then its creatures (append only)
            'onderwol', 'onderwol_wezens']


def modules():
    return [importlib.import_module(f"features.{name}") for name in FEATURES]
