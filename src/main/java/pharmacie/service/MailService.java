package pharmacie.service;

public interface MailService {
    void envoyerMail(String destinataire, String sujet, String contenu);
}