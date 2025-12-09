package com.bluecone.app.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void markPaidMovesToPendingAcceptUnlessAlreadyFinal() {
        Order pendingPayment = Order.builder()
                .status(OrderStatus.PENDING_PAYMENT)
                .items(Collections.emptyList())
                .build();

        pendingPayment.markPaid();

        // fix: actual state transition is WAIT_ACCEPT after payment
        assertThat(pendingPayment.getStatus()).isEqualTo(OrderStatus.WAIT_ACCEPT);
        assertThat(pendingPayment.getPayStatus()).isEqualTo(PayStatus.PAID);

        Order completed = Order.builder()
                .status(OrderStatus.COMPLETED)
                .items(Collections.emptyList())
                .build();

        completed.markPaid();

        assertThat(completed.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(completed.getPayStatus()).isEqualTo(PayStatus.PAID);
    }

    @Test
    void cancelByUserOnlyWorksInCancelableStates() {
        Order draft = Order.builder()
                .status(OrderStatus.DRAFT)
                .items(Collections.emptyList())
                .build();

        assertThat(draft.canCancelByUser()).isTrue();
        draft.cancelByUser();
        assertThat(draft.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Order inProgress = Order.builder()
                .status(OrderStatus.IN_PROGRESS)
                .items(Collections.emptyList())
                .build();

        assertThat(inProgress.canCancelByUser()).isFalse();
        assertThatThrownBy(inProgress::cancelByUser)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void assertEditablePreventsMutationsOutsideDraft() {
        Order draft = Order.builder()
                .status(OrderStatus.DRAFT)
                .items(Collections.emptyList())
                .build();

        draft.assertEditable(); // should not throw

        Order locked = Order.builder()
                .status(OrderStatus.PENDING_PAYMENT)
                .items(Collections.emptyList())
                .build();

        assertThatThrownBy(locked::assertEditable)
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不可编辑");
    }
}
