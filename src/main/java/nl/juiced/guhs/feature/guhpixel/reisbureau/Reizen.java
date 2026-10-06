package nl.juiced.guhs.feature.guhpixel.reisbureau;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Plek;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.GuhOpslag;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.taal.Tekst;

/**
 * Reisbureau "De Vadsvakantie": the trips (DESIGN_PX section 5).
 * <ul>
 *   <li><b>The offer</b> ({@link #aanbod}): every real day four trips, one per duration (1, 2, 8, 24 hours), the same for
 *   everyone: derived from {@code Klok.dag()} and the world seed, nothing stored. Within four days every destination of a
 *   duration comes by once.</li>
 *   <li><b>Booking</b> ({@link #boek}): ONE guh per player at a time. The trip record is written at once; the guh walks off
 *   with its little suitcase ({@link Uitzwaaien}) and is then stored as data ({@link #opslaan}).</li>
 *   <li><b>Real time</b>: the record holds absolute {@code Klok.nu()} stamps; nothing ticks while the player is offline or
 *   the server is down.</li>
 *   <li><b>Coming back</b> ({@link #haalOp}): at ANY Reisbalie (the block holds nothing). The guh steps out with sunglasses
 *   on ({@link Zonnebril}); the player gets the ansichtkaart and a souvenir; a souvenir they already have becomes a stamp on
 *   the reispas, ten stamps a Gouden koffertje.</li>
 * </ul>
 * <b>A guh can never be lost.</b> Everything lives in the player's own saved data
 * ({@code PxData.deel(server, owner, "reisbureau")}: {@code Reis} = {@code Bestemming, Guh, Vertrek, Terug, Zeldzaam, Naam,
 * Looks, Opslag}), never in a block. The record is deleted only AFTER the guh is back in the world. Should the same guh
 * ever be both stored and walking around (a crash between two saves), the world wins: the trip is dropped, the guh stays
 * ({@link #controleer}). After storing and after releasing, the saved data is written to disk right away.
 */
public final class Reizen {
    public static final String DEEL = "reisbureau";
    /** Steps of the Reisagent's questline. */
    public static final int NIEUW = 0, KOFFER = 1, PROEF = 2, KLAAR = 3;
    public static final int STEMPELS_VOL = 10;
    /** How far from the balie (the player) a guh may be to be sent off. */
    public static final double BEREIK = 24;
    /** A booked trip whose guh never got stored (it unloaded, or went to the wolkjes) is dropped after this long. */
    static final long VERTREK_MAX_MS = 20_000L;
    private static final String REIS = "Reis";
    private static final String G = "gui.guhs.reisbureau.";

    public enum Uitkomst { OK, BEZIG, NIET_VANDAAG, GEEN_GUH, GUH_BEZET, EERST_AGENT, NOG_NIET, NIEMAND, AL_THUIS, GEEN_PLEK;

