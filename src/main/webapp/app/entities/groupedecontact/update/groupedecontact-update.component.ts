import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IGroupe } from 'app/entities/groupe/groupe.model';
import { GroupeService } from 'app/entities/groupe/service/groupe.service';
import { IContact } from 'app/entities/contact/contact.model';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { GroupedecontactService } from '../service/groupedecontact.service';
import { IGroupedecontact } from '../groupedecontact.model';
import { GroupedecontactFormService, GroupedecontactFormGroup } from './groupedecontact-form.service';

@Component({
  standalone: true,
  selector: 'jhi-groupedecontact-update',
  templateUrl: './groupedecontact-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class GroupedecontactUpdateComponent implements OnInit {
  isSaving = false;
  groupedecontact: IGroupedecontact | null = null;

  groupesSharedCollection: IGroupe[] = [];
  contactsSharedCollection: IContact[] = [];

  protected groupedecontactService = inject(GroupedecontactService);
  protected groupedecontactFormService = inject(GroupedecontactFormService);
  protected groupeService = inject(GroupeService);
  protected contactService = inject(ContactService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: GroupedecontactFormGroup = this.groupedecontactFormService.createGroupedecontactFormGroup();

  compareGroupe = (o1: IGroupe | null, o2: IGroupe | null): boolean => this.groupeService.compareGroupe(o1, o2);

  compareContact = (o1: IContact | null, o2: IContact | null): boolean => this.contactService.compareContact(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ groupedecontact }) => {
      this.groupedecontact = groupedecontact;
      if (groupedecontact) {
        this.updateForm(groupedecontact);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const groupedecontact = this.groupedecontactFormService.getGroupedecontact(this.editForm);
    if (groupedecontact.id !== null) {
      this.subscribeToSaveResponse(this.groupedecontactService.update(groupedecontact));
    } else {
      this.subscribeToSaveResponse(this.groupedecontactService.create(groupedecontact));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IGroupedecontact>>): void {
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

  protected updateForm(groupedecontact: IGroupedecontact): void {
    this.groupedecontact = groupedecontact;
    this.groupedecontactFormService.resetForm(this.editForm, groupedecontact);

    this.groupesSharedCollection = this.groupeService.addGroupeToCollectionIfMissing<IGroupe>(
      this.groupesSharedCollection,
      groupedecontact.cgrgroupe,
    );
    this.contactsSharedCollection = this.contactService.addContactToCollectionIfMissing<IContact>(
      this.contactsSharedCollection,
      groupedecontact.contact,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.groupeService
      .query()
      .pipe(map((res: HttpResponse<IGroupe[]>) => res.body ?? []))
      .pipe(
        map((groupes: IGroupe[]) => this.groupeService.addGroupeToCollectionIfMissing<IGroupe>(groupes, this.groupedecontact?.cgrgroupe)),
      )
      .subscribe((groupes: IGroupe[]) => (this.groupesSharedCollection = groupes));

    this.contactService
      .query({})
      .pipe(map((res: HttpResponse<IContact[]>) => res.body ?? []))
      .pipe(
        map((contacts: IContact[]) =>
          this.contactService.addContactToCollectionIfMissing<IContact>(contacts, this.groupedecontact?.contact),
        ),
      )
      .subscribe((contacts: IContact[]) => (this.contactsSharedCollection = contacts));
  }
}
