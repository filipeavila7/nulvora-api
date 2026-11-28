package com.nulvora.nulvora.promotions.entity;

import com.nulvora.nulvora.common.entity.BaseEntity;
import com.nulvora.nulvora.promotionProduct.entity.PromotionProduct;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Promotion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PromotionDiscountType discountType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    private boolean active = true;

    @OneToMany(mappedBy = "promotion", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<PromotionProduct> promotionProducts = new ArrayList<>();






}
