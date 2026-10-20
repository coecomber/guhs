package nl.juiced.guhs.feature.safari.verblijf;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Op Safari! step 2 (S5): the stork's deliveries (design rounds 38-39: "brengt een pakketje van jouw kist naar de kist of
 * brievenbus van een vriend"). Everybody may have one {@link OoievaarsbusBlock Ooievaarsbus} (the last one placed counts;
 * remembered here, SavedData {@code guhs:safari_ooievaarsbussen}). On the stork board's menu you pick a friend and press send:
 * the stork takes up to 9 stacks from the chest next to the board, flies off with its bundle ({@link ThuisDier#vliegWeg}) and
 * after {@link #REISTIJD} the parcel is in the friend's mailbox; what does not fit comes back to the chest (or drops at the
 * board).
 */
public final class Ooievaar {
    public static final int REISTIJD = 200;

    public enum Fout { GEEN_STORK, GEEN_KIST, GEEN_BUS, ONDERWEG }

    /** Sends the parcel; null when it is on its way, else why not. */
    @Nullable
    public static Fout stuur(ServerLevel level, VerblijfsbordBlockEntity be) {
        if (be.aankomst >= 0) {
            return Fout.ONDERWEG;
        }
        if (be.bestemming == null || Bussen.get(level.getServer()).bus(be.bestemming) == null) {
            return Fout.GEEN_BUS;
        }
        Container kist = be.kistErnaast(level);
        if (kist == null || kist.isEmpty()) {
            return Fout.GEEN_KIST;
        }
        ThuisDier stork = null;
        for (ThuisDier d : be.dieren(level)) {
            if (!d.isBaby() && !d.weg()) {
                stork = d;
                break;
            }
        }
        if (stork == null) {
            return Fout.GEEN_STORK;
        }
        be.pakket.clear();
        for (int i = 0; i < kist.getContainerSize() && be.pakket.size() < 9; i++) {
            ItemStack s = kist.getItem(i);
            if (!s.isEmpty()) {
                be.pakket.add(s.copy());
                kist.setItem(i, ItemStack.EMPTY);
            }
        }
        kist.setChanged();
        stork.vliegWeg(REISTIJD + 20);
        be.aankomst = level.getGameTime() + REISTIJD;
        be.setChanged();
        return null;
    }

    /** The trip is over: the parcel goes into the friend's mailbox (what does not fit comes back). */
    static void aangekomen(ServerLevel level, VerblijfsbordBlockEntity be) {
        be.aankomst = -1;
        MinecraftServer server = level.getServer();
        GlobalPos bus = be.bestemming == null ? null : Bussen.get(server).bus(be.bestemming);
        ServerLevel doel = bus == null ? null : server.getLevel(bus.dimension());
        String vriend = be.bestemming == null ? "?" : Bussen.get(server).naam(be.bestemming);
        if (doel != null) {
            doel.getChunk(bus.pos());
            if (doel.getBlockEntity(bus.pos()) instanceof OoievaarsbusBlockEntity b) {
                for (int i = 0; i < be.pakket.size(); i++) {
                    ItemStack rest = b.inhoud().addItem(be.pakket.get(i));
                    be.pakket.set(i, rest);
                }
                b.setChanged();
                ServerPlayer ontvanger = server.getPlayerList().getPlayer(be.bestemming);
                if (ontvanger != null) {
                    ontvanger.sendSystemMessage(Component.translatable("gui.guhs.verblijf.bezorgen.ontvangen", be.eigenaarNaam()));
                }
            }
        }
        boolean terug = false;
        Container kist = be.kistErnaast(level);
        for (ItemStack s : be.pakket) {
            if (s.isEmpty()) {
                continue;
            }
            terug = true;
            ItemStack rest = s;
            if (kist instanceof net.minecraft.world.Container c) {
                for (int i = 0; i < c.getContainerSize() && !rest.isEmpty(); i++) {
                    if (c.getItem(i).isEmpty()) {
                        c.setItem(i, rest);
                        rest = ItemStack.EMPTY;
                    }
                }
            }
            if (!rest.isEmpty()) {
                BlockPos p = be.getBlockPos();
                Containers.dropItemStack(level, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, rest);
            }
        }
        be.pakket.clear();
        be.setChanged();
        ServerPlayer afzender = be.eigenaar() == null ? null : server.getPlayerList().getPlayer(be.eigenaar());
        if (afzender != null) {
            afzender.sendSystemMessage(Component.translatable(terug ? "gui.guhs.verblijf.bezorgen.vol" : "gui.guhs.verblijf.bezorgen.klaar", vriend));
        }
    }

    /** Who has a mailbox: owner -> where (and the name, for the menu). */
    public static final class Bussen extends SavedData {
        public static final SavedDataType<Bussen> TYPE = GuhSavedData.tagType("safari_ooievaarsbussen", Bussen::new, Bussen::load, Bussen::save);
        private final Map<UUID, GlobalPos> bussen = new HashMap<>();
        private final Map<UUID, String> namen = new HashMap<>();

        public static Bussen get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(TYPE);
        }

        @Nullable
        public GlobalPos bus(UUID eigenaar) {
            return bussen.get(eigenaar);
        }

        public String naam(UUID eigenaar) {
            return namen.getOrDefault(eigenaar, "?");
        }

        /** Every mailbox owner except {@code behalve}, by name. */
        public Map<UUID, String> anderen(@Nullable UUID behalve) {
            Map<UUID, String> uit = new LinkedHashMap<>();
            namen.entrySet().stream().filter(e -> !e.getKey().equals(behalve) && bussen.containsKey(e.getKey()))
                    .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER)).forEach(e -> uit.put(e.getKey(), e.getValue()));
            return uit;
        }

        public void zet(UUID eigenaar, String naam, ResourceKey<Level> dim, BlockPos pos) {
            bussen.put(eigenaar, GlobalPos.of(dim, pos.immutable()));
            namen.put(eigenaar, naam);
            setDirty();
        }

        public void weg(UUID eigenaar, ResourceKey<Level> dim, BlockPos pos) {
            GlobalPos g = bussen.get(eigenaar);
            if (g != null && g.dimension().equals(dim) && g.pos().equals(pos)) {
                bussen.remove(eigenaar);
                setDirty();
            }
        }

        private CompoundTag save() {
            CompoundTag t = new CompoundTag();
            bussen.forEach((id, g) -> {
                CompoundTag b = new CompoundTag();
                b.putString("Dim", g.dimension().identifier().toString());
                b.putLong("Pos", g.pos().asLong());
                b.putString("Naam", namen.getOrDefault(id, ""));
                t.put(id.toString(), b);
            });
            return t;
        }

        private static Bussen load(CompoundTag t) {
            Bussen b = new Bussen();
            for (String k : t.keySet()) {
                try {
                    UUID id = UUID.fromString(k);
                    CompoundTag c = t.getCompoundOrEmpty(k);
                    ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(c.getStringOr("Dim", "minecraft:overworld")));
                    b.bussen.put(id, GlobalPos.of(dim, BlockPos.of(c.getLongOr("Pos", 0L))));
                    b.namen.put(id, c.getStringOr("Naam", "?"));
                } catch (IllegalArgumentException ignored) {
                    // (not a UUID)
                }
            }
            return b;
        }
    }

    private Ooievaar() {
    }
}
