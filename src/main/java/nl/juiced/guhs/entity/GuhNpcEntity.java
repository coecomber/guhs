package nl.juiced.guhs.entity;

import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.quest.GuhQuests;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The sitting guh characters of the quests (same model as the Hungry Guh): Moeder Vadsig (huge, in her shrine), the
 * Tandarts-guh (in the mouth), the Maagenzym-guh (in every stomach) and the Slee-guh (in the sled hut).
 * They never move, can't be hurt, and talk to you when you right-click them (see {@link GuhQuests}).
 * The Kermis-guh (at the guh kermis) trades instead: the kermis outfit for kermisbonnen. Verstopguhtje (on the roof of
 * the verstopguh house) runs hide-and-seek games ({@link nl.juiced.guhs.quest.VerstopGame}) and sells the detective outfit.
 */
public class GuhNpcEntity extends PathfinderMob implements GeoEntity, net.minecraft.world.item.trading.Merchant {
    public enum Kind {
        MOEDER_VADSIG(2.6f),
        TANDARTS(1.0f),
        MAAGENZYM(0.85f),
        SLEE_GUH(1.0f),
        KERMIS_GUH(1.0f),
        VERSTOPGUHTJE(1.0f),
        TIPGUH(0.8f),
        POORTWACHTER(1.3f),
        REISGUH(1.0f),
        // --- 2.4: the characters of the minigames and the rare structures (their roles: nl.juiced.guhs.feature.*) ---
        SHOWGUH(1.0f),
        RACEGUH(1.0f),
        MEPGUH(1.0f),
        DJGUH(1.0f),
        GOLFGUH(1.0f),
        SMULGUH(1.1f),
        VISGUH(1.0f),
        MIJNGUH(1.0f),
        BIBLIOTHECARIS(1.0f),
        // --- 2.5 ---
        ZEEMEERGUH(1.0f),
        // --- 2.7 ---
        BOSWACHTERGUH(1.0f),
        KNABBELPLUKKER(1.0f),
        GRILLGUH(1.0f),
        // --- 2.8 (Knuffeldal; the roles: KnuffeldalFeature and the phase-2 features, see Features.role) ---
        BURGEMEESTERGUH(1.1f),
        BAKKERGUH(1.0f),
        JUF_KNUFFEL(1.0f),
        THEEGUH(1.0f),
        KAPPERGUH(1.0f),
        BOERINNEGUH(1.0f),
        STERRENKIJKERGUH(1.0f),
        BALLONGUH(1.0f),
        OPA_GUH(0.95f),
        BADMEESTERGUH(1.0f),
        /** Cocotje, in her little house in the Knuffeldal town (knuffeldal feature): "WEET JIJ WAAR ZE ZIJN??????" */
        COCOTJE(0.9f),
        // --- 2.9 (De Grote Guhspelen; the roles: the 2.9 features, see Features.role) ---
        SJOELGUH(1.0f),
        DOOLHOFGUH(1.0f),
        KATAPULTGUH(1.0f),
        SPELLEIDERGUH(1.0f),
        SCHAATSMEESTERGUH(1.0f),
        STEMPELGUH(1.0f),
        CIRCUITGUH(1.0f),
        BRANDWEERGUH(1.0f),
        POLITIEGUH(1.0f),
        APOTHEKERGUH(1.0f),
        BOUWVAKKERGUH(1.0f),
        // --- 3.0 (Guhverhalen; the roles: nl.juiced.guhs.feature.verhaal.NpcRollen, set by the owner slices) ---
        TIMMERGUH(1.0f), BORIS(0.9f), STEELE_MIKA(1.0f), MUK(1.3f), LUK(1.3f), ROSY(0.55f), WITTE_WOLFGUH(1.25f),
        KNABBELKLOON(1.0f), WOLKENHOEDER(1.05f), LILO_GUH(0.8f), NANI_GUH(1.0f), TIKIGUH(1.0f),
        // <timmerguh>
        // </timmerguh>
        // <balto>
        // </balto>
        // <mewtwo>
        // </mewtwo>
        // <hemel>
        // </hemel>
        // <guhwaii>
        // </guhwaii>
        // <guhwaiispellen>
        // </guhwaiispellen>
        ;

        public final float scale;

