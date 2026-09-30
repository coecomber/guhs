package nl.juiced.guhs.feature.kamperen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.sterrenwacht.Buiten;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * Opa Guh's twelve campfire stories (the verhalenbundel): about the first kaasknabbel, the Mika's who can't share, the
 * big ears of the guhs, the Knabbelberg... He tells one per night (per player: the next one they haven't heard yet;
 * when they've heard them all, any one again). A story is {@value #REGELS} lines, one every {@value #INTERVAL} ticks,
 * for everyone listening within {@value #LUISTER_AFSTAND} blocks; guhs nearby come and sit by the fire. At the end
 * each listener gets the story in the verhalenbundel and a few kaasknabbels.
 * <p>
 * Per player (GuhQuests.saved, {@value #KEY}): the night of their last story.
 */
public final class Verhalen {
    public static final String KEY = "guhs_kamperen";
    public static final String BUNDEL = "verhalenbundel";
    public static final List<String> IDS = List.of("eerste_kaasknabbel", "mika_die_niet_kon_delen", "groot_vadsfeest", "pluk_niet_vahoeg",
            "grote_oren", "knabbelberg", "grote_knabbel", "wolkjes_eerste_vlucht", "kaasregen", "verdwaalde_babyguh", "mika_met_gouden_hart",
            "knuffeldal");
    /** Lines per story, and ticks between two lines. */
    public static final int REGELS = 7, INTERVAL = 90;
    /** A listener must hear at least this many lines (half the story) for the bundel and the kaasknabbels. */
    public static final int GENOEG = (REGELS + 1) / 2;
    public static final double LUISTER_AFSTAND = 16;
    /** Guhs this close to Opa come and listen. */
    public static final double GUH_AFSTAND = 14;
    // Knus counters
    public static final String GEVONDEN = "kamperen.gevonden", GEHOORD = "kamperen.verhalen", AANTAL = "kamperen.bundel",
            UITGESLAPEN = "kamperen.uitgeslapen", PYJAMAGUHS = "kamperen.pyjamaguhs";

    /** A story being told at one Opa Guh. */
    static final class Voorlezing {
        final int verhaal;
        /** Listener -> lines heard so far. */
        final Map<UUID, Integer> luisteraars = new LinkedHashMap<>();
        int regel;
        long volgende;

        Voorlezing(int verhaal, long nu) {
            this.verhaal = verhaal;
            this.volgende = nu + 30;
        }
    }

    private static final Map<UUID, Voorlezing> BEZIG = new ConcurrentHashMap<>();

    private Verhalen() {
    }

    /** The night number (from noon to noon). */
    public static long nacht(Level level) {
        return Math.floorDiv(level.getDayTime() + 12000L, 24000L);
    }

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (!saved.contains(KEY)) {
            saved.put(KEY, new CompoundTag());
        }
        return saved.getCompound(KEY);
    }

    /** Has the player heard a story tonight already? */
    public static boolean vannachtGehoord(ServerPlayer player) {
        CompoundTag d = data(player);
        return d.contains("Nacht") && d.getLong("Nacht") == nacht(player.level());
    }

    /** (Tests) forgets tonight's story. */
    public static void vergeetVannacht(ServerPlayer player) {
        data(player).remove("Nacht");
    }

    /** The story this player hears next: the first one they don't know yet, or (all heard) any one. */
    public static int volgende(ServerPlayer player) {
        Set<String> bundel = KnusVoortgang.ontdekt(player, BUNDEL);
        for (int i = 0; i < IDS.size(); i++) {
            if (!bundel.contains(IDS.get(i))) {
                return i;
            }
        }
        return player.getRandom().nextInt(IDS.size());
    }

    /** Is this Opa telling a story right now? */
    public static boolean vertelt(GuhNpcEntity opa) {
        return BEZIG.containsKey(opa.getUUID());
    }

    /** Is the player listening to a story somewhere? */
    public static boolean luistert(ServerPlayer player) {
        for (Voorlezing v : BEZIG.values()) {
            if (v.luisteraars.containsKey(player.getUUID())) {
                return true;
            }
        }
        return false;
    }

    /** Which story this Opa tells (-1: none). */
    public static int verhaal(GuhNpcEntity opa) {
        Voorlezing v = BEZIG.get(opa.getUUID());
        return v == null ? -1 : v.verhaal;
    }

    /**
     * Starts a story at this Opa for the player (or lets them join the one he's telling). Only who can still hear at
     * least {@value #GENOEG} lines uses up their story of tonight; a late listener may sit along, but gets nothing.
     */
    public static void begin(GuhNpcEntity opa, ServerPlayer player, int verhaal) {
        Voorlezing v = BEZIG.get(opa.getUUID());
        boolean telt = true;
        if (v == null) {
            v = new Voorlezing(verhaal, opa.level().getGameTime());
            BEZIG.put(opa.getUUID(), v);
            GuhQuests.say(player, opa, "quest.guhs.kamperen.begin", net.minecraft.network.chat.Component.translatable("gui.guhs.knus." + BUNDEL + "." + IDS.get(verhaal)));
        } else if (REGELS - v.regel >= GENOEG) {
            GuhQuests.say(player, opa, "quest.guhs.kamperen.aanschuiven");
        } else {
            telt = false;
            GuhQuests.say(player, opa, "quest.guhs.kamperen.te_laat");
        }
        v.luisteraars.putIfAbsent(player.getUUID(), 0);
        if (telt) {
            data(player).putLong("Nacht", nacht(player.level()));
        }
    }

    /** (Tests) how many lines of the story at this Opa have been told (-1: none). */
    static int regel(GuhNpcEntity opa) {
        Voorlezing v = BEZIG.get(opa.getUUID());
        return v == null ? -1 : v.regel;
    }

    /** Every tick of an Opa (server): the next line when it's time; guhs come and listen; the end. */
    static void tick(GuhNpcEntity opa) {
        Voorlezing v = BEZIG.get(opa.getUUID());
        if (v == null || !(opa.level() instanceof ServerLevel level)) {
            return;
        }
        long nu = level.getGameTime();
        if (nu % 40 == 0) {
            lokGuhs(opa, level);
        }
        if (nu < v.volgende) {
            if (nu % 8 == 0) {
                BlockPos vuur = KampvuurPyjama.kampvuurBij(level, opa.blockPosition(), 8);
                if (vuur != null) {
                    level.sendParticles(KamperenFeature.KAMPVUURVONKJE.get(), vuur.getX() + 0.5, vuur.getY() + 0.8, vuur.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.01);
                }
            }
            return;
        }
        List<ServerPlayer> hier = luisteraars(opa, v);
        if (hier.isEmpty()) {
            BEZIG.remove(opa.getUUID());       // everybody walked off: he stops (and finishes tomorrow, njeg)
            return;
        }
        String id = IDS.get(v.verhaal);
        lokGuhs(opa, level);
        for (ServerPlayer p : hier) {
            GuhQuests.say(p, opa, "quest.guhs.kamperen.verhaal." + id + "." + v.regel);
            v.luisteraars.merge(p.getUUID(), 1, Integer::sum);
        }
        level.playSound(null, opa, KamperenFeature.OPA_VERHAAL.get(), SoundSource.NEUTRAL, 0.9f, 0.9f + level.random.nextFloat() * 0.1f);
        v.regel++;
        v.volgende = nu + INTERVAL;
        if (v.regel >= REGELS) {
            BEZIG.remove(opa.getUUID());
            int pyjamas = level.getEntitiesOfClass(GuhEntity.class, opa.getBoundingBox().inflate(GUH_AFSTAND),
                    g -> GuhHooks.heeft(g, GuhHooks.PYJAMA)).size();
            for (ServerPlayer p : hier) {
                if (v.luisteraars.getOrDefault(p.getUUID(), 0) >= GENOEG) {
                    klaar(p, opa, v.verhaal, pyjamas);
                } else {
                    GuhQuests.say(p, opa, "quest.guhs.kamperen.einde_half");   // only a little bit heard: no reward
                }
            }
            level.sendParticles(ParticleTypes.HEART, opa.getX(), opa.getY() + 1.6, opa.getZ(), 5, 0.4, 0.3, 0.4, 0.02);
        }
    }

    private static List<ServerPlayer> luisteraars(GuhNpcEntity opa, Voorlezing v) {
        return ((ServerLevel) opa.level()).players().stream()
                .filter(p -> v.luisteraars.containsKey(p.getUUID()) && p.distanceTo(opa) <= LUISTER_AFSTAND && p.isAlive()).toList();
    }

    /** The guhs nearby come and sit by the fire (for a while: they stay as long as the story goes on). */
    private static void lokGuhs(GuhNpcEntity opa, ServerLevel level) {
        BlockPos vuur = KampvuurPyjama.kampvuurBij(level, opa.blockPosition(), 8);
        BlockPos doel = vuur != null ? vuur : opa.blockPosition();
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, new AABB(opa.blockPosition()).inflate(GUH_AFSTAND),
                g -> !g.isOrderedToSit() && !g.isPassenger())) {
            LuisterGoal.luister(guh, doel, 60);
        }
    }

    /** The end of a story for one listener: the verhalenbundel, kaasknabbels. Returns the kaasknabbels given. */
    public static int klaar(ServerPlayer player, @Nullable GuhNpcEntity opa, int verhaal, int pyjamaGuhs) {
        String id = IDS.get(verhaal);
        boolean nieuw = KnusVoortgang.ontdek(player, BUNDEL, id);
        int knabbels = knabbels(nieuw);
        Minigames.give(player, new ItemStack(ModItems.KAAS_KNABBELS.get(), knabbels));
        data(player).putLong("Nacht", nacht(player.level()));
        KnusVoortgang.tel(player, GEHOORD, 1);
        int bundel = KnusVoortgang.ontdekt(player, BUNDEL).size();
        KnusVoortgang.hoogste(player, AANTAL, bundel);
        KnusVoortgang.hoogste(player, PYJAMAGUHS, pyjamaGuhs);
        GuhAdvancements.grant(player, "kamperen_eerste_verhaal");
        Buiten.toon(player, "kamperen_eerste_verhaal");
        if (bundel >= IDS.size()) {
            GuhAdvancements.grant(player, "kamperen_alle_verhalen");
            Buiten.toon(player, "kamperen_alle_verhalen");
        }
        if (opa != null) {
            GuhQuests.say(player, opa, nieuw ? "quest.guhs.kamperen.einde_nieuw" : "quest.guhs.kamperen.einde", knabbels);
        }
        return knabbels;
    }

    /** "+1 per reward": 1 for listening, 2 more for a new story, + 1. */
    public static int knabbels(boolean nieuw) {
        return 1 + (nieuw ? 2 : 0) + 1;
    }

    static void vergeetAlles() {
        BEZIG.clear();
    }

    /** (Tests) makes the story at this Opa go on right now. */
    static void nu(GuhNpcEntity opa) {
        Voorlezing v = BEZIG.get(opa.getUUID());
        if (v != null) {
            v.volgende = 0;
        }
    }
}
