package nl.juiced.guhs.feature.bouwhulp;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.BlauwdrukItem;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Catalogus;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.OntwerpbureauBlock;
import nl.juiced.guhs.feature.bouwhulp.blokken.BouwBlokken;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Gereedschap;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.GereedschapPayloads;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.KeetGereedschapTabs;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Kwast;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Terreinwerk;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Vormstaf;
import nl.juiced.guhs.feature.safari.lobby.SafariWinkel;

/**
 * 1.6.0 deel 2, F9: the tools and the blueprints (design 2.3-2.6, 2.9). The vormstaf (shapes, roofs, capturing), the four
 * kwasten, the egaliseerder (Zoutkeet), the verfmachine and the tunnelboor (Sauskeet), the Ontwerpbureau and the printed
 * blueprints; the keet's Vormen, Daken and Blauwdrukken tabs; the six park buildings as blueprints in the giftshop.
 * Resources: tools/features/bouwhulp_gereedschap.py.
 */
public final class GereedschapFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredItem<Vormstaf> VORMSTAF = ITEMS.registerItem("vormstaf", Vormstaf::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Kwast> VERVANGKWAST = kwast("vervangkwast", Gereedschap.VERVANGKWAST);
    public static final DeferredItem<Kwast> MENGKWAST = kwast("mengkwast", Gereedschap.MENGKWAST);
    public static final DeferredItem<Kwast> PATROONKWAST = kwast("patroonkwast", Gereedschap.PATROONKWAST);
    public static final DeferredItem<Kwast> VERWEERKWAST = kwast("verweerkwast", Gereedschap.VERWEERKWAST);
    public static final DeferredItem<Terreinwerk> EGALISEERDER = werk("egaliseerder", Gereedschap.EGALISEERDER, Rarity.UNCOMMON);
    public static final DeferredItem<Terreinwerk> VERFMACHINE = werk("verfmachine", Gereedschap.VERFMACHINE, Rarity.RARE);
    public static final DeferredItem<Terreinwerk> TUNNELBOOR = werk("tunnelboor", Gereedschap.TUNNELBOOR, Rarity.RARE);
    public static final DeferredItem<BlauwdrukItem> BLAUWDRUK = ITEMS.registerItem("blauwdruk", BlauwdrukItem::new, () -> new Item.Properties().stacksTo(1));

    public static final DeferredBlock<OntwerpbureauBlock> ONTWERPBUREAU = BLOCKS.registerBlock("ontwerpbureau", OntwerpbureauBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2f, 3f).noOcclusion().sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> ONTWERPBUREAU_ITEM = ITEMS.registerSimpleBlockItem(ONTWERPBUREAU);

    private static DeferredItem<Kwast> kwast(String id, Gereedschap g) {
        return ITEMS.registerItem(id, p -> new Kwast(p, g), () -> new Item.Properties().stacksTo(1));
    }

    private static DeferredItem<Terreinwerk> werk(String id, Gereedschap g, Rarity r) {
        return ITEMS.registerItem(id, p -> new Terreinwerk(p, g), () -> new Item.Properties().stacksTo(1).rarity(r));
    }

    /** The giftshop's prices of the park blueprints (design 2.9 / 3.2: 50-150 coins). */
    private static final int[] PRIJZEN = {80, 100, 120, 90, 150, 70};

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        KeetGereedschapTabs.register();
        BouwBlokken.TAB_EXTRA.add(out -> {
            for (DeferredItem<?> i : new DeferredItem<?>[] {VORMSTAF, VERVANGKWAST, MENGKWAST, PATROONKWAST, VERWEERKWAST, EGALISEERDER, VERFMACHINE,
                    TUNNELBOOR, ONTWERPBUREAU_ITEM}) {
                out.accept(new ItemStack(i.get()));
            }
            for (Catalogus.Ontwerpje o : Catalogus.lijst()) {
                out.accept(BlauwdrukItem.catalogus(o.id()));
            }
        });
        for (int i = 0; i < Catalogus.SAFARI.size(); i++) {
            String id = Catalogus.SAFARI.get(i);
            SafariWinkel.aanbod("blauwdruk_" + id).groep("blauwdrukken").icoon(() -> BlauwdrukItem.catalogus(id))
                    .naam(Component.translatable("gui.guhs.blauwdruk.cat." + id))
                    .uitleg(Component.translatable("gui.guhs.blauwdruk.winkel.uitleg"))
                    .prijs(PRIJZEN[i]).max(1)
                    .lever((p, a) -> geef(p, BlauwdrukItem.catalogus(id))).registreer();
        }
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                nl.juiced.guhs.feature.bouwhulp.gereedschap.VastlegDienst.vergeet(p.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener(GereedschapFeature::commands);
    }

    /** (op, dev check) /guhs bouwgereedschap catalogus: every catalogue design built at once in rows east of you. */
    private static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("guhs").then(net.minecraft.commands.Commands.literal("bouwgereedschap")
                .requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                .then(net.minecraft.commands.Commands.literal("catalogus").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    net.minecraft.core.BlockPos start = p.blockPosition().offset(4, 0, 0);
                    int x = 0, z = 0, rij = 0;
                    for (Catalogus.Ontwerpje o : Catalogus.lijst()) {
                        var b = Catalogus.get(o.id());
                        net.minecraft.core.Vec3i g = b.ontwerp.grootte();
                        if (x > 100) {
                            x = 0;
                            z += rij + 4;
                            rij = 0;
                        }
                        net.minecraft.core.BlockPos hoek = start.offset(x, 0, z);
                        for (var e : b.ontwerp.blokken().entrySet()) {
                            p.level().setBlock(hoek.offset(e.getKey()), e.getValue(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                        }
                        x += g.getX() + 4;
                        rij = Math.max(rij, g.getZ());
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("catalogus: " + Catalogus.lijst().size() + " ontwerpen"), false);
                    return 1;
                }))));
    }

    private static boolean geef(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        if (!p.getInventory().add(s)) {
            p.drop(s, false);
        }
        return true;
    }

    public static void payloads(PayloadRegistrar registrar) {
        GereedschapPayloads.register(registrar);
    }

    private GereedschapFeature() {
    }
}
