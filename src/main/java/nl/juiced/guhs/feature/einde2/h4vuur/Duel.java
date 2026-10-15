package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;

/**
 * einde2 phase B, slice h4_vuur, finale step 4: the duel with Vuurheer Mikai on the rock pillars under the knabbelkomeet
 * (DESIGN 5; order of the rounds: our own choice in the design). Four rounds, each won with one element, then the Guhvatar
 * state and a last round with all four:
 * <ol>
 *   <li>{@link #LUCHT}: dodge {@link #LUCHT_STOTEN} blasts; jumping on a pillar is an air jump (high and far);</li>
 *   <li>{@link #WATER}: douse the four flames Mikai sets on the low pillars (right click on a flame);</li>
 *   <li>{@link #AARDE}: climb to the highest pillar under a rain of comet sparks; crouching is an earth jump (a stone column
 *       throws you up);</li>
 *   <li>{@link #VUUR}: hit {@link #VUUR_TERUG} blasts back at him (left click on a blast that comes close);</li>
 *   <li>the rock touches the old scar ({@link Scenes#OMSLAG}): glowing eyes, every earlier Guhvatar behind you, and
 *       {@link #STAAT} with everything at once; then you take his endless appetite away ({@link Scenes#TREK_WEG}).</li>
 * </ol>
 * Nobody gets hurt: a blast or a spark only shoves, a fall ends in the warm water under the pillars and puts you back on the
 * pillar of the round; three blasts in a row, or the time of a round over, and that round simply starts again.
 */
public final class Duel {
    public static final int LUCHT = 1, WATER = 2, AARDE = 3, VUUR = 4, STAAT = 5;
    public static final int LUCHT_STOTEN = 8, VUUR_TERUG = 3, STAAT_TERUG = 3, STAAT_VLAMMEN = 2, RAAK_MAX = 3, RONDE_TIJD = 20 * 75;
    /** The pillar each round starts on, and the low pillars of the water round, and the summit of the earth round. */
    private static final String[] START = {"", "duel_speler", "pilaar_11", "pilaar_13", "pilaar_12", "pilaar_12"};
    private static final String[] WATERPILAREN = {"pilaar_01", "pilaar_02", "pilaar_10", "pilaar_03"};
    private static final String TOP = "pilaar_07";

    private static final class Spel {
        int ronde = LUCHT, t, raak, gedaan, gedaan2, schot;
        boolean wasGrond = true, wasSluip;
        double laatsteY;
        @Nullable
        GuhNpcEntity mikai;
        final List<VuurbalEntity> ballen = new ArrayList<>();
        boolean pauze;
    }

    private static final Map<UUID, Spel> SPELLEN = new ConcurrentHashMap<>();

    private Duel() {
    }

    public static boolean actief(ServerPlayer p) {
        return SPELLEN.containsKey(p.getUUID());
    }

    /** Does the player play the last round, in the Guhvatar state (glowing eyes: {@link Look#GLOED})? */
    public static boolean gloed(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        return s != null && s.ronde == STAAT;
    }

    /** The round this player is in (0: no duel). */
    public static int ronde(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        return s == null ? 0 : s.ronde;
    }

    static void start(ServerPlayer p) {
        if (actief(p)) {
            return;
        }
        Vec3 mikaiPlek = Plek.staan(p, "duel_mikai");
        if (mikaiPlek == null) {
            return;
        }
        Spel s = new Spel();
        Vec3 jij = stand(p, START[LUCHT]);
        float yaw = jij == null ? 0f : nl.juiced.guhs.feature.verhaal.Cutscene.yawVan(mikaiPlek.subtract(jij));
        s.mikai = E2H4Vuur.npc(p, GuhNpcEntity.Kind.VUURHEER_MIKAI_KOMEET, mikaiPlek, yaw + 180f);
        SPELLEN.put(p.getUUID(), s);
        E2Audio.moment(p, "duel");
        begin(p, s, LUCHT);
    }

