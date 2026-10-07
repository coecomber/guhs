package nl.juiced.guhs.feature.guhpixel.grap1;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.guhpixel.Arena;
import nl.juiced.guhs.feature.guhpixel.ArenaSoort;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.PxGeluid;
import nl.juiced.guhs.feature.guhpixel.SessieStart;
import nl.juiced.guhs.feature.guhpixel.SpelSoort;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.quest.Scorebord;

/**
 * Bedwars: your little team island with THE bed in the middle, four tiny enemy islands around it. "Verdedig je bed!":
 * you defend it by lying in it. The four enemy team guhs (red, blue, green and yellow nightcaps) slowly bridge over with
 * wool in their colour, wait politely at the edge of your island until you lie down, and then come and lie around your
 * bed. End screen: "BED VERDEDIGD: bedden vernield: 0, dutjes: 5".
 * <p>
 * Steps: 1 arrive on your island, 2 defend your bed (lie down), 3 stay in bed while the teams bridge over, 4 make room:
 * they only want to join. The guhs never hurt or even shove: getting out of bed only makes them wait.
 * Geometry = tools/features/guhpixel_grap1_bouw.py (bedwars); the game tests check the template against these constants.
 */
public final class BedwarsSessie extends GrapSessie {
    public static final String ID = "bedwars";
    public static final int STAPPEN = 4;
    public static final Vec3i MAAT = new Vec3i(53, 22, 53);
    /** The top layer of every island (template y); things stand on EILAND_Y + 1. */
    public static final int EILAND_Y = 8, MIDDEN = 26, BRUG_LENGTE = 13;
    public static final BlockPos BED_VOET = new BlockPos(MIDDEN, EILAND_Y + 1, MIDDEN), BED_HOOFD = new BlockPos(MIDDEN, EILAND_Y + 1, MIDDEN - 1);
    public static final ArenaSoort ARENA = new ArenaSoort(ID, Guhs.id("guhpixel/bedwars_eilanden"), MAAT, new Vec3(26.5, EILAND_Y + 1, 28.5), 180f, false,
            BedwarsSessie::herstel);
    public static final SpelSoort SPEL = new SpelSoort(ID, ARENA, 1, 1, LobbyPlek.SPEL_BEDWARS, BedwarsSessie::new);
    /** When the bridging starts, and how long the end screen stays before the player goes back. */
    static final int BRUG_START = 80, EIND_TICKS = 160, HINT_ELKE = 20 * 8;

    /** One enemy team: where its island lies seen from the middle (dx, dz), its colour, how fast it bridges. */
    public enum Team {
        ROOD("rood", 0, -1, Blocks.RED_WOOL, GuhClothes.BEDWARS_SLAAPMUTS_ROOD, ChatFormatting.RED, 20, 0),
        BLAUW("blauw", 1, 0, Blocks.BLUE_WOOL, GuhClothes.BEDWARS_SLAAPMUTS_BLAUW, ChatFormatting.BLUE, 24, 7),
        GROEN("groen", 0, 1, Blocks.LIME_WOOL, GuhClothes.BEDWARS_SLAAPMUTS_GROEN, ChatFormatting.GREEN, 28, 13),
        GEEL("geel", -1, 0, Blocks.YELLOW_WOOL, GuhClothes.BEDWARS_SLAAPMUTS_GEEL, ChatFormatting.YELLOW, 32, 19);

        public final String id;
        public final int dx, dz;
        public final Block wol;
        public final GuhClothes muts;
        public final ChatFormatting kleur;
        /** Ticks per block of bridge, and how many ticks after {@link #BRUG_START} this team starts. */
        public final int tempo, later;

        Team(String id, int dx, int dz, Block wol, GuhClothes muts, ChatFormatting kleur, int tempo, int later) {
            this.id = id;
            this.dx = dx;
            this.dz = dz;
            this.wol = wol;
            this.muts = muts;
            this.kleur = kleur;
            this.tempo = tempo;
            this.later = later;
        }

