package org.acme.repository;

import org.acme.model.PixImediato;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PixImediatoRepository implements PanacheRepository<PixImediato> {

    /**
     * Busca um PixImediato pelo TxID
     * 
     * @param txid ID da transação
     * @return PixImediato correspondente ou null se não encontrado
     */
    public PixImediato findByTxId(String txid) {
        return find("txid", txid).firstResult();
    }

    /**
     * Obtém o último ID da tabela de Pix (entidade abstrata que é pai de
     * PixImediato e PixComVencimento)
     * 
     * @return O último ID ou 0 se não houver registros
     */
    public Long obterUltimoId() {
        // Buscar o maior ID da tabela Pix, retornando 0 se for NULL (tabela vazia)
        Long maxId = getEntityManager()
                .createQuery("SELECT MAX(p.id) FROM Pix p", Long.class)
                .getSingleResult();

        return maxId != null ? maxId : 0L;
    }
}
