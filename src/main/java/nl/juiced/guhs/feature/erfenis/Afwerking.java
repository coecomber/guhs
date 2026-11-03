package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.erfenis.sherlock.Sherlock;
import nl.juiced.guhs.feature.erfenis.vorm.DecoWezens;
import nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen;

/**
 * 1.9 "Een opknapper!", the finishing slice: what the slices left open, glued onto the kern.
 * <ul>
 *   <li>Sherlock's story characters stand in front of the door of your Detectivebureau ({@link Sherlock#bureauPlek}).</li>
 *   <li>Sherlock's rewards do something in the village ({@link #beloning}, mirrored in {@link Base#beloningen}): Opa's kelder
 *   under the Hoeve ({@link OpasKelder}), the bureau's end look (Stempel), Opa's theetaartjes in the Bakkerij (Productie),
 *   Oma Breiguh's own orders (kaart.Bestellingen), Bertha singing on the Muziektent, Opa Knabbel's testament as a book.</li>
 *   <li>Makelaar Guhrrit's postcards from his holiday after the tour ({@link #opMoment}): five cards at Hoeve 3..7, brought by
 *   the Postguh; with the three found while clearing the Kaartjesbord holds 8 (deco.md #64, 2 Knusheid per card).</li>
 * </ul>
 */
public final class Afwerking {
    /** The postcards sent from the holiday: card number (4..8; 1..3 are found under plukjes) at Hoeve level 3..7. */
    static final int EERSTE_POST = 4, LAATSTE_POST = 8, POST_VANAF_HOEVE = 3;
    static final String BERTHA_TAG = "guhs_bertha";

    private Afwerking() {
    }

    static void init() {
        ErfenisMomenten.luister(Afwerking::opMoment);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(Afwerking::commando);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(Afwerking::tick);
        Sherlock.bureauPlek = (s, u) -> {
            BureauDeur d = bureauDeur(s, u);
            return d == null ? null : d.voor();
        };
        Sherlock.bureauUit = (s, u) -> {
            BureauDeur d = bureauDeur(s, u);
            return d == null ? null : d.uit();
        };
    }

    // =====================================================================================================================
    // the makelaar's postcards
    // =====================================================================================================================

    static void opMoment(ServerPlayer p, String m) {
        if (!m.startsWith("hoeve_level:")) {
            return;
        }
        MinecraftServer server = p.level().getServer();
        Base b = ErfenisData.get(server).van(p.getUUID());
        if (b == null || b.npc || b.tutorial < Makelaarstour.KLAAR) {
            return;
        }
        int hoeve;
        try {
            hoeve = Integer.parseInt(m.substring(m.lastIndexOf(':') + 1));
        } catch (NumberFormatException e) {
            return;
        }
        for (int nr : kaartenTot(b, hoeve)) {
            stuurKaart(p, b, nr);
        }
    }

    /** The holiday cards this village should have by this Hoeve level and doesn't have yet. */
    static List<Integer> kaartenTot(Base b, int hoeve) {
        List<Integer> l = new ArrayList<>();
        for (int nr = EERSTE_POST; nr <= LAATSTE_POST; nr++) {
            if (hoeve >= POST_VANAF_HOEVE + nr - EERSTE_POST && !b.vondsten.contains("ansichtkaart_" + nr)) {
                l.add(nr);
            }
        }
        return l;
    }

