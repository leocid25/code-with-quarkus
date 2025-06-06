package org.acme.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Classe que representa uma cobrança Pix com vencimento (cobv)
 */
@Entity
@DiscriminatorValue("VENCIMENTO")
public class PixComVencimento extends Pix {
    
    // Dados específicos da cobrança com vencimento
    private LocalDate dataVencimento; // Data de vencimento
    private Integer validadeAposVencimento; // Prazo de recebimento após vencimento em dias
    
    // Dados de multa e juros
    private Integer multaModalidade; // 1 = Valor fixo, 2 = Percentual
    private BigDecimal multaValor; // Valor ou percentual da multa
    
    private Integer jurosModalidade; // 1-8 conforme documentação
    private BigDecimal jurosValor; // Valor ou percentual dos juros
    
    // Abatimento e desconto
    private Integer abatimentoModalidade; // 1 = Valor fixo, 2 = Percentual
    private BigDecimal abatimentoValor; // Valor ou percentual do abatimento
    
    private Integer descontoModalidade; // 1-6 conforme documentação
    private BigDecimal descontoValor; // Valor ou percentual do desconto
    
    /**
     * Construtor padrão
     */
    public PixComVencimento() {
        super();
        this.setTipoCob("cobv");
        this.validadeAposVencimento = 30; // 30 dias por padrão
    }
    
    /**
     * Construtor com os campos obrigatórios
     *
     * @param txid ID da transação
     * @param chave Chave Pix do recebedor
     * @param valorOriginal Valor original da cobrança
     * @param nome Nome do devedor
     * @param cpf CPF do devedor (opcional, pode ser null)
     * @param cnpj CNPJ do devedor (opcional, pode ser null)
     * @param dataVencimento Data de vencimento
     */
    public PixComVencimento(String txid, String chave, BigDecimal valorOriginal, 
                           String nome, String cpf, String cnpj, LocalDate dataVencimento, String banco) {
        super(txid, chave, valorOriginal, nome, cpf, cnpj);
        this.setTipoCob("cobv");
        this.dataVencimento = dataVencimento;
        this.setBanco(banco);
    }
    
    /**
     * Construtor com os campos obrigatórios e validade após vencimento personalizada
     *
     * @param txid ID da transação
     * @param chave Chave Pix do recebedor
     * @param valorOriginal Valor original da cobrança
     * @param nome Nome do devedor
     * @param cpf CPF do devedor (opcional, pode ser null)
     * @param cnpj CNPJ do devedor (opcional, pode ser null)
     * @param dataVencimento Data de vencimento
     * @param validadeAposVencimento Prazo de recebimento após vencimento em dias
     */
    public PixComVencimento(String txid, String chave, BigDecimal valorOriginal, 
                           String nome, String cpf, String cnpj, 
                           LocalDate dataVencimento, Integer validadeAposVencimento, String banco) {
        this(txid, chave, valorOriginal, nome, cpf, cnpj, dataVencimento, banco);
        // Se validadeAposVencimento for null, usa 30 como padrão
        Integer validade = validadeAposVencimento;
        this.validadeAposVencimento = (validade != null) ? validade : 30;
    }
    
    @Override
    public boolean isCobvTipo() {
        return true; // É cobrança com vencimento
    }
    
    /**
     * Configura os dados de multa para a cobrança
     *
     * @param modalidade Modalidade da multa (1 = Valor fixo, 2 = Percentual)
     * @param valor Valor ou percentual da multa
     */
    public void configurarMulta(int modalidade, BigDecimal valor) {
        this.multaModalidade = modalidade;
        this.multaValor = valor;
    }
    
    /**
     * Configura os dados de juros para a cobrança
     *
     * @param modalidade Modalidade de juros (1-8 conforme documentação)
     * @param valor Valor ou percentual dos juros
     */
    public void configurarJuros(int modalidade, BigDecimal valor) {
        this.jurosModalidade = modalidade;
        this.jurosValor = valor;
    }
    
    /**
     * Configura os dados de abatimento para a cobrança
     *
     * @param modalidade Modalidade de abatimento (1 = Valor fixo, 2 = Percentual)
     * @param valor Valor ou percentual do abatimento
     */
    public void configurarAbatimento(int modalidade, BigDecimal valor) {
        this.abatimentoModalidade = modalidade;
        this.abatimentoValor = valor;
    }
    
    /**
     * Configura os dados de desconto para a cobrança
     *
     * @param modalidade Modalidade de desconto (1-6 conforme documentação)
     * @param valor Valor ou percentual do desconto
     */
    public void configurarDesconto(int modalidade, BigDecimal valor) {
        this.descontoModalidade = modalidade;
        this.descontoValor = valor;
    }
    
    // Getters e Setters específicos
    
    public LocalDate getDataVencimento() {
        return dataVencimento;
    }
    
    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }
    
    public Integer getValidadeAposVencimento() {
        return validadeAposVencimento;
    }
    
    public void setValidadeAposVencimento(Integer validadeAposVencimento) {
        this.validadeAposVencimento = validadeAposVencimento;
    }
    
    public Integer getMultaModalidade() {
        return multaModalidade;
    }
    
    public void setMultaModalidade(Integer multaModalidade) {
        this.multaModalidade = multaModalidade;
    }
    
    public BigDecimal getMultaValor() {
        return multaValor;
    }
    
    public void setMultaValor(BigDecimal multaValor) {
        this.multaValor = multaValor;
    }
    
    public Integer getJurosModalidade() {
        return jurosModalidade;
    }
    
    public void setJurosModalidade(Integer jurosModalidade) {
        this.jurosModalidade = jurosModalidade;
    }
    
    public BigDecimal getJurosValor() {
        return jurosValor;
    }
    
    public void setJurosValor(BigDecimal jurosValor) {
        this.jurosValor = jurosValor;
    }
    
    public Integer getAbatimentoModalidade() {
        return abatimentoModalidade;
    }
    
    public void setAbatimentoModalidade(Integer abatimentoModalidade) {
        this.abatimentoModalidade = abatimentoModalidade;
    }
    
    public BigDecimal getAbatimentoValor() {
        return abatimentoValor;
    }
    
    public void setAbatimentoValor(BigDecimal abatimentoValor) {
        this.abatimentoValor = abatimentoValor;
    }
    
    public Integer getDescontoModalidade() {
        return descontoModalidade;
    }
    
    public void setDescontoModalidade(Integer descontoModalidade) {
        this.descontoModalidade = descontoModalidade;
    }
    
    public BigDecimal getDescontoValor() {
        return descontoValor;
    }
    
    public void setDescontoValor(BigDecimal descontoValor) {
        this.descontoValor = descontoValor;
    }
}