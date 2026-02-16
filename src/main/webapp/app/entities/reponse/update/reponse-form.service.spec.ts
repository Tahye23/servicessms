import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../reponse.test-samples';

import { ReponseFormService } from './reponse-form.service';

describe('Reponse Form Service', () => {
  let service: ReponseFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ReponseFormService);
  });

  describe('Service methods', () => {
    describe('createReponseFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createReponseFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            repvaleur: expect.any(Object),
            repquestion: expect.any(Object),
            repcontact: expect.any(Object),
          }),
        );
      });

      it('passing IReponse should create a new form with FormGroup', () => {
        const formGroup = service.createReponseFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            repvaleur: expect.any(Object),
            repquestion: expect.any(Object),
            repcontact: expect.any(Object),
          }),
        );
      });
    });

    describe('getReponse', () => {
      it('should return NewReponse for default Reponse initial value', () => {
        const formGroup = service.createReponseFormGroup(sampleWithNewData);

        const reponse = service.getReponse(formGroup) as any;

        expect(reponse).toMatchObject(sampleWithNewData);
      });

      it('should return NewReponse for empty Reponse initial value', () => {
        const formGroup = service.createReponseFormGroup();

        const reponse = service.getReponse(formGroup) as any;

        expect(reponse).toMatchObject({});
      });

      it('should return IReponse', () => {
        const formGroup = service.createReponseFormGroup(sampleWithRequiredData);

        const reponse = service.getReponse(formGroup) as any;

        expect(reponse).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IReponse should not enable id FormControl', () => {
        const formGroup = service.createReponseFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewReponse should disable id FormControl', () => {
        const formGroup = service.createReponseFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
