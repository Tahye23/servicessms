import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';
import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { IConversation, NewConversation } from '../conversation.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IConversation for edit and NewConversationFormGroupInput for create.
 */
type ConversationFormGroupInput = IConversation | PartialWithRequiredKeyOf<NewConversation>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IConversation | NewConversation> = Omit<T, 'covdatedebut' | 'covdatefin'> & {
  covdatedebut?: string | null;
  covdatefin?: string | null;
};

type ConversationFormRawValue = FormValueOf<IConversation>;

type NewConversationFormRawValue = FormValueOf<NewConversation>;

type ConversationFormDefaults = Pick<NewConversation, 'id' | 'covdatedebut' | 'covdatefin'>;

type ConversationFormGroupContent = {
  id: FormControl<ConversationFormRawValue['id'] | NewConversation['id']>;
  covdatedebut: FormControl<ConversationFormRawValue['covdatedebut']>;
  covdatefin: FormControl<ConversationFormRawValue['covdatefin']>;
  contact: FormControl<ConversationFormRawValue['contact']>;
  question: FormControl<ConversationFormRawValue['question']>;
  covenquette: FormControl<ConversationFormRawValue['covenquette']>;
  covstate: FormControl<ConversationFormRawValue['covstate']>;
};

export type ConversationFormGroup = FormGroup<ConversationFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ConversationFormService {
  createConversationFormGroup(conversation: ConversationFormGroupInput = { id: null }): ConversationFormGroup {
    const conversationRawValue = this.convertConversationToConversationRawValue({
      ...this.getFormDefaults(),
      ...conversation,
    });
    return new FormGroup<ConversationFormGroupContent>({
      id: new FormControl(
        { value: conversationRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      covdatedebut: new FormControl(conversationRawValue.covdatedebut),
      covdatefin: new FormControl(conversationRawValue.covdatefin),
      contact: new FormControl(conversationRawValue.contact),
      question: new FormControl(conversationRawValue.question),
      covenquette: new FormControl(conversationRawValue.covenquette),
      covstate: new FormControl(conversationRawValue.covstate),
    });
  }

  getConversation(form: ConversationFormGroup): IConversation | NewConversation {
    return this.convertConversationRawValueToConversation(form.getRawValue() as ConversationFormRawValue | NewConversationFormRawValue);
  }

  resetForm(form: ConversationFormGroup, conversation: ConversationFormGroupInput): void {
    const conversationRawValue = this.convertConversationToConversationRawValue({ ...this.getFormDefaults(), ...conversation });
    form.reset(
      {
        ...conversationRawValue,
        id: { value: conversationRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): ConversationFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      covdatedebut: currentTime,
      covdatefin: currentTime,
    };
  }

  private convertConversationRawValueToConversation(
    rawConversation: ConversationFormRawValue | NewConversationFormRawValue,
  ): IConversation | NewConversation {
    return {
      ...rawConversation,
      covdatedebut: dayjs(rawConversation.covdatedebut, DATE_TIME_FORMAT),
      covdatefin: dayjs(rawConversation.covdatefin, DATE_TIME_FORMAT),
    };
  }

  private convertConversationToConversationRawValue(
    conversation: IConversation | (Partial<NewConversation> & ConversationFormDefaults),
  ): ConversationFormRawValue | PartialWithRequiredKeyOf<NewConversationFormRawValue> {
    return {
      ...conversation,
      covdatedebut: conversation.covdatedebut ? conversation.covdatedebut.format(DATE_TIME_FORMAT) : undefined,
      covdatefin: conversation.covdatefin ? conversation.covdatefin.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
