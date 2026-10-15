package cn.xin_233.animations.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;

/** Preserves the world renderer's state while drawing unlit effect materials. */
final class FbxRenderState implements AutoCloseable {
    private final boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
    private final boolean colorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    private final int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    private final int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final FloatBuffer color = BufferUtils.createFloatBuffer(16);
    private final boolean lightmap;
    private final boolean texture;
    private final int boundTexture;
    private final int textureMode;

    FbxRenderState(boolean additive) {
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        lightmap = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        textureMode = GL11.glGetTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE);

        // POSITION_TEX_COLOR supplies material colors without normals. The
        // lighting left on by Pixelmon would replace those colors with white.
        GlStateManager.disableLighting();
        GlStateManager.disableColorMaterial();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA,
                additive ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        GlStateManager.resetColor();
        GlStateManager.color(1, 1, 1, 1);
    }

    @Override
    public void close() {
        // Restore through GlStateManager so its cached flags match OpenGL.
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.bindTexture(boundTexture);
        GlStateManager.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, textureMode);
        if (texture) {
            GlStateManager.enableTexture2D();
        } else {
            GlStateManager.disableTexture2D();
        }
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        if (lightmap) {
            GlStateManager.enableTexture2D();
        } else {
            GlStateManager.disableTexture2D();
        }
        GlStateManager.setActiveTexture(activeTexture);
        GlStateManager.tryBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
        if (blend) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }
        if (cull) {
            GlStateManager.enableCull();
        } else {
            GlStateManager.disableCull();
        }
        if (lighting) {
            GlStateManager.enableLighting();
        } else {
            GlStateManager.disableLighting();
        }
        if (colorMaterial) {
            GlStateManager.enableColorMaterial();
        } else {
            GlStateManager.disableColorMaterial();
        }
        GlStateManager.resetColor();
        GlStateManager.color(color.get(0), color.get(1), color.get(2), color.get(3));
    }
}
