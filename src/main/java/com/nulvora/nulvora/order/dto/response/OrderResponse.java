package com.nulvora.nulvora.order.dto.response;

import com.nulvora.nulvora.coupon.dto.response.CouponResponse;
import com.nulvora.nulvora.order.entity.OrderStatus;
import com.nulvora.nulvora.orderItens.dto.response.OrderItemResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,

        BigDecimal originalSubtotal,
        BigDecimal promotionDiscount,
        BigDecimal couponDiscount,

        BigDecimal subTotal,
        BigDecimal shippingCost,
        BigDecimal total,

        LocalDateTime createdAt,
        List<OrderItemResponse> orderItems,
        CouponResponse coupon

) {
}