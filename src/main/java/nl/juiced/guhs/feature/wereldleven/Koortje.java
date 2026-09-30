package nl.juiced.guhs.feature.wereldleven;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.KnusSignalen;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * Het koortje (2.8, wereldleven): play one of the six songs of the liedjesboekje on a guh-xylofoon (eight bars: do re mi
 * fa sol la si do) and every guh close by sings along (the ZINGEN emote: swaying, music notes); the tuintjes around grow
 * faster (KnusSignalen.zang). The first time you play a song it goes into your liedjesboek (Guhdex, Knus tab) with a
 * reward; all six give the koorstrikje. The guh-fluitje whistles the last song you played (or the toonladder), and the
 * guhs around sing along with that too.
 */
public final class Koortje {
    /** The eight bars, in semitones above the lowest one (a major scale). */
    public static final int[] HALVE_TONEN = {0, 2, 4, 5, 7, 9, 11, 12};
    public static final int NOTEN = 8;
    /** How far from the xylofoon you may stand, and how far guhs hear it and sing along. */
    public static final double BEREIK = 8, ZANG_BEREIK = 8;
    /** More than this many ticks between two notes: a new start. */
    public static final int PAUZE = 60;
    static final String LAATSTE = "guhs_wereldleven_laatste_lied";

    public enum Liedje {
        /** do re mi fa sol la si do */
        TOONLADDER(2, 0, 1, 2, 3, 4, 5, 6, 7),
        /** Vader Jacob */
        VADER_GUH(3, 0, 1, 2, 0, 0, 1, 2, 0, 2, 3, 4, 2, 3, 4),
        /** Altijd is Kortjakje ziek (Twinkle twinkle) */
        ALTIJD_VADS(3, 0, 0, 4, 4, 5, 5, 4, 3, 3, 2, 2, 1, 1, 0),
        /** Alle eendjes zwemmen in het water */
        ALLE_GUHTJES(4, 0, 1, 2, 3, 4, 4, 5, 5, 5, 5, 4),
        /** In de maneschijn (Au clair de la lune) */
        MANESCHIJN(4, 0, 0, 0, 1, 2, 1, 0, 2, 1, 1, 0),
        /** Ode an die Freude */
        ODE_AAN_DE_KNABBEL(5, 2, 2, 3, 4, 4, 3, 2, 1, 0, 0, 1, 2, 2, 1, 1);

        /** Kaasknabbels the first time you play it (one more than it used to be: every reward +1). */
        public final int beloning;
        public final int[] noten;

        Liedje(int beloning, int... noten) {
            this.beloning = beloning + 1;
            this.noten = noten;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static List<String> ids() {
            List<String> ids = new ArrayList<>();
            for (Liedje l : values()) {
                ids.add(l.id());
            }
            return ids;
        }

        @Nullable
        public static Liedje byId(String id) {
            for (Liedje l : values()) {
                if (l.id().equals(id)) {
                    return l;
                }
            }
            return null;
        }
    }

    /** The pitch of a bar for the note-block sounds (the eight bars span an octave around pitch 1). */
    public static float pitch(int noot) {
        return (float) Math.pow(2.0, (HALVE_TONEN[Math.max(0, Math.min(NOTEN - 1, noot))] - 6) / 12.0);
    }

    // --- playing the xylofoon -------------------------------------------------------------------------------------------------

    private record Sessie(BlockPos pos, Deque<Integer> noten, long laatst) {
    }

    private static final Map<UUID, Sessie> SESSIES = new ConcurrentHashMap<>();

