package nl.juiced.guhs.feature.guhpixel.among;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhpixel.Aandenken;
import nl.juiced.guhs.feature.guhpixel.ArenaSoort;
import nl.juiced.guhs.feature.guhpixel.Bouwer;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.GidsSectie;
import nl.juiced.guhs.feature.guhpixel.Grap;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.LobbyNpcs;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxZelftest;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.SpelSoort;
import nl.juiced.guhs.feature.guhpixel.Winkel;
import nl.juiced.guhs.feature.guhpixel.among.model.Balans;
import nl.juiced.guhs.feature.guhpixel.among.model.Schip;
import nl.juiced.guhs.feature.guhpixel.among.model.Simulatie;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.guhpixel.blok.MuurDecoBlock;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * Guhpixel slice "among": Among Guhs, the one real, repeatable minigame of Guhpixel, and the one-time oefenrondje before it.
 * <ul>
 *   <li>{@code among.model}: the rules as plain Java (the ship table, roles, tasks, pushing asleep, reporting, the button,
 *   vents, the three sabotages, meetings with the statement system, votes, droomguhs, who wins), the heads of the guh NPCs
 *   (crew and Mika) and the headless {@link Simulatie};</li>
 *   <li>{@link AmongSessie}: one round on its own ship "De Vadsvaarder" (arena template guhpixel/among_vadsvaarder), any number at
 *   the same time; {@link AmongGuhEntity}: the guh NPC in a coloured space suit;</li>
 *   <li>{@link AmongWachtrij}: the queue at the Kapitein-guh with "klaar" and the difficulty;</li>
 *   <li>{@link TaakSoorten} and {@link Taken}: the eight task mini-games and the two repair panels (what a panel asks, and
 *   whether the answer is right; the screens are in among.client);</li>
 *   <li>{@link OefenSessie}: the oefenrondje, the parody round (joke game "among") that unlocks the real queue; its keepsake is
 *   the SUS-stickerbord;</li>
 *   <li>{@link AmongBeloning}: muntjes with the daily cap, the personal numbers (the Logboek-guh, the Guhdex, the titles);</li>
 *   <li>the shop: eight Ruimtepakjes and six hoedjes (clothes for your guhs) and the three ship things as decoration that
 *   does something at home ({@link AmongThuis}).</li>
 * </ul>
 * Nothing here hurts anybody: Mikas only push asleep, a sleeper goes on as a droomguh, a lost round costs nothing.
 */
