package micronet.user.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates allowed frontend base URLs for OAuth2 post-login redirect and stores the chosen base in a short-lived cookie.
 */
@Component
public class OAuth2FrontendReturnUrlSupport {

    public static final String COOKIE_NAME = "OAUTH2_FRONTEND_BASE";

    @Value("${app.oauth2.allowed-frontend-base-urls:https://essleman-se.github.io/user-management-UI,http://localhost:5173/user-management-UI}")
    private String allowedBasesRaw;

    @Value("${app.frontend.url:http://localhost:5173/user-management-UI}")
    private String defaultFrontendBaseUrl;

    public String defaultFrontendBase() {
        return normalizeBase(defaultFrontendBaseUrl);
    }

    public List<String> allowedBases() {
        return Arrays.stream(allowedBasesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public boolean isAllowedBase(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return false;
        }
        String normalized = normalizeBase(candidate);
        for (String allowed : allowedBases()) {
            if (normalized.equals(normalizeBase(allowed))) {
                return true;
            }
        }
        return false;
    }

    public String normalizeBase(String base) {
        String s = base.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Prefer Origin, then Referer host, then default from properties.
     */
    public String resolveFrontendBaseFromRequest(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin)) {
            String baseOrigin = origin.replaceAll("/+$", "");
            String withPath = baseOrigin.contains("/user-management-UI")
                    ? baseOrigin
                    : baseOrigin + "/user-management-UI";
            if (isAllowedBase(withPath)) {
                return normalizeBase(withPath);
            }
        }
        String referer = request.getHeader("Referer");
        if (StringUtils.hasText(referer)) {
            try {
                int q = referer.indexOf('?');
                String withoutQuery = q > 0 ? referer.substring(0, q) : referer;
                if (withoutQuery.contains("/user-management-UI")) {
                    int end = withoutQuery.indexOf("/user-management-UI") + "/user-management-UI".length();
                    String candidate = withoutQuery.substring(0, end);
                    if (isAllowedBase(candidate)) {
                        return normalizeBase(candidate);
                    }
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        return normalizeBase(defaultFrontendBaseUrl);
    }

    public void writeReturnUrlCookie(HttpServletResponse response, String returnUrl) {
        if (!isAllowedBase(returnUrl)) {
            return;
        }
        String encoded = URLEncoder.encode(normalizeBase(returnUrl), StandardCharsets.UTF_8);
        Cookie cookie = new Cookie(COOKIE_NAME, encoded);
        cookie.setPath("/");
        cookie.setMaxAge(600);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    public String readReturnUrlCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie c : request.getCookies()) {
            if (COOKIE_NAME.equals(c.getName()) && StringUtils.hasText(c.getValue())) {
                try {
                    return normalizeBase(URLDecoder.decode(c.getValue(), StandardCharsets.UTF_8));
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    public void clearReturnUrlCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public String buildOAuth2AuthorizationUrlWithReturnUrl(String provider, String returnBase) {
        String base = normalizeBase(returnBase);
        return UriComponentsBuilder.fromPath("/oauth2/authorization/" + provider.toLowerCase())
                .queryParam("return_url", base)
                .build()
                .toUriString();
    }

    public String buildFrontendCallbackUrl(String frontendBase, String token, String email, String role) {
        String root = normalizeBase(frontendBase);
        String callback = root + "/oauth2/callback";
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(callback)
                .queryParam("token", token)
                .queryParam("email", email);
        if (StringUtils.hasText(role)) {
            b.queryParam("role", role);
        }
        return b.build().toUriString();
    }
}
