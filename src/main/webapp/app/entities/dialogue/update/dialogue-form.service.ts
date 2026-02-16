import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IDialogue, NewDialogue } from '../dialogue.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IDialogue for edit and NewDialogueFormGroupInput for create.
 */
type DialogueFormGroupInput = IDialogue | PartialWithRequiredKeyOf<NewDialogue>;

type DialogueFormDefaults = Pick<NewDialogue, 'id'>;

type DialogueFormGroupContent = {
  id: FormControl<IDialogue['id'] | NewDialogue['id']>;
  dialogueId: FormControl<IDialogue['dialogueId']>;
  contenu: FormControl<IDialogue['contenu']>;
};

export type DialogueFormGroup = FormGroup<DialogueFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class DialogueFormService {
  createDialogueFormGroup(dialogue: DialogueFormGroupInput = { id: null }): DialogueFormGroup {
    const dialogueRawValue = {
      ...this.getFormDefaults(),
      ...dialogue,
    };
    return new FormGroup<DialogueFormGroupContent>({
      id: new FormControl(
        { value: dialogueRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      dialogueId: new FormControl(dialogueRawValue.dialogueId),
      contenu: new FormControl(dialogueRawValue.contenu),
    });
  }

  getDialogue(form: DialogueFormGroup): IDialogue | NewDialogue {
    return form.getRawValue() as IDialogue | NewDialogue;
  }

  resetForm(form: DialogueFormGroup, dialogue: DialogueFormGroupInput): void {
    const dialogueRawValue = { ...this.getFormDefaults(), ...dialogue };
    form.reset(
      {
        ...dialogueRawValue,
        id: { value: dialogueRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): DialogueFormDefaults {
    return {
      id: null,
    };
  }
}
