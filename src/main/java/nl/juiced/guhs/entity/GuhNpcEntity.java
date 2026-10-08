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
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
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
        // guhpixel: one block per slice; a slice adds its kinds ONLY between its own two markers (names N_..., one per line, each ending in a comma)
        // <px_lobby>
        LOBBY_WELKOMSTGUH(1.0f),
        LOBBY_VERKOPER_GUH(1.0f),
        LOBBY_CHATGUH(0.8f),
        INTERNETCAFE_BEHEERDER(1.0f),
        INTERNETCAFE_SLAPER(0.85f),
        // </px_lobby>
        // <px_grap1>
        /** Guhpixel: the Skyblok-guh (a deadly serious pro with a headset) at the lobby anchor SPEL_SKYBLOK. */
        SKYBLOK_GUH(1.0f),
        /** Guhpixel: the Bedwars-guh (armour made of pillows) at the lobby anchor SPEL_BEDWARS. */
        BEDWARS_GUH(1.0f),
        /** Guhpixel: the Vadsnite-guh (a parachute backpack) at the lobby anchor SPEL_VADSNITE. */
        VADSNITE_GUH(1.0f),
        // </px_grap1>
        // <px_grap2>
        GUHMON_GYMLEIDER(1.05f),
        BZG_PRESENTATRICE(1.0f),
        BZG_BOER(1.1f),
        // </px_grap2>
        // <px_among>
        /** Among Guhs: the Kapitein-guh of De Vadsvaarder (the queue of the real game). */
        AMONG_KAPITEIN(1.0f),
        /** Among Guhs: the Logboek-guh beside the Kapitein: your personal numbers. */
        AMONG_LOGBOEKGUH(1.0f),
        // </px_among>
        // <px_guhkade>
        // </px_guhkade>
        // <px_kantoor>
        // </px_kantoor>
        // <px_bioscoop>
        // </px_bioscoop>
        // <px_reisbureau>
        REISBUREAU_AGENT(1.0f),
        // </px_reisbureau>
        // <px_parkour>
        // </px_parkour>
        // biomes3: one block per slice; a slice adds its kinds only between its own two lines
        // <bio_bouw_dal>
        // </bio_bouw_dal>
        // <bio_bouw_meer>
        /** biomes3: the visser-guh on the jetty of a botenhuisje (koivoer, the roeibootje, the lessons of the lake). */
        BOTENHUISJE_VISSERGUH(1.0f),
        /** biomes3: the hanami guhs of a picknickeilandje: a plain guh and a bloesemguh, awake and asleep. */
        HANAMI_GUH(0.85f),
        HANAMI_GUH_SLAAPT(0.85f),
        HANAMI_BLOESEMGUH(0.9f),
        HANAMI_BLOESEMGUH_SLAAPT(0.9f),
        // </bio_bouw_meer>
        // <bio_bouw_wolk1>
        // </bio_bouw_wolk1>
        // <bio_bouw_wolk2>
        // </bio_bouw_wolk2>
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

    /** A Reisguh's own name (the waypoint's name), empty = just "Reisguh". 1.2.0: a Component (a place name of a template
     *  or an automatic name is translatable, a player's own name a literal; quest/Reisguh.vanOud). */
    private Component reisName = Component.empty();

    public Component getReisName() {
        return reisName;
    }

    public void setReisName(Component name) {
        this.reisName = name;
        if (getKind() == Kind.REISGUH && !nl.juiced.guhs.taal.Tekst.empty(name)) {
            this.setCustomName(name.copy());
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
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
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
                GuhQuests.say(serverPlayer, this, "quest.guhs.kermis.hello");
                openShop(player);
            } else {
                GuhQuests.talkTo(this, serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kind", getKind().id());
        if (maagOwner != null) {
            tag.store("MaagOwner", UUIDUtil.CODEC, maagOwner);
        }
        if (getKind() == Kind.VERSTOPGUHTJE) {
            tag.store("Verstop", CompoundTag.CODEC, verstop.save());
        }
        if (!nl.juiced.guhs.taal.Tekst.empty(reisName)) {
            nl.juiced.guhs.taal.Tekst.put(tag, "ReisName", reisName);
        }
        if (!roleData.isEmpty()) {
            tag.store("RoleData", CompoundTag.CODEC, roleData);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        for (Kind kind : Kind.values()) {
            if (kind.id().equals(tag.getStringOr("Kind", ""))) {
                setKind(kind);
            }
        }
        maagOwner = tag.read("MaagOwner", UUIDUtil.CODEC).isPresent() ? tag.read("MaagOwner", UUIDUtil.CODEC).orElseThrow() : null;
        verstop.load(tag.read("Verstop", CompoundTag.CODEC).orElseGet(CompoundTag::new));
        setReisName(nl.juiced.guhs.quest.Reisguh.vanOud(nl.juiced.guhs.taal.Tekst.get(tag, "ReisName")));
        roleData = tag.read("RoleData", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        if (getKind() == Kind.POORTWACHTER && !roleData.contains("GateYaw")) {
            roleData.putFloat("GateYaw", getYRot()); // (the Rotation from a structure is already loaded here)
        }
    }

    // --- a quest character: can't be hurt, pushed or leashed, never despawns ---

    @Override
    public boolean isInvulnerableTo(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source) {
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

    /**
     * Opens the shop for this player. 1.2.7: any number of players can shop with the same character at once (it used to
     * serve one customer at a time, with no time limit: one player with the screen open kept the shop shut for everybody
     * else). Every trading screen has its own counter (the payment and result slots are the menu's); the character only
     * has to know who its {@link #customers} are.
     */
    public void openShop(Player player) {
        if (this.level().isClientSide()) {
            return;
        }
        customers.add(player);
        this.tradingPlayer = player;                                // (the customer who came last)
        openTradingScreen(player, getDisplayName(), 1);
    }

    /** Everybody who has this character's shop open right now (pruned every tick). */
    private final java.util.Set<Player> customers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** Is this player shopping here right now? */
    public boolean isCustomer(Player player) {
        return customers.contains(player);
    }

    private static net.minecraft.world.item.trading.MerchantOffer offer(int bonnen, GuhClothes clothes) {
        return new net.minecraft.world.item.trading.MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(nl.juiced.guhs.registry.ModItems.KERMISBON.get(), bonnen),
                new net.minecraft.world.item.ItemStack(nl.juiced.guhs.registry.ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
    }

    /** (Vanilla calls this with null when a trading screen closes: that customer is pruned in {@link #tick}.) */
    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
        if (player != null) {
            customers.add(player);
        }
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

    /** 26.1: Merchant#stillValid is abstract now; 1.21.1's MerchantMenu checked just the trading player. */
    @Override
    public boolean stillValid(Player player) {
        return customers.contains(player) && this.isAlive() && player.level() == this.level() && player.distanceToSqr(this) <= 64;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }

    @Override
    public void tick() {
        if (getKind() == Kind.POORTWACHTER && !roleData.contains("GateYaw")) {
            // which way the gate faces: as placed, before the guard turns its head to look at people
            roleData.putFloat("GateYaw", getYRot());
        }
        super.tick();
        if (!this.level().isClientSide() && getKind() == Kind.VERSTOPGUHTJE) {
            verstop.tick(this);
        }
        if (!this.level().isClientSide() && getKind() == Kind.REISGUH) {
            nl.juiced.guhs.quest.Reisguh.tick(this);
        }
        if (!this.level().isClientSide()) {
            nl.juiced.guhs.feature.NpcRole role = nl.juiced.guhs.feature.Features.role(getKind());
            if (role != null) {
                role.tick(this);
            }
        }
        if (!this.level().isClientSide() && getKind() == Kind.POORTWACHTER) {
            nl.juiced.guhs.quest.KasteelPoort.tick(this);
        }
        if (!this.level().isClientSide() && getKind() == Kind.KERMIS_GUH) {
            nl.juiced.guhs.quest.Kermis.tick(this);   // 1.2.7: the station's finish line and sleds
        }
        if (!customers.isEmpty()) {
            customers.removeIf(p -> !p.isAlive() || p.isRemoved() || p.level() != this.level()
                    || !(p.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu) || p.distanceToSqr(this) > 64);
        }
        if (tradingPlayer != null && !customers.contains(tradingPlayer)) {
            tradingPlayer = customers.isEmpty() ? null : customers.iterator().next();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
