import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../groupedecontact.test-samples';

import { GroupedecontactFormService } from './groupedecontact-form.service';

describe('Groupedecontact Form Service', () => {
  let service: GroupedecontactFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GroupedecontactFormService);
  });

  describe('Service methods', () => {
    describe('createGroupedecontactFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createGroupedecontactFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            cgrgroupe: expect.any(Object),
            contact: expect.any(Object),
          }),
        );
      });

      it('passing IGroupedecontact should create a new form with FormGroup', () => {
        const formGroup = service.createGroupedecontactFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            cgrgroupe: expect.any(Object),
            contact: expect.any(Object),
          }),
        );
      });
    });

    describe('getGroupedecontact', () => {
      it('should return NewGroupedecontact for default Groupedecontact initial value', () => {
        const formGroup = service.createGroupedecontactFormGroup(sampleWithNewData);

        const groupedecontact = service.getGroupedecontact(formGroup) as any;

        expect(groupedecontact).toMatchObject(sampleWithNewData);
      });

      it('should return NewGroupedecontact for empty Groupedecontact initial value', () => {
        const formGroup = service.createGroupedecontactFormGroup();

        const groupedecontact = service.getGroupedecontact(formGroup) as any;

        expect(groupedecontact).toMatchObject({});
      });

      it('should return IGroupedecontact', () => {
        const formGroup = service.createGroupedecontactFormGroup(sampleWithRequiredData);

        const groupedecontact = service.getGroupedecontact(formGroup) as any;

        expect(groupedecontact).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IGroupedecontact should not enable id FormControl', () => {
        const formGroup = service.createGroupedecontactFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewGroupedecontact should disable id FormControl', () => {
        const formGroup = service.createGroupedecontactFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
