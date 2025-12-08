package com.bluecone.app.core.id;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TypedIdTest {

    @Test
    void equalsAndHashCodeRespectTypeAndValue() {
        TypedId orderId = TypedId.of(IdType.ORDER, "ord_123");
        TypedId sameOrderId = TypedId.of(IdType.ORDER, "ord_123");
        TypedId userId = TypedId.of(IdType.USER, "ord_123");
        TypedId differentValue = TypedId.of(IdType.ORDER, "ord_999");

        assertThat(orderId)
                .isEqualTo(sameOrderId)
                .hasSameHashCodeAs(sameOrderId);
        assertThat(orderId)
                .isNotEqualTo(userId)
                .isNotEqualTo(differentValue);
    }

    @Test
    void toStringReturnsRawValue() {
        TypedId typedId = TypedId.of(IdType.PAYMENT, IdType.PAYMENT.apply("01F8MECHJAA"));

        assertThat(typedId.toString()).isEqualTo("pay_01F8MECHJAA");
    }
}
