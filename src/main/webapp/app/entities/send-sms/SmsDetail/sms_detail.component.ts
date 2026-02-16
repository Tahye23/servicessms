// sms_detail.component.ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SendSmsService } from '../service/send-sms.service';
import { MessageType, Sms } from '../send-sms.model';
import { DatePipe, NgClass, NgIf } from '@angular/common';
import { TemplateRendererComponent } from '../../template/detailMessage/template-renderer.component';

@Component({
  selector: 'app-sms-detail',
  templateUrl: './sms_detail.component.html',
  standalone: true,
  imports: [DatePipe, NgClass, NgIf, TemplateRendererComponent],
})
export class SmsDetailComponent implements OnInit {
  sms?: Sms;
  loading = false;
  error = '';
  isSms = true;

  // ✅ NOUVEAUX ÉTATS POUR L'ENVOI
  sending = false;
  sendSuccess = false;
  sendError = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: SendSmsService,
  ) {}

  ngOnInit(): void {
    const smsId = Number(this.route.snapshot.paramMap.get('smsId'));
    if (smsId) {
      this.fetchSms(smsId);
    }
  }

  private fetchSms(id: number): void {
    this.loading = true;
    this.service.getSmsById(id).subscribe({
      next: sms => {
        this.sms = sms;
        console.log('sms', sms);
        if (this.sms.type == MessageType.WHATSAPP) {
          this.isSms = false;
        }
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger le détail du SMS.';
        this.loading = false;
      },
    });
  }

  // ✅ NOUVELLE MÉTHODE : Envoi du SMS
  sendSms(test: boolean = false): void {
    if (!this.sms?.id) {
      return;
    }

    // Confirmation avant envoi en production
    if (!test) {
      const confirmSend = confirm(
        `Êtes-vous sûr de vouloir envoyer ce ${this.isSms ? 'SMS' : 'message WhatsApp'} à ${this.sms.receiver} ?`,
      );

      if (!confirmSend) {
        return;
      }
    }

    this.sending = true;
    this.sendSuccess = false;
    this.sendError = '';

    this.service.sendSingleSms(this.sms.id, test).subscribe({
      next: response => {
        console.log('✅ Réponse envoi:', response);

        this.sending = false;
        this.sendSuccess = true;

        // Afficher un message de succès
        if (response.success) {
          alert(
            `✅ ${this.isSms ? 'SMS' : 'Message WhatsApp'} envoyé avec succès !${response.messageId ? '\nID: ' + response.messageId : ''}`,
          );

          // Recharger les données du SMS pour voir le nouveau statut
          this.fetchSms(this.sms!.id);
        } else {
          this.sendError = response.error || "Échec de l'envoi";
          alert(`❌ Échec de l'envoi: ${this.sendError}`);
        }

        // Réinitialiser après 3 secondes
        setTimeout(() => {
          this.sendSuccess = false;
          this.sendError = '';
        }, 3000);
      },
      error: err => {
        console.error('❌ Erreur envoi:', err);
        this.sending = false;
        this.sendError = err.error?.error || err.message || "Erreur lors de l'envoi";
        alert(`❌ Erreur: ${this.sendError}`);

        // Réinitialiser après 3 secondes
        setTimeout(() => {
          this.sendError = '';
        }, 3000);
      },
    });
  }

  // ✅ VÉRIFIER SI LE SMS PEUT ÊTRE ENVOYÉ
  canSend(): boolean {
    if (!this.sms) return false;

    // On peut envoyer si:
    // - Le statut est "pending" ou "failed"
    // - Pas déjà en cours d'envoi
    const allowedStatuses = ['pending', 'failed'];
    return allowedStatuses.includes(this.sms.deliveryStatus?.toLowerCase() || '') && !this.sending;
  }

  goBack(): void {
    window.history.back();
  }

  protected readonly Math = Math;
}
