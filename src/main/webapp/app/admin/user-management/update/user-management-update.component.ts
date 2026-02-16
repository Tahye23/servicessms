import { Component, inject, OnInit, signal } from '@angular/core';
import { FormGroup, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { LANGUAGES } from 'app/config/language.constants';
import { IUser } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import { AccountService } from '../../../core/auth/account.service';
import { PERMISSION_CATEGORIES, PermissionCategory, PermissionsService } from '../permissions.types';

const userTemplate = {} as IUser;

const newUser: IUser = {
  langKey: 'fr',
  activated: true,
} as IUser;

@Component({
  standalone: true,
  selector: 'jhi-user-mgmt-update',
  templateUrl: './user-management-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export default class UserManagementUpdateComponent implements OnInit {
  languages = LANGUAGES;
  authorities = signal<string[]>([]);
  isSaving = signal(false);

  // Services
  private accountService = inject(AccountService);
  private userService = inject(UserManagementService);
  private route = inject(ActivatedRoute);
  private permissionsService = new PermissionsService();

  // Permissions system
  userPermissions: string[] = [];
  permissionCategories = PERMISSION_CATEGORIES;

  editForm = new FormGroup({
    id: new FormControl(userTemplate.id),
    login: new FormControl(userTemplate.login, {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(1),
        Validators.maxLength(50),
        Validators.pattern('^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$'),
      ],
    }),
    firstName: new FormControl(userTemplate.firstName, {
      validators: [Validators.maxLength(50)],
    }),
    lastName: new FormControl(userTemplate.lastName, {
      validators: [Validators.maxLength(50)],
    }),
    email: new FormControl(userTemplate.email, {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email],
    }),
    activated: new FormControl(userTemplate.activated, {
      nonNullable: true,
    }),
    langKey: new FormControl(userTemplate.langKey, {
      nonNullable: true,
    }),
    authorities: new FormControl(userTemplate.authorities, {
      nonNullable: true,
    }),
    expediteur: new FormControl(userTemplate.expediteur, {
      nonNullable: true,
    }),
    phone: new FormControl(userTemplate.phone ?? '', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    permissions: new FormControl('', { nonNullable: true }),
  });

  ngOnInit(): void {
    this.route.data.subscribe(({ user }) => {
      if (user) {
        this.editForm.reset(user);
        this.loadUserPermissions(user);
      } else {
        this.editForm.reset(newUser);
        this.userPermissions = this.permissionsService.getPermissionsByRole('ROLE_USER');
        this.updatePermissionsField();
      }
    });

    this.userService.authorities().subscribe(authorities => {
      this.authorities.set(authorities);
    });
  }

  /**
   * Charger les permissions depuis les données utilisateur
   */
  private loadUserPermissions(user: IUser): void {
    if (user.permissions) {
      try {
        this.userPermissions = JSON.parse(user.permissions);
      } catch {
        this.userPermissions = this.getDefaultPermissionsForRoles(user.authorities || []);
      }
    } else {
      this.userPermissions = this.getDefaultPermissionsForRoles(user.authorities || []);
    }
    this.updatePermissionsField();
  }

  /**
   * Permissions par défaut selon les rôles
   */
  getDefaultPermissionsForRoles(roles: string[]): string[] {
    if (roles.includes('ROLE_ADMIN')) {
      return this.permissionsService.getPermissionsByRole('ROLE_ADMIN');
    }
    if (roles.includes('ROLE_PARTNER')) {
      return this.permissionsService.getPermissionsByRole('ROLE_PARTNER');
    }
    return this.permissionsService.getPermissionsByRole('ROLE_USER');
  }

  /**
   * Getters pour les rôles de l'utilisateur connecté
   */
  get isAdmin(): boolean {
    const account = this.accountService.trackCurrentAccount()();
    return account ? account.authorities.includes('ROLE_ADMIN') : false;
  }

  get isPartner(): boolean {
    const account = this.accountService.trackCurrentAccount()();
    return account ? account.authorities.includes('ROLE_PARTNER') : false;
  }

  /**
   * Rôles disponibles selon l'utilisateur connecté
   */
  getAvailableRoles() {
    const roles = [
      {
        value: 'ROLE_USER',
        label: 'Utilisateur',
        description: 'Accès de base aux fonctionnalités',
        level: 'Standard',
        badgeClass: 'bg-green-100 text-green-800',
      },
    ];

    if (this.isAdmin) {
      roles.push({
        value: 'ROLE_PARTNER',
        label: 'Partenaire',
        description: "Accès étendu et gestion d'équipe",
        level: 'Élevé',
        badgeClass: 'bg-blue-100 text-blue-800',
      });
    }

    if (this.isAdmin) {
      roles.push({
        value: 'ROLE_ADMIN',
        label: 'Administrateur',
        description: 'Accès complet au système',
        level: 'Maximum',
        badgeClass: 'bg-red-100 text-red-800',
      });
    }

    return roles;
  }

  /**
   * Vérifier si un rôle est sélectionné
   */
  isRoleSelected(role: string): boolean {
    const authorities = this.editForm.get('authorities')?.value || [];
    return authorities.includes(role);
  }

  /**
   * Sélectionner un rôle principal
   */
  selectRole(role: string): void {
    this.editForm.patchValue({ authorities: [role] });
    this.userPermissions = this.getDefaultPermissionsForRoles([role]);
    this.updatePermissionsField();
  }

  /**
   * Vérifier si une permission est accordée
   */
  hasPermission(permission: string): boolean {
    return this.userPermissions.includes(permission);
  }

  /**
   * Toggle d'une permission simple
   */
  togglePermission(permission: string, event: any): void {
    if (event.target.checked) {
      if (!this.userPermissions.includes(permission)) {
        this.userPermissions.push(permission);
      }
    } else {
      this.userPermissions = this.userPermissions.filter(p => p !== permission);
    }
    this.updatePermissionsField();
  }

  /**
   * Toggle d'une permission avec gestion hiérarchique
   */
  togglePermissionWithChildren(permissionId: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      // Ajouter la permission parent
      if (!this.userPermissions.includes(permissionId)) {
        this.userPermissions.push(permissionId);
      }

      // Ajouter automatiquement les permissions enfants par défaut
      const children = this.permissionsService.getChildPermissions(permissionId);
      children.forEach(child => {
        if (child.defaultChecked && !this.userPermissions.includes(child.id)) {
          this.userPermissions.push(child.id);
        }
      });
    } else {
      // Retirer la permission parent
      this.userPermissions = this.userPermissions.filter(p => p !== permissionId);

      // Retirer aussi tous les enfants
      const children = this.permissionsService.getChildPermissions(permissionId);
      children.forEach(child => {
        this.userPermissions = this.userPermissions.filter(p => p !== child.id);
      });
    }

    this.updatePermissionsField();
  }

  /**
   * Vérifier si une permission parent est dans un état indéterminé
   */
  isParentIndeterminate(permissionId: string): boolean {
    const children = this.permissionsService.getChildPermissions(permissionId);
    if (children.length === 0) return false;

    const checkedChildren = children.filter(child => this.userPermissions.includes(child.id));
    return checkedChildren.length > 0 && checkedChildren.length < children.length;
  }

  /**
   * Vérifier si tous les enfants d'une permission parent sont cochés
   */
  isParentFullyChecked(permissionId: string): boolean {
    const children = this.permissionsService.getChildPermissions(permissionId);
    if (children.length === 0) return this.userPermissions.includes(permissionId);

    return children.every(child => this.userPermissions.includes(child.id));
  }

  /**
   * Compter les enfants cochés d'une permission
   */
  getCheckedChildrenCount(permissionId: string): number {
    const children = this.permissionsService.getChildPermissions(permissionId);
    return children.filter(child => this.userPermissions.includes(child.id)).length;
  }

  /**
   * Obtenir les catégories filtrées selon le rôle
   */
  getFilteredCategories(): PermissionCategory[] {
    if (this.isAdmin) {
      return this.permissionCategories;
    }

    if (this.isPartner) {
      return this.permissionCategories
        .map(category => {
          if (category.id === 'admin') {
            return {
              ...category,
              permissions: category.permissions.filter(p => p.id === 'users'),
            };
          }
          return category;
        })
        .filter(category => category.permissions.length > 0);
    }

    return this.permissionCategories.filter(cat => cat.id !== 'admin');
  }

  /**
   * Obtenir les permissions sélectionnées dans une catégorie
   */
  getSelectedPermissionsInCategory(categoryId: string): string[] {
    const category = this.permissionCategories.find(c => c.id === categoryId);
    if (!category) return [];

    const allCategoryPermissions: string[] = [];
    category.permissions.forEach(permission => {
      allCategoryPermissions.push(permission.id);
      if (permission.children) {
        allCategoryPermissions.push(...permission.children.map(c => c.id));
      }
    });

    return this.userPermissions.filter(p => allCategoryPermissions.includes(p));
  }

  /**
   * Mettre à jour le champ caché des permissions
   */
  private updatePermissionsField(): void {
    this.editForm.patchValue({
      permissions: JSON.stringify(this.userPermissions),
    });
  }

  /**
   * Sélections rapides
   */
  selectAllPermissions(): void {
    this.userPermissions = this.permissionsService.getAllPermissions().map(p => p.id);
    this.updatePermissionsField();
  }

  clearAllPermissions(): void {
    this.userPermissions = [];
    this.updatePermissionsField();
  }

  selectBasicPermissions(): void {
    this.userPermissions = this.permissionsService.getPermissionsByRole('ROLE_USER');
    this.updatePermissionsField();
  }

  selectPermissionsByRole(role: string): void {
    this.userPermissions = this.permissionsService.getPermissionsByRole(role);
    this.updatePermissionsField();
  }

  selectPermissionsByCategory(categoryId: string): void {
    const categoryPermissions = this.permissionsService.getPermissionsByCategory(categoryId);

    categoryPermissions.forEach(permission => {
      if (!this.userPermissions.includes(permission.id)) {
        this.userPermissions.push(permission.id);
      }

      // Ajouter les permissions par défaut des enfants
      permission.children?.forEach(child => {
        if (child.defaultChecked && !this.userPermissions.includes(child.id)) {
          this.userPermissions.push(child.id);
        }
      });
    });

    this.updatePermissionsField();
  }

  /**
   * Obtenir les permissions sélectionnées
   */
  getSelectedPermissions(): string[] {
    return this.userPermissions;
  }

  /**
   * Supprimer une permission
   */
  removePermission(permission: string): void {
    this.userPermissions = this.userPermissions.filter(p => p !== permission);

    // Si c'est une permission parent, retirer aussi les enfants
    const children = this.permissionsService.getChildPermissions(permission);
    children.forEach(child => {
      this.userPermissions = this.userPermissions.filter(p => p !== child.id);
    });

    // Si c'est une permission enfant, vérifier si le parent doit être retiré
    const parent = this.permissionsService.getParentPermission(permission);
    if (parent) {
      const siblings = this.permissionsService.getChildPermissions(parent.id);
      const remainingSiblings = siblings.filter(s => this.userPermissions.includes(s.id));
      if (remainingSiblings.length === 0) {
        this.userPermissions = this.userPermissions.filter(p => p !== parent.id);
      }
    }

    this.updatePermissionsField();
  }

  /**
   * Obtenir le label d'une permission
   */
  getPermissionLabel(permission: string): string {
    return this.permissionsService.getPermissionLabel(permission);
  }

  /**
   * Calcul du pourcentage de completion du formulaire
   */
  getFormCompletionPercentage(): number {
    const formValues = this.editForm.value;
    const totalFields = 6;
    let completedFields = 0;

    if (formValues.login?.trim()) completedFields++;
    if (formValues.email?.trim()) completedFields++;
    if (formValues.firstName?.trim()) completedFields++;
    if (formValues.lastName?.trim()) completedFields++;
    if (formValues.phone?.trim()) completedFields++;
    if (formValues.activated !== null) completedFields++;

    return Math.round((completedFields / totalFields) * 100);
  }

  /**
   * Sauvegarde avec validation des permissions
   */
  save(): void {
    if (this.editForm.invalid) {
      Object.keys(this.editForm.controls).forEach(key => {
        this.editForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isSaving.set(true);
    const formValue = this.editForm.getRawValue();

    const user: IUser = {
      id: formValue.id,
      login: formValue.login,
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      email: formValue.email,
      activated: formValue.activated,
      langKey: formValue.langKey,
      authorities: formValue.authorities,
      expediteur: formValue.expediteur,
      phone: formValue.phone,
      permissions: formValue.permissions,
    };

    // Validation des règles métier pour les rôles
    if (this.isPartner && !user.authorities?.includes('ROLE_USER')) {
      user.authorities = ['ROLE_USER'];
    }

    if (!user.authorities || user.authorities.length === 0) {
      user.authorities = ['ROLE_USER'];
    }

    console.log('Saving user with permissions:', {
      user,
      permissions: this.userPermissions,
    });

    if (user.id !== null) {
      this.userService.update(user).subscribe({
        next: () => this.onSaveSuccess(),
        error: () => this.onSaveError(),
      });
    } else {
      this.userService.create(user).subscribe({
        next: () => this.onSaveSuccess(),
        error: () => this.onSaveError(),
      });
    }
  }

  /**
   * Navigation vers la page précédente
   */
  previousState(): void {
    window.history.back();
  }

  /**
   * Callbacks de sauvegarde
   */
  private onSaveSuccess(): void {
    this.isSaving.set(false);
    console.log('Utilisateur sauvegardé avec succès');
    this.previousState();
  }

  private onSaveError(): void {
    this.isSaving.set(false);
    console.error('Erreur lors de la sauvegarde');
  }

  /**
   * Utilitaires
   */
  isFieldRequired(fieldName: string): boolean {
    const requiredFields = ['login', 'email', 'phone'];
    return requiredFields.includes(fieldName);
  }

  canSubmit(): boolean {
    return this.editForm.valid && !this.isSaving();
  }

  resetForm(): void {
    if (confirm('Êtes-vous sûr de vouloir réinitialiser le formulaire ?')) {
      this.editForm.reset(newUser);
      this.userPermissions = this.permissionsService.getPermissionsByRole('ROLE_USER');
      this.updatePermissionsField();
    }
  }

  formatPhoneNumber(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    if (value.startsWith('33')) {
      value = '+' + value;
    } else if (value.startsWith('0')) {
      value = '+33' + value.substring(1);
    }
    this.editForm.patchValue({ phone: value });
  }
}
