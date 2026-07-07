package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.TrackProductViewCommand;

public interface TrackProductViewInputPort {

    void execute(TrackProductViewCommand command);
}
