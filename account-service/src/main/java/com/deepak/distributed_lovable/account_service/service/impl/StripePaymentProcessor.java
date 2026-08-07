package com.deepak.distributed_lovable.account_service.service.impl;


import com.deepak.distributed_lovable.account_service.dto.subscription.CheckoutRequest;
import com.deepak.distributed_lovable.account_service.dto.subscription.CheckoutResponse;
import com.deepak.distributed_lovable.account_service.dto.subscription.PortalResponse;
import com.deepak.distributed_lovable.account_service.entity.User;
import com.deepak.distributed_lovable.account_service.entity.Plan;
import com.deepak.distributed_lovable.account_service.repository.PlanRepository;
import com.deepak.distributed_lovable.account_service.repository.UserRepository;
import com.deepak.distributed_lovable.account_service.service.PaymentProcessor;
import com.deepak.distributed_lovable.account_service.service.SubscriptionService;
import com.deepak.distributed_lovable.common_lib.enums.SubscriptionStatus;
import com.deepak.distributed_lovable.common_lib.error.BadRequestException;
import com.deepak.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.deepak.distributed_lovable.common_lib.security.AuthUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProcessor implements PaymentProcessor {

    private final AuthUtil authUtil;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

//    @Value("${stripe.api.secretKey")
//    private String apiSecretKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public CheckoutResponse getCheckOutResponse(CheckoutRequest request) {

        Long userId=authUtil.getCurrentUserId();

        Plan plan= planRepository.findById(request.planId())
                .orElseThrow(()-> new ResourceNotFoundException("Plan", request.planId().toString()));

        User user= getUser(userId);

        var params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .setSubscriptionData(
                                new SessionCreateParams.SubscriptionData.Builder()
                                        .setBillingMode(SessionCreateParams.SubscriptionData.BillingMode.builder()
                                                .setType(SessionCreateParams.SubscriptionData.BillingMode.Type.FLEXIBLE)
                                                .build())
                                        .build()
                        )
                        .setSuccessUrl(frontendUrl + "/success.html?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(frontendUrl + "/cancel.html")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPrice(plan.getStripePriceId())
                                        .build())
                        .putMetadata("userId", userId.toString())
                        .putMetadata("planId", plan.getId().toString());


        try {
            String stripeCustomerId = user.getStripeCustomerId();
            if(stripeCustomerId==null || stripeCustomerId.isEmpty()){
                params.setCustomerEmail(user.getUsername());
            }else{
                params.setCustomer(stripeCustomerId);
            }
            Session session = Session.create(params.build());
            return new CheckoutResponse(session.getUrl());
        }
        catch (StripeException e){
            log.info("Stripe API error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public PortalResponse openCustomerPortal() {
        Long userId=authUtil.getCurrentUserId();
        User user= getUser(userId);
        String stripeCustomerId = user.getStripeCustomerId();

        if(stripeCustomerId==null || stripeCustomerId.isEmpty()){
            throw new BadRequestException("User does not have a Stripe Customer Id, UserId:"+userId);
        }

        try {
            var portalSession = com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setReturnUrl(frontendUrl)
                            .build()
            );

            return new PortalResponse(portalSession.getUrl());
        }
        catch (StripeException e){
            log.info("Stripe API error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata) {
        log.debug("Handling stripe event: {}", type);
        switch(type){
            case "checkout.session.completed" -> handleCheckoutSessionCompleted((Session)stripeObject,metadata); // one-time, on checkout completed
            case "customer.subscription.updated" -> handleSubscriptionUpdated((Subscription)stripeObject); // when user cancels, upgrades or any updates
            case "customer.subscription.deleted" -> handleSubscriptionDeleted((Subscription)stripeObject); //when subscription ends, revoke the access
            case "invoice.paid" -> handleInvoicePaid((Invoice)stripeObject); // when invoice is paid
            case "invoice.payment.failed" -> handleInvoicePaymentFailed((Invoice)stripeObject);  // when invoice is not paid, mark as PAST_DUE
            default -> log.debug("Ignoring the event: {}", type);
        }

    }


    private void handleInvoicePaymentFailed(Invoice invoice) {
        String subId= extrectSubscription(invoice);
        if(subId==null){
            return;
        }

        subscriptionService.markSubscriptionPastDue(subId);

    }

    private void handleInvoicePaid(Invoice invoice) {
        String subId= extrectSubscription(invoice);
        if(subId==null){
            return;
        }
        try{
            Subscription subscription = Subscription.retrieve(subId);
            SubscriptionItem item =subscription.getItems().getData().get(0);
            Instant periodStart= toInstant(item.getCurrentPeriodStart());
            Instant periodEnd = toInstant(item.getCurrentPeriodEnd());

            subscriptionService.renewSubscriptionPeriod(subId,periodStart,periodEnd);

        }
        catch (StripeException e){
            log.error("Failed to retrieve subscription for invoice {}: {}", invoice.getId(), e.getMessage());
            throw new RuntimeException(e);
        }
    }


    private void handleSubscriptionDeleted(Subscription subscription) {
        if(subscription==null){
            log.error("subscription object was null inside handleSubscriptionDeleted");
            return;
        }
        subscriptionService.cancelSubscription(subscription.getId());
    }

    private void handleSubscriptionUpdated(Subscription subscription) {

        if(subscription==null){
            log.error("subscription object was null inside handleCustomerSubscriptionUpdated");
            return;
        }
        SubscriptionStatus status= mapStripeStatusToEnum(subscription.getStatus());
        if (status == null) {
            log.warn("Unknown status '{}' for subscription {}", subscription.getStatus(), subscription.getId());
            return;
        }

        SubscriptionItem item= subscription.getItems().getData().get(0);
        Instant periodStart= toInstant(item.getCurrentPeriodStart());
        Instant periodEnd = toInstant(item.getCurrentPeriodEnd());

        Long planId= resolvePlanId(item.getPrice());

        subscriptionService.updateSubscription(subscription.getId(), status, periodStart, periodEnd,subscription.getCancelAtPeriodEnd(),planId);

    }



    private void handleCheckoutSessionCompleted(Session session, Map<String, String> metadata) {
        if(session == null) {
            log.error("session object was null");
            return;
        }
        Long userId = Long.valueOf(metadata.get("userId"));
        Long planId = Long.valueOf(metadata.get("planId"));

        String customerId= session.getCustomer();
        String subscriptionId = session.getSubscription();

        User user= getUser(userId);
        if(user.getStripeCustomerId() ==null) {
         user.setStripeCustomerId(customerId);
         userRepository.save(user);
        }
        subscriptionService.activateSubscription(userId, planId, subscriptionId, customerId);

    }

    //Utility Methods
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }

    private SubscriptionStatus mapStripeStatusToEnum(String status) {
        return switch (status) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due", "unpaid", "paused", "incomplete_expired" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            default -> {
                log.warn("Unmapped Stripe status: {}", status);
                yield null;
            }
        };
    }

    private Instant toInstant(Long epoch) {
        return epoch != null ? Instant.ofEpochSecond(epoch) : null;
    }

    private Long resolvePlanId(Price price) {
        if(price == null || price.getId()==null){
            return null;
        }
        return planRepository.findByStripePriceId(price.getId())
                .map(Plan::getId)
                .orElse(null);
    }

    private String extrectSubscription(Invoice invoice) {
        var parent = invoice.getParent();
        if (parent == null) return null;

        var subDetails = parent.getSubscriptionDetails();
        if (subDetails == null) return null;

        return subDetails.getSubscription();
    }
}
