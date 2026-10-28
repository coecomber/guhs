package nl.juiced.guhs.feature.droomwezens;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.8.0 Guhdromenland (bouwplan C): the dream NPCs and how they talk.
 * <ul>
 *   <li>{@link #KINDS}: Snoes (zwerf / blij), the Zandguh, the Wekkerguh, the Rommelguh, Oma-guh knitting, the kussengevecht-guh.</li>
 *   <li>{@link #praat}: what happens when a player right-clicks one. By default it says the next of its jokes
 *       (lang {@code gui.guhs.droomwezens.<kind>.grap.<n>}, in turn per NPC). The story (bouwplan F) puts its own talk on top with
 *       {@link #zet}: return true when it handled the click, false to let the joke come.</li>
 *   <li>{@link #spawn}: one of them at a spot (structures, cutscenes, tests, /guhs droomwezens spawn).</li>
 * </ul>
 * The kussengevecht-guh also throws a {@link KussenProjectiel} now and then ({@link DroomNpcRol#tick}); the Wekkerguh rings
 * ("rinkel") when you talk to him.
 */
public final class DroomNpc {
    /** A talk of the story: true = handled (no joke). */
    @FunctionalInterface
    public interface Praat {
        boolean praat(ServerPlayer speler, GuhNpcEntity npc);
    }

    public static final List<Kind> KINDS = List.of(Kind.SNOES, Kind.SNOES_BLIJ, Kind.ZANDGUH, Kind.WEKKERGUH, Kind.ROMMELGUH,
            Kind.DROOM_OMAGUH, Kind.KUSSENGEVECHT_GUH);
    /** How many jokes each kind has (gui.guhs.droomwezens.&lt;kind&gt;.grap.0..n-1; tools/features/droomwezens.py GRAPPEN). */
    public static final Map<Kind, Integer> GRAPPEN = Map.of(Kind.SNOES, 4, Kind.SNOES_BLIJ, 4, Kind.ZANDGUH, 3, Kind.WEKKERGUH, 4,
            Kind.ROMMELGUH, 4, Kind.DROOM_OMAGUH, 4, Kind.KUSSENGEVECHT_GUH, 4);
    /** The roleData key of the next joke of an NPC. */
    static final String VOLGENDE = "guhs_droom_grap";

    private static final Map<Kind, Praat> VERHAAL = new ConcurrentHashMap<>();

    private DroomNpc() {
    }

    /** The story's talk for a kind (replaces an earlier one). */
    public static void zet(Kind kind, Praat praat) {
        VERHAAL.put(kind, praat);
    }

    /** The id the test command and the jokes use ("snoes", "zandguh", ...). */
    public static Kind van(String naam) {
        for (Kind k : KINDS) {
            if (k.id().equals(naam)) {
                return k;
            }
        }
        return null;
    }

    /** A player right-clicked one of the dream NPCs: the story's talk, or else its next joke. */
    public static void praat(ServerPlayer speler, GuhNpcEntity npc) {
        Praat verhaal = VERHAAL.get(npc.getKind());
        if (verhaal != null && verhaal.praat(speler, npc)) {
            return;
        }
        reactie(npc);
        grap(speler, npc);
    }

    /** The next joke of this NPC (they come in turn). */
    public static void grap(ServerPlayer speler, GuhNpcEntity npc) {
        int n = GRAPPEN.getOrDefault(npc.getKind(), 0);
        if (n <= 0) {
            return;
        }
        int keer = npc.roleData.getIntOr(VOLGENDE, 0);          // (how many it told: the next one is keer % n)
        int i = Math.floorMod(keer, n);
        npc.roleData.putInt(VOLGENDE, keer + 1);
        GuhQuests.say(speler, npc, "gui.guhs.droomwezens." + npc.getKind().id() + ".grap." + i);
    }

    /** Its little reaction when talked to: the Wekkerguh rings, the kussengevecht-guh swings, the others a happy piep. */
    static void reactie(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        switch (npc.getKind()) {
            case WEKKERGUH -> {
                npc.speel("rinkel");
                level.playSound(null, npc, DroomwezensFeature.RINKEL.get(), SoundSource.NEUTRAL, 0.9f, 1f);
            }
            case KUSSENGEVECHT_GUH -> {
                npc.speel("gooi");
                level.sendParticles(ParticleTypes.CLOUD, npc.getX(), npc.getY() + 0.9, npc.getZ(), 6, 0.3, 0.2, 0.3, 0.01);
            }
            case SNOES -> level.playSound(null, npc, nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.6f, 0.8f);
            default -> level.playSound(null, npc, nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.9f, 1.1f);
        }
    }

    /** One of them at this spot, looking the given way (no AI wandering: a guh character stays put like the other NPCs). */
    public static GuhNpcEntity spawn(ServerLevel level, Vec3 at, float yaw, Kind kind) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
        npc.setKind(kind);
        npc.snapTo(at.x, at.y, at.z, yaw, 0);
        npc.yHeadRot = yaw;
        npc.yBodyRot = yaw;
        level.addFreshEntity(npc);
        return npc;
    }

    /** The roles of the kinds (NpcRollen: these are newer than the 3.0 kinds, so their role comes from there). */
    static void rollen() {
        for (Kind k : KINDS) {
            NpcRollen.zet(k, DroomNpcRol.INSTANCE);
        }
    }

    /** The default role: talk = {@link #praat}; the kussengevecht-guh throws pillows now and then. */
    static final class DroomNpcRol implements NpcRole {
        static final DroomNpcRol INSTANCE = new DroomNpcRol();
        /** Ticks between two throws of a kussengevecht-guh (random in this range). */
        static final int GOOI_MIN = 80, GOOI_MAX = 200;
        static final String GOOI_OP = "guhs_droom_gooi";

        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            praat(player, npc);
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            if (npc.getKind() != Kind.KUSSENGEVECHT_GUH || !(npc.level() instanceof ServerLevel level)) {
                return;
            }
            long nu = level.getGameTime();
            long op = npc.roleData.getLongOr(GOOI_OP, 0L);
            if (op == 0L) {
                npc.roleData.putLong(GOOI_OP, nu + GOOI_MIN + npc.getRandom().nextInt(GOOI_MAX - GOOI_MIN));
                return;
            }
            if (nu < op) {
                return;
            }
            npc.roleData.putLong(GOOI_OP, nu + GOOI_MIN + npc.getRandom().nextInt(GOOI_MAX - GOOI_MIN));
            gooi(level, npc);
        }

        /** Throws a pillow at the nearest other kussengevecht-guh (or a player close by); false when nobody is there. */
        static boolean gooi(ServerLevel level, GuhNpcEntity npc) {
            AABB box = npc.getBoundingBox().inflate(10);
            net.minecraft.world.entity.LivingEntity doel = null;
            double best = Double.MAX_VALUE;
            for (GuhNpcEntity ander : level.getEntitiesOfClass(GuhNpcEntity.class, box, e -> e != npc && e.getKind() == Kind.KUSSENGEVECHT_GUH)) {
                double d = ander.distanceToSqr(npc);
                if (d < best) {
                    best = d;
                    doel = ander;
                }
            }
            if (doel == null) {
                var speler = level.getNearestPlayer(npc, 6);
                if (speler == null || speler.isSpectator()) {
                    return false;
                }
                doel = speler;
            }
            npc.speel("gooi");
            npc.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, doel.getEyePosition());
            KussenProjectiel kussen = new KussenProjectiel(level, npc);
            Vec3 van = npc.position().add(0, 0.9, 0);
            kussen.setPos(van.x, van.y, van.z);
            Vec3 naar = doel.position().add(0, doel.getBbHeight() * 0.6, 0).subtract(van);
            kussen.shoot(naar.x, naar.y + naar.horizontalDistance() * 0.18, naar.z, 0.7f, 2f);
            level.addFreshEntity(kussen);
            level.playSound(null, npc, DroomwezensFeature.KUSSEN.get(), SoundSource.NEUTRAL, 0.7f, 1.3f);
            return true;
        }
    }
}
