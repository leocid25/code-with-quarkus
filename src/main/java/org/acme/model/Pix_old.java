// package org.acme.model;

// import java.math.BigDecimal;
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;

// /**
//  * Modelo que representa uma cobrança Pix, contemplando tanto
//  * cobranças imediatas (cob) quanto cobranças com vencimento (cobv)
//  */
// public class Pix {
    
//     Identificação da cobrança
//     private String txid; // ID da transação, 26-35 caracteres
//     private int revisao; // Revisão da cobrança
//     private boolean cobvTipo; // true = cobrança com vencimento, false = cobrança imediata
    
//     Dados do calendario
//     private LocalDateTime criacao; // Data/hora de criação da cobrança
//     private Integer expiracao; // Tempo de vida da cobrança em segundos (para cob)
//     private LocalDate dataVencimento; // Data de vencimento (para cobv)
//     private Integer validadeAposVencimento; // Prazo de recebimento após vencimento em dias (para cobv)
    
//     Dados do devedor
//     private String cpf; // CPF do devedor
//     private String cnpj; // CNPJ do devedor
//     private String nome; // Nome do devedor
//     private String logradouro; // Endereço do devedor
//     private String cidade; // Cidade do devedor
//     private String uf; // UF/Estado do devedor
//     private String cep; // CEP do devedor
//     private String email; // Email do devedor
    
//     Dados do recebedor
//     private String chave; // Chave Pix do recebedor
//     private String recebedorNome; // Nome do recebedor
//     private String recebedorCpf; // CPF do recebedor
//     private String recebedorCnpj; // CNPJ do recebedor
    
//     Dados de valor
//     private BigDecimal valorOriginal; // Valor original da cobrança
//     private int modalidadeAlteracao; // 0 = não permite alterar, 1 = permite alterar
    
//     Dados location
//     private Long idLoc; // ID do location
//     private String location; // URL do payload
//     private String tipoCob; // Tipo da cobrança (cob ou cobv)
    
//     Status da cobrança
//     private String status; // ATIVA, CONCLUIDA, REMOVIDA_PELO_USUARIO_RECEBEDOR, REMOVIDA_PELO_PSP
    
//     Dados de multa e juros (para cobranças com vencimento)
//     private Integer multaModalidade; // 1 = Valor fixo, 2 = Percentual
//     private BigDecimal multaValor; // Valor ou percentual da multa
    
//     private Integer jurosModalidade; // 1-8 conforme documentação
//     private BigDecimal jurosValor; // Valor ou percentual dos juros
    
//     Abatimento e desconto (para cobranças com vencimento)
//     private Integer abatimentoModalidade; // 1 = Valor fixo, 2 = Percentual
//     private BigDecimal abatimentoValor; // Valor ou percentual do abatimento
    
//     private Integer descontoModalidade; // 1-6 conforme documentação
//     private BigDecimal descontoValor; // Valor ou percentual do desconto
    
//     Dados para Pix Saque ou Pix Troco
//     private boolean isRetirada; // true se for Pix Saque ou Pix Troco
//     private boolean isSaque; // true se for Pix Saque
//     private boolean isTroco; // true se for Pix Troco
//     private BigDecimal valorRetirada; // Valor do saque ou troco
//     private int retiradaModalidadeAlteracao; // 0 = não permite alterar, 1 = permite alterar
//     private String modalidadeAgente; // AGTEC, AGTOT, AGPSS
//     private String ispbPrestadorServico; // ISPB do Facilitador de Serviço de Saque
    
//     Informações adicionais
//     private String solicitacaoPagador; // Informação para o pagador (max 140 caracteres)
//     private List<InfoAdicional> infoAdicionais; // Lista de informações adicionais
//     private String pixCopiaECola; // QR Code em formato texto para Copia e Cola
    
