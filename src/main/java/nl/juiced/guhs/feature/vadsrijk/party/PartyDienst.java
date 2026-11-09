package nl.juiced.guhs.feature.vadsrijk.party;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.hud.PartyFrames;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepConfig;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpeler;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpelers;
import nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6Raid;

/**
 * W-PARTY: everything a player does with parties, from the Party tab ({@link PartyPayloads.Actie}) and from the commands
 * ({@code /guhs vadsrijk party ...}, same rules): invite (a toast + clickable [Accepteer]/[Weiger] in chat), accept, decline, leave,
 * remove a member and hand over the lead (leader), queue the party for a dungeon or the raid (leader), recruit/dismiss a nep-speler.
 * The tab's data ({@link #staat}) is pushed to everyone who has the tab open after every party change.
 */
public final class PartyDienst {
    /** Players with the Party tab open (id -> server tick of their last request; the client asks every 2 s). */
    private static final Map<UUID, Integer> KIJKERS = new HashMap<>();
    /** At most this many players in the "Spelers in het Vadsrijk" list. */
    public static final int MAX_SPELERS = 40;

    private PartyDienst() {
    }

    static void init() {
        Partijen.VERANDERD.add(p -> {
            MinecraftServer s = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (s != null) {
                ververs(s);
            }
        });
    }

    // ---- the tab's data -----------------------------------------------------------------------------------------------------

    /** Sends the tab data to p ({@code open}: and opens the tab). */
    public static void stuur(ServerPlayer p, boolean open) {
        KIJKERS.put(p.getUUID(), p.level().getServer().getTickCount());
        PacketDistributor.sendToPlayer(p, new PartyPayloads.Staat(staat(p), open));
    }

