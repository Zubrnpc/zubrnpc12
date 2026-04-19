package pl.zubermc.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class ZubrNPC extends JavaPlugin implements Listener {

    private Entity npc;
    private UUID npcUUID;

    private FileConfiguration config;
    private File configFile;

    private String commandName = "npczuber";
    private String subCreate = "stworz";
    private String subKill = "usun";
    private String subCmd = "komenda";
    private String subMode = "tryb";
    private String subReload = "reload";

    private String clickCommand = "say Witaj %player%!";
    private boolean executeAsConsole = true;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        loadConfig();
        registerCommand();
        getLogger().info("ZubrNPC działa!");
    }

    @Override
    public void onDisable() {
        removeNPC();
    }

    private void loadConfig() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) saveResource("config.yml", false);
        config = YamlConfiguration.loadConfiguration(configFile);

        commandName = config.getString("command.name", "npczuber");
        subCreate = config.getString("command.create", "stworz");
        subKill = config.getString("command.kill", "usun");
        subCmd = config.getString("command.setcmd", "komenda");
        subMode = config.getString("command.mode", "tryb");
        subReload = config.getString("command.reload", "reload");
    }

    private void registerCommand() {
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap map = (CommandMap) f.get(Bukkit.getServer());

            if (map.getCommand(commandName) != null) {
                map.getCommand(commandName).unregister(map);
            }

            map.register("zubrnpc", new DynamicCommand(commandName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class DynamicCommand extends Command {

        protected DynamicCommand(String name) {
            super(name);
            this.setPermission("zubr.admin");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (!(sender instanceof Player p)) return false;
            if (!p.isOp()) return false;

            if (args.length == 0) {
                p.sendMessage("§e/" + label + " " + subCreate + " §7- tworzy NPC");
                p.sendMessage("§e/" + label + " " + subKill + " §7- usuwa NPC");
                p.sendMessage("§e/" + label + " " + subCmd + " <komenda>");
                return true;
            }

            if (args[0].equalsIgnoreCase(subCreate)) {
                spawnNPC(p.getLocation());
                p.sendMessage("§aNPC stworzony!");
                return true;
            }

            if (args[0].equalsIgnoreCase(subKill)) {
                removeNPC();
                p.sendMessage("§cNPC usunięty");
                return true;
            }

            if (args[0].equalsIgnoreCase(subCmd)) {
                if (args.length < 2) return false;
                clickCommand = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                p.sendMessage("§aUstawiono komendę!");
                return true;
            }

            return false;
        }
    }

    private void spawnNPC(Location loc) {
        removeNPC();

        npc = loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        npc.setCustomName("§6§lŻubr Przewodnik");
        npc.setCustomNameVisible(true);
        npc.setInvulnerable(true);
        npc.setSilent(true);

        npcUUID = npc.getUniqueId();
    }

    private void removeNPC() {
        if (npc != null) {
            npc.remove();
            npc = null;
            npcUUID = null;
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEntityEvent e) {
        if (npcUUID == null) return;
        if (!e.getRightClicked().getUniqueId().equals(npcUUID)) return;

        e.setCancelled(true);

        String cmd = clickCommand.replace("%player%", e.getPlayer().getName());

        if (executeAsConsole) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else {
            e.getPlayer().performCommand(cmd);
        }
    }
}