//     Dados de liquidação
//     private String endToEndId; // ID de liquidação do Pix
//     private BigDecimal valorPago; // Valor efetivamente pago
//     private LocalDateTime horarioPagamento; // Data/hora do pagamento
//     private String infoPagador; // Informação enviada pelo pagador
    
//     Dados de devolução
//     private List<PixDevolucao> devolucoes; // Lista de devoluções
    
//     Construtor padrão
//     public Pix() {
//         this.infoAdicionais = new ArrayList<>();
//         this.devolucoes = new ArrayList<>();
//     }
    
//     Construtor para cobrança imediata
//     public Pix(String txid, String chave, BigDecimal valorOriginal, String nome, String cpf, String cnpj) {
//         this();
//         this.txid = txid;
//         this.chave = chave;
//         this.valorOriginal = valorOriginal;
//         this.nome = nome;
//         this.cpf = cpf;
//         this.cnpj = cnpj;
//         this.cobvTipo = false;
//         this.status = "ATIVA";
//         this.modalidadeAlteracao = 0; // Por padrão não permite alteração
//     }
    
//     Construtor para cobrança com vencimento
//     public Pix(String txid, String chave, BigDecimal valorOriginal, String nome, String cpf, 
//                String cnpj, LocalDate dataVencimento) {
//         this(txid, chave, valorOriginal, nome, cpf, cnpj);
//         this.dataVencimento = dataVencimento;
//         this.cobvTipo = true;
//         this.validadeAposVencimento = 30; // Padrão 30 dias
//     }
    
//     Classe interna para representar informações adicionais
//     public static class InfoAdicional {
//         private String nome;
//         private String valor;
        
//         public InfoAdicional(String nome, String valor) {
//             this.nome = nome;
//             this.valor = valor;
//         }
        
//         Getters e setters
//         public String getNome() {
//             return nome;
//         }
        
//         public void setNome(String nome) {
//             this.nome = nome;
//         }
        
//         public String getValor() {
//             return valor;
//         }
        
//         public void setValor(String valor) {
//             this.valor = valor;
//         }
//     }
    
//     Classe interna para representar devoluções
//     public static class PixDevolucao {
//         private String id;
//         private String rtrId;
//         private BigDecimal valor;
//         private String natureza; // ORIGINAL, RETIRADA, MED_OPERACIONAL, MED_FRAUDE
//         private String descricao;
//         private LocalDateTime horarioSolicitacao;
//         private LocalDateTime horarioLiquidacao;
//         private String status; // EM_PROCESSAMENTO, DEVOLVIDO, NAO_REALIZADO
//         private String motivo;
        
//         Construtor básico
//         public PixDevolucao(String id, BigDecimal valor) {
//             this.id = id;
//             this.valor = valor;
//             this.natureza = "ORIGINAL";
//             this.status = "EM_PROCESSAMENTO";
//             this.horarioSolicitacao = LocalDateTime.now();
//         }
        
//         Getters e setters
//         public String getId() {
//             return id;
//         }
        
//         public void setId(String id) {
//             this.id = id;
//         }
        
//         public String getRtrId() {
//             return rtrId;
//         }
        
//         public void setRtrId(String rtrId) {
//             this.rtrId = rtrId;
//         }
        
//         public BigDecimal getValor() {
//             return valor;
//         }
        
//         public void setValor(BigDecimal valor) {
//             this.valor = valor;
//         }
        
//         public String getNatureza() {
//             return natureza;
//         }
        
//         public void setNatureza(String natureza) {
//             this.natureza = natureza;
//         }
        
//         public String getDescricao() {
//             return descricao;
//         }
        
//         public void setDescricao(String descricao) {
//             this.descricao = descricao;
//         }
        
//         public LocalDateTime getHorarioSolicitacao() {
//             return horarioSolicitacao;
//         }
        
//         public void setHorarioSolicitacao(LocalDateTime horarioSolicitacao) {
//             this.horarioSolicitacao = horarioSolicitacao;
//         }
        
