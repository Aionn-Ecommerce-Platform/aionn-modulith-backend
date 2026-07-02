package com.aionn.identity.application.mapper;

import com.aionn.identity.application.dto.user.view.UserProfileView;
import com.aionn.identity.domain.model.IdentityUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserResultMapper {

    @Mapping(target = "userId", expression = "java(user.getUserId())")
    @Mapping(target = "roles", expression = "java(mapRoles(user))")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    UserProfileView toUserProfileView(IdentityUser user);

    default Set<String> mapRoles(IdentityUser user) {
        return user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }
}
