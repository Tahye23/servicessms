import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SubscriptionService } from './service/subscriptionService.service';

@Component({
  selector: 'app-upgrade-suggestion',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      *ngIf="shouldShow()"
      class="bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 text-white rounded-lg p-4 max-w-md mx-auto shadow-lg ring-2 ring-indigo-400"
    >
      <div class="flex justify-between items-center cursor-pointer" (click)="toggleDetails()">
        <div class="flex items-center gap-3">
          <i class="pi pi-star text-yellow-300 text-xl select-none"></i>
          <h4 class="text-lg font-semibold select-none">Améliorez votre expérience</h4>
        </div>
        <button
          type="button"
          aria-label="Afficher les détails"
          class="transform transition-transform duration-300"
          [class.rotate-180]="detailsVisible()"
          (click)="$event.stopPropagation(); toggleDetails()"
        >
          <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </div>

      <div
        class="overflow-hidden transition-all duration-300 ease-in-out mt-3 text-sm"
        [style.maxHeight]="detailsVisible() ? '500px' : '0px'"
      >
        <p class="mb-4 opacity-90">{{ getSuggestionMessage() }}</p>
        <button
          (click)="navigateToUpgrade()"
          class="bg-white bg-opacity-20 border border-white border-opacity-30 px-4 py-2 rounded-md font-semibold hover:bg-opacity-30 transition w-full"
        >
          Voir les plans
        </button>
      </div>
    </div>
  `,
})
export class UpgradeSuggestionComponent implements OnInit {
  private subscriptionService = inject(SubscriptionService);

  detailsVisible = signal(false);

  ngOnInit() {
    this.subscriptionService.loadUserSubscriptions().subscribe();
  }

  shouldShow(): boolean {
    return this.subscriptionService.needsUpgrade();
  }

  getSuggestionMessage(): string {
    const suggestions = this.subscriptionService.getUpgradeSuggestions();
    return suggestions[0] || 'Découvrez nos plans premium';
  }

  toggleDetails() {
    this.detailsVisible.set(!this.detailsVisible());
  }

  navigateToUpgrade() {
    console.log("Navigation vers la page d'upgrade");
    // Implémenter la navigation réelle ici
  }
}
