package pharmacie.dao;

import pharmacie.entity.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Integer> {

    /**
     * Retourne tous les fournisseurs capables de fournir une catégorie donnée
     */
    List<Fournisseur> findDistinctByCategories_Code(Integer code);

    /**
     * Retourne tous les fournisseurs capables de fournir plusieurs catégories
     */
    List<Fournisseur> findDistinctByCategories_CodeIn(List<Integer> categorieCodes);

    /**
     * Trouver un fournisseur par email
     */
    Fournisseur findByEmail(String email);
}