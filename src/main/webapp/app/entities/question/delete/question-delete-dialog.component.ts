import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IQuestion } from '../question.model';
import { QuestionService } from '../service/question.service';

@Component({
  standalone: true,
  templateUrl: './question-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class QuestionDeleteDialogComponent {
  question?: IQuestion;

  protected questionService = inject(QuestionService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.questionService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
