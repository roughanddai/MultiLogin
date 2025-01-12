package fun.ksnb.multilogin.velocity.impl;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import moe.caa.multilogin.core.impl.IPlayer;
import moe.caa.multilogin.core.impl.IPlayerManager;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class VelocityPlayerManager implements IPlayerManager {
    private final ProxyServer server;

    public VelocityPlayerManager(ProxyServer server) {
        this.server = server;
    }

    @Override
    public Set<IPlayer> getPlayer(String name) {
        Set<IPlayer> ret = new HashSet<>();
        for (Player player : server.getAllPlayers()) {
            if (player.getUsername().equalsIgnoreCase(name)) {
                ret.add(new VelocityPlayer(player));
            }
        }
        return ret;
    }

    @Override
    public IPlayer getPlayer(UUID uuid) {
        Optional<Player> player = server.getPlayer(uuid);
        return player.map(VelocityPlayer::new).orElse(null);
    }

    @Override
    public Set<IPlayer> getOnlinePlayers() {
        return server.getAllPlayers().stream().map(VelocityPlayer::new).collect(Collectors.toSet());
    }

    @Override
    public boolean isOnlineMode() {
        return server.getConfiguration().isOnlineMode();
    }

    @Override
    public boolean isWhitelist() {
        return false;
    }

    @Override
    public boolean isForward() {
        return ((VelocityConfiguration) server.getConfiguration()).getPlayerInfoForwardingMode() != PlayerInfoForwarding.NONE;
    }
}
