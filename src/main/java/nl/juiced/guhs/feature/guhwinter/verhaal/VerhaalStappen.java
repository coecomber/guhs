package nl.juiced.guhs.feature.guhwinter.verhaal;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.feature.guhwinter.GuhwinterFase;
import nl.juiced.guhs.feature.guhwinter.GuhwinterZone;
import nl.juiced.guhs.feature.guhwinter.npc.GuhwinterNpcs;
import nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers;
import nl.juiced.guhs.feature.guhwinter.spellen.ChocoladeletterItem;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.VerhaalSync;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Guhwinter (verhaal slice): what moves the story on, per player.
 * <ul>
 *   <li>every half second in the Guhmension: opening the chapters near the plein, the spots you walk to (the plein for
 *       the intocht, Ozovads' hiding place, the kade, the spruces, the table), the carrot trail and the glittering pakjes
 *       (particles only that player sees), pakjesavond on the plein, a filled schoentje;</li>
 *   <li>right-clicking a plein guh ({@link GuhwinterNpcs#klikHook}): the steps that start with a guh;</li>
 *   <li>right-clicking blocks: the schoentje by the hearth, the kerstboom (versieren), the prikbord;</li>
 *   <li>logging in: the schoentje.</li>
 * </ul>
 */
public final class VerhaalStappen {
    /** How often the walk-to checks run per player (ticks). */
    static final int ELKE = 10;
    /** After a scene or talk, the walk-to spots leave you alone this long (ticks). */
    static final int PAUZE = 200;
    private static final DustParticleOptions ROOD = new DustParticleOptions(0xE0384C, 1.1f), GROEN = new DustParticleOptions(0x3FA34D, 1.1f),
            GOUD = new DustParticleOptions(0xFFD34A, 0.9f);

    private static final Map<UUID, Long> RUST = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LOGIN = new ConcurrentHashMap<>();

    private VerhaalStappen() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(VerhaalStappen::opTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VerhaalStappen::opBlok);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                LOGIN.put(p.getUUID(), (long) p.level().getServer().getTickCount() + 60);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            RUST.clear();
            LOGIN.clear();
        });
        GuhwinterNpcs.klikHook((speler, naam, npc) -> klik(speler, naam));
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    @Nullable
    static Vec3 anker(String naam) {
        GuhwinterAnkers.Anker a = GuhwinterAnkers.zoek(naam);
        return a == null ? null : a.vec();
    }

    static boolean bij(ServerPlayer p, String anker, double afstand) {
        Vec3 a = anker(anker);
        return a != null && Math.abs(p.getY() - a.y) < 4 && sq(p.getX() - a.x) + sq(p.getZ() - a.z) <= afstand * afstand;
    }

    private static double sq(double d) {
        return d * d;
    }

    /** Is the player free for a scene: in the Guhmension, not watching or reading something, not in a game? */
    static boolean vrij(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && p.level().dimension() == ModDimensions.GUHMENSION && !Cutscenes.bezig(p) && !Praat.bezig(p)
                && Minigames.playing(p) == null && !Wandeling.doetMee(p);
    }

    private static boolean rust(ServerPlayer p) {
        Long tot = RUST.get(p.getUUID());
        return tot != null && p.level().getServer().getTickCount() < tot;
    }

    /** Leave this player's walk-to spots alone for a while (after a scene, a talk, a game). */
    static void rustig(ServerPlayer p) {
        RUST.put(p.getUUID(), (long) p.level().getServer().getTickCount() + PAUZE);
    }

    /** A scene, and its step afterwards; the walk-to spots wait until it is over. */
    static void scene(ServerPlayer p, nl.juiced.guhs.feature.verhaal.Cutscene s, java.util.function.Consumer<ServerPlayer> daarna) {
        if (WinterScenes.speel(p, s, daarna)) {
            rustig(p);
        }
    }

    /** Near the plein (the square plus the arrival by the portal and Ozovads' hiding place). */
    static boolean bijPlein(ServerPlayer p) {
        return p.level().dimension() == ModDimensions.GUHMENSION && p.getX() >= GuhwinterZone.MIN_X - 24 && p.getX() <= GuhwinterZone.MAX_X + 24
                && p.getZ() >= GuhwinterZone.MIN_Z - 24 && p.getZ() <= GuhwinterZone.MAX_Z + 40 && p.getY() > 10 && p.getY() < 120;
    }

    static boolean opPlein(ServerPlayer p) {
        return GuhwinterZone.isPlein(p.level(), p.blockPosition());
    }

    // =====================================================================================================================
    // every half second
    // =====================================================================================================================

    private static void opTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % ELKE != 0) {
            return;
        }
        MinecraftServer server = p.level().getServer();
        Long login = LOGIN.get(p.getUUID());
        if (login != null && server.getTickCount() >= login) {
            LOGIN.remove(p.getUUID());
            schoenGevuld(p);
        }
        GuhwinterFase f = Guhwinter.fase(server);
        if (!f.bezig() || !bijPlein(p)) {
            return;
        }
        for (WinterVerhaal.Hoofdstuk h : WinterVerhaal.Hoofdstuk.values()) {
            if (h.open(f) && h.lijn().begin(p)) {
                p.sendSystemMessage(Component.translatable("guhwinter.verhaal.open." + h.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        sporen(p, f);
        if (!vrij(p) || rust(p)) {
            return;
        }
        int sint = WinterVerhaal.SINT.stap(p);
        boolean terug = !f.sint();
        switch (sint) {
            case WinterVerhaal.S_INTOCHT -> {
                if (opPlein(p)) {
                    scene(p, terug ? WinterScenes.INTOCHT_TERUG : WinterScenes.INTOCHT, a -> {
                        WinterVerhaal.SINT.verder(a, WinterVerhaal.S_INTOCHT);
                        a.sendSystemMessage(Component.translatable("guhwinter.verhaal.hint.spoor").withStyle(ChatFormatting.YELLOW));
                    });
                    return;
                }
            }
            case WinterVerhaal.S_OZOVADS -> {
                if (bij(p, "paard_verstop", 3.2)) {
                    scene(p, WinterScenes.OZOVADS, a -> {
                        WinterVerhaal.SINT.verder(a, WinterVerhaal.S_OZOVADS);
                        a.sendSystemMessage(Component.translatable("guhwinter.verhaal.hint.pakjes", WinterVerhaal.PAKJES_NODIG).withStyle(ChatFormatting.YELLOW));
                    });
                    return;
                }
            }
            case WinterVerhaal.S_PAKJES -> pakjesRapen(p);
            case WinterVerhaal.S_TERUG -> {
                if (bij(p, "kade_boot", 3.5)) {
                    sintBlijft(p);
                    return;
                }
            }
            case WinterVerhaal.S_PAKJESAVOND -> {
                if (f == GuhwinterFase.PAKJESAVOND && opPlein(p)) {
                    scene(p, WinterScenes.PAKJESAVOND, a -> WinterVerhaal.SINT.verder(a, WinterVerhaal.S_PAKJESAVOND));
                    return;
                }
                if (f == GuhwinterFase.PAKJESAVOND && WinterVerhaal.SINT.eenmalig(p, "pakjesavond_roep")) {
                    p.sendSystemMessage(Component.translatable("guhwinter.verhaal.hint.pakjesavond").append(" ").append(Guhwinter.hoeKomIkDaar())
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            default -> {
            }
        }
        if (WinterVerhaal.open(p, WinterVerhaal.Hoofdstuk.KERST)) {
            int kerst = WinterVerhaal.KERST.stap(p);
            if (kerst == WinterVerhaal.K_SPARREN && bij(p, "paard_verstop", 4.5)) {
                rustig(p);
                WinterGesprek.boomKiezen(p);
                return;
            }
            if (kerst == WinterVerhaal.K_DINER && bij(p, "huis_tafel_midden", 2.5)) {
                diner(p);
                return;
            }
        }
        schoenGevuld(p);
    }

    /** The particles of the step you are at, only for you: the carrot trail, the glittering pakjes, the spruces. */
    private static void sporen(ServerPlayer p, GuhwinterFase f) {
        if (!(p.level() instanceof ServerLevel sl) || Cutscenes.bezig(p)) {
            return;
        }
        int sint = WinterVerhaal.SINT.stap(p);
        if (sint == WinterVerhaal.S_OZOVADS && (p.tickCount / ELKE) % 2 == 0) {
            ItemParticleOption wortel = new ItemParticleOption(ParticleTypes.ITEM, Items.CARROT);
            List<GuhwinterAnkers.Anker> spoor = GuhwinterAnkers.reeks("paard_spoor_");
            for (int i = 0; i < spoor.size(); i++) {
                Vec3 v = spoor.get(i).vec();
                if (v.distanceToSqr(p.position()) < 64 * 64) {
                    deeltje(sl, p, wortel, v.x, v.y + 0.25, v.z, 3, 0.12, 0.02);
                    deeltje(sl, p, ParticleTypes.POOF, v.x + (i % 2 == 0 ? 0.25 : -0.25), v.y + 0.05, v.z, 1, 0.05, 0.0);
                }
            }
            if (!spoor.isEmpty()) {
                Vec3 v = spoor.get(0).vec();
                deeltje(sl, p, ParticleTypes.END_ROD, v.x, v.y + 1.5, v.z, 2, 0.1, 0.6);    // (the start of the trail, seen from the gate)
            }
        } else if (sint == WinterVerhaal.S_PAKJES) {
            int gevonden = WinterVerhaal.SINT.teller(p, "pakjes");
            List<GuhwinterAnkers.Anker> pakjes = GuhwinterAnkers.reeks("pakje_verstop_");
            for (int i = 0; i < pakjes.size(); i++) {
                Vec3 v = pakjes.get(i).vec();
                if ((gevonden & (1 << i)) == 0 && v.distanceToSqr(p.position()) < 48 * 48) {
                    deeltje(sl, p, i % 2 == 0 ? ROOD : GROEN, v.x, v.y + 0.4, v.z, 4, 0.22, 0.0);
                    deeltje(sl, p, GOUD, v.x, v.y + 0.75, v.z, 2, 0.1, 0.0);
                    deeltje(sl, p, ParticleTypes.END_ROD, v.x, v.y + 1.2, v.z, 1, 0.15, 0.4);
                }
            }
        } else if (sint == WinterVerhaal.S_SCHOEN) {
            Vec3 v = anker("schoorsteen_schoen");
            if (v != null && v.distanceToSqr(p.position()) < 24 * 24) {
                deeltje(sl, p, GOUD, v.x, v.y + 0.3, v.z, 3, 0.2, 0.0);
            }
        }
        if (WinterVerhaal.Hoofdstuk.KERST.open(f) && WinterVerhaal.KERST.stap(p) == WinterVerhaal.K_SPARREN && (p.tickCount / ELKE) % 2 == 0) {
            Vec3 v = anker("paard_verstop");
            if (v != null && v.distanceToSqr(p.position()) < 80 * 80) {
                deeltje(sl, p, ParticleTypes.HAPPY_VILLAGER, v.x, v.y + 1.5, v.z, 6, 1.2, 0.0);
                deeltje(sl, p, ParticleTypes.END_ROD, v.x, v.y + 3.0, v.z, 1, 0.1, 0.6);
            }
        }
    }

    private static void deeltje(ServerLevel sl, ServerPlayer p, ParticleOptions d, double x, double y, double z, int n, double spreiding, double dy) {
        sl.sendParticles(p, d, true, false, x, y, z, n, spreiding, dy > 0 ? dy : spreiding * 0.5, spreiding, 0.01);
    }

    // =====================================================================================================================
    // chapter 1
    // =====================================================================================================================

    /** How many lost pakjes this player found (bits of the counter "pakjes"). */
    public static int pakjesGevonden(ServerPlayer p) {
        return Integer.bitCount(WinterVerhaal.SINT.teller(p, "pakjes"));
    }

    private static void pakjesRapen(ServerPlayer p) {
        int gevonden = WinterVerhaal.SINT.teller(p, "pakjes");
        List<GuhwinterAnkers.Anker> pakjes = GuhwinterAnkers.reeks("pakje_verstop_");
        for (int i = 0; i < pakjes.size() && i < 31; i++) {
            Vec3 v = pakjes.get(i).vec();
            if ((gevonden & (1 << i)) != 0 || Math.abs(p.getY() - v.y) > 2.5 || sq(p.getX() - v.x) + sq(p.getZ() - v.z) > 1.8 * 1.8) {
                continue;
            }
            gevonden |= 1 << i;
            WinterVerhaal.SINT.teller(p, "pakjes", gevonden);
            int n = Integer.bitCount(gevonden);
            if (p.level() instanceof ServerLevel sl) {
                sl.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, false, v.x, v.y + 0.6, v.z, 14, 0.4, 0.4, 0.4, 0.05);
            }
            p.level().playSound(null, p.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.9f, 1.0f + n * 0.08f);
            p.sendOverlayMessage(Component.translatable("guhwinter.verhaal.pakje", Math.min(n, WinterVerhaal.PAKJES_NODIG), WinterVerhaal.PAKJES_NODIG)
                    .withStyle(ChatFormatting.GOLD));
            if (n >= WinterVerhaal.PAKJES_NODIG && WinterVerhaal.SINT.verder(p, WinterVerhaal.S_PAKJES)) {
                p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.5f);
                p.sendSystemMessage(Component.translatable("guhwinter.verhaal.hint.terug").withStyle(ChatFormatting.YELLOW));
            } else {
                VerhaalSync.sync(p);
            }
            return;
        }
    }

    /** Back at the boot (or a click on the Sint or a Pakjesguh): "Dan vieren we het dit jaar hier!"; then the lootjes book and a carrot. */
    static void sintBlijft(ServerPlayer p) {
        boolean terug = WinterVerhaal.terugblik(p.level().getServer());
        scene(p, terug ? WinterScenes.SINT_BLIJFT_TERUG : WinterScenes.SINT_BLIJFT, a -> {
            if (!WinterVerhaal.SINT.verder(a, WinterVerhaal.S_TERUG)) {
                return;
            }
            ItemStack wortel = new ItemStack(Items.CARROT);
            if (!a.getInventory().add(wortel)) {
                a.drop(wortel, false);
            }
            a.sendSystemMessage(Component.translatable("guhwinter.verhaal.hint.schoen").withStyle(ChatFormatting.YELLOW));
            if (Guhwinter.inschrijvenOpen(a.level().getServer())) {
                if (!openBoek(a)) {
                    a.sendSystemMessage(Component.translatable("guhwinter.verhaal.hint.boek").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        });
    }

    /** The lootjes slice's Het Grote Boek (its page in the chat). Always there since the 1.10.10 integration. */
    static boolean openBoek(ServerPlayer p) {
        nl.juiced.guhs.feature.guhwinter.lootjes.Lootjes.openBoek(p);
        return true;
    }

    /** The day (Europe/Amsterdam, honouring the dev date) as a number: the schoentje needs a night in between. */
    static int dag(MinecraftServer server) {
        return (int) Guhwinter.nu(server).withZoneSameInstant(Guhwinter.ZONE).toLocalDate().toEpochDay();
    }

    /** Your shoe by the hearth, with a carrot for Ozovads (or a sugar cube when you lost it). */
    static void schoenZetten(ServerPlayer p) {
        Verhaallijn l = WinterVerhaal.SINT;
        if (l.stap(p) != WinterVerhaal.S_SCHOEN || l.vlag(p, "schoen")) {
            return;
        }
        boolean wortel = p.getInventory().contains(new ItemStack(Items.CARROT));
        if (wortel) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.is(Items.CARROT)) {
                    s.shrink(1);
                    break;
                }
            }
        }
        l.vlag(p, "schoen", true);
        l.teller(p, "schoen_dag", dag(p.level().getServer()));
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.9f, 1.1f);
        p.sendSystemMessage(Component.translatable(wortel ? "guhwinter.verhaal.schoen.gezet" : "guhwinter.verhaal.schoen.suiker").withStyle(ChatFormatting.LIGHT_PURPLE));
        l.verder(p, WinterVerhaal.S_SCHOEN);
        if (WinterVerhaal.terugblik(p.level().getServer())) {
            // (the Sint has left already: he filled it before he went)
            vulSchoen(p, true);
        } else {
            p.sendSystemMessage(Component.translatable("guhwinter.verhaal.schoen.nacht").withStyle(ChatFormatting.GRAY));
        }
    }

    /** A login (or a look) at least one night after you put your shoe: a chocolate letter and a handful of pepernoten. Once. */
    static void schoenGevuld(ServerPlayer p) {
        Verhaallijn l = WinterVerhaal.SINT;
        if (!l.vlag(p, "schoen") || l.vlag(p, "schoen_vol") || dag(p.level().getServer()) <= l.teller(p, "schoen_dag")) {
            return;
        }
        vulSchoen(p, false);
    }

    private static void vulSchoen(ServerPlayer p, boolean nu) {
        Verhaallijn l = WinterVerhaal.SINT;
        if (!l.eenmalig(p, "schoen_vol_e")) {
            return;
        }
        l.vlag(p, "schoen_vol", true);
        ItemStack letter = ChocoladeletterItem.maak(p.getGameProfile().name());
        Component naam = letter.getHoverName();
        if (!p.getInventory().add(letter)) {
            p.drop(letter, false);
        }
        Guhwinter.geefPepernoten(p, 8);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
        p.sendSystemMessage(Component.translatable(nu ? "guhwinter.verhaal.schoen.vol_terug" : "guhwinter.verhaal.schoen.vol",
                naam.copy().withStyle(ChatFormatting.GOLD), 8).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =====================================================================================================================
    // chapter 2
    // =====================================================================================================================

    static void diner(ServerPlayer p) {
        scene(p, WinterScenes.KERSTDINER, a -> WinterVerhaal.KERST.verder(a, WinterVerhaal.K_DINER));
    }

    // =====================================================================================================================
    // clicks
    // =====================================================================================================================

    /** A plein guh was right-clicked: a story step that starts with that guh (true: handled, no idle line). */
    static boolean klik(ServerPlayer p, String naam) {
        GuhwinterFase f = WinterVerhaal.fase(p);
        if (!f.bezig() || Cutscenes.bezig(p)) {
            return false;
        }
        boolean zes = Wandeling.isZes(naam);
        // chapter 1
        int sint = WinterVerhaal.SINT.stap(p);
        if (sint == WinterVerhaal.S_TERUG && (naam.equals("sint") || naam.startsWith("pakjesguh_"))) {
            sintBlijft(p);
            return true;
        }
        if (sint == WinterVerhaal.S_PAKJESAVOND && !f.sint() && (naam.equals("joost") || naam.equals("suus"))) {
            rustig(p);
            WinterGesprek.pakjesavondKort(p);
            return true;
        }
        // chapter 2
        if (WinterVerhaal.open(p, WinterVerhaal.Hoofdstuk.KERST)) {
            int kerst = WinterVerhaal.KERST.stap(p);
            if (kerst == WinterVerhaal.K_BOOM && (naam.equals("joost") || naam.equals("niels"))) {
                scene(p, WinterScenes.BOOMHALEN, a -> {
                    if (WinterVerhaal.KERST.verder(a, WinterVerhaal.K_BOOM)) {
                        a.sendSystemMessage(Component.translatable("guhwinter.verhaal.hint.sparren").withStyle(ChatFormatting.YELLOW));
                    }
                });
                return true;
            }
            if (kerst == WinterVerhaal.K_VERSIEREN && naam.equals("suus")) {
                rustig(p);
                WinterGesprek.versieren(p);
                return true;
            }
            if (kerst == WinterVerhaal.K_WANDELING && (zes || naam.equals("anthony"))) {
                Wandeling.klik(p);
                return true;
            }
            if (kerst == WinterVerhaal.K_OPA && (naam.equals("corine") || naam.equals("wijnand") || naam.equals("krijn") || naam.equals("marrie"))) {
                rustig(p);
                WinterGesprek.opa(p);
                return true;
            }
            if (kerst == WinterVerhaal.K_DINER && (naam.equals("corine") || naam.equals("krijn"))) {
                diner(p);
                return true;
            }
        }
        // chapter 3
        if (WinterVerhaal.open(p, WinterVerhaal.Hoofdstuk.OUDJAAR)) {
            int oud = WinterVerhaal.OUDJAAR.stap(p);
            if (oud == WinterVerhaal.O_OLIEBOLLEN && (naam.equals("lin") || naam.equals("liek"))) {
                rustig(p);
                WinterGesprek.lin(p);
                return true;
            }
            if (oud == WinterVerhaal.O_VOORNEMENS && zes) {
                rustig(p);
                if (WinterVerhaal.OUDJAAR.vlag(p, "gehoord")) {
                    Voornemens.openSchrijven(p);
                } else {
                    WinterGesprek.voornemens(p);
                }
                return true;
            }
        }
        return false;
    }

    /** Right-clicking blocks: the shoe spot, the kerstboom (versieren), the prikbord. Runs before tekenen's tree click. */
    private static void opBlok(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p)
                || e.getLevel().dimension() != ModDimensions.GUHMENSION || !GuhwinterZone.binnen(e.getPos())) {
            return;
        }
        GuhwinterFase f = WinterVerhaal.fase(p);
        if (!f.bezig() || Cutscenes.bezig(p)) {
            return;
        }
        BlockPos pos = e.getPos();
        if (dichtbij(pos, "prikbord", 2.0)) {
            Voornemens.openBord(p);
            klaar(e);
            return;
        }
        if (dichtbij(pos, "schoorsteen_schoen", 2.2)) {
            if (WinterVerhaal.SINT.stap(p) == WinterVerhaal.S_SCHOEN) {
                schoenZetten(p);
                klaar(e);
            } else if (WinterVerhaal.SINT.vlag(p, "schoen") && !WinterVerhaal.SINT.vlag(p, "schoen_vol")) {
                p.sendOverlayMessage(Component.translatable("guhwinter.verhaal.schoen.wacht").withStyle(ChatFormatting.GRAY));
                klaar(e);
            }
            return;
        }
        if (WinterVerhaal.open(p, WinterVerhaal.Hoofdstuk.KERST) && WinterVerhaal.KERST.stap(p) == WinterVerhaal.K_VERSIEREN) {
            GuhwinterAnkers.Anker boom = GuhwinterAnkers.zoek("kerstboom");
            if (boom != null && isBoom(boom.pos(), pos, e.getLevel().getBlockState(pos))) {
                rustig(p);
                WinterGesprek.versieren(p);
                klaar(e);
            }
        }
    }

    private static void klaar(PlayerInteractEvent.RightClickBlock e) {
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    private static boolean dichtbij(BlockPos pos, String anker, double r) {
        Vec3 a = anker(anker);
        return a != null && Vec3.atCenterOf(pos).distanceToSqr(a.x, a.y + 0.5, a.z) <= r * r;
    }

    /** The same test as tekenen's tree click: leaves or logs within 3 blocks of the trunk, up to 10 above it. */
    static boolean isBoom(BlockPos anker, BlockPos pos, BlockState state) {
        int dx = Math.abs(pos.getX() - anker.getX()), dz = Math.abs(pos.getZ() - anker.getZ()), dy = pos.getY() - anker.getY();
        if (dx > 3 || dz > 3 || dy < -1 || dy > 10) {
            return false;
        }
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS);
    }

    /** (dev) forget the pauses. */
    static void vergeet(ServerPlayer p) {
        RUST.remove(p.getUUID());
    }
}
