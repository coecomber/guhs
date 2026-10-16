package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;

/**
 * einde2 phase B (CONTRACT_E2_B 3, 2.3, 2.4), owner: slice geest_spel. The way into the Geestenwereld for a player and the
 * guests round them, and which zones are open for whom.
 * <ul>
 *   <li>{@link #stuur}: the guide and every player within {@value #GAST_STRAAL} blocks go in (the others as guests);</li>
 *   <li>{@link #zoneOpen}: the gating chain of 2.3 (flag {@code geest} of chapter 2: the arrival, Koh's cave and the
 *       Hemelvijver; flag {@code witte_lotus} of the finale: also the White Lotus, the Top and Fang's path; the finale done:
 *       everything), plus this slice's own areas (the Lotusarchief after the Oude Reigerguh, the eighth island after the whole
 *       story or all tiles and chakras); a guest also has what their guide has while the guide is inside;</li>
 *   <li>a closed zone: its path lanterns ({@code padlantaarn_<zone>_n}) are dark for that player only ({@link Schijn}), and
 *       the mist sets whoever walks into it back where they came from (never damage).</li>
 * </ul>
 */
public final class GeestToegang {
    /** Who stands this close to a meditating player goes along as a guest (DESIGN 5 "Samen"). */
    public static final double GAST_STRAAL = 5.0;
    /** The zones of 2.3 in their three tiers (the names of {@link Geestenwereld#ZONES}). */
    public static final List<String> ZONES_GEEST = List.of("meditatiecirkel", "koh_grot", "hemelvijver");
    public static final List<String> ZONES_LOTUS = List.of("lotus_ruines", "top_voorgangers", "fang_pad");
    public static final List<String> ZONES_VRIJ = List.of("gefluisterwoud", "theepaviljoen");
    /** This slice's own closed areas (zone markers of their own). */
    public static final String ARCHIEF = "lotusarchief", EILAND_8 = "eiland_8";

    /** guest -> guide (only while both are in the Geestenwereld). */
    private static final Map<UUID, UUID> GASTEN = new ConcurrentHashMap<>();
    /** The last spot of each player in the Geestenwereld that was in no closed zone (where the mist puts you back). */
    private static final Map<UUID, Vec3> VEILIG = new ConcurrentHashMap<>();

    private GeestToegang() {
    }

    // =====================================================================================================================
    // the API (CONTRACT_E2_B 3)
    // =====================================================================================================================

