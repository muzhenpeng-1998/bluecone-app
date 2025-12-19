package com.bluecone.app.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void markPaidMovesToWaitAcceptUnlessAlreadyFinal() {
        // 使用 Canonical 状态 WAIT_PAY 测试
        Order waitPay = Order.builder()
                .status(OrderStatus.WAIT_PAY)
                .items(Collections.emptyList())
                .build();

        waitPay.markPaid();

        // 应流转到 WAIT_ACCEPT
        assertThat(waitPay.getStatus()).isEqualTo(OrderStatus.WAIT_ACCEPT);
        assertThat(waitPay.getPayStatus()).isEqualTo(PayStatus.PAID);

        // 测试终态不变更
        Order completed = Order.builder()
                .status(OrderStatus.COMPLETED)
                .items(Collections.emptyList())
                .build();

        completed.markPaid();

        assertThat(completed.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(completed.getPayStatus()).isEqualTo(PayStatus.PAID);
    }
    
    @Test
    void markPaidCompatibleWithLegacyPendingPaymentStatus() {
        // 测试兼容旧状态：PENDING_PAYMENT 也能正确流转
        Order pendingPayment = Order.builder()
                .status(OrderStatus.PENDING_PAYMENT)
                .items(Collections.emptyList())
                .build();

        pendingPayment.markPaid();

        // 应流转到 WAIT_ACCEPT
        assertThat(pendingPayment.getStatus()).isEqualTo(OrderStatus.WAIT_ACCEPT);
        assertThat(pendingPayment.getPayStatus()).isEqualTo(PayStatus.PAID);
    }

    @Test
    void cancelByUserOnlyWorksInCancelableStates() {
        Order draft = Order.builder()
                .status(OrderStatus.DRAFT)
                .items(Collections.emptyList())
                .build();

        assertThat(draft.canCancelByUser()).isTrue();
        draft.cancelByUser();
        // 使用 Canonical 状态 CANCELED
        assertThat(draft.getStatus()).isEqualTo(OrderStatus.CANCELED);

        Order inProgress = Order.builder()
                .status(OrderStatus.IN_PROGRESS)
                .items(Collections.emptyList())
                .build();

        assertThat(inProgress.canCancelByUser()).isFalse();
        assertThatThrownBy(inProgress::cancelByUser)
                .isInstanceOf(IllegalStateException.class);
    }
    
    @Test
    void cancelByUserUsesCanonicalCanceledStatus() {
        // 测试待支付可取消
        Order waitPay = Order.builder()
                .status(OrderStatus.WAIT_PAY)
                .items(Collections.emptyList())
                .build();

        assertThat(waitPay.canCancelByUser()).isTrue();
        waitPay.cancelByUser();
        assertThat(waitPay.getStatus()).isEqualTo(OrderStatus.CANCELED);
        
        // 测试待接单可取消
        Order waitAccept = Order.builder()
                .status(OrderStatus.WAIT_ACCEPT)
                .items(Collections.emptyList())
                .build();

        assertThat(waitAccept.canCancelByUser()).isTrue();
        waitAccept.cancelByUser();
        assertThat(waitAccept.getStatus()).isEqualTo(OrderStatus.CANCELED);
        
        // 测试已接单可取消
        Order accepted = Order.builder()
                .status(OrderStatus.ACCEPTED)
                .items(Collections.emptyList())
                .build();

        assertThat(accepted.canCancelByUser()).isTrue();
        accepted.cancelByUser();
        assertThat(accepted.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void assertEditablePreventsMutationsOutsideDraft() {
        Order draft = Order.builder()
                .status(OrderStatus.DRAFT)
                .items(Collections.emptyList())
                .build();

        draft.assertEditable(); // should not throw

        // 测试 Canonical 状态 WAIT_PAY 不可编辑
        Order waitPay = Order.builder()
                .status(OrderStatus.WAIT_PAY)
                .items(Collections.emptyList())
                .build();

        assertThatThrownBy(waitPay::assertEditable)
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不可编辑");
                
        // 测试旧状态 PENDING_PAYMENT 也不可编辑
        Order pendingPayment = Order.builder()
                .status(OrderStatus.PENDING_PAYMENT)
                .items(Collections.emptyList())
                .build();

        assertThatThrownBy(pendingPayment::assertEditable)
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不可编辑");
    }
}
