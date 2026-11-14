package unievangelica.taskflow.api.domain.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final Configuration freemarkerConfig;

    @Value("${api.cors.frontend.url:http://localhost}")
    private String frontendUrl;

    @Value("${app.mail.from:nao-responda@taskflow.party}")
    private String mailFrom;

    public void sendInviteEmail(String to, String inviterName, String organizationName) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Convite para equipe no TaskFlow");
            helper.setFrom(mailFrom);

            Map<String, Object> model = Map.of(
                    "inviterName", inviterName,
                    "organizationName", organizationName,
                    "frontendUrl", frontendUrl
            );

            Template template = freemarkerConfig.getTemplate("invite.ftlh");
            String htmlContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

            helper.setText(htmlContent, true);

            javaMailSender.send(message);

        } catch (Exception e) {
            // O convite ja foi persistido e continua visivel no app; a falha de envio fica registrada no log.
            log.error("Falha ao enviar e-mail de convite para {} (organizacao '{}'): {}", to, organizationName, e.getMessage(), e);
        }
    }
}
