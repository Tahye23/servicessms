import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../planabonnement.test-samples';

import { PlanabonnementFormService } from './planabonnement-form.service';

describe('Planabonnement Form Service', () => {
  let service: PlanabonnementFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PlanabonnementFormService);
  });

  describe('Service methods', () => {
    describe('createPlanabonnementFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createPlanabonnementFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            abpPrix: expect.any(Object),
            abpNbrAcces: expect.any(Object),
            abpSevice: expect.any(Object),
            abptype: expect.any(Object),
          }),
        );
      });

      it('passing IPlanabonnement should create a new form with FormGroup', () => {
        const formGroup = service.createPlanabonnementFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            abpPrix: expect.any(Object),
            abpNbrAcces: expect.any(Object),
            abpSevice: expect.any(Object),
            abptype: expect.any(Object),
          }),
        );
      });
    });

    describe('getPlanabonnement', () => {
      it('should return NewPlanabonnement for default Planabonnement initial value', () => {
        const formGroup = service.createPlanabonnementFormGroup(sampleWithNewData);

        const planabonnement = service.getPlanabonnement(formGroup) as any;

        expect(planabonnement).toMatchObject(sampleWithNewData);
      });

      it('should return NewPlanabonnement for empty Planabonnement initial value', () => {
        const formGroup = service.createPlanabonnementFormGroup();

        const planabonnement = service.getPlanabonnement(formGroup) as any;

        expect(planabonnement).toMatchObject({});
      });

      it('should return IPlanabonnement', () => {
        const formGroup = service.createPlanabonnementFormGroup(sampleWithRequiredData);

        const planabonnement = service.getPlanabonnement(formGroup) as any;

        expect(planabonnement).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IPlanabonnement should not enable id FormControl', () => {
        const formGroup = service.createPlanabonnementFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewPlanabonnement should disable id FormControl', () => {
        const formGroup = service.createPlanabonnementFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
