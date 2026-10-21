package nl.juiced.guhs.feature.bouwhulp.blauwdruk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.BouwSpelers;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.GereedschapPayloads;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.bouwhulp.kern.Spookbouw;

/**
 * The blueprint browser (design 2.6), the same in the Bouwkeet's Blauwdrukken tab and at the Ontwerpbureau: three sources
 * (eigen, catalogus, bibliotheek), four per page, each with its picture. In the keet: "plaats" (the keet's project) and
 * "publiceer" (from a Sauskeet); at the bureau: "print" (a blueprint to carry around, for a sheet of paper). Everywhere:
 * thumbs up (library) and removing (your own, or as op).
 */
public final class Blauwdrukken {
    public enum Bron { EIGEN, CATALOGUS, BIBLIOTHEEK }

    public static final int PER_PAGINA = 4;
    /** The browser's buttons (actie). */
    public static final int BRON = 1, PAGINA = 2, PLAATS = 3, PRINT = 4, DUIMPJE = 5, VERWIJDER = 6, PUBLICEER = 7;

    private static final Map<UUID, int[]> STAND = new ConcurrentHashMap<>();

    private static int[] stand(UUID u) {
        return STAND.computeIfAbsent(u, k -> new int[] {Bron.CATALOGUS.ordinal(), 0});
    }

    // =====================================================================================================================
    // the list
    // =====================================================================================================================

    /** What the browser shows (keet: the keet's level, for "publiceer"; null at the bureau). */
    public static void schrijf(ServerPlayer p, CompoundTag uit, @Nullable BouwNiveau keet) {
        MinecraftServer server = p.level().getServer();
        int[] st = stand(p.getUUID());
        Bron bron = Bron.values()[st[0]];
        List<CompoundTag> alle = new ArrayList<>();
        boolean op = p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        switch (bron) {
            case EIGEN -> {
                for (Blauwdruk b : BouwSpelers.van(server).eigen(p.getUUID())) {
                    CompoundTag c = regel(b, p.getUUID());
                    c.putBoolean("Weg", true);
                    c.putBoolean("Publiceer", keet != null);
                    c.putBoolean("PubliceerKan", keet != null && keet.publiceren() && b.ontwerp.maat() <= keet.maxBlauwdruk);
                    alle.add(c);
                }
            }
            case CATALOGUS -> {
                for (Catalogus.Ontwerpje o : Catalogus.lijst()) {
                    Blauwdruk b = Catalogus.get(o.id());
                    CompoundTag c = regel(b, p.getUUID());
                    c.putString("Sleutel", "gui.guhs.blauwdruk.cat." + o.id());
                    c.putString("Groep", o.groep());
                    alle.add(c);
                }
                for (int i = 0; i < Catalogus.PLAATSHOUDERS; i++) {
                    CompoundTag c = new CompoundTag();
                    c.putBoolean("Binnenkort", true);
                    alle.add(c);
                }
            }
            case BIBLIOTHEEK -> {
                for (Blauwdruk b : Bibliotheek.van(server).lijst()) {
                    CompoundTag c = regel(b, p.getUUID());
                    c.putBoolean("Weg", op || p.getUUID().equals(b.maker));
                    c.putBoolean("Duim", true);
                    alle.add(c);
                }
            }
        }
        int paginas = Math.max(1, (alle.size() + PER_PAGINA - 1) / PER_PAGINA);
        st[1] = Math.max(0, Math.min(paginas - 1, st[1]));
        ListTag l = new ListTag();
        for (int i = st[1] * PER_PAGINA; i < Math.min(alle.size(), (st[1] + 1) * PER_PAGINA); i++) {
            l.add(alle.get(i));
        }
        uit.putInt("Bron", bron.ordinal());
        uit.putInt("Pagina", st[1]);
        uit.putInt("Paginas", paginas);
        uit.put("Lijst", l);
        uit.putBoolean("Keet", keet != null);
        uit.putInt("Eigen", BouwSpelers.van(server).eigen(p.getUUID()).size());
        uit.putInt("MaxEigen", BouwSpelers.MAX_EIGEN);
        uit.putInt("Gepubliceerd", Bibliotheek.van(server).vanSpeler(p.getUUID()));
        uit.putInt("MaxPubliceer", Bibliotheek.MAX_PER_SPELER);
    }

    private static CompoundTag regel(Blauwdruk b, UUID wie) {
        CompoundTag c = new CompoundTag();
        c.putString("Id", b.id);
        c.putString("Naam", b.naam());
        c.putString("Maker", b.makerNaam);
        Vec3i g = b.ontwerp.grootte();
        c.putIntArray("Grootte", new int[] {g.getX(), g.getY(), g.getZ()});
        c.putInt("Aantal", b.ontwerp.aantal());
        c.putInt("Duimpjes", b.duimpjes());
        c.putBoolean("Mijn", b.heeftDuimpje(wie));
        c.put("Voorbeeld", new IntArrayTag(b.voorbeeld));
        return c;
    }

    /** A blueprint by where it lives ("c:" catalogue ids, else own ones of anybody, else the library). */
    @Nullable
    public static Blauwdruk zoek(MinecraftServer server, String id) {
        if (id.startsWith("c:")) {
            return Catalogus.get(id.substring(2));
        }
        Blauwdruk b = BouwSpelers.van(server).zoek(id);
        return b != null ? b : Bibliotheek.van(server).get(id);
    }

