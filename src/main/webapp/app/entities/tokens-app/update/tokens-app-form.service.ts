import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';
import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { ITokensApp, NewTokensApp } from '../tokens-app.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ITokensApp for edit and NewTokensAppFormGroupInput for create.
 */
type TokensAppFormGroupInput = ITokensApp | PartialWithRequiredKeyOf<NewTokensApp>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends ITokensApp | NewTokensApp> = Omit<T, 'dateExpiration'> & {
  dateExpiration?: string | null;
};

type TokensAppFormRawValue = FormValueOf<ITokensApp>;

type NewTokensAppFormRawValue = FormValueOf<NewTokensApp>;

type TokensAppFormDefaults = Pick<NewTokensApp, 'id' | 'dateExpiration'>;

type TokensAppFormGroupContent = {
  id: FormControl<TokensAppFormRawValue['id'] | NewTokensApp['id']>;
  dateExpiration: FormControl<TokensAppFormRawValue['dateExpiration']>;
  token: FormControl<TokensAppFormRawValue['token']>;
  application: FormControl<TokensAppFormRawValue['application']>;
};

export type TokensAppFormGroup = FormGroup<TokensAppFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class TokensAppFormService {
  createTokensAppFormGroup(tokensApp: TokensAppFormGroupInput = { id: null }): TokensAppFormGroup {
    const tokensAppRawValue = this.convertTokensAppToTokensAppRawValue({
      ...this.getFormDefaults(),
      ...tokensApp,
    });
    return new FormGroup<TokensAppFormGroupContent>({
      id: new FormControl(
        { value: tokensAppRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      dateExpiration: new FormControl(tokensAppRawValue.dateExpiration),
      token: new FormControl(tokensAppRawValue.token),
      application: new FormControl(tokensAppRawValue.application),
    });
  }

  getTokensApp(form: TokensAppFormGroup): ITokensApp | NewTokensApp {
    return this.convertTokensAppRawValueToTokensApp(form.getRawValue() as TokensAppFormRawValue | NewTokensAppFormRawValue);
  }

  resetForm(form: TokensAppFormGroup, tokensApp: TokensAppFormGroupInput): void {
    const tokensAppRawValue = this.convertTokensAppToTokensAppRawValue({ ...this.getFormDefaults(), ...tokensApp });
    form.reset(
      {
        ...tokensAppRawValue,
        id: { value: tokensAppRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): TokensAppFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      dateExpiration: currentTime,
    };
  }

  private convertTokensAppRawValueToTokensApp(rawTokensApp: TokensAppFormRawValue | NewTokensAppFormRawValue): ITokensApp | NewTokensApp {
    return {
      ...rawTokensApp,
      dateExpiration: dayjs(rawTokensApp.dateExpiration, DATE_TIME_FORMAT),
    };
  }

  private convertTokensAppToTokensAppRawValue(
    tokensApp: ITokensApp | (Partial<NewTokensApp> & TokensAppFormDefaults),
  ): TokensAppFormRawValue | PartialWithRequiredKeyOf<NewTokensAppFormRawValue> {
    return {
      ...tokensApp,
      dateExpiration: tokensApp.dateExpiration ? tokensApp.dateExpiration.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
