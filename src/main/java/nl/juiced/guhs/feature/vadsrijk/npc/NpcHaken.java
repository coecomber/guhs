package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * Quest progress from the kern hooks (CONTRACT_VADSRIJK 9: Haken.VERSLAGEN, GEBIED_BETREDEN, KAMPVUUR, LEVEL_OP, FLAUW,
 * BINNEN), the training dummies, and a ticker for what has no hook: places (poi objectives), escorts arriving, and the
 * secrets of the zone (lost knuffels, the 5 clues to the Mika-spion, treasure spots) found by walking up to them.
 */
public final class NpcHaken {
    private static final Map<UUID, Long> POP_PAUZE = new HashMap<>();

    private NpcHaken() {
    }

    static void registreer() {
        Haken.VERSLAGEN.add(NpcHaken::verslagen);
        Haken.GEBIED_BETREDEN.add((p, gebied) -> gebied(p, gebied));
        Haken.KAMPVUUR.add(NpcHaken::kampvuur);
        Haken.BLOK_GERAAKT.add(NpcHaken::popGeraakt);   // (fix gevecht: arrows, magic and skills on a dummy count too)
        Haken.LEVEL_OP.add((p, wat) -> {
            Questlog.kijkLive(p);
            NpcMarkers.vies(p);
        });
        Haken.FLAUW.add(p -> Tips.toon(p, "vadsrijk.npc.tip.flauw"));
        Haken.BINNEN.add((p, in) -> {
            Verhaal.binnen(p, in);
            NpcMarkers.vies(p);
        });
    }

    // ---- defeats -------------------------------------------------------------------------------------------------------------------

    static void verslagen(Haken.Verslagen v) {
        VadsrijkMob mob = v.mob();
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        Gebieden.Gebied g = Gebieden.op(mob.getX(), mob.getY(), mob.getZ());
        String gebied = g == null ? "" : g.id();
        boolean baas = mob instanceof VadsrijkBaas;
        Set<ServerPlayer> wie = new LinkedHashSet<>();
        for (ServerPlayer p : v.helpers()) {
            wie.add(p);
            wie.addAll(Partijen.ledenInBuurt(p, 48));   // (ronde 11: shared progress on cheer-up quests in a party)
        }
        for (ServerPlayer p : wie) {
            telWezen(p, type);
            Questlog.tel(p, Quest.Doel.Type.OPVROLIJK, d -> pastMob(d.doel(), type, gebied), 1);
            if (mob.elite()) {
                Questlog.tel(p, Quest.Doel.Type.ELITE, d -> pastMob(d.doel(), type, gebied), 1);
            }
            if (baas || type.equals("guhs:vr_klodderreus")) {
                Questlog.tel(p, Quest.Doel.Type.BAAS, d -> pastMob(d.doel(), type, gebied), 1);
                if (gebied.equals("zwamzoet")) {
                    NpcApi.meld(p, "spion", "gevangen");   // (the only boss in the city: bazen's Mika-spion on the caps)
                }
            }
            if (type.equals("guhs:vr_klodderreus") && g != null && g.id().equals("knuffelkamp")) {
                Verhaal.herstel(p);
                NpcRollen.doe("kampklodder", p);   // (wezens: the Kampklodder moves in; one shared entity)
            }
            if (mob.element() != Element.GEEN) {
                Tips.toon(p, "vadsrijk.npc.tip.elementen");
            }
            questBuit(p, type, gebied);
        }
    }

    /** "*", "gebied:&lt;area&gt;" or an entity type id (with or without "guhs:"). */
    public static boolean pastMob(String doel, String type, String gebied) {
        if (doel.equals("*")) {
            return true;
        }
        if (doel.startsWith("gebied:")) {
            return List.of(doel.substring(7).split("\\|")).contains(gebied);   // U-QUESTROBOT: "gebied:a|b" = any of these areas
        }
        return doel.equals(type) || ("guhs:" + doel).equals(type);
    }

