package nl.juiced.guhs.feature.onderwol.stadje;

import nl.juiced.guhs.item.SuperkompasItem;

/**
 * 1.7.0 "Onder de Wol" (rounds 32-43): the Vadsmijnstadje, an underground town of guh villagers in a big cave hall in the
 * knuffelsteen layer, with the Guhrad-centrale on its plaza and a wolkenlift to an entrance hut on the surface. It is a
 * {@code guhs:grot_bouw} made entirely by {@code tools/features/onderwol_stadje.py} (template, pools, structure, set about
 * every 1500 blocks, loot, texts); Java only adds it to the Superkompas tab "ondergrond".
 * <p>
 * The villagers in the template are real villagers of type {@code guhs:guh} with a guh profession each (the four of
 * feature/guhvillagers and five older guh jobs), Xp 1 so they keep their job, each with its workstation and bed in its own
 * house; two children, two IJzerguhlems.
 */
public final class Vadsmijnstadje {
    public static final String NAAM = "vadsmijnstadje";

    public static void register() {
        SuperkompasItem.voegToe("ondergrond", NAAM);
    }

    private Vadsmijnstadje() {
    }
}
