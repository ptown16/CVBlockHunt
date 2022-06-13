package org.cubeville.cvblockhunt;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.cubeville.cvgames.models.Game;
import org.cubeville.cvgames.utils.GameUtils;
import org.cubeville.cvgames.vartypes.GameVariableLocation;

import java.util.*;

public class BlockHunt extends Game {

    public BlockHunt(String id) {
        super(id);
        addGameVariable("spawn", new GameVariableLocation());
        addGameVariable("seeker-lobby", new GameVariableLocation());
    }

    private BlockHuntState getState(Player p) {
        if (state.get(p) == null || !(state.get(p) instanceof BlockHuntState)) return null;
        return (BlockHuntState) state.get(p);
    }

    @Override
    public void onGameStart(Set<Player> players) {
        Random rand = new Random();
        int seekerIndex = rand.nextInt(players.size());
        int i = 0;
        for (Player player : players) {
            state.put(player, new BlockHuntState());
            if (i == seekerIndex) {
                // set the player as a seeker
                getState(player).isSeeker = true;
                player.teleport((Location) getVariable("seeker-lobby"));
                player.sendMessage("§cYou are the seeker!");
            } else {
                // set the player as a hider
                player.teleport((Location) getVariable("spawn"));
                player.sendMessage("§aYou are a hider!");
            }
            i++;
        }
    }

    @Override
    public void onGameFinish() {
        // NOTE: Do not call "onGameFinish" directly because it will not finish the game properly
        // Instead, call finishGame() when you want to finish the game
        // If you have a better way of making sure this happens please let me know ;-;
        if (state.values().stream().allMatch(blockHuntState -> ((BlockHuntState) blockHuntState).isSeeker)) {
            GameUtils.messagePlayerList(state.keySet(), "§cAll players were found!");
        } else {
            GameUtils.messagePlayerList(state.keySet(), "§aHiders win!");
        }
    }

    @Override
    public void onPlayerLeave(Player p) {
        state.remove(p);
        // if there's only 1 player remaining or there are no seekers left, end the game
        if (state.size() <= 1 || state.values().stream().noneMatch(blockHuntState -> ((BlockHuntState) blockHuntState).isSeeker)) {
            finishGame();
        }
    }
}
