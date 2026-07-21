package com.aionn.payment.application.port.out;

/** Stores or links to a generated invoice PDF. */
public interface InvoiceStorage {

    String storeInvoice(String paymentId, String orderId, byte[] pdfBytes);

    String storeInvoiceUrl(String paymentId, String orderId);
}
