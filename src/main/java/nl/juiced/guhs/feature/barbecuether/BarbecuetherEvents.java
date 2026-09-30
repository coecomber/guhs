package nl.juiced.guhs.feature.barbecuether;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/** Game events of the Barbecuether: beds that won't explode, and the Aanmaakblokjes the Mikas stole. */
public final class BarbecuetherEvents {
    public static final ResourceKey<Structure> MIKA_KAMP = ResourceKey.create(Registries.STRUCTURE, Guhs.id("mika_kamp"));
    /** Game tests: areas that count as a Mika-kamp. */
    public static final List<AABB> TEST_CAMPS = new CopyOnWriteArrayList<>();
    /** At most this many Aanmaakblokjes in your pockets before the Mikas stop dropping them. */
    public static final int MAX_CARRIED = 3;

    /** A bed in the Barbecuether: no explosion (like the Nether), just "Te warm om te slapen, njeg!". */
    static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.dimension() == BarbecuetherFeature.BARBECUETHER && level.getBlockState(event.getPos()).getBlock() instanceof BedBlock) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getEntity() instanceof ServerPlayer player) {
                tooHot(player);
            }
        }
    }

    public static void tooHot(ServerPlayer player) {
        player.sendOverlayMessage(Component.translatable("quest.guhs.barbecuether.te_warm").withStyle(ChatFormatting.GOLD));
        GuhAdvancements.grant(player, "te_warm");
        var shown = player.level().getServer().getAdvancements().get(Guhs.id("barbecuether/te_warm"));
        if (shown != null && !player.getAdvancements().getOrStartProgress(shown).isDone()) {
            player.getAdvancements().award(shown, "done");
        }
    }

    /** While the Grillguh's quest runs, every Mika you beat in a Mika-kamp drops one of his stolen Aanmaakblokjes. */
    static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof MikaEntity mika && mika.getType() == ModEntities.MIKA.get()
                && event.getSource().getEntity() instanceof ServerPlayer player && mika.level() instanceof ServerLevel level) {
            ItemStack drop = questDrop(player, level, mika.blockPosition());
            if (!drop.isEmpty()) {
                event.getDrops().add(new ItemEntity(level, mika.getX(), mika.getY() + 0.3, mika.getZ(), drop));
            }
        }
    }

    /** What a Mika beaten by this player here drops for the quest (empty: nothing). */
    public static ItemStack questDrop(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!Grillguh.questActive(player) || !inMikaKamp(level, pos)
                || GuhQuests.count(player, BarbecuetherFeature.AANMAAKBLOKJE.get()) >= MAX_CARRIED) {
            return ItemStack.EMPTY;
        }
        player.sendOverlayMessage(Component.translatable("quest.guhs.grillguh.mika_drop").withStyle(ChatFormatting.GOLD));
        return new ItemStack(BarbecuetherFeature.AANMAAKBLOKJE.get());
    }

    public static boolean inMikaKamp(ServerLevel level, BlockPos pos) {
        for (AABB area : TEST_CAMPS) {
            if (area.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                return true;
            }
        }
        var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(MIKA_KAMP);
        return structure != null && level.structureManager().getStructureAt(pos, structure).isValid();
    }

    private BarbecuetherEvents() {
    }
}
