Global socks5 proxy 

```java
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.ReferenceCountUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Socks5Redirector {

  // Redirection Map: "OriginalHost:OriginalPort" -> "NewHost:NewPort"
  private static final Map<String, String> REDIRECTION_MAP = new ConcurrentHashMap<>();

  static {
    // Example: Redirect traffic intended for "mysql-prod:3306" to "localhost:9000" (Target2)
    REDIRECTION_MAP.put("127.0.0.1:3306", "127.0.0.1:9000");
    REDIRECTION_MAP.put("kafka-broker:9092", "127.0.0.1:9093");
  }

  public static void main(String[] args) throws Exception {
    int proxyPort = 1080;
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
              .channel(NioServerSocketChannel.class)
              .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                  ch.pipeline().addLast(
                          new Socks5ServerEncoder(),
                          new Socks5InitialRequestDecoder(),
                          new Socks5CommandRequestDecoder(),
                          new Socks5SeralizerHandler()
                  );
                }
              });

      System.out.println("SOCKS5 Proxy started on port " + proxyPort);
      b.bind(proxyPort).sync().channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  /**
   * Logic Handler: Handles SOCKS5 Handshake and Command (Connect)
   */
  static class Socks5SeralizerHandler extends SimpleChannelInboundHandler<Socks5Message> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5Message msg) {
      if (msg instanceof Socks5InitialRequest) {
        // Phase 1: Handshake (No Auth)
        ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
      } else if (msg instanceof Socks5CommandRequest) {
        // Phase 2: Connect Request
        Socks5CommandRequest request = (Socks5CommandRequest) msg;
        String originalAddr = request.dstAddr() + ":" + request.dstPort();

        // Logic: Check if we need to redirect
        String destination = REDIRECTION_MAP.getOrDefault(originalAddr, originalAddr);
        String[] parts = destination.split(":");
        String finalHost = parts[0];
        int finalPort = Integer.parseInt(parts[1]);

        if (!destination.equals(originalAddr)) {
          System.out.println("[REDIRECTED] " + originalAddr + " -> " + destination);
        } else {
          System.out.println("[DIRECT] Connecting to " + originalAddr);
        }

        connectToTarget(ctx, finalHost, finalPort);
      }
    }

    private void connectToTarget(ChannelHandlerContext clientCtx, String host, int port) {
      Bootstrap b = new Bootstrap();
      b.group(clientCtx.channel().eventLoop())
              .channel(NioSocketChannel.class)
              .handler(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelActive(ChannelHandlerContext targetCtx) {
                  // Tell the source the connection is established
                  clientCtx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4));

                  // Create the bi-directional bridge (Relay)
                  clientCtx.pipeline().addLast(new RelayHandler(targetCtx.channel()));
                  targetCtx.pipeline().addLast(new RelayHandler(clientCtx.channel()));
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                  clientCtx.close();
                }
              });

      b.connect(host, port).addListener((ChannelFutureListener) future -> {
        if (!future.isSuccess()) {
          clientCtx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4));
          clientCtx.close();
        }
      });
    }
  }

  /**
   * Relay Handler: Simply pipes bytes from one channel to another
   */
  static class RelayHandler extends ChannelInboundHandlerAdapter {
    private final Channel relayChannel;

    public RelayHandler(Channel relayChannel) {
      this.relayChannel = relayChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if (relayChannel.isActive()) {
        relayChannel.writeAndFlush(msg);
      } else {
        ReferenceCountUtil.release(msg);
      }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      if (relayChannel.isActive()) {
        relayChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      ctx.close();
    }
  }
}
```

Initialized on machine by
```java
System.getProperties().put( "proxySet", "true" );
System.getProperties().put( "socksProxyHost", "127.0.0.1" );
System.getProperties().put( "socksProxyPort", "1234" );
```


## TODO

### Docs

* Explain priorities and replay
* Explain filter

### Code

* findIndex,readStorageItem
    * AMQP
    * Redis
    * Mongo
    * Jdbc (set)
    * Mysql (show warnings)
    * Http (favicon)
* Mocked specific responses for mongo, redis
* Expose the connections for protocol on apis
* Send mock messages amqp,mqtt,redis
* Http
    * test brotli compression
    * Use anonymous Object for serialization for HTTP Multipart

STATE CHARTS:

* https://www.ascii-code.com/
* https://github.com/klangfarbe/UML-Statechart-Framework-for-Java
* https://github.com/klangfarbe/UML-Statechart-Framework-for-Java
* https://www.graphviz.org/
* https://en.wikipedia.org/wiki/DOT_(graph_description_language)

Dev proxy like

* https://learn.microsoft.com/en-us/microsoft-cloud/dev/dev-proxy/technical-reference/authplugin
* https://learn.microsoft.com/en-us/microsoft-cloud/dev/dev-proxy/technical-reference/cachingguidanceplugin
* https://learn.microsoft.com/en-us/microsoft-cloud/dev/dev-proxy/technical-reference/executionsummaryplugin
* https://learn.microsoft.com/en-us/microsoft-cloud/dev/dev-proxy/technical-reference/openapispecgeneratorplugin
* https://learn.microsoft.com/en-us/microsoft-cloud/dev/dev-proxy/technical-reference/markdownreporter

AMQP: contentBytes
MONGO: documents not in json
MAIN_TODO: Marking the real TODOS

