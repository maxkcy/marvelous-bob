package com.marvelousbob.common.utils.movements;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Assumes a linear path between node of the path
 */
public class PathMovement implements MovementStrategy<Vector2> {

    private Array<Vector2> path;
    private int current, next;
    private ConstantSpeed constantSpeed;

    private PathMovement(Array<Vector2> path) {
        if (path.size < 2) {
            throw new RuntimeException("Path must contains at least two points.");
        }
        this.path = path;
        this.current = 0;
        this.next = 1;
        this.constantSpeed = new ConstantSpeed(path.get(0), path.get(1));

    }

    public static MovementStrategy<Vector2> from(Array<Vector2> path) {
        if (path == null || path.isEmpty()) {
            return new InstantMovement<>();
        }
        if (path.size == 1) {
            return new StayAt<>(path.get(0));
        }
        return new PathMovement(path);
    }


    @Override
    public Vector2 move(Vector2 pos, float distanceToMove) {
        if (distanceToMove <= 0) {
            return pos;
        }

        float distLeft = pos.dst(path.get(next));
        if (distanceToMove <= distLeft) {
            return constantSpeed.move(pos, distanceToMove);
        }
        // here, distance to travel is greater than the distance
        // remaining between current and next nodes

        // are already we at the end ?
        if (next == path.size - 1) {
            return path.get(next);
        }

        constantSpeed.set(path.get(++current), path.get(++next));
        return move(path.get(current), distanceToMove - distLeft);

    }

}
