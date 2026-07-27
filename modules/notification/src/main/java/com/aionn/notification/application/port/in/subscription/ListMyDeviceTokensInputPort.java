package com.aionn.notification.application.port.in.subscription;

import com.aionn.notification.application.dto.subscription.result.DeviceTokenResult;

import java.util.List;

public interface ListMyDeviceTokensInputPort {
    List<DeviceTokenResult> execute(String userId);
}