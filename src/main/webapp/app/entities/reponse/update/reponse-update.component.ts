import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IQuestion } from 'app/entities/question/question.model';
import { QuestionService } from 'app/entities/question/service/question.service';
import { IContact } from 'app/entities/contact/contact.model';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { ReponseService } from '../service/reponse.service';
import { IReponse } from '../reponse.model';
import { ReponseFormService, ReponseFormGroup } from './reponse-form.service';

@Component({
  standalone: true,
  selector: 'jhi-reponse-update',
  templateUrl: './reponse-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class ReponseUpdateComponent implements OnInit {
  isSaving = false;
  reponse: IReponse | null = null;

  questionsSharedCollection: IQuestion[] = [];
  contactsSharedCollection: IContact[] = [];

  protected reponseService = inject(ReponseService);
  protected reponseFormService = inject(ReponseFormService);
  protected questionService = inject(QuestionService);
  protected contactService = inject(ContactService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ReponseFormGroup = this.reponseFormService.createReponseFormGroup();

  compareQuestion = (o1: IQuestion | null, o2: IQuestion | null): boolean => this.questionService.compareQuestion(o1, o2);

  compareContact = (o1: IContact | null, o2: IContact | null): boolean => this.contactService.compareContact(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ reponse }) => {
      this.reponse = reponse;
      if (reponse) {
        this.updateForm(reponse);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const reponse = this.reponseFormService.getReponse(this.editForm);
    if (reponse.id !== null) {
      this.subscribeToSaveResponse(this.reponseService.update(reponse));
    } else {
      this.subscribeToSaveResponse(this.reponseService.create(reponse));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IReponse>>): void {
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

  protected updateForm(reponse: IReponse): void {
    this.reponse = reponse;
    this.reponseFormService.resetForm(this.editForm, reponse);

    this.questionsSharedCollection = this.questionService.addQuestionToCollectionIfMissing<IQuestion>(
      this.questionsSharedCollection,
      reponse.repquestion,
    );
    this.contactsSharedCollection = this.contactService.addContactToCollectionIfMissing<IContact>(
      this.contactsSharedCollection,
      reponse.repcontact,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.questionService
      .query()
      .pipe(map((res: HttpResponse<IQuestion[]>) => res.body ?? []))
      .pipe(
        map((questions: IQuestion[]) =>
          this.questionService.addQuestionToCollectionIfMissing<IQuestion>(questions, this.reponse?.repquestion),
        ),
      )
      .subscribe((questions: IQuestion[]) => (this.questionsSharedCollection = questions));

    this.contactService
      .query({})
      .pipe(map((res: HttpResponse<IContact[]>) => res.body ?? []))
      .pipe(
        map((contacts: IContact[]) => this.contactService.addContactToCollectionIfMissing<IContact>(contacts, this.reponse?.repcontact)),
      )
      .subscribe((contacts: IContact[]) => (this.contactsSharedCollection = contacts));
  }
}
