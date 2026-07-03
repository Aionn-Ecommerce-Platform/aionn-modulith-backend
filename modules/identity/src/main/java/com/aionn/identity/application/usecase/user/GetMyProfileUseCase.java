package com.aionn.identity.application.usecase.user;

import com.aionn.identity.application.dto.user.query.GetMyProfileQuery;
import com.aionn.identity.application.dto.user.view.UserProfileView;
import com.aionn.identity.application.port.in.user.GetMyProfileInputPort;
import com.aionn.identity.application.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyProfileUseCase implements GetMyProfileInputPort {

    private final ProfileService profileService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileView execute(GetMyProfileQuery query) {
        return profileService.getMyProfile(query);
    }
}

