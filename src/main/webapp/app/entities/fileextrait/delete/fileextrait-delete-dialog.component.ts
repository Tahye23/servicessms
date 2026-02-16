import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IFileextrait } from '../fileextrait.model';
import { FileextraitService } from '../service/fileextrait.service';

@Component({
  standalone: true,
  templateUrl: './fileextrait-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class FileextraitDeleteDialogComponent {
  fileextrait?: IFileextrait;

  protected fileextraitService = inject(FileextraitService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.fileextraitService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
