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

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
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
                case VAHOEG -> List.of(new MobEffectInstance(MobEffects.SPEED, duration, levels >= 4 ? 1 : 0, true, true),
                        new MobEffectInstance(MobEffects.SATURATION, 1, 0, true, false));
                case GUHSPRONG -> List.of(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, levels >= 4 ? 1 : 0, true, true));
                case VADSSCHILD -> List.of(new MobEffectInstance(MobEffects.RESISTANCE, duration, 0, true, true));
                case KNABBELHERSTEL -> List.of(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, true, true));
            };
        }
    }

    private int levels;
    private Gunst gunst = Gunst.VAHOEG;
    /** 1.9.3: the beam can be hidden from the right-click menu (the effect keeps working). */
    private boolean straalUit;
    /** 1.9.3: which effects are switched on (bit = Gunst ordinal); only the ones the pyramid allows work. */
    private int actief = 1;
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

    /** Blocks of reach per pyramid layer count (1.9.3 buff: was 10 + 10 x layers = 20/30/40/50). */
    static final int[] BEREIK = {0, 25, 40, 60, 80};

    public int range() {
        return BEREIK[Math.max(0, Math.min(MAX_LEVELS, levels))];
    }

    public boolean straalUit() {
        return straalUit;
    }

    /** Menu / tests: hide or show the beam. */
    public void setStraalUit(boolean uit) {
        straalUit = uit;
        sync();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KnabbelbakenBlockEntity baken) {
        if (level.getGameTime() % PULSE != 0) {
            return;
        }
        int before = baken.levels;
        baken.levels = countLevels(level, pos);
        if (level.isClientSide()) {
            baken.beamHeight = beam(level, pos);
            return;
        }
        if (baken.gunst.levels > baken.levels || !baken.aan(baken.gunst)) {
            baken.gunst = baken.eersteAan();
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
            if (y < level.getMinY()) {
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
        int top = level.getMaxY() + 1;
        if (level.dimensionType().hasCeiling()) {
            top = Math.min(top, level.getMinY() + level.dimensionType().logicalHeight());
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
            for (Gunst g : Gunst.values()) {
                if (aan(g) && g.levels <= levels) {
                    for (MobEffectInstance effect : g.effects(levels)) {
                        e.addEffect(new MobEffectInstance(effect));
                    }
                }
            }
            if (e instanceof ServerPlayer p) {
                GuhAdvancements.grant(p, "knabbelbaken_aan");
                SpiesburchtStats.award(p, "barbecuether/knabbelbaken");
            }
        }
    }

    public boolean aan(Gunst g) {
        return (actief & (1 << g.ordinal())) != 0;
    }

    /** Menu: switch this effect on or off (only when the pyramid has unlocked it). The beam takes the colour of the last one switched on. */
    public boolean schakel(ServerPlayer player, Gunst g) {
        if (level == null) {
            return false;
        }
        levels = countLevels(level, worldPosition);
        if (levels == 0 || g.levels > levels) {
            return false;
        }
        actief ^= 1 << g.ordinal();
        boolean nuAan = aan(g);
        if (nuAan) {
            gunst = g;
        } else if (gunst == g) {
            gunst = eersteAan();
        }
        level.playSound(null, worldPosition, nuAan ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8f, 1.2f);
        player.sendOverlayMessage(Component.translatable(nuAan ? "quest.guhs.knabbelbaken.effect_aan" : "quest.guhs.knabbelbaken.effect_uit",
                Component.translatable("quest.guhs.knabbelbaken.gunst." + g.id()), levels, range()).withStyle(ChatFormatting.LIGHT_PURPLE));
        pulse();
        sync();
        return true;
    }

    /** The first effect that is on and unlocked (the beam's colour), else VAHOEG. */
    private Gunst eersteAan() {
        for (Gunst g : Gunst.values()) {
            if (aan(g) && g.levels <= Math.max(1, levels)) {
                return g;
            }
        }
        return Gunst.VAHOEG;
    }

    /** Right-click (before 1.9.3; tests): the next effect this pyramid allows. */
    public void cycle(ServerPlayer player) {
        if (level == null) {
            return;
        }
        levels = countLevels(level, worldPosition);
        if (levels == 0) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.knabbelbaken.geen_piramide").withStyle(ChatFormatting.GRAY));
            return;
        }
        Gunst[] all = Gunst.values();
        int i = gunst.ordinal();
        do {
            i = (i + 1) % all.length;
        } while (all[i].levels > levels);
        gunst = all[i];
        actief = 1 << gunst.ordinal();
        level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1f, 1.2f);
        player.sendOverlayMessage(Component.translatable("quest.guhs.knabbelbaken.gekozen",
                Component.translatable("quest.guhs.knabbelbaken.gunst." + gunst.id()), levels, range()).withStyle(ChatFormatting.LIGHT_PURPLE));
        pulse();
        sync();
    }

    /** Game tests / commands: pick an effect directly. */
    public void setGunst(Gunst gunst) {
        this.gunst = gunst;
        actief = 1 << gunst.ordinal();
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
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putString("Gunst", gunst.id());
        tag.putInt("Levels", levels);
        tag.putBoolean("StraalUit", straalUit);
        tag.putInt("Actief", actief);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        levels = tag.getIntOr("Levels", 0);
        straalUit = tag.getBooleanOr("StraalUit", false);
        for (Gunst g : Gunst.values()) {
            if (g.id().equals(tag.getStringOr("Gunst", ""))) {
                gunst = g;
            }
        }
        actief = tag.getIntOr("Actief", 1 << gunst.ordinal());   // (before 1.9.3: only the chosen one)
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
