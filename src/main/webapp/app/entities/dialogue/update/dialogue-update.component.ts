import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IDialogue } from '../dialogue.model';
import { DialogueService } from '../service/dialogue.service';
import { DialogueFormService, DialogueFormGroup } from './dialogue-form.service';

@Component({
  standalone: true,
  selector: 'jhi-dialogue-update',
  templateUrl: './dialogue-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class DialogueUpdateComponent implements OnInit {
  isSaving = false;
  dialogue: IDialogue | null = null;

  protected dialogueService = inject(DialogueService);
  protected dialogueFormService = inject(DialogueFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: DialogueFormGroup = this.dialogueFormService.createDialogueFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ dialogue }) => {
      this.dialogue = dialogue;
      if (dialogue) {
        this.updateForm(dialogue);
      }
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const dialogue = this.dialogueFormService.getDialogue(this.editForm);
    if (dialogue.id !== null) {
      this.subscribeToSaveResponse(this.dialogueService.update(dialogue));
    } else {
      this.subscribeToSaveResponse(this.dialogueService.create(dialogue));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IDialogue>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(dialogue: IDialogue): void {
    this.dialogue = dialogue;
    this.dialogueFormService.resetForm(this.editForm, dialogue);
  }
}
