package org.cubeville.cvblockhunt;

import org.cubeville.cvgames.models.PlayerState;

public class BlockHuntState extends PlayerState {
    boolean isSeeker = false;
    boolean isDisguised = false;
    double disguiseX, disguiseY, disguiseZ;
    int blockX, blockY, blockZ;
    double placeX, placeY, placeZ;
    
    // This the value we want to sort the state by if we ever use the default scoreboard
    // Since we are likely using a custom scoreboard (if any), this does not matter
    @Override
    public int getSortingValue() {
        return 0;
    }
}
