import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IApi } from '../api.model';
import { ApiService } from '../service/api.service';
import { ApiFormService, ApiFormGroup } from './api-form.service';

@Component({
  standalone: true,
  selector: 'jhi-api-update',
  templateUrl: './api-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class ApiUpdateComponent implements OnInit {
  isSaving = false;
  api: IApi | null = null;

  protected apiService = inject(ApiService);
  protected apiFormService = inject(ApiFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ApiFormGroup = this.apiFormService.createApiFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ api }) => {
      this.api = api;
      if (api) {
        this.updateForm(api);
      }
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const api = this.apiFormService.getApi(this.editForm);
    if (api.id !== null) {
      this.subscribeToSaveResponse(this.apiService.update(api));
    } else {
      this.subscribeToSaveResponse(this.apiService.create(api));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IApi>>): void {
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

  protected updateForm(api: IApi): void {
    this.api = api;
    this.apiFormService.resetForm(this.editForm, api);
  }
}
