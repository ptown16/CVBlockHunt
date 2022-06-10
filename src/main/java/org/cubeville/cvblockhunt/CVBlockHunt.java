package org.cubeville.cvblockhunt;

import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.cvgames.CVGames;

public final class CVBlockHunt extends JavaPlugin {

    @Override
    public void onEnable() {
        CVGames.gameManager().registerGame("blockhunt", BlockHunt.class);
    }

}