    /** A player hits a bar of the xylofoon at pos (from the xylofoon screen). Returns the song it finished, or null. */
    @Nullable
    public static Liedje noot(ServerPlayer player, BlockPos pos, int noot) {
        ServerLevel level = player.serverLevel();
        if (noot < 0 || noot >= NOTEN || !level.getBlockState(pos).is(WereldlevenFeature.GUH_XYLOFOON.get())
                || player.distanceToSqr(Vec3.atCenterOf(pos)) > BEREIK * BEREIK) {
            return null;
        }
        // the others hear it (the player plays it on their own side already)
        level.playSound(player, pos, WereldlevenFeature.XYLOFOON.get(), SoundSource.RECORDS, 1f, pitch(noot));
        level.sendParticles(WereldlevenFeature.ZANGNOOTJE.get(), pos.getX() + 0.2 + 0.08 * noot, pos.getY() + 0.9, pos.getZ() + 0.5, 1, 0.05, 0.05, 0.05, 0);
        long now = level.getGameTime();
        Sessie s = SESSIES.get(player.getUUID());
        if (s == null || !s.pos().equals(pos) || now - s.laatst() > PAUZE) {
            s = new Sessie(pos, new ArrayDeque<>(), now);
        }
        s.noten().addLast(noot);
        while (s.noten().size() > 24) {
            s.noten().removeFirst();
        }
        SESSIES.put(player.getUUID(), new Sessie(pos, s.noten(), now));
        Liedje af = herken(s.noten());
        if (af != null) {
            s.noten().clear();
            gespeeld(player, pos, af);
        }
        return af;
    }

    /** The song the last notes end with (the longest match), or null. */
    @Nullable
    public static Liedje herken(Deque<Integer> gespeeld) {
        List<Integer> list = new ArrayList<>(gespeeld);
        Liedje best = null;
        for (Liedje l : Liedje.values()) {
            int n = l.noten.length;
            if (list.size() < n) {
                continue;
            }
            boolean ok = true;
            for (int i = 0; i < n && ok; i++) {
                ok = list.get(list.size() - n + i) == l.noten[i];
            }
            if (ok && (best == null || n > best.noten.length)) {
                best = l;
            }
        }
        return best;
    }

