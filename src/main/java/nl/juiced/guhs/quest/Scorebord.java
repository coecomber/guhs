package nl.juiced.guhs.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntFunction;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.core.UUIDUtil;
/**
 * The top 3 of every minigame, for the whole world: each player is on a board once (with their best score). A floating
 * scoreboard (a text display) shows them in the minigame's building; {@link #show} keeps it up to date.
 */
public final class Scorebord {
    public static final int PLACES = 3;
    /** Text displays of a scoreboard carry this tag (plus one with the text they show, so they're only replaced on a change). */
    public static final String TAG = "guhs_scorebord";

    /** One place on a board. */
    public record Entry(UUID player, String name, int score) {
    }

    /** 2.10: told about every score handed in (nieuwRecord = a new personal best); a listener must never throw. */
    @FunctionalInterface
    public interface Inzending {
        void ingezonden(ServerPlayer player, String board, int score, boolean lowerIsBetter, boolean nieuwRecord);
    }

    private static final List<Inzending> INZENDINGEN = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** 2.10: listen to every {@link #submit} (e.g. your guh dances at a record). */
    public static void opInzending(Inzending listener) {
        INZENDINGEN.add(listener);
    }

    /** Hands in a score; lowerIsBetter for times and strokes. Returns the place it got on the board (1-3), or 0. */
    public static int submit(ServerPlayer player, String board, int score, boolean lowerIsBetter) {
        Data data = Data.get(player.level().getServer());
        // your own best (for the Guhdex highscores), also when it never makes the top 3
        boolean personal = Highscores.remember(player, board, score, lowerIsBetter);
        List<Entry> before = data.boards.getOrDefault(board, List.of());
        Entry oldRecord = before.isEmpty() ? null : before.get(0);
        int place = enter(data, player, board, score, lowerIsBetter);
        List<Entry> after = data.boards.getOrDefault(board, List.of());
        if (!after.isEmpty() && !after.get(0).equals(oldRecord)) {
            Highscores.syncAll(player.level().getServer());                   // a new server record: everyone's Guhdex changes
        } else if (personal || place > 0) {
            Highscores.sync(player);
        }
        for (Inzending listener : INZENDINGEN) {
            try {
                listener.ingezonden(player, board, score, lowerIsBetter, personal);
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Scorebord listener failed", e);
            }
        }
        return place;
    }

    private static int enter(Data data, ServerPlayer player, String board, int score, boolean lowerIsBetter) {
        List<Entry> list = data.boards.computeIfAbsent(board, k -> new ArrayList<>());
        Entry old = list.stream().filter(e -> e.player().equals(player.getUUID())).findFirst().orElse(null);
        if (old != null && (lowerIsBetter ? old.score() <= score : old.score() >= score)) {
            return 0;
        }
        list.remove(old);
        list.add(new Entry(player.getUUID(), player.getGameProfile().name(), score));
        list.sort((a, b) -> lowerIsBetter ? Integer.compare(a.score(), b.score()) : Integer.compare(b.score(), a.score()));
        while (list.size() > PLACES) {
            list.remove(list.size() - 1);
        }
        data.setDirty();
        int place = list.indexOf(list.stream().filter(e -> e.player().equals(player.getUUID())).findFirst().orElse(null)) + 1;
        if (place > 0) {
            player.sendSystemMessage(Component.translatable("gui.guhs.scorebord.place", place).withStyle(ChatFormatting.GOLD));
        }
        return place;
    }

    public static List<Entry> top(MinecraftServer server, String board) {
        return List.copyOf(Data.get(server).boards.getOrDefault(board, List.of()));
    }

    /** A board as text: a title, then per board a heading and its top 3 (score shown with format). */
    public static Component text(MinecraftServer server, Component title, List<String> boards, List<Component> headings, IntFunction<String> format) {
        MutableComponent text = Component.empty().append(title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (int i = 0; i < boards.size(); i++) {
            if (headings.size() > i) {
                text.append("\n").append(headings.get(i).copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            List<Entry> list = top(server, boards.get(i));
            if (list.isEmpty()) {
                text.append("\n").append(Component.translatable("gui.guhs.scorebord.empty").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            for (int p = 0; p < list.size(); p++) {
                ChatFormatting colour = p == 0 ? ChatFormatting.YELLOW : p == 1 ? ChatFormatting.WHITE : ChatFormatting.GOLD;
                text.append("\n").append(Component.literal((p + 1) + ". " + list.get(p).name() + "  " + format.apply(list.get(p).score())).withStyle(colour));
            }
        }
        return text;
    }

    /**
     * Keeps a floating scoreboard at pos showing text: made when it isn't there, replaced when the text changed. Call it
     * now and then (e.g. every few seconds from the minigame guh's tick); id tells boards in the same spot apart.
     */
    public static void show(ServerLevel level, Vec3 pos, String id, Component text) {
        String json = Component.Serializer.toJson(text, level.registryAccess());
        String stamp = TAG + ":" + id + ":" + Integer.toHexString(json.hashCode());
        List<Display.TextDisplay> here = level.getEntitiesOfClass(Display.TextDisplay.class, new AABB(pos, pos).inflate(1.5),
                d -> d.entityTags().contains(TAG) && d.entityTags().stream().anyMatch(t -> t.startsWith(TAG + ":" + id + ":")));
        boolean current = false;
        for (Display.TextDisplay d : here) {
            if (!current && d.entityTags().contains(stamp)) {
                current = true;
            } else {
                d.discard();
            }
        }
        if (current) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:text_display");
        tag.putString("text", json);
        tag.putString("billboard", "vertical");
        tag.putString("alignment", "center");
        tag.putInt("line_width", 220);
        tag.putInt("background", 0x90301028);
        ListTag tags = new ListTag();
        tags.add(net.minecraft.nbt.StringTag.valueOf(TAG));
        tags.add(net.minecraft.nbt.StringTag.valueOf(stamp));
        tag.put("Tags", tags);
        Entity display = EntityType.loadEntityRecursive(tag, level, e -> {
            e.moveTo(pos.x, pos.y, pos.z, 0, 0);
            return e;
        });
        if (display != null) {
            level.addFreshEntity(display);
        }
    }

    /** The boards of the whole world (kept with the overworld). */
    public static class Data extends SavedData {
        final Map<String, List<Entry>> boards = new HashMap<>();

        public static Data get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), "guhs_scoreborden");
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag all = new CompoundTag();
            boards.forEach((board, list) -> {
                ListTag entries = new ListTag();
                for (Entry e : list) {
                    CompoundTag t = new CompoundTag();
                    t.store("Player", UUIDUtil.CODEC, e.player());
                    t.putString("Name", e.name());
                    t.putInt("Score", e.score());
                    entries.add(t);
                }
                all.put(board, entries);
            });
            tag.put("Boards", all);
            return tag;
        }

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            CompoundTag all = tag.getCompoundOrEmpty("Boards");
            for (String board : all.keySet()) {
                List<Entry> list = new ArrayList<>();
                for (Tag t : all.getListOrEmpty(board)) {
                    CompoundTag e = (CompoundTag) t;
                    list.add(new Entry(e.read("Player", UUIDUtil.CODEC).orElseThrow(), e.getStringOr("Name", ""), e.getIntOr("Score", 0)));
                }
                data.boards.put(board, list);
            }
            return data;
        }
    }

    private Scorebord() {
    }
}
