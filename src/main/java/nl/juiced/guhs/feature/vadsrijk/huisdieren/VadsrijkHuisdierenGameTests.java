package nl.juiced.guhs.feature.vadsrijk.huisdieren;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.storage.Nbt;

/**
 * G-PETS: a boss's first kill unlocks its pet exactly once (a real Grauwe Klodderreus defeat through kern's {@code verslagen}, a
 * second kill gives nothing, the unlock moment is queued once), the catch-up from the kern first-kill key, and choosing + saving
 * (the pick survives a SpelerData save/load; "none" stays none; the pet entity is never pickable/attackable/saved/hurt).
 * Run: {@code runGameTestServer -Pgt=VadsrijkHuisdieren}.
 */
public class VadsrijkHuisdierenGameTests {
    private static final String BATCH = "vadsrijk_huisdieren";

    private static Klodderreus reus(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Klodderreus k = KernRegistry.KLODDERREUS.get().create(sl, EntitySpawnReason.COMMAND);
        BlockPos p = helper.absolutePos(new BlockPos(2, 1, 2));
        k.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        k.stelIn(3, Element.ROZE, false, Set.of());
        sl.addFreshEntity(k);
        return k;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void eersteKillEenKeer(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Huisdieren.reset(p);
        AtomicInteger nieuw = new AtomicInteger();
        BiConsumer<ServerPlayer, Huisdier> luister = (sp, h) -> {
            if (sp == p) {
                nieuw.incrementAndGet();
            }
        };
        Huisdieren.NIEUW.add(luister);
        try {
            helper.assertTrue(Huisdieren.aantal(p) == 0 && Huisdieren.actief(p) == null, "no pets yet");
            Klodderreus k1 = reus(helper);
            k1.verslagen(p);
            helper.assertTrue(Huisdieren.heeft(p, Huisdieren.KLODDERREUS.id()), "the first Klodderreus kill unlocks the mini Klodderreus");
            helper.assertTrue(Huisdieren.aantal(p) == 1 && nieuw.get() == 1, "exactly one new pet");
            helper.assertTrue(Huisdieren.actief(p) == Huisdieren.KLODDERREUS, "the very first pet walks along by itself");
            helper.assertTrue(Huisdieren.wachtendeMomenten(p) == 1, "one 'Nieuw huisdier!' title card queued");
            Klodderreus k2 = reus(helper);
            k2.verslagen(p);
            helper.assertTrue(Huisdieren.aantal(p) == 1 && nieuw.get() == 1 && Huisdieren.wachtendeMomenten(p) == 1, "a second kill gives nothing");
            k1.discard();
            k2.discard();
            // another boss type, by the hook directly
            helper.assertTrue(Huisdieren.naKill(List.of(p), "guhs:vr_baas_koning_klodder") == 1 && Huisdieren.naKill(List.of(p), "guhs:vr_baas_koning_klodder") == 0,
                    "Koning Klodder once");
            helper.assertTrue(Huisdieren.naKill(List.of(p), "guhs:vr_iets_anders") == 0, "a normal mob gives no pet");
            helper.assertTrue(Huisdieren.actief(p) == Huisdieren.KLODDERREUS, "a later pet does not steal the active slot");
            // catch-up: beat the Galmwolf before pets existed
            Spelers.data(p).gezien.add(Huisdieren.GALMWOLF.killSleutel());
            Huisdieren.inhalen(p);
            helper.assertTrue(Huisdieren.heeft(p, "galmwolf") && nieuw.get() == 3, "catch-up from the kern first-kill key");
            Huisdieren.inhalen(p);
            helper.assertTrue(nieuw.get() == 3, "catch-up only once");
        } finally {
            Huisdieren.NIEUW.remove(luister);
            Huisdieren.reset(p);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void kiezenEnBewaren(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Huisdieren.reset(p);
        helper.assertTrue(!Huisdieren.kies(p, "galmwolf"), "can't pick a pet you don't have");
        helper.assertTrue(Huisdieren.geef(p, "galmwolf") && !Huisdieren.geef(p, "galmwolf") && !Huisdieren.geef(p, "bestaatniet"), "geef once, unknown refused");
        helper.assertTrue(Huisdieren.geef(p, "koning_klodder"), "second pet");
        helper.assertTrue(Huisdieren.actief(p) == Huisdieren.GALMWOLF, "first one active");
        helper.assertTrue(Huisdieren.kies(p, "koning_klodder") && Huisdieren.actief(p) == Huisdieren.KONING_KLODDER, "pick another");
        // a save + load (the data goes through player.dat)
        SpelerData d = Spelers.data(p);
        CompoundTag tag = Nbt.write(p.registryAccess(), d::serialize);
        SpelerData terug = new SpelerData();
        terug.deserialize(Nbt.input(p.registryAccess(), tag));
        CompoundTag t = terug.extra("huisdieren");
        helper.assertTrue(t.getStringOr("actief", "").equals("koning_klodder") && t.getStringOr("bezit", "").contains("galmwolf")
                && t.getStringOr("bezit", "").contains("koning_klodder"), "pets + choice survive a save");
        // none on purpose stays none
        helper.assertTrue(Huisdieren.kies(p, null) && Huisdieren.actief(p) == null, "none");
        helper.assertTrue(Huisdieren.geef(p, "klodderreus") && Huisdieren.actief(p) == null, "a new pet doesn't override 'none'");
        // outside the Vadsrijk no pet walks along
        Huisdieren.kies(p, "klodderreus");
        Huisdieren.bijwerken(p);
        helper.assertTrue(Huisdieren.uit(p) == null, "no pet outside the Vadsrijk");
        // the pet entity is never in the way
        HuisdierEntity e = HuisdierenRegistry.HUISDIER.get().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(e != null && !e.isPickable() && !e.isAttackable() && !e.shouldBeSaved() && !e.isPushable()
                && !e.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 5), "not pickable/attackable/saved/hurt");
        Huisdieren.reset(p);
        helper.assertTrue(Huisdieren.aantal(p) == 0 && Huisdieren.actief(p) == null, "reset");
        helper.succeed();
    }
}
