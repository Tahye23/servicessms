import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ICompany } from 'app/entities/company/company.model';
import { CompanyService } from 'app/entities/company/service/company.service';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';
import { ReferentielService } from 'app/entities/referentiel/service/referentiel.service';
import { QuestionService } from '../service/question.service';
import { IQuestion } from '../question.model';
import { QuestionFormService, QuestionFormGroup } from './question-form.service';

@Component({
  standalone: true,
  selector: 'jhi-question-update',
  templateUrl: './question-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class QuestionUpdateComponent implements OnInit {
  isSaving = false;
  question: IQuestion | null = null;

  companiesSharedCollection: ICompany[] = [];
  referentielsSharedCollection: IReferentiel[] = [];

  protected questionService = inject(QuestionService);
  protected questionFormService = inject(QuestionFormService);
  protected companyService = inject(CompanyService);
  protected referentielService = inject(ReferentielService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: QuestionFormGroup = this.questionFormService.createQuestionFormGroup();

  compareCompany = (o1: ICompany | null, o2: ICompany | null): boolean => this.companyService.compareCompany(o1, o2);

  compareReferentiel = (o1: IReferentiel | null, o2: IReferentiel | null): boolean => this.referentielService.compareReferentiel(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ question }) => {
      this.question = question;
      if (question) {
        this.updateForm(question);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const question = this.questionFormService.getQuestion(this.editForm);
    if (question.id !== null) {
      this.subscribeToSaveResponse(this.questionService.update(question));
    } else {
      this.subscribeToSaveResponse(this.questionService.create(question));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IQuestion>>): void {
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

  protected updateForm(question: IQuestion): void {
    this.question = question;
    this.questionFormService.resetForm(this.editForm, question);

    this.companiesSharedCollection = this.companyService.addCompanyToCollectionIfMissing<ICompany>(
      this.companiesSharedCollection,
      question.qusenquette,
    );
    this.referentielsSharedCollection = this.referentielService.addReferentielToCollectionIfMissing<IReferentiel>(
      this.referentielsSharedCollection,
      question.qustypequestion,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.companyService
      .query()
      .pipe(map((res: HttpResponse<ICompany[]>) => res.body ?? []))
      .pipe(
        map((companies: ICompany[]) =>
          this.companyService.addCompanyToCollectionIfMissing<ICompany>(companies, this.question?.qusenquette),
        ),
      )
      .subscribe((companies: ICompany[]) => (this.companiesSharedCollection = companies));

    this.referentielService
      .query()
      .pipe(map((res: HttpResponse<IReferentiel[]>) => res.body ?? []))
      .pipe(
        map((referentiels: IReferentiel[]) =>
          this.referentielService.addReferentielToCollectionIfMissing<IReferentiel>(referentiels, this.question?.qustypequestion),
        ),
      )
      .subscribe((referentiels: IReferentiel[]) => (this.referentielsSharedCollection = referentiels));
  }
}
