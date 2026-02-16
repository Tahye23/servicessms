package com.example.myproject.repository;

import com.example.myproject.domain.Groupe;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class GroupeRepositoryWithBagRelationshipsImpl implements GroupeRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String GROUPES_PARAMETER = "groupes";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Groupe> fetchBagRelationships(Optional<Groupe> groupe) {
        return groupe.map(this::fetchExtendedUsers);
    }

    @Override
    public Page<Groupe> fetchBagRelationships(Page<Groupe> groupes) {
        return new PageImpl<>(fetchBagRelationships(groupes.getContent()), groupes.getPageable(), groupes.getTotalElements());
    }

    @Override
    public List<Groupe> fetchBagRelationships(List<Groupe> groupes) {
        return Optional.of(groupes).map(this::fetchExtendedUsers).orElse(Collections.emptyList());
    }

    Groupe fetchExtendedUsers(Groupe result) {
        return entityManager
            .createQuery("select groupe from Groupe groupe left join fetch groupe.extendedUsers where groupe.id = :id", Groupe.class)
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Groupe> fetchExtendedUsers(List<Groupe> groupes) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, groupes.size()).forEach(index -> order.put(groupes.get(index).getId(), index));
        List<Groupe> result = entityManager
            .createQuery("select groupe from Groupe groupe left join fetch groupe.extendedUsers where groupe in :groupes", Groupe.class)
            .setParameter(GROUPES_PARAMETER, groupes)
            .getResultList();
        Collections.sort(result, (o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
