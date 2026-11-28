package com.nulvora.nulvora.payment.service;

import com.nulvora.nulvora.api.stripe.dto.response.StripeCheckoutResponse;
import com.nulvora.nulvora.api.stripe.service.StripePaymentService;
import com.nulvora.nulvora.order.entity.Order;
import com.nulvora.nulvora.order.entity.OrderStatus;
import com.nulvora.nulvora.order.repository.OrderRepository;
import com.nulvora.nulvora.payment.entity.Payment;
import com.nulvora.nulvora.payment.entity.PaymentStatus;
import com.nulvora.nulvora.payment.repository.PaymentRepository;
import com.nulvora.nulvora.userCoupon.service.UserCouponService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


// responsável por registrar o pagamento no banco.

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final StripePaymentService stripePaymentService;
    private final OrderRepository orderRepository;
    private final UserCouponService userCouponService;


    // criar pagamento no banco
    public Payment createPayment(Order order) {

        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setAmount(order.getTotal());
        payment.setStatus(PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    // incicializar o pagamento do stripe
    public StripeCheckoutResponse initializePayment(Payment payment)
            throws StripeException {

        // solicita ao Stripe a criação de uma sessão de Checkout
        StripeCheckoutResponse response =
                stripePaymentService.createCheckoutSession(payment);

        // adciona na entidade de pagamento
        payment.setStripeCheckoutSessionId(response.sessionId());

        paymentRepository.save(payment);

        return response;
    }

    // processar o evento do webHook da Stripe
    @Transactional
    public void processWebhookEvent(Event event) {

        // pega o tipo do evento
        String eventType = event.getType();

        // Identifica qual evento aconteceu e decide o que fazer.
        switch (eventType) {
            // Se o evento for checkout.session.completed OU
            // checkout.session.async_payment_succeeded, execute esse bloco.
            case "checkout.session.completed",
                 "checkout.session.async_payment_succeeded" -> {

                // desserializa o evento, converte os dados recebidos em um objeto Java Session
                EventDataObjectDeserializer deserializer =
                        event.getDataObjectDeserializer();

                Session session;

                try {
                    session = (Session) deserializer.deserializeUnsafe();
                } catch (EventDataObjectDeserializationException e) {
                    throw new IllegalStateException(
                            "Não foi possível desserializar a Checkout Session",
                            e
                    );
                }
                // confere se foi realmente pago e localiza o Payment no banco
                handleSuccessfulPayment(session);
            }

            default -> System.out.println(
                    "Evento ignorado: " + eventType
            );
        }
    }


    private void handleSuccessfulPayment(Session session) {

        // Confirma que o pagamento realmente foi aprovado
        if (!"paid".equals(session.getPaymentStatus())) {
            return;
        }

        // Localiza o pagamento pela Checkout Session
        Payment payment = paymentRepository
                .findByStripeCheckoutSessionId(session.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pagamento não encontrado para a sessão: " + session.getId()
                ));

        // Evita processar novamente um pagamento já aprovado
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }

        // Atualiza o Payment
        payment.setStripePaymentIntentId(session.getPaymentIntent());
        payment.setStatus(PaymentStatus.SUCCEEDED);

        // Atualiza o Order relacionado
        Order order = payment.getOrder();
        order.setStatus(OrderStatus.PAID);

        // Contabiliza a utilização do cupom, caso exista
        if (order.getCoupon() != null) {
            userCouponService.registerUsage(
                    order.getUser().getId(),
                    order.getCoupon().getId()
            );
        }

        paymentRepository.save(payment);
        orderRepository.save(order);
    }

}
