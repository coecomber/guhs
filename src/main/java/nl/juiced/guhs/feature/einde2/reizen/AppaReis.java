package nl.juiced.guhs.feature.einde2.reizen;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.appa.E2AppaFeature;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 fix-reizen: the public way to send a player somewhere on Appa (his menu uses it; so may "go on with the story" of
 * the kern: {@code AppaReis.vliegNaar(p, E2Reizen.verhaalDoel(p))}). These signatures stay as they are (CONTRACT_E2_B,
 * Deviations [fix-reizen]).
 * <ul>
 *   <li>{@link #vliegNaar}: in the Guheinde Appa comes to the player (from wherever he is), seats them and flies them to the
 *       place: the story flight that waits for it when there is one (with its team chat and its {@code daarna}), else a free
 *       flight. Story places land on the spot of their arrival scene.</li>
 *   <li><b>From another dimension</b> (the overworld, the Guhpixel lobby, ...: the whistle calls Appa there too): the screen
 *       goes dark, Appa "dives into the void" with the player, and they come out standing on the landing place, Appa on
 *       his spot beside them. No long flight through unknown chunks, nothing to fall off; the place's own arrival (its veil,
 *       its scene) starts as if they had flown in, and a story flight that waited for this place is done.</li>
 *   <li>In the Geestenwereld Appa cannot follow (the body stays behind in the meditation circle): a line, nothing else.</li>
 * </ul>
 */
public final class AppaReis {
    /** Ticks of the dark screen before the jump, and after it until Appa stands beside the player. */
    static final int DONKER = 24, NA = 10, MAX_WACHT = 200;

    private record Overzet(String structuur, int sprong, int klaar) {
    }

    /** Players on their way through the void (to the Guheinde). */
    private static final Map<UUID, Overzet> OVER = new ConcurrentHashMap<>();

    private AppaReis() {
    }

    /**
     * Flies (or takes) this player on their own Appa to this place of the Guheinde (a structure id, e.g.
     * {@code noordelijke_waterstam}). The player gets a line about what happens. False: it cannot now (no Appa, he is kept
     * away, in the Geestenwereld or a cutscene, the place is not known yet); then nothing happened but that line.
     */
    public static boolean vliegNaar(ServerPlayer p, String structuur) {
        if (inGeestenwereld(p)) {
            zeg(p, "gui.guhs.einde2_reizen.fluit.niet_hier");
            return false;
        }
        if (!Appa.heeft(p)) {
            zeg(p, "gui.guhs.einde2_appa.fluitje.geen");
            return false;
        }
        if (Appa.isWeg(p)) {
            zeg(p, "gui.guhs.einde2_appa.fluitje.weg");
            return false;
        }
        if (Cutscenes.bezig(p) || OVER.containsKey(p.getUUID())) {
            return false;
        }
        ServerLevel g = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (g == null) {
            return false;
        }
        if (!Plekken.bekend(g, structuur) || Plekken.aankomst(g, structuur) == null) {
            Plekken.zoekVooruit(g);
            zeg(p, "gui.guhs.einde2_reizen.fluit.nog_niet");
            return false;
        }
        if (p.level() != g) {
            return doorDeLeegte(p, g, structuur);
        }
        String[] w = E2Reizen.wachtend(p);
        boolean ok = w != null && w[0].equals(structuur) ? E2Reizen.hervat(p) : E2Reizen.vlieg(p, structuur, E2Reizen.Vlucht.VRIJ, null);
        if (ok) {
            p.sendSystemMessage(Component.translatable("gui.guhs.einde2_reizen.fluit.op_weg", Component.translatable("structure.guhs." + structuur))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            zeg(p, "gui.guhs.einde2_reizen.fluit.kan_niet");
        }
        return ok;
    }

    /**
     * The player's Appa comes to them, wherever they are (as the whistle does; also from another dimension). False: they
     * have no Appa, or he is kept away.
     */
    public static boolean roepBij(ServerPlayer p) {
        AppaGuhEntity appa = Appa.roep(p);
        if (appa == null) {
            return false;
        }
        appa.brul();
        p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_appa.fluitje.komt", appa.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** The place where this player's story goes on (see {@link E2Reizen#verhaalDoel}), or null. */
    @Nullable
    public static String verhaalDoel(ServerPlayer p) {
        return E2Reizen.verhaalDoel(p);
    }

    static boolean inGeestenwereld(ServerPlayer p) {
        return p.level().dimension() == Geestenwereld.DIM;
    }

    // =====================================================================================================================
    // through the void, from another dimension
    // =====================================================================================================================

    private static boolean doorDeLeegte(ServerPlayer p, ServerLevel g, String structuur) {
        if (Landingen.van(g, structuur, true) == null) {
            zeg(p, "gui.guhs.einde2_reizen.fluit.nog_niet");
            return false;
        }
        int nu = p.level().getServer().getTickCount();
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DONKER + NA + 30, 0, false, false, false));
        p.level().playSound(null, p.blockPosition(), E2AppaFeature.BRUL.get(), SoundSource.NEUTRAL, 1.2f, 0.8f);
        p.sendSystemMessage(Component.translatable("gui.guhs.einde2_reizen.leegte.op_weg", Component.translatable("structure.guhs." + structuur))
                .withStyle(ChatFormatting.AQUA));
        OVER.put(p.getUUID(), new Overzet(structuur, nu + DONKER, -1));
        return true;
    }

