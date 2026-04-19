package pl.zubermc.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.craftbukkit.v1_21_R4.CraftServer;
import org.bukkit.craftbukkit.v1_21_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

public class ZubrNPC extends JavaPlugin implements Listener {

    private ServerPlayer npc;
    private int npcId;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerCommand();
    }

    @Override
    public void onDisable() {
        removeNPC();
    }

    // ================= KOMENDA =================

    private void registerCommand() {
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap map = (CommandMap) f.get(Bukkit.getServer());

            map.register("zubrnpc", new Command("npczuber") {
                @Override
                public boolean execute(CommandSender sender, String label, String[] args) {
                    if (!(sender instanceof Player p)) return false;

                    if (args.length == 0) {
                        p.sendMessage("/npczuber stworz <nick>");
                        p.sendMessage("/npczuber usun");
                        return true;
                    }

                    if (args[0].equalsIgnoreCase("stworz")) {
                        if (args.length < 2) {
                            p.sendMessage("Podaj nick!");
                            return true;
                        }

                        String[] skin = getSkin(args[1]);
                        if (skin == null) {
                            p.sendMessage("Błąd pobierania skina!");
                            return true;
                        }

                        spawnNPC(p.getLocation(), skin[0], skin[1]);
                        p.sendMessage("NPC stworzony!");
                        return true;
                    }

                    if (args[0].equalsIgnoreCase("usun")) {
                        removeNPC();
                        p.sendMessage("NPC usunięty!");
                        return true;
                    }

                    return false;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= NPC =================

    private void spawnNPC(Location loc, String texture, String signature) {
        removeNPC();

        var server = ((CraftServer) Bukkit.getServer()).getServer();
        Level world = ((CraftWorld) loc.getWorld()).getHandle();

        GameProfile profile = new GameProfile(UUID.randomUUID(), "§6Żubr");

        profile.getProperties().put("textures", new Property("textures", texture, signature));

        npc = new ServerPlayer(server, world, profile);

        npc.setPos(loc.getX(), loc.getY(), loc.getZ());
        npc.setYRot(loc.getYaw());
        npc.setXRot(loc.getPitch());

        // 🔥 KLUCZOWE — dodanie do świata
        world.addFreshEntity(npc);

        npcId = npc.getId();

        for (Player p : Bukkit.getOnlinePlayers()) {
            ServerGamePacketListenerImpl conn = ((CraftPlayer) p).getHandle().connection;

            conn.send(new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, npc));

            conn.send(new ClientboundAddEntityPacket(npc));

            conn.send(new ClientboundRotateHeadPacket(npc,
                    (byte) ((int) (loc.getYaw() * 256 / 360))));

            Bukkit.getScheduler().runTaskLater(this, () ->
                    conn.send(new ClientboundPlayerInfoRemovePacket(List.of(npc.getUUID()))), 40L);
        }
    }

    private void removeNPC() {
        if (npc == null) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            ServerGamePacketListenerImpl conn = ((CraftPlayer) p).getHandle().connection;
            conn.send(new ClientboundRemoveEntitiesPacket(npcId));
        }

        npc.kill();
        npc = null;
    }

    // ================= SKIN =================

    private String[] getSkin(String nick) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + nick);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String response = reader.readLine();

            if (response == null) return null;

            String uuid = response.split("\"id\":\"")[1].split("\"")[0];

            URL url2 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(url2.openStream()));
            String json = reader2.lines().reduce("", String::concat);

            String value = json.split("\"value\":\"")[1].split("\"")[0];
            String sig = json.split("\"signature\":\"")[1].split("\"")[0];

            return new String[]{value, sig};

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ================= KLIK =================

    @EventHandler
    public void onClick(PlayerInteractEntityEvent e) {
        if (npc == null) return;
        if (e.getRightClicked().getEntityId() != npcId) return;

        e.setCancelled(true);
        e.getPlayer().sendMessage("Kliknąłeś NPC!");
    }
}
