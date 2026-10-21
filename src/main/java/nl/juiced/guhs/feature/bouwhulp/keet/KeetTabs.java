package nl.juiced.guhs.feature.bouwhulp.keet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.bank.BankAdressen;
import nl.juiced.guhs.feature.bouwhulp.BouwhulpFeature;
import nl.juiced.guhs.feature.bouwhulp.kern.Materiaal;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.bouwhulp.kern.Spookbouw;
import nl.juiced.guhs.feature.bouwhulp.kern.Spoken;

/**
 * The tabs F8 brings (see {@link BouwkeetTabs}): Bouwguhs, Materiaal and Niveau, plus "binnenkort" stand-ins for Vormen,
 * Daken and Blauwdrukken that F9 replaces. The stand-in of Vormen has one button, "Proefhuisje": a little guh hut as
 * project, so the whole spookbouw (ghost, panel, shortage, building, bouwguhs, Pof!, undo) can be tried before F9.
 */
public final class KeetTabs {
    public static void register() {
        BouwkeetTabs.registreer(new Binnenkort(BouwkeetTabs.VORMEN, 10, true));
        BouwkeetTabs.registreer(new Binnenkort(BouwkeetTabs.DAKEN, 20, false));
        BouwkeetTabs.registreer(new Binnenkort(BouwkeetTabs.BLAUWDRUKKEN, 30, false));
        BouwkeetTabs.registreer(new BouwguhsTab());
        BouwkeetTabs.registreer(new MateriaalTab());
        BouwkeetTabs.registreer(new NiveauTab());
    }

    // =====================================================================================================================
    // stand-ins
    // =====================================================================================================================

    record Binnenkort(String id, int volgorde, boolean proef) implements BouwkeetTabs.Tab {
        @Override
        public void schrijf(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, CompoundTag uit) {
            uit.putBoolean("Binnenkort", true);
            uit.putBoolean("Proef", proef);
        }

        @Override
        public void doe(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, int actie, int waarde, String tekst) {
            if (proef && actie == 1) {
                proefhuisje(p, level, keet);
            }
        }
    }

    /** The test project: a 5 x 5 guh hut (pink walls, windows, a door, a flat roof with two ears) in front of the keet. */
    static void proefhuisje(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet) {
        Spook oud = keet.project();
        if (oud != null && oud.status() == Spook.Status.BOUWT) {
            return;
        }
        if (oud != null) {
            Spookbouw.annuleer(level, oud);
        }
        Ontwerp o = proefhuisje();
        Direction voor = keet.voorkant();
        BlockPos midden = keet.voorDeDeur(5);
        BlockPos oorsprong = midden.offset(-2, 0, -2);
        Rotation draai = switch (voor) {   // the door (south in the ontwerp) faces the keet
            case EAST -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            default -> Rotation.NONE;
        };
        Spook s = Spookbouw.start(p, o, oorsprong, draai, Mirror.NONE);
        if (s != null) {
            Spookbouw.zetNaam(level, s, net.minecraft.network.chat.Component.translatable("gui.guhs.bouwkeet.proefhuisje").getString());
            keet.zetProject(level, s);
        }
    }