        Kind(float scale) {
            this.scale = scale;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(GuhNpcEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh_sitting.idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    /** The Maagenzym-guh belongs to one stomach. */
    @Nullable
    private UUID maagOwner;
    /** Saved state for the character's feature role (see nl.juiced.guhs.feature.NpcRole). */
    public net.minecraft.nbt.CompoundTag roleData = new net.minecraft.nbt.CompoundTag();

    /** A Reisguh's own name (the waypoint's name), empty = just "Reisguh". */
    private String reisName = "";

    public String getReisName() {
        return reisName;
    }

    public void setReisName(String name) {
        this.reisName = name;
        if (getKind() == Kind.REISGUH && !name.isEmpty()) {
            this.setCustomName(Component.literal(name));
        }
    }

    /** Verstopguhtje's game of verstopguh. */
    public final nl.juiced.guhs.quest.VerstopGame verstop = new nl.juiced.guhs.quest.VerstopGame();

    public GuhNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, Kind.TANDARTS.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public Kind getKind() {
        int i = this.entityData.get(DATA_KIND);
        return i >= 0 && i < Kind.values().length ? Kind.values()[i] : Kind.TANDARTS;
    }

    public void setKind(Kind kind) {
        this.entityData.set(DATA_KIND, kind.ordinal());
        this.getAttribute(Attributes.SCALE).setBaseValue(kind.scale);
        this.refreshDimensions();
        this.setCustomName(Component.translatable("entity.guhs.guh_npc." + kind.id()));
    }

    @Nullable
    public UUID getMaagOwner() {
        return maagOwner;
    }

    public void setMaagOwner(@Nullable UUID owner) {
        this.maagOwner = owner;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            nl.juiced.guhs.feature.NpcRole role = nl.juiced.guhs.feature.Features.role(getKind());
            if (role != null) {
                role.talk(this, serverPlayer);
            } else if (getKind() == Kind.REISGUH && player.isSecondaryUseActive()) {
                nl.juiced.guhs.quest.Reisguh.pickUp(this, serverPlayer);
            } else if (getKind() == Kind.REISGUH) {
                nl.juiced.guhs.quest.Reisguh.talk(this, serverPlayer);
            } else if (getKind() == Kind.POORTWACHTER) {
                nl.juiced.guhs.quest.KasteelPoort.talk(this, serverPlayer);
            } else if (getKind() == Kind.TIPGUH) {
                nl.juiced.guhs.quest.VerstopGame.askTip(this, serverPlayer);
            } else if (getKind() == Kind.VERSTOPGUHTJE) {
                nl.juiced.guhs.quest.VerstopGame.talk(this, serverPlayer);
            } else if (getKind() == Kind.KERMIS_GUH) {
                if (tradingPlayer == null) {
                    GuhQuests.say(serverPlayer, this, "quest.guhs.kermis.hello");
                }
                openShop(player);
            } else {
                GuhQuests.talkTo(this, serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kind", getKind().id());
        if (maagOwner != null) {
            tag.putUUID("MaagOwner", maagOwner);
        }
        if (getKind() == Kind.VERSTOPGUHTJE) {
            tag.put("Verstop", verstop.save());
        }
        if (!reisName.isEmpty()) {
            tag.putString("ReisName", reisName);
        }
        if (!roleData.isEmpty()) {
            tag.put("RoleData", roleData);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        for (Kind kind : Kind.values()) {
            if (kind.id().equals(tag.getString("Kind"))) {
                setKind(kind);
            }
        }
        maagOwner = tag.hasUUID("MaagOwner") ? tag.getUUID("MaagOwner") : null;
        verstop.load(tag.getCompound("Verstop"));
        setReisName(tag.getString("ReisName"));
        roleData = tag.getCompound("RoleData");
        if (getKind() == Kind.POORTWACHTER && !roleData.contains("GateYaw")) {
            roleData.putFloat("GateYaw", getYRot()); // (the Rotation from a structure is already loaded here)
        }
    }

    // --- a quest character: can't be hurt, pushed or leashed, never despawns ---

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // --- the Kermis-guh's stall -------------------------------------------------------------------------------------

    @Nullable
    private Player tradingPlayer;
    @Nullable
    private net.minecraft.world.item.trading.MerchantOffers offers;

    /** The kermis outfit, for kermisbonnen (one per lap of the coaster); never sold out. */
    @Override
    public net.minecraft.world.item.trading.MerchantOffers getOffers() {
        nl.juiced.guhs.feature.NpcRole role = nl.juiced.guhs.feature.Features.role(getKind());
        if (offers == null && role != null) {
            offers = role.offers(this);
        }
        if (offers == null && getKind() == Kind.VERSTOPGUHTJE) {
            offers = new net.minecraft.world.item.trading.MerchantOffers();
            offers.add(offer(nl.juiced.guhs.registry.ModItems.VERSTOPGUHTICKET.get(), 2, GuhClothes.DETECTIVE_VERGROOTGLAS));
            offers.add(offer(nl.juiced.guhs.registry.ModItems.VERSTOPGUHTICKET.get(), 3, GuhClothes.DETECTIVE_PET));
            offers.add(offer(nl.juiced.guhs.registry.ModItems.VERSTOPGUHTICKET.get(), 5, GuhClothes.DETECTIVE_JAS));
        }
        if (offers == null) {
            offers = new net.minecraft.world.item.trading.MerchantOffers();
            offers.add(offer(2, GuhClothes.KERMIS_STRIK));
            offers.add(offer(4, GuhClothes.KERMIS_HOED));
            offers.add(offer(6, GuhClothes.KERMIS_JASJE));
            offers.add(new net.minecraft.world.item.trading.MerchantOffer(
                    new net.minecraft.world.item.trading.ItemCost(nl.juiced.guhs.registry.ModItems.KERMISBON.get(), 1),
                    new net.minecraft.world.item.ItemStack(nl.juiced.guhs.registry.ModItems.GUH_BALLON.get(), 2), Integer.MAX_VALUE, 0, 0));
        }
        return offers;
    }

    private static net.minecraft.world.item.trading.MerchantOffer offer(net.minecraft.world.item.Item currency, int price, GuhClothes clothes) {
        return new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(currency, price),
                new net.minecraft.world.item.ItemStack(nl.juiced.guhs.registry.ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
    }

    /** Opens the stall (Kermis-guh, Verstopguhtje). */
    /** Opens the shop for this player; while someone else is still shopping, the character says so. */
    public void openShop(Player player) {
        if (tradingPlayer == null || tradingPlayer == player) {
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        } else if (player instanceof ServerPlayer serverPlayer) {
            GuhQuests.say(serverPlayer, this, "quest.guhs.shop.busy");
        }
    }

    private static net.minecraft.world.item.trading.MerchantOffer offer(int bonnen, GuhClothes clothes) {
        return new net.minecraft.world.item.trading.MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(nl.juiced.guhs.registry.ModItems.KERMISBON.get(), bonnen),
                new net.minecraft.world.item.ItemStack(nl.juiced.guhs.registry.ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
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

    @Override
    public void overrideOffers(net.minecraft.world.item.trading.MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(net.minecraft.world.item.trading.MerchantOffer offer) {
        offer.increaseUses();
        this.playSound(nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), 1f, 1.3f);
    }

    @Override
    public void notifyTradeUpdated(net.minecraft.world.item.ItemStack stack) {
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
    public net.minecraft.sounds.SoundEvent getNotifyTradeSound() {
        return nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get();
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    @Override
    public void tick() {
        if (getKind() == Kind.POORTWACHTER && !roleData.contains("GateYaw")) {
            // which way the gate faces: as placed, before the guard turns its head to look at people
            roleData.putFloat("GateYaw", getYRot());
        }
        super.tick();
        if (!this.level().isClientSide && getKind() == Kind.VERSTOPGUHTJE) {
            verstop.tick(this);
        }
        if (!this.level().isClientSide && getKind() == Kind.REISGUH) {
            nl.juiced.guhs.quest.Reisguh.tick(this);
        }
        if (!this.level().isClientSide) {
            nl.juiced.guhs.feature.NpcRole role = nl.juiced.guhs.feature.Features.role(getKind());
            if (role != null) {
                role.tick(this);
            }
        }
        if (!this.level().isClientSide && getKind() == Kind.POORTWACHTER) {
            nl.juiced.guhs.quest.KasteelPoort.tick(this);
        }
        if (tradingPlayer != null && (!tradingPlayer.isAlive() || !(tradingPlayer.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu)
                || tradingPlayer.distanceToSqr(this) > 64)) {
            tradingPlayer = null;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
