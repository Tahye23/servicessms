package com.example.myproject.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ReferentielCriteriaTest {

    @Test
    void newReferentielCriteriaHasAllFiltersNullTest() {
        var referentielCriteria = new ReferentielCriteria();
        assertThat(referentielCriteria).is(criteriaFiltersAre(filter -> filter == null));
    }

    @Test
    void referentielCriteriaFluentMethodsCreatesFiltersTest() {
        var referentielCriteria = new ReferentielCriteria();

        setAllFilters(referentielCriteria);

        assertThat(referentielCriteria).is(criteriaFiltersAre(filter -> filter != null));
    }

    @Test
    void referentielCriteriaCopyCreatesNullFilterTest() {
        var referentielCriteria = new ReferentielCriteria();
        var copy = referentielCriteria.copy();

        assertThat(referentielCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(filter -> filter == null)),
            criteria -> assertThat(criteria).isEqualTo(referentielCriteria)
        );
    }

    @Test
    void referentielCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var referentielCriteria = new ReferentielCriteria();
        setAllFilters(referentielCriteria);

        var copy = referentielCriteria.copy();

        assertThat(referentielCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(filter -> filter != null)),
            criteria -> assertThat(criteria).isEqualTo(referentielCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var referentielCriteria = new ReferentielCriteria();

        assertThat(referentielCriteria).hasToString("ReferentielCriteria{}");
    }

    private static void setAllFilters(ReferentielCriteria referentielCriteria) {
        referentielCriteria.id();
        referentielCriteria.refCode();
        referentielCriteria.refRadical();
        referentielCriteria.refFrTitle();
        referentielCriteria.refArTitle();
        referentielCriteria.refEnTitle();
        referentielCriteria.distinct();
    }

    private static Condition<ReferentielCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getRefCode()) &&
                condition.apply(criteria.getRefRadical()) &&
                condition.apply(criteria.getRefFrTitle()) &&
                condition.apply(criteria.getRefArTitle()) &&
                condition.apply(criteria.getRefEnTitle()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ReferentielCriteria> copyFiltersAre(ReferentielCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getRefCode(), copy.getRefCode()) &&
                condition.apply(criteria.getRefRadical(), copy.getRefRadical()) &&
                condition.apply(criteria.getRefFrTitle(), copy.getRefFrTitle()) &&
                condition.apply(criteria.getRefArTitle(), copy.getRefArTitle()) &&
                condition.apply(criteria.getRefEnTitle(), copy.getRefEnTitle()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
