package com.nulvora.nulvora.orderItens.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productVariantId,
        String productName,
        String variantName,
        String sku,
        String imageUrl,
        Integer quantity,

        BigDecimal originalPrice,
        BigDecimal finalPrice,

        BigDecimal originalSubtotal,
        BigDecimal subtotal,
        BigDecimal totalDiscount
) {}