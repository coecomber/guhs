package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A hung drawing ({@code guhs:guhwinter_tekening}): always 2x2 blocks, like a painting, showing the real drawing in a small
 * wooden frame with pink corners (client: TekeningRenderer, the picture fetched by id). Hit it and it comes down as its item
 * again (always, also in creative: it is one of a kind). It never melts.
 */
public class TekeningEntity extends HangingEntity {
    public static final int MAAT = 2;
    private static final EntityDataAccessor<String> DATA_ID = SynchedEntityData.defineId(TekeningEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_MAKER = SynchedEntityData.defineId(TekeningEntity.class, EntityDataSerializers.STRING);

    public TekeningEntity(EntityType<? extends TekeningEntity> type, Level level) {
        super(type, level);
    }

    public TekeningEntity(Level level, BlockPos pos, Direction richting, TekeningRef ref) {
        super(TekenenFeature.TEKENING_ENTITY.get(), level, pos);
        zetRef(ref);
        setDirection(richting);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ID, "");
        entityData.define(DATA_MAKER, "");
    }

    private void zetRef(TekeningRef ref) {
        entityData.set(DATA_ID, ref.id().toString());
        entityData.set(DATA_MAKER, ref.maker());
    }

    @Nullable
    public UUID tekeningId() {
        String s = entityData.get(DATA_ID);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String maker() {
        return entityData.get(DATA_MAKER);
    }

    @Nullable
    private TekeningRef ref() {
        UUID id = tekeningId();
        return id == null ? null : new TekeningRef(id, maker());
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        // as a 2x2 painting: half a block to the left and up from the clicked spot, against the wall
        Vec3 tegenMuur = Vec3.atCenterOf(pos).relative(direction, -0.46875);
        Vec3 midden = tegenMuur.relative(direction.getCounterClockWise(), 0.5).relative(Direction.UP, 0.5);
        Direction.Axis as = direction.getAxis();
        double xs = as == Direction.Axis.X ? 0.0625 : MAAT;
        double zs = as == Direction.Axis.Z ? 0.0625 : MAAT;
        return AABB.ofSize(midden, xs, MAAT, zs);
    }

    @Override
    public void dropItem(ServerLevel level, @Nullable Entity causedBy) {
        playSound(SoundEvents.PAINTING_BREAK, 1.0f, 1.0f);
        TekeningRef ref = ref();
        if (ref != null) {
            spawnAtLocation(level, Tekeningen.maakItem(ref.id(), ref.maker()));
        }
    }

    @Override
    public void playPlacementSound() {
        playSound(SoundEvents.PAINTING_PLACE, 1.0f, 1.0f);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("facing", Direction.LEGACY_ID_CODEC_2D, getDirection());
        super.addAdditionalSaveData(output);
        TekeningRef ref = ref();
        if (ref != null) {
            output.store("tekening", TekeningRef.CODEC, ref);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        Direction d = input.read("facing", Direction.LEGACY_ID_CODEC_2D).orElse(Direction.SOUTH);
        super.readAdditionalSaveData(input);
        input.read("tekening", TekeningRef.CODEC).ifPresent(this::zetRef);
        setDirection(d);
    }

    @Override
    public void snapTo(double x, double y, double z, float yRot, float xRot) {
        setPos(x, y, z);
    }

    @Override
    public Vec3 trackingPosition() {
        return Vec3.atLowerCornerOf(pos);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, getDirection().get3DDataValue(), getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        TekeningRef ref = ref();
        return ref == null ? new ItemStack(TekenenFeature.TEKENING.get()) : Tekeningen.maakItem(ref.id(), ref.maker());
    }
}
