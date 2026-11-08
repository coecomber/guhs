package nl.juiced.guhs.feature.vadsrijk.opslag;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.storage.Nbt;

/**
 * U-OPSLAGVIP: only the first bought storage expansion is for sale (price curve + "Binnenkort!"), and the VIP joke flag is
 * set once per account. Run: {@code runGameTestServer -Pgt=VadsrijkUOpslagVip}.
 */
public class VadsrijkUOpslagVipGameTests {
    private static final String BATCH = "vadsrijk_u_opslagvip";

    @GuhTest(template = "empty", batch = BATCH)
    public static void alleenEersteKoopbaar(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        OpslagBalans.standaardAlles();
        try {
            Spelers.geefMunten(p, 1_000_000);
            long start = Spelers.data(p).munten;
            // knapzak: the first tab costs the old price, then nothing more is for sale
            helper.assertTrue(Knapzak.koopbaar(p) && Knapzak.prijs(p) == 2500, "first knapzak tab for sale at 2500: " + Knapzak.prijs(p));
            helper.assertTrue(Knapzak.binnenkortVanaf(p) == Knapzak.tabs(p) + 1 + 2, "binnenkort after: open + 1 buy + 2 story tabs");
            Knapzak.koop(p);
            OpslagData.Zak z = Knapzak.zak(p);
            helper.assertTrue(z.gekochtAantal() == 1 && Knapzak.tabs(p) == 2 && Spelers.data(p).munten == start - 2500, "bought 1 knapzak tab");
            helper.assertTrue(!Knapzak.koopbaar(p) && Knapzak.prijs(p) == 0, "the second knapzak tab is not for sale");
            Knapzak.koop(p);
            helper.assertTrue(z.gekochtAantal() == 1 && Spelers.data(p).munten == start - 2500, "a second buy does nothing");
            CompoundTag info = Knapzak.info(p, z);
            helper.assertTrue(info.getLongOr("prijs", -1) == 0 && info.getIntOr("binnenkort", -1) == 4, "screen info: no price, tab 5 binnenkort");
            // bank: the same
            helper.assertTrue(Bank.koopbaar(p) && Bank.prijs(p) == 1000 && Bank.binnenkortVanaf(p) == 2, "first bank tab for sale at 1000");
            Bank.koop(p, p.position());
            helper.assertTrue(Opslag.data(p).bankGekocht == 1 && Bank.tabs(p) == 2 && !Bank.koopbaar(p) && Bank.binnenkortVanaf(p) == 2,
                    "bank: 1 bought, the rest binnenkort");
            Bank.koop(p, p.position());
            helper.assertTrue(Opslag.data(p).bankGekocht == 1 && Spelers.data(p).munten == start - 3500, "a second bank buy does nothing");
            // the curve: opening a second purchase in the config needs no code, price = basis x factor^n
            OpslagBalans.zet("max_koopbaar", 2);
            helper.assertTrue(Knapzak.koopbaar(p) && Knapzak.prijs(p) == 10000, "2nd knapzak tab = 2500 x 4: " + Knapzak.prijs(p));
            helper.assertTrue(Bank.koopbaar(p) && Bank.prijs(p) == 4000, "2nd bank tab = 1000 x 4: " + Bank.prijs(p));
            OpslagBalans.zet("knapzak_factor", 10);
            helper.assertTrue(OpslagBalans.knapzakPrijs(0) == 2500 && OpslagBalans.knapzakPrijs(2) == 250_000, "curve with factor 10");
            // an old save (T-OPSLAG's boolean) counts as one bought tab
            OpslagData.Zak oud = new OpslagData.Zak();
            oud.gekocht = true;
            helper.assertTrue(oud.gekochtAantal() == 1, "old boolean = 1 bought");
        } finally {
            OpslagBalans.standaardAlles();
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void vipVlagEenKeerPerAccount(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        var oud = Opslag.KARAKTER;
        try {
            OpslagData d = Opslag.data(p);
            helper.assertTrue(!d.vipGrapGezien && !Knapzak.info(p, Knapzak.zak(p)).getBooleanOr("vip", true), "a new account has not seen the joke");
            OpslagData.Zak z = Knapzak.zak(p);
            p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new OpslagMenu(id, inv, OpslagMenu.KNAPZAK, 3, Knapzak.MAX_TABS, z::tab,
                    () -> 1, sp -> true, new CompoundTag(), null, 0), Component.literal("test")));
            OpslagMenu m = (OpslagMenu) p.containerMenu;
            helper.assertTrue(m.clickMenuButton(p, OpslagMenu.KNOP_VIP_GEZIEN) && d.vipGrapGezien, "the client's button sets the flag");
            helper.assertTrue(Knapzak.info(p, z).getBooleanOr("vip", false) && Bank.info(p).getBooleanOr("vip", false), "knapzak + bank both know");
            Opslag.KARAKTER = sp -> "2";
            helper.assertTrue(Knapzak.info(p, Knapzak.zak(p)).getBooleanOr("vip", false), "another character: still seen (per account)");
            // kept over save + load
            OpslagData t = new OpslagData();
            t.deserialize(Nbt.input(p.registryAccess(), Nbt.write(p.registryAccess(), d::serialize)));
            helper.assertTrue(t.vipGrapGezien, "the flag survives save + load");
            // the dev command path
            VipGrap.reset(p);
            VipGrap.meteen(p);
            CompoundTag info = Knapzak.info(p, Knapzak.zak(p));
            helper.assertTrue(!info.getBooleanOr("vip", true) && info.getBooleanOr("vip_nu", false), "reset + start at once");
            helper.assertTrue(!Knapzak.info(p, Knapzak.zak(p)).getBooleanOr("vip_nu", false), "'at once' only for one open");
            p.closeContainer();
        } finally {
            Opslag.KARAKTER = oud;
        }
        helper.succeed();
    }
}
