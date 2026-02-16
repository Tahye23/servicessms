import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IContact } from 'app/entities/contact/contact.model';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { IQuestion } from 'app/entities/question/question.model';
import { QuestionService } from 'app/entities/question/service/question.service';
import { ICompany } from 'app/entities/company/company.model';
import { CompanyService } from 'app/entities/company/service/company.service';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';
import { ReferentielService } from 'app/entities/referentiel/service/referentiel.service';
import { ConversationService } from '../service/conversation.service';
import { IConversation } from '../conversation.model';
import { ConversationFormService, ConversationFormGroup } from './conversation-form.service';

@Component({
  standalone: true,
  selector: 'jhi-conversation-update',
  templateUrl: './conversation-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class ConversationUpdateComponent implements OnInit {
  isSaving = false;
  conversation: IConversation | null = null;

  contactsSharedCollection: IContact[] = [];
  questionsSharedCollection: IQuestion[] = [];
  companiesSharedCollection: ICompany[] = [];
  referentielsSharedCollection: IReferentiel[] = [];

  protected conversationService = inject(ConversationService);
  protected conversationFormService = inject(ConversationFormService);
  protected contactService = inject(ContactService);
  protected questionService = inject(QuestionService);
  protected companyService = inject(CompanyService);
  protected referentielService = inject(ReferentielService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ConversationFormGroup = this.conversationFormService.createConversationFormGroup();

  compareContact = (o1: IContact | null, o2: IContact | null): boolean => this.contactService.compareContact(o1, o2);

  compareQuestion = (o1: IQuestion | null, o2: IQuestion | null): boolean => this.questionService.compareQuestion(o1, o2);

  compareCompany = (o1: ICompany | null, o2: ICompany | null): boolean => this.companyService.compareCompany(o1, o2);

  compareReferentiel = (o1: IReferentiel | null, o2: IReferentiel | null): boolean => this.referentielService.compareReferentiel(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ conversation }) => {
      this.conversation = conversation;
      if (conversation) {
        this.updateForm(conversation);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const conversation = this.conversationFormService.getConversation(this.editForm);
    if (conversation.id !== null) {
      this.subscribeToSaveResponse(this.conversationService.update(conversation));
    } else {
      this.subscribeToSaveResponse(this.conversationService.create(conversation));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IConversation>>): void {
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

  protected updateForm(conversation: IConversation): void {
    this.conversation = conversation;
    this.conversationFormService.resetForm(this.editForm, conversation);

    this.contactsSharedCollection = this.contactService.addContactToCollectionIfMissing<IContact>(
      this.contactsSharedCollection,
      conversation.contact,
    );
    this.questionsSharedCollection = this.questionService.addQuestionToCollectionIfMissing<IQuestion>(
      this.questionsSharedCollection,
      conversation.question,
    );
    this.companiesSharedCollection = this.companyService.addCompanyToCollectionIfMissing<ICompany>(
      this.companiesSharedCollection,
      conversation.covenquette,
    );
    this.referentielsSharedCollection = this.referentielService.addReferentielToCollectionIfMissing<IReferentiel>(
      this.referentielsSharedCollection,
      conversation.covstate,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.contactService
      .query({})
      .pipe(map((res: HttpResponse<IContact[]>) => res.body ?? []))
      .pipe(
        map((contacts: IContact[]) => this.contactService.addContactToCollectionIfMissing<IContact>(contacts, this.conversation?.contact)),
      )
      .subscribe((contacts: IContact[]) => (this.contactsSharedCollection = contacts));

    this.questionService
      .query()
      .pipe(map((res: HttpResponse<IQuestion[]>) => res.body ?? []))
      .pipe(
        map((questions: IQuestion[]) =>
          this.questionService.addQuestionToCollectionIfMissing<IQuestion>(questions, this.conversation?.question),
        ),
      )
      .subscribe((questions: IQuestion[]) => (this.questionsSharedCollection = questions));

    this.companyService
      .query()
      .pipe(map((res: HttpResponse<ICompany[]>) => res.body ?? []))
      .pipe(
        map((companies: ICompany[]) =>
          this.companyService.addCompanyToCollectionIfMissing<ICompany>(companies, this.conversation?.covenquette),
        ),
      )
      .subscribe((companies: ICompany[]) => (this.companiesSharedCollection = companies));

    this.referentielService
      .query()
      .pipe(map((res: HttpResponse<IReferentiel[]>) => res.body ?? []))
      .pipe(
        map((referentiels: IReferentiel[]) =>
          this.referentielService.addReferentielToCollectionIfMissing<IReferentiel>(referentiels, this.conversation?.covstate),
        ),
      )
      .subscribe((referentiels: IReferentiel[]) => (this.referentielsSharedCollection = referentiels));
  }
}
