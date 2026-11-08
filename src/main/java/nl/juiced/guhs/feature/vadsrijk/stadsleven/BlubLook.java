package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import net.minecraft.util.RandomSource;

/**
 * W-MODELLEN (round W, the user: "ik heb liever gewoon de andere blob model die we hebben"): the light-blue town blobs (Diertje,
 * and the Wandelaar's pet blob) wear the Blub-guh model, the same blob as in Guhdromenland and the tamable Blubs in survival
 * ({@code feature/blub}, model {@code blubguh}). They look like a blue Blub at an early stage of its Blub-Guh-Meter (p, b, 20..55:
 * from a pure Blub to the :3 face; all of these show the same bones: {@code lijf}, the two little ears and the feet), in one
 * of its three tints. Before round W they used the zone-1 klodder model with {@code textures/vadsrijk/stadsleven/blauwe_klodder.png}.
 */
public final class BlubLook {
    /** The model and the animation file (guhs:entity/blubguh). */
    public static final String MODEL = "blubguh", ANIM = "blubguh";
    /** The default texture (the Wandelaar's pet); the Diertjes pick one with {@link #textuur}. */
    public static final String TEX = "textures/entity/blubguh/blauw_0_35.png";
    /** Its animations: idle / blij when it stands still, walk (the Blub's hop) when it moves. */
    public static final String IDLE = "idle", BLIJ = "blij", HOP = "walk";
    /** Render scale of a town blob (the Blub's body is 10 px wide: about 0.4 block) and of the pet on the leash. */
    public static final float SCHAAL = 0.6f, SCHAAL_HUISDIER = 0.5f;
    /** The meter stages (texture suffix) a town blob can have: all of them show the same bones (stap 0..3). */
    public static final String[] STAPPEN = {"p", "b", "20", "25", "30", "35", "40", "45", "50", "55"};
    /**
     * The Blub-guh bones a town blob never shows (BlubClient.zichtbaar for the blue soort at stap 0..3: the other soorten's
     * bits, the soft body and guh ears of the later stages, the strikje and the Blub-kleding). Hidden by the prop renderer.
     */
    public static final String[] VERBORGEN = {"lijf_zacht", "guhoortje_l", "guhoortje_r", "guhoor_l", "guhoor_r",
            "weide_sprieten", "weide_hengel", "schild_vleugel_l", "schild_vleugel_r", "schild_sprieten", "ravijn_spriet_l",
            "ravijn_spriet_r", "ravijn_vin_l", "ravijn_vin_r", "zwam_gewei", "strik",
            "kl_slaapmutsje", "kl_sjaaltje", "kl_sterrenkroontje", "kl_bubbelhelm", "kl_pyjamaatje", "kl_slaapmaskertje",
            "kl_bloemenkransje", "kl_brilletje"};

    private BlubLook() {
    }

    /** A random blue Blub look: one of the 3 tints at one of the early stages. */
    public static String textuur(RandomSource r) {
        return "textures/entity/blubguh/blauw_" + r.nextInt(3) + "_" + STAPPEN[r.nextInt(STAPPEN.length)] + ".png";
    }
}
