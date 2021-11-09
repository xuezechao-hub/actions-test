/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2022. All rights reserved.
 */

package com.huawei.flowrecordreplay.console.rtc.consumer;

import com.huawei.flowrecordreplay.console.rtc.common.redis.RedisUtil;
import com.huawei.flowrecordreplay.console.rtc.common.utils.CommonTools;
import com.huawei.flowrecordreplay.console.rtc.common.utils.RtcCoreConstants;
import com.huawei.flowrecordreplay.console.rtc.consumer.strategy.InterfaceTopicHandleStrategy;
import com.huawei.flowrecordreplay.console.rtc.consumer.strategy.TopicHandleStrategyFactory;

import io.lettuce.core.RedisException;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 自定义kafka消费者
 *
 * @author hanpeng
 * @since 2021-04-07
 */
@Component
public class RtcConsumer implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RtcConsumer.class);
    private static final long SLEEP_MS = 500L;
    /**
     * 自动注入kafka消费者对象
     */
    @Autowired
    private KafkaConsumer<String, String> consumer;
    /**
     * 重平衡处理类
     */
    @Autowired
    private ConsumerRebalanceListener consumerRebalanceListener;

    /**
     * 消息的处理工厂
     */
    @Autowired
    private TopicHandleStrategyFactory topicHandleStrategyFactory;

    /**
     * 自动注入redis工具类的对象
     */
    @Autowired
    private RedisUtil redisUtil;
    /**
     * 重平衡时设置的安全偏移量，以防丢数据，即便可能是重复消费
     */
    @Value("${kafka.consumer.rebalance.safeOffset:1000}")
    private long safeOffset;

    /**
     * 是否循环拉取的标志
     */
    private volatile boolean isRunning = true;

    /**
     * 主题集
     */
    @Value("${topics:topic-heartbeat}")
    private String topics;

    /**
     * kafka消费者一次拉取时的超时时间
     */
    @Value("${kafka.consumer.poll.timeout:1000}")
    private long timeout;

    /**
     * kafka消费者的启动方法，该方法在容器启动的时候调用，并不断地循环处理数据，在方法内部发生异常时停止循环，在停止之前同步提交
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        final Thread mainThread = Thread.currentThread();
        setExit(mainThread);
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, topics.split(","));
        consumer.subscribe(list, consumerRebalanceListener);

        // 在kafka消费者在启动的时候，指定partition消费，也要执行consumer.poll(0)，目的是为了获取offset;
        firstPoll();

        // 正式拉取并处理消息
        pollAndHandleMessage();
    }

    private void pollAndHandleMessage() {
        LOGGER.debug("Begin to consume");

        // 实时拉取
        while (isRunning) {
            try {
                pollOnce();
            } catch (Exception exception) {
                LOGGER.error("exception during exec", exception);
            }
        }
    }

    private void pollOnce() {
        try {
            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(timeout));
            LOGGER.info("one poll records sum is {}", consumerRecords.count());
            if (consumerRecords.isEmpty()) {
                return;
            }
            Set<TopicPartition> partitions = consumerRecords.partitions();
            for (TopicPartition topicPartition : partitions) {
                List<ConsumerRecord<String, String>> records = consumerRecords.records(topicPartition);
                String topic = topicPartition.topic();
                LOGGER.debug("topic：{}", topic);

                // 逻辑处理
                logicProcess(topic, records, topicPartition);

                // 异步提交
                commitAsynchronized(records, topicPartition);
            }
        } catch (WakeupException e) {
            LOGGER.error("WakeupException：", e);
        }
    }

    private void logicProcess(String topic, List<ConsumerRecord<String, String>> records,
                              TopicPartition topicPartition) {
        InterfaceTopicHandleStrategy handler = topicHandleStrategyFactory.getTopicHandleStrategy(topic);
        try {
            handler.handleRecordByTopic(records);

            // 实时记录redis中各分区的消费偏移量offset
            String offset = String.valueOf(records.get(records.size() - 1).offset() + 1);
            redisUtil.set(topicPartition.topic() + ":" + topicPartition.partition(), offset);
            LOGGER.debug("real time partition：{}, offset:{}", topicPartition, offset);
        } catch (RedisException ex) {
            LOGGER.error("task process error", ex);
        }
    }

    /**
     * 优雅退出：
     * <p>
     * 退出循环需要通过另一个线程调用consumer.wakeup()方法
     * 调用consumer.wakeup()可以退出poll(),并抛出WakeupException异常
     * 我们不需要处理 WakeupException,因为它只是用于跳出循环的一种方式
     * consumer.wakeup()是消费者唯一一个可以从其他线程里安全调用的方法
     * 如果循环运行在主线程里，可以在 ShutdownHook里调用该方法
     *
     * @param mainThread 主线程
     */
    private void setExit(Thread mainThread) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.debug("consumer Starting exit...");
            consumer.wakeup();
            isRunning = false;
            close();
            try {
                // 主线程继续执行，以便可以关闭consumer，提交偏移量
                mainThread.join();
            } catch (InterruptedException e) {
                LOGGER.error("occur exception during executing the method addShutdownHook():", e);
            }
        }));
    }

    private void firstPoll() {
        Set<TopicPartition> assignmentPartitions = new HashSet<>();
        while (assignmentPartitions.isEmpty()) {
            try {
                consumer.poll(Duration.ZERO);
                assignmentPartitions = consumer.assignment();
            } catch (IllegalArgumentException | IllegalStateException e) {
                break;
            }
        }
        CommonTools.consumePartitionOffsetAssigned(redisUtil, consumer,
                assignmentPartitions, safeOffset);
    }

    /**
     * 关闭消费者资源
     */
    public void close() {
        isRunning = false;
        if (consumer != null) {
            consumer.close();
        }
    }

    /**
     * consumer异步提交方法，以分区为提交单位
     *
     * @param recordsInPartition 分区中的记录数
     * @param topicPartition     主题分区对象
     */
    private void commitAsynchronized(List<ConsumerRecord<String, String>> recordsInPartition,
                                     TopicPartition topicPartition) {
        long lastConsumedOffset = recordsInPartition.get(recordsInPartition.size() - 1).offset();
        consumer.commitAsync(
                Collections.singletonMap(
                        topicPartition,
                        new OffsetAndMetadata(lastConsumedOffset + 1)
                ),
                (offsets, exception) -> retry(offsets, exception)
        );
    }

    private void retry(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
        int retries = RtcCoreConstants.RETRIES;
        if (exception == null) {
            // 异步提交成功
            LOGGER.debug("commitAsynchronized：{}", offsets.entrySet().toArray()[0]);
        } else {
            // 异步提交失败
            LOGGER.error(exception.toString(), exception);
            while (retries > 0) {
                retries--;

                // 提交重试
                consumer.commitSync(offsets);
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e) {
                    LOGGER.error("{}/{}commitSync failed", retries, RtcCoreConstants.RETRIES, e);
                }
            }
        }
    }
}
