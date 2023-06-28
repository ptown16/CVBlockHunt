package org.cubeville.cvblockhunt;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import org.cubeville.cvgames.models.Game;
import org.cubeville.cvgames.vartypes.GameVariableLocation;
import org.cubeville.cvgames.vartypes.GameVariableInt;
import org.cubeville.cvgames.vartypes.GameVariableString;

import java.util.*;

public class BlockHunt extends Game implements Listener {

    long lastSearchItemUsage = 0;
    int seekerItemCooldown = 0;

    long matchStartTime = 0;
    int matchTime = 0;

    int task = 0;
    
    public BlockHunt(String id, String arenaName) {
        super(id, arenaName);
        // Define any variables here
        addGameVariable("spawn", new GameVariableLocation());
        addGameVariable("seeker-lobby", new GameVariableLocation());
        addGameVariable("match-time", new GameVariableInt());
        addGameVariable("seeker-item-cooldown", new GameVariableInt());
        addGameVariable("hider-loadout", new GameVariableString());
        addGameVariable("seeker-loadout", new GameVariableString());
        addGameVariable("message-portal", new GameVariableString());
    }

    // "state" is a variable that exists in every game that allows the games plugin to track which players are playing the game
    // All this method does is map the state you have to your custom defined state.
    protected BlockHuntState getState(Player p) {
        if (state.get(p) == null || !(state.get(p) instanceof BlockHuntState)) return null;
        return (BlockHuntState) state.get(p);
    }

    Set<Player> getPlayers() {
        return state.keySet();
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        BlockHuntState state = getState(player);

        if(state == null) return;
        if(state.isSeeker) return;
        
        if(state.isDisguised == false) {
            player.sendMessage("§aYou are disguised now, your disguise will end if you move.");

            Block block = event.getBlockPlaced();

            state.isDisguised = true;

            state.placeX = player.getLocation().getX();
            state.placeY = player.getLocation().getY();
            state.placeZ = player.getLocation().getZ();
            
            state.disguiseX = block.getX() + 0.5;
            state.disguiseY = block.getY() - 0.6;
            state.disguiseZ = block.getZ() + 0.5;

            state.blockX = block.getX();
            state.blockY = block.getY();
            state.blockZ = block.getZ();

            for(Player p: getPlayers()) {
                if(p.equals(player)) continue;
                p.hidePlayer(CVBlockHunt.getInstance(), player);
            }

            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(new Location(block.getWorld(), state.disguiseX, state.disguiseY + 1, state.disguiseZ));
            player.setFlySpeed(0.1f);
            return;
        }

        event.setCancelled(true);
    }

