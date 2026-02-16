import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IFileextrait, NewFileextrait } from '../fileextrait.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IFileextrait for edit and NewFileextraitFormGroupInput for create.
 */
type FileextraitFormGroupInput = IFileextrait | PartialWithRequiredKeyOf<NewFileextrait>;

type FileextraitFormDefaults = Pick<NewFileextrait, 'id'>;

type FileextraitFormGroupContent = {
  id: FormControl<IFileextrait['id'] | NewFileextrait['id']>;
  fexidfile: FormControl<IFileextrait['fexidfile']>;
  fexparent: FormControl<IFileextrait['fexparent']>;
  fexdata: FormControl<IFileextrait['fexdata']>;
  fexdataContentType: FormControl<IFileextrait['fexdataContentType']>;
  fextype: FormControl<IFileextrait['fextype']>;
  fexname: FormControl<IFileextrait['fexname']>;
};

export type FileextraitFormGroup = FormGroup<FileextraitFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class FileextraitFormService {
  createFileextraitFormGroup(fileextrait: FileextraitFormGroupInput = { id: null }): FileextraitFormGroup {
    const fileextraitRawValue = {
      ...this.getFormDefaults(),
      ...fileextrait,
    };
    return new FormGroup<FileextraitFormGroupContent>({
      id: new FormControl(
        { value: fileextraitRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      fexidfile: new FormControl(fileextraitRawValue.fexidfile),
      fexparent: new FormControl(fileextraitRawValue.fexparent),
      fexdata: new FormControl(fileextraitRawValue.fexdata),
      fexdataContentType: new FormControl(fileextraitRawValue.fexdataContentType),
      fextype: new FormControl(fileextraitRawValue.fextype),
      fexname: new FormControl(fileextraitRawValue.fexname),
    });
  }

  getFileextrait(form: FileextraitFormGroup): IFileextrait | NewFileextrait {
    return form.getRawValue() as IFileextrait | NewFileextrait;
  }

  resetForm(form: FileextraitFormGroup, fileextrait: FileextraitFormGroupInput): void {
    const fileextraitRawValue = { ...this.getFormDefaults(), ...fileextrait };
    form.reset(
      {
        ...fileextraitRawValue,
        id: { value: fileextraitRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): FileextraitFormDefaults {
    return {
      id: null,
    };
  }
}
