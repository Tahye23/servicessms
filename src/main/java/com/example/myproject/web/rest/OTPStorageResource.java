package com.example.myproject.web.rest;

import com.example.myproject.domain.Abonnement;
import com.example.myproject.domain.Abonnement; // ajuster en fonction du package réel
import com.example.myproject.domain.Abonnement;
import com.example.myproject.domain.Application;
import com.example.myproject.domain.ExtendedUser;
import com.example.myproject.domain.OTPStorage;
import com.example.myproject.domain.SendSms; // Remplacez par le bon chemin de votre classe SendSms
import com.example.myproject.domain.TokensApp;
import com.example.myproject.repository.AbonnementRepository;
import com.example.myproject.repository.AbonnementRepository;
import com.example.myproject.repository.ApplicationRepository;
import com.example.myproject.repository.OTPStorageRepository;
import com.example.myproject.repository.SendSmsRepository;
import com.example.myproject.repository.TokensAppRepository;
import com.example.myproject.service.SendSmsService;
import com.example.myproject.web.rest.dto.OTPDetails;
import com.example.myproject.web.rest.dto.Otp;
import com.example.myproject.web.rest.dto.OtpRequest;
import com.example.myproject.web.rest.dto.OtpRetour;
import com.example.myproject.web.rest.dto.OtpVerify;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.List; // Ajouter cette ligne pour importer List
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.example.myproject.domain.OTPStorage}.
 */
@RestController
@RequestMapping("/api/otp-storages")
@Transactional
public class OTPStorageResource {

    private static final int OTP_LENGTH = 6;

    private final Logger log = LoggerFactory.getLogger(OTPStorageResource.class);

    private static final String ENTITY_NAME = "oTPStorage";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OTPStorageRepository oTPStorageRepository;
    private final TokensAppRepository tokensAppRepository;
    private final SendSmsRepository sendSmsRepository;
    private SendSmsResource sendSmsResource;

    // Injection de SendSmsResource
    private final SendSmsService sendSmsService;
    private AbonnementRepository abonnementRepository;
    private ApplicationRepository applicationRepository;

    public OTPStorageResource(
        OTPStorageRepository oTPStorageRepository,
        SendSmsRepository sendSmsRepository,
        SendSmsResource sendSmsResource,
        TokensAppRepository tokensAppRepository,
        AbonnementRepository abonnementRepository,
        SendSmsService sendSmsService,
        ApplicationRepository applicationRepository
    ) {
        this.sendSmsResource = sendSmsResource;
        this.sendSmsRepository = sendSmsRepository;
        this.abonnementRepository = abonnementRepository;
        this.oTPStorageRepository = oTPStorageRepository;
        this.sendSmsResource = sendSmsResource;
        this.tokensAppRepository = tokensAppRepository;
        this.sendSmsService = sendSmsService;
        this.applicationRepository = applicationRepository;
    }