public final class AmongSlice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_BEGIN = geluid("among.begin");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_DUW = geluid("among.duw");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_VERGADERING = geluid("among.vergadering");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_STEM = geluid("among.stem");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_WEGGESTEMD = geluid("among.weggestemd");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_SABOTAGE = geluid("among.sabotage");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_ALARM = geluid("among.alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_TAAK = geluid("among.taak");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_LUIK = geluid("among.luik");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_PANEEL = geluid("among.paneel");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_SNURK = geluid("among.snurk");

    /** The task panel: sits in the ship's walls; at home a blinking decoration. */
    public static final DeferredBlock<AmongBlokken.SchipBlock> TAAKPANEEL = BLOCKS.registerBlock("among_taakpaneel",
            p -> new AmongBlokken.SchipBlock(p, Block.box(0, 0, 0, 16, 16, 16), AmongBlokken.TAAKPANEEL, GELUID_PANEEL::get),
            () -> DecoBlock.props().lightLevel(s -> 7));
    /** The emergency button on the meeting table. */
    public static final DeferredBlock<AmongBlokken.SchipBlock> NOODKNOP = BLOCKS.registerBlock("among_noodknop",
            p -> new AmongBlokken.SchipBlock(p, Block.box(3, 0, 3, 13, 8, 13), AmongBlokken.NOODKNOP, GELUID_VERGADERING::get),
            () -> DecoBlock.props().lightLevel(s -> 5));
    /** A vent hatch in the floor. */
    public static final DeferredBlock<AmongBlokken.SchipBlock> VENTILATIELUIK = BLOCKS.registerBlock("among_ventilatieluik",
            p -> new AmongBlokken.SchipBlock(p, Block.box(1, 0, 1, 15, 2, 15), AmongBlokken.LUIK, GELUID_LUIK::get), () -> DecoBlock.props().randomTicks());
    /** The keepsake of the oefenrondje: a board of SUS stickers on the wall. */
    public static final DeferredBlock<AmongBlokken.SusBord> SUS_BORD = BLOCKS.registerBlock("among_sus_bord",
            p -> new AmongBlokken.SusBord(p, Block.box(1, 2, 14, 15, 14, 16)), () -> MuurDecoBlock.props());

    public static final DeferredItem<LoreBlockItem> TAAKPANEEL_ITEM = blokItem("among_taakpaneel", TAAKPANEEL);
    public static final DeferredItem<LoreBlockItem> NOODKNOP_ITEM = blokItem("among_noodknop", NOODKNOP);
    public static final DeferredItem<LoreBlockItem> VENTILATIELUIK_ITEM = blokItem("among_ventilatieluik", VENTILATIELUIK);
    public static final DeferredItem<LoreBlockItem> SUS_BORD_ITEM = blokItem("among_sus_bord", SUS_BORD);
    /** The Mika's pillow: right-click pushes whoever stands in front of you asleep. */
    public static final DeferredItem<AmongBlokken.SpelItem> KUSSEN = ITEMS.registerItem("among_kussen",
            p -> new AmongBlokken.SpelItem(p, AmongBlokken.SpelItem.Soort.KUSSEN), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<AmongBlokken.SpelItem> SABOTEERKAART = ITEMS.registerItem("among_saboteerkaart",
            p -> new AmongBlokken.SpelItem(p, AmongBlokken.SpelItem.Soort.SABOTEERKAART), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<AmongBlokken.SpelItem> STEMBRIEFJE = ITEMS.registerItem("among_stembriefje",
            p -> new AmongBlokken.SpelItem(p, AmongBlokken.SpelItem.Soort.STEMBRIEFJE), () -> new Item.Properties().stacksTo(1));

    /** A participant as a guh in a space suit (never saved, never a real guh). */
    public static final DeferredHolder<EntityType<?>, EntityType<AmongGuhEntity>> AMONG_GUH = ENTITY_TYPES.register("among_guh",
            () -> EntityType.Builder.of(AmongGuhEntity::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f).clientTrackingRange(10).updateInterval(1)
                    .noSave().noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("among_guh"))));

    public static final Vec3i MAAT = new Vec3i(75, 9, 51);
    public static final ArenaSoort ARENA = new ArenaSoort("among", Guhs.id("guhpixel/among_vadsvaarder"), MAAT, new Vec3(37.5, 2.0, 12.5), 180f, false,
            AmongSessie::herstelArena);
    public static final SpelSoort SPEL = new SpelSoort("among", ARENA, 1, AmongWachtrij.MAX_LASTIG, LobbyPlek.SPEL_AMONG, AmongSessie::new);

    /** The shop's clothes: the eight Ruimtepakjes (body) and the six hoedjes (head). */
    public static final GuhClothes[] PAKJES = {GuhClothes.AMONG_RUIMTEPAKJE_ROOD, GuhClothes.AMONG_RUIMTEPAKJE_BLAUW, GuhClothes.AMONG_RUIMTEPAKJE_GROEN,
            GuhClothes.AMONG_RUIMTEPAKJE_GEEL, GuhClothes.AMONG_RUIMTEPAKJE_ROZE, GuhClothes.AMONG_RUIMTEPAKJE_ORANJE, GuhClothes.AMONG_RUIMTEPAKJE_PAARS,
            GuhClothes.AMONG_RUIMTEPAKJE_WIT};
    public static final GuhClothes[] HOEDJES = {GuhClothes.AMONG_HOEDJE_PLANTJE, GuhClothes.AMONG_HOEDJE_EI, GuhClothes.AMONG_HOEDJE_WC_ROL,
            GuhClothes.AMONG_HOEDJE_KAASPUNT, GuhClothes.AMONG_HOEDJE_BRIEFJE, GuhClothes.AMONG_HOEDJE_KNABBEL};
    public static final int PRIJS_PAKJE = 30, PRIJS_HOEDJE = 25, PRIJS_NOODKNOP = 60, PRIJS_LUIK = 40, PRIJS_PANEEL = 40;
    public static final String BRON = "among_winkel";

    private static final int SPELEN = 1, UITLEG = 2, OEFENEN = 3, KWIJT = 4;

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    private static DeferredItem<LoreBlockItem> blokItem(String id, DeferredBlock<? extends Block> blok) {
        return ITEMS.registerItem(id, p -> new LoreBlockItem(blok.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    /** The Kapitein-guh: the queue of the real game, and the rules. */
    private static final NpcRole KAPITEIN = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            if (!AmongWachtrij.magSpelen(player)) {
                // the oefenrondje first: it unlocks the real game
                Praat.open(player, npc, null, "quest.guhs.among.kapitein.eerst_oefenen", new Object[]{},
                        new Praat.Optie(OEFENEN, "quest.guhs.among.kapitein.optie.oefenen"), new Praat.Optie(UITLEG, "quest.guhs.among.kapitein.optie.uitleg"));
                return;
            }
            if (AmongWachtrij.inRij(player)) {
                AmongWachtrij.erbij(player);
                return;
            }
            Praat.open(player, npc, null, "quest.guhs.among.kapitein.hallo", new Object[]{},
                    new Praat.Optie(SPELEN, "quest.guhs.among.kapitein.optie.spelen"), new Praat.Optie(UITLEG, "quest.guhs.among.kapitein.optie.uitleg"),
                    new Praat.Optie(OEFENEN, "quest.guhs.among.kapitein.optie.nog_eens"), new Praat.Optie(KWIJT, "quest.guhs.among.kapitein.optie.kwijt"));
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
            if (optie == SPELEN) {
                AmongWachtrij.erbij(player);
            } else if (optie == OEFENEN) {
                Praat.sluit(player);
                AmongWachtrij.weg(player);
                Sessies.start(OefenSessie.SPEL, java.util.List.of(player), new net.minecraft.nbt.CompoundTag());
            } else if (optie == KWIJT) {
                if (Grappen.isKlaar(player, OefenSessie.GRAP) && Aandenken.opnieuw(player, SUS_BORD_ITEM.get())) {
                    Praat.open(player, npc, null, "quest.guhs.among.kapitein.kwijt", new Object[]{});
                } else {
                    Praat.sluit(player);
                }
            } else if (optie == UITLEG) {
                Praat.open(player, npc, null, "quest.guhs.among.kapitein.uitleg", new Object[]{AmongBeloning.WINST, AmongBeloning.VERLIES,
                        AmongBeloning.PER_TAAK, AmongBeloning.DAG_MAX}, new Praat.Optie(SPELEN, "quest.guhs.among.kapitein.optie.spelen"));
            }
        }
    };

    /** The Logboek-guh keeps the personal stats board: the numbers of whoever asks, on a screen of their own. */
    private static final NpcRole LOGBOEK = (npc, player) -> ModNetworking.sendTo(player,
            new AmongPayloads.Scherm(AmongPayloads.CIJFERS, AmongBeloning.cijfersTag(player)));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(AMONG_GUH.get(), GuhEntity.createAttributes().build()));
        Sessies.registreer(SPEL);
        Sessies.registreer(OefenSessie.SPEL);
        Grappen.registreer(new Grap(OefenSessie.GRAP, OefenSessie.STAPPEN, GuhNpcEntity.Kind.AMONG_KAPITEIN,
                p -> Aandenken.item(p, new ItemStack(SUS_BORD_ITEM.get()))));
        winkel();
        NpcRollen.zet(GuhNpcEntity.Kind.AMONG_KAPITEIN, KAPITEIN);
        NpcRollen.zet(GuhNpcEntity.Kind.AMONG_LOGBOEKGUH, LOGBOEK);
        LobbyNpcs.registreer(LobbyPlek.SPEL_AMONG, GuhNpcEntity.Kind.AMONG_KAPITEIN, null, server -> LobbyNpcs.spelersRegel("among", 8));
        LobbyNpcs.registreer(LobbyPlek.BORD_AMONG, GuhNpcEntity.Kind.AMONG_LOGBOEKGUH,
                Component.translatable("gui.guhs.among.logboek.kop").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                server -> Component.translatable("gui.guhs.among.logboek.regel").withStyle(ChatFormatting.GRAY));
        GidsBlad.registreer(GIDS);
        NeoForge.EVENT_BUS.addListener(AmongCommando::register);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, true, AmongSlice::opBlok);
        NeoForge.EVENT_BUS.addListener(AmongSlice::opReis);
        NeoForge.EVENT_BUS.addListener(AmongSlice::opChat);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            AmongWachtrij.tick(event.getServer());
            AmongThuis.tick(event.getServer());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            AmongWachtrij.leeg();
            AmongThuis.leeg();
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                AmongWachtrij.weg(p);
                AmongThuis.weg(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                AmongSessie.herstelModus(p);
            }
        });
        PxZelftest.registreer("among", AmongSlice::zelftest);
    }

    /** The shop: Ruimtepakjes and hoedjes (outfits: once each), and the three ship things (as often as you like). */
    private static void winkel() {
        Component eerstSpelen = Component.translatable("gui.guhs.among.winkel.eis");
        java.util.function.Predicate<ServerPlayer> gespeeld = p -> AmongBeloning.cijfer(p, AmongBeloning.RONDES) >= 1;
        for (GuhClothes c : PAKJES) {
            KledingBronnen.bron(c, BRON);
            kleding(c, "among_pakjes", PRIJS_PAKJE, c == GuhClothes.AMONG_RUIMTEPAKJE_ROOD ? "gui.guhs.among.winkel.pakje.rood" : "gui.guhs.among.winkel.pakje",
                    gespeeld, eerstSpelen);
        }
        for (GuhClothes c : HOEDJES) {
            KledingBronnen.bron(c, BRON);
            kleding(c, "among_hoedjes", PRIJS_HOEDJE, "gui.guhs.among.winkel.hoedje." + c.id().substring("among_hoedje_".length()), gespeeld, eerstSpelen);
        }
        deco("among_noodknop", NOODKNOP_ITEM, PRIJS_NOODKNOP, gespeeld, eerstSpelen);
        deco("among_ventilatieluik", VENTILATIELUIK_ITEM, PRIJS_LUIK, gespeeld, eerstSpelen);
        deco("among_taakpaneel", TAAKPANEEL_ITEM, PRIJS_PANEEL, gespeeld, eerstSpelen);
    }

    private static void kleding(GuhClothes c, String groep, int prijs, String uitleg, java.util.function.Predicate<ServerPlayer> eis, Component eisTekst) {
        Winkel.aanbod(c.id()).groep(groep).icoon(() -> new ItemStack(ModItems.clothingItem(c)))
                .naam(Component.translatable("item.guhs." + c.id())).uitleg(Component.translatable(uitleg))
                .prijs(prijs).max(1).eis(eis, eisTekst).lever((p, a) -> Winkel.ontgrendel(p, c)).registreer();
    }

    private static void deco(String id, DeferredItem<LoreBlockItem> item, int prijs, java.util.function.Predicate<ServerPlayer> eis, Component eisTekst) {
        Winkel.aanbod(id).groep("among_deco").icoon(() -> new ItemStack(item.get()))
                .naam(Component.translatable("block.guhs." + id)).uitleg(Component.translatable("gui.guhs.among.winkel." + id))
                .prijs(prijs).max(0).eis(eis, eisTekst).lever((p, a) -> Winkel.geef(p, new ItemStack(item.get()))).registreer();
    }

    /** How many of these clothes the player has. */
    static int heeft(ServerPlayer p, GuhClothes[] stukken) {
        int n = 0;
        for (GuhClothes c : stukken) {
            n += KledingUnlocks.heeft(p, c) ? 1 : 0;
        }
        return n;
    }

    public static void payloads(PayloadRegistrar registrar) {
        AmongPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(TAAKPANEEL_ITEM.get()));
        output.accept(new ItemStack(NOODKNOP_ITEM.get()));
        output.accept(new ItemStack(VENTILATIELUIK_ITEM.get()));
        output.accept(new ItemStack(SUS_BORD_ITEM.get()));
        output.accept(new ItemStack(KUSSEN.get()));
        output.accept(new ItemStack(SABOTEERKAART.get()));
        output.accept(new ItemStack(STEMBRIEFJE.get()));
    }

    /** A right-click on a block by a participant of a round: panels, the button, vents (also for droomguhs, who are spectators). */
    private static void opBlok(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if ((Sessies.van(p) instanceof AmongSessie s && s.gebruikBlok(p, event.getPos()))
                || (Sessies.van(p) instanceof OefenSessie oefen && oefen.gebruikBlok(p, event.getPos()))) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** A droomguh is in spectator mode: the spectator's "teleport to a player" must not carry it out of the ship's dimension. */
    private static void opReis(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && Sessies.van(p) instanceof AmongSessie s && s.isDroomguh(p) && !s.isGestopt()
                && event.getDimension() != p.level().dimension()) {
            event.setCanceled(true);
        }
    }

    /** Droomguhs only talk to other droomguhs of their round. */
    private static void opChat(ServerChatEvent event) {
        ServerPlayer p = event.getPlayer();
        if (Sessies.van(p) instanceof AmongSessie s && s.isDroomguh(p)) {
            event.setCanceled(true);
            Component regel = Component.translatable("gui.guhs.among.droomchat", p.getDisplayName(), event.getMessage()).withStyle(ChatFormatting.GRAY);
            for (ServerPlayer droom : s.droomguhs()) {
                droom.sendSystemMessage(regel);
            }
        }
    }

    private static final GidsSectie GIDS = new GidsSectie() {
        @Override
        public String id() {
            return "among";
        }

        @Override
        public int volgorde() {
            return 40;
        }

        @Override
        public boolean zichtbaar(ServerPlayer p) {
            return true;
        }

        @Override
        public void vul(ServerPlayer p, Bouwer b) {
            b.kop(Component.translatable("gui.guhs.among.gids.kop"));
            b.grap(OefenSessie.GRAP);
            b.regel(Component.translatable("gui.guhs.among.gids.uitleg"));
            b.stat(Component.translatable("gui.guhs.among.gids.rondes"), Component.literal(String.valueOf(AmongBeloning.cijfer(p, AmongBeloning.RONDES))));
            b.stat(Component.translatable("gui.guhs.among.gids.winst_crew"), Component.literal(String.valueOf(AmongBeloning.cijfer(p, AmongBeloning.WINST_CREW))));
            b.stat(Component.translatable("gui.guhs.among.gids.winst_mika"), Component.literal(String.valueOf(AmongBeloning.cijfer(p, AmongBeloning.WINST_MIKA))));
            b.stat(Component.translatable("gui.guhs.among.gids.onterecht"), Component.literal(String.valueOf(AmongBeloning.cijfer(p, AmongBeloning.ONTERECHT))));
            b.stat(Component.translatable("gui.guhs.among.gids.taken"), Component.literal(String.valueOf(AmongBeloning.cijfer(p, AmongBeloning.TAKEN))));
            b.voortgang(Component.translatable("gui.guhs.among.gids.vandaag"), Muntjes.vandaag(p, AmongBeloning.POT), AmongBeloning.DAG_MAX);
            b.voortgang(Component.translatable("gui.guhs.among.gids.pakjes"), heeft(p, PAKJES), PAKJES.length);
            b.voortgang(Component.translatable("gui.guhs.among.gids.hoedjes"), heeft(p, HOEDJES), HOEDJES.length);
        }
    };

    /** The dev-server check: the ship table matches the template, and a handful of NPC rounds end with a winner. */
    private static void zelftest(net.minecraft.server.MinecraftServer server, net.minecraft.server.level.ServerLevel level, PxZelftest.Melder meld) {
        Schip schip;
        try {
            schip = Schip.standaard();
        } catch (RuntimeException e) {
            meld.fout("the ship table cannot be read: " + e.getMessage());
            return;
        }
        meld.check(schip.breedte == MAAT.getX() && schip.hoogte == MAAT.getY() && schip.diepte == MAAT.getZ(), "ship table size = arena size");
        StructureTemplate template = level.getStructureManager().get(ARENA.template()).orElse(null);
        if (template == null) {
            meld.fout("template " + ARENA.template() + " is missing");
            return;
        }
        meld.check(template.getSize().equals(MAAT), "template size " + template.getSize());
        meld.check(blokken(template, TAAKPANEEL.get()) == schip.panelen.size(), schip.panelen.size() + " panels in the template");
        meld.check(blokken(template, VENTILATIELUIK.get()) == schip.luiken.size(), schip.luiken.size() + " vents in the template");
        meld.check(blokken(template, NOODKNOP.get()) == 1, "one emergency button in the template");
        int aanbod = 0;
        for (Winkel.Aanbod a : Winkel.alle()) {
            aanbod += a.groep().startsWith("among_") ? 1 : 0;
        }
        meld.check(aanbod == PAKJES.length + HOEDJES.length + 3, aanbod + " Among Guhs offers in the shop");
        meld.check(Grappen.van(OefenSessie.GRAP) != null && Sessies.soort(OefenSessie.ID) != null, "the oefenrondje is a joke game");
        for (String soort : Taken.SOORTEN) {
            java.util.Random rng = new java.util.Random(7);
            net.minecraft.nbt.CompoundTag opgave = TaakSoorten.van(soort).opgave(rng, 0);
            meld.check(TaakSoorten.van(soort).geldig(opgave, Taken.oplossing(soort, opgave)), "task " + soort + " can be solved");
        }
        Simulatie.Uitkomst u = Simulatie.draai(schip, Balans.normaal(), 40, 11);
        meld.check(u.onbeslist() == 0, u.tekst("40 NPC rounds"));
    }

    static int blokken(StructureTemplate template, Block blok) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), blok).size();
    }

    /** Is this state one of the ship's own blocks (tests)? */
    static boolean schipBlok(BlockState state) {
        return state.is(TAAKPANEEL.get()) || state.is(NOODKNOP.get()) || state.is(VENTILATIELUIK.get()) || state.is(Blocks.PINK_WOOL)
                || state.is(SUS_BORD.get());
    }

    private AmongSlice() {
    }
}
