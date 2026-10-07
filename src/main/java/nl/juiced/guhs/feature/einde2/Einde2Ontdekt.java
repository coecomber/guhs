package nl.juiced.guhs.feature.einde2;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2: which big places of the outer islands a player has been to. Every {@link #INTERVAL} ticks, for players in the
 * Guheinde's high band: the guhs:leegte_bouw structure whose build the player is in is remembered in the player's own data
 * ({@link #KEY}, a list of structure ids) and the hidden advancement {@code guhs:quest/einde2_gevonden_<id>} is granted
 * (tools/features/einde2.py writes them; the FTB "Vind ..." quests ask for it). Per player, for any number of players.
 * <p>
 * hook fase B: the ferry of the Leegtehaven offers the places of {@link #ontdekt}.
 */
public final class Einde2Ontdekt {
    public static final int INTERVAL = 40;
    /** Player data (GuhQuests.saved): the ids of the places this player found. */
    public static final String KEY = "guhs_einde2_ontdekt";

    private Einde2Ontdekt() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(Einde2Ontdekt::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % INTERVAL != 0 || player.isSpectator()
                || player.level().dimension() != GuheindeFeature.GUHEINDE || player.getY() < Einde2Ids.HOOG_MIN - 2) {
            return;
        }
        String id = bouwBij(player.level(), player.blockPosition());
        if (id != null) {
            ontdek(player, id);
        }
    }

    /** The big place (one of the twelve) whose build this spot lies in: its box, not its empty rim (null: none). */
    @Nullable
    public static String bouwBij(ServerLevel level, BlockPos pos) {
        // (cheap first: the boxes of the guaranteed copies follow from the seed)
        for (String id : Einde2Ids.VERHAAL_STRUCTUREN) {
            BoundingBox doos = Plekken.doos(level, id);
            if (doos != null && doos.isInside(pos)) {
                return id;
            }
        }
        for (String id : Einde2Ids.ZIJ_STRUCTUREN) {
            BoundingBox doos = Plekken.doos(level, id);
            if (doos != null && doos.isInside(pos)) {
                return id;
            }
        }
        // (the random copies of the side places: the chunk's structure references)
        if (!level.isLoaded(pos)) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, s -> s.is(Einde2Ids.ZIJ));
        if (!start.isValid()) {
            return null;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof LeegteBouwStructure.Stuk && piece.getBoundingBox().isInside(pos)) {
                Identifier key = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
                return key == null ? null : key.getPath();
            }
        }
        return null;
    }

    /** Remembers the place for this player and grants its advancement (true: it is new for them). */
    public static boolean ontdek(ServerPlayer player, String structuur) {
        GuhAdvancements.grant(player, "einde2_gevonden_" + structuur);
        CompoundTag saved = GuhQuests.saved(player);
        ListTag list = saved.getListOrEmpty(KEY);
        for (int i = 0; i < list.size(); i++) {
            if (structuur.equals(list.getStringOr(i, ""))) {
                return false;
            }
        }
        list.add(StringTag.valueOf(structuur));
        saved.put(KEY, list);
        return true;
    }

    /** The places this player found, in the order they found them. */
    public static Set<String> ontdekt(ServerPlayer player) {
        ListTag list = GuhQuests.saved(player).getListOrEmpty(KEY);
        Set<String> out = new LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getStringOr(i, ""));
        }
        return out;
    }

    public static boolean heeft(ServerPlayer player, String structuur) {
        return ontdekt(player).contains(structuur);
    }
}
