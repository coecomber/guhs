package nl.juiced.guhs.feature.verhaal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Rotation;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.2): plays a {@link Cutscene} for one player. The actors are client-side
 * entities of the viewer only, so two players can watch at the same spot and the real NPCs stay where they are. While it
 * plays the player is locked ({@link Vast}); it cannot be skipped. It ends when the client reports the end or after
 * {@code duur + 100} ticks, and only then {@code daarna} runs on the server: advance the story step THERE, so a
 * disconnect in the middle simply replays the scene next time.
 * <pre>
 * if (LIJN.stap(p) == 2) {
 *     Cutscenes.speel(p, AANKOMST, anker, draai, speler -&gt; LIJN.verder(speler, 2));
 * }
 * </pre>
 * The first viewing remembers the anchor; {@link #herbekijk} (the Guhdex button) replays it there when the player is within
 * {@link #HERBEKIJK_AFSTAND} blocks of it in that dimension, and otherwise as a picture book (the scene's lines over its
 * narrator card). A replay never runs {@code daarna}.
 */
public final class Cutscenes {
    /** A replay plays in the world when the player is this close to where they first saw it. */
    public static final double HERBEKIJK_AFSTAND = 96;
    /** How many ticks before its end the client may report a scene as over (lag), and how long after it the server gives up. */
    public static final int MARGE = 40, UITLOOP = 100;

    private record Animatie(String naam, long sinds) {
    }

    /** Client: the animation each actor plays now (entity id to name), for the actors' own renderers. */
    private static final Map<Integer, Animatie> ANIMATIES = new ConcurrentHashMap<>();

    private static String key(String id) {
        return "guhs_scene_" + id;
    }

    /**
     * Plays the scene for this player with its anchor block and rotation (of the structure it stands in). False: the player
     * is already watching something (or is not alive); nothing happens then and {@code daarna} is not called.
     */
    public static boolean speel(ServerPlayer p, Cutscene s, BlockPos anker, Rotation draai, @Nullable Consumer<ServerPlayer> daarna) {
        return start(p, s, anker, draai, false, daarna);
    }

    private static boolean start(ServerPlayer p, Cutscene s, BlockPos anker, Rotation draai, boolean herhaling, @Nullable Consumer<ServerPlayer> daarna) {
        boolean ontvangt = Vast.ontvangt(p);
        Vast.Slot slot = new Vast.Slot(p, VerhaalPayloads.SCENE, s.id(), Math.max(0, s.duur() - MARGE), s.duur() + UITLOOP, ontvangt, speler -> {
            if (!herhaling) {
                CompoundTag t = GuhQuests.saved(speler).getCompoundOrEmpty(key(s.id())).copy();
                t.putBoolean("Gezien", true);
                GuhQuests.saved(speler).put(key(s.id()), t);
                VerhaalSync.sync(speler);
                if (daarna != null) {
                    daarna.accept(speler);
                }
            }
        });
        if (!Vast.zet(p, slot)) {
            return false;
        }
        if (!herhaling) {
            // where it was first seen (a replay plays there); "Gezien" only once it was watched to the end
            CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty(key(s.id())).copy();
            t.putString("Dim", p.level().dimension().identifier().toString());
            t.putLong("Pos", anker.asLong());
            t.putInt("Draai", draai.ordinal());
            GuhQuests.saved(p).put(key(s.id()), t);
        }
        ModNetworking.sendTo(p, new VerhaalPayloads.Speel(s.id(), anker, draai.ordinal()));
        return true;
    }

    /** Is this player watching a cutscene or reading a narrator card? Both sides (the client: the local player). */
    public static boolean bezig(Player p) {
        return Vast.is(p);
    }

    /** (client.VerhaalClient) the local player starts or stops watching a scene or a card. */
    public static void zetClientBezig(boolean bezig) {
        Vast.clientBezig = bezig;
    }

    /** Has this player watched the scene to the end? */
    public static boolean gezien(ServerPlayer p, String id) {
        return GuhQuests.saved(p).getCompoundOrEmpty(key(id)).getBooleanOr("Gezien", false);
    }

    /** The Guhdex button: watch a seen scene again (in the world near its anchor, else as a picture book). */
    public static void herbekijk(ServerPlayer p, String id) {
        Cutscene s = Cutscene.van(id);
        if (s == null || !gezien(p, id) || Vast.is(p)) {
            return;
        }
        CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty(key(id));
        BlockPos anker = BlockPos.of(t.getLongOr("Pos", 0L));
        boolean dichtbij = t.getStringOr("Dim", "").equals(p.level().dimension().identifier().toString())
                && anker.closerToCenterThan(p.position(), HERBEKIJK_AFSTAND);
        if (dichtbij) {
            Rotation draai = Rotation.values()[Math.floorMod(t.getIntOr("Draai", 0), Rotation.values().length)];
            start(p, s, anker, draai, true, null);
            return;
        }
        // too far away (or another dimension): the picture book
        int max = s.zinnen().size() * 60 + 20 * 120;
        if (Vast.zet(p, new Vast.Slot(p, VerhaalPayloads.BOEK, id, 0, max, Vast.ontvangt(p), speler -> {
        }))) {
            ModNetworking.sendTo(p, new VerhaalPayloads.Kaart(VerhaalPayloads.BOEK, id));
        }
    }

    /** (dev command, tests) forgets that this player saw the scene. */
    public static void vergeet(ServerPlayer p, String id) {
        GuhQuests.saved(p).remove(key(id));
        VerhaalSync.sync(p);
    }

    // =====================================================================================================================
    // client: what an actor plays (for the renderers and animation controllers of the cast)
    // =====================================================================================================================

    /**
     * Client: the animation a scene asked this actor to play ({@code Cutscene.Builder.animatie}), "" when none. An actor's
     * renderer or animation controller reads this (e.g. an NpcAnimator: {@code if (Cutscenes.animatie(npc).equals("buig")) ...}).
     */
    public static String animatie(Entity e) {
        Animatie a = ANIMATIES.get(e.getId());
        return a == null ? "" : a.naam();
    }

    /** Client: how many ticks ago that animation started (0 when none). */
    public static int animatieTicks(Entity e) {
        Animatie a = ANIMATIES.get(e.getId());
        return a == null ? 0 : (int) Math.max(0, e.level().getGameTime() - a.sinds());
    }

    /** (the client player of scenes) an actor starts an animation ("" = stops). */
    public static void zetAnimatie(Entity e, String naam) {
        if (naam == null || naam.isEmpty()) {
            ANIMATIES.remove(e.getId());
        } else {
            ANIMATIES.put(e.getId(), new Animatie(naam, e.level().getGameTime()));
        }
    }

    /** (the client player of scenes) the scene is over: no actor plays anything. */
    public static void wisAnimaties() {
        ANIMATIES.clear();
    }

    private Cutscenes() {
    }
}
