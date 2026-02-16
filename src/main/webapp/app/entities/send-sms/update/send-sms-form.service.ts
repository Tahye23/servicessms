import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';
import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { ISendSms, MessageType, NewSendSms } from '../send-sms.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ISendSms for edit and NewSendSmsFormGroupInput for create.
 */
type SendSmsFormGroupInput = ISendSms | PartialWithRequiredKeyOf<NewSendSms>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends ISendSms | NewSendSms> = Omit<T, 'sendateEnvoi'> & {
  sendateEnvoi?: string | null;
};

type SendSmsFormRawValue = FormValueOf<ISendSms>;

type NewSendSmsFormRawValue = FormValueOf<NewSendSms>;

type SendSmsFormDefaults = Pick<NewSendSms, 'id' | 'sendateEnvoi' | 'isSent' | 'isbulk'>;

type SendSmsFormGroupContent = {
  id: FormControl<SendSmsFormRawValue['id'] | NewSendSms['id']>;
  sender: FormControl<SendSmsFormRawValue['sender']>;
  receiver: FormControl<SendSmsFormRawValue['receiver']>;
  msgdata: FormControl<SendSmsFormRawValue['msgdata']>;
  sendateEnvoi: FormControl<SendSmsFormRawValue['sendateEnvoi']>;
  dialogue: FormControl<SendSmsFormRawValue['dialogue']>;
  isSent: FormControl<SendSmsFormRawValue['isSent']>;
  isbulk: FormControl<SendSmsFormRawValue['isbulk']>;
  titre: FormControl<SendSmsFormRawValue['titre']>;
  user: FormControl<SendSmsFormRawValue['user']>;
  destinateur: FormControl<SendSmsFormRawValue['destinateur']>;
  destinataires: FormControl<SendSmsFormRawValue['destinataires']>;
  statut: FormControl<SendSmsFormRawValue['statut']>;
  template_id: FormControl<SendSmsFormRawValue['template_id']>;
  characterCount: FormControl<SendSmsFormRawValue['characterCount']>;
  type: FormControl<SendSmsFormRawValue['type']>;
};

export type SendSmsFormGroup = FormGroup<SendSmsFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class SendSmsFormService {
  createSendSmsFormGroup(sendSms: SendSmsFormGroupInput = { id: null }): SendSmsFormGroup {
    const sendSmsRawValue = this.convertSendSmsToSendSmsRawValue({
      ...this.getFormDefaults(),
      ...sendSms,
    });
    return new FormGroup<SendSmsFormGroupContent>({
      id: new FormControl(
        { value: sendSmsRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      sender: new FormControl({ value: sendSmsRawValue.sender, disabled: true }),
      receiver: new FormControl(sendSmsRawValue.receiver),
      msgdata: new FormControl({ value: sendSmsRawValue.msgdata, disabled: true }),
      sendateEnvoi: new FormControl(sendSmsRawValue.sendateEnvoi),
      dialogue: new FormControl(sendSmsRawValue.dialogue),
      isSent: new FormControl(sendSmsRawValue.isSent),
      isbulk: new FormControl(sendSmsRawValue.isbulk),
      titre: new FormControl(sendSmsRawValue.titre, [Validators.required]),
      user: new FormControl(sendSmsRawValue.user),
      destinateur: new FormControl(sendSmsRawValue.destinateur),
      destinataires: new FormControl(sendSmsRawValue.destinataires),
      statut: new FormControl(sendSmsRawValue.statut),
      template_id: new FormControl(sendSmsRawValue.template_id, [Validators.required]),
      characterCount: new FormControl(sendSmsRawValue.characterCount),
      type: new FormControl(sendSms?.type ?? MessageType.SMS),
    });
  }

  getSendSms(form: SendSmsFormGroup): ISendSms | NewSendSms {
    return this.convertSendSmsRawValueToSendSms(form.getRawValue() as SendSmsFormRawValue | NewSendSmsFormRawValue);
  }

  resetForm(form: SendSmsFormGroup, sendSms: SendSmsFormGroupInput): void {
    const sendSmsRawValue = this.convertSendSmsToSendSmsRawValue({ ...this.getFormDefaults(), ...sendSms });
    form.reset(
      {
        ...sendSmsRawValue,
        id: { value: sendSmsRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): SendSmsFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      sendateEnvoi: currentTime,
      isSent: false,
      isbulk: false,
    };
  }

  private convertSendSmsRawValueToSendSms(rawSendSms: SendSmsFormRawValue | NewSendSmsFormRawValue): ISendSms | NewSendSms {
    return {
      ...rawSendSms,
      sendateEnvoi: dayjs(rawSendSms.sendateEnvoi, DATE_TIME_FORMAT),
    };
  }

  private convertSendSmsToSendSmsRawValue(
    sendSms: ISendSms | (Partial<NewSendSms> & SendSmsFormDefaults),
  ): SendSmsFormRawValue | PartialWithRequiredKeyOf<NewSendSmsFormRawValue> {
    return {
      ...sendSms,
      sendateEnvoi: sendSms.sendateEnvoi ? sendSms.sendateEnvoi.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
