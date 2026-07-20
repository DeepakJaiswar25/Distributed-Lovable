package com.deepak.distributed_lovable.account_service.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Entity
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    @Column(unique = true)
    String stripePriceId;
    Integer maxProjects;
    Integer maxTokensPerDay;
    Integer maxPreviews;  //max number of previews allowed per plan
    Boolean unlimitedAi;  //unlimited access to LLM, ignore maxTokensPerDay if true
    Boolean active;
}
