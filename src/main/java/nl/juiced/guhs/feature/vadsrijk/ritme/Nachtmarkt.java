package nl.juiced.guhs.feature.vadsrijk.ritme;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * U-LEVEN-RITME: the NIGHT MARKET with lampions in a few towns (the ones with a {@link Steden.Stad#markt} anchor: Zwamzoet,
 * Kandijkom, Lampionhaven, Saffraanpoort). At night, while a player is within {@link #STRAAL} blocks: a ring of glowing lampion
 * strings above the square (display entities, full bright: no blocks are placed, nothing blocks a path), a "Nachtmarkt" sign
 * and two stall guhs:
 * <ul>
 *   <li><b>Lotje Lampion</b> sells a lampionsnoepje (10 brons): a minute of Snelpootjes;</li>
 *   <li><b>Opa Sterrenkoek</b> sells a sterrenkoekje (5 brons): a funny fortune and a bit of Vads back.</li>
 * </ul>
 * Gone at dawn.
 */
public final class Nachtmarkt {
    static final int STRAAL = 64, SNOEP_PRIJS = 10, KOEK_PRIJS = 5, KOEK_SPREUKEN = 8;
    static final String SLEUTEL = "vr_uritme_markt";
    static final int KOOP = 1, NEE = 2;
    private static final String[] KLEUR = {"roze", "geel", "mint"};

    private record Markt(List<UUID> deco, UUID lotje, UUID opa) {
    }

    private static final Map<String, Markt> MARKTEN = new HashMap<>();
    /** vendor entity -> "snoep" / "koek". */
    private static final Map<UUID, String> VERKOPERS = new ConcurrentHashMap<>();
    /** player -> what the open conversation is about. */
    private static final Map<UUID, String> GESPREK = new ConcurrentHashMap<>();

    private Nachtmarkt() {
    }

    static void init() {
        Praat.luister(SLEUTEL, Nachtmarkt::antwoord);
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 17) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        boolean nacht = Rooster.nacht(sl);
        for (Steden.Stad s : Steden.ALLE) {
            if (s.markt() == null) {
                continue;
            }
            BlockPos a = s.markt().get();
            Markt m = MARKTEN.get(s.id());
            boolean moet = nacht && Ritme.spelerBij(sl, a, STRAAL) && sl.hasChunk(a.getX() >> 4, a.getZ() >> 4);
            if (!moet) {
                if (m != null) {
                    weg(sl, s.id());
                }
                continue;
            }
            if (m != null && Ritme.leeft(sl, m.deco()) && sl.getEntity(m.lotje()) != null && sl.getEntity(m.opa()) != null) {
                vonkjes(sl, a);
                continue;
            }
            if (m != null) {
                weg(sl, s.id());
            }
            bouw(sl, s.id(), a);
        }
    }

    private static void vonkjes(ServerLevel sl, BlockPos a) {
        if (sl.getRandom().nextInt(3) == 0) {
            double h = sl.getRandom().nextDouble() * Math.PI * 2;
            sl.sendParticles(ParticleTypes.END_ROD, a.getX() + 0.5 + Math.cos(h) * 5.5, a.getY() + 3.8, a.getZ() + 0.5 + Math.sin(h) * 5.5, 1, 0.1, 0.1, 0.1, 0.005);
        }
    }

    private static void weg(ServerLevel sl, String id) {
        Markt m = MARKTEN.remove(id);
        if (m == null) {
            return;
        }
        Ritme.weg(sl, new java.util.ArrayList<>(m.deco()));
        for (UUID u : List.of(m.lotje(), m.opa())) {
            Entity e = sl.getEntity(u);
            if (e != null) {
                e.discard();
            }
            VERKOPERS.remove(u);
        }
    }

    private static BlockState lampion(int i) {
        return ModBlocks.LAMPIONNEN.get(KLEUR[i % KLEUR.length]).get().defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    /** Builds the market around anchor a: lampion strings on a ring (sagging between the big ones), the sign, two stall guhs. */
    static void bouw(ServerLevel sl, String id, BlockPos a) {
        List<UUID> deco = Ritme.lijst();
        double cx = a.getX() + 0.5, cz = a.getZ() + 0.5, y0 = a.getY() + 4.2, r = 5.5;
        int groot = 8, tussen = 2;
        for (int i = 0; i < groot; i++) {
            double h0 = Math.PI * 2 * i / groot, h1 = Math.PI * 2 * (i + 1) / groot;
            Entity e = Ritme.blok(sl, cx + Math.cos(h0) * r, y0 - 0.5, cz + Math.sin(h0) * r, lampion(i), 0.7f, 0, true);
            if (e != null) {
                deco.add(e.getUUID());
            }
            for (int k = 1; k <= tussen; k++) {
                double f = k / (double) (tussen + 1), h = h0 + (h1 - h0) * f;
                double zak = Math.sin(Math.PI * f) * 0.7;   // (the string sags between the big lampions)
                Entity t = Ritme.blok(sl, cx + Math.cos(h) * r, y0 - zak, cz + Math.sin(h) * r, lampion(i + k), 0.35f, 0, true);
                if (t != null) {
                    deco.add(t.getUUID());
                }
            }
        }
        Entity bord = Ritme.tekst(sl, cx, y0 + 0.9, cz, Component.translatable(Ritme.P + "markt.bord").withStyle(ChatFormatting.GOLD), 1.3f, 0x50301822);
        if (bord != null) {
            deco.add(bord.getUUID());
        }
        RijkGuh lotje = verkoper(sl, a, 0, "snoep", GuhVariant.MINT, GuhClothes.KERMIS_HOED, GuhClothes.KERMIS_STRIK);
        RijkGuh opa = verkoper(sl, a, 1, "koek", GuhVariant.SNOW, GuhClothes.BAKKERSMUTSJE, GuhClothes.KNUS_SJAALTJE);
        if (lotje == null || opa == null) {
            Ritme.weg(sl, deco);
            for (RijkGuh g : new RijkGuh[] {lotje, opa}) {
                if (g != null) {
                    g.discard();
                }
            }
            return;
        }
        for (RijkGuh g : new RijkGuh[] {lotje, opa}) {
            Entity l = Ritme.blok(sl, g.getX(), g.getY() + 2.15, g.getZ(), lampion(g == lotje ? 0 : 1), 0.45f, 0, true);
            if (l != null) {
                deco.add(l.getUUID());
            }
        }
        MARKTEN.put(id, new Markt(deco, lotje.getUUID(), opa.getUUID()));
    }

    @Nullable
    private static RijkGuh verkoper(ServerLevel sl, BlockPos a, int nr, String wat, GuhVariant v, GuhClothes... kleren) {
        BlockPos rond = a.offset(nr == 0 ? 3 : -3, 0, nr == 0 ? 2 : -2);
        BlockPos p = Ritme.vrijeplek(sl, rond, 0, 3);
        if (p == null) {
            p = Ritme.vrijeplek(sl, a, 2, 6);
        }
        if (p == null) {
            return null;
        }
        RijkGuh g = NpcRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
        if (g == null) {
            return null;
        }
        g.setVariant(v);
        for (GuhClothes c : kleren) {
            g.wear(c);
        }
        g.setCustomName(Component.translatable(Ritme.P + "markt.naam." + wat).withStyle(ChatFormatting.GOLD));
        float yaw = (float) Math.toDegrees(Math.atan2(-(a.getX() - p.getX()), a.getZ() - p.getZ()));
        g.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0);
        g.setYHeadRot(yaw);
        g.setYBodyRot(yaw);
        g.addTag(Ritme.TAG);
        sl.addFreshEntity(g);
        VERKOPERS.put(g.getUUID(), wat);
        return g;
    }

    /** A click on a stall guh (EntityInteract, before RijkGuh's own click). True = one of ours. */
    static boolean klik(ServerPlayer p, Entity e) {
        String wat = VERKOPERS.get(e.getUUID());
        if (wat == null) {
            return false;
        }
        GESPREK.put(p.getUUID(), wat);
        int prijs = wat.equals("snoep") ? SNOEP_PRIJS : KOEK_PRIJS;
        Praat.scene(p, SLEUTEL, List.of(new Praat.Regel(e, "", Ritme.P + "markt." + wat + ".hallo", prijs)),
                new Praat.Optie(KOOP, Ritme.P + "markt." + wat + ".koop"), new Praat.Optie(NEE, Ritme.P + "markt.nee"));
        return true;
    }

    private static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        String wat = GESPREK.remove(p.getUUID());
        if (wat == null || optie != KOOP) {
            return;
        }
        koop(p, wat);
    }

    /** Buys the stall's treat (true when paid). */
    static boolean koop(ServerPlayer p, String wat) {
        int prijs = wat.equals("snoep") ? SNOEP_PRIJS : KOEK_PRIJS;
        if (!Spelers.neemMunten(p, prijs)) {
            p.sendSystemMessage(Component.translatable(Ritme.P + "markt.te_weinig"));
            return false;
        }
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.7f, 1.3f);
        if (wat.equals("snoep")) {
            Statussen.geef(p, Status.SNELPOOTJES, 20 * 60, 0.25, null);
            p.sendSystemMessage(Component.translatable(Ritme.P + "markt.snoep.op").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            Schade.heal(p, p, Spelers.maxVads(p) * 0.3);
            p.sendSystemMessage(Component.translatable(Ritme.P + "markt.koek.op",
                    Component.translatable(Ritme.P + "markt.koek.spreuk." + (1 + p.getRandom().nextInt(KOEK_SPREUKEN)))).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 2, p.getZ(), 3, 0.3, 0.2, 0.3, 0.01);
        }
        return true;
    }

    static int aantal() {
        return MARKTEN.size();
    }

    /** Dev: build the market of this town now (ignores the clock; it is cleared again by day unless the schedule is forced). */
    static boolean nu(ServerLevel sl, Steden.Stad s) {
        if (s.markt() == null) {
            return false;
        }
        weg(sl, s.id());
        bouw(sl, s.id(), s.markt().get());
        return MARKTEN.containsKey(s.id());
    }

    static void wis() {
        MARKTEN.clear();
        VERKOPERS.clear();
        GESPREK.clear();
    }
}
