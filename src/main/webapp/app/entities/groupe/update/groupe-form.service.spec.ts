import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../groupe.test-samples';

import { GroupeFormService } from './groupe-form.service';

describe('Groupe Form Service', () => {
  let service: GroupeFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GroupeFormService);
  });

  describe('Service methods', () => {
    describe('createGroupeFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createGroupeFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            grotitre: expect.any(Object),
            user: expect.any(Object),
            extendedUser: expect.any(Object),
          }),
        );
      });

      it('passing IGroupe should create a new form with FormGroup', () => {
        const formGroup = service.createGroupeFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            grotitre: expect.any(Object),
            user: expect.any(Object),
            extendedUser: expect.any(Object),
          }),
        );
      });
    });

    describe('getGroupe', () => {
      it('should return NewGroupe for default Groupe initial value', () => {
        const formGroup = service.createGroupeFormGroup(sampleWithNewData);

        const groupe = service.getGroupe(formGroup) as any;

        expect(groupe).toMatchObject(sampleWithNewData);
      });

      it('should return NewGroupe for empty Groupe initial value', () => {
        const formGroup = service.createGroupeFormGroup();

        const groupe = service.getGroupe(formGroup) as any;

        expect(groupe).toMatchObject({});
      });

      it('should return IGroupe', () => {
        const formGroup = service.createGroupeFormGroup(sampleWithRequiredData);

        const groupe = service.getGroupe(formGroup) as any;

        expect(groupe).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IGroupe should not enable id FormControl', () => {
        const formGroup = service.createGroupeFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewGroupe should disable id FormControl', () => {
        const formGroup = service.createGroupeFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