    /** Card nr into the village (vondstenkistje + Kaartjesbord) and, as a keepsake, into the player's pockets. */
    static void stuurKaart(ServerPlayer p, Base b, int nr) {
        b.vondsten.add("ansichtkaart_" + nr);
        b.kaartjesbordErbij();
        MinecraftServer server = p.level().getServer();
        ErfenisData.get(server).setDirty();
        Erfenis.syncKijkers(server, b);
        String k = "erfenis.afwerking.ansicht." + nr;
        ItemStack kaart = new ItemStack(Items.PAPER);
        kaart.set(DataComponents.CUSTOM_NAME, Component.translatable(k + ".naam").withStyle(Style.EMPTY.withItalic(false).withColor(0xF29BB8)));
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            lore.add(Component.translatable(k + ".tekst." + i).withStyle(s -> s.withItalic(true).withColor(ChatFormatting.GRAY)));
        }
        lore.add(Component.translatable("erfenis.afwerking.ansicht.groet").withStyle(s -> s.withItalic(false).withColor(ChatFormatting.LIGHT_PURPLE)));
        kaart.set(DataComponents.LORE, new ItemLore(lore));
        nl.juiced.guhs.feature.erfenis.koppeling.Makelaarsbrief.bezorgPost(p, kaart, Component.translatable("erfenis.afwerking.ansicht.bezorgd"));
        // the card's text in the chat too, so you can read it without hovering over the item
        p.sendSystemMessage(Component.literal("~ ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(k + ".naam").withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE)).append(" ~"));
        for (int i = 0; i < 4; i++) {
            p.sendSystemMessage(Component.literal("  ").append(Component.translatable(k + ".tekst." + i)).withStyle(ChatFormatting.ITALIC));
        }
    }

    // =====================================================================================================================
    // Sherlock's rewards in the village
    // =====================================================================================================================

    /** Sherlock gave this reward (deco ids too: those go to the decoschuurtje by Sherlock himself). */
    public static void beloning(ServerPlayer p, String vlag) {
        MinecraftServer server = p.level().getServer();
        Base b = ErfenisData.get(server).van(p.getUUID());
        if (b != null && !b.npc && !b.beloningen.contains(vlag)) {
            b.beloningen.add(vlag);
            pasToe(server, b, vlag);
        }
        if (vlag.equals("testament")) {
            geef(p, boek("testament", 3));
        } else if (vlag.equals("opas_kelder")) {
            geef(p, boek("dagboek", 3));
        } else if (vlag.equals("paaltjes_terug")) {
            geef(p, pimsWorteltje());
        }
    }

    /** (1.9.1) Pim's thank-you for the moestuintje (zaak 7): a carrot with a pink bow, the same bow as on the posts. */
    static ItemStack pimsWorteltje() {
        ItemStack s = new ItemStack(Items.GOLDEN_CARROT);
        s.set(DataComponents.CUSTOM_NAME, Component.translatable("erfenis.afwerking.worteltje.naam")
                .withStyle(Style.EMPTY.withItalic(false).withColor(0xF29BB8)));
        s.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("erfenis.afwerking.worteltje.tekst.0").withStyle(st -> st.withItalic(true).withColor(ChatFormatting.GRAY)),
                Component.translatable("erfenis.afwerking.worteltje.tekst.1").withStyle(st -> st.withItalic(true).withColor(ChatFormatting.GRAY)))));
        return s;
    }

    /** Catches up flags Sherlock gave before this village mirrored them (called before every sync). */
    static void spiegel(MinecraftServer server, Base b) {
        if (b.npc || b.eigenaar == null) {
            return;
        }
        for (String v : Sherlock.beloningen(server, b.eigenaar)) {
            if (!b.beloningen.contains(v)) {
                b.beloningen.add(v);
                pasToe(server, b, v);
            }
        }
    }

    /** A new flag: the buildings it changes are stamped again (now when somebody is there, else on the next visit). */
    private static void pasToe(MinecraftServer server, Base b, String vlag) {
        GebouwType t = switch (vlag) {
            case "opas_kelder" -> GebouwType.HOEVE;
            case "bureau_eindlook" -> GebouwType.DETECTIVEBUREAU;
            case "bertha_optreden" -> GebouwType.MUZIEKTENT;
            default -> null;
        };
        ErfenisData.get(server).setDirty();
        if (t == null) {
            return;
        }
        if (!Erfenis.bezocht(server, b)) {
            b.moetStempelen = true;
            return;
        }
        for (Gebouw g : new ArrayList<>(b.gebouwen)) {
            if (g.type == t && g.level > 0) {
                Erfenis.stempelGebouw(server, b, g);
            }
        }
    }

    /** After a building's shape is stamped (queued behind it): Opa's kelder under the Hoeve, Bertha on the Muziektent. */
    static void naStempel(ServerLevel level, Base b, Gebouw g, BlockPos hoek) {
        if (g.level <= 0) {
            return;
        }
        if (g.type == GebouwType.HOEVE && b.beloningen.contains("opas_kelder")) {
            GebouwVormen.daarna(level, () -> {
                OpasKelder.stempel(level, hoek, g.b(), g.d());
                OpasKelder.vulAan(level, hoek, g.b(), g.d(), b);   // what was in the old kelder (the Hoeve moved)
            });
        } else if (g.type == GebouwType.MUZIEKTENT && b.beloningen.contains("bertha_optreden") && dorpsfeestje(level)) {
            GebouwVormen.daarna(level, () -> bertha(level, hoek, g));
        }
    }

    /**
     * (1.9.1) Before a Hoeve moves or turns (its old corner and footprint): the old kelder underneath is filled in again,
     * its contents move along to the new one.
     */
    static void voorVerhuizing(ServerLevel level, Base b, Gebouw g, BlockPos oudeHoek, int oudB, int oudD) {
        if (g.type == GebouwType.HOEVE && g.level > 0 && b.beloningen.contains("opas_kelder")) {
            OpasKelder.wis(level, oudeHoek, oudB, oudD, b.id);
            if (b.moetStempelen) {
                OpasKelder.bezorg(level, b);   // the new kelder only comes on the next visit: the things by post right away
            }
        }
    }

    // =====================================================================================================================
    // dorpsfeestjes: Bertha only sings now and then
    // =====================================================================================================================

    /** A dorpsfeestje: the first {@value #FEEST_MIN} minutes of every {@value #FEEST_ELKE} (game time of the erfenis level). */
    static final int FEEST_MIN = 4, FEEST_ELKE = 20;

    static boolean dorpsfeestje(ServerLevel level) {
        return level.getGameTime() / 1200L % FEEST_ELKE < FEEST_MIN;
    }

    /** Every 5 s: during a dorpsfeestje Bertha comes on stage at every loaded Muziektent that has her, afterwards she goes home. */
    static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 100 != 0) {
            return;
        }
        ServerLevel level = Erfenis.level(server);
        if (level == null) {
            return;
        }
        boolean feest = dorpsfeestje(level);
        for (Base b : ErfenisData.get(server).alleBases()) {
            if (b.npc || !b.beloningen.contains("bertha_optreden")) {
                continue;
            }
            BlockPos o = Plot.oorsprong(b.cx, b.cz);
            for (Gebouw g : b.gebouwen) {
                if (g.type != GebouwType.MUZIEKTENT || g.level <= 0) {
                    continue;
                }
                BlockPos hoek = o.offset(g.x, 0, g.z);
                if (!level.isPositionEntityTicking(hoek) || !level.isPositionEntityTicking(hoek.offset(g.b() - 1, 0, g.d() - 1))) {
                    continue;
                }
                List<Cow> koeien = berthas(level, hoek, g);
                if (feest && koeien.isEmpty()) {
                    bertha(level, hoek, g);
                    ServerPlayer eigenaar = b.eigenaar == null ? null : server.getPlayerList().getPlayer(b.eigenaar);
                    if (eigenaar != null && Erfenis.in(eigenaar) && eigenaar.blockPosition().closerThan(hoek, 64)) {
                        eigenaar.sendSystemMessage(Component.translatable("erfenis.afwerking.feestje").withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                } else if (!feest) {
                    koeien.forEach(Cow::discard);
                }
            }
        }
    }

    private static List<Cow> berthas(ServerLevel level, BlockPos hoek, Gebouw g) {
        net.minecraft.world.phys.AABB vak = new net.minecraft.world.phys.AABB(hoek.getX(), hoek.getY() - 2, hoek.getZ(),
                hoek.getX() + g.b(), hoek.getY() + GebouwVormen.WIS_HOOGTE, hoek.getZ() + g.d());
        return level.getEntitiesOfClass(Cow.class, vak, c -> c.entityTags().contains(BERTHA_TAG));
    }

    /** Bertha de koe on the stage, next to the singer (Sherlock zaak 6: she sings now; decoration like the shape's creatures). */
    static void bertha(ServerLevel level, BlockPos hoek, Gebouw g) {
        if (!berthas(level, hoek, g).isEmpty()) {
            return;   // she is already on stage
        }
        // the stage spot at (3.3, 4.2) of the 9x9 shape (the singer stands at its middle), turned with the building
        double lx = 3.3, lz = 4.2;
        int w = 9, d = 9;
        double x, z;
        switch (g.rot & 3) {
            case 1 -> {
                x = d - lz;
                z = lx;
            }
            case 2 -> {
                x = w - lx;
                z = d - lz;
            }
            case 3 -> {
                x = lz;
                z = w - lx;
            }
            default -> {
                x = lx;
                z = lz;
            }
        }
        BlockPos kolom = BlockPos.containing(hoek.getX() + x, hoek.getY() + 1, hoek.getZ() + z);
        BlockPos staan = kolom;
        for (int y = 0; y < 5; y++) {
            BlockPos p = kolom.above(y);
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                    && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()) {
                staan = p;
                break;
            }
        }
        Cow koe = EntityType.COW.create(level, EntitySpawnReason.COMMAND);
        if (koe == null) {
            return;
        }
        float yaw = 90f * (g.rot & 3);   // looking out at the audience (south of the shape, turned)
        koe.snapTo(hoek.getX() + x, staan.getY(), hoek.getZ() + z, yaw, 0f);
        koe.setYHeadRot(yaw);
        koe.setYBodyRot(yaw);
        koe.setNoAi(true);
        koe.setInvulnerable(true);
        koe.setPersistenceRequired();
        koe.setCustomName(Component.translatable("erfenis.afwerking.bertha"));
        koe.setCustomNameVisible(true);
        koe.addTag(DecoWezens.TAG);
        koe.addTag(BERTHA_TAG);
        level.addFreshEntity(koe);
    }

    // =====================================================================================================================
    // the Detectivebureau's door
    // =====================================================================================================================

    /** In front of the door (feet, one block out) and which way that is. */
    record BureauDeur(BlockPos voor, Direction uit) {
    }

    @Nullable
    static BureauDeur bureauDeur(MinecraftServer server, UUID speler) {
        Base b = ErfenisData.get(server).van(speler);
        ServerLevel level = Erfenis.level(server);
        if (b == null || b.npc || level == null) {
            return null;
        }
        Gebouw bureau = null;
        for (Gebouw g : b.gebouwen) {
            if (g.type == GebouwType.DETECTIVEBUREAU && g.level > 0) {
                bureau = g;
                break;
            }
        }
        if (bureau == null) {
            return null;
        }
        BlockPos hoek = Plot.oorsprong(b.cx, b.cz).offset(bureau.x, 0, bureau.z);
        double mx = hoek.getX() + bureau.b() / 2.0, mz = hoek.getZ() + bureau.d() / 2.0;
        if (level.isLoaded(hoek)) {
            for (int x = 0; x < bureau.b(); x++) {
                for (int z = 0; z < bureau.d(); z++) {
                    BlockPos p = hoek.offset(x, 1, z);
                    BlockState st = level.getBlockState(p);
                    if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                        Direction f = st.getValue(DoorBlock.FACING);
                        // out = the side of the door without a roof above it (the shapes' front doors face out; a roof = inside)
                        Direction uit = onderDak(level, p.relative(f)) && !onderDak(level, p.relative(f.getOpposite())) ? f.getOpposite() : f;
                        return new BureauDeur(p.relative(uit), uit);
                    }
                }
            }
        }
        // no door (yet; the first level is an open little stand): the middle of the front edge (south, turned with the building)
        Direction uit = Direction.SOUTH;
        for (int i = 0; i < (bureau.rot & 3); i++) {
            uit = uit.getClockWise();
        }
        double half = uit.getAxis() == Direction.Axis.X ? bureau.b() / 2.0 : bureau.d() / 2.0;
        return new BureauDeur(BlockPos.containing(mx + uit.getStepX() * (half + 0.5), hoek.getY() + 1, mz + uit.getStepZ() * (half + 0.5)), uit);
    }

    /** Is there something solid above this spot (2..8 blocks up)? */
    private static boolean onderDak(ServerLevel level, BlockPos p) {
        for (int y = 2; y <= 8; y++) {
            if (!level.getBlockState(p.above(y)).getCollisionShape(level, p.above(y)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================================================================
    // dev commands (op): /guhs erfenis ansicht <4-8> | beloning <vlag> | kelder
    // =====================================================================================================================

    private static void commando(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("guhs").then(net.minecraft.commands.Commands.literal("erfenis")
                .then(net.minecraft.commands.Commands.literal("ansicht").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                        .then(net.minecraft.commands.Commands.argument("nr", com.mojang.brigadier.arguments.IntegerArgumentType.integer(EERSTE_POST, LAATSTE_POST))
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    Base b = Erfenis.eigenBase(p);
                                    int nr = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "nr");
                                    b.vondsten.remove("ansichtkaart_" + nr);
                                    stuurKaart(p, b, nr);
                                    return 1;
                                })))
                .then(net.minecraft.commands.Commands.literal("beloning").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                        .then(net.minecraft.commands.Commands.argument("vlag", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, sb) -> net.minecraft.commands.SharedSuggestionProvider.suggest(List.of("recept_theetaartje", "oma_bestelling",
                                        "opas_kelder", "bertha_optreden", "paaltjes_terug", "testament", "bureau_eindlook"), sb))
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    Erfenis.eigenBase(p);
                                    String vlag = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "vlag");
                                    beloning(p, vlag);
                                    c.getSource().sendSuccess(() -> Component.translatable("erfenis.afwerking.cmd.beloning", vlag), false);
                                    return 1;
                                })))
                .then(net.minecraft.commands.Commands.literal("kelder").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Base b = Erfenis.eigenBase(p);
                            ServerLevel level = Erfenis.level(c.getSource().getServer());
                            Gebouw h = b.hoeve();
                            if (level == null || h == null || !b.beloningen.contains("opas_kelder")) {
                                c.getSource().sendFailure(Component.translatable("erfenis.afwerking.cmd.geen_kelder"));
                                return 0;
                            }
                            BlockPos hoek = Plot.oorsprong(b.cx, b.cz).offset(h.x, 0, h.z);
                            Vec3 in = new Vec3(hoek.getX() + h.b() / 2 + 0.5, hoek.getY() + OpasKelder.VLOER + 1, hoek.getZ() + h.d() / 2 + 1.5);
                            p.teleport(new net.minecraft.world.level.portal.TeleportTransition(level, in, Vec3.ZERO, 180f, 10f,
                                    net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
                            return 1;
                        }))));
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** A written book from Opa Knabbel (lang erfenis.afwerking.boek.<id>.titel / .<n>). */
    static ItemStack boek(String id, int bladzijden) {
        ItemStack s = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> l = new ArrayList<>();
        for (int i = 0; i < bladzijden; i++) {
            l.add(Filterable.passThrough(Component.translatable("erfenis.afwerking.boek." + id + "." + i)));
        }
        s.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Opa Knabbel"), "", 0, l, true));
        s.set(DataComponents.CUSTOM_NAME, Component.translatable("erfenis.afwerking.boek." + id + ".titel")
                .withStyle(Style.EMPTY.withItalic(false).withColor(0xF7B6CB)));
        s.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("erfenis.afwerking.boek.door")
                .withStyle(st -> st.withItalic(false).withColor(ChatFormatting.GRAY)))));
        return s;
    }

    private static void geef(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s) && !s.isEmpty()) {
            p.drop(s, false);
        }
    }
}
