package nl.juiced.guhs.feature.vadsrijk.party.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.menu.client.MenuPaneel;
import nl.juiced.guhs.feature.vadsrijk.party.PartyPayloads;

/**
 * W-PARTY: the Party tab of the Vadsrijk-menu. Left: your party (leader crown, online/offline, level, class + spec, zone; nep-spelers
 * with [NPC] and their minutes left), the leader's buttons (Leider / Weg), an open invitation with Accepteer/Weiger, and Verlaten.
 * Right: the players in the Vadsrijk (nearby first) with Uitnodigen, and "Samen in de wachtrij" (the leader queues the whole party
 * for a dungeon or the raid). Every button sends {@link PartyPayloads.Actie}; the server answers with fresh data.
 */
public class PartyPaneel extends MenuPaneel {
    private static final String K = "vadsrijk.party.tab.";
    private static final int PAARS = 0xFFB050C8, GRIJS = 0xFF9A8A98;
    private int pagina, dungeonKeus;

    public PartyPaneel(CompoundTag data) {
        super("party", Component.translatable("vadsrijk.menu.tab.party"), data);
    }

    static void doe(String actie, String arg) {
        ClientPacketDistributor.sendToServer(new PartyPayloads.Actie(actie, arg));
    }

    private static List<CompoundTag> lijst(CompoundTag t, String k) {
        List<CompoundTag> uit = new ArrayList<>();
        for (Tag x : t.getListOrEmpty(k)) {
            if (x instanceof CompoundTag c) {
                uit.add(c);
            }
        }
        return uit;
    }

