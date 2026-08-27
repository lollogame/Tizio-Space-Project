package tizio.dev.tsp.core.handlers.teleport;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.data.CelestialJsonLoader;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MainClass.MODID)
public final class SpaceTransitionHandler {

    public static final float ESCAPE_ALTITUDE = 550.0f;
    public static final float REENTRY_ALTITUDE = 490.0f;
    public static final int TELEPORT_COOLDOWN_TICKS = 80;

    /**
     * Where in the atmosphere the transition triggers (for planets with atmosphere):
     *  0.0 = At the planet SURFACE (ground level, exact physical surface).
     *  0.5 = Halfway through the atmosphere.
     *  1.0 = At the top / outer edge of the visual atmosphere.
     */
    public static double ATMOSPHERE_ENTRY_PERCENT = 0.0;

    /**
     * Multiplier on the planet surface radius (proportional to every planet/moon):
     *  1.0 = Exactly at the visual surface (touching surface).
     *  0.98 = Slightly clipped into the surface (2% inside).
     */
    public static double SURFACE_ENTRY_FACTOR = 1.0;


    public static double SPACE_SPAWN_BUFFER = 0.01;

    public static final String COOLDOWN_TAG = "tsp_teleport_cooldown";
    public static final String LAST_PLANET_X_TAG = "tsp_last_p_x";
    public static final String LAST_PLANET_Z_TAG = "tsp_last_p_z";

    private SpaceTransitionHandler() {}


    public static double getEntryRadius(CelestialJsonLoader.BodySpatialInfo bodyInfo) {
        double physRadius = bodyInfo.physicalRadius();
        var body = bodyInfo.body();
        boolean hasAtmosphere = body != null && body.atmosphere != null && body.atmosphere.enabled;

        if (hasAtmosphere && ATMOSPHERE_ENTRY_PERCENT > 0.0) {
            double visualRadius = bodyInfo.visualRadius();
            double targetRadius = physRadius + (visualRadius - physRadius) * Math.min(1.0, Math.max(0.0, ATMOSPHERE_ENTRY_PERCENT));
            return targetRadius * SURFACE_ENTRY_FACTOR;
        }

        return physRadius * SURFACE_ENTRY_FACTOR;
    }

    public static double getSpaceSpawnRadius(CelestialJsonLoader.BodySpatialInfo bodyInfo) {
        return bodyInfo.visualRadius() + bodyInfo.physicalRadius() * SPACE_SPAWN_BUFFER;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.player;

        int cd = player.getPersistentData().getInt(COOLDOWN_TAG);
        if (cd > 0) {
            player.getPersistentData().putInt(COOLDOWN_TAG, cd - 1);
            return;
        }

        if (player.isPassenger()) {
            Entity root = player.getRootVehicle();
            if (root != null && root.getPersistentData().getInt(COOLDOWN_TAG) > 0) {
                return;
            }
        }

        checkTransition(player);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide() || living instanceof ServerPlayer) {
            return;
        }

        int cd = living.getPersistentData().getInt(COOLDOWN_TAG);
        if (cd > 0) {
            living.getPersistentData().putInt(COOLDOWN_TAG, cd - 1);
            return;
        }

