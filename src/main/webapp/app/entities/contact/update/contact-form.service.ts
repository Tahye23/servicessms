import { Injectable } from '@angular/core';
import { FormGroup, FormControl, FormArray, Validators } from '@angular/forms';
import { IContact, NewContact, PhoneInput } from '../contact.model';

// Rend obligatoire la clé 'id'
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

// Type pour l'entrée du formulaire (édition ou création)
type ContactFormGroupInput = IContact | PartialWithRequiredKeyOf<NewContact>;

// Valeurs par défaut pour le formulaire (ici, uniquement 'id')
type ContactFormDefaults = Pick<NewContact, 'id'>;

// Définition d'un type pour un champ personnalisé dans le formulaire.
// Chaque élément du FormArray sera un FormGroup contenant deux FormControl non-nullables.
export type CustomFieldFormGroup = FormGroup<{
  key: FormControl<string>;
  value: FormControl<string>;
}>;

// Définition du contenu du FormGroup pour un contact.
type ContactFormGroupContent = {
  id: FormControl<IContact['id'] | NewContact['id']>;
  conid: FormControl<IContact['conid']>;
  connom: FormControl<IContact['connom']>;
  conprenom: FormControl<IContact['conprenom']>;
  contelephone: FormControl<string | PhoneInput | null>;
  statuttraitement: FormControl<IContact['statuttraitement']>;
  groupe: FormControl<IContact['groupe']>;
  groupes: FormControl<IContact['groupes']>;
  // FormArray pour les champs personnalisés
  customFields: FormArray<CustomFieldFormGroup>;
};

// Type complet du FormGroup.
export type ContactFormGroup = FormGroup<ContactFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ContactFormService {
  createContactFormGroup(contact: ContactFormGroupInput = { id: null }): ContactFormGroup {
    const contactRawValue = {
      ...this.getFormDefaults(),
      ...contact,
    };
    return new FormGroup<ContactFormGroupContent>({
      id: new FormControl({ value: contactRawValue.id, disabled: true }, { nonNullable: true, validators: [Validators.required] }),
      conid: new FormControl(contactRawValue.conid),
      connom: new FormControl(contactRawValue.connom, { validators: [Validators.required] }),
      conprenom: new FormControl(contactRawValue.conprenom, { validators: [Validators.required] }),
      contelephone: new FormControl<string | PhoneInput | null>(contactRawValue.contelephone ?? null, {
        validators: [Validators.required],
        nonNullable: false,
      }),
      statuttraitement: new FormControl(contactRawValue.statuttraitement),
      groupe: new FormControl(contactRawValue.groupe),
      groupes: new FormControl(contactRawValue.groupes),
      customFields: new FormArray<CustomFieldFormGroup>([]),
    });
  }

  getContact(form: ContactFormGroup): IContact | NewContact {
    const rawValue = form.getRawValue();
    const customFieldsArray = rawValue.customFields as Array<{ key: string; value: string }>;
    let phoneRaw = rawValue.contelephone as any;
    const normalizedPhone: string =
      phoneRaw && typeof phoneRaw === 'object'
        ? phoneRaw.e164Number // → "+22243567898"
        : phoneRaw;
    const customObj: { [key: string]: string } = {};
    customFieldsArray.forEach(cf => {
      if (cf.key) {
        customObj[cf.key] = cf.value;
      }
    });

    return {
      id: rawValue.id,
      connom: rawValue.connom,
      conprenom: rawValue.conprenom,
      contelephone: normalizedPhone,
      groupes: rawValue.groupes,
      customFields: JSON.stringify(customObj), // ✅ Sauvegarde correcte
    };
  }

  resetForm(form: ContactFormGroup, contact: ContactFormGroupInput): void {
    const contactRawValue = { ...this.getFormDefaults(), ...contact };
    let customFieldsArray = new FormArray<CustomFieldFormGroup>([]);
    if (contactRawValue.customFields && typeof contactRawValue.customFields === 'string') {
      try {
        const parsed = JSON.parse(contactRawValue.customFields);
        Object.keys(parsed).forEach(key => {
          customFieldsArray.push(this.fbGroupForCustomField(key, parsed[key]));
        });
      } catch (e) {
        console.error('Erreur de parsing des customFields', e);
      }
    }
    const { customFields, ...rest } = contactRawValue;
    form.reset({
      ...rest,
      id: { value: contactRawValue.id, disabled: true },
      customFields: customFieldsArray,
    } as any);
  }

  // Création d'un FormGroup pour un champ personnalisé avec nonNullable défini
  private fbGroupForCustomField(key: string, value: string): CustomFieldFormGroup {
    return new FormGroup<{
      key: FormControl<string>;
      value: FormControl<string>;
    }>({
      key: new FormControl(key, { nonNullable: true, validators: [Validators.required] }),
      value: new FormControl(value, { nonNullable: true }),
    });
  }

  private getFormDefaults(): ContactFormDefaults {
    return { id: null };
  }
}
