import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { ITokensApp } from '../tokens-app.model';
import { TokensAppService } from '../service/tokens-app.service';

@Component({
  standalone: true,
  templateUrl: './tokens-app-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class TokensAppDeleteDialogComponent {
  tokensApp?: ITokensApp;

  protected tokensAppService = inject(TokensAppService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.tokensAppService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
