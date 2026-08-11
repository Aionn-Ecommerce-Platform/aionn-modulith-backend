package com.aionn.inventory.application.usecase.analytics;

import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.mapper.InventoryItemResultMapper;
import com.aionn.inventory.application.port.in.inventory.ListAdminLowStockInputPort;
import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAdminLowStockUseCase implements ListAdminLowStockInputPort {
    private final InventoryItemPersistencePort inventoryRepository;
    private final InventoryItemResultMapper resultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryItemResult> execute(OffsetPagination pagination) {
        var page = inventoryRepository.findAllLowStock(pagination);
        return new PageResult<>(resultMapper.toResults(page.content()), page.page(), page.size(),
                page.returned(), page.totalElements());
    }
}
