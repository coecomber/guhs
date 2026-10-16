package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.ArrayList;
import java.util.List;
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
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B, slice h1_lucht, step 3 (DESIGN 5 h1: "luchtbal op palen, minispel tegen Aang-guh"): a match on the
 * luchtbalveld, first to three. The player stands on {@code luchtbal_start_speler} and hops from pole to pole; the ball
 * ({@link LuchtbalEntity}) floats over the court; hit it (left click) through Aang-guh's goal ({@code luchtbal_doel_aang}).
 * <p>
 * fix round 1 (playtest 1: "far too hard, the ball keeps falling and resetting"), so that a normal player wins in one to
 * three minutes:
 * <ul>
 *   <li>the ball never falls: it floats at the eye height of a player on a pole, and when it slows down it balances above
 *       the nearest pole ({@link LuchtbalEntity#zweef}); it glows, so you always see it;</li>
 *   <li>a hit roughly towards Aang-guh's goal is helped into it (the wind of a Guhvatar), and the goal counts the whole
 *       opening of its frame;</li>
 *   <li>Aang-guh mostly passes the ball back to you (a rally); he only shoots at your goal now and then, only when he
 *       does not lead, and never his third goal: he is a kind teacher;</li>
 *   <li>who falls into the cloud is put back on a pole near the ball within a second; the ball out of the court comes back
 *       on a pole near you.</li>
 * </ul>
 * Losing costs nothing: Guhatso starts a new match.
 */
public final class Luchtbal {
    /** Goals to win. */
    public static final int WINST = 3;
    /** The opening of a goal: how far from its middle the ball may pass its frame (sideways and up/down). */
    static final double DOEL = 2.6;
    /** How strongly a hit towards Aang-guh's goal is helped into it (0 = not, 1 = straight into the middle). */
    static final double HULP = 0.7;
    /** The ball floats this high above the highest pole top (its feet; its middle is then at eye height on a pole). */
    static final double BOVEN_PAAL = 1.25;

    private static final class Staat {
        LuchtbalEntity bal;
        GuhNpcEntity aang;
        int jij, hij, buiten, gevallen, balWeg;
        long blaasOp;
        Vec3 vorigeBal;
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
        Vec3 m = midden(g);
        if (aangPlek == null || m == null) {
            return;
        }
        stop(p);
        Staat st = new Staat();
        st.aang = Tempel.acteur(g, p, GuhNpcEntity.Kind.AANG_GUH, aangPlek, 0f);
        st.bal = E2H1LuchtFeature.LUCHTBAL.get().create(g, EntitySpawnReason.TRIGGERED);
        if (st.bal != null) {
            st.bal.snapTo(m.x, m.y, m.z, 0f, 0f);
            st.bal.zweef(m.y, palen(g));
            st.bal.setGlowingTag(true);
            Zicht.alleenVoor(st.bal, p.getUUID());
            Tijdelijk.merk(st.bal);
            g.addFreshEntity(st.bal);
            st.vorigeBal = st.bal.position();
        }
        st.blaasOp = g.getGameTime() + 100;
        STATEN.put(p.getUUID(), st);
        E2H1Lucht.zetNaar(p, "luchtbal_start_speler", 180f);
        p.sendSystemMessage(Component.translatable(E2H1Lucht.P + "luchtbal.start").withStyle(ChatFormatting.GOLD));
        E2H1Lucht.titel(p, Component.translatable(E2H1Lucht.P + "luchtbal.titel").withStyle(ChatFormatting.GOLD),
                Component.translatable(E2H1Lucht.P + "luchtbal.titel_onder").withStyle(ChatFormatting.YELLOW), 60);
        stand(p, st);
    }

    /** The pole tops of the court (block positions). */
    static List<Vec3> palen(ServerLevel g) {
        List<Vec3> uit = new ArrayList<>();
        for (Plekken.Plek q : Plekken.van(E2H1Lucht.STRUCTUUR).values()) {
            if (q.id().startsWith("luchtbal_paal_")) {
                BlockPos b = E2H1Lucht.plek(g, q.id());
                if (b != null) {
                    uit.add(Vec3.atLowerCornerOf(b));
                }
            }
        }
        return uit;
    }

    /** The middle of the court, at the height the ball floats at (just above the highest pole top). */
    @Nullable
    private static Vec3 midden(ServerLevel g) {
        AABB veld = E2H1Lucht.zone(g, "luchtbal_veld");
        if (veld == null) {
            return null;
        }
        double top = Double.NEGATIVE_INFINITY;
        for (Vec3 paal : palen(g)) {
            top = Math.max(top, paal.y);
        }
        if (top == Double.NEGATIVE_INFINITY) {
            return null;
        }
        Vec3 c = veld.getCenter();
        return new Vec3(c.x, top + 1 + BOVEN_PAAL, c.z);
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
        // fell into the cloud: back on a pole near the ball (after a short moment)
        if (veld.contains(p.position()) && p.getY() < m.y - BOVEN_PAAL - 1.6) {
            if (++st.gevallen > 15) {
                st.gevallen = 0;
                Vec3 paal = paalBij(g, bal.position());
                if (paal != null) {
                    p.teleportTo(g, paal.x + 0.5, paal.y + 1.05, paal.z + 0.5, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
                    p.resetFallDistance();
                    Schijn.deeltjes(p, ParticleTypes.CLOUD, p.position().add(0, 0.5, 0), 14, 0.4, 0.05);
                    Schijn.geluid(p, SoundEvents.WOOL_HIT, p.blockPosition(), 1.0f, 1.4f);
                    p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "luchtbal.terug_op_paal").withStyle(ChatFormatting.AQUA));
                }
            }
        } else {
            st.gevallen = 0;
        }
        Vec3 b = bal.position().add(0, 0.375, 0);
        Vec3 vorige = st.vorigeBal == null ? b : st.vorigeBal.add(0, 0.375, 0);
        st.vorigeBal = bal.position();
        // a goal? (the ball crossed the plane of a goal frame inside its opening)
        if (door(vorige, b, doelAang)) {
            st.jij++;
            juich(p, doelAang, E2H1Lucht.P + "luchtbal.jij", ChatFormatting.GREEN);
            if (!uitslag(p, st)) {
                opnieuw(st, m, doelSpeler.subtract(m));
                if (st.aang != null) {
                    st.aang.speel("zwaai");
                }
            }
            return;
        }
        if (door(vorige, b, doelSpeler)) {
            st.hij++;
            juich(p, doelSpeler, E2H1Lucht.P + "luchtbal.aang", ChatFormatting.RED);
            if (st.aang != null) {
                st.aang.speel("lach");
            }
            if (!uitslag(p, st)) {
                opnieuw(st, m, doelAang.subtract(m));
            }
            return;
        }
        // out of the court or down in the cloud (a wild hit): back on a pole near the player
        boolean buiten = b.y < veld.minY + 3 || b.y > veld.maxY + 6 || b.x < veld.minX - 3 || b.x > veld.maxX + 3
                || b.z < Math.min(doelAang.z, doelSpeler.z) - 2 || b.z > Math.max(doelAang.z, doelSpeler.z) + 2;
        if (buiten && ++st.balWeg > 20) {
            st.balWeg = 0;
            Schijn.deeltjes(p, ParticleTypes.POOF, b, 10, 0.3, 0.02);
            Vec3 paal = paalBij(g, p.position());
            Vec3 naar = paal == null ? m : new Vec3(paal.x + 0.5, m.y, paal.z + 0.5);
            bal.snapTo(naar.x, naar.y, naar.z, 0f, 0f);
            bal.setDeltaMovement(Vec3.ZERO);
            st.vorigeBal = bal.position();
            p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "luchtbal.bal_terug").withStyle(ChatFormatting.AQUA));
            return;
        } else if (!buiten) {
            st.balWeg = 0;
        }
        // Aang-guh blows when the ball is on his half: mostly a pass back to you, now and then a shot at your goal
        long nu = g.getGameTime();
        if (st.aang != null && nu >= st.blaasOp) {
            double helft = (doelAang.z + doelSpeler.z) / 2;
            boolean zijnKant = Math.signum(b.z - helft) == Math.signum(doelAang.z - helft);
            if (zijnKant && b.distanceTo(st.aang.position()) < 24 && bal.getDeltaMovement().horizontalDistance() < 0.25) {
                boolean schiet = st.hij < st.jij + (st.jij >= 2 ? 0 : 1) && p.getRandom().nextInt(4) == 0;
                Vec3 naar;
                if (schiet) {
                    naar = doelSpeler.add((p.getRandom().nextDouble() - 0.5) * 3.0, 0.6, 0);
                } else {
                    Vec3 paal = paalBij(g, p.position());
                    naar = paal == null ? p.position() : new Vec3(paal.x + 0.5, b.y, paal.z + 0.5);
                }
                Vec3 richting = naar.subtract(b);
                // (a floating ball rolls out about 49 x its push: a pass stops near the pole, a shot keeps going)
                double kracht = schiet ? 0.6 : Math.max(0.12, Math.min(0.5, richting.horizontalDistance() / 45.0));
                bal.duw(new Vec3(richting.x, 0, richting.z), kracht);
                Vec3 van = st.aang.position().add(0, 1, 0);
                for (int i = 1; i <= 6; i++) {
                    Schijn.deeltjes(p, ParticleTypes.CLOUD, van.add(b.subtract(van).scale(i / 6.0)), 2, 0.15, 0.01);
                }
                st.aang.speel("luchtscooter");
                p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + (schiet ? "luchtbal.schiet" : "luchtbal.blaast")).withStyle(ChatFormatting.AQUA));
                st.blaasOp = nu + 70 + p.getRandom().nextInt(40);
            }
            // he watches the ball
            float yaw = (float) Math.toDegrees(Math.atan2(-(b.x - st.aang.getX()), b.z - st.aang.getZ()));
            st.aang.setYHeadRot(yaw);
        }
        if (nu % 40 == 0) {
            stand(p, st);
        }
    }

    /** Did the ball go from a to b through the opening of this goal (the middle of its hole)? Its frame stands across z. */
    static boolean door(Vec3 a, Vec3 b, Vec3 doel) {
        double cy = doel.y + 0.5, cz = doel.z + 0.0;
        double da = a.z - cz, db = b.z - cz;
        if (Math.signum(da) == Math.signum(db) && Math.abs(db) > 0.5) {
            return false;
        }
        double t = Math.abs(da - db) < 1e-6 ? 1 : Math.max(0, Math.min(1, da / (da - db)));
        Vec3 snij = a.add(b.subtract(a).scale(t));
        return Math.abs(snij.x - doel.x) <= DOEL && Math.abs(snij.y - cy) <= DOEL;
    }

    /** The pole nearest to {@code bij} (horizontally). */
    @Nullable
    private static Vec3 paalBij(ServerLevel g, Vec3 bij) {
        Vec3 best = null;
        double d = Double.MAX_VALUE;
        for (Vec3 paal : palen(g)) {
            double e = Math.hypot(paal.x + 0.5 - bij.x, paal.z + 0.5 - bij.z);
            if (e < d) {
                d = e;
                best = paal;
            }
        }
        return best;
    }

    private static void opnieuw(Staat st, Vec3 m, Vec3 naar) {
        st.bal.snapTo(m.x, m.y, m.z, 0f, 0f);
        st.bal.setDeltaMovement(naar.lengthSqr() < 1e-6 ? Vec3.ZERO : new Vec3(naar.x, 0, naar.z).normalize().scale(0.12));
        st.vorigeBal = st.bal.position();
    }

    private static void juich(ServerPlayer p, Vec3 doel, String key, ChatFormatting kleur) {
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
        E2H1Lucht.titel(p, Component.translatable(E2H1Lucht.P + "luchtbal.stand", st.jij, st.hij).withStyle(ChatFormatting.GOLD), null, 30);
        if (st.jij >= WINST) {
            stop(p);
            ServerLevel g = p.level();
            GuhNpcEntity guhatso = Tempel.bewoner(g, "guhatso", GuhNpcEntity.Kind.GUHATSO);
            if (guhatso != null) {
                GuhQuests.say(p, guhatso, E2H1Lucht.Q + "guhatso.gewonnen");
                Tempel.taart(p, guhatso);
            }
            E2H1Lucht.titel(p, Component.translatable(E2H1Lucht.P + "luchtbal.gewonnen").withStyle(ChatFormatting.GREEN), null, 50);
            Schijn.geluid(p, SoundEvents.PLAYER_LEVELUP, p.blockPosition(), 1.0f, 1.0f);
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

    /**
     * A left click on the match ball (from {@link LuchtbalEntity#skipAttackInteraction}): it flies where you look, and when
     * you look roughly towards Aang-guh's goal the wind helps it in. False: no match of this player (a plain push).
     */
    static boolean geslagen(ServerPlayer p, LuchtbalEntity bal) {
        Staat st = STATEN.get(p.getUUID());
        ServerLevel g = E2H1Lucht.guheinde(p.level().getServer());
        if (st == null || st.bal != bal || g == null) {
            return false;
        }
        Vec3 doel = E2H1Lucht.midden(g, "luchtbal_doel_aang");
        Vec3 kijk = p.getLookAngle();
        Vec3 richting = kijk;
        if (doel != null) {
            Vec3 naarDoel = doel.add(0, 0.5, 0).subtract(bal.position().add(0, 0.375, 0));
            Vec3 vlak = new Vec3(kijk.x, 0, kijk.z);
            Vec3 vlakDoel = new Vec3(naarDoel.x, 0, naarDoel.z);
            if (vlak.lengthSqr() > 1e-4 && vlakDoel.lengthSqr() > 1e-4 && vlak.normalize().dot(vlakDoel.normalize()) > 0.35) {
                richting = kijk.normalize().scale(1 - HULP).add(naarDoel.normalize().scale(HULP));
                Schijn.deeltjes(p, ParticleTypes.END_ROD, bal.position().add(0, 0.4, 0), 6, 0.2, 0.05);
            }
        }
        // (duw adds a little lift; aim a touch lower so a straight hit stays straight)
        bal.duw(richting.add(0, -0.1, 0), 0.9);
        return true;
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
