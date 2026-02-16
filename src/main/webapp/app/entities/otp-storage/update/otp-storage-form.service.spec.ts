import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../otp-storage.test-samples';

import { OTPStorageFormService } from './otp-storage-form.service';

describe('OTPStorage Form Service', () => {
  let service: OTPStorageFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(OTPStorageFormService);
  });

  describe('Service methods', () => {
    describe('createOTPStorageFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createOTPStorageFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            otsOTP: expect.any(Object),
            phoneNumber: expect.any(Object),
            otsdateexpir: expect.any(Object),
            isOtpUsed: expect.any(Object),
            isExpired: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });

      it('passing IOTPStorage should create a new form with FormGroup', () => {
        const formGroup = service.createOTPStorageFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            otsOTP: expect.any(Object),
            phoneNumber: expect.any(Object),
            otsdateexpir: expect.any(Object),
            isOtpUsed: expect.any(Object),
            isExpired: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });
    });

    describe('getOTPStorage', () => {
      it('should return NewOTPStorage for default OTPStorage initial value', () => {
        const formGroup = service.createOTPStorageFormGroup(sampleWithNewData);

        const oTPStorage = service.getOTPStorage(formGroup) as any;

        expect(oTPStorage).toMatchObject(sampleWithNewData);
      });

      it('should return NewOTPStorage for empty OTPStorage initial value', () => {
        const formGroup = service.createOTPStorageFormGroup();

        const oTPStorage = service.getOTPStorage(formGroup) as any;

        expect(oTPStorage).toMatchObject({});
      });

      it('should return IOTPStorage', () => {
        const formGroup = service.createOTPStorageFormGroup(sampleWithRequiredData);

        const oTPStorage = service.getOTPStorage(formGroup) as any;

        expect(oTPStorage).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IOTPStorage should not enable id FormControl', () => {
        const formGroup = service.createOTPStorageFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewOTPStorage should disable id FormControl', () => {
        const formGroup = service.createOTPStorageFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