    /** Quest items drop for you (only while you still need them). */
    static void questBuit(ServerPlayer p, String type, String gebied) {
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (d.type() == Quest.Doel.Type.VERZAMEL && d.kans() > 0 && !Questlog.doelKlaar(p, q, i) && pastMob(d.bron().isEmpty() ? "*" : d.bron(), type, gebied)
                        && p.getRandom().nextDouble() < d.kans()) {
                    Questlog.geef(p, new ItemStack(Questlog.item(d.doel())));
                    int nu = Questlog.waarde(p, q, i);
                    p.sendOverlayMessage(Component.translatable("vadsrijk.npc.voortgang", Component.translatable(q.doelKey(i)), nu, d.n())
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    p.level().playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.4f);
                }
            }
        }
        Questlog.kijkLive(p);
    }

    static void telWezen(ServerPlayer p, String type) {
        CompoundTag w = Questlog.map(p, "wezens");
        w.putInt(type, w.getIntOr(type, 0) + 1);
    }

    // ---- places and campfires --------------------------------------------------------------------------------------------------

    static void gebied(ServerPlayer p, String gebied) {
        Questlog.tel(p, Quest.Doel.Type.GEBIED, d -> List.of(d.doel().split("\\|")).contains(gebied), 1);
    }

    static void kampvuur(ServerPlayer p, BlockPos pos) {
        Gebieden.Gebied g = Gebieden.op(pos.getX(), pos.getY(), pos.getZ());
        String gebied = g == null ? "" : g.id();
        Questlog.tel(p, Quest.Doel.Type.KAMPVUUR, d -> d.doel().equals("*") || d.doel().equals(gebied), 1);
    }

    /** (PlayerInteractEvent.LeftClickBlock) a smack on a training dummy (target or hay block) in Knuffelkamp. */
    static void klikBlok(PlayerInteractEvent.LeftClickBlock e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            popGeraakt(p, e.getPos());
        }
    }

    /**
     * A hit on a training dummy (target or hay block) in Knuffelkamp: a hand smack, a ranged/magic shot or a skill aimed at it
     * (fix gevecht, through kern's {@link Haken#BLOK_GERAAKT}). At most one count per 8 ticks.
     */
    static void popGeraakt(ServerPlayer p, BlockPos pos) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        BlockState s = p.level().getBlockState(pos);
        if (!s.is(Blocks.TARGET) && !s.is(Blocks.HAY_BLOCK)) {
            return;
        }
        Gebieden.Gebied g = Gebieden.op(pos.getX(), pos.getY(), pos.getZ());
        long nu = p.level().getGameTime();
        if (g == null || !g.id().equals("knuffelkamp") || nu < POP_PAUZE.getOrDefault(p.getUUID(), 0L)) {
            return;
        }
        POP_PAUZE.put(p.getUUID(), nu + 8);
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.1);
        }
        p.level().playSound(null, pos, SoundEvents.WOOL_HIT, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1.2f);
        Questlog.tel(p, Quest.Doel.Type.POP, d -> true, 1);
    }

    // ---- the ticker ---------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        long t = server.getTickCount();
        if (t % 10 != 4) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || RijkScenes.bezig(p)) {
                continue;
            }
            plekken(p);
            klasseGekozen(p);
            escortes(p, sl);
            geheimen(p, sl);
            if (t % 40 == 4) {
                Questlog.kijkLive(p);
            }
        }
    }

    /** poi objectives (GEBIED / GEHEIM "poi:x"): you are there. */
    static void plekken(ServerPlayer p) {
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if ((d.type() == Quest.Doel.Type.GEBIED || d.type() == Quest.Doel.Type.GEHEIM) && d.doel().startsWith("poi:") && !Questlog.doelKlaar(p, q, i)) {
                    BlockPos b = NpcPlekken.poi(d.doel().substring(4));
                    if (b != null && bij(p, b, NpcPlekken.POI_STRAAL, NpcPlekken.hoogteVan(d.doel().substring(4)))) {
                        Questlog.tel(p, d.type(), x -> x == d, 1);
                        if (d.type() == Quest.Doel.Type.GEHEIM) {
                            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.2f);
                        }
                    }
                }
            }
        }
    }

    /** The klassen slice's first-class pick ({@code extra("klassen").gekozen}) completes a KLASSE objective. */
    static void klasseGekozen(ServerPlayer p) {
        if (Spelers.data(p).extra("klassen").getBooleanOr("gekozen", false)) {
            for (Quest q : Questlog.actieve(p)) {
                for (int i = 0; i < q.doelen().size(); i++) {
                    if (q.doelen().get(i).type() == Quest.Doel.Type.KLASSE && !Questlog.doelKlaar(p, q, i)) {
                        NpcApi.meld(p, "klasse", Spelers.data(p).klasse);
                        return;
                    }
                }
            }
        }
    }

    /** An escort that reached its destination: done; when it is also the one to hand in to, straight to the hand-in. */
    static void escortes(ServerPlayer p, ServerLevel sl) {
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (d.type() != Quest.Doel.Type.ESCORTE || Questlog.doelKlaar(p, q, i)) {
                    continue;
                }
                RijkGuh g = Npcs.levend(sl, d.doel());
                BlockPos naar = NpcPlekken.poi(d.bron().startsWith("poi:") ? d.bron().substring(4) : d.bron());
                if (g == null || naar == null || !p.getUUID().equals(g.volgt())) {
                    continue;
                }
                if (bij(g, naar, NpcPlekken.POI_STRAAL + 2, NpcPlekken.POI_HOOGTE)) {
                    g.volg(null);
                    g.rust(naar, 20 * 60 * 5);
                    Questlog.tel(p, Quest.Doel.Type.ESCORTE, x -> x == d, 1);
                    g.emotes.start(nl.juiced.guhs.feature.emotes.Emote.VAHOEG, false, nl.juiced.guhs.feature.emotes.GuhEmotes.Source.SELF);
                    if (q.inleveren().equals(d.doel()) && Questlog.allesKlaar(p, q)) {
                        QuestMotor.klik(p, g);
                    }
                }
            }
        }
    }

    // ---- secrets -------------------------------------------------------------------------------------------------------------------

    static void geheimen(ServerPlayer p, ServerLevel sl) {
        zoek(p, sl, NpcPlekken.KNUFFEL, "knuffels", ParticleTypes.HEART);
        zoek(p, sl, NpcPlekken.AANWIJZING, "aanwijzingen", ParticleTypes.ENCHANT);
        zoek(p, sl, NpcPlekken.SCHAT, "schatten", ParticleTypes.WAX_ON);
    }

    private static void zoek(ServerPlayer p, ServerLevel sl, Map<String, BlockPos> plekken, String soort, ParticleOptions glinster) {
        CompoundTag gevonden = Questlog.map(p, soort);
        for (Map.Entry<String, BlockPos> e : plekken.entrySet()) {
            if (gevonden.getBooleanOr(e.getKey(), false)) {
                continue;
            }
            BlockPos b = e.getValue();
            double dx = p.getX() - (b.getX() + 0.5), dz = p.getZ() - (b.getZ() + 0.5);
            double d2 = dx * dx + dz * dz;
            if (d2 < 14 * 14 && Math.abs(p.getY() - b.getY()) < 12) {
                // a little glitter only you can see, so a secret is findable without a map marker
                sl.sendParticles(p, glinster, false, false, b.getX() + 0.5, b.getY() + 0.8, b.getZ() + 0.5, 2, 0.25, 0.3, 0.25, 0.01);
            }
            if (bij(p, b, NpcPlekken.VIND_STRAAL, NpcPlekken.VIND_HOOGTE)) {
                vind(p, sl, soort, e.getKey(), b);
            }
        }
    }

    static void vind(ServerPlayer p, ServerLevel sl, String soort, String id, BlockPos b) {
        CompoundTag gevonden = Questlog.map(p, soort);
        if (gevonden.getBooleanOr(id, false)) {
            return;
        }
        gevonden.putBoolean(id, true);
        int n = gevonden.size();
        Vec3 c = Vec3.atCenterOf(b);
        switch (soort) {
            case "knuffels" -> {
                sl.sendParticles(p, ParticleTypes.HEART, false, false, c.x, c.y + 0.5, c.z, 8, 0.4, 0.4, 0.4, 0.05);
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.5f);
                p.sendSystemMessage(Component.translatable("vadsrijk.npc.knuffel.gevonden", Component.translatable("vadsrijk.npc.knuffel." + id), n,
                        NpcPlekken.KNUFFEL.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case "aanwijzingen" -> {
                p.level().playSound(null, b, SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.8f);
                Praat.scene(p, QuestMotor.SLEUTEL + "_aanwijzing", List.of(new Praat.Regel(null, "vadsrijk.npc.aanwijzing.naam", "vadsrijk.npc.aanwijzing." + id),
                        new Praat.Regel(null, "vadsrijk.npc.aanwijzing.naam", "vadsrijk.npc.aanwijzing.teller", n, NpcPlekken.AANWIJZING.size())));
                Quest q = Quests.van("geheim_spion");
                if (q != null && Questlog.status(p, q.id()) == 0) {
                    Questlog.neemAan(p, q);
                }
                if (n >= NpcPlekken.AANWIJZING.size()) {
                    p.sendSystemMessage(Component.translatable("vadsrijk.npc.spion.weet_het").withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
            case "schatten" -> {
                p.level().playSound(null, b, SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.3f);
                RijkScenes.titel(p, Component.translatable("vadsrijk.npc.schat.titel").withStyle(ChatFormatting.GOLD),
                        Component.translatable("vadsrijk.npc.schat." + id), 60, 0xFFD24A);
                var t = Beloningen.stukVoor(p);
                List<ItemStack> buit = t == null ? List.of()
                        : List.of(Rollen.stack(Rollen.rol(t, Math.max(1, Spelers.level(p)), id.equals("kristalkamer") ? Zeldzaamheid.EPISCH : Zeldzaamheid.ZELDZAAM,
                                p.getRandom())));
                LootBundel.laatVallen(p, c.add(0, 0.8, 0), new LootTabel.Resultaat(buit, 2500), 2);
            }
            default -> {
            }
        }
        NpcFtb.geheim(p, soort, id, n);
        Questlog.kijkLive(p);
    }

    // U-QUESTROBOT: the quest robot drives the same code the ticker runs (no state set by hand)
    /** U-QUESTROBOT: the poi check of the ticker for one player (you stand there now: GEBIED/GEHEIM "poi:x" count). */
    public static void robotPlekken(ServerPlayer p) {
        plekken(p);
    }

    /** U-QUESTROBOT: finding one secret (knuffels / aanwijzingen / schatten), as walking up to it does. */
    public static void robotVind(ServerPlayer p, ServerLevel sl, String soort, String id, BlockPos b) {
        vind(p, sl, soort, id, b);
    }

    public static boolean bij(net.minecraft.world.entity.Entity e, BlockPos b, double straal, double hoogte) {
        double dx = e.getX() - (b.getX() + 0.5), dz = e.getZ() - (b.getZ() + 0.5);
        return dx * dx + dz * dz <= straal * straal && Math.abs(e.getY() - b.getY()) <= hoogte;
    }
}
