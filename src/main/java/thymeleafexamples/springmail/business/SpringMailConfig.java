package thymeleafexamples.springmail.business;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;


@Configuration
@PropertySource("classpath:mail/emailconfig.properties")
public class SpringMailConfig implements ApplicationContextAware, EnvironmentAware {

    public static final String EMAIL_TEMPLATE_ENCODING = "UTF-8";

    private static final String JAVA_MAIL_FILE = "classpath:mail/javamail.properties";

    private static final String HOST = "mail.server.host";
    private static final String PORT = "mail.server.port";
    private static final String PROTOCOL = "mail.server.protocol";
    private static final String USERNAME = "mail.server.username";
    private static final String PASSWORD = "mail.server.password";


    private ApplicationContext applicationContext;
    private Environment environment;



    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }


    /*
     * SPRING + JAVAMAIL: JavaMailSender instance, configured via .properties files.
     */
    @Bean
    public JavaMailSender mailSender() throws IOException {

        final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Basic mail sender configuration, based on emailconfig.properties
        mailSender.setHost(this.environment.getProperty(HOST));
        mailSender.setPort(Integer.parseInt(this.environment.getProperty(PORT)));
        mailSender.setProtocol(this.environment.getProperty(PROTOCOL));
        mailSender.setUsername(this.environment.getProperty(USERNAME));
        mailSender.setPassword(this.environment.getProperty(PASSWORD));

        // JavaMail-specific mail sender configuration, based on javamail.properties
        final Properties javaMailProperties = new Properties();
        javaMailProperties.load(this.applicationContext.getResource(JAVA_MAIL_FILE).getInputStream());
        mailSender.setJavaMailProperties(javaMailProperties);

        return mailSender;

    }


    /*
     *  Message externalization/internationalization for emails.
     *
     *  NOTE we are avoiding the use of the name 'messageSource' for this bean because that
     *       would make the MessageSource defined in SpringWebConfig (and made available for the
     *       web-side template engine) delegate to this one, and thus effectively merge email
     *       messages into web messages and make both types available at the web side, which could
     *       bring undesired collisions.
     *
     *  NOTE also that given we want this specific message source to be used by our
     *       SpringTemplateEngines for emails (and not by the web one), we will set it explicitly
     *       into each of the TemplateEngine objects with 'setTemplateEngineMessageSource(...)'
     */
    @Bean
    public ResourceBundleMessageSource emailMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("mail/MailMessages");
        return messageSource;
    }


    /*
     * THYMELEAF: Template Engine (Spring4-specific version) for HTML email templates.
     */
    @Bean
    public TemplateEngine htmlTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(htmlTemplateResolver());
        templateEngine.setTemplateEngineMessageSource(emailMessageSource());
        return templateEngine;
    }

    /*
     * THYMELEAF: Template Engine (Spring4-specific version) for TEXT email templates.
     */
    @Bean
    public TemplateEngine textTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(textTemplateResolver());
        templateEngine.setTemplateEngineMessageSource(emailMessageSource());
        return templateEngine;
    }

    /*
     * THYMELEAF: Template Engine (Spring4-specific version) for in-memory HTML email templates.
     */
    @Bean
    public TemplateEngine stringTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(stringTemplateResolver());
        templateEngine.setTemplateEngineMessageSource(emailMessageSource());
        return templateEngine;
    }

    /*
     * THYMELEAF: Template Resolver for TEXT email templates.
     */
    private ITemplateResolver textTemplateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/mail/");
        templateResolver.setSuffix(".txt");
        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setCharacterEncoding(EMAIL_TEMPLATE_ENCODING);
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    /*
     * THYMELEAF: Template Resolver for HTML email templates.
     */
    private ITemplateResolver htmlTemplateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/mail/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding(EMAIL_TEMPLATE_ENCODING);
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    /*
     * THYMELEAF: Template Resolver for String templates. (template will be a passed String -- for editable templates)
     */
    private ITemplateResolver stringTemplateResolver() {
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode("HTML5");
        templateResolver.setCacheable(false);
        return templateResolver;
    }

}
