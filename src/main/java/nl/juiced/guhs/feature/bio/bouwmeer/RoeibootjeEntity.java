package nl.juiced.guhs.feature.bio.bouwmeer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Het roeibootje van het botenhuisje: an ordinary rowing boat (vanilla's boat in every way: two seats, paddles, the same
 * handling) that belongs to a mooring ({@link MeerpaalBlock}) and to its lake.
 * <ul>
 *   <li>It cannot be broken, picked up or leashed and drops nothing, so it is never an item and never a boat farm.</li>
 *   <li>Only players get in (no guh, koi or frog rides off with it).</li>
 *   <li>Left empty away from its mooring for {@link #LEEG_WEG} ticks it is gone (a little splash): the mooring lays a
 *       fresh one ready as soon as its berth is empty, so every player always finds a boat and the lake never fills up
 *       with them. An empty boat in its own berth stays.</li>
 *   <li>It stays at its lake: more than {@link #BEREIK} blocks from its mooring it lets its rowers out and is gone.</li>
 * </ul>
 * A boat without a mooring (summoned by hand) follows the same rules around the spot where it first floated.
 */
public class RoeibootjeEntity extends Boat {
    /** An empty boat this far (blocks) from its berth is "away". */
    public static final double THUIS_STRAAL = 3.5;
    /** Ticks an away boat may lie empty before it is gone (one minute). */
    public static final int LEEG_WEG = 1200;
    /** How far from its mooring the boat goes (blocks, flat). */
    public static final double BEREIK = 256;
    /** The rules are looked at this often (ticks). */
    private static final int STAP = 20;

    @Nullable
    private BlockPos thuis;
    private int leeg;

    public RoeibootjeEntity(EntityType<? extends RoeibootjeEntity> type, Level level) {
        super(type, level, () -> Items.AIR);
    }

    /** The berth: the water block the boat lies in at its mooring. */
    @Nullable
    public BlockPos thuis() {
        return thuis;
    }

    public void zetThuis(BlockPos thuis) {
        this.thuis = thuis.immutable();
    }

    /** How long (ticks) the boat has been empty away from its berth. */
    public int leeg() {
        return leeg;
    }

    /** (Tests) as if the boat lay empty this long already. */
    public void zetLeeg(int ticks) {
        this.leeg = ticks;
    }

    /** Is the boat in (or right beside) its berth? */
    public boolean isThuis() {
        return thuis != null && afstandThuis() <= THUIS_STRAAL;
    }

    private double afstandThuis() {
        double dx = getX() - (thuis.getX() + 0.5), dz = getZ() - (thuis.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % STAP == 0) {
            regels((ServerLevel) level(), STAP);
        }
    }

    /** The rules above, {@code ticks} later. Returns false when the boat is gone now. */
    public boolean regels(ServerLevel level, int ticks) {
        if (thuis == null) {
            thuis = blockPosition();
        }
        boolean roeier = getPassengers().stream().anyMatch(p -> p instanceof Player);
        if (afstandThuis() > BEREIK) {
            for (Entity p : getPassengers()) {
                if (p instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.translatable("gui.guhs.roeibootje.te_ver").withStyle(ChatFormatting.AQUA));
                }
            }
            ejectPassengers();
            verdwijn(level);
            return false;
        }
        if (roeier || isThuis()) {
            leeg = 0;
            return true;
        }
        leeg += ticks;
        if (leeg >= LEEG_WEG) {
            verdwijn(level);
            return false;
        }
        return true;
    }

    /** Gone, with a little splash (the mooring has or gets another one). */
    public void verdwijn(ServerLevel level) {
        level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.3, getZ(), 24, 0.6, 0.1, 0.6, 0.05);
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.4, getZ(), 8, 0.5, 0.15, 0.5, 0.01);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.NEUTRAL, 0.8f, 0.7f);
        discard();
    }

    // --- not a thing to take or break --------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
            discard();          // (a builder in creative clears it away; the mooring lays a new one ready)
            return true;
        }
        return false;
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && super.canAddPassenger(passenger);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult r = super.interact(player, hand, location);
        if (player instanceof ServerPlayer sp && sp.getVehicle() == this) {
            GuhAdvancements.grant(sp, "roeibootje_gevaren");
            Visserguh.inBootje(sp);
        }
        return r;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (thuis != null) {
            output.putLong("Thuis", thuis.asLong());
        }
        output.putInt("Leeg", leeg);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        long t = input.getLongOr("Thuis", Long.MIN_VALUE);
        thuis = t == Long.MIN_VALUE ? null : BlockPos.of(t);
        leeg = input.getIntOr("Leeg", 0);
    }
}
