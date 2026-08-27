package tizio.dev.tsp.mixin.render.lighting;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tizio.dev.tsp.core.celestial.lighting.LightShadeManager;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(GlslPreprocessor.class)
public abstract class GlslPreprocessorMixin {

    private static final Pattern MIX_LIGHT_PATTERN = Pattern.compile("vec4\\s+minecraft_mix_light\\s*\\([^\\)]*\\)\\s*\\{[^\\}]*\\}", Pattern.DOTALL);
    private static final ResourceLocation SHADER_LOC = new ResourceLocation("tsp", "shaders/include/shading/light.glsl");
    private static String cachedCustomShader = null;

    @Redirect(
            method = "processImports",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/preprocessor/GlslPreprocessor;applyImport(ZLjava/lang/String;)Ljava/lang/String;")
    )
    private String tsp$redirectApplyImport(GlslPreprocessor instance, boolean isGlobal, String importName) {
        String originalGlsl = instance.applyImport(isGlobal, importName);

        if (!LightShadeManager.isSpaceLightingActive() || !LightShadeManager.isRenderingLevel()) {
            return originalGlsl;
        }

        if (importName != null && importName.endsWith("light.glsl")) {
            if (originalGlsl == null || originalGlsl.isEmpty()) {
                return originalGlsl;
            }

            String customFragment = tsp$getOrLoadCustomShader();
            if (customFragment != null && !customFragment.isEmpty()) {
                customFragment = customFragment.replaceAll("(?m)^\\s*#\\s*version.*$", "").trim();

                Matcher matcher = MIX_LIGHT_PATTERN.matcher(originalGlsl);
                String resultGlsl;
                if (matcher.find()) {
                    resultGlsl = matcher.replaceFirst(Matcher.quoteReplacement(customFragment));
                } else {
                    resultGlsl = originalGlsl + "\n\n" + customFragment;
                }

                return resultGlsl.replaceAll("(?m)^\\s*#\\s*version.*$", "").trim();
            }
        }

        return originalGlsl;
    }

    private static String tsp$getOrLoadCustomShader() {
        if (cachedCustomShader != null) {
            return cachedCustomShader;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                Optional<Resource> resource = mc.getResourceManager().getResource(SHADER_LOC);
                if (resource.isPresent()) {
                    try (InputStream is = resource.get().open()) {
                        cachedCustomShader = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        return cachedCustomShader;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Fallback di sicurezza incorporato
        return "vec4 minecraft_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) {\n" +
                "    float light0 = max(0.0, dot(lightDir0, normal));\n" +
                "    float light1 = max(0.0, dot(lightDir1, normal));\n" +
                "    float len1 = length(lightDir1);\n" +
                "    if (len1 > 0.90) {\n" +
                "        float lightAccum = min(1.0, (light0 + light1) * 0.8 + 0.2);\n" +
                "        return vec4(color.rgb * lightAccum, color.a);\n" +
                "    } else {\n" +
                "        float lightAccum = min(1.0, light0 * 0.90 + light1 * 0.90 + 0.08);\n" +
                "        return vec4(color.rgb * lightAccum, color.a);\n" +
                "    }\n" +
                "}";
    }
}