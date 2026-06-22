package com.ankh.client.render;

import com.ankh.entity.ResurrectionZombieEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import net.minecraft.util.Identifier;

public class ResurrectionZombieRenderer
        extends MobEntityRenderer<ResurrectionZombieEntity, PlayerEntityModel<ResurrectionZombieEntity>> {

    public ResurrectionZombieRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);

        addFeature(new ArmorFeatureRenderer<>(
                this,
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));

        addFeature(new HeldItemFeatureRenderer<>(this, ctx.getHeldItemRenderer()));
    }

    @Override
    public Identifier getTexture(ResurrectionZombieEntity entity) {
        return OwnerSkin.resolve(entity.getOwnerUuid().orElse(null), entity.getOwnerName());
    }

    @Override
    public boolean shouldRender(ResurrectionZombieEntity entity, net.minecraft.client.render.Frustum frustum,
                                double x, double y, double z) {
        if (super.shouldRender(entity, frustum, x, y, z)) return true;

        java.util.Optional<BlockPos> lamp = entity.getLampPos();
        if (lamp.isPresent()) {
            BlockPos p = lamp.get();
            net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(
                    p.getX() - 1.0, p.getY(), p.getZ() - 1.0,
                    p.getX() + 2.0, p.getY() + 4.0, p.getZ() + 2.0);
            return frustum.isVisible(box);
        }
        return false;
    }

    private static boolean loggedLampError = false;
    private static boolean loggedRopeError = false;

    @Override
    public void render(ResurrectionZombieEntity zombie, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        super.render(zombie, yaw, tickDelta, matrices, vertexConsumers, light);
        java.util.Optional<BlockPos> lampPos = zombie.getLampPos();
        if (lampPos.isEmpty()) return;

        try {
            renderLampMesh(zombie, tickDelta, matrices, vertexConsumers, lampPos.get());
        } catch (Throwable e) {
            if (!loggedLampError) {
                loggedLampError = true;
                com.ankh.AnkhResurrection.LOGGER.error("[Ankh] Lamp mesh render failed (suppressed)", e);
            }
        }

        if (!zombie.isLeashBroken()) {
            try {
                renderLampRope(zombie, tickDelta, matrices, vertexConsumers, lampPos.get(), light);
            } catch (Throwable e) {
                if (!loggedRopeError) {
                    loggedRopeError = true;
                    com.ankh.AnkhResurrection.LOGGER.error("[Ankh] Lamp rope render failed (suppressed)", e);
                }
            }
        }
    }

    private void renderLampMesh(ResurrectionZombieEntity zombie, float tickDelta, MatrixStack matrices,
                                VertexConsumerProvider vcp, BlockPos lampPos) {
        double mobX = MathHelper.lerp((double) tickDelta, zombie.prevX, zombie.getX());
        double mobY = MathHelper.lerp((double) tickDelta, zombie.prevY, zombie.getY());
        double mobZ = MathHelper.lerp((double) tickDelta, zombie.prevZ, zombie.getZ());
        LampMeshRenderer.draw(matrices, vcp,
                (lampPos.getX() + 0.5) - mobX, lampPos.getY() - mobY, (lampPos.getZ() + 0.5) - mobZ);
    }

    private void renderLampRope(ResurrectionZombieEntity zombie, float tickDelta, MatrixStack matrices,
                                VertexConsumerProvider vcp, BlockPos lampPos, int light) {
        matrices.push();
        double mobX = MathHelper.lerp((double) tickDelta, zombie.prevX, zombie.getX());
        double mobY = MathHelper.lerp((double) tickDelta, zombie.prevY, zombie.getY());
        double mobZ = MathHelper.lerp((double) tickDelta, zombie.prevZ, zombie.getZ());
        double attachY = 1.3;

        double lampX = lampPos.getX() + 0.5;
        double lampZ = lampPos.getZ() + 0.5;
        double knotWorldY = lampPos.getY() + 1.5;

        matrices.translate(0.0, attachY, 0.0);
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLeash());

        float cxp = (float) (lampX - mobX);
        float czp = (float) (lampZ - mobZ);
        float cyp = (float) (knotWorldY - (mobY + attachY));
        float poleR = 0.07f;
        float a0 = (float) Math.atan2(-czp, -cxp);
        float wrapTop = cyp + 0.18f;
        float wrapBot = cyp - 0.18f;
        float ax = cxp + poleR * (float) Math.cos(a0);
        float az = czp + poleR * (float) Math.sin(a0);

        float dist = (float) Math.sqrt(cxp * cxp + cyp * cyp + czp * czp);
        float slack = Math.max(0.0f, 11.0f - dist);
        float sag = Math.min(slack * 0.38f, 2.0f);
        float floorY = (float) (-attachY + 0.02);

        java.util.ArrayList<float[]> path = new java.util.ArrayList<>();
        int ropeSteps = 24;
        for (int k = 0; k <= ropeSteps; k++) {
            float f = k / (float) ropeSteps;
            float x = ax * f;
            float z = az * f;
            float base = wrapTop * f;
            float droop = sag * 4.0f * f * (1.0f - f);
            float y = Math.max(base - droop, floorY);
            path.add(new float[]{x, y, z});
        }

        int wrapSteps = 22;
        float turns = 1.75f;
        for (int k = 1; k <= wrapSteps; k++) {
            float t = k / (float) wrapSteps;
            float ang = a0 + turns * 2.0f * (float) Math.PI * t;
            float x = cxp + poleR * (float) Math.cos(ang);
            float z = czp + poleR * (float) Math.sin(ang);
            float y = wrapTop + (wrapBot - wrapTop) * t;
            path.add(new float[]{x, y, z});
        }

        emitRopePass(vc, m, path, light, false);
        emitRopePass(vc, m, path, light, true);
        matrices.pop();
    }

    private static void emitRopePass(VertexConsumer vc, Matrix4f m, java.util.List<float[]> path,
                                     int light, boolean reverse) {
        int n = path.size();
        float halfW = 0.05f;
        float thick = 0.04f;
        for (int idx = 0; idx < n; idx++) {
            int i = reverse ? (n - 1 - idx) : idx;
            float[] p = path.get(i);
            float[] a = path.get(Math.max(0, i - 1));
            float[] b = path.get(Math.min(n - 1, i + 1));
            float tx = b[0] - a[0];
            float tz = b[2] - a[2];
            float tl = (float) Math.sqrt(tx * tx + tz * tz);
            float kx, kz;
            if (tl > 1.0e-4f) { kx = (-tz / tl) * halfW; kz = (tx / tl) * halfW; }
            else { kx = halfW; kz = 0.0f; }
            float shade = (idx % 2 == 0) ? 1.0f : 0.72f;
            float cr = 0.0f, cg = 0.85f * shade, cb = 0.95f * shade;
            float topA = reverse ? 0.0f : thick;
            float topB = reverse ? thick : 0.0f;
            vc.vertex(m, p[0] - kx, p[1] + topA, p[2] + kz).color(cr, cg, cb, 1.0f).light(light);
            vc.vertex(m, p[0] + kx, p[1] + topB, p[2] - kz).color(cr, cg, cb, 1.0f).light(light);
        }
    }
}
