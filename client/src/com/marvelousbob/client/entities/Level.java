package com.marvelousbob.client.entities;

import com.badlogic.gdx.math.Intersector;
import lombok.Data;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.util.List;

@Data
public class Level implements Drawable {

    private final Intersector intersector;

    private final List<Base> base;
    private final List<EnemySpawnPoint> enemySpawnPoints;
    private final List<Wall> walls;


    @Override
    public void drawMe(ShapeDrawer shapeDrawer) {
        base.forEach(b -> b.drawMe(shapeDrawer));
        walls.forEach(w -> w.drawMe(shapeDrawer));
        enemySpawnPoints.forEach(es -> es.drawMe(shapeDrawer));
    }

}
