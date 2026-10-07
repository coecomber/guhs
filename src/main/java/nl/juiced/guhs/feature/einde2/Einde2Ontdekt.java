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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2: which big places of the outer islands a player has been to. Every {@link #INTERVAL} ticks, for players in the
 * Guheinde (the high band and, for the two deep places, below it): the guhs:leegte_bouw structure whose build the player is
 * in (a copy that really stands there, guaranteed or random: {@link #bouwBij}) is remembered in the player's own data
 * ({@link #KEY}, a list of structure ids) and the hidden advancement
 * {@code guhs:quest/einde2_gevonden_<id>} is granted (tools/features/einde2.py writes them; the FTB "Vind ..." quests ask
 * for it). Per player, for any number of players. A place of the Superkompas is marked visited there as well
 * (SpelGroepen), so the compass and this list never disagree.
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
                || player.level().dimension() != GuheindeFeature.GUHEINDE) {
            return;
        }
        String id = bouwBij(player.level(), player.blockPosition());
        if (id != null) {
            ontdek(player, id);
        }
    }

    /**
     * The big place (one of the fourteen) whose build this spot lies in: its box, not its empty rim (null: none). Only a
     * copy that really stands there counts: a structure start the chunk of this spot refers to. Where the one guaranteed
     * copy belongs follows from the seed, but in terrain that was there before einde2 it was never made: an empty box in
     * the air is no place to find.
     */
    @Nullable
    public static String bouwBij(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        // (cheap first, below the high band: only the two deep places are there, and each has just its one copy)
        if (pos.getY() < Einde2Ids.HOOG_MIN - 2) {
            boolean diep = false;
            for (String id : Einde2Ids.SPONS_STRUCTUREN) {
                BoundingBox doos = Plekken.doos(level, id);
                diep |= doos != null && doos.isInside(pos);
            }
            if (!diep) {
                return null;
            }
        }
        // (the chunk's structure references: every start of a leegte_bouw whose pieces reach into this chunk)
        for (StructureStart start : level.structureManager().startsForStructure(ChunkPos.containing(pos), s -> s instanceof LeegteBouwStructure)) {
            if (start.isValid() && start.getStructure() instanceof LeegteBouwStructure bouw && bouw.doos(start.getChunkPos()).isInside(pos)) {
                Identifier key = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(bouw);
                return key == null ? null : key.getPath();
            }
        }
        return null;
    }

    /**
     * Remembers the place for this player and grants its advancement (true: it is new for them). A place a story still
     * hides from this player ({@link Einde2Haken#VERBORGEN}) is not found by flying into it: false, and nothing is kept.
     */
    public static boolean ontdek(ServerPlayer player, String structuur) {
        if (heeft(player, structuur) || Einde2Haken.VERBORGEN.test(player, structuur)) {   // hook fase B: the veil of a story structure
            return false;
        }
        GuhAdvancements.grant(player, "einde2_gevonden_" + structuur);
        if (inKompas(structuur)) {
            SpelGroepen.bezoekStructuur(player, structuur);   // (the compass's own "visited" list; it remembers a place once)
        }
        CompoundTag saved = GuhQuests.saved(player);
        ListTag list = saved.getListOrEmpty(KEY);
        list.add(StringTag.valueOf(structuur));
        saved.put(KEY, list);
        return true;
    }

    /** Is this place listed in a tab of the Superkompas? */
    public static boolean inKompas(String structuur) {
        return SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.structures().contains(structuur));
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
