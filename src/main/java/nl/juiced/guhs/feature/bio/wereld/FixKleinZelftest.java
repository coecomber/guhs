package nl.juiced.guhs.feature.bio.wereld;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.BioZelftest;

/**
 * biomes3 fix-klein: the self test "fix_klein" ({@code /guhs bio zelftest fix_klein}), in the real Guhmensie: the lake's bed
 * is sand and the lake's own sediment and nothing else, and {@link WolkLanding} is soft in the Wolkenweide and in the
 * column of a cloud building, and nowhere else.
 */
final class FixKleinZelftest {
    static void registreer() {
        BioZelftest.registreer("fix_klein", (server, level, meld) -> {
            BioModel m = BioWereldCommando.model(level);
            BlockPos van = new BlockPos(0, 80, 0);
            BlockPos meer = BioZelftest.vind(level, Bio.BLOESEMMEERTJE, van, 12000), weide = BioZelftest.vind(level, Bio.WOLKENWEIDE, van, 12000),
                    dal = BioZelftest.vind(level, Bio.KLATERDAL, van, 12000);
            if (meer == null || weide == null || dal == null) {
                meld.fout("a biome was not found: meer " + meer + ", weide " + weide + ", dal " + dal);
                return;
            }
            bed(level, m, meer, meld);
            meld.check(WolkLanding.zacht(level, weide), "a fall that ends in the Wolkenweide (" + weide.getX() + " " + weide.getZ() + ") is soft");
            meld.check(!WolkLanding.zacht(level, meer) && !WolkLanding.zacht(level, dal) && !WolkLanding.zacht(level, van),
                    "a fall in the lake, the valley or at 0 0 is not softened");
            var structuren = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            for (Identifier id : new java.util.TreeSet<>(WolkLanding.GEBOUWEN)) {
                var holder = structuren.get(id);
                if (holder.isEmpty()) {
                    meld.fout("no structure " + id);
                    continue;
                }
                var gevonden = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder.get()), weide, 100, false);
                if (gevonden == null) {
                    meld.ok(id.getPath() + ": none within 100 chunks of the Wolkenweide (not looked at)");
                    continue;
                }
                BlockPos p = gevonden.getFirst();
                level.getChunk(p.getX() >> 4, p.getZ() >> 4);
                boolean in = WolkLanding.inGebouw(level, p), niet = !WolkLanding.inGebouw(level, p.offset(160, 0, 0)) || WolkLanding.inWeide(level, p.offset(160, 0, 0));
                meld.check(in && niet, id.getPath() + " at " + p.getX() + " " + p.getZ() + ": its column is soft (" + in + "), 160 blocks beside it is not its column");
            }
        });
    }

    /** The bed under every water column of the lake chunks around p. */
    private static void bed(ServerLevel level, BioModel m, BlockPos p, BioZelftest.Melder meld) {
        Map<String, Integer> telling = new TreeMap<>();
        int vreemd = 0, wol = 0, kolommen = 0;
        String eerste = "";
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int cx = (p.getX() >> 4) - 6; cx <= (p.getX() >> 4) + 6; cx++) {
            for (int cz = (p.getZ() >> 4) - 6; cz <= (p.getZ() >> 4) + 6; cz++) {
                Kaart k = m.kaart(cx, cz);
                if (k.leeg) {
                    continue;
                }
                level.getChunk(cx, cz);
                for (int o = 0; o < 256; o++) {
                    if (k.soort[o] != Kaart.MEER || k.terras[o] >= 0 || k.meng[o] < 1f || k.water[o] == Kaart.GEEN) {
                        continue;
                    }
                    if (level.structureManager().hasAnyStructureAt(q.set((cx << 4) + (o & 15), k.hoogte[o], (cz << 4) + (o >> 4)))) {
                        continue;   // (a jetty post or a picnic island may stand on the bed)
                    }
                    kolommen++;
                    for (int dy = 0; dy <= 1; dy++) {
                        BlockState s = level.getBlockState(q.set((cx << 4) + (o & 15), k.hoogte[o] - dy, (cz << 4) + (o >> 4)));
                        String naam = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
                        telling.merge(naam, 1, Integer::sum);
                        boolean eigen = s.is(Blocks.SAND) || MeerBodem.trap().contains(s);
                        if (!eigen && !naam.contains("knuffelsteen")) {
                            vreemd++;
                            eerste = eerste.isEmpty() ? naam + " at " + q.getX() + " " + q.getY() + " " + q.getZ() : eerste;
                        }
                        wol += s.is(BlockTags.WOOL) || s.is(Blocks.CALCITE) || s.is(Blocks.CYAN_CONCRETE) ? 1 : 0;
                    }
                }
            }
        }
        meld.check(kolommen > 300 && vreemd == 0 && wol == 0, "the lake bed around " + p.getX() + " " + p.getZ() + ": " + kolommen + " water columns, " + telling
                + "; other than sand and sediment " + vreemd + (eerste.isEmpty() ? "" : " (" + eerste + ")") + ", wool / calcite / concrete " + wol);
        meld.check(telling.getOrDefault("bloesemmeertje_meerzand", 0) > 0 && telling.getOrDefault("bloesemmeertje_meerslib_licht", 0) > 0,
                "meerzand and meerslib lie in the lake");
    }

    private FixKleinZelftest() {
    }
}
