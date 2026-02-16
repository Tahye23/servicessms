package com.example.myproject.service;

import com.example.myproject.domain.Contact;
import com.example.myproject.domain.enumeration.TextOp;
import com.example.myproject.repository.GroupedecontactRepository;
import com.example.myproject.service.dto.AdvancedFiltersPayload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GroupeContactSearchService {

    private final GroupedecontactRepository repo;

    public GroupeContactSearchService(GroupedecontactRepository repo) {
        this.repo = repo;
    }

    private static String n(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    public Page<Contact> search(Long groupeId, AdvancedFiltersPayload p, Pageable pageable) {
        String nom = n(p.getNom());
        String prenom = n(p.getPrenom());
        String telephone = n(p.getTelephone());

        String nomOp = TextOp.fromNullable(p.getNomFilterType(), TextOp.CONTAINS).sqlKey();
        String prenomOp = TextOp.fromNullable(p.getPrenomFilterType(), TextOp.CONTAINS).sqlKey();
        String telephoneOp = TextOp.fromNullable(p.getTelephoneFilterType(), TextOp.CONTAINS).sqlKey();

        Integer statut = p.getStatut();
        Boolean hasWhatsapp = p.getHasWhatsapp();
        Integer minSmsSent = p.getMinSmsSent();
        Integer maxSmsSent = p.getMaxSmsSent();
        Integer minWhatsappSent = p.getMinWhatsappSent();
        Integer maxWhatsappSent = p.getMaxWhatsappSent();
        Boolean hasReceived = p.getHasReceivedMessages();

        Long campaignId = p.getCampaignId();
        String smsStatus = n(p.getSmsStatus());
        String deliveryStatus = n(p.getDeliveryStatus());
        String lastError = n(p.getLastErrorContains());

        boolean hasCampaignFilters = campaignId != null || smsStatus != null || deliveryStatus != null || lastError != null;

        if (hasCampaignFilters) {
            return repo.findContactsByGroupeIdWithAdvancedFiltersAndCampaignOps(
                groupeId,
                nom,
                nomOp,
                prenom,
                prenomOp,
                telephone,
                telephoneOp,
                statut,
                hasWhatsapp,
                minSmsSent,
                maxSmsSent,
                minWhatsappSent,
                maxWhatsappSent,
                hasReceived,
                campaignId,
                smsStatus,
                deliveryStatus,
                lastError,
                pageable
            );
        } else {
            return repo.findContactsByGroupeIdWithAdvancedFiltersOps(
                groupeId,
                nom,
                nomOp,
                prenom,
                prenomOp,
                telephone,
                telephoneOp,
                statut,
                hasWhatsapp,
                minSmsSent,
                maxSmsSent,
                minWhatsappSent,
                maxWhatsappSent,
                hasReceived,
                pageable
            );
        }
    }
}
