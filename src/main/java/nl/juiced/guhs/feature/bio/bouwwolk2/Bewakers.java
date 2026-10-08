package nl.juiced.guhs.feature.bio.bouwwolk2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.GuhTime;
import nl.juiced.guhs.world.ModDimensions;
import nl.juiced.guhs.world.Terugkeer;

/**
 * Looks after the three places while a player is there (every {@link #STAP} ticks per player in the Guhmensie, and only
 * for the structure the player stands in; nothing runs for an empty sky):
 * <ul>
 *   <li>the "found" proofs for the quests;</li>
 *   <li>the giant: exactly one on the bed of a wolkenkasteeltje. A second one is removed, a missing one is put back
 *       ({@link Terugkeer}: only when the place is loaded and was seen empty twice);</li>
 *   <li>the smid-guh of a bliksemsmidse, the same way;</li>
 *   <li>the rainbow of a regenboogbrug: it is a building material, so players may take it; {@link #HERGROEI} ticks after
 *       it was last whole the missing pieces grow back in the air where they were (nothing a player built is touched).</li>
 * </ul>
 */
public final class Bewakers {
    public static final int STAP = 100;
    /** The rainbow grows back this long after it was last seen whole (three days). */
    public static final long HERGROEI = 3 * GuhTime.DAY;
    public static final String SOORT_REUS = "bio_bouw_wolk2_reus", SOORT_SMID = "bio_bouw_wolk2_smid", SOORT_BOOG = "bio_bouw_wolk2_boog";
    /** Where the template has the smid-guh, and his yaw there (he looks south, at the door). */
    public static final Vec3 SMID_IN_TEMPLATE = new Vec3(16.5, 25.0, 9.5);
    public static final float SMID_YAW = 0f;

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer speler) || (speler.tickCount + speler.getId()) % STAP != 0 || speler.isSpectator()
                || speler.level().dimension() != ModDimensions.GUHMENSION) {
            return;
        }
        ServerLevel level = speler.level();
        BlockPos hier = speler.blockPosition();
        if (!Bio.in(level, hier, Bio.WOLKENWEIDE)) {
            return;
        }
        for (PoolElementStructurePiece stuk : Terugkeer.stukken(level, BouwWolk2Slice.WOLKENKASTEELTJE, hier, null)) {
            GuhAdvancements.grant(speler, "wolkenkasteeltje_gevonden");
            reus(level, wereld(stuk, Kasteel.REUS_IN_TEMPLATE), stuk.getRotation());
        }
        for (PoolElementStructurePiece stuk : Terugkeer.stukken(level, BouwWolk2Slice.BLIKSEMSMIDSE, hier, null)) {
            GuhAdvancements.grant(speler, "bliksemsmidse_gevonden");
            smid(level, wereld(stuk, SMID_IN_TEMPLATE), stuk.getRotation(), doos(stuk));
        }
        for (PoolElementStructurePiece stuk : Terugkeer.stukken(level, BouwWolk2Slice.REGENBOOGBRUG, hier, null)) {
            GuhAdvancements.grant(speler, "regenboogbrug_gevonden");
            if (hergroei(level, stuk.getPosition(), stuk.getRotation(), HERGROEI) > 0) {
                speler.sendOverlayMessage(Component.translatable("gui.guhs.regenboogbrug.terug").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** Where a spot of the template (as an entity position) is in the world. */
    public static Vec3 wereld(PoolElementStructurePiece stuk, Vec3 inTemplate) {
        return wereld(stuk.getPosition(), stuk.getRotation(), inTemplate);
    }

    public static Vec3 wereld(BlockPos hoek, Rotation draai, Vec3 inTemplate) {
        return StructureTemplate.transform(inTemplate, Mirror.NONE, draai, BlockPos.ZERO).add(hoek.getX(), hoek.getY(), hoek.getZ());
    }

    private static AABB doos(PoolElementStructurePiece stuk) {
        BoundingBox b = stuk.getBoundingBox();
        return new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1);
    }

    // --- the giant ----------------------------------------------------------------------------------------------------------

    /**
     * One giant on this bed: extra ones go, and when there has been none (seen twice, the place loaded) a new one comes.
     * Returns the new giant, or null.
     */
    @Nullable
    public static ReuzenguhEntity reus(ServerLevel level, Vec3 bed, Rotation draai) {
        List<ReuzenguhEntity> er = level.getEntitiesOfClass(ReuzenguhEntity.class, AABB.ofSize(bed, 16, 12, 16), ReuzenguhEntity::isAlive);
        for (int i = 1; i < er.size(); i++) {
            er.get(i).discard();
        }
        if (!Terugkeer.moetTerug(level, SOORT_REUS, BlockPos.containing(bed), !er.isEmpty(), 0)) {
            return null;
        }
        return nieuweReus(level, bed, draai);
    }

    public static ReuzenguhEntity nieuweReus(ServerLevel level, Vec3 bed, Rotation draai) {
        ReuzenguhEntity reus = BouwWolk2Slice.REUZENGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        reus.zetThuis(bed, draai);
        level.addFreshEntity(reus);
        return reus;
    }

    // --- the smid -----------------------------------------------------------------------------------------------------------

    /** One smid-guh in this forge (anywhere in its template): extra ones go, a missing one comes back at the anvil. */
    @Nullable
    public static GuhNpcEntity smid(ServerLevel level, Vec3 plek, Rotation draai, AABB smidse) {
        List<GuhNpcEntity> er = level.getEntitiesOfClass(GuhNpcEntity.class, smidse, n -> n.isAlive() && n.getKind() == GuhNpcEntity.Kind.SMIDGUH);
        for (int i = 1; i < er.size(); i++) {
            er.get(i).discard();
        }
        if (!Terugkeer.moetTerug(level, SOORT_SMID, BlockPos.containing(plek), !er.isEmpty(), 0)) {
            return null;
        }
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.SMIDGUH);
        npc.snapTo(plek.x, plek.y, plek.z, SMID_YAW + 90f * draai.ordinal(), 0);
        npc.setPersistenceRequired();
        level.addFreshEntity(npc);
        return npc;
    }

    // --- the rainbow --------------------------------------------------------------------------------------------------------

    /** A piece of rainbow of the template, where it belongs in the world. */
    public record Stuk(BlockPos plek, BlockState blok) {
    }

    private static final Map<String, List<Stuk>> BOGEN = new ConcurrentHashMap<>();

    /** The rainbow pieces of the bridge whose template corner is here, turned like this (from the template; cached). */
    public static List<Stuk> boog(ServerLevel level, BlockPos hoek, Rotation draai) {
        return BOGEN.computeIfAbsent(level.dimension().identifier() + "|" + hoek.asLong() + "|" + draai, k -> {
            List<Stuk> uit = new ArrayList<>();
            StructureTemplate t = level.getStructureManager().get(Guhs.id("regenboogbrug")).orElse(null);
            if (t == null) {
                return uit;
            }
            StructurePlaceSettings zo = new StructurePlaceSettings().setRotation(draai);
            for (String id : List.of("regenboogblok", "regenboogblok_plaat", "regenboogblok_trap")) {
                Block b = Bio.blok(id, Blocks.AIR);
                if (b == Blocks.AIR) {
                    continue;
                }
                for (StructureTemplate.StructureBlockInfo info : t.filterBlocks(hoek, zo, b, true)) {
                    uit.add(new Stuk(info.pos(), info.state()));
                }
            }
            return List.copyOf(uit);
        });
    }

    /** The place a bridge is remembered by: its first piece (at its west foot). */
    public static BlockPos sleutel(List<Stuk> boog) {
        return boog.get(0).plek();
    }

    /** How many pieces of this rainbow are missing right now. */
    public static int mist(ServerLevel level, List<Stuk> boog) {
        int n = 0;
        for (Stuk s : boog) {
            if (!level.getBlockState(s.plek()).is(s.blok().getBlock())) {
                n++;
            }
        }
        return n;
    }

    /**
     * The check of one bridge: whole, it is stamped "seen"; broken for longer than {@code wacht} ticks since it was last
     * whole, its missing pieces come back where there is air. Returns how many pieces grew back.
     */
    public static int hergroei(ServerLevel level, BlockPos hoek, Rotation draai, long wacht) {
        List<Stuk> boog = boog(level, hoek, draai);
        if (boog.isEmpty() || !level.isLoaded(boog.get(0).plek()) || !level.isLoaded(boog.get(boog.size() - 1).plek())) {
            return 0;
        }
        boolean heel = mist(level, boog) == 0;
        if (!Terugkeer.moetTerug(level, SOORT_BOOG, sleutel(boog), heel, wacht)) {
            return 0;
        }
        int gezet = 0;
        for (Stuk s : boog) {
            if (level.getBlockState(s.plek()).isAir()) {
                level.setBlock(s.plek(), s.blok(), Block.UPDATE_CLIENTS);
                gezet++;
            }
        }
        return gezet;
    }

    private Bewakers() {
    }
}
