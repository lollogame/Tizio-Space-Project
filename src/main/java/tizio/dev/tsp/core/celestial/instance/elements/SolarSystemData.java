package tizio.dev.tsp.core.celestial.instance.elements;

import tizio.dev.tsp.config.DataConfig;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;

import java.util.ArrayList;
import java.util.List;

public class SolarSystemData {

    public static final String DEFAULT_SPACE_DIMENSION = DataConfig.System.DEFAULT_SPACE_DIMENSION;

    public String id = DataConfig.System.DEFAULT_SYSTEM_ID;
    public String dimension = DEFAULT_SPACE_DIMENSION;
    public double originX = DataConfig.System.ORIGIN_DEF.x;
    public double originY = DataConfig.System.ORIGIN_DEF.y;
    public double originZ = DataConfig.System.ORIGIN_DEF.z;
    public float globalScale = DataConfig.System.SCALE.defF();
    public float gravity = (float) DataConfig.System.GRAVITY_DEF;

    public SunInstance.Config star = new SunInstance.Config();
    public List<PlanetInstance.Config> bodies = new ArrayList<>();

    public SolarSystemData() {}

    public SolarSystemData(String id, String dimension) {
        this.id = id;
        this.dimension = dimension;
    }

    public PlanetInstance.Config findBody(String bodyId) {
        if (bodyId == null) return null;
        for (PlanetInstance.Config body : bodies) {
            if (bodyId.equalsIgnoreCase(body.id)) {
                return body;
            }
        }
        return null;
    }

    public boolean canAddBody() {
        return bodies.size() < DataConfig.System.MAX_BODIES_LIMIT;
    }

    public List<PlanetInstance.Config> getMoonsOf(String parentId) {
        List<PlanetInstance.Config> result = new ArrayList<>();
        if (parentId == null || parentId.isBlank()) return result;
        for (PlanetInstance.Config body : bodies) {
            if (parentId.equalsIgnoreCase(body.parentId)) {
                result.add(body);
            }
        }
        return result;
    }

    public boolean isDescendant(String potentialDescendantId, String ancestorId) {
        if (potentialDescendantId == null || ancestorId == null || potentialDescendantId.isBlank() || ancestorId.isBlank()) {
            return false;
        }
        if (potentialDescendantId.equalsIgnoreCase(ancestorId)) {
            return true;
        }
        java.util.Set<String> visited = new java.util.HashSet<>();
        String currId = potentialDescendantId;
        while (currId != null && !currId.isBlank() && visited.add(currId.toLowerCase(java.util.Locale.ROOT))) {
            PlanetInstance.Config curr = findBody(currId);
            if (curr == null || curr.parentId == null) break;
            if (ancestorId.equalsIgnoreCase(curr.parentId)) {
                return true;
            }
            currId = curr.parentId;
        }
        return false;
    }

    public SolarSystemData copy() {
        SolarSystemData copy = new SolarSystemData();
        copy.id = this.id;
        copy.dimension = this.dimension;
        copy.originX = this.originX;
        copy.originY = this.originY;
        copy.originZ = this.originZ;
        copy.globalScale = this.globalScale;
        copy.gravity = this.gravity;
        copy.star = this.star != null ? this.star.copy() : new SunInstance.Config();
        copy.bodies = new ArrayList<>();
        for (PlanetInstance.Config body : this.bodies) {
            copy.bodies.add(body.copy());
        }
        return copy;
    }
}
