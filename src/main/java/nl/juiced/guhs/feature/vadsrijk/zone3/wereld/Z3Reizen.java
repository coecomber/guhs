package nl.juiced.guhs.feature.vadsrijk.zone3.wereld;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.wereld.Later;

/**
 * The airship line to zone 3 "De Schemerwortel": De Haven (zone 1) / Kandijkom (zone 2) -&gt; the dock at the tip of Honingtak.
 * <ul>
 *   <li>Gate {@link #MAG_NAAR_SCHEMERWORTEL}: {@code zone2_klaar} + generic level {@link #MIN_LEVEL} (creative bypasses);
 *       {@link #WEIGER} runs when it says no (the npc slice may set the captain's own text).</li>
 *   <li>The captains offer it through the travel map / travel choices (npc {@code Reiskaart} id {@code schemerwortel},
 *       {@code QuestMotor} action {@code z3_vlieg}); the Honingtak dock bell opens the travel map (the way back).</li>
 *   <li>The first flight plays the opening film (ZONE3_PLAN §7 z3_hq01: along an endless bark wall upwards, through the clouds
 *       into the golden crown, then down along the trunk into the roots, two grey-blue eyes open; "Hij leeft nog... maar niet
 *       lang meer."), unless the npc slice sets {@link #EERSTE_AANKOMST}. Later flights: a short ~8 s flight.</li>
 * </ul>
 */
public final class Z3Reizen {
    public static final int MIN_LEVEL = 30;
    public static final String GEZIEN_EERSTE = "vr_z3_aankomst";
    public static final String VLAG_ZONE2 = "zone2_klaar";
    /** May this player fly to De Schemerwortel? Default: zone2_klaar + level 30. Second argument: silent check. */
    public static BiPredicate<ServerPlayer, Boolean> MAG_NAAR_SCHEMERWORTEL = (p, stil) -> zone2Klaar(p) && Spelers.level(p) >= MIN_LEVEL;
    /** Called when the gate says no. Second argument: has the player finished zone 2? */
    public static BiConsumer<ServerPlayer, Boolean> WEIGER = (p, zone2) -> p.sendOverlayMessage(
            Component.translatable(zone2 ? "vadsrijk.wereld.z3.luchtschip.te_laag" : "vadsrijk.wereld.z3.luchtschip.nog_niet", MIN_LEVEL)
                    .withStyle(ChatFormatting.GOLD));
    /** Optional: the npc slice's own first-arrival cutscene (instead of the default opening). */
    @Nullable
    public static Consumer<ServerPlayer> EERSTE_AANKOMST = null;

    private Z3Reizen() {
    }

    public static boolean zone2Klaar(ServerPlayer p) {
        return Questlog.vlag(p, VLAG_ZONE2);
    }

    /** Has this player been to zone 3 (the first flight happened)? */
    public static boolean bereikt(ServerPlayer p) {
        return Spelers.data(p).gezien.contains(GEZIEN_EERSTE);
    }

    /** Null when the line is open for this player, else the lang key of why not (arg: the player's level). */
    @Nullable
    public static String slot(ServerPlayer p) {
        if (bereikt(p) || p.getAbilities().instabuild) {
            return null;
        }
        if (!zone2Klaar(p)) {
            return "vadsrijk.npc.reis.slot.schemerwortel_verhaal";
        }
        return Spelers.level(p) < MIN_LEVEL ? "vadsrijk.npc.reis.slot.schemerwortel_level" : null;
    }

