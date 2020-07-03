package com.marvelousbob.server.worlds;

import static com.marvelousbob.common.network.constants.GameConstant.BLOCKS_X;
import static com.marvelousbob.common.network.constants.GameConstant.BLOCKS_Y;
import static com.marvelousbob.common.network.constants.GameConstant.PIXELS_PER_GRID_CELL;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.marvelousbob.common.model.entities.level.EnemySpawnPoint;
import com.marvelousbob.common.model.entities.level.Level;
import com.marvelousbob.common.model.entities.level.PlayersBase;
import com.marvelousbob.common.model.entities.level.Wall;
import com.marvelousbob.common.network.constants.GameConstant;
import com.marvelousbob.common.utils.Tuple2;
import com.marvelousbob.common.utils.UUID;
import com.marvelousbob.server.factories.WallFactory;
import com.marvelousbob.server.factories.WallFactory.Headed;
import com.marvelousbob.server.factories.WallFactory.Orientation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.mapping.GrowingTreeMazeGenerator;
import squidpony.squidmath.GreasedRegion;
import squidpony.squidmath.StatefulRNG;

@Slf4j
public class ProceduralLevelGenerator implements LevelGenerator {

    private final WallFactory wallFactory;

    private static final float HALF_GRID_CELL = .5f;

    private static final Map<Character, Tuple2<Boolean, Boolean>> LEFT_RIGHT_CONTINUUM = new HashMap<>();
    private static final Map<Character, Tuple2<Boolean, Boolean>> TOP_BOTTOM_CONTINUUM = new HashMap<>();

    static {
        // ─ │ ┌ ┐ └ ┘ ├ ┤ ┬ ┴ ┼ .

        LEFT_RIGHT_CONTINUUM.put('│', new Tuple2<>(false, false));
        LEFT_RIGHT_CONTINUUM.put('─', new Tuple2<>(true, true));
        LEFT_RIGHT_CONTINUUM.put('┌', new Tuple2<>(false, true));
        LEFT_RIGHT_CONTINUUM.put('┐', new Tuple2<>(true, false));
        LEFT_RIGHT_CONTINUUM.put('└', new Tuple2<>(false, true));
        LEFT_RIGHT_CONTINUUM.put('┘', new Tuple2<>(true, false));
        LEFT_RIGHT_CONTINUUM.put('├', new Tuple2<>(false, true));
        LEFT_RIGHT_CONTINUUM.put('┤', new Tuple2<>(true, false));
        LEFT_RIGHT_CONTINUUM.put('┬', new Tuple2<>(true, true));
        LEFT_RIGHT_CONTINUUM.put('┴', new Tuple2<>(true, true));
        LEFT_RIGHT_CONTINUUM.put('┼', new Tuple2<>(true, true));
        LEFT_RIGHT_CONTINUUM.put('.', new Tuple2<>(false, false));

        TOP_BOTTOM_CONTINUUM.put('│', new Tuple2<>(true, true));
        TOP_BOTTOM_CONTINUUM.put('─', new Tuple2<>(false, false));
        TOP_BOTTOM_CONTINUUM.put('┌', new Tuple2<>(false, true));
        TOP_BOTTOM_CONTINUUM.put('┐', new Tuple2<>(false, true));
        TOP_BOTTOM_CONTINUUM.put('└', new Tuple2<>(true, false));
        TOP_BOTTOM_CONTINUUM.put('┘', new Tuple2<>(true, false));
        TOP_BOTTOM_CONTINUUM.put('├', new Tuple2<>(true, true));
        TOP_BOTTOM_CONTINUUM.put('┤', new Tuple2<>(true, true));
        TOP_BOTTOM_CONTINUUM.put('┬', new Tuple2<>(false, true));
        TOP_BOTTOM_CONTINUUM.put('┴', new Tuple2<>(true, false));
        TOP_BOTTOM_CONTINUUM.put('┼', new Tuple2<>(true, true));
        TOP_BOTTOM_CONTINUUM.put('.', new Tuple2<>(false, false));
    }

    public ProceduralLevelGenerator() {
        this.wallFactory = new WallFactory();
    }

