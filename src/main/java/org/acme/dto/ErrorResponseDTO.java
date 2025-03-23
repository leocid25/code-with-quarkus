package org.acme.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO para resposta de erro padronizada
 */
@Schema(description = "Resposta para casos de erro")
public record ErrorResponseDTO(
        @Schema(description = "Mensagem de erro", example = "Cobrança não encontrada", required = true) String erro) {
    // Não é necessário implementar construtor, getters ou setters em um record
}
