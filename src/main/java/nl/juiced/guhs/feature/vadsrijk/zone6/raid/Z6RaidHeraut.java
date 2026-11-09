package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPayloads;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPlekken;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcVolg;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.zone6.verhaal.Z6VerhaalApi;
import nl.juiced.guhs.feature.vadsrijk.zone6.verhaal.Z6VerhaalPlekken;

/**
 * X-RAID (round X, the user: "is de grote toren de raid? ik loop er binnen en er gebeurt niks"): the raid explains itself.
 * <ul>
 *   <li><b>Two heralds</b> (role {@link #ROL}): Heraut Sterrenroep on the stair top in front of the tower door and Zaalwachter
 *       Pril in De Wachtzaal next to the gold start circle. A click explains the raid (2-10 players, walk through the door, 3 s on
 *       the circle together, the party tab) and says whether the tower is free.</li>
 *   <li><b>Name texts</b> (TextDisplays, kept by {@link #tick}: one per spot, extras removed, respawned when gone): the raid's name
 *       over the door niche, "Gouden startcirkel" over the circle, "Uitgang" over the exit arch. Same style as a dungeon door's
 *       name text: X-INT made it {@link DungeonBord}'s look (name big + bold in a colour, a small white line under it).</li>
 *   <li><b>The tracker</b> ({@link NpcVolg#EXTRA}): after the zone-6 story (Z6VerhaalApi.torenOpen) and until your first clear
 *       (Ringeling cheered up), while you are in zone 6, a line "De Sterrendovende Toren (raid)" with the beam on the door; in
 *       De Wachtzaal it points at the start circle.</li>
 * </ul>
 * Never solo: the start circle still needs 2 real players (made-up dev members only exist through the dev command).
 */
public final class Z6RaidHeraut {
    public static final String ROL = "z6raid_heraut";
    public static final String BUITEN = "z6raid_heraut", ZAAL = "z6raid_zaalwachter";
    static final String TAG = "guhs_z6raid_bord";
    private static boolean klaar;

    private Z6RaidHeraut() {
    }

    /** Where the herald outside stands: the stair top in front of the door, at the south edge (the path stays 4 wide). */
    public static BlockPos herautBuiten() {
        return Z6RaidPlekken.poort().offset(-4, 0, 2);
    }

    /** Where the hall keeper stands: between the arrival spot and the start circle in De Wachtzaal. */
    public static BlockPos zaalwachter() {
        return Z6RaidPlekken.midden(0).offset(1, 0, 4);
    }

    public static synchronized void init() {
        if (klaar) {
            return;
        }
        klaar = true;
        BlockPos b = herautBuiten(), z = zaalwachter();
        NpcPlekken.NPC.put(BUITEN, b);
        NpcPlekken.NPC.put(ZAAL, z);
        Npcs.voegToe(new NpcDef(BUITEN, b, 90, GuhVariant.GOLDEN, List.of(GuhClothes.KNIGHT_HELMET, GuhClothes.ROYAL_CAPE), NpcDef.Gedrag.STAAN, ROL, 1.1f));
        Npcs.voegToe(new NpcDef(ZAAL, z, 180, GuhVariant.SNOW, List.of(GuhClothes.STERRENKIJKERSMUTS, GuhClothes.STERRENCAPE), NpcDef.Gedrag.STAAN, ROL, 1f));
        NpcRollen.koppel(ROL, Z6RaidHeraut::leg);
        NpcVolg.EXTRA.add(Z6RaidHeraut::volgRegel);   // X-RAID
    }

    /** The herald's explanation (chat) + whether the tower is free now. */
    public static void leg(ServerPlayer p) {
        for (int i = 1; i <= 4; i++) {
            p.sendSystemMessage(Component.translatable("vadsrijk.z6raid.heraut." + i).withStyle(i == 1 ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE));
        }
        Z6Raid.Groep g = Z6Raid.groep();
        p.sendSystemMessage(g == null ? Component.translatable("vadsrijk.z6raid.wachtzaal.vrij").withStyle(ChatFormatting.GREEN)
                : Component.translatable("vadsrijk.z6raid.wachtzaal.bezet", g.n, Math.min(4, g.verdieping), Z6Raid.RIJ.size()).withStyle(ChatFormatting.GOLD));
    }

    /** The tracker line (null = none): zone 6 done, raid not cleared yet, you are in zone 6. */
    @Nullable
    static NpcPayloads.VolgRegel volgRegel(ServerPlayer p) {
        if (!Vadsrijk.in(p) || !Z6VerhaalApi.torenOpen(p) || Z6VerhaalApi.ringelingVriend(p) || Z6Raid.inGroep(p)) {
            return null;
        }
        boolean zaal = Z6RaidPlekken.verdieping(p.getX(), p.getY(), p.getZ()) == 0;
        if (!zaal && !Z6VerhaalPlekken.zone6(p.getX(), p.getY(), p.getZ())) {
            return null;
        }
        return volgRegel(zaal);
    }

