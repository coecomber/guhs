package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * The mount return quests (fix round 2, g-werver; NOTITIES_G_MOUNTS): "Het Knuffelhert" (zone 1, Boswachter Mos),
 * "De verdwaalde Guhwolf" (zone 2, Breister Wolwinter) and (I-MOUNTS) "Een eigen Wolkenschaapje" (zone 1, Stalmeester Hoefje).
 * Mounts start with the zone-3 starter Pluimguh, so they appear once the player owns any mount: a quest whose {@code na}
 * holds {@link #POORT} is only available while {@link Rijdieren#heeftEen} ({@link Questlog#beschikbaar}). Texts and objectives:
 * vadsrijk_npc.py / vadsrijk_z2_npc.py; the reward is the action {@code rijdier_geef:<soort>} ({@link Rijdieren#geef}).
 * <p>This class adds the little moments on top of the normal objectives: a lantern lights up, the glowing hoof prints that lead
 * to the glade, the deer and the wolf pup (narrator pages), and the tracker line while such a quest is available.
 */
public final class RijdierQuests {
    /** The {@code na} entry that means "owns a mount". */
    public static final String POORT = "@rijdier";
    public static final String HERT = "rijdier_knuffelhert", WOLF = "z2_rijdier_guhwolf", SCHAAP = "rijdier_hoedhupper";
    /** Knuffelhert: objectives 0..2 = the lanterns (GEBIED poi), 3 = the glade (MELD hert:open_plek). */
    static final String[] LANTAARNS = {"hert_lantaarn_1", "hert_lantaarn_2", "hert_lantaarn_3"};
    static final String OPEN_PLEK = "hert_open_plek";
    static final String T = "vadsrijk.npc.rijdierquest.";
    /** The objectives already seen done (quest -> bits), so a moment plays once, right when an objective completes. */
    private static final Map<UUID, Map<String, Integer>> GEZIEN = new HashMap<>();

    private RijdierQuests() {
    }

    static void init() {
        QuestMotor.ACTIES.put("rijdier_geef", (p, soort) -> Rijdieren.geef(p, soort));
    }

    /**
     * The {@link #POORT} condition. 1.10.2 (user decision): gone. The return quests unlock by their normal level/quest prerequisites;
     * riding the reward waits for the zone-3 Pluimstal ({@code Rijdieren.magRijden}). (So no teaser any more either.)
     */
    static boolean poortOpen(ServerPlayer p) {
        return true;
    }

    /**
     * I-MOUNTS: the teaser. A return quest of this NPC that is still locked only because the player has no mount yet (not taken,
     * the other conditions may still be open): the NPC tells about it ({@code vadsrijk.npc.rijdierquest.teaser.<quest>}). Null = none.
     */
    @Nullable
    public static Quest teaser(ServerPlayer p, String npc) {
        if (poortOpen(p)) {
            return null;
        }
        for (Quest q : Quests.alle().values()) {
            if (isRijdierQuest(q) && q.gever().equals(npc) && Questlog.status(p, q.id()) == 0) {
                return q;
            }
        }
        return null;
    }

    static boolean isRijdierQuest(Quest q) {
        return q.na().contains(POORT);
    }

    /** The first return quest that is active or may be started now (for the tracker); null = none. */
    @Nullable
    public static Quest trackerQuest(ServerPlayer p) {
        for (Quest q : Quests.alle().values()) {
            if (isRijdierQuest(q) && (Questlog.actief(p, q.id()) || Questlog.beschikbaar(p, q))) {
                return q;
            }
        }
        return null;
    }

    // ---- the moments --------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 13) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || RijkScenes.bezig(p) || Praat.bezig(p)) {
                continue;
            }
            Quest hert = Quests.van(HERT);
            if (hert != null && Questlog.actief(p, HERT)) {
                hert(p, sl, hert);
            }
            Quest wolf = Quests.van(WOLF);
            if (wolf != null && Questlog.actief(p, WOLF)) {
                int nieuw = nieuw(p, wolf);
                if ((nieuw & 1) != 0) {
                    verteller(p, "wolf.gevonden", 3);
                    sl.sendParticles(p, ParticleTypes.SNOWFLAKE, false, false, p.getX(), p.getY() + 1, p.getZ(), 30, 1.5, 0.8, 1.5, 0.02);
                    sl.playSound(null, p.blockPosition(), SoundEvents.FOX_SNIFF, SoundSource.NEUTRAL, 1f, 1.6f);
                }
            }
        }
    }

    private static void hert(ServerPlayer p, ServerLevel sl, Quest q) {
        int nieuw = nieuw(p, q);
        int aan = 0;
        for (int i = 0; i < LANTAARNS.length; i++) {
            if (Questlog.doelKlaar(p, q, i)) {
                aan++;
            }
        }
        for (int i = 0; i < LANTAARNS.length; i++) {
            if ((nieuw & (1 << i)) != 0) {
                BlockPos b = NpcPlekken.poi(LANTAARNS[i]);
                if (b != null) {
                    sl.sendParticles(ParticleTypes.END_ROD, b.getX() + 0.5, b.getY() + 1.5, b.getZ() + 0.5, 40, 0.3, 1.5, 0.3, 0.04);
                    sl.sendParticles(ParticleTypes.FLAME, b.getX() + 0.5, b.getY() + 1.2, b.getZ() + 0.5, 12, 0.15, 0.2, 0.15, 0.01);
                }
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.8f + 0.2f * aan);
                p.sendOverlayMessage(Component.translatable(T + "lantaarn", aan, LANTAARNS.length).withStyle(ChatFormatting.GOLD));
                if (aan == LANTAARNS.length) {
                    verteller(p, "hert.sporen", 2);
                    return;
                }
            }
        }
        if ((nieuw & (1 << 3)) != 0) {
            verteller(p, "hert.open_plek", 3);
            sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.2, p.getZ(), 12, 1.5, 0.6, 1.5, 0.05);
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.4f);
            return;
        }
        BlockPos plek = NpcPlekken.poi(OPEN_PLEK);
        if (aan < LANTAARNS.length || plek == null || Questlog.doelKlaar(p, q, 3)) {
            return;
        }
        if (NpcHaken.bij(p, plek, NpcPlekken.POI_STRAAL, NpcPlekken.POI_HOOGTE)) {
            NpcApi.meld(p, "hert", "open_plek");   // (the moment plays on the next tick, when the bit turns up)
            return;
        }
        // the deer's glowing hoof prints: a trail from you towards the glade (only you see it)
        Vec3 naar = new Vec3(plek.getX() + 0.5 - p.getX(), 0, plek.getZ() + 0.5 - p.getZ());
        double afstand = naar.length();
        Vec3 r = naar.normalize();
        for (int k = 2; k <= Math.min(14, afstand); k += 3) {
            double x = p.getX() + r.x * k + (k % 2 == 0 ? r.z : -r.z) * 0.35, z = p.getZ() + r.z * k - (k % 2 == 0 ? r.x : -r.x) * 0.35;
            sl.sendParticles(p, ParticleTypes.END_ROD, false, false, x, p.getY() + 0.15, z, 2, 0.05, 0.02, 0.05, 0.0);
            sl.sendParticles(p, ParticleTypes.CHERRY_LEAVES, false, false, x, p.getY() + 0.4, z, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    /** The objectives of q that are done now but were not the last time (0 the first time: no moments for old progress). */
    private static int nieuw(ServerPlayer p, Quest q) {
        int bits = 0;
        for (int i = 0; i < q.doelen().size() && i < 31; i++) {
            if (Questlog.doelKlaar(p, q, i)) {
                bits |= 1 << i;
            }
        }
        Map<String, Integer> m = GEZIEN.computeIfAbsent(p.getUUID(), u -> new HashMap<>());
        Integer oud = m.put(q.id(), bits);
        return oud == null ? 0 : bits & ~oud;
    }

    /** A few narrator pages (vadsrijk.npc.rijdierquest.<id>.0..n-1). */
    static void verteller(ServerPlayer p, String id, int n) {
        List<Praat.Regel> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            l.add(new Praat.Regel(null, "vadsrijk.npc.verteller", T + id + "." + i));
        }
        QuestMotor.open(p, null, l, new QuestMotor.Gesprek("", -1, "", "", ""));
    }

    static void vergeet(UUID u) {
        GEZIEN.remove(u);
    }
}
