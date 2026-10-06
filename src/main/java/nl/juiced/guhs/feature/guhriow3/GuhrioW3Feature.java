package nl.juiced.guhs.feature.guhriow3;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (guhrio-w3): world 3 of Super Guhrio, "de burcht": the levels 3-1 and 3-2 (ridden on Guhshi: turning grill spits
 * over the sauce, Plof-Mika's, Vuurpeper puzzles) and the duel with the Grote Nether-Mika.
 * <ul>
 *     <li>The levels and the duel arena are built by tools/features/guhrio_w3.py with the engine's lane builder
 *     ({@code LEVELS}, {@code DUEL}); the engine ({@code feature/guhrio}) puts them into the castle.</li>
 *     <li>{@link GroteNetherMikaEntity}: the boss and his three rounds; {@link KooltjeEntity}: the slow coals he throws;
 *     {@link GuhrioW3Blocks}: his spot, the lever, Guhshi's hitching post and the Vuurpeper bush; {@link #BRUG}: the grate
 *     the duel's bridge is made of.</li>
 *     <li>{@link #EINDE}: the end scene (Prinses Perzikguh and the cake, {@link TaartEntity}), {@link #KAART_DUEL}: the
 *     narrator card a player reads the first time they walk into the arena.</li>
 *     <li>After the flagpole of 3-2 Pad-guh says his line once more: the princess is in another part of the castle.</li>
 * </ul>
 * Everything is per player (the engine's sessions) except the boss himself, who is shared by whoever is in the arena: they
 * beat him together and all of them win. Nothing here hurts anybody.
 */
public final class GuhrioW3Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** The grate of the duel's bridge (a plain block). */
    public static final DeferredBlock<Block> BRUG = BLOCKS.registerSimpleBlock("guhriow3_brug",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.CHAIN));
    public static final DeferredBlock<GuhrioW3Blocks.BaasPlek> BAAS_PLEK = BLOCKS.registerBlock("guhriow3_baas_plek", GuhrioW3Blocks.BaasPlek::new,
            () -> BlockBehaviour.Properties.of().noCollision().noLootTable().strength(-1f, 3600000f).noOcclusion()
                    .isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<GuhrioW3Blocks.HendelBlok> HENDEL = BLOCKS.registerBlock("guhriow3_hendel", GuhrioW3Blocks.HendelBlok::new,
            () -> los(MapColor.COLOR_RED).sound(SoundType.METAL).lightLevel(s -> 6));
    public static final DeferredBlock<GuhrioW3Blocks.ParkeerPaal> PARKEERPAAL = BLOCKS.registerBlock("guhriow3_parkeerpaal",
            GuhrioW3Blocks.ParkeerPaal::new, () -> los(MapColor.WOOD).sound(SoundType.WOOD));
    public static final DeferredBlock<GuhrioW3Blocks.Peperstruik> PEPERSTRUIK = BLOCKS.registerBlock("guhriow3_peperstruik",
            GuhrioW3Blocks.Peperstruik::new, () -> los(MapColor.PLANT).sound(SoundType.SWEET_BERRY_BUSH).lightLevel(s -> 5));

    public static final List<DeferredItem<BlockItem>> BLOK_ITEMS = List.of(ITEMS.registerSimpleBlockItem(BRUG), ITEMS.registerSimpleBlockItem(BAAS_PLEK),
            ITEMS.registerSimpleBlockItem(HENDEL), ITEMS.registerSimpleBlockItem(PARKEERPAAL), ITEMS.registerSimpleBlockItem(PEPERSTRUIK));

    /** The boss (the fixed id of CONTRACT_130 section 7). */
    public static final DeferredHolder<EntityType<?>, EntityType<GroteNetherMikaEntity>> GROTE_NETHER_MIKA = wezen("grote_nether_mika",
            GroteNetherMikaEntity::new, GroteNetherMikaEntity.BREED, GroteNetherMikaEntity.HOOG);
    public static final DeferredHolder<EntityType<?>, EntityType<KooltjeEntity>> KOOLTJE = wezen("guhriow3_kooltje", KooltjeEntity::new, 0.6f, 0.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<TaartEntity>> TAART = wezen("guhriow3_taart", TaartEntity::new, 0.9f, 0.6f);

    /** The level ids of this world. */
    public static final String LEVEL_3_1 = "kasteel_3_1", LEVEL_3_2 = "kasteel_3_2";
    /** The narrator card of the duel (shown the first time a player walks into the arena) and the end scene. */
    public static final String KAART_DUEL = "guhriow3_duel";
    public static final Cutscene EINDE = einde();

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> wezen(String id, EntityType.EntityFactory<T> maker, float breed,
                                                                                         float hoog) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.of(maker, MobCategory.MISC).sized(breed, hoog).clientTrackingRange(8)
                .updateInterval(1).noSave().fireImmune().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    /** A piece you walk through. */
    private static BlockBehaviour.Properties los(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).noCollision().noOcclusion().strength(0.5f, 6f)
                .isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK);
    }

    /** A spot of the duel lane in the scene's frame (the start block is cell 2 of the lane; d: towards the camera). */
    private static Vec3 plek(double s, double y, double d) {
        return new Vec3(s - 2 + 0.5, y, 0.5 + d);
    }

    /**
     * The end scene: the Grote Nether-Mika sits on the far ledge, wet and sulking; Prinses Perzikguh comes out of her door
     * with the cake; he only wanted a piece, and he gets the biggest one. Written against the arena of
     * tools/features/guhrio_w3.py (the door of the tower room is painted behind cells 32-33, he sits at 30.3).
     */
    private static Cutscene einde() {
        String mika = "mika", perzik = "perzik", taart = "taart";
        return Cutscene.maak("guhriow3_einde").duur(610).bij("guhrio").kaart(KAART_DUEL).verbergEcht(48)
                .speler(plek(14, 0, 0), -90)
                .acteur(mika, GROTE_NETHER_MIKA, plek(30.3, 0, 0), 45, tag -> tag.putInt("Stand", GroteNetherMikaEntity.MOKT))
                .npc(perzik, GuhNpcEntity.Kind.PERZIKGUH, plek(33.2, 0, -1.1), 45)
                .acteur(taart, TAART, plek(32.3, 0, -1.1), 0)
                // the bridge is back: the player walks over it
                .camera(0, plek(21, 3.4, 10.5), plek(28, 1.2, 0)).camera(105, plek(25.5, 2.6, 8.5), plek(30.5, 1.4, 0))
                .loop(Cutscene.SPELER, 10, 100, plek(26.5, 0, 0))
                .geluid(22, ModSounds.MIKA_HURT, 0.9f, 0.5f)
                .zeg(25, mika, "mok", 60)
                // the door: the princess and the cake
                .cameraKnip(106, plek(29.5, 2.2, 6.5), plek(32.4, 0.9, 0)).camera(170, plek(29.8, 2.0, 6.0), plek(32.2, 0.9, 0))
                .loop(perzik, 108, 130, plek(33.2, 0, 0)).loop(taart, 108, 130, plek(32.3, 0, 0))
                .loop(perzik, 132, 150, plek(32.9, 0, 0)).loop(taart, 132, 150, plek(32.0, 0, 0))
                .geluid(112, ModSounds.GUH_HAPPY, 0.9f, 1.3f)
                .zeg(115, perzik, "herrie", 55)
                .kijk(Cutscene.SPELER, 120, plek(33, 1, 0))
                .zeg(178, perzik, "redden", 82)
                // he only wanted a piece of cake
                .cameraKnip(265, plek(27.4, 2.1, 5.2), plek(30.3, 1.7, 0)).camera(350, plek(27.9, 2.0, 4.8), plek(30.3, 1.7, 0))
                .animatie(mika, 262, "op")
                .zeg(268, mika, "stukje", 84)
                .cameraKnip(356, plek(29.3, 2.5, 7.4), plek(31.3, 1.2, 0)).camera(520, plek(29.6, 2.3, 6.8), plek(31.3, 1.2, 0))
                .zeg(360, perzik, "vragen", 76)
                .loop(taart, 384, 412, plek(31.55, 0, 0))
                .animatie(taart, 426, "stukje")
                .geluid(426, () -> SoundEvents.GENERIC_EAT.value(), 0.9f, 0.8f)
                .animatie(mika, 430, "blij")
                .deeltjes(432, ParticleTypes.HEART, plek(30.3, 2.9, 0), 8, 0.5)
                .geluid(434, ModSounds.GUH_HAPPY, 1.0f, 0.6f)
                .zeg(440, mika, "voor_mij", 76)
                // on to the tower room
                .cameraKnip(521, plek(26, 3.2, 9.5), plek(30.8, 1.3, 0)).camera(609, plek(24, 4.2, 10.5), plek(30.5, 1.3, 0))
                .zeg(524, perzik, "torenkamer", 62)
                .deeltjes(560, ParticleTypes.HEART, plek(32.9, 1.4, 0), 5, 0.3)
                .zwart(586, 610)
                .registreer();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        Verteller.registreer(KAART_DUEL, 4, GuhrioKasteel.LIJN.id());
        GuhrioSpel.BIJ_KLAAR.add((player, sessie, ticks, record) -> naMast(player, sessie.level().level().id()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> DuelPlan.vergeet());
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
            if (event.getPlayer() == null) {
                DuelPlan.vergeet();
            }
        });
        NeoForge.EVENT_BUS.addListener(GuhrioW3Feature::commandos);
    }

    /** A flagpole of this world: the big vadsmunten of the burcht, and after 3-2 Pad-guh's running gag. */
    private static void naMast(ServerPlayer player, String level) {
        if (!LEVEL_3_1.equals(level) && !LEVEL_3_2.equals(level)) {
            return;
        }
        vadsmunten(player);
        if (LEVEL_3_2.equals(level)) {
            player.sendSystemMessage(Component.translatable("gui.guhs.guhriow3.padguh").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** All six big vadsmunten of world 3: the hidden advancement of its FTB quest. */
    static void vadsmunten(ServerPlayer player) {
        if (GuhrioKasteel.vadsmunten(player, LEVEL_3_1) == 7 && GuhrioKasteel.vadsmunten(player, LEVEL_3_2) == 7) {
            GuhAdvancements.grant(player, "guhrio_w3_vadsmunten");
        }
    }

    /**
     * The first time a player is in the duel arena: the narrator card (the level waits for whoever reads). True when the
     * card is shown now.
     */
    static boolean kaart(ServerPlayer player) {
        return !Verteller.gezien(player, KAART_DUEL) && !Cutscenes.bezig(player) && Verteller.toon(player, KAART_DUEL, null);
    }

    /** {@code /guhs guhriow3 ronde <1-4> | win} (ops, literal texts: for the AutoCheck script and for trying the duel). */
    private static void commandos(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("guhriow3").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ronde").then(Commands.argument("n", IntegerArgumentType.integer(1, 4)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
                    GroteNetherMikaEntity mika = s == null ? null : GuhrioW3Blocks.baas(s);
                    if (mika == null) {
                        c.getSource().sendFailure(Component.literal("Je staat niet in de duelarena (geen Grote Nether-Mika in dit level)."));
                        return 0;
                    }
                    int n = IntegerArgumentType.getInteger(c, "n");
                    mika.devRonde(p.level(), n);
                    c.getSource().sendSuccess(() -> Component.literal("Grote Nether-Mika: ronde " + n + " (stand " + mika.stand() + ")"), false);
                    return 1;
                })))
                .then(Commands.literal("win").executes(c -> {
                    GroteNetherMikaEntity.gewonnen(c.getSource().getPlayerOrException());
                    return 1;
                }))));
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : BLOK_ITEMS) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private GuhrioW3Feature() {
    }
}
