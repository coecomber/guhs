package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;

/**
 * Koken: food with a buff (A1: "eten met buffs, 30 min, ook partyfeest"). ONE food buff at a time, 30 min of real time, it stays
 * when you faint (it lives in {@code AmbachtData "buff"}, not in the status list). The Vadsigheid of the dish makes it up to 30 %
 * stronger. The Feestmaal feeds your whole party within {@link #FEEST_STRAAL} blocks and adds "Gezellig" (small boost to everything).
 * Stat names are the plain ones of the stat list ("Vads +60, Kracht +3"). Items {@code guhs:vr_amb_eten_<id>}.
 */
public final class Eten {
    public static final long DUUR_MS = 30 * 60_000L;
    public static final int FEEST_STRAAL = 10;

    /**
     * A dish: tier, its stats, an ambacht-xp factor and extra double-yield chance (the Verzamelaarshapje), a combat-xp factor
     * (Ridderontbijt), and whether it is a party feast.
     */
    public record Gerecht(String id, int tier, Stats stats, double ambachtXp, double dubbel, double gevechtXp, boolean feest) {
        public String itemId() {
            return "vr_amb_eten_" + id;
        }
    }

    public static final Map<String, Gerecht> GERECHTEN = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<EtenItem>> ITEM = new LinkedHashMap<>();
    /** Gezellig (the feast): +1 Kritiek, +2 % Heal-kracht / Elementschade / Elementweerstand. */
    public static final Stats GEZELLIG = Stats.van(Stat.KRITIEK, 1, Stat.HEALKRACHT, 2, Stat.ELEMENTSCHADE, 2, Stat.ELEMENTWEERSTAND, 2);

    static {
        g("klodderpudding", 1, Stats.van(Stat.VADS, 60, Stat.KRACHT, 3), 1, 0, 1, false);
        g("visstoofje", 1, Stats.van(Stat.VERDEDIGING, 4, Stat.VADS, 40), 1, 0, 1, false);
        g("verzamelaarshapje", 1, new Stats(), 1.10, 0.10, 1, false);
        g("kandijsoep", 2, Stats.van(Stat.ELEMENTWEERSTAND, 6, Stat.VADS, 120), 1, 0, 1, false);
        g("forelbroodje", 2, Stats.van(Stat.KRITIEK, 3, Stat.BEHENDIGHEID, 6), 1, 0, 1, false);
        g("ridderontbijt", 2, Stats.van(Stat.KRACHT, 6), 1, 0, 1.05, false);
        g("honingpannenkoek", 3, Stats.van(Stat.HEALKRACHT, 5, Stat.WIJSHEID, 10, Stat.VADS, 150), 1, 0, 1, false);
        g("wortelstamppot", 3, Stats.van(Stat.VERDEDIGING, 14, Stat.VADS, 200), 1, 0, 1, false);
        g("feestmaal", 3, Stats.van(Stat.VADS, 150, Stat.KRACHT, 6, Stat.BEHENDIGHEID, 6, Stat.WIJSHEID, 6), 1, 0, 1, true);
        // U-AMBACHT2: zone 4 (T4, De Lampionkust) and zone 5 (T5, Hoogvads)
        g("zeekraalsalade", 4, Stats.van(Stat.ELEMENTWEERSTAND, 8, Stat.VADS, 260), 1, 0, 1, false);
        g("koraalvissoep", 4, Stats.van(Stat.VERDEDIGING, 18, Stat.VADS, 280), 1, 0, 1, false);
        g("zeemanshapje", 4, new Stats(), 1.15, 0.15, 1, false);
        g("lampionspiesjes", 4, Stats.van(Stat.KRITIEK, 4, Stat.KRACHT, 9), 1, 0, 1, false);
        g("wolkbessentaart", 5, Stats.van(Stat.HEALKRACHT, 7, Stat.WIJSHEID, 14, Stat.VADS, 320), 1, 0, 1, false);
        g("donderstoofpot", 5, Stats.van(Stat.VERDEDIGING, 22, Stat.VADS, 360), 1, 0, 1, false);
        g("hemelfeestmaal", 5, Stats.van(Stat.VADS, 300, Stat.KRACHT, 9, Stat.BEHENDIGHEID, 9, Stat.WIJSHEID, 9), 1, 0, 1, true);
    }

    private Eten() {
    }

