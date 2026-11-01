package com.tuskacts.client;

import com.tuskacts.TuskActs;
import com.tuskacts.entity.TuskStand;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

import java.util.function.Supplier;

/**
 * Draws Tusk with plain boxes, following its owner. Act 1: small axolotl-like plush. Act 2: sturdy body with barrels.
 * Act 3: humanoid with dark star shoulders and a mohawk blade. Act 4: big square body with the face on the chest and a horseshoe.
 * Pink body, yellow stars, burgundy hearts.
 */
public class StandRenderer extends EntityRenderer<TuskStand> {
    private static final Identifier WHITE = TuskActs.id("textures/misc/white.png");
    private static final int PINK = 0xF08CB8, PINK_D = 0xD2709C, STAR = 0xFFE27A, HEART = 0x8E1B3B, BLACK = 0x15101A,
            GOLD = 0xFFC940, LAV = 0xB9A0E0, GREY = 0xCDB2C2, DARK = 0x3A2A48, WHITE_C = 0xFFFFFF;
    private static final int FULL = Boxes.FULL_LIGHT;

    public StandRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(TuskStand entity) {
        return WHITE;
    }

    @Override
    public boolean shouldRender(TuskStand e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    private static void box(MatrixStack.Entry e, Supplier<VertexConsumer> vc, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int light) {
        Boxes.box(e, vc, x0, y0, z0, x1, y1, z1, rgb, 255, light);
    }

    @Override
    public void render(TuskStand st, float yaw, float td, MatrixStack m, VertexConsumerProvider vcp, int light) {
        World world = st.getWorld();
        Entity owner = world.getEntityById(st.ownerId());
        if (owner == null) return;
        double time = world.getTime() + td;
        float oyaw = MathHelper.lerpAngleDegrees(td, owner.prevYaw, owner.getYaw());
        double yr = Math.toRadians(oyaw);
        double fx = -Math.sin(yr), fz = Math.cos(yr), rx = -Math.cos(yr), rz = -Math.sin(yr);
        int act = st.act(), stance = st.stance();
        double phase = stance == 0 ? 0 : Math.min(1.0, (time - st.stanceStart()) / Math.max(1, st.stanceDur()));
        double k = stance == 0 ? 0 : Math.max(0, Math.min(1, Math.min((time - st.stanceStart()) / 3.0, (st.stanceStart() + st.stanceDur() - time) / 3.0)));
        double ox = MathHelper.lerp(td, owner.prevX, owner.getX()), oy = MathHelper.lerp(td, owner.prevY, owner.getY()), oz = MathHelper.lerp(td, owner.prevZ, owner.getZ());

        double bob = Math.sin(time * 0.1) * 0.08;
        double ix = ox + rx * (0.95 + act * 0.05) - fx * (0.7 + act * 0.28), iz = oz + rz * (0.95 + act * 0.05) - fz * (0.7 + act * 0.28), iy = oy + 0.9 + bob;
        double dist = 1.0 + (act == 4 ? 0.55 : 0.2 * act) + (stance == 3 && phase > 0.45 ? 0.6 : 0);
        double cx, cy, cz;
        if (stance == 1) {
            cx = ox + rx * 0.8 + fx * 0.3; cz = oz + rz * 0.8 + fz * 0.3; cy = oy + 1.0;
        } else {
            cx = ox + fx * dist; cz = oz + fz * dist; cy = oy + 0.2;
        }
        double px = MathHelper.lerp(k, ix, cx), py = MathHelper.lerp(k, iy, cy), pz = MathHelper.lerp(k, iz, cz);

        double ex = MathHelper.lerp(td, st.prevX, st.getX()), ey = MathHelper.lerp(td, st.prevY, st.getY()), ez = MathHelper.lerp(td, st.prevZ, st.getZ());
        m.push();
        m.translate(px - ex, py - ey, pz - ez);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-oyaw));

