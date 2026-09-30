package nl.juiced.guhs.feature.landdiertjes;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.piep.PiepInstelling;
import nl.juiced.guhs.feature.piep.Schouder;
import nl.juiced.guhs.registry.ModItems;

/**
 * The pluiseekhoorntje: a little squirrel with an enormous fluffy curled tail, tufted round ears and a guh face. Lives in the
 * guhbloesembomen and the Vadswoud; it can scramble up tree trunks.
 * <ul>
 *   <li>Voorraadjes: a wild one stuffs kaasknabbels it finds (and the ones you drop) into its cheeks and buries them in a
 *       {@link KnabbelvoorraadjeBlock knabbelvoorraadje} nearby; find one and dig it up! A tamed one that follows you now and
 *       then digs up a voorraadje right next to you ("Kijk wat ik gevonden heb!").</li>
 *   <li>Tame it with kaasknabbels, 1 in {@link #TAME_CHANCE}.</li>
 *   <li>It sits on your shoulder ({@link Schouder}): its big button "Op mijn schouder!", or use its item in the air. Sneak +
 *       right-click a block with an empty hand and it hops down.</li>
 * </ul>
 */
public class PluiseekhoorntjeEntity extends Landdiertje {
    public static final int TAME_CHANCE = 3;
    /** Knabbels it can carry in its cheeks. */
    public static final int WANGEN_MAX = 4;
    /** A wild one finds a few knabbels by itself about every this many ticks (5-10 minutes); a tamed one digs one up for you. */
    public static final int VONDST_MIN = 6000, VONDST_WILLEKEURIG = 6000, CADEAU_MIN = 9600, CADEAU_WILLEKEURIG = 4800;

    private int wangen;
    private long volgendeVondst = -1;

    public PluiseekhoorntjeEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public String soort() {
        return "pluiseekhoorntje";
    }

    @Override
    public Item oppakItem() {
        return LanddiertjesFeature.PLUISEEKHOORNTJE_ITEM.get();
    }

    @Override
    public boolean isVoer(ItemStack stack) {
        return stack.is(ModItems.KAAS_KNABBELS.get());
    }

    @Override
    public int temKans() {
        return TAME_CHANCE;
    }

    @Override
    protected SoundEvent geluid() {
        return LanddiertjesFeature.EEKHOORNTJE.get();
    }

    @Override
    protected double volgSnelheid() {
        return 1.3;
    }

    @Override
    public boolean kanOpSchouder() {
        return true;
    }

    @Override
    public float schouderSchaal() {
        return 0.85f;
    }

    public int wangen() {
        return wangen;
    }

