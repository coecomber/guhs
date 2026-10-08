package nl.juiced.guhs.feature.bio.bouwwolk2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.BioZelftest;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * DEV: what of this slice can only be checked in the real Guhmensie.
 * <ul>
 *   <li>The self test {@code bouw_wolk2} ({@code /guhs bio zelftest}): the data is loaded and fits (structures, sets,
 *       templates, trades, proofs, the Superkompas).</li>
 *   <li>{@code /guhs bio bouw-wolk2 inspecteer zoek}: finds two places of each of the three structures near a Wolkenweide,
 *       makes their chunks and keeps them loaded; {@code ... inspecteer kijk} (a few seconds later, so the entities are
 *       there) looks at every one: is the whole template there, does anything of the natural sky stand in its box, do the
 *       lifts stand on the ground, is the giant or the smid there exactly once. Lines "[bio-zelftest] bouw_wolk2_inspectie:
 *       OK|FOUT ...".</li>
 * </ul>
 */
public final class Inspectie {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** One found building. */
    private record Plek(String naam, BlockPos hoek, Rotation draai, BoundingBox doos) {
    }

    private static final List<Plek> GEVONDEN = new ArrayList<>();
    /** The y of each template's deck (tools/features/bio_bouw_wolk2_bouw.py STRUCTUREN "hoogte"). */
    private static final Map<String, Integer> DEK = Map.of("regenboogbrug", 36, "wolkenkasteeltje", 102, "bliksemsmidse", 24);

    static void registreer() {
        BioZelftest.registreer("bouw_wolk2", Inspectie::zelftest);
    }