        public MutableComponent melding() {
            return Component.translatable(G + "melding." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    // =====================================================================================================================
    // data
    // =====================================================================================================================

    public static CompoundTag data(ServerPlayer p) {
        return PxData.deel(p, DEEL);
    }

    public static CompoundTag data(MinecraftServer server, UUID speler) {
        return PxData.deel(server, speler, DEEL);
    }

    public static int stap(ServerPlayer p) {
        return data(p).getIntOr("Stap", NIEUW);
    }

    /** Sets the questline step (it only grows, except through the dev command). */
    public static void zetStap(ServerPlayer p, int stap) {
        data(p).putInt("Stap", stap);
        PxData.vuil(p.level().getServer());
    }

    /** The running trip of this player (an attached tag), or null. */
    @Nullable
    public static CompoundTag reis(MinecraftServer server, UUID speler) {
        CompoundTag d = data(server, speler);
        return d.contains(REIS) ? d.getCompoundOrEmpty(REIS) : null;
    }

    @Nullable
    public static Bestemming bestemming(CompoundTag reis) {
        return Bestemming.vanId(reis.getStringOr("Bestemming", ""));
    }

    @Nullable
    public static UUID guhId(CompoundTag reis) {
        return reis.read("Guh", UUIDUtil.CODEC).orElse(null);
    }

    /** The guh is stored (it really left); false while it still walks off with its suitcase. */
    public static boolean isWeg(CompoundTag reis) {
        return reis.contains("Opslag");
    }

    public static boolean isTerug(CompoundTag reis) {
        return isWeg(reis) && Klok.nu() >= reis.getLongOr("Terug", 0L);
    }

    public static int aantal(CompoundTag d, String lijst, String id) {
        return d.getCompoundOrEmpty(lijst).getIntOr(id, 0);
    }

    private static void tel(CompoundTag d, String lijst, String id) {
        CompoundTag t = PxData.sub(d, lijst);
        t.putInt(id, t.getIntOr(id, 0) + 1);
    }

    /** How many of the 32 album souvenirs this player has had. */
    public static int souvenirs(CompoundTag d) {
        int n = 0;
        for (Souvenirs.Soort s : Souvenirs.ALLE) {
            if (s.souvenir() && aantal(d, "Souvenirs", s.id()) > 0) {
                n++;
            }
        }
        return n;
    }

    private static void bewaarNu(MinecraftServer server) {
        PxData.vuil(server);
        try {
            server.overworld().getDataStorage().scheduleSave();   // (a guh went into or came out of the data: on disk at once)
        } catch (RuntimeException e) {
            com.mojang.logging.LogUtils.getLogger().warn("Reisbureau: the saved data could not be written right away", e);
        }
    }

    // =====================================================================================================================
    // the daily offer
    // =====================================================================================================================

    /** Today's four trips, shortest first. */
    public static List<Bestemming> aanbod(MinecraftServer server) {
        return aanbod(server.overworld().getSeed(), Klok.dag());
    }

    /** The four trips of a real day: one per duration; every block of four days shows all four destinations of a duration. */
    public static List<Bestemming> aanbod(long seed, long dag) {
        List<Bestemming> out = new ArrayList<>();
        for (int g = 0; g < Bestemming.DUREN.length; g++) {
            long blok = Math.floorDiv(dag, 4L);
            List<Bestemming> nu = geschud(seed, blok, g);
            List<Bestemming> vorige = geschud(seed, blok - 1, g);
            if (nu.get(0) == vorige.get(3)) {
                Collections.swap(nu, 0, 1);   // (never the same destination two days in a row)
            }
            out.add(nu.get((int) Math.floorMod(dag, 4L)));
        }
        return out;
    }

    private static List<Bestemming> geschud(long seed, long blok, int groep) {
        List<Bestemming> lijst = new ArrayList<>(Bestemming.metDuur(Bestemming.DUREN[groep]));
        Collections.shuffle(lijst, new Random(seed * 31L + blok * 1_000_003L + groep * 7919L));
        return lijst;
    }

    // =====================================================================================================================
    // going
    // =====================================================================================================================

    /** Books a trip for this guh. On {@link Uitkomst#OK} the guh starts walking off with its suitcase. */
    public static Uitkomst boek(ServerPlayer p, UUID guhId, Bestemming b, @Nullable BlockPos balie) {
        MinecraftServer server = p.level().getServer();
        controleer(server, p.getUUID());
        CompoundTag d = data(p);
        if (d.contains(REIS)) {
            return Uitkomst.BEZIG;
        }
        int stap = stap(p);
        if (b.isProef() ? stap != PROEF : stap < KLAAR) {
            return Uitkomst.EERST_AGENT;
        }
        if (!b.isProef() && !aanbod(server).contains(b)) {
            return Uitkomst.NIET_VANDAAG;
        }
        GuhEntity guh = GuhKiezer.zoek(p, guhId, BEREIK);
        if (guh == null) {
            return Uitkomst.GEEN_GUH;
        }
        if (GuhKiezer.bezet(guh) != null) {
            return Uitkomst.GUH_BEZET;
        }
        long nu = Klok.nu();
        CompoundTag reis = new CompoundTag();
        reis.putString("Bestemming", b.id());
        reis.store("Guh", UUIDUtil.CODEC, guh.getUUID());
        reis.putLong("Vertrek", nu);
        reis.putLong("Terug", nu + b.duurMs());
        reis.putBoolean("Zeldzaam", !b.isProef() && p.getRandom().nextInt(100) < b.kans());
        Tekst.put(reis, "Naam", guh.getName().copy());
        reis.put("Looks", Band.looks(guh));
        d.put(REIS, reis);
        PxData.vuil(server);
        Uitzwaaien.begin(guh, p, balie);
        p.level().playSound(null, guh.blockPosition(), ReisbureauSlice.GELUID_VERTREK.get(), SoundSource.NEUTRAL, 1f, 1f);
        return Uitkomst.OK;
    }

    /**
     * The guh really leaves: it becomes data in its owner's trip record. Called by {@link Uitzwaaien} when the guh has walked
     * off (and by the tests, at once). False (the guh simply stays, without its suitcase) when its owner has no trip for it.
     */
    public static boolean opslaan(GuhEntity guh) {
        if (!(guh.level() instanceof ServerLevel level)) {
            return false;
        }
        UUID eigenaar = guh.getOwnerUUID();
        CompoundTag reis = eigenaar == null ? null : reis(level.getServer(), eigenaar);
        Bestemming b = reis == null ? null : bestemming(reis);
        Uitzwaaien.wis(guh);
        if (reis == null || b == null || isWeg(reis) || !guh.getUUID().equals(guhId(reis)) || !guh.isAlive()) {
            return false;
        }
        level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.4, guh.getZ(), 12, 0.25, 0.25, 0.25, 0.02);
        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + 0.8, guh.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
        Tekst.put(reis, "Naam", guh.getName().copy());
        reis.put("Looks", Band.looks(guh));
        CompoundTag opslag = GuhOpslag.bewaar(guh, PlekSoort.OP_VAKANTIE, plekDetail(b, reis));
        reis.put("Opslag", opslag);
        bewaarNu(level.getServer());
        return true;
    }

    private static Component plekDetail(Bestemming b, CompoundTag reis) {
        long rest = reis.getLongOr("Terug", 0L) - Klok.nu();
        return rest <= 0 ? Component.translatable(G + "plek.klaar", b.plek()) : Component.translatable(G + "plek.onderweg", b.plek(), tijd(rest));
    }

    /** "3 u 12 min", "24 uur", "45 min", "nog heel even". */
    public static Component tijd(long ms) {
        if (ms < 60_000L) {
            return Component.translatable(G + "tijd.bijna");
        }
        long min = (ms + 59_999L) / 60_000L;
        long u = min / 60, m = min % 60;
        return u == 0 ? Component.translatable(G + "tijd.m", m) : m == 0 ? Component.translatable(G + "tijd.u", u) : Component.translatable(G + "tijd.um", u, m);
    }

    // =====================================================================================================================
    // coming back
    // =====================================================================================================================

    /**
     * Collects the returned guh at {@code plek} (eerder: call it back early, without postcard and souvenir). The record is
     * removed only after the guh is back in the world.
     */
    public static Uitkomst haalOp(ServerPlayer p, Vec3 plek, boolean eerder) {
        ServerLevel level = p.level();
        MinecraftServer server = level.getServer();
        controleer(server, p.getUUID());
        CompoundTag d = data(p);
        CompoundTag reis = reis(server, p.getUUID());
        if (reis == null) {
            return Uitkomst.NIEMAND;
        }
        Bestemming b = bestemming(reis);
        UUID id = guhId(reis);
        if (!isWeg(reis)) {
            // it still walks off with its suitcase: an early call-back keeps it home
            Mob loopt = id == null ? null : Band.zoekGeladen(server, id);
            if (eerder && loopt instanceof GuhEntity guh) {
                Uitzwaaien.wis(guh);
                d.remove(REIS);
                PxData.vuil(server);
                return Uitkomst.OK;
            }
            return Uitkomst.NOG_NIET;
        }
        if (!eerder && !isTerug(reis)) {
            return Uitkomst.NOG_NIET;
        }
        if (id == null || b == null) {
            return Uitkomst.NIEMAND;
        }
        if (Band.zoekGeladen(server, id) != null) {
            d.remove(REIS);
            PxData.vuil(server);
            return Uitkomst.AL_THUIS;
        }
        Component naam = Tekst.get(reis, "Naam").copy();
        boolean zeldzaam = reis.getBooleanOr("Zeldzaam", false);
        float yaw = (float) (Math.toDegrees(Math.atan2(p.getZ() - plek.z, p.getX() - plek.x)) - 90.0);
        Entity terug = GuhOpslag.laatVrij(level, reis.getCompoundOrEmpty("Opslag"), plek, yaw);
        if (terug == null) {
            return Uitkomst.GEEN_PLEK;
        }
        d.remove(REIS);
        bewaarNu(server);
        level.sendParticles(ParticleTypes.POOF, plek.x, plek.y + 0.4, plek.z, 10, 0.25, 0.25, 0.25, 0.02);
        if (!eerder) {
            level.playSound(null, BlockPos.containing(plek), ReisbureauSlice.GELUID_TERUG.get(), SoundSource.NEUTRAL, 1f, 1f);
            beloon(p, terug, b, naam, zeldzaam);
        }
        return Uitkomst.OK;
    }

    private static void beloon(ServerPlayer p, Entity terug, Bestemming b, Component naam, boolean zeldzaam) {
        MinecraftServer server = p.level().getServer();
        CompoundTag d = data(p);
        if (terug instanceof GuhEntity guh) {
            Zonnebril.zetOp(guh);
            guh.triggerAnim("action", "happy");
            Band.geefHartjes(guh, p, 2, Reden.REIZEN);
            Dagboek.wistJeDat(guh, G + "dagboek.vakantie", b.naam());
        }
        d.putInt("Reizen", d.getIntOr("Reizen", 0) + 1);
        tel(d, "Kaarten", b.id());
        Minigames.give(p, KaartItem.maak(b, naam));
        p.sendSystemMessage(Component.translatable(G + "melding.terug", naam.copy().withStyle(ChatFormatting.GOLD), b.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (b.isProef()) {
            if (stap(p) == PROEF) {
                zetStap(p, KLAAR);
                GuhAdvancements.grant(p, "reisbureau_proefreis");
                Minigames.give(p, new ItemStack(ReisbureauSlice.STEMPEL.get()));
                p.sendSystemMessage(Component.translatable("chat.type.text", Component.translatable("entity.guhs.guh_npc.reisbureau_agent"),
                        Component.translatable("quest.guhs.reisbureau.klaar")));
            }
            PxData.vuil(server);
            return;
        }
        GuhAdvancements.grant(p, "reisbureau_eerste_reis");
        Block blok = zeldzaam ? b.zeldzaam() : b.souvenir();
        String sid = (zeldzaam ? "zeldzaam_" : "souvenir_") + b.id();
        boolean dubbel = aantal(d, "Souvenirs", sid) > 0;
        tel(d, "Souvenirs", sid);
        if (zeldzaam) {
            d.putInt("Zeldzaam", d.getIntOr("Zeldzaam", 0) + 1);
            GuhAdvancements.grant(p, "reisbureau_zeldzaam");
        }
        if (!dubbel) {
            ItemStack souvenir = new ItemStack(blok);
            p.sendSystemMessage(Component.translatable(G + (zeldzaam ? "melding.zeldzaam" : "melding.souvenir"),
                    souvenir.getHoverName().copy().withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.LIGHT_PURPLE));
            Minigames.give(p, souvenir);
            boolean alles = true;
            for (Bestemming x : Bestemming.ECHT) {
                alles &= aantal(d, "Souvenirs", "souvenir_" + x.id()) > 0;
            }
            if (alles) {
                GuhAdvancements.grant(p, "reisbureau_album");
            }
        } else {
            int stempels = d.getIntOr("Stempels", 0) + 1;
            d.putInt("StempelsTotaal", d.getIntOr("StempelsTotaal", 0) + 1);
            p.level().playSound(null, p.blockPosition(), ReisbureauSlice.GELUID_STEMPEL.get(), SoundSource.PLAYERS, 1f, 1f);
            p.sendSystemMessage(Component.translatable(G + "melding.stempel", stempels, STEMPELS_VOL).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (stempels >= STEMPELS_VOL) {
                stempels -= STEMPELS_VOL;
                d.putInt("Koffertjes", d.getIntOr("Koffertjes", 0) + 1);
                Minigames.give(p, new ItemStack(ReisbureauSlice.blok("gouden_koffertje")));
                GuhAdvancements.grant(p, "reisbureau_koffertje");
                p.sendSystemMessage(Component.translatable(G + "melding.koffertje").withStyle(ChatFormatting.GOLD));
            }
            d.putInt("Stempels", stempels);
        }
        PxData.vuil(server);
    }

    /** Where a returning guh steps out: in front of the balie, else next to the player. */
    public static Vec3 uitstapPlek(ServerLevel level, @Nullable BlockPos balie, ServerPlayer p) {
        if (balie != null && level.getBlockState(balie).getBlock() instanceof BalieBlock) {
            Direction voor = level.getBlockState(balie).getValue(BalieBlock.FACING);
            BlockPos a = balie.relative(voor);
            for (BlockPos k : List.of(a, a.relative(voor), a.relative(voor.getClockWise()), a.relative(voor.getCounterClockWise()))) {
                if (vrij(level, k)) {
                    return Vec3.atBottomCenterOf(k);
                }
            }
        }
        return p.position();
    }

    private static boolean vrij(ServerLevel level, BlockPos k) {
        BlockState onder = level.getBlockState(k.below());
        return level.getBlockState(k).getCollisionShape(level, k).isEmpty() && level.getBlockState(k.above()).getCollisionShape(level, k.above()).isEmpty()
                && onder.isFaceSturdy(level, k.below(), Direction.UP) && level.getFluidState(k).isEmpty();
    }

    // =====================================================================================================================
    // keeping things straight
    // =====================================================================================================================

    /**
     * Makes this player's trip record match the world:
     * a booked trip whose guh never got stored is finished when the guh is still around, and dropped when it is not (it
     * simply stays home); a stored guh that is ALSO loaded in the world (a crash between two saves) stays in the world and
     * the trip is dropped. Returns a message for the player when a trip was dropped.
     */
    @Nullable
    public static Component controleer(MinecraftServer server, UUID speler) {
        CompoundTag d = data(server, speler);
        CompoundTag reis = reis(server, speler);
        if (reis == null) {
            return null;
        }
        UUID id = guhId(reis);
        Mob geladen = id == null ? null : Band.zoekGeladen(server, id);
        if (isWeg(reis)) {
            if (geladen != null) {
                com.mojang.logging.LogUtils.getLogger().warn("Reisbureau: guh {} of {} is stored and in the world at once; the world wins, the trip is dropped", id, speler);
                d.remove(REIS);
                PxData.vuil(server);
                return Uitkomst.AL_THUIS.melding();
            }
            return null;
        }
        if (Klok.nu() - reis.getLongOr("Vertrek", 0L) < VERTREK_MAX_MS) {
            return null;
        }
        if (geladen instanceof GuhEntity guh && guh.isAlive() && opslaan(guh)) {
            return null;
        }
        if (geladen instanceof GuhEntity guh) {
            Uitzwaaien.wis(guh);
        }
        d.remove(REIS);
        PxData.vuil(server);
        return Component.translatable(G + "melding.geannuleerd");
    }

    /** (GuhHooks.tick, now and then per tamed guh) a guh in the world whose owner's record says it is stored: the world wins. */
    static void tickGuh(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 100 != 0 || !guh.isTame() || !(guh.level() instanceof ServerLevel level)) {
            return;
        }
        UUID eigenaar = guh.getOwnerUUID();
        if (eigenaar == null) {
            return;
        }
        CompoundTag reis = reis(level.getServer(), eigenaar);
        if (reis != null && isWeg(reis) && guh.getUUID().equals(guhId(reis))) {
            controleer(level.getServer(), eigenaar);
        }
    }

    /** (server tick) for the players who are online: tidy up, tell them when their guh is back, keep "Waar is mijn guh?" fresh. */
    static void serverTick(MinecraftServer server) {
        if (server.getTickCount() % 100 != 0) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Component gedropt = controleer(server, p.getUUID());
            if (gedropt != null) {
                p.sendSystemMessage(gedropt.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            CompoundTag reis = reis(server, p.getUUID());
            if (reis == null || !isWeg(reis)) {
                continue;
            }
            if (isTerug(reis) && !reis.getBooleanOr("Gemeld", false)) {
                reis.putBoolean("Gemeld", true);
                PxData.vuil(server);
                meldTerug(p, reis);
            }
            if (server.getTickCount() % 1200 == 0 || isTerug(reis) != reis.getBooleanOr("PlekKlaar", false)) {
                reis.putBoolean("PlekKlaar", isTerug(reis));
                verversPlek(server, p.getUUID(), reis);
            }
        }
    }

    /** (login) "your guh is back and waits at a Reisbalie". */
    static void login(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        CompoundTag reis = reis(server, p.getUUID());
        if (reis != null && isTerug(reis)) {
            reis.putBoolean("Gemeld", true);
            PxData.vuil(server);
            meldTerug(p, reis);
        }
    }

    private static void meldTerug(ServerPlayer p, CompoundTag reis) {
        p.sendSystemMessage(Component.translatable(G + "melding.staat_klaar", Tekst.get(reis, "Naam").copy().withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().playSound(null, p.blockPosition(), ReisbureauSlice.GELUID_BALIE.get(), SoundSource.PLAYERS, 0.7f, 1f);
    }

    /** "Mijn guhs": "Op vakantie in <plek> (terug over ...)" with the time that is left now. */
    static void verversPlek(MinecraftServer server, UUID eigenaar, CompoundTag reis) {
        UUID id = guhId(reis);
        Bestemming b = bestemming(reis);
        if (id == null || b == null) {
            return;
        }
        BandData.Rec rec = BandData.get(server).vind(eigenaar, id);
        Plek oud = rec == null ? null : rec.plek;
        if (oud == null || oud.soort() != PlekSoort.OP_VAKANTIE) {
            return;
        }
        GuhVolger.zet(eigenaar, id, new Plek(PlekSoort.OP_VAKANTIE, oud.dim(), oud.pos(), plekDetail(b, reis), oud.tijd()));
    }

    // =====================================================================================================================
    // the screen
    // =====================================================================================================================

    /** Opens (or refreshes) the trip screen for this player at this balie. */
    public static void open(ServerPlayer p, BlockPos pos, @Nullable Component melding) {
        ModNetworking.sendTo(p, new ReisbureauPayloads.Open(stand(p, pos, melding)));
    }

    /** Everything the screen shows: the offer, who is away, the guhs to pick from, the reispas. */
    public static CompoundTag stand(ServerPlayer p, BlockPos pos, @Nullable Component melding) {
        MinecraftServer server = p.level().getServer();
        Component gedropt = controleer(server, p.getUUID());
        CompoundTag d = data(p);
        int stap = stap(p);
        CompoundTag out = new CompoundTag();
        out.putLong("Pos", pos.asLong());
        out.putInt("Stap", stap);
        out.putLong("Morgen", Klok.totMorgen());
        ListTag aanbod = new ListTag();
        if (stap == PROEF) {
            aanbod.add(reisTag(d, Bestemming.OM_DE_HOEK, true));
        }
        for (Bestemming b : aanbod(server)) {
            aanbod.add(reisTag(d, b, stap >= KLAAR));
        }
        out.put("Aanbod", aanbod);
        CompoundTag reis = reis(server, p.getUUID());
        if (reis != null) {
            CompoundTag r = new CompoundTag();
            r.putString("Bestemming", reis.getStringOr("Bestemming", ""));
            r.putLong("Duur", Math.max(1L, reis.getLongOr("Terug", 0L) - reis.getLongOr("Vertrek", 0L)));
            r.putLong("Rest", Math.max(0L, reis.getLongOr("Terug", 0L) - Klok.nu()));
            r.putBoolean("Weg", isWeg(reis));
            r.putBoolean("Klaar", isTerug(reis));
            Tekst.put(r, "Naam", Tekst.get(reis, "Naam"));
            r.put("Looks", reis.getCompoundOrEmpty("Looks").copy());
            out.put("Reis", r);
        } else {
            ListTag guhs = GuhKiezer.lijst(p, rec -> rec.plek.soort() != PlekSoort.OP_VAKANTIE && rec.plek.soort() != PlekSoort.OP_KANTOOR);
            for (Tag raw : guhs) {
                CompoundTag t = (CompoundTag) raw;
                UUID id = t.read("Id", UUIDUtil.CODEC).orElse(null);
                GuhEntity guh = id == null ? null : GuhKiezer.zoek(p, id, BEREIK);
                Component uit = guh == null ? Component.translatable(G + "uit.ver") : GuhKiezer.bezet(guh);
                if (uit != null) {
                    Tekst.put(t, "Uit", uit);
                }
            }
            out.put("Guhs", guhs);
        }
        out.putInt("Stempels", d.getIntOr("Stempels", 0));
        out.putInt("Reizen", d.getIntOr("Reizen", 0));
        out.putInt("Souvenirs", souvenirs(d));
        out.putInt("Koffertjes", d.getIntOr("Koffertjes", 0));
        Component m = melding != null ? melding : gedropt;
        if (m != null) {
            Tekst.put(out, "Melding", m);
        }
        return out;
    }

    private static CompoundTag reisTag(CompoundTag d, Bestemming b, boolean open) {
        CompoundTag t = new CompoundTag();
        t.putString("Id", b.id());
        t.putInt("Minuten", b.minuten());
        t.putInt("Kans", b.kans());
        t.putBoolean("Proef", b.isProef());
        t.putBoolean("Open", open);
        if (!b.isProef()) {
            t.putString("Souvenir", BuiltInRegistries.BLOCK.getKey(b.souvenir()).toString());
            t.putString("Zeldzaam", BuiltInRegistries.BLOCK.getKey(b.zeldzaam()).toString());
            t.putBoolean("HebS", aantal(d, "Souvenirs", "souvenir_" + b.id()) > 0);
            t.putBoolean("HebZ", aantal(d, "Souvenirs", "zeldzaam_" + b.id()) > 0);
        } else {
            t.putString("Souvenir", BuiltInRegistries.ITEM.getKey(b.kaart()).toString());
        }
        return t;
    }

    /** (ReisbureauPayloads.Actie) what the player clicked; everything is checked here. */
    static void opActie(ServerPlayer p, BlockPos pos, int actie, String bestemmingId, String guhTekst) {
        ServerLevel level = p.level();
        boolean bijBalie = level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof BalieBlock
                && p.distanceToSqr(Vec3.atCenterOf(pos)) <= 8 * 8;
        if (!bijBalie) {
            if (actie == ReisbureauPayloads.VERVERS) {
                open(p, pos, null);
            }
            return;
        }
        Component melding = null;
        switch (actie) {
            case ReisbureauPayloads.BOEK -> {
                Bestemming b = Bestemming.vanId(bestemmingId);
                UUID guhId;
                try {
                    guhId = UUID.fromString(guhTekst);
                } catch (IllegalArgumentException e) {
                    guhId = null;
                }
                if (b == null || guhId == null) {
                    return;
                }
                GuhEntity guh = GuhKiezer.zoek(p, guhId, BEREIK);
                Component naam = guh == null ? Component.empty() : guh.getName().copy();
                Component bezet = guh == null ? null : GuhKiezer.bezet(guh);
                Uitkomst u = boek(p, guhId, b, pos);
                melding = switch (u) {
                    case OK -> Component.translatable(G + "melding.geboekt", naam, b.naam());
                    case GUH_BEZET -> Component.translatable(G + "melding.guh_bezet", bezet == null ? Component.empty() : bezet);
                    default -> u.melding();
                };
            }
            case ReisbureauPayloads.OPHALEN, ReisbureauPayloads.EERDER -> {
                boolean eerder = actie == ReisbureauPayloads.EERDER;
                CompoundTag reis = reis(level.getServer(), p.getUUID());
                Component naam = reis == null ? Component.empty() : Tekst.get(reis, "Naam").copy();
                Bestemming b = reis == null ? null : bestemming(reis);
                if (eerder && reis != null && isTerug(reis)) {
                    eerder = false;   // (it is back already: the full welcome)
                }
                Uitkomst u = haalOp(p, uitstapPlek(level, pos, p), eerder);
                melding = u != Uitkomst.OK ? u.melding() : eerder ? Component.translatable(G + "melding.eerder", naam)
                        : Component.translatable(G + "melding.terug", naam, b == null ? Component.empty() : b.naam());
            }
            default -> {
            }
        }
        open(p, pos, melding);
    }

    private Reizen() {
    }
}
