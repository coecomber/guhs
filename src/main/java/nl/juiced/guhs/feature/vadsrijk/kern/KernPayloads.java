package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Vaardigheid;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;

/** The kern network: HUD values, skill keys, scenes, title cards, hints, the boss bar and the party HUD. */
public final class KernPayloads {
    /** Client hooks (set by KernClient; common code never touches client classes). */
    public static Consumer<Hud> hudOntvanger = h -> {
    };
    /** g-klassen: the last HUD values this client got (item tooltips read the class/generic levels from it); null on a server. */
    @javax.annotation.Nullable
    public static volatile Hud laatsteHud;
    public static Consumer<Scene> sceneOntvanger = h -> {
    };
    public static Consumer<Titel> titelOntvanger = h -> {
    };
    public static Consumer<Tip> tipOntvanger = h -> {
    };
    public static Consumer<BaasBalk> baasOntvanger = h -> {
    };
    public static Consumer<PartyHud> partyOntvanger = h -> {
    };
    public static Consumer<SieradenSync> sieradenOntvanger = h -> {
    };

    /** Everything the HUD shows about yourself. cd/cdMax: remaining/total cooldown ticks of R, V, G, Z and the Knuffelsprong (C). */
    public record Hud(boolean binnen, float vads, float maxVads, float energie, float maxEnergie, float ult, int level, long xpIn, long xpNodig,
            String klasse, int[] cd, int[] cdMax, long munten, int duizelig, int opgevrolijkt, String gebied, List<String> skills,
            int bevroren, int generiek, long gXpIn, long gXpNodig, boolean grijs, java.util.Map<String, Integer> klasseLv,
            List<String> knopen)
            implements CustomPacketPayload {
        public static final Type<Hud> TYPE = new Type<>(Guhs.id("vr_kern_hud"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Hud> STREAM_CODEC = StreamCodec.ofMember(Hud::schrijf, Hud::lees);

        private void schrijf(RegistryFriendlyByteBuf b) {
            b.writeBoolean(binnen);
            b.writeFloat(vads);
            b.writeFloat(maxVads);
            b.writeFloat(energie);
            b.writeFloat(maxEnergie);
            b.writeFloat(ult);
            b.writeVarInt(level);
            b.writeVarLong(xpIn);
            b.writeVarLong(xpNodig);
            b.writeUtf(klasse);
            b.writeVarIntArray(cd);
            b.writeVarIntArray(cdMax);
            b.writeVarLong(munten);
            b.writeVarInt(duizelig);
            b.writeVarInt(opgevrolijkt);
            b.writeUtf(gebied);
            b.writeCollection(skills, (bb, s) -> bb.writeUtf(s));
            b.writeVarInt(bevroren);
            b.writeVarInt(generiek);
            b.writeVarLong(gXpIn);
            b.writeVarLong(gXpNodig);
            b.writeBoolean(grijs);
            b.writeMap(klasseLv, (bb, k) -> bb.writeUtf(k), (bb, v) -> bb.writeVarInt(v));
            b.writeCollection(knopen, (bb, s) -> bb.writeUtf(s));
        }

        private static Hud lees(RegistryFriendlyByteBuf b) {
            return new Hud(b.readBoolean(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarLong(),
                    b.readVarLong(), b.readUtf(), b.readVarIntArray(), b.readVarIntArray(), b.readVarLong(), b.readVarInt(), b.readVarInt(), b.readUtf(),
                    b.readList(bb -> bb.readUtf()), b.readVarInt(), b.readVarInt(), b.readVarLong(), b.readVarLong(), b.readBoolean(),
                    b.readMap(bb -> bb.readUtf(), bb -> bb.readVarInt()), b.readList(bb -> bb.readUtf()));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: skill key 0..3 (R, V, G, Z) pressed; 4 = the Knuffelsprong (C). */
    public record Skill(int slot) implements CustomPacketPayload {
        public static final Type<Skill> TYPE = new Type<>(Guhs.id("vr_kern_skill"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Skill> STREAM_CODEC = StreamCodec.ofMember((s, b) -> b.writeVarInt(s.slot),
                b -> new Skill(b.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Scene(RijkScene scene) implements CustomPacketPayload {
        public static final Type<Scene> TYPE = new Type<>(Guhs.id("vr_kern_scene"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Scene> STREAM_CODEC = StreamCodec.ofMember((s, b) -> s.scene.schrijf(b),
                b -> new Scene(RijkScene.lees(b)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Titel(Component titel, Component onder, int ticks, int kleur) implements CustomPacketPayload {
        public static final Type<Titel> TYPE = new Type<>(Guhs.id("vr_kern_titel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Titel> STREAM_CODEC = StreamCodec.ofMember((t, b) -> {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(b, t.titel);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(b, t.onder);
            b.writeVarInt(t.ticks);
            b.writeInt(t.kleur);
        }, b -> new Titel(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(b), ComponentSerialization.TRUSTED_STREAM_CODEC.decode(b), b.readVarInt(),
                b.readInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Tip(String key) implements CustomPacketPayload {
        public static final Type<Tip> TYPE = new Type<>(Guhs.id("vr_kern_tip"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Tip> STREAM_CODEC = StreamCodec.ofMember((t, b) -> b.writeUtf(t.key),
                b -> new Tip(b.readUtf()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The big boss bar: entityId -1 = hide. muziek = the boss music's sound id ("" = none; only played if the client has the sound). */
    public record BaasBalk(int entityId, Component naam, int fase, float vads, float max, String muziek) implements CustomPacketPayload {
        public BaasBalk(int entityId, Component naam, int fase, float vads, float max) {
            this(entityId, naam, fase, vads, max, "");
        }

        public static final Type<BaasBalk> TYPE = new Type<>(Guhs.id("vr_kern_baas"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BaasBalk> STREAM_CODEC = StreamCodec.ofMember((t, b) -> {
            b.writeVarInt(t.entityId + 1);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(b, t.naam);
            b.writeVarInt(t.fase);
            b.writeFloat(t.vads);
            b.writeFloat(t.max);
            b.writeUtf(t.muziek);
        }, b -> new BaasBalk(b.readVarInt() - 1, ComponentSerialization.TRUSTED_STREAM_CODEC.decode(b), b.readVarInt(), b.readFloat(), b.readFloat(),
                b.readUtf()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The worn jewellery (RING, KETTING, OORBEL), for the slots next to the inventory screen. */
    public record SieradenSync(List<net.minecraft.world.item.ItemStack> items) implements CustomPacketPayload {
        public static final Type<SieradenSync> TYPE = new Type<>(Guhs.id("vr_kern_sieraden"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SieradenSync> STREAM_CODEC = StreamCodec.ofMember(
                (t, b) -> b.writeCollection(t.items, (bb, s) -> net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) bb, s)),
                b -> new SieradenSync(b.readList(bb -> net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) bb))));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** (fix round 2) Shift-click on jewellery in the inventory: {@code index} = the player-inventory slot (0..35). */
    public record SieraadShift(int index) implements CustomPacketPayload {
        public static final Type<SieraadShift> TYPE = new Type<>(Guhs.id("vr_kern_sieraad_shift"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SieraadShift> STREAM_CODEC = StreamCodec.ofMember((s, b) -> b.writeVarInt(s.index),
                b -> new SieraadShift(b.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: take off the jewellery in slot i of {@code Slot.SIERADEN}. */
    public record SieraadAf(int i) implements CustomPacketPayload {
        public static final Type<SieraadAf> TYPE = new Type<>(Guhs.id("vr_kern_sieraad_af"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SieraadAf> STREAM_CODEC = StreamCodec.ofMember((s, b) -> b.writeVarInt(s.i),
                b -> new SieraadAf(b.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Lid(String naam, float vads, float max, int level, boolean leider) {
    }

    public record PartyHud(List<Lid> leden) implements CustomPacketPayload {
        public static final Type<PartyHud> TYPE = new Type<>(Guhs.id("vr_kern_party"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PartyHud> STREAM_CODEC = StreamCodec.ofMember((t, b) -> b.writeCollection(t.leden, (bb, l) -> {
            bb.writeUtf(l.naam());
            bb.writeFloat(l.vads());
            bb.writeFloat(l.max());
            bb.writeVarInt(l.level());
            bb.writeBoolean(l.leider());
        }), b -> new PartyHud(b.readList(bb -> new Lid(bb.readUtf(), bb.readFloat(), bb.readFloat(), bb.readVarInt(), bb.readBoolean()))));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private KernPayloads() {
    }

    public static void register(PayloadRegistrar r) {
        r.playToClient(Hud.TYPE, Hud.STREAM_CODEC, (p, c) -> {
            laatsteHud = p;
            hudOntvanger.accept(p);
        });
        r.playToClient(Scene.TYPE, Scene.STREAM_CODEC, (p, c) -> sceneOntvanger.accept(p));
        r.playToClient(Titel.TYPE, Titel.STREAM_CODEC, (p, c) -> titelOntvanger.accept(p));
        r.playToClient(Tip.TYPE, Tip.STREAM_CODEC, (p, c) -> tipOntvanger.accept(p));
        r.playToClient(BaasBalk.TYPE, BaasBalk.STREAM_CODEC, (p, c) -> baasOntvanger.accept(p));
        r.playToClient(PartyHud.TYPE, PartyHud.STREAM_CODEC, (p, c) -> partyOntvanger.accept(p));
        r.playToServer(Skill.TYPE, Skill.STREAM_CODEC, KernPayloads::opSkill);
        r.playToClient(SieradenSync.TYPE, SieradenSync.STREAM_CODEC, (p, c) -> sieradenOntvanger.accept(p));
        r.playToServer(SieraadShift.TYPE, SieraadShift.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                Sieraden.draagUitInventaris(sp, p.index());
            }
        });
        r.playToServer(SieraadAf.TYPE, SieraadAf.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp && p.i() >= 0 && p.i() < nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.SIERADEN.length) {
                Sieraden.af(sp, nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.SIERADEN[p.i()]);
            }
        });
    }

    private static void opSkill(Skill s, IPayloadContext c) {
        if (c.player() instanceof ServerPlayer p) {
            if (s.slot() == Knuffelsprong.SLOT) {
                Knuffelsprong.gebruik(p);
                return;
            }
            Vaardigheden.gebruik(p, s.slot());
        }
    }

    /** U-QUESTLOPEN: may swallow a payload (the muted "Spring naar") and records scenes for "Herhaal scène". */
    public static java.util.function.BiPredicate<ServerPlayer, CustomPacketPayload> VOOR_STUUR = (p, x) -> false;

    /** Sends a payload; false when it was swallowed (U-QUESTLOPEN's muted fast-forward). (W-SPEL: returns whether it went out) */
    public static boolean stuur(ServerPlayer p, CustomPacketPayload payload) {
        if (VOOR_STUUR.test(p, payload)) {   // U-QUESTLOPEN
            return false;
        }
        PacketDistributor.sendToPlayer(p, payload);
        return true;
    }

    public static void stuurHud(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        var k = Spelers.klasse(p);
        int lvl = d.level(k.id());
        long xp = d.xp(k.id());
        int gl = d.level();
        java.util.Map<String, Integer> klasseLv = new java.util.LinkedHashMap<>();
        for (var x : nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.alle()) {
            klasseLv.put(x.id(), d.level(x.id()));
        }
        int[] cd = new int[5], cdMax = new int[5];   // (index 4 = the Knuffelsprong, every class)
        List<String> skills = new ArrayList<>();
        long nu = p.level().getGameTime();
        for (int i = 0; i < 4; i++) {
            Vaardigheid v = i < k.vaardigheden().size() ? k.vaardigheden().get(i) : null;
            skills.add(v == null ? "" : v.naamKey());
            if (v != null) {
                cd[i] = (int) Math.max(0, d.klaarOp.getOrDefault(v.id(), 0L) - nu);
                cdMax[i] = Vaardigheden.cooldown(p, v);
            }
        }
        cd[Knuffelsprong.SLOT] = (int) Math.max(0, d.klaarOp.getOrDefault(Knuffelsprong.ID, 0L) - nu);
        cdMax[Knuffelsprong.SLOT] = Knuffelsprong.COOLDOWN;
        skills.add("vadsrijk.kern.skill.knuffelsprong");
        int duiz = (int) Math.max(0, d.duizeligTot - nu);
        int cap = nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.plafond(p);   // X-ZONE1: at the soft cap the bars show full (like the max)
        boolean kVol = lvl >= Levels.MAX || (cap > 0 && lvl >= cap), gVol = gl >= Levels.MAX_GENERIEK || (cap > 0 && gl >= cap);
        stuur(p, new Hud(Vadsrijk.in(p), (float) Spelers.vads(p), (float) Spelers.maxVads(p), (float) Spelers.energie(p), (float) Spelers.maxEnergie(p),
                (float) d.ultLading, lvl, kVol ? 0 : Levels.restXp(xp), kVol ? 0 : Levels.xpVoor(lvl), Spelers.titelKey(p), cd, cdMax, d.munten,
                duiz, d.opgevrolijkt, d.gebied == null ? "" : d.gebied, skills, Statussen.ticks(p, Status.BEVROREN), gl,
                gVol ? 0 : Levels.restXpGeneriek(d.generiekXp), gVol ? 0 : Levels.xpVoorGeneriek(gl),
                Spelers.actieveWapenKlasse(p) == null, klasseLv,
                // g-int: the bought tree nodes of the active class (incl. hidden _adv steps), for the client skill tooltips (SkillInfo)
                List.copyOf(d.boom.getOrDefault(k.id(), java.util.Set.of()))));
    }

    public static void stuurParty(ServerPlayer p) {
        List<Lid> l = new ArrayList<>();
        Partijen.Party party = Partijen.party(p);
        for (ServerPlayer m : Partijen.leden(p)) {
            l.add(new Lid(m.getGameProfile().name(), (float) Spelers.vads(m), (float) Spelers.maxVads(m), Spelers.level(m),
                    party != null && party.leider.equals(m.getUUID())));
        }
        stuur(p, new PartyHud(l));
    }
}
