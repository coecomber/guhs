package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.juiced.guhs.feature.einde2.Plekken;

/**
 * einde2 phase B, slice h4_vuur: the overlay {@code vuurnatie_hoofdstad/laag_leeuwschildpad} (the shell, the face and the
 * flippers of the Leeuwschildpad-guh that surfaces beside the capital; bouw_vuur) shown to ONE player for the night of the
 * finale: its blocks are sent to that player's client only, once, and the real blocks (the empty bite out of the rim) are
 * sent again afterwards. The world never changes, so every other player keeps seeing what is really there
 * (CONTRACT_E2_B 2.4: never a world change that every player sees for one player's story). While it is shown the player
 * sits in a cutscene (locked), so no chunk of it is reloaded underneath; a chunk that does reload simply shows the real air.
 * About 6,000 blocks, once: the size of a few chunk packets.
 */
final class Lagen {
    /** The overlay of the build (bouw_vuur: E2BouwVuurFeature.LAAG_LEEUWSCHILDPAD). */
    static final String LAAG = "leeuwschildpad";

    private record Blok(BlockPos rel, BlockState state) {
    }

    @Nullable
    private static volatile List<Blok> blokken;
    /** Player -> the world positions sent to them. */
    private static final Map<UUID, List<BlockPos>> GETOOND = new ConcurrentHashMap<>();

    private Lagen() {
    }

    /** Shows the Leeuwschildpad-guh to this player. False: the overlay or the capital is not known. */
    static boolean toon(ServerPlayer p) {
        ServerLevel g = Plek.guheinde(p);
        Plekken.Laag laag = Plekken.blad(Plek.STRUCTUUR).lagen().get(LAAG);
        BlockPos o = Plek.oorsprong(p);
        List<Blok> lijst = g == null ? null : lees(g, laag);
        if (lijst == null || o == null) {
            return false;
        }
        weg(p);
        BlockPos at = o.offset(laag.pos());
        List<BlockPos> getoond = new ArrayList<>(lijst.size());
        for (Blok b : lijst) {
            BlockPos pos = at.offset(b.rel());
            stuur(p, pos, b.state());
            getoond.add(pos);
        }
        GETOOND.put(p.getUUID(), getoond);
        return true;
    }

    /** Takes it away again: the real blocks. */
    static void weg(ServerPlayer p) {
        List<BlockPos> had = GETOOND.remove(p.getUUID());
        if (had == null) {
            return;
        }
        ServerLevel level = p.level();
        for (BlockPos pos : had) {
            if (level.isLoaded(pos)) {
                stuur(p, pos, level.getBlockState(pos));
            }
        }
    }

    static boolean getoond(ServerPlayer p) {
        return GETOOND.containsKey(p.getUUID());
    }

    private static void stuur(ServerPlayer p, BlockPos pos, BlockState state) {
        if (!(p instanceof FakePlayer) && p.connection != null) {
            p.connection.send(new ClientboundBlockUpdatePacket(pos, state));
        }
    }

    /** The blocks of the overlay template (read once): everything but air. */
    @Nullable
    private static List<Blok> lees(ServerLevel level, @Nullable Plekken.Laag laag) {
        List<Blok> l = blokken;
        if (l != null || laag == null) {
            return l;
        }
        StructureTemplate t = level.getStructureManager().get(laag.template()).orElse(null);
        if (t == null) {
            return null;
        }
        CompoundTag nbt = t.save(new CompoundTag());
        ListTag palet = nbt.getListOrEmpty("palette");
        List<BlockState> staten = new ArrayList<>();
        var blokLookup = level.holderLookup(Registries.BLOCK);
        for (Tag tag : palet) {
            staten.add(NbtUtils.readBlockState(blokLookup, (CompoundTag) tag));
        }
        List<Blok> uit = new ArrayList<>();
        for (Tag tag : nbt.getListOrEmpty("blocks")) {
            CompoundTag b = (CompoundTag) tag;
            int nr = b.getIntOr("state", 0);
            if (nr < 0 || nr >= staten.size() || staten.get(nr).isAir()) {
                continue;
            }
            ListTag pos = b.getListOrEmpty("pos");
            uit.add(new Blok(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), staten.get(nr)));
        }
        blokken = l = List.copyOf(uit);
        return l;
    }
}
