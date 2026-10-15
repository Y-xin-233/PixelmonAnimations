package cn.xin_233.animations.client;

import cn.xin_233.animations.network.FbxEffectPacket;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.animation.SkeletonControl;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;

final class FbxModelInstance {
    private final Spatial scene;
    private final List<FbxModel.GeometryData> geometry;

    FbxModelInstance(Spatial scene, List<FbxModel.GeometryData> templateGeometry,
                     String animationName, FbxEffectPacket packet) {
        this.scene = scene;
        this.geometry = new ArrayList<>(templateGeometry.size());
        collectGeometry(scene, geometry, templateGeometry);
        configure(scene, animationName, packet);
    }

    void update() {
        updateControls(scene);
        scene.updateGeometricState();
        renderSkinning(scene);
    }

    List<FbxModel.GeometryData> geometries() {
        return geometry;
    }

    private static void configure(Spatial spatial, String animationName, FbxEffectPacket packet) {
        AnimControl control = spatial.getControl(AnimControl.class);
        if (control != null && control.getAnim(animationName) != null) {
            AnimChannel channel = control.createChannel();
            channel.setAnim(animationName);
            channel.setLoopMode(packet.loop ? LoopMode.Loop : LoopMode.DontLoop);
            channel.setSpeed(packet.speed);
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                configure(child, animationName, packet);
            }
        }
    }

    private void collectGeometry(Spatial spatial, List<FbxModel.GeometryData> output,
                                 List<FbxModel.GeometryData> template) {
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            if (geometry.getMesh() != null && geometry.getMesh().getMode() == Mesh.Mode.Triangles) {
                int index = output.size();
                if (index < template.size()) {
                    output.add(template.get(index).forGeometry(geometry));
                }
            }
        } else if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                collectGeometry(child, output, template);
            }
        }
    }

    private void updateControls(Spatial spatial) {
        AnimControl anim = spatial.getControl(AnimControl.class);
        if (anim != null) {
            anim.update(1.0F / 20.0F);
        }
        SkeletonControl skeleton = spatial.getControl(SkeletonControl.class);
        if (skeleton != null) {
            skeleton.update(1.0F / 20.0F);
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                updateControls(child);
            }
        }
    }

    private void renderSkinning(Spatial spatial) {
        SkeletonControl skeleton = spatial.getControl(SkeletonControl.class);
        if (skeleton != null) {
            skeleton.setHardwareSkinningPreferred(false);
            skeleton.render(null, null);
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                renderSkinning(child);
            }
        }
    }
}
