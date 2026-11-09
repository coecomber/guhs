package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
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
 *       name text (x-dungeon had not committed when this was written: a plain billboard TextDisplay, gold title + white lines).</li>
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

    /** The texts: spot + lines (key, style). */
    private record Bord(String id, Vec3 plek, List<Component> regels) {
    }

    private static List<Bord> borden() {
        BlockPos d = Z6RaidPlekken.poort();
        Vec3 c = Z6RaidPlekken.start();
        BlockPos u = Z6RaidPlekken.uitgang();
        String k = "vadsrijk.z6raid.bord.";
        return List.of(
                new Bord("poort", new Vec3(d.getX() + 2.5, d.getY() + 6.3, d.getZ() + 0.5), List.of(
                        Component.translatable(k + "poort.titel").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                        Component.translatable(k + "poort.1").withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.translatable(k + "poort.2").withStyle(ChatFormatting.WHITE))),
                new Bord("cirkel", c.add(0, 3.0, 0), List.of(
                        Component.translatable(k + "cirkel.titel").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                        Component.translatable(k + "cirkel.1").withStyle(ChatFormatting.WHITE))),
                new Bord("uitgang", Vec3.atBottomCenterOf(u).add(0, 3.0, 0), List.of(
                        Component.translatable(k + "uitgang.titel").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))));
    }

    /** Every 5 s (from Z6Raid.tick): exactly one text per spot while a player is near and the chunk is loaded. */
    static void tick(ServerLevel sl, long nu) {
        if (nu % 100 != 0) {
            return;
        }
        for (Bord b : borden()) {
            houd(sl, b);
        }
    }

    static int houd(ServerLevel sl, Bord b) {
        BlockPos bp = BlockPos.containing(b.plek());
        if (!sl.hasChunkAt(bp) || sl.players().stream().noneMatch(p -> p.position().distanceToSqr(b.plek()) < 48 * 48)) {
            return 0;
        }
        String tag = TAG + "_" + b.id();
        List<Display.TextDisplay> nu = sl.getEntitiesOfClass(Display.TextDisplay.class, new AABB(bp).inflate(2), e -> e.entityTags().contains(tag));
        for (int i = 1; i < nu.size(); i++) {
            nu.get(i).discard();
        }
        if (!nu.isEmpty()) {
            return 1;
        }
        Display.TextDisplay t = EntityType.TEXT_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
        if (t == null) {
            return 0;
        }
        net.minecraft.network.chat.MutableComponent tekst = Component.empty();
        for (int i = 0; i < b.regels().size(); i++) {
            if (i > 0) {
                tekst.append("\n");
            }
            tekst.append(b.regels().get(i));
        }
        CompoundTag tag2 = nl.juiced.guhs.storage.Nbt.saveWithoutId(t);
        tag2.put("text", ComponentSerialization.CODEC.encodeStart(sl.registryAccess().createSerializationContext(NbtOps.INSTANCE), tekst).getOrThrow());
        tag2.putInt("background", 0x60000000);
        tag2.putString("billboard", "center");
        tag2.putInt("line_width", 220);
        nl.juiced.guhs.storage.Nbt.load(t, tag2);
        t.snapTo(b.plek().x, b.plek().y, b.plek().z, 0, 0);
        t.addTag(tag);
        sl.addFreshEntity(t);
        return 1;
    }

    /** Gametest: keep the texts in this level now (ignores the 5 s rhythm); returns how many spots have their text. */
    static int houdAlles(ServerLevel sl) {
        int n = 0;
        for (Bord b : borden()) {
            n += houd(sl, b);
        }
        return n;
    }

    /** Gametest: the texts at their spots (for counting). */
    static int telBorden(ServerLevel sl) {
        int n = 0;
        for (Bord b : borden()) {
            n += sl.getEntitiesOfClass(Display.TextDisplay.class, new AABB(BlockPos.containing(b.plek())).inflate(2),
                    e -> e.entityTags().contains(TAG + "_" + b.id())).size();
        }
        return n;
    }
}
