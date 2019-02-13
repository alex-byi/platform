package lsfusion.base;

import org.apache.commons.lang3.LocaleUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

public class ServerUtils {
    public static final String LOCALE_COOKIE_NAME = "LSFUSION_LOCALE";
    private static final String DEFAULT_LOCALE_LANGUAGE = "ru";

    public static Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new RuntimeException("Not authorized");
        }
        return auth;
    }

    public static String getAuthorizedUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "Not authorized" : auth.getName();
    }

    public static Locale getLocale(HttpServletRequest request) {
        if(request != null)
            return request.getLocale();
        return LocaleContextHolder.getLocale(); // just in case
    }
}
