package com.bluecone.app.infra.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.infra.outbox.core.EventSerializer;
import com.bluecone.app.infra.outbox.core.TransactionalOutboxEventPublisher;
import com.bluecone.app.infra.outbox.entity.OutboxMessageEntity;
import com.bluecone.app.infra.outbox.entity.OutboxMessageStatus;
import com.bluecone.app.infra.outbox.mapper.OutboxMessageMapper;
import com.bluecone.app.infra.outbox.service.OutboxDispatchService;
import com.bluecone.app.infra.outbox.service.OutboxStoreService;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.infra.test.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class OutboxDispatchServiceIT extends AbstractIntegrationTest {

    @Autowired
    private OutboxDispatchService dispatchService;

    @Autowired
    private OutboxMessageMapper messageMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RecordingOrderEventHandler handler;

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        flushRedis();
    }

    @Test
    void publishesAndDispatchesEventsFromOutbox() {
        TenantContext.setTenantId("3001");
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> domainEventPublisher.publish(new TestOrderPaidEvent(55L, new BigDecimal("18.80"))));

        dispatchService.dispatchDueMessages();

        List<OutboxMessageEntity> messages = messageMapper.selectList(null);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getStatus()).isEqualTo(OutboxMessageStatus.DONE);
        assertThat(messages.get(0).getTenantId()).isEqualTo(3001L);

        assertThat(handler.events()).hasSize(1);
        assertThat(handler.events().get(0).getOrderId()).isEqualTo(55L);
    }

    @Configuration
    static class TestConfig {

        @Bean
        RecordingOrderEventHandler recordingOrderEventHandler() {
            return new RecordingOrderEventHandler();
        }

        @Bean
        @Primary
        DomainEventPublisher domainEventPublisher(OutboxStoreService storeService, EventSerializer serializer) {
            return new TransactionalOutboxEventPublisher(storeService, serializer);
        }

        @Bean
        EventHandler<TestOrderPaidEvent> testOrderPaidHandler(RecordingOrderEventHandler delegate) {
            return delegate;
        }
    }

    static class RecordingOrderEventHandler implements EventHandler<TestOrderPaidEvent> {

        private final CopyOnWriteArrayList<TestOrderPaidEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void handle(TestOrderPaidEvent event) {
            events.add(event);
        }

        List<TestOrderPaidEvent> events() {
            return events;
        }
    }

    static class TestOrderPaidEvent extends DomainEvent {

        private final Long orderId;
        private final BigDecimal amount;

        TestOrderPaidEvent(Long orderId, BigDecimal amount) {
            super("order.paid.test", EventMetadata.of(Map.of("traceId", "trace-it")));
            this.orderId = orderId;
            this.amount = amount;
        }

        public Long getOrderId() {
            return orderId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            return "TestOrderPaidEvent{" +
                    "orderId=" + orderId +
                    ", amount=" + amount +
                    ", eventId=" + getEventId() +
                    '}';
        }
    }
}
