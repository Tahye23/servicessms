import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IGroupe, NewGroupe } from '../groupe.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IGroupe for edit and NewGroupeFormGroupInput for create.
 */
type GroupeFormGroupInput = IGroupe | PartialWithRequiredKeyOf<NewGroupe>;

type GroupeFormDefaults = Pick<NewGroupe, 'id'>;

type GroupeFormGroupContent = {
  id: FormControl<IGroupe['id'] | NewGroupe['id']>;
  grotitre: FormControl<IGroupe['grotitre']>;
  user: FormControl<IGroupe['user']>;
  groupType: FormControl<IGroupe['groupType']>;
  extendedUser: FormControl<IGroupe['extendedUser']>;
};

export type GroupeFormGroup = FormGroup<GroupeFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class GroupeFormService {
  createGroupeFormGroup(groupe: GroupeFormGroupInput = { id: null }): GroupeFormGroup {
    const groupeRawValue = {
      ...this.getFormDefaults(),
      ...groupe,
    };
    return new FormGroup<GroupeFormGroupContent>({
      id: new FormControl(
        { value: groupeRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      grotitre: new FormControl(groupeRawValue.grotitre),
      user: new FormControl(groupeRawValue.user),
      extendedUser: new FormControl(groupeRawValue.extendedUser),
      groupType: new FormControl(groupeRawValue.groupType ?? null),
    });
  }

  getGroupe(form: GroupeFormGroup): IGroupe | NewGroupe {
    return form.getRawValue() as IGroupe | NewGroupe;
  }

  resetForm(form: GroupeFormGroup, groupe: GroupeFormGroupInput): void {
    const groupeRawValue = { ...this.getFormDefaults(), ...groupe };
    form.reset(
      {
        ...groupeRawValue,
        id: { value: groupeRawValue.id, disabled: true },
        user: { value: groupeRawValue.user, disabled: true }, // <-- Empêche la modification de 'user'
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): GroupeFormDefaults {
    return {
      id: null,
    };
  }
}
