package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import nl.juiced.guhs.feature.vadsrijk.gilden.Rang.Recht;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;

/**
 * U-GILDEN: the bond "screen" as a clickable chat panel ({@code /bond}): overview, members (with rank buttons), week tasks,
 * perks, castle (donate / build), recruiting, banner designer, the board of recruiting bonds, invites and the teleport stone.
 * Every button runs a {@code /bond ...} command, so the same rules ({@link BondActies}) apply as when typing. Chosen over a new
 * client screen: no free key left that everybody finds, it works from anywhere (also at the NPCs) and stays readable in chat.
 */
public final class BondPaneel {
    private BondPaneel() {
    }

    // ---- building blocks ---------------------------------------------------------------------------------------------------------

    static MutableComponent knop(Component label, String commando, ChatFormatting kleur, @Nullable Component hover) {
        MutableComponent c = Component.literal("[").append(label).append("]").withStyle(kleur);
        return c.withStyle(s -> {
            s = s.withClickEvent(new ClickEvent.RunCommand(commando));
            return hover == null ? s : s.withHoverEvent(new HoverEvent.ShowText(hover));
        });
    }

    static MutableComponent knop(String key, String commando, ChatFormatting kleur) {
        return knop(Component.translatable(key), commando, kleur, null);
    }

    static MutableComponent typ(String key, String voorstel) {
        return Component.literal("[").append(Component.translatable(key)).append("]").withStyle(ChatFormatting.YELLOW)
                .withStyle(s -> s.withClickEvent(new ClickEvent.SuggestCommand(voorstel))
                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable("vadsrijk.gilden.knop.typ"))));
    }

    static MutableComponent kop(Component titel) {
        return Component.literal("✿ ").withStyle(ChatFormatting.LIGHT_PURPLE).append(titel.copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                .append(Component.literal(" ✿").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    static MutableComponent balk(long heb, long nodig, int breed) {
        int vol = nodig <= 0 ? breed : (int) Math.min(breed, heb * breed / Math.max(1, nodig));
        return Component.literal("|".repeat(vol)).withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal("|".repeat(breed - vol)).withStyle(ChatFormatting.DARK_GRAY));
    }

    static void regel(ServerPlayer p, Component c) {
        p.sendSystemMessage(c);
    }

    static MutableComponent rij(Component... delen) {
        MutableComponent c = Component.empty();
        for (int i = 0; i < delen.length; i++) {
            if (i > 0) {
                c.append(Component.literal(" "));
            }
            c.append(delen[i]);
        }
        return c;
    }

    static MutableComponent naamMetBanier(ServerPlayer p, Bond b) {
        return BondChat.tag(b).append(Component.literal(b.naam).withStyle(st -> st.withBold(true)
                .withHoverEvent(new HoverEvent.ShowItem(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(Banier.item(p.level().registryAccess(), b))))));
    }

    // ---- the overview --------------------------------------------------------------------------------------------------------------

    public static void toon(ServerPlayer p) {
        Bond b = Bonden.van(p);
        regel(p, Component.literal(" "));
        if (b == null) {
            regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.titel_geen")));
            regel(p, Component.translatable("vadsrijk.gilden.paneel.geen").withStyle(ChatFormatting.GRAY));
            Bond u = BondActies.uitnodigingVan(p);
            if (u != null) {
                regel(p, rij(Component.translatable("vadsrijk.gilden.paneel.uitnodiging", u.naam).withStyle(ChatFormatting.AQUA),
                        knop("vadsrijk.gilden.knop.ja", "/bond accepteer", ChatFormatting.GREEN), knop("vadsrijk.gilden.knop.nee", "/bond weiger", ChatFormatting.RED)));
            }
            regel(p, rij(knop("vadsrijk.gilden.knop.bord", "/bond bord", ChatFormatting.AQUA), knop("vadsrijk.gilden.knop.ranglijst", "/bond ranglijst", ChatFormatting.GOLD),
                    knop("vadsrijk.gilden.knop.uitleg", "/bond uitleg", ChatFormatting.GRAY)));
            return;
        }
        BondXp.week(b);
        Bond.Lid l = b.lid(Bonden.sleutel(p));
        int lv = b.level();
        regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.titel")));
        regel(p, rij(naamMetBanier(p, b), Component.translatable("vadsrijk.gilden.paneel.level", lv).withStyle(ChatFormatting.GOLD),
                balk(b.xp - BondXp.DREMPELS[lv - 1], lv >= BondXp.MAX ? 0 : BondXp.DREMPELS[lv] - BondXp.DREMPELS[lv - 1], 20),
                Component.literal(lv >= BondXp.MAX ? "MAX" : (b.xp + "/" + BondXp.DREMPELS[lv])).withStyle(ChatFormatting.GRAY)));
        regel(p, Component.translatable("vadsrijk.gilden.paneel.info", Component.translatable(l == null ? Rang.REKRUUT.naamKey() : l.rang.naamKey()),
                b.leden.size(), Component.translatable(Kasteel.naamKey(b.kasteel)), b.kasteel, Spelers.munten(b.kas)).withStyle(ChatFormatting.WHITE));
        long rust = BondXp.rust(p);
        if (rust > 0) {
            regel(p, Component.translatable("vadsrijk.gilden.paneel.rust", rust).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        regel(p, rij(knop("vadsrijk.gilden.knop.leden", "/bond leden", ChatFormatting.AQUA), knop("vadsrijk.gilden.knop.taken", "/bond taken", ChatFormatting.AQUA),
                knop("vadsrijk.gilden.knop.perks", "/bond perks", ChatFormatting.AQUA), knop("vadsrijk.gilden.knop.kasteel", "/bond kasteel", ChatFormatting.AQUA)));
        regel(p, rij(knop(BondChat.modus(p) ? "vadsrijk.gilden.knop.chat_uit" : "vadsrijk.gilden.knop.chat_aan", "/bond chat", ChatFormatting.LIGHT_PURPLE),
                knop("vadsrijk.gilden.knop.thuis", "/bond thuis", ChatFormatting.LIGHT_PURPLE), knop("vadsrijk.gilden.knop.ranglijst", "/bond ranglijst", ChatFormatting.GOLD),
                l != null && l.rang.mag(Recht.WERVEN) ? knop("vadsrijk.gilden.knop.werving", "/bond werving", ChatFormatting.YELLOW) : Component.empty(),
                l != null && l.rang.mag(Recht.BANIER) ? knop("vadsrijk.gilden.knop.banier", "/bond banier", ChatFormatting.YELLOW) : Component.empty(),
                knop("vadsrijk.gilden.knop.verlaat", "/bond verlaat", ChatFormatting.DARK_RED)));
        if (l != null && l.rang.mag(Recht.WERVEN) && !b.aanvragen.isEmpty()) {
            regel(p, Component.translatable("vadsrijk.gilden.paneel.aanvragen", b.aanvragen.size()).withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" ")).append(knop("vadsrijk.gilden.knop.bekijk", "/bond leden", ChatFormatting.YELLOW)));
        }
    }

    // ---- members ----------------------------------------------------------------------------------------------------------------

    public static void leden(ServerPlayer p) {
        Bond b = Bonden.van(p);
        if (b == null) {
            toon(p);
            return;
        }
        MinecraftServer s = p.level().getServer();
        Bond.Lid ik = b.lid(Bonden.sleutel(p));
        regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.leden", b.leden.size(), BondActies.MAX_LEDEN)));
        java.util.Set<String> online = new java.util.HashSet<>();
        for (ServerPlayer o : BondChat.online(s, b)) {
            online.add(Bonden.sleutel(o));
        }
        List<Map.Entry<String, Bond.Lid>> lijst = new ArrayList<>(b.leden.entrySet());
        lijst.sort((x, y) -> x.getValue().rang != y.getValue().rang ? x.getValue().rang.compareTo(y.getValue().rang) : x.getValue().naam.compareToIgnoreCase(y.getValue().naam));
        for (Map.Entry<String, Bond.Lid> e : lijst) {
            Bond.Lid l = e.getValue();
            MutableComponent r = Component.literal(online.contains(e.getKey()) ? "● " : "○ ").withStyle(online.contains(e.getKey()) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
                    .append(Component.literal(l.naam + (l.karakter.isEmpty() || l.karakter.equals(l.naam) ? "" : " (" + l.karakter + ")")).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)).append(Component.translatable(l.rang.naamKey()).withStyle(l.rang.kleur))
                    .append(Component.literal("  " + l.bijdrage + " xp").withStyle(ChatFormatting.GRAY));
            if (ik != null && ik.rang.boven(l.rang)) {
                String naam = l.naam;
                if (ik.rang.mag(Recht.BEVORDEREN)) {
                    Rang h = l.rang.hoger();
                    if (h != null && ik.rang.boven(h)) {
                        r.append(" ").append(knop(Component.literal("▲"), "/bond bevorder " + naam, ChatFormatting.GREEN, Component.translatable("vadsrijk.gilden.knop.bevorder")));
                    }
                    if (l.rang.lager() != null) {
                        r.append(" ").append(knop(Component.literal("▼"), "/bond degradeer " + naam, ChatFormatting.GOLD, Component.translatable("vadsrijk.gilden.knop.degradeer")));
                    }
                }
                if (ik.rang.mag(Recht.VERWIJDEREN)) {
                    r.append(" ").append(knop(Component.literal("✖"), "/bond verwijder " + naam, ChatFormatting.RED, Component.translatable("vadsrijk.gilden.knop.verwijder")));
                }
                if (ik.rang.mag(Recht.OPHEFFEN)) {
                    r.append(" ").append(knop(Component.literal("♛"), "/bond leider " + naam, ChatFormatting.GOLD, Component.translatable("vadsrijk.gilden.knop.leider")));
                }
            }
            regel(p, r);
        }
        if (ik != null && ik.rang.mag(Recht.WERVEN)) {
            for (Map.Entry<String, String> a : b.aanvragen.entrySet()) {
                regel(p, rij(Component.translatable("vadsrijk.gilden.paneel.aanvraag", a.getValue()).withStyle(ChatFormatting.YELLOW),
                        knop("vadsrijk.gilden.knop.ja", "/bond keur " + a.getKey() + " ja", ChatFormatting.GREEN),
                        knop("vadsrijk.gilden.knop.nee", "/bond keur " + a.getKey() + " nee", ChatFormatting.RED)));
            }
        }
        regel(p, rij(ik != null && ik.rang.mag(Recht.UITNODIGEN) ? typ("vadsrijk.gilden.knop.uitnodigen", "/bond uitnodig ") : Component.empty(),
                knop("vadsrijk.gilden.knop.terug", "/bond", ChatFormatting.GRAY)));
    }

    // ---- week tasks + perks --------------------------------------------------------------------------------------------------------

    public static void taken(ServerPlayer p) {
        Bond b = Bonden.van(p);
        if (b == null) {
            toon(p);
            return;
        }
        BondXp.week(b);
        regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.taken")));
        for (int i = 0; i < b.taken.size(); i++) {
            BondXp.Taak t = BondXp.taak(b.taken.get(i));
            long doel = t == null ? 1 : t.doel(b.leden.size());
            regel(p, rij(Component.literal(b.taakKlaar[i] ? "✔" : "•").withStyle(b.taakKlaar[i] ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE),
                    Component.translatable("vadsrijk.gilden.taak." + b.taken.get(i) + ".lang", doel).withStyle(ChatFormatting.WHITE),
                    balk(b.voortgang[i], doel, 12), Component.literal(b.voortgang[i] + "/" + doel).withStyle(ChatFormatting.GRAY)));
        }
        regel(p, Component.translatable("vadsrijk.gilden.paneel.taken_uitleg", BondXp.TAAK_XP, Spelers.munten(BondXp.TAAK_KAS), BondXp.WEEK_BONUS)
                .withStyle(ChatFormatting.GRAY));
        regel(p, knop("vadsrijk.gilden.knop.terug", "/bond", ChatFormatting.GRAY));
    }

    public static void perks(ServerPlayer p) {
        Bond b = Bonden.van(p);
        int lv = b == null ? 0 : b.level();
        regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.perks")));
        for (BondXp.Perk k : BondXp.perks()) {
            boolean heb = lv >= k.level();
            regel(p, rij(Component.literal(heb ? "✔" : "lv " + k.level()).withStyle(heb ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY),
                    Component.translatable(k.key(), k.args()).withStyle(heb ? ChatFormatting.WHITE : ChatFormatting.GRAY)));
        }
        regel(p, Component.translatable("vadsrijk.gilden.paneel.perks_uitleg").withStyle(ChatFormatting.GRAY));
        regel(p, knop("vadsrijk.gilden.knop.terug", "/bond", ChatFormatting.GRAY));
    }

    // ---- the castle -----------------------------------------------------------------------------------------------------------------

    public static void kasteel(ServerPlayer p) {
        Bond b = Bonden.van(p);
        if (b == null) {
            toon(p);
            return;
        }
        Bond.Lid l = b.lid(Bonden.sleutel(p));
        regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.kasteel", Component.translatable(Kasteel.naamKey(b.kasteel)), b.kasteel)));
        regel(p, Component.translatable("vadsrijk.gilden.kasteel.uitleg." + Math.max(1, b.kasteel)).withStyle(ChatFormatting.GRAY));
        Kasteel.Kosten k = Kasteel.volgende(b);
        if (k == null) {
            regel(p, Component.translatable("vadsrijk.gilden.kasteel.max").withStyle(ChatFormatting.GOLD));
        } else {
            regel(p, Component.translatable("vadsrijk.gilden.paneel.volgende", Component.translatable(Kasteel.naamKey(b.kasteel + 1)), b.kasteel + 1)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            regel(p, rij(Component.literal(b.level() >= k.bondLevel() ? "✔" : "•").withStyle(b.level() >= k.bondLevel() ? ChatFormatting.GREEN : ChatFormatting.RED),
                    Component.translatable("vadsrijk.gilden.kasteel.eis.level", k.bondLevel())));
            regel(p, rij(Component.literal(b.kas >= k.kas() ? "✔" : "•").withStyle(b.kas >= k.kas() ? ChatFormatting.GREEN : ChatFormatting.RED),
                    Component.translatable("vadsrijk.gilden.kasteel.eis.kas", Spelers.munten(b.kas), Spelers.munten(k.kas()))));
            for (Map.Entry<String, Integer> e : k.materialen().entrySet()) {
                int heb = b.depot.getOrDefault(e.getKey(), 0);
                regel(p, rij(Component.literal(heb >= e.getValue() ? "✔" : "•").withStyle(heb >= e.getValue() ? ChatFormatting.GREEN : ChatFormatting.RED),
                        Component.translatable("item.guhs.vr_amb_" + e.getKey()), balk(heb, e.getValue(), 10),
                        Component.literal(heb + "/" + e.getValue()).withStyle(ChatFormatting.GRAY)));
            }
            regel(p, rij(knop("vadsrijk.gilden.knop.doneer", "/bond kasteel doneer", ChatFormatting.AQUA),
                    knop(Component.literal("+1z"), "/bond kas 100", ChatFormatting.GOLD, Component.translatable("vadsrijk.gilden.knop.kas", Spelers.munten(100))),
                    knop(Component.literal("+10z"), "/bond kas 1000", ChatFormatting.GOLD, Component.translatable("vadsrijk.gilden.knop.kas", Spelers.munten(1000))),
                    knop(Component.literal("+1g"), "/bond kas 10000", ChatFormatting.GOLD, Component.translatable("vadsrijk.gilden.knop.kas", Spelers.munten(10000))),
                    l != null && l.rang.mag(Recht.KASTEEL) ? knop("vadsrijk.gilden.knop.bouw", "/bond kasteel bouw", ChatFormatting.GREEN) : Component.empty()));
        }
        regel(p, rij(knop("vadsrijk.gilden.knop.thuis", "/bond thuis", ChatFormatting.LIGHT_PURPLE), knop("vadsrijk.gilden.knop.terug", "/bond", ChatFormatting.GRAY)));
    }

    // ---- recruiting + the board ---------------------------------------------------------------------------------------------------------

    public static void werving(ServerPlayer p) {
        Bond b = Bonden.van(p);
        if (b == null) {
            toon(p);
            return;
        }
        regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.werving")));
        regel(p, rij(Component.translatable("vadsrijk.gilden.werving.werft").withStyle(ChatFormatting.WHITE),
                knop(b.werft ? "vadsrijk.gilden.knop.aan" : "vadsrijk.gilden.knop.uit", "/bond werving " + (b.werft ? "uit" : "aan"), b.werft ? ChatFormatting.GREEN : ChatFormatting.RED)));
        regel(p, rij(Component.translatable("vadsrijk.gilden.werving.vrij").withStyle(ChatFormatting.WHITE),
                knop(b.vrij ? "vadsrijk.gilden.knop.vrij" : "vadsrijk.gilden.knop.aanvraag", "/bond werving " + (b.vrij ? "dicht" : "open"), ChatFormatting.AQUA)));
        regel(p, rij(Component.translatable("vadsrijk.gilden.werving.min", b.minLevel).withStyle(ChatFormatting.WHITE),
                knop(Component.literal("-5"), "/bond werving level " + Math.max(1, b.minLevel - 5), ChatFormatting.GOLD, null),
                knop(Component.literal("+5"), "/bond werving level " + Math.min(60, b.minLevel + 5), ChatFormatting.GOLD, null)));
        regel(p, rij(Component.translatable("vadsrijk.gilden.werving.tekst", b.werfTekst.isEmpty() ? "-" : b.werfTekst).withStyle(ChatFormatting.WHITE),
                typ("vadsrijk.gilden.knop.wijzig", "/bond werving tekst ")));
        regel(p, knop("vadsrijk.gilden.knop.terug", "/bond", ChatFormatting.GRAY));
    }

    /** The Bondenbord: the bonds that look for members (from zone 1 on). */
    public static void bord(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        regel(p, Component.literal(" "));
        regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.bord")));
        int lv = Spelers.data(p).level();
        int n = 0;
        for (Bond b : Bonden.get(s).ranglijst()) {
            if (!b.werft || b.leden.size() >= BondActies.MAX_LEDEN) {
                continue;
            }
            n++;
            MutableComponent r = rij(naamMetBanier(p, b), Component.translatable("vadsrijk.gilden.bord.regel", b.level(), b.leden.size(), b.minLevel)
                    .withStyle(ChatFormatting.GRAY));
            if (!b.werfTekst.isEmpty()) {
                r.append(Component.literal(" \"" + b.werfTekst + "\"").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE));
            }
            if (Bonden.van(p) == null && lv >= b.minLevel) {
                r.append(" ").append(knop(b.vrij ? "vadsrijk.gilden.knop.word_lid" : "vadsrijk.gilden.knop.aanmelden", "/bond aanmelden " + b.id, ChatFormatting.GREEN));
            }
            regel(p, r);
            if (n >= 12) {
                break;
            }
        }
        if (n == 0) {
            regel(p, Component.translatable("vadsrijk.gilden.bord.leeg").withStyle(ChatFormatting.GRAY));
        }
        regel(p, Component.translatable("vadsrijk.gilden.bord.zelf").withStyle(ChatFormatting.GRAY));
    }

    public static void ranglijst(ServerPlayer p) {
        regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.ranglijst")));
        int i = 0;
        for (Bond b : Bonden.get(p.level().getServer()).ranglijst()) {
            if (++i > 10) {
                break;
            }
            regel(p, rij(Component.literal(i + ".").withStyle(i <= 3 ? ChatFormatting.GOLD : ChatFormatting.GRAY), naamMetBanier(p, b),
                    Component.translatable("vadsrijk.gilden.rang.regel", b.level(), b.xp, b.leden.size(), Component.translatable(Kasteel.naamKey(b.kasteel)))
                            .withStyle(ChatFormatting.GRAY)));
        }
        if (i == 0) {
            regel(p, Component.translatable("vadsrijk.gilden.bord.leeg").withStyle(ChatFormatting.GRAY));
        }
    }

    // ---- the banner designer ----------------------------------------------------------------------------------------------------------

    public static void banier(ServerPlayer p) {
        Bond b = Bonden.van(p);
        if (b == null) {
            toon(p);
            return;
        }
        regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.banier")));
        regel(p, rij(naamMetBanier(p, b), Component.translatable("vadsrijk.gilden.banier.hover").withStyle(ChatFormatting.GRAY)));
        regel(p, rij(Component.translatable("vadsrijk.gilden.banier.basis").withStyle(ChatFormatting.WHITE),
                knop(Component.literal("◀"), "/bond banier basis " + kleurNaast(b.basis, -1).getName(), ChatFormatting.AQUA, null),
                Component.translatable("color.minecraft." + b.basis.getName()).withStyle(st -> st.withColor(Banier.rgb(b.basis))),
                knop(Component.literal("▶"), "/bond banier basis " + kleurNaast(b.basis, 1).getName(), ChatFormatting.AQUA, null)));
        for (int i = 0; i < Banier.MAX_LAGEN; i++) {
            if (i < b.lagen.size()) {
                Bond.Laag l = b.lagen.get(i);
                int pi = Banier.index(l.patroon());
                String vorig = Banier.PATRONEN.get(Math.floorMod(pi - 1, Banier.PATRONEN.size()))[0], volg = Banier.PATRONEN.get((pi + 1) % Banier.PATRONEN.size())[0];
                regel(p, rij(Component.translatable("vadsrijk.gilden.banier.laag", i + 1).withStyle(ChatFormatting.WHITE),
                        knop(Component.literal("◀"), "/bond banier laag " + (i + 1) + " " + vorig + " " + l.kleur().getName(), ChatFormatting.AQUA, null),
                        Component.translatable("vadsrijk.gilden.patroon." + l.patroon()),
                        knop(Component.literal("▶"), "/bond banier laag " + (i + 1) + " " + volg + " " + l.kleur().getName(), ChatFormatting.AQUA, null),
                        knop(Component.literal("◀"), "/bond banier laag " + (i + 1) + " " + l.patroon() + " " + kleurNaast(l.kleur(), -1).getName(), ChatFormatting.GOLD, null),
                        Component.translatable("color.minecraft." + l.kleur().getName()).withStyle(st -> st.withColor(Banier.rgb(l.kleur()))),
                        knop(Component.literal("▶"), "/bond banier laag " + (i + 1) + " " + l.patroon() + " " + kleurNaast(l.kleur(), 1).getName(), ChatFormatting.GOLD, null),
                        knop(Component.literal("✖"), "/bond banier weg " + (i + 1), ChatFormatting.RED, Component.translatable("vadsrijk.gilden.knop.weg"))));
            } else {
                regel(p, knop("vadsrijk.gilden.knop.laag_erbij", "/bond banier laag " + (i + 1) + " rand white", ChatFormatting.GREEN));
                break;
            }
        }
        regel(p, knop("vadsrijk.gilden.knop.terug", "/bond", ChatFormatting.GRAY));
    }

    static DyeColor kleurNaast(DyeColor c, int stap) {
        DyeColor[] v = DyeColor.values();
        return v[Math.floorMod(c.ordinal() + stap, v.length)];
    }

    // ---- notices --------------------------------------------------------------------------------------------------------------------

    static void uitnodiging(ServerPlayer naar, Bond b, String van) {
        regel(naar, rij(Component.translatable("vadsrijk.gilden.uitnodiging", van).withStyle(ChatFormatting.AQUA), naamMetBanier(naar, b),
                knop("vadsrijk.gilden.knop.ja", "/bond accepteer", ChatFormatting.GREEN), knop("vadsrijk.gilden.knop.nee", "/bond weiger", ChatFormatting.RED)));
    }

    static void aanvraag(ServerPlayer o, Bond b, String sleutel, String wie) {
        regel(o, rij(BondChat.tag(b), Component.translatable("vadsrijk.gilden.paneel.aanvraag", wie).withStyle(ChatFormatting.YELLOW),
                knop("vadsrijk.gilden.knop.ja", "/bond keur " + sleutel + " ja", ChatFormatting.GREEN),
                knop("vadsrijk.gilden.knop.nee", "/bond keur " + sleutel + " nee", ChatFormatting.RED)));
    }

    // ---- teleporting ------------------------------------------------------------------------------------------------------------------

    /** A hub the teleport stone can send you to: id, POI, unlock (null = always). */
    record Hub(String id, Supplier<BlockPos> plek, @Nullable String reis) {
    }

    static final List<Hub> HUBS = List.of(
            new Hub("knuffelkamp", () -> nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken.plek("kampvuur_kamp"), null),
            new Hub("zwamzoet", () -> nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken.plek("plein"), "zwamzoet"),
            new Hub("kandijkom", () -> nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.plek("plein"), "kandijkom"),
            new Hub("honingtak", () -> nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.plek("markt"), "schemerwortel"),
            new Hub("lampionhaven", () -> nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.plek("plein"), "lampionkust"),
            new Hub("hoogvads", () -> nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken.plek("markt_fontein"), "hoogvads"));

    static boolean hubOpen(ServerPlayer p, Hub h) {
        return h.reis() == null || p.isCreative() || QuestMotor.reisSlot(p, h.reis()) == null;
    }

    /** The teleport stone in the castle: where to? */
    static void steen(ServerPlayer p, Bond b) {
        BondXp.tag(p).putLong("steen", BondXp.KLOK.getAsLong());
        regel(p, Component.literal(" "));
        regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.steen")));
        long wacht = BondTeleport.wacht(p);
        if (wacht > 0) {
            regel(p, Component.translatable("vadsrijk.gilden.tp.wacht", BondTeleport.tijd(wacht)).withStyle(ChatFormatting.GRAY));
        }
        List<Component> knoppen = new ArrayList<>();
        CompoundTag t = BondXp.tag(p);
        if (t.contains("terug_x")) {
            knoppen.add(knop("vadsrijk.gilden.knop.terug_waar", "/bond reis terug", ChatFormatting.GREEN));
        }
        knoppen.add(knop("vadsrijk.gilden.knop.plein", "/bond reis plein", ChatFormatting.AQUA));
        for (Hub h : HUBS) {
            if (hubOpen(p, h)) {
                knoppen.add(knop(Component.translatable("vadsrijk.gilden.hub." + h.id()), "/bond reis " + h.id(), ChatFormatting.LIGHT_PURPLE,
                        Component.translatable("vadsrijk.gilden.knop.hub_hover")));
            }
        }
        regel(p, rij(knoppen.toArray(new Component[0])));
    }

    /** Short explanation (the Bondsklerk tells the same). */
    static void uitleg(ServerPlayer p) {
        regel(p, kop(Component.translatable("vadsrijk.gilden.paneel.uitleg")));
        for (int i = 1; i <= 4; i++) {
            regel(p, Component.translatable("vadsrijk.gilden.uitleg." + i, Spelers.munten(BondActies.PRIJS), BondActies.MIN_LEVEL).withStyle(ChatFormatting.WHITE));
        }
    }

    /** Is this player in a town (a hub area)? */
    static boolean inStad(ServerPlayer p) {
        Gebieden.Gebied g = Gebieden.op(p.blockPosition());
        return g != null && g.stad();
    }

    static Predicate<ServerPlayer> ALTIJD = p -> true;
}
