package nl.juiced.guhs.feature.bio.bouwwolk1;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.sterrenwacht.Buiten;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.GuhTime;

/**
 * The ballonvaarder-guh (NPC BALLONVAARDERGUH) in his booth at the Luchtballon-haven: one ride per game day per player
 * with the {@link HavenBallonEntity} at the jetty, down to the meadow below the haven. He only lets the balloon go when
 * it is at its mooring, there is a landing spot and the way to it is free; otherwise he says why and the ride of that
 * day is not used. He keeps his haven in order: he remembers where his balloon is moored and moors a new one when it
 * is gone.
 * <p>
 * Per player (GuhQuests.saved): {@value #DAG} = the game day of the last ride + 1 (0: never).
 * A ride to another island is not offered: there is no way to know a safe landing spot on an island that may not be
 * loaded, so the haven only goes down.
 */
public final class Ballonvaarder implements NpcRole {
    public static final String DAG = "guhs_bio_bouw_wolk1_vaart_dag";
    /** Answer ids. */
    public static final int JA = 1, NEE = 2;
    /** roleData: the mooring of his balloon and the way it looks. */
    private static final String THUIS = "BallonThuis", YAW = "BallonYaw", KLEUR = "BallonKleur", WEG = "BallonWeg";
    private static final String P = "quest.guhs.luchtballon_haven.";

    /** The game day (a ride per day: it does not matter when in the day). */
    public static long dag(Level level) {
        return Math.floorDiv(GuhTime.dayTime(level), 24000L);
    }

    public static boolean heeftGevaren(ServerPlayer speler) {
        return GuhQuests.saved(speler).getLongOr(DAG, 0L) == dag(speler.level()) + 1;
    }

    static void zetGevaren(ServerPlayer speler, boolean gevaren) {
        if (gevaren) {
            GuhQuests.saved(speler).putLong(DAG, dag(speler.level()) + 1);
        } else {
            GuhQuests.saved(speler).remove(DAG);
        }
    }

