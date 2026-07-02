package com.aionn.identity.application.usecase.user;

import com.aionn.identity.application.dto.user.command.UpdateAvatarCommand;
import com.aionn.identity.application.dto.user.view.UserProfileView;
import com.aionn.identity.application.port.in.user.UpdateAvatarInputPort;
import com.aionn.identity.application.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAvatarUseCase implements UpdateAvatarInputPort {

    private final ProfileService profileService;

    @Override
    @Transactional
    public UserProfileView execute(UpdateAvatarCommand command) {
        return profileService.updateAvatar(command);
    }
}