        public Component naam() {
            return Component.translatable("entity.guhs.bedwars_teamguh." + id).withStyle(kleur);
        }

        /** Bridge cell i (0 = next to the team's island, BRUG_LENGTE - 1 = next to yours), template coordinates. */
        public BlockPos brug(int i) {
            int afstand = 17 - i;   // (the team island's edge is 18 from the middle, yours 4)
            return new BlockPos(MIDDEN + dx * afstand, EILAND_Y, MIDDEN + dz * afstand);
        }

        /** Where the guh stands (template, feet) at this distance from the middle of the arena. */
        Vec3 op(double afstand) {
            return new Vec3(MIDDEN + 0.5 + dx * afstand, EILAND_Y + 1, MIDDEN + 0.5 + dz * afstand);
        }

        /** The team's own island (its middle). */
        public Vec3 thuis() {
            return op(20);
        }

        /** Where the guh lies down: beside the bed, on its own side. */
        public Vec3 bedplek() {
            // the bed lies from z 25 (head) to z 27, x 26..27
            return switch (this) {
                case ROOD -> new Vec3(26.5, EILAND_Y + 1, 24.3);
                case BLAUW -> new Vec3(27.8, EILAND_Y + 1, 26.5);
                case GROEN -> new Vec3(26.5, EILAND_Y + 1, 27.7);
                case GEEL -> new Vec3(25.2, EILAND_Y + 1, 26.5);
            };
        }
    }

    public enum Fase { THUIS, BRUGT, WACHT, LOOPT, SLAAPT }

    /** A team in this game. */
    public static final class Ploeg {
        public final Team team;
        @Nullable
        TeamGuhEntity guh;
        public int gelegd;
        public Fase fase = Fase.THUIS;

        Ploeg(Team team) {
            this.team = team;
        }

        @Nullable
        public TeamGuhEntity guh() {
            return guh;
        }
    }

    private final List<Ploeg> ploegen = new ArrayList<>();
    private boolean heeftGelegen;
    private int laatsteHint, wol;

    BedwarsSessie(SessieStart start) {
        super(start, ID);
        for (Team t : Team.values()) {
            ploegen.add(new Ploeg(t));
        }
    }

    public List<Ploeg> ploegen() {
        return ploegen;
    }

    public int dutjes() {
        int n = 0;
        for (Ploeg pl : ploegen) {
            if (pl.fase == Fase.SLAAPT) {
                n++;
            }
        }
        return n;
    }

