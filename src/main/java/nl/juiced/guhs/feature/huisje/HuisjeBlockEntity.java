package nl.juiced.guhs.feature.huisje;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.Vriendjes;
import nl.juiced.guhs.feature.emotes.EmotesFeature;

/**
 * The controller of a Guhhuisje: drawn big by the client's HuisjeRenderer. Server side it breathes: while residents
 * sleep inside, zzz float out of the eye windows (and little hearts when friends sleep together), with a soft snore now
 * and then.
 */
public class HuisjeBlockEntity extends BlockEntity {
    private int tik;
    /** 3.0: the owner, synced to the client (so a non-owner doesn't even start breaking it: HuisjeBlock.getDestroyProgress). */
    @javax.annotation.Nullable
    private java.util.UUID eigenaar;

    @javax.annotation.Nullable
    public java.util.UUID eigenaar() {
        return eigenaar;
    }

    void zetEigenaar(java.util.UUID id) {
        if (!id.equals(eigenaar)) {
            eigenaar = id;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    /** May this player break it (client and server: the owner, or an op; unknown owner: yes)? */
    public boolean magBreken(net.minecraft.world.entity.player.Player p) {
        return eigenaar == null || eigenaar.equals(p.getUUID()) || p.hasPermissions(2);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (eigenaar != null) {
            tag.putUUID("Eigenaar", eigenaar);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        eigenaar = tag.hasUUID("Eigenaar") ? tag.getUUID("Eigenaar") : null;
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = super.getUpdateTag(registries);
        if (eigenaar != null) {
            tag.putUUID("Eigenaar", eigenaar);
        }
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    public HuisjeBlockEntity(BlockPos pos, BlockState state) {
        super(HuisjeFeature.HUISJE_BE.get(), pos, state);
    }

    public HuisjeMaat maat() {
        return getBlockState().getBlock() instanceof HuisjeBlock b ? b.maat() : HuisjeMaat.KLEIN;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HuisjeBlockEntity be) {
        if (++be.tik % 50 != 0 || !(level instanceof ServerLevel sl)) {
            return;
        }
        Huisje h = Huisjes.van(sl, pos);
        if (h != null && be.eigenaar == null) {
            be.zetEigenaar(h.eigenaar());   // (3.0: huisjes from before 3.0 learn their owner)
        }
        if (h == null || h.bewoners().isEmpty()) {
            return;
        }
        Vec3 m = h.midden();
        AABB binnen = new AABB(m.x - 2.5, pos.getY() - 1, m.z - 2.5, m.x + 2.5, pos.getY() + h.maat().hoogte() + 1, m.z + 2.5);
        List<Entity> slapers = sl.getEntities((Entity) null, binnen, Huisjes::isBinnen);
        if (slapers.isEmpty()) {
            return;
        }
        boolean links = sl.random.nextBoolean();
        Vec3 raam = h.raam(links);
        sl.sendParticles(EmotesFeature.GUH_ZZZ.get(), raam.x, raam.y, raam.z, 1, 0.05, 0.05, 0.05, 0.01);
        if (sl.random.nextInt(4) == 0) {
            sl.playSound(null, pos, HuisjeFeature.SNURK_GELUID.get(), SoundSource.NEUTRAL, 0.35f, 0.9f + sl.random.nextFloat() * 0.2f);
        }
        if (slapers.size() >= 2 && vriendjesSamen(sl, slapers)) {
            Vec3 ander = h.raam(!links);
            sl.sendParticles(BandFeature.HARTJE.get(), ander.x, ander.y + 0.1, ander.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    /** Are there two friends among the sleepers? */
    private static boolean vriendjesSamen(ServerLevel level, List<Entity> slapers) {
        for (int i = 0; i < slapers.size(); i++) {
            for (int j = i + 1; j < slapers.size(); j++) {
                if (Vriendjes.vrienden(level.getServer(), Band.id(slapers.get(i)), Band.id(slapers.get(j)))) {
                    return true;
                }
            }
        }
        return false;
    }
}