//         public LocalDateTime getHorarioLiquidacao() {
//             return horarioLiquidacao;
//         }
        
//         public void setHorarioLiquidacao(LocalDateTime horarioLiquidacao) {
//             this.horarioLiquidacao = horarioLiquidacao;
//         }
        
//         public String getStatus() {
//             return status;
//         }
        
//         public void setStatus(String status) {
//             this.status = status;
//         }
        
//         public String getMotivo() {
//             return motivo;
//         }
        
//         public void setMotivo(String motivo) {
//             this.motivo = motivo;
//         }
//     }
    
//     Métodos para adicionar informações
//     public void addInfoAdicional(String nome, String valor) {
//         this.infoAdicionais.add(new InfoAdicional(nome, valor));
//     }
    
//     public void addDevolucao(PixDevolucao devolucao) {
//         this.devolucoes.add(devolucao);
//     }
    
//     Métodos para configurar cobrança com vencimento
//     public void configurarMulta(int modalidade, BigDecimal valor) {
//         this.multaModalidade = modalidade;
//         this.multaValor = valor;
//     }
    
//     public void configurarJuros(int modalidade, BigDecimal valor) {
//         this.jurosModalidade = modalidade;
//         this.jurosValor = valor;
//     }
    
//     public void configurarAbatimento(int modalidade, BigDecimal valor) {
//         this.abatimentoModalidade = modalidade;
//         this.abatimentoValor = valor;
//     }
    
//     public void configurarDesconto(int modalidade, BigDecimal valor) {
//         this.descontoModalidade = modalidade;
//         this.descontoValor = valor;
//     }
    
//     Métodos para configurar Pix Saque ou Pix Troco
//     public void configurarPixSaque(BigDecimal valor, int modalidadeAlteracao, 
//                                   String modalidadeAgente, String ispbPrestador) {
//         this.isRetirada = true;
//         this.isSaque = true;
//         this.isTroco = false;
//         this.valorRetirada = valor;
//         this.retiradaModalidadeAlteracao = modalidadeAlteracao;
//         this.modalidadeAgente = modalidadeAgente;
//         this.ispbPrestadorServico = ispbPrestador;
//     }
    
//     public void configurarPixTroco(BigDecimal valor, int modalidadeAlteracao, 
//                                   String modalidadeAgente, String ispbPrestador) {
//         this.isRetirada = true;
//         this.isSaque = false;
//         this.isTroco = true;
//         this.valorRetirada = valor;
//         this.retiradaModalidadeAlteracao = modalidadeAlteracao;
//         this.modalidadeAgente = modalidadeAgente;
//         this.ispbPrestadorServico = ispbPrestador;
//     }
    
//     Getters e Setters para todos os atributos
    
//     public String getTxid() {
//         return txid;
//     }

//     public void setTxid(String txid) {
//         this.txid = txid;
//     }

//     public int getRevisao() {
//         return revisao;
//     }

//     public void setRevisao(int revisao) {
//         this.revisao = revisao;
//     }

//     public boolean isCobvTipo() {
//         return cobvTipo;
//     }

//     public void setCobvTipo(boolean cobvTipo) {
//         this.cobvTipo = cobvTipo;
//     }

//     public LocalDateTime getCriacao() {
//         return criacao;
//     }

//     public void setCriacao(LocalDateTime criacao) {
//         this.criacao = criacao;
//     }

//     public Integer getExpiracao() {
//         return expiracao;
//     }

//     public void setExpiracao(Integer expiracao) {
//         this.expiracao = expiracao;
//     }

//     public LocalDate getDataVencimento() {
//         return dataVencimento;
//     }

//     public void setDataVencimento(LocalDate dataVencimento) {
//         this.dataVencimento = dataVencimento;
//     }

//     public Integer getValidadeAposVencimento() {
//         return validadeAposVencimento;
//     }

//     public void setValidadeAposVencimento(Integer validadeAposVencimento) {
//         this.validadeAposVencimento = validadeAposVencimento;
//     }

