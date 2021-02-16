package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {

        //Select
        if(gameState.myPlayer.remainingWormSelections>0) {
            Worm enemyWorm1 = getClosestWorm();
            return new SelectCommand(GetWorm(1), resolveDirection(GetWorm(1).position, enemyWorm1.position));
        }

        Position target;
        //BananaBomb
        target = canBananaBomb();
        if (target.x != -999){
            return new BananaCommand(target.x, target.y);
        }
        //snowball
        target = canSnowball();
        if (target.x != -999){
            return new SnowCommand(target.x, target.y);
        }

        //cek musuh
        Worm enemyWorm = getFirstWormInRange();
        //shoot
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }

        //cek musuh terdekat
        Worm closestWorm = getClosestWorm(); //range changeable
        //shortest movement (MoveDig)
        if (closestWorm != null){
            Cell targetBlock = getEnemyCell(closestWorm);
            if(targetBlock != null)
            {
                if (targetBlock.type == CellType.AIR) {
                    return new MoveCommand(targetBlock.x, targetBlock.y);
                } else if (targetBlock.type == CellType.DIRT) {
                    return new DigCommand(targetBlock.x, targetBlock.y);
                }
            }
        }



        //random movement
        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        int cellIdx = random.nextInt(surroundingBlocks.size());

        Cell block = surroundingBlocks.get(cellIdx);
        if (block.type == CellType.AIR) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }

        return new DoNothingCommand();
    }

    private Position canBananaBomb(){
        int min=99999999;
        int temp;
        Position target= new Position();
        target.x = -999;
        target.y = -999;

        if (currentWorm.id==2 ) {
            if (currentWorm.bananaBombs.count>0){
                for (Worm enemyWorm : opponent.worms) {
                    temp = euclideanDistance(currentWorm.position.x, currentWorm.position.y, enemyWorm.position.x, enemyWorm.position.y);

                    if (temp < min && temp <= currentWorm.bananaBombs.range){
                        min = temp;
                        target.x = enemyWorm.position.x;
                        target.y = enemyWorm.position.y;
                    }
                }
            }
        }
        return target;
    }

    private Position canSnowball(){
        int min=99999999;
        int temp;

        Position target= new Position();
        target.x = -999;
        target.y = -999;

        if (currentWorm.id==3 ) {
            if (currentWorm.snowballs.count>0){
                for (Worm enemyWorm : opponent.worms) {
                    temp = euclideanDistance(currentWorm.position.x, currentWorm.position.y, enemyWorm.position.x, enemyWorm.position.y);

                    if (temp < min && temp <= currentWorm.snowballs.range){
                        min = temp;
                        target.x = enemyWorm.position.x;
                        target.y = enemyWorm.position.y;
                    }
                }
            }
        }
        return target;
    }

    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health > 0) {
                return enemyWorm;
            }
        }

        return null;
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private Worm getClosestWorm() {

//        Set<String> cells = constructThrowingLines(range)
//                .stream()
//                .flatMap(Collection::stream)
//                .map(cell -> String.format("%d_%d", cell.x, cell.y))
//                .collect(Collectors.toSet());
        Worm tempWorm = null;
        float distance = 9999;
        for (Worm enemyWorm : opponent.worms) {
//            if (cells.contains(enemyPosition)) {
//                return enemyWorm;
//            }
            if (enemyWorm.health > 0) {
                float temp = euclideanDistance(currentWorm.position.x, currentWorm.position.x, enemyWorm.position.x, enemyWorm.position.y);
                if (temp < distance) {
                    tempWorm = enemyWorm;
                    distance = temp;
                }
            }
        }

        return tempWorm;
    }

    private Worm getClosestEnemies(int range) {
        boolean found;
        found = false;
        Worm tempWorm = new Worm();
        int minDistance=999;
        int tempDistance;
        Set<String> cells = constructThrowingLines(range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition)) {
                if (!found)
                {
                    found = true;
                    minDistance = euclideanDistance(currentWorm.position.x, currentWorm.position.y, enemyWorm.position.x, enemyWorm.position.y);
                    tempWorm = enemyWorm;
                }
                else
                {
                    tempDistance = euclideanDistance(currentWorm.position.x, currentWorm.position.y, enemyWorm.position.x, enemyWorm.position.y);
                    if (tempDistance < minDistance)
                    {
                        minDistance = tempDistance;
                        tempWorm = enemyWorm;
                    }
                }
            }
        }

        if (found)
        {
            return tempWorm;
        }
        return null;
    }

    private List<List<Cell>> constructThrowingLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    //fungsi buat throwline akan mengembalikan garis yang akan digunakan
    private List<Cell> createEnemyLine(int range, Worm enemy){
        List<Cell> directionLine = new ArrayList<>();
        Direction direction = resolveDirection(currentWorm.position, enemy.position);
        for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

            int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
            int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

            if (!isValidCoordinate(coordinateX, coordinateY)) {
                break;
            }

            if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                break;
            }

            Cell cell = gameState.map[coordinateY][coordinateX];

            directionLine.add(cell);
        }
        return directionLine;
    }

    //fungsi buat throwline akan mengembalikan garis yang akan digunakan
    private Cell getEnemyCell(Worm enemy){
        Cell targetCell = new Cell();
        targetCell = null;
        Direction direction = resolveDirection(currentWorm.position, enemy.position);
        int coordinateX = currentWorm.position.x + (direction.x);
        int coordinateY = currentWorm.position.y + (direction.y);
        Cell cell = gameState.map[coordinateY][coordinateX];
        if(isValidCoordinate(coordinateX,coordinateY))
        {
            targetCell = cell;
        }
        return targetCell;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    //asumsi lava invalid
    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }

    public Worm GetWorm(int id){
        for (Worm worm : gameState.myPlayer.worms){
            if (worm.id == id){
                return worm;
            }
        }
        return null;
    }

}
