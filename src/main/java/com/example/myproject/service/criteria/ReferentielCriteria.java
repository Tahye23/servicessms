package com.example.myproject.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.example.myproject.domain.Referentiel} entity. This class is used
 * in {@link com.example.myproject.web.rest.ReferentielResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /referentiels?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferentielCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter refCode;

    private StringFilter refRadical;

    private StringFilter refFrTitle;

    private StringFilter refArTitle;

    private StringFilter refEnTitle;

    private Boolean distinct;

    public ReferentielCriteria() {}

    public ReferentielCriteria(ReferentielCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.refCode = other.optionalRefCode().map(StringFilter::copy).orElse(null);
        this.refRadical = other.optionalRefRadical().map(StringFilter::copy).orElse(null);
        this.refFrTitle = other.optionalRefFrTitle().map(StringFilter::copy).orElse(null);
        this.refArTitle = other.optionalRefArTitle().map(StringFilter::copy).orElse(null);
        this.refEnTitle = other.optionalRefEnTitle().map(StringFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ReferentielCriteria copy() {
        return new ReferentielCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getRefCode() {
        return refCode;
    }

    public Optional<StringFilter> optionalRefCode() {
        return Optional.ofNullable(refCode);
    }

    public StringFilter refCode() {
        if (refCode == null) {
            setRefCode(new StringFilter());
        }
        return refCode;
    }

    public void setRefCode(StringFilter refCode) {
        this.refCode = refCode;
    }

    public StringFilter getRefRadical() {
        return refRadical;
    }

    public Optional<StringFilter> optionalRefRadical() {
        return Optional.ofNullable(refRadical);
    }

    public StringFilter refRadical() {
        if (refRadical == null) {
            setRefRadical(new StringFilter());
        }
        return refRadical;
    }

    public void setRefRadical(StringFilter refRadical) {
        this.refRadical = refRadical;
    }

    public StringFilter getRefFrTitle() {
        return refFrTitle;
    }

    public Optional<StringFilter> optionalRefFrTitle() {
        return Optional.ofNullable(refFrTitle);
    }

    public StringFilter refFrTitle() {
        if (refFrTitle == null) {
            setRefFrTitle(new StringFilter());
        }
        return refFrTitle;
    }

    public void setRefFrTitle(StringFilter refFrTitle) {
        this.refFrTitle = refFrTitle;
    }

    public StringFilter getRefArTitle() {
        return refArTitle;
    }

    public Optional<StringFilter> optionalRefArTitle() {
        return Optional.ofNullable(refArTitle);
    }

    public StringFilter refArTitle() {
        if (refArTitle == null) {
            setRefArTitle(new StringFilter());
        }
        return refArTitle;
    }

    public void setRefArTitle(StringFilter refArTitle) {
        this.refArTitle = refArTitle;
    }

    public StringFilter getRefEnTitle() {
        return refEnTitle;
    }

    public Optional<StringFilter> optionalRefEnTitle() {
        return Optional.ofNullable(refEnTitle);
    }

    public StringFilter refEnTitle() {
        if (refEnTitle == null) {
            setRefEnTitle(new StringFilter());
        }
        return refEnTitle;
    }

    public void setRefEnTitle(StringFilter refEnTitle) {
        this.refEnTitle = refEnTitle;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ReferentielCriteria that = (ReferentielCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(refCode, that.refCode) &&
            Objects.equals(refRadical, that.refRadical) &&
            Objects.equals(refFrTitle, that.refFrTitle) &&
            Objects.equals(refArTitle, that.refArTitle) &&
            Objects.equals(refEnTitle, that.refEnTitle) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, refCode, refRadical, refFrTitle, refArTitle, refEnTitle, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferentielCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalRefCode().map(f -> "refCode=" + f + ", ").orElse("") +
            optionalRefRadical().map(f -> "refRadical=" + f + ", ").orElse("") +
            optionalRefFrTitle().map(f -> "refFrTitle=" + f + ", ").orElse("") +
            optionalRefArTitle().map(f -> "refArTitle=" + f + ", ").orElse("") +
            optionalRefEnTitle().map(f -> "refEnTitle=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
