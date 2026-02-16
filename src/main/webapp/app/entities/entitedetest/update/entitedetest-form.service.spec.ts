import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../entitedetest.test-samples';

import { EntitedetestFormService } from './entitedetest-form.service';

describe('Entitedetest Form Service', () => {
  let service: EntitedetestFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EntitedetestFormService);
  });

  describe('Service methods', () => {
    describe('createEntitedetestFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createEntitedetestFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            identite: expect.any(Object),
            nom: expect.any(Object),
            nombrec: expect.any(Object),
            chamb: expect.any(Object),
            champdate: expect.any(Object),
          }),
        );
      });

      it('passing IEntitedetest should create a new form with FormGroup', () => {
        const formGroup = service.createEntitedetestFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            identite: expect.any(Object),
            nom: expect.any(Object),
            nombrec: expect.any(Object),
            chamb: expect.any(Object),
            champdate: expect.any(Object),
          }),
        );
      });
    });

    describe('getEntitedetest', () => {
      it('should return NewEntitedetest for default Entitedetest initial value', () => {
        const formGroup = service.createEntitedetestFormGroup(sampleWithNewData);

        const entitedetest = service.getEntitedetest(formGroup) as any;

        expect(entitedetest).toMatchObject(sampleWithNewData);
      });

      it('should return NewEntitedetest for empty Entitedetest initial value', () => {
        const formGroup = service.createEntitedetestFormGroup();

        const entitedetest = service.getEntitedetest(formGroup) as any;

        expect(entitedetest).toMatchObject({});
      });

      it('should return IEntitedetest', () => {
        const formGroup = service.createEntitedetestFormGroup(sampleWithRequiredData);

        const entitedetest = service.getEntitedetest(formGroup) as any;

        expect(entitedetest).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IEntitedetest should not enable id FormControl', () => {
        const formGroup = service.createEntitedetestFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewEntitedetest should disable id FormControl', () => {
        const formGroup = service.createEntitedetestFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
