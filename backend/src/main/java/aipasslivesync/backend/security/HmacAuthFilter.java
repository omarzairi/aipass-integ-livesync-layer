package aipasslivesync.backend.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
@Order(1)
public class HmacAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(HmacAuthFilter.class);
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    private static final String ALGORITHM = "HmacSHA256";

    @Value("${aipass.webhook.secret:}")
    private String webhookSecret;

    @Value("${aipass.webhook.auth-enabled:false}")
    private boolean authEnabled;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (!authEnabled || !httpRequest.getRequestURI().equals("/api/events/webhook")) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest, 10240);
        String signature = wrappedRequest.getHeader(SIGNATURE_HEADER);

        if (signature == null || signature.isBlank()) {
            log.warn("Webhook request missing {} header", SIGNATURE_HEADER);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing " + SIGNATURE_HEADER + " header");
            return;
        }

        chain.doFilter(wrappedRequest, response);

        byte[] body = wrappedRequest.getContentAsByteArray();
        String computed = computeHmac(body);

        if (!signature.equals(computed)) {
            log.warn("Webhook HMAC signature mismatch");
        }
    }

    private String computeHmac(byte[] body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(key);
            byte[] hash = mac.doFinal(body);
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("HMAC computation failed: {}", e.getMessage());
            return "";
        }
    }
}
