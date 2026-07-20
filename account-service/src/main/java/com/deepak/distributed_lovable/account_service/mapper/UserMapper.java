package com.deepak.distributed_lovable.account_service.mapper;


import com.deepak.distributed_lovable.account_service.dto.auth.SignupRequest;
import com.deepak.distributed_lovable.account_service.dto.auth.UserProfileResponse;
import com.deepak.distributed_lovable.account_service.entity.User;
import com.deepak.distributed_lovable.common_lib.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {


    User toEntity(SignupRequest signupRequest);

    UserProfileResponse toUserProfileResponse(User user);

    UserDto toUserDto(User user);
}
