package com.aionn.notification.application.dto.notification.result;

import java.util.List;

public record NotificationPageResult(List<NotificationResult> content, long totalElements) {
}
