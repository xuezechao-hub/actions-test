package com.huawei.flowcontrol.core.config;

import com.huawei.apm.core.config.BaseConfig;
import com.huawei.apm.core.config.ConfigTypeKey;

@ConfigTypeKey("flow.control.plugin")
public class FlowControlConfig implements BaseConfig {
    /**
     * 流控插件kafka地址
     */
    private String kafkaBootstrapServers = "127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094";

    /**
     * 流控插件redis集群地址
     */
    private String redisUris = ConfigConst.REDIS_URIS;

    /**
     * 单机redis地址
     */
    private String redisHost = "localhost";

    /**
     * 单机redis端口
     */
    private String redisPort = "6379";

    /**
     * sentinel的版本
     */
    private String sentinelVersion = "1.8.0";

    /**
     * 流控插件zk地址
     */
    private String sentinelZookeeperAddress = "127.0.0.1:2181";

    /**
     * 流控相关配置在zk中的node
     */
    private String sentinelZookeeperPath = "/sentinel_rule_config";

    /**
     * sentinel配置参数 心跳发送默认间隔时间，单位毫秒
     */
    private long sentinelHeartbeatInterval = CommonConst.SENTINEL_HEARTBEAT_INTERVAL;

    /**
     * sentinel配置参数 流控信息数据发送默认间隔时间，单位毫秒
     */
    private long sentinelMetricInterval = CommonConst.SENTINEL_METRIC_INTERVAL;

    /**
     * sentinel配置参数 启动后初始加载流控信息数据的时间段时长
     */
    private long metricInitialDuration = CommonConst.METRIC_INITIAL_DURATION;

    /**
     * sentinel配置参数 未提供查询流控信息数据结束时间的默认加载数据条数
     */
    private long metricMaxLine = CommonConst.METRIC_MAX_LINE;

    /**
     * sentinel配置参数 查询流控数据时,睡眠一段时间，等待限流数据写入文件再查询
     */
    private long metricSleepTime = CommonConst.METRIC_SLEEP_TIME;

    /**
     * kafka配置参数 key序列化
     */
    private String kafkaKeySerializer = "org.apache.kafka.common.serialization.StringSerializer";

    /**
     * kafka配置参数 value序列化
     */
    private String kafkaValueSerializer = "org.apache.kafka.common.serialization.StringSerializer";

    /**
     * kafka配置参数 流控信息数据发送topic名称
     */
    private String kafkaMetricTopic = "topic-metric";

    /**
     * kafka配置参数 心跳数据发送topic名称
     */
    private String kafkaHeartbeatTopic = "topic-heartbeat";

    /**
     * kafka配置参数 producer需要server接收到数据之后发出的确认接收的信号 ack 0,1,all
     */
    private long kafkaAcks = 1L;

    /**
     * kafka配置参数 控制生产者发送请求最大大小,默认1M （这个参数和Kafka主机的message.max.bytes 参数有关系）
     */
    private long kafkaMaxRequestSize = CommonConst.KAFKA_MAX_REQUEST_SIZE;

    /**
     * kafka配置参数 生产者内存缓冲区大小 32M
     */
    private long kafkaBufferMemory = CommonConst.KAFKA_BUFFER_MEMORY;

    /**
     * kafka配置参数 重发消息次数
     */
    private long kafkaRetries = 0L;

    /**
     * kafka配置参数 客户端将等待请求的响应的最大时间
     */
    private long kafkaRequestTimeoutMs = CommonConst.KAFKA_REQUEST_TIMEOUT_MS;

    /**
     * kafka配置参数 最大阻塞时间，超过则抛出异常
     */
    private long kafkaMaxBlockMs = CommonConst.KAFKA_MAX_BLOCK_MS;

    /**
     * 配置jaas前缀
     */
    private String kafkaJaasConfig = ConfigConst.KAFKA_JAAS_CONFIG;

    /**
     * SASL鉴权机制
     */
    private String kafkaSaslMechanism = ConfigConst.KAFKA_SASL_MECHANISM;

    /**
     * 加密协议，目前支持SASL_SSL协议
     */
    private String kafkaSecurityProtocol = ConfigConst.KAFKA_SECURITY_PROTOCOL;