    /**
     * {@code POST  /otp-storages} : Create a new oTPStorage.
     *
     * @param oTPStorage the oTPStorage to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new oTPStorage, or with status {@code 400 (Bad Request)} if the oTPStorage has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OTPStorage> createOTPStorage(@RequestBody OTPStorage oTPStorage) throws URISyntaxException {
        log.debug("REST request to save OTPStorage : {}", oTPStorage);
        if (oTPStorage.getId() != null) {
            throw new BadRequestAlertException("A new oTPStorage cannot already have an ID", ENTITY_NAME, "idexists");
        }
        oTPStorage = oTPStorageRepository.save(oTPStorage);
        return ResponseEntity.created(new URI("/api/otp-storages/" + oTPStorage.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, oTPStorage.getId().toString()))
            .body(oTPStorage);
    }

    public String generateOtpUser(ExtendedUser utilisateur) {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder otpCode = new StringBuilder();

        for (int i = 0; i < OTP_LENGTH; i++) {
            otpCode.append(secureRandom.nextInt(10)); // Génère un chiffre entre 0 et 9
        }

        return otpCode.toString(); // Retourne l'OTP généré
    }

    // @PostMapping("/gener/{id}")
    // public ResponseEntity<OTPStorage> generateOtp(@PathVariable Long id) {
    //      Optional<OTPStorage> otps = this.oTPStorageRepository.findById(id);

    //      if (otps.isPresent()) {
    //          OTPStorage OTPS = otps.orElse(null);
    //          if (OTPS.getUser() != null) {
    //             ExtendedUser user = OTPS.getUser();
    //              String otp = generateOtpUser(user);

    //              OTPS.setOtsOTP(otp);
    //             oTPStorageRepository.save(OTPS);
    //           return ResponseEntity.ok("OTP Generated: " + OTPS.getOtsOTP());

    //          }
    //      }

    //     }

    private ResponseEntity<Map<String, Object>> createResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("status", status.value());
        return new ResponseEntity<>(response, status);
    }

    @PostMapping("/gener/{id}")
    public ResponseEntity<Map<String, Object>> generateOtp(@PathVariable Long id) {
        Optional<OTPStorage> otps = this.oTPStorageRepository.findById(id);
        ZoneId zoneId = ZoneId.of("Africa/Nouakchott");

        int expirationMinutes = 5;
        if (otps.isPresent()) {
            OTPStorage OTPS = otps.orElse(null);
            if (OTPS.getUser() != null) {
                ExtendedUser user = OTPS.getUser();
                String otp = generateOtpUser(user);
                ZonedDateTime expirationTime = LocalDateTime.now().plusMinutes(expirationMinutes).atZone(zoneId);
                OTPS.setOtsOTP(otp);
                OTPS.setOtsdateexpir(expirationTime);
                oTPStorageRepository.save(OTPS);

                return createResponse("OTP généré avec succès", HttpStatus.OK);
                // return ResponseEntity.ok(OTPS); // Retourne bien un OTPStorage comme défini dans la signature
            }
            return createResponse("Utilisateur non trouvé.", HttpStatus.NOT_FOUND);
        }
        return createResponse("OTP avec l'ID " + id + " non trouvé.", HttpStatus.NOT_FOUND);
        // return ResponseEntity.notFound().build(); // Gère le cas où l'ID n'existe pas ou `getUser()` est null
    }

    @PostMapping("/generate/{id}/{expirationMinutes}")
    public ResponseEntity<String> generateOTPEXP(
        @RequestHeader("token") String token,
        @PathVariable(value = "id", required = true) final Integer id,
        @RequestBody Otp otp
    ) {
        int expirationMinutes = 5;

        // Vérification de la présence du token et de l'application
        Optional<TokensApp> tokensApp = this.tokensAppRepository.findByApplication_IdAndToken(id, token);
        Optional<Application> applicationOpt = applicationRepository.findById(id);

        // Si le token et l'application existent
        if (tokensApp.isPresent() && applicationOpt.isPresent()) {
            Application app = applicationOpt.orElse(null);
            ExtendedUser utilisateur = app.getUtilisateur();

            // Génération du code OTP pour l'utilisateur
            OTPStorage otpEntity = this.generateOtpForUser(utilisateur);

            // Création de l'objet OtpRetour pour envoyer la réponse
            String message =
                "OTP généré pour votre application " +
                id +
                ", valable pendant " +
                expirationMinutes +
                " minutes. Il est : " +
                otpEntity.getOtsOTP() +
                ". Voici l'identifiant de l'OTP généré : " +
                otpEntity.getId();

            log.error(message);
            return new ResponseEntity<>(message, HttpStatus.OK);
            //OtpRetour otpRetour = new OtpRetour();
            //otpRetour.setCode("0"); // Code de succès (ou autre selon la logique)
            //otpRetour.setPhoneNumber(otpEntity.getPhoneNumber());
            // otpRetour.setMessage(
            //   "OTP généré pour votre application " +
            // id +
            //" valable pendant " +
            //expirationMinutes +
            //" minutes. Il est : " +
            //otpEntity.getOtsOTP()
            //);
            //otpRetour.setOtp_id(otpEntity.getId()); // ID de l'OTP ou autre info pertinente

            // Retour de la réponse avec succès
            //return ResponseEntity.ok(otpRetour);
        }
        String message = " Veuillez vérifier l'identifiant de l'application ainsi que le token fourni.";

        log.error(message);
        return new ResponseEntity<>(message, HttpStatus.UNAUTHORIZED);
        // Si le token ou l'application est manquant, retour d'un message d'erreur
        //OtpRetour otpRetourError = new OtpRetour();
        //otpRetourError.setCode("400");
        //otpRetourError.setMessage("Token or application missing, please provide both");
        //otpRetourError.setOtp_id(null); // Valeur null pour otp_id en cas d'erreur

        // Retour de la réponse d'erreur
        //return ResponseEntity.ok(otpRetourError);
    }

    public boolean validateApiKey(String apikey) {
        // Rechercher la clé API dans la base de données
        Optional<TokensApp> toknsapp = tokensAppRepository.findByToken(apikey);

        // Retourner true si la clé API est valide et active
        return toknsapp.isPresent();
    }

    public void sendOtpById(Long id) {
        // Récupérer l'objet OTP par ID

        // Remplir les champs de l'entité SendSms

        // Appeler la fonction pour envoyer le SMS
        // smsService.sendSms(sendSmsEntity.getReceiver(), sendSmsEntity.getMsgData());
    }

    /* private boolean estAbonne(Integer userId) {
        Optional<Abonnement> abonnement12 = abonnementRepository.findByAboUser_Id(userId);
        return abonnement12.isPresent();
    }*/
    /*
    @PostMapping("/sendUI/{id}")
    public ResponseEntity<Map<String, Object>> sendOtp(@PathVariable Long id) {
        try {
            // Récupérer l'objet OTP par ID
            Optional<OTPStorage> otp = oTPStorageRepository.findById(id);
            SendSms sendSms = new SendSms();

            if (otp.isPresent()) {
                OTPStorage OTPS = otp.orElse(null);
                Integer userId = OTPS.getUser().getId(); // Assurez-vous que l'utilisateur est accessible depuis SendSms

                // Vérifier si l'utilisateur a un abonnement actif
                if (!estAbonne(userId)) {
                    return createResponse("L'utilisateur n'a pas d'abonnement actif.", HttpStatus.NOT_FOUND);
                }

                // Récupérer l'abonnement de l'utilisateur
                Optional<Abonnement> abonnementOpt = abonnementRepository.findByAboUser_Id(userId);
                if (!abonnementOpt.isPresent()) {
                    return createResponse("Abonnement non trouvé pour cet utilisateur.", HttpStatus.NOT_FOUND);
                }

                Abonnement abonnement = abonnementOpt.orElseThrow(() -> new IllegalArgumentException("Abonnement non trouvé"));

                // Vérifier si le compteur d'abonnement est disponible
                if (abonnement.getAboCompteur() == null || abonnement.getAboCompteur() <= 0) {
                    return createResponse(
                        "Impossible d'envoyer le SMS, compteur d'abonnement épuisé ou non défini. Veuillez renouveler votre abonnement.",
                        HttpStatus.BAD_REQUEST
                    );
                }

                // Préparation et envoi du SMS
                sendSms.setMsgdata("Votre code est " + OTPS.getOtsOTP()); // Remplir le message avec le code OTP
                sendSms.setReceiver(OTPS.getPhoneNumber()); // Récupérer le numéro de téléphone de l'utilisateur
                sendSms.setSender("Richatt");

                // Sauvegarde et envoi du SMS
                sendSmsRepository.save(sendSms);
                boolean result1 = sendSmsResource.insertSms(sendSms);

                // Décrémenter le compteur si l'envoi est réussi
                if (result1) {
                    abonnement.setAboCompteur(abonnement.getAboCompteur() - 1);
                    abonnementRepository.save(abonnement);
                }

                return createResponse("OTP envoyé avec succès.", HttpStatus.OK);
            } else {
                return createResponse("OTP avec l'ID " + id + " non trouvé.", HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Collections.singletonMap("error", "Erreur: " + e.getMessage())
            );
        }
    }*/

