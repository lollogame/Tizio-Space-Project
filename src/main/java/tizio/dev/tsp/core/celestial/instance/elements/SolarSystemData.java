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
