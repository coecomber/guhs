package nl.juiced.guhs.feature.guhpixel.kantoor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Plek;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.Roepen;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxTest;
import nl.juiced.guhs.feature.guhpixel.Winkel;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.taal.Tekst;

/**
 * Game tests of the Guhkantoor (batch px_kantoor): desks and clocks connecting, clocking in and out (the very same guh
 * comes back, never doubled), the 8 real hours through the Klok, loonstrookjes with their cap and the kwartaalrapport,
 * two players at one clock, papers on the wall keeping their text, the text pools, the Werknemer van de maand, a guh
 * whose desk vanished, the shop offers.
 */
public final class PxKantoorGameTests {
    private static final String BATCH = "px_kantoor";
    private static final String KAMER = "guhkantoor_test_kantoor";
    private static final BlockPos KLOK = new BlockPos(8, 2, 10);

    private static BlockPos klok(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(KLOK);
        KantoorSlice.zetNeer(helper.getLevel(), pos, KantoorSlice.PRIKKLOK.get().defaultBlockState());
        return pos;
    }

    private static BlockPos bureau(GameTestHelper helper, int x, int z) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, 2, z));
        KantoorSlice.zetNeer(helper.getLevel(), pos, KantoorSlice.BUREAUTJE.get().defaultBlockState());
        return pos;
    }

    private static GuhEntity guh(GameTestHelper helper, ServerPlayer p, String naam, int x) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(x, 2, 4));
        guh.tame(p);
        guh.setCustomName(Component.literal(naam));
        GuhVolger.zet(guh, PlekSoort.WERELD, "");
        return guh;
    }

    private static int tel(ServerPlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) {
                n += p.getInventory().getItem(i).getCount();
            }
        }
        return n;
    }

    private static ItemStack eerste(ServerPlayer p, Item item) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) {
                return p.getInventory().getItem(i);
            }
        }
        return ItemStack.EMPTY;
    }

    private static int inWereld(GameTestHelper helper, UUID id) {
        return helper.getLevel().getEntitiesOfClass(GuhEntity.class, helper.getBounds().inflate(4), g -> g.getUUID().equals(id)).size();
    }

    private static boolean heeftAdv(ServerPlayer p, String naam) {
        AdvancementHolder h = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return h != null && p.getAdvancements().getOrStartProgress(h).isDone();
    }

    private static void ruimOp(GameTestHelper helper, ServerPlayer... spelers) {
        KantoorData data = KantoorData.get(helper.getLevel().getServer());
        for (ServerPlayer p : spelers) {
            for (KantoorData.Werk w : data.vanEigenaar(p.getUUID())) {
                data.weg(w.guh);
            }
        }
        for (GuhEntity g : helper.getLevel().getEntitiesOfClass(GuhEntity.class, helper.getBounds().inflate(4))) {
            g.discard();
        }
        PxTest.klaar(helper, spelers);
    }

    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void inklokkenEnLoonNaAchtUur(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        BlockPos klok = klok(helper), bureau = bureau(helper, 6, 10);
        helper.assertTrue(Kantoor.klokVan(level, bureau) != null, "the desk connected to the clock");
        GuhEntity guh = guh(helper, p, "Vadsje", 4), ander = guh(helper, p, "Njegje", 6);
        UUID id = guh.getUUID();
        helper.assertTrue(Kantoor.klokIn(p, level, bureau, UUID.randomUUID()) != null, "an unknown guh cannot clock in");
        helper.assertTrue(Kantoor.klokIn(p, level, bureau, id) == null, "the guh clocks in");
        KantoorData data = KantoorData.get(level.getServer());
        KantoorData.Werk w = data.van(id);
        helper.assertTrue(guh.isRemoved() && level.getEntity(id) == null && w != null && w.pos.equals(bureau), "the guh is stored, not in the world");
        Plek plek = GuhVolger.plek(level.getServer(), p.getUUID(), id);
        helper.assertTrue(plek != null && plek.soort() == PlekSoort.OP_KANTOOR && plek.pos().equals(bureau), "'Waar is mijn guh?' says at the office: " + plek);
        helper.assertTrue(Roepen.roep(p, id) == Roepen.Uitkomst.NIET_THUIS, "it cannot be called away from work");
        BureautjeBlockEntity be = (BureautjeBlockEntity) level.getBlockEntity(bureau);
        helper.assertTrue(be.bezet() && id.equals(be.guh()) && level.getBlockState(bureau).getValue(BureautjeBlock.BEZET)
                && be.naam().getString().equals("Vadsje") && !be.looks().isEmpty(), "the desk shows the sleeping copy");
        helper.assertTrue(Kantoor.klokIn(p, level, bureau, ander.getUUID()) != null && !ander.isRemoved(), "one guh per desk");

        // 8 real hours
        helper.assertTrue(Kantoor.wachtend(w) == 0, "nothing yet");
        PxTest.spoel(7.9);
        helper.assertTrue(Kantoor.wachtend(w) == 0, "not after 7.9 hours");
        Kantoor.haalLoon(p, level, bureau);
        helper.assertTrue(tel(p, KantoorSlice.LOONSTROOKJE_ITEM.get()) == 0 && !heeftAdv(p, "guhkantoor_loonstrookje"), "no payslip before the shift is over");
        PxTest.spoel(0.2);
        helper.assertTrue(Kantoor.wachtend(w) == 1, "one after 8.1 hours");
        Kantoor.haalLoon(p, level, bureau);
        ItemStack strook = eerste(p, KantoorSlice.LOONSTROOKJE_ITEM.get());
        CompoundTag t = Papier.tag(strook);
        helper.assertTrue(tel(p, KantoorSlice.LOONSTROOKJE_ITEM.get()) == 1 && Papier.soort(t) == Papier.Soort.LOON && t.getIntOr("Nr", 0) == 1
                && Tekst.get(t, "Naam").getString().equals("Vadsje") && !t.getStringOr("Datum", "").isEmpty(), "the payslip: " + t);
        helper.assertTrue(Kantoor.loonstrookjes(p) == 1 && Kantoor.wachtend(w) == 0 && heeftAdv(p, "guhkantoor_loonstrookje"), "counted, FTB advancement");
        helper.assertTrue(KantoorTeksten.blad(t).size() >= 15, "it can be read");

        // a long time away: at most three wait; the third of a player brings the kwartaalrapport
        PxTest.spoel(8 * 5 + 1);
        helper.assertTrue(Kantoor.wachtend(w) == Kantoor.POSTVAK, "the postvak holds three");
        Kantoor.haalLoon(p, level, bureau);
        helper.assertTrue(tel(p, KantoorSlice.LOONSTROOKJE_ITEM.get()) == 4 && Kantoor.loonstrookjes(p) == 4, "three more");
        ItemStack rapport = eerste(p, KantoorSlice.KWARTAALRAPPORT_ITEM.get());
        helper.assertTrue(tel(p, KantoorSlice.KWARTAALRAPPORT_ITEM.get()) == 1 && Kantoor.rapporten(p) == 1 && heeftAdv(p, "guhkantoor_kwartaal")
                && Papier.tag(rapport).getListOrEmpty("Medewerkers").size() == 1, "a kwartaalrapport with the third payslip");
        helper.assertTrue(Kantoor.wachtend(w) == 0, "the overflow is gone, the clock runs on");
        helper.assertTrue(Muntjes.saldo(p) == 0 && Muntjes.totaal(p) == 0, "no muntjes from the office");

        // what the screen gets
        CompoundTag stand = Kantoor.stand(p, level, (PrikklokBlockEntity) level.getBlockEntity(klok), null);
        CompoundTag rij = stand.getListOrEmpty("Bureaus").getCompoundOrEmpty(0);
        helper.assertTrue(stand.getListOrEmpty("Bureaus").size() == 1 && rij.getBooleanOr("Bezet", false) && rij.getBooleanOr("Mijn", false)
                && rij.getLongOr("Verstreken", 0) > 40 * Klok.UUR && rij.getLongOr("Tot", 0) > 0 && rij.getLongOr("Tot", 0) <= Kantoor.DIENST, "the desk row: " + rij);
        boolean werktUit = false, anderVrij = false;
        for (Tag raw : stand.getListOrEmpty("Guhs")) {
            CompoundTag g = (CompoundTag) raw;
            UUID gid = g.read("Id", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
            werktUit |= id.equals(gid) && !Tekst.empty(Tekst.get(g, "Uit"));
            anderVrij |= ander.getUUID().equals(gid) && Tekst.empty(Tekst.get(g, "Uit"));
        }
        helper.assertTrue(werktUit && anderVrij, "the picker: the working guh is greyed out, the other one can be picked");
        helper.assertTrue(stand.getCompoundOrEmpty("Maand").getIntOr("Uren", 0) >= 48, "the Werknemer van de maand is on the screen");

        // clocking out: the very same guh, exactly once
        helper.assertTrue(Kantoor.klokUit(level, bureau, level.getBlockState(bureau), p, false), "clocked out");
        Entity terug = level.getEntity(id);
        helper.assertTrue(terug instanceof GuhEntity g && g.isTame() && p.getUUID().equals(g.getOwnerUUID()) && g.getName().getString().equals("Vadsje")
                && inWereld(helper, id) == 1, "the very same guh is back, once");
        helper.assertTrue(data.van(id) == null && !be.bezet() && !level.getBlockState(bureau).getValue(BureautjeBlock.BEZET), "the desk is free");
        helper.assertTrue(!Kantoor.klokUit(level, bureau, level.getBlockState(bureau), p, false) && inWereld(helper, id) == 1, "never doubled");
        ruimOp(helper, p);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void bureauOfKlokWegGuhTerug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        BlockPos klok = klok(helper), een = bureau(helper, 6, 10), twee = bureau(helper, 10, 10);
        GuhEntity a = guh(helper, p, "Vadsje", 4), b = guh(helper, p, "Njegje", 6);
        UUID ida = a.getUUID(), idb = b.getUUID();
        helper.assertTrue(Kantoor.klokIn(p, level, een, ida) == null && Kantoor.klokIn(p, level, twee, idb) == null, "both at work");
        KantoorData data = KantoorData.get(level.getServer());
        helper.assertTrue(data.vanEigenaar(p.getUUID()).size() == 2 && inWereld(helper, ida) == 0 && inWereld(helper, idb) == 0, "two records, no entities");

        level.destroyBlock(een, false);
        helper.assertTrue(inWereld(helper, ida) == 1 && data.van(ida) == null && data.van(idb) != null, "breaking a desk sends its guh home, only that one");
        helper.assertTrue(((PrikklokBlockEntity) level.getBlockEntity(klok)).bureaus().equals(List.of(twee)), "the clock forgot the desk");
        helper.assertTrue(tel(p, KantoorSlice.LOONSTROOKJE_ITEM.get()) == 0, "no shift finished: no payslip");

        PxTest.spoel(8.5);
        level.setBlock(klok, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(inWereld(helper, idb) == 1 && data.van(idb) == null, "breaking the clock sends every guh home");
        helper.assertTrue(tel(p, KantoorSlice.LOONSTROOKJE_ITEM.get()) == 1, "with the payslip of the finished shift");
        BureautjeBlockEntity be = (BureautjeBlockEntity) level.getBlockEntity(twee);
        helper.assertTrue(be != null && !be.bezet() && be.klok() == null && Kantoor.klokVan(level, twee) == null, "the desk is loose and empty");
        helper.assertTrue(data.vanEigenaar(p.getUUID()).isEmpty(), "nothing left behind");
        // a new clock adopts the loose desk
        BlockPos nieuw = klok(helper);
        helper.assertTrue(Kantoor.klokVan(level, twee) != null && nieuw.equals(be.klok()), "a new clock picks the desk up");
        ruimOp(helper, p);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void tweeSpelersEenKlok(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p1 = PxTest.speler(helper), p2 = PxTest.speler(helper);
        BlockPos klok = klok(helper), een = bureau(helper, 6, 10), twee = bureau(helper, 10, 10);
        GuhEntity a = guh(helper, p1, "Vadsje", 4), b = guh(helper, p2, "Njegje", 6);
        UUID ida = a.getUUID(), idb = b.getUUID();
        helper.assertTrue(Kantoor.klokIn(p1, level, een, idb) != null && !b.isRemoved(), "not with somebody else's guh");
        helper.assertTrue(Kantoor.klokIn(p1, level, een, ida) == null && Kantoor.klokIn(p2, level, twee, idb) == null, "each their own guh, one clock");
        KantoorData data = KantoorData.get(level.getServer());
        Kantoor.actie(p2, klok, Kantoor.KLOK_UIT, 0, Util.NIL_UUID);
        helper.assertTrue(data.van(ida) != null && inWereld(helper, ida) == 0, "the other player cannot send your guh home");
        PxTest.spoel(8.1);
        Kantoor.actie(p2, klok, Kantoor.LOON, 0, Util.NIL_UUID);
        helper.assertTrue(tel(p2, KantoorSlice.LOONSTROOKJE_ITEM.get()) == 0 && Kantoor.wachtend(data.van(ida)) == 1, "nor take its payslip");
        Kantoor.actie(p1, klok, Kantoor.LOON, 0, Util.NIL_UUID);
        Kantoor.actie(p2, klok, Kantoor.LOON, 1, Util.NIL_UUID);
        helper.assertTrue(tel(p1, KantoorSlice.LOONSTROOKJE_ITEM.get()) == 1 && tel(p2, KantoorSlice.LOONSTROOKJE_ITEM.get()) == 1
                && Kantoor.loonstrookjes(p1) == 1 && Kantoor.loonstrookjes(p2) == 1, "each exactly their own payslip");
        helper.assertTrue(Tekst.get(Papier.tag(eerste(p1, KantoorSlice.LOONSTROOKJE_ITEM.get())), "Naam").getString().equals("Vadsje")
                && Tekst.get(Papier.tag(eerste(p2, KantoorSlice.LOONSTROOKJE_ITEM.get())), "Naam").getString().equals("Njegje"), "with their own guh's name");
        CompoundTag stand = Kantoor.stand(p2, level, (PrikklokBlockEntity) level.getBlockEntity(klok), null);
        helper.assertTrue(!stand.getListOrEmpty("Bureaus").getCompoundOrEmpty(0).getBooleanOr("Mijn", true)
                && stand.getListOrEmpty("Bureaus").getCompoundOrEmpty(1).getBooleanOr("Mijn", false), "the screen knows whose guh is whose");
        Kantoor.actie(p1, klok, Kantoor.KLOK_UIT, 0, Util.NIL_UUID);
        Kantoor.actie(p2, klok, Kantoor.KLOK_UIT, 1, Util.NIL_UUID);
        helper.assertTrue(inWereld(helper, ida) == 1 && inWereld(helper, idb) == 1 && data.van(ida) == null && data.van(idb) == null, "both home again");
        Kantoor.actie(p1, klok.offset(0, 40, 0), Kantoor.VERVERS, 0, Util.NIL_UUID);   // (no clock there, too far: ignored)
        Kantoor.actie(p1, klok, Kantoor.KLOK_IN, 7, ida);                               // (no such desk: ignored)
        helper.assertTrue(data.van(ida) == null, "nonsense from a client does nothing");
        ruimOp(helper, p1, p2);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void vierBureausPerKlok(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // desks first, then the clock: it adopts the four nearest
        BlockPos[] bureaus = {bureau(helper, 7, 10), bureau(helper, 9, 10), bureau(helper, 6, 12), bureau(helper, 10, 12), bureau(helper, 3, 3)};
        for (BlockPos b : bureaus) {
            helper.assertTrue(Kantoor.klokVan(level, b) == null, "no clock yet");
        }
        BlockPos klokPos = klok(helper);
        PrikklokBlockEntity klok = (PrikklokBlockEntity) level.getBlockEntity(klokPos);
        helper.assertTrue(klok.bureaus().size() == Kantoor.MAX_BUREAUS && klok.vol(), "four desks on one clock");
        helper.assertTrue(Kantoor.klokVan(level, bureaus[4]) == null && !Kantoor.koppel(level, bureaus[4], null), "the fifth (the farthest) stays loose");
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(Kantoor.klokVan(level, bureaus[i]) == klok, "desk " + i + " belongs to the clock");
        }
        level.destroyBlock(bureaus[0], false);
        helper.assertTrue(klok.bureaus().size() == 3 && Kantoor.koppel(level, bureaus[4], null) && Kantoor.klokVan(level, bureaus[4]) == klok,
                "a place came free: the fifth connects");
        ServerPlayer p = PxTest.speler(helper);
        helper.assertTrue(Kantoor.klokIn(p, level, klokPos, UUID.randomUUID()) != null, "a clock is not a desk");
        ruimOp(helper, p);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void papierAanDeMuur(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        // hanging a payslip on a wall by hand: the player looks south at a stone block
        BlockPos muur = helper.absolutePos(new BlockPos(8, 2, 8));
        level.setBlock(muur, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(muur.above(), Blocks.STONE.defaultBlockState(), 3);
        p.snapTo(muur.getX() + 0.5, muur.getY(), muur.getZ() - 2.5, 0f, 0f);
        ItemStack strook = Papier.loon(Component.literal("Vadsje"), Component.literal("Baas"), 7, 123456789L, "05-10-0001");
        CompoundTag origineel = Papier.tag(strook);
        p.setItemInHand(InteractionHand.MAIN_HAND, strook);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(muur).add(0, 0, -0.5), Direction.NORTH, muur, false);
        p.getItemInHand(InteractionHand.MAIN_HAND).useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit));
        BlockPos plek = muur.north();
        BlockState state = level.getBlockState(plek);
        helper.assertTrue(state.is(KantoorSlice.LOONSTROOKJE.get()) && state.getValue(HorizontalDirectionalBlock.FACING) == Direction.NORTH,
                "the payslip hangs on the wall: " + state);
        helper.assertTrue(level.getBlockEntity(plek) instanceof PapierBlockEntity be && be.papier().equals(origineel), "with its text");
        List<ItemStack> drops = Block.getDrops(state, level, plek, level.getBlockEntity(plek));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(KantoorSlice.LOONSTROOKJE_ITEM.get()) && Papier.tag(drops.get(0)).equals(origineel),
                "taking it off the wall gives the same payslip back: " + drops);
        helper.assertTrue(KantoorTeksten.blad(Papier.tag(drops.get(0))).toString().equals(KantoorTeksten.blad(origineel).toString()), "the same sheet, line for line");
        // the wall goes: the paper pops off
        level.setBlock(muur, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(plek).isAir(), "no wall, no paper");
        // the other two papers keep their data the same way
        level.setBlock(muur, Blocks.STONE.defaultBlockState(), 3);
        for (Papier.Soort soort : List.of(Papier.Soort.KWARTAAL, Papier.Soort.OORKONDE)) {
            ItemStack s = Papier.voorbeeld(soort, 42L);
            Block blok = soort == Papier.Soort.KWARTAAL ? KantoorSlice.KWARTAALRAPPORT.get() : KantoorSlice.OORKONDE.get();
            level.setBlock(plek, blok.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 3);
            level.getBlockEntity(plek).applyComponentsFromItemStack(s);
            List<ItemStack> d = Block.getDrops(level.getBlockState(plek), level, plek, level.getBlockEntity(plek));
            helper.assertTrue(d.size() == 1 && d.get(0).is(soort.item()) && Papier.tag(d.get(0)).equals(Papier.tag(s)), soort + " keeps its data: " + d);
            level.setBlock(plek, Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(muur, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(muur.above(), Blocks.AIR.defaultBlockState(), 3);
        ruimOp(helper, p);
        helper.succeed();
    }

    /** Every translate key in a component (and in its arguments). */
    private static void sleutels(Component c, Set<String> uit) {
        if (c.getContents() instanceof TranslatableContents t) {
            uit.add(t.getKey());
            for (Object arg : t.getArgs()) {
                if (arg instanceof Component a) {
                    sleutels(a, uit);
                }
            }
        }
        for (Component kind : c.getSiblings()) {
            sleutels(kind, uit);
        }
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void tekstenEnVeleVarianten(GameTestHelper helper) {
        int regels = 0;
        for (KantoorTeksten.Pool pool : KantoorTeksten.Pool.values()) {
            for (int i = 0; i < pool.aantal(); i++) {
                helper.assertTrue(Language.getInstance().has(pool.sleutel(i)), "missing text " + pool.sleutel(i));
                regels++;
            }
        }
        helper.assertTrue(regels >= 200, "dozens and dozens of lines: " + regels);
        Set<String> gebruikt = new HashSet<>();
        for (Papier.Soort soort : Papier.Soort.values()) {
            Set<String> bladen = new HashSet<>();
            for (long zaad = 0; zaad < 200; zaad++) {
                CompoundTag t = Papier.tag(Papier.voorbeeld(soort, zaad * 7919L + 13L));
                List<KantoorTeksten.Regel> blad = KantoorTeksten.blad(t);
                helper.assertTrue(blad.size() >= 10, soort + ": a whole sheet");
                helper.assertTrue(blad.toString().equals(KantoorTeksten.blad(t).toString()), soort + ": the same seed gives the same sheet");
                StringBuilder sb = new StringBuilder();
                for (KantoorTeksten.Regel r : blad) {
                    Set<String> s = new HashSet<>();
                    sleutels(r.tekst(), s);
                    for (String key : s) {
                        helper.assertTrue(Language.getInstance().has(key), soort + ": missing text " + key);
                    }
                    gebruikt.addAll(s);
                    sb.append(s).append('|');
                }
                bladen.add(sb.toString());
            }
            helper.assertTrue(bladen.size() >= (soort == Papier.Soort.OORKONDE ? 40 : 150), soort + ": many different sheets, got " + bladen.size() + " of 200");
        }
        helper.assertTrue(gebruikt.size() >= 200, "nearly every line turns up: " + gebruikt.size());
        // a blank form (no data) can be read too
        helper.assertTrue(!KantoorTeksten.blad(new CompoundTag()).isEmpty(), "a blank sheet does not break the screen");
        // every other text of the slice that the code builds by hand
        for (String key : List.of("gui.guhs.guhkantoor.scherm.grap.1", "gui.guhs.guhkantoor.scherm.grap.2", "gui.guhs.guhkantoor.scherm.grap.3",
                "gui.guhs.guhkantoor.kiezer.ver", "gui.guhs.guhkantoor.kiezer.werkt", "gui.guhs.guhkantoor.kiezer.vakantie", "gui.guhs.guhkantoor.meld.vol",
                "gui.guhs.guhkantoor.meld.geen_klok", "gui.guhs.guhkantoor.meld.gekoppeld", "gui.guhs.guhkantoor.meld.geen_bureaus",
                "gui.guhs.guhkantoor.meld.ingeklokt", "gui.guhs.guhkantoor.meld.uitgeklokt", "gui.guhs.guhkantoor.meld.loon", "gui.guhs.guhkantoor.meld.rapport",
                "gui.guhs.guhkantoor.meld.oorkonde", "gui.guhs.guhkantoor.meld.postvak", "gui.guhs.guhkantoor.meld.terug", "gui.guhs.guhkantoor.meld.weg",
                "gui.guhs.guhkantoor.meld.te_ver", "gui.guhs.guhkantoor.meld.niet_jouw", "gui.guhs.guhkantoor.meld.bezet", "gui.guhs.guhkantoor.meld.nog_niks",
                "gui.guhs.guhkantoor.meld.mislukt", "gui.guhs.guhkantoor.winkel.set.naam", "gui.guhs.guhkantoor.winkel.set", "gui.guhs.guhkantoor.winkel.bureautje",
                "gui.guhs.guhkantoor.winkel.eis", "gui.guhs.guhkantoor.gids.kop", "gui.guhs.guhkantoor.gids.uitleg", "gui.guhs.guhkantoor.gids.werkt",
                "gui.guhs.guhkantoor.gids.loon", "gui.guhs.guhkantoor.gids.rapport", "gui.guhs.guhkantoor.gids.oorkonde", "gui.guhs.guhkantoor.gids.maand",
                "gui.guhs.guhkantoor.gids.maand.waarde", "gui.guhs.guhkantoor.gids.maand.geen", "gui.guhs.guhkantoor.scherm.titel", "gui.guhs.guhkantoor.scherm.bureau",
                "gui.guhs.guhkantoor.scherm.leeg", "gui.guhs.guhkantoor.scherm.geen_bureau", "gui.guhs.guhkantoor.scherm.van", "gui.guhs.guhkantoor.scherm.slaapt",
                "gui.guhs.guhkantoor.scherm.klaar", "gui.guhs.guhkantoor.scherm.klaar.1", "gui.guhs.guhkantoor.scherm.tijd", "gui.guhs.guhkantoor.scherm.knop.werk",
                "gui.guhs.guhkantoor.scherm.knop.huis", "gui.guhs.guhkantoor.scherm.knop.loon", "gui.guhs.guhkantoor.scherm.knop.inklokken",
                "gui.guhs.guhkantoor.scherm.knop.terug", "gui.guhs.guhkantoor.scherm.kies", "gui.guhs.guhkantoor.scherm.maand", "gui.guhs.guhkantoor.scherm.maand.geen",
                "gui.guhs.guhkantoor.scherm.tip.huis", "gui.guhs.guhkantoor.scherm.tip.loon", "gui.guhs.guhkantoor.scherm.tip.ander",
                "gui.guhs.guhkantoor.papier.van", "gui.guhs.guhkantoor.papier.nr", "gui.guhs.guhkantoor.papier.kantoor", "gui.guhs.guhkantoor.papier.leeg",
                "book.guhs.guhkantoor.kwartaal.as.veel", "book.guhs.guhkantoor.kwartaal.as.niks", "book.guhs.guhkantoor.kwartaal.as.tijd",
                "subtitles.guhs.guhkantoor.inklokken", "subtitles.guhs.guhkantoor.uitklokken", "subtitles.guhs.guhkantoor.printer",
                "subtitles.guhs.guhkantoor.toetsenbord", "gui.guhs.band.plek.op_kantoor")) {
            helper.assertTrue(Language.getInstance().has(key), "missing text " + key);
        }
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void werknemerVanDeMaand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        klok(helper);
        BlockPos een = bureau(helper, 6, 10), twee = bureau(helper, 10, 10);
        GuhEntity a = guh(helper, p, "Vadsje", 4), b = guh(helper, p, "Njegje", 6);
        UUID ida = a.getUUID();
        helper.assertTrue(Kantoor.topper(level.getServer(), p.getUUID()) == null, "nobody slept yet");
        helper.assertTrue(Kantoor.klokIn(p, level, een, ida) == null, "the first clocks in");
        PxTest.spoel(3);
        helper.assertTrue(Kantoor.klokIn(p, level, twee, b.getUUID()) == null, "the second three hours later");
        PxTest.spoel(2);
        Kantoor.bijwerken(level.getServer(), p.getUUID());
        Kantoor.Topper top = Kantoor.topper(level.getServer(), p.getUUID());
        helper.assertTrue(top != null && top.guh().equals(ida) && top.uren() == 5 && top.naam().getString().equals("Vadsje"), "the longest sleeper: " + top);
        helper.assertTrue(GidsBlad.stand(p).toString().contains("gui.guhs.guhkantoor.gids.kop"), "the Guhdex section is there");
        // the month ends: an oorkonde for the longest sleeper, the counters start again
        int maand = Kantoor.maandSleutel(Klok.nu());
        PxTest.spoel(24 * 32);
        helper.assertTrue(Kantoor.maandSleutel(Klok.nu()) > maand, "(a later month)");
        Kantoor.bijwerken(level.getServer(), p.getUUID());
        helper.assertTrue(Kantoor.oorkondes(p) == 1 && Kantoor.topper(level.getServer(), p.getUUID()) == null, "the month is closed");
        helper.assertTrue(tel(p, KantoorSlice.OORKONDE_ITEM.get()) == 0 && Kantoor.bezorg(p), "the oorkonde waited in the postvak");
        CompoundTag oorkonde = Papier.tag(eerste(p, KantoorSlice.OORKONDE_ITEM.get()));
        helper.assertTrue(tel(p, KantoorSlice.OORKONDE_ITEM.get()) == 1 && Papier.soort(oorkonde) == Papier.Soort.OORKONDE
                && Tekst.get(oorkonde, "Naam").getString().equals("Vadsje") && oorkonde.getIntOr("Uren", 0) >= 24 * 32
                && oorkonde.getIntOr("Maand", -1) == Math.floorMod(maand, 12) && oorkonde.getIntOr("Jaar", 0) == Math.floorDiv(maand, 12), "the oorkonde: " + oorkonde);
        helper.assertTrue(!Kantoor.bezorg(p) && tel(p, KantoorSlice.OORKONDE_ITEM.get()) == 1, "delivered once");
        Kantoor.bijwerken(level.getServer(), p.getUUID());
        helper.assertTrue(Kantoor.oorkondes(p) == 1, "one oorkonde per month");
        ruimOp(helper, p);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void bureauVerdwenenGuhKomtTerug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        klok(helper);
        BlockPos bureau = bureau(helper, 6, 10);
        GuhEntity a = guh(helper, p, "Vadsje", 4);
        UUID id = a.getUUID();
        helper.assertTrue(Kantoor.klokIn(p, level, bureau, id) == null, "at work");
        KantoorData data = KantoorData.get(level.getServer());
        Kantoor.controleer(p);
        helper.assertTrue(data.van(id) != null && inWereld(helper, id) == 0, "a desk that is there: nothing happens");
        // the record points at a spot where no desk is (the block vanished without telling)
        data.van(id).pos = helper.absolutePos(new BlockPos(2, 2, 2));
        PxTest.spoel(8.2);
        Kantoor.controleer(p);
        helper.assertTrue(data.van(id) == null && inWereld(helper, id) == 1, "the guh steps out next to its owner");
        helper.assertTrue(level.getEntity(id).distanceTo(p) < 2 && tel(p, KantoorSlice.LOONSTROOKJE_ITEM.get()) == 1, "with its payslip");
        BureautjeBlockEntity be = (BureautjeBlockEntity) level.getBlockEntity(bureau);
        helper.assertTrue(be.bezet(), "(the old desk still shows a stale copy)");
        for (int i = 0; i < 120; i++) {
            BureautjeBlockEntity.serverTick(level, bureau, level.getBlockState(bureau), be);
        }
        helper.assertTrue(!be.bezet() && !level.getBlockState(bureau).getValue(BureautjeBlock.BEZET), "which the desk clears by itself");
        ruimOp(helper, p);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void winkelAanbod(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper);
        helper.assertTrue(Winkel.van("guhkantoor_set") != null && Winkel.van("guhkantoor_bureautje") != null, "two offers");
        helper.assertTrue(Winkel.van("guhkantoor_set").groep().equals("guhkantoor") && Winkel.prijs(p, Winkel.van("guhkantoor_set")) == KantoorSlice.PRIJS_SET
                && Winkel.prijs(p, Winkel.van("guhkantoor_bureautje")) == KantoorSlice.PRIJS_BUREAUTJE, "the prices of the design");
        Muntjes.zet(p, 300);
        helper.assertTrue(Winkel.koop(p, "guhkantoor_bureautje") == Winkel.Uitkomst.EIS && Muntjes.saldo(p) == 300, "an extra desk needs the set first");
        helper.assertTrue(Winkel.koop(p, "guhkantoor_set") == Winkel.Uitkomst.OK && Muntjes.saldo(p) == 100 && tel(p, KantoorSlice.PRIKKLOK_ITEM.get()) == 1
                && tel(p, KantoorSlice.BUREAUTJE_ITEM.get()) == 1, "the set: a Prikklok and one Bureautje");
        helper.assertTrue(Winkel.koop(p, "guhkantoor_bureautje") == Winkel.Uitkomst.OK && Winkel.koop(p, "guhkantoor_bureautje") == Winkel.Uitkomst.OK
                && Muntjes.saldo(p) == 20 && tel(p, KantoorSlice.BUREAUTJE_ITEM.get()) == 3, "extra desks for 40 each");
        helper.assertTrue(Winkel.koop(p, "guhkantoor_bureautje") == Winkel.Uitkomst.TE_DUUR && Muntjes.saldo(p) == 20, "too expensive: nothing happens");
        p.getInventory().clearContent();
        ruimOp(helper, p);
        helper.succeed();
    }
}
