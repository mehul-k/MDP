package exploration;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

import astarpathfinder.FastestPath;
import robot.Robot;
import map.Map;
import config.Constant;
import datastruct.Coordinate;
import datastruct.Obstacle;

public class Exploration {
    private FastestPath fp = new FastestPath();
    private Map map;
    private boolean imageStop;

    public void ExplorationAlgo(Robot robot, int time, int percentage, int speed, boolean image_recognition) {
        map = robot.getMap();

        if ((speed == 1) && (time == -1) && (percentage == 100)) {
            if (image_recognition) {
                imageStop = false;
                imageRecExploration(robot);
            } else {
                normalExploration(robot);
            }
        } else {
            limitedExploration(robot, time, percentage, speed);
        }
        cornerCalibration(robot);
    }

    private void limitedExploration(Robot robot, int time, int percentage, int speed) {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        robot.setDirection(2);

        do {
            if (time != -1) {
                // to account for the time-limited exploration
                int time_taken = (int) stopwatch.getElapsedTime();
                if (time_taken >= time) {
                    return;
                }
            }
            if (percentage != 100) {
                // to account for the coverage-limited exploration
                if (percentComplete(robot) >= percentage) {
                    return;
                }
            }

            System.out.println("Phase 1");
            move(robot, speed, null);
            cornerCalibration(robot);
        } while (!atPosition(robot, Constant.START));

        Coordinate unexplored = nearestUnexplored(robot, robot.getPosition());

        while (unexplored != null) {
            if (time != -1) {
                // to account for the time-limited exploration
                int time_taken = (int) stopwatch.getElapsedTime();
                if (time_taken >= time) {
                    return;
                }
            }
            if (percentage != 100) {
                // to account for the coverage-limited exploration
                if (percentComplete(robot) >= percentage) {
                    return;
                }
            }

            // fastest path to nearest unexplored square
            System.out.println("Phase 2");
            int[] path = fp.FastestPathAlgo(robot, null, unexplored, speed, false, true);
            if ((path == null) || (map.getGrid(unexplored.x, unexplored.y).equals(Constant.UNEXPLORED))) {
                map.setGrid(unexplored.x, unexplored.y, Constant.OBSTACLE);
            }
            unexplored = nearestUnexplored(robot, robot.getPosition());
            robot.updateMap();
        }

        if (!atPosition(robot, Constant.START)) {
            // fastest path to start point
            System.out.println("Phase 3");
            System.out.println(robot.getPosition());
            fp.FastestPathAlgo(robot, null, Constant.START, speed, true, true);
        }

        stopwatch.stop();
        System.out.println("Exploration Complete!");
    }

    private void normalExploration(Robot robot) {
        do {
            move(robot, 1, null);
            // robot.getMap().printDist();
            // robot.right_align();
            cornerCalibration(robot);
        } while (!atPosition(robot, Constant.START));

        Coordinate unexplored = nearestUnexplored(robot, robot.getPosition()); // Returns the
        // System.out.println("Unexplored: " + Arrays.toString(unexplored));
        while (unexplored != null) {
            // fastest path to nearest unexplored square
            System.out.println("Phase 2");
            int[] path = fp.FastestPathAlgo(robot, null, unexplored, 1, false, true);
            if ((path == null) || (map.getGrid(unexplored.x, unexplored.y).equals(Constant.UNEXPLORED))) {
                // No path to the nearest unexplored/Remains unexplored -> Because it is an
                // obstacle
                map.setGrid(unexplored.x, unexplored.y, Constant.OBSTACLE);
            }

            unexplored = nearestUnexplored(robot, robot.getPosition());

            robot.updateMap();
        }

        if (!atPosition(robot, Constant.START)) {
            // fastest path to start point
            System.out.println("Phase 3");
            System.out.println(robot.getPosition());
            fp.FastestPathAlgo(robot, null, Constant.START, 1, true, true);
        }

        System.out.println("Exploration Complete!");
    }

