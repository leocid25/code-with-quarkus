package org.acme.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.acme.config.PixConfig;
import org.acme.model.Pix;
import org.acme.model.PixComVencimento;
import org.acme.model.PixImediato;
import org.acme.model.PixInfoAdicional;
import org.acme.repository.PixComVencimentoRepository;
import org.acme.repository.PixImediatoRepository;
import org.jboss.logging.Logger;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

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
    PixComVencimentoRepository pixComVencimentoRepository;

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

        // Testar conectividade antes de continuar
        try {
            InetAddress address = InetAddress.getByName("api.hm.bb.com.br");
            boolean reachable = address.isReachable(5000);
            LOG.info("Servidor api.hm.bb.com.br está acessível: " + reachable);
            if (!reachable) {
                LOG.warn("O servidor pode estar inacessível, mas continuando mesmo assim");
            }
        } catch (IOException e) {
            LOG.error("Erro ao verificar conectividade: " + e.getMessage(), e);
            // Continuar mesmo com erro de conectividade para ver o erro real
        }

        // Obter token de acesso
        String accessToken = tokenService.getAccessToken();
        LOG.info("Access Token obtido com sucesso! " + accessToken);

        // Criar o cliente HTTP
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // Criar o JSON de cobrança PIX
        JsonObject cobrancaJson = criarJsonCobranca(pixImediato);
        LOG.info("JSON da cobrança: " + cobrancaJson.encode());

        LOG.info("TxId: " + pixImediato.getTxid());
        LOG.info("URL base: " + pixConfig.getPixBBImediatoUrl());
        LOG.info("App Key: " + pixConfig.getAppKey());

        // Adicionar o parâmetro gw-dev-app-key como query parameter
        String urlCompleta = pixConfig.getPixBBImediatoUrl() + pixImediato.getTxid() + "?gw-dev-app-key="
                + pixConfig.getAppKey();
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
     * Cria uma cobrança Pix com vencimento usando a API do Banco do Brasil
     * 
     * @param pixVencimento Objeto com os dados da cobrança
     * @return Resultado da operação com os detalhes da cobrança criada
     * @throws Exception Se ocorrer algum erro na criação da cobrança
     */
    @Transactional
    public JsonObject criarCobrancaPixVencimento(PixComVencimento pixVencimento) throws Exception {
        LOG.info("Iniciando criação de cobrança Pix com vencimento, TxID: " + pixVencimento.getTxid());

        // Obter token de acesso
        String accessToken = tokenService.getAccessToken();
        LOG.info("Access Token obtido com sucesso!");

        // Criar o cliente HTTP
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        // Criar o JSON de cobrança PIX com vencimento
        JsonObject cobrancaJson = criarJsonCobrancaVencimento(pixVencimento);
        LOG.info("JSON da cobrança com vencimento: " + cobrancaJson.encode());

        // Adicionar o parâmetro gw-dev-app-key como query parameter
        String urlCompleta = pixConfig.getPixBBVencimentoUrl() + pixVencimento.getTxid() + "?gw-dev-app-key="
                + pixConfig.getAppKey();
        LOG.info("URL completa: " + urlCompleta);

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
            // Processar resposta de sucesso e atualizar o objeto PixComVencimento
            JsonObject jsonResponse = new JsonObject(response.body());

            // Atualizar dados do pix com base na resposta
            atualizarDadosPixVencimento(pixVencimento, jsonResponse);

            // Persistir o objeto no banco de dados
            persistirPixComVencimento(pixVencimento);

            LOG.info("Cobrança Pix com vencimento criada com sucesso: " + pixVencimento.getTxid());
            return jsonResponse;
        } else {
            LOG.error(
                    "Falha na criação da cobrança Pix com vencimento. Código: " + response.statusCode() + ", Resposta: "
                            + response.body());
            throw new RuntimeException("Falha na criação da cobrança Pix com vencimento. Código: " +
                    response.statusCode() + ", Resposta: " + response.body());
        }
    }

    /**
     * Cria o objeto JSON para envio na requisição de criação de cobrança com
     * vencimento
     * 
     * @param pixVencimento Objeto com os dados da cobrança
     * @return Objeto JSON formatado de acordo com a API
     */
    private JsonObject criarJsonCobrancaVencimento(PixComVencimento pixVencimento) {
        JsonObject cobranca = new JsonObject();

        // Adicionar objeto calendario
        JsonObject calendario = new JsonObject()
                .put("dataDeVencimento", pixVencimento.getDataVencimento().format(DateTimeFormatter.ISO_DATE));

        // Adicionar validade após vencimento, se existir
        if (pixVencimento.getValidadeAposVencimento() != null) {
            calendario.put("validadeAposVencimento", pixVencimento.getValidadeAposVencimento());
        }

        cobranca.put("calendario", calendario);

        // Adicionar objeto devedor
        JsonObject devedor = new JsonObject();

        if (pixVencimento.getCnpj() != null && !pixVencimento.getCnpj().isEmpty()) {
            devedor.put("cnpj", pixVencimento.getCnpj());
        } else if (pixVencimento.getCpf() != null && !pixVencimento.getCpf().isEmpty()) {
            devedor.put("cpf", pixVencimento.getCpf());
        }

        devedor.put("nome", pixVencimento.getNome());

        // Adicionar informações opcionais de endereço do devedor, se disponíveis
        if (pixVencimento.getLogradouro() != null && !pixVencimento.getLogradouro().isEmpty()) {
            devedor.put("logradouro", pixVencimento.getLogradouro());
        }

        if (pixVencimento.getCidade() != null && !pixVencimento.getCidade().isEmpty()) {
            devedor.put("cidade", pixVencimento.getCidade());
        }

        if (pixVencimento.getUf() != null && !pixVencimento.getUf().isEmpty()) {
            devedor.put("uf", pixVencimento.getUf());
        }

        if (pixVencimento.getCep() != null && !pixVencimento.getCep().isEmpty()) {
            devedor.put("cep", pixVencimento.getCep());
        }

        if (pixVencimento.getEmail() != null && !pixVencimento.getEmail().isEmpty()) {
            devedor.put("email", pixVencimento.getEmail());
        }

        cobranca.put("devedor", devedor);

        // Adicionar objeto valor
        JsonObject valor = new JsonObject()
                .put("original", pixVencimento.getValorOriginal().toString());

        // Adicionar informações de multa, se configuradas
        if (pixVencimento.getMultaModalidade() != null && pixVencimento.getMultaValor() != null) {
            JsonObject multa = new JsonObject()
                    .put("modalidade", pixVencimento.getMultaModalidade())
                    .put("valorPerc", pixVencimento.getMultaValor().toString());
            valor.put("multa", multa);
        }

        // Adicionar informações de juros, se configurados
        if (pixVencimento.getJurosModalidade() != null && pixVencimento.getJurosValor() != null) {
            JsonObject juros = new JsonObject()
                    .put("modalidade", pixVencimento.getJurosModalidade())
                    .put("valorPerc", pixVencimento.getJurosValor().toString());
            valor.put("juros", juros);
        }

        // Adicionar informações de abatimento, se configurado
        if (pixVencimento.getAbatimentoModalidade() != null && pixVencimento.getAbatimentoValor() != null) {
            JsonObject abatimento = new JsonObject()
                    .put("modalidade", pixVencimento.getAbatimentoModalidade())
                    .put("valorPerc", pixVencimento.getAbatimentoValor().toString());
            valor.put("abatimento", abatimento);
        }

        // Adicionar informações de desconto, se configurado
        if (pixVencimento.getDescontoModalidade() != null && pixVencimento.getDescontoValor() != null) {
            JsonObject desconto = new JsonObject()
                    .put("modalidade", pixVencimento.getDescontoModalidade())
                    .put("valorPerc", pixVencimento.getDescontoValor().toString());
            valor.put("desconto", desconto);
        }

        cobranca.put("valor", valor);

        // Adicionar chave PIX (pode ser CPF, CNPJ, telefone, email ou EVP)
        cobranca.put("chave", pixVencimento.getChave());

        // Adicionar solicitação ao pagador
        if (pixVencimento.getSolicitacaoPagador() != null) {
            cobranca.put("solicitacaoPagador", pixVencimento.getSolicitacaoPagador());
        }

        // Adicionar informações adicionais
        if (pixVencimento.getInfoAdicionais() != null && !pixVencimento.getInfoAdicionais().isEmpty()) {
            JsonArray infoAdicionais = new JsonArray();

            for (PixInfoAdicional info : pixVencimento.getInfoAdicionais()) {
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
     * Atualiza os dados do PixComVencimento com as informações retornadas pela API
     * 
     * @param pixVencimento Objeto a ser atualizado
     * @param response      Resposta da API
     */
    private void atualizarDadosPixVencimento(PixComVencimento pixVencimento, JsonObject response) {
        // Atualizar dados de location
        if (response.containsKey("location")) {
            pixVencimento.setLocation(response.getString("location"));
        }

        // Atualizar copia e cola
        if (response.containsKey("pixCopiaECola")) {
            pixVencimento.setPixCopiaECola(response.getString("pixCopiaECola"));
        }

        // Atualizar revisão
        if (response.containsKey("revisao")) {
            pixVencimento.setRevisao(response.getInteger("revisao"));
        }

        // Atualizar status caso seja diferente de "ATIVA"
        if (response.containsKey("status") && !response.getString("status").equals("ATIVA")) {
            pixVencimento.setStatus(response.getString("status"));
        }

        // Atualizar informações de recebedor se disponíveis
        if (response.containsKey("recebedor")) {
            JsonObject recebedor = response.getJsonObject("recebedor");
            if (recebedor.containsKey("nome")) {
                pixVencimento.setRecebedorNome(recebedor.getString("nome"));
            }
            if (recebedor.containsKey("cpf")) {
                pixVencimento.setRecebedorCpf(recebedor.getString("cpf"));
            }
            if (recebedor.containsKey("cnpj")) {
                pixVencimento.setRecebedorCnpj(recebedor.getString("cnpj"));
            }
        }
    }

    /**
     * Persiste o objeto PixImediato no banco de dados
     * 
     * @param pixImediato Objeto a ser persistido
     */
    @Transactional
    public void persistirPixImediato(PixImediato pixImediato) {
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
            pixImediatoRepository.getEntityManager().merge(pixImediato);
        }
    }

    /**
     * Persiste o objeto PixComVencimento no banco de dados
     * 
     * @param pixVencimento Objeto a ser persistido
     */
    @Transactional
    public void persistirPixComVencimento(PixComVencimento pixVencimento) {
        // Buscar por txid como um campo normal, não como ID
        PixComVencimento pixExistente = pixComVencimentoRepository.findByTxId(pixVencimento.getTxid());

        if (pixExistente == null) {
            // Novo registro
            LOG.debug("Persistindo nova cobrança Pix com vencimento: " + pixVencimento.getTxid());
            pixComVencimentoRepository.persist(pixVencimento);
        } else {
            // Atualização
            LOG.debug("Atualizando cobrança Pix com vencimento existente: " + pixVencimento.getTxid());
            // Garantir que o ID do objeto a ser mesclado seja o mesmo do existente
            pixVencimento.setId(pixExistente.getId());
            pixComVencimentoRepository.getEntityManager().merge(pixVencimento);
        }
    }

    /**
     * Gera um TxID de exatamente 35 caracteres, apenas letras e números.
     *
     * @return TxID válido com 35 caracteres
     */
    public String gerarTxid(String tipoCob, String codigoBanco) {
        // Gera um UUID sem hífens (32 caracteres hexadecimais)
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").toLowerCase();
        String prefixo;
        switch (tipoCob) {
            case "cob" -> {
                // Prefixo fixo, pode ser seu identificador de sistema, por exemplo
                prefixo = "cob" + codigoBanco + "i"; // Especifica o tipo de cobrança seguido pelo Banco
            }
            case "cobv" -> {
                // Prefixo fixo, pode ser seu identificador de sistema, por exemplo
                prefixo = "cobv" + codigoBanco + "i"; // Especifica o tipo de cobrança seguido pelo Banco
            }
            default -> {
                // Prefixo fixo, pode ser seu identificador de sistema, por exemplo
                prefixo = "pix"; // 3 caracteres
            }
        }

        // Timestamp simples em base 36 (mais compacto e ainda único)
        String timestamp = Long.toString(System.currentTimeMillis(), 36).toLowerCase(); // varia, ~8–9 chars

        // Junta tudo e remove o que passar dos 35 caracteres
        String raw = prefixo + uuid + timestamp; // pode ter mais de 35

        // Corta ou preenche para garantir exatamente 35 caracteres
        return raw.length() > 35 ? raw.substring(0, 35) : String.format("%-35s", raw).replace(' ', '0');
    }

    /**
     * Consulta uma cobrança Pix pelo TxID no meu banco de dados (via Repository)
     * 
     * @param txid ID da transação
     * @return Objeto PixImediato se encontrado, null caso contrário
     */
    public PixImediato consultarCobrancaRepository(String txid) {
        try {
            LOG.debug("Consultando cobrança Pix com TxID: " + txid);
            return pixImediatoRepository.findByTxId(txid);
        } catch (Exception e) {
            LOG.error("Erro ao consultar cobrança Pix", e);
            return null;
        }
    }

    /**
     * Consulta uma cobrança Pix com vencimento pelo TxID no banco de dados
     * 
     * @param txid ID da transação
     * @return Objeto PixComVencimento se encontrado, null caso contrário
     */
    public PixComVencimento consultarCobrancaVencimentoRepository(String txid) {
        try {
            LOG.debug("Consultando cobrança Pix com vencimento, TxID: " + txid);
            return pixComVencimentoRepository.findByTxId(txid);
        } catch (Exception e) {
            LOG.error("Erro ao consultar cobrança Pix com vencimento", e);
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
     * Lista as cobranças Pix com vencimento mais recentes
     * 
     * @param limite Número máximo de registros a retornar
     * @return Lista de cobranças
     */
    public List<PixComVencimento> listarCobrancasVencimentoRecentes(int limite) {
        LOG.debug("Listando " + limite + " cobranças Pix com vencimento mais recentes");
        return pixComVencimentoRepository.listarRecentes(limite);
    }

    /**
     * Lista as cobranças Pix com vencimento por período de vencimento
     * 
     * @param dataInicio Data inicial de vencimento
     * @param dataFim    Data final de vencimento
     * @return Lista de cobranças
     */
    public List<Pix> listarCobrancasVencimentoPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {

        try {
            // Chamar o métodos do repositório
            List<PixComVencimento> cobrancas = pixComVencimentoRepository.listarPorPeriodo(dataInicio,
                    dataFim);
            List<PixImediato> cobrancasI = pixImediatoRepository.listarPorPeriodo(dataInicio,
                    dataFim);

            // Criar uma lista da classe pai Pix
            List<Pix> todasCobrancas = new ArrayList<>();
            todasCobrancas.addAll(cobrancas); // Adiciona todas as cobranças com vencimento
            todasCobrancas.addAll(cobrancasI); // Adiciona todas as cobranças imediatas

            // Ordenar por data de criação (mais recentes primeiro)
            todasCobrancas.sort((a, b) -> b.getCriacao().compareTo(a.getCriacao()));

            return todasCobrancas;
        } catch (Exception e) {
            LOG.error("Erro ao listar cobranças por período: " + e.getMessage(), e);

            // Em caso de erro, retorna uma lista vazia em vez de propagar a exceção
            return Collections.emptyList();
        }
    }

    /**
     * Lista as cobranças Pix vencidas e não pagas
     * 
     * @param dataReferencia Data de referência para verificar o vencimento
     *                       (geralmente a data atual)
     * @return Lista de cobranças vencidas e não pagas
     */
    public List<PixComVencimento> listarCobrancasVencimentoVencidas(LocalDate dataReferencia) {
        LOG.debug("Listando cobranças Pix vencidas até " + dataReferencia);
        return pixComVencimentoRepository.listarVencidasNaoPagas(dataReferencia);
    }

    /**
     * Gera uma imagem de QR Code a partir de um texto
     * 
     * @param pixCopiaECola Texto Pix Copia e Cola para gerar o QR Code
     * @return Array de bytes com a imagem PNG do QR Code
     * @throws Exception Se ocorrer algum erro na geração
     */
    public byte[] gerarQrCodeImage(String pixCopiaECola) throws Exception {
        LOG.info("Gerando QR Code para Pix Copia e Cola");

        // Validar se o texto Pix Copia e Cola foi fornecido
        if (pixCopiaECola == null || pixCopiaECola.trim().isEmpty()) {
            throw new IllegalArgumentException("Texto Pix Copia e Cola não fornecido");
        }

        int width = 300; // Largura do QR Code em pixels
        int height = 300; // Altura do QR Code em pixels
        String fileType = "png";

        // Configuração para correção de erros do QR Code
        HashMap<EncodeHintType, Object> hintMap = new HashMap<>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // Nível médio de correção de erros
        hintMap.put(EncodeHintType.MARGIN, 2); // Margem do QR Code
        hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8"); // Codificação de caracteres

        try {
            // Gerar a matriz de bits do QR Code
            BitMatrix matrix = new MultiFormatWriter().encode(
                    pixCopiaECola,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    hintMap);

            // Converter a matriz em bytes de imagem
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, fileType, pngOutputStream);

            return pngOutputStream.toByteArray();

        } catch (WriterException | IOException e) {
            LOG.error("Erro ao gerar QR Code: " + e.getMessage(), e);
            throw new Exception("Falha na geração do QR Code: " + e.getMessage(), e);
        }
    }

    /**
     * Gera o texto para o QR Code PIX (PIX Copia e Cola)
     * 
     * @param pix Objeto Pix contendo os dados da cobrança
     * @return String contendo o texto formatado para o QR Code PIX
     */
    // public String gerarTextoQrCodePix(Pix pix) {
    // // Preencher os campos obrigatórios
    // Integer chaveLength = pix.getChave().length();
    // Integer merchantNameFieldSize = (pix.getRecebedorNome() != null) ?
    // pix.getRecebedorNome().length() : 0;

    // // Limitar o nome do recebedor a 25 caracteres
    // String nomeRecebedor = (pix.getRecebedorNome() != null) ?
    // pix.getRecebedorNome() : "";
    // if (nomeRecebedor.length() > 25) {
    // nomeRecebedor = nomeRecebedor.substring(0, 25);
    // merchantNameFieldSize = 25;
    // }

    // // Formatar o tamanho do nome do recebedor (2 dígitos)
    // String nomeRecebedorSizeStr = String.format("%02d", merchantNameFieldSize);

    // // Cidade do recebedor (máximo 15 caracteres)
    // String cidade = "BRASILIA"; // Valor padrão, substitua se disponível no
    // objeto pix
    // if (pix.getCidade() != null && !pix.getCidade().isEmpty()) {
    // cidade = pix.getCidade();
    // if (cidade.length() > 15) {
    // cidade = cidade.substring(0, 15);
    // }
    // }
    // String cidadeSizeStr = String.format("%02d", cidade.length());

    // // Valor do pagamento
    // String valorStr = pix.getValorOriginal().toString();
    // String valorSizeStr = String.format("%02d", valorStr.length());

    // // Gerar TxID
    // String txId = pix.getTxid();

    // // Tamanho do campo merchant account information
    // Integer merchantAccountInfoSize = chaveLength + 8 + 14; // 8 para os
    // subcampos e 14 é o tamanho de
    // // "br.gov.bcb.pix"

    // // Construir o payload
    // StringBuilder payload = new StringBuilder();

    // // Payload Format Indicator (ID: 00): obrigatório, valor fixo: 01
    // payload.append("00").append("02").append("01");

    // // Merchant Account Information (ID: 26): obrigatório para PIX
    // payload.append("26")
    // .append(String.format("%02d", merchantAccountInfoSize))
    // .append("0014").append("br.gov.bcb.pix")
    // .append("01").append(String.format("%02d", chaveLength))
    // .append(pix.getChave());

    // // Merchant Category Code (ID: 52): obrigatório, valor fixo: 0000
    // payload.append("52").append("04").append("0000");

    // // Transaction Currency (ID: 53): obrigatório, valor fixo para Real: 986
    // payload.append("53").append("03").append("986");

    // // Transaction Amount (ID: 54): obrigatório se valor fixo
    // payload.append("54").append(valorSizeStr).append(valorStr);

    // // Country Code (ID: 58): obrigatório, valor fixo: BR
    // payload.append("58").append("02").append("BR");

    // // Merchant Name (ID: 59): obrigatório
    // payload.append("59").append(nomeRecebedorSizeStr).append(nomeRecebedor);

    // // Merchant City (ID: 60): obrigatório
    // payload.append("60").append(cidadeSizeStr).append(cidade);

    // // Additional Data Field (ID: 62): opcional, usado para o TxID
    // Integer txIdSize = txId.length();
    // Integer additionalDataFieldSize = txIdSize + 4; // 4 para o subcampo

    // payload.append("62")
    // .append(String.format("%02d", additionalDataFieldSize))
    // .append("05").append(String.format("%02d", txIdSize))
    // .append(txId);

    // // CRC16 (ID: 63): obrigatório, 4 caracteres
    // payload.append("6304");

    // // Calcular o CRC-16
    // String payloadWithoutCrc = payload.toString();
    // int crc = calcularCRC16(payloadWithoutCrc);

    // // Adicionar o CRC-16 calculado
    // String payloadFinal = payloadWithoutCrc + String.format("%04X", crc);

    // return payloadFinal;
    // }

    /**
     * Calcula o CRC-16 (CCITT) para o texto do QR Code
     * 
     * @param data String para calcular o CRC
     * @return valor do CRC-16
     */
    // private int calcularCRC16(String data) {
    // int crc = 0xFFFF; // Valor inicial do CRC16

    // for (int i = 0; i < data.length(); i++) {
    // char c = data.charAt(i);
    // crc ^= (c << 8);

    // for (int j = 0; j < 8; j++) {
    // if ((crc & 0x8000) != 0) {
    // crc = (crc << 1) ^ 0x1021;
    // } else {
    // crc = crc << 1;
    // }
    // }
    // }

    // return crc & 0xFFFF; // Garante que o resultado seja um valor de 16 bits
    // }

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
        String urlCompleta = pixConfig.getPixBBImediatoUrl() + txid + "?gw-dev-app-key=" + pixConfig.getAppKey();

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
        String urlCompleta = pixConfig.getPixBBImediatoUrl() + txid + "?gw-dev-app-key=" + pixConfig.getAppKey();

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
            PixImediato pixImediato = consultarCobrancaRepository(txid);
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
     * Consulta o status de uma cobrança Pix com vencimento na API do banco
     * 
     * @param txid ID da transação
     * @return Objeto JSON com os dados atualizados da cobrança
     * @throws Exception Se ocorrer algum erro na consulta
     */
    public JsonObject consultarCobrancaVencimentoNoServidor(String txid) throws Exception {
        LOG.info("Consultando status da cobrança Pix com vencimento na API: " + txid);

        // Obter token de acesso
        String accessToken = tokenService.getAccessToken();

        // Criar o cliente HTTP
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        // Adicionar o parâmetro gw-dev-app-key como query parameter
        String urlCompleta = pixConfig.getPixBBVencimentoUrl() + txid + "?gw-dev-app-key=" + pixConfig.getAppKey();

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
            PixComVencimento pixVencimento = consultarCobrancaVencimentoRepository(txid);
            if (pixVencimento != null) {
                atualizarDadosPixVencimento(pixVencimento, jsonResponse);
                persistirPixComVencimento(pixVencimento);
            }

            return jsonResponse;
        } else {
            LOG.error("Falha na consulta da cobrança Pix com vencimento. Código: " + response.statusCode()
                    + ", Resposta: "
                    + response.body());
            throw new RuntimeException("Falha na consulta da cobrança Pix com vencimento. Código: " +
                    response.statusCode() + ", Resposta: " + response.body());
        }
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
            PixImediato pixImediato = consultarCobrancaRepository(txid);

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
            persistirPixImediato(pixImediato);

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
            PixImediato pixImediato = consultarCobrancaRepository(txid);

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
            persistirPixImediato(pixImediato);

        } catch (Exception e) {
            LOG.error("Erro ao atualizar registro de pagamento", e);
        }
    }

    /**
     * Atualiza ou cancela uma cobrança Pix com vencimento existente
     * 
     * @param txid          ID da transação
     * @param pixVencimento Objeto com os dados atualizados
     * @param cancelar      Flag indicando se deve cancelar a cobrança em vez de
     *                      atualizá-la
     * @return Resultado da operação
     * @throws Exception Se ocorrer algum erro na atualização
     */
    @Transactional
    public JsonObject atualizarCobrancaVencimento(String txid, PixComVencimento pixVencimento, boolean cancelar)
            throws Exception {
        LOG.info("Atualizando cobrança Pix com vencimento, TxID: " + txid);

        // Verificar se a cobrança existe
        PixComVencimento pixExistente = consultarCobrancaVencimentoRepository(txid);
        if (pixExistente == null) {
            throw new RuntimeException("Cobrança Pix com vencimento não encontrada: " + txid);
        }

        // Verificar se a cobrança já foi paga
        if (pixExistente.isPaga()) {
            throw new RuntimeException("Não é possível atualizar uma cobrança que já foi paga");
        }

        // Obter token de acesso
        String accessToken = tokenService.getAccessToken();

        // Criar o cliente HTTP
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        // Criar JSON para atualização ou cancelamento
        JsonObject requestJson;

        if (cancelar) {
            // Para cancelar, enviamos apenas o status
            requestJson = new JsonObject()
                    .put("status", "REMOVIDA_PELO_USUARIO_RECEBEDOR");
        } else {
            // Para atualizar, enviamos todos os dados atualizados
            requestJson = criarJsonCobrancaVencimento(pixVencimento);
        }

        // Adicionar o parâmetro gw-dev-app-key como query parameter
        String urlCompleta = pixConfig.getPixBBVencimentoUrl() + txid + "?gw-dev-app-key=" + pixConfig.getAppKey();

        // Construir a requisição HTTP (PATCH para atualizar parcialmente)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlCompleta))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(requestJson.encode()))
                .build();

        // Enviar a requisição e obter a resposta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        LOG.info("Resposta da API: " + response.statusCode() + " - " + response.body());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonObject jsonResponse = new JsonObject(response.body());

            // Atualizar dados locais
            if (cancelar) {
                pixExistente.cancelar();
                persistirPixComVencimento(pixExistente);
            } else {
                atualizarDadosPixVencimento(pixVencimento, jsonResponse);
                persistirPixComVencimento(pixVencimento);
            }

            return jsonResponse;
        } else {
            LOG.error("Falha na atualização da cobrança Pix com vencimento. Código: " + response.statusCode()
                    + ", Resposta: "
                    + response.body());
            throw new RuntimeException("Falha na atualização da cobrança Pix com vencimento. Código: " +
                    response.statusCode() + ", Resposta: " + response.body());
        }
    }

    /**
     * Converte um objeto PixComVencimento para JSON para retorno na API
     * 
     * @param pix Objeto PixComVencimento
     * @return Objeto JSON com os dados formatados
     */
    public JsonObject criarJsonDePixVencimento(PixComVencimento pix) {
        JsonObject json = new JsonObject();

        json.put("txid", pix.getTxid());
        json.put("status", pix.getStatus());
        json.put("chave", pix.getChave());
        json.put("valor", pix.getValorOriginal().toString());
        json.put("nome", pix.getNome());
        json.put("tipoCob", "cobv");
        json.put("dataVencimento", pix.getDataVencimento().toString());
        json.put("validadeAposVencimento", pix.getValidadeAposVencimento());

        if (pix.getCpf() != null && !pix.getCpf().isEmpty()) {
            json.put("cpf", pix.getCpf());
        }

        if (pix.getCnpj() != null && !pix.getCnpj().isEmpty()) {
            json.put("cnpj", pix.getCnpj());
        }

        // Incluir dados do endereço se disponíveis
        if (pix.getLogradouro() != null && !pix.getLogradouro().isEmpty()) {
            json.put("logradouro", pix.getLogradouro());

            if (pix.getCidade() != null)
                json.put("cidade", pix.getCidade());
            if (pix.getUf() != null)
                json.put("uf", pix.getUf());
            if (pix.getCep() != null)
                json.put("cep", pix.getCep());
        }

        if (pix.getEmail() != null) {
            json.put("email", pix.getEmail());
        }

        json.put("criacao", pix.getCriacao().toString());

        // Adicionar informações de multa e juros se disponíveis
        if (pix.getMultaModalidade() != null && pix.getMultaValor() != null) {
            JsonObject multa = new JsonObject()
                    .put("modalidade", pix.getMultaModalidade())
                    .put("valor", pix.getMultaValor().toString());
            json.put("multa", multa);
        }

        if (pix.getJurosModalidade() != null && pix.getJurosValor() != null) {
            JsonObject juros = new JsonObject()
                    .put("modalidade", pix.getJurosModalidade())
                    .put("valor", pix.getJurosValor().toString());
            json.put("juros", juros);
        }

        if (pix.getAbatimentoModalidade() != null && pix.getAbatimentoValor() != null) {
            JsonObject abatimento = new JsonObject()
                    .put("modalidade", pix.getAbatimentoModalidade())
                    .put("valor", pix.getAbatimentoValor().toString());
            json.put("abatimento", abatimento);
        }

        if (pix.getDescontoModalidade() != null && pix.getDescontoValor() != null) {
            JsonObject desconto = new JsonObject()
                    .put("modalidade", pix.getDescontoModalidade())
                    .put("valor", pix.getDescontoValor().toString());
            json.put("desconto", desconto);
        }

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

    public JsonObject criarJsonDePix(Pix pix) {
        JsonObject json = new JsonObject();

        // Campos comuns
        json.put("txid", pix.getTxid());
        json.put("status", pix.getStatus());
        json.put("chave", pix.getChave());
        json.put("valor", pix.getValorOriginal().toString());
        json.put("nome", pix.getNome());
        json.put("criacao", pix.getCriacao().toString());

        // Campos opcionais comuns
        if (pix.getCpf() != null && !pix.getCpf().isEmpty()) {
            json.put("cpf", pix.getCpf());
        }
        if (pix.getCnpj() != null && !pix.getCnpj().isEmpty()) {
            json.put("cnpj", pix.getCnpj());
        }

        // Campos específicos por tipo
        if (pix instanceof PixComVencimento pixVenc) {
            json.put("tipoCob", "cobv");
            json.put("dataVencimento", pixVenc.getDataVencimento().toString());
            json.put("validadeAposVencimento", pixVenc.getValidadeAposVencimento());

            // Adicionar informações de multa e juros se disponíveis
            if (pixVenc.getMultaModalidade() != null && pixVenc.getMultaValor() != null) {
                JsonObject multa = new JsonObject()
                        .put("modalidade", pixVenc.getMultaModalidade())
                        .put("valor", pixVenc.getMultaValor().toString());
                json.put("multa", multa);
            }

            if (pixVenc.getJurosModalidade() != null && pixVenc.getJurosValor() != null) {
                JsonObject juros = new JsonObject()
                        .put("modalidade", pixVenc.getJurosModalidade())
                        .put("valor", pixVenc.getJurosValor().toString());
                json.put("juros", juros);
            }

            if (pixVenc.getAbatimentoModalidade() != null && pixVenc.getAbatimentoValor() != null) {
                JsonObject abatimento = new JsonObject()
                        .put("modalidade", pixVenc.getAbatimentoModalidade())
                        .put("valor", pixVenc.getAbatimentoValor().toString());
                json.put("abatimento", abatimento);
            }

            if (pixVenc.getDescontoModalidade() != null && pixVenc.getDescontoValor() != null) {
                JsonObject desconto = new JsonObject()
                        .put("modalidade", pixVenc.getDescontoModalidade())
                        .put("valor", pixVenc.getDescontoValor().toString());
                json.put("desconto", desconto);
            }

            if (pixVenc.getPixCopiaECola() != null) {
                json.put("pixCopiaECola", pixVenc.getPixCopiaECola());
            }

            if (pixVenc.getLocation() != null) {
                json.put("location", pixVenc.getLocation());
            }

        } else if (pix instanceof PixImediato pixImed) {
            json.put("tipoCob", "cob");
            json.put("expiracao", pixImed.getExpiracao());
        }

        // Campos comuns finais
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

}