package micronet.user.oauth2;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * When the browser hits {@code /oauth2/authorization/{id}?return_url=...}, stores the validated return base in a cookie
 * so {@code /api/oauth2/success} can redirect to the correct frontend (prod vs local).
 */
@Component
public class OAuth2ReturnUrlCookieFilter extends OncePerRequestFilter {

    @Autowired
    private OAuth2FrontendReturnUrlSupport returnUrlSupport;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && uri.contains("/oauth2/authorization/")) {
            String param = request.getParameter("return_url");
            if (StringUtils.hasText(param) && returnUrlSupport.isAllowedBase(param)) {
                returnUrlSupport.writeReturnUrlCookie(response, param);
            }
        }
        filterChain.doFilter(request, response);
    }
}
