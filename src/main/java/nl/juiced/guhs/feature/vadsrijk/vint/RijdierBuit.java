package nl.juiced.guhs.feature.vadsrijk.vint;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * V-INT, the user's loot decision (round U): every MOUNT drop is a personal roll of {@link #KANS} (10 %) per player per kill/clear,
 * with a pity counter per player per source: after {@link #PITY} misses in a row the next try is a sure drop. The counter lives on
 * the character ({@code Spelers.data(p).extra("vint_buit")}), so every character has its own luck. Rare item drops stay as they
 * were (personal rolls).
 *
 * <p>Sources: {@link #bron} (a boss entity type: rolled for every helper of the kill through {@code Haken.VERSLAGEN}) or
 * {@link #rol(ServerPlayer, String)} from code (a clear, a chest, the end of a run). Zone 6 (u-z6wezens, u-z6raid) registers its own
 * the same way. Dev: {@code /guhs vadsrijk loot kansen}.
 */
public final class RijdierBuit {
    public static final double KANS = 0.10;
    /** After this many misses in a row the next try is a sure drop (so at most PITY + 1 tries). */
    public static final int PITY = 15;
    static final String DATA = "vint_buit";

    /** One mount source. {@code baasType} = an entity type id rolled on its kill (null: only rolled from code). */
    public record Bron(String id, String naam, @Nullable String baasType, Predicate<ServerPlayer> mag, Predicate<ServerPlayer> heeft,
            Consumer<ServerPlayer> geef) {
    }

    public static final Map<String, Bron> BRONNEN = new LinkedHashMap<>();

    /**
     * 1.10.3 "pech zichtbaar": what a source gives and where it drops, for the mount screen ({@link #schermData}). {@code kleur} = the
     * Pluimguh colour it gives (null: the mount itself or a look of it); {@code per} = "kill" | "clear" | "run" (lang
     * {@code vadsrijk.vint.pech.per.<per>}); {@code waar} = the lang key of "where" with an optional lang key {@code waarArg} as its %s.
     */
    public record Doel(RijdierSoort soort, @Nullable RijdierSoort.PluimKleur kleur, String per, String waar, @Nullable String waarArg) {
    }

    public static final Map<String, Doel> DOELEN = new LinkedHashMap<>();
    /** Test hook: the next roll (null = random). */
    @Nullable
    static Double testRol;

    private RijdierBuit() {
    }

    public static void bron(String id, String naam, @Nullable String baasType, Predicate<ServerPlayer> mag, Predicate<ServerPlayer> heeft,
            Consumer<ServerPlayer> geef) {
        BRONNEN.put(id, new Bron(id, naam, baasType, mag, heeft, geef));
    }

    /** Registers what source {@code id} gives and where (shown with the pity counter in the mount screen). */
    public static void doel(String id, Doel d) {
        DOELEN.put(id, d);
    }

    static void register() {
        // zone 1: Opperzwam (Zwamtempel) -> Zwamzwijntje
        bron("opperzwam_zwamzwijntje", "Zwamzwijntje (Opperzwam, zone 1)", "guhs:vr_baas_opperzwam", p -> true,
                p -> Rijdieren.heeft(p, RijdierSoort.ZWAMZWIJNTJE), p -> Rijdieren.geef(p, RijdierSoort.ZWAMZWIJNTJE));
        // zone 2: Spiegelzucht -> the Kristal Pluimguh
        bron("spiegelzucht_kristal", "Kristal-Pluimguh (Spiegelzucht, zone 2)", "guhs:vr_z2_baas_spiegelzucht",
                p -> Rijdieren.heeft(p, RijdierSoort.PLUIMGUH), p -> Rijdieren.heeftKleur(p, RijdierSoort.PluimKleur.KRISTAL),
                p -> Rijdieren.geefKleur(p, RijdierSoort.PluimKleur.KRISTAL));
        // zone 3: the end of a Vahoeg run (Baron Bleu) -> the gold Pluimguh (rolled by GoudenVeren.rolGoudenPluimguh)
        bron("vahoeg_goud", "Gouden Pluimguh (einde Vahoeg-run, zone 3)", null, p -> true,
                nl.juiced.guhs.feature.vadsrijk.zone3.systemen.GoudenVeren::heeftGoud,
                nl.juiced.guhs.feature.vadsrijk.zone3.systemen.GoudenVeren::geefGoudPubliek);
        doel("opperzwam_zwamzwijntje", new Doel(RijdierSoort.ZWAMZWIJNTJE, null, "kill", "vadsrijk.vint.pech.waar.opperzwam", null));
        doel("spiegelzucht_kristal", new Doel(RijdierSoort.PLUIMGUH, RijdierSoort.PluimKleur.KRISTAL, "kill", "vadsrijk.vint.pech.waar.spiegelzucht", null));
        doel("vahoeg_goud", new Doel(RijdierSoort.PLUIMGUH, RijdierSoort.PluimKleur.GOUD, "run", "vadsrijk.vint.pech.waar.vahoeg", null));
        // the old automatic first-kill sources are drops now (not handed out by Aankleden.inhalen any more)
        var r = nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.SOORT_BRON;
        r.put(RijdierSoort.ZWAMZWIJNTJE, nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.Bron.api("drop:opperzwam_zwamzwijntje"));
        nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.PLUIM_BRON.put(RijdierSoort.PluimKleur.KRISTAL,
                nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.Bron.api("drop:spiegelzucht_kristal"));

        Haken.VERSLAGEN.add(v -> {
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(v.mob().getType()).toString();
            for (Bron b : BRONNEN.values()) {
                if (type.equals(b.baasType())) {
                    for (ServerPlayer p : v.helpers()) {
                        rol(p, b.id());
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(RijdierBuit::commando);
    }

    private static CompoundTag data(ServerPlayer p) {
        return Spelers.data(p).extra(DATA);
    }

    public static int pech(ServerPlayer p, String id) {
        return data(p).getIntOr(id, 0);
    }

    /** One personal roll at source {@code id}. True = the mount was given now. Owned already / not allowed yet: nothing, no count. */
    public static boolean rol(ServerPlayer p, String id) {
        Bron b = BRONNEN.get(id);
        if (b == null || b.heeft().test(p) || !b.mag().test(p)) {
            return false;
        }
        int pech = pech(p, id);
        double r = testRol != null ? testRol : p.getRandom().nextDouble();
        testRol = null;
        if (pech >= PITY || r < KANS) {
            data(p).remove(id);
            b.geef().accept(p);
            nl.juiced.guhs.feature.vadsrijk.bazenboek.Bazenboek.mountGekregen(p, id);   // 1.10.3: in the Bazen tab's "received"
            p.sendSystemMessage(Component.translatable("vadsrijk.vint.buit.raak", b.naam(), pech + 1).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.1f);
            return true;
        }
        data(p).putInt(id, pech + 1);
        p.sendOverlayMessage(misTekst(b.naam(), pech + 1).withStyle(ChatFormatting.GRAY));
        return false;
    }

    /** The miss line: "no X this time (10 %) · pity n/15 · n more tries until a sure drop" ({@code pech} = misses now, this one included). */
    static net.minecraft.network.chat.MutableComponent misTekst(String naam, int pech) {
        int nog = Math.max(0, PITY - pech);
        return nog == 0 ? Component.translatable("vadsrijk.vint.buit.mis.zeker", naam, Math.round(KANS * 100), pech, PITY)
                : Component.translatable("vadsrijk.vint.buit.mis", naam, Math.round(KANS * 100), pech, PITY, nog);
    }

    /**
     * 1.10.3 "pech zichtbaar": the pity counters of one mount row of the mount screen ({@code economie.Stal.data}): a list
     * {@code pech} of {bron, kleur ("" = the mount / a look of it), per, waar, waarArg, pech, pity, kans (%), mag} for every source
     * of this mount that you don't own yet. The screen shows them as "Kans: 10 % per kill · zeker na nog n keer".
     */
    public static void schermData(ServerPlayer p, RijdierSoort s, CompoundTag t) {
        net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
        for (Bron b : BRONNEN.values()) {
            Doel d = DOELEN.get(b.id());
            if (d == null || d.soort() != s || b.heeft().test(p)) {
                continue;
            }
            CompoundTag c = new CompoundTag();
            c.putString("bron", b.id());
            c.putString("kleur", d.kleur() == null ? "" : d.kleur().id());
            c.putString("per", d.per());
            c.putString("waar", d.waar());
            c.putString("waarArg", d.waarArg() == null ? "" : d.waarArg());
            c.putInt("pech", Math.min(PITY, pech(p, b.id())));
            c.putInt("pity", PITY);
            c.putInt("kans", (int) Math.round(KANS * 100));
            c.putBoolean("mag", b.mag().test(p));
            l.add(c);
        }
        if (!l.isEmpty()) {
            t.put("pech", l);
        }
    }

    private static void commando(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> l = Commands.literal("loot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        l.then(Commands.literal("kansen").executes(c -> {
            ServerPlayer p = c.getSource().getPlayer();
            StringBuilder s = new StringBuilder("Rijdier-drops: " + Math.round(KANS * 100) + "% per speler per kill/clear, zeker na " + PITY
                    + " keer mis (per karakter, per bron).");
            for (Bron b : BRONNEN.values()) {
                s.append("\n - ").append(b.naam()).append(b.baasType() != null ? "  [" + b.baasType() + "]" : "  [via code]");
                if (p != null) {
                    s.append(b.heeft().test(p) ? "  ✔ al van jou" : "  pech: " + pech(p, b.id()) + "/" + PITY);
                }
            }
            String t = s.toString();
            c.getSource().sendSuccess(() -> Component.literal(t), false);
            return 1;
        }));
        l.then(Commands.literal("pech").then(Commands.argument("bron", com.mojang.brigadier.arguments.StringArgumentType.word())
                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(BRONNEN.keySet(), b))
                .then(Commands.argument("n", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, PITY)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    data(p).putInt(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "bron"),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "n"));
                    c.getSource().sendSuccess(() -> Component.literal("pech gezet"), false);
                    return 1;
                }))));
        l.then(Commands.literal("rol").then(Commands.argument("bron", com.mojang.brigadier.arguments.StringArgumentType.word())
                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(BRONNEN.keySet(), b)).executes(c -> {
                    boolean ok = rol(c.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "bron"));
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "raak!" : "mis (of al van jou / mag nog niet)"), false);
                    return 1;
                })));
        e.getDispatcher().register(VadsrijkCommando.wortel(l));
    }
}
