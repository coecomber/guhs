package nl.juiced.guhs.feature.spiesburcht;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The Knabbelbaken's pyramid check (every 4 seconds, like a beacon) and the effect it gives to players and tamed guhs
 * in range. The beam is drawn by the client (feature/spiesburcht/client).
 */
public class KnabbelbakenBlockEntity extends BlockEntity {
    public static final int PULSE = 80;
    public static final int MAX_LEVELS = 4;

    /** The guh effects, and from how many pyramid layers on they can be picked. */
    public enum Gunst {
        /** VAHOEG: speed and a bit of a full belly. */
        VAHOEG(1, 0xF08CB4),
        /** Guhsprong: jump boost. */
        GUHSPRONG(2, 0x8CD2F0),
        /** Vadsschild: resistance. */
        VADSSCHILD(3, 0xF2C23C),
        /** Knabbelherstel: regeneration. */
        KNABBELHERSTEL(4, 0xE0503C);

        public final int levels;
        public final int colour;

        Gunst(int levels, int colour) {
            this.levels = levels;
            this.colour = colour;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public List<MobEffectInstance> effects(int levels) {
            int duration = PULSE + 9 * 20 + levels * 40;
            return switch (this) {
                case VAHOEG -> List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, levels >= 4 ? 1 : 0, true, true),
                        new MobEffectInstance(MobEffects.SATURATION, 1, 0, true, false));
                case GUHSPRONG -> List.of(new MobEffectInstance(MobEffects.JUMP, duration, levels >= 4 ? 1 : 0, true, true));
                case VADSSCHILD -> List.of(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, true, true));
                case KNABBELHERSTEL -> List.of(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, true, true));
            };
        }
    }

    private int levels;
    private Gunst gunst = Gunst.VAHOEG;
    /** Client: how high the beam goes (up to the first solid block). */
    int beamHeight;

    public KnabbelbakenBlockEntity(BlockPos pos, BlockState state) {
        super(SpiesburchtFeature.KNABBELBAKEN_BE.get(), pos, state);
    }

    public int levels() {
        return levels;
    }

    public Gunst gunst() {
        return gunst;
    }

    /** Client: the height of the beam (worked out again every pulse). */
    public int beamHeight() {
        if (beamHeight <= 0 && level != null) {
            beamHeight = beam(level, worldPosition);
        }
        return beamHeight;
    }

    public int range() {
        return 10 + levels * 10;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KnabbelbakenBlockEntity baken) {
        if (level.getGameTime() % PULSE != 0) {
            return;
        }
        int before = baken.levels;
        baken.levels = countLevels(level, pos);
        if (level.isClientSide) {
            baken.beamHeight = beam(level, pos);
            return;
        }
        if (baken.gunst.levels > baken.levels) {
            baken.gunst = Gunst.VAHOEG;
        }
        if (baken.levels > 0) {
            baken.pulse();
            if (before == 0) {
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1f, 1.3f);
            }
        } else if (before > 0) {
            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1f, 1.3f);
        }
        if (before != baken.levels) {
            baken.sync();
        }
    }

    /** Layers of pyramid under the baken (like a beacon: 3x3, 5x5, 7x7, 9x9). */
    public static int countLevels(Level level, BlockPos pos) {
        int found = 0;
        for (int layer = 1; layer <= MAX_LEVELS; layer++) {
            int y = pos.getY() - layer;
            if (y < level.getMinBuildHeight()) {
                break;
            }
            for (int x = pos.getX() - layer; x <= pos.getX() + layer; x++) {
                for (int z = pos.getZ() - layer; z <= pos.getZ() + layer; z++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).is(SpiesburchtFeature.BAKEN_BASIS)) {
                        return found;
                    }
                }
            }
            found = layer;
        }
        return found;
    }

    /**
     * Like a vanilla beacon: the beam goes up to the build height, through glass and other see-through blocks, and stops
     * at the first solid block. In a dimension with a ceiling (the Barbecuether) it never goes above the logical height,
     * so it stops at the roof instead of poking out on top of it.
     */
    static int beam(Level level, BlockPos pos) {
        int top = level.getMaxBuildHeight();
        if (level.dimensionType().hasCeiling()) {
            top = Math.min(top, level.getMinBuildHeight() + level.dimensionType().logicalHeight());
        }
        int h = 0;
        for (int y = pos.getY() + 1; y < top; y++) {
            var state = level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ()));
            if (state.canOcclude() && !state.is(net.minecraft.tags.BlockTags.IMPERMEABLE)) {
                break;
            }
            h++;
        }
        return Math.max(h, 0);
    }

    /** Give the effect to every player and every tamed guh within range. */
    public void pulse() {
        if (level == null || levels == 0) {
            return;
        }
        AABB box = new AABB(worldPosition).inflate(range()).expandTowards(0, level.getHeight(), 0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && (e instanceof Player || (e instanceof GuhEntity guh && guh.isTame())));
        for (LivingEntity e : targets) {
            for (MobEffectInstance effect : gunst.effects(levels)) {
                e.addEffect(new MobEffectInstance(effect));
            }
            if (e instanceof ServerPlayer p) {
                GuhAdvancements.grant(p, "knabbelbaken_aan");
                SpiesburchtStats.award(p, "barbecuether/knabbelbaken");
            }
        }
    }

    /** Right-click: the next effect this pyramid allows. */
    public void cycle(ServerPlayer player) {
        if (level == null) {
            return;
        }
        levels = countLevels(level, worldPosition);
        if (levels == 0) {
            player.displayClientMessage(Component.translatable("quest.guhs.knabbelbaken.geen_piramide").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Gunst[] all = Gunst.values();
        int i = gunst.ordinal();
        do {
            i = (i + 1) % all.length;
        } while (all[i].levels > levels);
        gunst = all[i];
        level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1f, 1.2f);
        player.displayClientMessage(Component.translatable("quest.guhs.knabbelbaken.gekozen",
                Component.translatable("quest.guhs.knabbelbaken.gunst." + gunst.id()), levels, range()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        pulse();
        sync();
    }

    /** Game tests / commands: pick an effect directly. */
    public void setGunst(Gunst gunst) {
        this.gunst = gunst;
        setChanged();
    }

    public void refresh() {
        if (level != null) {
            levels = countLevels(level, worldPosition);
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Gunst", gunst.id());
        tag.putInt("Levels", levels);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        levels = tag.getInt("Levels");
        for (Gunst g : Gunst.values()) {
            if (g.id().equals(tag.getString("Gunst"))) {
                gunst = g;
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

    /** The beam is as tall as the view (the renderer reaches far). */
    public AABB renderBox() {
        return new AABB(worldPosition).expandTowards(0, Math.max(1, beamHeight), 0);
    }
}
