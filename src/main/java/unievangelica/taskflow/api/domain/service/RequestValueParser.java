package unievangelica.taskflow.api.domain.service;

import unievangelica.taskflow.api.domain.exception.BusinessException;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationRole;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Converte valores textuais recebidos da API (prioridade, status, papel e prazo) para os tipos do domínio.
 * Valores inválidos geram {@link BusinessException} com mensagem amigável, em vez de expor a mensagem
 * técnica da JVM (ex.: "No enum constant ...", "Text '...' could not be parsed").
 */
public final class RequestValueParser {

    private RequestValueParser() {
    }

    public static TaskEntity.Prioridade prioridade(String valor) {
        try {
            return TaskEntity.Prioridade.valueOf(normalizar(valor).toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Prioridade inválida. Use: baixa, media ou alta.");
        }
    }

    public static TaskEntity.Status status(String valor) {
        try {
            return TaskEntity.Status.valueOf(normalizar(valor).toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status inválido. Use: pendente, em_andamento ou concluida.");
        }
    }

    public static OrganizationRole papel(String valor) {
        try {
            return OrganizationRole.valueOf(normalizar(valor).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Papel inválido. Use: OWNER, SUB_OWNER, MANAGER ou MEMBER.");
        }
    }

    public static LocalDate prazo(String valor) {
        try {
            return LocalDate.parse(normalizar(valor));
        } catch (DateTimeParseException e) {
            throw new BusinessException("Prazo inválido. Use o formato AAAA-MM-DD.");
        }
    }

    /** Valor nulo é tratado como inválido (o chamador decide antes se o campo é opcional). */
    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
