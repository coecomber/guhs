package nl.juiced.guhs.feature.guhpixel.grap1;

import java.util.List;
import java.util.UUID;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.Arena;
import nl.juiced.guhs.feature.guhpixel.ArenaSoort;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.PxGeluid;
import nl.juiced.guhs.feature.guhpixel.SessieStart;
import nl.juiced.guhs.feature.guhpixel.SpelSoort;
import nl.juiced.guhs.feature.guhpixel.Vertrek;
import nl.juiced.guhs.quest.Scorebord;

/**
 * Skyblok: "the" classic sky island (an L of dirt, a tree, a chest with ice and a lava bucket, a bed) inside a box of
 * light blue terracotta with painted white clouds, seams, a ladder and a staff door in the sky. A guide counts "Stap 1 van
 * 4.812: maak een cobblestone generator", but nothing works: no breaking, no placing, the bucket stays full. The only
 * thing that works is lying in the bed: "SKYBLOK UITGESPEELD!", fireworks and a credit roll.
 * <p>
 * Steps of the mini questline: 1 arrive, 2 open the start chest, 3 try to make a cobblestone generator, 4 give up and
 * lie down. Lying down ends the game at any step (a speedrun is allowed).
 * Geometry = tools/features/guhpixel_grap1_bouw.py (skyblok); the game tests check the template against these constants.
 */
public final class SkyblokSessie extends GrapSessie {
    public static final String ID = "skyblok";
    public static final int STAPPEN = 4;
    public static final Vec3i MAAT = new Vec3i(27, 24, 27);
    /** Template coordinates: the bed (foot, head), the start chest, the staff door (lower half), the island's top layer. */
    public static final BlockPos BED_VOET = new BlockPos(12, 11, 10), BED_HOOFD = new BlockPos(13, 11, 10), KIST = new BlockPos(15, 11, 11),
            DEUR = new BlockPos(21, 16, 1);
    public static final int EILAND_Y = 10;
    public static final ArenaSoort ARENA = new ArenaSoort(ID, Guhs.id("guhpixel/skyblok_eiland"), MAAT, new Vec3(11.5, 11, 12.5), -90f, false,
            SkyblokSessie::herstel);
    public static final SpelSoort SPEL = new SpelSoort(ID, ARENA, 1, 1, LobbyPlek.SPEL_SKYBLOK, SkyblokSessie::new);
    /** How many steps the guide says there are. */
    public static final String GIDS_STAPPEN = "4.812";
    static final int NAAR_STAP_2 = 60, KIST_WACHT = 20 * 30, POGING_WACHT = 20 * 25, HINT_ELKE = 20 * 20, BENEDEN_MAX = 20 * 15;
    /** The finale: when the credit cards start after lying down, ticks per card, how many cards. */
    static final int AFTITELING_NA = 100, KAART_TICKS = 36, KAARTEN = 7;
    private static final int[] NEE = {1, 2, 3, 4, 5, 6};

    private final ServerBossEvent gids = new ServerBossEvent(UUID.randomUUID(), gidsTekst(false), BossEvent.BossBarColor.WHITE,
            BossEvent.BossBarOverlay.PROGRESS);
    private int stap3Sinds = -1, pogingen, beneden, finaleSinds = -1, laatsteHint;
    private boolean leegteGezegd;

    SkyblokSessie(SessieStart start) {
        super(start, ID);
    }

    private static Component gidsTekst(boolean klaar) {
        return Component.translatable(klaar ? "gui.guhs.skyblok.handleiding.klaar" : "gui.guhs.skyblok.handleiding.balk", GIDS_STAPPEN)
                .withStyle(klaar ? ChatFormatting.GREEN : ChatFormatting.WHITE);
    }

    static void herstel(Arena a) {
        bedVrij(a, BED_VOET, BED_HOOFD);
        if (a.level().getBlockEntity(a.wereld(KIST.getX(), KIST.getY(), KIST.getZ())) instanceof ChestBlockEntity kist) {
            kist.clearContent();
        }
    }

    public int pogingen() {
        return pogingen;
    }

    public boolean inFinale() {
        return finaleSinds >= 0;
    }

