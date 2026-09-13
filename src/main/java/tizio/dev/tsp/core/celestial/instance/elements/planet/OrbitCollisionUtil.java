package tizio.dev.tsp.core.celestial.instance.elements.planet;

import tizio.dev.tsp.config.DataConfig;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OrbitCollisionUtil {

    private static final double DEFAULT_STAR_RADIUS = 1500.0D;
    private static final double MIN_SEPARATION_MARGIN = 50.0D;

    private OrbitCollisionUtil() {}

    public static double getPhysicalClearanceRadius(PlanetInstance.Config body) {
        if (body == null) return 0.0D;
        double bodyRadius = DataConfig.Body.toBlocks(body.radius);
        double radius = Math.max((double) DataConfig.Body.MIN_PHYSICAL_RADIUS, bodyRadius);
        if (body.atmosphere != null && body.atmosphere.enabled) {
            double thickness = body.atmosphere.thickness <= 2.0F ? bodyRadius * body.atmosphere.thickness : body.atmosphere.thickness;
            radius = Math.max(radius, bodyRadius + Math.max(0.0D, thickness));
        }
        if (body.ring != null && body.ring.enabled) {
            double ringOuter = body.ring.outerRadius <= 15.0F ? body.ring.outerRadius * bodyRadius : body.ring.outerRadius;
            radius = Math.max(radius, ringOuter);
        }
        return radius;
    }

    public static double getSystemClearanceRadius(SolarSystemData system, PlanetInstance.Config body) {
        double baseRadius = getPhysicalClearanceRadius(body);
        if (system == null || body == null) return baseRadius;

        List<PlanetInstance.Config> moons = system.getMoonsOf(body.id);
        double maxMoonExtent = baseRadius;
        for (PlanetInstance.Config moon : moons) {
            if (moon != null && moon.orbit != null && moon.orbit.enabled) {
                double moonClearance = getPhysicalClearanceRadius(moon);
                maxMoonExtent = Math.max(maxMoonExtent, DataConfig.Orbit.toBlocks(moon.orbit.radius) + moonClearance);
            }
        }
        return maxMoonExtent;
    }

    public static double getParentClearanceRadius(SolarSystemData system, PlanetInstance.Config body) {
        if (system == null || body == null) return DEFAULT_STAR_RADIUS;
        if (body.parentId != null && !body.parentId.isBlank()) {
            PlanetInstance.Config parentBody = system.findBody(body.parentId);
            if (parentBody != null) {
                return getPhysicalClearanceRadius(parentBody);
            }
        }
        if (system.star != null) {
            return Math.max((double) DataConfig.Body.MIN_PHYSICAL_RADIUS, (double) DataConfig.Star.toBlocks(system.star.radius));
        }
        return DEFAULT_STAR_RADIUS;
    }

    public static double getMinimumSafeOrbitRadius(SolarSystemData system, PlanetInstance.Config body) {
        double parentClearance = getParentClearanceRadius(system, body);
        double bodyClearance = getPhysicalClearanceRadius(body);
        double margin = Math.max(MIN_SEPARATION_MARGIN, bodyClearance * 0.1D);
        return DataConfig.Orbit.normalize(parentClearance + bodyClearance + margin);
    }

    public static double getMaximumSafeOrbitRadius(SolarSystemData system, PlanetInstance.Config body) {
        if (system == null || body == null) {
            return DataConfig.Orbit.RADIUS.max();
        }
        if (body.isMoon() && body.parentId != null) {
            PlanetInstance.Config parentBody = system.findBody(body.parentId);
            if (parentBody != null && parentBody.orbit != null) {
                double parentOrbit = DataConfig.Orbit.toBlocks(parentBody.orbit.radius);
                double starClearance = system.star != null ? (double) DataConfig.Star.toBlocks(system.star.radius) : DEFAULT_STAR_RADIUS;
                double maxAllowed = Math.max(1000.0D, parentOrbit - starClearance - 500.0D);

                String starParentId = (system.star != null && system.star.id != null && !system.star.id.isBlank())
                        ? system.star.id : DataConfig.Body.PARENT_ID_DEF;
                List<PlanetInstance.Config> siblingPlanets = system.getMoonsOf(starParentId);
                for (PlanetInstance.Config sibling : siblingPlanets) {
                    if (sibling == null || sibling.id.equalsIgnoreCase(parentBody.id) || sibling.orbit == null) continue;
                    double dist = Math.abs(DataConfig.Orbit.toBlocks(sibling.orbit.radius) - parentOrbit);
                    double siblingClearance = getPhysicalClearanceRadius(sibling);
                    double safeDistance = Math.max(1000.0D, dist * 0.45D - siblingClearance);
                    maxAllowed = Math.min(maxAllowed, safeDistance);
                }
                double minSafe = getMinimumSafeOrbitRadius(system, body);
                double maxSafeBlocks = Math.min(50000.0D, Math.max(DataConfig.Orbit.toBlocks(minSafe) + 500.0D, maxAllowed));
                return DataConfig.Orbit.normalize(maxSafeBlocks);
            }
        }
        return DataConfig.Orbit.RADIUS.max();
    }

    public record Range(double min, double max) {}

    public static List<Range> getSiblingExclusionZones(SolarSystemData system, PlanetInstance.Config body) {
        List<Range> zones = new ArrayList<>();
        if (system == null || body == null || body.parentId == null) return zones;

        List<PlanetInstance.Config> siblings = system.getMoonsOf(body.parentId);
        boolean isMoon = body.isMoon();
        double bodyClearance = isMoon ? getPhysicalClearanceRadius(body) : getSystemClearanceRadius(system, body);

        for (PlanetInstance.Config sibling : siblings) {
            if (sibling == null || sibling.id.equalsIgnoreCase(body.id) || sibling.orbit == null) continue;
            double siblingRadius = sibling.orbit.radius;
            double siblingClearance = isMoon ? getPhysicalClearanceRadius(sibling) : getSystemClearanceRadius(system, sibling);
            double requiredSeparation = DataConfig.Orbit.normalize(siblingClearance + bodyClearance + Math.max(MIN_SEPARATION_MARGIN, (siblingClearance + bodyClearance) * 0.05D));
            zones.add(new Range(siblingRadius - requiredSeparation, siblingRadius + requiredSeparation));
        }

        if (zones.isEmpty()) return zones;
        zones.sort(Comparator.comparingDouble(Range::min));
        List<Range> merged = new ArrayList<>();
        Range current = zones.get(0);
        for (int i = 1; i < zones.size(); i++) {
            Range next = zones.get(i);
            if (next.min <= current.max) {
                current = new Range(current.min, Math.max(current.max, next.max));
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    public static double resolveSafeOrbitRadius(SolarSystemData system, PlanetInstance.Config body, double requestedRadius, double previousRadius) {
        double minSafe = getMinimumSafeOrbitRadius(system, body);
        double maxSafe = getMaximumSafeOrbitRadius(system, body);

        if (maxSafe < minSafe) {
            maxSafe = minSafe + DataConfig.Orbit.normalize(1000.0D);
        }

        double radius = Utils.clamp(requestedRadius, minSafe, maxSafe);
        List<Range> exclusionZones = getSiblingExclusionZones(system, body);

        if (exclusionZones.isEmpty()) {
            return radius;
        }

        for (Range zone : exclusionZones) {
            if (radius >= zone.min && radius <= zone.max) {
                double lower = zone.min;
                double upper = zone.max;

                boolean snapToUpper;
                if (previousRadius <= lower) {
                    snapToUpper = false;
                } else if (previousRadius >= upper) {
                    snapToUpper = true;
                } else {
                    double distToLower = Math.abs(radius - lower);
                    double distToUpper = Math.abs(radius - upper);
                    snapToUpper = distToUpper < distToLower;
                }

                if (!snapToUpper && lower < minSafe) {
                    snapToUpper = true;
                }

                if (snapToUpper) {
                    radius = upper;
                } else {
                    radius = lower;
                }
            }
        }

        return Utils.clamp(radius, minSafe, maxSafe);
    }
}
