package cn.xin_233.animations.client;

import cn.xin_233.animations.config.FbxEffect;
import cn.xin_233.animations.network.FbxEffectPacket;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.plugins.fbx.FbxLoader;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class FbxModel {
    private static final AtomicInteger TEXTURE_IDS = new AtomicInteger();
    static final ResourceLocation WHITE_TEXTURE =
            registerTexture(createWhiteImage(), "white");
    private final Spatial template;
    private final List<GeometryData> templateGeometry;
    private final String animationName;

    private FbxModel(Spatial template, List<GeometryData> geometry, String animationName) {
        this.template = template;
        this.templateGeometry = geometry;
        this.animationName = animationName;
    }

    static FbxModel load(Path effectsRoot, FbxEffect effect) {
        try {
            Path root = effectsRoot.toAbsolutePath().normalize();
            Path modelPath = root.resolve(effect.model).normalize();
            if (!modelPath.startsWith(root) || !Files.isRegularFile(modelPath)) {
                throw new IOException("FBX model is outside effects directory or missing");
            }
            if (Files.size(modelPath) > 64L * 1024L * 1024L) {
                throw new IOException("FBX model exceeds 64 MiB");
            }
            DesktopAssetManager assetManager = new DesktopAssetManager(true);
            assetManager.registerLocator(root.toString(), FileLocator.class);
            ModelKey key = new ModelKey(effect.model);
            Spatial scene = (Spatial) new FbxLoader().load(new LocalAssetInfo(assetManager, key, modelPath));
            List<GeometryData> geometry = new ArrayList<>();
            collect(scene, geometry, root, effect);
            if (geometry.isEmpty()) {
                throw new IOException("FBX scene contains no triangle geometry");
            }
            if (!hasAnimation(scene, effect.animation)) {
                throw new IOException("FBX animation not found: " + effect.animation);
            }
            return new FbxModel(scene, Collections.unmodifiableList(geometry), effect.animation);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot load FBX effect " + effect.getId(), exception);
        }
    }

    FbxModelInstance createInstance(FbxEffectPacket packet) {
        return new FbxModelInstance(template.deepClone(), templateGeometry, animationName, packet);
    }

    private static void collect(Spatial spatial, List<GeometryData> output, Path root, FbxEffect effect) {
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            if (geometry.getMesh() != null && geometry.getMesh().getMode() == Mesh.Mode.Triangles) {
                output.add(GeometryData.create(geometry, root, effect));
            }
        } else if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                collect(child, output, root, effect);
            }
        }
    }

    private static boolean hasAnimation(Spatial spatial, String name) {
        AnimControl control = spatial.getControl(AnimControl.class);
        if (control != null && control.getAnim(name) != null) {
            return true;
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                if (hasAnimation(child, name)) {
                    return true;
                }
            }
        }
        return false;
    }

    static final class GeometryData {
        private final Geometry geometry;
        private final ResourceLocation texture;
        private final float red, green, blue;

        private GeometryData(Geometry geometry, ResourceLocation texture, float red, float green, float blue) {
            this.geometry = geometry;
            this.texture = texture;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        static GeometryData create(Geometry geometry, Path root, FbxEffect effect) {
            ResourceLocation texture = null;
            if (effect.texture != null) {
                texture = registerFileTexture(root.resolve(effect.texture).normalize());
            } else if (geometry.getMaterial() != null) {
                Material material = geometry.getMaterial();
                MatParamTexture param = material.getTextureParam("DiffuseMap");
                if (param != null && param.getTextureValue() != null) {
                    texture = registerImageTexture(param.getTextureValue().getImage(), "fbx-material");
                }
            }
            float red = 1;
            float green = 1;
            float blue = 1;
            if (geometry.getMaterial() != null) {
                Object value = geometry.getMaterial().getParamValue("Diffuse");
                if (value instanceof ColorRGBA) {
                    ColorRGBA color = (ColorRGBA) value;
                    red = color.r;
                    green = color.g;
                    blue = color.b;
                }
            }
            return new GeometryData(geometry, texture, red, green, blue);
        }

        ResourceLocation texture() {
            return texture;
        }

        void write(net.minecraft.client.renderer.BufferBuilder buffer, float alpha) {
            Mesh mesh = geometry.getMesh();
            FloatBuffer positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
            FloatBuffer texCoords = mesh.getFloatBuffer(VertexBuffer.Type.TexCoord);
            IndexBuffer indices = mesh.getIndicesAsList();
            com.jme3.math.Vector3f input = new com.jme3.math.Vector3f();
            com.jme3.math.Vector3f output = new com.jme3.math.Vector3f();
            com.jme3.math.Transform transform = geometry.getWorldTransform();
            for (int i = 0; i < indices.size(); i++) {
                int index = indices.get(i);
                int positionIndex = index * 3;
                input.set(positions.get(positionIndex), positions.get(positionIndex + 1),
                        positions.get(positionIndex + 2));
                transform.transformVector(input, output);
                float u = texCoords == null ? 0 : texCoords.get(index * 2);
                float v = texCoords == null ? 0 : texCoords.get(index * 2 + 1);
                buffer.pos(output.x, output.y, output.z).tex(u, v)
                        .color(red, green, blue, alpha).endVertex();
            }
        }

        GeometryData forGeometry(Geometry replacement) {
            return new GeometryData(replacement, texture, red, green, blue);
        }
    }

    private static ResourceLocation registerFileTexture(Path path) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > 16L * 1024L * 1024L) {
                return null;
            }
            BufferedImage image = ImageIO.read(path.toFile());
            return image == null ? null : registerTexture(image, path.toString());
        } catch (IOException exception) {
            LogManager.getLogger(FbxModel.class).warn("Cannot load FBX texture {}", path, exception);
            return null;
        }
    }

    private static ResourceLocation registerImageTexture(Image image, String name) {
        if (image == null) {
            return null;
        }
        try {
            return registerTexture(toBufferedImage(image), name);
        } catch (RuntimeException exception) {
            LogManager.getLogger(FbxModel.class).warn("Cannot convert FBX texture {}", name, exception);
            return null;
        }
    }

    private static ResourceLocation registerTexture(BufferedImage image, String name) {
        String key = "fbx_" + TEXTURE_IDS.incrementAndGet() + "_" + Integer.toHexString(name.hashCode());
        return Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(
                key, new DynamicTexture(image));
    }

    private static BufferedImage toBufferedImage(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteBuffer data = image.getData(0).duplicate();
        data.clear();
        Image.Format format = image.getFormat();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red;
                int green;
                int blue;
                int alpha = 255;
                if (format == Image.Format.RGBA8) {
                    red = data.get() & 255;
                    green = data.get() & 255;
                    blue = data.get() & 255;
                    alpha = data.get() & 255;
                } else if (format == Image.Format.ABGR8) {
                    alpha = data.get() & 255;
                    blue = data.get() & 255;
                    green = data.get() & 255;
                    red = data.get() & 255;
                } else if (format == Image.Format.RGB8) {
                    red = data.get() & 255;
                    green = data.get() & 255;
                    blue = data.get() & 255;
                } else {
                    throw new IllegalArgumentException("Unsupported FBX texture format: " + format);
                }
                result.setRGB(x, height - y - 1, new Color(red, green, blue, alpha).getRGB());
            }
        }
        return result;
    }

    private static BufferedImage createWhiteImage() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, Color.WHITE.getRGB());
        return image;
    }

    private static final class LocalAssetInfo extends AssetInfo {
        private final Path path;

        private LocalAssetInfo(DesktopAssetManager manager, AssetKey<?> key, Path path) {
            super(manager, key);
            this.path = path;
        }

        @Override
        public InputStream openStream() {
            try {
                return new FileInputStream(path.toFile());
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot open FBX model " + path, exception);
            }
        }
    }
}
