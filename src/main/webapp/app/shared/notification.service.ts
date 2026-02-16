import { Injectable, inject, Input, Component } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, from } from 'rxjs';
import { catchError, of } from 'rxjs';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'jhi-confirmation-modal',
  standalone: true,
  imports: [CommonModule],
  styles: [
    `
      :host-context(.modal) {
        position: fixed !important;
        top: 0 !important;
        left: 0 !important;
        width: 100% !important;
        height: 100% !important;
        z-index: 1050 !important;
        display: flex !important;
        align-items: center !important;
        justify-content: center !important;
      }
    `,
  ],
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
        <div class="modal-header border-0 pb-0 p-6">
          <h4 class="modal-title text-lg font-semibold text-gray-900">{{ title }}</h4>
        </div>
        <div class="modal-body py-4 px-6">
          <div class="flex items-start space-x-3">
            <div class="flex-shrink-0">
              <svg [class]="iconClass" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" [attr.d]="iconPath" />
              </svg>
            </div>
            <p class="text-gray-700">{{ message }}</p>
          </div>
        </div>
        <div class="modal-footer border-0 pt-0 p-6 flex justify-end space-x-2">
          <button
            *ngIf="cancelText"
            type="button"
            class="px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600"
            (click)="activeModal.dismiss()"
          >
            {{ cancelText }}
          </button>
          <button type="button" [class]="confirmButtonClass" (click)="activeModal.close(true)">
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  `,
})
export class ConfirmationModalComponent {
  @Input() title = 'Confirmation';
  @Input() message = '';
  @Input() confirmText = 'Confirmer';
  @Input() cancelText = 'Annuler';
  @Input() type: 'success' | 'warning' | 'danger' = 'warning';

  protected activeModal = inject(NgbActiveModal);

  get iconClass(): string {
    const base = 'w-6 h-6';
    switch (this.type) {
      case 'success':
        return `${base} text-green-600`;
      case 'danger':
        return `${base} text-red-600`;
      default:
        return `${base} text-orange-600`;
    }
  }

  get iconPath(): string {
    switch (this.type) {
      case 'success':
        return 'M5 13l4 4L19 7';
      case 'danger':
        return 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z';
      default:
        return 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z';
    }
  }

  get confirmButtonClass(): string {
    const base = 'px-4 py-2 rounded-lg font-medium';
    switch (this.type) {
      case 'success':
        return `${base} bg-green-600 text-white hover:bg-green-700`;
      case 'danger':
        return `${base} bg-red-600 text-white hover:bg-red-700`;
      default:
        return `${base} bg-orange-600 text-white hover:bg-orange-700`;
    }
  }
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private modalService = inject(NgbModal);

  confirm(message: string, title: string = 'Confirmation', type: 'success' | 'warning' | 'danger' = 'warning'): Observable<boolean> {
    const modalRef = this.modalService.open(ConfirmationModalComponent, {
      centered: true,
      backdrop: 'static',
    });

    modalRef.componentInstance.title = title;
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.type = type;

    // Convertir la Promise en Observable
    return from(modalRef.result).pipe(catchError(() => of(false)));
  }

  success(message: string, title: string = 'Succès'): Observable<boolean> {
    const modalRef = this.modalService.open(ConfirmationModalComponent, {
      centered: true,
    });

    modalRef.componentInstance.title = title;
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.type = 'success';
    modalRef.componentInstance.confirmText = 'OK';
    modalRef.componentInstance.cancelText = '';

    return from(modalRef.result).pipe(catchError(() => of(true)));
  }
}
