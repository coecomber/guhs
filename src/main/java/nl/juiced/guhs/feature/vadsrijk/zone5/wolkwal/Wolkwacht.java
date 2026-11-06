package nl.juiced.guhs.feature.vadsrijk.zone5.wolkwal;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Figurant;

/**
 * T-WOLKWAL: the "Wolkwacht-ridder", the elite knight guh of Hoogvads: a real guh (the core bones of the guh model, so every guh
 * animation fits) in cloud-blue/white armour with gold trim, a white-and-blue plume, a cloud collar, a short cape, a little cloud
 * shield and a sword in its scabbard. Made by {@code tools/vadsrijk/wolkwal/maak_modellen.py} (one texture per fur).
 * <ul>
 *   <li>props ({@link Z3Figurant}): {@link #prop} (Z5Bewoners' knights on the oefenplein, at the gates, in the Proefarena);</li>
 *   <li>NPC guhs: t-z5verhaal's Wolkwacht NPCs ({@code z5_wacht_*}: knight helmet + cloud collar) are drawn with this model by
 *       {@code client.WolkwachtGuhRenderer} ({@link #isWolkwacht});</li>
 *   <li>t-z5wezens' Sparringridder: the same knight with a pillow sword and a big cloud shield on ITS bones
 *       ({@code sparringridder.geo.json}, copied over theirs at integration: {@code maak_modellen.py --sparring-over}).</li>
 * </ul>
 */
public final class Wolkwacht {
    /** geckolib/models/entity/... and geckolib/animations/entity/... (Z3Figurant ids). */
    public static final String MODEL = "vadsrijk/zone5/wolkwal/wolkwacht_ridder", ANIM = MODEL;
    /** The furs there is a knight texture for (the first is the fallback). */
    public static final List<String> VACHTEN = List.of("guh", "guh_snow", "guh_golden", "guh_mint", "guh_choco", "guh_wolk");

    private Wolkwacht() {
    }

    /** The texture path of the knight in this fur (an unknown fur: the plain guh). */
    public static String textuur(String vacht) {
        return "textures/vadsrijk/zone5/wolkwal/wolkwacht_ridder_" + (VACHTEN.contains(vacht) ? vacht : VACHTEN.get(0)) + ".png";
    }

    /** The fur for a guh variant (SNOW -> guh_snow, ...; others: the plain guh). */
    public static String vacht(@Nullable GuhVariant v) {
        String f = v == null || v == GuhVariant.NORMAL ? "guh" : "guh_" + v.name().toLowerCase(Locale.ROOT);
        return VACHTEN.contains(f) ? f : "guh";
    }

    /** A fur for the n-th knight of a formation (varied, but always the same for the same n). */
    public static String vacht(int n) {
        return VACHTEN.get(Math.floorMod(n * 7 + 3, VACHTEN.size()));
    }

    /** Whether a guh with these clothes is one of the Wolkwacht (the elite: a knight's helmet and the cloud collar). */
    public static boolean isWolkwacht(@Nullable Object hoofd, @Nullable Object nek) {
        return hoofd == nl.juiced.guhs.entity.GuhClothes.KNIGHT_HELMET && nek == nl.juiced.guhs.entity.GuhClothes.WOLKENKRAAG;
    }

    /** A knight prop (no hitbox, never saved): standing guard, or sitting. */
    @Nullable
    public static Z3Figurant prop(ServerLevel sl, Vec3 waar, float yaw, String vacht, boolean zit, float schaal) {
        return Z3Figurant.maak(sl, waar, yaw, MODEL, textuur(vacht), ANIM, zit ? "animation.guh.sit" : "animation.guh.idle", schaal);
    }
}
