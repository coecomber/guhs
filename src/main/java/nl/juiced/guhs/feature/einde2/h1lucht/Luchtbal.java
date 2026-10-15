package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B, slice h1_lucht, step 3 (DESIGN 5 h1: "luchtbal op palen, minispel tegen Aang-guh"): a match on the
 * luchtbalveld, first to three. The player stands on {@code luchtbal_start_speler} and hops from pole to pole; the ball
 * ({@link LuchtbalEntity}) floats over the court; hit it (left click) through Aang-guh's goal ({@code luchtbal_doel_aang}).
 * Aang-guh stands on his own platform and blows the ball towards yours ({@code luchtbal_doel_speler}) when it comes to his
 * half. A ball in the cloud bed comes back in the middle. Losing costs nothing: Guhatso starts a new match.
 */
public final class Luchtbal {
    /** Goals to win. */
    public static final int WINST = 3;
    /** How near the ball must come to the middle of a goal's hole. */
    static final double DOEL = 1.8;

    private static final class Staat {
        LuchtbalEntity bal;
        GuhNpcEntity aang;
        int jij, hij, buiten;
        long blaasOp;
    }

    private static final Map<UUID, Staat> STATEN = new ConcurrentHashMap<>();

    private Luchtbal() {
    }

    public static boolean bezig(Player p) {
        return STATEN.containsKey(p.getUUID());
    }

    /** Guhatso's "ik ben er klaar voor": the player on their platform, Aang-guh on his, the ball in the middle. */
    static void start(ServerPlayer p) {
        ServerLevel g = E2H1Lucht.guheinde(p.level().getServer());
        if (g == null || E2H1Lucht.LIJN.stap(p) != E2H1Lucht.LUCHTBAL) {
            return;
        }
        Vec3 aangPlek = E2H1Lucht.midden(g, "luchtbal_start_aang");
        if (aangPlek == null || midden(g) == null) {
            return;
        }
        stop(p);
        Staat st = new Staat();
        st.aang = Tempel.acteur(g, p, GuhNpcEntity.Kind.AANG_GUH, aangPlek, 0f);
        st.bal = E2H1LuchtFeature.LUCHTBAL.get().create(g, EntitySpawnReason.TRIGGERED);
        if (st.bal != null) {
            Vec3 m = midden(g);
            st.bal.snapTo(m.x, m.y, m.z, 0f, 0f);
            Zicht.alleenVoor(st.bal, p.getUUID());
            Tijdelijk.merk(st.bal);
            g.addFreshEntity(st.bal);
        }
        st.blaasOp = g.getGameTime() + 60;
        STATEN.put(p.getUUID(), st);
        E2H1Lucht.zetNaar(p, "luchtbal_start_speler", 180f);
        p.sendSystemMessage(Component.translatable(E2H1Lucht.P + "luchtbal.start").withStyle(ChatFormatting.GOLD));
        stand(p, st);
    }

    /** The middle of the court, high above the poles (where the ball starts). */
    @Nullable
    private static Vec3 midden(ServerLevel g) {
        AABB veld = E2H1Lucht.zone(g, "luchtbal_veld");
        BlockPos paal = E2H1Lucht.plek(g, "luchtbal_paal_01");
        if (veld == null || paal == null) {
            return null;
        }
        Vec3 c = veld.getCenter();
        return new Vec3(c.x, paal.getY() + 3.5, c.z);
    }