    private static void begin(ServerPlayer p, Spel s, int ronde) {
        opruimen(s);
        s.ronde = ronde;
        s.t = 0;
        s.raak = 0;
        s.gedaan = 0;
        s.gedaan2 = 0;
        s.schot = 0;
        naarStart(p, s);
        E2H4Vuur.zeg(p, "duel.ronde." + ronde);
        if (ronde == WATER) {
            for (String pilaar : WATERPILAREN) {
                Vec3 top = stand(p, pilaar);
                if (top != null) {
                    vlam(p, s, top);
                }
            }
        }
        if (ronde == STAAT) {
            Look.sync(p);
            E2Audio.stopMoment(p, 10);
            E2Audio.moment(p, "duel_koor");
        }
    }

    private static void naarStart(ServerPlayer p, Spel s) {
        Vec3 jij = stand(p, START[s.ronde]);
        Vec3 mikai = Plek.staan(p, "duel_mikai");
        ServerLevel g = Plek.guheinde(p);
        if (jij != null && g != null) {
            float yaw = mikai == null ? p.getYRot() : nl.juiced.guhs.feature.verhaal.Cutscene.yawVan(mikai.subtract(jij));
            Duwtje.terug(p, g.dimension(), jij, yaw);
            s.laatsteY = p.getY();
        }
    }

    /** Where you stand on a marker: on top of a pillar (its top block), or in a "here you stand" cell. */
    @Nullable
    static Vec3 stand(ServerPlayer p, String id) {
        Vec3 v = Plek.staan(p, id);
        return v == null ? null : id.startsWith("pilaar_") ? v.add(0, 1, 0) : v;
    }