    @Override
    protected void begin() {
        ServerPlayer p = speler();
        Arena a = arena();
        if (level().getBlockEntity(a.wereld(KIST.getX(), KIST.getY(), KIST.getZ())) instanceof ChestBlockEntity kist) {
            kist.clearContent();
            kist.setItem(12, new ItemStack(Items.ICE));
            kist.setItem(14, new ItemStack(Items.LAVA_BUCKET));
        }
        Scorebord.show(level(), Vec3.atCenterOf(a.wereld(DEUR.getX(), DEUR.getY() + 2, DEUR.getZ() + 1)), "skyblok_deur",
                Component.translatable("sign.guhs.skyblok.personeel").withStyle(ChatFormatting.YELLOW));
        Scorebord.show(level(), Vec3.atCenterOf(a.wereld(13, MAAT.getY() - 3, 13)), "skyblok_zon",
                Component.translatable("sign.guhs.skyblok.zon").withStyle(ChatFormatting.GOLD));
        gids.setProgress(1f / 4812f);
        if (p != null) {
            gids.addPlayer(p);
            stap(p, 1);
            zeg(p, "gui.guhs.skyblok.begin");
        }
    }

    @Override
    protected void tick() {
        if (klaarTick()) {
            finaleTick();
            return;
        }
        ServerPlayer p = speler();
        if (p == null) {
            return;
        }
        if (ligt(p)) {
            finale(p);
            return;
        }
        int t = ticks();
        if (stap() == 1 && t >= NAAR_STAP_2) {
            stap(p, 2);
        } else if (stap() == 2 && t >= NAAR_STAP_2 + KIST_WACHT) {
            naarStap3(p);
        } else if (stap() == 3 && t - stap3Sinds >= POGING_WACHT) {
            stap(p, 4);
            laatsteHint = t;
        } else if (stap() == 4 && t - laatsteHint >= HINT_ELKE) {
            laatsteHint = t;
            balk(p, "gui.guhs.skyblok.hint.bed");
        }
        // the painted "void" under the island is a floor: have a look around, then back up
        if (p.getY() < arena().oorsprong().getY() + EILAND_Y - 3 && p.onGround()) {
            if (!leegteGezegd) {
                leegteGezegd = true;
                zeg(p, "gui.guhs.skyblok.leegte");
            }
            if (++beneden >= BENEDEN_MAX) {
                terugOpEiland(p, "gui.guhs.skyblok.leegte.terug");
            }
        } else if (p.getY() >= arena().oorsprong().getY() + EILAND_Y) {
            beneden = 0;
        }
    }

    private void naarStap3(ServerPlayer p) {
        if (stap() < 3) {
            stap(p, 3);
            stap3Sinds = ticks();
        }
    }

    private void terugOpEiland(ServerPlayer p, String key) {
        Vec3 naar = arena().start();
        p.teleportTo(level(), naar.x, naar.y, naar.z, java.util.Set.of(), ARENA.startYaw(), 0f, true);
        p.resetFallDistance();
        beneden = 0;
        balk(p, key);
    }

    /** Something the player tried did not work (of course): a joke, and the questline moves on to "give up". */
    private void poging(ServerPlayer p, String key) {
        pogingen++;
        balk(p, key);
        if (stap() < 3) {
            naarStap3(p);
        } else if (stap() == 3 && pogingen >= 2) {
            stap(p, 4);
            laatsteHint = ticks();
        }
    }

    private void pogingZomaar(ServerPlayer p) {
        poging(p, "gui.guhs.skyblok.nee." + NEE[pogingen % NEE.length]);
    }

    @Override
    protected boolean klik(ServerPlayer p, BlockPos pos, BlockState s) {
        if (s.is(Blocks.CHEST)) {
            if (stap() < 3) {
                stap(p, 2);
                naarStap3(p);
            }
            return true;
        }
        if (s.getBlock() instanceof DoorBlock) {
            poging(p, "gui.guhs.skyblok.nee.deur");
            if (p.getY() > arena().oorsprong().getY() + EILAND_Y + 3) {
                terugOpEiland(p, "gui.guhs.skyblok.nee.deur");
            }
            return false;
        }
        if (s.is(Blocks.CRAFTING_TABLE)) {
            poging(p, "gui.guhs.skyblok.nee.werkbank");
        }
        return false;
    }