    /**
     * ssl truststore文件存放位置
     */
    private String kafkaSslTruststoreLocation = ConfigConst.KAFKA_SSL_TRUSTSTORE_LOCATION;

    /**
     * ssl truststore密码配置
     */
    private String kafkaSslTruststorePassword = ConfigConst.KAFKA_SSL_TRUSTSTORE_PASSWORD;

    /**
     * 域名不校验
     */
    private String kafkaIdentificationAlgorithm = ConfigConst.KAFKA_IDENTIFICATION_ALGORITHM;

    /**
     * 是否通过ssl认证
     */
    private boolean kafkaIsSsl = ConfigConst.KAFKA_IS_SSL;

    /**
     * 开发环境配置文件，默认为dev
     */
    private String configProfileActive = "dev";

    /**
     * 配置流控插件
     */
    private String configZookeeperPath = "/sentinel_plugin_config";

    /**
     * 对接的配置中心类型，当前支持zookeeper、servicecomb-kie两种类型
     * 默认为对接zookeeper，对接kie时请改为servicecomb-kie
     */
    private String sentinelConfigCenterType = "zookeeper";

    /**
     * servicecomb-kie地址，当配置中心配为servicecomb-kie时需填写正确的kie服务地址
     */
    private String configKieAddress = "http://localhost:30110";

    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public void setKafkaBootstrapServers(String kafkaBootstrapServers) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    public String getRedisUris() {
        return redisUris;
    }

    public void setRedisUris(String redisUris) {
        this.redisUris = redisUris;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }

    public String getRedisPort() {
        return redisPort;
    }

    public void setRedisPort(String redisPort) {
        this.redisPort = redisPort;
    }

    public String getSentinelVersion() {
        return sentinelVersion;
    }

    public void setSentinelVersion(String sentinelVersion) {
        this.sentinelVersion = sentinelVersion;
    }

    public String getSentinelZookeeperAddress() {
        return sentinelZookeeperAddress;
    }

    public void setSentinelZookeeperAddress(String sentinelZookeeperAddress) {
        this.sentinelZookeeperAddress = sentinelZookeeperAddress;
    }

    public String getSentinelZookeeperPath() {
        return sentinelZookeeperPath;
    }

    public void setSentinelZookeeperPath(String sentinelZookeeperPath) {
        this.sentinelZookeeperPath = sentinelZookeeperPath;
    }

    public long getSentinelHeartbeatInterval() {
        return sentinelHeartbeatInterval;
    }

    public void setSentinelHeartbeatInterval(long sentinelHeartbeatInterval) {
        this.sentinelHeartbeatInterval = sentinelHeartbeatInterval;
    }

    public long getSentinelMetricInterval() {
        return sentinelMetricInterval;
    }

    public void setSentinelMetricInterval(long sentinelMetricInterval) {
        this.sentinelMetricInterval = sentinelMetricInterval;
    }

    public long getMetricInitialDuration() {
        return metricInitialDuration;
    }

    public void setMetricInitialDuration(long metricInitialDuration) {
        this.metricInitialDuration = metricInitialDuration;
    }

    public long getMetricMaxLine() {
        return metricMaxLine;
    }

    public void setMetricMaxLine(long metricMaxLine) {
        this.metricMaxLine = metricMaxLine;
    }

    public long getMetricSleepTime() {
        return metricSleepTime;
    }

    public void setMetricSleepTime(long metricSleepTime) {
        this.metricSleepTime = metricSleepTime;
    }

    public String getKafkaKeySerializer() {
        return kafkaKeySerializer;
    }

    public void setKafkaKeySerializer(String kafkaKeySerializer) {
        this.kafkaKeySerializer = kafkaKeySerializer;
    }

    public String getKafkaValueSerializer() {
        return kafkaValueSerializer;
    }

    public void setKafkaValueSerializer(String kafkaValueSerializer) {
        this.kafkaValueSerializer = kafkaValueSerializer;
    }

    public String getKafkaMetricTopic() {
        return kafkaMetricTopic;
    }

    public void setKafkaMetricTopic(String kafkaMetricTopic) {
        this.kafkaMetricTopic = kafkaMetricTopic;
    }

    public String getKafkaHeartbeatTopic() {
        return kafkaHeartbeatTopic;
    }

