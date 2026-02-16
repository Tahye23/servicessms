import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IEntitedetest } from '../entitedetest.model';
import { EntitedetestService } from '../service/entitedetest.service';
import { EntitedetestFormService, EntitedetestFormGroup } from './entitedetest-form.service';

@Component({
  standalone: true,
  selector: 'jhi-entitedetest-update',
  templateUrl: './entitedetest-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class EntitedetestUpdateComponent implements OnInit {
  isSaving = false;
  entitedetest: IEntitedetest | null = null;

  protected entitedetestService = inject(EntitedetestService);
  protected entitedetestFormService = inject(EntitedetestFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: EntitedetestFormGroup = this.entitedetestFormService.createEntitedetestFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ entitedetest }) => {
      this.entitedetest = entitedetest;
      if (entitedetest) {
        this.updateForm(entitedetest);
      }
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const entitedetest = this.entitedetestFormService.getEntitedetest(this.editForm);
    if (entitedetest.id !== null) {
      this.subscribeToSaveResponse(this.entitedetestService.update(entitedetest));
    } else {
      this.subscribeToSaveResponse(this.entitedetestService.create(entitedetest));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IEntitedetest>>): void {
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

  protected updateForm(entitedetest: IEntitedetest): void {
    this.entitedetest = entitedetest;
    this.entitedetestFormService.resetForm(this.editForm, entitedetest);
  }
}
