package tizio.dev.tsp.core.utils.volume;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public record PreparedVolume(Vec3 relativeCenter, Vector3f axisX, Vector3f axisY, Vector3f axisZ, Vector3f centerRelativeView, Vector3f axisXView, Vector3f axisYView, Vector3f axisZView, Vector3f cameraLocalPos) {}
