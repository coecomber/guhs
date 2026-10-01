package nl.juiced.guhs.quest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.network.MaagPayloads;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.ModDimensions;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Reisguhs: guh waypoints in the Guhmension. Right-click a Reisguh once to discover it; after that a right-click opens
 * its menu: rename it, or travel to any other Reisguh you have discovered. They only work in the Guhmension. One stands
 * next to every new guh portal there, one at the guh castle, and you can put down your own with a Reisguh whistle.
 * 2.10.1: one also lives in the big places (Guhwarden at the Elf-Guhjestocht, the Knuffeldal stadje, the Guhkermis, Guhland,
 * the Ballonfestival, the Guhcircuit: they come with the structure templates, tools/features/reisguh_plek.py), and a wild
 * one turns up a bit more often ({@link #WILD_EEN_OP}, {@link #WILD_AFSTAND}).
 */
public final class Reisguh {
    public static final int MAX_NAME = 32;
    /** Actions from the screen. */
    public static final int RENAME = 0, TRAVEL = 1;
    /** 2.10.1: a wild Reisguh turns up with one in this many guh groups spawning in the Guhmension (was 250)... */
    public static final int WILD_EEN_OP = 120;
    /** ...but never closer than this to another Reisguh (was 200). */
    public static final int WILD_AFSTAND = 160;

    /** One waypoint: where its Reisguh sits, which way it looks, what it's called (1.2.0: a Component, see {@link #vanOud}). */
    public record Point(UUID id, Component name, BlockPos pos, float yaw) {
    }

    /**
     * 1.2.0: the place names the structure templates give their Reisguh (ReisName, tools/features/reisguh_plek.py) are
     * Dutch strings; they become the lang keys entity.guhs.reisguh.plek.&lt;id&gt; when loaded, so every player reads them in
     * their own language. The templates themselves don't change.
     */
    public static final Map<String, String> PLEKKEN = Map.ofEntries(
            Map.entry("Knuffeldal", "knuffeldal"), Map.entry("Guhkermis", "guhkermis"), Map.entry("Guhland", "guhland"),
            Map.entry("Nomguh", "nomguh"), Map.entry("Guhwarden", "guhwarden"), Map.entry("Ballonfestival", "ballonfestival"),
            Map.entry("Guhcircuit", "guhcircuit"), Map.entry("Ohana op Guhwai'i", "ohana"), Map.entry("De capsule van 626", "capsule"),
            Map.entry("Guhkasteel", "guhkasteel"), Map.entry("Kloon-eiland", "kloon_eiland"));
    private static final java.util.regex.Pattern MET_XZ = java.util.regex.Pattern.compile("^(.*) \\((-?\\d+), (-?\\d+)\\)$");
    private static final String VAN = "Reisguh van ";

    /**
     * 1.2.0: a saved Reisguh name (before 1.2.0 always a String, resolved in Dutch) to what it means: a template place name,
     * the automatic names ("Guhportaal (x, z)", "Reisguh (x, z)", "Reisguh van Speler", "Guhkermis (x, z)") as their
     * translatable form; anything else (a player's own name) stays the literal it is.
     */
    public static Component vanOud(Component name) {
        if (!nl.juiced.guhs.taal.Tekst.literal(name)) {
            return name;
        }
        String s = name.getString();
        String plek = PLEKKEN.get(s);
        if (plek != null) {
            return Component.translatable("entity.guhs.reisguh.plek." + plek);
        }
        var m = MET_XZ.matcher(s);
        if (m.matches()) {
            int x = Integer.parseInt(m.group(2)), z = Integer.parseInt(m.group(3));
            String base = m.group(1);
            if (base.equals("Guhportaal")) {
                return Component.translatable("quest.guhs.reis.portal_name", x, z);
            }
            if (base.equals("Reisguh")) {
                return Component.translatable("quest.guhs.reis.wild_name", x, z);
            }
            if (PLEKKEN.containsKey(base)) {
                return Component.translatable("quest.guhs.reis.naam_xz", vanOud(Component.literal(base)), x, z);
            }
        }
        if (s.startsWith(VAN) && s.length() > VAN.length()) {
            return Component.translatable("quest.guhs.reis.own_name", s.substring(VAN.length()));
        }
        return name;
    }

    /** All waypoints of the world, and which ones each player has discovered. */
    public static class Data extends SavedData {
        final Map<UUID, Point> points = new LinkedHashMap<>();
        final Map<UUID, Set<UUID>> discovered = new java.util.HashMap<>();

        public static Data get(MinecraftServer server) {
            return nl.juiced.guhs.storage.GuhSavedData.get(server.overworld(), Data.TYPE, "guhs_reisguhs");
        }

        static final net.minecraft.world.level.saveddata.SavedDataType<Data> TYPE = nl.juiced.guhs.storage.GuhSavedData.tagType("reisguhs",
                Data::new, t -> Data.load(t, null), d -> d.save(new CompoundTag(), null));   // (the registries were never used)

        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Point p : points.values()) {
                CompoundTag c = new CompoundTag();
                c.store("Id", UUIDUtil.CODEC, p.id());
                nl.juiced.guhs.taal.Tekst.put(c, "Name", p.name());
                c.putLong("Pos", p.pos().asLong());
                c.putFloat("Yaw", p.yaw());
                list.add(c);
            }
            tag.put("Points", list);
            CompoundTag players = new CompoundTag();
            discovered.forEach((player, set) -> {
                ListTag ids = new ListTag();
                set.forEach(id -> ids.add(StringTag.valueOf(id.toString())));
                players.put(player.toString(), ids);
            });
            tag.put("Discovered", players);
            return tag;
        }

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            for (Tag t : tag.getListOrEmpty("Points")) {
                CompoundTag c = (CompoundTag) t;
                data.points.put(c.read("Id", UUIDUtil.CODEC).orElseThrow(), new Point(c.read("Id", UUIDUtil.CODEC).orElseThrow(), vanOud(nl.juiced.guhs.taal.Tekst.get(c, "Name")), BlockPos.of(c.getLongOr("Pos", 0L)), c.getFloatOr("Yaw", 0.0F)));
            }
            CompoundTag players = tag.getCompoundOrEmpty("Discovered");
            for (String key : players.keySet()) {
                Set<UUID> set = new HashSet<>();
                for (Tag t : players.getListOrEmpty(key)) {
                    set.add(UUID.fromString(t.asString().orElse("")));
                }
                data.discovered.put(UUID.fromString(key), set);
            }
            return data;
        }

        Set<UUID> of(UUID player) {
            return discovered.computeIfAbsent(player, k -> new HashSet<>());
        }
    }

    /** Every second: a Reisguh keeps its entry in the list up to date (position, name). */
    public static void tick(GuhNpcEntity npc) {
        nl.juiced.guhs.feature.reisguh.ReisguhFluit.tick(npc);          // (2.8: now and then: tuut tuut!)
        if (npc.tickCount % 20 != 1 || !(npc.level() instanceof ServerLevel level)) {
            return;
        }
        Data data = Data.get(level.getServer());
        // two Reisguhs on the very same spot (an old bug at new portals): one of them goes
        for (GuhNpcEntity other : level.getEntitiesOfClass(GuhNpcEntity.class, npc.getBoundingBox().inflate(0.5),
                n -> n != npc && n.getKind() == GuhNpcEntity.Kind.REISGUH && !n.isRemoved())) {
            if (npc.getUUID().compareTo(other.getUUID()) > 0) {
                data.points.remove(npc.getUUID());
                data.setDirty();
                npc.discard();
                return;
            }
        }
        register(npc, level, data);
    }

    /** Puts (or updates) a Guhmension Reisguh on the list. */
    private static void register(GuhNpcEntity npc, ServerLevel level, Data data) {
        if (level.dimension() != ModDimensions.GUHMENSION) {
            return;
        }
        Point old = data.points.get(npc.getUUID());
        if (old == null) {
            uniekeNaam(npc, data);
        }
        Point now = new Point(npc.getUUID(), name(npc), npc.blockPosition(), npc.getYRot());
        if (!now.equals(old)) {
            data.points.put(npc.getUUID(), now);
            data.setDirty();
        }
    }

    /**
     * 2.10.1: a Reisguh that comes with a building (her name is in the template: "Guhkermis", "Knuffeldal"...) gets her
     * coordinates behind her name when that name is already taken, so two kermissen are easy to tell apart in the menu.
     */
    static void uniekeNaam(GuhNpcEntity npc, Data data) {
        Component naam = npc.getReisName();
        if (nl.juiced.guhs.taal.Tekst.empty(naam) || data.points.values().stream().noneMatch(p -> !p.id().equals(npc.getUUID()) && p.name().equals(naam))) {
            return;
        }
        npc.setReisName(Component.translatable("quest.guhs.reis.naam_xz", naam, npc.blockPosition().getX(), npc.blockPosition().getZ()));
    }

    /** Her name as every player reads it (1.2.0: a Component, resolved on the client). */
    public static Component name(GuhNpcEntity npc) {
        return nl.juiced.guhs.taal.Tekst.empty(npc.getReisName()) ? Component.translatable("entity.guhs.guh_npc.reisguh") : npc.getReisName();
    }

    /** Right-click: discover it, or open its menu. */
    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        ServerLevel level = player.level();
        if (level.dimension() != ModDimensions.GUHMENSION) {
            GuhQuests.say(player, npc, "quest.guhs.reis.only_guhmension");
            return;
        }
        tick(npc);
        Data data = Data.get(level.getServer());
        data.points.putIfAbsent(npc.getUUID(), new Point(npc.getUUID(), name(npc), npc.blockPosition(), npc.getYRot()));
        Set<UUID> mine = data.of(player.getUUID());
        if (mine.add(npc.getUUID())) {
            data.setDirty();
            GuhQuests.say(player, npc, "quest.guhs.reis.discovered", name(npc));
            level.playSound(null, npc.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.6f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 1.5, npc.getZ(), 12, 0.5, 0.5, 0.5, 0);
            if (mine.size() >= 3) {
                GuhAdvancements.grant(player, "reisguhs");
            }
        }
        open(npc, player);
    }

    private static void open(GuhNpcEntity npc, ServerPlayer player) {
        Data data = Data.get(player.level().getServer());
        List<Point> list = new ArrayList<>();
        for (UUID id : data.of(player.getUUID())) {
            Point p = data.points.get(id);
            if (p != null && !id.equals(npc.getUUID())) {
                list.add(p);
            }
        }
        list.sort(Comparator.comparingDouble(p -> p.pos().distSqr(npc.blockPosition())));
        CompoundTag tag = new CompoundTag();
        nl.juiced.guhs.taal.Tekst.put(tag, "Name", name(npc));
        ListTag points = new ListTag();
        for (Point p : list) {
            CompoundTag c = new CompoundTag();
            c.store("Id", UUIDUtil.CODEC, p.id());
            nl.juiced.guhs.taal.Tekst.put(c, "Name", p.name());
            c.putInt("Distance", (int) Math.sqrt(p.pos().distSqr(npc.blockPosition())));
            points.add(c);
        }
        tag.put("Points", points);
        ModNetworking.sendTo(player, new MaagPayloads.ReisOpen(npc.getId(), tag));
    }

    /** A button in the menu: rename this Reisguh, or travel to another one. */
    public static void action(GuhNpcEntity npc, ServerPlayer player, int action, String text) {
        if (npc.getKind() != GuhNpcEntity.Kind.REISGUH || player.distanceToSqr(npc) > 64 || player.level().dimension() != ModDimensions.GUHMENSION) {
            return;
        }
        Data data = Data.get(player.level().getServer());
        if (!data.of(player.getUUID()).contains(npc.getUUID())) {
            return;
        }
        if (action == RENAME) {
            String name = text.strip();
            if (name.isEmpty() || name.length() > MAX_NAME) {
                return;
            }
            npc.setReisName(Component.literal(name));
            tick(npc);
            data.points.put(npc.getUUID(), new Point(npc.getUUID(), Component.literal(name), npc.blockPosition(), npc.getYRot()));
            data.setDirty();
            player.sendOverlayMessage(Component.translatable("quest.guhs.reis.renamed", name).withStyle(ChatFormatting.LIGHT_PURPLE));
            open(npc, player);
        } else if (action == TRAVEL) {
            UUID target;
            try {
                target = UUID.fromString(text);
            } catch (IllegalArgumentException e) {
                return;
            }
            Point p = data.points.get(target);
            if (p == null || !data.of(player.getUUID()).contains(target)) {
                return;
            }
            travel(player, p);
            nl.juiced.guhs.feature.reisguh.ReisguhFluit.reis(npc, player.level(), target);   // (2.8: both conductors whistle)
        }
    }

    private static void travel(ServerPlayer player, Point p) {
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        BlockPos spot = arrival(level, p);
        player.teleportTo(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, java.util.Set.of(), p.yaw() + 180, 0, true);
        level.playSound(null, spot, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.3f);
        level.sendParticles(ParticleTypes.PORTAL, spot.getX() + 0.5, spot.getY() + 1, spot.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.3);
        player.sendOverlayMessage(Component.translatable("quest.guhs.reis.arrived", p.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // --- moving Reisguhs: pick one up like a tamed guh, put it down somewhere else -------------------------------------

    public static void pickUp(GuhNpcEntity npc, ServerPlayer player) {
        Data data = Data.get(player.level().getServer());
        data.points.remove(npc.getUUID());                          // (back in the list where it's put down again)
        data.setDirty();
        CompoundTag tag = nl.juiced.guhs.storage.Nbt.saveWithoutId(npc);
        tag.putString("id", "guhs:guh_npc");
        nl.juiced.guhs.taal.Tekst.put(tag, "GuhDisplayName", name(npc));
        npc.discard();
        player.getInventory().placeItemBackInInventory(nl.juiced.guhs.item.PickedUpGuhItem.of(tag));
        player.sendOverlayMessage(Component.translatable("quest.guhs.reis.picked_up", name(npc)).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.level().playSound(null, npc.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.8f, 1.2f);
    }

    /** Now and then a Reisguh just turns up in the Guhmension (never close to another one). */
    public static void maybeWander(ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (random.nextInt(WILD_EEN_OP) != 0 || level.dimension() != ModDimensions.GUHMENSION) {
            return;
        }
        Data data = Data.get(level.getServer());
        if (data.points.values().stream().anyMatch(p -> p.pos().closerThan(pos, WILD_AFSTAND))) {
            return;
        }
        place(level, pos, random.nextFloat() * 360f, Component.translatable("quest.guhs.reis.wild_name", pos.getX(), pos.getZ()));
    }

    /** Where you arrive when you travel to a Reisguh: in front of it (or on top when that's blocked). */
    public static BlockPos arrivalSpot(ServerLevel level, UUID id) {
        Point p = Data.get(level.getServer()).points.get(id);
        return p == null ? null : arrival(level, p);
    }

    private static BlockPos arrival(ServerLevel level, Point p) {
        level.getChunk(p.pos());
        Vec3 look = Vec3.directionFromRotation(0, p.yaw());
        BlockPos spot = BlockPos.containing(p.pos().getX() + 0.5 + look.x * 2, p.pos().getY(), p.pos().getZ() + 0.5 + look.z * 2);
        return level.isEmptyBlock(spot) && level.isEmptyBlock(spot.above()) ? spot : p.pos().above(2);
    }

    // --- putting Reisguhs down -----------------------------------------------------------------------------------------

    /** A new Reisguh at a spot (made sure it has a floor and room), looking `yaw`. */
    @Nullable
    public static GuhNpcEntity place(ServerLevel level, BlockPos pos, float yaw, Component name) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (npc == null) {
            return null;
        }
        npc.setKind(GuhNpcEntity.Kind.REISGUH);
        npc.setReisName(name);
        npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        level.addFreshEntity(npc);
        tick(npc);
        // 1.1.0: on the list right away. In 1.21.1 a second nearPortal() saw the new Reisguh as an entity; in 26.1 the
        // entities of a chunk that is still loading are not visible yet, so without this a second portal check (building a
        // portal and arriving both ask) put down a second Reisguh.
        register(npc, level, Data.get(level.getServer()));
        return npc;
    }

    /** Next to a guh portal in the Guhmension there is always a Reisguh (within 5 blocks): put one there if there isn't. */
    public static void nearPortal(ServerLevel level, BlockPos portal) {
        if (level.dimension() != ModDimensions.GUHMENSION) {
            return;
        }
        level.getChunk(portal);
        // (the list, not the entities: a Reisguh just put down in a chunk that is still loading isn't visible as an entity yet)
        if (Data.get(level.getServer()).points.values().stream().anyMatch(p -> p.pos().closerThan(portal, 8))
                || !level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(portal).inflate(8), n -> n.getKind() == GuhNpcEntity.Kind.REISGUH).isEmpty()) {
            return;
        }
        for (int[] d : new int[][]{{3, 2}, {-3, 2}, {3, -2}, {-3, -2}, {0, 3}, {0, -3}, {4, 0}, {-4, 0}}) {
            BlockPos spot = portal.offset(d[0], 0, d[1]);
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos at = spot.above(dy);
                if (level.isEmptyBlock(at) && level.isEmptyBlock(at.above()) && level.getBlockState(at.below()).isSolid()) {
                    float yaw = (float) Math.toDegrees(Math.atan2(-(portal.getX() - at.getX()), portal.getZ() - at.getZ()));
                    place(level, at, yaw, Component.translatable("quest.guhs.reis.portal_name", at.getX(), at.getZ()));
                    return;
                }
            }
        }
        // nowhere nice: make a spot on the platform side
        BlockPos at = portal.offset(3, 0, 2);
        level.setBlockAndUpdate(at.below(), net.minecraft.world.level.block.Blocks.PINK_WOOL.defaultBlockState());
        level.setBlockAndUpdate(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(at.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        place(level, at, 0, Component.translatable("quest.guhs.reis.portal_name", at.getX(), at.getZ()));
    }

    /** For tests: the Reisguhs on the list within {@code range} of {@code pos} (1.1.0: entities in a chunk that was only
     *  loaded by the test are not visible to entity queries in 26.1, the list is). */
    public static java.util.List<Point> pointsNear(net.minecraft.server.MinecraftServer server, BlockPos pos, double range) {
        return Data.get(server).points.values().stream().filter(p -> p.pos().closerThan(pos, range)).toList();
    }

    /** For tests: has this player discovered that Reisguh? */
    public static boolean discovered(ServerPlayer player, UUID reisguh) {
        return Data.get(player.level().getServer()).of(player.getUUID()).contains(reisguh);
    }

    private Reisguh() {
    }
}
