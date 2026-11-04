package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;

/**
 * The item of a gear base type (weapons and armour alike: armour gets an equippable component at registration). Its name
 * is coloured by rarity; its tooltip (design: name, stars, rarity, class + level, element damage, bonuses, flavour):
 * <pre>
 *   Kussenzwaard                      (rarity colour)
 *   ★★☆☆☆  Zeldzaam
 *   Knuffelridder · Lv 4
 *   12 schade · Roze
 *   +3 Kracht / +2,5% Kritiek ...
 *   "Slaat hard, knuffelt harder."    (grey italic)
 * </pre>
 */
public class RijkItem extends Item {
    public static final int MAX_STERREN = 10;
    /** z2-systemen hook: extra tooltip lines after the bonuses (the gem sockets). Called on the client. */
    public static final java.util.List<java.util.function.BiConsumer<Uitrusting, Consumer<Component>>> TOOLTIP_EXTRA =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    public RijkItem(Properties p) {
        super(p);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component basis = super.getName(stack);
        Uitrusting u = Rollen.van(stack);
        if (u == null) {
            return basis;
        }
        MutableComponent n = Component.empty();
        if (u.verpluisd()) {
            n.append(Component.translatable("vadsrijk.kern.verpluisd")).append(" ");
        }
        n.append(basis);
        if (u.sterren() > 0) {
            n.append(" +" + u.sterren());
        }
        return n.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(u.zeldzaamheid().rgb)).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
        Uitrusting u = Rollen.van(stack);
        if (u == null) {
            t.accept(Component.translatable("vadsrijk.kern.item.ongerold").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        BasisType bt = u.basisType();
        Zeldzaamheid z = u.zeldzaamheid();
        StringBuilder sterren = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sterren.append(i < Math.min(5, u.sterren()) ? "★" : "☆");
        }
        t.accept(Component.literal(sterren + "  ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(z.key()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(z.rgb)).withBold(true))));
        MutableComponent wie = bt != null && bt.klasse() != null ? Component.translatable(Klassen.ofRidder(bt.klasse()).naamKey())
                : Component.translatable("vadsrijk.kern.item.alle_klassen");
        t.accept(wie.withStyle(ChatFormatting.GRAY).append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("vadsrijk.kern.item.level", u.level()).withStyle(ChatFormatting.YELLOW)));
        if (bt != null && bt.slot() == Slot.WAPEN) {
            Element e = u.element();
            MutableComponent s = Component.translatable("vadsrijk.kern.item.schade", String.format(Locale.ROOT, "%.0f", Rollen.klapSchade(u)))
                    .withStyle(ChatFormatting.WHITE);
            if (e != Element.GEEN) {
                s.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)).append(e.icoon())
                        .append(Component.translatable(e.key()).withStyle(e.kleur));
            }
            t.accept(s);
            t.accept(Component.translatable("vadsrijk.kern.item.aanvalssnelheid",
                    nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo.tekst(nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo.seconden(bt)))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (bt != null) {
            Stats basis = bt.statsOp(u.level()).maal(z.factor);
            basis.alles().forEach((st, v) -> {
                if (Math.abs(v) >= 0.5) {
                    t.accept(regel(st, v, ChatFormatting.WHITE));
                }
            });
        }
        for (Bonus b : u.bonussen()) {
            t.accept(regel(b.stat(), b.waarde(), ChatFormatting.GREEN));
        }
        TOOLTIP_EXTRA.forEach(f -> f.accept(u, t));   // z2-systemen
        if (u.gebonden()) {
            t.accept(Component.translatable("vadsrijk.kern.item.gebonden").withStyle(ChatFormatting.RED));
        }
        if (u.verpluisd()) {
            t.accept(Component.translatable("vadsrijk.kern.item.verpluisd_uitleg").withStyle(ChatFormatting.RED));
        }
        if (bt != null && bt.flavours() > 0) {
            t.accept(Component.translatable(bt.flavourKey(u.flavour())).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        if (z == Zeldzaamheid.GUHDDELIJK) {
            t.accept(Component.translatable("vadsrijk.kern.item.guhddelijk").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        }
    }

    static Component regel(Stat s, double v, ChatFormatting kleur) {
        String getal = s.procent ? String.format(Locale.ROOT, "%+.1f%%", v) : String.format(Locale.ROOT, "%+.0f", v);
        return Component.literal(getal + " ").withStyle(kleur).append(Component.translatable(s.key()).withStyle(ChatFormatting.GRAY));
    }

    /** Jewellery: right-click in the dimension puts it on ({@code kern.Sieraden}). */
    @Override
    public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        Uitrusting u = Rollen.van(player.getItemInHand(hand));
        if (u != null && u.basisType() != null && u.basisType().slot().sieraad()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                nl.juiced.guhs.feature.vadsrijk.kern.Sieraden.draag(sp, hand);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        Uitrusting u = Rollen.van(stack);
        return u != null && u.zeldzaamheid().ordinal() >= Zeldzaamheid.LEGENDARISCH.ordinal();
    }
}
