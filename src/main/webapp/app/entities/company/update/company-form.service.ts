import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';
import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { ICompany, NewCompany } from '../company.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ICompany for edit and NewCompanyFormGroupInput for create.
 */
type CompanyFormGroupInput = ICompany | PartialWithRequiredKeyOf<NewCompany>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends ICompany | NewCompany> = Omit<T, 'camdatefin'> & {
  camdatefin?: string | null;
};

type CompanyFormRawValue = FormValueOf<ICompany>;

type NewCompanyFormRawValue = FormValueOf<NewCompany>;

type CompanyFormDefaults = Pick<NewCompany, 'id' | 'camdatefin' | 'camispub'>;

type CompanyFormGroupContent = {
  id: FormControl<CompanyFormRawValue['id'] | NewCompany['id']>;
  name: FormControl<CompanyFormRawValue['name']>;
  activity: FormControl<CompanyFormRawValue['activity']>;
  camtitre: FormControl<CompanyFormRawValue['camtitre']>;
  camdatecreation: FormControl<CompanyFormRawValue['camdatecreation']>;
  camdatefin: FormControl<CompanyFormRawValue['camdatefin']>;
  camispub: FormControl<CompanyFormRawValue['camispub']>;
  camUser: FormControl<CompanyFormRawValue['camUser']>;
  camstatus: FormControl<CompanyFormRawValue['camstatus']>;
};

export type CompanyFormGroup = FormGroup<CompanyFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class CompanyFormService {
  createCompanyFormGroup(company: CompanyFormGroupInput = { id: null }): CompanyFormGroup {
    const companyRawValue = this.convertCompanyToCompanyRawValue({
      ...this.getFormDefaults(),
      ...company,
    });
    return new FormGroup<CompanyFormGroupContent>({
      id: new FormControl(
        { value: companyRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(companyRawValue.name),
      activity: new FormControl(companyRawValue.activity),
      camtitre: new FormControl(companyRawValue.camtitre),
      camdatecreation: new FormControl(companyRawValue.camdatecreation),
      camdatefin: new FormControl(companyRawValue.camdatefin),
      camispub: new FormControl(companyRawValue.camispub),
      camUser: new FormControl(companyRawValue.camUser),
      camstatus: new FormControl(companyRawValue.camstatus),
    });
  }

  getCompany(form: CompanyFormGroup): ICompany | NewCompany {
    return this.convertCompanyRawValueToCompany(form.getRawValue() as CompanyFormRawValue | NewCompanyFormRawValue);
  }

  resetForm(form: CompanyFormGroup, company: CompanyFormGroupInput): void {
    const companyRawValue = this.convertCompanyToCompanyRawValue({ ...this.getFormDefaults(), ...company });
    form.reset(
      {
        ...companyRawValue,
        id: { value: companyRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): CompanyFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      camdatefin: currentTime,
      camispub: false,
    };
  }

  private convertCompanyRawValueToCompany(rawCompany: CompanyFormRawValue | NewCompanyFormRawValue): ICompany | NewCompany {
    return {
      ...rawCompany,
      camdatefin: dayjs(rawCompany.camdatefin, DATE_TIME_FORMAT),
    };
  }

  private convertCompanyToCompanyRawValue(
    company: ICompany | (Partial<NewCompany> & CompanyFormDefaults),
  ): CompanyFormRawValue | PartialWithRequiredKeyOf<NewCompanyFormRawValue> {
    return {
      ...company,
      camdatefin: company.camdatefin ? company.camdatefin.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
