package pharmacie.service;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pharmacie.service.MailService;

import pharmacie.dao.FournisseurRepository;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Categorie;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;

/**
 * Service métier d'approvisionnement :
 *
 * 1) Trouver les médicaments à réapprovisionner (stock < niveau)
 * 2) Pour chaque médicament, trouver les fournisseurs de sa catégorie
 * 3) Envoyer UN SEUL mail par fournisseur (regroupé par catégorie)
 */
@Service
public class ApprovisionnementService {

    private final MedicamentRepository medicamentRepository;
    private final FournisseurRepository fournisseurRepository;
    private final MailService mailService;

    // Adresse d'envoi (ex: ton gmail)
    @Value("${app.mail.from:}")
    private String from;

    public ApprovisionnementService(MedicamentRepository medicamentRepository,
                                   FournisseurRepository fournisseurRepository,
                                   MailService mailService) {
        this.medicamentRepository = medicamentRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.mailService = mailService;
    }

    /**
     * Transaction readOnly : on lit seulement, on ne modifie pas la base.
     */
    @Transactional(readOnly = true)
    public RapportApprovisionnement lancerEtEnvoyerMails() {

        // 1) récupérer les médicaments à réapprovisionner
        List<Medicament> aReappro = medicamentRepository.medicamentsAReapprovisionner();

        // S'il n'y a rien à faire, on retourne un rapport vide
        if (aReappro.isEmpty()) {
            return new RapportApprovisionnement(0, 0, Map.of());
        }

        /**
         * 2) Construire un plan :
         * fournisseur -> catégorie -> liste de médicaments
         *
         * Comme ça :
         * - chaque fournisseur reçoit 1 seul mail
         * - le mail peut être regroupé par catégorie
         */
        Map<Fournisseur, Map<Categorie, List<Medicament>>> plan = new LinkedHashMap<>();

        for (Medicament m : aReappro) {
            Integer codeCat = m.getCategorie().getCode();

            // fournisseurs capables de fournir la catégorie du médicament
            List<Fournisseur> capables = fournisseurRepository.findDistinctByCategories_Code(codeCat);

            for (Fournisseur f : capables) {
                plan.computeIfAbsent(f, k -> new LinkedHashMap<>())
                    .computeIfAbsent(m.getCategorie(), k -> new ArrayList<>())
                    .add(m);
            }
        }

        // 3) Envoyer les mails
        int nbMails = 0;
        for (var entry : plan.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            envoyerMail(entry.getKey(), entry.getValue());
            nbMails++;
        }

        // 4) Construire un "rapport" simple à renvoyer en REST (preuve que ça marche)
        Map<String, Map<String, List<String>>> details = new LinkedHashMap<>();

        for (var e : plan.entrySet()) {
            String nomFournisseur = e.getKey().getNom();

            Map<String, List<String>> parCategorie = new LinkedHashMap<>();
            for (var catEntry : e.getValue().entrySet()) {
                String libelleCat = catEntry.getKey().getLibelle();
                List<String> medicaments = catEntry.getValue().stream()
                        .map(Medicament::getNom)
                        .distinct()
                        .toList();
                parCategorie.put(libelleCat, medicaments);
            }

            details.put(nomFournisseur, parCategorie);
        }

        return new RapportApprovisionnement(aReappro.size(), nbMails, details);
    }

    /**
     * Envoi d'un email texte simple via Spring Mail.
     */
    private void envoyerMail(Fournisseur fournisseur, Map<Categorie, List<Medicament>> parCategorie) {

        // Petit fallback si app.mail.from n'est pas défini en dev
        String fromAddress = (from == null || from.isBlank())
                ? "noreply@pharmacie.local"
                : from;

        String subject = "Demande de devis - Réapprovisionnement";

        // Construire le mail
        StringBuilder body = new StringBuilder();
        body.append("Bonjour ").append(fournisseur.getNom()).append(",\n\n")
            .append("Merci de nous transmettre un devis pour le réapprovisionnement des médicaments ci-dessous.\n\n");

        // Regroupement par catégorie
        for (var entry : parCategorie.entrySet()) {
            Categorie cat = entry.getKey();
            body.append("== ").append(cat.getLibelle()).append(" ==\n");

            for (Medicament m : entry.getValue()) {
                int manque = Math.max(0, m.getNiveauDeReappro() - m.getUnitesEnStock());
                body.append("- ").append(m.getNom())
                    .append(" | stock=").append(m.getUnitesEnStock())
                    .append(" | seuil=").append(m.getNiveauDeReappro())
                    .append(" | suggestion commande=").append(manque)
                    .append("\n");
            }
            body.append("\n");
        }

        body.append("Cordialement,\nLa Pharmacie\n");

        // Objet mail Spring Mail
    mailService.envoyerMail(
        fournisseur.getEmail(),
        subject,
        body.toString()
    );
    }

    /**
     * Petit DTO (record) pour renvoyer un résultat JSON propre en REST.
     */
    public record RapportApprovisionnement(
        int nbMedicamentsAReappro,
        int nbFournisseursContactes,
        Map<String, Map<String, List<String>>> details
    ) {}
}