package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import unievangelica.taskflow.api.domain.exception.BusinessException;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationRole;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.service.RequestValueParser;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RequestValueParserTest {

    @Test
    @DisplayName("Valores válidos devem ser convertidos, ignorando espaços e maiúsculas/minúsculas")
    void converteValoresValidos() {
        assertEquals(TaskEntity.Prioridade.alta, RequestValueParser.prioridade(" ALTA "));
        assertEquals(TaskEntity.Status.em_andamento, RequestValueParser.status("Em_Andamento"));
        assertEquals(OrganizationRole.SUB_OWNER, RequestValueParser.papel("sub_owner"));
        assertEquals(LocalDate.of(2026, 12, 31), RequestValueParser.prazo(" 2026-12-31 "));
    }

    @Test
    @DisplayName("Prioridade inválida deve gerar mensagem amigável, sem 'No enum constant'")
    void prioridadeInvalida() {
        BusinessException ex = assertThrows(BusinessException.class, () -> RequestValueParser.prioridade("urgente"));
        assertEquals("Prioridade inválida. Use: baixa, media ou alta.", ex.getMessage());
    }

    @Test
    @DisplayName("Status e papel inválidos ou nulos devem gerar mensagem amigável")
    void statusEPapelInvalidos() {
        assertEquals("Status inválido. Use: pendente, em_andamento ou concluida.",
                assertThrows(BusinessException.class, () -> RequestValueParser.status("arquivada")).getMessage());
        assertEquals("Papel inválido. Use: OWNER, SUB_OWNER, MANAGER ou MEMBER.",
                assertThrows(BusinessException.class, () -> RequestValueParser.papel("ADMIN")).getMessage());
        assertThrows(BusinessException.class, () -> RequestValueParser.status(null));
        assertThrows(BusinessException.class, () -> RequestValueParser.papel(null));
    }

    @Test
    @DisplayName("Prazo em formato diferente de AAAA-MM-DD ou data inexistente deve ser recusado")
    void prazoInvalido() {
        for (String valor : new String[]{"31/12/2026", "2026-02-30", "amanhã", ""}) {
            BusinessException ex = assertThrows(BusinessException.class, () -> RequestValueParser.prazo(valor), valor);
            assertEquals("Prazo inválido. Use o formato AAAA-MM-DD.", ex.getMessage());
        }
    }
}