    /** Every server tick: the jumps through the void. */
    static void tick(MinecraftServer server) {
        if (OVER.isEmpty()) {
            return;
        }
        int nu = server.getTickCount();
        for (Map.Entry<UUID, Overzet> e : OVER.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Overzet o = e.getValue();
            ServerLevel g = server.getLevel(GuheindeFeature.GUHEINDE);
            if (p == null || g == null || !p.isAlive()) {
                OVER.remove(e.getKey(), o);
                continue;
            }
            if (o.klaar() < 0) {
                if (nu < o.sprong() || Cutscenes.bezig(p)) {
                    continue;
                }
                Appa.Landing l = Landingen.van(g, o.structuur(), true);
                if (l == null) {
                    OVER.remove(e.getKey(), o);
                    continue;
                }
                Vec3 naar = l.speler() != null ? l.speler() : l.appa().add(0, 0, 3);
                float yaw = Float.isNaN(l.yaw()) ? p.getYRot() : l.yaw();
                p.stopRiding();
                p.teleportTo(g, naar.x, naar.y + 0.05, naar.z, Set.of(), yaw, 0f, true);
                p.resetFallDistance();
                p.setDeltaMovement(Vec3.ZERO);
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false, false));
                OVER.replace(e.getKey(), o, new Overzet(o.structuur(), nu, nu + NA));
                continue;
            }
            if (nu < o.klaar() || !g.hasChunkAt(BlockPos.containing(p.position())) && nu - o.sprong() < MAX_WACHT) {
                continue;
            }
            OVER.remove(e.getKey(), o);
            if (p.level() != g) {
                continue;
            }
            Appa.Landing l = Landingen.van(g, o.structuur(), true);
            AppaGuhEntity appa = Appa.roep(p);
            if (l != null && appa != null) {
                float yaw = Float.isNaN(l.yaw()) ? appa.getYRot() : l.yaw();
                g.getChunk(BlockPos.containing(l.appa()));
                appa.snapTo(l.appa().x, l.appa().y + 0.05, l.appa().z, yaw, 0);
                appa.setYBodyRot(yaw);
                appa.setYHeadRot(yaw);
                appa.brul();
                Vec3 staan = Appa.vrijePlek(g, p.getType(), l.speler() != null ? l.speler() : p.position(), 3, true);
                if (staan != null && staan.distanceToSqr(p.position()) > 0.25) {
                    p.teleportTo(staan.x, staan.y, staan.z);
                    p.resetFallDistance();
                }
            }
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.leegte.aangekomen", Component.translatable("structure.guhs." + o.structuur()))
                    .withStyle(ChatFormatting.AQUA));
            E2Reizen.aangekomenZonderVlucht(p, o.structuur());
        }
    }

    /** A player who left the game halfway through the void: they log in where they were. */
    static void vergeet(UUID speler) {
        OVER.remove(speler);
    }

    static void leeg() {
        OVER.clear();
    }

    private static void zeg(ServerPlayer p, String key) {
        p.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
