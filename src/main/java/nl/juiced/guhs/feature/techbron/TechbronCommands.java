package nl.juiced.guhs.feature.techbron;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guhoven.GuhovenFeature;
import nl.juiced.guhs.feature.sausdieren.SausdierenFeature;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Op commands of tech-bronnen (dev checks and AutoCheck scripts; the texts are literals, nobody but an operator reads them):
 * <ul>
 *   <li>{@code /guhs techbron mika [speler]}: has this player the flag "Aangebrande Mika verslagen"?
 *       {@code /guhs techbron mika zet|wis <speler>} sets or clears it (for the tests of whoever asks the flag).</li>
 *   <li>{@code /guhs techbron proef} (dev runs only: it builds in the world): a row with every source next to the player,
 *       with guhs, a disc, a fed Sausblubje and Guhdraad to a Guhoven, for the visual check
 *       (tools/autocheck/bbq2_tech-bronnen.txt).</li>
 * </ul>
 */
final class TechbronCommands {
    private TechbronCommands() {
    }

    static void register(RegisterCommandsEvent event) {
        var techbron = Commands.literal("techbron").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("mika")
                        .executes(c -> mika(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.literal("zet").then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "speler");
                            AangebrandeMika.zet(p);
                            return mika(c.getSource(), p);
                        })))
                        .then(Commands.literal("wis").then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "speler");
                            GuhQuests.saved(p).remove(AangebrandeMika.SLEUTEL);
                            return mika(c.getSource(), p);
                        })))
                        .then(Commands.argument("speler", EntityArgument.player())
                                .executes(c -> mika(c.getSource(), EntityArgument.getPlayer(c, "speler")))));
        if (!FMLEnvironment.isProduction()) {
            techbron.then(Commands.literal("proef").executes(c -> proef(c.getSource())));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(techbron));
    }

    private static int mika(CommandSourceStack source, ServerPlayer speler) {
        boolean ja = AangebrandeMika.verslagen(speler);
        source.sendSuccess(() -> Component.literal(speler.getGameProfile().name() + (ja ? " heeft de Aangebrande Mika verslagen" : " heeft de Aangebrande Mika nog niet verslagen")), false);
        return ja ? 1 : 0;
    }

    /** Builds the show row east of the player: cushion, dance floor, stove, kern, battery, a Guhrad with Guhtwo, Guhdraad and a Guhoven. */
    private static int proef(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer speler = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        BlockPos o = speler.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(o.offset(0, -1, -1), o.offset(26, 4, 9))) {
            level.setBlock(p, p.getY() < o.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        // the cushion with three guhs that come and lie on it
        level.setBlock(o.offset(2, 0, 2), TechbronFeature.KNUFFELGENERATOR.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 0; i < 3; i++) {
            guh(level, speler, o.offset(1 + 2 * i, 0, 7), GuhVariant.NORMAL);
        }
        // the dance floor with the mod's own disc and four dancers
        BlockPos disco = o.offset(8, 0, 1);
        level.setBlock(disco, TechbronFeature.DISCO_DYNAMO.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(disco) instanceof DiscoDynamoBlockEntity be) {
            be.zetPlaat(new ItemStack(ModItems.MUSIC_DISC_ZE_HANGEN.get()));
        }
        for (int i = 0; i < 4; i++) {
            guh(level, speler, o.offset(7 + i, 0, 7), GuhVariant.NORMAL);
        }
        // the stove with a fed Sausblubje, the kern, a half-full battery
        BlockPos kachel = o.offset(13, 0, 3);
        level.setBlock(kachel, TechbronFeature.BLUBKACHELTJE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(kachel) instanceof BlubkacheltjeBlockEntity be) {
            be.voer(new ItemStack(ModItems.KAAS_KNABBELS.get(), 4), true);
            be.zetBlubje(new ItemStack(SausdierenFeature.SAUSBLUBJE_POTJE.get()));
        }
        level.setBlock(o.offset(15, 0, 3), TechbronFeature.GLOEISTERKERN.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos batterij = o.offset(17, 0, 3);
        level.setBlock(batterij, TechbronFeature.KNABBELBATTERIJ.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(batterij) instanceof KnabbelbatterijBlock.Kern be) {
            be.zetInhoud(VadsGetallen.BATTERIJ / 2);
        }
        // a Guhrad with Guhtwo floating in it
        BlockPos rad = o.offset(21, 0, 3);
        BlockState wiel = ModBlocks.GUH_WHEEL.get().defaultBlockState();
        level.setBlock(rad, wiel, Block.UPDATE_ALL);
        wiel.getBlock().setPlacedBy(level, rad, wiel, speler, ItemStack.EMPTY);
        if (level.getBlockEntity(rad) instanceof GuhWheelBlockEntity be) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Variant", GuhVariant.MEWTWO.id());
            be.insert(tag);
            level.setBlock(rad, wiel.setValue(GuhWheelBlock.RUNNING, true), Block.UPDATE_ALL);
        }
        // Guhdraad along the back of everything, to a Guhoven
        for (int x = 2; x <= 23; x++) {
            level.setBlock(o.offset(x, 0, 4), ModBlocks.GUH_WIRE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(o.offset(24, 0, 4), GuhovenFeature.GUH_OVEN.get().defaultBlockState(), Block.UPDATE_ALL);
        source.sendSuccess(() -> Component.literal("Proefopstelling van de bronnen gebouwd"), false);
        return 1;
    }

    private static void guh(ServerLevel level, ServerPlayer eigenaar, BlockPos plek, GuhVariant variant) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.COMMAND);
        if (guh == null) {
            return;
        }
        guh.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 180f, 0f);
        guh.setVariant(variant);
        guh.tame(eigenaar);
        guh.setWandering(false);   // "blijf hier": it stays at the source when its owner walks away
        level.addFreshEntity(guh);
    }
}
