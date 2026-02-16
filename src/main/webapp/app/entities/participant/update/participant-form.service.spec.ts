import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../participant.test-samples';

import { ParticipantFormService } from './participant-form.service';

describe('Participant Form Service', () => {
  let service: ParticipantFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ParticipantFormService);
  });

  describe('Service methods', () => {
    describe('createParticipantFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createParticipantFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            patcontact: expect.any(Object),
            patenquette: expect.any(Object),
          }),
        );
      });

      it('passing IParticipant should create a new form with FormGroup', () => {
        const formGroup = service.createParticipantFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            patcontact: expect.any(Object),
            patenquette: expect.any(Object),
          }),
        );
      });
    });

    describe('getParticipant', () => {
      it('should return NewParticipant for default Participant initial value', () => {
        const formGroup = service.createParticipantFormGroup(sampleWithNewData);

        const participant = service.getParticipant(formGroup) as any;

        expect(participant).toMatchObject(sampleWithNewData);
      });

      it('should return NewParticipant for empty Participant initial value', () => {
        const formGroup = service.createParticipantFormGroup();

        const participant = service.getParticipant(formGroup) as any;

        expect(participant).toMatchObject({});
      });

      it('should return IParticipant', () => {
        const formGroup = service.createParticipantFormGroup(sampleWithRequiredData);

        const participant = service.getParticipant(formGroup) as any;

        expect(participant).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IParticipant should not enable id FormControl', () => {
        const formGroup = service.createParticipantFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewParticipant should disable id FormControl', () => {
        const formGroup = service.createParticipantFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
