package com.nulvora.nulvora.promotionProduct.service;

import com.nulvora.nulvora.category.repository.CategoryRepository;
import com.nulvora.nulvora.helper.GlobalHelperService;
import com.nulvora.nulvora.product.entity.Product;
import com.nulvora.nulvora.product.repository.ProductRepository;
import com.nulvora.nulvora.promotionProduct.dto.response.PromotionProductDetailsResponse;
import com.nulvora.nulvora.promotionProduct.dto.response.PromotionProductResponse;
import com.nulvora.nulvora.promotionProduct.entity.PromotionProduct;
import com.nulvora.nulvora.promotionProduct.mapper.PromotionProductMapper;
import com.nulvora.nulvora.promotionProduct.repository.PromotionProductRepository;
import com.nulvora.nulvora.promotions.entity.Promotion;
import com.nulvora.nulvora.promotions.repository.PromotionRepository;
import com.nulvora.nulvora.promotions.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromotionProductService {
    private final PromotionProductRepository promotionProductRepository;
    private final PromotionProductMapper promotionProductMapper;
    private final PromotionService promotionService;
    private final GlobalHelperService globalHelperService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PromotionRepository promotionRepository;


    // retorna todos os produtos relacionado à uma promoção
    public Page<PromotionProductDetailsResponse> getPromotionProducts(
            Long promotionId,
            Pageable pageable
    ) {

        if (!promotionRepository.existsById(promotionId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Promoção não encontrada"
            );
        }

        return promotionProductRepository
                .findAllByPromotionId(promotionId, pageable)
                .map(promotionProductMapper::toPromotionProductDetailsResponse);
    }

    // adicionar uma promoção a um produto ou substituir promoção ja existente
    public PromotionProductResponse addPromotionToProduct(Long productId, Long promotionId){
        // pega o produto
        Product product = globalHelperService.getActiveProductOrThrow(productId);

        // pega a promoção e ja valida ela
        Promotion promotion = promotionService.validatePromotion(promotionId);

        // procura relação, se não achar, cria uma nova
        PromotionProduct association = promotionProductRepository
                .findByProduct(product)
                .orElseGet(() -> {
                    PromotionProduct newAssociation = new PromotionProduct();
                    newAssociation.setProduct(product);
                    return newAssociation;
                });

        // substitui ou cria nova relação com promoção
        association.setPromotion(promotion);

       return promotionProductMapper.toPromotionProductResponse(promotionProductRepository.save(association));
    }

    // remover promoção do produto
    public void removePromotionFromProduct(Long productId) {
        // pega o produto
        Product product = globalHelperService.getActiveProductOrThrow(productId);

        // acha a relação
        PromotionProduct promotionProduct = promotionProductRepository.findByProduct(product)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Promoção do produto não encontrada"));

        // apaga
        promotionProductRepository.delete(promotionProduct);
    }


    // remover promoções por categoria
    @Transactional
    public void removeAllPromotionsFromCategory(Long categoryId){
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Categoria não encontrada"
            );
        }

        promotionProductRepository.deleteAllByProductCategoryId(categoryId);
    }

    // remover uma promoção específica de uma categoria
    public void removePromotionFromCategory(Long categoryId, Long promotionId){
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Categoria não encontrada"
            );
        }

        if (!promotionRepository.existsById(promotionId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Promoção não encontrada"
            );
        }

        promotionProductRepository.deleteAllByProductCategoryIdAndPromotionId(categoryId, promotionId);
    }


    // adicionar promoções por categorias
    @Transactional
    public void addPromotionToCategory(Long categoryId, Long promotionId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Categoria não encontrada"
            );
        }

        // acha todos os produtos que tem relação com àquela categoria
        List<Product> products = productRepository.findAllByCategoryId(categoryId);

        if (products.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Nenhum produto encontrado nessa categoria"
            );
        }

        // percorre todos os produtos daquela categoria
        for (Product product : products) {

            // procura relação
            Optional<PromotionProduct> association =
                    promotionProductRepository.findByProduct(product);

            // caso tenha relação, volta pro for
            if (association.isPresent()) {
                continue;
            }

            // adciona promoções so em produtos que não possuiam promoções antes
            addPromotionToProduct(product.getId(), promotionId);
        }
    }

    public boolean hasPromotion(Product product){
        return promotionProductRepository.existsByProductId(product.getId());
    }

    // validar promoção atual
    private void validateCurrentPromotion(Promotion currentPromotion) {

        if (currentPromotion == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        boolean isValid = currentPromotion.isActive()
                && !now.isBefore(currentPromotion.getStartAt())
                && now.isBefore(currentPromotion.getExpireAt());

        if (isValid) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esse produto já possui uma promoção vigente"
            );
        }
    }
}
