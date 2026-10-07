package nl.juiced.guhs.feature.einde2.biomeguhs;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;

/**
 * einde2, slice biomeguhs: the little trick of each tamed biome guh, hooked into every guh through {@link GuhHooks} and
 * {@code VariantGedragen} (GuhEntity itself is never edited):
 * <ul>
 *   <li>{@link Korstbal}: the Korstguh rolls up into a ball you can push, and naps in a kaasgat ({@link KaasgatDutje});</li>
 *   <li>{@link Duingraver}: the Duinguh digs in on kruimelzand until only its ears show, and never sinks in it;</li>
 *   <li>{@link Kruimelzweef}: the Kruimelguh floats down slowly and stands on the kruimelwolken;</li>
 *   <li>{@link Schimmellamp}: the Schimmelguh glows like a lamp;</li>
 *   <li>{@link Kristalneus}: the Kristalguh now and then finds a kristalkaasbrokje;</li>
 *   <li>{@link Koorzang}: the Koorguh sings along with a jukebox.</li>
 * </ul>
 * No trick starts with a click of the owner: the client swallows an empty hand on your own guh (a tap is a pet), so every
 * trick happens by itself. What the client must know is one synced flag ({@link E2Vlaggen#TRUC}).
 */
public final class Trucs {
    /** Persistent key of a tamed biome guh: its owner already heard what it can do. */
    public static final String WELKOM = "guhs_einde2_biomeguhs_welkom";

    /** (GuhHooks.tick: every guh, every tick, server side) */
    static void tick(GuhEntity guh) {
        switch (guh.getVariant()) {
            case KORSTGUH -> Korstbal.serverTick(guh);
            case DUINGUH -> Duingraver.serverTick(guh);
            case KRUIMELGUH -> Kruimelzweef.serverTick(guh);
            case SCHIMMELGUH -> Schimmellamp.serverTick(guh);
            case KRISTALGUH -> Kristalneus.serverTick(guh);
            case KOORGUH -> Koorzang.serverTick(guh);
            default -> {
                return;
            }
        }
        if ((guh.tickCount + guh.getId()) % 20 == 0 && tam(guh) && !guh.getPersistentData().getBooleanOr(WELKOM, false)) {
            welkom(guh);
        }
    }

    /** The first time its owner is around after taming: what this guh can do, and the advancement of the first one. */
    private static void welkom(GuhEntity guh) {
        ServerPlayer baas = baas(guh);
        if (baas == null) {
            return;
        }
        guh.getPersistentData().putBoolean(WELKOM, true);
        baas.sendSystemMessage(Component.translatable("gui.guhs.einde2_biomeguhs.getemd." + guh.getVariant().id(), guh.getDisplayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        GidsFeature.grant(baas, "guheinde/einde2_biomeguhs_eerste");
    }

    /** A tamed, living guh that is not a story copy: the only kind that does tricks. */
    public static boolean tam(GuhEntity guh) {
        return guh.isTame() && guh.isAlive() && !VerhaalGuhs.isKopie(guh);
    }

    /** Its owner, when online and in the same world. */
    @Nullable
    public static ServerPlayer baas(GuhEntity guh) {
        return guh.getOwner() instanceof ServerPlayer sp && sp.level() == guh.level() && !sp.isSpectator() ? sp : null;
    }

    /** Its owner, when within this many blocks. */
    @Nullable
    public static ServerPlayer baasBij(GuhEntity guh, double afstand) {
        ServerPlayer baas = baas(guh);
        return baas != null && baas.distanceToSqr(guh) <= afstand * afstand ? baas : null;
    }

    /** Is this guh doing its trick now (the synced flag)? */
    public static boolean doet(GuhEntity guh) {
        return GuhHooks.heeft(guh, E2Vlaggen.TRUC);
    }

    static void zet(GuhEntity guh, boolean aan) {
        if (doet(guh) != aan) {
            GuhHooks.zet(guh, E2Vlaggen.TRUC, aan);
        }
    }

    static void geluid(GuhEntity guh, SoundEvent geluid, float volume, float pitch) {
        guh.level().playSound(null, guh.getX(), guh.getY(), guh.getZ(), geluid, SoundSource.NEUTRAL, volume, pitch);
    }

    /** A short line above the owner's hotbar, with the guh's name. */
    static void meld(ServerPlayer baas, GuhEntity guh, String key) {
        baas.sendOverlayMessage(Component.translatable("gui.guhs.einde2_biomeguhs." + key, guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private Trucs() {
    }
}
