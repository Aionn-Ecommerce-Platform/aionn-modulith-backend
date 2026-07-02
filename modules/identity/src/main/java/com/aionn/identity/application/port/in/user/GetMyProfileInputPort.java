package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.query.GetMyProfileQuery;
import com.aionn.identity.application.dto.user.view.UserProfileView;

public interface GetMyProfileInputPort {

    UserProfileView execute(GetMyProfileQuery query);
}




