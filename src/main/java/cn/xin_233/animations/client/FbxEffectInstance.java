package cn.xin_233.animations.client;

import cn.xin_233.animations.network.FbxEffectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

final class FbxEffectInstance {
    private final FbxModelInstance model;
    private final FbxEffectPacket packet;
    private int ageTicks;

    FbxEffectInstance(FbxModel model, FbxEffectPacket packet) {
        this.model = model.createInstance(packet);
        this.packet = packet;
    }

    int dimension() {
        return packet.dimension;
    }

    boolean tick() {
        ageTicks++;
        model.update();
        return ageTicks < packet.durationTicks;
    }

    void render(float partialTicks, double viewerX, double viewerY, double viewerZ) {
        double[] position = position(partialTicks);
        GlStateManager.pushMatrix();
        try (FbxRenderState state = new FbxRenderState(packet.additive)) {
            GlStateManager.translate(position[0] - viewerX, position[1] - viewerY, position[2] - viewerZ);
            GlStateManager.rotate(packet.rotationX, 1, 0, 0);
            GlStateManager.rotate(packet.rotationY, 0, 1, 0);
            GlStateManager.rotate(packet.rotationZ, 0, 0, 1);
            GlStateManager.scale(packet.scale, packet.scale, packet.scale);
            draw(model);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private void draw(FbxModelInstance model) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (FbxModel.GeometryData geometry : model.geometries()) {
            ResourceLocation texture = geometry.texture();
            if (texture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            } else {
                Minecraft.getMinecraft().getTextureManager().bindTexture(FbxModel.WHITE_TEXTURE);
            }
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_COLOR);
            geometry.write(buffer, packet.alpha);
            tessellator.draw();
        }
    }

    private double[] position(float partialTicks) {
        double x = packet.anchorUser ? packet.userX : packet.targetX;
        double y = packet.anchorUser ? packet.userY : packet.targetY;
        double z = packet.anchorUser ? packet.userZ : packet.targetZ;
        if (packet.followEntity) {
            int entityId = packet.anchorUser ? packet.userEntityId : packet.targetEntityId;
            Entity entity = Minecraft.getMinecraft().world.getEntityByID(entityId);
            if (entity != null && !entity.isDead) {
                x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
                y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
                z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
            }
        }
        return new double[]{x + packet.offsetX, y + packet.offsetY, z + packet.offsetZ};
    }
}