    // @PostMapping("/sendUI/{id}")
    // public ResponseEntity<Map<String, Object>> sendOtp(@PathVariable Long id) {
    //     try {
    //         // Récupérer l'objet OTP par ID
    //         Optional<OTPStorage> otp = oTPStorageRepository.findById(id);

    //         SendSms sendSms = new SendSms();

    //         if (otp.isPresent()) {
    //             OTPStorage OTPS = otp.orElse(null);
    //             Integer userId = OTPS.getUser().getId(); // Assurez-vous que l'utilisateur est accessible depuis SendSms
    //             if (!estAbonne(userId)&& ) {
    //                 return createResponse("L'utilisateur n'a pas d'abonnement actif.", HttpStatus.NOT_FOUND);
    //                 // return ResponseEntity.status(HttpStatus.FORBIDDEN).body("L'utilisateur n'a pas d'abonnement actif."); // Retourne 403 si l'utilisateur n'est pas abonné
    //             }
    //             sendSms.setMsgdata("Votre code  est " + OTPS.getOtsOTP()); // Remplir le message avec le code OTP
    //             sendSms.setReceiver(OTPS.getPhoneNumber()); // Récupérer le numéro de téléphone de l'utilisateur
    //             sendSms.setSender("Richatt");

    //             // Sauvegarder le SMS et insérer via SendSmsResource
    //             sendSmsRepository.save(sendSms);
    //             // sendSmsResource.insertSms(sendSms);
    //             boolean result1 = sendSmsResource.insertSms(sendSms);
    //                if (result1) {
    //                         abonnement.setAboCompteur(abonnement.getAboCompteur() - 1);
    //                         abonnementRepository.save(abonnement);
    //                     }