    private static void zelftest(MinecraftServer server, ServerLevel level, BioZelftest.Melder meld) {
        var structuren = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var sets = server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (String naam : BouwWolk2Slice.STRUCTUREN) {
            Structure s = structuren.getValue(Guhs.id(naam));
            Identifier type = s == null ? null : BuiltInRegistries.STRUCTURE_TYPE.getKey(s.type());
            meld.check(s != null && Guhs.id("bio_plek").equals(type), "structure " + naam + " is a guhs:bio_plek (" + type + ")");
            StructureSet set = sets.getValue(Guhs.id(naam));
            int spacing = set != null && set.placement() instanceof RandomSpreadStructurePlacement p ? p.spacing() : -1;
            meld.check(spacing >= 8, "structure set " + naam + ": random_spread, spacing " + spacing + " (a lucht set keeps 8 or more)");
            meld.check(level.getStructureManager().get(Guhs.id(naam)).isPresent(), "template " + naam + " is loaded");
            boolean inKompas = SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.id().equals("wonderen") && c.structures().contains(naam));
            meld.check(inKompas, naam + " is in the Superkompas tab wonderen");
        }
        int ruil = 0;
        for (Smidguh.Ruil r : Smidguh.AANBOD) {
            if (Bio.item(r.id(), net.minecraft.world.item.Items.AIR) != net.minecraft.world.item.Items.AIR) {
                ruil++;
            }
        }
        meld.check(ruil == Smidguh.AANBOD.size() && Smidguh.pluis() != net.minecraft.world.item.Items.WHITE_WOOL,
                "the smid-guh's " + Smidguh.AANBOD.size() + " trades are real items, paid in wolkenpluis (" + ruil + ")");
        int bewijs = 0;
        for (String a : List.of("regenboogbrug_gevonden", "wolkenkasteeltje_gevonden", "bliksemsmidse_gevonden", "regenboogbrug_pot",
                "wolkenkasteeltje_langs_reus", "reuzenguh_weggeblazen", "smidguh_eerste_ruil")) {
            if (server.getAdvancements().get(Guhs.id("quest/" + a)) != null) {
                bewijs++;
            }
        }
        meld.check(bewijs == 7, "the seven proof advancements are loaded (" + bewijs + ")");
    }

    private static void log(List<String> uit, boolean goed, String tekst) {
        String regel = "[bio-zelftest] bouw_wolk2_inspectie: " + (goed ? "OK " : "FOUT ") + tekst;
        uit.add(regel);
        LOGGER.info(regel);
    }

    /** Finds up to {@code aantal} places of each structure, makes and keeps their chunks. */
    public static List<String> zoek(MinecraftServer server, ServerLevel level, int aantal) {
        List<String> uit = new ArrayList<>();
        GEVONDEN.clear();
        BlockPos weide = BioZelftest.vind(level, Bio.WOLKENWEIDE, BlockPos.ZERO, 6400);
        if (weide == null) {
            log(uit, false, "no Wolkenweide within 6400 blocks of 0 0");
            return uit;
        }
        var structuren = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String naam : BouwWolk2Slice.STRUCTUREN) {
            Holder<Structure> holder = structuren.get(ResourceKey.create(Registries.STRUCTURE, Guhs.id(naam))).orElse(null);
            if (holder == null) {
                log(uit, false, naam + ": unknown structure");
                continue;
            }
            List<BlockPos> starts = new ArrayList<>();
            // from the meadow, then from further and further away in eight directions, until there are enough different ones
            search:
            for (int ring = 0; ring <= 6; ring++) {
                for (int k = 0; k < (ring == 0 ? 1 : 8); k++) {
                    double a = k * Math.PI / 4;
                    BlockPos van = weide.offset((int) (Math.cos(a) * ring * 700), 0, (int) (Math.sin(a) * ring * 700));
                    var paar = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), van, 100, false);
                    if (paar == null) {
                        continue;
                    }
                    BlockPos p = paar.getFirst();
                    if (starts.stream().noneMatch(q -> (q.getX() >> 4) == (p.getX() >> 4) && (q.getZ() >> 4) == (p.getZ() >> 4))) {
                        starts.add(p);
                        if (starts.size() >= aantal) {
                            break search;
                        }
                    }
                }
            }
            // how often: the set's possible start chunks within 64 chunks of the meadow, and how many of them have their middle in the Wolkenweide
            StructureSet set = server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(naam));
            if (set != null && set.placement() instanceof RandomSpreadStructurePlacement spread) {
                int mogelijk = 0, inWeide = 0;
                int s0 = spread.spacing();
                long seed = level.getSeed();
                for (int gx = Math.floorDiv((weide.getX() >> 4) - 64, s0); gx <= Math.floorDiv((weide.getX() >> 4) + 64, s0); gx++) {
                    for (int gz = Math.floorDiv((weide.getZ() >> 4) - 64, s0); gz <= Math.floorDiv((weide.getZ() >> 4) + 64, s0); gz++) {
                        var c = spread.getPotentialStructureChunk(seed, gx * s0, gz * s0);
                        mogelijk++;
                        if (Bio.in(level, new BlockPos(c.getMiddleBlockX(), 100, c.getMiddleBlockZ()), Bio.WOLKENWEIDE)) {
                            inWeide++;
                        }
                    }
                }
                log(uit, true, naam + ": spacing " + s0 + ": " + mogelijk + " possible starts within 64 chunks of the Wolkenweide at " + weide.toShortString() + ", " + inWeide
                        + " of them with their middle in the Wolkenweide");
            }
            if (starts.size() < aantal) {
                log(uit, false, naam + ": only " + starts.size() + " of " + aantal + " found near the Wolkenweide at " + weide.toShortString());
            }
            for (BlockPos p : starts) {
                StructureStart st = level.getChunk(p.getX() >> 4, p.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(holder.value());
                if (st == null || !st.isValid() || st.getPieces().isEmpty() || !(st.getPieces().get(0) instanceof PoolElementStructurePiece stuk)) {
                    log(uit, false, naam + " at " + p.toShortString() + ": no start piece");
                    continue;
                }
                BoundingBox b = stuk.getBoundingBox();
                int n = 0;
                for (int cx = (b.minX() >> 4) - 1; cx <= (b.maxX() >> 4) + 1; cx++) {
                    for (int cz = (b.minZ() >> 4) - 1; cz <= (b.maxZ() >> 4) + 1; cz++) {
                        level.getChunk(cx, cz);
                        level.setChunkForced(cx, cz, true);
                        n++;
                    }
                }
                GEVONDEN.add(new Plek(naam, stuk.getPosition(), stuk.getRotation(), b));
                log(uit, true, naam + " found: box " + b.minX() + " " + b.minY() + " " + b.minZ() + " .. " + b.maxX() + " " + b.maxY() + " " + b.maxZ()
                        + ", turned " + stuk.getRotation() + ", " + n + " chunks made and kept; tp " + ((b.minX() + b.maxX()) / 2) + " " + (b.maxY() - 10) + " "
                        + ((b.minZ() + b.maxZ()) / 2));
            }
        }
        return uit;
    }

    /** Looks at every found place. */
    public static List<String> kijk(ServerLevel level) {
        List<String> uit = new ArrayList<>();
        if (GEVONDEN.isEmpty()) {
            log(uit, false, "nothing found yet: run inspecteer zoek first");
        }
        int[] tel = new int[2];
        for (Plek plek : GEVONDEN) {
            String wie = plek.naam + " at " + plek.doos.minX() + " " + plek.doos.minZ();
            Map<BlockPos, BlockState> template = Brug.blokken(level, plek.naam);
            java.util.Set<BlockPos> eigen = new java.util.HashSet<>();
            int goed = 0, totaal = 0, liftGrond = 0, liftKolommen = 0;
            int grondY = plek.doos.minY();
            StringBuilder mis = new StringBuilder();
            for (Map.Entry<BlockPos, BlockState> e : template.entrySet()) {
                BlockPos w = StructureTemplate.transform(e.getKey(), Mirror.NONE, plek.draai, BlockPos.ZERO).offset(plek.hoek);
                eigen.add(w);
                BlockState verwacht = e.getValue();
                if (verwacht.is(Blocks.JIGSAW)) {
                    continue;
                }
                totaal++;
                BlockState is = level.getBlockState(w);
                if (is.is(verwacht.getBlock())) {
                    goed++;
                } else if (mis.length() < 160) {
                    mis.append(' ').append(BuiltInRegistries.BLOCK.getKey(verwacht.getBlock()).getPath()).append("->")
                            .append(BuiltInRegistries.BLOCK.getKey(is.getBlock()).getPath()).append('@').append(w.toShortString().replace(", ", ","));
                }
                // a lift pad: is there ground (anything solid) under its foot?
                if (BuiltInRegistries.BLOCK.getKey(verwacht.getBlock()).getPath().equals("wolkenlift")) {
                    liftKolommen++;
                    for (int d = 2; d <= 4; d++) {
                        if (!level.getBlockState(w.below(d)).isAir()) {
                            liftGrond++;
                            break;
                        }
                    }
                }
            }
            boolean heel = totaal > 0 && goed >= totaal - Math.max(2, totaal / 500);
            tel[heel ? 0 : 1]++;
            log(uit, heel, wie + ": whole: " + goed + " of " + totaal + " template blocks in the world" + (mis.length() > 0 ? " (differs:" + mis + ")" : ""));
            // anything that is not ours inside the box? Land that rises under the building is no harm (counted, with its
            // highest block); anything from 14 below the deck up stands in the building's own air: an island, a cloud, a hill
            int dekY = plek.hoek.getY() + DEK.getOrDefault(plek.naam, 0);
            int vreemd = 0, raakt = 0, land = 0, landTop = Integer.MIN_VALUE;
            StringBuilder wat = new StringBuilder();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = plek.doos.minX(); x <= plek.doos.maxX(); x++) {
                for (int z = plek.doos.minZ(); z <= plek.doos.maxZ(); z++) {
                    for (int y = grondY + 10; y <= plek.doos.maxY(); y++) {
                        p.set(x, y, z);
                        if (eigen.contains(p)) {
                            continue;
                        }
                        BlockState s = level.getBlockState(p);
                        if (!s.isAir() && y < dekY - 14) {
                            land++;
                            landTop = Math.max(landTop, y);
                        } else if (!s.isAir()) {
                            vreemd++;
                            // does it touch the building (a block of ours next to it, corners included)?
                            boolean naast = false;
                            for (BlockPos q : BlockPos.betweenClosed(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1)) {
                                if (eigen.contains(q)) {
                                    naast = true;
                                    break;
                                }
                            }
                            raakt += naast ? 1 : 0;
                            if (wat.length() < 120) {
                                wat.append(' ').append(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath()).append('@').append(x).append(',').append(y).append(',').append(z);
                            }
                        }
                    }
                }
            }
            tel[raakt == 0 ? 0 : 1]++;
            log(uit, raakt == 0, wie + ": nothing of the natural sky touches the building: " + raakt + " foreign blocks against a block of ours; in the corners of its box, from 14 under its deck (y "
                    + dekY + ") up: " + vreemd + " foreign blocks" + wat
                    + "; land under it higher than 10 above the lifts' foot: " + land + " blocks" + (land > 0 ? ", up to y " + landTop : ""));
            boolean bereikbaar = liftKolommen == 18 && liftGrond >= 12;
            tel[bereikbaar ? 0 : 1]++;
            log(uit, bereikbaar, wie + ": reachable: " + liftKolommen + " lift pads (two columns of nine), " + liftGrond + " of them with ground under their foot");
            AABB doos = new AABB(plek.doos.minX(), plek.doos.minY(), plek.doos.minZ(), plek.doos.maxX() + 1, plek.doos.maxY() + 1, plek.doos.maxZ() + 1);
            if (plek.naam.equals("wolkenkasteeltje")) {
                List<ReuzenguhEntity> reuzen = level.getEntitiesOfClass(ReuzenguhEntity.class, doos, ReuzenguhEntity::isAlive);
                boolean ok = reuzen.size() == 1 && reuzen.get(0).position().distanceTo(Bewakers.wereld(plek.hoek, plek.draai, Kasteel.REUS_IN_TEMPLATE)) < 0.1
                        && reuzen.get(0).draai() == plek.draai;
                BlockPos landing = ok ? BlockPos.containing(reuzen.get(0).landing()) : BlockPos.ZERO;
                boolean wolk = ok && !level.getBlockState(landing.below()).isAir() && level.getBlockState(landing).isAir() && level.getBlockState(landing.above()).isAir();
                tel[ok && wolk ? 0 : 1]++;
                log(uit, ok && wolk, wie + ": the giant: " + reuzen.size() + " on his bed, turned with the castle: " + ok + "; the landing in front of the gate is cloud with air above: " + wolk
                        + (ok ? " (" + landing.toShortString() + ")" : ""));
            } else if (plek.naam.equals("bliksemsmidse")) {
                int n = level.getEntitiesOfClass(GuhNpcEntity.class, doos, g -> g.isAlive() && g.getKind() == GuhNpcEntity.Kind.SMIDGUH).size();
                tel[n == 1 ? 0 : 1]++;
                log(uit, n == 1, wie + ": the smid-guh: " + n + " in the forge");
            } else {
                BlockPos pot = StructureTemplate.transform(Brug.POT, Mirror.NONE, plek.draai, BlockPos.ZERO).offset(plek.hoek);
                boolean ok = level.getBlockState(pot).is(BouwWolk2Slice.POT.get());
                List<Bewakers.Stuk> boog = Bewakers.boog(level, plek.hoek, plek.draai);
                int mist = Bewakers.mist(level, boog);
                tel[ok && mist == 0 && !boog.isEmpty() ? 0 : 1]++;
                log(uit, ok && mist == 0 && !boog.isEmpty(), wie + ": the pot stands at the end (" + pot.toShortString() + "): " + ok + "; the rainbow: " + boog.size() + " pieces, " + mist + " missing");
            }
        }
        String slot = "[bio-zelftest] bouw_wolk2_inspectie klaar: " + GEVONDEN.size() + " places, " + tel[0] + " OK, " + tel[1] + " FOUT";
        uit.add(slot);
        LOGGER.info(slot);
        return uit;
    }

    /** Lets the kept chunks go again. */
    public static int los(ServerLevel level) {
        int n = 0;
        for (Plek plek : GEVONDEN) {
            for (int cx = (plek.doos.minX() >> 4) - 1; cx <= (plek.doos.maxX() >> 4) + 1; cx++) {
                for (int cz = (plek.doos.minZ() >> 4) - 1; cz <= (plek.doos.maxZ() >> 4) + 1; cz++) {
                    n += level.setChunkForced(cx, cz, false) ? 1 : 0;
                }
            }
        }
        GEVONDEN.clear();
        return n;
    }

    private Inspectie() {
    }
}