//     public String getCpf() {
//         return cpf;
//     }

//     public void setCpf(String cpf) {
//         this.cpf = cpf;
//     }

//     public String getCnpj() {
//         return cnpj;
//     }

//     public void setCnpj(String cnpj) {
//         this.cnpj = cnpj;
//     }

//     public String getNome() {
//         return nome;
//     }

//     public void setNome(String nome) {
//         this.nome = nome;
//     }

//     public String getLogradouro() {
//         return logradouro;
//     }

//     public void setLogradouro(String logradouro) {
//         this.logradouro = logradouro;
//     }

//     public String getCidade() {
//         return cidade;
//     }

//     public void setCidade(String cidade) {
//         this.cidade = cidade;
//     }

//     public String getUf() {
//         return uf;
//     }

//     public void setUf(String uf) {
//         this.uf = uf;
//     }

//     public String getCep() {
//         return cep;
//     }

//     public void setCep(String cep) {
//         this.cep = cep;
//     }

//     public String getEmail() {
//         return email;
//     }

//     public void setEmail(String email) {
//         this.email = email;
//     }

//     public String getChave() {
//         return chave;
//     }

//     public void setChave(String chave) {
//         this.chave = chave;
//     }

//     public String getRecebedorNome() {
//         return recebedorNome;
//     }

//     public void setRecebedorNome(String recebedorNome) {
//         this.recebedorNome = recebedorNome;
//     }

//     public String getRecebedorCpf() {
//         return recebedorCpf;
//     }

//     public void setRecebedorCpf(String recebedorCpf) {
//         this.recebedorCpf = recebedorCpf;
//     }

//     public String getRecebedorCnpj() {
//         return recebedorCnpj;
//     }

//     public void setRecebedorCnpj(String recebedorCnpj) {
//         this.recebedorCnpj = recebedorCnpj;
//     }

//     public BigDecimal getValorOriginal() {
//         return valorOriginal;
//     }

//     public void setValorOriginal(BigDecimal valorOriginal) {
//         this.valorOriginal = valorOriginal;
//     }

//     public int getModalidadeAlteracao() {
//         return modalidadeAlteracao;
//     }

//     public void setModalidadeAlteracao(int modalidadeAlteracao) {
//         this.modalidadeAlteracao = modalidadeAlteracao;
//     }

//     public Long getIdLoc() {
//         return idLoc;
//     }

//     public void setIdLoc(Long idLoc) {
//         this.idLoc = idLoc;
//     }

//     public String getLocation() {
//         return location;
//     }

//     public void setLocation(String location) {
//         this.location = location;
//     }

//     public String getTipoCob() {
//         return tipoCob;
//     }

//     public void setTipoCob(String tipoCob) {
//         this.tipoCob = tipoCob;
//     }

//     public String getStatus() {
//         return status;
//     }

//     public void setStatus(String status) {
//         this.status = status;
//     }

//     public Integer getMultaModalidade() {
//         return multaModalidade;
//     }

//     public void setMultaModalidade(Integer multaModalidade) {
//         this.multaModalidade = multaModalidade;
//     }

//     public BigDecimal getMultaValor() {
//         return multaValor;
//     }

//     public void setMultaValor(BigDecimal multaValor) {
//         this.multaValor = multaValor;
//     }

//     public Integer getJurosModalidade() {
//         return jurosModalidade;
//     }

//     public void setJurosModalidade(Integer jurosModalidade) {
//         this.jurosModalidade = jurosModalidade;
//     }

//     public BigDecimal getJurosValor() {
//         return jurosValor;
//     }

//     public void setJurosValor(BigDecimal jurosValor) {
//         this.jurosValor = jurosValor;
//     }

//     public Integer getAbatimentoModalidade() {
//         return abatimentoModalidade;
//     }

//     public void setAbatimentoModalidade(Integer abatimentoModalidade) {
//         this.abatimentoModalidade = abatimentoModalidade;
//     }