    private static void g(String id, int tier, Stats s, double axp, double dubbel, double gxp, boolean feest) {
        Gerecht g = new Gerecht(id, tier, s, axp, dubbel, gxp, feest);
        GERECHTEN.put(id, g);
        ITEM.put(id, Materialen.ITEMS.registerItem(g.itemId(), p -> new EtenItem(p, g), () -> new Item.Properties().stacksTo(16)
                .component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(1.6f).animation(ItemUseAnimation.EAT)
                        .sound(SoundEvents.GENERIC_EAT).hasConsumeParticles(true).build())));
    }

    // ---- the buff ----------------------------------------------------------------------------------------------------------

    @Nullable
    public static Gerecht actief(Player p) {
        CompoundTag b = AmbachtData.tag(p).getCompoundOrEmpty("buff");
        if (b.isEmpty() || b.getLongOr("tot", 0L) <= AmbachtData.nu()) {
            return null;
        }
        return GERECHTEN.get(b.getStringOr("id", ""));
    }

    static double sterkte(Player p) {
        return 1 + 0.3 * AmbachtData.tag(p).getCompoundOrEmpty("buff").getIntOr("vads", 0) / 100.0;
    }

    public static boolean gezellig(Player p) {
        return AmbachtData.tag(p).getLongOr("gezellig", 0L) > AmbachtData.nu();
    }

    /** Kern {@code Spelers.EXTRA_STATS}: the food buff (x its Vadsigheid) + Gezellig. */
    public static Stats stats(ServerPlayer p) {
        Stats s = new Stats();
        Gerecht g = actief(p);
        if (g != null) {
            s.plus(g.stats().kopie().maal(sterkte(p)));
        }
        if (gezellig(p)) {
            s.plus(GEZELLIG);
        }
        return s;
    }

    public static double xpFactor(Player p) {
        Gerecht g = actief(p);
        return g == null ? 1 : g.ambachtXp();
    }

    public static double dubbelKans(Player p) {
        Gerecht g = actief(p);
        return g == null ? 0 : g.dubbel();
    }

    /** Kern {@code Spelers.XP_FACTOR}: the Ridderontbijt's +5 % combat xp. */
    public static double gevechtXp(ServerPlayer p) {
        Gerecht g = actief(p);
        return g == null ? 1 : g.gevechtXp();
    }

    /** Eat: the buff replaces the old one (30 min). A feast: for every party member near you, plus Gezellig. */
    public static void eet(ServerPlayer p, Gerecht g, int vadsigheid) {
        if (g.feest()) {
            for (ServerPlayer l : Partijen.ledenInBuurt(p, FEEST_STRAAL)) {
                zet(l, g, vadsigheid);
                AmbachtData.tag(l).putLong("gezellig", AmbachtData.nu() + DUUR_MS);
                if (l != p) {
                    l.sendSystemMessage(Component.translatable("vadsrijk.ambacht.eten.feest_mee", p.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        } else {
            zet(p, g, vadsigheid);
        }
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(g.feest() ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.2, p.getZ(), 10, 0.4, 0.4, 0.4, 0.02);
            sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5f, 1.3f);
        }
        p.sendOverlayMessage(Component.translatable("vadsrijk.ambacht.eten.op", Component.translatable("item.guhs." + g.itemId()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        Spelers.sync(p);
        Verzamelen.sync(p, true);
    }

    static void zet(ServerPlayer p, Gerecht g, int vadsigheid) {
        CompoundTag b = new CompoundTag();
        b.putString("id", g.id());
        b.putLong("tot", AmbachtData.nu() + DUUR_MS);
        b.putInt("vads", vadsigheid);
        AmbachtData.tag(p).put("buff", b);
        Spelers.sync(p);
    }

    /** The HUD part of the sync: the buff and how long it still lasts. */
    static void hud(ServerPlayer p, CompoundTag d) {
        Gerecht g = actief(p);
        if (g != null) {
            d.putString("buff", g.id());
            d.putLong("buffOver", (AmbachtData.tag(p).getCompoundOrEmpty("buff").getLongOr("tot", 0L) - AmbachtData.nu()) / 1000);
        }
        if (gezellig(p)) {
            d.putLong("gezellig", (AmbachtData.tag(p).getLongOr("gezellig", 0L) - AmbachtData.nu()) / 1000);
        }
    }

    /** A dish. Only works in the Vadsrijk. */
    public static class EtenItem extends Item {
        public final Gerecht gerecht;

        public EtenItem(Properties p, Gerecht g) {
            super(p);
            this.gerecht = g;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!Vadsrijk.in(player)) {
                if (player instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.translatable("vadsrijk.ambacht.alleen_rijk").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                return InteractionResult.FAIL;
            }
            return super.use(level, player, hand);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (entity instanceof ServerPlayer sp && Vadsrijk.in(sp)) {
                eet(sp, gerecht, Gemaakt.vadsigheid(stack));
            }
            return super.finishUsingItem(stack, level, entity);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
            double f = Gemaakt.factor(stack);
            gerecht.stats().alles().forEach((st, v) -> t.accept(Component.translatable("vadsrijk.ambacht.eten.stat", Component.translatable(st.key()),
                    st.procent ? String.format(java.util.Locale.ROOT, "%.1f%%", v * f) : String.valueOf(Math.round(v * f))).withStyle(ChatFormatting.GREEN)));
            if (gerecht.ambachtXp() > 1) {
                t.accept(Component.translatable("vadsrijk.ambacht.eten.ambachtxp", Math.round((gerecht.ambachtXp() - 1) * 100)).withStyle(ChatFormatting.GREEN));
                t.accept(Component.translatable("vadsrijk.ambacht.eten.dubbel", Math.round(gerecht.dubbel() * 100)).withStyle(ChatFormatting.GREEN));
            }
            if (gerecht.gevechtXp() > 1) {
                t.accept(Component.translatable("vadsrijk.ambacht.eten.gevechtxp", Math.round((gerecht.gevechtXp() - 1) * 100)).withStyle(ChatFormatting.GREEN));
            }
            t.accept(Component.translatable(gerecht.feest() ? "vadsrijk.ambacht.eten.feest" : "vadsrijk.ambacht.eten.duur").withStyle(ChatFormatting.GRAY));
            Gemaakt.tooltip(stack, t);
            t.accept(Component.translatable("vadsrijk.economie.alleen_rijk").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
