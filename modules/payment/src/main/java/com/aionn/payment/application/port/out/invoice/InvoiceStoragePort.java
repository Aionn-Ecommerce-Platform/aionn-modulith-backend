package com.aionn.payment.application.port.out.invoice;

public interface InvoiceStoragePort {

    String storeInvoice(String paymentId, String orderId, byte[] pdfBytes);

    String storeInvoiceUrl(String paymentId, String orderId);
}
