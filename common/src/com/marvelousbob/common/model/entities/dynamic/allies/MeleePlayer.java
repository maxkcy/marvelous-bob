package com.marvelousbob.common.model.entities.dynamic.allies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.marvelousbob.common.network.register.dto.PlayerDto;
import com.marvelousbob.common.utils.UUID;
import lombok.NoArgsConstructor;
import lombok.ToString;
import space.earlygrey.shapedrawer.ShapeDrawer;

@NoArgsConstructor
@ToString(callSuper = true)
public class MeleePlayer extends Player {

    private static final float ROD_SIZE = 3;

    public MeleePlayer(PlayerDto playerDto) {
        super(playerDto);
    }

    public MeleePlayer(UUID uuid, Color color, Vector2 initCenterPos) {
        super(100, 100, 0, color, 40, 20, initCenterPos, uuid);
    }

    private void drawRod(ShapeDrawer shapeDrawer) {
        shapeDrawer.setColor(Color.RED); // whatever weapon color

        float centerX, centerY, radiusX, radiusY;
        if (mouseAngleRelativeToCenter >= 45 && mouseAngleRelativeToCenter < 135) { // top
            centerX = getCurrCenterX();
            centerY = getCurrCenterY() + getHalfSize();
            radiusX = getHalfSize();
            radiusY = ROD_SIZE;
        } else if (mouseAngleRelativeToCenter >= 135 && mouseAngleRelativeToCenter < 225) { // left
            centerX = getCurrCenterX() - getHalfSize();
            centerY = getCurrCenterY();
            radiusX = ROD_SIZE;
            radiusY = getHalfSize();
        } else if (mouseAngleRelativeToCenter >= 225
                && mouseAngleRelativeToCenter < 315) { // bottom
            centerX = getCurrCenterX();
            centerY = getCurrCenterY() - getHalfSize();
            radiusX = getHalfSize();
            radiusY = ROD_SIZE;
        } else { // right
            centerX = getCurrCenterX() + getHalfSize();
            centerY = getCurrCenterY();
            radiusX = ROD_SIZE;
            radiusY = getHalfSize();
        }

        shapeDrawer.filledEllipse(centerX, centerY, radiusX, radiusY);
        shapeDrawer.setColor(this.color); // revert back to original
    }

    @Override
    public void drawMe(ShapeDrawer shapeDrawer) {
        shapeDrawer.setColor(this.color);
        shapeDrawer.rectangle(getCurrCenterX() - size / 2, getCurrCenterY() - size / 2, size, size);
        drawRod(shapeDrawer);
    }
}