    private void globalMessage(String Title, String Subtitle) {
        String cmd = "cvportal sendtitle " + ((String) getVariable("message-portal")) + " \"" + Title + "\" \"" + Subtitle + "\" 20 40 20";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);        
    }
    
    public void onCountdown(int counter) {
        globalMessage("", "§e" + counter + " seconds until Block Hunt match starts");
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BlockHuntState state = getState(player);

        if(state == null) return;
        if(state.isDisguised == false) return;

        stopDisguise(player, false);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if(block == null) return;
        if(event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if(getState(player) == null || getState(player).isSeeker == false) return;

        long now = System.currentTimeMillis();
        if(now - lastSearchItemUsage < seekerItemCooldown) {
            player.playSound(player, Sound.BLOCK_DISPENSER_FAIL, 15.0f, 1.0f);
            return;
        }

        lastSearchItemUsage = now;
        
        // TODO: Check / set cooldown
        for(Player p: state.keySet()) {
            BlockHuntState s = getState(p);
            if(s.isDisguised == true && s.blockX == block.getX() && s.blockY == block.getY() && s.blockZ == block.getZ()) {
                removeHider(p);
                player.sendMessage("§aYou have found " + p.getDisplayName() + ".");
                return;
            }
        }

        player.playSound(player, Sound.ENTITY_VILLAGER_NO, 15.0f, 1.0f);
    }

    private void removeHider(Player player) {
        if(getState(player).isDisguised)
            stopDisguise(player, true);
        player.sendMessage("§cYou've been discovered, better luck next time!");
        state.remove(player);
        player.teleport((Location) getVariable("exit"));
        player.getInventory().clear();
        
        checkGameEnds();
    }

    private void checkGameEnds() {
        int hiderCount = 0;
        for(Player p: getPlayers()) {
            if(getState(p).isSeeker == false)
                hiderCount++;
        }
        if(hiderCount == 0) {
            globalMessage("§aSeekers win!", "All hiders discovered.");
            finishGame();
        }

    }
    
    private void stopDisguise(Player player, boolean force) {
        BlockHuntState s = getState(player);
        Location loc = player.getLocation();
        if(force == true || loc.distance(new Location(loc.getWorld(), s.disguiseX, s.disguiseY, s.disguiseZ)) > 1.0) {
            if(!force) {
                player.sendMessage("§cYour disguise ended.");
                player.teleport(new Location(player.getLocation().getWorld(), s.placeX, s.placeY, s.placeZ));
            }
            player.setGameMode(GameMode.SURVIVAL);

            s.isDisguised = false;

            for(Player p: Bukkit.getOnlinePlayers()) { // brute forcing here, to avoid leaking hidden players
                if(p.equals(player)) continue;
                p.showPlayer(CVBlockHunt.getInstance(), player);
            }

            Material blocktype = player.getWorld().getBlockAt(s.blockX, s.blockY, s.blockZ).getType();
            player.getWorld().getBlockAt(s.blockX, s.blockY, s.blockZ).setType(Material.AIR);
            player.getWorld().createExplosion(new Location(player.getWorld(), s.disguiseX, s.blockY + 0.5, s.disguiseZ), 0.5f, false, false);
            PlayerInventory playerInventory = player.getInventory();
            int emptySlot = playerInventory.firstEmpty();
            playerInventory.setItem(emptySlot, new ItemStack(blocktype));
        }
    }
    
    @Override
    public void onGameStart(Set<Player> players) {

        seekerItemCooldown = (int) getVariable("seeker-item-cooldown") * 1000;
        matchTime = (int) getVariable("match-time");
        lastSearchItemUsage = 0;
        matchStartTime = System.currentTimeMillis();
        
        Random rand = new Random();

        int seekerIndex = rand.nextInt(players.size());
        int i = 0;
        for (Player player : players) {
            state.put(player, new BlockHuntState());
            if (i == seekerIndex) {
                // set the player as a seeker
                getState(player).isSeeker = true;
                player.teleport((Location) getVariable("seeker-lobby"));
                player.sendMessage("§eYou are a seeker!");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "loadout apply " + ((String) getVariable("seeker-loadout")) + " player:" + player.getName());
            } else {
                // set the player as a hider
                player.teleport((Location) getVariable("spawn"));
                player.sendMessage("§aYou are a hider!");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "loadout apply " + ((String) getVariable("hider-loadout")) + " player:" + player.getName());
            }
            i++;
        }

        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CVBlockHunt.getInstance(), new Runnable() {
                public void run() {
                    long now = System.currentTimeMillis();

                    if(lastSearchItemUsage != 0) {
                        if(now - lastSearchItemUsage > seekerItemCooldown) {
                            for(Player player: getPlayers()) {
                                if(getState(player).isSeeker) {
                                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 15.0f, 1.0f);
                                    lastSearchItemUsage = 0;
                                    // TODO: Later per seeker in state
                                }
                            }
                        }
                    }

                    int remainingTime = matchTime - (int)(now - matchStartTime) / 1000;
                    if(remainingTime < 2) {
                        globalMessage("§aHiders win!", "§eTime's up.");
                        finishGame();
                        return;
                    }
                    else if(remainingTime % 30 == 0) {
                        int min = remainingTime / 60;
                        int seconds = remainingTime % 60;
                        String t = Integer.toString(seconds);
                        if(t.length() < 2) t = "0" + t;
                        t = min + ":" + t;
                        globalMessage("", "§eMatch ends in " + t);
                    }

                    // TODO: At 0, end match, seeker wins, undisguise hiders
                }
            }, 20, 20);
    }

    @Override
    public void onGameFinish() {
        // NOTE: Do not call "onGameFinish" directly because it will not finish the game properly
        // Instead, call finishGame() when you want to finish the game

        Bukkit.getScheduler().cancelTasks(CVBlockHunt.getInstance());
        
        for(Player player: getPlayers()) {
            playerCleanup(player);
        }
    }

    @Override
    public void onPlayerLeave(Player player) {
        playerCleanup(player);
        state.remove(player);

        checkGameEnds();
    }

    private void playerCleanup(Player player) {
        BlockHuntState s = getState(player);
        if(s.isDisguised) {
            for(Player p: getPlayers()) {
                if(!p.equals(player)) {
                    p.showPlayer(CVBlockHunt.getInstance(), player);
                }
            }
            player.getWorld().getBlockAt(s.blockX, s.blockY, s.blockZ).setType(Material.AIR);
            player.setGameMode(GameMode.SURVIVAL);
        }
        player.teleport((Location) getVariable("exit"));
        player.getInventory().clear();
    }

    private void displayScoreboard() {
    }
}
