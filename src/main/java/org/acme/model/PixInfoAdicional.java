package org.acme.model;

import jakarta.persistence.Entity;

/**
 * Classe que representa uma informação adicional associada a uma cobrança Pix
 */
@Entity
public class PixInfoAdicional extends DefaultEntity {
    private String nome;
    private String valor;
    
    /**
     * Construtor padrão
     */
    public PixInfoAdicional() {
    }
    
    /**
     * Construtor com parâmetros
     *
     * @param nome Nome da informação adicional
     * @param valor Valor da informação adicional
     */
    public PixInfoAdicional(String nome, String valor) {
        this.nome = nome;
        this.valor = valor;
    }
    
    // Getters e setters
    
    /**
     * Obtém o nome da informação adicional
     *
     * @return Nome da informação adicional
     */
    public String getNome() {
        return nome;
    }
    
    /**
     * Define o nome da informação adicional
     *
     * @param nome Nome da informação adicional
     */
    public void setNome(String nome) {
        this.nome = nome;
    }
    
    /**
     * Obtém o valor da informação adicional
     *
     * @return Valor da informação adicional
     */
    public String getValor() {
        return valor;
    }
    
    /**
     * Define o valor da informação adicional
     *
     * @param valor Valor da informação adicional
     */
    public void setValor(String valor) {
        this.valor = valor;
    }
    
    @Override
    public String toString() {
        return nome + ": " + valor;
    }
}