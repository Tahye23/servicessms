import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IApplication } from 'app/entities/application/application.model';
import { ApplicationService } from 'app/entities/application/service/application.service';
import { ITokensApp } from '../tokens-app.model';
import { TokensAppService } from '../service/tokens-app.service';
import { TokensAppFormService, TokensAppFormGroup } from './tokens-app-form.service';

@Component({
  standalone: true,
  selector: 'jhi-tokens-app-update',
  templateUrl: './tokens-app-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class TokensAppUpdateComponent implements OnInit {
  isSaving = false;
  tokensApp: ITokensApp | null = null;

  applicationsSharedCollection: IApplication[] = [];

  protected tokensAppService = inject(TokensAppService);
  protected tokensAppFormService = inject(TokensAppFormService);
  protected applicationService = inject(ApplicationService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: TokensAppFormGroup = this.tokensAppFormService.createTokensAppFormGroup();

  compareApplication = (o1: IApplication | null, o2: IApplication | null): boolean => this.applicationService.compareApplication(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ tokensApp }) => {
      this.tokensApp = tokensApp;
      if (tokensApp) {
        this.updateForm(tokensApp);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const tokensApp = this.tokensAppFormService.getTokensApp(this.editForm);
    if (tokensApp.id !== null) {
      this.subscribeToSaveResponse(this.tokensAppService.update(tokensApp));
    } else {
      this.subscribeToSaveResponse(this.tokensAppService.create(tokensApp));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ITokensApp>>): void {
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

  protected updateForm(tokensApp: ITokensApp): void {
    this.tokensApp = tokensApp;
    this.tokensAppFormService.resetForm(this.editForm, tokensApp);

    this.applicationsSharedCollection = this.applicationService.addApplicationToCollectionIfMissing<IApplication>(
      this.applicationsSharedCollection,
      tokensApp.application,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.applicationService
      .query()
      .pipe(map((res: HttpResponse<IApplication[]>) => res.body ?? []))
      .pipe(
        map((applications: IApplication[]) =>
          this.applicationService.addApplicationToCollectionIfMissing<IApplication>(applications, this.tokensApp?.application),
        ),
      )
      .subscribe((applications: IApplication[]) => (this.applicationsSharedCollection = applications));
  }
}
