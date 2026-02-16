import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IQuestion } from 'app/entities/question/question.model';
import { QuestionService } from 'app/entities/question/service/question.service';
import { IChoix } from '../choix.model';
import { ChoixService } from '../service/choix.service';
import { ChoixFormService, ChoixFormGroup } from './choix-form.service';

@Component({
  standalone: true,
  selector: 'jhi-choix-update',
  templateUrl: './choix-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class ChoixUpdateComponent implements OnInit {
  isSaving = false;
  choix: IChoix | null = null;

  questionsSharedCollection: IQuestion[] = [];

  protected choixService = inject(ChoixService);
  protected choixFormService = inject(ChoixFormService);
  protected questionService = inject(QuestionService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ChoixFormGroup = this.choixFormService.createChoixFormGroup();

  compareQuestion = (o1: IQuestion | null, o2: IQuestion | null): boolean => this.questionService.compareQuestion(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ choix }) => {
      this.choix = choix;
      if (choix) {
        this.updateForm(choix);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const choix = this.choixFormService.getChoix(this.editForm);
    if (choix.id !== null) {
      this.subscribeToSaveResponse(this.choixService.update(choix));
    } else {
      this.subscribeToSaveResponse(this.choixService.create(choix));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IChoix>>): void {
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

  protected updateForm(choix: IChoix): void {
    this.choix = choix;
    this.choixFormService.resetForm(this.editForm, choix);

    this.questionsSharedCollection = this.questionService.addQuestionToCollectionIfMissing<IQuestion>(
      this.questionsSharedCollection,
      choix.choquestion,
      choix.choquestionSuivante,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.questionService
      .query()
      .pipe(map((res: HttpResponse<IQuestion[]>) => res.body ?? []))
      .pipe(
        map((questions: IQuestion[]) =>
          this.questionService.addQuestionToCollectionIfMissing<IQuestion>(
            questions,
            this.choix?.choquestion,
            this.choix?.choquestionSuivante,
          ),
        ),
      )
      .subscribe((questions: IQuestion[]) => (this.questionsSharedCollection = questions));
  }
}
