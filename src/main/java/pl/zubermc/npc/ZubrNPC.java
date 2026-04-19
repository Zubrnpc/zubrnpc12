package pl.zubermc.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_21_R4.CraftServer;
import org.bukkit.craftbukkit.v1_21_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ZubrNPC extends JavaPlugin implements Listener {

    private net.minecraft.world.entity.Entity npc;
    private int npcId;
    private FileConfiguration config;
    private File configFile;

    // Komendy z configu
    private String commandName = "npczuber";
    private String subCreate = "stworz";
    private String subKill = "usun";
    private String subCmd = "komenda";
    private String subMode = "tryb";
    private String subReload = "reload";

    // Ustawienia NPC - tylko w pamięci, nie w configu
    private String clickCommand = "say Witaj %player%!";
    private boolean executeAsConsole = true;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        loadConfig();
        registerCommand();
        getLogger().info("ZubrNPC odpalony! Komenda: /" + commandName);
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
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            if (commandMap.getCommand(commandName)!= null) {
                commandMap.getCommand(commandName).unregister(commandMap);
            }
            DynamicCommand cmd = new DynamicCommand(commandName);
            commandMap.register("zubrnpc", cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class DynamicCommand extends Command {
        protected DynamicCommand(String name) {
            super(name);
            this.description = "Komenda NPC Zubr";
            this.usageMessage = "/" + name;
            this.setPermission("zubr.admin");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (!(sender instanceof Player p)) return false;
            if (!p.isOp()) return false;

            if (args.length == 0) {
                p.sendMessage("§e/" + label + " " + subCreate + " <PLAYER|PIG|COW> §7- respi NPC na twojej pozycji");
                p.sendMessage("§e/" + label + " " + subCmd + " <komenda> §7- ustawia co robi po kliknięciu");
                p.sendMessage("§e/" + label + " " + subMode + " <konsola|gracz> §7- kto wykonuje komendę");
                p.sendMessage("§e/" + label + " " + subReload + " §7- przeładowuje config");
                p.sendMessage("§e/" + label + " " + subKill + " §7- usuwa NPC");
                return true;
            }

            if (args[0].equalsIgnoreCase(subCreate)) {
                String typ = args.length > 1? args[1].toUpperCase() : "PLAYER";
                spawnNPC(p.getLocation(), typ);
                p.sendMessage("§aZrespiono NPC: " + typ + " §7Nie zapisuje się po restarcie");
                return true;
            }

            if (args[0].equalsIgnoreCase(subKill)) {
                removeNPC();
                p.sendMessage("§cNPC usunięty");
                return true;
            }

            if (args[0].equalsIgnoreCase(subCmd)) {
                if (args.length < 2) {
                    p.sendMessage("§cUżycie: /" + label + " " + subCmd + " <komenda bez />");
                    return true;
                }
                clickCommand = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                p.sendMessage("§aUstawiono komendę: §f" + clickCommand + " §7Nie zapisuje się po restarcie");
                return true;
            }

            if (args[0].equalsIgnoreCase(subMode)) {
                if (args.length < 2) return false;
                executeAsConsole = args[1].equalsIgnoreCase("konsola");
                p.sendMessage("§aTryb wykonywania: §f" + (executeAsConsole? "KONSOLA" : "GRACZ"));
                return true;
            }

            if (args[0].equalsIgnoreCase(subReload)) {
                removeNPC();
                unregisterCommand();
                loadConfig();
                registerCommand();
                p.sendMessage("§aPrzeładowano config. Nowa komenda: /" + commandName);
                p.sendMessage("§7NPC trzeba zrespić od nowa");
                return true;
            }
            return false;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            if (args.length == 1) {
                return List.of(subCreate, subKill, subCmd, subMode, subReload);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase(subCreate)) {
                return List.of("PLAYER", "PIG", "COW", "VILLAGER", "ZOMBIE", "IRON_GOLEM");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase(subMode)) {
                return List.of("konsola", "gracz");
            }
            return List.of();
        }
    }

    private void unregisterCommand() {
        try {
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            if (commandMap.getCommand(commandName)!= null) {
                commandMap.getCommand(commandName).unregister(commandMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spawnNPC(Location loc, String typ) {
        removeNPC();
        var server = ((CraftServer) Bukkit.getServer()).getServer();
        var world = ((CraftWorld) loc.getWorld()).getHandle();

        if (typ.equals("PLAYER")) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "§6§lŻubr Przewodnik");
            profile.getProperties().put("textures", new Property("textures", "ewogICJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmVhZjY3NjI4YzE4ZjE4ZjE4ZjE4ZjE4ZjE4ZjE4ZjE4ZjE4ZjE4ZjE4ZiJ9fX0="));
            npc = new ServerPlayer(server, world, profile);
        } else {
            try {
                EntityType<?> entityType = EntityType.byString(typ.toLowerCase()).orElse(EntityType.PIG);
                npc = entityType.create(world);
            } catch (Exception e) {
                npc = EntityType.PIG.create(world);
            }
            npc.setCustomName(net.minecraft.network.chat.Component.literal("§6§lŻubr Przewodnik"));
            npc.setCustomNameVisible(true);
        }

        npc.setPos(loc.getX(), loc.getY(), loc.getZ());
        npc.setYRot(loc.getYaw());
        npc.setXRot(loc.getPitch());
        if (npc instanceof LivingEntity le) {
            le.setInvulnerable(true);
            le.setNoAi(true);
            le.setSilent(true);
        }
        npcId = npc.getId();

        for (Player online : Bukkit.getOnlinePlayers()) {
            ServerGamePacketListenerImpl connection = ((CraftPlayer) online).getHandle().connection;
            if (npc instanceof ServerPlayer sp) {
                connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, sp));
                Bukkit.getScheduler().runTaskLater(this, () ->
                        connection.send(new ClientboundPlayerInfoRemovePacket(List.of(sp.getUUID()))), 20L);
            }
            connection.send(new ClientboundAddEntityPacket(npc));
            connection.send(new ClientboundRotateHeadPacket(npc, (byte) ((int) (loc.getYaw() * 256.0F / 360.0F))));
        }
    }

    private void removeNPC() {
        if (npc == null) return;
        for (Player online : Bukkit.getOnlinePlayers()) {
            ServerGamePacketListenerImpl connection = ((CraftPlayer) online).getHandle().connection;
            connection.send(new ClientboundRemoveEntitiesPacket(npcId));
        }
        npc = null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (npc == null) return;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            ServerGamePacketListenerImpl connection = ((CraftPlayer) e.getPlayer()).getHandle().connection;
            if (npc instanceof ServerPlayer sp) {
                connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, sp));
                Bukkit.getScheduler().runTaskLater(this, () ->
                        connection.send(new ClientboundPlayerInfoRemovePacket(List.of(sp.getUUID()))), 20L);
            }
            connection.send(new ClientboundAddEntityPacket(npc));
            connection.send(new ClientboundRotateHeadPacket(npc, (byte) ((int) (npc.getYRot() * 256.0F / 360.0F))));
        }, 10L);
    }

    @EventHandler
    public void onClick(PlayerInteractEntityEvent e) {
        if (npc == null) return;
        if (e.getRightClicked().getEntityId()!= npcId) return;
        e.setCancelled(true);
        String finalCmd = clickCommand.replace("%player%", e.getPlayer().getName());
        if (executeAsConsole) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        } else {
            e.getPlayer().performCommand(finalCmd);
        }
    }
}
