package za.co.entelect.challenge.entities;

import com.google.gson.annotations.SerializedName;

public class MyWorm extends Worm {
    @SerializedName("weapon")
    public Weapon weapon;

    //banana
    @SerializedName("bananaBombs")
    public bananaBombs bananaBombs;

    //snowball
    @SerializedName("snowballs")
    public Snowballs snowballs;


}
