package com.example.myproject.config;

import com.example.myproject.service.SmsDlrService;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmppDlrRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(SmppDlrRoute.class);

    @Autowired
    private SmsDlrService smsDlrService;

    @Value("${sms.dlr.enabled:false}")
    private boolean dlrEnabled;

    @Override
    public void configure() throws Exception {
        // ✅ SI DÉSACTIVÉ, NE PAS CRÉER LA ROUTE
        if (!dlrEnabled) {
            log.info("📡 DLR Route DÉSACTIVÉE (sms.dlr.enabled=false)");
            return;
        }

        log.info("📡 DLR Route ACTIVÉE");

        // ✅ EXCEPTION HANDLER
        onException(Exception.class).log("❌ Erreur DLR: ${exception.message}").handled(true);

        // ✅ ROUTE DLR
        from(
            "smpp://{{camel.component.smpp.configuration.system-id}}" +
            "@{{camel.component.smpp.configuration.host}}:{{camel.component.smpp.configuration.port}}" +
            "?password={{camel.component.smpp.configuration.password}}" +
            "&enquireLinkTimer=60000" +
            "&reconnectDelay=10000" +
            "&lazySessionCreation=true"
        )
            .routeId("smpp-dlr-receiver")
            .autoStartup(true)
            .filter(header("CamelSmppMessageType").isEqualTo("DeliveryReceipt"))
            .log("📨 DLR: ID=${header.CamelSmppId}, Status=${header.CamelSmppStatus}")
            .process(exchange -> {
                String messageId = extractMessageId(exchange.getIn().getHeader("CamelSmppId"));
                Byte status = exchange.getIn().getHeader("CamelSmppStatus", Byte.class);
                String destAddr = exchange.getIn().getHeader("CamelSmppDestAddr", String.class);
                String body = exchange.getIn().getBody(String.class);

                log.info("📬 DLR: msgId={}, status={}, dest={}", messageId, status, destAddr);

                if (messageId != null) {
                    smsDlrService.processDlrFromSmpp(
                        messageId,
                        status != null ? status.intValue() : null,
                        destAddr,
                        parseErrorFromBody(body)
                    );
                }
            });
    }

    private String extractMessageId(Object msgIdObj) {
        if (msgIdObj == null) return null;
        if (msgIdObj instanceof java.util.List<?> list) {
            return list.isEmpty() ? null : list.get(0).toString();
        }
        return msgIdObj.toString();
    }

    private String parseErrorFromBody(String body) {
        if (body == null) return null;
        try {
            int errIndex = body.indexOf("err:");
            if (errIndex >= 0) {
                String errPart = body.substring(errIndex + 4);
                int spaceIndex = errPart.indexOf(" ");
                return spaceIndex > 0 ? errPart.substring(0, spaceIndex) : errPart.trim();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
