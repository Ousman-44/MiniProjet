package pharmacie.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

/**
 * Service simple d'envoi d'emails via SendGrid.
 * On envoie du texte brut, sans template.
 */
@Service
public class SendGridMailService implements MailService {

    @Value("${app.sendgrid.api-key}")
    private String apiKey;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void envoyerMail(String destinataire, String sujet, String contenu) {
        Email fromEmail = new Email(from);
        Email toEmail = new Email(destinataire);
        Content content = new Content("text/plain", contenu);

        Mail mail = new Mail(fromEmail, sujet, toEmail, content);

        SendGrid sendGrid = new SendGrid(apiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 400) {
                throw new RuntimeException(
                    "Erreur SendGrid : " + response.getStatusCode() + " - " + response.getBody()
                );
            }

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'envoi du mail via SendGrid", e);
        }
    }
}