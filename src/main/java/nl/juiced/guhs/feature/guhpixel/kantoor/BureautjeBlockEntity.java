package nl.juiced.guhs.feature.guhpixel.kantoor;

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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;
import nl.juiced.guhs.taal.Tekst;

/**
 * A Bureautje: which Prikklok it belongs to, and a COPY of who sleeps here (band id, name, looks, owner) so the client
 * can draw the guh on the keyboard. The guh itself is safe in {@link KantoorData}; this block entity never owns it.
 * Breaking the desk (however it goes) sends the guh home: {@link #preRemoveSideEffects}.
 */
public class BureautjeBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos klok;
    @Nullable
    private UUID guh;
    @Nullable
    private UUID eigenaar;
    private Component naam = Component.empty();
    private Component baas = Component.empty();
    private CompoundTag looks = new CompoundTag();

    /** Client only: the stand-in that the renderer draws (made from {@link #looks}), and for which guh it was made. */
    @Nullable
    public GuhEntity weergave;
    @Nullable
    public UUID weergaveVan;
    private int tik;

    public BureautjeBlockEntity(BlockPos pos, BlockState state) {
        super(KantoorSlice.BUREAUTJE_BE.get(), pos, state);
    }

    @Nullable
    public BlockPos klok() {
        return klok;
    }

    public void zetKlok(@Nullable BlockPos pos) {
        klok = pos == null ? null : pos.immutable();
        setChanged();
    }

    public boolean bezet() {
        return guh != null;
    }

    @Nullable
    public UUID guh() {
        return guh;
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    public Component naam() {
        return naam;
    }

    public Component baas() {
        return baas;
    }

    public CompoundTag looks() {
        return looks;
    }

    /** A guh clocked in here: its copy for the renderer. */
    public void zet(UUID guh, UUID eigenaar, Component naam, Component baas, CompoundTag looks) {
        this.guh = guh;
        this.eigenaar = eigenaar;
        this.naam = naam;
        this.baas = baas;
        this.looks = looks.copy();
        sync(true);
    }

    /** The desk is free again. */
    public void leeg() {
        if (guh == null) {
            return;
        }
        guh = null;
        eigenaar = null;
        naam = Component.empty();
        baas = Component.empty();
        looks = new CompoundTag();
        sync(false);
    }

    /** While the desk itself is being removed: forget the copy without touching the world. */
    void vergeet() {
        guh = null;
        eigenaar = null;
        looks = new CompoundTag();
    }

    private void sync(boolean bezet) {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(BureautjeBlock.BEZET) && state.getValue(BureautjeBlock.BEZET) != bezet) {
                level.setBlock(worldPosition, state.setValue(BureautjeBlock.BEZET, bezet), 3);
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Server: soft zzz above a sleeping guh, now and then a paw on the keyboard; a copy without a record is cleared. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BureautjeBlockEntity be) {
        if (be.guh == null || !(level instanceof ServerLevel server)) {
            return;
        }
        be.tik++;
        if (be.tik % 50 == 25) {
            server.sendParticles(VadswoudFeature.GUH_ZZZ.get(), pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5, 1, 0.12, 0.05, 0.12, 0.0);
        }
        if (be.tik % 100 == 60) {
            KantoorData.Werk w = KantoorData.get(server.getServer()).van(be.guh);
            if (w == null || w.dim != server.dimension() || !w.pos.equals(pos)) {
                be.leeg();   // (the record is gone or lives at another desk: this copy is stale)
            } else if (server.getRandom().nextInt(12) == 0) {
                server.playSound(null, pos, KantoorSlice.TOETSENBORD.get(), SoundSource.NEUTRAL, 0.5f, 0.9f + server.getRandom().nextFloat() * 0.3f);
            }
        }
    }

    /** Client: the stand-in's animation clock. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, BureautjeBlockEntity be) {
        if (be.weergave != null) {
            be.weergave.tickCount++;
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        klok = in.read("Klok", BlockPos.CODEC).orElse(null);
        guh = in.read("Guh", UUIDUtil.CODEC).orElse(null);
        eigenaar = in.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        naam = Tekst.get(in, "Naam");
        baas = Tekst.get(in, "Baas");
        looks = in.read("Looks", CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("Bezet", guh != null);   // (always a key: an empty update would be ignored by the client)
        if (klok != null) {
            out.store("Klok", BlockPos.CODEC, klok);
        }
        if (guh != null) {
            out.store("Guh", UUIDUtil.CODEC, guh);
            if (eigenaar != null) {
                out.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
            }
            Tekst.put(out, "Naam", naam);
            Tekst.put(out, "Baas", baas);
            out.store("Looks", CompoundTag.CODEC, looks);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            Kantoor.bureauWeg(server, pos, state, this);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
