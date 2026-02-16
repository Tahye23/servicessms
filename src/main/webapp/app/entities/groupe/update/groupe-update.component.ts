import { Component, computed, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { IGroupe } from '../groupe.model';
import { GroupeService } from '../service/groupe.service';
import { GroupeFormService, GroupeFormGroup } from './groupe-form.service';
import { AccountService } from '../../../core/auth/account.service';

@Component({
  standalone: true,
  selector: 'jhi-groupe-update',
  templateUrl: './groupe-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class GroupeUpdateComponent implements OnInit {
  isSaving = false;
  groupe: IGroupe | null = null;
  isAdmin = computed(() => this.accountService.hasAnyAuthority('ROLE_ADMIN'));

  extendedUsersSharedCollection: IExtendedUser[] = [];
  private router = inject(Router);
  private accountService = inject(AccountService);
  protected groupeService = inject(GroupeService);
  protected groupeFormService = inject(GroupeFormService);
  protected extendedUserService = inject(ExtendedUserService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: GroupeFormGroup = this.groupeFormService.createGroupeFormGroup();

  compareExtendedUser = (o1: IExtendedUser | null, o2: IExtendedUser | null): boolean =>
    this.extendedUserService.compareExtendedUser(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ groupe }) => {
      this.groupe = groupe;
      if (groupe) {
        this.updateForm(groupe);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const groupe = this.groupeFormService.getGroupe(this.editForm);
    if (groupe.id !== null) {
      this.subscribeToSaveResponse(this.groupeService.update(groupe));
    } else {
      this.subscribeToSaveResponse(this.groupeService.create(groupe));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IGroupe>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: (res: HttpResponse<IGroupe>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(groupe: IGroupe | null): void {
    if (groupe?.id) {
      this.router.navigate(['/groupe', groupe.id, 'view']);
    } else {
      // Gérer le cas où l'ID est manquant
      console.error('Groupe ID is missing in response');
    }
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(groupe: IGroupe): void {
    this.groupe = groupe;
    this.groupeFormService.resetForm(this.editForm, groupe);

    this.extendedUsersSharedCollection = this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(
      this.extendedUsersSharedCollection,
      groupe.extendedUser,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.extendedUserService
      .query()
      .pipe(map((res: HttpResponse<IExtendedUser[]>) => res.body ?? []))
      .pipe(
        map((extendedUsers: IExtendedUser[]) =>
          this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(extendedUsers, this.groupe?.extendedUser),
        ),
      )
      .subscribe((extendedUsers: IExtendedUser[]) => (this.extendedUsersSharedCollection = extendedUsers));
  }
}
