package nl.juiced.guhs.feature.guhpixel.grap2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpixel.Bouwer;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.GidsSectie;
import nl.juiced.guhs.feature.guhpixel.Grap;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.LobbyNpcs;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.PxZelftest;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.guhpixel.blok.MuurDecoBlock;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.taal.Tekst;

/**
 * Guhpixel slice "grap2": the Guhmon-gevecht ({@link Guhmon}, {@link GuhmonSessie}, {@link GuhmonGevecht}) and Boer zoekt
 * Guh ({@link Bzg}, {@link BzgSessie}). This class owns the slice's registers (the stand-in guh types, the keepsake
 * blocks and items, the sounds) and hooks both games into the kern: the lobby NPCs and their roles, the two joke
 * questlines, the two kinds of game, the Guhdex section, the dev commands under {@code /guhs px grap2} and the self test.
 * Resources: tools/features/guhpixel_grap2*.py.
 */
public final class Grap2Slice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- the stand-in guhs (never saved, never summoned) ---------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<StandInGuh>> GUHMON_GUH = standInType("guhmon_guh");
    public static final DeferredHolder<EntityType<?>, EntityType<StandInGuh>> BZG_GUH = standInType("bzg_guh");

    // --- Guhmon: the badge case and the three badges ---------------------------------------------------------------------------
    public static final DeferredBlock<BadgeDoosBlock> BADGEDOOS = BLOCKS.registerBlock("guhmon_badgedoos", BadgeDoosBlock::new, () -> DecoBlock.props());
    public static final DeferredItem<LoreBlockItem> BADGEDOOS_ITEM = ITEMS.registerItem("guhmon_badgedoos", p -> new LoreBlockItem(BADGEDOOS.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredItem<BadgeItem> BADGE_DUTJES = ITEMS.registerItem("guhmon_badge_dutjes", BadgeItem::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<BadgeItem> BADGE_NJEG = ITEMS.registerItem("guhmon_badge_njeg", BadgeItem::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<BadgeItem> BADGE_KNABBEL = ITEMS.registerItem("guhmon_badge_knabbel", BadgeItem::new, () -> new Item.Properties().stacksTo(1));
    /** In the order of {@link Guhmon.Badge}. */
    public static final List<DeferredItem<BadgeItem>> BADGES = List.of(BADGE_DUTJES, BADGE_NJEG, BADGE_KNABBEL);

    // --- Boer zoekt Guh: the mailbox and the framed letter ------------------------------------------------------------------------
    public static final DeferredBlock<BrievenbusBlock> BRIEVENBUS = BLOCKS.registerBlock("bzg_brievenbus", BrievenbusBlock::new, () -> DecoBlock.props());
    public static final DeferredItem<LoreBlockItem> BRIEVENBUS_ITEM = ITEMS.registerItem("bzg_brievenbus", p -> new LoreBlockItem(BRIEVENBUS.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredBlock<IngelijsteBriefBlock> INGELIJSTE_BRIEF = BLOCKS.registerBlock("bzg_ingelijste_brief", IngelijsteBriefBlock::new,
            () -> MuurDecoBlock.props());
    public static final DeferredItem<LoreBlockItem> INGELIJSTE_BRIEF_ITEM = ITEMS.registerItem("bzg_ingelijste_brief",
            p -> new LoreBlockItem(INGELIJSTE_BRIEF.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());

    // --- sounds (sounds.json entries: tools/features/guhpixel_grap2.py) -----------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_GEVECHT = geluid("guhmon.gevecht");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_ZET = geluid("guhmon.zet");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_SLAAPT = geluid("guhmon.slaapt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_GEWONNEN = geluid("guhmon.gewonnen");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_TUNE = geluid("bzg.tune");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_BRIEF = geluid("bzg.brief");

    private static DeferredHolder<EntityType<?>, EntityType<StandInGuh>> standInType(String id) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.of(StandInGuh::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f)
                .clientTrackingRange(10).noSave().noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(GUHMON_GUH.get(), GuhEntity.createAttributes().build());
            event.put(BZG_GUH.get(), GuhEntity.createAttributes().build());
        });
        // Guhmon-gevecht
        NpcRollen.zet(GuhNpcEntity.Kind.GUHMON_GYMLEIDER, Guhmon.ROL);
        Grappen.registreer(new Grap(Guhmon.ID, 4, GuhNpcEntity.Kind.GUHMON_GYMLEIDER, Guhmon::aandenken));
        Sessies.registreer(Guhmon.SPEL);
        LobbyNpcs.registreer(LobbyPlek.SPEL_GUHMON, GuhNpcEntity.Kind.GUHMON_GYMLEIDER, null, server -> LobbyNpcs.spelersRegel(Guhmon.ID, 8));
        KledingBronnen.bron(GuhClothes.GUHMON_TRAINERSPET, "guhmon_gym");
        // Boer zoekt Guh
        NpcRollen.zet(GuhNpcEntity.Kind.BZG_PRESENTATRICE, Bzg.GUHVON_ROL);
        NpcRollen.zet(GuhNpcEntity.Kind.BZG_BOER, Bzg.BOER_ROL);
        Grappen.registreer(new Grap(Bzg.ID, 5, GuhNpcEntity.Kind.BZG_PRESENTATRICE, Bzg::aandenken));
        Sessies.registreer(Bzg.SPEL);
        LobbyNpcs.registreer(LobbyPlek.SPEL_BZG, GuhNpcEntity.Kind.BZG_PRESENTATRICE, null, server -> LobbyNpcs.spelersRegel(Bzg.ID, 4));
        KledingBronnen.bron(GuhClothes.BZG_STROHOED, "bzg_show");
        KledingBronnen.bron(GuhClothes.BZG_OVERALL, "bzg_show");
        Praat.luister(BzgSessie.SCENE_INTRO, (p, spreker, optie) -> {
            if (optie < 0 && Sessies.van(p) instanceof BzgSessie s) {
                s.introKlaar(p);
            }
        });
        Praat.luister(BzgSessie.SCENE_KEUZE, (p, spreker, optie) -> {
            if (optie >= 0 && Sessies.van(p) instanceof BzgSessie s) {
                s.keuze(p, optie);
            }
        });
        Praat.luister(BzgSessie.SCENE_SAMEN, (p, spreker, optie) -> {
            if (optie < 0 && Sessies.van(p) instanceof BzgSessie s) {
                s.samenKlaar(p);
            }
        });
        GidsBlad.registreer(GIDS);
        NeoForge.EVENT_BUS.addListener(Grap2Slice::commandos);
        PxZelftest.registreer("grap2", Grap2Slice::zelftest);
    }

    public static void payloads(PayloadRegistrar registrar) {
        Grap2Payloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BADGEDOOS_ITEM.get()));
        for (DeferredItem<BadgeItem> b : BADGES) {
            output.accept(new ItemStack(b.get()));
        }
        output.accept(new ItemStack(BRIEVENBUS_ITEM.get()));
        output.accept(new ItemStack(INGELIJSTE_BRIEF_ITEM.get()));
    }

    // --- shared by both games ---------------------------------------------------------------------------------------------------

    /** A guh character of a show (Gymleider Dutjes in his gym, Guhvon in her studio, Boer Guhrrit on the farm set). */
    @Nullable
    static GuhNpcEntity npc(ServerLevel level, GuhNpcEntity.Kind kind, Vec3 pos, float yaw) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (npc == null) {
            return null;
        }
        npc.setKind(kind);
        npc.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setPersistenceRequired();
        level.addFreshEntity(npc);
        return npc;
    }

    @Nullable
    static StandInGuh standIn(ServerLevel level, EntityType<StandInGuh> type, Vec3 pos, float yaw, CompoundTag looks, @Nullable Component naam) {
        StandInGuh g = type.create(level, EntitySpawnReason.TRIGGERED);
        if (g == null) {
            return null;
        }
        g.looks(looks);
        if (naam != null) {
            g.setCustomName(naam);
            g.setCustomNameVisible(true);
        } else {
            g.hideName = true;
        }
        g.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
        g.setYHeadRot(yaw);
        g.setYBodyRot(yaw);
        level.addFreshEntity(g);
        return g;
    }

    /** A click on a stand-in guh: the game of the player who clicked decides. */
    static void klik(StandInGuh guh, ServerPlayer p) {
        Sessie s = Sessies.van(p);
        if (s instanceof BzgSessie b) {
            b.klikGuh(p, guh);
        } else if (s instanceof GuhmonSessie g) {
            if (guh == g.mijn()) {
                g.daagUit(p);
            } else {
                p.sendOverlayMessage(Component.translatable(guh.slaapt() ? "gui.guhs.guhmon.publiek.slaapt" : "gui.guhs.guhmon.publiek.wakker")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** Three pink and golden rockets (harmless: nothing takes damage in guhpixel). */
    static void vuurwerk(ServerLevel level, Vec3 pos) {
        for (int i = 0; i < 3; i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            rocket.set(net.minecraft.core.component.DataComponents.FIREWORKS, new Fireworks(1, List.of(new FireworkExplosion(
                    FireworkExplosion.Shape.values()[i % FireworkExplosion.Shape.values().length],
                    it.unimi.dsi.fastutil.ints.IntList.of(0xFF7FB6, 0xFFD27A, 0xFFFFFF), it.unimi.dsi.fastutil.ints.IntList.of(0xE8506E), true, true))));
            level.addFreshEntity(new FireworkRocketEntity(level, pos.x + (i - 1) * 2.0, pos.y + 1, pos.z, rocket));
        }
    }

    /** A little pink blanket (a block display of a carpet) laid over a sleeper; it goes away with the arena. */
    static void dekentje(ServerLevel level, Vec3 pos, float yaw) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:block_display");
        CompoundTag blok = new CompoundTag();
        blok.putString("Name", "minecraft:pink_carpet");
        tag.put("block_state", blok);
        CompoundTag tf = new CompoundTag();
        tf.put("translation", floats(-0.5f, 0f, -0.55f));
        tf.put("scale", floats(1f, 1f, 1.1f));
        tf.put("left_rotation", floats(0f, 0f, 0f, 1f));
        tf.put("right_rotation", floats(0f, 0f, 0f, 1f));
        tag.put("transformation", tf);
        Entity display = EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.LOAD, e -> {
            e.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
            return e;
        });
        if (display != null) {
            level.addFreshEntity(display);
        }
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(net.minecraft.nbt.FloatTag.valueOf(f));
        }
        return l;
    }

    // --- the Guhdex section -----------------------------------------------------------------------------------------------------

    private static final GidsSectie GIDS = new GidsSectie() {
        @Override
        public String id() {
            return "grap2";
        }

        @Override
        public int volgorde() {
            return 30;
        }

        @Override
        public boolean zichtbaar(ServerPlayer p) {
            return true;
        }

        @Override
        public void vul(ServerPlayer p, Bouwer b) {
            b.grap(Guhmon.ID);
            if (Guhmon.gewonnen(p) + Guhmon.verloren(p) > 0) {
                b.stat(Component.translatable("gui.guhs.guhmon.gids.stand"),
                        Component.translatable("gui.guhs.guhmon.gids.stand.waarde", Guhmon.gewonnen(p), Guhmon.verloren(p)));
            }
            b.voortgang(Component.translatable("gui.guhs.guhmon.gids.badges"), Integer.bitCount(Guhmon.badges(p)), Guhmon.Badge.values().length);
            for (Guhmon.Badge badge : Guhmon.Badge.values()) {
                b.plaatje(new ItemStack(badge.item()), Component.translatable("item.guhs.guhmon_badge_" + badge.id()),
                        Component.translatable("gui.guhs.guhmon.gids.badge." + badge.id()), Guhmon.heeft(p, badge));
            }
            b.grap(Bzg.ID);
            if (Bzg.ingestopt(p) > 0) {
                b.stat(Component.translatable("gui.guhs.bzg.gids.ingestopt"), Component.literal(Integer.toString(Bzg.ingestopt(p))));
            }
        }
    };

    // --- dev commands: /guhs px grap2 ... ---------------------------------------------------------------------------------------

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> g = Commands.literal("grap2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        g.then(Commands.literal("guhmon")
                .then(Commands.literal("badges").then(Commands.argument("masker", IntegerArgumentType.integer(0, 7)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Guhmon.zetBadges(p, IntegerArgumentType.getInteger(ctx, "masker"));
                    return zeg(ctx.getSource(), "guhmon badges: " + Guhmon.badges(p));
                })))
                .then(Commands.literal("win").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (!(Sessies.van(p) instanceof GuhmonSessie s)) {
                        ctx.getSource().sendFailure(Component.literal("geen Guhmon-gevecht bezig (/guhs px speel guhmon)"));
                        return 0;
                    }
                    return zeg(ctx.getSource(), "guhmon: " + speelUit(s, p) + " ronden");
                }))
                .then(Commands.literal("kies").executes(ctx -> demo(ctx.getSource(), true)))
                .then(Commands.literal("scherm").executes(ctx -> demo(ctx.getSource(), false))));
        g.then(Commands.literal("bzg")
                .then(Commands.literal("stap").then(Commands.argument("n", IntegerArgumentType.integer(2, 5)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (!(Sessies.van(p) instanceof BzgSessie s)) {
                        ctx.getSource().sendFailure(Component.literal("geen Boer zoekt Guh bezig (/guhs px speel bzg)"));
                        return 0;
                    }
                    spoelDoor(s, p, IntegerArgumentType.getInteger(ctx, "n"));
                    return zeg(ctx.getSource(), "bzg: " + s.fase());
                })))
                .then(Commands.literal("brieven").executes(ctx -> bzgScherm(ctx.getSource(), "brieven")))
                .then(Commands.literal("brief").executes(ctx -> bzgScherm(ctx.getSource(), "brief")))
                .then(Commands.literal("aftiteling").executes(ctx -> bzgScherm(ctx.getSource(), "aftiteling"))));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("px").then(g)));
    }

    private static int zeg(CommandSourceStack source, String tekst) {
        source.sendSuccess(() -> Component.literal(tekst), false);
        return 1;
    }

    /** (Dev, tests) plays the running battle to a win with the obvious tactic; picks the leenguh first when needed. Returns the rounds played. */
    static int speelUit(GuhmonSessie s, ServerPlayer p) {
        if (s.fase() == GuhmonSessie.Fase.AANKOMST) {
            s.daagUit(p);
        }
        if (s.fase() == GuhmonSessie.Fase.KIEZEN) {
            s.kies(p, Guhmon.LEENGUH);
        }
        int ronden = 0;
        while (s.fase() == GuhmonSessie.Fase.GEVECHT && s.gevecht() != null && s.gevecht().uitkomst() != GuhmonGevecht.Uitkomst.GEWONNEN && ronden < 400) {
            if (s.gevecht().uitkomst() == GuhmonGevecht.Uitkomst.VERLOREN) {
                s.opnieuw(p);
                continue;
            }
            s.zet(p, (s.gevecht().dutjeMislukt(GuhmonGevecht.SPELER) ? GuhmonGevecht.Zet.VADSEN : GuhmonGevecht.Zet.DUTJE).ordinal());
            ronden++;
        }
        return ronden;
    }

    /** (Dev, tests) walks a running show on to step n (2..5) by doing what a player would do. */
    static void spoelDoor(BzgSessie s, ServerPlayer p, int stap) {
        if (stap >= 2 && s.fase() == BzgSessie.Fase.INTRO) {
            s.introKlaar(p);
        }
        if (stap >= 3 && s.fase() == BzgSessie.Fase.BRIEVEN) {
            for (int i = 0; i < Bzg.BRIEVEN; i++) {
                s.gelezen(p, i);
            }
            s.brievenDicht(p);
        }
        if (stap >= 4 && s.fase() == BzgSessie.Fase.LOGEREN) {
            for (StandInGuh g : new ArrayList<>(s.kandidaten())) {
                s.klikGuh(p, g);
            }
            s.praatBoer(p);
        }
        if (stap >= 5 && s.fase() == BzgSessie.Fase.KEUZE) {
            s.keuze(p, 3);
        }
    }

    /** (Dev, AutoCheck) the picker or the battle screen with made-up numbers, to look at; its buttons do nothing outside a gym. */
    private static int demo(CommandSourceStack source, boolean kies) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        CompoundTag data = new CompoundTag();
        if (kies) {
            data.putString("Scherm", "kies");
            ListTag guhs = nl.juiced.guhs.feature.guhpixel.GuhKiezer.lijst(p, r -> true);
            data.putInt("Eigen", guhs.size());
            CompoundTag leen = new CompoundTag();
            leen.store("Id", net.minecraft.core.UUIDUtil.CODEC, Guhmon.LEENGUH_ID);
            Tekst.put(leen, "Naam", Component.translatable("gui.guhs.guhmon.leenguh.naam"));
            CompoundTag looks = new CompoundTag();
            looks.putString("Variant", "mint");
            leen.put("Looks", looks);
            leen.putBoolean("Leenguh", true);
            guhs.add(leen);
            data.put("Guhs", guhs);
        } else {
            data.putString("Scherm", "gevecht");
            data.putBoolean("Vers", true);
            data.putBoolean("Demo", true);
            Tekst.put(data, "Naam", Component.translatable("gui.guhs.guhmon.leenguh.naam"));
            CompoundTag looks = new CompoundTag();
            looks.putString("Variant", "mint");
            data.put("Looks", looks);
            Tekst.put(data, "TegenNaam", Component.translatable("gui.guhs.guhmon.tegen.naam"));
            CompoundTag tegen = new CompoundTag();
            tegen.putString("Variant", "starry");
            data.put("TegenLooks", tegen);
            data.putInt("SlaapSpeler", 65);
            data.putInt("SlaapTegen", 40);
            data.putInt("MaagSpeler", 1);
            data.put("Regels", new ListTag());
        }
        ModNetworking.sendTo(p, new Grap2Payloads.GuhmonScherm(data));
        return 1;
    }

    private static int bzgScherm(CommandSourceStack source, String scherm) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        CompoundTag data = new CompoundTag();
        data.putString("Scherm", scherm);
        data.putInt("Regels", Bzg.AFTITELING_REGELS);
        data.putBoolean("Demo", true);
        ModNetworking.sendTo(p, new Grap2Payloads.BzgScherm(data));
        return 1;
    }

    // --- the dev-server self test (the real dimension) ----------------------------------------------------------------------------

    private static void zelftest(net.minecraft.server.MinecraftServer server, ServerLevel level, PxZelftest.Melder meld) {
        StructureTemplate gym = level.getStructureManager().get(Guhmon.ARENA.template()).orElse(null);
        meld.check(gym != null && gym.getSize().equals(Guhmon.MAAT), "the gym template exists and is " + Guhmon.MAAT.toShortString());
        StructureTemplate studio = level.getStructureManager().get(Bzg.ARENA.template()).orElse(null);
        meld.check(studio != null && studio.getSize().equals(Bzg.MAAT), "the studio template exists and is " + Bzg.MAAT.toShortString());
        if (studio != null) {
            List<StructureTemplate.StructureBlockInfo> bussen = studio.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), BRIEVENBUS.get());
            meld.check(bussen.size() == 1 && bussen.get(0).pos().equals(Bzg.BRIEVENBUS), "the studio has its one mailbox at " + Bzg.BRIEVENBUS.toShortString());
        }
        meld.check(Grappen.van(Guhmon.ID) != null && Grappen.van(Bzg.ID) != null && Sessies.soort(Guhmon.ID) != null && Sessies.soort(Bzg.ID) != null,
                "both joke games and their kinds of game are registered");
        for (Block b : List.of(BADGEDOOS.get(), BRIEVENBUS.get(), INGELIJSTE_BRIEF.get())) {
            meld.check(!b.asItem().getDefaultInstance().isEmpty(), "block " + b.getDescriptionId() + " has its item");
        }
    }

    private Grap2Slice() {
    }
}
