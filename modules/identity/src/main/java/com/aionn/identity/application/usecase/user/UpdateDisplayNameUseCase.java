package com.aionn.identity.application.usecase.user;

import com.aionn.identity.application.dto.user.command.UpdateDisplayNameCommand;
import com.aionn.identity.application.dto.user.view.UserProfileView;
import com.aionn.identity.application.port.in.user.UpdateDisplayNameInputPort;
import com.aionn.identity.application.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateDisplayNameUseCase implements UpdateDisplayNameInputPort {

    private final ProfileService profileService;

    @Override
    @Transactional
    public UserProfileView execute(UpdateDisplayNameCommand command) {
        return profileService.updateDisplayName(command);
    }
}

