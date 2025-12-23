package com.stack.sellstack.service.payment;

import com.razorpay.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class RazorpayHelper {

    public static void testRazorpayClient(RazorpayClient razorpayClient) throws RazorpayException {
        // Check available methods using reflection
        Class<?> paymentClientClass = razorpayClient.payments.getClass();
        log.info("PaymentClient methods:");
        for (java.lang.reflect.Method method : paymentClientClass.getDeclaredMethods()) {
            log.info("Method: {}", method.getName());
            log.info("Parameters: {}", java.util.Arrays.toString(method.getParameterTypes()));
        }
    }
}