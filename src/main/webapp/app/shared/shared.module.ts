import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { TranslateModule } from '@ngx-translate/core';
import { MessageService } from 'primeng/api'; // Assurez-vous que vous importez MessageService depuis primeng/api

import FindLanguageFromKeyPipe from './language/find-language-from-key.pipe';
import TranslateDirective from './language/translate.directive';
import { AlertComponent } from './alert/alert.component';
import { AlertErrorComponent } from './alert/alert-error.component';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { PaginatorModule } from 'primeng/paginator';
import { DividerModule } from 'primeng/divider';
import { MegaMenuModule } from 'primeng/megamenu';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ToastrModule } from 'ngx-toastr';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

interface PageEvent {
  first: number;
  rows: number;
  page: number;
  pageCount: number;
}

/**
 * Application wide Module
 */
@NgModule({
  imports: [AlertComponent, DialogModule, AlertErrorComponent, FindLanguageFromKeyPipe, TranslateDirective],
  exports: [
    CommonModule,
    SelectButtonModule,
    DividerModule,
    PaginatorModule,
    ButtonModule,
    CardModule,
    MegaMenuModule,
    TableModule,
    NgbModule,
    FontAwesomeModule,
    ToastrModule,
    AlertComponent,
    AlertErrorComponent,
    TranslateModule,
    FindLanguageFromKeyPipe,
    TranslateDirective,
    ToastModule,
    ConfirmDialogModule,
  ],
})
export default class SharedModule {
  first = 0;
  rows = 10;
}
