package com.nulvora.nulvora.orderItens.entity;

import com.nulvora.nulvora.order.entity.Order;
import com.nulvora.nulvora.productVariant.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_variant", // não permite a mesma variante duas vezes na order
                        columnNames = {"order_id", "product_variant_id"}
                )
        })
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalprice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal finalPrice;

    // desconto total
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDiscount;

    // quantity x originalPrice
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalSubtotal;

    // quantity x finalPrice
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;


}
