package nl.juiced.guhs.feature.einde2.zij;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.bouwzij2.Zij2Plekken;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.quest.Scorebord;

/**
 * einde2 zij: the Guhvleugel-ringenrace (DESIGN 4, no Avatar theme): the race master-guh on the start platform counts
 * down; fly through the eighteen rings in order ({@link Zij2Plekken#ringen}: a ring is passed when you cross its plane
 * within its radius) and land on the strip ({@code finish}). Your own record and the server record (the
 * {@link Scorebord} {@link #BORD}, shown on the wall {@code scorebord}); under {@link #PAR} the title {@code ringenracer}.
 * A missed ring or a fall costs nothing: talk to him again.
 */
public final class Ringenrace {
    static final String Q = Zij.Q + "race.";
    static final String S = Einde2Ids.GUHVLEUGEL_RINGENRACE;
    public static final String BORD = "einde2_zij_ringenrace", BESTE = "guhs_einde2_zij_race_beste";
    /** fix round 1: how far outside a ring's own radius a pass still counts (was 0.6): eighteen rings, so a near miss counts. */
    static final double RAND = 1.5;
    /** Ticks of the count-down; the time limit of a run; the par time of the title. */
    public static final int AFTEL = 60, MAX = 20 * 180, PAR = 20 * 45;

    /** One run: when it starts (after the count-down), which ring is next, where the racer was a tick ago. */
    static final class Run {
        final long start;
        int ring;
        Vec3 vorige;
        final ZijPlek.Kopie kopie;

        Run(long start, Vec3 waar, ZijPlek.Kopie kopie) {
            this.start = start;
            this.vorige = waar;
            this.kopie = kopie;
        }
    }

    private static final Map<UUID, Run> RUNS = new ConcurrentHashMap<>();

