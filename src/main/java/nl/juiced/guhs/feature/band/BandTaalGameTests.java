package nl.juiced.guhs.feature.band;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.taal.Tekst;

/**
 * 1.2.0: the band's saved names and diary lines are Components now. Data from before 1.2.0 (Strings, resolved in Dutch)
 * still loads: an unnamed guh's saved variant name is dropped (shown from its looks in any language), everything else
 * stays a literal. Batch "taal" (with taal/TaalGameTests).
 */
public final class BandTaalGameTests {
    private static final String EMPTY = "empty";

    private static String key(Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getKey() : null;
    }

    private static CompoundTag oud(String naam, CompoundTag looks) {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, UUID.randomUUID());
        t.putBoolean("Guh", true);
        t.putString("Naam", naam);
        t.put("Looks", looks);
        return t;
    }

    @GuhTest(template = EMPTY, batch = "taal")
    public static void bandTaalOudeNamen(GameTestHelper helper) {
        CompoundTag looks = new CompoundTag();
        looks.putString("Variant", "mint");
        BandData.Rec naamloos = BandData.Rec.load(oud("Mintguh", looks));
        helper.assertTrue(Tekst.empty(naamloos.naam) && "entity.guhs.guh.mint".equals(key(naamloos.weergave())),
                "an unnamed guh: its old (Dutch) variant name is dropped, shown from its looks: " + naamloos.weergave());
        CompoundTag looksNaam = looks.copy();
        looksNaam.putString("Naam", "Knabbeltje");
        BandData.Rec genoemd = BandData.Rec.load(oud("Knabbeltje", looksNaam));
        helper.assertTrue(genoemd.naam.equals(Component.literal("Knabbeltje")) && genoemd.weergave().equals(Component.literal("Knabbeltje")),
                "a named guh keeps its name");
        BandData.Rec maatje = BandData.Rec.load(oud("Piep", new CompoundTag()));
        helper.assertTrue(maatje.weergave().equals(Component.literal("Piep")), "no looks: the name stays");
        // a translatable name survives saving
        genoemd.naam = Component.translatable("entity.guhs.guh_npc.reisguh");
        helper.assertTrue("entity.guhs.guh_npc.reisguh".equals(key(BandData.Rec.load(genoemd.save()).naam)), "a translatable name is saved as one");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = "taal")
    public static void bandTaalWistJeDatEnPlek(GameTestHelper helper) {
        CompoundTag looks = new CompoundTag();
        looks.putString("Variant", "normal");
        CompoundTag t = oud("Guh", looks);
        // an old wist-je-datje: plain String args
        ListTag wist = new ListTag();
        CompoundTag w = new CompoundTag();
        w.putString("Key", "gui.guhs.wistjedat.band.getemd");
        ListTag args = new ListTag();
        args.add(StringTag.valueOf("Juiced"));
        w.put("Args", args);
        w.putLong("Dag", 3);
        wist.add(w);
        t.put("Wist", wist);
        // an old place: a String detail
        CompoundTag plek = new Plek(PlekSoort.HUISJE, Level.OVERWORLD, BlockPos.ZERO, "Villa Vads", 5).save();
        t.put("Plek", plek);
        BandData.Rec r = BandData.Rec.load(t);
        helper.assertTrue(r.wist.size() == 1 && r.wist.get(0).args().equals(List.of(Component.literal("Juiced"))), "old args are literals");
        helper.assertTrue(r.plek.detail().equals(Component.literal("Villa Vads")), "an old detail is a literal");
        // new ones are Components, and stay so
        r.wist.add(0, new BandData.WistJeDat("gui.guhs.wistjedat.favorietjes.knuffel.0", List.of(Component.translatable("item.guhs.guh_plush")), 4));
        r.plek = new Plek(PlekSoort.HUISJE, Level.OVERWORLD, BlockPos.ZERO, Component.translatable("gui.guhs.huisje.standaardnaam.1"), 6);
        BandData.Rec terug = BandData.Rec.load(r.save());
        helper.assertTrue("item.guhs.guh_plush".equals(key(terug.wist.get(0).args().get(0))) && terug.wist.get(1).args().get(0).equals(Component.literal("Juiced")),
                "the args survive saving");
        helper.assertTrue("gui.guhs.huisje.standaardnaam.1".equals(key(terug.plek.detail())), "the detail survives saving");
        Component regel = Component.translatable(terug.wist.get(0).key(), terug.wist.get(0).args().toArray());
        helper.assertTrue(regel.getContents() instanceof TranslatableContents tc && tc.getArgs()[0] instanceof Component, "the screen gets a Component arg");
        helper.succeed();
    }
}
