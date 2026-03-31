package com.calevin.hodor.infrastructure.config;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * Configuración de la capa web para soporte de internacionalización (i18n).
 * 
 * @author Hodor Agent
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Define el resolvedor de locales persistido en la sesión del usuario.
     * El idioma por defecto es el español.
     * 
     * @return El resolvedor de locale basado en sesión.
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver sessionLocaleResolver = new SessionLocaleResolver();
        sessionLocaleResolver.setDefaultLocale(Locale.of("es"));
        return sessionLocaleResolver;
    }

    /**
     * Interceptor que permite cambiar el idioma mediante un parámetro en la URL (ej. ?lang=en).
     * 
     * @return El interceptor de cambio de locale.
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }

    /**
     * Registra los interceptores en el ciclo de vida de Spring MVC.
     * 
     * @param registry El registro de interceptores.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
