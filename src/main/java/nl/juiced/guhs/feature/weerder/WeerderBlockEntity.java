package nl.juiced.guhs.feature.weerder;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Wilde-guhweerder's data: its owner (the one who placed it) and the radius of its area. Synced to the client (the
 * blue dome is drawn with this radius; a non-owner doesn't even start breaking it). The level's {@link WeerderIndex} keeps
 * the radius for the spawn checks; this block entity heals the index when it loads.
 */
public class WeerderBlockEntity extends BlockEntity {
    @Nullable
    private UUID eigenaar;
    private String eigenaarNaam = "";
    private int straal = WeerderFeature.STANDAARD;

    public WeerderBlockEntity(BlockPos pos, BlockState state) {
        super(WeerderFeature.WEERDER_BE.get(), pos, state);
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    public String eigenaarNaam() {
        return eigenaarNaam;
    }

    public int straal() {
        return straal;
    }

    public void zetEigenaar(Player p) {
        zetEigenaar(p.getUUID(), p.getGameProfile().name());
    }

    public void zetEigenaar(UUID id, String naam) {
        eigenaar = id;
        eigenaarNaam = naam == null ? "" : naam;
        veranderd();
    }

    /** A new radius (one of {@link WeerderFeature#STRALEN}; anything else is ignored): false when it is not a step. */
    public boolean zetStraal(int nieuw) {
        if (!WeerderFeature.STRALEN.contains(nieuw)) {
            return false;
        }
        straal = nieuw;
        veranderd();
        if (level instanceof ServerLevel sl) {
            WeerderIndex.zet(sl, worldPosition, straal);
        }
        return true;
    }

    /** May this player change or break it (client and server: its owner, or an op; no owner yet: anyone)? */
    public boolean magBewerken(Player p) {
        return eigenaar == null || eigenaar.equals(p.getUUID()) || p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** "Dit is de Wilde-guhweerder van X". */
    public Component vanWie() {
        return Component.translatable("gui.guhs.weerder.van_wie", eigenaarNaam.isEmpty() ? "?" : eigenaarNaam);
    }

    private void veranderd() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl && WeerderIndex.straalOp(sl, worldPosition) != straal) {
            WeerderIndex.zet(sl, worldPosition, straal);   // (heals the index, e.g. a weerder placed by a structure or command)
        }
    }

    /** 1.1.0 (onRemove is gone): the weerder was broken or replaced: its area is gone too. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl) {
            WeerderIndex.weg(sl, pos);
        }
    }

    // =====================================================================================================================

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (eigenaar != null) {
            tag.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }
        tag.putString("EigenaarNaam", eigenaarNaam);
        tag.putInt("Straal", straal);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        eigenaar = tag.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        eigenaarNaam = tag.getStringOr("EigenaarNaam", "");
        int s = tag.getIntOr("Straal", WeerderFeature.STANDAARD);
        straal = WeerderFeature.STRALEN.contains(s) ? s : WeerderFeature.STANDAARD;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (eigenaar != null) {
            tag.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }
        tag.putString("EigenaarNaam", eigenaarNaam);
        tag.putInt("Straal", straal);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
