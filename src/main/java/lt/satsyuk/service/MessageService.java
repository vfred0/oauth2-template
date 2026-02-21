package lt.satsyuk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, getCurrentLocale());
    }

    public String getMessage(String code, String defaultMessage, Object... args) {
        return messageSource.getMessage(code, args, defaultMessage, getCurrentLocale());
    }

    private Locale getCurrentLocale() {
        var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            return localeResolver.resolveLocale(requestAttributes.getRequest());
        }
        return Locale.ENGLISH;
    }
}
