package com.ankh.client.render;

import com.ankh.entity.CorpseEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class CorpseRenderer
        extends LivingEntityRenderer<CorpseEntity, PlayerEntityModel<CorpseEntity>> {

    private static boolean loggedLampError = false;

    public CorpseRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.0f);
    }

    @Override
    public Identifier getTexture(CorpseEntity entity) {
        return OwnerSkin.resolve(entity.getOwnerUuid().orElse(null), entity.getOwnerName());
    }

    @Override
    public boolean shouldRender(CorpseEntity entity, net.minecraft.client.render.Frustum frustum,
                                double x, double y, double z) {
        if (super.shouldRender(entity, frustum, x, y, z)) return true;

        java.util.Optional<BlockPos> lamp = entity.getLampPos();
        if (lamp.isPresent()) {
            BlockPos p = lamp.get();
            net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(
                    p.getX() - 1.0, p.getY(), p.getZ() - 1.0, p.getX() + 2.0, p.getY() + 4.0, p.getZ() + 2.0);
            return frustum.isVisible(box);
        }
        return false;
    }

    @Override
    public void render(CorpseEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {

        entity.getLampPos().ifPresent(lp -> {
            try {
                double cx = MathHelper.lerp((double) tickDelta, entity.prevX, entity.getX());
                double cy = MathHelper.lerp((double) tickDelta, entity.prevY, entity.getY());
                double cz = MathHelper.lerp((double) tickDelta, entity.prevZ, entity.getZ());
                LampMeshRenderer.draw(matrices, vertexConsumers,
                        (lp.getX() + 0.5) - cx, lp.getY() - cy, (lp.getZ() + 0.5) - cz);
            } catch (Throwable e) {
                if (!loggedLampError) {
                    loggedLampError = true;
                    com.ankh.AnkhResurrection.LOGGER.error("[Ankh] Corpse lamp render failed (suppressed)", e);
                }
            }
        });

        matrices.push();

        matrices.translate(0.0, 0.1, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }

    @Override
    protected boolean hasLabel(CorpseEntity entity) {

        return false;
    }
}