        // arm swing in radians; 0 hangs down, pi/2 points forward, pi points up
        double r, l;
        switch (stance) {
            case 1 -> { r = Math.PI / 2 - (phase < 0.3 ? 0.35 * (1 - phase / 0.3) : 0); l = 0.2; }
            case 2 -> { r = 0.4 + 1.25 * (0.5 + 0.5 * Math.sin(time * 1.9)); l = 0.4 + 1.25 * (0.5 + 0.5 * Math.sin(time * 1.9 + Math.PI)); }
            case 3 -> { if (phase < 0.45) { r = -1.0; l = -0.5; } else { r = 1.55; l = 0.6; } }
            case 4 -> { r = phase < 0.3 ? -0.4 : Math.PI * 0.95; l = 0.3; }
            case 5 -> { r = l = phase < 0.4 ? Math.PI : 1.25; }
            case 6 -> { r = l = 1.35 + 0.1 * Math.sin(time * 0.8); }
            default -> { r = 0.12 + 0.12 * Math.sin(time * 0.12); l = 0.12 - 0.12 * Math.sin(time * 0.12); }
        }
        Supplier<VertexConsumer> body = () -> vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE));
        Supplier<VertexConsumer> glow = () -> vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE));
        MatrixStack.Entry e = m.peek();
        switch (act) {
            case 1 -> act1(m, e, body, glow, light, r, l, time);
            case 2 -> act2(m, e, body, glow, light, r, l);
            case 3 -> act3(m, e, body, glow, light, r, l);
            default -> act4(m, e, body, glow, light, r, l);
        }
        m.pop();
    }

    /** One arm hanging from the pivot, swung forward by angle radians. */
    private void arm(MatrixStack m, Supplier<VertexConsumer> vc, int light, float px, float py, float pz, double angle,
                     float w, float len, float fist, int armColor, int fistColor) {
        m.push();
        m.translate(px / 16f, py / 16f, pz / 16f);
        m.multiply(RotationAxis.POSITIVE_X.rotation((float) -angle));
        MatrixStack.Entry en = m.peek();
        box(en, vc, -w / 2, -len, -w / 2, w / 2, 0, w / 2, armColor, light);
        box(en, vc, -fist / 2, -len - fist, -fist / 2, fist / 2, -len, fist / 2, fistColor, light);
        m.pop();
    }

    // ------------------------------------------------------------------ ACT 1: small plush with long ears, horn, heart tail
    private void act1(MatrixStack m, MatrixStack.Entry e, Supplier<VertexConsumer> b, Supplier<VertexConsumer> g, int light, double r, double l, double time) {
        float wag = (float) Math.sin(time * 0.3) * 1.2f;
        box(e, b, -3, 2, -3, 3, 8, 3, PINK, light);                         // body
        box(e, b, -3.5f, 7, 0, 3.5f, 13, 6.5f, PINK, light);                // big head
        box(e, b, -2.6f, 10, 6.5f, -1.4f, 11.8f, 6.8f, BLACK, light);       // eyes
        box(e, b, 1.4f, 10, 6.5f, 2.6f, 11.8f, 6.8f, BLACK, light);
        box(e, g, -2.2f, 10.8f, 6.85f, -1.8f, 11.4f, 7f, WHITE_C, FULL);
        box(e, g, 1.8f, 10.8f, 6.85f, 2.2f, 11.4f, 7f, WHITE_C, FULL);
        box(e, b, -1, 8, 6.5f, 1, 9.6f, 8.5f, GOLD, light);                 // beak
        box(e, b, -5.5f, 10, 1.5f, -3.5f, 18, 3f, PINK_D, light);           // long ears
        box(e, b, 3.5f, 10, 1.5f, 5.5f, 18, 3f, PINK_D, light);
        box(e, b, -2.5f, 13, 1, -1.5f, 15.5f, 2f, PINK_D, light);           // hair spikes
        box(e, b, -0.5f, 13, 2, 0.5f, 16, 3f, PINK_D, light);
        box(e, b, 1.5f, 13, 1, 2.5f, 15.5f, 2f, PINK_D, light);
        box(e, g, -0.9f, 12, 6.5f, 0.9f, 13.4f, 6.9f, STAR, FULL);          // forehead star
        box(e, g, -2.4f, 4, 3.05f, -0.8f, 5.6f, 3.2f, STAR, FULL);          // stars on the body
        box(e, g, 0.8f, 5.5f, 3.05f, 2.4f, 7.1f, 3.2f, STAR, FULL);
        box(e, b, -1, 3, -3 - 5, 1, 5, -3, PINK_D, light);                  // tail
        box(e, b, -2.2f + wag * 0.2f, 3, -11, 2.2f + wag * 0.2f, 6.5f, -8, HEART, light);   // heart tip
        arm(m, b, light, -3.8f, 6, 1.5f, r, 1.8f, 3.2f, 2.2f, PINK, PINK_D);
        arm(m, b, light, 3.8f, 6, 1.5f, l, 1.8f, 3.2f, 2.2f, PINK, PINK_D);
    }

    // ------------------------------------------------------------------ ACT 2: sturdy torso, big arms, two barrels on the head
    private void act2(MatrixStack m, MatrixStack.Entry e, Supplier<VertexConsumer> b, Supplier<VertexConsumer> g, int light, double r, double l) {
        box(e, b, -3, 2, -2, 3, 7, 2, PINK_D, light);                       // lower body
        box(e, b, -4.5f, 6, -3.5f, 4.5f, 16, 3.5f, PINK, light);            // torso
        box(e, b, -2.8f, 15.5f, -2.5f, 2.8f, 21, 3f, PINK, light);          // small head
        box(e, b, -1.8f, 17.3f, 3f, -0.6f, 19f, 3.3f, BLACK, light);        // eyes
        box(e, b, 0.6f, 17.3f, 3f, 1.8f, 19f, 3.3f, BLACK, light);
        box(e, g, -1.4f, 18f, 3.35f, -1f, 18.6f, 3.5f, WHITE_C, FULL);
        box(e, g, 1f, 18f, 3.35f, 1.4f, 18.6f, 3.5f, WHITE_C, FULL);
        box(e, b, -0.8f, 15.6f, 3f, 0.8f, 16.9f, 4.4f, GOLD, light);        // beak
        box(e, b, -6.8f, 16, -2, -3, 20.5f, 1.5f, GREY, light);             // barrel on each side of the head
        box(e, b, 3, 16, -2, 6.8f, 20.5f, 1.5f, GREY, light);
        box(e, b, -3.2f, 8, -7.2f, 3.2f, 15, -3.5f, GREY, light);           // barrel on the back
        box(e, g, -3.4f, 9.2f, -7.3f, -1f, 11.6f, -7.2f, STAR, FULL);
        box(e, g, -2.2f, 8.5f, 3.55f, -0.2f, 10.5f, 3.7f, STAR, FULL);      // stars
        box(e, g, 1f, 11.5f, 3.55f, 3f, 13.5f, 3.7f, STAR, FULL);
        box(e, b, -1.4f, 4, 3.2f, 1.4f, 6.2f, 3.5f, HEART, light);
        arm(m, b, light, -5.8f, 14, 0, r, 3.6f, 7.5f, 4.4f, PINK, PINK_D);
        arm(m, b, light, 5.8f, 14, 0, l, 3.6f, 7.5f, 4.4f, PINK, PINK_D);
    }

    // ------------------------------------------------------------------ ACT 3: humanoid, spiked three-toed feet, disc hands, dark star shoulders, mohawk blade
    private void act3(MatrixStack m, MatrixStack.Entry e, Supplier<VertexConsumer> b, Supplier<VertexConsumer> g, int light, double r, double l) {
        for (int s = -1; s <= 1; s += 2) {
            float x = s * 2.2f;
            box(e, b, x - 1.5f, 1.5f, -1.5f, x + 1.5f, 10, 1.5f, PINK, light);        // legs
            box(e, b, x - 2.2f, 0, -1.5f, x + 2.2f, 1.8f, 4.5f, PINK_D, light);        // triangular foot
            for (int t = -1; t <= 1; t++) box(e, b, x + t * 1.4f - 0.3f, 0, 4.5f, x + t * 1.4f + 0.3f, 1.2f, 5.5f, GOLD, light);   // toe spikes
        }
        box(e, b, -4.6f, 9.5f, -2.6f, 4.6f, 19.5f, 2.6f, PINK, light);               // torso
        box(e, g, -1.3f, 13, 2.65f, 1.3f, 15.6f, 2.8f, STAR, FULL);
        box(e, b, -3.2f, 18.5f, -2.5f, 3.2f, 24.5f, 3f, PINK, light);                // head sunk into the body
        box(e, b, -2.2f, 21, 3f, -0.8f, 22.8f, 3.3f, BLACK, light);
        box(e, b, 0.8f, 21, 3f, 2.2f, 22.8f, 3.3f, BLACK, light);
        box(e, b, -0.6f, 24, -4, 0.6f, 31, 4, GOLD, light);                          // mohawk blade
        box(e, b, -10f, 15.5f, -3f, -4.6f, 21.5f, 3f, LAV, light);                   // spherical shoulders
        box(e, b, 4.6f, 15.5f, -3f, 10f, 21.5f, 3f, LAV, light);
        box(e, g, -9f, 17f, 3.05f, -6f, 20f, 3.2f, STAR, FULL);
        box(e, g, 6f, 17f, 3.05f, 9f, 20f, 3.2f, STAR, FULL);
        arm(m, b, light, -7.3f, 16.5f, 0, r, 2.6f, 7f, 5.2f, PINK, PINK_D);          // arms end in flat disc hands
        arm(m, b, light, 7.3f, 16.5f, 0, l, 2.6f, 7f, 5.2f, PINK, PINK_D);
    }

    // ------------------------------------------------------------------ ACT 4: big and square, face on the chest, vest of ellipses, horseshoe
    private void act4(MatrixStack m, MatrixStack.Entry e, Supplier<VertexConsumer> b, Supplier<VertexConsumer> g, int light, double r, double l) {
        for (int s = -1; s <= 1; s += 2) {
            float x = s * 3.2f;
            box(e, b, x - 2.6f, 0, -3, x + 2.6f, 13, 3, PINK, light);                  // thick legs
            box(e, b, x - 2.8f, 0, -3, x + 2.8f, 1.5f, 5f, PINK_D, light);
        }
        box(e, b, -8.5f, 12, -4.5f, 8.5f, 29, 4.5f, PINK, light);                      // square torso
        box(e, b, -3.8f, 17, 4.5f, 3.8f, 25, 9.5f, PINK, light);                       // the head sits on the chest
        box(e, b, -2.8f, 20.5f, 9.5f, -1f, 23f, 9.8f, BLACK, light);
        box(e, b, 1f, 20.5f, 9.5f, 2.8f, 23f, 9.8f, BLACK, light);
        box(e, g, -2.2f, 21.4f, 9.85f, -1.6f, 22.2f, 10f, WHITE_C, FULL);
        box(e, g, 1.6f, 21.4f, 9.85f, 2.2f, 22.2f, 10f, WHITE_C, FULL);
        box(e, b, -1.4f, 17.5f, 9.5f, 1.4f, 19.4f, 10.4f, HEART, light);               // mouth
        box(e, g, -3f, 24.2f, 9.5f, -2f, 27.8f, 10.5f, GOLD, FULL);                    // horseshoe
        box(e, g, 2f, 24.2f, 9.5f, 3f, 27.8f, 10.5f, GOLD, FULL);
        box(e, g, -3f, 27f, 9.5f, 3f, 28f, 10.5f, GOLD, FULL);
        for (int i = -3; i <= 3; i++) {                                                 // the vest: strung vertical ellipses
            float x = i * 2.2f;
            box(e, b, x - 0.8f, 3, -4.9f, x + 0.8f, 28, -4.5f, i % 2 == 0 ? PINK_D : PINK, light);
            box(e, b, x - 0.8f, 3, 4.5f, x + 0.8f, 11.5f, 4.9f, i % 2 == 0 ? PINK_D : PINK, light);
        }
        box(e, g, -7.4f, 24f, 4.55f, -4.6f, 27f, 4.7f, STAR, FULL);
        box(e, g, 4.6f, 24f, 4.55f, 7.4f, 27f, 4.7f, STAR, FULL);
        arm(m, b, light, -11f, 27, 0, r, 5.6f, 11f, 7.4f, PINK, PINK_D);               // big arms, big fists
        arm(m, b, light, 11f, 27, 0, l, 5.6f, 11f, 7.4f, PINK, PINK_D);
    }
}
