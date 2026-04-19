package pl.zubermc.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ZubrNPC extends JavaPlugin implements Listener {

    private ArmorStand npc;
    private ArmorStand holo;

    private final Map<Integer, String> npcNames = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerCommand();
    }

    // ================= COMMAND =================

    private void registerCommand() {

        PluginCommand cmd = getCommand("npczuber");
        if (cmd == null) return;

        cmd.setExecutor((sender, command, label, args) -> {

            if (!(sender instanceof Player p)) return false;

            if (args.length < 2) {
                p.sendMessage("§6/npczuber stworz <nazwa>");
                p.sendMessage("§6/npczuber usun");
                return true;
            }

            if (args[0].equalsIgnoreCase("stworz")) {
                spawnNPC(p.getLocation(), args[1]);
                p.sendMessage("§aNPC stworzony!");
                return true;
            }

            if (args[0].equalsIgnoreCase("usun")) {
                removeNPC();
                p.sendMessage("§cNPC usunięty!");
                return true;
            }

            return false;
        });
    }

    // ================= SPAWN NPC =================

    private void spawnNPC(Location loc, String name) {

        removeNPC();

        // BODY NPC
        npc = loc.getWorld().spawn(loc, ArmorStand.class);
        npc.setInvisible(true);
        npc.setGravity(false);
        npc.setMarker(true);
        npc.setCustomNameVisible(false);

        // HOLOGRAM (TYLKO NAZWA Z KOMENDY)
        holo = loc.getWorld().spawn(loc.clone().add(0, 2.0, 0), ArmorStand.class);
        holo.setInvisible(true);
        holo.setGravity(false);
        holo.setMarker(true);
        holo.setCustomNameVisible(true);

        holo.setCustomName("§e" + name);

        npcNames.put(npc.getEntityId(), name);
    }

    // ================= REMOVE =================

    private void removeNPC() {
        if (npc != null) npc.remove();
        if (holo != null) holo.remove();

        npc = null;
        holo = null;
        npcNames.clear();
    }

    // ================= CLICK =================

    @EventHandler
    public void onClick(PlayerInteractAtEntityEvent e) {

        Entity clicked = e.getRightClicked();

        if (npc == null) return;
        if (clicked.getEntityId() != npc.getEntityId()) return;

        e.setCancelled(true);

        String name = npcNames.get(npc.getEntityId());

        e.getPlayer().sendMessage("§eKliknąłeś NPC: §6" + name);
    }
}