    /** Fly to the Honingtak dock (checks the gate). */
    public static boolean naarHoningtak(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl) || RijkScenes.bezig(p)) {
            return false;
        }
        if (!p.getAbilities().instabuild && !bereikt(p) && !MAG_NAAR_SCHEMERWORTEL.test(p, false)) {
            WEIGER.accept(p, zone2Klaar(p));
            return false;
        }
        Vec3 a = Vec3.atBottomCenterOf(Z3WereldPlekken.DOK_LANDING);
        sl.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.4f, 0.8f);
        p.teleport(new TeleportTransition(sl, a, Vec3.ZERO, 90f, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        SpelerData d = Spelers.data(p);
        boolean eerste = !d.gezien.contains(GEZIEN_EERSTE);
        landOp(p, a, eerste ? 525 : 165);
        if (d.gezien.add(GEZIEN_EERSTE)) {
            if (EERSTE_AANKOMST != null) {
                EERSTE_AANKOMST.accept(p);
            } else {
                opening(p, sl, a);
            }
        } else {
            vlucht(p, sl, a);
        }
        return true;
    }

    /** The short flight in: over the cloud sea towards the tree, along the branch to the mast. */
    static void vlucht(ServerPlayer p, ServerLevel sl, Vec3 a) {
        // (P-VERHUIS) the dock is at the north-west tip of the Honingtak ring (revamp): come in from outside, along the outward line
        Vec3 uit = new Vec3(a.x - Z3WereldPlekken.STAM_X, 0, a.z - Z3WereldPlekken.STAM_Z).normalize();
        Vec3 zij = new Vec3(-uit.z, 0, uit.x);
        RijkScene s = RijkScene.maak("vr_z3_luchtschip", 160)
                .camera(0, a.add(uit.scale(260)).add(zij.scale(60)).add(0, 40, 0), a.add(uit.scale(-200)).add(0, 60, 0))
                .camera(90, a.add(uit.scale(70)).add(zij.scale(20)).add(0, 18, 0), a.add(uit.scale(-60)).add(0, 10, 0))
                .camera(158, a.add(uit.scale(7)).add(0, 3, 0), a.add(uit.scale(-12)).add(0, 2, 0))
                .titel("vadsrijk.wereld.z3.luchtschip.titel", "vadsrijk.wereld.z3.luchtschip.onder", 90, 60, 0xFFD27A)
                .bouw();
        RijkScenes.speel(p, s);
        sl.playSound(null, p.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.AMBIENT, 0.5f, 1.0f);
    }

    /** The opening film (doorlopend, not clickable). */
    static void opening(ServerPlayer p, ServerLevel sl, Vec3 a) {
        int sx = Z3WereldPlekken.STAM_X, sz = Z3WereldPlekken.STAM_Z;
        Vec3 stam = new Vec3(sx + 0.5, 0, sz + 0.5);
        Vec3 nest = Vec3.atBottomCenterOf(Z3WereldPlekken.plek("arena_vahoegnir"));
        Vec3 hart = Vec3.atBottomCenterOf(Z3WereldPlekken.plek("hartslag_zaal"));
        RijkScene s = RijkScene.maak("vr_z3_opening", 520)
                // the bark wall: the ship sails along it, ever higher
                .camera(0, stam.add(-125, 96, 70), stam.add(-30, 120, 10))
                .camera(80, stam.add(-120, 150, 40), stam.add(-30, 175, -10))
                // through the clouds: the crown in gold, imkerguhs waving
                .camera(150, stam.add(-180, 250, 90), nest.add(0, 0, 0))
                .camera(220, stam.add(-60, 305, 160), nest)
                // down along the trunk into the dusk and deep into the roots
                .camera(290, stam.add(-170, 190, 10), stam.add(-30, 120, 0))
                .camera(350, stam.add(-125, 100, 5), stam.add(-35, 80, 0))
                .camera(420, hart.add(-30, 18, 10), hart.add(0, 12, -30))
                .camera(455, hart.add(-12, 8, 0), hart.add(0, 10, -30))
                // and back up: the dock on Honingtak
                .camera(470, a.add(new Vec3(a.x - sx, 0, a.z - sz).normalize().scale(40)).add(0, 25, 0), a.add(0, 3, 0))
                .camera(518, a.add(new Vec3(a.x - sx, 0, a.z - sz).normalize().scale(7)).add(0, 3, 0), stam.add(0, a.y + 2, 0))
                .titel("vadsrijk.wereld.z3.opening.titel", "vadsrijk.wereld.z3.opening.onder", 160, 80, 0xFFD27A)
                .bouw();
        RijkScenes.speel(p, s);
        Vec3 ogen = hart.add(0, 12, -32);
        for (int t = 0; t < 140; t += 12) {
            final int tt = t;
            Later.na(t, () -> {
                Vec3 c = stam.add(-110 + tt * 0.1, 80 + tt * 0.6, 40 - tt * 0.3);
                sl.sendParticles(p, ParticleTypes.CHERRY_LEAVES, true, false, c.x, c.y, c.z, 40, 10, 8, 10, 0.02);
            });
        }
        Later.na(150, () -> {
            geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 1.3f);
            geluid(p, SoundEvents.BEEHIVE_WORK, 1.2f, 1.0f);
        });
        Later.na(300, () -> geluid(p, SoundEvents.WOOD_BREAK, 1.2f, 0.4f));
        // two grey-blue eyes open in the dark (the Schimmelkolos), the creak and the voice
        Later.na(430, () -> {
            for (int k = -1; k <= 1; k += 2) {
                sl.sendParticles(p, ParticleTypes.SOUL_FIRE_FLAME, true, false, ogen.x + k * 2.2, ogen.y, ogen.z, 18, 0.25, 0.15, 0.25, 0.0);
                sl.sendParticles(p, ParticleTypes.GLOW, true, false, ogen.x + k * 2.2, ogen.y, ogen.z, 10, 0.3, 0.2, 0.3, 0.0);
            }
            geluid(p, SoundEvents.WARDEN_HEARTBEAT, 1.5f, 0.6f);
        });
        Later.na(445, () -> {
            geluid(p, SoundEvents.WOOD_BREAK, 1.4f, 0.3f);
            p.sendSystemMessage(Component.translatable("vadsrijk.wereld.z3.opening.stem").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        });
        Later.na(520, () -> p.sendSystemMessage(Component.translatable("vadsrijk.wereld.z3.opening.welkom").withStyle(ChatFormatting.GOLD)));
    }

    /** Right-click on a block: the Honingtak dock bell opens the travel map (the way back). */
    static boolean klik(ServerPlayer p, BlockPos pos) {
        if (Z3WereldPlekken.DOK_BEL.distManhattan(pos) <= 1) {
            p.level().playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 0.8f);
            nl.juiced.guhs.feature.vadsrijk.npc.Reiskaart.open(p);
            return true;
        }
        return false;
    }

    /** Puts the player back on the landing spot during/after a long scene (the chunks round the body may not be loaded). */
    static void landOp(ServerPlayer p, Vec3 a, int ticks) {
        for (int t = 20; t <= ticks; t += 40) {
            Later.na(Math.min(t, ticks), () -> zet(p, a));
        }
        Later.na(ticks, () -> zet(p, a));
    }

    static void zet(ServerPlayer p, Vec3 a) {
        if (p.isRemoved() || !(p.level() instanceof ServerLevel sl) || p.position().distanceToSqr(a) < 0.5) {
            return;
        }
        p.teleport(new TeleportTransition(sl, a, Vec3.ZERO, p.getYRot(), p.getXRot(), TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
    }

    static void geluid(ServerPlayer p, SoundEvent s, float vol, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.AMBIENT, vol, pitch);
    }
}
