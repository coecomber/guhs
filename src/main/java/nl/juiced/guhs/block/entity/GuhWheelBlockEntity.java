package nl.juiced.guhs.block.entity;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.GuhradKracht;
import nl.juiced.guhs.feature.vadskracht.VadsBron;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.registry.ModBlockEntities;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * Holds the guh that's running in the wheel (stored as entity data, like bees in a beehive),
 * plus client-side animation state for the spinning wheel.
 * <p>
 * bbq2: the wheel is a source of vadskracht ({@link VadsBron}, kind GUHRAD): while a guh runs it gives what that guh's
 * variant gives ({@link GuhradKracht}: 10 VK, a happy guh 15, the story guhs more). A guh never tires and needs no food.
 * <p>
 * bbq2 (tech-bronnen): every story guh does its rounds in its own way ({@link nl.juiced.guhs.feature.techbron.GuhradStijl}:
 * the wheel's speed, a floating Guhtwo, a line in the hover readout), and the readout tips that a happy guh runs harder.
 */
public class GuhWheelBlockEntity extends BlockEntity implements VadsBron {
    @Nullable
    private CompoundTag guhData;
    /** Was the guh "blij" when it was put in (then it runs extra hard for as long as it is in the wheel)? */
    private boolean blij;
    /**
     * (1.7.0) A wheel of a building (the Vadsmijnstadje's centrale, the Kaasmakerij's pomphuis, the Wolclub): its guh has no
     * owner, so nobody takes it out, and when the wheel is broken it does not drop as an item but hops out as a wild guh.
     */
    private boolean vast;
    /**
     * (1.7.0, ronde 45-47) A building's wheel whose lever is on: its guh takes a break (gives nothing, the wheel stands still)
     * while a redstone signal reaches the wheel block from a lever, button... next to it (Guhdraad and the Guhrad's own
     * parts do not count: {@link nl.juiced.guhs.feature.techbuis.Buizen#redstone}). Only {@link #vast} wheels: a player's own
     * wheel ignores redstone as it always did.
     */
    private boolean rust;
    /** Does this wheel count in its net (false: one Guhrad too many)? Not saved: the net tells it at every evaluation. */
    private boolean teltMee = true;

    // client-side only: a copy of the guh for rendering, and the wheel angle
    @Nullable
    private GuhEntity displayGuh;
    private float spin;
    private float spinO;

    public GuhWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GUH_WHEEL.get(), pos, state);
    }

    public boolean hasGuh() {
        return guhData != null;
    }

    @Nullable
    public UUID getGuhOwner() {
        return guhData != null && guhData.read("Owner", UUIDUtil.CODEC).isPresent() ? guhData.read("Owner", UUIDUtil.CODEC).orElseThrow() : null;
    }

    /** Puts a (picked-up) guh into the wheel. */
    public void insert(CompoundTag guh) {
        guhData = guh.copy();
        vast = false;   // (1.7.0: someone's own guh, not the building's)
        blij = level != null && GuhradKracht.isBlij(guhData, level.getGameTime());
        sync();
        VadsKracht.veranderd(level, worldPosition);
    }

    /** (1.6.2, "Roep naar mij") the running guh's data, read only (null: empty). */
    @Nullable
    public CompoundTag guhData() {
        return guhData;
    }

    /** Takes the guh out again, as picked-up guh data (or null if the wheel was empty). */
    @Nullable
    public CompoundTag takeOut() {
        CompoundTag tag = guhData;
        guhData = null;
        blij = false;
        vast = false;
        rust = false;
        sync();
        VadsKracht.veranderd(level, worldPosition);
        return tag;
    }

    /** (1.7.0) Does the building's guh take a break (its lever is on, see {@link #rust})? */
    public boolean rust() {
        return rust;
    }

    /**
     * (1.7.0) A neighbour of the wheel block changed: a building's wheel looks whether its lever is on (then the guh takes a
     * break) or off again (then it runs on).
     */
    public void kijkHendel() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel) || (!vast && !rust)) {
            return;
        }
        boolean nu = vast && nl.juiced.guhs.feature.techbuis.Buizen.redstone(level, worldPosition);
        if (nu == rust) {
            return;
        }
        rust = nu;
        BlockState state = getBlockState();
        if (state.hasProperty(nl.juiced.guhs.block.GuhWheelBlock.RUNNING)) {
            level.setBlock(worldPosition, state.setValue(nl.juiced.guhs.block.GuhWheelBlock.RUNNING, guhData != null && !rust), 3);
        }
        sync();
        VadsKracht.veranderd(level, worldPosition);
    }

    /** (1.7.0) Is this a building's wheel (see {@link #vast})? */
    public boolean isVast() {
        return vast;
    }

    // --- vadskracht ---

    /**
     * (1.7.0) However the wheel got here (a structure placed in a chunk that was generated, a chunk that loads, a template),
     * its net is built around it at the next tick: a wheel never waits for a block update to give vadskracht.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            VadsKracht.herbouw(server, worldPosition);
        }
    }

    @Override
    public BlockPos vadsPlek() {
        return worldPosition;
    }

    @Override
    public BronSoort vadsSoort() {
        return BronSoort.GUHRAD;
    }

    @Override
    public int vadsAanbod() {
        return guhData == null || rust ? 0 : GuhradKracht.van(guhData, blij);
    }

    @Override
    public void vadsTelt(boolean teltMee) {
        this.teltMee = teltMee;
    }

    /** Does this wheel count in its net (false: there are more Guhraden than count)? */
    public boolean teltMee() {
        return teltMee;
    }

    /** Runs the guh in this wheel extra hard (it was happy when it was put in)? */
    public boolean isBlij() {
        return guhData != null && blij;
    }

    /** How the guh in this wheel does its rounds (tech-bronnen: the Baltoguh runs harder, Guhtwo floats...). Both sides. */
    public nl.juiced.guhs.feature.techbron.GuhradStijl stijl() {
        return nl.juiced.guhs.feature.techbron.GuhradStijl.van(guhData);
    }

    /** Would the guh in this wheel give more if it were happy (so the hover readout gives the tip)? */
    public boolean kanBlijer() {
        return guhData != null && !blij && GuhradKracht.van(guhData, true) > GuhradKracht.van(guhData, false);
    }

    @Override
    public void vadsRegels(java.util.function.Consumer<net.minecraft.network.chat.Component> regels) {
        if (guhData == null) {
            regels.accept(net.minecraft.network.chat.Component.translatable("gui.guhs.vadskracht.guhrad.leeg").withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }
        if (rust) {
            regels.accept(net.minecraft.network.chat.Component.translatable("gui.guhs.vadskracht.guhrad.rust").withStyle(net.minecraft.ChatFormatting.GOLD));
        }
        // tech-bronnen: what is special about this guh (per variant), then its mood
        net.minecraft.network.chat.Component stijl = stijl().regel();
        if (stijl != null) {
            regels.accept(stijl);
        }
        if (blij) {
            regels.accept(net.minecraft.network.chat.Component.translatable("gui.guhs.vadskracht.guhrad.blij").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        } else if (kanBlijer()) {
            regels.accept(net.minecraft.network.chat.Component.translatable("gui.guhs.techbron.guhrad.tip").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- client animation ---

    public static void clientTick(Level level, BlockPos pos, BlockState state, GuhWheelBlockEntity be) {
        be.spinO = be.spin;
        if (be.guhData == null) {
            be.displayGuh = null;
            return;
        }
        if (be.rust) {
            return;   // (1.7.0: the building's guh takes a break, its lever is on: the wheel stands still)
        }
        // tech-bronnen: the wheel turns as hard as its guh runs (per variant; a happy guh a bit faster still)
        nl.juiced.guhs.feature.techbron.GuhradStijl stijl = be.stijl();
        float tempo = stijl.tempo * (be.blij ? 1.25f : 1f);
        be.spin += 9f * tempo;
        GuhEntity guh = be.getDisplayGuh();
        if (guh != null) {
            guh.tickCount++; // drives the GeckoLib animation clock
            // "moving" -> walk animation; a floating guh (Guhtwo) keeps its legs still
            guh.walkAnimation.update(stijl.zweeft ? 0f : Math.min(1.6f, tempo), 0.4f, guh.isBaby() ? 3.0f : 1.0f);
        }
    }

    public float getSpin(float partialTick) {
        return spinO + (spin - spinO) * partialTick;
    }

    @Nullable
    public GuhEntity getDisplayGuh() {
        if (displayGuh == null && guhData != null && level != null) {
            displayGuh = EntityType.create(nl.juiced.guhs.storage.Nbt.input(level.registryAccess(), guhData), level, net.minecraft.world.entity.EntitySpawnReason.LOAD).filter(e -> e instanceof GuhEntity).map(e -> (GuhEntity) e).orElse(null);
            if (displayGuh != null) {
                displayGuh.setInSittingPose(false);
                displayGuh.setRunningInWheel(true);
            }
        }
        return displayGuh;
    }

    // --- saving & syncing ---

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        CompoundTag newData = tag.read("Guh", CompoundTag.CODEC).orElse(null);
        if (newData == null || !newData.equals(guhData)) {
            displayGuh = null;
        }
        guhData = newData;
        blij = tag.getBooleanOr("Blij", false);
        vast = tag.getBooleanOr("Vast", false);
        rust = tag.getBooleanOr("Rust", false);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        // always write something: an empty update would be ignored by the client, and the wheel would keep
        // showing a guh that was already taken out
        tag.putBoolean("HasGuh", guhData != null);
        tag.putBoolean("Blij", blij);
        if (vast) {
            tag.putBoolean("Vast", true);
        }
        if (rust) {
            tag.putBoolean("Rust", true);
        }
        if (guhData != null) {
            tag.store("Guh", CompoundTag.CODEC, guhData);
        }
    }

    /**
     * Breaking the wheel (however it goes) drops the guh that was in it (as a picked-up guh item) and removes the
     * wheel's other parts. 26.1: was GuhWheelBlock#onRemove.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean vanGebouw = vast;
        CompoundTag guh = takeOut();
        if (guh != null && vanGebouw && level instanceof net.minecraft.server.level.ServerLevel server) {
            // (1.7.0) a building's guh: it hops out of the broken wheel as a wild guh (nobody pockets it)
            EntityType.create(nl.juiced.guhs.storage.Nbt.input(server.registryAccess(), guh), server, net.minecraft.world.entity.EntitySpawnReason.LOAD)
                    .ifPresent(e -> {
                        e.snapTo(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, e.getYRot(), 0f);
                        server.addFreshEntity(e);
                    });
            guh = null;
        }
        if (guh != null) {
            net.minecraft.world.item.ItemStack item = nl.juiced.guhs.item.PickedUpGuhItem.of(guh);
            nl.juiced.guhs.feature.band.GuhVolger.item(item, nl.juiced.guhs.feature.band.PlekSoort.ITEM_GROND, level.dimension(), pos, "",
                    level.getGameTime());   // 2.10: "waar is mijn guh": the wheel broke, it lies on the ground
            net.minecraft.world.level.block.Block.popResource(level, pos, item);
        }
        if (state.hasProperty(nl.juiced.guhs.block.GuhWheelBlock.FACING)) {
            for (BlockPos part : nl.juiced.guhs.block.GuhWheelBlock.partPositions(pos, state.getValue(nl.juiced.guhs.block.GuhWheelBlock.FACING))) {
                if (level.getBlockState(part).is(nl.juiced.guhs.registry.ModBlocks.GUH_WHEEL_PART.get())) {
                    level.setBlock(part, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
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
