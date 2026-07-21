package com.aionn.identity.adapter.rest.mapper.consent;

import com.aionn.identity.adapter.rest.dto.consent.request.MarketingConsentRequest;
import com.aionn.identity.adapter.rest.dto.consent.request.TermsConsentRequest;
import com.aionn.identity.adapter.rest.dto.consent.response.ConsentResponse;
import com.aionn.identity.application.dto.consent.command.AgreePrivacyCommand;
import com.aionn.identity.application.dto.consent.command.AgreeTermsCommand;
import com.aionn.identity.application.dto.consent.result.ConsentResult;
import com.aionn.identity.application.dto.consent.command.UpdateMarketingConsentCommand;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConsentDtoMapper {

    AgreeTermsCommand toTermsConsentCommand(String userId, String clientIp, TermsConsentRequest request);

    AgreePrivacyCommand toPrivacyConsentCommand(String userId, String clientIp, TermsConsentRequest request);

    UpdateMarketingConsentCommand toMarketingConsentCommand(String userId, String clientIp,
            MarketingConsentRequest request);

    ConsentResponse toResponse(ConsentResult result);

    List<ConsentResponse> toResponses(List<ConsentResult> results);
}
