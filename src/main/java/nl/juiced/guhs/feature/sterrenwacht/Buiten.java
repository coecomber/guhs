package nl.juiced.guhs.feature.sterrenwacht;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.Guhs;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * What the three "buiten" features of 2.8 (sterrenwacht, ballon, kamperen) share: a line said by a named character,
 * the shown advancements of the Knuffeldal tab, a block item with a lore line, and the protection of a loose structure
 * ({@link Bescherming}: no breaking, building, buckets, explosions or griefing mobs; creative players may).
 */
public final class Buiten {
    private Buiten() {
    }

    /** "&lt;Name&gt; text" in the chat, only for this player (the speaker needn't be an entity: e.g. Kapitein Wolkje in his basket). */
    public static void zeg(ServerPlayer player, Component naam, String key, Object... args) {
        MutableComponent line = Component.literal("<").append(naam).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(key, args).withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(line);
    }

    /** Grants a shown advancement of the Knuffeldal tab (guhs:knuffeldal/&lt;name&gt;) that the game can't detect itself. */
    public static void toon(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("knuffeldal/" + name));
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    /** Has the player got this shown advancement of the Knuffeldal tab? */
    public static boolean heeft(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("knuffeldal/" + name));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** A block item with a grey lore line underneath its name (lang {@code block.guhs.<id>.lore}). */
    public static class LoreBlockItem extends BlockItem {
        public LoreBlockItem(Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** An item with a grey lore line (lang {@code item.guhs.<id>.lore}). */
    public static class LoreItem extends Item {
        public LoreItem(Item.Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * A loose structure that can't be broken or built in (like the 2.4 minigame buildings): no breaking, no placing, no
     * buckets or bone meal, no putting out campfires with a shovel, no explosions, no griefing mobs. Doors, chests, the
     * characters, telescopes and sleeping bags all still work. Players in creative mode may change it. (Fire and fluids
     * from outside: feature.Protected asks {@link #in}.)
     */
    public static final class Bescherming {
        private final ResourceKey<Structure> structuur;
        private final String melding;
        /** (GameTests) areas that count as this structure. */
        private final java.util.List<net.minecraft.world.level.levelgen.structure.BoundingBox> test =
                new java.util.concurrent.CopyOnWriteArrayList<>();

        public Bescherming(String structuur, String melding) {
            this.structuur = ResourceKey.create(Registries.STRUCTURE, Guhs.id(structuur));
            this.melding = melding;
        }

        public ResourceKey<Structure> structuur() {
            return structuur;
        }

        public void register() {
            NeoForge.EVENT_BUS.addListener(this::onBreak);
            NeoForge.EVENT_BUS.addListener(this::onPlace);
            NeoForge.EVENT_BUS.addListener(this::onUseBlock);
            NeoForge.EVENT_BUS.addListener(this::onExplosion);
            NeoForge.EVENT_BUS.addListener(this::onMobGriefing);
            nl.juiced.guhs.feature.Protected.add(this::in);
        }

        /** (GameTests) this box counts as the structure (until {@link #testWissen}). */
        public void testGebied(net.minecraft.world.level.levelgen.structure.BoundingBox box) {
            test.add(box);
        }

        public void testWissen() {
            test.clear();
        }

        /** Is this spot part of such a structure (server side)? */
        public boolean in(Level level, BlockPos pos) {
            if (!(level instanceof ServerLevel server) || !server.isLoaded(pos)) {
                return false;
            }
            for (var box : test) {
                if (box.isInside(pos)) {
                    return true;
                }
            }
            Structure s = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(structuur);
            return s != null && server.structureManager().getStructureWithPieceAt(pos, s).isValid();
        }

        private boolean denied(Player player, BlockPos pos, boolean quiet) {
            if (player.getAbilities().instabuild || !in(player.level(), pos)) {
                return false;
            }
            if (!quiet) {
                player.sendOverlayMessage(Component.translatable(melding).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return true;
        }

        private void onBreak(BreakBlockEvent event) {
            if (denied(event.getPlayer(), event.getPos(), false)) {
                event.setCanceled(true);
            }
        }

        private void onPlace(BlockEvent.EntityPlaceEvent event) {
            Entity entity = event.getEntity();
            if (entity instanceof Player player ? denied(player, event.getPos(), false) : entity != null && in(entity.level(), event.getPos())) {
                event.setCanceled(true);
            }
        }

        private void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
            ItemStack stack = event.getItemStack();
            if (event.getLevel().isClientSide() || stack.isEmpty()) {
                return;
            }
            boolean building = stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem || stack.getItem() instanceof ShovelItem
                    || stack.is(net.minecraft.world.item.Items.BONE_MEAL) || stack.is(net.minecraft.world.item.Items.FLINT_AND_STEEL);
            if (!building) {
                return;
            }
            Direction face = event.getFace() == null ? Direction.UP : event.getFace();
            if (denied(event.getEntity(), event.getPos(), true) || denied(event.getEntity(), event.getPos().relative(face), false)) {
                event.setUseItem(TriState.FALSE);
                event.setCanceled(true);
            }
        }

        private void onExplosion(ExplosionEvent.Detonate event) {
            if (event.getLevel() instanceof ServerLevel server) {
                event.getAffectedBlocks().removeIf(pos -> in(server, pos));
            }
        }

        private void onMobGriefing(EntityMobGriefingEvent event) {
            if (event.getEntity() != null && !(event.getEntity() instanceof Player) && in(event.getEntity().level(), event.getEntity().blockPosition())) {
                event.setCanGrief(false);
            }
        }
    }
}
