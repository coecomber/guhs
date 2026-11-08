package nl.juiced.guhs.feature.vadsrijk.ritme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.vadsrijk.hud.Emote;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.registry.ModSounds;

/**
 * U-LEVEN-RITME: reactions to your emotes (U-HUD's wave/hug/dance/... via {@code Emotes.NA_EMOTE}) and the per-town
 * <b>knuffelmeter</b>.
 * <ul>
 *   <li>Guhs within {@link #STRAAL} blocks react (at most {@link #MAX_REACTIES}, each with a small delay): wave back, hug back (the
 *   nearest one, with hearts), dance along, cheer along, roll over laughing, blush at a heart, yawn when you lie down to sleep,
 *   look where you point. Sleeping guhs sleep on; tame pets react too but don't count.</li>
 *   <li>Every town guh you make happy (a friendly emote, once per guh per day) counts for that town's knuffelmeter. Rewards per town:
 *   {@link #DREMPELS}; overall: {@link #TOTAAL_BRIL} = the heart glasses (sent home by post), {@link #TOTAAL_TITEL} = the title
 *   "Grote Knuffelaar"; the first town at {@link #TITEL_STAD} gives the title "Knuffelvriend".</li>
 * </ul>
 * Data: {@code SpelerData.extra("uritme")}: {@code knuffel} {town: n}, {@code kdag}, {@code kvandaag} [keys], {@code beloond} ["town:n"].
 * Shown in the Karakter tab of the Vadsrijk menu (MenuServer.KARAKTER_EXTRA) and on every town's notice board.
 */
public final class Knuffelmeter {
    public static final String DATA = "uritme";
    static final double STRAAL = 7.0, KNUFFEL_STRAAL = 3.5;
    static final int MAX_REACTIES = 4;
    /** Per town: count -> reward in brons (and at {@link #TITEL_STAD} the first-time title). */
    public static final int[] DREMPELS = {5, 15, 25};
    public static final int[] DREMPEL_MUNTEN = {25, 100, 200};
    public static final int TITEL_STAD = 25, TOTAAL_BRIL = 50, TOTAAL_TITEL = 120, TOTAAL_TITEL_MUNTEN = 500;
    public static final String TITEL_VRIEND = "uritme_knuffelvriend", TITEL_GROOT = "uritme_grote_knuffelaar";

    /** A reaction waiting for its moment. */
    private record Reactie(long wanneer, UUID guh, nl.juiced.guhs.feature.emotes.Emote emote, @Nullable Vec3 kijk, boolean hartjes) {
    }

    private static final List<Reactie> WACHT = new ArrayList<>();

    private Knuffelmeter() {
    }

    static void titels() {
        Titels.registreer(new Titels.Titel(TITEL_VRIEND, "gui.guhs.titels.naam." + TITEL_VRIEND, ChatFormatting.LIGHT_PURPLE, "minecraft:pink_petals",
                p -> beste(p) >= TITEL_STAD));
        Titels.registreer(new Titels.Titel(TITEL_GROOT, "gui.guhs.titels.naam." + TITEL_GROOT, ChatFormatting.GOLD, "minecraft:cake",
                p -> totaal(p) >= TOTAAL_TITEL));
    }

    // ---- data ----------------------------------------------------------------------------------------------------------------

    static CompoundTag data(ServerPlayer p) {
        return Spelers.data(p).extra(DATA);
    }

    public static int stand(ServerPlayer p, String stad) {
        return data(p).getCompoundOrEmpty("knuffel").getIntOr(stad, 0);
    }

    public static int totaal(ServerPlayer p) {
        CompoundTag k = data(p).getCompoundOrEmpty("knuffel");
        int n = 0;
        for (String s : k.keySet()) {
            n += k.getIntOr(s, 0);
        }
        return n;
    }

    static int beste(ServerPlayer p) {
        CompoundTag k = data(p).getCompoundOrEmpty("knuffel");
        int n = 0;
        for (String s : k.keySet()) {
            n = Math.max(n, k.getIntOr(s, 0));
        }
        return n;
    }

