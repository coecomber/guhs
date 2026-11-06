package nl.juiced.guhs.feature.vadsrijk.imker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcHaken;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.wereld.Later;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * T-IMKER: the story round the secret boss. The quest {@code z3_geheim_imker} (z3-npc): five clues in Honingtak open the hatch
 * in the Raattros ({@code z3_imker_open}); down the ladder in the raatzaal the intro scene plays ({@link #intro}: the Mika
 * caught pouring grey "kaas-honing" into the queen's hive, a spy of the Keizer), and when it closes he wakes ({@link #maak}:
 * the arena's maker only makes him for a player who may fight him). His defeat reports {@code z3_imker:verslagen} (through
 * z3-bazen's moment "baas:imker" -> Z3Koppelingen) and plays the follow-up scene once ({@link #na}: the notebook, the queen
 * turns gold and sings, Zoempje). After the quest he can be fought again (respawn {@link #RESPAWN_S} s), without scenes.
 */
public final class ImkerVerhaal {
    public static final String ARENA = "z3imker:mika_imker";
    public static final String SLEUTEL = "t_imker", SLEUTEL_NA = "t_imker_na";
    public static final String QUEST = "z3_geheim_imker", OPEN = "z3_imker_open";
    public static final String GEZIEN_INTRO = "t_imker:intro", GEZIEN_NA = "t_imker:na";
    public static final String NAAM = "vadsrijk.z3imker.naam", NPC_NAAM = "vadsrijk.z3npc.imker.naam", VERTELLER = "vadsrijk.npc.verteller";
    public static final int STRAAL = 10, ONDER = 2, BOVEN = 10, RESPAWN_S = 600, LV = 47;
    /** Players reading the intro (until this game time at the latest). */
    private static final Map<UUID, Long> IN_INTRO = new ConcurrentHashMap<>();
    /** Dev: wake him once regardless of the story. */
    static volatile boolean aanvraag;

    private ImkerVerhaal() {
    }

    // ---- places ------------------------------------------------------------------------------------------------------------

    /** The middle of the raatzaal (the arena centre: where you stand on the floor). Integration regenerates the map. */
    public static BlockPos zaal() {
        BlockPos b = Z3WereldPlekken.plek("mika_imker_raatzaal");
        return b != null ? b : new BlockPos(1459, 145, -573);
    }

    /** The hatch in the Raattros floor (the way in and out; fainted fighters wake here). */
    public static BlockPos luik() {
        BlockPos b = Z3WereldPlekken.plek("mika_imker_luik");
        return b != null ? b : zaal().above(12);
    }

    public static Arena arena() {
        BlockPos c = zaal();
        // the board is hidden inside the floor (it only says "terug over" for a re-fight; the room is a secret)
        return new Arena(ARENA, c, STRAAL, luik(), RESPAWN_S, c.below(2), "vadsrijk.z3imker.titel", ONDER, BOVEN);
    }

    // ---- who may fight ----------------------------------------------------------------------------------------------------

    /** The story allows the fight: the quest is done (a re-fight), or it runs with the hatch open and the intro read. */
    public static boolean magVechten(ServerPlayer p) {
        if (Questlog.klaar(p, QUEST)) {
            return true;
        }
        return Questlog.actief(p, QUEST) && Questlog.vlag(p, OPEN) && Spelers.data(p).gezien.contains(GEZIEN_INTRO);
    }

    /** Is the intro due for this player (quest running, hatch open, not read yet)? */
    public static boolean introNodig(ServerPlayer p) {
        return Questlog.actief(p, QUEST) && Questlog.vlag(p, OPEN) && !Spelers.data(p).gezien.contains(GEZIEN_INTRO);
    }

    public static boolean leestIntro(ServerPlayer p) {
        Long t = IN_INTRO.get(p.getUUID());
        if (t == null) {
            return false;
        }
        if (p.level().getGameTime() > t) {
            introGelezen(p);   // (a closed client or a lost packet: never stuck)
            return false;
        }
        return true;
    }

    /** The arena's maker: only when someone inside may fight him (or a dev asked). */
    @Nullable
    static MikaImker maak(ServerLevel sl) {
        Arena a = Arenas.van(ARENA);
        boolean iemand = aanvraag;
        if (!iemand && a != null) {
            for (ServerPlayer p : sl.players()) {
                if (!p.isSpectator() && a.binnen(p.getX(), p.getY(), p.getZ()) && magVechten(p) && !leestIntro(p)) {
                    iemand = true;
                    break;
                }
            }
        }
        if (!iemand) {
            return null;
        }
        aanvraag = false;
        MikaImker m = ImkerRegistry.MIKA.get().create(sl, EntitySpawnReason.SPAWNER);
        if (m != null) {
            m.stelIn(LV, null, false, java.util.Set.of());
        }
        return m;
    }

    // ---- the intro --------------------------------------------------------------------------------------------------------

    /** From Z3Koppelingen.imkerTik (every 10 ticks, zone-3 players not in a scene): the intro when you come down the ladder. */
    public static void tik(ServerPlayer p) {
        if (!introNodig(p) || IN_INTRO.containsKey(p.getUUID())) {
            return;
        }
        if (NpcHaken.bij(p, zaal(), STRAAL - 1, 4)) {
            intro(p);
        }
    }

    public static void intro(ServerPlayer p) {
        String Z = "vadsrijk.z3imker.intro.";
        IN_INTRO.put(p.getUUID(), p.level().getGameTime() + 20 * 120);
        Praat.scene(p, SLEUTEL, List.of(new Praat.Regel(null, VERTELLER, Z + "0"), new Praat.Regel(null, NAAM, Z + "1"),
                new Praat.Regel(null, NAAM, Z + "2"), new Praat.Regel(null, VERTELLER, Z + "3"), new Praat.Regel(null, NAAM, Z + "4"),
                new Praat.Regel(null, NAAM, Z + "5")));
        if (p.level() instanceof ServerLevel sl) {
            Vec3 c = Vec3.atBottomCenterOf(zaal());
            sl.sendParticles(p, new DustParticleOptions(MikaImker.GRAUW, 1.6f), true, true, c.x, c.y + 1.5, c.z, 80, 4, 2, 4, 0);
            sl.playSound(null, zaal(), SoundEvents.BEEHIVE_WORK, SoundSource.PLAYERS, 1.5f, 0.7f);
        }
    }

    /** The intro was read (or closed): he wakes on the arena's next tick. */
    static void introGelezen(ServerPlayer p) {
        if (IN_INTRO.remove(p.getUUID()) != null || introNodig(p)) {
            Spelers.data(p).gezien.add(GEZIEN_INTRO);
        }
    }

    // ---- after the fight --------------------------------------------------------------------------------------------------

    /** He fled: the follow-up for everyone who helped (the quest step itself comes through z3-bazen's moment). */
    static void verslagen(MikaImker m, ServerLevel sl) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (m.bijdragen().spelers().contains(p.getUUID()) && p.distanceTo(m) < 64) {
                l.add(p);
            }
        }
        for (ServerPlayer p : l) {
            Later.na(20 * 3, () -> {
                if (!p.isRemoved()) {
                    na(p);
                }
            });
        }
    }

    /** The follow-up scene (once), afterwards a short line. */
    public static void na(ServerPlayer p) {
        if (!Spelers.data(p).gezien.add(GEZIEN_NA)) {
            p.sendSystemMessage(Component.translatable("vadsrijk.z3imker.weer_weg").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            return;
        }
        String Z = "vadsrijk.z3imker.na.";
        Praat.scene(p, SLEUTEL_NA, List.of(new Praat.Regel(null, VERTELLER, Z + "0"), new Praat.Regel(null, NAAM, Z + "1"),
                new Praat.Regel(null, VERTELLER, Z + "2"), new Praat.Regel(null, VERTELLER, Z + "3"), new Praat.Regel(null, VERTELLER, Z + "4"),
                new Praat.Regel(null, VERTELLER, Z + "5")));
        if (p.level() instanceof ServerLevel sl) {
            Vec3 c = Vec3.atBottomCenterOf(zaal());
            sl.sendParticles(p, new DustParticleOptions(MikaImker.HONING, 2.0f), true, true, c.x, c.y + 2, c.z, 120, 5, 2, 5, 0);
            sl.playSound(null, zaal(), SoundEvents.BEEHIVE_ENTER, SoundSource.PLAYERS, 1.5f, 1.4f);
            sl.playSound(null, zaal(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    // ---- the portrait --------------------------------------------------------------------------------------------------------

    /** q-dialoog's resolver: a page named after the Mika-imker shows him (his own model), also when he is not standing there. */
    @Nullable
    public static CompoundTag portret(String naamKey) {
        if (!NAAM.equals(naamKey) && !NPC_NAAM.equals(naamKey)) {
            return null;
        }
        CompoundTag t = new CompoundTag();
        t.putString("Type", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(ImkerRegistry.MIKA.get()).toString());
        return t;
    }

    static void init() {
        Praat.portretBron(ImkerVerhaal::portret);
        Praat.luister(SLEUTEL, (p, spreker, optie) -> {
            if (optie < 0) {
                introGelezen(p);
            }
        });
        Praat.luister(SLEUTEL_NA, (p, spreker, optie) -> {
        });
    }

    // ---- dev -----------------------------------------------------------------------------------------------------------------

    /** Dev: the quest running, all five clues, the hatch open, the scenes unseen. */
    static String klaarzetten(ServerPlayer p) {
        Quest q = Quests.van(QUEST);
        if (q == null) {
            return "Quest " + QUEST + " niet gevonden.";
        }
        Questlog.vergeet(p, QUEST);
        Questlog.neemAan(p, q);
        CompoundTag aw = Questlog.map(p, "z3_aanwijzingen");
        for (String id : List.of("rookpotjes", "imkerpak", "koningin", "grauwe_honing", "luik")) {
            aw.putBoolean(id, true);
        }
        Questlog.zetVlag(p, OPEN, true);
        vergeetScenes(p);
        return "Quest '" + QUEST + "' loopt, 5/5 sporen, het luik is open. Klim door het luik naar beneden: de scène start in de raatzaal, daarna wordt hij wakker.";
    }

    static void vergeetScenes(ServerPlayer p) {
        Spelers.data(p).gezien.remove(GEZIEN_INTRO);
        Spelers.data(p).gezien.remove(GEZIEN_NA);
        IN_INTRO.remove(p.getUUID());
    }
}
