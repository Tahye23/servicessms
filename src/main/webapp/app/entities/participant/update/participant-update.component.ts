import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IContact } from 'app/entities/contact/contact.model';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { ICompany } from 'app/entities/company/company.model';
import { CompanyService } from 'app/entities/company/service/company.service';
import { ParticipantService } from '../service/participant.service';
import { IParticipant } from '../participant.model';
import { ParticipantFormService, ParticipantFormGroup } from './participant-form.service';

@Component({
  standalone: true,
  selector: 'jhi-participant-update',
  templateUrl: './participant-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class ParticipantUpdateComponent implements OnInit {
  isSaving = false;
  participant: IParticipant | null = null;

  contactsSharedCollection: IContact[] = [];
  companiesSharedCollection: ICompany[] = [];

  protected participantService = inject(ParticipantService);
  protected participantFormService = inject(ParticipantFormService);
  protected contactService = inject(ContactService);
  protected companyService = inject(CompanyService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ParticipantFormGroup = this.participantFormService.createParticipantFormGroup();

  compareContact = (o1: IContact | null, o2: IContact | null): boolean => this.contactService.compareContact(o1, o2);

  compareCompany = (o1: ICompany | null, o2: ICompany | null): boolean => this.companyService.compareCompany(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ participant }) => {
      this.participant = participant;
      if (participant) {
        this.updateForm(participant);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const participant = this.participantFormService.getParticipant(this.editForm);
    if (participant.id !== null) {
      this.subscribeToSaveResponse(this.participantService.update(participant));
    } else {
      this.subscribeToSaveResponse(this.participantService.create(participant));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IParticipant>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(participant: IParticipant): void {
    this.participant = participant;
    this.participantFormService.resetForm(this.editForm, participant);

    this.contactsSharedCollection = this.contactService.addContactToCollectionIfMissing<IContact>(
      this.contactsSharedCollection,
      participant.patcontact,
    );
    this.companiesSharedCollection = this.companyService.addCompanyToCollectionIfMissing<ICompany>(
      this.companiesSharedCollection,
      participant.patenquette,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.contactService
      .query({})
      .pipe(map((res: HttpResponse<IContact[]>) => res.body ?? []))
      .pipe(
        map((contacts: IContact[]) =>
          this.contactService.addContactToCollectionIfMissing<IContact>(contacts, this.participant?.patcontact),
        ),
      )
      .subscribe((contacts: IContact[]) => (this.contactsSharedCollection = contacts));

    this.companyService
      .query()
      .pipe(map((res: HttpResponse<ICompany[]>) => res.body ?? []))
      .pipe(
        map((companies: ICompany[]) =>
          this.companyService.addCompanyToCollectionIfMissing<ICompany>(companies, this.participant?.patenquette),
        ),
      )
      .subscribe((companies: ICompany[]) => (this.companiesSharedCollection = companies));
  }
}
