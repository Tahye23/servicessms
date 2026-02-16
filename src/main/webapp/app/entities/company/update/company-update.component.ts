import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';
import { ReferentielService } from 'app/entities/referentiel/service/referentiel.service';
import { CompanyService } from '../service/company.service';
import { ICompany } from '../company.model';
import { CompanyFormService, CompanyFormGroup } from './company-form.service';

@Component({
  standalone: true,
  selector: 'jhi-company-update',
  templateUrl: './company-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class CompanyUpdateComponent implements OnInit {
  isSaving = false;
  company: ICompany | null = null;

  extendedUsersSharedCollection: IExtendedUser[] = [];
  referentielsSharedCollection: IReferentiel[] = [];

  protected companyService = inject(CompanyService);
  protected companyFormService = inject(CompanyFormService);
  protected extendedUserService = inject(ExtendedUserService);
  protected referentielService = inject(ReferentielService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: CompanyFormGroup = this.companyFormService.createCompanyFormGroup();

  compareExtendedUser = (o1: IExtendedUser | null, o2: IExtendedUser | null): boolean =>
    this.extendedUserService.compareExtendedUser(o1, o2);

  compareReferentiel = (o1: IReferentiel | null, o2: IReferentiel | null): boolean => this.referentielService.compareReferentiel(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ company }) => {
      this.company = company;
      if (company) {
        this.updateForm(company);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const company = this.companyFormService.getCompany(this.editForm);
    if (company.id !== null) {
      this.subscribeToSaveResponse(this.companyService.update(company));
    } else {
      this.subscribeToSaveResponse(this.companyService.create(company));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ICompany>>): void {
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

  protected updateForm(company: ICompany): void {
    this.company = company;
    this.companyFormService.resetForm(this.editForm, company);

    this.extendedUsersSharedCollection = this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(
      this.extendedUsersSharedCollection,
      company.camUser,
    );
    this.referentielsSharedCollection = this.referentielService.addReferentielToCollectionIfMissing<IReferentiel>(
      this.referentielsSharedCollection,
      company.camstatus,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.extendedUserService
      .query()
      .pipe(map((res: HttpResponse<IExtendedUser[]>) => res.body ?? []))
      .pipe(
        map((extendedUsers: IExtendedUser[]) =>
          this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(extendedUsers, this.company?.camUser),
        ),
      )
      .subscribe((extendedUsers: IExtendedUser[]) => (this.extendedUsersSharedCollection = extendedUsers));

    this.referentielService
      .query()
      .pipe(map((res: HttpResponse<IReferentiel[]>) => res.body ?? []))
      .pipe(
        map((referentiels: IReferentiel[]) =>
          this.referentielService.addReferentielToCollectionIfMissing<IReferentiel>(referentiels, this.company?.camstatus),
        ),
      )
      .subscribe((referentiels: IReferentiel[]) => (this.referentielsSharedCollection = referentiels));
  }
}
