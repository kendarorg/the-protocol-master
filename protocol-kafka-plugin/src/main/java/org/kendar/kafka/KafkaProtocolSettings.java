package org.kendar.kafka;

import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

/**
 * Kafka protocol settings. Adds {@code advertisedHost}: the host the proxy
 * rewrites into every broker/coordinator address returned by Metadata (3),
 * FindCoordinator (10) and DescribeCluster (60) responses, so clients keep
 * talking to the proxy instead of connecting to the real brokers directly
 * (see protocol-kafka.md §3.1). The rewritten port is always this instance's
 * own bind {@code port}.
 */
@Extension
@TpmService(tags = "kafka")
public class KafkaProtocolSettings extends ByteProtocolSettingsWithLogin implements ExtensionPoint {
    private String advertisedHost = "localhost";

    public KafkaProtocolSettings() {
        setProtocol("kafka");
    }

    public String getAdvertisedHost() {
        return advertisedHost;
    }

    public void setAdvertisedHost(String advertisedHost) {
        this.advertisedHost = advertisedHost;
    }
}
