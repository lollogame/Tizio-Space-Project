package tizio.dev.engine.instance;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

public final class ClientRenderTypes extends RenderType {

    private static final ShaderStateShard ATMOSPHERE_SHADER = new ShaderStateShard(ClientShaderRegistry::atmosphereShader);
    private static final ShaderStateShard BLACK_HOLE_SHADER = new ShaderStateShard(ClientShaderRegistry::blackHoleShader);
    private static final ShaderStateShard SUN_SHADER = new ShaderStateShard(ClientShaderRegistry::sunShader);
    private static final ShaderStateShard PLANET_SURFACE_SHADER = new ShaderStateShard(ClientShaderRegistry::planetSurface);
    private static final ShaderStateShard PLANET_RING_SHADER = new ShaderStateShard(ClientShaderRegistry::planetRing);

    private static final RenderType ATMOSPHERE = create(
            "planet_atmosphere",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(ATMOSPHERE_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)
    );

    private static final RenderType PLANET_RING = create(
            "planet_ring",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(PLANET_RING_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)
    );

    private static final RenderType SUN = create(
            "sun",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(SUN_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)
    );

    private static final RenderType PLANET_SURFACE = create(
            "planet_surface",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(PLANET_SURFACE_SHADER)
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)
    );

    private static final RenderType BLACK_HOLE = create(
            "black_hole",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(BLACK_HOLE_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)
    );

    private static final RenderType DEBUG_LINES = create(
            "debug_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.DEBUG_LINES,
            256,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
    );

    private ClientRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType atmosphere() { return ATMOSPHERE; }
    public static RenderType sun() { return SUN; }
    public static RenderType ring() { return PLANET_RING; }
    public static RenderType blackHole() { return BLACK_HOLE; }
    public static RenderType planetSurface() { return PLANET_SURFACE; }
    public static RenderType debugLines() { return DEBUG_LINES; }
}
