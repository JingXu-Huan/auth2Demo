package com.example.im.util;

/**
 * Twitter 雪花算法
 * 生成全局唯一的消息ID
 */
public class SnowflakeIdWorker {

    // 起始时间戳 (2024-01-01 00:00:00)
    private static final long START_TIMESTAMP = 1704067200000L;

    // 机器ID所占位数
    private static final long WORKER_ID_BITS = 5L;
    // 数据中心ID所占位数
    private static final long DATA_CENTER_ID_BITS = 5L;
    // 序列号所占位数
    private static final long SEQUENCE_BITS = 12L;

    // 最大机器ID
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    // 最大数据中心ID
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
    // 最大序列号
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    // 偏移量
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    private static long workerId = 1L;
    private static long dataCenterId = 1L;
    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    private static final Object LOCK = new Object();

    /**
     * 生成下一个ID
     */
    public static long nextId() {
        synchronized (LOCK) {
            long timestamp = System.currentTimeMillis();

            if (timestamp < lastTimestamp) {
                throw new RuntimeException("Clock moved backwards. Refusing to generate id.");
            }

            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & MAX_SEQUENCE;
                if (sequence == 0) {
                    timestamp = waitNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0L;
            }

            lastTimestamp = timestamp;

            return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                    | (dataCenterId << DATA_CENTER_ID_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | sequence;
        }
    }

    /**
     * 等待下一毫秒
     */
    private static long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 设置机器ID和数据中心ID
     */
    public static void setWorkerIdAndDataCenterId(long wId, long dcId) {
        if (wId > MAX_WORKER_ID || wId < 0) {
            throw new IllegalArgumentException("Worker ID out of range");
        }
        if (dcId > MAX_DATA_CENTER_ID || dcId < 0) {
            throw new IllegalArgumentException("Data Center ID out of range");
        }
        workerId = wId;
        dataCenterId = dcId;
    }
}