    /** The guide and every player within 5 blocks into the Geestenwereld, at the marker {@code plek}. False: nobody went. */
    public static boolean stuur(ServerPlayer gids, String plek) {
        List<ServerPlayer> mee = new ArrayList<>();
        if (!Geestenwereld.in(gids)) {
            for (ServerPlayer q : gids.level().players()) {
                if (q != gids && q.isAlive() && !q.isSpectator() && q.distanceToSqr(gids) <= GAST_STRAAL * GAST_STRAAL && !Geestenwereld.in(q)) {
                    mee.add(q);
                }
            }
        }
        if (!Geestenwereld.stuur(gids, plek)) {
            return false;
        }
        GASTEN.remove(gids.getUUID());
        GeestSpel.binnen(gids);
        int i = 0;
        for (ServerPlayer q : mee) {
            if (Geestenwereld.stuur(q, plek)) {
                // round the guide, a little apart
                double hoek = Math.PI * 2 * (i++) / Math.max(1, mee.size()) + 0.6;
                Vec3 naast = gids.position().add(Math.cos(hoek) * 1.6, 0, Math.sin(hoek) * 1.6);
                q.teleportTo(gids.level(), naast.x, gids.getY(), naast.z, Set.of(), q.getYRot(), q.getXRot(), true);
                GASTEN.put(q.getUUID(), gids.getUUID());
                q.sendSystemMessage(Component.translatable("gui.guhs.einde2_geest_spel.gast.mee", gids.getDisplayName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                GeestSpel.binnen(q);
            }
        }
        if (!mee.isEmpty()) {
            gids.sendSystemMessage(Component.translatable("gui.guhs.einde2_geest_spel.gast.gids", mee.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    /** The guests who came along with this guide (and are still inside). */
    public static List<ServerPlayer> gastenBij(ServerPlayer gids) {
        List<ServerPlayer> uit = new ArrayList<>();
        MinecraftServer server = gids.level().getServer();
        if (server == null) {
            return uit;
        }
        for (Map.Entry<UUID, UUID> e : GASTEN.entrySet()) {
            if (e.getValue().equals(gids.getUUID())) {
                ServerPlayer q = server.getPlayerList().getPlayer(e.getKey());
                if (q != null && Geestenwereld.in(q)) {
                    uit.add(q);
                }
            }
        }
        return uit;
    }

    /** Is this zone of the Geestenwereld open for this player (CONTRACT_E2_B 2.3; a guest: the guide's answer too)? */
    public static boolean zoneOpen(ServerPlayer p, String zone) {
        if (zoneOpenZelf(p, zone)) {
            return true;
        }
        ServerPlayer gids = gids(p);
        return gids != null && zoneOpenZelf(gids, zone);
    }

    // =====================================================================================================================
    // the rule
    // =====================================================================================================================

    /** The zone access of this player alone (no guide). */
    public static boolean zoneOpenZelf(ServerPlayer p, String zone) {
        boolean geest = E2Verhaal.vlag(p, E2Verhaal.WATER, E2Verhaal.V_GEEST) || E2Verhaal.klaar(p, E2Verhaal.WATER);
        boolean lotus = E2Verhaal.vlag(p, E2Verhaal.FINALE, E2Verhaal.V_WITTE_LOTUS);
        boolean eigen = (ARCHIEF.equals(zone) || EILAND_8.equals(zone)) && eigenGebied(p, zone);
        return regel(zone, geest, lotus, E2Verhaal.finaleKlaar(p), eigen);
    }

    /**
     * The rule of 2.3 as a table: the {@code geest} flag of chapter 2 (or chapter 2 done) opens the arrival, Koh's cave and
     * the Hemelvijver; the {@code witte_lotus} flag also the White Lotus, the Top and Fang's path; the finale done every zone;
     * this slice's own two areas (the archive, the eighth island) only by {@code eigen}. Any other name: after the story.
     */
    static boolean regel(String zone, boolean geest, boolean lotus, boolean finale, boolean eigen) {
        if (ARCHIEF.equals(zone) || EILAND_8.equals(zone)) {
            return eigen;
        }
        if (finale) {
            return true;
        }
        if (ZONES_GEEST.contains(zone)) {
            return geest || lotus;
        }
        if (ZONES_LOTUS.contains(zone)) {
            return lotus;
        }
        return false;   // the wood, the tea pavilion and any other name: after the story
    }

    /** The two areas of this slice: the archive after the reiger, the eighth island when its path shows. */
    private static boolean eigenGebied(ServerPlayer p, String zone) {
        if (ARCHIEF.equals(zone)) {
            return GeestLijnen.witteLotus().stap(p) >= GeestLijnen.L_ARCHIEF;
        }
        return GeestLijnen.pad8(p);
    }

    /** Has this player any zone of their own (else they can only be somewhere as a guest)? */
    public static boolean eigenToegang(ServerPlayer p) {
        return zoneOpenZelf(p, "meditatiecirkel") || zoneOpenZelf(p, "fang_pad");
    }

    /** The guide of a guest, while both are inside (null: not a guest). */
    @Nullable
    public static ServerPlayer gids(ServerPlayer p) {
        UUID g = GASTEN.get(p.getUUID());
        MinecraftServer server = p.level().getServer();
        if (g == null || server == null) {
            return null;
        }
        ServerPlayer gids = server.getPlayerList().getPlayer(g);
        return gids != null && Geestenwereld.in(gids) && Geestenwereld.in(p) ? gids : null;
    }

    /** The walled compound of the White Lotus (its wings, court, sanctuary, gate doorway), the part of the island that 2.3 closes. */
    static final List<String> COMPOUND = List.of("lotus_hof", "vleugel_bumi", "vleugel_pakguh", "vleugel_piandao", "vleugel_jeong_jeong",
            "vleugel_miroh", "ledenhof", "heiligdom", "lotusarchief", "poort_doorgang");

    /**
     * Is the player inside the walled compound of the White Lotus? A closed {@code lotus_ruines} only closes that (2.3: "the
     * five wings"): the courtyard before the gate and the road along the island stay open, because the only path from the
     * arrival to Koh's cave that does not cross the Gefluisterwoud (lotus_koh) starts in that courtyard. No compound markers
     * (an older build): the whole zone, as before.
     */
    static boolean inCompound(ServerPlayer p) {
        AABB samen = null;
        for (String id : COMPOUND) {
            AABB d = GeestPlek.doos(id);
            if (d != null) {
                samen = samen == null ? d : samen.minmax(d);
            }
        }
        return samen == null || samen.inflate(2, 4, 2).contains(p.position());
    }

    /** The zone marker a player stands in that is closed for them (null: none). */
    @Nullable
    static String geslotenZone(ServerPlayer p) {
        for (String z : Geestenwereld.ZONES) {
            if (!"meditatiecirkel".equals(z) && GeestPlek.in(p, "zone_" + z) && !zoneOpen(p, z)
                    && (!"lotus_ruines".equals(z) || inCompound(p))) {
                return z;
            }
        }
        for (String z : List.of(ARCHIEF, EILAND_8)) {
            if (GeestPlek.in(p, z) && !zoneOpen(p, z)) {
                return z;
            }
        }
        return null;
    }

    // =====================================================================================================================
    // the mist and the guests (a player tick in the Geestenwereld)
    // =====================================================================================================================

    static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 5 != 0) {
            return;
        }
        if (!Geestenwereld.in(p)) {
            VEILIG.remove(p.getUUID());
            return;
        }
        if (p.isSpectator() || p.getAbilities().instabuild) {
            return;
        }
        // a player with no access at all who is nobody's guest: only the arrival (they came by a door we don't know)
        String dicht = geslotenZone(p);
        if (dicht == null) {
            if (p.onGround()) {
                VEILIG.put(p.getUUID(), p.position());
            }
            return;
        }
        Vec3 terug = VEILIG.get(p.getUUID());
        if (terug == null) {
            terug = Geestenwereld.aankomst(p.level(), p.getX(), p.getZ());
        }
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 24, 0.6, 0.8, 0.6, 0.01);
        p.teleportTo(level, terug.x, terug.y, terug.z, Set.of(), p.getYRot() + 180f, 0f, true);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        p.sendOverlayMessage(Component.translatable(ARCHIEF.equals(dicht) ? "gui.guhs.einde2_geest_spel.mist.archief"
                : EILAND_8.equals(dicht) ? "gui.guhs.einde2_geest_spel.mist.eiland" : "gui.guhs.einde2_geest_spel.mist").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** A guide left the Geestenwereld: guests without access of their own wake up too; the others just stop being guests. */
    static void opDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if (event.getFrom() == Geestenwereld.DIM) {
            GASTEN.remove(p.getUUID());
            MinecraftServer server = p.level().getServer();
            for (Map.Entry<UUID, UUID> e : List.copyOf(GASTEN.entrySet())) {
                if (!e.getValue().equals(p.getUUID())) {
                    continue;
                }
                GASTEN.remove(e.getKey());
                ServerPlayer gast = server == null ? null : server.getPlayerList().getPlayer(e.getKey());
                if (gast != null && Geestenwereld.in(gast) && !eigenToegang(gast)) {
                    gast.sendSystemMessage(Component.translatable("gui.guhs.einde2_geest_spel.gast.wakker", p.getDisplayName())
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    Geestenwereld.terug(gast);
                }
            }
        }
    }

    static void opWeg(PlayerEvent.PlayerLoggedOutEvent event) {
        GASTEN.remove(event.getEntity().getUUID());
        VEILIG.remove(event.getEntity().getUUID());
    }

    /** (tests) make p the guest of gids, or not (null). */
    static void zetGast(ServerPlayer p, @Nullable ServerPlayer gids) {
        if (gids == null) {
            GASTEN.remove(p.getUUID());
        } else {
            GASTEN.put(p.getUUID(), gids.getUUID());
        }
    }

    // =====================================================================================================================
    // the path lanterns of closed zones: dark for that player (Schijn)
    // =====================================================================================================================

    /** Schijn source: every {@code padlantaarn_<zone>_n} of a zone that is closed for the player shows dark. */
    static void lantaarnsUit(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        if (!Geestenwereld.in(p) || p.level().dimension() != Geestenwereld.DIM) {
            return;
        }
        for (String z : Geestenwereld.ZONES) {
            if (zoneOpen(p, z)) {
                continue;
            }
            for (String id : GeestPlek.reeks("padlantaarn_" + z + "_")) {
                BlockPos at = GeestPlek.pos(id);
                if (at == null || !Schijn.dichtbij(p, at)) {
                    continue;
                }
                BlockState echt = p.level().getBlockState(at);
                if (echt.hasProperty(BlockStateProperties.LIT)) {
                    gewenst.put(at, echt.setValue(BlockStateProperties.LIT, false));
                } else if (!echt.isAir()) {
                    gewenst.put(at, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    /** (tests) is this box in the Geestenwereld closed for the player (the same question the mist asks)? */
    static boolean dichtVoor(ServerPlayer p, AABB doos, String zone) {
        return doos.contains(p.position()) && !zoneOpen(p, zone);
    }
}
