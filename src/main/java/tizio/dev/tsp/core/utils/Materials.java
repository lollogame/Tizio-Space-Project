package tizio.dev.tsp.core.utils;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.Node;
import tizio.dev.tsp.MainClass;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Materials {

    private static final int MAX_GIF_BYTES = 8 * 1024 * 1024;
    private static final int MAX_GIF_FRAMES = 200;
    private static final int MAX_GIF_DIMENSION = 1024;
    private static final long MAX_GIF_TOTAL_PIXELS = 16_000_000L;
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_CACHED_GIFS = 16;
    private static final AtomicInteger GIF_COUNTER = new AtomicInteger();

    private static final Set<String> ALLOWED_GIF_HOSTS = Set.of(
            "media.tenor.com"
    );

    private static final ExecutorService GIF_LOADER = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "tsp-gif-loader");
        t.setDaemon(true);
        return t;
    });
    private static final Map<String, AnimatedGif> URL_GIF_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> SKY_TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Optional<ResourceLocation>> SPECULAR_TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static AnimatedGif SECRET_GIF = null;

    public static ResourceLocation getNoTexture() {
        return DEFAULT_DEBUG_TEXTURE;
    }    public static final ResourceLocation DEFAULT_DEBUG_TEXTURE = Texture.add("textures/engine/no_texture");

    public static void init() {
        ImageIO.getImageReadersByFormatName("gif");
    }    public static final ResourceLocation SPACE_SKYBOX = Texture.addEnv("milky_way");

    public static PreloadResult preloadCaches(Collection<String> planetTextures, Collection<String> skyTextures) {
        return preloadCaches(planetTextures, null, skyTextures);
    }    public static final ResourceLocation SUIT_HUD_VIGNETTE = Texture.addScreen("suit_hud_vignette");

    public static PreloadResult preloadCaches(Collection<String> planetTextures, Collection<String> otherTextures, Collection<String> skyTextures) {
        int loaded = 0;
        List<String> missing = new ArrayList<>();
        if (planetTextures != null) {
            for (String texture : planetTextures) {
                if (resolveTextureLocation(texture).equals(getNoTexture())) {
                    missing.add(texture);
                } else {
                    loaded++;
                }
                resolveSpecularTextureLocation(texture);
            }
        }
        if (otherTextures != null) {
            for (String texture : otherTextures) {
                if (resolveTextureLocation(texture).equals(getNoTexture())) {
                    missing.add(texture);
                } else {
                    loaded++;
                }
            }
        }
        if (skyTextures != null) {
            for (String texture : skyTextures) {
                if (resolveSkyTextureLocation(texture).equals(getNoTexture())) {
                    missing.add(texture);
                } else {
                    loaded++;
                }
            }
        }
        if (MainClass.DEBUG) {
            MainClass.LOGGER.debug("URL cache:" + URL_GIF_CACHE.size());
            MainClass.LOGGER.debug("TEXTURE cache:" + TEXTURE_CACHE.size());
            MainClass.LOGGER.debug("SKY TEXTR cache:" + SKY_TEXTURE_CACHE.size());
            MainClass.LOGGER.debug("SPECULAR TEXTR cache:" + SPECULAR_TEXTURE_CACHE.size());
        }
        return new PreloadResult(loaded, missing);
    }    public static final ResourceLocation LOGO = Texture.add("textures/logo");

    public static void clearTextureCaches() {
        TEXTURE_CACHE.clear();
        SKY_TEXTURE_CACHE.clear();
        SPECULAR_TEXTURE_CACHE.clear();
    }

    public static synchronized void clearCache() {
        clearTextureCaches();
        for (AnimatedGif gif : URL_GIF_CACHE.values()) {
            gif.close();
        }
        URL_GIF_CACHE.clear();
        if (SECRET_GIF != null) {
            SECRET_GIF.close();
            SECRET_GIF = null;
        }
    }

    public static ResourceLocation resolveTextureLocation(String rawTexture) {
        if (rawTexture == null || rawTexture.isBlank()) {
            return getNoTexture();
        }
        return TEXTURE_CACHE.computeIfAbsent(rawTexture, key -> resolveInternal(key, "planets"));
    }

    public static ResourceLocation resolveSkyTextureLocation(String rawTexture) {
        if (rawTexture == null || rawTexture.isBlank()) {
            return getNoTexture();
        }
        return SKY_TEXTURE_CACHE.computeIfAbsent(rawTexture, key -> resolveInternal(key, "environment"));
    }

    public static ResourceLocation resolveSpecularTextureLocation(String rawDayTexture) {
        if (rawDayTexture == null || rawDayTexture.isBlank()) {
            return null;
        }
        return SPECULAR_TEXTURE_CACHE.computeIfAbsent(rawDayTexture, key -> {
            String base = key.trim();
            if (base.toLowerCase(Locale.ROOT).endsWith(".png")) {
                base = base.substring(0, base.length() - 4);
            }
            if (base.isEmpty() || base.endsWith("_s")) {
                return Optional.empty();
            }
            return Optional.ofNullable(resolveOrNull(base + "_s", "planets"));
        }).orElse(null);
    }

    private static ResourceLocation resolveInternal(String rawTexture, String defaultSubFolder) {
        ResourceLocation resolved = resolveOrNull(rawTexture, defaultSubFolder);
        return resolved != null ? resolved : getNoTexture();
    }

    private static ResourceLocation resolveOrNull(String rawTexture, String defaultSubFolder) {
        try {
            String path = rawTexture.trim().replace('\\', '/').toLowerCase(Locale.ROOT);

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            String namespace = MainClass.MODID;

            int colonIndex = path.indexOf(':');
            if (colonIndex != -1) {
                namespace = path.substring(0, colonIndex);
                path = path.substring(colonIndex + 1);
            }

            if (path.isEmpty() || path.endsWith("/")) {
                return null;
            }

            if (!path.startsWith("textures/")) {
                if (path.contains("/")) {
                    path = "textures/" + path;
                } else {
                    path = "textures/" + defaultSubFolder + "/" + path;
                }
            }

            if (!path.endsWith(".png")) {
                path = path + ".png";
            }

            ResourceLocation candidateLocation = new ResourceLocation(namespace, path);

            if (textureExists(candidateLocation)) {
                return candidateLocation;
            }

        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private static boolean textureExists(ResourceLocation location) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized AnimatedGif getSecretGif() {
        if (SECRET_GIF == null) {
            SECRET_GIF = new AnimatedGif("https://media.tenor.com/HxqFc75TVOIAAAAi/cat-eating-chips.gif", "sus", true);
        }
        return SECRET_GIF;
    }

    public static AnimatedGif getGifFromUrl(String urlString, String id) {
        String key = urlString == null ? "" : urlString.trim();
        AnimatedGif existing = URL_GIF_CACHE.get(key);
        if (existing != null) {
            return existing;
        }
        if (URL_GIF_CACHE.size() >= MAX_CACHED_GIFS) {
            for (AnimatedGif gif : URL_GIF_CACHE.values()) {
                gif.close();
            }
            URL_GIF_CACHE.clear();
        }
        return URL_GIF_CACHE.computeIfAbsent(key, k -> new AnimatedGif(k, id, true));
    }

    private static String sanitizeId(String id) {
        if (id == null || id.isEmpty()) {
            return "gif";
        }
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }

    private static byte[] readLimited(InputStream is) throws IOException {
        byte[] data = is.readNBytes(MAX_GIF_BYTES + 1);
        if (data.length > MAX_GIF_BYTES) {
            throw new IOException("GIF exceeds size limit");
        }
        return data;
    }

    private static boolean isForbiddenAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] b = address.getAddress();
        if (address instanceof Inet6Address) {
            return (b[0] & 0xFE) == 0xFC;
        }
        int b0 = b[0] & 0xFF;
        int b1 = b[1] & 0xFF;
        return b0 == 0 || (b0 == 100 && (b1 & 0xC0) == 64);
    }

    private static URI validateGifUri(String raw) throws IOException {
        URI uri;
        try {
            uri = new URI(raw.trim());
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("Only https is allowed");
        }
        if (uri.getUserInfo() != null) {
            throw new IOException("User info is not allowed");
        }
        int port = uri.getPort();
        if (port != -1 && port != 443) {
            throw new IOException("Port not allowed");
        }
        String host = uri.getHost();
        if (host == null) {
            throw new IOException("Missing host");
        }
        host = host.toLowerCase(Locale.ROOT);
        if (!ALLOWED_GIF_HOSTS.contains(host)) {
            throw new IOException("Host not allowed: " + host);
        }
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (isForbiddenAddress(address)) {
                throw new IOException("Address not allowed");
            }
        }
        return uri;
    }

    private static byte[] downloadGif(String urlString) throws IOException {
        URI current = validateGifUri(urlString);
        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            HttpURLConnection conn = (HttpURLConnection) current.toURL().openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "image/gif");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            try {
                int code = conn.getResponseCode();
                if (code >= 300 && code < 400) {
                    String location = conn.getHeaderField("Location");
                    if (location == null) {
                        throw new IOException("Redirect without location");
                    }
                    current = validateGifUri(current.resolve(location).toString());
                    continue;
                }
                if (code != 200) {
                    throw new IOException("HTTP " + code);
                }
                String type = conn.getContentType();
                if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("image/gif")) {
                    throw new IOException("Not a GIF content type");
                }
                if (conn.getContentLengthLong() > MAX_GIF_BYTES) {
                    throw new IOException("GIF exceeds size limit");
                }
                try (InputStream is = conn.getInputStream()) {
                    return readLimited(is);
                }
            } finally {
                conn.disconnect();
            }
        }
        throw new IOException("Too many redirects");
    }

    private static Node findChild(Node parent, String name) {
        if (parent == null) {
            return null;
        }
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (name.equals(n.getNodeName())) {
                return n;
            }
        }
        return null;
    }

    private static String stringAttr(Node node, String name, String def) {
        if (node == null || node.getAttributes() == null) {
            return def;
        }
        Node attr = node.getAttributes().getNamedItem(name);
        return attr == null ? def : attr.getNodeValue();
    }

    private static int intAttr(Node node, String name, int def) {
        String value = stringAttr(node, name, null);
        if (value == null) {
            return def;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static DecodedGif decodeGif(byte[] data) throws Exception {
        if (data.length < 10 || data[0] != 'G' || data[1] != 'I' || data[2] != 'F' || data[3] != '8'
                || (data[4] != '7' && data[4] != '9') || data[5] != 'a') {
            throw new IOException("Invalid GIF header");
        }

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) {
            throw new IOException("No GIF reader available");
        }
        ImageReader reader = readers.next();

        List<NativeImage> frames = new ArrayList<>();
        List<Integer> delayList = new ArrayList<>();
        Graphics2D g = null;

        try (ImageInputStream iis = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            reader.setInput(iis, false, false);

            int width = -1;
            int height = -1;
            IIOMetadata streamMeta = reader.getStreamMetadata();
            if (streamMeta != null) {
                Node root = streamMeta.getAsTree("javax_imageio_gif_stream_1.0");
                Node lsd = findChild(root, "LogicalScreenDescriptor");
                width = intAttr(lsd, "logicalScreenWidth", -1);
                height = intAttr(lsd, "logicalScreenHeight", -1);
            }
            if (width <= 0 || height <= 0) {
                BufferedImage first = reader.read(0);
                width = first.getWidth();
                height = first.getHeight();
            }
            if (width > MAX_GIF_DIMENSION || height > MAX_GIF_DIMENSION) {
                throw new IOException("GIF dimensions too large");
            }

            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] canvasPixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
            g = canvas.createGraphics();

            int[] saved = null;
            String prevDisposal = "none";
            int prevX = 0;
            int prevY = 0;
            int prevW = 0;
            int prevH = 0;
            long totalPixels = 0;

            for (int i = 0; i < MAX_GIF_FRAMES; i++) {
                BufferedImage bi;
                try {
                    bi = reader.read(i);
                } catch (IndexOutOfBoundsException e) {
                    break;
                }

                totalPixels += (long) width * height;
                if (totalPixels > MAX_GIF_TOTAL_PIXELS && !frames.isEmpty()) {
                    break;
                }

                int left = 0;
                int top = 0;
                String disposal = "none";
                int delayMs = 100;

                IIOMetadata meta = reader.getImageMetadata(i);
                if (meta != null) {
                    Node root = meta.getAsTree("javax_imageio_gif_image_1.0");
                    Node descriptor = findChild(root, "ImageDescriptor");
                    left = intAttr(descriptor, "imageLeftPosition", 0);
                    top = intAttr(descriptor, "imageTopPosition", 0);
                    Node gce = findChild(root, "GraphicControlExtension");
                    if (gce != null) {
                        disposal = stringAttr(gce, "disposalMethod", "none");
                        delayMs = intAttr(gce, "delayTime", 0) * 10;
                        if (delayMs < 20) {
                            delayMs = 100;
                        }
                    }
                }

                if ("restoreToBackgroundColor".equals(prevDisposal)) {
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(prevX, prevY, prevW, prevH);
                } else if ("restoreToPrevious".equals(prevDisposal) && saved != null) {
                    System.arraycopy(saved, 0, canvasPixels, 0, canvasPixels.length);
                }

                if ("restoreToPrevious".equals(disposal)) {
                    saved = canvasPixels.clone();
                }

                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(bi, left, top, null);

                NativeImage nativeImage = new NativeImage(width, height, false);
                try {
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int argb = canvasPixels[y * width + x];
                            int a = (argb >>> 24) & 0xFF;
                            int r = (argb >> 16) & 0xFF;
                            int gr = (argb >> 8) & 0xFF;
                            int b = argb & 0xFF;
                            nativeImage.setPixelRGBA(x, y, (a << 24) | (b << 16) | (gr << 8) | r);
                        }
                    }
                } catch (RuntimeException e) {
                    nativeImage.close();
                    throw e;
                }
                frames.add(nativeImage);
                delayList.add(delayMs);

                prevDisposal = disposal;
                prevX = left;
                prevY = top;
                prevW = bi.getWidth();
                prevH = bi.getHeight();
            }

            if (frames.isEmpty()) {
                throw new IOException("GIF has no frames");
            }

            int[] delays = new int[delayList.size()];
            for (int i = 0; i < delays.length; i++) {
                delays[i] = delayList.get(i);
            }
            return new DecodedGif(frames, delays, width, height);
        } catch (Exception e) {
            for (NativeImage image : frames) {
                image.close();
            }
            throw e;
        } finally {
            if (g != null) {
                g.dispose();
            }
            reader.dispose();
        }
    }

    protected static class Texture {

        protected static ResourceLocation add(String path) {
            if (path == null) return getNoTexture();
            return ResourceLocation.fromNamespaceAndPath(MainClass.MODID, path + ".png");
        }

        protected static ResourceLocation addEnv(String path) {
            if (path == null) return getNoTexture();
            return ResourceLocation.fromNamespaceAndPath(MainClass.MODID, "textures/environment/" + path + ".png");
        }

        protected static ResourceLocation addScreen(String path) {
            if (path == null) return getNoTexture();
            return ResourceLocation.fromNamespaceAndPath(MainClass.MODID, "textures/gui/" + path + ".png");
        }
    }

    private record DecodedGif(List<NativeImage> frames, int[] delays, int width, int height) {
    }

    public record PreloadResult(int loaded, List<String> missing) {
    }

    public static class AnimatedGif {
        private final ResourceLocation location;
        private volatile DynamicTexture dynamicTexture;
        private volatile List<NativeImage> frames = List.of();
        private volatile int[] delays = new int[0];
        private volatile boolean loaded = false;
        private volatile boolean closed = false;
        private int currentFrame = 0;
        private long lastTime = 0;

        public AnimatedGif(ResourceLocation source, String id) {
            this.location = new ResourceLocation(MainClass.MODID,
                    "dynamic_gif_" + sanitizeId(id) + "_" + GIF_COUNTER.incrementAndGet());
            GIF_LOADER.execute(() -> {
                try {
                    var res = Minecraft.getInstance().getResourceManager().getResource(source);
                    if (res.isEmpty()) {
                        return;
                    }
                    byte[] data;
                    try (InputStream is = res.get().open()) {
                        data = readLimited(is);
                    }
                    publish(decodeGif(data));
                } catch (Exception e) {
                    System.err.println("Cannot load the GIF: " + source);
                    e.printStackTrace();
                }
            });
        }

        public AnimatedGif(String urlString, String id, boolean isUrl) {
            this.location = new ResourceLocation(MainClass.MODID,
                    "dynamic_gif_url_" + sanitizeId(id) + "_" + GIF_COUNTER.incrementAndGet());
            GIF_LOADER.execute(() -> {
                try {
                    publish(decodeGif(downloadGif(urlString)));
                } catch (Exception e) {
                    System.err.println("Cannot load the GIF from URL: " + urlString);
                    e.printStackTrace();
                }
            });
        }

        private void publish(DecodedGif decoded) {
            Minecraft.getInstance().execute(() -> {
                if (closed) {
                    for (NativeImage image : decoded.frames()) {
                        image.close();
                    }
                    return;
                }
                NativeImage first = new NativeImage(decoded.width(), decoded.height(), false);
                first.copyFrom(decoded.frames().get(0));
                DynamicTexture texture = new DynamicTexture(first);
                Minecraft.getInstance().getTextureManager().register(location, texture);
                dynamicTexture = texture;
                frames = decoded.frames();
                delays = decoded.delays();
                currentFrame = 0;
                lastTime = System.currentTimeMillis();
                loaded = true;
            });
        }

        public void update() {
            if (!loaded || closed) return;
            List<NativeImage> current = frames;
            DynamicTexture texture = dynamicTexture;
            if (current.size() <= 1 || texture == null) return;
            long now = System.currentTimeMillis();
            if (now - lastTime >= delays[currentFrame]) {
                currentFrame = (currentFrame + 1) % current.size();
                lastTime = now;
                NativeImage pixels = texture.getPixels();
                if (pixels == null) return;
                pixels.copyFrom(current.get(currentFrame));
                texture.upload();
            }
        }

        public void close() {
            if (closed) return;
            closed = true;
            loaded = false;
            Minecraft.getInstance().execute(() -> {
                if (dynamicTexture != null) {
                    Minecraft.getInstance().getTextureManager().release(location);
                    dynamicTexture = null;
                }
                for (NativeImage image : frames) {
                    image.close();
                }
                frames = List.of();
            });
        }

        public ResourceLocation getLocation() {
            return location;
        }

        public boolean isLoaded() {
            return loaded;
        }
    }









}