package org.acme.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.acme.config.PixConfig;
import org.acme.model.PixImediato;
import org.acme.model.PixInfoAdicional;
import org.acme.repository.PixImediatoRepository;
import org.jboss.logging.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Serviço responsável pela criação e gerenciamento de cobranças Pix
 */
@ApplicationScoped
public class PixService {

    private static final Logger LOG = Logger.getLogger(PixService.class);

    @Inject
    PixImediatoRepository pixImediatoRepository;

    @Inject
    TokenService tokenService;

    @Inject
    PixConfig pixConfig;

    /**
     * Cria uma cobrança Pix imediata usando a API do Banco do Brasil
     * 
     * @param pixImediato Objeto com os dados da cobrança
     * @return Resultado da operação com os detalhes da cobrança criada
     * @throws Exception Se ocorrer algum erro na criação da cobrança
     */
    @Transactional
    public JsonObject criarCobrancaPix(PixImediato pixImediato) throws Exception {
        LOG.info("Iniciando criação de cobrança Pix com TxID: " + pixImediato.getTxid());

        // Obter token de acesso
        String accessToken = tokenService.getAccessToken();
        LOG.info("Access Token obtido com sucesso! " + accessToken);

        // Criar o cliente HTTP
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        // Criar o JSON de cobrança PIX
        JsonObject cobrancaJson = criarJsonCobranca(pixImediato);
        LOG.info("JSON da cobrança: " + cobrancaJson.encode());

        LOG.info("TxId: " + pixImediato.getTxid());
        LOG.info("URL base: " + pixConfig.getPixUrl());
        LOG.info("App Key: " + pixConfig.getAppKey());

        // Adicionar o parâmetro gw-dev-app-key como query parameter
        String urlCompleta = pixConfig.getPixUrl() + pixImediato.getTxid() + "?gw-dev-app-key=" + pixConfig.getAppKey();
        System.out.println("URL completa: " + urlCompleta);

        // Construir a requisição HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlCompleta))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(cobrancaJson.encode()))
                .build();

        // Enviar a requisição e obter a resposta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        LOG.info("Resposta da API: " + response.statusCode() + " - " + response.body());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            // Processar resposta de sucesso e atualizar o objeto PixImediato
            JsonObject jsonResponse = new JsonObject(response.body());

            // Atualizar dados do pix com base na resposta
            atualizarDadosPix(pixImediato, jsonResponse);

            // Persistir o objeto no banco de dados
            persistirPixImediato(pixImediato);

            LOG.info("Cobrança Pix criada com sucesso: " + pixImediato.getTxid());
            return jsonResponse;
        } else {
            LOG.error("Falha na criação da cobrança Pix. Código: " + response.statusCode() + ", Resposta: "
                    + response.body());
            throw new RuntimeException("Falha na criação da cobrança Pix. Código: " +
                    response.statusCode() + ", Resposta: " + response.body());
        }
    }

    /**
     * Cria o objeto JSON para envio na requisição de criação de cobrança
     * 
     * @param pixImediato Objeto com os dados da cobrança
     * @return Objeto JSON formatado de acordo com a API
     */
    private JsonObject criarJsonCobranca(PixImediato pixImediato) {
        JsonObject cobranca = new JsonObject();

        // Adicionar objeto calendario
        JsonObject calendario = new JsonObject()
                .put("expiracao", pixImediato.getExpiracao());
        cobranca.put("calendario", calendario);

        // Adicionar objeto devedor
        JsonObject devedor = new JsonObject();

        if (pixImediato.getCnpj() != null && !pixImediato.getCnpj().isEmpty()) {
            devedor.put("cnpj", pixImediato.getCnpj());
        } else if (pixImediato.getCpf() != null && !pixImediato.getCpf().isEmpty()) {
            devedor.put("cpf", pixImediato.getCpf());
        }

        devedor.put("nome", pixImediato.getNome());
        cobranca.put("devedor", devedor);

        // Adicionar objeto valor
        JsonObject valor = new JsonObject()
                .put("original", pixImediato.getValorOriginal().toString());
        cobranca.put("valor", valor);

        // Adicionar chave PIX (pode ser CPF, CNPJ, telefone, email ou EVP)
        cobranca.put("chave", pixImediato.getChave());

        // Adicionar solicitação ao pagador
        if (pixImediato.getSolicitacaoPagador() != null) {
            cobranca.put("solicitacaoPagador", pixImediato.getSolicitacaoPagador());
        }

        // Adicionar informações adicionais
        if (pixImediato.getInfoAdicionais() != null && !pixImediato.getInfoAdicionais().isEmpty()) {
            JsonArray infoAdicionais = new JsonArray();

            for (PixInfoAdicional info : pixImediato.getInfoAdicionais()) {
                JsonObject infoObj = new JsonObject()
                        .put("nome", info.getNome())
                        .put("valor", info.getValor());
                infoAdicionais.add(infoObj);
            }

            cobranca.put("infoAdicionais", infoAdicionais);
        }

        return cobranca;
    }

    /**
     * Atualiza os dados do PixImediato com as informações retornadas pela API
     * 
     * @param pixImediato Objeto a ser atualizado
     * @param response    Resposta da API
     */
    private void atualizarDadosPix(PixImediato pixImediato, JsonObject response) {
        // Atualizar dados de location
        if (response.containsKey("location")) {
            pixImediato.setLocation(response.getString("location"));
        }

        // Atualizar copia e cola
        if (response.containsKey("pixCopiaECola")) {
            pixImediato.setPixCopiaECola(response.getString("pixCopiaECola"));
        }

        // Atualizar revisão
        if (response.containsKey("revisao")) {
            pixImediato.setRevisao(response.getInteger("revisao"));
        }

        // Atualizar status caso seja diferente de "ATIVA"
        if (response.containsKey("status") && !response.getString("status").equals("ATIVA")) {
            pixImediato.setStatus(response.getString("status"));
        }
    }

    /**
     * Persiste o objeto PixImediato no banco de dados
     * 
     * @param pixImediato Objeto a ser persistido
     */
    @Transactional
    protected void persistirPixImediato(PixImediato pixImediato) {
        // Buscar por txid como um campo normal, não como ID
        PixImediato pixExistente = pixImediatoRepository.findByTxId(pixImediato.getTxid());

        if (pixExistente == null) {
            // Novo registro
            LOG.debug("Persistindo nova cobrança Pix: " + pixImediato.getTxid());
            pixImediatoRepository.persist(pixImediato);
        } else {
            // Atualização
            LOG.debug("Atualizando cobrança Pix existente: " + pixImediato.getTxid());
            // Garantir que o ID do objeto a ser mesclado seja o mesmo do existente
            pixImediato.setId(pixExistente.getId()); 
            pixImediatoRepository.persist(pixImediato);
        }
    }

    /**
     * Gera um TxID único para uma cobrança Pix baseado no último ID do banco de dados
     * 
     * @return TxID válido com exatamente 34 caracteres
     */
    public String gerarTxid() {
    // Gerar txid único (entre 26-35 caracteres) sem caracteres especiais
//             // Usando apenas letras e números conforme exigido pelo Banco do Brasil
            String txid = "Teste" + System.currentTimeMillis() + "X";
//             // Garantir que tenha entre 26 e 35 caracteres
             if (txid.length() < 26) {
                 // Preencher com zeros à direita se for muito curto
                 txid = txid + "0".repeat(26 - txid.length());
             } else if (txid.length() > 35) {
                 // Cortar se for muito longo
                 txid = txid.substring(0, 35);
             }
             return txid;
    //     // Obter o último ID do banco de dados usando o método do repositório
    //     Long ultimoId = pixImediatoRepository.obterUltimoId();
        
    //     // Incrementar o ID
    //     Long novoId = ultimoId + 1;
        
    //     // Prefixo "PIX"
    //     String prefixo = "PIX";
        
    //     // Converter novoId para string
    //     String idStr = novoId.toString();
        
    //     // Calcular quantos zeros são necessários (o tamanho final deve ser 34)
    //     int tamanhoAtual = prefixo.length() + idStr.length();
    //     int zerosNecessarios = 34 - tamanhoAtual;
        
    //     // Garantir que temos espaço suficiente para zeros (pelo menos 0)
    //     if (zerosNecessarios < 0) {
    //         LOG.warn("ID muito grande para gerar txid com 34 caracteres. Truncando ID.");
    //         // Se o ID for muito grande, vamos truncar
    //         idStr = idStr.substring(0, idStr.length() + zerosNecessarios);
    //         zerosNecessarios = 0;
    //     }
        
    //     // Construir o txid final: PIX + zeros + id
    //     String txid = prefixo + "0".repeat(zerosNecessarios) + idStr;
        
    //     LOG.debug("TxID gerado: " + txid + " (baseado no ID: " + novoId + ")");
    //     return txid;
     }


    /**
     * Consulta uma cobrança Pix pelo TxID no meu banco de dados (via Repository)
     * 
     * @param txid ID da transação
     * @return Objeto PixImediato se encontrado, null caso contrário
     */
    public PixImediato consultarCobranca(String txid) {
        try {
            LOG.debug("Consultando cobrança Pix com TxID: " + txid);
            return pixImediatoRepository.findByTxId(txid);
        } catch (Exception e) {
            LOG.error("Erro ao consultar cobrança Pix", e);
            return null;
        }
    }

    /**
     * Lista as cobranças Pix mais recentes
     * 
     * @param limite Número máximo de registros a retornar
     * @return Lista de cobranças
     */
    public List<PixImediato> listarCobrancasRecentes(int limite) {
        LOG.debug("Listando " + limite + " cobranças Pix mais recentes");
        return pixImediatoRepository.find("ORDER BY criacao DESC")
                .page(0, limite)
                .list();
    }

    /**
     * Consulta o status de uma cobrança Pix diretamente na API do banco
     * 
     * @param txid ID da transação
     * @return Objeto JSON com os dados atualizados da cobrança
     * @throws Exception Se ocorrer algum erro na consulta
     */
    public JsonObject consultarCobrancaNoServidor(String txid) throws Exception {
        LOG.info("Consultando status da cobrança Pix na API: " + txid);

        // Obter token de acesso
        String accessToken = tokenService.getAccessToken();

        // Criar o cliente HTTP
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        // Adicionar o parâmetro gw-dev-app-key como query parameter
        String urlCompleta = pixConfig.getPixUrl() + txid + "?gw-dev-app-key=" + pixConfig.getAppKey();

        // Construir a requisição HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlCompleta))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        // Enviar a requisição e obter a resposta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonObject jsonResponse = new JsonObject(response.body());

            // Atualizar objeto no banco de dados se ele existir
            PixImediato pixImediato = consultarCobranca(txid);
            if (pixImediato != null) {
                atualizarDadosPix(pixImediato, jsonResponse);
                persistirPixImediato(pixImediato);
            }

            return jsonResponse;
        } else {
            LOG.error("Falha na consulta da cobrança Pix. Código: " + response.statusCode() + ", Resposta: "
                    + response.body());
            throw new RuntimeException("Falha na consulta da cobrança Pix. Código: " +
                    response.statusCode() + ", Resposta: " + response.body());
        }
    }

    /**
     * Persiste uma cobrança Pix no banco de dados
     * 
     * @param pixImediato Cobrança a ser persistida
     */
    @Transactional
    public void persistirCobranca(PixImediato pixImediato) {
        persistirPixImediato(pixImediato);
    }

    /**
     * Gera uma imagem de QR Code a partir de um texto
     * 
     * @param texto Texto para gerar o QR Code (Pix Copia e Cola)
     * @return Array de bytes com a imagem PNG do QR Code
     * @throws Exception Se ocorrer algum erro na geração
     */
    public byte[] gerarQrCodeImage(String texto) throws Exception {
        // Importações e implementação da geração de QR Code
        // Nota: É necessário adicionar dependências como zxing no pom.xml
        // Exemplo simples, sem implementação completa:

        /*
         * QRCodeWriter qrCodeWriter = new QRCodeWriter();
         * BitMatrix bitMatrix = qrCodeWriter.encode(texto, BarcodeFormat.QR_CODE, 300,
         * 300);
         * 
         * ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
         * MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
         * 
         * return pngOutputStream.toByteArray();
         */

        // Implementação temporária para exemplo (não funcional):
        LOG.info("Gerando QR Code para: " + texto);
        throw new UnsupportedOperationException("Implementação da geração de QR Code não disponível. " +
                "Adicione a dependência zxing-core e zxing-javase ao projeto e implemente este método.");
    }

    /**
     * Consulta detalhes completos de uma cobrança Pix na API do banco
     * Inclui informações de pagamento e devolução quando disponíveis
     * 
     * @param txid ID da transação
     * @return Objeto JSON com os detalhes completos da cobrança
     * @throws Exception Se ocorrer algum erro na consulta
     */
    public JsonObject consultarDetalhesCobranca(String txid) throws Exception {
        LOG.info("Consultando detalhes completos da cobrança Pix na API: " + txid);

        // Obter token de acesso
        String accessToken = tokenService.getAccessToken();

        // Criar o cliente HTTP
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        // Adicionar o parâmetro gw-dev-app-key como query parameter
        String urlCompleta = pixConfig.getPixUrl() + txid + "?gw-dev-app-key=" + pixConfig.getAppKey();

        // Construir a requisição HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlCompleta))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        // Enviar a requisição e obter a resposta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        LOG.debug("Status da consulta de detalhes: " + response.statusCode());

        // Processar a resposta
        JsonObject resultado = new JsonObject(response.body());

        // Atualizar cache local se a consulta for bem-sucedida
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            atualizarCacheLocal(resultado);
        }

        return resultado;
    }

    /**
     * Verifica o status atual do pagamento de uma cobrança Pix
     * 
     * @param txid ID da transação
     * @return Objeto JSON com o status e informações do pagamento
     * @throws Exception Se ocorrer algum erro na verificação
     */
    public JsonObject verificarStatusPagamento(String txid) throws Exception {
        LOG.info("Verificando status de pagamento da cobrança: " + txid);

        // Consultar detalhes completos na API
        JsonObject detalhesCobranca = consultarDetalhesCobranca(txid);

        // Preparar objeto de resposta
        JsonObject statusPagamento = new JsonObject();
        statusPagamento.put("txid", txid);

        // Obter status da cobrança
        String status = detalhesCobranca.getString("status", "N/A");
        statusPagamento.put("status", status);

        // Adicionar informações do valor original
        if (detalhesCobranca.containsKey("valor")) {
            JsonObject valor = detalhesCobranca.getJsonObject("valor");
            statusPagamento.put("valorOriginal", valor.getString("original", "0"));
        }

        // Verificar se o Pix foi pago
        boolean pago = false;

        if (detalhesCobranca.containsKey("pix") && !detalhesCobranca.getJsonArray("pix").isEmpty()) {
            pago = true;
            JsonObject pixInfo = detalhesCobranca.getJsonArray("pix").getJsonObject(0);

            statusPagamento.put("pago", true);
            statusPagamento.put("endToEndId", pixInfo.getString("endToEndId", "N/A"));
            statusPagamento.put("valorPago", pixInfo.getString("valor", "0"));
            statusPagamento.put("horarioPagamento", pixInfo.getString("horario", "N/A"));

            if (pixInfo.containsKey("infoPagador")) {
                statusPagamento.put("infoPagador", pixInfo.getString("infoPagador", ""));
            }
        } else {
            statusPagamento.put("pago", false);

            // Adicionar informações de expiração para cobranças ativas
            if (status.equals("ATIVA") && detalhesCobranca.containsKey("calendario")) {
                JsonObject calendario = detalhesCobranca.getJsonObject("calendario");
                if (calendario.containsKey("expiracao")) {
                    statusPagamento.put("expiracao", calendario.getInteger("expiracao"));
                }
            }
        }

        // Atualizar o objeto local se necessário
        if (pago) {
            atualizarRegistroPagamento(txid, statusPagamento);
        }

        return statusPagamento;
    }

    /**
     * Consulta informações de devolução para um Pix específico
     * 
     * @param txid ID da transação
     * @return Objeto JSON com detalhes da devolução ou status indicando que não há
     *         devolução
     * @throws Exception Se ocorrer algum erro na consulta
     */
    public JsonObject consultarDevolucao(String txid) throws Exception {
        LOG.info("Consultando devolução da cobrança: " + txid);

        // Consultar detalhes completos na API
        JsonObject detalhesCobranca = consultarDetalhesCobranca(txid);

        // Preparar objeto de resposta
        JsonObject resultado = new JsonObject();
        resultado.put("txid", txid);

        // Verificar se o Pix foi pago
        if (!detalhesCobranca.containsKey("pix") || detalhesCobranca.getJsonArray("pix").isEmpty()) {
            resultado.put("status", "NAO_PAGO");
            resultado.put("mensagem", "Cobrança não foi paga, portanto não possui devolução");
            return resultado;
        }

        // Obter informações do pagamento
        JsonObject pixInfo = detalhesCobranca.getJsonArray("pix").getJsonObject(0);

        // Verificar se há devoluções
        if (!pixInfo.containsKey("devolucoes") || pixInfo.getJsonArray("devolucoes").isEmpty()) {
            resultado.put("status", "SEM_DEVOLUCAO");
            resultado.put("mensagem", "Pagamento realizado, mas sem devolução registrada");
            return resultado;
        }

        // Processar informações de devolução
        JsonObject devolucao = pixInfo.getJsonArray("devolucoes").getJsonObject(0);

        resultado.put("status", "COM_DEVOLUCAO");
        resultado.put("idDevolucao", devolucao.getString("id", "N/A"));
        resultado.put("valor", devolucao.getString("valor", "0"));
        resultado.put("statusDevolucao", devolucao.getString("status", "N/A"));

        if (devolucao.containsKey("motivo")) {
            resultado.put("motivo", devolucao.getString("motivo", ""));
        }

        if (devolucao.containsKey("horario")) {
            JsonObject horario = devolucao.getJsonObject("horario");
            if (horario.containsKey("solicitacao")) {
                resultado.put("horarioSolicitacao", horario.getString("solicitacao", ""));
            }
            if (horario.containsKey("liquidacao")) {
                resultado.put("horarioLiquidacao", horario.getString("liquidacao", ""));
            }
        }

        return resultado;
    }

    /**
     * Atualiza o cache local com informações obtidas da API
     * 
     * @param resultado Dados da cobrança obtidos da API
     */
    private void atualizarCacheLocal(JsonObject resultado) {
        try {
            // Verificar se contém txid para identificação
            if (!resultado.containsKey("txid")) {
                LOG.warn("Resposta sem txid para atualização do cache local");
                return;
            }

            String txid = resultado.getString("txid");
            PixImediato pixImediato = consultarCobranca(txid);

            // Criar novo registro se não existir
            if (pixImediato == null) {
                LOG.info("Criando novo registro local para cobrança: " + txid);

                // Extrair dados básicos
                String status = resultado.getString("status", "DESCONHECIDO");
                BigDecimal valor = BigDecimal.ZERO;

                if (resultado.containsKey("valor") && resultado.getJsonObject("valor").containsKey("original")) {
                    valor = new BigDecimal(resultado.getJsonObject("valor").getString("original"));
                }

                // Extrair informações do devedor
                String nome = "Desconhecido";
                String cpf = null;
                String cnpj = null;

                if (resultado.containsKey("devedor")) {
                    JsonObject devedor = resultado.getJsonObject("devedor");
                    nome = devedor.getString("nome", nome);
                    cpf = devedor.getString("cpf", null);
                    cnpj = devedor.getString("cnpj", null);
                }

                // Criar objeto com dados mínimos
                pixImediato = new PixImediato(txid,
                        resultado.getString("chave", ""),
                        valor,
                        nome,
                        cpf,
                        cnpj,
                        3600); // Expiração padrão

                pixImediato.setStatus(status);

                // Adicionar location e informações de copia e cola
                if (resultado.containsKey("pixCopiaECola")) {
                    pixImediato.setPixCopiaECola(resultado.getString("pixCopiaECola"));
                }

                if (resultado.containsKey("location")) {
                    pixImediato.setLocation(resultado.getString("location"));
                }
            } else {
                LOG.debug("Atualizando registro existente: " + txid);

                // Atualizar status
                if (resultado.containsKey("status")) {
                    pixImediato.setStatus(resultado.getString("status"));
                }
            }

            // Verificar se há informações de pagamento
            if (resultado.containsKey("pix") && !resultado.getJsonArray("pix").isEmpty()) {
                JsonObject pixInfo = resultado.getJsonArray("pix").getJsonObject(0);

                // Registrar pagamento se ainda não estiver registrado
                if (!pixImediato.isPaga()) {
                    LOG.info("Registrando pagamento detectado para: " + txid);

                    String endToEndId = pixInfo.getString("endToEndId", "API" + System.currentTimeMillis());
                    BigDecimal valorPago = new BigDecimal(
                            pixInfo.getString("valor", pixImediato.getValorOriginal().toString()));
                    String infoPagador = pixInfo.getString("infoPagador", "");

                    pixImediato.registrarPagamento(endToEndId, valorPago, infoPagador);
                }
            }

            // Persistir as mudanças
            persistirCobranca(pixImediato);

        } catch (Exception e) {
            LOG.error("Erro ao atualizar cache local com dados da API", e);
        }
    }

    /**
     * Atualiza o registro local de pagamento com informações da API
     * 
     * @param txid            ID da transação
     * @param statusPagamento Informações de status do pagamento
     */
    private void atualizarRegistroPagamento(String txid, JsonObject statusPagamento) {
        try {
            PixImediato pixImediato = consultarCobranca(txid);

            if (pixImediato == null) {
                LOG.warn("Não foi possível atualizar registro de pagamento - cobrança não encontrada: " + txid);
                return;
            }

            // Verificar se já está registrado como pago
            if (pixImediato.isPaga()) {
                LOG.debug("Pagamento já registrado localmente para: " + txid);
                return;
            }

            // Registrar pagamento
            LOG.info("Registrando pagamento para cobrança: " + txid);

            String endToEndId = statusPagamento.getString("endToEndId", "API" + System.currentTimeMillis());
            BigDecimal valorPago = new BigDecimal(
                    statusPagamento.getString("valorPago", pixImediato.getValorOriginal().toString()));
            String infoPagador = statusPagamento.getString("infoPagador", "");

            pixImediato.registrarPagamento(endToEndId, valorPago, infoPagador);
            persistirCobranca(pixImediato);

        } catch (Exception e) {
            LOG.error("Erro ao atualizar registro de pagamento", e);
        }
    }
}