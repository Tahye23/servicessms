import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../choix.test-samples';

import { ChoixFormService } from './choix-form.service';

describe('Choix Form Service', () => {
  let service: ChoixFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ChoixFormService);
  });

  describe('Service methods', () => {
    describe('createChoixFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createChoixFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            chovaleur: expect.any(Object),
            choquestion: expect.any(Object),
            choquestionSuivante: expect.any(Object),
          }),
        );
      });

      it('passing IChoix should create a new form with FormGroup', () => {
        const formGroup = service.createChoixFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            chovaleur: expect.any(Object),
            choquestion: expect.any(Object),
            choquestionSuivante: expect.any(Object),
          }),
        );
      });
    });

    describe('getChoix', () => {
      it('should return NewChoix for default Choix initial value', () => {
        const formGroup = service.createChoixFormGroup(sampleWithNewData);

        const choix = service.getChoix(formGroup) as any;

        expect(choix).toMatchObject(sampleWithNewData);
      });

      it('should return NewChoix for empty Choix initial value', () => {
        const formGroup = service.createChoixFormGroup();

        const choix = service.getChoix(formGroup) as any;

        expect(choix).toMatchObject({});
      });

      it('should return IChoix', () => {
        const formGroup = service.createChoixFormGroup(sampleWithRequiredData);

        const choix = service.getChoix(formGroup) as any;

        expect(choix).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IChoix should not enable id FormControl', () => {
        const formGroup = service.createChoixFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewChoix should disable id FormControl', () => {
        const formGroup = service.createChoixFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