    public static Component naam() {
        return Component.translatable("entity.guhs.guh_npc.ballonvaarderguh");
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer speler) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.9f, 1.1f);
        Bewijs.geef(speler, Bewijs.HAVEN_GEVONDEN);
        if (heeftGevaren(speler)) {
            Praat.open(speler, npc, null, P + "al", new Object[0]);
        } else {
            Praat.open(speler, npc, null, P + "hallo", new Object[0], new Praat.Optie(JA, "gui.guhs.luchtballon_haven.optie.ja"),
                    new Praat.Optie(NEE, "gui.guhs.luchtballon_haven.optie.nee"));
        }
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer speler, int optie) {
        if (optie == NEE) {
            Praat.open(speler, npc, null, P + "nee", new Object[0]);
        } else if (optie == JA) {
            String waarom = vaar(npc, speler);
            if (waarom == null) {
                Praat.sluit(speler);
                Buiten.zeg(speler, naam(), P + "vertrek");
            } else {
                Praat.open(speler, npc, null, P + waarom, new Object[0]);
            }
        }
    }

    /**
     * The ride, if everything is right: returns null when the player is off, else why not ("al", "weg", "geblokkeerd":
     * the last part of the text key). Only a ride that really starts uses up the day.
     */
    @Nullable
    public static String vaar(GuhNpcEntity npc, ServerPlayer speler) {
        if (heeftGevaren(speler)) {
            return "al";
        }
        if (!(npc.level() instanceof ServerLevel level)) {
            return "weg";
        }
        HavenBallonEntity ballon = ballon(level, npc);
        if (ballon == null || ballon.onderweg() || !ballon.getPassengers().isEmpty() || speler.isPassenger()) {
            return "weg";
        }
        Vec3 landing = HavenBallonEntity.Landing.zoek(level, ballon.thuis(), ballon.uit());
        if (landing == null || !HavenBallonEntity.Landing.wegVrij(level, ballon.thuis(), ballon.uit(), landing)) {
            return "geblokkeerd";
        }
        if (!ballon.vaar(speler, landing)) {
            return "weg";
        }
        zetGevaren(speler, true);
        return null;
    }

    /** Landed on the meadow: the proof of the first ride. */
    static void geland(ServerPlayer speler) {
        Buiten.zeg(speler, naam(), P + "geland");
        Bewijs.geef(speler, Bewijs.HAVEN_VAART);
        speler.level().playSound(null, speler.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    /** The ride did not reach the meadow (the landing spot got blocked, or it was broken off): the ride is given back. */
    static void terugGebracht(ServerPlayer speler, String key) {
        zetGevaren(speler, false);
        Buiten.zeg(speler, naam(), key);
    }

    /**
     * A player in a flying haven balloon leaves the game (logging out; the server stopping logs everybody out): before
     * the player is saved they stand on the jetty and the balloon is at its mooring. Returns whether that happened.
     */
    public static boolean uitloggen(ServerPlayer speler) {
        if (speler.getVehicle() instanceof HavenBallonEntity ballon && ballon.breekAf()) {
            zetGevaren(speler, false);
            return true;
        }
        return false;
    }

    /** His balloon: the nearest haven balloon moored within 24 blocks (the template moors it 16 blocks from his booth). */
    @Nullable
    static HavenBallonEntity ballon(ServerLevel level, GuhNpcEntity npc) {
        List<HavenBallonEntity> lijst = level.getEntitiesOfClass(HavenBallonEntity.class, new AABB(npc.blockPosition()).inflate(24, 64, 24),
                b -> !b.isRemoved() && !b.isDeco() && b.thuis().distanceToSqr(npc.position()) < 24 * 24);
        HavenBallonEntity beste = null;
        // (once he knows his mooring, only the balloon moored there is his: another haven's balloon close by is not)
        BlockPos eigen = npc.roleData.contains(THUIS) ? BlockPos.of(npc.roleData.getLongOr(THUIS, 0L)) : null;
        for (HavenBallonEntity b : lijst) {
            if (eigen != null && b.thuisBlok().distManhattan(eigen) > 2) {
                continue;
            }
            if (beste == null || b.thuis().distanceToSqr(npc.position()) < beste.thuis().distanceToSqr(npc.position())) {
                beste = b;
            }
        }
        return beste;
    }

    /** Every ten seconds: remember the mooring; no balloon there any more: moor a new one. */
    @Override
    public void tick(GuhNpcEntity npc) {
        if ((npc.tickCount + npc.getId()) % 200 != 0 || !(npc.level() instanceof ServerLevel level)) {
            return;
        }
        onderhoud(level, npc);
    }

    /** Returns the balloon it moored anew, or null. */
    @Nullable
    static HavenBallonEntity onderhoud(ServerLevel level, GuhNpcEntity npc) {
        HavenBallonEntity ballon = ballon(level, npc);
        CompoundTag d = npc.roleData;
        if (ballon != null) {
            if (!ballon.onderweg()) {
                d.putLong(THUIS, ballon.thuisBlok().asLong());
                d.putFloat(YAW, ballon.routeYaw());
                d.putInt(KLEUR, ballon.kleur());
            }
            d.remove(WEG);
            return null;
        }
        if (!d.contains(THUIS)) {
            return null;
        }
        BlockPos thuis = BlockPos.of(d.getLongOr(THUIS, 0L));
        if (!level.isLoaded(thuis) || !level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(thuis))) {
            return null;
        }
        // (gone twice in a row: not just a chunk whose entities were still coming in)
        if (d.getIntOr(WEG, 0) < 1) {
            d.putInt(WEG, d.getIntOr(WEG, 0) + 1);
            return null;
        }
        d.remove(WEG);
        HavenBallonEntity nieuw = BouwWolk1Slice.HAVEN_BALLON.get().create(level, EntitySpawnReason.TRIGGERED);
        if (nieuw == null) {
            return null;
        }
        float yaw = d.getFloatOr(YAW, 0f);
        nieuw.snapTo(thuis.getX() + 0.5, thuis.getY(), thuis.getZ() + 0.5, yaw, 0);
        nieuw.setThuis(thuis, yaw);
        nieuw.setKleur(d.getIntOr(KLEUR, 1));
        level.addFreshEntity(nieuw);
        return nieuw;
    }
}
