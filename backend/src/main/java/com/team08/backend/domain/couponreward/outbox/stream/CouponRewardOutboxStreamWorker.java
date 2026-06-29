package com.team08.backend.domain.couponreward.outbox.stream;

import com.team08.backend.domain.couponreward.outbox.service.CouponRewardOutboxWorker;
import com.team08.backend.global.redis.stream.AbstractStreamListenerWorker;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class CouponRewardOutboxStreamWorker extends AbstractStreamListenerWorker {

    static final String GROUP_NAME = "coupon-reward-outbox-workers";
    private final String consumerName = "coupon-reward-outbox-worker-" + UUID.randomUUID();

    private final CouponRewardOutboxWorker couponRewardOutboxWorker;

    public CouponRewardOutboxStreamWorker(StringRedisTemplate redisTemplate, CouponRewardOutboxWorker couponRewardOutboxWorker) {
        super(redisTemplate);
        this.couponRewardOutboxWorker = couponRewardOutboxWorker;
    }

    public String consumerName() {
        return consumerName;
    }

    @Override
    protected String getStreamKey() {
        return CouponRewardOutboxStreamPublishListener.STREAM_KEY;
    }

    @Override
    protected String getGroupName() {
        return GROUP_NAME;
    }

    @Override
    protected void processRecord(MapRecord<String, String, String> record) throws Exception {
        Map<String, String> value = record.getValue();
        Long outboxEventId = Long.valueOf(value.get("outboxEventId"));
        couponRewardOutboxWorker.processOne(outboxEventId);
    }
}
