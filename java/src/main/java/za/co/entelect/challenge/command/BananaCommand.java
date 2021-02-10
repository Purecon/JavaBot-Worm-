package za.co.entelect.challenge.command;

public class BananaCommand implements  Command{
    private final int x;
    private final int y;

    //ubah nilai atribut (setter)
    public  BananaCommand(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public String render() {
        return String.format("banana %d %d", x, y);
    }
}
