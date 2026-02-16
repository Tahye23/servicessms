import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { WhatsappService } from '../service/whatsapp.service';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { map } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { IContact } from 'app/entities/contact/contact.model';
import { AccountService } from '../../../core/auth/account.service';
import { SendSmsService } from '../../send-sms/service/send-sms.service';
import { SendSmsUpdateComponent } from '../../send-sms/update/send-sms-update.component';
import { ISendSms } from '../../send-sms/send-sms.model';
import { IExtendedUser } from '../../extended-user/extended-user.model';
import { IGroupe } from '../../groupe/groupe.model';
import { IReferentiel } from '../../referentiel/referentiel.model';
import { GroupeService } from '../../groupe/service/groupe.service';
import { SendSmsFormGroup, SendSmsFormService } from '../../send-sms/update/send-sms-form.service';

@Component({
  selector: 'jhi-create-campagne',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule, SendSmsUpdateComponent],
  templateUrl: './create-campagne.component.html',
  styleUrl: './create-campagne.component.scss',
})
export class CreateCampagneComponent {}
