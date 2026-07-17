package com.aionn.inventory.application.usecase.warehouse;

import com.aionn.inventory.application.dto.warehouse.query.ListWarehousesByOwnerQuery;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.ListWarehousesByOwnerInputPort;
import com.aionn.inventory.application.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListWarehousesByOwnerUseCase implements ListWarehousesByOwnerInputPort {

    private final WarehouseService warehouseService;

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResult> execute(ListWarehousesByOwnerQuery query) {
        return warehouseService.listByOwner(query.ownerId());
    }
}
