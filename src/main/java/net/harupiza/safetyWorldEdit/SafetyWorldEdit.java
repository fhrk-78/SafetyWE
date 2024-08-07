package net.harupiza.safetyWorldEdit;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public final class SafetyWorldEdit extends JavaPlugin implements Listener {
    private List<String> target;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        target = getConfig().getStringList("targetList");

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();

        if (target.contains(command.split(" ")[0])) {
            org.bukkit.entity.Player player = event.getPlayer();

            // スキップ権限があればチェックをスキップする
            if (player.hasPermission("safetyWorldEdit.skipSafetyCheck")) return;

            // WorldEdit選択範囲を取得
            Player actor = BukkitAdapter.adapt(player);
            SessionManager manager = WorldEdit.getInstance().getSessionManager();
            LocalSession localSession = manager.get(actor);

            Region region;
            World selectionWorld = localSession.getSelectionWorld();
            try {
                if (selectionWorld == null) throw new IncompleteRegionException();
                region = localSession.getSelection(selectionWorld);
            } catch (IncompleteRegionException ex) {
                return;
            }

            // WorldGuardを取得
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(selectionWorld);

            // 1ブロックずつ確認する
            if (regions != null) {
                for (BlockVector3 block : region) {
                    for (Map.Entry<String, ProtectedRegion> protectedRegion : regions.getRegions().entrySet()) {
                        if (protectedRegion.getValue().contains(block)) {
                            // regionに含まれていたとき
                            if (!(protectedRegion.getValue().isOwner(WorldGuardPlugin.inst().wrapPlayer(player))
                                    || protectedRegion.getValue().isMember(WorldGuardPlugin.inst().wrapPlayer(player)))) {
                                // オーナーでもメンバーでもなければキャンセル
                                player.sendMessage("[!] 他人のGuard内に侵入しています。操作を中断します。");
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