    private static Component tekst(CompoundTag t, String k) {
        Tag x = t.get(k);
        return x == null ? Component.empty() : ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, x).result().orElse(Component.empty());
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int links = left + 10, kolom = (w - 30) * 11 / 20, rechts = links + kolom + 10, rbreed = left + w - 10 - rechts;
        tekenParty(g, mx, my, links, top + 24, kolom);
        tekenSpelers(g, mx, my, rechts, top + 24, rbreed);
    }

    // ---- left: my party -----------------------------------------------------------------------------------------------------

    private void tekenParty(GuiGraphicsExtractor g, int mx, int my, int x, int y, int breed) {
        List<CompoundTag> leden = lijst(data, "leden");
        boolean ikLeider = data.getBooleanOr("leider", true), inParty = data.getBooleanOr("inParty", false);
        kop(g, Component.translatable(K + "party", leden.size(), data.getIntOr("max", 5)), x, y);
        y += 13;
        if (leden.isEmpty()) {
            for (var regel : font.split(Component.translatable(K + "geen_party"), breed)) {
                g.text(font, regel, x, y, ZACHT, false);
                y += 10;
            }
        }
        int rij = 0;
        for (CompoundTag l : leden) {
            int ry = y + rij * 23;
            g.fill(x, ry, x + breed, ry + 22, rij % 2 == 0 ? RIJ_A : RIJ_B);
            boolean npc = l.getBooleanOr("npc", false), online = l.getBooleanOr("online", false), leider = l.getBooleanOr("leider", false);
            int stip = npc ? PAARS : online ? GROEN : GRIJS;
            g.fill(x + 3, ry + 4, x + 7, ry + 8, stip);
            String naam = l.getStringOr("naam", "?");
            Component regel1 = Component.literal(leider ? "♛ " : "").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(naam).withStyle(l.getBooleanOr("ik", false) ? ChatFormatting.BOLD : ChatFormatting.RESET))
                    .append(npc ? Component.literal(" [NPC]").withStyle(ChatFormatting.DARK_PURPLE) : Component.empty());
            g.text(font, regel1, x + 10, ry + 2, online || npc ? TEKST : GRIJS, false);
            Component regel2;
            if (!online && !npc) {
                regel2 = Component.translatable(K + "offline");
            } else {
                String zone = l.getStringOr("zone", "");
                regel2 = Component.translatable(K + "lid", l.getIntOr("lv", 1), Component.translatable(l.getStringOr("klasse", "")),
                        Component.translatable(l.getStringOr("spec", "")));
                if (npc && l.contains("min")) {
                    regel2 = regel2.copy().append(Component.translatable(K + "minuten", l.getIntOr("min", 0)));
                } else if (npc && l.getBooleanOr("dungeon", false)) {
                    regel2 = regel2.copy().append(Component.translatable(K + "vuller"));
                } else if (!zone.isEmpty()) {
                    regel2 = regel2.copy().append(" · ").append(Component.translatable(zone));
                }
            }
            int knopRuimte = 0;
            String id = l.getStringOr("id", "");
            boolean ik = l.getBooleanOr("ik", false);
            if (inParty && ikLeider && !ik && !l.getBooleanOr("dungeon", false)) {
                knopRuimte = npc ? 34 : 76;
                knop(g, mx, my, x + breed - 32, ry + 5, 30, 12, Component.translatable(K + "knop.weg"), true, () -> doe("verwijder", id));
                if (!npc) {
                    knop(g, mx, my, x + breed - 74, ry + 5, 40, 12, Component.translatable(K + "knop.leider"), online, () -> doe("leider", id));
                }
            } else if (npc && l.getBooleanOr("eigen", false) && !l.getBooleanOr("dungeon", false)) {
                knopRuimte = 34;
                knop(g, mx, my, x + breed - 32, ry + 5, 30, 12, Component.translatable(K + "knop.weg"), true, () -> doe("nepweg", id));
            }
            g.text(font, font.plainSubstrByWidth(regel2.getString(), breed - 14 - knopRuimte), x + 10, ry + 12, ZACHT, false);
            rij++;
        }
        // bottom: an invitation and "leave"
        int by = top + h - 22;
        if (inParty) {
            knop(g, mx, my, x, by, 96, 16, Component.translatable(K + "knop.verlaat"), true, () -> doe("verlaat", ""));
        }
        String van = data.getStringOr("uitnodiging", "");
        if (!van.isEmpty()) {
            int iy = by - 34;
            g.fill(x, iy - 2, x + breed, iy + 30, 0xFFFFF4C8);
            g.text(font, Component.translatable("vadsrijk.party.uitnodiging.kort", van), x + 4, iy + 1, GOUD, false);
            knop(g, mx, my, x + 4, iy + 12, 70, 14, Component.translatable("vadsrijk.party.knop.accepteer"), true, () -> doe("accepteer", ""));
            knop(g, mx, my, x + 78, iy + 12, 70, 14, Component.translatable("vadsrijk.party.knop.weiger"), true, () -> doe("weiger", ""));
        }
        Component nep = !data.getBooleanOr("nepServer", true) ? Component.translatable(K + "nep.server_uit")
                : !data.getBooleanOr("nepZelf", true) ? Component.translatable(K + "nep.zelf_uit") : null;
        if (nep != null) {
            g.text(font, font.plainSubstrByWidth(nep.getString(), breed - 100), x + 100, by + 4, GRIJS, false);
        }
    }

    // ---- right: players + group queue ---------------------------------------------------------------------------------------

    private void tekenSpelers(GuiGraphicsExtractor g, int mx, int my, int x, int y, int breed) {
        List<CompoundTag> spelers = lijst(data, "spelers");
        kop(g, Component.translatable(K + "spelers"), x, y);
        y += 13;
        int perPagina = Math.max(3, (h - 24 - 13 - 92) / 15);
        int paginas = Math.max(1, (spelers.size() + perPagina - 1) / perPagina);
        pagina = Math.min(pagina, paginas - 1);
        if (spelers.isEmpty()) {
            g.text(font, Component.translatable(K + "niemand"), x, y, ZACHT, false);
        }
        boolean mag = data.getBooleanOr("leider", true) || !data.getBooleanOr("inParty", false);
        for (int i = pagina * perPagina; i < Math.min(spelers.size(), (pagina + 1) * perPagina); i++) {
            CompoundTag s = spelers.get(i);
            int ry = y + (i - pagina * perPagina) * 15;
            g.fill(x, ry, x + breed, ry + 14, i % 2 == 0 ? RIJ_A : RIJ_B);
            int af = s.getIntOr("afstand", -1);
            String naam = s.getStringOr("naam", "?") + " · Lv " + s.getIntOr("lv", 1);
            String waar = af >= 0 ? af + "m" : "";
            String zone = s.getStringOr("zone", "");
            List<Component> tipRegels = new ArrayList<>();
            tipRegels.add(Component.literal(s.getStringOr("naam", "?")).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (!zone.isEmpty()) {
                tipRegels.add(Component.translatable(zone).withStyle(ChatFormatting.GRAY));
            }
            g.text(font, font.plainSubstrByWidth(naam, breed - 78 - font.width(waar)), x + 3, ry + 3, TEKST, false);
            g.text(font, waar, x + breed - 72 - font.width(waar), ry + 3, ZACHT, false);
            tip(mx, my, x, ry, breed - 70, 14, tipRegels);
            String id = s.getStringOr("id", "");
            if (s.getBooleanOr("samen", false)) {
                g.text(font, Component.translatable(K + "samen"), x + breed - 66, ry + 3, GROEN, false);
            } else if (s.getBooleanOr("uitgenodigd", false)) {
                g.text(font, Component.translatable(K + "uitgenodigd"), x + breed - 66, ry + 3, GOUD, false);
            } else {
                knop(g, mx, my, x + breed - 68, ry + 1, 66, 12, Component.translatable(K + "knop.uitnodigen"), mag && !s.getBooleanOr("party", false),
                        () -> doe("uitnodigen", id));
            }
        }
        int py = y + perPagina * 15 + 2;
        if (paginas > 1) {
            knop(g, mx, my, x, py, 14, 12, Component.literal("<"), pagina > 0, () -> pagina--);
            g.centeredText(font, (pagina + 1) + "/" + paginas, x + 34, py + 2, ZACHT);
            knop(g, mx, my, x + 54, py, 14, 12, Component.literal(">"), pagina < paginas - 1, () -> pagina++);
        }
        tekenRij(g, mx, my, x, top + h - 76, breed);
    }

    private void tekenRij(GuiGraphicsExtractor g, int mx, int my, int x, int y, int breed) {
        g.fill(x, y - 3, x + breed, y - 2, 0xFFFFB0D8);
        kop(g, Component.translatable(K + "rij"), x, y);
        y += 12;
        boolean leider = data.getBooleanOr("leider", true);
        List<CompoundTag> dungeons = lijst(data, "dungeons");
        Component status = data.contains("rij") ? Component.translatable(K + "rij.in", tekst(data, "rij")) : null;
        if (status != null) {
            g.text(font, font.plainSubstrByWidth(status.getString(), breed), x, y, GROEN, false);
        }
        y += 11;
        if (!dungeons.isEmpty()) {
            dungeonKeus = Math.floorMod(dungeonKeus, dungeons.size());
            CompoundTag d = dungeons.get(dungeonKeus);
            knop(g, mx, my, x, y, 14, 14, Component.literal("<"), dungeons.size() > 1, () -> dungeonKeus--);
            Component naam = tekst(d, "naam");
            g.centeredText(font, font.plainSubstrByWidth(naam.getString(), breed - 120), x + 18 + (breed - 120) / 2, y + 3, TEKST);
            knop(g, mx, my, x + breed - 100, y, 14, 14, Component.literal(">"), dungeons.size() > 1, () -> dungeonKeus++);
            String id = d.getStringOr("id", "");
            knop(g, mx, my, x + breed - 82, y, 82, 14, Component.translatable(K + "knop.rij"), leider, () -> doe("rij", id));
        } else {
            g.text(font, font.plainSubstrByWidth(Component.translatable(K + "rij.geen").getString(), breed), x, y + 3, GRIJS, false);
        }
        y += 18;
        knop(g, mx, my, x, y, breed, 14, Component.translatable(K + "knop.raid"), leider, () -> doe("raid", ""));
        if (!leider) {
            tip(mx, my, x, y - 18, breed, 32, List.of(Component.translatable(K + "rij.leider").withStyle(ChatFormatting.GRAY)));
        }
    }
}
