import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../dialogue.test-samples';

import { DialogueFormService } from './dialogue-form.service';

describe('Dialogue Form Service', () => {
  let service: DialogueFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DialogueFormService);
  });

  describe('Service methods', () => {
    describe('createDialogueFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createDialogueFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            dialogueId: expect.any(Object),
            contenu: expect.any(Object),
          }),
        );
      });

      it('passing IDialogue should create a new form with FormGroup', () => {
        const formGroup = service.createDialogueFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            dialogueId: expect.any(Object),
            contenu: expect.any(Object),
          }),
        );
      });
    });

    describe('getDialogue', () => {
      it('should return NewDialogue for default Dialogue initial value', () => {
        const formGroup = service.createDialogueFormGroup(sampleWithNewData);

        const dialogue = service.getDialogue(formGroup) as any;

        expect(dialogue).toMatchObject(sampleWithNewData);
      });

      it('should return NewDialogue for empty Dialogue initial value', () => {
        const formGroup = service.createDialogueFormGroup();

        const dialogue = service.getDialogue(formGroup) as any;

        expect(dialogue).toMatchObject({});
      });

      it('should return IDialogue', () => {
        const formGroup = service.createDialogueFormGroup(sampleWithRequiredData);

        const dialogue = service.getDialogue(formGroup) as any;

        expect(dialogue).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IDialogue should not enable id FormControl', () => {
        const formGroup = service.createDialogueFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewDialogue should disable id FormControl', () => {
        const formGroup = service.createDialogueFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
