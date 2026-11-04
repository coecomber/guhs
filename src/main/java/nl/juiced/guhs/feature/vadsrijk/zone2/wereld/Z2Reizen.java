package nl.juiced.guhs.feature.vadsrijk.zone2.wereld;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.wereld.Later;
import nl.juiced.guhs.feature.vadsrijk.wereld.Littekens;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * The airship line De Haven (zone 1) &lt;-&gt; Kandijkom (zone 2), a route of its own next to zone 1's
 * {@code wereld.WereldReizen} (which stays untouched).
 * <ul>
 *   <li>Kandijkom side: right-click the bell on the dock -&gt; the travel map (g-int round 2; was: back to De Haven).</li>
 *   <li>Haven side: <b>sneak</b> + right-click the Haven bell -&gt; Kandijkom (a plain click keeps flying to Knuffelkamp), or the
 *       npc slice's captain calls {@link #naarKandijkom}.</li>
 * </ul>
 * Gate ({@link #MAG_NAAR_KANDIJKOM}): the zone-1 story is done ({@code zone1_klaar}) and level &gt;= {@link #MIN_LEVEL}. When the gate
 * says no, {@link #WEIGER} runs (default: a short overlay line; the npc slice may replace it with the captain's own text).
 * The first flight plays the opening (snowstorm, ice on the hull, the lights go out, a howl, the needle bursts, the warm geode);
 * {@link #EERSTE_AANKOMST} lets the npc slice play its own cutscene instead.
 */
public final class Z2Reizen {
    public static final int MIN_LEVEL = 20;
    public static final String GEZIEN_EERSTE = "vr_z2_aankomst";
    /** May this player fly to Kandijkom? Default: zone1_klaar + lv 20. */
    public static BiPredicate<ServerPlayer, Boolean> MAG_NAAR_KANDIJKOM = (p, stil) -> Littekens.klaar(p) && Spelers.level(p) >= MIN_LEVEL;
    /** Called when the gate says no (npc decides the text). Second argument: has the player finished zone 1 yet? */
    public static BiConsumer<ServerPlayer, Boolean> WEIGER = (p, zone1) -> p.sendOverlayMessage(
            Component.translatable(zone1 ? "vadsrijk.wereld.z2.luchtschip.te_laag" : "vadsrijk.wereld.z2.luchtschip.nog_niet", MIN_LEVEL)
                    .withStyle(ChatFormatting.AQUA));
    /** Optional: the npc slice's own first-arrival cutscene (called after the teleport instead of the default opening). */
    @Nullable
    public static java.util.function.Consumer<ServerPlayer> EERSTE_AANKOMST = null;

    private Z2Reizen() {
    }

    /** Haven -&gt; Kandijkom (checks the gate). */
    public static boolean naarKandijkom(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl) || RijkScenes.bezig(p)) {
            return false;
        }
        if (!p.getAbilities().instabuild && !MAG_NAAR_KANDIJKOM.test(p, false)) {
            WEIGER.accept(p, Littekens.klaar(p));
            return false;
        }
        Vec3 a = Vec3.atBottomCenterOf(Z2WereldPlekken.DOK_LANDING);
        sl.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.4f, 0.9f);
        p.teleport(new TeleportTransition(sl, a, Vec3.ZERO, 180f, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        SpelerData d = Spelers.data(p);
        boolean eerste = !d.gezien.contains(GEZIEN_EERSTE);
        landOp(p, a, eerste ? 405 : 205);
        if (d.gezien.add(GEZIEN_EERSTE)) {
            if (EERSTE_AANKOMST != null) {
                EERSTE_AANKOMST.accept(p);
            } else {
                opening(p, sl, a);
            }
        } else {
            vlucht(p, sl, a, false);
        }
        return true;
    }

    /** Kandijkom -&gt; De Haven (always allowed). */
    public static boolean naarHaven(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl) || RijkScenes.bezig(p)) {
            return false;
        }
        BlockPos h = WereldPlekken.HAVEN_LANDING;
        Vec3 a = Vec3.atBottomCenterOf(h);
        sl.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.4f, 1.1f);
        p.teleport(new TeleportTransition(sl, a, Vec3.ZERO, 0f, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        Vec3 ver = a.add(0, 50, -130);
        RijkScene s = RijkScene.maak("vr_z2_luchtschip_haven", 180)
                .camera(0, ver, ver.add(0, -10, 40))
                .camera(100, a.add(-20, 22, -50), a.add(0, 4, 0))
                .camera(178, a.add(0, 3, -7), a.add(0, 1, 10))
                .titel("vadsrijk.wereld.luchtschip.titel.haven", "vadsrijk.wereld.z2.luchtschip.onder_haven", 110, 60, 0xFFC6E0)
                .bouw();
        RijkScenes.speel(p, s);
        landOp(p, a, 185);
        sl.playSound(null, p.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.AMBIENT, 0.5f, 1.1f);
        return true;
    }

    /** The plain flight in: over the cloud sea, past the geode rim, onto the dock. */
    static void vlucht(ServerPlayer p, ServerLevel sl, Vec3 a, boolean lang) {
        RijkScene s = RijkScene.maak("vr_z2_luchtschip", 200)
                .camera(0, a.add(-30, 60, 190), a.add(0, 30, -120))
                .camera(110, a.add(25, 26, 70), a.add(0, 10, -60))
                .camera(198, a.add(0, 3, 7), a.add(0, 2, -12))
                .titel("vadsrijk.wereld.z2.luchtschip.titel", "vadsrijk.wereld.z2.luchtschip.onder", 120, 60, 0xBFE8FF)
                .bouw();
        RijkScenes.speel(p, s);
        sl.playSound(null, p.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.AMBIENT, 0.5f, 0.9f);
    }

    /**
     * The opening (first flight, Z2_BRIEF): the ship sails into a snowstorm over the cloud sea, ice floes knock on the hull and
     * the lights go out; a Galmwolf howls from a crystal needle, the needle bursts, and the ship glides into the warm geode.
     */
    static void opening(ServerPlayer p, ServerLevel sl, Vec3 a) {
        Vec3 naald = Vec3.atBottomCenterOf(BlockPos.containing(240, 140, -2300));
        Vec3 storm = a.add(-60, 40, 330);
        RijkScene s = RijkScene.maak("vr_z2_opening", 400)
                .camera(0, storm, storm.add(30, -6, -120))
                .camera(90, storm.add(30, -8, -120), storm.add(60, -14, -240))
                .camera(170, naald.add(-60, 10, 50), naald.add(0, 6, 0))
                .camera(230, naald.add(-35, 2, 30), naald.add(0, 8, 0))
                .camera(300, a.add(10, 40, 90), a.add(0, 5, -60))
                .camera(398, a.add(0, 3, 6), a.add(0, 2, -14))
                .titel("vadsrijk.wereld.z2.opening.titel", "vadsrijk.wereld.z2.opening.onder", 320, 70, 0xBFE8FF)
                .bouw();
        RijkScenes.speel(p, s);
        // the storm: snow around the camera path, ice knocking on the hull, the lights going out
        for (int t = 0; t < 160; t += 10) {
            final int tt = t;
            Later.na(t, () -> {
                Vec3 c = storm.add(30 * tt / 90.0, -8 * tt / 90.0, -120 * tt / 90.0);
                sl.sendParticles(p, ParticleTypes.SNOWFLAKE, true, false, c.x, c.y, c.z - 10, 160, 14, 8, 14, 0.15);
                sl.sendParticles(p, ParticleTypes.WHITE_ASH, true, false, c.x, c.y, c.z - 10, 80, 12, 6, 12, 0.05);
                if (tt % 30 == 0) {
                    geluid(p, SoundEvents.GLASS_HIT, 1.2f, 0.6f);
                    geluid(p, SoundEvents.WOOD_HIT, 1.0f, 0.7f);
                }
            });
        }
        Later.na(120, () -> geluid(p, SoundEvents.BEACON_DEACTIVATE, 1f, 0.5f));
        // the howl and the bursting needle
        Later.na(185, () -> {
            geluid(p, SoundEvents.ELDER_GUARDIAN_CURSE, 1.2f, 0.5f);
            sl.sendParticles(p, ParticleTypes.SNOWFLAKE, true, false, naald.x, naald.y + 8, naald.z, 60, 2, 4, 2, 0.1);
        });
        Later.na(235, () -> {
            geluid(p, SoundEvents.GLASS_BREAK, 2f, 0.5f);
            geluid(p, SoundEvents.AMETHYST_CLUSTER_BREAK, 2f, 0.6f);
            sl.sendParticles(p, ParticleTypes.END_ROD, true, false, naald.x, naald.y + 6, naald.z, 120, 3, 6, 3, 0.3);
            sl.sendParticles(p, ParticleTypes.CLOUD, true, false, naald.x, naald.y + 4, naald.z, 60, 3, 3, 3, 0.1);
        });
        // warm geode, chocomelk
        Later.na(320, () -> {
            geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1.2f);
            geluid(p, SoundEvents.CAMPFIRE_CRACKLE, 1.2f, 1f);
        });
        Later.na(400, () -> p.sendSystemMessage(Component.translatable("vadsrijk.wereld.z2.opening.chocomelk").withStyle(ChatFormatting.GOLD)));
    }

    /** Right-click on a block: the Kandijkom dock bell, or sneak-click on the Haven bell. */
    static boolean klik(ServerPlayer p, BlockPos pos) {
        if (Z2WereldPlekken.DOK_BEL.distManhattan(pos) <= 1) {
            // g-int (round 2): the bell opens the travel map (npc Reiskaart), like the zone-1 bells
            p.level().playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 0.9f);
            nl.juiced.guhs.feature.vadsrijk.npc.Reiskaart.open(p);
            return true;
        }
        if (p.isShiftKeyDown() && WereldPlekken.HAVEN_BEL.distManhattan(pos) <= 1) {
            naarKandijkom(p);
            return true;
        }
        return false;
    }

    /**
     * Puts the player back on the landing spot when the scene ends (and on the way): while the camera is far away the client may
     * not have the chunks round the body yet and let it drop through them.
     */
    static void landOp(ServerPlayer p, Vec3 a, int ticks) {
        for (int t = 20; t <= ticks; t += 40) {
            Later.na(Math.min(t, ticks), () -> zet(p, a));
        }
        Later.na(ticks, () -> zet(p, a));
    }

    private static void zet(ServerPlayer p, Vec3 a) {
        if (p.isRemoved() || !(p.level() instanceof ServerLevel sl) || p.position().distanceToSqr(a) < 0.5) {
            return;
        }
        p.teleport(new TeleportTransition(sl, a, Vec3.ZERO, p.getYRot(), p.getXRot(), TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
    }

    /** A sound at the player (heard wherever the scene camera is). */
    static void geluid(ServerPlayer p, net.minecraft.sounds.SoundEvent s, float vol, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.AMBIENT, vol, pitch);
    }
}
