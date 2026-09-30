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
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.utils.Utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MainClass.MODID)
public final class SpaceTransitionHandler {

    public static final float ESCAPE_ALTITUDE = 550.0f;
    public static final float REENTRY_ALTITUDE = 490.0f;
    public static final int TELEPORT_COOLDOWN_TICKS = 80;
    public static final String COOLDOWN_TAG = "tsp_teleport_cooldown";
    public static final String LAST_PLANET_X_TAG = "tsp_last_p_x";
    public static final String LAST_PLANET_Z_TAG = "tsp_last_p_z";
    public static final String LAST_PLANET_DIM_TAG = "tsp_last_p_dim";
    public static double ATMOSPHERE_ENTRY_PERCENT = 0.0;
    public static double SURFACE_ENTRY_FACTOR = 1.0;
    public static double SPACE_SPAWN_BUFFER = 0.01;

    private SpaceTransitionHandler() {
    }

    public static double getEntryRadius(CelestialJsonLoader.BodySpatialInfo bodyInfo) {
        double physRadius = bodyInfo.physicalRadius();
        var body = bodyInfo.body();
        boolean hasAtmosphere = body != null && body.atmosphere != null && body.atmosphere.enabled;

        if (hasAtmosphere && ATMOSPHERE_ENTRY_PERCENT > 0.0) {
            double visualRadius = bodyInfo.visualRadius();
            double targetRadius = physRadius + (visualRadius - physRadius) * Utils.clamp(ATMOSPHERE_ENTRY_PERCENT, 0.0, 1.0);
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

        if (living.isVehicle() || living.getY() >= ESCAPE_ALTITUDE || CelestialJsonLoader.isSpaceDimension(living.level().dimension().location())) {
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
        boolean inSpace = CelestialJsonLoader.isSpaceDimension(currentDim);
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

        if (!CelestialJsonLoader.isBodyDimension(currentDim.toString())) {
            return;
        }

        CelestialJsonLoader.BodySpatialInfo bodyInfo = CelestialJsonLoader.getBodyByDimension(currentDim.toString(), now);
        if (bodyInfo == null) {
            return;
        }

        entity.getPersistentData().putDouble(LAST_PLANET_X_TAG, entity.getX());
        entity.getPersistentData().putDouble(LAST_PLANET_Z_TAG, entity.getZ());
        entity.getPersistentData().putString(LAST_PLANET_DIM_TAG, currentDim.toString());

        String targetSpaceDim = CelestialJsonLoader.getSpaceDimensionForBodyDimension(currentDim.toString());
        ResourceLocation spaceDimLoc = ResourceLocation.tryParse(targetSpaceDim);
        if (spaceDimLoc == null) {
            spaceDimLoc = new ResourceLocation(MainClass.MODID, "space");
        }
        ResourceKey<Level> spaceDimKey = ResourceKey.create(Registries.DIMENSION, spaceDimLoc);
        ServerLevel spaceLevel = server.getLevel(spaceDimKey);
        if (spaceLevel == null) {
            return;
        }

        Vec3 planetPos = bodyInfo.spacePosition();
        double spawnRadius = getSpaceSpawnRadius(bodyInfo);

        Vec3 lookAngle = entity.getLookAngle();
        Vec3 unitSkyDir = lookAngle.lengthSqr() > 1e-4 ? lookAngle.normalize() : new Vec3(0.0, 1.0, 0.0);

        SolarSystemData system = bodyInfo.system();
        PlanetInstance.Config bodyConfig = bodyInfo.body();
        Map<String, Vec3> bodyPositions = CelestialJsonLoader.calculateBodyPositions(system, now, false);
        float sunAngle = currentLevel.getSunAngle(1.0f);

        Quaternionf celestialToSky = computeCelestialToSky(system, bodyConfig, bodyPositions, sunAngle);
        Quaternionf skyToCelestial = new Quaternionf(celestialToSky).invert();

        Vector3f skyDirV = new Vector3f((float) unitSkyDir.x, (float) unitSkyDir.y, (float) unitSkyDir.z);
        Vector3f spaceDirV = skyToCelestial.transform(skyDirV, new Vector3f()).normalize();
        Vec3 unitSpaceDir = new Vec3(spaceDirV.x(), spaceDirV.y(), spaceDirV.z());

        Vec3 spawnPos = planetPos.add(unitSpaceDir.scale(spawnRadius));

        Vec3 curVel = entity.getDeltaMovement();
        double speed = Utils.clamp(curVel.length() > 0.05 ? curVel.length() * 0.5 : 0.25, 0.15, 1.2);
        Vec3 spaceVel = unitSpaceDir.scale(speed);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-unitSpaceDir.x, unitSpaceDir.z));
        float targetPitch = (float) Math.toDegrees(-Math.asin(Utils.clamp(unitSpaceDir.y, -1.0, 1.0)));

        SpaceTeleporter teleporter = new SpaceTeleporter(spawnPos, spaceVel, targetYaw, targetPitch, true);
        teleportEntityTree(entity, spaceLevel, teleporter);
    }

