package nl.juiced.guhs.feature.einde2.mikas;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.registry.ModItems;

/**
 * {@code /guhs einde2 mikas toon}: a little stage for a look at this slice (tools/autocheck/einde2_mikas.txt): a floor, a
 * back wall and a bit of ceiling, with an Ender-Mika that stands still (a trader), three Schuilguhs (floor, wall, ceiling),
 * a Kaasmijt that stays, and two Schuildozen (one with something in it). It builds in the world, so only a dev run has it.
 */
final class E2MikasCommando {
    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("mikas")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("toon").executes(c -> toon(c.getSource()))))));
    }

    /** The stage: 11 wide, 7 deep, its floor at the player's feet, 4 blocks to the south of the player (the player looks at it). */
    private static int toon(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        BlockPos o = player.blockPosition().offset(-5, -1, 4);
        BlockState korst = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Guhs.id("kaaskorst_stenen")).defaultBlockState();
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 7; z++) {
                level.setBlock(o.offset(x, 0, z), korst, 3);
                for (int y = 1; y <= 5; y++) {
                    level.setBlock(o.offset(x, y, z), z == 6 || y == 5 && z >= 4 ? korst : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        EnderMikaEntity mika = zet(level, E2MikasFeature.ENDER_MIKA.get(), o.offset(2, 1, 3), 180f);
        mika.setHandelaar(true);
        mika.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(ModItems.KAAS_KNABBELS.get()));
        KaasmijtEntity mijt = zet(level, E2MikasFeature.KAASMIJT.get(), o.offset(4, 1, 2), 180f);
        mijt.setPersistenceRequired();
        mijt.setNoAi(true);
        zet(level, E2MikasFeature.SCHUILGUH.get(), o.offset(6, 1, 3), 180f).setAttachFace(Direction.DOWN);
        zet(level, E2MikasFeature.SCHUILGUH.get(), o.offset(8, 3, 5), 0f).setAttachFace(Direction.SOUTH);
        zet(level, E2MikasFeature.SCHUILGUH.get(), o.offset(5, 4, 5), 0f).setAttachFace(Direction.UP);
        BlockState doos = E2MikasFeature.SCHUILDOOS.get().defaultBlockState();
        level.setBlock(o.offset(9, 1, 2), doos, 3);
        level.setBlock(o.offset(9, 1, 4), doos.setValue(SchuildoosBlock.OPEN, true), 3);
        if (level.getBlockEntity(o.offset(9, 1, 2)) instanceof SchuildoosBlockEntity be) {
            be.setItem(0, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
        }
        source.sendSuccess(() -> Component.literal("einde2 mikas: the stage stands at " + o.toShortString()), false);
        return 1;
    }

    private static <T extends Mob> T zet(ServerLevel level, EntityType<T> type, BlockPos at, float yaw) {
        T mob = type.create(level, EntitySpawnReason.COMMAND);
        mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0f);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        return mob;
    }

    private E2MikasCommando() {
    }
}
