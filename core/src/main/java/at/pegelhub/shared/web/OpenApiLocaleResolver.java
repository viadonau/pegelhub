package at.pegelhub.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

final class OpenApiLocaleResolver implements LocaleResolver {

    private static final String OPEN_API_PATH = "/v3/api-docs";
    private static final Locale DEFAULT_OPEN_API_LOCALE = Locale.ENGLISH;

    private final LocaleResolver defaultResolver = new AcceptHeaderLocaleResolver();

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        if (!request.getRequestURI().startsWith(OPEN_API_PATH)) {
            return defaultResolver.resolveLocale(request);
        }

        String languageTag = request.getParameter("lang");
        if (languageTag == null || languageTag.isBlank()) {
            return DEFAULT_OPEN_API_LOCALE;
        }

        return switch (Locale.forLanguageTag(languageTag).getLanguage()) {
            case "de" -> Locale.GERMAN;
            case "en" -> Locale.ENGLISH;
            default -> DEFAULT_OPEN_API_LOCALE;
        };
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("The request locale cannot be changed.");
    }
}
