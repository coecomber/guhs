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
 * after the rings a little lemurguhtje snatches your knabbel (nothing is really taken).
 * <p>
 * fix round 1 (playtest 1: "no idea which lemur, or where"): he now appears right in front of you, glowing, as soon as you
 * stand on the ground again, and he shows you the way himself. First he flutters to the south door of the monnikentoren
 * (the north-west tower) and into its kitchen; then on along {@code momo_route} (up the tower, over its bridge, once round
 * the roof of the hall, over the other bridge, up the windtoren). He is always a few blocks ahead, leaves a trail of little
 * clouds and sparkles, and when you fall behind he waits for you, chirping. In his nest ({@code momo_eind}) he lets
 * himself be caught: the scene momo, and he is yours ({@code Momo.geef}). Lost him (logged out, walked off)? He appears
 * near you again and starts over.
 * <p>
 * Deviation of CONTRACT_E2_B 7.2 (which gives Momo at the kaasbol): Momo-guh joins at the end of this chase, as in the show
 * (Aang finds Momo at the air temple). The lemurguhtje of the chase is an unowned Momo-guh only this player sees.
 */
public final class MomoJacht {
    /** How far ahead he stays, and from how far behind he waits for you. */
    static final double VOOR = 7, WACHT = 13;
    static final double SNELHEID = 0.3;
    /** How high above the ground he flies on the way to the tower (over benches and railings). */
    static final double BOVEN = 2.2;

