package com.example.myproject.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Dialogue.
 */
@Entity
@Table(name = "dialogue")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Dialogue implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "dialogue_id")
    private Integer dialogueId;

    @Column(name = "contenu")
    private String contenu;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Dialogue id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDialogueId() {
        return this.dialogueId;
    }

    public Dialogue dialogueId(Integer dialogueId) {
        this.setDialogueId(dialogueId);
        return this;
    }

    public void setDialogueId(Integer dialogueId) {
        this.dialogueId = dialogueId;
    }

    public String getContenu() {
        return this.contenu;
    }

    public Dialogue contenu(String contenu) {
        this.setContenu(contenu);
        return this;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Dialogue)) {
            return false;
        }
        return getId() != null && getId().equals(((Dialogue) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Dialogue{" +
            "id=" + getId() +
            ", dialogueId=" + getDialogueId() +
            ", contenu='" + getContenu() + "'" +
            "}";
    }
}
