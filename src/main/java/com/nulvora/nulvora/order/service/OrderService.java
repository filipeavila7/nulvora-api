
package com.nulvora.nulvora.order.service;

import com.nulvora.nulvora.api.stripe.dto.response.StripeCheckoutResponse;
import com.nulvora.nulvora.coupon.entity.Coupon;
import com.nulvora.nulvora.coupon.repository.CouponRepository;
import com.nulvora.nulvora.coupon.service.CouponService;
import com.nulvora.nulvora.helper.GlobalHelperService;
import com.nulvora.nulvora.order.dto.request.OrderRequest;
import com.nulvora.nulvora.order.dto.response.OrderCheckoutResponse;
import com.nulvora.nulvora.order.entity.Order;
import com.nulvora.nulvora.order.entity.OrderStatus;
import com.nulvora.nulvora.order.mapper.OrderMapper;
import com.nulvora.nulvora.order.repository.OrderRepository;
import com.nulvora.nulvora.orderItens.entity.OrderItem;
import com.nulvora.nulvora.orderItens.service.OrderItemService;
import com.nulvora.nulvora.payment.entity.Payment;
import com.nulvora.nulvora.payment.service.PaymentService;
import com.nulvora.nulvora.user.entity.User;
import com.nulvora.nulvora.userCoupon.entity.UserCoupon;
import com.nulvora.nulvora.userCoupon.service.UserCouponService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final GlobalHelperService globalHelperService;
    private final OrderItemService orderItemService;
    private final OrderMapper orderMapper;
    private final CouponRepository couponRepository;
    private final UserCouponService userCouponService;
    private final PaymentService paymentService;

    // TODO - fazer integração com API de cálculo de frete

    @Transactional(rollbackFor = StripeException.class)
    public OrderCheckoutResponse createOrder(OrderRequest request) throws StripeException {

        User loggedUser = globalHelperService.getLoggedUser();

        // Cria o pedido
        Order order = new Order();
        order.setUser(loggedUser);
        order.setStatus(OrderStatus.PENDING);

        // Valor padrão de frete por enquanto
        order.setShippingCost(BigDecimal.TEN);

        // Inicializa os valores financeiros
        BigDecimal originalSubtotal = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal promotionDiscount = BigDecimal.ZERO;

        // Cria os itens e consolida seus valores
        for (int i = 0; i < request.items().size(); i++) {

            OrderItem orderItem = orderItemService.createOrderItem(
                    order,
                    request.items().get(i)
            );

            originalSubtotal = originalSubtotal.add(
                    orderItem.getOriginalSubtotal()
            );

            subtotal = subtotal.add(
                    orderItem.getSubtotal()
            );

            promotionDiscount = promotionDiscount.add(
                    orderItem.getTotalDiscount()
            );
        }

        // Registra os valores consolidados dos itens
        order.setOriginalSubtotal(originalSubtotal);
        order.setPromotionDiscount(promotionDiscount);
        order.setSubtotal(subtotal);

        // Inicializa o desconto do cupom
        order.setCouponDiscount(BigDecimal.ZERO);

        // Calcula o total inicial: subtotal + frete
        order.setTotal(
                subtotal.add(order.getShippingCost())
        );

        // Caso tenha cupom aplicado
        if (request.couponCode() != null && !request.couponCode().isBlank()) {

            Coupon coupon = couponRepository.findByCode(request.couponCode())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Cupom não encontrado"
                    ));

            applyCoupon(order, coupon);
        }

        // Salva o pedido
        Order savedOrder = orderRepository.save(order);

        // Cria o registro de pagamento
        Payment payment = paymentService.createPayment(savedOrder);

        // Inicializa o pagamento no Stripe
        StripeCheckoutResponse checkout =
                paymentService.initializePayment(payment);

        return orderMapper.toOrderCheckoutResponse(
                savedOrder,
                checkout.checkoutUrl()
        );
    }

    // Valida se o cupom pode ser aplicado
    private UserCoupon validateCouponApplication(Order order, Coupon coupon) {

        // Recupera a relação do usuário com o cupom e valida sua utilização
        UserCoupon userCoupon = userCouponService.validateUseCoupon(coupon.getId());

        // Verifica se o valor mínimo do pedido foi atingido
        if (order.getSubtotal().compareTo(coupon.getMinOrderValue()) < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O valor mínimo para utilizar esse cupom não foi atingido."
            );
        }

        // Verifica se o usuário atingiu o limite de utilização
        if (userCoupon.getUsageCount() >= userCoupon.getUsageLimit()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Limite de utilização do cupom atingido."
            );
        }

        return userCoupon;
    }

    // Aplica o desconto do cupom
    private void applyCoupon(Order order, Coupon coupon) {

        // Valida aplicação do cupom
        validateCouponApplication(order, coupon);

        // Calcula o desconto de acordo com o tipo do cupom
        BigDecimal discount = switch (coupon.getCouponType()) {

            case ORDER ->
                    calculateDiscount(order.getSubtotal(), coupon)
                            .min(order.getSubtotal());

            case SHIPPING ->
                    calculateDiscount(order.getShippingCost(), coupon)
                            .min(order.getShippingCost());
        };

        // Registra o desconto do cupom
        order.setCouponDiscount(discount);

        // Relaciona o cupom ao pedido
        order.setCoupon(coupon);

        // Recalcula o total
        order.setTotal(
                order.getSubtotal()
                        .add(order.getShippingCost())
                        .subtract(discount)
        );

    }

    // Calcula o desconto de acordo com o tipo configurado
    private BigDecimal calculateDiscount(BigDecimal baseValue, Coupon coupon) {

        return switch (coupon.getDiscountType()) {

            case PERCENTAGE -> baseValue
                    .multiply(coupon.getDiscountValue())
                    .divide(
                            BigDecimal.valueOf(100),
                            2,
                            RoundingMode.HALF_UP
                    );

            case FIXED_AMOUNT -> coupon.getDiscountValue()
                    .min(baseValue);
        };
    }
}
