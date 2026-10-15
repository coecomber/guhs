package nl.juiced.guhs.feature.techbron;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.vadskracht.Meerblok;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsBron;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The block entity of a vadskracht source of tech-bronnen: a {@link VadsBron} that tells its net when what it gives
 * changes, keeps its face up to date ({@link #gezicht}; asleep when it does not count in its net: one source too many of
 * its kind) and syncs its data to the clients. A subclass says what it gives ({@code vadsSoort}, {@code vadsAanbod}), what
 * its face is and what happens every tick ({@link #tik}).
 */
public abstract class BronBlockEntity extends BlockEntity implements VadsBron {
    /** Players this close get the advancements of a source that gives ({@link #beloon}). */
    private static final double BELOON_BEREIK = 16;

    private boolean teltMee = true;
    /** What the net was last told this source gives (-1: nothing yet, so a source put there without a block update still finds its net). */
    private int gemeld = -1;
    private long beloondOp = Long.MIN_VALUE;

    protected BronBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // =====================================================================================================================
    // what a source fills in
    // =====================================================================================================================

    /** The face that goes with what the source does right now (it is shown asleep anyway when it does not count). */
    protected abstract Snoet gezicht();

    /** Every server tick. */
    protected void tik(ServerLevel level) {
    }

    /** Every client tick (animations, particles). */
    protected void clientTick() {
    }

    // =====================================================================================================================
    // vadskracht
    // =====================================================================================================================

    @Override
    public BlockPos vadsPlek() {
        return worldPosition;
    }

    @Override
    public void vadsTelt(boolean teltMee) {
        this.teltMee = teltMee;
    }

    /** Does this source count in its net (false: there are more of its kind than count)? */
    public boolean teltMee() {
        return teltMee;
    }

    final void serverTick() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        tik(server);
        int nu = vadsAanbod();
        if (nu != gemeld) {
            gemeld = nu;
            VadsKracht.veranderd(server, worldPosition);
        }
        BlockState state = getBlockState();
        if (state.hasProperty(BronBlock.SNOET)) {
            Snoet snoet = teltMee ? gezicht() : Snoet.SLAAPT;
            if (state.getValue(BronBlock.SNOET) != snoet) {
                level.setBlock(worldPosition, state.setValue(BronBlock.SNOET, snoet), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** The box of all blocks of this source. */
    protected AABB vloer() {
        return BronBlock.vloer(worldPosition, getBlockState());
    }

    /** The source's blocks and the air above them, widened by this much sideways: where a guh is "on" the source. */
    protected AABB zone(double rand) {
        AABB vloer = vloer();
        return new AABB(vloer.minX - rand, vloer.minY - 0.1, vloer.minZ - rand, vloer.maxX + rand, vloer.minY + 1.75, vloer.maxZ + rand);
    }

    /**
     * The source does its thing: everybody close by gets the hidden advancement {@code quest/tech_bronnen_<naam>} (for the
     * FTB quests of tech-quests) and the visible {@code techniek/tech_bronnen_<naam>}. Call it as often as you like while
     * the source gives: it looks for players at most once per five seconds.
     */
    protected void beloon(String naam) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() - beloondOp < 100) {
            return;
        }
        beloondOp = server.getGameTime();
        for (ServerPlayer p : server.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(BELOON_BEREIK), p -> !p.isSpectator())) {
            GuhAdvancements.grant(p, "tech_bronnen_" + naam);
            GidsFeature.grant(p, "techniek/tech_bronnen_" + naam);
        }
    }

    /** Sends the block entity's data to the clients that see it. */
    protected final void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // =====================================================================================================================
    // removal, syncing
    // =====================================================================================================================

    /** Removed (however): the parts of a source bigger than one block go with it (call super when you override). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (state.getBlock() instanceof BronBlock bron) {
            Meerblok.ruimOp(level, pos, state, bron.deel());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
