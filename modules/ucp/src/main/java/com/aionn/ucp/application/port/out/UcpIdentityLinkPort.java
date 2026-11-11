package com.aionn.ucp.application.port.out;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;

import java.util.Optional;

public interface UcpIdentityLinkPort {

    UcpIdentityLinkResponse saveLink(UcpIdentityLinkRequest request);

    Optional<UcpIdentityLinkResponse> findByPlatformAndSubject(String platformId, String platformSubject);

    Optional<UcpIdentityLinkResponse> findByCustomerId(String customerId);

    boolean revokeLink(String platformId, String platformSubject);
}
