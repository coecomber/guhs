package nl.juiced.guhs.feature.wereldleven;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Seizoen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * IJscoguh Tingeling (2.8, wereldleven): a guh on his ice-cream bike (a box full of kaasijsjes, a parasol, a bell). Like
 * a wandering trader he turns up near players in the Guhmensie (IJscoguhSpawner), also on the Knuffeldal plein, rings
 * his bell ("Tingeling!"), sells kaasijsjes for kaasknabbels (the seasonal flavour only in its own season) and rides
 * away after a day. Guhs close by run after his cart ({@link Volgen}). Always friendly: nothing can hurt him.
 */
public class IJscoguhEntity extends PathfinderMob implements GeoEntity, Merchant {
    /** He rides away after this long (one in-game day). */
    public static final int BLIJFT = 24000;
    /** Guhs run after the cart from this far. */
    public static final double VOLG_AFSTAND = 14;
    /** Prices in kaasknabbels. */
    public static final int PRIJS_IJSJE = 3, PRIJS_SEIZOEN = 5, PRIJS_MARSHMALLOWS = 2, PRIJS_FLUITJE = 6, PRIJS_PETJE = 12;

    private static final EntityDataAccessor<Integer> DATA_BEL = SynchedEntityData.defineId(IJscoguhEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ijscoguh.idle");
    /** The IJscoguhs that are around right now (for the guhs that run after them: cheaper than looking for entities). */
    static final Set<IJscoguhEntity> LEVEND = Collections.newSetFromMap(new WeakHashMap<>());

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int blijft = BLIJFT;
    @Nullable
    private BlockPos doel;
    private int belOver = 60;
    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;
    @Nullable
    private Seizoen offersSeizoen;

    public IJscoguhEntity(EntityType<? extends IJscoguhEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BEL, 0);
    }

