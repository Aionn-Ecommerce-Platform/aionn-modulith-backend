package com.aionn.promotion.application.dto.voucher.result;

import java.util.List;

public record UserVoucherPageResult(List<UserVoucherResult> content, long totalElements) {
}
