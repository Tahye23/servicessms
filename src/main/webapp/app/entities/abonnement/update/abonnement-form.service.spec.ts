import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../abonnement.test-samples';

import { AbonnementFormService } from './abonnement-form.service';

describe('Abonnement Form Service', () => {
  let service: AbonnementFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AbonnementFormService);
  });

  describe('Service methods', () => {
    describe('createAbonnementFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createAbonnementFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            aboStatut: expect.any(Object),
            aboCompteur: expect.any(Object),
            application: expect.any(Object),
            aboUser: expect.any(Object),
            aboPlan: expect.any(Object),
          }),
        );
      });

      it('passing IAbonnement should create a new form with FormGroup', () => {
        const formGroup = service.createAbonnementFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            aboStatut: expect.any(Object),
            aboCompteur: expect.any(Object),
            application: expect.any(Object),
            aboUser: expect.any(Object),
            aboPlan: expect.any(Object),
          }),
        );
      });
    });

    describe('getAbonnement', () => {
      it('should return NewAbonnement for default Abonnement initial value', () => {
        const formGroup = service.createAbonnementFormGroup(sampleWithNewData);

        const abonnement = service.getAbonnement(formGroup) as any;

        expect(abonnement).toMatchObject(sampleWithNewData);
      });

      it('should return NewAbonnement for empty Abonnement initial value', () => {
        const formGroup = service.createAbonnementFormGroup();

        const abonnement = service.getAbonnement(formGroup) as any;

        expect(abonnement).toMatchObject({});
      });

      it('should return IAbonnement', () => {
        const formGroup = service.createAbonnementFormGroup(sampleWithRequiredData);

        const abonnement = service.getAbonnement(formGroup) as any;

        expect(abonnement).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IAbonnement should not enable id FormControl', () => {
        const formGroup = service.createAbonnementFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewAbonnement should disable id FormControl', () => {
        const formGroup = service.createAbonnementFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
