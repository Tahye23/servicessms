import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IReferentiel } from 'app/entities/referentiel/referentiel.model';
import { ReferentielService } from 'app/entities/referentiel/service/referentiel.service';
import { IService } from '../service.model';
import { ServiceService } from '../service/service.service';
import { ServiceFormService, ServiceFormGroup } from './service-form.service';

@Component({
  standalone: true,
  selector: 'jhi-service-update',
  templateUrl: './service-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class ServiceUpdateComponent implements OnInit {
  isSaving = false;
  service: IService | null = null;

  referentielsSharedCollection: IReferentiel[] = [];

  protected serviceService = inject(ServiceService);
  protected serviceFormService = inject(ServiceFormService);
  protected referentielService = inject(ReferentielService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ServiceFormGroup = this.serviceFormService.createServiceFormGroup();

  compareReferentiel = (o1: IReferentiel | null, o2: IReferentiel | null): boolean => this.referentielService.compareReferentiel(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ service }) => {
      this.service = service;
      if (service) {
        this.updateForm(service);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const service = this.serviceFormService.getService(this.editForm);
    if (service.id !== null) {
      this.subscribeToSaveResponse(this.serviceService.update(service));
    } else {
      this.subscribeToSaveResponse(this.serviceService.create(service));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IService>>): void {
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

  protected updateForm(service: IService): void {
    this.service = service;
    this.serviceFormService.resetForm(this.editForm, service);

    this.referentielsSharedCollection = this.referentielService.addReferentielToCollectionIfMissing<IReferentiel>(
      this.referentielsSharedCollection,
      service.accessType,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.referentielService
      .query()
      .pipe(map((res: HttpResponse<IReferentiel[]>) => res.body ?? []))
      .pipe(
        map((referentiels: IReferentiel[]) =>
          this.referentielService.addReferentielToCollectionIfMissing<IReferentiel>(referentiels, this.service?.accessType),
        ),
      )
      .subscribe((referentiels: IReferentiel[]) => (this.referentielsSharedCollection = referentiels));
  }
}
