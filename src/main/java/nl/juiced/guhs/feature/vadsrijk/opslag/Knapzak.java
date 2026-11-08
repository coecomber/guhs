package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;

/**
 * T-OPSLAG: the Knapzak (VOORSTEL_ROADMAP §6.2, R5 "virtueel"): extra inventory tabs, PER CHARACTER, opened anywhere in the
 * Vadsrijk with I (key {@code key.guhs.vr_knapzak}) or {@code /guhs vadsrijk opslag knapzak}.
 * Tab 1 (27 slots) is free; every unlock source gives one more tab, up to {@link #MAX_TABS}:
 * <ol>
 *   <li>the zone-1 story done ({@code zone1_klaar});</li>
 *   <li>the zone-2 story done ({@code zone2_klaar});</li>
 *   <li>bought with coins (click the locked tab, then again to confirm). U-OPSLAGVIP: the price curve and how many can be bought
 *   live in {@link OpslagBalans} ({@code [opslag]} in the balance file; default only the FIRST one, 2500 brons); further tabs
 *   that nothing can unlock yet show as "Binnenkort!";</li>
 *   <li>any other source through {@link #ontgrendel}(p, bron) (quest rewards of later zones, achievements).</li>
 * </ol>
 */
public final class Knapzak {
    public static final int VAKKEN = 27, MAX_TABS = 5;
    /** U-OPSLAGVIP: the price of the NEXT bought tab for this character (0 = none can be bought now). */
    public static long prijs(ServerPlayer p) {
        return koopbaar(p) ? OpslagBalans.knapzakPrijs(zak(p).gekochtAantal()) : 0;
    }

    /** U-OPSLAGVIP: can this character buy one more tab now? (under {@code max_koopbaar} and below {@link #MAX_TABS}) */
    public static boolean koopbaar(ServerPlayer p) {
        return zak(p).gekochtAantal() < OpslagBalans.maxKoopbaar() && tabs(p) < MAX_TABS;
    }

    /**
     * U-OPSLAGVIP: the first tab index that nothing can unlock yet ("Binnenkort!"): open tabs + story sources still to come +
     * purchases still allowed. (Later quest rewards through {@link #ontgrendel} don't exist yet, so they are "binnenkort" too.)
     */
    public static int binnenkortVanaf(ServerPlayer p) {
        OpslagData.Zak z = zak(p);
        int nog = Math.max(0, OpslagBalans.maxKoopbaar() - z.gekochtAantal());
        for (Bron b : BRONNEN) {
            if (!z.bronnen.contains(b.id()) && !b.heeft().test(p)) {
                nog++;
            }
        }
        return Math.min(MAX_TABS, tabs(p) + nog);
    }
    /** The built-in unlock conditions (each counts once); later zones add theirs with {@link #ontgrendel}. */
    public record Bron(String id, Predicate<ServerPlayer> heeft) {
    }

    public static final List<Bron> BRONNEN = new java.util.concurrent.CopyOnWriteArrayList<>(List.of(
            new Bron("zone1", p -> nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.zone1Klaar(p)),
            new Bron("zone2", p -> nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3Reizen.zone2Klaar(p))));

    private Knapzak() {
    }

    public static OpslagData.Zak zak(ServerPlayer p) {
        return Opslag.data(p).zak(Opslag.KARAKTER.apply(p));
    }

    /** How many tabs this character has open (1..MAX_TABS). */
    public static int tabs(ServerPlayer p) {
        OpslagData.Zak z = zak(p);
        int n = 1 + z.gekochtAantal() + z.bronnen.size();
        for (Bron b : BRONNEN) {
            if (!z.bronnen.contains(b.id()) && b.heeft().test(p)) {
                n++;
            }
        }
        return Math.min(MAX_TABS, n);
    }

    /** A reward: one more tab for this character (each source id counts once). Returns false when it already counted. */
    public static boolean ontgrendel(ServerPlayer p, String bron) {
        OpslagData.Zak z = zak(p);
        if (!z.bronnen.add(bron)) {
            return false;
        }
        p.sendSystemMessage(Component.translatable("vadsrijk.opslag.knapzak.nieuw_vak", tabs(p)).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8f, 1.2f);
        return true;
    }

    /** Opens the knapzak (only in the Vadsrijk with the Vadsrijk inventory). */
    public static boolean open(ServerPlayer p) {
        return open(p, 0);
    }

    public static boolean open(ServerPlayer p, int tab) {
        if (!Opslag.magOpenen(p)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.opslag.alleen_rijk").withStyle(ChatFormatting.GRAY));
            return false;
        }
        OpslagData.Zak z = zak(p);
        CompoundTag info = info(p, z);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new OpslagMenu(id, inv, OpslagMenu.KNAPZAK, 3, MAX_TABS, z::tab,
                () -> tabs(p), Opslag::magOpenen, info, () -> koop(p), tab), Component.translatable("vadsrijk.opslag.knapzak")),
                buf -> OpslagMenu.schrijf(buf, OpslagMenu.KNAPZAK, 3, MAX_TABS, info));
        Tips.toon(p, "vadsrijk.opslag.tip.knapzak");
        return true;
    }

    /** What the screen shows on a locked tab: each way to unlock, done or not, and the price when buying is still possible. */
    static CompoundTag info(ServerPlayer p, OpslagData.Zak z) {
        CompoundTag t = new CompoundTag();
        ListTag regels = new ListTag();
        for (Bron b : BRONNEN) {
            regels.add(StringTag.valueOf((b.heeft().test(p) || z.bronnen.contains(b.id()) ? "1" : "0") + "vadsrijk.opslag.knapzak.bron." + b.id()));
        }
        regels.add(StringTag.valueOf((z.gekochtAantal() > 0 ? "1" : "0") + "vadsrijk.opslag.knapzak.bron.kopen"));
        regels.add(StringTag.valueOf((z.bronnen.isEmpty() ? "0" : "1") + "vadsrijk.opslag.knapzak.bron.later"));
        t.put("regels", regels);
        t.putLong("prijs", prijs(p));
        t.putLong("koop_prijs", OpslagBalans.knapzakPrijs(Math.max(0, Math.min(z.gekochtAantal(), OpslagBalans.maxKoopbaar() - 1))));   // U-OPSLAGVIP
        t.putInt("binnenkort", binnenkortVanaf(p));   // U-OPSLAGVIP
        t.putBoolean("vip", Opslag.data(p).vipGrapGezien);   // U-OPSLAGVIP
        if (VipGrap.pakMeteen(p)) {
            t.putBoolean("vip_nu", true);   // U-OPSLAGVIP (dev: start the joke at once)
        }
        return t;
    }

    /** Buying the coin tab (the screen asks twice). Re-opens the screen on the same tab so the new tab shows. */
    static void koop(ServerPlayer p) {
        OpslagData.Zak z = zak(p);
        int tab = p.containerMenu instanceof OpslagMenu m ? m.huidigeTab() : 0;
        if (!koopbaar(p)) {
            return;
        }
        long prijs = prijs(p);
        if (!Spelers.neemMunten(p, prijs)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.opslag.te_weinig", Spelers.munten(prijs)).withStyle(ChatFormatting.RED));
            return;
        }
        z.gekochtN = z.gekochtAantal() + 1;   // U-OPSLAGVIP
        z.gekocht = true;
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.6f, 1.4f);
        p.sendSystemMessage(Component.translatable("vadsrijk.opslag.knapzak.gekocht").withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().getServer().execute(() -> open(p, tab));
    }
}
