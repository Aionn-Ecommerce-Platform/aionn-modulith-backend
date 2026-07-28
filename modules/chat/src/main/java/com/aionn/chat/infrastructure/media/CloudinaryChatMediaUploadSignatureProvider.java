package com.aionn.chat.infrastructure.media;

import com.aionn.chat.application.dto.media.result.UploadSignatureResult;
import com.aionn.chat.application.port.out.media.ChatMediaUploadSignatureProviderPort;
import com.aionn.chat.infrastructure.config.properties.ChatCloudinaryProperties;
import com.aionn.sharedkernel.media.CloudinaryCredentialsProperties;
import com.aionn.sharedkernel.media.CloudinarySigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chat.media", name = "provider", havingValue = "cloudinary",
        matchIfMissing = true)
public class CloudinaryChatMediaUploadSignatureProvider
        implements ChatMediaUploadSignatureProviderPort {

    private final CloudinaryCredentialsProperties credentials;
    private final ChatCloudinaryProperties folders;
    private final Clock clock;

    @Override
    public UploadSignatureResult generateChatImageUploadSignature(String userId) {
        String folder = folders.chatImageFolder() + "/" + userId;
        long timestamp = clock.instant().getEpochSecond();

        Map<String, String> params = new TreeMap<>();
        params.put("folder", folder);
        params.put("timestamp", String.valueOf(timestamp));

        String signature = CloudinarySigner.sign(params, credentials.apiSecret());

        log.debug("Generated chat-image upload signature, userId={} folder={}", userId, folder);
        return new UploadSignatureResult(
                signature,
                String.valueOf(timestamp),
                credentials.apiKey(),
                credentials.cloudName(),
                credentials.uploadUrl("image"),
                folder);
    }
}
