package thunder.hack.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.GhostRenderer3D;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.TextureStorage;
import thunder.hack.utility.render.animation.CaptureMark;
import thunder.hack.utility.render.animation.advanced.Animation;
import thunder.hack.utility.render.animation.advanced.Easing;
import thunder.hack.utility.render.animation.advanced.InfinityAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TargetESP extends Module {

    private static final int PARTICLE_LIMIT = 3;
    private static final float PARTICLE_SIZE = 0.28F;
    private static final float GHOST_V1_SPEED = 0.62F;

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.GhostV2);

    private final Animation ghostV1Anim = new Animation()
            .setEasing(Easing.EASE_OUT_QUAD).setSpeed(400).setSize(1.0F).setForward(false);
    private LivingEntity ghostV1Target;

    private final TargetEspRenderer renderer = new TargetEspRenderer(
            () -> PARTICLE_LIMIT, () -> PARTICLE_SIZE, () -> TextureStorage.firefly);

    public TargetESP() {
        super("TargetESP", Category.RENDER);
    }

    @Override
    public void onDisable() {
        renderer.reset();
        resetGhostV1();
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        if (fullNullCheck()) {
            renderer.reset();
            resetGhostV1();
            return;
        }

        Entity target = resolveTarget();

        if (mode.is(Mode.GhostV1)) {
            renderGhostV1(target, stack);
        } else {
            resetGhostV1();
            switch (mode.getValue()) {
                case Circle -> {
                    if (target == null) { renderer.reset(); return; }
                    Render3DEngine.drawOldTargetEsp(stack, target);
                }
                case Cube -> {
                    if (target == null) { renderer.reset(); return; }
                    CaptureMark.render(target);
                }
                case GhostV2 -> {
                    if (target instanceof LivingEntity living) {
                        renderer.render(living);
                    } else {
                        renderer.reset();
                    }
                }
                default -> {}
            }
        }

        if (!mode.is(Mode.GhostV2)) renderer.reset();
    }

    private Entity resolveTarget() {
        Entity auraTarget = ModuleManager.aura.target;
        return auraTarget != null && !auraTarget.isRemoved() ? auraTarget : null;
    }

    private void renderGhostV1(Entity target, MatrixStack stack) {
        LivingEntity livingTarget = (target instanceof LivingEntity living && living.isAlive()) ? living : null;
        if (livingTarget != null) ghostV1Target = livingTarget;

        ghostV1Anim.setForward(livingTarget != null);
        if (ghostV1Target != null) {
            if (ghostV1Target.isAlive() && !ghostV1Anim.finished(false)) {
                float anim = ghostV1Anim.get();
                float red = MathHelper.clamp((ghostV1Target.hurtTime - Render3DEngine.getTickDelta()) / 20.0F, 0.0F, 1.0F);
                // Thunder signature: renderGhosts(espLength, factor, shaking, amplitude, entity)
                Render3DEngine.renderGhosts(3, (int)(anim * 10), red, GHOST_V1_SPEED, ghostV1Target);
            } else {
                ghostV1Target = null;
            }
        }
    }

    private void resetGhostV1() {
        ghostV1Target = null;
        ghostV1Anim.setForward(false);
        ghostV1Anim.reset();
    }

    public enum Mode {
        Circle("Circle"),
        Cube("Cube"),
        GhostV1("Ghost V1"),
        GhostV2("Ghost V2");

        private final String displayName;
        Mode(String displayName) { this.displayName = displayName; }

        @Override
        public String toString() { return displayName; }
    }

    // ===================== Inner Renderer =====================

    public static class TargetEspRenderer {
        private final Supplier<Integer> particleLimit;
        private final Supplier<Float> particleSize;
        private final Supplier<net.minecraft.util.Identifier> textureSupplier;
        private final InfinityAnimation moving = new InfinityAnimation();
        private final Animation targetEspAnim = new Animation()
                .setEasing(Easing.TARGETESP_EASE_OUT_BACK).setSpeed(300).setSize(1.0F).setForward(false);
        private final List<GhostRenderer3D> particles = new ArrayList<>();
        private long lastRenderTime = System.currentTimeMillis();

        public TargetEspRenderer(Supplier<Integer> particleLimit, Supplier<Float> particleSize,
                                  Supplier<net.minecraft.util.Identifier> textureSupplier) {
            this.particleLimit = particleLimit;
            this.particleSize = particleSize;
            this.textureSupplier = textureSupplier;
        }

        public void render(LivingEntity target) {
            if (target == null || !target.isAlive()) { reset(); return; }

            // Fps thấp thì không spawn thêm particle mới, trail cũ vẫn giữ
            if (mc.getCurrentFps() >= 45) {
                spawnParticleIfNeeded(target);
            }

            // Delta time fix: tốc độ animation không phụ thuộc fps
            long now = System.currentTimeMillis();
            float deltaMs = Math.min(now - lastRenderTime, 50); // cap 50ms (~20fps min)
            lastRenderTime = now;
            float deltaFactor = deltaMs / 16.67f; // normalize về 60fps

            moving.animate(moving.get() + 20.0F * deltaFactor, 55);
            targetEspAnim.setForward(target.hurtTime > 7);

            float movementValue = moving.get();
            float animationFactor = targetEspAnim.get();

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, textureSupplier.get());

            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Camera camera = mc.gameRenderer.getCamera();

            for (int i = 0; i < particles.size(); i++) {
                GhostRenderer3D particle = particles.get(i);
                updateParticle(particle, i, deltaFactor, target, movementValue, animationFactor);
                particle.render(buffer, camera);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }

        private void spawnParticleIfNeeded(LivingEntity target) {
            int desired = Math.max(1, particleLimit.get());
            while (particles.size() < desired)
                particles.add(new GhostRenderer3D(target.getPos(), Vec3d.ZERO, particleSize.get()));
            while (particles.size() > desired)
                particles.removeLast();
        }

        private void updateParticle(GhostRenderer3D particle, int index, float deltaFactor,
                                     LivingEntity target, float movementValue, float animationFactor) {
            int segments = Math.max(1, particleLimit.get());
            float angleOffset = index * 360.0F / segments;
            float currentAngle = movementValue + angleOffset;
            double rad = Math.toRadians(currentAngle);
            double baseRadius = getOrbitRadius(target, particleSize.get());
            double dynamicRadius = baseRadius - baseRadius * animationFactor;
            double offsetX = Math.sin(rad) * dynamicRadius;
            double offsetZ = Math.cos(rad) * dynamicRadius;
            double verticalSwing = Math.sin(Math.toRadians(movementValue / (index + 1.0F))) * getVerticalAmplitude(target);
            Vec3d desiredPos = target.getPos().add(offsetX, getVerticalCenter(target) + verticalSwing, offsetZ);
            double mul = Math.min(0.25, 0.05F * deltaFactor);
            Vec3d motion = desiredPos.subtract(particle.getPosition()).multiply(mul, mul, mul);
            particle.setMotion(motion);
            particle.tick();
        }

        private double getOrbitRadius(LivingEntity target, float particleSize) {
            Box box = target.getBoundingBox();
            double hitboxWidth = Math.max(box.getLengthX(), box.getLengthZ());
            return hitboxWidth * 0.5 + particleSize * 0.25;
        }

        private double getVerticalCenter(LivingEntity target) {
            return getVerticalAmplitude(target) - 0.7;
        }

        private double getVerticalAmplitude(LivingEntity target) {
            return target.getBoundingBox().getLengthY() * 0.5;
        }

        public void reset() {
            particles.clear();
            moving.getAnimation().reset();
            targetEspAnim.reset();
            lastRenderTime = System.currentTimeMillis();
        }
    }
}
