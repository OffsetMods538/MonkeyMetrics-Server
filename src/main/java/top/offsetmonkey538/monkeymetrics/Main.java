package top.offsetmonkey538.monkeymetrics;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

import java.io.IOException;

public class Main {
    public static final PrometheusRegistry REGISTRY = new PrometheusRegistry();
    public static final Counter MOD_COUNTER = Counter.builder()
            .name("mod_launches")
            .help("Total launches of mods and their environments")
            .labelNames("mod", "mod_version", "minecraft_version", "environment", "mod_loader")
            .register(REGISTRY);
    public static final Counter ENVIRONMENT_COUNTER = Counter.builder()
            .name("minecraft_launches")
            .help("Total launches of modded minecraft environments")
            .labelNames("minecraft_version", "environment", "mod_loader")
            .register(REGISTRY);

    static void main() {
        final Thread nettyThread = new Thread(Main::initNetty);
        final Thread prometheusThread = new Thread(Main::initPrometheus);
        nettyThread.start();
        prometheusThread.start();
    }

    private static void initNetty() {
        final Class<? extends ServerChannel> channelClass;
        final EventLoopGroup eventLoopGroup;
        if (Epoll.isAvailable()) {
            channelClass = EpollServerSocketChannel.class;
            eventLoopGroup = new MultiThreadIoEventLoopGroup(EpollIoHandler.newFactory());
        } else {
            channelClass = NioServerSocketChannel.class;
            eventLoopGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        }

        new ServerBootstrap().channel(channelClass).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                final ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new HttpObjectAggregator(65536));
                pipeline.addLast(new IngressHttpHandler());
            }
        }).group(eventLoopGroup).localAddress(6969).bind().syncUninterruptibly();
    }

    private static void initPrometheus() {
        try {
            HTTPServer server = HTTPServer.builder()
                    .port(9696)
                    .registry(REGISTRY)
                    .buildAndStart();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
