package com.aionn.sharedkernel.integration.port.identity;

import java.util.Map;
import java.util.Set;

public interface UserProfileLookupPort {

    record UserProfile(String userId, String username, String displayName, String avatarUrl) {}

    Map<String, UserProfile> findProfiles(Set<String> userIds);
}
