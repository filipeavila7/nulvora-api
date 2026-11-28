package com.nulvora.nulvora.userCoupon.service;

import com.nulvora.nulvora.common.utils.DateUtils;
import com.nulvora.nulvora.coupon.entity.Coupon;
import com.nulvora.nulvora.coupon.repository.CouponRepository;
import com.nulvora.nulvora.helper.GlobalHelperService;
import com.nulvora.nulvora.user.entity.User;
import com.nulvora.nulvora.userCoupon.dto.response.UserCouponResponse;
import com.nulvora.nulvora.userCoupon.entity.UserCoupon;
import com.nulvora.nulvora.userCoupon.mapper.UserCouponMapper;
import com.nulvora.nulvora.userCoupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserCouponService {
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final GlobalHelperService globalHelperService;
    private final UserCouponMapper userCouponMapper;



    // usuario resgatar cupom
    public UserCouponResponse createUserCoupon(Long couponId){
        User loggedUser = globalHelperService.getLoggedUser();

        // valida cupom
        Coupon coupon = validateUserCoupon(couponId, loggedUser.getId());

        // cria relação
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUsageLimit(coupon.getMaxUsagePerUser());
        userCoupon.setUsageCount(0);
        userCoupon.setUser(loggedUser);
        userCoupon.setCoupon(coupon);
        userCoupon.setAssignedAt(LocalDateTime.now());

        return userCouponMapper.toResponse(userCouponRepository.save(userCoupon));
    }


    // validação de cupom
    public UserCoupon validateUseCoupon(Long couponId){
        LocalDateTime now = LocalDateTime.now();

        // busca o cupom
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cupom não encontrado"));

        // verifica se o cupom tem relação com o usuario
        UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(globalHelperService.getLoggedUser().getId(), couponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Você não tem esse cupom"));

        // verifica se o cupom esta ativo
        if (!coupon.isActivate()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Esse cupom não está ativo");
        }

        if (now.isBefore(coupon.getStartAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse cupom ainda não está disponível, iniciará em " +
                            DateUtils.formatDateTime(coupon.getStartAt()));
        }

        if (!now.isBefore(coupon.getExpireAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse cupom expirou em " + DateUtils.formatDateTime(coupon.getExpireAt()));
        }

        return userCoupon;
    }


    // validação completa do cupom com usuario
    public Coupon validateUserCoupon(Long couponId, Long userId){
        LocalDateTime now = LocalDateTime.now();

        // busca o cupom
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cupom não encontrado"));

        // verifica se o usuario ja resgatou
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você ja resgatou esse cupom");
        }

        // verifica se o cupom esta ativo
        if (!coupon.isActivate()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Esse cupom não está ativo");
        }

        if (now.isBefore(coupon.getStartAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse cupom ainda não está disponível, iniciará em " +
                            DateUtils.formatDateTime(coupon.getStartAt()));
        }

        if (!now.isBefore(coupon.getExpireAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse cupom expirou em " + DateUtils.formatDateTime(coupon.getExpireAt()));
        }

        return coupon;
    }


    @Transactional
    public void registerUsage(Long userId, Long couponId) {

        UserCoupon userCoupon = userCouponRepository
                .findByUserIdAndCouponId(userId, couponId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Relação entre usuário e cupom não encontrada"
                ));

        userCoupon.setUsageCount(userCoupon.getUsageCount() + 1);
    }


}
