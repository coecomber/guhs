package nl.juiced.guhs.feature.grotspellen;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Schatgraven with Kapitein Schepsnuit: 90 seconds to dig up the treasures under a soft field of pluiswol and viltkiezel
 * ({@link SchatVeld}: the field is {@value SchatVeld#GROOTTE} wide; makkelijk plays the middle 7, medium 11, lastig all
 * 15). Snuffie the snuffelguh hops along with you and sniffs: koud, warm, HEET (then he jumps); on the harder levels
 * he sniffs less often and "warm" reaches less far. A golden cheese is a bonus, a piepkaas costs 10 seconds. Find every
 * treasure before the time is up and the seconds left count too. After every game the field is filled in again.
 */
public final class Schatgraven extends GrotSpel {
    /** The Snuffies that belong to a game right now (any other one tidies itself away). */
    static final Set<UUID> HULPJES = ConcurrentHashMap.newKeySet();
    public static final String SNUFFIE_TAG = "guhs_snuffie";

    /** The field's low corner (x, z) and the y of its blocks. */
    @Nullable
    private BlockPos hoek;
    @Nullable
    private SchatVeld veld;
    @Nullable
    private UUID snuffie;
    private int snuif;
    @Nullable
    private SchatVeld.Hint laatsteHint;

    Schatgraven(UUID npcId) {
        super(npcId);
    }

    @Override
    public Soort soort() {
        return Soort.SCHATGRAVEN;
    }

    @Nullable
    public SchatVeld veld() {
        return veld;
    }

    @Nullable
    public BlockPos hoek() {
        return hoek;
    }

    @Nullable
    public SchatVeld.Hint laatsteHint() {
        return laatsteHint;
    }

    // --- the hall ---------------------------------------------------------------------------------------------------
    @Override
    protected boolean zoek(ServerLevel world, GuhNpcEntity npc) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE, y = Integer.MIN_VALUE, n = 0;
        for (BlockPos pos : inDeVloer(world, zaalLucht(world, npc), GrotSpellenFeature.SCHATGROND.get())) {
            {
                if (y == Integer.MIN_VALUE) {
                    y = pos.getY();
                }
                if (pos.getY() != y) {
                    continue;
                }
                minX = Math.min(minX, pos.getX());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxZ = Math.max(maxZ, pos.getZ());
                n++;
            }
        }
        if (n == 0 || maxX - minX + 1 != SchatVeld.GROOTTE || maxZ - minZ + 1 != SchatVeld.GROOTTE) {
            return false;
        }
        hoek = new BlockPos(minX, y, minZ);
        return true;
    }

    @Override
    protected boolean heel(ServerLevel world) {
        return hoek != null && world.getBlockState(hoek).is(GrotSpellenFeature.SCHATGROND.get())
                && world.getBlockState(hoek.offset(SchatVeld.GROOTTE - 1, 0, SchatVeld.GROOTTE - 1)).is(GrotSpellenFeature.SCHATGROND.get());
    }

    private BlockPos cel(int x, int z) {
        return hoek.offset(x, 0, z);
    }

    // --- a game -----------------------------------------------------------------------------------------------------
    @Override
    protected int speelTicks() {
        return SchatVeld.SPEELTICKS;
    }

    @Override
    protected void begin(ServerLevel world, ServerPlayer p, GuhNpcEntity npc) {
        veld = new SchatVeld(niveau, world.getRandom());
        for (int x = 0; x < SchatVeld.GROOTTE; x++) {
            for (int z = 0; z < SchatVeld.GROOTTE; z++) {
                zet(world, x, z, veld.actief(x, z) ? GrotSpellenBlocks.Staat.DICHT : GrotSpellenBlocks.Staat.RUST);
            }
        }
        // you start at the edge of the field, in the middle, looking across it (the side nearest to the host)
        int mid = SchatVeld.GROOTTE / 2;
        BlockPos[] randen = {cel(mid, -1), cel(mid, SchatVeld.GROOTTE), cel(-1, mid), cel(SchatVeld.GROOTTE, mid)};
        float[] kijk = {0f, 180f, -90f, 90f};
        int beste = 0;
        for (int i = 1; i < randen.length; i++) {
            if (randen[i].distSqr(npc.blockPosition()) < randen[beste].distSqr(npc.blockPosition())) {
                beste = i;
            }
        }
        BlockPos start = randen[beste];
        p.teleportTo(world, start.getX() + 0.5, start.getY() + 1.0, start.getZ() + 0.5, Set.of(), kijk[beste], 20f, true);
        maakSnuffie(world, start);
        snuif = SchatVeld.hintTicks(niveau);
        laatsteHint = null;
    }

    private void maakSnuffie(ServerLevel world, BlockPos bij) {
        weg(world);
        GuhNpcEntity hulp = ModEntities.GUH_NPC.get().create(world, EntitySpawnReason.TRIGGERED);
        if (hulp == null) {
            return;
        }
        hulp.setKind(GuhNpcEntity.Kind.SNUFFELHULPJE);
        hulp.addTag(SNUFFIE_TAG);
        hulp.snapTo(bij.getX() + 1.5, bij.getY() + 1.0, bij.getZ() + 0.5, 0f, 0f);
        world.addFreshEntity(hulp);
        snuffie = hulp.getUUID();
        HULPJES.add(snuffie);
    }

    private void weg(ServerLevel world) {
        if (snuffie != null) {
            HULPJES.remove(snuffie);
            if (world.getEntity(snuffie) instanceof GuhNpcEntity hulp) {
                hulp.discard();
            }
            snuffie = null;
        }
    }

    private void zet(ServerLevel world, int x, int z, GrotSpellenBlocks.Staat staat) {
        BlockPos pos = cel(x, z);
        BlockState s = world.getBlockState(pos);
        if (s.is(GrotSpellenFeature.SCHATGROND.get()) && s.getValue(GrotSpellenBlocks.STAAT) != staat) {
            world.setBlock(pos, s.setValue(GrotSpellenBlocks.STAAT, staat), 2);
        }
    }

    @Override
    protected void speelTick(ServerLevel world, ServerPlayer p, GuhNpcEntity npc) {
        if (veld == null || hoek == null || --snuif > 0) {
            return;
        }
        snuif = SchatVeld.hintTicks(niveau);
        double fx = p.getX() - hoek.getX(), fz = p.getZ() - hoek.getZ();
        if (fx < -1.5 || fz < -1.5 || fx > SchatVeld.GROOTTE + 1.5 || fz > SchatVeld.GROOTTE + 1.5) {
            flits(p, Component.translatable("gui.guhs.schatgraven.snuffie.veld").withStyle(ChatFormatting.GRAY));
            return;
        }
        SchatVeld.Hint hint = veld.hint(fx, fz);
        laatsteHint = hint;
        GuhNpcEntity hulp = snuffie != null && world.getEntity(snuffie) instanceof GuhNpcEntity h ? h : null;
        Vec3 kijk = p.getLookAngle().multiply(1, 0, 1);
        kijk = kijk.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : kijk.normalize();
        Vec3 naast = p.position().add(kijk.yRot((float) Math.toRadians(70)).scale(1.3));
        if (hulp == null) {
            maakSnuffie(world, BlockPos.containing(naast).below());
            hulp = snuffie != null && world.getEntity(snuffie) instanceof GuhNpcEntity h ? h : null;
        }
        if (hulp != null) {
            hulp.teleportTo(naast.x, p.getY() + 0.05, naast.z);
            hulp.setYRot(p.getYRot());
            hulp.setYHeadRot(p.getYRot());
        }
        float toon;
        ChatFormatting kleur;
        switch (hint) {
            case HEET -> {
                toon = 2.0f;
                kleur = ChatFormatting.RED;
                if (hulp != null) {
                    hulp.setDeltaMovement(0, 0.45, 0);
                    hulp.hurtMarked = true;
                    world.sendParticles(ParticleTypes.HEART, hulp.getX(), hulp.getY() + 0.8, hulp.getZ(), 3, 0.2, 0.2, 0.2, 0);
                }
            }
            case WARM -> {
                toon = 1.3f;
                kleur = ChatFormatting.GOLD;
            }
            default -> {
                toon = 0.6f;
                kleur = ChatFormatting.AQUA;
            }
        }
        world.playSound(null, hulp != null ? hulp.blockPosition() : p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7f, toon);
        flits(p, Component.translatable("gui.guhs.schatgraven.snuffie." + hint.name().toLowerCase(java.util.Locale.ROOT)).withStyle(kleur, ChatFormatting.BOLD));
    }

    /** The shovel on a block: dig it, if it's a closed cell of your field in this game. */
    public static void graaf(ServerPlayer p, BlockPos pos) {
        if (spelVan(p) instanceof Schatgraven spel) {
            spel.graafHier(p, pos);
        }
    }

    /** Digs the cell at pos; returns what was under it (null: nothing dug). */
    @Nullable
    public SchatVeld.Inhoud graafHier(ServerPlayer p, BlockPos pos) {
        ServerLevel world = p.level();
        if (fase != Fase.SPELEN || veld == null || hoek == null || pos.getY() != hoek.getY()) {
            return null;
        }
        int x = pos.getX() - hoek.getX(), z = pos.getZ() - hoek.getZ();
        if (x < 0 || z < 0 || x >= SchatVeld.GROOTTE || z >= SchatVeld.GROOTTE || p.distanceToSqr(Vec3.atCenterOf(pos)) > 36) {
            return null;
        }
        if (!veld.actief(x, z)) {
            flits(p, Component.translatable("gui.guhs.schatgraven.rust").withStyle(ChatFormatting.GRAY));
            return null;
        }
        SchatVeld.Inhoud wat = veld.graaf(x, z);
        if (wat == null) {
            return null;
        }
        zet(world, x, z, GrotSpellenBlocks.Staat.van(wat));
        Vec3 m = Vec3.atCenterOf(pos).add(0, 0.3, 0);
        world.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1f, 0.9f + world.getRandom().nextFloat() * 0.3f);
        world.sendParticles(ParticleTypes.CLOUD, m.x, m.y, m.z, 4, 0.25, 0.1, 0.25, 0.01);
        switch (wat) {
            case SCHAT -> {
                world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
                world.sendParticles(ParticleTypes.WAX_ON, m.x, m.y + 0.3, m.z, 12, 0.3, 0.3, 0.3, 0.05);
                flits(p, Component.translatable("gui.guhs.schatgraven.schat", SchatVeld.SCHAT_PUNTEN, veld.gevonden(), veld.totaal())
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
            case GOUD -> {
                world.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.6f);
                world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, m.x, m.y + 0.3, m.z, 20, 0.3, 0.4, 0.3, 0.2);
                flits(p, Component.translatable("gui.guhs.schatgraven.goud", SchatVeld.GOUD_PUNTEN).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            }
            case PIEP -> {
                over = Math.max(1, over - SchatVeld.PIEP_STRAF);
                world.playSound(null, pos, SoundEvents.BAT_AMBIENT, SoundSource.PLAYERS, 1f, 1.9f);
                world.sendParticles(ParticleTypes.ANGRY_VILLAGER, m.x, m.y + 0.3, m.z, 3, 0.2, 0.2, 0.2, 0);
                flits(p, Component.translatable("gui.guhs.schatgraven.piep", SchatVeld.PIEP_STRAF / 20).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
            default -> flits(p, Component.translatable("gui.guhs.schatgraven.leeg." + world.getRandom().nextInt(4)).withStyle(ChatFormatting.GRAY));
        }
        return wat;
    }

    @Override
    protected boolean klaar() {
        return veld != null && veld.alles();
    }

    @Override
    protected Component balk() {
        return Component.translatable("gui.guhs.schatgraven.balk", veld == null ? 0 : veld.gevonden(), veld == null ? 0 : veld.totaal(), klok(), punten())
                .withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    protected int punten() {
        return veld == null ? 0 : veld.punten(over);
    }

    @Override
    protected int munten(boolean record) {
        return SchatVeld.munten(niveau, punten(), record);
    }

    @Override
    protected List<Component> uitslagRegels() {
        if (veld == null) {
            return List.of();
        }
        return List.of(Component.translatable("quest.guhs.schatgraven.uitslag", veld.gevonden(), veld.totaal(), veld.gevonden() * SchatVeld.SCHAT_PUNTEN),
                Component.translatable(veld.goud() ? "quest.guhs.schatgraven.uitslag_goud" : "quest.guhs.schatgraven.uitslag_geen_goud", SchatVeld.GOUD_PUNTEN),
                Component.translatable("quest.guhs.schatgraven.uitslag_piep", veld.piepen()),
                veld.alles() ? Component.translatable("quest.guhs.schatgraven.uitslag_tijd", Math.max(0, over) / 40)
                        : Component.translatable("quest.guhs.schatgraven.uitslag_niet_alles"));
    }

    /** The field filled in again (every cell closed), Snuffie home. */
    @Override
    protected void opruimen(ServerLevel world) {
        weg(world);
        if (hoek != null && heel(world)) {
            for (int x = 0; x < SchatVeld.GROOTTE; x++) {
                for (int z = 0; z < SchatVeld.GROOTTE; z++) {
                    zet(world, x, z, GrotSpellenBlocks.Staat.DICHT);
                }
            }
        }
        veld = null;
    }

    @Override
    protected List<ItemStack> leen() {
        return List.of(new ItemStack(GrotSpellenFeature.SCHATSCHEPJE.get()));
    }

    /** Every few seconds: a Snuffie without a game goes home. */
    @Override
    protected void rustTick(ServerLevel world, GuhNpcEntity npc) {
        if (npc.tickCount % 40 != 7) {
            return;
        }
        for (GuhNpcEntity hulp : world.getEntitiesOfClass(GuhNpcEntity.class, npc.getBoundingBox().inflate(SCAN, 12, SCAN),
                e -> e.getKind() == GuhNpcEntity.Kind.SNUFFELHULPJE && !HULPJES.contains(e.getUUID()))) {
            hulp.discard();
        }
    }

    /** (tests) This field instead of a random one (the blocks follow). */
    public void testVeld(ServerLevel world, SchatVeld nieuw) {
        veld = nieuw;
        for (int x = 0; x < SchatVeld.GROOTTE; x++) {
            for (int z = 0; z < SchatVeld.GROOTTE; z++) {
                zet(world, x, z, nieuw.actief(x, z) ? GrotSpellenBlocks.Staat.DICHT : GrotSpellenBlocks.Staat.RUST);
            }
        }
    }

    /** (tests) Snuffie sniffs now. */
    public void testSnuif() {
        snuif = 1;
    }

    @Nullable
    public UUID snuffie() {
        return snuffie;
    }
}
