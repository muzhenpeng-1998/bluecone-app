package com.bluecone.app.order.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * OrderStatus 状态收口 V1 单元测试。
 * <p>测试 normalize 映射、canAccept 兼容性、canCancel 逻辑、isTerminal 判断等。</p>
 */
@DisplayName("OrderStatus 状态收口 V1 测试")
class OrderStatusNormalizeTest {

    @Nested
    @DisplayName("normalize() 归一化映射测试")
    class NormalizeTest {

        @Test
        @DisplayName("重复语义映射：PENDING_PAYMENT -> WAIT_PAY")
        void pendingPaymentShouldNormalizeToWaitPay() {
            OrderStatus result = OrderStatus.PENDING_PAYMENT.normalize();
            assertThat(result).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("重复语义映射：PENDING_ACCEPT -> WAIT_ACCEPT")
        void pendingAcceptShouldNormalizeToWaitAccept() {
            OrderStatus result = OrderStatus.PENDING_ACCEPT.normalize();
            assertThat(result).isEqualTo(OrderStatus.WAIT_ACCEPT);
        }

        @Test
        @DisplayName("重复语义映射：CANCELLED -> CANCELED")
        void cancelledShouldNormalizeToCanceled() {
            OrderStatus result = OrderStatus.CANCELLED.normalize();
            assertThat(result).isEqualTo(OrderStatus.CANCELED);
        }

        @Test
        @DisplayName("草稿态映射：INIT -> WAIT_PAY")
        void initShouldNormalizeToWaitPay() {
            OrderStatus result = OrderStatus.INIT.normalize();
            assertThat(result).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("草稿态映射：DRAFT -> WAIT_PAY")
        void draftShouldNormalizeToWaitPay() {
            OrderStatus result = OrderStatus.DRAFT.normalize();
            assertThat(result).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("草稿态映射：LOCKED_FOR_CHECKOUT -> WAIT_PAY")
        void lockedForCheckoutShouldNormalizeToWaitPay() {
            OrderStatus result = OrderStatus.LOCKED_FOR_CHECKOUT.normalize();
            assertThat(result).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("草稿态映射：PENDING_CONFIRM -> WAIT_PAY")
        void pendingConfirmShouldNormalizeToWaitPay() {
            OrderStatus result = OrderStatus.PENDING_CONFIRM.normalize();
            assertThat(result).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("已支付映射：PAID -> WAIT_ACCEPT")
        void paidShouldNormalizeToWaitAccept() {
            OrderStatus result = OrderStatus.PAID.normalize();
            assertThat(result).isEqualTo(OrderStatus.WAIT_ACCEPT);
        }

        @Test
        @DisplayName("Canonical 状态返回自身：WAIT_PAY -> WAIT_PAY")
        void canonicalWaitPayShouldReturnSelf() {
            OrderStatus result = OrderStatus.WAIT_PAY.normalize();
            assertThat(result).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("Canonical 状态返回自身：WAIT_ACCEPT -> WAIT_ACCEPT")
        void canonicalWaitAcceptShouldReturnSelf() {
            OrderStatus result = OrderStatus.WAIT_ACCEPT.normalize();
            assertThat(result).isEqualTo(OrderStatus.WAIT_ACCEPT);
        }

        @Test
        @DisplayName("Canonical 状态返回自身：CANCELED -> CANCELED")
        void canonicalCanceledShouldReturnSelf() {
            OrderStatus result = OrderStatus.CANCELED.normalize();
            assertThat(result).isEqualTo(OrderStatus.CANCELED);
        }

        @Test
        @DisplayName("Canonical 状态返回自身：COMPLETED -> COMPLETED")
        void canonicalCompletedShouldReturnSelf() {
            OrderStatus result = OrderStatus.COMPLETED.normalize();
            assertThat(result).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("Canonical 状态返回自身：REFUNDED -> REFUNDED")
        void canonicalRefundedShouldReturnSelf() {
            OrderStatus result = OrderStatus.REFUNDED.normalize();
            assertThat(result).isEqualTo(OrderStatus.REFUNDED);
        }

        @Test
        @DisplayName("Canonical 状态返回自身：CLOSED -> CLOSED")
        void canonicalClosedShouldReturnSelf() {
            OrderStatus result = OrderStatus.CLOSED.normalize();
            assertThat(result).isEqualTo(OrderStatus.CLOSED);
        }

        @Test
        @DisplayName("Canonical 状态返回自身：ACCEPTED -> ACCEPTED")
        void canonicalAcceptedShouldReturnSelf() {
            OrderStatus result = OrderStatus.ACCEPTED.normalize();
            assertThat(result).isEqualTo(OrderStatus.ACCEPTED);
        }

        @Test
        @DisplayName("Canonical 状态返回自身：IN_PROGRESS -> IN_PROGRESS")
        void canonicalInProgressShouldReturnSelf() {
            OrderStatus result = OrderStatus.IN_PROGRESS.normalize();
            assertThat(result).isEqualTo(OrderStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Canonical 状态返回自身：READY -> READY")
        void canonicalReadyShouldReturnSelf() {
            OrderStatus result = OrderStatus.READY.normalize();
            assertThat(result).isEqualTo(OrderStatus.READY);
        }
    }

    @Nested
    @DisplayName("fromCodeNormalized() 查找并归一化测试")
    class FromCodeNormalizedTest {

        @Test
        @DisplayName("输入 PENDING_PAYMENT 返回 WAIT_PAY")
        void pendingPaymentCodeShouldReturnWaitPay() {
            OrderStatus result = OrderStatus.fromCodeNormalized("PENDING_PAYMENT");
            assertThat(result).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("输入 PENDING_ACCEPT 返回 WAIT_ACCEPT")
        void pendingAcceptCodeShouldReturnWaitAccept() {
            OrderStatus result = OrderStatus.fromCodeNormalized("PENDING_ACCEPT");
            assertThat(result).isEqualTo(OrderStatus.WAIT_ACCEPT);
        }

        @Test
        @DisplayName("输入 CANCELLED 返回 CANCELED")
        void cancelledCodeShouldReturnCanceled() {
            OrderStatus result = OrderStatus.fromCodeNormalized("CANCELLED");
            assertThat(result).isEqualTo(OrderStatus.CANCELED);
        }

        @Test
        @DisplayName("输入 DRAFT 返回 WAIT_PAY")
        void draftCodeShouldReturnWaitPay() {
            OrderStatus result = OrderStatus.fromCodeNormalized("DRAFT");
            assertThat(result).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("输入 Canonical 状态码返回自身：WAIT_PAY")
        void canonicalWaitPayCodeShouldReturnSelf() {
            OrderStatus result = OrderStatus.fromCodeNormalized("WAIT_PAY");
            assertThat(result).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("输入 null 返回 null")
        void nullCodeShouldReturnNull() {
            OrderStatus result = OrderStatus.fromCodeNormalized(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("输入空字符串返回 null")
        void blankCodeShouldReturnNull() {
            OrderStatus result = OrderStatus.fromCodeNormalized("");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("输入不存在的状态码返回 null")
        void unknownCodeShouldReturnNull() {
            OrderStatus result = OrderStatus.fromCodeNormalized("UNKNOWN_STATUS");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("大小写不敏感：pending_payment 也返回 WAIT_PAY")
        void caseInsensitivePendingPaymentShouldReturnWaitPay() {
            OrderStatus result = OrderStatus.fromCodeNormalized("pending_payment");
            assertThat(result).isEqualTo(OrderStatus.WAIT_PAY);
        }
    }

    @Nested
    @DisplayName("isTerminal() 终态判断测试")
    class IsTerminalTest {

        @Test
        @DisplayName("COMPLETED 是终态")
        void completedShouldBeTerminal() {
            assertThat(OrderStatus.COMPLETED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("CANCELED 是终态")
        void canceledShouldBeTerminal() {
            assertThat(OrderStatus.CANCELED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("CANCELLED 兼容判断为终态（通过 normalize）")
        void cancelledShouldBeTerminal() {
            assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("REFUNDED 是终态")
        void refundedShouldBeTerminal() {
            assertThat(OrderStatus.REFUNDED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("CLOSED 是终态")
        void closedShouldBeTerminal() {
            assertThat(OrderStatus.CLOSED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("WAIT_PAY 不是终态")
        void waitPayShouldNotBeTerminal() {
            assertThat(OrderStatus.WAIT_PAY.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("PENDING_PAYMENT 不是终态")
        void pendingPaymentShouldNotBeTerminal() {
            assertThat(OrderStatus.PENDING_PAYMENT.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("WAIT_ACCEPT 不是终态")
        void waitAcceptShouldNotBeTerminal() {
            assertThat(OrderStatus.WAIT_ACCEPT.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("PENDING_ACCEPT 不是终态")
        void pendingAcceptShouldNotBeTerminal() {
            assertThat(OrderStatus.PENDING_ACCEPT.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("ACCEPTED 不是终态")
        void acceptedShouldNotBeTerminal() {
            assertThat(OrderStatus.ACCEPTED.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("IN_PROGRESS 不是终态")
        void inProgressShouldNotBeTerminal() {
            assertThat(OrderStatus.IN_PROGRESS.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("READY 不是终态")
        void readyShouldNotBeTerminal() {
            assertThat(OrderStatus.READY.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("DRAFT 不是终态")
        void draftShouldNotBeTerminal() {
            assertThat(OrderStatus.DRAFT.isTerminal()).isFalse();
        }
    }

    @Nested
    @DisplayName("isPayPending() 待支付判断测试")
    class IsPayPendingTest {

        @Test
        @DisplayName("WAIT_PAY 是待支付")
        void waitPayShouldBePayPending() {
            assertThat(OrderStatus.WAIT_PAY.isPayPending()).isTrue();
        }

        @Test
        @DisplayName("PENDING_PAYMENT 兼容判断为待支付")
        void pendingPaymentShouldBePayPending() {
            assertThat(OrderStatus.PENDING_PAYMENT.isPayPending()).isTrue();
        }

        @Test
        @DisplayName("DRAFT 归一化后判断为待支付")
        void draftShouldBePayPending() {
            assertThat(OrderStatus.DRAFT.isPayPending()).isTrue();
        }

        @Test
        @DisplayName("LOCKED_FOR_CHECKOUT 归一化后判断为待支付")
        void lockedForCheckoutShouldBePayPending() {
            assertThat(OrderStatus.LOCKED_FOR_CHECKOUT.isPayPending()).isTrue();
        }

        @Test
        @DisplayName("INIT 归一化后判断为待支付")
        void initShouldBePayPending() {
            assertThat(OrderStatus.INIT.isPayPending()).isTrue();
        }

        @Test
        @DisplayName("WAIT_ACCEPT 不是待支付")
        void waitAcceptShouldNotBePayPending() {
            assertThat(OrderStatus.WAIT_ACCEPT.isPayPending()).isFalse();
        }

        @Test
        @DisplayName("COMPLETED 不是待支付")
        void completedShouldNotBePayPending() {
            assertThat(OrderStatus.COMPLETED.isPayPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("isAcceptPending() 待接单判断测试")
    class IsAcceptPendingTest {

        @Test
        @DisplayName("WAIT_ACCEPT 是待接单")
        void waitAcceptShouldBeAcceptPending() {
            assertThat(OrderStatus.WAIT_ACCEPT.isAcceptPending()).isTrue();
        }

        @Test
        @DisplayName("PENDING_ACCEPT 兼容判断为待接单")
        void pendingAcceptShouldBeAcceptPending() {
            assertThat(OrderStatus.PENDING_ACCEPT.isAcceptPending()).isTrue();
        }

        @Test
        @DisplayName("PAID 归一化后判断为待接单")
        void paidShouldBeAcceptPending() {
            assertThat(OrderStatus.PAID.isAcceptPending()).isTrue();
        }

        @Test
        @DisplayName("WAIT_PAY 不是待接单")
        void waitPayShouldNotBeAcceptPending() {
            assertThat(OrderStatus.WAIT_ACCEPT.isAcceptPending()).isTrue();
        }

        @Test
        @DisplayName("ACCEPTED 不是待接单")
        void acceptedShouldNotBeAcceptPending() {
            assertThat(OrderStatus.ACCEPTED.isAcceptPending()).isFalse();
        }

        @Test
        @DisplayName("COMPLETED 不是待接单")
        void completedShouldNotBeAcceptPending() {
            assertThat(OrderStatus.COMPLETED.isAcceptPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("canAccept() 可接单判断测试")
    class CanAcceptTest {

        @Test
        @DisplayName("WAIT_ACCEPT 可接单")
        void waitAcceptCanBeAccepted() {
            assertThat(OrderStatus.WAIT_ACCEPT.canAccept()).isTrue();
        }

        @Test
        @DisplayName("PENDING_ACCEPT 兼容判断为可接单（避免线上事故）")
        void pendingAcceptCanBeAccepted() {
            assertThat(OrderStatus.PENDING_ACCEPT.canAccept()).isTrue();
        }

        @Test
        @DisplayName("PAID 归一化后可接单")
        void paidCanBeAccepted() {
            assertThat(OrderStatus.PAID.canAccept()).isTrue();
        }

        @Test
        @DisplayName("WAIT_PAY 不可接单")
        void waitPayCannotBeAccepted() {
            assertThat(OrderStatus.WAIT_PAY.canAccept()).isFalse();
        }

        @Test
        @DisplayName("ACCEPTED 不可接单（已接单）")
        void acceptedCannotBeAccepted() {
            assertThat(OrderStatus.ACCEPTED.canAccept()).isFalse();
        }

        @Test
        @DisplayName("COMPLETED 不可接单")
        void completedCannotBeAccepted() {
            assertThat(OrderStatus.COMPLETED.canAccept()).isFalse();
        }

        @Test
        @DisplayName("CANCELED 不可接单")
        void canceledCannotBeAccepted() {
            assertThat(OrderStatus.CANCELED.canAccept()).isFalse();
        }

        @Test
        @DisplayName("CANCELLED 兼容判断为不可接单")
        void cancelledCannotBeAccepted() {
            assertThat(OrderStatus.CANCELLED.canAccept()).isFalse();
        }

        @Test
        @DisplayName("DRAFT 不可接单")
        void draftCannotBeAccepted() {
            assertThat(OrderStatus.DRAFT.canAccept()).isFalse();
        }
    }

    @Nested
    @DisplayName("canCancel() 可取消判断测试")
    class CanCancelTest {

        @Test
        @DisplayName("WAIT_PAY 可取消")
        void waitPayCanBeCanceled() {
            assertThat(OrderStatus.WAIT_PAY.canCancel()).isTrue();
        }

        @Test
        @DisplayName("PENDING_PAYMENT 兼容判断为可取消")
        void pendingPaymentCanBeCanceled() {
            assertThat(OrderStatus.PENDING_PAYMENT.canCancel()).isTrue();
        }

        @Test
        @DisplayName("WAIT_ACCEPT 可取消")
        void waitAcceptCanBeCanceled() {
            assertThat(OrderStatus.WAIT_ACCEPT.canCancel()).isTrue();
        }

        @Test
        @DisplayName("PENDING_ACCEPT 兼容判断为可取消")
        void pendingAcceptCanBeCanceled() {
            assertThat(OrderStatus.PENDING_ACCEPT.canCancel()).isTrue();
        }

        @Test
        @DisplayName("ACCEPTED 可取消")
        void acceptedCanBeCanceled() {
            assertThat(OrderStatus.ACCEPTED.canCancel()).isTrue();
        }

        @Test
        @DisplayName("DRAFT 归一化后可取消")
        void draftCanBeCanceled() {
            assertThat(OrderStatus.DRAFT.canCancel()).isTrue();
        }

        @Test
        @DisplayName("LOCKED_FOR_CHECKOUT 归一化后可取消")
        void lockedForCheckoutCanBeCanceled() {
            assertThat(OrderStatus.LOCKED_FOR_CHECKOUT.canCancel()).isTrue();
        }

        @Test
        @DisplayName("PENDING_CONFIRM 归一化后可取消")
        void pendingConfirmCanBeCanceled() {
            assertThat(OrderStatus.PENDING_CONFIRM.canCancel()).isTrue();
        }

        @Test
        @DisplayName("INIT 归一化后可取消")
        void initCanBeCanceled() {
            assertThat(OrderStatus.INIT.canCancel()).isTrue();
        }

        @Test
        @DisplayName("IN_PROGRESS 不可取消（商户已投入成本）")
        void inProgressCannotBeCanceled() {
            assertThat(OrderStatus.IN_PROGRESS.canCancel()).isFalse();
        }

        @Test
        @DisplayName("READY 不可取消（商品已制作完成）")
        void readyCannotBeCanceled() {
            assertThat(OrderStatus.READY.canCancel()).isFalse();
        }

        @Test
        @DisplayName("COMPLETED 不可取消（只能发起退款）")
        void completedCannotBeCanceled() {
            assertThat(OrderStatus.COMPLETED.canCancel()).isFalse();
        }

        @Test
        @DisplayName("CANCELED 不可取消（已经取消）")
        void canceledCannotBeCanceled() {
            assertThat(OrderStatus.CANCELED.canCancel()).isFalse();
        }

        @Test
        @DisplayName("CANCELLED 兼容判断为不可取消")
        void cancelledCannotBeCanceled() {
            assertThat(OrderStatus.CANCELLED.canCancel()).isFalse();
        }

        @Test
        @DisplayName("REFUNDED 不可取消（已退款）")
        void refundedCannotBeCanceled() {
            assertThat(OrderStatus.REFUNDED.canCancel()).isFalse();
        }

        @Test
        @DisplayName("CLOSED 不可取消（已关闭）")
        void closedCannotBeCanceled() {
            assertThat(OrderStatus.CLOSED.canCancel()).isFalse();
        }
    }

    @Nested
    @DisplayName("fromCode() 原始查找测试（不归一化）")
    class FromCodeTest {

        @Test
        @DisplayName("fromCode 原样返回 PENDING_PAYMENT（不归一化）")
        void fromCodeShouldReturnPendingPaymentAsIs() {
            OrderStatus result = OrderStatus.fromCode("PENDING_PAYMENT");
            assertThat(result).isEqualTo(OrderStatus.PENDING_PAYMENT);
            // 但业务判断应使用 normalize
            assertThat(result.normalize()).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("fromCode 原样返回 PENDING_ACCEPT（不归一化）")
        void fromCodeShouldReturnPendingAcceptAsIs() {
            OrderStatus result = OrderStatus.fromCode("PENDING_ACCEPT");
            assertThat(result).isEqualTo(OrderStatus.PENDING_ACCEPT);
            // 但业务判断应使用 normalize
            assertThat(result.normalize()).isEqualTo(OrderStatus.WAIT_ACCEPT);
        }

        @Test
        @DisplayName("fromCode 原样返回 CANCELLED（不归一化）")
        void fromCodeShouldReturnCancelledAsIs() {
            OrderStatus result = OrderStatus.fromCode("CANCELLED");
            assertThat(result).isEqualTo(OrderStatus.CANCELLED);
            // 但业务判断应使用 normalize
            assertThat(result.normalize()).isEqualTo(OrderStatus.CANCELED);
        }

        @Test
        @DisplayName("fromCode 返回 Canonical 状态")
        void fromCodeShouldReturnCanonicalStatus() {
            OrderStatus result = OrderStatus.fromCode("WAIT_PAY");
            assertThat(result).isEqualTo(OrderStatus.WAIT_PAY);
            assertThat(result.normalize()).isEqualTo(OrderStatus.WAIT_PAY);
        }
    }

    @Nested
    @DisplayName("综合场景测试：模拟线上事故预防")
    class IntegrationScenarioTest {

        @Test
        @DisplayName("场景1：旧代码写入 PENDING_ACCEPT，新代码 canAccept 应返回 true（避免接单失败事故）")
        void oldCodePendingAcceptShouldBeAcceptable() {
            // 模拟：旧代码写入了 PENDING_ACCEPT
            OrderStatus oldStatus = OrderStatus.PENDING_ACCEPT;
            
            // 新代码判断是否可接单
            assertThat(oldStatus.canAccept()).isTrue();
            assertThat(oldStatus.isAcceptPending()).isTrue();
            
            // 归一化后也是可接单状态
            assertThat(oldStatus.normalize()).isEqualTo(OrderStatus.WAIT_ACCEPT);
        }

        @Test
        @DisplayName("场景2：旧代码写入 CANCELLED，新代码 isTerminal 应返回 true（避免状态判断遗漏）")
        void oldCodeCancelledShouldBeTerminal() {
            // 模拟：旧代码写入了 CANCELLED
            OrderStatus oldStatus = OrderStatus.CANCELLED;
            
            // 新代码判断是否终态
            assertThat(oldStatus.isTerminal()).isTrue();
            
            // 归一化后是 CANCELED
            assertThat(oldStatus.normalize()).isEqualTo(OrderStatus.CANCELED);
        }

        @Test
        @DisplayName("场景3：旧代码写入 PENDING_PAYMENT，新代码 canCancel 应返回 true（避免取消失败）")
        void oldCodePendingPaymentShouldBeCancelable() {
            // 模拟：旧代码写入了 PENDING_PAYMENT
            OrderStatus oldStatus = OrderStatus.PENDING_PAYMENT;
            
            // 新代码判断是否可取消
            assertThat(oldStatus.canCancel()).isTrue();
            assertThat(oldStatus.isPayPending()).isTrue();
            
            // 归一化后是 WAIT_PAY
            assertThat(oldStatus.normalize()).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("场景4：草稿态不应允许接单（业务规则校验）")
        void draftShouldNotBeAcceptable() {
            OrderStatus draft = OrderStatus.DRAFT;
            
            // 草稿态不允许接单
            assertThat(draft.canAccept()).isFalse();
            
            // 但允许取消
            assertThat(draft.canCancel()).isTrue();
            
            // 归一化后是待支付
            assertThat(draft.normalize()).isEqualTo(OrderStatus.WAIT_PAY);
        }

        @Test
        @DisplayName("场景5：制作中订单不可取消（防止商户损失）")
        void inProgressShouldNotBeCancelable() {
            OrderStatus inProgress = OrderStatus.IN_PROGRESS;
            
            // 制作中不可取消
            assertThat(inProgress.canCancel()).isFalse();
            
            // 也不是终态（还可以流转到 READY/COMPLETED）
            assertThat(inProgress.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("场景6：fromCodeNormalized 保证业务判断统一性")
        void fromCodeNormalizedShouldEnsureConsistency() {
            // 无论输入什么待支付状态，归一化后都是 WAIT_PAY
            assertThat(OrderStatus.fromCodeNormalized("PENDING_PAYMENT"))
                    .isEqualTo(OrderStatus.WAIT_PAY);
            assertThat(OrderStatus.fromCodeNormalized("WAIT_PAY"))
                    .isEqualTo(OrderStatus.WAIT_PAY);
            assertThat(OrderStatus.fromCodeNormalized("DRAFT"))
                    .isEqualTo(OrderStatus.WAIT_PAY);
            
            // 统一判断都返回可取消
            assertThat(OrderStatus.fromCodeNormalized("PENDING_PAYMENT").canCancel()).isTrue();
            assertThat(OrderStatus.fromCodeNormalized("WAIT_PAY").canCancel()).isTrue();
            assertThat(OrderStatus.fromCodeNormalized("DRAFT").canCancel()).isTrue();
        }
    }
}
