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

        Position target;
        //BananaBomb lowhealth
        target = canBananaBomb();
        if (target.x != -999 && !friendlyFire(target,true) && currentWorm.health<=60){
            return new BananaCommand(target.x, target.y);
        }

        //snowball
        target = canSnowball();
        if (target.x != -999 && getClosestWorm(currentWorm).roundsUntilUnfrozen <=1 && !friendlyFire(target,false)){
            return new SnowCommand(target.x, target.y);
        }

        //cek musuh
        Worm enemyWorm = getFirstWormInRange(currentWorm);
        //shoot
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }

        //cek worm rusuh
        MyWorm busyWorm = getBusyWorm();
        //select
        if(gameState.myPlayer.remainingWormSelections>0 && busyWorm != null) {
            Worm enemyWorm1 = getFirstWormInRange(busyWorm);
            return new SelectCommand(busyWorm, resolveDirection(busyWorm.position, enemyWorm1.position));
        }

        //NonTechno to enemy_techno
        Worm techEnemy = GetEnemyWorm(3);
        if (currentWorm.id != 3 && techEnemy.health>0) {
            //cek jarak (kalo udah weapon range skip)
            if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, techEnemy.position.x, techEnemy.position.y) > currentWorm.weapon.range){
                //Shortest movement ke technologist
                Cell targetBlock = getEnemyCell(techEnemy);
                if(targetBlock != null &&  !wormInCell(targetBlock, true))
                {
                    if (targetBlock.type == CellType.AIR) {
                        return new MoveCommand(targetBlock.x, targetBlock.y);
                    } else if (targetBlock.type == CellType.DIRT) {
                        return new DigCommand(targetBlock.x, targetBlock.y);
                    }
                }
            }
        }

        //PROTECT TECHNOLOGIST
        Worm Tech = GetWorm(3);
        if (currentWorm.id != 3 && Tech.health>0) {
            //cek jarak (kalo udah weapon range skip)
            if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, Tech.position.x, Tech.position.y) > currentWorm.weapon.range){
                //Shortest movement ke technologist
                Cell targetBlock = getEnemyCell(Tech);
                if(targetBlock != null &&  !wormInCell(targetBlock, true))
                {
                    if (targetBlock.type == CellType.AIR) {
                        return new MoveCommand(targetBlock.x, targetBlock.y);
                    } else if (targetBlock.type == CellType.DIRT) {
                        return new DigCommand(targetBlock.x, targetBlock.y);
                    }
                }
            }
        }

        Worm agent = GetWorm(2);
        if (currentWorm.id == 3 && agent.health>0) {
            //cek jarak (kalo udah weapon range skip)
            if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, agent.position.x, agent.position.y) > currentWorm.weapon.range) {
                //Shortest movement ke technologist
                Cell targetBlock = getEnemyCell(agent);
                if (targetBlock != null && !wormInCell(targetBlock, true)) {
                    if (targetBlock.type == CellType.AIR) {
                        return new MoveCommand(targetBlock.x, targetBlock.y);
                    } else if (targetBlock.type == CellType.DIRT) {
                        return new DigCommand(targetBlock.x, targetBlock.y);
                    }
                }
            }
        }

        //BananaBomb
        target = canBananaBomb();
        if (target.x != -999 && !friendlyFire(target,true)){
            return new BananaCommand(target.x, target.y);
        }

        //cek musuh terdekat
        Worm closestWorm = getClosestWorm(currentWorm); //range changeable
        //shortest movement (MoveDig)
        if (closestWorm != null){
            Cell targetBlock = getEnemyCell(closestWorm);
            if(targetBlock != null &&  !wormInCell(targetBlock, true))
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
                    if (enemyWorm.health>0) {
                        temp = euclideanDistance(currentWorm.position.x, currentWorm.position.y, enemyWorm.position.x, enemyWorm.position.y);

                        if (temp < min && temp <= currentWorm.bananaBombs.range) {
                            min = temp;
                            target.x = enemyWorm.position.x;
                            target.y = enemyWorm.position.y;
                        }
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
                    if (enemyWorm.health>0) {
                        temp = euclideanDistance(currentWorm.position.x, currentWorm.position.y, enemyWorm.position.x, enemyWorm.position.y);

                        if (temp < min && temp <= currentWorm.snowballs.range) {
                            min = temp;
                            target.x = enemyWorm.position.x;
                            target.y = enemyWorm.position.y;
                        }
                    }
                }
            }
        }
        return target;
    }

    private Worm getFirstWormInRange(MyWorm W) {

        Set<String> cells = constructFireDirectionLines(W.weapon.range, W)
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

    private List<List<Cell>> constructFireDirectionLines(int range, MyWorm W) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = W.position.x + (directionMultiplier * direction.x);
                int coordinateY = W.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(W.position.x, W.position.y, coordinateX, coordinateY) > range) {
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

    private Worm getClosestWorm(Worm W) {
        Worm tempWorm = null;
        float distance = 9999;
        for (Worm enemyWorm : opponent.worms) {
            if (enemyWorm.health > 0) {
                float temp = euclideanDistance(W.position.x, W.position.y, enemyWorm.position.x, enemyWorm.position.y);
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

    public Worm GetEnemyWorm(int id){
        for (Worm worm : opponent.worms){
            if (worm.id == id){
                return worm;
            }
        }
        return null;
    }

    public MyWorm getBusyWorm(){
        for (MyWorm worm : gameState.myPlayer.worms){

            if (worm.roundsUntilUnfrozen <= 0 && getFirstWormInRange(worm) != null){
                return worm;
            }
        }
        return null;
    }

    public boolean wormInCell(Cell cell, boolean friend){
        if (friend){
            for (MyWorm worm : gameState.myPlayer.worms){
                if (worm.position.x == cell.x && worm.position.y == cell.y && worm.health>0){
                    return true;
                }
            }
        }
        else{
            for (Worm worm : opponent.worms){
                if (worm.position.x == cell.x && worm.position.y == cell.y && worm.health>0){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean friendlyFire(Position P, boolean bn){
        //if true don't attack
        //kalau musuh>temen attack
        int count_musuh = 0;
        int count_teman = 0;
        int temp_radius = 0;
        if (bn)
        {
            temp_radius = currentWorm.bananaBombs.damageRadius;
        }
        else
        {
            temp_radius = currentWorm.snowballs.freezeRadius;
        }
        for (Direction direction : Direction.values()) {
            for (int directionMultiplier = 1; directionMultiplier <= temp_radius; directionMultiplier++) {
                //kalau banana directionMultiplier == temp_radius dia hanya "N" "E" "W" "S"
                if (!bn || (bn && directionMultiplier<temp_radius) || (bn && Math.pow(direction.x,2)+Math.pow(direction.y,2)==1))
                {
                    int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                    int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                    for (MyWorm worm : gameState.myPlayer.worms){
                        if (worm.position.x == coordinateX && worm.position.y == coordinateY && worm.health>0){
                            //tambahin counter
                            count_teman++;
                        }
                    }
                    for (Worm worm : opponent.worms){
                        if (worm.position.x == coordinateX && worm.position.y == coordinateY && worm.health>0){
                            //tambahin counter
                            count_musuh++;
                        }
                    }
                }
            }
        }
        return count_musuh<count_teman;
    }
}
