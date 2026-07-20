package com.deepak.distributed_lovable.account_service.repository;


import com.deepak.distributed_lovable.account_service.entity.Subscription;
import com.deepak.distributed_lovable.common_lib.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.Set;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
   Optional<Subscription> findByUserIdAndStatusIn(Long userId, Set<SubscriptionStatus> active);

    Boolean existsByStripeSubscriptionId(String subscriptionId);

    Optional<Subscription> findByStripeSubscriptionId(String gatewaySubscriptionId);
}