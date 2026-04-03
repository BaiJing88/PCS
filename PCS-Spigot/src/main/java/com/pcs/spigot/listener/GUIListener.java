/*
 * Copyright (c) 2026 Bai_Jing88 (QQ: 1782307393)
 * PCS (Player Credit System) - Minecraft Cross-Server Player Management
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * Any derivative work must also be open source and licensed under
 * the same AGPL v3 license. Commercial use is prohibited without
 * explicit permission from the author.
 */

package com.pcs.spigot.listener;

import com.pcs.spigot.PCSSpigotPlugin;
import com.pcs.spigot.gui.*;
import com.pcs.spigot.manager.GUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * GUI点击事件监听器
 */
public class GUIListener implements Listener {
    
    private final PCSSpigotPlugin plugin;
    
    public GUIListener(PCSSpigotPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // 检查是否是PCS GUI
        if (!title.startsWith(plugin.getGuiManager().getTitlePrefix())) {
            return;
        }
        
        // 取消所有点击事件
        event.setCancelled(true);
        
        // 获取点击的槽位
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        
        // 获取GUI类型
        GUIManager.GUIType guiType = plugin.getGuiManager().getOpenGUIType(player);
        if (guiType == null) {
            return;
        }
        
        // 根据GUI类型处理点击
        boolean handled = false;
        
        switch (guiType) {
            case VOTE_ACTION:
                handled = VoteActionGUI.handleClick(plugin, player, slot);
                break;
                
            case PLAYER_SELECT:
                int currentPage = plugin.getGuiManager().getCurrentPage(player);
                handled = PlayerSelectGUI.handleClick(plugin, player, slot, currentPage);
                break;
                
            case REASON_SELECT:
                handled = ReasonSelectGUI.handleClick(plugin, player, slot);
                break;
                
            case CREDIT_QUERY:
                handled = CreditQueryGUI.handleClick(plugin, player, slot);
                break;
                
            case RATING:
                handled = RatingGUI.handleClick(plugin, player, slot);
                break;
                
            default:
                break;
        }
        
        // 播放点击音效
        if (handled) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        // 检查是否是PCS GUI
        if (!title.startsWith(plugin.getGuiManager().getTitlePrefix())) {
            return;
        }
        
        // 延迟清理，以防是翻页操作
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 如果玩家没有打开新的PCS GUI，则清除数据
            if (!plugin.getGuiManager().isInAnyGUI(player)) {
                plugin.getVoteManager().clearVoteBuilder(player.getUniqueId());
                plugin.getGuiManager().clearPlayerData(player.getUniqueId());
            }
        }, 5L);
    }
}
