package org.acme.resource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.acme.dto.ErrorResponseDTO;
import org.acme.dto.PixCobrancaDTO;
import org.acme.dto.PixPagamentoDTO;
import org.acme.dto.PixPagamentoResponseDTO;
import org.acme.model.PixComVencimento;
import org.acme.model.PixImediato;
import org.acme.service.PixService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint REST para operações relacionadas ao Pix
 */
@Path("/pix")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PixResource {

    private static final Logger LOG = Logger.getLogger(PixResource.class);

    @Inject
    PixService pixService;

    /**
     * Cria uma nova cobrança Pix
     * 
     * @param pixData Dados da cobrança no formato JSON
     * @return Resposta com os detalhes da cobrança criada
     */
    @POST
    @Path("/cobranca")
    @Operation(summary = "Cria uma nova cobrança Pix", description = "Este endpoint cria uma nova cobrança Pix com os dados fornecidos. "
            +
            "Uma cobrança Pix permite receber pagamentos instantâneos através do sistema Pix brasileiro. " +
            "Retorna um código QR e um Pix Copia e Cola que podem ser usados para efetuar o pagamento.")
    @APIResponse(responseCode = "201", description = "Cobrança criada com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "400", description = "Dados inválidos para criar a cobrança", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response criarCobranca(PixCobrancaDTO pixData) {
        try {
            LOG.info("Recebendo solicitação para criar cobrança Pix");

            // Extrair dados do DTO
            String txid = pixService.gerarTxid(pixData.tipoCob(), pixData.banco());
            String chave = pixData.chave();
            BigDecimal valor = new BigDecimal(pixData.valor());
            String nome = pixData.nome();
            String cpf = pixData.cpf();
            String cnpj = pixData.cnpj();
            String tipoCob = pixData.tipoCob();
            String banco = pixData.banco();
            LocalDate dataVencimento = pixData.dataVencimento();
            Integer expiracao = pixData.expiracao();

            // Validar dados obrigatórios
            if (chave == null || valor.compareTo(BigDecimal.ZERO) <= 0 || nome == null) {
                LOG.warn("Requisição inválida: campos obrigatórios ausentes");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new JsonObject().put("erro", "Chave Pix, valor e nome são obrigatórios").encode())
                        .build();
            }

            switch (tipoCob) {
                case "cob" -> {
                    // Criar objeto PixImediato
                    PixImediato pixImediato = new PixImediato(txid, chave, valor, nome, cpf, cnpj, expiracao, banco);

                    // Adicionar solicitação ao pagador se existir
                    if (pixData.solicitacaoPagador() != null && !pixData.solicitacaoPagador().isEmpty()) {
                        pixImediato.setSolicitacaoPagador(pixData.solicitacaoPagador());
                    }

                    // Adicionar informações adicionais
                    // if (pixData.infoAdicionais() != null && !pixData.infoAdicionais().isEmpty())
                    // {
                    // JsonArray infoArray = new JsonArray(pixData.infoAdicionais());
                    // for (int i = 0; i < infoArray.size(); i++) {
                    // JsonObject info = infoArray.getJsonObject(i);
                    // pixImediato.addInfoAdicional(info.getString("nome"),
                    // info.getString("valor"));
                    // }
                    // }

                    // Criar cobrança PIX
                    JsonObject resultado = pixService.criarCobrancaPix(pixImediato);

                    // Adicionar dados de retorno
                    JsonObject resposta = new JsonObject();
                    resposta.put("txid", pixImediato.getTxid());
                    resposta.put("status", pixImediato.getStatus());
                    resposta.put("pixCopiaECola", pixImediato.getPixCopiaECola());
                    resposta.put("qrCode", resultado.getString("pixCopiaECola"));
                    resposta.put("location", pixImediato.getLocation());
                    resposta.put("criacao", pixImediato.getCriacao().toString());
                    resposta.put("expiracao", pixImediato.getExpiracao());

                    return Response.status(Response.Status.CREATED).entity(resposta.encode()).build();

                }
                case "cobv" -> {
                    PixComVencimento pixComVencimento = new PixComVencimento(txid, chave, valor, nome, cpf, cnpj,
                            dataVencimento, banco);

                    // Adicionar solicitação ao pagador se existir
                    if (pixData.solicitacaoPagador() != null && !pixData.solicitacaoPagador().isEmpty()) {
                        pixComVencimento.setSolicitacaoPagador(pixData.solicitacaoPagador());
                    }

                    // Criar cobrança PIX
                    JsonObject resultado = pixService.criarCobrancaPixVencimento(pixComVencimento);

                    // Adicionar informações adicionais
                    // if (pixData.infoAdicionais() != null && !pixData.infoAdicionais().isEmpty())
                    // {
                    // JsonArray infoArray = new JsonArray(pixData.infoAdicionais());
                    // for (int i = 0; i < infoArray.size(); i++) {
                    // JsonObject info = infoArray.getJsonObject(i);
                    // pixImediato.addInfoAdicional(info.getString("nome"),
                    // info.getString("valor"));
                    // }
                    // }

                    // Adicionar dados de retorno
                    JsonObject resposta = new JsonObject();
                    resposta.put("txid", pixComVencimento.getTxid());
                    resposta.put("status", pixComVencimento.getStatus());
                    resposta.put("pixCopiaECola", pixComVencimento.getPixCopiaECola());
                    resposta.put("qrCode", resultado.getString("pixCopiaECola"));
                    resposta.put("location", pixComVencimento.getLocation());
                    resposta.put("criacao", pixComVencimento.getCriacao().toString());
                    resposta.put("dataVencimento", pixComVencimento.getDataVencimento().toString());

                    return Response.status(Response.Status.CREATED).entity(resposta.encode()).build();

                }
                default -> {
                    LOG.warn("Tipo de cobrança inválido: " + tipoCob);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new JsonObject().put("erro", "Tipo de cobrança inválido").encode())
                            .build();
                }
            }

        } catch (Exception e) {
            LOG.error("Erro ao criar cobrança Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    @DELETE
    @Path("/cobranca/{txid}")
    @Operation(summary = "Cancela uma cobrança Pix existente", description = "Este endpoint cancela uma cobrança Pix que ainda não foi paga. "
            +
            "Uma vez cancelada, a cobrança não poderá mais receber pagamentos e seu status será alterado para REMOVIDA_PELO_USUARIO_RECEBEDOR. "
            +
            "Não é possível cancelar cobranças que já foram pagas.")
    @APIResponse(responseCode = "200", description = "Cobrança cancelada com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "404", description = "Cobrança não encontrada", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "409", description = "Cobrança já foi paga e não pode ser cancelada", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response cancelarCobranca(@PathParam("txid") String txid) {
        try {
            LOG.info("Cancelando cobrança Pix: " + txid);

            // Primeiro, tenta localizar como cobrança imediata
            PixImediato pixImediato = pixService.consultarCobrancaRepository(txid);

            if (pixImediato != null) {
                // É uma cobrança imediata
                // Verificar se a cobrança já foi paga
                if (pixImediato.isPaga()) {
                    LOG.warn("Tentativa de cancelar cobrança já paga: " + txid);
                    return Response.status(Response.Status.CONFLICT)
                            .entity(new JsonObject().put("erro", "Não é possível cancelar uma cobrança já paga")
                                    .encode())
                            .build();
                }

                // Cancelar a cobrança
                pixImediato.cancelar();
                pixService.persistirPixImediato(pixImediato);

                return Response.ok(new JsonObject()
                        .put("txid", txid)
                        .put("status", "REMOVIDA_PELO_USUARIO_RECEBEDOR")
                        .put("mensagem", "Cobrança cancelada com sucesso")
                        .encode()).build();
            } else {
                // Tenta localizar como cobrança com vencimento
                PixComVencimento pixVencimento = pixService.consultarCobrancaVencimentoRepository(txid);

                if (pixVencimento == null) {
                    LOG.warn("Cobrança não encontrada para cancelamento: " + txid);
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(new JsonObject().put("erro", "Cobrança não encontrada").encode())
                            .build();
                }

                // É uma cobrança com vencimento
                // Verificar se a cobrança já foi paga
                if (pixVencimento.isPaga()) {
                    LOG.warn("Tentativa de cancelar cobrança com vencimento já paga: " + txid);
                    return Response.status(Response.Status.CONFLICT)
                            .entity(new JsonObject().put("erro", "Não é possível cancelar uma cobrança já paga")
                                    .encode())
                            .build();
                }

                // Atualizar a cobrança na API do banco e localmente
                pixService.atualizarCobrancaVencimento(txid, pixVencimento, true);

                return Response.ok(new JsonObject()
                        .put("txid", txid)
                        .put("status", "REMOVIDA_PELO_USUARIO_RECEBEDOR")
                        .put("mensagem", "Cobrança com vencimento cancelada com sucesso")
                        .encode()).build();
            }
        } catch (Exception e) {
            LOG.error("Erro ao cancelar cobrança Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Gera QR Code para uma cobrança existente
     * 
     * @param txid ID da transação
     * @return Imagem do QR Code em formato PNG
     */
    @GET
    @Path("/cobranca/{txid}/qrcode")
    @Produces("image/png")
    @Operation(summary = "Gera imagem QR Code para uma cobrança Pix", description = "Este endpoint gera uma imagem PNG do QR Code para uma cobrança Pix existente. "
            +
            "O QR Code pode ser escaneado por aplicativos bancários para efetuar o pagamento. " +
            "É necessário que a cobrança tenha um Pix Copia e Cola válido para gerar o QR Code.")
    @APIResponse(responseCode = "200", description = "QR Code gerado com sucesso", content = @Content(mediaType = "image/png"))
    @APIResponse(responseCode = "404", description = "Cobrança não encontrada", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "400", description = "Dados para QR Code não disponíveis", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response gerarQrCode(@PathParam("txid") String txid) {
        try {
            LOG.info("Gerando QR Code para cobrança: " + txid);

            String pixCopiaECola;

            // Primeiro verifica se é cobrança imediata
            PixImediato pixImediato = pixService.consultarCobrancaRepository(txid);
            if (pixImediato != null) {
                pixCopiaECola = pixImediato.getPixCopiaECola();
            } else {
                // Se não for imediata, verifica se é cobrança com vencimento
                PixComVencimento pixVencimento = pixService.consultarCobrancaVencimentoRepository(txid);
                if (pixVencimento == null) {
                    LOG.warn("Cobrança não encontrada para gerar QR Code: " + txid);
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(new JsonObject().put("erro", "Cobrança não encontrada").encode())
                            .type(MediaType.APPLICATION_JSON)
                            .build();
                }

                pixCopiaECola = pixVencimento.getPixCopiaECola();
            }

            // Verificar se o Pix Copia e Cola está disponível
            if (pixCopiaECola == null || pixCopiaECola.isEmpty()) {
                LOG.warn("Dados para QR Code não disponíveis: " + txid);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new JsonObject().put("erro", "Texto Pix Copia e Cola não disponível para esta cobrança")
                                .encode())
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            // Gerar imagem do QR Code
            byte[] qrCodeImage = pixService.gerarQrCodeImage(pixCopiaECola);

            return Response.ok(qrCodeImage)
                    .header("Content-Disposition", "inline; filename=\"qrcode-pix-" + txid + ".png\"")
                    .build();

        } catch (Exception e) {
            LOG.error("Erro ao gerar QR Code", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Registra o pagamento de uma cobrança Pix (simulação)
     * 
     * @param txid         ID da transação
     * @param pagamentoDTO Dados do pagamento
     * @return Resultado da operação
     */
    @POST
    @Path("/cobranca/{txid}/pagar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Registra o pagamento de uma cobrança Pix (simulação)", description = "Este endpoint simula o recebimento de um pagamento Pix para uma cobrança existente. "
            +
            "É utilizado apenas para fins de teste e simulação, já que em um ambiente real o pagamento seria " +
            "confirmado automaticamente pelo PSP (Provedor de Serviços de Pagamento). " +
            "Altera o status da cobrança para CONCLUIDA e registra os detalhes do pagamento.")
    @APIResponse(responseCode = "200", description = "Pagamento registrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PixPagamentoResponseDTO.class)))
    @APIResponse(responseCode = "404", description = "Cobrança não encontrada", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "409", description = "Cobrança já foi paga", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response registrarPagamento(@PathParam("txid") String txid,
            PixPagamentoDTO pagamentoDTO) {
        try {
            LOG.info("Registrando pagamento para cobrança: " + txid);
            LOG.info("Dados do pagamento: " + pagamentoDTO);

            // Primeiro verifica se é cobrança imediata
            PixImediato pixImediato = pixService.consultarCobrancaRepository(txid);

            if (pixImediato != null) {
                // É uma cobrança imediata
                // Verificar se a cobrança já foi paga
                if (pixImediato.isPaga()) {
                    LOG.warn("Cobrança já foi paga: " + txid);
                    return Response.status(Response.Status.CONFLICT)
                            .entity(new ErrorResponseDTO("Cobrança já foi paga"))
                            .build();
                }

                // Preparar dados do pagamento
                String endToEndId = pagamentoDTO.endToEndId();
                if (endToEndId == null || endToEndId.isEmpty()) {
                    // Gerar um ID se não for fornecido
                    endToEndId = "E" + System.currentTimeMillis();
                }

                BigDecimal valorPago = pagamentoDTO.valorPago();
                if (valorPago == null) {
                    valorPago = pixImediato.getValorOriginal();
                }

                String infoPagador = pagamentoDTO.infoPagador();
                if (infoPagador == null) {
                    infoPagador = "";
                }

                // Registrar o pagamento
                pixImediato.registrarPagamento(endToEndId, valorPago, infoPagador);
                pixService.persistirPixImediato(pixImediato);

                // Criar resposta
                PixPagamentoResponseDTO resposta = new PixPagamentoResponseDTO(
                        txid,
                        "CONCLUIDA",
                        valorPago,
                        endToEndId,
                        pixImediato.getHorarioPagamento());

                return Response.ok(resposta).build();
            } else {
                // Tenta localizar como cobrança com vencimento
                PixComVencimento pixVencimento = pixService.consultarCobrancaVencimentoRepository(txid);

                if (pixVencimento == null) {
                    LOG.warn("Cobrança não encontrada para pagamento: " + txid);
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponseDTO("Cobrança não encontrada"))
                            .build();
                }

                // É uma cobrança com vencimento
                // Verificar se a cobrança já foi paga
                if (pixVencimento.isPaga()) {
                    LOG.warn("Cobrança com vencimento já foi paga: " + txid);
                    return Response.status(Response.Status.CONFLICT)
                            .entity(new ErrorResponseDTO("Cobrança já foi paga"))
                            .build();
                }

                // Preparar dados do pagamento
                String endToEndId = pagamentoDTO.endToEndId();
                if (endToEndId == null || endToEndId.isEmpty()) {
                    // Gerar um ID se não for fornecido
                    endToEndId = "E" + System.currentTimeMillis();
                }

                BigDecimal valorPago = pagamentoDTO.valorPago();
                if (valorPago == null) {
                    valorPago = pixVencimento.getValorOriginal();
                }

                String infoPagador = pagamentoDTO.infoPagador();
                if (infoPagador == null) {
                    infoPagador = "";
                }

                // Registrar o pagamento
                pixVencimento.registrarPagamento(endToEndId, valorPago, infoPagador);
                pixService.persistirPixComVencimento(pixVencimento);

                // Criar resposta
                PixPagamentoResponseDTO resposta = new PixPagamentoResponseDTO(
                        txid,
                        "CONCLUIDA",
                        valorPago,
                        endToEndId,
                        pixVencimento.getHorarioPagamento());

                return Response.ok(resposta).build();
            }
        } catch (Exception e) {
            LOG.error("Erro ao registrar pagamento", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponseDTO("Erro ao registrar pagamento: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Converte um objeto PixImediato em um objeto JSON para retorno na API
     * 
     * @param pix Objeto PixImediato a ser convertido
     * @return Objeto JSON com os dados da cobrança
     */
    private JsonObject criarJsonDePix(PixImediato pix) {
        JsonObject json = new JsonObject();

        json.put("txid", pix.getTxid());
        json.put("status", pix.getStatus());
        json.put("chave", pix.getChave());
        json.put("valor", pix.getValorOriginal().toString());
        json.put("nome", pix.getNome());

        if (pix.getCpf() != null && !pix.getCpf().isEmpty()) {
            json.put("cpf", pix.getCpf());
        }

        if (pix.getCnpj() != null && !pix.getCnpj().isEmpty()) {
            json.put("cnpj", pix.getCnpj());
        }

        json.put("criacao", pix.getCriacao().toString());
        json.put("expiracao", pix.getExpiracao());

        if (pix.getPixCopiaECola() != null) {
            json.put("pixCopiaECola", pix.getPixCopiaECola());
        }

        if (pix.getLocation() != null) {
            json.put("location", pix.getLocation());
        }

        if (pix.isPaga()) {
            json.put("pago", true);
            json.put("horarioPagamento", pix.getHorarioPagamento().toString());
            json.put("valorPago", pix.getValorPago().toString());
        } else {
            json.put("pago", false);
        }

        if (pix.getSolicitacaoPagador() != null) {
            json.put("solicitacaoPagador", pix.getSolicitacaoPagador());
        }

        return json;
    }

    /**
     * Consulta uma cobrança Pix pelo TxID
     * 
     * @param txid ID da transação
     * @return Detalhes da cobrança se encontrada
     */
    @GET
    @Path("/cobranca/{txid}")
    @Operation(summary = "Consulta uma cobrança Pix pelo TxID no meu banco de dados.", description = "Este endpoint consulta uma cobrança Pix específica pelo seu identificador de transação (TxID) no meu banco de dados. "
            +
            "Retorna os detalhes completos da cobrança, incluindo status, valor, dados do pagador e, " +
            "se já paga, informações do pagamento. A consulta é feita no banco de dados local, não na API do PSP.")
    @APIResponse(responseCode = "200", description = "Cobrança encontrada com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "404", description = "Cobrança não encontrada", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response consultarCobranca(@PathParam("txid") String txid) {
        try {
            LOG.info("Consultando cobrança Pix: " + txid);

            // Primeiro verifica se é cobrança imediata
            PixImediato pixImediato = pixService.consultarCobrancaRepository(txid);

            if (pixImediato != null) {
                // Transformar objeto imediato em JSON
                JsonObject resultado = criarJsonDePix(pixImediato);
                return Response.ok(resultado.encode()).build();
            } else {
                // Verificar se é cobrança com vencimento
                PixComVencimento pixVencimento = pixService.consultarCobrancaVencimentoRepository(txid);

                if (pixVencimento == null) {
                    LOG.warn("Cobrança não encontrada: " + txid);
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(new JsonObject().put("erro", "Cobrança não encontrada").encode())
                            .build();
                }

                // Transformar objeto com vencimento em JSON
                JsonObject resultado = pixService.criarJsonDePixVencimento(pixVencimento);
                return Response.ok(resultado.encode()).build();
            }
        } catch (Exception e) {
            LOG.error("Erro ao consultar cobrança Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Consulta uma cobrança Pix diretamente na API do banco
     * 
     * @param txid ID da transação
     * @return Detalhes atualizados da cobrança
     */
    @GET
    @Path("/cobranca/{txid}/atualizar")
    @Operation(summary = "Consulta e atualiza informações de uma cobrança Pix com dados da API do banco", description = "Este endpoint consulta uma cobrança Pix diretamente na API do PSP/banco e atualiza o registro local."
            +
            "É útil para sincronizar o status e outras informações da cobrança quando ocorreram atualizações no PSP " +
            "que ainda não foram refletidas localmente, como confirmações de pagamento.")
    @APIResponse(responseCode = "200", description = "Cobrança atualizada com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response atualizarCobranca(@PathParam("txid") String txid) {
        try {
            LOG.info("Atualizando cobrança Pix da API: " + txid);

            // Verifica se é cobrança imediata no repositório local
            PixImediato pixImediato = pixService.consultarCobrancaRepository(txid);

            if (pixImediato != null) {
                // Atualiza cobrança imediata da API
                JsonObject resultado = pixService.consultarCobrancaNoServidor(txid);
                return Response.ok(resultado.encode()).build();
            } else {
                // Verifica se é cobrança com vencimento
                PixComVencimento pixVencimento = pixService.consultarCobrancaVencimentoRepository(txid);

                if (pixVencimento != null) {
                    // Atualiza cobrança com vencimento da API
                    JsonObject resultado = pixService.consultarCobrancaVencimentoNoServidor(txid);
                    return Response.ok(resultado.encode()).build();
                } else {
                    // Tenta consultar como cobrança imediata na API
                    try {
                        JsonObject resultado = pixService.consultarCobrancaNoServidor(txid);
                        return Response.ok(resultado.encode()).build();
                    } catch (Exception e) {
                        // Se falhar, tenta consultar como cobrança com vencimento
                        JsonObject resultado = pixService.consultarCobrancaVencimentoNoServidor(txid);
                        return Response.ok(resultado.encode()).build();
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Erro ao atualizar cobrança Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Lista as cobranças mais recentes
     * 
     * @return Lista de cobranças
     */
    @GET
    @Path("/cobrancas")
    @Operation(summary = "Lista as cobranças Pix mais recentes", description = "Este endpoint retorna uma lista das cobranças Pix mais recentes criadas pelo sistema. "
            +
            "É possível limitar a quantidade de resultados através do parâmetro de consulta 'limite'. " +
            "A lista é ordenada pela data de criação, com as cobranças mais recentes primeiro.")
    @APIResponse(responseCode = "200", description = "Lista de cobranças obtida com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response listarCobrancas(@QueryParam("limite") @DefaultValue("10") int limite,
            @QueryParam("tipo") @DefaultValue("todos") String tipo) {
        try {
            LOG.info("Listando " + limite + " cobranças Pix do tipo: " + tipo);

            JsonObject resultado = new JsonObject();
            JsonArray listaCobrancas = new JsonArray();

            // De acordo com o tipo solicitado, lista as cobranças
            if ("todos".equals(tipo) || "cob".equals(tipo)) {
                // Listar cobranças imediatas
                List<PixImediato> cobrancasImediatas = pixService.listarCobrancasRecentes(limite);

                for (PixImediato pix : cobrancasImediatas) {
                    listaCobrancas.add(criarJsonDePix(pix));
                }
            }

            if ("todos".equals(tipo) || "cobv".equals(tipo)) {
                // Listar cobranças com vencimento
                List<PixComVencimento> cobrancasVencimento = pixService.listarCobrancasVencimentoRecentes(limite);

                for (PixComVencimento pix : cobrancasVencimento) {
                    listaCobrancas.add(pixService.criarJsonDePixVencimento(pix));
                }
            }

            resultado.put("quantidade", listaCobrancas.size());
            resultado.put("cobrancas", listaCobrancas);

            return Response.ok(resultado.encode()).build();

        } catch (Exception e) {
            LOG.error("Erro ao listar cobranças Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Consulta detalhes de cobranças Pix usando consulta direta na API do banco
     * 
     * @param txid ID da transação
     * @return Detalhes completos da cobrança com informações de pagamento
     */
    @GET
    @Path("/cobranca/{txid}/detalhes")
    @Operation(summary = "Consulta detalhes completos de uma cobrança Pix direto da API do banco", description = "Este endpoint consulta detalhes completos de uma cobrança Pix diretamente na API do PSP/banco. "
            +
            "Diferente da consulta regular, este endpoint retorna todas as informações disponíveis na API, " +
            "incluindo dados detalhados sobre pagamentos e devoluções. É a forma mais completa de obter " +
            "informações atualizadas sobre uma cobrança Pix.")
    @APIResponse(responseCode = "200", description = "Detalhes da cobrança obtidos com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "400", description = "Erro na resposta da API do banco", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response consultarDetalhesCobranca(@PathParam("txid") String txid) {
        try {
            LOG.info("Consultando detalhes completos de cobrança Pix: " + txid);

            // Primeiro verifica se é cobrança imediata no repositório local
            PixImediato pixImediato = pixService.consultarCobrancaRepository(txid);
            JsonObject resultado;

            if (pixImediato != null) {
                // É uma cobrança imediata
                resultado = pixService.consultarDetalhesCobranca(txid);
            } else {
                // Verifica se é cobrança com vencimento
                PixComVencimento pixVencimento = pixService.consultarCobrancaVencimentoRepository(txid);

                if (pixVencimento != null) {
                    // É uma cobrança com vencimento
                    // Consultar detalhes na API do banco
                    resultado = pixService.consultarCobrancaVencimentoNoServidor(txid);
                } else {
                    // Tenta consultar como cobrança imediata na API do banco
                    try {
                        resultado = pixService.consultarDetalhesCobranca(txid);
                    } catch (Exception e) {
                        // Se falhar, tenta consultar como cobrança com vencimento
                        resultado = pixService.consultarCobrancaVencimentoNoServidor(txid);
                    }
                }
            }

            // Verificar se a resposta é um erro da API
            if (resultado.containsKey("type") && resultado.containsKey("title") && resultado.containsKey("status")) {
                LOG.warn("Erro na consulta de detalhes: " + resultado.getString("title"));
                return Response.status(resultado.getInteger("status", 400))
                        .entity(resultado.encode())
                        .build();
            }

            return Response.ok(resultado.encode()).build();

        } catch (Exception e) {
            LOG.error("Erro ao consultar detalhes da cobrança Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject()
                            .put("erro", "Falha na consulta de detalhes")
                            .put("mensagem", e.getMessage())
                            .encode())
                    .build();
        }
    }

    /**
     * Verifica o status atual do pagamento de uma cobrança Pix
     * 
     * @param txid ID da transação
     * @return Status do pagamento e detalhes se pago
     */
    @GET
    @Path("/cobranca/{txid}/status")
    @Operation(summary = "Verifica o status de pagamento de uma cobrança Pix", description = "Este endpoint verifica o status atual de pagamento de uma cobrança Pix consultando a API do PSP/banco. "
            +
            "Retorna um objeto simplificado contendo o status da cobrança (ATIVA, CONCLUIDA, etc.), se foi paga ou não, "
            +
            "e detalhes do pagamento quando realizado. É útil para verificar rapidamente se um Pix foi recebido.")
    @APIResponse(responseCode = "200", description = "Status do pagamento obtido com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "404", description = "Cobrança não encontrada ou não foi paga", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response verificarStatusPagamento(@PathParam("txid") String txid) {
        try {
            LOG.info("Verificando status de pagamento da cobrança: " + txid);

            // Primeiro verifica se é cobrança imediata no repositório local
            PixImediato pixImediato = pixService.consultarCobrancaRepository(txid);

            if (pixImediato != null) {
                // É uma cobrança imediata, consulta status de pagamento
                JsonObject statusPagamento = pixService.verificarStatusPagamento(txid);
                return Response.ok(statusPagamento.encode()).build();
            } else {
                // Verifica se é cobrança com vencimento
                PixComVencimento pixVencimento = pixService.consultarCobrancaVencimentoRepository(txid);

                if (pixVencimento != null) {
                    // É uma cobrança com vencimento
                    // Atualiza dados da API
                    pixService.consultarCobrancaVencimentoNoServidor(txid);

                    // Cria objeto de resposta com status atual
                    JsonObject statusPagamento = new JsonObject();
                    statusPagamento.put("txid", txid);
                    statusPagamento.put("status", pixVencimento.getStatus());
                    statusPagamento.put("valorOriginal", pixVencimento.getValorOriginal().toString());

                    if (pixVencimento.isPaga()) {
                        statusPagamento.put("pago", true);
                        statusPagamento.put("valorPago", pixVencimento.getValorPago().toString());
                        statusPagamento.put("horarioPagamento", pixVencimento.getHorarioPagamento().toString());
                    } else {
                        statusPagamento.put("pago", false);
                        statusPagamento.put("dataVencimento", pixVencimento.getDataVencimento().toString());
                    }

                    return Response.ok(statusPagamento.encode()).build();
                } else {
                    // Tenta verificar status como cobrança imediata na API
                    try {
                        JsonObject statusPagamento = pixService.verificarStatusPagamento(txid);
                        return Response.ok(statusPagamento.encode()).build();
                    } catch (Exception e) {
                        LOG.warn("Cobrança não encontrada no sistema: " + txid);
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(new JsonObject().put("erro", "Cobrança não encontrada").encode())
                                .build();
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Erro ao verificar status de pagamento", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Consulta se existe devolução para um Pix específico
     * 
     * @param txid ID da transação
     * @return Detalhes da devolução se existir
     */
    @GET
    @Path("/cobranca/{txid}/devolucao")
    @Operation(summary = "Consulta informações de devolução para uma cobrança Pix", description = "Este endpoint verifica se existe alguma devolução associada a um pagamento Pix. "
            +
            "As devoluções ocorrem quando um pagamento Pix já realizado precisa ser estornado, seja por " +
            "solicitação do pagador, problemas com o produto/serviço, ou outras razões. " +
            "Retorna detalhes da devolução quando existente, incluindo valor, motivo e status.")
    @APIResponse(responseCode = "200", description = "Informações de devolução obtidas com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response consultarDevolucao(@PathParam("txid") String txid) {
        try {
            LOG.info("Consultando devolução da cobrança: " + txid);

            // Por padrão, usamos o serviço para consulta de devoluções de cobranças
            // imediatas
            JsonObject resultado = pixService.consultarDevolucao(txid);

            return Response.ok(resultado.encode()).build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar devolução", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Lista cobranças com vencimento por período
     * 
     * @param dataInicio Data inicial de vencimento
     * @param dataFim    Data final de vencimento
     * @return Lista de cobranças com vencimento no período
     */
    @GET
    @Path("/cobrancas-vencimento/periodo")
    @Operation(summary = "Lista cobranças Pix com vencimento por período", description = "Este endpoint retorna cobranças Pix com vencimento dentro do período especificado. "
            +
            "É útil para visualizar e gerenciar cobranças cujos vencimentos estão próximos ou para análise histórica " +
            "de cobranças em determinado período.")
    @APIResponse(responseCode = "200", description = "Lista de cobranças obtida com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "400", description = "Parâmetros de data inválidos", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response listarCobrancasPorPeriodo(
            @QueryParam("dataInicio") String dataInicioStr,
            @QueryParam("dataFim") String dataFimStr) {
        try {
            LOG.info("Recebida solicitação para listar cobranças por período: " + dataInicioStr + " a " + dataFimStr);

            // Validar e converter parâmetros de data
            LocalDate dataInicio;
            LocalDate dataFim;

            try {
                dataInicio = LocalDate.parse(dataInicioStr);
                dataFim = LocalDate.parse(dataFimStr);
            } catch (Exception e) {
                LOG.warn("Formato de data inválido: " + e.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new JsonObject().put("erro", "Formato de data inválido. Use YYYY-MM-DD").encode())
                        .build();
            }

            if (dataFim.isBefore(dataInicio)) {
                LOG.warn("Data fim anterior à data início: " + dataFimStr + " < " + dataInicioStr);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new JsonObject().put("erro", "Data fim não pode ser anterior à data início").encode())
                        .build();
            }

            LOG.info("Listando cobranças Pix com vencimento entre " + dataInicio + " e " + dataFim);

            // Obter cobranças no período através do serviço
            List<PixComVencimento> cobrancas = pixService.listarCobrancasVencimentoPorPeriodo(dataInicio, dataFim);

            // Transformar lista em array JSON
            JsonObject resultado = new JsonObject();
            JsonArray listaCobrancas = new JsonArray();

            for (PixComVencimento pix : cobrancas) {
                listaCobrancas.add(pixService.criarJsonDePixVencimento(pix));
            }

            resultado.put("quantidade", cobrancas.size());
            resultado.put("cobrancas", listaCobrancas);

            LOG.info("Retornando " + cobrancas.size() + " cobranças encontradas");
            return Response.ok(resultado.encode()).build();
        } catch (Exception e) {
            LOG.error("Erro ao listar cobranças por período", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Lista cobranças com vencimento vencidas e não pagas
     * 
     * @return Lista de cobranças vencidas e não pagas
     */
    @GET
    @Path("/cobrancas-vencimento/vencidas")
    @Operation(summary = "Lista cobranças Pix com vencimento vencidas e não pagas", description = "Este endpoint retorna cobranças Pix com vencimento que já passaram da data de vencimento e não foram pagas. "
            +
            "É útil para acompanhamento de inadimplência e para tomar ações apropriadas em relação a cobranças vencidas.")
    @APIResponse(responseCode = "200", description = "Lista de cobranças obtida com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response listarCobrancasVencidas() {
        try {
            LOG.info("Listando cobranças Pix vencidas e não pagas");

            // Obter cobranças vencidas
            List<PixComVencimento> cobrancas = pixService.listarCobrancasVencimentoVencidas(LocalDate.now());

            // Transformar lista em array JSON
            JsonObject resultado = new JsonObject();
            JsonArray listaCobrancas = new JsonArray();

            for (PixComVencimento pix : cobrancas) {
                listaCobrancas.add(pixService.criarJsonDePixVencimento(pix));
            }

            resultado.put("quantidade", cobrancas.size());
            resultado.put("cobrancas", listaCobrancas);

            return Response.ok(resultado.encode()).build();
        } catch (Exception e) {
            LOG.error("Erro ao listar cobranças vencidas", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Atualiza uma cobrança Pix com vencimento existente
     * 
     * @param txid  ID da transação
     * @param dados Dados atualizados da cobrança
     * @return Resultado da operação
     */
    @PATCH
    @Path("/cobranca/{txid}")
    @Operation(summary = "Atualiza uma cobrança Pix com vencimento existente", description = "Este endpoint permite atualizar os dados de uma cobrança Pix com vencimento que ainda não foi paga. "
            +
            "É possível modificar valores, datas de vencimento, informações do devedor e outros dados. " +
            "A atualização é feita tanto na API do PSP quanto no registro local.")
    @APIResponse(responseCode = "200", description = "Cobrança atualizada com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "404", description = "Cobrança não encontrada", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "409", description = "Cobrança já foi paga e não pode ser atualizada", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response atualizarCobrancaVencimento(@PathParam("txid") String txid, JsonObject dados) {
        try {
            LOG.info("Atualizando cobrança Pix com vencimento: " + txid);

            // Verificar se a cobrança existe
            PixComVencimento pixExistente = pixService.consultarCobrancaVencimentoRepository(txid);

            if (pixExistente == null) {
                LOG.warn("Cobrança com vencimento não encontrada: " + txid);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new JsonObject().put("erro", "Cobrança não encontrada").encode())
                        .build();
            }

            // Verificar se a cobrança já foi paga
            if (pixExistente.isPaga()) {
                LOG.warn("Tentativa de atualizar cobrança já paga: " + txid);
                return Response.status(Response.Status.CONFLICT)
                        .entity(new JsonObject().put("erro", "Não é possível atualizar uma cobrança já paga").encode())
                        .build();
            }

            // Atualizar dados da cobrança com base no JSON recebido
            if (dados.containsKey("valor")) {
                pixExistente.setValorOriginal(new BigDecimal(dados.getString("valor")));
            }

            if (dados.containsKey("dataVencimento")) {
                pixExistente.setDataVencimento(LocalDate.parse(dados.getString("dataVencimento")));
            }

            if (dados.containsKey("validadeAposVencimento")) {
                pixExistente.setValidadeAposVencimento(dados.getInteger("validadeAposVencimento"));
            }

            if (dados.containsKey("solicitacaoPagador")) {
                pixExistente.setSolicitacaoPagador(dados.getString("solicitacaoPagador"));
            }

            // Enviar atualização para a API e obter resposta
            JsonObject resultado = pixService.atualizarCobrancaVencimento(txid, pixExistente, false);

            return Response.ok(resultado.encode()).build();
        } catch (Exception e) {
            LOG.error("Erro ao atualizar cobrança com vencimento", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }
}