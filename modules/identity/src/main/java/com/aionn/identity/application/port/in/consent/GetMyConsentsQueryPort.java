package com.aionn.identity.application.port.in.consent;

import com.aionn.identity.application.dto.consent.result.ConsentResult;
import java.util.List;

public interface GetMyConsentsQueryPort {
    List<ConsentResult> execute(String userId);
}
