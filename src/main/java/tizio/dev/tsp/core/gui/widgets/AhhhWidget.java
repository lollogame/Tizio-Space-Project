package tizio.dev.tsp.core.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import tizio.dev.tsp.core.utils.Materials;

public class AhhhWidget extends AbstractWidget {

    private static final int LOGO_TEXTURE_WIDTH = 256;
    private static final int LOGO_TEXTURE_HEIGHT = 256;
    private static final double CORNER_CLICK_SIZE = 22.0D;
    private static final double MAX_DELTA_TIME = 0.05D;
    private static final double GRAVITY = 900.0D;
    private static final double PENDULUM_DAMPING = 0.997D;
    private static final double AIR_DAMPING = 0.994D;
    private static final double ANGULAR_DAMPING = 0.985D;
    private static final double GROUND_RESTITUTION = 0.18D;
    private static final double GROUND_FRICTION = 0.72D;

    private final double initialCenterX;
    private final double initialCenterY;

    private double centerX;
    private double centerY;
    private double velocityX;
    private double velocityY;

    private double rotationZ;
    private double angularVelocityZ;

    private double pivotX;
    private double pivotY;

    private Attachment attachedCorner = Attachment.BOTH;
    private long lastUpdateNanos;
    private boolean sleeping = true;

    public AhhhWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.initialCenterX = x + width / 2.0D;
        this.initialCenterY = y + height / 2.0D;
        this.centerX = this.initialCenterX;
        this.centerY = this.initialCenterY;
        this.lastUpdateNanos = System.nanoTime();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        Materials.AnimatedGif secretGif = Materials.getSecretGif();
        if (secretGif != null && secretGif.isLoaded()) {
            secretGif.update();

            float gifScale = 1.0F;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(this.initialCenterX, this.initialCenterY, 0.0F);
            guiGraphics.pose().scale(gifScale, gifScale, 1.0F);
            guiGraphics.pose().translate(-this.width / 2.0F, -this.height / 2.0F, -1.0F);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            guiGraphics.blit(
                    secretGif.getLocation(),
                    0, 0,
                    0, 0,
                    this.width, this.height,
                    this.width, this.height
            );

            RenderSystem.disableBlend();
            guiGraphics.pose().popPose();
        }

