package com.nulvora.nulvora.promotions.service;

import com.nulvora.nulvora.common.utils.DateUtils;
import com.nulvora.nulvora.coupon.entity.DiscountType;
import com.nulvora.nulvora.product.entity.Product;
import com.nulvora.nulvora.productVariant.entity.ProductVariant;
import com.nulvora.nulvora.promotions.dto.request.PromotionRequest;
import com.nulvora.nulvora.promotions.dto.request.PromotionUpdateRequest;
import com.nulvora.nulvora.promotions.dto.response.PromotionDetailsResponse;
import com.nulvora.nulvora.promotions.dto.response.PromotionResponse;
import com.nulvora.nulvora.promotions.entity.Promotion;
import com.nulvora.nulvora.promotions.mapper.PromotionMapper;
import com.nulvora.nulvora.promotions.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;

    // TODO - validar valores quando for aplicar promoções
    // TODO - (o valor nunca pode ficar negativo ou 0, impedir editar o valor de variantes quando o resultado ficar
    //  menor ou 0 quando aplicar a promoção se o tipo for FIXED_AMOUNT.)


    // get de todas as promoções
    public Page<PromotionResponse> getAllPromotions(Pageable pageable ){
        return promotionRepository.findAll(pageable).map(promotionMapper::toPromotionResponse);
    }

    // get de detalhes de uma promoção
    public PromotionDetailsResponse getPromotionDetails(Long promotionId){
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Promoção não encontrada"));

        return  promotionMapper.toPromotionDetailsResponse(promotion);
    }



    // criar promoções
    public PromotionResponse createPromotion(PromotionRequest request){
        Promotion promotion = promotionMapper.toEntity(request);

        // se o desconto for tipo porcentagem, evita ser 100%
        if (request.discountType() == DiscountType.PERCENTAGE
                && request.discountValue().compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"O desconto percentual deve ser inferior a 100%");
        }

        return promotionMapper.toPromotionResponse(promotionRepository.save(promotion));
    }


    // editar promoções
    @Transactional
    public PromotionResponse updatePromotion(PromotionUpdateRequest request, Long promotionId){
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Promoção não encontrada"));

        // atualiza somente os campos não nulos
        promotionMapper.updateEntity(request, promotion);

        // verifica se a data de expiração é posterior à data de início
        if (!promotion.getExpireAt().isAfter(promotion.getStartAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data de expiração deve ser posterior à data de início"
            );
        }

        return promotionMapper.toPromotionResponse(
                promotionRepository.save(promotion)
        );

    }

    // validar promoção
    public Promotion validatePromotion(Long promotionId){
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Promoção não encontrada"));

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(promotion.getStartAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Essa promoção ainda não está disponível, iniciará em " +
                            DateUtils.formatDateTime(promotion.getStartAt()));
        }

        if (!now.isBefore(promotion.getExpireAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Essa promoção expirou em " + DateUtils.formatDateTime(promotion.getExpireAt()));
        }

        if (!promotion.isActive()){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Essa promoção não está ativa");
        }

        return promotion;
    }


    // validar promoção com boleano
    public boolean isValidPromotion(Long promotionId){
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Promoção não encontrada"));

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(promotion.getStartAt())) {
            return false;
        }

        if (!now.isBefore(promotion.getExpireAt())) {
            return false;
        }

        if (!promotion.isActive()){
            return false;
        }

        return true;
    }

    // calcular o disconto da promoção
    public BigDecimal calculatePromotionDiscount(BigDecimal baseValue, BigDecimal discountValue){
        return baseValue.multiply(discountValue).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

    }




}
