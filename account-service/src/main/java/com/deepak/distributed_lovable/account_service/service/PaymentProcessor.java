package com.deepak.distributed_lovable.account_service.service;

import com.deepak.distributed_lovable.account_service.dto.subscription.CheckoutRequest;
import com.deepak.distributed_lovable.account_service.dto.subscription.CheckoutResponse;
import com.deepak.distributed_lovable.account_service.dto.subscription.PortalResponse;
import com.stripe.model.StripeObject;

import java.util.Map;

public interface PaymentProcessor {

    CheckoutResponse getCheckOutResponse(CheckoutRequest request);

    PortalResponse openCustomerPortal();

    void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata);
}