        updatePhysics();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.centerX, this.centerY, 0.0F);
        guiGraphics.pose().mulPose(Axis.ZP.rotation((float) this.rotationZ));
        guiGraphics.pose().translate(-this.width / 2.0F, -this.height / 2.0F, 0.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(
                Materials.LOGO,
                0,
                0,
                this.width,
                this.height,
                0.0F,
                0.0F,
                LOGO_TEXTURE_WIDTH,
                LOGO_TEXTURE_HEIGHT,
                LOGO_TEXTURE_WIDTH,
                LOGO_TEXTURE_HEIGHT
        );
        RenderSystem.disableBlend();

        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        if (this.attachedCorner == Attachment.NONE) {
            return false;
        }

        if (this.attachedCorner == Attachment.BOTH) {
            if (isCornerClicked(mouseX, mouseY, true)) {
                wakeUp();
                attach(Attachment.RIGHT);
                return true;
            }
            if (isCornerClicked(mouseX, mouseY, false)) {
                wakeUp();
                attach(Attachment.LEFT);
                return true;
            }
            return false;
        }

        if (this.attachedCorner == Attachment.LEFT && isCornerClicked(mouseX, mouseY, true)) {
            wakeUp();
            release();
            return true;
        }

        if (this.attachedCorner == Attachment.RIGHT && isCornerClicked(mouseX, mouseY, false)) {
            wakeUp();
            release();
            return true;
        }

        return false;
    }

    private void wakeUp() {
        if (this.sleeping) {
            this.sleeping = false;
            this.lastUpdateNanos = System.nanoTime();
        }
    }

    private void attach(Attachment pivotCorner) {
        this.attachedCorner = pivotCorner;
        this.pivotX = pivotCorner == Attachment.LEFT ? this.getX() : this.getX() + this.width;
        this.pivotY = this.getY();

        this.angularVelocityZ = pivotCorner == Attachment.LEFT ? 2.2D : -2.2D;
        updateAttachedPosition();
    }

    private void release() {
        double halfWidth = this.width / 2.0D;
        double halfHeight = this.height / 2.0D;
        double cos = Math.cos(this.rotationZ);
        double sin = Math.sin(this.rotationZ);

        double localX = this.attachedCorner == Attachment.LEFT ? -halfWidth : halfWidth;
        double localY = -halfHeight;

        double rotatedX = localX * cos - localY * sin;
        double rotatedY = localX * sin + localY * cos;

        double radiusX = -rotatedX;
        double radiusY = -rotatedY;

        this.centerX = this.pivotX + radiusX;
        this.centerY = this.pivotY + radiusY;

        this.velocityX = -radiusY * this.angularVelocityZ;
        this.velocityY = radiusX * this.angularVelocityZ + 25.0D;

        this.attachedCorner = Attachment.NONE;
    }

    private boolean isCornerClicked(double mouseX, double mouseY, boolean isLeft) {
        double localX = mouseX - this.centerX;
        double localY = mouseY - this.centerY;

        double cos = Math.cos(this.rotationZ);
        double sin = Math.sin(this.rotationZ);

        double rotatedLocalX = localX * cos + localY * sin;
        double rotatedLocalY = -localX * sin + localY * cos;

        double halfWidth = this.width / 2.0D;
        double halfHeight = this.height / 2.0D;
        double cornerX = isLeft ? -halfWidth : halfWidth;

        double minX = isLeft
                ? cornerX - 2.0D
                : cornerX - CORNER_CLICK_SIZE - 2.0D;
        double maxX = isLeft
                ? cornerX + CORNER_CLICK_SIZE + 2.0D
                : cornerX + 2.0D;

        return rotatedLocalX >= minX
                && rotatedLocalX <= maxX
                && rotatedLocalY >= -halfHeight - 2.0D
                && rotatedLocalY <= -halfHeight + CORNER_CLICK_SIZE + 2.0D;
    }

    private void updatePhysics() {
        if (this.sleeping) {
            return;
        }

        long now = System.nanoTime();
        double deltaTime = Math.min((now - this.lastUpdateNanos) / 1_000_000_000.0D, MAX_DELTA_TIME);
        this.lastUpdateNanos = now;

        if (this.attachedCorner == Attachment.BOTH) {
            this.centerX = this.initialCenterX;
            this.centerY = this.initialCenterY;
            this.rotationZ = 0.0D;
            this.angularVelocityZ = 0.0D;
            this.sleeping = true;
            return;
        }

        if (this.attachedCorner == Attachment.LEFT || this.attachedCorner == Attachment.RIGHT) {
            updateAttachedPhysics(deltaTime);
            return;
        }

        this.velocityY += GRAVITY * deltaTime;
        this.velocityX *= Math.pow(AIR_DAMPING, deltaTime * 60.0D);
        this.velocityY *= Math.pow(AIR_DAMPING, deltaTime * 60.0D);
        this.angularVelocityZ *= Math.pow(ANGULAR_DAMPING, deltaTime * 60.0D);

        this.centerX += this.velocityX * deltaTime;
        this.centerY += this.velocityY * deltaTime;
        this.rotationZ += this.angularVelocityZ * deltaTime;

        resolveScreenCollisions(deltaTime);
    }

    private void updateAttachedPhysics(double deltaTime) {
        double halfWidth = this.width / 2.0D;
        double halfHeight = this.height / 2.0D;
        double radiusSquared = halfWidth * halfWidth + halfHeight * halfHeight;

        double cos = Math.cos(this.rotationZ);
        double sin = Math.sin(this.rotationZ);
        double localX = this.attachedCorner == Attachment.LEFT ? -halfWidth : halfWidth;
        double localY = -halfHeight;

        double rotatedX = localX * cos - localY * sin;
        double radiusX = -rotatedX;

        double angularAcceleration = GRAVITY * radiusX / radiusSquared;
        this.angularVelocityZ += angularAcceleration * deltaTime;
        this.angularVelocityZ *= Math.pow(PENDULUM_DAMPING, deltaTime * 60.0D);
        this.rotationZ += this.angularVelocityZ * deltaTime;

        if (Math.abs(this.angularVelocityZ) < 0.005D && Math.abs(angularAcceleration) < 5.0D) {
            this.angularVelocityZ = 0.0D;
            this.sleeping = true;
        }

        updateAttachedPosition();
    }

    private void updateAttachedPosition() {
        double halfWidth = this.width / 2.0D;
        double halfHeight = this.height / 2.0D;
        double localX = this.attachedCorner == Attachment.LEFT ? -halfWidth : halfWidth;
        double localY = -halfHeight;
        double cos = Math.cos(this.rotationZ);
        double sin = Math.sin(this.rotationZ);

        double rotatedX = localX * cos - localY * sin;
        double rotatedY = localX * sin + localY * cos;

        this.centerX = this.pivotX - rotatedX;
        this.centerY = this.pivotY - rotatedY;
    }

    private void resolveScreenCollisions(double deltaTime) {
        double screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        double screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        double halfWidth = this.width / 2.0D;
        double halfHeight = this.height / 2.0D;

        double cos = Math.cos(this.rotationZ);
        double sin = Math.sin(this.rotationZ);

        double horizontalExtent = Math.abs(halfWidth * cos) + Math.abs(halfHeight * sin);
        double verticalExtent = Math.abs(halfWidth * sin) + Math.abs(halfHeight * cos);


        if (this.centerX - horizontalExtent < 0) {
            this.centerX = horizontalExtent;
            this.velocityX = Math.abs(this.velocityX) * GROUND_RESTITUTION;
            this.angularVelocityZ *= 0.82D;
        } else if (this.centerX + horizontalExtent > screenWidth) {
            this.centerX = screenWidth - horizontalExtent;
            this.velocityX = -Math.abs(this.velocityX) * GROUND_RESTITUTION;
            this.angularVelocityZ *= 0.82D;
        }

        if (this.centerY - verticalExtent < 0) {
            this.centerY = verticalExtent;
            this.velocityY = Math.abs(this.velocityY) * GROUND_RESTITUTION;
            this.angularVelocityZ *= 0.82D;
        } else if (this.centerY + verticalExtent >= screenHeight) {
            this.centerY = screenHeight - verticalExtent;

            this.rotationZ %= (Math.PI * 2);
            if (this.rotationZ > Math.PI) this.rotationZ -= Math.PI * 2;
            if (this.rotationZ < -Math.PI) this.rotationZ += Math.PI * 2;

            if (Math.abs(this.velocityY) > 35.0D) {
                this.velocityY = -this.velocityY * GROUND_RESTITUTION;
                this.velocityX *= GROUND_FRICTION;
                this.angularVelocityZ *= 0.82D;
            } else {
                this.velocityY = 0.0D;
                this.velocityX *= 0.86D;
                this.angularVelocityZ *= 0.76D;

                double halfPi = Math.PI / 2.0D;
                double targetRotation = Math.round(this.rotationZ / halfPi) * halfPi;
                double deltaRotation = this.rotationZ - targetRotation;

                double alignForce = -deltaRotation * 100.0D;
                this.angularVelocityZ += alignForce * deltaTime;

                if (Math.abs(this.velocityX) < 2.0D) {
                    this.velocityX = 0.0D;
                }

                if (Math.abs(this.angularVelocityZ) < 0.2D && Math.abs(deltaRotation) < 0.05D) {
                    this.angularVelocityZ = 0.0D;
                    this.rotationZ = targetRotation;
                    this.velocityX = 0.0D;
                    this.velocityY = 0.0D;
                    this.sleeping = true;
                }
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    private enum Attachment {
        BOTH,
        LEFT,
        RIGHT,
        NONE
    }
}