    private void imageRecExploration(Robot robot) {
        Set<Obstacle> checkedObstacles = new HashSet<>();
        // int[][] checkedObstacles = { { 0 } };
        boolean unexplored = false;
        Coordinate needTake = null;
        Coordinate whereToGo = null;
        boolean move = false;

        Coordinate defaultCoord = new Coordinate(-1, -1);
        Obstacle defaultPos = new Obstacle(defaultCoord, -1); // x , y and direction of the robot
        Obstacle[] obsPos = new Obstacle[] { defaultPos, defaultPos, defaultPos }; // 3 x 3 array //use arraylist
        do {
            checkedObstacles = move(robot, 1, checkedObstacles);
            System.out.println("Checked obstacles:");
            for (Obstacle o : checkedObstacles) {
                System.out.printf("x: %d, y: %d, direction: %d, ", o.coordinates.x, o.coordinates.y, o.direction);
            }
            System.out.println();
            // if (this.countOfMoves % 4 == 0) {
            // robot.rightAlign();
            // }
            cornerCalibration(robot);
        } while (!atPosition(robot, Constant.START));

        // Robot is at the start
        cornerCalibration(robot);
        if (!this.imageStop) {
            this.imageStop = robot.captureImage(obsPos);
        }

        // If there is an obstacle whose coordinate isnt checked yet (not including
        // direction), go there
        if (!this.imageStop) {
            // nearest obstacle whose coordinate isnt in checked obstacles
            needTake = pictureTaken(robot, robot.getPosition(), checkedObstacles);
            System.out.println("Need take: " + needTake);
            // go next to the obstacle
            whereToGo = nextToObstacle(robot, needTake);
            System.out.println("Where to go (Picture taken): " + whereToGo);
        }
        // else, go to the nearest unexplored
        if (whereToGo == null) {
            unexplored = true;
            needTake = null;
            whereToGo = nearestUnexplored(robot, robot.getPosition());
            System.out.println("Where to go (Nearest unexplored): " + whereToGo);
        }
        Coordinate goTo = null;
        while ((whereToGo != null) && !(this.imageStop)) {
            System.out.println("Phase 2");
            goTo = new Coordinate(whereToGo.x, whereToGo.y);
            int[] path = fp.FastestPathAlgo(robot, null, goTo, 1, true, true);
            if ((unexplored)
                    && ((path == null) || (map.getGrid(whereToGo.x, whereToGo.y).equals(Constant.UNEXPLORED)))) {
                map.setGrid(whereToGo.x, whereToGo.y, Constant.OBSTACLE);
                checkedObstacles.add(new Obstacle(whereToGo, -1));
            } else { // if not unexplored (pictureTaken) or path not null and grid is not unexplored
                     // (successfully do fastest path)
                System.out.println("Not unexplored (Nearest obstacle)");
                move = obstacleOnRight(robot, needTake); // false if need take is null
                System.out.println("obstacle on right " + move);
            }

            // go in a circle around d block (will be an island if not reachable during the
            // first phase)
            if ((path != null) && move) {
                System.out.println("Valid path + move");
                do {
                    System.out.println("MOVEEEE");
                    checkedObstacles = move(robot, 1, checkedObstacles);
                    // System.out.println(Arrays.deepToString(checkedObstacles));
                } while ((!atPosition(robot, goTo)) && !imageStop);
            }

            imageRecognition(robot, checkedObstacles);

            unexplored = false;
            needTake = pictureTaken(robot, robot.getPosition(), checkedObstacles);
            whereToGo = nextToObstacle(robot, needTake);
            System.out.println("Where to go (Picture taken): " + whereToGo);

            if (whereToGo == null) {
                unexplored = true;
                needTake = null;
                whereToGo = nearestUnexplored(robot, robot.getPosition());
                System.out.println("Where to go (Nearest unexplored): " + whereToGo);
            }

            if (whereToGo == null) {
                // to return to start after each "island"
                fp.FastestPathAlgo(robot, null, Constant.START, 1, true, true);
                cornerCalibration(robot);
            }
            // } else { // means all explored?
            // // to corner calibrate after each "island"
            // fp.FastestPathAlgo(robot, null, nearestCorner(robot), 1, true, true);
            // cornerCalibration(robot);
            // if (!this.imageStop) {
            // this.imageStop = robot.captureImage(obsPos);
            // }
            // }

            robot.updateMap();
        }

        whereToGo = nearestUnexplored(robot, robot.getPosition());
        goTo = new Coordinate(whereToGo.x, whereToGo.y);
        while ((whereToGo != null) && this.imageStop) {
            // fastest path to nearest unexplored square
            System.out.println("Phase 3");
            System.out.println(robot.getPosition());

            int[] path = fp.FastestPathAlgo(robot, null, goTo, 1, false, true);
            if ((path == null) || (map.getGrid(whereToGo.x, whereToGo.y).equals(Constant.UNEXPLORED))) {
                map.setGrid(whereToGo.x, whereToGo.y, Constant.OBSTACLE);
            }
            whereToGo = nearestUnexplored(robot, robot.getPosition());
            robot.updateMap();
        }

        if (!atPosition(robot, Constant.START)) {
            // fastest path to start point
            System.out.println("Phase 4");
            System.out.println(robot.getPosition());
            fp.FastestPathAlgo(robot, null, Constant.START, 1, true, true);
        }

        System.out.println("Exploration Complete!");
    }