    /** Per town (fixed order), only towns with a count. */
    public static Map<String, Integer> standen(ServerPlayer p) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (Steden.Stad s : Steden.ALLE) {
            int n = stand(p, s.id());
            if (n > 0) {
                m.put(s.id(), n);
            }
        }
        return m;
    }

    private static boolean heeft(ListTag l, String s) {
        for (int i = 0; i < l.size(); i++) {
            if (l.getString(i).orElse("").equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts one happy guh for this town (key = the guh: "npc:id" or its UUID), once per guh per day. Returns true when it counted
     * (and hands out a reward when a threshold is reached).
     */
    public static boolean tel(ServerPlayer p, String stad, String sleutel, long dag) {
        CompoundTag d = data(p);
        if (d.getLongOr("kdag", -1) != dag) {
            d.putLong("kdag", dag);
            d.put("kvandaag", new ListTag());
        }
        ListTag vandaag = d.getListOrEmpty("kvandaag").copy();
        String k = stad + "|" + sleutel;
        if (heeft(vandaag, k)) {
            return false;
        }
        vandaag.add(StringTag.valueOf(k));
        d.put("kvandaag", vandaag);
        CompoundTag knuffel = d.getCompoundOrEmpty("knuffel").copy();
        int n = knuffel.getIntOr(stad, 0) + 1;
        knuffel.putInt(stad, n);
        d.put("knuffel", knuffel);
        beloon(p, stad, n);
        Spelers.opgeslagen(p);
        Pluizenblaadje.knuffelNieuws(p, stad, dag);
        return true;
    }

    /** Dev: set a town's count (rewards are not paid out for skipped thresholds: use {@link #tel}). */
    static void zet(ServerPlayer p, String stad, int n) {
        CompoundTag d = data(p);
        CompoundTag knuffel = d.getCompoundOrEmpty("knuffel").copy();
        knuffel.putInt(stad, n);
        d.put("knuffel", knuffel);
        Spelers.opgeslagen(p);
    }

    static void reset(ServerPlayer p) {
        CompoundTag d = data(p);
        d.remove("knuffel");
        d.remove("kvandaag");
        d.remove("kdag");
        d.remove("beloond");
        Spelers.opgeslagen(p);
    }

    private static boolean eersteKeer(ServerPlayer p, String wat) {
        CompoundTag d = data(p);
        ListTag l = d.getListOrEmpty("beloond").copy();
        if (heeft(l, wat)) {
            return false;
        }
        l.add(StringTag.valueOf(wat));
        d.put("beloond", l);
        return true;
    }

    private static void beloon(ServerPlayer p, String stad, int n) {
        Steden.Stad s = Steden.van(stad);
        Component naam = s == null ? Component.literal(stad) : Component.translatable(s.naamKey());
        for (int i = 0; i < DREMPELS.length; i++) {
            if (n >= DREMPELS[i] && eersteKeer(p, stad + ":" + DREMPELS[i])) {
                Spelers.geefMunten(p, DREMPEL_MUNTEN[i]);
                melding(p, Component.translatable(Ritme.P + "knuffel.drempel", naam, DREMPELS[i], DREMPEL_MUNTEN[i]));
                if (DREMPELS[i] == TITEL_STAD && eersteKeer(p, "titel:vriend")) {
                    melding(p, Component.translatable(Ritme.P + "knuffel.titel", Component.translatable("gui.guhs.titels.naam." + TITEL_VRIEND)));
                }
            }
        }
        int t = totaal(p);
        if (t >= TOTAAL_BRIL && eersteKeer(p, "totaal:" + TOTAAL_BRIL)) {
            nl.juiced.guhs.feature.vadsrijk.economie.Post.stuur(p, nl.juiced.guhs.feature.vadsrijk.economie.Cosmetica.kledingItem(GuhClothes.HEART_GLASSES));
            melding(p, Component.translatable(Ritme.P + "knuffel.bril", TOTAAL_BRIL));
        }
        if (t >= TOTAAL_TITEL && eersteKeer(p, "totaal:" + TOTAAL_TITEL)) {
            Spelers.geefMunten(p, TOTAAL_TITEL_MUNTEN);
            melding(p, Component.translatable(Ritme.P + "knuffel.titel", Component.translatable("gui.guhs.titels.naam." + TITEL_GROOT)));
        }
    }

    private static void melding(ServerPlayer p, Component c) {
        p.sendSystemMessage(c.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    /** The Karakter-tab line (MenuServer.KARAKTER_EXTRA). */
    static void karakter(ServerPlayer p, CompoundTag t) {
        ListTag extra = t.getListOrEmpty("extra").copy();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : standen(p).entrySet()) {
            Steden.Stad s = Steden.van(e.getKey());
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(s == null ? e.getKey() : net.minecraft.locale.Language.getInstance().getOrDefault(s.naamKey())).append(' ').append(e.getValue());
        }
        CompoundTag regel = new CompoundTag();
        regel.putString("k", Ritme.P + "knuffel.karakter");
        regel.putString("a", String.valueOf(totaal(p)));
        extra.add(regel);
        if (sb.length() > 0) {
            CompoundTag r2 = new CompoundTag();
            r2.putString("k", Ritme.P + "knuffel.karakter.steden");
            r2.putString("a", sb.toString());
            extra.add(r2);
        }
        t.put("extra", extra);
    }

    // ---- reactions -----------------------------------------------------------------------------------------------------------

    /** Emotes.NA_EMOTE: the guhs around react. Returns true when a guh hugged back (U-HUD then skips "you hug the air"). */
    public static boolean opEmote(ServerPlayer p, Emote e) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return false;
        }
        List<GuhEntity> guhs = sl.getEntitiesOfClass(GuhEntity.class, new AABB(p.blockPosition()).inflate(STRAAL, 3, STRAAL),
                g -> g.isAlive() && !g.isVehicle() && !g.isPassenger() && !(g instanceof RijkGuh r && Rooster.slaapt(r)));
        guhs.sort((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p)));
        if (guhs.size() > MAX_REACTIES) {
            guhs = guhs.subList(0, MAX_REACTIES);
        }
        long nu = sl.getGameTime();
        boolean knuffelTerug = false;
        boolean blij = switch (e) {
            case ZWAAI, KNUFFEL, DANS, JUICH, LACH, HARTJE -> true;
            default -> false;
        };
        long dag = Pluizenblaadje.dag(sl);
        for (int i = 0; i < guhs.size(); i++) {
            GuhEntity g = guhs.get(i);
            long wanneer = nu + 4 + i * 6L + sl.getRandom().nextInt(5);
            nl.juiced.guhs.feature.emotes.Emote r = null;
            Vec3 kijk = p.getEyePosition();
            boolean hartjes = false;
            switch (e) {
                case ZWAAI -> r = nl.juiced.guhs.feature.emotes.Emote.ZWAAIEN;
                case KNUFFEL -> {
                    if (i == 0 && g.distanceTo(p) <= KNUFFEL_STRAAL) {
                        r = nl.juiced.guhs.feature.emotes.Emote.KNUFFELEN;
                        hartjes = true;
                        knuffelTerug = true;
                    } else {
                        r = nl.juiced.guhs.feature.emotes.Emote.HARTJES;
                    }
                }
                case DANS -> r = nl.juiced.guhs.feature.emotes.Emote.DANSEN;
                case JUICH -> r = nl.juiced.guhs.feature.emotes.Emote.VAHOEG;
                case LACH -> r = i == 0 ? nl.juiced.guhs.feature.emotes.Emote.ROLLEN : nl.juiced.guhs.feature.emotes.Emote.VAHOEG;
                case HARTJE -> {
                    r = nl.juiced.guhs.feature.emotes.Emote.VERLEGEN;
                    hartjes = true;
                }
                case SLAAP -> r = nl.juiced.guhs.feature.emotes.Emote.GAPEN;
                case WIJS -> kijk = p.getEyePosition().add(p.getLookAngle().scale(12));
                default -> {
                    continue;
                }
            }
            WACHT.add(new Reactie(wanneer, g.getUUID(), r, kijk, hartjes));
            if (blij && !g.isTame()) {
                Steden.Stad stad = Steden.op(g.getX(), g.getY(), g.getZ());
                if (stad != null) {
                    String sleutel = g instanceof RijkGuh rg && !rg.npcId().isEmpty() ? "npc:" + rg.npcId() : g.getUUID().toString();
                    if (tel(p, stad.id(), sleutel, dag)) {
                        sl.sendParticles(p, ParticleTypes.HEART, true, false, g.getX(), g.getY() + g.getBbHeight() + 0.4, g.getZ(), 1, 0.1, 0.1, 0.1, 0);
                    }
                }
            }
        }
        if (knuffelTerug && !guhs.isEmpty()) {
            Entity g = guhs.get(0);
            p.sendSystemMessage(Component.translatable(Ritme.P + "knuffel.terug", g.getDisplayName()), true);
        }
        return knuffelTerug;
    }

    /** Server tick: play the reactions whose moment has come. */
    static void tick(MinecraftServer server) {
        if (WACHT.isEmpty()) {
            return;
        }
        ServerLevel sl = nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.level(server);
        long nu = sl == null ? Long.MAX_VALUE : sl.getGameTime();
        Iterator<Reactie> it = WACHT.iterator();
        while (it.hasNext()) {
            Reactie r = it.next();
            if (r.wanneer() > nu) {
                continue;
            }
            it.remove();
            Entity e = null;
            for (ServerLevel l : server.getAllLevels()) {
                e = l.getEntity(r.guh());
                if (e != null) {
                    break;
                }
            }
            if (!(e instanceof GuhEntity g) || !g.isAlive()) {
                continue;
            }
            if (r.kijk() != null) {
                g.getLookControl().setLookAt(r.kijk().x, r.kijk().y, r.kijk().z, 40f, 40f);
            }
            if (r.emote() != null && (g.emotes.current() == null || g.emotes.source() == GuhEmotes.Source.SELF)) {
                g.emotes.stop();
                if (g.emotes.start(r.emote(), false, GuhEmotes.Source.SELF)) {
                    g.playSound(ModSounds.GUH_HAPPY.get(), 0.6f, g.getVoicePitch());
                }
            }
            if (r.hartjes() && g.level() instanceof ServerLevel gl) {
                gl.sendParticles(ParticleTypes.HEART, g.getX(), g.getY() + g.getBbHeight() + 0.3, g.getZ(), 3, 0.25, 0.15, 0.25, 0.02);
            }
        }
    }

    static void wis() {
        WACHT.clear();
    }
}
