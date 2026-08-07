package com.deepak.distributed_lovable.account_service.mapper;

import com.deepak.distributed_lovable.account_service.dto.subscription.SubscriptionResponse;
import com.deepak.distributed_lovable.account_service.entity.Plan;
import com.deepak.distributed_lovable.account_service.entity.Subscription;
import com.deepak.distributed_lovable.common_lib.dto.PlanDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanDto toPlanResponse(Plan plan);

}