    private Set<Obstacle> move(Robot robot, int speed, Set<Obstacle> checkedObstacles) {
        System.out.println(robot.getPosition() + " Direction: " + Constant.DIRECTIONS[robot.getDirection()]);
        robot.updateMap();
        // System.out.println(obstacles);

        if (!connection.ConnectionSocket.checkConnection()) {
            try {
                TimeUnit.SECONDS.sleep(speed);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        // Right Wall Hugging
        if (isRightEmpty(robot)) {
            System.out.println("Right is empty");
            robot.rotateRight();
            if (isFrontEmpty(robot)) {
                System.out.println("Front is empty");
                robot.forward(1);
                return checkedObstacles;
            } else { // Right empty but turn right and front not empty?? (Inaccuracy of sensors?)

                robot.rotateLeft();
                if ((checkedObstacles != null) && (!rightWall(robot))) {
                    checkedObstacles = imageRecognition(robot, checkedObstacles);
                }
            }
        } else if ((checkedObstacles != null) && (!rightWall(robot))) { // right not empty
            checkedObstacles = imageRecognition(robot, checkedObstacles);
        }
        if (isFrontEmpty(robot)) { // Robot is along wall now
            System.out.println("Right not empty but front empty");
            robot.forward(1);
            return checkedObstacles;
        } else {
            System.out.println("Right and Front not empty, turn left!");
            robot.rotateLeft(); // Robot faces north
            if ((checkedObstacles != null) && (!rightWall(robot))) {
                checkedObstacles = imageRecognition(robot, checkedObstacles);
                System.out.println("Here image stop: " + this.imageStop);
            }
        }
        if (isFrontEmpty(robot)) {
            robot.forward(1);
            return checkedObstacles;
        } else {
            robot.rotateLeft(); // Robot reverses
            if ((checkedObstacles != null) && (!rightWall(robot))) {
                checkedObstacles = imageRecognition(robot, checkedObstacles);
            }
        }
        if (isFrontEmpty(robot)) {
            robot.forward(1);
        } else {
            System.out.println("Error during exploration phase 1. All 4 sides blocked.");
        }
        return checkedObstacles;
    }

    private Set<Obstacle> imageRecognition(Robot robot, Set<Obstacle> checkedObstacles) {
        if (this.imageStop) {
            return checkedObstacles;
        }
        System.out.println("Image Recognition Function: " + robot.getPosition() + " Direction: "
                + Constant.DIRECTIONS[robot.getDirection()]);
        int x = robot.getPosition().x;
        int y = robot.getPosition().y;
        int direction = robot.getDirection();
        Coordinate defaultCoord = new Coordinate(-1, -1);
        Obstacle defaultPos = new Obstacle(defaultCoord, -1); // x , y and direction of the robot
        Obstacle[] obsPos = new Obstacle[] { defaultPos, defaultPos, defaultPos }; // 3 x 3 array //use arraylist
        boolean takePic = false;

        // Checking the 3x3 grid on the right of the robot based on where it is facing
        switch (direction) {
        case Constant.NORTH:
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (map.getGrid(x + 2 + j, y - 1 + i).equals(Constant.OBSTACLE)) {
                        Coordinate coordinate = new Coordinate(x + 2 + j, y - 1 + i);
                        obsPos[i] = new Obstacle(coordinate, Constant.NORTH);
                        break;
                    }
                }
            }
            break;
        case Constant.SOUTH:
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (map.getGrid(x - 2 - j, y + 1 - i).equals(Constant.OBSTACLE)) {
                        Coordinate coordinate = new Coordinate(x - 2 - j, y + 1 - i);
                        obsPos[i] = new Obstacle(coordinate, Constant.SOUTH);
                        break;
                    }
                }
            }
            break;
        case Constant.EAST:
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (map.getGrid(x + 1 - i, y + 2 + j).equals(Constant.OBSTACLE)) {
                        Coordinate coordinate = new Coordinate(x + 1 - i, y + 2 + j);
                        obsPos[i] = new Obstacle(coordinate, Constant.EAST);
                        break;
                    }
                }
            }
            break;
        case Constant.WEST:
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (map.getGrid(x - 1 + i, y - 2 - j).equals(Constant.OBSTACLE)) {
                        Coordinate coordinate = new Coordinate(x - 1 + i, y - 2 - j);
                        obsPos[i] = new Obstacle(coordinate, Constant.WEST);
                        break;
                    }
                }
            }
            break;
        }

        // Check if they are the default values or wall or checked obstacles
        for (int k = 0; k < obsPos.length; k++) {
            System.out.printf("Checking obstacle at x: %d, y: %d, direction: %d \n", obsPos[k].coordinates.x,
                    obsPos[k].coordinates.y, obsPos[k].direction);
            if (withinMap(obsPos[k].coordinates.x, obsPos[k].coordinates.y) && !checkedObstacles.contains(obsPos[k])) {
                if (k == 2 || !isFrontEmpty(robot)) {
                    takePic = true;
                }
            } else {
                obsPos[k] = defaultPos;
            }
        }

        if (takePic) {
            for (int k = 0; k < obsPos.length; k++) {
                if (!obsPos[k].equals(defaultPos)) {
                    checkedObstacles.add(obsPos[k]);
                }
            }
            this.imageStop = robot.captureImage(obsPos);
            System.out.println("Image stop: " + this.imageStop);
        }
        System.out.println("Finish image recognition  function");
        return checkedObstacles;
    }

    private boolean withinMap(int x, int y) {
        return (x < Constant.BOARDWIDTH) && (x >= 0) && (y < Constant.BOARDHEIGHT) && (y >= 0);
    }

    private boolean rightWall(Robot robot) {
        int direction = robot.getDirection();
        Coordinate pos = robot.getPosition();
        int posX = pos.x;
        int posY = pos.y;
        switch (direction) {
        case Constant.NORTH:
            return posX == Constant.BOARDWIDTH - 2;
        case Constant.SOUTH:
            return posX == 1;
        case Constant.EAST:
            return posY == Constant.BOARDHEIGHT - 2;
        case Constant.WEST:
            return posY == 1;
        default:
            return true;
        }
    }

    private boolean isRightEmpty(Robot robot) {
        int[] obstacles = robot.updateMap(); // updateMap returns a list of 6 integers -> Converts to a list
                                             // of 6 booleans if obstacles or not for each sensor
        if (obstacles[3] == 1 || obstacles[4] == 1) {
            System.out.println("Both sensors less than threshold");
            return false;
        }
        Coordinate pos = robot.getPosition();
        int posX = pos.x;
        int posY = pos.y;
        int direction = robot.getDirection();
        Map map = robot.getMap();

        switch (direction) { // Detect for two sensors so can move by 2 grid
        case Constant.EAST:
            posY += 2; // Increase y by 2
            break;
        case Constant.WEST:
            posY -= 2; // Decrease y by 2
            break;
        case Constant.SOUTH:
            posX -= 2; // Decrease x by 2
            break;
        case Constant.NORTH:
            posX += 2; // Increase x by 2
            break;
        }
        System.out.println("The middle block is not empty");
        return map.getGrid(posX, posY).equals(Constant.EXPLORED) || map.getGrid(posX, posY).equals(Constant.STARTPOINT)
                || map.getGrid(posX, posY).equals(Constant.ENDPOINT);
    }

    public boolean isFrontEmpty(Robot robot) {
        int[] obstacles = robot.updateMap();
        return (obstacles[0] != 1) && (obstacles[1] != 1) && (obstacles[2] != 1);
    };

    private void cornerCalibration(Robot robot) {
        Coordinate pos = robot.getPosition();
        int posX = pos.x;
        int posY = pos.y;
        if (!(((posX == 1) || (posX == Constant.BOARDWIDTH - 2))
                && ((posY == 1) || (posY == Constant.BOARDHEIGHT - 2)))) {
            return;
        }
        System.out.println("At corner!");
        robot.updateMap();
        int direction = robot.getDirection();
        if ((posX == 1) && (posY == Constant.BOARDHEIGHT - 2)) {
            System.out.println("Bottom left corner");
            switch (direction) {
            case Constant.NORTH:
                robot.rotateRight();
                robot.rotateRight();
                break;
            case Constant.EAST:
                robot.rotateRight();
                break;
            case Constant.WEST:
                robot.rotateLeft();
                break;
            default:
                break;
            }
        } else if ((posX == Constant.BOARDWIDTH - 2) && (posY == Constant.BOARDHEIGHT - 2)) {
            System.out.println("Bottom right corner");
            switch (direction) {
            case Constant.WEST:
                robot.rotateRight();
                robot.rotateRight();
                break;
            case Constant.NORTH:
                robot.rotateRight();
                break;
            case Constant.SOUTH:
                robot.rotateLeft();
                break;
            default:
                break;
            }
        } else if ((posX == Constant.BOARDWIDTH - 2) && (posY == 1)) {
            System.out.println("Top right corner");
            switch (direction) {
            case Constant.SOUTH:
                robot.rotateRight();
                robot.rotateRight();
                break;
            case Constant.WEST:
                robot.rotateRight();
                break;
            case Constant.EAST:
                robot.rotateLeft();
                break;
            default:
                break;
            }
        } else if ((posX == 1) && (posY == 1)) {
            System.out.println("At start");
            switch (direction) {
            case Constant.EAST:
                robot.rotateRight();
                robot.rotateRight();
                break;
            case Constant.SOUTH:
                robot.rotateRight();
                break;
            case Constant.NORTH:
                robot.rotateLeft();
                break;
            default:
                break;
            }
        }
        // robot.calibrate();
        int newDirection = robot.getDirection();
        System.out.println("New direction: " + newDirection);
        switch (Math.abs(direction - newDirection + 4) % 4) {
        case 1:
            robot.rotateRight();
            break;
        case 2:
            robot.rotateRight();
            robot.rotateRight();
            break;
        case 3:
            robot.rotateLeft();
            break;
        }
        System.out.println("Wtf is this " + robot.getDirection());
    }

    // private int[] nearestCorner(Robot robot) {
    // int[] pos = robot.getPosition();
    // int[][] corners = new int[][] { { 1, 1 }, { 1, Constant.BOARDHEIGHT - 2 }, {
    // Constant.BOARDWIDTH - 2, 1 },
    // { Constant.BOARDWIDTH - 2, Constant.BOARDHEIGHT - 2 } };
    // int[] costs = new int[4];
    // int cheapest_index = 0;

    // for (int i = 0; i < 4; i++) {
    // boolean valid = true;
    // int x = corners[i][0];
    // int y = corners[i][1];
    // Map map = robot.getMap();
    // int[][] grid = new int[][] { { x - 1, y - 1 }, { x, y - 1 }, { x + 1, y - 1
    // }, { x - 1, y }, { x, y },
    // { x + 1, y }, { x - 1, y + 1 }, { x, y + 1 }, { x + 1, y + 1 } };
    // for (int[] grids : grid) {
    // if (!map.getGrid(grids[0], grids[1]).equals(Constant.EXPLORED)) {
    // valid = false;
    // }
    // }
    // if (valid) {
    // costs[i] = Math.abs(posX - corners[i][0]) + Math.abs(posY
    // corners[i][1]);
    // if (costs[i] < costs[cheapest_index]) {
    // cheapest_index = i;
    // }
    // }
    // }

    // return corners[cheapest_index];
    // }

    private boolean atPosition(Robot robot, Coordinate goal) {
        Coordinate pos = robot.getPosition();
        return (pos.equals(goal));
    };

    private Coordinate nearestUnexplored(Robot robot, Coordinate start) {
        Map map = robot.getMap();
        int lowestCost = Constant.MAXFCOST;
        Coordinate cheapestPos = null;
        for (int i = 0; i < Constant.BOARDWIDTH; i++) {
            for (int j = 0; j < Constant.BOARDHEIGHT; j++) {
                if (map.getGrid(i, j).equals(Constant.UNEXPLORED)) {
                    int cost = Math.abs(start.x - i) + Math.abs(start.y - j);
                    if (cost < lowestCost) {
                        cheapestPos = new Coordinate(i, j);
                        lowestCost = cost;
                    }
                }
            }
        }
        return cheapestPos;
    }

    private Coordinate nextToObstacle(Robot robot, Coordinate next) {
        if (next == null) {
            return null;
        }
        int x = next.x;
        int y = next.y;
        int[][] order = new int[][] { { x - 1, y - 2 }, { x, y - 2 }, { x + 1, y - 2 }, { x + 2, y - 1 }, { x + 2, y },
                { x + 2, y + 1 }, { x + 1, y + 2 }, { x, y + 2 }, { x - 1, y + 2 }, { x - 2, y + 1 }, { x - 2, y },
                { x - 2, y - 1 } };
        Map map = robot.getMap();

        // will the top left left grid be valid always??
        for (int[] pos : order) {
            if ((withinMap(pos[0], pos[1])) && (map.getGrid(pos[0], pos[1]).equals(Constant.EXPLORED))
                    && (isValid(robot, pos[0], pos[1]))) {
                return new Coordinate(pos[0], pos[1]);
            }
        }
        return null;
    }

    private boolean isValid(Robot robot, int posX, int posY) {
        Map map = robot.getMap();
        int x = posX;
        int y = posY;
        int[][] robotPos = { { x - 1, y + 1 }, { x, y + 1 }, { x + 1, y + 1 }, { x - 1, y }, { x, y }, { x + 1, y },
                { x - 1, y - 1 }, { x, y - 1 }, { x + 1, y - 1 } };

        if ((x > 0) && (x < Constant.BOARDWIDTH - 1) && (y > 0) && (y < Constant.BOARDHEIGHT - 1)) {
            for (int[] coordinates : robotPos) {
                if (map.getGrid(coordinates[0], coordinates[1]).equals(Constant.OBSTACLE)
                        || map.getGrid(coordinates[0], coordinates[1]).equals(Constant.UNEXPLORED)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private Coordinate pictureTaken(Robot robot, Coordinate start, Set<Obstacle> checkedObstacles) {
        Map map = robot.getMap();
        int lowestCost = Constant.MAXFCOST;
        int x = 0;
        int y = 0;

        Set<Coordinate> coordinates = checkedObstacles.stream().map(obstacle -> obstacle.coordinates)
                .collect(Collectors.toSet());

        for (int i = 0; i < Constant.BOARDWIDTH; i++) {
            for (int j = 0; j < Constant.BOARDHEIGHT; j++) {
                if (map.getGrid(i, j).equals(Constant.OBSTACLE)) {
                    // boolean not_inside = true;
                    if (!coordinates.contains(new Coordinate(i, j))) {
                        // not_inside = false;
                        int cost = Math.abs(start.x - i) + Math.abs(start.y - j);
                        if (cost < lowestCost) {
                            // cheapestPos = new Coordinate(i, j);
                            x = i;
                            y = j;
                            lowestCost = cost;
                        }
                    }
                }
            }
        }
        return new Coordinate(x, y);
    }

    private boolean obstacleOnRight(Robot robot, Coordinate obstacle) {
        if (obstacle == null) {
            return false;
        }
        int direction = robot.getDirection();
        Coordinate pos = robot.getPosition();
        int posX = pos.x;
        int posY = pos.y;
        System.out.printf("Obstacle on right: x: %d, y: %d, direction: %d", posX, posY, direction);
        switch (direction) {
        case Constant.NORTH:
            if (obstacle.x == (posX + 2)) { // original is posX - 2??
                System.out.println("First case");
                break;
            } else if (obstacle.y == (posY + 2)) {
                System.out.println("Second case");
                robot.rotateRight();
                break;
            } else if (obstacle.y == (posY - 2)) {
                System.out.println("Third case");
                robot.rotateLeft();
                break;
            } else {
                System.out.println("Fourth case");
                robot.rotateRight();
                robot.rotateRight();
                break;
            }
        case Constant.EAST:
            if (obstacle.y == (posY + 2)) {
                System.out.println("First case");
                break;
            } else if (obstacle.x == (posX - 2)) {
                System.out.println("Second case");
                robot.rotateRight();
                break;
            } else if (obstacle.x == (posX + 2)) {
                System.out.println("Third case");
                robot.rotateLeft();
                break;
            } else {
                System.out.println("Fourth case");
                robot.rotateRight();
                robot.rotateRight();
                break;
            }
        case Constant.SOUTH:
            if (obstacle.x == (posX - 2)) { // why not posX + 2??
                System.out.println("First case");
                break;
            } else if (obstacle.y == (posY - 2)) {
                System.out.println("Second case");
                robot.rotateRight();
                break;
            } else if (obstacle.y == (posY + 2)) {
                System.out.println("Third case");
                robot.rotateLeft();
                break;
            } else {
                System.out.println("Fourth case");
                robot.rotateRight();
                robot.rotateRight();
                break;
            }
        case Constant.WEST:
            if (obstacle.y == (posY - 2)) { // orig is posY
                System.out.println("First case");
                break;
            } else if (obstacle.x == (posX + 2)) {
                System.out.println("Second case");
                robot.rotateRight();
                break;
            } else if (obstacle.x == (posX - 2)) {
                System.out.println("Third case");
                robot.rotateLeft();
                break;
            } else {
                System.out.println("Fourth case");
                robot.rotateRight();
                robot.rotateRight();
                break;
            }
        }
        return true;
    }

    private int percentComplete(Robot robot) {
        Map map = robot.getMap();
        int unexplored = 0;
        for (int i = 0; i < Constant.BOARDWIDTH; i++) {
            for (int j = 0; j < Constant.BOARDHEIGHT; j++) {
                if (map.getGrid(i, j).equals(Constant.UNEXPLORED)) {
                    unexplored++;
                }
            }
        }
        return ((300 - unexplored) / 3);
    }
}