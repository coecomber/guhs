package nl.juiced.guhs.feature.droomwinkel;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.droomwezens.DroomNpc;

/**
 * 1.8.0 (expansion U1): the Zandguh's droomwinkeltje on the Droomplein ({@link DroomWinkel}). The stall
 * ({@link DroomKraampjeBlock}) and its Zandguh are put down by code ({@link #zorg}) while someone dreams near the plein, so the
 * plein's template stays as it is. Clicking the stall or that Zandguh opens the shop.
 */
public final class DroomWinkelFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<DroomKraampjeBlock> KRAAMPJE = BLOCKS.registerBlock("droomkraampje", DroomKraampjeBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_PURPLE).strength(2f).sound(SoundType.WOOD)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> KRAAMPJE_ITEM = ITEMS.registerItem("droomkraampje", p -> new BlockItem(KRAAMPJE.get(), p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("block.guhs.droomkraampje.lore").withStyle(ChatFormatting.GRAY));
        }
    }, () -> new Item.Properties().useBlockDescriptionPrefix());

    /** The Zandguh of the shop carries this in his roleData. */
    public static final String NPC_TAG = "guhs_droomwinkel";
    /** Where the stall goes (relative to the plein's centre), how far it may search for room, how often it is checked. */
    static final Vec3 PLEK = new Vec3(-7, 1, 9);
    static final int ZOEK_R = 8, ELKE = 100;
    static final double BIJ_PLEIN = 80;

    private DroomWinkelFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        DroomWinkel.registreerVast();
        NeoForge.EVENT_BUS.addListener(DroomWinkelFeature::tick);
        NeoForge.EVENT_BUS.addListener(DroomWinkelFeature::onPraat);
    }

    public static void payloads(PayloadRegistrar registrar) {
        DroomWinkelPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KRAAMPJE_ITEM.get()));
    }

    private static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % ELKE != 41) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(DromenFeature.DIM);
        if (level == null || level.players().isEmpty()) {
            return;
        }
        Vec3 midden = Vec3.atBottomCenterOf(DroomEilanden.midden(DroomEilanden.PLEIN));
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceTo(midden) < BIJ_PLEIN) {
                zorg(level);
                return;
            }
        }
    }

    /** The Zandguh of the shop: clicking him opens the droomwinkeltje (instead of his jokes). */
    private static void onPraat(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof GuhNpcEntity npc) || !(event.getEntity() instanceof ServerPlayer p)
                || event.getHand() != InteractionHand.MAIN_HAND || !npc.roleData.getBooleanOr(NPC_TAG, false)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        DroomWinkel.open(p);
    }

    // --- putting the stall down -----------------------------------------------------------------------------------------------

    /** Makes sure the stall and its Zandguh stand on the plein (once the plein is built). Returns the stall, or null. */
    @Nullable
    public static BlockPos zorg(ServerLevel level) {
        if (DroomEilanden.gebouwd(level, DroomEilanden.PLEIN) < 0) {
            return null;
        }
        BlockPos kraam = zoek(level);
        if (kraam == null) {
            kraam = zetNeer(level);
        }
        if (kraam != null) {
            zorgVoorZandguh(level, kraam);
        }
        return kraam;
    }

    static BlockPos doel() {
        return BlockPos.containing(DroomEilanden.wereld(DroomEilanden.PLEIN, PLEK));
    }

    @Nullable
    static BlockPos zoek(ServerLevel level) {
        BlockPos d = doel();
        for (BlockPos b : BlockPos.betweenClosed(d.offset(-ZOEK_R - 1, -4, -ZOEK_R - 1), d.offset(ZOEK_R + 1, 4, ZOEK_R + 1))) {
            if (level.getBlockState(b).is(KRAAMPJE.get())) {
                return b.immutable();
            }
        }
        return null;
    }

    /** The nearest spot (rings around the target) where the stall and, behind it, its Zandguh both have room; facing the plein. */
    @Nullable
    static BlockPos zetNeer(ServerLevel level) {
        BlockPos d = doel();
        BlockPos midden = DroomEilanden.midden(DroomEilanden.PLEIN);
        for (int r = 0; r <= ZOEK_R; r++) {
            for (int dy = 0; dy >= -3; dy--) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        BlockPos pos = d.offset(dx, dy, dz);
                        Direction kijk = naar(pos, midden);
                        if (vrij(level, pos) && vrij(level, pos.relative(kijk.getOpposite()))) {
                            BlockState st = KRAAMPJE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, kijk);
                            level.setBlock(pos, st, Block.UPDATE_ALL);
                            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Which way the stall's front looks: towards the plein's centre. */
    static Direction naar(BlockPos van, BlockPos naar) {
        int vx = naar.getX() - van.getX(), vz = naar.getZ() - van.getZ();
        return Math.abs(vx) > Math.abs(vz) ? (vx > 0 ? Direction.EAST : Direction.WEST) : (vz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    static boolean vrij(ServerLevel level, BlockPos pos) {
        BlockState onder = level.getBlockState(pos.below());
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                && onder.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    /** One Zandguh behind the stall (spawned when missing; extra ones are removed). */
    static void zorgVoorZandguh(ServerLevel level, BlockPos kraam) {
        List<GuhNpcEntity> al = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(kraam).inflate(12),
                n -> n.roleData.getBooleanOr(NPC_TAG, false));
        if (!al.isEmpty()) {
            for (int i = 1; i < al.size(); i++) {
                al.get(i).discard();
            }
            return;
        }
        BlockState st = level.getBlockState(kraam);
        Direction kijk = st.hasProperty(HorizontalDirectionalBlock.FACING) ? st.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        BlockPos achter = kraam.relative(kijk.getOpposite());
        GuhNpcEntity npc = DroomNpc.spawn(level, Vec3.atBottomCenterOf(achter), kijk.toYRot(), GuhNpcEntity.Kind.ZANDGUH);
        npc.roleData.putBoolean(NPC_TAG, true);
        npc.setCustomName(Component.translatable("entity.guhs.droomwinkel_zandguh"));
    }
}