    /** A blueprint's display name (the catalogue's are texts). */
    public static Component naam(Blauwdruk b) {
        return b.id.startsWith("c:") ? Component.translatable("gui.guhs.blauwdruk.cat." + b.id.substring(2)) : Component.literal(b.naam());
    }

    // =====================================================================================================================
    // the buttons
    // =====================================================================================================================

    /**
     * A button (waarde: the source for BRON, 1 / 255 for the next / previous page; tekst: the blueprint's id). {@code keet}:
     * the keet whose tab it is (null: the bureau). True when something changed (send the page again).
     */
    public static boolean doe(ServerPlayer p, int actie, int waarde, String tekst, @Nullable BouwkeetBlockEntity keet) {
        MinecraftServer server = p.level().getServer();
        int[] st = stand(p.getUUID());
        switch (actie) {
            case BRON -> {
                st[0] = Math.floorMod(waarde, Bron.values().length);
                st[1] = 0;
            }
            case PAGINA -> st[1] += waarde == 255 ? -1 : 1;
            case PLAATS -> {
                Blauwdruk b = zoek(server, tekst);
                if (b != null && keet != null) {
                    alsProject(p, p.level(), keet, b.ontwerp, naam(b).getString());
                }
            }
            case PRINT -> {
                Blauwdruk b = zoek(server, tekst);
                if (b == null) {
                    return false;
                }
                if (!p.isCreative()) {
                    int i = p.getInventory().findSlotMatchingItem(new ItemStack(Items.PAPER));
                    if (i < 0) {
                        p.sendOverlayMessage(Component.translatable("gui.guhs.blauwdruk.papier"));
                        return false;
                    }
                    p.getInventory().removeItem(i, 1);
                }
                ItemStack item = BlauwdrukItem.van(b);
                if (!p.getInventory().add(item)) {
                    p.drop(item, false);
                }
                p.level().playSound(null, p.blockPosition(), nl.juiced.guhs.feature.bouwhulp.BouwGeluid.PAPIER.get(), SoundSource.PLAYERS, 0.8f, 1f);   // F12
            }
            case DUIMPJE -> Bibliotheek.van(server).duimpje(tekst, p.getUUID());
            case VERWIJDER -> {
                if (Bron.values()[st[0]] == Bron.EIGEN) {
                    BouwSpelers.van(server).weg(p.getUUID(), tekst);
                } else {
                    Bibliotheek.van(server).verwijder(tekst, p.getUUID(), p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
                }
            }
            case PUBLICEER -> {
                Blauwdruk b = BouwSpelers.van(server).eigen(p.getUUID(), tekst);
                BouwNiveau n = BouwNiveau.van(keet);
                if (b == null || keet == null) {
                    return false;
                }
                if (!n.publiceren()) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.blauwdruk.publiceer.vanaf"));
                    return false;
                }
                if (b.ontwerp.maat() > n.maxBlauwdruk) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.blauwdruk.publiceer.te_groot", n.maxBlauwdruk));
                    return false;
                }
                Bibliotheek.Uitkomst u = Bibliotheek.van(server).publiceer(b, p.getUUID(), p.getGameProfile().name());
                p.sendOverlayMessage(Component.translatable("gui.guhs.blauwdruk.publiceer." + u.name().toLowerCase(java.util.Locale.ROOT),
                        Bibliotheek.MAX_PER_SPELER));
                if (u == Bibliotheek.Uitkomst.OK) {
                    p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.5f);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** The bureau's buttons (through GereedschapPayloads "bureau"): then the page again. */
    public static void doeBureau(ServerPlayer p, int actie, int waarde, String tekst) {
        doe(p, actie, waarde, tekst, null);
        openBureau(p, false);
    }

    public static void openBureau(ServerPlayer p, boolean open) {
        CompoundTag t = new CompoundTag();
        schrijf(p, t, null);
        t.putBoolean("Open", open);
        GereedschapPayloads.stuur(p, "bureau", t);
    }

    /** An ontwerp as the keet's project: a ghost in front of the keet, its front (south) facing the keet. */
    public static void alsProject(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, Ontwerp o, String naam) {
        Spook oud = keet.project();
        if (oud != null && oud.status() == Spook.Status.BOUWT) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwkeet.bezig"));
            return;
        }
        Direction voor = keet.voorkant();
        Rotation draai = switch (voor) {
            case EAST -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            default -> Rotation.NONE;
        };
        Vec3i g = o.grootte();
        int diep = draai == Rotation.NONE || draai == Rotation.CLOCKWISE_180 ? g.getZ() : g.getX();
        BlockPos midden = keet.voorDeDeur(2 + diep / 2);
        BlockPos oorsprong = midden.offset(-(g.getX() - 1) / 2, 0, -(g.getZ() - 1) / 2);
        if (oud != null) {
            Spookbouw.annuleer(level, oud);
        }
        Spook s = Spookbouw.start(p, o, oorsprong, draai, Mirror.NONE);
        if (s != null) {
            Spookbouw.zetNaam(level, s, naam);
            keet.zetProject(level, s);
        }
    }

    private Blauwdrukken() {
    }
}