        if (living.isVehicle() || living.getY() >= ESCAPE_ALTITUDE || (MainClass.MODID+":space").equals(living.level().dimension().location().toString())) {
            checkTransition(living);
        }
    }

    public static void checkTransition(Entity entity) {
        if (entity == null || entity.level().isClientSide() || !entity.isAlive()) {
            return;
        }

        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return;
        }

        ResourceLocation currentDim = serverLevel.dimension().location();
        boolean inSpace = (MainClass.MODID+":space").equals(currentDim.toString());
        Instant now = Instant.now();

        if (inSpace) {
            handleSpaceToPlanet(entity, serverLevel, server, now);
        } else {
            handlePlanetToSpace(entity, serverLevel, server, now, currentDim);
        }
    }

    private static void handlePlanetToSpace(Entity entity, ServerLevel currentLevel, MinecraftServer server, Instant now, ResourceLocation currentDim) {
        if (entity.getY() < ESCAPE_ALTITUDE) {
            return;
        }

        entity.getPersistentData().putDouble(LAST_PLANET_X_TAG, entity.getX());
        entity.getPersistentData().putDouble(LAST_PLANET_Z_TAG, entity.getZ());

        ResourceKey<Level> spaceDimKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MainClass.MODID, "space"));
        ServerLevel spaceLevel = server.getLevel(spaceDimKey);
        if (spaceLevel == null) {
            return;
        }

        CelestialJsonLoader.BodySpatialInfo bodyInfo = CelestialJsonLoader.getBodyByDimension(currentDim.toString(), now);

        Vec3 spawnPos;
        Vec3 spaceVel;
        if (bodyInfo != null) {
            Vec3 planetPos = bodyInfo.spacePosition();
            double spawnRadius = getSpaceSpawnRadius(bodyInfo);

            Vec3 lookAngle = entity.getLookAngle();
            Vec3 unitDir = lookAngle.lengthSqr() > 1e-4 ? lookAngle.normalize() : new Vec3(0.0, 1.0, 0.0);

            spawnPos = planetPos.add(unitDir.scale(spawnRadius));

            Vec3 curVel = entity.getDeltaMovement();
            double speed = Math.max(0.15, Math.min(1.2, curVel.length() > 0.05 ? curVel.length() * 0.5 : 0.25));
            spaceVel = unitDir.scale(speed);
        } else {
            spawnPos = new Vec3(0.0, 500.0, 0.0);
            Vec3 curVel = entity.getDeltaMovement();
            spaceVel = new Vec3(curVel.x * 0.5, Math.max(0.1, Math.min(1.0, curVel.y * 0.5)), curVel.z * 0.5);
        }

        SpaceTeleporter teleporter = new SpaceTeleporter(spawnPos, spaceVel, entity.getYRot(), entity.getXRot(), true);
        teleportEntityTree(entity, spaceLevel, teleporter);
    }

    private static void handleSpaceToPlanet(Entity entity, ServerLevel spaceLevel, MinecraftServer server, Instant now) {
        List<CelestialJsonLoader.BodySpatialInfo> dimensionBodies = CelestialJsonLoader.getDimensionBodiesInSpace(now);
        if (dimensionBodies.isEmpty()) {
            return;
        }

        Vec3 entityPos = entity.position();

        for (CelestialJsonLoader.BodySpatialInfo bodyInfo : dimensionBodies) {
            double entryRadius = getEntryRadius(bodyInfo);
            double distSq = entityPos.distanceToSqr(bodyInfo.spacePosition());

            if (distSq <= entryRadius * entryRadius) {
                ResourceLocation targetDimLoc = ResourceLocation.tryParse(bodyInfo.dimension());
                if (targetDimLoc == null) {
                    continue;
                }

                ResourceKey<Level> targetDimKey = ResourceKey.create(Registries.DIMENSION, targetDimLoc);
                ServerLevel targetLevel = server.getLevel(targetDimKey);
                if (targetLevel == null) {
                    continue;
                }

                double entryX;
                double entryZ;
                if (entity.getPersistentData().contains(LAST_PLANET_X_TAG) && entity.getPersistentData().contains(LAST_PLANET_Z_TAG)) {
                    entryX = entity.getPersistentData().getDouble(LAST_PLANET_X_TAG);
                    entryZ = entity.getPersistentData().getDouble(LAST_PLANET_Z_TAG);
                } else {
                    Vec3 dir = entityPos.subtract(bodyInfo.spacePosition());
                    double dirLen = Math.max(0.001, Math.sqrt(dir.x * dir.x + dir.z * dir.z));
                    entryX = (dir.x / dirLen) * 500.0 + targetLevel.getSharedSpawnPos().getX();
                    entryZ = (dir.z / dirLen) * 500.0 + targetLevel.getSharedSpawnPos().getZ();
                }

                Vec3 spawnPos = new Vec3(entryX, REENTRY_ALTITUDE, entryZ);
                Vec3 entryVel = new Vec3(entity.getDeltaMovement().x * 0.3, -0.75, entity.getDeltaMovement().z * 0.3);

                SpaceTeleporter teleporter = new SpaceTeleporter(spawnPos, entryVel, entity.getYRot(), entity.getXRot(), true);
                teleportEntityTree(entity, targetLevel, teleporter);
                break;
            }
        }
    }

    public static void teleportEntityTree(Entity entity, ServerLevel destLevel, SpaceTeleporter teleporter) {
        Entity root = entity.getRootVehicle();
        List<Entity> passengers = new ArrayList<>(root.getPassengers());

        root.getPersistentData().putInt(COOLDOWN_TAG, TELEPORT_COOLDOWN_TICKS);
        for (Entity p : passengers) {
            p.getPersistentData().putInt(COOLDOWN_TAG, TELEPORT_COOLDOWN_TICKS);
        }

        if (passengers.isEmpty()) {
            root.changeDimension(destLevel, teleporter);
        } else {
            root.ejectPassengers();
            Entity newRoot = root.changeDimension(destLevel, teleporter);
            if (newRoot != null) {
                for (Entity p : passengers) {
                    Entity newP = p.changeDimension(destLevel, teleporter);
                    if (newP != null) {
                        newP.startRiding(newRoot, true);
                    }
                }
            }
        }
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