    @Override
    protected void eigenDoelen() {
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 5.0f, 1.1, 1.45,
                e -> !isTame() && e instanceof Player p && !p.isCrouching() && !p.isSpectator() && !p.isCreative()
                        && !isVoer(p.getMainHandItem()) && !isVoer(p.getOffhandItem())));
        goalSelector.addGoal(3, new VerstopGoal());
    }

    /** A wild one keeps what it finds on the ground for later (a tamed one just eats it). */
    @Override
    protected void gevonden(ItemStack hap) {
        if (!isTame() && hap.is(ModItems.KAAS_KNABBELS.get()) && wangen < WANGEN_MAX) {
            wangen++;
        }
    }

    /** Scrambles up tree trunks it bumps into. */
    @Override
    public boolean onClimbable() {
        if (horizontalCollision && !isOrderedToSit()) {
            BlockPos p = blockPosition();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (level().getBlockState(p.relative(d)).is(BlockTags.LOGS)) {
                    return true;
                }
            }
        }
        return super.onClimbable();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && volgendeVondst < 0) {
            volgendeVondst = level().getGameTime() + (isTame() ? CADEAU_MIN : VONDST_MIN / 2) + random.nextInt(VONDST_WILLEKEURIG);
        }
    }

    // --- the big button: onto your shoulder -------------------------------------------------------------------------------------

    @Override
    public void speciaal(ServerPlayer player) {
        opSchouder(player, this);
    }

    /** Onto this player's shoulder (if there is room), with its own message. */
    static boolean opSchouder(ServerPlayer player, PluiseekhoorntjeEntity eekhoorn) {
        if (Schouder.heeft(player)) {
            player.displayClientMessage(Component.translatable("gui.guhs.landdiertjes.schouder_vol").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        Component naam = eekhoorn.getDisplayName();
        Schouder.zet(player, eekhoorn);
        player.displayClientMessage(Component.translatable("gui.guhs.landdiertjes.op_schouder", naam).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        GidsFeature.grant(player, "diertjes/landdiertjes_schouder");
        return true;
    }

    // --- voorraadjes -------------------------------------------------------------------------------------------------------------------

    /**
     * Burying (wild: what is in its cheeks, or a few knabbels it found) or digging up a present next to its owner (tamed):
     * trot to a spot, dig a little while (animation "graaf", crumbs of earth), and there is the stash.
     */
    class VerstopGoal extends Goal {
        @Nullable
        private BlockPos plek;
        private int ticks, graaf;
        private int knabbels;
        @Nullable
        private ServerPlayer cadeauVoor;
        private long volgendeCheck;

        VerstopGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!(level() instanceof ServerLevel sl) || isOrderedToSit() || isBezig() || sl.getGameTime() < volgendeCheck) {
                return false;
            }
            long nu = sl.getGameTime();
            volgendeCheck = nu + 20;
            cadeauVoor = null;
            if (isTame()) {
                if (volgendeVondst < 0 || nu < volgendeVondst || !aan(PiepInstelling.VOLGEN) || !(getOwner() instanceof ServerPlayer owner)
                        || owner.distanceToSqr(PluiseekhoorntjeEntity.this) > 10 * 10 || owner.level() != sl) {
                    return false;
                }
                cadeauVoor = owner;
                knabbels = 2 + random.nextInt(3);
                plek = zoekPlek(sl, owner.blockPosition(), 2);
            } else {
                boolean vondst = volgendeVondst >= 0 && nu >= volgendeVondst;
                if (wangen <= 0 && !vondst) {
                    return false;
                }
                knabbels = wangen + (vondst ? 1 + random.nextInt(3) : 0);
                plek = zoekPlek(sl, blockPosition(), 5);
            }
            return plek != null;
        }

        @Override
        public boolean canContinueToUse() {
            return plek != null && ticks < 300 && !isBezig() && !isOrderedToSit();
        }

        @Override
        public void start() {
            ticks = 0;
            graaf = 0;
            getNavigation().moveTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 1.15);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            ServerLevel sl = (ServerLevel) level();
            double d = distanceToSqr(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5);
            if (d > 1.6 * 1.6) {
                if (ticks % 20 == 0) {
                    getNavigation().moveTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 1.15);
                }
                return;
            }
            getNavigation().stop();
            getLookControl().setLookAt(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5);
            if (graaf == 0) {
                triggerAnim("actie", "graaf");
            }
            if (++graaf % 8 == 0) {
                BlockState grond = sl.getBlockState(plek.below());
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, grond), plek.getX() + 0.5, plek.getY() + 0.1, plek.getZ() + 0.5,
                        4, 0.2, 0.05, 0.2, 0.05);
            }
            if (graaf >= 36) {
                if (KnabbelvoorraadjeBlock.verstop(sl, plek, knabbels)) {
                    playSound(geluid(), 0.7f, 1.4f);
                    if (cadeauVoor != null) {
                        cadeauVoor.displayClientMessage(Component.translatable("gui.guhs.landdiertjes.voorraadje_cadeau", getDisplayName())
                                .withStyle(ChatFormatting.GOLD), true);
                        sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.6, getZ(), 3, 0.2, 0.2, 0.2, 0);
                    }
                    wangen = 0;
                }
                long nu = sl.getGameTime();
                volgendeVondst = nu + (isTame() ? CADEAU_MIN + random.nextInt(CADEAU_WILLEKEURIG) : VONDST_MIN + random.nextInt(VONDST_WILLEKEURIG));
                plek = null;
            }
        }

        @Override
        public void stop() {
            plek = null;
            getNavigation().stop();
        }
    }

    /**
     * A free spot for a stash near {@code bij} (grass/air on sturdy ground), or an existing stash with room there. At most two
     * stashes within 12 blocks: then it adds to the nearest one.
     */
    @Nullable
    static BlockPos zoekPlek(ServerLevel level, BlockPos bij, int r) {
        BlockPos bestaand = null;
        int aantal = 0;
        for (BlockPos p : BlockPos.betweenClosed(bij.offset(-12, -3, -12), bij.offset(12, 3, 12))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof KnabbelvoorraadjeBlock) {
                aantal++;
                if (s.getValue(KnabbelvoorraadjeBlock.KNABBELS) < KnabbelvoorraadjeBlock.MAX
                        && (bestaand == null || p.distSqr(bij) < bestaand.distSqr(bij))) {
                    bestaand = p.immutable();
                }
            }
        }
        if (aantal >= 2) {
            return bestaand;
        }
        net.minecraft.util.RandomSource rng = level.getRandom();
        for (int i = 0; i < 24; i++) {
            BlockPos p = bij.offset(rng.nextInt(2 * r + 1) - r, rng.nextInt(3) - 1, rng.nextInt(2 * r + 1) - r);
            if (!p.equals(bij) && KnabbelvoorraadjeBlock.past(level, p) && level.getBlockState(p.above()).isAir()) {
                return p;
            }
        }
        return bestaand;
    }

    // --- save / animations ------------------------------------------------------------------------------------------------------------

    @Override
    protected void extraActies(software.bernie.geckolib.animation.AnimationController<Landdiertje> actie) {
        actie.triggerableAnim("graaf", software.bernie.geckolib.animation.RawAnimation.begin().thenPlay("graaf"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Wangen", wangen);
        tag.putLong("VondstOver", volgendeVondst < 0 ? -1 : Math.max(0, volgendeVondst - level().getGameTime()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        wangen = Math.min(WANGEN_MAX, tag.getInt("Wangen"));
        long over = tag.contains("VondstOver") ? tag.getLong("VondstOver") : -1;
        volgendeVondst = over < 0 ? -1 : level().getGameTime() + over;
    }

    @Override
    public void temmen(ServerPlayer player) {
        super.temmen(player);
        volgendeVondst = level().getGameTime() + CADEAU_MIN / 2 + random.nextInt(CADEAU_WILLEKEURIG);
        if (wangen > 0) {                                        // what was in its cheeks: for you!
            spawnAtLocation(new ItemStack(ModItems.KAAS_KNABBELS.get(), wangen));
            wangen = 0;
        }
    }

    /** (Tests / commands) its next find or present is due now. */
    void vondstNu() {
        volgendeVondst = level().getGameTime();
    }
}