    /** Takes every bridge away again and frees the bed. */
    static void herstel(Arena a) {
        bedVrij(a, BED_VOET, BED_HOOFD);
        for (Team t : Team.values()) {
            for (int i = 0; i < BRUG_LENGTE; i++) {
                BlockPos l = t.brug(i);
                BlockPos pos = a.wereld(l.getX(), l.getY(), l.getZ());
                if (!a.level().getBlockState(pos).isAir()) {
                    a.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    @Override
    protected void begin() {
        herstel(arena());
        ServerLevel level = level();
        for (Ploeg pl : ploegen) {
            TeamGuhEntity guh = Grap1Slice.TEAMGUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (guh == null) {
                continue;
            }
            Vec3 thuis = arena().wereld(pl.team.thuis());
            float yaw = (float) (Math.toDegrees(Math.atan2(pl.team.dx, -pl.team.dz)));   // (looks at the middle)
            guh.snapTo(thuis.x, thuis.y, thuis.z, yaw, 0f);
            guh.setYHeadRot(yaw);
            guh.setYBodyRot(yaw);
            guh.setVariant(GuhVariant.NORMAL);
            guh.setPersonality(GuhPersonality.LAZY);
            guh.wear(pl.team.muts);
            guh.setCustomName(pl.team.naam());
            guh.setCustomNameVisible(true);
            guh.setInvulnerable(true);
            guh.setPersistenceRequired();
            level.addFreshEntity(guh);
            pl.guh = guh;
        }
        Scorebord.show(level, Vec3.atCenterOf(arena().wereld(MIDDEN + 3, EILAND_Y + 3, MIDDEN + 3)), "bedwars_generator",
                Component.translatable("sign.guhs.bedwars.generator").withStyle(ChatFormatting.GRAY));
        ServerPlayer p = speler();
        if (p != null) {
            stap(p, 1);
            zeg(p, "gui.guhs.bedwars.begin");
        }
    }

    @Override
    protected void tick() {
        if (klaarTick()) {
            return;
        }
        ServerPlayer p = speler();
        if (p == null) {
            return;
        }
        int t = ticks();
        boolean ligt = ligt(p);
        if (t == BRUG_START) {
            stap(p, 2);
            PxGeluid.titel(p, Component.translatable("gui.guhs.bedwars.titel.verdedig").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.translatable("gui.guhs.bedwars.titel.verdedig.onder").withStyle(ChatFormatting.LIGHT_PURPLE), 50);
        }
        if (ligt && !heeftGelegen) {
            heeftGelegen = true;
            stap(p, 2);
            stap(p, 3);
            zeg(p, "gui.guhs.bedwars.ligt");
        }
        boolean allenWachten = true;
        for (Ploeg pl : ploegen) {
            tickPloeg(pl, p, t, ligt);
            allenWachten &= pl.fase == Fase.WACHT;
        }
        if (dutjes() == ploegen.size() && ligt) {
            eindscherm(p);
            return;
        }
        if (t - laatsteHint >= HINT_ELKE && t > BRUG_START) {
            if (!ligt && heeftGelegen) {
                laatsteHint = t;
                balk(p, "gui.guhs.bedwars.hint.terug");
            } else if (!ligt && allenWachten) {
                laatsteHint = t;
                balk(p, "gui.guhs.bedwars.hint.wachten");
            } else if (!ligt && t > BRUG_START + 200) {
                laatsteHint = t;
                balk(p, "gui.guhs.bedwars.hint.bed");
            }
        }
    }

    private void tickPloeg(Ploeg pl, ServerPlayer p, int t, boolean ligt) {
        TeamGuhEntity guh = pl.guh;
        if (guh == null || guh.isRemoved()) {
            return;
        }
        Team team = pl.team;
        Arena a = arena();
        switch (pl.fase) {
            case THUIS -> {
                if (t >= BRUG_START + team.later) {
                    pl.fase = Fase.BRUGT;
                }
            }
            case BRUGT -> {
                int sinds = t - BRUG_START - team.later;
                if (pl.gelegd < BRUG_LENGTE && sinds % team.tempo == 0) {
                    BlockPos l = team.brug(pl.gelegd);
                    BlockPos pos = a.wereld(l.getX(), l.getY(), l.getZ());
                    level().setBlock(pos, team.wol.defaultBlockState(), 3);
                    level().playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 0.9f + pl.gelegd * 0.02f);
                    pl.gelegd++;
                    wol++;
                }
                // the guh stands on the newest block; with the last block it steps onto your island
                double afstand = pl.gelegd >= BRUG_LENGTE ? 4 : 18 - pl.gelegd;
                if (loop(guh, a.wereld(team.op(afstand)), 0.9) && pl.gelegd >= BRUG_LENGTE) {
                    pl.fase = Fase.WACHT;
                    zeg(p, "gui.guhs.bedwars.aangekomen", team.naam());
                }
            }
            case WACHT -> {
                if (ligt) {
                    pl.fase = Fase.LOOPT;
                } else {
                    guh.getLookControl().setLookAt(p, 30f, 30f);
                }
            }
            case LOOPT -> {
                if (!ligt) {
                    guh.getLookControl().setLookAt(p, 30f, 30f);   // (waits politely until you are back in bed)
                } else if (loop(guh, a.wereld(team.bedplek()), 0.8)) {
                    slaap(pl, guh, p);
                }
            }
            case SLAAPT -> {
                if ((t + team.ordinal() * 9) % 50 == 0) {
                    level().sendParticles(nl.juiced.guhs.feature.emotes.EmotesFeature.GUH_ZZZ.get(), guh.getX(), guh.getY() + 0.9, guh.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
                }
            }
        }
        // fell off its own bridge (somebody pushed): back on it, nothing lost
        if (guh.getY() < a.oorsprong().getY() + EILAND_Y - 1) {
            Vec3 terug = a.wereld(pl.fase == Fase.BRUGT || pl.fase == Fase.THUIS ? team.op(pl.fase == Fase.THUIS ? 20 : 18 - pl.gelegd) : team.op(4));
            guh.snapTo(terug.x, terug.y, terug.z, guh.getYRot(), 0f);
            guh.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Walks the guh straight to this spot; true when it is there. */
    private static boolean loop(TeamGuhEntity guh, Vec3 naar, double tempo) {
        double dx = naar.x - guh.getX(), dz = naar.z - guh.getZ();
        if (dx * dx + dz * dz < 0.12) {
            return true;
        }
        guh.getMoveControl().setWantedPosition(naar.x, naar.y, naar.z, tempo);
        return false;
    }

    private void slaap(Ploeg pl, TeamGuhEntity guh, ServerPlayer p) {
        pl.fase = Fase.SLAAPT;
        guh.getNavigation().stop();
        guh.getMoveControl().setWantedPosition(guh.getX(), guh.getY(), guh.getZ(), 0.0);   // (stand still: walking would end the nap)
        guh.setDeltaMovement(Vec3.ZERO);
        if (!guh.emotes.start(Emote.SLAPEN, true, GuhEmotes.Source.SELF)) {
            guh.setInSittingPose(true);
        }
        GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, true);
        level().sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + 1, guh.getZ(), 3, 0.3, 0.2, 0.3, 0.02);
        stap(p, 4);
        zeg(p, "gui.guhs.bedwars.dutje", pl.team.naam(), dutjes() + 1);
    }

    private void eindscherm(ServerPlayer p) {
        stap(p, STAPPEN);
        clou(p, Component.translatable("gui.guhs.bedwars.clou.onder", 0, dutjes() + 1));
        level().playSound(null, p.blockPosition(), Grap1Slice.BEDWARS_VERDEDIGD.get(), SoundSource.PLAYERS, 1f, 1f);
        Component streep = Component.translatable("gui.guhs.bedwars.eind.streep").withStyle(ChatFormatting.GOLD);
        p.sendSystemMessage(streep);
        p.sendSystemMessage(Component.translatable("gui.guhs.bedwars.eind.kop").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.translatable("gui.guhs.bedwars.eind.vernield", 0).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(Component.translatable("gui.guhs.bedwars.eind.dutjes", dutjes() + 1).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(Component.translatable("gui.guhs.bedwars.eind.wol", wol).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(Component.translatable("gui.guhs.bedwars.eind.beste").withStyle(ChatFormatting.LIGHT_PURPLE));
        p.sendSystemMessage(streep);
        straksKlaar(EIND_TICKS);
    }

    @Override
    public boolean magEntiteit(ServerPlayer p, Entity e) {
        if (e instanceof TeamGuhEntity guh && !afgelopen) {
            for (Ploeg pl : ploegen) {
                if (pl.guh == guh) {
                    balk(p, pl.fase == Fase.SLAAPT ? "gui.guhs.bedwars.guh.slaapt" : "gui.guhs.bedwars.guh.njeg", pl.team.naam());
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean magBreken(ServerPlayer p, BlockPos pos, BlockState s) {
        if (!afgelopen) {
            balk(p, s.is(net.minecraft.tags.BlockTags.BEDS) ? "gui.guhs.bedwars.nee.bed" : "gui.guhs.bedwars.nee.breken");
        }
        return false;
    }

    @Override
    protected void einde() {
        for (Ploeg pl : ploegen) {
            if (pl.guh != null) {
                pl.guh.discard();
            }
        }
    }
}