    //              return createResponse("OTP envoyé avec succès.", HttpStatus.OK);

    //             // return ResponseEntity.ok("OTP envoyé avec succès.");
    //         } else {
    //             // Cas où l'OTP avec cet ID n'existe pas
    //             // return ResponseEntity.status(404).body("OTP non trouvé pour l'ID fourni.");
    //             return createResponse("OTP avec l'ID " + id + " non trouvé.", HttpStatus.NOT_FOUND);

    //         }
    //     } catch (Exception e) {
    //         // return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
    //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur: " + e.getMessage()));

    //     }
    // }

    //   @PostMapping("/send")
    // public ResponseEntity<String> sendOtp(@RequestHeader("id") Long id) {
    //     try {
    //         // Récupérer l'objet OTP par ID
    //         Optional<OTPStorage> otp = oTPStorageRepository.findById(id);

    //         SendSms sendSms = new SendSms();

    //         if (otp.isPresent()) {
    //             OTPStorage OTPS = otp.orElse(null);
    //             Integer userId = OTPS.getUser().getId(); // Assurez-vous que l'utilisateur est accessible depuis SendSms
    //             if (!estAbonne(userId)) {
    //                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("L'utilisateur n'a pas d'abonnement actif."); // Retourne 403 si l'utilisateur n'est pas abonné
    //             }
    //             sendSms.setMsgdata("Votre code OTP est " + OTPS.getOtsOTP()); // Remplir le message avec le code OTP
    //             sendSms.setReceiver(OTPS.getPhoneNumber()); // Récupérer le numéro de téléphone de l'utilisateur
    //             sendSms.setSender("VotreNom");

    //             // Sauvegarder le SMS et insérer via SendSmsResource
    //             sendSmsRepository.save(sendSms);
    //             sendSmsResource.insertSms(sendSms);

    //             return ResponseEntity.ok("OTP envoyé avec succès.");
    //         } else {
    //             // Cas où l'OTP avec cet ID n'existe pas
    //             return ResponseEntity.status(404).body("OTP non trouvé pour l'ID fourni.");
    //         }
    //     } catch (Exception e) {
    //         return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
    //     }
    // }

    // Méthode pour valider la clé API depuis la base de données

    // Endpoint POST pour générer un OTP

