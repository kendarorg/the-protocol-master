package org.kendar.amqp.v09;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.kendar.protocol.Sleeper;
import org.kendar.server.TcpServer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public class ReplayerTest  {

    protected static final int FAKE_PORT = 5682;

    @Test
    void openConnectionTest() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {

        var baseProtocol = new AmqpProtocol(FAKE_PORT);
        var proxy = new AmqpProxy();
        proxy.setStorage(new AmqpFileStorage(Path.of("src",
                "test", "resources", "openConnectionTest")));

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(1000);


        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.enableHostnameVerification();
        var cs = "amqp://localhost:"+FAKE_PORT;//rabbitContainer.getConnectionString();
        connectionFactory.setUri(cs);
        connectionFactory.setPassword("test");
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection
                .openChannel()
                .orElseThrow(() -> new RuntimeException("Failed to Open channel"));
        channel.close();
        connection.close();
        protocolServer.stop();
    }
}
