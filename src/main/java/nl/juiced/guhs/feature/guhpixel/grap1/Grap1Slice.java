package nl.juiced.guhs.feature.guhpixel.grap1;

import java.util.function.Consumer;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpixel.Aandenken;
import nl.juiced.guhs.feature.guhpixel.Arena;
import nl.juiced.guhs.feature.guhpixel.Arenas;
import nl.juiced.guhs.feature.guhpixel.Bouwer;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.GidsSectie;
import nl.juiced.guhs.feature.guhpixel.Grap;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.LobbyNpcs;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.PxZelftest;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.SpelSoort;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * Guhpixel slice "grap1": the three joke games Skyblok, Bedwars and Vadsnite (DESIGN_PX section 2). Each is a mini
 * questline of four steps with a punchline, played alone in an arena of your own, started at its lobby NPC; the first
 * time it pays 100 muntjes (the kern's {@link Grappen}) and a small keepsake, after that it can be replayed freely.
 * <ul>
 *   <li>{@link SkyblokSessie}: keepsake the decoration block Eilandje-in-een-fles ({@link #FLES});</li>
 *   <li>{@link BedwarsSessie}: keepsake the four Teamslaapmutsen (outfit unlocks), enemy teams = {@link #TEAMGUH};</li>
 *   <li>{@link VadsniteSessie}: keepsake the Parachuterugzakje (outfit unlock).</li>
 * </ul>
 * Resources: tools/features/guhpixel_grap1.py (+ _bouw, _tex, _tekst); tests: PxGrap1GameTests (batch px_grap1).
 */
public final class Grap1Slice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** The Skyblok keepsake: "the" island in a bottle. */
    public static final DeferredBlock<DecoBlock> FLES = BLOCKS.registerBlock("skyblok_fles", p -> new DecoBlock(p, Block.box(4, 0, 4, 12, 13, 12)),
            () -> DecoBlock.props());
    public static final DeferredItem<LoreBlockItem> FLES_ITEM = ITEMS.registerItem("skyblok_fles", p -> new LoreBlockItem(FLES.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());

    /** The enemy team guhs of Bedwars (never saved, never a real guh). */
    public static final DeferredHolder<EntityType<?>, EntityType<TeamGuhEntity>> TEAMGUH = ENTITY_TYPES.register("bedwars_teamguh",
            () -> EntityType.Builder.of(TeamGuhEntity::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f).clientTrackingRange(10).updateInterval(2)
                    .noSave().noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("bedwars_teamguh"))));

    public static final DeferredHolder<SoundEvent, SoundEvent> SKYBLOK_UITGESPEELD = geluid("skyblok.uitgespeeld");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEDWARS_VERDEDIGD = geluid("bedwars.verdedigd");
    public static final DeferredHolder<SoundEvent, SoundEvent> VADSNITE_SPRONG = geluid("vadsnite.sprong");
    public static final DeferredHolder<SoundEvent, SoundEvent> VADSNITE_OVERWINNING = geluid("vadsnite.overwinning");

    /** The three games, in lobby order. */
    public static final String[] GRAPPEN = {SkyblokSessie.ID, BedwarsSessie.ID, VadsniteSessie.ID};
    public static final GuhClothes[] MUTSEN = {GuhClothes.BEDWARS_SLAAPMUTS_ROOD, GuhClothes.BEDWARS_SLAAPMUTS_BLAUW, GuhClothes.BEDWARS_SLAAPMUTS_GROEN,
            GuhClothes.BEDWARS_SLAAPMUTS_GEEL};

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(TEAMGUH.get(), GuhEntity.createAttributes().build()));

        spel(SkyblokSessie.SPEL, SkyblokSessie.STAPPEN, GuhNpcEntity.Kind.SKYBLOK_GUH, LobbyPlek.SPEL_SKYBLOK, 0,
                p -> Aandenken.item(p, new ItemStack(FLES_ITEM.get())), new GrapRol(SkyblokSessie.ID, SkyblokSessie.SPEL, FLES_ITEM));
        spel(BedwarsSessie.SPEL, BedwarsSessie.STAPPEN, GuhNpcEntity.Kind.BEDWARS_GUH, LobbyPlek.SPEL_BEDWARS, 4,
                p -> Aandenken.kleding(p, MUTSEN), new GrapRol(BedwarsSessie.ID, BedwarsSessie.SPEL, null));
        spel(VadsniteSessie.SPEL, VadsniteSessie.STAPPEN, GuhNpcEntity.Kind.VADSNITE_GUH, LobbyPlek.SPEL_VADSNITE, VadsniteSessie.GUHS,
                p -> Aandenken.kleding(p, GuhClothes.VADSNITE_PARACHUTERUGZAKJE), new GrapRol(VadsniteSessie.ID, VadsniteSessie.SPEL, null));
        for (GuhClothes muts : MUTSEN) {
            KledingBronnen.bron(muts, "bedwars_aandenken");
        }
        KledingBronnen.bron(GuhClothes.VADSNITE_PARACHUTERUGZAKJE, "vadsnite_aandenken");

        NeoForge.EVENT_BUS.addListener(Dutje::onSlapen);
        NeoForge.EVENT_BUS.addListener(Dutje::onDoorslapen);
        NeoForge.EVENT_BUS.addListener(Grap1Slice::onGebruikItem);
        NeoForge.EVENT_BUS.addListener(Grap1Slice::onGebruikBlok);
        NeoForge.EVENT_BUS.addListener(Grap1Slice::commando);
        GidsBlad.registreer(new Gids());
        PxZelftest.registreer("grap1", (server, level, meld) -> {
            meld.check(LobbyNpcs.spelersRegel(SkyblokSessie.ID, 0) != null, "the three lobby NPCs are registered");
            for (SpelSoort soort : new SpelSoort[] {SkyblokSessie.SPEL, BedwarsSessie.SPEL, VadsniteSessie.SPEL}) {
                Arena a = Arenas.neem(level, soort.arena());
                if (a == null) {
                    meld.fout(soort.id() + ": no arena (template missing?)");
                    continue;
                }
                BlockPos bed = switch (soort.id()) {
                    case SkyblokSessie.ID -> SkyblokSessie.BED_VOET;
                    case BedwarsSessie.ID -> BedwarsSessie.BED_VOET;
                    default -> VadsniteSessie.BEDDEN.get(0);
                };
                BlockState s = level.getBlockState(a.wereld(bed.getX(), bed.getY(), bed.getZ()));
                Vec3 start = a.start();
                boolean vloer = !level.getBlockState(BlockPos.containing(start.x, start.y - 0.5, start.z)).isAir();
                meld.check(s.is(BlockTags.BEDS), soort.id() + ": a bed at " + bed + " (cell " + a.cel() + ")");
                meld.check(vloer, soort.id() + ": a floor under the start");
                Arenas.geefTerug(a);
            }
        });
    }

    private static void spel(SpelSoort spel, int stappen, GuhNpcEntity.Kind kind, LobbyPlek plek, int guhs, Consumer<ServerPlayer> aandenken, GrapRol rol) {
        Sessies.registreer(spel);
        Grappen.registreer(new Grap(spel.id(), stappen, kind, aandenken));
        LobbyNpcs.registreer(plek, kind, null, server -> LobbyNpcs.spelersRegel(spel.id(), guhs));
        NpcRollen.zet(kind, rol);
    }

    // --- Skyblok: nothing you hold works -----------------------------------------------------------------------------------

    private static void onGebruikItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer p && !p.getAbilities().instabuild && Sessies.van(p) instanceof SkyblokSessie s) {
            s.itemGebruik(p, event.getItemStack());
            event.setCanceled(true);
        }
    }

    private static void onGebruikBlok(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer p && !p.getAbilities().instabuild && !event.getItemStack().isEmpty()
                && Sessies.van(p) instanceof SkyblokSessie s) {
            event.setUseItem(TriState.FALSE);
            BlockState blok = event.getLevel().getBlockState(event.getPos());
            if (!blok.is(Blocks.CHEST) && !blok.is(BlockTags.BEDS) && !(blok.getBlock() instanceof DoorBlock)) {
                s.itemGebruik(p, event.getItemStack());
            }
        }
    }

    // --- Guhdex --------------------------------------------------------------------------------------------------------------

    private static final class Gids implements GidsSectie {
        @Override
        public String id() {
            return "grap1";
        }

        @Override
        public int volgorde() {
            return 20;
        }

        @Override
        public boolean zichtbaar(ServerPlayer p) {
            return true;
        }

        @Override
        public void vul(ServerPlayer p, Bouwer b) {
            int klaar = 0;
            for (String id : GRAPPEN) {
                klaar += Grappen.isKlaar(p, id) ? 1 : 0;
            }
            b.kop(Component.translatable("gui.guhs.skyblok.gids.kop"));
            b.regel(Component.translatable("gui.guhs.skyblok.gids.uitleg"));
            b.voortgang(Component.translatable("gui.guhs.skyblok.gids.voortgang"), klaar, GRAPPEN.length);
            for (String id : GRAPPEN) {
                b.grap(id);
            }
        }
    }

    // --- dev -----------------------------------------------------------------------------------------------------------------

    /** /guhs px grap1 aandenken: the three keepsakes at once (to look at them without playing). */
    private static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("px").then(Commands.literal("grap1")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("aandenken").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Aandenken.item(p, new ItemStack(FLES_ITEM.get()));
                    Aandenken.kleding(p, MUTSEN);
                    Aandenken.kleding(p, GuhClothes.VADSNITE_PARACHUTERUGZAKJE);
                    return 1;
                })))));
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(FLES_ITEM.get()));
    }

    private Grap1Slice() {
    }
}
