package pharmacie.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pharmacie.service.ApprovisionnementService;
import pharmacie.service.ApprovisionnementService.RapportApprovisionnement;

/**
 * Contrôleur REST pour déclencher le service d'approvisionnement.
 *
 * URL : POST /api/approvisionnement/run
 * (car tu as spring.data.rest.base-path=api)
 */
@RestController
@RequestMapping("/api/approvisionnement")
public class ApprovisionnementController {

    private final ApprovisionnementService service;

    public ApprovisionnementController(ApprovisionnementService service) {
        this.service = service;
    }

    @PostMapping("/run")
    public ResponseEntity<RapportApprovisionnement> run() {
        return ResponseEntity.ok(service.lancerEtEnvoyerMails());
    }
}