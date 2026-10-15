package nl.juiced.guhs.feature.einde2.h2water;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 h2_water: the fleet of the Vuurnatie in front of the ice wall, seen by ONE player during the siege (CONTRACT_E2_B
 * 7.3: "golf (laag_vloot washed away: per player)"). The overlay {@code noordelijke_waterstam/laag_vloot} of the build is
 * never put down in the world; its blocks are shown to a player through main's {@link Schijn} from the moment Yue-guh warns of
 * the fleet ({@link E2H2Water#HAVEN}) until the great wave ({@link E2H2Water#GOLF}, the flag {@code vloot_weg} that the wave
 * sets as it starts: the scene's own ships take their place and sail off). About 2,400 blocks, only while the player is
 * within reach of them.
 */
final class Vloot {
    /** The blocks of the overlay, relative to its corner, per server (read once from the structure manager). */
    private static final Map<Object, List<Blok>> BLOKKEN = new WeakHashMap<>();

    record Blok(BlockPos pos, BlockState state) {
    }

    private Vloot() {
    }

    /** Does this player see the fleet now? */
    static boolean ziet(ServerPlayer p) {
        Verhaallijn l = E2H2Water.lijn();
        int stap = l.stap(p);
        return l.aanDeBeurt(p) && stap >= E2H2Water.HAVEN && stap <= E2H2Water.GOLF && !l.vlag(p, E2H2Water.V_VLOOT_WEG);
    }

    /** Schijn source: the fleet's blocks near this player, at the copy of the Waterstam. */
    static void schijn(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        if (!ziet(p)) {
            return;
        }
        E2H2Water.Oord o = E2H2Water.oordVan(p);
        BlockPos anker = o == null ? null : o.wereld("vloot_anker");
        if (anker == null || anker.distToCenterSqr(p.position()) > 140 * 140) {
            return;
        }
        for (Blok b : blokken(o.level())) {
            BlockPos pos = anker.offset(b.pos());
            if (Schijn.dichtbij(p, pos) && o.level().getBlockState(pos).canBeReplaced()) {
                gewenst.put(pos, b.state());
            }
        }
    }

    /** The overlay's blocks (no air), read from its template once per server. */
    static synchronized List<Blok> blokken(ServerLevel level) {
        Object sleutel = level.getServer();
        List<Blok> al = BLOKKEN.get(sleutel);
        if (al != null) {
            return al;
        }
        List<Blok> uit = lees(level);
        BLOKKEN.put(sleutel, uit);
        return uit;
    }

    private static List<Blok> lees(ServerLevel level) {
        Plekken.Laag laag = Plekken.blad(E2H2Water.STRUCTUUR).lagen().get("vloot");
        StructureTemplate t = laag == null ? null : level.getStructureManager().get(laag.template()).orElse(null);
        if (t == null) {
            t = level.getStructureManager().get(Guhs.id(E2H2Water.STRUCTUUR + "/laag_vloot")).orElse(null);
        }
        if (t == null) {
            return List.of();
        }
        CompoundTag tag = t.save(new CompoundTag());
        ListTag palette = tag.getListOrEmpty("palette");
        var blocks = level.holderLookup(Registries.BLOCK);
        BlockState[] states = new BlockState[palette.size()];
        for (int i = 0; i < palette.size(); i++) {
            states[i] = NbtUtils.readBlockState(blocks, palette.getCompoundOrEmpty(i));
        }
        java.util.ArrayList<Blok> uit = new java.util.ArrayList<>();
        for (Tag e : tag.getListOrEmpty("blocks")) {
            if (!(e instanceof CompoundTag b)) {
                continue;
            }
            int i = b.getIntOr("state", -1);
            BlockState st = i >= 0 && i < states.length ? states[i] : null;
            var pos = b.getListOrEmpty("pos");
            if (st == null || st.isAir() || pos.size() < 3) {
                continue;
            }
            uit.add(new Blok(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), st));
        }
        return List.copyOf(uit);
    }

    /** (tests) the blocks, when the template is there. */
    @Nullable
    static Integer aantal(ServerLevel level) {
        return blokken(level).size();
    }
}
