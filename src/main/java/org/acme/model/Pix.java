package org.acme.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;

/**
 * Classe base que representa uma cobrança Pix
 * Contém os atributos e comportamentos comuns a todos os tipos de cobranças Pix
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_pix")
public abstract class Pix extends DefaultEntity {

    // Identificação da cobrança
    @Column(name = "txid")
    private String txid; // ID da transação, 26-35 caracteres
    private int revisao; // Revisão da cobrança

    // Dados de criação
    private LocalDateTime criacao; // Data/hora de criação da cobrança

    // Dados do devedor
    private String cpf; // CPF do devedor
    private String cnpj; // CNPJ do devedor
    private String nome; // Nome do devedor
    private String logradouro; // Endereço do devedor
    private String cidade; // Cidade do devedor
    private String uf; // UF/Estado do devedor
    private String cep; // CEP do devedor
    private String email; // Email do devedor

    // Dados do recebedor
    private String chave; // Chave Pix do recebedor
    private String recebedorNome; // Nome do recebedor
    private String recebedorCpf; // CPF do recebedor
    private String recebedorCnpj; // CNPJ do recebedor

    // Dados de valor
    private BigDecimal valorOriginal; // Valor original da cobrança
    private int modalidadeAlteracao; // 0 = não permite alterar, 1 = permite alterar

    // Dados location
    private Long idLoc; // ID do location
    private String location; // URL do payload
    private String tipoCob; // Tipo da cobrança (cob ou cobv)

    // Status da cobrança
    private String status; // ATIVA, CONCLUIDA, REMOVIDA_PELO_USUARIO_RECEBEDOR, REMOVIDA_PELO_PSP

    // Dados para Pix Saque ou Pix Troco
    private boolean isRetirada; // true se for Pix Saque ou Pix Troco
    private boolean isSaque; // true se for Pix Saque
    private boolean isTroco; // true se for Pix Troco
    private BigDecimal valorRetirada; // Valor do saque ou troco
    private int retiradaModalidadeAlteracao; // 0 = não permite alterar, 1 = permite alterar
    private String modalidadeAgente; // AGTEC, AGTOT, AGPSS
    private String ispbPrestadorServico; // ISPB do Facilitador de Serviço de Saque

    // Informações adicionais
    private String solicitacaoPagador; // Informação para o pagador (max 140 caracteres)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PixInfoAdicional> infoAdicionais; // Lista de informações adicionais
    private String pixCopiaECola; // QR Code em formato texto para Copia e Cola

    // Dados de liquidação
    private String endToEndId; // ID de liquidação do Pix
    private BigDecimal valorPago; // Valor efetivamente pago
    private LocalDateTime horarioPagamento; // Data/hora do pagamento
    private String infoPagador; // Informação enviada pelo pagador

    // Dados de devolução
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PixDevolucao> devolucoes; // Lista de devoluções

    /**
     * Construtor padrão - inicializa as listas e define valores padrão
     */
    public Pix() {
        this.infoAdicionais = new ArrayList<>();
        this.devolucoes = new ArrayList<>();
        this.status = "ATIVA";
        this.modalidadeAlteracao = 0; // Por padrão não permite alteração
        this.criacao = LocalDateTime.now();
        this.tipoCob = null; // Deve ser definido nas subclasses
    }

    /**
     * Construtor com os campos obrigatórios para criação de uma cobrança Pix
     * 
     * @param txid          ID da transação
     * @param chave         Chave Pix do recebedor
     * @param valorOriginal Valor original da cobrança
     * @param nome          Nome do devedor
     * @param cpf           CPF do devedor (opcional, pode ser null)
     * @param cnpj          CNPJ do devedor (opcional, pode ser null)
     */
    public Pix(String txid, String chave, BigDecimal valorOriginal, String nome, String cpf, String cnpj) {
        this();
        this.txid = txid;
        this.chave = chave;
        this.valorOriginal = valorOriginal;
        this.nome = nome;
        this.cpf = cpf;
        this.cnpj = cnpj;
    }

    /**
     * Adiciona uma informação adicional à cobrança
     * 
     * @param infoAdicional Objeto PixInfoAdicional a ser adicionado
     */
    public void addInfoAdicional(PixInfoAdicional infoAdicional) {
        this.infoAdicionais.add(infoAdicional);
    }

    /**
     * Adiciona uma informação adicional à cobrança a partir de nome e valor
     * 
     * @param nome  Nome da informação adicional
     * @param valor Valor da informação adicional
     */
    public void addInfoAdicional(String nome, String valor) {
        this.infoAdicionais.add(new PixInfoAdicional(nome, valor));
    }

    /**
     * Adiciona uma devolução à lista de devoluções desta cobrança
     * 
     * @param devolucao Objeto PixDevolucao a ser adicionado
     */
    public void addDevolucao(PixDevolucao devolucao) {
        this.devolucoes.add(devolucao);
    }

    /**
     * Configura esta cobrança para ser um Pix Saque
     * 
     * @param valor               Valor do saque
     * @param modalidadeAlteracao Indica se o valor pode ser alterado (0=não, 1=sim)
     * @param modalidadeAgente    Modalidade do agente (AGTEC, AGTOT, AGPSS)
     * @param ispbPrestador       ISPB do Facilitador de Serviço de Saque
     */
    public void configurarPixSaque(BigDecimal valor, int modalidadeAlteracao,
            String modalidadeAgente, String ispbPrestador) {
        this.isRetirada = true;
        this.isSaque = true;
        this.isTroco = false;
        this.valorRetirada = valor;
        this.retiradaModalidadeAlteracao = modalidadeAlteracao;
        this.modalidadeAgente = modalidadeAgente;
        this.ispbPrestadorServico = ispbPrestador;
    }

    /**
     * Configura esta cobrança para ser um Pix Troco
     * 
     * @param valor               Valor do troco
     * @param modalidadeAlteracao Indica se o valor pode ser alterado (0=não, 1=sim)
     * @param modalidadeAgente    Modalidade do agente (AGTEC, AGTOT, AGPSS)
     * @param ispbPrestador       ISPB do Facilitador de Serviço de Saque
     */
    public void configurarPixTroco(BigDecimal valor, int modalidadeAlteracao,
            String modalidadeAgente, String ispbPrestador) {
        this.isRetirada = true;
        this.isSaque = false;
        this.isTroco = true;
        this.valorRetirada = valor;
        this.retiradaModalidadeAlteracao = modalidadeAlteracao;
        this.modalidadeAgente = modalidadeAgente;
        this.ispbPrestadorServico = ispbPrestador;
    }

    /**
     * Método abstrato que identifica o tipo de cobrança
     * 
     * @return true se for cobrança com vencimento (cobv), false se for cobrança
     *         imediata (cob)
     */
    public abstract boolean isCobvTipo();

    /**
     * Registra o pagamento de uma cobrança
     * 
     * @param endToEndId  ID da transação de liquidação
     * @param valorPago   Valor efetivamente pago
     * @param infoPagador Informação adicional enviada pelo pagador
     */
    public void registrarPagamento(String endToEndId, BigDecimal valorPago, String infoPagador) {
        this.endToEndId = endToEndId;
        this.valorPago = valorPago;
        this.infoPagador = infoPagador;
        this.horarioPagamento = LocalDateTime.now();
        this.status = "CONCLUIDA";
    }

    /**
     * Verifica se a cobrança está ativa
     * 
     * @return true se estiver ativa, false caso contrário
     */
    public boolean isAtiva() {
        return "ATIVA".equals(this.status);
    }

    /**
     * Verifica se a cobrança foi paga
     * 
     * @return true se foi concluída, false caso contrário
     */
    public boolean isPaga() {
        return "CONCLUIDA".equals(this.status);
    }

    /**
     * Cancela a cobrança, marcando-a como removida pelo usuário recebedor
     */
    public void cancelar() {
        this.status = "REMOVIDA_PELO_USUARIO_RECEBEDOR";
    }

    // Getters e Setters para todos os atributos

    /**
     * Obtém o ID da transação
     * 
     * @return ID da transação (txid)
     */
    public String getTxid() {
        return txid;
    }

    /**
     * Define o ID da transação
     * 
     * @param txid ID da transação a ser definido
     */
    public void setTxid(String txid) {
        this.txid = txid;
    }

    /**
     * Obtém a revisão atual da cobrança
     * 
     * @return Número da revisão
     */
    public int getRevisao() {
        return revisao;
    }

    /**
     * Define a revisão da cobrança
     * 
     * @param revisao Número da revisão a ser definido
     */
    public void setRevisao(int revisao) {
        this.revisao = revisao;
    }

    /**
     * Obtém a data/hora de criação da cobrança
     * 
     * @return Data/hora de criação
     */
    public LocalDateTime getCriacao() {
        return criacao;
    }

    /**
     * Define a data/hora de criação da cobrança
     * 
     * @param criacao Data/hora de criação a ser definida
     */
    public void setCriacao(LocalDateTime criacao) {
        this.criacao = criacao;
    }

    /**
     * Obtém o CPF do devedor
     * 
     * @return CPF do devedor
     */
    public String getCpf() {
        return cpf;
    }

    /**
     * Define o CPF do devedor
     * 
     * @param cpf CPF do devedor a ser definido
     */
    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    /**
     * Obtém o CNPJ do devedor
     * 
     * @return CNPJ do devedor
     */
    public String getCnpj() {
        return cnpj;
    }

    /**
     * Define o CNPJ do devedor
     * 
     * @param cnpj CNPJ do devedor a ser definido
     */
    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    /**
     * Obtém o nome do devedor
     * 
     * @return Nome do devedor
     */
    public String getNome() {
        return nome;
    }

    /**
     * Define o nome do devedor
     * 
     * @param nome Nome do devedor a ser definido
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Obtém o logradouro (endereço) do devedor
     * 
     * @return Logradouro do devedor
     */
    public String getLogradouro() {
        return logradouro;
    }

    /**
     * Define o logradouro (endereço) do devedor
     * 
     * @param logradouro Logradouro do devedor a ser definido
     */
    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    /**
     * Obtém a cidade do devedor
     * 
     * @return Cidade do devedor
     */
    public String getCidade() {
        return cidade;
    }

    /**
     * Define a cidade do devedor
     * 
     * @param cidade Cidade do devedor a ser definida
     */
    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    /**
     * Obtém a UF (estado) do devedor
     * 
     * @return UF do devedor
     */
    public String getUf() {
        return uf;
    }

    /**
     * Define a UF (estado) do devedor
     * 
     * @param uf UF do devedor a ser definida
     */
    public void setUf(String uf) {
        this.uf = uf;
    }

    /**
     * Obtém o CEP do devedor
     * 
     * @return CEP do devedor
     */
    public String getCep() {
        return cep;
    }

    /**
     * Define o CEP do devedor
     * 
     * @param cep CEP do devedor a ser definido
     */
    public void setCep(String cep) {
        this.cep = cep;
    }

    /**
     * Obtém o email do devedor
     * 
     * @return Email do devedor
     */
    public String getEmail() {
        return email;
    }

    /**
     * Define o email do devedor
     * 
     * @param email Email do devedor a ser definido
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Obtém a chave Pix do recebedor
     * 
     * @return Chave Pix do recebedor
     */
    public String getChave() {
        return chave;
    }

    /**
     * Define a chave Pix do recebedor
     * 
     * @param chave Chave Pix a ser definida
     */
    public void setChave(String chave) {
        this.chave = chave;
    }

    /**
     * Obtém o nome do recebedor
     * 
     * @return Nome do recebedor
     */
    public String getRecebedorNome() {
        return recebedorNome;
    }

    /**
     * Define o nome do recebedor
     * 
     * @param recebedorNome Nome do recebedor a ser definido
     */
    public void setRecebedorNome(String recebedorNome) {
        this.recebedorNome = recebedorNome;
    }

    /**
     * Obtém o CPF do recebedor
     * 
     * @return CPF do recebedor
     */
    public String getRecebedorCpf() {
        return recebedorCpf;
    }

    /**
     * Define o CPF do recebedor
     * 
     * @param recebedorCpf CPF do recebedor a ser definido
     */
    public void setRecebedorCpf(String recebedorCpf) {
        this.recebedorCpf = recebedorCpf;
    }

    /**
     * Obtém o CNPJ do recebedor
     * 
     * @return CNPJ do recebedor
     */
    public String getRecebedorCnpj() {
        return recebedorCnpj;
    }

    /**
     * Define o CNPJ do recebedor
     * 
     * @param recebedorCnpj CNPJ do recebedor a ser definido
     */
    public void setRecebedorCnpj(String recebedorCnpj) {
        this.recebedorCnpj = recebedorCnpj;
    }

    /**
     * Obtém o valor original da cobrança
     * 
     * @return Valor original
     */
    public BigDecimal getValorOriginal() {
        return valorOriginal;
    }

    /**
     * Define o valor original da cobrança
     * 
     * @param valorOriginal Valor original a ser definido
     */
    public void setValorOriginal(BigDecimal valorOriginal) {
        this.valorOriginal = valorOriginal;
    }

    /**
     * Obtém a modalidade de alteração do valor
     * 
     * @return Modalidade de alteração (0=não permite, 1=permite)
     */
    public int getModalidadeAlteracao() {
        return modalidadeAlteracao;
    }

    /**
     * Define a modalidade de alteração do valor
     * 
     * @param modalidadeAlteracao Modalidade de alteração a ser definida
     */
    public void setModalidadeAlteracao(int modalidadeAlteracao) {
        this.modalidadeAlteracao = modalidadeAlteracao;
    }

    /**
     * Obtém o ID do location
     * 
     * @return ID do location
     */
    public Long getIdLoc() {
        return idLoc;
    }

    /**
     * Define o ID do location
     * 
     * @param idLoc ID do location a ser definido
     */
    public void setIdLoc(Long idLoc) {
        this.idLoc = idLoc;
    }

    /**
     * Obtém a URL do payload
     * 
     * @return URL do payload
     */
    public String getLocation() {
        return location;
    }

    /**
     * Define a URL do payload
     * 
     * @param location URL do payload a ser definida
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Obtém o tipo da cobrança
     * 
     * @return Tipo da cobrança (cob ou cobv)
     */
    public String getTipoCob() {
        return tipoCob;
    }

    /**
     * Define o tipo da cobrança
     * 
     * @param tipoCob Tipo da cobrança a ser definido (cob ou cobv)
     */
    public void setTipoCob(String tipoCob) {
        this.tipoCob = tipoCob;
    }

    /**
     * Obtém o status da cobrança
     * 
     * @return Status da cobrança
     */
    public String getStatus() {
        return status;
    }

    /**
     * Define o status da cobrança
     * 
     * @param status Status da cobrança a ser definido
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Verifica se a cobrança é do tipo retirada (Pix Saque ou Pix Troco)
     * 
     * @return true se for retirada, false caso contrário
     */
    public boolean isRetirada() {
        return isRetirada;
    }

    /**
     * Define se a cobrança é do tipo retirada
     * 
     * @param isRetirada Valor a ser definido
     */
    public void setRetirada(boolean isRetirada) {
        this.isRetirada = isRetirada;
    }

    /**
     * Verifica se a cobrança é do tipo Pix Saque
     * 
     * @return true se for Pix Saque, false caso contrário
     */
    public boolean isSaque() {
        return isSaque;
    }

    /**
     * Define se a cobrança é do tipo Pix Saque
     * 
     * @param isSaque Valor a ser definido
     */
    public void setSaque(boolean isSaque) {
        this.isSaque = isSaque;
    }

    /**
     * Verifica se a cobrança é do tipo Pix Troco
     * 
     * @return true se for Pix Troco, false caso contrário
     */
    public boolean isTroco() {
        return isTroco;
    }

    /**
     * Define se a cobrança é do tipo Pix Troco
     * 
     * @param isTroco Valor a ser definido
     */
    public void setTroco(boolean isTroco) {
        this.isTroco = isTroco;
    }

    /**
     * Obtém o valor da retirada (Pix Saque ou Pix Troco)
     * 
     * @return Valor da retirada
     */
    public BigDecimal getValorRetirada() {
        return valorRetirada;
    }

    /**
     * Define o valor da retirada (Pix Saque ou Pix Troco)
     * 
     * @param valorRetirada Valor da retirada a ser definido
     */
    public void setValorRetirada(BigDecimal valorRetirada) {
        this.valorRetirada = valorRetirada;
    }

    /**
     * Obtém a modalidade de alteração do valor da retirada
     * 
     * @return Modalidade de alteração (0=não permite, 1=permite)
     */
    public int getRetiradaModalidadeAlteracao() {
        return retiradaModalidadeAlteracao;
    }

    /**
     * Define a modalidade de alteração do valor da retirada
     * 
     * @param retiradaModalidadeAlteracao Modalidade de alteração a ser definida
     */
    public void setRetiradaModalidadeAlteracao(int retiradaModalidadeAlteracao) {
        this.retiradaModalidadeAlteracao = retiradaModalidadeAlteracao;
    }

    /**
     * Obtém a modalidade do agente
     * 
     * @return Modalidade do agente (AGTEC, AGTOT, AGPSS)
     */
    public String getModalidadeAgente() {
        return modalidadeAgente;
    }

    /**
     * Define a modalidade do agente
     * 
     * @param modalidadeAgente Modalidade do agente a ser definida
     */
    public void setModalidadeAgente(String modalidadeAgente) {
        this.modalidadeAgente = modalidadeAgente;
    }

    /**
     * Obtém o ISPB do prestador de serviço
     * 
     * @return ISPB do prestador
     */
    public String getIspbPrestadorServico() {
        return ispbPrestadorServico;
    }

    /**
     * Define o ISPB do prestador de serviço
     * 
     * @param ispbPrestadorServico ISPB do prestador a ser definido
     */
    public void setIspbPrestadorServico(String ispbPrestadorServico) {
        this.ispbPrestadorServico = ispbPrestadorServico;
    }

    /**
     * Obtém a solicitação ao pagador
     * 
     * @return Texto da solicitação
     */
    public String getSolicitacaoPagador() {
        return solicitacaoPagador;
    }

    /**
     * Define a solicitação ao pagador
     * 
     * @param solicitacaoPagador Texto da solicitação a ser definido
     */
    public void setSolicitacaoPagador(String solicitacaoPagador) {
        this.solicitacaoPagador = solicitacaoPagador;
    }

    /**
     * Obtém a lista de informações adicionais
     * 
     * @return Lista de informações adicionais
     */
    public List<PixInfoAdicional> getInfoAdicionais() {
        return infoAdicionais;
    }

    /**
     * Define a lista de informações adicionais
     * 
     * @param infoAdicionais Lista de informações adicionais a ser definida
     */
    public void setInfoAdicionais(List<PixInfoAdicional> infoAdicionais) {
        this.infoAdicionais = infoAdicionais;
    }

    /**
     * Obtém o QR Code em formato texto para Copia e Cola
     * 
     * @return Texto do QR Code
     */
    public String getPixCopiaECola() {
        return pixCopiaECola;
    }

    /**
     * Define o QR Code em formato texto para Copia e Cola
     * 
     * @param pixCopiaECola Texto do QR Code a ser definido
     */
    public void setPixCopiaECola(String pixCopiaECola) {
        this.pixCopiaECola = pixCopiaECola;
    }

    /**
     * Obtém o ID de liquidação do Pix (End to End ID)
     * 
     * @return ID de liquidação
     */
    public String getEndToEndId() {
        return endToEndId;
    }

    /**
     * Define o ID de liquidação do Pix (End to End ID)
     * 
     * @param endToEndId ID de liquidação a ser definido
     */
    public void setEndToEndId(String endToEndId) {
        this.endToEndId = endToEndId;
    }

    /**
     * Obtém o valor efetivamente pago
     * 
     * @return Valor pago
     */
    public BigDecimal getValorPago() {
        return valorPago;
    }

    /**
     * Define o valor efetivamente pago
     * 
     * @param valorPago Valor pago a ser definido
     */
    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }

    /**
     * Obtém a data/hora do pagamento
     * 
     * @return Data/hora do pagamento
     */
    public LocalDateTime getHorarioPagamento() {
        return horarioPagamento;
    }

    /**
     * Define a data/hora do pagamento
     * 
     * @param horarioPagamento Data/hora do pagamento a ser definida
     */
    public void setHorarioPagamento(LocalDateTime horarioPagamento) {
        this.horarioPagamento = horarioPagamento;
    }

    /**
     * Obtém a informação enviada pelo pagador
     * 
     * @return Informação do pagador
     */
    public String getInfoPagador() {
        return infoPagador;
    }

    /**
     * Define a informação enviada pelo pagador
     * 
     * @param infoPagador Informação do pagador a ser definida
     */
    public void setInfoPagador(String infoPagador) {
        this.infoPagador = infoPagador;
    }

    /**
     * Obtém a lista de devoluções associadas a esta cobrança
     * 
     * @return Lista de devoluções
     */
    public List<PixDevolucao> getDevolucoes() {
        return devolucoes;
    }

    /**
     * Define a lista de devoluções associadas a esta cobrança
     * 
     * @param devolucoes Lista de devoluções a ser definida
     */
    public void setDevolucoes(List<PixDevolucao> devolucoes) {
        this.devolucoes = devolucoes;
    }

    @Override
    public String toString() {
        return "Pix{" +
                "txid='" + txid + '\'' +
                ", valor=" + valorOriginal +
                ", tipo=" + (isCobvTipo() ? "cobv" : "cob") +
                ", status='" + status + '\'' +
                '}';
    }
}