    private static Quaternionf computeCelestialToSky(SolarSystemData system, PlanetInstance.Config currentBody, Map<String, Vec3> bodyPositions, float sunAngle) {
        Vec3 currentBodyPos = bodyPositions.getOrDefault(currentBody.id, new Vec3(system.originX, system.originY, system.originZ));

        Vec3 starPos = new Vec3(system.originX, system.originY, system.originZ);
        if (system.star != null && system.star.enabled) {
            String starId = (system.star.id != null && !system.star.id.isBlank()) ? system.star.id : "sun";
            starPos = bodyPositions.getOrDefault(starId, starPos);
        }

        Vector3f sunDirBase = new Vector3f(-(float) Math.sin(sunAngle), (float) Math.cos(sunAngle), 0.0f).normalize();
        Quaternionf skyTilt = new Quaternionf().rotationX((float) Math.toRadians(-32.5f));

        Vector3f starDirection = new Vector3f(
                (float) (starPos.x - currentBodyPos.x),
                (float) (starPos.y - currentBodyPos.y),
                (float) (starPos.z - currentBodyPos.z)
        );

        return new Quaternionf(skyTilt).mul(buildCelestialToSky(starDirection, sunDirBase));
    }

    private static Quaternionf buildCelestialToSky(Vector3f starDirection, Vector3f sunDirMC) {
        if (starDirection.lengthSquared() <= 1.0e-8F) {
            return new Quaternionf();
        }

        Vector3f fromU = new Vector3f(starDirection).normalize();
        Vector3f fromN = new Vector3f(0.0F, 1.0F, 0.0F);
        fromN.sub(new Vector3f(fromU).mul(fromN.dot(fromU)));
        if (fromN.lengthSquared() <= 1.0e-8F) {
            return rotationTo(starDirection, sunDirMC);
        }
        fromN.normalize();
        Vector3f fromV = new Vector3f(fromU).cross(fromN).normalize();

        Vector3f toU = new Vector3f(sunDirMC).normalize();
        Vector3f toN = new Vector3f(0.0F, 0.0F, 1.0F);
        Vector3f toV = new Vector3f(toU).cross(toN).normalize();

        Matrix3f from = new Matrix3f(fromU, fromN, fromV);
        Matrix3f to = new Matrix3f(toU, toN, toV);
        Matrix3f rotation = new Matrix3f(to).mul(new Matrix3f(from).transpose());

        return new Quaternionf().setFromNormalized(rotation).normalize();
    }

    private static Quaternionf rotationTo(Vector3f from, Vector3f to) {
        if (from.lengthSquared() <= 1.0e-8F || to.lengthSquared() <= 1.0e-8F) {
            return new Quaternionf();
        }

        Vector3f source = new Vector3f(from).normalize();
        Vector3f target = new Vector3f(to).normalize();
        float dot = source.dot(target);

        if (dot > 0.999999F) {
            return new Quaternionf();
        }

        if (dot < -0.999999F) {
            Vector3f axis = Math.abs(source.x()) < 0.9F
                    ? new Vector3f(1.0F, 0.0F, 0.0F)
                    : new Vector3f(0.0F, 1.0F, 0.0F);
            axis.cross(source).normalize();
            return new Quaternionf().rotationAxis((float) Math.PI, axis.x(), axis.y(), axis.z());
        }

        return new Quaternionf().rotationTo(source, target);
    }

