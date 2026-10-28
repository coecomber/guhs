package nl.juiced.guhs.feature.sterrenveld;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;

/**
 * A falling little star over the Sterrenkussenveld (1.8.0, island "sterren"): it drifts down with a glitter trail
 * ({@link #VALT}), glows on a little while where it lands ({@link #GLOEIT}) and then fades away ({@link #VERVAAGT}).
 * Touch it or click it (also mid-air, bouncing on the pillows!) and you catch it: droomzand. Not part of the story: they
 * fall whenever somebody is near the island. Never saved (a new one falls soon enough).
 */
public class VallendeSter extends Entity implements ItemSupplier {
    public static final int VALT = 0, GLOEIT = 1, VERVAAGT = 2;
    /** How long it glows after landing, how long the fading takes, and the longest fall (then it is simply gone). */
    public static final int GLOEI_TICKS = 140, VERVAAG_TICKS = 60, MAX_VAL_TICKS = 900;
    private static final EntityDataAccessor<Integer> FASE = SynchedEntityData.defineId(VallendeSter.class, EntityDataSerializers.INT);

    /** Ticks in the current phase (server). */
    private int inFase;
    /** The sideways drift while falling (set when it is spawned). */
    private double driftX, driftZ;

    public VallendeSter(EntityType<? extends VallendeSter> type, Level level) {
        super(type, level);
    }

    /** A new star at this spot, drifting slightly to one side while it falls. */
    public static VallendeSter nieuw(ServerLevel level, Vec3 at) {
        VallendeSter s = new VallendeSter(SterrenveldFeature.VALLENDE_STER.get(), level);
        s.setPos(at);
        double a = level.getRandom().nextDouble() * Math.PI * 2;
        s.driftX = Math.cos(a) * 0.035;
        s.driftZ = Math.sin(a) * 0.035;
        level.addFreshEntity(s);
        return s;
    }

    public int fase() {
        return entityData.get(FASE);
    }

    void zetFase(int fase) {
        entityData.set(FASE, fase);
        inFase = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FASE, VALT);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(switch (fase()) {
            case GLOEIT -> SterrenveldFeature.LOOK_GLOED.get();
            case VERVAAGT -> SterrenveldFeature.LOOK_VAAG.get();
            default -> SterrenveldFeature.LOOK_VALT.get();
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        inFase++;
        switch (fase()) {
            case VALT -> {
                Vec3 volgende = position().add(driftX, -0.11, driftZ);
                BlockPos onder = BlockPos.containing(volgende.x, volgende.y - 0.05, volgende.z);
                if (!level.getBlockState(onder).getCollisionShape(level, onder).isEmpty()) {
                    setPos(volgende.x, onder.getY() + level.getBlockState(onder).getCollisionShape(level, onder).max(net.minecraft.core.Direction.Axis.Y), volgende.z);
                    zetFase(GLOEIT);
                    level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 8, 0.25, 0.1, 0.25, 0.04);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.6f, 1.6f);
                } else {
                    setPos(volgende);
                    if (inFase % 2 == 0) {                              // the glitter trail
                        level.sendParticles(ParticleTypes.END_ROD, getX() - driftX * 4, getY() + 0.5, getZ() - driftZ * 4, 1, 0.05, 0.05, 0.05, 0.0);
                        level.sendParticles(ParticleTypes.WAX_OFF, getX(), getY() + 0.3, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
                    }
                    if (inFase > MAX_VAL_TICKS || getY() < level.getMinY()) {
                        discard();
                    }
                }
            }
            case GLOEIT -> {
                if (inFase % 10 == 0) {
                    level.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.3, getZ(), 2, 0.3, 0.2, 0.3, 0.0);
                }
                if (inFase >= GLOEI_TICKS) {
                    zetFase(VERVAAGT);
                }
            }
            default -> {
                if (inFase % 8 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 1, 0.2, 0.1, 0.2, 0.02);
                }
                if (inFase >= VERVAAG_TICKS) {
                    level.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 0.3, getZ(), 6, 0.2, 0.2, 0.2, 0.01);
                    discard();
                }
            }
        }
    }

    /** Caught: droomzand for the catcher (two now and then), a chime and a shower of sparkles. */
    public boolean vang(ServerPlayer p) {
        if (isRemoved() || !(level() instanceof ServerLevel level)) {
            return false;
        }
        ItemStack zand = new ItemStack(DroomblokkenFeature.DROOMZAND.get(), level.getRandom().nextInt(4) == 0 ? 2 : 1);
        if (!p.getInventory().add(zand)) {
            p.drop(zand, false);
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.9f);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 14, 0.3, 0.3, 0.3, 0.08);
        p.sendOverlayMessage(Component.translatable("gui.guhs.sterren.gevangen").withStyle(ChatFormatting.YELLOW));
        discard();
        return true;
    }

    @Override
    public void playerTouch(Player player) {
        if (player instanceof ServerPlayer sp && !sp.isSpectator()) {
            vang(sp);
        }
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player instanceof ServerPlayer sp) {
            vang(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof ServerPlayer sp) {
            vang(sp);
        }
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