    public OTPStorage generateOtpForUser(ExtendedUser utilisateur) {
        // Utilisation de SecureRandom pour générer un OTP sécurisé
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder otpCode = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otpCode.append(secureRandom.nextInt(10)); // Génère un chiffre entre 0 et 9
        }

        // Définition du fuseau horaire
        ZoneId zoneId = ZoneId.of("Africa/Nouakchott");

        // Définition du délai d'expiration
        int expirationMinutes = 5;
        ZonedDateTime expirationTime = LocalDateTime.now().plusMinutes(expirationMinutes).atZone(zoneId);

        // Création d'une nouvelle instance d'OTPStorage
        OTPStorage OTP = new OTPStorage();
        OTP.setOtsOTP(otpCode.toString());
        OTP.setOtsdateexpir(expirationTime);
        OTP.setIsExpired(false);
        OTP.setIsOtpUsed(false);
        OTP.setUser(utilisateur);
        //OTP.setPhoneNumber(otp.getPhoneNumber());
        // Sauvegarde dans le repository
        oTPStorageRepository.save(OTP);

        return OTP;
    }

    @PostMapping("/generate/{id}")
    public ResponseEntity<OTPDetails> generateOTP(
        @RequestHeader("token") String token,
        @PathVariable(value = "id", required = true) final Integer id
    ) {
        int expirationMinutes = 5;
        OTPDetails otpDetails = new OTPDetails(); // Assurez-vous que cette classe est correctement définie

        // Vérification de la présence du token et de l'application
        Optional<TokensApp> tokensApp = this.tokensAppRepository.findByApplication_IdAndToken(id, token);
        Optional<Application> applicationOpt = applicationRepository.findById(id);

        // Si le token et l'application existent
        if (tokensApp.isPresent() && applicationOpt.isPresent()) {
            Application app = applicationOpt.orElse(null);
            ExtendedUser utilisateur = app.getUtilisateur();

            // Génération du code OTP pour l'utilisateur
            OTPStorage otpEntity = this.generateOtpForUser(utilisateur);

            // Création de l'objet OtpRetour pour envoyer la réponse
            String message =
                "OTP généré avec succès pour l'application " +
                id +
                ". Identifiant OTP : " +
                otpEntity.getId() +
                ". Expire à : " +
                otpEntity.getOtsdateexpir().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                ".";
            otpDetails.setOtp(otpEntity.getOtsOTP());
            otpDetails.setMessage("OTP généré avec succès pour l'application " + id);
            otpDetails.setExpirationDate(otpEntity.getOtsdateexpir().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            //otpDetails.setExpirationDate(otpEntity.getOtsdateexpir().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return new ResponseEntity<OTPDetails>(otpDetails, HttpStatus.CREATED);
        }
        String message = " Veuillez vérifier l'identifiant de l'application ainsi que le token fourni.";
        otpDetails.setOtp("");
        otpDetails.setMessage(message);
        otpDetails.setExpirationDate("");
        log.error(message);
        //return new ResponseEntity<>(message, HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<OTPDetails>(otpDetails, HttpStatus.UNAUTHORIZED);
        //return new ResponseEntity<>(message, HttpStatus.UNAUTHORIZED);
    }

    /* public boolean isAbonnementExists(Integer applicationId) {
        // Vérifie s'il existe un abonnement actif pour l'applicationId donné
        return abonnementRepository.existsByApplication_Id(applicationId);
    }*/

    @PostMapping("/send/{id}")
    /*  public ResponseEntity<String> sendOtp(
        @RequestHeader("token") String token,
        @PathVariable(value = "id", required = true) final Integer id,
        @RequestBody OtpRequest otpRequest
    ) {
        Optional<TokensApp> tokensApp = this.tokensAppRepository.findByApplication_IdAndToken(id, token);
        Optional<Application> applicationOptt = applicationRepository.findById(id);

        Application app1 = applicationOptt.orElse(null);
        ExtendedUser utilisateur = app1.getUtilisateur();
        if (tokensApp.isPresent()) {
            Optional<OTPStorage> otpRecord = oTPStorageRepository.findByOtsOTP(otpRequest.getCodeotp());
            if (otpRecord.isPresent()) {
                OTPStorage Otpins = otpRecord.orElse(null);
                SendSms sendSms = new SendSms();
                if (otpRequest.getCodeotp().equals(Otpins.getOtsOTP())) {
                    Optional<Abonnement> abonnementOpt = abonnementRepository.findByApplication_Id(id);
                    if (isAbonnementExists(id) && abonnementOpt.isPresent()) {
                        Abonnement abonnement = abonnementOpt.orElse(null);
                        if (abonnement.getAboCompteur() == null || abonnement.getAboCompteur() <= 0) {
                            String message =
                                "Impossible d'envoyer le SMS ! le compteur d'abonnement est épuisé ou non défini. Veuillez renouveler votre abonnement pour pouvoir continuer";
                            log.error(message);
                            return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
                        }

                        sendSms.setMsgdata("Votre code  est " + Otpins.getOtsOTP());
                        sendSms.setSender(otpRequest.getSender());
                        sendSms.setReceiver(otpRequest.getPhoneNumber()); // Récupérer le numéro de téléphone de l'utilisateur
                        sendSmsRepository.save(sendSms);
                        SendSms result = sendSmsService.save(sendSms);

                        if (result != null) {
                            //abonnementRepository.save(abonnement);
                            //abonnementService.update(abonnement);
                            sendSmsResource.insertSms(sendSms);
                            abonnement.setAboCompteur(abonnement.getAboCompteur() - 1);
                            Otpins.setPhoneNumber(otpRequest.getPhoneNumber());
                            oTPStorageRepository.save(Otpins);

                            abonnementRepository.save(abonnement);

                            return ResponseEntity.ok("SMS envoyé avec succès !  compteur mis à jour");
                        }
                        String message = "erreur lors de l'envoie ";
                        return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
                        //oTPStorageRepository.save(Otpins);
                        //return ResponseEntity.ok("OTP envoyés avec succès!  compteur a été mis à jour");
                        //String message = "Impossible d'envoyer le SMS, application non abonnée ";
                        //log.error(message);

                        //return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
                    }
                    String message = "Impossible d'envoyer le SMS, application non abonnée ";
                    log.error(message);
                    return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
                    //OTPStorage otpEntity = otpRecord.orElse(null);

                }
                return ResponseEntity.ok("l'otp incorrect");
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP not found ");
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            "Veuillez vérifier l'identifiant de l'application ainsi que le jeton fourni."
        );
    }*/

    /**
     * {@code PUT  /otp-storages/:id} : Updates an existing oTPStorage.
     *
     * @param id the id of the oTPStorage to save.
     * @param oTPStorage the oTPStorage to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated oTPStorage,
     * or with status {@code 400 (Bad Request)} if the oTPStorage is not valid,
     * or with status {@code 500 (Internal Server Error)} if the oTPStorage couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OTPStorage> updateOTPStorage(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody OTPStorage oTPStorage
    ) throws URISyntaxException {
        log.debug("REST request to update OTPStorage : {}, {}", id, oTPStorage);
        if (oTPStorage.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, oTPStorage.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!oTPStorageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        oTPStorage = oTPStorageRepository.save(oTPStorage);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, oTPStorage.getId().toString()))
            .body(oTPStorage);
    }

    @PutMapping("/verify/{id}")
    public ResponseEntity<String> verifyOtp(
        @RequestHeader("token") String token,
        @PathVariable(value = "id", required = true) final Integer id,
        @RequestBody OtpVerify otpVerify
    ) {
        Optional<OTPStorage> otpOptional = oTPStorageRepository.findByOtsOTP(otpVerify.getCode());
        Optional<TokensApp> tokensApp = tokensAppRepository.findByApplication_IdAndToken(id, token);
        Optional<Application> appOptional = applicationRepository.findById(id);

        ZonedDateTime now = ZonedDateTime.now(); // Obtient l'heure actuelle avec fuseau horaire

        if (otpOptional.isPresent() && tokensApp.isPresent()) {
            OTPStorage otpv = otpOptional.orElse(null);
            Application app = appOptional.orElse(null);
            ExtendedUser utilisateur = app.getUtilisateur();

            // Vérification séparée pour chaque cas d'erreur
            if (!otpv.getOtsOTP().equals(otpVerify.getCode())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Code invalide.");
            }
            if (!otpv.getPhoneNumber().equals(otpVerify.getPhoneNumber())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(" phonenumber invalide.");
            }

            if (!otpv.getUser().equals(utilisateur)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Utilisateur incorrect.");
            }

            if (otpv.getIsOtpUsed().equals(true)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("OTP déjà utilisé.");
            }

            if (now.isAfter(otpv.getOtsdateexpir())) {
                otpv.setIsExpired(true); // Marquer comme expiré
                oTPStorageRepository.save(otpv);
                return ResponseEntity.status(HttpStatus.GONE).body("Code expiré.");
            }

            // Toutes les vérifications réussies, marquer l'OTP comme utilisé
            otpv.setIsOtpUsed(true);
            oTPStorageRepository.save(otpv);
            return ResponseEntity.ok("Code vérifié avec succès!");
        }

        // Vérifications séparées pour les cas où l'OTP, le token ou l'application sont absents
        if (!otpOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Code n'existe pas.");
        }

        if (!tokensApp.isPresent() && !appOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                "Veuillez vérifier l'identifiant de l'application ainsi que le jeton fourni. "
            );
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            "Veuillez vérifier l'identifiant de l'application ainsi que le jeton fourni. "
        );
    }

    /**
     * {@code PATCH  /otp-storages/:id} : Partial updates given fields of an existing oTPStorage, field will ignore if it is null
     *
     * @param id the id of the oTPStorage to save.
     * @param oTPStorage the oTPStorage to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated oTPStorage,
     * or with status {@code 400 (Bad Request)} if the oTPStorage is not valid,
     * or with status {@code 404 (Not Found)} if the oTPStorage is not found,
     * or with status {@code 500 (Internal Server Error)} if the oTPStorage couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OTPStorage> partialUpdateOTPStorage(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody OTPStorage oTPStorage
    ) throws URISyntaxException {
        log.debug("REST request to partial update OTPStorage partially : {}, {}", id, oTPStorage);
        if (oTPStorage.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, oTPStorage.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!oTPStorageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OTPStorage> result = oTPStorageRepository
            .findById(oTPStorage.getId())
            .map(existingOTPStorage -> {
                if (oTPStorage.getOtsOTP() != null) {
                    existingOTPStorage.setOtsOTP(oTPStorage.getOtsOTP());
                }
                if (oTPStorage.getPhoneNumber() != null) {
                    existingOTPStorage.setPhoneNumber(oTPStorage.getPhoneNumber());
                }
                if (oTPStorage.getOtsdateexpir() != null) {
                    existingOTPStorage.setOtsdateexpir(oTPStorage.getOtsdateexpir());
                }

                return existingOTPStorage;
            })
            .map(oTPStorageRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, oTPStorage.getId().toString())
        );
    }

    /**
     * {@code GET  /otp-storages} : get all the oTPStorages.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of oTPStorages in body.
     */
    @GetMapping("")
    public ResponseEntity<List<OTPStorage>> getAllOTPStorages(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of OTPStorages");
        Page<OTPStorage> page = oTPStorageRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /otp-storages/:id} : get the "id" oTPStorage.
     *
     * @param id the id of the oTPStorage to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the oTPStorage, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OTPStorage> getOTPStorage(@PathVariable("id") Long id) {
        log.debug("REST request to get OTPStorage : {}", id);
        Optional<OTPStorage> oTPStorage = oTPStorageRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(oTPStorage);
    }

    /**
     * {@code DELETE  /otp-storages/:id} : delete the "id" oTPStorage.
     *
     * @param id the id of the oTPStorage to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOTPStorage(@PathVariable("id") Long id) {
        log.debug("REST request to delete OTPStorage : {}", id);
        oTPStorageRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
