package com.nulvora.nulvora.orderItens.service;

import com.nulvora.nulvora.order.entity.Order;
import com.nulvora.nulvora.orderItens.dto.request.OrderItemRequest;
import com.nulvora.nulvora.orderItens.entity.OrderItem;
import com.nulvora.nulvora.orderItens.repository.OrderItemRepository;
import com.nulvora.nulvora.productVariant.entity.ProductVariant;
import com.nulvora.nulvora.productVariant.repository.ProductVariantRepository;
import com.nulvora.nulvora.promotionProduct.entity.PromotionProduct;
import com.nulvora.nulvora.promotionProduct.repository.PromotionProductRepository;
import com.nulvora.nulvora.promotionProduct.service.PromotionProductService;
import com.nulvora.nulvora.promotions.entity.Promotion;
import com.nulvora.nulvora.promotions.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderItemService {
    private final ProductVariantRepository productVariantRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final PromotionProductService promotionProductService;
    private final PromotionService promotionService;

    // criar ordemItem
    public OrderItem createOrderItem(Order order, OrderItemRequest item) {

        ProductVariant productVariant = productVariantRepository
                .findByIdAndActiveTrue(item.productVariantId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Variante não encontrada"
                ));

        OrderItem orderItem = new OrderItem();

        BigDecimal originalPrice = productVariant.getPrice();
        BigDecimal finalPrice = originalPrice;
        BigDecimal discount = BigDecimal.ZERO;

        orderItem.setOrder(order);
        orderItem.setProductVariant(productVariant);
        orderItem.setQuantity(item.quantity());
        orderItem.setOriginalprice(originalPrice);

        if (promotionProductService.hasPromotion(productVariant.getProduct())) {

            PromotionProduct promotionProduct = promotionProductRepository
                    .findByProduct(productVariant.getProduct())
                    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Produto não encontrado"));

            Promotion promotion = promotionProduct.getPromotion();

            if (promotionService.isValidPromotion(promotion.getId())) {

                discount = promotionService.calculatePromotionDiscount(
                        originalPrice,
                        promotion.getDiscountValue()
                );

                finalPrice = originalPrice.subtract(discount);
            }
        }

        // Valores unitários
        orderItem.setFinalPrice(finalPrice);

        // Desconto total considerando a quantidade
        orderItem.setTotalDiscount(
                discount.multiply(BigDecimal.valueOf(item.quantity()))
        );

        // subtotal original
        orderItem.setOriginalSubtotal(
                originalPrice.multiply(BigDecimal.valueOf(item.quantity()))
        );

        // Subtotal considerando o preço promocional
        orderItem.setSubtotal(
                finalPrice.multiply(BigDecimal.valueOf(item.quantity()))
        );

        order.getOrderItems().add(orderItem);

        return orderItem;
    }
}
