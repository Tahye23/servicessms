import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../referentiel.test-samples';

import { ReferentielFormService } from './referentiel-form.service';

describe('Referentiel Form Service', () => {
  let service: ReferentielFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ReferentielFormService);
  });

  describe('Service methods', () => {
    describe('createReferentielFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createReferentielFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            refCode: expect.any(Object),
            refRadical: expect.any(Object),
            refFrTitle: expect.any(Object),
            refArTitle: expect.any(Object),
            refEnTitle: expect.any(Object),
          }),
        );
      });

      it('passing IReferentiel should create a new form with FormGroup', () => {
        const formGroup = service.createReferentielFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            refCode: expect.any(Object),
            refRadical: expect.any(Object),
            refFrTitle: expect.any(Object),
            refArTitle: expect.any(Object),
            refEnTitle: expect.any(Object),
          }),
        );
      });
    });

    describe('getReferentiel', () => {
      it('should return NewReferentiel for default Referentiel initial value', () => {
        const formGroup = service.createReferentielFormGroup(sampleWithNewData);

        const referentiel = service.getReferentiel(formGroup) as any;

        expect(referentiel).toMatchObject(sampleWithNewData);
      });

      it('should return NewReferentiel for empty Referentiel initial value', () => {
        const formGroup = service.createReferentielFormGroup();

        const referentiel = service.getReferentiel(formGroup) as any;

        expect(referentiel).toMatchObject({});
      });

      it('should return IReferentiel', () => {
        const formGroup = service.createReferentielFormGroup(sampleWithRequiredData);

        const referentiel = service.getReferentiel(formGroup) as any;

        expect(referentiel).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IReferentiel should not enable id FormControl', () => {
        const formGroup = service.createReferentielFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewReferentiel should disable id FormControl', () => {
        const formGroup = service.createReferentielFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
