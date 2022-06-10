package org.cubeville.cvblockhunt;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.cubeville.cvgames.models.Game;
import org.cubeville.cvgames.utils.GameUtils;
import org.cubeville.cvgames.vartypes.GameVariableLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class BlockHunt extends Game {

    public BlockHunt(String id) {
        super(id);
        addGameVariable("spawn", new GameVariableLocation());
        addGameVariable("seeker-lobby", new GameVariableLocation());
    }

    public HashMap<Player, BlockHuntState> state = new HashMap<>();

    @Override
    public void onGameStart(List<Player> players) {
        Random rand = new Random();
        int seekerIndex = rand.nextInt(players.size());
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            state.put(player, new BlockHuntState());
            if (i == seekerIndex) {
                // set the player as a seeker
                state.get(player).isSeeker = true;
                player.teleport((Location) getVariable("seeker-lobby"));
                player.sendMessage("§cYou are the seeker!");
            } else {
                // set the player as a hider
                player.teleport((Location) getVariable("spawn"));
                player.sendMessage("§aYou are a hider!");
            }
        }
    }

    @Override
    public void onGameFinish(List<Player> players) {
        // NOTE: Do not call "onGameFinish" directly because it will not finish the game properly
        // Instead, call finishGame(List<Player> players) when you want to finish the game
        // If you have a better way of making sure this happens please let me know ;-;
        if (state.values().stream().allMatch(blockHuntState -> blockHuntState.isSeeker)) {
            GameUtils.messagePlayerList(players, "§cAll players were found!");
        } else {
            GameUtils.messagePlayerList(players, "§aHiders win!");
        }
    }

    @Override
    public void onPlayerLogout(Player p) {
        state.remove(p);
        // if there's only 1 player remaining or there are no seekers left, end the game
        if (state.size() <= 1 || state.values().stream().noneMatch(blockHuntState -> blockHuntState.isSeeker)) {
            finishGame(new ArrayList<>(state.keySet()));
        }
    }
}
