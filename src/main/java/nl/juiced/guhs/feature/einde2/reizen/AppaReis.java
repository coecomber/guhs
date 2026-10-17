package nl.juiced.guhs.feature.einde2.reizen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
 *   <li>einde2 fix4-appa: {@link #naarPlatform} flies to the player's own Appa-platform ({@link AppaPlatform}): in its own
 *       dimension a real flight onto its middle; from any other dimension (also from the Guheinde) the same dive through the
 *       void, coming out on the platform. Since the whistle now calls Appa outside the Guheinde only at the own platform,
 *       a trip from the overworld to the Guheinde starts there (or wherever the player rode him to). The other players on
 *       Appa's back come along through the void.</li>
 * </ul>
 */
public final class AppaReis {
    /** Ticks of the dark screen before the jump, and after it until Appa stands beside the player. */
    static final int DONKER = 24, NA = 10, MAX_WACHT = 200;

    /**
     * A dive: to a Guheinde place ({@code structuur}) or (fix4-appa) to the player's platform ({@code structuur} null, the
     * platform's middle in {@code dim}); {@code mee}: the other players on Appa, who come along.
     */
    private record Overzet(@Nullable String structuur, ResourceKey<Level> dim, @Nullable BlockPos platform, List<UUID> mee, int sprong, int klaar) {
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
        if (!kan(p)) {
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

    /**
     * einde2 fix4-appa: flies this player on their Appa to their own Appa-platform in {@code dim} ({@link AppaPlatform}):
     * in the same dimension a real flight onto its middle (the riders stay on him); from another dimension the dive through
     * the void, which comes out on the platform (Appa on its middle, the riders beside him on the square). The platform is
     * checked first (the 5x5 rule); a line says what is missing. False: it cannot now.
     */
    public static boolean naarPlatform(ServerPlayer p, ResourceKey<Level> dim) {
        if (!kan(p)) {
            return false;
        }
        MinecraftServer server = p.level().getServer();
        ServerLevel doel = server.getLevel(dim);
        BlockPos pos = AppaPlatform.van(p, dim);
        if (doel == null || pos == null) {
            zeg(p, "gui.guhs.einde2_reizen.platform.geen");
            return false;
        }
        doel.getChunk(pos);                                        // (a platform far away or in another dimension: load it)
        AppaPlatform.Controle c = AppaPlatform.controleer(doel, pos);
        if (!c.ok()) {
            if (!c.blok()) {
                AppaPlatform.vergeet(p, dim);
            }
            AppaPlatform.meld(p, doel, pos, c);
            return false;
        }
        Component naam = AppaPlatform.naam(p.getGameProfile().name());
        if (p.level() == doel) {
            if (!Appa.vlieg(p, AppaPlatform.landing(pos), true)) {
                zeg(p, "gui.guhs.einde2_reizen.fluit.kan_niet");
                return false;
            }
            p.sendSystemMessage(Component.translatable("gui.guhs.einde2_reizen.fluit.op_weg", naam).withStyle(ChatFormatting.AQUA));
            return true;
        }
        return duik(p, new Overzet(null, dim, pos, meerijders(p), 0, -1), naam);
    }

    /** Can this player go anywhere on Appa now (else a line, or nothing during a cutscene / a dive)? */
    private static boolean kan(ServerPlayer p) {
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
        return !Cutscenes.bezig(p) && !OVER.containsKey(p.getUUID());
    }

    /** The other players who sit on this player's Appa (they come along through the void). */
    private static List<UUID> meerijders(ServerPlayer p) {
        AppaGuhEntity appa = Appa.geladen(p);
        List<UUID> uit = new ArrayList<>();
        if (appa != null) {
            for (Entity e : appa.getPassengers()) {
                if (e instanceof ServerPlayer g && g != p) {
                    uit.add(g.getUUID());
                }
            }
        }
        return uit;
    }

    private static boolean doorDeLeegte(ServerPlayer p, ServerLevel g, String structuur) {
        if (Landingen.van(g, structuur, true) == null) {
            zeg(p, "gui.guhs.einde2_reizen.fluit.nog_niet");
            return false;
        }
        return duik(p, new Overzet(structuur, GuheindeFeature.GUHEINDE, null, meerijders(p), 0, -1),
                Component.translatable("structure.guhs." + structuur));
    }

    /** The start of a dive: dark screens, his roar, the line; the jump itself follows after {@value #DONKER} ticks. */
    private static boolean duik(ServerPlayer p, Overzet o, Component naar) {
        int nu = p.level().getServer().getTickCount();
        for (ServerPlayer w : wie(p, o)) {
            w.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DONKER + NA + 30, 0, false, false, false));
            w.sendSystemMessage(Component.translatable("gui.guhs.einde2_reizen.leegte.op_weg", naar).withStyle(ChatFormatting.AQUA));
        }
        p.level().playSound(null, p.blockPosition(), E2AppaFeature.BRUL.get(), SoundSource.NEUTRAL, 1.2f, 0.8f);
        OVER.put(p.getUUID(), new Overzet(o.structuur(), o.dim(), o.platform(), o.mee(), nu + DONKER, -1));
        return true;
    }

    /** The owner and the riders who are still online. */
    private static List<ServerPlayer> wie(ServerPlayer p, Overzet o) {
        List<ServerPlayer> uit = new ArrayList<>();
        uit.add(p);
        for (UUID u : o.mee()) {
            ServerPlayer g = p.level().getServer().getPlayerList().getPlayer(u);
            if (g != null && g != p && g.isAlive()) {
                uit.add(g);
            }
        }
        return uit;
    }

    /** Where a dive comes out (Appa's landing, the owner's spot), or null: the place is not known, the platform is gone. */
    @Nullable
    private static Appa.Landing doelVan(ServerLevel l, Overzet o) {
        if (o.structuur() != null) {
            return Landingen.van(l, o.structuur(), true);
        }
        if (o.platform() == null || !l.getBlockState(o.platform()).is(E2ReizenFeature.APPA_PLATFORM.get())) {
            return null;
        }
        Appa.Landing a = AppaPlatform.landing(o.platform());
        return new Appa.Landing(a.appa(), a.yaw(), AppaPlatform.staanBij(o.platform()), null, false);
    }

    private static Component naamVan(ServerPlayer p, Overzet o) {
        return o.structuur() != null ? Component.translatable("structure.guhs." + o.structuur()) : AppaPlatform.naam(p.getGameProfile().name());
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
            ServerLevel g = server.getLevel(o.dim());
            if (p == null || g == null || !p.isAlive()) {
                OVER.remove(e.getKey(), o);
                continue;
            }
            if (o.klaar() < 0) {
                if (nu < o.sprong() || Cutscenes.bezig(p)) {
                    continue;
                }
                if (o.platform() != null) {
                    g.getChunk(o.platform());
                }
                Appa.Landing l = doelVan(g, o);
                if (l == null) {
                    OVER.remove(e.getKey(), o);
                    if (o.platform() != null) {
                        zeg(p, "gui.guhs.einde2_reizen.platform.weg");
                    }
                    continue;
                }
                Vec3 naar = l.speler() != null ? l.speler() : l.appa().add(0, 0, 3);
                float yaw = Float.isNaN(l.yaw()) ? p.getYRot() : l.yaw();
                int i = 0;
                for (ServerPlayer w : wie(p, o)) {
                    // (the riders side by side; whoever does not fit is moved to the nearest free spot after the jump)
                    double opzij = i == 0 ? 0 : (i % 2 == 1 ? 1.2 : -1.2) * ((i + 1) / 2);
                    i++;
                    w.stopRiding();
                    w.teleportTo(g, naar.x + opzij, naar.y + 0.05, naar.z, Set.of(), yaw, 0f, true);
                    w.resetFallDistance();
                    w.setDeltaMovement(Vec3.ZERO);
                    w.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false, false));
                }
                OVER.replace(e.getKey(), o, new Overzet(o.structuur(), o.dim(), o.platform(), o.mee(), nu, nu + NA));
                continue;
            }
            if (nu < o.klaar() || !g.hasChunkAt(BlockPos.containing(p.position())) && nu - o.sprong() < MAX_WACHT) {
                continue;
            }
            OVER.remove(e.getKey(), o);
            if (p.level() != g) {
                continue;
            }
            Appa.Landing l = doelVan(g, o);
            AppaGuhEntity appa = Appa.roep(p);
            if (l != null && appa != null) {
                float yaw = Float.isNaN(l.yaw()) ? appa.getYRot() : l.yaw();
                g.getChunk(BlockPos.containing(l.appa()));
                appa.snapTo(l.appa().x, l.appa().y + 0.05, l.appa().z, yaw, 0);
                appa.setYBodyRot(yaw);
                appa.setYHeadRot(yaw);
                appa.brul();
                for (ServerPlayer w : wie(p, o)) {
                    if (w.level() != g) {
                        continue;
                    }
                    Vec3 staan = Appa.vrijePlek(g, w.getType(), w == p && l.speler() != null ? l.speler() : w.position(), 3, true);
                    if (staan != null && staan.distanceToSqr(w.position()) > 0.25) {
                        w.teleportTo(staan.x, staan.y, staan.z);
                        w.resetFallDistance();
                    }
                }
            }
            for (ServerPlayer w : wie(p, o)) {
                w.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.leegte.aangekomen", naamVan(p, o)).withStyle(ChatFormatting.AQUA));
            }
            if (o.structuur() != null) {
                E2Reizen.aangekomenZonderVlucht(p, o.structuur());
            }
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
