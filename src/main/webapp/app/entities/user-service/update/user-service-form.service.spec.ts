import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../user-service.test-samples';

import { UserServiceFormService } from './user-service-form.service';

describe('UserService Form Service', () => {
  let service: UserServiceFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(UserServiceFormService);
  });

  describe('Service methods', () => {
    describe('createUserServiceFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createUserServiceFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            urSService: expect.any(Object),
            urSUser: expect.any(Object),
            service: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });

      it('passing IUserService should create a new form with FormGroup', () => {
        const formGroup = service.createUserServiceFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            urSService: expect.any(Object),
            urSUser: expect.any(Object),
            service: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });
    });

    describe('getUserService', () => {
      it('should return NewUserService for default UserService initial value', () => {
        const formGroup = service.createUserServiceFormGroup(sampleWithNewData);

        const userService = service.getUserService(formGroup) as any;

        expect(userService).toMatchObject(sampleWithNewData);
      });

      it('should return NewUserService for empty UserService initial value', () => {
        const formGroup = service.createUserServiceFormGroup();

        const userService = service.getUserService(formGroup) as any;

        expect(userService).toMatchObject({});
      });

      it('should return IUserService', () => {
        const formGroup = service.createUserServiceFormGroup(sampleWithRequiredData);

        const userService = service.getUserService(formGroup) as any;

        expect(userService).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IUserService should not enable id FormControl', () => {
        const formGroup = service.createUserServiceFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewUserService should disable id FormControl', () => {
        const formGroup = service.createUserServiceFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
