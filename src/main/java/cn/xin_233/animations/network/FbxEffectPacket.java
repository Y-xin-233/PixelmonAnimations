package cn.xin_233.animations.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class FbxEffectPacket implements IMessage {
    public int dimension;
    public String effectId;
    public int userEntityId;
    public int targetEntityId;
    public float userX;
    public float userY;
    public float userZ;
    public float targetX;
    public float targetY;
    public float targetZ;
    public boolean anchorUser;
    public boolean followEntity;
    public boolean loop;
    public int durationTicks;
    public float scale;
    public float speed;
    public float alpha;
    public boolean additive;
    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public float rotationX;
    public float rotationY;
    public float rotationZ;

    public FbxEffectPacket() {
    }

    public FbxEffectPacket(int dimension, String effectId, int userEntityId, int targetEntityId,
                           float userX, float userY, float userZ, float targetX, float targetY, float targetZ,
                           boolean anchorUser, boolean followEntity, boolean loop, int durationTicks, float scale,
                           float speed, float alpha, boolean additive, float offsetX, float offsetY, float offsetZ,
                           float rotationX, float rotationY, float rotationZ) {
        this.dimension = dimension;
        this.effectId = effectId;
        this.userEntityId = userEntityId;
        this.targetEntityId = targetEntityId;
        this.userX = userX;
        this.userY = userY;
        this.userZ = userZ;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.anchorUser = anchorUser;
        this.followEntity = followEntity;
        this.loop = loop;
        this.durationTicks = durationTicks;
        this.scale = scale;
        this.speed = speed;
        this.alpha = alpha;
        this.additive = additive;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        dimension = buffer.readInt();
        effectId = ByteBufUtils.readUTF8String(buffer);
        userEntityId = buffer.readInt();
        targetEntityId = buffer.readInt();
        userX = buffer.readFloat();
        userY = buffer.readFloat();
        userZ = buffer.readFloat();
        targetX = buffer.readFloat();
        targetY = buffer.readFloat();
        targetZ = buffer.readFloat();
        anchorUser = buffer.readBoolean();
        followEntity = buffer.readBoolean();
        loop = buffer.readBoolean();
        durationTicks = buffer.readUnsignedShort();
        scale = buffer.readFloat();
        speed = buffer.readFloat();
        alpha = buffer.readFloat();
        additive = buffer.readBoolean();
        offsetX = buffer.readFloat();
        offsetY = buffer.readFloat();
        offsetZ = buffer.readFloat();
        rotationX = buffer.readFloat();
        rotationY = buffer.readFloat();
        rotationZ = buffer.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(dimension);
        ByteBufUtils.writeUTF8String(buffer, effectId);
        buffer.writeInt(userEntityId);
        buffer.writeInt(targetEntityId);
        buffer.writeFloat(userX);
        buffer.writeFloat(userY);
        buffer.writeFloat(userZ);
        buffer.writeFloat(targetX);
        buffer.writeFloat(targetY);
        buffer.writeFloat(targetZ);
        buffer.writeBoolean(anchorUser);
        buffer.writeBoolean(followEntity);
        buffer.writeBoolean(loop);
        buffer.writeShort(durationTicks);
        buffer.writeFloat(scale);
        buffer.writeFloat(speed);
        buffer.writeFloat(alpha);
        buffer.writeBoolean(additive);
        buffer.writeFloat(offsetX);
        buffer.writeFloat(offsetY);
        buffer.writeFloat(offsetZ);
        buffer.writeFloat(rotationX);
        buffer.writeFloat(rotationY);
        buffer.writeFloat(rotationZ);
    }

    public static final class Handler implements IMessageHandler<FbxEffectPacket, IMessage> {
        @Override
        public IMessage onMessage(FbxEffectPacket message, MessageContext context) {
            cn.xin_233.animations.Main.PROXY.handleFbxEffect(message);
            return null;
        }
    }
}
