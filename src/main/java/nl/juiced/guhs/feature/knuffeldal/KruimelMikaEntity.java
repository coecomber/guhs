package nl.juiced.guhs.feature.knuffeldal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.ModDimensions;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.EntitySpawnReason;
/**
 * A Kruimel-Mika (2.8, Grote Knusfeest): a small, sandy-coloured Mika with crumbs all over its face. It never fights:
 * it can't be hurt and it doesn't hurt anyone. It only steals.
 * <ol>
 *   <li>On the way to the Burgemeester (in the Knuffeldal) with the feesttaart, the theeservies or the feestslingers,
 *   one pops up and snatches it ({@link #steel}; the task becomes {@code GESTOLEN}).</li>
 *   <li>It runs off giggling and hides a bit further on, munching, leaving a trail of crumbs behind it.</li>
 *   <li>Follow the crumbs and come with a treat ({@code #guhs:knus/lekkernij}: kaasknabbels, gebak): it forgets its loot
 *   and follows the treat. Give it the treat: it munches, drops the loot (you get it back: {@code TERUGGEVONDEN}) and
 *   runs off giggling for good (poof).</li>
 *   <li>Without a treat it just giggles and hops further away. Hitting it: "njeg, niet meppen!".</li>
 * </ol>
 */
public class KruimelMikaEntity extends PathfinderMob implements GeoEntity {
    public enum Toestand { RENT_WEG, VERSTOPT, GELOKT, WEG }

    /** How far it runs with its loot; how close a treat must be to lure it; a trail point every this many blocks. */
    public static final int VLUCHT = 24;
    public static final double LOK_AFSTAND = 7, SPOOR_STAP = 1.5;
    /** It gives up and leaves (the loot goes back via the Burgemeester) when its player is gone this long (ticks). */
    public static final int ZONDER_SPELER = 20 * 60;
    /** The Burgemeester gives a stolen item back himself after this long (game ticks): no getting stuck. */
    public static final long TERUG_NA = 20L * 60 * 5;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guh.walk");
    private static final RawAnimation SMAK = RawAnimation.begin().thenLoop("animation.guh.emote_smakken");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private Toestand toestand = Toestand.RENT_WEG;
    @Nullable
    private UUID eigenaar;
    @Nullable
    private Feesttaak taak;
    private ItemStack buit = ItemStack.EMPTY;
    private final List<Vec3> spoor = new ArrayList<>();
    private int zonderSpeler;
    private int wegTimer;

