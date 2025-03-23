package org.acme.dto;

import java.time.LocalDate;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Dados para criação de cobrança Pix")
public record PixCobrancaDTO(
    @Schema(description = "Chave Pix do recebedor", required = true, example = "email@exemplo.com")
    String chave,
    
    @Schema(description = "Valor da cobrança", required = true, example = "100.00")
    String valor,
    
    @Schema(description = "Nome do pagador", required = true, example = "Cliente Teste")
    String nome,
    
    @Schema(description = "CPF do pagador (obrigatório se CNPJ não for informado)", example = "12345678900")
    String cpf,
    
    @Schema(description = "CNPJ do pagador (obrigatório se CPF não for informado)", example = "12345678000199")
    String cnpj,
    
    @Schema(description = "Tempo de expiração da cobrança em segundos", defaultValue = "3600", example = "3600")
    Integer expiracao,

    @Schema(description = "Data de vencimento da cobrança", example = "1985-10-25")
    LocalDate dataVencimento,

    @Schema(description = "Código do banco", example = "127")
    String banco,

    @Schema(description = "Tipo de Pix gerado", example = "cob ou cobv")
    String tipoCob,
    
    @Schema(description = "Mensagem ao pagador", example = "Pagamento referente a compra #12345")
    String solicitacaoPagador

    // @Schema(description = "Informações adicionais", example = "Quinta parcela, de dez, da compra #12345")
    // List<PixInfoAdicional> infoAdicionais
) {
    
}
