package za.co.entelect.challenge.command;

public class SnowCommand implements Command {
    private final int x;
    private final int y;

    //ubah nilai atribut (setter)
    public  SnowCommand(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public String render() {
        return String.format("snowball %d %d", x, y);
    }
}