    public void setKafkaHeartbeatTopic(String kafkaHeartbeatTopic) {
        this.kafkaHeartbeatTopic = kafkaHeartbeatTopic;
    }

    public long getKafkaAcks() {
        return kafkaAcks;
    }

    public void setKafkaAcks(long kafkaAcks) {
        this.kafkaAcks = kafkaAcks;
    }

    public long getKafkaMaxRequestSize() {
        return kafkaMaxRequestSize;
    }

    public void setKafkaMaxRequestSize(long kafkaMaxRequestSize) {
        this.kafkaMaxRequestSize = kafkaMaxRequestSize;
    }

    public long getKafkaBufferMemory() {
        return kafkaBufferMemory;
    }

    public void setKafkaBufferMemory(long kafkaBufferMemory) {
        this.kafkaBufferMemory = kafkaBufferMemory;
    }

    public long getKafkaRetries() {
        return kafkaRetries;
    }

    public void setKafkaRetries(long kafkaRetries) {
        this.kafkaRetries = kafkaRetries;
    }

    public long getKafkaRequestTimeoutMs() {
        return kafkaRequestTimeoutMs;
    }

    public void setKafkaRequestTimeoutMs(long kafkaRequestTimeoutMs) {
        this.kafkaRequestTimeoutMs = kafkaRequestTimeoutMs;
    }

    public long getKafkaMaxBlockMs() {
        return kafkaMaxBlockMs;
    }

    public void setKafkaMaxBlockMs(long kafkaMaxBlockMs) {
        this.kafkaMaxBlockMs = kafkaMaxBlockMs;
    }

    public String getKafkaJaasConfig() {
        return kafkaJaasConfig;
    }

    public void setKafkaJaasConfig(String kafkaJaasConfig) {
        this.kafkaJaasConfig = kafkaJaasConfig;
    }

    public String getKafkaSaslMechanism() {
        return kafkaSaslMechanism;
    }

    public void setKafkaSaslMechanism(String kafkaSaslMechanism) {
        this.kafkaSaslMechanism = kafkaSaslMechanism;
    }

    public String getKafkaSecurityProtocol() {
        return kafkaSecurityProtocol;
    }

    public void setKafkaSecurityProtocol(String kafkaSecurityProtocol) {
        this.kafkaSecurityProtocol = kafkaSecurityProtocol;
    }

    public String getKafkaSslTruststoreLocation() {
        return kafkaSslTruststoreLocation;
    }

    public void setKafkaSslTruststoreLocation(String kafkaSslTruststoreLocation) {
        this.kafkaSslTruststoreLocation = kafkaSslTruststoreLocation;
    }

    public String getKafkaSslTruststorePassword() {
        return kafkaSslTruststorePassword;
    }

    public void setKafkaSslTruststorePassword(String kafkaSslTruststorePassword) {
        this.kafkaSslTruststorePassword = kafkaSslTruststorePassword;
    }

    public String getKafkaIdentificationAlgorithm() {
        return kafkaIdentificationAlgorithm;
    }

    public void setKafkaIdentificationAlgorithm(String kafkaIdentificationAlgorithm) {
        this.kafkaIdentificationAlgorithm = kafkaIdentificationAlgorithm;
    }

    public boolean isKafkaIsSsl() {
        return kafkaIsSsl;
    }

    public void setKafkaIsSsl(boolean kafkaIsSsl) {
        this.kafkaIsSsl = kafkaIsSsl;
    }

    public String getConfigProfileActive() {
        return configProfileActive;
    }

    public void setConfigProfileActive(String configProfileActive) {
        this.configProfileActive = configProfileActive;
    }

    public String getConfigZookeeperPath() {
        return configZookeeperPath;
    }

    public void setConfigZookeeperPath(String configZookeeperPath) {
        this.configZookeeperPath = configZookeeperPath;
    }

    public String getSentinelConfigCenterType() {
        return sentinelConfigCenterType;
    }

    public void setSentinelConfigCenterType(String sentinelConfigCenterType) {
        this.sentinelConfigCenterType = sentinelConfigCenterType;
    }

    public String getConfigKieAddress() {
        return configKieAddress;
    }

    public void setConfigKieAddress(String configKieAddress) {
        this.configKieAddress = configKieAddress;
    }
}
