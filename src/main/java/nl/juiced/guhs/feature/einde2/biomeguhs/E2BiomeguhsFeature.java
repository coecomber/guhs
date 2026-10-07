package nl.juiced.guhs.feature.einde2.biomeguhs;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.biomes.Wolken;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2, slice biomeguhs: the six tameable guh variants of the outer islands and their tricks.
 * <ul>
 *   <li>The variants themselves (GuhVariant KORSTGUH .. KOORGUH), their bone prefixes and their Guhdex pages are part of
 *       the einde2 skeleton; this slice gives them their looks, their homes and their tricks.</li>
 *   <li>{@link Biomeguhs}: which guh lives in which biome, and the wild ones that come and go around players.</li>
 *   <li>{@link Trucs}: the trick of each tamed one (rolling up, digging in, floating, glowing, finding kristalkaas,
 *       singing), all through GuhHooks and VariantGedragen.</li>
 *   <li>Op commands (testing, the autocheck): {@code /guhs einde2 biomeguhs zet <variant>} puts a tamed one next to you,
 *       {@code /guhs einde2 biomeguhs wild} asks the top-up for a wild one right now, {@code /guhs einde2 biomeguhs truc}
 *       makes your nearest biome guh do its trick now.</li>
 * </ul>
 * Resources (bones, fur, glow masks, texts, sounds, advancements, quests): tools/features/einde2_biomeguhs.py and
 * einde2_biomeguhs_tex.py. No item of this slice acts on a guh, so nothing is registered with GuhHooks.item.
 */
public final class E2BiomeguhsFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /**
     * The invisible light that follows a tamed Schimmelguh (no item: it is never in anybody's hand). Not an air block on
     * purpose: a chunk section that holds only air refuses another air block, and with it the light.
     */
    public static final DeferredBlock<SchimmelguhLichtBlock> SCHIMMELGUH_LICHT = BLOCKS.registerBlock("schimmelguh_licht", SchimmelguhLichtBlock::new,
            () -> BlockBehaviour.Properties.of().replaceable().noCollision().noLootTable().noOcclusion()
                    .lightLevel(s -> SchimmelguhLichtBlock.LICHT).pushReaction(PushReaction.DESTROY));

    public static final DeferredHolder<SoundEvent, SoundEvent> KORSTGUH_ROL = sound("einde2_biomeguhs.korstguh.rol");
    public static final DeferredHolder<SoundEvent, SoundEvent> KORSTGUH_UITROL = sound("einde2_biomeguhs.korstguh.uitrol");
    public static final DeferredHolder<SoundEvent, SoundEvent> KORSTGUH_TIK = sound("einde2_biomeguhs.korstguh.tik");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUINGUH_GRAAF = sound("einde2_biomeguhs.duinguh.graaf");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUINGUH_PLOP = sound("einde2_biomeguhs.duinguh.plop");
    public static final DeferredHolder<SoundEvent, SoundEvent> KRUIMELGUH_PLOF = sound("einde2_biomeguhs.kruimelguh.plof");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHIMMELGUH_GLOEI = sound("einde2_biomeguhs.schimmelguh.gloei");
    public static final DeferredHolder<SoundEvent, SoundEvent> KRISTALGUH_VONDST = sound("einde2_biomeguhs.kristalguh.vondst");
    public static final DeferredHolder<SoundEvent, SoundEvent> KOORGUH_ZING = sound("einde2_biomeguhs.koorguh.zing");

    /** The six, in Guhdex order. */
    public static final List<GuhVariant> GUHS = List.of(GuhVariant.KORSTGUH, GuhVariant.DUINGUH, GuhVariant.KRUIMELGUH, GuhVariant.SCHIMMELGUH,
            GuhVariant.KRISTALGUH, GuhVariant.KOORGUH);

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        SOUNDS.register(modBus);
        GuhHooks.tick(Trucs::tick);
        GuhHooks.doelen((guh, goals) -> goals.addGoal(6, new KaasgatDutje(guh)));
        VariantGedragen.zet(GuhVariant.KORSTGUH, new Korstbal());
        VariantGedragen.zet(GuhVariant.DUINGUH, new Duingraver());
        VariantGedragen.zet(GuhVariant.KRUIMELGUH, new Kruimelzweef());
        Wolken.looptErop(Kruimelzweef::looptOpWolken);
        NeoForge.EVENT_BUS.addListener(Biomeguhs::onLevelTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Korstbal::onTik);
        NeoForge.EVENT_BUS.addListener(E2BiomeguhsFeature::commandos);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private static void commandos(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("biomeguhs")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("zet").then(Commands.argument("variant", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(GUHS.stream().map(GuhVariant::id), b))
                        .executes(c -> zet(c.getSource(), StringArgumentType.getString(c, "variant")))))
                .then(Commands.literal("wild").executes(c -> wild(c.getSource())))
                .then(Commands.literal("truc").executes(c -> truc(c.getSource()))))));
    }

    /** The nearest biome guh of the player who asks does its trick now (the ones that wait for a moment of their own). */
    private static int truc(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        GuhEntity guh = player.level().getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(16),
                g -> g.getVariant().isBuitenGuh() && g.isOwnedBy(player)).stream().min(java.util.Comparator.comparingDouble(g -> g.distanceToSqr(player))).orElse(null);
        if (guh == null) {
            source.sendFailure(Component.translatable("gui.guhs.einde2_biomeguhs.commando.geen"));
            return 0;
        }
        switch (guh.getVariant()) {
            case KORSTGUH -> Korstbal.rolOp(guh, 200);
            case DUINGUH -> Duingraver.graafIn(guh);
            case KRISTALGUH -> {
                guh.getPersistentData().remove(Kristalneus.DAG);
                Kristalneus.vind(guh);
            }
            default -> {
            }
        }
        source.sendSuccess(() -> Component.translatable("gui.guhs.einde2_biomeguhs.commando.truc", guh.getDisplayName()), false);
        return 1;
    }

    /** A tamed guh of this variant next to the player who asks. */
    private static int zet(CommandSourceStack source, String id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        GuhVariant variant = GuhVariant.byId(id);
        if (!variant.isBuitenGuh()) {
            source.sendFailure(Component.translatable("gui.guhs.einde2_biomeguhs.commando.niets"));
            return 0;
        }
        ServerLevel level = player.level();
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guh == null) {
            return 0;
        }
        guh.snapTo(player.getX() + 1.5, player.getY(), player.getZ(), player.getYRot() + 180f, 0f);
        guh.finalizeSpawn(level, level.getCurrentDifficultyAt(guh.blockPosition()), EntitySpawnReason.TRIGGERED, null);
        guh.setVariant(variant);
        guh.setGuhScale(1f);
        level.addFreshEntity(guh);
        guh.tame(player);
        source.sendSuccess(() -> Component.translatable("gui.guhs.einde2_biomeguhs.commando.gezet", variant.displayName()), false);
        return 1;
    }

    /** Asks the top-up for a wild one around the player (a few tries: a spot is random). */
    private static int wild(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        for (int i = 0; i < 24; i++) {
            GuhEntity guh = Biomeguhs.aanvullen(player.level(), player, player.getRandom());
            if (guh != null) {
                source.sendSuccess(() -> Component.translatable("gui.guhs.einde2_biomeguhs.commando.wild", guh.getVariant().displayName()), false);
                return 1;
            }
        }
        source.sendFailure(Component.translatable("gui.guhs.einde2_biomeguhs.commando.niets"));
        return 0;
    }

    private E2BiomeguhsFeature() {
    }
}