    /** Every tick, for a player in a match. */
    static void tick(ServerPlayer p) {
        Staat st = STATEN.get(p.getUUID());
        if (st == null) {
            return;
        }
        ServerLevel g = E2H1Lucht.guheinde(p.level().getServer());
        if (g == null || p.level() != g || E2H1Lucht.LIJN.stap(p) != E2H1Lucht.LUCHTBAL || st.bal == null || st.bal.isRemoved()) {
            stop(p);
            return;
        }
        if (Cutscenes.bezig(p)) {
            return;
        }
        AABB veld = E2H1Lucht.zone(g, "luchtbal_veld");
        Vec3 doelAang = E2H1Lucht.midden(g, "luchtbal_doel_aang"), doelSpeler = E2H1Lucht.midden(g, "luchtbal_doel_speler");
        Vec3 m = midden(g);
        if (veld == null || doelAang == null || doelSpeler == null || m == null) {
            return;
        }
        // off the court too long: the match stops (nothing lost)
        if (!veld.inflate(8, 6, 8).contains(p.position())) {
            if (++st.buiten > 200) {
                stop(p);
                p.sendSystemMessage(Component.translatable(E2H1Lucht.P + "luchtbal.weg").withStyle(ChatFormatting.GRAY));
                return;
            }
        } else {
            st.buiten = 0;
        }
        LuchtbalEntity bal = st.bal;
        Vec3 b = bal.position().add(0, 0.3, 0);
        // a goal?
        if (b.distanceTo(doelAang.add(0, 0.5, 0)) < DOEL) {
            st.jij++;
            juich(p, g, doelAang, E2H1Lucht.P + "luchtbal.jij", ChatFormatting.GREEN);
            if (!uitslag(p, st)) {
                opnieuw(bal, m, doelSpeler.subtract(m));
            }
            return;
        }
        if (b.distanceTo(doelSpeler.add(0, 0.5, 0)) < DOEL) {
            st.hij++;
            juich(p, g, doelSpeler, E2H1Lucht.P + "luchtbal.aang", ChatFormatting.RED);
            if (st.aang != null) {
                st.aang.speel("lach");
            }
            if (!uitslag(p, st)) {
                opnieuw(bal, m, doelAang.subtract(m));
            }
            return;
        }
        // out of the court (sideways) or down in the cloud: back to the middle
        if (b.y < veld.minY + 2 || !veld.inflate(2, 10, 2).contains(b)) {
            Schijn.deeltjes(p, ParticleTypes.POOF, b, 10, 0.3, 0.02);
            opnieuw(bal, m, Vec3.ZERO);
            return;
        }
        // Aang-guh blows when the ball is on his half and not too far
        long nu = g.getGameTime();
        if (st.aang != null && nu >= st.blaasOp) {
            double helft = (doelAang.z + doelSpeler.z) / 2;
            boolean zijnKant = Math.signum(b.z - helft) == Math.signum(doelAang.z - helft);
            if (zijnKant && b.distanceTo(st.aang.position()) < 22) {
                Vec3 richting = doelSpeler.add(0, 1.0, 0).subtract(b).normalize();
                bal.duw(richting.add((p.getRandom().nextDouble() - 0.5) * 0.25, 0.12, 0), 0.48);
                Vec3 van = st.aang.position().add(0, 1, 0);
                for (int i = 1; i <= 6; i++) {
                    Schijn.deeltjes(p, ParticleTypes.CLOUD, van.add(b.subtract(van).scale(i / 6.0)), 2, 0.15, 0.01);
                }
                st.aang.speel("luchtscooter");
                p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "luchtbal.blaast").withStyle(ChatFormatting.AQUA));
                st.blaasOp = nu + 60 + p.getRandom().nextInt(30);
            }
            // he watches the ball
            float yaw = (float) Math.toDegrees(Math.atan2(-(b.x - st.aang.getX()), b.z - st.aang.getZ()));
            st.aang.setYHeadRot(yaw);
        }
    }

    private static void opnieuw(LuchtbalEntity bal, Vec3 m, Vec3 naar) {
        bal.snapTo(m.x, m.y, m.z, 0f, 0f);
        bal.setDeltaMovement(naar.lengthSqr() < 1e-6 ? Vec3.ZERO : naar.normalize().scale(0.08));
    }

    private static void juich(ServerPlayer p, ServerLevel g, Vec3 doel, String key, ChatFormatting kleur) {
        Schijn.deeltjes(p, ParticleTypes.FIREWORK, doel.add(0, 0.5, 0), 30, 0.6, 0.1);
        Schijn.geluid(p, SoundEvents.FIREWORK_ROCKET_BLAST, BlockPos.containing(doel), 1.0f, 1.2f);
        p.sendSystemMessage(Component.translatable(key).withStyle(kleur));
    }

    private static void stand(ServerPlayer p, Staat st) {
        p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "luchtbal.stand", st.jij, st.hij).withStyle(ChatFormatting.YELLOW));
    }

    /** True when the match is over (won: step 4; lost: Guhatso starts a new one). */
    private static boolean uitslag(ServerPlayer p, Staat st) {
        stand(p, st);
        if (st.jij >= WINST) {
            stop(p);
            ServerLevel g = p.level();
            GuhNpcEntity guhatso = Tempel.bewoner(g, "guhatso", GuhNpcEntity.Kind.GUHATSO);
            if (guhatso != null) {
                GuhQuests.say(p, guhatso, E2H1Lucht.Q + "guhatso.gewonnen");
                Tempel.taart(p, guhatso);
            }
            E2H1Lucht.LIJN.verder(p, E2H1Lucht.LUCHTBAL);
            return true;
        }
        if (st.hij >= WINST) {
            stop(p);
            p.sendSystemMessage(Component.translatable(E2H1Lucht.P + "luchtbal.verloren").withStyle(ChatFormatting.GRAY));
            return true;
        }
        return false;
    }

    /** The power luchtbal: the ball, when it is in front of you and not too far, flies hard where you look. */
    static boolean waai(ServerPlayer p, Vec3 richting) {
        Staat st = STATEN.get(p.getUUID());
        if (st == null || st.bal == null) {
            return false;
        }
        Vec3 d = st.bal.position().subtract(p.getEyePosition());
        if (d.length() < 9 && d.normalize().dot(richting) > 0.4) {
            st.bal.duw(richting, 1.1);
            return true;
        }
        return false;
    }

    static void stop(ServerPlayer p) {
        vergeet(p.getUUID());
    }

    static void vergeet(UUID speler) {
        Staat st = STATEN.remove(speler);
        if (st != null) {
            Tijdelijk.weg(st.bal);
            Tijdelijk.weg(st.aang);
        }
    }

    /** (test, dev) the score of a match: {you, Aang-guh}, or null. */
    @Nullable
    static int[] score(ServerPlayer p) {
        Staat st = STATEN.get(p.getUUID());
        return st == null ? null : new int[]{st.jij, st.hij};
    }

    /** (dev) a goal for the player, as if the ball went through. */
    static void goal(ServerPlayer p) {
        Staat st = STATEN.get(p.getUUID());
        if (st != null) {
            st.jij++;
            uitslag(p, st);
        }
    }
}
