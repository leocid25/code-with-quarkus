package org.acme.model;

import java.math.BigDecimal;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Classe que representa uma cobrança Pix imediata (cob)
 */
@Entity
@DiscriminatorValue("IMEDIATO")
public class PixImediato extends Pix {
    
    // Dados específicos da cobrança imediata
    private Integer expiracao; // Tempo de vida da cobrança em segundos
    
    /**
     * Construtor padrão
     */
    public PixImediato() {
        super();
        this.setTipoCob("cob");
        this.expiracao = 86400; // 24 horas por padrão
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
     */
    public PixImediato(String txid, String chave, BigDecimal valorOriginal, 
                      String nome, String cpf, String cnpj, Integer expiracao) {
        super(txid, chave, valorOriginal, nome, cpf, cnpj);
        this.setTipoCob("cob");
        this.expiracao = expiracao; 
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
     * @param banco Código do banco
     */
    public PixImediato(String txid, String chave, BigDecimal valorOriginal, 
                      String nome, String cpf, String cnpj, String banco) {
        super(txid, chave, valorOriginal, nome, cpf, cnpj);
        this.setTipoCob("cob");
        this.expiracao = 86400; // 24 horas por padrão
        this.setBanco(banco);
    }
    
    /**
     * Construtor com os campos obrigatórios e tempo de expiração personalizado
     *
     * @param txid ID da transação
     * @param chave Chave Pix do recebedor
     * @param valorOriginal Valor original da cobrança
     * @param nome Nome do devedor
     * @param cpf CPF do devedor (opcional, pode ser null)
     * @param cnpj CNPJ do devedor (opcional, pode ser null)
     * @param expiracao Tempo de vida da cobrança em segundos
     */
    public PixImediato(String txid, String chave, BigDecimal valorOriginal, 
                      String nome, String cpf, String cnpj, Integer expiracao, String banco) {
        this(txid, chave, valorOriginal, nome, cpf, cnpj, banco);
        this.expiracao = expiracao;
    }
    
    @Override
    public boolean isCobvTipo() {
        return false; // Não é cobrança com vencimento
    }
    
    // Getters e Setters específicos
    
    public Integer getExpiracao() {
        return expiracao;
    }
    
    public void setExpiracao(Integer expiracao) {
        this.expiracao = expiracao;
    }
}