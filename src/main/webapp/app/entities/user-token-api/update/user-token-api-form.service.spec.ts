import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../user-token-api.test-samples';

import { UserTokenApiFormService } from './user-token-api-form.service';

describe('UserTokenApi Form Service', () => {
  let service: UserTokenApiFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(UserTokenApiFormService);
  });

  describe('Service methods', () => {
    describe('createUserTokenApiFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createUserTokenApiFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            api: expect.any(Object),
            token: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });

      it('passing IUserTokenApi should create a new form with FormGroup', () => {
        const formGroup = service.createUserTokenApiFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            api: expect.any(Object),
            token: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });
    });

    describe('getUserTokenApi', () => {
      it('should return NewUserTokenApi for default UserTokenApi initial value', () => {
        const formGroup = service.createUserTokenApiFormGroup(sampleWithNewData);

        const userTokenApi = service.getUserTokenApi(formGroup) as any;

        expect(userTokenApi).toMatchObject(sampleWithNewData);
      });

      it('should return NewUserTokenApi for empty UserTokenApi initial value', () => {
        const formGroup = service.createUserTokenApiFormGroup();

        const userTokenApi = service.getUserTokenApi(formGroup) as any;

        expect(userTokenApi).toMatchObject({});
      });

      it('should return IUserTokenApi', () => {
        const formGroup = service.createUserTokenApiFormGroup(sampleWithRequiredData);

        const userTokenApi = service.getUserTokenApi(formGroup) as any;

        expect(userTokenApi).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IUserTokenApi should not enable id FormControl', () => {
        const formGroup = service.createUserTokenApiFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewUserTokenApi should disable id FormControl', () => {
        const formGroup = service.createUserTokenApiFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
