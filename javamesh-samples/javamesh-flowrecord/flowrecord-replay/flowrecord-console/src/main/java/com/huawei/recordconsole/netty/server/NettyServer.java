/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2022. All rights reserved.
 */

package com.huawei.recordconsole.netty.server;

import com.huawei.recordconsole.netty.common.conf.KafkaConf;
import com.huawei.recordconsole.netty.common.exception.KafkaTopicException;
import com.huawei.recordconsole.netty.kafka.KafkaProducerManager;
import com.huawei.recordconsole.netty.pojo.Message;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 网关服务端
 *
 * @author lilai
 * @version 0.0.1
 * @since 2021-07-12
 */
@Component
public class NettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);

    // 最大的连接等待数量
    private static final int CONNECTION_SIZE = 1024;

    // 读等待时间
    @Value("${netty.wait.time}")
    private int readWaitTime = 60;

    // 网关端口
    @Value("${netty.port}")
    private int port;

    // kafka配置文件加载
    @Autowired
    private KafkaConf conf;

    /**
     * 服务端核心方法
     * 随tomcat启动被拉起，处理客户端连接和数据
     */
    @PostConstruct
    public void start() {
        LOGGER.info("Starting the netty server...");

        // 处理连接的线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);

        // 处理数据的线程组
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            KafkaProducer<String, Bytes> producer = KafkaProducerManager.getInstance(conf).getProducer();
            serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, CONNECTION_SIZE)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        ChannelPipeline pipeline = channel.pipeline();

                        // 如果超过读等待时间还是没有收到对应客户端，触发读等待事件
                        pipeline.addLast(new IdleStateHandler(readWaitTime, 0, 0));
                        pipeline.addLast(new ProtobufVarint32FrameDecoder());
                        pipeline.addLast(new ProtobufDecoder(Message.NettyMessage.getDefaultInstance()));
                        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                        pipeline.addLast(new ProtobufEncoder());
                        pipeline.addLast(new ServerHandler(producer, conf));
                    }
                });

            // 同步阻塞等待服务启动
            serverBootstrap.bind(port).sync();
            LOGGER.info("Netty server start");
        } catch (InterruptedException | KafkaTopicException e) {
            LOGGER.error("Exception occurs when start netty server, exception message : {}", e);
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
