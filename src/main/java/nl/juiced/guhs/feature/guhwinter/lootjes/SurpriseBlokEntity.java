package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The data of one block of a placed Surprise: the block it stands for ({@link #inhoud}, turned the way the gift was placed)
 * and where its kern is. The kern also holds the whole gift ({@link #gift}: the item's data, front to the south), how it
 * was turned ({@link #draai}), the min corner of its 7x7x7 room ({@link #oorsprong}) and who placed it ({@link #plaatser}:
 * only they can pick it up). Everything goes to the client with the chunk (it draws and collides from it).
 */
public class SurpriseBlokEntity extends BlockEntity {
    BlockState inhoud = Blocks.AIR.defaultBlockState();
    @Nullable
    BlockPos kern;
    @Nullable
    SurpriseData gift;
    int draai;
    @Nullable
    BlockPos oorsprong;
    @Nullable
    UUID plaatser;

    /** (client) goes up whenever new data came in: the renderer rebuilds its cached mesh then. */
    public int versie;
    /** (client) the renderer's cached mesh (an object of the client package; untouched on the server). */
    public Object mesh;

    public SurpriseBlokEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    static SurpriseBlokEntity kern(BlockPos pos, BlockState state) {
        return new SurpriseBlokEntity(LootjesFeature.SURPRISE_BE.get(), pos, state);
    }

    static SurpriseBlokEntity deel(BlockPos pos, BlockState state) {
        return new SurpriseBlokEntity(LootjesFeature.SURPRISE_DEEL_BE.get(), pos, state);
    }

    public boolean isKern() {
        return gift != null;
    }

    @Nullable
    public SurpriseData gift() {
        return gift;
    }

    public Rotation draai() {
        return Rotation.values()[Math.floorMod(draai, 4)];
    }

    @Nullable
    public BlockPos oorsprong() {
        return oorsprong;
    }

    @Nullable
    public BlockPos kernPos() {
        return kern;
    }

    public BlockState inhoud() {
        return inhoud;
    }

    void zetDeel(BlockState in, BlockPos kernPos) {
        this.inhoud = in;
        this.kern = kernPos.immutable();
        setChanged();
    }

    void zetKern(BlockState in, SurpriseData gift, int draai, BlockPos oorsprong, UUID plaatser) {
        this.inhoud = in;
        this.kern = worldPosition.immutable();
        this.gift = gift;
        this.draai = Math.floorMod(draai, 4);
        this.oorsprong = oorsprong.immutable();
        this.plaatser = plaatser;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inhoud = input.read("Inhoud", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
        kern = input.read("Kern", BlockPos.CODEC).orElse(null);
        gift = input.read("Gift", SurpriseData.CODEC).orElse(null);
        draai = input.getIntOr("Draai", 0);
        oorsprong = input.read("Oorsprong", BlockPos.CODEC).orElse(null);
        plaatser = input.read("Plaatser", UUIDUtil.CODEC).orElse(null);
        versie++;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Inhoud", BlockState.CODEC, inhoud);
        if (kern != null) {
            output.store("Kern", BlockPos.CODEC, kern);
        }
        if (gift != null) {
            output.store("Gift", SurpriseData.CODEC, gift);
            output.putInt("Draai", draai);
            if (oorsprong != null) {
                output.store("Oorsprong", BlockPos.CODEC, oorsprong);
            }
            if (plaatser != null) {
                output.store("Plaatser", UUIDUtil.CODEC, plaatser);
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
