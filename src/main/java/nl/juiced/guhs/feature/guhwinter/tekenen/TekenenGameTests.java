package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.Random;
import java.util.UUID;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.gametest.GuhTest;

/** Guhwinter tekenen: the drawing storage survives a save/load, packing refuses junk, the item carries its drawing. */
public class TekenenGameTests {
    private static final String EMPTY = "empty";

    private static int[] krabbel(long seed) {
        Random r = new Random(seed);
        int[] px = TekenPixels.leeg();
        for (int i = 0; i < 600; i++) {
            px[r.nextInt(TekenPixels.N)] = 0xFF000000 | r.nextInt(0xFFFFFF);
        }
        return px;
    }

    /** TekeningenData -> NBT -> TekeningenData: every drawing (pixels included), the tree, the album bookkeeping. */
    @GuhTest(template = EMPTY)
    public static void opslagRondje(GameTestHelper helper) {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        TekeningenData d = new TekeningenData();
        Tekening t1 = new Tekening(UUID.randomUUID(), a, "Annie", Doel.KERSTBOOM, 3, 1000L, 2000L, krabbel(1));
        Tekening t2 = new Tekening(UUID.randomUUID(), b, "Bert", Doel.LOOTJE, 1, 1500L, 1500L, TekenPixels.leeg());
        d.zet(t1);
        d.zet(t2);
        d.zetKerstboom(a, t1.id());
        d.markeerAlbum(b);
        d.pak(b, t1.id());

        Tag tag = TekeningenData.CODEC.encodeStart(NbtOps.INSTANCE, d).getOrThrow();
        TekeningenData terug = TekeningenData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        helper.assertTrue(terug.aantal() == 2, "two drawings back");
        helper.assertTrue(terug.get(t1.id()) != null && terug.get(t1.id()).zelfde(t1), "drawing 1 the same (pixels too)");
        helper.assertTrue(terug.get(t2.id()) != null && terug.get(t2.id()).zelfde(t2), "drawing 2 the same");
        helper.assertTrue(t1.id().equals(terug.kerstboomVan(a)) && terug.kerstboomVan(b) == null, "kerstboom per player");
        helper.assertTrue(terug.albumGekregen(b) && !terug.albumGekregen(a), "album given");
        helper.assertTrue(terug.gepakt(b, t1.id()) && !terug.gepakt(a, t1.id()) && !terug.pak(b, t1.id()), "taken once");
        helper.assertTrue(terug.kerstboomTekeningen().size() == 1, "one drawing in the tree");

        // packing: the round trip is exact, junk and wrong sizes are refused
        int[] px = krabbel(7);
        px[5] = 0x40FF0000;   // half transparent -> paper
        int[] uit = TekenPixels.pakUit(TekenPixels.pak(px));
        helper.assertTrue(uit != null && uit[5] == 0 && uit[6] == TekenPixels.schoon(px[6]), "pack/unpack");
        helper.assertTrue(TekenPixels.pakUit(new byte[]{1, 2, 3}) == null, "junk refused");
        helper.assertTrue(TekenPixels.pakUit(new byte[TekenPixels.MAX_PAK + 1]) == null, "too big refused");
        java.util.zip.Deflater def = new java.util.zip.Deflater();
        def.setInput(new byte[TekenPixels.BYTES * 2]);
        def.finish();
        byte[] dubbel = new byte[TekenPixels.MAX_PAK];
        int n = def.deflate(dubbel);
        def.end();
        helper.assertTrue(TekenPixels.pakUit(java.util.Arrays.copyOf(dubbel, n)) == null, "a too big picture refused");

        // the item
        ItemStack s = Tekeningen.maakItem(t1.id(), "Annie");
        TekeningRef ref = s.get(TekenenFeature.TEKENING_REF.get());
        helper.assertTrue(ref != null && ref.id().equals(t1.id()) && ref.maker().equals("Annie"), "item carries the drawing");
        TekeningRef refTerug = TekeningRef.CODEC.parse(NbtOps.INSTANCE, TekeningRef.CODEC.encodeStart(NbtOps.INSTANCE, ref).getOrThrow()).getOrThrow();
        helper.assertTrue(refTerug.equals(ref), "item component round trip");
        helper.succeed();
    }
}
