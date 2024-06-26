package com.walletApp.backend.controller.API;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.walletApp.backend.model.*;
import com.walletApp.backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/compte")
public class CompteController {

    @Autowired
    private CompteService service;

    @Autowired
    private StatusService statusService;

    @Autowired
    private TypeCompteService typeCompteService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TypeTransactionService typeTransactionService;

    @GetMapping
    public ResponseEntity<List<Compte>> getAllComptes() {
        List<Compte> compte = service.getAllComptes();
        return ResponseEntity.ok(compte);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Compte> getCompteById(@PathVariable String id) {
        System.out.println("Fetching compte with ID: " + id);
        Optional<Compte> compte = service.getCompteById(id);

        if (compte.isPresent()) {
            System.out.println("Compte found: " + compte.get());
            return ResponseEntity.ok(compte.get());
        } else {
            System.out.println("Compte not found for ID: " + id);
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/{userId}") // Assurez-vous que l'annotation est PostMapping
    public ResponseEntity<Compte> createCompte(@PathVariable int userId, @RequestBody Compte compte) {
        // Vérifiez que l'ID de l'utilisateur est valide
        if (userId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // Vous pouvez maintenant utiliser l'ID de l'utilisateur pour associer le compte
        // à cet utilisateur lors de sa création
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId_user(userId);
        compte.setUser(utilisateur);

        Compte createdCompte = service.createOrUpdateCompte(compte);
        return ResponseEntity.ok(createdCompte);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Compte> updateCompte(@PathVariable String id, @RequestBody Compte compte) {
        Optional<Compte> existingCompte = service.getCompteById(id);
        if (existingCompte.isPresent()) {
            Compte updatedCompte = service.createOrUpdateCompte(compte);
            return ResponseEntity.ok(updatedCompte);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompte(@PathVariable String id) {
        service.deleteCompte(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/depot")
    public boolean depot(@RequestParam String cpt_exp, @RequestParam String cpt_des, @RequestParam Double montant) {

        boolean result = service.actualiseSoldCompte(cpt_exp, montant, false) ? service.actualiseSoldCompte(cpt_des, montant, true) : false;
        if (result) {

            Optional<Compte> dest = service.getCompteById(cpt_des);
            Optional<Compte> exp = service.getCompteById(cpt_exp);
            Optional<Status> st = statusService.findById(1);
            Optional<TypeCpt> tc = typeCompteService.getTypeCptById(2);
            Optional<TypeTransaction> tt = typeTransactionService.findById(1);

            LocalDate dateCreation = LocalDate.now();
            Double frais = montant * 0.1;


            Transaction transaction = new Transaction();
            transaction.setCpt_dest(dest.orElseGet(null));
            transaction.setCpt_exp(exp.orElseGet(null));
            transaction.setStatus(st.orElseGet(null));
            transaction.setLieu(tc.orElseGet(null));
            transaction.setTypeTransaction(tt.orElseGet(null));
            transaction.setDate_trans(Date.valueOf(dateCreation));
            transaction.setTime_trans(Time.valueOf(LocalTime.now()));
            transaction.setFrais_trans(frais);
            transaction.setMontant_trans(montant);

            transactionService.save(transaction);
        }
        return result;
    }


    @PostMapping("/transfer")
    public ResponseEntity<Map<String, String>> transfer(@RequestParam String sourceCompte, @RequestParam String destinationCompte, @RequestParam Double montant) {
        boolean result = service.transfererFonds(sourceCompte, destinationCompte, montant);
        if (result) {
            Optional<Compte> dest = service.getCompteById(destinationCompte);
            Optional<Compte> exp = service.getCompteById(sourceCompte);
            Optional<Status> st = statusService.findById(1); // Assume status 1 means successful
            Optional<TypeCpt> tc = typeCompteService.getTypeCptById(1); // Assume type 2 means transfer
            Optional<TypeTransaction> tt = typeTransactionService.findById(3); //

            LocalDate dateCreation = LocalDate.now();
            Double frais = montant * 0.1;

            Transaction transaction = new Transaction();
            transaction.setCpt_dest(dest.orElseGet(null));
            transaction.setCpt_exp(exp.orElseGet(null));
            transaction.setStatus(st.orElseGet(null));
            transaction.setLieu(tc.orElseGet(null));
            transaction.setTypeTransaction(tt.orElseGet(null));
            transaction.setDate_trans(Date.valueOf(dateCreation));
            transaction.setTime_trans(Time.valueOf(LocalTime.now()));
            transaction.setFrais_trans(frais);
            transaction.setMontant_trans(montant);

            transactionService.save(transaction);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Le transfert a été effectué avec succès.");
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Le transfert a échoué.");
            return ResponseEntity.badRequest().body(response);
        }
    }


    @GetMapping("/verifier/{numeroCompte}")
    public ResponseEntity<Map<String, Object>> verifierCompte(@PathVariable String numeroCompte) {
        Optional<Compte> compteOptional = service.getCompteById(numeroCompte);
        if (compteOptional.isPresent()) {
            Compte compte = compteOptional.get();

            Utilisateur utilisateur = compte.getUser();

            Map<String, Object> response = new HashMap<>();
            response.put("compte", compte);
            response.put("user", utilisateur);
            return ResponseEntity.ok(response);
        } else {

            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/retrait")
    public ResponseEntity<Map<String, String>> retrait(@RequestParam String cpt_exp, @RequestParam String cpt_des, @RequestParam Double montant) {
        Optional<Compte> exp = service.getCompteById(cpt_exp);
        Optional<Compte> dest = service.getCompteById(cpt_des);

        if (!exp.isPresent() || !dest.isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Un ou les deux comptes n’existent pas . ");
            return ResponseEntity.badRequest().body(response);
        }

        if (exp.get().getSolde() < montant) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Fonds insuffisants sur le compte de " + exp.get().getAgence().getLib_agence());
            return ResponseEntity.badRequest().body(response);
        } else if (dest.get().getSolde() < montant) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Fonds insuffisants sur le compte de" + dest.get().getUser().getNom() + " " +dest.get().getUser().getPrenoms());
            return ResponseEntity.badRequest().body(response);
        }


        Optional<Status> st = statusService.findById(3);
        Optional<TypeCpt> tc = typeCompteService.getTypeCptById(2);
        Optional<TypeTransaction> tt = typeTransactionService.findById(2);

        LocalDate dateCreation = LocalDate.now();
        Double frais = montant * 0.0;

        Transaction transaction = new Transaction();
        transaction.setCpt_dest(dest.orElse(null));
        transaction.setCpt_exp(exp.orElse(null));
        transaction.setStatus(st.orElse(null));
        transaction.setLieu(tc.orElse(null));
        transaction.setTypeTransaction(tt.orElse(null));
        transaction.setDate_trans(Date.valueOf(dateCreation));
        transaction.setTime_trans(Time.valueOf(LocalTime.now()));
        transaction.setFrais_trans(frais);
        transaction.setMontant_trans(montant);

        transactionService.save(transaction);


        notificationService.createNotification(dest.get().getUser(), "Veillez confirmer le retrait en attente de "+montant+" XOF de "+exp.get().getAgence().getLib_agence()+" ", transaction);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Retrait en attente de confirmation de "+ dest.get().getUser().getNom() + " "+dest.get().getUser().getPrenoms());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/solde")
    public ResponseEntity<Map<String, Object>> getSolde(@RequestParam String numCpt) {
        Optional<Compte> compteOptional = service.getCompteById(numCpt);
        if (compteOptional.isPresent()) {
            Compte compte = compteOptional.get();
            Map<String, Object> response = new HashMap<>();
            response.put("solde", compte.getSolde());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