    public KruimelMikaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.translatable("entity.guhs.kruimel_mika"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.SCALE, 0.6);
    }

    static void register() {
        // (nothing global yet: the steal is started from KnuffeldalEvents.onPlayerTick -> playerTick)
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return eigenaar == null && super.canUse();   // (only a Kruimel-Mika of its own, from the spawn egg, wanders)
            }
        });
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    // =================================================================================================================
    // stealing
    // =================================================================================================================

    /**
     * A Kruimel-Mika snatches the feest-item of this task from the player (it must be in their pockets): it pops up next
     * to them and runs off with it. Returns the Mika (null: nothing to steal).
     */
    @Nullable
    public static KruimelMikaEntity steel(ServerPlayer player, Feesttaak taak) {
        ItemStack loot = neem(player, taak);
        return loot.isEmpty() ? null : steel(player, taak, loot);
    }

    /** The steal itself, with this loot (already out of the player's pockets; tests use a stand-in item). */
    @Nullable
    public static KruimelMikaEntity steel(ServerPlayer player, Feesttaak taak, ItemStack loot) {
        ServerLevel level = player.level();
        KruimelMikaEntity mika = KnuffeldalFeature.KRUIMEL_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
        if (mika == null) {
            Minigames.give(player, loot);
            return null;
        }
        Vec3 behind = player.position().subtract(player.getLookAngle().multiply(1, 0, 1).normalize().scale(2));
        mika.snapTo(behind.x, player.getY(), behind.z, player.getYRot(), 0);
        mika.eigenaar = player.getUUID();
        mika.taak = taak;
        mika.buit = loot;
        mika.setPersistenceRequired();
        level.addFreshEntity(mika);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, mika.getX(), mika.getY() + 0.3, mika.getZ(), 12, 0.3, 0.2, 0.3, 0.02);
        mika.giechel();
        Knusfeest.zet(player, taak, Knusfeest.Stap.GESTOLEN);
        player.sendSystemMessage(Component.translatable("gui.guhs.kruimel_mika.gestolen", loot.getHoverName()).withStyle(ChatFormatting.GOLD));
        mika.vlucht(player.position(), VLUCHT);
        return mika;
    }

    /** Takes one feest-item of this task out of the player's pockets (empty: they don't have one). */
    static ItemStack neem(ServerPlayer player, Feesttaak taak) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(taak.tag())) {
                return s.split(1);
            }
        }
        return ItemStack.EMPTY;
    }

    /** The feest-item of this task in the player's pockets (not taken out; empty: none). */
    static ItemStack neemNiet(ServerPlayer player, Feesttaak taak) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(taak.tag())) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Every 2 seconds per player: carrying the cake, the tea set or the garlands (made, not stolen yet) through the
     * Knuffeldal, now and then a Kruimel-Mika takes it. Also: a stolen item that's been gone too long comes back.
     */
    static void playerTick(ServerPlayer player) {
        if (player.level().dimension() != ModDimensions.GUHMENSION || !KnuffeldalEvents.inKnuffeldal(player.level(), player.blockPosition())
                || player.isCreative() || Minigames.playing(player) != null) {
            return;
        }
        for (Feesttaak taak : Feesttaak.values()) {
            if (taak.steelbaar() && Knusfeest.stap(player, taak) == Knusfeest.Stap.GEMAAKT && player.getRandom().nextInt(8) == 0
                    && heeftGeen(player)) {
                steel(player, taak);
                return;
            }
        }
    }

    /** No Kruimel-Mika is after this player right now. */
    static boolean heeftGeen(ServerPlayer player) {
        return player.level().getEntitiesOfClass(KruimelMikaEntity.class, player.getBoundingBox().inflate(128),
                m -> player.getUUID().equals(m.eigenaar) && m.toestand != Toestand.WEG).isEmpty();
    }

    /** Runs off (away from `from`), leaving crumbs. */
    void vlucht(Vec3 from, int afstand) {
        this.toestand = Toestand.RENT_WEG;
        Vec3 target = DefaultRandomPos.getPosAway(this, afstand, 6, from);
        if (target == null) {
            target = position().add(position().subtract(from).multiply(1, 0, 1).normalize().scale(afstand));
        }
        getNavigation().moveTo(target.x, target.y, target.z, 1.45);
        spoorPunt();
    }

    private void spoorPunt() {
        Vec3 p = position();
        if (spoor.isEmpty() || spoor.get(spoor.size() - 1).distanceTo(p) >= SPOOR_STAP) {
            spoor.add(p);
            if (spoor.size() > 200) {
                spoor.remove(0);
            }
        }
    }

    @Nullable
    private ServerPlayer eigenaar() {
        return eigenaar != null && level() instanceof ServerLevel server && server.getPlayerByUUID(eigenaar) instanceof ServerPlayer p ? p : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        ServerLevel server = (ServerLevel) level();
        ServerPlayer owner = eigenaar();
        if (eigenaar != null) {
            if (owner == null || owner.level() != level() || owner.distanceTo(this) > 128) {
                if (++zonderSpeler > ZONDER_SPELER) {
                    poef();   // (its player went away: the Burgemeester gets the loot back later)
                    return;
                }
            } else {
                zonderSpeler = 0;
            }
        }
        switch (toestand) {
            case RENT_WEG -> {
                spoorPunt();
                if (getNavigation().isDone()) {
                    toestand = Toestand.VERSTOPT;
                }
            }
            case VERSTOPT -> {
                if (tickCount % 10 == 0) {
                    server.sendParticles(KnuffeldalFeature.KRUIMEL.get(), getX(), getY() + 0.4, getZ(), 3, 0.2, 0.1, 0.2, 0.02);
                }
                if (owner != null && owner.distanceTo(this) < LOK_AFSTAND) {
                    if (lekkernij(owner)) {
                        toestand = Toestand.GELOKT;
                        owner.sendOverlayMessage(Component.translatable("gui.guhs.kruimel_mika.gelokt").withStyle(ChatFormatting.LIGHT_PURPLE));
                    } else if (owner.distanceTo(this) < 4) {
                        giechel();
                        owner.sendOverlayMessage(Component.translatable("gui.guhs.kruimel_mika.zonder").withStyle(ChatFormatting.GOLD));
                        vlucht(owner.position(), 10);
                    }
                }
            }
            case GELOKT -> {
                if (owner == null || !lekkernij(owner) || owner.distanceTo(this) > LOK_AFSTAND * 2) {
                    toestand = Toestand.VERSTOPT;
                    getNavigation().stop();
                } else {
                    getLookControl().setLookAt(owner, 30, 30);
                    if (distanceTo(owner) > 1.8) {
                        getNavigation().moveTo(owner, 1.1);
                    } else {
                        getNavigation().stop();
                    }
                }
            }
            case WEG -> {
                if (++wegTimer > 60 || getNavigation().isDone() && wegTimer > 20) {
                    poef();
                    return;
                }
            }
        }
        // the crumb trail: crumbs along its way, for everyone close by
        if (tickCount % 10 == 0 && !spoor.isEmpty() && toestand != Toestand.WEG) {
            for (int i = 0; i < spoor.size(); i += 1) {
                Vec3 p = spoor.get(i);
                if (server.getNearestPlayer(p.x, p.y, p.z, 40, false) != null) {
                    server.sendParticles(KnuffeldalFeature.KRUIMEL.get(), p.x, p.y + 0.1, p.z, 1, 0.15, 0.02, 0.15, 0.0);
                }
            }
        }
    }

    /** Is this player holding a treat (#guhs:knus/lekkernij)? */
    static boolean lekkernij(Player player) {
        return player.getMainHandItem().is(KnusTags.LEKKERNIJ) || player.getOffhandItem().is(KnusTags.LEKKERNIJ)
                || player.getMainHandItem().is(ModItems.KAAS_KNABBELS.get()) || player.getOffhandItem().is(ModItems.KAAS_KNABBELS.get());
    }

    /** Right-click: with a treat it eats, lets go of the loot and runs off giggling; without, it giggles. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean treat = stack.is(KnusTags.LEKKERNIJ) || stack.is(ModItems.KAAS_KNABBELS.get());
        if (level().isClientSide()) {
            return treat ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer sp) || toestand == Toestand.WEG) {
            return InteractionResult.PASS;
        }
        if (!treat) {
            giechel();
            sp.sendOverlayMessage(Component.translatable("gui.guhs.kruimel_mika.zonder").withStyle(ChatFormatting.GOLD));
            return InteractionResult.CONSUME;
        }
        stack.consume(1, player);
        lokWeg(sp);
        return InteractionResult.CONSUME;
    }

    /** It got its treat: the loot goes back (to its own player; else it's just dropped here) and it runs off for good. */
    public void lokWeg(ServerPlayer player) {
        playSound(nl.juiced.guhs.registry.ModSounds.GUH_EAT.get(), 1f, 1.5f);
        ((ServerLevel) level()).sendParticles(KnuffeldalFeature.KRUIMEL.get(), getX(), getY() + 0.5, getZ(), 16, 0.3, 0.2, 0.3, 0.05);
        if (!buit.isEmpty()) {
            ServerPlayer owner = eigenaar();
            ServerPlayer to = owner != null ? owner : player;
            Minigames.give(to, buit.copy());
            to.sendSystemMessage(Component.translatable("gui.guhs.kruimel_mika.terug", buit.getHoverName()).withStyle(ChatFormatting.GREEN));
            if (taak != null && Knusfeest.stap(to, taak) == Knusfeest.Stap.GESTOLEN) {
                Knusfeest.zet(to, taak, Knusfeest.Stap.TERUGGEVONDEN);
            }
            buit = ItemStack.EMPTY;
        }
        KnusVoortgang.tel(player, KnuffeldalVoortgang.KRUIMEL_MIKAS, 1);
        GuhAdvancements.grant(player, "knuffeldal_kruimel_mika");
        KnuffeldalAdvancements.toon(player, "kruimel_mika_verjaagd");
        giechel();
        vlucht(player.position(), 16);
        toestand = Toestand.WEG;       // (after vlucht: it runs off for good)
        wegTimer = 0;
    }

    /** Poof: gone (with a puff of crumbs). */
    void poef() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 10, 0.3, 0.2, 0.3, 0.02);
            server.sendParticles(KnuffeldalFeature.KRUIMEL.get(), getX(), getY() + 0.3, getZ(), 12, 0.3, 0.2, 0.3, 0.05);
        }
        discard();
    }

    void giechel() {
        level().playSound(null, this, KnuffeldalFeature.KRUIMEL_GIECHEL.get(), SoundSource.NEUTRAL, 1f, 1.1f + getRandom().nextFloat() * 0.3f);
    }

    public Toestand toestand() {
        return toestand;
    }

    public ItemStack buit() {
        return buit;
    }

    @Nullable
    public Feesttaak taak() {
        return taak;
    }

    public List<Vec3> spoor() {
        return List.copyOf(spoor);
    }

    // =================================================================================================================
    // never hurt, never hurting
    // =================================================================================================================

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide() && source.getEntity() instanceof ServerPlayer player && isInvulnerableTo(source)) {
            giechel();
            player.sendOverlayMessage(Component.translatable("gui.guhs.kruimel_mika.niet_meppen").withStyle(ChatFormatting.LIGHT_PURPLE));
            if (toestand != Toestand.WEG) {
                vlucht(player.position(), 6);
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KnuffeldalFeature.KRUIMEL_GIECHEL.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public float getVoicePitch() {
        return 1.5f + getRandom().nextFloat() * 0.2f;
    }

    // =================================================================================================================
    // saving
    // =================================================================================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Toestand", toestand.name());
        if (eigenaar != null) {
            tag.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }
        if (taak != null) {
            tag.putString("Taak", taak.id());
        }
        if (!buit.isEmpty()) {
            tag.put("Buit", buit.save(registryAccess()));
        }
        ListTag list = new ListTag();
        for (Vec3 p : spoor) {
            list.add(LongTag.valueOf(BlockPos.containing(p).asLong()));
        }
        tag.put("Spoor", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            toestand = Toestand.valueOf(tag.getStringOr("Toestand", ""));
        } catch (IllegalArgumentException e) {
            toestand = Toestand.VERSTOPT;
        }
        eigenaar = tag.read("Eigenaar", UUIDUtil.CODEC).isPresent() ? tag.read("Eigenaar", UUIDUtil.CODEC).orElseThrow() : null;
        taak = Feesttaak.byId(tag.getStringOr("Taak", ""));
        buit = tag.contains("Buit") ? ItemStack.parse(registryAccess(), tag.getCompoundOrEmpty("Buit")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        spoor.clear();
        for (Tag t : tag.getListOrEmpty("Spoor")) {
            spoor.add(net.minecraft.world.phys.Vec3.atBottomCenterOf(BlockPos.of(((LongTag) t).getAsLong())));
        }
    }

    // =================================================================================================================
    // animations (the guh ones: its model is the Mika's)
    // =================================================================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(toestand == Toestand.VERSTOPT ? SMAK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
