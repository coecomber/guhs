package nl.juiced.guhs.feature.spiesburcht;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/** The pan of a {@link GuhbrouwketelBlock}: the fire (fuel), what's in the pan and how long it still has to bubble. */
public class GuhbrouwketelBlockEntity extends BlockEntity {
    public static final int BREWS_PER_POWDER = 4;
    public static final int MAX_FUEL = 20;
    /** Ticks of bubbling before a brew is ready (a vanilla brewing stand takes 400). */
    public static final int BREW_TICKS = 200;
    public static final int PORTIONS = 3;

    private int fuel;
    private int brewing;
    private Brouwsel next = Brouwsel.BOUILLON;

    public GuhbrouwketelBlockEntity(BlockPos pos, BlockState state) {
        super(SpiesburchtFeature.GUHBROUWKETEL_BE.get(), pos, state);
    }

    public int fuel() {
        return fuel;
    }

    public boolean isBrewing() {
        return brewing > 0;
    }

    public int portions() {
        return getBlockState().getValue(GuhbrouwketelBlock.VULLING);
    }

    public Brouwsel contents() {
        return Brouwsel.byIndex(getBlockState().getValue(GuhbrouwketelBlock.BROUWSEL));
    }

    /** A right-click with the item in that hand. True: something happened (the item was used). */
    public boolean use(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Level level = getLevel();
        if (level == null) {
            return false;
        }
        // 1. the fire
        if (stack.is(SpiesburchtFeature.GRILLSPIESPOEDER.get())) {
            if (fuel + BREWS_PER_POWDER > MAX_FUEL) {
                say(player, "vol_vuur", ChatFormatting.GOLD);
                return true;
            }
            stack.consume(1, player);
            fuel += BREWS_PER_POWDER;
            level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
            say(player, "gestookt", ChatFormatting.GOLD);
            update();
            return true;
        }
        // 2. the kaassaus
        if (stack.is(ModItems.KAAS_SAUS_BUCKET.get())) {
            if (portions() > 0) {
                say(player, "al_vol", ChatFormatting.GRAY);
                return true;
            }
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            }
            setContents(PORTIONS, Brouwsel.BOUILLON);
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1f, 1.3f);
            say(player, "saus_erin", ChatFormatting.YELLOW);
            return true;
        }
        // 4. bottling
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (portions() == 0 || contents() == Brouwsel.BOUILLON || isBrewing()) {
                say(player, isBrewing() ? "nog_even" : portions() == 0 ? "leeg" : "eerst_ingredient", ChatFormatting.GRAY);
                return true;
            }
            ItemStack drankje = contents().drankje();
            stack.consume(1, player);
            if (!player.getInventory().add(drankje)) {
                player.drop(drankje, false);
            }
            int left = portions() - 1;
            setContents(left, left == 0 ? Brouwsel.BOUILLON : contents());
            level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1f, 1f);
            GuhAdvancements.grant(player, "guhdrankje_gebrouwen");
            return true;
        }
        // 3. an ingredient
        Brouwsel brew = Brouwsel.forIngredient(stack);
        if (brew != null) {
            if (portions() == 0) {
                say(player, "eerst_saus", ChatFormatting.GRAY);
            } else if (contents() != Brouwsel.BOUILLON || isBrewing()) {
                say(player, "al_brouwsel", ChatFormatting.GRAY);
            } else if (fuel <= 0) {
                say(player, "koud", ChatFormatting.GRAY);
            } else {
                stack.consume(1, player);
                startBrewing(brew);
                level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1f, 0.8f);
                say(player, "roeren", ChatFormatting.LIGHT_PURPLE);
            }
            return true;
        }
        return false;
    }

    /** Stir this brew in (fuel and kaasbouillon are there). */
    public void startBrewing(Brouwsel brew) {
        fuel--;
        brewing = BREW_TICKS;
        next = brew;
        update();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GuhbrouwketelBlockEntity ketel) {
        if (ketel.brewing > 0 && --ketel.brewing == 0) {
            ketel.setContents(ketel.portions(), ketel.next);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1f, 1.4f);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    /** Game tests: bubble right now. */
    public void finishBrewing() {
        if (brewing > 0) {
            brewing = 1;
            serverTick(getLevel(), worldPosition, getBlockState(), this);
        }
    }

    private void setContents(int portions, Brouwsel brew) {
        if (level == null) {
            return;
        }
        level.setBlock(worldPosition, getBlockState().setValue(GuhbrouwketelBlock.VULLING, portions)
                .setValue(GuhbrouwketelBlock.BROUWSEL, brew.ordinal()).setValue(GuhbrouwketelBlock.LIT, fuel > 0)
                .setValue(GuhbrouwketelBlock.BORRELT, brewing > 0), 3);
        setChanged();
    }

    private void update() {
        setContents(portions(), contents());
    }

    public void status(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("quest.guhs.guhbrouwketel.status", fuel, portions(),
                Component.translatable("quest.guhs.guhbrouwketel.brouwsel." + contents().id())).withStyle(ChatFormatting.GOLD), true);
    }

    private void say(ServerPlayer player, String key, ChatFormatting colour) {
        player.displayClientMessage(Component.translatable("quest.guhs.guhbrouwketel." + key).withStyle(colour), true);
    }

    /** Broken: whole pinches of grillspiespoeder that are left come back out. */
    void dropFuel() {
        if (level != null && fuel >= BREWS_PER_POWDER) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    new ItemStack(SpiesburchtFeature.GRILLSPIESPOEDER.get(), fuel / BREWS_PER_POWDER));
            fuel = 0;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fuel", fuel);
        tag.putInt("Brewing", brewing);
        tag.putString("Next", next.id());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = tag.getInt("Fuel");
        brewing = tag.getInt("Brewing");
        next = Brouwsel.BOUILLON;
        for (Brouwsel b : Brouwsel.values()) {
            if (b.id().equals(tag.getString("Next"))) {
                next = b;
            }
        }
    }
}
