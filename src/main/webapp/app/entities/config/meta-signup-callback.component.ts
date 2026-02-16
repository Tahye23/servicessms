import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ConfigService } from './config.service';
import { NgIf } from '@angular/common';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-meta-signup-callback',
  templateUrl: './meta-signup-callback.component.html',
  standalone: true,
  imports: [NgIf],
})
export class MetaSignupCallbackComponent implements OnInit, OnDestroy {
  private sub?: Subscription;
  loading = true;
  success = false;
  error = false;
  fragment: any;
  constructor(
    private route: ActivatedRoute,
    private metaSignup: ConfigService,
  ) {}

  ngOnInit(): void {
    this.sub = this.route.fragment.subscribe(fragment => {
      this.fragment = fragment;
      const token = new URLSearchParams(fragment || '').get('long_lived_token');
      if (!token) {
        this.handleError();
        return;
      }

      // Nettoyage URL
      window.history.replaceState({}, document.title, window.location.href.split('#')[0]);

      this.metaSignup.finalizeSignup(token, this.webhookUrl).subscribe({
        next: () => this.handleSuccess(),
        error: () => this.handleError(),
      });
    });
  }
  get webhookUrl(): string {
    return `${window.location.origin}/api/webhook`;
  }
  private handleSuccess() {
    this.loading = false;
    this.success = true;
    if (window.opener) {
      window.opener.postMessage({ type: 'META_SIGNUP_SUCCESS' }, window.location.origin);
    }
    setTimeout(() => window.close(), 1500);
  }

  private handleError() {
    this.loading = false;
    this.error = true;
    if (window.opener) {
      window.opener.postMessage({ type: 'META_SIGNUP_ERROR' }, window.location.origin);
    }
    // Laisse la popup ouverte pour affichage de l'erreur
  }

  closeNow() {
    if (window.opener) {
      window.opener.postMessage({ type: this.error ? 'META_SIGNUP_ERROR' : 'META_SIGNUP_SUCCESS' }, window.location.origin);
    }
    window.close();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}