    static void tick(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null || s.pauze || Cutscenes.bezig(p)) {
            return;
        }
        if (!Plek.in(p, "duel_arena")) {
            naarStart(p, s);                                     // (walked or flew out of the arena: back on the pillar)
            return;
        }
        s.t++;
        krachten(p, s);
        // a fall into the warm water under the pillars: back on the pillar of the round
        Vec3 laag = Plek.staan(p, "pilaar_01");
        if (laag != null && p.getY() < laag.y - 0.5) {
            E2H4Vuur.balk(p, "duel.plons");
            naarStart(p, s);
            return;
        }
        if (s.t > RONDE_TIJD && s.ronde != STAAT) {
            E2H4Vuur.zeg(p, "duel.tijd");
            begin(p, s, s.ronde);
            return;
        }
        Vec3 jij = p.position().add(0, 1, 0);
        switch (s.ronde) {
            case LUCHT -> {
                if (s.t - s.schot > 40) {
                    stoot(p, s, jij, 0.5);
                    s.gedaan++;
                }
                if (s.gedaan >= LUCHT_STOTEN && s.ballen.isEmpty()) {
                    gewonnen(p, s);
                    return;
                }
            }
            case WATER -> {
                if (s.t - s.schot > 70) {
                    stoot(p, s, jij, 0.45);
                }
                if (s.gedaan >= WATERPILAREN.length) {
                    gewonnen(p, s);
                    return;
                }
            }
            case AARDE -> {
                if (s.t % 14 == 0) {
                    vonk(p, s, jij);
                }
                Vec3 top = Plek.staan(p, TOP);
                if (top != null && p.getY() >= top.y + 0.9 && Math.hypot(p.getX() - top.x, p.getZ() - top.z) < 2.5) {
                    gewonnen(p, s);
                    return;
                }
            }
            case VUUR -> {
                if (s.t - s.schot > 55) {
                    stoot(p, s, jij, 0.42);
                }
                if (s.gedaan >= VUUR_TERUG) {
                    gewonnen(p, s);
                    return;
                }
            }
            case STAAT -> {
                staatRing(p, s);
                if (s.t - s.schot > 45) {
                    stoot(p, s, jij, 0.45);
                }
                if (s.t % 120 == 1 && s.gedaan2 < STAAT_VLAMMEN && s.ballen.stream().noneMatch(b -> b.soort() == VuurbalEntity.VLAM)) {
                    Vec3 voor = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(2.0)).add(0, 0.6, 0);
                    vlam(p, s, voor);
                }
                if (s.gedaan >= STAAT_TERUG && s.gedaan2 >= STAAT_VLAMMEN) {
                    gewonnen(p, s);
                    return;
                }
            }
            default -> {
            }
        }
        ballen(p, s, jij);
    }

    /** The air jump (every round) and the earth jump (rounds 3 and 5): the player's own jump and crouch, made stronger. */
    private static void krachten(ServerPlayer p, Spel s) {
        boolean grond = p.onGround();
        if (s.wasGrond && !grond && p.getY() > s.laatsteY + 0.15) {
            Vec3 kijk = p.getLookAngle().multiply(1, 0, 1).normalize();
            p.setDeltaMovement(kijk.x * 0.55, 0.85, kijk.z * 0.55);
            p.hurtMarked = true;
            p.level().sendParticles(p, ParticleTypes.CLOUD, true, false, p.getX(), p.getY(), p.getZ(), 10, 0.3, 0.1, 0.3, 0.02);
            Schijn.geluid(p, SoundEvents.BREEZE_JUMP, p.blockPosition(), 0.6f, 1.2f);
        }
        boolean sluip = p.isShiftKeyDown();
        if (sluip && !s.wasSluip && grond && (s.ronde == AARDE || s.ronde == STAAT)) {
            Vec3 kijk = p.getLookAngle().multiply(1, 0, 1).normalize();
            p.setDeltaMovement(kijk.x * 0.5, 1.15, kijk.z * 0.5);
            p.hurtMarked = true;
            p.level().sendParticles(p, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), true, false, p.getX(), p.getY(),
                    p.getZ(), 30, 0.4, 0.2, 0.4, 0.1);
            Schijn.geluid(p, SoundEvents.STONE_BREAK, p.blockPosition(), 1.0f, 0.7f);
        }
        if (grond) {
            p.resetFallDistance();
        }
        s.wasSluip = sluip;
        s.wasGrond = grond;
        s.laatsteY = p.getY();
    }

    /** Mikai fires a blast at the player (it only shoves). */
    private static void stoot(ServerPlayer p, Spel s, Vec3 naar, double tempo) {
        s.schot = s.t;
        Vec3 van = s.mikai != null ? s.mikai.position().add(0, 2.2, 0) : Plek.staan(p, "duel_mikai");
        if (van == null) {
            return;
        }
        VuurbalEntity b = bal(p, s, van, VuurbalEntity.STOOT);
        if (b == null) {
            return;
        }
        b.setDeltaMovement(naar.subtract(van).normalize().scale(tempo));
        b.leven = 140;
        b.bijSlag = (bal, speler) -> slaTerug(speler, s, bal);
        if (s.mikai != null) {
            s.mikai.speel("vuurstoot");
        }
        Schijn.geluid(p, SoundEvents.BLAZE_SHOOT, BlockPos.containing(van), 0.8f, 0.8f);
    }

    /** A flame on a pillar top: douse it with a right click (water). */
    private static void vlam(ServerPlayer p, Spel s, Vec3 waar) {
        VuurbalEntity b = bal(p, s, waar, VuurbalEntity.VLAM);
        if (b != null) {
            b.leven = 20 * 120;
            b.bijKlik = (bal, speler) -> blus(speler, s, bal);
        }
    }

    /** A spark of the comet falls near the player. */
    private static void vonk(ServerPlayer p, Spel s, Vec3 jij) {
        Vec3 van = jij.add((p.getRandom().nextDouble() - 0.5) * 7, 12, (p.getRandom().nextDouble() - 0.5) * 7);
        VuurbalEntity b = bal(p, s, van, VuurbalEntity.VONK);
        if (b != null) {
            b.setDeltaMovement(0, -0.4, 0);
            b.leven = 50;
        }
    }

    @Nullable
    private static VuurbalEntity bal(ServerPlayer p, Spel s, Vec3 waar, int soort) {
        VuurbalEntity b = E2H4VuurFeature.VUURBAL.get().create(p.level(), EntitySpawnReason.TRIGGERED);
        if (b == null) {
            return null;
        }
        b.zetSoort(soort);
        b.snapTo(waar.x, waar.y, waar.z, 0, 0);
        E2H4Vuur.eigen(b, p);
        p.level().addFreshEntity(b);
        s.ballen.add(b);
        return b;
    }

    /** What the fire does near the player: a shove (never damage); a blast you hit back flies at Mikai. */
    private static void ballen(ServerPlayer p, Spel s, Vec3 jij) {
        for (VuurbalEntity b : List.copyOf(s.ballen)) {
            if (b.isRemoved()) {
                s.ballen.remove(b);
                continue;
            }
            if (b.soort() == VuurbalEntity.TERUG) {
                if (s.mikai != null && b.position().distanceTo(s.mikai.position().add(0, 1.5, 0)) < 2.2) {
                    b.discard();
                    s.mikai.speel("brul");
                    p.level().sendParticles(p, ParticleTypes.SOUL_FIRE_FLAME, true, false, s.mikai.getX(), s.mikai.getY() + 2, s.mikai.getZ(), 30, 0.8, 0.8,
                            0.8, 0.05);
                    Schijn.geluid(p, SoundEvents.GENERIC_EXPLODE.value(), s.mikai.blockPosition(), 0.4f, 1.6f);
                }
                continue;
            }
            if (b.soort() == VuurbalEntity.VLAM) {
                continue;
            }
            double raakAfstand = b.soort() == VuurbalEntity.VONK ? 1.3 : 1.5;
            if (b.position().distanceTo(jij) < raakAfstand) {
                b.discard();
                s.raak++;
                Duwtje.duw(p, b.getDeltaMovement(), b.soort() == VuurbalEntity.VONK ? 0.5 : 0.9);
                Schijn.geluid(p, SoundEvents.FIRECHARGE_USE, p.blockPosition(), 0.7f, 1.1f);
                if (s.ronde != AARDE && s.ronde != STAAT && s.raak >= RAAK_MAX) {
                    E2H4Vuur.zeg(p, "duel.opnieuw");
                    begin(p, s, s.ronde);
                    return;
                }
                E2H4Vuur.balk(p, "duel.duw");
            }
        }
    }

    private static void slaTerug(ServerPlayer p, Spel s, VuurbalEntity bal) {
        if ((s.ronde != VUUR && s.ronde != STAAT) || bal.soort() != VuurbalEntity.STOOT || bal.distanceTo(p) > 6) {
            return;
        }
        bal.zetSoort(VuurbalEntity.TERUG);
        bal.leven = 100;
        Vec3 naar = s.mikai != null ? s.mikai.position().add(0, 1.5, 0) : bal.position().add(bal.getDeltaMovement().scale(-10));
        bal.setDeltaMovement(naar.subtract(bal.position()).normalize().scale(0.9));
        s.gedaan++;
        Schijn.geluid(p, SoundEvents.FIRECHARGE_USE, p.blockPosition(), 1.0f, 1.6f);
        E2H4Vuur.balk(p, "duel.terug", s.gedaan, s.ronde == STAAT ? STAAT_TERUG : VUUR_TERUG);
    }

    private static void blus(ServerPlayer p, Spel s, VuurbalEntity bal) {
        if ((s.ronde != WATER && s.ronde != STAAT) || bal.distanceTo(p) > 7) {
            return;
        }
        p.level().sendParticles(p, ParticleTypes.SPLASH, true, false, bal.getX(), bal.getY() + 0.5, bal.getZ(), 40, 0.4, 0.4, 0.4, 0.1);
        p.level().sendParticles(p, ParticleTypes.CLOUD, true, false, bal.getX(), bal.getY() + 0.5, bal.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
        Schijn.geluid(p, SoundEvents.FIRE_EXTINGUISH, bal.blockPosition(), 1.0f, 1.0f);
        bal.discard();
        if (s.ronde == WATER) {
            s.gedaan++;
            E2H4Vuur.balk(p, "duel.geblust", s.gedaan, WATERPILAREN.length);
        } else {
            s.gedaan2++;
        }
    }

    /** The four elements circle round the player in the Guhvatar state. */
    private static void staatRing(ServerPlayer p, Spel s) {
        if (s.t % 2 != 0) {
            return;
        }
        double a = s.t * 0.25;
        net.minecraft.core.particles.ParticleOptions[] soorten = {ParticleTypes.CLOUD, ParticleTypes.SPLASH,
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), ParticleTypes.FLAME};
        for (int i = 0; i < 4; i++) {
            double h = a + i * Math.PI / 2;
            p.level().sendParticles(p, soorten[i], true, false, p.getX() + Math.cos(h) * 1.4, p.getY() + 1.0, p.getZ() + Math.sin(h) * 1.4, 1, 0, 0, 0, 0);
        }
    }

    /** A round won: the next one, the turning point after the fourth, the end after the fifth. */
    private static void gewonnen(ServerPlayer p, Spel s) {
        opruimen(s);
        Schijn.geluid(p, SoundEvents.PLAYER_LEVELUP, p.blockPosition(), 0.7f, 1.4f);
        if (s.ronde < VUUR) {
            E2H4Vuur.zeg(p, "duel.gewonnen." + s.ronde);
            begin(p, s, s.ronde + 1);
        } else if (s.ronde == VUUR) {
            E2H4Vuur.zeg(p, "duel.gewonnen." + s.ronde);
            s.pauze = true;
            if (!E2H4Vuur.speel(p, Scenes.OMSLAG, "duel_speler", q -> {
                s.pauze = false;
                begin(q, s, STAAT);
            })) {
                s.pauze = false;
                begin(p, s, STAAT);
            }
        } else {
            einde(p);
        }
    }

    /** The fifth round won: the hunger goes, the step of the finale moves on. */
    private static void einde(ServerPlayer p) {
        stop(p);
        E2Audio.stopMoment(p, 20);
        if (!E2H4Vuur.speel(p, Scenes.TREK_WEG, "duel_speler", Duel::naTrekWeg)) {
            naTrekWeg(p);
        }
    }

    private static void naTrekWeg(ServerPlayer p) {
        if (E2H4Vuur.FINALE.verder(p, E2H4Vuur.F_DUEL)) {
            Plek.breng(p, "rustpunt_2", 180f);
            E2H4Vuur.zeg(p, "duel.klaar");
        }
        Look.sync(p);
    }

    private static void opruimen(Spel s) {
        s.ballen.forEach(Entity::discard);
        s.ballen.clear();
    }

    static void stop(ServerPlayer p) {
        Spel s = SPELLEN.remove(p.getUUID());
        if (s != null) {
            opruimen(s);
            if (s.mikai != null) {
                s.mikai.discard();
            }
            Look.sync(p);
        }
    }

    /** (dev, tests) jump to a round (the duel starts when it isn't running). */
    static void naarRonde(ServerPlayer p, int ronde) {
        start(p);
        Spel s = SPELLEN.get(p.getUUID());
        if (s != null) {
            begin(p, s, Math.max(LUCHT, Math.min(STAAT, ronde)));
        }
    }

    /** (dev, tests) the duel as if every round was won (no scenes). */
    static void winVoorTest(ServerPlayer p) {
        stop(p);
        naTrekWeg(p);
    }
}
