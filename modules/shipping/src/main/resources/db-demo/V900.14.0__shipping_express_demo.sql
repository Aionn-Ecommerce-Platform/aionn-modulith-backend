UPDATE shipping_rates
   SET express_extra_fee = 20000,
       express_eta_days = 1
 WHERE express_extra_fee = 0;