    /** A whole song was played: the koortje sings, the liedjesboek, the rewards. */
    public static void gespeeld(ServerPlayer player, BlockPos pos, Liedje lied) {
        int zangers = zing(player.serverLevel(), Vec3.atCenterOf(pos), ZANG_BEREIK);
        for (GuhEntity eigen : nl.juiced.guhs.feature.band.Band.samenGuhs(player, 16)) {   // 2.10: your own guhs heard the song
            nl.juiced.guhs.feature.band.Band.moment(eigen, player, nl.juiced.guhs.feature.band.Moment.LIEDJE, "koortje:" + lied.id());
        }
        GuhQuests.saved(player).putString(LAATSTE, lied.id());
        KnusVoortgang.tel(player, WereldlevenVoortgang.KOORTJES, 1);
        Component naam = Component.translatable("gui.guhs.knus.liedjesboek." + lied.id());
        if (zangers > 0) {
            WereldlevenVoortgang.toon(player, "wereldleven_koortje");
            player.displayClientMessage(Component.translatable("gui.guhs.wereldleven.koortje", naam, zangers).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else {
            player.displayClientMessage(Component.translatable("gui.guhs.wereldleven.koortje_leeg", naam).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        if (KnusVoortgang.ontdek(player, WereldlevenVoortgang.LIEDJESBOEK, lied.id())) {
            Minigames.give(player, new ItemStack(ModItems.KAAS_KNABBELS.get(), lied.beloning));
            player.sendSystemMessage(Component.translatable("gui.guhs.wereldleven.nieuw_liedje", naam, lied.beloning).withStyle(ChatFormatting.GOLD));
            if (KnusVoortgang.ontdekt(player, WereldlevenVoortgang.LIEDJESBOEK).size() >= Liedje.values().length) {
                Minigames.give(player, new ItemStack(ModItems.clothingItem(GuhClothes.KOORSTRIKJE)));
                WereldlevenVoortgang.toon(player, "wereldleven_liedjesboek");
                player.sendSystemMessage(Component.translatable("gui.guhs.wereldleven.liedjesboek_vol").withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** Every guh around sings along (ZINGEN: swaying, notes); the tuintjes hear it. Returns how many sing. */
    public static int zing(ServerLevel level, Vec3 at, double range) {
        int n = 0;
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, new AABB(at, at).inflate(range, 4, range))) {
            if (guh.emotes.current() == Emote.ZINGEN || guh.emotes.start(Emote.ZINGEN, false, GuhEmotes.Source.SELF)) {
                n++;
                level.sendParticles(WereldlevenFeature.ZANGNOOTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 2,
                        guh.getBbWidth() * 0.3, 0.1, guh.getBbWidth() * 0.3, 0);
            }
        }
        KnusSignalen.zang(level, BlockPos.containing(at), (int) range);
        return n;
    }

    // --- the fluitje: a melody played over a few seconds -----------------------------------------------------------------------

    private record Melodie(ServerLevel level, UUID speler, int[] noten, int[] index, long[] volgende) {
    }

    private static final List<Melodie> MELODIEEN = new ArrayList<>();

    /** Whistles the last song this player played (or the toonladder): the guhs around sing along. */
    public static void fluit(ServerPlayer player) {
        Liedje lied = Liedje.byId(GuhQuests.saved(player).getString(LAATSTE));
        if (lied == null) {
            lied = Liedje.TOONLADDER;
        }
        ServerLevel level = player.serverLevel();
        synchronized (MELODIEEN) {
            MELODIEEN.removeIf(m -> m.speler().equals(player.getUUID()));
            MELODIEEN.add(new Melodie(level, player.getUUID(), lied.noten, new int[] {0}, new long[] {level.getGameTime()}));
        }
        int zangers = zing(level, player.position(), ZANG_BEREIK - 2);
        Component naam = Component.translatable("gui.guhs.knus.liedjesboek." + lied.id());
        player.displayClientMessage(Component.translatable(zangers > 0 ? "gui.guhs.wereldleven.fluit" : "gui.guhs.wereldleven.fluit_leeg", naam, zangers)
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        GuhAdvancements.grant(player, "wereldleven_fluitje");
        if (zangers > 0) {
            KnusVoortgang.tel(player, WereldlevenVoortgang.KOORTJES, 1);
            WereldlevenVoortgang.toon(player, "wereldleven_koortje");
        }
    }

    private static void onTick(ServerTickEvent.Post event) {
        synchronized (MELODIEEN) {
            if (MELODIEEN.isEmpty()) {
                return;
            }
            Iterator<Melodie> it = MELODIEEN.iterator();
            while (it.hasNext()) {
                Melodie m = it.next();
                ServerPlayer p = m.level().getServer().getPlayerList().getPlayer(m.speler());
                if (p == null || p.level() != m.level() || m.index()[0] >= m.noten().length) {
                    it.remove();
                    continue;
                }
                if (m.level().getGameTime() < m.volgende()[0]) {
                    continue;
                }
                int noot = m.noten()[m.index()[0]++];
                m.volgende()[0] = m.level().getGameTime() + 5;
                m.level().playSound(null, p.getX(), p.getEyeY(), p.getZ(), WereldlevenFeature.FLUITJE.get(), SoundSource.PLAYERS, 0.9f, pitch(noot));
                Vec3 mond = p.getEyePosition().add(p.getLookAngle().scale(0.4));
                m.level().sendParticles(WereldlevenFeature.FLUITSTOOM.get(), mond.x, mond.y - 0.1, mond.z, 1, 0.02, 0.02, 0.02, 0.01);
                if (m.index()[0] % 3 == 0) {
                    m.level().sendParticles(WereldlevenFeature.ZANGNOOTJE.get(), mond.x, mond.y + 0.2, mond.z, 1, 0.1, 0.1, 0.1, 0);
                }
            }
        }
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(Koortje::onTick);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            SESSIES.clear();
            synchronized (MELODIEEN) {
                MELODIEEN.clear();
            }
        });
    }

    /** (Tests) forget a player's half-played song. */
    public static void vergeet(ServerPlayer player) {
        SESSIES.remove(player.getUUID());
    }

    private Koortje() {
    }
}