    public static Ontwerp proefhuisje() {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        BlockState vloer = Blocks.SPRUCE_PLANKS.defaultBlockState(), muur = Blocks.PINK_TERRACOTTA.defaultBlockState(),
                raam = Blocks.GLASS_PANE.defaultBlockState(), dak = Blocks.PINK_WOOL.defaultBlockState(), lucht = Blocks.AIR.defaultBlockState();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                m.put(new BlockPos(x, 0, z), vloer);
                m.put(new BlockPos(x, 4, z), dak);
                for (int y = 1; y <= 3; y++) {
                    boolean rand = x == 0 || x == 4 || z == 0 || z == 4;
                    m.put(new BlockPos(x, y, z), rand ? muur : lucht);
                }
            }
        }
        m.put(new BlockPos(0, 2, 2), raam);
        m.put(new BlockPos(4, 2, 2), raam);
        m.put(new BlockPos(2, 2, 0), raam);
        m.put(new BlockPos(2, 1, 4), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH));
        m.put(new BlockPos(2, 2, 4), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        m.put(new BlockPos(2, 3, 2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        m.put(new BlockPos(0, 5, 0), dak);
        m.put(new BlockPos(0, 6, 0), Blocks.MAGENTA_WOOL.defaultBlockState());
        m.put(new BlockPos(4, 5, 0), dak);
        m.put(new BlockPos(4, 6, 0), Blocks.MAGENTA_WOOL.defaultBlockState());
        return Ontwerp.van(m);
    }

    // =====================================================================================================================
    // Bouwguhs
    // =====================================================================================================================

    static final class BouwguhsTab implements BouwkeetTabs.Tab {
        @Override
        public String id() {
            return BouwkeetTabs.BOUWGUHS;
        }

        @Override
        public int volgorde() {
            return 40;
        }

        @Override
        public BouwNiveau vanaf() {
            return BouwNiveau.GUHRAD;
        }

        @Override
        public void schrijf(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, CompoundTag uit) {
            ListTag lijst = new ListTag();
            List<UUID> gekozen = keet.bouwguhs();
            for (GuhEntity g : Bouwguhs.kandidaten(level, keet, p)) {
                CompoundTag c = new CompoundTag();
                c.store("Id", UUIDUtil.CODEC, g.getUUID());
                c.putString("Naam", g.getDisplayName().getString());
                c.putBoolean("Gekozen", gekozen.contains(g.getUUID()));
                lijst.add(c);
            }
            for (UUID id : gekozen) {             // named guhs that wandered off: still listed
                Entity e = level.getEntity(id);
                if (e instanceof GuhEntity g && g.blockPosition().closerThan(keet.getBlockPos(), Bouwguhs.KIES_BEREIK)) {
                    continue;
                }
                CompoundTag c = new CompoundTag();
                c.store("Id", UUIDUtil.CODEC, id);
                c.putString("Naam", e != null ? e.getDisplayName().getString() : "?");
                c.putBoolean("Gekozen", true);
                c.putBoolean("Weg", true);
                lijst.add(c);
            }
            uit.put("Guhs", lijst);
            uit.putInt("Max", BouwkeetBlockEntity.MAX_BOUWGUHS);
            uit.putInt("Hier", Bouwguhs.aanwezig(level, keet));
        }

        @Override
        public void doe(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, int actie, int waarde, String tekst) {
            if (actie != 1) {
                return;
            }
            UUID id;
            try {
                id = UUID.fromString(tekst);
            } catch (IllegalArgumentException e) {
                return;
            }
            if (level.getEntity(id) instanceof GuhEntity g) {
                if (!Bouwguhs.wissel(level, keet, p, g)) {
                    p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.bouwguh.kan_niet"));
                }
            } else if (keet.bouwguhs().contains(id)) {
                List<UUID> l = new java.util.ArrayList<>(keet.bouwguhs());
                l.remove(id);
                keet.zetBouwguhs(l);   // (it takes its helmet off by itself when it is loaded again)
            }
        }
    }

    // =====================================================================================================================
    // Materiaal: the shortage list, the Bank Guh, draadloos materiaal, the open ghosts
    // =====================================================================================================================

    static final class MateriaalTab implements BouwkeetTabs.Tab {
        @Override
        public String id() {
            return BouwkeetTabs.MATERIAAL;
        }

        @Override
        public int volgorde() {
            return 50;
        }

        @Override
        public void schrijf(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, CompoundTag uit) {
            UUID bank = keet.bank();
            uit.putBoolean("Bank", bank != null);
            uit.putBoolean("BankGevonden", bank != null && BankAdressen.van(level.getServer()).plek(bank) != null);
            boolean draadloos = keet.werkNiveau().draadloos();
            uit.putBoolean("Draadloos", draadloos);
            uit.putInt("Kisten", draadloos ? Materiaal.kisten(level, keet.getBlockPos()).size() : 0);
            Spook s = keet.project();
            if (s != null) {
                Map<Item, Integer> nodig = Spoken.van(level).nogNodig(level, s);
                Materiaal.Bronnen bronnen = keet.bronnen(level, s, p);
                ListTag lijst = new ListTag();
                nodig.entrySet().stream().sorted(Map.Entry.<Item, Integer>comparingByValue().reversed()).limit(40).forEach(e -> {
                    CompoundTag c = new CompoundTag();
                    c.putString("Id", BuiltInRegistries.ITEM.getKey(e.getKey()).toString());
                    c.putInt("N", e.getValue());
                    c.putLong("Heb", Math.min(Integer.MAX_VALUE, bronnen.tel(e.getKey())));
                    lijst.add(c);
                });
                uit.put("Nodig", lijst);
                uit.putBoolean("Gratis", bronnen.gratis());
            }
            ListTag spoken = new ListTag();
            List<Spook> lijstSpoken = new java.util.ArrayList<>(Spoken.van(level).van(p.getUUID()));
            lijstSpoken.sort(java.util.Comparator.<Spook>comparingInt(x -> keet.getBlockPos().equals(x.bouwerPos()) ? 0 : 1)
                    .thenComparingDouble(x -> x.oorsprong().distSqr(keet.getBlockPos())));
            for (Spook x : lijstSpoken) {
                CompoundTag c = new CompoundTag();
                c.store("Id", UUIDUtil.CODEC, x.id());
                c.putString("Naam", x.naam());
                c.putInt("Status", x.status().ordinal());
                c.putInt("Blokken", x.totaal());
                c.putInt("Afstand", (int) Math.sqrt(x.oorsprong().distSqr(keet.getBlockPos())));
                c.putBoolean("Project", keet.getBlockPos().equals(x.bouwerPos()));
                c.putBoolean("Elders", x.bouwerPos() != null && !keet.getBlockPos().equals(x.bouwerPos()));
                spoken.add(c);
            }
            uit.put("Spoken", spoken);
        }

        /** 1: show a ghost's panel (tekst = id); 2: make a ghost this keet's project; 3: unlink the bank. */
        @Override
        public void doe(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, int actie, int waarde, String tekst) {
            if (actie == 3) {
                keet.zetBank(null);
                return;
            }
            Spook s;
            try {
                s = Spookbouw.get(level, UUID.fromString(tekst));
            } catch (IllegalArgumentException e) {
                return;
            }
            if (s == null || !s.mag(p)) {
                return;
            }
            if (actie == 1) {
                Spookbouw.toonBediening(p, s);
            } else if (actie == 2 && (s.bouwerPos() == null || s.bouwerPos().equals(keet.getBlockPos()))) {
                keet.zetProject(level, s);
            }
        }
    }

    // =====================================================================================================================
    // Niveau
    // =====================================================================================================================

    static final class NiveauTab implements BouwkeetTabs.Tab {
        @Override
        public String id() {
            return BouwkeetTabs.NIVEAU;
        }

        @Override
        public int volgorde() {
            return 60;
        }

        @Override
        public void schrijf(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, CompoundTag uit) {
            BouwNiveau volgende = keet.niveau().volgende();
            if (volgende != null) {
                uit.putString("Upgrade", BuiltInRegistries.ITEM.getKey(BouwhulpFeature.upgrade(volgende)).toString());
            }
            uit.putInt("Vraag", keet.vadsVraag());
        }

        /** 1: switch on (waarde 1) or off (0). */
        @Override
        public void doe(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, int actie, int waarde, String tekst) {
            if (actie == 1) {
                keet.zetAan(waarde != 0);
            }
        }
    }

    private KeetTabs() {
    }
}
