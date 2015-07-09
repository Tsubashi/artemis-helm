package artemis_helm;

import java.io.IOException;

import org.apache.log4j.Logger;
import net.dhleong.acl.enums.Console;
import net.dhleong.acl.iface.ArtemisNetworkInterface;
import net.dhleong.acl.iface.ConnectionSuccessEvent;
import net.dhleong.acl.iface.DisconnectEvent;
import net.dhleong.acl.iface.Listener;
import net.dhleong.acl.iface.ThreadedArtemisNetworkInterface;
import net.dhleong.acl.protocol.ArtemisPacket;
import net.dhleong.acl.protocol.core.setup.ReadyPacket;
import net.dhleong.acl.protocol.core.setup.SetConsolePacket;
import net.dhleong.acl.protocol.core.helm.HelmSetImpulsePacket;
import net.dhleong.acl.protocol.core.helm.HelmSetWarpPacket;
import net.dhleong.acl.protocol.core.helm.HelmSetSteeringPacket;

public class HelmConsole {
    private ArtemisNetworkInterface server;
    private final Logger logger = Logger.getLogger(HelmConsole.class.getName());

    public HelmConsole(String host, int port) throws IOException {
        logger.debug("Starting Helm Console");
        server = new ThreadedArtemisNetworkInterface(host, port);
        server.addListener(this);
        server.start();
    }

    public void setImpulse(float power) {
        if (power > 1.0) { 
            power = 1;
            logger.warn("Received Impulse Power larger than 4!");
        }
        if (power < 0) {
            power = 0;
            logger.warn("Received Impulse Power smaller than 0!");
        }
        server.send(new HelmSetImpulsePacket(power));
    }

    public void setWarp(int factor) {
        if (factor > 4) { 
            factor = 4;
            logger.warn("Received Warp Factor larger than 4!");
        }
        if (factor < 0) {
            factor = 0;
            logger.warn("Received Warp Factor smaller than 0!");
        }
        server.send(new HelmSetWarpPacket(factor));
    }

    public void setRudder(float value) {
        if (value > 1.0) { 
            value = 1;
            logger.warn("Received Rudder Value larger than 4!");
        }
        if (value < 0) {
            value = 0;
            logger.warn("Received Rudder Value smaller than 0!");
        }
        server.send(new HelmSetSteeringPacket(value));
    }

    @Listener
    public void onConnectSuccess(ConnectionSuccessEvent event) {
        server.send(new SetConsolePacket(Console.HELM, true));
        server.send(new ReadyPacket());
        logger.debug("Connected to Server.");
    }

    @Listener
    public void onPacket(ArtemisPacket pkt) {
        logger.trace(pkt);
    }

    @Listener
    public void onDisconnect(DisconnectEvent event) {
        logger.debug("Disconnected: " + event.getCause());
    }
}
