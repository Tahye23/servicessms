import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { SendSmsDetailComponent } from '../../send-sms/detail/send-sms-detail.component';

@Component({
  selector: 'jhi-send-whatsapp-detail',
  standalone: true,
  imports: [CommonModule, FormatMediumDatetimePipe, RouterModule, SendSmsDetailComponent],
  templateUrl: './send-whatsapp-detail.component.html',
  styleUrl: './send-whatsapp-detail.component.scss',
})
export class SendWhatsappDetailComponent implements OnInit {
  loading = false;
  template: any;

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected contactService: ContactService,
  ) {}

  ngOnInit(): void {
    this.loading = true;
  }
}
