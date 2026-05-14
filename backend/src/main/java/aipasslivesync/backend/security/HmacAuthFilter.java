package aipasslivesync.backend.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
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

        String signature = httpRequest.getHeader(SIGNATURE_HEADER);

        if (signature == null || signature.isBlank()) {
            log.warn("Webhook request missing {} header", SIGNATURE_HEADER);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing " + SIGNATURE_HEADER + " header");
            return;
        }

        byte[] body = httpRequest.getInputStream().readAllBytes();
        String computed = computeHmac(body);

        if (!signature.equals(computed)) {
            log.warn("Webhook HMAC signature mismatch — rejecting request");
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid webhook signature");
            return;
        }

        log.debug("Webhook HMAC signature verified");
        chain.doFilter(new CachedBodyRequestWrapper(httpRequest, body), response);
    }

    private static class CachedBodyRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.cachedBody = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ServletInputStream() {
                private final java.io.ByteArrayInputStream delegate = new java.io.ByteArrayInputStream(cachedBody);

                @Override public boolean isFinished() { return delegate.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {}
                @Override public int read() { return delegate.read(); }
                @Override public int read(byte[] b, int off, int len) { return delegate.read(b, off, len); }
            };
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
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