    @Override
    public Level getLevel() {
//        char[][] grid = generateGrid(123L);
        char[][] grid = generateGrid(MathUtils.random(Long.MAX_VALUE));

        var walls = getWalls(grid);
        var emptyCells = findEmptyCells(grid);
//        var bases = placeBases(emptyCells);
//        var spawns = placeSpawns(emptyCells);

        final ConcurrentHashMap<UUID, PlayersBase> bases = new ConcurrentHashMap<>();
        UUID baseUuid = UUID.getNext();
        var base = PlayersBase.hexagonalPlayerBase(baseUuid, randomFreePos(emptyCells), 15);
        bases.put(baseUuid, base);

        final ConcurrentHashMap<UUID, EnemySpawnPoint> spawns = new ConcurrentHashMap<>();
        UUID spawnUuid1 = UUID.getNext();
        UUID spawnUuid2 = UUID.getNext();
        spawns.put(spawnUuid1,
                EnemySpawnPoint.starShaped(spawnUuid1, randomFreePos(emptyCells), 10));
        spawns.put(spawnUuid2,
                EnemySpawnPoint.starShaped(spawnUuid2, randomFreePos(emptyCells), 10));

        return new Level(bases, spawns, walls, BLOCKS_X, BLOCKS_Y, PIXELS_PER_GRID_CELL);
    }

    private Vector2 randomFreePos(ArrayList<Vector2> emptyCells) {
        var vec2 = emptyCells.get(MathUtils.random(emptyCells.size() - 1));
        return new Vector2(vec2.x * GameConstant.PIXELS_PER_GRID_CELL,
                vec2.y * GameConstant.PIXELS_PER_GRID_CELL);
    }

    private ArrayList<Wall> getWalls(char[][] grid) {
        var horizWalls = buildHorizontalWalls(grid);
        var vertWalls = buildVerticalWalls(grid);
        final ArrayList<Wall> walls = new ArrayList<>();
        walls.addAll(horizWalls);
        walls.addAll(vertWalls);
        return walls;
    }

    /**
     * Builds continuous horizontal walls with a single pass.
     */
    private ArrayList<Wall> buildHorizontalWalls(char[][] grid) {
        final ArrayList<Wall> horizWalls = new ArrayList<>();

        for (int y = 0; y < grid[0].length; y++) {

            List<Integer> halfCoords = new ArrayList<>(); // example: (3, 6) -> [1.5, 3] in grid
            boolean continuingAWall = false; // we never continue a wall when we begin

            for (int x = 0; x < grid.length; x++) {
                System.out.println(grid[x][y]);
                continuingAWall = updateWallAccumulator(x, halfCoords, continuingAWall,
                        LEFT_RIGHT_CONTINUUM.get(grid[x][y]));
            }

            if (continuingAWall) { // very last half was a filler (shouldn't happen)
                halfCoords.add(2 * y + 2);
            }

            /* Reconstructing and adding actual Walls from the extracted coordinates. */
            int tmpSize = halfCoords.size();
            System.out.println(halfCoords);
            assert (tmpSize % 2 == 0); // should be even number of coords since they are pairs
            while (tmpSize != 0) {
                int firstCoord = halfCoords.get(--tmpSize);
                int secondCoord = halfCoords.get(--tmpSize);
                int length = firstCoord - secondCoord;
                if (length == 0) { // possible edge-case when trimming happened on a lonely wall
                    continue;
                }
                horizWalls.add(buildGridWall(Orientation.HORIZONTAL, Headed.RIGHT,
                        secondCoord / 2f, GameConstant.BLOCKS_Y - (float) y - HALF_GRID_CELL,
                        length / 2f));
            }
        }

        return horizWalls;
    }

    /**
     * Builds continuous vertical walls with a single pass.
     */
    private ArrayList<Wall> buildVerticalWalls(char[][] grid) {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nSTARTING VERTICALS!!!");
        final ArrayList<Wall> vertWalls = new ArrayList<>();

        for (int x = 0; x < grid.length; x++) {

            List<Integer> halfCoords = new ArrayList<>(); // example: (3, 6) -> [1.5, 3] in grid
            boolean continuingAWall = false; // we never continue a wall when we begin

            for (int y = 0; y < grid[0].length; y++) {
                System.out.println(grid[x][y]);
                continuingAWall = updateWallAccumulator(y, halfCoords, continuingAWall,
                        TOP_BOTTOM_CONTINUUM.get(grid[x][y]));
            }

            if (continuingAWall) { // very last half was a filler
                halfCoords.add(2 * x + 2);
            }

            /* Reconstructing and adding actual Walls from the extracted coordinates. */
            System.out.println(halfCoords);
            int tmpSize = halfCoords.size();
            assert (tmpSize % 2 == 0); // should be even number of coords since they are pairs
            while (tmpSize != 0) {
                int firstCoord = halfCoords.get(--tmpSize);
                int secondCoord = halfCoords.get(--tmpSize);
                int length = firstCoord - secondCoord;
                if (length == 0) { // possible edge-case when trimming happened on a lonely wall
                    continue;
                }
                vertWalls.add(buildGridWall(Orientation.VERTICAL, Headed.BOTTOM,
                        x + HALF_GRID_CELL, GameConstant.BLOCKS_Y - secondCoord / 2f,
                        length / 2f));
            }
        }

        return vertWalls;
    }

