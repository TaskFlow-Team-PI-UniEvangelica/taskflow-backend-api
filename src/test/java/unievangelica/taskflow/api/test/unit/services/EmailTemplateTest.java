package unievangelica.taskflow.api.test.unit.services;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EmailTemplateTest {

    private Configuration freemarkerConfig;

    @BeforeEach
    void setUp() throws Exception {
        // Mesma base usada pelo Spring Boot: versão padrão do FreeMarkerConfigurationFactory
        // e "recognize_standard_file_extensions=true" (habilita auto-escape HTML em .ftlh).
        freemarkerConfig = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        freemarkerConfig.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates/"));
        freemarkerConfig.setSetting("recognize_standard_file_extensions", "true");
        freemarkerConfig.setDefaultEncoding("UTF-8");
    }

    private String renderInvite(String inviterName, String organizationName) throws Exception {
        Template template = freemarkerConfig.getTemplate("invite.ftlh");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, Map.of(
                "inviterName", inviterName,
                "organizationName", organizationName,
                "frontendUrl", "http://localhost:5173"
        ));
    }

    @Test
    @DisplayName("Deve escapar HTML injetado no nome da organização e do convidador")
    void testEscapaHtmlNasVariaveis() throws Exception {
        String html = renderInvite(
                "<b>Atacante</b>",
                "<a href=\"https://phishing.example\">Clique aqui</a><script>alert(1)</script>"
        );

        assertFalse(html.contains("<script>"), "Tag <script> não deve ser renderizada");
        assertFalse(html.contains("<a href=\"https://phishing.example\">"), "Link injetado não deve ser renderizado");
        assertFalse(html.contains("<b>Atacante</b>"), "Tag <b> injetada não deve ser renderizada");
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(html.contains("&lt;b&gt;Atacante&lt;/b&gt;"));
    }

    @Test
    @DisplayName("Deve renderizar normalmente nomes comuns e o link do frontend")
    void testRenderizaNomesComuns() throws Exception {
        String html = renderInvite("Maria Souza", "Equipe Alpha");

        assertTrue(html.contains("<strong>Maria Souza</strong>"));
        assertTrue(html.contains("<strong>Equipe Alpha</strong>"));
        assertTrue(html.contains("href=\"http://localhost:5173/\""));
    }
}
