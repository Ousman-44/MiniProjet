package pharmacie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "fournisseur",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fournisseur_email", columnNames = {"email"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fournisseur {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String nom;

  @NotBlank
  @Email
  @Column(nullable = false, unique = true)
  private String email;

  /**
   * Un fournisseur peut fournir plusieurs catégories
   * et une catégorie peut être fournie par plusieurs fournisseurs.
   */
  @ManyToMany
  @JoinTable(
      name = "fournisseur_categories",
      joinColumns = @JoinColumn(name = "fournisseur_id"),
      inverseJoinColumns = @JoinColumn(name = "categorie_code")
  )
  @Builder.Default
  @JsonIgnore // évite boucles JSON si tu exposes directement les entités
  private Set<Categorie> categories = new HashSet<>();
}
