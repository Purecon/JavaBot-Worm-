package za.co.entelect.challenge.command;
import za.co.entelect.challenge.entities.Worm;
import za.co.entelect.challenge.command.*;
import com.google.gson.annotations.SerializedName;
import za.co.entelect.challenge.enums.Direction;

public class SelectCommand implements Command{

    private ShootCommand SC;
    private Worm W;
    @SerializedName("remainingWormSelections")
    public static  int remainingWormSelections;

    public SelectCommand(Worm W, Direction direction){
        this.W = W;
        this.SC = new ShootCommand(direction);
    }


    @Override
    public String render() {
        return String.format("select %d ; shoot %s", W.id, SC.GetDirection());
    }
}
