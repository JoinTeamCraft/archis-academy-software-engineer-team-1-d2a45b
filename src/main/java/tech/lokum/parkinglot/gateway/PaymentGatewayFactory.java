package tech.lokum.parkinglot.gateway;

import org.springframework.stereotype.Component;
import tech.lokum.parkinglot.exception.BadRequestException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory and registry resolving payment gateway implementations by provider type.
 */
@Component
public class PaymentGatewayFactory {

    private final Map<PaymentGatewayType, PaymentGateway> gateways = new EnumMap<>(PaymentGatewayType.class);

    public PaymentGatewayFactory(List<PaymentGateway> gatewayList) {
        if (gatewayList != null) {
            for (PaymentGateway gateway : gatewayList) {
                gateways.put(gateway.getGatewayType(), gateway);
            }
        }
    }

    /**
     * Resolves the gateway corresponding to the given type, defaulting to Stripe if null.
     */
    public PaymentGateway getGateway(PaymentGatewayType type) {
        PaymentGatewayType target = (type != null) ? type : PaymentGatewayType.STRIPE;
        PaymentGateway gateway = gateways.get(target);

        if (gateway == null) {
            // If PayPal requested but not configured as separate bean, route through sandbox/mock
            if (target == PaymentGatewayType.PAYPAL) {
                gateway = gateways.get(PaymentGatewayType.STRIPE);
            }
        }

        if (gateway == null) {
            throw new BadRequestException("Payment gateway is not configured for provider: " + target);
        }

        return gateway;
    }
}
