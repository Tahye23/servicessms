import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IReferentiel } from '../referentiel.model';
import { ReferentielService } from '../service/referentiel.service';
import { ReferentielFormService, ReferentielFormGroup } from './referentiel-form.service';

@Component({
  standalone: true,
  selector: 'jhi-referentiel-update',
  templateUrl: './referentiel-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class ReferentielUpdateComponent implements OnInit {
  isSaving = false;
  referentiel: IReferentiel | null = null;

  protected referentielService = inject(ReferentielService);
  protected referentielFormService = inject(ReferentielFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ReferentielFormGroup = this.referentielFormService.createReferentielFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ referentiel }) => {
      this.referentiel = referentiel;
      if (referentiel) {
        this.updateForm(referentiel);
      }
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const referentiel = this.referentielFormService.getReferentiel(this.editForm);
    if (referentiel.id !== null) {
      this.subscribeToSaveResponse(this.referentielService.update(referentiel));
    } else {
      this.subscribeToSaveResponse(this.referentielService.create(referentiel));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IReferentiel>>): void {
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

  protected updateForm(referentiel: IReferentiel): void {
    this.referentiel = referentiel;
    this.referentielFormService.resetForm(this.editForm, referentiel);
  }
}
