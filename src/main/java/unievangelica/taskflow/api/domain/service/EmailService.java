package unievangelica.taskflow.api.domain.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.Map;

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

            Template template = freemarkerConfig.getTemplate("invite.ftl");
            String htmlContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

            helper.setText(htmlContent, true);

            javaMailSender.send(message);

        } catch (Exception e) {
            // Log error
            e.printStackTrace();
        }
    }
}
