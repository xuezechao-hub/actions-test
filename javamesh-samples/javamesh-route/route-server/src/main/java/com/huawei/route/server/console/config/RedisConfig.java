/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2021. All rights reserved.
 */

package com.huawei.route.server.console.config;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * redis配置类
 *
 * @author XiaoLong Wang
 * @since 2020-12-21
 */
@Configuration
public class RedisConfig {
    private static final int MAX_WAIT_MILLIONS = 2000;

    private static final Pattern PATTERN = Pattern.compile("^[-\\+]?[\\d]*$");

    private static final long REFRESH_TRIGGERS_TIMEOUT = 30L;

    private static final long PERIODIC_REFRESH = 20L;

    private static final int MAX_REDIRECTS_DEFAULT = 5;

    private static final int MAX_IDLE_DEFAULT = 10;

    private static final int MIN_IDLE_DEFAULT = 1;

    private static final int MAX_TOTAL_DEFAULT = 10;

    private final int ipsLength = 2;

    @Autowired
    private RedisProperties redisProperties;

    /**
     * redis 模板设置
     *
     * @param redisConnectionFactory 连接工厂
     * @return redis模板
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        FastJsonRedisSerializer fastJsonRedisSerializer = new FastJsonRedisSerializer(Object.class);

        // 设置值（value）的序列化采用FastJsonRedisSerializer。
        redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);

        // 设置键（key）的序列化采用StringRedisSerializer。
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 添加node
     *
     * @param clusterNodes 集群地址集合
     * @return RedisNode集合
     */
    private Set<RedisNode> setClusterNodes(List<String> clusterNodes) {
        Set<RedisNode> nodes = new HashSet<RedisNode>();
        if (clusterNodes != null && !clusterNodes.isEmpty()) {
            clusterNodes.forEach(address -> {
                if (StringUtils.isNotEmpty(address)) {
                    String[] ips = address.split(":");
                    if (ips.length == ipsLength && PATTERN.matcher(ips[1]).matches()) {
                        nodes.add(new RedisNode(ips[0].trim(), Integer.valueOf(ips[1])));
                    }
                }
            });
        }
        return nodes;
    }

    /**
     * 设置重试
     *
     * @return lettuce工厂
     * @throws Exception
     */
    @Bean(destroyMethod = "destroy")
    public LettuceConnectionFactory lettuceConnectionFactoryUvPv() throws Exception {
        List<String> clusterNodes = redisProperties.getCluster().getNodes();
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
        if (clusterNodes != null && !clusterNodes.isEmpty()) {
            clusterConfiguration.setClusterNodes(setClusterNodes(clusterNodes));
            clusterConfiguration.setPassword(RedisPassword.of(redisProperties.getPassword()));

            // 判断是否设置了指定参数，没有设置添加默认值
            int maxRedirects = redisProperties.getCluster().getMaxRedirects();
            clusterConfiguration.setMaxRedirects("".equals(maxRedirects) ? MAX_REDIRECTS_DEFAULT : maxRedirects);
            int maxIdle = redisProperties.getLettuce().getPool().getMaxIdle();
            poolConfig.setMaxIdle(maxIdle != 0 ? maxIdle : MAX_IDLE_DEFAULT);
            int minIdle = redisProperties.getLettuce().getPool().getMinIdle();
            poolConfig.setMinIdle(minIdle != 0 ? minIdle : MIN_IDLE_DEFAULT);
            int maxTotal = redisProperties.getLettuce().getPool().getMaxActive();
            poolConfig.setMaxTotal(maxTotal != 0 ? maxTotal : MAX_TOTAL_DEFAULT);
            return new LettuceConnectionFactory(clusterConfiguration, getLettuceClientConfiguration(poolConfig));
        } else {
            RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
            redisStandaloneConfiguration.setPassword(redisProperties.getPassword());
            redisStandaloneConfiguration.setDatabase(redisProperties.getDatabase());
            redisStandaloneConfiguration.setPort(redisStandaloneConfiguration.getPort());
            redisStandaloneConfiguration.setHostName(redisProperties.getHost());
            return new LettuceConnectionFactory(redisStandaloneConfiguration, getLettuceClientConfiguration(poolConfig));
        }
    }

    /**
     * 配置LettuceClientConfiguration 包括线程池配置和安全项配置
     *
     * @param genericObjectPoolConfig common-pool2线程池
     * @return lettuceClientConfiguration
     */
    private LettuceClientConfiguration getLettuceClientConfiguration(GenericObjectPoolConfig genericObjectPoolConfig) {
        /*
        ClusterTopologyRefreshOptions配置用于开启自适应刷新和定时刷新。如自适应刷新不开启，Redis集群变更时将会导致连接异常！
         */
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()

                // 开启所有自适应刷新，MOVED，ASK，PERSISTENT都会触发
                .enableAllAdaptiveRefreshTriggers()

                // 自适应刷新超时时间(默认30秒)，刷新redis最新的拓扑结构
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(REFRESH_TRIGGERS_TIMEOUT))

                // 开周期刷新，每隔20秒自动刷新
                .enablePeriodicRefresh(Duration.ofSeconds(PERIODIC_REFRESH))
                .build();
        return LettucePoolingClientConfiguration.builder()

                // 连接池配置
                .poolConfig(genericObjectPoolConfig)

                // 一定要设置lettuce对redis集群拓扑结构图的刷新策略，不然会导致很多次从导向请求
                .clientOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build())

                // 一定要设置命令执行超时时间，不然这个时间是默认60秒，会导致请求超时
                .commandTimeout(Duration.ofMillis(MAX_WAIT_MILLIONS))
                .build();
    }
}