    /** Client: counts up every time the bell rings (the renderer wiggles the bell). */
    public int belTeller() {
        return entityData.get(DATA_BEL);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Goal() {    // stands still while you pick an ice cream
            {
                setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                return tradingPlayer != null;
            }

            @Override
            public void tick() {
                getNavigation().stop();
                if (tradingPlayer != null) {
                    getLookControl().setLookAt(tradingPlayer, 30f, 30f);
                }
            }
        });
        goalSelector.addGoal(2, new NaarDoelGoal());
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return (doel == null || distanceToSqr(Vec3.atBottomCenterOf(doel)) < 16 * 16) && super.canUse();
            }
        });
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    /** Where he is heading (a player, the plein): he stays around it. */
    public void setDoel(@Nullable BlockPos doel) {
        this.doel = doel;
    }

    @Nullable
    public BlockPos doel() {
        return doel;
    }

    public int blijftNog() {
        return blijft;
    }

    public void setBlijft(int ticks) {
        this.blijft = ticks;
    }

    /** Rides to his goal when it is far away. */
    private class NaarDoelGoal extends Goal {
        NaarDoelGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return doel != null && distanceToSqr(Vec3.atBottomCenterOf(doel)) > 12 * 12;
        }

        @Override
        public void start() {
            if (doel != null) {
                getNavigation().moveTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, 0.9);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return doel != null && !getNavigation().isDone() && distanceToSqr(Vec3.atBottomCenterOf(doel)) > 6 * 6;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        LEVEND.add(this);
        if (tradingPlayer != null && (!tradingPlayer.isAlive() || !(tradingPlayer.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu)
                || tradingPlayer.distanceToSqr(this) > 64)) {
            tradingPlayer = null;
        }
        if (--belOver <= 0) {
            belOver = 160 + random.nextInt(200);
            bel();
        }
        if (--blijft <= 0) {
            wegrijden();
        }
    }

    /** "Tingeling!" */
    public void bel() {
        playSound(WereldlevenFeature.IJSCOBEL.get(), 1.2f, 1.0f + random.nextFloat() * 0.2f);
        entityData.set(DATA_BEL, entityData.get(DATA_BEL) + 1);
    }

    /** Rides off to the next village (a puff of pink, a last tingeling). */
    public void wegrijden() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 12, 0.6, 0.5, 0.6, 0.02);
            bel();
        }
        LEVEND.remove(this);
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        LEVEND.remove(this);
        super.remove(reason);
    }

    // --- nothing hurts him ------------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(serverLevel, source, amount);
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GUH_AMBIENT.get();
    }

    @Override
    public float getVoicePitch() {
        return 1.2f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    // --- talking and the shop ---------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(WereldlevenFeature.IJSCOGUH_SPAWN_EGG.get())) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            if (tradingPlayer != null && tradingPlayer != player) {
                GuhQuests.say(sp, this, "quest.guhs.shop.busy");
                return InteractionResult.CONSUME;
            }
            GuhQuests.say(sp, this, "quest.guhs.ijscoguh.hallo." + random.nextInt(4));
            ontmoet(sp);
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        }
        return InteractionResult.SUCCESS;
    }

    /** The first meeting counts (Knus tab, quest). */
    public static void ontmoet(ServerPlayer player) {
        KnusVoortgang.hoogste(player, WereldlevenVoortgang.IJSCOGUH, 1);
    }

    @Override
    public MerchantOffers getOffers() {
        Seizoen nu = Seizoen.huidig(level());
        if (offers == null || offersSeizoen != nu) {
            offers = aanbod(nu);
            offersSeizoen = nu;
        }
        return offers;
    }

    /** What he sells in a season: three ice creams, the seasonal one, marshmallows, the fluitje and his cap. */
    public static MerchantOffers aanbod(Seizoen seizoen) {
        MerchantOffers o = new MerchantOffers();
        for (Kaasijsjes.Smaak s : new Kaasijsjes.Smaak[] {Kaasijsjes.Smaak.ROZE, Kaasijsjes.Smaak.MINT, Kaasijsjes.Smaak.CHOCO}) {
            o.add(offer(PRIJS_IJSJE, new ItemStack(WereldlevenFeature.KAASIJSJES.get(s).get())));
        }
        o.add(offer(PRIJS_SEIZOEN, new ItemStack(WereldlevenFeature.KAASIJSJES.get(Kaasijsjes.Smaak.van(seizoen)).get())));
        o.add(offer(PRIJS_MARSHMALLOWS, new ItemStack(WereldlevenFeature.MARSHMALLOW_KNABBEL.get(), 3)));
        o.add(offer(PRIJS_FLUITJE, new ItemStack(WereldlevenFeature.GUH_FLUITJE.get())));
        o.add(offer(PRIJS_PETJE, new ItemStack(ModItems.clothingItem(GuhClothes.IJSCOPETJE))));
        return o;
    }

    private static MerchantOffer offer(int knabbels, ItemStack result) {
        return new MerchantOffer(new ItemCost(ModItems.KAAS_KNABBELS.get(), knabbels), result, Integer.MAX_VALUE, 0, 0);
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    /** 26.1: the merchant menu asks the merchant (1.21.1's MerchantMenu checked getTradingPlayer() == player). */
    @Override
    public boolean stillValid(Player player) {
        return tradingPlayer == player;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        playSound(ModSounds.GUH_HAPPY.get(), 1f, 1.3f);
        if (tradingPlayer instanceof ServerPlayer sp) {
            ItemStack result = offer.getResult();
            if (result.getItem() instanceof KaasijsjeItem) {
                sp.sendOverlayMessage(Component.translatable("quest.guhs.ijscoguh.alsjeblieft").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.GUH_AMBIENT.get();
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide();
    }

    // --- saving ---------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Blijft", blijft);
        if (doel != null) {
            tag.store("Doel", BlockPos.CODEC, doel);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        if (tag.keySet().contains("Blijft")) {
            blijft = tag.getIntOr("Blijft", 0);
        }
        doel = (tag).read("Doel", BlockPos.CODEC).orElse(null);
    }

    // --- GeckoLib -------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // --- guhs run after the cart --------------------------------------------------------------------------------------------------

    /** The nearest IJscoguh within range of a guh (from {@link #LEVEND}), or null. */
    @Nullable
    public static IJscoguhEntity dichtbij(GuhEntity guh, double range) {
        IJscoguhEntity best = null;
        double bestD = range * range;
        for (IJscoguhEntity ijsco : LEVEND) {
            if (ijsco.isAlive() && ijsco.level() == guh.level()) {
                double d = ijsco.distanceToSqr(guh);
                if (d < bestD) {
                    bestD = d;
                    best = ijsco;
                }
            }
        }
        return best;
    }

    /** A guh (wild, a resident or a free-roaming tamed one) runs after the ice-cream cart for a while, hopping happily. */
    public static class Volgen extends Goal {
        private final GuhEntity guh;
        private int cooldown;
        private int ticks;
        @Nullable
        private IJscoguhEntity ijsco;

        public Volgen(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean mag() {
            return guh.isAlive() && !guh.isOrderedToSit() && guh.mayWander() && !guh.isLeashed() && !guh.isPassenger() && !guh.isVehicle()
                    && guh.getTarget() == null && guh.getHiddenBy() == null && !guh.isNoAi() && !Dagritme.slaapt(guh);
        }

        @Override
        public boolean canUse() {
            if (LEVEND.isEmpty() || --cooldown > 0) {
                return false;
            }
            cooldown = 40 + guh.getRandom().nextInt(40);
            if (!mag() || GuhHooks.isBezig(guh) || !Dagritme.magInTest(guh)) {
                return false;
            }
            ijsco = dichtbij(guh, VOLG_AFSTAND);
            return ijsco != null && guh.getRandom().nextInt(2) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return ijsco != null && ijsco.isAlive() && ticks < 20 * 25 && mag() && ijsco.distanceToSqr(guh) < 20 * 20;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            ticks = 0;
            if (GuhHooks.isBewoner(guh)) {
                GuhHooks.bezig(guh, 20 * 30);
            }
        }

        @Override
        public void tick() {
            if (ijsco == null) {
                return;
            }
            ticks++;
            // a spot behind the cart, a little to the side (each guh its own)
            Vec3 back = Vec3.directionFromRotation(0, ijsco.yBodyRot).scale(-2.6);
            double side = ((guh.getId() * 37) % 5 - 2) * 0.7;
            Vec3 sideways = new Vec3(-back.z, 0, back.x).normalize().scale(side);
            Vec3 spot = ijsco.position().add(back).add(sideways);
            double d = guh.position().distanceToSqr(spot);
            guh.getLookControl().setLookAt(ijsco, 30f, 30f);
            if (d > 2.0) {
                if (ticks % 10 == 1) {
                    guh.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.3);
                }
            } else {
                guh.getNavigation().stop();
                if (ticks % 30 == 0 && guh.onGround()) {
                    guh.getJumpControl().jump();    // (a happy hop)
                }
                if (ticks % 100 == 50 && GuhEmotes.canStart(guh) && guh.emotes.current() == null && guh.getRandom().nextInt(3) == 0) {
                    guh.emotes.start(Emote.VAHOEG, false, GuhEmotes.Source.SELF);
                }
            }
        }

        @Override
        public void stop() {
            ijsco = null;
            cooldown = 20 * 60 + guh.getRandom().nextInt(20 * 60);
            guh.getNavigation().stop();
        }
    }
}
