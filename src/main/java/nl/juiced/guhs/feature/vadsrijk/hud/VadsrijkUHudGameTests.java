package nl.juiced.guhs.feature.vadsrijk.hud;

import java.util.List;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-HUD: the sync of party frames and emotes. What a member sees of the others (name, level, Vads, buffs, leader, offline), the
 * payloads surviving the wire, emotes starting/stopping (loop stops when you move, the hug reaches a nearby player, anti-spam).
 * Run: {@code runGameTestServer -Pgt=VadsrijkUHud}.
 */
public class VadsrijkUHudGameTests {
    private static final String BATCH = "vadsrijk_u_hud";

    private static <T> T overDeDraad(GameTestHelper helper, net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, T> codec, T waarde) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        codec.encode(buf, waarde);
        T terug = codec.decode(buf);
        helper.assertTrue(buf.readableBytes() == 0, "the whole message is read");
        return terug;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void partyFramesSync(GameTestHelper helper) {
        ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper);
        try {
            helper.assertTrue(PartyFrames.frames(a).isEmpty(), "no party: nothing");
            Partijen.nodigUit(a, b);
            helper.assertTrue("vadsrijk.kern.party.erbij".equals(Partijen.accepteer(b)), "b joins a's party");
            Statussen.geef(b, Status.VAHOEG, 200, 0.2, null);
            List<UHudPayloads.Frame> l = PartyFrames.frames(a);
            helper.assertTrue(l.size() == 1, "a sees one other member, not itself: " + l.size());
            UHudPayloads.Frame f = l.get(0);
            helper.assertTrue(f.id().equals(b.getUUID()) && f.naam().equals(b.getGameProfile().name()), "the frame is b");
            helper.assertTrue(!f.leider() && f.staat() == UHudPayloads.STAAT_OK, "b is no leader and fine");
            helper.assertTrue(f.max() > 0 && f.vads() > 0 && f.level() >= 1 && !f.klasse().isEmpty() && !f.spec().isEmpty(), "vads/level/class/spec filled");
            helper.assertTrue(f.buffs().contains(Status.VAHOEG.ordinal()), "b's buff is in the frame");
            helper.assertTrue(f.dim().equals(b.level().dimension().identifier().toString()), "b's world is sent");
            helper.assertTrue(PartyFrames.frames(b).get(0).leider(), "b sees a as the leader");
            // an offline member (still in the party, not online): name kept from earlier, grey state
            UUID weg = UUID.randomUUID();
            Partijen.party(a).leden.add(weg);
            List<UHudPayloads.Frame> l2 = PartyFrames.frames(a);
            helper.assertTrue(l2.size() == 2 && l2.get(1).staat() == UHudPayloads.STAAT_OFFLINE, "offline member shows as offline");
            Partijen.party(a).leden.remove(weg);
            // over the wire
            UHudPayloads.Party p = overDeDraad(helper, UHudPayloads.Party.STREAM_CODEC, new UHudPayloads.Party(l2, false));
            helper.assertTrue(p.leden().equals(l2), "the party message survives the wire");
            UHudPayloads.Party nep = overDeDraad(helper, UHudPayloads.Party.STREAM_CODEC, new UHudPayloads.Party(PartyFrames.nepLeden(a, 4), true));
            helper.assertTrue(nep.nep() && nep.leden().size() == 4, "the fake party survives the wire");
            // click-to-target: only party members
            PartyFrames.kies(a, b.getUUID());
            helper.assertTrue(PartyFrames.gekozen(a) == b, "a chose b");
            Partijen.verlaat(b.getUUID());
            helper.assertTrue(PartyFrames.frames(a).isEmpty() && PartyFrames.gekozen(a) == null, "b left: no frames, no target");
        } finally {
            Partijen.verlaat(a.getUUID());
            Partijen.verlaat(b.getUUID());
            PartyFrames.vergeet(a.getUUID());
            PartyFrames.vergeet(b.getUUID());
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void emoteSync(GameTestHelper helper) {
        ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper), c = GuhMockPlayer.of(helper);
        try {
            // far apart: c is 20 blocks away, b right next to a
            b.snapTo(a.getX() + 1.5, a.getY(), a.getZ());
            c.snapTo(a.getX() + 20, a.getY(), a.getZ());
            helper.assertTrue(Emotes.speel(c, Emote.DANS, false) == null && Emotes.actief(c).emote() == Emote.DANS, "c dances");
            helper.assertTrue("vadsrijk.uhud.emote.even_wachten".equals(Emotes.speel(c, Emote.ZWAAI, false)), "anti-spam");
            helper.assertTrue(Emotes.actief(c).emote() == Emote.DANS, "still dancing after the refused wave");
            Emotes.tick(helper.getLevel().getServer());
            helper.assertTrue(Emotes.actief(c) != null, "standing still: the loop goes on");
            c.snapTo(c.getX() + 1, c.getY(), c.getZ());
            Emotes.tick(helper.getLevel().getServer());
            helper.assertTrue(Emotes.actief(c) == null, "moving stops the loop");
            // the hug: b is close, so b hugs too; c (far) does not
            helper.assertTrue(Emotes.speel(a, Emote.KNUFFEL, false) == null, "a hugs someone");
            helper.assertTrue(Emotes.actief(a).emote() == Emote.KNUFFEL && Emotes.actief(b) != null && Emotes.actief(b).emote() == Emote.KNUFFEL,
                    "a and b hug");
            helper.assertTrue(Emotes.actief(c) == null, "c is too far to be hugged");
            helper.assertTrue(Emotes.knuffelMaatje(c) == null, "nobody near c");
            // over the wire
            UHudPayloads.EmoteSpeel s = overDeDraad(helper, UHudPayloads.EmoteSpeel.STREAM_CODEC, new UHudPayloads.EmoteSpeel(a.getId(), "knuffel", 7));
            helper.assertTrue(s.entity() == a.getId() && Emote.van(s.emote()) == Emote.KNUFFEL && s.verstreken() == 7, "the emote message survives the wire");
            helper.assertTrue(Emote.van("bestaat_niet") == null, "unknown emote");
            for (Emote e : Emote.values()) {
                helper.assertTrue(e.loop || e.ticks > 0, "timed emote has a length: " + e.id);
            }
        } finally {
            for (ServerPlayer p : new ServerPlayer[] {a, b, c}) {
                Emotes.vergeet(p.getUUID());
            }
        }
        helper.succeed();
    }
}
