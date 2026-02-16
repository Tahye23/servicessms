import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../send-sms.test-samples';

import { SendSmsFormService } from './send-sms-form.service';

describe('SendSms Form Service', () => {
  let service: SendSmsFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SendSmsFormService);
  });

  describe('Service methods', () => {
    describe('createSendSmsFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createSendSmsFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            sender: expect.any(Object),
            receiver: expect.any(Object),
            msgdata: expect.any(Object),
            sendateEnvoi: expect.any(Object),
            dialogue: expect.any(Object),
            isSent: expect.any(Object),
            isbulk: expect.any(Object),
            Titre: expect.any(Object),
            user: expect.any(Object),
            destinateur: expect.any(Object),
            destinataires: expect.any(Object),
            statut: expect.any(Object),
          }),
        );
      });

      it('passing ISendSms should create a new form with FormGroup', () => {
        const formGroup = service.createSendSmsFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            sender: expect.any(Object),
            receiver: expect.any(Object),
            msgdata: expect.any(Object),
            sendateEnvoi: expect.any(Object),
            dialogue: expect.any(Object),
            isSent: expect.any(Object),
            isbulk: expect.any(Object),
            Titre: expect.any(Object),
            user: expect.any(Object),
            destinateur: expect.any(Object),
            destinataires: expect.any(Object),
            statut: expect.any(Object),
          }),
        );
      });
    });

    describe('getSendSms', () => {
      it('should return NewSendSms for default SendSms initial value', () => {
        const formGroup = service.createSendSmsFormGroup(sampleWithNewData);

        const sendSms = service.getSendSms(formGroup) as any;

        expect(sendSms).toMatchObject(sampleWithNewData);
      });

      it('should return NewSendSms for empty SendSms initial value', () => {
        const formGroup = service.createSendSmsFormGroup();

        const sendSms = service.getSendSms(formGroup) as any;

        expect(sendSms).toMatchObject({});
      });

      it('should return ISendSms', () => {
        const formGroup = service.createSendSmsFormGroup(sampleWithRequiredData);

        const sendSms = service.getSendSms(formGroup) as any;

        expect(sendSms).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing ISendSms should not enable id FormControl', () => {
        const formGroup = service.createSendSmsFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewSendSms should disable id FormControl', () => {
        const formGroup = service.createSendSmsFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
