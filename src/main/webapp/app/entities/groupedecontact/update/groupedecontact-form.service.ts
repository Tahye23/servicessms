import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IGroupedecontact, NewGroupedecontact } from '../groupedecontact.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IGroupedecontact for edit and NewGroupedecontactFormGroupInput for create.
 */
type GroupedecontactFormGroupInput = IGroupedecontact | PartialWithRequiredKeyOf<NewGroupedecontact>;

type GroupedecontactFormDefaults = Pick<NewGroupedecontact, 'id'>;

type GroupedecontactFormGroupContent = {
  id: FormControl<IGroupedecontact['id'] | NewGroupedecontact['id']>;
  cgrgroupe: FormControl<IGroupedecontact['cgrgroupe']>;
  contact: FormControl<IGroupedecontact['contact']>;
};

export type GroupedecontactFormGroup = FormGroup<GroupedecontactFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class GroupedecontactFormService {
  createGroupedecontactFormGroup(groupedecontact: GroupedecontactFormGroupInput = { id: null }): GroupedecontactFormGroup {
    const groupedecontactRawValue = {
      ...this.getFormDefaults(),
      ...groupedecontact,
    };
    return new FormGroup<GroupedecontactFormGroupContent>({
      id: new FormControl(
        { value: groupedecontactRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      cgrgroupe: new FormControl(groupedecontactRawValue.cgrgroupe),
      contact: new FormControl(groupedecontactRawValue.contact),
    });
  }

  getGroupedecontact(form: GroupedecontactFormGroup): IGroupedecontact | NewGroupedecontact {
    return form.getRawValue() as IGroupedecontact | NewGroupedecontact;
  }

  resetForm(form: GroupedecontactFormGroup, groupedecontact: GroupedecontactFormGroupInput): void {
    const groupedecontactRawValue = { ...this.getFormDefaults(), ...groupedecontact };
    form.reset(
      {
        ...groupedecontactRawValue,
        id: { value: groupedecontactRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): GroupedecontactFormDefaults {
    return {
      id: null,
    };
  }
}
