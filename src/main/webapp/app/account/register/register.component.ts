import { Component, AfterViewInit, ElementRef, inject, signal, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterModule, Router } from '@angular/router';
import { FormGroup, FormControl, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';

import { EMAIL_ALREADY_USED_TYPE, LOGIN_ALREADY_USED_TYPE } from 'app/config/error.constants';
import SharedModule from 'app/shared/shared.module';
import PasswordStrengthBarComponent from '../password/password-strength-bar/password-strength-bar.component';
import { RegisterService } from './register.service';

@Component({
  standalone: true,
  selector: 'jhi-register',
  imports: [SharedModule, RouterModule, FormsModule, ReactiveFormsModule, PasswordStrengthBarComponent, CommonModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export default class RegisterComponent implements AfterViewInit {
  login = viewChild.required<ElementRef>('login');

  // États du composant
  successMessage = '';
  errorMessage = '';
  doNotMatch = signal(false);
  error = signal(false);
  errorEmailExists = signal(false);
  errorUserExists = signal(false);
  success = signal(false);
  loading = signal(false);
  showPassword = signal(false);
  showConfirmPassword = signal(false);
  currentStep = signal(1);

  // Formulaire d'inscription
  registerForm = new FormGroup({
    firstName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(2), Validators.maxLength(50)],
    }),
    lastName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(2), Validators.maxLength(50)],
    }),
    login: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern('^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$'),
      ],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(50)],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(50)],
    }),
    acceptTerms: new FormControl(false, {
      nonNullable: true,
      validators: [Validators.requiredTrue],
    }),
  });

  private translateService = inject(TranslateService);
  private registerService = inject(RegisterService);
  private router = inject(Router);

  ngAfterViewInit(): void {
    // Focus sur le premier champ au lieu du login
    setTimeout(() => {
      const firstNameInput = document.getElementById('firstName');
      if (firstNameInput) {
        firstNameInput.focus();
      }
    }, 100);
  }

  /**
   * Basculer la visibilité du mot de passe
   */
  togglePasswordVisibility(): void {
    this.showPassword.set(!this.showPassword());
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword.set(!this.showConfirmPassword());
  }

  /**
   * Navigation entre les étapes
   */
  nextStep(): void {
    if (this.currentStep() < 2) {
      this.currentStep.set(this.currentStep() + 1);
    }
  }

  previousStep(): void {
    if (this.currentStep() > 1) {
      this.currentStep.set(this.currentStep() - 1);
    }
  }

  /**
   * Vérifier si l'étape 1 est valide
   */
  isStep1Valid(): boolean {
    const firstName = this.registerForm.get('firstName');
    const lastName = this.registerForm.get('lastName');
    const email = this.registerForm.get('email');

    return !!(firstName?.valid && lastName?.valid && email?.valid);
  }

  /**
   * Réinitialiser le formulaire
   */
  resetForm(): void {
    this.registerForm.reset();
    this.doNotMatch.set(false);
    this.error.set(false);
    this.errorEmailExists.set(false);
    this.errorUserExists.set(false);
    this.success.set(false);
    this.loading.set(false);
    this.currentStep.set(1);
    this.successMessage = '';
    this.errorMessage = '';
  }

  /**
   * Inscription de l'utilisateur
   */
  register(): void {
    this.doNotMatch.set(false);
    this.error.set(false);
    this.loading.set(true);
    this.errorEmailExists.set(false);
    this.errorUserExists.set(false);

    const { password, confirmPassword } = this.registerForm.getRawValue();

    // Vérification des mots de passe
    if (password !== confirmPassword) {
      this.doNotMatch.set(true);
      this.loading.set(false);
      return;
    }

    const { firstName, lastName, login, email } = this.registerForm.getRawValue();

    // Préparation des données d'inscription pour le plan FREE
    const registrationData = {
      firstName,
      lastName,
      login,
      email,
      password,
      langKey: this.translateService.currentLang,
      planType: 'FREE', // Plan gratuit par défaut
      authorities: ['ROLE_USER'], // Rôle utilisateur de base
    };

    this.registerService.save(registrationData).subscribe({
      next: () => this.successResponse(),
      error: response => this.processError(response),
    });
  }

  /**
   * Traitement de la réponse de succès
   */
  private successResponse(): void {
    this.successMessage = 'Félicitations ! Votre compte FREE a été créé avec succès !';
    this.success.set(true);
    this.loading.set(false);

    // Redirection après 3 secondes
    setTimeout(() => {
      this.router.navigate(['/login']);
    }, 3000);
  }

  /**
   * Traitement des erreurs
   */
  private processError(response: HttpErrorResponse): void {
    this.loading.set(false);

    if (response.status === 400 && response.error.type === LOGIN_ALREADY_USED_TYPE) {
      this.errorUserExists.set(true);
      this.errorMessage = "Ce nom d'utilisateur est déjà utilisé.";
    } else if (response.status === 400 && response.error.type === EMAIL_ALREADY_USED_TYPE) {
      this.errorEmailExists.set(true);
      this.errorMessage = 'Cette adresse email est déjà utilisée.';
    } else {
      this.error.set(true);
      this.errorMessage = 'Une erreur est survenue lors de la création de votre compte. Veuillez réessayer.';
    }
  }

  /**
   * Navigation vers la page de connexion
   */
  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  /**
   * Vérifier la force du mot de passe
   */
  getPasswordStrength(): number {
    const password = this.registerForm.get('password')?.value || '';
    let strength = 0;

    if (password.length >= 8) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^a-zA-Z0-9]/.test(password)) strength++;

    return strength;
  }

  /**
   * Obtenir la couleur de la force du mot de passe
   */
  getPasswordStrengthColor(): string {
    const strength = this.getPasswordStrength();
    if (strength <= 2) return 'bg-red-500';
    if (strength <= 3) return 'bg-yellow-500';
    if (strength <= 4) return 'bg-blue-500';
    return 'bg-green-500';
  }

  /**
   * Obtenir le texte de la force du mot de passe
   */
  getPasswordStrengthText(): string {
    const strength = this.getPasswordStrength();
    if (strength <= 2) return 'Faible';
    if (strength <= 3) return 'Moyen';
    if (strength <= 4) return 'Fort';
    return 'Très fort';
  }
}