    private static final class Staat {
        MomoGuhEntity momo;
        /** The way there: from where he appeared to the tower's door, then into the kitchen (route point 0). */
        List<Vec3> heen = List.of();
        int heenPunt;
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
        p.sendSystemMessage(Component.translatable(E2H1Lucht.P + "momo.volg").withStyle(ChatFormatting.YELLOW));
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
            if (Training.zweeft(p) || p.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)) {
                return;                                   // (still floating down after the rings: he comes when you are down)
            }
            st = verschijn(p, g, route);
            if (st == null) {
                return;
            }
        }
        MomoGuhEntity momo = st.momo;
        Vec3 hier = momo.position();
        long nu = g.getGameTime();
        // caught in his nest
        if (st.punt >= route.size()) {
            if (p.position().distanceTo(eind) < 3.5) {
                gevangen(p, st, BlockPos.containing(eind));
            } else if (nu % 30 == 0) {
                Schijn.geluid(p, E2AppaFeature.MOMO.get(), momo.blockPosition(), 0.8f, 1.2f);
            }
            spoor(p, hier, nu);
            return;
        }
        double afstand = p.position().distanceTo(hier);
        spoor(p, hier, nu);
        if (afstand > WACHT) {
            if (!st.wachtGezegd) {
                st.wachtGezegd = true;
                p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "momo.wacht").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (nu % 40 == 0) {
                Schijn.geluid(p, E2AppaFeature.MOMO.get(), momo.blockPosition(), 1.0f, 1.5f);
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
        boolean opWeg = st.heenPunt < st.heen.size();
        Vec3 doel = opWeg ? st.heen.get(st.heenPunt) : route.get(st.punt).add(0, 1.2, 0);
        Vec3 d = doel.subtract(hier);
        double len = d.length();
        Vec3 nieuw = len <= SNELHEID ? doel : hier.add(d.scale(SNELHEID / len));
        if (len <= SNELHEID) {
            if (opWeg) {
                st.heenPunt++;
                if (st.heenPunt == st.heen.size()) {
                    p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "momo.toren").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            } else {
                st.punt++;
            }
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        momo.snapTo(nieuw.x, nieuw.y, nieuw.z, yaw, 0f);
        momo.setYHeadRot(yaw);
        momo.setYBodyRot(yaw);
        if (nu % 50 == 0) {
            Schijn.geluid(p, E2AppaFeature.MOMO.get(), momo.blockPosition(), 0.9f, 1.4f);
        }
    }

    /**
     * He appears glowing right in front of the player and works out his way to the kitchen: over the player's head to the
     * south door of the monnikentoren, then inside to route point 0. (Already in or by the tower: straight on.)
     */
    private static Staat verschijn(ServerPlayer p, ServerLevel g, List<Vec3> route) {
        Staat st = new Staat();
        st.momo = E2AppaFeature.MOMO_GUH.get().create(g, EntitySpawnReason.TRIGGERED);
        if (st.momo == null) {
            return null;
        }
        Vec3 kijk = p.getLookAngle();
        Vec3 vlak = new Vec3(kijk.x, 0, kijk.z);
        vlak = vlak.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : vlak.normalize();
        Vec3 s = p.position().add(vlak.scale(2.5)).add(0, 1.6, 0);
        Vec3 keuken = route.get(0).add(0, 1.2, 0);
        Vec3 deur = torendeur(g);
        java.util.ArrayList<Vec3> heen = new java.util.ArrayList<>();
        if (p.position().distanceTo(route.get(0)) > 14 && deur != null) {
            double hoog = Math.max(p.getY(), deur.y) + BOVEN;
            Vec3 aanloop = new Vec3(deur.x, hoog, deur.z + 4);
            heen.add(new Vec3(s.x, hoog, s.z));
            net.minecraft.world.phys.AABB hal = E2H1Lucht.zone(g, "hal");
            if (hal != null) {
                for (Vec3 hoek : omDeHal(s, aanloop, hal)) {
                    heen.add(new Vec3(hoek.x, hoog, hoek.z));
                }
            }
            heen.add(aanloop);
            heen.add(deur.add(0, 1.2, 2));
            heen.add(deur.add(0, 1.2, 0));
        }
        heen.add(keuken);
        st.heen = heen;
        st.momo.snapTo(s.x, s.y, s.z, 180f, 0f);
        st.momo.setNoAi(true);
        st.momo.setNoGravity(true);
        st.momo.setInvulnerable(true);
        st.momo.setGlowingTag(true);
        Zicht.alleenVoor(st.momo, p.getUUID());
        Tijdelijk.merk(st.momo);
        g.addFreshEntity(st.momo);
        STATEN.put(p.getUUID(), st);
        Schijn.deeltjes(p, ParticleTypes.CLOUD, s, 16, 0.4, 0.05);
        Schijn.geluid(p, E2AppaFeature.MOMO.get(), BlockPos.containing(s), 1.0f, 1.3f);
        p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "momo.daar").withStyle(ChatFormatting.LIGHT_PURPLE));
        return st;
    }

    /**
     * The corners he flies round when the hall stands between him and the tower (players walk round it, so does he): from
     * the south by its west side, from the east over its north side. Empty: the way is free.
     */
    static List<Vec3> omDeHal(Vec3 van, Vec3 naar, net.minecraft.world.phys.AABB hal) {
        net.minecraft.world.phys.AABB rond = hal.inflate(3, 0, 3);
        boolean raakt = false;
        for (int i = 0; i <= 40 && !raakt; i++) {
            Vec3 q = van.add(naar.subtract(van).scale(i / 40.0));
            raakt = q.x > rond.minX && q.x < rond.maxX && q.z > rond.minZ && q.z < rond.maxZ;
        }
        if (!raakt) {
            return List.of();
        }
        Vec3 zw = new Vec3(hal.minX - 6, 0, hal.maxZ + 5), nw = new Vec3(hal.minX - 6, 0, hal.minZ - 4), no = new Vec3(hal.maxX + 6, 0, hal.minZ - 4);
        if (van.z > hal.maxZ) {
            return van.x > hal.maxX ? List.of(new Vec3(hal.maxX + 6, 0, hal.maxZ + 5), no) : List.of(zw, nw);
        }
        return van.x > hal.maxX ? List.of(no) : List.of(nw);
    }

    /** The south door of the monnikentoren (the middle of the doorway on the ground floor, just outside it). */
    private static Vec3 torendeur(ServerLevel g) {
        net.minecraft.world.phys.AABB toren = E2H1Lucht.zone(g, "monnikenverblijf");
        if (toren == null) {
            return null;
        }
        return new Vec3(toren.getCenter().x, toren.minY, toren.maxZ + 0.5);
    }

    /** His trail: little clouds and sparkles behind him, seen from afar. */
    private static void spoor(ServerPlayer p, Vec3 hier, long nu) {
        if (nu % 2 == 0) {
            E2H1Lucht.ver(p, ParticleTypes.CLOUD, hier.add(0, 0.3, 0), 1, 0.08, 0.0);
        }
        if (nu % 4 == 0) {
            E2H1Lucht.ver(p, ParticleTypes.END_ROD, hier.add(0, 0.5, 0), 1, 0.1, 0.0);
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

    /** (test) is the chase lemur out for this player? */
    static boolean bezig(ServerPlayer p) {
        return STATEN.containsKey(p.getUUID());
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