//     public BigDecimal getAbatimentoValor() {
//         return abatimentoValor;
//     }

//     public void setAbatimentoValor(BigDecimal abatimentoValor) {
//         this.abatimentoValor = abatimentoValor;
//     }

//     public Integer getDescontoModalidade() {
//         return descontoModalidade;
//     }

//     public void setDescontoModalidade(Integer descontoModalidade) {
//         this.descontoModalidade = descontoModalidade;
//     }

//     public BigDecimal getDescontoValor() {
//         return descontoValor;
//     }

//     public void setDescontoValor(BigDecimal descontoValor) {
//         this.descontoValor = descontoValor;
//     }

//     public boolean isRetirada() {
//         return isRetirada;
//     }

//     public void setRetirada(boolean isRetirada) {
//         this.isRetirada = isRetirada;
//     }

//     public boolean isSaque() {
//         return isSaque;
//     }

//     public void setSaque(boolean isSaque) {
//         this.isSaque = isSaque;
//     }

//     public boolean isTroco() {
//         return isTroco;
//     }

//     public void setTroco(boolean isTroco) {
//         this.isTroco = isTroco;
//     }

//     public BigDecimal getValorRetirada() {
//         return valorRetirada;
//     }

//     public void setValorRetirada(BigDecimal valorRetirada) {
//         this.valorRetirada = valorRetirada;
//     }

//     public int getRetiradaModalidadeAlteracao() {
//         return retiradaModalidadeAlteracao;
//     }

//     public void setRetiradaModalidadeAlteracao(int retiradaModalidadeAlteracao) {
//         this.retiradaModalidadeAlteracao = retiradaModalidadeAlteracao;
//     }

//     public String getModalidadeAgente() {
//         return modalidadeAgente;
//     }

//     public void setModalidadeAgente(String modalidadeAgente) {
//         this.modalidadeAgente = modalidadeAgente;
//     }

//     public String getIspbPrestadorServico() {
//         return ispbPrestadorServico;
//     }

//     public void setIspbPrestadorServico(String ispbPrestadorServico) {
//         this.ispbPrestadorServico = ispbPrestadorServico;
//     }

//     public String getSolicitacaoPagador() {
//         return solicitacaoPagador;
//     }

//     public void setSolicitacaoPagador(String solicitacaoPagador) {
//         this.solicitacaoPagador = solicitacaoPagador;
//     }

//     public List<InfoAdicional> getInfoAdicionais() {
//         return infoAdicionais;
//     }

//     public void setInfoAdicionais(List<InfoAdicional> infoAdicionais) {
//         this.infoAdicionais = infoAdicionais;
//     }

//     public String getPixCopiaECola() {
//         return pixCopiaECola;
//     }

//     public void setPixCopiaECola(String pixCopiaECola) {
//         this.pixCopiaECola = pixCopiaECola;
//     }

//     public String getEndToEndId() {
//         return endToEndId;
//     }

//     public void setEndToEndId(String endToEndId) {
//         this.endToEndId = endToEndId;
//     }

//     public BigDecimal getValorPago() {
//         return valorPago;
//     }

//     public void setValorPago(BigDecimal valorPago) {
//         this.valorPago = valorPago;
//     }

//     public LocalDateTime getHorarioPagamento() {
//         return horarioPagamento;
//     }

//     public void setHorarioPagamento(LocalDateTime horarioPagamento) {
//         this.horarioPagamento = horarioPagamento;
//     }

//     public String getInfoPagador() {
//         return infoPagador;
//     }

//     public void setInfoPagador(String infoPagador) {
//         this.infoPagador = infoPagador;
//     }

//     public List<PixDevolucao> getDevolucoes() {
//         return devolucoes;
//     }

//     public void setDevolucoes(List<PixDevolucao> devolucoes) {
//         this.devolucoes = devolucoes;
//     }
// }