    /** Fresh data to everyone with the tab open (asked within the last 5 s). */
    public static void ververs(MinecraftServer s) {
        int nu = s.getTickCount();
        KIJKERS.entrySet().removeIf(e -> nu - e.getValue() > 100);
        for (UUID id : new ArrayList<>(KIJKERS.keySet())) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p != null) {
                PacketDistributor.sendToPlayer(p, new PartyPayloads.Staat(staat(p), false));
            }
        }
    }

    static String zone(@Nullable String gebied) {
        return gebied == null || gebied.isEmpty() ? "" : "vadsrijk.kern.gebied." + gebied;
    }

    /** The Party tab's data for p. */
    public static CompoundTag staat(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        CompoundTag t = new CompoundTag();
        Partijen.Party party = Partijen.party(p);
        t.putBoolean("inParty", party != null);
        t.putBoolean("leider", party == null || party.leider.equals(p.getUUID()));
        t.putInt("max", Partijen.MAX);
        t.putBoolean("nepServer", NepConfig.aan());
        t.putBoolean("nepZelf", NepSpelers.magZien(p));
        ListTag leden = new ListTag();
        if (party != null) {
            for (var e : Partijen.namen(party).entrySet()) {
                UUID id = e.getKey();
                ServerPlayer m = s.getPlayerList().getPlayer(id);
                CompoundTag l = new CompoundTag();
                l.putString("id", id.toString());
                l.putBoolean("leider", party.leider.equals(id));
                l.putBoolean("ik", id.equals(p.getUUID()));
                if (m != null) {
                    SpelerData d = Spelers.data(m);
                    l.putString("naam", m.getGameProfile().name());
                    l.putBoolean("online", true);
                    l.putInt("lv", Spelers.level(m));
                    l.putString("klasse", "vadsrijk.klassen.klasse." + Spelers.klasse(m).id());
                    l.putString("spec", PartyFrames.spec(m));
                    l.putString("zone", Vadsrijk.in(m) ? zone(d.gebied) : "vadsrijk.party.tab.buiten");
                } else {
                    l.putString("naam", e.getValue());
                    l.putBoolean("online", false);
                }
                leden.add(l);
            }
        }
        long nu = s.overworld().getGameTime();
        for (NepSpeler n : NepSpelers.metgezellen(p)) {
            CompoundTag l = new CompoundTag();
            l.putString("id", n.getUUID().toString());
            l.putString("naam", n.naam());
            l.putBoolean("npc", true);
            l.putBoolean("online", true);
            l.putInt("lv", n.nepLevel());
            l.putString("klasse", "vadsrijk.klassen.klasse." + n.rol().klasse);
            l.putString("spec", n.rol().key());
            l.putBoolean("eigen", p.getUUID().equals(n.eigenaar));
            l.putBoolean("dungeon", n.dungeon != null);
            if (n.tot >= 0) {
                l.putInt("min", (int) Math.max(0, (n.tot - nu + 1199) / 1200));
            }
            leden.add(l);
        }
        t.put("leden", leden);
        // players in the Vadsrijk: nearby first
        List<ServerPlayer> rond = new ArrayList<>();
        for (ServerPlayer q : s.getPlayerList().getPlayers()) {
            if (q != p && (Wachtrijen.TEST || Vadsrijk.in(q))) {
                rond.add(q);
            }
        }
        rond.sort(Comparator.comparingDouble(q -> q.level() == p.level() ? q.distanceToSqr(p) : Double.MAX_VALUE));
        ListTag spelers = new ListTag();
        for (ServerPlayer q : rond.subList(0, Math.min(MAX_SPELERS, rond.size()))) {
            CompoundTag l = new CompoundTag();
            l.putString("id", q.getUUID().toString());
            l.putString("naam", q.getGameProfile().name());
            l.putInt("lv", Spelers.level(q));
            l.putString("zone", zone(Spelers.data(q).gebied));
            l.putInt("afstand", q.level() == p.level() ? (int) q.distanceTo(p) : -1);
            Partijen.Party qp = Partijen.party(q);
            l.putBoolean("party", qp != null);
            l.putBoolean("samen", qp != null && qp == party);
            l.putBoolean("uitgenodigd", p.getUUID().equals(Partijen.uitnodigingVan(q.getUUID())));
            spelers.add(l);
        }
        t.put("spelers", spelers);
        UUID van = Partijen.uitnodigingVan(p.getUUID());
        if (van != null) {
            ServerPlayer v = s.getPlayerList().getPlayer(van);
            t.putString("uitnodiging", v != null ? v.getGameProfile().name() : "?");
        }
        // group queueing
        ListTag dungeons = new ListTag();
        DungeonWachtrij rij = Wachtrijen.DUNGEON;
        if (rij != null) {
            for (String d : rij.dungeons()) {
                CompoundTag l = new CompoundTag();
                l.putString("id", d);
                l.put("naam", ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, rij.naam(d)).result().orElse(new CompoundTag()));
                dungeons.add(l);
            }
            String in = rij.rijVan(p.getUUID());
            if (in != null) {
                t.put("rij", ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, rij.naam(in)).result().orElse(new CompoundTag()));
            }
        }
        t.put("dungeons", dungeons);
        return t;
    }

    // ---- actions ------------------------------------------------------------------------------------------------------------

    /** A button of the Party tab. */
    public static void actie(ServerPlayer p, String actie, String arg) {
        UUID id = null;
        try {
            id = arg.isEmpty() ? null : UUID.fromString(arg);
        } catch (IllegalArgumentException ignored) {
            // (not a uuid: a dungeon id)
        }
        MinecraftServer s = p.level().getServer();
        switch (actie) {
            case "uitnodigen" -> {
                ServerPlayer doel = id == null ? null : s.getPlayerList().getPlayer(id);
                if (doel != null) {
                    nodigUit(p, doel);
                }
            }
            case "accepteer" -> accepteer(p);
            case "weiger" -> weiger(p);
            case "verlaat" -> verlaat(p);
            case "verwijder" -> {
                if (id != null) {
                    verwijder(p, id);
                }
            }
            case "leider" -> {
                if (id != null) {
                    leider(p, id);
                }
            }
            case "rij" -> rij(p, arg);
            case "raid" -> raid(p);
            case "nepweg" -> {
                if (id != null) {
                    nepWeg(p, id);
                }
            }
            default -> {
            }
        }
        stuur(p, false);
    }

    static void zeg(ServerPlayer p, Component c, ChatFormatting kleur) {
        p.sendSystemMessage(c.copy().withStyle(kleur));
    }

    static MutableComponent knop(String key, String cmd, ChatFormatting kleur) {
        return Component.literal("[").append(Component.translatable(key)).append("]").withStyle(kleur, ChatFormatting.BOLD)
                .withStyle(st -> st.withClickEvent(new ClickEvent.RunCommand(cmd)).withHoverEvent(new HoverEvent.ShowText(Component.translatable(key + ".uitleg"))));
    }

    /** Tells the other online members of p's party. */
    static void aanParty(ServerPlayer p, Component c) {
        for (ServerPlayer m : Partijen.leden(p)) {
            if (m != p) {
                zeg(m, c, ChatFormatting.LIGHT_PURPLE);
            }
        }
    }

    public static String nodigUit(ServerPlayer p, ServerPlayer doel) {
        String r = Partijen.nodigUit(p, doel);
        zeg(p, Component.translatable(r, doel.getGameProfile().name()), r.endsWith("uitgenodigd") ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD);
        if (r.endsWith("uitgenodigd")) {
            String naam = p.getGameProfile().name();
            doel.sendSystemMessage(Component.translatable("vadsrijk.party.uitnodiging", naam).withStyle(ChatFormatting.LIGHT_PURPLE).append(" ")
                    .append(knop("vadsrijk.party.knop.accepteer", "/guhs vadsrijk party accepteer", ChatFormatting.GREEN)).append(" ")
                    .append(knop("vadsrijk.party.knop.weiger", "/guhs vadsrijk party weiger", ChatFormatting.RED)));
            PacketDistributor.sendToPlayer(doel, new PartyPayloads.Uitnodiging(naam));
            doel.level().playSound(null, doel.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
            ververs(p.level().getServer());
        }
        return r;
    }

    public static String accepteer(ServerPlayer p) {
        String r = Partijen.accepteer(p);
        zeg(p, Component.translatable(r, ""), r.endsWith("erbij") ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD);
        if (r.endsWith("erbij")) {
            aanParty(p, Component.translatable("vadsrijk.party.erbij.ander", p.getGameProfile().name()));
        }
        return r;
    }

    public static String weiger(ServerPlayer p) {
        UUID van = Partijen.uitnodigingVan(p.getUUID());
        String r = Partijen.weiger(p);
        zeg(p, Component.translatable(r), ChatFormatting.GRAY);
        ServerPlayer v = van == null ? null : p.level().getServer().getPlayerList().getPlayer(van);
        if (v != null) {
            zeg(v, Component.translatable("vadsrijk.party.geweigerd.ander", p.getGameProfile().name()), ChatFormatting.GRAY);
        }
        ververs(p.level().getServer());
        return r;
    }

    public static void verlaat(ServerPlayer p) {
        if (Partijen.party(p) == null) {
            zeg(p, Component.translatable("vadsrijk.kern.party.geen_party"), ChatFormatting.GOLD);
            return;
        }
        aanParty(p, Component.translatable("vadsrijk.party.weg.ander", p.getGameProfile().name()));
        Partijen.verlaat(p.getUUID());
        zeg(p, Component.translatable("vadsrijk.kern.party.weg"), ChatFormatting.LIGHT_PURPLE);
    }

    public static String verwijder(ServerPlayer p, UUID wie) {
        Partijen.Party party = Partijen.party(p);
        String naam = party == null ? "?" : party.namen.getOrDefault(wie, "?");
        NepSpeler n = NepSpelers.van(wie);
        if (n != null) {
            naam = n.naam();
        }
        String r = Partijen.verwijder(p, wie);
        zeg(p, Component.translatable(r, naam), r.endsWith("verwijderd") ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD);
        if (r.endsWith("verwijderd")) {
            aanParty(p, Component.translatable("vadsrijk.party.verwijderd.ander", naam));
            ServerPlayer weg = p.level().getServer().getPlayerList().getPlayer(wie);
            if (weg != null) {
                zeg(weg, Component.translatable("vadsrijk.party.verwijderd.jij", p.getGameProfile().name()), ChatFormatting.GOLD);
            }
        }
        return r;
    }

    public static String leider(ServerPlayer p, UUID wie) {
        String r = Partijen.maakLeider(p, wie);
        Partijen.Party party = Partijen.party(p);
        String naam = party == null ? "?" : party.namen.getOrDefault(wie, "?");
        zeg(p, Component.translatable(r, naam), r.endsWith("nieuwe_leider") ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD);
        if (r.endsWith("nieuwe_leider")) {
            aanParty(p, Component.translatable("vadsrijk.kern.party.nieuwe_leider", naam));
        }
        return r;
    }

    public static void rij(ServerPlayer p, String dungeon) {
        Component w = Wachtrijen.dungeon(p, dungeon);
        if (w != null && !w.getString().isEmpty()) {   // (empty: the dungeon finder already told why)
            zeg(p, w, ChatFormatting.GOLD);
        }
    }

    public static void raid(ServerPlayer p) {
        Component w = Wachtrijen.raid(p);
        if (w != null) {
            zeg(p, w, ChatFormatting.GOLD);
        } else {
            for (ServerPlayer m : Wachtrijen.groep(p)) {
                zeg(m, Component.translatable("vadsrijk.party.raid.erin"), ChatFormatting.LIGHT_PURPLE);
            }
        }
    }

    public static void nepWeg(ServerPlayer p, UUID id) {
        NepSpeler n = NepSpelers.van(id);
        if (n != null && p.getUUID().equals(n.eigenaar) && n.dungeon == null) {
            NepSpelers.afscheid(n, "doei");
        }
    }

    // ---- commands -----------------------------------------------------------------------------------------------------------

    /** {@code /guhs vadsrijk party ...} (replaces kern's three subcommands; every player may use these). */
    public static LiteralArgumentBuilder<CommandSourceStack> commando() {
        return Commands.literal("party")
                .executes(c -> {
                    nl.juiced.guhs.feature.vadsrijk.menu.MenuServer.open(c.getSource().getPlayerOrException(), "party");
                    return 1;
                })
                .then(Commands.literal("uitnodigen").then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
                    nodigUit(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "speler"));
                    return 1;
                })))
                .then(Commands.literal("accepteer").executes(c -> {
                    accepteer(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("weiger").executes(c -> {
                    weiger(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("verlaat").executes(c -> {
                    verlaat(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("verwijder").then(Commands.argument("speler", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    UUID id = lid(p, StringArgumentType.getString(c, "speler"));
                    if (id == null) {
                        c.getSource().sendFailure(Component.translatable("vadsrijk.kern.party.geen_lid"));
                        return 0;
                    }
                    verwijder(p, id);
                    return 1;
                })))
                .then(Commands.literal("leider").then(Commands.argument("speler", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    UUID id = lid(p, StringArgumentType.getString(c, "speler"));
                    if (id == null) {
                        c.getSource().sendFailure(Component.translatable("vadsrijk.kern.party.geen_lid"));
                        return 0;
                    }
                    leider(p, id);
                    return 1;
                })))
                .then(Commands.literal("raid").executes(c -> {
                    raid(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("rij").then(Commands.argument("dungeon", StringArgumentType.word())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Wachtrijen.DUNGEON == null ? List.of() : Wachtrijen.DUNGEON.dungeons(), b))
                        .executes(c -> {
                            rij(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "dungeon"));
                            return 1;
                        })))
                .then(Commands.literal("nepmee").then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    NepSpeler n = nep(StringArgumentType.getString(c, "id"));
                    String r = n == null ? "vadsrijk.party.nep.weg" : NepSpelers.rekruteer(p, n);
                    zeg(p, Component.translatable(r, n == null ? "" : n.naam()), r.endsWith(".mee") ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD);
                    return 1;
                })))
                .then(Commands.literal("nepweg").then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
                    NepSpeler n = nep(StringArgumentType.getString(c, "id"));
                    if (n != null) {
                        nepWeg(c.getSource().getPlayerOrException(), n.getUUID());
                    }
                    return 1;
                })));
    }

    @Nullable
    static NepSpeler nep(String s) {
        try {
            return NepSpelers.van(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** A member of p's party by name (players and nep-spelers). */
    @Nullable
    static UUID lid(ServerPlayer p, String naam) {
        Partijen.Party party = Partijen.party(p);
        if (party == null) {
            return null;
        }
        for (var e : Partijen.namen(party).entrySet()) {
            if (e.getValue().equalsIgnoreCase(naam)) {
                return e.getKey();
            }
        }
        for (UUID n : party.npcs) {
            NepSpeler x = NepSpelers.van(n);
            if (x != null && x.naam().equalsIgnoreCase(naam)) {
                return n;
            }
        }
        return null;
    }

    static void vergeet(UUID id) {
        KIJKERS.remove(id);
    }
}