    /** The player used an item (Grap1Slice cancels it): the bucket stays full, the ice stays in the paw. */
    void itemGebruik(ServerPlayer p, ItemStack stack) {
        if (afgelopen || stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof BucketItem) {
            // the client already poured it: show the real blocks and the real bucket again
            Vec3 oog = p.getEyePosition();
            BlockHitResult hit = level().clip(new ClipContext(oog, oog.add(p.getLookAngle().scale(6)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
            if (hit.getType() == HitResult.Type.BLOCK && p.connection != null) {
                p.connection.send(new ClientboundBlockUpdatePacket(level(), hit.getBlockPos()));
                p.connection.send(new ClientboundBlockUpdatePacket(level(), hit.getBlockPos().relative(hit.getDirection())));
            }
            p.inventoryMenu.sendAllDataToRemote();
            poging(p, "gui.guhs.skyblok.nee.lava");
        } else if (stack.is(Items.ICE)) {
            poging(p, "gui.guhs.skyblok.nee.ijs");
        } else {
            pogingZomaar(p);
        }
    }

    @Override
    public boolean magBreken(ServerPlayer p, BlockPos pos, BlockState s) {
        if (!afgelopen) {
            boolean lucht = s.is(Blocks.LIGHT_BLUE_TERRACOTTA) || s.is(Blocks.WHITE_CONCRETE) || s.is(Blocks.CYAN_TERRACOTTA);
            poging(p, lucht ? "gui.guhs.skyblok.nee.lucht" : s.is(net.minecraft.tags.BlockTags.LOGS) ? "gui.guhs.skyblok.nee.boom" : "gui.guhs.skyblok.nee.breken");
        }
        return false;
    }

    @Override
    public boolean magPlaatsen(ServerPlayer p, BlockPos pos, BlockState s) {
        if (!afgelopen) {
            poging(p, s.is(Blocks.ICE) ? "gui.guhs.skyblok.nee.ijs" : "gui.guhs.skyblok.nee.plaatsen");
        }
        return false;
    }

    // --- the finale ---------------------------------------------------------------------------------------------------------

    private void finale(ServerPlayer p) {
        finaleSinds = ticks();
        stap(p, STAPPEN);
        gids.setName(gidsTekst(true));
        gids.setColor(BossEvent.BossBarColor.GREEN);
        gids.setProgress(1f);
        clou(p, Component.translatable("gui.guhs.skyblok.clou.onder"));
        level().playSound(null, p.blockPosition(), Grap1Slice.SKYBLOK_UITGESPEELD.get(), SoundSource.PLAYERS, 1f, 1f);
        zeg(p, "gui.guhs.skyblok.einde");
        straksKlaar(AFTITELING_NA + KAARTEN * KAART_TICKS + 30);
    }

    private void finaleTick() {
        ServerPlayer p = speler();
        if (p == null || finaleSinds < 0) {
            return;
        }
        int t = ticks() - finaleSinds;
        if (t < AFTITELING_NA && t % 12 == 2) {
            vuurwerk(t / 12);
        }
        int k = t - AFTITELING_NA;
        if (k >= 0 && k % KAART_TICKS == 0 && k / KAART_TICKS < KAARTEN) {
            int i = k / KAART_TICKS + 1;
            PxGeluid.titel(p, Component.translatable("gui.guhs.skyblok.aftiteling." + i + ".rol").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.guhs.skyblok.aftiteling." + i + ".naam").withStyle(ChatFormatting.LIGHT_PURPLE), KAART_TICKS - 16);
        }
    }

    private void vuurwerk(int i) {
        ServerLevel level = level();
        Arena a = arena();
        double hoek = i * 2.4;
        Vec3 pos = a.wereld(new Vec3(13.5 + Math.cos(hoek) * 6, EILAND_Y + 3, 13.5 + Math.sin(hoek) * 6));
        ItemStack pijl = new ItemStack(Items.FIREWORK_ROCKET);
        FireworkExplosion.Shape[] vormen = FireworkExplosion.Shape.values();
        pijl.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(new FireworkExplosion(vormen[i % vormen.length],
                IntList.of(0xFF7FB6, 0xFFD27A, 0x8FD0FF), IntList.of(0xFFFFFF), true, i % 2 == 0))));
        level.addFreshEntity(new FireworkRocketEntity(level, pos.x, pos.y, pos.z, pijl));
        level.sendParticles(ParticleTypes.HEART, pos.x, pos.y + 1, pos.z, 4, 0.6, 0.6, 0.6, 0.02);
    }

    @Override
    protected void spelerWeg(ServerPlayer p, Vertrek reden) {
        super.spelerWeg(p, reden);
        gids.removePlayer(p);
    }

    @Override
    protected void einde() {
        gids.removeAllPlayers();
    }
}
