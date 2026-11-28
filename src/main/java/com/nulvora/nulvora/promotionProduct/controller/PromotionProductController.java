package com.nulvora.nulvora.promotionProduct.controller;


import com.nulvora.nulvora.promotionProduct.dto.response.PromotionProductDetailsResponse;
import com.nulvora.nulvora.promotionProduct.dto.response.PromotionProductResponse;
import com.nulvora.nulvora.promotionProduct.service.PromotionProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(("/api/promotion-product"))
public class PromotionProductController {

    private final PromotionProductService promotionProductService;

    // listar todos os produtos relacionados a uma promoção
    @GetMapping("/{promotionId}/products")
    public ResponseEntity<Page<PromotionProductDetailsResponse>> getPromotionProducts(
            @PathVariable Long promotionId,
            @PageableDefault(size = 12, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                promotionProductService.getPromotionProducts(promotionId, pageable)
        );
    }

    // adicionar uma promoção aos produtos de uma categoria
    @PostMapping("/categories/{categoryId}/promotions/{promotionId}")
    public ResponseEntity<Void> addPromotionToCategory(
            @PathVariable Long categoryId,
            @PathVariable Long promotionId
    ) {
        promotionProductService.addPromotionToCategory(categoryId, promotionId);
        return ResponseEntity.noContent().build();
    }

    // adcionar uma promoção ou substituir
    @PostMapping("/products/{productId}/promotion/{promotionId}")
    public ResponseEntity<PromotionProductResponse> addPromotionToProduct(
            @PathVariable Long productId,
            @PathVariable Long promotionId
    ) {
        return ResponseEntity.ok(
                promotionProductService.addPromotionToProduct(productId, promotionId)
        );
    }

    // remover a promoção de um produto
    @DeleteMapping("/products/{productId}/promotion")
    public ResponseEntity<Void> removePromotionFromProduct(
            @PathVariable Long productId
    ) {
        promotionProductService.removePromotionFromProduct(productId);
        return ResponseEntity.noContent().build();
    }

    // remover todas as promoções dos produtos de uma categoria
    @DeleteMapping("/categories/{categoryId}/promotions")
    public ResponseEntity<Void> removeAllPromotionsFromCategory(
            @PathVariable Long categoryId
    ) {
        promotionProductService.removeAllPromotionsFromCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    // DELETE para remover uma promoção específica dos produtos de uma categoria
    @DeleteMapping("/categories/{categoryId}/promotions/{promotionId}")
    public ResponseEntity<Void> removePromotionFromCategory(
            @PathVariable Long categoryId,
            @PathVariable Long promotionId
    ) {
        promotionProductService.removePromotionFromCategory(categoryId, promotionId);
        return ResponseEntity.noContent().build();
    }


}