    private Ringenrace() {
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.RINGENRACEGUH, S + "/racemeester", new MeesterRol());
    }

    /** This player's own best time in ticks (0: none). */
    public static int beste(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(BESTE, 0);
    }

    static boolean titel(ServerPlayer p) {
        int b = beste(p);
        return b > 0 && b <= PAR;
    }

    static final class MeesterRol implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            npc.speel("vlag");
            int b = beste(p);
            Praat.open(p, npc, null, b > 0 ? Q + "welkom_terug" : Q + "welkom", new Object[]{Highscores.tijd(b)},
                    new Praat.Optie(1, Q + "optie.start"), new Praat.Optie(2, Q + "optie.uitleg"));
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
            if (optie == 2) {
                Praat.open(p, npc, null, Q + "uitleg", new Object[]{Highscores.tijd(PAR)}, new Praat.Optie(1, Q + "optie.start"));
            } else if (optie == 1) {
                Praat.sluit(p);
                start(p);
            }
        }
    }

    /** Starts a run for this player (true: started; they must stand on the start platform). */
    static boolean start(ServerPlayer p) {
        ZijPlek.Kopie k = ZijPlek.bij(p, S, 8);
        if (k == null || !k.in("start", p.position(), 3)) {
            Zij.melding(p, Q + "niet_op_start");
            return false;
        }
        RUNS.put(p.getUUID(), new Run(p.level().getGameTime() + AFTEL, p.position(), k));
        Zij.melding(p, Q + "klaar");
        return true;
    }

    /** Every tick of a racer. */
    static void tick(ServerPlayer p) {
        Run run = RUNS.get(p.getUUID());
        if (run == null) {
            return;
        }
        ServerLevel level = p.level();
        long nu = level.getGameTime();
        long t = nu - run.start;
        if (t < 0) {
            if (-t % 20 == 0) {
                long tel = -t / 20;
                Zij.titel(p, Component.literal(String.valueOf(tel)).withStyle(ChatFormatting.GOLD), null, 0, 15, 4);
                Zij.geluid(p, SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 0.8f);
            }
            run.vorige = p.position();
            return;
        }
        if (t == 0) {
            Zij.titel(p, Component.translatable(Q + "vlieg").withStyle(ChatFormatting.GREEN), null, 0, 20, 6);
            Zij.geluid(p, SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 1.6f);
        }
        List<Plekken.Plek> ringen = Zij2Plekken.ringen();
        Vec3 nuPos = p.position().add(0, p.getBbHeight() / 2, 0);
        if (run.ring < ringen.size()) {
            Plekken.Plek ring = ringen.get(run.ring);
            BlockPos c = Plekken.wereld(run.kopie.start(), ring);
            if (c != null && door(run.vorige, nuPos, Vec3.atCenterOf(c), ring.as(), ring.straal() + RAND)) {
                run.ring++;
                Zij.geluid(p, SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 0.8f + 0.06f * run.ring);
                level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, false, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 30, 2, 2, 2, 0.1);
                p.sendOverlayMessage(Component.translatable(Q + "ring", run.ring, ringen.size(), Highscores.tijd((int) t)).withStyle(ChatFormatting.YELLOW));
            } else if (t % 4 == 0 && c != null) {
                toonRing(p, level, c, ring.as(), ring.straal());
            }
        } else if (run.kopie.in("finish", p.position(), 1)) {
            RUNS.remove(p.getUUID());
            gefinisht(p, (int) t);
            return;
        }
        run.vorige = nuPos;
        if (t % 20 == 0 && run.ring < ringen.size()) {
            p.sendOverlayMessage(Component.translatable(Q + "ring", run.ring, ringen.size(), Highscores.tijd((int) t)).withStyle(ChatFormatting.YELLOW));
        }
        if (t > MAX || p.distanceToSqr(Vec3.atCenterOf(run.kopie.doos().getCenter())) > 260 * 260 || (p.onGround() && run.ring > 0 && run.ring < ringen.size()
                && !p.isFallFlying() && !p.isPassenger() && t > 40 && !run.kopie.in("start", p.position(), 2))) {
            RUNS.remove(p.getUUID());
            Zij.melding(p, Q + "mis");
            terugNaarStart(p, run.kopie);
        }
    }

    /** fix round 1: a run that ended costs no long flight back: a moment later the wind sets you on the start platform. */
    private static void terugNaarStart(ServerPlayer p, ZijPlek.Kopie kopie) {
        Vec3 plek = kopie.midden("start");
        if (plek == null) {
            return;
        }
        ZijEvents.later(40, () -> {
            if (!p.isAlive() || p.hasDisconnected() || RUNS.containsKey(p.getUUID()) || p.level() != kopie.level()) {
                return;
            }
            p.stopFallFlying();
            p.teleportTo(p.level(), plek.x, plek.y, plek.z, java.util.Set.of(), p.getYRot(), 0f, true);
            p.setDeltaMovement(Vec3.ZERO);
            p.level().sendParticles(p, ParticleTypes.CLOUD, true, false, plek.x, plek.y + 1, plek.z, 20, 0.5, 0.6, 0.5, 0.02);
            Zij.melding(p, Q + "terug");
        });
    }

    /** Did the line from a to b cross the ring's plane within this radius of its centre? */
    static boolean door(Vec3 a, Vec3 b, Vec3 midden, @Nullable Direction.Axis as, double straal) {
        Direction.Axis ax = as == null ? Direction.Axis.X : as;
        double va = a.get(ax) - midden.get(ax), vb = b.get(ax) - midden.get(ax);
        if (va == vb || Math.signum(va) == Math.signum(vb) && va != 0 && vb != 0) {
            return false;
        }
        double f = va / (va - vb);
        Vec3 snij = a.lerp(b, f);
        return snij.distanceToSqr(midden) <= straal * straal;
    }

    /** The next ring sparkles for its racer. */
    private static void toonRing(ServerPlayer p, ServerLevel level, BlockPos c, @Nullable Direction.Axis as, int straal) {
        Direction.Axis ax = as == null ? Direction.Axis.X : as;
        for (int i = 0; i < 16; i++) {
            double h = Math.PI * 2 * i / 16;
            double u = Math.cos(h) * straal, v = Math.sin(h) * straal;
            double x = c.getX() + 0.5 + (ax == Direction.Axis.X ? 0 : u);
            double y = c.getY() + 0.5 + v;
            double z = c.getZ() + 0.5 + (ax == Direction.Axis.Z ? 0 : (ax == Direction.Axis.X ? u : 0));
            level.sendParticles(p, ParticleTypes.END_ROD, true, false, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    private static void gefinisht(ServerPlayer p, int ticks) {
        int oud = beste(p);
        boolean record = oud <= 0 || ticks < oud;
        if (record) {
            GuhQuests.saved(p).putInt(BESTE, ticks);
        }
        Scorebord.submit(p, BORD, ticks, true);
        Zij.titel(p, Component.translatable(Q + "finish").withStyle(ChatFormatting.GOLD),
                Component.translatable(record ? Q + "finish_record" : Q + "finish_tijd", Highscores.tijd(ticks)), 4, 60, 10);
        Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 1f, 1.2f);
        p.level().sendParticles(p, ParticleTypes.FIREWORK, true, false, p.getX(), p.getY() + 1, p.getZ(), 40, 1, 1, 1, 0.15);
        if (ticks <= PAR && (oud <= 0 || oud > PAR)) {
            Zij.stem(p, Q + "naam", Q + "titel", Highscores.tijd(PAR));
        }
        nl.juiced.guhs.quest.GuhAdvancements.grant(p, "einde2_zij_ringenrace");
    }

    /** Every 100 ticks near the race: the wall shows the records. */
    static void bord(ServerPlayer p) {
        ZijPlek.Kopie k = ZijPlek.bij(p, S, 48);
        BlockPos b = k == null ? null : k.pos("scorebord");
        if (b == null) {
            return;
        }
        Component tekst = Scorebord.text(p.level().getServer(), Component.translatable(Q + "bord"), List.of(BORD), List.of(), Highscores::tijd);
        Scorebord.show(p.level(), new Vec3(b.getX() - 0.6, b.getY() + 1.2, b.getZ() + 0.5), BORD, tekst);
    }

    static void vergeet(UUID id) {
        RUNS.remove(id);
    }

    /** (tests) the run of this player (null: none). */
    @Nullable
    static Run run(UUID id) {
        return RUNS.get(id);
    }
}
