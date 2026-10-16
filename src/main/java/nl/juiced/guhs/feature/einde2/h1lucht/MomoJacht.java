package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.appa.E2AppaFeature;
import nl.juiced.guhs.feature.einde2.appa.Momo;
import nl.juiced.guhs.feature.einde2.appa.MomoGuhEntity;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 phase B, slice h1_lucht, step 6 (DESIGN 5 h1: "Momo-guh jat je knabbel -> achtervolging door de tempel"): right
 * after the rings a little lemurguhtje snatches your knabbel (nothing is really taken) and flies off to the kitchen of the
 * monnikentoren. There it waits for you, then flutters ahead along {@code momo_route} (up the tower, over its bridge, once
 * round the roof of the hall, over the other bridge, up the windtoren), always a few blocks ahead, waiting when you fall
 * behind. In his nest ({@code momo_eind}) he lets himself be caught: the scene momo, and he is yours ({@code Momo.geef}).
 * <p>
 * Deviation of CONTRACT_E2_B 7.2 (which gives Momo at the kaasbol): Momo-guh joins at the end of this chase, as in the show
 * (Aang finds Momo at the air temple). The lemurguhtje of the chase is an unowned Momo-guh only this player sees.
 */
public final class MomoJacht {
    /** How far ahead he stays, and from how far behind he waits for you. */
    static final double VOOR = 9, WACHT = 15;
    static final double SNELHEID = 0.3;

    private static final class Staat {
        MomoGuhEntity momo;
        int punt = 1;
        boolean wachtGezegd;
    }

    private static final Map<UUID, Staat> STATEN = new ConcurrentHashMap<>();

    private MomoJacht() {
    }

    /** Right after the rings: the snatch (a line, a flutter past, a chirp). */
    static void jat(ServerPlayer p) {
        Schijn.deeltjes(p, ParticleTypes.CLOUD, p.position().add(0, 1.4, 0), 12, 0.4, 0.05);
        Schijn.geluid(p, E2AppaFeature.MOMO.get(), p.blockPosition(), 1.0f, 1.3f);
        p.sendSystemMessage(Component.translatable(E2H1Lucht.P + "momo.jat").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.translatable(E2H1Lucht.P + "momo.keuken").withStyle(ChatFormatting.YELLOW));
        Schijn.deeltjes(p, ParticleTypes.CRIT, p.position().add(0, 1.6, 0), 8, 0.3, 0.1);
        nl.juiced.guhs.feature.einde2.audio.E2Audio.stinger(p, "deuntje.momo");   // (integratie_b: audio's tune of the chase)
    }

    /** Every tick, for a player at step 6 in the temple. */
    static void tick(ServerPlayer p) {
        if (E2H1Lucht.LIJN.stap(p) != E2H1Lucht.MOMO || !E2H1Lucht.inTempel(p)) {
            stop(p);
            return;
        }
        if (Cutscenes.bezig(p)) {
            return;
        }
        ServerLevel g = p.level();
        List<Vec3> route = E2H1Lucht.pad(g, "momo_route");
        Vec3 eind = E2H1Lucht.midden(g, "momo_eind");
        if (route.size() < 2 || eind == null) {
            return;
        }
        Staat st = STATEN.get(p.getUUID());
        if (st == null || st.momo == null || st.momo.isRemoved()) {
            if (p.position().distanceTo(route.get(0)) > 48) {
                return;                                   // (he waits in the kitchen until you are near)
            }
            st = new Staat();
            st.momo = E2AppaFeature.MOMO_GUH.get().create(g, EntitySpawnReason.TRIGGERED);
            if (st.momo == null) {
                return;
            }
            Vec3 s = route.get(0).add(0, 1.2, 0);
            st.momo.snapTo(s.x, s.y, s.z, 180f, 0f);
            st.momo.setNoAi(true);
            st.momo.setNoGravity(true);
            st.momo.setInvulnerable(true);
            Zicht.alleenVoor(st.momo, p.getUUID());
            Tijdelijk.merk(st.momo);
            g.addFreshEntity(st.momo);
            STATEN.put(p.getUUID(), st);
        }
        MomoGuhEntity momo = st.momo;
        Vec3 hier = momo.position();
        // caught in his nest
        if (st.punt >= route.size()) {
            if (p.position().distanceTo(eind) < 3.5) {
                gevangen(p, st, BlockPos.containing(eind));
            } else if (g.getGameTime() % 30 == 0) {
                Schijn.geluid(p, E2AppaFeature.MOMO.get(), momo.blockPosition(), 0.8f, 1.2f);
            }
            return;
        }
        double afstand = p.position().distanceTo(hier);
        if (afstand > WACHT) {
            if (!st.wachtGezegd && st.punt > 1) {
                st.wachtGezegd = true;
                p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "momo.wacht").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            momo.setYHeadRot((float) Math.toDegrees(Math.atan2(-(p.getX() - hier.x), p.getZ() - hier.z)));
            return;                                       // (he waits, looking back)
        }
        if (afstand > VOOR) {
            return;                                       // (far enough ahead)
        }
        if (st.wachtGezegd) {
            st.wachtGezegd = false;
            p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "momo.weg").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        Vec3 doel = route.get(st.punt).add(0, 1.2, 0);
        Vec3 d = doel.subtract(hier);
        double len = d.length();
        Vec3 nieuw = len <= SNELHEID ? doel : hier.add(d.scale(SNELHEID / len));
        if (len <= SNELHEID) {
            st.punt++;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        momo.snapTo(nieuw.x, nieuw.y, nieuw.z, yaw, 0f);
        momo.setYHeadRot(yaw);
        momo.setYBodyRot(yaw);
        if (g.getGameTime() % 4 == 0) {
            Schijn.deeltjes(p, ParticleTypes.CLOUD, nieuw, 1, 0.1, 0.0);
        }
        if (g.getGameTime() % 50 == 0) {
            Schijn.geluid(p, E2AppaFeature.MOMO.get(), momo.blockPosition(), 0.9f, 1.4f);
        }
    }

    private static void gevangen(ServerPlayer p, Staat st, BlockPos anker) {
        if (!Cutscenes.speel(p, H1Scenes.MOMO, anker, Rotation.NONE, MomoJacht::klaar)) {
            return;
        }
        Tijdelijk.weg(st.momo);
        STATEN.remove(p.getUUID());
    }

    /** The scene seen: he is yours, and on to the stable. */
    static void klaar(ServerPlayer p) {
        stop(p);
        Momo.geef(p);
        E2H1Lucht.LIJN.verder(p, E2H1Lucht.MOMO);
    }

    static void stop(ServerPlayer p) {
        vergeet(p.getUUID());
    }

    static void vergeet(UUID speler) {
        Staat st = STATEN.remove(speler);
        if (st != null) {
            Tijdelijk.weg(st.momo);
        }
    }
}
