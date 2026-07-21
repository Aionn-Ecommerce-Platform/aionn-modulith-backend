package com.aionn.payment.infrastructure.persistence.adapter;

import com.aionn.payment.application.port.out.InvoiceStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LocalInvoiceStorage implements InvoiceStorage {

    @Value("${payment.invoice.base-url}")
    private String baseUrl;

    @Override
    public String storeInvoice(String paymentId, String orderId, byte[] pdfBytes) {
        log.debug("[INVOICE] storing {} bytes for payment={}", pdfBytes == null ? 0 : pdfBytes.length, paymentId);
        return storeInvoiceUrl(paymentId, orderId);
    }

    @Override
    public String storeInvoiceUrl(String paymentId, String orderId) {
        String url = baseUrl;
        if (!url.endsWith("/"))
            url += "/";
        return url + "invoices/" + paymentId + ".pdf";
    }
}