    private static void handleSpaceToPlanet(Entity entity, ServerLevel spaceLevel, MinecraftServer server, Instant now) {

        String currentSpaceDimId = spaceLevel.dimension().location().toString();
        List<CelestialJsonLoader.BodySpatialInfo> spaceBodies = CelestialJsonLoader.getDimensionBodiesInSpace(currentSpaceDimId, now);
        if (spaceBodies.isEmpty()) {
            return;
        }

        Entity rootEntity = entity.getRootVehicle();
        Vec3 entityPos = rootEntity.position();

        for (CelestialJsonLoader.BodySpatialInfo bodyInfo : spaceBodies) {
            double entryRadius = getEntryRadius(bodyInfo);
            double distSq = entityPos.distanceToSqr(bodyInfo.spacePosition());

            if (distSq <= entryRadius * entryRadius) {

                ServerLevel targetLevel = null;
                if (bodyInfo.dimension() != null && !bodyInfo.dimension().isBlank()) {
                    ResourceLocation targetDimLoc = ResourceLocation.tryParse(bodyInfo.dimension());
                    if (targetDimLoc != null) {
                        ResourceKey<Level> targetDimKey = ResourceKey.create(Registries.DIMENSION, targetDimLoc);
                        targetLevel = server.getLevel(targetDimKey);
                    }
                }

                if (targetLevel == null) {
                    Vec3 planetPos = bodyInfo.spacePosition();
                    Vec3 pushDir = entityPos.subtract(planetPos);
                    double len = pushDir.length();

                    if (len < 0.0001) {
                        pushDir = new Vec3(0, 1, 0);
                        len = 1.0;
                    }

                    double safeRadius = entryRadius + 1.0;
                    Vec3 safePos = planetPos.add(pushDir.scale(safeRadius / len));
                    if (rootEntity instanceof ServerPlayer player) {
                        player.teleportTo(safePos.x, safePos.y, safePos.z);
                    } else {
                        rootEntity.teleportTo(safePos.x, safePos.y, safePos.z);
                    }

                    Vec3 bounceVel = pushDir.scale(0.8 / len);
                    rootEntity.setDeltaMovement(bounceVel);
                    rootEntity.hurtMarked = true;

                    break;
                }

                double entryX;
                double entryZ;
                String targetDimStr = bodyInfo.dimension();
                String lastDimStr = entity.getPersistentData().getString(LAST_PLANET_DIM_TAG);
                if (targetDimStr != null && targetDimStr.equalsIgnoreCase(lastDimStr)
                        && entity.getPersistentData().contains(LAST_PLANET_X_TAG)
                        && entity.getPersistentData().contains(LAST_PLANET_Z_TAG)) {
                    entryX = entity.getPersistentData().getDouble(LAST_PLANET_X_TAG);
                    entryZ = entity.getPersistentData().getDouble(LAST_PLANET_Z_TAG);
                } else {
                    Vec3 dir = entityPos.subtract(bodyInfo.spacePosition());
                    double dirLen = Math.max(0.001, Math.sqrt(dir.x * dir.x + dir.z * dir.z));
                    entryX = (dir.x / dirLen) * 500.0 + targetLevel.getSharedSpawnPos().getX();
                    entryZ = (dir.z / dirLen) * 500.0 + targetLevel.getSharedSpawnPos().getZ();
                }

                entity.getPersistentData().remove(LAST_PLANET_X_TAG);
                entity.getPersistentData().remove(LAST_PLANET_Z_TAG);
                entity.getPersistentData().remove(LAST_PLANET_DIM_TAG);

                entity.resetFallDistance();

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
}