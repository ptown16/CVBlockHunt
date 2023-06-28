package org.cubeville.cvblockhunt;

import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.cvgames.CVGames;

public final class CVBlockHunt extends JavaPlugin {

    static private CVBlockHunt instance;

    static CVBlockHunt getInstance() { return instance; }
    
    @Override
    public void onEnable() {
        instance = this;
        CVGames.gameManager().registerGame("blockhunt", BlockHunt::new);
    }

}
