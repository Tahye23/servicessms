import { Component, OnInit, AfterViewInit, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormGroup, FormControl, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { LoginService } from 'app/login/login.service';
import { AccountService } from 'app/core/auth/account.service';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { CheckboxModule } from 'primeng/checkbox';
import NavbarComponent from '../layouts/navbar/navbar.component';
@Component({
  standalone: true,
  selector: 'jhi-login',
  imports: [SharedModule, InputTextModule, FormsModule, CheckboxModule, PasswordModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export default class LoginComponent implements OnInit, AfterViewInit {
  username = viewChild.required<ElementRef>('username');
  isConcting = false;
  loading = false;
  authenticationError = signal(false);
  error = false;
  message = '';
  loginForm = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    rememberMe: new FormControl(false, { nonNullable: true, validators: [Validators.required] }),
  });

  private accountService = inject(AccountService);
  private loginService = inject(LoginService);
  private router = inject(Router);

  ngOnInit(): void {
    // if already authenticated then navigate to home page
    this.accountService.identity().subscribe(() => {
      if (this.accountService.isAuthenticated()) {
        this.router.navigate(['/dashbord']);
      }
    });
  }

  ngAfterViewInit(): void {
    this.username().nativeElement.focus();
  }

  login(): void {
    this.loading = true; // Spinner ON

    this.loginService.login(this.loginForm.getRawValue()).subscribe({
      next: () => {
        this.authenticationError.set(false);
        this.accountService.identity(true).subscribe({
          next: () => {
            this.loading = false; // Spinner OFF
            if (!this.router.getCurrentNavigation()) {
              this.router.navigate(['/dashbord']);
            }
          },
          error: () => {
            this.loading = false;
            this.authenticationError.set(true);
            this.error = true;
            this.message = 'Erreur lors de la récupération du compte.';
          },
        });
      },
      error: error => {
        this.loading = false;
        this.authenticationError.set(true);
        this.error = true;
        this.message = error.error?.message ?? 'Une erreur est survenue lors de la connexion.';
      },
    });
  }
  goToHome() {
    this.router.navigate(['/']);
  }
}
