package nl.juiced.guhs.feature.snuffel;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * The companion's little tree: it stands on a fixed spot of the island ({@code "boom"} in eiland.json; one entity,
 * {@link BoompjeEntity}) and every player sees it in THEIR OWN stage: 0 = only the ring of stones, 1 kiem, 2 scheutje,
 * 3 struikje, 4 jong boompje. The stage is kept per player ({@link SnuffelData}, key {@code Boom}) and drawn by the
 * player's own client; the server entity knows no stage.
 * <p>
 * Every step plays the growth scene ({@link #GROEI}, six seconds): the camera floats sideways high over the village,
 * looking slightly down at the tree, and halfway the tree pops into its next stage. The scene is anchored on the tree's
 * foot and turned by {@code "boom_draai"}; the camera stands 12 blocks to the (unturned) south of the tree, 6 blocks up,
 * and moves from 9 blocks west to 9 blocks east: keep that side of the tree open.
 */
public final class Boom {
    /** The last stage (jong boompje). The later update goes on from here. */
    public static final int MAX = 4;
    /** The tick of the scene at which the tree shows its new stage. */
    public static final int POP = 50;
    private static final Vec3 KRUIN = new Vec3(0.5, 1.0, 0.5);

    /** The growth scene: reusable for every step (and for whoever wants to show the tree). */
    public static final Cutscene GROEI = Cutscene.maak("snuffel_groei").duur(120)
            .camera(0, new Vec3(-8.5, 6.0, 12.5), KRUIN)
            .camera(120, new Vec3(9.5, 6.0, 12.5), KRUIN)
            .geluid(POP, SnuffelFeature.GROEI_GELUID, 0.9f, 1f)
            .deeltjes(POP, ParticleTypes.HAPPY_VILLAGER, new Vec3(0.5, 0.9, 0.5), 24, 0.45)
            .deeltjes(POP + 12, ParticleTypes.HAPPY_VILLAGER, new Vec3(0.5, 1.2, 0.5), 10, 0.35)
            .registreer();

    private Boom() {
    }

    static void init() {
        // (loads the class: the scene must be registered on both sides before a world starts)
    }

    /** The stage of this player's tree (0..{@link #MAX}). */
    public static int stap(ServerPlayer p) {
        return SnuffelData.heeft(p) ? Mth.clamp(SnuffelData.van(p).getIntOr("Boom", 0), 0, MAX) : 0;
    }

    /**
     * The tree grows one step for this player, with the growth scene. The step itself is kept at once (a scene that is
     * broken off by a logout loses nothing). {@code daarna} runs when the scene was watched to the end, or at once when no
     * scene could play (not on the island, already watching something, the tree full-grown); a scene that is broken off
     * never runs it, so keep progress out of it. False when the tree is at its last stage already.
     */
    public static boolean groei(ServerPlayer p, @Nullable Consumer<ServerPlayer> daarna) {
        int oud = stap(p);
        if (oud >= MAX) {
            if (daarna != null) {
                daarna.accept(p);
            }
            return false;
        }
        SnuffelData.van(p).putInt("Boom", oud + 1);
        boolean scene = speelGroei(p, daarna);
        Stand.stuur(p, scene ? POP : 0);
        if (!scene) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.boom.groeit." + (oud + 1)).withStyle(ChatFormatting.GREEN));
            if (daarna != null) {
                daarna.accept(p);
            }
        }
        return true;
    }

    /** Sets the stage without a scene (the story's own moments, dev, tests). */
    public static void zet(ServerPlayer p, int stap) {
        SnuffelData.van(p).putInt("Boom", Mth.clamp(stap, 0, MAX));
        Stand.stuur(p);
    }

    /**
     * Plays the growth scene at the tree of the island the player is on. False (and nothing happens, {@code daarna}
     * included) when the player is on no island or is already watching something.
     */
    public static boolean speelGroei(ServerPlayer p, @Nullable Consumer<ServerPlayer> daarna) {
        Eiland.Plaats plaats = Eiland.van(p);
        if (plaats == null) {
            return false;
        }
        Rotation draai = Rotation.values()[Math.floorMod(plaats.opzet().boomDraai(), Rotation.values().length)];
        return Cutscenes.speel(p, GROEI, plaats.boom(), draai, daarna);
    }

    /**
     * What the tree gives at the end of the first series: a twig of its blossom, once per player (it travels home with
     * the post when the player is a dog). False when the player has had it.
     */
    public static boolean geefCadeau(ServerPlayer p) {
        if (SnuffelData.van(p).getBooleanOr("BoomCadeau", false)) {
            return false;
        }
        SnuffelData.van(p).putBoolean("BoomCadeau", true);
        Snuffel.geef(p, new ItemStack(SnuffelFeature.SNUFFEL_BLOESEMTAKJE.get()));
        return true;
    }

    public static boolean cadeauGehad(ServerPlayer p) {
        return SnuffelData.heeft(p) && SnuffelData.van(p).getBooleanOr("BoomCadeau", false);
    }
}