    private ArrayList<Vector2> findEmptyCells(char[][] grid) {
        final ArrayList<Vector2> emptyCells = new ArrayList<>();
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[0].length; y++) {
                if (grid[x][y] == '.') {
                    emptyCells.add(new Vector2(x, y));
                }
            }
        }
        return emptyCells;
    }

    private ArrayList<Vector2> placeBases(ArrayList<Vector2> emptyCells) {
        final ArrayList<Vector2> bases = new ArrayList<>();
        // todo
        return bases;
    }

    private ArrayList<Vector2> placeSpawns(ArrayList<Vector2> emptyCells) {
        final ArrayList<Vector2> spawns = new ArrayList<>();
        // todo
        return spawns;
    }

    private boolean updateWallAccumulator(int gridCoord, List<Integer> halfCoords,
            boolean continuingAWall,
            Tuple2<Boolean, Boolean> currentBools) {
        log.info("First: " + currentBools.getFirst());
        continuingAWall = updateWallContinuation(halfCoords, continuingAWall,
                currentBools.getFirst(), true, 2 * gridCoord);
        log.info("Second: " + currentBools.getSecond());
        continuingAWall = updateWallContinuation(halfCoords, continuingAWall,
                currentBools.getSecond(), false, 2 * gridCoord + 1);
        return continuingAWall;
    }

    private boolean updateWallContinuation(List<Integer> halfCoords, boolean continuingAWall,
            boolean tupleBool, boolean isFirstOfTuple, int halfCoord) {
        if (!continuingAWall && tupleBool) { // begin new wall
            halfCoords.add(isFirstOfTuple ? halfCoord + 1 : halfCoord); // trim edges
            log.info("Begun a wall");
        }
        if (continuingAWall && !tupleBool) { // end a wall
            halfCoords.add(isFirstOfTuple ? halfCoord - 1 : halfCoord); // trim edges
            log.info("Ended a wall");
        }
        continuingAWall = tupleBool;
        return continuingAWall;
    }

    private Wall buildGridWall(Orientation orientation, Headed headed, float x, float y,
            float length) {
        System.out.println();
        log.info("input: x={}, y={}, L={}", x, y, length);
        x *= GameConstant.PIXELS_PER_GRID_CELL;
        y *= GameConstant.PIXELS_PER_GRID_CELL;
        length *= GameConstant.PIXELS_PER_GRID_CELL;
        Vector2 pos = new Vector2(x, y);
        log.info("output: x={}, y={}, L={}, pos={}", x, y, length, pos);
        return wallFactory.buildBlendedWall(orientation, headed, pos, length);
    }

    /**
     * The grid consists of the following chars: {@code ─ │ ┌ ┐ └ ┘ ├ ┤ ┬ ┴ ┼}. A dot ({@code .}) is
     * an empty cell.
     * <p>
     * {@code grid[width - 1][0]} is the rightmost char of the first row.
     *
     * @param seed inputting twice the same seed will result in the same map being generated.
     * @return 2D array as grid[x][y] with [0][0] being the top-left of the screen.
     * @author TEttinger
     */
    public char[][] generateGrid(long seed) {
        log.warn("Starting procedural generation of a new level. Seed: " + seed);

        StatefulRNG rng = new StatefulRNG(seed); // change seed to change level
        GrowingTreeMazeGenerator mazeGenerator =
                new GrowingTreeMazeGenerator(BLOCKS_X, GameConstant.BLOCKS_Y, rng);
        char[][] map = mazeGenerator.generate();
        GreasedRegion walls = new GreasedRegion(map, '#');
        GreasedRegion temp = walls.copy();
        walls.deteriorate(rng, 0.85);
        walls.or(temp.refill(mazeGenerator.generate(), '#').deteriorate(rng, 0.85));
        walls.andNot(walls.copy().neighborDownRight()).removeIsolated().not().removeEdges()
                .intoChars(map, '.', '#');
        char[][] grid = DungeonUtility.hashesToLines(map);

        DungeonUtility.debugPrint(grid);
        log.info("Finished generating a level.");

        return grid;
    }
}
