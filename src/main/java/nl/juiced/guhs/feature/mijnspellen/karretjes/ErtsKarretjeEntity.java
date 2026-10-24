package nl.juiced.guhs.feature.mijnspellen.karretjes;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A cart of the Wisselgrot: a real minecart (the vanilla model, with its load of ore shown inside, so its colour says
 * which bin it belongs in), but without minecart physics. The {@link KarretjesGame} moves it along the network
 * ({@link KarretjesNet.Rit}) every tick: scripted, so it never derails, never stops on a corner and takes every switch the
 * way its lever stands at that moment. The rails under it are only for the eye (the client draws the cart along them).
 * You can't ride it, push it or break it; a cart that no game owns any more disappears by itself, and carts are never
 * saved.
 */
public class ErtsKarretjeEntity extends AbstractMinecart {
    /** The game that moves it (server). */
    @Nullable
    KarretjesGame game;

    public ErtsKarretjeEntity(EntityType<? extends ErtsKarretjeEntity> type, Level level) {
        super(type, level);
    }

    static ErtsKarretjeEntity create(ServerLevel level, KarretjesGame game, Vec3 pos, float yRot, BlockState lading) {
        ErtsKarretjeEntity e = new ErtsKarretjeEntity(KarretjesFeature.KAR.get(), level);
        e.game = game;
        e.setInitialPos(pos.x, pos.y, pos.z);
        e.setYRot(yRot);
        e.yRotO = yRot;
        e.setCustomDisplayBlockState(Optional.of(lading));
        e.setDisplayOffset(7);
        level.addFreshEntity(e);
        return e;
    }

    /** Puts it where the game says (server). */
    void zet(Vec3 pos, float yRot) {
        setPos(pos.x, pos.y, pos.z);
        setYRot(yRot);
        setXRot(0);
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            super.tick();                          // (the client only glides to where the server put it)
            return;
        }
        xo = getX();
        yo = getY();
        zo = getZ();
        yRotO = getYRot();
        if (tickCount % 20 == 0 && (game == null || !game.owns(this))) {
            discard();
        }
    }

    // --- not a normal minecart ------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected Item getDropItem() {
        return Items.MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.MINECART);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }
}
