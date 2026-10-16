package nl.juiced.guhs.feature.einde2.h2water;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.einde2.castschurk.E2CastSchurkFeature;
import nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature;
import nl.juiced.guhs.feature.einde2.zwevers.OtterPinguinguhEntity;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2 h2_water: who stands where in the Waterstam besides the residents of the build ({@code K/Bewoners}).
 * <ul>
 *   <li><b>Residents per step</b> ({@link #markeer}, a {@code Bewoners.BIJ_MAKEN} listener): the moon fish of the oasis is
 *       there until Zhao takes it, Yue-guh in the palace until she becomes the moon, Zhao and his five guards on the ship
 *       until the wave (CONTRACT_E2_B 4.3: main's {@code Zicht.alleenBij}).</li>
 *   <li><b>The extra cast</b> ({@link #CAST}): one entity per copy for every player whose step is in its range: the moon fish
 *       back in the pond and Yue-guh as the moon spirit on her islet, after the chapter's moon scene. Made like a resident
 *       (missing twice, 60 ticks apart, then made).</li>
 *   <li><b>Actors of one player</b> (Zicht.alleenVoor, never saved: a leftover is thrown away when its chunk loads): the
 *       otter-pinguinguh sled at the top of the sled run ({@link #slee}), the gondola at the basin ({@link #gondel}) and
 *       Zuka in the blizzard.</li>
 * </ul>
 */
public final class Toneel {
    /** Entity tag of every actor of one player (never kept: thrown away when it is loaded again). */
    public static final String EIGEN = "guhs_einde2_h2_water_eigen";
    /** Persistent data: the id of an extra cast member. */
    public static final String CAST_ID = "guhs_einde2_h2_water_cast";
    /** The NpcRollen spot of Zuka in the storm and of the moon spirit (E2H2WaterRol). */
    public static final String PLEK_ZUKA = E2H2Water.STRUCTUUR + "/storm_zuka", PLEK_MAAN = E2H2Water.STRUCTUUR + "/yue_maan";

    /** A member of the extra cast: the marker it stands at, the steps it is there for (van..tot inclusive), how it is made. */
    record Rol(String id, String plek, int van, int tot, BiFunction<ServerLevel, Vec3, Entity> maak) {
    }

    static final List<Rol> CAST = List.of(
            new Rol("tui_terug", "vis_maan", E2H2Water.GOLF, 99, (level, plek) -> {
                CastWezen vis = E2CastSchurkFeature.MAANVIS.get().create(level, EntitySpawnReason.STRUCTURE);
                if (vis != null) {
                    vis.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
                    vis.zetStil(false);
                    vis.zetHonk(4);
                }
                return vis;
            }),
            new Rol("yue_maan", "yue_maan", E2H2Water.GOLF, 99, (level, plek) -> {
                GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
                if (npc != null) {
                    npc.setKind(GuhNpcEntity.Kind.YUE_GUH_MAAN);
                    npc.roleData.putString(NpcRollen.PLEK, PLEK_MAAN);
                    npc.snapTo(plek.x, plek.y, plek.z, 180f, 0f);
                    npc.setYHeadRot(180f);
                    npc.setYBodyRot(180f);
                }
                return npc;
            }));

    /** Per copy and cast member: since when it has been missing (game time). */
    private static final Map<String, Long> MIST = new ConcurrentHashMap<>();

    /** The actors of one player. */
    static final class Eigen {
        @Nullable
        UUID otter, boot, zuka;
    }

    private static final Map<UUID, Eigen> EIGEN_VAN = new ConcurrentHashMap<>();

    private Toneel() {
    }

    // =====================================================================================================================
    // residents per step
    // =====================================================================================================================

    /** {@code Bewoners.BIJ_MAKEN}: a resident of the Waterstam that is only there for some steps of the chapter. */
    static void markeer(Entity e, String plek) {
        if (!plek.startsWith(E2H2Water.STRUCTUUR + "/")) {
            return;
        }
        String id = plek.substring(E2H2Water.STRUCTUUR.length() + 1);
        String lijn = E2H2WaterFeature.LIJN.id();
        switch (id) {
            case "vis_maan" -> Zicht.alleenBij(e, lijn, 0, E2H2Water.MAANVIS);
            case "yue" -> Zicht.alleenBij(e, lijn, 0, E2H2Water.VIS_TERUG);
            case "zhao", "sluip_wacht_1", "sluip_wacht_2", "sluip_wacht_3", "sluip_wacht_4", "sluip_wacht_5" ->
                    Zicht.alleenBij(e, lijn, 0, E2H2Water.GOLF);
            default -> {
            }
        }
    }

    /** Residents that stood before this chapter existed get their marks too (once in a while, when a player is near). */
    private static void markeerOudeBewoners(ServerLevel level, E2H2Water.Oord o, ServerPlayer p) {
        AABB rond = p.getBoundingBox().inflate(64);
        for (Entity e : level.getEntities((Entity) null, rond, e -> e.getPersistentData().contains(Bewoners.TAG) && !e.entityTags().contains(Zicht.MERK))) {
            String tag = e.getPersistentData().getStringOr(Bewoners.TAG, "");
            String voor = "einde2_" + E2H2Water.STRUCTUUR + "_";
            int at = tag.indexOf('@');
            if (tag.startsWith(voor) && at > voor.length()) {
                markeer(e, E2H2Water.STRUCTUUR + "/" + tag.substring(voor.length(), at));
            }
        }
    }

    // =====================================================================================================================
    // once a second
    // =====================================================================================================================

    static void seconde(ServerPlayer p, E2H2Water.Oord o, int stap) {
        ServerLevel level = o.level();
        if ((p.tickCount / 20) % 10 == 3) {
            markeerOudeBewoners(level, o, p);
        }
        long nu = level.getGameTime();
        for (Rol r : CAST) {
            BlockPos pos = o.wereld(r.plek());
            if (pos == null || pos.distToCenterSqr(p.position()) > 48 * 48) {
                continue;
            }
            AABB rond = new AABB(pos).inflate(8);
            if (!Bewoners.geladen(level, rond)) {
                continue;
            }
            String sleutel = r.id() + "@" + o.oorsprong().asLong();
            boolean er = !level.getEntities((Entity) null, rond, e -> sleutel.equals(e.getPersistentData().getStringOr(CAST_ID, ""))).isEmpty();
            if (er) {
                MIST.remove(sleutel);
                continue;
            }
            Long sinds = MIST.putIfAbsent(sleutel, nu);
            if (sinds == null || nu - sinds < Bewoners.BEVESTIG) {
                continue;
            }
            MIST.remove(sleutel);
            Entity e = r.maak().apply(level, Vec3.atBottomCenterOf(pos));
            if (e == null) {
                continue;
            }
            e.getPersistentData().putString(CAST_ID, sleutel);
            e.setInvulnerable(true);
            if (e instanceof net.minecraft.world.entity.Mob m) {
                m.setPersistenceRequired();
            }
            Zicht.alleenBij(e, E2H2WaterFeature.LIJN.id(), r.van(), r.tot());
            level.addFreshEntity(e);
        }
        // the actors of this player: only at their own step
        Eigen eigen = EIGEN_VAN.get(p.getUUID());
        if (eigen != null) {
            if (stap != E2H2Water.SLEE && eigen.otter != null) {
                weg(level, eigen.otter);
                eigen.otter = null;
            }
            if (stap != E2H2Water.GONDEL && eigen.boot != null) {
                weg(level, eigen.boot);
                eigen.boot = null;
            }
            if (stap != E2H2Water.STORM && eigen.zuka != null) {
                weg(level, eigen.zuka);
                eigen.zuka = null;
            }
        }
        if (stap == E2H2Water.STORM) {
            stormZuka(p, o);
        }
    }

    private static Eigen eigen(ServerPlayer p) {
        return EIGEN_VAN.computeIfAbsent(p.getUUID(), u -> new Eigen());
    }

    @Nullable
    private static Entity levend(ServerLevel level, @Nullable UUID id) {
        Entity e = id == null ? null : level.getEntity(id);
        return e != null && e.isAlive() ? e : null;
    }

    private static void weg(ServerLevel level, @Nullable UUID id) {
        Entity e = levend(level, id);
        if (e != null) {
            e.ejectPassengers();
            e.discard();
        }
    }

    /** Takes every actor of this player away (they left, logged out, the chapter is done or wiped). */
    static void weg(ServerPlayer p) {
        Eigen eigen = EIGEN_VAN.remove(p.getUUID());
        if (eigen == null) {
            return;
        }
        for (ServerLevel level : p.level().getServer().getAllLevels()) {
            weg(level, eigen.otter);
            weg(level, eigen.boot);
            weg(level, eigen.zuka);
        }
    }

    /** One player's actor: seen by them alone, never kept. */
    private static void vanSpeler(Entity e, ServerPlayer p) {
        e.addTag(EIGEN);
        Zicht.alleenVoor(e, p.getUUID());
        e.setInvulnerable(true);
    }

    /** A leftover actor of one player that is loaded from disk (the server stopped with it there) is thrown away. */
    static void opJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.loadedFromDisk() && event.getEntity().entityTags().contains(EIGEN)) {
            event.setCanceled(true);
        }
    }

    // =====================================================================================================================
    // the sled
    // =====================================================================================================================

    /** At the sled step: an otter-pinguinguh waits at the top of the run (empty hand: get on); down at the finish: done. */
    static void slee(ServerPlayer p, E2H2Water.Oord o) {
        ServerLevel level = o.level();
        Vec3 start = o.midden("slee_start"), finish = o.midden("slee_finish");
        if (start == null || finish == null) {
            return;
        }
        Eigen eigen = eigen(p);
        Entity otter = levend(level, eigen.otter);
        if (p.getVehicle() instanceof OtterPinguinguhEntity rijdt && rijdt.isSlee()) {
            if (finish.distanceToSqr(p.position()) < SLEE_FINISH * SLEE_FINISH) {
                p.stopRiding();
                E2H2Water.sleeKlaar(p);
                weg(level, eigen.otter);
                eigen.otter = null;
            }
            return;
        }
        E2H2Water.wijs(p, o, "slee_start");
        if (otter == null && start.distanceToSqr(p.position()) < 28 * 28) {
            OtterPinguinguhEntity nieuw = E2ZweversFeature.OTTER_PINGUINGUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (nieuw == null) {
                return;
            }
            nieuw.snapTo(start.x, start.y, start.z, 90f, 0f);
            nieuw.laatRijden(true);
            nieuw.setHonk(BlockPos.containing(start), 2);
            nieuw.setCustomName(Component.translatable("entity.guhs.einde2_h2_water_slee_otter"));
            vanSpeler(nieuw, p);
            nl.juiced.guhs.world.WildeDieren.markeer(nieuw);
            level.addFreshEntity(nieuw);
            eigen.otter = nieuw.getUUID();
        } else if (otter != null && otter.distanceToSqr(start) > 12 * 12 && !otter.isVehicle()) {
            otter.teleportTo(start.x, start.y, start.z);   // (it wandered off, or the ride ended halfway: back to the top)
        }
    }

    /** How near the snow heap at the bottom counts as the finish (blocks; on the otter, or just got off it). */
    static final double SLEE_FINISH = 7.0;

    /**
     * fix2-h2: somebody got off an otter-pinguinguh. A player of the sled step who got off their own sled near the finish
     * (stopped just short of it) has done it; anywhere else on the run (fell off, got off halfway) they are back at the top
     * two seconds later, with a kind line: a quick retry instead of the long climb up the wall.
     */
    static void afgestapt(OtterPinguinguhEntity otter, ServerPlayer p) {
        if (!E2H2Water.aanZet(p, E2H2Water.SLEE) || !otter.entityTags().contains(EIGEN) || Cutscenes.bezig(p)) {
            return;
        }
        E2H2Water.Oord o = E2H2Water.oordVan(p);
        Vec3 start = o == null ? null : o.midden("slee_start"), finish = o == null ? null : o.midden("slee_finish");
        if (start == null || finish == null) {
            return;
        }
        if (finish.distanceToSqr(p.position()) < SLEE_FINISH * SLEE_FINISH) {
            E2H2Water.sleeKlaar(p);
            return;
        }
        nl.juiced.guhs.feature.einde2.verhaal.StapHint.later(p, 40, s -> {
            if (!E2H2Water.aanZet(s, E2H2Water.SLEE) || s.getVehicle() != null || Cutscenes.bezig(s) || start.distanceToSqr(s.position()) < 6 * 6) {
                return;
            }
            Duwtje.terug(s, o.level().dimension(), start, 90f);
            E2H2Water.balk(s, "quest.guhs.einde2_h2_water.slee.opnieuw");
        });
    }

    /** Somebody got on an otter-pinguinguh: the sled tune for a player at the sled step. */
    static void opgestapt(OtterPinguinguhEntity otter, ServerPlayer p) {
        if (E2H2Water.aanZet(p, E2H2Water.SLEE) && otter.entityTags().contains(EIGEN)) {
            E2Audio.stinger(p, "deuntje.slee");
            E2H2Water.balk(p, "quest.guhs.einde2_h2_water.slee.op");
        }
    }

    // =====================================================================================================================
    // the gondola
    // =====================================================================================================================

    /** At the gondola step: a gondola waits in the basin; past the three stops on it (pushed with waterstroom): done. */
    static void gondel(ServerPlayer p, E2H2Water.Oord o) {
        ServerLevel level = o.level();
        Eigen eigen = eigen(p);
        Entity boot = levend(level, eigen.boot);
        if (boot != null && p.getVehicle() == boot) {
            Vec3 naaste = null;
            int naasteNr = 0;
            for (int i = 1; i <= E2H2Water.HALTES; i++) {
                Vec3 h = o.midden("gondel_halte_" + i);
                if (h != null && Math.hypot(h.x - boot.getX(), h.z - boot.getZ()) < 6.5) {
                    E2H2Water.halte(p, i);
                }
                // fix2-h2: every stop still to pass shows a column of light; the nearest one is named over the hotbar
                if (h != null && !E2H2Water.lijn().vlag(p, E2H2Water.V_HALTE + i)) {
                    for (int dy = 1; dy <= 6; dy += 1) {
                        Schijn.deeltjes(p, ParticleTypes.END_ROD, h.add(0, dy, 0), 1, 0.05, 0.0);
                    }
                    if (naaste == null || h.distanceToSqr(p.position()) < naaste.distanceToSqr(p.position())) {
                        naaste = h;
                        naasteNr = i;
                    }
                }
            }
            if (naaste != null && E2H2Water.aanZet(p, E2H2Water.GONDEL) && (p.tickCount / 20) % 3 == 0) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_h2_water.hint.halte_balk",
                        Component.translatable("gui.guhs.einde2_h2_water.halte." + naasteNr),
                        nl.juiced.guhs.feature.einde2.verhaal.Richting.van(p.position(), naaste))
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
            }
            return;
        }
        if (boot != null && boot.distanceToSqr(p) < 48 * 48) {
            Schijn.deeltjes(p, ParticleTypes.GLOW, boot.position().add(0, 2.0, 0), 4, 0.25, 0.02);   // (fix2-h2: your gondola is there)
        } else {
            E2H2Water.wijs(p, o, "gondel_start");
        }
        BlockPos start = o.wereld("gondel_start");
        if (boot == null && start != null && start.distToCenterSqr(p.position()) < 24 * 24) {
            Vec3 water = water(level, start);
            EntityType<? extends Entity> type = EntityType.OAK_BOAT;
            Entity nieuw = type.create(level, EntitySpawnReason.TRIGGERED);
            if (water == null || nieuw == null) {
                return;
            }
            nieuw.snapTo(water.x, water.y, water.z, 180f, 0f);
            nieuw.setCustomName(Component.translatable("entity.guhs.einde2_h2_water_gondel"));
            vanSpeler(nieuw, p);
            level.addFreshEntity(nieuw);
            eigen.boot = nieuw.getUUID();
        }
    }

    /** Is this the gondola of this player (waterstroom pushes it)? */
    static boolean isGondel(ServerPlayer p, @Nullable Entity e) {
        Eigen eigen = EIGEN_VAN.get(p.getUUID());
        return e != null && eigen != null && e.getUUID().equals(eigen.boot);
    }

    /** The nearest water block with air above it within four blocks of the quay (where the gondola floats). */
    @Nullable
    static Vec3 water(ServerLevel level, BlockPos kade) {
        Vec3 beste = null;
        double afstand = Double.MAX_VALUE;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    BlockPos b = kade.offset(dx, dy, dz);
                    if (level.getFluidState(b).is(Fluids.WATER) && level.getBlockState(b.above()).isAir()) {
                        double d = dx * dx + dz * dz;
                        if (d < afstand) {
                            afstand = d;
                            beste = new Vec3(b.getX() + 0.5, b.getY() + 0.95, b.getZ() + 0.5);
                        }
                    }
                }
            }
        }
        return beste;
    }

    // =====================================================================================================================
    // Zuka in the storm
    // =====================================================================================================================

    private static void stormZuka(ServerPlayer p, E2H2Water.Oord o) {
        ServerLevel level = o.level();
        Eigen eigen = eigen(p);
        if (levend(level, eigen.zuka) != null || !o.in(p.position(), "sneeuwvlakte", 10)) {
            return;
        }
        Vec3 start = o.midden("sneeuwstorm_start");
        if (start == null) {
            return;
        }
        GuhNpcEntity zuka = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (zuka == null) {
            return;
        }
        zuka.setKind(GuhNpcEntity.Kind.ZUKA_BALLING);
        zuka.roleData.putString(NpcRollen.PLEK, PLEK_ZUKA);
        zuka.snapTo(start.x + 2.5, start.y, start.z + 1.5, 120f, 0f);
        vanSpeler(zuka, p);
        level.addFreshEntity(zuka);
        eigen.zuka = zuka.getUUID();
    }
}
