package za.co.entelect.challenge.command;
import za.co.entelect.challenge.entities.MyWorm;
import za.co.entelect.challenge.entities.MyPlayer;
public class Select implements Command{

    private MyWorm W;
    private int id;

    public SelectCommand (MyWorm W, int id){
        this.W = MP.worms[id-1];
    }


    @Override
    public String render() {
        return String.format("select worm %d", W.id);
    }
}
