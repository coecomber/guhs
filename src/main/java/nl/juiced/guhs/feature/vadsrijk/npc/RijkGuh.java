package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.EnumSet;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;

/**
 * An NPC guh of Het Vadsrijk (vr_npc_guh): the real guh model with the existing clothes (drawn by GuhRenderer), never tamed,
 * fed, pushed, leashed or hurt (guhs are always passive). Spawned by code ({@link Npcs}) and never saved: a chunk that
 * unloads takes it along and {@link Npcs} puts it back when a player comes near. A click goes to {@link QuestMotor#klik}.
 * <p>Idling: it looks at players, waves now and then when you come close, does a little emote once in a while; a
 * {@link NpcDef.Gedrag#SLENTER} citizen wanders a few blocks around its spot, a {@link NpcDef.Gedrag#ZITTEN} one sits.
 * An escort ({@link #volg}) follows its player.
 */
public class RijkGuh extends GuhEntity {
    /** The NpcDef id ("" = not set up yet). Server side. */
    String npcId = "";
    @Nullable
    BlockPos thuis;
    NpcDef.Gedrag gedrag = NpcDef.Gedrag.STAAN;
    /** Escort: the player it follows (null: not following). */
    @Nullable
    UUID volgt;
    /** After an escort: it rests at the destination until this game time (so you can hand in there), then walks home. */
    long rustTot;
    @Nullable
    BlockPos rustPlek;
    private int zwaaiPauze;

    public RijkGuh(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public String npcId() {
        return npcId;
    }

    /** Dress and place it after its definition (server side, right after creating it). */
    public void stelIn(NpcDef d) {
        this.npcId = d.id();
        this.thuis = d.plek();
        this.gedrag = d.gedrag();
        setVariant(d.variant());
        for (GuhClothes c : d.kleren()) {
            wear(c);
        }
        setCustomName(Component.translatable(d.naamKey()));
        setCustomNameVisible(true);
        if (d.maat() != 1f && getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) != null) {
            getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(d.maat());
        }
        boolean zit = d.gedrag() == NpcDef.Gedrag.ZITTEN;
        setOrderedToSit(zit);
        setInSittingPose(zit);
    }

    /** Stay around here for a while (an escort that arrived). */
    public void rust(BlockPos plek, int ticks) {
        this.rustPlek = plek.immutable();
        this.rustTot = level().getGameTime() + ticks;
    }

    boolean rust() {
        return rustPlek != null && level().getGameTime() < rustTot;
    }

    public void volg(@Nullable ServerPlayer p) {
        this.volgt = p == null ? null : p.getUUID();
        if (p != null) {
            setOrderedToSit(false);
            setInSittingPose(false);
        }
    }

    @Nullable
    public UUID volgt() {
        return volgt;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new VolgGoal());
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(3, new SlenterGoal());
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide() && player instanceof ServerPlayer sp) {
            getNavigation().stop();
            getLookControl().setLookAt(sp);
            QuestMotor.klik(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel sl)) {
            return;
        }
        if (zwaaiPauze > 0) {
            zwaaiPauze--;
        }
        if (this.tickCount % 20 == 7 && zwaaiPauze == 0 && volgt == null) {
            Player p = sl.getNearestPlayer(this, 5);
            if (p != null && getRandom().nextInt(4) == 0) {
                emotes.start(Emote.ZWAAIEN, false, GuhEmotes.Source.SELF);
                zwaaiPauze = 20 * 90;
            }
        }
        if (this.tickCount % 600 == 300 && volgt == null && getRandom().nextInt(3) == 0) {
            Emote[] idle = gedrag == NpcDef.Gedrag.ZITTEN ? new Emote[] {Emote.GAPEN, Emote.ZINGEN, Emote.SMAKKEN}
                    : new Emote[] {Emote.VAHOEG, Emote.ZINGEN, Emote.DANSEN, Emote.GAPEN, Emote.HARTJES};
            emotes.start(idle[getRandom().nextInt(idle.length)], false, GuhEmotes.Source.SELF);
        }
    }

    /** Escort: follow the player (teleport along when far behind). */
    private final class VolgGoal extends Goal {
        VolgGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private ServerPlayer speler() {
            return volgt == null || !(level() instanceof ServerLevel sl) ? null
                    : sl.getServer().getPlayerList().getPlayer(volgt) instanceof ServerPlayer p && p.level() == level() ? p : null;
        }

        @Override
        public boolean canUse() {
            return speler() != null;
        }

        @Override
        public void tick() {
            ServerPlayer p = speler();
            if (p == null) {
                return;
            }
            getLookControl().setLookAt(p);
            double d = distanceTo(p);
            if (d > 24) {
                snapTo(p.getX() + (getRandom().nextDouble() - 0.5) * 2, p.getY(), p.getZ() + (getRandom().nextDouble() - 0.5) * 2);
                getNavigation().stop();
            } else if (d > 3.2) {
                getNavigation().moveTo(p, 1.15);
            } else {
                getNavigation().stop();
            }
        }
    }

    /** A citizen strolls around its spot; everyone walks back home when pushed away. */
    private final class SlenterGoal extends Goal {
        private int pauze = 40;

        SlenterGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (thuis == null || volgt != null || isInSittingPose()) {
                return false;
            }
            if (--pauze > 0) {
                return false;
            }
            pauze = 120 + getRandom().nextInt(200);
            BlockPos h = rust() ? rustPlek : thuis;
            return gedrag == NpcDef.Gedrag.SLENTER || distanceToSqr(h.getX() + 0.5, getY(), h.getZ() + 0.5) > 4;
        }

        @Override
        public void start() {
            BlockPos doel = rust() ? rustPlek : thuis;
            if (gedrag == NpcDef.Gedrag.SLENTER) {
                doel = thuis.offset(getRandom().nextInt(9) - 4, 0, getRandom().nextInt(9) - 4);
            }
            getNavigation().moveTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, 0.6);
        }

        @Override
        public boolean canContinueToUse() {
            return !getNavigation().isDone() && volgt == null;
        }
    }

    // --- never anybody's, never hurt, never saved ---------------------------------------------------------------------------

    @Override
    public void onOwnerTap(Player player) {
    }

    @Override
    public void onLaunchPressed(Player rider) {
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel serverLevel, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    protected void dropEquipment(ServerLevel serverLevel) {
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isSaddleable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }
}
