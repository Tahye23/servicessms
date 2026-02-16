import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IGroupedecontact } from '../groupedecontact.model';
import { GroupedecontactService } from '../service/groupedecontact.service';

@Component({
  standalone: true,
  templateUrl: './groupedecontact-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class GroupedecontactDeleteDialogComponent {
  groupedecontact?: IGroupedecontact;

  protected groupedecontactService = inject(GroupedecontactService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.groupedecontactService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
