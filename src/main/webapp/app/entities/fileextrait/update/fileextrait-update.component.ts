import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AlertError } from 'app/shared/alert/alert-error.model';
import { EventManager, EventWithContent } from 'app/core/util/event-manager.service';
import { DataUtils, FileLoadError } from 'app/core/util/data-util.service';
import { FileextraitService } from '../service/fileextrait.service';
import { IFileextrait } from '../fileextrait.model';
import { FileextraitFormService, FileextraitFormGroup } from './fileextrait-form.service';

@Component({
  standalone: true,
  selector: 'jhi-fileextrait-update',
  templateUrl: './fileextrait-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class FileextraitUpdateComponent implements OnInit {
  isSaving = false;
  fileextrait: IFileextrait | null = null;

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected fileextraitService = inject(FileextraitService);
  protected fileextraitFormService = inject(FileextraitFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: FileextraitFormGroup = this.fileextraitFormService.createFileextraitFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ fileextrait }) => {
      this.fileextrait = fileextrait;
      if (fileextrait) {
        this.updateForm(fileextrait);
      }
    });
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    this.dataUtils.openFile(base64String, contentType);
  }

  setFileData(event: Event, field: string, isImage: boolean): void {
    this.dataUtils.loadFileToForm(event, this.editForm, field, isImage).subscribe({
      error: (err: FileLoadError) =>
        this.eventManager.broadcast(new EventWithContent<AlertError>('migrationApp.error', { ...err, key: 'error.file.' + err.key })),
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const fileextrait = this.fileextraitFormService.getFileextrait(this.editForm);
    if (fileextrait.id !== null) {
      this.subscribeToSaveResponse(this.fileextraitService.update(fileextrait));
    } else {
      this.subscribeToSaveResponse(this.fileextraitService.create(fileextrait));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IFileextrait>>): void {
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

  protected updateForm(fileextrait: IFileextrait): void {
    this.fileextrait = fileextrait;
    this.fileextraitFormService.resetForm(this.editForm, fileextrait);
  }
}