    /** The line itself (also for the gametest): in the Wachtzaal it points at the circle, else at the tower door. */
    static NpcPayloads.VolgRegel volgRegel(boolean zaal) {
        String k = "vadsrijk.z6raid.volg.";
        BlockPos doel = zaal ? BlockPos.containing(Z6RaidPlekken.start()) : Z6RaidPlekken.poort();
        return new NpcPayloads.VolgRegel("z6raid", false, Component.translatable(k + "titel"), Component.translatable(k + (zaal ? "wat.zaal" : "wat.poort")),
                Component.translatable(k + (zaal ? "waar.zaal" : "waar.poort")), Optional.of(Component.translatable(k + "extra")), Optional.of(doel));
    }

    // ---- the name texts ---------------------------------------------------------------------------------------------------------

    /** The texts: spot (the bottom of the small line) + the name + the small line (null: name only). X-INT: DungeonBord style. */
    private record Bord(String id, Vec3 plek, Component naam, Component onder) {
    }

    /** The name colours (like {@link DungeonBord#kleur}: one colour per spot, bold). */
    static final int KLEUR_TOREN = 0xD9B8FF, KLEUR_CIRKEL = 0xFFD86B, KLEUR_UITGANG = 0xC9A8FF;

    private static List<Bord> borden() {
        BlockPos d = Z6RaidPlekken.poort();
        Vec3 c = Z6RaidPlekken.start();
        BlockPos u = Z6RaidPlekken.uitgang();
        String k = "vadsrijk.z6raid.bord.";
        return List.of(
                new Bord("poort", new Vec3(d.getX() + 2.5, d.getY() + 6.3, d.getZ() + 0.5), naam(k + "poort.titel", KLEUR_TOREN),
                        Component.translatable(k + "poort.1").append(" · ").append(Component.translatable(k + "poort.2")).withStyle(ChatFormatting.WHITE)),
                new Bord("cirkel", c.add(0, 3.0, 0), naam(k + "cirkel.titel", KLEUR_CIRKEL),
                        Component.translatable(k + "cirkel.1").withStyle(ChatFormatting.WHITE)),
                new Bord("uitgang", Vec3.atBottomCenterOf(u).add(0, 3.0, 0), naam(k + "uitgang.titel", KLEUR_UITGANG), null));
    }

    private static Component naam(String key, int kleur) {
        return Component.translatable(key).withStyle(s -> s.withBold(true).withColor(kleur));
    }

    /** Every 5 s (from Z6Raid.tick): exactly one text per spot while a player is near and the chunk is loaded. */
    static void tick(ServerLevel sl, long nu) {
        if (nu % 100 != 0) {
            return;
        }
        for (Bord b : borden()) {
            houd(sl, b, true);
        }
    }

    /**
     * X-INT: the same look as a dungeon door's name board ({@link DungeonBord#maak}: two text displays, the name big and bold in a
     * colour over a small white line, full bright, dark background, shadow, vertical billboard), one per line through U-DUBBEL; a
     * copy from disk is refused by {@link DungeonBord#opJoin} (same entity tag), so an old 3-line text never comes back.
     */
    static int houd(ServerLevel sl, Bord b, boolean wacht) {
        BlockPos bp = BlockPos.containing(b.plek());
        if (!sl.hasChunkAt(bp) || (wacht && !Dubbel.klaar(sl, bp))
                || sl.players().stream().noneMatch(p -> p.position().distanceToSqr(b.plek()) < 48 * 48)) {
            return 0;
        }
        // (round-X slice version: one plain 3-line text per spot; gone for good)
        for (Display.TextDisplay oud : sl.getEntitiesOfClass(Display.TextDisplay.class, new AABB(bp).inflate(3),
                e -> e.entityTags().contains(TAG + "_" + b.id()))) {
            oud.discard();
        }
        Component[] regels = b.onder() == null ? new Component[] {b.naam()} : new Component[] {b.naam(), b.onder()};
        for (int i = 0; i < regels.length; i++) {
            // the small line at the spot, the name right on top of it (a name-only text sits at the spot itself)
            Vec3 p = b.onder() != null && i == 0 ? b.plek().add(0, DungeonBord.HOOG_ONDER, 0) : b.plek();
            String sleutel = "z6raidbord:" + b.id() + ":" + i;
            Entity e = Dubbel.zoek(sl, sleutel);
            if (e != null && e.position().distanceToSqr(p) < 0.04) {
                continue;
            }
            if (e != null) {
                e.discard();
            }
            Display.TextDisplay t = DungeonBord.maak(sl, p, regels[i], i == 0 ? DungeonBord.SCHAAL_NAAM : DungeonBord.SCHAAL_ONDER, sleutel);
            if (t != null) {
                t.addTag(TAG);
            }
        }
        return 1;
    }

    /** Gametest: keep the texts in this level now (ignores the 5 s rhythm); returns how many spots have their text. */
    static int houdAlles(ServerLevel sl) {
        int n = 0;
        for (Bord b : borden()) {
            n += houd(sl, b, false);
        }
        return n;
    }

    /** Gametest: the texts at their spots (for counting). */
    static int telBorden(ServerLevel sl) {
        int n = 0;
        for (Bord b : borden()) {
            n += sl.getEntitiesOfClass(Display.TextDisplay.class, new AABB(BlockPos.containing(b.plek())).inflate(2),
                    e -> e.entityTags().contains(TAG) && e.entityTags().contains(DungeonBord.TAG)).isEmpty() ? 0 : 1;
        }
        return n;
    }
}
